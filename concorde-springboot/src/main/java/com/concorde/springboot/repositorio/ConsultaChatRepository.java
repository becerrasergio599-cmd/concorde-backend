package com.concorde.springboot.repositorio;

import com.concorde.springboot.modelo.ConsultaChat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConsultaChatRepository extends JpaRepository<ConsultaChat, Integer> {
    List<ConsultaChat> findByEstadoInOrderByActualizadoEnDesc(List<String> estados);
    List<ConsultaChat> findAllByOrderByActualizadoEnDesc();
}
