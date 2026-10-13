package com.concorde.springboot.modelo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// Representa una conversación de atención al cliente iniciada desde el
// chatbot. Mientras el bot puede responder solo, estado = BOT. Si el
// bot no entiende la consulta o el cliente pide un asesor, estado pasa
// a ESCALADA (esperando que un agente la tome) y luego a EN_ATENCION
// cuando un agente ya está respondiendo. CERRADA = conversación
// finalizada.
@Entity
@Table(name = "consulta_chat")
public class ConsultaChat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idConsulta;

    // Cliente logueado (puede ser null si el visitante escribe sin sesión)
    @ManyToOne
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    // Agente que tomó la conversación (null mientras no se asigna)
    @ManyToOne
    @JoinColumn(name = "id_agente")
    private Usuario agente;

    private String nombreContacto;
    private String correoContacto;
    private String asunto;

    // BOT | ESCALADA | EN_ATENCION | CERRADA
    private String estado;

    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;

    public Integer getIdConsulta() {
        return idConsulta;
    }

    public void setIdConsulta(Integer idConsulta) {
        this.idConsulta = idConsulta;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Usuario getAgente() {
        return agente;
    }

    public void setAgente(Usuario agente) {
        this.agente = agente;
    }

    public String getNombreContacto() {
        return nombreContacto;
    }

    public void setNombreContacto(String nombreContacto) {
        this.nombreContacto = nombreContacto;
    }

    public String getCorreoContacto() {
        return correoContacto;
    }

    public void setCorreoContacto(String correoContacto) {
        this.correoContacto = correoContacto;
    }

    public String getAsunto() {
        return asunto;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(LocalDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }

    public LocalDateTime getActualizadoEn() {
        return actualizadoEn;
    }

    public void setActualizadoEn(LocalDateTime actualizadoEn) {
        this.actualizadoEn = actualizadoEn;
    }
}
