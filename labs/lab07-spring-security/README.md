# Lab07 - Securisation d'une API REST avec Spring Security et JWT

## Objectif

Mettre en oeuvre l'authentification et le controle d'acces par roles dans une API REST Spring Boot a l'aide de Spring Security et de JSON Web Tokens (JWT). L'objectif est de comprendre le mecanisme de securite sans etat (stateless), la generation et validation de tokens JWT, ainsi que le controle d'acces base sur les roles (RBAC).

## Enonce

Developper une application Spring Boot exposee via une API REST securisee. L'application doit permettre a des utilisateurs de s'authentifier via un endpoint `/api/auth/login` qui retourne un token JWT. Ce token doit ensuite etre transmis dans le header `Authorization` de chaque requete subsequente pour acceder aux ressources protegees. Trois niveaux d'acces sont a implementer : public (accessible sans authentification), utilisateur (roles USER ou ADMIN) et administrateur (role ADMIN uniquement). Les utilisateurs sont stockes en base de donnees H2 avec leurs mots de passe chiffres via BCrypt.

## Prerequis

- Java 17 ou superieur
- Maven 3.8+
- Connaissances de base en Spring Boot
- Notions de Spring Security (filtres, chaine de securite)
- Comprendre le principe des tokens JWT (header, payload, signature)

## Etapes

### Etape 1 : Creation du projet et dependances

Creer un projet Spring Boot avec les dependances suivantes :
- `spring-boot-starter-web` : exposition des endpoints REST
- `spring-boot-starter-security` : framework de securite
- `spring-boot-starter-data-jpa` : persistance des utilisateurs
- `h2` : base de donnees en memoire pour les tests
- `jjwt-api`, `jjwt-impl`, `jjwt-jackson` : generation et validation des tokens JWT (version 0.12.5)
- `spring-security-test` : support des tests de securite

### Etape 2 : Entite et repository Utilisateur

Creer l'entite JPA `Utilisateur` avec les champs `id`, `username` (unique), `password`, `role` (ex: ADMIN, USER) et `actif` (boolean). L'entite est mappee sur la table `utilisateurs`. Definir un constructeur par defaut et un constructeur parametre. Creer l'interface `UtilisateurRepository` qui etend `JpaRepository` avec deux methodes : `findByUsername` et `existsByUsername`.

### Etape 3 : Configuration Spring Security

Creer `SecurityConfig` annotee `@Configuration`, `@EnableWebSecurity` et `@EnableMethodSecurity`. Configurer la chaine de filtres pour :
- Desactiver CSRF (API REST sans etat)
- Definir une politique de session `STATELESS`
- Autoriser toutes les requetes vers `/api/auth/**`
- Autoriser les requetes GET vers `/api/produits/**` pour tout le monde
- Restreindre les methodes POST, PUT, DELETE sur `/api/produits/**` au role ADMIN
- Exiger l'authentification pour toute autre requete
- Ajouter le filtre `JwtAuthenticationFilter` avant `UsernamePasswordAuthenticationFilter`
Definir un bean `PasswordEncoder` utilisant `BCryptPasswordEncoder`.

### Etape 4 : Utilitaire JWT (JwtUtil)

Creer la classe `JwtUtil` annotee `@Component` qui encapsule la generation et la validation des tokens JWT :
- Une cle secrete HMAC-SHA256 generee a partir d'une chaine d'au moins 256 bits
- `genererToken(username, role)` : construit un JWT avec le sujet (username), le claim `role`, la date d'emission et une expiration d'une heure
- `extraireUsername(token)` : extrait le sujet du token
- `extraireRole(token)` : extrait le role du claim personnalise
- `estTokenValide(token)` : verifie la signature et la validite du token

### Etape 5 : Filtre d'authentification JWT (JwtAuthenticationFilter)

Creer la classe `JwtAuthenticationFilter` qui etend `OncePerRequestFilter` (execution unique par requete). Dans la methode `doFilterInternal` :
- Extraire le header `Authorization`
- Si le header commence par `Bearer `, extraire le token
- Valider le token via `JwtUtil.estTokenValide()`
- Si valide, extraire le username et le role, puis construire un objet `UsernamePasswordAuthenticationToken` avec l'autorite `ROLE_<role>`
- Definir l'authentification dans le `SecurityContextHolder`

### Etape 6 : Controleurs REST

Creer deux controleurs :
- `AuthController` (`/api/auth`) avec un endpoint `POST /login` qui recoit `username` et `password`, authentifie via `AuthenticationManager`, puis genere et retourne un token JWT. En cas d'echec, retourner un statut 401.
- `ProduitController` (`/api/produits`) avec trois endpoints GET :
  - `/public` : accessible sans restriction
  - `/admin` : restreint au role ADMIN via `@PreAuthorize("hasRole('ADMIN')")`
  - `/user` : accessible aux roles ADMIN et USER via `@PreAuthorize("hasAnyRole('ADMIN','USER')")`

### Etape 7 : Service et initialisation des utilisateurs

Creer `UtilisateurService` avec une methode `creer(username, password, role)` qui verifie l'unicite du username et encode le mot de passe avant sauvegarde. Dans `AppConfig`, definir un `CommandLineRunner` qui initialise deux utilisateurs par defaut (`admin` / `admin123` avec role ADMIN, `user` / `user123` avec role USER) si la base est vide. Declarer egalement les beans `UserDetailsService` et `AuthenticationManager`.

### Etape 8 : Tests unitaires et d'integration

Creer deux classes de test :
- `JwtUtilTest` : tests unitaires du `JwtUtil` couvrant la generation, l'extraction de username et role, la validation de tokens valides et la detection de tokens invalides (null, vide, modifies, aleatoires).
- `SecurityIntegrationTest` : tests d'integration avec `MockMvc` verifiant les regles d'acces (endpoint public accessible sans token, acces admin reserve au role ADMIN, acces utilisateur ouvert aux deux roles, rejet des tokens invalides ou absents avec code 403).

## Execution

1. Compiler le projet :
   ```
   mvn clean compile
   ```

2. Executer les tests :
   ```
   mvn test
   ```

3. Demarrer l'application :
   ```
   mvn spring-boot:run
   ```

4. Tester l'API manuellement (exemples avec curl) :
   ```
   # Connexion en tant qu'admin
   curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"admin123"}'
   # Retourne {"token":"eyJ..."}

   # Acces a l'espace admin avec le token
   curl -X GET http://localhost:8080/api/produits/admin \
     -H "Authorization: Bearer <token>"

   # Acces public sans token
   curl http://localhost:8080/api/produits/public
   ```

## Criteres de reussite

- Les tests `mvn test` passent tous sans erreur (minimum 13 tests)
- L'endpoint `/api/auth/login` retourne un token JWT valide pour des identifiants corrects
- L'endpoint `/api/auth/login` retourne 401 pour des identifiants incorrects
- L'endpoint `/api/produits/public` est accessible sans authentification
- L'endpoint `/api/produits/admin` est accessible uniquement avec un token portant le role ADMIN
- L'endpoint `/api/produits/user` est accessible avec un token portant le role USER ou ADMIN
- Les requetes sans token ou avec un token invalide recoivent un code 403 Forbidden
- Les mots de passe sont chiffres avec BCrypt et jamais stockes en clair
- Le filtre JWT s'execute une seule fois par requete (OncePerRequestFilter)
- L'application est totalement sans etat (pas de session HTTP)
