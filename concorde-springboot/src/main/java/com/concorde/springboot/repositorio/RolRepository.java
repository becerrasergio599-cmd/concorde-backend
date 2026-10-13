package com.concorde.springboot.repositorio;

import com.concorde.springboot.modelo.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolRepository extends JpaRepository<Rol, Integer> {
}
