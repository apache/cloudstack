#!/usr/bin/env bash
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
set -Eeuo pipefail

SCRIPT_NAME="$(basename "$0")"
DB_PROPS="/etc/cloudstack/management/db.properties"
KEY_FILE="/etc/cloudstack/management/key"
ENC_JAR="/usr/share/cloudstack-common/lib/cloudstack-utils.jar"

log() {
  echo "[INFO] $*"
}

fail() {
  echo "[ERROR] $*" >&2
  exit 1
}

on_error() {
  local exit_code=$?
  echo "[ERROR] Command failed at line $1 with exit code $exit_code" >&2
  exit "$exit_code"
}
trap 'on_error $LINENO' ERR

usage() {
  cat <<EOF
Usage:
  $SCRIPT_NAME view <config-key>
  $SCRIPT_NAME update <config-key> <value>

Examples:
  $SCRIPT_NAME view allow.operations.on.users.in.same.account
  $SCRIPT_NAME update allow.operations.on.users.in.same.account false
EOF
}

require_file() {
  [[ -f "$1" ]] || fail "$1 not found"
}

prop() {
  local key="$1"
  local value
  value="$(grep -E "^${key}=" "$DB_PROPS" | tail -n1 | cut -d= -f2- || true)"
  [[ -n "$value" ]] || fail "Property '$key' not found in $DB_PROPS"
  printf "%s" "$value"
}

extract_enc_value() {
  local key="$1"
  grep -oP "^${key}=ENC\\(\\K[^)]+" "$DB_PROPS" | tail -n1
}

sql_escape() {
  printf "%s" "$1" | sed "s/'/''/g"
}

decrypt_value() {
  local input="$1"
  local password="$2"
  java -classpath "$ENC_JAR" \
    com.cloud.utils.crypt.EncryptionCLI \
    -d \
    -i "$input" \
    -p "$password" \
    "$ENC_VERSION"
}

encrypt_value() {
  local input="$1"
  local password="$2"
  java -classpath "$ENC_JAR" \
    com.cloud.utils.crypt.EncryptionCLI \
    -i "$input" \
    -p "$password" \
    "$ENC_VERSION"
}

mysql_exec() {
  local query="$1"
  MYSQL_PWD="$DB_PASS" mysql \
    --batch \
    --raw \
    --skip-column-names \
    -h "$DB_HOST" \
    -P "$DB_PORT" \
    -u "$DB_USER" \
    "$DB_NAME" \
    -e "$query"
}

load_db_context() {
  require_file "$DB_PROPS"
  require_file "$KEY_FILE"
  require_file "$ENC_JAR"

  DB_USER="$(prop db.cloud.username)"
  DB_HOST="$(prop db.cloud.host)"
  DB_PORT="$(prop db.cloud.port)"
  DB_NAME="$(prop db.cloud.name)"
  ENC_VERSION="$(prop db.cloud.encryptor.version)"

  log "Loaded DB properties from $DB_PROPS"
  log "Connecting to database '$DB_NAME' on ${DB_HOST}:${DB_PORT} as user '$DB_USER'"
  log "Using encryptor version: $ENC_VERSION"

  local db_secret_enc
  local db_pass_enc
  local mgmt_secret

  db_secret_enc="$(extract_enc_value db.cloud.encrypt.secret)"
  [[ -n "$db_secret_enc" ]] || fail "Encrypted db.cloud.encrypt.secret not found in $DB_PROPS"

  mgmt_secret="$(tr -d '\r\n' < "$KEY_FILE")"

  log "Decrypting db.cloud.encrypt.secret"
  DB_SECRET="$(decrypt_value "$db_secret_enc" "$mgmt_secret")"
  [[ -n "$DB_SECRET" ]] || fail "Failed to decrypt db.cloud.encrypt.secret"

  if grep -q '^db.cloud.password=ENC(' "$DB_PROPS"; then
    db_pass_enc="$(extract_enc_value db.cloud.password)"
    [[ -n "$db_pass_enc" ]] || fail "Encrypted db.cloud.password not found in $DB_PROPS"
    log "Decrypting database password"
    DB_PASS="$(decrypt_value "$db_pass_enc" "$DB_SECRET")"
    [[ -n "$DB_PASS" ]] || fail "Failed to decrypt database password"
  else
    log "Using plain database password from db.properties"
    DB_PASS="$(prop db.cloud.password)"
  fi
}

view_config() {
  local config_name="$1"
  local row
  local category
  local value
  local default_value
  local display_value

  log "Reading config key: $config_name"

  row="$(mysql_exec "SELECT category, value, default_value FROM configuration WHERE name='$(sql_escape "$config_name")' LIMIT 1;")"
  [[ -n "$row" ]] || fail "Config key '$config_name' not found"

  category="$(printf '%s\n' "$row" | awk -F'\t' '{print $1}')"
  value="$(printf '%s\n' "$row" | awk -F'\t' '{print $2}')"
  default_value="$(printf '%s\n' "$row" | awk -F'\t' '{print $3}')"

  if [[ -z "$value" || "$value" == "NULL" ]]; then
    log "Config value is NULL, using default_value"
    display_value="$default_value"
  else
    display_value="$value"
    if [[ "$category" == "Hidden" || "$category" == "Secure" ]]; then
      log "Decrypting stored config value for display"
      display_value="$(decrypt_value "$value" "$DB_SECRET")"
    fi
  fi

  echo "Config key: $config_name"
  echo "Category: $category"
  echo "Stored value: ${value:-NULL}"
  echo "Default value: $default_value"
  echo "Display value: $display_value"
}

update_config() {
  local config_name="$1"
  local new_value="$2"
  local category
  local final_value
  local encrypted_value
  local updated_value

  log "Starting update for config key: $config_name"

  category="$(mysql_exec "SELECT category FROM configuration WHERE name='$(sql_escape "$config_name")' LIMIT 1;")"
  [[ -n "$category" ]] || fail "Config key '$config_name' not found"

  log "Config category is: $category"

  final_value="$new_value"
  if [[ "$category" == "Hidden" || "$category" == "Secure" ]]; then
    log "Encrypting new value before storing"
    encrypted_value="$(encrypt_value "$new_value" "$DB_SECRET")"
    [[ -n "$encrypted_value" ]] || fail "Encryption returned empty value"
    final_value="$encrypted_value"
  else
    log "Storing value without encryption"
  fi

  log "Updating configuration table"
  mysql_exec "UPDATE configuration SET value='$(sql_escape "$final_value")' WHERE name='$(sql_escape "$config_name")';"

  log "Verifying update"
  updated_value="$(mysql_exec "SELECT value FROM configuration WHERE name='$(sql_escape "$config_name")' LIMIT 1;")"
  [[ -n "$updated_value" ]] || fail "Verification failed after update"

  echo "Updated config key: $config_name"
  echo "Category: $category"
  if [[ "$category" == "Hidden" || "$category" == "Secure" ]]; then
    echo "Stored as: encrypted ciphertext"
  else
    echo "Stored as: plain"
  fi
  echo "Update successful"
}

main() {
  local action="${1:-}"

  if [[ "$action" == "-h" || "$action" == "--help" || -z "$action" ]]; then
    usage
    exit 0
  fi

  load_db_context

  case "$action" in
    view)
      [[ $# -eq 2 ]] || fail "Usage: $SCRIPT_NAME view <config-key>"
      view_config "$2"
      ;;
    update)
      [[ $# -eq 3 ]] || fail "Usage: $SCRIPT_NAME update <config-key> <value>"
      update_config "$2" "$3"
      ;;
    *)
      fail "Unknown action '$action'. Use 'view' or 'update'."
      ;;
  esac
}

main "$@"
