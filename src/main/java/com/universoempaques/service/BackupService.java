package com.universoempaques.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * RF-19: copia de seguridad de la base de datos (autora: Amelie).
 *
 * Estrategia: una copia TOTAL (mensual.sql, la que se entrega al ente
 * externo; se sobrescribe, no se guarda un historico), una COMPLETA
 * semanal (semanal.sql) y, los demas dias, una PARCIAL solo con las
 * tablas que cambiaron desde la semanal (parcial_<dia>.sql).
 *
 * Agregado para la interfaz del administrador (AdminRespaldoController):
 *  - "Hacer copia ahora" hace una copia TOTAL que reemplaza mensual.sql
 *    (no toca la semanal ni las parciales, para no romper su logica).
 *  - La copia automatica (encendida/apagada, hora y segunda carpeta) se
 *    configura desde la pantalla y se guarda en
 *    <carpeta de respaldos>/configuracion.properties (propio de cada equipo).
 *  - Estado de la ultima ejecucion, lista de archivos y descarga segura.
 *  - Restaurar la base desde un archivo de la carpeta (ver restaurar()).
 *    Antes de restaurar se guarda el estado actual en antes-de-restaurar.sql,
 *    asi que una restauracion equivocada se puede deshacer.
 */
@Service
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);
    /** Copia automatica del estado que habia justo antes de la ultima restauracion. */
    public static final String ANTES_DE_RESTAURAR = "antes-de-restaurar.sql";
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


    @Value("${backup.cron:0 0 19 * * *}")
    private String cron;

    private final ReentrantLock candado = new ReentrantLock();

    /** Programador de Spring: permite cambiar la hora sin reiniciar la app. */
    private final TaskScheduler programador;
    private volatile ScheduledFuture<?> tareaProgramada;

    public BackupService(TaskScheduler programador) {
        this.programador = programador;
    }

    // ---- Estado de la ultima ejecucion (lo muestra la pantalla del admin) ----
    private volatile LocalDateTime ultimaEjecucion;
    private volatile boolean ultimaExitosa;
    private volatile String ultimoMensaje = "Aún no se ha ejecutado ninguna copia desde que arrancó la aplicación.";

    /** Un archivo de respaldo, para listarlo en pantalla. */
    public record ArchivoRespaldo(String nombre, String tipo, long tamanoBytes, LocalDateTime modificado) {
        public String getTamanoLegible() {
            if (tamanoBytes < 1024) return tamanoBytes + " B";
            if (tamanoBytes < 1024 * 1024) return String.format("%.1f KB", tamanoBytes / 1024.0);
            return String.format("%.1f MB", tamanoBytes / (1024.0 * 1024));
        }
    }

    /** Una opcion del selector "Restaurar la base de datos". */
    public record OpcionRestauracion(String archivo, String descripcion) {
    }


    /** Al arrancar: lee la configuracion guardada, programa la copia automatica y, si hace falta, respalda. */
    @EventListener(ApplicationReadyEvent.class)
    public void respaldoAlArrancar() {
        cargarConfiguracionGuardada();
        programar();
        if (activo && !hayCopiaDeHoy()) respaldarAhora();
    }

    /** (Re)programa la copia automatica segun "activo" y "cron". */
    private synchronized void programar() {
        if (tareaProgramada != null) {
            tareaProgramada.cancel(false);
            tareaProgramada = null;
        }
        if (activo) {
            tareaProgramada = programador.schedule(this::respaldarAhora, new CronTrigger(cron));
            log.info("Copia automática programada: {}", getHorarioLegible());
        }
    }

    /**
     * Boton "Hacer copia ahora": copia TOTAL de la base que reemplaza
     * mensual.sql. No toca semanal.sql, su manifiesto ni las parciales,
     * asi la estrategia automatica sigue teniendo sentido.
     */
    public boolean respaldarTotal() {
        return conCandado(opciones -> {
            Path carpeta = Files.createDirectories(Paths.get(directorio).toAbsolutePath());
            respaldoCompleto(opciones, carpeta, true, false);
            return "Copia total lista: se actualizó mensual.sql.";
        });
    }

    /**
     * Copia automatica (la del horario): decide sola si toca mensual,
     * semanal o parcial. Devuelve true si salio bien.
     */
    public boolean respaldarAhora() {
        return conCandado(this::respaldar);
    }

    @FunctionalInterface
    private interface Operacion {
        String ejecutar(Path opciones) throws IOException;
    }

    /** Evita dos copias al tiempo y guarda el resultado para la pantalla. */
    private boolean conCandado(Operacion operacion) {
        if (!candado.tryLock()) {
            log.warn("Ya hay un respaldo en curso; se omite este.");
            ultimoMensaje = "Ya había una copia en curso; espera unos segundos y revisa la lista.";
            return false;
        }
        Path opciones = null;
        try {
            opciones = crearArchivoOpciones();
            ultimoMensaje = operacion.ejecutar(opciones);
            ultimaExitosa = true;
            return true;
        } catch (Exception e) {
            // Un respaldo fallido nunca debe tumbar la aplicacion.
            log.error("Fallo el respaldo: {}", e.getMessage());
            ultimaExitosa = false;
            ultimoMensaje = e.getMessage();
            return false;
        } finally {
            ultimaEjecucion = LocalDateTime.now();
            borrarSilencioso(opciones);
            candado.unlock();
        }
    }


    private String respaldar(Path opciones) throws IOException {
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
            return "Copia completa lista (" + (mensualDebido ? "mensual" : "")
                    + (mensualDebido && semanalDebido ? " y " : "") + (semanalDebido ? "semanal" : "") + ").";
        } else {
            respaldoParcial(opciones, carpeta, hoy);
            return "Copia parcial lista (solo las tablas que cambiaron desde la semanal).";
        }
    }

    private void respaldoCompleto(Path opciones, Path carpeta, boolean mensualDebido, boolean semanalDebido)
            throws IOException {
        // Las huellas se toman ANTES del volcado: si algo cambia en medio, el
        // parcial siguiente lo volvera a incluir (queda del lado seguro).
        Map<String, String> huellas = huellasDeTablas(opciones);

        Path temporal = carpeta.resolve("completo.tmp");
        try {
            volcarCompleto(opciones, temporal);

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

    /** mysqldump de TODA la base (con CREATE DATABASE/USE, para restaurarla sola). */
    private void volcarCompleto(Path opciones, Path destino) throws IOException {
        ejecutar(List.of(rutaMysqldump, "--defaults-extra-file=" + opciones,
                "--single-transaction", "--routines", "--triggers",
                "--default-character-set=utf8mb4",
                "--result-file=" + destino,
                "--databases", baseDatos), null);
    }

    // ------------------------------------------------------------------
    // Restaurar
    // ------------------------------------------------------------------

    /** Archivos que se pueden restaurar desde la pantalla. */
    public static boolean esRestaurable(String nombre) {
        return nombre != null && (nombre.equals("mensual.sql") || nombre.equals("semanal.sql")
                || nombre.equals(ANTES_DE_RESTAURAR)
                || nombre.matches("^parcial_[A-Za-z0-9_-]+\\.sql$"));
    }

    /**
     * Restaura la base de datos desde un archivo de la carpeta de respaldos.
     * <ul>
     *   <li>mensual.sql, semanal.sql y antes-de-restaurar.sql son copias
     *       completas: se aplican solas.</li>
     *   <li>parcial_&lt;dia&gt;.sql solo trae las tablas que cambiaron: se
     *       aplica semanal.sql y encima ese parcial (= estado de ese dia).</li>
     * </ul>
     * Antes de tocar la base se guarda el estado actual en
     * antes-de-restaurar.sql (salvo que se este restaurando justo ese
     * archivo, para no perder el punto de "deshacer").
     *
     * @return true si salio bien; el detalle queda en getUltimoMensaje()
     * @throws IllegalArgumentException si el archivo no existe o no se puede restaurar
     */
    public boolean restaurar(String nombre) {
        Path archivo = archivoParaDescargar(nombre);   // valida el nombre y que exista
        if (!esRestaurable(nombre)) {
            throw new IllegalArgumentException("Ese archivo no se puede restaurar.");
        }
        boolean esParcial = nombre.startsWith("parcial_");
        Path semanal = archivo.resolveSibling("semanal.sql");
        if (esParcial && !Files.isRegularFile(semanal)) {
            throw new IllegalArgumentException("Para restaurar un parcial hace falta semanal.sql y no está en la carpeta.");
        }

        return conCandado(opciones -> {
            Path carpeta = archivo.getParent();
            boolean guardoPrevio = false;
            if (!nombre.equals(ANTES_DE_RESTAURAR)) {
                Path temporal = carpeta.resolve("antes.tmp");
                try {
                    volcarCompleto(opciones, temporal);
                    publicar(temporal, carpeta.resolve(ANTES_DE_RESTAURAR));
                    guardoPrevio = true;
                } finally {
                    borrarSilencioso(temporal);
                }
            }
            if (esParcial) {
                aplicar(opciones, semanal, false);
                aplicar(opciones, archivo, true);
            } else {
                aplicar(opciones, archivo, false);
            }
            log.warn("Base de datos restaurada desde {}", nombre);
            return "Base de datos restaurada desde " + (esParcial ? "semanal.sql + " : "") + nombre + "."
                    + (guardoPrevio ? " El estado anterior quedó en " + ANTES_DE_RESTAURAR + " por si hay que deshacerlo." : "");
        });
    }

    /**
     * Ejecuta un .sql con el cliente mysql. Las copias completas traen su
     * propio "USE"; los parciales no, por eso a ellos se les indica la base.
     */
    private void aplicar(Path opciones, Path archivo, boolean indicarBase) throws IOException {
        List<String> comando = new ArrayList<>(List.of(rutaMysql, "--defaults-extra-file=" + opciones,
                "--default-character-set=utf8mb4"));
        if (indicarBase) comando.add(baseDatos);
        ejecutar(comando, archivo);
    }

    /** Opciones del selector de restauracion, con una descripcion clara de cada una. */
    public List<OpcionRestauracion> listarRestaurables() {
        List<ArchivoRespaldo> archivos = listarArchivos();
        boolean haySemanal = archivos.stream().anyMatch(a -> a.nombre().equals("semanal.sql"));
        List<OpcionRestauracion> opciones = new ArrayList<>();
        for (ArchivoRespaldo a : archivos) {
            String n = a.nombre();
            if (!esRestaurable(n) || (n.startsWith("parcial_") && !haySemanal)) continue;
            String fecha = a.modificado().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            String descripcion;
            if (n.equals(ANTES_DE_RESTAURAR)) {
                descripcion = "Deshacer la última restauración (estado del " + fecha + ")";
            } else if (n.startsWith("parcial_")) {
                descripcion = tipoDe(n).replace("Parcial del", "Estado del") + " — semanal.sql + " + n + " (" + fecha + ")";
            } else {
                descripcion = tipoDe(n) + " — " + n + " (" + fecha + ")";
            }
            opciones.add(new OpcionRestauracion(n, descripcion));
        }
        return opciones;
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
                // "localhost" haria que mysqldump use un socket local en Mac/Linux
                // (que no existe con MySQL en Docker); 127.0.0.1 fuerza TCP.
                + "host=\"" + escapar("localhost".equalsIgnoreCase(host) ? "127.0.0.1" : host) + "\"\n"
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

    // ------------------------------------------------------------------
    // Para la pantalla del administrador
    // ------------------------------------------------------------------

    /** Archivos .sql de la carpeta de respaldos, del mas reciente al mas viejo. */
    public List<ArchivoRespaldo> listarArchivos() {
        Path carpeta = Paths.get(directorio).toAbsolutePath();
        if (!Files.isDirectory(carpeta)) return List.of();
        try (Stream<Path> archivos = Files.list(carpeta)) {
            return archivos
                    .filter(p -> p.getFileName().toString().endsWith(".sql"))
                    .map(p -> {
                        try {
                            return new ArchivoRespaldo(p.getFileName().toString(),
                                    tipoDe(p.getFileName().toString()), Files.size(p),
                                    LocalDateTime.ofInstant(Files.getLastModifiedTime(p).toInstant(), ZoneId.systemDefault()));
                        } catch (IOException e) {
                            return null;
                        }
                    })
                    .filter(a -> a != null)
                    .sorted(Comparator.comparing(ArchivoRespaldo::modificado).reversed())
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    /**
     * Devuelve la ruta de un archivo de respaldo para descargarlo.
     * Solo acepta nombres simples que esten DENTRO de la carpeta de
     * respaldos (evita que alguien pida "../../algo" por la URL).
     */
    public Path archivoParaDescargar(String nombre) {
        if (nombre == null || !nombre.matches("^[A-Za-z0-9_-]+\\.sql$")) {
            throw new IllegalArgumentException("Archivo no válido.");
        }
        Path carpeta = Paths.get(directorio).toAbsolutePath().normalize();
        Path archivo = carpeta.resolve(nombre).normalize();
        if (!archivo.startsWith(carpeta) || !Files.isRegularFile(archivo)) {
            throw new IllegalArgumentException("El archivo no existe.");
        }
        return archivo;
    }

    private static String tipoDe(String nombre) {
        if (nombre.equals("mensual.sql")) return "Total (para el ente externo)";
        if (nombre.equals("semanal.sql")) return "Completa semanal";
        if (nombre.equals(ANTES_DE_RESTAURAR)) return "Estado previo a la última restauración";
        if (nombre.startsWith("parcial_")) {
            String dia = nombre.substring(nombre.indexOf('-') + 1, nombre.length() - 4);
            return "Parcial del " + dia;
        }
        return "Otro";
    }

    // ------------------------------------------------------------------
    // Configuracion editable desde la pantalla del administrador
    // ------------------------------------------------------------------

    private Path archivoConfiguracion() {
        return Paths.get(directorio).toAbsolutePath().resolve("configuracion.properties");
    }

    /** Si el admin ya configuro algo desde la pantalla, eso manda sobre application.properties. */
    private void cargarConfiguracionGuardada() {
        Path archivo = archivoConfiguracion();
        if (!Files.isRegularFile(archivo)) return;
        try {
            Properties p = cargarManifiesto(archivo);
            activo = Boolean.parseBoolean(p.getProperty("activo", String.valueOf(activo)));
            cron = p.getProperty("cron", cron);
            directorioCopia = p.getProperty("directorio-copia", directorioCopia);
            log.info("Configuración de respaldos cargada de {}", archivo);
        } catch (IOException e) {
            log.warn("No se pudo leer {}: {}", archivo, e.getMessage());
        }
    }

    /**
     * Guarda la configuracion elegida en la pantalla y reprograma la
     * copia automatica al instante (sin reiniciar la app).
     *
     * @param hora           "HH:mm", la copia se hace todos los dias a esa hora
     * @param carpetaCopia   segunda carpeta opcional; vacio = sin segunda copia
     */
    public synchronized void guardarConfiguracion(boolean nuevoActivo, LocalTime hora, String carpetaCopia) throws IOException {
        String copia = carpetaCopia == null ? "" : carpetaCopia.trim();
        if (!copia.isEmpty()) {
            Path destino = Paths.get(copia).toAbsolutePath().normalize();
            try {
                Files.createDirectories(destino);
            } catch (IOException | SecurityException e) {
                throw new IllegalArgumentException("No se pudo crear la carpeta de la segunda copia: " + destino);
            }
            if (!Files.isWritable(destino)) {
                throw new IllegalArgumentException("No hay permiso para escribir en: " + destino);
            }
            if (destino.equals(Paths.get(directorio).toAbsolutePath().normalize())) {
                throw new IllegalArgumentException("La segunda copia debe ir en una carpeta distinta a la principal.");
            }
            copia = destino.toString();
        }
        activo = nuevoActivo;
        cron = String.format("0 %d %d * * *", hora.getMinute(), hora.getHour());
        directorioCopia = copia;

        Properties p = new Properties();
        p.setProperty("activo", String.valueOf(activo));
        p.setProperty("cron", cron);
        p.setProperty("directorio-copia", directorioCopia);
        Path archivo = archivoConfiguracion();
        Files.createDirectories(archivo.getParent());
        try (Writer w = Files.newBufferedWriter(archivo, StandardCharsets.UTF_8)) {
            p.store(w, "Configuracion de copias de seguridad (se edita desde la app: Administracion > Copias de seguridad)");
        }
        programar();
    }

    /** Hora de la copia automatica, si el horario es diario simple (para el formulario). */
    public LocalTime getHoraAutomatica() {
        String[] p = cron.trim().split("\\s+");
        if (p.length == 6 && p[1].matches("\\d+") && p[2].matches("\\d+")) {
            return LocalTime.of(Integer.parseInt(p[2]), Integer.parseInt(p[1]));
        }
        return LocalTime.of(19, 0);
    }

    public boolean isActivo() { return activo; }
    public String getCron() { return cron; }
    public String getDirectorioAbsoluto() { return Paths.get(directorio).toAbsolutePath().normalize().toString(); }
    public String getDirectorioCopia() { return directorioCopia; }
    public String getRutaMysqldump() { return rutaMysqldump; }
    public LocalDateTime getUltimaEjecucion() { return ultimaEjecucion; }
    public boolean isUltimaExitosa() { return ultimaExitosa; }
    public String getUltimoMensaje() { return ultimoMensaje; }

    /** "0 0 19 * * *" -> "Todos los días a las 19:00" (para los horarios diarios simples). */
    public String getHorarioLegible() {
        String[] p = cron.trim().split("\\s+");
        if (p.length == 6 && p[3].equals("*") && p[4].equals("*") && p[5].equals("*")
                && p[1].matches("\\d+") && p[2].matches("\\d+")) {
            return String.format("Todos los días a las %02d:%02d", Integer.parseInt(p[2]), Integer.parseInt(p[1]));
        }
        return "Según la expresión cron: " + cron;
    }

    private boolean hayCopiaDeHoy() {
        Path carpeta = Paths.get(directorio).toAbsolutePath();
        if (!Files.isDirectory(carpeta)) return false;
        LocalDate hoy = LocalDate.now();
        try (Stream<Path> archivos = Files.list(carpeta)) {
            return archivos.filter(p -> p.getFileName().toString().endsWith(".sql"))
                    .filter(p -> !p.getFileName().toString().equals(ANTES_DE_RESTAURAR))
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