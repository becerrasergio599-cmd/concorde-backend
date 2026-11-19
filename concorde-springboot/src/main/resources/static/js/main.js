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
      .lb { width: min(340px, 85vw); text-align: center; color: #fff; }
      .lb__escena { position: relative; }

      .lb__bus {
        width: 270px; display: block; margin: 0 auto; position: relative; z-index: 1;
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
      @keyframes lb-ruta { to { background-position-x: -48px; } }

      .lb__lineas { position: absolute; left: 0; top: 28px; width: 44px; height: 50px; z-index: 0; }
      .lb__lineas span {
        position: absolute; right: 0; height: 3px; border-radius: 2px; background: #fff; opacity: 0;
        animation: lb-vel .9s linear infinite;
      }
      .lb__lineas span:nth-child(1) { top: 4px;  width: 30px; animation-delay: 0s; }
      .lb__lineas span:nth-child(2) { top: 22px; width: 40px; animation-delay: .3s; }
      .lb__lineas span:nth-child(3) { top: 40px; width: 24px; animation-delay: .6s; }
      @keyframes lb-vel {
        0%   { opacity: 0;  transform: translateX(20px); }
        30%  { opacity: .7; }
        100% { opacity: 0;  transform: translateX(-30px); }
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
          <svg class="lb__bus" viewBox="0 0 300 96" aria-hidden="true">
            <path d="M8 26 Q8 14 22 14 H236 Q258 14 268 30 L276 44 Q290 47 292 58 V72 Q292 76 288 76 H14 Q8 76 8 70 Z"
                  fill="#000" stroke="#fff" stroke-width="2" stroke-linejoin="round"/>
            <rect x="70"  y="9" width="26" height="6" rx="3" fill="#000" stroke="#fff" stroke-width="1.5"/>
            <rect x="160" y="9" width="26" height="6" rx="3" fill="#000" stroke="#fff" stroke-width="1.5"/>
            <rect x="20"  y="22" width="40" height="22" rx="2" fill="#fff"/>
            <rect x="64"  y="22" width="40" height="22" rx="2" fill="#fff"/>
            <rect x="108" y="22" width="40" height="22" rx="2" fill="#fff"/>
            <rect x="152" y="22" width="40" height="22" rx="2" fill="#fff"/>
            <rect x="198" y="22" width="32" height="38" rx="3" fill="#fff"/>
            <path d="M238 22 H246 Q256 22 262 33 L268 44 H238 Z" fill="#fff"/>
            <rect x="12" y="62" width="262" height="2" fill="#fff"/>
            <rect x="285" y="52" width="7" height="5" rx="2" fill="#fff"/>
            <g class="lb__rueda">
              <circle cx="62" cy="76" r="15" fill="#000" stroke="#fff" stroke-width="2.5"/>
              <circle cx="62" cy="76" r="9" fill="#fff"/>
              <circle cx="62" cy="76" r="6.5" fill="#000"/>
              <rect x="61.25" y="70" width="1.5" height="12" fill="#fff"/>
              <circle cx="62" cy="76" r="2.2" fill="#fff"/>
            </g>
            <g class="lb__rueda">
              <circle cx="222" cy="76" r="15" fill="#000" stroke="#fff" stroke-width="2.5"/>
              <circle cx="222" cy="76" r="9" fill="#fff"/>
              <circle cx="222" cy="76" r="6.5" fill="#000"/>
              <rect x="221.25" y="70" width="1.5" height="12" fill="#fff"/>
              <circle cx="222" cy="76" r="2.2" fill="#fff"/>
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
