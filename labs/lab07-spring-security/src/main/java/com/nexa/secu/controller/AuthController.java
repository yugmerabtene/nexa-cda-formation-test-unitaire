package com.nexa.secu.controller;

import com.nexa.secu.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controleur REST gerant l'authentification des utilisateurs.
 * <p>
 * Expose l'endpoint {@code POST /api/auth/login} qui valide les identifiants,
 * genere un token JWT et le retourne au client. En cas d'echec d'authentification,
 * un statut HTTP 401 est renvoye.
 * <p>
 * Ce controleur est explicitement exclu de la securite dans {@code SecurityConfig}
 * via {@code .requestMatchers("/api/auth/**").permitAll()}.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /** Gestionnaire d'authentification Spring Security pour valider les identifiants. */
    private final AuthenticationManager authManager;

    /** Utilitaire de generation et validation des tokens JWT. */
    private final JwtUtil jwtUtil;

    /** Service de chargement des informations utilisateur depuis la base de donnees. */
    private final UserDetailsService userDetailsService;

    /** Encodeur de mots de passe (non utilise directement ici mais disponible). */
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructeur avec injection de dependances.
     *
     * @param authManager        le gestionnaire d'authentification
     * @param jwtUtil            l'utilitaire JWT
     * @param userDetailsService le service de chargement des utilisateurs
     * @param passwordEncoder    l'encodeur de mots de passe BCrypt
     */
    public AuthController(AuthenticationManager authManager, JwtUtil jwtUtil,
                          UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        this.authManager = authManager;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Authentifie un utilisateur a partir de son nom d'utilisateur et mot de passe.
     * <p>
     * En cas de succes, un token JWT est genere avec le username comme sujet
     * et le role de l'utilisateur comme claim personnalise. Le token est retourne
     * dans le corps de la reponse au format JSON.
     * <p>
     * En cas d'echec (identifiants invalides), un statut HTTP 401 est renvoye
     * avec un message d'erreur.
     *
     * @param request une map contenant les cles "username" et "password"
     * @return une reponse HTTP contenant le token JWT ou un message d'erreur
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");

            // Authentification via le gestionnaire Spring Security
            authManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));

            // Recuperation du role depuis les authorities de l'utilisateur
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            String role = userDetails.getAuthorities().stream()
                .findFirst().orElse(new SimpleGrantedAuthority("ROLE_USER"))
                .getAuthority().replace("ROLE_", "");

            // Generation du token JWT et retour au client
            String token = jwtUtil.genererToken(username, role);
            return ResponseEntity.ok(Map.of("token", token));
        } catch (AuthenticationException e) {
            // Echec de l'authentification : retour d'une erreur 401
            return ResponseEntity.status(401).body(Map.of("error", "Identifiants invalides"));
        }
    }
}
