package com.universoempaques.config;

import com.universoempaques.model.Area;
import com.universoempaques.model.Producto;
import com.universoempaques.model.Rol;
import com.universoempaques.model.Usuario;
import com.universoempaques.repository.AreaRepository;
import com.universoempaques.repository.ProductoRepository;
import com.universoempaques.repository.RolRepository;
import com.universoempaques.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Al arrancar la aplicacion por primera vez (tabla usuario vacia),
 * crea el usuario Administrador inicial, las areas/roles base, y
 * un catalogo de productos de ejemplo para poder probar el modulo
 * de pedidos sin tener que cargarlos a mano.
 *
 * Credenciales iniciales:
 *   correo:     admin@universoempaques.com
 *   contrasena: admin123
 *
 * IMPORTANTE: cambien esta contrasena una vez ingresen por primera vez.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final AreaRepository areaRepository;
    private final ProductoRepository productoRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UsuarioRepository usuarioRepository, RolRepository rolRepository,
                      AreaRepository areaRepository, ProductoRepository productoRepository,
                      PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.areaRepository = areaRepository;
        this.productoRepository = productoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() == 0) {
            sembrarUsuarioAdministrador();
        }
        if (productoRepository.count() == 0) {
            sembrarProductosDeEjemplo();
        }
    }

    private void sembrarUsuarioAdministrador() {
        Rol rolAdmin = rolRepository.findByNombre(Rol.ADMINISTRADOR)
                .orElseGet(() -> rolRepository.save(new Rol(Rol.ADMINISTRADOR)));
        rolRepository.findByNombre(Rol.EMPLEADO)
                .orElseGet(() -> rolRepository.save(new Rol(Rol.EMPLEADO)));

        if (areaRepository.count() == 0) {
            areaRepository.save(new Area("Comercial", "Planta Universo Empaques - Bucaramanga"));
            areaRepository.save(new Area("Diseno", "Planta Universo Empaques - Bucaramanga"));
            areaRepository.save(new Area("Produccion", "Planta Universo Empaques - Bucaramanga"));
            areaRepository.save(new Area("Bodega", "Planta Universo Empaques - Bucaramanga"));
        }

        Usuario admin = new Usuario();
        admin.setNombre("Administrador");
        admin.setCorreo("admin@universoempaques.com");
        admin.setContrasena(passwordEncoder.encode("admin123"));
        admin.setRol(rolAdmin);
        usuarioRepository.save(admin);

        System.out.println("========================================================");
        System.out.println(" Usuario administrador creado.");
        System.out.println(" Correo:     admin@universoempaques.com");
        System.out.println(" Contrasena: admin123  (cambienla despues de ingresar)");
        System.out.println("========================================================");
    }

    private void sembrarProductosDeEjemplo() {
        productoRepository.save(crearProducto("Caja Kraft mediana", "Cartón Kraft", "3500"));
        productoRepository.save(crearProducto("Bolsa personalizada", "Papel Kraft", "1200"));
        productoRepository.save(crearProducto("Caja para repostería", "Cartón microcorrugado", "2800"));
        productoRepository.save(crearProducto("Empaque para accesorios", "Cartón rígido", "4200"));

        System.out.println("========================================================");
        System.out.println(" Catalogo de productos de ejemplo creado (4 productos).");
        System.out.println("========================================================");
    }

    private Producto crearProducto(String nombre, String material, String precio) {
        Producto producto = new Producto();
        producto.setNombre(nombre);
        producto.setMaterial(material);
        producto.setPrecio(new BigDecimal(precio));
        return producto;
    }
}