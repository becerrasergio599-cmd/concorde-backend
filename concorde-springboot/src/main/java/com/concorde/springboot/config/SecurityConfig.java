package com.concorde.springboot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Reglas de acceso de la API, en un solo lugar:
 *
 *  - Público (sin token): las páginas estáticas, login, registro, el
 *    chatbot (lo puede usar cualquier visitante) y la búsqueda de
 *    viajes/rutas/terminales (para poder cotizar antes de iniciar
 *    sesión, como en cualquier página de venta de tiquetes real).
 *  - Solo ADMIN o AGENTE: gestión de usuarios, reportes, conductores
 *    y personas -- son datos internos/administrativos, no algo que un
 *    cliente cualquiera deba poder listar.
 *  - Cualquier usuario logueado (ADMIN, AGENTE o CLIENTE): todo lo
 *    demás (reservas, pagos, asientos...), porque un cliente necesita
 *    poder reservar y pagar su propio viaje.
 *  - Borrar (DELETE) cualquier cosa: solo ADMIN o AGENTE.
 *
 * Nota: esto no filtra "solo tus propias reservas" (no hay ese control
 * fino a nivel de fila) -- es una limitación conocida del alcance
 * actual del proyecto, no algo que se nos haya olvidado configurar.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    private static final String[] ORIGENES_PERMITIDOS = {
            "http://localhost:8082", "http://127.0.0.1:8082",
            "http://localhost:5500", "http://127.0.0.1:5500",
            "http://localhost:5501", "http://127.0.0.1:5501"
    };

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(ORIGENES_PERMITIDOS));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> {})
            .httpBasic(httpBasic -> httpBasic.disable())
            .formLogin(form -> form.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Recursos estáticos del frontend
                .requestMatchers("/", "/*.html", "/css/**", "/js/**", "/img/**", "/favicon.ico").permitAll()
                // Autenticación y registro público
                .requestMatchers("/api/login/**", "/api/registro/**").permitAll()
                // El chatbot lo usa cualquier visitante, con o sin sesión
                .requestMatchers("/api/chatbot/**").permitAll()
                // Búsqueda pública (cotizar antes de iniciar sesión)
                .requestMatchers(HttpMethod.GET, "/api/viajes/**", "/api/rutas/**",
                        "/api/terminales/**", "/api/vehiculos/**", "/api/roles/**").permitAll()
                // Listado completo y creación de usuarios: solo personal interno
                // (un cliente no debe poder ver ni crear la lista de clientes).
                .requestMatchers(HttpMethod.GET, "/api/usuarios").hasAnyRole("ADMIN", "AGENTE")
                .requestMatchers(HttpMethod.POST, "/api/usuarios").hasAnyRole("ADMIN", "AGENTE")
                // Ver/editar un usuario puntual: cualquier usuario logueado (así
                // un cliente puede ver y editar su propio perfil en p16-perfil.html).
                // Nota: no hay verificación de que el id sea el suyo -- es una
                // limitación conocida, no algo que se nos olvidó configurar.
                .requestMatchers("/api/usuarios/**").authenticated()
                // Reportes, conductores y personas: solo personal interno
                .requestMatchers("/api/reportes/**", "/api/conductores/**", "/api/personas/**").hasAnyRole("ADMIN", "AGENTE")
                // Crear/editar catálogo (rutas, viajes, terminales, vehículos): solo personal
                .requestMatchers(HttpMethod.POST, "/api/viajes/**", "/api/rutas/**",
                        "/api/terminales/**", "/api/vehiculos/**").hasAnyRole("ADMIN", "AGENTE")
                .requestMatchers(HttpMethod.PUT, "/api/viajes/**", "/api/rutas/**",
                        "/api/terminales/**", "/api/vehiculos/**").hasAnyRole("ADMIN", "AGENTE")
                // Borrar cualquier cosa: solo personal
                .requestMatchers(HttpMethod.DELETE, "/api/**").hasAnyRole("ADMIN", "AGENTE")
                // Todo lo demás (reservas, pagos, asientos, detalle-reservas):
                // cualquier usuario que haya iniciado sesión
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
