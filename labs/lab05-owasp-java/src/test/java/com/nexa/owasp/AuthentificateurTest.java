package com.nexa.owasp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires de securite pour la classe {@link Authentificateur}.
 *
 * <p>Cette classe de test valide deux categories de vulnerabilites OWASP :</p>
 * <ul>
 *   <li>Injection SQL : verification que la version vulnerable accepte les
 *       charges utiles et que la version securisee utilise un parametre prepare.</li>
 *   <li>Cross-Site Scripting (XSS) : verification que les scripts sont injectes
 *       dans la version vulnerable et echappes dans la version securisee.</li>
 * </ul>
 *
 * <p>Les cas nominaux et les cas limites (entrees nulles) sont egalement testes.</p>
 */
@DisplayName("OWASP : Tests de securite de l'Authentificateur")
class AuthentificateurTest {

    /**
     * Instance de l'authentificateur utilisee dans tous les tests.
     */
    private final Authentificateur auth = new Authentificateur();

    /**
     * Tests d'injection SQL.
     *
     * <p>Demontre que la methode vulnerable permet l'injection de charges
     * utiles SQL dans la requete, tandis que la methode securisee utilise
     * un parametre prepare qui empeche toute manipulation.</p>
     */
    @Nested
    @DisplayName("Injection SQL")
    class InjectionSQL {

        /**
         * Verifie que la requete vulnerable contient la charge utile d'injection SQL.
         *
         * <p>L'injection {@code ' OR '1'='1' --} modifie la clause WHERE
         * pour qu'elle retourne toujours vrai, contournant l'authentification.</p>
         */
        @Test
        @DisplayName("Requete vulnerable : injection SQL possible")
        void vulnerableInjectionPossible() {
            String requete = auth.rechercherUtilisateur("' OR '1'='1' --");
            assertTrue(requete.contains("' OR '1'='1' --"),
                "Preuve de vulnerabilite : la charge utile est dans la requete");
        }

        /**
         * Verifie que la requete securisee utilise un parametre prepare {@code ?}
         * et ne contient aucun guillemet issu de l'utilisateur.
         */
        @Test
        @DisplayName("Requete securisee : injection impossible avec parametre ?")
        void securiseePasInjection() {
            String requete = auth.rechercherUtilisateurSecurisee();
            assertTrue(requete.contains("?"));
            assertFalse(requete.contains("'"),
                "Aucun guillemet utilisateur dans la requete securisee");
        }

        /**
         * Verifie que plusieurs vecteurs d'injection SQL connus sont presents
         * dans la requete vulnerable, prouvant l'absence de filtrage.
         *
         * @param payload la charge utile d'injection SQL a tester
         */
        @ParameterizedTest
        @DisplayName("Detection injection SQL sur entrees malveillantes")
        @ValueSource(strings = {
            "' OR '1'='1",
            "'; DROP TABLE users; --",
            "1' UNION SELECT * FROM users --",
            "admin'--",
            "' OR 1=1 --"
        })
        void detectionMultiVecteurs(String payload) {
            String requete = auth.rechercherUtilisateur(payload);
            assertTrue(requete.contains(payload),
                "La charge utile '" + payload + "' atteint la requete vulnerable");
        }
    }

    /**
     * Tests de Cross-Site Scripting (XSS).
     *
     * <p>Demontre que la version vulnerable insere les scripts sans filtrage,
     * tandis que la version securisee echappe les caracteres speciaux HTML.</p>
     */
    @Nested
    @DisplayName("XSS")
    class XSS {

        /**
         * Verifie qu'une balise script est injectee telle quelle dans la
         * version vulnerable de l'affichage du profil.
         */
        @Test
        @DisplayName("Affichage vulnerable : script injecte dans HTML")
        void vulnerableScriptInjection() {
            String html = auth.afficherProfil("<script>alert('xss')</script>");
            assertTrue(html.contains("<script>"),
                "Preuve de vulnerabilite XSS : le script est dans le HTML");
        }

        /**
         * Verifie que la version securisee echappe la balise {@code <script>}
         * en {@code &lt;script&gt;}, la rendant inoffensive.
         */
        @Test
        @DisplayName("Affichage securise : script echappe en &lt;script&gt;")
        void securiseeScriptEchappe() {
            String html = auth.afficherProfilSecurisee("<script>alert('xss')</script>");
            assertFalse(html.contains("<script>"),
                "Pas de balise script dans la sortie securisee");
            assertTrue(html.contains("&lt;script&gt;"),
                "La balise script est echappee");
        }

        /**
         * Verifie l'echappement de plusieurs vecteurs XSS courants dans la
         * version securisee (balises script, gestionnaires d'evenements).
         *
         * @param payload la charge utile XSS a tester
         */
        @ParameterizedTest
        @DisplayName("Echappement XSS sur vecteurs multiples")
        @ValueSource(strings = {
            "<img src=x onerror=alert(1)>",
            "<svg onload=alert(1)>",
            "\"><script>alert(1)</script>"
        })
        void echappementMultiVecteurs(String payload) {
            String html = auth.afficherProfilSecurisee(payload);
            assertFalse(html.contains("<script>"));
            assertFalse(html.contains("onerror="));
            assertFalse(html.contains("onload="));
        }
    }

    /**
     * Tests des cas nominaux (utilisation normale sans attaque).
     *
     * <p>Verifient que les fonctionnalites de base fonctionnent correctement
     * avec des entrees legitimes.</p>
     */
    @Nested
    @DisplayName("Cas nominaux")
    class CasNominaux {

        /**
         * Verifie qu'un login normal est correctement insere dans la requete.
         */
        @Test
        @DisplayName("Login normal fonctionne")
        void loginNormal() {
            String requete = auth.rechercherUtilisateur("jean.dupont");
            assertTrue(requete.contains("jean.dupont"));
        }

        /**
         * Verifie qu'un nom sans script est correctement affiche dans la
         * version securisee du profil.
         */
        @Test
        @DisplayName("Profil normal sans script")
        void profilNormal() {
            String html = auth.afficherProfilSecurisee("Jean Dupont");
            assertTrue(html.contains("Jean Dupont"));
        }
    }

    /**
     * Tests des cas limites avec entrees nulles.
     */
    @Nested
    @DisplayName("Cas null")
    class CasNull {

        /**
         * Verifie que l'echappement d'une entree nulle retourne une chaine vide.
         */
        @Test
        @DisplayName("Echappement de null retourne chaine vide")
        void echapperNull() {
            String resultat = auth.echapperHtml(null);
            assertEquals("", resultat);
        }
    }
}
