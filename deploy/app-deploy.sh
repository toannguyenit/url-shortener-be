#!/bin/bash
# Deploy application services (microservices + frontend + nginx)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

if ! docker network inspect urlshortener-net >/dev/null 2>&1; then
  echo "ERROR: Network urlshortener-net not found. Run ./infra-up.sh first."
  exit 1
fi

COMPOSE="docker compose --env-file .env -f app/docker-compose.yml"

echo "==> Pulling latest app images..."
$COMPOSE pull \
  auth-service url-service redirect-service analytics-service api-gateway frontend

echo "==> Starting application services..."
$COMPOSE up -d --remove-orphans

echo "==> Pruning old images..."
docker image prune -f

echo "==> App deploy complete!"
$COMPOSE ps
