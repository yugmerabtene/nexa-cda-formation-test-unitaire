#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "════════════════════════════════════════════════════════════════"
echo "  NEXA - Exécution de TOUS les tests"
echo "════════════════════════════════════════════════════════════════"
echo ""

LABS=(
    "labs/lab01-fondamentaux"
    "labs/lab02-parametres"
    "labs/lab03-mocking"
    "labs/lab04-tdd-banque"
    "labs/lab05-owasp-java"
    "labs/lab06-spring-intro"
    "labs/lab07-spring-security"
    "labs/lab08-user-manager"
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
