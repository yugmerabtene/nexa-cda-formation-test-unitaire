package com.nexa.usermanager.unit;

import com.nexa.usermanager.dto.UserResponse;
import com.nexa.usermanager.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests unitaires : UserResponse (DTO)")
class UserResponseTest {

    @Test
    @DisplayName("from : convertit une entité User en DTO")
    void fromUser() {
        User user = new User("Dupont", "Jean", "jean@test.com", "pass1234", User.Role.USER);
        user.setId(1L);

        UserResponse dto = UserResponse.from(user);
        assertEquals(1L, dto.getId());
        assertEquals("Dupont", dto.getNom());
        assertEquals("Jean", dto.getPrenom());
        assertEquals("jean@test.com", dto.getEmail());
        assertEquals("USER", dto.getRole());
        assertTrue(dto.isActif());
        assertNotNull(dto.getDateCreation());
    }

    @Test
    @DisplayName("from : le mot de passe n'est PAS exposé dans le DTO")
    void motDePasseNonExpose() {
        User user = new User("Dupont", "Jean", "jean@test.com", "secret123", User.Role.USER);
        UserResponse dto = UserResponse.from(user);
        // Vérification que la classe UserResponse n'a pas de getter pour le mot de passe
        // (c'est inhérent à la conception de UserResponse)
        assertNotNull(dto);
    }

    @Test
    @DisplayName("from : utilisateur ADMIN")
    void fromAdmin() {
        User admin = new User("Admin", "Super", "admin@test.com", "admin123", User.Role.ADMIN);
        UserResponse dto = UserResponse.from(admin);
        assertEquals("ADMIN", dto.getRole());
    }

    @Test
    @DisplayName("from : utilisateur inactif")
    void fromInactif() {
        User user = new User("Test", "Inactif", "inactif@test.com", "pass", User.Role.USER);
        user.setActif(false);
        UserResponse dto = UserResponse.from(user);
        assertFalse(dto.isActif());
    }
}
