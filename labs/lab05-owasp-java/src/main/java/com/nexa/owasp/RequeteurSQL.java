package com.nexa.owasp;

/**
 * Classe demonstrant la vulnérabilité d'injection SQL dans la construction
 * de requetes SQL par concatenation de chaines.
 *
 * <p>Les méthodes vulnerables concatenent directement les entrees utilisateur
 * dans les clauses WHERE et LIKE. Les méthodes securisees utilisent des
 * paramètrès prepares (placeholder {@code ?}) qui empechent toute injection.</p>
 *
 * <p>L'injection SQL est la vulnérabilité numero 1 du Top 10 OWASP (A03:2021).</p>
 */
public class RequeteurSQLVulnerable {

    /**
     * Construit une requête SQL vulnerable a l'injection par concatenation directe.
     *
     * <p>L'entree utilisateur est inseree telle quelle dans la clause WHERE.
     * Un attaquant peut injecter {@code ' OR '1'='1} pour contourner
     * l'authentification ou {@code '; DROP TABLE users; --} pour detruire des donnees.</p>
     *
     * @param nom le nom utilisateur non validé
     * @return une requête SQL contenant l'entree utilisateur sans filtre
     */
    public String construireRequeteVulnerable(String nom) {
        return "SELECT * FROM users WHERE nom = '" + nom + "'";
    }

    /**
     * Construit une requête SQL securisee avec un paramètre prepare.
     *
     * <p>Le placeholder {@code ?} sera remplace par le SGBD avec l'entree
     * utilisateur correctement echappee, empechant toute injection.</p>
     *
     * @param nom le nom utilisateur (non utilisé dans la requête, lie separement)
     * @return une requête SQL avec paramètre prepare
     */
    public String construireRequeteSecurisee(String nom) {
        return "SELECT * FROM users WHERE nom = ?";
    }

    /**
     * Construit une requête de recherche LIKE vulnerable a l'injection SQL.
     *
     * <p>L'entree utilisateur est directement concatenee dans la clause LIKE,
     * y compris les caracteres jokers SQL ({@code %}, {@code _}).</p>
     *
     * @param terme le terme de recherche non validé
     * @return une requête SQL LIKE contenant l'entree utilisateur brute
     */
    public String construireRechercheVulnerable(String terme) {
        return "SELECT * FROM produits WHERE nom LIKE '%" + terme + "%'";
    }

    /**
     * Construit une requête de recherche LIKE securisee avec paramètre prepare.
     *
     * <p>Le placeholder {@code ?} protege contre l'injection SQL. Les jokers
     * doivent etre fournis dans la valeur du paramètre, pas dans la requête.</p>
     *
     * @return une requête SQL LIKE avec paramètre prepare
     */
    public String construireRechercheSecurisee() {
        return "SELECT * FROM produits WHERE nom LIKE ?";
    }

    /**
     * Encode un terme de recherche pour une clause LIKE securisee.
     *
     * <p>Les caracteres speciaux de LIKE ({@code %} et {@code _}) sont echappes
     * avec un antislash, puis le terme est encadre par des {@code %} pour une
     * recherche partielle.</p>
     *
     * @param terme le terme de recherche brut
     * @return le terme echappe et encadre par des jokers {@code %terme%}
     */
    public String encoderParametrePourLike(String terme) {
        return "%" + terme.replace("%", "\\%").replace("_", "\\_") + "%";
    }
}
