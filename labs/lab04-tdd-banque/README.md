# Lab04 - TDD Banque

## Objectif

Apprendre et mettre en pratique le Test-Driven Development (TDD) en construisant une application bancaire simple en Java. L'objectif est de developper un système de comptes bancaires avec gestion des depots, retraits et virements, en garantissant la sécurité des threads (thread-safety) et la robustesse des calculs monetaires via `BigDecimal`.

## Énoncé

Developper une application bancaire composee de :

- Un compte bancaire (`CompteBancaire`) capable d'enregistrer des depots, des retraits et des virements, avec un historique des transactions.
- Un enregistrement de transaction (`Transaction`) decrivant chaque opération (type, montant, solde après opération, description, date/heure).
- Un service de virement (`ServiceVirement`) orchestrant les transferts entre deux comptes avec validation des entrees.
- Une exception metier (`SoldeInsuffisantException`) signalant les tentatives d'opération lorsque le solde est insuffisant.

L'application doit gérer la concurrence (accès multi-threads sans corruption du solde), utilisé́r `BigDecimal` pour les montants monetaires, et fournir un historique de transactions immuable.

## Prérequis

- Java 17 ou superieur
- Maven 3.8+ ou Gradle (le projet utilisé Maven)
- JUnit Jupiter 5.x (JUnit 5)
- Connaissance de base du TDD : Red (écrire le test, il échoué) / Green (écrire le code minimal, il passe) / Refactor (ameliorer sans casser)

## Etapes

### Étape 1 : Creer la classe Transaction

Ecrire d'abord le test unitaire pour la classe `Transaction`, qui decrit une opération bancaire. Verifier que le constructeur initialise correctement l'ID, le type, le montant, le soldeApresOperation, la description et la dateHeure. Implementer la classe pour que le test passe.

### Étape 2 : Creer la classe CompteBancaire (creation et solde initial)

Rediger les tests de creation du compte (`CompteBancaireTest`) :
- Un compte est créé avec un ID, un titulaire et un solde initial correct.
- Le solde initial peut etre zero.
- Un solde initial negatif doit lever une `IllegalArgumentException`.
Implementer le constructeur de `CompteBancaire` pour satisfaire ces tests.

### Étape 3 : Ajouter les operations de dépôt

Ecrire les tests pour la méthode `deposer` :
- Un dépôt augmente le solde.
- Un dépôt de zero ou negatif est interdit.
- Plusieurs depots successifs modifient le solde correctement.
Implementer `deposer` avec synchronisation et enregistrement de la transaction.

### Étape 4 : Ajouter les operations de retrait

Ecrire les tests pour la méthode `retirer` :
- Un retrait diminue le solde.
- Un retrait peut vider le compte (solde final zero).
- Un retrait superieur au solde est interdit.
- Un retrait de zero ou negatif est interdit.
Implementer `retirer` avec synchronisation et enregistrement de la transaction.

### Étape 5 : Gerer l'historique des transactions

Ecrire les tests pour l'historique :
- Le compte neuf a un historique vide.
- Chaque dépôt et retrait créé une transaction dans l'historique.
- Plusieurs operations produisent un historique ordonne.
- L'historique retourne par `getHistorique()` est immuable.
- `getDerniereTransaction()` retourne `null` si l'historique est vide.
Implementer `getHistorique()`, `getNombreTransactions()` et `getDerniereTransaction()`.

### Étape 6 : Ajouter les virements internes

Ecrire les tests pour `emettreVirement` et `recevoirVirement` :
- L'emission debite le compte.
- La reception credite le compte.
- Chaque opération créé une transaction du type approprie (`VIREMENT_EMIS` / `VIREMENT_RECU`).
- Un virement emis superieur au solde est interdit.
Implementer les deux méthodes avec synchronisation.

### Étape 7 : Creer le ServiceVirement

Ecrire les tests pour `ServiceVirementTest` :
- Le virement debite la source et credite la destination.
- Une transaction est créée sur chaque compte.
- La somme totale des soldes est conservee après virement.
- Le virement vers le même compte est interdit.
- Le montant nul ou negatif est interdit.
- Le solde insuffisant chez l'emetteur lève une exception.
Implementer `ServiceVirement.effectuerVirement()`.

### Étape 8 : Tests de concurrence

Ecrire les tests multi-threads (`@Timeout`, `CountDownLatch`, `ExecutorService`) :
- Des depots concurrents produisent un solde final correct et un nombre de transactions egal au nombre d'operations.
- Des depots et retraits concurrents preservent l'integrite du solde.
Verifier que la synchronisation au niveau des méthodes de `CompteBancaire` garantit la coherence.

### Étape 9 : Test de stabilite avec @RepeatedTest

Ajouter un test repete (`@RepeatedTest`) qui exécuté une sequence d'operations (dépôt, retrait, dépôt) et vérifié que le solde final est toujours identique après chaque repetition, demontrant la fiabilite deterministe du code.

### Étape 10 : Creer l'exception SoldeInsuffisantException

Bien que le code actuel utilisé `IllegalArgumentException`, reflechir a la creation d'une exception metier dediee `SoldeInsuffisantException` pour distinguer les erreurs de solde des autres erreurs de validation. L'exception existe déjà dans le projet comme `RuntimeException`.

## Commande d'exécution

```bash
# Compiler et exécuter les tests
mvn test

# Executer uniquement les tests du lab04
mvn test -pl labs/lab04-tdd-banque

# Executer un test spécifique
mvn test -pl labs/lab04-tdd-banque -Dtest=CompteBancaireTest

# Generer le rapport de couverture (si JaCoCo est configure)
mvn test jacoco:report
```

## Criteres de réussite

1. Tous les tests unitaires passent sans erreur ni échec.
2. Les montants sont manipules exclusivement avec `BigDecimal` (pas de `double` ou `float`).
3. Toutes les méthodes publiques de `CompteBancaire` sont synchronisees (`synchronized`) pour garantir la thread-safety.
4. L'historique des transactions retourne par `getHistorique()` est une liste non modifiable.
5. Aucune transaction n'est créée si une opération échoué (validation avant modification).
6. Les tests de concurrence passent en moins de 5 secondes (`@Timeout`).
7. Le test repete (`@RepeatedTest`) passe 10 fois sur 10 sans defaillance.
8. Les virements conservent la masse monetaire totale (somme des soldes source + destination inchangee).
9. Les identifiants de transaction sont uniques et auto-incrementes via `AtomicLong`.
10. Le code est structure avec `@Nested` dans les classes de test pour une organisation claire par fonctionnalite.
