
package com.concorde.springboot.controlador;

import com.concorde.springboot.modelo.Asiento;
import com.concorde.springboot.modelo.DetalleReserva;
import com.concorde.springboot.modelo.Pago;
import com.concorde.springboot.modelo.Reserva;
import com.concorde.springboot.modelo.Usuario;
import com.concorde.springboot.modelo.Viaje;

import com.concorde.springboot.repositorio.PagoRepository;
import com.concorde.springboot.repositorio.ReservaRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    @Autowired
    private ReservaRepository repository;

    @Autowired
    private PagoRepository pagoRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // ==========================================
    // LISTAR RESERVAS
    // ==========================================

    @GetMapping
    public List<Reserva> listar() {
        return repository.findAll(
            PageRequest.of(0, 100)
        ).getContent();
    }

    // ==========================================
    // OBTENER RESERVA POR ID
    // ==========================================

    @GetMapping("/{id}")
    public ResponseEntity<Reserva> obtener(
            @PathVariable Integer id) {

        return repository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // ==========================================
    // CREAR RESERVA, ASIENTOS Y PAGO
    // ==========================================

    @PostMapping
    @Transactional
    public ResponseEntity<Reserva> crear(
            @RequestBody Map<String, Object> cuerpo) {

        // Obtener usuario y viaje del JSON
        Object usuarioRaw = cuerpo.get("usuario");
        Object viajeRaw = cuerpo.get("viaje");

        if (!(usuarioRaw instanceof Map<?, ?> usuarioMap)
                || !(viajeRaw instanceof Map<?, ?> viajeMap)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Falta el usuario o el viaje"
            );
        }

        Integer idUsuario = convertirEntero(
            usuarioMap.get("idUsuario"),
            "idUsuario"
        );

        Integer idViaje = convertirEntero(
            viajeMap.get("idViaje"),
            "idViaje"
        );

        // Obtener asientos seleccionados
        Object asientosRaw = cuerpo.get("numerosAsientos");

        if (!(asientosRaw instanceof List<?> listaRaw)
                || listaRaw.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Debes seleccionar al menos un asiento"
            );
        }

        List<String> numerosAsientos = new ArrayList<>();

        for (Object valor : listaRaw) {
            if (valor == null
                    || String.valueOf(valor).isBlank()) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Número de asiento inválido"
                );
            }

            numerosAsientos.add(
                String.valueOf(valor).trim()
            );
        }

        // Comprobar usuario real
        Usuario usuario = entityManager.find(
            Usuario.class,
            idUsuario
        );

        if (usuario == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El usuario no existe"
            );
        }

        // Bloquear viaje durante la compra
        Viaje viaje = entityManager.find(
            Viaje.class,
            idViaje,
            LockModeType.PESSIMISTIC_WRITE
        );

        if (viaje == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "El viaje no existe"
            );
        }

        if (viaje.getVehiculo() == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El viaje no tiene vehículo asignado"
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

        // Buscar asientos reales del vehículo
        List<Asiento> asientosVehiculo =
            entityManager.createQuery(
                "SELECT a FROM Asiento a " +
                "WHERE a.vehiculo.idVehiculo = :idVehiculo",
                Asiento.class
            )
            .setParameter("idVehiculo", idVehiculo)
            .getResultList();

        List<Asiento> seleccionados = new ArrayList<>();
        Set<Integer> idsSeleccionados = new HashSet<>();

        for (String numero : numerosAsientos) {

            Asiento asiento = null;

            for (Asiento actual : asientosVehiculo) {
                if (actual.getNumeroAsiento() != null
                        && actual.getNumeroAsiento()
                            .trim().equalsIgnoreCase(numero)) {
                    asiento = actual;
                    break;
                }
            }

            if (asiento == null) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El asiento " + numero +
                    " no existe en este vehículo"
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

            // Comprobar que el asiento no esté
            // ocupado en este viaje específico
            Number ocupados = (Number)
                entityManager.createNativeQuery(
                    "SELECT COUNT(*) " +
                    "FROM detalle_reserva d " +
                    "JOIN reserva r " +
                    "ON r.id_reserva = d.id_reserva " +
                    "WHERE r.id_viaje = :idViaje " +
                    "AND d.id_asiento = :idAsiento " +
                    "AND UPPER(COALESCE(r.estado, '')) " +
                    "NOT IN ('CANCELADA', 'CANCELADO')"
                )
                .setParameter("idViaje", idViaje)
                .setParameter(
                    "idAsiento",
                    asiento.getIdAsiento()
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

        // ======================================
        // CREAR RESERVA
        // ======================================

        String clase = cuerpo.get("clase") == null
            ? "ECONOMICA"
            : String.valueOf(cuerpo.get("clase"))
                .trim().toUpperCase();

        if (!clase.equals("ECONOMICA")
                && !clase.equals("PREMIUM")) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Clase de servicio inválida"
            );
        }

        Integer total = convertirEntero(
            cuerpo.get("total"),
            "total"
        );

        if (total <= 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El total debe ser mayor que cero"
            );
        }

        Reserva reserva = new Reserva();
        reserva.setUsuario(usuario);
        reserva.setViaje(viaje);
        reserva.setFechaInicial(LocalDate.now());
        reserva.setEstado("CONFIRMADA");
        reserva.setClase(clase);
        reserva.setTotal(total);

        Reserva guardada = repository.save(reserva);

        // ======================================
        // GUARDAR DETALLES DE RESERVA
        // ======================================

        for (Asiento asiento : seleccionados) {

            DetalleReserva detalle =
                new DetalleReserva();

            detalle.setReserva(guardada);
            detalle.setAsiento(asiento);
            detalle.setAsientoAsignado(true);

            entityManager.persist(detalle);
        }

        // ======================================
        // CREAR PAGO
        // ======================================

        Pago pago = new Pago();

        pago.setReserva(guardada);
        pago.setMetodoPago("TARJETA");
        pago.setValorPagado(total.floatValue());
        pago.setFechaPago(LocalDate.now());
        pago.setEstado("PROCESADO");

        pagoRepository.save(pago);

        // Se guardan reserva, detalles y pago
        // en una misma transacción
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(guardada);
    }

    // ==========================================
    // ACTUALIZAR RESERVA
    // ==========================================

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Reserva> actualizar(
            @PathVariable Integer id,
            @RequestBody Reserva datos) {

        return repository.findById(id).map(e -> {

            // No permitir cambiar el viaje desde
            // este endpoint porque invalidaría
            // los asientos ya asignados.

            if (datos.getUsuario() != null) {
                e.setUsuario(datos.getUsuario());
            }

            if (datos.getFechaInicial() != null) {
                e.setFechaInicial(
                    datos.getFechaInicial()
                );
            }

            if (datos.getEstado() != null) {
                e.setEstado(datos.getEstado());
            }

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

    // ==========================================
    // ELIMINAR RESERVA
    // ==========================================

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> eliminar(
            @PathVariable Integer id) {

        Reserva reserva = repository.findById(id)
            .orElse(null);

        if (reserva == null) {
            return ResponseEntity.notFound().build();
        }

        // Eliminar detalles asociados primero
        entityManager.createQuery(
            "DELETE FROM DetalleReserva d " +
            "WHERE d.reserva.idReserva = :id"
        )
        .setParameter("id", id)
        .executeUpdate();

        // Eliminar pagos asociados primero
        entityManager.createQuery(
            "DELETE FROM Pago p " +
            "WHERE p.reserva.idReserva = :id"
        )
        .setParameter("id", id)
        .executeUpdate();

        repository.delete(reserva);

        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // CONVERTIR VALORES A ENTERO
    // ==========================================

    private Integer convertirEntero(
            Object valor,
            String campo) {

        if (valor == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Falta el campo " + campo
            );
        }

        try {
            return Integer.valueOf(
                String.valueOf(valor)
            );
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Valor inválido para " + campo
            );
        }
    }
}
