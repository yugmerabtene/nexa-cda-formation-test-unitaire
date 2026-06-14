package com.nexa.owasp;

import java.util.regex.Pattern;

/**
 * <h1>SanitizerXSS — Protection contre les attaques Cross-Site Scripting</h1>
 *
 * <h2>XSS (Cross-Site Scripting)</h2>
 * <p>
 * Un attaquant injecte du code JavaScript malveillant dans une page web
 * via des entrées utilisateur non échappées.
 * </p>
 *
 * <h3>Types de XSS</h3>
 * <ul>
 *   <li><b>Reflected XSS</b> : le script est dans l'URL/paramètre et exécuté immédiatement</li>
 *   <li><b>Stored XSS</b> : le script est stocké en base et exécuté à chaque affichage</li>
 *   <li><b>DOM-based XSS</b> : manipulation du DOM côté client</li>
 * </ul>
 *
 * <h3>Correction</h3>
 * <ul>
 *   <li>Échapper les caractères HTML (&lt; &gt; &amp; &quot; &#39;)</li>
 *   <li>Utiliser des bibliothèques dédiées : OWASP Java Encoder, HtmlUtils</li>
 *   <li>Jamais faire confiance aux entrées utilisateur</li>
 * </ul>
 */
public class SanitizerXSS {

    /**
     * ⚠️ VULNÉRABLE : Aucun échappement.
     * Si `nom` contient {@code <script>alert('XSS')</script>},
     * le script sera exécuté dans le navigateur.
     */
    public String genererPageAccueilVulnerable(String nom) {
        return "<html><body><h1>Bienvenue " + nom + "</h1></body></html>";
    }

    /**
     * ✅ SÉCURISÉ : Échappe les caractères HTML spéciaux.
     */
    public String genererPageAccueilSecurisee(String nom) {
        return "<html><body><h1>Bienvenue " + echapperHtml(nom) + "</h1></body></html>";
    }

    /**
     * Échappe les 5 caractères HTML critiques.
     * <ul>
     *   <li>&lt; → &amp;lt; (empêche l'ouverture de balises HTML)</li>
     *   <li>&gt; → &amp;gt; (empêche la fermeture de balises HTML)</li>
     *   <li>&amp; → &amp;amp; (empêche les entités HTML malveillantes)</li>
     *   <li>&quot; → &amp;quot; (empêche la sortie d'attribut HTML)</li>
     *   <li>&#39; → &amp;#39; (empêche la sortie d'attribut HTML simple quote)</li>
     * </ul>
     */
    public String echapperHtml(String input) {
        if (input == null) return "";
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    /**
     * Détecte la présence de contenu potentiellement malveillant (heuristique simple).
     */
    public boolean contientScript(String input) {
        if (input == null) return false;
        String lower = input.toLowerCase();
        return lower.contains("<script") || lower.contains("javascript:") || lower.contains("onerror=")
            || lower.contains("onload=") || lower.contains("onclick=");
    }
}
