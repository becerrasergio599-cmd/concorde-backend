-- =========================================================
-- Tablas para el chatbot de atención al cliente (Concorde)
-- Correr sobre la base concorde_springboot ya existente.
-- =========================================================

CREATE TABLE IF NOT EXISTS consulta_chat (
    id_consulta        SERIAL PRIMARY KEY,
    id_usuario         INTEGER REFERENCES usuario(id_usuario),
    id_agente          INTEGER REFERENCES usuario(id_usuario),
    nombre_contacto    VARCHAR(150),
    correo_contacto    VARCHAR(150),
    asunto             VARCHAR(200),
    estado             VARCHAR(20) NOT NULL DEFAULT 'BOT'
                       CHECK (estado IN ('BOT','ESCALADA','EN_ATENCION','CERRADA')),
    creado_en          TIMESTAMP NOT NULL DEFAULT now(),
    actualizado_en     TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS mensaje_chat (
    id_mensaje     SERIAL PRIMARY KEY,
    id_consulta    INTEGER NOT NULL REFERENCES consulta_chat(id_consulta) ON DELETE CASCADE,
    remitente      VARCHAR(20) NOT NULL CHECK (remitente IN ('CLIENTE','BOT','AGENTE')),
    contenido      TEXT NOT NULL,
    enviado_en     TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_consulta_estado ON consulta_chat(estado);
CREATE INDEX IF NOT EXISTS idx_mensaje_consulta ON mensaje_chat(id_consulta);
