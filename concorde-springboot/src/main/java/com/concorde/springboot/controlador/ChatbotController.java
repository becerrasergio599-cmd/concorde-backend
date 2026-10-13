package com.concorde.springboot.controlador;

import com.concorde.springboot.modelo.ConsultaChat;
import com.concorde.springboot.modelo.MensajeChat;
import com.concorde.springboot.modelo.Usuario;
import com.concorde.springboot.repositorio.ConsultaChatRepository;
import com.concorde.springboot.repositorio.MensajeChatRepository;
import com.concorde.springboot.repositorio.UsuarioRepository;
import com.concorde.springboot.servicio.MotorFaqChatbot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Chatbot de atención al cliente:
//  - POST /api/chatbot/iniciar   -> abre una conversación nueva y saluda
//  - POST /api/chatbot/mensaje   -> procesa lo que escribe el cliente
//  - GET  /api/chatbot/consultas/{id}/mensajes -> historial (cliente y agente)
//  - GET  /api/chatbot/consultas -> lista de tickets para el panel de agente
//  - POST /api/chatbot/consultas/{id}/responder -> un agente responde
//  - POST /api/chatbot/consultas/{id}/cerrar    -> cierra el ticket
@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    @Autowired
    private ConsultaChatRepository consultaRepository;

    @Autowired
    private MensajeChatRepository mensajeRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MotorFaqChatbot motor;

    private MensajeChat guardarMensaje(ConsultaChat consulta, String remitente, String contenido) {
        MensajeChat m = new MensajeChat();
        m.setConsulta(consulta);
        m.setRemitente(remitente);
        m.setContenido(contenido);
        m.setEnviadoEn(LocalDateTime.now());
        return mensajeRepository.save(m);
    }

    // ── Iniciar conversación ──────────────────────────────────────
    @PostMapping("/iniciar")
    public ResponseEntity<?> iniciar(@RequestBody(required = false) Map<String, Object> body) {
        ConsultaChat consulta = new ConsultaChat();

        if (body != null && body.get("usuarioId") != null) {
            Integer usuarioId = Integer.valueOf(body.get("usuarioId").toString());
            Optional<Usuario> usuario = usuarioRepository.findById(usuarioId);
            usuario.ifPresent(consulta::setUsuario);
            usuario.ifPresent(u -> consulta.setCorreoContacto(u.getCorreoElectronico()));
            usuario.ifPresent(u -> consulta.setNombreContacto(u.getNombreCompleto()));
        }
        if (body != null && body.get("nombre") != null) consulta.setNombreContacto(body.get("nombre").toString());
        if (body != null && body.get("correo") != null) consulta.setCorreoContacto(body.get("correo").toString());

        consulta.setAsunto("Chat de atención al cliente");
        consulta.setEstado("BOT");
        consulta.setCreadoEn(LocalDateTime.now());
        consulta.setActualizadoEn(LocalDateTime.now());
        consultaRepository.save(consulta);

        MensajeChat bienvenida = guardarMensaje(consulta, "BOT",
                "¡Hola" + (consulta.getNombreContacto() != null ? ", " + consulta.getNombreContacto().split(" ")[0] : "") +
                        "! 👋 Soy el asistente virtual de Concorde. Puedo resolver dudas sobre horarios, equipaje, cambios, cancelaciones, pagos y más. Si necesitas algo más puntual, te comunico con un asesor. ¿En qué te ayudo?");

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "idConsulta", consulta.getIdConsulta(),
                "estado", consulta.getEstado(),
                "mensajes", List.of(dtoMensaje(bienvenida))
        ));
    }

    // ── Procesar mensaje del cliente ──────────────────────────────
    @PostMapping("/mensaje")
    public ResponseEntity<?> mensaje(@RequestBody Map<String, Object> body) {
        if (body.get("idConsulta") == null || body.get("texto") == null) {
            return ResponseEntity.badRequest().body(Map.of("detail", "idConsulta y texto son obligatorios"));
        }
        Integer idConsulta = Integer.valueOf(body.get("idConsulta").toString());
        String texto = body.get("texto").toString().trim();
        if (texto.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("detail", "El mensaje no puede estar vacío"));
        }

        Optional<ConsultaChat> opt = consultaRepository.findById(idConsulta);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        ConsultaChat consulta = opt.get();

        guardarMensaje(consulta, "CLIENTE", texto);

        // Si ya hay un humano en la conversación, el bot no interviene:
        // el mensaje queda esperando respuesta del agente.
        if ("ESCALADA".equals(consulta.getEstado()) || "EN_ATENCION".equals(consulta.getEstado())) {
            consulta.setActualizadoEn(LocalDateTime.now());
            consultaRepository.save(consulta);
            return ResponseEntity.ok(Map.of(
                    "respuesta", (Object) null,
                    "escalado", true,
                    "estado", consulta.getEstado()
            ));
        }

        // Si estaba cerrada, reabrir la conversación con el bot.
        if ("CERRADA".equals(consulta.getEstado())) {
            consulta.setEstado("BOT");
        }

        MotorFaqChatbot.Resultado resultado = motor.responder(texto);
        guardarMensaje(consulta, "BOT", resultado.respuesta);

        if (resultado.escalar) {
            consulta.setEstado("ESCALADA");
        }
        consulta.setActualizadoEn(LocalDateTime.now());
        consultaRepository.save(consulta);

        return ResponseEntity.ok(Map.of(
                "respuesta", resultado.respuesta,
                "escalado", resultado.escalar,
                "estado", consulta.getEstado()
        ));
    }

    // ── Historial de una conversación (cliente y agente lo usan) ──
    @GetMapping("/consultas/{id}/mensajes")
    public ResponseEntity<?> mensajes(@PathVariable Integer id) {
        Optional<ConsultaChat> opt = consultaRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        List<MensajeChat> mensajes = mensajeRepository.findByConsulta_IdConsultaOrderByEnviadoEnAsc(id);
        List<Map<String, Object>> dto = new ArrayList<>();
        for (MensajeChat m : mensajes) dto.add(dtoMensaje(m));
        return ResponseEntity.ok(Map.of(
                "idConsulta", id,
                "estado", opt.get().getEstado(),
                "mensajes", dto
        ));
    }

    // ── Listado de tickets para el panel de agente ────────────────
    // GET /api/chatbot/consultas?estados=ESCALADA,EN_ATENCION (por defecto)
    // GET /api/chatbot/consultas?estados=TODAS -> incluye BOT y CERRADA
    @GetMapping("/consultas")
    public List<Map<String, Object>> consultas(@RequestParam(required = false) String estados) {
        List<ConsultaChat> lista;
        if (estados != null && estados.equalsIgnoreCase("TODAS")) {
            lista = consultaRepository.findAllByOrderByActualizadoEnDesc();
        } else {
            List<String> filtro = estados != null
                    ? Arrays.asList(estados.split(","))
                    : List.of("ESCALADA", "EN_ATENCION");
            lista = consultaRepository.findByEstadoInOrderByActualizadoEnDesc(filtro);
        }
        List<Map<String, Object>> dto = new ArrayList<>();
        for (ConsultaChat c : lista) dto.add(dtoConsulta(c));
        return dto;
    }

    // ── Un agente responde ─────────────────────────────────────────
    @PostMapping("/consultas/{id}/responder")
    public ResponseEntity<?> responder(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        Optional<ConsultaChat> opt = consultaRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        if (body.get("texto") == null || body.get("texto").toString().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("detail", "El texto de respuesta es obligatorio"));
        }
        ConsultaChat consulta = opt.get();

        if (body.get("agenteId") != null && consulta.getAgente() == null) {
            usuarioRepository.findById(Integer.valueOf(body.get("agenteId").toString()))
                    .ifPresent(consulta::setAgente);
        }
        consulta.setEstado("EN_ATENCION");
        consulta.setActualizadoEn(LocalDateTime.now());
        consultaRepository.save(consulta);

        MensajeChat m = guardarMensaje(consulta, "AGENTE", body.get("texto").toString().trim());
        return ResponseEntity.ok(dtoMensaje(m));
    }

    // ── Cerrar ticket ───────────────────────────────────────────────
    @PostMapping("/consultas/{id}/cerrar")
    public ResponseEntity<?> cerrar(@PathVariable Integer id) {
        Optional<ConsultaChat> opt = consultaRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        ConsultaChat consulta = opt.get();
        consulta.setEstado("CERRADA");
        consulta.setActualizadoEn(LocalDateTime.now());
        consultaRepository.save(consulta);
        guardarMensaje(consulta, "AGENTE", "Conversación cerrada. ¡Gracias por contactar a Concorde!");
        return ResponseEntity.ok(Map.of("detail", "Ticket cerrado"));
    }

    private Map<String, Object> dtoMensaje(MensajeChat m) {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("idMensaje", m.getIdMensaje());
        map.put("remitente", m.getRemitente());
        map.put("contenido", m.getContenido());
        map.put("enviadoEn", m.getEnviadoEn());
        return map;
    }

    private Map<String, Object> dtoConsulta(ConsultaChat c) {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("idConsulta", c.getIdConsulta());
        map.put("estado", c.getEstado());
        map.put("asunto", c.getAsunto());
        map.put("nombreContacto", c.getNombreContacto());
        map.put("correoContacto", c.getCorreoContacto());
        map.put("usuarioId", c.getUsuario() != null ? c.getUsuario().getIdUsuario() : null);
        map.put("agenteId", c.getAgente() != null ? c.getAgente().getIdUsuario() : null);
        map.put("agenteNombre", c.getAgente() != null ? c.getAgente().getNombreCompleto() : null);
        map.put("creadoEn", c.getCreadoEn());
        map.put("actualizadoEn", c.getActualizadoEn());
        return map;
    }
}
