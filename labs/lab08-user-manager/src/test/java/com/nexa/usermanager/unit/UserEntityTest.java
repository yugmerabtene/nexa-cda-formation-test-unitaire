package com.nexa.usermanager.unit;

import com.nexa.usermanager.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests unitaires : Entité User")
class UserEntityTest {

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("Constructeur paramétré initialise correctement")
        void constructeurParametre() {
            User user = new User("Martin", "Paul", "paul@test.com", "password", User.Role.ADMIN);
            assertEquals("Martin", user.getNom());
            assertEquals("Paul", user.getPrenom());
            assertEquals("paul@test.com", user.getEmail());
            assertEquals("password", user.getPassword());
            assertEquals(User.Role.ADMIN, user.getRole());
            assertTrue(user.isActif());
            assertNotNull(user.getDateCreation());
        }

        @Test
        @DisplayName("Le constructeur par défaut crée un objet avec rôle USER")
        void constructeurDefaut() {
            User user = new User();
            assertEquals(User.Role.USER, user.getRole());
            assertTrue(user.isActif());
        }
    }

    @Nested
    @DisplayName("Setters / Getters")
    class Accesseurs {

        @Test
        @DisplayName("Tous les setters fonctionnent")
        void tousLesSetters() {
            User user = new User();
            user.setId(42L);
            user.setNom("Nouveau");
            user.setPrenom("Utilisateur");
            user.setEmail("new@test.com");
            user.setPassword("newpass");
            user.setRole(User.Role.ADMIN);
            user.setActif(false);
            LocalDateTime now = LocalDateTime.now();
            user.setDateCreation(now);
            user.setDateModification(now);

            assertEquals(42L, user.getId());
            assertEquals("Nouveau", user.getNom());
            assertEquals("Utilisateur", user.getPrenom());
            assertEquals("new@test.com", user.getEmail());
            assertEquals("newpass", user.getPassword());
            assertEquals(User.Role.ADMIN, user.getRole());
            assertFalse(user.isActif());
            assertEquals(now, user.getDateCreation());
            assertEquals(now, user.getDateModification());
        }
    }

    @Nested
    @DisplayName("Enum Role")
    class RoleEnum {

        @Test
        @DisplayName("Role.USER et Role.ADMIN existent")
        void rolesDisponibles() {
            assertEquals("USER", User.Role.USER.name());
            assertEquals("ADMIN", User.Role.ADMIN.name());
        }

        @Test
        @DisplayName("ValueOf fonctionne")
        void valueOf() {
            assertEquals(User.Role.ADMIN, User.Role.valueOf("ADMIN"));
            assertEquals(User.Role.USER, User.Role.valueOf("USER"));
        }
    }
}
