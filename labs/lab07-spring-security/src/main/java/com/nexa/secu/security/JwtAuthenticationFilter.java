package com.nexa.secu.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtre de sécurité qui intercepte chaque requête HTTP pour extraire et
 * valider un token JWT depuis le header {@code Authorization}.
 * <p>
 * Ce filtre herite de {@link OncePerRequestFilter}, garantissant une exécution
 * unique par requête. Il est insere dans la chaine de filtres Spring Security
 * avant le {@code UsernamePasswordAuthenticationFilter} (voir {@code SecurityConfig}).
 * <p>
 * Fonctionnement :
 * <ol>
 *   <li>Extraire le header {@code Authorization}</li>
 *   <li>Verifier le prefixe {@code Bearer }</li>
 *   <li>Extraire et valider le token via {@link JwtUtil}</li>
 *   <li>Si le token est validé, creer un objet d'authentification et le placer
 *       dans le {@code SecurityContextHolder}</li>
 *   <li>Poursuivre la chaine de filtres quel que soit le résultat</li>
 * </ol>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Utilitaire de gestion des tokens JWT (generation, extraction, validation). */
    private final JwtUtil jwtUtil;

    /**
     * Constructeur avec injection de l'utilitaire JWT.
     *
     * @param jwtUtil l'utilitaire JWT injecte par Spring
     */
    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * Methode principale du filtre, executee pour chaque requête HTTP.
     * <p>
     * Si un token Bearer validé est present dans le header {@code Authorization},
     * l'utilisateur est authentifie dans le contexte de sécurité avec le role
     * extrait du token. Sinon, la requête poursuit sans authentification.
     *
     * @param request     la requête HTTP entrante
     * @param response    la réponse HTTP
     * @param filterChain la chaine de filtres a poursuivre
     * @throws ServletException en cas d'erreur de filtrage
     * @throws IOException      en cas d'erreur d'entree/sortie
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Recuperation du header Authorization
        String authHeader = request.getHeader("Authorization");

        // Verification de la presence du prefixe "Bearer "
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // Extraction du token (7 caracteres = "Bearer ")
            String token = authHeader.substring(7);

            // Validation du token : signature correcte et non expire
            if (jwtUtil.estTokenValide(token)) {
                // Extraction du username et du role depuis le token
                String username = jwtUtil.extraireUsername(token);
                String role = jwtUtil.extraireRole(token);

                // Construction du token d'authentification avec l'autorite ROLE_<role>
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );

                // Placement de l'authentification dans le contexte de sécurité
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        // Poursuite de la chaine de filtres (le filtre ne bloque jamais)
        filterChain.doFilter(request, response);
    }
}
