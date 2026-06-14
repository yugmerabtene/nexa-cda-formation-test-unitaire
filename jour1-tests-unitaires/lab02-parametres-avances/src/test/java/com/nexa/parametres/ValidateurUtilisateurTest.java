package com.nexa.parametres;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.junit.jupiter.params.provider.EnumSource.Mode.INCLUDE;

/**
 * <h1>Tests paramétrés JUnit 5 — Documentation exhaustive</h1>
 *
 * <h2>Pourquoi les tests paramétrés ?</h2>
 * <p>
 * Sans tests paramétrés, on écrit UN test par cas, ce qui mène à :
 * </p>
 * <pre>
 * {@code @Test void testEmailValide1()} { ... }
 * {@code @Test void testEmailValide2()} { ... }
 * {@code @Test void testEmailValide3()} { ... }
 * // ... 50 tests identiques à l'argument près
 * </pre>
 * <p>
 * Avec les tests paramétrés, on écrit UN test et on fournit les données séparément.
 * Chaque jeu de données génère UNE exécution du test.
 * </p>
 *
 * <h2>{@code @ParameterizedTest}</h2>
 * <p>
 * REMPLACE {@code @Test} pour indiquer un test paramétré.
 * Le framework injecte les arguments dans la méthode via une <b>source</b> de données.
 * </p>
 *
 * <h2>Sources de données disponibles</h2>
 * <table>
 *   <tr><th>Annotation</th><th>Description</th></tr>
 *   <tr><td>{@code @ValueSource}</td><td>Valeurs littérales simples (int, string, etc.)</td></tr>
 *   <tr><td>{@code @CsvSource}</td><td>Lignes CSV avec plusieurs colonnes</td></tr>
 *   <tr><td>{@code @CsvFileSource}</td><td>Fichier CSV externe</td></tr>
 *   <tr><td>{@code @EnumSource}</td><td>Valeurs d'un enum</td></tr>
 *   <tr><td>{@code @MethodSource}</td><td>Méthode factory retournant des arguments</td></tr>
 *   <tr><td>{@code @NullSource}</td><td>Injecte {@code null}</td></tr>
 *   <tr><td>{@code @EmptySource}</td><td>Injecte chaîne vide / collection vide</td></tr>
 *   <tr><td>{@code @NullAndEmptySource}</td><td>Combine les deux</td></tr>
 * </table>
 */
@DisplayName("Tests paramétrés du ValidateurUtilisateur")
class ValidateurUtilisateurTest {

    private final ValidateurUtilisateur validateur = new ValidateurUtilisateur();

    /*
     * ────────────────────────────────────────────────────────────────────
     * SECTION 1 : @ValueSource — valeurs simples
     *
     * @ValueSource permet de fournir un tableau de valeurs d'UN SEUL type.
     * Types supportés : ints, longs, doubles, strings, chars, shorts, bytes,
     * floats, booleans, classes.
     *
     * Chaque valeur est injectée UNE PAR UNE dans le paramètre de la méthode.
     * Ici, 5 valeurs → 5 exécutions du test.
     * ────────────────────────────────────────────────────────────────────
     */
    @ParameterizedTest(name = "{index} : email \"{0}\" est valide → {1}")
    @DisplayName("Validation d'emails valides")
    @ValueSource(strings = {
        "test@example.com",
        "user.name@domain.co",
        "a@b.co",
        "contact@entreprise.fr",
        "nom.prenom@site.gouv.fr"
    })
    void emailsValides(String email) {
        assertTrue(validateur.estEmailValide(email),
            () -> "L'email '" + email + "' devrait être valide");
    }

    @ParameterizedTest(name = "\"{0}\" → email INVALIDE")
    @DisplayName("Validation d'emails invalides")
    @ValueSource(strings = {
        "",           // vide
        "pasd'arobase",
        "@domaine.com",   // @ au début
        "user@",         // pas de domaine
        "user@domaine",  // pas de point dans le domaine
        "user@@domaine.com" // double @
    })
    void emailsInvalides(String email) {
        assertFalse(validateur.estEmailValide(email),
            () -> "L'email '" + email + "' devrait être invalide");
    }

    /*
     * ────────────────────────────────────────────────────────────────────
     * SECTION 2 : @CsvSource — plusieurs colonnes
     *
     * @CsvSource fournit des lignes CSV où chaque colonne correspond
     * à UN paramètre de la méthode de test.
     *
     * Syntaxe :
     *   "valeur1, valeur2, valeur3"  → exécution 1
     *   "valeur4, valeur5, valeur6"  → exécution 2
     *
     * Délimiteur personnalisable avec delimiter = ';'
     * Guillemets simples pour échapper : 'valeur avec, virgule'
     * Le séparateur par défaut est la virgule.
     * ────────────────────────────────────────────────────────────────────
     */
    @ParameterizedTest(name = "\"{0}\" → score = {1}/100")
    @DisplayName("Score de robustesse des mots de passe")
    @CsvSource({
        "abc,              0",
        "abcd1234,        40",
        "Abcd1234,        60",
        "Abcd1234!,        70",
        "MotDePasseTresLong123!, 100",
        "12345678,         25"
    })
    void scoreMotDePasse(String motDePasse, int scoreAttendu) {
        assertEquals(scoreAttendu, validateur.scoreMotDePasse(motDePasse),
            "Score incorrect pour '" + motDePasse + "'");
    }

    /*
     * ────────────────────────────────────────────────────────────────────
     * SECTION 3 : @CsvSource avec nullValues — gestion des null
     *
     * Le paramètre nullValues permet de spécifier quelles chaînes
     * doivent être interprétées comme la valeur null (et non comme "N/A").
     * ────────────────────────────────────────────────────────────────────
     */
    @ParameterizedTest(name = "email = {0} est valide → {1}")
    @DisplayName("Emails : cas limites avec null")
    @CsvSource(value = {
        "N/A, false",
        "'', false"
    }, nullValues = "N/A")
    void emailsAvecNull(String email, boolean attendu) {
        assertEquals(attendu, validateur.estEmailValide(email));
    }

    /*
     * ────────────────────────────────────────────────────────────────────
     * SECTION 4 : @EnumSource — toutes les valeurs d'un enum
     *
     * @EnumSource injecte chaque valeur de l'enum spécifié.
     * Options :
     *   - mode = INCLUDE  : inclut seulement les valeurs listées
     *   - mode = EXCLUDE  : exclut les valeurs listées
     *   - names = {"A", "B"} : filtrage par nom
     * ────────────────────────────────────────────────────────────────────
     */

    enum StatutUtilisateur { ACTIF, INACTIF, SUSPENDU, SUPPRIME }

    @ParameterizedTest
    @DisplayName("Tous les statuts sauf SUPPRIME sont valides")
    @EnumSource(value = StatutUtilisateur.class, mode = EXCLUDE, names = "SUPPRIME")
    void statutsValides(StatutUtilisateur statut) {
        assertNotEquals(StatutUtilisateur.SUPPRIME, statut,
            "Tous les statuts sauf SUPPRIME devraient passer");
    }

    @ParameterizedTest
    @DisplayName("Seuls ACTIF et SUSPENDU sont testés ici")
    @EnumSource(value = StatutUtilisateur.class, mode = INCLUDE, names = {"ACTIF", "SUSPENDU"})
    void statutsSpecifiques(StatutUtilisateur statut) {
        assertTrue(statut == StatutUtilisateur.ACTIF || statut == StatutUtilisateur.SUSPENDU);
    }

    /*
     * ────────────────────────────────────────────────────────────────────
     * SECTION 5 : @CsvFileSource — lecture depuis un fichier CSV
     *
     * Charge les données depuis un fichier CSV externe (src/test/resources/).
     * Format du fichier :
     *   email,attendu
     *   test@example.com,true
     *   invalid,false
     * ────────────────────────────────────────────────────────────────────
     */
    @ParameterizedTest(name = "Téléphone \"{0}\" valide → {1}")
    @DisplayName("Validation téléphones via fichier CSV externe")
    @CsvFileSource(resources = "/telephones-test.csv", numLinesToSkip = 1)
    void telephonesDepuisFichier(String telephone, boolean attendu) {
        assertEquals(attendu, validateur.estTelephoneValide(telephone),
            "Échec pour le téléphone : " + telephone);
    }

    /*
     * ────────────────────────────────────────────────────────────────────
     * SECTION 6 : @MethodSource — méthode factory
     *
     * @MethodSource référence une méthode (par son nom) qui retourne
     * un Stream, Iterable, Iterator, ou tableau d'Arguments.
     *
     * La méthode source DOIT être static (sauf si la classe de test
     * est annotée @TestInstance(Lifecycle.PER_CLASS)).
     *
     * Avantage : logique complexe de génération de données,
     * réutilisation entre plusieurs tests.
     * ────────────────────────────────────────────────────────────────────
     */
    @ParameterizedTest(name = "{0} ans → catégorie \"{1}\"")
    @DisplayName("Catégorisation par âge (via @MethodSource)")
    @MethodSource("fournirAgesEtCategories")
    void categorisationAge(int age, String categorieAttendue) {
        assertEquals(categorieAttendue, validateur.categorieAge(age),
            "Catégorie incorrecte pour l'âge " + age);
    }

    static Stream<Arguments> fournirAgesEtCategories() {
        return Stream.of(
            Arguments.of(0, "MINEUR"),
            Arguments.of(17, "MINEUR"),
            Arguments.of(18, "JEUNE_ADULTE"),
            Arguments.of(24, "JEUNE_ADULTE"),
            Arguments.of(25, "ADULTE"),
            Arguments.of(59, "ADULTE"),
            Arguments.of(60, "SENIOR"),
            Arguments.of(119, "SENIOR"),
            Arguments.of(120, "CENTENAIRE"),
            Arguments.of(150, "CENTENAIRE")
        );
    }

    /*
     * ────────────────────────────────────────────────────────────────────
     * SECTION 7 : @MethodSource retournant des entiers simples
     * ────────────────────────────────────────────────────────────────────
     */
    @ParameterizedTest
    @DisplayName("Âges valides (via @MethodSource d'entiers)")
    @MethodSource("agesValides")
    void agesValides(int age) {
        assertTrue(validateur.estAgeValide(age),
            "L'âge " + age + " devrait être valide");
    }

    static Stream<Integer> agesValides() {
        return Stream.of(18, 25, 30, 60, 100, 120);
    }

    /*
     * ────────────────────────────────────────────────────────────────────
     * SECTION 8 : @NullSource, @EmptySource, @NullAndEmptySource
     *
     * Ces annotations sont des raccourcis pour tester les cas null/vide,
     * qui sont souvent des cas limites importants.
     *
     * On peut les COMBINER avec d'autres sources (ex: @ValueSource + @NullSource).
     * ────────────────────────────────────────────────────────────────────
     */
    @ParameterizedTest
    @DisplayName("Email invalide pour null et chaîne vide")
    @NullAndEmptySource
    void emailNullOuVide(String email) {
        assertFalse(validateur.estEmailValide(email));
    }

    @ParameterizedTest
    @DisplayName("Téléphone invalide pour null ET chaîne vide")
    @NullSource
    @EmptySource
    void telephoneNullEtVide(String telephone) {
        assertFalse(validateur.estTelephoneValide(telephone));
    }

    @ParameterizedTest
    @DisplayName("Score = 0 pour null et chaîne vide")
    @NullAndEmptySource
    @ValueSource(strings = " ") // on peut combiner avec @ValueSource !
    void scoreZeroPourEntreesInvalides(String mdp) {
        assertEquals(0, validateur.scoreMotDePasse(mdp));
    }

    /*
     * ────────────────────────────────────────────────────────────────────
     * SECTION 9 : CsvSource avec Messages personnalisés
     * Le paramètre `name` de @ParameterizedTest utilise des placeholders :
     *   {index}   : numéro de l'invocation (0, 1, 2...)
     *   {0}, {1}  : valeur de l'argument n°0, n°1
     *   {arguments} : tous les arguments concaténés
     * ────────────────────────────────────────────────────────────────────
     */
    @ParameterizedTest(name = "Téléphone \"{0}\" doit être valide")
    @DisplayName("Téléphones valides (formats variés)")
    @CsvSource({
        "0612345678",
        "06 12 34 56 78",
        "06.12.34.56.78",
        "+33612345678",
        "+33 6 12 34 56 78"
    })
    void telephonesValidesFormatsVariés(String telephone) {
        assertTrue(validateur.estTelephoneValide(telephone),
            "Format attendu valide : " + telephone);
    }

    /*
     * ────────────────────────────────────────────────────────────────────
     * SECTION 10 : Conversions automatiques de types
     *
     * JUnit 5 convertit automatiquement les chaînes CSV vers le type
     * du paramètre de la méthode (int, long, double, boolean, enum...).
     *
     * Types supportés nativement :
     *   - byte, short, int, long, float, double, char, boolean
     *   - String
     *   - Class (via le nom qualifié complet)
     *   - Enum (par nom)
     *   - java.time (Duration, Instant, LocalDate, LocalDateTime, etc.)
     *   - BigDecimal, BigInteger
     *   - URL, URI, File, Path
     *
     * Pour les types personnalisés : implémenter ArgumentConverter.
     * ────────────────────────────────────────────────────────────────────
     */
    @ParameterizedTest
    @DisplayName("Conversion automatique : String → int → boolean")
    @CsvSource({
        "18, true",
        "17, false",
        "120, true",
        "121, false",
        "0, false"
    })
    void conversionAutoTypes(int age, boolean attendu) {
        assertEquals(attendu, validateur.estAgeValide(age));
    }
}
