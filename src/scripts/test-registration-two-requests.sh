#!/usr/bin/env bash
set -euo pipefail

# Testcase 1: two students register the same 1-seat workshop at the same time.
# Expected result: one request succeeds, the other fails as sold out or duplicate-safe conflict.
#
# Prerequisites:
# - docker compose stack is running from src/docker-compose.yml
# - service-sync has imported src/service-sync-data/student_latest.csv into Keycloak
# - Redis workshop slots are initialized from the seed data
#
# Usage:
#   ./src/scripts/test-registration-two-requests.sh
#
# Optional overrides:
#   API_BASE_URL=http://localhost/api \
#   KEYCLOAK_TOKEN_URL=http://localhost:8080/realms/unihub/protocol/openid-connect/token \
#   CLIENT_ID=unihub-client \
#   CSV_FILE=src/service-sync-data/student_latest.csv \
#   WORKSHOP_ID=11111111-1111-1111-1111-111111111111 \
#   ./src/scripts/test-registration-two-requests.sh

API_BASE_URL="${API_BASE_URL:-http://localhost/api}"
KEYCLOAK_TOKEN_URL="${KEYCLOAK_TOKEN_URL:-http://localhost:8080/realms/unihub/protocol/openid-connect/token}"
CLIENT_ID="${CLIENT_ID:-unihub-client}"
CSV_FILE="${CSV_FILE:-src/service-sync-data/student_latest.csv}"
WORKSHOP_ID="${WORKSHOP_ID:-11111111-1111-1111-1111-111111111111}"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

require_command curl
require_command awk
require_command python3

if [ ! -f "$CSV_FILE" ]; then
  echo "CSV file not found: $CSV_FILE" >&2
  exit 1
fi

read_csv_row() {
  local row_number="$1"
  awk -F',' -v row="$row_number" 'NR == row { gsub(/\r/, "", $4); print $1 "|" $4 }' "$CSV_FILE"
}

STUDENT_1="$(read_csv_row 2)"
STUDENT_2="$(read_csv_row 3)"

if [ -z "$STUDENT_1" ] || [ -z "$STUDENT_2" ]; then
  echo "CSV must contain at least two student rows." >&2
  exit 1
fi

STUDENT_1_MSSV="${STUDENT_1%%|*}"
STUDENT_1_PASSWORD="${STUDENT_1#*|}"
STUDENT_2_MSSV="${STUDENT_2%%|*}"
STUDENT_2_PASSWORD="${STUDENT_2#*|}"

uuid() {
  if command -v uuidgen >/dev/null 2>&1; then
    uuidgen | tr '[:upper:]' '[:lower:]'
  else
    python3 - <<'PY'
import uuid
print(uuid.uuid4())
PY
  fi
}

extract_access_token() {
  python3 -c 'import json,sys; print(json.load(sys.stdin)["access_token"])'
}

get_jwt() {
  local username="$1"
  local password="$2"
  local response

  response="$(curl -sS \
    -X POST "$KEYCLOAK_TOKEN_URL" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    --data-urlencode "grant_type=password" \
    --data-urlencode "client_id=$CLIENT_ID" \
    --data-urlencode "username=$username" \
    --data-urlencode "password=$password")"

  if ! printf '%s' "$response" | python3 -c 'import json,sys; data=json.load(sys.stdin); sys.exit(0 if "access_token" in data else 1)' >/dev/null 2>&1; then
    echo "Cannot get JWT for student $username." >&2
    echo "Keycloak response:" >&2
    echo "$response" >&2
    echo >&2
    echo "Check that service-sync imported this student and password is birthday from CSV: $password" >&2
    exit 1
  fi

  printf '%s' "$response" | extract_access_token
}

register_student() {
  local label="$1"
  local mssv="$2"
  local token="$3"
  local output_file="$4"
  local idempotency_key
  idempotency_key="$(uuid)"

  curl -sS \
    -X POST "$API_BASE_URL/registrations" \
    -H "Authorization: Bearer $token" \
    -H "Content-Type: application/json" \
    -d "{\"workshopId\":\"$WORKSHOP_ID\",\"idempotencyKey\":\"$idempotency_key\"}" \
    -w "\nHTTP_STATUS:%{http_code}\nREQUEST_LABEL:$label\nMSSV:$mssv\nIDEMPOTENCY_KEY:$idempotency_key\n" \
    > "$output_file"
}

echo "Using API_BASE_URL: $API_BASE_URL"
echo "Using Keycloak token URL: $KEYCLOAK_TOKEN_URL"
echo "Using workshop: $WORKSHOP_ID"
echo "Student 1: $STUDENT_1_MSSV"
echo "Student 2: $STUDENT_2_MSSV"
echo

echo "Getting JWT tokens..."
TOKEN_1="$(get_jwt "$STUDENT_1_MSSV" "$STUDENT_1_PASSWORD")"
TOKEN_2="$(get_jwt "$STUDENT_2_MSSV" "$STUDENT_2_PASSWORD")"

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

echo "Sending 2 registration requests at the same time..."
register_student "request-1" "$STUDENT_1_MSSV" "$TOKEN_1" "$TMP_DIR/request-1.out" &
PID_1=$!
register_student "request-2" "$STUDENT_2_MSSV" "$TOKEN_2" "$TMP_DIR/request-2.out" &
PID_2=$!

wait "$PID_1" || true
wait "$PID_2" || true

echo
echo "===== Result: request-1 ($STUDENT_1_MSSV) ====="
cat "$TMP_DIR/request-1.out"
echo
echo "===== Result: request-2 ($STUDENT_2_MSSV) ====="
cat "$TMP_DIR/request-2.out"
echo
echo "Expected for seeded workshop 111... with max_seats = 1:"
echo "- exactly one request should return success/PENDING/SUCCESS depending on workshop type"
echo "- the other request should fail with sold out or a safe registration error"
