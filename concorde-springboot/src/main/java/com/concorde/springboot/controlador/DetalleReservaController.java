package com.concorde.springboot.controlador;

import com.concorde.springboot.modelo.DetalleReserva;
import com.concorde.springboot.repositorio.DetalleReservaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/detalle-reservas")
public class DetalleReservaController {

    @Autowired
    private DetalleReservaRepository repository;

    @GetMapping
    public List<DetalleReserva> listar() { return repository.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<DetalleReserva> obtener(@PathVariable Integer id) {
        return repository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<DetalleReserva> crear(@RequestBody DetalleReserva d) {
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(d));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DetalleReserva> actualizar(@PathVariable Integer id, @RequestBody DetalleReserva datos) {
        return repository.findById(id).map(e -> {
            e.setReserva(datos.getReserva());
            e.setAsiento(datos.getAsiento());
            e.setAsientoAsignado(datos.getAsientoAsignado());
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
