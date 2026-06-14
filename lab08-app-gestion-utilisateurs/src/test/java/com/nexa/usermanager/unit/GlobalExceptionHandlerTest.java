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

@DisplayName("Tests unitaires : GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("ResourceNotFoundException → 404")
    void handleNotFound() {
        var ex = new ResourceNotFoundException("Utilisateur 42 non trouvé");
        ResponseEntity<ErrorResponse> response = handler.handleNotFound(ex);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("Utilisateur 42 non trouvé", response.getBody().getDetail());
    }

    @Test
    @DisplayName("BusinessException → 409")
    void handleBusiness() {
        var ex = new BusinessException("Email déjà utilisé");
        ResponseEntity<ErrorResponse> response = handler.handleBusiness(ex);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Email déjà utilisé", response.getBody().getDetail());
    }

    @Test
    @DisplayName("IllegalArgumentException → 400")
    void handleIllegalArgument() {
        var ex = new IllegalArgumentException("Argument invalide");
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Argument invalide", response.getBody().getDetail());
    }
}
