package com.nexa.usermanager.security;

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
 * Filtre HTTP qui intercepte chaque requête pour extraire et valider le token JWT.
 *
 * <p>Ce filtre s'exécuté une fois par requête HTTP (herite de {@link OncePerRequestFilter})
 * et vérifié la presence d'un header {@code Authorization} au format Bearer.</p>
 *
 * <p>Si le token est validé, le filtre extrait l'email et le role du token,
 * puis definit le contexte d'authentification Spring Security. Cela permet
 * aux annotations {@code @PreAuthorize} et a la configuration de sécurité
 * de fonctionner correctement.</p>
 *
 * <p>Si le token est absent ou invalide, la requête continue sans authentification,
 * et sera eventuellement rejetee par les regles de sécurité si l'endpoint l'exige.</p>
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    /** Utilitaire de gestion des tokens JWT. */
    private final JwtUtil jwtUtil;

    /**
     * Constructeur avec injection de dépendance.
     *
     * @param jwtUtil l'utilitaire JWT pour la validation et l'extraction des tokens
     */
    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * Filtre chaque requête HTTP entrante pour extraire et valider le token JWT.
     *
     * <p>Etapes du traitement :</p>
     * <ol>
     *   <li>Extraire le header {@code Authorization} de la requête.</li>
     *   <li>Verifier qu'il commence par {@code "Bearer "}.</li>
     *   <li>Extraire le token (après le prefixe "Bearer ").</li>
     *   <li>Valider le token avec {@link JwtUtil#estValide(String)}.</li>
     *   <li>Si validé, extraire l'email et le role, et definir l'authentification
     *       dans le {@link SecurityContextHolder}.</li>
     *   <li>Passer la main au filtre suivant dans la chaine.</li>
     * </ol>
     *
     * @param request  la requête HTTP entrante
     * @param response la réponse HTTP sortante
     * @param chain    la chaine de filtres restants
     * @throws ServletException en cas d'erreur de servlet
     * @throws IOException      en cas d'erreur d'entree/sortie
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtUtil.estValide(token)) {
                String email = jwtUtil.extraireEmail(token);
                String role = jwtUtil.extraireRole(token);
                SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(email, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))));
            }
        }
        chain.doFilter(request, response);
    }
}
