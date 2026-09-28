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
        response.sendRedirect(request.getContextPath() + destinoPara(authentication));
    }

    /**
     * Panel que le corresponde a quien inicio sesion. Lo usa tambien
     * /mi-panel (el logo de la barra superior) para llevar a cada quien
     * a su inicio.
     */
    public static String destinoPara(Authentication authentication) {
        String destino = "/";
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            switch (authority.getAuthority()) {
                case "ROLE_CLIENTE"    -> destino = "/cliente/panel";
                case "ROLE_ADMIN"      -> destino = "/admin/panel";
                case "ROLE_COMERCIAL"  -> destino = "/comercial/panel";
                case "ROLE_DISENO"     -> destino = "/diseno/panel";
                case "ROLE_PRODUCCION" -> destino = "/produccion/panel";
                case "ROLE_BODEGA"     -> destino = "/bodega/panel";
                default -> { }
            }
        }
        return destino;
    }
}
