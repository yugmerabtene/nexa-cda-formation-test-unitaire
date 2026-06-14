# Module 5 : Vulnérabilités OWASP en Java

**Durée :** 1h30 (9h00–10h30) — Jour 2 Matin
**Prérequis :** Module 1 à 4 (JUnit, Mockito, assertions, tests paramétrés)
**Projet support :** `labs/lab05-owasp-java` (Java 17, Maven, JUnit Jupiter 5.10.2)

---

## PARTIE 1 — THÉORIE (35 min)

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
public String construireRequeteVulnerable(String nom) {
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
public String construireRequeteSecurisee(String nom) {
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
    return "SELECT * FROM produits WHERE nom LIKE '%" + terme + "%'";
}
```

Un attaquant peut injecter `%` pour forcer un `LIKE '%%%'` qui matche tout.

La correction (`RequeteurSQL.java:17-19`) paramétrise la requête, et la méthode `encoderParametrePourLike` (`RequeteurSQL.java:21-23`) échappe les métacaractères du `LIKE` :

```java
public String construireRechercheSecurisee() {
    return "SELECT * FROM produits WHERE nom LIKE ?";
}

public String encoderParametrePourLike(String terme) {
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
public boolean contientScript(String input) {
    if (input == null) return false;
    String lower = input.toLowerCase();
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
public String echapperHtml(String input) {
    if (input == null) return "";
    return input
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
}
```

Le test `echappementCaracteresSpeciaux` (`SanitizerXSSTest.java:57-60`) vérifie que les 5 caractères sont remplacés :

```java
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
    return REPERTOIRE_AUTORISE + nomFichier;  // "/var/data/" + "../../etc/passwd"
}
```

Résultat : `/var/data/../../etc/passwd` → après résolution : `/etc/passwd`.

#### 4.2 La correction en couches

La version sécurisée (`SecuriteFichier.java:14-30`) applique une **défense en profondeur** :

```java
public String construireCheminSecurise(String nomFichier) {
    // Couche 1 : rejet explicite des caractères dangereux
    if (nomFichier == null || nomFichier.isEmpty()) {
        throw new IllegalArgumentException("Nom de fichier invalide");
    }
    if (nomFichier.contains("..") || nomFichier.contains("/")
            || nomFichier.contains("\\")) {
        throw new IllegalArgumentException("Caractères interdits dans le nom de fichier");
    }

    // Couche 2 : normalisation et vérification startsWith
    Path base = Paths.get(REPERTOIRE_AUTORISE).normalize();
    Path fichier = base.resolve(nomFichier).normalize();

    if (!fichier.startsWith(base)) {
        throw new SecurityException("Tentative de path traversal détectée");
    }

    return fichier.toString();
}
```

**Pourquoi les deux couches ?** La normalisation seule ne suffit pas toujours (contournements Unicode, encodages alternatifs). La liste noire explicite et le `startsWith` après normalisation forment une double protection.

La détection proactive (`SecuriteFichier.java:32-36`) vérifie les motifs suspects :

```java
public boolean estTentativePathTraversal(String input) {
    if (input == null) return false;
    return input.contains("../") || input.contains("..\\")
        || input.contains("/..") || input.contains("\\..")
        || input.startsWith("/") || input.contains("\0");
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
public String hacherVulnerable(String motDePasse) {
    try {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(motDePasse.getBytes());
        return Base64.getEncoder().encodeToString(hash);
    } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException(e);
    }
}
```

Ce hachage est **déterministe** : `hacherVulnerable("password123")` produit **toujours le même hash**. Le test le prouve (`GestionnaireMotDePasseTest.java:20-24`) :

```java
String h1 = gestionnaire.hacherVulnerable("password123");
String h2 = gestionnaire.hacherVulnerable("password123");
assertEquals(h1, h2);  // Même entrée → même hash
```

Conséquence : un attaquant peut précalculer une **rainbow table** (SHA-256("password123") = X, SHA-256("123456") = Y...) et retrouver instantanément les mots de passe communs.

**Hachage sécurisé** (`GestionnaireMotDePasse.java:20-34`) :
```java
public String hacherAvecSel(String motDePasse) {
    try {
        SecureRandom random = new SecureRandom();
        byte[] sel = new byte[16];
        random.nextBytes(sel);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(sel);  // Le sel est combiné AVANT le mot de passe
        byte[] hash = digest.digest(motDePasse.getBytes());

        return Base64.getEncoder().encodeToString(sel) + ":" + Base64.getEncoder().encodeToString(hash);
    } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException(e);
    }
}
```

Le test prouve la non-déterminisme (`GestionnaireMotDePasseTest.java:28-33`) :

```java
String h1 = gestionnaire.hacherAvecSel("password123");
String h2 = gestionnaire.hacherAvecSel("password123");
assertNotEquals(h1, h2);  // Même entrée → hashs différents (sel aléatoire)
```

Format de sortie : `sel:hash` (sel en Base64, suivi de `:`, puis le hash en Base64). La vérification (`GestionnaireMotDePasse.java:36-51`) extrait le sel stocké, recalcule le hash, et compare :

```java
public boolean verifierMotDePasse(String motDePasse, String hashStocke) {
    String[] parties = hashStocke.split(":");
    byte[] sel = Base64.getDecoder().decode(parties[0]);
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    digest.update(sel);
    byte[] hashCalcule = digest.digest(motDePasse.getBytes());
    String hashCalculeB64 = Base64.getEncoder().encodeToString(hashCalcule);
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
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
String hash = encoder.encode("password123");        // $2a$10$...
boolean ok  = encoder.matches("password123", hash); // true
```

---

## PARTIE 2 — PRATIQUE PAS À PAS (35 min)

Nous allons décortiquer chaque classe de test du lab pour comprendre **comment** on prouve la sécurité (ou l'insécurité) d'un code Java.

### 2.1 RequeteurSQLTest.java — Prouver l'injection SQL

Le test est structuré en tests unitaires simples et tests paramétrés. Trois approches de vérification complémentaires :

#### Preuve de vulnérabilité (ligne 22-26)

```java
@Test
@DisplayName("Requête vulnérable : l'injection SQL est possible")
void requeteVulnerableInjectionPossible() {
    String requete = requeteur.construireRequeteVulnerable("' OR '1'='1' --");
    assertTrue(requete.contains("' OR '1'='1' --"),
        "Preuve de vulnérabilité : la chaîne d'injection est dans la requête");
}
```

**Ce qu'on prouve :** La chaîne d'injection est présente **telle quelle** dans le SQL généré. C'est la preuve irréfutable que le code est vulnérable. Le message d'assertion `"Preuve de vulnérabilité"` est pédagogique : il documente l'intention du test.

#### Preuve de correction (ligne 29-36)

```java
@Test
@DisplayName("Requête sécurisée : pas possible d'injecter — le paramètre '?' est utilisé")
void requeteSecuriseePasInjection() {
    String requete = requeteur.construireRequeteSecurisee("' OR '1'='1' --");
    assertTrue(requete.contains("?"),
        "La requête utilise un paramètre ?");
    assertFalse(requete.contains("' OR '1'='1'"),
        "Les données utilisateur NE sont PAS dans la requête");
}
```

**Double vérification :**
1. Assertion **positive** : le `?` est présent → la requête est bien paramétrée
2. Assertion **négative** : la charge utile n'apparaît pas dans le SQL → la donnée est isolée

#### Test paramétré des 5 vecteurs (ligne 38-55)

```java
@ParameterizedTest
@DisplayName("Détection d'injection sur entrées malveillantes")
@ValueSource(strings = {
    "' OR '1'='1",
    "'; DROP TABLE users; --",
    "1' UNION SELECT * FROM users --",
    "admin'--",
    "' OR 1=1 --"
})
void entréesMalveillantes(String injection) {
    String vuln = requeteur.construireRequeteVulnerable(injection);
    assertTrue(vuln.contains(injection),
        "La version vulnérable injecte directement : " + injection);

    String sec = requeteur.construireRequeteSecurisee(injection);
    assertFalse(sec.contains(injection),
        "La version sécurisée n'injecte pas : " + injection);
}
```

**Pourquoi un test paramétré ?** On veut prouver que la correction fonctionne pour **tous** les vecteurs d'attaque connus, pas juste un seul. Chaque vecteur est exécuté comme un test indépendant. Si un seul échoue, le test échoue avec le vecteur précis en clair dans le message.

---

### 2.2 SanitizerXSSTest.java — Prouver l'échappement XSS

#### Preuve de vulnérabilité (ligne 21-26)

```java
@Test
@DisplayName("Page vulnérable : le script est injecté tel quel")
void pageVulnerableScriptNonEchappe() {
    String page = sanitizer.genererPageAccueilVulnerable("<script>alert('XSS')</script>");
    assertTrue(page.contains("<script>"),
        "Preuve de vulnérabilité : la balise script est présente");
}
```

Même principe que pour SQL : on vérifie que la balise `<script>` arrive **intacte** dans le HTML.

#### Preuve de correction (ligne 29-36)

```java
@Test
@DisplayName("Page sécurisée : le script est neutralisé")
void pageSecuriseeScriptNeutralise() {
    String page = sanitizer.genererPageAccueilSecurisee("<script>alert('XSS')</script>");
    assertFalse(page.contains("<script>"),
        "La balise script est neutralisée : " + page);
    assertTrue(page.contains("&lt;script&gt;"),
        "Le script est échappé en entités HTML");
}
```

**Double vérification :**
1. `<script>` a disparu du HTML
2. `&lt;script&gt;` est présent → preuve que l'échappement a bien eu lieu

#### Détection de contenu malveillant (ligne 39-44)

```java
@Test
@DisplayName("Détection de contenu script malveillant")
void detectionScriptMalveillant() {
    assertTrue(sanitizer.contientScript("<script>alert(1)</script>"));
    assertTrue(sanitizer.contientScript("<img src=x onerror=alert(1)>"));
    assertTrue(sanitizer.contientScript("javascript:void(0)"));
    assertTrue(sanitizer.contientScript("<body onload=alert(1)>"));
}
```

La méthode `contientScript` est un **détecteur de signatures**. Elle ne bloque pas le XSS (l'échappement le fait), mais elle permet de **logger et alerter** quand un utilisateur tente une injection.

#### Détection négative — pas de faux positifs (ligne 48-53)

```java
@Test
@DisplayName("Contenu légitime non détecté comme malveillant")
void contenuLegitimeNonDetecte() {
    assertFalse(sanitizer.contientScript("Bonjour tout le monde"));
    assertFalse(sanitizer.contientScript("Je m'appelle Jean"));
    assertFalse(sanitizer.contientScript(null));
}
```

Un détecteur qui crie au loup sur du contenu légitime est inutile. Ce test garantit l'absence de **faux positifs**.

#### Neutralisation de 6 vecteurs XSS connus (ligne 69-83)

```java
@ParameterizedTest
@DisplayName("Neutralisation de vecteurs XSS connus")
@ValueSource(strings = {
    "<script>alert(1)</script>",
    "<img src=x onerror='alert(1)'>",
    "\"><script>alert(document.cookie)</script>",
    "<svg onload=alert(1)>",
    "'-alert(1)-'",
    "<body onload='alert(1)'>"
})
void neutralisationVecteursXSS(String vecteur) {
    String securise = sanitizer.echapperHtml(vecteur);
    assertFalse(securise.contains("<script>"), "Script non neutralisé pour : " + vecteur);
    assertFalse(securise.contains("<script "), "Script avec espace non neutralisé");
}
```

Chaque vecteur est échappé puis vérifié. Notez le test du `<script ` (avec espace) : il couvre des variantes comme `<script src=...>` ou `<script type=...>`.

---

### 2.3 SecuriteFichierTest.java — Prouver la protection Path Traversal

#### Test de la version vulnérable (ligne 21-26)

```java
@Test
@DisplayName("Chemin vulnérable : le path traversal est possible")
void cheminVulnerablePathTraversalPossible() {
    String chemin = securite.construireCheminVulnerable("../../etc/passwd");
    assertEquals("/var/data/../../etc/passwd", chemin,
        "Preuve : le ../ est concaténé tel quel");
}
```

Le `../` est présent tel quel dans le résultat. Aucune validation, aucun blocage.

#### Blocage explicite (lignes 29-33)

```java
@Test
@DisplayName("Chemin sécurisé : les ../ sont bloqués")
void cheminSecurisePathTraversalBloque() {
    assertThrows(IllegalArgumentException.class,
        () -> securite.construireCheminSecurise("../../etc/passwd"));
}
```

On vérifie qu'une **exception** est levée, pas qu'un chemin modifié est retourné. Le comportement correct face à une attaque est le **rejet catégorique**, pas le "nettoyage" silencieux qui pourrait rater un cas.

#### Blocage des chemins absolus (ligne 36-40) et backslash Windows (ligne 53-57)

```java
// Chemins absolus
assertThrows(IllegalArgumentException.class,
    () -> securite.construireCheminSecurise("/etc/passwd"));

// Backslash Windows
assertThrows(IllegalArgumentException.class,
    () -> securite.construireCheminSecurise("..\\..\\windows\\system32"));
```

**Pourquoi tester `\` ?** Même si notre application tourne sous Linux, un attaquant peut cibler un déploiement Windows ou utiliser des techniques cross-platform. La sécurité doit être **indépendante de la plateforme**.

#### Test du cas nominal (ligne 43-50)

```java
@Test
@DisplayName("Chemin sécurisé : nom de fichier valide accepté")
void nomFichierValideAccepte() {
    String chemin = securite.construireCheminSecurise("rapport.pdf");
    assertTrue(chemin.endsWith("rapport.pdf"),
        "Le nom de fichier valide est accepté");
    assertTrue(chemin.startsWith("/var/data/"),
        "Le chemin est dans le répertoire autorisé");
}
```

Une sécurité qui bloque tout n'est pas une bonne sécurité. On vérifie que les **cas légitimes** continuent de fonctionner.

#### Détection paramétrée (ligne 71-83)

```java
@ParameterizedTest
@DisplayName("Détection positive de path traversal")
@ValueSource(strings = {
    "../../etc/passwd",
    "..\\..\\windows\\system32",
    "/etc/passwd",
    "foo/../bar",
    "file.txt\0.jpg"
})
void detectionPositive(String input) {
    assertTrue(securite.estTentativePathTraversal(input),
        "Devrait détecter une tentative : " + input);
}
```

5 variantes de path traversal testées, incluant le **null byte injection** (`\0`).

---

### 2.4 GestionnaireMotDePasseTest.java — Prouver le hachage sécurisé

#### Preuve du problème : SHA-256 déterministe (ligne 20-24)

```java
@Test
@DisplayName("Hachage vulnérable : déterministe, même entrée → même sortie")
void hachageVulnerableDeterministe() {
    String h1 = gestionnaire.hacherVulnerable("password123");
    String h2 = gestionnaire.hacherVulnerable("password123");
    assertEquals(h1, h2,
        "Le hachage sans sel produit toujours le même résultat → vulnérable aux rainbow tables");
}
```

C'est le test le plus important du module : il **démontre** la vulnérabilité des fonctions de hachage sans sel. Le message d'assertion explique le *pourquoi* : rainbow tables.

#### Preuve de la correction : SHA-256 avec sel aléatoire (ligne 28-33)

```java
@Test
@DisplayName("Hachage sécurisé : aléatoire, même entrée → sorties différentes")
void hachageSecuriseAleatoire() {
    String h1 = gestionnaire.hacherAvecSel("password123");
    String h2 = gestionnaire.hacherAvecSel("password123");
    assertNotEquals(h1, h2,
        "Avec un sel aléatoire, le même mot de passe produit des hashs différents");
}
```

Même mot de passe, deux hashs **différents**. C'est la preuve que le salage rend les rainbow tables inutilisables.

#### Vérification positive et négative (lignes 37-44)

```java
// Correct accepté
String hash = gestionnaire.hacherAvecSel("monSuperMotDePasse");
assertTrue(gestionnaire.verifierMotDePasse("monSuperMotDePasse", hash));

// Incorrect rejeté
String hash = gestionnaire.hacherAvecSel("motDePasse");
assertFalse(gestionnaire.verifierMotDePasse("mauvaisMotDePasse", hash));
```

La vérification fonctionne dans les deux sens : elle **accepte** le bon mot de passe, elle **rejette** le mauvais.

#### Gestion des cas limites (lignes 51-55)

```java
@Test
@DisplayName("Vérification : hash invalide rejeté")
void verificationHashInvalide() {
    assertFalse(gestionnaire.verifierMotDePasse("test", "hash_invalide"));
    assertFalse(gestionnaire.verifierMotDePasse("test", null));
}
```

Le code doit être robuste face à des entrées corrompues : un hash sans `:`, un hash `null` — dans ces cas, `verifierMotDePasse` retourne `false` sans crasher.

#### Test de structure (lignes 58-67)

```java
@Test
@DisplayName("Hash sécurisé contient le sel (format sel:hash)")
void hashSecuriseContientSel() {
    String hash = gestionnaire.hacherAvecSel("test");
    assertTrue(hash.contains(":"));
    String[] parties = hash.split(":");
    assertEquals(2, parties.length);
    assertFalse(parties[0].isEmpty(), "Le sel ne doit pas être vide");
    assertFalse(parties[1].isEmpty(), "Le hash ne doit pas être vide");
}
```

Ce test vérifie le **format de stockage**, pas la sécurité cryptographique. Le format `sel:hash` est un contrat implicite : si quelqu'un modifie la méthode pour stocker différemment, ce test le signale.

---

## PARTIE 3 — LAB (20 min)

### 🎯 Objectif

Corriger une classe `Authentificateur` vulnérable fournie par le formateur.

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

### ✅ Critères de réussite

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

## FICHE MÉMO

### Injection SQL

| ❌ À ne pas faire | ✅ À faire |
|-------------------|-------------|
| `"SELECT * FROM users WHERE nom = '" + nom + "'"` | `"SELECT * FROM users WHERE nom = ?"` |
| Concaténer des données utilisateur dans le SQL | Utiliser `PreparedStatement` / paramètres liés |
| Faire confiance aux entrées | Traiter TOUTE entrée comme malveillante |
| LIKE sans échappement | Échapper `%` et `_` avant de passer au paramètre LIKE |

### XSS

| ❌ À ne pas faire | ✅ À faire |
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
❌ SHA-256 sans sel     → déterministe → rainbow tables
❌ MD5, SHA-1           → cassés
✅ BCrypt, Argon2, PBKDF2  → sel + itérations intégrés
✅ Sel unique par utilisateur
✅ Au moins 600 000 itérations (PBKDF2) ou cost 10-12 (BCrypt)
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
