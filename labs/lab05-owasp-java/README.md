# Lab05 - Vulnerabilites OWASP en Java

## Objectif

Identifier et corriger les vulnerabilites de securite les plus courantes selon le Top 10 OWASP dans une application Java : injection SQL, Cross-Site Scripting (XSS), Path Traversal et gestion des mots de passe. Les tests unitaires prouvent les failles et verifient les correctifs.

## Enonce

Vous integrez une equipe de securite applicative. L'audit d'une application Java existante a revele plusieurs vulnerabilites critiques. Votre mission consiste a comprendre chaque faille, ecrire des tests unitaires qui la demontrent, puis implementer une version securisee. Les quatre themes abordes sont l'injection SQL, le XSS, le Path Traversal et le hachage de mots de passe. Chaque classe du package `com.nexa.owasp` presente une methode vulnerable et une methode securisee correspondante.

## Prerequis

- JDK 17 ou superieur
- Apache Maven 3.8+
- Connaissances de base en securite OWASP Top 10
- IDE Java (IntelliJ, Eclipse, VS Code)

## Etapes

### Etape 1 : Analyser la vulnerabilite d'injection SQL

Ouvrir la classe `RequeteurSQLVulnerable` dans `src/main/java/com/nexa/owasp/RequeteurSQL.java`. La methode `construireRequeteVulnerable` concatene directement les entrees utilisateur dans une requete SQL, rendant possible une injection. La methode `construireRequeteSecurisee` utilise un parametre prepare (`?`). Lire la classe de test `RequeteurSQLTest` pour comprendre comment les tests detectent l'injection.

### Etape 2 : Comprendre le Cross-Site Scripting (XSS)

Examiner la classe `SanitizerXSS`. La methode `genererPageAccueilVulnerable` insere du HTML utilisateur sans filtrage, permettant l'injection de balises `<script>`. La version securisee `genererPageAccueilSecurisee` echappe les caracteres speciaux HTML via `echapperHtml`. La methode `contientScript` permet de detecter des motifs XSS connus. Consulter `SanitizerXSSTest` pour les scenarios de test.

### Etape 3 : Etudier le Path Traversal

La classe `SecuriteFichier` illustre la vulnerabilite Path Traversal. `construireCheminVulnerable` concatene directement le nom de fichier sans validation, permettant de remonter l'arborescence (`../../etc/passwd`). `construireCheminSecurise` valide le nom, bloque les caracteres interdits, normalise le chemin et verifie qu'il reste dans le repertoire autorise. Les tests dans `SecuriteFichierTest` couvrent les cas de detection.

### Etape 4 : Corriger le hachage des mots de passe

La classe `GestionnaireMotDePasse` contient `hacherVulnerable` qui utilise SHA-256 sans sel, produisant un hash deterministe vulnerable aux rainbow tables. `hacherAvecSel` ajoute un sel aleatoire de 16 octets avant le hachage, rendant chaque hash unique. `verifierMotDePasse` permet de verifier un mot de passe en separant le sel du hash stocke. Les tests dans `GestionnaireMotDePasseTest` comparent les deux approches.

### Etape 5 : Executer les tests unitaires

Lancer la commande `mvn test` a la racine du projet. Tous les tests doivent passer. Observer les sorties JUnit qui indiquent pour chaque test si la version vulnerable laisse passer l'attaque et si la version securisee la bloque.

### Etape 6 : Analyser les resultats et les logs

Verifier que chaque test unitaire :
- Prouve la vulnerabilite sur la methode vulnerable (assertTrue sur l'injection)
- Confirme le blocage sur la methode securisee (assertFalse sur l'injection)
- Couvre les cas nominaux et les cas limites (null, entrees vides)
- Valide les fonctions de detection

## Execution

```bash
cd labs/lab05-owasp-java
mvn test
```

Pour executer une classe de test specifique :

```bash
mvn test -Dtest=AuthentificateurTest
mvn test -Dtest=RequeteurSQLTest
mvn test -Dtest=SanitizerXSSTest
mvn test -Dtest=SecuriteFichierTest
mvn test -Dtest=GestionnaireMotDePasseTest
```

Pour compiler sans executer les tests :

```bash
mvn compile
```

## Criteres de reussite

- Tous les tests unitaires passent avec `mvn test` sans erreur ni echec
- Chaque classe expose au moins une methode vulnerable et une methode securisee
- Les tests demontrent explicitement la faille sur la version vulnerable
- Les tests confirment l'absence de faille sur la version securisee
- La couverture comprend les cas nominaux, les cas limites (null, chaines vides) et les vecteurs d'attaque multiples
- Le hachage avec sel produit des resultats differents pour un meme mot de passe
- Les tentatives de Path Traversal sont detectees et bloquees
- Les scripts XSS sont echappes en entites HTML sans etre executes
- Le code compile avec `mvn compile` sans avertissement
