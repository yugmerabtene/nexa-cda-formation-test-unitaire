# Formation Tests Unitaires et Securite Java

**Ecole Nexa -- 2 jours (14 heures)**

---

## Sommaire

- [Bienvenue](#bienvenue)
- [Prerequis](#prerequis)
- [Installation pas a pas](#installation-pas-a-pas)
- [Structure du projet](#structure-du-projet)
- [Parcours de la formation](#parcours-de-la-formation)
- [Comment utiliser ce depot](#comment-utiliser-ce-depot)
- [Depannage](#depannage)
- [Pour aller plus loin](#pour-aller-plus-loin)

---

## Bienvenue

Cette formation vous apprendra a ecrire des tests unitaires de qualite avec JUnit 5 et Mockito, a appliquer la methode TDD, et a securiser vos applications Java avec Spring Security et JWT.

Vous trouverez dans ce depot :

- **`cours/`** -- 8 modules pedagogiques detailles (theorie, pratique pas a pas, exercices, fiches memo)
- **`labs/`** -- Le code source des 8 travaux pratiques (propre, fonctionnel, sans commentaire)

---

## Prerequis

Avant la formation, installez les outils suivants :

| Outil | Version minimale | Verification |
|---|---|---|
| **Docker Desktop** | 24+ | `docker --version` |
| **Git** | 2.30+ | `git --version` |
| **VS Code** ou **IntelliJ IDEA** |   | Editeur de code |
| **Java 17** (optionnel) | 17 (Temurin) | `java -version` |

> Les labs s'executent dans des conteneurs Docker. Vous n'avez pas besoin d'installer Maven ni Java localement.

---

## Installation pas a pas

### 1. Installer Docker Desktop

- **Windows / macOS** : telecharger depuis [docker.com](https://www.docker.com/products/docker-desktop/) et installer
- **Linux** : `sudo apt install docker.io docker-compose-v2`

Verifiez que Docker fonctionne :

```bash
docker --version
docker compose version
```

### 2. Cloner le depot

```bash
git clone <url-du-depot> nexa-formation
cd nexa-formation
```

### 3. Demarrer les services

Cette commande lance SonarQube (analyse statique) et PostgreSQL (base de tests) :

```bash
docker compose -f docker/docker-compose.yml up -d sonarqube postgres-sonar postgres-app
```

> Le premier demarrage peut prendre 2 a 3 minutes (telechargement des images).

### 4. Initialiser SonarQube

```bash
bash scripts/init-sonarqube.sh
```

Ce script configure le projet SonarQube automatiquement.

### 5. Construire le conteneur de compilation

```bash
docker compose -f docker/docker-compose.yml build lab-runner
```

L'image `lab-runner` contient Java 17 et Maven 3.9. Tous les tests seront executes dans ce conteneur.

### 6. Verifier avec un premier lab

Lancez les tests du Module 1 (fondamentaux JUnit) :

```bash
bash scripts/run-lab.sh labs/lab01-fondamentaux clean test
```

Vous devriez voir un resultat comme :

```
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### 7. Lancer tous les tests

```bash
bash scripts/run-all-tests.sh
```

Cette commande execute les 8 labs en sequence.

---

## Structure du projet

```
nexa-cda-test-unitaire/
├── cours/                              # Les 8 modules pedagogiques
│   ├── README.md
│   ├── module01-junit-fondamentaux.md   # Jour 1 -- Matin
│   ├── module02-tests-parametres.md     # Jour 1 -- Matin
│   ├── module03-mocking-mockito.md      # Jour 1 -- Apres-midi
│   ├── module04-tdd-bancaire.md         # Jour 1 -- Apres-midi
│   ├── module05-owasp-java.md           # Jour 2 -- Matin
│   ├── module06-spring-boot-tests.md    # Jour 2 -- Matin
│   ├── module07-spring-security.md      # Jour 2 -- Apres-midi
│   └── module08-projet-final.md         # Jour 2 -- Apres-midi
│
├── labs/                               # Code source des 8 travaux pratiques
│   ├── lab01-fondamentaux/             # JUnit 5 de base
│   ├── lab02-parametres/               # Tests parametres
│   ├── lab03-mocking/                  # Mockito
│   ├── lab04-tdd-banque/               # TDD bancaire
│   ├── lab05-owasp-java/               # Vulnerabilites OWASP
│   ├── lab06-spring-intro/             # Spring Boot + tests
│   ├── lab07-spring-security/          # Spring Security + JWT
│   └── lab08-user-manager/             # Projet final
│
├── docker/                             # Configuration Docker
├── scripts/                            # Scripts utilitaires
├── .github/workflows/ci.yml            # Pipeline CI/CD
└── syllabus.md                         # Syllabus de la formation
```

---

## Parcours de la formation

| Module | Fichier | Jour | Duree | Theme |
|---|---|---|---|---|
| M01 | `module01-junit-fondamentaux.md` | J1 | 1h30 | Fondamentaux JUnit 5 |
| M02 | `module02-tests-parametres.md` | J1 | 1h45 | Tests parametres |
| M03 | `module03-mocking-mockito.md` | J1 | 1h30 | Mocking avec Mockito |
| M04 | `module04-tdd-bancaire.md` | J1 | 1h45 | TDD Application Bancaire |
| M05 | `module05-owasp-java.md` | J2 | 1h30 | Vulnerabilites OWASP |
| M06 | `module06-spring-boot-tests.md` | J2 | 1h45 | Spring Boot + Tests |
| M07 | `module07-spring-security.md` | J2 | 1h30 | Spring Security + JWT |
| M08 | `module08-projet-final.md` | J2 | 1h45 | Projet Final Gestion Utilisateurs |

Chaque module suit la structure : **Theorie -> Pratique pas a pas -> Lab -> Fiche memo**.
Le code dans `labs/` est strictement identique a celui presente dans les cours.

---

## Comment utiliser ce depot

1. **Lisez le cours** dans `cours/moduleXX-...md` pour comprendre les concepts
2. **Ouvrez le code** dans `labs/labXX/` pour voir l'implementation reelle
3. **Executez les tests** avec `bash scripts/run-lab.sh labs/labXX clean test`
4. **Faites le lab** : chaque module contient un enonce avec des objectifs
5. **Consultez la fiche memo** a la fin de chaque module pour reviser

---

## Depannage

| Probleme | Solution |
|---|---|
| `docker: command not found` | Docker Desktop n'est pas installe ou pas demarre |
| `Port 9000 already in use` | Un autre service utilise le port SonarQube. Modifiez `docker-compose.yml` |
| Tests lents au premier lancement | Maven telecharge les dependances, c'est normal |
| `BUILD FAILURE` | Verifiez que `docker compose build lab-runner` a reussi |

---

## Pour aller plus loin

La pipeline CI/CD (`.github/workflows/ci.yml`) execute automatiquement :

1. Analyse SonarQube (qualite du code)
2. Tests paralleles des 8 labs
3. Tests de mutation (PITest)
4. Scan de vulnerabilites (OWASP Dependency Check)
5. Build Docker
6. Scan de securite ZAP

Pour configurer cette pipeline sur votre depot GitHub, definissez les secrets suivants :
`SONAR_HOST_URL`, `SONAR_TOKEN`, `DOCKER_REGISTRY`, `DOCKER_USERNAME`, `DOCKER_PASSWORD`.
