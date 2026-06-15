# Module 2 : Tests Paramétrés avec JUnit 5

**Durée : 1h30 (10h45–12h15)**

---

## Objectifs pédagogiques

À l'issue de ce module, vous serez capable de :

1. **Expliquer** la différence entre `@Test` et `@ParameterizedTest` et quand utiliser chacun.
2. **Utiliser** `@ValueSource` pour tester une méthode avec une liste de valeurs simples (int, string, long, double).
3. **Utiliser** `@CsvSource` et `@CsvFileSource` pour injecter des couples (entrée, résultat attendu) dans un test.
4. **Utiliser** `@EnumSource` pour itérer sur les valeurs d'une énumération avec les modes INCLUDE et EXCLUDE.
5. **Utiliser** `@MethodSource` pour fournir des arguments complexes via une méthode factory statique retournant un `Stream<Arguments>`.
6. **Combiner** `@NullSource`, `@EmptySource` et `@NullAndEmptySource` pour tester les cas de nullité et chaînes vides.
7. **Expliquer** le mécanisme de conversion automatique des types de JUnit (String → int, String → boolean, etc.).
8. **Personnaliser** l'affichage des tests paramétrés avec le paramètre `name` de `@ParameterizedTest`.

---

## Prérequis

- **Module 1** : maîtrise de `@Test`, des assertions fondamentales, et du pattern AAA.
- **Java** : compréhension des énumérations (`enum`), des Streams (`Stream<T>`), et des lambdas.

---

## PARTIE 1 -- THEORIE (45 min)

---

## 1.1 Pourquoi les tests paramétrés ?

### Le problème des tests répétitifs

Imaginez que vous deviez tester une méthode `estEmailValide(String email)`. Vous voulez vérifier qu'elle accepte 5 emails valides et rejette 7 emails invalides.

Avec `@Test`, vous devriez écrire 12 méthodes distinctes :

```java
@Test void emailValide1() { assertTrue(validateur.estEmailValide("test@example.com")); } // @Test classique : un test par email
@Test void emailValide2() { assertTrue(validateur.estEmailValide("user@domain.co")); } // Chaque méthode teste un seul cas
@Test void emailValide3() { assertTrue(validateur.estEmailValide("a@b.co")); } // assertTrue : vérifie que la condition est vraie
// ... 9 autres méthodes ... // Approche verbeuse : 12 méthodes pour 12 cas
```

Ou alors tout mettre dans une seule méthode :

```java
@Test void emailsValides() { // @Test classique avec plusieurs assertions dans une seule méthode
 assertTrue(validateur.estEmailValide("test@example.com")); // Vérifie le premier email
 assertTrue(validateur.estEmailValide("user@domain.co")); // Vérifie le deuxième email
 // Si la première assertion échoue, les autres ne sont jamais exécutées ! // Problème : pas d'isolation des cas
}
```

Aucune de ces approches n'est satisfaisante. La première est verbeuse, la seconde perd toute visibilité sur quel cas a échoué.

### La solution : `@ParameterizedTest`

Un test paramétré permet d'exécuter la **même logique de test** avec **plusieurs jeux de données**. C'est l'équivalent d'une boucle `for` sur des assertions, mais avec la puissance du framework JUnit : chaque jeu de données est traité comme un test **indépendant**, avec son propre nom, son propre résultat, et si un jeu échoue les autres continuent.

```java
@ParameterizedTest // Remplace @Test : indique que la méthode est un test paramétré (org.junit.jupiter.params.ParameterizedTest)
@ValueSource(strings = {"test@example.com", "user@domain.co", "a@b.co"}) // Source d'arguments : tableau de chaînes, fournit 3 valeurs
void emailEstValide(String email) { // La méthode prend un paramètre String, injecté par @ValueSource
 assertTrue(validateur.estEmailValide(email)); // assertTrue : vérifie que l'email est valide pour chaque valeur injectée
}
```

**Résultat** : 3 tests exécutés, affichés séparément dans le rapport, avec le nom de chaque valeur. Si un seul échoue, vous savez exactement lequel.

### Avantages des tests paramétrés

1. **Moins de code** : une seule méthode au lieu de N.
2. **Ajout facile de cas** : ajouter une chaîne dans `@ValueSource` est immédiat.
3. **Isolation** : chaque jeu de données est un test indépendant — un échec n'empêche pas les autres.
4. **Rapports clairs** : chaque paramètre apparaît dans le nom du test.
5. **Conversion automatique** : JUnit convertit automatiquement les `String` en `int`, `boolean`, `double`, etc.

---

## 1.2 `@ParameterizedTest` : la base

L'annotation `@ParameterizedTest` remplace `@Test`. Elle vient de `org.junit.jupiter.params.ParameterizedTest`.

### Contrat

- La méthode doit être `void` (comme `@Test`).
- La méthode prend des **paramètres** correspondant aux valeurs injectées.
- Elle doit être accompagnée d'au moins une **source d'arguments** (`@ValueSource`, `@CsvSource`, etc.), sans quoi JUnit ne saura pas quoi passer.

### Le paramètre `name`

Le paramètre `name` de `@ParameterizedTest` permet de contrôler l'affichage du test dans les rapports :

```java
@ParameterizedTest(name = "{index} : email \"{0}\" est valide → {1}") // name : personnalise l'affichage du test. {index}=numéro, {0}=1er paramètre, {1}=2ème paramètre
```

**Placeholders disponibles** :

| Placeholder | Signification | Exemple |
|-------------|---------------|---------|
| `{index}` | Numéro du jeu de données (commence à 1) | `1`, `2`, `3` |
| `{0}` | Premier paramètre de la méthode | `"test@example.com"` |
| `{1}` | Deuxième paramètre de la méthode | `true` |
| `{n}` | n-ième paramètre | ... |
| `{arguments}` | Liste complète des arguments, séparés par des virgules | `test@example.com, true` |

**Exemple concret** :

```java
@ParameterizedTest(name = "{index} : email \"{0}\" est valide → {1}") // {index}=numéro du jeu (débute à 1), {0}=premier paramètre (email), {1}=deuxième paramètre (booléen attendu)
@DisplayName("Validation d'emails valides") // @DisplayName : nom lisible du test dans les rapports
@CsvSource({"test@example.com, true", "pasd'arobase, false"}) // @CsvSource : fournit des couples (email, résultatAttendu) au format CSV
void testEmail(String email, boolean resultatAttendu) { // email = 1ère colonne CSV, resultatAttendu = 2ème colonne CSV (converti String→boolean automatiquement)
 // ... // Corps du test
}
```

Avec `name` personnalisé, le rapport affichera :
```
[1] 1 : email "test@example.com" est valide → true
[2] 2 : email "pasd'arobase" est valide → false
```

Sans `name`, le rapport afficherait simplement `[1] test@example.com, true`, ce qui est moins informatif.

---

## 1.3 `@ValueSource` : la source la plus simple

`@ValueSource` fournit un tableau de valeurs littérales d'un seul type. C'est la source d'arguments la plus simple et la plus utilisée pour les tests à un seul paramètre.

### Types supportés

| Attribut | Type fourni | Exemple |
|----------|-------------|---------|
| `strings` | `String` | `@ValueSource(strings = {"a", "b", "c"})` |
| `ints` | `int` (primitif) | `@ValueSource(ints = {1, 2, 3})` |
| `longs` | `long` (primitif) | `@ValueSource(longs = {1L, 2L, 3L})` |
| `doubles` | `double` (primitif) | `@ValueSource(doubles = {1.1, 2.2})` |
| `floats` | `float` (primitif) | `@ValueSource(floats = {1.1f, 2.2f})` |
| `shorts` | `short` (primitif) | `@ValueSource(shorts = {1, 2, 3})` |
| `bytes` | `byte` (primitif) | `@ValueSource(bytes = {1, 2, 3})` |
| `chars` | `char` (primitif) | `@ValueSource(chars = {'A', 'B', 'C'})` |
| `classes` | `Class<?>` | `@ValueSource(classes = {String.class, Integer.class})` |
| `booleans` | `boolean` (primitif) | `@ValueSource(booleans = {true, false})` |

### Exemple : emails valides

```java
@ParameterizedTest(name = "{index} : email \"{0}\" est valide → {1}") // name : personnalise l'affichage, {index}=numéro, {0}=email
@DisplayName("Validation d'emails valides") // @DisplayName : nom du test dans le rapport
@ValueSource(strings = { // @ValueSource : fournit un tableau de 5 chaînes = 5 exécutions du test
 "test@example.com", // Email valide basique
 "user.name@domain.co", // Email avec point dans la partie locale
 "a@b.co", // Email minimal (domaine très court)
 "contact@entreprise.fr", // Email avec TLD français
 "nom.prenom@site.gouv.fr" // Email avec sous-domaine et TLD .gouv.fr
})
void emailsValides(String email) { // Prend un paramètre String injecté par @ValueSource
 assertTrue(validateur.estEmailValide(email), // assertTrue : vérifie que l'email est valide (retourne true)
 () -> "L'email '" + email + "' devrait être valide"); // Supplier<String> : message paresseux, évalué seulement en cas d'échec
}
```

**Analyse** :
- `@ValueSource(strings = {...})` : fournit un tableau de 5 chaînes.
- La méthode `emailsValides(String email)` prend un paramètre `String email` : pour chaque chaîne du tableau, JUnit crée une instance de test et appelle la méthode avec cette chaîne.
- Le `name` utilise `{index}` et `{0}` pour afficher un numéro et la valeur.
- Résultat : 5 tests exécutés, chacun avec un email différent.

**Avantage du Supplier dans le message** : le message utilise `() -> "L'email '" + email + "' devrait être valide"`. En cas de succès, la lambda n'est pas exécutée. En cas d'échec, le message indique exactement quel email a posé problème.

### Exemple : emails invalides

```java
@ParameterizedTest(name = "\"{0}\" → email INVALIDE") // name : personnalise l'affichage, {0}=email testé
@DisplayName("Validation d'emails invalides") // @DisplayName : nom du test dans le rapport
@ValueSource(strings = { // @ValueSource : fournit 6 chaînes invalides = 6 exécutions
 "", // Chaîne vide → test de la condition isEmpty()
 "pasd'arobase", // Pas de @ → test de arobaseIndex <= 0 (indexOf retourne -1)
 "@domaine.com", // @ en première position → arobaseIndex == 0 <= 0 → false
 "user@", // @ en dernière position, domaine vide → pas de point dans le domaine
 "user@domaine", // Pas de point après le @ → domaine.contains(".") == false
 "user@@domaine.com" // Double @ → test du deuxième @ détecté par indexOf('@', arobaseIndex + 1)
})
void emailsInvalides(String email) { // Paramètre String injecté par @ValueSource
 assertFalse(validateur.estEmailValide(email), // assertFalse : vérifie que l'email est INVALIDE (retourne false)
 () -> "L'email '" + email + "' devrait être invalide"); // Supplier<String> : message paresseux, évalué en cas d'échec seulement
}
```

**Analyse des cas invalides** :
- `""` : chaîne vide — test de la condition `if (email == null || email.isEmpty()) return false;`
- `"pasd'arobase"` : pas d'`@` — test de `if (arobaseIndex <= 0) return false;`
- `"@domaine.com"` : `@` en première position (index 0) — `arobaseIndex <= 0` est vrai (0 <= 0), donc rejeté.
- `"user@"` : `@` en dernière position, pas de point après — `domaine.contains(".")` est faux, donc rejeté.
- `"user@domaine"` : pas de point dans le domaine — même raison.
- `"user@@domaine.com"` : deux `@` — test de `if (email.indexOf('@', arobaseIndex + 1) != -1) return false;`

---

## 1.4 `@CsvSource` : couples (entrée, sortie attendue)

`@CsvSource` est la source la plus puissante pour les tests à plusieurs paramètres. Elle permet de spécifier un tableau de chaînes au format CSV (Comma-Separated Values), où chaque chaîne représente un jeu de paramètres.

### Syntaxe

```java
@CsvSource({ // @CsvSource : source d'arguments au format CSV (Comma-Separated Values)
 "valeur1, valeur2, valeur3", // 3 paramètres pour le test 1 : chaque colonne = un paramètre de la méthode
 "valeur4, valeur5, valeur6" // 3 paramètres pour le test 2
})
```

Par défaut, le délimiteur est la virgule. Chaque ligne produit une exécution du test.

### Options avancées

| Option | Description | Exemple |
|--------|-------------|---------|
| `delimiter` | Change le délimiteur | `delimiter = ';'` pour du CSV à point-virgule |
| `nullValues` | Définit les valeurs à interpréter comme `null` | `nullValues = "N/A"` |
| `textBlock` | Permet d'utiliser la syntaxe de bloc texte (Java 15+) | (pas utilisé dans ce lab) |
| `quoteCharacter` | Change le caractère de citation | `quoteCharacter = '\''` |
| `useHeadersInDisplayName` | Utilise la première ligne comme en-tête | (pas utilisé) |

### Exemple : score de mot de passe

```java
@ParameterizedTest(name = "\"{0}\" → score = {1}/100") // name : {0}=motDePasse, {1}=score attendu
@DisplayName("Score de robustesse des mots de passe") // @DisplayName : nom du test
@CsvSource({ // @CsvSource : chaque ligne = un couple (motDePasse String, scoreAttendu int)
 "abc, 0", // 3 caractères, rien de spécial → score = 0
 "abcd1234, 40", // ≥8 car (+25) + minuscules (+15) → 40
 "Abcd1234, 60", // ≥8 car (+25) + minuscules (+15) + majuscule (+20) → 60
 "Abcd1234!, 70", // ≥8 car (+25) + minuscule (+15) + majuscule (+20) + spécial (+10) → 70
 "MotDePasseTresLong123!, 100", // ≥12 car (+25+15) + tout le reste → 100 (plafonné)
 "12345678, 25" // ≥8 car (+25) mais <12, pas de lettres → 25
})
void scoreMotDePasse(String motDePasse, int scoreAttendu) { // motDePasse en String, scoreAttendu en int (conversion auto)
 assertEquals(scoreAttendu, validateur.scoreMotDePasse(motDePasse), // assertEquals : compare le score calculé au score attendu. Paramètres : (attendu, réel)
 "Score incorrect pour '" + motDePasse + "'"); // Message en cas d'échec
}
```

**Analyse détaillée** :

Chaque ligne de `@CsvSource` a deux colonnes : le mot de passe (String) et le score attendu (int).

1. **`"abc", 0`** : 3 caractères, pas de majuscule, pas de chiffre, pas de caractère spécial → score = 0.
2. **`"abcd1234", 40`** : ≥ 8 caractères (+25), contient des minuscules (+15) → score = 40.
3. **`"Abcd1234", 60`** : ≥ 8 caractères (+25), contient des majuscules (+20), contient des minuscules (+15) → score = 60.
4. **`"Abcd1234!", 70`** : ≥ 8 caractères (+25), majuscule (+20), minuscule (+15), caractère spécial (+10) → score = 70.
5. **`"MotDePasseTresLong123!", 100`** : ≥ 12 caractères (+25+15), majuscule (+20), minuscule (+15), chiffre (+15), spécial (+10) → score = 100 (plafonné par `Math.min(score, 100)`).
6. **`"12345678", 25`** : ≥ 8 caractères (+25) mais < 12, pas de majuscule ni minuscule → score = 25.

**Conversion automatique** : remarquez que le deuxième paramètre de la méthode est `int scoreAttendu`. JUnit lit `"40"` (une String dans le CSV) et le convertit automatiquement en `int 40`.

**Pourquoi les espaces sont nombreux ?** Les espaces avant/après les virgules sont ignorés par défaut par le parser CSV de JUnit. Cela rend le tableau plus lisible.

### Exemple avec `nullValues`

```java
@ParameterizedTest(name = "email = {0} est valide → {1}") // name : {0}=email, {1}=booléen attendu
@DisplayName("Emails : cas limites avec null") // @DisplayName : nom du test
@CsvSource(value = { // @CsvSource avec option nullValues pour gérer les valeurs null
 "N/A, false", // "N/A" sera interprété comme null grâce à nullValues
 "'', false" // '' (deux apostrophes) = chaîne vide "" dans le format CSV JUnit
}, nullValues = "N/A") // nullValues : définit la chaîne "N/A" comme représentant la valeur null Java
void emailsAvecNull(String email, boolean attendu) { // email peut être null (via nullValues). attendu = true/false
 assertEquals(attendu, validateur.estEmailValide(email)); // assertEquals : compare le booléen attendu au résultat de estEmailValide(email)
}
```

**Analyse** :

- `nullValues = "N/A"` indique à JUnit que la chaîne `"N/A"` doit être interprétée comme `null`.
- Première ligne : `N/A, false` → `email = null, attendu = false`. Le test vérifie que `estEmailValide(null)` retourne `false`.
- Deuxième ligne : `''` → `email = ""` (chaîne vide), `attendu = false`. Le test vérifie que `estEmailValide("")` retourne `false`.

**Pourquoi utiliser `nullValues` ?** Parce qu'il est impossible d'écrire `null` dans un fichier CSV. JUnit fournit ce mécanisme pour représenter la nullité.

**Pourquoi `''` pour une chaîne vide ?** Dans le format CSV de JUnit, deux apostrophes consécutives `''` représentent une chaîne vide. Sans les apostrophes, le parser pourrait confondre avec une valeur `null` ou une chaîne inexistante.

---

## 1.5 `@CsvFileSource` : données depuis un fichier CSV externe

Quand le nombre de cas de test devient important, les mettre dans l'annotation `@CsvSource` rend le code illisible. `@CsvFileSource` permet de lire les données depuis un fichier CSV externe situé dans `src/test/resources/`.

### Attributs

| Attribut | Description | Défaut |
|----------|-------------|--------|
| `resources` | Chemin vers le fichier CSV (dans `src/test/resources/`) | Obligatoire |
| `numLinesToSkip` | Nombre de lignes à ignorer en début de fichier (pour l'en-tête) | 0 |
| `encoding` | Encodage du fichier | "UTF-8" |
| `delimiter` | Caractère délimiteur | ',' |
| `nullValues` | Valeurs à interpréter comme null | "" |
| `lineSeparator` | Séparateur de lignes | "\n" |

### Exemple : validation de téléphones

**Le fichier CSV** (`src/test/resources/telephones-test.csv`) :

```csv
telephone,valide
0612345678,true
0612345678,true
0612345678,true
+33612345678,true
06 12 34 56 78,true
0123456789,true
999999,false
061234567,false
06123456789,false
abc,false
```

**Le test** :

```java
@ParameterizedTest(name = "Téléphone \"{0}\" valide → {1}") // name : {0}=téléphone, {1}=booléen attendu
@DisplayName("Validation téléphones via fichier CSV externe") // @DisplayName : nom du test
@CsvFileSource(resources = "/telephones-test.csv", numLinesToSkip = 1) // @CsvFileSource : lit le fichier CSV dans src/test/resources/. numLinesToSkip=1 saute l'en-tête
void telephonesDepuisFichier(String telephone, boolean attendu) { // telephone = 1ère colonne, attendu = 2ème colonne (converti String→boolean)
 assertEquals(attendu, validateur.estTelephoneValide(telephone), // assertEquals : compare le booléen attendu au résultat de estTelephoneValide()
 "Échec pour le téléphone : " + telephone); // Message en cas d'échec, inclut le téléphone testé
}
```

**Analyse** :

- `resources = "/telephones-test.csv"` : le fichier est cherché dans `src/test/resources/`. Le slash initial `/` indique la racine du classpath.
- `numLinesToSkip = 1` : la première ligne (`telephone,valide`) est l'en-tête et n'est pas une donnée. On la saute.
- Chaque ligne suivante fournit un `telephone` (String) et un `attendu` (boolean). JUnit convertit automatiquement `"true"` en `true` (boolean) et `"false"` en `false`.
- La méthode est appelée une fois par ligne (soit 10 exécutions).

**Analyse des cas de test du CSV** :

| Ligne | Téléphone | Valide | Pourquoi |
|-------|-----------|--------|----------|
| 2 | `0612345678` | true | 10 chiffres, commence par 0 |
| 3 | `0612345678` | true | (duplication volontaire pour insister sur les tests) |
| 4 | `0612345678` | true | (idem) |
| 5 | `+33612345678` | true | Format international, converti en `0612345678` |
| 6 | `06 12 34 56 78` | true | Avec espaces (nettoyés par `replaceAll`) |
| 7 | `0123456789` | true | 10 chiffres valides |
| 8 | `999999` | false | Trop court (6 chiffres) |
| 9 | `061234567` | false | 9 chiffres au lieu de 10 |
| 10 | `06123456789` | false | 11 chiffres au lieu de 10 |
| 11 | `abc` | false | Contient des lettres |

**Avantage de `@CsvFileSource` sur `@CsvSource`** :
- Séparation des données et du code.
- Possibilité d'avoir des centaines de cas sans alourdir le fichier Java.
- Facilite la collaboration avec des non-développeurs (un analyste métier peut remplir le CSV).

---

## 1.6 `@EnumSource` : itérer sur les valeurs d'une énumération

`@EnumSource` permet d'injecter successivement chaque valeur d'une énumération dans le test.

### L'énumération de démonstration

```java
enum StatutUtilisateur { ACTIF, INACTIF, SUSPENDU, SUPPRIME } // Énumération de démonstration : 4 valeurs possibles pour le statut d'un utilisateur
```

### Mode EXCLUDE

```java
@ParameterizedTest // Test paramétré (pas de @Test)
@DisplayName("Tous les statuts sauf SUPPRIME sont valides") // @DisplayName : nom du test
@EnumSource(value = StatutUtilisateur.class, mode = EXCLUDE, names = "SUPPRIME") // @EnumSource : itère sur l'énumération. mode=EXCLUDE exclut les noms listés. names="SUPPRIME" = la valeur à exclure
void statutsValides(StatutUtilisateur statut) { // Paramètre de type StatutUtilisateur, injecté automatiquement pour chaque valeur de l'énumération (sauf SUPPRIME)
 assertNotEquals(StatutUtilisateur.SUPPRIME, statut, // assertNotEquals : vérifie que le statut N'EST PAS SUPPRIME. Paramètres : (valeurNonAttendue, valeurRéelle)
 "Tous les statuts sauf SUPPRIME devraient passer"); // Message en cas d'échec
}
```

**Analyse** :
- `value = StatutUtilisateur.class` : l'énumération source.
- `mode = EXCLUDE` : on exclut certaines valeurs.
- `names = "SUPPRIME"` : la valeur à exclure.
- Résultat : le test est exécuté 3 fois, pour `ACTIF`, `INACTIF`, `SUSPENDU`.

### Mode INCLUDE

```java
@ParameterizedTest // Test paramétré
@DisplayName("Seuls ACTIF et SUSPENDU sont testés ici") // @DisplayName : nom du test
@EnumSource(value = StatutUtilisateur.class, mode = INCLUDE, names = {"ACTIF", "SUSPENDU"}) // @EnumSource : mode=INCLUDE inclut SEULEMENT les noms listés. names={"ACTIF", "SUSPENDU"} = tableau des valeurs à inclure
void statutsSpecifiques(StatutUtilisateur statut) { // Paramètre injecté : seulement ACTIF puis SUSPENDU
 assertTrue(statut == StatutUtilisateur.ACTIF || statut == StatutUtilisateur.SUSPENDU); // assertTrue : vérifie que le statut est bien l'une des deux valeurs incluses
}
```

**Analyse** :
- `mode = INCLUDE` : on inclut SEULEMENT les valeurs spécifiées.
- `names = {"ACTIF", "SUSPENDU"}` : un tableau de noms à inclure.
- Résultat : le test est exécuté 2 fois, pour `ACTIF` et `SUSPENDU`.

**Quand utiliser `@EnumSource` ?** Chaque fois que vous avez une méthode qui accepte ou traite une énumération, et que vous voulez tester son comportement pour chaque valeur possible.

---

## 1.7 `@MethodSource` : la source la plus flexible

`@MethodSource` est la source d'arguments la plus puissante. Elle fait référence à une **méthode factory statique** qui retourne les jeux de données. Contrairement aux autres sources qui sont limitées à des types simples et des annotations, `@MethodSource` permet de créer des objets complexes, des listes, des combinaisons, etc.

### Contrat de la méthode factory

- La méthode doit être **`static`** (sauf si la classe de test est annotée `@TestInstance(Lifecycle.PER_CLASS)`, mais restons sur `static`).
- La méthode ne prend **aucun paramètre**.
- La méthode retourne un **`Stream<Arguments>`** (ou `Stream<Integer>`, `Stream<String>`, etc. pour le cas à un seul paramètre, ou `Iterator`, `Iterable`).
- Les arguments sont construits avec `Arguments.of(...)`.

### Exemple : catégorisation par âge

```java
@ParameterizedTest(name = "{0} ans → catégorie \"{1}\"") // name : {0}=âge, {1}=catégorie textuelle attendue
@DisplayName("Catégorisation par âge (via @MethodSource)") // @DisplayName : nom du test
@MethodSource("fournirAgesEtCategories") // @MethodSource : référence une méthode factory static qui fournit les arguments
void categorisationAge(int age, String categorieAttendue) { // age en int, categorieAttendue en String, injectés par la factory
 assertEquals(categorieAttendue, validateur.categorieAge(age), // assertEquals : compare la catégorie calculée avec la catégorie attendue. Paramètres : (attendu, réel)
 "Catégorie incorrecte pour l'âge " + age); // Message en cas d'échec
}

static Stream<Arguments> fournirAgesEtCategories() { // Méthode factory : static, sans paramètre, retourne Stream<Arguments>
 return Stream.of( // Stream.of : crée un flux de 10 jeux de données
 Arguments.of(0, "MINEUR"), // Arguments.of : crée un jeu de 2 arguments (int, String). Âge 0 → MINEUR (limite basse)
 Arguments.of(17, "MINEUR"), // Âge 17 → MINEUR (dernier âge avant la limite 18)
 Arguments.of(18, "JEUNE_ADULTE"), // Âge 18 → JEUNE_ADULTE (limite d'entrée)
 Arguments.of(24, "JEUNE_ADULTE"), // Âge 24 → JEUNE_ADULTE (dernier avant 25)
 Arguments.of(25, "ADULTE"), // Âge 25 → ADULTE (limite d'entrée)
 Arguments.of(59, "ADULTE"), // Âge 59 → ADULTE (dernier avant 60)
 Arguments.of(60, "SENIOR"), // Âge 60 → SENIOR (limite d'entrée)
 Arguments.of(119, "SENIOR"), // Âge 119 → SENIOR (dernier avant 120)
 Arguments.of(120, "CENTENAIRE"), // Âge 120 → CENTENAIRE (limite basse centenaire)
 Arguments.of(150, "CENTENAIRE") // Âge 150 → CENTENAIRE (au-delà de 120, cas par défaut)
 );
}
```

**Analyse détaillée** :

1. **`@MethodSource("fournirAgesEtCategories")`** : le nom de la méthode factory, sous forme de chaîne. JUnit va chercher une méthode `static` avec ce nom dans la classe de test (ou dans une autre classe si on utilise la syntaxe `"NomClasse#methode"`).

2. **La méthode factory** :
 - Elle est `static` — obligatoire.
 - Elle retourne `Stream<Arguments>` — un flux de jeux de données.
 - Chaque `Arguments.of(...)` crée un jeu de paramètres. Les types sont variés : `int`, `String`.

3. **Les cas testés** :
 - `0` et `17` → `"MINEUR"` : testent la limite basse et la borne de la condition `age < 18`.
 - `18` et `24` → `"JEUNE_ADULTE"` : testent l'entrée et la borne de la condition `age < 25`.
 - `25` et `59` → `"ADULTE"` : testent la condition `age < 60`.
 - `60` et `119` → `"SENIOR"` : testent la condition `age < 120`.
 - `120` et `150` → `"CENTENAIRE"` : testent le cas par défaut `return "CENTENAIRE"`.

4. **Stratégie de test** : ce test utilise le **Boundary Value Analysis** (analyse aux bornes). Pour chaque intervalle, on teste la valeur minimale et la valeur maximale :
 - Intervalle MINEUR : testé à 0 (min) et 17 (max).
 - Intervalle JEUNE_ADULTE : testé à 18 (min) et 24 (max).
 - Etc.

### Exemple : `@MethodSource` avec un seul paramètre

```java
@ParameterizedTest // Test paramétré (pas de @Test)
@DisplayName("Âges valides (via @MethodSource d'entiers)") // @DisplayName : nom du test
@MethodSource("agesValides") // @MethodSource : référence la méthode static agesValides() qui retourne Stream<Integer>
void agesValides(int age) { // Un seul paramètre int, injecté directement depuis le Stream<Integer>
 assertTrue(validateur.estAgeValide(age), // assertTrue : vérifie que l'âge est valide (entre 18 et 120)
 "L'âge " + age + " devrait être valide"); // Message en cas d'échec
}

static Stream<Integer> agesValides() { // Méthode factory : retourne Stream<Integer> (pas Stream<Arguments>) car 1 seul paramètre
 return Stream.of(18, 25, 30, 60, 100, 120); // Stream.of : 6 âges valides couvrant différents intervalles (18=limite basse, 120=limite haute)
}
```

**Analyse** :
- Un seul paramètre → la méthode retourne `Stream<Integer>` au lieu de `Stream<Arguments>`.
- Chaque entier est injecté directement.
- Les âges choisis couvrent différents intervalles (18 est la limite basse, 120 la limite haute).

**Quand utiliser `@MethodSource` plutôt que `@CsvSource` ?**
- Quand les jeux de données sont complexes (objets, listes, combinaisons).
- Quand vous voulez générer les données dynamiquement (calcul, aléatoire).
- Quand vous voulez réutiliser la même factory pour plusieurs tests.
- Quand les données viennent d'une source externe que vous interrogez dans la méthode factory.

---

## 1.8 `@NullSource`, `@EmptySource`, `@NullAndEmptySource`

Ces trois annotations comblent un besoin très fréquent : tester le comportement d'une méthode quand on lui passe `null` ou une chaîne vide.

### `@NullSource`

Injecte une valeur `null` dans le test :

```java
@ParameterizedTest // Test paramétré
@NullSource // @NullSource : injecte une valeur null dans le paramètre du test (1 exécution avec valeur=null)
void testAvecNull(String valeur) { // Paramètre String qui recevra la valeur null
 // valeur sera null pour cette exécution // Comportement : le test est exécuté 1 fois avec valeur = null
}
```

### `@EmptySource`

Injecte une valeur "vide" adaptée au type du paramètre :
- Pour `String` : injecte `""` (chaîne vide).
- Pour `List` : injecte une liste vide.
- Pour `Set` : injecte un ensemble vide.
- Pour `Map` : injecte une map vide.
- Pour `int[]`, `long[]`, etc. : injecte un tableau vide.

```java
@ParameterizedTest // Test paramétré
@EmptySource // @EmptySource : injecte une valeur vide adaptée au type : "" pour String, [] pour tableaux, liste vide pour List/Set/Map
void testAvecVide(String valeur) { // Paramètre String qui recevra ""
 // valeur sera "" (chaîne vide) pour cette exécution // 1 exécution avec valeur = ""
}
```

### `@NullAndEmptySource`

Combine `@NullSource` et `@EmptySource` :

```java
@ParameterizedTest // Test paramétré
@NullAndEmptySource // @NullAndEmptySource : combine @NullSource et @EmptySource, injecte null puis "" (2 exécutions)
void testAvecNullEtVide(String valeur) { // Paramètre String
 // Exécuté 2 fois : une fois avec null, une fois avec "" // Ordre : null d'abord, puis chaîne vide
}
```

### Exemple complet tiré du lab

```java
@ParameterizedTest // Test paramétré
@DisplayName("Email invalide pour null et chaîne vide") // @DisplayName : nom du test
@NullAndEmptySource // @NullAndEmptySource : injecte null puis "" (2 exécutions)
void emailNullOuVide(String email) { // Paramètre String
 assertFalse(validateur.estEmailValide(email)); // assertFalse : vérifie que estEmailValide() retourne false pour null et pour ""
}
```

Ce test est exécuté **2 fois** : une fois avec `email = null`, une fois avec `email = ""`. Dans les deux cas, la méthode `estEmailValide` doit retourner `false`.

### Combinaison de `@NullAndEmptySource` avec `@ValueSource`

On peut combiner `@NullAndEmptySource` avec `@ValueSource` pour tester également une valeur supplémentaire (par exemple une chaîne avec un espace) :

```java
@ParameterizedTest // Test paramétré
@DisplayName("Score = 0 pour null et chaîne vide") // @DisplayName : nom du test
@NullAndEmptySource // @NullAndEmptySource : injecte null et "" (2 premières exécutions)
@ValueSource(strings = " ") // @ValueSource : injecte un espace " " (3ème exécution) — combinaison des deux sources
void scoreZeroPourEntreesInvalides(String mdp) { // Paramètre String
 assertEquals(0, validateur.scoreMotDePasse(mdp)); // assertEquals : vérifie que le score est 0. Paramètres : (attendu=0, réel=scoreMotDePasse(mdp))
}
```

**Ordre d'exécution** : `null`, puis `""`, puis `" "`. Soit 3 exécutions.

**Pourquoi `" "` (espace) ?** Une chaîne contenant un espace n'est pas vide (`isEmpty()` retourne `false`), mais elle ne constitue pas un mot de passe valide. Le test vérifie que le score est bien 0 pour cette entrée dégénérée.

### Exemple : `@NullSource` et `@EmptySource` séparés

```java
@ParameterizedTest // Test paramétré
@DisplayName("Téléphone invalide pour null ET chaîne vide") // @DisplayName : nom du test
@NullSource // @NullSource : injecte null (1ère exécution)
@EmptySource // @EmptySource : injecte "" (2ème exécution) — même résultat que @NullAndEmptySource mais annotations séparées
void telephoneNullEtVide(String telephone) { // Paramètre String
 assertFalse(validateur.estTelephoneValide(telephone)); // assertFalse : vérifie que estTelephoneValide() retourne false pour null et ""
}
```

Ici, les deux annotations sont séparées. Le résultat est le même qu'avec `@NullAndEmptySource` : 2 exécutions (`null` et `""`).

---

## 1.9 La conversion automatique de types

JUnit 5 possède un mécanisme puissant de conversion automatique : quand vous déclarez un paramètre de type `int`, `boolean`, `double`, etc., et que la source fournit une `String`, JUnit convertit automatiquement la chaîne vers le type cible.

### Conversions supportées par défaut

| Type cible | Conversion depuis String |
|------------|--------------------------|
| `int` / `Integer` | `Integer.parseInt("42")` |
| `long` / `Long` | `Long.parseLong("42")` |
| `double` / `Double` | `Double.parseDouble("3.14")` |
| `float` / `Float` | `Float.parseFloat("3.14")` |
| `boolean` / `Boolean` | `Boolean.parseBoolean("true")` |
| `short` / `Short` | `Short.parseShort("42")` |
| `byte` / `Byte` | `Byte.parseByte("42")` |
| `char` / `Character` | Si la chaîne a une longueur de 1, prend le premier caractère |
| `Class<?>` | `Class.forName(...)` |
| `Enum` | `Enum.valueOf(...)` |
| `Path` | `Paths.get(...)` |
| `File` | `new File(...)` |
| `URI` / `URL` | `new URI(...)` / `new URL(...)` |
| `UUID` | `UUID.fromString(...)` |
| `Instant` | `Instant.parse(...)` |
| `LocalDate` / `LocalTime` / `LocalDateTime` | `LocalDate.parse(...)` etc. |

### Exemple dans le lab

```java
@ParameterizedTest // Test paramétré
@DisplayName("Conversion automatique : String → int → boolean") // @DisplayName : nom du test
@CsvSource({ // @CsvSource : données au format CSV (chaînes)
 "18, true", // "18" sera converti en int 18, "true" en boolean true
 "17, false", // "17" → int 17, "false" → boolean false
 "120, true", // "120" → int 120, "true" → boolean true
 "121, false", // "121" → int 121, "false" → boolean false
 "0, false" // "0" → int 0, "false" → boolean false
})
void conversionAutoTypes(int age, boolean attendu) { // Types déclarés : int et boolean (pas String !) — JUnit convertit automatiquement
 assertEquals(attendu, validateur.estAgeValide(age)); // assertEquals : compare le booléen attendu au résultat. age est déjà un int grâce à la conversion auto
}
```

**Ce qui se passe** :

1. JUnit lit `@CsvSource`. Chaque ligne est une chaîne de caractères : `"18, true"`, `"17, false"`, etc.
2. Pour la première ligne, JUnit parse `"18"` et `"true"`.
3. Comme le premier paramètre de la méthode est `int age`, JUnit convertit `"18"` en `int` via `Integer.parseInt("18")`.
4. Comme le deuxième paramètre est `boolean attendu`, JUnit convertit `"true"` en `boolean` via `Boolean.parseBoolean("true")`.
5. La méthode est appelée avec `age = 18`, `attendu = true`.

**Pourquoi c'est important** : sans cette conversion automatique, il faudrait écrire :

```java
void conversionAutoTypes(String ageStr, String attenduStr) { // Sans conversion auto : les paramètres restent en String
 int age = Integer.parseInt(ageStr); // Conversion manuelle String → int avec Integer.parseInt()
 boolean attendu = Boolean.parseBoolean(attenduStr); // Conversion manuelle String → boolean avec Boolean.parseBoolean()
 assertEquals(attendu, validateur.estAgeValide(age)); // assertEquals : compare les valeurs après conversion manuelle
}
```

La conversion automatique élimine ce code boilerplate et rend les tests plus propres.

---

## 1.10 Tableau récapitulatif des sources d'arguments

| Source | Utilisation | Nombre de paramètres | Flexibilité |
|--------|-------------|---------------------|-------------|
| `@ValueSource` | Liste de valeurs simples d'un seul type | 1 | Faible (un seul type) |
| `@CsvSource` | Tableau de lignes CSV dans l'annotation | N | Moyenne (types simples, dans l'annotation) |
| `@CsvFileSource` | Fichier CSV externe | N | Moyenne (comme CsvSource mais données externes) |
| `@EnumSource` | Valeurs d'une énumération | 1 (l'enum) | Faible (seulement les enums) |
| `@MethodSource` | Méthode factory retournant un `Stream` | N | Élevée (objets complexes, données dynamiques) |
| `@NullSource` | Injecte `null` | 1 | Très faible |
| `@EmptySource` | Injecte une valeur vide (`""`) | 1 | Très faible |
| `@NullAndEmptySource` | Combine `@NullSource` + `@EmptySource` | 1 | Très faible |
| `@ArgumentsSource` | Implémentation personnalisée d'`ArgumentsProvider` | N | Maximale (non vu dans ce module) |

---

## PARTIE 2 -- PRATIQUE PAS A PAS (40 min)

---

### 2.1 Mise en place du projet

> `labs/lab02-parametres/pom.xml`

Le `pom.xml` du module 2 est identique dans sa structure à celui du module 1. Il contient les mêmes plugins (`maven-compiler-plugin`, `maven-surefire-plugin`, `jacoco-maven-plugin`) et la même dépendance `junit-jupiter`. La seule différence est l'`artifactId` : `lab02-parametres-avances`.

Nous ne répéterons pas l'explication détaillée de chaque balise (voir module 1, section 2.1). Retenez simplement que ce POM configure :
- Java 17 comme version de compilation.
- JUnit Jupiter 5.10.2 comme framework de test (scope `test`).
- Surefire 3.2.5 pour l'exécution des tests.
- JaCoCo 0.8.11 pour la couverture de code.

---

### 2.2 La classe ValidateurUtilisateur

> `labs/lab02-parametres/src/main/java/com/nexa/parametres/ValidateurUtilisateur.java`

### Code complet

```java
package com.nexa.parametres; // Déclaration du package

public class ValidateurUtilisateur { // Classe contenant les méthodes de validation à tester

 public boolean estEmailValide(String email) { // Méthode de validation d'email, retourne true si valide
 if (email == null || email.isEmpty()) return false; // Rejette null et chaîne vide
 if (email.length() > 255) return false; // Rejette les emails > 255 caractères (limite RFC 5321)

 int arobaseIndex = email.indexOf('@'); // Trouve la position du premier @ (-1 si absent)
 if (arobaseIndex <= 0) return false; // Rejette si @ absent (index -1) ou en première position (index 0)

 if (email.indexOf('@', arobaseIndex + 1) != -1) return false; // Rejette si un DEUXIÈME @ est présent (commence la recherche après le premier)

 String domaine = email.substring(arobaseIndex + 1); // Extrait la partie domaine (tout ce qui suit le @)
 return domaine.contains(".") && domaine.length() > 1; // Valide si le domaine contient un point et a au moins 2 caractères
 }

 public boolean estTelephoneValide(String telephone) { // Méthode de validation de téléphone français
 if (telephone == null || telephone.isEmpty()) return false; // Rejette null et chaîne vide

 String nettoye = telephone.replaceAll("[\\s.+-]", ""); // Nettoie : supprime espaces, points, + et -
 if (nettoye.length() == 12 && nettoye.startsWith("+33")) { // Si format international français (12 chiffres, commence par +33)
 nettoye = "0" + nettoye.substring(3); // Convertit en format national : remplace +33 par 0
 }
 if (nettoye.length() != 10) return false; // Rejette si le numéro n'a pas exactement 10 chiffres
 if (nettoye.charAt(0) != '0') return false; // Rejette si le premier chiffre n'est pas 0
 char deuxieme = nettoye.charAt(1); // Récupère le deuxième chiffre
 if (deuxieme < '1' || deuxieme > '9') return false; // Rejette si le deuxième chiffre n'est pas entre 1 et 9 (pas de 00...)

 for (int i = 2; i < nettoye.length(); i++) { // Vérifie les chiffres restants à partir de la position 2
 if (!Character.isDigit(nettoye.charAt(i))) return false; // Rejette si un caractère n'est pas un chiffre
 }
 return true; // Toutes les validations passées : téléphone valide
 }

 public int scoreMotDePasse(String motDePasse) { // Calcule un score de robustesse (0 à 100)
 if (motDePasse == null || motDePasse.isEmpty()) return 0; // Retourne 0 pour null ou vide
 int score = 0; // Initialise le score à 0
 if (motDePasse.length() >= 8) score += 25; // Longueur ≥ 8 : +25 points
 if (motDePasse.length() >= 12) score += 15; // Longueur ≥ 12 : +15 points supplémentaires (cumulatif)
 if (motDePasse.matches(".*[A-Z].*")) score += 20; // Contient au moins une majuscule : +20
 if (motDePasse.matches(".*[a-z].*")) score += 15; // Contient au moins une minuscule : +15
 if (motDePasse.matches(".*[0-9].*")) score += 15; // Contient au moins un chiffre : +15
 if (motDePasse.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) score += 10; // Contient un caractère spécial : +10
 return Math.min(score, 100); // Plafonne le score à 100 maximum
 }

 public boolean estAgeValide(int age) { // Valide si l'âge est entre 18 et 120 ans (inclus)
 return age >= 18 && age <= 120; // Retourne true si 18 ≤ age ≤ 120
 }

 public String categorieAge(int age) { // Catégorise un âge en texte
 if (age < 0) throw new IllegalArgumentException("L'âge ne peut pas être négatif"); // Lève une exception pour les âges négatifs
 if (age < 18) return "MINEUR"; // 0-17 : MINEUR
 if (age < 25) return "JEUNE_ADULTE"; // 18-24 : JEUNE_ADULTE
 if (age < 60) return "ADULTE"; // 25-59 : ADULTE
 if (age < 120) return "SENIOR"; // 60-119 : SENIOR
 return "CENTENAIRE"; // ≥ 120 : CENTENAIRE (cas par défaut)
 }
}
```

### Analyse de chaque méthode

#### `estEmailValide(String email)`

Cette méthode implémente une validation basique d'adresse email. Analysons chaque règle :

1. **`if (email == null || email.isEmpty()) return false;`** : rejette `null` et chaîne vide.
2. **`if (email.length() > 255) return false;`** : rejette les emails trop longs (limite standard RFC 5321).
3. **`int arobaseIndex = email.indexOf('@');`** : trouve la position du premier `@`.
4. **`if (arobaseIndex <= 0) return false;`** : rejette si `@` est absent (index -1) ou en première position (index 0). Exemples rejetés : `"pasd'arobase"`, `"@domaine.com"`.
5. **`if (email.indexOf('@', arobaseIndex + 1) != -1) return false;`** : vérifie qu'il n'y a pas un DEUXIÈME `@` (commence la recherche après le premier). Exemple rejeté : `"user@@domaine.com"`.
6. **`String domaine = email.substring(arobaseIndex + 1);`** : extrait tout ce qui suit le `@`.
7. **`return domaine.contains(".") && domaine.length() > 1;`** : le domaine doit contenir un point (`.com`, `.fr`) et avoir au moins 2 caractères. Rejette : `"user@"` (domaine vide), `"user@domaine"` (pas de point).

**Pourquoi cette méthode est idéale pour les tests paramétrés ?** Elle prend une `String` en entrée et retourne un `boolean`. On a besoin de tester des dizaines de cas (valides, invalides, cas limites). Les tests paramétrés permettent de le faire avec très peu de code.

#### `estTelephoneValide(String telephone)`

Cette méthode valide un numéro de téléphone français. Son algorithme :

1. **`if (telephone == null || telephone.isEmpty()) return false;`** : rejette null et vide.
2. **`String nettoye = telephone.replaceAll("[\\s.+-]", "");`** : supprime les espaces, points, `+` et `-`. Exemple : `"06 12.34+56-78"` → `"0612345678"`.
3. **`if (nettoye.length() == 12 && nettoye.startsWith("+33"))`** : si le numéro est au format international français (12 chiffres, commence par +33), on le convertit en format national : `"0" + nettoye.substring(3)`. Exemple : `"+33612345678"` → `"0612345678"`.
4. **`if (nettoye.length() != 10) return false;`** : un numéro français doit avoir exactement 10 chiffres.
5. **`if (nettoye.charAt(0) != '0') return false;`** : le premier chiffre doit être 0.
6. **`char deuxieme = nettoye.charAt(1);`** puis `if (deuxieme < '1' || deuxieme > '9')` : le deuxième chiffre doit être un chiffre de 1 à 9. Cela rejette `00...` (pas un préfixe valide).
7. **Boucle `for (int i = 2; i < nettoye.length(); i++)`** : tous les caractères restants doivent être des chiffres.

#### `scoreMotDePasse(String motDePasse)`

Cette méthode calcule un score de robustesse pour un mot de passe. Elle retourne un score entre 0 et 100.

| Critère | Points |
|---------|--------|
| Longueur ≥ 8 | +25 |
| Longueur ≥ 12 | +15 (cumulatif, total = 40 si longueur ≥ 12) |
| Contient une majuscule | +20 |
| Contient une minuscule | +15 |
| Contient un chiffre | +15 |
| Contient un caractère spécial | +10 |
| **Plafond** | max 100 |

Le regex `".*[A-Z].*"` signifie : n'importe quel caractère (`.`) répété 0 ou plusieurs fois (`*`), suivi d'une majuscule (`[A-Z]`), suivi de n'importe quoi (`.`). Autrement dit : la chaîne contient au moins une majuscule.

Le plafond `Math.min(score, 100)` empêche le score de dépasser 100, même si un mot de passe remplit tous les critères.

#### `estAgeValide(int age)`

```java
public boolean estAgeValide(int age) {
 return age >= 18 && age <= 120;
}
```

Très simple : âge valide entre 18 et 120 ans (inclus). Cette méthode sert à démontrer la conversion automatique et les combinaisons de sources.

#### `categorieAge(int age)`

```java
public String categorieAge(int age) {
 if (age < 0) throw new IllegalArgumentException("L'âge ne peut pas être négatif");
 if (age < 18) return "MINEUR";
 if (age < 25) return "JEUNE_ADULTE";
 if (age < 60) return "ADULTE";
 if (age < 120) return "SENIOR";
 return "CENTENAIRE";
}
```

Cette méthode catégorise un âge en texte. Les tranches sont :
- < 0 : exception `IllegalArgumentException`
- 0–17 : MINEUR
- 18–24 : JEUNE_ADULTE
- 25–59 : ADULTE
- 60–119 : SENIOR
- ≥ 120 : CENTENAIRE

**Piège** : `age < 0` lève une exception. Attention à ne pas appeler `categorieAge(-5)` sans `assertThrows` !

---

### 2.3 Les tests paramétrés décortiqués

> `labs/lab02-parametres/src/test/java/com/nexa/parametres/ValidateurUtilisateurTest.java`

### Structure générale

```java
package com.nexa.parametres; // Déclaration du package de test

import org.junit.jupiter.api.*; // Importe @Test, @DisplayName, @BeforeEach, @AfterEach, etc.
import org.junit.jupiter.params.*; // Importe @ParameterizedTest
import org.junit.jupiter.params.provider.*; // Importe @ValueSource, @CsvSource, @CsvFileSource, @EnumSource, @MethodSource, @NullSource, @EmptySource, @NullAndEmptySource

import java.util.stream.Stream; // Import pour Stream utilisé par @MethodSource

import static org.junit.jupiter.api.Assertions.*; // Import statique de toutes les assertions (assertEquals, assertTrue, assertFalse, assertNotEquals, etc.)
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE; // Import statique pour écrire EXCLUDE directement
import static org.junit.jupiter.params.provider.EnumSource.Mode.INCLUDE; // Import statique pour écrire INCLUDE directement

@DisplayName("Tests paramétrés du ValidateurUtilisateur") // @DisplayName sur la classe : nom du groupe de tests dans le rapport
class ValidateurUtilisateurTest { // Classe de test

 private final ValidateurUtilisateur validateur = new ValidateurUtilisateur(); // Instance de la classe à tester, créée une fois par instance de test
```

**Points importants sur les imports** :
- `org.junit.jupiter.params.*` : importe `@ParameterizedTest`.
- `org.junit.jupiter.params.provider.*` : importe toutes les sources (`@ValueSource`, `@CsvSource`, `@CsvFileSource`, `@EnumSource`, `@MethodSource`, `@NullSource`, `@EmptySource`, `@NullAndEmptySource`).
- `static import EnumSource.Mode.EXCLUDE` et `INCLUDE` : permet d'écrire simplement `EXCLUDE` et `INCLUDE` dans les annotations.

---

### Test 1 : `emailsValides` — `@ValueSource`

```java
@ParameterizedTest(name = "{index} : email \"{0}\" est valide → {1}") // name : {index}=numéro, {0}=email, {1} inutilisé ici (test à 1 paramètre)
@DisplayName("Validation d'emails valides") // @DisplayName : nom du test
@ValueSource(strings = { // @ValueSource : 5 chaînes = 5 exécutions du test
 "test@example.com", // Email basique valide
 "user.name@domain.co", // Email avec point dans partie locale
 "a@b.co", // Email minimal (domaine très court de 2 lettres)
 "contact@entreprise.fr", // Email avec TLD .fr
 "nom.prenom@site.gouv.fr" // Email avec sous-domaine et TLD .gouv.fr
})
void emailsValides(String email) { // Un seul paramètre String injecté par @ValueSource
 assertTrue(validateur.estEmailValide(email), // assertTrue : vérifie que estEmailValide() retourne true
 () -> "L'email '" + email + "' devrait être valide"); // Supplier<String> : message paresseux, évalué seulement en cas d'échec
}
```

**Décortiquons** :

- `@ParameterizedTest(name = "{index} : email \"{0}\" est valide → {1}")` — le `name` contient `{1}` mais le test n'a qu'un seul paramètre. Ce placeholder sera simplement vide. Ce n'est pas une erreur, juste un placeholder inutilisé.
- `@ValueSource(strings = {...})` — 5 chaînes, donc 5 exécutions du test.
- `void emailsValides(String email)` — un seul paramètre `String email`. Pour chaque valeur du `@ValueSource`, JUnit appelle cette méthode.
- `assertTrue(...)` avec un `Supplier<String>` — message paresseux qui inclut l'email testé.

**Résultat dans le rapport Maven** :
```
[1] 1 : email "test@example.com" est valide →
[2] 2 : email "user.name@domain.co" est valide →
[3] 3 : email "a@b.co" est valide →
[4] 4 : email "contact@entreprise.fr" est valide →
[5] 5 : email "nom.prenom@site.gouv.fr" est valide →
```

**Comment `@ValueSource` remplacerait 10 méthodes `@Test` identiques**

Sans tests paramétrés, voici ce qu'il faudrait écrire :

```java
@Test void emailValide1() { assertTrue(validateur.estEmailValide("test@example.com")); } // @Test classique : 1 méthode = 1 test
@Test void emailValide2() { assertTrue(validateur.estEmailValide("user.name@domain.co")); } // Chaque méthode teste un seul email
@Test void emailValide3() { assertTrue(validateur.estEmailValide("a@b.co")); } // Approche verbeuse : il faut 12 méthodes pour 12 cas
@Test void emailValide4() { assertTrue(validateur.estEmailValide("contact@entreprise.fr")); } // assertTrue : vérifie que l'email est valide
@Test void emailValide5() { assertTrue(validateur.estEmailValide("nom.prenom@site.gouv.fr")); } // Duplication massive de code
@Test void emailInvalide1() { assertFalse(validateur.estEmailValide("")); } // assertFalse : vérifie que l'email est invalide
@Test void emailInvalide2() { assertFalse(validateur.estEmailValide("pasd'arobase")); } // Chaque cas invalide nécessite sa propre méthode
// ... 5+ autres méthodes ... // Total : 12+ méthodes redondantes
```

Avec les tests paramétrés : **2 méthodes** au lieu de 12. Gain : 83% de code en moins, plus lisible, plus maintenable.

---

### Test 2 : `emailsInvalides` — `@ValueSource`

```java
@ParameterizedTest(name = "\"{0}\" → email INVALIDE") // name : {0}=email testé
@DisplayName("Validation d'emails invalides") // @DisplayName : nom du test
@ValueSource(strings = { // @ValueSource : 6 chaînes invalides = 6 exécutions
 "", // Chaîne vide → test de isEmpty()
 "pasd'arobase", // Pas de @ → arobaseIndex = -1 ≤ 0 → false
 "@domaine.com", // @ en position 0 → arobaseIndex = 0 ≤ 0 → false
 "user@", // Domaine vide après @ → pas de point → false
 "user@domaine", // Pas de . dans le domaine → false
 "user@@domaine.com" // Double @ → détecté par le deuxième indexOf
})
void emailsInvalides(String email) { // Paramètre String injecté par @ValueSource
 assertFalse(validateur.estEmailValide(email), // assertFalse : vérifie que estEmailValide() retourne false
 () -> "L'email '" + email + "' devrait être invalide"); // Supplier<String> : message paresseux, évalué en cas d'échec
}
```

**Analyse des cas** : chaque chaîne teste une règle différente de validation :
1. `""` → `isEmpty() == true` → false.
2. `"pasd'arobase"` → pas de `@` → `arobaseIndex == -1 <= 0` → false.
3. `"@domaine.com"` → `@` en position 0 → `arobaseIndex == 0 <= 0` → false.
4. `"user@"` → domaine vide → `domaine.contains(".")` → false.
5. `"user@domaine"` → pas de `.` dans le domaine → false.
6. `"user@@domaine.com"` → double `@` → `indexOf('@', arobaseIndex + 1) != -1` → false.

---

### Test 3 : `scoreMotDePasse` — `@CsvSource`

```java
@ParameterizedTest(name = "\"{0}\" → score = {1}/100") // name : {0}=motDePasse, {1}=score attendu
@DisplayName("Score de robustesse des mots de passe") // @DisplayName : nom du test
@CsvSource({ // @CsvSource : chaque ligne = un couple (motDePasse, scoreAttendu)
 "abc, 0", // 3 caractères, rien de spécial → score = 0
 "abcd1234, 40", // ≥8 car (+25) + minuscules (+15) → 40
 "Abcd1234, 60", // ≥8 car (+25) + minuscule (+15) + majuscule (+20) → 60
 "Abcd1234!, 70", // ≥8 car (+25) + minuscule (+15) + majuscule (+20) + spécial (+10) → 70
 "MotDePasseTresLong123!, 100", // ≥12 car (+25+15) + tous les critères → 100 (plafonné par Math.min)
 "12345678, 25" // ≥8 car (+25) mais <12, pas de lettres → 25
})
void scoreMotDePasse(String motDePasse, int scoreAttendu) { // motDePasse en String, scoreAttendu en int (conversion auto String→int)
 assertEquals(scoreAttendu, validateur.scoreMotDePasse(motDePasse), // assertEquals : compare le score calculé au score attendu. Paramètres : (attendu, réel)
 "Score incorrect pour '" + motDePasse + "'"); // Message en cas d'échec
}
```

**Comment `@CsvSource` remplace 6 méthodes `@Test` :**

Chaque ligne du CSV correspond à un test indépendant. Si on devait écrire cela avec `@Test` :

```java
@Test void scoreAbc() { assertEquals(0, validateur.scoreMotDePasse("abc")); } // @Test classique : 1 méthode par score. assertEquals : vérifie score=0
@Test void scoreAbcd1234() { assertEquals(40, validateur.scoreMotDePasse("abcd1234")); } // Chaque cas = sa propre méthode = duplication
// ... 4 autres méthodes ... // 6 méthodes redondantes au lieu d'une seule avec @CsvSource
```

**Conversion automatique** : `"40"` (String dans le CSV) → `40` (int dans la méthode). Pas besoin de `Integer.parseInt()`.

**Pourquoi les espaces avant les nombres ?** Le parser CSV de JUnit ignore les espaces autour des délimiteurs. Les espaces rendent le tableau plus lisible (les scores sont alignés).

---

### Test 4 : `emailsAvecNull` — `@CsvSource` avec `nullValues`

```java
@ParameterizedTest(name = "email = {0} est valide → {1}") // name : {0}=email, {1}=booléen attendu
@DisplayName("Emails : cas limites avec null") // @DisplayName : nom du test
@CsvSource(value = { // @CsvSource avec option nullValues
 "N/A, false", // "N/A" sera interprété comme null (référence Java) grâce à nullValues
 "'', false" // '' (deux apostrophes) = chaîne vide "" dans le format CSV JUnit
}, nullValues = "N/A") // nullValues : définit "N/A" comme représentant la valeur null
void emailsAvecNull(String email, boolean attendu) { // email peut être null (via nullValues). attendu en boolean
 assertEquals(attendu, validateur.estEmailValide(email)); // assertEquals : compare le booléen attendu au résultat. Paramètres : (attendu, réel)
}
```

**Décortiquons `nullValues = "N/A"` :**

- Sans `nullValues`, JUnit lirait `"N/A"` comme la chaîne `"N/A"`.
- Avec `nullValues = "N/A"`, JUnit interprète `"N/A"` comme la valeur `null` (la référence Java).
- Donc pour la première ligne `"N/A, false"` : `email = null`, `attendu = false`.

**Pourquoi `N/A` ?** C'est une convention arbitraire. On aurait pu utiliser `"NULL"`, `"null"`, `" "`. L'important est que la valeur choisie ne soit pas une donnée légitime du test. `"N/A"` (Not Available) est une convention lisible.

**Pourquoi `''` ?** Dans le CSV de JUnit, deux apostrophes consécutives `''` représentent une chaîne vide `""`. Sans les apostrophes, le parser pourrait interpréter la valeur comme absente.

**Résultat** : 2 exécutions.
1. `email = null, attendu = false` → `estEmailValide(null)` → false
2. `email = "", attendu = false` → `estEmailValide("")` → false

---

### Test 5 et 6 : `@EnumSource` — Modes EXCLUDE et INCLUDE

```java
enum StatutUtilisateur { ACTIF, INACTIF, SUSPENDU, SUPPRIME } // Énumération définie dans la classe de test pour démontrer @EnumSource
```

Cette énumération est définie DANS la classe de test. C'est une pratique courante pour les besoins des tests : on définit une petite énumération juste pour démontrer `@EnumSource`.

```java
@ParameterizedTest // Test paramétré (remplace @Test)
@DisplayName("Tous les statuts sauf SUPPRIME sont valides") // @DisplayName : nom du test
@EnumSource(value = StatutUtilisateur.class, mode = EXCLUDE, names = "SUPPRIME") // @EnumSource : itère sur StatutUtilisateur. mode=EXCLUDE exclut SUPPRIME. Résultat : 3 exécutions (ACTIF, INACTIF, SUSPENDU)
void statutsValides(StatutUtilisateur statut) { // Paramètre de type StatutUtilisateur injecté automatiquement
 assertNotEquals(StatutUtilisateur.SUPPRIME, statut, // assertNotEquals : vérifie que le statut n'est PAS SUPPRIME. Paramètres : (valeurNonAttendue, valeurRéelle)
 "Tous les statuts sauf SUPPRIME devraient passer"); // Message en cas d'échec
}
```

- `value = StatutUtilisateur.class` : indique l'énumération à utiliser.
- `mode = EXCLUDE` : on EXCLUT les valeurs listées dans `names`.
- `names = "SUPPRIME"` : la valeur à exclure.
- Résultat : 3 exécutions (ACTIF, INACTIF, SUSPENDU).

```java
@ParameterizedTest // Test paramétré
@DisplayName("Seuls ACTIF et SUSPENDU sont testés ici") // @DisplayName : nom du test
@EnumSource(value = StatutUtilisateur.class, mode = INCLUDE, names = {"ACTIF", "SUSPENDU"}) // @EnumSource : mode=INCLUDE inclut SEULEMENT ACTIF et SUSPENDU. Résultat : 2 exécutions
void statutsSpecifiques(StatutUtilisateur statut) { // Paramètre injecté : seulement ACTIF puis SUSPENDU
 assertTrue(statut == StatutUtilisateur.ACTIF || statut == StatutUtilisateur.SUSPENDU); // assertTrue : vérifie que le statut est l'une des deux valeurs incluses
}
```

- `mode = INCLUDE` : on inclut SEULEMENT les valeurs listées.
- `names = {"ACTIF", "SUSPENDU"}` : tableau de deux noms.
- Résultat : 2 exécutions (ACTIF, SUSPENDU).

**Quand utiliser EXCLUDE vs INCLUDE ?**
- **EXCLUDE** : quand vous voulez tester presque toutes les valeurs sauf quelques-unes. Par exemple, tester tous les statuts sauf SUPPRIME (qui est un cas particulier à tester séparément).
- **INCLUDE** : quand vous voulez tester seulement un sous-ensemble précis. Par exemple, tester seulement les statuts qui autorisent la connexion.

---

### Test 7 : `telephonesDepuisFichier` — `@CsvFileSource`

```java
@ParameterizedTest(name = "Téléphone \"{0}\" valide → {1}")
@DisplayName("Validation téléphones via fichier CSV externe")
@CsvFileSource(resources = "/telephones-test.csv", numLinesToSkip = 1)
void telephonesDepuisFichier(String telephone, boolean attendu) {
 assertEquals(attendu, validateur.estTelephoneValide(telephone),
 "Échec pour le téléphone : " + telephone);
}
```

**Analyse** :

- `@CsvFileSource(resources = "/telephones-test.csv", numLinesToSkip = 1)` : lit le fichier CSV dans `src/test/resources/`, saute la première ligne (en-tête).
- Le fichier CSV contient 10 lignes de données → 10 exécutions du test.
- Pour chaque ligne, JUnit lit `telephone` et `valide`, convertit `"true"`/`"false"` en boolean, et appelle la méthode.
- La conversion String → boolean est automatique (voir section 1.9).

**Différence entre `@CsvSource` et `@CsvFileSource` :**

| Critère | `@CsvSource` | `@CsvFileSource` |
|---------|-------------|------------------|
| Stockage des données | Dans l'annotation (fichier .java) | Dans un fichier CSV externe |
| Nombre de cas | Idéal pour < 20 cas | Idéal pour > 20 cas |
| Modification | Nécessite de modifier le code Java | Modifier le fichier CSV suffit |
| Collaboration | Réservé au développeur | Un analyste métier peut éditer le CSV |
| Versionnage | Les données sont dans le code → review facile | Les données sont dans un fichier séparé |

---

### Test 8 : `categorisationAge` — `@MethodSource`

```java
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
```

**Explication pas à pas** :

1. `@MethodSource("fournirAgesEtCategories")` : JUnit cherche une méthode `static` nommée `fournirAgesEtCategories` dans la même classe.

2. La méthode factory `fournirAgesEtCategories()` est exécutée **une seule fois** avant le premier test. Elle retourne un `Stream<Arguments>` de 10 jeux de données.

3. Pour chaque jeu de données, JUnit appelle `categorisationAge(int age, String categorieAttendue)` avec les arguments correspondants.

4. La méthode utilise `assertEquals` pour comparer la catégorie calculée avec la catégorie attendue.

**Pourquoi utiliser `@MethodSource` ici plutôt que `@CsvSource` ?** On aurait pu écrire :

```java
@CsvSource({
 "0, MINEUR",
 "17, MINEUR",
 "18, JEUNE_ADULTE",
 // ... 7 autres lignes ...
})
```

Mais `@MethodSource` offre des avantages :
- Les valeurs sont typées (vrais `int`, pas de conversion String → int).
- La factory peut être réutilisée par plusieurs méthodes de test.
- On peut générer les données dynamiquement (par exemple, tous les âges de 1 à 150).
- Le code est plus lisible quand le nombre de cas est important.

---

### Test 9 : `agesValides` — `@MethodSource` avec un seul paramètre

```java
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
```

**Analyse** :
- `@MethodSource("agesValides")` fait référence à `static Stream<Integer> agesValides()`.
- La méthode retourne un `Stream<Integer>` (pas `Stream<Arguments>`) car il n'y a qu'un seul paramètre.
- 6 exécutions : pour chaque âge, on vérifie qu'il est valide.
- 18 est la limite basse, 120 la limite haute : Boundary Value Analysis.

---

### Test 10 : `emailNullOuVide` — `@NullAndEmptySource`

```java
@ParameterizedTest
@DisplayName("Email invalide pour null et chaîne vide")
@NullAndEmptySource
void emailNullOuVide(String email) {
 assertFalse(validateur.estEmailValide(email));
}
```

- Exécuté 2 fois : `email = null`, `email = ""`.
- Dans les deux cas, `estEmailValide` doit retourner `false`.
- C'est la manière la plus concise de tester null et chaîne vide.

---

### Test 11 : `telephoneNullEtVide` — `@NullSource` + `@EmptySource`

```java
@ParameterizedTest
@DisplayName("Téléphone invalide pour null ET chaîne vide")
@NullSource
@EmptySource
void telephoneNullEtVide(String telephone) {
 assertFalse(validateur.estTelephoneValide(telephone));
}
```

Même résultat que `@NullAndEmptySource`, mais avec les deux annotations séparées. Cela montre que les deux annotations peuvent être combinées manuellement.

---

### Test 12 : `scoreZeroPourEntreesInvalides` — Combinaison `@NullAndEmptySource` + `@ValueSource`

```java
@ParameterizedTest
@DisplayName("Score = 0 pour null et chaîne vide")
@NullAndEmptySource
@ValueSource(strings = " ")
void scoreZeroPourEntreesInvalides(String mdp) {
 assertEquals(0, validateur.scoreMotDePasse(mdp));
}
```

**Analyse** :
- `@NullAndEmptySource` injecte `null` et `""`.
- `@ValueSource(strings = " ")` injecte `" "` (une chaîne avec un espace).
- Ordre d'exécution : `null` → `""` → `" "`. Soit 3 exécutions.
- Dans tous les cas, le score doit être 0.

**Pourquoi tester `" "` ?** Parce que `" "` n'est pas `null`, et `" ".isEmpty()` est `false`. Mais ce n'est pas un mot de passe valide pour autant. C'est un **cas limite** : une entrée non nulle et non vide qui devrait quand même produire un score de 0.

---

### Test 13 : `telephonesValidesFormatsVariés` — `@CsvSource` (un seul paramètre)

```java
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
```

**Analyse des formats** :
- `"0612345678"` : format basique sans séparateur.
- `"06 12 34 56 78"` : avec espaces.
- `"06.12.34.56.78"` : avec points.
- `"+33612345678"` : format international sans séparateur.
- `"+33 6 12 34 56 78"` : format international avec espaces.

Tous ces formats doivent être acceptés car la méthode `estTelephoneValide` nettoie d'abord la chaîne avec `replaceAll("[\\s.+-]", "")`.

**Note** : `@CsvSource` peut aussi être utilisée avec un seul paramètre par ligne. JUnit traite chaque ligne comme une valeur unique.

---

### Test 14 : `conversionAutoTypes` — Démonstration de conversion

```java
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
```

**Démonstration de la conversion automatique** :

Ce test montre que JUnit :
1. Lit `"18, true"` dans le CSV.
2. Parse `"18"` en `int` automatiquement.
3. Parse `"true"` en `boolean` automatiquement.
4. Appelle `conversionAutoTypes(18, true)`.

Sans conversion automatique, il faudrait écrire :

```java
void conversionAutoTypes(String ageStr, String attenduStr) {
 int age = Integer.parseInt(ageStr);
 boolean attendu = Boolean.parseBoolean(attenduStr);
 assertEquals(attendu, validateur.estAgeValide(age));
}
```

Ce test couvre les cas suivants pour `estAgeValide` :
- 18 : valide (limite basse)
- 17 : invalide (juste en dessous de la limite)
- 120 : valide (limite haute)
- 121 : invalide (juste au-dessus de la limite)
- 0 : invalide (bien en dessous)

---

### 2.4 Exécution des tests

### Commande

```bash
mvn clean test
```

### Résultat attendu

```
[INFO] Running com.nexa.parametres.ValidateurUtilisateurTest
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**Note** : bien qu'il n'y ait que 14 méthodes annotées `@ParameterizedTest`, le nombre réel de tests exécutés est bien plus élevé (environ 50), car chaque méthode est exécutée plusieurs fois avec des paramètres différents. Le rapport Surefire regroupe les exécutions paramétrées sous une seule méthode.

Pour voir le détail de chaque paramètre, utilisez le paramètre `name` de `@ParameterizedTest` : chaque exécution apparaîtra avec son propre nom dans la console.

### Rapport JaCoCo

```bash
mvn jacoco:report
```

Ouvrez `target/site/jacoco/index.html` pour voir la couverture. Les tests paramétrés couvrent de nombreuses branches, ce qui devrait donner une couverture élevée (> 80%).

---

## PARTIE 3 -- LAB (45 min)

---

### Énoncé

### Objectif

Ajouter deux nouvelles méthodes de validation à la classe `ValidateurUtilisateur` et écrire leurs tests avec les sources paramétrées appropriées.

### Consignes

#### 1. Ajouter `estCodePostalValide(String codePostal)`

Cette méthode valide un code postal français (5 chiffres). Règles :
- Ne doit pas être `null` ou vide.
- Doit contenir exactement 5 caractères.
- Tous les caractères doivent être des chiffres.
- Le premier chiffre ne doit pas être 0 (les codes postaux français commencent à 01... et vont jusqu'à 97..., 98... pour Monaco, 99... pour les DOM-TOM).

```java
public boolean estCodePostalValide(String codePostal) {
 // À implémenter
}
```

**Tests attendus** :
- Utiliser `@CsvFileSource` avec un fichier CSV contenant **20 codes postaux** (10 valides, 10 invalides).
- Le fichier CSV doit être créé dans `src/test/resources/codes-postaux-test.csv`.
- Inclure des cas : valide simple, limite basse (01000), limite haute (99999), trop court, trop long, avec lettres, avec espaces, null (via `nullValues`), vide.
- Utiliser `numLinesToSkip = 1` pour l'en-tête.

#### 2. Ajouter `estUrlValide(String url)`

Cette méthode valide une URL simple. Règles :
- Ne doit pas être `null` ou vide.
- Doit commencer par `http://` ou `https://`.
- Doit contenir au moins un point après le protocole (pour le domaine).

```java
public boolean estUrlValide(String url) {
 // À implémenter
}
```

**Tests attendus** :
- Utiliser `@MethodSource` avec une méthode `static Stream<Arguments> fournirUrlsEtValidite()`.
- Tester au moins 10 URLs (6 valides, 4 invalides).
- Cas à couvrir : `http://`, `https://`, avec `www`, sans `www`, avec chemin, sans chemin, `ftp://` (invalide), pas de protocole, pas de point, null.
- Utiliser `@NullAndEmptySource` pour les cas null et vide.

### Fichiers à modifier / créer

- `labs/lab02-parametres/src/main/java/com/nexa/parametres/ValidateurUtilisateur.java` (ajouter les 2 méthodes)
- `labs/lab02-parametres/src/test/java/com/nexa/parametres/ValidateurUtilisateurTest.java` (ajouter les tests)
- `labs/lab02-parametres/src/test/resources/codes-postaux-test.csv` (créer le fichier CSV)

### Critères de réussite

- Tous les tests passent (`mvn clean test` → BUILD SUCCESS).
- Au moins 3 sources d'arguments différentes sont utilisées (`@CsvFileSource`, `@MethodSource`, `@NullAndEmptySource`).
- Le fichier CSV contient au moins 20 entrées.
- La couverture de code > 80%.

---

### Correction

### Implémentation de `estCodePostalValide`

```java
public boolean estCodePostalValide(String codePostal) {
 if (codePostal == null || codePostal.isEmpty()) return false;
 if (codePostal.length() != 5) return false;
 if (codePostal.charAt(0) == '0') return false;
 for (int i = 0; i < codePostal.length(); i++) {
 if (!Character.isDigit(codePostal.charAt(i))) return false;
 }
 return true;
}
```

**Points clés** :
- La vérification `charAt(0) == '0'` rejette les codes commençant par 0.
- La boucle vérifie que tous les caractères sont des chiffres.
- L'ordre des vérifications est important : null/vide d'abord, puis longueur, puis contenu.

### Implémentation de `estUrlValide`

```java
public boolean estUrlValide(String url) {
 if (url == null || url.isEmpty()) return false;
 String urlMinuscule = url.toLowerCase();
 if (!urlMinuscule.startsWith("http://") && !urlMinuscule.startsWith("https://")) {
 return false;
 }
 String sansProtocole = url.substring(url.indexOf("://") + 3);
 return sansProtocole.contains(".") && sansProtocole.length() > 1;
}
```

**Points clés** :
- `toLowerCase()` pour accepter `HTTP://`, `Https://`, etc.
- `indexOf("://") + 3` pour extraire ce qui suit le protocole.
- `contains(".")` pour vérifier la présence d'un point dans le domaine.
- `sansProtocole.length() > 1` pour rejeter `http://.` (un point seul n'est pas un domaine valide).

### Fichier CSV pour les codes postaux

Contenu de `src/test/resources/codes-postaux-test.csv` :

```csv
codePostal,valide
75001,true
13002,true
01000,true
99999,true
59000,true
33000,true
44000,true
69001,true
21000,true
34000,true
00000,false
1234,false
123456,false
ABCDE,false
12 34,false
7500A,false
,false
999999,false
00001,false
-1234,false
```

### Tests avec `@CsvFileSource`

```java
@ParameterizedTest(name = "Code postal \"{0}\" valide → {1}")
@DisplayName("Validation codes postaux via fichier CSV")
@CsvFileSource(resources = "/codes-postaux-test.csv", numLinesToSkip = 1)
void codesPostauxDepuisFichier(String codePostal, boolean attendu) {
 assertEquals(attendu, validateur.estCodePostalValide(codePostal),
 "Échec pour le code postal : " + codePostal);
}
```

### Tests avec `@MethodSource` pour les URLs

```java
@ParameterizedTest(name = "URL \"{0}\" valide → {1}")
@DisplayName("Validation d'URLs (via @MethodSource)")
@MethodSource("fournirUrlsEtValidite")
void urlsValides(String url, boolean attendu) {
 assertEquals(attendu, validateur.estUrlValide(url),
 "Échec pour l'URL : " + url);
}

static Stream<Arguments> fournirUrlsEtValidite() {
 return Stream.of(
 Arguments.of("http://example.com", true),
 Arguments.of("https://www.google.fr", true),
 Arguments.of("http://site.org/path", true),
 Arguments.of("https://sub.domain.co/page?q=1", true),
 Arguments.of("http://localhost:8080", true),
 Arguments.of("https://a.b", true),
 Arguments.of("ftp://example.com", false),
 Arguments.of("example.com", false),
 Arguments.of("http://sanspoint", false),
 Arguments.of("https://", false)
 );
}

@ParameterizedTest
@DisplayName("URL invalide pour null et chaîne vide")
@NullAndEmptySource
void urlNullOuVide(String url) {
 assertFalse(validateur.estUrlValide(url));
}
```

### Points clés de la correction

1. **`@CsvFileSource` pour 20 entrées** : justifie l'utilisation d'un fichier externe. Avec 20 lignes, `@CsvSource` serait illisible.

2. **`@MethodSource` pour les URLs** : les cas de test sont variés (protocoles, sous-domaines, chemins). La méthode factory les regroupe de manière lisible.

3. **`@NullAndEmptySource`** : toujours tester null et chaîne vide pour les méthodes qui prennent une String.

4. **Conversion automatique** : le CSV contient `"true"` et `"false"` en chaînes, JUnit les convertit automatiquement en `boolean`.

5. **Cas limites pour les URLs** : `"https://a.b"` teste une URL minimale valide (domaine de 3 caractères après le protocole). `"https://"` teste une URL sans domaine (juste le protocole).

6. **Cas limites pour les codes postaux** : `"01000"` est valide (premier chiffre ≠ 0), `"00000"` est invalide (commence par 0), `"00001"` est invalide pour la même raison.

---

## FICHE MEMO -- Module 2

### Annotations de tests paramétrés

| Annotation | Rôle | Exemple |
|---|---|---|
| `@ParameterizedTest` | Remplace `@Test` pour un test paramétré | `@ParameterizedTest void test(int x)` |
| `@ValueSource` | Fournit un tableau de valeurs simples | `@ValueSource(ints = {1, 2, 3})` |
| `@CsvSource` | Fournit des couples (entrée, sortie) en CSV | `@CsvSource({"a,1", "b,2"})` |
| `@CsvFileSource` | Lit les données depuis un fichier CSV | `@CsvFileSource(resources = "/data.csv")` |
| `@EnumSource` | Itère sur les valeurs d'une énumération | `@EnumSource(MaEnum.class)` |
| `@MethodSource` | Appelle une méthode factory statique | `@MethodSource("fournirDonnees")` |
| `@NullSource` | Injecte `null` | `@NullSource` |
| `@EmptySource` | Injecte une valeur vide (`""`) | `@EmptySource` |
| `@NullAndEmptySource` | Injecte `null` puis `""` | `@NullAndEmptySource` |

### Placeholders pour le paramètre `name`

| Placeholder | Signification |
|-------------|---------------|
| `{index}` | Numéro du jeu de données (commence à 1) |
| `{0}` | Premier paramètre |
| `{1}` | Deuxième paramètre |
| `{n}` | n-ième paramètre |
| `{arguments}` | Tous les arguments concaténés |

### Commandes Maven

| Commande | Effet |
|---|---|
| `mvn clean test` | Nettoie, compile, exécute les tests |
| `mvn test` | Compile et exécute les tests (sans nettoyer) |
| `mvn jacoco:report` | Génère le rapport de couverture HTML |

### Rappels essentiels

- **`@ParameterizedTest` remplace `@Test`** : ne mettez pas les deux sur la même méthode.
- **Une source est obligatoire** : sans `@ValueSource`, `@CsvSource`, etc., JUnit ne sait pas quoi injecter.
- **Conversion automatique** : JUnit convertit les String en int, boolean, double, etc. Profitez-en.
- **Méthode factory en `static`** : pour `@MethodSource`, la méthode doit être `static`.
- **`nullValues` pour représenter null** : dans `@CsvSource` et `@CsvFileSource`, utilisez `nullValues = "N/A"` (ou autre) pour injecter `null`.
- **`numLinesToSkip = 1`** : pour sauter la ligne d'en-tête du fichier CSV.
- **`@NullAndEmptySource` + `@ValueSource`** : vous pouvez combiner ces annotations pour couvrir null, vide, et des valeurs supplémentaires.
- **Préférez `@CsvFileSource` pour > 20 cas** : gardez votre code Java propre et vos données dans des fichiers CSV.
