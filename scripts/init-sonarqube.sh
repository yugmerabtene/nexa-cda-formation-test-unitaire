#!/bin/bash
set -e

echo "════════════════════════════════════════════════════════════════"
echo "  Initialisation SonarQube pour NEXA Formation"
echo "════════════════════════════════════════════════════════════════"
echo ""

SONAR_URL="${SONAR_HOST_URL:-http://localhost:9000}"
SONAR_USER="${SONAR_USER:-admin}"
SONAR_PASSWORD="${SONAR_PASSWORD:-admin}"
NEW_PASSWORD="nexa2024!"

echo "Attente du démarrage de SonarQube..."
until curl -s -o /dev/null -w "%{http_code}" "$SONAR_URL/api/system/health" | grep -q "200\|503"; do
    sleep 5
    echo "  En attente..."
done

echo "SonarQube est prêt."

if [ "$SONAR_PASSWORD" = "admin" ]; then
    echo "Changement du mot de passe admin..."
    curl -s -u "$SONAR_USER:$SONAR_PASSWORD" \
        -X POST "$SONAR_URL/api/users/change_password" \
        -d "login=admin&password=$NEW_PASSWORD&previousPassword=admin"
    SONAR_PASSWORD="$NEW_PASSWORD"
    echo "  Mot de passe changé."
fi

echo "Création du token d'analyse..."
TOKEN_RESPONSE=$(curl -s -u "$SONAR_USER:$SONAR_PASSWORD" \
    -X POST "$SONAR_URL/api/user_tokens/generate" \
    -d "name=nexa-cda-token")

SONAR_TOKEN=$(echo "$TOKEN_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

echo ""
echo "════════════════════════════════════════════════════════════════"
echo "  Configuration terminée"
echo "════════════════════════════════════════════════════════════════"
echo ""
echo "  URL SonarQube  : $SONAR_URL"
echo "  Utilisateur    : admin"
echo "  Mot de passe   : $SONAR_PASSWORD"
echo "  Token          : $SONAR_TOKEN"
echo ""
echo "  Pour configurer le token dans votre CI :"
echo "    export SONAR_TOKEN=$SONAR_TOKEN"
echo "    export SONAR_HOST_URL=$SONAR_URL"
echo ""
