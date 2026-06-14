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
@Test
@DisplayName("Un compte est créé avec un solde initial correct")
void creationAvecSoldeInitial() {
 CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("1000.00"));

 assertEquals(1L, compte.getId());
 assertEquals("Alice", compte.getTitulaire());
 assertEquals(new BigDecimal("1000.00"), compte.getSolde());
}
```

À ce stade, la classe `CompteBancaire` n'existe pas. Le code ne compile même pas. C'est le **RED le plus extrême** : échec de compilation.

### Phase GREEN — Écrire le minimum de code

On ecrit **le minimum necessaire** de code pour que le test passe. Pas de fonctionnalites supplementaires, pas d'optimisation, pas de generalisation.

```java
public class CompteBancaire {
 private final Long id;
 private final String titulaire;
 private BigDecimal solde;

 public CompteBancaire(Long id, String titulaire, BigDecimal soldeInitial) {
 this.id = id;
 this.titulaire = titulaire;
 this.solde = soldeInitial;
 }

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
public CompteBancaire(Long id, String titulaire, BigDecimal soldeInitial) {
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
@DisplayName("TDD : Compte Bancaire")
class CompteBancaireTest {

 @Nested
 @DisplayName("Création du compte")
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
@Tag("unitaire")
void testRapide() { ... }

@Test
@Tag("integration")
@Tag("lent")
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
@Timeout(value = 5, unit = TimeUnit.SECONDS)
void testQuiPourraitBloquer() throws InterruptedException {
 // ...
}
```

### Pourquoi `@Timeout` est crucial

Dans les tests de **concurrence**, un thread peut se bloquer indéfiniment (deadlock, boucle infinie, `await()` sans `countDown()`). Sans timeout, le test bloquerait la suite de tests **pour toujours** (ou jusqu'à ce que le CI tue le build après 10 minutes).

### Exemple du lab04

Les deux tests de la classe `Concurrence` utilisent `@Timeout(value = 5, unit = TimeUnit.SECONDS)` :

```java
@Nested
@DisplayName("Concurrence et thread-safety")
class Concurrence {

 @Test
 @DisplayName("Dépôts concurrents : le solde final est correct")
 @Timeout(value = 5, unit = TimeUnit.SECONDS)
 void depotsConcurrents() throws InterruptedException {
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
@RepeatedTest(value = 10, name = "{displayName} — répétition {currentRepetition}/{totalRepetitions}")
@DisplayName("Stabilité : le solde après 3 opérations est toujours correct")
void stabiliteSoldeApresOperations() {
 CompteBancaire compte = new CompteBancaire(1L, "Test", new BigDecimal("100.00"));
 compte.deposer(new BigDecimal("50.00"), "Depot");
 compte.retirer(new BigDecimal("30.00"), "Retrait");
 compte.deposer(new BigDecimal("20.00"), "Depot 2");
 assertEquals(new BigDecimal("140.00"), compte.getSolde(),
 "Après 100+50-30+20 le solde doit toujours être 140.00");
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
@Disabled("Bug #4521 : la validation du découvert n'est pas encore implémentée")
void decouvertAutorise() {
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
@Nested
@DisplayName("Création du compte")
class CreationCompte {
```

**Intention pédagogique :** valider le constructeur de `CompteBancaire`. Ces tests sont écrits en **premier** dans le cycle TDD. La classe `CompteBancaire` n'existe pas encore — on définit son API par les tests.

#### Test 1.1 — `creationAvecSoldeInitial`

```java
@Test
@DisplayName("Un compte est créé avec un solde initial correct")
void creationAvecSoldeInitial() {
 CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("1000.00"));

 assertEquals(1L, compte.getId());
 assertEquals("Alice", compte.getTitulaire());
 assertEquals(new BigDecimal("1000.00"), compte.getSolde());
}
```

- **Arrange** : création d'un compte avec ID 1, titulaire "Alice", solde 1000.00€
- **Act** : le constructeur (l'acte est la construction elle-même)
- **Assert** : l'ID, le titulaire et le solde sont correctement stockés

**Cas nominal.** Utilisation de `BigDecimal` et non `double` pour les montants : `BigDecimal` garantit une précision décimale exacte, indispensable pour la manipulation d'argent.

#### Test 1.2 — `creationSoldeZero`

```java
@Test
@DisplayName("Un compte peut être créé avec un solde initial de zéro")
void creationSoldeZero() {
 CompteBancaire compte = new CompteBancaire(2L, "Bob", BigDecimal.ZERO);
 assertEquals(BigDecimal.ZERO, compte.getSolde());
}
```

**Cas limite :** le solde zéro est valide. Ce test garantit que la validation `soldeInitial >= 0` utilise `>=` et non `>`.

#### Test 1.3 — `soldeInitialNegatifInterdit`

```java
@Test
@DisplayName("Le solde initial ne peut pas être négatif")
void soldeInitialNegatifInterdit() {
 assertThrows(IllegalArgumentException.class,
 () -> new CompteBancaire(3L, "Charlie", new BigDecimal("-100.00")));
}
```

**Cas d'erreur :** le constructeur refuse un solde négatif. Ce test force l'ajout de la validation dans le constructeur :

```java
if (soldeInitial.compareTo(BigDecimal.ZERO) < 0) {
 throw new IllegalArgumentException("Le solde initial ne peut pas être négatif");
}
```

---

### Groupe 2 : "Opérations de dépôt" (4 tests)

```java
@Nested
@DisplayName("Opérations de dépôt")
class Depot {
```

**Intention pédagogique :** valider la méthode `deposer()` — cas nominal, cas limites (zéro, négatif), et dépôts multiples.

#### Test 2.1 — `depotAugmenteSolde`

```java
@Test
@DisplayName("Un dépôt augmente le solde")
void depotAugmenteSolde() {
 CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("500.00"));
 compte.deposer(new BigDecimal("150.00"), "Salaire");
 assertEquals(new BigDecimal("650.00"), compte.getSolde());
}
```

**Cas nominal** : 500 + 150 = 650. Vérifie que `deposer()` ajoute bien le montant au solde.

#### Test 2.2 — `depotZeroInterdit`

```java
@Test
@DisplayName("Dépôt de zéro est interdit")
void depotZeroInterdit() {
 CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
 assertThrows(IllegalArgumentException.class,
 () -> compte.deposer(BigDecimal.ZERO, "Dépôt nul"));
}
```

**Cas d'erreur** : un dépôt nul n'a pas de sens métier. La validation dans le code source utilise `compareTo(BigDecimal.ZERO) <= 0`, donc zéro ET négatif sont rejetés par la même condition.

#### Test 2.3 — `depotNegatifInterdit`

```java
@Test
@DisplayName("Dépôt négatif est interdit")
void depotNegatifInterdit() {
 CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
 assertThrows(IllegalArgumentException.class,
 () -> compte.deposer(new BigDecimal("-50.00"), "Dépôt négatif"));
}
```

**Cas d'erreur** : cohérence métier — on ne peut pas déposer un montant négatif (ce serait un retrait déguisé).

#### Test 2.4 — `plusieursDepots`

```java
@Test
@DisplayName("Plusieurs dépôts successifs")
void plusieursDepots() {
 CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
 compte.deposer(new BigDecimal("50.00"), "Dépôt 1");
 compte.deposer(new BigDecimal("75.00"), "Dépôt 2");
 compte.deposer(new BigDecimal("25.00"), "Dépôt 3");
 assertEquals(new BigDecimal("250.00"), compte.getSolde());
}
```

**Cas d'accumulation** : 100 + 50 + 75 + 25 = 250. Ce test garantit que l'état du compte persiste entre les appels et que les dépôts successifs s'additionnent correctement.

---

### Groupe 3 : "Opérations de retrait" (5 tests)

```java
@Nested
@DisplayName("Opérations de retrait")
class Retrait {
```

**Intention pédagogique :** valider `retirer()` avec les mêmes patterns que `deposer()`, plus le cas critique du solde insuffisant.

#### Test 3.1 — `retraitDiminueSolde`

```java
@Test
@DisplayName("Un retrait diminue le solde")
void retraitDiminueSolde() {
 CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("500.00"));
 compte.retirer(new BigDecimal("200.00"), "Retrait DAB");
 assertEquals(new BigDecimal("300.00"), compte.getSolde());
}
```

#### Test 3.2 — `retraitTotal`

```java
@Test
@DisplayName("Retrait de la totalité du solde")
void retraitTotal() {
 CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
 compte.retirer(new BigDecimal("100.00"), "Retrait total");
 assertEquals(BigDecimal.ZERO, compte.getSolde());
}
```

**Cas limite** : retirer exactement le solde est autorisé. Le solde atteint 0.00.

#### Test 3.3 — `retraitSuperieurAuSolde`

```java
@Test
@DisplayName("Retrait supérieur au solde est interdit")
void retraitSuperieurAuSolde() {
 CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
 assertThrows(IllegalArgumentException.class,
 () -> compte.retirer(new BigDecimal("200.00"), "Trop"));
}
```

**Cas d'erreur critique** : sans cette validation, le compte pourrait passer en négatif. Le code source vérifie `montant.compareTo(this.solde) > 0` avant de soustraire.

#### Tests 3.4 et 3.5 — Zéro et négatif

Identiques aux tests de dépôt : `retirer(BigDecimal.ZERO)` et `retirer(new BigDecimal("-50.00"))` lèvent `IllegalArgumentException`.

---

### Groupe 4 : "Historique des transactions" (5 tests)

```java
@Nested
@DisplayName("Historique des transactions")
class Historique {
```

**Intention pédagogique :** valider le système d'historique — chaque opération crée une transaction enregistrée dans une liste, avec le bon type, le bon montant, et le solde après opération.

#### Test 4.1 — `compteNeufHistoriqueVide`

```java
@Test
@DisplayName("Le compte neuf a un historique vide")
void compteNeufHistoriqueVide() {
 CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
 assertTrue(compte.getHistorique().isEmpty());
 assertEquals(0, compte.getNombreTransactions());
}
```

**État initial** : à la création, l'historique est vide (0 transaction).

#### Test 4.2 — `depotCreeTransaction`

```java
@Test
@DisplayName("Un dépôt crée une transaction dans l'historique")
void depotCreeTransaction() {
 CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
 compte.deposer(new BigDecimal("50.00"), "Dépôt test");

 assertEquals(1, compte.getNombreTransactions());
 Transaction derniere = compte.getDerniereTransaction();
 assertNotNull(derniere);
 assertEquals(Transaction.Type.DEPOT, derniere.getType());
 assertEquals(new BigDecimal("50.00"), derniere.getMontant());
 assertEquals(new BigDecimal("150.00"), derniere.getSoldeApresOperation());
}
```

Ce test valide que :
- Le nombre de transactions passe à 1
- La transaction a le type `DEPOT`
- Le montant de la transaction est 50.00€
- Le solde **après** opération (150.00€) est correctement enregistré — c'est crucial pour l'audit : on sait quel était le solde après chaque opération

#### Test 4.3 — `retraitCreeTransaction`

```java
@Test
@DisplayName("Un retrait crée une transaction dans l'historique")
void retraitCreeTransaction() {
 CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
 compte.retirer(new BigDecimal("30.00"), "Retrait test");

 Transaction derniere = compte.getDerniereTransaction();
 assertEquals(Transaction.Type.RETRAIT, derniere.getType());
 assertEquals(new BigDecimal("30.00"), derniere.getMontant());
 assertEquals(new BigDecimal("70.00"), derniere.getSoldeApresOperation());
}
```

Même validation pour les retraits : type `RETRAIT`, solde après opération 70.00€.

#### Test 4.4 — `plusieursOperationsHistorique`

```java
@Test
@DisplayName("Plusieurs opérations créent des transactions ordonnées")
void plusieursOperationsHistorique() {
 CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("0.00"));
 compte.deposer(new BigDecimal("1000.00"), "Salaire");
 compte.retirer(new BigDecimal("200.00"), "Loyer");
 compte.deposer(new BigDecimal("50.00"), "Remboursement");

 List<Transaction> historique = compte.getHistorique();
 assertEquals(3, historique.size());

 assertEquals(Transaction.Type.DEPOT, historique.get(0).getType());
 assertEquals(Transaction.Type.RETRAIT, historique.get(1).getType());
 assertEquals(Transaction.Type.DEPOT, historique.get(2).getType());

 assertEquals(new BigDecimal("1000.00"), historique.get(0).getSoldeApresOperation());
 assertEquals(new BigDecimal("800.00"), historique.get(1).getSoldeApresOperation());
 assertEquals(new BigDecimal("850.00"), historique.get(2).getSoldeApresOperation());
}
```

Ce test vérifie :
- L'**ordre chronologique** : les transactions sont enregistrées dans l'ordre d'exécution
- Les **types** : DEPOT, RETRAIT, DEPOT
- Le **solde progressif** : après chaque opération, le solde reflète l'accumulation exacte (0 → 1000 → 800 → 850)

#### Test 4.5 — `historiqueImmuable`

```java
@Test
@DisplayName("L'historique est immuable (ne peut pas être modifié de l'extérieur)")
void historiqueImmuable() {
 CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
 compte.deposer(new BigDecimal("50.00"), "Test");

 List<Transaction> historique = compte.getHistorique();
 assertThrows(UnsupportedOperationException.class,
 () -> historique.add(null),
 "La liste retournée doit être non modifiable");
}
```

**Test d'encapsulation** : `getHistorique()` retourne une liste **non modifiable** grâce à `Collections.unmodifiableList()`. Une tentative de modification externe lève `UnsupportedOperationException`. Cela protège l'intégrité de l'historique.

#### Test 4.6 — `derniereTransactionNullSiVide`

```java
@Test
@DisplayName("getDerniereTransaction retourne null si historique vide")
void derniereTransactionNullSiVide() {
 CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
 assertNull(compte.getDerniereTransaction());
}
```

**Cas limite** : `getDerniereTransaction()` sur un historique vide retourne `null` (pas d'exception). Le code source gère cela : `if (historique.isEmpty()) return null;`.

---

### Groupe 5 : "Virements internes au compte" (5 tests)

```java
@Nested
@DisplayName("Virements internes au compte")
class OperationsVirement {
```

**Intention pédagogique :** valider les méthodes `emettreVirement()` et `recevoirVirement()` qui seront utilisées par `ServiceVirement`.

#### Test 5.1 — `emissionVirementDebite`

```java
@Test
@DisplayName("Émission d'un virement débite le compte")
void emissionVirementDebite() {
 CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("1000.00"));
 compte.emettreVirement(new BigDecimal("300.00"), "Paiement facture");
 assertEquals(new BigDecimal("700.00"), compte.getSolde());
}
```

#### Test 5.2 — `receptionVirementCredite`

```java
@Test
@DisplayName("Réception d'un virement crédite le compte")
void receptionVirementCredite() {
 CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("500.00"));
 compte.recevoirVirement(new BigDecimal("200.00"), "Remboursement");
 assertEquals(new BigDecimal("700.00"), compte.getSolde());
}
```

**Différence avec `deposer` :** `recevoirVirement` ne vérifie pas si le montant est positif — c'est la responsabilité de `ServiceVirement`. `recevoirVirement` est une méthode interne utilisée uniquement par le service.

#### Tests 5.3 et 5.4 — Types de transaction

```java
@Test
@DisplayName("Le virement émis crée une transaction de type VIREMENT_EMIS")
void transactionVirementEmis() {
 CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("500.00"));
 compte.emettreVirement(new BigDecimal("100.00"), "Test virement");
 assertEquals(Transaction.Type.VIREMENT_EMIS,
 compte.getDerniereTransaction().getType());
}
```

Chaque type de virement crée une transaction avec le bon type (`VIREMENT_EMIS` ou `VIREMENT_RECU`), ce qui permet de distinguer les flux sortants des flux entrants dans l'historique.

#### Test 5.5 — `virementEmisSuperieurSolde`

```java
@Test
@DisplayName("Virement émis supérieur au solde interdit")
void virementEmisSuperieurSolde() {
 CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
 assertThrows(IllegalArgumentException.class,
 () -> compte.emettreVirement(new BigDecimal("200.00"), "Trop"));
}
```

**Cas d'erreur** : idem retrait, le solde doit être suffisant. Cette validation est dans `emettreVirement`, pas dans `ServiceVirement` — le modèle protège ses propres invariants.

---

### Groupe 6 : "Concurrence et thread-safety" (2 tests)

```java
@Nested
@DisplayName("Concurrence et thread-safety")
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
@Test
@DisplayName("Dépôts concurrents : le solde final est correct")
@Timeout(value = 5, unit = TimeUnit.SECONDS)
void depotsConcurrents() throws InterruptedException {
 CompteBancaire compte = new CompteBancaire(1L, "Alice", BigDecimal.ZERO);
 int nbThreads = 10;
 int nbOperations = 100;
 BigDecimal montant = BigDecimal.ONE;

 ExecutorService executor = Executors.newFixedThreadPool(nbThreads);
 CountDownLatch latch = new CountDownLatch(nbThreads);

 for (int i = 0; i < nbThreads; i++) {
 executor.submit(() -> {
 try {
 for (int j = 0; j < nbOperations; j++) {
 compte.deposer(montant, "Dépôt concurrent");
 }
 } finally {
 latch.countDown();
 }
 });
 }

 latch.await();
 executor.shutdown();
 executor.awaitTermination(5, TimeUnit.SECONDS);

 BigDecimal attendu = new BigDecimal(nbThreads * nbOperations);
 assertEquals(attendu, compte.getSolde(),
 "Avec " + nbThreads + " threads × " + nbOperations + " dépôts de 1€, le solde doit être " + attendu + "€");
 assertEquals(nbThreads * nbOperations, compte.getNombreTransactions());
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
@Test
@DisplayName("Dépôts et retraits concurrents : intégrité du solde")
@Timeout(value = 5, unit = TimeUnit.SECONDS)
void operationsMixtesConcurrentes() throws InterruptedException {
 CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("1000.00"));
 int nbThreads = 5;
 BigDecimal montant = new BigDecimal("1.00");

 ExecutorService executor = Executors.newFixedThreadPool(nbThreads);
 CountDownLatch latch = new CountDownLatch(nbThreads);

 for (int i = 0; i < nbThreads / 2 + 1; i++) {
 executor.submit(() -> {
 try {
 for (int j = 0; j < 50; j++) {
 compte.deposer(montant, "Dépôt");
 }
 } finally {
 latch.countDown();
 }
 });
 }
 for (int i = nbThreads / 2 + 1; i < nbThreads; i++) {
 executor.submit(() -> {
 try {
 for (int j = 0; j < 50; j++) {
 compte.retirer(montant, "Retrait");
 }
 } finally {
 latch.countDown();
 }
 });
 }

 latch.await();
 executor.shutdown();
 executor.awaitTermination(5, TimeUnit.SECONDS);

 int nbDepots = 3 * 50; // 3 threads de dépôt × 50 opérations = 150
 int nbRetraits = 2 * 50; // 2 threads de retrait × 50 opérations = 100

 BigDecimal attendu = new BigDecimal("1000.00")
 .add(new BigDecimal(nbDepots))
 .subtract(new BigDecimal(nbRetraits));

 assertEquals(attendu, compte.getSolde());
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
@RepeatedTest(value = 10, name = "{displayName} — répétition {currentRepetition}/{totalRepetitions}")
@DisplayName("Stabilité : le solde après 3 opérations est toujours correct")
void stabiliteSoldeApresOperations() {
 CompteBancaire compte = new CompteBancaire(1L, "Test", new BigDecimal("100.00"));
 compte.deposer(new BigDecimal("50.00"), "Depot");
 compte.retirer(new BigDecimal("30.00"), "Retrait");
 compte.deposer(new BigDecimal("20.00"), "Depot 2");
 assertEquals(new BigDecimal("140.00"), compte.getSolde(),
 "Après 100+50-30+20 le solde doit toujours être 140.00");
}
```

Ce test est exécuté **10 fois**. Chaque exécution crée un nouveau compte, effectue trois opérations, et vérifie que 100 + 50 - 30 + 20 = 140 exactement. Ce test n'est pas concurrentiel, mais il vérifie qu'il n'y a pas d'état statique résiduel entre les exécutions (fuite mémoire, variable `static` mal nettoyée, etc.).

Le `name` personnalisé permet d'identifier chaque répétition dans le rapport de test.

---

## Fichier 2 : `ServiceVirementTest.java` — Tests du service

```java
@DisplayName("TDD : Service de Virement")
class ServiceVirementTest {

 private ServiceVirement service;
 private CompteBancaire compteAlice;
 private CompteBancaire compteBob;

 @BeforeEach
 void setUp() {
 service = new ServiceVirement();
 compteAlice = new CompteBancaire(1L, "Alice", new BigDecimal("1000.00"));
 compteBob = new CompteBancaire(2L, "Bob", new BigDecimal("500.00"));
 }
```

**`@BeforeEach`** : avant chaque test, on crée un service et deux comptes (Alice: 1000€, Bob: 500€). Chaque test démarre avec un état propre.

### Groupe 1 : "Virement standard" (3 tests)

#### Test 1.1 — `virementDebiteSourceCrediteDestination`

```java
@Test
@DisplayName("Le virement débite la source et crédite la destination")
void virementDebiteSourceCrediteDestination() {
 service.effectuerVirement(compteAlice, compteBob,
 new BigDecimal("200.00"), "Remboursement");

 assertEquals(new BigDecimal("800.00"), compteAlice.getSolde(),
 "Alice doit être débitée de 200€");
 assertEquals(new BigDecimal("700.00"), compteBob.getSolde(),
 "Bob doit être crédité de 200€");
}
```

**Cas nominal** : Alice (1000€) vire 200€ à Bob (500€). Après virement, Alice a 800€, Bob a 700€. Les deux assertions sont nécessaires : vérifier un seul compte ne suffit pas.

#### Test 1.2 — `virementCreeTransactions`

```java
@Test
@DisplayName("Le virement crée une transaction chez l'émetteur et le bénéficiaire")
void virementCreeTransactions() {
 service.effectuerVirement(compteAlice, compteBob,
 new BigDecimal("100.00"), "Cadeau");

 assertEquals(1, compteAlice.getNombreTransactions());
 assertEquals(1, compteBob.getNombreTransactions());
 assertEquals(
 com.nexa.banque.model.Transaction.Type.VIREMENT_EMIS,
 compteAlice.getDerniereTransaction().getType());
 assertEquals(
 com.nexa.banque.model.Transaction.Type.VIREMENT_RECU,
 compteBob.getDerniereTransaction().getType());
}
```

Ce test valide la **traçabilité** : chaque virement crée une transaction chez l'émetteur (type `VIREMENT_EMIS`) et une chez le bénéficiaire (type `VIREMENT_RECU`). Les deux comptes ont exactement 1 transaction (pas plus).

#### Test 1.3 — `sommeSoldesConservee`

```java
@Test
@DisplayName("Somme des soldes conservée après virement")
void sommeSoldesConservee() {
 BigDecimal sommeAvant = compteAlice.getSolde().add(compteBob.getSolde());

 service.effectuerVirement(compteAlice, compteBob,
 new BigDecimal("150.00"), "Test conservation");

 BigDecimal sommeApres = compteAlice.getSolde().add(compteBob.getSolde());
 assertEquals(sommeAvant, sommeApres,
 "La somme totale des soldes doit être conservée");
}
```

**Invariant fondamental** : un virement ne crée ni ne détruit d'argent. La somme des soldes avant (1000 + 500 = 1500€) doit être égale à la somme après (850 + 650 = 1500€). Ce test garantit qu'aucun centime n'est perdu ou créé pendant le transfert.

### Groupe 2 : "Cas d'erreur" (4 tests)

```java
@Nested
@DisplayName("Cas d'erreur")
class CasErreur {
```

#### Test 2.1 — `virementMemeCompteInterdit`

```java
@Test
@DisplayName("Virement vers le même compte interdit")
void virementMemeCompteInterdit() {
 assertThrows(IllegalArgumentException.class,
 () -> service.effectuerVirement(compteAlice, compteAlice,
 new BigDecimal("100.00"), "Moi-même"));
}
```

Le code source vérifie `source.equals(destination)` avant toute opération.

#### Test 2.2 — `montantNulInterdit`

```java
@Test
@DisplayName("Montant nul interdit")
void montantNulInterdit() {
 assertThrows(IllegalArgumentException.class,
 () -> service.effectuerVirement(compteAlice, compteBob,
 BigDecimal.ZERO, "Zéro"));
}
```

#### Test 2.3 — `montantNegatifInterdit`

```java
@Test
@DisplayName("Montant négatif interdit")
void montantNegatifInterdit() {
 assertThrows(IllegalArgumentException.class,
 () -> service.effectuerVirement(compteAlice, compteBob,
 new BigDecimal("-50.00"), "Négatif"));
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

4. **`retraitDansDecouvert`** : solde 100€, découvert 500€, retrait de 400€ → solde devient -300€ (dans la limite)
5. **`retraitDepasseDecouvert`** : solde 100€, découvert 500€, retrait de 700€ → exception (100 - 700 = -600 < -500)

**Phase GREEN** — Modifier `retirer()` :

```java
public synchronized void retirer(BigDecimal montant, String description) {
 if (montant.compareTo(BigDecimal.ZERO) <= 0) {
 throw new IllegalArgumentException("Le montant du retrait doit être strictement positif");
 }
 BigDecimal nouveauSolde = this.solde.subtract(montant);
 if (nouveauSolde.compareTo(this.decouvertAutorise.negate()) < 0) {
 throw new IllegalArgumentException(
 String.format("Dépassement du découvert autorisé : solde deviendrait %s€, découvert max %s€",
 nouveauSolde, this.decouvertAutorise.negate()));
 }
 this.solde = nouveauSolde;
 historique.add(new Transaction(transactionIdGenerator.getAndIncrement(),
 Transaction.Type.RETRAIT, montant, this.solde, description));
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
