// aviso-agotados.js
// Muestra un aviso en la parte superior de la página si hay viajes sin
// cupo disponible. Se agrega una sola vez, en cualquier página del
// sitio cliente donde tenga sentido avisar (por ejemplo index.html).
//
// Cómo usarlo: agrega esta línea justo antes de cerrar </body>,
// después de <script src="js/auth.js"></script>:
//   <script src="js/aviso-agotados.js"></script>

(function () {
  const API = 'http://localhost:8082/api';

  async function mostrarAvisoAgotados() {
    try {
      const agotados = await fetch(`${API}/reportes/viajes-agotados`).then(r => r.json());
      if (!agotados.length) return;

      const banner = document.createElement('div');
      banner.setAttribute('role', 'alert');
      banner.style.cssText = `
        background:#fff3cd; color:#664d03; border-bottom:1px solid #ffe69c;
        padding:.75rem 1.25rem; font-size:.9rem; display:flex; align-items:center;
        justify-content:center; gap:.5rem; text-align:center;
      `;
      banner.innerHTML = `
        <i class="bi bi-exclamation-triangle-fill" aria-hidden="true"></i>
        <span>
          ${agotados.length} viaje${agotados.length > 1 ? 's' : ''} agotado${agotados.length > 1 ? 's' : ''} en este momento:
          ${agotados.map(v => `${v.ruta} (${v.fechaViaje})`).join(', ')}.
          Revisa otras fechas u horarios disponibles.
        </span>
        <button type="button" aria-label="Cerrar aviso" style="background:none;border:none;font-size:1.1rem;line-height:1;cursor:pointer;color:#664d03;">&times;</button>
      `;
      banner.querySelector('button').onclick = () => banner.remove();

      document.body.insertBefore(banner, document.body.firstChild);
    } catch (e) {
      // Si la API no responde, simplemente no se muestra el aviso -- no
      // debe romper la navegación normal del sitio.
      console.warn('No se pudo consultar viajes agotados:', e);
    }
  }

  document.addEventListener('DOMContentLoaded', mostrarAvisoAgotados);
})();
