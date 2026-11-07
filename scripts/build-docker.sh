#!/usr/bin/env bash
set -Eeuo pipefail

BASE_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
REGISTRY="${REGISTRY:-192.168.100.133}"
# Docker repository names must be lowercase.
NAMESPACE="${NAMESPACE:-kunpeng}"
APP_NAME="part-service"
TAG="${1:-latest}"

if [[ ! "${NAMESPACE}" =~ ^[a-z0-9]+([._-][a-z0-9]+)*$ ]]; then
  echo "Invalid Docker namespace: ${NAMESPACE}" >&2
  exit 1
fi
if [[ ! "${TAG}" =~ ^[a-zA-Z0-9_][a-zA-Z0-9_.-]{0,127}$ ]]; then
  echo "Invalid Docker tag: ${TAG}" >&2
  exit 1
fi

IMAGE="${REGISTRY}/${NAMESPACE}/${APP_NAME}:${TAG}"

"${BASE_DIR}/scripts/build.sh"
docker build -t "${IMAGE}" "${BASE_DIR}"
docker push "${IMAGE}"
echo "Pushed ${IMAGE}"
