package com.nexa.owasp;

public class Authentificateur {

    public String rechercherUtilisateur(String login) {
        return "SELECT * FROM users WHERE login = '" + login + "'";
    }

    public String rechercherUtilisateurSecurisee() {
        return "SELECT * FROM users WHERE login = ?";
    }

    public String afficherProfil(String html) {
        return "<html><body><div>" + html + "</div></body></html>";
    }

    public String afficherProfilSecurisee(String html) {
        return "<html><body><div>" + echapperHtml(html) + "</div></body></html>";
    }

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
