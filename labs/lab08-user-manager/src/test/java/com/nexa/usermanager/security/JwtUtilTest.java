package com.nexa.usermanager.security;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour {@link JwtUtil}.
 *
 * <p>Cette classe teste les fonctionnalites de generation, extraction
 * et validation des tokens JWT :</p>
 * <ul>
 *   <li>Generation d'un token valide.</li>
 *   <li>Extraction de l'email (sujet) du token.</li>
 *   <li>Extraction du role (claim) du token.</li>
 *   <li>Rejet des tokens null, vides ou modifies.</li>
 * </ul>
 */
@DisplayName("Tests unitaires : JwtUtil")
class JwtUtilTest {

    /** Instance de JwtUtil creee directement (sans Spring). */
    private final JwtUtil jwtUtil = new JwtUtil();

    /**
     * Verifie que le token genere est non null et valide.
     */
    @Test
    @DisplayName("Genere un token valide")
    void genererTokenValide() {
        String token = jwtUtil.genererToken("user@nexa.fr", "USER");
        assertNotNull(token);
        assertTrue(jwtUtil.estValide(token));
    }

    /**
     * Verifie que l'email extrait correspond a celui fourni lors de la generation.
     */
    @Test
    @DisplayName("Extrait l'email")
    void extraireEmail() {
        String token = jwtUtil.genererToken("admin@nexa.fr", "ADMIN");
        assertEquals("admin@nexa.fr", jwtUtil.extraireEmail(token));
    }

    /**
     * Verifie que le role extrait correspond a celui fourni lors de la generation.
     */
    @Test
    @DisplayName("Extrait le role")
    void extraireRole() {
        String token = jwtUtil.genererToken("user@nexa.fr", "USER");
        assertEquals("USER", jwtUtil.extraireRole(token));
    }

    /**
     * Verifie qu'un token null est considere comme invalide.
     */
    @Test
    @DisplayName("Token null est invalide")
    void tokenNull() {
        assertFalse(jwtUtil.estValide(null));
    }

    /**
     * Verifie qu'un token vide est considere comme invalide.
     */
    @Test
    @DisplayName("Token vide est invalide")
    void tokenVide() {
        assertFalse(jwtUtil.estValide(""));
    }

    /**
     * Verifie qu'un token modifie (altere) est considere comme invalide.
     * La modification d'un seul caractere doit invalider la signature.
     */
    @Test
    @DisplayName("Token modifie est invalide")
    void tokenModifie() {
        String token = jwtUtil.genererToken("user@nexa.fr", "USER");
        assertFalse(jwtUtil.estValide(token + "x"));
    }
}
