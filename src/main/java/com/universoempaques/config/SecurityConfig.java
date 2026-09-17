package com.universoempaques.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Reglas de acceso del sistema (RNF-02: autenticacion, RNF-03: control
 * de acceso segun el rol/cargo).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt: las contrasenas nunca se guardan en texto plano.
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Paginas publicas: inicio, login, registro de cliente y recursos estaticos
                .requestMatchers("/", "/login", "/registro", "/css/**", "/js/**", "/img/**").permitAll()

                // Modulo de administracion: solo el rol Administrador (RF-03, RF-04, RF-18, RF-19)
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // Cada area solo entra a su propio modulo interno
                .requestMatchers("/comercial/**").hasAnyRole("ADMIN", "COMERCIAL")
                .requestMatchers("/diseno/**").hasAnyRole("ADMIN", "DISENO")
                .requestMatchers("/produccion/**").hasAnyRole("ADMIN", "PRODUCCION")
                .requestMatchers("/bodega/**").hasAnyRole("ADMIN", "BODEGA")

                // Panel del cliente
                .requestMatchers("/cliente/**").hasRole("CLIENTE")

                // Cualquier otra ruta requiere, al menos, haber iniciado sesion
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(new RedireccionSegunRolHandler())
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

        return http.build();
    }
}
