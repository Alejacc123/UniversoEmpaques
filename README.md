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

No es necesario crear la base de datos a mano: al arrancar la aplicación,
Spring Boot la crea automáticamente (`createDatabaseIfNotExist=true`) y
Hibernate genera las tablas a partir de las clases en `model/`. Aun así,
dejamos el script `src/main/resources/db/schema.sql` como referencia y
para poder montar la base manualmente si lo prefieren.

Abran el archivo `src/main/resources/application.properties` y cambien
estas dos líneas por su usuario y contraseña reales de MySQL:

```properties
spring.datasource.username=root
spring.datasource.password=CAMBIAR_CONTRASENA
```

Si van a probar el envío de notificaciones por correo (RF-16) más adelante,
también deberán completar `spring.mail.username` y `spring.mail.password`
(usando una "contraseña de aplicación" de Gmail, no la contraseña normal
de la cuenta). Mientras no lo configuren, el resto del sistema funciona
igual — el envío de correo simplemente no está implementado todavía.

## 3. Ejecutar el proyecto

**Opción A — desde IntelliJ/VS Code:** abran la carpeta del proyecto,
esperen a que el IDE descargue las dependencias de Maven (barra de progreso
abajo), y ejecuten la clase `GestionRepartosEmpaquesApplication.java`
(botón ▶ verde).

**Opción B — desde la terminal**, parados en la carpeta del proyecto:

```bash
mvn spring-boot:run
```

La primera vez que arranca, la aplicación:
1. Crea la base de datos y las tablas.
2. Crea automáticamente un usuario **Administrador** con:
   - Correo: `admin@universoempaques.com`
   - Contraseña: `admin123`
   (verán este mensaje impreso en la consola). **Cámbienla luego.**

Abran el navegador en **http://localhost:8080**

## 4. Qué pueden probar ya mismo

- Entrar a `/` → página de inicio pública.
- Clic en "Registrarme" → crear una cuenta de cliente (RF-02) y quedar
  con acceso inmediato.
- Clic en "Iniciar sesión" → entrar como cliente recién creado, o como
  `admin@universoempaques.com` / `admin123` → cada uno debe llegar a un
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
 └── db/schema.sql -> script de referencia de la base de datos
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
