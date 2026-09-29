# Sistema de Gestión de Repartos de Empaques — Universo Empaques

Proyecto de Ingeniería de Software (UPB Seccional Bucaramanga). Backend en
**Java + Spring Boot**, patrón **MVC**, base de datos **MySQL**.

Este es el **primer avance del código** — cubre el inicio de sesión, el
autoregistro de clientes, y la redirección a cada panel según el cargo
(RF-01 a RF-04, RNF-02, RNF-03). Los módulos de cotizaciones, pedidos,
diseño, producción, despacho, reportes y copia de seguridad se agregan en
las siguientes iteraciones.

## 1. Requisitos previos (instalar una sola vez)

1. **Java 17** — verifica con `java -version` en la terminal.
2. **Maven** — verifica con `mvn -version`. Si no lo tienes, IntelliJ/Eclipse
   ya lo traen incluido; no es obligatorio instalarlo aparte si usan un IDE.
3. **MySQL** (Workbench o el que ya tengan instalado), corriendo en el
   puerto por defecto (3306).
4. Un IDE: **IntelliJ IDEA** (recomendado para Spring Boot) o **VS Code**
   con la extensión "Extension Pack for Java" + "Spring Boot Extension Pack".

## 2. Configurar la base de datos

La aplicación **no crea tablas** (`ddl-auto=validate`): solo revisa al
arrancar que las entidades coincidan con la base de datos. La base se
crea con el script oficial de Amelie: `src/main/resources/db/schema.sql`.

⚠️ El script **borra** la base `universo_empaques` y la crea de nuevo.

**Windows (MySQL instalado normal):** MySQL Workbench → File → Open SQL
Script → `schema.sql` → botón del rayo ⚡. O desde la terminal:

```bash
mysql -u root -p < src/main/resources/db/schema.sql
```

**Mac con Docker (contenedor `mysql-bd`):**

```bash
docker start mysql-bd
docker exec -i mysql-bd mysql -uroot -puniverso123 < src/main/resources/db/schema.sql
```

**Configuración personal (contraseña de MySQL, puerto, correo...):**
`application.properties` es compartido y **no se edita** con datos de su
computador. Si su MySQL NO usa `root` / `universo123` en `localhost:3306`:

1. En la raíz del proyecto copien `application-local.properties.example`
   con el nombre `application-local.properties`.
2. Quiten el `#` de la línea que necesiten, ej: `db.contrasena=su_clave`.

Ese archivo lo ignora Git: cada quien tiene el suyo y nadie pisa la
configuración del otro al hacer `git pull`.

**No restauren respaldos de otro computador:** MySQL en Windows guarda los
nombres de tabla en minúscula y en Mac/Linux no. Cada uno crea su base
con `schema.sql`.

Si van a probar el envío de notificaciones por correo (RF-16), pongan
`spring.mail.username` y `spring.mail.password` en su
`application-local.properties` (con una "contraseña de aplicación" de
Gmail, no la contraseña normal de la cuenta). Mientras no lo configuren, el resto del sistema funciona
igual: cada notificación queda escrita en la consola como "[Notificación simulada]".

## 3. Ejecutar el proyecto

**Opción A — desde IntelliJ/VS Code:** abran la carpeta del proyecto,
esperen a que el IDE descargue las dependencias de Maven (barra de progreso
abajo), y ejecuten la clase `GestionRepartosEmpaquesApplication.java`
(botón ▶ verde).

**Opción B — desde la terminal**, parados en la carpeta del proyecto:

```bash
mvn spring-boot:run
```

La primera vez que arranca (con la base ya creada por `schema.sql`), la aplicación:
1. Inserta los roles, las áreas y 4 productos de ejemplo.
2. Con `app.datos-prueba=true` (lo normal para desarrollar y para la demo) crea los
   usuarios de prueba de la sección 4 y los datos de ejemplo; el administrador es
   `admin@prueba.com` / `prueba123`.
3. Solo en el computador de producción (`app.datos-prueba=false`) y con la base vacía,
   crea el administrador inicial `admin@universoempaques.com` / `admin123`
   (sale en la consola). **Cámbienla luego** desde "Mi cuenta".

Abran el navegador en **http://localhost:8080**

## 4. Usuarios de prueba

Con `app.datos-prueba=true` (valor por defecto), al arrancar se crean o
restablecen estos usuarios. **Todos usan la contraseña `prueba123`**:

| Tipo | Correo | Entra a |
|------|--------|---------|
| Administrador | `admin@prueba.com` | /admin/panel |
| Comercial | `comercial@prueba.com` | /comercial/panel |
| Diseño | `diseno@prueba.com` | /diseno/panel |
| Producción | `produccion@prueba.com` | /produccion/panel |
| Bodega | `bodega@prueba.com` | /bodega/panel |
| Cliente (NIT 900000001-1) | `cliente@prueba.com` | /cliente/panel |

Si cambian la contraseña de alguno desde la app, al reiniciar vuelve a
`prueba123`. En el computador de producción pongan `app.datos-prueba=false`
en su `application-local.properties`.

### Datos de ejemplo

Si la base está **recién creada** (acaban de correr `schema.sql`), al arrancar
también se llenan datos de ejemplo para no tener que crearlos a mano:

- **Clientes extra** (contraseña `prueba123`): `espiga@prueba.com` (Panadería La Espiga),
  `cafe@prueba.com` (Café Montaña Azul), `laura@prueba.com` (persona natural) e
  `inactivo@prueba.com` (cuenta INACTIVA: sirve para ver que no puede entrar).
- **Productos**: los 4 del catálogo + "Caja para pizza" y "Bolsa de papel para pan".
- **Cotizaciones** en los 4 estados: solicitada, cotizada (esperando al cliente),
  aprobada sin pedido y rechazada, más una aprobada que ya generó su pedido.
- **Pedidos** en los 6 estados (solicitado, en diseño, en producción, terminado,
  despachado y entregado), con historial, fechas de las últimas semanas (para los
  reportes) y diseños pendientes, aprobados y con ajuste solicitado.

Si la base ya tiene pedidos o cotizaciones no se agrega nada. Para volver a
tenerlos: corran `schema.sql` otra vez y reinicien la app.

## 4.1 Qué pueden probar ya mismo

- Entrar a `/` → página de inicio pública.
- Clic en "Registrarme" → crear una cuenta de cliente (RF-02) y quedar
  con acceso inmediato.
- Clic en "Iniciar sesión" → entrar como cliente recién creado, o como
  `admin@prueba.com` / `prueba123` → cada uno debe llegar a un
  panel distinto automáticamente (RF-01, RF-04).
- Si un cliente intenta entrar a una URL interna (ej. `/admin/panel`),
  Spring Security debe bloquearlo (RNF-03).

## 5. Estructura del proyecto (coincide con el diagrama de paquetes)

```
src/main/java/com/universoempaques/
 ├── controller/   -> recibe las peticiones web (RF -> URLs)
 ├── service/      -> logica de negocio (validaciones, reglas)
 ├── repository/   -> acceso a la base de datos (Spring Data JPA)
 ├── model/        -> entidades (tablas de la base de datos)
 ├── dto/          -> datos de formularios (no son tablas)
 └── config/       -> seguridad, login, permisos por cargo

src/main/resources/
 ├── templates/    -> vistas HTML (Thymeleaf)
 ├── static/css/   -> estilos (colores de marca de Universo Empaques)
 └── db/schema.sql -> script OFICIAL de la base de datos (modelo de Amelie)
```

## 6. Próximos pasos sugeridos (siguientes iteraciones)

1. Módulo de **Cotizaciones** (RF-06 a RF-09) — Comercial y Cliente.
2. Módulo de **Pedidos** (RF-10, RF-11, RF-14, RF-15).
3. Módulo de **Diseño** (RF-12, RF-13).
4. Módulo de **Producción** y **Despacho** (RF-14, RF-17).
5. Módulo de **Reportes** y **Copia de seguridad** (RF-18, RF-19).
6. Conectar el envío real de correos (RF-16).

Cada uno se puede ir pidiendo por separado para no mezclar demasiado
código nuevo de una sola vez, dado que el equipo está aprendiendo
Spring Boot sobre la marcha.
