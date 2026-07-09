#!/bin/bash
# Khởi động infrastructure (MongoDB, Redis, RabbitMQ) — chạy 1 lần hoặc khi cần restart infra
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "==> Starting URL Shortener INFRA (MongoDB, Redis, RabbitMQ)..."
docker compose --env-file .env -f infra/docker-compose.yml up -d

echo "==> Waiting for infra to be healthy..."
for i in {1..30}; do
  mongo_ok=$(docker inspect --format='{{.State.Health.Status}}' urlshortener-mongodb 2>/dev/null || echo "missing")
  redis_ok=$(docker inspect --format='{{.State.Health.Status}}' urlshortener-redis 2>/dev/null || echo "missing")
  rabbit_ok=$(docker inspect --format='{{.State.Health.Status}}' urlshortener-rabbitmq 2>/dev/null || echo "missing")
  if [[ "$mongo_ok" == "healthy" && "$redis_ok" == "healthy" && "$rabbit_ok" == "healthy" ]]; then
    echo "==> Infra is healthy!"
    docker compose --env-file .env -f infra/docker-compose.yml ps
    exit 0
  fi
  sleep 2
done

echo "WARNING: Infra health check timeout — check logs:"
echo "  docker compose -f infra/docker-compose.yml logs"
exit 1
