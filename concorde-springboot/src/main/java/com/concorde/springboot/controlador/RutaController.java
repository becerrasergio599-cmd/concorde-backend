package com.concorde.springboot.controlador;

import com.concorde.springboot.modelo.Ruta;
import com.concorde.springboot.repositorio.RutaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rutas")
public class RutaController {

    @Autowired
    private RutaRepository repository;

    @GetMapping
    public List<Ruta> listar() { return repository.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<Ruta> obtener(@PathVariable Integer id) {
        return repository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    // Body de ejemplo:
    // { "terminalOrigen": {"idTerminal": 1}, "terminalDestino": {"idTerminal": 2}, "valorTiquete": 25000 }
    @PostMapping
    public ResponseEntity<Ruta> crear(@RequestBody Ruta r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(r));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Ruta> actualizar(@PathVariable Integer id, @RequestBody Ruta datos) {
        return repository.findById(id).map(e -> {
            e.setTerminalOrigen(datos.getTerminalOrigen());
            e.setTerminalDestino(datos.getTerminalDestino());
            e.setValorTiquete(datos.getValorTiquete());
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
