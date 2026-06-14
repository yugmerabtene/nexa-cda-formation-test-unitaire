# Lab 02 — Tests Paramétrés Avancés

## Objectif

Maîtriser les tests paramétrés JUnit 5 : `@ValueSource`, `@CsvSource`, `@CsvFileSource`, `@EnumSource`, `@MethodSource`, `@NullSource`, `@EmptySource`, `@NullAndEmptySource`.

## Contexte

On teste un `ValidateurUtilisateur` qui valide emails, téléphones, mots de passe et âges. Pour chaque type de validation, il y a des dizaines de cas à tester (valides, invalides, cas limites). Les tests paramétrés évitent la duplication de code.

## Structure

```
lab02-parametres-avances/
├── pom.xml
├── src/main/java/com/nexa/parametres/
│   └── ValidateurUtilisateur.java
├── src/test/java/com/nexa/parametres/
│   └── ValidateurUtilisateurTest.java
└── src/test/resources/
    └── telephones-test.csv
```

## Annotations étudiées

| Annotation | Rôle |
|---|---|
| `@ParameterizedTest` | Remplace `@Test` : test paramétré |
| `@ValueSource` | Injecte des valeurs simples (ints, strings...) |
| `@CsvSource` | Injecte des lignes CSV multi-colonnes |
| `@CsvFileSource` | Injecte depuis un fichier CSV externe |
| `@EnumSource` | Injecte les valeurs d'un enum |
| `@MethodSource` | Injecte depuis une méthode factory |
| `@NullSource` | Injecte `null` |
| `@EmptySource` | Injecte chaîne vide / collection vide |
| `@NullAndEmptySource` | Combine `@NullSource` + `@EmptySource` |

## Exercices supplémentaires

1. Ajouter `estCodePostalValide(String)` avec 50 codes postaux dans un CSV
2. Créer un générateur de données via `@MethodSource` avec `IntStream.range()`
3. Implémenter un `ArgumentConverter` personnalisé pour un type métier
