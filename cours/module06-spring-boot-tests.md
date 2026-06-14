# Module 6 : Tests Spring Boot — Slices, MockMvc & DataJpa

**Durée :** 1h45 (10h45–12h30) — Jour 2 Matin
**Prérequis :** Module 1 à 5, notions de base Spring Boot
**Projet support :** `labs/lab06-spring-intro` (Spring Boot 3.2.5, Java 17, H2, JPA, Validation)

---

## PARTIE 1 — THÉORIE (40 min)

### 1. Rappels Spring Boot

Le fichier `pom.xml` du lab (`lab06-spring-intro/pom.xml`) montre les 4 starters utilisés :

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.5</version>
</parent>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>        <!-- REST + Tomcat embarqué -->
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>   <!-- JPA + Hibernate -->
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId> <!-- Bean Validation -->
    </dependency>
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>                                    <!-- Base en mémoire -->
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>        <!-- JUnit 5 + Mockito + MockMvc + JSONassert -->
        <scope>test</scope>
    </dependency>
</dependencies>
```

**spring-boot-starter-test** (ligne 44-46) est le starter qui prépare tout l'écosystème de test :
- JUnit Jupiter (5.x)
- Mockito (`@Mock`, `@InjectMocks`, `@ExtendWith(MockitoExtension.class)`)
- MockMvc (tests de contrôleurs)
- JSONassert et JsonPath (vérification de réponses JSON)
- Spring TestContext Framework

La base de données H2 (`jdbc:h2:mem:produitsdb`) est configurée dans `application.properties` :

```properties
spring.datasource.url=jdbc:h2:mem:produitsdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false
```

`create-drop` signifie que les tables sont créées au démarrage et supprimées à l'arrêt — parfait pour les tests.

---

### 2. Inversion de contrôle (IoC) et Injection de dépendances (DI)

Spring Boot repose sur un **conteneur IoC** : vous ne créez jamais vos objets avec `new`, vous les **déclarez** et Spring les instancie.

#### Les stéréotypes Spring

| Annotation | Rôle | Exemple dans le lab |
|------------|------|---------------------|
| `@Service` | Logique métier | `ProduitService.java:11` |
| `@Repository` | Accès aux données | `ProduitRepository.java:9` |
| `@Controller` / `@RestController` | Couche HTTP | `ProduitController.java:12` |
| `@Component` | Bean générique | Non utilisé dans ce lab |

Ces annotations rendent la classe **détectable** par le **component scan** (`@ComponentScan`). Spring crée une instance unique (singleton) de chaque classe annotée et l'enregistre dans le **contexte d'application** (`ApplicationContext`).

#### Injection par constructeur

Dans le lab, toutes les injections utilisent le **constructeur** (pas de `@Autowired` explicite — il est optionnel depuis Spring 4.3 quand il n'y a qu'un seul constructeur) :

```java
// ProduitService.java:15-18
@Service
@Transactional
public class ProduitService {
    private final ProduitRepository repository;

    public ProduitService(ProduitRepository repository) {  // Injection implicite
        this.repository = repository;
    }
}
```

```java
// ProduitController.java:16-19
@RestController
@RequestMapping("/api/produits")
public class ProduitController {
    private final ProduitService service;

    public ProduitController(ProduitService service) {     // Injection implicite
        this.service = service;
    }
}
```

**Pourquoi l'injection par constructeur ?**
1. Le champ peut être `final` → immutabilité
2. Pas de dépendance à Spring dans le test (on peut instancier avec `new`)
3. Détection au compile-time si une dépendance manque (vs `null` au runtime avec `@Autowired` sur champ)

---

### 3. L'annotation `@SpringBootApplication`

```java
// SpringIntroApplication.java:6-10
@SpringBootApplication
public class SpringIntroApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringIntroApplication.class, args);
    }
}
```

`@SpringBootApplication` est une **méta-annotation** qui combine :

| Annotation composante | Rôle |
|-----------------------|------|
| `@Configuration` | La classe peut déclarer des `@Bean` |
| `@EnableAutoConfiguration` | Active l'auto-configuration de Spring Boot (détecte les starters et configure automatiquement) |
| `@ComponentScan` | Scanne les packages à partir de celui de la classe annotée pour trouver `@Service`, `@Repository`, `@Controller`, etc. |

---

### 4. Les slices de test Spring Boot — Concept fondamental

Le **contexte Spring complet** est lourd à charger (toutes les couches : MVC, services, repositories, sécurité, bases de données...). Pour les tests, Spring Boot propose des **slices** : des portions du contexte qui ne chargent que la couche nécessaire.

#### Tableau comparatif des 3 slices principales

| Slice | Charge | Ne charge pas | Dans le lab |
|-------|--------|---------------|-------------|
| **`@WebMvcTest`** | Contrôleurs, MockMvc, filtres MVC, `@ControllerAdvice` | Services, repositories, JPA, base de données | `ProduitControllerTest` |
| **`@DataJpaTest`** | Repositories, EntityManager, DataSource H2, `@Entity` | Contrôleurs, services, autres beans | `ProduitRepositoryTest` |
| **`@SpringBootTest`** | Contexte COMPLET (tous les beans) | Rien (sauf exclusions explicites) | Non utilisé dans ce lab |

#### Représentation mentale

```
Contexte Spring complet (@SpringBootTest)
┌──────────────────────────────────────────────────────────┐
│  ┌─────────────────────┐  ┌───────────────────────────┐  │
│  │  @WebMvcTest         │  │  @DataJpaTest             │  │
│  │  ┌───────────────┐   │  │  ┌─────────────────────┐  │  │
│  │  │ Contrôleurs   │   │  │  │ Repositories        │  │  │
│  │  │ MockMvc       │   │  │  │ EntityManager       │  │  │
│  │  │ @MockBean     │   │  │  │ DataSource H2       │  │  │
│  │  │ Filtres HTTP  │   │  │  │ @Entity classes     │  │  │
│  │  └───────────────┘   │  │  │ Transaction mgr     │  │  │
│  │  PAS de :            │  │  └─────────────────────┘  │  │
│  │  - Services          │  │  PAS de :                 │  │
│  │  - Repositories      │  │  - Contrôleurs            │  │
│  │  - Base de données   │  │  - Services               │  │
│  └─────────────────────┘  │  - MockMvc                 │  │
│                            └───────────────────────────┘  │
└──────────────────────────────────────────────────────────┘
```

**Avantages des slices :**
- **Démarrage ultra-rapide** : un `@WebMvcTest` démarre en ~1s, un `@SpringBootTest` en ~10s
- **Isolation** : un test MVC n'est pas pollué par un bug dans le repository
- **Périmètre clair** : on teste une seule responsabilité à la fois

---

### 5. `@WebMvcTest` en détail

`@WebMvcTest(ProduitController.class)` (`ProduitControllerTest.java:22`) charge **uniquement** la couche MVC de Spring :

```java
@WebMvcTest(ProduitController.class)
class ProduitControllerTest {
    @Autowired
    private MockMvc mockMvc;         // Injecté automatiquement

    @Autowired
    private ObjectMapper objectMapper; // Jackson pour sérialiser/désérialiser

    @MockBean
    private ProduitService produitService; // Mock du service — PAS le vrai
}
```

**Ce qui est configuré automatiquement :**
- `MockMvc` — permet d'envoyer des requêtes HTTP simulées sans démarrer de serveur
- `ObjectMapper` — sérialise/désérialise le JSON
- Toute la configuration MVC de Spring (convertisseurs de messages, validation, gestion d'erreurs)

**`@MockBean` vs `@Mock` :**
- `@MockBean` (Spring) : crée un mock ET l'enregistre dans le contexte Spring → remplace le vrai bean
- `@Mock` (Mockito) : crée un mock local, non connu du contexte Spring
- Dans `@WebMvcTest`, on utilise **toujours** `@MockBean` car le contrôleur attend un service dans le contexte

#### Construction de requêtes avec MockMvc

Le pattern est toujours le même :
```java
mockMvc.perform( REQUÊTE )
       .andExpect( VÉRIFICATION_1 )
       .andExpect( VÉRIFICATION_2 )
       ...
```

Les méthodes de requête sont dans `MockMvcRequestBuilders` (import statique) :
```java
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

get("/api/produits")              // GET
post("/api/produits")             // POST
put("/api/produits/1")            // PUT
delete("/api/produits/1")         // DELETE
```

Les vérifications sont dans `MockMvcResultMatchers` (import statique) :
```java
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

status().isOk()                   // 200
status().isCreated()              // 201
status().isNoContent()            // 204
status().isBadRequest()           // 400
status().isNotFound()             // 404
status().isInternalServerError()  // 500

content().contentType(MediaType.APPLICATION_JSON)  // Content-Type: application/json
jsonPath("$.nom").value("Ordinateur")              // Extraire un champ JSON
jsonPath("$[0].prix").value(999.99)                // Premier élément d'un tableau
```

---

### 6. `@DataJpaTest` en détail

`@DataJpaTest` (`ProduitRepositoryTest.java:13`) charge **uniquement** la couche JPA :

```java
@DataJpaTest
class ProduitRepositoryTest {
    @Autowired
    private ProduitRepository repository;  // Vrai repository — pas un mock
}
```

**Ce qui est configuré automatiquement :**
- Une base de données **H2 en mémoire** (remplace la DataSource configurée si H2 est dans le classpath)
- `EntityManager` / `TestEntityManager` pour interagir avec la base
- `@Transactional` avec **rollback automatique** après chaque test
- Les entités `@Entity` sont scannées, les tables sont créées et détruites automatiquement

**Rollback automatique :** chaque test s'exécute dans une transaction ouverte au début et annulée à la fin. Résultat : les tests sont **isolés** — le test B ne voit pas les données insérées par le test A.

**`@AutoConfigureTestDatabase`** est implicite dans `@DataJpaTest`. Il remplace la DataSource configurée par une base embarquée (H2 par défaut). Si vous voulez tester contre une vraie base PostgreSQL, utilisez `@AutoConfigureTestDatabase(replace = Replace.NONE)`.

---

### 7. `@Valid` et Bean Validation

La validation est déclenchée par `@Valid` dans les contrôleurs (`ProduitController.java:36`) :

```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public Produit creer(@Valid @RequestBody Produit produit) {
    return service.creer(produit);
}
```

Quand Spring voit `@Valid`, il exécute le **Validator** sur l'objet avant d'appeler la méthode. Si une contrainte échoue, Spring lance `MethodArgumentNotValidException`, qui est convertie en **400 Bad Request** par le handler par défaut.

Les contraintes sont définies directement sur l'entité (`Produit.java`) :

```java
@Entity
@Table(name = "produits")
public class Produit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
    @Column(nullable = false, length = 100)
    private String nom;

    @Size(max = 500, message = "La description ne peut pas dépasser 500 caractères")
    @Column(length = 500)
    private String description;

    @Positive(message = "Le prix doit être strictement positif")
    @Column(nullable = false)
    private double prix;

    @Min(value = 0, message = "La quantité ne peut pas être négative")
    @Max(value = 100000, message = "La quantité maximale est 100 000")
    @Column(nullable = false)
    private int quantite;
}
```

---

### 8. Annotations JPA — Décortiquage

| Annotation | Rôle | Dans le lab |
|------------|------|-------------|
| `@Entity` | Marque la classe comme entité persistable (mappée à une table) | `Produit.java:6` |
| `@Table(name = "produits")` | Nom explicite de la table (par défaut = nom de la classe) | `Produit.java:7` |
| `@Id` | Clé primaire | `Produit.java:10` |
| `@GeneratedValue(strategy = GenerationType.IDENTITY)` | Auto-incrément géré par la base | `Produit.java:11` — H2 génère l'ID |
| `@Column(nullable = false, length = 100)` | Contrainte NOT NULL + taille max de colonne | `Produit.java:16` |

---

### 9. Annotations Bean Validation — Décortiquage

| Annotation | Contrainte | Exemple |
|------------|-----------|---------|
| `@NotBlank` | Non null, non vide, pas uniquement des espaces | `" "` → rejeté |
| `@Size(min = 2, max = 100)` | Longueur de chaîne entre 2 et 100 caractères | `"A"` → rejeté, `"A".repeat(101)` → rejeté |
| `@Positive` | Nombre strictement positif (> 0) | `0.0` → rejeté, `-10` → rejeté |
| `@Min(0)` | Valeur minimale (inclusif) | `-1` → rejeté |
| `@Max(100000)` | Valeur maximale (inclusif) | `100001` → rejeté |
| `@Email` | Format email valide | `"pasunemail"` → rejeté |

---

### 10. Annotations REST — Décortiquage

#### `@RestController` (`ProduitController.java:12`)

```java
@RestController  // = @Controller + @ResponseBody
@RequestMapping("/api/produits")
public class ProduitController {
```

`@RestController` est un alias pour `@Controller` + `@ResponseBody`. Cela signifie que **toutes** les méthodes renvoient directement le corps de la réponse HTTP (JSON), et non un nom de vue (comme avec `@Controller` seul).

#### `@RequestMapping` et ses spécialisations

| Annotation | Méthode HTTP | Exemple dans le lab |
|------------|--------------|---------------------|
| `@GetMapping` | GET | Ligne 22 : `listerTous()`, ligne 27 : `trouverParId()`, ligne 51 : `rechercher()` |
| `@PostMapping` | POST | Ligne 34 : `creer()` |
| `@PutMapping` | PUT | Ligne 40 : `mettreAJour()` |
| `@DeleteMapping` | DELETE | Ligne 45 : `supprimer()` |

#### `@RequestMapping` au niveau classe (ligne 13)

```java
@RequestMapping("/api/produits")
```

Tous les mappings de méthodes sont **relatifs** à ce préfixe :
- `@GetMapping` → `GET /api/produits`
- `@GetMapping("/{id}")` → `GET /api/produits/1`
- `@PostMapping` → `POST /api/produits`

#### `@PathVariable` (ligne 28)

```java
@GetMapping("/{id}")
public ResponseEntity<Produit> trouverParId(@PathVariable Long id) {
```

Lie la variable de chemin `{id}` dans l'URL au paramètre `id` de la méthode. Si le nom du paramètre correspond au nom dans l'URL, pas besoin de préciser `@PathVariable("id")` — Spring infère automatiquement.

#### `@RequestBody` (ligne 36)

```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public Produit creer(@Valid @RequestBody Produit produit) {
```

Désérialise le corps JSON de la requête HTTP en objet Java `Produit`. Jackson (`ObjectMapper`) fait la conversion automatiquement : `{"nom":"Souris","prix":19.99}` → `new Produit("Souris", null, 19.99, 0)`.

#### `@RequestParam` (ligne 52)

```java
@GetMapping("/recherche")
public List<Produit> rechercher(@RequestParam String nom) {
```

Lie le paramètre de requête `?nom=Ordi` au paramètre `nom` de la méthode. Par défaut, `@RequestParam` est **obligatoire** (si absent → 400). Pour le rendre optionnel : `@RequestParam(required = false)` ou `@RequestParam(defaultValue = "")`.

#### `@ResponseStatus` (ligne 35, ligne 46)

```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)    // Force le statut HTTP à 201
public Produit creer(@Valid @RequestBody Produit produit) {
```

Sans `@ResponseStatus`, Spring renvoie 200 par défaut pour `@PostMapping`. Ici on force 201 Created (convention REST pour une création).

```java
@DeleteMapping("/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT) // Force le statut HTTP à 204
public void supprimer(@PathVariable Long id) {
```

Un `DELETE` réussi retourne 204 No Content (pas de corps de réponse).

#### `ResponseEntity` (ligne 28-31)

```java
@GetMapping("/{id}")
public ResponseEntity<Produit> trouverParId(@PathVariable Long id) {
    return service.trouverParId(id)
        .map(ResponseEntity::ok)          // 200 avec le produit dans le corps
        .orElse(ResponseEntity.notFound().build()); // 404 sans corps
}
```

`ResponseEntity` permet un **contrôle fin** du statut HTTP et des headers. Ici, le pattern `Optional.map().orElse()` est une façon élégante de gérer le cas 200/404 sans `if`.

---

### 11. Spring Data JPA — Repository

```java
// ProduitRepository.java:9-18
@Repository
public interface ProduitRepository extends JpaRepository<Produit, Long> {

    List<Produit> findByNomContainingIgnoreCase(String nom);
    List<Produit> findByPrixLessThanEqual(double prixMax);
    List<Produit> findByQuantiteGreaterThan(int quantiteMin);
    boolean existsByNomIgnoreCase(String nom);
}
```

**`JpaRepository<Produit, Long>`** : le premier paramètre générique est le type de l'entité, le second le type de la clé primaire. Spring Data implémente automatiquement l'interface (via un proxy JDK) avec les méthodes CRUD standard :

| Méthode héritée | Équivalent SQL |
|-----------------|----------------|
| `findAll()` | `SELECT * FROM produits` |
| `findById(id)` | `SELECT * FROM produits WHERE id = ?` |
| `save(entity)` | `INSERT INTO ...` ou `UPDATE ...` |
| `deleteById(id)` | `DELETE FROM produits WHERE id = ?` |
| `count()` | `SELECT COUNT(*) FROM produits` |
| `existsById(id)` | `SELECT COUNT(*) > 0 FROM produits WHERE id = ?` |

**Méthodes dérivées** (query methods) : Spring Data analyse le nom de la méthode et génère la requête JPQL correspondante.

| Méthode dérivée | Requête générée |
|-----------------|-----------------|
| `findByNomContainingIgnoreCase(String nom)` | `WHERE LOWER(p.nom) LIKE %?1%` |
| `findByPrixLessThanEqual(double prixMax)` | `WHERE p.prix <= ?1` |
| `findByQuantiteGreaterThan(int qteMin)` | `WHERE p.quantite > ?1` |
| `existsByNomIgnoreCase(String nom)` | `SELECT COUNT(*) > 0 WHERE LOWER(p.nom) = ?1` |

**Syntaxe de dérivation :** `findBy` + `[Propriété]` + `[Opérateur]` + `[Modificateurs]`
- Propriétés : `Nom`, `Prix`, `Quantite`
- Opérateurs : `Containing` (LIKE %...%), `LessThanEqual` (<=), `GreaterThan` (>)
- Modificateurs : `IgnoreCase` (insensible à la casse)

---

## PARTIE 2 — PRATIQUE PAS À PAS (45 min)

### 2.1 Produit.java — L'entité

Découpons chaque annotation en contexte :

```java
@Entity                          // => "Cette classe est une table"
@Table(name = "produits")        // => "La table s'appelle 'produits'"
public class Produit {

    @Id                          // => "Ce champ est la clé primaire"
    @GeneratedValue(strategy = GenerationType.IDENTITY)
                                  // => "H2 gère l'auto-incrément"
    private Long id;

    @NotBlank(message = "Le nom est obligatoire")
                                  // => Validation : pas null, pas "", pas "   "
    @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
                                  // => Validation : "A" → rejeté, 101 chars → rejeté
    @Column(nullable = false, length = 100)
                                  // => DDL : NOT NULL, VARCHAR(100)
    private String nom;

    @Size(max = 500, message = "La description ne peut pas dépasser 500 caractères")
    @Column(length = 500)         // Pas de @NotBlank → nullable
    private String description;

    @Positive(message = "Le prix doit être strictement positif")
    @Column(nullable = false)     // 0.01 → ok, 0 → rejeté, -10 → rejeté
    private double prix;

    @Min(value = 0, message = "La quantité ne peut pas être négative")
    @Max(value = 100000, message = "La quantité maximale est 100 000")
    @Column(nullable = false)
    private int quantite;
}
```

**Pourquoi deux couches de contraintes ?**
- Les annotations **JPA** (`@Column`) agissent sur le schéma DDL (structure de la table)
- Les annotations **Validation** (`@NotBlank`, `@Size`, etc.) agissent au niveau applicatif avant la persistence
- Les deux sont complémentaires : JPA protège la base, Validation protège l'application

---

### 2.2 ProduitControllerTest.java — Tests MVC

#### Test GET /api/produits — Liste de tous les produits (ligne 43-53)

```java
@Test
@DisplayName("GET /api/produits → 200 OK avec la liste des produits")
void listerTous() throws Exception {
    when(produitService.listerTous()).thenReturn(List.of(produit));

    mockMvc.perform(get("/api/produits"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$[0].nom").value("Ordinateur"))
        .andExpect(jsonPath("$[0].prix").value(999.99));
}
```

**Décomposition pas à pas :**

1. `when(produitService.listerTous()).thenReturn(List.of(produit))` — On mocke le service (`@MockBean`). Le service retourne une liste avec un seul produit. Le contrôleur ne sait pas que c'est un mock, il appelle `service.listerTous()` normalement.

2. `mockMvc.perform(get("/api/produits"))` — On simule un `GET /api/produits`. Pas de serveur qui tourne, pas de port réseau. MockMvc appelle directement le `DispatcherServlet` de Spring.

3. `.andExpect(status().isOk())` — On vérifie que la réponse HTTP a le statut **200 OK**.

4. `.andExpect(content().contentType(MediaType.APPLICATION_JSON))` — On vérifie le header `Content-Type: application/json`.

5. `.andExpect(jsonPath("$[0].nom").value("Ordinateur"))` — JsonPath `$[0]` = premier élément du tableau JSON. `.nom` = champ `nom` de cet élément. La valeur doit être `"Ordinateur"`.

6. `.andExpect(jsonPath("$[0].prix").value(999.99))` — Même principe pour le prix.

#### Test GET /api/produits/99 → 404 (ligne 67-73)

```java
@Test
@DisplayName("GET /api/produits/99 → 404 Not Found")
void trouverParId_inexistant() throws Exception {
    when(produitService.trouverParId(99L)).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/produits/99"))
        .andExpect(status().isNotFound());
}
```

Quand le service retourne `Optional.empty()`, le contrôleur (`ProduitController.java:28-31`) construit un `ResponseEntity.notFound().build()` → statut **404**. MockMvc vérifie exactement ce comportement.

#### Test POST /api/produits → 201 Created (ligne 75-86)

```java
@Test
@DisplayName("POST /api/produits → 201 Created")
void creer_valide() throws Exception {
    when(produitService.creer(any(Produit.class))).thenReturn(produit);

    mockMvc.perform(post("/api/produits")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(produit)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.nom").value("Ordinateur"));
}
```

**Points clés :**
- `.contentType(MediaType.APPLICATION_JSON)` — définit le header `Content-Type` de la requête
- `.content(objectMapper.writeValueAsString(produit))` — sérialise l'objet `Produit` en JSON : `{"nom":"Ordinateur","description":"PC portable","prix":999.99,"quantite":10,"id":1}`
- `status().isCreated()` — la méthode `creer()` a `@ResponseStatus(HttpStatus.CREATED)` → on vérifie 201

#### Test POST avec données invalides → 400 (ligne 88-97)

```java
@Test
@DisplayName("POST /api/produits avec nom vide → 400 Bad Request")
void creer_invalide() throws Exception {
    Produit invalide = new Produit("", "desc", -10, -1);

    mockMvc.perform(post("/api/produits")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalide)))
        .andExpect(status().isBadRequest());
}
```

**Ce qui se passe :**
1. Spring reçoit le JSON, désérialise en `Produit("", "desc", -10, -1)`
2. `@Valid` déclenche la validation
3. `@NotBlank` sur `nom` échoue (chaîne vide)
4. `@Positive` sur `prix` échoue (-10)
5. `@Min(0)` sur `quantite` échoue (-1)
6. Spring lance `MethodArgumentNotValidException` → convertie en `400 Bad Request`
7. **La méthode `creer()` du service n'est jamais appelée** — la validation a lieu avant

C'est ce qu'on appelle la **validation en amont** (fail-fast).

#### Test DELETE /api/produits/1 → 204 (ligne 112-119)

```java
@Test
@DisplayName("DELETE /api/produits/1 → 204 No Content")
void supprimer() throws Exception {
    doNothing().when(produitService).supprimer(1L);

    mockMvc.perform(delete("/api/produits/1"))
        .andExpect(status().isNoContent());
}
```

La méthode `supprimer()` retourne `void`. On utilise `doNothing().when(...)` car `when(mock.voidMethod())` ne compile pas (Mockito ne peut pas stuber `void` avec `when()`). `doNothing()` est le comportement par défaut pour les méthodes void mockées, mais c'est une bonne pratique de l'expliciter.

#### Test POST avec conflit métier → 500 (ligne 134-143)

```java
@Test
@DisplayName("POST création avec conflit → 500 (géré par le service)")
void creer_conflit() throws Exception {
    when(produitService.creer(any(Produit.class)))
        .thenThrow(new IllegalArgumentException("Un produit avec ce nom existe déjà"));

    mockMvc.perform(post("/api/produits")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(produit)))
        .andExpect(status().isInternalServerError());
}
```

Le service lance `IllegalArgumentException` si le produit existe déjà (`ProduitService.java:30-31`). Le contrôleur ne gère pas explicitement cette exception, donc Spring la convertit en **500 Internal Server Error**. Dans une application réelle, on ajouterait un `@ExceptionHandler` ou un `@ControllerAdvice` pour renvoyer un **409 Conflict** plus approprié.

---

### 2.3 ProduitServiceTest.java — Tests unitaires avec Mockito

Ce test est un test unitaire **pur** — pas de contexte Spring, que du Mockito.

```java
@ExtendWith(MockitoExtension.class)
class ProduitServiceTest {
    @Mock
    private ProduitRepository repository;

    @InjectMocks
    private ProduitService service;
}
```

- `@ExtendWith(MockitoExtension.class)` active Mockito pour JUnit Jupiter (initialise les `@Mock` avant chaque test)
- `@Mock` crée un faux `ProduitRepository`
- `@InjectMocks` crée un `ProduitService` et y injecte le mock du repository

#### Test creer — cas nominal (ligne 36-45)

```java
@Test
@DisplayName("creer : sauvegarde et retourne le produit")
void creer() {
    when(repository.existsByNomIgnoreCase("Clavier")).thenReturn(false);
    when(repository.save(any(Produit.class))).thenReturn(produit);

    Produit resultat = service.creer(produit);
    assertNotNull(resultat);
    assertEquals("Clavier", resultat.getNom());
}
```

Deux `when` pour stuber deux appels distincts :
1. `existsByNomIgnoreCase` → `false` (le nom n'existe pas encore)
2. `save()` → retourne le produit avec l'ID assigné

#### Test creer — doublon (ligne 47-53)

```java
@Test
@DisplayName("creer : échoue si le nom existe déjà")
void creer_nomExistant() {
    when(repository.existsByNomIgnoreCase("Clavier")).thenReturn(true);
    assertThrows(IllegalArgumentException.class, () -> service.creer(produit));
    verify(repository, never()).save(any());
}
```

**Double vérification :**
1. `assertThrows` — une exception est bien levée
2. `verify(repository, never()).save(any())` — la méthode `save` **n'a jamais été appelée** (l'exception est levée avant)

Ce pattern `verify(..., never())` est crucial pour prouver que le code s'arrête au bon moment.

#### Test supprimer (ligne 63-69)

```java
@Test
@DisplayName("supprimer : supprime le produit existant")
void supprimer() {
    when(repository.existsById(1L)).thenReturn(true);
    service.supprimer(1L);
    verify(repository).deleteById(1L);
}
```

`verify(repository).deleteById(1L)` vérifie que la méthode attendue a bien été appelée. Pas d'assertion sur le retour (la méthode est `void`), on vérifie l'**interaction**.

---

### 2.4 ProduitRepositoryTest.java — Tests d'intégration JPA

Ce test utilise `@DataJpaTest` : de vraies interactions avec une base H2 en mémoire. Aucun mock.

```java
@DataJpaTest
class ProduitRepositoryTest {
    @Autowired
    private ProduitRepository repository;  // Vrai repository, vraie base

    private Produit p1, p2, p3;

    @BeforeEach
    void setUp() {
        p1 = repository.save(new Produit("Ordinateur", "PC", 999.99, 10));
        p2 = repository.save(new Produit("Souris", "Sans fil", 29.99, 100));
        p3 = repository.save(new Produit("Clavier", "Mécanique", 89.99, 50));
    }
}
```

**Data Fixture :** 3 produits persistés avant chaque test. Après chaque test, la transaction est annulée (rollback) → les tests suivants repartent d'une base vide.

#### Test findAll (ligne 30-34)

```java
@Test
@DisplayName("findAll : retourne tous les produits")
void findAll() {
    List<Produit> produits = repository.findAll();
    assertEquals(3, produits.size());
}
```

Simple et direct : on vérifie que le setup a bien persisté 3 produits.

#### Test findByNomContainingIgnoreCase — insensibilité à la casse (ligne 45-53)

```java
@Test
@DisplayName("findByNomContainingIgnoreCase : insensible à la casse")
void rechercheInsensibleCasse() {
    List<Produit> r1 = repository.findByNomContainingIgnoreCase("ordi");
    assertEquals(1, r1.size());
    assertEquals("Ordinateur", r1.get(0).getNom());

    List<Produit> r2 = repository.findByNomContainingIgnoreCase("ORDI");
    assertEquals(1, r2.size());
}
```

**Ce test prouve deux choses :**
1. `IgnoreCase` fonctionne : `"ordi"` (minuscules) trouve `"Ordinateur"` (majuscule initiale)
2. `IgnoreCase` est cohérent : `"ORDI"` (tout en majuscules) trouve aussi `"Ordinateur"`

Si le test ne faisait qu'un seul cas, on ne saurait pas si c'est l'insensibilité à la casse qui fonctionne, ou juste une coïncidence.

#### Test findByPrixLessThanEqual (ligne 56-60)

```java
@Test
@DisplayName("findByPrixLessThanEqual : filtre par prix max")
void filtrePrixMax() {
    List<Produit> abordables = repository.findByPrixLessThanEqual(99.99);
    assertEquals(2, abordables.size());
}
```

Produits en base : 999.99, 29.99, 89.99. Prix ≤ 99.99 → Souris (29.99) et Clavier (89.99) → 2 résultats.

#### Test findByQuantiteGreaterThan (ligne 63-67)

```java
@Test
@DisplayName("findByQuantiteGreaterThan : filtre par quantité min")
void filtreQuantiteMin() {
    List<Produit> enStock = repository.findByQuantiteGreaterThan(49);
    assertEquals(2, enStock.size());
}
```

Quantités en base : 10, 100, 50. Quantité > 49 → Souris (100) et Clavier (50) → 2 résultats. La condition est **stricte** (`GreaterThan` = `>`), pas `>=`.

#### Test existsByNomIgnoreCase — détection de doublon (ligne 86-91)

```java
@Test
@DisplayName("existsByNomIgnoreCase : détection de doublon")
void existsByNom() {
    assertTrue(repository.existsByNomIgnoreCase("Ordinateur"));
    assertTrue(repository.existsByNomIgnoreCase("ordinateur"));
    assertFalse(repository.existsByNomIgnoreCase("Tablette"));
}
```

Ce test est critique pour la logique métier : le service utilise `existsByNomIgnoreCase` dans `creer()` (`ProduitService.java:30`) pour empêcher les doublons. On vérifie :
- Le nom exact retourne `true`
- La variante en minuscules retourne `true` (insensible à la casse)
- Un nom inexistant retourne `false`

#### Test deleteById (ligne 79-83)

```java
@Test
@DisplayName("deleteById : supprime un produit")
void deleteById() {
    repository.deleteById(p1.getId());
    assertEquals(2, repository.count());
    assertFalse(repository.existsById(p1.getId()));
}
```

Vérification en deux temps :
1. `count()` passe de 3 à 2
2. L'ID supprimé n'existe plus

---

## PARTIE 3 — LAB (20 min)

### 🎯 Objectif

Ajouter un endpoint `PATCH /api/produits/{id}/prix` pour modifier uniquement le prix d'un produit.

### Contexte

L'application existante gère des produits avec un CRUD complet (GET, POST, PUT, DELETE). On veut maintenant une mise à jour **partielle** : changer seulement le prix, sans toucher au nom, à la description, ou à la quantité.

### Travail à réaliser

#### 1. Service — Méthode `mettreAJourPrix`

Ajouter dans `ProduitService` :
```java
public Produit mettreAJourPrix(Long id, double nouveauPrix) {
    // Trouver le produit ou lever IllegalArgumentException
    // Valider que le prix est > 0
    // Mettre à jour le prix
    // Sauvegarder et retourner
}
```

#### 2. Contrôleur — Endpoint PATCH

Ajouter dans `ProduitController` :
```java
@PatchMapping("/{id}/prix")
public Produit mettreAJourPrix(@PathVariable Long id,
                               @RequestBody Map<String, Double> body) {
    double prix = body.get("prix");
    return service.mettreAJourPrix(id, prix);
}
```

#### 3. Tests — Les 3 couches

**ProduitServiceTest :**
- `mettreAJourPrix` — prix valide → produit retourné avec le nouveau prix
- `mettreAJourPrix` — produit inexistant → `IllegalArgumentException`
- `mettreAJourPrix` — prix négatif → `IllegalArgumentException`

**ProduitControllerTest (`@WebMvcTest`) :**
- `PATCH /api/produits/1/prix` → 200 OK avec le produit mis à jour
- `PATCH /api/produits/99/prix` → 500 (ID inexistant → exception du service)
- `PATCH /api/produits/1/prix` avec prix négatif → 500 (exception du service)
- Vérifier avec `jsonPath("$.prix")` que le prix retourné est le nouveau prix

**ProduitRepositoryTest (`@DataJpaTest`) :**
- Persister un produit, récupérer par ID, vérifier le prix initial
- Mettre à jour le prix via `save()`, vérifier que le prix a changé
- Vérifier que la mise à jour ne modifie pas les autres champs (nom, description, quantité)

### ✅ Critères de réussite

- Les 3 couches de tests sont présentes (Service, Controller MVC, Repository JPA)
- La couverture de code est ≥ 80% (vérifiable avec `mvn test jacoco:report`)
- Tous les tests passent au `mvn test -pl lab06-spring-intro`
- Les tests MVC incluent une vérification `jsonPath("$.prix")`

### Commandes utiles

```bash
# Lancer tous les tests du lab 06
mvn test -pl lab06-spring-intro

# Lancer les tests avec rapport de couverture JaCoCo
mvn test jacoco:report -pl lab06-spring-intro

# Ouvrir le rapport de couverture
firefox lab06-spring-intro/target/site/jacoco/index.html

# Lancer une classe de test spécifique
mvn test -pl lab06-spring-intro -Dtest=ProduitControllerTest
```

---

## FICHE MÉMO

### Hiérarchie des tests Spring Boot

```
Test unitaire (Mockito seul)
  → @ExtendWith(MockitoExtension.class)
  → @Mock + @InjectMocks
  → Le plus rapide, pas de contexte Spring
  → ProduitServiceTest

Test MVC (@WebMvcTest)
  → Charge UNIQUEMENT la couche Web
  → MockMvc, @MockBean, ObjectMapper
  → Pas de base de données
  → ProduitControllerTest

Test JPA (@DataJpaTest)
  → Charge UNIQUEMENT la couche JPA
  → Base H2 en mémoire, rollback auto
  → Pas de contrôleur, pas de service
  → ProduitRepositoryTest

Test d'intégration (@SpringBootTest)
  → Charge TOUT le contexte
  → Le plus lent
  → Utilisé pour les tests end-to-end
```

### Slices de test — aide-mémoire

| Annotation | Charge | Base de données | Utilisation |
|------------|--------|-----------------|-------------|
| `@WebMvcTest(MonController.class)` | MVC uniquement | Non | Tester les endpoints REST |
| `@DataJpaTest` | JPA uniquement | H2 en mémoire | Tester les repositories |
| `@SpringBootTest` | Contexte complet | Configurée | Tests end-to-end |
| `@WebMvcTest` + `@MockBean` | MVC + mocks | Non | Isoler le contrôleur du service |

### MockMvc — patterns de requête

```java
// GET simple
mockMvc.perform(get("/api/produits"))

// GET avec path variable
mockMvc.perform(get("/api/produits/{id}", 1))

// GET avec query param
mockMvc.perform(get("/api/produits/recherche").param("nom", "Ordi"))

// POST avec corps JSON
mockMvc.perform(post("/api/produits")
    .contentType(MediaType.APPLICATION_JSON)
    .content(objectMapper.writeValueAsString(objet)))

// PUT avec corps JSON
mockMvc.perform(put("/api/produits/1")
    .contentType(MediaType.APPLICATION_JSON)
    .content(objectMapper.writeValueAsString(objet)))

// DELETE
mockMvc.perform(delete("/api/produits/1"))
```

### MockMvc — patterns de vérification

```java
.andExpect(status().isOk())               // 200
.andExpect(status().isCreated())          // 201
.andExpect(status().isNoContent())        // 204
.andExpect(status().isBadRequest())       // 400
.andExpect(status().isNotFound())         // 404
.andExpect(content().contentType(MediaType.APPLICATION_JSON))
.andExpect(jsonPath("$.nom").value("Ordi"))          // champ
.andExpect(jsonPath("$[0].id").value(1))             // tableau
.andExpect(jsonPath("$.length()").value(3))          // taille tableau
```

### Annotations REST — résumé

| Annotation | Méthode HTTP | Utilisation typique |
|------------|-------------|---------------------|
| `@GetMapping` | GET | Lire des données |
| `@PostMapping` | POST | Créer une ressource |
| `@PutMapping` | PUT | Remplacer une ressource entièrement |
| `@PatchMapping` | PATCH | Mise à jour partielle |
| `@DeleteMapping` | DELETE | Supprimer une ressource |

### Injection de paramètres

| Annotation | Source | Exemple |
|------------|--------|---------|
| `@PathVariable` | Segment d'URL | `/{id}` → `@PathVariable Long id` |
| `@RequestBody` | Corps JSON | `@RequestBody Produit p` |
| `@RequestParam` | Query string | `?nom=Ordi` → `@RequestParam String nom` |

### Bean Validation — contraintes utiles

| Annotation | Condition |
|------------|-----------|
| `@NotNull` | Pas null |
| `@NotBlank` | Pas null, pas "", pas "   " |
| `@NotEmpty` | Pas null, pas vide (collections, strings) |
| `@Size(min=, max=)` | Longueur/ taille entre min et max |
| `@Min(n)` / `@Max(n)` | Nombre ≥ n / ≤ n |
| `@Positive` / `@PositiveOrZero` | > 0 / ≥ 0 |
| `@Negative` / `@NegativeOrZero` | < 0 / ≤ 0 |
| `@Email` | Format email |
| `@Pattern(regexp=)` | Expression régulière |

### Spring Data JPA — mots-clés de dérivation

| Mot-clé | Signification SQL | Exemple |
|---------|-------------------|---------|
| `findBy...` | `SELECT ... WHERE ...` | `findByNom(String n)` |
| `...Containing` | `LIKE %...%` | `findByNomContaining(String n)` |
| `...IgnoreCase` | `LOWER(col)` | `findByNomIgnoreCase(...)` |
| `...LessThan` | `<` | `findByPrixLessThan(double p)` |
| `...LessThanEqual` | `<=` | `findByPrixLessThanEqual(double p)` |
| `...GreaterThan` | `>` | `findByQuantiteGreaterThan(int q)` |
| `...Between` | `BETWEEN` | `findByPrixBetween(double a, double b)` |
| `existsBy...` | `SELECT COUNT(*) > 0` | `existsByNom(String n)` |
