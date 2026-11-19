package mx.edu.utez.rete.controller;

import mx.edu.utez.rete.model.Estudiante;
import mx.edu.utez.rete.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    // Aqui se verifica si el usuario es estudiante o administrador y se le da acceso a la aplicacion dependiendo de su rol

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> credenciales) {
        return authService.login(
                credenciales.get("identificador"),
                credenciales.get("password")
        );
    }

    @PostMapping("/registro")
    public Map<String, Object> registrar(@RequestBody Estudiante estudiante) {
        return authService.registrar(estudiante);
    }
}