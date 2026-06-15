# Lab08 - Application de Gestion d'Utilisateurs

## Objectif

Developper une API REST complete de gestion d'utilisateurs avec Spring Boot, integrant l'authentification JWT, la gestion des roles (RBAC), la pagination, la validation des donnees, et la gestion centralisee des exceptions. L'application permet de creer, lire, mettre a jour et supprimer des utilisateurs de maniere securisee.

## Enonce

Vous devez realiser une API REST de gestion d'utilisateurs (CRUD) avec les fonctionnalites suivantes :

- Authentification par JWT (JSON Web Token).
- Controle d'acces base sur les roles (ADMIN et USER).
- Operations CRUD completes sur les utilisateurs.
- Pagination des resultats de recherche.
- Validation des donnees d'entree avec Jakarta Bean Validation.
- Gestion centralisee des exceptions avec des messages d'erreur structures (RFC 7807).
- Hachage des mots de passe avec BCrypt.
- Initialisation automatique d'utilisateurs par defaut au demarrage.
- Persistance en base de donnees PostgreSQL (H2 en test).

L'application expose les endpoints suivants :

| Methode | Endpoint                  | Roles            | Action                          |
|---------|---------------------------|------------------|---------------------------------|
| POST    | `/api/auth/login`         | Public           | Authentification, obtention JWT |
| GET     | `/api/users`              | ADMIN, USER      | Lister tous les utilisateurs    |
| GET     | `/api/users/page`         | ADMIN, USER      | Lister avec pagination          |
| GET     | `/api/users/{id}`         | ADMIN, USER      | Trouver un utilisateur par ID   |
| POST    | `/api/users`              | ADMIN            | Creer un utilisateur            |
| PUT     | `/api/users/{id}`         | ADMIN            | Mettre a jour un utilisateur    |
| DELETE  | `/api/users/{id}`         | ADMIN            | Supprimer un utilisateur        |
| GET     | `/api/users/recherche`    | ADMIN, USER      | Rechercher par nom              |
| GET     | `/api/users/actifs`       | ADMIN, USER      | Lister les utilisateurs actifs  |
| GET     | `/actuator/health`        | Public           | Health check                    |

## Prerequis

- Java 17 ou superieur.
- Maven 3.8 ou superieur.
- PostgreSQL (pour l'execution en production ; H2 est utilise pour les tests).
- Connaissance de base de Spring Boot, Spring Security et JPA.

## Deroule etapes

### Etape 1 : Creation du projet et configuration Maven

Creer un projet Spring Boot avec les dependances suivantes dans le fichier `pom.xml` :
- `spring-boot-starter-web` : pour les controleurs REST.
- `spring-boot-starter-security` : pour l'authentification et l'autorisation.
- `spring-boot-starter-data-jpa` : pour la persistance avec JPA/Hibernate.
- `spring-boot-starter-validation` : pour la validation des donnees.
- `spring-boot-starter-actuator` : pour les endpoints de monitoring.
- `postgresql` : driver PostgreSQL.
- `jjwt-api`, `jjwt-impl`, `jjwt-jackson` : bibliotheque JWT (io.jsonwebtoken).
- `h2` : base de donnees en memoire pour les tests.
- `spring-boot-starter-test`, `spring-security-test`, `testcontainers`, `rest-assured` : frameworks de test.
- `jacoco-maven-plugin` : couverture de code (minimum 80% lignes, 70% branches).
- `pitest-maven` : tests de mutation.
- `owasp/dependency-check-maven` : analyse des vulnerabilites des dependances.

### Etape 2 : Creation de l'entite JPA

Creer la classe `User` dans le package `entity` avec les annotations JPA :
- `@Entity`, `@Table(name = "users")` pour le mapping.
- `@Id` avec `@GeneratedValue(strategy = GenerationType.IDENTITY)` pour la cle primaire auto-generee.
- Champs : `nom`, `prenom`, `email` (unique), `password`, `role` (enum USER/ADMIN), `actif`, `dateCreation`, `dateModification`.
- Contraintes de validation avec `@NotBlank`, `@Size`, `@Email`, `@NotNull`.
- Enum interne `Role` avec les valeurs `USER` et `ADMIN`.

### Etape 3 : Creation des DTOs

Creer les objets de transfert de donnees dans le package `dto` :
- `UserRequest` : DTO d'entree avec validation (`@NotBlank`, `@Size`, `@Email`, `@NotNull`) pour les champs nom, prenom, email, password, role.
- `UserResponse` : DTO de sortie sans le mot de passe, avec methode statique `from(User)` pour la conversion entite vers DTO.
- `ErrorResponse` : DTO pour les reponses d'erreur conforme au standard RFC 7807 (Problem Details), avec champs status, title, detail, errors.

### Etape 4 : Creation du repository et du service

Creer l'interface `UserRepository` dans le package `repository` qui etend `JpaRepository<User, Long>` avec les methodes de requete derivees :
- `findByEmail` : recherche par email.
- `existsByEmail` : verification d'existence par email.
- `findByNomContainingIgnoreCase` : recherche insensible a la casse par nom.
- `findByActif` : filtrage par statut actif/inactif.
- `findByRole` avec `Pageable` : recherche paginee par role.
- `countByRole` : comptage par role.

Creer la classe `UserService` dans le package `service` avec les methodes metier :
- `creer(User)` : creation avec hachage BCrypt du mot de passe et verification d'unicite de l'email.
- `listerTous()` : liste complete.
- `listerPagine(Pageable)` : liste paginee.
- `trouverParId(Long)` : recherche par ID, leve `ResourceNotFoundException` si absent.
- `trouverParEmail(String)` : recherche par email.
- `mettreAJour(Long, User)` : mise a jour partielle, ne modifie le mot de passe que s'il est fourni.
- `supprimer(Long)` : suppression avec verification d'existence.
- `desactiver(Long)` : desactivation logique.
- `rechercherParNom(String)` : recherche par nom partiel.
- `listerActifs()` : liste des utilisateurs actifs.
- `compterParRole(Role)` : comptage par role.

### Etape 5 : Securite - JWT et Spring Security

Creer la classe `JwtUtil` dans le package `security` :
- Generer des tokens JWT signes avec HMAC-SHA256.
- Extraire l'email (subject) et le role (claim) d'un token.
- Valider un token (verification de signature et d'expiration).
- Cle secrete de 256 bits minimum, expiration a 1 heure (3600000 ms).

Creer la classe `JwtFilter` qui etend `OncePerRequestFilter` :
- Intercepter chaque requete HTTP.
- Extraire le token du header `Authorization: Bearer <token>`.
- Valider le token et definir le contexte d'authentification Spring Security avec le role.

Configurer Spring Security dans `SecurityConfig` :
- Desactiver CSRF (API stateless).
- Politique de session `STATELESS`.
- Proteger les endpoints : `/api/auth/**` et `/actuator/health` publics, GET accessible a USER et ADMIN, POST/PUT/DELETE reserve a ADMIN.
- Ajouter `JwtFilter` avant `UsernamePasswordAuthenticationFilter`.
- Configurer `BCryptPasswordEncoder` et `AuthenticationManager`.

### Etape 6 : Controleur REST et gestion des exceptions

Creer la classe `UserController` dans le package `controller` :
- `POST /api/auth/login` : authentification par email/mot de passe, retourne un token JWT.
- `GET /api/users` : liste tous les utilisateurs (ADMIN, USER).
- `GET /api/users/page` : liste paginee (ADMIN, USER).
- `GET /api/users/{id}` : detail d'un utilisateur (ADMIN, USER).
- `POST /api/users` : creation (ADMIN uniquement), retourne 201 Created.
- `PUT /api/users/{id}` : mise a jour (ADMIN uniquement).
- `DELETE /api/users/{id}` : suppression (ADMIN uniquement), retourne 204 No Content.
- `GET /api/users/recherche?nom=...` : recherche par nom (ADMIN, USER).
- `GET /api/users/actifs` : utilisateurs actifs (ADMIN, USER).

Creer les exceptions personnalisees dans le package `exception` :
- `ResourceNotFoundException` : pour les ressources non trouvees (HTTP 404).
- `BusinessException` : pour les conflits metier (HTTP 409).

Creer `GlobalExceptionHandler` avec `@RestControllerAdvice` :
- `ResourceNotFoundException` -> 404.
- `BusinessException` -> 409.
- `MethodArgumentNotValidException` -> 400 avec details par champ.
- `IllegalArgumentException` -> 400.

### Etape 7 : Configuration et initialisation

Creer `AppConfig` dans le package `config` :
- Definir le bean `UserDetailsService` qui charge un utilisateur par email depuis le repository.
- Definir le bean `CommandLineRunner` qui initialise deux utilisateurs par defaut au demarrage si la base est vide : `admin@nexa.fr` (role ADMIN, mot de passe `admin123`) et `user@nexa.fr` (role USER, mot de passe `user123`).

### Etape 8 : Tests unitaires et d'integration

Ecrire les tests pour chaque couche de l'application :

- **Tests d'entite** (`UserEntityTest`) : constructeurs, setters/getters, enum Role.
- **Tests DTO** (`UserRequestTest`, `UserResponseTest`, `ErrorResponseTest`) : conversion, validation.
- **Tests repository** (`UserRepositoryTest`) : toutes les methodes de requete derivees, unicite email, CRUD de base avec `@DataJpaTest`.
- **Tests service** (`UserServiceTest`) : toutes les methodes metier avec Mockito, cas d'erreur, cas nominaux.
- **Tests controleur** (`UserControllerTest`) : tous les endpoints avec `@WebMvcTest`, `MockMvc`, `@WithMockUser`.
- **Tests securite** (`SecurityTests`) : acces anonyme, acces par role, refus d'acces.
- **Tests JWT** (`JwtUtilTest`) : generation, extraction, validation, cas d'erreur.
- **Tests exceptions** (`ExceptionsTest`, `GlobalExceptionHandlerTest`) : messages, codes HTTP.

## Execution

### Compilation et tests

```bash
cd labs/lab08-user-manager
mvn clean test
```

### Execution de l'application

```bash
mvn spring-boot:run
```

L'application demarre sur le port 8080 par defaut.

### Verification de la couverture de code

```bash
mvn jacoco:report
```

Le rapport est genere dans `target/site/jacoco/index.html`.

### Tests de mutation

```bash
mvn pitest:mutationCoverage
```

## Criteres de reussite

1. **Compilation sans erreur** : le projet compile avec `mvn compile` sans aucune erreur.
2. **Tous les tests passent** : les 74 tests s'executent avec succes (`mvn test`).
3. **Couverture de code** : minimum 80% de couverture de lignes et 70% de couverture de branches (verifie par JaCoCo).
4. **API fonctionnelle** : tous les endpoints CRUD repondent correctement avec les bons codes HTTP.
5. **Securite** : l'authentification JWT fonctionne, les roles sont respectes, les acces non autorises retournent 403.
6. **Validation** : les donnees invalides sont rejetees avec un code 400 et des messages d'erreur precis.
7. **Exceptions** : les erreurs metier et les ressources non trouvees sont gerees proprement avec des messages structures.
8. **Mots de passe** : les mots de passe sont hashes avec BCrypt et jamais exposes dans les reponses API.
9. **Initialisation** : les utilisateurs par defaut sont crees automatiquement au premier demarrage.
10. **Absence de regressions** : le code modifie ne doit pas alterer le comportement existant ; seuls des commentaires sont ajoutes.
