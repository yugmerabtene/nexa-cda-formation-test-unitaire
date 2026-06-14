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

@DataJpaTest
@DisplayName("Tests repository : UserRepository")
class UserRepositoryTest {

    @Autowired private UserRepository repo;
    @Autowired private TestEntityManager em;

    private User admin, user1, user2;

    @BeforeEach
    void setUp() {
        admin = repo.save(new User("Admin", "Super", "admin@nexa.fr", "admin123", User.Role.ADMIN));
        user1 = repo.save(new User("Martin", "Paul", "paul@nexa.fr", "user123", User.Role.USER));
        user2 = repo.save(new User("Dupont", "Marie", "marie@nexa.fr", "user123", User.Role.USER));
    }

    @Test
    @DisplayName("findByEmail : trouve par email")
    void findByEmail() {
        Optional<User> found = repo.findByEmail("admin@nexa.fr");
        assertTrue(found.isPresent());
        assertEquals("Admin", found.get().getNom());
    }

    @Test
    @DisplayName("findByEmail : retourne empty si inexistant")
    void findByEmailInexistant() {
        assertTrue(repo.findByEmail("inconnu@nexa.fr").isEmpty());
    }

    @Test
    @DisplayName("existsByEmail : détecte l'existence")
    void existsByEmail() {
        assertTrue(repo.existsByEmail("paul@nexa.fr"));
        assertFalse(repo.existsByEmail("inconnu@nexa.fr"));
    }

    @Test
    @DisplayName("findByNomContainingIgnoreCase : insensible à la casse")
    void rechercheParNom() {
        assertEquals(1, repo.findByNomContainingIgnoreCase("adm").size());
        assertEquals(1, repo.findByNomContainingIgnoreCase("ADM").size());
        assertEquals(0, repo.findByNomContainingIgnoreCase("inconnu").size());
    }

    @Test
    @DisplayName("findByActif : filtre par statut")
    void findByActif() {
        user1.setActif(false);
        repo.save(user1);

        List<User> actifs = repo.findByActif(true);
        assertEquals(2, actifs.size());
        assertTrue(actifs.stream().allMatch(User::isActif));
    }

    @Test
    @DisplayName("findByRole avec pagination")
    void findByRolePagination() {
        Page<User> page = repo.findByRole(User.Role.USER, PageRequest.of(0, 10));
        assertEquals(2, page.getTotalElements());
        assertEquals("USER", page.getContent().get(0).getRole().name());
    }

    @Test
    @DisplayName("countByRole : compte les utilisateurs par rôle")
    void countByRole() {
        assertEquals(1, repo.countByRole(User.Role.ADMIN));
        assertEquals(2, repo.countByRole(User.Role.USER));
    }

    @Test
    @DisplayName("save : persiste avec ID auto-généré")
    void saveAvecId() {
        User newUser = repo.save(new User("Nouveau", "User", "new@nexa.fr", "pass", User.Role.USER));
        assertNotNull(newUser.getId());
    }

    @Test
    @DisplayName("deleteById : supprime l'utilisateur")
    void deleteById() {
        repo.deleteById(user1.getId());
        em.flush();
        assertFalse(repo.existsById(user1.getId()));
        assertEquals(2, repo.count());
    }

    @Test
    @DisplayName("findAll : retourne tous les utilisateurs")
    void findAll() {
        assertEquals(3, repo.findAll().size());
    }

    @Test
    @DisplayName("findById : retourne l'utilisateur")
    void findById() {
        assertTrue(repo.findById(admin.getId()).isPresent());
        assertEquals("admin@nexa.fr", repo.findById(admin.getId()).get().getEmail());
    }

    @Test
    @DisplayName("La contrainte d'unicité sur l'email est respectée")
    void uniciteEmail() {
        assertThrows(Exception.class, () -> {
            repo.save(new User("Dupont2", "Jean", "admin@nexa.fr", "pass", User.Role.USER));
            em.flush();
        });
    }
}
