package com.nexa.usermanager.config;

import com.nexa.usermanager.security.JwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuration de la securite Spring Security pour l'API REST.
 *
 * <p>Cette classe definit la chaine de filtres de securite, les regles
 * d'autorisation HTTP, l'encodeur de mots de passe et le gestionnaire
 * d'authentification.</p>
 *
 * <p>Politique de securite :</p>
 * <ul>
 *   <li>CSRF desactive (API REST stateless).</li>
 *   <li>Sessions desactivees ({@code STATELESS}).</li>
 *   <li>Endpoints d'authentification et health check publics.</li>
 *   <li>Lecture ({@code GET}) accessible aux roles ADMIN et USER.</li>
 *   <li>Ecriture ({@code POST}, {@code PUT}, {@code DELETE}) reservee au role ADMIN.</li>
 *   <li>Le filtre JWT est execute avant le filtre d'authentification standard.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** Filtre JWT pour l'authentification par token. */
    private final JwtFilter jwtFilter;

    /**
     * Constructeur avec injection du filtre JWT.
     *
     * @param jwtFilter le filtre d'authentification JWT
     */
    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    /**
     * Configure la chaine de filtres de securite HTTP.
     *
     * <p>Cette methode definit :</p>
     * <ol>
     *   <li>La desactivation de la protection CSRF.</li>
     *   <li>La gestion des sessions en mode stateless.</li>
     *   <li>Les regles d'autorisation par URL et methode HTTP.</li>
     *   <li>L'insertion du filtre JWT dans la chaine.</li>
     * </ol>
     *
     * @param http le constructeur de configuration de securite HTTP
     * @return la chaine de filtres de securite construite
     * @throws Exception si la configuration echoue
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/actuator/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/users/**").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.POST, "/api/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Fournit l'encodeur de mots de passe BCrypt.
     *
     * <p>BCrypt est un algorithme de hachage a sens unique concu specifiquement
     * pour les mots de passe. Il est resistant aux attaques par force brute
     * grace a son cout de calcul ajustable.</p>
     *
     * @return un encodeur {@link BCryptPasswordEncoder}
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Fournit le gestionnaire d'authentification Spring Security.
     *
     * <p>Ce bean est necessaire pour le processus d'authentification
     * dans le controleur (endpoint {@code /api/auth/login}).</p>
     *
     * @param config la configuration d'authentification de Spring Security
     * @return le gestionnaire d'authentification
     * @throws Exception si la creation du gestionnaire echoue
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
