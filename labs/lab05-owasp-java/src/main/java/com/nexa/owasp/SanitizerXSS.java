package com.nexa.owasp;

import java.util.regex.Pattern;

/**
 * Classe de protection contre les attaques Cross-Site Scripting (XSS).
 *
 * <p>Fournit des methodes vulnerables et securisees pour la generation de
 * contenu HTML, ainsi que des fonctions d'echappement et de detection de
 * scripts malveillants.</p>
 *
 * <p>Le XSS (A03:2021 dans le Top 10 OWASP) permet a un attaquant d'injecter
 * du code JavaScript dans une page vue par d'autres utilisateurs.</p>
 */
public class SanitizerXSS {

    /**
     * Genere une page d'accueil vulnerable au XSS.
     *
     * <p>Le nom utilisateur est insere directement dans le HTML sans
     * echappement. Un attaquant peut fournir {@code <script>alert(1)</script>}
     * qui sera execute par le navigateur de la victime.</p>
     *
     * @param nom le nom utilisateur non echappe
     * @return une page HTML vulnerable contenant potentiellement du code malveillant
     */
    public String genererPageAccueilVulnerable(String nom) {
        return "<html><body><h1>Bienvenue " + nom + "</h1></body></html>";
    }

    /**
     * Genere une page d'accueil securisee contre le XSS.
     *
     * <p>Le nom utilisateur est echappe via {@link #echapperHtml(String)}
     * avant insertion, neutralisant toute balise HTML ou script.</p>
     *
     * @param nom le nom utilisateur a afficher
     * @return une page HTML ou les caracteres speciaux sont echappes
     */
    public String genererPageAccueilSecurisee(String nom) {
        return "<html><body><h1>Bienvenue " + echapperHtml(nom) + "</h1></body></html>";
    }

    /**
     * Echappe les caracteres speciaux HTML pour neutraliser les attaques XSS.
     *
     * <p>Les substitutions effectuees :</p>
     * <ul>
     *   <li>{@code &} → {@code &amp;amp;}</li>
     *   <li>{@code <} → {@code &amp;lt;}</li>
     *   <li>{@code >} → {@code &amp;gt;}</li>
     *   <li>{@code "} → {@code &amp;quot;}</li>
     *   <li>{@code '} → {@code &amp;#39;}</li>
     * </ul>
     *
     * @param input la chaine a echapper, peut etre nulle
     * @return la chaine echappee, ou une chaine vide si l'entree est nulle
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
     * Detecte la presence de motifs XSS courants dans une entree utilisateur.
     *
     * <p>Recherche les motifs suivants (insensible a la casse) :</p>
     * <ul>
     *   <li>{@code <script} — balise script</li>
     *   <li>{@code javascript:} — pseudo-protocole JavaScript</li>
     *   <li>{@code onerror=} — gestionnaire d'evenement d'erreur</li>
     *   <li>{@code onload=} — gestionnaire d'evenement de chargement</li>
     *   <li>{@code onclick=} — gestionnaire d'evenement de clic</li>
     * </ul>
     *
     * <p>Cette detection est une heuristique et ne remplace pas un echappement
     * systematique.</p>
     *
     * @param input la chaine a analyser, peut etre nulle
     * @return {@code true} si un motif XSS est detecte, {@code false} sinon
     */
    public boolean contientScript(String input) {
        if (input == null) return false;
        String lower = input.toLowerCase();
        return lower.contains("<script") || lower.contains("javascript:") || lower.contains("onerror=")
            || lower.contains("onload=") || lower.contains("onclick=");
    }
}
