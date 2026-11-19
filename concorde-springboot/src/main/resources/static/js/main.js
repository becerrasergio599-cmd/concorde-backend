/* =============================================
   CONCORDE AIRLINES - JavaScript Principal
   Cumple: Interactividad y experiencia de usuario
   ============================================= */

'use strict';

// ============================================
// MÓDULO DE BÚSQUEDA DE VUELOS
// ============================================
const BusquedaVuelos = {
  init() {
    const form = document.getElementById('formBusqueda');
    if (!form) return;

    // Intercambiar origen/destino
    const btnSwap = document.getElementById('btnSwap');
    if (btnSwap) {
      btnSwap.addEventListener('click', () => {
        const origen = document.getElementById('origen');
        const destino = document.getElementById('destino');
        if (origen && destino) {
          const temp = origen.value;
          origen.value = destino.value;
          destino.value = temp;
          // Animación visual
          btnSwap.classList.add('girar');
          setTimeout(() => btnSwap.classList.remove('girar'), 400);
        }
      });
    }

    // Fecha mínima: hoy
    const inputFecha = document.getElementById('fechaIda');
    const inputFechaVuelta = document.getElementById('fechaVuelta');
    if (inputFecha) {
      const hoy = new Date().toISOString().split('T')[0];
      inputFecha.min = hoy;
      inputFecha.value = hoy;
      if (inputFechaVuelta) inputFechaVuelta.min = hoy;

      inputFecha.addEventListener('change', () => {
        if (inputFechaVuelta) {
          inputFechaVuelta.min = inputFecha.value;
          if (inputFechaVuelta.value && inputFechaVuelta.value < inputFecha.value) {
            inputFechaVuelta.value = inputFecha.value;
          }
        }
      });
    }
    if (inputFechaVuelta) {
      inputFechaVuelta.addEventListener('change', () => inputFechaVuelta.classList.remove('is-invalid'));
    }

    // Tab solo ida / ida y vuelta
    const tabIdaVuelta = document.getElementById('tab-ida-vuelta');
    const tabSoloIda = document.getElementById('tab-solo-ida');
    const grupoFechaVuelta = document.getElementById('grupoFechaVuelta');
    if (tabIdaVuelta && grupoFechaVuelta) {
      tabIdaVuelta.addEventListener('shown.bs.tab', () => {
        grupoFechaVuelta.classList.remove('d-none');
      });
    }
    if (tabSoloIda && grupoFechaVuelta) {
      tabSoloIda.addEventListener('shown.bs.tab', () => {
        grupoFechaVuelta.classList.add('d-none');
      });
    }

    // Validación y submit: envía la búsqueda a la página de resultados por URL
    form.addEventListener('submit', (e) => {
      e.preventDefault();
      if (!this.validarFormBusqueda(form)) return;

      if (form.origen.value === form.destino.value) {
        UI.mostrarToast('El origen y el destino deben ser distintos', 'warning');
        return;
      }

      const soloIda = grupoFechaVuelta && grupoFechaVuelta.classList.contains('d-none');

      // Ida y vuelta: la fecha de vuelta es obligatoria
      if (!soloIda) {
        if (!form.fechaVuelta.value) {
          form.fechaVuelta.classList.add('is-invalid');
          UI.mostrarToast('Selecciona la fecha de vuelta o elige "Solo ida"', 'warning');
          form.fechaVuelta.focus();
          return;
        }
        if (form.fechaVuelta.value < form.fechaIda.value) {
          form.fechaVuelta.classList.add('is-invalid');
          UI.mostrarToast('La fecha de vuelta no puede ser anterior a la de ida', 'warning');
          return;
        }
      }
      form.fechaVuelta.classList.remove('is-invalid');

      // Nueva búsqueda = compra nueva: se limpia lo guardado de una compra anterior
      [
        'concorde_viaje', 'concorde_viaje_vuelta', 'concorde_ida_rt',
        'concorde_asientos', 'concorde_asiento', 'concorde_asientos_vuelta',
        'concorde_total', 'concorde_confirmacion', 'concorde_busqueda_rt'
      ].forEach(k => sessionStorage.removeItem(k));

      // Si es ida y vuelta, se recuerda la búsqueda completa para el paso de la vuelta
      if (!soloIda) {
        sessionStorage.setItem('concorde_busqueda_rt', JSON.stringify({
          origen: form.origen.value,
          destino: form.destino.value,
          ida: form.fechaIda.value,
          vuelta: form.fechaVuelta.value,
          pasajeros: form.pasajeros.value
        }));
      }

      const params = new URLSearchParams({
        origen: form.origen.value,
        destino: form.destino.value,
        ida: form.fechaIda.value,
        vuelta: soloIda ? '' : form.fechaVuelta.value,
        pasajeros: form.pasajeros.value
      });

      UI.mostrarLoader('Buscando trayectos disponibles...');
      setTimeout(() => {
        window.location.href = 'p02-resultados.html?' + params.toString();
      }, 1500);
    });
  },

  validarFormBusqueda(form) {
    let valido = true;
    form.querySelectorAll('[required]').forEach(campo => {
      if (!campo.value.trim()) {
        campo.classList.add('is-invalid');
        valido = false;
      } else {
        campo.classList.remove('is-invalid');
      }
    });
    return valido;
  }
};

// ============================================
// MÓDULO DE ASIENTOS
// ============================================
const MapaAsientos = {
  seleccionados: [],
  maxSeleccion: 1,

  init() {
    const asientos = document.querySelectorAll('.asiento:not(.asiento-ocupado)');
    if (!asientos.length) return;

    // Leer cuántos pasajeros hay que seleccionar
    const dataPasajeros = document.querySelector('[data-pasajeros]');
    if (dataPasajeros) {
      this.maxSeleccion = parseInt(dataPasajeros.dataset.pasajeros) || 1;
    }

    asientos.forEach(asiento => {
      asiento.setAttribute('role', 'button');
      asiento.setAttribute('tabindex', '0');
      asiento.setAttribute('aria-label', `Asiento ${asiento.dataset.numero}, ${asiento.dataset.estado || 'disponible'}`);

      asiento.addEventListener('click', () => this.toggleAsiento(asiento));
      asiento.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          this.toggleAsiento(asiento);
        }
      });
    });

    this.actualizarResumen();
  },

  toggleAsiento(asiento) {
    const id = asiento.dataset.numero;

    if (asiento.classList.contains('asiento-seleccionado')) {
      // Deseleccionar
      asiento.classList.remove('asiento-seleccionado');
      asiento.classList.add('asiento-libre');
      asiento.setAttribute('aria-pressed', 'false');
      this.seleccionados = this.seleccionados.filter(a => a !== id);
    } else {
      // Seleccionar
      if (this.seleccionados.length >= this.maxSeleccion) {
        UI.mostrarToast(`Solo puedes seleccionar ${this.maxSeleccion} asiento(s)`, 'warning');
        return;
      }
      asiento.classList.remove('asiento-libre');
      asiento.classList.add('asiento-seleccionado');
      asiento.setAttribute('aria-pressed', 'true');
      this.seleccionados.push(id);
    }

    this.actualizarResumen();
  },

  actualizarResumen() {
    const contenedorResumen = document.getElementById('resumenAsientos');
    const btnContinuar = document.getElementById('btnContinuarAsientos');

    if (contenedorResumen) {
      if (this.seleccionados.length === 0) {
        contenedorResumen.textContent = 'Ningún asiento seleccionado';
      } else {
        contenedorResumen.textContent = `Asientos: ${this.seleccionados.join(', ')}`;
      }
    }

    if (btnContinuar) {
      btnContinuar.disabled = this.seleccionados.length < this.maxSeleccion;
    }
  }
};

// ============================================
// MÓDULO DE VALIDACIÓN DE FORMULARIOS
// ============================================
const Validacion = {
  init() {
    // Validación en tiempo real para todos los forms con clase .form-concorde
    document.querySelectorAll('.form-concorde').forEach(form => {
      form.querySelectorAll('input, select, textarea').forEach(campo => {
        campo.addEventListener('blur', () => this.validarCampo(campo));
        campo.addEventListener('input', () => {
          if (campo.classList.contains('is-invalid')) {
            this.validarCampo(campo);
          }
        });
      });

      // El buscador (#formBusqueda) tiene su propio submit en BusquedaVuelos
      if (form.id !== 'formBusqueda') {
        form.addEventListener('submit', (e) => {
          e.preventDefault();
          if (this.validarForm(form)) {
            this.procesarForm(form);
          }
        });
      }
    });

    // Validación especial de email
    document.querySelectorAll('input[type="email"]').forEach(campo => {
      campo.addEventListener('blur', () => {
        if (campo.value && !this.esEmailValido(campo.value)) {
          campo.classList.add('is-invalid');
          this.mostrarError(campo, 'Ingresa un correo electrónico válido');
        }
      });
    });

    // Confirmación de contraseña
    const passConfirm = document.getElementById('confirmarPassword');
    if (passConfirm) {
      passConfirm.addEventListener('blur', () => {
        const pass = document.getElementById('password');
        if (pass && passConfirm.value !== pass.value) {
          passConfirm.classList.add('is-invalid');
          this.mostrarError(passConfirm, 'Las contraseñas no coinciden');
        }
      });
    }
  },

  validarCampo(campo) {
    if (campo.hasAttribute('required') && !campo.value.trim()) {
      campo.classList.add('is-invalid');
      campo.classList.remove('is-valid');
      return false;
    }
    campo.classList.remove('is-invalid');
    if (campo.value.trim()) campo.classList.add('is-valid');
    return true;
  },

  validarForm(form) {
    let valido = true;
    form.querySelectorAll('[required]').forEach(campo => {
      if (!this.validarCampo(campo)) valido = false;
    });
    return valido;
  },

  esEmailValido(email) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  },

  mostrarError(campo, mensaje) {
    let feedback = campo.nextElementSibling;
    if (!feedback || !feedback.classList.contains('invalid-feedback')) {
      feedback = document.createElement('div');
      feedback.className = 'invalid-feedback';
      campo.parentNode.insertBefore(feedback, campo.nextSibling);
    }
    feedback.textContent = mensaje;
  },

  procesarForm(form) {
    const action = form.dataset.action || 'default';
    UI.mostrarLoader();
    const destinos = {
      'login': 'p11-reservas.html',
      'registro': 'p04b-confirmacion-registro.html',
      'pasajero': 'p07-asientos.html',
      'pago': 'p10-confirmacion.html',
      'recuperar': 'p17-confirmacion-mensaje.html',
      'atencion': 'p17-confirmacion-mensaje.html',
      'perfil': 'p16-perfil.html',
    };
    setTimeout(() => {
      window.location.href = destinos[action] || '#';
    }, 1200);
  }
};

// ============================================
// MÓDULO DE UI GENERAL
// ============================================
const UI = {
  init() {
    this.initTooltips();
    this.initScrollAnimations();
    this.initNavbarScroll();
    this.initContadorPasajeros();
    this.initFiltrosResultados();
  },

  // ---------- LOADER: bus en blanco y negro sobre la carretera ----------
  _loaderTimer: null,

  _inyectarEstilosLoader() {
    if (document.getElementById('estilosLoaderBus')) return;
    const style = document.createElement('style');
    style.id = 'estilosLoaderBus';
    style.textContent = `
      #loaderGlobal {
        position: fixed; inset: 0; z-index: 99999;
        display: flex; align-items: center; justify-content: center;
        background: rgba(0, 32, 91, .94);
      }
      .lb { width: min(380px, 88vw); text-align: center; color: #fff; }
      .lb__escena { position: relative; }

      .lb__bus {
        width: 340px; max-width: 100%; display: block; margin: 0 auto; position: relative; z-index: 1;
        animation: lb-rebote .45s ease-in-out infinite alternate;
      }
      @keyframes lb-rebote { to { transform: translateY(-2px); } }

      .lb__rueda { transform-box: fill-box; transform-origin: center; animation: lb-gira .5s linear infinite; }
      @keyframes lb-gira { to { transform: rotate(360deg); } }

      .lb__carretera {
        height: 14px; margin-top: -1px; border-radius: 3px;
        background-color: #000;
        background-image: repeating-linear-gradient(90deg, #fff 0 22px, transparent 22px 48px);
        background-size: 48px 3px; background-repeat: repeat-x; background-position: 0 center;
        box-shadow: 0 -1px 0 #fff;
        animation: lb-ruta .45s linear infinite;
      }
      @keyframes lb-ruta { to { background-position-x: 48px; } }

      .lb__lineas { position: absolute; right: -26px; top: 36px; width: 44px; height: 50px; z-index: 0; }
      .lb__lineas span {
        position: absolute; left: 0; height: 3px; border-radius: 2px; background: #fff; opacity: 0;
        animation: lb-vel .9s linear infinite;
      }
      .lb__lineas span:nth-child(1) { top: 4px;  width: 30px; animation-delay: 0s; }
      .lb__lineas span:nth-child(2) { top: 22px; width: 40px; animation-delay: .3s; }
      .lb__lineas span:nth-child(3) { top: 40px; width: 24px; animation-delay: .6s; }
      @keyframes lb-vel {
        0%   { opacity: 0;  transform: translateX(-20px); }
        30%  { opacity: .7; }
        100% { opacity: 0;  transform: translateX(30px); }
      }

      .lb__texto { margin-top: 20px; font-weight: 700; transition: opacity .3s; }

      @media (prefers-reduced-motion: reduce) {
        #loaderGlobal * { animation: none !important; }
      }`;
    document.head.appendChild(style);
  },

  mostrarLoader(mensaje = 'Cargando...') {
    // Evita duplicar el loader si ya hay uno visible
    this.ocultarLoader();
    this._inyectarEstilosLoader();

    const loader = document.createElement('div');
    loader.id = 'loaderGlobal';
    loader.setAttribute('role', 'status');
    loader.setAttribute('aria-live', 'polite');
    loader.innerHTML = `
      <div class="lb">
        <div class="lb__escena">
          <div class="lb__lineas"><span></span><span></span><span></span></div>
          <svg class="lb__bus" viewBox="36 104 476 156" aria-hidden="true">
            <path d="M46.0 162.8 L46.0 178.0 L47.2 181.5 L48.2 182.5 L49.8 182.8 L50.8 181.8 L51.0 160.2 L52.8 158.8 L55.0 158.8 L60.8 156.8 L64.0 156.8 L66.2 156.0 L67.0 157.0 L66.0 158.8 L66.0 161.2 L65.0 162.8 L65.2 166.8 L64.2 168.2 L64.5 173.8 L64.5 170.0 L65.5 168.5 L65.5 165.0 L66.8 163.0 L67.8 156.8 L69.0 155.8 L71.0 155.8 L72.8 154.8 L74.2 155.0 L75.0 156.0 L75.0 158.0 L74.0 159.8 L74.0 165.0 L73.0 166.8 L73.0 175.0 L72.0 176.8 L72.0 189.0 L71.0 190.8 L71.0 203.0 L68.8 206.5 L64.5 209.8 L63.5 209.8 L62.5 208.5 L62.2 184.0 L62.2 209.2 L61.0 211.8 L61.0 221.0 L62.0 222.8 L62.0 229.8 L60.0 232.0 L60.0 236.0 L61.2 238.5 L64.8 240.8 L122.0 240.8 L124.8 238.8 L124.8 231.8 L125.8 230.0 L125.8 227.8 L129.0 221.2 L135.2 215.0 L141.8 211.8 L144.0 211.8 L145.8 210.8 L151.0 210.8 L152.8 211.8 L156.0 211.8 L162.5 215.0 L167.8 220.2 L172.0 227.8 L172.0 230.0 L173.0 231.8 L173.2 244.5 L173.8 241.8 L175.8 240.2 L177.8 240.8 L355.0 240.8 L357.8 238.8 L357.8 234.8 L358.8 233.0 L358.8 229.8 L359.8 226.8 L364.0 219.2 L370.2 214.0 L378.8 210.8 L385.0 210.8 L386.8 211.8 L389.0 211.8 L394.5 214.0 L399.8 218.2 L402.8 222.2 L406.0 229.8 L406.2 244.5 L406.5 242.2 L408.0 240.8 L435.0 240.8 L436.8 239.8 L464.0 239.8 L465.8 238.8 L474.0 238.8 L475.8 237.8 L483.0 237.8 L484.8 236.8 L491.0 236.8 L492.8 235.8 L496.0 235.8 L498.0 234.8 L500.8 230.0 L500.8 225.0 L497.8 222.8 L497.8 218.8 L498.8 217.0 L498.8 198.8 L499.8 197.0 L499.8 164.8 L498.8 163.0 L498.8 152.8 L497.8 151.0 L497.8 145.8 L496.8 144.0 L496.8 139.8 L495.8 138.0 L495.8 134.8 L494.8 133.0 L494.8 130.8 L493.8 129.0 L493.8 125.8 L490.5 118.2 L486.0 115.0 L404.8 115.0 L398.0 111.0 L389.8 111.0 L388.0 110.0 L375.8 110.0 L374.0 111.0 L365.8 111.0 L359.0 115.0 L289.8 115.0 L286.5 112.2 L284.0 111.0 L274.8 111.0 L273.0 110.0 L261.8 110.0 L260.0 111.0 L250.8 111.0 L248.2 112.2 L245.0 115.0 L174.8 115.0 L171.5 112.2 L169.0 111.0 L160.8 111.0 L159.0 110.0 L146.8 110.0 L145.0 111.0 L135.8 111.0 L130.0 115.0 L100.8 115.0 L99.0 116.0 L95.8 116.0 L92.0 118.0 L90.2 118.2 L84.2 122.2 L78.0 128.2 L74.2 133.2 L72.2 137.2 L72.8 139.5 L75.5 140.0 L77.5 142.8 L76.8 147.5 L75.0 149.0 L71.2 149.2 L70.8 148.8 L70.8 146.5 L71.5 145.5 L71.5 143.8 L70.2 145.2 L70.0 147.2 L68.8 149.5 L67.5 150.8 L64.8 151.0 L55.8 154.0 L49.2 157.2 L47.2 159.2Z" fill="#fff" stroke="#fff" stroke-width="3" stroke-linejoin="round"/>
            <path d="M46.0 162.8 L46.0 178.0 L47.2 181.5 L48.2 182.5 L49.8 182.8 L50.8 181.8 L51.0 160.2 L52.8 158.8 L55.0 158.8 L60.8 156.8 L64.0 156.8 L66.2 156.0 L67.0 157.0 L66.0 158.8 L66.0 161.2 L65.0 162.8 L65.2 166.8 L64.2 168.2 L64.5 173.8 L64.5 170.0 L65.5 168.5 L65.5 165.0 L66.8 163.0 L67.8 156.8 L69.0 155.8 L71.0 155.8 L72.8 154.8 L74.2 155.0 L75.0 156.0 L75.0 158.0 L74.0 159.8 L74.0 165.0 L73.0 166.8 L73.0 175.0 L72.0 176.8 L72.0 189.0 L71.0 190.8 L71.0 203.0 L68.8 206.5 L64.5 209.8 L63.5 209.8 L62.5 208.5 L62.2 184.0 L62.2 209.2 L61.0 211.8 L61.0 221.0 L62.0 222.8 L62.0 229.8 L60.0 232.0 L60.0 236.0 L61.2 238.5 L64.8 240.8 L122.0 240.8 L124.8 238.8 L124.8 231.8 L125.8 230.0 L125.8 227.8 L129.0 221.2 L135.2 215.0 L141.8 211.8 L144.0 211.8 L145.8 210.8 L151.0 210.8 L152.8 211.8 L156.0 211.8 L162.5 215.0 L167.8 220.2 L172.0 227.8 L172.0 230.0 L173.0 231.8 L173.2 244.5 L173.8 241.8 L175.8 240.2 L177.8 240.8 L355.0 240.8 L357.8 238.8 L357.8 234.8 L358.8 233.0 L358.8 229.8 L359.8 226.8 L364.0 219.2 L370.2 214.0 L378.8 210.8 L385.0 210.8 L386.8 211.8 L389.0 211.8 L394.5 214.0 L399.8 218.2 L402.8 222.2 L406.0 229.8 L406.2 244.5 L406.5 242.2 L408.0 240.8 L435.0 240.8 L436.8 239.8 L464.0 239.8 L465.8 238.8 L474.0 238.8 L475.8 237.8 L483.0 237.8 L484.8 236.8 L491.0 236.8 L492.8 235.8 L496.0 235.8 L498.0 234.8 L500.8 230.0 L500.8 225.0 L497.8 222.8 L497.8 218.8 L498.8 217.0 L498.8 198.8 L499.8 197.0 L499.8 164.8 L498.8 163.0 L498.8 152.8 L497.8 151.0 L497.8 145.8 L496.8 144.0 L496.8 139.8 L495.8 138.0 L495.8 134.8 L494.8 133.0 L494.8 130.8 L493.8 129.0 L493.8 125.8 L490.5 118.2 L486.0 115.0 L404.8 115.0 L398.0 111.0 L389.8 111.0 L388.0 110.0 L375.8 110.0 L374.0 111.0 L365.8 111.0 L359.0 115.0 L289.8 115.0 L286.5 112.2 L284.0 111.0 L274.8 111.0 L273.0 110.0 L261.8 110.0 L260.0 111.0 L250.8 111.0 L248.2 112.2 L245.0 115.0 L174.8 115.0 L171.5 112.2 L169.0 111.0 L160.8 111.0 L159.0 110.0 L146.8 110.0 L145.0 111.0 L135.8 111.0 L130.0 115.0 L100.8 115.0 L99.0 116.0 L95.8 116.0 L92.0 118.0 L90.2 118.2 L84.2 122.2 L78.0 128.2 L74.2 133.2 L72.2 137.2 L72.8 139.5 L75.5 140.0 L77.5 142.8 L76.8 147.5 L75.0 149.0 L71.2 149.2 L70.8 148.8 L70.8 146.5 L71.5 145.5 L71.5 143.8 L70.2 145.2 L70.0 147.2 L68.8 149.5 L67.5 150.8 L64.8 151.0 L55.8 154.0 L49.2 157.2 L47.2 159.2Z M113.0 149.8 L114.0 151.0 L114.0 189.0 L112.0 193.0 L108.5 195.8 L102.0 198.0 L99.8 198.0 L95.0 200.0 L92.8 200.0 L91.0 201.0 L87.8 201.0 L82.0 203.0 L78.2 202.8 L75.8 200.0 L75.8 186.8 L76.8 185.0 L76.8 174.8 L77.8 173.0 L77.8 164.8 L78.8 163.0 L78.8 157.8 L81.0 151.2 L83.8 148.8 L111.0 148.8Z M411.0 129.5 L412.0 128.8 L470.0 128.8 L472.5 130.0 L473.8 131.2 L475.0 134.8 L475.0 138.0 L476.0 139.8 L476.0 144.0 L477.0 145.8 L477.0 152.0 L478.0 153.8 L478.0 163.0 L478.8 164.2 L477.8 167.5 L476.0 169.0 L411.5 168.8 L410.8 167.8Z M339.0 129.5 L340.0 128.8 L406.2 129.0 L407.0 130.0 L406.8 168.2 L405.8 169.0 L339.5 168.8 L338.8 167.8Z M267.0 129.5 L268.0 128.8 L333.5 129.0 L335.0 130.8 L334.8 168.2 L333.8 169.0 L267.5 168.8 L266.8 167.8Z M195.0 129.5 L196.0 128.8 L261.5 129.0 L263.0 130.8 L262.8 168.2 L261.8 169.0 L195.5 168.8 L194.8 167.8Z M123.0 131.2 L125.8 128.8 L189.5 129.0 L191.0 130.8 L190.8 168.2 L189.8 169.0 L124.2 168.8 L122.8 167.0Z" fill="#000" fill-rule="evenodd"/>
            <path d="M126.2 241 A23.5 23.5 0 1 1 170.4 241 Z" fill="#fff"/>
            <g class="lb__rueda">
              <circle cx="148.3" cy="233" r="19.5" fill="#000" stroke="#fff" stroke-width="1.6"/>
              <circle cx="148.3" cy="233" r="9" fill="none" stroke="#fff" stroke-width="3"/>
              <rect x="147.5" y="226" width="1.6" height="7" fill="#fff"/>
            </g>
            <path d="M359.8 241 A23.5 23.5 0 1 1 404.0 241 Z" fill="#fff"/>
            <g class="lb__rueda">
              <circle cx="381.9" cy="233" r="19.5" fill="#000" stroke="#fff" stroke-width="1.6"/>
              <circle cx="381.9" cy="233" r="9" fill="none" stroke="#fff" stroke-width="3"/>
              <rect x="381.1" y="226" width="1.6" height="7" fill="#fff"/>
            </g>
          </svg>
          <div class="lb__carretera"></div>
        </div>
        <p class="lb__texto" id="lbTexto">${mensaje}</p>
      </div>`;
    document.body.appendChild(loader);

    // Mensajes rotativos solo cuando se están buscando trayectos
    if (/trayectos/i.test(mensaje)) {
      const mensajes = [
        'Buscando trayectos disponibles...',
        'Comparando horarios y precios...',
        'Casi listo, ya arrancamos...'
      ];
      let i = 0;
      const texto = loader.querySelector('#lbTexto');
      this._loaderTimer = setInterval(() => {
        i = (i + 1) % mensajes.length;
        texto.style.opacity = 0;
        setTimeout(() => {
          texto.textContent = mensajes[i];
          texto.style.opacity = 1;
        }, 300);
      }, 1800);
    }
  },

  ocultarLoader() {
    clearInterval(this._loaderTimer);
    this._loaderTimer = null;
    const loader = document.getElementById('loaderGlobal');
    if (loader) loader.remove();
  },
  // ---------- FIN LOADER ----------

  mostrarToast(mensaje, tipo = 'info') {
    const colores = {
      info: 'var(--azul-claro)',
      success: 'var(--verde-exito)',
      warning: 'var(--dorado)',
      danger: 'var(--rojo-error)',
    };

    const toast = document.createElement('div');
    toast.className = 'position-fixed bottom-0 end-0 m-3 p-3 rounded-3 text-white shadow-lg';
    toast.style.cssText = `
      background: ${colores[tipo] || colores.info};
      z-index: 9999;
      max-width: 300px;
      animation: fadeInUp 0.3s ease;
    `;
    toast.setAttribute('role', 'alert');
    toast.setAttribute('aria-live', 'polite');
    toast.textContent = mensaje;
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), 3500);
  },

  initTooltips() {
    const tooltipEls = document.querySelectorAll('[data-bs-toggle="tooltip"]');
    tooltipEls.forEach(el => new bootstrap.Tooltip(el));
  },

  initScrollAnimations() {
    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add('visible');
        }
      });
    }, { threshold: 0.1 });

    document.querySelectorAll('.fade-in-up').forEach(el => observer.observe(el));
  },

  initNavbarScroll() {
    const navbar = document.querySelector('.navbar-concorde');
    if (!navbar) return;
    window.addEventListener('scroll', () => {
      if (window.scrollY > 50) {
        navbar.style.boxShadow = '0 4px 30px rgba(0,0,0,0.3)';
      } else {
        navbar.style.boxShadow = '';
      }
    });
  },

  initContadorPasajeros() {
    document.querySelectorAll('.contador-pasajeros').forEach(grupo => {
      const input = grupo.querySelector('input');
      const btnMas = grupo.querySelector('.btn-mas');
      const btnMenos = grupo.querySelector('.btn-menos');
      if (!input || !btnMas || !btnMenos) return;

      const min = parseInt(input.min) || 1;
      const max = parseInt(input.max) || 9;

      btnMas.addEventListener('click', () => {
        if (parseInt(input.value) < max) input.value = parseInt(input.value) + 1;
      });

      btnMenos.addEventListener('click', () => {
        if (parseInt(input.value) > min) input.value = parseInt(input.value) - 1;
      });
    });
  },

  initFiltrosResultados() {
    // Los filtros de p02-resultados.html se aplican en el script de esa página.
    // Aquí solo se deja el aviso visual en las demás páginas que usen data-filtro.
    if (document.getElementById('listaTrayectos')) return;
    const filtros = document.querySelectorAll('[data-filtro]');
    filtros.forEach(filtro => {
      filtro.addEventListener('change', () => {
        UI.mostrarToast('Aplicando filtros...', 'info');
      });
    });
  }
};

// ============================================
// MÓDULO DE CANCELACIÓN
// ============================================
const Cancelacion = {
  init() {
    const form = document.getElementById('formCancelacion');
    if (!form) return;

    const btnConfirmar = document.getElementById('btnConfirmarCancelacion');
    const checkAceptar = document.getElementById('checkAceptarCancelacion');

    if (checkAceptar && btnConfirmar) {
      checkAceptar.addEventListener('change', () => {
        btnConfirmar.disabled = !checkAceptar.checked;
      });
    }

    if (btnConfirmar) {
      btnConfirmar.addEventListener('click', () => {
        UI.mostrarLoader('Procesando cancelación...');
        setTimeout(() => {
          window.location.href = 'p15b-cancelacion-ok.html';
        }, 1800);
      });
    }
  }
};

// ============================================
// MÓDULO DE PAGO
// ============================================
const Pago = {
  init() {
    const metodoPago = document.querySelectorAll('[name="metodoPago"]');
    metodoPago.forEach(radio => {
      radio.addEventListener('change', () => {
        document.querySelectorAll('.seccion-pago').forEach(s => s.classList.add('d-none'));
        const seccion = document.getElementById(`pago-${radio.value}`);
        if (seccion) seccion.classList.remove('d-none');
      });
    });

    // Formatear número de tarjeta
    const inputTarjeta = document.getElementById('numeroTarjeta');
    if (inputTarjeta) {
      inputTarjeta.addEventListener('input', (e) => {
        let val = e.target.value.replace(/\D/g, '').substring(0, 16);
        e.target.value = val.match(/.{1,4}/g)?.join(' ') || val;
      });
    }

    // Formatear fecha expiración
    const inputExp = document.getElementById('expiracion');
    if (inputExp) {
      inputExp.addEventListener('input', (e) => {
        let val = e.target.value.replace(/\D/g, '').substring(0, 4);
        if (val.length > 2) val = val.substring(0, 2) + '/' + val.substring(2);
        e.target.value = val;
      });
    }
  }
};

// ============================================
// INICIALIZACIÓN GLOBAL
// ============================================
document.addEventListener('DOMContentLoaded', () => {
  UI.init();
  BusquedaVuelos.init();
  MapaAsientos.init();
  Validacion.init();
  Cancelacion.init();
  Pago.init();

  // Marcar enlace activo en navbar según página actual
  const paginaActual = window.location.pathname.split('/').pop() || 'index.html';
  document.querySelectorAll('.nav-link').forEach(link => {
    if (link.getAttribute('href') === paginaActual) {
      link.classList.add('active');
      link.setAttribute('aria-current', 'page');
    }
  });
});
