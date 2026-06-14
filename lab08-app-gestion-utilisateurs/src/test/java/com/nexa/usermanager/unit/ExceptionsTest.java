package com.nexa.usermanager.unit;

import com.nexa.usermanager.exception.ResourceNotFoundException;
import com.nexa.usermanager.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests unitaires : Exceptions personnalisées")
class ExceptionsTest {

    @Test
    @DisplayName("ResourceNotFoundException porte le message")
    void resourceNotFoundException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User 42 non trouvé");
        assertEquals("User 42 non trouvé", ex.getMessage());
        assertTrue(ex instanceof RuntimeException);
    }

    @Test
    @DisplayName("BusinessException porte le message")
    void businessException() {
        BusinessException ex = new BusinessException("Opération non autorisée");
        assertEquals("Opération non autorisée", ex.getMessage());
        assertTrue(ex instanceof RuntimeException);
    }
}
