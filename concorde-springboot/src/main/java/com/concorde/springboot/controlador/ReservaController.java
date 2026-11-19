
package com.concorde.springboot.controlador;

import com.concorde.springboot.modelo.*;
import com.concorde.springboot.repositorio.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    @Autowired
    private ReservaRepository repository;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private ViajeRepository viajeRepository;

    @Autowired
    private AsientoRepository asientoRepository;

    @Autowired
    private DetalleReservaRepository detalleRepository;

    @Autowired
    private EntityManager entityManager;

    @GetMapping
    public List<Reserva> listar() {
        return repository.findAll(
            PageRequest.of(0, 100)
        ).getContent();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Reserva> obtener(
            @PathVariable Integer id) {

        return repository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Reserva> crear(
            @RequestBody ReservaCompraRequest datos) {

        if (datos.getUsuario() == null ||
            datos.getUsuario().getIdUsuario() == null ||
            datos.getViaje() == null ||
            datos.getViaje().getIdViaje() == null) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Falta el usuario o el viaje"
            );
        }

        List<String> numeros = datos.getNumerosAsientos();

        if (numeros == null || numeros.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Debes seleccionar al menos un asiento"
            );
        }

        // Bloquear el viaje mientras se asignan asientos.
        // Las compras que utilicen este mismo flujo
        // se procesan una por una para ese viaje.
        Viaje viaje = entityManager.find(
            Viaje.class,
            datos.getViaje().getIdViaje(),
            LockModeType.PESSIMISTIC_WRITE
        );

        if (viaje == null || viaje.getVehiculo() == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El viaje no existe o no tiene vehículo"
            );
        }

        if (!"PROGRAMADO".equalsIgnoreCase(
                viaje.getEstadoViaje())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "El viaje no está disponible"
            );
        }

        Integer idVehiculo =
            viaje.getVehiculo().getIdVehiculo();

        List<Asiento> asientosVehiculo =
            asientoRepository.findByVehiculo_IdVehiculo(
                idVehiculo
            );

        Map<String, Asiento> mapa = new HashMap<>();

        for (Asiento a : asientosVehiculo) {
            if (a.getNumeroAsiento() != null) {
                mapa.put(
                    a.getNumeroAsiento().trim(),
                    a
                );
            }
        }

        List<Asiento> seleccionados = new ArrayList<>();
        Set<Integer> idsSeleccionados = new HashSet<>();

        for (String numero : numeros) {

            if (numero == null || numero.isBlank()) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Número de asiento inválido"
                );
            }

            Asiento asiento = mapa.get(numero.trim());

            if (asiento == null) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El asiento " + numero +
                    " no pertenece al vehículo"
                );
            }

            if (!Boolean.TRUE.equals(
                    asiento.getDisponible())) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El asiento " + numero +
                    " no está habilitado"
                );
            }

            if (!idsSeleccionados.add(
                    asiento.getIdAsiento())) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Asiento repetido: " + numero
                );
            }

            // Comprobar ocupación en ESTE viaje.
            Number ocupados = (Number)
                entityManager.createNativeQuery("""
                    SELECT COUNT(*)
                    FROM detalle_reserva d
                    JOIN reserva r
                      ON r.id_reserva = d.id_reserva
                    WHERE r.id_viaje = :viaje
                      AND d.id_asiento = :asiento
                      AND UPPER(COALESCE(r.estado, ''))
                          NOT IN ('CANCELADA', 'CANCELADO')
                    """)
                .setParameter(
                    "viaje", viaje.getIdViaje()
                )
                .setParameter(
                    "asiento", asiento.getIdAsiento()
                )
                .getSingleResult();

            if (ocupados.longValue() > 0) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El asiento " + numero +
                    " ya está reservado para este viaje"
                );
            }

            seleccionados.add(asiento);
        }

        if (datos.getClase() == null ||
            datos.getClase().isBlank()) {
            datos.setClase("ECONOMICA");
        }

        Reserva reserva = new Reserva();
        reserva.setUsuario(datos.getUsuario());
        reserva.setViaje(viaje);
        reserva.setClase(datos.getClase());
        reserva.setTotal(datos.getTotal());
        reserva.setEstado("CONFIRMADA");
        reserva.setFechaInicial(LocalDate.now());

        Reserva guardada = repository.save(reserva);

        // Guardar un detalle por cada asiento.
        for (Asiento asiento : seleccionados) {
            DetalleReserva detalle =
                new DetalleReserva();

            detalle.setReserva(guardada);
            detalle.setAsiento(asiento);
            detalle.setAsientoAsignado(true);

            detalleRepository.save(detalle);
        }

        // Mantener la creación del pago existente.
        Float valor = null;

        if (guardada.getTotal() != null) {
            valor = guardada.getTotal().floatValue();
        } else {
            valor = viaje.getPrecio();
        }

        Pago pago = new Pago();
        pago.setReserva(guardada);
        pago.setMetodoPago("TARJETA");
        pago.setValorPagado(
            valor != null ? valor : 0f
        );
        pago.setFechaPago(LocalDate.now());
        pago.setEstado("PROCESADO");

        pagoRepository.save(pago);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(guardada);
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

            if (datos.getClase() != null) {
                e.setClase(datos.getClase());
            }

            if (datos.getTotal() != null) {
                e.setTotal(datos.getTotal());
            }

            return ResponseEntity.ok(
                repository.save(e)
            );

        }).orElse(ResponseEntity.notFound().build());
    }

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
