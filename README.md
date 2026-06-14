# NEXA - Formation Tests Unitaires et Sécurité Java

**Formation intensive 2 jours — 14 heures**

---

## Pour le Formateur

### Préparation de l'environnement

```bash
# 1. Cloner le dépôt
git clone <url-depot-prive> nexa-formation
cd nexa-formation

# 2. Démarrer les services (SonarQube, PostgreSQL)
docker compose -f docker/docker-compose.yml up -d sonarqube postgres-sonar

# 3. Initialiser SonarQube
bash scripts/init-sonarqube.sh

# 4. Construire l'image du runner de lab
docker compose -f docker/docker-compose.yml build lab-runner

# 5. Lancer un lab
bash scripts/run-lab.sh jour1-tests-unitaires/lab01-fondamentaux clean test

# 6. Lancer tous les tests
bash scripts/run-all-tests.sh
```

### Structure du projet

```
nexa-cda-test-unitaire/
├── .github/workflows/ci.yml         # Pipeline CI/CD complet
├── docker/                           # Images Docker et Compose
├── scripts/                          # Scripts utilitaires
├── syllabus.md                       # Syllabus détaillé
├── jour1-tests-unitaires/            # Labs Jour 1
│   ├── lab01-fondamentaux/           # JUnit 5 de base
│   ├── lab02-parametres-avances/     # Tests paramétrés
│   ├── lab03-mocking/                # Mockito
│   └── lab04-tdd-banque/             # TDD bancaire
├── jour2-securite/                   # Labs Jour 2
│   ├── lab05-owasp-java/             # Vulnérabilités OWASP
│   ├── lab06-intro-spring/           # Spring Boot + tests
│   └── lab07-spring-security/        # Spring Security + JWT
└── lab08-app-gestion-utilisateurs/   # Projet final
```

### Labs disponibles

| Lab | Durée | Thème | Tests |
|---|---|---|---|
| lab01 | 1h30 | Fondamentaux JUnit 5 | Assertions, exceptions, cycle de vie |
| lab02 | 1h45 | Tests paramétrés | CSV, Enum, Method sources |
| lab03 | 1h30 | Mocking Mockito | Mocks, stubs, verify, spy |
| lab04 | 1h45 | TDD Bancaire | Red-Green-Refactor, couverture |
| lab05 | 1h30 | OWASP Java | SQLi, XSS, Path Traversal |
| lab06 | 1h45 | Spring Boot tests | WebMvcTest, DataJpaTest |
| lab07 | 1h30 | Spring Security JWT | Auth, rôles, JWT |
| lab08 | 1h45 | Projet final | 110+ tests, CI/CD complet |

### Pipeline CI/CD

Le fichier `.github/workflows/ci.yml` définit un pipeline multi-étapes :
1. **SonarQube Analysis** — Analyse statique de tout le code
2. **Tests Jour 1** — 4 jobs parallèles (lab01 à lab04)
3. **Tests Jour 2** — 3 jobs parallèles (lab05 à lab07)
4. **Tests Lab08** — Tests unitaires + intégration + mutation
5. **OWASP Dependency Check** — Scan des CVE sur les dépendances
6. **Build Docker** — Construction et push de l'image
7. **ZAP Security Scan** — Scan de sécurité automatisé

### Configuration des secrets GitHub

Pour utiliser le pipeline CI/CD, configurer les secrets suivants dans le dépôt GitHub :

| Secret | Description |
|---|---|
| `SONAR_HOST_URL` | URL de l'instance SonarQube |
| `SONAR_TOKEN` | Token d'authentification SonarQube |
| `DOCKER_REGISTRY` | URL du registre Docker |
| `DOCKER_USERNAME` | Nom d'utilisateur Docker |
| `DOCKER_PASSWORD` | Mot de passe Docker |
