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
 * <h1>JwtAuthenticationFilter — Filtre JWT personnalisé</h1>
 *
 * <h2>{@code OncePerRequestFilter}</h2>
 * <p>
 * Classe de base Spring qui garantit que le filtre est exécuté
 * <b>une seule fois par requête</b> (même en cas de forward/include).
 * </p>
 *
 * <h2>Fonctionnement</h2>
 * <ol>
 *   <li>Extraire le header {@code Authorization: Bearer <token>}</li>
 *   <li>Valider le token (signature + expiration)</li>
 *   <li>Extraire username + rôle</li>
 *   <li>Créer un {@code Authentication} et le placer dans le {@code SecurityContext}</li>
 *   <li>Continuer la chaîne de filtres</li>
 * </ol>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtUtil.estTokenValide(token)) {
                String username = jwtUtil.extraireUsername(token);
                String role = jwtUtil.extraireRole(token);

                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
