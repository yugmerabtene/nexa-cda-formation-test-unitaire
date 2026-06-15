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
 * Configuration centrale de Spring Security pour l'application.
 * <p>
 * Cette classe definit la chaine de filtres de securite, les regles
 * de controle d'acces par URL et par methode HTTP, la politique de session
 * (sans etat pour une API REST), et active la securite au niveau des methodes
 * via les annotations {@code @PreAuthorize}.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Filtre JWT injecte par Spring et place dans la chaine de securite
     * avant le filtre d'authentification standard.
     */
    private final JwtAuthenticationFilter jwtFilter;

    /**
     * Constructeur avec injection du filtre JWT.
     *
     * @param jwtFilter le filtre personnalise d'extraction et validation des tokens JWT
     */
    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    /**
     * Construit et configure la chaine de filtres de securite.
     * <ul>
     *   <li>Desactive CSRF (non necessaire pour une API REST sans etat)</li>
     *   <li>Definit une politique de session {@code STATELESS} : aucun cookie
     *       de session HTTP n'est cree</li>
     *   <li>Declare les regles d'autorisation par chemin et methode HTTP</li>
     *   <li>Insere le filtre JWT avant le {@link UsernamePasswordAuthenticationFilter}</li>
     * </ul>
     *
     * @param http l'objet de configuration HTTP de Spring Security
     * @return la chaine de filtres de securite construite
     * @throws Exception si la construction de la chaine echoue
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
     * Definit le bean {@link PasswordEncoder} utilisant l'algorithme BCrypt
     * pour le chiffrement des mots de passe.
     *
     * @return un encodeur BCryptPasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
