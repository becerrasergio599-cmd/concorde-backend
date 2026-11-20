
'use strict';

/* =============================================
   CONCORDE - JAVASCRIPT PRINCIPAL
   ASIENTOS REALES + BUS ANIMADO
   ============================================= */

// =============================================
// MÓDULO DE ASIENTOS - CONSERVADO
// =============================================

const MapaAsientos = {
  seleccionados: [],
  maxSeleccion: 1,
  cargado: false,
  viajeId: null,
  asientosValidos: new Map(),
  ocupados: new Set(),

  normalizar(valor) {
    return String(valor ?? '').trim().toUpperCase();
  },

  obtenerViaje() {
    try {
      const esVuelta =
        sessionStorage.getItem('concorde_seleccionando_vuelta') === 'true';

      const clave = esVuelta
        ? 'concorde_viaje_vuelta'
        : 'concorde_viaje';

      return JSON.parse(sessionStorage.getItem(clave) || 'null');
    } catch (error) {
      return null;
    }
  },

  async consultarDatos() {
    const viaje = this.obtenerViaje();

    this.viajeId = Number(
      viaje?.idViaje || viaje?.viaje?.idViaje
    );

    if (!Number.isInteger(this.viajeId) || this.viajeId <= 0) {
      throw new Error(
        'No se encontró el identificador del viaje seleccionado.'
      );
    }

    const urls = [
      '/api/asientos',
      '/api/detalle-reservas',
      `/api/viajes/${this.viajeId}`
    ];

    const respuestas = await Promise.all(
      urls.map(url => fetch(url, { cache: 'no-store' }))
    );

    for (let i = 0; i < respuestas.length; i++) {
      if (!respuestas[i].ok) {
        throw new Error(
          `${urls[i]} respondió HTTP ${respuestas[i].status}`
        );
      }
    }

    const [
      todosAsientos,
      detalles,
      viajeServidor
    ] = await Promise.all(
      respuestas.map(respuesta => respuesta.json())
    );

    if (
      !Array.isArray(todosAsientos) ||
      !Array.isArray(detalles)
    ) {
      throw new Error(
        'La API no devolvió las listas de asientos y reservas esperadas.'
      );
    }

    const idVehiculo = Number(
      viajeServidor?.vehiculo?.idVehiculo
    );

    if (!idVehiculo) {
      throw new Error(
        'El viaje no tiene un vehículo asignado.'
      );
    }

    // Mostrar solamente los asientos de este vehículo.
    const asientosDelBus = todosAsientos.filter(asiento => {
      const id = Number(
        asiento.vehiculo?.idVehiculo || asiento.idVehiculo
      );

      return id === idVehiculo;
    });

    if (!asientosDelBus.length) {
      throw new Error(
        'El vehículo no tiene asientos registrados.'
      );
    }

    this.asientosValidos = new Map(
      asientosDelBus.map(asiento => [
        this.normalizar(asiento.numeroAsiento),
        asiento
      ])
    );

    this.ocupados = new Set();

    // Comprobar reservas activas de este viaje.
    for (const detalle of detalles) {
      const reserva = detalle.reserva;

      if (!reserva) continue;

      const idViajeReserva = Number(
        reserva.viaje?.idViaje
      );

      if (idViajeReserva !== this.viajeId) {
        continue;
      }

      const estado = this.normalizar(reserva.estado);

      if (
        ['CANCELADA', 'CANCELADO', 'ANULADA'].includes(estado)
      ) {
        continue;
      }

      const numero = this.normalizar(
        detalle.asiento?.numeroAsiento
      );

      if (
        numero &&
        this.asientosValidos.has(numero)
      ) {
        this.ocupados.add(numero);
      }
    }
  },

  pintar() {
    const elementos = document.querySelectorAll(
      '.asiento[data-numero]'
    );

    elementos.forEach(elemento => {
      const numero = this.normalizar(
        elemento.dataset.numero
      );

      const dato = this.asientosValidos.get(numero);

      if (!dato) {
        elemento.style.display = 'none';
        elemento.setAttribute('aria-hidden', 'true');
        return;
      }

      elemento.style.display = '';
      elemento.removeAttribute('aria-hidden');

      const premium =
        this.normalizar(dato.tipo) === 'PREMIUM';

      const ocupado =
        dato.disponible === false ||
        this.ocupados.has(numero);

      elemento.classList.remove(
        'asiento-ocupado',
        'asiento-libre',
        'asiento-seleccionado',
        'asiento-premium'
      );

      elemento.classList.add(
        ocupado ? 'asiento-ocupado' : 'asiento-libre'
      );

      if (premium) {
        elemento.classList.add('asiento-premium');
      }

      elemento.dataset.estado = ocupado
        ? 'ocupado'
        : 'disponible';

      elemento.dataset.tipo = premium
        ? 'PREMIUM'
        : 'ESTANDAR';

      elemento.setAttribute(
        'aria-disabled',
        String(ocupado)
      );

      elemento.setAttribute(
        'aria-label',
        `Asiento ${numero}, ${
          premium ? 'Premium' : 'Estándar'
        }, ${
          ocupado ? 'ocupado' : 'disponible'
        }`
      );

      elemento.setAttribute(
        'tabindex',
        ocupado ? '-1' : '0'
      );

      elemento.setAttribute('role', 'button');
      elemento.setAttribute('aria-pressed', 'false');

      if (ocupado) {
        elemento.style.backgroundColor = '#ffe1e5';
        elemento.style.borderColor = '#e53950';
        elemento.style.color = '#a91e33';
      } else if (premium) {
        elemento.style.backgroundColor = '#fff0da';
        elemento.style.borderColor = '#f0a32a';
        elemento.style.color = '#a36500';
      } else {
        elemento.style.backgroundColor = '#e4f7e9';
        elemento.style.borderColor = '#25a244';
        elemento.style.color = '#146b2b';
      }

      elemento.style.cursor = ocupado
        ? 'not-allowed'
        : 'pointer';

      elemento.style.opacity = ocupado
        ? '0.8'
        : '1';
    });

    this.seleccionados = this.seleccionados.filter(
      numero =>
        this.asientosValidos.has(numero) &&
        !this.ocupados.has(numero)
    );

    this.actualizarResumen();
  },

  async init() {
    const elementos = document.querySelectorAll(
      '.asiento[data-numero]'
    );

    if (!elementos.length) return;

    const dataPasajeros = document.querySelector(
      '[data-pasajeros]'
    );

    const viaje = this.obtenerViaje();

    this.maxSeleccion = Number(
      dataPasajeros?.dataset.pasajeros ||
      viaje?.pasajeros ||
      1
    ) || 1;

    this.seleccionados = [];
    this.cargado = false;

    this.actualizarResumen();

    elementos.forEach(elemento => {
      elemento.setAttribute('tabindex', '-1');
      elemento.setAttribute('aria-disabled', 'true');

      elemento.addEventListener('click', () => {
        this.toggleAsiento(elemento);
      });

      elemento.addEventListener('keydown', evento => {
        if (
          evento.key === 'Enter' ||
          evento.key === ' '
        ) {
          evento.preventDefault();
          this.toggleAsiento(elemento);
        }
      });
    });

    try {
      await this.consultarDatos();

      this.cargado = true;
      this.pintar();

    } catch (error) {
      console.error(
        'Error cargando asientos reales:',
        error
      );

      elementos.forEach(elemento => {
        elemento.classList.add('asiento-ocupado');
        elemento.setAttribute(
          'aria-disabled',
          'true'
        );
        elemento.style.cursor = 'not-allowed';
      });

      UI.mostrarToast(
        'No se pudo verificar la disponibilidad. Recarga la página.',
        'danger'
      );
    }
  },

  toggleAsiento(elemento) {
    if (!this.cargado) return;

    const numero = this.normalizar(
      elemento.dataset.numero
    );

    const dato = this.asientosValidos.get(numero);

    if (
      !dato ||
      dato.disponible === false ||
      this.ocupados.has(numero)
    ) {
      UI.mostrarToast(
        'Ese asiento no está disponible para este viaje.',
        'warning'
      );
      return;
    }

    if (this.seleccionados.includes(numero)) {
      this.seleccionados = this.seleccionados.filter(
        asiento => asiento !== numero
      );

      elemento.classList.remove(
        'asiento-seleccionado'
      );

      elemento.classList.add('asiento-libre');

      elemento.setAttribute(
        'aria-pressed',
        'false'
      );

      elemento.style.backgroundColor =
        this.normalizar(dato.tipo) === 'PREMIUM'
          ? '#fff0da'
          : '#e4f7e9';

    } else {
      if (
        this.seleccionados.length >=
        this.maxSeleccion
      ) {
        UI.mostrarToast(
          `Solo puedes seleccionar ${this.maxSeleccion} asiento(s)`,
          'warning'
        );
        return;
      }

      elemento.classList.remove('asiento-libre');

      elemento.classList.add(
        'asiento-seleccionado'
      );

      elemento.setAttribute(
        'aria-pressed',
        'true'
      );

      elemento.style.backgroundColor = '#f3a51c';

      this.seleccionados.push(numero);
    }

    this.actualizarResumen();
  },

  actualizarResumen() {
    const resumen = document.getElementById(
      'resumenAsientos'
    );

    const boton = document.getElementById(
      'btnContinuarAsientos'
    );

    if (resumen) {
      resumen.textContent = this.seleccionados.length
        ? `Asientos: ${this.seleccionados.join(', ')}`
        : 'Ningún asiento seleccionado';
    }

    if (boton) {
      boton.disabled =
        !this.cargado ||
        this.seleccionados.length !==
        this.maxSeleccion;
    }
  }
};

// =============================================
// INTERFAZ GENERAL + ANIMACIÓN DEL BUS
// =============================================

const UI = {
  _loaderTimer: null,

  init() {
    this.initTooltips();
    this.initScrollAnimations();
    this.initNavbarScroll();
    this.initContadorPasajeros();
    this.initFiltrosResultados();
  },

  _inyectarEstilosLoader() {
    if (document.getElementById('estilosLoaderBus')) return;

    const style = document.createElement('style');
    style.id = 'estilosLoaderBus';

    style.textContent = `
      #loaderGlobal {
        position: fixed;
        inset: 0;
        z-index: 99999;
        display: flex;
        align-items: center;
        justify-content: center;
        background: rgba(0,32,91,.96);
      }

      .lb {
        width: min(380px,88vw);
        text-align: center;
        color: white;
        font-family: Arial,sans-serif;
      }

      .lb__escena {
        position: relative;
      }

      .lb__bus {
        display: block;
        width: 100%;
        max-width: 340px;
        margin: 0 auto;
        position: relative;
        z-index: 2;
        animation: lb-rebote .45s ease-in-out infinite alternate;
      }

      @keyframes lb-rebote {
        from { transform: translateY(0); }
        to { transform: translateY(-3px); }
      }

      .lb__rueda {
        transform-box: fill-box;
        transform-origin: center;
        animation: lb-gira .5s linear infinite;
      }

      @keyframes lb-gira {
        to { transform: rotate(360deg); }
      }

      .lb__carretera {
        height: 14px;
        margin-top: -3px;
        border-radius: 3px;
        background-color: #000;
        background-image:
          repeating-linear-gradient(
            90deg,
            #fff 0 22px,
            transparent 22px 48px
          );
        background-size: 48px 3px;
        background-repeat: repeat-x;
        background-position: 0 center;
        box-shadow: 0 -1px 0 #fff;
        animation: lb-ruta .45s linear infinite;
      }

      @keyframes lb-ruta {
        to { background-position-x: -48px; }
      }

      .lb__lineas {
        position: absolute;
        left: -26px;
        top: 36px;
        width: 44px;
        height: 50px;
        z-index: 0;
      }

      .lb__lineas span {
        position: absolute;
        right: 0;
        height: 3px;
        border-radius: 2px;
        background: white;
        opacity: 0;
        animation: lb-vel .9s linear infinite;
      }

      .lb__lineas span:nth-child(1) {
        top: 4px;
        width: 30px;
        animation-delay: 0s;
      }

      .lb__lineas span:nth-child(2) {
        top: 22px;
        width: 40px;
        animation-delay: .3s;
      }

      .lb__lineas span:nth-child(3) {
        top: 40px;
        width: 24px;
        animation-delay: .6s;
      }

      @keyframes lb-vel {
        0% {
          opacity: 0;
          transform: translateX(20px);
        }
        30% { opacity: .7; }
        100% {
          opacity: 0;
          transform: translateX(-30px);
        }
      }

      .lb__texto {
        margin-top: 20px;
        font-size: 17px;
        font-weight: 700;
        transition: opacity .3s;
      }

      @media (prefers-reduced-motion: reduce) {
        #loaderGlobal * {
          animation: none !important;
        }
      }
    `;

    document.head.appendChild(style);
  },

  mostrarLoader(mensaje = 'Cargando...') {
    this.ocultarLoader();
    this._inyectarEstilosLoader();

    const loader = document.createElement('div');
    loader.id = 'loaderGlobal';
    loader.setAttribute('role', 'status');
    loader.setAttribute('aria-live', 'polite');

    loader.innerHTML = `
      <div class="lb">
        <div class="lb__escena">

          <div class="lb__lineas">
            <span></span>
            <span></span>
            <span></span>
          </div>

          <!-- ===================================
               BUS CONCORDE NEGRO
               DISEÑO SIMILAR A LA REFERENCIA
               FRENTE Y ESPEJO A LA DERECHA
               =================================== -->

          <svg
            class="lb__bus"
            viewBox="0 0 600 230"
            xmlns="http://www.w3.org/2000/svg"
            aria-hidden="true">

            <!-- Carrocería negra -->
            <path
              d="
                M45 85
                Q45 67 64 67
                H405
                Q445 67 474 83
                Q509 102 528 140
                Q538 159 538 180
                Q538 190 527 190
                H53
                Q40 190 40 177
                V100
                Q40 85 45 85
                Z"
              fill="#050505"
              stroke="#ffffff"
              stroke-width="2"/>

            <!-- Ventana trasera -->
            <path
              d="
                M66 82
                H129
                L146 127
                H67
                Q56 127 56 115
                V94
                Q56 82 66 82
                Z"
              fill="#ffffff"/>

            <!-- Ventana 2 -->
            <path
              d="
                M144 82
                H215
                L232 127
                H161
                Z"
              fill="#ffffff"/>

            <!-- Ventana 3 -->
            <path
              d="
                M230 82
                H300
                L317 127
                H247
                Z"
              fill="#ffffff"/>

            <!-- Ventana 4 -->
            <path
              d="
                M316 82
                H386
                L403 127
                H333
                Z"
              fill="#ffffff"/>

            <!-- Parabrisas delantero -->
            <path
              d="
                M401 82
                H417
                Q454 82 480 106
                Q495 120 502 141
                Q507 156 492 156
                H427
                Q414 156 409 143
                Z"
              fill="#ffffff"/>

            <!-- ESPEJO RETROVISOR DELANTERO UNIDO AL BUS -->
            <path
              d="M523 135 L536 135 Q545 135 548 145"
              fill="none"
              stroke="#ffffff"
              stroke-width="5"
              stroke-linecap="round"
              stroke-linejoin="round"/>

            <!-- Carcasa del espejo, conectada al soporte -->
            <path
              d="M545 140 Q551 138 554 144 L558 155 Q559 160 553 160 L550 160 Q547 158 547 153 Z"
              fill="#050505"
              stroke="#ffffff"
              stroke-width="3"
              stroke-linejoin="round"/>

            <!--
              TRES RUEDAS:
              DOS TRASERAS A LA IZQUIERDA
              UNA DELANTERA A LA DERECHA
            -->

            <!-- Rueda trasera 1 -->
            <g class="lb__rueda">
              <circle
                cx="126"
                cy="190"
                r="27"
                fill="#050505"
                stroke="#ffffff"
                stroke-width="5"/>

              <circle
                cx="126"
                cy="190"
                r="16"
                fill="none"
                stroke="#ffffff"
                stroke-width="4"/>

              <path
                d="M126 179 V201"
                stroke="#ffffff"
                stroke-width="3"
                stroke-linecap="round"/>
            </g>

            <!-- Rueda trasera 2 -->
            <g class="lb__rueda">
              <circle
                cx="182"
                cy="190"
                r="27"
                fill="#050505"
                stroke="#ffffff"
                stroke-width="5"/>

              <circle
                cx="182"
                cy="190"
                r="16"
                fill="none"
                stroke="#ffffff"
                stroke-width="4"/>

              <path
                d="M182 179 V201"
                stroke="#ffffff"
                stroke-width="3"
                stroke-linecap="round"/>
            </g>

            <!-- Rueda delantera -->
            <g class="lb__rueda">
              <circle
                cx="453"
                cy="190"
                r="27"
                fill="#050505"
                stroke="#ffffff"
                stroke-width="5"/>

              <circle
                cx="453"
                cy="190"
                r="16"
                fill="none"
                stroke="#ffffff"
                stroke-width="4"/>

              <path
                d="M453 179 V201"
                stroke="#ffffff"
                stroke-width="3"
                stroke-linecap="round"/>
            </g>

          </svg>

          <div class="lb__carretera"></div>
        </div>

        <p class="lb__texto" id="lbTexto"></p>
      </div>
    `;

    loader.querySelector('#lbTexto').textContent = mensaje;
    document.body.appendChild(loader);

    if (/trayectos|viajes|rutas/i.test(mensaje)) {
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
          if (!texto.isConnected) return;
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

  mostrarToast(mensaje, tipo = 'info') {
    const colores = {
      info: 'var(--azul-claro, #1673c9)',
      success: 'var(--verde-exito, #198754)',
      warning: 'var(--dorado, #d6a02e)',
      danger: 'var(--rojo-error, #dc3545)'
    };

    const toast = document.createElement('div');
    toast.className =
      'position-fixed bottom-0 end-0 m-3 p-3 rounded-3 text-white shadow-lg';

    toast.style.cssText = `
      background: ${colores[tipo] || colores.info};
      z-index: 999999;
      max-width: 300px;
      animation: fadeInUp .3s ease;
    `;

    toast.setAttribute('role', 'alert');
    toast.setAttribute('aria-live', 'polite');
    toast.textContent = mensaje;

    document.body.appendChild(toast);

    setTimeout(() => toast.remove(), 3500);
  },

  initTooltips() {
    if (typeof bootstrap === 'undefined') return;

    document.querySelectorAll(
      '[data-bs-toggle="tooltip"]'
    ).forEach(el => new bootstrap.Tooltip(el));
  },

  initScrollAnimations() {
    if (typeof IntersectionObserver === 'undefined') return;

    const observer = new IntersectionObserver(entries => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add('visible');
        }
      });
    }, { threshold: 0.1 });

    document.querySelectorAll('.fade-in-up')
      .forEach(el => observer.observe(el));
  },

  initNavbarScroll() {
    const navbar = document.querySelector('.navbar-concorde');
    if (!navbar) return;

    window.addEventListener('scroll', () => {
      if (window.scrollY > 50) {
        navbar.style.boxShadow =
          '0 4px 30px rgba(0,0,0,0.3)';
      } else {
        navbar.style.boxShadow = '';
      }
    });
  },

  initContadorPasajeros() {
    // El index.html actual ya gestiona este contador.
    // Evitar registrar eventos duplicados en esa página.
    if (document.getElementById('formBusqueda')) return;

    document.querySelectorAll('.contador-pasajeros')
      .forEach(grupo => {
        const input = grupo.querySelector('input');
        const btnMas = grupo.querySelector('.btn-mas');
        const btnMenos = grupo.querySelector('.btn-menos');

        if (!input || !btnMas || !btnMenos) return;

        const min = parseInt(input.min) || 1;
        const max = parseInt(input.max) || 9;

        btnMas.addEventListener('click', () => {
          if (parseInt(input.value) < max) {
            input.value = parseInt(input.value) + 1;
          }
        });

        btnMenos.addEventListener('click', () => {
          if (parseInt(input.value) > min) {
            input.value = parseInt(input.value) - 1;
          }
        });
      });
  },

  initFiltrosResultados() {
    if (document.getElementById('listaTrayectos')) return;

    document.querySelectorAll('[data-filtro]')
      .forEach(filtro => {
        filtro.addEventListener('change', () => {
          UI.mostrarToast('Aplicando filtros...', 'info');
        });
      });
  }
};

// =============================================
// INICIALIZACIÓN GLOBAL
// =============================================

document.addEventListener('DOMContentLoaded', () => {
  UI.init();
  MapaAsientos.init();
});
