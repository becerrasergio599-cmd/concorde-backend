package com.concorde.springboot.repositorio;

import com.concorde.springboot.modelo.DetalleReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DetalleReservaRepository extends JpaRepository<DetalleReserva, Integer> {
}
