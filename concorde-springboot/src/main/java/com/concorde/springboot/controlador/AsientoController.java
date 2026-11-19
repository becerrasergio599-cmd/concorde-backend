package com.concorde.springboot.controlador;

import com.concorde.springboot.modelo.Asiento;
import com.concorde.springboot.repositorio.AsientoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/asientos")
public class AsientoController {

    @Autowired
    private AsientoRepository repository;

    @GetMapping
    public List<Asiento> listar() { return repository.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<Asiento> obtener(@PathVariable Integer id) {
        return repository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Asiento> crear(@RequestBody Asiento a) {
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(a));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Asiento> actualizar(@PathVariable Integer id, @RequestBody Asiento datos) {
        return repository.findById(id).map(e -> {
            e.setVehiculo(datos.getVehiculo());
            e.setNumeroAsiento(datos.getNumeroAsiento());
            e.setTipo(datos.getTipo());
            e.setDisponible(datos.getDisponible());
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
