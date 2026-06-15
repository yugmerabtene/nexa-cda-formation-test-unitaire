# Module 7 — Spring Security et Authentification JWT

**Durée : 1h30 (13h30–15h00) — Jour 2 Après-midi**

**Prérequis :** Modules 1 à 6 (JUnit, Mockito, TDD, OWASP, Spring Boot Tests)

**Labs associés :** `labs/lab07-spring-security/`

---

## Objectifs pédagogiques

À l'issue de ce module, vous serez capable de :

1. Expliquer l'architecture de Spring Security (chaîne de filtres)
2. Configurer un `SecurityFilterChain` pour une API REST stateless
3. Comprendre le fonctionnement de JWT (JSON Web Token)
4. Générer, extraire et valider des tokens JWT avec la librairie JJWT
5. Créer un filtre d'authentification JWT personnalisé (`OncePerRequestFilter`)
6. Utiliser `@PreAuthorize` et `@PostAuthorize` pour la sécurité déclarative
7. Écrire des tests de sécurité avec `@WithMockUser`
8. Simuler des accès par rôle et vérifier les refus (403 Forbidden)

---

## PARTIE 1 -- THEORIE (30 min)

## 1. Pourquoi sécuriser une API REST ?

Une API REST exposée sur Internet est une **porte d'entrée** vers vos données et votre logique métier.
Sans sécurité, n'importe qui peut :

- Lire des données sensibles (GET non protégé)
- Modifier ou supprimer des ressources (POST/PUT/DELETE non protégé)
- Voler des informations utilisateurs
- Effectuer des attaques par force brute sur les endpoints de connexion

La sécurité d'une API REST repose sur 3 piliers :

| Pilier | Signification | Implémentation |
|---|---|---|
| **Authentification** | Qui êtes-vous ? | Login → JWT |
| **Autorisation** | Qu'avez-vous le droit de faire ? | Rôles + `@PreAuthorize` |
| **Confidentialité** | Les données sont-elles protégées ? | HTTPS (TLS) + BCrypt |

Dans ce module, nous implémentons **l'authentification par JWT** et **l'autorisation par rôles** (ADMIN, USER)
sur une API REST Spring Boot.

## 2. Spring Security : architecture

Spring Security est un framework qui s'intercale dans le traitement des requêtes HTTP via une **chaîne de filtres**
(`SecurityFilterChain`). Chaque requête HTTP traverse une série de filtres avant d'atteindre le contrôleur.

### Les composants clés

```mermaid
graph TD
    REQ["REQUETE HTTP"] --> FILTER["SecurityFilterChain"]
    subgraph FILTER["SecurityFilterChain"]
        JWT["JwtAuthenticationFilter"]
        UP["UsernamePasswordAuthFilter"]
        ET["ExceptionTranslationFilter"]
        JWT --> UP --> ET
    end
    FILTER --> AUTH["AuthenticationManager"]
    AUTH --> UDS["UserDetailsService"]
    FILTER --> CTRL["CONTROLEUR (RestController)"]
```

#### SecurityFilterChain
La **chaîne de filtres de sécurité**. C'est le cœur de Spring Security.
Chaque filtre a une responsabilité unique (authentification, autorisation, CSRF, etc.).
Les filtres s'exécutent dans un ordre défini.

#### AuthenticationManager
L'objet responsable de **valider les credentials** (email/mot de passe).
Il délègue la validation à un ou plusieurs `AuthenticationProvider`.
Dans notre cas, c'est le `AuthenticationProvider` par défaut qui utilise le `UserDetailsService`
et le `PasswordEncoder`.

#### UserDetailsService
Interface avec une seule méthode : `loadUserByUsername(String username)`.
Elle **charge les informations d'un utilisateur** depuis la base de données et retourne un objet `UserDetails`.
Spring Security utilise cet objet pour comparer le mot de passe fourni avec le mot de passe haché en base.

#### PasswordEncoder
Interface responsable du **hashage des mots de passe**.
On utilise `BCryptPasswordEncoder` qui implémente l'algorithme **BCrypt** :
- Fonction de hachage à sens unique (impossible de retrouver le mot de passe original)
- Salage automatique (chaque hash est différent, même pour le même mot de passe)
- Paramètre de coût configurable (plus c'est lent, plus c'est résistant aux attaques par force brute)

```
Mot de passe : "admin123"

 BCryptPasswordEncoder.encode()
Hash : $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy

 Hash (31 caractères)
 Coût (2^10 = 1024 itérations)
 Algorithme (2a = BCrypt)
```

## 3. Les annotations de configuration

### @Configuration
Marque une classe comme **classe de configuration Spring**. Les méthodes annotées `@Bean`
dans cette classe produisent des beans gérés par le conteneur Spring.

### @EnableWebSecurity
Active la **sécurité web Spring Security**. Cette annotation :
- Désactive la configuration de sécurité par défaut de Spring Boot
- Active l'intégration avec Spring MVC
- Permet de définir un `SecurityFilterChain` personnalisé

### @EnableMethodSecurity
Active la **sécurité déclarative par annotations** sur les méthodes.
Sans cette annotation, `@PreAuthorize` et `@PostAuthorize` sont ignorés.
Cette annotation ouvre plusieurs possibilités :

```java
@EnableMethodSecurity(
 prePostEnabled = true, // Active @PreAuthorize / @PostAuthorize
 securedEnabled = true, // Active @Secured
 jsr250Enabled = true // Active @RolesAllowed
)
```

Par défaut, `prePostEnabled = true` est activé avec `@EnableMethodSecurity`.

## 4. SecurityFilterChain : configuration détaillée

La méthode `filterChain(HttpSecurity http)` est le point central de la configuration de sécurité.
Analysons chaque directive.

### 4.1 `csrf.disable()`

**CSRF** (Cross-Site Request Forgery) est une attaque qui exploite la session utilisateur stockée
dans un cookie de navigateur. Un site malveillant peut envoyer des requêtes à votre API en utilisant
le cookie de session de l'utilisateur connecté.

Pour une **API REST stateless (sans session serveur)**, la protection CSRF est **inutile** car :
- Il n'y a **pas de cookie de session** (le token JWT est envoyé dans le header `Authorization`)
- L'attaquant ne peut pas lire le token JWT stocké dans le `localStorage` du navigateur
- Le token JWT est envoyé **explicitement** à chaque requête

```java
.csrf(csrf -> csrf.disable())
```

> **Ne jamais désactiver CSRF pour une application web avec sessions (Thymeleaf, JSP).**
> La désactivation est **spécifique aux APIs REST stateless.**

### 4.2 `sessionManagement()`

```java
.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```

`SessionCreationPolicy.STATELESS` indique à Spring Security de **ne jamais créer de session HTTP**.
Pourquoi ?

- Avec JWT, **le serveur ne stocke aucun état** (pas de session)
- Chaque requête est **autonome** : le token contient toutes les infos nécessaires
- Cela permet le **scaling horizontal** : n'importe quel serveur peut traiter n'importe quelle requête

| Politique | Comportement |
|---|---|
| `ALWAYS` | Crée une session HTTP si elle n'existe pas |
| `IF_REQUIRED` | Crée une session si nécessaire (défaut) |
| `NEVER` | Ne crée jamais de session, mais utilise celle existante |
| `STATELESS` | **Ne crée et n'utilise jamais de session** |

### 4.3 `authorizeHttpRequests()`

Définit les **règles de contrôle d'accès** par URL et méthode HTTP.

```java
.authorizeHttpRequests(auth -> auth
 .requestMatchers("/api/auth/**").permitAll()
 .requestMatchers(HttpMethod.GET, "/api/produits/**").permitAll()
 .requestMatchers(HttpMethod.POST, "/api/produits/**").hasRole("ADMIN")
 .requestMatchers(HttpMethod.PUT, "/api/produits/**").hasRole("ADMIN")
 .requestMatchers(HttpMethod.DELETE, "/api/produits/**").hasRole("ADMIN")
 .anyRequest().authenticated()
)
```

**Ordre des règles :** les règles sont évaluées **de haut en bas**. La première règle qui correspond
est appliquée. Il faut donc mettre les règles les plus spécifiques en premier.

**Méthodes de contrôle d'accès :**

| Méthode | Description |
|---|---|
| `permitAll()` | Accessible à tous, authentifié ou non |
| `authenticated()` | Nécessite d'être authentifié (peu importe le rôle) |
| `hasRole("ADMIN")` | Nécessite l'autorité `ROLE_ADMIN` |
| `hasAnyRole("ADMIN", "USER")` | Nécessite `ROLE_ADMIN` ou `ROLE_USER` |
| `hasAuthority("LECTURE")` | Nécessite l'autorité exacte (sans préfixe ROLE_) |
| `denyAll()` | Refusé à tout le monde |

**Analyse des règles du lab :**

| Règle | Explication |
|---|---|
| `/api/auth/**` → `permitAll()` | Les endpoints d'authentification (login) sont publics |
| `GET /api/produits/**` → `permitAll()` | La lecture des produits est ouverte à tous |
| `POST /api/produits/**` → `hasRole("ADMIN")` | Seuls les ADMIN peuvent créer des produits |
| `PUT /api/produits/**` → `hasRole("ADMIN")` | Seuls les ADMIN peuvent modifier |
| `DELETE /api/produits/**` → `hasRole("ADMIN")` | Seuls les ADMIN peuvent supprimer |
| `anyRequest()` → `authenticated()` | Tout le reste nécessite d'être authentifié |

### 4.4 `addFilterBefore()`

```java
.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
```

Insère le filtre JWT personnalisé **avant** le `UsernamePasswordAuthenticationFilter` standard
dans la chaîne de filtres.

**Pourquoi avant ?** Le `UsernamePasswordAuthenticationFilter` traite l'authentification
par formulaire (username/password). En insérant notre filtre JWT avant, on court-circuite
ce mécanisme : si un token JWT valide est présent, l'utilisateur est authentifié sans
passer par le formulaire.

## 5. JWT (JSON Web Token) : explication complète

### 5.1 Qu'est-ce qu'un JWT ?

Un JWT est un **format de token standardisé** (RFC 7519) qui permet d'échanger des
informations de manière sécurisée entre deux parties. Il est composé de 3 parties
séparées par des points :

```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJBRE1JTiIsImlhdCI6MTcwMDAwMDAwMCwiZXhwIjoxNzAwMDAzNjAwfQ.3Kx8L7mN2pQ9rS4tV6wX1yZ0aB3cD5eF7gH8iJ9kL0

 HEADER PAYLOAD SIGNATURE

 Base64 encodé Base64 encodé Base64 encodé
```

#### Header (en-tête)
```json
{
 "alg": "HS256",
 "typ": "JWT"
}
```

- `alg` : algorithme de signature (HS256 = HMAC-SHA256)
- `typ` : type de token

#### Payload (corps)
```json
{
 "sub": "admin",
 "role": "ADMIN",
 "iat": 1700000000,
 "exp": 1700003600
}
```

- `sub` (subject) : identifiant de l'utilisateur (username ou email)
- `role` : rôle personnalisé (claim)
- `iat` (issued at) : timestamp d'émission
- `exp` (expiration) : timestamp d'expiration

#### Signature
```
HMACSHA256(
 base64UrlEncode(header) + "." + base64UrlEncode(payload),
 secret
)
```

La signature est calculée en appliquant l'algorithme HMAC-SHA256 sur les deux premières
parties avec la clé secrète. Elle garantit l'**intégrité** du token : toute modification
du header ou du payload sera détectée car la signature ne correspondra plus.

### 5.2 HS256 : signature HMAC-SHA256

**HS256** = HMAC avec SHA-256. C'est un algorithme de signature **symétrique** :
la même clé secrète est utilisée pour signer et vérifier le token.

**Clé secrète minimum 256 bits (32 octets) :**
```
"cette-cle-est-un-secret-tres-long-dau-moins-256-bits-pour-hs256"
```
Cette chaîne de 56 caractères UTF-8 produit 56 octets (448 bits), largement suffisant.

### 5.3 Stateless : pourquoi pas de session serveur ?

Un des avantages majeurs de JWT est le **stateless** :

```
Architecture AVEC sessions (stateful) :

 Client Serveur Base de
 Session sessions
 Map (Redis/DB)

Problème : le serveur doit stocker les sessions → pas scalable

Architecture JWT (stateless) :

 Client Serveur
 Stocke Vérifie Aucune base de sessions nécessaire
 le JWT le JWT

Avantage : n'importe quel serveur peut traiter la requête → scalable
```

### 5.4 Sécurité : encodé ≠ chiffré

> **ATTENTION : le payload JWT est encodé en Base64, PAS chiffré.**

Base64 est un **encodage**, pas un chiffrement. N'importe qui peut décoder le payload
et lire son contenu :

```bash
# Décoder le payload d'un JWT (partie du milieu)
echo "eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJBRE1JTiJ9" | base64 -d
# Résultat : {"sub":"admin","role":"ADMIN"}
```

**Règles de sécurité :**
- Ne **jamais** mettre de données sensibles dans le payload JWT (mot de passe, numéro de carte)
- Toujours utiliser **HTTPS** pour empêcher l'interception du token
- La **signature** garantit l'intégrité (le token n'a pas été modifié), pas la confidentialité
- Utiliser des **dates d'expiration courtes** (1h dans notre lab)

### 5.5 Dépendances Maven pour JJWT

JJWT est divisé en trois artefacts Maven. jjwt-api contient les interfaces (nécessaire à la compilation, scope compile). jjwt-impl fournit l'implémentation (uniquement au runtime). jjwt-jackson gère la sérialisation JSON des claims. Cette séparation permet de changer d'implémentation sans recompiler le code applicatif.

```xml
<!-- API JJWT (compile) -->
<dependency>
 <groupId>io.jsonwebtoken</groupId>
 <artifactId>jjwt-api</artifactId>
 <version>0.12.5</version>
</dependency>

<!-- Implémentation (runtime) -->
<dependency>
 <groupId>io.jsonwebtoken</groupId>
 <artifactId>jjwt-impl</artifactId>
 <version>0.12.5</version>
 <scope>runtime</scope>
</dependency>

<!-- Sérialisation JSON (runtime) -->
<dependency>
 <groupId>io.jsonwebtoken</groupId>
 <artifactId>jjwt-jackson</artifactId>
 <version>0.12.5</version>
 <scope>runtime</scope>
</dependency>
```

- **jjwt-api** : interfaces et classes publiques (nécessaire à la compilation)
- **jjwt-impl** : implémentation interne (uniquement à l'exécution)
- **jjwt-jackson** : sérialisation/désérialisation JSON avec Jackson

---

## PARTIE 2 -- PRATIQUE PAS A PAS (40 min)

### Structure du projet

```
lab07-spring-security/
  pom.xml
  src/
    main/
      java/com/nexa/secu/
        SpringSecurityApplication.java
        config/
          SecurityConfig.java
          AppConfig.java
        entity/
          Utilisateur.java
        repository/
          UtilisateurRepository.java
        service/
          UtilisateurService.java
        controller/
          AuthController.java
          ProduitController.java
        security/
          JwtUtil.java
          JwtAuthenticationFilter.java
      resources/
        application.properties
    test/
      java/com/nexa/secu/
        security/
          JwtUtilTest.java
        controller/
          SecurityIntegrationTest.java
```

---

### pom.xml — Dépendances clés

Ce pom.xml déclare 5 starters Spring Boot et 3 artefacts JJWT. Les deux dépendances critiques sont spring-boot-starter-security (qui active automatiquement l'authentification sur toutes les requêtes) et spring-security-test (qui fournit @WithMockUser pour simuler des utilisateurs dans les tests).

```xml
<parent>
 <groupId>org.springframework.boot</groupId>
 <artifactId>spring-boot-starter-parent</artifactId>
 <version>3.2.5</version>
</parent>

<properties>
 <java.version>17</java.version>
 <jjwt.version>0.12.5</jjwt.version>
</properties>

<dependencies>
 <!-- Web : REST controllers, embedded Tomcat -->
 <dependency>
 <groupId>org.springframework.boot</groupId>
 <artifactId>spring-boot-starter-web</artifactId>
 </dependency>

 <!-- Security : authentification, autorisation, filtres -->
 <dependency>
 <groupId>org.springframework.boot</groupId>
 <artifactId>spring-boot-starter-security</artifactId>
 </dependency>

 <!-- JPA + H2 : persistance des utilisateurs -->
 <dependency>
 <groupId>org.springframework.boot</groupId>
 <artifactId>spring-boot-starter-data-jpa</artifactId>
 </dependency>
 <dependency>
 <groupId>com.h2database</groupId>
 <artifactId>h2</artifactId>
 <scope>runtime</scope>
 </dependency>

 <!-- Validation : @NotBlank, @Size, etc. -->
 <dependency>
 <groupId>org.springframework.boot</groupId>
 <artifactId>spring-boot-starter-validation</artifactId>
 </dependency>

 <!-- JJWT : génération et validation de tokens JWT -->
 <dependency>
 <groupId>io.jsonwebtoken</groupId>
 <artifactId>jjwt-api</artifactId>
 <version>${jjwt.version}</version>
 </dependency>
 <dependency>
 <groupId>io.jsonwebtoken</groupId>
 <artifactId>jjwt-impl</artifactId>
 <version>${jjwt.version}</version>
 <scope>runtime</scope>
 </dependency>
 <dependency>
 <groupId>io.jsonwebtoken</groupId>
 <artifactId>jjwt-jackson</artifactId>
 <version>${jjwt.version}</version>
 <scope>runtime</scope>
 </dependency>

 <!-- Tests -->
 <dependency>
 <groupId>org.springframework.boot</groupId>
 <artifactId>spring-boot-starter-test</artifactId>
 <scope>test</scope>
 </dependency>
 <dependency>
 <groupId>org.springframework.security</groupId>
 <artifactId>spring-security-test</artifactId>
 <scope>test</scope>
 </dependency>
</dependencies>
```

**Points importants :**
- `spring-boot-starter-security` : ajoute automatiquement la sécurité à toutes les requêtes
- `spring-security-test` : fournit `@WithMockUser`, `SecurityMockMvcRequestPostProcessors`
- H2 en `runtime` : base en mémoire pour les tests et le développement

---

### SecurityConfig.java — Décortication ligne par ligne

 `labs/lab07-spring-security/src/main/java/com/nexa/secu/config/SecurityConfig.java`

```java
package com.nexa.secu.config;

import com.nexa.secu.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
```

**Annotations de classe :**

| Annotation | Rôle |
|---|---|
| `@Configuration` | Déclare une classe de configuration Spring (les `@Bean` seront gérés par le conteneur) |
| `@EnableWebSecurity` | Active Spring Security et désactive la configuration automatique par défaut |
| `@EnableMethodSecurity` | Active `@PreAuthorize` et `@PostAuthorize` sur les méthodes des contrôleurs |

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

 private final JwtAuthenticationFilter jwtFilter;

 // Injection par constructeur du filtre JWT
 public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
 this.jwtFilter = jwtFilter;
 }
```

### La méthode `filterChain()` — le cœur de la configuration

```java
 @Bean
 public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
```

`HttpSecurity http` est un objet builder qui permet de configurer la sécurité web.
Chaque méthode retourne `http` pour le chaînage (pattern builder).

#### Étape 1 : Désactiver CSRF

```java
 http
 .csrf(csrf -> csrf.disable())
```

**Pourquoi désactiver CSRF ?**
Le CSRF (Cross-Site Request Forgery) protège les applications web traditionnelles où
le navigateur envoie automatiquement les cookies de session. Dans une API REST avec JWT :
- Pas de cookies de session (le token est dans le header `Authorization`)
- Le client (React, mobile, Postman) envoie **explicitement** le token
- Un site malveillant ne peut pas lire le localStorage pour voler le token

> C'est le **seul cas** où désactiver CSRF est acceptable.

#### Étape 2 : Session Stateless

```java
 .sessionManagement(session ->
 session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```

**Pourquoi STATELESS ?**
- Le serveur ne stocke aucune session HTTP
- Chaque requête est indépendante, authentifiée par le JWT
- Permet le déploiement sur plusieurs serveurs (scaling horizontal)

#### Étape 3 : Règles d'autorisation

```java
 .authorizeHttpRequests(auth -> auth
 .requestMatchers("/api/auth/**").permitAll()
 .requestMatchers(HttpMethod.GET, "/api/produits/**").permitAll()
 .requestMatchers(HttpMethod.POST, "/api/produits/**").hasRole("ADMIN")
 .requestMatchers(HttpMethod.PUT, "/api/produits/**").hasRole("ADMIN")
 .requestMatchers(HttpMethod.DELETE, "/api/produits/**").hasRole("ADMIN")
 .anyRequest().authenticated()
 )
```

**Analyse de chaque règle :**

| Ligne | Règle | Explication |
|---|---|---|
| `requestMatchers("/api/auth/**").permitAll()` | URL commençant par `/api/auth/` | Le login doit être accessible sans authentification, sinon personne ne peut se connecter |
| `requestMatchers(HttpMethod.GET, "/api/produits/**").permitAll()` | GET sur les produits | La consultation est publique |
| `requestMatchers(HttpMethod.POST, ...).hasRole("ADMIN")` | Création de produit | Réservé aux administrateurs |
| `requestMatchers(HttpMethod.PUT, ...).hasRole("ADMIN")` | Modification | Réservé aux administrateurs |
| `requestMatchers(HttpMethod.DELETE, ...).hasRole("ADMIN")` | Suppression | Réservé aux administrateurs |
| `anyRequest().authenticated()` | Tout le reste | Doit être authentifié (peu importe le rôle) |

**Ordre d'évaluation :** Les règles sont testées **de haut en bas**. Dès qu'une règle correspond,
elle est appliquée et les suivantes sont ignorées. C'est pourquoi `/api/auth/**` doit être
avant `anyRequest()`.

**`hasRole("ADMIN")` vs `hasAuthority("ROLE_ADMIN")` :**
- `hasRole("ADMIN")` ajoute automatiquement le préfixe `ROLE_` → vérifie `ROLE_ADMIN`
- `hasAuthority("ROLE_ADMIN")` vérifie littéralement `ROLE_ADMIN`
- Dans la pratique, on préfère `hasRole()` qui est plus lisible

#### Étape 4 : Insérer le filtre JWT

```java
 .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
```

Ajoute le filtre JWT **avant** le filtre d'authentification standard.
Cela permet au filtre JWT d'authentifier la requête via le token avant que
Spring Security ne tente l'authentification par formulaire.

#### Étape 5 : Construire et retourner

```java
 return http.build();
 }
```

### Le bean PasswordEncoder

```java
 @Bean
 public PasswordEncoder passwordEncoder() {
 return new BCryptPasswordEncoder();
 }
}
```

`BCryptPasswordEncoder` est l'implémentation recommandée de `PasswordEncoder`.
- Algorithme **BCrypt** : conçu spécialement pour le hachage de mots de passe
- **Salage automatique** : chaque appel à `encode()` produit un hash différent
- **Résistant aux attaques par rainbow table** grâce au sel aléatoire
- **Coût configurable** (défaut : 10 = 1024 itérations)
- **Fonction à sens unique** : impossible de retrouver le mot de passe original

---

### JwtUtil.java — Explication ligne par ligne

 `labs/lab07-spring-security/src/main/java/com/nexa/secu/security/JwtUtil.java`

```java
package com.nexa.secu.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
```

**Imports clés :**
- `io.jsonwebtoken.*` : classes JJWT pour construire et parser les tokens
- `io.jsonwebtoken.security.Keys` : fabrique de clés cryptographiques
- `javax.crypto.SecretKey` : clé secrète pour HMAC-SHA256

```java
@Component
public class JwtUtil {
```

`@Component` : rend cette classe disponible pour l'injection de dépendances.
On pourra faire `@Autowired private JwtUtil jwtUtil;` n'importe où.

### Constantes et clé secrète

```java
 private static final String SECRET =
 "cette-cle-est-un-secret-tres-long-dau-moins-256-bits-pour-hs256";
 private static final long EXPIRATION_MS = 3600000; // 1 heure en millisecondes

 private final SecretKey key;

 public JwtUtil() {
 this.key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
 }
```

**Analyse :**

- `SECRET` : la clé secrète partagée. Elle doit faire **au moins 256 bits** (32 octets) pour HS256.
 Cette chaîne de 56 caractères UTF-8 produit 56 octets (448 bits), ce qui est largement suffisant.
 **En production, cette clé doit être externalisée** (variable d'environnement, Vault, etc.)

- `EXPIRATION_MS = 3600000` : 1 heure. Pour 1h : `60 * 60 * 1000 = 3 600 000 ms`

- `Keys.hmacShaKeyFor()` : fabrique une `SecretKey` à partir des bytes de la chaîne.
 Cette clé sera utilisée pour **signer** et **vérifier** les tokens.

### Méthode `genererToken()`

```java
 public String genererToken(String username, String role) {
 return Jwts.builder()
 .subject(username)
 .claim("role", role)
 .issuedAt(new Date())
 .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
 .signWith(key)
 .compact();
 }
```

Décortiquons chaque étape du **builder pattern** :

| Étape | Méthode | Contenu du token |
|---|---|---|
| `Jwts.builder()` | Crée un builder JJWT | Initialisation |
| `.subject(username)` | Définit le sujet (sub) | `"sub": "admin"` |
| `.claim("role", role)` | Ajoute un claim personnalisé | `"role": "ADMIN"` |
| `.issuedAt(new Date())` | Date d'émission (iat) | `"iat": 1700000000` |
| `.expiration(...)` | Date d'expiration (exp) | `"exp": 1700003600` |

**Différence entre `subject` et `claim` :**
- `subject` (sub) = claim standard RFC 7519 pour l'identifiant principal
- `claim("role", role)` = claim personnalisé pour le rôle

**`new Date(System.currentTimeMillis() + EXPIRATION_MS)`**
- `System.currentTimeMillis()` = timestamp actuel en ms
- On ajoute 3 600 000 ms (1 heure)
- Le token expirera 1 heure après sa création

**`.signWith(key)` :** Signe le token avec la clé secrète en utilisant l'algorithme
par défaut (HS256). La signature est calculée automatiquement.

**`.compact()` :** Sérialise le tout en une chaîne JWT complète :
`header.payload.signature` (encodées en Base64URL).

### Méthode `extraireUsername()`

```java
 public String extraireUsername(String token) {
 return parseToken(token).getPayload().getSubject();
 }
```

**Chaîne d'appels :**
1. `parseToken(token)` → retourne le `Jws<Claims>` (token parsé et vérifié)
2. `.getPayload()` → extrait le payload (Claims)
3. `.getSubject()` → extrait le sujet (username/email)

### Méthode `extraireRole()`

```java
 public String extraireRole(String token) {
 return parseToken(token).getPayload().get("role", String.class);
 }
```

`.get("role", String.class)` : extrait un claim personnalisé par son nom, avec le type attendu.
Contrairement à `getSubject()` qui est standard, `"role"` est un claim personnalisé.

### Méthode `estTokenValide()`

```java
 public boolean estTokenValide(String token) {
 try {
 parseToken(token);
 return true;
 } catch (JwtException | IllegalArgumentException e) {
 return false;
 }
 }
```

**Pourquoi try/catch ?** `parseToken()` lève des exceptions si :
- Le token est malformé (pas 3 parties séparées par des `.`)
- La signature est invalide (token modifié)
- Le token a expiré (`ExpiredJwtException`)
- Le token est vide ou null

La méthode retourne simplement `true`/`false` au lieu de laisser l'exception se propager.

**Types d'exceptions JWT :**

| Exception | Cause |
|---|---|
| `ExpiredJwtException` | Le token a dépassé sa date d'expiration |
| `MalformedJwtException` | Le token n'est pas un JWT valide |
| `SignatureException` | La signature ne correspond pas |
| `UnsupportedJwtException` | Format non supporté |
| `IllegalArgumentException` | Token null ou vide |

### Méthode privée `parseToken()`

```java
 private Claims parseToken(String token) {
 return Jwts.parser()
 .verifyWith(key)
 .build()
 .parseSignedClaims(token)
 .getPayload();
 }
```

**Étapes :**
1. `Jwts.parser()` → crée un parser JJWT
2. `.verifyWith(key)` → définit la clé de vérification
3. `.build()` → construit le parser
4. `.parseSignedClaims(token)` → parse et vérifie le token → retourne un `Jws<Claims>`
5. `.getPayload()` → extrait le payload

> Avec JJWT 0.12.x, l'API a changé par rapport à 0.11.x :
> - `parserBuilder()` → `parser()`
> - `setSigningKey()` → `verifyWith()`
> - `parseClaimsJws()` → `parseSignedClaims()`
> - `getBody()` → `getPayload()`

---

### JwtAuthenticationFilter.java — Explication ligne par ligne

 `labs/lab07-spring-security/src/main/java/com/nexa/secu/security/JwtAuthenticationFilter.java`

```java
package com.nexa.secu.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
```

### OncePerRequestFilter

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
```

**Pourquoi `OncePerRequestFilter` ?**
- Garantit que le filtre est exécuté **une seule fois par requête**
- Évite les problèmes de double exécution dans les forward/include de Servlet
- Classe de base recommandée pour les filtres Spring

**Pourquoi pas un `Filter` standard ?**
Un `Filter` Servlet classique peut être exécuté plusieurs fois pour une même requête
si celle-ci est forwardée. `OncePerRequestFilter` s'en assure.

### Constructeur

```java
 private final JwtUtil jwtUtil;

 public JwtAuthenticationFilter(JwtUtil jwtUtil) {
 this.jwtUtil = jwtUtil;
 }
```

Injection par constructeur du service JwtUtil. Spring injecte automatiquement le bean `@Component`.

### Méthode `doFilterInternal()`

```java
 @Override
 protected void doFilterInternal(HttpServletRequest request,
 HttpServletResponse response,
 FilterChain filterChain)
 throws ServletException, IOException {
```

C'est la méthode principale. Elle est appelée pour **chaque requête HTTP**.

#### Étape 1 : Extraire le header Authorization

```java
 String authHeader = request.getHeader("Authorization");
```

Le client envoie le token dans le header HTTP :
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

#### Étape 2 : Vérifier la présence d'un token Bearer

```java
 if (authHeader != null && authHeader.startsWith("Bearer ")) {
```

Deux conditions :
1. Le header `Authorization` est présent (pas null)
2. Il commence par `"Bearer "` (le préfixe standard pour JWT)

**Schéma "Bearer" :** Dans le protocole OAuth 2.0 et JWT, le type de token est "Bearer"
(porteur). Cela signifie : "toute personne qui porte ce token peut accéder aux ressources".

#### Étape 3 : Extraire le token

```java
 String token = authHeader.substring(7);
```

`"Bearer "` fait 7 caractères (B-e-a-r-e-r-espace). On extrait tout ce qui suit.

```
"Bearer eyJhbGciOiJIUzI1NiJ9..."
 substring(7)
```

#### Étape 4 : Valider le token

```java
 if (jwtUtil.estTokenValide(token)) {
```

Vérifie que le token est syntaxiquement correct, signé avec la bonne clé, et non expiré.

#### Étape 5 : Extraire les informations

```java
 String username = jwtUtil.extraireUsername(token);
 String role = jwtUtil.extraireRole(token);
```

Extrait le username (subject) et le rôle (claim personnalisé) du token.

#### Étape 6 : Créer l'objet Authentication

```java
 UsernamePasswordAuthenticationToken authentication =
 new UsernamePasswordAuthenticationToken(
 username,
 null,
 List.of(new SimpleGrantedAuthority("ROLE_" + role))
 );
```

**`UsernamePasswordAuthenticationToken` :** C'est l'implémentation standard de l'interface
`Authentication` dans Spring Security. Elle représente un utilisateur authentifié.

**Constructeur à 3 paramètres (utilisateur authentifié) :**
1. `principal` : l'identifiant de l'utilisateur (`username`)
2. `credentials` : le mot de passe ou secret (`null` car on ne le stocke pas)
3. `authorities` : la liste des autorités/roles

**`SimpleGrantedAuthority("ROLE_" + role)` :**
- Une autorité Spring Security = un droit accordé à l'utilisateur
- Convention : les rôles sont préfixés par `ROLE_`
- Si `role = "ADMIN"` → autorité = `ROLE_ADMIN`
- C'est cette chaîne que `hasRole("ADMIN")` vérifie

**Pourquoi `List.of()` ?**
`List.of()` crée une liste immuable. Il pourrait y avoir plusieurs rôles.
Dans notre cas, un utilisateur a un seul rôle.

#### Étape 7 : Enregistrer dans le SecurityContext

```java
 SecurityContextHolder.getContext().setAuthentication(authentication);
```

**`SecurityContextHolder`** est le conteneur global qui stocke le contexte de sécurité
pour le thread courant. En y plaçant l'authentification, on indique à Spring Security :

> "Cette requête est authentifiée en tant que `username` avec le rôle `ROLE_ADMIN`."

Spring Security utilisera cette information pour :
- Autoriser/refuser l'accès aux endpoints (`hasRole`, `hasAnyRole`)
- Évaluer `@PreAuthorize`
- Rendre disponible l'utilisateur via `SecurityContextHolder.getContext().getAuthentication()`

#### Étape 8 : Continuer la chaîne

```java
 }

 filterChain.doFilter(request, response);
 }
}
```

**`filterChain.doFilter(request, response)`** est **toujours appelé**, même si le token
est absent ou invalide. Cela permet aux filtres suivants de traiter la requête.

Si aucun token valide n'est trouvé :
- Le `SecurityContext` reste vide (pas d'authentification)
- Les règles `hasRole()` et `anyRequest().authenticated()` bloqueront l'accès (403)

---

### AppConfig.java — Configuration des beans

 `labs/lab07-spring-security/src/main/java/com/nexa/secu/config/AppConfig.java`

```java
@Configuration
public class AppConfig {

 @Bean
 public UserDetailsService userDetailsService(UtilisateurRepository repo) {
 return username -> {
 Utilisateur u = repo.findByUsername(username)
 .orElseThrow(() -> new UsernameNotFoundException(
 "Utilisateur introuvable : " + username));
 return org.springframework.security.core.userdetails.User.builder()
 .username(u.getUsername())
 .password(u.getPassword())
 .roles(u.getRole())
 .build();
 };
 }
```

**`UserDetailsService` :** Interface fonctionnelle avec une seule méthode
`loadUserByUsername(String username)`. Spring Security l'appelle pour charger
un utilisateur lors du login.

**`User.builder()` :** Builder fourni par Spring Security pour créer un `UserDetails`.

| Méthode | Valeur | Explication |
|---|---|---|
| `.username(u.getUsername())` | `"admin"` | Identifiant de connexion |
| `.password(u.getPassword())` | `"$2a$10$..."` | Mot de passe hashé (BCrypt) |
| `.roles(u.getRole())` | `"ADMIN"` | Rôle (préfixé ROLE_ automatiquement) |

```java
 @Bean
 public AuthenticationManager authenticationManager(
 AuthenticationConfiguration config) throws Exception {
 return config.getAuthenticationManager();
 }
```

**`AuthenticationManager` :** Bean nécessaire pour le login programmatique (dans AuthController).
`AuthenticationConfiguration.getAuthenticationManager()` retourne le manager configuré
par défaut qui utilise notre `UserDetailsService` et notre `PasswordEncoder`.

```java
 @Bean
 public CommandLineRunner initUsers(UtilisateurRepository repo,
 PasswordEncoder encoder) {
 return args -> {
 if (!repo.existsByUsername("admin")) {
 repo.save(new Utilisateur("admin", encoder.encode("admin123"), "ADMIN"));
 }
 if (!repo.existsByUsername("user")) {
 repo.save(new Utilisateur("user", encoder.encode("user123"), "USER"));
 }
 };
 }
}
```

**`CommandLineRunner` :** Exécuté au démarrage de l'application. Insère des utilisateurs
de test en base si ils n'existent pas déjà.

| Utilisateur | Mot de passe | Rôle |
|---|---|---|
| `admin` | `admin123` | `ADMIN` |
| `user` | `user123` | `USER` |

> Les mots de passe sont **hashés avec BCrypt** avant d'être stockés.
> En base, on ne voit jamais `admin123`, seulement le hash BCrypt.

---

### Utilisateur.java — L'entité JPA

 `labs/lab07-spring-security/src/main/java/com/nexa/secu/entity/Utilisateur.java`

```java
@Entity
@Table(name = "utilisateurs")
public class Utilisateur {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long id;

 @Column(unique = true, nullable = false)
 private String username;

 @Column(nullable = false)
 private String password;

 @Column(nullable = false)
 private String role;

 private boolean actif = true;
 // ... constructeurs, getters, setters
}
```

**Champs de l'entité :**

| Champ | Type | Contrainte | Description |
|---|---|---|---|
| `id` | `Long` | `@Id`, auto-généré | Clé primaire |
| `username` | `String` | `unique`, `nullable=false` | Nom d'utilisateur unique |
| `password` | `String` | `nullable=false` | Mot de passe hashé |
| `role` | `String` | `nullable=false` | Rôle (ADMIN ou USER) |
| `actif` | `boolean` | défaut `true` | Compte actif ou désactivé |

---

### UtilisateurRepository.java — Accès aux données

 `labs/lab07-spring-security/src/main/java/com/nexa/secu/repository/UtilisateurRepository.java`

```java
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
 Optional<Utilisateur> findByUsername(String username);
 boolean existsByUsername(String username);
}
```

**Méthodes dérivées Spring Data :**

| Méthode | Requête JPQL générée | Utilisation |
|---|---|---|
| `findByUsername(String)` | `SELECT u FROM Utilisateur u WHERE u.username = ?1` | Login, chargement utilisateur |
| `existsByUsername(String)` | `SELECT COUNT(u) > 0 FROM Utilisateur u WHERE u.username = ?1` | Vérifier si le username est déjà pris |

---

### UtilisateurService.java — Logique métier

 `labs/lab07-spring-security/src/main/java/com/nexa/secu/service/UtilisateurService.java`

```java
@Service
public class UtilisateurService {

 private final UtilisateurRepository repository;
 private final PasswordEncoder passwordEncoder;

 public Utilisateur creer(String username, String password, String role) {
 if (repository.existsByUsername(username)) {
 throw new IllegalArgumentException("Nom d'utilisateur déjà pris");
 }
 Utilisateur u = new Utilisateur(username, passwordEncoder.encode(password), role);
 return repository.save(u);
 }
}
```

**Création d'un utilisateur :**
1. Vérifie que le username n'est pas déjà pris
2. Hash le mot de passe avec BCrypt
3. Sauvegarde l'utilisateur en base

> **Règle d'or :** Ne **jamais** stocker un mot de passe en clair. Toujours le hasher.

---

### AuthController.java — Le flux de login

 `labs/lab07-spring-security/src/main/java/com/nexa/secu/controller/AuthController.java`

### Structure

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {

 private final AuthenticationManager authManager;
 private final JwtUtil jwtUtil;
 private final UserDetailsService userDetailsService;
 private final PasswordEncoder passwordEncoder;
```

4 dépendances injectées :
- `AuthenticationManager` : valide les credentials
- `JwtUtil` : génère le token
- `UserDetailsService` : charge l'utilisateur
- `PasswordEncoder` : non utilisé directement ici (disponible pour d'autres fonctionnalités)

### La méthode `login()`

```java
 @PostMapping("/login")
 public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
```

L'endpoint reçoit un JSON :
```json
{
 "username": "admin",
 "password": "admin123"
}
```

#### Étape 1 : Extraire credentials

```java
 try {
 String username = request.get("username");
 String password = request.get("password");
```

#### Étape 2 : Authentifier

```java
 authManager.authenticate(
 new UsernamePasswordAuthenticationToken(username, password));
```

**Ce qu'il se passe en interne :**
1. `AuthenticationManager` reçoit le token d'authentification non vérifié
2. Il le passe au `AuthenticationProvider` (DaoAuthenticationProvider par défaut)
3. Le provider appelle `UserDetailsService.loadUserByUsername(username)`
4. Il compare le mot de passe fourni avec le hash en base via `PasswordEncoder.matches()`
5. Si ça correspond → authentification réussie
6. Si non → `AuthenticationException` levée → catch → 401

#### Étape 3 : Récupérer le rôle

```java
 UserDetails userDetails = userDetailsService.loadUserByUsername(username);
 String role = userDetails.getAuthorities().stream()
 .findFirst().orElse(new SimpleGrantedAuthority("ROLE_USER"))
 .getAuthority().replace("ROLE_", "");
```

**Analyse :**
- Recharge l'utilisateur pour obtenir ses autorités
- `.getAuthorities()` → collection de `GrantedAuthority`
- `.findFirst()` → prend la première (un seul rôle dans notre cas)
- `.getAuthority()` → `"ROLE_ADMIN"` ou `"ROLE_USER"`
- `.replace("ROLE_", "")` → `"ADMIN"` ou `"USER"` (nettoie pour le claim JWT)

#### Étape 4 : Générer le token

```java
 String token = jwtUtil.genererToken(username, role);
 return ResponseEntity.ok(Map.of("token", token));
```

Réponse JSON :
```json
{
 "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJBRE1JTiJ9..."
}
```

#### Étape 5 : Gérer l'échec

```java
 } catch (AuthenticationException e) {
 return ResponseEntity.status(401)
 .body(Map.of("error", "Identifiants invalides"));
 }
 }
}
```

En cas d'échec d'authentification : HTTP 401 Unauthorized avec un message d'erreur.

### Flux complet de login

```
1. Client envoie POST /api/auth/login {"username":"admin","password":"admin123"}
2. AuthController.login() extrait username/password
3. AuthenticationManager.authenticate() vérifie les credentials
4. UserDetailsService charge l'utilisateur depuis la DB
5. PasswordEncoder.matches() compare les mots de passe
6. Si OK → génération du JWT avec username + rôle
7. Réponse : 200 OK {token: "..."}

Requêtes suivantes :
8. Client envoie GET /api/produits/admin avec header Authorization: Bearer <token>
9. JwtAuthenticationFilter extrait et valide le token
10. Extrait username et rôle du payload JWT
11. Crée UsernamePasswordAuthenticationToken dans SecurityContextHolder
12. SecurityFilterChain vérifie la règle hasRole("ADMIN")
13. Si OK → la requête atteint le contrôleur
```

---

### ProduitController.java — @PreAuthorize

 `labs/lab07-spring-security/src/main/java/com/nexa/secu/controller/ProduitController.java`

```java
@RestController
@RequestMapping("/api/produits")
public class ProduitController {

 @GetMapping("/public")
 public Map<String, String> listePublique() {
 return Map.of("message", "Liste publique des produits accessible à tous");
 }
```

**Endpoint public :** aucun contrôle d'accès. N'importe qui peut y accéder.
La règle `permitAll()` dans `SecurityConfig` le permet.

```java
 @GetMapping("/admin")
 @PreAuthorize("hasRole('ADMIN')")
 public Map<String, String> espaceAdmin() {
 return Map.of("message", "Espace administration — réservé aux ADMIN");
 }
```

**`@PreAuthorize("hasRole('ADMIN')")` :**
- Exécuté **avant** la méthode du contrôleur
- Évalue l'expression SpEL (Spring Expression Language) `hasRole('ADMIN')`
- Si l'utilisateur authentifié n'a pas `ROLE_ADMIN` → **403 Forbidden**
- Si OK → la méthode s'exécute normalement

```java
 @GetMapping("/user")
 @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
 public Map<String, String> espaceUtilisateur() {
 return Map.of("message", "Espace utilisateur — accessible aux ADMIN et USER");
 }
}
```

**`@PreAuthorize("hasAnyRole('ADMIN', 'USER')")` :**
- Vérifie que l'utilisateur a **au moins un** des rôles listés
- `ADMIN` **ou** `USER` → OK
- Ni l'un ni l'autre → 403

### Double couche de sécurité

Dans cette application, la sécurité est appliquée à **deux niveaux** :

| Niveau | Mécanisme | Fichier |
|---|---|---|
| **Configuration** | `SecurityFilterChain` avec `authorizeHttpRequests()` | `SecurityConfig.java` |
| **Déclaratif** | `@PreAuthorize` sur les méthodes | `ProduitController.java` |

**Pourquoi les deux ?**
- `SecurityFilterChain` pour des règles **globales par URL/HTTP method**
- `@PreAuthorize` pour des règles **fines par méthode**

Exemple avec `/api/produits/admin` :
1. `SecurityFilterChain` : `GET /api/produits/**` → `permitAll()` → autorisé
2. `@PreAuthorize("hasRole('ADMIN')")` → vérification supplémentaire → seuls ADMIN passent

### @PreAuthorize vs @PostAuthorize

| Annotation | Moment d'exécution | Usage |
|---|---|---|
| `@PreAuthorize` | **Avant** la méthode | Vérifier l'accès avant d'exécuter la logique |
| `@PostAuthorize` | **Après** la méthode | Vérifier le résultat (ex: l'utilisateur est propriétaire de l'objet retourné) |

Exemple de `@PostAuthorize` :
```java
@GetMapping("/{id}")
@PostAuthorize("returnObject.createdBy == authentication.name")
public Document getDocument(@PathVariable Long id) {
 return documentService.findById(id);
}
```

---

### application.properties — Configuration H2

 `labs/lab07-spring-security/src/main/resources/application.properties`

```properties
spring.application.name=lab07-spring-security
spring.datasource.url=jdbc:h2:mem:secudb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false
```

| Propriété | Valeur | Explication |
|---|---|---|
| `datasource.url` | `jdbc:h2:mem:secudb` | Base H2 en mémoire, nommée `secudb` |
| `ddl-auto` | `create-drop` | Crée les tables au démarrage, les détruit à l'arrêt |
| `show-sql` | `false` | N'affiche pas les requêtes SQL dans la console |

---

### Tests — JwtUtilTest.java

 `labs/lab07-spring-security/src/test/java/com/nexa/secu/security/JwtUtilTest.java`

### Structure du test

```java
@DisplayName("Tests du JwtUtil")
class JwtUtilTest {

 private final JwtUtil jwtUtil = new JwtUtil();
```

**Pas de mock** : on teste le `JwtUtil` réel. La classe n'a pas de dépendances externes
(la clé est hardcodée en constante), donc un test unitaire pur suffit.

### Groupe 1 : Génération et validation

```java
 @Nested
 @DisplayName("Génération et validation de token")
 class GenerationValidation {

 @Test
 @DisplayName("Un token généré est valide")
 void tokenGenereEstValide() {
 String token = jwtUtil.genererToken("alice", "ADMIN");
 assertNotNull(token);
 assertFalse(token.isEmpty());
 assertTrue(jwtUtil.estTokenValide(token));
 }
```

**Vérifications :**
1. Le token n'est pas null
2. Le token n'est pas vide
3. Le token passe la validation (signature OK, pas expiré)

```java
 @Test
 @DisplayName("Extraire le username d'un token valide")
 void extraireUsername() {
 String token = jwtUtil.genererToken("bob", "USER");
 assertEquals("bob", jwtUtil.extraireUsername(token));
 }
```

Vérifie l'extraction du sujet (subject → username).

```java
 @Test
 @DisplayName("Extraire le rôle d'un token valide")
 void extraireRole() {
 String token = jwtUtil.genererToken("charlie", "ADMIN");
 assertEquals("ADMIN", jwtUtil.extraireRole(token));
 }
```

Vérifie l'extraction du claim personnalisé `role`.

### Groupe 2 : Tokens invalides

```java
 @Nested
 @DisplayName("Tokens invalides")
 class TokensInvalides {

 @Test
 @DisplayName("Token vide ou null est invalide")
 void tokenVideNullInvalide() {
 assertFalse(jwtUtil.estTokenValide(null));
 assertFalse(jwtUtil.estTokenValide(""));
 }
```

Test des cas limites : null et chaîne vide. La méthode `estTokenValide` doit retourner
`false` sans lever d'exception.

```java
 @Test
 @DisplayName("Token modifié (tampered) est invalide")
 void tokenModifieInvalide() {
 String token = jwtUtil.genererToken("dave", "USER");
 String tokenModifie = token.substring(0, token.length() - 3) + "xxx";
 assertFalse(jwtUtil.estTokenValide(tokenModifie));
 }
```

**Test d'intégrité :** Si quelqu'un modifie le token (ne serait-ce qu'un caractère),
la signature ne correspondra plus et le token sera rejeté.

```java
 @Test
 @DisplayName("Token aléatoire est invalide")
 void tokenAleatoireInvalide() {
 assertFalse(jwtUtil.estTokenValide("eyJhbGciOiJIUzI1NiJ9.abc.def"));
 }
```

Même un token qui ressemble syntaxiquement à un JWT mais avec un contenu aléatoire
doit être rejeté.

```java
 @Test
 @DisplayName("Tokens différents pour le même utilisateur")
 void tokensDifferentsPourMemeUtilisateur() {
 String t1 = jwtUtil.genererToken("eve", "USER");
 String t2 = jwtUtil.genererToken("eve", "USER");
 assertNotEquals(t1, t2);
 }
```

**Pourquoi les tokens sont différents ?** Le claim `iat` (issued at) change à chaque
appel car il contient le timestamp courant. Donc le hash global est différent.

---

### Tests — SecurityIntegrationTest.java

 `labs/lab07-spring-security/src/test/java/com/nexa/secu/controller/SecurityIntegrationTest.java`

### Configuration du test

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DisplayName("Tests de sécurité — intégration")
class SecurityIntegrationTest {

 @Autowired private MockMvc mockMvc;
 @Autowired private JwtUtil jwtUtil;
```

**Annotations :**
- `@SpringBootTest` : démarre le contexte Spring complet (tous les beans)
- `webEnvironment = RANDOM_PORT` : démarre le serveur sur un port aléatoire (tests plus isolés)
- `@AutoConfigureMockMvc` : configure MockMvc automatiquement

### Groupe 1 : Accès public

```java
 @Nested
 @DisplayName("Accès public")
 class AccesPublic {

 @Test
 @DisplayName("Sans token : l'endpoint public est accessible")
 void sansTokenPublicAccessible() throws Exception {
 mockMvc.perform(get("/api/produits/public"))
 .andExpect(status().isOk());
 }
```

**Ce test vérifie :** l'endpoint `/api/produits/public` est bien accessible sans aucune
authentification. La règle `permitAll()` fonctionne.

```java
 @Test
 @DisplayName("Avec token USER : l'endpoint public est accessible")
 void avecTokenUserPublicAccessible() throws Exception {
 String token = jwtUtil.genererToken("user", "USER");
 mockMvc.perform(get("/api/produits/public")
 .header("Authorization", "Bearer " + token))
 .andExpect(status().isOk());
 }
```

Même avec un token, l'endpoint public reste accessible. Pas de régression.

### Groupe 2 : Contrôle d'accès par rôle

```java
 @Nested
 @DisplayName("Contrôle d'accès par rôle")
 class ControleAcces {

 @Test
 @DisplayName("Token ADMIN peut accéder à l'espace admin")
 void adminAccedeAdmin() throws Exception {
 String token = jwtUtil.genererToken("admin", "ADMIN");
 mockMvc.perform(get("/api/produits/admin")
 .header("Authorization", "Bearer " + token))
 .andExpect(status().isOk());
 }
```

**ADMIN accède à l'espace admin** : token avec rôle ADMIN → `@PreAuthorize("hasRole('ADMIN')")` → OK.

```java
 @Test
 @DisplayName("Token USER ne peut PAS accéder à l'espace admin → 403")
 void userNePeutPasAccederAdmin() throws Exception {
 String token = jwtUtil.genererToken("user", "USER");
 mockMvc.perform(get("/api/produits/admin")
 .header("Authorization", "Bearer " + token))
 .andExpect(status().isForbidden());
 }
```

**USER ne peut pas accéder à l'espace admin** : token USER → `hasRole('ADMIN')` → 403 Forbidden.
C'est le test de contrôle d'accès le plus important.

```java
 @Test
 @DisplayName("Token USER peut accéder à l'espace user")
 void userAccedeUser() throws Exception {
 String token = jwtUtil.genererToken("user", "USER");
 mockMvc.perform(get("/api/produits/user")
 .header("Authorization", "Bearer " + token))
 .andExpect(status().isOk());
 }
```

USER entre dans l'espace user car `hasAnyRole('ADMIN', 'USER')` inclut USER.

```java
 @Test
 @DisplayName("Token ADMIN peut accéder à l'espace user (hasAnyRole)")
 void adminAccedeUser() throws Exception {
 String token = jwtUtil.genererToken("admin", "ADMIN");
 mockMvc.perform(get("/api/produits/user")
 .header("Authorization", "Bearer " + token))
 .andExpect(status().isOk());
 }
```

ADMIN entre aussi dans l'espace user car `hasAnyRole('ADMIN', 'USER')` inclut ADMIN.

### Groupe 3 : Authentification

```java
 @Nested
 @DisplayName("Authentification")
 class Authentification {

 @Test
 @DisplayName("Sans token : endpoint sécurisé → 403 Forbidden")
 void sansTokenEndpointSecurise() throws Exception {
 mockMvc.perform(get("/api/produits/admin"))
 .andExpect(status().isForbidden());
 }
```

Aucun token → pas d'authentification → un endpoint protégé retourne 403.

```java
 @Test
 @DisplayName("Token invalide → 403 Forbidden")
 void tokenInvalide() throws Exception {
 mockMvc.perform(get("/api/produits/user")
 .header("Authorization", "Bearer token_invalide"))
 .andExpect(status().isForbidden());
 }
```

Un token invalide est traité comme "pas authentifié" → 403.

```java
 @Test
 @DisplayName("Header Authorization absent → 403")
 void headerAuthorizationAbsent() throws Exception {
 mockMvc.perform(get("/api/produits/user"))
 .andExpect(status().isForbidden());
 }
```

Pas de header `Authorization` du tout → 403.

```java
 @Test
 @DisplayName("Header Authorization format incorrect → 403")
 void headerFormatIncorrect() throws Exception {
 String token = jwtUtil.genererToken("user", "USER");
 mockMvc.perform(get("/api/produits/user")
 .header("Authorization", token)) // Sans "Bearer "
 .andExpect(status().isForbidden());
 }
```

Le token est valide mais le préfixe `Bearer ` est absent → le filtre ignore le header → 403.

### Tableau récapitulatif des scénarios de test

| Scénario | Token | Endpoint | Résultat |
|---|---|---|---|
| Public, sans token | Aucun | `/public` | 200 OK |
| Public, token USER | USER | `/public` | 200 OK |
| Admin, token ADMIN | ADMIN | `/admin` | 200 OK |
| Admin, token USER | USER | `/admin` | **403 Forbidden** |
| User, token USER | USER | `/user` | 200 OK |
| User, token ADMIN | ADMIN | `/user` | 200 OK |
| Sans token | Aucun | `/admin` | 403 |
| Token invalide | `"token_invalide"` | `/user` | 403 |
| Sans header Auth | Aucun | `/user` | 403 |
| Format incorrect | Token sans `Bearer ` | `/user` | 403 |

---

## PARTIE 3 -- LAB (20 min)

### Objectif

Ajouter un rôle **MODERATOR** et un endpoint `/api/produits/moderate` accessible
uniquement aux rôles **MODERATOR** et **ADMIN**.

### Consignes

### Étape 1 : Créer l'utilisateur MODERATOR

Dans `AppConfig.initUsers()`, ajouter un utilisateur de test :

```java
if (!repo.existsByUsername("moderator")) {
 repo.save(new Utilisateur("moderator", encoder.encode("mod123"), "MODERATOR"));
}
```

### Étape 2 : Ajouter l'endpoint dans ProduitController

```java
@GetMapping("/moderate")
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
public Map<String, String> espaceModeration() {
 return Map.of("message", "Espace modération — réservé aux ADMIN et MODERATOR");
}
```

### Étape 3 : Mettre à jour SecurityConfig

S'assurer que la règle `GET /api/produits/**` permet l'accès (déjà `permitAll()` — la sécurité
est gérée par `@PreAuthorize`).

### Étape 4 : Écrire les tests

Dans `SecurityIntegrationTest`, ajouter une nouvelle classe imbriquée :

```java
@Nested
@DisplayName("Rôle MODERATOR")
class RoleModerator {

 @Test
 @DisplayName("Token MODERATOR peut accéder à l'espace modération")
 void moderatorAccedeModeration() throws Exception {
 String token = jwtUtil.genererToken("moderator", "MODERATOR");
 mockMvc.perform(get("/api/produits/moderate")
 .header("Authorization", "Bearer " + token))
 .andExpect(status().isOk());
 }

 @Test
 @DisplayName("Token ADMIN peut accéder à l'espace modération")
 void adminAccedeModeration() throws Exception {
 String token = jwtUtil.genererToken("admin", "ADMIN");
 mockMvc.perform(get("/api/produits/moderate")
 .header("Authorization", "Bearer " + token))
 .andExpect(status().isOk());
 }

 @Test
 @DisplayName("Token USER ne peut PAS accéder à l'espace modération → 403")
 void userNePeutPasAccederModeration() throws Exception {
 String token = jwtUtil.genererToken("user", "USER");
 mockMvc.perform(get("/api/produits/moderate")
 .header("Authorization", "Bearer " + token))
 .andExpect(status().isForbidden());
 }

 @Test
 @DisplayName("Sans token : espace modération → 403")
 void sansTokenModeration() throws Exception {
 mockMvc.perform(get("/api/produits/moderate"))
 .andExpect(status().isForbidden());
 }
}
```

### Critères de réussite

- [ ] L'endpoint `/api/produits/moderate` retourne 200 avec les tokens ADMIN et MODERATOR
- [ ] L'endpoint retourne 403 avec un token USER
- [ ] L'endpoint retourne 403 sans token
- [ ] Tous les tests existants continuent de passer
- [ ] La commande `mvn test` est verte

---

## FICHE MEMO -- Module 7

### Annotations de configuration Spring Security

| Annotation | Rôle |
|---|---|
| `@Configuration` | Classe de configuration Spring |
| `@EnableWebSecurity` | Active Spring Security web |
| `@EnableMethodSecurity` | Active `@PreAuthorize` / `@PostAuthorize` |
| `@Bean` | Déclare un bean géré par Spring |

### Configuration SecurityFilterChain

| Méthode | Effet |
|---|---|
| `csrf(csrf -> csrf.disable())` | Désactive CSRF (API stateless) |
| `sessionManagement(s -> s.sessionCreationPolicy(STATELESS))` | Pas de session HTTP |
| `authorizeHttpRequests(auth -> ...)` | Règles d'accès |
| `requestMatchers("/path").permitAll()` | Accès public |
| `requestMatchers(...).hasRole("ADMIN")` | Réservé ADMIN |
| `requestMatchers(...).hasAnyRole("A","B")` | Rôle A ou B |
| `requestMatchers(HttpMethod.GET, "/**").permitAll()` | Méthode HTTP spécifique |
| `anyRequest().authenticated()` | Tout le reste nécessite auth |
| `addFilterBefore(filter, class)` | Insère un filtre personnalisé |

### JWT avec JJWT 0.12.x

| Opération | Code |
|---|---|
| Créer une clé | `Keys.hmacShaKeyFor(secret.getBytes(UTF_8))` |
| Générer un token | `Jwts.builder().subject().claim().issuedAt().expiration().signWith(key).compact()` |
| Parser un token | `Jwts.parser().verifyWith(key).build().parseSignedClaims(token)` |
| Extraire le sujet | `parseToken(token).getPayload().getSubject()` |
| Extraire un claim | `parseToken(token).getPayload().get("role", String.class)` |

### Authentification JWT (JwtAuthenticationFilter)

| Étape | Code |
|---|---|
| Extraire header | `request.getHeader("Authorization")` |
| Vérifier préfixe | `header.startsWith("Bearer ")` |
| Extraire token | `header.substring(7)` |
| Créer auth | `new UsernamePasswordAuthenticationToken(user, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)))` |
| Stocker dans contexte | `SecurityContextHolder.getContext().setAuthentication(auth)` |

### Sécurité déclarative

| Annotation | Expression |
|---|---|
| `@PreAuthorize("hasRole('ADMIN')")` | Vérifié avant exécution |
| `@PreAuthorize("hasAnyRole('ADMIN', 'USER')")` | Un des rôles |
| `@PostAuthorize("returnObject.owner == authentication.name")` | Vérifié après exécution |

### Tests de sécurité

| Annotation/Outil | Usage |
|---|---|
| `@SpringBootTest` | Contexte Spring complet |
| `@AutoConfigureMockMvc` | Configurer MockMvc |
| `@WithMockUser(roles = "ADMIN")` | Simuler un utilisateur avec rôle |
| `@WithAnonymousUser` | Simuler un utilisateur non authentifié |
| `.header("Authorization", "Bearer " + token)` | Envoyer un JWT |
| `.andExpect(status().isOk())` | Vérifier 200 |
| `.andExpect(status().isForbidden())` | Vérifier 403 |
| `.andExpect(status().isUnauthorized())` | Vérifier 401 |

### Codes HTTP en sécurité

| Code | Signification |
|---|---|
| 200 OK | Accès autorisé |
| 401 Unauthorized | Non authentifié (login échoué) |
| 403 Forbidden | Authentifié mais pas les droits suffisants |
