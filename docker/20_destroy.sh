#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "Stopping Redis and removing containers and network..."
docker compose -f "$SCRIPT_DIR/compose.yaml" down

echo "Removing data directory..."
rm -rf "$SCRIPT_DIR/data"

echo "Done."
