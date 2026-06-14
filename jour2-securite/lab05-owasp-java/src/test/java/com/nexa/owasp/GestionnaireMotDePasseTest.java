package com.nexa.owasp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OWASP : Gestion des mots de passe")
class GestionnaireMotDePasseTest {

    private final GestionnaireMotDePasse gestionnaire = new GestionnaireMotDePasse();

    @Nested
    @DisplayName("Hachage et vérification")
    class HachageVerification {

        @Test
        @DisplayName("Hachage vulnérable : déterministe, même entrée → même sortie")
        void hachageVulnerableDeterministe() {
            String h1 = gestionnaire.hacherVulnerable("password123");
            String h2 = gestionnaire.hacherVulnerable("password123");
            assertEquals(h1, h2,
                "Le hachage sans sel produit toujours le même résultat → vulnérable aux rainbow tables");
        }

        @Test
        @DisplayName("Hachage sécurisé : aléatoire, même entrée → sorties différentes")
        void hachageSecuriseAleatoire() {
            String h1 = gestionnaire.hacherAvecSel("password123");
            String h2 = gestionnaire.hacherAvecSel("password123");
            assertNotEquals(h1, h2,
                "Avec un sel aléatoire, le même mot de passe produit des hashs différents");
        }

        @Test
        @DisplayName("Vérification : mot de passe correct validé")
        void verificationMotDePasseCorrect() {
            String hash = gestionnaire.hacherAvecSel("monSuperMotDePasse");
            assertTrue(gestionnaire.verifierMotDePasse("monSuperMotDePasse", hash));
        }

        @Test
        @DisplayName("Vérification : mot de passe incorrect rejeté")
        void verificationMotDePasseIncorrect() {
            String hash = gestionnaire.hacherAvecSel("motDePasse");
            assertFalse(gestionnaire.verifierMotDePasse("mauvaisMotDePasse", hash));
        }

        @Test
        @DisplayName("Vérification : hash invalide rejeté")
        void verificationHashInvalide() {
            assertFalse(gestionnaire.verifierMotDePasse("test", "hash_invalide"));
            assertFalse(gestionnaire.verifierMotDePasse("test", null));
        }

        @Test
        @DisplayName("Hash sécurisé contient le sel (format sel:hash)")
        void hashSecuriseContientSel() {
            String hash = gestionnaire.hacherAvecSel("test");
            assertTrue(hash.contains(":"),
                "Le hash doit contenir le sel et le hash séparés par ':'");
            String[] parties = hash.split(":");
            assertEquals(2, parties.length);
            assertFalse(parties[0].isEmpty(), "Le sel ne doit pas être vide");
            assertFalse(parties[1].isEmpty(), "Le hash ne doit pas être vide");
        }
    }
}
