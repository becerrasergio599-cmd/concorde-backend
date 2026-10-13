/* =============================================
   CONCORDE TRANSPORTE TERRESTRE
   Navbar dinámica con sesión + Footer
   ============================================= */

function inyectarNavbar() {
  const nav = document.getElementById('navbar-placeholder');
  if (!nav) return;

  const user = (typeof Auth !== 'undefined') ? Auth.getUsuario() : null;

  let bloqueUsuario = '';
  if (user) {
    const ROLES_VISUAL = {
      administrador: { color: 'var(--dorado)', icono: 'bi-shield-fill' },
      agente:        { color: '#3498db',       icono: 'bi-headset' },
      cliente:       { color: '#2ecc71',       icono: 'bi-person-fill' }
    };
    const visual = ROLES_VISUAL[user.rol] || ROLES_VISUAL.cliente;
    const colorRol  = visual.color;
    const iconoRol  = visual.icono;
    const panelLink = user.rol === 'administrador'
      ? `<li><a class="dropdown-item" href="admin-panel.html"><i class="bi bi-speedometer2 me-2"></i>Panel Admin</a></li>`
      : `<li><a class="dropdown-item" href="p11-reservas.html"><i class="bi bi-ticket-detailed me-2"></i>Mis Reservas</a></li>
         <li><a class="dropdown-item" href="p16-perfil.html"><i class="bi bi-person-gear me-2"></i>Mi Perfil</a></li>`;

    bloqueUsuario = `
      <li class="nav-item ms-lg-3 dropdown">
        <a class="nav-link dropdown-toggle d-flex align-items-center gap-2 p-0 py-2 py-lg-0"
           href="#" id="userDropdown" role="button"
           data-bs-toggle="dropdown" aria-expanded="false"
           aria-label="Menú de usuario: ${user.nombre}">
          <span style="width:36px;height:36px;border-radius:50%;background:${colorRol};
            color:var(--azul-oscuro);font-weight:700;font-size:.75rem;
            display:flex;align-items:center;justify-content:center;flex-shrink:0;
            position:relative;" aria-hidden="true">
            ${user.avatar}
            <span style="position:absolute;bottom:1px;right:1px;width:8px;height:8px;
              background:#2ecc71;border-radius:50%;border:2px solid var(--azul-oscuro)"></span>
          </span>
          <span class="d-none d-lg-block text-start" style="line-height:1.2">
            <span style="color:#fff;font-weight:600;font-size:.85rem;display:block">${user.nombre}</span>
            <span style="color:${colorRol};font-size:.7rem;text-transform:uppercase;letter-spacing:.6px">
              <i class="bi ${iconoRol} me-1" aria-hidden="true"></i>${user.rol}
            </span>
          </span>
        </a>
        <ul class="dropdown-menu dropdown-menu-end shadow" aria-labelledby="userDropdown" style="min-width:220px;border-radius:12px;overflow:hidden;border:1px solid rgba(0,0,0,.08)">
          <li>
            <div class="px-3 py-2 border-bottom" style="background:var(--gris-claro)">
              <div class="fw-bold small">${user.nombre}</div>
              <div class="text-muted" style="font-size:.75rem">${user.email}</div>
              <span class="badge mt-1" style="background:${colorRol};color:var(--azul-oscuro);font-size:.65rem;padding:.25em .7em;border-radius:20px">
                ${user.rol.toUpperCase()}
              </span>
            </div>
          </li>
          ${panelLink}
          <li><hr class="dropdown-divider my-1"></li>
          <li>
            <button class="dropdown-item text-danger" onclick="Auth.logout()">
              <i class="bi bi-box-arrow-right me-2"></i>Cerrar sesión
            </button>
          </li>
        </ul>
      </li>`;
  } else {
    bloqueUsuario = `
      <li class="nav-item ms-lg-2">
        <a class="nav-link btn-nav-login" href="p03-login.html">
          <i class="bi bi-person-circle me-1" aria-hidden="true"></i>Iniciar sesión
        </a>
      </li>`;
  }

  const esAdmin = user && user.rol === 'administrador';

  nav.innerHTML = `
  <a class="skip-link" href="#contenido-principal">Saltar al contenido principal</a>
  <nav class="navbar navbar-concorde navbar-expand-lg" role="navigation" aria-label="Navegación principal">
    <div class="container">
      <a class="navbar-brand" href="${esAdmin ? 'admin-panel.html' : 'index.html'}" aria-label="Concorde Transporte Terrestre - Inicio">
        <span class="logo-icono" aria-hidden="true"><img src="img/LogoConcorde.png" alt="Logo Concorde" style="height: 40px;"></span>
        <div>
          <span class="logo-texto">CONCORDE</span>
          <span class="logo-subtexto">Transporte Terrestre</span>
        </div>
      </a>
      <button class="navbar-toggler" type="button" data-bs-toggle="collapse"
        data-bs-target="#navbarPrincipal" aria-controls="navbarPrincipal"
        aria-expanded="false" aria-label="Abrir menú de navegación">
        <span class="navbar-toggler-icon"></span>
      </button>
      <div class="collapse navbar-collapse" id="navbarPrincipal">
        <ul class="navbar-nav ms-auto align-items-lg-center gap-lg-1">
          ${esAdmin ? `
          <li class="nav-item"><a class="nav-link" href="admin-panel.html">Dashboard</a></li>
          <li class="nav-item"><a class="nav-link" href="admin-panel.html">Usuarios</a></li>
          <li class="nav-item"><a class="nav-link" href="admin-panel.html">Rutas</a></li>
          <li class="nav-item"><a class="nav-link" href="admin-panel.html">Reservas</a></li>
          ` : `
          <li class="nav-item"><a class="nav-link" href="index.html">Inicio</a></li>
          <li class="nav-item"><a class="nav-link" href="p02-resultados.html">Trayectos</a></li>
          <li class="nav-item"><a class="nav-link" href="p11-reservas.html">Mis Reservas</a></li>
          <li class="nav-item"><a class="nav-link" href="p12-atencion.html">Atención al Cliente</a></li>
          `}
          ${bloqueUsuario}
        </ul>
      </div>
    </div>
  </nav>`;
}

function inyectarFooter() {
  const footer = document.getElementById('footer-placeholder');
  if (!footer) return;
  footer.innerHTML = `
  <footer role="contentinfo">
    <div class="container">
      <div class="row g-4">
        <div class="col-lg-4 col-md-6">
          <div class="d-flex align-items-center gap-2 mb-3">
            <span class="logo-icono" aria-hidden="true"><img src="img/LogoConcorde.png" alt="Logo Concorde" style="height: 40px;"></span>
            <div>
              <div class="logo-texto" style="font-size:1.2rem">CONCORDE</div>
              <div class="logo-subtexto">Transporte Terrestre</div>
            </div>
          </div>
          <p style="font-size:0.88rem;opacity:0.7;line-height:1.7">
            Más de 20 años conectando Colombia con seguridad, comodidad y puntualidad.
            Tu destino, nuestra misión.
          </p>
          <div class="redes-sociales mt-3">
            <a href="#" aria-label="Facebook de Concorde Transporte"><i class="bi bi-facebook"></i></a>
            <a href="#" aria-label="Instagram de Concorde Transporte"><i class="bi bi-instagram"></i></a>
            <a href="#" aria-label="Twitter de Concorde Transporte"><i class="bi bi-twitter-x"></i></a>
            <a href="#" aria-label="WhatsApp de Concorde Transporte"><i class="bi bi-whatsapp"></i></a>
          </div>
        </div>
        <div class="col-lg-2 col-md-6 col-6">
          <h5>Destinos</h5>
          <nav aria-label="Destinos disponibles">
            <a href="p02-resultados.html">Tunja</a>
            <a href="p02-resultados.html">Bogotá</a>
            <a href="p02-resultados.html">Sogamoso</a>
            <a href="p02-resultados.html">Duitama</a>
            <a href="p02-resultados.html">Yopal</a>
            <a href="p02-resultados.html">Ver todos</a>
          </nav>
        </div>
        <div class="col-lg-2 col-md-6 col-6">
          <h5>Mi Cuenta</h5>
          <nav aria-label="Links de cuenta">
            <a href="p03-login.html">Iniciar sesión</a>
            <a href="p04-registro.html">Registrarse</a>
            <a href="p16-perfil.html">Mi perfil</a>
            <a href="p11-reservas.html">Mis reservas</a>
            <a href="p13-recuperar.html">Recuperar contraseña</a>
          </nav>
        </div>
        <div class="col-lg-2 col-md-6 col-6">
          <h5>Ayuda</h5>
          <nav aria-label="Links de ayuda">
            <a href="p12-atencion.html">Atención al cliente</a>
            <a href="p99-accesibilidad.html">Accesibilidad</a>
            <a href="#">Política de equipaje</a>
            <a href="#">Términos y condiciones</a>
          </nav>
        </div>
        <div class="col-lg-2 col-md-6 col-6">
          <h5>Contacto</h5>
          <address style="font-style:normal;font-size:0.88rem;opacity:0.7;line-height:2">
            <i class="bi bi-telephone-fill me-2" aria-hidden="true"></i>01-800-266-267<br>
            <i class="bi bi-envelope-fill me-2" aria-hidden="true"></i>info@concordetransporte.co<br>
            <i class="bi bi-geo-alt-fill me-2" aria-hidden="true"></i>Tunja, Boyacá, Colombia<br>
            <i class="bi bi-clock-fill me-2" aria-hidden="true"></i>Lun–Dom 4:00am – 11:00pm
          </address>
        </div>
      </div>
      <hr class="footer-divider">
      <p class="copyright">
        © 2025 Concorde Transporte Terrestre S.A.S. Todos los derechos reservados.
        Proyecto SENA — Ficha 3171149-B
      </p>
    </div>
  </footer>`;
}

// Inyecta el widget flotante de chat de atención al cliente (js/chatbot.js)
// en toda página que cargue componentes.js, sin tener que agregar el
// <script> manualmente en cada .html.
function inyectarChatbot() {
  if (document.getElementById('concorde-chatbot-script')) return;
  const script = document.createElement('script');
  script.id = 'concorde-chatbot-script';
  script.src = 'js/chatbot.js';
  document.body.appendChild(script);
}

document.addEventListener('DOMContentLoaded', () => {
  inyectarNavbar();
  inyectarFooter();
  inyectarChatbot();
  const pagina = window.location.pathname.split('/').pop() || 'index.html';
  document.querySelectorAll('.nav-link').forEach(link => {
    if (link.getAttribute('href') === pagina) {
      link.classList.add('active');
      link.setAttribute('aria-current', 'page');
    }
  });
});
