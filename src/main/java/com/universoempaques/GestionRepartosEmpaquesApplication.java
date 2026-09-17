package com.universoempaques;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la aplicacion. Al ejecutar este archivo
 * (o correr "mvn spring-boot:run" desde la terminal) se levanta
 * el servidor web en http://localhost:8080
 */
@SpringBootApplication
public class GestionRepartosEmpaquesApplication {

    public static void main(String[] args) {
        SpringApplication.run(GestionRepartosEmpaquesApplication.class, args);
    }
}
