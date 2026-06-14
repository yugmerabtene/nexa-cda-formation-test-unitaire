package com.nexa.secu.config;

import com.nexa.secu.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * <h1>Configuration Spring Security</h1>
 *
 * <h2>{@code @Configuration}</h2>
 * <p>Indique que cette classe contient des méthodes {@code @Bean}
 * qui seront gérées par le conteneur Spring.</p>
 *
 * <h2>{@code @EnableWebSecurity}</h2>
 * <p>Active la sécurité web de Spring Security.</p>
 *
 * <h2>{@code @EnableMethodSecurity}</h2>
 * <p>Active les annotations de sécurité au niveau méthode :
 * {@code @PreAuthorize}, {@code @PostAuthorize}, {@code @Secured}, etc.</p>
 *
 * <h2>{@code SecurityFilterChain}</h2>
 * <p>Bean qui définit la chaîne de filtres de sécurité.
 * Configure : quelles routes sont protégées, la gestion de session,
 * les filtres personnalisés (JWT), CSRF, CORS.</p>
 *
 * <h2>{@code SessionCreationPolicy.STATELESS}</h2>
 * <p>Pas de session HTTP côté serveur. L'authentification se fait
 * via le token JWT à chaque requête.</p>
 *
 * <h2>CSRF désactivé</h2>
 * <p>Le CSRF (Cross-Site Request Forgery) est désactivé car
 * l'API est stateless avec JWT. Chaque requête porte son token,
 * donc une attaque CSRF n'est pas possible.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    /**
     * Définit la chaîne de sécurité.
     *
     * <h3>Méthode : authorizeHttpRequests</h3>
     * <p>
     * {@code requestMatchers(...).permitAll()} : routes publiques, sans authentification.
     * {@code requestMatchers(...).hasRole("ADMIN")} : réservé au rôle ADMIN.
     * {@code anyRequest().authenticated()} : tout le reste nécessite une authentification.
     * </p>
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/produits/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/produits/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/produits/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/produits/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * {@code PasswordEncoder} utilisant BCrypt.
     * <p>BCrypt est lent volontairement (coût ajustable) pour résister
     * aux attaques par force brute. Chaque hash inclut un sel aléatoire.</p>
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
