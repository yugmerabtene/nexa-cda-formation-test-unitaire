package com.nexa.usermanager.exception;

/**
 * Exception metier levee en cas de conflit ou de violation des regles metier.
 *
 * <p>Exemples d'utilisation :</p>
 * <ul>
 *   <li>Tentative de creation d'un utilisateur avec un email déjà existant.</li>
 *   <li>Operation non autorisee selon l'état courant d'une ressource.</li>
 * </ul>
 *
 * <p>Cette exception est interceptee par le {@link GlobalExceptionHandler}
 * et convertie en réponse HTTP 409 (Conflict).</p>
 */
public class BusinessException extends RuntimeException {

    /**
     * Cree une nouvelle exception metier avec le message detaille.
     *
     * @param message le message decrivant la nature du conflit metier
     */
    public BusinessException(String message) {
        super(message);
    }
}
