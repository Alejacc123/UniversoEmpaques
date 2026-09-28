-- =====================================================================
-- Sistema de Gestion de Repartos de Empaques - Universo Empaques
-- Script de la base de datos (modelo v2 de Amelie - "SCRIPT vf2")
--
-- Es el script de Amelie con estos cambios (marcados en el script):
--   1. CORREGIDO - Cotizacion: sobraba una coma despues del ultimo FOREIGN KEY.
--   2. CORREGIDO - Diseno: la columna se declaraba "CodigoDetallePedidO" pero
--      el FOREIGN KEY apuntaba a "CodigoDetallePedid". Se unifico como
--      "CodigoDetallePedido" (el nombre del Diccionario de Datos).
--   3. CAMBIADO - Cliente.Telefono y Usuario.TelefonoEmpresa pasan de INT a
--      VARCHAR(20): un numero colombiano de 10 digitos (3001234567) no cabe
--      en INT (maximo 2.147.483.647). Pendiente que Amelie lo actualice en
--      el diccionario de datos.
--
-- IMPORTANTE (Linux/Docker): MySQL distingue mayusculas en los nombres
-- de tabla. No cambien "Cliente" por "cliente", etc.
--
-- Como correrlo (con el contenedor mysql-bd encendido):
--   docker exec -i mysql-bd mysql -uroot -puniverso123 < src/main/resources/db/schema.sql
--
-- OJO: la primera linea BORRA la base de datos completa (datos incluidos).
-- =====================================================================

DROP DATABASE IF EXISTS universo_empaques;

CREATE DATABASE universo_empaques
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE universo_empaques;

CREATE TABLE Cliente (
    NIT VARCHAR(255) PRIMARY KEY NOT NULL,
    Correo VARCHAR(255),
    Contrasena VARCHAR(255),
    Direccion VARCHAR(255),
    NombreEmpresa VARCHAR(255),
    Telefono VARCHAR(20),                             -- CAMBIADO: antes INT
    FechaRegistro DATE,
    EstadoCliente VARCHAR(255),
    Celular VARCHAR(255),
    RazonSocial VARCHAR(255)
);

CREATE TABLE Usuario (
    Codigo INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
    Correo VARCHAR(255),
    Contrasena VARCHAR(255),
    NombreEmpresa VARCHAR(255),
    TelefonoPersonal VARCHAR(255),
    TelefonoEmpresa VARCHAR(20),                      -- CAMBIADO: antes INT
    NumDocumento VARCHAR(255),
    FechaIngreso DATE,
    CantHorasTrabajadas DECIMAL(10,2),
    FecVencimientoContrato DATE,
    Nomina DECIMAL(10,2)
);

CREATE TABLE Producto (
    Codigo INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
    Nombre VARCHAR(255),
    Material VARCHAR(255),
    Precio DECIMAL(10,2),
    Forma VARCHAR(255),
    Tamano VARCHAR(255)
);

CREATE TABLE Area (
    Codigo INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
    Nombre VARCHAR(255),
    Direccion VARCHAR(255)
);

CREATE TABLE Rol (
    Codigo INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
    Nombre VARCHAR(255),
    Permisos VARCHAR(255)
);

CREATE TABLE Pedido (
    Codigo INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
    FechaRegistroTecnicas DATETIME,
    FechaEntrega DATETIME,
    NITCliente VARCHAR(255),
    CodigoUsuario INT,
    FormaPago VARCHAR(255),
    DireccionEntrega VARCHAR(255),
    FOREIGN KEY (NITCliente) REFERENCES Cliente(NIT),
    FOREIGN KEY (CodigoUsuario) REFERENCES Usuario(Codigo)
);

CREATE TABLE DetallePedido (
    Codigo INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
    Cantidad INT,
    CodigoPedido INT,
    CodigoProducto INT,
    PrecioUnitario DECIMAL(10,2),
    FOREIGN KEY (CodigoPedido) REFERENCES Pedido(Codigo),
    FOREIGN KEY (CodigoProducto) REFERENCES Producto(Codigo)
);

CREATE TABLE EstadoPedido (
    Codigo INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
    FechaInicio DATETIME,
    FechaFin DATETIME,
    Estado VARCHAR(255),
    Reporte VARCHAR(255),
    CodigoUsuario INT,
    CodigoPedido INT,
    FOREIGN KEY (CodigoUsuario) REFERENCES Usuario(Codigo),
    FOREIGN KEY (CodigoPedido) REFERENCES Pedido(Codigo)
);

CREATE TABLE Cotizacion (
    Codigo INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
    Valor DOUBLE,
    Estado VARCHAR(255),
    EspecificacionesEmpaqueSolicitado VARCHAR(255),
    CodigoUsuario INT,
    CodigoPedido INT,
    FechaSolicitud DATE,
    NITCliente VARCHAR(255),
    FOREIGN KEY (CodigoUsuario) REFERENCES Usuario(Codigo),
    FOREIGN KEY (CodigoPedido) REFERENCES Pedido(Codigo),
    FOREIGN KEY (NITCliente) REFERENCES Cliente(NIT)   -- CORREGIDO: sin coma final
);

CREATE TABLE UsuarioArea (
    Codigo INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
    CodigoUsuario INT,
    CodigoArea INT,
    FOREIGN KEY (CodigoUsuario) REFERENCES Usuario(Codigo),
    FOREIGN KEY (CodigoArea) REFERENCES Area(Codigo)
);

CREATE TABLE UsuarioRol (
    Codigo INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
    CodigoUsuario INT,
    CodigoRol INT,
    FOREIGN KEY (CodigoUsuario) REFERENCES Usuario(Codigo),
    FOREIGN KEY (CodigoRol) REFERENCES Rol(Codigo)
);

CREATE TABLE Diseno (
    Codigo INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
    Estado VARCHAR(255),
    Color VARCHAR(255),
    Logo BLOB,
    ArchivoDiseno BLOB,
    FechaAprobado DATE,
    Observaciones VARCHAR(255),
    CodigoUsuario INT,
    Version INT,
    CodigoDetallePedido INT,                           -- CORREGIDO: antes "CodigoDetallePedidO"
    FOREIGN KEY (CodigoUsuario) REFERENCES Usuario(Codigo),
    FOREIGN KEY (CodigoDetallePedido) REFERENCES DetallePedido(Codigo)  -- CORREGIDO: antes "CodigoDetallePedid"
);

SHOW TABLES;
