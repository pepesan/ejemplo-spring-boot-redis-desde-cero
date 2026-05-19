#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "Starting Redis..."
docker compose -f "$SCRIPT_DIR/compose.yaml" up -d --wait

echo "Redis ready on localhost:6379"
