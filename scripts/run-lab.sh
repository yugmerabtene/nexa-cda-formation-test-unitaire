#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

usage() {
    echo "Usage: $0 <lab-directory> [maven-args...]"
    echo ""
    echo "Exemples:"
    echo "  $0 jour1-tests-unitaires/lab01-fondamentaux"
    echo "  $0 lab08-app-gestion-utilisateurs clean test"
    echo "  $0 jour2-securite/lab05-owasp-java -DskipTests package"
    echo ""
    echo "Labs disponibles:"
    echo "  Jour 1:"
    echo "    jour1-tests-unitaires/lab01-fondamentaux"
    echo "    jour1-tests-unitaires/lab02-parametres-avances"
    echo "    jour1-tests-unitaires/lab03-mocking"
    echo "    jour1-tests-unitaires/lab04-tdd-banque"
    echo ""
    echo "  Jour 2:"
    echo "    jour2-securite/lab05-owasp-java"
    echo "    jour2-securite/lab06-intro-spring"
    echo "    jour2-securite/lab07-spring-security"
    echo ""
    echo "  Projet Final:"
    echo "    lab08-app-gestion-utilisateurs"
}

if [ $# -lt 1 ]; then
    usage
    exit 1
fi

LAB_DIR="$1"
shift
MAVEN_ARGS="$@"

LAB_PATH="$PROJECT_DIR/$LAB_DIR"

if [ ! -d "$LAB_PATH" ]; then
    echo "❌ ERREUR: Le dossier '$LAB_DIR' n'existe pas."
    usage
    exit 1
fi

if [ ! -f "$LAB_PATH/pom.xml" ]; then
    echo "❌ ERREUR: Aucun pom.xml trouvé dans '$LAB_DIR'."
    exit 1
fi

echo "════════════════════════════════════════════════════════════"
echo "  NEXA - Exécution du lab : $LAB_DIR"
echo "════════════════════════════════════════════════════════════"
echo ""

docker compose -f "$PROJECT_DIR/docker/docker-compose.yml" run --rm lab-runner \
    bash -c "cd /workspace/$LAB_DIR && mvn -B $MAVEN_ARGS"

EXIT_CODE=$?

echo ""
if [ $EXIT_CODE -eq 0 ]; then
    echo "✅ Lab '$LAB_DIR' exécuté avec succès."
else
    echo "❌ Lab '$LAB_DIR' a échoué (code $EXIT_CODE)."
fi

exit $EXIT_CODE
