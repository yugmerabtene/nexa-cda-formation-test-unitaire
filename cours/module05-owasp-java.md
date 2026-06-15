# Module 5 : Vulnérabilités OWASP en Java

**Durée :** 1h30 (9h00–10h30) — Jour 2 Matin
**Prérequis :** Module 1 à 4 (JUnit, Mockito, assertions, tests paramétrés)
**Projet support :** `labs/lab05-owasp-java` (Java 17, Maven, JUnit Jupiter 5.10.2)

---

## Objectifs pédagogiques

A l'issue de ce module, vous serez capable de :

1. Lister les 10 categories de l'OWASP Top 10 (2021).
2. Expliquer le mecanisme d'une injection SQL et la corriger avec `PreparedStatement`.
3. Expliquer le mecanisme d'une XSS et la corriger avec l'echappement HTML.
4. Exploiter et corriger une faille de path traversal.
5. Hacher un mot de passe avec BCrypt et verifier le format `sel:hash`.
6. Ecrire des tests qui prouvent qu'une vulnerabilite est corrigee.

---

## PARTIE 1 -- THEORIE (35 min)

### 1. L'OWASP et son Top 10 (2021)

**OWASP** (*Open Web Application Security Project*) est une fondation à but non lucratif qui publie régulièrement le **Top 10 des risques de sécurité applicative**. La dernière édition (2021) classe les vulnérabilités les plus critiques :

| # | Catégorie | Application en Java |
|---|-----------|---------------------|
| **A01** | Contrôle d'accès défaillant | `@PreAuthorize` mal configuré, endpoints non protégés |
| **A02** | Échecs cryptographiques | SHA-256 sans sel, MD5, clés en dur dans le code |
| **A03** | **Injection** | SQL, LDAP, OS — concaténation au lieu de `PreparedStatement` |
| **A04** | Conception non sécurisée | Absence de validation des entrées, pas de threat modeling |
| **A05** | Mauvaise configuration | Debug=true en prod, ports exposés, logs verbeux |
| **A06** | Composants vulnérables | Log4Shell, Spring4Shell — dépendances non mises à jour |
| **A07** | Identification défaillante | Sessions faibles, pas de MFA, mots de passe par défaut |
| **A08** | Défaillance logicielle et intégrité | Désérialisation non sécurisée, pas de signature de CI/CD |
| **A09** | Journalisation insuffisante | Pas d'audit des connexions, pas de monitoring d'erreurs |
| **A10** | SSRF (*Server-Side Request Forgery*) | L'application télécharge des URLs fournies par l'utilisateur |

Dans ce module, nous traitons **A03 (Injection)** et **A02 (Échecs cryptographiques)**, qui sont les deux catégories les plus fréquentes dans le code Java quotidien.

---

### 2. Injection SQL — A03

#### 2.1 Le mécanisme de l'attaque

L'injection SQL exploite une **concaténation naïve** de chaînes pour modifier la sémantique d'une requête.

Prenons le code vulnérable de notre lab (`RequeteurSQL.java:5-7`) :

```java
// Méthode VOLONTAIREMENT vulnérable pour démonstration pédagogique
public String construireRequeteVulnerable(String nom) {
    // Concaténation directe : la saisie utilisateur est injectée dans le SQL brut
    // OWASP A03 : si nom = "' OR '1'='1' --", la clause WHERE est contournée → bypass d'auth
 return "SELECT * FROM users WHERE nom = '" + nom + "'";
}
```

Si l'utilisateur saisit `Jean`, la requête devient :
```sql
SELECT * FROM users WHERE nom = 'Jean'
```

Mais si l'attaquant saisit `' OR '1'='1' --`, la requête devient :
```sql
SELECT * FROM users WHERE nom = '' OR '1'='1' --'
```

**Analyse de la charge utile :**
- `'` ferme le guillemet ouvert par le code
- `OR '1'='1'` ajoute une condition toujours vraie → **bypass d'authentification**
- `--` commente tout le reste de la requête SQL (ignore la clause `AND password = ...`)

Voici les 5 vecteurs d'attaque classiques testés dans notre lab (`RequeteurSQLTest.java:40-46`) :

| Vecteur d'injection | Effet |
|---------------------|-------|
| `' OR '1'='1` | Bypass d'authentification |
| `'; DROP TABLE users; --` | Destruction de table |
| `1' UNION SELECT * FROM users --` | Exfiltration de données |
| `admin'--` | Usurpation d'identité |
| `' OR 1=1 --` | Bypass (variante numérique) |

#### 2.2 La correction : requêtes paramétrées

Le code sécurisé (`RequeteurSQL.java:9-11`) utilise un **paramètre lié** (le `?`) :

```java
// Méthode SÉCURISÉE utilisant un paramètre lié (placeholder ?)
// La requête SQL est fixe ; la valeur utilisateur est transmise via un canal séparé
public String construireRequeteSecurisee(String nom) {
    // Le '?' est un placeholder que JDBC remplit avec la valeur échappée
    // Même si nom = "' OR '1'='1'", JDBC le traite comme une chaîne littérale, pas du SQL
    // La prévention de l'injection SQL est garantie par le driver lui-même
 return "SELECT * FROM users WHERE nom = ?";
}
```

Le `?` est un **placeholder** que le driver JDBC remplit avec la valeur échappée. La donnée utilisateur n'est **jamais** concaténée dans le SQL — elle passe par un canal séparé (le paramètre bind). Même si l'utilisateur injecte `' OR '1'='1'`, le driver traite cette chaîne comme une **valeur littérale** (le nom d'utilisateur devient littéralement `' OR '1'='1'`), jamais comme du code SQL.

**Règle d'or :** Jamais de concaténation de chaînes dans une requête SQL. Toujours utiliser `PreparedStatement`, `JdbcTemplate`, ou JPA.

#### 2.3 Le cas particulier du `LIKE`

Le `LIKE` est plus délicat car les métacaractères `%` et `_` sont valides en SQL :

```java
// Version vulnérable (RequeteurSQL.java:13-15)
public String construireRechercheVulnerable(String terme) {
    // LIKE + concaténation = double risque : injection SQL ET métacaractères (% et _)
    // Un attaquant peut injecter '%' pour matcher toutes les lignes de la table
    // Exemple : terme = "%" → LIKE '%%%' qui correspond à tout
 return "SELECT * FROM produits WHERE nom LIKE '%" + terme + "%'";
}
```

Un attaquant peut injecter `%` pour forcer un `LIKE '%%%'` qui matche tout.

La correction (`RequeteurSQL.java:17-19`) paramétrise la requête, et la méthode `encoderParametrePourLike` (`RequeteurSQL.java:21-23`) échappe les métacaractères du `LIKE` :

```java
// Version SÉCURISÉE : le LIKE est paramétré avec un placeholder '?'
// La requête ne contient aucune donnée utilisateur concaténée
public String construireRechercheSecurisee() {
 return "SELECT * FROM produits WHERE nom LIKE ?";
}

// Échappe les métacaractères LIKE (% et _) pour éviter qu'ils soient interprétés
// Le backslash '\' est le caractère d'échappement SQL par défaut
public String encoderParametrePourLike(String terme) {
    // Ajoute les jokers % aux extrémités pour une recherche "contient"
    // remplace % par \% et _ par \_ pour qu'ils soient littéraux
 return "%" + terme.replace("%", "\\%").replace("_", "\\_") + "%";
}
```

**Note :** `\` est le caractère d'échappement par défaut dans SQL. Certains SGBD utilisent d'autres caractères (ex: `ESCAPE '!'`).

---

### 3. XSS (Cross-Site Scripting) — A03

#### 3.1 Les trois types de XSS

| Type | Description | Exemple |
|------|-------------|---------|
| **Reflected** | La charge utile arrive via la requête HTTP et est reflétée immédiatement dans la réponse | `?nom=<script>alert(1)</script>` |
| **Stored** | La charge utile est stockée en base et exécutée à chaque affichage | Commentaire de blog contenant `<script>` |
| **DOM-based** | La vulnérabilité est côté client uniquement (JavaScript manipule le DOM) | `document.write(location.hash)` |

#### 3.2 Vecteurs d'attaque courants

Notre lab teste 6 vecteurs (`SanitizerXSSTest.java:71-78`) :

| Vecteur | Explication |
|---------|-------------|
| `<script>alert(1)</script>` | Balise script classique |
| `<img src=x onerror='alert(1)'>` | Événement sur balise auto-fermante (pas besoin de `</img>`) |
| `"><script>alert(document.cookie)</script>` | Fermeture d'attribut HTML + injection |
| `<svg onload=alert(1)>` | SVG avec gestionnaire d'événement |
| `'-alert(1)-'` | Injection dans un contexte JavaScript |
| `<body onload='alert(1)'>` | Événement sur élément body |

La méthode `contientScript()` (`SanitizerXSS.java:25-30`) détecte la présence de ces motifs :

```java
// Détecteur par signatures de tentatives XSS (basé sur une blacklist de motifs)
// Utile pour logger / alerter, NE REMPLACE PAS l'échappement HTML en sortie
public boolean contientScript(String input) {
    // Protection NullPointerException : input null = pas de script
 if (input == null) return false;
    // Normalise la casse pour détecter <Script>, <SCRIPT>, etc.
 String lower = input.toLowerCase();
    // Vérifie les signatures de balises et gestionnaires d'événements XSS courants
    // OWASP A03 (Injection) : <script>, javascript:, onerror=, onload=, onclick=
 return lower.contains("<script") || lower.contains("javascript:")
 || lower.contains("onerror=") || lower.contains("onload=")
 || lower.contains("onclick=");
}
```

#### 3.3 La correction : échappement HTML

La sortie HTML doit être **encodée avant affichage**. Les 5 caractères critiques sont :

| Caractère | Entité HTML | Rôle |
|-----------|-------------|------|
| `<` | `&lt;` | Ouvre une balise |
| `>` | `&gt;` | Ferme une balise |
| `&` | `&amp;` | Commence une entité (sinon `&lt;` serait réinterprété) |
| `"` | `&quot;` | Ferme un attribut HTML |
| `'` | `&#39;` | Ferme un attribut HTML (simple quote) |

**Ordre impératif :** toujours échapper `&` en premier, sinon `&lt;` deviendrait `&amp;lt;`.

Implémentation dans notre lab (`SanitizerXSS.java:15-23`) :

```java
// Échappe les 5 caractères HTML critiques pour neutraliser tout XSS
// OWASP A03 (Injection/XSS) : empêche l'exécution de scripts dans le navigateur
public String echapperHtml(String input) {
    // null → chaîne vide : pas de NullPointerException en production
 if (input == null) return "";
    // ORDRE IMPÉRATIF : échapper & en premier, sinon &lt; deviendrait &amp;lt;
 return input
 .replace("&", "&amp;")  // & → &amp; (doit être le premier, sinon réinterprétation)
 .replace("<", "&lt;")   // < → &lt; (ouvre une balise HTML)
 .replace(">", "&gt;")   // > → &gt; (ferme une balise HTML)
 .replace("\"", "&quot;") // " → &quot; (ferme un attribut HTML)
 .replace("'", "&#39;");  // ' → &#39; (ferme un attribut HTML en simple quote)
}
```

Le test `echappementCaracteresSpeciaux` (`SanitizerXSSTest.java:57-60`) vérifie que les 5 caractères sont remplacés :

```java
// Assertion : vérifie que les 5 caractères HTML sont correctement transformés
// Chaque caractère spécial devient son entité HTML correspondante
assertEquals("&lt;&gt;&amp;&quot;&#39;",
 sanitizer.echapperHtml("<>&\"'"));
```

Pour la production, utilisez **OWASP Java Encoder** (`org.owasp.encoder:encoder`) qui gère le contexte (HTML body, attribut, JavaScript, CSS, URL).

---

### 4. Path Traversal — A01

#### 4.1 Le mécanisme

Un attaquant manipule le chemin d'un fichier pour sortir du répertoire autorisé :

```java
// Vulnérable (SecuriteFichier.java:10-12)
public String construireCheminVulnerable(String nomFichier) {
    // Concaténation naïve : l'utilisateur peut sortir du répertoire autorisé
    // OWASP A01 (Contrôle d'accès défaillant) : path traversal
    // Exemple : "/var/data/" + "../../etc/passwd" → lecture du fichier passwd système
 return REPERTOIRE_AUTORISE + nomFichier; // "/var/data/" + "../../etc/passwd"
}
```

Résultat : `/var/data/../../etc/passwd` → après résolution : `/etc/passwd`.

#### 4.2 La correction en couches

La version sécurisée (`SecuriteFichier.java:14-30`) applique une **défense en profondeur** :

```java
// Version SÉCURISÉE avec défense en profondeur (2 couches de protection)
// OWASP A01 : empêche le path traversal par validation + normalisation
public String construireCheminSecurise(String nomFichier) {
 // Couche 1 : rejet explicite des caractères dangereux (blacklist)
 if (nomFichier == null || nomFichier.isEmpty()) {
 throw new IllegalArgumentException("Nom de fichier invalide");
 }
    // Bloque ".." (remontée), "/" et "\\" (séparateurs de chemins)
    // Empêche les attaques de base comme "../../etc/passwd"
 if (nomFichier.contains("..") || nomFichier.contains("/")
 || nomFichier.contains("\\")) {
 throw new IllegalArgumentException("Caractères interdits dans le nom de fichier");
 }

 // Couche 2 : normalisation et vérification startsWith (file d'appoint)
    // Normalise les chemins pour résoudre les .., les symlinks, etc.
 Path base = Paths.get(REPERTOIRE_AUTORISE).normalize();
    // Résout le nom du fichier par rapport au répertoire de base
 Path fichier = base.resolve(nomFichier).normalize();

    // Vérifie que le chemin final commence bien par le répertoire autorisé
    // Si un contournement a réussi (Unicode, encodage), cette vérification le détecte
 if (!fichier.startsWith(base)) {
 throw new SecurityException("Tentative de path traversal détectée");
 }

 return fichier.toString();
}
```

**Pourquoi les deux couches ?** La normalisation seule ne suffit pas toujours (contournements Unicode, encodages alternatifs). La liste noire explicite et le `startsWith` après normalisation forment une double protection.

La détection proactive (`SecuriteFichier.java:32-36`) vérifie les motifs suspects :

```java
// Détection proactive de tentatives de path traversal (blacklist de motifs)
// Permet de logger les attaques avant qu'elles n'atteignent les couches de sécurité
public boolean estTentativePathTraversal(String input) {
    // null n'est pas une attaque, on retourne false
 if (input == null) return false;
    // Détecte les motifs de remontée : ../ (Linux) et ..\\ (Windows)
    // Détecte aussi /.. et \.. (variantes avec chemin absolu)
 return input.contains("../") || input.contains("..\\")
 || input.contains("/..") || input.contains("\\..")
    // Chemins absolus (commençant par /) — peuvent être malveillants selon le contexte
 || input.startsWith("/") || input.contains("\0");
    // Null byte (\0) : vecteur historique de troncature (fichier.txt\0.jpg)
}
```

**Note :** `\0` (null byte) est un vecteur historique : `fichier.txt\0.jpg` → certains langages (C, PHP) tronquent au null byte et ne voient que `.jpg`, tandis que le système lit `.jpg` mais exécute le code présent dans le fichier binaire.

---

### 5. Hachage des mots de passe — A02

#### 5.1 Ce qu'il ne faut JAMAIS faire

| Pratique | Raison |
|----------|--------|
| Stocker en clair | Une fuite de base = tous les mots de passe compromis |
| MD5 | Cassé cryptographiquement, collisions en secondes |
| SHA-1 | Cassé (SHAttered, 2017) |
| SHA-256 **sans sel** | Déterministe → vulnérable aux **rainbow tables** |
| Chiffrer au lieu de hacher | Réversible si la clé fuit |

#### 5.2 La preuve par le code : SHA-256 avec vs sans sel

Notre lab contient deux méthodes côte à côte pour démontrer la différence :

**Hachage vulnérable** (`GestionnaireMotDePasse.java:10-18`) :
```java
// Hachage VOLONTAIREMENT vulnérable : SHA-256 sans sel
// OWASP A02 (Échecs cryptographiques) : déterministe → vulnérable aux rainbow tables
public String hacherVulnerable(String motDePasse) {
 try {
    // MessageDigest fournit une fonction de hachage (SHA-256 ici)
 MessageDigest digest = MessageDigest.getInstance("SHA-256");
    // digest() calcule le hash directement sur le mot de passe, SANS ajouter de sel
    // getBytes() utilise l'encodage par défaut (problème supplémentaire)
 byte[] hash = digest.digest(motDePasse.getBytes());
    // Encode le hash binaire en Base64 pour un stockage lisible en chaîne
 return Base64.getEncoder().encodeToString(hash);
 } catch (NoSuchAlgorithmException e) {
    // Propagé en RuntimeException car l'absence de SHA-256 est impossible (JRE standard)
 throw new RuntimeException(e);
 }
}
```

Ce hachage est **déterministe** : `hacherVulnerable("password123")` produit **toujours le même hash**. Le test le prouve (`GestionnaireMotDePasseTest.java:20-24`) :

```java
// Preuve de vulnérabilité : deux appels avec le même mot de passe produisent le même hash
// C'est le comportement DÉTERMINISTE qui permet les rainbow tables
String h1 = gestionnaire.hacherVulnerable("password123");
String h2 = gestionnaire.hacherVulnerable("password123");
assertEquals(h1, h2); // Même entrée → même hash (A02 : pas de sel → vulnérable)
```

Conséquence : un attaquant peut précalculer une **rainbow table** (SHA-256("password123") = X, SHA-256("123456") = Y...) et retrouver instantanément les mots de passe communs.

**Hachage sécurisé** (`GestionnaireMotDePasse.java:20-34`) :
```java
// Hachage SÉCURISÉ avec sel aléatoire : même mot de passe → hash différent à chaque appel
// OWASP A02 : le sel neutralise les rainbow tables
public String hacherAvecSel(String motDePasse) {
 try {
    // SecureRandom : générateur cryptographique fort (imprévisible)
 SecureRandom random = new SecureRandom();
    // Sel de 16 octets (128 bits) — taille recommandée
 byte[] sel = new byte[16];
 random.nextBytes(sel);

    // Le sel est injecté dans le MessageDigest AVANT le mot de passe
    // digest.update() ajoute des données au buffer de hachage
 MessageDigest digest = MessageDigest.getInstance("SHA-256");
 digest.update(sel); // Le sel est combiné AVANT le mot de passe
    // digest(byte[]) finalise le calcul avec le mot de passe
 byte[] hash = digest.digest(motDePasse.getBytes());

    // Format de stockage : sel:hash (les deux en Base64)
    // Le sel doit être stocké pour pouvoir vérifier le mot de passe plus tard
 return Base64.getEncoder().encodeToString(sel) + ":" + Base64.getEncoder().encodeToString(hash);
 } catch (NoSuchAlgorithmException e) {
 throw new RuntimeException(e);
 }
}
```

Le test prouve la non-déterminisme (`GestionnaireMotDePasseTest.java:28-33`) :

```java
// Preuve de correction : deux appels avec le même mot de passe → hashs différents
// C'est le sel aléatoire qui garantit cette propriété
// Un attaquant ne peut pas précalculer une rainbow table pour chaque sel possible
String h1 = gestionnaire.hacherAvecSel("password123");
String h2 = gestionnaire.hacherAvecSel("password123");
assertNotEquals(h1, h2); // Même entrée → hashs différents (sel aléatoire → A02 corrigé)
```

Format de sortie : `sel:hash` (sel en Base64, suivi de `:`, puis le hash en Base64). La vérification (`GestionnaireMotDePasse.java:36-51`) extrait le sel stocké, recalcule le hash, et compare :

```java
// Vérifie un mot de passe contre un hash stocké au format sel:hash
// Extrait le sel, recalcule le hash, compare (technique de vérification standard)
public boolean verifierMotDePasse(String motDePasse, String hashStocke) {
    // Split le format sel:hash pour récupérer les deux parties
 String[] parties = hashStocke.split(":");
    // Décode le sel depuis le Base64 (octets originaux nécessaires pour le hash)
 byte[] sel = Base64.getDecoder().decode(parties[0]);
    // Recalcule le hash avec le même sel et le même algorithme
 MessageDigest digest = MessageDigest.getInstance("SHA-256");
 digest.update(sel);
 byte[] hashCalcule = digest.digest(motDePasse.getBytes());
 String hashCalculeB64 = Base64.getEncoder().encodeToString(hashCalcule);
    // Compare le hash calculé avec le hash stocké (constante de temps non garantie ici)
 return hashCalculeB64.equals(parties[1]);
}
```

#### 5.3 Recommandations pour la production

| Algorithme | Caractéristique | Utilisation |
|------------|-----------------|-------------|
| **BCrypt** | Lent par conception, facteur de coût configurable (10-12) | Standard de facto Java |
| **Argon2** | Vainqueur du Password Hashing Competition 2015, résistant GPU/ASIC | Meilleur choix moderne |
| **PBKDF2** | Standard NIST, configurable (SHA-256, 600 000 itérations min) | Acceptable si BCrypt/Argon2 indisponible |

Tous trois intègrent **nativement** le salage et les itérations. Exemple avec Spring Security :

```java
// Exemple Spring Security : BCrypt intègre sel + itérations nativement
// OWASP A02 : BCrypt est lent par conception (facteur de coût 10-12)
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
String hash = encoder.encode("password123"); // $2a$10$... (coût 10, sel intégré au hash)
boolean ok = encoder.matches("password123", hash); // true — vérification avec extraction du sel
```

---

## PARTIE 2 -- PRATIQUE PAS A PAS (35 min)

Nous allons décortiquer chaque classe de test du lab pour comprendre **comment** on prouve la sécurité (ou l'insécurité) d'un code Java.

### 2.1 RequeteurSQLTest.java — Prouver l'injection SQL

Le test est structuré en tests unitaires simples et tests paramétrés. Trois approches de vérification complémentaires :

#### Preuve de vulnérabilité (ligne 22-26)

```java
// Test de PREUVE DE VULNÉRABILITÉ : démontre que l'injection SQL est possible
// OWASP A03 (Injection) : la charge utile est concaténée telle quelle dans le SQL
@Test
@DisplayName("Requête vulnérable : l'injection SQL est possible")
void requeteVulnerableInjectionPossible() {
    // Arrange : charge utile d'injection SQL classique (bypass d'authentification)
 String requete = requeteur.construireRequeteVulnerable("' OR '1'='1' --");
    // Act : génère la requête SQL avec la charge utile concaténée
    // Assert : vérifie que la chaîne d'injection est présente dans le SQL généré
 assertTrue(requete.contains("' OR '1'='1' --"),
 "Preuve de vulnérabilité : la chaîne d'injection est dans la requête");
}
```

**Ce qu'on prouve :** La chaîne d'injection est présente **telle quelle** dans le SQL généré. C'est la preuve irréfutable que le code est vulnérable. Le message d'assertion `"Preuve de vulnérabilité"` est pédagogique : il documente l'intention du test.

#### Preuve de correction (ligne 29-36)

```java
// Test de PREUVE DE CORRECTION : vérifie que l'injection est impossible avec la version sécurisée
// Double vérification : présence du placeholder '?' + absence de la charge utile dans le SQL
@Test
@DisplayName("Requête sécurisée : pas possible d'injecter — le paramètre '?' est utilisé")
void requeteSecuriseePasInjection() {
    // Arrange : on tente d'injecter la même charge utile que dans le test vulnérable
 String requete = requeteur.construireRequeteSecurisee("' OR '1'='1' --");
    // Act : génère la requête sécurisée (la charge utile passe par le paramètre ?)
    // Assert positif : vérifie que le paramètre '?' est présent → requête paramétrée
 assertTrue(requete.contains("?"),
 "La requête utilise un paramètre ?");
    // Assert négatif : vérifie que la charge utile N'EST PAS dans le SQL → donnée isolée
 assertFalse(requete.contains("' OR '1'='1'"),
 "Les données utilisateur NE sont PAS dans la requête");
}
```

**Double vérification :**
1. Assertion **positive** : le `?` est présent → la requête est bien paramétrée
2. Assertion **négative** : la charge utile n'apparaît pas dans le SQL → la donnée est isolée

#### Test paramétré des 5 vecteurs (ligne 38-55)

```java
// Test PARAMÉTRÉ : vérifie que la correction fonctionne pour TOUS les vecteurs d'attaque connus
// Chaque vecteur est exécuté comme un test JUnit indépendant → isolation des échecs
@Test
@DisplayName("Détection d'injection sur entrées malveillantes")
@ValueSource(strings = {
    // Bypass d'authentification (condition toujours vraie)
 "' OR '1'='1",
    // Destruction de table (DROP)
 "'; DROP TABLE users; --",
    // Exfiltration de données (UNION)
 "1' UNION SELECT * FROM users --",
    // Usurpation d'identité (commentaire)
 "admin'--",
    // Bypass (variante numérique)
 "' OR 1=1 --"
})
void entréesMalveillantes(String injection) {
    // Arrange : le vecteur d'injection est fourni par @ValueSource
    // Act : version VULNÉRABLE — la charge utile doit être concaténée dans le SQL
 String vuln = requeteur.construireRequeteVulnerable(injection);
    // Assert : prouve que l'injection est possible (la chaîne apparaît dans la requête)
 assertTrue(vuln.contains(injection),
 "La version vulnérable injecte directement : " + injection);

    // Act : version SÉCURISÉE — la charge utile passe par un paramètre lié
 String sec = requeteur.construireRequeteSecurisee(injection);
    // Assert : prouve que la correction fonctionne (la chaîne est absente du SQL)
 assertFalse(sec.contains(injection),
 "La version sécurisée n'injecte pas : " + injection);
}
```

**Pourquoi un test paramétré ?** On veut prouver que la correction fonctionne pour **tous** les vecteurs d'attaque connus, pas juste un seul. Chaque vecteur est exécuté comme un test indépendant. Si un seul échoue, le test échoue avec le vecteur précis en clair dans le message.

---

### 2.2 SanitizerXSSTest.java — Prouver l'échappement XSS

#### Preuve de vulnérabilité (ligne 21-26)

```java
// Test de PREUVE DE VULNÉRABILITÉ XSS : la balise <script> arrive intacte dans le HTML
// OWASP A03 (Injection/XSS) : sans échappement, le navigateur exécute le script
@Test
@DisplayName("Page vulnérable : le script est injecté tel quel")
void pageVulnerableScriptNonEchappe() {
    // Arrange : contenu malveillant contenant une balise script
 String page = sanitizer.genererPageAccueilVulnerable("<script>alert('XSS')</script>");
    // Act : génère la page HTML vulnérable (sans échappement)
    // Assert : la balise <script> est présente → le navigateur l'exécutera
 assertTrue(page.contains("<script>"),
 "Preuve de vulnérabilité : la balise script est présente");
}
```

Même principe que pour SQL : on vérifie que la balise `<script>` arrive **intacte** dans le HTML.

#### Preuve de correction (ligne 29-36)

```java
// Test de PREUVE DE CORRECTION XSS : le script est neutralisé par échappement HTML
// Double vérification : <script> disparaît, &lt;script&gt; apparaît
@Test
@DisplayName("Page sécurisée : le script est neutralisé")
void pageSecuriseeScriptNeutralise() {
    // Arrange : même charge utile malveillante que le test vulnérable
 String page = sanitizer.genererPageAccueilSecurisee("<script>alert('XSS')</script>");
    // Act : génère la page sécurisée (avec échappement HTML)
    // Assert négatif : la balise <script> brute n'est plus présente
 assertFalse(page.contains("<script>"),
 "La balise script est neutralisée : " + page);
    // Assert positif : la version échappée &lt;script&gt; est présente
    // Preuve que l'échappement HTML a bien eu lieu
 assertTrue(page.contains("&lt;script&gt;"),
 "Le script est échappé en entités HTML");
}
```

**Double vérification :**
1. `<script>` a disparu du HTML
2. `&lt;script&gt;` est présent → preuve que l'échappement a bien eu lieu

#### Détection de contenu malveillant (ligne 39-44)

```java
// Test de DÉTECTION POSITIVE : le détecteur identifie correctement les entrées malveillantes
// Vérifie les 4 grandes familles de signatures XSS : script, onerror, javascript:, onload
@Test
@DisplayName("Détection de contenu script malveillant")
void detectionScriptMalveillant() {
    // Arrange : 4 entrées malveillantes à tester (pas de paramètre à configurer)
    // Act : appelle contientScript() sur chaque entrée
    // Assert : chaque entrée malveillante doit être détectée comme dangereuse
 assertTrue(sanitizer.contientScript("<script>alert(1)</script>"));   // Balise script
 assertTrue(sanitizer.contientScript("<img src=x onerror=alert(1)>")); // Gestionnaire d'événement
 assertTrue(sanitizer.contientScript("javascript:void(0)"));           // Protocole javascript:
 assertTrue(sanitizer.contientScript("<body onload=alert(1)>"));       // Événement sur body
}
```

La méthode `contientScript` est un **détecteur de signatures**. Elle ne bloque pas le XSS (l'échappement le fait), mais elle permet de **logger et alerter** quand un utilisateur tente une injection.

#### Détection négative — pas de faux positifs (ligne 48-53)

```java
// Test de DÉTECTION NÉGATIVE : vérifie l'absence de faux positifs
// Un détecteur trop agressif qui bloque du contenu légitime est inutilisable en production
@Test
@DisplayName("Contenu légitime non détecté comme malveillant")
void contenuLegitimeNonDetecte() {
    // Arrange : 3 entrées légitimes (texte normal + null)
    // Act : appelle contientScript() sur chaque entrée inoffensive
    // Assert : du texte normal ne doit PAS être signalé comme malveillant
 assertFalse(sanitizer.contientScript("Bonjour tout le monde"));  // Texte français normal
 assertFalse(sanitizer.contientScript("Je m'appelle Jean"));       // Texte inoffensif
 assertFalse(sanitizer.contientScript(null));                       // null n'est pas une attaque
}
```

Un détecteur qui crie au loup sur du contenu légitime est inutile. Ce test garantit l'absence de **faux positifs**.

#### Neutralisation de 6 vecteurs XSS connus (ligne 69-83)

```java
// Test PARAMÉTRÉ : vérifie que l'échappement HTML neutralise 6 vecteurs XSS courants
// OWASP A03 (XSS) : chaque variante doit être neutralisée, pas seulement <script> classique
@ParameterizedTest
@DisplayName("Neutralisation de vecteurs XSS connus")
@ValueSource(strings = {
    // Balise script classique
 "<script>alert(1)</script>",
    // Événement sur balise auto-fermante (pas besoin de </img>)
 "<img src=x onerror='alert(1)'>",
    // Fermeture d'attribut HTML + injection
 "\"><script>alert(document.cookie)</script>",
    // SVG avec gestionnaire d'événement
 "<svg onload=alert(1)>",
    // Contexte JavaScript (apostrophes)
 "'-alert(1)-'",
    // Événement sur élément body
 "<body onload='alert(1)'>"
})
void neutralisationVecteursXSS(String vecteur) {
    // Arrange : le vecteur XSS est passé via @ValueSource
    // Act : échappe le vecteur XSS avec echapperHtml()
 String securise = sanitizer.echapperHtml(vecteur);
    // Assert 1 : la balise <script> brute n'est plus présente
 assertFalse(securise.contains("<script>"), "Script non neutralisé pour : " + vecteur);
    // Assert 2 : variante <script avec espace (ex: <script src=...>)
 assertFalse(securise.contains("<script "), "Script avec espace non neutralisé");
}
```

Chaque vecteur est échappé puis vérifié. Notez le test du `<script ` (avec espace) : il couvre des variantes comme `<script src=...>` ou `<script type=...>`.

---

### 2.3 SecuriteFichierTest.java — Prouver la protection Path Traversal

#### Test de la version vulnérable (ligne 21-26)

```java
// Test de PREUVE DE VULNÉRABILITÉ : les ../ ne sont ni filtrés ni bloqués
// OWASP A01 (Contrôle d'accès défaillant) : path traversal possible
@Test
@DisplayName("Chemin vulnérable : le path traversal est possible")
void cheminVulnerablePathTraversalPossible() {
  // Arrange : tentative de remontée vers /etc/passwd
  String chemin = securite.construireCheminVulnerable("../../etc/passwd");
  // Act : génère le chemin vulnérable (concaténation sans validation)
  // Assert : le ../ est présent tel quel → pas de validation, aucun blocage
  assertEquals("/var/data/../../etc/passwd", chemin,
 "Preuve : le ../ est concaténé tel quel");
}
```

Le `../` est présent tel quel dans le résultat. Aucune validation, aucun blocage.

#### Blocage explicite (lignes 29-33)

```java
// Test de PREUVE DE CORRECTION : le code REJETTE catégoriquement le path traversal
// assertThrows vérifie qu'une exception est levée → comportement sécurisé attendu
@Test
@DisplayName("Chemin sécurisé : les ../ sont bloqués")
void cheminSecurisePathTraversalBloque() {
 // Arrange : tentative de path traversal avec ../../
 // Act : appelle construireCheminSecurise avec entrée malveillante
 // Assert : la méthode doit lever une exception (rejet catégorique)
 // Le rejet catégorique est préférable au "nettoyage" silencieux
 assertThrows(IllegalArgumentException.class,
 () -> securite.construireCheminSecurise("../../etc/passwd"));
}
```

On vérifie qu'une **exception** est levée, pas qu'un chemin modifié est retourné. Le comportement correct face à une attaque est le **rejet catégorique**, pas le "nettoyage" silencieux qui pourrait rater un cas.

#### Blocage des chemins absolus (ligne 36-40) et backslash Windows (ligne 53-57)

```java
// Test rapide : les chemins absolus (commençant par /) sont également bloqués
// Un chemin absolu sort du répertoire autorisé, même sans ..
// Assert : vérifie qu'une exception est levée pour "/etc/passwd"
assertThrows(IllegalArgumentException.class,
 () -> securite.construireCheminSecurise("/etc/passwd"));

// Test de compatibilité cross-platform : les backslash Windows sont aussi bloqués
// Même sous Linux, un attaquant peut utiliser des techniques cross-platform
// Assert : vérifie qu'une exception est levée pour "..\\..\\windows\\system32"
assertThrows(IllegalArgumentException.class,
 () -> securite.construireCheminSecurise("..\\..\\windows\\system32"));
```

**Pourquoi tester `\` ?** Même si notre application tourne sous Linux, un attaquant peut cibler un déploiement Windows ou utiliser des techniques cross-platform. La sécurité doit être **indépendante de la plateforme**.

#### Test du cas nominal (ligne 43-50)

```java
// Test du CAS NOMINAL : la sécurité ne doit PAS bloquer les fichiers légitimes
// Une sécurité trop agressive qui bloque tout est inutilisable
@Test
@DisplayName("Chemin sécurisé : nom de fichier valide accepté")
void nomFichierValideAccepte() {
 // Arrange : nom de fichier inoffensif
 String chemin = securite.construireCheminSecurise("rapport.pdf");
 // Act : construit le chemin sécurisé avec un fichier légitime
 // Assert 1 : vérifie que le nom du fichier est conservé dans le chemin final
 assertTrue(chemin.endsWith("rapport.pdf"),
 "Le nom de fichier valide est accepté");
 // Assert 2 : vérifie que le chemin reste dans le répertoire autorisé
 assertTrue(chemin.startsWith("/var/data/"),
 "Le chemin est dans le répertoire autorisé");
}
```

Une sécurité qui bloque tout n'est pas une bonne sécurité. On vérifie que les **cas légitimes** continuent de fonctionner.

#### Détection paramétrée (ligne 71-83)

```java
// Test PARAMÉTRÉ : vérifie que le détecteur identifie 5 variantes de path traversal
// Couvre les séparateurs Linux (../), Windows (..\\), chemins absolus (/),
// traversée au milieu (foo/../bar) et null byte (\0)
@ParameterizedTest
@DisplayName("Détection positive de path traversal")
@ValueSource(strings = {
 "../../etc/passwd",            // Remontée standard Linux
 "..\\..\\windows\\system32",   // Remontée standard Windows
 "/etc/passwd",                 // Chemin absolu
 "foo/../bar",                  // Traversée au milieu du chemin
 "file.txt\0.jpg"               // Null byte injection (troncature)
})
void detectionPositive(String input) {
    // Arrange : l'entrée malveillante est passée via @ValueSource
    // Act : appelle estTentativePathTraversal() sur l'entrée
    // Assert : chaque entrée doit être détectée comme une tentative de path traversal
 assertTrue(securite.estTentativePathTraversal(input),
 "Devrait détecter une tentative : " + input);
}
```

5 variantes de path traversal testées, incluant le **null byte injection** (`\0`).

---

### 2.4 GestionnaireMotDePasseTest.java — Prouver le hachage sécurisé

#### Preuve du problème : SHA-256 déterministe (ligne 20-24)

```java
// Test de PREUVE DE VULNÉRABILITÉ (A02) : SHA-256 sans sel est déterministe
// Deux appels avec le même mot de passe produisent le même hash → rainbow tables possibles
@Test
@DisplayName("Hachage vulnérable : déterministe, même entrée → même sortie")
void hachageVulnerableDeterministe() {
    // Arrange : deux appels avec le même mot de passe
 String h1 = gestionnaire.hacherVulnerable("password123");
 String h2 = gestionnaire.hacherVulnerable("password123");
    // Act : appelle la méthode de hachage vulnérable (SHA-256 sans sel) deux fois
    // Assert : le hachage sans sel retourne le même hash → vulnérabilité démontrée
 assertEquals(h1, h2,
 "Le hachage sans sel produit toujours le même résultat → vulnérable aux rainbow tables");
}
```

C'est le test le plus important du module : il **démontre** la vulnérabilité des fonctions de hachage sans sel. Le message d'assertion explique le *pourquoi* : rainbow tables.

#### Preuve de la correction : SHA-256 avec sel aléatoire (ligne 28-33)

```java
// Test de PREUVE DE CORRECTION (A02) : SHA-256 avec sel est non déterministe
// Même mot de passe, deux hashs différents → rainbow tables inutilisables
@Test
@DisplayName("Hachage sécurisé : aléatoire, même entrée → sorties différentes")
void hachageSecuriseAleatoire() {
    // Arrange : deux appels avec le même mot de passe
 String h1 = gestionnaire.hacherAvecSel("password123");
 String h2 = gestionnaire.hacherAvecSel("password123");
    // Act : appelle la méthode sécurisée (SHA-256 avec sel) deux fois
    // Assert : le sel aléatoire garantit des hashs différents
 assertNotEquals(h1, h2,
 "Avec un sel aléatoire, le même mot de passe produit des hashs différents");
}
```

Même mot de passe, deux hashs **différents**. C'est la preuve que le salage rend les rainbow tables inutilisables.

#### Vérification positive et négative (lignes 37-44)

```java
// Test de VÉRIFICATION POSITIVE : le bon mot de passe est accepté
// Génère un hash pour "monSuperMotDePasse" puis vérifie qu'il correspond
// Arrange : hash du mot de passe à tester
String hash = gestionnaire.hacherAvecSel("monSuperMotDePasse");
// Act : vérifie le mot de passe correct contre le hash
// Assert : le même mot de passe doit passer la vérification
assertTrue(gestionnaire.verifierMotDePasse("monSuperMotDePasse", hash));

// Test de VÉRIFICATION NÉGATIVE : le mauvais mot de passe est rejeté
// Arrange : hash du mot de passe "motDePasse"
String hash = gestionnaire.hacherAvecSel("motDePasse");
// Act : vérifie un mot de passe différent contre le hash
// Assert : un mot de passe différent est rejeté (sans fuite d'information)
assertFalse(gestionnaire.verifierMotDePasse("mauvaisMotDePasse", hash));
```

La vérification fonctionne dans les deux sens : elle **accepte** le bon mot de passe, elle **rejette** le mauvais.

#### Gestion des cas limites (lignes 51-55)

```java
// Test de ROBUSTESSE : le code ne doit pas crasher sur des entrées invalides
// Un hash sans séparateur ':' ou un hash null doivent retourner false silencieusement
@Test
@DisplayName("Vérification : hash invalide rejeté")
void verificationHashInvalide() {
    // Arrange : entrées invalides (hash mal formé, hash null)
    // Act : appelle verifierMotDePasse avec ces entrées corrompues
    // Assert 1 : hash mal formé (sans ':') → doit retourner false
 assertFalse(gestionnaire.verifierMotDePasse("test", "hash_invalide"));
    // Assert 2 : hash null → doit retourner false (pas de NullPointerException)
 assertFalse(gestionnaire.verifierMotDePasse("test", null));
}
```

Le code doit être robuste face à des entrées corrompues : un hash sans `:`, un hash `null` — dans ces cas, `verifierMotDePasse` retourne `false` sans crasher.

#### Test de structure (lignes 58-67)

```java
// Test de CONTRAT DE FORMAT : vérifie que le hash stocké respecte le format sel:hash
// Si le format change (ex: on passe à BCrypt), ce test le signale immédiatement
@Test
@DisplayName("Hash sécurisé contient le sel (format sel:hash)")
void hashSecuriseContientSel() {
    // Arrange : génère un hash avec sel
 String hash = gestionnaire.hacherAvecSel("test");
    // Act : analyse le format du hash (split sur ':')
    // Assert 1 : le séparateur ':' est présent (sépare le sel du hash)
 assertTrue(hash.contains(":"));
    // Assert 2 : il y a exactement 2 parties (sel et hash)
 String[] parties = hash.split(":");
 assertEquals(2, parties.length);
    // Assert 3 : le sel n'est pas vide
 assertFalse(parties[0].isEmpty(), "Le sel ne doit pas être vide");
    // Assert 4 : le hash n'est pas vide
 assertFalse(parties[1].isEmpty(), "Le hash ne doit pas être vide");
}
```

Ce test vérifie le **format de stockage**, pas la sécurité cryptographique. Le format `sel:hash` est un contrat implicite : si quelqu'un modifie la méthode pour stocker différemment, ce test le signale.

---

## PARTIE 3 -- LAB (20 min)

### Objectif

Corriger une classe `Authentificateur` vulnérable fournie dans le dossier `labs/lab05-owasp-java`.

### Contexte

Vous recevez une classe `Authentificateur` avec deux méthodes vulnérables :
- `rechercherUtilisateur(String login)` — concatène le login dans une requête SQL
- `afficherProfil(String html)` — affiche du HTML sans échappement

### Travail à réaliser

1. **Corriger l'injection SQL** dans la recherche d'utilisateur :
 - Remplacer la concaténation par une requête paramétrée avec `?`
 - Pas de `String.format`, pas de `+`, pas de `StringBuilder`

2. **Corriger le XSS** dans l'affichage du profil :
 - Échapper les 5 caractères HTML critiques avant de générer la page
 - Gérer le cas `null` (retourner `""`)

3. **Écrire les tests de sécurité** correspondants :
 - Test d'injection SQL : prouver que la charge utile n'atteint pas la requête
 - Test XSS : prouver que `<script>` devient `&lt;script&gt;`
 - Test de non-régression : un login normal fonctionne toujours

### Critères de réussite

- Tous les tests de sécurité passent au `mvn test`
- Les tests couvrent au moins 3 vecteurs d'injection SQL
- Les tests couvrent au moins 3 vecteurs XSS
- Le cas `null` est géré pour l'échappement HTML

### Commandes utiles

```bash
# Lancer les tests du lab 05
mvn test -pl lab05-owasp-java

# Vérifier la couverture de test
mvn test jacoco:report -pl lab05-owasp-java
```

---

## FICHE MEMO

### Injection SQL

| À ne pas faire | À faire |
|-------------------|-------------|
| `"SELECT * FROM users WHERE nom = '" + nom + "'"` | `"SELECT * FROM users WHERE nom = ?"` |
| Concaténer des données utilisateur dans le SQL | Utiliser `PreparedStatement` / paramètres liés |
| Faire confiance aux entrées | Traiter TOUTE entrée comme malveillante |
| LIKE sans échappement | Échapper `%` et `_` avant de passer au paramètre LIKE |

### XSS

| À ne pas faire | À faire |
|-------------------|-------------|
| Afficher du contenu utilisateur sans échappement | `echapperHtml(input)` avant tout affichage |
| Échapper `&` en dernier | Échapper `&` **en premier** (`&amp;`) |
| Réinventer l'échappement HTML | Utiliser OWASP Java Encoder en production |

**Les 5 caractères à échapper :** `<` → `&lt;` | `>` → `&gt;` | `&` → `&amp;` | `"` → `&quot;` | `'` → `&#39;`

### Path Traversal

```
Défense en profondeur :
1. Rejeter les caractères dangereux (.., /, \, \0)
2. Normaliser le Path (Paths.get().normalize())
3. Vérifier startsWith sur le répertoire de base
4. Ne jamais faire confiance à la normalisation seule
```

### Mots de passe

```
 SHA-256 sans sel → déterministe → rainbow tables
 MD5, SHA-1 → cassés
 BCrypt, Argon2, PBKDF2 → sel + itérations intégrés
 Sel unique par utilisateur
 Au moins 600 000 itérations (PBKDF2) ou cost 10-12 (BCrypt)
```

### Assertions de sécurité — patterns

| Type de preuve | Assertion |
|----------------|-----------|
| La charge utile est dans la sortie | `assertTrue(sortie.contains(injection))` → **vulnérable** |
| La charge utile est absente de la sortie | `assertFalse(sortie.contains(injection))` → **corrigé** |
| Le code rejette l'attaque | `assertThrows(SecurityException.class, ...)` → **corrigé** |
| Même entrée → même hash | `assertEquals(h1, h2)` → **vulnérable (pas de sel)** |
| Même entrée → hashs différents | `assertNotEquals(h1, h2)` → **corrigé (sel aléatoire)** |
| Cas `null` géré | `assertEquals("", methode(null))` ou `assertFalse(methode(null))` |
