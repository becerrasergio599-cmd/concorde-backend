package com.concorde.springboot.modelo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// Cada mensaje individual dentro de una ConsultaChat.
@Entity
@Table(name = "mensaje_chat")
public class MensajeChat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idMensaje;

    @ManyToOne
    @JoinColumn(name = "id_consulta")
    private ConsultaChat consulta;

    // CLIENTE | BOT | AGENTE
    private String remitente;

    @Column(columnDefinition = "TEXT")
    private String contenido;

    private LocalDateTime enviadoEn;

    public Integer getIdMensaje() {
        return idMensaje;
    }

    public void setIdMensaje(Integer idMensaje) {
        this.idMensaje = idMensaje;
    }

    public ConsultaChat getConsulta() {
        return consulta;
    }

    public void setConsulta(ConsultaChat consulta) {
        this.consulta = consulta;
    }

    public String getRemitente() {
        return remitente;
    }

    public void setRemitente(String remitente) {
        this.remitente = remitente;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public LocalDateTime getEnviadoEn() {
        return enviadoEn;
    }

    public void setEnviadoEn(LocalDateTime enviadoEn) {
        this.enviadoEn = enviadoEn;
    }
}
