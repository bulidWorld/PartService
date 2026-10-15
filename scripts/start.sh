#!/usr/bin/env bash
set -Eeuo pipefail
BASE_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
PID_FILE="${PID_FILE:-${BASE_DIR}/run/part-service.pid}"
LOG_DIR="${LOG_DIR:-/var/service/log/part-service}"
mkdir -p "${LOG_DIR}"
mkdir -p "$(dirname -- "${PID_FILE}")"
if [[ -f "${PID_FILE}" ]] && kill -0 "$(cat "${PID_FILE}")" 2>/dev/null; then echo "part-service is already running"; exit 0; fi
cd "${BASE_DIR}"
# Schema is managed by sync_schema.py; older JARs still enable SQL init.
export SPRING_SQL_INIT_MODE="${SPRING_SQL_INIT_MODE:-never}"
nohup java ${JAVA_OPTS:-} -jar "${JAR_FILE:-${BASE_DIR}/target/part-service-0.0.1-SNAPSHOT.jar}" >>"${LOG_DIR}/console.log" 2>&1 &
echo $! >"${PID_FILE}"
echo "part-service started (pid $(cat "${PID_FILE}"))"
