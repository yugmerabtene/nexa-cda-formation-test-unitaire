package com.nexa.owasp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OWASP : Tests du SanitizerXSS")
class SanitizerXSSTest {

    private final SanitizerXSS sanitizer = new SanitizerXSS();

    @Nested
    @DisplayName("Attaques XSS")
    class AttaquesXSS {

        @Test
        @DisplayName("Page vulnérable : le script est injecté tel quel")
        void pageVulnerableScriptNonEchappe() {
            String page = sanitizer.genererPageAccueilVulnerable("<script>alert('XSS')</script>");
            assertTrue(page.contains("<script>"),
                "Preuve de vulnérabilité : la balise script est présente");
        }

        @Test
        @DisplayName("Page sécurisée : le script est neutralisé")
        void pageSecuriseeScriptNeutralise() {
            String page = sanitizer.genererPageAccueilSecurisee("<script>alert('XSS')</script>");
            assertFalse(page.contains("<script>"),
                "La balise script est neutralisée : " + page);
            assertTrue(page.contains("&lt;script&gt;"),
                "Le script est échappé en entités HTML");
        }

        @Test
        @DisplayName("Détection de contenu script malveillant")
        void detectionScriptMalveillant() {
            assertTrue(sanitizer.contientScript("<script>alert(1)</script>"));
            assertTrue(sanitizer.contientScript("<img src=x onerror=alert(1)>"));
            assertTrue(sanitizer.contientScript("javascript:void(0)"));
            assertTrue(sanitizer.contientScript("<body onload=alert(1)>"));
        }

        @Test
        @DisplayName("Contenu légitime non détecté comme malveillant")
        void contenuLegitimeNonDetecte() {
            assertFalse(sanitizer.contientScript("Bonjour tout le monde"));
            assertFalse(sanitizer.contientScript("Je m'appelle Jean"));
            assertFalse(sanitizer.contientScript(null));
        }

        @Test
        @DisplayName("Échappement HTML : tous les caractères spéciaux")
        void echappementCaracteresSpeciaux() {
            assertEquals("&lt;&gt;&amp;&quot;&#39;",
                sanitizer.echapperHtml("<>&\"'"));
        }

        @Test
        @DisplayName("Entrée null retourne chaîne vide")
        void entreeNullRetourneVide() {
            assertEquals("", sanitizer.echapperHtml(null));
        }
    }

    @ParameterizedTest
    @DisplayName("Neutralisation de vecteurs XSS connus")
    @ValueSource(strings = {
        "<script>alert(1)</script>",
        "<img src=x onerror='alert(1)'>",
        "\"><script>alert(document.cookie)</script>",
        "<svg onload=alert(1)>",
        "'-alert(1)-'",
        "<body onload='alert(1)'>"
    })
    void neutralisationVecteursXSS(String vecteur) {
        String securise = sanitizer.echapperHtml(vecteur);
        assertFalse(securise.contains("<script>"), "Script non neutralisé pour : " + vecteur);
        assertFalse(securise.contains("<script "), "Script avec espace non neutralisé");
    }
}
