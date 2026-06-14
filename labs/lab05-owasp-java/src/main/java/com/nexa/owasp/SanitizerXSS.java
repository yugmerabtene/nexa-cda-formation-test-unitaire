package com.nexa.owasp;

import java.util.regex.Pattern;

public class SanitizerXSS {

    public String genererPageAccueilVulnerable(String nom) {
        return "<html><body><h1>Bienvenue " + nom + "</h1></body></html>";
    }

    public String genererPageAccueilSecurisee(String nom) {
        return "<html><body><h1>Bienvenue " + echapperHtml(nom) + "</h1></body></html>";
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

    public boolean contientScript(String input) {
        if (input == null) return false;
        String lower = input.toLowerCase();
        return lower.contains("<script") || lower.contains("javascript:") || lower.contains("onerror=")
            || lower.contains("onload=") || lower.contains("onclick=");
    }
}
