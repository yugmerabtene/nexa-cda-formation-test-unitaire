package com.nexa.secu.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests du JwtUtil")
class JwtUtilTest {

    private final JwtUtil jwtUtil = new JwtUtil();

    @Nested
    @DisplayName("Génération et validation de token")
    class GenerationValidation {

        @Test
        @DisplayName("Un token généré est valide")
        void tokenGenereEstValide() {
            String token = jwtUtil.genererToken("alice", "ADMIN");
            assertNotNull(token);
            assertFalse(token.isEmpty());
            assertTrue(jwtUtil.estTokenValide(token));
        }

        @Test
        @DisplayName("Extraire le username d'un token valide")
        void extraireUsername() {
            String token = jwtUtil.genererToken("bob", "USER");
            assertEquals("bob", jwtUtil.extraireUsername(token));
        }

        @Test
        @DisplayName("Extraire le rôle d'un token valide")
        void extraireRole() {
            String token = jwtUtil.genererToken("charlie", "ADMIN");
            assertEquals("ADMIN", jwtUtil.extraireRole(token));
        }
    }

    @Nested
    @DisplayName("Tokens invalides")
    class TokensInvalides {

        @Test
        @DisplayName("Token vide ou null est invalide")
        void tokenVideNullInvalide() {
            assertFalse(jwtUtil.estTokenValide(null));
            assertFalse(jwtUtil.estTokenValide(""));
        }

        @Test
        @DisplayName("Token modifié (tampered) est invalide")
        void tokenModifieInvalide() {
            String token = jwtUtil.genererToken("dave", "USER");
            String tokenModifie = token.substring(0, token.length() - 3) + "xxx";
            assertFalse(jwtUtil.estTokenValide(tokenModifie),
                "Un token dont la signature a été modifiée doit être invalide");
        }

        @Test
        @DisplayName("Token aléatoire est invalide")
        void tokenAleatoireInvalide() {
            assertFalse(jwtUtil.estTokenValide("eyJhbGciOiJIUzI1NiJ9.abc.def"));
        }

        @Test
        @DisplayName("Tokens différents pour le même utilisateur")
        void tokensDifferentsPourMemeUtilisateur() {
            String t1 = jwtUtil.genererToken("eve", "USER");
            String t2 = jwtUtil.genererToken("eve", "USER");
            assertNotEquals(t1, t2,
                "Chaque appel génère un token différent (timestamp iat différent)");
        }
    }
}
