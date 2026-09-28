package com.universoempaques.service;

import com.universoempaques.dto.DatosColombia;
import com.universoempaques.dto.RegistrarUsuarioForm;
import com.universoempaques.model.Area;
import com.universoempaques.model.Rol;
import com.universoempaques.model.Usuario;
import com.universoempaques.repository.AreaRepository;
import com.universoempaques.repository.ClienteRepository;
import com.universoempaques.repository.CotizacionRepository;
import com.universoempaques.repository.DisenoRepository;
import com.universoempaques.repository.EstadoPedidoRepository;
import com.universoempaques.repository.PedidoRepository;
import com.universoempaques.repository.RolRepository;
import com.universoempaques.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Logica de trabajadores internos (RF-03 y RF-04).
 * El rol y el area ya no son columnas del Usuario: se guardan en las
 * tablas puente UsuarioRol y UsuarioArea (ver Usuario.asignarRolPrincipal).
 */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final AreaRepository areaRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final PedidoRepository pedidoRepository;
    private final EstadoPedidoRepository estadoPedidoRepository;
    private final CotizacionRepository cotizacionRepository;
    private final DisenoRepository disenoRepository;

    public UsuarioService(UsuarioRepository usuarioRepository, ClienteRepository clienteRepository,
                          AreaRepository areaRepository, RolRepository rolRepository,
                          PasswordEncoder passwordEncoder, PedidoRepository pedidoRepository,
                          EstadoPedidoRepository estadoPedidoRepository,
                          CotizacionRepository cotizacionRepository, DisenoRepository disenoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.areaRepository = areaRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.pedidoRepository = pedidoRepository;
        this.estadoPedidoRepository = estadoPedidoRepository;
        this.cotizacionRepository = cotizacionRepository;
        this.disenoRepository = disenoRepository;
    }

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAllByOrderByNombreAsc();
    }

    public Usuario buscarPorId(Integer codigo) {
        return usuarioRepository.findById(codigo)
                .orElseThrow(() -> new IllegalArgumentException("El usuario no existe."));
    }

    public List<Area> listarAreas() {
        return areaRepository.findAll();
    }

    public List<Rol> listarRoles() {
        return rolRepository.findAll();
    }

    /**
     * RF-03 (registrar trabajador) y RF-04 (asignar permisos): un mismo
     * formulario cubre ambos, porque el rol + area es lo que define los
     * permisos (ver AppUserPrincipal).
     *
     * @Transactional: el usuario queda "administrado" por Hibernate
     * mientras se cambian sus tablas puente, y todo se guarda junto
     * (o nada, si algo falla).
     */
    @Transactional
    public Usuario guardar(RegistrarUsuarioForm form) {
        return guardar(form, null);
    }

    /**
     * @param quienEdita codigo del administrador que esta usando el
     *                   formulario; se usa para que no se quite a si
     *                   mismo el rol de Administrador y quede sin acceso.
     */
    @Transactional
    public Usuario guardar(RegistrarUsuarioForm form, Integer quienEdita) {
        String correo = form.getCorreo().trim().toLowerCase();
        Usuario usuario;

        if (form.getCodigo() != null) {
            usuario = buscarPorId(form.getCodigo());
            if (!correo.equalsIgnoreCase(usuario.getCorreo())) {
                validarCorreoLibre(correo);
            }
        } else {
            validarCorreoLibre(correo);
            usuario = new Usuario();
        }

        String documento = vacioANull(form.getNumDocumento());
        if (documento != null) {
            usuarioRepository.findByNumDocumento(documento)
                    .filter(otro -> !otro.getCodigo().equals(form.getCodigo()))
                    .ifPresent(otro -> {
                        throw new IllegalArgumentException("Ya existe otro usuario con ese número de documento.");
                    });
        }
        if (form.getFechaIngreso() != null && form.getFecVencimientoContrato() != null
                && form.getFecVencimientoContrato().isBefore(form.getFechaIngreso())) {
            throw new IllegalArgumentException("El vencimiento del contrato no puede ser anterior a la fecha de ingreso.");
        }

        usuario.setNombre(form.getNombre().trim());
        usuario.setCorreo(correo);
        usuario.setNumDocumento(documento);
        usuario.setTelefonoPersonal(DatosColombia.soloDigitos(form.getTelefonoPersonal()));
        usuario.setTelefonoEmpresa(DatosColombia.soloDigitos(form.getTelefonoEmpresa()));
        usuario.setFechaIngreso(form.getFechaIngreso());
        usuario.setFecVencimientoContrato(form.getFecVencimientoContrato());
        usuario.setCantHorasTrabajadas(form.getCantHorasTrabajadas());
        usuario.setNomina(form.getNomina());

        if (form.getContrasena() != null && !form.getContrasena().isBlank()) {
            usuario.setContrasena(passwordEncoder.encode(form.getContrasena()));
        } else if (usuario.getContrasena() == null) {
            throw new IllegalArgumentException("Debes definir una contrasena para el nuevo usuario.");
        }

        Rol rol = rolRepository.findById(form.getCodigoRol())
                .orElseThrow(() -> new IllegalArgumentException("El rol seleccionado no existe."));
        if (quienEdita != null && quienEdita.equals(usuario.getCodigo())
                && usuario.esAdministrador() && !Rol.ADMINISTRADOR.equalsIgnoreCase(rol.getNombre())) {
            throw new IllegalArgumentException("No puedes quitarte a ti mismo el rol de Administrador.");
        }
        usuario.asignarRolPrincipal(rol);

        Area area = null;
        if (form.getCodigoArea() != null) {
            area = areaRepository.findById(form.getCodigoArea())
                    .orElseThrow(() -> new IllegalArgumentException("El area seleccionada no existe."));
        }
        if (area == null && !Rol.ADMINISTRADOR.equalsIgnoreCase(rol.getNombre())) {
            throw new IllegalArgumentException("Un Empleado debe tener un area (define a que panel entra).");
        }
        usuario.asignarAreaPrincipal(area);

        return usuarioRepository.save(usuario);
    }

    /**
     * Elimina un trabajador. Reglas:
     *   - Nadie se puede eliminar a si mismo.
     *   - Siempre debe quedar al menos un Administrador.
     *   - Si ya participo en pedidos, estados, cotizaciones o disenos NO
     *     se borra: esos registros lo referencian (llaves foraneas) y se
     *     perderia la trazabilidad de quien hizo que.
     * Sus filas en UsuarioRol y UsuarioArea se borran solas (cascade).
     */
    @Transactional
    public void eliminar(Integer codigo, Integer quienElimina) {
        Usuario usuario = buscarPorId(codigo);
        if (codigo.equals(quienElimina)) {
            throw new IllegalArgumentException("No puedes eliminar tu propia cuenta.");
        }
        if (usuario.esAdministrador()
                && usuarioRepository.findAll().stream().filter(Usuario::esAdministrador).count() <= 1) {
            throw new IllegalArgumentException("Debe quedar al menos un Administrador en el sistema.");
        }
        if (pedidoRepository.existsByUsuario(usuario) || estadoPedidoRepository.existsByUsuario(usuario)
                || cotizacionRepository.existsByUsuario(usuario) || disenoRepository.existsByUsuario(usuario)) {
            throw new IllegalArgumentException(usuario.getNombre()
                    + " ya tiene pedidos, cotizaciones o diseños a su nombre; no se puede eliminar sin perder ese historial.");
        }
        usuarioRepository.delete(usuario);
    }

    /** Un correo no puede repetirse ni entre usuarios ni entre clientes (login unico). */
    private void validarCorreoLibre(String correo) {
        if (usuarioRepository.existsByCorreo(correo) || clienteRepository.existsByCorreo(correo)) {
            throw new IllegalArgumentException("Ya existe una cuenta registrada con ese correo.");
        }
    }

    private static String vacioANull(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }
}
