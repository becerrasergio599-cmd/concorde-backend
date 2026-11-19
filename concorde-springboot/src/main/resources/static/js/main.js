
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

    // Mostrar solamente los asientos del bus de este viaje.
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

    // Comprobar qué asientos tienen una reserva activa
    // PARA ESTE VIAJE, no para todos los viajes del bus.
    for (const detalle of detalles) {
      const reserva = detalle.reserva;

      if (!reserva) continue;

      const idViajeReserva = Number(
        reserva.viaje?.idViaje
      );

      if (idViajeReserva !== this.viajeId) {
        continue;
      }

      const estado = this.normalizar(
        reserva.estado
      );

      // Una reserva cancelada no ocupa el asiento.
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

      // No mostrar asientos que no existen en el bus.
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

      // Eliminar los colores de prueba anteriores.
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

      // COLORES REALES
      // Rojo: reservado
      // Naranja claro: Premium
      // Verde: disponible
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

    // Eliminar selecciones que ya no sean válidas.
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

      // Por seguridad no permitir seleccionar
      // cuando no se puede comprobar disponibilidad.
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

    // DESELECCIONAR
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

      // SELECCIONAR
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
