package com.concorde.springboot.controlador;

import com.concorde.springboot.modelo.Vehiculo;
import com.concorde.springboot.repositorio.VehiculoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehiculos")
public class VehiculoController {

    @Autowired
    private VehiculoRepository repository;

    @GetMapping
    public List<Vehiculo> listar() { return repository.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<Vehiculo> obtener(@PathVariable Integer id) {
        return repository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Vehiculo> crear(@RequestBody Vehiculo v) {
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(v));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Vehiculo> actualizar(@PathVariable Integer id, @RequestBody Vehiculo datos) {
        return repository.findById(id).map(e -> {
            e.setPlaca(datos.getPlaca());
            e.setCapacidad(datos.getCapacidad());
            e.setModelo(datos.getModelo());
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
