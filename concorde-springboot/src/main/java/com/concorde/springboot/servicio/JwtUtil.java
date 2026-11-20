
package com.concorde.springboot.servicio;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtUtil {

    private static final long DURACION_MS =
            8L * 60 * 60 * 1000; // 8 horas

    private final SecretKey clave;

    public JwtUtil(
            @Value("${JWT_SECRET}") String claveSecreta
    ) {
        if (claveSecreta == null ||
                claveSecreta.getBytes(StandardCharsets.UTF_8).length < 64) {
            throw new IllegalStateException(
                    "JWT_SECRET debe tener al menos 64 bytes UTF-8."
            );
        }

        this.clave = Keys.hmacShaKeyFor(
                claveSecreta.getBytes(StandardCharsets.UTF_8)
        );
    }

    // ==========================================
    // GENERAR TOKEN JWT
    // ==========================================

    public String generarToken(
            Integer idUsuario,
            String correo,
            String rol
    ) {
        Date ahora = new Date();

        Date expira = new Date(
                ahora.getTime() + DURACION_MS
        );

        return Jwts.builder()
                .subject(correo)
                .claim("idUsuario", idUsuario)
                .claim("rol", rol)
                .issuedAt(ahora)
                .expiration(expira)
                .signWith(clave)
                .compact();
    }

    // ==========================================
    // VALIDAR TOKEN JWT
    // ==========================================

    public Claims validarYObtenerClaims(String token) {

        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException(
                    "El token JWT esta vacio."
            );
        }

        return Jwts.parser()
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
