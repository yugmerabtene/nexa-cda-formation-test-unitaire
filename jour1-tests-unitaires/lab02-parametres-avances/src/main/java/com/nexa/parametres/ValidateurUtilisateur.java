package com.nexa.parametres;

/**
 * <h1>Validateur de données utilisateur</h1>
 *
 * <p>Cette classe contient des méthodes de validation « pures »
 * (sans dépendances externes), ce qui les rend idéales pour les tests paramétrés.</p>
 *
 * <p>Une méthode de validation bien conçue retourne un booléen
 * (vrai = valide / faux = invalide) plutôt que de lancer une exception.
 * Cela facilite les tests car on n'a pas à gérer les exceptions pour les cas invalides.</p>
 */
public class ValidateurUtilisateur {

    /**
     * Valide une adresse email selon des règles métier simplifiées.
     *
     * <h3>Règles :</h3>
     * <ul>
     *   <li>Ne peut pas être null</li>
     *   <li>Ne peut pas être vide</li>
     *   <li>Doit contenir exactement un {@code @}</li>
     *   <li>Partie locale (avant le {@code @}) non vide</li>
     *   <li>Partie domaine (après le {@code @}) doit contenir un point</li>
     *   <li>Longueur maximale 255 caractères</li>
     * </ul>
     */
    public boolean estEmailValide(String email) {
        if (email == null || email.isEmpty()) return false;
        if (email.length() > 255) return false;

        int arobaseIndex = email.indexOf('@');
        if (arobaseIndex <= 0) return false;           // @ pas présent ou au début
        if (email.indexOf('@', arobaseIndex + 1) != -1) return false; // plusieurs @

        String domaine = email.substring(arobaseIndex + 1);
        return domaine.contains(".") && domaine.length() > 1;
    }

    /**
     * Valide un numéro de téléphone français.
     *
     * <h3>Règles :</h3>
     * <ul>
     *   <li>10 chiffres</li>
     *   <li>Commence par 0</li>
     *   <li>Le 2ᵉ chiffre est entre 1 et 9</li>
     * </ul>
     * <p>Formats acceptés : 0612345678 ou 06 12 34 56 78 ou +33612345678</p>
     */
    public boolean estTelephoneValide(String telephone) {
        if (telephone == null || telephone.isEmpty()) return false;

        String nettoye = telephone.replaceAll("[\\s.+-]", "");
        if (nettoye.length() == 12 && nettoye.startsWith("+33")) {
            nettoye = "0" + nettoye.substring(3);
        }
        if (nettoye.length() != 10) return false;
        if (nettoye.charAt(0) != '0') return false;
        char deuxieme = nettoye.charAt(1);
        if (deuxieme < '1' || deuxieme > '9') return false;

        for (int i = 2; i < nettoye.length(); i++) {
            if (!Character.isDigit(nettoye.charAt(i))) return false;
        }
        return true;
    }

    /**
     * Calcule le score de robustesse d'un mot de passe (0 à 100).
     */
    public int scoreMotDePasse(String motDePasse) {
        if (motDePasse == null || motDePasse.isEmpty()) return 0;
        int score = 0;
        if (motDePasse.length() >= 8) score += 25;
        if (motDePasse.length() >= 12) score += 15;
        if (motDePasse.matches(".*[A-Z].*")) score += 20;
        if (motDePasse.matches(".*[a-z].*")) score += 15;
        if (motDePasse.matches(".*[0-9].*")) score += 15;
        if (motDePasse.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) score += 10;
        return Math.min(score, 100);
    }

    /**
     * Indique si un âge est valide pour l'inscription.
     * Doit être entre 18 et 120 ans inclus.
     */
    public boolean estAgeValide(int age) {
        return age >= 18 && age <= 120;
    }

    /**
     * Catégorise un âge en tranche.
     */
    public String categorieAge(int age) {
        if (age < 0) throw new IllegalArgumentException("L'âge ne peut pas être négatif");
        if (age < 18) return "MINEUR";
        if (age < 25) return "JEUNE_ADULTE";
        if (age < 60) return "ADULTE";
        if (age < 120) return "SENIOR";
        return "CENTENAIRE";
    }
}
