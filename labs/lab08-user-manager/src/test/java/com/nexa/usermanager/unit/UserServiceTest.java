package com.nexa.usermanager.unit;

import com.nexa.usermanager.entity.User;
import com.nexa.usermanager.exception.ResourceNotFoundException;
import com.nexa.usermanager.repository.UserRepository;
import com.nexa.usermanager.service.UserService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour le service metier {@link UserService}.
 *
 * <p>Cette classe utilisé Mockito pour mocker le repository et l'encodeur
 * de mot de passe, et teste toutes les méthodes du service :</p>
 * <ul>
 *   <li><b>Creation</b> : succès, échec si email existant, hachage du mot de passe.</li>
 *   <li><b>Lecture</b> : par ID, par email, liste complète, pagination, recherche, filtrage.</li>
 *   <li><b>Mise a jour</b> : modification des champs, gestion du mot de passe vide.</li>
 *   <li><b>Suppression</b> : suppression reussie, exception si ID inexistant, desactivation.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires : UserService")
class UserServiceTest {

    /** Mock du repository utilisateur. */
    @Mock private UserRepository repo;

    /** Mock de l'encodeur de mot de passe BCrypt. */
    @Mock private PasswordEncoder encoder;

    /** Service sous test avec les mocks injectes. */
    @InjectMocks private UserService service;

    /** Utilisateur de test reinitialise avant chaque test. */
    private User user;

    /**
     * Initialise un utilisateur de test avec des valeurs par defaut.
     */
    @BeforeEach
    void setUp() {
        user = new User("Dupont", "Jean", "jean@test.com", "rawpass", User.Role.USER);
        user.setId(1L);
    }

    /**
     * Tests regroupes pour les operations de creation d'utilisateur.
     */
    @Nested
    @DisplayName("CRUD - Creation")
    class Creation {

        /**
         * Verifie qu'un utilisateur est créé avec le mot de passe hache
         * et que le repository.save est bien appelé.
         */
        @Test
        @DisplayName("creer : encode le mot de passe et sauvegarde")
        void creerSucces() {
            when(repo.existsByEmail("jean@test.com")).thenReturn(false);
            when(encoder.encode("rawpass")).thenReturn("hashed");
            when(repo.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(1L);
                return u;
            });

            User résultat = service.creer(user);
            assertNotNull(résultat);
            assertEquals("hashed", résultat.getPassword());
            assertEquals(1L, résultat.getId());
            verify(repo).save(any(User.class));
        }

        /**
         * Verifie que la creation échoué avec une RuntimeException
         * si l'email est déjà utilisé, et que save n'est jamais appelé.
         */
        @Test
        @DisplayName("creer : échoué si l'email existe déjà")
        void creerEmailExistant() {
            when(repo.existsByEmail("jean@test.com")).thenReturn(true);
            assertThrows(RuntimeException.class, () -> service.creer(user));
            verify(repo, never()).save(any());
        }
    }

    /**
     * Tests regroupes pour les operations de lecture.
     */
    @Nested
    @DisplayName("CRUD - Lecture")
    class Lecture {

        /**
         * Verifie la recherche par ID avec succès.
         */
        @Test
        @DisplayName("trouverParId : retourne l'utilisateur")
        void trouverParId() {
            when(repo.findById(1L)).thenReturn(Optional.of(user));
            assertEquals("Dupont", service.trouverParId(1L).getNom());
        }

        /**
         * Verifie que la recherche par ID inexistant lève ResourceNotFoundException.
         */
        @Test
        @DisplayName("trouverParId : lève ResourceNotFoundException")
        void trouverParIdInexistant() {
            when(repo.findById(99L)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> service.trouverParId(99L));
        }

        /**
         * Verifie que listerTous retourne tous les utilisateurs.
         */
        @Test
        @DisplayName("listerTous : retourne la liste")
        void listerTous() {
            when(repo.findAll()).thenReturn(List.of(user));
            assertEquals(1, service.listerTous().size());
        }

        /**
         * Verifie que listerPagine retourne une page d'utilisateurs.
         */
        @Test
        @DisplayName("listerPagine : retourne une page")
        void listerPagine() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> page = new PageImpl<>(List.of(user), pageable, 1);
            when(repo.findAll(pageable)).thenReturn(page);
            Page<User> result = service.listerPagine(pageable);
            assertEquals(1, result.getTotalElements());
        }

        /**
         * Verifie la recherche par email avec succès.
         */
        @Test
        @DisplayName("trouverParEmail : retourne l'utilisateur")
        void trouverParEmail() {
            when(repo.findByEmail("jean@test.com")).thenReturn(Optional.of(user));
            assertTrue(service.trouverParEmail("jean@test.com").isPresent());
        }

        /**
         * Verifie la recherche par nom partiel.
         */
        @Test
        @DisplayName("rechercherParNom : retourne les resultats")
        void rechercherParNom() {
            when(repo.findByNomContainingIgnoreCase("dup")).thenReturn(List.of(user));
            assertEquals(1, service.rechercherParNom("dup").size());
        }

        /**
         * Verifie le filtrage des utilisateurs actifs.
         */
        @Test
        @DisplayName("listerActifs : retourne les utilisateurs actifs")
        void listerActifs() {
            when(repo.findByActif(true)).thenReturn(List.of(user));
            assertEquals(1, service.listerActifs().size());
        }

        /**
         * Verifie le comptage par role.
         */
        @Test
        @DisplayName("compterParRole : retourne le compte")
        void compterParRole() {
            when(repo.countByRole(User.Role.USER)).thenReturn(5L);
            assertEquals(5L, service.compterParRole(User.Role.USER));
        }
    }

    /**
     * Tests regroupes pour les operations de mise a jour.
     */
    @Nested
    @DisplayName("CRUD - Mise a jour")
    class MiseAJour {

        /**
         * Verifie que la mise a jour modifie correctement tous les champs.
         */
        @Test
        @DisplayName("mettreAJour : modifie les champs")
        void mettreAJour() {
            when(repo.findById(1L)).thenReturn(Optional.of(user));
            when(repo.save(any(User.class))).thenReturn(user);

            User update = new User("Nouveau", "Nom", "new@test.com", "newpass", User.Role.ADMIN);
            User résultat = service.mettreAJour(1L, update);

            assertEquals("Nouveau", résultat.getNom());
            assertEquals("Nom", résultat.getPrenom());
            assertEquals(User.Role.ADMIN, résultat.getRole());
            verify(repo).save(any(User.class));
        }

        /**
         * Verifie que le mot de passe n'est pas modifie si la nouvelle
         * valeur est vide ou nulle (mise a jour partielle).
         */
        @Test
        @DisplayName("mettreAJour : ne change pas le mot de passe si null/vide")
        void mettreAJourSansMotDePasse() {
            when(repo.findById(1L)).thenReturn(Optional.of(user));
            when(repo.save(any(User.class))).thenReturn(user);

            User update = new User("Nouveau", "Nom", "new@test.com", "", User.Role.USER);
            service.mettreAJour(1L, update);

            assertEquals("rawpass", user.getPassword(),
                "Le mot de passe ne doit pas etre modifie si la nouvelle valeur est vide");
        }
    }

    /**
     * Tests regroupes pour les operations de suppression et desactivation.
     */
    @Nested
    @DisplayName("CRUD - Suppression et desactivation")
    class Suppression {

        /**
         * Verifie la suppression d'un utilisateur existant.
         */
        @Test
        @DisplayName("supprimer : supprime si l'utilisateur existe")
        void supprimer() {
            when(repo.existsById(1L)).thenReturn(true);
            service.supprimer(1L);
            verify(repo).deleteById(1L);
        }

        /**
         * Verifie que la suppression lève ResourceNotFoundException si l'ID est inexistant.
         */
        @Test
        @DisplayName("supprimer : lève exception si inexistant")
        void supprimerInexistant() {
            when(repo.existsById(99L)).thenReturn(false);
            assertThrows(ResourceNotFoundException.class, () -> service.supprimer(99L));
        }

        /**
         * Verifie que la desactivation passe le statut actif a false
         * et sauvegarde l'utilisateur.
         */
        @Test
        @DisplayName("désactiver : désactivé l'utilisateur")
        void désactiver() {
            when(repo.findById(1L)).thenReturn(Optional.of(user));
            service.désactiver(1L);
            assertFalse(user.isActif());
            verify(repo).save(user);
        }
    }
}
