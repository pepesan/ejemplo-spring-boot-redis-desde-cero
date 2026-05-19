#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

mkdir -p "$SCRIPT_DIR/data"
chmod 777 "$SCRIPT_DIR/data"

echo "Directory data/ created and ready."
