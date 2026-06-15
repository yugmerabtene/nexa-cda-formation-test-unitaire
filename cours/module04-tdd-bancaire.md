# Module 4 : TDD — Application Bancaire

> **Durée :** 1h45 (15h15–17h00) — Jour 1 Après-midi
> **Projet support :** `labs/lab04-tdd-banque`
> **Dépendances :** JUnit 5.10.2, Mockito 5.10.0, JaCoCo 0.8.11

---

## Objectifs pédagogiques

A l'issue de ce module, vous serez capable de :

1. Appliquer le cycle TDD : Red -> Green -> Refactor.
2. Utiliser `@Nested` pour organiser les tests hierarchiquement.
3. Utiliser `@Timeout` pour detecter les boucles infinies ou traitements trop lents.
4. Utiliser `@Tag` pour categoriser les tests et filtrer leur execution.
5. Ecrire des tests pour des operations concurrentes (virement, thread-safety).
6. Generer et interpreter un rapport de couverture de code avec JaCoCo.
7. Atteindre un seuil de couverture de code superieur a 90%.

---

## PARTIE 1 -- THEORIE (50 min)

## 1.1 TDD : Red → Green → Refactor

Le **Test-Driven Development** (développement piloté par les tests) est une discipline de développement en trois phases. On n'écrit **jamais** de code de production avant d'avoir écrit un test qui échoue.

### Le cycle en trois phases

```

 RED ← Écrire un test qui ÉCHOUE

 GREEN ← Écrire le MINIMUM de code pour que le test passe

 REFACTOR ← Améliorer le code SANS changer son comportement

 → Retour à RED pour la fonctionnalité suivante
```

### Phase RED — Écrire un test qui échoue

On écrit un test qui décrit le comportement **attendu** avant même que le code n'existe. Le test doit échouer (RED) pour prouver qu'il teste bien quelque chose qui n'existe pas encore. Un test qui passe immédiatement est un **faux positif**.

**Exemple concret — Créer un compte bancaire :**

```java
// Phase RED : ce test décrit le comportement attendu avant que le code n'existe
@Test
@DisplayName("Un compte est créé avec un solde initial correct") // @DisplayName : nom lisible dans le rapport de test
void creationAvecSoldeInitial() {
 // Arrange : création d'un compte avec ID 1, titulaire "Alice", solde 1000.00€
 // BigDecimal("1000.00") : précision décimale exacte pour les montants (évite les erreurs de float/double)
 CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("1000.00"));

 // Assert : vérification que chaque champ est correctement stocké
 assertEquals(1L, compte.getId());
 assertEquals("Alice", compte.getTitulaire());
 assertEquals(new BigDecimal("1000.00"), compte.getSolde()); // BigDecimal.compareTo() ou equals() pour comparer
}
```

À ce stade, la classe `CompteBancaire` n'existe pas. Le code ne compile même pas. C'est le **RED le plus extrême** : échec de compilation.

### Phase GREEN — Écrire le minimum de code

On ecrit **le minimum necessaire** de code pour que le test passe. Pas de fonctionnalites supplementaires, pas d'optimisation, pas de generalisation.

```java
// Phase GREEN : strict minimum de code pour satisfaire le test RED précédent
public class CompteBancaire {
 private final Long id; // final : l'ID est immuable après création
 private final String titulaire; // final : le titulaire ne change pas
 private BigDecimal solde; // non-final : le solde varie avec les opérations

 public CompteBancaire(Long id, String titulaire, BigDecimal soldeInitial) { // BigDecimal : type pour montants, précision exacte
 this.id = id;
 this.titulaire = titulaire;
 this.solde = soldeInitial;
 }

 // Getters : minimum requis pour que le test passe
 public Long getId() { return id; }
 public String getTitulaire() { return titulaire; }
 public BigDecimal getSolde() { return solde; }
}
```

Le test passe. On a écrit le strict minimum pour satisfaire le test.

### Phase REFACTOR — Améliorer sans changer le comportement

Une fois le test vert, on peut améliorer le code : renommer des variables, extraire des méthodes, ajouter des validations, optimiser. Les tests existants garantissent qu'on ne casse rien.

**Exemple — Ajouter une validation :** après le premier test vert, on écrit un **nouveau** test RED pour le cas du solde négatif :

```java
@Test
@DisplayName("Le solde initial ne peut pas être négatif")
void soldeInitialNegatifInterdit() {
 assertThrows(IllegalArgumentException.class,
 () -> new CompteBancaire(3L, "Charlie", new BigDecimal("-100.00")));
}
```

Ce test échoue (RED) car le constructeur accepte un solde négatif. On passe en GREEN en ajoutant la validation :

```java
// Phase GREEN : ajout de la validation pour que le test RED (soldeInitialNegatif) passe
public CompteBancaire(Long id, String titulaire, BigDecimal soldeInitial) {
 // Validation : le solde doit être >= 0
 // BigDecimal.compareTo(BigDecimal.ZERO) < 0 signifie "est négatif"
 // compareTo() retourne -1, 0, ou 1 (jamais == pour comparer des BigDecimal)
 if (soldeInitial.compareTo(BigDecimal.ZERO) < 0) {
  throw new IllegalArgumentException("Le solde initial ne peut pas être négatif");
 }
 this.id = id;
 this.titulaire = titulaire;
 this.solde = soldeInitial;
 // ...
}
```

### Pourquoi écrire le test AVANT le code ?

1. **Le test définit le design** — En écrivant le test d'abord, on réfléchit à l'API (signature, nommage, exceptions) du point de vue de l'**utilisateur** de la classe, pas de son implémenteur. Cela force à concevoir des interfaces propres.

2. **Le test est exécutable immédiatement** — On sait tout de suite si le test est correct (il échoue pour la bonne raison) et si le code est correct (il fait passer le test).

3. **Couverture garantie** — Chaque ligne de code de production existe parce qu'un test l'exige. Pas de code mort, pas de fonctionnalité non testée.

4. **Filet de sécurité** — Les tests s'accumulent et forment un filet qui détecte instantanément les régressions lors des refactorings futurs.

### Application dans le lab04

Le fichier `CompteBancaireTest.java` est structuré pour refléter le processus TDD : chaque `@Nested` (groupe de tests) correspond à une itération Red→Green→Refactor.

---

## 1.2 `@Nested` — Organisation hiérarchique des tests

L'annotation `@Nested` permet de créer des **classes internes de test** qui organisent les tests de manière hiérarchique et logique.

### Syntaxe

```java
@DisplayName("TDD : Compte Bancaire") // @DisplayName sur la classe : nom du groupe racine
class CompteBancaireTest {

 // @Nested : classe interne non statique pour organiser les tests hiérarchiquement
 // Chaque @Nested peut avoir son propre @BeforeEach qui s'exécute en plus du parent
 @Nested
 @DisplayName("Création du compte") // @DisplayName du @Nested : apparaît comme sous-groupe dans l'IDE
 class CreationCompte {
  // tests...
 }

 @Nested
 @DisplayName("Opérations de dépôt")
 class Depot {
  // tests...
 }
}
```

Chaque `@Nested` est une **classe interne non statique** contenant ses propres `@Test`. Le `@DisplayName` de la classe externe et des classes imbriquées forme une hiérarchie :

```
TDD : Compte Bancaire
 Création du compte
 Un compte est créé avec un solde initial correct
 Un compte peut être créé avec un solde initial de zéro
 Le solde initial ne peut pas être négatif
 Opérations de dépôt
 Un dépôt augmente le solde
 Dépôt de zéro est interdit
 Dépôt négatif est interdit
 Plusieurs dépôts successifs
...
```

### Avantages

1. **Lisibilité** — Les tests sont regroupés par fonctionnalité plutôt qu'en une liste plate de 25 méthodes. L'IDE (IntelliJ, Eclipse) affiche cette hiérarchie dans l'explorateur de tests.

2. **`@BeforeEach` par groupe** — Chaque `@Nested` peut avoir son propre `@BeforeEach`, qui s'exécute **en plus** du `@BeforeEach` de la classe parente. Cela permet d'initialiser des données spécifiques à un groupe de tests sans les dupliquer.

3. **Indépendance** — Chaque `@Nested` est indépendant. On peut exécuter un seul groupe dans l'IDE sans lancer tous les tests.

### Interaction parent-enfant

Dans le lab04, la classe parente `CompteBancaireTest` ne définit pas de `@BeforeEach`, donc chaque `@Nested` crée ses propres instances dans ses propres tests. Les champs de la classe parente sont accessibles depuis les `@Nested`, mais ce pattern est utilisé avec parcimonie pour éviter les dépendances cachées.

---

## 1.3 `@Tag` — Catégorisation des tests

L'annotation `@Tag` permet d'étiqueter les tests pour les filtrer lors de l'exécution Maven.

### Syntaxe

```java
@Test
@Tag("unitaire") // @Tag : étiquette pour filtrer les tests à l'exécution (mvn test -Dgroups="unitaire")
void testRapide() { ... }

@Test
@Tag("integration") // Un test peut avoir plusieurs @Tag
@Tag("lent") // @Tag("lent") : exécuté en CI, pas en local
void testLent() { ... }
```

### Exécution filtrée avec Maven Surefire

**Exécuter seulement les tests tagués "unitaire" :**

```bash
mvn test -Dgroups="unitaire"
```

**Exclure les tests tagués "lent" :**

```bash
mvn test -DexcludedGroups="lent"
```

**Combiner plusieurs tags (ET logique) :**

```bash
mvn test -Dgroups="unitaire & rapide"
```

### Cas d'usage dans un projet

- `@Tag("unitaire")` : tests rapides exécutés à chaque commit
- `@Tag("integration")` : tests nécessitant une base de données, exécutés avant le merge
- `@Tag("lent")` : tests de performance ou de concurrence, exécutés la nuit en CI/CD
- `@Tag("fumeux")` : smoke tests, exécutés en premier pour valider rapidement l'état global

> **Note pour le lab04 :** bien que les tests de concurrence soient plus lents, le `@Tag` n'est pas encore utilisé dans le code du lab. C'est une bonne pratique que vous pouvez appliquer dans le lab pour taguer les tests de concurrence avec `@Tag("lent")`.

---

## 1.4 `@Timeout` — Protection contre les tests bloquants

L'annotation `@Timeout` définit une durée maximale pour un test. Si le test dépasse cette durée, il échoue automatiquement.

### Syntaxe

```java
@Test
@Timeout(value = 5, unit = TimeUnit.SECONDS) // @Timeout : échoue le test s'il dépasse 5 secondes (protection anti-blocage)
void testQuiPourraitBloquer() throws InterruptedException { // throws InterruptedException : nécessaire pour latch.await()
 // ...
}
```

### Pourquoi `@Timeout` est crucial

Dans les tests de **concurrence**, un thread peut se bloquer indéfiniment (deadlock, boucle infinie, `await()` sans `countDown()`). Sans timeout, le test bloquerait la suite de tests **pour toujours** (ou jusqu'à ce que le CI tue le build après 10 minutes).

### Exemple du lab04

Les deux tests de la classe `Concurrence` utilisent `@Timeout(value = 5, unit = TimeUnit.SECONDS)` :

```java
@Nested // @Nested : groupe de tests dédié à la concurrence
@DisplayName("Concurrence et thread-safety")
class Concurrence {

 @Test
 @DisplayName("Dépôts concurrents : le solde final est correct")
 @Timeout(value = 5, unit = TimeUnit.SECONDS) // @Timeout : évite que le test bloque indéfiniment si deadlock
 void depotsConcurrents() throws InterruptedException { // throws InterruptedException : requis pour les appels bloquants (await)
  // ...
 }
}
```

Si un des `latch.await()` ou `executor.awaitTermination()` bloque au-delà de 5 secondes, JUnit force l'échec du test au lieu d'attendre indéfiniment.

### Bonnes pratiques

- Toujours mettre `@Timeout` sur les tests qui utilisent `CountDownLatch`, `ExecutorService`, ou toute attente explicite
- Une valeur de timeout raisonnable est 2 à 5 secondes pour des tests unitaires de concurrence
- Ne pas mettre de timeout trop court qui ferait échouer un test légitime sur une machine lente (CI)

---

## 1.5 `@RepeatedTest` — Exécution multiple pour la stabilité

L'annotation `@RepeatedTest` exécute le même test **plusieurs fois** de suite. Elle remplace `@Test`.

### Syntaxe

```java
// @RepeatedTest : remplace @Test, exécute le test N fois pour détecter les flaky tests
// value = 10 : exécute 10 fois (nouvelle instance de la classe de test à chaque répétition)
// name : modèle de nommage pour le rapport (chaque répétition a un nom unique)
@RepeatedTest(value = 10, name = "{displayName} — répétition {currentRepetition}/{totalRepetitions}")
@DisplayName("Stabilité : le solde après 3 opérations est toujours correct") // {displayName} dans le name fait référence à ceci
void stabiliteSoldeApresOperations() {
 CompteBancaire compte = new CompteBancaire(1L, "Test", new BigDecimal("100.00")); // BigDecimal("100.00") : précision décimale
 compte.deposer(new BigDecimal("50.00"), "Depot"); // deposer() : opération de dépôt, utilise BigDecimal
 compte.retirer(new BigDecimal("30.00"), "Retrait"); // retirer() : opération de retrait
 compte.deposer(new BigDecimal("20.00"), "Depot 2");
 // 100 + 50 - 30 + 20 = 140.00
 assertEquals(new BigDecimal("140.00"), compte.getSolde(),
  "Après 100+50-30+20 le solde doit toujours être 140.00"); // Message d'assertion : visible dans le rapport en cas d'échec
}
```

- `value = 10` : le test est exécuté 10 fois
- `name` : modèle de nommage pour chaque répétition
 - `{displayName}` : le nom défini par `@DisplayName`
 - `{currentRepetition}` : numéro de la répétition en cours (1, 2, ..., 10)
 - `{totalRepetitions}` : nombre total de répétitions (10)
- Le résultat dans le rapport : *"Stabilité : le solde après 3 opérations est toujours correct — répétition 3/10"*

### Pourquoi répéter un test ?

1. **Détecter des flaky tests** — Un test peut passer 9 fois sur 10 et échouer à la 10ᵉ à cause d'une condition de course, d'un état partagé non nettoyé, ou d'une dépendance à l'ordre d'exécution

2. **Vérifier l'absence d'état résiduel** — Après chaque répétition, JUnit crée une **nouvelle instance** de la classe de test. Le test doit donc passer de manière isolée 10 fois de suite

3. **Tester des opérations non déterministes** — Certains algorithmes (aléatoire, concurrence légère) peuvent produire des résultats différents à chaque exécution

### Ce que `@RepeatedTest` n'est pas

Ce n'est **pas** un test de performance ou de charge. Pour cela, on utilise JMH (Java Microbenchmark Harness) ou JMeter. Ici, on vérifie juste la **stabilité** (le même résultat 10 fois de suite).

---

## 1.6 `@Disabled` — Désactiver temporairement un test

L'annotation `@Disabled` désactive un test sans le supprimer.

### Syntaxe

```java
@Test
@Disabled("Bug #4521 : la validation du découvert n'est pas encore implémentée") // @Disabled : désactive le test (apparaît comme "disabled" dans le rapport)
void decouvertAutorise() { // Le test existe mais n'est pas exécuté tant que le @Disabled est présent
 // ...
}
```

### Comportement

- Le test n'est **pas exécuté**
- Dans le rapport, il apparaît comme **"disabled"** (ni succès ni échec)
- Le message fourni (`"Bug #4521 : ..."`) apparaît dans le rapport, ce qui documente la raison de la désactivation

### Quand l'utiliser

- Fonctionnalité en cours de développement (le test existe mais le code pas encore)
- Bug connu en attente de correctif
- Test qui dépend d'un service externe indisponible
- Test trop lent pour l'exécution locale (désactivé, exécuté seulement en CI)

> **Règle :** ne jamais commiter un `@Disabled` sans message explicatif et sans ticket associé. Un `@Disabled` sans justification est un test mort.

### Équivalent JUnit 4

En JUnit 4, l'annotation équivalente est `@Ignore`. En JUnit 5, c'est `@Disabled`. Ne pas les confondre.

---

## 1.7 Couverture de code avec JaCoCo

**JaCoCo** (Java Code Coverage) est un outil qui mesure la couverture de code par les tests. Il s'intègre à Maven via le plugin `jacoco-maven-plugin`.

### Types de couverture

| Métrique | Signification |
|----------|--------------|
| **Couverture de lignes** | Pourcentage de lignes de code exécutées au moins une fois |
| **Couverture de branches** | Pourcentage de branches conditionnelles (`if`, `switch`, `?:`) couvertes (les deux cas `true` et `false` doivent être testés) |
| **Couverture de méthodes** | Pourcentage de méthodes appelées au moins une fois |
| **Couverture de classes** | Pourcentage de classes dont au moins une méthode est testée |

### Configuration Maven dans le lab04

Le `pom.xml` du lab04 (lignes 54-88) configure JaCoCo avec trois goals :

#### 1. `prepare-agent` — Préparer l'agent JaCoCo

```xml
<execution>
 <id>prepare-agent</id>
 <goals><goal>prepare-agent</goal></goals>
</execution>
```

Cet objectif prépare l'agent JaCoCo qui s'attache à la JVM pendant l'exécution des tests. Il collecte les données de couverture en temps réel.

#### 2. `report` — Générer le rapport HTML

```xml
<execution>
 <id>report</id>
 <phase>test</phase>
 <goals><goal>report</goal></goals>
</execution>
```

Après les tests, JaCoCo génère un rapport dans `target/site/jacoco/index.html`. Ouvrir ce fichier dans un navigateur affiche :
- La couverture par package
- La couverture par classe
- Le code source avec les lignes en **vert** (couvertes), **rouge** (non couvertes), **jaune** (partiellement couvertes)

#### 3. `check` — Appliquer des seuils minimum

```xml
<execution>
 <id>check</id>
 <phase>test</phase>
 <goals><goal>check</goal></goals>
 <configuration>
 <rules>
 <rule>
 <element>BUNDLE</element>
 <limits>
 <limit>
 <counter>LINE</counter>
 <value>COVEREDRATIO</value>
 <minimum>0.80</minimum>
 </limit>
 </limits>
 </rule>
 </rules>
 </configuration>
</execution>
```

Cette règle impose une couverture de lignes **minimum de 80%** (0.80). Si la couverture est inférieure, le build Maven **échoue** avec un message explicite.

### Commande pour exécuter avec couverture

```bash
mvn clean test
```

Le rapport est généré dans `target/site/jacoco/index.html`.

### Seuils recommandés

| Métrique | Seuil minimum | Seuil idéal |
|----------|--------------|------------|
| Lignes | 80% | 90% |
| Branches | 70% | 80% |
| Méthodes | 90% | 95% |
| Classes | 90% | 100% |

> **Dans le lab04, JaCoCo est configuré avec un seuil de 80% de couverture de lignes.** C'est le seuil que vous devez atteindre et maintenir pendant le lab.

### Bonnes pratiques

- La couverture à 100% n'est **pas un objectif absolu** — certaines lignes (getters, toString) n'ont pas besoin de tests dédiés
- Une couverture haute avec de mauvais tests (sans assertions) est pire qu'une couverture plus basse avec de bons tests
- Le `check` JaCoCo ne remplace pas la revue humaine de la qualité des tests

---

## PARTIE 2 -- PRATIQUE PAS A PAS (50 min)

Nous allons décortiquer chaque groupe de tests du fichier `CompteBancaireTest.java` et du fichier `ServiceVirementTest.java`.

---

## Fichier 1 : `CompteBancaireTest.java` — Tests du modèle

### Groupe 1 : "Création du compte" (3 tests)

```java
// @Nested : groupe "Création du compte" — testé en premier dans le cycle TDD (Phase RED)
@Nested
@DisplayName("Création du compte") // @DisplayName : nom affiché dans l'arborescence de tests
class CreationCompte {
```

**Intention pédagogique :** valider le constructeur de `CompteBancaire`. Ces tests sont écrits en **premier** dans le cycle TDD. La classe `CompteBancaire` n'existe pas encore — on définit son API par les tests.

#### Test 1.1 — `creationAvecSoldeInitial`

```java
// Phase RED : écrit avant que CompteBancaire n'existe — test du cas nominal
@Test
@DisplayName("Un compte est créé avec un solde initial correct") // @DisplayName : description du comportement attendu
void creationAvecSoldeInitial() {
 // Arrange : new BigDecimal("1000.00") — le constructeur String garantit la précision décimale exacte
 CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("1000.00"));

 // Assert : trois vérifications indépendantes (id, titulaire, solde)
 assertEquals(1L, compte.getId());
 assertEquals("Alice", compte.getTitulaire());
 assertEquals(new BigDecimal("1000.00"), compte.getSolde()); // BigDecimal : equals() compare la valeur ET l'échelle
}
```

- **Arrange** : création d'un compte avec ID 1, titulaire "Alice", solde 1000.00€
- **Act** : le constructeur (l'acte est la construction elle-même)
- **Assert** : l'ID, le titulaire et le solde sont correctement stockés

**Cas nominal.** Utilisation de `BigDecimal` et non `double` pour les montants : `BigDecimal` garantit une précision décimale exacte, indispensable pour la manipulation d'argent.

#### Test 1.2 — `creationSoldeZero`

```java
// Phase RED : cas limite — le solde zéro doit être accepté (validation >= 0, pas > 0)
@Test
@DisplayName("Un compte peut être créé avec un solde initial de zéro") // @DisplayName : précise que zéro est valide
void creationSoldeZero() {
 CompteBancaire compte = new CompteBancaire(2L, "Bob", BigDecimal.ZERO); // BigDecimal.ZERO : constante pour la valeur 0
 assertEquals(BigDecimal.ZERO, compte.getSolde()); // compareTo(BigDecimal.ZERO) == 0 est la bonne comparaison
}
```

**Cas limite :** le solde zéro est valide. Ce test garantit que la validation `soldeInitial >= 0` utilise `>=` et non `>`.

#### Test 1.3 — `soldeInitialNegatifInterdit`

```java
// Phase RED : test du cas d'erreur — force l'ajout d'une validation
@Test
@DisplayName("Le solde initial ne peut pas être négatif") // @DisplayName : décrit la règle métier
void soldeInitialNegatifInterdit() {
 // assertThrows : vérifie que le code lève bien IllegalArgumentException
 // Le lambda est exécuté et l'exception attendue est capturée
 assertThrows(IllegalArgumentException.class,
  () -> new CompteBancaire(3L, "Charlie", new BigDecimal("-100.00"))); // BigDecimal("-100.00") : valeur négative
}
```

**Cas d'erreur :** le constructeur refuse un solde négatif. Ce test force l'ajout de la validation dans le constructeur :

```java
// Phase GREEN : code de validation ajouté pour faire passer le test de solde négatif
// compareTo() est la méthode de comparaison de BigDecimal (jamais == ni <, >)
if (soldeInitial.compareTo(BigDecimal.ZERO) < 0) { // compareTo() retourne -1 si inférieur, 0 si égal, 1 si supérieur
 throw new IllegalArgumentException("Le solde initial ne peut pas être négatif");
}
```

---

### Groupe 2 : "Opérations de dépôt" (4 tests)

```java
// @Nested : groupe "Opérations de dépôt" — tests de la méthode deposer()
@Nested
@DisplayName("Opérations de dépôt") // @DisplayName : sous-groupe fonctionnel
class Depot {
```

**Intention pédagogique :** valider la méthode `deposer()` — cas nominal, cas limites (zéro, négatif), et dépôts multiples.

#### Test 2.1 — `depotAugmenteSolde`

```java
// Phase RED : cas nominal du dépôt — solde doit augmenter du montant déposé
@Test
@DisplayName("Un dépôt augmente le solde") // @DisplayName : description du comportement métier
void depotAugmenteSolde() {
 // Arrange : création d'un compte avec 500.00€
 CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("500.00"));
 // Act : dépôt de 150.00€
 compte.deposer(new BigDecimal("150.00"), "Salaire"); // deposer(BigDecimal, String) : montant + description
 // Assert : 500 + 150 = 650
 assertEquals(new BigDecimal("650.00"), compte.getSolde()); // BigDecimal : comparaison exacte
}
```

**Cas nominal** : 500 + 150 = 650. Vérifie que `deposer()` ajoute bien le montant au solde.

#### Test 2.2 — `depotZeroInterdit`

```java
// Phase RED : cas d'erreur — dépôt de zéro interdit (règle métier)
@Test
@DisplayName("Dépôt de zéro est interdit") // @DisplayName : décrit le cas d'erreur
void depotZeroInterdit() {
 CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
 // assertThrows : le dépôt de BigDecimal.ZERO doit lever IllegalArgumentException
 assertThrows(IllegalArgumentException.class,
  () -> compte.deposer(BigDecimal.ZERO, "Dépôt nul")); // BigDecimal.ZERO : constante représentant 0
}
```

**Cas d'erreur** : un dépôt nul n'a pas de sens métier. La validation dans le code source utilise `compareTo(BigDecimal.ZERO) <= 0`, donc zéro ET négatif sont rejetés par la même condition.

#### Test 2.3 — `depotNegatifInterdit`

```java
// Phase RED : cas d'erreur — cohérence métier (dépôt négatif = retrait déguisé)
@Test
@DisplayName("Dépôt négatif est interdit") // @DisplayName : règle métier
void depotNegatifInterdit() {
 CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
 assertThrows(IllegalArgumentException.class,
  () -> compte.deposer(new BigDecimal("-50.00"), "Dépôt négatif")); // BigDecimal("-50.00") : valeur négative
}
```

**Cas d'erreur** : cohérence métier — on ne peut pas déposer un montant négatif (ce serait un retrait déguisé).

#### Test 2.4 — `plusieursDepots`

```java
// Phase RED : cas d'accumulation — vérifie que l'état persiste entre les appels
@Test
@DisplayName("Plusieurs dépôts successifs") // @DisplayName : accumulation de dépôts
void plusieursDepots() {
 CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00")); // Solde initial 100
 compte.deposer(new BigDecimal("50.00"), "Dépôt 1"); // 100 + 50 = 150
 compte.deposer(new BigDecimal("75.00"), "Dépôt 2"); // 150 + 75 = 225
 compte.deposer(new BigDecimal("25.00"), "Dépôt 3"); // 225 + 25 = 250
 assertEquals(new BigDecimal("250.00"), compte.getSolde()); // Vérification : 100 + 50 + 75 + 25 = 250
}
```

**Cas d'accumulation** : 100 + 50 + 75 + 25 = 250. Ce test garantit que l'état du compte persiste entre les appels et que les dépôts successifs s'additionnent correctement.

---

### Groupe 3 : "Opérations de retrait" (5 tests)

```java
// @Nested : groupe "Opérations de retrait" — tests de la méthode retirer()
@Nested
@DisplayName("Opérations de retrait") // @DisplayName : sous-groupe fonctionnel dans l'IDE
class Retrait {
```

**Intention pédagogique :** valider `retirer()` avec les mêmes patterns que `deposer()`, plus le cas critique du solde insuffisant.

#### Test 3.1 — `retraitDiminueSolde`

```java
// Phase RED : cas nominal du retrait — solde doit diminuer
@Test
@DisplayName("Un retrait diminue le solde") // @DisplayName : comportement attendu
void retraitDiminueSolde() {
 CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("500.00")); // Solde initial 500
 compte.retirer(new BigDecimal("200.00"), "Retrait DAB"); // retirer(BigDecimal, String) : montant + description
 assertEquals(new BigDecimal("300.00"), compte.getSolde()); // 500 - 200 = 300
}
```

#### Test 3.2 — `retraitTotal`

```java
// Phase RED : cas limite — retirer exactement le solde est autorisé (solde atteint 0)
@Test
@DisplayName("Retrait de la totalité du solde") // @DisplayName : description du cas limite
void retraitTotal() {
 CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00")); // Solde 100
 compte.retirer(new BigDecimal("100.00"), "Retrait total"); // Retrait de 100 = totalité
 assertEquals(BigDecimal.ZERO, compte.getSolde()); // BigDecimal.ZERO : solde doit être exactement 0
}
```

**Cas limite** : retirer exactement le solde est autorisé. Le solde atteint 0.00.

#### Test 3.3 — `retraitSuperieurAuSolde`

```java
// Phase RED : cas d'erreur critique — retrait > solde interdit (pas de découvert par défaut)
@Test
@DisplayName("Retrait supérieur au solde est interdit") // @DisplayName : règle métier du cas d'erreur
void retraitSuperieurAuSolde() {
 CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00")); // Solde 100
 assertThrows(IllegalArgumentException.class,
  () -> compte.retirer(new BigDecimal("200.00"), "Trop")); // Tentative de retrait de 200 avec 100 de solde
}
```

**Cas d'erreur critique** : sans cette validation, le compte pourrait passer en négatif. Le code source vérifie `montant.compareTo(this.solde) > 0` avant de soustraire.

#### Tests 3.4 et 3.5 — Zéro et négatif

Identiques aux tests de dépôt : `retirer(BigDecimal.ZERO)` et `retirer(new BigDecimal("-50.00"))` lèvent `IllegalArgumentException`.

---

### Groupe 4 : "Historique des transactions" (5 tests)

```java
// @Nested : groupe "Historique des transactions" — tests du système d'audit
@Nested
@DisplayName("Historique des transactions") // @DisplayName : sous-groupe pour l'historique
class Historique {
```

**Intention pédagogique :** valider le système d'historique — chaque opération crée une transaction enregistrée dans une liste, avec le bon type, le bon montant, et le solde après opération.

#### Test 4.1 — `compteNeufHistoriqueVide`

```java
// Phase RED : état initial — un compte neuf n'a aucune transaction
@Test
@DisplayName("Le compte neuf a un historique vide") // @DisplayName : état initial
void compteNeufHistoriqueVide() {
 CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
 assertTrue(compte.getHistorique().isEmpty()); // getHistorique() : retourne la liste des transactions
 assertEquals(0, compte.getNombreTransactions()); // getNombreTransactions() : 0 transaction à la création
}
```

**État initial** : à la création, l'historique est vide (0 transaction).

#### Test 4.2 — `depotCreeTransaction`

```java
// Phase RED : un dépôt doit créer une transaction d'audit avec les bonnes valeurs
@Test
@DisplayName("Un dépôt crée une transaction dans l'historique") // @DisplayName : comportement d'audit
void depotCreeTransaction() {
 CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
 compte.deposer(new BigDecimal("50.00"), "Dépôt test"); // Dépôt de 50€

 assertEquals(1, compte.getNombreTransactions()); // Une seule transaction créée
 Transaction derniere = compte.getDerniereTransaction(); // getDerniereTransaction() : retourne la dernière transaction
 assertNotNull(derniere); // Vérifie que la transaction n'est pas null
 assertEquals(Transaction.Type.DEPOT, derniere.getType()); // Le type doit être DEPOT
 assertEquals(new BigDecimal("50.00"), derniere.getMontant()); // BigDecimal : montant exact de la transaction
 assertEquals(new BigDecimal("150.00"), derniere.getSoldeApresOperation()); // BigDecimal : solde après opération = 100 + 50
}
```

Ce test valide que :
- Le nombre de transactions passe à 1
- La transaction a le type `DEPOT`
- Le montant de la transaction est 50.00€
- Le solde **après** opération (150.00€) est correctement enregistré — c'est crucial pour l'audit : on sait quel était le solde après chaque opération

#### Test 4.3 — `retraitCreeTransaction`

```java
// Phase RED : symétrique — un retrait crée aussi une transaction
@Test
@DisplayName("Un retrait crée une transaction dans l'historique") // @DisplayName : décrit le comportement d'audit
void retraitCreeTransaction() {
 CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
 compte.retirer(new BigDecimal("30.00"), "Retrait test"); // Retrait de 30€

 Transaction derniere = compte.getDerniereTransaction(); // getDerniereTransaction() : dernière transaction enregistrée
 assertEquals(Transaction.Type.RETRAIT, derniere.getType()); // Type RETRAIT (pas DEPOT)
 assertEquals(new BigDecimal("30.00"), derniere.getMontant()); // BigDecimal : montant de la transaction
 assertEquals(new BigDecimal("70.00"), derniere.getSoldeApresOperation()); // BigDecimal : solde après = 100 - 30 = 70
}
```

Même validation pour les retraits : type `RETRAIT`, solde après opération 70.00€.

#### Test 4.4 — `plusieursOperationsHistorique`

```java
// Phase RED : vérifie l'ordre chronologique et les soldes progressifs
@Test
@DisplayName("Plusieurs opérations créent des transactions ordonnées") // @DisplayName : ordre et contenu
void plusieursOperationsHistorique() {
 CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("0.00")); // Solde initial 0
 compte.deposer(new BigDecimal("1000.00"), "Salaire"); // 0 + 1000 = 1000
 compte.retirer(new BigDecimal("200.00"), "Loyer"); // 1000 - 200 = 800
 compte.deposer(new BigDecimal("50.00"), "Remboursement"); // 800 + 50 = 850

 List<Transaction> historique = compte.getHistorique(); // getHistorique() : retourne la liste ordonnée
 assertEquals(3, historique.size()); // 3 transactions au total

 // Vérification de l'ordre : index 0 = première opération, index 2 = dernière
 assertEquals(Transaction.Type.DEPOT, historique.get(0).getType()); // Première transaction : DEPOT
 assertEquals(Transaction.Type.RETRAIT, historique.get(1).getType()); // Deuxième : RETRAIT
 assertEquals(Transaction.Type.DEPOT, historique.get(2).getType()); // Troisième : DEPOT

 // Vérification du solde après chaque opération (progression du solde)
 assertEquals(new BigDecimal("1000.00"), historique.get(0).getSoldeApresOperation()); // BigDecimal : après salaire
 assertEquals(new BigDecimal("800.00"), historique.get(1).getSoldeApresOperation()); // BigDecimal : après loyer
 assertEquals(new BigDecimal("850.00"), historique.get(2).getSoldeApresOperation()); // BigDecimal : après remboursement
}
```

Ce test vérifie :
- L'**ordre chronologique** : les transactions sont enregistrées dans l'ordre d'exécution
- Les **types** : DEPOT, RETRAIT, DEPOT
- Le **solde progressif** : après chaque opération, le solde reflète l'accumulation exacte (0 → 1000 → 800 → 850)

#### Test 4.5 — `historiqueImmuable`

```java
// Phase RED : test d'encapsulation — l'historique est protégé contre les modifications externes
@Test
@DisplayName("L'historique est immuable (ne peut pas être modifié de l'extérieur)") // @DisplayName : encapsulation
void historiqueImmuable() {
 CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
 compte.deposer(new BigDecimal("50.00"), "Test");

 List<Transaction> historique = compte.getHistorique(); // getHistorique() retourne Collections.unmodifiableList()
 // Toute tentative de modification externe doit lever UnsupportedOperationException
 assertThrows(UnsupportedOperationException.class,
  () -> historique.add(null), // Tentative d'ajout = modification interdite
  "La liste retournée doit être non modifiable"); // Message d'assertion en cas d'échec
}
```

**Test d'encapsulation** : `getHistorique()` retourne une liste **non modifiable** grâce à `Collections.unmodifiableList()`. Une tentative de modification externe lève `UnsupportedOperationException`. Cela protège l'intégrité de l'historique.

#### Test 4.6 — `derniereTransactionNullSiVide`

```java
// Phase RED : cas limite — getDerniereTransaction() sur un historique vide retourne null
@Test
@DisplayName("getDerniereTransaction retourne null si historique vide") // @DisplayName : comportement du cas limite
void derniereTransactionNullSiVide() {
 CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
 assertNull(compte.getDerniereTransaction()); // getDerniereTransaction() : null si aucune transaction (pas d'exception)
}
```

**Cas limite** : `getDerniereTransaction()` sur un historique vide retourne `null` (pas d'exception). Le code source gère cela : `if (historique.isEmpty()) return null;`.

---

### Groupe 5 : "Virements internes au compte" (5 tests)

```java
// @Nested : groupe "Virements internes au compte" — tests de emettreVirement() et recevoirVirement()
@Nested
@DisplayName("Virements internes au compte") // @DisplayName : sous-groupe pour les méthodes de virement
class OperationsVirement {
```

**Intention pédagogique :** valider les méthodes `emettreVirement()` et `recevoirVirement()` qui seront utilisées par `ServiceVirement`.

#### Test 5.1 — `emissionVirementDebite`

```java
// Phase RED : émission d'un virement débite le compte émetteur
@Test
@DisplayName("Émission d'un virement débite le compte") // @DisplayName : comportement attendu
void emissionVirementDebite() {
 CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("1000.00")); // Solde initial 1000
 compte.emettreVirement(new BigDecimal("300.00"), "Paiement facture"); // emettreVirement() : débite le compte
 assertEquals(new BigDecimal("700.00"), compte.getSolde()); // BigDecimal : 1000 - 300 = 700
}
```

#### Test 5.2 — `receptionVirementCredite`

```java
// Phase RED : réception d'un virement crédite le compte bénéficiaire
@Test
@DisplayName("Réception d'un virement crédite le compte") // @DisplayName : comportement symétrique
void receptionVirementCredite() {
 CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("500.00")); // Solde initial 500
 compte.recevoirVirement(new BigDecimal("200.00"), "Remboursement"); // recevoirVirement() : crédite le compte
 assertEquals(new BigDecimal("700.00"), compte.getSolde()); // BigDecimal : 500 + 200 = 700
}
```

**Différence avec `deposer` :** `recevoirVirement` ne vérifie pas si le montant est positif — c'est la responsabilité de `ServiceVirement`. `recevoirVirement` est une méthode interne utilisée uniquement par le service.

#### Tests 5.3 et 5.4 — Types de transaction

```java
// Phase RED : le virement émis crée une transaction de type VIREMENT_EMIS
@Test
@DisplayName("Le virement émis crée une transaction de type VIREMENT_EMIS") // @DisplayName : vérification du type
void transactionVirementEmis() {
 CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("500.00"));
 compte.emettreVirement(new BigDecimal("100.00"), "Test virement"); // emettreVirement() : crée transaction VIREMENT_EMIS
 assertEquals(Transaction.Type.VIREMENT_EMIS, // VIREMENT_EMIS : distingue les flux sortants
  compte.getDerniereTransaction().getType()); // getDerniereTransaction().getType() : type de la dernière opération
}
```

Chaque type de virement crée une transaction avec le bon type (`VIREMENT_EMIS` ou `VIREMENT_RECU`), ce qui permet de distinguer les flux sortants des flux entrants dans l'historique.

#### Test 5.5 — `virementEmisSuperieurSolde`

```java
// Phase RED : cas d'erreur — virement supérieur au solde interdit
@Test
@DisplayName("Virement émis supérieur au solde interdit") // @DisplayName : règle métier
void virementEmisSuperieurSolde() {
 CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00")); // Solde 100
 assertThrows(IllegalArgumentException.class, // L'exception est levée par emettreVirement()
  () -> compte.emettreVirement(new BigDecimal("200.00"), "Trop")); // Tentative de virement de 200 avec 100 de solde
}
```

**Cas d'erreur** : idem retrait, le solde doit être suffisant. Cette validation est dans `emettreVirement`, pas dans `ServiceVirement` — le modèle protège ses propres invariants.

---

### Groupe 6 : "Concurrence et thread-safety" (2 tests)

```java
// @Nested : groupe "Concurrence et thread-safety" — tests de synchronisation multi-thread
@Nested
@DisplayName("Concurrence et thread-safety") // @DisplayName : sous-groupe dédié à la concurrence
class Concurrence {
```

**Intention pédagogique :** démontrer que les méthodes `synchronized` protègent correctement l'état du compte en environnement multi-threadé. C'est un aspect critique pour une application bancaire réelle.

#### Concepts utilisés

Avant d'analyser les tests, voici les trois classes Java utilisées :

**`ExecutorService`** (depuis Java 5) : un pool de threads qui exécute des tâches (`Runnable`). `Executors.newFixedThreadPool(n)` crée un pool de n threads. `executor.submit(task)` soumet une tâche. `executor.shutdown()` arrête le pool après la fin des tâches. `executor.awaitTermination(t, unit)` attend la fin de toutes les tâches (avec timeout).

**`CountDownLatch`** : un compteur à rebours initialisé à N. `latch.countDown()` décrémente le compteur. `latch.await()` bloque le thread courant jusqu'à ce que le compteur atteigne 0. Utilisé ici pour synchroniser la fin de tous les threads avant de vérifier le résultat.

**`synchronized`** (mot-clé Java) : garantit qu'une seule thread à la fois exécute une méthode sur un objet donné. Toutes les méthodes de `CompteBancaire` qui modifient l'état (`deposer`, `retirer`, `emettreVirement`, `recevoirVirement`) sont `synchronized`. Sans cela, deux threads pourraient lire le même solde, chacun ajouter son montant, et écraser le résultat de l'autre.

#### Test 6.1 — `depotsConcurrents`

```java
// Phase RED : teste la thread-safety — dépôts concurrents sur un même compte
@Test
@DisplayName("Dépôts concurrents : le solde final est correct") // @DisplayName : intention du test concurrent
@Timeout(value = 5, unit = TimeUnit.SECONDS) // @Timeout : sécurité anti-blocage (deadlock ou boucle infinie)
void depotsConcurrents() throws InterruptedException { // throws InterruptedException : requis pour await()
 // Arrange : création d'un compte à 0€
 CompteBancaire compte = new CompteBancaire(1L, "Alice", BigDecimal.ZERO); // BigDecimal.ZERO : solde initial 0
 int nbThreads = 10; // Nombre de threads concurrents
 int nbOperations = 100; // Nombre d'opérations par thread
 BigDecimal montant = BigDecimal.ONE; // BigDecimal.ONE : constante pour la valeur 1

 // ExecutorService : pool de threads pour exécuter des tâches parallèles
 ExecutorService executor = Executors.newFixedThreadPool(nbThreads); // newFixedThreadPool(n) : pool de n threads
 // CountDownLatch : synchroniseur — attend que tous les threads aient terminé
 CountDownLatch latch = new CountDownLatch(nbThreads); // Initialisé au nombre de threads à attendre

 for (int i = 0; i < nbThreads; i++) {
  executor.submit(() -> { // submit() : soumet une tâche Runnable au pool
   try {
    for (int j = 0; j < nbOperations; j++) {
     compte.deposer(montant, "Dépôt concurrent"); // deposer() est synchronized : un seul thread à la fois
    }
   } finally {
    latch.countDown(); // countDown() : décrémente le compteur, toujours dans finally (même si exception)
   }
  });
 }

 latch.await(); // await() : bloque le thread principal jusqu'à ce que le compteur atteigne 0
 executor.shutdown(); // shutdown() : arrêt du pool après la fin des tâches
 executor.awaitTermination(5, TimeUnit.SECONDS); // awaitTermination() : attend l'arrêt complet avec timeout

 // Assert : solde attendu = 10 threads × 100 opérations × 1€ = 1000€
 // Sans synchronized, le solde serait < 1000 à cause des race conditions
 BigDecimal attendu = new BigDecimal(nbThreads * nbOperations); // BigDecimal : multiplication exacte (int × int)
 assertEquals(attendu, compte.getSolde(),
  "Avec " + nbThreads + " threads × " + nbOperations + " dépôts de 1€, le solde doit être " + attendu + "€");
 assertEquals(nbThreads * nbOperations, compte.getNombreTransactions()); // Vérifie aussi le nombre de transactions
}
```

**Déroulement :**

1. **Création du compte** à 0€
2. **10 threads** sont créées, chacune effectuant **100 dépôts de 1€** (soit 1000 opérations au total)
3. Chaque thread, après avoir terminé ses 100 dépôts, appelle `latch.countDown()` dans un bloc `finally` (garantit le countDown même en cas d'exception)
4. Le thread principal (`latch.await()`) attend que les 10 threads aient terminé
5. `executor.shutdown()` puis `awaitTermination(5, SECONDS)` : attente gracieuse de l'arrêt du pool
6. **Assertion** : le solde doit être exactement 10 × 100 = 1000€

Si les méthodes n'étaient pas `synchronized`, des race conditions se produiraient : deux threads liraient le solde en même temps, feraient `solde + 1` chacun, et écriraient le même résultat, perdant un dépôt. Le solde final serait < 1000€.

#### Test 6.2 — `operationsMixtesConcurrentes`

```java
// Phase RED : test de concurrence plus exigeant — dépôts ET retraits simultanés
@Test
@DisplayName("Dépôts et retraits concurrents : intégrité du solde") // @DisplayName : test mixte dépôts/retraits
@Timeout(value = 5, unit = TimeUnit.SECONDS) // @Timeout : protège contre le blocage en cas de deadlock
void operationsMixtesConcurrentes() throws InterruptedException {
 // Arrange : solde initial 1000€
 CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("1000.00"));
 int nbThreads = 5; // 5 threads au total
 BigDecimal montant = new BigDecimal("1.00"); // BigDecimal("1.00") : montant unitaire pour chaque opération

 // ExecutorService : pool de threads
 ExecutorService executor = Executors.newFixedThreadPool(nbThreads); // newFixedThreadPool(5) : jusqu'à 5 threads
 CountDownLatch latch = new CountDownLatch(nbThreads); // Compteur initialisé à 5

 // Première boucle : nbThreads/2 + 1 = 3 threads de dépôt
 // Chaque thread effectue 50 dépôts de 1€ → 150 dépôts au total
 for (int i = 0; i < nbThreads / 2 + 1; i++) {
  executor.submit(() -> {
   try {
    for (int j = 0; j < 50; j++) {
     compte.deposer(montant, "Dépôt"); // deposer() synchronized : exclusion mutuelle
    }
   } finally {
    latch.countDown(); // finally : garantit countDown() même en cas d'exception
   }
  });
 }
 // Deuxième boucle : les 2 threads restants font des retraits
 // Chaque thread effectue 50 retraits de 1€ → 100 retraits au total
 for (int i = nbThreads / 2 + 1; i < nbThreads; i++) {
  executor.submit(() -> {
   try {
    for (int j = 0; j < 50; j++) {
     compte.retirer(montant, "Retrait"); // retirer() synchronized : exclusion mutuelle
    }
   } finally {
    latch.countDown(); // finally : décrémente même si une exception survient
   }
  });
 }

 latch.await(); // await() : attend que les 5 threads aient terminé
 executor.shutdown(); // shutdown() : arrêt gracieux du pool
 executor.awaitTermination(5, TimeUnit.SECONDS); // awaitTermination() : attente avec timeout

 int nbDepots = 3 * 50; // 3 threads de dépôt × 50 opérations = 150 dépôts
 int nbRetraits = 2 * 50; // 2 threads de retrait × 50 opérations = 100 retraits

 // BigDecimal : calcul du solde attendu avec add() et subtract()
 // 1000 (initial) + 150 (dépôts) - 100 (retraits) = 1050
 BigDecimal attendu = new BigDecimal("1000.00")
  .add(new BigDecimal(nbDepots)) // add() : addition BigDecimal, ne pas utiliser +
  .subtract(new BigDecimal(nbRetraits)); // subtract() : soustraction BigDecimal

 assertEquals(attendu, compte.getSolde()); // Sans synchronized, le solde serait incorrect
}
```

**Déroulement :**

- `nbThreads = 5`, donc 5 threads au total
- `nbThreads / 2 + 1 = 3` threads font des **dépôts** (50 chacun → 150 dépôts)
- Les `2` threads restantes font des **retraits** (50 chacun → 100 retraits)
- Solde initial : 1000€
- Solde attendu : 1000 + 150 - 100 = 1050€

Ce test est plus exigeant car il mélange lectures et écritures concurrentes de types opposés (dépôt/retrait), ce qui multiplie les risques de race condition.

---

### Groupe 7 : `@RepeatedTest` — Stabilité

```java
// @RepeatedTest : exécute le test 10 fois pour vérifier l'absence d'état résiduel (flaky test)
// value = 10 : 10 exécutions indépendantes (nouvelle instance de classe à chaque répétition)
// name : modèle d'affichage dans le rapport — {currentRepetition}/{totalRepetitions} donne "3/10"
@RepeatedTest(value = 10, name = "{displayName} — répétition {currentRepetition}/{totalRepetitions}")
@DisplayName("Stabilité : le solde après 3 opérations est toujours correct") // @DisplayName : décrit l'objectif de stabilité
void stabiliteSoldeApresOperations() {
 // Chaque répétition crée un nouveau compte isolé
 CompteBancaire compte = new CompteBancaire(1L, "Test", new BigDecimal("100.00")); // BigDecimal : solde initial 100
 compte.deposer(new BigDecimal("50.00"), "Depot"); // 100 + 50 = 150
 compte.retirer(new BigDecimal("30.00"), "Retrait"); // 150 - 30 = 120
 compte.deposer(new BigDecimal("20.00"), "Depot 2"); // 120 + 20 = 140
 // Assert : après 100+50-30+20, le solde doit TOUJOURS être 140.00 (quelle que soit la répétition)
 assertEquals(new BigDecimal("140.00"), compte.getSolde(),
  "Après 100+50-30+20 le solde doit toujours être 140.00"); // Message visible dans le rapport
}
```

Ce test est exécuté **10 fois**. Chaque exécution crée un nouveau compte, effectue trois opérations, et vérifie que 100 + 50 - 30 + 20 = 140 exactement. Ce test n'est pas concurrentiel, mais il vérifie qu'il n'y a pas d'état statique résiduel entre les exécutions (fuite mémoire, variable `static` mal nettoyée, etc.).

Le `name` personnalisé permet d'identifier chaque répétition dans le rapport de test.

---

## Fichier 2 : `ServiceVirementTest.java` — Tests du service

```java
@DisplayName("TDD : Service de Virement") // @DisplayName sur la classe : nom racine dans l'arborescence de tests
class ServiceVirementTest {

 private ServiceVirement service; // Service à tester (instance réelle, pas de mock)
 private CompteBancaire compteAlice; // Compte source pour les virements
 private CompteBancaire compteBob; // Compte destination pour les virements

 // @BeforeEach : exécuté avant CHAQUE test — garantit un état propre et isolé
 @BeforeEach
 void setUp() {
  service = new ServiceVirement(); // Nouvelle instance du service à chaque test
  compteAlice = new CompteBancaire(1L, "Alice", new BigDecimal("1000.00")); // Alice : solde 1000€
  compteBob = new CompteBancaire(2L, "Bob", new BigDecimal("500.00")); // Bob : solde 500€
 }
```

**`@BeforeEach`** : avant chaque test, on crée un service et deux comptes (Alice: 1000€, Bob: 500€). Chaque test démarre avec un état propre.

### Groupe 1 : "Virement standard" (3 tests)

#### Test 1.1 — `virementDebiteSourceCrediteDestination`

```java
// Phase RED : test du cas nominal — virement entre deux comptes
@Test
@DisplayName("Le virement débite la source et crédite la destination") // @DisplayName : les deux effets attendus
void virementDebiteSourceCrediteDestination() {
 // Act : effectuerVirement(source, destination, montant, description)
 service.effectuerVirement(compteAlice, compteBob,
  new BigDecimal("200.00"), "Remboursement"); // BigDecimal("200.00") : montant du virement

 // Assert : les deux comptes sont modifiés
 assertEquals(new BigDecimal("800.00"), compteAlice.getSolde(), // BigDecimal : Alice 1000 - 200 = 800
  "Alice doit être débitée de 200€"); // Message d'assertion : visible en cas d'échec
 assertEquals(new BigDecimal("700.00"), compteBob.getSolde(), // BigDecimal : Bob 500 + 200 = 700
  "Bob doit être crédité de 200€"); // Vérifier un seul compte ne suffit pas
}
```

**Cas nominal** : Alice (1000€) vire 200€ à Bob (500€). Après virement, Alice a 800€, Bob a 700€. Les deux assertions sont nécessaires : vérifier un seul compte ne suffit pas.

#### Test 1.2 — `virementCreeTransactions`

```java
// Phase RED : traçabilité — un virement crée une transaction chez l'émetteur ET le bénéficiaire
@Test
@DisplayName("Le virement crée une transaction chez l'émetteur et le bénéficiaire") // @DisplayName : traçabilité
void virementCreeTransactions() {
 service.effectuerVirement(compteAlice, compteBob,
  new BigDecimal("100.00"), "Cadeau"); // Virement de 100€

 // Chaque compte doit avoir exactement 1 transaction (pas plus)
 assertEquals(1, compteAlice.getNombreTransactions()); // Alice : 1 transaction
 assertEquals(1, compteBob.getNombreTransactions()); // Bob : 1 transaction
 assertEquals(
  com.nexa.banque.model.Transaction.Type.VIREMENT_EMIS, // Type VIREMENT_EMIS pour l'émetteur
  compteAlice.getDerniereTransaction().getType()); // getDerniereTransaction().getType() : type de la transaction
 assertEquals(
  com.nexa.banque.model.Transaction.Type.VIREMENT_RECU, // Type VIREMENT_RECU pour le bénéficiaire
  compteBob.getDerniereTransaction().getType()); // Permet de distinguer flux entrants et sortants
}
```

Ce test valide la **traçabilité** : chaque virement crée une transaction chez l'émetteur (type `VIREMENT_EMIS`) et une chez le bénéficiaire (type `VIREMENT_RECU`). Les deux comptes ont exactement 1 transaction (pas plus).

#### Test 1.3 — `sommeSoldesConservee`

```java
// Phase RED : invariant fondamental — l'argent ne se crée ni ne se détruit
@Test
@DisplayName("Somme des soldes conservée après virement") // @DisplayName : conservation de la masse monétaire
void sommeSoldesConservee() {
 // BigDecimal.add() : addition de deux BigDecimal (jamais +)
 BigDecimal sommeAvant = compteAlice.getSolde().add(compteBob.getSolde()); // 1000 + 500 = 1500

 service.effectuerVirement(compteAlice, compteBob,
  new BigDecimal("150.00"), "Test conservation"); // Virement de 150€

 BigDecimal sommeApres = compteAlice.getSolde().add(compteBob.getSolde()); // 850 + 650 = 1500
 // La somme avant et après doit être identique : aucun centime perdu ou créé
 assertEquals(sommeAvant, sommeApres,
  "La somme totale des soldes doit être conservée"); // Invariant comptable fondamental
}
```

**Invariant fondamental** : un virement ne crée ni ne détruit d'argent. La somme des soldes avant (1000 + 500 = 1500€) doit être égale à la somme après (850 + 650 = 1500€). Ce test garantit qu'aucun centime n'est perdu ou créé pendant le transfert.

### Groupe 2 : "Cas d'erreur" (4 tests)

```java
// @Nested : groupe "Cas d'erreur" — tests des validations métier du service de virement
@Nested
@DisplayName("Cas d'erreur") // @DisplayName : sous-groupe pour les cas d'erreur
class CasErreur {
```

#### Test 2.1 — `virementMemeCompteInterdit`

```java
// Phase RED : cas d'erreur — virement vers soi-même interdit
@Test
@DisplayName("Virement vers le même compte interdit") // @DisplayName : règle métier
void virementMemeCompteInterdit() {
 // assertThrows : la source et la destination sont le même compte → exception
 assertThrows(IllegalArgumentException.class,
  () -> service.effectuerVirement(compteAlice, compteAlice, // Même compte pour source et destination
   new BigDecimal("100.00"), "Moi-même")); // BigDecimal : montant du virement (ignoré car exception)
}
```

Le code source vérifie `source.equals(destination)` avant toute opération.

#### Test 2.2 — `montantNulInterdit`

```java
// Phase RED : cas d'erreur — montant nul interdit
@Test
@DisplayName("Montant nul interdit") // @DisplayName : règle métier
void montantNulInterdit() {
 assertThrows(IllegalArgumentException.class,
  () -> service.effectuerVirement(compteAlice, compteBob,
   BigDecimal.ZERO, "Zéro")); // BigDecimal.ZERO : montant nul rejeté
}
```

#### Test 2.3 — `montantNegatifInterdit`

```java
// Phase RED : cas d'erreur — montant négatif interdit
@Test
@DisplayName("Montant négatif interdit") // @DisplayName : règle métier
void montantNegatifInterdit() {
 assertThrows(IllegalArgumentException.class,
  () -> service.effectuerVirement(compteAlice, compteBob,
   new BigDecimal("-50.00"), "Négatif")); // BigDecimal("-50.00") : montant négatif rejeté
}
```

#### Test 2.4 — `soldeInsuffisantEmetteur`

```java
@Test
@DisplayName("Solde insuffisant chez l'émetteur")
void soldeInsuffisantEmetteur() {
 assertThrows(IllegalArgumentException.class,
 () -> service.effectuerVirement(compteAlice, compteBob,
 new BigDecimal("2000.00"), "Trop"));
}
```

Alice a 1000€, elle tente de virer 2000€. L'exception est levée par `emettreVirement()` dans `CompteBancaire` (pas par `ServiceVirement`), mais le service laisse l'exception se propager sans l'intercepter.

---

## PARTIE 3 -- LAB (55 min)

## Objectif

Ajouter la fonctionnalité de **découvert autorisé** au système bancaire en suivant la méthodologie TDD.

## Contexte

Jusqu'à présent, un compte ne peut jamais avoir un solde négatif : toute tentative de retrait supérieur au solde lève une exception. La banque souhaite maintenant proposer un **découvert autorisé** : un montant maximum en dessous de zéro que le client peut atteindre.

Par exemple, avec un découvert autorisé de 500€ et un solde de 200€, le client peut retirer jusqu'à 700€ (son solde passera à -500€).

## Consignes

### Étape 1 — Ajouter le champ `decouvertAutorise` à `CompteBancaire` (TDD)

**Phase RED** — Écrire d'abord le test qui échoue :

Dans `CompteBancaireTest.java`, ajouter un `@Nested` "Découvert autorisé" avec ces tests :

1. **`creationAvecDecouvert`** : `new CompteBancaire(1L, "Test", new BigDecimal("100.00"), new BigDecimal("500.00"))` crée un compte avec 500€ de découvert. Vérifier que `getDecouvertAutorise()` retourne 500.00.
2. **`decouvertParDefautZero`** : quand on ne spécifie pas le découvert, il vaut `BigDecimal.ZERO` (compatibilité ascendante avec le constructeur à 3 paramètres).
3. **`decouvertNegatifInterdit`** : la création avec un découvert négatif lève `IllegalArgumentException`.

**Phase GREEN** — Modifier `CompteBancaire` :
- Ajouter le champ `private BigDecimal decouvertAutorise;`
- Ajouter un second constructeur `CompteBancaire(Long id, String titulaire, BigDecimal soldeInitial, BigDecimal decouvertAutorise)`
- Modifier le constructeur existant pour appeler `this(id, titulaire, soldeInitial, BigDecimal.ZERO)`
- Ajouter le getter `getDecouvertAutorise()`

**Phase RED** — Test suivant :

```java
// Phase RED : nouveau test qui force une validation qui n'existe pas encore
@Test
@DisplayName("Le solde initial ne peut pas être négatif") // @DisplayName : décrit l'intention du test
void soldeInitialNegatifInterdit() {
 // assertThrows : vérifie qu'une exception est bien levée
 // Le lambda () -> new CompteBancaire(...) décrit l'action qui doit échouer
 assertThrows(IllegalArgumentException.class,
  () -> new CompteBancaire(3L, "Charlie", new BigDecimal("-100.00"))); // BigDecimal négatif : test du cas d'erreur
}
```

Appliquer la même logique à `emettreVirement()`.

### Étape 2 — Créer `ServiceDecouvert` en TDD

Créez le fichier `src/main/java/com/nexa/banque/service/ServiceDecouvert.java` :

**Phase RED** — Écrire `ServiceDecouvertTest.java` d'abord :

1. **`retraitAvecDecouvertSuffisant`** : un compte avec solde 50€, découvert 200€. `serviceDecouvert.retirerAvecDecouvert(compte, 150€, "Retrait")` → le retrait réussit, solde = -100€
2. **`retraitAvecDecouvertInsuffisant`** : même compte. `serviceDecouvert.retirerAvecDecouvert(compte, 300€, "Retrait")` → exception `SoldeInsuffisantException`
3. **`verifierDecouvert_disponible`** : `serviceDecouvert.verifierDecouvert(compte, 100€)` sur un compte avec découvert 500€ retourne `true`
4. **`verifierDecouvert_indisponible`** : `serviceDecouvert.verifierDecouvert(compte, 100€)` sur un compte avec découvert 0€ retourne `false`

**Phase GREEN** — Implémenter `ServiceDecouvert` :

```java
package com.nexa.banque.service;

import com.nexa.banque.exception.SoldeInsuffisantException;
import com.nexa.banque.model.CompteBancaire;
import java.math.BigDecimal;

public class ServiceDecouvert {

 public void retirerAvecDecouvert(CompteBancaire compte, BigDecimal montant, String description) {
 if (!verifierDecouvert(compte, montant)) {
 throw new SoldeInsuffisantException(
 String.format("Découvert insuffisant : solde %s€, découvert %s€, demande %s€",
 compte.getSolde(), compte.getDecouvertAutorise(), montant));
 }
 compte.retirer(montant, description);
 }

 public boolean verifierDecouvert(CompteBancaire compte, BigDecimal montant) {
 BigDecimal soldeDisponible = compte.getSolde().add(compte.getDecouvertAutorise());
 return soldeDisponible.compareTo(montant) >= 0;
 }
}
```

### Étape 3 — Exécuter les tests avec couverture

```bash
mvn clean test
```

Vérifier que le build passe (y compris le check JaCoCo à 80%).

### Étape 4 — Vérifier le rapport de couverture

```bash
# Ouvrir le rapport dans un navigateur
firefox target/site/jacoco/index.html
```

Vérifier que toutes les nouvelles lignes sont couvertes (vertes dans le rapport).

## Critères de réussite

- [ ] Tous les tests passent au vert (`mvn test` sans erreur)
- [ ] La couverture de lignes est ≥ 80% (le `check` JaCoCo du `pom.xml` passe)
- [ ] TDD respecté : les tests ont été écrits **avant** le code (vous pouvez le prouver en montrant l'historique des commits)
- [ ] Le champ `decouvertAutorise` est ajouté à `CompteBancaire`
- [ ] `retirer()` et `emettreVirement()` autorisent un solde négatif dans la limite du découvert
- [ ] `ServiceDecouvert` est testé avec les cas nominal et d'erreur
- [ ] `SoldeInsuffisantException` existante est utilisée pour `ServiceDecouvert`

---

## FICHE MEMO -- Annotations JUnit 5

| Annotation | Rôle | Syntaxe | Exemple du lab |
|-----------|------|---------|---------------|
| `@Nested` | Classe interne de test pour organisation hiérarchique | `@Nested class Depot { ... }` | `CompteBancaireTest` : 6 groupes (`CreationCompte`, `Depot`, `Retrait`, `Historique`, `OperationsVirement`, `Concurrence`) |
| `@Tag("nom")` | Étiquette pour filtrer l'exécution | `@Tag("lent")` sur un `@Test` ou une classe | Non utilisé dans le lab04, à appliquer pour le lab |
| `@Timeout(value=n, unit=...)` | Durée max d'exécution du test | `@Timeout(value=5, unit=TimeUnit.SECONDS)` | Tests de concurrence : `depotsConcurrents`, `operationsMixtesConcurrentes` |
| `@RepeatedTest(n)` | Exécute le test n fois | `@RepeatedTest(value=10, name="{displayName} — {currentRepetition}/{totalRepetitions}")` | `stabiliteSoldeApresOperations` exécuté 10 fois |
| `@Disabled("raison")` | Désactive temporairement un test | `@Disabled("Bug #4521")` sur un `@Test` | Non utilisé dans le lab04, pattern à connaître |

| Annotation Mockito | Rôle | Syntaxe |
|--------------------|------|---------|
| `@ExtendWith(MockitoExtension.class)` | Active Mockito avec JUnit 5 | Sur la classe de test |
| `@Mock` | Crée un simulacre | `@Mock private Service mock;` |
| `@InjectMocks` | Injecte les `@Mock` dans une instance réelle | `@InjectMocks private Service service;` |

| Méthode TDD | Phase | Action |
|-------------|-------|--------|
| Écrire un test qui échoue | RED | Décrit le comportement attendu AVANT le code |
| Écrire le minimum de code | GREEN | Juste assez pour que le test passe |
| Améliorer le code | REFACTOR | Renommer, extraire, optimiser sans casser les tests |

| Concept JaCoCo | Signification |
|----------------|-------------|
| `prepare-agent` | Prépare l'agent de collecte des données de couverture |
| `report` | Génère le rapport HTML dans `target/site/jacoco/` |
| `check` | Fait échouer le build si les seuils ne sont pas atteints |
| `LINE` / `COVEREDRATIO` | Couverture de lignes, ratio de 0.0 à 1.0 (0.80 = 80%) |

| Classe de concurrence | Rôle |
|----------------------|------|
| `ExecutorService` | Pool de threads pour exécuter des tâches parallèles |
| `CountDownLatch` | Synchronisation : attendre que N threads aient terminé |
| `synchronized` | Verrou d'exclusion mutuelle sur une méthode d'instance |

> **Rappel TDD :** ne jamais écrire de code de production avant d'avoir un test qui échoue. Si le test passe du premier coup sans code ajouté, c'est un faux positif — le test est mal écrit ou teste un comportement déjà existant.
