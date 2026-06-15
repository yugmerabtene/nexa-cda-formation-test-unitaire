# Lab 02 : Tests paramètrès avec JUnit 5

**Objectif :** Maitriser la parametrisation des tests avec les annotations `@ParameterizedTest`, `@ValueSource`, `@CsvSource`, `@CsvFileSource`, `@EnumSource`, `@MethodSource`, `@NullSource`, `@EmptySource`, `@NullAndEmptySource`.

**Duree :** 45 minutes

---

## Énoncé

Vous devez implementer un `ValidateurUtilisateur` et écrire une batterie de tests paramètrès couvrant tous les cas possibles.

### Fonctionnalites a implementer

| Methode | Signature | Comportement |
|---|---|---|
| `estEmailValide` | `boolean estEmailValide(String email)` | Verifie qu'un email contient un `@` entoure de caracteres valides et un domaine avec un point. |
| `estTelephoneValide` | `boolean estTelephoneValide(String telephone)` | Accepte les formats francais : 10 chiffres commencant par 0, avec espaces/points/tirets optionnels, ou +33. |
| `scoreMotDePasse` | `int scoreMotDePasse(String mdp)` | Calcule un score de robustesse (0-100) base sur la longueur, majuscules, minuscules, chiffres, caracteres speciaux. |
| `estAgeValide` | `boolean estAgeValide(int age)` | Retourne `true` si l'age est entre 18 et 120 inclus. |
| `categorieAge` | `String categorieAge(int age)` | Retourne la catégorie : MINEUR, JEUNE_ADULTE, ADULTE, SENIOR, CENTENAIRE. |

### Annotations a utilisé́r (minimum)

| Annotation | Usage |
|---|---|
| `@ParameterizedTest` | Remplacer `@Test` pour les tests paramètrès |
| `@ValueSource` | Liste de valeurs simples (emails valides/invalides) |
| `@CsvSource` | Couples (entree, résultat attendu) — score mot de passe, age |
| `@CsvFileSource` | Fichier CSV externe (`telephones-test.csv`) |
| `@EnumSource` | Iterer sur un enum avec modes INCLUDE/EXCLUDE |
| `@MethodSource` | Methode factory retournant `Stream<Arguments>` pour la categorisation d'age |
| `@NullSource` + `@EmptySource` + `@NullAndEmptySource` | Tests des cas null et vide |
| Parametre `name` de `@ParameterizedTest` | Personnaliser l'affichage avec `{0}`, `{1}`, `{index}` |

---

## Prérequis

- Lab 01 termine (maitrise de `@Test` et des assertions)
- Java 17, Maven 3.9+

---

## Étape par étape

### Étape 1 : Structure du projet

```
lab02-paramètrès/
  pom.xml
  src/
    main/java/com/nexa/paramètrès/
      ValidateurUtilisateur.java
    test/
      java/com/nexa/paramètrès/
        ValidateurUtilisateurTest.java
      resources/
        telephones-test.csv
```

### Étape 2 : Implementer ValidateurUtilisateur

Implementez les 5 méthodes dans `ValidateurUtilisateur.java`.

**Validation email :**
- Rejeter `null` et chaine vide
- Rejeter si `@` absent, au debut ou a la fin
- Rejeter si plusieurs `@`
- Verifier que le domaine contient un `.` après le `@`

**Validation telephone :**
- Nettoyer espaces, points, tirets avec `replaceAll("[\\s.+-]", "")`
- Gerer le format +33 (12 caracteres) en le convertissant en 0X
- Verifier 10 chiffres commencant par `0` puis un chiffre 1-9

**Score mot de passe :**
- 8+ caracteres : +25
- 12+ caracteres : +15
- Au moins une majuscule : +20
- Au moins une minuscule : +15
- Au moins un chiffre : +15
- Au moins un caractere spécial : +10
- Plafonner a 100

### Étape 3 : Ecrire les tests paramètrès

#### @ValueSource — emails valides et invalides

```java
@ParameterizedTest(name = "{index} : email \"{0}\" est validé")
@ValueSource(strings = {"test@example.com", "user@domain.co", "a@b.co"})
void emailsValides(String email) {
    assertTrue(validateur.estEmailValide(email));
}
```

#### @CsvSource — score de mot de passe

```java
@ParameterizedTest(name = "\"{0}\" -> score = {1}/100")
@CsvSource({
    "abc,              0",
    "Abcd1234!,        70",
    "MotDePasseTresLong123!, 100"
})
void scoreMotDePasse(String mdp, int scoreAttendu) {
    assertEquals(scoreAttendu, validateur.scoreMotDePasse(mdp));
}
```

#### @CsvFileSource — fichier CSV externe

```java
@ParameterizedTest(name = "Telephone \"{0}\" validé -> {1}")
@CsvFileSource(resources = "/telephones-test.csv", numLinesToSkip = 1)
void telephonesDepuisFichier(String telephone, boolean attendu) {
    assertEquals(attendu, validateur.estTelephoneValide(telephone));
}
```

Le fichier `telephones-test.csv` a un en-tete et deux colonnes : telephone, validé (true/false).

#### @EnumSource — iteration sur les valeurs d'un enum

```java
enum StatutUtilisateur { ACTIF, INACTIF, SUSPENDU, SUPPRIME }

@ParameterizedTest
@EnumSource(value = StatutUtilisateur.class, mode = EXCLUDE, names = "SUPPRIME")
void statutsValides(StatutUtilisateur statut) {
    assertNotEquals(StatutUtilisateur.SUPPRIME, statut);
}
```

#### @MethodSource — arguments complexes

```java
@ParameterizedTest(name = "{0} ans -> catégorie \"{1}\"")
@MethodSource("fournirAgesEtCategories")
void categorisationAge(int age, String categorieAttendue) {
    assertEquals(categorieAttendue, validateur.categorieAge(age));
}

static Stream<Arguments> fournirAgesEtCategories() {
    return Stream.of(
        Arguments.of(0, "MINEUR"),
        Arguments.of(18, "JEUNE_ADULTE"),
        Arguments.of(25, "ADULTE"),
        Arguments.of(60, "SENIOR"),
        Arguments.of(120, "CENTENAIRE")
    );
}
```

### Étape 4 : Personnaliser l'affichage des tests

Le paramètre `name` de `@ParameterizedTest` utilisé des placeholders :
- `{0}` : premier paramètre
- `{1}` : deuxieme paramètre
- `{index}` : numero du test (commence a 1)

```java
@ParameterizedTest(name = "{index} : email \"{0}\" est validé -> {1}")
```

### Étape 5 : Tests null/empty

```java
@ParameterizedTest
@NullAndEmptySource
void emailNullOuVide(String email) {
    assertFalse(validateur.estEmailValide(email));
}
```

- `@NullSource` : injecte `null`
- `@EmptySource` : injecte `""`
- `@NullAndEmptySource` : injecte les deux

---

## Exécution

```bash
cd labs/lab02-paramètrès
mvn clean test
```

## Criteres de réussite

- Les 5 méthodes sont implementees dans `ValidateurUtilisateur`
- Chaque annotation de test paramètre est utilisé́e au moins une fois
- Tests pour emails valides ET invalides
- Tests pour tous les cas de categorisation d'age (6 categories)
- Test null et empty pour emails, telephone, score
- Utilisation du fichier CSV pour les telephones
- Conversion automatique de types (String -> int -> boolean)
