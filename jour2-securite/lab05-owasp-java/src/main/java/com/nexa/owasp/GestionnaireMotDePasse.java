package com.nexa.owasp;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * <h1>GestionnaireMotDePasse — Bonnes pratiques de hachage</h1>
 *
 * <h2>À NE PAS FAIRE</h2>
 * <ul>
 *   <li>Stocker les mots de passe en clair</li>
 *   <li>Utiliser MD5 ou SHA-1 (cassés, trop rapides)</li>
 *   <li>Hacher sans sel (vulnérable aux rainbow tables)</li>
 * </ul>
 *
 * <h2>BONNES PRATIQUES</h2>
 * <ul>
 *   <li>Utiliser BCrypt, Argon2 ou PBKDF2</li>
 *   <li>Toujours utiliser un sel unique par mot de passe</li>
 *   <li>Appliquer plusieurs itérations pour ralentir les attaques</li>
 * </ul>
 */
public class GestionnaireMotDePasse {

    /**
     * ⚠️ VULNÉRABLE : Hachage SHA-256 sans sel.
     * Vulnérable aux attaques par dictionnaire et rainbow tables.
     */
    public String hacherVulnerable(String motDePasse) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(motDePasse.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * ✅ SÉCURISÉ : Hachage SHA-256 avec sel aléatoire.
     * Format de retour : base64(sel):base64(hash)
     */
    public String hacherAvecSel(String motDePasse) {
        try {
            SecureRandom random = new SecureRandom();
            byte[] sel = new byte[16];
            random.nextBytes(sel);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(sel);
            byte[] hash = digest.digest(motDePasse.getBytes());

            return Base64.getEncoder().encodeToString(sel) + ":" + Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Vérifie un mot de passe contre un hash salé.
     */
    public boolean verifierMotDePasse(String motDePasse, String hashStocke) {
        try {
            String[] parties = hashStocke.split(":");
            if (parties.length != 2) return false;

            byte[] sel = Base64.getDecoder().decode(parties[0]);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(sel);
            byte[] hashCalcule = digest.digest(motDePasse.getBytes());
            String hashCalculeB64 = Base64.getEncoder().encodeToString(hashCalcule);

            return hashCalculeB64.equals(parties[1]);
        } catch (NoSuchAlgorithmException | IllegalArgumentException e) {
            return false;
        }
    }
}
