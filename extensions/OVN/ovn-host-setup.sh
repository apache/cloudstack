#!/bin/bash
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
#
# ---------------------------------------------------------------------------
# Prepare hosts for the CloudStack OVN network extension.
#
# The extension programs the OVN northbound database and nothing else. It does
# not install OVN, does not configure a hypervisor, and does not edit
# agent.properties. Everything in this script is the part CloudStack will never
# do for you, collected in one place so it can be run the same way on every
# host.
#
# Roles:
#
#   management  ovn-nbctl and jq, so the extension script can run, plus the
#               log directory it writes to.
#   host        Open vSwitch and ovn-controller, pointed at the OVN southbound
#               database, with a stable chassis name. Also sets the CloudStack
#               agent's VIF driver, without which the Lswitch branch in
#               OvsVifDriver is never reached.
#   gateway     A host that may additionally carry north-south traffic: adds
#               the provider bridge and the ovn-bridge-mappings entry that
#               ovn_public_physnet refers to.
#
# Everything is idempotent. Run it again after a change and it converges.
#
# Usage:
#   ovn-host-setup.sh --role management --nb-db tcp:10.0.0.1:6641
#   ovn-host-setup.sh --role host    --sb-db tcp:10.0.0.1:6642 --encap-ip 10.1.0.11
#   ovn-host-setup.sh --role gateway --sb-db tcp:10.0.0.1:6642 --encap-ip 10.1.0.11 \
#                     --public-nic eth1 --physnet physnet1
#   ovn-host-setup.sh --verify
#   ovn-host-setup.sh --hosts hosts.txt --role host --sb-db tcp:10.0.0.1:6642
#
# --hosts fans the script out over ssh: it copies itself to each host listed
# (one per line, "name" or "name encap-ip") and runs it there with the same
# arguments. Without --hosts it configures the machine it is running on.
#
# This is a convenience, not a replacement for configuration management. If you
# run Ansible or Puppet, read this for the list of things that need doing and
# express them there instead.
# ---------------------------------------------------------------------------

set -u
set -o pipefail

ROLE=""
SB_DB=""
NB_DB=""
ENCAP_IP=""
ENCAP_TYPE="geneve"
PHYSNET="physnet1"
PUBLIC_NIC=""
BRIDGE="br-ex"
SYSTEM_ID=""
AGENT_PROPERTIES="/etc/cloudstack/agent/agent.properties"
EXTENSION_LOG_DIR="/var/log/cloudstack/management/extensions"
HOSTS_FILE=""
SSH_USER="root"
DRY_RUN=0
VERIFY_ONLY=0
FORCE=0
RESTART_AGENT=0

RED=""; GREEN=""; YELLOW=""; RESET=""
if [ -t 1 ]; then RED=$'\033[31m'; GREEN=$'\033[32m'; YELLOW=$'\033[33m'; RESET=$'\033[0m'; fi

say()  { printf '  %s\n' "$*"; }
ok()   { printf '  %sok%s    %s\n' "$GREEN" "$RESET" "$*"; }
warn() { printf '  %swarn%s  %s\n' "$YELLOW" "$RESET" "$*" >&2; }
die()  { printf '  %serror%s %s\n' "$RED" "$RESET" "$*" >&2; exit 1; }
head2() { printf '\n== %s ==\n' "$*"; }

# run <command...> -- echoes under --dry-run, executes otherwise.
run() {
    if [ "${DRY_RUN}" -eq 1 ]; then
        printf '  would run: %s\n' "$*"
        return 0
    fi
    "$@"
}

usage() { sed -n '19,60p' "$0" | sed 's/^# \{0,1\}//'; exit "${1:-0}"; }

while [ $# -gt 0 ]; do
    case "$1" in
        --role)          ROLE="${2:?}"; shift 2 ;;
        --sb-db)         SB_DB="${2:?}"; shift 2 ;;
        --nb-db)         NB_DB="${2:?}"; shift 2 ;;
        --encap-ip)      ENCAP_IP="${2:?}"; shift 2 ;;
        --encap-type)    ENCAP_TYPE="${2:?}"; shift 2 ;;
        --physnet)       PHYSNET="${2:?}"; shift 2 ;;
        --public-nic)    PUBLIC_NIC="${2:?}"; shift 2 ;;
        --bridge)        BRIDGE="${2:?}"; shift 2 ;;
        --system-id)     SYSTEM_ID="${2:?}"; shift 2 ;;
        --agent-properties) AGENT_PROPERTIES="${2:?}"; shift 2 ;;
        --hosts)         HOSTS_FILE="${2:?}"; shift 2 ;;
        --ssh-user)      SSH_USER="${2:?}"; shift 2 ;;
        --restart-agent) RESTART_AGENT=1; shift ;;
        --dry-run|-n)    DRY_RUN=1; shift ;;
        --verify)        VERIFY_ONLY=1; shift ;;
        --force)         FORCE=1; shift ;;
        -h|--help)       usage 0 ;;
        *)               die "unknown argument: $1 (try --help)" ;;
    esac
done

# ---------------------------------------------------------------------------
# ssh fan-out
# ---------------------------------------------------------------------------

if [ -n "${HOSTS_FILE}" ]; then
    [ -r "${HOSTS_FILE}" ] || die "cannot read host list ${HOSTS_FILE}"
    SELF="$(cd "$(dirname "$0")" && pwd)/$(basename "$0")"
    rc=0
    while read -r host hostip _; do
        case "${host}" in ''|\#*) continue ;; esac
        printf '\n######## %s ########\n' "${host}"
        args=(--role "${ROLE}")
        [ -n "${SB_DB}" ]      && args+=(--sb-db "${SB_DB}")
        [ -n "${NB_DB}" ]      && args+=(--nb-db "${NB_DB}")
        [ -n "${PUBLIC_NIC}" ] && args+=(--public-nic "${PUBLIC_NIC}")
        [ -n "${PHYSNET}" ]    && args+=(--physnet "${PHYSNET}")
        # Per-host encap IP from the second column, else the shared one, else
        # let the remote side work it out from its own route to the SB DB.
        if [ -n "${hostip}" ]; then args+=(--encap-ip "${hostip}")
        elif [ -n "${ENCAP_IP}" ]; then args+=(--encap-ip "${ENCAP_IP}")
        fi
        [ "${DRY_RUN}" -eq 1 ]       && args+=(--dry-run)
        [ "${VERIFY_ONLY}" -eq 1 ]   && args+=(--verify)
        [ "${FORCE}" -eq 1 ]         && args+=(--force)
        [ "${RESTART_AGENT}" -eq 1 ] && args+=(--restart-agent)
        if ! scp -q "${SELF}" "${SSH_USER}@${host}:/tmp/ovn-host-setup.sh"; then
            warn "${host}: could not copy the script"; rc=1; continue
        fi
        # shellcheck disable=SC2029
        if ! ssh "${SSH_USER}@${host}" "chmod +x /tmp/ovn-host-setup.sh && /tmp/ovn-host-setup.sh ${args[*]}"; then
            warn "${host}: setup failed"; rc=1
        fi
    done < "${HOSTS_FILE}"
    exit "${rc}"
fi

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

need_root() {
    [ "$(id -u)" -eq 0 ] || die "must run as root (or under sudo)"
}

pkg_manager() {
    for m in apt-get dnf yum zypper; do
        command -v "$m" >/dev/null 2>&1 && { printf '%s' "$m"; return 0; }
    done
    return 1
}

# install_packages <purpose> <pkg...> -- skips entirely if the commands that
# matter are already present, so this is safe on a host built by other means.
install_packages() {
    local purpose="$1"; shift
    local mgr
    if ! mgr="$(pkg_manager)"; then
        warn "no supported package manager found; install ${purpose} yourself: $*"
        return 0
    fi
    say "installing ${purpose}: $*"
    case "${mgr}" in
        apt-get) run env DEBIAN_FRONTEND=noninteractive apt-get install -y "$@" ;;
        dnf|yum) run "${mgr}" install -y "$@" ;;
        zypper)  run zypper --non-interactive install "$@" ;;
    esac
}

service_enable() {
    local svc="$1"
    if command -v systemctl >/dev/null 2>&1; then
        run systemctl enable --now "${svc}" || warn "could not enable ${svc}"
    else
        warn "no systemctl; start ${svc} yourself"
    fi
}

ovs_set() {   # ovs_set <key> <value> -- idempotent, reports only real changes
    local key="$1" value="$2" current
    current="$(ovs-vsctl --if-exists get open_vswitch . "external_ids:${key}" 2>/dev/null | tr -d '"')"
    if [ "${current}" = "${value}" ]; then
        ok "${key} already ${value}"
        return 0
    fi
    run ovs-vsctl set open_vswitch . "external_ids:${key}=${value}" \
        || die "failed to set ${key}"
    ok "${key} = ${value}${current:+  (was ${current})}"
}

# Pick the source address the kernel would use to reach the southbound DB, so
# the Geneve tunnels come up on the same path as the control connection.
derive_encap_ip() {
    local target="${SB_DB#*:}"; target="${target%:*}"
    case "${target}" in
        ''|*[!0-9.]*) return 1 ;;
    esac
    ip -4 route get "${target}" 2>/dev/null | sed -n 's/.* src \([0-9.]*\).*/\1/p' | head -1
}

# A chassis name must be stable across reboots: ovn-controller uses it to find
# its own Chassis row, and a new name on every boot leaks rows and strands
# port bindings.
resolve_system_id() {
    local existing
    existing="$(ovs-vsctl --if-exists get open_vswitch . external_ids:system-id 2>/dev/null | tr -d '"')"
    if [ -n "${SYSTEM_ID}" ]; then printf '%s' "${SYSTEM_ID}"; return; fi
    if [ -n "${existing}" ]; then printf '%s' "${existing}"; return; fi
    if [ -r /etc/openvswitch/system-id.conf ]; then
        cat /etc/openvswitch/system-id.conf; return
    fi
    hostname -s
}

# Set a key in a java .properties file, in place, idempotently.
set_property() {
    local file="$1" key="$2" value="$3" current
    [ -w "$(dirname "${file}")" ] || true
    if [ ! -f "${file}" ]; then
        warn "${file} not found; skipping ${key} (is this host a CloudStack agent?)"
        return 0
    fi
    current="$(sed -n "s/^[[:space:]]*${key}[[:space:]]*=[[:space:]]*//p" "${file}" | tail -1)"
    if [ "${current}" = "${value}" ]; then
        ok "${key} already ${value}"
        return 0
    fi
    if [ "${DRY_RUN}" -eq 1 ]; then
        printf '  would set %s=%s in %s (currently %s)\n' "${key}" "${value}" "${file}" "${current:-unset}"
        return 0
    fi
    cp -n "${file}" "${file}.before-ovn" 2>/dev/null || true
    if [ -n "${current}" ]; then
        sed -i "s|^[[:space:]]*${key}[[:space:]]*=.*|${key}=${value}|" "${file}"
    else
        printf '%s=%s\n' "${key}" "${value}" >> "${file}"
    fi
    ok "${key} = ${value}${current:+  (was ${current})}  [backup: ${file}.before-ovn]"
}

# ---------------------------------------------------------------------------
# Roles
# ---------------------------------------------------------------------------

do_management() {
    head2 "management server"
    need_root
    if command -v ovn-nbctl >/dev/null 2>&1; then
        ok "ovn-nbctl present ($(ovn-nbctl --version 2>/dev/null | head -1))"
    else
        install_packages "the OVN client" ovn-common || install_packages "the OVN client" ovn
    fi
    if command -v jq >/dev/null 2>&1; then ok "jq present"; else install_packages "jq" jq; fi

    run mkdir -p "${EXTENSION_LOG_DIR}"
    if id cloud >/dev/null 2>&1; then
        run chown cloud:cloud "${EXTENSION_LOG_DIR}" || warn "could not chown ${EXTENSION_LOG_DIR}"
    fi
    ok "extension log directory ${EXTENSION_LOG_DIR}"

    if [ -n "${NB_DB}" ]; then
        if [ "${DRY_RUN}" -eq 1 ]; then
            say "would test the northbound connection to ${NB_DB}"
        elif ovn-nbctl --db="${NB_DB}" --timeout=10 show >/dev/null 2>&1; then
            ok "northbound database reachable at ${NB_DB}"
        else
            warn "cannot reach the northbound database at ${NB_DB}"
            warn "  check 'ovn-nbctl set-connection ptcp:6641' on the OVN central nodes"
        fi
    else
        say "no --nb-db given; skipping the northbound reachability check"
    fi

    cat <<'NOTE'

  Still to do by hand, because they are CloudStack API calls rather than host setup:
    * install ovn.sh as <extensions-root>/ovn/ovn.sh on EVERY management server
      (note the lowercase directory; see README section 4.1)
    * cmk create extension / register extension / enable the provider
    * set the guest traffic label on the physical network to br-int
NOTE
}

do_host() {
    head2 "hypervisor"
    need_root
    [ -n "${SB_DB}" ] || die "--sb-db is required for --role host (e.g. tcp:10.0.0.1:6642)"

    if command -v ovs-vsctl >/dev/null 2>&1 && command -v ovn-controller >/dev/null 2>&1; then
        ok "openvswitch and ovn-controller already installed"
    else
        install_packages "Open vSwitch and ovn-controller" openvswitch-switch ovn-host \
            || install_packages "Open vSwitch and ovn-controller" openvswitch ovn-host
    fi

    service_enable openvswitch-switch 2>/dev/null || service_enable openvswitch

    if ! ovs-vsctl show >/dev/null 2>&1; then
        if [ "${DRY_RUN}" -eq 1 ]; then
            say "would configure Open vSwitch once ovsdb-server is running"
            return 0
        fi
        die "ovs-vsctl cannot reach ovsdb-server; is openvswitch running?"
    fi

    local sysid encap
    sysid="$(resolve_system_id)"
    [ -n "${sysid}" ] || die "could not determine a chassis system-id; pass --system-id"

    encap="${ENCAP_IP}"
    if [ -z "${encap}" ]; then
        encap="$(derive_encap_ip || true)"
        [ -n "${encap}" ] && say "derived encap ip ${encap} from the route to the southbound db"
    fi
    [ -n "${encap}" ] || die "could not determine the overlay address; pass --encap-ip"

    ovs_set system-id "${sysid}"
    ovs_set ovn-remote "${SB_DB}"
    ovs_set ovn-encap-type "${ENCAP_TYPE}"
    ovs_set ovn-encap-ip "${encap}"

    service_enable ovn-controller

    head2 "CloudStack agent"
    # Without these two the agent plugs VIFs with the bridge driver and the
    # Lswitch branch in OvsVifDriver is never reached, so nothing binds.
    set_property "${AGENT_PROPERTIES}" "libvirt.vif.driver" \
        "com.cloud.hypervisor.kvm.resource.OvsVifDriver"
    set_property "${AGENT_PROPERTIES}" "network.bridge.type" "openvswitch"
    if [ "${RESTART_AGENT}" -eq 1 ]; then
        service_restart_agent
    elif [ -f "${AGENT_PROPERTIES}" ]; then
        say "restart cloudstack-agent for the VIF driver change to take effect"
        say "  (re-run with --restart-agent to do that here; it disconnects the host briefly)"
    fi
}

service_restart_agent() {
    if command -v systemctl >/dev/null 2>&1; then
        run systemctl restart cloudstack-agent || warn "could not restart cloudstack-agent"
        ok "cloudstack-agent restarted"
    else
        warn "no systemctl; restart cloudstack-agent yourself"
    fi
}

do_gateway() {
    do_host
    head2 "gateway chassis"
    [ -n "${PUBLIC_NIC}" ] || die "--public-nic is required for --role gateway"

    # Moving the NIC that carries the default route into a bridge will cut this
    # host off unless the address moves with it. Refuse unless told twice.
    local defnic
    defnic="$(ip -4 route show default 2>/dev/null | sed -n 's/.* dev \([^ ]*\).*/\1/p' | head -1)"
    if [ "${PUBLIC_NIC}" = "${defnic}" ] && [ "${FORCE}" -ne 1 ]; then
        die "${PUBLIC_NIC} carries this host's default route; enslaving it will cut the host off.
        Move the address onto ${BRIDGE} in your network configuration first, or pass --force
        if you have already arranged for that."
    fi

    if ovs-vsctl br-exists "${BRIDGE}" 2>/dev/null; then
        ok "bridge ${BRIDGE} exists"
    else
        run ovs-vsctl add-br "${BRIDGE}" || die "could not create ${BRIDGE}"
        ok "bridge ${BRIDGE} created"
    fi

    if ovs-vsctl list-ports "${BRIDGE}" 2>/dev/null | grep -qx "${PUBLIC_NIC}"; then
        ok "${PUBLIC_NIC} already on ${BRIDGE}"
    else
        run ovs-vsctl add-port "${BRIDGE}" "${PUBLIC_NIC}" || die "could not add ${PUBLIC_NIC} to ${BRIDGE}"
        ok "${PUBLIC_NIC} added to ${BRIDGE}"
    fi

    # add-port succeeds even when the device does not exist -- OVS records the
    # port and leaves the interface in an error state. A typo in --public-nic
    # would otherwise look like success and only surface as "no north-south
    # traffic" much later.
    if [ "${DRY_RUN}" -ne 1 ]; then
        local iface_err
        iface_err="$(ovs-vsctl --if-exists get interface "${PUBLIC_NIC}" error 2>/dev/null | tr -d '"')"
        if [ -n "${iface_err}" ] && [ "${iface_err}" != "[]" ]; then
            die "${PUBLIC_NIC} is on ${BRIDGE} but the interface is in error: ${iface_err}
        Check the device name. Remove the port with:
            ovs-vsctl del-port ${BRIDGE} ${PUBLIC_NIC}"
        fi
        ok "${PUBLIC_NIC} interface is healthy"
    fi

    # Merge into any existing mappings rather than overwriting another provider.
    local existing new
    existing="$(ovs-vsctl --if-exists get open_vswitch . external_ids:ovn-bridge-mappings 2>/dev/null | tr -d '"')"
    if printf '%s' "${existing}" | tr ',' '\n' | grep -qx "${PHYSNET}:${BRIDGE}"; then
        ok "bridge mapping ${PHYSNET}:${BRIDGE} already present"
    else
        if [ -n "${existing}" ]; then new="${existing},${PHYSNET}:${BRIDGE}"; else new="${PHYSNET}:${BRIDGE}"; fi
        ovs_set ovn-bridge-mappings "${new}"
    fi

    say "add this chassis to ovn_gateway_chassis in the physical network details:"
    say "  $(resolve_system_id)"
}

# ---------------------------------------------------------------------------
# Verify
# ---------------------------------------------------------------------------

do_verify() {
    head2 "verification"
    local fail=0
    # A management server runs the extension script, not a hypervisor: it has no
    # Open vSwitch and no chassis, and checking for them would only produce
    # warnings that are correct to ignore.
    if [ "${ROLE}" = "management" ]; then
        if command -v ovn-nbctl >/dev/null 2>&1; then ok "ovn-nbctl present"; else warn "ovn-nbctl missing"; fail=1; fi
        if command -v jq >/dev/null 2>&1; then ok "jq present"; else warn "jq missing"; fail=1; fi
        if [ -d "${EXTENSION_LOG_DIR}" ]; then ok "log directory present"; else warn "log directory missing"; fail=1; fi
        if [ "${fail}" -eq 0 ]; then printf '\n  %s\n' "${GREEN}all checks passed${RESET}"
        else printf '\n  %s\n' "${RED}some checks failed${RESET}"; fi
        return "${fail}"
    fi
    if command -v ovs-vsctl >/dev/null 2>&1; then ok "ovs-vsctl present"; else warn "ovs-vsctl missing"; fail=1; fi
    if command -v ovn-controller >/dev/null 2>&1; then ok "ovn-controller present"; else warn "ovn-controller missing"; fail=1; fi

    if ovs-vsctl show >/dev/null 2>&1; then
        ok "ovsdb-server reachable"
        local ids
        ids="$(ovs-vsctl get open_vswitch . external_ids 2>/dev/null)"
        for key in system-id ovn-remote ovn-encap-type ovn-encap-ip; do
            if printf '%s' "${ids}" | grep -q "${key}"; then ok "${key} set"
            else warn "${key} NOT set"; fail=1; fi
        done
        if ovs-vsctl br-exists br-int 2>/dev/null; then
            ok "br-int exists (ovn-controller has connected at least once)"
        else
            warn "br-int does not exist yet; ovn-controller has not connected"
            fail=1
        fi
        if printf '%s' "${ids}" | grep -q "ovn-bridge-mappings"; then
            ok "bridge mappings: $(ovs-vsctl get open_vswitch . external_ids:ovn-bridge-mappings 2>/dev/null)"
        else
            say "no bridge mappings (fine unless this is a gateway chassis)"
        fi
    else
        warn "cannot reach ovsdb-server"; fail=1
    fi

    if pgrep -x ovn-controller >/dev/null 2>&1; then ok "ovn-controller running"
    else warn "ovn-controller not running"; fail=1; fi

    if [ -f "${AGENT_PROPERTIES}" ]; then
        local drv btype
        drv="$(sed -n 's/^[[:space:]]*libvirt.vif.driver[[:space:]]*=[[:space:]]*//p' "${AGENT_PROPERTIES}" | tail -1)"
        btype="$(sed -n 's/^[[:space:]]*network.bridge.type[[:space:]]*=[[:space:]]*//p' "${AGENT_PROPERTIES}" | tail -1)"
        if [ "${drv}" = "com.cloud.hypervisor.kvm.resource.OvsVifDriver" ]; then ok "agent VIF driver is OvsVifDriver"
        else warn "agent VIF driver is '${drv:-unset}', must be com.cloud.hypervisor.kvm.resource.OvsVifDriver"; fail=1; fi
        if [ "${btype}" = "openvswitch" ]; then ok "agent bridge type is openvswitch"
        else warn "agent bridge type is '${btype:-unset}', must be openvswitch"; fail=1; fi
    else
        say "no ${AGENT_PROPERTIES}; not a CloudStack agent host"
    fi

    if [ "${fail}" -eq 0 ]; then printf '\n  %s\n' "${GREEN}all checks passed${RESET}"
    else printf '\n  %s\n' "${RED}some checks failed${RESET}"; fi
    return "${fail}"
}

# ---------------------------------------------------------------------------

if [ "${VERIFY_ONLY}" -eq 1 ]; then
    do_verify
    exit $?
fi

[ -n "${ROLE}" ] || usage 1
[ "${DRY_RUN}" -eq 1 ] && printf '\n  (dry run: nothing will be changed)\n'

case "${ROLE}" in
    management) do_management ;;
    host)       do_host ;;
    gateway)    do_gateway ;;
    *)          die "unknown role '${ROLE}' (management | host | gateway)" ;;
esac

printf '\n'
[ "${DRY_RUN}" -eq 1 ] || do_verify || true
