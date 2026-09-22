-- MySQL dump 10.13  Distrib 8.0.40, for Win64 (x86_64)
--
-- Host: localhost    Database: universo_empaques
-- ------------------------------------------------------
-- Server version	8.0.40

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `universo_empaques`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `universo_empaques` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `universo_empaques`;

--
-- Table structure for table `area`
--

DROP TABLE IF EXISTS `area`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `area` (
  `codigo` int NOT NULL AUTO_INCREMENT,
  `direccion` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tipo` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`codigo`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `area`
--

LOCK TABLES `area` WRITE;
/*!40000 ALTER TABLE `area` DISABLE KEYS */;
INSERT INTO `area` VALUES (1,'Planta Universo Empaques - Bucaramanga','Comercial'),(2,'Planta Universo Empaques - Bucaramanga','Diseno'),(3,'Planta Universo Empaques - Bucaramanga','Produccion'),(4,'Planta Universo Empaques - Bucaramanga','Bodega');
/*!40000 ALTER TABLE `area` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cliente`
--

DROP TABLE IF EXISTS `cliente`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cliente` (
  `codigo` int NOT NULL AUTO_INCREMENT,
  `contrasena` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `correo` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `direccion` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nombre` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `telefono` int DEFAULT NULL,
  PRIMARY KEY (`codigo`),
  UNIQUE KEY `UK_k6i2j3upwar1uora4mgiol6b` (`correo`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cliente`
--

LOCK TABLES `cliente` WRITE;
/*!40000 ALTER TABLE `cliente` DISABLE KEYS */;
INSERT INTO `cliente` VALUES (1,'$2a$10$ELwzvBSY.8q1L1CO0rkoO.ed.ujDLeni0LxnEk5u0hgMiewNemPhS','ewe@ewe','123','e',123),(2,'$2a$10$rtoerxXSQRGmVq1nvRN/qOVY/rGAIIVuoGBM2DKgxqbwlL3hMUgxa','acermo@a','1','acermo',1111111);
/*!40000 ALTER TABLE `cliente` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `detalle_pedido`
--

DROP TABLE IF EXISTS `detalle_pedido`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `detalle_pedido` (
  `codigo` int NOT NULL AUTO_INCREMENT,
  `cantidad` int DEFAULT NULL,
  `codigo_pedido` int DEFAULT NULL,
  `codigo_producto` int DEFAULT NULL,
  PRIMARY KEY (`codigo`),
  KEY `FKno00qjdldx7qifq47n2tj264l` (`codigo_pedido`),
  KEY `FKcwijdleua1uiudej4bb2xajfn` (`codigo_producto`),
  CONSTRAINT `FKcwijdleua1uiudej4bb2xajfn` FOREIGN KEY (`codigo_producto`) REFERENCES `producto` (`codigo`),
  CONSTRAINT `FKno00qjdldx7qifq47n2tj264l` FOREIGN KEY (`codigo_pedido`) REFERENCES `pedido` (`codigo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `detalle_pedido`
--

LOCK TABLES `detalle_pedido` WRITE;
/*!40000 ALTER TABLE `detalle_pedido` DISABLE KEYS */;
/*!40000 ALTER TABLE `detalle_pedido` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `diseno`
--

DROP TABLE IF EXISTS `diseno`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `diseno` (
  `codigo` int NOT NULL AUTO_INCREMENT,
  `especificaciones_tecnicas` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `estado` enum('PENDIENTE','APROBADO','AJUSTE_SOLICITADO') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `codigo_pedido` int DEFAULT NULL,
  `codigo_usuario` int DEFAULT NULL,
  PRIMARY KEY (`codigo`),
  KEY `FK2wup09qitwy7gvsyt69xhalcr` (`codigo_pedido`),
  KEY `FKdaq3jpo8eq86mt3mneu1lf0qg` (`codigo_usuario`),
  CONSTRAINT `FK2wup09qitwy7gvsyt69xhalcr` FOREIGN KEY (`codigo_pedido`) REFERENCES `pedido` (`codigo`),
  CONSTRAINT `FKdaq3jpo8eq86mt3mneu1lf0qg` FOREIGN KEY (`codigo_usuario`) REFERENCES `usuario` (`codigo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `diseno`
--

LOCK TABLES `diseno` WRITE;
/*!40000 ALTER TABLE `diseno` DISABLE KEYS */;
/*!40000 ALTER TABLE `diseno` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `estado_pedido`
--

DROP TABLE IF EXISTS `estado_pedido`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `estado_pedido` (
  `codigo` int NOT NULL AUTO_INCREMENT,
  `estado` enum('SOLICITADO','EN_DISENO','EN_PRODUCCION','TERMINADO','DESPACHADO','ENTREGADO') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fecha_fin` datetime(6) DEFAULT NULL,
  `fecha_inicio` datetime(6) DEFAULT NULL,
  `reporte` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `codigo_pedido` int DEFAULT NULL,
  `codigo_usuario` int DEFAULT NULL,
  PRIMARY KEY (`codigo`),
  KEY `FK1o5wdas2g0ed38f8q977b3t7g` (`codigo_pedido`),
  KEY `FK92we8n6qitu7nx6asxtub40mt` (`codigo_usuario`),
  CONSTRAINT `FK1o5wdas2g0ed38f8q977b3t7g` FOREIGN KEY (`codigo_pedido`) REFERENCES `pedido` (`codigo`),
  CONSTRAINT `FK92we8n6qitu7nx6asxtub40mt` FOREIGN KEY (`codigo_usuario`) REFERENCES `usuario` (`codigo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `estado_pedido`
--

LOCK TABLES `estado_pedido` WRITE;
/*!40000 ALTER TABLE `estado_pedido` DISABLE KEYS */;
/*!40000 ALTER TABLE `estado_pedido` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pedido`
--

DROP TABLE IF EXISTS `pedido`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pedido` (
  `codigo` int NOT NULL AUTO_INCREMENT,
  `fecha_entrega` datetime(6) DEFAULT NULL,
  `fecha_registro` datetime(6) DEFAULT NULL,
  `codigo_cliente` int DEFAULT NULL,
  `codigo_usuario` int DEFAULT NULL,
  PRIMARY KEY (`codigo`),
  KEY `FKlljc9v6pce0jpy3ng2wm91mbd` (`codigo_cliente`),
  KEY `FKmqlda2hhcolmusws35fq3ki6q` (`codigo_usuario`),
  CONSTRAINT `FKlljc9v6pce0jpy3ng2wm91mbd` FOREIGN KEY (`codigo_cliente`) REFERENCES `cliente` (`codigo`),
  CONSTRAINT `FKmqlda2hhcolmusws35fq3ki6q` FOREIGN KEY (`codigo_usuario`) REFERENCES `usuario` (`codigo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pedido`
--

LOCK TABLES `pedido` WRITE;
/*!40000 ALTER TABLE `pedido` DISABLE KEYS */;
/*!40000 ALTER TABLE `pedido` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `producto`
--

DROP TABLE IF EXISTS `producto`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `producto` (
  `codigo` int NOT NULL AUTO_INCREMENT,
  `material` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nombre` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `precio` decimal(38,2) DEFAULT NULL,
  PRIMARY KEY (`codigo`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `producto`
--

LOCK TABLES `producto` WRITE;
/*!40000 ALTER TABLE `producto` DISABLE KEYS */;
INSERT INTO `producto` VALUES (1,'Cartón Kraft','Caja Kraft mediana',3500.00),(2,'Papel Kraft','Bolsa personalizada',1200.00),(3,'Cartón microcorrugado','Caja para repostería',2800.00),(4,'Cartón rígido','Empaque para accesorios',4200.00);
/*!40000 ALTER TABLE `producto` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rol`
--

DROP TABLE IF EXISTS `rol`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rol` (
  `codigo` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`codigo`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rol`
--

LOCK TABLES `rol` WRITE;
/*!40000 ALTER TABLE `rol` DISABLE KEYS */;
INSERT INTO `rol` VALUES (1,'Administrador'),(2,'Empleado');
/*!40000 ALTER TABLE `rol` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `usuario`
--

DROP TABLE IF EXISTS `usuario`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `usuario` (
  `codigo` int NOT NULL AUTO_INCREMENT,
  `contrasena` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `correo` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nombre` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `telefono` int DEFAULT NULL,
  `codigo_area` int DEFAULT NULL,
  `codigo_rol` int DEFAULT NULL,
  PRIMARY KEY (`codigo`),
  UNIQUE KEY `UK_2mlfr087gb1ce55f2j87o74t` (`correo`),
  KEY `FKgs3fdvpr1kn8110mewwp384rm` (`codigo_area`),
  KEY `FK1kgw3g1oelf4ok4oehhkwst7v` (`codigo_rol`),
  CONSTRAINT `FK1kgw3g1oelf4ok4oehhkwst7v` FOREIGN KEY (`codigo_rol`) REFERENCES `rol` (`codigo`),
  CONSTRAINT `FKgs3fdvpr1kn8110mewwp384rm` FOREIGN KEY (`codigo_area`) REFERENCES `area` (`codigo`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `usuario`
--

LOCK TABLES `usuario` WRITE;
/*!40000 ALTER TABLE `usuario` DISABLE KEYS */;
INSERT INTO `usuario` VALUES (1,'$2a$10$Aq4U95DiGS5acpwVw1LfKODtUpet2atMv4/.EQUwDq/5rwNfJHqgm','admin@universoempaques.com','Administrador',NULL,NULL,1),(2,'$2a$10$ezFLNHRtMtwfBL9sUfLUzuSrFaXdywTlI9MbhCr4Js0.krb1ehTwi','wewe@eedds','ewe',123,1,2),(3,'$2a$10$B87HsbVQ8/L.rdwtKdZMfuvRe/a4Z7QKgWCI3E4Wjn933JUkfk0/O','pulga@h','pulgaa',3434,4,2);
/*!40000 ALTER TABLE `usuario` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'universo_empaques'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-09-21 20:05:55
