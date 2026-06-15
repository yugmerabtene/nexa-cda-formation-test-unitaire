package com.nexa.usermanager.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Utilitaire de gestion des tokens JWT (JSON Web Token).
 *
 * <p>Cette classe fournit les méthodes pour :</p>
 * <ul>
 *   <li>Generer un token JWT signe avec HMAC-SHA256</li>
 *   <li>Extraire l'email (subject) d'un token</li>
 *   <li>Extraire le role (claim) d'un token</li>
 *   <li>Valider l'integrite et l'expiration d'un token</li>
 * </ul>
 *
 * <p>Les tokens contiennent l'email en tant que sujet ({@code sub}) et le role
 * en tant que claim personnalise ({@code role}). La durée de validite est de
 * 1 heure (3600000 ms).</p>
 *
 * <p>La cle secrete doit faire au moins 256 bits pour etre compatible avec
 * l'algorithme HMAC-SHA256 (HS256).</p>
 */
@Component
public class JwtUtil {

    /**
     * Cle secrete utilisé́e pour signer et verifier les tokens JWT.
     * Doit faire au moins 256 bits (32 caracteres) pour HS256.
     */
    private static final String SECRET = "cette-cle-est-un-secret-très-long-dau-moins-256-bits-pour-hs256-user-manager";

    /** Duree de validite des tokens en millisecondes (1 heure = 3600000 ms). */
    private static final long EXPIRATION = 3600000;

    /** Cle cryptographique derivee de la cle secrete. */
    private final SecretKey key;

    /**
     * Constructeur qui initialise la cle cryptographique HMAC a partir
     * de la cle secrete.
     */
    public JwtUtil() {
        this.key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Genere un token JWT pour un utilisateur donne.
     *
     * <p>Le token contient l'email comme sujet et le role comme claim personnalise.
     * Il est signe avec la cle HMAC et expire après 1 heure.</p>
     *
     * @param email l'adresse email de l'utilisateur (utilisé́e comme sujet)
     * @param role  le role de l'utilisateur (USER ou ADMIN)
     * @return le token JWT signe sous forme de chaine compacte
     */
    public String genererToken(String email, String role) {
        return Jwts.builder()
            .subject(email)
            .claim("role", role)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
            .signWith(key)
            .compact();
    }

    /**
     * Extrait l'email (sujet) contenu dans un token JWT.
     *
     * @param token le token JWT a analyser
     * @return l'email contenu dans le sujet du token
     * @throws JwtException si le token est invalide ou expire
     */
    public String extraireEmail(String token) {
        return parse(token).getPayload().getSubject();
    }

    /**
     * Extrait le role (claim personnalise) contenu dans un token JWT.
     *
     * @param token le token JWT a analyser
     * @return le role contenu dans le claim {@code role} du token
     * @throws JwtException si le token est invalide ou expire
     */
    public String extraireRole(String token) {
        return parse(token).getPayload().get("role", String.class);
    }

    /**
     * Verifie si un token JWT est validé (signature correcte et non expire).
     *
     * @param token le token JWT a valider
     * @return {@code true} si le token est validé, {@code false} sinon
     */
    public boolean estValide(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Parse et vérifié un token JWT, puis retourne les claims (payload).
     *
     * <p>Cette méthode vérifié la signature et l'expiration du token.
     * Toute anomalie (signature invalide, token expire, format incorrect)
     * lève une {@link JwtException} ou {@link IllegalArgumentException}.</p>
     *
     * @param token le token JWT a parser et verifier
     * @return les claims extraits du token
     * @throws JwtException             si le token est invalide, mal forme ou expire
     * @throws IllegalArgumentException si le token est null ou vide
     */
    private Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token).getPayload();
    }
}
