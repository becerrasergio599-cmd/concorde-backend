
package com.concorde.springboot.controlador;

import com.concorde.springboot.modelo.Asiento;
import com.concorde.springboot.modelo.DetalleReserva;
import com.concorde.springboot.modelo.Viaje;
import com.concorde.springboot.repositorio.AsientoRepository;
import com.concorde.springboot.repositorio.DetalleReservaRepository;
import com.concorde.springboot.repositorio.ViajeRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/asientos")
public class AsientoController {

    @Autowired
    private AsientoRepository repository;

    @Autowired
    private ViajeRepository viajeRepository;

    @Autowired
    private DetalleReservaRepository detalleReservaRepository;

    // Listar todos los asientos
    @GetMapping
    public List<Asiento> listar() {
        return repository.findAll();
    }

    // Consultar un asiento por ID
    @GetMapping("/{id}")
    public ResponseEntity<Asiento> obtener(
            @PathVariable Integer id) {

        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Consultar disponibilidad real para un viaje
    @GetMapping("/viaje/{idViaje}")
    public ResponseEntity<?> asientosPorViaje(
            @PathVariable Integer idViaje) {

        Viaje viaje = viajeRepository.findById(idViaje)
                .orElse(null);

        if (viaje == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "error", "El viaje no existe"
                    ));
        }

        if (viaje.getVehiculo() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "El viaje no tiene vehículo"
                    ));
        }

        Integer idVehiculo =
                viaje.getVehiculo().getIdVehiculo();

        // Asientos físicos del vehículo
        List<Asiento> asientos = repository.findAll()
                .stream()
                .filter(a ->
                        a.getVehiculo() != null
                        && idVehiculo.equals(
                                a.getVehiculo().getIdVehiculo()
                        )
                )
                .toList();

        // Reservas de asientos del viaje
        List<DetalleReserva> detalles =
                detalleReservaRepository.findAll();

        List<Map<String, Object>> resultado =
                new ArrayList<>();

        for (Asiento asiento : asientos) {

            boolean reservado = detalles.stream()
                    .anyMatch(d ->
                            d.getAsiento() != null
                            && d.getReserva() != null
                            && d.getReserva().getViaje() != null
                            && d.getAsiento().getIdAsiento()
                                    .equals(asiento.getIdAsiento())
                            && idViaje.equals(
                                    d.getReserva()
                                            .getViaje()
                                            .getIdViaje()
                            )
                            && !"CANCELADA".equalsIgnoreCase(
                                    d.getReserva().getEstado()
                            )
                    );

            boolean disponibleFisicamente =
                    Boolean.TRUE.equals(
                            asiento.getDisponible()
                    );

            boolean disponible =
                    disponibleFisicamente && !reservado;

            Map<String, Object> fila =
                    new LinkedHashMap<>();

            fila.put(
                    "idAsiento",
                    asiento.getIdAsiento()
            );

            fila.put(
                    "numeroAsiento",
                    asiento.getNumeroAsiento()
            );

            fila.put(
                    "tipo",
                    asiento.getTipo() == null
                            ? "ESTANDAR"
                            : asiento.getTipo()
            );

            fila.put("disponible", disponible);
            fila.put("ocupado", reservado);

            fila.put(
                    "estado",
                    reservado
                            ? "OCUPADO"
                            : disponible
                                    ? "DISPONIBLE"
                                    : "NO DISPONIBLE"
            );

            resultado.add(fila);
        }

        return ResponseEntity.ok(resultado);
    }

    // Crear asiento
    @PostMapping
    public ResponseEntity<Asiento> crear(
            @RequestBody Asiento a) {

        if (a.getTipo() == null
                || a.getTipo().isBlank()) {
            a.setTipo("ESTANDAR");
        }

        if (a.getDisponible() == null) {
            a.setDisponible(true);
        }

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(repository.save(a));
    }

    // Actualizar asiento
    @PutMapping("/{id}")
    public ResponseEntity<Asiento> actualizar(
            @PathVariable Integer id,
            @RequestBody Asiento datos) {

        return repository.findById(id)
                .map(e -> {

                    e.setVehiculo(datos.getVehiculo());
                    e.setNumeroAsiento(
                            datos.getNumeroAsiento()
                    );
                    e.setTipo(datos.getTipo());
                    e.setDisponible(
                            datos.getDisponible()
                    );

                    return ResponseEntity.ok(
                            repository.save(e)
                    );

                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Eliminar asiento
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Integer id) {

        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        repository.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}
