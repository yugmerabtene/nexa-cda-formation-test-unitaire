package com.nexa.mocking;

/**
 * <h1>EmailService — Interface pour l'envoi d'emails</h1>
 *
 * <p>
 * Dans les tests, on ne veut PAS envoyer de vrais emails.
 * On mock cette interface pour :
 * </p>
 * <ul>
 *   <li>Éviter les effets de bord (pas d'email envoyé)</li>
 *   <li>Vérifier QUE l'email a bien été demandé (via {@code verify})</li>
 *   <li>Simuler des échecs pour tester la gestion d'erreur</li>
 * </ul>
 */
public interface EmailService {

    /**
     * Envoie un email.
     * @return true si l'envoi a réussi
     */
    boolean envoyerEmail(String destinataire, String sujet, String contenu);
}
