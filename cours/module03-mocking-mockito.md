# Module 3 : Mocking avec Mockito

> **Durée :** 1h30 (13h30–15h00) — Jour 1 Après-midi
> **Projet support :** `labs/lab03-mocking`
> **Dépendances :** JUnit 5.10.2, Mockito 5.10.0, mockito-junit-jupiter

---

## Objectifs pédagogiques

A l'issue de ce module, vous serez capable de :

1. Expliquer pourquoi le mocking est necessaire (isolement, rapidite, controle).
2. Differencier les 5 types de doublures de test (Dummy, Stub, Spy, Mock, Fake).
3. Creer des mocks avec `@Mock` et les injecter avec `@InjectMocks`.
4. Configurer le comportement des mocks avec `when().thenReturn()` et `doThrow()`.
5. Verifier les interactions avec `verify()` et ses variantes (times, never, atLeast).
6. Capturer des arguments passes a un mock avec `ArgumentCaptor`.
7. Utiliser `@Spy` pour des mocks partiels.

---

## PARTIE 1 -- THEORIE (45 min)

## 1.1 Pourquoi mocker ?

Lorsqu'on teste une classe `UserService`, celle-ci dépend de `UserRepository` (accès aux données) et de `EmailService` (envoi d'emails). Tester `UserService` sans mocks poserait quatre problèmes majeurs :

### Raison 1 — Isolation

Sans mock, un test de `UserService` testerait aussi `UserRepository` et `EmailService`. Si le test échoue, impossible de savoir immédiatement quelle couche est fautive. Le **mock** isole la classe testée : seul son comportement est vérifié, les dépendances sont simulées.

```
Sans mock : Test → UserService → UserRepository (vrai) → Base de données
 → EmailService (vrai) → Serveur SMTP

Avec mock : Test → UserService → UserRepository (mock)
 → EmailService (mock)
```

### Raison 2 — Rapidité

Un vrai `UserRepository` nécessite une base de données (H2, PostgreSQL, etc.) : connexion, transaction, rollback. Un `EmailService` réel enverrait un vrai email via SMTP. Ces opérations prennent des centaines de millisecondes, parfois des secondes. Multipliées par des centaines de tests, la suite devient lente.

Avec Mockito, un test s'exécute en **moins de 10 millisecondes** car tout se passe en mémoire.

### Raison 3 — Contrôle

Impossible de forcer la base de données à retourner une erreur réseau, ou le serveur SMTP à être indisponible, de façon fiable et reproductible. Avec un mock, on peut :

- Faire retourner `null` au repository pour simuler un utilisateur inexistant
- Faire lever une exception pour simuler une panne
- Retourner `true`/`false` à volonté pour l'envoi d'email

### Raison 4 — Simulation d'erreurs

Tester les cas d'erreur est indispensable (utilisateur introuvable, email déjà existant, solde insuffisant). Sans mock, provoquer ces situations réelles est fastidieux, voire impossible. Un mock permet de les simuler en une ligne :

```java
when(userRepository.findById(99L)).thenReturn(null); // when().thenReturn(null) : simule un utilisateur inexistant
```

Cette ligne force le repository à retourner `null` pour l'ID 99, ce qui déclenchera le chemin d'erreur dans `UserService.trouverParId()`.

---

## 1.2 Types de doublures de test

Le vocabulaire des doublures de test (test doubles) est standardisé par Gerard Meszaros dans *xUnit Test Patterns*. Voici les cinq types, du plus simple au plus sophistiqué :

| Doublure | Création | Comportement | Vérification | Usage |
|----------|----------|-------------|-------------|-------|
| **Dummy** | `new User(null, null, null, false)` | Aucun, juste présent pour remplir un paramètre | Aucune | Paramètre obligatoire mais jamais utilisé dans le test |
| **Stub** | `when(repo.findById(1L)).thenReturn(user)` | Retourne une valeur prédéfinie | Aucune | Fournir des données d'entrée contrôlées |
| **Spy** | `@Spy` ou `Mockito.spy(obj)` | Appelle la vraie méthode SAUF si stubbée | Peut être vérifié | Tester une classe réelle tout en surveillant certains appels |
| **Mock** | `@Mock` ou `Mockito.mock(Class)` | Retourne valeurs par défaut (null, 0, false) | **Oui** : `verify()` | Tester les **interactions** (la classe testée a-t-elle bien appelé la dépendance ?) |
| **Fake** | Classe écrite manuellement (ex: `FakeUserRepository` avec une `HashMap`) | Logique métier simplifiée mais fonctionnelle | Selon besoin | Remplacer une dépendance lourde par une version légère (ex: base en mémoire) |

### Détail de chaque type

**Dummy :** un objet qui ne sert qu'à satisfaire la signature d'une méthode. Il n'est jamais utilisé par le test. Exemple : `new User()` passé à une méthode qui ne lira que l'ID, mais l'ID est fourni séparément.

**Stub :** un mock configuré uniquement pour retourner des valeurs. On fait du *stubbing* avec `when().thenReturn()`. Le stub répond à des appels, mais on ne vérifie pas s'il a été appelé.

**Mock :** un stub + vérification. On configure son comportement (`when`) ET on vérifie qu'il a bien été appelé comme prévu (`verify`). C'est le type le plus utilisé dans les tests unitaires.

**Spy :** un objet **réel** partiellement mocké. Par défaut, toutes les méthodes appellent le vrai code. On peut *stubber* certaines méthodes pour en modifier le comportement. Utile quand on veut tester une classe concrète sans la réécrire entièrement.

**Fake :** une implémentation manuelle de l'interface. Par exemple, un `FakeUserRepository` qui stocke les utilisateurs dans une `HashMap` au lieu de PostgreSQL. Le fake a un vrai comportement, mais simplifié.

> **Dans ce module, nous utilisons principalement les MOCKS et les SPIES avec Mockito.**

---

## 1.3 L'extension Mockito pour JUnit 5

Pour utiliser Mockito avec JUnit 5, on ajoute la dépendance Maven suivante dans le `pom.xml` :

```xml
<dependency>
 <groupId>org.mockito</groupId>
 <artifactId>mockito-junit-jupiter</artifactId>
 <version>5.10.0</version>
 <scope>test</scope>
</dependency>
```

Cette dépendance (`pom.xml` du lab03, ligne 37-42) fournit l'extension JUnit 5 qui s'active avec l'annotation sur la classe de test :

```java
@ExtendWith(MockitoExtension.class) // Active l'intégration Mockito + JUnit 5 (initialise @Mock, @InjectMocks, @Captor, @Spy)
class UserServiceTest {
 // ...
}
```

Voici ce que fait `MockitoExtension` à chaque test :

1. **Initialisation automatique des `@Mock`** — Avant chaque test (`@BeforeEach`), l'extension détecte tous les champs annotés `@Mock` et crée un mock (un simulacre) pour chacun. Cela remplace l'appel manuel `Mockito.mock()` en `@BeforeEach`.

2. **Injection des mocks dans `@InjectMocks`** — L'extension crée une **instance réelle** de la classe annotée `@InjectMocks` et y injecte tous les `@Mock` du test. L'injection suit l'ordre : constructeur, puis setters, puis champs directement.

3. **Validation de l'usage des mocks** — Après chaque test (`@AfterEach`), l'extension vérifie qu'aucun appel non stubé n'a été fait sur les mocks (détection des `UnnecessaryStubbingException`), assure la cohérence des stubs, et garantit que les mocks sont utilisés correctement.

> **Sans `@ExtendWith(MockitoExtension.class)`, les `@Mock` et `@InjectMocks` ne sont pas initialisés et restent `null`.**

---

## 1.4 `@Mock` — Créer un simulacre

L'annotation `@Mock` crée un **simulacre** (proxy) qui implémente l'interface ou étend la classe cible. Ce simulacre n'a **aucun comportement réel** : toutes ses méthodes retournent des valeurs par défaut.

### Comportement par défaut d'un `@Mock`

| Type de retour | Valeur par défaut |
|---------------|-------------------|
| Objet (classe) | `null` |
| `int`, `long`, `short`, `byte` | `0` |
| `double`, `float` | `0.0` |
| `boolean` | `false` |
| `char` | `'\u0000'` (caractère nul) |
| Collection (`List`, `Set`, `Map`) | Collection vide (pas `null`) |
| `Optional` | `Optional.empty()` |

Exemple tiré du code du lab :

```java
@Mock          // @Mock crée un simulacre (proxy sans comportement réel) de l'interface
private UserRepository userRepository;

@Mock          // Toutes les méthodes de ce mock retournent des valeurs par défaut (null, false, 0)
private EmailService emailService;
```

Ici, `userRepository` est un mock de l'interface `UserRepository`. Sans stubbing, appeler `userRepository.findById(1L)` retourne `null`. Appeler `userRepository.existsByEmail("x@y.com")` retourne `false`. Appeler `emailService.envoyerEmail(...)` retourne `false`.

### Équivalent manuel (sans annotation)

```java
// Sans @Mock, on écrirait dans un @BeforeEach :
UserRepository userRepository = Mockito.mock(UserRepository.class); // Mockito.mock() : crée un mock manuellement (sans annotation)
EmailService emailService = Mockito.mock(EmailService.class);       // @Mock évite ce code répétitif
```

L'annotation `@Mock` supprime ce code répétitif : Mockito le fait pour nous.

### Une interface seulement

`UserRepository` et `EmailService` sont des **interfaces**. Mockito crée un proxy dynamique qui implémente l'interface sans logique métier. On peut aussi mocker une classe concrète (le proxy étend alors la classe), mais c'est plus rare.

---

## 1.5 `@InjectMocks` — Injecter les mocks dans la classe réelle

L'annotation `@InjectMocks` crée une **instance réelle** de la classe et y injecte automatiquement tous les `@Mock` présents dans le test.

```java
@InjectMocks  // @InjectMocks crée une instance réelle de UserService
private UserService userService;
```

Ici, `userService` est un **vrai** objet `UserService` (pas un mock). Son constructeur exige deux paramètres :

```java
public UserService(UserRepository userRepository, EmailService emailService) {
 this.userRepository = userRepository; // Injection des dépendances par constructeur
 this.emailService = emailService;     // Les @Mock déclarés sont injectés automatiquement
}
```

Mockito détecte ce constructeur et y injecte les deux `@Mock` déclarés dans le test : `userRepository` et `emailService`. Résultat : on a un `UserService` fonctionnel dont toutes les dépendances sont des mocks.

### Ordre d'injection

Mockito essaie trois stratégies dans l'ordre :

1. **Injection par constructeur** — C'est la méthode préférée. Si la classe possède un constructeur dont les paramètres correspondent exactement aux `@Mock` disponibles, Mockito l'utilise. C'est le cas de `UserService` car son constructeur `UserService(UserRepository, EmailService)` correspond aux deux `@Mock`.

2. **Injection par setter** — Si l'injection par constructeur échoue, Mockito cherche des setters correspondant aux types des `@Mock`.

3. **Injection par champ** — En dernier recours, Mockito injecte directement dans les champs privés via réflexion.

> **Condition indispensable :** la classe annotée `@InjectMocks` doit pouvoir être instanciée avec les `@Mock` disponibles. Si le constructeur exige un troisième paramètre et qu'il n'y a que deux `@Mock`, l'injection échoue.

---

## 1.6 Stubbing — `when().thenReturn()` et variantes

Le **stubbing** consiste à définir ce que le mock doit retourner quand une méthode est appelée avec certains arguments. C'est le cœur du *Arrange* en AAA.

### Syntaxe de base : `when().thenReturn()`

```java
when(userRepository.findById(1L)).thenReturn(userAttendu); // when().thenReturn() : stubbing de base
                                                            // quand findById(1L) est appelé, retourne userAttendu
```

Cette ligne signifie : *quand la méthode `findById` sera appelée avec l'argument `1L`, alors retourne l'objet `userAttendu`*.

- `when(...)` : spécifie la méthode du mock qu'on veut stùbber
- `thenReturn(...)` : définit la valeur de retour

Si la méthode est appelée avec un argument différent (ex: `findById(2L)`), le stubbing ne s'applique pas et la valeur par défaut est retournée (`null` pour un objet).

### Syntaxe complète dans le code du lab

**Retourner un objet (test `trouverParId_existant`, ligne 30) :**

```java
User userAttendu = new User(1L, "Alice", "alice@example.com", true);
when(userRepository.findById(1L)).thenReturn(userAttendu);
                                                  // when().thenReturn() : quand findById(1L) est appelé, retourne Alice
```

→ Quand `findById` est appelé avec `1L`, le mock retourne l'utilisateur Alice. Cela permet de tester la méthode `trouverParId` sans base de données.

**Retourner `null` (test `trouverParId_inexistant`, ligne 42) :**

```java
when(userRepository.findById(99L)).thenReturn(null); // thenReturn(null) : simule un utilisateur inexistant
```

→ Simule un utilisateur inexistant. La méthode `trouverParId` vérifie si le résultat est `null` et lève `UserNotFoundException`, ce que le test vérifie avec `assertThrows`.

**Retourner un booléen (test `creerUtilisateur_succes`, ligne 51) :**

```java
when(userRepository.existsByEmail("bob@test.com")).thenReturn(false); // when().thenReturn(false) : l'email n'existe pas encore
```

→ L'email n'existe pas encore, la création peut continuer.

### `thenThrow()` — Simuler une exception

On peut forcer un mock à lever une exception :

```java
when(userRepository.save(any(User.class)))                    // any(User.class) : accepte n'importe quel objet User
 .thenThrow(new RuntimeException("Base de données indisponible")); // thenThrow() : force le mock à lever une exception
```

Ce n'est pas utilisé dans le lab03 mais c'est une technique courante pour tester les blocs `try-catch` et la résilience.

### `thenAnswer()` — Comportement dynamique

`thenReturn` retourne toujours la même valeur. `thenAnswer` permet un comportement **dynamique**, calculé au moment de l'appel :

```java
when(userRepository.save(any(User.class)))          // any(User.class) : accepte n'importe quel User
 .thenAnswer(invocation -> invocation.getArgument(0)); // thenAnswer() : retourne dynamiquement le 1er argument reçu
```

Détaillons cette ligne utilisée dans le test `creerUtilisateur_succes` (ligne 52-53) :

- `invocation` est un objet `InvocationOnMock` qui représente l'appel en cours
- `invocation.getArgument(0)` récupère le premier argument passé à `save()`, c'est-à-dire l'objet `User` que `UserService` essaie de sauvegarder
- Le mock **retourne ce même objet**, simulant le comportement d'un repository qui renvoie l'entité sauvegardée

**Variante plus sophistiquée (test `saveRetourneUtilisateurAvecId`, ligne 171-175) :**

```java
when(userRepository.save(any(User.class))).thenAnswer(invocation -> { // thenAnswer() : comportement dynamique défini par une lambda
 User user = invocation.getArgument(0);                               // Récupère l'argument passé à save()
 user.setId(42L); // Simule l'attribution d'un ID par la base
 return user;     // Retourne l'utilisateur modifié avec son nouvel ID
});
```

→ Ici, `thenAnswer` **modifie** l'objet passé en paramètre (lui attribue un ID de 42) avant de le retourner. Cela simule le comportement d'une base de données qui génère une clé primaire lors de l'insertion. Le test vérifie ensuite que `resultat.getId()` vaut `42L`.

### `doReturn().when()` — Stubbing pour les spies

Pour les objets annotés `@Spy` (voir section 1.10), la syntaxe est inversée :

```java
doReturn(100).when(compteurSpy).incrementer(); // doReturn().when() : stubbing pour spy (n'appelle pas la vraie méthode)
```

On utilise `doReturn().when()` au lieu de `when().thenReturn()` car `when(spy.methode())` appellerait la **vraie** méthode du spy avant même d'enregistrer le stub, ce qui peut causer des effets de bord. `doReturn().when()` contourne ce problème en n'appelant jamais la vraie méthode.

### `doThrow().when()` — Pour les méthodes `void`

Les méthodes `void` ne peuvent pas être stubbées avec `when()` car `when()` exige un type de retour. On utilise `doThrow()` :

```java
doThrow(new RuntimeException("Erreur")).when(emailService).envoyerEmail(anyString(), anyString(), anyString());
// doThrow().when() : force une méthode void à lever une exception (when() ne fonctionne pas avec void)
```

---

## 1.7 Vérification — `verify()`

Le stubbing (`when`) définit le comportement **avant** l'exécution. La vérification (`verify`) contrôle les interactions **après** l'exécution.

Le principe : un mock doit avoir été appelé d'une certaine manière, un certain nombre de fois, éventuellement dans un certain ordre.

### `verify(mock).methode()` — Appelée exactement 1 fois

```java
verify(userRepository).save(any(User.class)); // verify() : vérifie que save() a été appelé exactement 1 fois
```

Cette ligne (test `creerUtilisateur_succes`, ligne 63) vérifie que `userRepository.save()` a été appelée **exactement une fois** avec n'importe quel `User`. Si la méthode n'a jamais été appelée, ou a été appelée 2 fois, le test échoue.

### `verify(mock, times(n))` — Appelée n fois

```java
verify(userRepository, times(2)).save(any(User.class)); // verify() + times(2) : vérifie que save() a été appelé exactement 2 fois
```

Vérifie que `save` a été appelée exactement 2 fois. `times(1)` est équivalent à `verify(mock)` tout court.

### `verify(mock, never())` — Jamais appelée

```java
verify(userRepository, never()).save(any(User.class));                                    // verify() + never() : vérifie que save() n'a jamais été appelé
verify(emailService, never()).envoyerEmail(anyString(), anyString(), anyString());         // never() garantit qu'aucun email n'a été envoyé
```

Ces deux lignes (test `creerUtilisateur_emailExistant`, lignes 75-76) vérifient que la sauvegarde ET l'envoi d'email **n'ont jamais eu lieu**. Le test valide que quand l'email existe déjà, la méthode `creerUtilisateur` lève une exception **avant** d'appeler `save` ou `envoyerEmail`.

### `verify(mock, atLeast(n))` / `atMost(n)` / `atLeastOnce()`

```java
verify(userRepository, atLeast(1)).save(any(User.class));                              // verify() + atLeast(1) : au moins 1 appel
verify(emailService, atMost(3)).envoyerEmail(anyString(), anyString(), anyString());    // atMost(3) : au plus 3 appels
verify(userRepository, atLeastOnce()).findById(anyLong());                             // atLeastOnce() : équivalent à atLeast(1)
```

- `atLeast(n)` : au moins n appels
- `atMost(n)` : au plus n appels
- `atLeastOnce()` : équivalent à `atLeast(1)`

### `verifyNoMoreInteractions(mock)` — Pas d'appels supplémentaires

```java
verify(userRepository).findById(1L);                // verify() : vérifie que findById(1L) a été appelé
verifyNoMoreInteractions(userRepository);            // verifyNoMoreInteractions() : garantit qu'aucune autre méthode de ce mock n'a été appelée
```

Utilisé dans le test `pasDAutresInteractions` (ligne 161). Après avoir vérifié que `findById` a été appelé, `verifyNoMoreInteractions` vérifie qu'**aucune autre méthode** du mock `userRepository` n'a été appelée. Cela garantit que `trouverParId` n'appelle que `findById` et rien d'autre.

### `verifyNoInteractions(mock)` — Aucun appel du tout

```java
verifyNoInteractions(emailService); // verifyNoInteractions() : vérifie que ce mock n'a subi aucun appel, d'aucune méthode
```

Utilisé dans le test `pasDAutresInteractions` (ligne 162). Vérifie que le mock `emailService` n'a subi **aucun appel**, d'aucune méthode. Ici, cela confirme que `trouverParId` ne touche pas du tout au service d'email, ce qui est correct car la recherche par ID n'envoie pas d'email.

### `InOrder` — Vérification de l'ordre des appels

```java
InOrder ordre = inOrder(userRepository, emailService);          // InOrder : crée un vérificateur d'ordre lié aux deux mocks
ordre.verify(userRepository).save(any(User.class));             // Le save() doit avoir eu lieu en PREMIER
ordre.verify(emailService).envoyerEmail(anyString(), anyString(), anyString()); // L'envoi d'email doit avoir eu lieu en SECOND
```

Utilisé dans le test `ordreDesAppels` (lignes 194-196). `InOrder` vérifie que les méthodes ont été appelées **dans un ordre précis** sur un ou plusieurs mocks :

1. `inOrder(mock1, mock2)` crée un vérificateur d'ordre lié à ces deux mocks
2. Les `verify` via `ordre` doivent être écrits dans l'ordre exact d'appel attendu
3. Si `save` est appelé après `envoyerEmail`, le test échoue

Dans le contexte de `creerUtilisateur`, il est crucial que l'utilisateur soit sauvegardé (`save`) **avant** l'envoi de l'email (`envoyerEmail`), car si l'email part mais la sauvegarde échoue, l'utilisateur reçoit un email pour un compte qui n'existe pas.

---

## 1.8 ArgumentMatchers

Les **matchers d'arguments** permettent de stùbber ou vérifier des méthodes sans spécifier la valeur exacte des paramètres. Ils sont dans la classe `org.mockito.ArgumentMatchers`.

### Matchers de base

| Matcher | Usage | Équivalent |
|---------|-------|-----------|
| `any()` | N'importe quelle valeur (objet ou primitif) | `any(Object.class)` |
| `any(Class)` | N'importe quelle instance de la classe | `any(User.class)` |
| `anyString()` | N'importe quelle chaîne (ou `null`) | — |
| `anyInt()` | N'importe quel `int` (ou `0` si `null`) | — |
| `anyLong()` | N'importe quel `long` (ou `0` si `null`) | — |
| `anyBoolean()` | N'importe quel `boolean` | — |
| `anyList()` | N'importe quelle `List` | — |
| `eq(valeur)` | La valeur exacte (utile quand on mélange matchers et valeurs) | — |

### Exemple du lab : `any()` et `anyString()`

```java
when(userRepository.save(any(User.class)))          // any(User.class) : matcher qui accepte n'importe quel objet User
 .thenAnswer(invocation -> invocation.getArgument(0)); // thenAnswer() : retourne l'argument reçu (simule la persistance)
when(emailService.envoyerEmail(anyString(), anyString(), anyString())) // anyString() : accepte n'importe quelle chaîne
 .thenReturn(true);                                                    // thenReturn(true) : l'envoi d'email réussit
```

- `any(User.class)` : accepte n'importe quel objet `User`
- `anyString()` : accepte n'importe quelle chaîne (utilisé 3 fois pour destinataire, sujet, contenu)

### RÈGLE IMPÉRATIVE : tout ou rien

Si **un seul** argument utilise un matcher (`any()`, `anyString()`, etc.), alors **tous les arguments** de cet appel doivent utiliser des matchers. On ne peut pas mélanger valeurs exactes et matchers.

 **Interdit :**
```java
// Interdit :
when(repo.save("exact", any(User.class))).thenReturn(...); // ERREUR !
// On ne peut pas mélanger valeur exacte ("exact") et matcher (any(User.class))
```

 **Correct avec `eq()` :**
```java
verify(emailService).envoyerEmail(eq("bob@test.com"), anyString(), anyString());
// eq() permet de mélanger une valeur exacte avec des matchers (anyString())
```

Cette ligne (test `creerUtilisateur_succes`, ligne 64) vérifie que l'email a été envoyé avec le destinataire exact `"bob@test.com"`, tout en acceptant n'importe quel sujet et contenu. Grâce à `eq()`, on mélange une valeur exacte avec des matchers, ce qui est autorisé.

### Exemple avec `anyLong()` et `eq()`

```java
// Stubbing avec anyLong() — test argumentMatchersDemo, ligne 82
when(userRepository.findById(anyLong())).thenReturn(          // anyLong() : accepte n'importe quel Long pour le stubbing
 new User(1L, "Test", "test@test.com", true));

// Appel réel
User u = userService.trouverParId(42L);

// Vérification avec eq() — ligne 88
verify(userRepository).findById(eq(42L));                    // eq(42L) : vérifie que l'appel était bien avec 42L précisément
```

Le stubbing utilise `anyLong()` pour accepter n'importe quel ID. La vérification utilise `eq(42L)` pour s'assurer que l'appel a bien été fait avec `42L` (et pas `43L` ou autre).

---

## 1.9 ArgumentCaptor — Capturer un argument pour l'inspecter

Parfois, on ne veut pas seulement vérifier qu'une méthode a été appelée, mais aussi **inspecter l'objet** qui lui a été passé. C'est le rôle de l'`ArgumentCaptor`.

### Déclaration

```java
@Captor                                                // @Captor crée un ArgumentCaptor pour intercepter les arguments passés à un mock
private ArgumentCaptor<User> userCaptor;               // ArgumentCaptor<User> : spécialisé pour capturer des objets de type User
```

L'annotation `@Captor` (ligne 91-92 du test) crée un capteur spécialisé pour le type `User`. L'extension Mockito l'initialise automatiquement (comme `@Mock`).

### Utilisation en trois étapes

```java
// --- Arrange : configuration des mocks ---
when(userRepository.existsByEmail(anyString())).thenReturn(false);        // L'email n'existe pas
when(userRepository.save(any(User.class)))                                // any(User.class) : accepte n'importe quel User
 .thenAnswer(inv -> inv.getArgument(0));                                  // thenAnswer() : retourne l'argument reçu
when(emailService.envoyerEmail(anyString(), anyString(), anyString()))    // anyString() : accepte n'importe quelle chaîne
 .thenReturn(true);                                                       // L'envoi d'email réussit
```

**Étape 2 — Act :** on appelle la méthode testée.

```java
userService.creerUtilisateur("Charlie", "charlie@test.com"); // Act : appel de la méthode à tester
```

**Étape 3 — Assert avec capture :** on utilise `captor.capture()` à la place d'un matcher dans `verify()`.

```java
verify(userRepository).save(userCaptor.capture()); // userCaptor.capture() intercepte l'argument passé à save()
User capture = userCaptor.getValue();               // getValue() : récupère l'objet User capturé
```

On peut ensuite faire des assertions sur l'objet capturé :

```java
assertEquals("Charlie", capture.getNom());           // Vérifie le nom de l'utilisateur capturé
assertEquals("charlie@test.com", capture.getEmail()); // Vérifie l'email
assertTrue(capture.isActif());                        // Vérifie que le compte est actif
assertNull(capture.getId(), "L'ID doit être null avant la persistance"); // L'ID est null (non encore généré par la base)
```

**Étape 2 — Act :** on appelle la méthode testée.

```java
userService.creerUtilisateur("Charlie", "charlie@test.com"); // Act : appel de la méthode testée
```

**Étape 3 — Assert avec capture :** on utilise `captor.capture()` à la place d'un matcher dans `verify()`.

```java
verify(userRepository).save(userCaptor.capture()); // userCaptor.capture() : intercepte le User passé à save()
User capture = userCaptor.getValue();               // getValue() : récupère l'objet User capturé
```

1. `userCaptor.capture()` intercepte l'argument passé à `save()` et le stocke
2. `userCaptor.getValue()` récupère l'objet capturé

On peut ensuite faire des assertions sur l'objet capturé :

```java
assertEquals("Charlie", capture.getNom());           // Vérifie le nom de l'utilisateur capturé
assertEquals("charlie@test.com", capture.getEmail()); // Vérifie l'email
assertTrue(capture.isActif());                        // Vérifie que le compte est actif
assertNull(capture.getId(), "L'ID doit être null avant la persistance"); // L'ID est null (généré par la base)
```

L'intérêt pédagogique de cette dernière assertion : avant d'être sauvegardé, l'utilisateur a un ID `null`. C'est la base de données (ou le mock via `thenAnswer`) qui attribue l'ID. Le test vérifie que `UserService` ne définit **pas** l'ID lui-même.

### Sans `@Captor` (équivalent manuel)

```java
ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class); // Création manuelle d'un ArgumentCaptor (sans @Captor)
verify(userRepository).save(captor.capture());                     // captor.capture() : intercepte l'argument passé à save()
User capture = captor.getValue();                                   // getValue() : récupère la valeur capturée
```

L'annotation `@Captor` supprime l'appel à `ArgumentCaptor.forClass()`.

---

## 1.10 `@Spy` — Mock partiel

Un **spy** (espion) est un objet **réel** dont on peut remplacer le comportement de certaines méthodes tout en gardant le comportement réel des autres.

### Différence `@Mock` vs `@Spy`

| Caractéristique | `@Mock` | `@Spy` |
|----------------|---------|--------|
| Objet créé | Proxy vide (pas d'instance réelle) | Instance réelle de la classe |
| Comportement par défaut | Valeurs par défaut (null, 0, false) | Vraie méthode appelée |
| Stubbing | `when().thenReturn()` | `doReturn().when()` |
| Utilisation typique | Dépendances externes (interfaces) | Classes concrètes dont on veut surveiller une partie |

### Exemple du lab : `CompteurService`

Le test `spyDemo` (lignes 140-150) définit une classe interne simple et un spy :

```java
static class CompteurService {
 public int incrementer() { return 1; }   // Méthode réelle : retourne 1
 public int decrementer() { return -1; }  // Méthode réelle : retourne -1
}

@Spy                                         // @Spy crée un espion : objet réel, méthodes partiellement remplaçables
private CompteurService compteurSpy;         // Par défaut, toutes les méthodes appellent le vrai code
```

`compteurSpy` est un **vrai** `CompteurService`. Par défaut, toutes ses méthodes exécutent le vrai code.

```java
// Sans stubbing, le spy appelle la vraie méthode
assertEquals(1, compteurSpy.incrementer(),           // Appel réel : retourne 1 (vraie méthode)
 "Par défaut, le spy appelle la vraie méthode");

// On stubbe une méthode avec doReturn().when()
doReturn(100).when(compteurSpy).incrementer();       // doReturn().when() : stubbing pour spy sans appeler la vraie méthode

// La méthode stubbée retourne 100
assertEquals(100, compteurSpy.incrementer(),          // Stubbing actif : retourne 100 au lieu de 1
 "La méthode stubbée retourne 100");

// La méthode non stubbée continue d'appeler la vraie implémentation
assertEquals(-1, compteurSpy.decrementer(),           // Non stubbée : appelle la vraie méthode, retourne -1
 "La méthode non stubbée appelle la vraie implémentation");
```

**Pourquoi `doReturn().when()` et pas `when().thenReturn()` ?**

Avec un spy, écrire `when(compteurSpy.incrementer()).thenReturn(100)` **appellerait la vraie méthode** `incrementer()` au moment du stubbing, ce qui pourrait avoir des effets de bord indésirables (modification d'état, appel réseau, etc.). `doReturn(100).when(compteurSpy).incrementer()` évite cet appel : le stub est enregistré sans exécuter la vraie méthode.

### Quand utiliser un spy ?

- Tester une classe qui a une méthode complexe qu'on ne veut pas exécuter intégralement
- Surveiller les appels internes d'une classe concrète (par exemple, vérifier qu'une méthode privée appelée par une méthode publique a bien été exécutée)
- Tester du code legacy difficile à refactorer

> **Règle générale : préférer `@Mock` pour les dépendances (interfaces) et `@Spy` pour les cas spécifiques où on a besoin du comportement réel partiel.**

---

## PARTIE 2 -- PRATIQUE PAS A PAS (40 min)

Nous allons décortiquer chaque test du fichier `UserServiceTest.java`, en suivant la structure **Arrange → Act → Assert** (AAA).

### Prérequis : les annotations de la classe de test

```java
@ExtendWith(MockitoExtension.class)                            // Active l'extension Mockito pour JUnit 5 (initialise @Mock, @InjectMocks, @Captor)
@DisplayName("Tests du UserService avec Mockito")              // Nom personnalisé affiché dans le rapport de test
class UserServiceTest {

 @Mock                                                        // @Mock crée un simulacre (proxy) de l'interface
 private UserRepository userRepository;                       // Toutes les méthodes retournent des valeurs par défaut

 @Mock                                                        // @Mock crée un simulacre de EmailService
 private EmailService emailService;                           // Les méthodes retournent null/false par défaut

 @InjectMocks                                                 // @InjectMocks crée une VRAIE instance de UserService
 private UserService userService;                             // Les @Mock sont injectés via le constructeur
```

- `@ExtendWith(MockitoExtension.class)` : active l'extension Mockito pour JUnit 5 (initialisation des mocks et injection)
- `@Mock` sur `userRepository` et `emailService` : crée des simulacres de ces deux interfaces
- `@InjectMocks` sur `userService` : crée une **vraie** instance de `UserService` en injectant les deux mocks via le constructeur `UserService(UserRepository, EmailService)`

---

## Test 1 : `trouverParId_existant` — Stubbing simple

```java
@Test                                                       // @Test marque cette méthode comme un test JUnit
@DisplayName("trouverParId — retourne l'utilisateur quand il existe")
void trouverParId_existant() {

 // --- Arrange : préparer les données et configurer les mocks ---
 User userAttendu = new User(1L, "Alice", "alice@example.com", true); // Crée l'utilisateur attendu (Alice, active)
 when(userRepository.findById(1L)).thenReturn(userAttendu);           // when().thenReturn() : quand findById(1L) est appelé, retourne Alice

 // --- Act : exécuter la méthode testée ---
 User resultat = userService.trouverParId(1L);                        // Appel de la méthode à tester avec l'ID 1

 // --- Assert : vérifier les résultats ---
 assertNotNull(resultat);                                             // L'utilisateur retourné n'est pas null
 assertEquals("Alice", resultat.getNom());                            // Le nom correspond à Alice
 assertEquals("alice@example.com", resultat.getEmail());              // L'email est correct
}
```

**Arrange :**
- Création d'un objet `User` représentant Alice (id=1, active)
- `when(userRepository.findById(1L)).thenReturn(userAttendu)` : on dit au mock *quand findById(1L) est appelé, retourne Alice*

**Act :**
- `userService.trouverParId(1L)` : appel de la méthode testée

**Assert :**
- `assertNotNull(resultat)` : l'utilisateur retourné n'est pas null
- `assertEquals("Alice", resultat.getNom())` : le nom est bien Alice
- `assertEquals("alice@example.com", resultat.getEmail())` : l'email est correct

**Ce que ce test valide :** la méthode `trouverParId` appelle `userRepository.findById()` avec le bon ID et retourne l'utilisateur trouvé sans modification.

---

## Test 2 : `trouverParId_inexistant` — `thenReturn(null)` + `assertThrows`

```java
@Test
@DisplayName("trouverParId — lève UserNotFoundException si l'utilisateur n'existe pas")
void trouverParId_inexistant() {
 // --- Arrange : le mock retourne null pour simuler un utilisateur inexistant ---
 when(userRepository.findById(99L)).thenReturn(null); // when().thenReturn(null) : simule un utilisateur inexistant

 // --- Act + Assert : vérifier que l'exception est levée ---
 assertThrows(UserNotFoundException.class,            // assertThrows : vérifie qu'une exception de ce type est levée
 () -> userService.trouverParId(99L));                // L'appel avec l'ID 99 doit lever UserNotFoundException
}
```

**Arrange :**
- `when(userRepository.findById(99L)).thenReturn(null)` : le mock retourne `null` pour l'ID 99, simulant un utilisateur inexistant

**Act + Assert combinés :**
- `assertThrows(UserNotFoundException.class, () -> userService.trouverParId(99L))` : on s'attend à ce que l'appel lève une `UserNotFoundException`

**Ce que ce test valide :** la méthode `trouverParId` détecte un résultat `null` et lève l'exception personnalisée `UserNotFoundException` au lieu de retourner `null`. Rappel du code source :

```java
public User trouverParId(Long id) {
 User user = userRepository.findById(id);            // Appel au repository (mocké en test)
 if (user == null) {                                  // Si le repository retourne null (utilisateur inexistant)
  throw new UserNotFoundException("Utilisateur introuvable : id=" + id); // Lever une exception personnalisée
 }
 return user;                                        // Sinon, retourner l'utilisateur trouvé
}
```

---

## Test 3 : `creerUtilisateur_succes` — `verify()`, `any()`, `eq()`

```java
@Test
@DisplayName("creerUtilisateur — sauvegarde l'utilisateur et envoie un email")
void creerUtilisateur_succes() {
 // --- Arrange : configurer les comportements des mocks ---
 when(userRepository.existsByEmail("bob@test.com")).thenReturn(false);       // L'email n'existe pas, création autorisée
 when(userRepository.save(any(User.class)))                                   // any(User.class) : accepte n'importe quel User
 .thenAnswer(invocation -> invocation.getArgument(0));                        // thenAnswer() : retourne le User reçu en argument
 when(emailService.envoyerEmail(anyString(), anyString(), anyString()))       // anyString() x3 : accepte n'importe quel destinataire, sujet, contenu
 .thenReturn(true);                                                           // L'envoi d'email réussit

 // --- Act : exécuter la méthode testée ---
 User resultat = userService.creerUtilisateur("Bob", "bob@test.com");         // Création d'un nouvel utilisateur Bob

 // --- Assert : vérifier la valeur de retour ---
 assertNotNull(resultat);                                                     // L'utilisateur créé n'est pas null
 assertEquals("Bob", resultat.getNom());                                      // Le nom est bien Bob
 assertTrue(resultat.isActif());                                              // Le compte est actif par défaut

 // --- Assert : vérifier les interactions avec les mocks ---
 verify(userRepository).save(any(User.class));                                // verify() : save() a été appelé exactement 1 fois
 verify(emailService).envoyerEmail(eq("bob@test.com"), anyString(), anyString()); // eq() : vérifie le destinataire exact, anyString() pour sujet/contenu
}
```

**Arrange (3 stubbings) :**

1. `existsByEmail("bob@test.com")` → `false` : l'email n'existe pas, la création est autorisée
2. `save(any(User.class))` → `thenAnswer` retourne l'argument reçu : simule un repository qui retourne l'entité sauvegardée (sans changer l'ID dans cette variante)
3. `envoyerEmail(anyString(), anyString(), anyString())` → `true` : simule un envoi d'email réussi

**Act :**
- Appel de `creerUtilisateur("Bob", "bob@test.com")`

**Assert :**

- `assertNotNull(resultat)` : l'utilisateur créé est retourné
- `assertEquals("Bob", resultat.getNom())` : le nom est bien Bob
- `assertTrue(resultat.isActif())` : le compte est actif par défaut (voir code source ligne 26 : `new User(null, nom, email, true)`)
- `verify(userRepository).save(any(User.class))` : la sauvegarde a été appelée exactement 1 fois
- `verify(emailService).envoyerEmail(eq("bob@test.com"), anyString(), anyString())` : l'email a été envoyé au bon destinataire (`eq("bob@test.com")`), peu importe le sujet et le contenu

**Ce que ce test valide :** le chemin nominal complet de `creerUtilisateur` — vérification d'unicité de l'email, création de l'utilisateur, sauvegarde, envoi d'email de bienvenue.

---

## Test 4 : `creerUtilisateur_emailExistant` — `verify` + `never()`

```java
@Test
@DisplayName("creerUtilisateur — échoue si l'email existe déjà")
void creerUtilisateur_emailExistant() {
 // --- Arrange : configurer le mock pour que l'email existe déjà ---
 when(userRepository.existsByEmail("existant@test.com")).thenReturn(true); // L'email est déjà utilisé

 // --- Act + Assert : vérifier que l'exception est levée ---
 assertThrows(IllegalArgumentException.class,                               // assertThrows : l'exception attendue est IllegalArgumentException
 () -> userService.creerUtilisateur("Eve", "existant@test.com"));           // La création avec un email existant doit échouer

 // --- Assert : vérifier que rien n'a été sauvegardé ni envoyé ---
 verify(userRepository, never()).save(any(User.class));                     // verify() + never() : save() n'a JAMAIS été appelé
 verify(emailService, never()).envoyerEmail(anyString(), anyString(), anyString()); // never() : aucun email n'a été envoyé
}
```

**Arrange :**
- `existsByEmail("existant@test.com")` → `true` : l'email est déjà pris

**Act + Assert :**
- `assertThrows` : l'appel lève `IllegalArgumentException`

**Vérifications critiques :**
- `verify(userRepository, never()).save(...)` : la sauvegarde n'a **jamais** été appelée. Si le code sauvegardait avant de vérifier l'email, ce `verify` échouerait
- `verify(emailService, never()).envoyerEmail(...)` : aucun email n'a été envoyé

**Ce que ce test valide :** la méthode protège contre les doublons d'email et **n'effectue aucune opération** (pas de sauvegarde, pas d'email) avant de lever l'exception. Rappel du code source :

```java
if (userRepository.existsByEmail(email)) {                  // Vérifie si l'email existe déjà
 throw new IllegalArgumentException("Cet email est déjà utilisé : " + email); // Lève une exception AVANT toute sauvegarde
}
```
```

Le `throw` est exécuté **avant** le `save` et l'`envoyerEmail`.

---

## Test 5 : `argumentMatchersDemo` — `anyLong()` et `eq()`

```java
@Test
@DisplayName("ArgumentMatchers : any() et eq()")
void argumentMatchersDemo() {
 // --- Arrange : stubbing avec anyLong() pour accepter n'importe quel ID ---
 when(userRepository.findById(anyLong())).thenReturn(                       // anyLong() : accepte n'importe quelle valeur de type Long
 new User(1L, "Test", "test@test.com", true));                              // Retourne toujours le même utilisateur de test

 // --- Act : appeler la méthode avec un ID arbitraire ---
 User u = userService.trouverParId(42L);                                    // Appel avec l'ID 42
 assertNotNull(u);                                                          // Vérifie que le résultat n'est pas null

 // --- Assert : vérifier que l'appel au mock était avec le bon ID ---
 verify(userRepository).findById(eq(42L));                                  // eq(42L) : vérifie que l'ID passé était bien 42L
}
```

**Arrange :**
- `anyLong()` : le stubbing accepte **n'importe quel `Long`**, pas seulement une valeur précise. Cela évite de devoir stùbber pour chaque ID possible

**Act :**
- Appel avec l'ID `42L`

**Assert :**
- `verify(userRepository).findById(eq(42L))` : on vérifie que l'appel a bien été fait avec `42L` (et pas avec une autre valeur). `eq()` est obligatoire ici car on ne peut pas mélanger matchers et valeurs exactes ; or `eq()` est lui-même un matcher, donc compatible

**Ce que ce test démontre :** la différence entre `anyLong()` (pour le stubbing, large) et `eq()` (pour la vérification, précis).

---

## Test 6 : `capturerUtilisateurSauvegarde` — `@Captor` + `captor.capture()` + `getValue()`

```java
@Captor                                                    // @Captor crée un ArgumentCaptor pour intercepter les arguments
private ArgumentCaptor<User> userCaptor;                   // Spécialisé pour le type User

@Test
@DisplayName("ArgumentCaptor : inspecter l'utilisateur sauvegardé")
void capturerUtilisateurSauvegarde() {
 // --- Arrange : configurer les mocks ---
 when(userRepository.existsByEmail(anyString())).thenReturn(false);     // Email disponible
 when(userRepository.save(any(User.class)))                              // any(User.class) : accepte n'importe quel User
 .thenAnswer(inv -> inv.getArgument(0));                                 // thenAnswer() : retourne l'utilisateur reçu
 when(emailService.envoyerEmail(anyString(), anyString(), anyString()))  // anyString() : accepte n'importe quelle chaîne
 .thenReturn(true);                                                      // Envoi d'email réussi

 // --- Act : créer un utilisateur ---
 userService.creerUtilisateur("Charlie", "charlie@test.com");            // Appel de la méthode testée

 // --- Assert : capturer et inspecter l'argument passé à save() ---
 verify(userRepository).save(userCaptor.capture());                      // userCaptor.capture() : intercepte le User passé à save()
 User capture = userCaptor.getValue();                                   // getValue() : récupère l'objet User capturé

 assertEquals("Charlie", capture.getNom());                              // Vérifie le nom de l'utilisateur capturé
 assertEquals("charlie@test.com", capture.getEmail());                   // Vérifie l'email
 assertTrue(capture.isActif());                                           // Vérifie que le compte est actif
 assertNull(capture.getId(), "L'ID doit être null avant la persistance"); // L'ID est null (généré par la base, pas par le service)
}
```

**Arrange :**
- Stubbings classiques pour autoriser la création

**Act :**
- `creerUtilisateur("Charlie", "charlie@test.com")`

**Assert avec capture :**
- `verify(userRepository).save(userCaptor.capture())` : au lieu de `any(User.class)`, on utilise `userCaptor.capture()` qui intercepte l'argument
- `userCaptor.getValue()` : récupère l'objet `User` passé à `save()`
- Assertions sur l'objet capturé : nom, email, actif, ID null

**Ce que ce test valide :** que `UserService` construit correctement l'objet `User` **avant** de le passer au repository. En particulier, l'ID est `null` car `new User(null, nom, email, true)` ne définit pas d'ID — c'est la base de données qui le génère.

---

## Test 7 : `desactiverUtilisateur_succes` — Méthode complémentaire

```java
@Test
@DisplayName("desactiverUtilisateur — gère l'échec d'envoi d'email")
void desactiverUtilisateur_succes() {
 // --- Arrange : préparer l'utilisateur et configurer les mocks ---
 User user = new User(1L, "Dave", "dave@test.com", true);                // Dave, actif, ID=1
 when(userRepository.findById(1L)).thenReturn(user);                     // when().thenReturn() : retourne Dave quand on cherche l'ID 1
 when(userRepository.save(any(User.class))).thenReturn(user);            // save() retourne l'utilisateur sauvegardé
 when(emailService.envoyerEmail(anyString(), anyString(), anyString()))  // anyString() x3 : peu importe les détails de l'email
 .thenReturn(true);                                                      // L'envoi d'email réussit

 // --- Act : désactiver l'utilisateur ---
 userService.desactiverUtilisateur(1L);                                   // Appel de la méthode à tester

 // --- Assert : vérifier l'état et les interactions ---
 assertFalse(user.isActif());                                             // L'utilisateur est bien désactivé (actif = false)
 verify(userRepository).save(user);                                       // verify() : save() a été appelé avec l'utilisateur modifié
 verify(emailService).envoyerEmail(eq("dave@test.com"), anyString(), anyString()); // eq() : email envoyé au bon destinataire
}
```

**Arrange :**
- Un utilisateur existant (Dave, actif)
- `findById(1L)` retourne Dave
- `save()` retourne l'utilisateur
- `envoyerEmail()` retourne `true`

**Act :**
- `desactiverUtilisateur(1L)`

**Assert :**
- `assertFalse(user.isActif())` : l'utilisateur est bien désactivé (le champ `actif` passe à `false`)
- `verify(userRepository).save(user)` : la sauvegarde a été appelée avec l'utilisateur modifié
- `verify(emailService).envoyerEmail(eq("dave@test.com"), ...)` : un email de désactivation a été envoyé

**Ce que ce test valide :** la méthode `desactiverUtilisateur` (1) trouve l'utilisateur, (2) le désactive, (3) le sauvegarde, (4) envoie un email de notification.

---

## Test 8 : `spyDemo` — `@Spy` et `doReturn()`

```java
static class CompteurService {
 public int incrementer() { return 1; }   // Méthode réelle : retourne 1
 public int decrementer() { return -1; }  // Méthode réelle : retourne -1
}

@Spy                                            // @Spy crée un espion : instance réelle, méthodes partiellement remplaçables
private CompteurService compteurSpy;            // Par défaut, toutes les méthodes appellent le VRAI code

@Test
@DisplayName("@Spy : l'objet réel est utilisé, sauf méthodes stubbées")
void spyDemo() {
 // --- Phase 1 : sans stubbing, le spy appelle la vraie méthode ---
 assertEquals(1, compteurSpy.incrementer(),     // Appel réel : la vraie méthode retourne 1
 "Par défaut, le spy appelle la vraie méthode");

 // --- Phase 2 : stubbing avec doReturn().when() pour un spy ---
 doReturn(100).when(compteurSpy).incrementer();  // doReturn().when() : remplace le comportement de incrementer() SANS appeler la vraie méthode

 // --- Phase 3 : vérifier le comportement après stubbing ---
 assertEquals(100, compteurSpy.incrementer(),    // Méthode stubbée : retourne 100 au lieu de 1
 "La méthode stubbée retourne 100");
 assertEquals(-1, compteurSpy.decrementer(),     // Méthode non stubbée : appelle toujours la vraie implémentation
 "La méthode non stubbée appelle la vraie implémentation");
}
```

**Phase 1 — Sans stubbing :**
- `compteurSpy.incrementer()` retourne `1` : la **vraie** méthode `return 1` est exécutée

**Phase 2 — Avec stubbing :**
- `doReturn(100).when(compteurSpy).incrementer()` : on remplace le comportement de `incrementer()` uniquement
- `compteurSpy.incrementer()` retourne maintenant `100` (valeur stubbée)
- `compteurSpy.decrementer()` continue de retourner `-1` (vraie méthode, non stubbée)

**Ce que ce test démontre :** un spy est un **mock partiel** : les méthodes non stubbées exécutent le vrai code, les méthodes stubbées retournent ce qu'on a défini.

---

## Test 9 : `pasDAutresInteractions` — `verifyNoMoreInteractions` et `verifyNoInteractions`

```java
@Test
@DisplayName("verifyNoMoreInteractions : pas d'appels surprise")
void pasDAutresInteractions() {
 // --- Arrange : préparer les données et configurer le mock ---
 User user = new User(1L, "Eve", "eve@test.com", true);    // Eve, active, ID=1
 when(userRepository.findById(1L)).thenReturn(user);       // when().thenReturn() : retourne Eve pour l'ID 1

 // --- Act : exécuter la méthode testée ---
 userService.trouverParId(1L);                              // Chercher l'utilisateur par ID

 // --- Assert : vérifier les interactions précises ---
 verify(userRepository).findById(1L);                       // verify() : findById(1L) a bien été appelé
 verifyNoMoreInteractions(userRepository);                  // verifyNoMoreInteractions() : AUCUNE autre méthode de userRepository n'a été appelée
 verifyNoInteractions(emailService);                        // verifyNoInteractions() : emailService n'a subi AUCUN appel du tout
}
```

**Arrange + Act :**
- Stubbing et appel de `trouverParId(1L)`

**Assert :**
- `verify(userRepository).findById(1L)` : `findById` a bien été appelé
- `verifyNoMoreInteractions(userRepository)` : **aucune autre méthode** de `userRepository` n'a été appelée (pas de `save`, pas de `deleteById`, etc.)
- `verifyNoInteractions(emailService)` : le mock `emailService` n'a subi **aucun appel d'aucune méthode**

**Ce que ce test valide :** l'isolation parfaite de la méthode `trouverParId` — elle n'interagit qu'avec `findById` et ne touche pas aux autres dépendances.

---

## Test 10 : `saveRetourneUtilisateurAvecId` — `thenAnswer` dynamique

```java
@Test
@DisplayName("thenAnswer : l'utilisateur sauvegardé reçoit un ID")
void saveRetourneUtilisateurAvecId() {
 // --- Arrange : configurer les mocks ---
 when(userRepository.existsByEmail(anyString())).thenReturn(false);  // Email disponible
 when(emailService.envoyerEmail(anyString(), anyString(), anyString())).thenReturn(true); // Email envoyé avec succès

 when(userRepository.save(any(User.class))).thenAnswer(invocation -> { // thenAnswer() : comportement dynamique
 User user = invocation.getArgument(0);                                // Récupère le User passé en argument
 user.setId(42L); // Simule l'attribution d'un ID par la base de données (comme JPA/Hibernate)
 return user;     // Retourne l'utilisateur avec son nouvel ID
 });

 // --- Act : créer un utilisateur ---
 User resultat = userService.creerUtilisateur("Frank", "frank@test.com"); // Appel de la méthode testée

 // --- Assert : vérifier que l'ID a bien été attribué ---
 assertEquals(42L, resultat.getId(),                                // L'ID 42 a été attribué par le mock (simulation base de données)
 "Le mock simule l'attribution d'un ID par la base de données");
}
```

**Arrange :**
- `existsByEmail` → `false` (email disponible)
- `envoyerEmail` → `true`
- `save()` avec `thenAnswer` : le mock **modifie** l'objet `User` reçu en lui attribuant l'ID `42L`, puis le retourne. Cela simule exactement ce que ferait JPA/Hibernate : prendre l'entité avec `id=null`, l'insérer en base, et retourner la même instance avec l'ID généré

**Act + Assert :**
- `creerUtilisateur("Frank", "frank@test.com")` retourne un utilisateur dont `getId()` vaut `42L`

**Ce que ce test valide :** l'intégration correcte entre `UserService` et le comportement simulé du repository : le service utilise bien l'objet retourné par `save()` (avec l'ID), et non l'objet original (sans ID).

---

## Test 11 : `ordreDesAppels` — `InOrder`

```java
@Test
@DisplayName("InOrder : l'email est envoyé APRÈS la sauvegarde")
void ordreDesAppels() {
 // --- Arrange : configurer les mocks ---
 when(userRepository.existsByEmail(anyString())).thenReturn(false);  // Email disponible
 when(userRepository.save(any(User.class)))                           // any(User.class) : accepte n'importe quel User
 .thenAnswer(inv -> inv.getArgument(0));                              // thenAnswer() : retourne l'utilisateur reçu
 when(emailService.envoyerEmail(anyString(), anyString(), anyString())) // anyString() : accepte n'importe quelles chaînes
 .thenReturn(true);                                                    // Envoi réussi

 // --- Act : créer un utilisateur ---
 userService.creerUtilisateur("Grace", "grace@test.com");             // Appel de la méthode testée

 // --- Assert : vérifier l'ordre des appels avec InOrder ---
 InOrder ordre = inOrder(userRepository, emailService);               // InOrder : crée un vérificateur d'ordre pour ces deux mocks
 ordre.verify(userRepository).save(any(User.class));                  // 1er appel : save() DOIT avoir eu lieu en premier
 ordre.verify(emailService).envoyerEmail(anyString(), anyString(), anyString()); // 2ème appel : envoyerEmail() DOIT avoir eu lieu en second
}
```

**Arrange + Act :** stubbings standard, appel de `creerUtilisateur`

**Assert :**
- `InOrder ordre = inOrder(userRepository, emailService)` : crée un vérificateur lié à ces deux mocks
- `ordre.verify(userRepository).save(...)` : la sauvegarde doit avoir eu lieu **en premier**
- `ordre.verify(emailService).envoyerEmail(...)` : l'email doit avoir été envoyé **en second**

Si on inversait ces deux lignes, le test échouerait car l'ordre réel ne correspondrait pas.

**Ce que ce test valide :** une règle métier implicite — on sauvegarde **avant** d'envoyer l'email. Si l'email partait avant la sauvegarde et que la sauvegarde échouait, l'utilisateur recevrait un email de bienvenue pour un compte inexistant.

---

## PARTIE 3 -- LAB (45 min)

## Objectif

Créer un `NotificationService` qui dépend d'un `SmsService` (interface). Vous allez implémenter les deux interfaces/classes et écrire les tests mockés.

## Contexte

Une application a besoin d'envoyer des notifications à ses utilisateurs. Une notification combine un SMS et éventuellement un email. Votre mission est de créer les briques manquantes en utilisant les techniques de mocking vues dans ce module.

## Consignes

### Étape 1 — Créer l'interface `SmsService`

Créez le fichier `src/main/java/com/nexa/mocking/SmsService.java` :

```java
package com.nexa.mocking;

public interface SmsService {
 boolean envoyerSms(String numero, String message); // Méthode à mocker dans les tests : envoie un SMS et retourne true/false
}
```

Cette interface représente le service d'envoi de SMS (comme Twilio ou AWS SNS dans la vraie vie). La méthode `envoyerSms` prend un numéro de téléphone et un message, retourne `true` si l'envoi a réussi.

### Étape 2 — Créer la classe `NotificationService`

Créez le fichier `src/main/java/com/nexa/mocking/NotificationService.java` :

```java
package com.nexa.mocking;

public class NotificationService {

 private final SmsService smsService;              // Dépendance à mocker avec @Mock dans les tests

 public NotificationService(SmsService smsService) { // Constructeur : Injection de dépendance (sera utilisé par @InjectMocks)
 this.smsService = smsService;
 }

 public boolean envoyerNotification(String telephone, String message) {
 // TODO : à implémenter
 return false;
 }

 public boolean envoyerNotificationUrgente(String telephone, String message) {
 // TODO : à implémenter
 return false;
 }
}
```

### Étape 3 — Implémenter les méthodes

**`envoyerNotification(String telephone, String message)`** :
- Si le numéro est null ou vide, lever `IllegalArgumentException` avec un message explicite
- Si le message est null ou vide, lever `IllegalArgumentException`
- Appeler `smsService.envoyerSms(telephone, message)` et retourner son résultat

**`envoyerNotificationUrgente(String telephone, String message)`** :
- Appeler `envoyerNotification` mais préfixer le message par `"[URGENT] "`
- Exemple : `envoyerNotificationUrgente("0612345678", "Rapport")` appelle `smsService.envoyerSms("0612345678", "[URGENT] Rapport")`

### Étape 4 — Écrire les tests `NotificationServiceTest`

Créez le fichier `src/test/java/com/nexa/mocking/NotificationServiceTest.java` avec les tests suivants :

1. **`envoyerNotification_succes`** : quand le SMS est envoyé avec succès, `smsService.envoyerSms` est appelé avec les bons arguments, et la méthode retourne `true`
2. **`envoyerNotification_echec`** : quand `smsService.envoyerSms` retourne `false`, la méthode `envoyerNotification` retourne `false`
3. **`envoyerNotification_telephoneNull`** : un numéro null lève `IllegalArgumentException` et le SMS n'est jamais envoyé (`verify` + `never()`)
4. **`envoyerNotification_messageVide`** : un message vide lève `IllegalArgumentException`
5. **`envoyerNotificationUrgente_prefixe`** : utiliser `@Captor` pour capturer le message passé à `smsService.envoyerSms` et vérifier qu'il commence par `"[URGENT] "`
6. **`envoyerNotificationUrgente_appelleEnvoyerNotification`** : si vous utilisez un spy sur `NotificationService`, vérifiez que `envoyerNotificationUrgente` appelle bien `envoyerNotification` (optionnel si le temps le permet)

### Étape 5 — Exécuter les tests

```bash
mvn test
```

## Critères de réussite

- [ ] Tous les tests passent au vert
- [ ] `verify()` est utilisé pour vérifier les appels à `smsService`
- [ ] `@Captor` est utilisé pour au moins un test
- [ ] `never()` est utilisé pour vérifier qu'aucun SMS n'est envoyé en cas d'erreur
- [ ] `@Mock` et `@InjectMocks` sont correctement utilisés
- [ ] `assertThrows` est utilisé pour les cas d'erreur

---

## FICHE MEMO -- Annotations et methodes Mockito

| Annotation / Méthode | Rôle | Syntaxe clé |
|----------------------|------|-------------|
| `@ExtendWith(MockitoExtension.class)` | Active l'intégration Mockito + JUnit 5 | Sur la classe de test |
| `@Mock` | Crée un simulacre (proxy sans comportement réel) | `@Mock private UserRepository repo;` |
| `@InjectMocks` | Crée l'instance réelle et injecte les `@Mock` | `@InjectMocks private UserService service;` |
| `@Spy` | Crée un espion (objet réel, méthodes partiellement remplaçables) | `@Spy private CompteurService spy;` |
| `@Captor` | Crée un capteur d'arguments pour `verify()` | `@Captor private ArgumentCaptor<User> captor;` |

| Méthode de stubbing | Usage |
|--------------------|-------|
| `when(mock.methode()).thenReturn(valeur)` | Retourner une valeur fixe |
| `when(mock.methode()).thenThrow(exception)` | Lever une exception |
| `when(mock.methode()).thenAnswer(inv -> { ... })` | Comportement dynamique (modifier l'argument, calculer le retour) |
| `doReturn(valeur).when(spy).methode()` | Stubbing de spy sans appeler la vraie méthode |
| `doThrow(ex).when(mock).methodeVoid()` | Stubbing d'une méthode void pour lever une exception |

| Méthode de vérification | Usage |
|-------------------------|-------|
| `verify(mock).methode()` | Appelée exactement 1 fois |
| `verify(mock, times(n)).methode()` | Appelée exactement n fois |
| `verify(mock, never()).methode()` | Jamais appelée |
| `verify(mock, atLeast(n)).methode()` | Au moins n fois |
| `verify(mock, atMost(n)).methode()` | Au plus n fois |
| `verifyNoMoreInteractions(mock)` | Aucun autre appel sur ce mock |
| `verifyNoInteractions(mock)` | Aucun appel du tout sur ce mock |
| `InOrder ordre = inOrder(m1, m2)` | Vérifier l'ordre des appels |

| Matcher | Usage |
|---------|-------|
| `any()` | N'importe quelle valeur (objet ou primitif) |
| `any(Class)` | N'importe quelle instance du type |
| `anyString()`, `anyInt()`, `anyLong()` | N'importe quelle valeur du type primitif |
| `eq(valeur)` | Valeur exacte (pour mélanger avec des matchers) |

| Méthode de capture | Usage |
|--------------------|-------|
| `captor.capture()` | Intercepter l'argument dans `verify()` |
| `captor.getValue()` | Récupérer la valeur capturée |
| `captor.getAllValues()` | Récupérer toutes les valeurs (appels multiples) |

> **Règle d'or :** si un argument utilise un matcher (`any()`, `anyString()`...), **tous** les arguments de cet appel doivent utiliser des matchers. Utiliser `eq()` pour les valeurs exactes dans ce contexte.
