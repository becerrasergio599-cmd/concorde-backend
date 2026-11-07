package com.concorde.springboot.controlador;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/viajes")
public class ViajeBusquedaController {

    private final JdbcTemplate jdbc;

    public ViajeBusquedaController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping("/buscar")
    public List<Map<String, Object>> buscar(@RequestParam String origen,
                                            @RequestParam String destino,
                                            @RequestParam String fecha) {
        String sql = """
            SELECT v.id_viaje                      AS "idViaje",
                   to_char(v.hora_salida,'HH24:MI') AS "salida",
                   v.precio                        AS "precio",
                   t_o.nombre                      AS "terminalOrigen",
                   t_d.nombre                      AS "terminalDestino"
            FROM viaje v
            JOIN ruta r      ON v.id_ruta = r.id_ruta
            JOIN terminal t_o ON r.id_terminal_origen  = t_o.id_terminal
            JOIN terminal t_d ON r.id_terminal_destino = t_d.id_terminal
            WHERE t_o.ciudad = ? AND t_d.ciudad = ?
              AND v.fecha_viaje = ?
              AND v.estado_viaje = 'PROGRAMADO'
            ORDER BY v.hora_salida
            """;
        return jdbc.queryForList(sql, origen, destino,
                java.sql.Date.valueOf(java.time.LocalDate.parse(fecha)));
    }
}