package com.universoempaques.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import java.io.IOException;

/**
 * Despues de iniciar sesion (RF-01), cada persona debe llegar a la
 * pantalla que le corresponde segun su rol o cargo, sin importar
 * que todos usaron el mismo formulario de login.
 */
public class RedireccionSegunRolHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException {
        String destino = "/";
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            switch (authority.getAuthority()) {
                case "ROLE_CLIENTE"    -> destino = "/cliente/panel";
                case "ROLE_ADMIN"      -> destino = "/admin/panel";
                case "ROLE_COMERCIAL"  -> destino = "/comercial/panel";
                case "ROLE_DISENO"     -> destino = "/diseno/panel";
                case "ROLE_PRODUCCION" -> destino = "/produccion/panel";
                case "ROLE_BODEGA"     -> destino = "/bodega/panel";
                default -> destino = "/";
            }
        }
        response.sendRedirect(request.getContextPath() + destino);
    }
}
