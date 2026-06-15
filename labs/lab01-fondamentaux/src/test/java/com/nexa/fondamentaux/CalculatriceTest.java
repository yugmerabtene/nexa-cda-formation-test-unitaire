package com.nexa.fondamentaux;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires de la classe Calculatrice.
 *
 * Ce fichier illustre :
 * - Le pattern AAA (Arrange, Act, Assert)
 * - Les assertions JUnit 5 : assertEquals, assertTrue, assertFalse, assertThrows,
 *   assertNull, assertNotNull, assertAll, assertArrayEquals
 * - L'utilisation de messages d'erreur personnalises
 * - Les Suppliers pour les messages paresseux (constructeur lambda)
 *
 * Chaque test est independant et peut etre execute dans n'importe quel ordre.
 */
@DisplayName("Tests de la classe Calculatrice")
class CalculatriceTest {

    /**
     * Instance de la classe a tester.
     *
     * Creee comme champ final car l'objet est immutable et partage
     * entre tous les tests. JUnit cree une nouvelle instance de la classe
     * de test AVANT chaque @Test, donc ce champ est reinitialise a chaque test.
     */
    private final Calculatrice calc = new Calculatrice();

    // ================================================================
    // TESTS D'ADDITION
    // ================================================================

    /**
     * Test de l'addition de deux nombres positifs.
     *
     * Pattern AAA :
     * - Arrange : calc est initialise comme champ de classe
     * - Act : calc.addition(2, 3) retourne 5
     * - Assert : assertEquals verifie que le resultat vaut 5
     *
     * Le troisieme parametre de assertEquals est le message affiche
     * UNIQUEMENT en cas d'echec du test.
     */
    @Test
    @DisplayName("Addition de deux nombres positifs")
    void additionDeuxPositifs() {
        assertEquals(5, calc.addition(2, 3),
            "2 + 3 devrait donner 5");
    }

    /**
     * Test de l'addition avec des nombres negatifs.
     *
     * On enchaine deux assertions pour verifier :
     * - L'addition d'un positif et d'un negatif
     * - L'addition de deux negatifs
     *
     * Chaque assertEquals est independante : si la premiere echoue,
     * la deuxieme est quand meme executee (contrairement a des if/else).
     */
    @Test
    @DisplayName("Addition avec un nombre negatif")
    void additionAvecNegatif() {
        assertEquals(1, calc.addition(3, -2));
        assertEquals(-5, calc.addition(-2, -3));
    }

    /**
     * Test de l'element neutre de l'addition : zero.
     *
     * a + 0 = a et 0 + a = a, quelle que soit la valeur de a.
     * Ces proprietes sont fondamentales en mathematiques et doivent
     * etre preservees par l'implementation.
     */
    @Test
    @DisplayName("Addition avec zero (element neutre)")
    void additionAvecZero() {
        assertEquals(7, calc.addition(7, 0),
            "a + 0 devrait toujours donner a");
        assertEquals(7, calc.addition(0, 7),
            "0 + a devrait toujours donner a");
    }

    // ================================================================
    // TESTS DE SOUSTRACTION
    // ================================================================

    /**
     * Test de la soustraction simple.
     * 7 - 4 = 3
     */
    @Test
    @DisplayName("Soustraction simple")
    void soustractionSimple() {
        assertEquals(3, calc.soustraction(7, 4));
    }

    /**
     * Test de la soustraction donnant un resultat negatif.
     *
     * 2 - 5 = -3. Ce test verifie que la soustraction gere correctement
     * les cas ou le resultat passe en dessous de zero.
     */
    @Test
    @DisplayName("Soustraction donnant un resultat negatif")
    void soustractionResultatNegatif() {
        assertEquals(-3, calc.soustraction(2, 5));
    }

    // ================================================================
    // TESTS DE MULTIPLICATION
    // ================================================================

    /**
     * Test de la multiplication simple.
     * 3 x 4 = 12
     */
    @Test
    @DisplayName("Multiplication simple")
    void multiplicationSimple() {
        assertEquals(12, calc.multiplication(3, 4));
    }

    /**
     * Test de l'element absorbant de la multiplication : zero.
     *
     * a x 0 = 0 et 0 x a = 0. Tout nombre multiplie par zero donne zero.
     */
    @Test
    @DisplayName("Multiplication par zero (element absorbant)")
    void multiplicationParZero() {
        assertEquals(0, calc.multiplication(5, 0));
        assertEquals(0, calc.multiplication(0, 5));
    }

    /**
     * Test de l'element neutre de la multiplication : un.
     *
     * a x 1 = a. Multiplier par 1 ne change pas la valeur.
     */
    @Test
    @DisplayName("Multiplication par un (element neutre)")
    void multiplicationParUn() {
        assertEquals(7, calc.multiplication(7, 1));
    }

    // ================================================================
    // TESTS DE DIVISION
    // ================================================================

    /**
     * Test de la division simple (sans reste).
     * 9 / 3 = 3
     */
    @Test
    @DisplayName("Division simple")
    void divisionSimple() {
        assertEquals(3, calc.division(9, 3));
    }

    /**
     * Test de la division entiere avec reste.
     *
     * 10 / 3 = 3 en division entiere (le reste 1 est ignore).
     * En Java, la division de deux int retourne un int, pas un float.
     */
    @Test
    @DisplayName("Division avec reste (troncature entiere)")
    void divisionAvecReste() {
        assertEquals(3, calc.division(10, 3),
            "10 / 3 = 3 (division entiere, le reste est ignore)");
    }

    /**
     * Test de la division par zero.
     *
     * Diviser par zero est indefini et DOIT lever une exception.
     * On utilise assertThrows pour :
     * 1. Verifier le TYPE de l'exception (ArithmeticException.class)
     * 2. Verifier le MESSAGE de l'exception
     *
     * La lambda () -> calc.division(10, 0) est le code qui doit lever l'exception.
     * Si aucune exception n'est levee, le test echoue.
     */
    @Test
    @DisplayName("Division par zero doit lever ArithmeticException")
    void divisionParZero() {
        // assertThrows retourne l'exception levee, ce qui permet
        // de faire des assertions supplementaires sur celle-ci
        ArithmeticException exception = assertThrows(
            ArithmeticException.class,
            () -> calc.division(10, 0),
            "La division par zero doit lever une ArithmeticException"
        );
        assertTrue(exception.getMessage().contains("Division par zero"));
    }

    // ================================================================
    // TESTS DE MODULO
    // ================================================================

    /**
     * Test du modulo simple.
     *
     * 10 % 3 = 1 car 10 = 3*3 + 1.
     */
    @Test
    @DisplayName("Modulo simple")
    void moduloSimple() {
        assertEquals(1, calc.modulo(10, 3));
    }

    /**
     * Test du modulo quand il n'y a pas de reste.
     *
     * 9 % 3 = 0 car 9 = 3*3 + 0.
     */
    @Test
    @DisplayName("Modulo sans reste")
    void moduloSansReste() {
        assertEquals(0, calc.modulo(9, 3));
    }

    /**
     * Test du modulo par zero.
     *
     * Tout comme la division, le modulo par zero doit lever une exception.
     * On utilise assertThrows sans capturer l'exception car on ne verifie
     * que le type ici.
     */
    @Test
    @DisplayName("Modulo par zero doit lever exception")
    void moduloParZero() {
        assertThrows(ArithmeticException.class,
            () -> calc.modulo(5, 0));
    }

    // ================================================================
    // TESTS DE PARITE
    // ================================================================

    /**
     * Test de nombres pairs.
     *
     * Verifie 3 cas :
     * - 2 : nombre pair positif
     * - 0 : zero est pair
     * - -4 : nombre pair negatif (la parite est preservee)
     */
    @Test
    @DisplayName("Nombre pair")
    void nombrePair() {
        assertTrue(calc.estPair(2), "2 est pair");
        assertTrue(calc.estPair(0), "0 est pair");
        assertTrue(calc.estPair(-4), "-4 est pair");
    }

    /**
     * Test de nombres impairs.
     *
     * assertFalse verifie que la condition est FALSE.
     * 1 et -3 sont impairs, donc estPair doit retourner false.
     */
    @Test
    @DisplayName("Nombre impair")
    void nombreImpair() {
        assertFalse(calc.estPair(1), "1 est impair");
        assertFalse(calc.estPair(-3), "-3 est impair");
    }

    // ================================================================
    // TESTS DE VALEUR ABSOLUE
    // ================================================================

    /**
     * Test de valeur absolue d'un nombre positif.
     *
     * La valeur absolue d'un nombre positif est le nombre lui-meme.
     */
    @Test
    @DisplayName("Valeur absolue d'un nombre positif (ne change pas)")
    void valeurAbsoluePositif() {
        assertEquals(5, calc.valeurAbsolue(5));
    }

    /**
     * Test de valeur absolue d'un nombre negatif.
     *
     * La valeur absolue d'un nombre negatif est son oppose.
     * |-5| = 5
     */
    @Test
    @DisplayName("Valeur absolue d'un nombre negatif (devient positif)")
    void valeurAbsolueNegatif() {
        assertEquals(5, calc.valeurAbsolue(-5));
    }

    /**
     * Test de valeur absolue de zero.
     *
     * |0| = 0. Zero est sa propre valeur absolue.
     */
    @Test
    @DisplayName("Valeur absolue de zero")
    void valeurAbsolueZero() {
        assertEquals(0, calc.valeurAbsolue(0));
    }

    // ================================================================
    // ASSERTIONS AVANCEES
    // ================================================================

    /**
     * Demonstration de assertAll : groupement d'assertions.
     *
     * assertAll execute TOUTES les assertions, meme si certaines echouent.
     * C'est different de plusieurs assertEquals a la suite :
     * - Avec assertEquals en sequence : le premier echec arrete le test
     * - Avec assertAll : toutes les assertions sont executees, et le rapport
     *   liste TOUS les echecs
     *
     * Avantage : vous voyez tous les problemes en une seule execution,
     * au lieu de corriger les erreurs une par une.
     */
    @Test
    @DisplayName("Verifications groupees avec assertAll")
    void testGroupeAvecAssertAll() {
        assertAll("Verifications groupees de la calculatrice",
            () -> assertEquals(5, calc.addition(2, 3), "addition"),
            () -> assertEquals(6, calc.multiplication(2, 3), "multiplication"),
            () -> assertEquals(-1, calc.soustraction(2, 3), "soustraction"),
            () -> assertEquals(0, calc.division(0, 5), "division de 0"),
            () -> assertTrue(calc.estPair(10), "parite de 10"),
            () -> assertEquals(5, calc.valeurAbsolue(-5), "valeur absolue")
        );
    }

    /**
     * Demonstration des messages d'assertion personnalises.
     *
     * Deux types de messages :
     * 1. String directe : evaluee AVANT l'assertion, meme si le test reussit
     * 2. Supplier<String> (lambda) : evalue UNIQUEMENT en cas d'echec
     *
     * Le Supplier (introduit par () -> "...") est dit "paresseux" (lazy)
     * car la lambda n'est executee que si necessaire.
     * Avantage : pas de calcul de message couteux pour les tests qui passent.
     */
    @Test
    @DisplayName("Demonstration : messages d'erreur personnalises")
    void testAvecMessagePersonnalise() {
        // Message String : toujours evalue, simple et direct
        assertEquals(4, calc.addition(2, 2),
            "Ce message s'affiche si 2+2 != 4");

        // Message Supplier : evalue UNIQUEMENT si le test echoue
        assertTrue(calc.estPair(2),
            () -> "Message paresseux construit uniquement en cas d'echec pour 2");
    }

    /**
     * Test de assertNotNull.
     *
     * Verifie qu'un objet n'est pas null.
     * Utile pour verifier qu'une fabrique ou un constructeur retourne bien un objet.
     */
    @Test
    @DisplayName("assertNotNull : l'objet calculatrice existe")
    void calculatriceNonNulle() {
        assertNotNull(calc, "L'instance de Calculatrice ne doit pas etre null");
    }

    /**
     * Test de assertNull.
     *
     * Verifie qu'une reference est null.
     * Utile pour tester qu'un objet optionnel est bien absent.
     */
    @Test
    @DisplayName("assertNull : test de nullite")
    void testNull() {
        String chaine = null;
        assertNull(chaine);
    }

    /**
     * Test de assertArrayEquals.
     *
     * Compare deux tableaux element par element.
     * Contrairement a assertEquals qui utiliserait equals() sur les tableaux
     * (ce qui comparerait les references, pas le contenu),
     * assertArrayEquals parcourt chaque element et les compare un a un.
     *
     * Ici on verifie que les 4 premiers multiples de 2 sont [2, 4, 6, 8].
     */
    @Test
    @DisplayName("assertArrayEquals : comparaison de tableaux")
    void testTableaux() {
        int[] attendu = {2, 4, 6, 8};
        int[] obtenu = {
            calc.multiplication(2, 1),  // 2 * 1 = 2
            calc.multiplication(2, 2),  // 2 * 2 = 4
            calc.multiplication(2, 3),  // 2 * 3 = 6
            calc.multiplication(2, 4)   // 2 * 4 = 8
        };
        assertArrayEquals(attendu, obtenu, "Table multipliee par 2");
    }
}
