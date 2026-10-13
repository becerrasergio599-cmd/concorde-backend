package com.concorde.springboot.controlador;

import com.concorde.springboot.modelo.Pago;
import com.concorde.springboot.repositorio.PagoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    @Autowired
    private PagoRepository repository;

    @GetMapping
    public List<Pago> listar() { return repository.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<Pago> obtener(@PathVariable Integer id) {
        return repository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Pago> crear(@RequestBody Pago p) {
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(p));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Pago> actualizar(@PathVariable Integer id, @RequestBody Pago datos) {
        return repository.findById(id).map(e -> {
            e.setReserva(datos.getReserva());
            e.setMetodoPago(datos.getMetodoPago());
            e.setValorPagado(datos.getValorPagado());
            e.setFechaPago(datos.getFechaPago());
            e.setEstado(datos.getEstado());
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
