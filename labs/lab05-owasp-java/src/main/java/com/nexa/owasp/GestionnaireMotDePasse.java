package com.nexa.owasp;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Gestionnaire de mots de passe illustrant les bonnes et mauvaises pratiques
 * de hachage selon les recommandations OWASP.
 *
 * <p>Deux approches sont presentees :</p>
 * <ul>
 *   <li>Hachage vulnerable : SHA-256 sans sel, deterministe, sensible aux rainbow tables.</li>
 *   <li>Hachage securise : SHA-256 avec sel aleatoire de 16 octets, chaque hash est unique.</li>
 * </ul>
 *
 * <p>Le format de stockage securise est {@code selBase64:hashBase64}, permettant
 * de retrouver le sel lors de la verification.</p>
 */
public class GestionnaireMotDePasse {

    /**
     * Hache un mot de passe avec SHA-256 sans sel (version vulnerable).
     *
     * <p>Ce hachage est deterministe : deux appels avec le meme mot de passe
     * produisent le meme resultat. Un attaquant peut utiliser des tables
     * arc-en-ciel (rainbow tables) pour inverser le hash.</p>
     *
     * @param motDePasse le mot de passe en clair a hacher
     * @return le hash SHA-256 encode en Base64
     * @throws RuntimeException si l'algorithme SHA-256 n'est pas disponible
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
     * Hache un mot de passe avec SHA-256 et un sel aleatoire (version securisee).
     *
     * <p>Un sel de 16 octets est genere via {@link SecureRandom}. Le sel est
     * d'abord injecte dans le digest, puis le mot de passe est hache. Le resultat
     * est au format {@code selBase64:hashBase64}.</p>
     *
     * <p>Chaque appel produit un hash different meme pour le meme mot de passe,
     * rendant les rainbow tables inoperantes.</p>
     *
     * @param motDePasse le mot de passe en clair a hacher
     * @return une chaine au format {@code selBase64:hashBase64}
     * @throws RuntimeException si l'algorithme SHA-256 n'est pas disponible
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
     * Verifie un mot de passe contre un hash stocke au format {@code sel:hash}.
     *
     * <p>La methode decode le sel depuis la partie gauche du hash stocke,
     * recalcule le hash du mot de passe fourni avec ce meme sel, puis compare
     * les deux hashs.</p>
     *
     * <p>Retourne {@code false} si le format est invalide, si le sel est
     * malforme, ou si l'algorithme n'est pas disponible.</p>
     *
     * @param motDePasse le mot de passe en clair a verifier
     * @param hashStocke le hash stocke au format {@code selBase64:hashBase64}
     * @return {@code true} si le mot de passe correspond, {@code false} sinon
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
