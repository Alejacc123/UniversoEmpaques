package com.universoempaques.config;

import com.universoempaques.model.Area;
import com.universoempaques.model.Cliente;
import com.universoempaques.model.Producto;
import com.universoempaques.model.Rol;
import com.universoempaques.model.Usuario;
import com.universoempaques.repository.AreaRepository;
import com.universoempaques.repository.ClienteRepository;
import com.universoempaques.repository.ProductoRepository;
import com.universoempaques.repository.RolRepository;
import com.universoempaques.repository.UsuarioRepository;
import com.universoempaques.service.UsuarioService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Datos iniciales. Se ejecuta cada vez que arranca la app, pero solo
 * inserta lo que falte (si la tabla ya tiene datos, no hace nada):
 *   - Roles: Administrador y Empleado.
 *   - Areas: Comercial, Diseno, Produccion, Bodega.
 *   - Usuario Administrador inicial, SOLO si la base esta vacia y
 *     app.datos-prueba=false (con datos de prueba se usa admin@prueba.com).
 *   - 4 productos de ejemplo.
 *
 * Credenciales iniciales (solo computador de produccion, sin datos de prueba):
 *   correo:     admin@universoempaques.com
 *   contrasena: admin123
 *
 * Como ahora usamos ddl-auto=validate, las TABLAS deben existir antes
 * (se crean con db/schema.sql). Este seeder solo inserta FILAS.
 *
 * USUARIOS DE PRUEBA (solo si app.datos-prueba=true): un usuario por
 * cada tipo, todos con la contrasena CONTRASENA_PRUEBA. En cada arranque
 * se aseguran de existir y se les restablece la contrasena, para que la
 * lista del README siempre funcione. En el computador de produccion se
 * pone app.datos-prueba=false.
 *
 * Ademas, si la base esta recien creada (sin pedidos ni cotizaciones),
 * DatosDeEjemplo llena clientes, cotizaciones, pedidos y disenos de
 * ejemplo en todos sus estados.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final String DIRECCION_PLANTA = "Planta Universo Empaques - Bucaramanga";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final AreaRepository areaRepository;
    private final ProductoRepository productoRepository;
    private final PasswordEncoder passwordEncoder;
    private final ClienteRepository clienteRepository;
    private final DatosDeEjemplo datosDeEjemplo;
    private final UsuarioService usuarioService;

    /** Crea/restablece los usuarios de prueba (ver application.properties). */
    @Value("${app.datos-prueba:false}")
    private boolean datosPrueba;

    public static final String CONTRASENA_PRUEBA = "prueba123";

    /** Administrador inicial, solo para una base vacia SIN datos de prueba (produccion). */
    static final String CORREO_ADMIN_INICIAL = "admin@universoempaques.com";

    public DataSeeder(UsuarioRepository usuarioRepository, RolRepository rolRepository,
                      AreaRepository areaRepository, ProductoRepository productoRepository,
                      PasswordEncoder passwordEncoder, ClienteRepository clienteRepository,
                      DatosDeEjemplo datosDeEjemplo, UsuarioService usuarioService) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.areaRepository = areaRepository;
        this.productoRepository = productoRepository;
        this.passwordEncoder = passwordEncoder;
        this.clienteRepository = clienteRepository;
        this.datosDeEjemplo = datosDeEjemplo;
        this.usuarioService = usuarioService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        sembrarRoles();
        sembrarAreas();
        // Base vacia y SIN datos de prueba (computador de produccion): se crea
        // el administrador inicial para poder entrar. Con datos de prueba se usa admin@prueba.com.
        if (usuarioRepository.count() == 0 && !datosPrueba) {
            sembrarUsuarioAdministrador();
        }
        if (productoRepository.count() == 0) {
            sembrarProductosDeEjemplo();
        }
        if (datosPrueba) {
            sembrarUsuariosDePrueba();
            quitarAdministradorInicial();
            // Clientes, cotizaciones, pedidos, historial y disenos de ejemplo (solo en una base nueva)
            datosDeEjemplo.sembrarSiHaceFalta();
        }
    }

    // ------------------------------------------------------------------
    // Usuarios de prueba: todos con contrasena "prueba123"
    // ------------------------------------------------------------------

    private void sembrarUsuariosDePrueba() {
        empleadoDePrueba("admin@prueba.com", "Admin Prueba", Rol.ADMINISTRADOR, null);
        empleadoDePrueba("comercial@prueba.com", "Comercial Prueba", Rol.EMPLEADO, "Comercial");
        empleadoDePrueba("diseno@prueba.com", "Diseño Prueba", Rol.EMPLEADO, "Diseno");
        empleadoDePrueba("produccion@prueba.com", "Producción Prueba", Rol.EMPLEADO, "Produccion");
        empleadoDePrueba("bodega@prueba.com", "Bodega Prueba", Rol.EMPLEADO, "Bodega");
        clienteDePrueba();

        System.out.println("========================================================");
        System.out.println(" Usuarios de prueba listos (contrasena: " + CONTRASENA_PRUEBA + ")");
        System.out.println("   admin@prueba.com, comercial@prueba.com, diseno@prueba.com,");
        System.out.println("   produccion@prueba.com, bodega@prueba.com, cliente@prueba.com");
        System.out.println("========================================================");
    }

    private void empleadoDePrueba(String correo, String nombre, String nombreRol, String nombreArea) {
        Usuario usuario = usuarioRepository.findByCorreo(correo).orElseGet(Usuario::new);
        usuario.setCorreo(correo);
        usuario.setNombre(nombre);
        if (usuario.getContrasena() == null || !passwordEncoder.matches(CONTRASENA_PRUEBA, usuario.getContrasena())) {
            usuario.setContrasena(passwordEncoder.encode(CONTRASENA_PRUEBA));
        }
        usuario.asignarRolPrincipal(rolRepository.findByNombre(nombreRol).orElseThrow());
        usuario.asignarAreaPrincipal(nombreArea == null ? null : areaRepository.findByNombre(nombreArea).orElseThrow());
        usuarioRepository.save(usuario);
    }

    private void clienteDePrueba() {
        Cliente cliente = clienteRepository.findByCorreo("cliente@prueba.com").orElse(null);
        if (cliente == null) {
            cliente = new Cliente();
            cliente.setNit("900000001-1");
            cliente.setFechaRegistro(LocalDate.now());
        }
        cliente.setCorreo("cliente@prueba.com");
        cliente.setNombre("Cliente Prueba S.A.S.");
        cliente.setRazonSocial("Cliente Prueba S.A.S.");
        cliente.setCelular("3001234567");
        cliente.setDireccion("Calle 1 # 2-3, Bucaramanga");
        cliente.setEstadoCliente(Cliente.ESTADO_ACTIVO);
        if (cliente.getContrasena() == null || !passwordEncoder.matches(CONTRASENA_PRUEBA, cliente.getContrasena())) {
            cliente.setContrasena(passwordEncoder.encode(CONTRASENA_PRUEBA));
        }
        clienteRepository.save(cliente);
    }

    /**
     * Con datos de prueba el administrador es admin@prueba.com; la cuenta
     * inicial admin@universoempaques.com sobra y se elimina (si no tiene
     * historial a su nombre).
     */
    private void quitarAdministradorInicial() {
        usuarioRepository.findByCorreo(CORREO_ADMIN_INICIAL).ifPresent(admin -> {
            try {
                usuarioService.eliminar(admin.getCodigo(), null);
                System.out.println(" Se eliminó la cuenta " + CORREO_ADMIN_INICIAL + " (se usa admin@prueba.com).");
            } catch (IllegalArgumentException e) {
                System.out.println(" No se eliminó " + CORREO_ADMIN_INICIAL + ": " + e.getMessage());
            }
        });
    }

    private void sembrarRoles() {
        if (rolRepository.findByNombre(Rol.ADMINISTRADOR).isEmpty()) {
            rolRepository.save(new Rol(Rol.ADMINISTRADOR,
                    "Gestiona usuarios y permisos, reportes y copias de seguridad; entra a todos los modulos"));
        }
        if (rolRepository.findByNombre(Rol.EMPLEADO).isEmpty()) {
            rolRepository.save(new Rol(Rol.EMPLEADO,
                    "Accede solo al modulo de su area"));
        }
    }

    private void sembrarAreas() {
        for (String nombre : new String[]{"Comercial", "Diseno", "Produccion", "Bodega"}) {
            if (areaRepository.findByNombre(nombre).isEmpty()) {
                areaRepository.save(new Area(nombre, DIRECCION_PLANTA));
            }
        }
    }

    private void sembrarUsuarioAdministrador() {
        Rol rolAdmin = rolRepository.findByNombre(Rol.ADMINISTRADOR).orElseThrow();

        Usuario admin = new Usuario();
        admin.setNombre("Administrador");
        admin.setCorreo(CORREO_ADMIN_INICIAL);
        admin.setContrasena(passwordEncoder.encode("admin123"));
        admin.setFechaIngreso(LocalDate.now());
        admin.asignarRolPrincipal(rolAdmin);   // crea la fila en UsuarioRol
        usuarioRepository.save(admin);

        System.out.println("========================================================");
        System.out.println(" Usuario administrador creado.");
        System.out.println(" Correo:     admin@universoempaques.com");
        System.out.println(" Contrasena: admin123  (cambienla despues de ingresar)");
        System.out.println("========================================================");
    }

    private void sembrarProductosDeEjemplo() {
        productoRepository.save(crearProducto("Caja Kraft mediana", "Cartón Kraft", "3500", "Rectangular", "30x20x15 cm"));
        productoRepository.save(crearProducto("Bolsa personalizada", "Papel Kraft", "1200", "Bolsa con manija", "25x32 cm"));
        productoRepository.save(crearProducto("Caja para repostería", "Cartón microcorrugado", "2800", "Cuadrada con visor", "20x20x10 cm"));
        productoRepository.save(crearProducto("Empaque para accesorios", "Cartón rígido", "4200", "Tapa y fondo", "12x12x5 cm"));

        System.out.println("========================================================");
        System.out.println(" Catalogo de productos de ejemplo creado (4 productos).");
        System.out.println("========================================================");
    }

    private Producto crearProducto(String nombre, String material, String precio, String forma, String tamano) {
        Producto producto = new Producto();
        producto.setNombre(nombre);
        producto.setMaterial(material);
        producto.setPrecio(new BigDecimal(precio));
        producto.setForma(forma);
        producto.setTamano(tamano);
        return producto;
    }
}
