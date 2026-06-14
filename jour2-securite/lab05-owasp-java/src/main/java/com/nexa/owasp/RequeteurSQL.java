package com.nexa.owasp;

/**
 * <h1>RequeteurSQLVulnerable — Démonstration d'injection SQL</h1>
 *
 * <p>NE JAMAIS UTILISER CE CODE EN PRODUCTION.</p>
 *
 * <h2>L'injection SQL</h2>
 * <p>
 * L'injection SQL se produit quand des données utilisateur sont concaténées
 * DIRECTEMENT dans une requête SQL, sans validation ni échappement.
 * </p>
 *
 * <h3>Exemple d'attaque</h3>
 * <pre>
 * Entrée utilisateur : ' OR '1'='1' --
 * Requête construite : SELECT * FROM users WHERE email = '' OR '1'='1' --'
 * Résultat : l'attaquant récupère TOUS les utilisateurs.
 * </pre>
 *
 * <h3>Correction : PreparedStatement (JDBC) ou requêtes paramétrées (JPA)</h3>
 * <p>
 * Avec un PreparedStatement, les paramètres sont traités comme des VALEURS,
 * jamais comme du code SQL. Le pilote JDBC échappe automatiquement.
 * </p>
 */
public class RequeteurSQLVulnerable {

    /**
     * ⚠️ VULNÉRABLE : Concaténation directe dans la requête SQL.
     * Un attaquant peut injecter du SQL via le paramètre `nom`.
     */
    public String construireRequeteVulnerable(String nom) {
        return "SELECT * FROM users WHERE nom = '" + nom + "'";
    }

    /**
     * ✅ SÉCURISÉ : Utilise un paramètre (?), sans concaténation.
     * Le pilote JDBC traite `nom` comme une valeur, pas comme du code.
     */
    public String construireRequeteSecurisee(String nom) {
        return "SELECT * FROM users WHERE nom = ?";
    }

    /**
     * ⚠️ VULNÉRABLE : LIKE avec concaténation.
     */
    public String construireRechercheVulnerable(String terme) {
        return "SELECT * FROM produits WHERE nom LIKE '%" + terme + "%'";
    }

    /**
     * ✅ SÉCURISÉ : Paramètre avec les % ajoutés dans le code Java, pas dans la requête.
     */
    public String construireRechercheSecurisee() {
        return "SELECT * FROM produits WHERE nom LIKE ?";
    }

    public String encoderParametrePourLike(String terme) {
        return "%" + terme.replace("%", "\\%").replace("_", "\\_") + "%";
    }
}
