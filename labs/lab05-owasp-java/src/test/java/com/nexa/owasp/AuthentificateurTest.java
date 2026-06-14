package com.nexa.owasp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OWASP : Tests de securite de l'Authentificateur")
class AuthentificateurTest {

    private final Authentificateur auth = new Authentificateur();

    @Nested
    @DisplayName("Injection SQL")
    class InjectionSQL {

        @Test
        @DisplayName("Requete vulnerable : injection SQL possible")
        void vulnerableInjectionPossible() {
            String requete = auth.rechercherUtilisateur("' OR '1'='1' --");
            assertTrue(requete.contains("' OR '1'='1' --"),
                "Preuve de vulnerabilite : la charge utile est dans la requete");
        }

        @Test
        @DisplayName("Requete securisee : injection impossible avec parametre ?")
        void securiseePasInjection() {
            String requete = auth.rechercherUtilisateurSecurisee();
            assertTrue(requete.contains("?"));
            assertFalse(requete.contains("'"),
                "Aucun guillemet utilisateur dans la requete securisee");
        }

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

    @Nested
    @DisplayName("XSS")
    class XSS {

        @Test
        @DisplayName("Affichage vulnerable : script injecte dans HTML")
        void vulnerableScriptInjection() {
            String html = auth.afficherProfil("<script>alert('xss')</script>");
            assertTrue(html.contains("<script>"),
                "Preuve de vulnerabilite XSS : le script est dans le HTML");
        }

        @Test
        @DisplayName("Affichage securise : script echappe en &lt;script&gt;")
        void securiseeScriptEchappe() {
            String html = auth.afficherProfilSecurisee("<script>alert('xss')</script>");
            assertFalse(html.contains("<script>"),
                "Pas de balise script dans la sortie securisee");
            assertTrue(html.contains("&lt;script&gt;"),
                "La balise script est echappee");
        }

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

    @Nested
    @DisplayName("Cas nominaux")
    class CasNominaux {

        @Test
        @DisplayName("Login normal fonctionne")
        void loginNormal() {
            String requete = auth.rechercherUtilisateur("jean.dupont");
            assertTrue(requete.contains("jean.dupont"));
        }

        @Test
        @DisplayName("Profil normal sans script")
        void profilNormal() {
            String html = auth.afficherProfilSecurisee("Jean Dupont");
            assertTrue(html.contains("Jean Dupont"));
        }
    }

    @Nested
    @DisplayName("Cas null")
    class CasNull {

        @Test
        @DisplayName("Echappement de null retourne chaine vide")
        void echapperNull() {
            String resultat = auth.echapperHtml(null);
            assertEquals("", resultat);
        }
    }
}
