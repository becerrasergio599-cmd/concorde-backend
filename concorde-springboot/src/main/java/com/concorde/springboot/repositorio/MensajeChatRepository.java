package com.concorde.springboot.repositorio;

import com.concorde.springboot.modelo.MensajeChat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MensajeChatRepository extends JpaRepository<MensajeChat, Integer> {
    List<MensajeChat> findByConsulta_IdConsultaOrderByEnviadoEnAsc(Integer idConsulta);
}
