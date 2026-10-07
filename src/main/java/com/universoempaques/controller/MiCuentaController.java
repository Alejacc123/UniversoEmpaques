package com.universoempaques.controller;

import com.universoempaques.config.AppUserPrincipal;
import com.universoempaques.dto.CambiarContrasenaForm;
import com.universoempaques.dto.MisDatosClienteForm;
import com.universoempaques.service.CuentaService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * "Mi cuenta" (/cuenta): disponible para cualquiera que haya iniciado
 * sesion. Todos cambian su contrasena; el cliente ademas actualiza sus
 * datos de contacto.
 */
@Controller
@RequestMapping("/cuenta")
public class MiCuentaController {

    private final CuentaService cuentaService;

    public MiCuentaController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    @GetMapping
    public String ver(Authentication auth, Model model) {
        AppUserPrincipal quien = principal(auth);
        model.addAttribute("cambiarContrasenaForm", new CambiarContrasenaForm());
        if (quien.esCliente()) {
            model.addAttribute("misDatosClienteForm", cuentaService.datosDelCliente(quien));
        }
        return mostrar(quien, model);
    }

    private String mostrar(AppUserPrincipal quien, Model model) {
        model.addAttribute("esCliente", quien.esCliente());
        if (quien.esCliente()) {
            model.addAttribute("cliente", cuentaService.clienteActual(quien));
        }
        return "cuenta";
    }

    @PostMapping("/contrasena")
    public String cambiarContrasena(@Valid @ModelAttribute CambiarContrasenaForm cambiarContrasenaForm,
                                    BindingResult resultado, Authentication auth, Model model,
                                    RedirectAttributes flash) {
        AppUserPrincipal quien = principal(auth);
        if (!resultado.hasErrors()) {
            try {
                cuentaService.cambiarContrasena(quien, cambiarContrasenaForm);
                flash.addFlashAttribute("exito", "Tu contraseña se cambió. Úsala la próxima vez que inicies sesión.");
                return "redirect:/cuenta";
            } catch (IllegalArgumentException ex) {
                model.addAttribute("errorContrasena", ex.getMessage());
            }
        }
        // Por seguridad, las contrasenas escritas no se devuelven al formulario
        cambiarContrasenaForm.setContrasenaActual(null);
        cambiarContrasenaForm.setContrasena(null);
        cambiarContrasenaForm.setConfirmarContrasena(null);
        if (quien.esCliente()) {
            model.addAttribute("misDatosClienteForm", cuentaService.datosDelCliente(quien));
        }
        return mostrar(quien, model);
    }

    @PostMapping("/datos")
    public String actualizarDatos(@Valid @ModelAttribute MisDatosClienteForm misDatosClienteForm,
                                  BindingResult resultado, Authentication auth, Model model,
                                  HttpServletRequest request, RedirectAttributes flash) throws ServletException {
        AppUserPrincipal quien = principal(auth);
        if (!quien.esCliente()) {
            return "redirect:/cuenta";
        }
        if (!resultado.hasErrors()) {
            try {
                boolean cambioCorreo = cuentaService.actualizarDatosCliente(quien, misDatosClienteForm);
                if (cambioCorreo) {
                    // El correo es el usuario del login: se entra de nuevo con el nuevo
                    request.logout();
                    return "redirect:/login?correoActualizado";
                }
                flash.addFlashAttribute("exito", "Tus datos se actualizaron.");
                return "redirect:/cuenta";
            } catch (IllegalArgumentException ex) {
                model.addAttribute("errorDatos", ex.getMessage());
            }
        }
        model.addAttribute("cambiarContrasenaForm", new CambiarContrasenaForm());
        return mostrar(quien, model);
    }

    private static AppUserPrincipal principal(Authentication auth) {
        return (AppUserPrincipal) auth.getPrincipal();
    }
}
