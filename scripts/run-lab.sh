#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

usage() {
    echo "Usage: $0 <lab-directory> [maven-args...]"
    echo ""
    echo "Exemples:"
    echo "  $0 labs/lab01-fondamentaux"
    echo "  $0 labs/lab08-user-manager clean test"
    echo "  $0 labs/lab05-owasp-java -DskipTests package"
    echo ""
    echo "Labs disponibles:"
    echo "  Jour 1:"
    echo "    labs/lab01-fondamentaux"
    echo "    labs/lab02-parametres"
    echo "    labs/lab03-mocking"
    echo "    labs/lab04-tdd-banque"
    echo ""
    echo "  Jour 2:"
    echo "    labs/lab05-owasp-java"
    echo "    labs/lab06-spring-intro"
    echo "    labs/lab07-spring-security"
    echo ""
    echo "  Projet Final:"
    echo "    labs/lab08-user-manager"
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
