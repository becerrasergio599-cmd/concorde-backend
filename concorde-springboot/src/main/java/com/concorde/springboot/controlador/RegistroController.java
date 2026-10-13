package com.concorde.springboot.controlador;

import com.concorde.springboot.modelo.Rol;
import com.concorde.springboot.modelo.Usuario;
import com.concorde.springboot.repositorio.RolRepository;
import com.concorde.springboot.repositorio.UsuarioRepository;
import com.concorde.springboot.servicio.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Registro público de clientes (usado por p04-registro.html).
//
// Es un endpoint distinto de POST /api/usuarios a propósito: ese otro
// endpoint lo usan admin-panel.html y agente-panel.html, donde SÍ tiene
// sentido que el formulario elija el rol (un administrador puede crear
// otro administrador). Este es público -- lo puede llamar cualquiera
// desde el navegador sin haber iniciado sesión -- así que el rol jamás
// se toma de lo que mande el cliente: siempre se asigna "CLIENTE" aquí
// mismo, en el servidor. De lo contrario, cualquiera podría editar el
// payload con las herramientas de desarrollador y auto-asignarse el
// rol ADMINISTRADOR.
@RestController
@RequestMapping("/api/registro")
public class RegistroController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordUtil passwordUtil;

    @PostMapping
    public ResponseEntity<?> registrar(@RequestBody Map<String, String> body) {
        String nombre = valor(body, "nombreCompleto");
        String documento = valor(body, "numeroDocumento");
        String telefono = valor(body, "telefono");
        String correo = valor(body, "correoElectronico");
        String contrasena = body.get("contrasena");

        if (nombre == null || documento == null || correo == null || contrasena == null || contrasena.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("detail", "Completa todos los campos obligatorios."));
        }
        if (contrasena.length() < 8) {
            return ResponseEntity.badRequest().body(Map.of("detail", "La contraseña debe tener mínimo 8 caracteres."));
        }
        boolean correoRepetido = usuarioRepository.findAll().stream()
                .anyMatch(u -> u.getCorreoElectronico() != null && u.getCorreoElectronico().equalsIgnoreCase(correo));
        if (correoRepetido) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("detail", "Ya existe una cuenta con ese correo electrónico."));
        }

        Optional<Rol> rolCliente = rolRepository.findAll().stream()
                .filter(r -> "CLIENTE".equalsIgnoreCase(r.getNombreRol()))
                .findFirst();
        if (rolCliente.isEmpty()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("detail", "No existe el rol CLIENTE en la base de datos. Contacta al administrador."));
        }

        Usuario u = new Usuario();
        u.setNombreCompleto(nombre);
        u.setNumeroDocumento(documento);
        u.setTelefono(telefono);
        u.setCorreoElectronico(correo);
        u.setContrasenaHash(passwordUtil.hash(contrasena));
        u.setRol(rolCliente.get());
        u.setCreadoEn(LocalDateTime.now());

        Usuario guardado = usuarioRepository.save(u);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "idUsuario", guardado.getIdUsuario(),
                "nombreCompleto", guardado.getNombreCompleto(),
                "correoElectronico", guardado.getCorreoElectronico()
        ));
    }

    private String valor(Map<String, String> body, String clave) {
        String v = body.get(clave);
        if (v == null) return null;
        v = v.trim();
        return v.isEmpty() ? null : v;
    }
}
