package com.nexa.secu.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * <h1>JwtUtil — Génération et validation de tokens JWT</h1>
 *
 * <h2>JWT (JSON Web Token)</h2>
 * <p>
 * Structure : {@code Header.Payload.Signature}
 * </p>
 * <ul>
 *   <li><b>Header</b> : algorithme (HS256) + type (JWT)</li>
 *   <li><b>Payload (claims)</b> : subject (username), rôle, expiration</li>
 *   <li><b>Signature</b> : HMAC-SHA256(header + "." + payload, secretKey)</li>
 * </ul>
 *
 * <h2>Pourquoi JWT ?</h2>
 * <p>Stateless : le serveur n'a pas besoin de stocker la session.
 * Le token contient toutes les informations nécessaires, signées
 * cryptographiquement pour garantir l'intégrité.</p>
 *
 * <h2>⚠️ Sécurité</h2>
 * <ul>
 *   <li>Le secret NE DOIT JAMAIS être en dur dans le code</li>
 *   <li>Toujours utiliser une clé d'au moins 256 bits (32 caractères)</li>
 *   <li>Définir une expiration courte (15-60 minutes)</li>
 *   <li>Ne jamais stocker de données sensibles dans le payload (il est encodé, pas chiffré)</li>
 * </ul>
 */
@Component
public class JwtUtil {

    private static final String SECRET = "cette-cle-est-un-secret-tres-long-dau-moins-256-bits-pour-hs256";
    private static final long EXPIRATION_MS = 3600000; // 1 heure

    private final SecretKey key;

    public JwtUtil() {
        this.key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Génère un token JWT.
     *
     * @param username le sujet (sub)
     * @param role     le rôle (claim personnalisé)
     * @return le token signé
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
     * Extrait le username (subject) du token.
     */
    public String extraireUsername(String token) {
        return parseToken(token).getPayload().getSubject();
    }

    /**
     * Extrait le rôle du token.
     */
    public String extraireRole(String token) {
        return parseToken(token).getPayload().get("role", String.class);
    }

    /**
     * Valide le token (signature + expiration).
     */
    public boolean estTokenValide(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
