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
# OVN NetworkOrchestrator extension for Apache CloudStack
#
# Drives an OVN northbound database through ovn-nbctl to provide guest
# networking on KVM:
#
#   * L2 and native OVN DHCP for isolated and shared guest networks
#   * L3 for isolated networks: source NAT, static NAT, port forwarding and
#     firewall rules on a per-network logical router
#   * VPCs: one logical router per VPC, a tier per logical switch, and network
#     ACLs applied to the tier's router port
#   * optionally, EVPN/VXLAN export of routed and bridged networks, driven by a
#     companion ovn-bgp-agent rather than by this script
#
# Nothing extra runs on the hypervisor beyond ovn-controller. CloudStack's
# OvsVifDriver already binds a guest NIC to a Logical_Switch_Port named after
# the NIC's UUID when the network's broadcast domain type is Lswitch — the
# port-binding convention OVN inherited from NVP — so the KVM data path needs
# no changes.
#
# Invoked by NetworkExtensionElement as:
#     ovn.sh <command> <payload-file> <timeout-seconds>
#
# CONTRACT NOTES (verified against cloudstack main @ 24.0.0-SNAPSHOT):
#   * The management server merges our stderr into stdout
#     (ProcessBuilder.redirectErrorStream(true)) and then JSON-parses the whole
#     thing. Therefore NOTHING may be written to stdout or stderr except the
#     single result JSON object. All diagnostics go to the log file.
#   * A "status" field that is present and not "success" fails the operation
#     even on exit code 0.
#   * implement-network MUST return network.broadcast_domain_type and
#     network.broadcast_uri or the management server throws.
# ---------------------------------------------------------------------------

set -u
set -o pipefail

COMMAND="${1:-}"
PAYLOAD_FILE="${2:-}"
TIMEOUT="${3:-60}"

LOG_DIR="${CS_OVN_LOG_DIR:-/var/log/cloudstack/management/extensions}"
LOG_FILE="${LOG_DIR}/ovn.log"

# Save the real stdout on fd 3. Result JSON is always written to fd 3, never to
# fd 1, so that a call site which redirects stdout (nbctl_or_fail ... >/dev/null)
# cannot swallow the result object on the way out.
exec 3>&1

# ---------------------------------------------------------------------------
# Logging — file only, never stdout/stderr (see CONTRACT NOTES)
# ---------------------------------------------------------------------------

if ! mkdir -p "${LOG_DIR}" 2>/dev/null || ! touch "${LOG_FILE}" 2>/dev/null; then
    LOG_FILE=/dev/null
fi

log() {
    printf '%s [%s] [%s] %s\n' \
        "$(date -u '+%Y-%m-%dT%H:%M:%SZ')" "$$" "${COMMAND:-?}" "$*" >> "${LOG_FILE}" 2>/dev/null || true
}

# ---------------------------------------------------------------------------
# Result emission — the ONLY thing allowed on stdout
# ---------------------------------------------------------------------------

emit_json() {
    printf '%s\n' "$1" >&3
}

emit_success() {
    # $@ : zero or more extra "key=value" pairs added to the result object
    local json pair key value
    json="$(jq -nc '{status:"success"}')"
    for pair in "$@"; do
        key="${pair%%=*}"
        value="${pair#*=}"
        json="$(printf '%s' "${json}" | jq -c --arg k "${key}" --arg v "${value}" '. + {($k): $v}')"
    done
    log "OK ${json}"
    emit_json "${json}"
    exit 0
}

emit_failure() {
    local message="$1"
    log "FAIL ${message}"
    emit_json "$(jq -nc --arg e "${message}" '{status:"failure", error:$e}')"
    exit 1
}

trap 'rc=$?; if [ ${rc} -ne 0 ]; then log "aborted rc=${rc}"; fi' EXIT

# ---------------------------------------------------------------------------
# Preflight
# ---------------------------------------------------------------------------

command -v jq >/dev/null 2>&1 || {
    printf '{"status":"failure","error":"jq is not installed on the management server"}\n'
    exit 1
}

[ -n "${COMMAND}" ]      || emit_failure "no command given"
[ -n "${PAYLOAD_FILE}" ] || emit_failure "no payload file given"
[ -r "${PAYLOAD_FILE}" ] || emit_failure "payload file ${PAYLOAD_FILE} is not readable"

command -v ovn-nbctl >/dev/null 2>&1 || \
    emit_failure "ovn-nbctl not found — install the ovn-common package on every management server"

PAYLOAD_JSON="$(cat "${PAYLOAD_FILE}")"
echo "${PAYLOAD_JSON}" | jq -e . >/dev/null 2>&1 || emit_failure "payload file is not valid JSON"

log "payload ${PAYLOAD_JSON}"

# ---------------------------------------------------------------------------
# Payload accessors
#
#   phys <key>  -> physical-network-extension-details.<key>   (admin-registered)
#   netd <key>  -> network-extension-details.<key>            (our own blob,
#                  returned by ensure-network-device and persisted by CloudStack)
#   arg  <key>  -> payload.<key>                              (per-operation)
# ---------------------------------------------------------------------------

_get() {
    local root="$1" key="$2"
    printf '%s' "${PAYLOAD_JSON}" | jq -r --arg r "${root}" --arg k "${key}" \
        '((.[$r] // {}) | .[$k]) // empty' 2>/dev/null
}

phys() { _get 'physical-network-extension-details' "$1"; }
netd() { _get 'network-extension-details' "$1"; }
arg()  { _get 'payload' "$1"; }

# require_arg <shell-var-name> <payload-key>
#
# Assigns into a named variable rather than echoing. emit_failure must run in
# the main shell: inside $( ) its exit would only end the subshell and the
# script would carry on with the error JSON as the variable's value.
require_arg() {
    local __var="$1" __key="$2" __val
    __val="$(arg "${__key}")"
    [ -n "${__val}" ] || emit_failure "payload.${__key} is required for ${COMMAND}"
    printf -v "${__var}" '%s' "${__val}"
}

# ---------------------------------------------------------------------------
# OVN Northbound connection
# ---------------------------------------------------------------------------

NB_DB="$(phys ovn_nb_db)"
[ -n "${NB_DB}" ] || emit_failure \
    "ovn_nb_db is not set in the physical network extension details (e.g. tcp:10.0.0.1:6641)"

NB_ARGS=(--db="${NB_DB}" --timeout="${TIMEOUT}")

NB_PRIVKEY="$(phys ovn_nb_private_key)"
NB_CERT="$(phys ovn_nb_certificate)"
NB_CACERT="$(phys ovn_nb_ca_cert)"
[ -n "${NB_PRIVKEY}" ] && NB_ARGS+=(--private-key="${NB_PRIVKEY}")
[ -n "${NB_CERT}" ]    && NB_ARGS+=(--certificate="${NB_CERT}")
[ -n "${NB_CACERT}" ]  && NB_ARGS+=(--ca-cert="${NB_CACERT}")

# nbctl: run a transaction, capture stdout, log stderr, never leak either.
nbctl() {
    local out rc err_file
    err_file="$(mktemp)"
    out="$(ovn-nbctl "${NB_ARGS[@]}" "$@" 2>"${err_file}")"
    rc=$?
    if [ -s "${err_file}" ]; then
        log "ovn-nbctl stderr: $(tr '\n' ' ' < "${err_file}")"
    fi
    rm -f "${err_file}"
    if [ ${rc} -ne 0 ]; then
        log "ovn-nbctl failed rc=${rc}: $*"
        return ${rc}
    fi
    printf '%s' "${out}"
    return 0
}

nbctl_or_fail() {
    local out
    if ! out="$(nbctl "$@")"; then
        emit_failure "ovn-nbctl failed: $* (see ${LOG_FILE})"
    fi
    printf '%s' "${out}"
}

# ---------------------------------------------------------------------------
# Naming
#
# The logical switch is derived from the CloudStack network id, which is stable
# and is the only network identity in the payload (network_uuid is NOT passed).
#
# The logical switch port name MUST equal the NIC uuid: OvsVifDriver emits
# <virtualport type='openvswitch'><parameters interfaceid='<nic-uuid>'/> and
# libvirt turns that into external_ids:iface-id, which is the key ovn-controller
# matches against Logical_Switch_Port.name to bind the port.
# ---------------------------------------------------------------------------

ls_name()      { printf 'cs-net-%s' "$1"; }          # guest logical switch (per tier/network)
lrp_guest()    { printf 'cs-lrp-%s' "$1"; }          # router port facing the guest LS
lsp_guest_rtr(){ printf 'cs-lsp-rtr-%s' "$1"; }      # guest LS port facing the router

# A VPC has ONE router shared by all its tiers; a standalone isolated network has
# its own. VPC_ID is set from the payload, so these pick the right scope on their own.
lr_name() {
    if [ -n "${VPC_ID}" ]; then printf 'cs-vpc-rtr-%s' "${VPC_ID}"
    else printf 'cs-rtr-%s' "$1"; fi
}
lrp_ext() {   # router port facing the external LS (the distributed gateway port)
    if [ -n "${VPC_ID}" ]; then printf 'cs-vpc-ext-lrp-%s' "${VPC_ID}"
    else printf 'cs-ext-lrp-%s' "$1"; fi
}
lsp_ext_rtr() {   # external LS port facing the router
    if [ -n "${VPC_ID}" ]; then printf 'cs-vpc-ext-lsp-%s' "${VPC_ID}"
    else printf 'cs-ext-lsp-%s' "$1"; fi
}

# External/provider logical switch. Normally shared by every router on the same
# public VLAN; with L3 EVPN it is scoped per VRF instead (see evpn_vni below),
# because ovn-bgp-agent maps one provider network to one VNI.
ext_ls_name() {
    local vlan="${1:-untagged}" vni="${2:-}"
    if [ -n "${vni}" ]; then printf 'cs-ext-%s-vni%s' "${vlan}" "${vni}"
    else printf 'cs-ext-%s' "${vlan}"; fi
}
ext_ln_name() {
    local vlan="${1:-untagged}" vni="${2:-}"
    if [ -n "${vni}" ]; then printf 'cs-ext-ln-%s-vni%s' "${vlan}" "${vni}"
    else printf 'cs-ext-ln-%s' "${vlan}"; fi
}

# Deterministic, locally-administered MACs. The second octet namespaces the role
# so the three MACs derived from one network id never collide.
_derived_mac() {
    local role_octet="$1" network_id="$2" hex
    hex="$(printf '%08x' $(( network_id & 0xFFFFFFFF )))"
    printf '02:%s:%s:%s:%s:%s' "${role_octet}" \
        "${hex:0:2}" "${hex:2:2}" "${hex:4:2}" "${hex:6:2}"
}
dhcp_server_mac() { _derived_mac 00 "$1"; }
guest_lrp_mac()   { _derived_mac 01 "$1"; }
pgw_lrp_mac()     { _derived_mac 04 "$1"; }

# VPC private gateway objects, keyed by the gateway id.
pgw_ls_name()  { printf 'cs-pgw-%s' "$1"; }
pgw_ln_name()  { printf 'cs-pgw-ln-%s' "$1"; }
pgw_lrp_name() { printf 'cs-pgw-lrp-%s' "$1"; }
pgw_lsp_name() { printf 'cs-pgw-lsp-%s' "$1"; }
# Role octet 02 for a standalone network's gateway port, 03 for a VPC's, so a
# network and a VPC that happen to share a numeric id never derive the same MAC.
ext_lrp_mac() {
    if [ -n "${VPC_ID}" ]; then _derived_mac 03 "${VPC_ID}"
    else _derived_mac 02 "$1"; fi
}

# Set for every VPC tier (addNetworkToPayload) and for the VPC-level commands
# (addVpcToPayload). Empty means a standalone isolated network.
VPC_ID="$(arg vpc_id)"

PORT_SECURITY="$(phys ovn_port_security)"
[ -n "${PORT_SECURITY}" ] || PORT_SECURITY="true"

# Name of the provider network in each host's ovn-bridge-mappings, used as the
# localnet port's network_name.
PUBLIC_PHYSNET="$(phys ovn_public_physnet)"
[ -n "${PUBLIC_PHYSNET}" ] || PUBLIC_PHYSNET="physnet1"

# Comma-separated chassis (system-id) names, highest priority first, that may
# host the distributed gateway port.
GATEWAY_CHASSIS="$(phys ovn_gateway_chassis)"

# Upper bound on how many individual OVN load-balancer VIPs one port-forwarding
# rule may expand into (OVN has no port-range DNAT).
PF_MAX_PORTS="$(phys ovn_pf_max_ports)"
[ -n "${PF_MAX_PORTS}" ] || PF_MAX_PORTS="128"

# ---------------------------------------------------------------------------
# EVPN (optional)
#
# OVN keeps using Geneve between chassis; EVPN is how those networks are exposed
# into a physical BGP/EVPN fabric. The work is done out of band by ovn-bgp-agent
# running on each host, which watches the NB DB and drives FRR plus Linux VRF and
# VXLAN devices. The CMS's entire job is to tag objects with external_ids, which
# is what this script does.
#
# The key names are fixed by ovn-bgp-agent (neutron_bgpvpn:type / :vni) and there
# is an open Neutron RFE noting that ovn-nbctl is currently the only supported way
# to set them. ovn_evpn_key_prefix exists so a fork or a renamed upstream key can
# be accommodated without editing the script.
# ---------------------------------------------------------------------------

EVPN_ENABLED="$(phys ovn_evpn_enabled)"
[ -n "${EVPN_ENABLED}" ] || EVPN_ENABLED="false"

EVPN_TYPE="$(printf '%s' "$(phys ovn_evpn_type)" | tr '[:upper:]' '[:lower:]')"
[ -n "${EVPN_TYPE}" ] || EVPN_TYPE="l3"

EVPN_VNI_BASE="$(phys ovn_evpn_vni_base)"
[ -n "${EVPN_VNI_BASE}" ] || EVPN_VNI_BASE="0"

EVPN_KEY_PREFIX="$(phys ovn_evpn_key_prefix)"
[ -n "${EVPN_KEY_PREFIX}" ] || EVPN_KEY_PREFIX="neutron_bgpvpn"

evpn_on() { [ "${EVPN_ENABLED}" = "true" ]; }

# An L3 VNI identifies a routing domain, so all tiers of a VPC share one. An L2
# VNI identifies a broadcast domain, so it is always per logical switch — giving
# a VPC's tiers the same L2 VNI would merge them into one L2 segment.
# Both are offset from a configured base so the range can be reserved in the fabric.
evpn_vni() {
    local network_id="${1:-}" scope
    if [ "${EVPN_TYPE}" = "l2" ] || [ -z "${VPC_ID}" ]; then
        scope="${network_id}"
    else
        scope="${VPC_ID}"
    fi
    case "${scope}${EVPN_VNI_BASE}" in *[!0-9]*) printf ''; return 1 ;; esac
    [ -n "${scope}" ] || { printf ''; return 1; }
    printf '%s' "$(( EVPN_VNI_BASE + scope ))"
}

# Tag a logical switch for ovn-bgp-agent. Escaping matters: the key contains a
# colon, so ovn-nbctl needs it backslash-escaped inside the column:key=value form.
evpn_tag_switch() {
    local ls="$1" type="$2" vni="$3"
    [ -n "${vni}" ] || { log "evpn: no VNI resolved for ${ls}, not tagging"; return 1; }
    log "evpn: tagging ${ls} as type=${type} vni=${vni}"
    nbctl set Logical_Switch "${ls}" \
        "external_ids:${EVPN_KEY_PREFIX}\\:type=${type}" \
        "external_ids:${EVPN_KEY_PREFIX}\\:vni=${vni}" >/dev/null || return 1
    return 0
}

# ACL priorities
ACL_PRIO_BASE=1000     # default policy
ACL_PRIO_RULE=2000     # explicit firewall rules (isolated networks)

# VPC network ACLs carry an explicit rule number: lower number = higher precedence
# in CloudStack, higher priority = evaluated first in OVN, so we invert.
ACL_PRIO_VPC_TOP=30000

# ---------------------------------------------------------------------------
# DHCP_Options helpers
# ---------------------------------------------------------------------------

find_dhcp_options() {
    local network_id="$1"
    nbctl --bare --columns=_uuid find DHCP_Options \
        "external_ids:cs-network-id=\"${network_id}\"" | head -n1
}

# ---------------------------------------------------------------------------
# Per-network serialisation
#
# A DHCP_Options row is a singleton per network, but the NB schema has no index
# on that table, so OVSDB cannot refuse a duplicate, and "create only if absent"
# cannot be expressed in one ovn-nbctl transaction. Two callers therefore both
# look, both find nothing, and both create.
#
# This is not hypothetical: CloudStack calls ensure_dhcp_options from both
# implement-network and prepare-nic, so a network coming up while instances are
# already being deployed produces several rows. VMs then bind to different ones,
# and a later config-dhcp-subnet updates only one of them -- the instances on
# the others keep being handed stale DNS, MTU or lease settings indefinitely,
# with nothing logged.
#
# The lock below removes the race within one management server, which is the
# common case. dedupe_dhcp_options converges the rest, because a cluster has
# several management servers and a file lock is local to one of them.
# ---------------------------------------------------------------------------

LOCK_DIR="${CS_OVN_LOCK_DIR:-/var/run/cloudstack-ovn}"

# lock_acquire <fd> <key>; lock_release <fd>
#
# fd 9 is the command-wide lock, fd 8 the external switch. They are separate
# because the external switch is shared by every network on a public VLAN,
# which is a different scope from the network or VPC the command names.
lock_acquire() {
    local fd="$1" key="$2"
    command -v flock >/dev/null 2>&1 || return 0
    mkdir -p "${LOCK_DIR}" 2>/dev/null || return 0
    eval "exec ${fd}>\"${LOCK_DIR}/${key}.lock\"" 2>/dev/null || return 0
    flock -w 30 "${fd}" 2>/dev/null \
        || log "warning: timed out waiting for the ${key} lock; continuing unlocked"
    return 0
}

lock_release() {
    local fd="$1"
    command -v flock >/dev/null 2>&1 || return 0
    flock -u "${fd}" 2>/dev/null || true
    eval "exec ${fd}>&-" 2>/dev/null || true
    return 0
}

# Collapse duplicate DHCP_Options rows for a network onto one, and echo it.
#
# Every caller picks the same winner (lowest uuid), so management servers that
# raced each other converge instead of fighting. Logical_Switch_Port's
# dhcpv4_options is a WEAK reference: destroying a row silently empties the
# column on every port that pointed at it, leaving those instances with no DHCP
# at all. Ports must therefore be re-pointed BEFORE the row they reference is
# destroyed, never after.
dedupe_dhcp_options() {
    local network_id="$1" uuids winner uuid lsp
    uuids="$(nbctl --bare --columns=_uuid find DHCP_Options \
                "external_ids:cs-network-id=\"${network_id}\"" 2>/dev/null | grep . | sort)"
    winner="$(printf '%s\n' "${uuids}" | head -1)"
    [ -n "${winner}" ] || return 1
    for uuid in ${uuids}; do
        [ "${uuid}" = "${winner}" ] && continue
        log "collapsing duplicate DHCP_Options ${uuid} for network ${network_id} onto ${winner}"
        for lsp in $(nbctl --bare --columns=name find Logical_Switch_Port \
                         "dhcpv4_options=${uuid}" 2>/dev/null); do
            nbctl lsp-set-dhcpv4-options "${lsp}" "${winner}" >/dev/null 2>&1 \
                || log "warning: could not re-point ${lsp} onto ${winner}"
        done
        nbctl --if-exists destroy DHCP_Options "${uuid}" >/dev/null 2>&1 || true
    done

    # Two racers can disagree about the winner for a moment, because OVSDB uuids
    # are random and a row created later can sort lower than one created
    # earlier. A port pointed at a row in that window loses its reference when
    # the row goes, and the reference is weak so nothing says so. Sweep this
    # network's ports and give any that ended up with nothing the winner.
    local port type
    for port in $(nbctl lsp-list "$(ls_name "${network_id}")" 2>/dev/null | sed -n 's/.*(\(.*\))$/\1/p'); do
        type="$(nbctl lsp-get-type "${port}" 2>/dev/null)"
        [ -n "${type}" ] && continue          # router and localnet ports take no DHCP
        [ -n "$(nbctl --bare --columns=dhcpv4_options list Logical_Switch_Port "${port}" 2>/dev/null)" ] && continue
        log "repairing ${port}: left with no DHCP_Options by a concurrent collapse"
        nbctl lsp-set-dhcpv4-options "${port}" "${winner}" >/dev/null 2>&1 || true
    done

    printf '%s' "${winner}"
}

# Create or update the DHCP_Options row for this network's subnet.
# Echoes the row uuid.
ensure_dhcp_options() {
    local network_id="$1" cidr="$2" gateway="$3"
    local uuid server_mac dns mtu lease opts

    [ -n "${cidr}" ] || return 1
    [ -n "${gateway}" ] || return 1

    server_mac="$(dhcp_server_mac "${network_id}")"
    dns="$(phys ovn_dns_servers)"
    [ -n "${dns}" ] || dns="${gateway}"
    mtu="$(phys ovn_mtu)"
    lease="$(phys ovn_dhcp_lease_time)"
    [ -n "${lease}" ] || lease="86400"

    # ovn-nbctl renders set-valued DHCP options (such as dns_server) as {a,b}
    opts=(
        "server_id=\"${gateway}\""
        "server_mac=\"${server_mac}\""
        "lease_time=\"${lease}\""
        "router=\"${gateway}\""
        "dns_server=\"{${dns}}\""
    )
    [ -n "${mtu}" ] && opts+=("mtu=\"${mtu}\"")

    uuid="$(find_dhcp_options "${network_id}")"
    if [ -n "${uuid}" ]; then
        log "updating DHCP_Options ${uuid} for network ${network_id}"
        nbctl set DHCP_Options "${uuid}" "cidr=${cidr}" \
            "options={$(IFS=,; echo "${opts[*]}")}" >/dev/null || return 1
    else
        log "creating DHCP_Options for network ${network_id} cidr=${cidr}"
        uuid="$(nbctl create DHCP_Options "cidr=${cidr}" \
            "options={$(IFS=,; echo "${opts[*]}")}" \
            "external_ids:cs-network-id=${network_id}")" || return 1
    fi

    # Another management server may have created its own row in the same window.
    # Every racer collapses onto the same winner, so this converges.
    local winner
    winner="$(dedupe_dhcp_options "${network_id}")" && [ -n "${winner}" ] && uuid="${winner}"

    printf '%s' "${uuid}"
}

destroy_dhcp_options() {
    local network_id="$1" uuid
    uuid="$(find_dhcp_options "${network_id}")"
    if [ -n "${uuid}" ]; then
        log "destroying DHCP_Options ${uuid}"
        nbctl destroy DHCP_Options "${uuid}" >/dev/null || true
    fi
}

# ---------------------------------------------------------------------------
# L3 helpers
#
# Topology per isolated network:
#
#        internet
#           │
#   ┌───────┴────────┐  cs-ext-<vlan>      shared external logical switch
#   │ localnet port  │  cs-ext-ln-<vlan>   mapped via ovn-bridge-mappings
#   └───────┬────────┘
#           │ cs-ext-lsp-<id>  (type=router)
#   ┌───────┴────────┐  cs-ext-lrp-<id>    DGP, holds the source-NAT IP
#   │ cs-rtr-<id>    │  logical router
#   └───────┬────────┘  cs-lrp-<id>        holds the guest gateway IP
#           │ cs-lsp-rtr-<id>  (type=router)
#   ┌───────┴────────┐
#   │ cs-net-<id>    │  guest logical switch
#   └────────────────┘
# ---------------------------------------------------------------------------

# "255.255.255.0" -> "24". Private gateways carry a netmask, not a CIDR.
prefix_from_netmask() {
    local mask="$1" bits=0 octet
    local IFS=.
    for octet in ${mask}; do
        case "${octet}" in
            255) bits=$(( bits + 8 )) ;; 254) bits=$(( bits + 7 )) ;;
            252) bits=$(( bits + 6 )) ;; 248) bits=$(( bits + 5 )) ;;
            240) bits=$(( bits + 4 )) ;; 224) bits=$(( bits + 3 )) ;;
            192) bits=$(( bits + 2 )) ;; 128) bits=$(( bits + 1 )) ;;
            0)   ;;
            *)   return 1 ;;
        esac
    done
    printf '%s' "${bits}"
}

# Prefix length out of "10.1.1.0/24" -> "24"
prefix_from_cidr() {
    local cidr="$1"
    case "${cidr}" in
        */*) printf '%s' "${cidr#*/}" ;;
        *)   printf '' ;;
    esac
}

# Create just the router. For a VPC this is the one router every tier hangs off,
# so it is also created by implement-vpc before any tier exists.
ensure_lr() {
    local network_id="${1:-}" lr tag
    lr="$(lr_name "${network_id}")"
    if [ -n "${VPC_ID}" ]; then tag="external_ids:cs-vpc-id=${VPC_ID}"
    else tag="external_ids:cs-network-id=${network_id}"; fi
    log "ensuring logical router ${lr}"
    nbctl -- --may-exist lr-add "${lr}" \
          -- set Logical_Router "${lr}" "${tag}" >/dev/null || return 1
    warn_if_duplicated Logical_Router "${lr}"
    return 0
}

# Logical_Switch, Logical_Router, DHCP_Options, Load_Balancer and ACL have no
# index on name in the NB schema -- only the two port tables do. ovn-nbctl's
# --may-exist is therefore a read followed by a write with nothing to stop two
# callers both reading "absent", and OVSDB will store both rows happily.
#
# The per-command lock below prevents that within one management server. A
# second management server has no visibility of that lock, so this at least
# makes the damage loud instead of silent: two switches of the same name split
# a network in half, and the instances on the loser are quietly isolated.
warn_if_duplicated() {
    local table="$1" name="$2" n
    n="$(nbctl --bare --columns=_uuid find "${table}" "name=${name}" 2>/dev/null | grep -c .)"
    [ "${n}" -le 1 ] 2>/dev/null && return 0
    log "WARNING: ${n} rows in ${table} are named ${name}. ${table} has no name index," \
        "so concurrent creation from more than one management server can duplicate it." \
        "Resolve by hand: ovn-nbctl find ${table} name=${name}"
    return 0
}

# Create the router and attach this network's (or VPC tier's) guest-side leg.
ensure_router() {
    local network_id="$1" cidr="$2" gateway="$3"
    local lr lrp lsp prefix mac

    lr="$(lr_name "${network_id}")"
    lrp="$(lrp_guest "${network_id}")"
    lsp="$(lsp_guest_rtr "${network_id}")"
    prefix="$(prefix_from_cidr "${cidr}")"

    [ -n "${gateway}" ] || { log "ensure_router: no gateway, skipping"; return 1; }
    [ -n "${prefix}" ]  || { log "ensure_router: no prefix in cidr '${cidr}'"; return 1; }

    ensure_lr "${network_id}" || return 1

    mac="$(guest_lrp_mac "${network_id}")"

    log "attaching ${gateway}/${prefix} to ${lr} via ${lrp}"
    nbctl -- --may-exist lrp-add "${lr}" "${lrp}" "${mac}" "${gateway}/${prefix}" \
          -- set Logical_Router_Port "${lrp}" "external_ids:cs-network-id=${network_id}" \
          -- --may-exist lsp-add "$(ls_name "${network_id}")" "${lsp}" \
          -- lsp-set-type "${lsp}" router \
          -- lsp-set-addresses "${lsp}" router \
          -- lsp-set-options "${lsp}" "router-port=${lrp}" >/dev/null || return 1
    return 0
}

# Create the shared external switch and this router's distributed gateway port.
# Only called for the source-NAT IP: that IP is the router's own external address.
ensure_external() {
    local network_id="$1" public_ip="$2" public_vlan="$3" public_gateway="$4" public_cidr="$5"
    local lr ext_ls ln lrp lsp prefix mac chassis prio vni

    lr="$(lr_name "${network_id}")"
    prefix="$(prefix_from_cidr "${public_cidr}")"
    [ -n "${prefix}" ] || { log "ensure_external: no prefix in public_cidr '${public_cidr}'"; return 1; }
    [ -n "${public_gateway}" ] || { log "ensure_external: no public_gateway"; return 1; }

    # With L3 EVPN the provider switch is scoped per VRF, so each VPC (or each
    # standalone network) gets its own provider network and therefore its own VNI.
    vni=""
    if evpn_on && [ "${EVPN_TYPE}" = "l3" ]; then
        vni="$(evpn_vni "${network_id}")" || vni=""
    fi
    ext_ls="$(ext_ls_name "${public_vlan}" "${vni}")"
    ln="$(ext_ln_name "${public_vlan}" "${vni}")"
    lrp="$(lrp_ext "${network_id}")"
    lsp="$(lsp_ext_rtr "${network_id}")"
    mac="$(ext_lrp_mac "${network_id}")"

    log "ensuring external switch ${ext_ls} (physnet=${PUBLIC_PHYSNET} vlan=${public_vlan:-untagged})"
    # Shared by every network on this public VLAN, so it needs a lock of its own:
    # the command lock is scoped to one network or VPC and two different networks
    # reach here concurrently.
    lock_acquire 8 "ext-${PUBLIC_PHYSNET}-${public_vlan:-untagged}${vni:+-vni${vni}}"
    nbctl -- --may-exist ls-add "${ext_ls}" \
          -- set Logical_Switch "${ext_ls}" "external_ids:cs-managed=true" \
          -- --may-exist lsp-add "${ext_ls}" "${ln}" \
          -- lsp-set-type "${ln}" localnet \
          -- lsp-set-addresses "${ln}" unknown \
          -- lsp-set-options "${ln}" "network_name=${PUBLIC_PHYSNET}" >/dev/null \
        || { lock_release 8; return 1; }
    warn_if_duplicated Logical_Switch "${ext_ls}"
    lock_release 8

    if [ -n "${vni}" ]; then
        evpn_tag_switch "${ext_ls}" "l3" "${vni}" \
            || log "warning: could not tag ${ext_ls} for EVPN"
    fi

    # Tag the localnet port only for a real VLAN id.
    case "${public_vlan}" in
        ''|untagged|*[!0-9]*) : ;;
        *) nbctl set Logical_Switch_Port "${ln}" "tag_request=${public_vlan}" >/dev/null \
               || log "warning: could not set tag_request=${public_vlan} on ${ln}" ;;
    esac

    log "ensuring gateway port ${lrp} with ${public_ip}/${prefix}"
    nbctl -- --may-exist lrp-add "${lr}" "${lrp}" "${mac}" "${public_ip}/${prefix}" \
          -- --may-exist lsp-add "${ext_ls}" "${lsp}" \
          -- lsp-set-type "${lsp}" router \
          -- lsp-set-addresses "${lsp}" router \
          -- lsp-set-options "${lsp}" "router-port=${lrp}" >/dev/null || return 1

    # Pin the distributed gateway port to chassis, highest priority first.
    if [ -n "${GATEWAY_CHASSIS}" ]; then
        prio=100
        local IFS=','
        for chassis in ${GATEWAY_CHASSIS}; do
            unset IFS
            chassis="$(printf '%s' "${chassis}" | tr -d '[:space:]')"
            [ -n "${chassis}" ] || continue
            nbctl lrp-set-gateway-chassis "${lrp}" "${chassis}" "${prio}" >/dev/null \
                || log "warning: could not pin ${lrp} to chassis ${chassis}"
            prio=$(( prio - 10 ))
            IFS=','
        done
        unset IFS
    else
        log "warning: ovn_gateway_chassis is not set — the gateway port will not be scheduled"
    fi

    # Default route out of the router.
    nbctl -- --may-exist lr-route-add "${lr}" "0.0.0.0/0" "${public_gateway}" >/dev/null \
        || log "warning: could not add default route via ${public_gateway}"
    return 0
}

# Fail loudly if L3 has not been set up yet. CloudStack applies the source-NAT IP
# during implement(), so anything else arriving first is a real ordering problem
# and silently half-configuring NAT would be worse than stopping.
require_router() {
    local network_id="$1" lr
    lr="$(lr_name "${network_id}")"
    if [ -z "$(nbctl --bare --columns=name find Logical_Router "name=${lr}")" ]; then
        emit_failure "logical router ${lr} does not exist — the source NAT IP must be assigned before NAT rules"
    fi
}

# Private IP behind a public IP, read back from the router's dnat_and_snat rules.
# Used to target ingress firewall rules at the right VM instead of the whole subnet.
nat_logical_ip() {
    local network_id="$1" public_ip="$2"
    nbctl --bare --columns=logical_ip find NAT \
        "type=dnat_and_snat" "external_ip=${public_ip}" 2>/dev/null | head -n1
}

# All load balancers this network owns, optionally filtered by public IP.
lb_find() {
    local network_id="$1" public_ip="${2:-}"
    if [ -n "${public_ip}" ]; then
        nbctl --bare --columns=_uuid find Load_Balancer \
            "external_ids:cs-network-id=\"${network_id}\"" \
            "external_ids:cs-public-ip=\"${public_ip}\""
    else
        nbctl --bare --columns=_uuid find Load_Balancer \
            "external_ids:cs-network-id=\"${network_id}\""
    fi
}

# Expand "22" or "22-25" into a space-separated list of ports.
expand_ports() {
    local range="$1" start end i out=""
    case "${range}" in
        *-*) start="${range%%-*}"; end="${range##*-}" ;;
        *)   start="${range}"; end="${range}" ;;
    esac
    case "${start}${end}" in *[!0-9]*) return 1 ;; esac
    [ "${start}" -le "${end}" ] || return 1
    if [ $(( end - start + 1 )) -gt "${PF_MAX_PORTS}" ]; then
        return 2
    fi
    i="${start}"
    while [ "${i}" -le "${end}" ]; do
        out="${out}${i} "
        i=$(( i + 1 ))
    done
    printf '%s' "${out% }"
    return 0
}

# ---------------------------------------------------------------------------
# Firewall ACL helpers
# ---------------------------------------------------------------------------

# Remove every ACL this script owns on the network's guest switch.
# acl_clear <scope-id> [switch] [tag-key]
# acl_add   <scope-id> <direction> <priority> <match> <action> [switch] [tag-key]
#
# The trailing arguments default to the guest switch of <scope-id> tagged
# cs-network-id, which is what isolated networks and VPC tiers want. Private
# gateways pass their own switch and cs-gateway-id instead.
acl_clear() {
    local scope_id="$1" ls="${2:-}" tag_key="${3:-cs-network-id}" uuid
    [ -n "${ls}" ] || ls="$(ls_name "${scope_id}")"
    for uuid in $(nbctl --bare --columns=_uuid find ACL \
                      "external_ids:${tag_key}=\"${scope_id}\"" 2>/dev/null); do
        nbctl -- remove Logical_Switch "${ls}" acls "${uuid}" >/dev/null 2>&1 || true
        nbctl destroy ACL "${uuid}" >/dev/null 2>&1 || true
    done
}

acl_add() {
    local scope_id="$1" direction="$2" priority="$3" match="$4" action="$5"
    local ls="${6:-}" tag_key="${7:-cs-network-id}"
    [ -n "${ls}" ] || ls="$(ls_name "${scope_id}")"
    # ACL is not a root table in the NB schema. A row created in a transaction
    # of its own is unreferenced when that transaction commits, so OVSDB garbage
    # collects it immediately and the follow-up "add" fails with a referential
    # integrity violation. Create and attach have to be one transaction, with
    # --id to carry the new row's uuid between the two commands.
    nbctl -- --id=@acl create ACL "direction=${direction}" "priority=${priority}" \
                "match=\"$(printf '%s' "${match}" | sed 's/"/\\"/g')\"" "action=${action}" \
                "external_ids:${tag_key}=${scope_id}" \
          -- add Logical_Switch "${ls}" acls @acl >/dev/null || return 1
    return 0
}

# Build the L3/L4 portion of an ACL match from a rule's protocol and ports.
acl_proto_match() {
    local proto="$1" port_start="$2" port_end="$3" icmp_type="$4" icmp_code="$5"
    local m=""
    case "$(printf '%s' "${proto}" | tr '[:upper:]' '[:lower:]')" in
        tcp|udp)
            m="${proto}"
            if [ -n "${port_start}" ] && [ "${port_start}" != "null" ]; then
                if [ -n "${port_end}" ] && [ "${port_end}" != "null" ] && [ "${port_end}" != "${port_start}" ]; then
                    m="${m} && ${proto}.dst >= ${port_start} && ${proto}.dst <= ${port_end}"
                else
                    m="${m} && ${proto}.dst == ${port_start}"
                fi
            fi
            ;;
        icmp)
            m="icmp4"
            [ -n "${icmp_type}" ] && [ "${icmp_type}" != "null" ] && [ "${icmp_type}" -ge 0 ] 2>/dev/null \
                && m="${m} && icmp4.type == ${icmp_type}"
            [ -n "${icmp_code}" ] && [ "${icmp_code}" != "null" ] && [ "${icmp_code}" -ge 0 ] 2>/dev/null \
                && m="${m} && icmp4.code == ${icmp_code}"
            ;;
        # "all" means every protocol; the caller already constrains the addresses
        # with ip4.src/ip4.dst, so an extra "ip4" term would just be noise.
        all|''|null) m="" ;;
        *) m="" ;;
    esac
    printf '%s' "${m}"
}

# Join non-empty match terms with " && ".
match_join() {
    local out="" term
    for term in "$@"; do
        [ -n "${term}" ] || continue
        if [ -z "${out}" ]; then out="${term}"; else out="${out} && ${term}"; fi
    done
    printf '%s' "${out}"
}

# ---------------------------------------------------------------------------
# Commands
# ---------------------------------------------------------------------------

cmd_ensure_network_device() {
    # OVN is a distributed fabric: there is no per-network host to select.
    # We use this hook as a Northbound reachability probe and to hand CloudStack
    # a stable details blob that is echoed back on every later call.
    local network_id nb_schema
    require_arg network_id network_id

    if ! nb_schema="$(nbctl --bare get NB_Global . external_ids 2>/dev/null)"; then
        # get on an empty table can legitimately fail; fall back to a plain list
        if ! nbctl list NB_Global >/dev/null; then
            emit_failure "cannot reach the OVN northbound database at ${NB_DB}"
        fi
    fi

    log "northbound ${NB_DB} reachable (NB_Global external_ids=${nb_schema:-none})"
    emit_json "$(jq -nc --arg db "${NB_DB}" --arg ls "$(ls_name "${network_id}")" \
        '{nb_db:$db, logical_switch:$ls, status:"success"}')"
    exit 0
}

cmd_implement_network() {
    local network_id ls cidr gateway dhcp_uuid
    require_arg network_id network_id
    ls="$(ls_name "${network_id}")"
    cidr="$(arg cidr)"
    gateway="$(arg gateway)"

    log "implementing logical switch ${ls} cidr=${cidr:-none} gateway=${gateway:-none}"

    nbctl_or_fail -- --may-exist ls-add "${ls}" \
        -- set Logical_Switch "${ls}" \
           "external_ids:cs-network-id=${network_id}" \
           "external_ids:cs-zone-id=$(arg zone_id)" >/dev/null
    warn_if_duplicated Logical_Switch "${ls}"

    # OVN native DHCP. If the network has no CIDR (shared network with upstream
    # addressing) we simply skip it.
    if [ -n "${cidr}" ] && [ -n "${gateway}" ]; then
        if dhcp_uuid="$(ensure_dhcp_options "${network_id}" "${cidr}" "${gateway}")"; then
            log "DHCP_Options ${dhcp_uuid} ready for ${ls}"
        else
            emit_failure "failed to configure DHCP options for network ${network_id}"
        fi
    else
        log "no cidr/gateway in payload — skipping DHCP_Options for ${ls}"
    fi

    # L2 EVPN stretches the guest subnet itself into the fabric, so the tag goes
    # on the guest switch rather than the provider switch.
    if evpn_on && [ "${EVPN_TYPE}" = "l2" ]; then
        evpn_tag_switch "${ls}" "l2" "$(evpn_vni "${network_id}")" \
            || log "warning: could not tag ${ls} for L2 EVPN"
    fi

    # Attach the gateway leg here rather than waiting for assign-ip. DHCP already
    # advertises this gateway, so the router port should exist as soon as the
    # network does — and a VPC tier that never receives a public IP of its own
    # would otherwise have no gateway at all.
    if [ -n "${cidr}" ] && [ -n "${gateway}" ]; then
        ensure_router "${network_id}" "${cidr}" "${gateway}" \
            || emit_failure "failed to attach the gateway leg for network ${network_id}"
    fi

    # REQUIRED by applyNetworkUpdateFromScriptOutput(). BroadcastDomainType.Lswitch
    # is what makes OvsVifDriver take the interfaceid path on KVM.
    emit_json "$(jq -nc --arg uri "lswitch:${ls}" '{
        status: "success",
        "network.broadcast_domain_type": "Lswitch",
        "network.broadcast_uri": $uri
    }')"
    exit 0
}

cmd_prepare_nic() {
    local network_id ls nic_uuid mac ip addresses dhcp_uuid cidr gateway
    require_arg network_id network_id
    ls="$(ls_name "${network_id}")"
    require_arg nic_uuid nic_uuid
    require_arg mac mac
    ip="$(arg ip)"
    cidr="$(arg cidr)"
    gateway="$(arg gateway)"

    # The logical switch may not exist yet if implement-network was skipped
    # (persistent networks, restarts). Make this path self-sufficient.
    nbctl_or_fail -- --may-exist ls-add "${ls}" \
        -- set Logical_Switch "${ls}" "external_ids:cs-network-id=${network_id}" >/dev/null
    warn_if_duplicated Logical_Switch "${ls}"

    if [ -n "${ip}" ]; then
        addresses="${mac} ${ip}"
    else
        addresses="${mac} dynamic"
    fi

    log "binding port ${nic_uuid} on ${ls} addresses='${addresses}'"

    local txn=(
        -- --may-exist lsp-add "${ls}" "${nic_uuid}"
        -- lsp-set-addresses "${nic_uuid}" "${addresses}"
        -- set Logical_Switch_Port "${nic_uuid}"
           "external_ids:cs-network-id=${network_id}"
           "external_ids:cs-nic-uuid=${nic_uuid}"
    )
    if [ "${PORT_SECURITY}" = "true" ]; then
        txn+=(-- lsp-set-port-security "${nic_uuid}" "${addresses}")
    fi

    nbctl_or_fail "${txn[@]}" >/dev/null

    # Attach DHCP options if we have a subnet for them.
    if [ -n "${cidr}" ] && [ -n "${gateway}" ]; then
        dhcp_uuid="$(find_dhcp_options "${network_id}")"
        if [ -z "${dhcp_uuid}" ]; then
            dhcp_uuid="$(ensure_dhcp_options "${network_id}" "${cidr}" "${gateway}")" || dhcp_uuid=""
        fi
        if [ -n "${dhcp_uuid}" ]; then
            nbctl lsp-set-dhcpv4-options "${nic_uuid}" "${dhcp_uuid}" >/dev/null \
                || log "warning: could not attach DHCP options ${dhcp_uuid} to ${nic_uuid}"
        fi
    fi

    # Wait for ovn-northd to compile the change into the southbound DB so the
    # flows exist by the time libvirt plugs the VIF.
    nbctl --wait=sb sync >/dev/null || log "warning: --wait=sb sync timed out"

    emit_success
}

cmd_release_nic() {
    local nic_uuid
    require_arg nic_uuid nic_uuid
    log "releasing port ${nic_uuid}"
    nbctl_or_fail -- --if-exists lsp-del "${nic_uuid}" >/dev/null
    emit_success
}

cmd_shutdown_network() {
    # Deliberately a no-op against the fabric.
    #
    # CloudStack shuts a network down while its NICs may still exist, and
    # implement-network is not guaranteed to be followed by prepare-nic for
    # already-running VMs. Deleting the logical switch here would blackhole
    # them. An idle logical switch costs nothing in OVN, so we keep it and let
    # destroy-network do the cleanup.
    local network_id
    require_arg network_id network_id
    log "shutdown-network for ${network_id}: keeping logical switch $(ls_name "${network_id}") in place"
    emit_success
}

cmd_destroy_network() {
    local network_id ls lr uuid
    require_arg network_id network_id
    ls="$(ls_name "${network_id}")"
    lr="$(lr_name "${network_id}")"

    # Load balancers first: they are referenced by the router.
    for uuid in $(lb_find "${network_id}"); do
        nbctl -- --if-exists lr-lb-del "${lr}" "${uuid}" >/dev/null 2>&1 || true
        nbctl destroy Load_Balancer "${uuid}" >/dev/null 2>&1 || true
    done

    acl_clear "${network_id}"

    if [ -n "${VPC_ID}" ]; then
        # A VPC tier: detach this tier's leg but leave the shared router alone —
        # sibling tiers are still using it. shutdown-vpc removes the router.
        log "destroying VPC tier ${ls} (router ${lr} belongs to VPC ${VPC_ID}, kept)"
        nbctl -- --if-exists lrp-del "$(lrp_guest "${network_id}")" >/dev/null 2>&1 || true
    else
        log "destroying logical router ${lr} and logical switch ${ls}"
        # lr-del and ls-del cascade to their ports; the external-side LSP lives on
        # the shared external switch, so it is removed by name first.
        nbctl -- --if-exists lsp-del "$(lsp_ext_rtr "${network_id}")" >/dev/null 2>&1 || true
        nbctl -- --if-exists lr-del "${lr}" >/dev/null 2>&1 || true
    fi

    nbctl_or_fail -- --if-exists ls-del "${ls}" >/dev/null
    destroy_dhcp_options "${network_id}"
    emit_success
}

# ---------------------------------------------------------------------------
# L3: NAT, port forwarding, firewall
# ---------------------------------------------------------------------------

cmd_assign_ip() {
    local network_id public_ip source_nat cidr gateway public_vlan public_gateway public_cidr lr
    require_arg network_id network_id
    require_arg public_ip public_ip
    source_nat="$(arg source_nat)"
    cidr="$(arg cidr)"
    gateway="$(arg gateway)"
    public_vlan="$(arg public_vlan)"
    public_gateway="$(arg public_gateway)"
    public_cidr="$(arg public_cidr)"
    lr="$(lr_name "${network_id}")"

    ensure_router "${network_id}" "${cidr}" "${gateway}" \
        || emit_failure "failed to create logical router for network ${network_id}"

    if [ "${source_nat}" = "true" ]; then
        ensure_external "${network_id}" "${public_ip}" "${public_vlan}" \
                        "${public_gateway}" "${public_cidr}" \
            || emit_failure "failed to configure external connectivity for ${public_ip}"

        log "adding snat ${cidr} -> ${public_ip} on ${lr}"
        nbctl -- --may-exist lr-nat-add "${lr}" snat "${public_ip}" "${cidr}" >/dev/null \
            || emit_failure "failed to add source NAT ${cidr} -> ${public_ip}"
    else
        # A plain public IP becomes reachable once a static NAT or port forward
        # references it; OVN answers ARP for NAT external IPs on the gateway port.
        log "public IP ${public_ip} assigned, no source NAT requested — nothing to program yet"
    fi

    nbctl --wait=sb sync >/dev/null || log "warning: --wait=sb sync timed out"
    emit_success
}

cmd_release_ip() {
    local network_id public_ip source_nat cidr lr lrp lsp uuid
    require_arg network_id network_id
    require_arg public_ip public_ip
    source_nat="$(arg source_nat)"
    cidr="$(arg cidr)"
    lr="$(lr_name "${network_id}")"

    # Drop any NAT entry that uses this address, whichever kind it is.
    nbctl -- --if-exists lr-nat-del "${lr}" dnat_and_snat "${public_ip}" >/dev/null 2>&1 || true

    # Tear down load balancers (port forwards) bound to this address.
    for uuid in $(lb_find "${network_id}" "${public_ip}"); do
        nbctl -- --if-exists lr-lb-del "${lr}" "${uuid}" >/dev/null 2>&1 || true
        nbctl destroy Load_Balancer "${uuid}" >/dev/null 2>&1 || true
    done

    if [ "${source_nat}" = "true" ]; then
        log "releasing source NAT IP ${public_ip} and the gateway port"
        [ -n "${cidr}" ] && nbctl -- --if-exists lr-nat-del "${lr}" snat "${cidr}" >/dev/null 2>&1 || true
        nbctl -- --if-exists lr-route-del "${lr}" "0.0.0.0/0" >/dev/null 2>&1 || true
        lrp="$(lrp_ext "${network_id}")"
        lsp="$(lsp_ext_rtr "${network_id}")"
        nbctl -- --if-exists lsp-del "${lsp}" >/dev/null 2>&1 || true
        nbctl -- --if-exists lrp-del "${lrp}" >/dev/null 2>&1 || true
    fi

    emit_success
}

cmd_add_static_nat() {
    local network_id public_ip private_ip lr
    require_arg network_id network_id
    require_arg public_ip public_ip
    require_arg private_ip private_ip
    require_router "${network_id}"
    lr="$(lr_name "${network_id}")"

    log "adding dnat_and_snat ${public_ip} <-> ${private_ip} on ${lr}"
    # Replace any stale mapping for this external IP first so a re-point is clean.
    nbctl -- --if-exists lr-nat-del "${lr}" dnat_and_snat "${public_ip}" >/dev/null 2>&1 || true
    nbctl -- lr-nat-add "${lr}" dnat_and_snat "${public_ip}" "${private_ip}" >/dev/null \
        || emit_failure "failed to add static NAT ${public_ip} -> ${private_ip}"

    nbctl --wait=sb sync >/dev/null || log "warning: --wait=sb sync timed out"
    emit_success
}

cmd_delete_static_nat() {
    local network_id public_ip lr
    require_arg network_id network_id
    require_arg public_ip public_ip
    lr="$(lr_name "${network_id}")"
    log "removing dnat_and_snat ${public_ip} from ${lr}"
    nbctl -- --if-exists lr-nat-del "${lr}" dnat_and_snat "${public_ip}" >/dev/null 2>&1 || true
    emit_success
}

# One Load_Balancer per (network, public IP, protocol); each forwarded port is a
# vips map entry. OVN has no port-range DNAT, so a range expands into one entry
# per port, capped by ovn_pf_max_ports.
_pf_lb_uuid() {
    local network_id="$1" public_ip="$2" proto="$3" create="$4" uuid lr
    uuid="$(nbctl --bare --columns=_uuid find Load_Balancer \
                "external_ids:cs-network-id=\"${network_id}\"" \
                "external_ids:cs-public-ip=\"${public_ip}\"" \
                "external_ids:cs-protocol=\"${proto}\"" 2>/dev/null | head -n1)"
    if [ -z "${uuid}" ] && [ "${create}" = "create" ]; then
        lr="$(lr_name "${network_id}")"
        uuid="$(nbctl create Load_Balancer "name=cs-pf-${network_id}-${public_ip}-${proto}" \
                    "protocol=${proto}" \
                    "external_ids:cs-network-id=${network_id}" \
                    "external_ids:cs-vpc-id=${VPC_ID:-none}" \
                    "external_ids:cs-public-ip=${public_ip}" \
                    "external_ids:cs-protocol=${proto}")" || return 1
        [ -n "${uuid}" ] || return 1
        nbctl lr-lb-add "${lr}" "${uuid}" >/dev/null 2>&1 || true
    fi
    printf '%s' "${uuid}"
}

cmd_add_port_forward() {
    local network_id public_ip private_ip public_port private_port proto
    local pub_list priv_list uuid rc i n pub priv
    require_arg network_id network_id
    require_arg public_ip public_ip
    require_arg private_ip private_ip
    require_arg public_port public_port
    require_arg private_port private_port
    proto="$(printf '%s' "$(arg protocol)" | tr '[:upper:]' '[:lower:]')"
    case "${proto}" in
        tcp|udp|sctp) : ;;
        *) emit_failure "unsupported port-forwarding protocol '${proto}' — OVN load balancers support tcp, udp and sctp" ;;
    esac
    require_router "${network_id}"

    pub_list="$(expand_ports "${public_port}")"; rc=$?
    [ ${rc} -eq 2 ] && emit_failure "public port range ${public_port} exceeds ovn_pf_max_ports=${PF_MAX_PORTS}; OVN has no port-range DNAT"
    [ ${rc} -ne 0 ] && emit_failure "invalid public port range '${public_port}'"
    priv_list="$(expand_ports "${private_port}")"; rc=$?
    [ ${rc} -eq 2 ] && emit_failure "private port range ${private_port} exceeds ovn_pf_max_ports=${PF_MAX_PORTS}"
    [ ${rc} -ne 0 ] && emit_failure "invalid private port range '${private_port}'"

    set -- ${pub_list}; n=$#
    set -- ${priv_list}
    [ $# -eq ${n} ] || emit_failure "public range ${public_port} and private range ${private_port} differ in size"

    uuid="$(_pf_lb_uuid "${network_id}" "${public_ip}" "${proto}" create)" \
        || emit_failure "failed to create load balancer for ${public_ip}/${proto}"
    [ -n "${uuid}" ] || emit_failure "failed to resolve load balancer for ${public_ip}/${proto}"

    i=1
    for pub in ${pub_list}; do
        priv="$(printf '%s' "${priv_list}" | cut -d' ' -f${i})"
        log "port forward ${public_ip}:${pub} -> ${private_ip}:${priv} (${proto})"
        nbctl set Load_Balancer "${uuid}" \
            "vips:\"${public_ip}:${pub}\"=\"${private_ip}:${priv}\"" >/dev/null \
            || emit_failure "failed to add vip ${public_ip}:${pub}"
        i=$(( i + 1 ))
    done

    nbctl --wait=sb sync >/dev/null || log "warning: --wait=sb sync timed out"
    emit_success
}

cmd_delete_port_forward() {
    local network_id public_ip public_port proto pub_list uuid pub remaining lr
    require_arg network_id network_id
    require_arg public_ip public_ip
    require_arg public_port public_port
    proto="$(printf '%s' "$(arg protocol)" | tr '[:upper:]' '[:lower:]')"
    lr="$(lr_name "${network_id}")"

    uuid="$(_pf_lb_uuid "${network_id}" "${public_ip}" "${proto}" nocreate)"
    if [ -z "${uuid}" ]; then
        log "no load balancer for ${public_ip}/${proto} — nothing to remove"
        emit_success
    fi

    pub_list="$(expand_ports "${public_port}")" || pub_list="${public_port}"
    for pub in ${pub_list}; do
        log "removing vip ${public_ip}:${pub} (${proto})"
        nbctl remove Load_Balancer "${uuid}" vips "\"${public_ip}:${pub}\"" >/dev/null 2>&1 || true
    done

    # Drop the load balancer once its last vip is gone.
    remaining="$(nbctl --bare --columns=vips list Load_Balancer "${uuid}" 2>/dev/null | tr -d '[:space:]')"
    if [ -z "${remaining}" ] || [ "${remaining}" = "{}" ]; then
        log "load balancer ${uuid} has no vips left — destroying it"
        nbctl -- --if-exists lr-lb-del "${lr}" "${uuid}" >/dev/null 2>&1 || true
        nbctl destroy Load_Balancer "${uuid}" >/dev/null 2>&1 || true
    fi

    emit_success
}

cmd_apply_fw_rules() {
    local network_id cidr fw default_allow base_action line
    local rtype proto pstart pend itype icode pubip scidrs dcidrs
    local match proto_match target src_match dst_match action prio
    require_arg network_id network_id

    fw="$(printf '%s' "${PAYLOAD_JSON}" | jq -c '.payload.fw_rules // {}')"
    cidr="$(printf '%s' "${fw}" | jq -r '.cidr // empty')"
    [ -n "${cidr}" ] || cidr="$(arg cidr)"
    [ -n "${cidr}" ] || emit_failure "apply-fw-rules needs the network cidr"
    # NOTE: not `.default_egress_allow // true` — jq's alternative operator treats
    # a literal false as empty, so that idiom silently turns a restrictive egress
    # policy into a permissive one.
    default_allow="$(printf '%s' "${fw}" | jq -r 'if .default_egress_allow == false then "false" else "true" end')"

    log "rebuilding ACLs for network ${network_id} cidr=${cidr} default_egress_allow=${default_allow}"

    # CloudStack sends the complete active rule set every time, so a full rebuild
    # is both correct and the simplest way to stay in sync.
    acl_clear "${network_id}"

    # Let DHCP through in both directions regardless of policy: OVN answers DHCP
    # itself and we must not let a restrictive egress policy break addressing.
    acl_add "${network_id}" from-lport $(( ACL_PRIO_RULE + 100 )) \
        "udp && udp.src == 68 && udp.dst == 67" allow || true
    acl_add "${network_id}" to-lport $(( ACL_PRIO_RULE + 100 )) \
        "udp && udp.src == 67 && udp.dst == 68" allow || true

    # Default egress policy.
    if [ "${default_allow}" = "true" ]; then
        base_action="allow-related"
    else
        base_action="drop"
    fi
    acl_add "${network_id}" from-lport "${ACL_PRIO_BASE}" "ip4.src == ${cidr}" "${base_action}" \
        || emit_failure "failed to apply the default egress policy"

    # Default ingress policy: anything arriving from outside the subnet is dropped
    # unless an explicit rule allows it. Replies to VM-initiated flows still pass,
    # because the egress ACL above is allow-related.
    acl_add "${network_id}" to-lport "${ACL_PRIO_BASE}" \
        "ip4.dst == ${cidr} && ip4.src != ${cidr}" drop \
        || emit_failure "failed to apply the default ingress policy"

    # Fields are joined with US (0x1f), not tab: tab is an IFS *whitespace*
    # character, so bash collapses runs of them and silently shifts every field
    # after an empty one. US is not whitespace, so empty fields survive.
    while IFS=$'\037' read -r rtype proto pstart pend itype icode pubip scidrs dcidrs; do
        [ -n "${rtype}" ] || continue
        proto_match="$(acl_proto_match "${proto}" "${pstart}" "${pend}" "${itype}" "${icode}")"
        prio="${ACL_PRIO_RULE}"

        if [ "${rtype}" = "egress" ]; then
            # Explicit egress rules invert the default policy.
            if [ "${default_allow}" = "true" ]; then action="drop"; else action="allow-related"; fi
            src_match="ip4.src == ${cidr}"
            if [ -n "${scidrs}" ] && [ "${scidrs}" != "0.0.0.0/0" ]; then
                src_match="ip4.src == {${scidrs}}"
            fi
            dst_match=""
            if [ -n "${dcidrs}" ] && [ "${dcidrs}" != "0.0.0.0/0" ]; then
                dst_match="ip4.dst == {${dcidrs}}"
            fi
            match="$(match_join "${src_match}" "${dst_match}" "${proto_match}")"
            acl_add "${network_id}" from-lport "${prio}" "${match}" "${action}" \
                || log "warning: could not apply egress rule (${match})"
        else
            # Ingress rules are always allow rules sitting above the default drop.
            # Resolve the public IP back to the VM behind it so the rule targets
            # that VM rather than the whole subnet.
            target=""
            [ -n "${pubip}" ] && target="$(nat_logical_ip "${network_id}" "${pubip}")"
            if [ -n "${target}" ]; then
                dst_match="ip4.dst == ${target}"
            else
                dst_match="ip4.dst == ${cidr}"
                [ -n "${pubip}" ] && log "no static NAT found for ${pubip}; widening ingress rule to ${cidr}"
            fi
            src_match=""
            if [ -n "${scidrs}" ] && [ "${scidrs}" != "0.0.0.0/0" ]; then
                src_match="ip4.src == {${scidrs}}"
            fi
            match="$(match_join "${src_match}" "${dst_match}" "${proto_match}")"
            acl_add "${network_id}" to-lport "${prio}" "${match}" allow-related \
                || log "warning: could not apply ingress rule (${match})"
        fi
    done <<EOF
$(printf '%s' "${fw}" | jq -r '
    (.rules // [])[] |
    [ .type,
      (.protocol // ""),
      (.portStart // ""),
      (.portEnd // ""),
      (.icmpType // ""),
      (.icmpCode // ""),
      (.publicIp // ""),
      ((.sourceCidrs // []) | join(",")),
      ((.destCidrs // []) | join(",")) ]
    | map(if . == null then "" else tostring end) | join("\u001f")')
EOF

    nbctl --wait=sb sync >/dev/null || log "warning: --wait=sb sync timed out"
    emit_success
}

# ---------------------------------------------------------------------------
# VPC and network ACLs
#
# One logical router per VPC (cs-vpc-rtr-<vpc_id>), shared by every tier. Each
# tier keeps its own logical switch and its own leg on that router, so the
# isolated-network code paths work unchanged — lr_name()/lrp_ext()/ext_lrp_mac()
# switch scope on their own when VPC_ID is set.
# ---------------------------------------------------------------------------

cmd_implement_vpc() {
    local vpc_id vpc_cidr public_ip source_nat public_vlan public_gateway public_cidr lr
    require_arg vpc_id vpc_id
    vpc_cidr="$(arg vpc_cidr)"
    public_ip="$(arg public_ip)"
    source_nat="$(arg source_nat)"
    public_vlan="$(arg public_vlan)"
    public_gateway="$(arg public_gateway)"
    public_cidr="$(arg public_cidr)"
    lr="$(lr_name "")"

    ensure_lr "" || emit_failure "failed to create logical router for VPC ${vpc_id}"

    # CloudStack includes the source NAT IP here only if one is already allocated;
    # otherwise it arrives later via update-vpc-source-nat-ip or assign-ip.
    if [ -n "${public_ip}" ] && [ "${source_nat}" = "true" ]; then
        ensure_external "" "${public_ip}" "${public_vlan}" "${public_gateway}" "${public_cidr}" \
            || emit_failure "failed to configure external connectivity for VPC ${vpc_id}"
        if [ -n "${vpc_cidr}" ]; then
            log "adding vpc-wide snat ${vpc_cidr} -> ${public_ip} on ${lr}"
            nbctl -- --may-exist lr-nat-add "${lr}" snat "${public_ip}" "${vpc_cidr}" >/dev/null \
                || emit_failure "failed to add source NAT ${vpc_cidr} -> ${public_ip}"
        else
            log "no vpc_cidr in payload — skipping the VPC-wide source NAT rule"
        fi
    else
        log "VPC ${vpc_id} implemented without a source NAT IP yet"
    fi

    nbctl --wait=sb sync >/dev/null || log "warning: --wait=sb sync timed out"
    emit_success
}

cmd_shutdown_vpc() {
    local vpc_id lr lsp lrp uuid
    require_arg vpc_id vpc_id
    lr="$(lr_name "")"
    lsp="$(lsp_ext_rtr "")"
    lrp="$(lrp_ext "")"

    # CloudStack calls destroy-network for every tier before this, so only the
    # VPC-wide objects are left.
    for uuid in $(nbctl --bare --columns=_uuid find Load_Balancer \
                      "external_ids:cs-vpc-id=\"${vpc_id}\"" 2>/dev/null); do
        nbctl -- --if-exists lr-lb-del "${lr}" "${uuid}" >/dev/null 2>&1 || true
        nbctl destroy Load_Balancer "${uuid}" >/dev/null 2>&1 || true
    done

    log "shutting down VPC ${vpc_id}: removing ${lsp}, ${lrp} and ${lr}"
    nbctl -- --if-exists lsp-del "${lsp}" >/dev/null 2>&1 || true
    nbctl -- --if-exists lrp-del "${lrp}" >/dev/null 2>&1 || true
    nbctl -- --if-exists lr-del "${lr}" >/dev/null 2>&1 || true
    emit_success
}

cmd_update_vpc_source_nat_ip() {
    local vpc_id vpc_cidr public_ip public_vlan public_gateway public_cidr lr lrp prefix
    require_arg vpc_id vpc_id
    require_arg public_ip public_ip
    vpc_cidr="$(arg vpc_cidr)"
    public_vlan="$(arg public_vlan)"
    public_gateway="$(arg public_gateway)"
    public_cidr="$(arg public_cidr)"
    lr="$(lr_name "")"
    lrp="$(lrp_ext "")"
    prefix="$(prefix_from_cidr "${public_cidr}")"

    ensure_lr "" || emit_failure "failed to ensure logical router for VPC ${vpc_id}"

    # Drop the old VPC-wide SNAT. For type=snat the IP argument is the *logical*
    # (internal) IP, so the VPC CIDR identifies the rule regardless of which
    # external address it used.
    if [ -n "${vpc_cidr}" ]; then
        nbctl -- --if-exists lr-nat-del "${lr}" snat "${vpc_cidr}" >/dev/null 2>&1 || true
    fi

    # Re-point the gateway port at the new address, creating it if this is the
    # first source NAT IP the VPC has had.
    if [ -n "$(nbctl --bare --columns=name find Logical_Router_Port "name=${lrp}")" ] \
       && [ -n "${prefix}" ]; then
        log "re-pointing ${lrp} at ${public_ip}/${prefix}"
        nbctl set Logical_Router_Port "${lrp}" "networks=\"${public_ip}/${prefix}\"" >/dev/null \
            || emit_failure "failed to update ${lrp} to ${public_ip}/${prefix}"
    else
        ensure_external "" "${public_ip}" "${public_vlan}" "${public_gateway}" "${public_cidr}" \
            || emit_failure "failed to configure external connectivity for VPC ${vpc_id}"
    fi

    if [ -n "${vpc_cidr}" ]; then
        log "adding vpc-wide snat ${vpc_cidr} -> ${public_ip} on ${lr}"
        nbctl -- --may-exist lr-nat-add "${lr}" snat "${public_ip}" "${vpc_cidr}" >/dev/null \
            || emit_failure "failed to add source NAT ${vpc_cidr} -> ${public_ip}"
    fi

    nbctl --wait=sb sync >/dev/null || log "warning: --wait=sb sync timed out"
    emit_success
}

cmd_apply_network_acl() {
    local network_id cidr rules count prio
    local number action rtype proto pstart pend itype icode scidrs
    local proto_match src_match dst_match match ovn_action
    require_arg network_id network_id
    cidr="$(arg cidr)"
    [ -n "${cidr}" ] || emit_failure "apply-network-acl needs the tier cidr"

    rules="$(printf '%s' "${PAYLOAD_JSON}" | jq -c '.payload.acl_rules // []')"
    count="$(printf '%s' "${rules}" | jq 'length')"
    log "rebuilding network ACLs for tier ${network_id} cidr=${cidr} rules=${count}"

    acl_clear "${network_id}"

    # DHCP above everything, as for isolated networks.
    acl_add "${network_id}" from-lport $(( ACL_PRIO_VPC_TOP + 100 )) \
        "udp && udp.src == 68 && udp.dst == 67" allow || true
    acl_add "${network_id}" to-lport $(( ACL_PRIO_VPC_TOP + 100 )) \
        "udp && udp.src == 67 && udp.dst == 68" allow || true

    # VPC tiers default to deny in both directions; CloudStack's "default allow"
    # ACL is expressed as explicit allow rules, so an empty list really does mean
    # deny everything.
    acl_add "${network_id}" from-lport "${ACL_PRIO_BASE}" "ip4.src == ${cidr}" drop \
        || emit_failure "failed to apply the default egress policy"
    acl_add "${network_id}" to-lport "${ACL_PRIO_BASE}" \
        "ip4.dst == ${cidr} && ip4.src != ${cidr}" drop \
        || emit_failure "failed to apply the default ingress policy"

    while IFS=$'\037' read -r number action rtype proto pstart pend itype icode scidrs; do
        [ -n "${rtype}" ] || continue

        # CloudStack: lower rule number wins. OVN: higher priority wins.
        prio=$(( ACL_PRIO_VPC_TOP - number ))
        [ "${prio}" -gt "${ACL_PRIO_BASE}" ] || prio=$(( ACL_PRIO_BASE + 1 ))

        case "${action}" in
            allow) ovn_action="allow-related" ;;
            *)     ovn_action="drop" ;;
        esac

        proto_match="$(acl_proto_match "${proto}" "${pstart}" "${pend}" "${itype}" "${icode}")"

        # The VR applies an ingress rule's CIDR as the source and an egress rule's
        # CIDR as the destination (systemvm configure.py: "-s" vs "-d"), so the
        # single sourceCidrs field means different things per direction.
        if [ "${rtype}" = "egress" ]; then
            src_match="ip4.src == ${cidr}"
            dst_match=""
            if [ -n "${scidrs}" ] && [ "${scidrs}" != "0.0.0.0/0" ]; then
                dst_match="ip4.dst == {${scidrs}}"
            fi
            match="$(match_join "${src_match}" "${dst_match}" "${proto_match}")"
            acl_add "${network_id}" from-lport "${prio}" "${match}" "${ovn_action}" \
                || log "warning: could not apply egress ACL rule ${number} (${match})"
        else
            src_match=""
            if [ -n "${scidrs}" ] && [ "${scidrs}" != "0.0.0.0/0" ]; then
                src_match="ip4.src == {${scidrs}}"
            fi
            dst_match="ip4.dst == ${cidr}"
            match="$(match_join "${src_match}" "${dst_match}" "${proto_match}")"
            acl_add "${network_id}" to-lport "${prio}" "${match}" "${ovn_action}" \
                || log "warning: could not apply ingress ACL rule ${number} (${match})"
        fi
    done <<EOF
$(printf '%s' "${rules}" | jq -r '
    .[] |
    [ (.number // 0),
      (.action // "deny"),
      (.trafficType // "ingress"),
      (.protocol // ""),
      (.portStart // ""),
      (.portEnd // ""),
      (.icmpType // ""),
      (.icmpCode // ""),
      ((.sourceCidrs // []) | join(",")) ]
    | map(if . == null then "" else tostring end) | join("\u001f")')
EOF

    nbctl --wait=sb sync >/dev/null || log "warning: --wait=sb sync timed out"
    emit_success
}

# ---------------------------------------------------------------------------
# VPC private gateways and static routes
#
# These need the matching verbs in NetworkExtensionElement; upstream CloudStack
# currently throws UnsupportedOperationException for all four. See the patch in
# ../cloudstack (branch network-extension-vpc-gateways-routes).
# ---------------------------------------------------------------------------

PRIVATE_PHYSNET="$(phys ovn_private_physnet)"
[ -n "${PRIVATE_PHYSNET}" ] || PRIVATE_PHYSNET="${PUBLIC_PHYSNET}"

cmd_create_private_gateway() {
    local gateway_id ip netmask vlan prefix lr ls ln lrp lsp mac
    require_arg gateway_id gateway_id
    require_arg ip gateway_ip_address
    netmask="$(arg gateway_netmask)"
    vlan="$(arg gateway_vlan)"

    prefix="$(prefix_from_netmask "${netmask}")" \
        || emit_failure "invalid netmask '${netmask}' on private gateway ${gateway_id}"
    [ -n "${prefix}" ] || emit_failure "could not derive a prefix from netmask '${netmask}'"

    ensure_lr "" || emit_failure "failed to ensure the VPC router for private gateway ${gateway_id}"

    lr="$(lr_name "")"
    ls="$(pgw_ls_name "${gateway_id}")"
    ln="$(pgw_ln_name "${gateway_id}")"
    lrp="$(pgw_lrp_name "${gateway_id}")"
    lsp="$(pgw_lsp_name "${gateway_id}")"
    mac="$(pgw_lrp_mac "${gateway_id}")"

    log "creating private gateway ${gateway_id}: ${ip}/${prefix} on vlan ${vlan:-untagged}"

    nbctl_or_fail -- --may-exist ls-add "${ls}" \
        -- set Logical_Switch "${ls}" "external_ids:cs-gateway-id=${gateway_id}" \
        -- --may-exist lsp-add "${ls}" "${ln}" \
        -- lsp-set-type "${ln}" localnet \
        -- lsp-set-addresses "${ln}" unknown \
        -- lsp-set-options "${ln}" "network_name=${PRIVATE_PHYSNET}" >/dev/null
    warn_if_duplicated Logical_Switch "${ls}"

    case "${vlan}" in
        ''|untagged|*[!0-9]*) : ;;
        *) nbctl set Logical_Switch_Port "${ln}" "tag_request=${vlan}" >/dev/null \
               || log "warning: could not tag ${ln} with vlan ${vlan}" ;;
    esac

    nbctl_or_fail -- --may-exist lrp-add "${lr}" "${lrp}" "${mac}" "${ip}/${prefix}" \
        -- set Logical_Router_Port "${lrp}" "external_ids:cs-gateway-id=${gateway_id}" \
        -- --may-exist lsp-add "${ls}" "${lsp}" \
        -- lsp-set-type "${lsp}" router \
        -- lsp-set-addresses "${lsp}" router \
        -- lsp-set-options "${lsp}" "router-port=${lrp}" >/dev/null

    nbctl --wait=sb sync >/dev/null || log "warning: --wait=sb sync timed out"
    emit_success
}

cmd_delete_private_gateway() {
    local gateway_id ls lrp
    require_arg gateway_id gateway_id
    ls="$(pgw_ls_name "${gateway_id}")"
    lrp="$(pgw_lrp_name "${gateway_id}")"

    log "deleting private gateway ${gateway_id}"
    acl_clear "${gateway_id}" "${ls}" "cs-gateway-id"
    nbctl -- --if-exists lrp-del "${lrp}" >/dev/null 2>&1 || true
    nbctl_or_fail -- --if-exists ls-del "${ls}" >/dev/null
    emit_success
}

cmd_apply_static_routes() {
    local vpc_id lr routes count uuid cidr next_hop
    require_arg vpc_id vpc_id
    ensure_lr "" || emit_failure "failed to ensure the VPC router for ${vpc_id}"
    lr="$(lr_name "")"

    routes="$(printf '%s' "${PAYLOAD_JSON}" | jq -c '.payload.static_routes // []')"
    count="$(printf '%s' "${routes}" | jq 'length')"
    log "rebuilding ${count} static route(s) on ${lr}"

    # Only routes this script created are cleared. The default route that
    # ensure_external adds with lr-route-add carries no external_ids, so a static
    # route rebuild can never strip the VPC's path to the internet.
    for uuid in $(nbctl --bare --columns=_uuid find Logical_Router_Static_Route \
                      "external_ids:cs-vpc-id=\"${vpc_id}\"" 2>/dev/null); do
        nbctl -- remove Logical_Router "${lr}" static_routes "${uuid}" >/dev/null 2>&1 || true
        nbctl destroy Logical_Router_Static_Route "${uuid}" >/dev/null 2>&1 || true
    done

    while IFS=$'\037' read -r cidr next_hop; do
        [ -n "${cidr}" ] || continue
        if [ -z "${next_hop}" ]; then
            log "skipping static route ${cidr}: no next hop"
            continue
        fi
        # Logical_Router_Static_Route is not a root table either, so the row
        # has to be created and attached in a single transaction (see acl_add).
        nbctl -- --id=@route create Logical_Router_Static_Route \
                    "ip_prefix=${cidr}" "nexthop=${next_hop}" \
                    "external_ids:cs-vpc-id=${vpc_id}" \
              -- add Logical_Router "${lr}" static_routes @route >/dev/null \
            || emit_failure "failed to add static route ${cidr} via ${next_hop} to ${lr}"
        log "static route ${cidr} via ${next_hop}"
    done <<EOF
$(printf '%s' "${routes}" | jq -r '.[] | [ (.cidr // ""), (.next_hop // "") ] | join("\u001f")')
EOF

    nbctl --wait=sb sync >/dev/null || log "warning: --wait=sb sync timed out"
    emit_success
}

cmd_apply_private_gw_acl() {
    local gateway_id ls cidr ip prefix netmask rules count prio
    local number action rtype proto pstart pend itype icode scidrs
    local proto_match src_match dst_match match ovn_action
    require_arg gateway_id gateway_id
    require_arg ip gateway_ip_address
    netmask="$(arg gateway_netmask)"
    ls="$(pgw_ls_name "${gateway_id}")"

    prefix="$(prefix_from_netmask "${netmask}")" || prefix=""
    [ -n "${prefix}" ] || emit_failure "invalid netmask '${netmask}' on private gateway ${gateway_id}"
    cidr="${ip}/${prefix}"

    rules="$(printf '%s' "${PAYLOAD_JSON}" | jq -c '.payload.acl_rules // []')"
    count="$(printf '%s' "${rules}" | jq 'length')"
    log "rebuilding ${count} ACL rule(s) on private gateway ${gateway_id} (${ls})"

    acl_clear "${gateway_id}" "${ls}" "cs-gateway-id"

    acl_add "${gateway_id}" from-lport "${ACL_PRIO_BASE}" "ip4.src == ${cidr}" drop \
        "${ls}" "cs-gateway-id" || emit_failure "failed to apply the default egress policy"
    acl_add "${gateway_id}" to-lport "${ACL_PRIO_BASE}" "ip4.dst == ${cidr}" drop \
        "${ls}" "cs-gateway-id" || emit_failure "failed to apply the default ingress policy"

    while IFS=$'\037' read -r number action rtype proto pstart pend itype icode scidrs; do
        [ -n "${rtype}" ] || continue
        prio=$(( ACL_PRIO_VPC_TOP - number ))
        [ "${prio}" -gt "${ACL_PRIO_BASE}" ] || prio=$(( ACL_PRIO_BASE + 1 ))
        case "${action}" in
            allow) ovn_action="allow-related" ;;
            *)     ovn_action="drop" ;;
        esac
        proto_match="$(acl_proto_match "${proto}" "${pstart}" "${pend}" "${itype}" "${icode}")"

        if [ "${rtype}" = "egress" ]; then
            src_match="ip4.src == ${cidr}"
            dst_match=""
            if [ -n "${scidrs}" ] && [ "${scidrs}" != "0.0.0.0/0" ]; then
                dst_match="ip4.dst == {${scidrs}}"
            fi
            match="$(match_join "${src_match}" "${dst_match}" "${proto_match}")"
            acl_add "${gateway_id}" from-lport "${prio}" "${match}" "${ovn_action}" \
                "${ls}" "cs-gateway-id" \
                || log "warning: could not apply egress ACL rule ${number} (${match})"
        else
            src_match=""
            if [ -n "${scidrs}" ] && [ "${scidrs}" != "0.0.0.0/0" ]; then
                src_match="ip4.src == {${scidrs}}"
            fi
            dst_match="ip4.dst == ${cidr}"
            match="$(match_join "${src_match}" "${dst_match}" "${proto_match}")"
            acl_add "${gateway_id}" to-lport "${prio}" "${match}" "${ovn_action}" \
                "${ls}" "cs-gateway-id" \
                || log "warning: could not apply ingress ACL rule ${number} (${match})"
        fi
    done <<EOF
$(printf '%s' "${rules}" | jq -r '
    .[] |
    [ (.number // 0), (.action // "deny"), (.trafficType // "ingress"),
      (.protocol // ""), (.portStart // ""), (.portEnd // ""),
      (.icmpType // ""), (.icmpCode // ""),
      ((.sourceCidrs // []) | join(",")) ]
    | map(if . == null then "" else tostring end) | join("\u001f")')
EOF

    nbctl --wait=sb sync >/dev/null || log "warning: --wait=sb sync timed out"
    emit_success
}

cmd_unsupported() {
    # CloudStack should only dispatch the verbs backing the services this
    # extension declares. Succeed loudly in the log rather than failing the
    # operation, so an over-broad network offering does not wedge a deployment.
    log "command '${COMMAND}' is not implemented by this extension — returning success"
    emit_success
}

# ---------------------------------------------------------------------------
# Dispatch
#
# Everything below mutates per-network or per-VPC singletons that the NB schema
# cannot keep unique for us, so the whole command runs under one lock.
#
# A VPC's tiers share one logical router, so VPC work locks on the VPC rather
# than the tier: two tiers coming up at once would otherwise both create
# cs-vpc-rtr-<id>. A standalone network locks on itself.
# ---------------------------------------------------------------------------

LOCK_KEY=""
if [ -n "${VPC_ID}" ]; then
    LOCK_KEY="vpc-${VPC_ID}"
elif [ -n "$(arg network_id)" ]; then
    LOCK_KEY="net-$(arg network_id)"
fi
if [ -n "${LOCK_KEY}" ]; then
    trap 'lock_release 9; lock_release 8' EXIT
    lock_acquire 9 "${LOCK_KEY}"
fi

case "${COMMAND}" in
    ensure-network-device) cmd_ensure_network_device ;;
    implement-network)     cmd_implement_network ;;
    prepare-nic)           cmd_prepare_nic ;;
    release-nic)           cmd_release_nic ;;
    shutdown-network)      cmd_shutdown_network ;;
    destroy-network)       cmd_destroy_network ;;

    # Declared-but-delegated DHCP verbs. prepare-nic already does the work and
    # implement-network already created the subnet options, so these reconcile.
    add-dhcp-entry)        cmd_prepare_nic ;;
    remove-dhcp-entry)     emit_success ;;
    config-dhcp-subnet)    cmd_implement_network ;;
    remove-dhcp-subnet)    emit_success ;;

    # L3: NAT, port forwarding, firewall
    assign-ip)             cmd_assign_ip ;;
    release-ip)            cmd_release_ip ;;
    add-static-nat)        cmd_add_static_nat ;;
    delete-static-nat)     cmd_delete_static_nat ;;
    add-port-forward)      cmd_add_port_forward ;;
    delete-port-forward)   cmd_delete_port_forward ;;
    apply-fw-rules)        cmd_apply_fw_rules ;;

    # VPC and network ACLs
    implement-vpc)             cmd_implement_vpc ;;
    shutdown-vpc)              cmd_shutdown_vpc ;;
    update-vpc-source-nat-ip)  cmd_update_vpc_source_nat_ip ;;
    apply-network-acl)         cmd_apply_network_acl ;;

    # VPC private gateways and static routes. These require the companion patch to
    # NetworkExtensionElement; stock CloudStack never dispatches them.
    create-private-gateway)    cmd_create_private_gateway ;;
    delete-private-gateway)    cmd_delete_private_gateway ;;
    apply-static-routes)       cmd_apply_static_routes ;;
    apply-private-gw-acl)      cmd_apply_private_gw_acl ;;

    *)                     cmd_unsupported ;;
esac
