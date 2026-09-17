-- =====================================================================
-- Sistema de Gestion de Repartos de Empaques - Universo Empaques
-- Script de creacion de base de datos (MySQL)
-- Corresponde 1:1 al Diccionario de Datos y al Diagrama de Tablas
-- entregados en el documento de Diseno Preliminar.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS universo_empaques
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE universo_empaques;

-- ---------------------------------------------------------------------
-- Tabla Area: departamento/cargo al que pertenece un trabajador
-- (Comercial, Diseno, Produccion, Bodega)
-- ---------------------------------------------------------------------
CREATE TABLE area (
  codigo    INT AUTO_INCREMENT PRIMARY KEY,
  tipo      VARCHAR(255),
  direccion VARCHAR(255)
);

-- ---------------------------------------------------------------------
-- Tabla Rol: nivel jerarquico de permisos (Administrador, Empleado)
-- ---------------------------------------------------------------------
CREATE TABLE rol (
  codigo INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(255)
);

-- ---------------------------------------------------------------------
-- Tabla Cliente: usuarios externos que solicitan cotizaciones/pedidos
-- ---------------------------------------------------------------------
CREATE TABLE cliente (
  codigo      INT AUTO_INCREMENT PRIMARY KEY,
  correo      VARCHAR(255) UNIQUE,
  contrasena  VARCHAR(255),
  direccion   VARCHAR(255),
  nombre      VARCHAR(255),
  telefono    INTEGER
);

-- ---------------------------------------------------------------------
-- Tabla Usuario: trabajadores internos, categorizados por Area y Rol
-- ---------------------------------------------------------------------
CREATE TABLE usuario (
  codigo       INT AUTO_INCREMENT PRIMARY KEY,
  correo       VARCHAR(255) UNIQUE,
  contrasena   VARCHAR(255),
  nombre       VARCHAR(255),
  telefono     INTEGER,
  codigo_area  INT,
  codigo_rol   INT,
  CONSTRAINT fk_usuario_area FOREIGN KEY (codigo_area) REFERENCES area(codigo),
  CONSTRAINT fk_usuario_rol  FOREIGN KEY (codigo_rol)  REFERENCES rol(codigo)
);

-- ---------------------------------------------------------------------
-- Tabla Pedido
-- ---------------------------------------------------------------------
CREATE TABLE pedido (
  codigo          INT AUTO_INCREMENT PRIMARY KEY,
  fecha_registro  DATETIME,
  fecha_entrega   DATETIME,
  codigo_cliente  INT,
  codigo_usuario  INT,
  CONSTRAINT fk_pedido_cliente FOREIGN KEY (codigo_cliente) REFERENCES cliente(codigo),
  CONSTRAINT fk_pedido_usuario FOREIGN KEY (codigo_usuario) REFERENCES usuario(codigo)
);

-- ---------------------------------------------------------------------
-- Tabla Diseno
-- ---------------------------------------------------------------------
CREATE TABLE diseno (
  codigo                    INT AUTO_INCREMENT PRIMARY KEY,
  especificaciones_tecnicas VARCHAR(255),
  estado                    VARCHAR(255),
  codigo_pedido             INT,
  codigo_usuario            INT,
  CONSTRAINT fk_diseno_pedido  FOREIGN KEY (codigo_pedido)  REFERENCES pedido(codigo),
  CONSTRAINT fk_diseno_usuario FOREIGN KEY (codigo_usuario) REFERENCES usuario(codigo)
);

-- ---------------------------------------------------------------------
-- Tabla Estado_pedido: historial de estados por los que pasa un pedido
-- ---------------------------------------------------------------------
CREATE TABLE estado_pedido (
  codigo          INT AUTO_INCREMENT PRIMARY KEY,
  fecha_inicio    DATETIME,
  fecha_fin       DATETIME,
  estado          VARCHAR(255),
  reporte         VARCHAR(255),
  codigo_usuario  INT,
  codigo_pedido   INT,
  CONSTRAINT fk_estadopedido_usuario FOREIGN KEY (codigo_usuario) REFERENCES usuario(codigo),
  CONSTRAINT fk_estadopedido_pedido  FOREIGN KEY (codigo_pedido)  REFERENCES pedido(codigo)
);

-- ---------------------------------------------------------------------
-- Tabla Producto: catalogo de productos/empaques ofrecidos
-- ---------------------------------------------------------------------
CREATE TABLE producto (
  codigo   INT AUTO_INCREMENT PRIMARY KEY,
  nombre   VARCHAR(255),
  material VARCHAR(255),
  precio   DECIMAL(12,2)
);

-- ---------------------------------------------------------------------
-- Tabla Detalle_pedido: productos y cantidades incluidos en un pedido
-- ---------------------------------------------------------------------
CREATE TABLE detalle_pedido (
  codigo          INT AUTO_INCREMENT PRIMARY KEY,
  cantidad        INT,
  codigo_pedido   INT,
  codigo_producto INT,
  CONSTRAINT fk_detalle_pedido   FOREIGN KEY (codigo_pedido)   REFERENCES pedido(codigo),
  CONSTRAINT fk_detalle_producto FOREIGN KEY (codigo_producto) REFERENCES producto(codigo)
);

-- =====================================================================
-- Datos iniciales (necesarios para poder iniciar sesion desde el primer
-- arranque de la aplicacion)
-- =====================================================================

INSERT INTO rol (nombre) VALUES ('Administrador'), ('Empleado');

INSERT INTO area (tipo, direccion) VALUES
  ('Comercial',   'Planta Universo Empaques - Bucaramanga'),
  ('Diseno',      'Planta Universo Empaques - Bucaramanga'),
  ('Produccion',  'Planta Universo Empaques - Bucaramanga'),
  ('Bodega',      'Planta Universo Empaques - Bucaramanga');

-- Usuario administrador inicial.
-- Correo: admin@universoempaques.com / Contrasena: admin123
-- (la contrasena real se guarda cifrada con BCrypt; este valor de
--  ejemplo se genera automaticamente la primera vez que se ejecuta
--  la aplicacion si la tabla usuario esta vacia - ver DataSeeder.java)
