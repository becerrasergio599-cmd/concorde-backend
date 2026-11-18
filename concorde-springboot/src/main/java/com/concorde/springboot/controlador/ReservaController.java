package com.concorde.springboot.controlador;

import com.concorde.springboot.modelo.Reserva;
import com.concorde.springboot.repositorio.ReservaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    @Autowired
    private ReservaRepository repository;

    @GetMapping
    public List<Reserva> listar() {
        return repository.findAll(PageRequest.of(0, 100)).getContent();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Reserva> obtener(@PathVariable Integer id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Reserva> crear(@RequestBody Reserva r) {
        // Si no llega la clase, se asume económica
        if (r.getClase() == null || r.getClase().isBlank()) {
            r.setClase("ECONOMICA");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(repository.save(r));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Reserva> actualizar(
            @PathVariable Integer id,
            @RequestBody Reserva datos) {

        return repository.findById(id).map(e -> {
            e.setUsuario(datos.getUsuario());
            e.setViaje(datos.getViaje());
            e.setFechaInicial(datos.getFechaInicial());
            e.setEstado(datos.getEstado());

            // Solo se actualizan si vienen en la petición (así cancelar no borra la tarifa Premium)
            if (datos.getClase() != null) {
                e.setClase(datos.getClase());
            }
            if (datos.getTotal() != null) {
                e.setTotal(datos.getTotal());
            }

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
