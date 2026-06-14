package com.nexa.usermanager.security;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests unitaires : JwtUtil")
class JwtUtilTest {

    private final JwtUtil jwtUtil = new JwtUtil();

    @Test
    @DisplayName("Génère un token valide")
    void genererTokenValide() {
        String token = jwtUtil.genererToken("user@nexa.fr", "USER");
        assertNotNull(token);
        assertTrue(jwtUtil.estValide(token));
    }

    @Test
    @DisplayName("Extrait l'email")
    void extraireEmail() {
        String token = jwtUtil.genererToken("admin@nexa.fr", "ADMIN");
        assertEquals("admin@nexa.fr", jwtUtil.extraireEmail(token));
    }

    @Test
    @DisplayName("Extrait le rôle")
    void extraireRole() {
        String token = jwtUtil.genererToken("user@nexa.fr", "USER");
        assertEquals("USER", jwtUtil.extraireRole(token));
    }

    @Test
    @DisplayName("Token null est invalide")
    void tokenNull() {
        assertFalse(jwtUtil.estValide(null));
    }

    @Test
    @DisplayName("Token vide est invalide")
    void tokenVide() {
        assertFalse(jwtUtil.estValide(""));
    }

    @Test
    @DisplayName("Token modifié est invalide")
    void tokenModifie() {
        String token = jwtUtil.genererToken("user@nexa.fr", "USER");
        assertFalse(jwtUtil.estValide(token + "x"));
    }
}
