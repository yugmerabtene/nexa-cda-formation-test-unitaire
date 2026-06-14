# NEXA - Formation Tests Unitaires et Sécurité Java

**Formation intensive 2 jours — 14 heures**

---

## Pour le Formateur

### Préparation de l'environnement

```bash
git clone <url-depot-prive> nexa-formation
cd nexa-formation

docker compose -f docker/docker-compose.yml up -d sonarqube postgres-sonar postgres-app

bash scripts/init-sonarqube.sh

docker compose -f docker/docker-compose.yml build lab-runner

# Lancer un lab
bash scripts/run-lab.sh labs/lab01-fondamentaux clean test

# Lancer tous les tests
bash scripts/run-all-tests.sh
```

### Structure du projet

```
nexa-cda-test-unitaire/
├── cours/                              # Les 8 modules pédagogiques
│   ├── README.md
│   ├── module01-junit-fondamentaux.md   # Jour 1 — Matin
│   ├── module02-tests-parametres.md     # Jour 1 — Matin
│   ├── module03-mocking-mockito.md      # Jour 1 — Après-midi
│   ├── module04-tdd-bancaire.md         # Jour 1 — Après-midi
│   ├── module05-owasp-java.md           # Jour 2 — Matin
│   ├── module06-spring-boot-tests.md    # Jour 2 — Matin
│   ├── module07-spring-security.md      # Jour 2 — Après-midi
│   └── module08-projet-final.md         # Jour 2 — Après-midi
│
├── labs/                               # Code source des 8 travaux pratiques
│   ├── lab01-fondamentaux/             # JUnit 5 de base
│   ├── lab02-parametres/               # Tests paramétrés
│   ├── lab03-mocking/                   # Mockito
│   ├── lab04-tdd-banque/               # TDD bancaire
│   ├── lab05-owasp-java/               # Vulnérabilités OWASP
│   ├── lab06-spring-intro/             # Spring Boot + tests
│   ├── lab07-spring-security/          # Spring Security + JWT
│   └── lab08-user-manager/             # Projet final
│
├── docker/                             # Dockerfiles et Compose
├── scripts/                            # Scripts utilitaires
├── .github/workflows/ci.yml            # Pipeline CI/CD
└── syllabus.md                         # Syllabus de la formation
```

### Modules de cours

| Module | Fichier | Durée | Thème |
|---|---|---|---|
| M01 | `module01-junit-fondamentaux.md` | 1h30 | Fondamentaux JUnit 5 |
| M02 | `module02-tests-parametres.md` | 1h45 | Tests paramétrés |
| M03 | `module03-mocking-mockito.md` | 1h30 | Mocking avec Mockito |
| M04 | `module04-tdd-bancaire.md` | 1h45 | TDD Application Bancaire |
| M05 | `module05-owasp-java.md` | 1h30 | Vulnérabilités OWASP |
| M06 | `module06-spring-boot-tests.md` | 1h45 | Spring Boot + Tests |
| M07 | `module07-spring-security.md` | 1h30 | Spring Security + JWT |
| M08 | `module08-projet-final.md` | 1h45 | Projet Final Gestion Utilisateurs |

Chaque module suit la structure : **Théorie → Pratique pas à pas → Lab → Fiche mémo**.
Le code dans `labs/` est strictement identique à celui présenté dans les cours.

### Pipeline CI/CD

Le fichier `.github/workflows/ci.yml` définit un pipeline multi-étapes :
1. SonarQube Analysis
2. Tests Jour 1 (4 jobs parallèles)
3. Tests Jour 2 (3 jobs parallèles)
4. Tests Lab08 (unitaires + intégration + mutation)
5. OWASP Dependency Check
6. Build Docker
7. ZAP Security Scan

### Configuration des secrets GitHub

| Secret | Description |
|---|---|
| `SONAR_HOST_URL` | URL de l'instance SonarQube |
| `SONAR_TOKEN` | Token d'authentification SonarQube |
| `DOCKER_REGISTRY` | URL du registre Docker |
| `DOCKER_USERNAME` | Nom d'utilisateur Docker |
| `DOCKER_PASSWORD` | Mot de passe Docker |
