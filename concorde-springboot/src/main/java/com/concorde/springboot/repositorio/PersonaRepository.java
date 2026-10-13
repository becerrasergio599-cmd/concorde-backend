package com.concorde.springboot.repositorio;

import com.concorde.springboot.modelo.Persona;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonaRepository extends JpaRepository<Persona, Integer> {
}
