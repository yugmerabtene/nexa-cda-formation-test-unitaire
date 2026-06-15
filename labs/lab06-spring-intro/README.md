# Lab06 - Introduction a Spring Boot avec Tests

## Objectif

Decouvrir le framework Spring Boot et ses outils de test integres. Apprendre a écrire des tests cibles par couche (slices) avec `@WebMvcTest` et `@DataJpaTest`, utilisé́r `MockMvc` pour tester les controleurs REST, employer `@MockBean` pour simuler des dépendances, et organiser une demarche de test systematique sur une application CRUD de gestion de produits.

## Énoncé

Developper une application Spring Boot exposant une API REST de gestion de produits. L'application doit permettre de lister, rechercher, creer, mettre a jour et supprimer des produits. Chaque produit possede un identifiant, un nom, une description, un prix et une quantite. La validation des entrees (contraintes Jakarta Bean Validation) doit etre en place. Les tests doivent couvrir les trois couches principales : le repository JPA, le service metier et le controleur REST, en utilisant les slices de test appropriees ainsi que Mockito.

## Prérequis

- Java 17+
- Maven 3.6+
- Spring Boot 3.2.x
- Connaissances de base de Maven, JUnit 5, Mockito et Jakarta Bean Validation
- IDE avec support Spring Boot (IntelliJ IDEA, Eclipse, VS Code)

## Etapes pas a pas

### Étape 1 - Creation du projet et configuration Maven

Creer un projet Maven avec le parent Spring Boot `spring-boot-starter-parent:3.2.5`. Ajouter les dépendances suivantes :

- `spring-boot-starter-web` : couche REST (Tomcat, Jackson)
- `spring-boot-starter-data-jpa` : accès aux donnees avec JPA/Hibernate
- `spring-boot-starter-validation` : validation des entrees via Jakarta Bean Validation
- `h2` (runtime) : base de donnees en memoire pour développement et tests
- `spring-boot-starter-test` (test) : JUnit 5, Mockito, MockMvc, test slices
- `jacoco-maven-plugin` : couverture de code

Configurer le port et la console H2 dans `application.properties`.

### Étape 2 - Creation de l'entité JPA `Produit`

Creer la classe `Produit` annotee `@Entity` avec les champs `id` (cle primaire, auto-générée), `nom` (`@NotBlank`, 2 a 100 caracteres), `description` (max 500 caracteres, optionnelle), `prix` (`@Positive`, obligatoire) et `quantite` (`@Min(0)`, `@Max(100000)`, obligatoire). Fournir un constructeur par defaut (requis par JPA) et un constructeur paramètre. Ajouter les getters/setters.

### Étape 3 - Creation du `ProduitRepository`

Creer l'interface `ProduitRepository` etendant `JpaRepository<Produit, Long>`. Ajouter des méthodes de requête derivee :

- `findByNomContainingIgnoreCase(String nom)` : recherche insensible a la casse
- `findByPrixLessThanEqual(double prixMax)` : filtre par prix maximum
- `findByQuantiteGreaterThan(int quantiteMin)` : filtre par quantite minimum
- `existsByNomIgnoreCase(String nom)` : detection de doublon

### Étape 4 - Creation du `ProduitService`

Creer la classe `ProduitService` annotee `@Service` et `@Transactional`. Implementer les operations CRUD :

- `listerTous()` : retourne tous les produits
- `trouverParId(Long id)` : retourne un `Optional<Produit>`
- `creer(Produit)` : vérifié l'unicite du nom avant sauvegarde, lève une exception sinon
- `mettreAJour(Long id, Produit)` : met a jour un produit existant
- `supprimer(Long id)` : supprime un produit après verification d'existence
- `rechercherParNom(String nom)` : recherche textuelle
- `filtrerParPrixMax(double prixMax)` : filtre par budget

### Étape 5 - Creation du `ProduitController`

Creer la classe `ProduitController` annotee `@RestController` et `@RequestMapping("/api/produits")`. Implementer les endpoints REST :

- `GET /api/produits` : liste de tous les produits
- `GET /api/produits/{id}` : produit par ID (retourne 200 ou 404)
- `POST /api/produits` : creation avec validation (`@Valid`) -> 201
- `PUT /api/produits/{id}` : mise a jour avec validation -> 200
- `DELETE /api/produits/{id}` : suppression -> 204
- `GET /api/produits/recherche?nom=` : recherche par nom

### Étape 6 - Tests du repository avec `@DataJpaTest`

Creer `ProduitRepositoryTest` dans `src/test`. Utiliser `@DataJpaTest` qui configure Hibernate, Spring Data et une base H2 en memoire. Tester chaque méthode du repository : `findAll`, `findById`, `findByNomContainingIgnoreCase` (insensibilite a la casse), `findByPrixLessThanEqual`, `findByQuantiteGreaterThan`, `save`, `deleteById` et `existsByNomIgnoreCase`.

### Étape 7 - Tests du service avec Mockito

Creer `ProduitServiceTest` avec `@ExtendWith(MockitoExtension.class)`. Utiliser `@Mock` pour simuler le repository et `@InjectMocks` pour injecter le mock dans le service. Tester le comportement metier : creation avec/sans conflit de nom, recherche par ID, suppression avec/sans existence, recherche par nom.

### Étape 8 - Tests du controleur avec `@WebMvcTest`

Creer `ProduitControllerTest` dans `src/test`. Utiliser `@WebMvcTest(ProduitController.class)` qui charge uniquement la couche web. Injecter `MockMvc` pour simuler des requetes HTTP et `ObjectMapper` pour serialiser/deserialiser le JSON. Declarer `@MockBean` pour le `ProduitService`. Tester tous les endpoints : GET (200, 404), POST (201, 400 Validation, 500 Conflit), PUT (200), DELETE (204) et GET recherche (200).

### Étape 9 - Exécution des tests et verification de la couverture

Lancer les tests avec `mvn test`. Verifier le rapport de couverture genere par JaCoCo dans `target/site/jacoco/index.html`. Analyser les resultats : tous les tests doivent passer, la couverture doit etre elevee sur les packages controller, service, repository et model.

## Exécution

```bash
# Cloner ou naviguer dans le repertoire du lab
cd labs/lab06-spring-intro

# Compiler et exécuter les tests
mvn clean test

# Generer le rapport de couverture (inclus dans la phase test via jacoco)
mvn clean verify

# Lancer l'application (mode développement)
mvn spring-boot:run

# Tester l'API manuellement avec curl
curl http://localhost:8080/api/produits
curl -X POST http://localhost:8080/api/produits \
  -H "Content-Type: application/json" \
  -d '{"nom":"Souris","description":"Sans fil","prix":29.99,"quantite":100}'

# Console H2 disponible sur http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:mem:testdb
```

## Criteres de réussite

- Tous les tests unitaires passent (repository, service, controleur) : `mvn test` termine avec BUILD SUCCESS
- Le nombre total de tests est d'au moins 15 (8 repository + 6 service + 8 controleur)
- La couverture de code JaCoCo depasse 80% sur les packages `controller`, `service`, `repository` et `model`
- Les tests de repository utilisent `@DataJpaTest` et valident la persistance réelle en base H2
- Les tests de service utilisent `@Mock` / `@InjectMocks` et ne dependent pas de Spring
- Les tests de controleur utilisent `@WebMvcTest`, `MockMvc` et `@MockBean` pour isoler la couche web
- La validation Jakarta (`@Valid`) est testee : cas invalide retourne 400
- La gestion des erreurs metier (doublon de nom) est testee au niveau du service et du controleur
- Le README.md est complèt et redige en francais
- Tous les fichiers Java contiennent des commentaires Javadoc en francais sur chaque classe, méthode et attribut
