package com.nexa.parametres;

public class ValidateurUtilisateur {

    public boolean estEmailValide(String email) {
        if (email == null || email.isEmpty()) return false;
        if (email.length() > 255) return false;

        int arobaseIndex = email.indexOf('@');
        if (arobaseIndex <= 0) return false;           

        if (email.indexOf('@', arobaseIndex + 1) != -1) return false; 

        String domaine = email.substring(arobaseIndex + 1);
        return domaine.contains(".") && domaine.length() > 1;
    }

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

    public boolean estAgeValide(int age) {
        return age >= 18 && age <= 120;
    }

    public String categorieAge(int age) {
        if (age < 0) throw new IllegalArgumentException("L'âge ne peut pas être négatif");
        if (age < 18) return "MINEUR";
        if (age < 25) return "JEUNE_ADULTE";
        if (age < 60) return "ADULTE";
        if (age < 120) return "SENIOR";
        return "CENTENAIRE";
    }
}
