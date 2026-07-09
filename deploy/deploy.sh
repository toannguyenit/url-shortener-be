#!/bin/bash
# Full deploy: infra (if needed) + app
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Start infra if not running
if ! docker ps --format '{{.Names}}' | grep -q '^urlshortener-mongodb$'; then
  echo "==> Infra not running, starting..."
  ./infra-up.sh
else
  echo "==> Infra already running, skipping."
fi

./app-deploy.sh

echo ""
echo "==> Full deploy complete!"
echo "    Infra:  docker compose -f infra/docker-compose.yml ps"
echo "    App:    docker compose -f app/docker-compose.yml ps"
