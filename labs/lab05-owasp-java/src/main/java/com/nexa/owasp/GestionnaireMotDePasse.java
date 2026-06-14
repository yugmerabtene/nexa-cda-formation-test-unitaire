package com.nexa.owasp;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class GestionnaireMotDePasse {

    public String hacherVulnerable(String motDePasse) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(motDePasse.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

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
