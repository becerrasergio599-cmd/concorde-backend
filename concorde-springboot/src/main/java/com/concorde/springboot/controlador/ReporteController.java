package com.concorde.springboot.controlador;

import com.concorde.springboot.modelo.*;
import com.concorde.springboot.repositorio.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

// Todos los reportes se calculan aquí mismo con streams de Java, a partir
// de los datos que ya traen los repositorios -- sin SQL nativo ni queries
// complejas, para que sea fácil de explicar y de seguir.
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    @Autowired private PagoRepository pagoRepository;
    @Autowired private ViajeRepository viajeRepository;
    @Autowired private ReservaRepository reservaRepository;
    @Autowired private DetalleReservaRepository detalleReservaRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    // ── 1. Ingresos por ruta ──────────────────────────────
    @GetMapping("/ingresos-por-ruta")
    public List<Map<String, Object>> ingresosPorRuta() {
        List<Pago> pagos = pagoRepository.findAll().stream()
                .filter(p -> "PROCESADO".equals(p.getEstado()))
                .collect(Collectors.toList());

        Map<String, Double> totales = new LinkedHashMap<>();
        for (Pago pago : pagos) {
            Ruta ruta = pago.getReserva().getViaje().getRuta();
            String etiqueta = ruta.getTerminalOrigen().getCiudad() + " → " + ruta.getTerminalDestino().getCiudad();
            totales.merge(etiqueta, pago.getValorPagado().doubleValue(), Double::sum);
        }

        return totales.entrySet().stream()
                .map(e -> Map.<String, Object>of("ruta", e.getKey(), "ingresos", e.getValue()))
                .sorted((a, b) -> Double.compare((double) b.get("ingresos"), (double) a.get("ingresos")))
                .collect(Collectors.toList());
    }

    // ── 2. Ingresos por fecha ─────────────────────────────
    @GetMapping("/ingresos-por-fecha")
    public List<Map<String, Object>> ingresosPorFecha() {
        List<Pago> pagos = pagoRepository.findAll().stream()
                .filter(p -> "PROCESADO".equals(p.getEstado()))
                .collect(Collectors.toList());

        Map<LocalDate, Double> totales = new TreeMap<>();
        for (Pago pago : pagos) {
            totales.merge(pago.getFechaPago(), pago.getValorPagado().doubleValue(), Double::sum);
        }

        return totales.entrySet().stream()
                .map(e -> Map.<String, Object>of("fecha", e.getKey().toString(), "ingresos", e.getValue()))
                .collect(Collectors.toList());
    }

    // ── 3. Ocupación de viajes (incluye cuáles están agotados) ──
    @GetMapping("/ocupacion-viajes")
    public List<Map<String, Object>> ocupacionViajes() {
        List<Viaje> viajes = viajeRepository.findAll();
        List<DetalleReserva> detalles = detalleReservaRepository.findAll();

        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Viaje viaje : viajes) {
            int capacidad = viaje.getVehiculo().getCapacidad();

            long ocupados = detalles.stream()
                    .filter(d -> d.getReserva().getViaje().getIdViaje().equals(viaje.getIdViaje()))
                    .filter(d -> !"CANCELADA".equals(d.getReserva().getEstado()))
                    .count();

            long disponibles = Math.max(0, capacidad - ocupados);

            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("idViaje", viaje.getIdViaje());
            fila.put("ruta", viaje.getRuta().getTerminalOrigen().getCiudad() + " → " + viaje.getRuta().getTerminalDestino().getCiudad());
            fila.put("fechaViaje", viaje.getFechaViaje().toString());
            fila.put("horaSalida", viaje.getHoraSalida().toString());
            fila.put("capacidad", capacidad);
            fila.put("ocupados", ocupados);
            fila.put("disponibles", disponibles);
            fila.put("agotado", disponibles == 0);
            resultado.add(fila);
        }
        return resultado;
    }

    // ── 3b. Solo los viajes agotados (para el aviso) ──────
    @GetMapping("/viajes-agotados")
    public List<Map<String, Object>> viajesAgotados() {
        return ocupacionViajes().stream()
                .filter(f -> (boolean) f.get("agotado"))
                .filter(f -> !"CANCELADO".equals(
                        viajeRepository.findById((Integer) f.get("idViaje")).map(Viaje::getEstadoViaje).orElse("")))
                .collect(Collectors.toList());
    }

    // ── 4. Viajes por estado ──────────────────────────────
    @GetMapping("/viajes-por-estado")
    public Map<String, Long> viajesPorEstado() {
        return viajeRepository.findAll().stream()
                .collect(Collectors.groupingBy(Viaje::getEstadoViaje, LinkedHashMap::new, Collectors.counting()));
    }

    // ── 5. Reservas por estado ─────────────────────────────
    @GetMapping("/reservas-por-estado")
    public Map<String, Long> reservasPorEstado() {
        return reservaRepository.findAll().stream()
                .collect(Collectors.groupingBy(Reserva::getEstado, LinkedHashMap::new, Collectors.counting()));
    }

    // ── 6. Usuarios por rol ────────────────────────────────
    @GetMapping("/usuarios-por-rol")
    public Map<String, Long> usuariosPorRol() {
        return usuarioRepository.findAll().stream()
                .collect(Collectors.groupingBy(u -> u.getRol().getNombreRol(), LinkedHashMap::new, Collectors.counting()));
    }

    // ── 7. Rutas más vendidas (por cantidad de reservas) ───
    @GetMapping("/rutas-mas-vendidas")
    public List<Map<String, Object>> rutasMasVendidas() {
        List<Reserva> reservas = reservaRepository.findAll().stream()
                .filter(r -> !"CANCELADA".equals(r.getEstado()))
                .collect(Collectors.toList());

        Map<String, Long> conteo = new LinkedHashMap<>();
        for (Reserva reserva : reservas) {
            Ruta ruta = reserva.getViaje().getRuta();
            String etiqueta = ruta.getTerminalOrigen().getCiudad() + " → " + ruta.getTerminalDestino().getCiudad();
            conteo.merge(etiqueta, 1L, Long::sum);
        }

        return conteo.entrySet().stream()
                .map(e -> Map.<String, Object>of("ruta", e.getKey(), "reservas", e.getValue()))
                .sorted((a, b) -> Long.compare((long) b.get("reservas"), (long) a.get("reservas")))
                .collect(Collectors.toList());
    }
}
