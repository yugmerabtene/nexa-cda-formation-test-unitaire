package com.nexa.mocking;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests du UserService avec Mockito")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("trouverParId — retourne l'utilisateur quand il existe")
    void trouverParId_existant() {

        User userAttendu = new User(1L, "Alice", "alice@example.com", true);
        when(userRepository.findById(1L)).thenReturn(userAttendu);

        User resultat = userService.trouverParId(1L);

        assertNotNull(resultat);
        assertEquals("Alice", resultat.getNom());
        assertEquals("alice@example.com", resultat.getEmail());
    }

    @Test
    @DisplayName("trouverParId — lève UserNotFoundException si l'utilisateur n'existe pas")
    void trouverParId_inexistant() {
        when(userRepository.findById(99L)).thenReturn(null);

        assertThrows(UserNotFoundException.class,
            () -> userService.trouverParId(99L));
    }

    @Test
    @DisplayName("creerUtilisateur — sauvegarde l'utilisateur et envoie un email")
    void creerUtilisateur_succes() {
        when(userRepository.existsByEmail("bob@test.com")).thenReturn(false);
        when(userRepository.save(any(User.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(emailService.envoyerEmail(anyString(), anyString(), anyString()))
            .thenReturn(true);

        User resultat = userService.creerUtilisateur("Bob", "bob@test.com");

        assertNotNull(resultat);
        assertEquals("Bob", resultat.getNom());
        assertTrue(resultat.isActif());

        verify(userRepository).save(any(User.class));
        verify(emailService).envoyerEmail(eq("bob@test.com"), anyString(), anyString());
    }

    @Test
    @DisplayName("creerUtilisateur — échoue si l'email existe déjà")
    void creerUtilisateur_emailExistant() {
        when(userRepository.existsByEmail("existant@test.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
            () -> userService.creerUtilisateur("Eve", "existant@test.com"));

        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).envoyerEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("ArgumentMatchers : any() et eq()")
    void argumentMatchersDemo() {
        when(userRepository.findById(anyLong())).thenReturn(
            new User(1L, "Test", "test@test.com", true));

        User u = userService.trouverParId(42L);
        assertNotNull(u);

        verify(userRepository).findById(eq(42L));
    }

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Test
    @DisplayName("ArgumentCaptor : inspecter l'utilisateur sauvegardé")
    void capturerUtilisateurSauvegarde() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(emailService.envoyerEmail(anyString(), anyString(), anyString()))
            .thenReturn(true);

        userService.creerUtilisateur("Charlie", "charlie@test.com");

        verify(userRepository).save(userCaptor.capture());
        User capture = userCaptor.getValue();

        assertEquals("Charlie", capture.getNom());
        assertEquals("charlie@test.com", capture.getEmail());
        assertTrue(capture.isActif());
        assertNull(capture.getId(), "L'ID doit être null avant la persistance");
    }

    @Test
    @DisplayName("desactiverUtilisateur — gère l'échec d'envoi d'email")
    void desactiverUtilisateur_succes() {
        User user = new User(1L, "Dave", "dave@test.com", true);
        when(userRepository.findById(1L)).thenReturn(user);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(emailService.envoyerEmail(anyString(), anyString(), anyString()))
            .thenReturn(true);

        userService.desactiverUtilisateur(1L);

        assertFalse(user.isActif());
        verify(userRepository).save(user);
        verify(emailService).envoyerEmail(eq("dave@test.com"), anyString(), anyString());
    }

    static class CompteurService {
        public int incrementer() { return 1; }
        public int decrementer() { return -1; }
    }

    @Spy
    private CompteurService compteurSpy;

    @Test
    @DisplayName("@Spy : l'objet réel est utilisé, sauf méthodes stubbées")
    void spyDemo() {
        assertEquals(1, compteurSpy.incrementer(),
            "Par défaut, le spy appelle la vraie méthode");

        doReturn(100).when(compteurSpy).incrementer();

        assertEquals(100, compteurSpy.incrementer(),
            "La méthode stubbée retourne 100");
        assertEquals(-1, compteurSpy.decrementer(),
            "La méthode non stubbée appelle la vraie implémentation");
    }

    @Test
    @DisplayName("verifyNoMoreInteractions : pas d'appels surprise")
    void pasDAutresInteractions() {
        User user = new User(1L, "Eve", "eve@test.com", true);
        when(userRepository.findById(1L)).thenReturn(user);

        userService.trouverParId(1L);

        verify(userRepository).findById(1L);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("thenAnswer : l'utilisateur sauvegardé reçoit un ID")
    void saveRetourneUtilisateurAvecId() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(emailService.envoyerEmail(anyString(), anyString(), anyString())).thenReturn(true);

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(42L);
            return user;
        });

        User resultat = userService.creerUtilisateur("Frank", "frank@test.com");

        assertEquals(42L, resultat.getId(),
            "Le mock simule l'attribution d'un ID par la base de données");
    }

    @Test
    @DisplayName("InOrder : l'email est envoyé APRÈS la sauvegarde")
    void ordreDesAppels() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(emailService.envoyerEmail(anyString(), anyString(), anyString()))
            .thenReturn(true);

        userService.creerUtilisateur("Grace", "grace@test.com");

        InOrder ordre = inOrder(userRepository, emailService);
        ordre.verify(userRepository).save(any(User.class));
        ordre.verify(emailService).envoyerEmail(anyString(), anyString(), anyString());
    }
}
