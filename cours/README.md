# Cours — Formation Tests Unitaires et Sécurité Java

**École Nexa — 2 jours (14 heures)**

## Comment utiliser ces cours

Ces 8 modules de cours sont conçus pour être utilisés **en présentiel** par un formateur.

### Structure de chaque module

```
──────────────────────────────────────────
PARTIE 1 — THÉORIE
──────────────────────────────────────────
    • Explication des concepts, annotations, outils
    • Schémas textuels et tableaux récapitulatifs
    • Durée : 30-45 minutes

──────────────────────────────────────────
PARTIE 2 — PRATIQUE PAS À PAS
──────────────────────────────────────────
    • Code commenté ligne par ligne
    • Chaque annotation décortiquée dans son contexte
    • Les extraits de code proviennent des dossiers labs/
    • Durée : 40-60 minutes

──────────────────────────────────────────
PARTIE 3 — LAB
──────────────────────────────────────────
    • Énoncé de l'exercice
    • Objectifs et critères de réussite
    • Correction détaillée
    • Durée : 30-45 minutes

──────────────────────────────────────────
FICHE MÉMO
──────────────────────────────────────────
    • Tableau synthétique des annotations et méthodes vues
```

### Dossiers

| Dossier | Contenu |
|---|---|
| `cours/` | Ces 8 modules pédagogiques (Théorie + Pratique + Lab) |
| `labs/` | Le code source complet (sans commentaire, pur et fonctionnel) |
| `docker/` | Infrastructure Docker (SonarQube, PostgreSQL, runner Maven) |
| `scripts/` | Scripts pour lancer les labs |
| `.github/workflows/` | Pipeline CI/CD complet |

### Parcours de la formation

| Module | Fichier | Jour | Matin/Après-midi |
|---|---|---|---|
| M01 | `module01-junit-fondamentaux.md` | J1 | Matin |
| M02 | `module02-tests-parametres.md` | J1 | Matin |
| M03 | `module03-mocking-mockito.md` | J1 | Après-midi |
| M04 | `module04-tdd-bancaire.md` | J1 | Après-midi |
| M05 | `module05-owasp-java.md` | J2 | Matin |
| M06 | `module06-spring-boot-tests.md` | J2 | Matin |
| M07 | `module07-spring-security.md` | J2 | Après-midi |
| M08 | `module08-projet-final.md` | J2 | Après-midi |

### Lancer un lab

```bash
bash scripts/run-lab.sh labs/lab01-fondamentaux clean test
```

### Important

- Le code dans `labs/` est **sans commentaire** : propre, concis, fonctionnel.
- Les explications détaillées de chaque ligne de code sont dans les modules `cours/`.
- Chaque module référence explicitement les fichiers qu'il couvre avec `📁 labs/labXX/.../Fichier.java`.
