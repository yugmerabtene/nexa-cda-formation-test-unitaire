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
@EnableMethodSecurity( // Active la sécurité déclarative par annotations
 prePostEnabled = true, // Active @PreAuthorize / @PostAuthorize : vérification avant/après exécution
 securedEnabled = true, // Active @Secured : annotation de rôle plus simple
 jsr250Enabled = true  // Active @RolesAllowed : standard JSR-250 pour la compatibilité
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
.csrf(csrf -> csrf.disable()) // Désactive CSRF car API stateless : pas de cookie de session à protéger
```

> **Ne jamais désactiver CSRF pour une application web avec sessions (Thymeleaf, JSP).**
> La désactivation est **spécifique aux APIs REST stateless.**

### 4.2 `sessionManagement()`

```java
 .sessionManagement(session -> // Configure la gestion des sessions HTTP
 session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Aucune session HTTP : chaque requête est autonome via JWT (scalable)
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
 .authorizeHttpRequests(auth -> auth // Définit les règles d'accès par URL et méthode HTTP
 .requestMatchers("/api/auth/**").permitAll() // Login public : nécessaire pour que quiconque puisse s'authentifier
 .requestMatchers(HttpMethod.GET, "/api/produits/**").permitAll() // Consultation des produits ouverte à tous
 .requestMatchers(HttpMethod.POST, "/api/produits/**").hasRole("ADMIN") // Création réservée aux ADMIN
 .requestMatchers(HttpMethod.PUT, "/api/produits/**").hasRole("ADMIN") // Modification réservée aux ADMIN
 .requestMatchers(HttpMethod.DELETE, "/api/produits/**").hasRole("ADMIN") // Suppression réservée aux ADMIN
 .anyRequest().authenticated() // Tout autre endpoint nécessite une authentification (peu importe le rôle)
) // L'ordre des règles est important : la première correspondante est appliquée, les suivantes ignorées
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
 .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class); // Insère le filtre JWT avant le filtre formulaire : si JWT valide, on court-circuite l'auth standard
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
package com.nexa.secu.config; // Package de configuration Spring

import com.nexa.secu.security.JwtAuthenticationFilter; // Filtre JWT personnalisé à injecter dans la chaîne
import org.springframework.context.annotation.Bean; // Déclare un bean géré par le conteneur Spring
import org.springframework.context.annotation.Configuration; // Marque une classe de configuration Spring
import org.springframework.http.HttpMethod; // Énumération des méthodes HTTP (GET, POST, etc.)
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity; // Active @PreAuthorize
import org.springframework.security.config.annotation.web.builders.HttpSecurity; // Builder pour configurer la sécurité HTTP
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity; // Active Spring Security web
import org.springframework.security.config.http.SessionCreationPolicy; // Politique de création de session
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // Implémentation BCrypt du hashage
import org.springframework.security.crypto.password.PasswordEncoder; // Interface pour le hashage des mots de passe
import org.springframework.security.web.SecurityFilterChain; // Chaîne de filtres de sécurité
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter; // Filtre d'auth standard par formulaire
```

**Annotations de classe :**

| Annotation | Rôle |
|---|---|
| `@Configuration` | Déclare une classe de configuration Spring (les `@Bean` seront gérés par le conteneur) |
| `@EnableWebSecurity` | Active Spring Security et désactive la configuration automatique par défaut |
| `@EnableMethodSecurity` | Active `@PreAuthorize` et `@PostAuthorize` sur les méthodes des contrôleurs |

```java
@Configuration // Déclare une classe de configuration Spring : les @Bean seront gérés par le conteneur
@EnableWebSecurity // Active Spring Security et désactive la configuration automatique par défaut
@EnableMethodSecurity // Active @PreAuthorize et @PostAuthorize sur les méthodes des contrôleurs
public class SecurityConfig { // Configuration centrale de la sécurité de l'API

 private final JwtAuthenticationFilter jwtFilter; // Référence vers le filtre JWT personnalisé

 // Injection par constructeur du filtre JWT
 public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
 this.jwtFilter = jwtFilter; // Spring injecte automatiquement le bean @Component JwtAuthenticationFilter
 }
```

### La méthode `filterChain()` — le cœur de la configuration

```java
 @Bean // Déclare un bean SecurityFilterChain : c'est le cœur de la configuration de sécurité
 public SecurityFilterChain filterChain(HttpSecurity http) throws Exception { // HttpSecurity est un builder pour configurer la sécurité web
```

`HttpSecurity http` est un objet builder qui permet de configurer la sécurité web.
Chaque méthode retourne `http` pour le chaînage (pattern builder).

#### Étape 1 : Désactiver CSRF

```java
 http // Chaînage du builder : chaque méthode retourne http pour continuer la configuration
 .csrf(csrf -> csrf.disable()) // Désactive CSRF car API stateless : pas de cookie de session à protéger
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
 return http.build(); // Construit la SecurityFilterChain à partir de toute la configuration chaînée
 } // Fin de la méthode filterChain
```

### Le bean PasswordEncoder

```java
 @Bean // Déclare un bean PasswordEncoder pour le hashage des mots de passe
 public PasswordEncoder passwordEncoder() {
 return new BCryptPasswordEncoder(); // BCrypt : hash à sens unique avec salage automatique, résistant aux rainbow tables
 }
} // Fin de SecurityConfig
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
package com.nexa.secu.security; // Package des composants de sécurité JWT

import io.jsonwebtoken.*; // API JJWT : construction, parsing et validation des tokens JWT
import io.jsonwebtoken.security.Keys; // Fabrique de clés cryptographiques pour HMAC
import org.springframework.stereotype.Component; // Annotation pour déclarer un bean Spring générique

import javax.crypto.SecretKey; // Clé secrète pour la signature HMAC-SHA256
import java.nio.charset.StandardCharsets; // Encodage UTF-8 pour convertir la chaîne secrète en bytes
import java.util.Date; // Timestamps pour les dates d'émission et d'expiration du token
```

**Imports clés :**
- `io.jsonwebtoken.*` : classes JJWT pour construire et parser les tokens
- `io.jsonwebtoken.security.Keys` : fabrique de clés cryptographiques
- `javax.crypto.SecretKey` : clé secrète pour HMAC-SHA256

```java
@Component // Bean Spring : disponible pour injection de dépendance dans toute l'application
public class JwtUtil { // Utilitaire de génération, extraction et validation de tokens JWT
```

`@Component` : rend cette classe disponible pour l'injection de dépendances.
On pourra faire `@Autowired private JwtUtil jwtUtil;` n'importe où.

### Constantes et clé secrète

```java
 private static final String SECRET = // Clé secrète partagée : doit faire au moins 256 bits (32 octets) pour HS256
 "cette-cle-est-un-secret-tres-long-dau-moins-256-bits-pour-hs256"; // 56 caractères UTF-8 = 448 bits > 256 bits requis
 private static final long EXPIRATION_MS = 3600000; // Durée de validité : 1 heure en millisecondes (60 * 60 * 1000)

 private final SecretKey key; // Clé cryptographique pour signer et vérifier les tokens

 public JwtUtil() { // Constructeur : fabrique la clé à partir de la chaîne secrète
 this.key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)); // Convertit la chaîne en clé HMAC-SHA256
 } // En production, externaliser la clé (variable d'environnement, Vault)
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
 public String genererToken(String username, String role) { // Génère un token JWT pour un utilisateur avec son rôle
 return Jwts.builder() // Crée un builder JJWT (design pattern builder)
 .subject(username) // Définit le sujet (claim standard "sub") : identifiant unique de l'utilisateur
 .claim("role", role) // Ajoute un claim personnalisé "role" : ADMIN ou USER (claim non standard)
 .issuedAt(new Date()) // Horodatage d'émission (claim "iat") : timestamp actuel en millisecondes
 .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS)) // Date d'expiration (claim "exp") : maintenant + 1h
 .signWith(key) // Signe le token avec la clé secrète (HS256 par défaut) : garantit l'intégrité
 .compact(); // Sérialise en chaîne format header.payload.signature (Base64URL encodé)
 } // Le token est prêt à être envoyé au client
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
 public String extraireUsername(String token) { // Extrait le nom d'utilisateur (subject) d'un token JWT
 return parseToken(token) // Parse et vérifie le token (signature, expiration)
 .getPayload() // Récupère le payload (les claims)
 .getSubject(); // Extrait le claim standard "sub" : identifiant de l'utilisateur
 } // Retourne le username stocké dans le token
```

**Chaîne d'appels :**
1. `parseToken(token)` → retourne le `Jws<Claims>` (token parsé et vérifié)
2. `.getPayload()` → extrait le payload (Claims)
3. `.getSubject()` → extrait le sujet (username/email)

### Méthode `extraireRole()`

```java
 public String extraireRole(String token) { // Extrait le rôle personnalisé d'un token JWT
 return parseToken(token) // Parse et vérifie le token
 .getPayload() // Récupère le payload
 .get("role", String.class); // Extrait le claim personnalisé "role" (conversion automatique en String)
 } // "role" est un claim non standard contrairement à "sub" qui est défini par la RFC 7519
```

`.get("role", String.class)` : extrait un claim personnalisé par son nom, avec le type attendu.
Contrairement à `getSubject()` qui est standard, `"role"` est un claim personnalisé.

### Méthode `estTokenValide()`

```java
 public boolean estTokenValide(String token) { // Vérifie si un token est syntaxiquement correct, signé et non expiré
 try { // Tentative de parsing : si le token est valide, aucune exception n'est levée
 parseToken(token); // Délègue à la méthode privée qui parse et vérifie la signature
 return true; // Token valide : signature OK, pas expiré, bien formé
 } catch (JwtException | IllegalArgumentException e) { // Attrape toutes les exceptions JWT possibles
 return false; // Token invalide : malformé, expiré, signature erronée, ou null/vide
 } // Plutôt que de laisser l'exception se propager, on retourne un booléen propre
 } // Exception types : ExpiredJwtException, MalformedJwtException, SignatureException, etc.
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
 private Claims parseToken(String token) { // Parse et vérifie un token JWT, retourne les claims (payload)
 return Jwts.parser() // Crée un parser JJWT pour décoder et valider le token
 .verifyWith(key) // Définit la clé secrète pour vérifier la signature HMAC
 .build() // Construit le parser avec la configuration ci-dessus
 .parseSignedClaims(token) // Parse le token et vérifie la signature → retourne Jws<Claims>
 .getPayload(); // Extrait le payload (corps du token contenant les claims : sub, role, iat, exp)
 } // Si la signature est invalide ou le token expiré, une exception JwtException est levée
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
package com.nexa.secu.security; // Package des composants de sécurité

import jakarta.servlet.FilterChain; // Chaîne de filtres Servlet : permet de passer à la requête suivante
import jakarta.servlet.ServletException; // Exception levée par les filtres Servlet
import jakarta.servlet.http.HttpServletRequest; // Requête HTTP entrante
import jakarta.servlet.http.HttpServletResponse; // Réponse HTTP sortante
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; // Implémentation standard d'Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority; // Autorité simple (rôle) pour Spring Security
import org.springframework.security.core.context.SecurityContextHolder; // Contexte de sécurité du thread courant
import org.springframework.stereotype.Component; // Annotation de bean Spring générique
import org.springframework.web.filter.OncePerRequestFilter; // Filtre garantissant une seule exécution par requête HTTP

import java.io.IOException; // Exception d'entrée/sortie pour les filtres
import java.util.List; // Liste immuable pour les autorités de l'utilisateur
```

### OncePerRequestFilter

```java
@Component // Bean Spring : ce filtre sera injecté dans SecurityConfig
public class JwtAuthenticationFilter extends OncePerRequestFilter { // OncePerRequestFilter : garanti une seule exécution par requête HTTP
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
 private final JwtUtil jwtUtil; // Dépendance vers l'utilitaire JWT pour la validation et l'extraction

 public JwtAuthenticationFilter(JwtUtil jwtUtil) { // Injection par constructeur : Spring fournit le bean JwtUtil
 this.jwtUtil = jwtUtil; // Stocke la référence pour une utilisation dans doFilterInternal
 }
```

Injection par constructeur du service JwtUtil. Spring injecte automatiquement le bean `@Component`.

### Méthode `doFilterInternal()`

```java
 @Override // Redéfinit la méthode de filtrage de OncePerRequestFilter
 protected void doFilterInternal(HttpServletRequest request, // Requête HTTP entrante
 HttpServletResponse response, // Réponse HTTP sortante
 FilterChain filterChain) // Chaîne des filtres restants à exécuter
 throws ServletException, IOException { // Exceptions déclarées par l'interface Filter
```

C'est la méthode principale. Elle est appelée pour **chaque requête HTTP**.

#### Étape 1 : Extraire le header Authorization

```java
 String authHeader = request.getHeader("Authorization"); // Récupère le header HTTP "Authorization" de la requête
```

Le client envoie le token dans le header HTTP :
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

#### Étape 2 : Vérifier la présence d'un token Bearer

```java
 if (authHeader != null && authHeader.startsWith("Bearer ")) { // Vérifie la présence et le format : "Bearer <token>"
```

Deux conditions :
1. Le header `Authorization` est présent (pas null)
2. Il commence par `"Bearer "` (le préfixe standard pour JWT)

**Schéma "Bearer" :** Dans le protocole OAuth 2.0 et JWT, le type de token est "Bearer"
(porteur). Cela signifie : "toute personne qui porte ce token peut accéder aux ressources".

#### Étape 3 : Extraire le token

```java
 String token = authHeader.substring(7); // Extrait le token JWT après les 7 caractères de "Bearer " (B-e-a-r-e-r- -)
```

`"Bearer "` fait 7 caractères (B-e-a-r-e-r-espace). On extrait tout ce qui suit.

```
"Bearer eyJhbGciOiJIUzI1NiJ9..."
 substring(7)
```

#### Étape 4 : Valider le token

```java
 if (jwtUtil.estTokenValide(token)) { // Valide le token : signature correcte, pas expiré, bien formé
```

Vérifie que le token est syntaxiquement correct, signé avec la bonne clé, et non expiré.

#### Étape 5 : Extraire les informations

```java
 String username = jwtUtil.extraireUsername(token); // Extrait le nom d'utilisateur depuis le payload JWT (claim "sub")
 String role = jwtUtil.extraireRole(token); // Extrait le rôle depuis le payload JWT (claim personnalisé "role")
```

Extrait le username (subject) et le rôle (claim personnalisé) du token.

#### Étape 6 : Créer l'objet Authentication

```java
 UsernamePasswordAuthenticationToken authentication = // Crée l'objet Authentication pour Spring Security
 new UsernamePasswordAuthenticationToken( // 3 paramètres = utilisateur authentifié (vs non authentifié)
 username, // principal : l'identifiant de l'utilisateur (username)
 null, // credentials : aucun mot de passe stocké dans le contexte (null = pas de secret en mémoire)
 List.of(new SimpleGrantedAuthority("ROLE_" + role)) // authorities : convertit "ADMIN" en "ROLE_ADMIN" (convention Spring)
 ); // List.of() crée une liste immuable, extensible pour plusieurs rôles
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
 SecurityContextHolder.getContext().setAuthentication(authentication); // Stocke l'authentification dans le contexte de sécurité du thread courant
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
 } // Fin du bloc if (token valide) : si token absent/invalide, SecurityContext reste vide → 403 Forbidden

 filterChain.doFilter(request, response); // Passe la requête au filtre suivant dans la chaîne (toujours appelé)
 } // Fin de doFilterInternal : même sans auth, la chaîne continue
} // Fin de JwtAuthenticationFilter
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
@Configuration // Classe de configuration Spring contenant des beans d'infrastructure
public class AppConfig { // Configuration des beans applicatifs (UserDetailsService, AuthenticationManager, init)

 @Bean // Déclare un bean UserDetailsService : charge un utilisateur depuis la base pour l'authentification
 public UserDetailsService userDetailsService(UtilisateurRepository repo) { // Injection du repository JPA
 return username -> { // Implémentation lambda de loadUserByUsername(String username)
 Utilisateur u = repo.findByUsername(username) // Cherche l'utilisateur en base par son nom
 .orElseThrow(() -> new UsernameNotFoundException( // Si introuvable, lève une exception Spring Security
 "Utilisateur introuvable : " + username)); // Message explicite pour le débogage
 return org.springframework.security.core.userdetails.User.builder() // Builder Spring Security pour UserDetails
 .username(u.getUsername()) // Définit le username (identifiant de connexion)
 .password(u.getPassword()) // Mot de passe hashé (BCrypt) stocké en base
 .roles(u.getRole()) // Rôle : le préfixe "ROLE_" est ajouté automatiquement par Spring
 .build(); // Construit l'objet UserDetails immutable
 }; // Fin de la lambda : retourne un UserDetails que Spring Security utilise pour l'auth
 } // Fin de userDetailsService
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
 @Bean // Déclare le gestionnaire d'authentification pour le login programmatique
 public AuthenticationManager authenticationManager( // Injection de la configuration d'authentification Spring
 AuthenticationConfiguration config) throws Exception { // AuthenticationConfiguration expose le AuthenticationManager par défaut
 return config.getAuthenticationManager(); // Retourne le manager qui utilise notre UserDetailsService + PasswordEncoder
 } // Ce bean est utilisé par AuthController.login() pour valider les credentials
```

**`AuthenticationManager` :** Bean nécessaire pour le login programmatique (dans AuthController).
`AuthenticationConfiguration.getAuthenticationManager()` retourne le manager configuré
par défaut qui utilise notre `UserDetailsService` et notre `PasswordEncoder`.

```java
 @Bean // Exécuté au démarrage de l'application : insère des utilisateurs de test
 public CommandLineRunner initUsers(UtilisateurRepository repo, // Injection du repository
 PasswordEncoder encoder) { // Injection du PasswordEncoder (BCrypt) pour hacher les mots de passe
 return args -> { // Lambda exécutée après le démarrage du contexte Spring
 if (!repo.existsByUsername("admin")) { // Vérifie si l'utilisateur admin existe déjà (évite les doublons)
 repo.save(new Utilisateur("admin", encoder.encode("admin123"), "ADMIN")); // Hash le mot de passe avant insertion
 } // En base, on stocke le hash BCrypt, jamais le mot de passe en clair
 if (!repo.existsByUsername("user")) { // Même vérification pour l'utilisateur standard
 repo.save(new Utilisateur("user", encoder.encode("user123"), "USER")); // Mot de passe hashé avec BCrypt
 } // Ces utilisateurs sont utilisés par les tests et le développement local
 }; // Fin de la lambda CommandLineRunner
 } // Fin de initUsers
} // Fin de AppConfig
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
@Entity // Entité JPA : mappée à la table "utilisateurs" en base de données H2
@Table(name = "utilisateurs") // Nom de la table dans la base
public class Utilisateur { // Modèle représentant un utilisateur stocké en base

 @Id // Clé primaire de la table
 @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-incrémentation gérée par la base H2
 private Long id; // Identifiant numérique unique

 @Column(unique = true, nullable = false) // Contrainte : valeur unique et obligatoire
 private String username; // Nom d'utilisateur pour la connexion (ex: "admin", "user")

 @Column(nullable = false) // Champ obligatoire : ne peut pas être null
 private String password; // Mot de passe hashé avec BCrypt (jamais stocké en clair)

 @Column(nullable = false) // Champ obligatoire
 private String role; // Rôle : "ADMIN" ou "USER" (sans préfixe ROLE_)

 private boolean actif = true; // Compte activé par défaut (pour désactivation future)
 // ... constructeurs, getters, setters // Code généré par IDE ou Lombok pour la manipulation de l'entité
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
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> { // Repository JPA avec CRUD automatique
 Optional<Utilisateur> findByUsername(String username); // Requête dérivée : SELECT * FROM utilisateurs WHERE username = ?
 boolean existsByUsername(String username); // Requête dérivée : vérifie si un username existe déjà (évite les doublons)
} // Spring Data génère automatiquement l'implémentation des requêtes à partir du nom des méthodes
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
@Service // Annotation de service Spring : contient la logique métier (couche service)
public class UtilisateurService { // Service de gestion des utilisateurs

 private final UtilisateurRepository repository; // Repository pour l'accès aux données
 private final PasswordEncoder passwordEncoder; // Encodeur BCrypt pour hacher les mots de passe

 public Utilisateur creer(String username, String password, String role) { // Crée un nouvel utilisateur
 if (repository.existsByUsername(username)) { // Vérifie si le username est déjà pris en base
 throw new IllegalArgumentException("Nom d'utilisateur déjà pris"); // Sécurité : éviter les doublons
 } // La validation en amont empêche les erreurs JPA et les confusions utilisateurs
 Utilisateur u = new Utilisateur(username, passwordEncoder.encode(password), role); // Hash le mot de passe AVANT insertion
 return repository.save(u); // Sauvegarde en base et retourne l'entité avec son ID généré
 } // Règle d'or : ne jamais stocker un mot de passe en clair, toujours le hasher avec BCrypt
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
@RestController // Contrôleur REST : les réponses sont directement sérialisées en JSON
@RequestMapping("/api/auth") // Préfixe d'URL pour tous les endpoints d'authentification
public class AuthController { // Gère le flux de connexion et la délivrance de tokens JWT

 private final AuthenticationManager authManager; // Gère l'authentification des credentials (username/password)
 private final JwtUtil jwtUtil; // Utilitaire JWT pour générer le token après authentification réussie
 private final UserDetailsService userDetailsService; // Charge les détails utilisateur depuis la base
 private final PasswordEncoder passwordEncoder; // Disponible pour d'éventuelles fonctionnalités supplémentaires
```

4 dépendances injectées :
- `AuthenticationManager` : valide les credentials
- `JwtUtil` : génère le token
- `UserDetailsService` : charge l'utilisateur
- `PasswordEncoder` : non utilisé directement ici (disponible pour d'autres fonctionnalités)

### La méthode `login()`

```java
 @PostMapping("/login") // Endpoint POST /api/auth/login : reçoit les credentials et retourne un JWT
 public ResponseEntity<?> login(@RequestBody Map<String, String> request) { // Corps JSON : {"username": "...", "password": "..."}
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
 try { // Bloc try-catch pour gérer les échecs d'authentification
 String username = request.get("username"); // Extrait le nom d'utilisateur du corps JSON
 String password = request.get("password"); // Extrait le mot de passe du corps JSON
```

#### Étape 2 : Authentifier

```java
 authManager.authenticate( // Délègue la validation à AuthenticationManager
 new UsernamePasswordAuthenticationToken(username, password)); // token non authentifié : username + password en clair
 // En interne : UserDetailsService.loadUserByUsername() + PasswordEncoder.matches() → compare le hash
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
 UserDetails userDetails = userDetailsService.loadUserByUsername(username); // Recharge l'utilisateur pour récupérer ses autorités
 String role = userDetails.getAuthorities().stream() // Obtient le flux des autorités (GrantedAuthority)
 .findFirst() // Prend la première autorité (un seul rôle par utilisateur dans notre cas)
 .orElse(new SimpleGrantedAuthority("ROLE_USER")) // Valeur par défaut si aucun rôle trouvé (sécurité)
 .getAuthority() // Extrait la chaîne de l'autorité : "ROLE_ADMIN" ou "ROLE_USER"
 .replace("ROLE_", ""); // Nettoie le préfixe : "ADMIN" ou "USER" pour le claim JWT
```

**Analyse :**
- Recharge l'utilisateur pour obtenir ses autorités
- `.getAuthorities()` → collection de `GrantedAuthority`
- `.findFirst()` → prend la première (un seul rôle dans notre cas)
- `.getAuthority()` → `"ROLE_ADMIN"` ou `"ROLE_USER"`
- `.replace("ROLE_", "")` → `"ADMIN"` ou `"USER"` (nettoie pour le claim JWT)

#### Étape 4 : Générer le token

```java
 String token = jwtUtil.genererToken(username, role); // Génère le JWT avec username et rôle (durée de validité : 1h)
 return ResponseEntity.ok(Map.of("token", token)); // Retourne 200 OK avec le token dans le corps JSON {"token": "..."}
```

Réponse JSON :
```json
{
 "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJBRE1JTiJ9..."
}
```

#### Étape 5 : Gérer l'échec

```java
 } catch (AuthenticationException e) { // Attrape toute exception d'authentification (mauvais mot de passe, utilisateur inconnu)
 return ResponseEntity.status(401) // HTTP 401 Unauthorized : authentification échouée
 .body(Map.of("error", "Identifiants invalides")); // Message générique : ne pas donner d'indice sur le champ erroné
 } // Pour la sécurité, on évite de préciser si c'est le username ou le password qui est incorrect
 } // Fin de la méthode login
} // Fin de AuthController
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
@RestController // Contrôleur REST pour l'API produits
@RequestMapping("/api/produits") // Préfixe d'URL pour tous les endpoints de gestion des produits
public class ProduitController { // Démonstration du contrôle d'accès par annotation @PreAuthorize

 @GetMapping("/public") // GET /api/produits/public : accessible à tous (permitAll dans SecurityConfig)
 public Map<String, String> listePublique() { // Aucune annotation de sécurité : la règle est dans SecurityConfig
 return Map.of("message", "Liste publique des produits accessible à tous"); // Endpoint totalement ouvert
 } // La configuration authorizeHttpRequests avec permitAll() gère l'accès ici
```

**Endpoint public :** aucun contrôle d'accès. N'importe qui peut y accéder.
La règle `permitAll()` dans `SecurityConfig` le permet.

```java
 @GetMapping("/admin") // GET /api/produits/admin : réservé aux administrateurs
 @PreAuthorize("hasRole('ADMIN')") // Sécurité déclarative : évaluée AVANT l'exécution de la méthode (expression SpEL)
 public Map<String, String> espaceAdmin() { // Si l'utilisateur n'a pas ROLE_ADMIN → 403 Forbidden
 return Map.of("message", "Espace administration — réservé aux ADMIN"); // Endpoint protégé par annotation
 } // @PreAuthorize s'ajoute à la sécurité de SecurityConfig (double couche)
```

**`@PreAuthorize("hasRole('ADMIN')")` :**
- Exécuté **avant** la méthode du contrôleur
- Évalue l'expression SpEL (Spring Expression Language) `hasRole('ADMIN')`
- Si l'utilisateur authentifié n'a pas `ROLE_ADMIN` → **403 Forbidden**
- Si OK → la méthode s'exécute normalement

```java
 @GetMapping("/user") // GET /api/produits/user : accessible aux ADMIN et USER
 @PreAuthorize("hasAnyRole('ADMIN', 'USER')") // hasAnyRole : l'utilisateur doit avoir AU MOINS UN des rôles listés
 public Map<String, String> espaceUtilisateur() { // ADMIN et USER passent, un visiteur non authentifié reçoit 403
 return Map.of("message", "Espace utilisateur — accessible aux ADMIN et USER"); // Endpoint partagé entre rôles
 } // @PreAuthorize est plus flexible que les règles dans SecurityConfig : granularité par méthode
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
@GetMapping("/{id}") // Exemple : GET /api/documents/42
@PostAuthorize("returnObject.createdBy == authentication.name") // Vérifié APRÈS la méthode : compare le créateur avec l'utilisateur connecté
public Document getDocument(@PathVariable Long id) { // returnObject fait référence à l'objet retourné par la méthode
 return documentService.findById(id); // Si le créateur du document n'est pas l'utilisateur courant → 403
} // @PostAuthorize permet des contrôles d'accès basés sur les données retournées
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
@DisplayName("Tests du JwtUtil") // Nom d'affichage pour le rapport de tests
class JwtUtilTest { // Tests unitaires purs : pas de mock, JwtUtil n'a pas de dépendances externes

 private final JwtUtil jwtUtil = new JwtUtil(); // Instance réelle : la clé secrète est hardcodée en constante
```

**Pas de mock** : on teste le `JwtUtil` réel. La classe n'a pas de dépendances externes
(la clé est hardcodée en constante), donc un test unitaire pur suffit.

### Groupe 1 : Génération et validation

```java
 @Nested // Classe imbriquée pour organiser les tests par groupe logique
 @DisplayName("Génération et validation de token") // Groupe 1 : création et vérification des tokens
 class GenerationValidation {

 @Test // Annotation JUnit 5 : méthode de test
 @DisplayName("Un token généré est valide") // Description lisible du scénario
 void tokenGenereEstValide() { // Test : un token bien formé doit passer la validation
 // Arrange
 String token = jwtUtil.genererToken("alice", "ADMIN"); // Génère un token pour l'utilisateur "alice" avec le rôle ADMIN
 // Act & Assert
 assertNotNull(token); // Vérifie que le token n'est pas null (génération réussie)
 assertFalse(token.isEmpty()); // Vérifie que le token n'est pas une chaîne vide
 assertTrue(jwtUtil.estTokenValide(token)); // Vérifie que le token passe la validation (signature OK, non expiré)
 } // Triple vérification pour s'assurer que le token est complet et valide
```

**Vérifications :**
1. Le token n'est pas null
2. Le token n'est pas vide
3. Le token passe la validation (signature OK, pas expiré)

```java
 @Test
 @DisplayName("Extraire le username d'un token valide")
 void extraireUsername() { // Test d'extraction du claim subject (username)
 // Arrange
 String token = jwtUtil.genererToken("bob", "USER"); // Génère un token avec le username "bob"
 // Act & Assert
 assertEquals("bob", jwtUtil.extraireUsername(token)); // Vérifie que l'extraction retourne bien "bob"
 } // Vérifie la correspondance entre le username fourni et celui récupéré du token
```

Vérifie l'extraction du sujet (subject → username).

```java
 @Test
 @DisplayName("Extraire le rôle d'un token valide")
 void extraireRole() { // Test d'extraction du claim personnalisé "role"
 // Arrange
 String token = jwtUtil.genererToken("charlie", "ADMIN"); // Génère un token avec le rôle ADMIN
 // Act & Assert
 assertEquals("ADMIN", jwtUtil.extraireRole(token)); // Vérifie que le rôle extrait correspond à "ADMIN"
 } // Le claim "role" est personnalisé (non standard contrairement à "sub")
```

Vérifie l'extraction du claim personnalisé `role`.

### Groupe 2 : Tokens invalides

```java
 @Nested // Groupe 2 : cas d'erreur et tokens malformés
 @DisplayName("Tokens invalides") // Teste les cas où estTokenValide doit retourner false
 class TokensInvalides {

 @Test
 @DisplayName("Token vide ou null est invalide")
 void tokenVideNullInvalide() { // Test des cas limites : null et chaîne vide
 // Act & Assert (pas d'Arrange : on teste directement les entrées invalides)
 assertFalse(jwtUtil.estTokenValide(null)); // Token null → doit retourner false sans lever d'exception
 assertFalse(jwtUtil.estTokenValide("")); // Token vide → doit retourner false
 } // La méthode doit gérer proprement ces entrées sans crash
```

Test des cas limites : null et chaîne vide. La méthode `estTokenValide` doit retourner
`false` sans lever d'exception.

```java
 @Test
 @DisplayName("Token modifié (tampered) est invalide")
 void tokenModifieInvalide() { // Test d'intégrité : vérifie que la signature protège contre la modification
 // Arrange
 String token = jwtUtil.genererToken("dave", "USER"); // Génère un token valide
 String tokenModifie = token.substring(0, token.length() - 3) + "xxx"; // Modifie les 3 derniers caractères de la signature
 // Act & Assert
 assertFalse(jwtUtil.estTokenValide(tokenModifie)); // La signature ne correspond plus → validation échoue
 } // La moindre modification du token invalide la signature HMAC (protection anti-falsification)
```

**Test d'intégrité :** Si quelqu'un modifie le token (ne serait-ce qu'un caractère),
la signature ne correspondra plus et le token sera rejeté.

```java
 @Test
 @DisplayName("Token aléatoire est invalide")
 void tokenAleatoireInvalide() { // Test : un JWT syntaxiquement correct mais au contenu aléatoire est rejeté
 // Act & Assert
 assertFalse(jwtUtil.estTokenValide("eyJhbGciOiJIUzI1NiJ9.abc.def")); // 3 parties, mais signature invalide → false
 } // La signature HMAC ne correspond pas au header et payload fournis → rejet
```

Même un token qui ressemble syntaxiquement à un JWT mais avec un contenu aléatoire
doit être rejeté.

```java
 @Test
 @DisplayName("Tokens différents pour le même utilisateur")
 void tokensDifferentsPourMemeUtilisateur() { // Vérifie que deux tokens pour le même user sont différents
 // Arrange
 String t1 = jwtUtil.genererToken("eve", "USER"); // Premier token pour eve
 String t2 = jwtUtil.genererToken("eve", "USER"); // Deuxième token pour eve (mêmes paramètres)
 // Act & Assert
 assertNotEquals(t1, t2); // Les tokens sont différents car le claim iat (issuedAt) change à chaque appel
 } // Le timestamp d'émission est unique → le hash global diffère → tokens différents
```

**Pourquoi les tokens sont différents ?** Le claim `iat` (issued at) change à chaque
appel car il contient le timestamp courant. Donc le hash global est différent.

---

### Tests — SecurityIntegrationTest.java

 `labs/lab07-spring-security/src/test/java/com/nexa/secu/controller/SecurityIntegrationTest.java`

### Configuration du test

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) // Démarre le contexte Spring complet sur un port aléatoire
@AutoConfigureMockMvc // Configure automatiquement MockMvc pour les requêtes HTTP simulées
@DisplayName("Tests de sécurité — intégration") // Test d'intégration : tous les beans Spring sont chargés
class SecurityIntegrationTest { // Vérifie les règles d'accès avec des tokens JWT réels

 @Autowired private MockMvc mockMvc; // Client HTTP simulé : envoie des requêtes sans démarrer le serveur
 @Autowired private JwtUtil jwtUtil; // Utilitaire JWT réel (injecté par Spring)
```

**Annotations :**
- `@SpringBootTest` : démarre le contexte Spring complet (tous les beans)
- `webEnvironment = RANDOM_PORT` : démarre le serveur sur un port aléatoire (tests plus isolés)
- `@AutoConfigureMockMvc` : configure MockMvc automatiquement

### Groupe 1 : Accès public

```java
 @Nested // Groupe 1 : endpoints publics (permitAll dans SecurityConfig)
 @DisplayName("Accès public")
 class AccesPublic {

 @Test
 @DisplayName("Sans token : l'endpoint public est accessible")
 void sansTokenPublicAccessible() throws Exception { // Vérifie que la règle permitAll() fonctionne
 // Arrange & Act
 mockMvc.perform(get("/api/produits/public")) // Requête GET sans aucun token
 // Assert
 .andExpect(status().isOk()); // 200 OK : l'accès public est autorisé sans authentification
 } // Endpoint ouvert à tous, y compris les visiteurs non identifiés
```

**Ce test vérifie :** l'endpoint `/api/produits/public` est bien accessible sans aucune
authentification. La règle `permitAll()` fonctionne.

```java
 @Test
 @DisplayName("Avec token USER : l'endpoint public est accessible")
 void avecTokenUserPublicAccessible() throws Exception { // Vérifie que le token ne bloque pas l'accès public
 // Arrange
 String token = jwtUtil.genererToken("user", "USER"); // Génère un token JWT valide pour un utilisateur USER
 // Act
 mockMvc.perform(get("/api/produits/public") // Requête GET sur l'endpoint public
 .header("Authorization", "Bearer " + token)) // Ajoute le token dans le header Authorization
 // Assert
 .andExpect(status().isOk()); // 200 OK : l'endpoint public reste accessible même avec un token
 } // Test de non-régression : le token ne doit pas casser l'accès public
```

Même avec un token, l'endpoint public reste accessible. Pas de régression.

### Groupe 2 : Contrôle d'accès par rôle

```java
 @Nested // Groupe 2 : vérification des droits par rôle
 @DisplayName("Contrôle d'accès par rôle") // Teste @PreAuthorize avec hasRole et hasAnyRole
 class ControleAcces {

 @Test
 @DisplayName("Token ADMIN peut accéder à l'espace admin")
 void adminAccedeAdmin() throws Exception { // ADMIN → hasRole('ADMIN') → 200 OK
 // Arrange
 String token = jwtUtil.genererToken("admin", "ADMIN"); // Génère un token avec le rôle ADMIN
 // Act
 mockMvc.perform(get("/api/produits/admin") // Endpoint protégé par @PreAuthorize("hasRole('ADMIN')")
 .header("Authorization", "Bearer " + token)) // Envoie le token JWT
 // Assert
 .andExpect(status().isOk()); // 200 : ADMIN est autorisé à accéder à l'espace admin
 } // Scénario nominal : un administrateur accède à son espace réservé
```

**ADMIN accède à l'espace admin** : token avec rôle ADMIN → `@PreAuthorize("hasRole('ADMIN')")` → OK.

```java
 @Test
 @DisplayName("Token USER ne peut PAS accéder à l'espace admin → 403")
 void userNePeutPasAccederAdmin() throws Exception { // Test critique : USER doit être refusé sur /admin
 // Arrange
 String token = jwtUtil.genererToken("user", "USER"); // Génère un token avec le rôle USER
 // Act
 mockMvc.perform(get("/api/produits/admin") // Endpoint réservé ADMIN
 .header("Authorization", "Bearer " + token))
 // Assert
 .andExpect(status().isForbidden()); // 403 Forbidden : USER n'a pas le rôle ADMIN
 } // Ce test est le plus important : il valide le contrôle d'accès négatif
```

**USER ne peut pas accéder à l'espace admin** : token USER → `hasRole('ADMIN')` → 403 Forbidden.
C'est le test de contrôle d'accès le plus important.

```java
 @Test
 @DisplayName("Token USER peut accéder à l'espace user")
 void userAccedeUser() throws Exception { // USER → hasAnyRole('ADMIN', 'USER') → 200 OK
 // Arrange
 String token = jwtUtil.genererToken("user", "USER");
 // Act
 mockMvc.perform(get("/api/produits/user")
 .header("Authorization", "Bearer " + token))
 // Assert
 .andExpect(status().isOk()); // 200 : USER a bien accès à son espace
 } // hasAnyRole inclut USER → accès autorisé
```

USER entre dans l'espace user car `hasAnyRole('ADMIN', 'USER')` inclut USER.

```java
 @Test
 @DisplayName("Token ADMIN peut accéder à l'espace user (hasAnyRole)")
 void adminAccedeUser() throws Exception { // ADMIN → hasAnyRole('ADMIN', 'USER') → 200 OK
 // Arrange
 String token = jwtUtil.genererToken("admin", "ADMIN");
 // Act
 mockMvc.perform(get("/api/produits/user")
 .header("Authorization", "Bearer " + token))
 // Assert
 .andExpect(status().isOk()); // 200 : ADMIN aussi accède à l'espace user
 } // hasAnyRole inclut ADMIN et USER → les deux rôles passent
```

ADMIN entre aussi dans l'espace user car `hasAnyRole('ADMIN', 'USER')` inclut ADMIN.

### Groupe 3 : Authentification

```java
 @Nested // Groupe 3 : comportement sans authentification ou avec token invalide
 @DisplayName("Authentification") // Vérifie que les endpoints protégés refusent les requêtes non authentifiées
 class Authentification {

 @Test
 @DisplayName("Sans token : endpoint sécurisé → 403 Forbidden")
 void sansTokenEndpointSecurise() throws Exception { // Aucune authentification fournie
 // Arrange & Act
 mockMvc.perform(get("/api/produits/admin")) // Requête GET sans header Authorization
 // Assert
 .andExpect(status().isForbidden()); // 403 : pas d'authentification → accès refusé
 } // Le filtre JWT ne trouve pas de token → SecurityContext vide → 403
```

Aucun token → pas d'authentification → un endpoint protégé retourne 403.

```java
 @Test
 @DisplayName("Token invalide → 403 Forbidden")
 void tokenInvalide() throws Exception { // Token syntaxiquement invalide (ne ressemble même pas à un JWT)
 // Arrange & Act
 mockMvc.perform(get("/api/produits/user") // Endpoint qui nécessite une authentification
 .header("Authorization", "Bearer token_invalide")) // Token clairement invalide
 // Assert
 .andExpect(status().isForbidden()); // 403 : token invalide = pas authentifié → refusé
 } // estTokenValide retourne false → le filtre n'enregistre pas d'authentification
```

Un token invalide est traité comme "pas authentifié" → 403.

```java
 @Test
 @DisplayName("Header Authorization absent → 403")
 void headerAuthorizationAbsent() throws Exception { // Aucun header d'authentification
 // Arrange & Act
 mockMvc.perform(get("/api/produits/user")) // Requête sans aucun header Authorization
 // Assert
 .andExpect(status().isForbidden()); // 403 : pas de header, pas d'authentification
 } // Même comportement que "sans token" : authHeader est null → filtre ignoré
```

Pas de header `Authorization` du tout → 403.

```java
 @Test
 @DisplayName("Header Authorization format incorrect → 403")
 void headerFormatIncorrect() throws Exception { // Token valide mais sans le préfixe "Bearer "
 // Arrange
 String token = jwtUtil.genererToken("user", "USER"); // Token valide, rôle USER
 // Act
 mockMvc.perform(get("/api/produits/user") // Même token valide, mais format incorrect
 .header("Authorization", token)) // Envoie le token SANS le préfixe "Bearer "
 // Assert
 .andExpect(status().isForbidden()); // 403 : le filtre ignore le header car ne commence pas par "Bearer "
 } // Le filtre vérifie authHeader.startsWith("Bearer ") → false → pas d'authentification
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
if (!repo.existsByUsername("moderator")) { // Vérifie que l'utilisateur moderator n'existe pas déjà
 repo.save(new Utilisateur("moderator", encoder.encode("mod123"), "MODERATOR")); // Hash le mot de passe et insère en base
} // Ajout du nouveau rôle MODERATOR pour le lab : mot de passe "mod123", hashé avec BCrypt
```

### Étape 2 : Ajouter l'endpoint dans ProduitController

```java
@GetMapping("/moderate") // GET /api/produits/moderate : nouveau endpoint du lab
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')") // Accessible aux ADMIN et MODERATOR (hasAnyRole)
public Map<String, String> espaceModeration() { // Les utilisateurs USER sont exclus (403)
 return Map.of("message", "Espace modération — réservé aux ADMIN et MODERATOR"); // Démonstration d'un nouveau rôle
} // @PreAuthorize évalue l'expression SpEL avant l'exécution de la méthode
```

### Étape 3 : Mettre à jour SecurityConfig

S'assurer que la règle `GET /api/produits/**` permet l'accès (déjà `permitAll()` — la sécurité
est gérée par `@PreAuthorize`).

### Étape 4 : Écrire les tests

Dans `SecurityIntegrationTest`, ajouter une nouvelle classe imbriquée :

```java
@Nested // Groupe 4 : tests du nouveau rôle MODERATOR ajouté dans le lab
@DisplayName("Rôle MODERATOR") // Vérifie que le nouveau endpoint /moderate est correctement protégé
class RoleModerator {

 @Test
 @DisplayName("Token MODERATOR peut accéder à l'espace modération")
 void moderatorAccedeModeration() throws Exception { // MODERATOR → hasAnyRole('ADMIN', 'MODERATOR') → 200
 // Arrange
 String token = jwtUtil.genererToken("moderator", "MODERATOR"); // Token avec le nouveau rôle MODERATOR
 // Act
 mockMvc.perform(get("/api/produits/moderate") // Nouvel endpoint de modération
 .header("Authorization", "Bearer " + token))
 // Assert
 .andExpect(status().isOk()); // 200 : MODERATOR est autorisé
 } // Scénario nominal pour le nouveau rôle

 @Test
 @DisplayName("Token ADMIN peut accéder à l'espace modération")
 void adminAccedeModeration() throws Exception { // ADMIN → hasAnyRole('ADMIN', 'MODERATOR') → 200
 // Arrange
 String token = jwtUtil.genererToken("admin", "ADMIN"); // Token ADMIN
 // Act
 mockMvc.perform(get("/api/produits/moderate")
 .header("Authorization", "Bearer " + token))
 // Assert
 .andExpect(status().isOk()); // 200 : ADMIN est aussi autorisé (hasAnyRole)
 } // Les deux rôles ADMIN et MODERATOR ont accès à l'espace modération

 @Test
 @DisplayName("Token USER ne peut PAS accéder à l'espace modération → 403")
 void userNePeutPasAccederModeration() throws Exception { // USER → hasAnyRole('ADMIN', 'MODERATOR') → 403
 // Arrange
 String token = jwtUtil.genererToken("user", "USER"); // Token USER (non autorisé)
 // Act
 mockMvc.perform(get("/api/produits/moderate")
 .header("Authorization", "Bearer " + token))
 // Assert
 .andExpect(status().isForbidden()); // 403 : USER n'a pas le bon rôle
 } // Test de contrôle d'accès négatif pour le nouveau rôle

 @Test
 @DisplayName("Sans token : espace modération → 403")
 void sansTokenModeration() throws Exception { // Pas d'authentification du tout
 // Arrange & Act
 mockMvc.perform(get("/api/produits/moderate")) // Requête sans token
 // Assert
 .andExpect(status().isForbidden()); // 403 : pas d'authentification
 } // Sans authentification, tout endpoint protégé retourne 403
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
