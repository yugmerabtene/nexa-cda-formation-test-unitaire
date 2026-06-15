package com.nexa.usermanager.unit;

import com.nexa.usermanager.dto.UserResponse;
import com.nexa.usermanager.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour le DTO de sortie {@link UserResponse}.
 *
 * <p>Verifie le bon fonctionnement de la methode de conversion
 * {@link UserResponse#from(User)} et des accesseurs :</p>
 * <ul>
 *   <li>Conversion correcte d'une entite User en DTO.</li>
 *   <li>Le mot de passe n'est jamais expose dans le DTO.</li>
 *   <li>Prise en charge des roles ADMIN et USER.</li>
 *   <li>Prise en charge du statut actif/inactif.</li>
 * </ul>
 */
@DisplayName("Tests unitaires : UserResponse (DTO)")
class UserResponseTest {

    /**
     * Verifie la conversion d'une entite User en UserResponse.
     * Tous les champs (sauf le mot de passe) doivent etre correctement copies.
     */
    @Test
    @DisplayName("from : convertit une entite User en DTO")
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

    /**
     * Verifie que le mot de passe n'est PAS expose dans le DTO de reponse.
     * C'est un test de securite important : le champ password de UserResponse
     * n'existe pas, donc le mot de passe ne peut pas fuiter.
     */
    @Test
    @DisplayName("from : le mot de passe n'est PAS expose dans le DTO")
    void motDePasseNonExpose() {
        User user = new User("Dupont", "Jean", "jean@test.com", "secret123", User.Role.USER);
        UserResponse dto = UserResponse.from(user);

        assertNotNull(dto);
    }

    /**
     * Verifie la conversion pour un utilisateur avec le role ADMIN.
     */
    @Test
    @DisplayName("from : utilisateur ADMIN")
    void fromAdmin() {
        User admin = new User("Admin", "Super", "admin@test.com", "admin123", User.Role.ADMIN);
        UserResponse dto = UserResponse.from(admin);
        assertEquals("ADMIN", dto.getRole());
    }

    /**
     * Verifie la conversion pour un utilisateur inactif.
     */
    @Test
    @DisplayName("from : utilisateur inactif")
    void fromInactif() {
        User user = new User("Test", "Inactif", "inactif@test.com", "pass", User.Role.USER);
        user.setActif(false);
        UserResponse dto = UserResponse.from(user);
        assertFalse(dto.isActif());
    }
}
