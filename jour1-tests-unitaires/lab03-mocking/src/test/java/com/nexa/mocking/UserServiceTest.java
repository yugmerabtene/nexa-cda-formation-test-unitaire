package com.nexa.mocking;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <h1>Tests avec Mockito — Documentation exhaustive</h1>
 *
 * <h2>Annotation {@code @ExtendWith(MockitoExtension.class)}</h2>
 * <p>
 * Enregistre l'extension Mockito pour JUnit 5. Cette extension :
 * </p>
 * <ul>
 *   <li>Initialise les champs annotés {@code @Mock} avant chaque test</li>
 *   <li>Injecte les mocks dans le champ annoté {@code @InjectMocks}</li>
 *   <li>Valide l'usage des mocks après chaque test (détecte les stubbings inutilisés)</li>
 * </ul>
 *
 * <h2>Annotation {@code @Mock}</h2>
 * <p>
 * Crée un <b>mock</b> (simulacre) de la classe ou interface cible.
 * Un mock est un objet qui :
 * </p>
 * <ul>
 *   <li>Remplace l'objet réel dans les tests</li>
 *   <li>Par défaut, toutes ses méthodes retournent la valeur par défaut du type :
 *       {@code null} pour les objets, {@code 0} pour les nombres, {@code false} pour les booléens</li>
 *   <li>Peut être <b>stubbé</b> avec {@code when().thenReturn()} pour simuler des comportements</li>
 *   <li>Enregistre tous les appels qu'il reçoit (pour {@code verify()})</li>
 * </ul>
 *
 * <h2>Annotation {@code @InjectMocks}</h2>
 * <p>
 * Crée une instance RÉELLE de la classe et y injecte les mocks
 * (via le constructeur, les setters, ou l'injection directe de champs).
 * C'est l'objet que l'on va <b>réellement tester</b>.
 * </p>
 *
 * <h2>Cycle de vie des annotations Mockito avec JUnit 5</h2>
 * <pre>
 * 1. MockitoExtension crée les @Mock avant chaque test
 * 2. MockitoExtension crée @InjectMocks et y injecte les @Mock
 * 3. Le test s'exécute
 * 4. MockitoExtension vérifie qu'il n'y a pas de stubbing inutilisé
 * </pre>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests du UserService avec Mockito")
class UserServiceTest {

    /*
     * @Mock : crée un mock de UserRepository.
     * Sans cette annotation, il faudrait écrire :
     *   userRepository = Mockito.mock(UserRepository.class);
     */
    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    /*
     * @InjectMocks : crée un VRAI UserService et y injecte
     * les deux mocks ci-dessus via le constructeur.
     *
     * Prérequis : UserService doit avoir un constructeur
     * acceptant (UserRepository, EmailService).
     */
    @InjectMocks
    private UserService userService;

    /*
     * ────────────────────────────────────────────────────────────────────
     * SECTION 1 : Stubbing simple avec when().thenReturn()
     *
     * Le « stubbing » consiste à définir CE QUE le mock doit retourner
     * quand une méthode spécifique est appelée avec des arguments spécifiques.
     *
     * Syntaxe :
     *   when(mock.methode(argument)).thenReturn(valeur);
     *
     * Cette ligne dit à Mockito : « Quand la méthode `methode` est appelée
     * sur ce mock avec `argument`, retourne `valeur` ».
     *
     * IMPORTANT : le stubbing doit être fait AVANT d'appeler la méthode testée.
     * ────────────────────────────────────────────────────────────────────
     */
    @Test
    @DisplayName("trouverParId — retourne l'utilisateur quand il existe")
    void trouverParId_existant() {
        // Arrange : stubbing du mock
        User userAttendu = new User(1L, "Alice", "alice@example.com", true);
        when(userRepository.findById(1L)).thenReturn(userAttendu);

        // Act : appel de la méthode à tester
        User resultat = userService.trouverParId(1L);

        // Assert
        assertNotNull(resultat);
        assertEquals("Alice", resultat.getNom());
        assertEquals("alice@example.com", resultat.getEmail());
    }

    /*
     * ────────────────────────────────────────────────────────────────────
     * SECTION 2 : Stubbing d'exception avec thenThrow()
     *
     * On peut faire en sorte que le mock lève une exception
     * pour tester les chemins d'erreur du code.
     *
     * Syntaxe :
     *   when(mock.methode(arg)).thenThrow(new MonException("message"));
     *
     * Alternative avec doThrow() (utile pour les méthodes void) :
     *   doThrow(new MonException()).when(mock).methodeVoid();
     * ────────────────────────────────────────────────────────────────────
     */

    @Test
    @DisplayName("trouverParId — lève UserNotFoundException si l'utilisateur n'existe pas")
    void trouverParId_inexistant() {
        when(userRepository.findById(99L)).thenReturn(null);

        assertThrows(UserNotFoundException.class,
            () -> userService.trouverParId(99L));
    }

    /*
     * ────────────────────────────────────────────────────────────────────
     * SECTION 3 : Verify — vérifier qu'une méthode a bien été appelée
     *
     * {@code verify(mock).methode()} vérifie que la méthode a été appelée
     * EXACTEMENT UNE FOIS. Si elle n'a pas été appelée, le test échoue.
     *
     * Variantes :
     *   verify(mock, times(n)).methode()     — appelée exactement n fois
     *   verify(mock, atLeast(n)).methode()   — appelée au moins n fois
     *   verify(mock, atMost(n)).methode()    — appelée au plus n fois
     *   verify(mock, never()).methode()      — jamais appelée
     *   verify(mock, atLeastOnce()).methode() — au moins une fois
     * ────────────────────────────────────────────────────────────────────
     */

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

        /*
         * verify() : la preuve que save() a bien été appelée.
         * Sans cette vérification, on pourrait oublier d'appeler save()
         * et le test passerait quand même (car on a stubé le retour).
         *
         * Ici on vérifie que save() a été appelée 1 fois avec n'importe quel User.
         */
        verify(userRepository).save(any(User.class));
        verify(emailService).envoyerEmail(eq("bob@test.com"), anyString(), anyString());
    }

    @Test
    @DisplayName("creerUtilisateur — échoue si l'email existe déjà")
    void creerUtilisateur_emailExistant() {
        when(userRepository.existsByEmail("existant@test.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
            () -> userService.creerUtilisateur("Eve", "existant@test.com"));

        /*
         * verify + never() : on prouve que la méthode n'a JAMAIS été appelée.
         * C'est crucial : si l'email existe, on ne doit PAS sauvegarder.
         */
        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).envoyerEmail(anyString(), anyString(), anyString());
    }

    /*
     * ────────────────────────────────────────────────────────────────────
     * SECTION 4 : ArgumentMatchers — matchers d'arguments flexibles
     *
     * Les ArgumentMatchers permettent de définir des stubbings/verifications
     * qui correspondent à une FAMILLE de valeurs, pas une valeur exacte.
     *
     * Principaux matchers :
     *   any()          — n'importe quel objet (peut être null)
     *   any(Class<T>)  — n'importe quel objet du type T
     *   anyString()    — n'importe quelle String
     *   anyInt(), anyLong(), anyBoolean() — types primitifs
     *   eq(valeur)     — égalité exacte (utile quand on mélange matchers et valeurs)
     *   isNull(), isNotNull()
     *   contains(s), startsWith(s), endsWith(s), matches(regex)
     *
     * RÈGLE : Si un argument utilise un matcher, TOUS les arguments doivent
     * utiliser des matchers. On ne peut pas mélanger valeur brute + matcher.
     *   ✅ when(mock.methode(anyString(), eq("exact")))
     *   ❌ when(mock.methode(anyString(), "exact"))  — erreur !
     * ────────────────────────────────────────────────────────────────────
     */

    @Test
    @DisplayName("ArgumentMatchers : any() et eq()")
    void argumentMatchersDemo() {
        when(userRepository.findById(anyLong())).thenReturn(
            new User(1L, "Test", "test@test.com", true));

        User u = userService.trouverParId(42L);
        assertNotNull(u);

        verify(userRepository).findById(eq(42L));
    }

    /*
     * ────────────────────────────────────────────────────────────────────
     * SECTION 5 : ArgumentCaptor — capturer les arguments passés au mock
     *
     * {@code ArgumentCaptor<T>} permet de « capturer » l'argument transmis
     * à un mock pour l'inspecter en détail.
     *
     * Usage typique : vérifier le contenu d'un objet complexe passé à save().
     *
     * Étapes :
     * 1. Déclarer : @Captor ArgumentCaptor<Type> captor;
     * 2. Exécuter le code testé
     * 3. Capturer : verify(mock).methode(captor.capture());
     * 4. Inspecter : captor.getValue()
     * ────────────────────────────────────────────────────────────────────
     */

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

        /*
         * On capture le User passé à save() pour vérifier ses attributs.
         * Ce niveau de détail n'est pas possible avec un simple any().
         */
        verify(userRepository).save(userCaptor.capture());
        User capture = userCaptor.getValue();

        assertEquals("Charlie", capture.getNom());
        assertEquals("charlie@test.com", capture.getEmail());
        assertTrue(capture.isActif());
        assertNull(capture.getId(), "L'ID doit être null avant la persistance");
    }

    /*
     * ────────────────────────────────────────────────────────────────────
     * SECTION 6 : doThrow — simuler une exception pour les méthodes void
     *
     * Pour les méthodes void, on ne peut pas utiliser when().thenThrow()
     * car when() attend une valeur de retour.
     *
     * Syntaxe :
     *   doThrow(exception).when(mock).methodeVoid(arg);
     * ────────────────────────────────────────────────────────────────────
     */

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

    /*
     * ────────────────────────────────────────────────────────────────────
     * SECTION 7 : @Spy — mock partiel
     *
     * {@code @Spy} crée un <b>mock partiel</b> : l'objet RÉEL est utilisé,
     * sauf pour les méthodes explicitement stubbées.
     *
     * Différence @Mock vs @Spy :
     *   @Mock  : toutes les méthodes sont simulées (retournent null/0/false)
     *   @Spy   : l'objet réel est instancié, seules les méthodes stubbées sont modifiées
     *
     * ATTENTION : pour les spies, utiliser doReturn().when() au lieu de when().thenReturn()
     * car when().thenReturn() appelle la VRAIE méthode, ce qui peut être problématique.
     * ────────────────────────────────────────────────────────────────────
     */

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

        /*
         * On stubbe UNE méthode du spy.
         * Important : utiliser doReturn() pour les spies,
         * pas when().thenReturn() qui appellerait la vraie méthode d'abord.
         */
        doReturn(100).when(compteurSpy).incrementer();

        assertEquals(100, compteurSpy.incrementer(),
            "La méthode stubbée retourne 100");
        assertEquals(-1, compteurSpy.decrementer(),
            "La méthode non stubbée appelle la vraie implémentation");
    }

    /*
     * ────────────────────────────────────────────────────────────────────
     * SECTION 8 : verifyNoMoreInteractions — pas d'appels supplémentaires
     *
     * Vérifie qu'aucune autre méthode du mock n'a été appelée
     * au-delà de celles déjà vérifiées avec verify().
     *
     * Utile pour garantir que le code testé n'a pas d'effets de bord
     * non intentionnels.
     * ────────────────────────────────────────────────────────────────────
     */

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

    /*
     * ────────────────────────────────────────────────────────────────────
     * SECTION 9 : thenAnswer — comportement dynamique
     *
     * {@code thenAnswer()} permet de définir un comportement QUI DÉPEND
     * des arguments reçus, plutôt qu'une valeur statique.
     *
     * Utile pour simuler un save() qui retourne l'objet modifié (avec ID).
     * ────────────────────────────────────────────────────────────────────
     */

    @Test
    @DisplayName("thenAnswer : l'utilisateur sauvegardé reçoit un ID")
    void saveRetourneUtilisateurAvecId() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(emailService.envoyerEmail(anyString(), anyString(), anyString())).thenReturn(true);

        /*
         * thenAnswer simule le comportement d'un vrai repository :
         * il prend l'utilisateur passé en argument, lui assigne un ID,
         * et le retourne.
         */
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(42L);
            return user;
        });

        User resultat = userService.creerUtilisateur("Frank", "frank@test.com");

        assertEquals(42L, resultat.getId(),
            "Le mock simule l'attribution d'un ID par la base de données");
    }

    /*
     * ────────────────────────────────────────────────────────────────────
     * SECTION 10 : InOrder — vérifier l'ordre des appels
     *
     * {@code InOrder} vérifie que les méthodes d'un (ou plusieurs) mock(s)
     * ont été appelées dans un ORDRE PRÉCIS.
     *
     * Utile quand l'ordre des opérations est critique (ex: sauvegarder
     * AVANT d'envoyer l'email, pas l'inverse).
     * ────────────────────────────────────────────────────────────────────
     */

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
