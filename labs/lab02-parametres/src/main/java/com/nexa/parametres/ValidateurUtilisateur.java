package com.nexa.paramètrès;

/**
 * Validateur de saisies utilisateur.
 *
 * Cette classe implemente 5 méthodes de validation :
 * - Email (format RFC basique)
 * - Telephone (formats francais : 10 chiffres, +33)
 * - Score de mot de passe (robustesse 0-100)
 * - Age validé (18-120 ans)
 * - Categorie d'age (MINEUR a CENTENAIRE)
 *
 * Chaque méthode est concue pour etre testee de maniere exhaustive
 * avec des tests paramètrès (@ParameterizedTest) qui couvrent
 * des dizaines de cas en une seule méthode de test.
 */
public class ValidateurUtilisateur {

    /**
     * Valide le format d'une adresse email.
     *
     * Regles de validation :
     * 1. Ne doit pas etre null ou vide
     * 2. Longueur maximale : 255 caracteres (limite RFC 5321)
     * 3. Doit contenir exactement un caractere '@'
     * 4. Le '@' ne doit pas etre en première position
     * 5. Le domaine (après le '@') doit contenir un point
     *
     * @param email l'adresse email a valider (peut etre null)
     * @return true si l'email est syntaxiquement validé
     *
     * Exemples :
     * - "test@example.com" -> true
     * - "user@domain" -> false (pas de point dans le domaine)
     * - "@domaine.com" -> false (pas de partie locale)
     */
    public boolean estEmailValide(String email) {
        // Protection contre null et chaine vide
        if (email == null || email.isEmpty()) return false;
        // Protection contre les emails trop longs (RFC 5321)
        if (email.length() > 255) return false;

        // Trouver la position du premier '@'
        int arobaseIndex = email.indexOf('@');
        // Le '@' ne doit pas etre absent ni en première position
        if (arobaseIndex <= 0) return false;

        // Verifier qu'il n'y a qu'un seul '@'
        // Si un second '@' est trouve après le premier, c'est invalide
        if (email.indexOf('@', arobaseIndex + 1) != -1) return false;

        // Extraire la partie domaine (tout après le '@')
        String domaine = email.substring(arobaseIndex + 1);
        // Le domaine doit contenir un point et avoir au moins 2 caracteres
        return domaine.contains(".") && domaine.length() > 1;
    }

    /**
     * Valide un numero de telephone au format francais.
     *
     * Formats acceptes :
     * - 0612345678 (10 chiffres, commence par 0)
     * - 06 12 34 56 78 (espaces acceptes)
     * - 06.12.34.56.78 (points acceptes)
     * - 06-12-34-56-78 (tirets acceptes)
     * - +33612345678 (format international, 12 caracteres)
     * - +33 6 12 34 56 78 (format international avec espaces)
     *
     * @param telephone le numero a valider (peut etre null)
     * @return true si le telephone est un numero francais validé
     *
     * Algorithme :
     * 1. Nettoyer les separateurs (espaces, points, tirets, +)
     * 2. Si le numero commence par +33, le convertir en 0X
     * 3. Verifier qu'il reste exactement 10 chiffres
     * 4. Le premier chiffre doit etre 0
     * 5. Le deuxieme chiffre doit etre entre 1 et 9
     */
    public boolean estTelephoneValide(String telephone) {
        // Protection contre null et chaine vide
        if (telephone == null || telephone.isEmpty()) return false;

        // Nettoyer tous les separateurs : espaces, points, tirets, +
        // La regex [\\s.+-] capture : espace, tabulation, point, +, tiret
        String nettoye = telephone.replaceAll("[\\s.+-]", "");

        // Gerer le format international +33 : 12 caracteres
        // +33612345678 -> on garde "6" et on ajoute "0" devant -> 0612345678
        if (nettoye.length() == 12 && nettoye.startsWith("+33")) {
            nettoye = "0" + nettoye.substring(3); // Enlever "+33", ajouter "0"
        }

        // Après nettoyage, un telephone francais fait exactement 10 chiffres
        if (nettoye.length() != 10) return false;

        // Le premier chiffre doit etre 0 (operateur de sortie France)
        if (nettoye.charAt(0) != '0') return false;

        // Le deuxieme chiffre doit etre entre 1 et 9 (indicatif regional)
        char deuxieme = nettoye.charAt(1);
        if (deuxieme < '1' || deuxieme > '9') return false;

        // Verifier que les 8 derniers caracteres sont bien des chiffres
        for (int i = 2; i < nettoye.length(); i++) {
            if (!Character.isDigit(nettoye.charAt(i))) return false;
        }
        return true;
    }

    /**
     * Calcule un score de robustesse pour un mot de passe.
     *
     * Le score est calcule en additionnant des points selon les criteres
     * suivants, puis plafonne a 100 :
     *
     * | Critere | Points |
     * |---------|--------|
     * | 8+ caracteres | +25 |
     * | 12+ caracteres | +15 |
     * | Au moins une majuscule [A-Z] | +20 |
     * | Au moins une minuscule [a-z] | +15 |
     * | Au moins un chiffre [0-9] | +15 |
     * | Au moins un caractere spécial | +10 |
     *
     * @param motDePasse le mot de passe a evaluer (peut etre null)
     * @return score entre 0 (très faible) et 100 (très fort)
     *
     * Exemples :
     * - "abc" -> 0 (trop court, pas de criteres satisfaits)
     * - "Abcd1234!" -> 70 (8+ chars, majuscule, minuscule, chiffre, spécial)
     * - "MotDePasseTresLong123!" -> 100 (tous les criteres)
     */
    public int scoreMotDePasse(String motDePasse) {
        // Mot de passe null ou vide -> score zero
        if (motDePasse == null || motDePasse.isEmpty()) return 0;

        int score = 0;

        // Criteres de longueur
        if (motDePasse.length() >= 8) score += 25;  // Longueur minimale recommandee
        if (motDePasse.length() >= 12) score += 15; // Longueur de confort

        // Critere de diversite : majuscule
        // La regex .*[A-Z].* signifie : n'importe quoi, puis une majuscule, puis n'importe quoi
        if (motDePasse.matches(".*[A-Z].*")) score += 20;

        // Critere de diversite : minuscule
        if (motDePasse.matches(".*[a-z].*")) score += 15;

        // Critere de diversite : chiffre
        if (motDePasse.matches(".*[0-9].*")) score += 15;

        // Critere de diversite : caractere spécial
        // La liste inclut : !@#$%^&*()_+-=[]{};':"\|,.<>/?
        if (motDePasse.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) score += 10;

        // Plafonner le score a 100 (score maximum possible)
        return Math.min(score, 100);
    }

    /**
     * Verifie si un age est dans la plage validé (18 a 120 ans).
     *
     * @param age l'age a verifier
     * @return true si 18 <= age <= 120
     */
    public boolean estAgeValide(int age) {
        return age >= 18 && age <= 120;
    }

    /**
     * Categorise une personne selon son age.
     *
     * Categories (inspirees des tranches d'age standard) :
     * - MINEUR : < 18 ans
     * - JEUNE_ADULTE : 18-24 ans
     * - ADULTE : 25-59 ans
     * - SENIOR : 60-119 ans
     * - CENTENAIRE : 120 ans et plus
     *
     * @param age l'age a categoriser
     * @return la catégorie sous forme de chaine
     * @throws IllegalArgumentException si l'age est negatif
     *
     * Note : les conditions sont evaluees de haut en bas (if/else if).
     * La première condition satisfaite determine la catégorie.
     */
    public String categorieAge(int age) {
        // Un age negatif n'a pas de sens physique
        if (age < 0) throw new IllegalArgumentException("L'age ne peut pas etre negatif");

        // Evaluation en cascade : la première condition vraie est retenue
        if (age < 18) return "MINEUR";       // 0-17 ans
        if (age < 25) return "JEUNE_ADULTE"; // 18-24 ans
        if (age < 60) return "ADULTE";       // 25-59 ans
        if (age < 120) return "SENIOR";      // 60-119 ans
        return "CENTENAIRE";                 // 120 ans et plus
    }
}
