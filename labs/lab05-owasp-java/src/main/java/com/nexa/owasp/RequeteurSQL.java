package com.nexa.owasp;

public class RequeteurSQLVulnerable {

    public String construireRequeteVulnerable(String nom) {
        return "SELECT * FROM users WHERE nom = '" + nom + "'";
    }

    public String construireRequeteSecurisee(String nom) {
        return "SELECT * FROM users WHERE nom = ?";
    }

    public String construireRechercheVulnerable(String terme) {
        return "SELECT * FROM produits WHERE nom LIKE '%" + terme + "%'";
    }

    public String construireRechercheSecurisee() {
        return "SELECT * FROM produits WHERE nom LIKE ?";
    }

    public String encoderParametrePourLike(String terme) {
        return "%" + terme.replace("%", "\\%").replace("_", "\\_") + "%";
    }
}
