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
 * @Mock            : cree un mock (simulacre) de l'interface
 * @InjectMocks     : injecte les mocks dans le SUT (System Under Test)
 * @Spy             : cree un mock partiel (vraie implementation + stubbing)
 * @Captor          : capture les arguments passes a un mock
 *
 * Stubbing (configuration du comportement) :
 *   when(mock.methode()).thenReturn(valeur)
 *   doReturn(valeur).when(mock).methode()
 *
 * Verification (controle des interactions) :
 *   verify(mock).methode()
 *   verify(mock, times(n)).methode()
 *   verify(mock, never()).methode()
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
    // MOCKS — Simulation des dependances
    // ================================================================

    /**
     * @Mock cree un mock (simulacre) de UserRepository.
     *
     * Le mock est un objet qui implemente l'interface UserRepository
     * mais dont toutes les methodes retournent des valeurs par defaut :
     * - null pour les objets
     * - 0 pour les nombres
     * - false pour les booleens
     *
     * On configure son comportement avec when().thenReturn().
     */
    @Mock
    private UserRepository userRepository;

    /**
     * @Mock cree un mock de EmailService.
     * Les emails ne seront jamais vraiment envoyes pendant les tests.
     * On peut verifier avec verify() qu'ils ont ete "appeles".
     */
    @Mock
    private EmailService emailService;

    /**
     * @InjectMocks cree une instance reelle de UserService et y injecte
     * les mocks crees par @Mock.
     *
     * Mockito cherche le constructeur de UserService et lui passe
     * les mocks qui correspondent aux types des parametres.
     *
     * C'est l'equivalent manuel de :
     *   userService = new UserService(userRepository, emailService);
     */
    @InjectMocks
    private UserService userService;

    // ================================================================
    // TEST 1 : trouverParId — succes
    // ================================================================

    /**
     * Test de trouverParId quand l'utilisateur existe.
     *
     * Pattern AAA avec Mockito :
     *
     * ARRANGE (stubbing) :
     *   when(userRepository.findById(1L)).thenReturn(userAttendu)
     *   -> Configure le mock pour retourner un utilisateur specifique
     *   -> Le mock ne fait PAS de vrai acces base de donnees
     *
     * ACT :
     *   userService.trouverParId(1L)
     *   -> Le service appelle userRepository.findById(1L)
     *   -> Le mock retourne userAttendu
     *
     * ASSERT :
     *   assertNotNull + assertEquals sur le resultat
     */
    @Test
    @DisplayName("trouverParId — retourne l'utilisateur quand il existe")
    void trouverParId_existant() {
        // ARRANGE : creer l'utilisateur attendu
        User userAttendu = new User(1L, "Alice", "alice@example.com", true);

        // ARRANGE : stubbing — quand findById(1L) est appele, retourner Alice
        when(userRepository.findById(1L)).thenReturn(userAttendu);

        // ACT : appeler la methode testee
        User resultat = userService.trouverParId(1L);

        // ASSERT : verifier les proprietes de l'utilisateur retourne
        assertNotNull(resultat);
        assertEquals("Alice", resultat.getNom());
        assertEquals("alice@example.com", resultat.getEmail());
    }

    // ================================================================
    // TEST 2 : trouverParId — exception
    // ================================================================

    /**
     * Test de trouverParId quand l'utilisateur n'existe pas.
     *
     * ARRANGE : stubbing — quand findById(99L) est appele, retourner null
     *   -> Simule un utilisateur inexistant en base
     *
     * ACT + ASSERT : assertThrows verifie que l'exception est bien levee
     *   -> Le service detecte le null et leve UserNotFoundException
     */
    @Test
    @DisplayName("trouverParId — leve UserNotFoundException si l'utilisateur n'existe pas")
    void trouverParId_inexistant() {
        // ARRANGE : le mock retourne null pour simuler une absence en base
        when(userRepository.findById(99L)).thenReturn(null);

        // ACT + ASSERT : la methode doit lever l'exception
        assertThrows(UserNotFoundException.class,
            () -> userService.trouverParId(99L));
    }

    // ================================================================
    // TEST 3 : creerUtilisateur — succes
    // ================================================================

    /**
     * Test de creerUtilisateur (cas nominal).
     *
     * Trois stubbings necessaires :
     * 1. existsByEmail -> false (email disponible)
     * 2. save -> retourne l'utilisateur (avec ID si on veut)
     * 3. envoyerEmail -> true (simule un envoi reussi)
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
        // thenAnswer permet de retourner le parametre recu (l'utilisateur non modifie)
        when(userRepository.save(any(User.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // ARRANGE — Stubbing 3 : l'envoi d'email reussit
        // anyString() accepte n'importe quelle String
        when(emailService.envoyerEmail(anyString(), anyString(), anyString()))
            .thenReturn(true);

        // ACT : creer un utilisateur
        User resultat = userService.creerUtilisateur("Bob", "bob@test.com");

        // ASSERT — Verifications du resultat
        assertNotNull(resultat);
        assertEquals("Bob", resultat.getNom());
        assertTrue(resultat.isActif());

        // ASSERT — Verifications des interactions avec les mocks
        // verify() verifie que la methode a bien ete appelee
        verify(userRepository).save(any(User.class));

        // eq() : matcher d'egalite — verifie la valeur exacte du parametre
        // anyString() : matcher generique — accepte n'importe quelle String
        verify(emailService).envoyerEmail(eq("bob@test.com"), anyString(), anyString());
    }

    // ================================================================
    // TEST 4 : creerUtilisateur — email existant
    // ================================================================

    /**
     * Test de creerUtilisateur quand l'email est deja utilise.
     *
     * Stubbing : existsByEmail -> true (email deja pris)
     *
     * Verifications de NON-interaction :
     * verify(userRepository, never()).save() — le save ne doit PAS etre appele
     * verify(emailService, never()).envoyerEmail() — l'email ne doit PAS etre envoye
     *
     * Ces verifications prouvent que le service s'arrete proprement
     * des que l'email est detecte comme existant, sans effets de bord.
     */
    @Test
    @DisplayName("creerUtilisateur — echoue si l'email existe deja")
    void creerUtilisateur_emailExistant() {
        // ARRANGE : l'email est deja pris
        when(userRepository.existsByEmail("existant@test.com")).thenReturn(true);

        // ACT + ASSERT : l'exception doit etre levee
        assertThrows(IllegalArgumentException.class,
            () -> userService.creerUtilisateur("Eve", "existant@test.com"));

        // VERIFICATION : aucun appel ne doit avoir eu lieu
        // never() : le mock ne doit jamais avoir ete appele avec ces parametres
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
     *   -> Utilise pour verifier la valeur exacte d'un parametre
     *   -> Necessaire des qu'on utilise un matcher dans un appel
     *
     * Regle Mockito : si vous utilisez un matcher pour un parametre,
     * tous les parametres doivent utiliser des matchers (ou etre des valeurs
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

        // VERIFY avec eq() — verifie que l'appel a ete fait avec 42L exactement
        verify(userRepository).findById(eq(42L));
    }

    // ================================================================
    // TEST 6 : ArgumentCaptor — capturer un argument
    // ================================================================

    /**
     * @Captor cree un ArgumentCaptor pour le type specifie.
     *
     * L'ArgumentCaptor permet de CAPTURER l'objet passe a un mock
     * pour l'inspecter en detail apres l'appel. C'est utile quand :
     * - L'objet est cree a l'interieur de la methode testee
     * - On veut verifier plusieurs proprietes de l'objet
     * - L'objet n'a pas de equals() adequat
     *
     * Utilisation :
     * 1. Declarer @Captor pour le type voulu
     * 2. verify(mock).methode(captor.capture())
     * 3. captor.getValue() pour obtenir l'objet capture
     */
    @Captor
    private ArgumentCaptor<User> userCaptor;

    /**
     * Test avec ArgumentCaptor : verifier l'utilisateur passe a save().
     *
     * Quand creerUtilisateur() appelle userRepository.save(user),
     * l'ArgumentCaptor intercepte l'objet User passe en parametre.
     * On peut ensuite verifier ses proprietes (nom, email, actif, id).
     */
    @Test
    @DisplayName("ArgumentCaptor : inspecter l'utilisateur sauvegarde")
    void capturerUtilisateurSauvegarde() {
        // ARRANGE : configurer les stubbings
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
     * 2. Le save() est appele pour persister la modification
     * 3. L'email de notification est envoye
     */
    @Test
    @DisplayName("desactiverUtilisateur — gere l'echec d'envoi d'email")
    void desactiverUtilisateur_succes() {
        // ARRANGE : l'utilisateur existe en base
        User user = new User(1L, "Dave", "dave@test.com", true);
        when(userRepository.findById(1L)).thenReturn(user);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(emailService.envoyerEmail(anyString(), anyString(), anyString()))
            .thenReturn(true);

        // ACT : desactiver l'utilisateur
        userService.desactiverUtilisateur(1L);

        // ASSERT : l'utilisateur est desactive
        assertFalse(user.isActif());

        // ASSERT : le save a ete appele avec le user modifie
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
     * Un spy est un mock PARTIEL : par defaut, il appelle les vraies methodes.
     * On peut ensuite stubber certaines methodes pour en modifier le comportement.
     *
     * Difference Mock vs Spy :
     * - Mock : tout est simule, valeurs par defaut (null, 0, false)
     * - Spy : vraie implementation, sauf les methodes stubbees
     */
    static class CompteurService {
        public int incrementer() { return 1; }
        public int decrementer() { return -1; }
    }

    /**
     * @Spy cree un spy du CompteurService.
     *
     * Contrairement a @Mock, le spy utilise la VRAIE instance.
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
     * APPELERAIT la vraie methode incrementer() avant de la stubber !
     * Cela pourrait avoir des effets de bord indesirables.
     *
     * doReturn(100).when(compteurSpy).incrementer() ne declenche PAS
     * l'appel reel — il enregistre directement le stub.
     *
     * Regle : utiliser doReturn().when() avec les spies.
     */
    @Test
    @DisplayName("@Spy : l'objet reel est utilise, sauf methodes stubbees")
    void spyDemo() {
        // Par defaut, le spy appelle la VRAIE methode incrementer()
        assertEquals(1, compteurSpy.incrementer(),
            "Par defaut, le spy appelle la vraie methode");

        // Stubber incrementer() pour ce test uniquement
        // doReturn().when() evite d'appeler la vraie methode d'abord
        doReturn(100).when(compteurSpy).incrementer();

        // Maintenant, incrementer() retourne 100 (stubbe)
        assertEquals(100, compteurSpy.incrementer(),
            "La methode stubbee retourne 100");

        // decrementer() n'est pas stubbee -> appelle la vraie implementation
        assertEquals(-1, compteurSpy.decrementer(),
            "La methode non stubbee appelle la vraie implementation");
    }

    // ================================================================
    // TEST 9 : verifyNoMoreInteractions et verifyNoInteractions
    // ================================================================

    /**
     * Test de verifyNoMoreInteractions et verifyNoInteractions.
     *
     * verifyNoMoreInteractions(mock) :
     *   Echec si le mock a ete appele avec des methodes non verifiees par verify().
     *   Utile pour detecter des appels "surprises" non prevus.
     *
     * verifyNoInteractions(mock) :
     *   Echec si le mock a ete appele, point.
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

        // ASSERT : seul findById a ete appele
        verify(userRepository).findById(1L);

        // Verifie qu'aucune autre methode de userRepository n'a ete appelee
        verifyNoMoreInteractions(userRepository);

        // Verifie que emailService n'a PAS DU TOUT ete appele
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
     * qui retourne toujours la meme valeur, thenAnswer() recoit
     * l'invocation et peut calculer la reponse.
     *
     * Cas d'usage typique : simuler l'attribution d'un ID par la base.
     * Quand on sauvegarde un nouvel utilisateur (id=null), la base
     * lui attribue un ID. thenAnswer() peut modifier l'objet recu
     * pour lui assigner un ID.
     */
    @Test
    @DisplayName("thenAnswer : l'utilisateur sauvegarde recoit un ID")
    void saveRetourneUtilisateurAvecId() {
        // ARRANGE : stubbings de base
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(emailService.envoyerEmail(anyString(), anyString(), anyString())).thenReturn(true);

        // ARRANGE : thenAnswer simule l'attribution d'un ID par la base
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            // Recuperer l'utilisateur passe en parametre
            User user = invocation.getArgument(0);
            // Simuler l'attribution d'un ID par la base de donnees
            user.setId(42L);
            // Retourner l'utilisateur modifie
            return user;
        });

        // ACT : creer un utilisateur (id=null au depart)
        User resultat = userService.creerUtilisateur("Frank", "frank@test.com");

        // ASSERT : l'ID a ete attribue par le mock
        assertEquals(42L, resultat.getId(),
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
     * InOrder verifie que les methodes sont appelees DANS CET ORDRE.
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

        // ASSERT : InOrder verifie l'ordre des appels
        InOrder ordre = inOrder(userRepository, emailService);

        // 1er appel : sauvegarde
        ordre.verify(userRepository).save(any(User.class));

        // 2eme appel : envoi d'email
        ordre.verify(emailService).envoyerEmail(anyString(), anyString(), anyString());

        // Si l'ordre est inverse, le test echoue
    }
}
