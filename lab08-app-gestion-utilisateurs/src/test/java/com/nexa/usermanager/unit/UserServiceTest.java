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

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires : UserService")
class UserServiceTest {

    @Mock private UserRepository repo;
    @Mock private PasswordEncoder encoder;
    @InjectMocks private UserService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("Dupont", "Jean", "jean@test.com", "rawpass", User.Role.USER);
        user.setId(1L);
    }

    @Nested
    @DisplayName("CRUD - Création")
    class Creation {

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

            User resultat = service.creer(user);
            assertNotNull(resultat);
            assertEquals("hashed", resultat.getPassword());
            assertEquals(1L, resultat.getId());
            verify(repo).save(any(User.class));
        }

        @Test
        @DisplayName("creer : échoue si l'email existe déjà")
        void creerEmailExistant() {
            when(repo.existsByEmail("jean@test.com")).thenReturn(true);
            assertThrows(RuntimeException.class, () -> service.creer(user));
            verify(repo, never()).save(any());
        }
    }

    @Nested
    @DisplayName("CRUD - Lecture")
    class Lecture {

        @Test
        @DisplayName("trouverParId : retourne l'utilisateur")
        void trouverParId() {
            when(repo.findById(1L)).thenReturn(Optional.of(user));
            assertEquals("Dupont", service.trouverParId(1L).getNom());
        }

        @Test
        @DisplayName("trouverParId : lève ResourceNotFoundException")
        void trouverParIdInexistant() {
            when(repo.findById(99L)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> service.trouverParId(99L));
        }

        @Test
        @DisplayName("listerTous : retourne la liste")
        void listerTous() {
            when(repo.findAll()).thenReturn(List.of(user));
            assertEquals(1, service.listerTous().size());
        }

        @Test
        @DisplayName("listerPagine : retourne une page")
        void listerPagine() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> page = new PageImpl<>(List.of(user), pageable, 1);
            when(repo.findAll(pageable)).thenReturn(page);
            Page<User> result = service.listerPagine(pageable);
            assertEquals(1, result.getTotalElements());
        }

        @Test
        @DisplayName("trouverParEmail : retourne l'utilisateur")
        void trouverParEmail() {
            when(repo.findByEmail("jean@test.com")).thenReturn(Optional.of(user));
            assertTrue(service.trouverParEmail("jean@test.com").isPresent());
        }

        @Test
        @DisplayName("rechercherParNom : retourne les résultats")
        void rechercherParNom() {
            when(repo.findByNomContainingIgnoreCase("dup")).thenReturn(List.of(user));
            assertEquals(1, service.rechercherParNom("dup").size());
        }

        @Test
        @DisplayName("listerActifs : retourne les utilisateurs actifs")
        void listerActifs() {
            when(repo.findByActif(true)).thenReturn(List.of(user));
            assertEquals(1, service.listerActifs().size());
        }

        @Test
        @DisplayName("compterParRole : retourne le compte")
        void compterParRole() {
            when(repo.countByRole(User.Role.USER)).thenReturn(5L);
            assertEquals(5L, service.compterParRole(User.Role.USER));
        }
    }

    @Nested
    @DisplayName("CRUD - Mise à jour")
    class MiseAJour {

        @Test
        @DisplayName("mettreAJour : modifie les champs")
        void mettreAJour() {
            when(repo.findById(1L)).thenReturn(Optional.of(user));
            when(repo.save(any(User.class))).thenReturn(user);

            User update = new User("Nouveau", "Nom", "new@test.com", "newpass", User.Role.ADMIN);
            User resultat = service.mettreAJour(1L, update);

            assertEquals("Nouveau", resultat.getNom());
            assertEquals("Nom", resultat.getPrenom());
            assertEquals(User.Role.ADMIN, resultat.getRole());
            verify(repo).save(any(User.class));
        }

        @Test
        @DisplayName("mettreAJour : ne change pas le mot de passe si null/vide")
        void mettreAJourSansMotDePasse() {
            when(repo.findById(1L)).thenReturn(Optional.of(user));
            when(repo.save(any(User.class))).thenReturn(user);

            User update = new User("Nouveau", "Nom", "new@test.com", "", User.Role.USER);
            service.mettreAJour(1L, update);

            assertEquals("rawpass", user.getPassword(),
                "Le mot de passe ne doit pas être modifié si la nouvelle valeur est vide");
        }
    }

    @Nested
    @DisplayName("CRUD - Suppression et désactivation")
    class Suppression {

        @Test
        @DisplayName("supprimer : supprime si l'utilisateur existe")
        void supprimer() {
            when(repo.existsById(1L)).thenReturn(true);
            service.supprimer(1L);
            verify(repo).deleteById(1L);
        }

        @Test
        @DisplayName("supprimer : lève exception si inexistant")
        void supprimerInexistant() {
            when(repo.existsById(99L)).thenReturn(false);
            assertThrows(ResourceNotFoundException.class, () -> service.supprimer(99L));
        }

        @Test
        @DisplayName("desactiver : désactive l'utilisateur")
        void desactiver() {
            when(repo.findById(1L)).thenReturn(Optional.of(user));
            service.desactiver(1L);
            assertFalse(user.isActif());
            verify(repo).save(user);
        }
    }
}
