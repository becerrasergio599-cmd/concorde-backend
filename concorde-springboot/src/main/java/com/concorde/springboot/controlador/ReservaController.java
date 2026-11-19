package com.concorde.springboot.controlador;

import com.concorde.springboot.modelo.Pago;
import com.concorde.springboot.modelo.Reserva;
import com.concorde.springboot.modelo.Viaje;
import com.concorde.springboot.repositorio.PagoRepository;
import com.concorde.springboot.repositorio.ReservaRepository;
import com.concorde.springboot.repositorio.ViajeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    @Autowired
    private ReservaRepository repository;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private ViajeRepository viajeRepository;

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
    @Transactional
    public ResponseEntity<Reserva> crear(@RequestBody Reserva r) {
        // Si no llega la clase, se asume económica
        if (r.getClase() == null || r.getClase().isBlank()) {
            r.setClase("ECONOMICA");
        }

        Reserva guardada = repository.save(r);

        // Valor: el total de la reserva, o si no viene, el precio del viaje
        Float valor = null;
        if (guardada.getTotal() != null) {
            valor = guardada.getTotal().floatValue();
        } else if (guardada.getViaje() != null && guardada.getViaje().getIdViaje() != null) {
            Viaje viaje = viajeRepository.findById(guardada.getViaje().getIdViaje()).orElse(null);
            if (viaje != null) valor = viaje.getPrecio();
        }

        // Pago automático de la reserva
        Pago pago = new Pago();
        pago.setReserva(guardada);
        pago.setMetodoPago("TARJETA");
        pago.setValorPagado(valor != null ? valor : 0f);
        pago.setFechaPago(LocalDate.now());
        pago.setEstado("PROCESADO");
        pagoRepository.save(pago);

        return ResponseEntity.status(HttpStatus.CREATED).body(guardada);
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
