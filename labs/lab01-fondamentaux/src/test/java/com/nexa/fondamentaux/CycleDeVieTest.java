package com.nexa.fondamentaux;

import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
