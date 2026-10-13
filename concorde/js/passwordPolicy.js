/* =============================================
   CONCORDE — Política de contraseñas seguras
   Módulo reutilizable: registro, perfil, panel de
   administrador y panel de agente.
   ============================================= */

'use strict';

const PasswordPolicy = {

  reglas: [
    { id: 'largo',    texto: 'Mínimo 8 caracteres',            test: v => v.length >= 8 },
    { id: 'mayus',    texto: 'Una letra mayúscula',             test: v => /[A-Z]/.test(v) },
    { id: 'minus',    texto: 'Una letra minúscula',             test: v => /[a-z]/.test(v) },
    { id: 'numero',   texto: 'Un número',                       test: v => /[0-9]/.test(v) },
    { id: 'especial', texto: 'Un carácter especial (!@#$%&*.)', test: v => /[^A-Za-z0-9]/.test(v) }
  ],

  /** Evalúa una contraseña contra todas las reglas.
   *  Retorna { cumplidas, faltantes, nivel (0-5), valida } */
  evaluar(pass) {
    pass = pass || '';
    const cumplidas = this.reglas.filter(r => r.test(pass));
    const faltantes = this.reglas.filter(r => !r.test(pass));
    return { cumplidas, faltantes, nivel: cumplidas.length, valida: faltantes.length === 0 };
  },

  /** Valida y retorna { valido, mensaje } listo para mostrar en un toast */
  validar(pass) {
    const r = this.evaluar(pass);
    if (r.valida) return { valido: true, mensaje: '' };
    return {
      valido: false,
      mensaje: 'La contraseña debe tener: ' + r.faltantes.map(f => f.texto.toLowerCase()).join(', ') + '.'
    };
  },

  /** Conecta un input de contraseña a una barra + texto de fortaleza en vivo.
   *  barra y texto son opcionales de forma independiente. */
  conectarMedidor(input, barra, texto) {
    if (!input) return;
    const colores   = ['#dc3545', '#dc3545', '#fd7e14', '#ffc107', '#20c997', '#28a745'];
    const etiquetas = ['Muy débil', 'Muy débil', 'Débil', 'Media', 'Fuerte', 'Muy fuerte'];
    input.addEventListener('input', () => {
      const r = this.evaluar(input.value);
      if (barra) {
        barra.style.width = (r.nivel / this.reglas.length) * 100 + '%';
        barra.style.background = colores[r.nivel];
      }
      if (texto) {
        if (!input.value) {
          texto.textContent = '';
        } else {
          const faltan = r.faltantes.map(f => f.texto.toLowerCase()).join(', ');
          texto.textContent = `Contraseña: ${etiquetas[r.nivel]}` + (faltan ? ` — falta: ${faltan}` : '');
        }
        texto.classList.toggle('text-success', r.valida && !!input.value);
        texto.classList.toggle('text-muted', !r.valida || !input.value);
      }
    });
  }
};
