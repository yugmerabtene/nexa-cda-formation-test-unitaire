package com.nexa.usermanager.repository;

import com.nexa.usermanager.entity.User;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'integration pour le repository {@link UserRepository}.
 *
 * <p>Cette classe utilise {@code @DataJpaTest} pour tester les interactions
 * avec la base de donnees H2 en memoire. Elle verifie :</p>
 * <ul>
 *   <li>Les methodes de requete derivees (findByEmail, existsByEmail, etc.).</li>
 *   <li>La persistance, la mise a jour et la suppression des entites.</li>
 *   <li>La contrainte d'unicite sur l'email.</li>
 *   <li>La pagination des resultats.</li>
 *   <li>Le comptage par role.</li>
 * </ul>
 */
@DataJpaTest
@DisplayName("Tests repository : UserRepository")
class UserRepositoryTest {

    /** Repository sous test. */
    @Autowired private UserRepository repo;

    /** EntityManager de test pour forcer les flush. */
    @Autowired private TestEntityManager em;

    /** Utilisateur admin de test. */
    private User admin;

    /** Premier utilisateur standard de test. */
    private User user1;

    /** Deuxieme utilisateur standard de test. */
    private User user2;

    /**
     * Initialise 3 utilisateurs en base avant chaque test :
     * un admin et deux utilisateurs standards.
     */
    @BeforeEach
    void setUp() {
        admin = repo.save(new User("Admin", "Super", "admin@nexa.fr", "admin123", User.Role.ADMIN));
        user1 = repo.save(new User("Martin", "Paul", "paul@nexa.fr", "user123", User.Role.USER));
        user2 = repo.save(new User("Dupont", "Marie", "marie@nexa.fr", "user123", User.Role.USER));
    }

    /**
     * Verifie que findByEmail retourne l'utilisateur correspondant a l'email.
     */
    @Test
    @DisplayName("findByEmail : trouve par email")
    void findByEmail() {
        Optional<User> found = repo.findByEmail("admin@nexa.fr");
        assertTrue(found.isPresent());
        assertEquals("Admin", found.get().getNom());
    }

    /**
     * Verifie que findByEmail retourne un Optional vide pour un email inexistant.
     */
    @Test
    @DisplayName("findByEmail : retourne empty si inexistant")
    void findByEmailInexistant() {
        assertTrue(repo.findByEmail("inconnu@nexa.fr").isEmpty());
    }

    /**
     * Verifie que existsByEmail detecte correctement la presence ou l'absence d'un email.
     */
    @Test
    @DisplayName("existsByEmail : detecte l'existence")
    void existsByEmail() {
        assertTrue(repo.existsByEmail("paul@nexa.fr"));
        assertFalse(repo.existsByEmail("inconnu@nexa.fr"));
    }

    /**
     * Verifie que la recherche par nom est insensible a la casse.
     */
    @Test
    @DisplayName("findByNomContainingIgnoreCase : insensible a la casse")
    void rechercheParNom() {
        assertEquals(1, repo.findByNomContainingIgnoreCase("adm").size());
        assertEquals(1, repo.findByNomContainingIgnoreCase("ADM").size());
        assertEquals(0, repo.findByNomContainingIgnoreCase("inconnu").size());
    }

    /**
     * Verifie le filtrage des utilisateurs par statut actif/inactif.
     */
    @Test
    @DisplayName("findByActif : filtre par statut")
    void findByActif() {
        user1.setActif(false);
        repo.save(user1);

        List<User> actifs = repo.findByActif(true);
        assertEquals(2, actifs.size());
        assertTrue(actifs.stream().allMatch(User::isActif));
    }

    /**
     * Verifie la recherche paginee par role.
     */
    @Test
    @DisplayName("findByRole avec pagination")
    void findByRolePagination() {
        Page<User> page = repo.findByRole(User.Role.USER, PageRequest.of(0, 10));
        assertEquals(2, page.getTotalElements());
        assertEquals("USER", page.getContent().get(0).getRole().name());
    }

    /**
     * Verifie le comptage des utilisateurs par role.
     */
    @Test
    @DisplayName("countByRole : compte les utilisateurs par role")
    void countByRole() {
        assertEquals(1, repo.countByRole(User.Role.ADMIN));
        assertEquals(2, repo.countByRole(User.Role.USER));
    }

    /**
     * Verifie que le save persiste correctement et genere un ID.
     */
    @Test
    @DisplayName("save : persiste avec ID auto-genere")
    void saveAvecId() {
        User newUser = repo.save(new User("Nouveau", "User", "new@nexa.fr", "pass", User.Role.USER));
        assertNotNull(newUser.getId());
    }

    /**
     * Verifie la suppression d'un utilisateur par ID.
     */
    @Test
    @DisplayName("deleteById : supprime l'utilisateur")
    void deleteById() {
        repo.deleteById(user1.getId());
        em.flush();
        assertFalse(repo.existsById(user1.getId()));
        assertEquals(2, repo.count());
    }

    /**
     * Verifie que findAll retourne tous les utilisateurs en base.
     */
    @Test
    @DisplayName("findAll : retourne tous les utilisateurs")
    void findAll() {
        assertEquals(3, repo.findAll().size());
    }

    /**
     * Verifie que findById retourne l'utilisateur correspondant.
     */
    @Test
    @DisplayName("findById : retourne l'utilisateur")
    void findById() {
        assertTrue(repo.findById(admin.getId()).isPresent());
        assertEquals("admin@nexa.fr", repo.findById(admin.getId()).get().getEmail());
    }

    /**
     * Verifie que la contrainte d'unicite sur l'email est bien appliquee.
     * Une tentative d'insertion d'un email deja existant doit lever une exception.
     */
    @Test
    @DisplayName("La contrainte d'unicite sur l'email est respectee")
    void uniciteEmail() {
        assertThrows(Exception.class, () -> {
            repo.save(new User("Dupont2", "Jean", "admin@nexa.fr", "pass", User.Role.USER));
            em.flush();
        });
    }
}
