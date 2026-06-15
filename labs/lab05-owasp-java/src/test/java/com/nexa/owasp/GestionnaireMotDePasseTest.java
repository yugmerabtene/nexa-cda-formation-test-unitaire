package com.nexa.owasp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour la classe {@link GestionnaireMotDePasse}.
 *
 * <p>Valide les deux approches de hachage :</p>
 * <ul>
 *   <li>Hachage vulnerable (sans sel) : deterministe, vulnerable aux rainbow tables.</li>
 *   <li>Hachage securise (avec sel) : aleatoire, chaque hash est unique.</li>
 * </ul>
 *
 * <p>Verifie egalement le mecanisme de verification de mot de passe et
 * la structure du hash stocke (format {@code sel:hash}).</p>
 */
@DisplayName("OWASP : Gestion des mots de passe")
class GestionnaireMotDePasseTest {

    /**
     * Instance du gestionnaire de mots de passe utilisee dans tous les tests.
     */
    private final GestionnaireMotDePasse gestionnaire = new GestionnaireMotDePasse();

    /**
     * Tests de hachage et de verification des mots de passe.
     */
    @Nested
    @DisplayName("Hachage et verification")
    class HachageVerification {

        /**
         * Verifie que le hachage vulnerable est deterministe : deux appels
         * avec le meme mot de passe produisent le meme hash. Cette propriete
         * le rend vulnerable aux attaques par rainbow tables.
         */
        @Test
        @DisplayName("Hachage vulnerable : deterministe, meme entree → meme sortie")
        void hachageVulnerableDeterministe() {
            String h1 = gestionnaire.hacherVulnerable("password123");
            String h2 = gestionnaire.hacherVulnerable("password123");
            assertEquals(h1, h2,
                "Le hachage sans sel produit toujours le meme resultat → vulnerable aux rainbow tables");
        }

        /**
         * Verifie que le hachage securise est aleatoire : deux appels avec
         * le meme mot de passe produisent des hashs differents grace au sel.
         */
        @Test
        @DisplayName("Hachage securise : aleatoire, meme entree → sorties differentes")
        void hachageSecuriseAleatoire() {
            String h1 = gestionnaire.hacherAvecSel("password123");
            String h2 = gestionnaire.hacherAvecSel("password123");
            assertNotEquals(h1, h2,
                "Avec un sel aleatoire, le meme mot de passe produit des hashs differents");
        }

        /**
         * Verifie qu'un mot de passe correct est valide par rapport a son hash.
         */
        @Test
        @DisplayName("Verification : mot de passe correct valide")
        void verificationMotDePasseCorrect() {
            String hash = gestionnaire.hacherAvecSel("monSuperMotDePasse");
            assertTrue(gestionnaire.verifierMotDePasse("monSuperMotDePasse", hash));
        }

        /**
         * Verifie qu'un mot de passe incorrect est rejete.
         */
        @Test
        @DisplayName("Verification : mot de passe incorrect rejete")
        void verificationMotDePasseIncorrect() {
            String hash = gestionnaire.hacherAvecSel("motDePasse");
            assertFalse(gestionnaire.verifierMotDePasse("mauvaisMotDePasse", hash));
        }

        /**
         * Verifie que les hashs au format invalide ou null sont rejetes
         * lors de la verification.
         */
        @Test
        @DisplayName("Verification : hash invalide rejete")
        void verificationHashInvalide() {
            assertFalse(gestionnaire.verifierMotDePasse("test", "hash_invalide"));
            assertFalse(gestionnaire.verifierMotDePasse("test", null));
        }

        /**
         * Verifie que le hash securise respecte le format {@code sel:hash}
         * et que les deux parties sont non vides.
         */
        @Test
        @DisplayName("Hash securise contient le sel (format sel:hash)")
        void hashSecuriseContientSel() {
            String hash = gestionnaire.hacherAvecSel("test");
            assertTrue(hash.contains(":"),
                "Le hash doit contenir le sel et le hash separes par ':'");
            String[] parties = hash.split(":");
            assertEquals(2, parties.length);
            assertFalse(parties[0].isEmpty(), "Le sel ne doit pas etre vide");
            assertFalse(parties[1].isEmpty(), "Le hash ne doit pas etre vide");
        }
    }
}
