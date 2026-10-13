package com.concorde.springboot.controlador;

import com.concorde.springboot.modelo.Viaje;
import com.concorde.springboot.repositorio.ViajeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/viajes")
public class ViajeController {

    @Autowired
    private ViajeRepository repository;

    @GetMapping
    public List<Viaje> listar() { return repository.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<Viaje> obtener(@PathVariable Integer id) {
        return repository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Viaje> crear(@RequestBody Viaje v) {
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(v));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Viaje> actualizar(@PathVariable Integer id, @RequestBody Viaje datos) {
        return repository.findById(id).map(e -> {
            e.setRuta(datos.getRuta());
            e.setVehiculo(datos.getVehiculo());
            e.setConductor(datos.getConductor());
            e.setFechaViaje(datos.getFechaViaje());
            e.setHoraSalida(datos.getHoraSalida());
            e.setPrecio(datos.getPrecio());
            e.setEstadoViaje(datos.getEstadoViaje());
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
