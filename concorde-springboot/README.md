# concorde-springboot

Proyecto Spring Boot + PostgreSQL basado en el sistema Concorde, con las 10
tablas completas (ya no solo Vehiculo).

## Antes de correr

1. Crea la base si no existe: `CREATE DATABASE concorde_springboot;`
2. Corre `crear_tablas_faltantes.sql` (crea todas las tablas si no existen).
3. Edita `src/main/resources/application.properties` con tu contraseña real
   de PostgreSQL:
   ```properties
   spring.datasource.password=TU_PASSWORD
   ```
4. Nota: `spring.jpa.hibernate.ddl-auto=none` a propósito -- Hibernate NO va
   a intentar crear/alterar tablas solo. El esquema lo manejas tú con los
   scripts `.sql`. Esto evita los errores que ya tuviste antes con el otro
   proyecto (Hibernate chocando con columnas/vistas existentes).

## Endpoints disponibles

Todos siguen el mismo patron GET/POST/PUT/DELETE:

| Entidad | Endpoint base |
|---|---|
| Usuario | `/api/usuarios` |
| Terminal | `/api/terminales` |
| Conductor | `/api/conductores` |
| Ruta | `/api/rutas` |
| Vehiculo | `/api/vehiculos` |
| Viaje | `/api/viajes` |
| Asiento | `/api/asientos` |
| Reserva | `/api/reservas` |
| DetalleReserva | `/api/detalle-reservas` |
| Pago | `/api/pagos` |

Ejemplo para ver todos los usuarios: `http://localhost:8082/api/usuarios`

## Seguridad: contraseñas y registro público

- Las contraseñas ahora se guardan con **BCrypt** (`PasswordUtil`), no en
  texto plano. Si tu base ya tenía usuarios con contraseña en texto
  plano de antes de este cambio, no se rompen: al iniciar sesión
  correctamente una vez, esa contraseña se reemplaza por su hash
  BCrypt automáticamente (migración transparente, sin tocar nada a
  mano en la base de datos).
- `POST /api/registro` es el endpoint público para que un visitante
  cree su cuenta (usado por `p04-registro.html`). A propósito es
  distinto de `POST /api/usuarios`: este último lo usan los paneles de
  administrador/agente (ya autenticados) para crear usuarios con
  cualquier rol; el registro público **siempre asigna el rol CLIENTE
  en el servidor**, sin importar qué mande el navegador, para que
  nadie pueda auto-asignarse el rol ADMINISTRADOR editando la
  petición.
- El CORS ya no acepta cualquier origen (`*`); revisa
  `CorsConfig.java` si despliegas el proyecto en otro dominio o
  puerto.

## Autenticación con JWT (Spring Security)

Ya no basta con "estar logueado en el navegador": la API exige un
token para las rutas protegidas.

- `POST /api/login` ahora devuelve, además de los datos del usuario,
  un campo `token` (JWT válido por 8 horas).
- El frontend (`auth.js`) guarda ese token en `sessionStorage` y
  **intercepta automáticamente `fetch`** para agregarle el header
  `Authorization: Bearer <token>` a toda petición a la API que lo
  necesite. No hubo que editar cada llamada de cada página una por
  una — se resolvió en un solo lugar.
- Reglas de acceso (`SecurityConfig.java`):
  - **Público** (sin token): páginas estáticas, `/api/login`,
    `/api/registro`, todo `/api/chatbot/**`, y la búsqueda de viajes,
    rutas, terminales y vehículos (para poder cotizar antes de
    iniciar sesión).
  - **Solo ADMIN o AGENTE**: listar/crear usuarios, reportes,
    conductores y personas, y crear/editar catálogo (rutas, viajes,
    terminales, vehículos).
  - **Cualquier usuario logueado**: ver/editar su propio perfil,
    reservas, pagos y asientos.
  - **Borrar cualquier cosa**: solo ADMIN o AGENTE.
- Limitación conocida: no hay verificación de que un `id` en la URL
  sea el del propio usuario (ej. un cliente autenticado técnicamente
  podría pedir el perfil de otro cliente si adivina su id). Para el
  alcance de este proyecto no se implementó ese control fino por
  fila; queda anotado como mejora futura.

## Chatbot de atención al cliente

Corre también `crear_tablas_chat.sql` (crea `consulta_chat` y `mensaje_chat`
si no existen). Endpoints del chatbot, todos bajo `/api/chatbot`:

| Método | Endpoint | Uso |
|---|---|---|
| POST | `/api/chatbot/iniciar` | Abre una conversación nueva y envía el saludo del bot |
| POST | `/api/chatbot/mensaje` | El cliente escribe; el bot responde o escala a un asesor |
| GET | `/api/chatbot/consultas/{id}/mensajes` | Historial completo de una conversación |
| GET | `/api/chatbot/consultas` | Tickets pendientes (por defecto `ESCALADA`,`EN_ATENCION`) para el panel de agente |
| POST | `/api/chatbot/consultas/{id}/responder` | Un agente responde y toma el ticket |
| POST | `/api/chatbot/consultas/{id}/cerrar` | Cierra la conversación |

El motor de respuestas (`MotorFaqChatbot`) es una base de preguntas
frecuentes por palabras clave (sin IA externa): si el cliente pide un
asesor o ninguna pregunta frecuente coincide, la conversación pasa a
`ESCALADA` para que un agente humano la atienda desde `agente-panel.html`.

## Crear un registro con llave foranea (POST)

Para las entidades que referencian a otra (por ejemplo Ruta necesita dos
Terminal), el JSON del body debe traer el id anidado. Ejemplo, crear una
Ruta con Postman/Thunder Client (`POST http://localhost:8082/api/rutas`):

```json
{
  "terminalOrigen": { "idTerminal": 1 },
  "terminalDestino": { "idTerminal": 2 },
  "valorTiquete": 25000
}
```

Mismo patron para Viaje (`ruta`, `vehiculo`, `conductor`), Reserva
(`usuario`, `viaje`), DetalleReserva (`reserva`, `asiento`) y Pago
(`reserva`) -- siempre con el id ya existente en la base.
