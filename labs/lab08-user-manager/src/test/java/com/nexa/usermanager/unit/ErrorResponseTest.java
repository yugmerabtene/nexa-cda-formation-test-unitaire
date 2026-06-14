package com.nexa.usermanager.unit;

import com.nexa.usermanager.dto.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests unitaires : ErrorResponse (RFC 7807)")
class ErrorResponseTest {

    @Test
    @DisplayName("Construction de base")
    void construction() {
        ErrorResponse error = new ErrorResponse(404, "Not Found", "L'utilisateur 42 n'existe pas");
        assertEquals(404, error.getStatus());
        assertEquals("Not Found", error.getTitle());
        assertEquals("L'utilisateur 42 n'existe pas", error.getDetail());
        assertNull(error.getErrors());
    }

    @Test
    @DisplayName("Construction avec erreurs de validation")
    void constructionAvecErreurs() {
        Map<String, String> validationErrors = Map.of(
            "email", "Email invalide",
            "nom", "Le nom est obligatoire"
        );
        ErrorResponse error = new ErrorResponse(400, "Bad Request",
            "Erreur de validation", validationErrors);

        assertEquals(400, error.getStatus());
        assertNotNull(error.getErrors());
        assertEquals(2, error.getErrors().size());
        assertEquals("Email invalide", error.getErrors().get("email"));
    }

    @Test
    @DisplayName("Setters fonctionnent")
    void setters() {
        ErrorResponse error = new ErrorResponse(0, "", "");
        error.setStatus(500);
        error.setTitle("Internal Server Error");
        error.setDetail("Erreur inattendue");
        error.setErrors(Map.of("champ", "message"));

        assertEquals(500, error.getStatus());
        assertEquals("Internal Server Error", error.getTitle());
        assertEquals("Erreur inattendue", error.getDetail());
        assertEquals(1, error.getErrors().size());
    }
}
