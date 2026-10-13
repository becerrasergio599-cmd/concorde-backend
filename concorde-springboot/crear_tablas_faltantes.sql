-- =========================================================
-- Tablas faltantes de Concorde para la base concorde_springboot
-- (vehiculo ya existe, por eso aqui solo se crea si no existiera)
-- =========================================================

CREATE TABLE IF NOT EXISTS vehiculo (
    id_vehiculo   SERIAL PRIMARY KEY,
    placa         VARCHAR(10) NOT NULL UNIQUE,
    capacidad     INTEGER NOT NULL CHECK (capacidad > 0),
    modelo        VARCHAR(50)
);

CREATE TABLE usuario (
    id_usuario          SERIAL PRIMARY KEY,
    telefono            VARCHAR(20),
    numero_documento    VARCHAR(30) NOT NULL UNIQUE,
    correo_electronico  VARCHAR(150) NOT NULL UNIQUE,
    contrasena_hash     VARCHAR(255) NOT NULL,
    nombre_completo     VARCHAR(150) NOT NULL,
    rol                 VARCHAR(20) NOT NULL DEFAULT 'CLIENTE'
                        CHECK (rol IN ('CLIENTE','AGENTE','ADMIN')),
    creado_en           TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE terminal (
    id_terminal   SERIAL PRIMARY KEY,
    nombre        VARCHAR(100) NOT NULL,
    ciudad        VARCHAR(100) NOT NULL,
    direccion     VARCHAR(150)
);

CREATE TABLE conductor (
    id_conductor                  SERIAL PRIMARY KEY,
    numero_licencia               VARCHAR(30) NOT NULL UNIQUE,
    nombre_completo               VARCHAR(150) NOT NULL,
    telefono                      VARCHAR(20),
    fecha_vencimiento_licencia    DATE NOT NULL
);

CREATE TABLE ruta (
    id_ruta               SERIAL PRIMARY KEY,
    id_terminal_origen    INTEGER NOT NULL REFERENCES terminal(id_terminal),
    id_terminal_destino   INTEGER NOT NULL REFERENCES terminal(id_terminal),
    valor_tiquete         NUMERIC(10,2) NOT NULL CHECK (valor_tiquete >= 0),
    CHECK (id_terminal_origen <> id_terminal_destino)
);

CREATE TABLE viaje (
    id_viaje       SERIAL PRIMARY KEY,
    id_ruta        INTEGER NOT NULL REFERENCES ruta(id_ruta),
    id_vehiculo    INTEGER NOT NULL REFERENCES vehiculo(id_vehiculo),
    id_conductor   INTEGER NOT NULL REFERENCES conductor(id_conductor),
    fecha_viaje    DATE NOT NULL,
    hora_salida    TIME NOT NULL,
    precio         NUMERIC(10,2) NOT NULL CHECK (precio >= 0),
    estado_viaje   VARCHAR(20) NOT NULL DEFAULT 'PROGRAMADO'
                   CHECK (estado_viaje IN ('PROGRAMADO','EN_CURSO','FINALIZADO','CANCELADO'))
);

CREATE TABLE asiento (
    id_asiento      SERIAL PRIMARY KEY,
    id_vehiculo     INTEGER NOT NULL REFERENCES vehiculo(id_vehiculo),
    numero_asiento  VARCHAR(10) NOT NULL,
    tipo            VARCHAR(20) NOT NULL DEFAULT 'ESTANDAR',
    disponible      BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (id_vehiculo, numero_asiento)
);

CREATE TABLE reserva (
    id_reserva      SERIAL PRIMARY KEY,
    id_usuario      INTEGER NOT NULL REFERENCES usuario(id_usuario) ON DELETE CASCADE,
    id_viaje        INTEGER NOT NULL REFERENCES viaje(id_viaje),
    fecha_inicial   DATE NOT NULL DEFAULT CURRENT_DATE,
    estado          VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE'
                    CHECK (estado IN ('PENDIENTE','CONFIRMADA','CANCELADA'))
);

CREATE TABLE detalle_reserva (
    id_detalle          SERIAL PRIMARY KEY,
    id_reserva          INTEGER NOT NULL REFERENCES reserva(id_reserva) ON DELETE CASCADE,
    id_asiento          INTEGER NOT NULL REFERENCES asiento(id_asiento),
    asiento_asignado    BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (id_reserva, id_asiento)
);

CREATE TABLE pago (
    id_pago        SERIAL PRIMARY KEY,
    id_reserva     INTEGER NOT NULL REFERENCES reserva(id_reserva) ON DELETE CASCADE,
    metodo_pago    VARCHAR(30) NOT NULL,
    valor_pagado   NUMERIC(10,2) NOT NULL CHECK (valor_pagado >= 0),
    fecha_pago     DATE NOT NULL DEFAULT CURRENT_DATE,
    estado         VARCHAR(20) NOT NULL DEFAULT 'PROCESADO'
                   CHECK (estado IN ('PROCESADO','REEMBOLSADO','FALLIDO'))
);

-- Indices utiles
CREATE INDEX idx_viaje_fecha ON viaje(fecha_viaje);
CREATE INDEX idx_reserva_usuario ON reserva(id_usuario);
CREATE INDEX idx_reserva_viaje ON reserva(id_viaje);
CREATE INDEX idx_pago_reserva ON pago(id_reserva);
CREATE INDEX idx_viaje_conductor ON viaje(id_conductor);
CREATE INDEX idx_ruta_terminales ON ruta(id_terminal_origen, id_terminal_destino);
