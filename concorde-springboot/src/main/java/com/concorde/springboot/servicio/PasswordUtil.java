package com.concorde.springboot.servicio;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Utilidad central de contraseñas: nadie en el proyecto debería volver
 * a comparar o guardar una contraseña "a mano" (con .equals()) — todo
 * pasa por aquí para que el criterio sea uno solo.
 *
 * Se mantiene compatible con los datos que ya existan en la base de
 * antes de este cambio (contraseñas en texto plano): si el valor
 * guardado no tiene formato de hash BCrypt, se compara literalmente
 * y, si coincide, se reemplaza por su hash en ese mismo momento
 * (ver LoginController). Así ningún usuario existente pierde acceso
 * y la base se va "auto-migrando" a hashes reales con el uso normal.
 */
@Service
public class PasswordUtil {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String hash(String contrasenaPlana) {
        return encoder.encode(contrasenaPlana);
    }

    /** true si el valor guardado ya es un hash BCrypt ($2a$/$2b$/$2y$...). */
    public boolean esHashBcrypt(String valorGuardado) {
        return valorGuardado != null && valorGuardado.matches("^\\$2[aby]\\$.{56}$");
    }

    public boolean coincide(String contrasenaEscrita, String valorGuardado) {
        if (contrasenaEscrita == null || valorGuardado == null) return false;
        if (esHashBcrypt(valorGuardado)) {
            return encoder.matches(contrasenaEscrita, valorGuardado);
        }
        // Dato antiguo sin hashear todavía: se compara tal cual.
        return contrasenaEscrita.equals(valorGuardado);
    }
}
