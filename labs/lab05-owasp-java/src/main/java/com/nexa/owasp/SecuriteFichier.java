package com.nexa.owasp;

import java.nio.file.Path;
import java.nio.file.Paths;

public class SecuriteFichier {

    private static final String REPERTOIRE_AUTORISE = "/var/data/";

    public String construireCheminVulnerable(String nomFichier) {
        return REPERTOIRE_AUTORISE + nomFichier;
    }

    public String construireCheminSecurise(String nomFichier) {
        if (nomFichier == null || nomFichier.isEmpty()) {
            throw new IllegalArgumentException("Nom de fichier invalide");
        }
        if (nomFichier.contains("..") || nomFichier.contains("/") || nomFichier.contains("\\")) {
            throw new IllegalArgumentException("Caractères interdits dans le nom de fichier");
        }

        Path base = Paths.get(REPERTOIRE_AUTORISE).normalize();
        Path fichier = base.resolve(nomFichier).normalize();

        if (!fichier.startsWith(base)) {
            throw new SecurityException("Tentative de path traversal détectée");
        }

        return fichier.toString();
    }

    public boolean estTentativePathTraversal(String input) {
        if (input == null) return false;
        return input.contains("../") || input.contains("..\\") || input.contains("/..")
            || input.contains("\\..") || input.startsWith("/") || input.contains("\0");
    }
}
