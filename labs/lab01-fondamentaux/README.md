# Lab 01 : Fondamentaux des tests unitaires avec JUnit 5

**Objectif :** Decouvrir JUnit 5, ecrire vos premiers tests unitaires, et comprendre le pattern AAA (Arrange, Act, Assert).

**Duree :** 45 minutes

---

## Enonce

Vous devez implementer une classe `Calculatrice` et ecrire les tests unitaires correspondants.

### Fonctionnalites a implementer

La classe `Calculatrice` doit fournir :

| Methode | Signature | Comportement |
|---|---|---|
| `addition` | `int addition(int a, int b)` | Retourne `a + b` |
| `soustraction` | `int soustraction(int a, int b)` | Retourne `a - b` |
| `multiplication` | `int multiplication(int a, int b)` | Retourne `a * b` |
| `division` | `int division(int a, int b)` | Retourne `a / b`. Si `b == 0`, leve `ArithmeticException` |
| `modulo` | `int modulo(int a, int b)` | Retourne `a % b`. Si `b == 0`, leve `ArithmeticException` |
| `estPair` | `boolean estPair(int nombre)` | Retourne `true` si le nombre est pair, `false` sinon |
| `valeurAbsolue` | `int valeurAbsolue(int nombre)` | Retourne la valeur absolue du nombre |

### Tests a ecrire

Vous devez ecrire **au minimum 20 tests** repartis comme suit :

1. **Addition** (3 tests) : positifs, negatifs, avec zero
2. **Soustraction** (2 tests) : simple, resultat negatif
3. **Multiplication** (3 tests) : simple, par zero, par un
4. **Division** (3 tests) : simple, avec reste, par zero (exception)
5. **Modulo** (2 tests) : simple, par zero (exception)
6. **Parite** (2 tests) : pair, impair
7. **Valeur absolue** (3 tests) : positif, negatif, zero
8. **assertAll** (1 test) : test groupe de 6 assertions
9. **assertArrayEquals** (1 test) : comparaison de tableaux
10. **Messages personnalises** (1 test) : avec `String` et `Supplier<String>`
11. **Cycle de vie** : ecrire `CycleDeVieTest.java` pour demontrer `@BeforeAll`, `@BeforeEach`, `@AfterEach`, `@AfterAll`

---

## PreRequis

- Java 17 installe (ou Docker pour executer les tests)
- Maven 3.9+ (ou Docker)

---

## Etape par etape

### Etape 1 : Comprendre la structure du projet

```
lab01-fondamentaux/
  pom.xml
  src/
    main/java/com/nexa/fondamentaux/
      Calculatrice.java        <- Code a implementer
    test/java/com/nexa/fondamentaux/
      CalculatriceTest.java    <- Tests de la calculatrice
      CycleDeVieTest.java      <- Demonstration du cycle de vie JUnit
```

### Etape 2 : Le fichier pom.xml

Le `pom.xml` configure JUnit Jupiter 5.10.2 comme dependance de test.
Le plugin `maven-surefire-plugin` est configure pour executer tous les tests lors de la phase `mvn test`.

### Etape 3 : Implementer la classe Calculatrice

Creez le fichier `src/main/java/com/nexa/fondamentaux/Calculatrice.java` avec les 7 methodes decrites dans l'enonce.

Points d'attention :
- La methode `division` doit verifier `b == 0` et lever `ArithmeticException`
- La methode `modulo` doit faire la meme verification
- La methode `estPair` utilise le modulo : `nombre % 2 == 0`
- La methode `valeurAbsolue` peut utiliser `Math.abs()`

### Etape 4 : Ecrire les tests

Dans `CalculatriceTest.java`, ecrivez les tests en respectant le pattern **AAA** :

```java
@Test
@DisplayName("Addition de deux nombres positifs")
void additionDeuxPositifs() {
    // ARRANGE : preparer les donnees d'entree et l'objet teste
    Calculatrice calc = new Calculatrice();

    // ACT : appeler la methode a tester
    int resultat = calc.addition(2, 3);

    // ASSERT : verifier que le resultat est conforme a l'attendu
    assertEquals(5, resultat, "2 + 3 devrait donner 5");
}
```

### Etape 5 : Tester la division par zero

Pour tester une exception, utilisez `assertThrows` :

```java
@Test
@DisplayName("Division par zero doit lever ArithmeticException")
void divisionParZero() {
    ArithmeticException exception = assertThrows(
        ArithmeticException.class,
        () -> calc.division(10, 0)
    );
    assertTrue(exception.getMessage().contains("Division"));
}
```

### Etape 6 : Utiliser assertAll pour grouper les tests

```java
@Test
void testGroupe() {
    assertAll("Verifications groupees",
        () -> assertEquals(5, calc.addition(2, 3), "addition"),
        () -> assertEquals(6, calc.multiplication(2, 3), "multiplication"),
        () -> assertTrue(calc.estPair(10), "parite")
    );
}
```

Toutes les assertions sont executees, meme si certaines echouent.

### Etape 7 : Le cycle de vie avec CycleDeVieTest

Creez `CycleDeVieTest.java` pour observer l'ordre d'execution des callbacks :

| Annotation | Quand | Frequence |
|---|---|---|
| `@BeforeAll` | Avant le tout premier test | 1 fois (static) |
| `@BeforeEach` | Avant chaque test | N fois |
| `@AfterEach` | Apres chaque test | N fois |
| `@AfterAll` | Apres le tout dernier test | 1 fois (static) |

Chaque test est execute sur une **nouvelle instance** de la classe — cela garantit l'isolation.

---

## Execution

```bash
# Depuis la racine du projet
cd labs/lab01-fondamentaux
mvn clean test

# Ou via Docker
bash ../../scripts/run-lab.sh labs/lab01-fondamentaux clean test
```

## Verification

- Tous les tests passent : `BUILD SUCCESS`
- Au moins 20 tests sont executes
- Le rapport JaCoCo est genere dans `target/site/jacoco/index.html`

## Criteres de reussite

- 20 tests ou plus repartis sur les categories de l'enonce
- Tous les tests passent (`mvn test` reussit)
- La couverture de code est superieure a 90%
- La classe `CycleDeVieTest.java` demontre le cycle de vie avec `System.out.println()`
- Les messages d'assertion sont personnalises
