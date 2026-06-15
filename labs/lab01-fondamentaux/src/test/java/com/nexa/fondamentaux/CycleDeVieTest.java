package com.nexa.fondamentaux;

import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Demonstration du cycle de vie des tests JUnit 5.
 *
 * Ce fichier illustre l'ordre d'execution des 4 annotations de cycle de vie :
 *
 *   @BeforeAll (static)  -> 1 fois, avant le tout premier test
 *   @BeforeEach          -> avant CHAQUE test
 *   @AfterEach           -> apres CHAQUE test
 *   @AfterAll (static)   -> 1 fois, apres le tout dernier test
 *
 * Chaque @Test s'execute sur une NOUVELLE INSTANCE de la classe.
 * Cela garantit l'isolation : aucun test ne peut affecter l'etat
 * des autres tests via les champs d'instance.
 *
 * Les System.out.println() permettent de visualiser l'ordre
 * dans la console de test.
 */
@DisplayName("Demonstration du cycle de vie JUnit 5")
class CycleDeVieTest {

    /**
     * Un champ d'instance pour la demonstration.
     *
     * Ce champ est reinitialise par @BeforeEach avant chaque test.
     * Si @BeforeEach n'etait pas present, le champ conserverait
     * les modifications du test precedent (violation d'isolation).
     */
    private List<String> historique;

    /**
     * @BeforeAll : execute UNE SEULE FOIS, avant tous les tests de la classe.
     *
     * DOIT etre static car elle est executee avant toute instance de la classe.
     * Utilisation typique :
     * - Connexion a une base de donnees de test
     * - Demarrage d'un serveur embarque
     * - Initialisation de ressources partagees (lourdes)
     * - Chargement de fichiers de configuration
     */
    @BeforeAll
    static void initialisationGlobale() {
        System.out.println("[@BeforeAll] — Appele UNE FOIS avant tous les tests");
    }

    /**
     * @BeforeEach : execute avant CHAQUE test.
     *
     * Non static, peut acceder aux champs d'instance.
     * Utilisation typique :
     * - Reinitialiser les donnees de test pour garantir l'isolation
     * - Creer des objets communs a plusieurs tests
     * - Inserer des donnees temporaires en base
     *
     * Ici, on cree une nouvelle ArrayList avant chaque test.
     * Ainsi, meme si le test precedent a ajoute des elements,
     * le test suivant demarre avec une liste vide.
     */
    @BeforeEach
    void preparationAvantChaqueTest() {
        this.historique = new ArrayList<>();
        System.out.println("  [@BeforeEach] — Appele avant chaque test");
    }

    /**
     * @AfterEach : execute apres CHAQUE test, meme si le test echoue.
     *
     * Utilisation typique :
     * - Nettoyer les ressources (fermer des fichiers, connexions)
     * - Supprimer des donnees temporaires
     * - Reinitialiser des variables statiques ou globales
     *
     * Ici, on vide la liste pour liberer la memoire.
     * (Ce n'est pas strictement necessaire car le garbage collector
     *  le ferait, mais cela illustre le concept.)
     */
    @AfterEach
    void nettoyageApresChaqueTest() {
        this.historique.clear();
        System.out.println("  [@AfterEach] — Appele apres chaque test");
    }

    /**
     * @AfterAll : execute UNE SEULE FOIS, apres tous les tests.
     *
     * DOIT etre static pour la meme raison que @BeforeAll.
     * Executee meme si certains tests ont echoue.
     * Utilisation typique :
     * - Fermer la connexion a la base de donnees
     * - Arreter un serveur embarque
     * - Liberer des ressources globales
     */
    @AfterAll
    static void nettoyageFinal() {
        System.out.println("[@AfterAll] — Appele UNE FOIS apres tous les tests");
    }

    /**
     * Test 1 : Verifie que l'historique est vide au depart.
     *
     * Ce test reussit grace a @BeforeEach qui reinitialise le champ
     * avant chaque test. Si @BeforeEach n'etait pas present, ce test
     * pourrait echouer si testAjoutElement() etait execute avant lui.
     *
     * L'isolation est cruciale : l'ordre d'execution des tests
     * n'est PAS garanti par JUnit.
     */
    @Test
    @DisplayName("Test 1 : l'historique est vide au depart")
    void testHistoriqueVide() {
        assertTrue(historique.isEmpty(),
            "Grace a @BeforeEach, l'historique est reinitialise avant chaque test");
    }

    /**
     * Test 2 : Ajoute un element et verifie qu'il est bien present.
     *
     * On ajoute "Action 1" a la liste.
     * On verifie que la taille est 1 et que l'element est correct.
     * Apres ce test, @AfterEach vide la liste.
     */
    @Test
    @DisplayName("Test 2 : on peut ajouter un element")
    void testAjoutElement() {
        historique.add("Action 1");
        assertEquals(1, historique.size(),
            "L'historique doit contenir l'element ajoute");
        assertEquals("Action 1", historique.get(0));
    }

    /**
     * Test 3 : Preuve d'isolation entre les tests.
     *
     * Ce test verifie que la liste est VIDE, meme si testAjoutElement()
     * a ajoute un element. Cela prouve que :
     * 1. @AfterEach a nettoye apres testAjoutElement()
     * 2. @BeforeEach a reinitialise avant testPreuveIsolation()
     *
     * Si ce test echoue, cela signifie que les tests ne sont PAS isoles,
     * ce qui est un probleme grave de conception des tests.
     */
    @Test
    @DisplayName("Test 3 : preuve d'isolation — l'historique est vide malgre le test 2")
    void testPreuveIsolation() {
        assertTrue(historique.isEmpty(),
            "Preuve : @BeforeEach a reinitialise l'historique.\n" +
            "Si ce test echoue, c'est qu'il y a interdependance entre les tests.");
    }
}
