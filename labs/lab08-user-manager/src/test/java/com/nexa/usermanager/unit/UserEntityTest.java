package com.nexa.usermanager.unit;

import com.nexa.usermanager.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour l'entité JPA {@link User}.
 *
 * <p>Cette classe vérifié le bon fonctionnement de l'entité User :</p>
 * <ul>
 *   <li>Les constructeurs (par defaut et paramètre).</li>
 *   <li>Les valeurs par defaut (role USER, compte actif, date de creation).</li>
 *   <li>Tous les setters et getters.</li>
 *   <li>L'enum {@code Role} et ses valeurs.</li>
 * </ul>
 */
@DisplayName("Tests unitaires : Entite User")
class UserEntityTest {

    /**
     * Tests regroupes pour les constructeurs de l'entité User.
     */
    @Nested
    @DisplayName("Construction")
    class Construction {

        /**
         * Verifie que le constructeur paramètre initialise correctement tous les champs.
         */
        @Test
        @DisplayName("Constructeur paramètre initialise correctement")
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

        /**
         * Verifie que le constructeur par defaut créé un utilisateur avec
         * le role USER et le compte actif.
         */
        @Test
        @DisplayName("Le constructeur par defaut créé un objet avec role USER")
        void constructeurDefaut() {
            User user = new User();
            assertEquals(User.Role.USER, user.getRole());
            assertTrue(user.isActif());
        }
    }

    /**
     * Tests regroupes pour les accesseurs (setters et getters).
     */
    @Nested
    @DisplayName("Setters / Getters")
    class Accesseurs {

        /**
         * Verifie que tous les setters modifient bien les valeurs
         * et que les getters correspondants les retournent correctement.
         */
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

    /**
     * Tests regroupes pour l'enum Role.
     */
    @Nested
    @DisplayName("Enum Role")
    class RoleEnum {

        /**
         * Verifie les noms des valeurs de l'enum Role.
         */
        @Test
        @DisplayName("Role.USER et Role.ADMIN existent")
        void rolesDisponibles() {
            assertEquals("USER", User.Role.USER.name());
            assertEquals("ADMIN", User.Role.ADMIN.name());
        }

        /**
         * Verifie que valueOf fonctionne pour les deux roles.
         */
        @Test
        @DisplayName("ValueOf fonctionne")
        void valueOf() {
            assertEquals(User.Role.ADMIN, User.Role.valueOf("ADMIN"));
            assertEquals(User.Role.USER, User.Role.valueOf("USER"));
        }
    }
}
