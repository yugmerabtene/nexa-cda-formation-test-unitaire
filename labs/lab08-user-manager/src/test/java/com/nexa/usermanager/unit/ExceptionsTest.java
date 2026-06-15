package com.nexa.usermanager.unit;

import com.nexa.usermanager.exception.ResourceNotFoundException;
import com.nexa.usermanager.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour les classes d'exceptions personnalisees.
 *
 * <p>Verifie que {@link ResourceNotFoundException} et {@link BusinessException}
 * heritent correctement de {@link RuntimeException} et conservent le message
 * fourni a la construction.</p>
 */
@DisplayName("Tests unitaires : Exceptions personnalisees")
class ExceptionsTest {

    /**
     * Verifie que {@link ResourceNotFoundException} conserve le message
     * et herite de {@link RuntimeException}.
     */
    @Test
    @DisplayName("ResourceNotFoundException porte le message")
    void resourceNotFoundException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User 42 non trouve");
        assertEquals("User 42 non trouve", ex.getMessage());
        assertTrue(ex instanceof RuntimeException);
    }

    /**
     * Verifie que {@link BusinessException} conserve le message
     * et herite de {@link RuntimeException}.
     */
    @Test
    @DisplayName("BusinessException porte le message")
    void businessException() {
        BusinessException ex = new BusinessException("Operation non autorisee");
        assertEquals("Operation non autorisee", ex.getMessage());
        assertTrue(ex instanceof RuntimeException);
    }
}
