package com.nexa.secu.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Utilitaire de gestion des JSON Web Tokens (JWT) pour l'authentification.
 * <p>
 * Cette classe est responsable de :
 * <ul>
 *   <li>Generer des tokens JWT signes avec HMAC-SHA256</li>
 *   <li>Extraire le username (sujet) et le role d'un token</li>
 *   <li>Valider l'integrite et l'expiration d'un token</li>
 * </ul>
 * La cle secrete est derivee d'une chaine de caracteres d'au moins 256 bits
 * pour respecter les exigences de l'algorithme HS256.
 * <p>
 * Les tokens generes ont une durée de validite de 1 heure (3600000 ms).
 */
@Component
public class JwtUtil {

    /** Cle secrete utilisé́e pour signer et verifier les tokens JWT.
     * Doit contenir au moins 256 bits (32 caracteres) pour HS256. */
    private static final String SECRET = "cette-cle-est-un-secret-très-long-dau-moins-256-bits-pour-hs256";

    /** Duree de validite des tokens en millisecondes (1 heure = 3 600 000 ms). */
    private static final long EXPIRATION_MS = 3600000;

    /** Cle de signature HMAC générée a partir de la chaine secrete. */
    private final SecretKey key;

    /**
     * Constructeur qui initialise la cle de signature HMAC-SHA256
     * a partir de la constante {@code SECRET}.
     */
    public JwtUtil() {
        this.key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Genere un token JWT pour un utilisateur donne.
     * <p>
     * Le token contient :
     * <ul>
     *   <li>Le username comme sujet ({@code sub})</li>
     *   <li>Le role comme claim personnalise ({@code role})</li>
     *   <li>La date d'emission ({@code iat})</li>
     *   <li>La date d'expiration ({@code exp}), calculee a partir d'EXPIRATION_MS</li>
     * </ul>
     *
     * @param username le nom d'utilisateur a inclure dans le token
     * @param role     le role a inclure comme claim personnalise
     * @return le token JWT compact sous forme de chaine
     */
    public String genererToken(String username, String role) {
        return Jwts.builder()
            .subject(username)
            .claim("role", role)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
            .signWith(key)
            .compact();
    }

    /**
     * Extrait le nom d'utilisateur (sujet) d'un token JWT.
     *
     * @param token le token JWT a analyser
     * @return le sujet (username) contenu dans le token
     */
    public String extraireUsername(String token) {
        return parseToken(token).getPayload().getSubject();
    }

    /**
     * Extrait le role d'un token JWT depuis le claim personnalise {@code role}.
     *
     * @param token le token JWT a analyser
     * @return le role contenu dans le claim {@code role}
     */
    public String extraireRole(String token) {
        return parseToken(token).getPayload().get("role", String.class);
    }

    /**
     * Verifie si un token JWT est validé (signature correcte et non expire).
     * <p>
     * Toute exception lors de l'analyse (signature invalide, token expire,
     * format incorrect) entraine le retour de {@code false}.
     *
     * @param token le token JWT a valider
     * @return {@code true} si le token est validé, {@code false} sinon
     */
    public boolean estTokenValide(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Analyse et vérifié un token JWT, puis retourne ses claims (payload).
     * <p>
     * Methode privee utilisé́e en interne par {@link #extraireUsername},
     * {@link #extraireRole} et {@link #estTokenValide}.
     *
     * @param token le token JWT a analyser
     * @return les claims (payload) du token
     * @throws JwtException si le token est invalide ou expire
     */
    private Claims parseToken(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
