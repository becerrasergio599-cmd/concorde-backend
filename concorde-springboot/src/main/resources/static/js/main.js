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

      inputFecha.addEventListener('change', () => {
        if (inputFechaVuelta) {
          inputFechaVuelta.min = inputFecha.value;
          if (inputFechaVuelta.value && inputFechaVuelta.value < inputFecha.value) {
            inputFechaVuelta.value = inputFecha.value;
          }
        }
      });
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

    // Validación y submit
    form.addEventListener('submit', (e) => {
      e.preventDefault();
      if (this.validarFormBusqueda(form)) {
        UI.mostrarLoader('Buscando trayectos disponibles...');
        setTimeout(() => {
          window.location.href = 'p02-resultados.html';
        }, 1500);
      }
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

      form.addEventListener('submit', (e) => {
        e.preventDefault();
        if (this.validarForm(form)) {
          this.procesarForm(form);
        }
      });
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

  mostrarLoader(mensaje = 'Cargando...') {
    const loader = document.createElement('div');
    loader.className = 'loader-overlay';
    loader.id = 'loaderGlobal';
    loader.setAttribute('role', 'status');
    loader.setAttribute('aria-live', 'polite');
    loader.innerHTML = `
      <div class="loader-avion" aria-hidden="true">✈️</div>
      <p class="mt-3 fw-semibold">${mensaje}</p>
    `;
    document.body.appendChild(loader);
  },

  ocultarLoader() {
    const loader = document.getElementById('loaderGlobal');
    if (loader) loader.remove();
  },

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
