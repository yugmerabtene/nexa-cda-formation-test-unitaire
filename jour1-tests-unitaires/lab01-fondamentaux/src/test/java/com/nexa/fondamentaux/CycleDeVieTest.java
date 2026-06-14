package com.nexa.fondamentaux;

import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>Cycle de Vie des Tests JUnit 5</h1>
 *
 * <p>
 * JUnit 5 crée une <b>nouvelle instance</b> de la classe de test pour
 * <b>chaque</b> méthode {@code @Test}. Cela garantit l'isolation entre
 * les tests : aucun état partagé, pas d'effet de bord d'un test sur l'autre.
 * </p>
 *
 * <h2>Le cycle complet pour N tests dans une classe :</h2>
 * <pre>
 * ┌──────────────┐
 * │ @BeforeAll   │ ← Exécuté UNE SEULE FOIS avant tous les tests (doit être static)
 * └──────┬───────┘
 *        │
 *    ┌───▼────────────────────────────────┐
 *    │ Pour chaque @Test (répété N fois) : │
 *    │                                     │
 *    │  1. new MaClasseDeTest()           │ ← Nouvelle instance
 *    │  2. @BeforeEach                     │ ← Préparation avant ce test
 *    │  3. @Test                           │ ← Exécution du test
 *    │  4. @AfterEach                      │ ← Nettoyage après ce test
 *    └───┬────────────────────────────────┘
 *        │
 * ┌──────▼───────┐
 * │ @AfterAll    │ ← Exécuté UNE SEULE FOIS après tous les tests (doit être static)
 * └──────────────┘
 * </pre>
 *
 * <h2>Pourquoi l'isolation est cruciale ?</h2>
 * <p>
 * Chaque test doit pouvoir s'exécuter indépendamment des autres,
 * dans n'importe quel ordre. Si le test B dépend de l'état créé par
 * le test A, on parle de <b>tests fragiles</b> (flaky tests) :
 * ils peuvent échouer aléatoirement selon l'ordre d'exécution.
 * </p>
 */
@DisplayName("Démonstration du cycle de vie JUnit 5")
class CycleDeVieTest {

    private List<String> historique;

    @BeforeAll
    static void initialisationGlobale() {
        System.out.println("[@BeforeAll] — Appelé UNE FOIS avant tous les tests");
    }

    @BeforeEach
    void preparationAvantChaqueTest() {
        this.historique = new ArrayList<>();
        System.out.println("  [@BeforeEach] — Appelé avant chaque test");
    }

    @AfterEach
    void nettoyageApresChaqueTest() {
        this.historique.clear();
        System.out.println("  [@AfterEach] — Appelé après chaque test");
    }

    @AfterAll
    static void nettoyageFinal() {
        System.out.println(  "[@AfterAll] — Appelé UNE FOIS après tous les tests");
    }

    @Test
    @DisplayName("Test 1 : l'historique est vide au départ")
    void testHistoriqueVide() {
        assertTrue(historique.isEmpty(),
            "Grâce à @BeforeEach, l'historique est réinitialisé avant chaque test");
    }

    @Test
    @DisplayName("Test 2 : on peut ajouter un élément")
    void testAjoutElement() {
        historique.add("Action 1");
        assertEquals(1, historique.size(),
            "L'historique doit contenir l'éléments ajouté");
        assertEquals("Action 1", historique.get(0));
    }

    @Test
    @DisplayName("Test 3 : preuve d'isolation — l'historique est vide malgré le test 2")
    void testPreuveIsolation() {
        assertTrue(historique.isEmpty(),
            "Preuve : @BeforeEach a réinitialisé l'historique,.\n" +
            "Si ce test échoue, c'est qu'il y a interdépendance entre les tests.");
    }
}
