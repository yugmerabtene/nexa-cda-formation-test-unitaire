package com.nexa.mocking;

/**
 * Interface du service d'envoi d'emails.
 *
 * Cette interface sera mockee dans les tests unitaires de UserService
 * pour eviter d'envoyer de vrais emails pendant les tests.
 *
 * Pourquoi une interface ?
 * - Elle definit un contrat sans implementation
 * - Un mock Mockito cree une implementation automatiquement
 * - En production, une vraie implementation (SMTP, SendGrid, etc.) sera injectee
 *
 * @see UserService
 * @see UserServiceTest
 */
public interface EmailService {

    /**
     * Envoie un email a un destinataire.
     *
     * @param destinataire l'adresse email du destinataire
     * @param sujet le sujet de l'email
     * @param contenu le corps de l'email
     * @return true si l'envoi a reussi, false sinon
     */
    boolean envoyerEmail(String destinataire, String sujet, String contenu);
}
