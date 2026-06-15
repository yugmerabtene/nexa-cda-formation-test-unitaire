# Lab 03 : Mocking avec Mockito

**Objectif :** Maitriser l'isolation des tests avec les mocks Mockito : `@Mock`, `@InjectMocks`, `@Spy`, `when/thenReturn`, `verify`, `ArgumentCaptor`, `InOrder`.

**Duree :** 45 minutes

---

## Enonce

Vous devez implementer un `UserService` qui gere des utilisateurs, et ecrire une suite de tests avec Mockito en mockant les dependances `UserRepository` et `EmailService`.

### Classes a implementer

| Classe | Role |
|---|---|
| `User` | Entite avec id, nom, email, actif |
| `UserRepository` | Interface d'acces aux donnees (mockee dans les tests) |
| `EmailService` | Interface d'envoi d'emails (mockee dans les tests) |
| `UserNotFoundException` | Exception levee quand un utilisateur est introuvable |
| `UserService` | Service metier avec 3 methodes : `trouverParId`, `creerUtilisateur`, `desactiverUtilisateur` |

### Methodes du UserService

| Methode | Comportement |
|---|---|
| `trouverParId(Long id)` | Retourne l'utilisateur ou leve `UserNotFoundException` |
| `creerUtilisateur(String nom, String email)` | Verifie l'unicite de l'email, sauvegarde, envoie un email de bienvenue |
| `desactiverUtilisateur(Long id)` | Trouve l'utilisateur, le desactive, sauvegarde, envoie un email |

### Tests a ecrire (minimum)

1. `trouverParId` — utilisateur existant (succes)
2. `trouverParId` — utilisateur inexistant (exception)
3. `creerUtilisateur` — succes (save + email)
4. `creerUtilisateur` — email deja utilise (exception)
5. `ArgumentCaptor` — capturer l'utilisateur sauvegarde
6. `@Spy` — mock partiel avec `doReturn().when()`
7. `verifyNoMoreInteractions` — pas d'appels surprise
8. `thenAnswer` — simulation d'ID genere par la base
9. `InOrder` — verification de l'ordre des appels

---

## Prerequis

- Lab 01 et 02 termines
- Maven avec dependance Mockito (`mockito-core` + `mockito-junit-jupiter`)

---

## Etape par etape

### Etape 1 : Structure du projet

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

### Etape 2 : Creer les interfaces et l'entite

Creez `UserRepository` (interface avec `findById`, `save`, `existsByEmail`, etc.) et `EmailService` (interface avec `envoyerEmail`). Ces interfaces seront mockees.

L'entite `User` a 4 champs : id, nom, email, actif (boolean).

### Etape 3 : Implementer UserService

Le `UserService` recoit ses dependances par injection via le constructeur :

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
2. Si `null` : leve `UserNotFoundException`
3. Sinon : retourne l'utilisateur

**`creerUtilisateur(String nom, String email)` :**
1. Verifie `userRepository.existsByEmail(email)` — si true, leve `IllegalArgumentException`
2. Cree un nouvel `User(nom, email, actif=true)`
3. Appelle `userRepository.save(user)`
4. Appelle `emailService.envoyerEmail(email, sujet, contenu)`
5. Retourne l'utilisateur sauvegarde

### Etape 4 : Configurer le test avec Mockito

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

### Etape 5 : Stubbing avec when().thenReturn()

```java
// Configurer le mock pour retourner une valeur specifique
when(userRepository.findById(1L)).thenReturn(userAttendu);

// Configurer le mock pour retourner null (utilisateur inexistant)
when(userRepository.findById(99L)).thenReturn(null);

// Matchers : any() accepte n'importe quelle valeur
when(userRepository.save(any(User.class))).thenReturn(savedUser);
```

### Etape 6 : Verification avec verify()

```java
// Verifier qu'une methode a ete appelee
verify(userRepository).save(any(User.class));

// Verifier qu'une methode n'a JAMAIS ete appelee
verify(userRepository, never()).save(any(User.class));

// Verifier qu'aucune autre interaction n'a eu lieu
verifyNoMoreInteractions(userRepository);
```

### Etape 7 : ArgumentCaptor

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

### Etape 8 : @Spy — mock partiel

Un spy appelle les vraies methodes par defaut. On peut stubber certaines methodes :

```java
@Spy
private CompteurService compteurSpy;

@Test
void spyDemo() {
    // Appelle la vraie methode
    assertEquals(1, compteurSpy.incrementer());

    // Stubbe la methode pour ce test
    doReturn(100).when(compteurSpy).incrementer();
    assertEquals(100, compteurSpy.incrementer());

    // Les methodes non stubbees utilisent la vraie implementation
    assertEquals(-1, compteurSpy.decrementer());
}
```

> **doReturn().when()** est utilise avec les spies (au lieu de `when().thenReturn()`) car `when()` appellerait la vraie methode avant de la stubber.

### Etape 9 : InOrder — verification de l'ordre

```java
InOrder ordre = inOrder(userRepository, emailService);
ordre.verify(userRepository).save(any(User.class));
ordre.verify(emailService).envoyerEmail(anyString(), anyString(), anyString());
```

---

## Execution

```bash
cd labs/lab03-mocking
mvn clean test
```

## Criteres de reussite

- Tous les mocks sont correctement configures (`@Mock`, `@InjectMocks`)
- Les tests couvrent les cas de succes ET d'erreur
- `verify()` est utilise au moins 3 fois
- `ArgumentCaptor` est utilise au moins 1 fois
- `@Spy` est utilise pour demontrer le mock partiel
- `InOrder` verifie l'ordre des appels
- `verifyNoMoreInteractions` est utilise
