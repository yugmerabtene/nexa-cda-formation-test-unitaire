package com.nexa.usermanager.exception;

/**
 * Exception levee lorsqu'une ressource demandee n'est pas trouvee dans le systeme.
 *
 * <p>Exemples d'utilisation :</p>
 * <ul>
 *   <li>Recherche d'un utilisateur par ID inexistant.</li>
 *   <li>Tentative de suppression d'un utilisateur deja supprime.</li>
 * </ul>
 *
 * <p>Cette exception est interceptee par le {@link GlobalExceptionHandler}
 * et convertie en reponse HTTP 404 (Not Found).</p>
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Cree une nouvelle exception avec le message detaille indiquant
     * la ressource non trouvee.
     *
     * @param message le message decrivant la ressource introuvable
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
