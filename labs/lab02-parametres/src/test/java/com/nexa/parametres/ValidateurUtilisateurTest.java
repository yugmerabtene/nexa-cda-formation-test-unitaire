package com.nexa.paramètrès;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.junit.jupiter.params.provider.EnumSource.Mode.INCLUDE;

/**
 * Tests paramètrès du validateur d'utilisateur.
 *
 * Ce fichier illustre TOUTES les sources de paramètrès de JUnit 5 :
 *
 * @ValueSource      : liste de valeurs simples (String, int, etc.)
 * @CsvSource        : couples de valeurs (entree, résultat attendu)
 * @CsvFileSource    : fichier CSV externe dans src/test/resources/
 * @EnumSource       : iteration sur les valeurs d'un enum
 * @MethodSource     : méthode factory retournant Stream<Arguments>
 * @NullSource       : injecter null
 * @EmptySource      : injecter chaine vide
 * @NullAndEmptySource : injecter null ET chaine vide
 *
 * La conversion automatique de types est egalement illustree :
 * JUnit convertit automatiquement les String du CSV en int, boolean, etc.
 */
@DisplayName("Tests paramètrès du ValidateurUtilisateur")
class ValidateurUtilisateurTest {

    /**
     * Instance du validateur, partagee entre tous les tests.
     * JUnit créé une nouvelle instance de la classe de test avant chaque test,
     * donc ce champ est reinitialise a chaque iteration parametree.
     */
    private final ValidateurUtilisateur validateur = new ValidateurUtilisateur();

    // ================================================================
    // @ValueSource — Tests paramètrès avec valeurs simples
    // ================================================================

    /**
     * Test paramètre avec @ValueSource pour les emails VALIDES.
     *
     * Chaque chaine dans @ValueSource est injectee comme paramètre 'email'.
     * JUnit exécuté cette méthode 5 fois (une par valeur).
     *
     * Le paramètre 'name' personnalise l'affichage dans le rapport de test :
     * - {index} : numero de l'iteration (commence a 1)
     * - {0}    : valeur du premier paramètre (l'email ici)
     *
     * Resultat dans le rapport :
     *   [1] : email "test@example.com" est validé -> true
     *   [2] : email "user.name@domain.co" est validé -> true
     *   ...
     */
    @ParameterizedTest(name = "{index} : email \"{0}\" est validé -> {1}")
    @DisplayName("Validation d'emails valides")
    @ValueSource(strings = {
        "test@example.com",        // Format standard
        "user.name@domain.co",     // Point dans la partie locale
        "a@b.co",                  // Format minimal validé
        "contact@entreprise.fr",   // Domaine .fr
        "nom.prenom@site.gouv.fr"  // Sous-domaine
    })
    void emailsValides(String email) {
        assertTrue(validateur.estEmailValide(email),
            // Supplier<String> : le message n'est construit que si le test échoué
            () -> "L'email '" + email + "' devrait etre validé");
    }

    /**
     * Test paramètre avec @ValueSource pour les emails INVALIDES.
     *
     * Chaque cas teste une regle de validation differente :
     * 1. Chaine vide -> invalide
     * 2. Pas d'arobase -> invalide
     * 3. Arobase en debut -> invalide (pas de partie locale)
     * 4. Arobase en fin -> invalide (pas de domaine)
     * 5. Pas de point dans le domaine -> invalide
     * 6. Double arobase -> invalide
     *
     * assertFalse vérifié que la méthode retourne false pour chaque cas.
     */
    @ParameterizedTest(name = "\"{0}\" -> email INVALIDE")
    @DisplayName("Validation d'emails invalides")
    @ValueSource(strings = {
        "",                    // Chaine vide
        "pasd'arobase",        // Pas de @
        "@domaine.com",        // @ en première position (pas de partie locale)
        "user@",               // @ en dernière position (pas de domaine)
        "user@domaine",        // Pas de point dans le domaine
        "user@@domaine.com"    // Double @ interdit
    })
    void emailsInvalides(String email) {
        assertFalse(validateur.estEmailValide(email),
            () -> "L'email '" + email + "' devrait etre invalide");
    }

    // ================================================================
    // @CsvSource — Tests avec entree et résultat attendu
    // ================================================================

    /**
     * Test paramètre avec @CsvSource pour le score de mot de passe.
     *
     * Chaque ligne du CSV contient deux colonnes :
     * - Colonne 1 : le mot de passe (String)
     * - Colonne 2 : le score attendu (int)
     *
     * JUnit convertit automatiquement la chaine "40" en int 40.
     * C'est la conversion automatique de types de JUnit 5.
     *
     * Les tests couvrent :
     * - Mot de passe trop court : score 0
     * - 8 caracteres : +25 (longueur)
     * - + majuscule : +20
     * - + caractere spécial : +10
     * - Tous les criteres : 100 (plafonne)
     */
    @ParameterizedTest(name = "\"{0}\" -> score = {1}/100")
    @DisplayName("Score de robustesse des mots de passe")
    @CsvSource({
        "abc,              0",   // Trop court, aucun critere
        "abcd1234,        40",   // 8+ chars (25) + chiffre (15) = 40
        "Abcd1234,        60",   // 8+ chars (25) + maj (20) + min (15) = 60
        "Abcd1234!,        70",  // + caractere spécial (10) = 70
        "MotDePasseTresLong123!, 100", // Tous les criteres -> 100 (plafonne)
        "12345678,         25"   // 8+ chars (25) uniquement
    })
    void scoreMotDePasse(String motDePasse, int scoreAttendu) {
        assertEquals(scoreAttendu, validateur.scoreMotDePasse(motDePasse),
            "Score incorrect pour '" + motDePasse + "'");
    }

    /**
     * Test paramètre avec gestion de la valeur null dans un CSV.
     *
     * nullValues = "N/A" indique a JUnit que la chaine "N/A"
     * doit etre interpretee comme la valeur null (pas la chaine "N/A").
     *
     * Sans cette option, JUnit passerait la chaine "N/A" telle quelle.
     */
    @ParameterizedTest(name = "email = {0} est validé -> {1}")
    @DisplayName("Emails : cas limites avec null")
    @CsvSource(value = {
        "N/A, false",  // N/A est converti en null par nullValues
        "'', false"     // Chaine vide (les guillemets empechent CSV de l'ignorer)
    }, nullValues = "N/A")
    void emailsAvecNull(String email, boolean attendu) {
        assertEquals(attendu, validateur.estEmailValide(email));
    }

    // ================================================================
    // @EnumSource — Iteration sur les valeurs d'un enum
    // ================================================================

    /**
     * Enum interne pour la demonstration de @EnumSource.
     *
     * Les valeurs de cet enum seront automatiquement iterees
     * par @EnumSource sans avoir a les lister manuellement.
     */
    enum StatutUtilisateur { ACTIF, INACTIF, SUSPENDU, SUPPRIME }

    /**
     * Test avec @EnumSource en mode EXCLUDE.
     *
     * EXCLUDE : exécuté le test pour TOUTES les valeurs SAUF celles listees.
     * Ici, le test s'exécuté pour ACTIF, INACTIF, SUSPENDU (pas SUPPRIME).
     *
     * Utile quand on veut tester "tout sauf un cas particulier".
     */
    @ParameterizedTest
    @DisplayName("Tous les statuts sauf SUPPRIME sont valides")
    @EnumSource(value = StatutUtilisateur.class, mode = EXCLUDE, names = "SUPPRIME")
    void statutsValides(StatutUtilisateur statut) {
        assertNotEquals(StatutUtilisateur.SUPPRIME, statut,
            "Tous les statuts sauf SUPPRIME devraient passer");
    }

    /**
     * Test avec @EnumSource en mode INCLUDE.
     *
     * INCLUDE : exécuté le test UNIQUEMENT pour les valeurs listees.
     * Ici, seuls ACTIF et SUSPENDU sont testes.
     *
     * Utile pour tester un sous-ensemble spécifique.
     */
    @ParameterizedTest
    @DisplayName("Seuls ACTIF et SUSPENDU sont testes ici")
    @EnumSource(value = StatutUtilisateur.class, mode = INCLUDE, names = {"ACTIF", "SUSPENDU"})
    void statutsSpecifiques(StatutUtilisateur statut) {
        assertTrue(statut == StatutUtilisateur.ACTIF || statut == StatutUtilisateur.SUSPENDU);
    }

    // ================================================================
    // @CsvFileSource — Donnees depuis un fichier CSV externe
    // ================================================================

    /**
     * Test avec @CsvFileSource qui lit un fichier CSV externe.
     *
     * Le fichier est cherche dans src/test/resources/.
     * Le '/' initial indique la racine du classpath.
     *
     * numLinesToSkip = 1 : ignore la première ligne (en-tete du CSV).
     *
     * Format du fichier telephones-test.csv :
     *   telephone,validé
     *   0612345678,true
     *   061234567,false
     *   ...
     *
     * @see src/test/resources/telephones-test.csv
     */
    @ParameterizedTest(name = "Telephone \"{0}\" validé -> {1}")
    @DisplayName("Validation telephones via fichier CSV externe")
    @CsvFileSource(resources = "/telephones-test.csv", numLinesToSkip = 1)
    void telephonesDepuisFichier(String telephone, boolean attendu) {
        assertEquals(attendu, validateur.estTelephoneValide(telephone),
            "Échec pour le telephone : " + telephone);
    }

    // ================================================================
    // @MethodSource — Arguments complexes via méthode factory
    // ================================================================

    /**
     * Test paramètre avec @MethodSource.
     *
     * La méthode referencee doit etre static et retourner un
     * Stream<Arguments>. Chaque Arguments.of(...) contient les paramètrès
     * dans l'ordre de la méthode de test.
     *
     * Avantages par rapport a @CsvSource :
     * - Les types sont preserves (pas de conversion String -> int)
     * - La logique de generation des donnees peut etre complexe
     * - Les objets non-String peuvent etre passes directement
     *
     * Ici, on teste les 6 categories d'age avec leurs bornes.
     * Chaque ligne teste un cas limite important.
     */
    @ParameterizedTest(name = "{0} ans -> catégorie \"{1}\"")
    @DisplayName("Categorisation par age (via @MethodSource)")
    @MethodSource("fournirAgesEtCategories")
    void categorisationAge(int age, String categorieAttendue) {
        assertEquals(categorieAttendue, validateur.categorieAge(age),
            "Categorie incorrecte pour l'age " + age);
    }

    /**
     * Methode factory pour @MethodSource.
     *
     * Retourne un flux d'arguments couvrant toutes les bornes de categories.
     * Chaque Arguments contient un age (int) et la catégorie attendue (String).
     *
     * Les bornes sont choisies pour tester les changements de catégorie :
     * - 0 et 17 : MINEUR
     * - 18 et 24 : JEUNE_ADULTE
     * - 25 et 59 : ADULTE
     * - 60 et 119 : SENIOR
     * - 120 et 150 : CENTENAIRE
     */
    static Stream<Arguments> fournirAgesEtCategories() {
        return Stream.of(
            Arguments.of(0, "MINEUR"),       // Age minimum possible
            Arguments.of(17, "MINEUR"),      // Derniere annee mineure
            Arguments.of(18, "JEUNE_ADULTE"), // Premier age adulte
            Arguments.of(24, "JEUNE_ADULTE"), // Derniere annee jeune adulte
            Arguments.of(25, "ADULTE"),       // Premier age adulte
            Arguments.of(59, "ADULTE"),       // Derniere annee adulte
            Arguments.of(60, "SENIOR"),       // Premier age senior
            Arguments.of(119, "SENIOR"),      // Derniere annee senior
            Arguments.of(120, "CENTENAIRE"),   // Premier age centenaire
            Arguments.of(150, "CENTENAIRE")    // Au-dela de 120
        );
    }

    /**
     * Test avec @MethodSource retournant Stream<Integer> (pas Stream<Arguments>).
     *
     * Quand la méthode de test n'a qu'un seul paramètre, la factory
     * peut retourner Stream<T> directement au lieu de Stream<Arguments>.
     * C'est plus simple et plus lisible pour les tests a un paramètre.
     */
    @ParameterizedTest
    @DisplayName("Ages valides (via @MethodSource d'entiers)")
    @MethodSource("agesValides")
    void agesValides(int age) {
        assertTrue(validateur.estAgeValide(age),
            "L'age " + age + " devrait etre validé");
    }

    /**
     * Factory retournant directement des entiers (1 paramètre).
     * Pas besoin de Arguments.of() car un seul paramètre.
     */
    static Stream<Integer> agesValides() {
        return Stream.of(18, 25, 30, 60, 100, 120);
    }

    // ================================================================
    // @NullSource / @EmptySource / @NullAndEmptySource
    // ================================================================

    /**
     * @NullAndEmptySource combine @NullSource et @EmptySource.
     *
     * Le test est exécuté DEUX fois :
     * - Une fois avec email = null
     * - Une fois avec email = ""
     *
     * Verifier le comportement avec null et vide est essentiel
     * pour eviter les NullPointerException en production.
     */
    @ParameterizedTest
    @DisplayName("Email invalide pour null et chaine vide")
    @NullAndEmptySource
    void emailNullOuVide(String email) {
        assertFalse(validateur.estEmailValide(email));
    }

    /**
     * Combinaison de @NullSource et @EmptySource (equivalent a @NullAndEmptySource).
     *
     * Les deux annotations sont independantes et peuvent etre combinees
     * librement avec d'autres sources comme @ValueSource.
     */
    @ParameterizedTest
    @DisplayName("Telephone invalide pour null ET chaine vide")
    @NullSource
    @EmptySource
    void telephoneNullEtVide(String telephone) {
        assertFalse(validateur.estTelephoneValide(telephone));
    }

    /**
     * Combinaison de @NullAndEmptySource avec @ValueSource.
     *
     * Le test est exécuté TROIS fois :
     * - null
     * - "" (chaine vide)
     * - " " (une espace, via @ValueSource)
     *
     * On vérifié que les entrees non significatives donnent un score de 0.
     */
    @ParameterizedTest
    @DisplayName("Score = 0 pour null et chaine vide")
    @NullAndEmptySource
    @ValueSource(strings = " ")  // Une espace -> score 0
    void scoreZeroPourEntreesInvalides(String mdp) {
        assertEquals(0, validateur.scoreMotDePasse(mdp));
    }

    // ================================================================
    // Tests supplementaires
    // ================================================================

    /**
     * Test des formats de telephone varies.
     *
     * @CsvSource permet de lister plusieurs formats sur une seule ligne.
     * Verifie que le nettoyage (espaces, points, tirets) fonctionne.
     */
    @ParameterizedTest(name = "Telephone \"{0}\" doit etre validé")
    @DisplayName("Telephones valides (formats varies)")
    @CsvSource({
        "0612345678",           // Format compact
        "06 12 34 56 78",       // Avec espaces
        "06.12.34.56.78",       // Avec points
        "+33612345678",         // Format international compact
        "+33 6 12 34 56 78"     // Format international avec espaces
    })
    void telephonesValidesFormatsVaries(String telephone) {
        assertTrue(validateur.estTelephoneValide(telephone),
            "Format attendu validé : " + telephone);
    }

    /**
     * Demonstration de la conversion automatique de types de JUnit.
     *
     * Les valeurs dans le CSV sont des String :
     *   "18, true"  -> JUnit convertit "18" en int 18 et "true" en boolean true
     *
     * Cette conversion fonctionne pour :
     * - String -> int, long, double, float
     * - String -> boolean ("true"/"false")
     * - String -> enum (le nom de la constante)
     * - Et d'autres types via des convertisseurs personnalises
     *
     * Pas besoin de Integer.parseInt() ou Boolean.parseBoolean() !
     */
    @ParameterizedTest
    @DisplayName("Conversion automatique : String -> int -> boolean")
    @CsvSource({
        "18, true",    // Age validé (18-120)
        "17, false",   // Trop jeune
        "120, true",   // Age maximum validé
        "121, false",  // Trop vieux
        "0, false"     // Bien en dessous du minimum
    })
    void conversionAutoTypes(int age, boolean attendu) {
        assertEquals(attendu, validateur.estAgeValide(age));
    }
}
