package com.concorde.springboot.repositorio;

import com.concorde.springboot.modelo.Asiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AsientoRepository extends JpaRepository<Asiento, Integer> {
    List<Asiento> findByVehiculo_IdVehiculo(Integer idVehiculo);
}
