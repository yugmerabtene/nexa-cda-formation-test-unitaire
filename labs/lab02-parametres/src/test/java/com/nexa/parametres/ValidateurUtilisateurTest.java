package com.nexa.parametres;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.junit.jupiter.params.provider.EnumSource.Mode.INCLUDE;

@DisplayName("Tests paramétrés du ValidateurUtilisateur")
class ValidateurUtilisateurTest {

    private final ValidateurUtilisateur validateur = new ValidateurUtilisateur();

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
        "",           

        "pasd'arobase",
        "@domaine.com",   

        "user@",         

        "user@domaine",  

        "user@@domaine.com" 

    })
    void emailsInvalides(String email) {
        assertFalse(validateur.estEmailValide(email),
            () -> "L'email '" + email + "' devrait être invalide");
    }

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

    @ParameterizedTest(name = "email = {0} est valide → {1}")
    @DisplayName("Emails : cas limites avec null")
    @CsvSource(value = {
        "N/A, false",
        "'', false"
    }, nullValues = "N/A")
    void emailsAvecNull(String email, boolean attendu) {
        assertEquals(attendu, validateur.estEmailValide(email));
    }

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

    @ParameterizedTest(name = "Téléphone \"{0}\" valide → {1}")
    @DisplayName("Validation téléphones via fichier CSV externe")
    @CsvFileSource(resources = "/telephones-test.csv", numLinesToSkip = 1)
    void telephonesDepuisFichier(String telephone, boolean attendu) {
        assertEquals(attendu, validateur.estTelephoneValide(telephone),
            "Échec pour le téléphone : " + telephone);
    }

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
    @ValueSource(strings = " ") 

    void scoreZeroPourEntreesInvalides(String mdp) {
        assertEquals(0, validateur.scoreMotDePasse(mdp));
    }

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
