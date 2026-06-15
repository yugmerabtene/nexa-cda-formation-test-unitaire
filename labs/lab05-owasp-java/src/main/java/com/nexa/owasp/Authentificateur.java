package com.nexa.owasp;

/**
 * Classe demonstrant les vulnerabilites de sécurité OWASP liees a l'injection SQL
 * et au Cross-Site Scripting (XSS) dans une application Java.
 *
 * <p>Chaque fonctionnalite expose une version vulnerable (concatenation directe)
 * et une version securisee (paramètre prepare ou échappement HTML).</p>
 *
 * <p>Les tests unitaires associes dans {@code AuthentificateurTest} prouvent
 * l'existence des failles et verifient l'efficacite des correctifs.</p>
 */
public class Authentificateur {

    /**
     * Construit une requête SQL vulnerable a l'injection SQL.
     *
     * <p>L'entree utilisateur est directement concatenee dans la clause WHERE,
     * permettant a un attaquant de modifier la structure de la requête.</p>
     *
     * @param login le login utilisateur non filtre
     * @return une requête SQL contenant l'entree utilisateur sans validation
     */
    public String rechercherUtilisateur(String login) {
        return "SELECT * FROM users WHERE login = '" + login + "'";
    }

    /**
     * Construit une requête SQL securisee utilisant un paramètre prepare.
     *
     * <p>Le point d'interrogation (?) est un placeholder pour un paramètre lie,
     * empechant toute modification de la structure de la requête.</p>
     *
     * @return une requête SQL avec paramètre prepare, immunisee contre l'injection
     */
    public String rechercherUtilisateurSecurisee() {
        return "SELECT * FROM users WHERE login = ?";
    }

    /**
     * Genere une page HTML vulnerable au Cross-Site Scripting (XSS).
     *
     * <p>Le contenu HTML fourni est insere tel quel sans filtrage, permettant
     * l'injection de balises script et l'exécution de code JavaScript malveillant.</p>
     *
     * @param html le contenu HTML utilisateur non echappe
     * @return une page HTML vulnerable contenant le code injecte
     */
    public String afficherProfil(String html) {
        return "<html><body><div>" + html + "</div></body></html>";
    }

    /**
     * Genere une page HTML securisee contre le XSS.
     *
     * <p>Le contenu utilisateur est echappe via {@link #echapperHtml(String)}
     * avant insertion, neutralisant toute balise ou script malveillant.</p>
     *
     * @param html le contenu utilisateur a afficher
     * @return une page HTML ou les caracteres speciaux sont echappes
     */
    public String afficherProfilSecurisee(String html) {
        return "<html><body><div>" + echapperHtml(html) + "</div></body></html>";
    }

    /**
     * Echappe les caracteres speciaux HTML pour prevenir les attaques XSS.
     *
     * <p>Transforme les caracteres suivants en entites HTML :
     * {@code &} → {@code &amp;amp;}, {@code <} → {@code &amp;lt;},
     * {@code >} → {@code &amp;gt;}, {@code "} → {@code &amp;quot;},
     * {@code '} → {@code &amp;#39;}.</p>
     *
     * <p>Retourne une chaine vide si l'entree est nulle.</p>
     *
     * @param input la chaine a échapper, peut etre nulle
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
}
