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

set -u
set -o pipefail

DATE=""
SSH_USER="${SSH_USER:-root}"
REQUESTED_HOSTS=()
SSH_OPTS="${SSH_OPTS:--o BatchMode=yes -o ConnectTimeout=10}"
SSH_KEY="${SSH_KEY:-}"
SSH_PASSWORD="${SSH_PASSWORD:-}"
HOST_PASSWORDS_FILE=""
WORKDIR_BASE=""

declare -A HOST_PASSWORDS=()

MANAGEMENT_LOG_DIR="/var/log/cloudstack/management"
MANAGEMENT_LOG="management-server.log"
AGENT_LOG_DIR="/var/log/cloudstack/agent"
AGENT_LOG="agent.log"
DB_PROPERTIES="/etc/cloudstack/management/db.properties"

DISCOVERY_METHOD="local"
WORKDIR=""
ARCHIVE=""

management_servers=()
kvm_hosts=()

usage() {
    cat <<EOF
Usage:
  $0 [--date YYYY-MM-DD] [--hosts HOST1,HOST2,...] [--workdir DIR]
     [--ssh-key FILE | --ssh-password PASSWORD | --host-passwords FILE]

Examples:
  $0
  $0 --date 2026-08-25
  $0 --hosts 10.0.0.21
  $0 --hosts 10.0.0.21,10.0.0.22,kvm03.example.com
  $0 --date 2026-08-25 --hosts 10.0.0.21,10.0.0.22
  $0 --workdir /data/cloudstack-logs
  $0 --ssh-key /root/.ssh/id_rsa
  $0 --ssh-password 'secret'
  $0 --host-passwords /root/host-passwords.txt

Environment variables:
  SSH_USER=root
  SSH_OPTS="-o BatchMode=yes -o ConnectTimeout=10"
  SSH_KEY
  SSH_PASSWORD

Behavior:
  KVM host:
    Collects the local CloudStack agent log.

  Management server:
    Discovers active management servers and KVM hosts using CloudMonkey.
    If CloudMonkey is unavailable or API discovery fails, falls back to
    the CloudStack database.

    Logs are pulled from other management servers and KVM hosts over
    SSH as \$SSH_USER, authenticating with (in order of precedence):
      1. A per-host password from --host-passwords, if the host appears
         in that file.
      2. A single password from --ssh-password or \$SSH_PASSWORD, applied
         to every remote host.
      3. A single private key from --ssh-key or \$SSH_KEY, applied to
         every remote host.
      4. Whatever SSH would otherwise use (agent, default identity
         files), if none of the above are given.

    Password-based authentication requires the "sshpass" utility.

  --host-passwords FILE:
    FILE contains one "host=password" pair per line (blank lines and
    lines starting with # are ignored). host must match the IP or
    hostname used to reach that server.

  Without --date:
    Collects the current active log files.

  With --date:
    Searches current and rotated logs and extracts entries for that date.

  With --hosts:
    On a management server, collects agent logs only from the specified
    comma-separated KVM host IPs or hostnames.

    On a KVM host, --hosts is ignored because only the local agent log is
    collected.

  Without --workdir:
    Collected logs and the resulting archive are placed in a directory
    under \${TMPDIR:-/tmp}.

  With --workdir:
    Collected logs and the resulting archive are placed in the specified
    directory instead, which must already exist.
EOF
}

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*"
}

die() {
    echo "ERROR: $*" >&2
    exit 1
}

# --- Arguments ---

while [[ $# -gt 0 ]]; do
    case "$1" in
        --date)
            [[ $# -ge 2 ]] || die "--date requires YYYY-MM-DD"
            DATE="$2"
            shift 2
            ;;
        --hosts)
            [[ $# -ge 2 ]] || die "--hosts requires comma-separated IPs or hostnames"

            IFS=',' read -ra hosts <<< "$2"
            for host in "${hosts[@]}"; do
                host="$(printf '%s' "$host" | xargs)"
                [[ -n "$host" ]] && REQUESTED_HOSTS+=("$host")
            done

            shift 2
            ;;
        --workdir)
            [[ $# -ge 2 ]] || die "--workdir requires a directory path"
            WORKDIR_BASE="$2"
            shift 2
            ;;
        --ssh-key)
            [[ $# -ge 2 ]] || die "--ssh-key requires a file path"
            SSH_KEY="$2"
            shift 2
            ;;
        --ssh-password)
            [[ $# -ge 2 ]] || die "--ssh-password requires a password"
            SSH_PASSWORD="$2"
            shift 2
            ;;
        --host-passwords)
            [[ $# -ge 2 ]] || die "--host-passwords requires a file path"
            HOST_PASSWORDS_FILE="$2"
            shift 2
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            die "Unknown argument: $1"
            ;;
    esac
done

if [[ -n "$DATE" ]]; then
    [[ "$DATE" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]] || die "Invalid date: $DATE"
    date -d "$DATE" '+%Y-%m-%d' >/dev/null 2>&1 || die "Invalid date: $DATE"
    DATE="$(date -d "$DATE" '+%Y-%m-%d')"
    SUFFIX="$DATE"
else
    SUFFIX="$(date '+%Y%m%d-%H%M%S')"
fi

WORKDIR_BASE="${WORKDIR_BASE:-${TMPDIR:-/tmp}}"
[[ -d "$WORKDIR_BASE" ]] || die "Working directory does not exist: $WORKDIR_BASE"

if [[ -n "$SSH_KEY" ]]; then
    [[ -f "$SSH_KEY" ]] || die "SSH key not found: $SSH_KEY"
fi

if [[ -n "$HOST_PASSWORDS_FILE" ]]; then
    [[ -r "$HOST_PASSWORDS_FILE" ]] || die "Cannot read host password file: $HOST_PASSWORDS_FILE"

    while IFS='=' read -r host password; do
        host="$(printf '%s' "$host" | xargs)"
        [[ -z "$host" || "$host" == \#* ]] && continue
        HOST_PASSWORDS["$host"]="$password"
    done < "$HOST_PASSWORDS_FILE"
fi

if [[ -n "$SSH_PASSWORD" || ${#HOST_PASSWORDS[@]} -gt 0 ]]; then
    command -v sshpass >/dev/null 2>&1 ||
        die "sshpass is required for password-based SSH authentication"
fi

WORKDIR="${WORKDIR_BASE%/}/cloudstack-logs-${SUFFIX}"
ARCHIVE="${WORKDIR}.tar.gz"

rm -rf "$WORKDIR"
mkdir -p "$WORKDIR"

# --- Detection ---

detect_node_type() {
    if [[ -f "${MANAGEMENT_LOG_DIR}/${MANAGEMENT_LOG}" ]]; then
        echo "management"
    elif [[ -f "${AGENT_LOG_DIR}/${AGENT_LOG}" ]]; then
        echo "kvm"
    else
        echo "unknown"
    fi
}

is_local_address() {
    local address="$1"

    ip -o addr show 2>/dev/null |
        awk '{print $4}' |
        cut -d/ -f1 |
        grep -Fxq "$address"
}

# --- Log extraction ---

filter_date() {
    local requested_date="$1"

    awk -v requested_date="$requested_date" '
        /^[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9][ T]/ {
            current_date = substr($0, 1, 10)
            print_line = (current_date == requested_date)
        }
        print_line {
            print
        }
    '
}

read_log_files() {
    local directory="$1"
    local basename="$2"

    find "$directory" \
        -maxdepth 1 \
        -type f \
        -name "${basename}*" \
        -print0 2>/dev/null |
    sort -z |
    while IFS= read -r -d '' file; do
        case "$file" in
            *.gz)
                gzip -cd "$file" 2>/dev/null || true
                ;;
            *)
                cat "$file" 2>/dev/null || true
                ;;
        esac
    done
}

collect_local_log() {
    local log_dir="$1"
    local log_name="$2"
    local output="$3"

    if [[ -z "$DATE" ]]; then
        cp "${log_dir}/${log_name}" "$output"
    else
        read_log_files "$log_dir" "$log_name" |
            filter_date "$DATE" > "$output"
    fi
}

host_password() {
    local host="$1"

    if [[ -n "${HOST_PASSWORDS[$host]:-}" ]]; then
        printf '%s' "${HOST_PASSWORDS[$host]}"
    else
        printf '%s' "$SSH_PASSWORD"
    fi
}

collect_remote_log() {
    local host="$1"
    local log_dir="$2"
    local log_name="$3"
    local output="$4"
    local password
    local passfile=""
    local ssh_cmd=(ssh)
    local ssh_opts="$SSH_OPTS"
    local rc

    password="$(host_password "$host")"

    if [[ -n "$password" ]]; then
        passfile="$(mktemp)" || return 1
        chmod 600 "$passfile"
        printf '%s' "$password" > "$passfile"
        ssh_cmd=(sshpass -f "$passfile" ssh)
        # BatchMode disables password/keyboard-interactive auth outright; an
        # earlier -o wins over a later one for the same key, so this has to
        # come before $SSH_OPTS to override any BatchMode=yes already in it.
        ssh_opts="-o BatchMode=no $ssh_opts"
    elif [[ -n "$SSH_KEY" ]]; then
        ssh_cmd=(ssh -i "$SSH_KEY")
    fi

    if [[ -z "$DATE" ]]; then
        "${ssh_cmd[@]}" ${ssh_opts} "${SSH_USER}@${host}" \
            "cat '${log_dir}/${log_name}'" > "$output"
        rc=$?
        [[ -n "$passfile" ]] && rm -f "$passfile"
        return $rc
    fi

    "${ssh_cmd[@]}" ${ssh_opts} "${SSH_USER}@${host}" \
        bash -s -- "$log_dir" "$log_name" "$DATE" > "$output" <<'REMOTE'
LOG_DIR="$1"
LOG_NAME="$2"
REQUESTED_DATE="$3"

find "$LOG_DIR" \
    -maxdepth 1 \
    -type f \
    -name "${LOG_NAME}*" \
    -print0 2>/dev/null |
sort -z |
while IFS= read -r -d '' file; do
    case "$file" in
        *.gz)
            gzip -cd "$file" 2>/dev/null || true
            ;;
        *)
            cat "$file" 2>/dev/null || true
            ;;
    esac
done |
awk -v requested_date="$REQUESTED_DATE" '
    /^[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9][ T]/ {
        current_date = substr($0, 1, 10)
        print_line = (current_date == requested_date)
    }
    print_line {
        print
    }
'
REMOTE
    rc=$?
    [[ -n "$passfile" ]] && rm -f "$passfile"
    return $rc
}

# --- CloudMonkey discovery ---

find_cloudmonkey() {
    if command -v cmk >/dev/null 2>&1; then
        echo "cmk"
    elif command -v cloudmonkey >/dev/null 2>&1; then
        echo "cloudmonkey"
    else
        return 1
    fi
}

cloudmonkey_json() {
    local command="$1"
    shift

    if [[ "$command" == "cmk" ]]; then
        "$command" -o json "$@"
    else
        "$command" -d json "$@"
    fi
}

discover_with_cloudmonkey() {
    local cmk_cmd
    local ms_output
    local host_output

    cmk_cmd="$(find_cloudmonkey)" || return 1

    if ! command -v jq >/dev/null 2>&1; then
        log "CloudMonkey found, but jq is unavailable"
        return 1
    fi

    log "Trying discovery using $cmk_cmd"

    if ! ms_output="$(
        cloudmonkey_json "$cmk_cmd" list managementservers 2>/dev/null
    )"; then
        log "CloudMonkey API call list managementservers failed"
        return 1
    fi

    if ! host_output="$(
        cloudmonkey_json "$cmk_cmd" list hosts \
            type=Routing hypervisor=KVM state=Up 2>/dev/null
    )"; then
        log "CloudMonkey API call list hosts failed"
        return 1
    fi

    mapfile -t management_servers < <(
        printf '%s\n' "$ms_output" |
            jq -r '
                (.managementserver // .managementservers // [])[]
                | select((.state // "Up") == "Up")
                | .serviceip // empty
            ' 2>/dev/null
    )

    mapfile -t kvm_hosts < <(
        printf '%s\n' "$host_output" |
            jq -r '
                (.host // .hosts // [])[]
                | select((.state // "Up") == "Up")
                | .ipaddress // empty
            ' 2>/dev/null
    )

    if [[ ${#management_servers[@]} -eq 0 ]]; then
        log "CloudMonkey returned no active management servers"
        return 1
    fi

    DISCOVERY_METHOD="cloudmonkey"
    return 0
}

# --- Database discovery ---

get_property() {
    local property="$1"

    awk -v property="$property" '
        {
            line = $0
            sub(/^[ \t]+/, "", line)
            sub(/[ \t]+$/, "", line)

            if (line == "" || line ~ /^[#!]/) next

            eq = index(line, "=")
            if (eq == 0) next

            key = substr(line, 1, eq - 1)
            sub(/[ \t]+$/, "", key)

            if (key == property) {
                value = substr(line, eq + 1)
                sub(/^[ \t]+/, "", value)
                print value
                exit
            }
        }
    ' "$DB_PROPERTIES"
}

query_cloudstack_db() {
    local query="$1"
    local db_host
    local db_port
    local db_name
    local db_user
    local db_password
    local defaults_file
    local rc

    [[ -r "$DB_PROPERTIES" ]] || return 1
    command -v mysql >/dev/null 2>&1 || return 1

    db_host="$(get_property db.cloud.host)"
    db_port="$(get_property db.cloud.port)"
    db_name="$(get_property db.cloud.name)"
    db_user="$(get_property db.cloud.username)"
    db_password="$(get_property db.cloud.password)"

    [[ -n "$db_host" && -n "$db_user" ]] || return 1

    db_port="${db_port:-3306}"
    db_name="${db_name:-cloud}"

    defaults_file="$(mktemp)" || return 1
    chmod 600 "$defaults_file"

    {
        printf '[client]\n'
        printf 'host=%s\n' "$db_host"
        printf 'port=%s\n' "$db_port"
        printf 'user=%s\n' "$db_user"
        printf 'password=%s\n' "$db_password"
    } > "$defaults_file"

    mysql \
        --defaults-extra-file="$defaults_file" \
        --batch \
        --skip-column-names \
        "$db_name" \
        -e "$query"
    rc=$?

    rm -f "$defaults_file"
    return $rc
}

discover_with_database() {
    local ms_output
    local host_output

    log "Trying discovery using the CloudStack database"

    if ! ms_output="$(
        query_cloudstack_db "
            SELECT service_ip
            FROM mshost
            WHERE state = 'Up'
              AND service_ip IS NOT NULL;
        " 2>/dev/null
    )"; then
        log "Database query for management servers failed"
        return 1
    fi

    if ! host_output="$(
        query_cloudstack_db "
            SELECT private_ip_address
            FROM host
            WHERE removed IS NULL
              AND type = 'Routing'
              AND hypervisor_type = 'KVM'
              AND status = 'Up'
              AND private_ip_address IS NOT NULL;
        " 2>/dev/null
    )"; then
        log "Database query for KVM hosts failed"
        return 1
    fi

    mapfile -t management_servers <<< "$ms_output"
    mapfile -t kvm_hosts <<< "$host_output"

    if [[ ${#management_servers[@]} -eq 0 || -z "${management_servers[0]:-}" ]]; then
        log "Database returned no active management servers"
        return 1
    fi

    DISCOVERY_METHOD="database"
    return 0
}

discover_cloudstack_servers() {
    management_servers=()
    kvm_hosts=()

    if discover_with_cloudmonkey; then
        return 0
    fi

    log "CloudMonkey discovery unavailable or failed; falling back to database"

    management_servers=()
    kvm_hosts=()

    discover_with_database
}

# --- Collection ---

collect_from_kvm_host() {
    local host_name

    host_name="$(hostname -f 2>/dev/null || hostname)"
    log "Running on KVM host: $host_name"

    mkdir -p "$WORKDIR/kvm/$host_name"

    if ! collect_local_log \
        "$AGENT_LOG_DIR" \
        "$AGENT_LOG" \
        "$WORKDIR/kvm/$host_name/$AGENT_LOG"; then
        log "WARNING: Failed to collect local agent log"
        rm -f "$WORKDIR/kvm/$host_name/$AGENT_LOG"
    fi
}

collect_from_management_server() {
    log "Running on CloudStack management server"

    discover_cloudstack_servers ||
        die "Unable to discover CloudStack management servers and KVM hosts"

    if [[ ${#REQUESTED_HOSTS[@]} -gt 0 ]]; then
        kvm_hosts=("${REQUESTED_HOSTS[@]}")
        log "Using ${#kvm_hosts[@]} explicitly specified KVM host(s)"
    fi

    log "Discovery method: $DISCOVERY_METHOD"
    log "Management servers: ${#management_servers[@]}"
    log "KVM hosts: ${#kvm_hosts[@]}"

    for host in "${management_servers[@]}"; do
        [[ -n "$host" ]] || continue

        log "Collecting management log from $host"
        mkdir -p "$WORKDIR/management/$host"

        if is_local_address "$host"; then
            if ! collect_local_log \
                "$MANAGEMENT_LOG_DIR" \
                "$MANAGEMENT_LOG" \
                "$WORKDIR/management/$host/$MANAGEMENT_LOG"; then
                log "WARNING: Failed to collect local management log"
                rm -f "$WORKDIR/management/$host/$MANAGEMENT_LOG"
            fi
        elif ! collect_remote_log \
            "$host" \
            "$MANAGEMENT_LOG_DIR" \
            "$MANAGEMENT_LOG" \
            "$WORKDIR/management/$host/$MANAGEMENT_LOG"; then
            log "WARNING: Failed to retrieve management log from $host"
            rm -f "$WORKDIR/management/$host/$MANAGEMENT_LOG"
        fi
    done

    for host in "${kvm_hosts[@]}"; do
        [[ -n "$host" ]] || continue

        log "Collecting agent log from $host"
        mkdir -p "$WORKDIR/kvm/$host"

        if ! collect_remote_log \
            "$host" \
            "$AGENT_LOG_DIR" \
            "$AGENT_LOG" \
            "$WORKDIR/kvm/$host/$AGENT_LOG"; then
            log "WARNING: Failed to retrieve agent log from $host"
            rm -f "$WORKDIR/kvm/$host/$AGENT_LOG"
        fi
    done
}

# --- Main ---

NODE_TYPE="$(detect_node_type)"

case "$NODE_TYPE" in
    management)
        collect_from_management_server
        ;;
    kvm)
        collect_from_kvm_host
        ;;
    *)
        die "Unable to determine whether this is a CloudStack management server or KVM host"
        ;;
esac

find "$WORKDIR" -type f -empty -delete

cat > "$WORKDIR/info.txt" <<EOF
CloudStack Log Collection

Generated: $(date)
Collector: $(hostname -f 2>/dev/null || hostname)
Node type: $NODE_TYPE
Date filter: ${DATE:-current logs}
Discovery method: $DISCOVERY_METHOD
Requested KVM hosts: ${REQUESTED_HOSTS[*]:-all}
EOF

log "Creating archive $ARCHIVE"
if tar -czf "$ARCHIVE" -C "$WORKDIR_BASE" "$(basename "$WORKDIR")"; then
    rm -rf "$WORKDIR"
else
    die "Failed to create archive $ARCHIVE; collected logs remain in $WORKDIR"
fi

echo
echo "CloudStack logs collected:"
echo "  $ARCHIVE"
