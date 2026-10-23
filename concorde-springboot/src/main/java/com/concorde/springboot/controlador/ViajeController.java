package com.concorde.springboot.controlador;

import com.concorde.springboot.modelo.Viaje;
import com.concorde.springboot.repositorio.ViajeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/viajes")
public class ViajeController {

    @Autowired
    private ViajeRepository repository;

    @GetMapping
    public List<Viaje> listar() { return repository.findAll(); }

    // Busqueda publica: /api/viajes/buscar?origen=TUNJA&destino=DUITAMA&fecha=2026-10-24
    @GetMapping("/buscar")
    public List<Viaje> buscar(@RequestParam String origen,
                              @RequestParam String destino,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return repository.buscar(ciudadDe(origen), ciudadDe(destino), fecha);
    }

    // La pagina envia claves como TUNJA o BOGOTA_SALITRE; la tabla guarda la ciudad
    private String ciudadDe(String clave) {
        String c = clave.trim().toUpperCase();
        if (c.startsWith("BOGOTA")) return "Bogota";
        return c.replace('_', ' ');
    }

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
