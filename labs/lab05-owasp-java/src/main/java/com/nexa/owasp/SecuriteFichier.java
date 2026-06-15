package com.nexa.owasp;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Classe de securite des acces fichiers illustrant la vulnerabilite de
 * Path Traversal (ou Directory Traversal).
 *
 * <p>La version vulnerable concatene directement le nom de fichier au chemin
 * de base, permettant a un attaquant de remonter l'arborescence avec {@code ../}
 * pour acceder a des fichiers sensibles ({@code /etc/passwd}, etc.).</p>
 *
 * <p>La version securisee valide le nom de fichier, bloque les caracteres
 * interdits, et normalise le chemin en verifiant qu'il reste dans le
 * repertoire autorise.</p>
 *
 * <p>Cette vulnerabilite correspond a A01:2021 (Controle d'acces defaillant)
 * du Top 10 OWASP.</p>
 */
public class SecuriteFichier {

    /**
     * Repertoire de base autorise pour les acces fichiers.
     *
     * <p>Tout chemin construit doit rester a l'interieur de ce repertoire.
     * Dans un environnement de production, cette valeur serait chargee depuis
     * la configuration.</p>
     */
    private static final String REPERTOIRE_AUTORISE = "/var/data/";

    /**
     * Construit un chemin de fichier vulnerable au Path Traversal.
     *
     * <p>Le nom de fichier est directement concatene au repertoire autorise
     * sans validation. Un attaquant peut utiliser {@code ../../etc/passwd}
     * (ou {@code ..\..\windows\system32} sous Windows) pour sortir du
     * repertoire et lire des fichiers systeme.</p>
     *
     * @param nomFichier le nom de fichier non valide
     * @return le chemin complet potentiellement dangereux
     */
    public String construireCheminVulnerable(String nomFichier) {
        return REPERTOIRE_AUTORISE + nomFichier;
    }

    /**
     * Construit un chemin de fichier securise contre le Path Traversal.
     *
     * <p>Plusieurs couches de protection sont appliquees :</p>
     * <ol>
     *   <li>Rejet des noms nuls ou vides</li>
     *   <li>Rejet des noms contenant {@code ..}, {@code /} ou {@code \}</li>
     *   <li>Normalisation du chemin absolu</li>
     *   <li>Verification que le chemin reste dans le repertoire autorise</li>
     * </ol>
     *
     * @param nomFichier le nom de fichier a valider
     * @return le chemin normalise et securise
     * @throws IllegalArgumentException si le nom est invalide ou contient des caracteres interdits
     * @throws SecurityException si une tentative de Path Traversal est detectee
     */
    public String construireCheminSecurise(String nomFichier) {
        if (nomFichier == null || nomFichier.isEmpty()) {
            throw new IllegalArgumentException("Nom de fichier invalide");
        }
        if (nomFichier.contains("..") || nomFichier.contains("/") || nomFichier.contains("\\")) {
            throw new IllegalArgumentException("Caracteres interdits dans le nom de fichier");
        }

        Path base = Paths.get(REPERTOIRE_AUTORISE).normalize();
        Path fichier = base.resolve(nomFichier).normalize();

        if (!fichier.startsWith(base)) {
            throw new SecurityException("Tentative de path traversal detectee");
        }

        return fichier.toString();
    }

    /**
     * Detecte une tentative de Path Traversal dans une entree utilisateur.
     *
     * <p>Verifie la presence de motifs typiques d'attaque :</p>
     * <ul>
     *   <li>{@code ../} ou {@code ..\\} — remontee de repertoire</li>
     *   <li>{@code /..} ou {@code \..} — remontee depuis la racine</li>
     *   <li>Chemin absolu (debutant par {@code /})</li>
     *   <li>Octet nul ({@code \0}) — attaque par injection de terminateur</li>
     * </ul>
     *
     * @param input la chaine a analyser, peut etre nulle
     * @return {@code true} si une tentative de Path Traversal est detectee
     */
    public boolean estTentativePathTraversal(String input) {
        if (input == null) return false;
        return input.contains("../") || input.contains("..\\") || input.contains("/..")
            || input.contains("\\..") || input.startsWith("/") || input.contains("\0");
    }
}
