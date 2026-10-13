package com.concorde.springboot.controlador;

import com.concorde.springboot.modelo.Terminal;
import com.concorde.springboot.repositorio.TerminalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/terminales")
public class TerminalController {

    @Autowired
    private TerminalRepository repository;

    @GetMapping
    public List<Terminal> listar() { return repository.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<Terminal> obtener(@PathVariable Integer id) {
        return repository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Terminal> crear(@RequestBody Terminal t) {
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(t));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Terminal> actualizar(@PathVariable Integer id, @RequestBody Terminal datos) {
        return repository.findById(id).map(e -> {
            e.setNombre(datos.getNombre());
            e.setCiudad(datos.getCiudad());
            e.setDireccion(datos.getDireccion());
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
