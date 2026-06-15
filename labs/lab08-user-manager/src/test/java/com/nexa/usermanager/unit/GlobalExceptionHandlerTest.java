package com.nexa.usermanager.unit;

import com.nexa.usermanager.dto.ErrorResponse;
import com.nexa.usermanager.exception.BusinessException;
import com.nexa.usermanager.exception.GlobalExceptionHandler;
import com.nexa.usermanager.exception.ResourceNotFoundException;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour le {@link GlobalExceptionHandler}.
 *
 * <p>Verifie que chaque type d'exception est correctement converti
 * en réponse HTTP avec le bon code de statut et le bon message :</p>
 * <ul>
 *   <li>{@link ResourceNotFoundException} -> HTTP 404</li>
 *   <li>{@link BusinessException}         -> HTTP 409</li>
 *   <li>{@link IllegalArgumentException}  -> HTTP 400</li>
 * </ul>
 */
@DisplayName("Tests unitaires : GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    /** Instance du gestionnaire d'exceptions sous test. */
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    /**
     * Verifie qu'une ResourceNotFoundException produit une réponse 404.
     */
    @Test
    @DisplayName("ResourceNotFoundException -> 404")
    void handleNotFound() {
        var ex = new ResourceNotFoundException("Utilisateur 42 non trouve");
        ResponseEntity<ErrorResponse> response = handler.handleNotFound(ex);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("Utilisateur 42 non trouve", response.getBody().getDetail());
    }

    /**
     * Verifie qu'une BusinessException produit une réponse 409 Conflict.
     */
    @Test
    @DisplayName("BusinessException -> 409")
    void handleBusiness() {
        var ex = new BusinessException("Email déjà utilisé");
        ResponseEntity<ErrorResponse> response = handler.handleBusiness(ex);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Email déjà utilisé", response.getBody().getDetail());
    }

    /**
     * Verifie qu'une IllegalArgumentException produit une réponse 400 Bad Request.
     */
    @Test
    @DisplayName("IllegalArgumentException -> 400")
    void handleIllegalArgument() {
        var ex = new IllegalArgumentException("Argument invalide");
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Argument invalide", response.getBody().getDetail());
    }
}
