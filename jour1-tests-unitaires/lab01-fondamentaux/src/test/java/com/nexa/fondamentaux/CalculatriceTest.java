package com.nexa.fondamentaux;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>Tests de la classe Calculatrice — Introduction à JUnit 5</h1>
 *
 * <h2>Architecture des tests Java</h2>
 * <pre>
 * src/main/java/com/nexa/fondamentaux/Calculatrice.java    ← Code à tester
 * src/test/java/com/nexa/fondamentaux/CalculatriceTest.java ← Tests
 * </pre>
 *
 * <h2>Annotation {@code @Test}</h2>
 * <p>
 * Marque une méthode comme étant un <b>test unitaire</b>.
 * JUnit exécute chaque méthode annotée {@code @Test} de manière <b>indépendante</b> :
 * une nouvelle instance de la classe de test est créée pour chaque test.
 * </p>
 *
 * <h3>Contrat de {@code @Test}</h3>
 * <ul>
 *   <li>La méthode doit être {@code public} (ou package-private en JUnit 5)</li>
 *   <li>Le type de retour doit être {@code void} (un test ne retourne rien)</li>
 *   <li>Pas de paramètres (sauf si test paramétré avec {@code @ParameterizedTest})</li>
 *   <li>La méthode ne doit pas être {@code static}</li>
 * </ul>
 *
 * <h2>Annotation {@code @DisplayName}</h2>
 * <p>
 * Fournit un <b>nom lisible</b> pour le test, affiché dans les rapports.
 * Sans {@code @DisplayName}, JUnit utilise le nom de la méthode Java.
 * Bonne pratique : utiliser un nom qui décrit le <b>comportement attendu</b>.
 * </p>
 */
@DisplayName("Tests de la classe Calculatrice")
class CalculatriceTest {

    /*
     * ─── INSTANCE À TESTER ─────────────────────────────────────────────
     *
     * On instancie la classe à tester AVANT chaque test.
     * Cela garantit que chaque test part d'un état propre (pas d'effet de bord).
     *
     * Alternative : déclarer la variable ici et l'initialiser dans @BeforeEach
     * (voir plus bas dans le fichier pour le pattern @BeforeEach).
     */
    private final Calculatrice calc = new Calculatrice();

    /*
     * ────────────────────────────────────────────────────────────────────
     * SECTION 1 : TESTS D'ADDITION
     *
     * Chaque méthode test est une preuve que, dans un cas précis,
     * la méthode testée se comporte comme attendu.
     * ────────────────────────────────────────────────────────────────────
     */

    /**
     * <h3>{@code assertEquals(expected, actual)}</h3>
     * <p>
     * Vérifie que deux valeurs sont <b>égales</b> selon la méthode {@code equals()}.
     * Si ce n'est pas le cas, le test échoue avec un message détaillé :
     * </p>
     * <pre>expected: &lt;5&gt; but was: &lt;4&gt;</pre>
     *
     * <h4>Variante avec message personnalisé</h4>
     * {@code assertEquals(expected, actual, "L'addition 2+2 devrait donner 4")}
     * <p>Si le test échoue, le message personnalisé est affiché AVANT le message par défaut.</p>
     */
    @Test
    @DisplayName("Addition de deux nombres positifs")
    void additionDeuxPositifs() {
        assertEquals(5, calc.addition(2, 3),
            "2 + 3 devrait donner 5");
    }

    @Test
    @DisplayName("Addition avec un nombre négatif")
    void additionAvecNegatif() {
        assertEquals(1, calc.addition(3, -2));
        assertEquals(-5, calc.addition(-2, -3));
    }

    @Test
    @DisplayName("Addition avec zéro (élément neutre)")
    void additionAvecZero() {
        assertEquals(7, calc.addition(7, 0),
            "a + 0 devrait toujours donner a");
        assertEquals(7, calc.addition(0, 7),
            "0 + a devrait toujours donner a");
    }

    /*
     * ────────────────────────────────────────────────────────────────────
     * SECTION 2 : TESTS DE SOUSTRACTION
     * ────────────────────────────────────────────────────────────────────
     */

    @Test
    @DisplayName("Soustraction simple")
    void soustractionSimple() {
        assertEquals(3, calc.soustraction(7, 4));
    }

    @Test
    @DisplayName("Soustraction donnant un résultat négatif")
    void soustractionResultatNegatif() {
        assertEquals(-3, calc.soustraction(2, 5));
    }

    /*
     * ────────────────────────────────────────────────────────────────────
     * SECTION 3 : TESTS DE MULTIPLICATION
     * ────────────────────────────────────────────────────────────────────
     */

    @Test
    @DisplayName("Multiplication simple")
    void multiplicationSimple() {
        assertEquals(12, calc.multiplication(3, 4));
    }

    @Test
    @DisplayName("Multiplication par zéro (élément absorbant)")
    void multiplicationParZero() {
        assertEquals(0, calc.multiplication(5, 0));
        assertEquals(0, calc.multiplication(0, 5));
    }

    @Test
    @DisplayName("Multiplication par un (élément neutre)")
    void multiplicationParUn() {
        assertEquals(7, calc.multiplication(7, 1));
    }

    /*
     * ────────────────────────────────────────────────────────────────────
     * SECTION 4 : TESTS DE DIVISION
     * La division est intéressante car elle peut lever une exception.
     * ────────────────────────────────────────────────────────────────────
     */

    @Test
    @DisplayName("Division simple")
    void divisionSimple() {
        assertEquals(3, calc.division(9, 3));
    }

    @Test
    @DisplayName("Division avec reste (troncature entière)")
    void divisionAvecReste() {
        assertEquals(3, calc.division(10, 3),
            "10 / 3 = 3 (division entière, le reste est ignoré)");
    }

    /**
     * <h3>{@code assertThrows(Class<T>, Executable)}</h3>
     * <p>
     * Vérifie qu'une <b>exception spécifique</b> est levée.
     * Le test échoue si :
     * </p>
     * <ul>
     *   <li>Aucune exception n'est levée</li>
     *   <li>Une exception d'un autre type est levée</li>
     * </ul>
     * <p>
     * Le deuxième argument est une <b>lambda</b> {@code () -> codeQuiLanceException}.
     * On peut aussi récupérer l'exception pour inspecter son message.
     * </p>
     */
    @Test
    @DisplayName("Division par zéro doit lever ArithmeticException")
    void divisionParZero() {
        ArithmeticException exception = assertThrows(
            ArithmeticException.class,
            () -> calc.division(10, 0),
            "La division par zéro doit lever une ArithmeticException"
        );
        assertTrue(exception.getMessage().contains("Division par zéro"));
    }

    /*
     * ────────────────────────────────────────────────────────────────────
     * SECTION 5 : TESTS DE MODULO
     * ────────────────────────────────────────────────────────────────────
     */

    @Test
    @DisplayName("Modulo simple")
    void moduloSimple() {
        assertEquals(1, calc.modulo(10, 3));
    }

    @Test
    @DisplayName("Modulo sans reste")
    void moduloSansReste() {
        assertEquals(0, calc.modulo(9, 3));
    }

    @Test
    @DisplayName("Modulo par zéro doit lever exception")
    void moduloParZero() {
        assertThrows(ArithmeticException.class,
            () -> calc.modulo(5, 0));
    }

    /*
     * ────────────────────────────────────────────────────────────────────
     * SECTION 6 : TESTS DE PARITÉ
     * Utilisation de assertTrue / assertFalse
     * ────────────────────────────────────────────────────────────────────
     */

    /**
     * <h3>{@code assertTrue(condition)} / {@code assertFalse(condition)}</h3>
     * <p>
     * Vérifie qu'une condition booléenne est vraie ou fausse.
     * Utile pour tester des méthodes qui retournent un {@code boolean}.
     * </p>
     */
    @Test
    @DisplayName("Nombre pair")
    void nombrePair() {
        assertTrue(calc.estPair(2), "2 est pair");
        assertTrue(calc.estPair(0), "0 est pair");
        assertTrue(calc.estPair(-4), "-4 est pair");
    }

    @Test
    @DisplayName("Nombre impair")
    void nombreImpair() {
        assertFalse(calc.estPair(1), "1 est impair");
        assertFalse(calc.estPair(-3), "-3 est impair");
    }

    /*
     * ────────────────────────────────────────────────────────────────────
     * SECTION 7 : ASSERTIONS MULTIPLES avec assertAll
     * ────────────────────────────────────────────────────────────────────
     */

    /**
     * <h3>{@code assertAll(Executable...)}</h3>
     * <p>
     * Regroupe plusieurs assertions en UN SEUL test.
     * <b>Différence cruciale</b> avec des assertions individuelles :
     * </p>
     * <ul>
     *   <li>Sans {@code assertAll} : la première assertion qui échoue arrête le test.
     *       Les assertions suivantes ne sont JAMAIS exécutées.</li>
     *   <li>Avec {@code assertAll} : <b>toutes</b> les assertions sont exécutées,
     *       même si certaines échouent. Le rapport liste toutes les erreurs.</li>
     * </ul>
     * <p>
     * On utilise des <b>lambdas</b> {@code () -> assertEquals(...)} pour chaque assertion.
     * </p>
     */
    @Test
    @DisplayName("Vérifications groupées avec assertAll")
    void testGroupeAvecAssertAll() {
        assertAll("Vérifications groupées de la calculatrice",
            () -> assertEquals(5, calc.addition(2, 3), "addition"),
            () -> assertEquals(6, calc.multiplication(2, 3), "multiplication"),
            () -> assertEquals(-1, calc.soustraction(2, 3), "soustraction"),
            () -> assertEquals(0, calc.division(0, 5), "division de 0"),
            () -> assertTrue(calc.estPair(10), "parité de 10"),
            () -> assertEquals(5, calc.valeurAbsolue(-5), "valeur absolue")
        );
    }

    /*
     * ────────────────────────────────────────────────────────────────────
     * SECTION 8 : VALEUR ABSOLUE
     * ────────────────────────────────────────────────────────────────────
     */

    @Test
    @DisplayName("Valeur absolue d'un nombre positif (ne change pas)")
    void valeurAbsoluePositif() {
        assertEquals(5, calc.valeurAbsolue(5));
    }

    @Test
    @DisplayName("Valeur absolue d'un nombre négatif (devient positif)")
    void valeurAbsolueNegatif() {
        assertEquals(5, calc.valeurAbsolue(-5));
    }

    @Test
    @DisplayName("Valeur absolue de zéro")
    void valeurAbsolueZero() {
        assertEquals(0, calc.valeurAbsolue(0));
    }

    /*
     * ────────────────────────────────────────────────────────────────────
     * SECTION 9 : ASSERTIONS AVEC MESSAGES PERSONNALISÉS
     * ────────────────────────────────────────────────────────────────────
     */

    /**
     * <h3>Messages d'assertion : le 3ᵉ argument optionnel</h3>
     * <p>
     * Toutes les méthodes d'assertion acceptent un {@code String message}
     * en <b>dernier paramètre</b>. Ce message est affiché UNIQUEMENT si
     * l'assertion <b>échoue</b>.
     * </p>
     * <pre>
     * assertEquals(valeurAttendue, valeurObtenue, "Description de ce qui est testé");
     * </pre>
     *
     * <h4>Version avec Supplier&lt;String&gt; (évaluation paresseuse)</h4>
     * <p>
     * Pour des messages coûteux à construire, on peut passer une lambda
     * {@code () -> "message construit à la demande"}.
     * Le message n'est construit QUE si le test échoue.
     * </p>
     */
    @Test
    @DisplayName("Démonstration : messages d'erreur personnalisés")
    void testAvecMessagePersonnalise() {
        assertEquals(4, calc.addition(2, 2),
            "Ce message s'affiche si 2+2 ≠ 4");

        /*
         * Supplier (lambda) : le message n'est construit qu'en cas d'échec.
         * Utile quand la construction du message est coûteuse (formatage, concaténations).
         */
        assertTrue(calc.estPair(2),
            () -> "Message paresseux construit uniquement en cas d'échec pour 2");
    }

    /*
     * ────────────────────────────────────────────────────────────────────
     * SECTION 10 : assertNull / assertNotNull
     * ────────────────────────────────────────────────────────────────────
     */

    /**
     * <h3>{@code assertNull(value)} / {@code assertNotNull(value)}</h3>
     * <p>
     * Vérifient qu'une référence est nulle ou non nulle.
     * Utile pour tester qu'un objet a bien été instancié,
     * ou qu'une méthode retourne bien null dans un cas d'erreur.
     * </p>
     */
    @Test
    @DisplayName("assertNotNull : l'objet calculatrice existe")
    void calculatriceNonNulle() {
        assertNotNull(calc, "L'instance de Calculatrice ne doit pas être null");
    }

    @Test
    @DisplayName("assertNull : test de nullité")
    void testNull() {
        String chaine = null;
        assertNull(chaine);
    }

    /*
     * ────────────────────────────────────────────────────────────────────
     * SECTION 11 : assertArrayEquals
     * ────────────────────────────────────────────────────────────────────
     */

    /**
     * <h3>{@code assertArrayEquals(expected[], actual[])}</h3>
     * <p>
     * Vérifie que deux tableaux sont <b>égaux élément par élément</b>.
     * On ne peut PAS utiliser {@code assertEquals} sur des tableaux car
     * ils n'ont pas de {@code equals()} qui compare le contenu.
     * </p>
     */
    @Test
    @DisplayName("assertArrayEquals : comparaison de tableaux")
    void testTableaux() {
        int[] attendu = {2, 4, 6, 8};
        int[] obtenu = {
            calc.multiplication(2, 1),
            calc.multiplication(2, 2),
            calc.multiplication(2, 3),
            calc.multiplication(2, 4)
        };
        assertArrayEquals(attendu, obtenu, "Table multipliée par 2");
    }
}
