package com.nexa.secu.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour la classe {@link JwtUtil}.
 * <p>
 * Cette classe verifie le comportement de l'utilitaire JWT :
 * <ul>
 *   <li>Generation de tokens valides</li>
 *   <li>Extraction correcte du username et du role</li>
 *   <li>Detection des tokens invalides (null, vides, modifies, aleatoires)</li>
 *   <li>Unicite des tokens generes (timestamp d'emission different)</li>
 * </ul>
 * Aucun contexte Spring n'est necessaire : le JwtUtil est instancie directement.
 */
@DisplayName("Tests du JwtUtil")
class JwtUtilTest {

    /** Instance du JwtUtil testee, creee manuellement sans Spring. */
    private final JwtUtil jwtUtil = new JwtUtil();

    /**
     * Groupe de tests portant sur la generation et la validation de tokens valides.
     */
    @Nested
    @DisplayName("Génération et validation de token")
    class GenerationValidation {

        /**
         * Verifie qu'un token genere est non null, non vide et considere valide
         * par la methode {@code estTokenValide()}.
         */
        @Test
        @DisplayName("Un token généré est valide")
        void tokenGenereEstValide() {
            String token = jwtUtil.genererToken("alice", "ADMIN");
            assertNotNull(token);
            assertFalse(token.isEmpty());
            assertTrue(jwtUtil.estTokenValide(token));
        }

        /**
         * Verifie que le username extrait d'un token correspond bien
         * a celui passe lors de la generation.
         */
        @Test
        @DisplayName("Extraire le username d'un token valide")
        void extraireUsername() {
            String token = jwtUtil.genererToken("bob", "USER");
            assertEquals("bob", jwtUtil.extraireUsername(token));
        }

        /**
         * Verifie que le role extrait d'un token correspond bien
         * a celui passe lors de la generation.
         */
        @Test
        @DisplayName("Extraire le rôle d'un token valide")
        void extraireRole() {
            String token = jwtUtil.genererToken("charlie", "ADMIN");
            assertEquals("ADMIN", jwtUtil.extraireRole(token));
        }
    }

    /**
     * Groupe de tests portant sur la detection de tokens invalides.
     */
    @Nested
    @DisplayName("Tokens invalides")
    class TokensInvalides {

        /**
         * Verifie qu'un token null ou une chaine vide est considere invalide.
         */
        @Test
        @DisplayName("Token vide ou null est invalide")
        void tokenVideNullInvalide() {
            assertFalse(jwtUtil.estTokenValide(null));
            assertFalse(jwtUtil.estTokenValide(""));
        }

        /**
         * Verifie qu'un token dont la signature a ete modifiee (tampered)
         * est detecte comme invalide.
         */
        @Test
        @DisplayName("Token modifié (tampered) est invalide")
        void tokenModifieInvalide() {
            String token = jwtUtil.genererToken("dave", "USER");
            // Alteration de la fin du token (partie signature)
            String tokenModifie = token.substring(0, token.length() - 3) + "xxx";
            assertFalse(jwtUtil.estTokenValide(tokenModifie),
                "Un token dont la signature a été modifiée doit être invalide");
        }

        /**
         * Verifie qu'un token aleatoire au format JWT mais non signe
         * avec la bonne cle est invalide.
         */
        @Test
        @DisplayName("Token aléatoire est invalide")
        void tokenAleatoireInvalide() {
            assertFalse(jwtUtil.estTokenValide("eyJhbGciOiJIUzI1NiJ9.abc.def"));
        }

        /**
         * Verifie que deux appels consecutifs a {@code genererToken} pour le meme
         * utilisateur produisent des tokens differents (a cause du timestamp iat).
         */
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
