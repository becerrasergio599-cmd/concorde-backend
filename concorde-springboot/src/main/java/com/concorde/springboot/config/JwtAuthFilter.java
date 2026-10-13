package com.concorde.springboot.config;

import com.concorde.springboot.servicio.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Se ejecuta en cada petición HTTP antes de que llegue a los
 * controladores. Si trae header "Authorization: Bearer &lt;token&gt;" y
 * el token es válido, deja al usuario autenticado en el contexto de
 * seguridad con su rol (ROLE_ADMIN / ROLE_AGENTE / ROLE_CLIENTE) para
 * que SecurityConfig pueda decidir si tiene permiso para esa ruta.
 *
 * Si no hay token, o es inválido, simplemente NO autentica y sigue --
 * son las reglas de SecurityConfig las que deciden si esa ruta en
 * particular exige estar autenticado o no.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtUtil.validarYObtenerClaims(token);
                String correo = claims.getSubject();
                String rol = claims.get("rol", String.class);

                var authentication = new UsernamePasswordAuthenticationToken(
                        correo, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException e) {
                // Token vencido, alterado o mal formado: se ignora y la
                // petición sigue sin autenticar (SecurityConfig la
                // rechazará más adelante si la ruta lo requiere).
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
