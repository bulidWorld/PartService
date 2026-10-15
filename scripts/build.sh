#!/usr/bin/env bash
set -Eeuo pipefail

BASE_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${BASE_DIR}"

# Production packaging skips tests by default. Pass extra Maven options when needed.
mvn package -DskipTests "$@"

echo "Build completed: ${BASE_DIR}/target/part-service-0.0.1-SNAPSHOT.jar"
