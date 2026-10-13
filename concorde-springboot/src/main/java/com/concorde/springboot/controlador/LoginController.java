package com.concorde.springboot.controlador;

import com.concorde.springboot.modelo.Usuario;
import com.concorde.springboot.repositorio.UsuarioRepository;
import com.concorde.springboot.servicio.JwtUtil;
import com.concorde.springboot.servicio.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

// Endpoint de login: valida el correo y la contraseña contra la tabla
// usuario real (con PasswordUtil / BCrypt) y, si son correctas, devuelve
// un token JWT que el resto de la API exige para las rutas protegidas
// (ver SecurityConfig y JwtAuthFilter). Si el registro todavía tenía la
// contraseña en texto plano (datos de antes del cambio a BCrypt), al
// validar correctamente se reemplaza por su hash de una vez -- migración
// transparente, sin tocar nada a mano en la base de datos.
@RestController
@RequestMapping("/api/login")
public class LoginController {

    @Autowired
    private UsuarioRepository repository;

    @Autowired
    private PasswordUtil passwordUtil;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String correo = body.get("correoElectronico");
        String contrasena = body.get("contrasena");

        List<Usuario> usuarios = repository.findAll();
        Optional<Usuario> encontrado = usuarios.stream()
                .filter(u -> u.getCorreoElectronico().equalsIgnoreCase(correo))
                .findFirst();

        if (encontrado.isEmpty() || !passwordUtil.coincide(contrasena, encontrado.get().getContrasenaHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("detail", "Correo o contraseña incorrectos"));
        }

        Usuario u = encontrado.get();
        if (!passwordUtil.esHashBcrypt(u.getContrasenaHash())) {
            u.setContrasenaHash(passwordUtil.hash(contrasena));
            repository.save(u);
        }

        String token = jwtUtil.generarToken(u.getIdUsuario(), u.getCorreoElectronico(), u.getRol().getNombreRol());

        return ResponseEntity.ok(Map.of(
                "token", token,
                "idUsuario", u.getIdUsuario(),
                "nombreCompleto", u.getNombreCompleto(),
                "correoElectronico", u.getCorreoElectronico(),
                "rol", u.getRol().getNombreRol()
        ));
    }
}
