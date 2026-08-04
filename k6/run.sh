#!/usr/bin/env bash

set -euo pipefail

K6_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCENARIO_PATH="${1:-}"

if [[ -z "${SCENARIO_PATH}" ]]; then
  echo "Usage: ./k6/run.sh <scenario-path> [k6 run options]" >&2
  echo "Example: ./k6/run.sh scenarios/examples/authenticated-read.js" >&2
  exit 1
fi

if [[ ! -f "${K6_DIR}/${SCENARIO_PATH}" ]]; then
  echo "Scenario not found: ${K6_DIR}/${SCENARIO_PATH}" >&2
  exit 1
fi

shift

SCENARIO_NAME="$(basename "${SCENARIO_PATH}" .js)"
if [[ -f "${K6_DIR}/.env" ]]; then
  if [[ -z "${TEST_ID:-}" ]]; then
    TEST_ID_LINE="$(grep -E '^TEST_ID=' "${K6_DIR}/.env" | tail -n 1 || true)"
    TEST_ID="${TEST_ID_LINE#TEST_ID=}"
  fi

  if [[ -z "${K6_OUTPUT:-}" ]]; then
    K6_OUTPUT_LINE="$(grep -E '^K6_OUTPUT=' "${K6_DIR}/.env" | tail -n 1 || true)"
    K6_OUTPUT="${K6_OUTPUT_LINE#K6_OUTPUT=}"
  fi
fi
TEST_ID="${TEST_ID:-${SCENARIO_NAME}-$(date +%Y%m%d-%H%M%S)}"
K6_OUTPUT="${K6_OUTPUT:-local}"
K6_ARGS=(run)

case "${K6_OUTPUT}" in
  local)
    ;;
  prometheus)
    K6_ARGS+=(-o experimental-prometheus-rw)
    ;;
  *)
    echo "Unsupported K6_OUTPUT: ${K6_OUTPUT} (local | prometheus)" >&2
    exit 1
    ;;
esac

echo "k6 scenario : ${SCENARIO_PATH}"
echo "k6 test id  : ${TEST_ID}"
echo "k6 output   : ${K6_OUTPUT}"

K6_ARGS+=(--tag "testid=${TEST_ID}")
K6_ARGS+=("$@")
K6_ARGS+=("/scripts/${SCENARIO_PATH}")

docker compose --project-directory "${K6_DIR}" -f "${K6_DIR}/compose.yaml" run --rm \
  -e TEST_ID="${TEST_ID}" \
  k6 "${K6_ARGS[@]}"
