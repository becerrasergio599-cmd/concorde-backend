package com.concorde.springboot.config;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Se aplica automáticamente a TODOS los @RestController del proyecto
// (no hay que agregar nada en cada uno). Cuando la base de datos
// rechaza un INSERT/UPDATE por un dato repetido (ej. dos usuarios con
// el mismo teléfono), Postgres devuelve un mensaje técnico mencionando
// el nombre de la restricción. Aquí se traduce a un mensaje formal que
// cualquier persona pueda entender, sin mencionar "llaves" ni nombres
// internos de la base de datos.
//
// Nota: se extrae el nombre de la restricción con una expresión regular
// sobre el texto del error, en vez de importar la clase PSQLException
// del driver -- así no depende de que esa dependencia esté en scope de
// compilación (por defecto el driver de Postgres solo está en scope
// "runtime" en el pom.xml).
@RestControllerAdvice
public class ManejadorErrores {

    private static final Pattern PATRON_RESTRICCION = Pattern.compile("constraint \"([^\"]+)\"");

    private static final Map<String, String> CAMPOS_AMIGABLES = Map.of(
            "telefono", "número de teléfono",
            "correo_electronico", "correo electrónico",
            "numero_documento", "número de documento",
            "numero_licencia", "número de licencia",
            "placa", "placa",
            "nombre_rol", "nombre de rol"
    );

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> manejarIntegridad(DataIntegrityViolationException ex) {
        Throwable causa = ex.getRootCause();
        String textoCausa = causa != null ? causa.getMessage() : "";
        if (textoCausa == null) textoCausa = "";

        String restriccion = null;
        Matcher m = PATRON_RESTRICCION.matcher(textoCausa);
        if (m.find()) {
            restriccion = m.group(1);
        }

        String mensaje = "Ya existe un registro con esos datos, o algún dato no es válido.";
        if (restriccion != null) {
            String base = restriccion.replaceAll("_(key|unique|fk)$", "");
            for (Map.Entry<String, String> entrada : CAMPOS_AMIGABLES.entrySet()) {
                if (base.endsWith(entrada.getKey())) {
                    mensaje = "Ya existe un registro con ese " + entrada.getValue() + ".";
                    break;
                }
            }
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("detail", mensaje));
    }
}