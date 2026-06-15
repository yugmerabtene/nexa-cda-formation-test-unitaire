# Lab04 - TDD Banque

## Objectif

Apprendre et mettre en pratique le Test-Driven Development (TDD) en construisant une application bancaire simple en Java. L'objectif est de developper un systeme de comptes bancaires avec gestion des depots, retraits et virements, en garantissant la securite des threads (thread-safety) et la robustesse des calculs monetaires via `BigDecimal`.

## Enonce

Developper une application bancaire composee de :

- Un compte bancaire (`CompteBancaire`) capable d'enregistrer des depots, des retraits et des virements, avec un historique des transactions.
- Un enregistrement de transaction (`Transaction`) decrivant chaque operation (type, montant, solde apres operation, description, date/heure).
- Un service de virement (`ServiceVirement`) orchestrant les transferts entre deux comptes avec validation des entrees.
- Une exception metier (`SoldeInsuffisantException`) signalant les tentatives d'operation lorsque le solde est insuffisant.

L'application doit gerer la concurrence (acces multi-threads sans corruption du solde), utiliser `BigDecimal` pour les montants monetaires, et fournir un historique de transactions immuable.

## Prerequis

- Java 17 ou superieur
- Maven 3.8+ ou Gradle (le projet utilise Maven)
- JUnit Jupiter 5.x (JUnit 5)
- Connaissance de base du TDD : Red (ecrire le test, il echoue) / Green (ecrire le code minimal, il passe) / Refactor (ameliorer sans casser)

## Etapes

### Etape 1 : Creer la classe Transaction

Ecrire d'abord le test unitaire pour la classe `Transaction`, qui decrit une operation bancaire. Verifier que le constructeur initialise correctement l'ID, le type, le montant, le soldeApresOperation, la description et la dateHeure. Implementer la classe pour que le test passe.

### Etape 2 : Creer la classe CompteBancaire (creation et solde initial)

Rediger les tests de creation du compte (`CompteBancaireTest`) :
- Un compte est cree avec un ID, un titulaire et un solde initial correct.
- Le solde initial peut etre zero.
- Un solde initial negatif doit lever une `IllegalArgumentException`.
Implementer le constructeur de `CompteBancaire` pour satisfaire ces tests.

### Etape 3 : Ajouter les operations de depot

Ecrire les tests pour la methode `deposer` :
- Un depot augmente le solde.
- Un depot de zero ou negatif est interdit.
- Plusieurs depots successifs modifient le solde correctement.
Implementer `deposer` avec synchronisation et enregistrement de la transaction.

### Etape 4 : Ajouter les operations de retrait

Ecrire les tests pour la methode `retirer` :
- Un retrait diminue le solde.
- Un retrait peut vider le compte (solde final zero).
- Un retrait superieur au solde est interdit.
- Un retrait de zero ou negatif est interdit.
Implementer `retirer` avec synchronisation et enregistrement de la transaction.

### Etape 5 : Gerer l'historique des transactions

Ecrire les tests pour l'historique :
- Le compte neuf a un historique vide.
- Chaque depot et retrait cree une transaction dans l'historique.
- Plusieurs operations produisent un historique ordonne.
- L'historique retourne par `getHistorique()` est immuable.
- `getDerniereTransaction()` retourne `null` si l'historique est vide.
Implementer `getHistorique()`, `getNombreTransactions()` et `getDerniereTransaction()`.

### Etape 6 : Ajouter les virements internes

Ecrire les tests pour `emettreVirement` et `recevoirVirement` :
- L'emission debite le compte.
- La reception credite le compte.
- Chaque operation cree une transaction du type approprie (`VIREMENT_EMIS` / `VIREMENT_RECU`).
- Un virement emis superieur au solde est interdit.
Implementer les deux methodes avec synchronisation.

### Etape 7 : Creer le ServiceVirement

Ecrire les tests pour `ServiceVirementTest` :
- Le virement debite la source et credite la destination.
- Une transaction est creee sur chaque compte.
- La somme totale des soldes est conservee apres virement.
- Le virement vers le meme compte est interdit.
- Le montant nul ou negatif est interdit.
- Le solde insuffisant chez l'emetteur leve une exception.
Implementer `ServiceVirement.effectuerVirement()`.

### Etape 8 : Tests de concurrence

Ecrire les tests multi-threads (`@Timeout`, `CountDownLatch`, `ExecutorService`) :
- Des depots concurrents produisent un solde final correct et un nombre de transactions egal au nombre d'operations.
- Des depots et retraits concurrents preservent l'integrite du solde.
Verifier que la synchronisation au niveau des methodes de `CompteBancaire` garantit la coherence.

### Etape 9 : Test de stabilite avec @RepeatedTest

Ajouter un test repete (`@RepeatedTest`) qui execute une sequence d'operations (depot, retrait, depot) et verifie que le solde final est toujours identique apres chaque repetition, demontrant la fiabilite deterministe du code.

### Etape 10 : Creer l'exception SoldeInsuffisantException

Bien que le code actuel utilise `IllegalArgumentException`, reflechir a la creation d'une exception metier dediee `SoldeInsuffisantException` pour distinguer les erreurs de solde des autres erreurs de validation. L'exception existe deja dans le projet comme `RuntimeException`.

## Commande d'execution

```bash
# Compiler et executer les tests
mvn test

# Executer uniquement les tests du lab04
mvn test -pl labs/lab04-tdd-banque

# Executer un test specifique
mvn test -pl labs/lab04-tdd-banque -Dtest=CompteBancaireTest

# Generer le rapport de couverture (si JaCoCo est configure)
mvn test jacoco:report
```

## Criteres de reussite

1. Tous les tests unitaires passent sans erreur ni echec.
2. Les montants sont manipules exclusivement avec `BigDecimal` (pas de `double` ou `float`).
3. Toutes les methodes publiques de `CompteBancaire` sont synchronisees (`synchronized`) pour garantir la thread-safety.
4. L'historique des transactions retourne par `getHistorique()` est une liste non modifiable.
5. Aucune transaction n'est creee si une operation echoue (validation avant modification).
6. Les tests de concurrence passent en moins de 5 secondes (`@Timeout`).
7. Le test repete (`@RepeatedTest`) passe 10 fois sur 10 sans defaillance.
8. Les virements conservent la masse monetaire totale (somme des soldes source + destination inchangee).
9. Les identifiants de transaction sont uniques et auto-incrementes via `AtomicLong`.
10. Le code est structure avec `@Nested` dans les classes de test pour une organisation claire par fonctionnalite.
