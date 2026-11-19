
package com.concorde.springboot.controlador;

import com.concorde.springboot.modelo.*;
import com.concorde.springboot.repositorio.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // ==========================================
    // 1. INGRESOS POR RUTA
    // ==========================================

    @GetMapping("/ingresos-por-ruta")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> ingresosPorRuta() {

        String sql = """
            SELECT
                origen.ciudad || ' → ' ||
                destino.ciudad AS ruta,
                COALESCE(SUM(p.valor_pagado), 0) AS ingresos
            FROM pago p
            JOIN reserva r
                ON p.id_reserva = r.id_reserva
            JOIN viaje v
                ON r.id_viaje = v.id_viaje
            JOIN ruta ru
                ON v.id_ruta = ru.id_ruta
            JOIN terminal origen
                ON ru.id_terminal_origen = origen.id_terminal
            JOIN terminal destino
                ON ru.id_terminal_destino = destino.id_terminal
            WHERE p.estado = 'PROCESADO'
            GROUP BY origen.ciudad, destino.ciudad
            ORDER BY ingresos DESC
            """;

        List<?> filas = entityManager
                .createNativeQuery(sql)
                .getResultList();

        List<Map<String, Object>> resultado =
                new ArrayList<>();

        for (Object fila : filas) {
            Object[] f = (Object[]) fila;

            Map<String, Object> dato =
                    new LinkedHashMap<>();

            dato.put("ruta", f[0]);
            dato.put("ingresos",
                    ((Number) f[1]).doubleValue());

            resultado.add(dato);
        }

        return resultado;
    }

    // ==========================================
    // 2. INGRESOS POR FECHA
    // ==========================================

    @GetMapping("/ingresos-por-fecha")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> ingresosPorFecha() {

        String sql = """
            SELECT
                fecha_pago,
                COALESCE(SUM(valor_pagado), 0)
                    AS ingresos
            FROM pago
            WHERE estado = 'PROCESADO'
              AND fecha_pago IS NOT NULL
            GROUP BY fecha_pago
            ORDER BY fecha_pago
            """;

        List<?> filas = entityManager
                .createNativeQuery(sql)
                .getResultList();

        List<Map<String, Object>> resultado =
                new ArrayList<>();

        for (Object fila : filas) {
            Object[] f = (Object[]) fila;

            Map<String, Object> dato =
                    new LinkedHashMap<>();

            dato.put("fecha", f[0].toString());
            dato.put("ingresos",
                    ((Number) f[1]).doubleValue());

            resultado.add(dato);
        }

        return resultado;
    }

    // ==========================================
    // CONSULTA OPTIMIZADA DE OCUPACION
    // ==========================================

    private String sqlOcupacion() {

        return """
            WITH capacidad_bus AS (
                SELECT
                    id_vehiculo,
                    COUNT(*) AS capacidad
                FROM asiento
                GROUP BY id_vehiculo
            ),
            asientos_ocupados AS (
                SELECT
                    r.id_viaje,
                    COUNT(DISTINCT d.id_asiento)
                        AS ocupados
                FROM detalle_reserva d
                JOIN reserva r
                    ON d.id_reserva = r.id_reserva
                WHERE COALESCE(
                    UPPER(r.estado), ''
                ) <> 'CANCELADA'
                GROUP BY r.id_viaje
            )
            SELECT
                v.id_viaje,
                origen.ciudad || ' → ' ||
                    destino.ciudad AS ruta,
                v.fecha_viaje,
                v.hora_salida,
                COALESCE(
                    cb.capacidad, ve.capacidad, 0
                ) AS capacidad,
                COALESCE(ao.ocupados, 0)
                    AS ocupados,
                GREATEST(
                    COALESCE(
                        cb.capacidad, ve.capacidad, 0
                    ) - COALESCE(ao.ocupados, 0),
                    0
                ) AS disponibles
            FROM viaje v
            JOIN vehiculo ve
                ON v.id_vehiculo = ve.id_vehiculo
            JOIN ruta ru
                ON v.id_ruta = ru.id_ruta
            JOIN terminal origen
                ON ru.id_terminal_origen =
                    origen.id_terminal
            JOIN terminal destino
                ON ru.id_terminal_destino =
                    destino.id_terminal
            LEFT JOIN capacidad_bus cb
                ON v.id_vehiculo = cb.id_vehiculo
            LEFT JOIN asientos_ocupados ao
                ON v.id_viaje = ao.id_viaje
            """;
    }

    private Map<String, Object> convertirOcupacion(
            Object[] f) {

        int capacidad =
                ((Number) f[4]).intValue();

        long ocupados =
                ((Number) f[5]).longValue();

        long disponibles =
                ((Number) f[6]).longValue();

        Map<String, Object> dato =
                new LinkedHashMap<>();

        dato.put("idViaje",
                ((Number) f[0]).intValue());

        dato.put("ruta", f[1]);

        dato.put("fechaViaje",
                f[2] != null ? f[2].toString() : "");

        dato.put("horaSalida",
                f[3] != null ? f[3].toString() : "");

        dato.put("capacidad", capacidad);
        dato.put("ocupados", ocupados);
        dato.put("disponibles", disponibles);

        dato.put("agotado",
                capacidad > 0 && disponibles == 0);

        return dato;
    }

    // ==========================================
    // 3. OCUPACION DE VIAJES
    // ==========================================

    @GetMapping("/ocupacion-viajes")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> ocupacionViajes() {

        String sql = sqlOcupacion()
                + " ORDER BY v.fecha_viaje DESC, "
                + "v.hora_salida DESC";

        List<?> filas = entityManager
                .createNativeQuery(sql)
                .getResultList();

        List<Map<String, Object>> resultado =
                new ArrayList<>();

        for (Object fila : filas) {
            resultado.add(
                convertirOcupacion((Object[]) fila)
            );
        }

        return resultado;
    }

    // ==========================================
    // 3B. VIAJES AGOTADOS
    // ==========================================

    @GetMapping("/viajes-agotados")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> viajesAgotados() {

        String sql = sqlOcupacion()
                + """
                  WHERE v.estado_viaje <> 'CANCELADO'
                  AND COALESCE(
                      cb.capacidad, ve.capacidad, 0
                  ) > 0
                  AND COALESCE(ao.ocupados, 0) >=
                      COALESCE(
                          cb.capacidad, ve.capacidad, 0
                      )
                  ORDER BY v.fecha_viaje,
                           v.hora_salida
                  """;

        List<?> filas = entityManager
                .createNativeQuery(sql)
                .getResultList();

        List<Map<String, Object>> resultado =
                new ArrayList<>();

        for (Object fila : filas) {
            resultado.add(
                convertirOcupacion((Object[]) fila)
            );
        }

        return resultado;
    }

    // ==========================================
    // 4. VIAJES POR ESTADO
    // ==========================================

    @GetMapping("/viajes-por-estado")
    @Transactional(readOnly = true)
    public Map<String, Long> viajesPorEstado() {

        String sql = """
            SELECT
                COALESCE(estado_viaje, 'SIN_ESTADO'),
                COUNT(*)
            FROM viaje
            GROUP BY estado_viaje
            ORDER BY COUNT(*) DESC
            """;

        List<?> filas = entityManager
                .createNativeQuery(sql)
                .getResultList();

        Map<String, Long> resultado =
                new LinkedHashMap<>();

        for (Object fila : filas) {
            Object[] f = (Object[]) fila;

            resultado.put(
                f[0].toString(),
                ((Number) f[1]).longValue()
            );
        }

        return resultado;
    }

    // ==========================================
    // 5. RESERVAS POR ESTADO
    // ==========================================

    @GetMapping("/reservas-por-estado")
    @Transactional(readOnly = true)
    public Map<String, Long> reservasPorEstado() {

        String sql = """
            SELECT
                COALESCE(estado, 'SIN_ESTADO'),
                COUNT(*)
            FROM reserva
            GROUP BY estado
            ORDER BY COUNT(*) DESC
            """;

        List<?> filas = entityManager
                .createNativeQuery(sql)
                .getResultList();

        Map<String, Long> resultado =
                new LinkedHashMap<>();

        for (Object fila : filas) {
            Object[] f = (Object[]) fila;

            resultado.put(
                f[0].toString(),
                ((Number) f[1]).longValue()
            );
        }

        return resultado;
    }

    // ==========================================
    // 6. USUARIOS POR ROL
    // ==========================================

    @GetMapping("/usuarios-por-rol")
    @Transactional(readOnly = true)
    public Map<String, Long> usuariosPorRol() {

        return usuarioRepository.findAll()
                .stream()
                .filter(u -> u.getRol() != null)
                .collect(Collectors.groupingBy(
                    u -> u.getRol().getNombreRol(),
                    LinkedHashMap::new,
                    Collectors.counting()
                ));
    }

    // ==========================================
    // 7. RUTAS MAS VENDIDAS
    // ==========================================

    @GetMapping("/rutas-mas-vendidas")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> rutasMasVendidas() {

        String sql = """
            SELECT
                origen.ciudad || ' → ' ||
                    destino.ciudad AS ruta,
                COUNT(*) AS reservas
            FROM reserva r
            JOIN viaje v
                ON r.id_viaje = v.id_viaje
            JOIN ruta ru
                ON v.id_ruta = ru.id_ruta
            JOIN terminal origen
                ON ru.id_terminal_origen =
                    origen.id_terminal
            JOIN terminal destino
                ON ru.id_terminal_destino =
                    destino.id_terminal
            WHERE COALESCE(
                UPPER(r.estado), ''
            ) <> 'CANCELADA'
            GROUP BY origen.ciudad, destino.ciudad
            ORDER BY reservas DESC
            """;

        List<?> filas = entityManager
                .createNativeQuery(sql)
                .getResultList();

        List<Map<String, Object>> resultado =
                new ArrayList<>();

        for (Object fila : filas) {
            Object[] f = (Object[]) fila;

            Map<String, Object> dato =
                    new LinkedHashMap<>();

            dato.put("ruta", f[0]);
            dato.put("reservas",
                    ((Number) f[1]).longValue());

            resultado.add(dato);
        }

        return resultado;
    }
}
