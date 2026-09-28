package com.universoempaques.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);
    private static final String[] DIAS = {"1-lunes", "2-martes", "3-miercoles", "4-jueves", "5-viernes", "6-sabado", "7-domingo"};

    @Value("${backup.activo:true}")
    private boolean activo;
    @Value("${backup.base-datos:universo_empaques}")
    private String baseDatos;
    @Value("${backup.host:localhost}")
    private String host;
    @Value("${backup.puerto:3306}")
    private int puerto;
    @Value("${backup.directorio:./respaldos}")
    private String directorio;
    /** Segunda copia opcional (USB, carpeta sincronizada, otro PC). Vacio = desactivada. */
    @Value("${backup.directorio-copia:}")
    private String directorioCopia;
    @Value("${backup.ruta-mysqldump:mysqldump}")
    private String rutaMysqldump;
    @Value("${backup.ruta-mysql:mysql}")
    private String rutaMysql;

    // Mismas credenciales con las que la app se conecta a MySQL.
    @Value("${spring.datasource.username}")
    private String root;
    @Value("${spring.datasource.password}")
    private String universo123;


    private final ReentrantLock candado = new ReentrantLock();


    @Scheduled(cron = "${backup.cron:0 0 19 * * *}")
    public void respaldoProgramado() {
        if (activo) respaldarAhora();
    }


    @EventListener(ApplicationReadyEvent.class)
    public void respaldoAlArrancar() {
        if (activo && !hayCopiaDeHoy()) respaldarAhora();
    }


    public void respaldarAhora() {
        if (!candado.tryLock()) {
            log.warn("Ya hay un respaldo en curso; se omite este.");
            return;
        }
        Path opciones = null;
        try {
            opciones = crearArchivoOpciones();
            respaldar(opciones);
        } catch (Exception e) {
            // Un respaldo fallido nunca debe tumbar la aplicacion.
            log.error("Fallo el respaldo: {}", e.getMessage());
        } finally {
            borrarSilencioso(opciones);
            candado.unlock();
        }
    }


    private void respaldar(Path opciones) throws IOException {
        Path carpeta = Files.createDirectories(Paths.get(directorio).toAbsolutePath());
        Path mensual = carpeta.resolve("mensual.sql");
        Path semanal = carpeta.resolve("semanal.sql");
        Path manifiesto = carpeta.resolve("semanal.manifiesto");
        LocalDate hoy = LocalDate.now();

        boolean mensualDebido = !Files.exists(mensual)
                || YearMonth.from(fechaDe(mensual)).isBefore(YearMonth.from(hoy));
        boolean semanalDebido = !Files.exists(semanal) || !Files.exists(manifiesto)
                || fechaDe(semanal).isBefore(hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));

        if (mensualDebido || semanalDebido) {
            respaldoCompleto(opciones, carpeta, mensualDebido, semanalDebido);
        } else {
            respaldoParcial(opciones, carpeta, hoy);
        }
    }

    private void respaldoCompleto(Path opciones, Path carpeta, boolean mensualDebido, boolean semanalDebido)
            throws IOException {
        // Las huellas se toman ANTES del volcado: si algo cambia en medio, el
        // parcial siguiente lo volvera a incluir (queda del lado seguro).
        Map<String, String> huellas = huellasDeTablas(opciones);

        Path temporal = carpeta.resolve("completo.tmp");
        try {
            ejecutar(List.of(rutaMysqldump, "--defaults-extra-file=" + opciones,
                    "--single-transaction", "--routines", "--triggers",
                    "--default-character-set=utf8mb4",
                    "--result-file=" + temporal,
                    "--databases", baseDatos), null);

            if (mensualDebido) {
                publicar(temporal, carpeta.resolve("mensual.sql"));
                copiarASegundoLugar(carpeta.resolve("mensual.sql"));
            }
            if (semanalDebido) {
                publicar(temporal, carpeta.resolve("semanal.sql"));
                guardarManifiesto(carpeta.resolve("semanal.manifiesto"), huellas);
                borrarParciales(carpeta); // eran "cambios respecto al semanal anterior": ya no sirven
                copiarASegundoLugar(carpeta.resolve("semanal.sql"));
            }
            log.info("Respaldo completo listo (mensual={}, semanal={}).", mensualDebido, semanalDebido);
        } finally {
            borrarSilencioso(temporal);
        }
    }

    private void respaldoParcial(Path opciones, Path carpeta, LocalDate hoy) throws IOException {
        Properties base = cargarManifiesto(carpeta.resolve("semanal.manifiesto"));
        Map<String, String> actuales = huellasDeTablas(opciones);

        // Tablas nuevas o con contenido distinto al que tenian en el respaldo semanal.
        List<String> cambiadas = actuales.entrySet().stream()
                .filter(e -> !e.getValue().equals(base.getProperty(e.getKey())))
                .map(Map.Entry::getKey)
                .toList();

        Path destino = carpeta.resolve("parcial_" + DIAS[hoy.getDayOfWeek().getValue() - 1] + ".sql");
        Path temporal = carpeta.resolve("parcial.tmp");
        try {
            if (cambiadas.isEmpty()) {
                Files.writeString(temporal, "-- Sin cambios desde el respaldo semanal.\n", StandardCharsets.UTF_8);
            } else {
                List<String> comando = new ArrayList<>(List.of(rutaMysqldump, "--defaults-extra-file=" + opciones,
                        "--single-transaction", "--default-character-set=utf8mb4",
                        "--result-file=" + temporal, baseDatos));
                comando.addAll(cambiadas);
                ejecutar(comando, null);
            }
            publicar(temporal, destino);
            copiarASegundoLugar(destino);
            log.info("Respaldo parcial listo ({}): tablas {}", destino.getFileName(), cambiadas);
        } finally {
            borrarSilencioso(temporal);
        }
    }

    private Map<String, String> huellasDeTablas(Path opciones) throws IOException {
        List<String> base = List.of(rutaMysql, "--defaults-extra-file=" + opciones, "--batch", "--skip-column-names");

        List<String> tablas = ejecutar(conConsulta(base, "SHOW TABLES FROM `" + baseDatos + "`"), null)
                .lines().map(String::trim)
                .filter(l -> l.matches("[A-Za-z0-9_$]+"))   // descarta avisos u otro texto
                .toList();

        Map<String, String> huellas = new TreeMap<>();
        if (tablas.isEmpty()) return huellas;

        String consulta = "CHECKSUM TABLE " + tablas.stream()
                .map(t -> "`" + baseDatos + "`.`" + t + "`")
                .collect(Collectors.joining(", "));
        for (String linea : ejecutar(conConsulta(base, consulta), null).split("\\R")) {
            String[] partes = linea.trim().split("\t");
            if (partes.length == 2 && partes[1].matches("\\d+")) {
                huellas.put(partes[0].substring(partes[0].lastIndexOf('.') + 1), partes[1]);
            }
        }
        return huellas;
    }

    private static List<String> conConsulta(List<String> base, String consulta) {
        List<String> comando = new ArrayList<>(base);
        comando.add("--execute");
        comando.add(consulta);
        return comando;
    }

    private void guardarManifiesto(Path archivo, Map<String, String> huellas) throws IOException {
        Properties p = new Properties();
        p.putAll(huellas);
        try (Writer w = Files.newBufferedWriter(archivo, StandardCharsets.UTF_8)) {
            p.store(w, "Huellas de las tablas al momento del respaldo semanal");
        }
    }

    private Properties cargarManifiesto(Path archivo) throws IOException {
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            p.load(r);
        }
        return p;
    }


    private String ejecutar(List<String> comando, Path archivoDeEntrada) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(comando).redirectErrorStream(true);
        if (archivoDeEntrada != null) pb.redirectInput(archivoDeEntrada.toFile());

        Process proceso;
        try {
            proceso = pb.start();
        } catch (IOException e) {
            throw new IOException("No se encontro el programa '" + comando.get(0)
                    + "'. Revisen backup.ruta-mysqldump / backup.ruta-mysql en application.properties.", e);
        }
        String salida = new String(proceso.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        try {
            if (proceso.waitFor() != 0) {
                throw new IOException("MySQL respondio con error: " + salida.trim());
            }
        } catch (InterruptedException e) {
            proceso.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new IOException("La operacion fue interrumpida.");
        }
        return salida;
    }

    /** Archivo temporal con usuario/clave: la clave no aparece en la linea de comandos. */
    private Path crearArchivoOpciones() throws IOException {
        Path archivo = Files.createTempFile("ue-mysql-", ".cnf");
        Files.writeString(archivo, "[client]\n"
                + "user=\"" + escapar(root) + "\"\n"
                + "password=\"" + escapar(universo123) + "\"\n"
                + "host=\"" + escapar(host) + "\"\n"
                + "port=" + puerto + "\n", StandardCharsets.UTF_8);
        return archivo;
    }

    private static String escapar(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void publicar(Path origen, Path destino) throws IOException {
        Path intermedio = destino.resolveSibling(destino.getFileName() + ".tmp");
        Files.copy(origen, intermedio, StandardCopyOption.REPLACE_EXISTING);
        Files.move(intermedio, destino, StandardCopyOption.REPLACE_EXISTING);
    }

    private void borrarParciales(Path carpeta) throws IOException {
        try (Stream<Path> archivos = Files.list(carpeta)) {
            for (Path p : archivos.filter(p -> p.getFileName().toString().startsWith("parcial_")).toList()) {
                Files.deleteIfExists(p);
            }
        }
    }

    private void copiarASegundoLugar(Path origen) {
        if (directorioCopia == null || directorioCopia.isBlank()) return;
        try {
            Path carpeta = Files.createDirectories(Paths.get(directorioCopia));
            Files.copy(origen, carpeta.resolve(origen.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            // Que falle la segunda copia (ej. USB desconectada) no invalida la principal.
            log.warn("No se pudo copiar {} a '{}': {}", origen.getFileName(), directorioCopia, e.getMessage());
        }
    }

    private boolean hayCopiaDeHoy() {
        Path carpeta = Paths.get(directorio).toAbsolutePath();
        if (!Files.isDirectory(carpeta)) return false;
        LocalDate hoy = LocalDate.now();
        try (Stream<Path> archivos = Files.list(carpeta)) {
            return archivos.filter(p -> p.getFileName().toString().endsWith(".sql"))
                    .anyMatch(p -> fechaDe(p).equals(hoy));
        } catch (IOException e) {
            return false;
        }
    }

    private static LocalDate fechaDe(Path p) {
        try {
            return Files.getLastModifiedTime(p).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        } catch (IOException e) {
            return LocalDate.MIN;
        }
    }

    private static void borrarSilencioso(Path p) {
        if (p == null) return;
        try {
            Files.deleteIfExists(p);
        } catch (IOException ignored) {
        }
    }
}