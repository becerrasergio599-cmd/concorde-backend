package com.concorde.springboot.controlador;

import com.concorde.springboot.modelo.Asiento;
import com.concorde.springboot.modelo.Vehiculo;
import com.concorde.springboot.repositorio.AsientoRepository;
import com.concorde.springboot.repositorio.VehiculoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/asientos")
public class AsientoController {

    @Autowired
    private AsientoRepository repository;

    @Autowired
    private VehiculoRepository vehiculoRepository;

    @GetMapping
    public List<Asiento> listar() { return repository.findAll(); }

    // Crea los asientos 1..capacidad de cada vehículo que aún no los tenga.
    // Es idempotente: si el asiento ya existe, no lo duplica.
    @PostMapping("/generar")
    public ResponseEntity<Map<String, Object>> generar() {
        int creados = 0;
        for (Vehiculo v : vehiculoRepository.findAll()) {
            if (v.getCapacidad() == null || v.getCapacidad() <= 0) continue;
            Set<String> existentes = new HashSet<>();
            for (Asiento a : repository.findAll()) {
                if (a.getVehiculo() != null && v.getIdVehiculo().equals(a.getVehiculo().getIdVehiculo())) {
                    existentes.add(a.getNumeroAsiento());
                }
            }
            for (int n = 1; n <= v.getCapacidad(); n++) {
                String numero = String.valueOf(n);
                if (existentes.contains(numero)) continue;
                Asiento nuevo = new Asiento();
                nuevo.setVehiculo(v);
                nuevo.setNumeroAsiento(numero);
                repository.save(nuevo);
                creados++;
            }
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("creados", creados);
        return ResponseEntity.ok(resp);
    }

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
