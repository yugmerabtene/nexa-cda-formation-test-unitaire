# Lab 01 — Fondamentaux JUnit 5

## Objectif

Écrire vos premiers tests unitaires Java avec JUnit 5. Comprendre la syntaxe des assertions, le cycle de vie des tests, et les conventions de nommage.

## Prérequis

- Java 17 installé
- Docker et Docker Compose

## Exécution

```bash
# Lancer tous les tests du lab
cd /chemin/vers/nexa-cda-test-unitaire
bash scripts/run-lab.sh jour1-tests-unitaires/lab01-fondamentaux clean test

# Avec couverture de code
bash scripts/run-lab.sh jour1-tests-unitaires/lab01-fondamentaux clean test jacoco:report
```

## Structure

```
lab01-fondamentaux/
├── pom.xml
├── src/main/java/com/nexa/fondamentaux/
│   └── Calculatrice.java          ← Code à tester
└── src/test/java/com/nexa/fondamentaux/
    ├── CalculatriceTest.java      ← Tests des opérations
    └── CycleDeVieTest.java        ← Démo du cycle de vie
```

## Annotations étudiées

| Annotation | Rôle |
|---|---|
| `@Test` | Marque une méthode comme test unitaire |
| `@DisplayName` | Nom lisible pour le rapport de test |
| `@BeforeAll` | Exécuté UNE FOIS avant tous les tests (static) |
| `@BeforeEach` | Exécuté avant CHAQUE test |
| `@AfterEach` | Exécuté après CHAQUE test |
| `@AfterAll` | Exécuté UNE FOIS après tous les tests (static) |

## Assertions étudiées

| Assertion | Usage |
|---|---|
| `assertEquals(expected, actual)` | Égalité par `equals()` |
| `assertTrue(condition)` | Condition vraie |
| `assertFalse(condition)` | Condition fausse |
| `assertNull(value)` | Référence nulle |
| `assertNotNull(value)` | Référence non nulle |
| `assertThrows(Class, lambda)` | Exception attendue |
| `assertAll(lambda1, lambda2, ...)` | Assertions groupées |
| `assertArrayEquals(a1, a2)` | Tableaux égaux élément par élément |

## Points clés

1. **AAA Pattern** : Arrange (préparer), Act (exécuter), Assert (vérifier)
2. **Isolation** : JUnit crée une nouvelle instance de la classe de test pour chaque `@Test`
3. **Messages d'assertion** : 3ᵉ argument optionnel, affiché seulement si le test échoue
4. **`assertAll`** : toutes les assertions sont exécutées, même si certaines échouent

## Exercices supplémentaires

1. Ajouter une méthode `puissance(int base, int exposant)` et ses tests
2. Ajouter `factorielle(int n)` avec tests (attention à factorielle(0) = 1)
3. Tester la performance avec `@Timeout`
