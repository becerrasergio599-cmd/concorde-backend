package com.concorde.springboot.controlador;

import com.concorde.springboot.modelo.Conductor;
import com.concorde.springboot.repositorio.ConductorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conductores")
public class ConductorController {

    @Autowired
    private ConductorRepository repository;

    @GetMapping
    public List<Conductor> listar() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Conductor> obtenerUno(@PathVariable Integer id) {
        return repository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Conductor> crear(@RequestBody Conductor c) {
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(c));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Conductor> actualizar(@PathVariable Integer id, @RequestBody Conductor datos) {
        return repository.findById(id).map(e -> {
            e.setNumeroLicencia(datos.getNumeroLicencia());
            e.setFechaVencimientoLicencia(datos.getFechaVencimientoLicencia());
            e.setPersona(datos.getPersona());
            return ResponseEntity.ok(repository.save(e));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}