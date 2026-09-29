# Bitácora del proyecto — Universo Empaques

Estado vivo del proyecto. **Léanla antes de empezar a trabajar** y agreguen
una entrada al final de cada sesión (en su propio commit).

## Estado de los requisitos

| RF | Descripción | Estado |
|----|-------------|--------|
| RF-01 | Inicio de sesión único con redirección por rol/área | Hecho (migrado a modelo v2) |
| RF-02 | Autoregistro de cliente (ahora con NIT) | Hecho (migrado a modelo v2) |
| RF-03 / RF-04 | Gestión de usuarios y permisos (Admin) | Hecho (migrado a modelo v2) |
| RF-05 | Gestión de clientes (Comercial) | Hecho (migrado a modelo v2) |
| RF-06 a RF-09 | Cotizaciones: cliente solicita, comercial pone valor, cliente aprueba/rechaza, ambos consultan | Hecho |
| RF-10 | Solicitar pedido | Hecho: desde una cotización aprobada (lo genera Comercial) o directo por el cliente desde "Mis pedidos" con productos del catálogo |
| RF-11 | Pedido directo con detalle y estado inicial | Hecho (guarda PrecioUnitario) |
| RF-12 / RF-13 | Diseño: cargar el archivo y aprobar/pedir ajustes | Hecho: Diseño sube versiones por producto (PNG/JPG/PDF ≤ 64 KB, logo, color RGB); Comercial aprueba o pide ajustes; sin todos aprobados no pasa a producción |
| RF-14 / RF-15 / RF-17 | Estado del pedido, consulta y despacho | Hecho: flujo SOLICITADO → EN_DISEÑO → EN_PRODUCCIÓN → TERMINADO → DESPACHADO → ENTREGADO; cola de trabajo por área; el cliente ve el avance en "Mis pedidos" |
| RF-16 | Notificaciones por correo (Observer) | Hecho: `EstadoPedidoCambiadoEvent` + `NotificacionService` (asíncrono). Sin correo configurado, deja el aviso en el log |
| RF-18 | Reportes | Hecho: pantalla del admin con rango de fechas: pedidos por estado, valor, cotizaciones y tasa de aprobación, tiempo promedio por etapa y hasta la entrega, productos más pedidos; descarga CSV para Excel |
| RF-19 | Copia de seguridad | Hecho (mensual.sql = copia TOTAL que se entrega al ente externo; se sobrescribe): `BackupService` (Amelie) + pantalla del admin: copia total manual (reemplaza mensual.sql), configuración de la copia automática (encendida, hora, segunda carpeta) sin reiniciar, estado, descarga y **restaurar** (con punto de deshacer `antes-de-restaurar.sql`) |

## Decisiones tomadas

- La fuente de verdad de la BD es `db/schema.sql` (script de Amelie con 2 errores de sintaxis corregidos).
- `ddl-auto=validate` + `PhysicalNamingStrategyStandardImpl`: los nombres de tabla/columna van en PascalCase exacto.
- Cada usuario tiene en la práctica UN rol y UN área principal (el primero asignado en `UsuarioRol` / `UsuarioArea`).
- En Java los atributos tienen nombres limpios; los nombres raros del script solo están en `@Column`.
- Configuración por computador en `application-local.properties` (ignorado por Git; plantilla en `application-local.properties.example`). `application.properties` es compartido y no lleva contraseñas ni rutas personales.
- `.gitattributes` unifica finales de línea (LF) entre Mac y Windows.
- Respaldos (RF-19) apagados por defecto; se activan solo en el computador servidor.
- Ramas: una por integrante (`Nombre-Area`). Pull Request hacia **`Development`**; ahí se prueba y Alejandra pasa `Development` → `main`.

## Preguntas pendientes para Amelie

1. `Usuario.NombreEmpresa` y `Cliente.NombreEmpresa`: el diccionario dice `Nombre`.
2. `Pedido.FechaRegistroTecnicas`: el diccionario dice `FechaRegistro`.
3. ~~`Cliente.Telefono` y `Usuario.TelefonoEmpresa` son INT~~ → **ya se cambiaron a VARCHAR(20)** en `schema.sql` (no cabía un celular de 10 dígitos). Amelie debe actualizar el diccionario de datos.
4. `Logo` y `ArchivoDiseno` son BLOB (máx. 64 KB): la app ya valida ese límite, pero es poco para un diseño real. Sugerencia: MEDIUMBLOB (16 MB).
7. `Diseno.CodigoUsuario` es uno solo: se guarda quien REVISÓ (como dice el diccionario); se pierde quién lo subió. ¿Agregar `CodigoUsuarioDisenador`?
5. `Cotizacion.CodigoPedido`: se usa así: la cotización nace sin pedido (NULL) y, al aprobarse, el comercial genera el pedido y se llena CodigoPedido. Confirmar con Amelie.
6. `Diseno` cuelga de `DetallePedido` (no de `Pedido`): confirmar que es intencional.

## Registro de sesiones

### 2026-09-28 — Juan Gamboa (Backend)
- Merge de `origin/main` en `Gamboa-Backend` (conflictos de `.idea/` resueltos con la versión local).
- Migración completa al modelo v2 de Amelie: entidades, repositorios, DTOs, services, `AppUserPrincipal`, `DataSeeder`, controllers y plantillas.
- Nuevas: `Cotizacion`, `EstadoCotizacionTipo`, `CotizacionRepository`.
- `.gitignore` nuevo; se sacan de Git `.idea/`, `target/`, `.DS_Store` y `respaldos/`.
- README actualizado (cómo crear la BD en Windows y en Mac).
- Validaciones de datos colombianos centralizadas en `dto/DatosColombia.java`: celular (10 dígitos, empieza por 3), teléfono fijo nuevo (60X + 7 dígitos), NIT, cédula, nombres, correo con dominio, contraseña (mín. 8, letras y números).
- `controller/FormulariosAdvice.java`: quita espacios sobrantes de todos los formularios (menos contraseñas).
- `Cliente.Telefono` y `Usuario.TelefonoEmpresa` pasan a VARCHAR(20); los teléfonos se guardan solo con dígitos.
- Validaciones extra: documento de usuario único, vencimiento de contrato ≥ ingreso, fecha de entrega del pedido ≥ hoy, cantidad máx. 1.000.000.
- `server.address=0.0.0.0` para abrir la app desde el celular en la misma red.
- Logo oficial en `static/img/` (`logo-blanco.png` para la barra negra, `logo-negro.png` para login/registro, `favicon.png` y `apple-touch-icon.png`), puesto en todas las vistas.

### 2026-09-28 (tarde) — Juan Gamboa (Backend)
- RF-06 a RF-10: módulo de cotizaciones (`CotizacionService`, `ClienteCotizacionController`, `ComercialCotizacionController`, 5 vistas nuevas). Flujo: SOLICITADA → COTIZADA → APROBADA/RECHAZADA → pedido generado.
- Panel del cliente con contadores reales y acceso a "Mis cotizaciones"; menú del comercial con "Cotizaciones".
- El menú lateral se acomoda en celular (pasa arriba, en fila).
- Primeras pruebas unitarias: `DatosColombiaTest` y `CotizacionServiceTest` (src/test).
- Configuración portable: `application-local.properties` personal (ignorado por Git), `.gitattributes`, respaldos apagados por defecto, puerto y datos de MySQL configurables.
- RF-19: interfaz del administrador para las copias de seguridad de Amelie (`AdminRespaldoController`, `admin-respaldos.html`): estado, horario, última ejecución, botón "Hacer copia ahora", lista y descarga de archivos. `BackupService` ahora guarda el resultado de la última copia y usa 127.0.0.1 en vez de localhost (necesario con MySQL en Docker).
- Usuarios de prueba (uno por tipo, contraseña `prueba123`) creados por `DataSeeder` cuando `app.datos-prueba=true`; lista en el README.
- Revisión general de diseño y funcionalidad:
  - Plantilla común `templates/fragments/layout.html` (head, barra superior, menú lateral por rol, mensajes). Todas las vistas internas la usan: el menú ya no cambia de tamaño entre páginas y se cambia en un solo lugar.
  - La barra superior muestra el cargo y el nombre reales de quien inició sesión; el logo lleva a `/mi-panel` (el inicio de cada rol). El admin ve en su menú las secciones de Administración, Comercial y las demás áreas.
  - Paneles con datos reales (admin, comercial, cliente); diseño/producción/bodega marcados "en construcción".
  - Página de error propia (`error.html`) para 403/404/500 en vez de la "Whitelabel".
  - Login avisa si la cuenta del cliente está inactiva.
  - Buscador rápido en las tablas (`static/js/filtro-tabla.js`).
  - Pedidos: quitar productos (solo en estado SOLICITADO), estados con etiqueta y color, el formulario de productos se oculta cuando el pedido avanzó.
  - Un administrador no puede quitarse a sí mismo el rol de Administrador.
- Admin puede eliminar usuarios (no a sí mismo, siempre queda un administrador, y no se borra quien ya tiene pedidos/cotizaciones/diseños para no perder el historial). Las áreas se muestran con tilde (Diseño, Producción).

### 2026-09-28 (noche) — Juan Gamboa (Backend)
- RF-14/RF-15/RF-17: flujo de estados del pedido con **patrón State** en `EstadoPedidoTipo` (siguiente estado, área responsable y texto del botón). `PedidoService.avanzarEstado` cierra la etapa actual y abre la siguiente con usuario y observación.
- Cola de trabajo por área: `/diseno/pedidos`, `/produccion/pedidos`, `/bodega/pedidos` (`AreaPedidoController`), con detalle, barra de progreso e historial. Comercial envía a diseño desde el detalle del pedido.
- Cliente: "Mis pedidos" con barra de progreso e historial (`ClientePedidoController`).
- RF-16 con **patrón Observer** (eventos de Spring): `EstadoPedidoCambiadoEvent` → `NotificacionService` envía el correo al cliente después del commit y en segundo plano (`@EnableAsync`).
- Prueba unitaria `EstadoPedidoTipoTest`.
- RF-19 (pedido de Amelie): "Hacer copia total ahora" hace una copia completa que reemplaza `mensual.sql` y no toca la semanal ni las parciales. La copia automática se configura desde la pantalla (encendida/apagada, hora, segunda carpeta) y se guarda en `respaldos/configuracion.properties` de cada equipo; se reprograma sin reiniciar (`TaskScheduler`).
- RF-12/RF-13: `DisenoService`, `DisenoController`, fragmento `disenos` en las pantallas de Diseño, Comercial y Cliente. Validación del archivo por su firma (no por la extensión). Prueba `DisenoServiceTest`.
- RF-18: `ReporteService` + `AdminReporteController` + `admin-reportes.html`. Prueba `ReporteServiceTest`.
- RF-10: el cliente solicita un pedido directo ("Mis pedidos" → "+ Solicitar pedido"). Queda SOLICITADO sin comercial (`CodigoUsuario` NULL, marcado "Web"); quien lo envía a diseño queda a cargo.
- Copias: `mensual.sql` se presenta como la copia **Total** (la que se entrega al ente externo); no se guarda histórico.

### 2026-09-29 — Juan Gamboa (Backend)
- PR #1 (`Gamboa-Backend` → `main`) fusionado. Desde ahora los PR van a `Development`.
- RF-19 completo: **restaurar la base desde la app** (Administración → Copias de seguridad → "Restaurar la base de datos").
  - Se elige la copia (total, semanal, el estado de un día = semanal + parcial, o deshacer) y se confirma escribiendo RESTAURAR.
  - Antes de restaurar se guarda el estado actual en `antes-de-restaurar.sql` (aparece en la lista para deshacer).
  - Al terminar se cierra la sesión y el login avisa que la base se restauró.
  - Usa `backup.ruta-mysql` (el cliente `mysql`), igual que las copias usan `mysqldump`.
- Prueba `BackupServiceTest`.
- **Mi cuenta** (`/cuenta`, en el menú de todos y al hacer clic en el nombre de la barra superior):
  - Todos cambian su contraseña (piden la actual; la nueva con la misma regla del registro y distinta a la actual).
  - El cliente actualiza correo, celular, teléfono y dirección (NIT, nombre y razón social solo los cambia Comercial). Si cambia el correo, se cierra la sesión y entra con el nuevo.
  - `CuentaService`, `MiCuentaController`, `CambiarContrasenaForm`, `MisDatosClienteForm`, vista `cuenta.html`. Prueba `CuentaServiceTest`.
- Logo centrado en login y registro.
- **Mejoras de interfaz:**
  - Iconos propios en SVG (`static/img/iconos.svg`, sin depender de internet): menú lateral, tarjetas de los paneles y botones "+".
  - Barra superior con círculo de iniciales (lleva a Mi cuenta) y botón Salir con icono; barra y menú fijos al hacer scroll.
  - Tablas con encabezados discretos y resaltado al pasar el mouse; login y registro con fondo de marca.
  - Registro: ayudas que explican NIT/cédula, nombre comercial y razón social (una cuenta por empresa o persona).
  - Landing renovada (misma identidad): portada con ejemplo del seguimiento, "Así funciona" en 4 pasos, catálogo real de productos (tabla Producto), beneficios, llamado a la acción y pie de página.
- **Datos de ejemplo** (`config/DatosDeEjemplo.java`, lo llama `DataSeeder` si `app.datos-prueba=true` y la base no tiene pedidos ni cotizaciones): 4 clientes más (uno inactivo), 2 productos más, cotizaciones en los 4 estados, 7 pedidos en los 6 estados con historial de las últimas semanas y diseños en sus 3 estados (imágenes PNG generadas). Lista en el README.
- **Catálogo de productos** (Administración → Productos, `/admin/productos`): agregar, editar y eliminar productos (nombre, material, forma, tamaño "30x20x15 cm" y precio en pesos sin puntos). Un producto usado en pedidos no se elimina (se muestra "En uso"); al cambiar el precio los pedidos anteriores conservan el suyo. `ProductoService`, `AdminProductoController`, `ProductoForm`, 2 vistas. Prueba `ProductoServiceTest`.
- Pruebas automáticas corridas en IntelliJ: **24 de 24 pasan**.
- Efecto de zoom suave al pasar el mouse por botones, tarjetas del panel, menú lateral y productos de la landing (se desactiva si el sistema pide "reducir movimiento").
- `admin@universoempaques.com` ya no se crea con datos de prueba y, si existe, se elimina al arrancar (se usa `admin@prueba.com`). Solo se crea en producción (`app.datos-prueba=false`) con la base vacía. README actualizado.
- Decisión: la demo se hace **con los datos de ejemplo**. La rama `Daniel-Backend` tiene una copia a mano de lo nuestro: **no se toca hasta que Daniel lo hable** (si hace merge de `Development` así, tendrá conflictos).

- Landing adaptada al celular: portada, pasos, productos, beneficios y llamado a la acción centrados; botones a lo ancho y barra superior en una sola línea.
