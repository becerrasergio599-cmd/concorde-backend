/* =============================================
   CONCORDE TRANSPORTE TERRESTRE
   Widget de chatbot de atención al cliente
   Se inyecta automáticamente en todas las páginas
   que cargan componentes.js (ver inyectarChatbot()).
   ============================================= */
'use strict';

const ChatbotWidget = {
  API: (typeof AUTH_API !== 'undefined') ? AUTH_API : 'http://localhost:8082/api',
  idConsulta: null,
  estado: null,
  abierto: false,
  intervaloPoll: null,
  totalMensajesMostrados: 0,

  CHIPS: [
    { label: 'Horarios', texto: '¿Cuál es el horario de los buses?' },
    { label: 'Equipaje', texto: '¿Cuánto equipaje puedo llevar?' },
    { label: 'Cambiar/cancelar', texto: 'Necesito cambiar o cancelar mi reserva' },
    { label: 'Cómo reservar', texto: '¿Cómo reservo un tiquete?' },
    { label: 'Hablar con un asesor', texto: 'Quiero hablar con un asesor' }
  ],

  init() {
    if (document.getElementById('chatbotBurbuja')) return; // ya inyectado
    this.idConsulta = sessionStorage.getItem('concorde_chat_id') || null;
    this.render();
    this.actualizarBadge();

    // Si ya había una conversación abierta en esta sesión, revisa en
    // segundo plano si tiene respuestas nuevas (para el punto rojo).
    if (this.idConsulta) {
      this.refrescarEstadoSilencioso();
      setInterval(() => { if (!this.abierto) this.refrescarEstadoSilencioso(); }, 20000);
    }
  },

  render() {
    const burbuja = document.createElement('button');
    burbuja.id = 'chatbotBurbuja';
    burbuja.className = 'chatbot-burbuja';
    burbuja.setAttribute('aria-label', 'Abrir chat de atención al cliente');
    burbuja.innerHTML = '<i class="bi bi-headset" aria-hidden="true"></i><span id="chatbotBadge" class="chatbot-badge d-none">1</span>';
    burbuja.addEventListener('click', () => this.toggle());
    document.body.appendChild(burbuja);

    const panel = document.createElement('div');
    panel.id = 'chatbotPanel';
    panel.className = 'chatbot-panel';
    panel.setAttribute('role', 'dialog');
    panel.setAttribute('aria-label', 'Chat de atención al cliente Concorde');
    panel.innerHTML = `
      <div class="chatbot-header">
        <div>
          <strong>Atención al cliente</strong>
          <small id="chatbotSubtitulo">Asistente virtual Concorde</small>
        </div>
        <button type="button" id="chatbotCerrar" aria-label="Cerrar chat"><i class="bi bi-x-lg" aria-hidden="true"></i></button>
      </div>
      <div id="chatbotAviso" class="chatbot-aviso d-none"></div>
      <div id="chatbotMensajes" class="chatbot-mensajes" role="log" aria-live="polite" aria-label="Mensajes del chat"></div>
      <div id="chatbotChips" class="chatbot-chips"></div>
      <form id="chatbotForm" class="chatbot-form">
        <input type="text" id="chatbotInput" placeholder="Escribe tu mensaje..." autocomplete="off" aria-label="Escribe tu mensaje">
        <button type="submit" aria-label="Enviar mensaje"><i class="bi bi-send-fill" aria-hidden="true"></i></button>
      </form>
    `;
    document.body.appendChild(panel);

    document.getElementById('chatbotCerrar').addEventListener('click', () => this.cerrar());
    document.getElementById('chatbotForm').addEventListener('submit', (e) => {
      e.preventDefault();
      const input = document.getElementById('chatbotInput');
      const texto = input.value.trim();
      if (!texto) return;
      input.value = '';
      this.enviarMensaje(texto);
    });

    this.renderChips();
  },

  renderChips() {
    const cont = document.getElementById('chatbotChips');
    if (!cont) return;
    cont.innerHTML = this.CHIPS.map(c =>
      `<button type="button" class="chatbot-chip" data-texto="${c.texto.replace(/"/g, '&quot;')}">${c.label}</button>`
    ).join('');
    cont.querySelectorAll('.chatbot-chip').forEach(btn => {
      btn.addEventListener('click', () => this.enviarMensaje(btn.dataset.texto));
    });
  },

  // Se llama cuando el widget está cerrado, solo para saber si hay
  // mensajes nuevos del agente y mostrar el punto de notificación.
  async refrescarEstadoSilencioso() {
    if (!this.idConsulta) return;
    try {
      const res = await fetch(`${this.API}/chatbot/consultas/${this.idConsulta}/mensajes`);
      if (!res.ok) return;
      const datos = await res.json();
      this.estado = datos.estado;
      const vistos = Number(sessionStorage.getItem('concorde_chat_vistos') || 0);
      if (datos.mensajes.length > vistos) this.actualizarBadge(true);
    } catch (e) { /* backend no disponible, se ignora en silencio */ }
  },

  actualizarBadge(mostrar) {
    const badge = document.getElementById('chatbotBadge');
    if (badge) badge.classList.toggle('d-none', !mostrar);
  },

  async toggle() {
    if (this.abierto) { this.cerrar(); return; }
    this.abierto = true;
    document.getElementById('chatbotPanel').classList.add('abierto');
    this.actualizarBadge(false);

    if (!this.idConsulta) {
      await this.iniciarConversacion();
    } else {
      await this.cargarHistorial();
    }
    document.getElementById('chatbotInput').focus();
    this.iniciarPolling();
  },

  cerrar() {
    this.abierto = false;
    document.getElementById('chatbotPanel').classList.remove('abierto');
    if (this.intervaloPoll) { clearInterval(this.intervaloPoll); this.intervaloPoll = null; }
  },

  async iniciarConversacion() {
    this.agregarMensajeDom('bot', 'Conectando con el asistente...', true);
    try {
      const usuario = (typeof Auth !== 'undefined') ? Auth.getUsuario() : null;
      const body = usuario ? { usuarioId: usuario.id, nombre: usuario.nombre, correo: usuario.email } : {};
      const res = await fetch(`${this.API}/chatbot/iniciar`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
      });
      const datos = await res.json();
      this.idConsulta = datos.idConsulta;
      this.estado = datos.estado;
      sessionStorage.setItem('concorde_chat_id', this.idConsulta);
      this.limpiarMensajes();
      datos.mensajes.forEach(m => this.agregarMensajeDom(m.remitente.toLowerCase(), m.contenido));
      this.totalMensajesMostrados = datos.mensajes.length;
      sessionStorage.setItem('concorde_chat_vistos', this.totalMensajesMostrados);
    } catch (e) {
      this.limpiarMensajes();
      this.agregarMensajeDom('bot', 'No pude conectarme con el servidor. Verifica tu conexión o llama al 01-800-266-267.');
    }
  },

  async cargarHistorial() {
    try {
      const res = await fetch(`${this.API}/chatbot/consultas/${this.idConsulta}/mensajes`);
      if (!res.ok) { this.idConsulta = null; sessionStorage.removeItem('concorde_chat_id'); return this.iniciarConversacion(); }
      const datos = await res.json();
      this.estado = datos.estado;
      this.limpiarMensajes();
      datos.mensajes.forEach(m => this.agregarMensajeDom(m.remitente.toLowerCase(), m.contenido));
      this.totalMensajesMostrados = datos.mensajes.length;
      sessionStorage.setItem('concorde_chat_vistos', this.totalMensajesMostrados);
      this.actualizarAviso();
    } catch (e) {
      this.agregarMensajeDom('bot', 'No pude cargar el historial del chat.');
    }
  },

  actualizarAviso() {
    const aviso = document.getElementById('chatbotAviso');
    const chips = document.getElementById('chatbotChips');
    const subtitulo = document.getElementById('chatbotSubtitulo');
    if (!aviso) return;
    if (this.estado === 'ESCALADA') {
      aviso.textContent = 'Tu consulta fue trasladada a un asesor. En breve te responderá por este mismo chat.';
      aviso.classList.remove('d-none');
      chips.classList.add('d-none');
      subtitulo.textContent = 'Esperando a un asesor...';
    } else if (this.estado === 'EN_ATENCION') {
      aviso.textContent = 'Un asesor está atendiendo tu consulta.';
      aviso.classList.remove('d-none');
      chips.classList.add('d-none');
      subtitulo.textContent = 'En atención con un asesor';
    } else if (this.estado === 'CERRADA') {
      aviso.textContent = 'Esta conversación fue cerrada. Puedes escribir de nuevo si necesitas algo más.';
      aviso.classList.remove('d-none');
      chips.classList.remove('d-none');
      subtitulo.textContent = 'Asistente virtual Concorde';
    } else {
      aviso.classList.add('d-none');
      chips.classList.remove('d-none');
      subtitulo.textContent = 'Asistente virtual Concorde';
    }
  },

  async enviarMensaje(texto) {
    this.agregarMensajeDom('cliente', texto);
    document.getElementById('chatbotChips').classList.add('d-none');
    try {
      const res = await fetch(`${this.API}/chatbot/mensaje`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ idConsulta: this.idConsulta, texto })
      });
      const datos = await res.json();
      this.estado = datos.estado;
      if (datos.respuesta) this.agregarMensajeDom('bot', datos.respuesta);
      this.actualizarAviso();
      this.totalMensajesMostrados += datos.respuesta ? 2 : 1;
      sessionStorage.setItem('concorde_chat_vistos', this.totalMensajesMostrados);
    } catch (e) {
      this.agregarMensajeDom('bot', 'No pude enviar tu mensaje. Intenta de nuevo en unos segundos.');
    }
  },

  iniciarPolling() {
    if (this.intervaloPoll) clearInterval(this.intervaloPoll);
    this.intervaloPoll = setInterval(async () => {
      if (!this.idConsulta || (this.estado !== 'ESCALADA' && this.estado !== 'EN_ATENCION')) return;
      try {
        const res = await fetch(`${this.API}/chatbot/consultas/${this.idConsulta}/mensajes`);
        if (!res.ok) return;
        const datos = await res.json();
        if (datos.mensajes.length > this.totalMensajesMostrados) {
          datos.mensajes.slice(this.totalMensajesMostrados).forEach(m => this.agregarMensajeDom(m.remitente.toLowerCase(), m.contenido));
          this.totalMensajesMostrados = datos.mensajes.length;
          sessionStorage.setItem('concorde_chat_vistos', this.totalMensajesMostrados);
        }
        if (datos.estado !== this.estado) { this.estado = datos.estado; this.actualizarAviso(); }
      } catch (e) { /* se reintenta en el próximo ciclo */ }
    }, 6000);
  },

  limpiarMensajes() {
    const cont = document.getElementById('chatbotMensajes');
    if (cont) cont.innerHTML = '';
  },

  agregarMensajeDom(remitente, texto, esTemporal) {
    const cont = document.getElementById('chatbotMensajes');
    if (!cont) return;
    if (esTemporal) cont.innerHTML = '';
    const etiquetas = { bot: 'Asistente', agente: 'Asesor', cliente: '' };
    const div = document.createElement('div');
    div.className = `chatbot-burbuja-msg ${remitente}`;
    const etiqueta = etiquetas[remitente] ? `<span class="chatbot-remitente">${etiquetas[remitente]}</span>` : '';
    div.innerHTML = etiqueta + this.escapar(texto);
    cont.appendChild(div);
    cont.scrollTop = cont.scrollHeight;
  },

  escapar(texto) {
    const div = document.createElement('div');
    div.textContent = texto;
    return div.innerHTML;
  }
};

ChatbotWidget.init();
