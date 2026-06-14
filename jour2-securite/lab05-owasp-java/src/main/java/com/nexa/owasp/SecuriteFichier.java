package com.nexa.owasp;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * <h1>SecuriteFichier — Protection contre le Path Traversal</h1>
 *
 * <h2>Path Traversal (Directory Traversal)</h2>
 * <p>
 * Un attaquant manipule le chemin d'un fichier pour sortir du répertoire
 * autorisé et accéder à des fichiers sensibles du système.
 * </p>
 *
 * <h3>Exemple d'attaque</h3>
 * <pre>
 * Entrée utilisateur : ../../etc/passwd
 * Chemin construit    : /var/data/../../etc/passwd → /etc/passwd
 * </pre>
 *
 * <h3>Correction</h3>
 * <ul>
 *   <li>Valider le nom du fichier (pas de ../, pas de caractères interdits)</li>
 *   <li>Normaliser le chemin et vérifier qu'il reste dans le répertoire autorisé</li>
 *   <li>Ne jamais construire un chemin fichier à partir d'une entrée utilisateur brute</li>
 * </ul>
 */
public class SecuriteFichier {

    private static final String REPERTOIRE_AUTORISE = "/var/data/";

    /**
     * ⚠️ VULNÉRABLE : Concaténation directe du nom de fichier.
     */
    public String construireCheminVulnerable(String nomFichier) {
        return REPERTOIRE_AUTORISE + nomFichier;
    }

    /**
     * ✅ SÉCURISÉ : Valide le nom de fichier et vérifie qu'il reste dans le répertoire.
     */
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

    /**
     * Détecte une tentative de path traversal dans une entrée utilisateur.
     */
    public boolean estTentativePathTraversal(String input) {
        if (input == null) return false;
        return input.contains("../") || input.contains("..\\") || input.contains("/..")
            || input.contains("\\..") || input.startsWith("/") || input.contains("\0");
    }
}
