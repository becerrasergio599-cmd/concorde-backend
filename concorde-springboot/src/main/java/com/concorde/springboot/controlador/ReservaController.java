
package com.concorde.springboot.controlador;

import com.concorde.springboot.modelo.Asiento;
import com.concorde.springboot.modelo.DetalleReserva;
import com.concorde.springboot.modelo.Pago;
import com.concorde.springboot.modelo.Reserva;
import com.concorde.springboot.modelo.Usuario;
import com.concorde.springboot.modelo.Viaje;

import com.concorde.springboot.repositorio.PagoRepository;
import com.concorde.springboot.repositorio.ReservaRepository;
import com.concorde.springboot.repositorio.ViajeRepository;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    @Autowired
    private ReservaRepository repository;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private ViajeRepository viajeRepository;

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
    // OBTENER RESERVA
    // ==========================================

    @GetMapping("/{id}")
    public ResponseEntity<Reserva> obtener(
            @PathVariable Integer id) {

        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ==========================================
    // CREAR RESERVA + PAGO + ASIENTOS
    // ==========================================

    @PostMapping
    @Transactional
    public ResponseEntity<Reserva> crear(
            @RequestBody JsonNode datos) {

        // 1. VALIDAR DATOS PRINCIPALES

        JsonNode usuarioJson = datos.path("usuario");
        JsonNode viajeJson = datos.path("viaje");
        JsonNode asientosJson = datos.path("numerosAsientos");

        if (!usuarioJson.path("idUsuario").canConvertToInt()
                || !viajeJson.path("idViaje").canConvertToInt()) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Falta el usuario o el viaje"
            );
        }

        int idUsuario = usuarioJson.path("idUsuario").asInt();
        int idViaje = viajeJson.path("idViaje").asInt();

        if (idUsuario <= 0 || idViaje <= 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Usuario o viaje no valido"
            );
        }

        // 2. VERIFICAR IDENTIDAD DEL COMPRADOR

        Usuario usuario = entityManager.find(
            Usuario.class, idUsuario
        );

        if (usuario == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Usuario no encontrado"
            );
        }

        Authentication autenticacion =
            SecurityContextHolder.getContext()
                .getAuthentication();

        if (autenticacion == null
                || !autenticacion.isAuthenticated()) {

            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Debes iniciar sesion"
            );
        }

        String correoAutenticado =
            autenticacion.getName();

        if (usuario.getCorreoElectronico() == null
                || !usuario.getCorreoElectronico()
                    .equalsIgnoreCase(correoAutenticado)) {

            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "No puedes reservar con otro usuario"
            );
        }

        // 3. BLOQUEAR EL VIAJE DURANTE LA COMPRA
        // Evita que dos compras simultaneas
        // asignen el mismo asiento.

        Viaje viaje = entityManager.find(
            Viaje.class,
            idViaje,
            LockModeType.PESSIMISTIC_WRITE
        );

        if (viaje == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Viaje no encontrado"
            );
        }

        if (viaje.getVehiculo() == null
                || viaje.getVehiculo().getIdVehiculo() == null) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El viaje no tiene vehiculo asignado"
            );
        }

        int idVehiculo =
            viaje.getVehiculo().getIdVehiculo();

        // 4. RECIBIR ASIENTOS SELECCIONADOS

        if (!asientosJson.isArray()
                || asientosJson.isEmpty()) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Debes seleccionar al menos un asiento"
            );
        }

        List<String> numeros = new ArrayList<>();
        Set<String> numerosUnicos = new HashSet<>();

        for (JsonNode elemento : asientosJson) {

            if (!elemento.isTextual()) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Formato de asiento no valido"
                );
            }

            String numero = elemento.asText()
                .trim()
                .toUpperCase(Locale.ROOT);

            if (numero.isEmpty()
                    || !numerosUnicos.add(numero)) {

                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Hay asientos vacios o repetidos"
                );
            }

            numeros.add(numero);
        }

        // 5. BUSCAR ASIENTOS REALES DEL VEHICULO

        List<Asiento> asientosVehiculo =
            entityManager.createQuery(
                "SELECT a FROM Asiento a " +
                "WHERE a.vehiculo.idVehiculo = :vehiculo",
                Asiento.class
            )
            .setParameter("vehiculo", idVehiculo)
            .getResultList();

        List<Asiento> seleccionados =
            new ArrayList<>();

        for (String numero : numeros) {

            Asiento encontrado = null;

            for (Asiento a : asientosVehiculo) {

                if (a.getNumeroAsiento() != null
                        && a.getNumeroAsiento()
                            .trim()
                            .equalsIgnoreCase(numero)) {

                    encontrado = a;
                    break;
                }
            }

            if (encontrado == null) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El asiento " + numero +
                    " no existe en el vehiculo"
                );
            }

            if (Boolean.FALSE.equals(
                    encontrado.getDisponible())) {

                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El asiento " + numero +
                    " no esta disponible"
                );
            }

            // 6. COMPROBAR SI YA FUE RESERVADO
            // EN ESTE MISMO VIAJE

            Long ocupaciones =
                entityManager.createQuery(
                    "SELECT COUNT(d) " +
                    "FROM DetalleReserva d " +
                    "JOIN d.reserva r " +
                    "WHERE r.viaje.idViaje = :viaje " +
                    "AND d.asiento.idAsiento = :asiento " +
                    "AND UPPER(r.estado) <> 'CANCELADA'",
                    Long.class
                )
                .setParameter("viaje", idViaje)
                .setParameter(
                    "asiento",
                    encontrado.getIdAsiento()
                )
                .getSingleResult();

            if (ocupaciones > 0) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El asiento " + numero +
                    " ya esta reservado para este viaje"
                );
            }

            seleccionados.add(encontrado);
        }

        // 7. VALIDAR TOTAL

        JsonNode totalJson = datos.path("total");

        if (!totalJson.canConvertToInt()
                || totalJson.asInt() <= 0) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El total de la compra no es valido"
            );
        }

        int total = totalJson.asInt();

        // 8. GUARDAR RESERVA

        Reserva reserva = new Reserva();

        reserva.setUsuario(usuario);
        reserva.setViaje(viaje);
        reserva.setFechaInicial(LocalDate.now());
        reserva.setEstado("CONFIRMADA");

        String clase = datos.path("clase")
            .asText("ECONOMICA")
            .trim()
            .toUpperCase(Locale.ROOT);

        if (!clase.equals("PREMIUM")) {
            clase = "ECONOMICA";
        }

        reserva.setClase(clase);
        reserva.setTotal(total);

        Reserva guardada = repository.saveAndFlush(
            reserva
        );

        // 9. GUARDAR DETALLE DE CADA ASIENTO

        for (Asiento a : seleccionados) {

            DetalleReserva detalle =
                new DetalleReserva();

            detalle.setReserva(guardada);
            detalle.setAsiento(a);
            detalle.setAsientoAsignado(true);

            entityManager.persist(detalle);
        }

        // 10. GUARDAR PAGO
        // Conserva la simulacion de pago
        // que ya utilizaba Concorde.

        Pago pago = new Pago();

        pago.setReserva(guardada);
        pago.setMetodoPago("TARJETA");
        pago.setValorPagado((float) total);
        pago.setFechaPago(LocalDate.now());
        pago.setEstado("PROCESADO");

        pagoRepository.save(pago);

        // 11. CONFIRMAR RESULTADO

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(guardada);
    }

    // ==========================================
    // ACTUALIZAR RESERVA
    // ==========================================

    @PutMapping("/{id}")
    public ResponseEntity<Reserva> actualizar(
            @PathVariable Integer id,
            @RequestBody Reserva datos) {

        return repository.findById(id).map(e -> {

            e.setUsuario(datos.getUsuario());
            e.setViaje(datos.getViaje());
            e.setFechaInicial(
                datos.getFechaInicial()
            );
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

    // ==========================================
    // ELIMINAR RESERVA
    // ==========================================

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
