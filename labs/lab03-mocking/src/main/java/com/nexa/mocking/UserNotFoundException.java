package com.nexa.mocking;

/**
 * Exception levee lorsqu'un utilisateur est introuvable.
 *
 * Herite de RuntimeException pour eviter de devoir declarer
 * throws dans toutes les signatures de methodes (non-verifiee).
 *
 * Utilisee dans UserService.trouverParId() quand findById retourne null.
 *
 * @see UserService
 */
public class UserNotFoundException extends RuntimeException {

    /**
     * Cree une exception avec un message explicatif.
     *
     * @param message le message d'erreur (ex: "Utilisateur introuvable : id=42")
     */
    public UserNotFoundException(String message) {
        super(message);
    }
}
