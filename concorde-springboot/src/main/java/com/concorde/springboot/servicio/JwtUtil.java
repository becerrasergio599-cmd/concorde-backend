package com.concorde.springboot.servicio;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Genera y valida los tokens JWT que usa toda la API para saber quién
 * hace cada petición, sin depender de sesiones de servidor (stateless).
 *
 * Nota para la sustentación: la clave de firma debería vivir en una
 * variable de entorno, nunca en el código fuente -- se deja aquí
 * embebida por simplicidad, ya que es un proyecto académico.
 */
@Service
public class JwtUtil {

    private static final String CLAVE_SECRETA =
            "concorde-transporte-terrestre-clave-secreta-para-firmar-tokens-jwt-cambiar-en-produccion";
    private static final long DURACION_MS = 8L * 60 * 60 * 1000; // 8 horas

    private final SecretKey clave = Keys.hmacShaKeyFor(CLAVE_SECRETA.getBytes());

    public String generarToken(Integer idUsuario, String correo, String rol) {
        Date ahora = new Date();
        Date expira = new Date(ahora.getTime() + DURACION_MS);
        return Jwts.builder()
                .subject(correo)
                .claim("idUsuario", idUsuario)
                .claim("rol", rol)
                .issuedAt(ahora)
                .expiration(expira)
                .signWith(clave)
                .compact();
    }

    /** Lanza JwtException si el token no es válido, está corrupto o ya expiró. */
    public Claims validarYObtenerClaims(String token) {
        return Jwts.parser()
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
