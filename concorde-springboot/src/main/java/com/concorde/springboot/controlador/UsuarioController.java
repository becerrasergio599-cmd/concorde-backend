package com.concorde.springboot.controlador;

import com.concorde.springboot.modelo.Usuario;
import com.concorde.springboot.repositorio.UsuarioRepository;
import com.concorde.springboot.servicio.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioRepository repository;

    @Autowired
    private PasswordUtil passwordUtil;

    @GetMapping
    public List<Usuario> listar() { return repository.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<Usuario> obtener(@PathVariable Integer id) {
        return repository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Usuario> crear(@RequestBody Usuario u) {
        // AQUÍ ESTÁ EL ARREGLO:
        // Le asignamos la fecha y hora actual antes de guardar
        // para que la base de datos no reciba un valor null en "creado_en".
        if (u.getCreadoEn() == null) {
            u.setCreadoEn(LocalDateTime.now());
        }
        // Nunca se guarda la contraseña tal cual llega del formulario.
        if (u.getContrasenaHash() != null && !passwordUtil.esHashBcrypt(u.getContrasenaHash())) {
            u.setContrasenaHash(passwordUtil.hash(u.getContrasenaHash()));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(u));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Usuario> actualizar(@PathVariable Integer id, @RequestBody Usuario datos) {
        return repository.findById(id).map(e -> {
            e.setTelefono(datos.getTelefono());
            e.setNumeroDocumento(datos.getNumeroDocumento());
            e.setCorreoElectronico(datos.getCorreoElectronico());
            // Si el formulario mandó una contraseña nueva (no vacía y que
            // todavía no es un hash), se hashea antes de guardar. Si vino
            // vacía o ya hasheada (el usuario no la tocó en el formulario),
            // se conserva tal cual para no invalidar el hash existente.
            String nuevaContrasena = datos.getContrasenaHash();
            if (nuevaContrasena != null && !nuevaContrasena.isBlank()) {
                e.setContrasenaHash(passwordUtil.esHashBcrypt(nuevaContrasena)
                        ? nuevaContrasena
                        : passwordUtil.hash(nuevaContrasena));
            }
            e.setNombreCompleto(datos.getNombreCompleto());
            e.setRol(datos.getRol());
            // Si al actualizar no traía fecha, conserva la que ya tenía previamente
            if (e.getCreadoEn() == null) {
                e.setCreadoEn(LocalDateTime.now());
            }
            return ResponseEntity.ok(repository.save(e));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
