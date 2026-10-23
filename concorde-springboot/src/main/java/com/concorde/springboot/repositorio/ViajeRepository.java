package com.concorde.springboot.repositorio;

import com.concorde.springboot.modelo.Viaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ViajeRepository extends JpaRepository<Viaje, Integer> {

    @Query(value = """
        SELECT v.* FROM viaje v
        JOIN ruta r ON v.id_ruta = r.id_ruta
        JOIN terminal o ON r.id_terminal_origen = o.id_terminal
        JOIN terminal d ON r.id_terminal_destino = d.id_terminal
        WHERE LOWER(translate(o.ciudad, 'áéíóúÁÉÍÓÚ', 'aeiouAEIOU')) = LOWER(:origen)
          AND LOWER(translate(d.ciudad, 'áéíóúÁÉÍÓÚ', 'aeiouAEIOU')) = LOWER(:destino)
          AND v.fecha_viaje = :fecha
          AND v.estado_viaje = 'PROGRAMADO'
        ORDER BY v.hora_salida
        """, nativeQuery = true)
    List<Viaje> buscar(@Param("origen") String origen,
                       @Param("destino") String destino,
                       @Param("fecha") LocalDate fecha);
}
