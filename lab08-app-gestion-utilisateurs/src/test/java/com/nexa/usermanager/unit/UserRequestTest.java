package com.nexa.usermanager.unit;

import com.nexa.usermanager.dto.UserRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests unitaires : UserRequest (DTO d'entrée)")
class UserRequestTest {

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
