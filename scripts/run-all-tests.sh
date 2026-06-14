#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "════════════════════════════════════════════════════════════════"
echo "  NEXA - Exécution de TOUS les tests"
echo "════════════════════════════════════════════════════════════════"
echo ""

LABS=(
    "jour1-tests-unitaires/lab01-fondamentaux"
    "jour1-tests-unitaires/lab02-parametres-avances"
    "jour1-tests-unitaires/lab03-mocking"
    "jour1-tests-unitaires/lab04-tdd-banque"
    "jour2-securite/lab05-owasp-java"
    "jour2-securite/lab06-intro-spring"
    "jour2-securite/lab07-spring-security"
    "lab08-app-gestion-utilisateurs"
)

PASSED=0
FAILED=0
FAILED_LABS=()

for lab in "${LABS[@]}"; do
    echo "────────────────────────────────────────────────────────────"
    echo "  Exécution : $lab"
    echo "────────────────────────────────────────────────────────────"

    if "$SCRIPT_DIR/run-lab.sh" "$lab" clean test; then
        echo "  ✅ PASSÉ"
        PASSED=$((PASSED + 1))
    else
        echo "  ❌ ÉCHOUÉ"
        FAILED=$((FAILED + 1))
        FAILED_LABS+=("$lab")
    fi

    echo ""
done

echo "════════════════════════════════════════════════════════════════"
echo "  RÉCAPITULATIF"
echo "════════════════════════════════════════════════════════════════"
echo ""
echo "  ✅ Passés : $PASSED"
echo "  ❌ Échoués : $FAILED"
echo ""

if [ $FAILED -gt 0 ]; then
    echo "  Labs en échec :"
    for lab in "${FAILED_LABS[@]}"; do
        echo "    - $lab"
    done
    echo ""
    exit 1
fi

echo "  🎉 Tous les tests sont passés !"
echo ""
