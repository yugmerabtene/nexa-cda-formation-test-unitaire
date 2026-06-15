package com.nexa.usermanager.exception;

import com.nexa.usermanager.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire global d'exceptions pour l'API REST.
 *
 * <p>Cette classe, annotee {@code @RestControllerAdvice}, intercepte les exceptions
 * levees par les controleurs et les convertit en reponses HTTP structurees au
 * format RFC 7807 (Problem Details).</p>
 *
 * <p>Exceptions gerees :</p>
 * <ul>
 *   <li>{@link ResourceNotFoundException}   -> HTTP 404 Not Found</li>
 *   <li>{@link BusinessException}           -> HTTP 409 Conflict</li>
 *   <li>{@link MethodArgumentNotValidException} -> HTTP 400 Bad Request (avec details par champ)</li>
 *   <li>{@link IllegalArgumentException}    -> HTTP 400 Bad Request</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Gere les exceptions de type {@link ResourceNotFoundException}.
     *
     * <p>Retourne une réponse HTTP 404 avec un corps structure indiquant
     * la ressource non trouvee et le message detaille.</p>
     *
     * @param e l'exception levee
     * @return une réponse HTTP 404 avec le corps d'erreur structure
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(404, "Ressource non trouvee", e.getMessage()));
    }

    /**
     * Gere les exceptions de type {@link BusinessException}.
     *
     * <p>Retourne une réponse HTTP 409 avec un corps structure indiquant
     * le conflit metier et le message detaille.</p>
     *
     * @param e l'exception levee
     * @return une réponse HTTP 409 avec le corps d'erreur structure
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(409, "Conflit metier", e.getMessage()));
    }

    /**
     * Gere les exceptions de validation des arguments de méthode.
     *
     * <p>Cette méthode est declenchee lorsque les annotations de validation
     * Jakarta Bean Validation ({@code @NotBlank}, {@code @Size}, {@code @Email}, etc.)
     * detectent des donnees invalides dans le corps d'une requête.</p>
     *
     * <p>Elle construit une map associant chaque champ en erreur a son message
     * de validation, permettant au client d'identifier precisement les champs
     * problematiques.</p>
     *
     * @param e l'exception de validation
     * @return une réponse HTTP 400 avec les erreurs detaillees par champ
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(400, "Erreur de validation", "Un ou plusieurs champs sont invalides", errors));
    }

    /**
     * Gere les exceptions de type {@link IllegalArgumentException}.
     *
     * <p>Retourne une réponse HTTP 400 indiquant qu'un argument fourni
     * est invalide (ex: valeur de role non reconnue).</p>
     *
     * @param e l'exception levee
     * @return une réponse HTTP 400 avec le corps d'erreur structure
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(400, "Argument invalide", e.getMessage()));
    }
}
