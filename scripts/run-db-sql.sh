#!/usr/bin/env bash
set -Eeuo pipefail

# Backward-compatible entry point. Schema definitions now live in XML.
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
exec python3 "${SCRIPT_DIR}/sync_schema.py" "$@"
