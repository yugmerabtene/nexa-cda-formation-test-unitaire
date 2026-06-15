package com.nexa.mocking;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du UserService avec Mockito.
 *
 * Ce fichier illustre les concepts fondamentaux du mocking :
 *
 * @ExtendWith(MockitoExtension.class) : active les annotations Mockito
 * @Mock            : créé un mock (simulacre) de l'interface
 * @InjectMocks     : injecte les mocks dans le SUT (System Under Test)
 * @Spy             : créé un mock partiel (vraie implementation + stubbing)
 * @Captor          : capture les arguments passes a un mock
 *
 * Stubbing (configuration du comportement) :
 *   when(mock.méthode()).thenReturn(valeur)
 *   doReturn(valeur).when(mock).méthode()
 *
 * Verification (controle des interactions) :
 *   verify(mock).méthode()
 *   verify(mock, times(n)).méthode()
 *   verify(mock, never()).méthode()
 *   verifyNoMoreInteractions(mock)
 *   verifyNoInteractions(mock)
 *
 * Matchers :
 *   any(), anyString(), anyLong(), eq(), any(User.class)
 *
 * Avance :
 *   thenAnswer()   : comportement dynamique
 *   InOrder         : verification de l'ordre des appels
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests du UserService avec Mockito")
class UserServiceTest {

    // ================================================================
    // MOCKS — Simulation des dépendances
    // ================================================================

    /**
     * @Mock créé un mock (simulacre) de UserRepository.
     *
     * Le mock est un objet qui implemente l'interface UserRepository
     * mais dont toutes les méthodes retournent des valeurs par defaut :
     * - null pour les objets
     * - 0 pour les nombres
     * - false pour les booleens
     *
     * On configure son comportement avec when().thenReturn().
     */
    @Mock
    private UserRepository userRepository;

    /**
     * @Mock créé un mock de EmailService.
     * Les emails ne seront jamais vraiment envoyes pendant les tests.
     * On peut verifier avec verify() qu'ils ont ete "appeles".
     */
    @Mock
    private EmailService emailService;

    /**
     * @InjectMocks créé une instance réelle de UserService et y injecte
     * les mocks crees par @Mock.
     *
     * Mockito cherche le constructeur de UserService et lui passe
     * les mocks qui correspondent aux types des paramètrès.
     *
     * C'est l'equivalent manuel de :
     *   userService = new UserService(userRepository, emailService);
     */
    @InjectMocks
    private UserService userService;

    // ================================================================
    // TEST 1 : trouverParId — succès
    // ================================================================

    /**
     * Test de trouverParId quand l'utilisateur existe.
     *
     * Pattern AAA avec Mockito :
     *
     * ARRANGE (stubbing) :
     *   when(userRepository.findById(1L)).thenReturn(userAttendu)
     *   -> Configure le mock pour retourner un utilisateur spécifique
     *   -> Le mock ne fait PAS de vrai accès base de donnees
     *
     * ACT :
     *   userService.trouverParId(1L)
     *   -> Le service appelle userRepository.findById(1L)
     *   -> Le mock retourne userAttendu
     *
     * ASSERT :
     *   assertNotNull + assertEquals sur le résultat
     */
    @Test
    @DisplayName("trouverParId — retourne l'utilisateur quand il existe")
    void trouverParId_existant() {
        // ARRANGE : creer l'utilisateur attendu
        User userAttendu = new User(1L, "Alice", "alice@example.com", true);

        // ARRANGE : stubbing — quand findById(1L) est appelé, retourner Alice
        when(userRepository.findById(1L)).thenReturn(userAttendu);

        // ACT : appeler la méthode testee
        User résultat = userService.trouverParId(1L);

        // ASSERT : verifier les proprietes de l'utilisateur retourne
        assertNotNull(résultat);
        assertEquals("Alice", résultat.getNom());
        assertEquals("alice@example.com", résultat.getEmail());
    }

    // ================================================================
    // TEST 2 : trouverParId — exception
    // ================================================================

    /**
     * Test de trouverParId quand l'utilisateur n'existe pas.
     *
     * ARRANGE : stubbing — quand findById(99L) est appelé, retourner null
     *   -> Simule un utilisateur inexistant en base
     *
     * ACT + ASSERT : assertThrows vérifié que l'exception est bien levee
     *   -> Le service detecte le null et lève UserNotFoundException
     */
    @Test
    @DisplayName("trouverParId — lève UserNotFoundException si l'utilisateur n'existe pas")
    void trouverParId_inexistant() {
        // ARRANGE : le mock retourne null pour simuler une absence en base
        when(userRepository.findById(99L)).thenReturn(null);

        // ACT + ASSERT : la méthode doit lever l'exception
        assertThrows(UserNotFoundException.class,
            () -> userService.trouverParId(99L));
    }

    // ================================================================
    // TEST 3 : creerUtilisateur — succès
    // ================================================================

    /**
     * Test de creerUtilisateur (cas nominal).
     *
     * Trois stubbings necessaires :
     * 1. existsByEmail -> false (email disponible)
     * 2. save -> retourne l'utilisateur (avec ID si on veut)
     * 3. envoyerEmail -> true (simule un envoi réussi)
     *
     * Deux verifications :
     * 1. verify(userRepository).save() -> l'utilisateur a bien ete sauvegarde
     * 2. verify(emailService).envoyerEmail() -> l'email a bien ete envoye
     */
    @Test
    @DisplayName("creerUtilisateur — sauvegarde l'utilisateur et envoie un email")
    void creerUtilisateur_succes() {
        // ARRANGE — Stubbing 1 : l'email n'existe pas encore
        when(userRepository.existsByEmail("bob@test.com")).thenReturn(false);

        // ARRANGE — Stubbing 2 : le save retourne l'objet tel quel
        // any(User.class) accepte n'importe quel objet User
        // thenAnswer permet de retourner le paramètre reçu (l'utilisateur non modifie)
        when(userRepository.save(any(User.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // ARRANGE — Stubbing 3 : l'envoi d'email reussit
        // anyString() accepte n'importe quelle String
        when(emailService.envoyerEmail(anyString(), anyString(), anyString()))
            .thenReturn(true);

        // ACT : creer un utilisateur
        User résultat = userService.creerUtilisateur("Bob", "bob@test.com");

        // ASSERT — Verifications du résultat
        assertNotNull(résultat);
        assertEquals("Bob", résultat.getNom());
        assertTrue(résultat.isActif());

        // ASSERT — Verifications des interactions avec les mocks
        // verify() vérifié que la méthode a bien ete appelé́e
        verify(userRepository).save(any(User.class));

        // eq() : matcher d'egalite — vérifié la valeur exacte du paramètre
        // anyString() : matcher generique — accepte n'importe quelle String
        verify(emailService).envoyerEmail(eq("bob@test.com"), anyString(), anyString());
    }

    // ================================================================
    // TEST 4 : creerUtilisateur — email existant
    // ================================================================

    /**
     * Test de creerUtilisateur quand l'email est déjà utilisé.
     *
     * Stubbing : existsByEmail -> true (email déjà pris)
     *
     * Verifications de NON-interaction :
     * verify(userRepository, never()).save() — le save ne doit PAS etre appelé
     * verify(emailService, never()).envoyerEmail() — l'email ne doit PAS etre envoye
     *
     * Ces verifications prouvent que le service s'arrete proprement
     * des que l'email est detecte comme existant, sans effets de bord.
     */
    @Test
    @DisplayName("creerUtilisateur — échoué si l'email existe déjà")
    void creerUtilisateur_emailExistant() {
        // ARRANGE : l'email est déjà pris
        when(userRepository.existsByEmail("existant@test.com")).thenReturn(true);

        // ACT + ASSERT : l'exception doit etre levee
        assertThrows(IllegalArgumentException.class,
            () -> userService.creerUtilisateur("Eve", "existant@test.com"));

        // VERIFICATION : aucun appel ne doit avoir eu lieu
        // never() : le mock ne doit jamais avoir ete appelé avec ces paramètrès
        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).envoyerEmail(anyString(), anyString(), anyString());
    }

    // ================================================================
    // TEST 5 : ArgumentMatchers — any() et eq()
    // ================================================================

    /**
     * Demonstration des ArgumentMatchers.
     *
     * anyLong() : accepte n'importe quelle valeur de type long
     *   -> Pratique quand on ne connait pas la valeur exacte a l'avance
     *   -> Ou quand la valeur n'a pas d'importance pour le test
     *
     * eq() : exige une egalite stricte avec la valeur fournie
     *   -> Utilise pour verifier la valeur exacte d'un paramètre
     *   -> Necessaire des qu'on utilisé un matcher dans un appel
     *
     * Regle Mockito : si vous utilisez un matcher pour un paramètre,
     * tous les paramètrès doivent utilisé́r des matchers (ou etre des valeurs
     * brutes pour les autres, ce qui est equivalent a eq()).
     */
    @Test
    @DisplayName("ArgumentMatchers : any() et eq()")
    void argumentMatchersDemo() {
        // Stubbing avec anyLong() — accepte n'importe quel Long
        when(userRepository.findById(anyLong())).thenReturn(
            new User(1L, "Test", "test@test.com", true));

        // ACT : appeler avec 42L — le stubbing anyLong() le capture
        User u = userService.trouverParId(42L);
        assertNotNull(u);

        // VERIFY avec eq() — vérifié que l'appel a ete fait avec 42L exactement
        verify(userRepository).findById(eq(42L));
    }

    // ================================================================
    // TEST 6 : ArgumentCaptor — capturer un argument
    // ================================================================

    /**
     * @Captor créé un ArgumentCaptor pour le type specifie.
     *
     * L'ArgumentCaptor permet de CAPTURER l'objet passe a un mock
     * pour l'inspecter en detail après l'appel. C'est utile quand :
     * - L'objet est créé a l'interieur de la méthode testee
     * - On veut verifier plusieurs proprietes de l'objet
     * - L'objet n'a pas de equals() adequat
     *
     * Utilisation :
     * 1. Declarer @Captor pour le type voulu
     * 2. verify(mock).méthode(captor.capture())
     * 3. captor.getValue() pour obtenir l'objet capture
     */
    @Captor
    private ArgumentCaptor<User> userCaptor;

    /**
     * Test avec ArgumentCaptor : verifier l'utilisateur passe a save().
     *
     * Quand creerUtilisateur() appelle userRepository.save(user),
     * l'ArgumentCaptor intercepte l'objet User passe en paramètre.
     * On peut ensuite verifier ses proprietes (nom, email, actif, id).
     */
    @Test
    @DisplayName("ArgumentCaptor : inspecter l'utilisateur sauvegarde")
    void capturerUtilisateurSauvegarde() {
        // ARRANGE : configuré les stubbings
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(emailService.envoyerEmail(anyString(), anyString(), anyString()))
            .thenReturn(true);

        // ACT : creer un utilisateur
        userService.creerUtilisateur("Charlie", "charlie@test.com");

        // ASSERT : capturer l'argument passe a save()
        verify(userRepository).save(userCaptor.capture());

        // Recuperer l'objet capture
        User capture = userCaptor.getValue();

        // Verifier ses proprietes
        assertEquals("Charlie", capture.getNom());
        assertEquals("charlie@test.com", capture.getEmail());
        assertTrue(capture.isActif());

        // L'ID doit etre null car il est cense etre attribue par la base
        assertNull(capture.getId(), "L'ID doit etre null avant la persistance");
    }

    // ================================================================
    // TEST 7 : desactiverUtilisateur
    // ================================================================

    /**
     * Test de desactiverUtilisateur.
     *
     * Verifie que :
     * 1. L'utilisateur est bien passe a isActif() = false
     * 2. Le save() est appelé pour persister la modification
     * 3. L'email de notification est envoye
     */
    @Test
    @DisplayName("desactiverUtilisateur — gère l'échec d'envoi d'email")
    void desactiverUtilisateur_succes() {
        // ARRANGE : l'utilisateur existe en base
        User user = new User(1L, "Dave", "dave@test.com", true);
        when(userRepository.findById(1L)).thenReturn(user);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(emailService.envoyerEmail(anyString(), anyString(), anyString()))
            .thenReturn(true);

        // ACT : désactiver l'utilisateur
        userService.desactiverUtilisateur(1L);

        // ASSERT : l'utilisateur est désactivé
        assertFalse(user.isActif());

        // ASSERT : le save a ete appelé avec le user modifie
        verify(userRepository).save(user);

        // ASSERT : l'email a ete envoye a la bonne adresse
        verify(emailService).envoyerEmail(eq("dave@test.com"), anyString(), anyString());
    }

    // ================================================================
    // @Spy — Mock partiel
    // ================================================================

    /**
     * Classe interne pour la demonstration du @Spy.
     *
     * Un spy est un mock PARTIEL : par defaut, il appelle les vraies méthodes.
     * On peut ensuite stubber certaines méthodes pour en modifier le comportement.
     *
     * Difference Mock vs Spy :
     * - Mock : tout est simule, valeurs par defaut (null, 0, false)
     * - Spy : vraie implementation, sauf les méthodes stubbees
     */
    static class CompteurService {
        public int incrementer() { return 1; }
        public int decrementer() { return -1; }
    }

    /**
     * @Spy créé un spy du CompteurService.
     *
     * Contrairement a @Mock, le spy utilisé la VRAIE instance.
     * Les appels non stubbees executent le vrai code.
     */
    @Spy
    private CompteurService compteurSpy;

    /**
     * Demonstration du @Spy avec doReturn().when().
     *
     * Pourquoi doReturn().when() et pas when().thenReturn() ?
     *
     * Avec un spy, when(compteurSpy.incrementer()).thenReturn(100)
     * APPELERAIT la vraie méthode incrementer() avant de la stubber !
     * Cela pourrait avoir des effets de bord indesirables.
     *
     * doReturn(100).when(compteurSpy).incrementer() ne déclenché PAS
     * l'appel réel — il enregistre directement le stub.
     *
     * Regle : utilisé́r doReturn().when() avec les spies.
     */
    @Test
    @DisplayName("@Spy : l'objet réel est utilisé, sauf méthodes stubbees")
    void spyDemo() {
        // Par defaut, le spy appelle la VRAIE méthode incrementer()
        assertEquals(1, compteurSpy.incrementer(),
            "Par defaut, le spy appelle la vraie méthode");

        // Stubber incrementer() pour ce test uniquement
        // doReturn().when() evite d'appeler la vraie méthode d'abord
        doReturn(100).when(compteurSpy).incrementer();

        // Maintenant, incrementer() retourne 100 (stubbe)
        assertEquals(100, compteurSpy.incrementer(),
            "La méthode stubbee retourne 100");

        // decrementer() n'est pas stubbee -> appelle la vraie implementation
        assertEquals(-1, compteurSpy.decrementer(),
            "La méthode non stubbee appelle la vraie implementation");
    }

    // ================================================================
    // TEST 9 : verifyNoMoreInteractions et verifyNoInteractions
    // ================================================================

    /**
     * Test de verifyNoMoreInteractions et verifyNoInteractions.
     *
     * verifyNoMoreInteractions(mock) :
     *   Échec si le mock a ete appelé avec des méthodes non verifiees par verify().
     *   Utile pour detecter des appels "surprises" non prevus.
     *
     * verifyNoInteractions(mock) :
     *   Échec si le mock a ete appelé, point.
     *   Utile pour verifier qu'un mock n'a pas du tout ete sollicite.
     */
    @Test
    @DisplayName("verifyNoMoreInteractions : pas d'appels surprise")
    void pasDAutresInteractions() {
        // ARRANGE : l'utilisateur existe
        User user = new User(1L, "Eve", "eve@test.com", true);
        when(userRepository.findById(1L)).thenReturn(user);

        // ACT : juste une recherche
        userService.trouverParId(1L);

        // ASSERT : seul findById a ete appelé
        verify(userRepository).findById(1L);

        // Verifie qu'aucune autre méthode de userRepository n'a ete appelé́e
        verifyNoMoreInteractions(userRepository);

        // Verifie que emailService n'a PAS DU TOUT ete appelé
        verifyNoInteractions(emailService);
    }

    // ================================================================
    // TEST 10 : thenAnswer — comportement dynamique
    // ================================================================

    /**
     * Demonstration de thenAnswer().
     *
     * thenAnswer() permet de definir un comportement DYNAMIQUE
     * qui depend des arguments recus. Contrairement a thenReturn()
     * qui retourne toujours la même valeur, thenAnswer() reçoit
     * l'invocation et peut calculer la réponse.
     *
     * Cas d'usage typique : simuler l'attribution d'un ID par la base.
     * Quand on sauvegarde un nouvel utilisateur (id=null), la base
     * lui attribue un ID. thenAnswer() peut modifier l'objet reçu
     * pour lui assigner un ID.
     */
    @Test
    @DisplayName("thenAnswer : l'utilisateur sauvegarde reçoit un ID")
    void saveRetourneUtilisateurAvecId() {
        // ARRANGE : stubbings de base
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(emailService.envoyerEmail(anyString(), anyString(), anyString())).thenReturn(true);

        // ARRANGE : thenAnswer simule l'attribution d'un ID par la base
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            // Recuperer l'utilisateur passe en paramètre
            User user = invocation.getArgument(0);
            // Simuler l'attribution d'un ID par la base de donnees
            user.setId(42L);
            // Retourner l'utilisateur modifie
            return user;
        });

        // ACT : creer un utilisateur (id=null au depart)
        User résultat = userService.creerUtilisateur("Frank", "frank@test.com");

        // ASSERT : l'ID a ete attribue par le mock
        assertEquals(42L, résultat.getId(),
            "Le mock simule l'attribution d'un ID par la base de donnees");
    }

    // ================================================================
    // TEST 11 : InOrder — verification de l'ordre des appels
    // ================================================================

    /**
     * Test de InOrder : verification de l'ordre d'appel des mocks.
     *
     * Dans creerUtilisateur(), l'ordre attendu est :
     *   1. userRepository.save() — sauvegarder d'abord
     *   2. emailService.envoyerEmail() — puis notifier
     *
     * InOrder vérifié que les méthodes sont appelees DANS CET ORDRE.
     * Si l'email etait envoye AVANT la sauvegarde, le test echouerait.
     *
     * C'est crucial pour les operations transactionnelles :
     * on ne notifie pas avant d'avoir persiste.
     */
    @Test
    @DisplayName("InOrder : l'email est envoye APRES la sauvegarde")
    void ordreDesAppels() {
        // ARRANGE : stubbings standard
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(emailService.envoyerEmail(anyString(), anyString(), anyString()))
            .thenReturn(true);

        // ACT
        userService.creerUtilisateur("Grace", "grace@test.com");

        // ASSERT : InOrder vérifié l'ordre des appels
        InOrder ordre = inOrder(userRepository, emailService);

        // 1er appel : sauvegarde
        ordre.verify(userRepository).save(any(User.class));

        // 2eme appel : envoi d'email
        ordre.verify(emailService).envoyerEmail(anyString(), anyString(), anyString());

        // Si l'ordre est inverse, le test échoué
    }
}
