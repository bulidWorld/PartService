#!/usr/bin/env bash
set -Eeuo pipefail
BASE_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
PID_FILE="${PID_FILE:-${BASE_DIR}/run/part-service.pid}"
if [[ ! -f "${PID_FILE}" ]]; then echo "part-service is not running"; exit 0; fi
pid="$(cat "${PID_FILE}")"
if kill -0 "$pid" 2>/dev/null; then kill "$pid"; for _ in {1..30}; do kill -0 "$pid" 2>/dev/null || break; sleep 1; done; kill -0 "$pid" 2>/dev/null && kill -9 "$pid" || true; fi
rm -f "${PID_FILE}"
echo "part-service stopped"
