package com.concorde.springboot.repositorio;

import com.concorde.springboot.modelo.Asiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AsientoRepository extends JpaRepository<Asiento, Integer> {
}
