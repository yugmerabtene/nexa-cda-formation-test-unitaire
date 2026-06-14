# Syllabus : Tests Unitaires et Sécurité des Applications Java

**École Nexa — Formation intensive 2 jours (14 heures)**

---

## Objectifs pédagogiques

A l'issue de cette formation, vous serez capable de :

1. **Concevoir et rédiger** des tests unitaires de qualité avec JUnit 5 et Mockito
2. **Appliquer** la méthodologie TDD (Test-Driven Development) en Java
3. **Identifier** les vulnérabilités de sécurité selon l'OWASP Top 10
4. **Sécuriser** une application Spring Boot avec Spring Security et JWT
5. **Intégrer** les tests et l'analyse de sécurité dans un pipeline CI/CD
6. **Utiliser** les outils d'analyse statique (SonarQube) et de scan de dépendances (OWASP Dependency-Check)

---

## Prérequis

- Java 11+ (Java 17 recommandé)
- Notions de Maven (pom.xml, cycle de vie, dépendances)
- Bases de Spring Boot (optionnel, introduction progressive)
- Docker et Docker Compose installés
- Git

---

## Environnement technique

| Outil | Version | Usage |
|---|---|---|
| **Java** | 17 (Temurin) | Langage de développement |
| **Maven** | 3.9+ | Build, tests, rapports |
| **JUnit 5** | 5.10+ | Framework de tests unitaires |
| **Mockito** | 5.7+ | Framework de mocking |
| **JaCoCo** | 0.8.11 | Couverture de code |
| **PITest** | 1.15+ | Tests de mutation |
| **Spring Boot** | 3.2+ | Framework applicatif |
| **Spring Security** | 6.2+ | Sécurité applicative |
| **Testcontainers** | 1.19+ | Tests d'intégration avec containers |
| **SonarQube** | 10.5 Community | Analyse statique de code |
| **OWASP Dependency-Check** | 9.0+ | Scan de vulnérabilités des dépendances |
| **ZAP** | 2.15+ | Test d'intrusion automatisé |
| **Docker** | 24+ | Conteneurisation des labs |

---

## Déroulé détaillé

---

### **Jour 1 — Tests Unitaires en Java (9h00–17h00)**

---

#### **Module 1 — Fondamentaux des Tests Unitaires (9h00–10h30)**

**Objectif :** Comprendre le pourquoi du test, maîtriser la syntaxe JUnit 5 de base.

**Contenu théorique (45 min) :**
- La pyramide des tests : unitaire → intégration → end-to-end
- Anatomie d'un test : Arrange, Act, Assert (AAA)
- Présentation de JUnit 5 : architecture (Jupiter, Vintage, Platform)
- L'annotation `@Test` : signature, visibilité, valeur de retour `void`
- Les assertions fondamentales :
 - `assertEquals(expected, actual)` — égalité par `equals()`
 - `assertTrue(condition)` / `assertFalse(condition)` — conditions booléennes
 - `assertNull(value)` / `assertNotNull(value)` — test de nullité
 - `assertSame(expected, actual)` — égalité par référence (`==`)
 - `assertThrows(Class<T>, Executable)` — validation d'exception
 - `assertAll(Executable...)` — exécution groupée (toutes les assertions s'exécutent même si l'une échoue)

**TP Lab01 — Fondamentaux (45 min) :**
- Implémenter et tester une classe `Calculatrice`
- Écrire les tests pour : addition, soustraction, multiplication, division (y compris division par zéro → exception)
- Utiliser les messages d'assertion (`assertEquals(expected, actual, "message")`)

**Annotations couvertes :** `@Test`, `@DisplayName`
**Méthodes :** `assertEquals`, `assertThrows`, `assertAll`, `assertTrue`, `assertFalse`, `assertNull`, `assertNotNull`

---

#### **Module 2 — Cycle de vie et Tests Paramétrés (10h45–12h30)**

**Objectif :** Maîtriser le cycle de vie des tests JUnit 5 et la paramétrisation.

**Contenu théorique (45 min) :**
- Le cycle de vie JUnit 5 :
 - `@BeforeAll` (static) — exécuté **une fois** avant **tous** les tests de la classe
 - `@BeforeEach` — exécuté **avant chaque** test
 - `@AfterEach` — exécuté **après chaque** test
 - `@AfterAll` (static) — exécuté **une fois** après **tous** les tests de la classe
 - L'ordre d'exécution : `@BeforeAll` → (`@BeforeEach` → `@Test` → `@AfterEach`) × N → `@AfterAll`
- Tests paramétrés :
 - `@ParameterizedTest` — remplace `@Test` pour les tests paramétrés
 - `@ValueSource(ints = {1, 2, 3})` — source de valeurs simples
 - `@CsvSource({"1, 2, 3", "4, 5, 9"})` — tuples de paramètres
 - `@CsvFileSource(resources = "/test-data.csv")` — fichier CSV
 - `@EnumSource(MyEnum.class)` — toutes les valeurs d'un enum
 - `@MethodSource("nomMethode")` — méthode fournissant les arguments
 - `@NullSource`, `@EmptySource`, `@NullAndEmptySource` — valeurs nulles/vides

**TP Lab02 — Paramétrés avancés (45 min) :**
- Implémenter un validateur d'email, de numéro de téléphone
- Utiliser `@CsvSource` pour tester des dizaines de cas
- Valider les cas limites : null, vide, caractères spéciaux

**Annotations couvertes :** `@BeforeAll`, `@BeforeEach`, `@AfterEach`, `@AfterAll`, `@ParameterizedTest`, `@ValueSource`, `@CsvSource`, `@CsvFileSource`, `@EnumSource`, `@MethodSource`, `@NullSource`, `@EmptySource`, `@NullAndEmptySource`

---

#### **Module 3 — Mocking avec Mockito (13h30–15h00)**

**Objectif :** Isoler le code testé de ses dépendances avec des mocks.

**Contenu théorique (45 min) :**
- Pourquoi mocker ? Isolement, rapidité, contrôle, simulation d'erreurs
- Mock vs Stub vs Spy vs Fake vs Dummy
- Mockito Core :
 - `MockitoExtension.class` — extension JUnit 5 pour Mockito
 - `@Mock` — crée une instance mockée de la classe/interface
 - `@InjectMocks` — injecte les mocks dans l'objet testé
 - `when(mock.methode()).thenReturn(valeur)` — **stubbing** : définit le comportement attendu
 - `verify(mock).methode()` — **vérification** : vérifie qu'une méthode a été appelée
 - `verify(mock, times(n)).methode()` — vérifie le nombre d'appels
 - `verify(mock, never()).methode()` — vérifie que la méthode n'a **jamais** été appelée
 - `ArgumentCaptor<T>` — capture les arguments passés à un mock
 - `@Spy` — mock partiel (l'objet réel est utilisé sauf pour les méthodes stubbées)
 - `doThrow(exception).when(mock).methode()` — simule une exception

**TP Lab03 — Mocking (45 min) :**
- Implémenter `UserService` qui dépend de `UserRepository` et `EmailService`
- Mocker le repository et le service email
- Tester les cas : succès, utilisateur non trouvé, échec d'envoi d'email
- Vérifier les appels avec `verify()`

**Annotations couvertes :** `@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`, `@Spy`, `@Captor`

---

#### **Module 4 — TDD et Couverture de code (15h15–17h00)**

**Objectif :** Appliquer le TDD (Test-Driven Development) sur un cas concret.

**Contenu théorique (30 min) :**
- TDD : Red → Green → Refactor
 - **Red** : Écrire un test qui échoue (car le code n'existe pas)
 - **Green** : Écrire le minimum de code pour que le test passe
 - **Refactor** : Améliorer le code sans casser les tests
- Couverture de code :
 - JaCoCo : `jacoco-maven-plugin`
 - Couverture de lignes, branches, méthodes
 - Objectifs de couverture : 80% lignes, 70% branches
 - Rapport HTML : `target/site/jacoco/index.html`
- `@Disabled` — désactive temporairement un test
- `@Timeout(value = n, unit = TimeUnit.MILLISECONDS)` — test échoue si trop long
- `@Nested` — organise les tests hiérarchiquement
- `@Tag("unitaire")` / `@Tag("integration")` — catégorise les tests

**TP Lab04 — TDD Fil Rouge (75 min) :**
- Développer un service bancaire (`CompteBancaire`) en TDD :
 1. Création de compte avec solde initial
 2. Dépôt (montant positif uniquement)
 3. Retrait (solde suffisant, montant positif)
 4. Virement entre deux comptes
 5. Consultation d'historique des transactions
- Chaque étape : test → code → refactor
- Vérifier la couverture > 90%

**Annotations couvertes :** `@Disabled`, `@Timeout`, `@Nested`, `@Tag`, `@RepeatedTest`

---

### **Jour 2 — Sécurité des Applications Java (9h00–17h00)**

---

#### **Module 5 — Vulnérabilités OWASP en Java (9h00–10h30)**

**Objectif :** Identifier et corriger les vulnérabilités de sécurité les plus courantes.

**Contenu théorique (45 min) :**
- OWASP Top 10 (2021) appliqué à Java :
 1. **Broken Access Control** — Contrôle d'accès défaillant
 2. **Cryptographic Failures** — Données sensibles exposées
 3. **Injection** — SQL, XSS, command injection
 4. **Insecure Design** — Conception non sécurisée
 5. **Security Misconfiguration** — Mauvaise configuration
 6. **Vulnerable Components** — Composants obsolètes/vulnérables
 7. **Authentication Failures** — Authentification faible
 8. **Software Integrity Failures** — Intégrité logicielle
 9. **Logging & Monitoring Failures** — Absence de journalisation
 10. **SSRF** — Server-Side Request Forgery
- Démonstrations en Java pur :
 - Injection SQL via concaténation de chaînes vs `PreparedStatement`
 - XSS : échappement vs non-échappement des entrées HTML
 - Path traversal : `../../../etc/passwd`
 - Désérialisation non sécurisée

**TP Lab05 — OWASP (45 min) :**
- Code vulnérable à analyser et corriger
- Écrire des tests qui exploitent les vulnérabilités (preuves)
- Écrire des tests qui prouvent que les corrections fonctionnent

**Outils abordés :** OWASP Dependency-Check, SonarLint

---

#### **Module 6 — Introduction à Spring Boot et Tests Spring (10h45–12h30)**

**Objectif :** Comprendre l'écosystème Spring Boot et ses possibilités de test.

**Contenu théorique (45 min) :**
- Spring Boot : auto-configuration, starters, application.properties
- Inversion de contrôle et injection de dépendances :
 - `@Service`, `@Repository`, `@Controller`, `@RestController`
 - `@Autowired` — injection par le conteneur Spring
- Les slices de test Spring Boot :
 - `@SpringBootTest` — charge le contexte complet (lourd, pour tests d'intégration)
 - `@WebMvcTest(UserController.class)` — charge uniquement la couche MVC
 - `@DataJpaTest` — charge uniquement la couche JPA/repository
 - `@JsonTest` — teste la sérialisation/désérialisation JSON
 - `@RestClientTest` — teste les clients REST
- `MockMvc` — simule les requêtes HTTP sans démarrer le serveur
- `@MockBean` — remplace un bean Spring par un mock dans le contexte

**TP Lab06 — Intro Spring (45 min) :**
- Créer un micro-service REST de gestion de produits
- Tester avec `@WebMvcTest` et `MockMvc`
- Tester le repository avec `@DataJpaTest` et H2 en mémoire
- Tester le service avec des mocks

**Annotations couvertes :** `@SpringBootApplication`, `@RestController`, `@Service`, `@Repository`, `@Autowired`, `@SpringBootTest`, `@WebMvcTest`, `@DataJpaTest`, `@MockBean`, `@AutoConfigureMockMvc`, `@Autowired MockMvc`

---

#### **Module 7 — Spring Security et JWT (13h30–15h00)**

**Objectif :** Sécuriser une API REST avec Spring Security et authentification JWT.

**Contenu théorique (45 min) :**
- Spring Security :
 - `SecurityFilterChain` — chaîne de filtres de sécurité
 - `@EnableWebSecurity` — active la sécurité web
 - `@EnableMethodSecurity` — active `@PreAuthorize`, `@PostAuthorize`
 - `UserDetailsService` — charge les utilisateurs
 - `PasswordEncoder` (BCrypt) — hachage des mots de passe
- JWT (JSON Web Token) :
 - Structure : Header.Payload.Signature
 - Access Token vs Refresh Token
 - `OncePerRequestFilter` — filtre JWT personnalisé
- Tests de sécurité :
 - `@WithMockUser` — simule un utilisateur authentifié
 - `@WithAnonymousUser` — simule un utilisateur non authentifié
 - `.with(csrf())` — inclut le token CSRF dans MockMvc
 - `.with(httpBasic("user", "pass"))` — authentification HTTP Basic
 - `SecurityMockMvcRequestPostProcessors.jwt()` — simule un JWT

**TP Lab07 — Spring Security (45 min) :**
- Ajouter Spring Security à l'API produits
- Implémenter l'authentification JWT
- Protéger les endpoints par rôle (ADMIN vs USER)
- Écrire les tests de sécurité : accès autorisé, refusé, token expiré

**Annotations couvertes :** `@EnableWebSecurity`, `@EnableMethodSecurity`, `@PreAuthorize`, `@PostAuthorize`, `@WithMockUser`, `@WithAnonymousUser`, `@Configuration`, `@Bean`

---

#### **Module 8 — Projet Final : Application de Gestion d'Utilisateurs (15h15–17h00)**

**Objectif :** Mettre en œuvre l'ensemble des compétences acquises sur un projet complet.

**Fonctionnalités de l'application :**
- CRUD complet des utilisateurs (nom, email, rôle, statut actif/inactif)
- Authentification JWT (login, logout, refresh token)
- RBAC (Role-Based Access Control) : ADMIN peut tout faire, USER peut lire son profil
- Validation des entrées avec Bean Validation (`@Valid`)
- Pagination et filtrage
- Gestion des erreurs RFC 7807 (Problem Details)
- Hachage BCrypt des mots de passe
- Audit des actions (création, modification, suppression)

**Matrice de tests (74 tests) :**

| Classe de test | Couche testee | Type | Nombre |
|---|---|---|---|
| `UserEntityTest` | Entite | Unitaire | 5 |
| `UserRequestTest` | DTO entree | Unitaire | 1 |
| `UserResponseTest` | DTO sortie | Unitaire | 4 |
| `ErrorResponseTest` | DTO erreur | Unitaire | 3 |
| `ExceptionsTest` | Exceptions | Unitaire | 2 |
| `GlobalExceptionHandlerTest` | Gestion erreurs | Unitaire | 3 |
| `UserServiceTest` | Service | Unitaire + Mockito | 15 |
| `UserControllerTest` | Controleur | MVC | 13 |
| `UserRepositoryTest` | Repository | JPA | 12 |
| `JwtUtilTest` | JWT | Unitaire | 6 |
| `SecurityTests` | Securite | MVC | 10 |

**Annotations couvertes :**
- JUnit 5 : `@Test`, `@DisplayName`, `@Nested`, `@BeforeEach`, `@AfterEach`, `@Timeout`
- Mockito : `@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`, `@Spy`, `@Captor`
- Spring : `@WebMvcTest`, `@DataJpaTest`, `@MockBean`, `@AutoConfigureMockMvc`
- Spring Security : `@WithMockUser`, `@WithAnonymousUser`
- Bean Validation : `@NotNull`, `@NotBlank`, `@Email`, `@Size`

---

## Évaluation

- **QCM final (20 questions)** : 50% tests unitaires, 50% sécurité — seuil de réussite : 14/20
- **Évaluation pratique continue** : chaque lab est noté sur la complétion (tests qui passent)
- **Projet final Lab08** : note sur 20 basée sur la couverture de tests (>80%), la qualité SonarQube (0 bugs, 0 vulnérabilités), et l'exhaustivité de la matrice de tests

---

## Ressources complémentaires

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [OWASP Dependency-Check](https://jeremylong.github.io/DependencyCheck/)
- [SonarQube Documentation](https://docs.sonarsource.com/sonarqube/latest/)
