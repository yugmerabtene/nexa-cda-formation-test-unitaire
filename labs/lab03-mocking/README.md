# Lab 03 : Mocking avec Mockito

**Objectif :** Maitriser l'isolation des tests avec les mocks Mockito : `@Mock`, `@InjectMocks`, `@Spy`, `when/thenReturn`, `verify`, `ArgumentCaptor`, `InOrder`.

**Duree :** 45 minutes

---

## Énoncé

Vous devez implementer un `UserService` qui gère des utilisateurs, et écrire une suite de tests avec Mockito en mockant les dépendances `UserRepository` et `EmailService`.

### Classes a implementer

| Classe | Role |
|---|---|
| `User` | Entite avec id, nom, email, actif |
| `UserRepository` | Interface d'accès aux donnees (mockee dans les tests) |
| `EmailService` | Interface d'envoi d'emails (mockee dans les tests) |
| `UserNotFoundException` | Exception levee quand un utilisateur est introuvable |
| `UserService` | Service metier avec 3 méthodes : `trouverParId`, `creerUtilisateur`, `desactiverUtilisateur` |

### Methodes du UserService

| Methode | Comportement |
|---|---|
| `trouverParId(Long id)` | Retourne l'utilisateur ou lève `UserNotFoundException` |
| `creerUtilisateur(String nom, String email)` | Verifie l'unicite de l'email, sauvegarde, envoie un email de bienvenue |
| `desactiverUtilisateur(Long id)` | Trouve l'utilisateur, le désactivé, sauvegarde, envoie un email |

### Tests a écrire (minimum)

1. `trouverParId` — utilisateur existant (succès)
2. `trouverParId` — utilisateur inexistant (exception)
3. `creerUtilisateur` — succès (save + email)
4. `creerUtilisateur` — email déjà utilisé (exception)
5. `ArgumentCaptor` — capturer l'utilisateur sauvegarde
6. `@Spy` — mock partiel avec `doReturn().when()`
7. `verifyNoMoreInteractions` — pas d'appels surprise
8. `thenAnswer` — simulation d'ID genere par la base
9. `InOrder` — verification de l'ordre des appels

---

## Prérequis

- Lab 01 et 02 termines
- Maven avec dépendance Mockito (`mockito-core` + `mockito-junit-jupiter`)

---

## Étape par étape

### Étape 1 : Structure du projet

```
lab03-mocking/
  pom.xml
  src/
    main/java/com/nexa/mocking/
      User.java
      UserRepository.java  (interface)
      EmailService.java    (interface)
      UserNotFoundException.java
      UserService.java
    test/java/com/nexa/mocking/
      UserServiceTest.java
```

### Étape 2 : Creer les interfaces et l'entité

Creez `UserRepository` (interface avec `findById`, `save`, `existsByEmail`, etc.) et `EmailService` (interface avec `envoyerEmail`). Ces interfaces seront mockees.

L'entité `User` a 4 champs : id, nom, email, actif (boolean).

### Étape 3 : Implementer UserService

Le `UserService` reçoit ses dépendances par injection via le constructeur :

```java
public class UserService {
    private final UserRepository userRepository;
    private final EmailService emailService;

    public UserService(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }
}
```

**`trouverParId(Long id)` :**
1. Appelle `userRepository.findById(id)`
2. Si `null` : lève `UserNotFoundException`
3. Sinon : retourne l'utilisateur

**`creerUtilisateur(String nom, String email)` :**
1. Verifie `userRepository.existsByEmail(email)` — si true, lève `IllegalArgumentException`
2. Cree un nouvel `User(nom, email, actif=true)`
3. Appelle `userRepository.save(user)`
4. Appelle `emailService.envoyerEmail(email, sujet, contenu)`
5. Retourne l'utilisateur sauvegarde

### Étape 4 : Configurer le test avec Mockito

```java
@ExtendWith(MockitoExtension.class)  // Active les annotations Mockito
class UserServiceTest {

    @Mock                   // Cree un mock de l'interface
    private UserRepository userRepository;

    @Mock                   // Cree un mock de l'interface
    private EmailService emailService;

    @InjectMocks            // Injecte les mocks dans userService
    private UserService userService;
```

### Étape 5 : Stubbing avec when().thenReturn()

```java
// Configurer le mock pour retourner une valeur spécifique
when(userRepository.findById(1L)).thenReturn(userAttendu);

// Configurer le mock pour retourner null (utilisateur inexistant)
when(userRepository.findById(99L)).thenReturn(null);

// Matchers : any() accepte n'importe quelle valeur
when(userRepository.save(any(User.class))).thenReturn(savedUser);
```

### Étape 6 : Verification avec verify()

```java
// Verifier qu'une méthode a ete appelé́e
verify(userRepository).save(any(User.class));

// Verifier qu'une méthode n'a JAMAIS ete appelé́e
verify(userRepository, never()).save(any(User.class));

// Verifier qu'aucune autre interaction n'a eu lieu
verifyNoMoreInteractions(userRepository);
```

### Étape 7 : ArgumentCaptor

Capture l'objet passe a un mock pour l'inspecter :

```java
@Captor
private ArgumentCaptor<User> userCaptor;

@Test
void capturerArgument() {
    userService.creerUtilisateur("Bob", "bob@test.com");
    verify(userRepository).save(userCaptor.capture());
    User capture = userCaptor.getValue();
    assertEquals("Bob", capture.getNom());
}
```

### Étape 8 : @Spy — mock partiel

Un spy appelle les vraies méthodes par defaut. On peut stubber certaines méthodes :

```java
@Spy
private CompteurService compteurSpy;

@Test
void spyDemo() {
    // Appelle la vraie méthode
    assertEquals(1, compteurSpy.incrementer());

    // Stubbe la méthode pour ce test
    doReturn(100).when(compteurSpy).incrementer();
    assertEquals(100, compteurSpy.incrementer());

    // Les méthodes non stubbees utilisent la vraie implementation
    assertEquals(-1, compteurSpy.decrementer());
}
```

> **doReturn().when()** est utilisé avec les spies (au lieu de `when().thenReturn()`) car `when()` appellerait la vraie méthode avant de la stubber.

### Étape 9 : InOrder — verification de l'ordre

```java
InOrder ordre = inOrder(userRepository, emailService);
ordre.verify(userRepository).save(any(User.class));
ordre.verify(emailService).envoyerEmail(anyString(), anyString(), anyString());
```

---

## Exécution

```bash
cd labs/lab03-mocking
mvn clean test
```

## Criteres de réussite

- Tous les mocks sont correctement configures (`@Mock`, `@InjectMocks`)
- Les tests couvrent les cas de succès ET d'erreur
- `verify()` est utilisé au moins 3 fois
- `ArgumentCaptor` est utilisé au moins 1 fois
- `@Spy` est utilisé pour demontrer le mock partiel
- `InOrder` vérifié l'ordre des appels
- `verifyNoMoreInteractions` est utilisé
