package com.nexa.usermanager.unit;

import com.nexa.usermanager.dto.UserRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour le DTO d'entree {@link UserRequest}.
 *
 * <p>Verifie que les setters et getters fonctionnent correctement
 * pour tous les champs du DTO de requete (nom, prenom, email,
 * password, role).</p>
 *
 * <p>Note : la validation des annotations Jakarta Bean Validation
 * est testee indirectement via les tests MVC.</p>
 */
@DisplayName("Tests unitaires : UserRequest (DTO d'entree)")
class UserRequestTest {

    /**
     * Verifie que les setters et getters propagent correctement les valeurs.
     */
    @Test
    @DisplayName("Setters / Getters")
    void settersGetters() {
        UserRequest req = new UserRequest();
        req.setNom("Dupont");
        req.setPrenom("Marie");
        req.setEmail("marie@test.com");
        req.setPassword("password123");
        req.setRole("USER");

        assertEquals("Dupont", req.getNom());
        assertEquals("Marie", req.getPrenom());
        assertEquals("marie@test.com", req.getEmail());
        assertEquals("password123", req.getPassword());
        assertEquals("USER", req.getRole());
    }
}
