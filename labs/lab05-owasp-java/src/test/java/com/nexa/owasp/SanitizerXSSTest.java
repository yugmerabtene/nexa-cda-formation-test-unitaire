package com.nexa.owasp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour la classe {@link SanitizerXSS}.
 *
 * <p>Valide les mecanismes de protection contre le Cross-Site Scripting :</p>
 * <ul>
 *   <li>Version vulnerable : les scripts sont injectes sans modification.</li>
 *   <li>Version securisee : les scripts sont echappes en entites HTML.</li>
 *   <li>Detection : identification des motifs XSS courants.</li>
 *   <li>Echappement : transformation de tous les caracteres speciaux HTML.</li>
 * </ul>
 */
@DisplayName("OWASP : Tests du SanitizerXSS")
class SanitizerXSSTest {

    /**
     * Instance du sanitizer XSS utilisé́e dans tous les tests.
     */
    private final SanitizerXSS sanitizer = new SanitizerXSS();

    /**
     * Tests des attaques XSS : injection, neutralisation, detection et échappement.
     */
    @Nested
    @DisplayName("Attaques XSS")
    class AttaquesXSS {

        /**
         * Verifie que la page vulnerable contient la balise script sans
         * modification, prouvant la vulnérabilité XSS.
         */
        @Test
        @DisplayName("Page vulnerable : le script est injecte tel quel")
        void pageVulnerableScriptNonEchappe() {
            String page = sanitizer.genererPageAccueilVulnerable("<script>alert('XSS')</script>");
            assertTrue(page.contains("<script>"),
                "Preuve de vulnérabilité : la balise script est presente");
        }

        /**
         * Verifie que la page securisee echappe la balise script en entites HTML,
         * rendant le code JavaScript inexecutable.
         */
        @Test
        @DisplayName("Page securisee : le script est neutralise")
        void pageSecuriseeScriptNeutralise() {
            String page = sanitizer.genererPageAccueilSecurisee("<script>alert('XSS')</script>");
            assertFalse(page.contains("<script>"),
                "La balise script est neutralisee : " + page);
            assertTrue(page.contains("&lt;script&gt;"),
                "Le script est echappe en entites HTML");
        }

        /**
         * Verifie la detection de plusieurs motifs XSS courants dans les
         * entrees utilisateur (balises script, gestionnaires d'evenements,
         * pseudo-protocole javascript).
         */
        @Test
        @DisplayName("Detection de contenu script malveillant")
        void detectionScriptMalveillant() {
            assertTrue(sanitizer.contientScript("<script>alert(1)</script>"));
            assertTrue(sanitizer.contientScript("<img src=x onerror=alert(1)>"));
            assertTrue(sanitizer.contientScript("javascript:void(0)"));
            assertTrue(sanitizer.contientScript("<body onload=alert(1)>"));
        }

        /**
         * Verifie que du contenu legitime (texte simple, noms) n'est pas
         * detecte comme malveillant (pas de faux positifs).
         */
        @Test
        @DisplayName("Contenu legitime non detecte comme malveillant")
        void contenuLegitimeNonDetecte() {
            assertFalse(sanitizer.contientScript("Bonjour tout le monde"));
            assertFalse(sanitizer.contientScript("Je m'appelle Jean"));
            assertFalse(sanitizer.contientScript(null));
        }

        /**
         * Verifie que tous les caracteres speciaux HTML sont correctement
         * echappes en leurs entites respectives.
         */
        @Test
        @DisplayName("Echappement HTML : tous les caracteres speciaux")
        void echappementCaracteresSpeciaux() {
            assertEquals("&lt;&gt;&amp;&quot;&#39;",
                sanitizer.echapperHtml("<>&\"'"));
        }

        /**
         * Verifie que l'échappement d'une entree nulle retourne une chaine vide.
         */
        @Test
        @DisplayName("Entree null retourne chaine vide")
        void entreeNullRetourneVide() {
            assertEquals("", sanitizer.echapperHtml(null));
        }
    }

    /**
     * Test paramètre verifiant que les vecteurs XSS les plus connus sont
     * neutralises par l'échappement HTML (plus de balise script active).
     *
     * @param vecteur la charge utile XSS a neutraliser
     */
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
        assertFalse(securise.contains("<script>"), "Script non neutralise pour : " + vecteur);
        assertFalse(securise.contains("<script "), "Script avec espace non neutralise");
    }
}
