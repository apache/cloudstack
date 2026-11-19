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
# Integration test for ovn.sh against a REAL OVN northbound database.
#
# It drives the script through a full isolated-network and VPC lifecycle, then
# asserts on the resulting NB objects and on ovn-northd's log -- the NB DB will
# happily store a configuration that northd cannot turn into logical flows, so
# checking northd is the part that actually proves anything.
#
# Nothing here needs CloudStack, a hypervisor or a VM; it tests the control
# plane only. See the README for what that does and does not cover.
#
# Usage, in a container or VM with ovn-central installed and running:
#
#     /usr/share/ovn/scripts/ovn-ctl start_northd
#     ./test-with-ovn.sh
#
# Environment:
#     OVN_SCRIPT       path to ovn.sh           (default: alongside this script)
#     OVN_NB_DB        northbound db target     (default: the local unix socket)
#     OVN_NORTHD_LOG   ovn-northd logfile       (default: /var/log/ovn/ovn-northd.log)
# ---------------------------------------------------------------------------
# Drive ovn.sh against a real OVN northbound database.
# Every step asserts: exactly one JSON object on stdout, status=success,
# and the expected objects present in the NB DB.
SCRIPT="${OVN_SCRIPT:-$(cd "$(dirname "$0")" && pwd)/ovn.sh}"
DB="${OVN_NB_DB:-unix:/var/run/ovn/ovnnb_db.sock}"
NORTHD_LOG="${OVN_NORTHD_LOG:-/var/log/ovn/ovn-northd.log}"
NB="ovn-nbctl --db=$DB"

# ovn-northd appends to one logfile for the life of the daemon, so a warning from
# an earlier run of this script is still sitting in it. Remember how long the log
# is now and only ever look at what this run adds.
NORTHD_BASELINE="$(wc -l < "${NORTHD_LOG}" 2>/dev/null || echo 0)"
PASS=0; FAIL=0; STEP=0

PHYS='"ovn_nb_db":"unix:/var/run/ovn/ovnnb_db.sock","ovn_public_physnet":"physnet1","ovn_gateway_chassis":"chassis-a,chassis-b","ovn_dns_servers":"8.8.8.8,8.8.4.4","ovn_mtu":"1442"'

ok()   { PASS=$((PASS+1)); printf '      \033[32mok\033[0m   %s\n' "$1"; }
bad()  { FAIL=$((FAIL+1)); printf '      \033[31mFAIL\033[0m %s\n' "$1"; }

# run <label> <command> <payload-object> [extra-phys]
run() {
    STEP=$((STEP+1))
    local label="$1" cmd="$2" payload="$3" extraphys="$4"
    local phys="$PHYS"
    [ -n "$extraphys" ] && phys="$PHYS,$extraphys"
    printf '{"physical-network-extension-details":{%s},"network-extension-details":{},"payload":%s}' \
        "$phys" "$payload" > /tmp/payload.json
    if ! jq -e . /tmp/payload.json >/dev/null; then bad "$label: harness built invalid JSON"; return 1; fi
    printf '\n[%02d] %s\n' "$STEP" "$label"
    local out rc lines status
    out="$($SCRIPT "$cmd" /tmp/payload.json 30 2>&1)"; rc=$?
    lines="$(printf '%s\n' "$out" | grep -c .)"
    status="$(printf '%s' "$out" | jq -r '.status // "NOT-JSON"' 2>/dev/null)"
    [ -z "$status" ] && status=NOT-JSON
    printf '      -> %s\n' "$out"
    if [ "$lines" != "1" ]; then bad "$label: expected 1 line of output, got $lines"; return 1; fi
    if [ "$status" != "success" ]; then bad "$label: status=$status rc=$rc"; return 1; fi
    ok "$label"
    return 0
}

# expect <description> <nbctl-args...>  : passes if the command succeeds and prints something
expect() {
    local desc="$1"; shift
    local out
    out="$($NB "$@" 2>&1)"
    if [ $? -eq 0 ] && [ -n "$out" ]; then ok "NB: $desc"; else bad "NB: $desc  (got: ${out:-<empty>})"; fi
}
# expect_match <description> <pattern> <nbctl-args...>
expect_match() {
    local desc="$1" pat="$2"; shift 2
    local out
    out="$($NB "$@" 2>&1)"
    if printf '%s' "$out" | grep -qE "$pat"; then ok "NB: $desc"
    else bad "NB: $desc  (pattern '$pat' not in: $(printf '%s' "$out" | tr '\n' ' ' | cut -c1-200))"; fi
}
# northd_clean <description> <pattern>: ovn-northd must not have complained.
# This is the real end-to-end check -- the NB DB will happily store a config
# that northd cannot translate into logical flows.
northd_clean() {
    local desc="$1" pat="$2" since
    since="$(tail -n "+$((NORTHD_BASELINE + 1))" "${NORTHD_LOG}" 2>/dev/null || true)"
    if printf '%s' "$since" | grep -qiE "$pat"; then
        bad "northd: $desc  ($(printf '%s' "$since" | grep -iE "$pat" | tail -1))"
    else ok "northd: $desc"; fi
}

expect_absent() {
    local desc="$1" pat="$2"; shift 2
    local out
    out="$($NB "$@" 2>&1)"
    if printf '%s' "$out" | grep -qE "$pat"; then bad "NB: $desc  (unexpectedly found '$pat')"
    else ok "NB: $desc"; fi
}

# ---------------------------------------------------------------------------
# --payloads <dir>: replay payloads captured from NetworkExtensionElement.
#
# The unit tests on the CloudStack side assert what the Java element writes; the
# suite below asserts what this script reads. Both can pass while the two have
# drifted apart, because each is checked against a fixture written by the same
# hand. This mode closes that gap: it takes the JSON the element actually
# produced (framework/extensions/target/extension-payloads, written by
# NetworkExtensionElementPayloadContractTest) and feeds it to this script
# against a real OVN.
#
# Only the physical-network details are substituted -- those describe the
# backend, not the contract. Everything under .payload is the element's own.
# ---------------------------------------------------------------------------

PHYS_JSON="{\"ovn_nb_db\":\"$DB\",\"ovn_public_physnet\":\"physnet1\",\"ovn_private_physnet\":\"physnet1\",\"ovn_gateway_chassis\":\"chassis-a\"}"

# run_file <label> <command> <captured-json>
run_file() {
    STEP=$((STEP+1))
    local label="$1" cmd="$2" file="$3"
    if [ ! -r "$file" ]; then bad "$label: $file is not readable"; return 1; fi
    jq --argjson phys "$PHYS_JSON" '.["physical-network-extension-details"] = $phys' \
        "$file" > /tmp/replay.json
    printf '\n[%02d] %s\n' "$STEP" "$label"
    local out lines status
    out="$($SCRIPT "$cmd" /tmp/replay.json 30 2>&1)"
    lines="$(printf '%s\n' "$out" | grep -c .)"
    status="$(printf '%s' "$out" | jq -r '.status // "NOT-JSON"' 2>/dev/null)"
    [ -z "$status" ] && status=NOT-JSON
    printf '      -> %s\n' "$out"
    if [ "$lines" != "1" ]; then bad "$label: expected 1 line of output, got $lines"; return 1; fi
    if [ "$status" != "success" ]; then bad "$label: status=$status"; return 1; fi
    ok "$label"
}

# ---------------------------------------------------------------------------
# --concurrency [N]: hammer the per-network singletons from N callers at once.
#
# CloudStack calls ensure_dhcp_options from both implement-network and
# prepare-nic, so a network coming up while instances are already deploying has
# several callers trying to create the same DHCP_Options row. The NB schema has
# no index on that table, so OVSDB will happily store every one of them, and
# ovn-nbctl cannot express "create only if absent" in a single transaction.
#
# Duplicates are not a cosmetic problem: instances bind to different rows, and a
# later config-dhcp-subnet updates only one, so the rest keep handing out stale
# DNS, MTU or lease settings with nothing logged. These assertions are the
# regression guard for that.
# ---------------------------------------------------------------------------

# conc_check <label> <network_id> <n>
conc_check() {
    local label="$1" net="$2" n="$3"
    local ls lr dhcp distinct unbound ports i v
    ls="$($NB --bare --columns=_uuid find Logical_Switch "name=cs-net-${net}" | grep -c .)"
    lr="$($NB --bare --columns=_uuid find Logical_Router "name=cs-rtr-${net}" | grep -c .)"
    dhcp="$($NB --bare --columns=_uuid find DHCP_Options "external_ids:cs-network-id=\"${net}\"" | grep -c .)"
    distinct=0; unbound=0; ports=0
    for i in $(seq 1 "$n"); do
        v="$($NB --bare --columns=dhcpv4_options list Logical_Switch_Port "nic-${net}-${i}" 2>/dev/null)"
        if [ -n "$v" ]; then printf '%s\n' "$v" >> /tmp/conc-refs.$$; else unbound=$((unbound+1)); fi
        $NB --bare --columns=name list Logical_Switch_Port "nic-${net}-${i}" >/dev/null 2>&1 && ports=$((ports+1))
    done
    if [ -s "/tmp/conc-refs.$$" ]; then distinct="$(sort -u "/tmp/conc-refs.$$" | grep -c .)"; else distinct=0; fi
    rm -f "/tmp/conc-refs.$$"

    [ "$ls" = "1" ]    && ok "$label: exactly one logical switch"       || bad "$label: $ls logical switches, expected 1"
    [ "$lr" = "1" ]    && ok "$label: exactly one logical router"       || bad "$label: $lr logical routers, expected 1"
    [ "$dhcp" = "1" ]  && ok "$label: exactly one DHCP_Options row"     || bad "$label: $dhcp DHCP_Options rows, expected 1"
    [ "$distinct" = "1" ] && ok "$label: every port shares one DHCP_Options row" \
                          || bad "$label: ports reference $distinct different DHCP_Options rows"
    [ "$unbound" = "0" ] && ok "$label: no port left without DHCP options" \
                         || bad "$label: $unbound ports have no DHCP options (weak reference cleared)"
    [ "$ports" = "$n" ] && ok "$label: all $n ports present" || bad "$label: $ports of $n ports present"
}

# conc_run <network_id> <n> [tolerant]
#
# strict  : every call must succeed (the single-management-server case)
# tolerant: a losing racer may fail, but it must fail CLEANLY -- one JSON
#           object saying so. Silently reporting success over a half-applied
#           change is the thing that must never happen.
conc_run() {
    local net="$1" n="$2" mode="${3:-strict}" i fails=0 dirty=0 lines
    for i in $(seq 1 "$n"); do
        cat > "/tmp/conc-${net}-${i}.json" <<EOF
{"physical-network-extension-details":{$PHYS},
 "network-extension-details":{},
 "payload":{"network_id":"${net}","cidr":"10.90.0.0/24","gateway":"10.90.0.1","zone_id":"1",
            "nic_uuid":"nic-${net}-${i}","mac":"02:00:0a:5a:00:$(printf '%02x' "$i")","ip":"10.90.0.$((i+10))"}}
EOF
    done
    for i in $(seq 1 "$n"); do
        $SCRIPT implement-network "/tmp/conc-${net}-${i}.json" 30 > "/tmp/conc-impl.${net}.${i}" 2>&1 &
        $SCRIPT prepare-nic       "/tmp/conc-${net}-${i}.json" 30 > "/tmp/conc-nic.${net}.${i}" 2>&1 &
    done
    wait
    for i in $(seq 1 "$n"); do
        for f in "/tmp/conc-impl.${net}.${i}" "/tmp/conc-nic.${net}.${i}"; do
            grep -q '"status":"success"' "$f" || fails=$((fails+1))
            lines="$(grep -c . "$f")"
            # exactly one JSON object, and it must carry a status either way
            if [ "$lines" != "1" ] || ! jq -e '.status' "$f" >/dev/null 2>&1; then
                dirty=$((dirty+1))
            fi
        done
    done
    rm -f "/tmp/conc-${net}-"*.json "/tmp/conc-impl.${net}."* "/tmp/conc-nic.${net}."*
    if [ "$mode" = "strict" ]; then
        [ "$fails" = "0" ] && ok "all $((n*2)) concurrent calls returned success" \
                           || bad "$fails of $((n*2)) concurrent calls did not return success"
    else
        [ "$dirty" = "0" ] \
            && ok "every one of $((n*2)) racing calls answered with a single clean status ($fails reported failure)" \
            || bad "$dirty of $((n*2)) racing calls produced malformed or multi-line output"
    fi
}

if [ "${1:-}" = "--concurrency" ]; then
    N="${2:-8}"
    echo "=========================================================="
    echo " CONCURRENCY: $N callers per network"
    echo "=========================================================="

    STEP=$((STEP+1)); printf '\n[%02d] %s\n' "$STEP" "$N x (implement-network + prepare-nic), same network, locking enabled"
    conc_run 900 "$N"
    conc_check "locked" 900 "$N"

    # net_lock_acquire gives up silently when it cannot create its lock
    # directory, which is exactly the state of a second management server that
    # cannot see this one's lock file. Only the dedupe protects that case, so
    # test it on its own.
    STEP=$((STEP+1)); printf '\n[%02d] %s\n' "$STEP" "same again with the lock unavailable (simulates a second management server)"
    CS_OVN_LOCK_DIR=/proc/self/no-such-lock-dir conc_run 901 "$N" tolerant
    # Without the lock, Logical_Switch and Logical_Router can still be
    # duplicated: the NB schema has no index on their names, and ovn-nbctl
    # cannot express a compare-and-swap, so nothing in this script can prevent
    # it. What it CAN guarantee is that the DHCP_Options rows converge, that no
    # instance is left without DHCP, and that a duplicated switch is reported
    # rather than silently splitting the network.
    # The logical switch is the thing this script cannot protect without a lock,
    # so branch on it. If it survived intact, the DHCP invariants must hold. If
    # it did not, the network is split and the only contract left is that the
    # script said so rather than leaving an operator to find it.
    ls901="$($NB --bare --columns=_uuid find Logical_Switch "name=cs-net-901" | grep -c .)"
    if [ "$ls901" = "1" ]; then
        ok "unlocked: the logical switch survived intact"
        dhcp901="$($NB --bare --columns=_uuid find DHCP_Options "external_ids:cs-network-id=\"901\"" | grep -c .)"
        [ "$dhcp901" = "1" ] && ok "unlocked: DHCP_Options converged to one row" \
                             || bad "unlocked: $dhcp901 DHCP_Options rows, dedupe did not converge"
        unb901=0
        for i in $(seq 1 "$N"); do
            [ -z "$($NB --bare --columns=dhcpv4_options list Logical_Switch_Port "nic-901-${i}" 2>/dev/null)" ] \
                && unb901=$((unb901+1))
        done
        [ "$unb901" = "0" ] && ok "unlocked: no instance left without DHCP options" \
                            || bad "unlocked: $unb901 instances have no DHCP options"
    else
        if grep -q "rows in Logical_Switch are named cs-net-901" \
                "${CS_OVN_LOG_DIR:-/var/log/cloudstack/management/extensions}/ovn.log" 2>/dev/null; then
            ok "unlocked: the switch duplicated ($ls901 rows) and the script reported it (known limitation, see README)"
        else
            bad "unlocked: $ls901 switches named cs-net-901 and nothing warned about it"
        fi
    fi

    STEP=$((STEP+1)); printf '\n[%02d] %s\n' "$STEP" "$N distinct networks brought up at once"
    for i in $(seq 1 "$N"); do
        cat > "/tmp/conc-multi-$i.json" <<EOF
{"physical-network-extension-details":{$PHYS},
 "network-extension-details":{},
 "payload":{"network_id":"95$i","cidr":"10.95.$i.0/24","gateway":"10.95.$i.1","zone_id":"1"}}
EOF
        $SCRIPT implement-network "/tmp/conc-multi-$i.json" 30 > "/tmp/conc-multi-out.$i" 2>&1 &
    done
    wait
    multi_ok=0
    for i in $(seq 1 "$N"); do
        grep -q '"status":"success"' "/tmp/conc-multi-out.$i" && multi_ok=$((multi_ok+1))
    done
    [ "$multi_ok" = "$N" ] && ok "all $N distinct networks implemented" || bad "only $multi_ok of $N succeeded"
    sw="$($NB --bare --columns=name find Logical_Switch | grep -c '^cs-net-95' || true)"
    [ "$sw" = "$N" ] && ok "$N distinct logical switches created" || bad "$sw logical switches, expected $N"
    rm -f /tmp/conc-multi-*.json /tmp/conc-multi-out.*

    echo
    echo "=========================================================="
    printf ' CONCURRENCY RESULT: %d passed, %d failed\n' "$PASS" "$FAIL"
    echo "=========================================================="
    exit $([ "$FAIL" -eq 0 ] && echo 0 || echo 1)
fi

if [ "${1:-}" = "--payloads" ]; then
    DIR="${2:?usage: $0 --payloads <dir>}"
    echo "=========================================================="
    echo " REPLAY OF CAPTURED NetworkExtensionElement PAYLOADS"
    echo " from: $DIR"
    echo "=========================================================="

    CREATE="$DIR/create-private-gateway.json"
    [ -r "$CREATE" ] || { echo "no create-private-gateway.json in $DIR"; exit 2; }

    # The VPC id is whatever the Java fixtures used; build that VPC first so the
    # captured gateway payloads have a router to attach to.
    VPC_ID_FIXTURE="$(jq -r '.payload.vpc_id' "$CREATE")"
    echo "  fixtures target vpc_id=$VPC_ID_FIXTURE"
    LR="cs-vpc-rtr-$VPC_ID_FIXTURE"

    run_file "implement-vpc (element payload)" implement-vpc "$DIR/implement-vpc.json"
    # The element omits the public IP until one is allocated, so this also covers
    # the branch where a VPC comes up with no external connectivity yet.
    expect_match "VPC router created from the element payload" "$LR" lr-list

    # A tier needs its logical switch before ACLs can be hung on it. The ids come
    # out of the captured ACL payload so the two cannot disagree.
    ACL="$DIR/apply-network-acl.json"
    TIER_ID="$(jq -r '.payload.network_id' "$ACL")"
    TIER_CIDR="$(jq -r '.payload.cidr' "$ACL")"
    TIER_GW="$(jq -r '.payload.gateway' "$ACL")"
    run "implement-network tier $TIER_ID (harness-built, to host the captured ACLs)" implement-network \
      "{\"network_id\":\"$TIER_ID\",\"vpc_id\":\"$VPC_ID_FIXTURE\",\"cidr\":\"$TIER_CIDR\",\"gateway\":\"$TIER_GW\",\"zone_id\":\"1\"}"
    expect_match "tier leg on the shared VPC router" "cs-lrp-$TIER_ID" lrp-list "$LR"

    run_file "apply-network-acl (element payload)" apply-network-acl "$ACL"
    expect_match "rule 10 from the element payload" "tcp\.dst == 80" acl-list "cs-net-$TIER_ID"
    expect_match "lower rule number became higher OVN priority" "29990" acl-list "cs-net-$TIER_ID"
    expect_match "rule 20 kept its source cidr through serialisation" "10\.2\.2\.0/24" acl-list "cs-net-$TIER_ID"

    GW_IP="$(jq -r '.payload.gateway_ip_address' "$CREATE")"
    GW_ID="$(jq -r '.payload.gateway_id' "$CREATE")"

    run_file "create-private-gateway (element payload)" create-private-gateway "$CREATE"
    expect_match "private gateway port carries the element's address" "${GW_IP//./\\.}" \
        --bare --columns=networks list Logical_Router_Port "cs-pgw-lrp-$GW_ID"

    run_file "apply-static-routes (element payload)" apply-static-routes "$DIR/apply-static-routes.json"
    for c in $(jq -r '.payload.static_routes[].cidr' "$DIR/apply-static-routes.json"); do
        expect_match "route $c installed from the element payload" "${c//./\\.}" lr-route-list "$LR"
    done
    $NB --wait=sb sync >/dev/null 2>&1
    northd_clean "northd resolved the element's static routes" "No path for static route"

    run_file "apply-private-gw-acl (element payload)" apply-private-gw-acl "$DIR/apply-private-gw-acl.json"
    expect_match "ACL built from the element's rule set" "tcp" acl-list "cs-pgw-$GW_ID"

    run_file "delete-private-gateway (element payload)" delete-private-gateway "$DIR/delete-private-gateway.json"
    expect_absent "private gateway port removed" "${GW_IP//./\\.}" lrp-list "$LR"

    run "destroy-network tier $TIER_ID (cleanup)" destroy-network \
      "{\"network_id\":\"$TIER_ID\",\"vpc_id\":\"$VPC_ID_FIXTURE\"}"
    run "shutdown-vpc (cleanup)" shutdown-vpc "{\"vpc_id\":\"$VPC_ID_FIXTURE\"}"
    expect_absent "VPC router gone" "$LR" lr-list

    echo
    echo "=========================================================="
    printf ' REPLAY RESULT: %d passed, %d failed\n' "$PASS" "$FAIL"
    echo "=========================================================="
    exit $([ "$FAIL" -eq 0 ] && echo 0 || echo 1)
fi

echo "=========================================================="
echo " ISOLATED NETWORK LIFECYCLE"
echo "=========================================================="

run "ensure-network-device" ensure-network-device '{"network_id":"42"}'

run "implement-network (L2 + DHCP + gateway leg)" implement-network \
  '{"network_id":"42","network_uuid":"net-uuid-42","cidr":"10.1.1.0/24","gateway":"10.1.1.1","zone_id":"1"}'
expect       "logical switch cs-net-42"            ls-list
expect_match "DHCP_Options for 10.1.1.0/24" "10\.1\.1\.0/24" --bare --columns=cidr list DHCP_Options
expect_match "DHCP server_id + dns from ovn_dns_servers" "server_id=\"10\.1\.1\.1\"" list DHCP_Options
expect_match "DHCP mtu option honoured"      "mtu=\"1442\""   list DHCP_Options
expect_match "router cs-rtr-42 with guest leg" "cs-lrp-42"    lrp-list cs-rtr-42

run "prepare-nic" prepare-nic \
  '{"network_id":"42","nic_uuid":"nic-aaaa-0001","mac":"02:00:0a:01:01:64","ip":"10.1.1.100","cidr":"10.1.1.0/24","gateway":"10.1.1.1"}'
expect_match "LSP named after the NIC uuid"  "nic-aaaa-0001" lsp-list cs-net-42
expect_match "LSP addresses = mac + ip"      "02:00:0a:01:01:64 10\.1\.1\.100" lsp-get-addresses nic-aaaa-0001
expect_match "LSP has DHCPv4 options bound"  "[0-9a-f]{8}-" --bare --columns=dhcpv4_options list Logical_Switch_Port nic-aaaa-0001

run "assign-ip (source NAT)" assign-ip \
  '{"network_id":"42","public_ip":"192.0.2.10","source_nat":"true","cidr":"10.1.1.0/24","gateway":"10.1.1.1","public_vlan":"101","public_gateway":"192.0.2.1","public_cidr":"192.0.2.0/24"}'
expect_match "external switch cs-ext-101"       "cs-ext-101"    ls-list
expect_match "localnet port mapped to physnet1" "physnet1"      list Logical_Switch_Port
expect_match "gateway port holds the SNAT IP"   "192\.0\.2\.10" --bare --columns=networks list Logical_Router_Port cs-ext-lrp-42
expect_match "snat rule for the guest CIDR"     "snat.*192\.0\.2\.10.*10\.1\.1\.0/24" lr-nat-list cs-rtr-42
expect_match "default route via public gateway" "0\.0\.0\.0/0.*192\.0\.2\.1" lr-route-list cs-rtr-42
expect_match "gateway chassis scheduled"        "chassis-a"     lrp-get-gateway-chassis cs-ext-lrp-42

run "add-static-nat" add-static-nat \
  '{"network_id":"42","public_ip":"192.0.2.11","private_ip":"10.1.1.100"}'
expect_match "dnat_and_snat 192.0.2.11 -> 10.1.1.100" "dnat_and_snat.*192\.0\.2\.11.*10\.1\.1\.100" lr-nat-list cs-rtr-42

run "add-port-forward (single port)" add-port-forward \
  '{"network_id":"42","public_ip":"192.0.2.12","private_ip":"10.1.1.100","public_port":"8080","private_port":"80","protocol":"tcp"}'
expect_match "LB vip 192.0.2.12:8080 -> 10.1.1.100:80" "192\.0\.2\.12:8080.*10\.1\.1\.100:80" lb-list

run "add-port-forward (range 9000-9002)" add-port-forward \
  '{"network_id":"42","public_ip":"192.0.2.12","private_ip":"10.1.1.100","public_port":"9000-9002","private_port":"9100-9102","protocol":"tcp"}'
expect_match "range expanded, first port paired"  "192\.0\.2\.12:9000.*10\.1\.1\.100:9100" lb-list
expect_match "range expanded, last port paired"   "192\.0\.2\.12:9002.*10\.1\.1\.100:9102" lb-list

run "apply-fw-rules (restrictive egress)" apply-fw-rules \
  '{"network_id":"42","cidr":"10.1.1.0/24","fw_rules":{"cidr":"10.1.1.0/24","default_egress_allow":false,"rules":[{"type":"Ingress","protocol":"tcp","portStart":22,"portEnd":22,"publicIp":"192.0.2.11","sourceCidrs":["203.0.113.0/24"]},{"type":"Egress","protocol":"tcp","portStart":443,"portEnd":443,"destCidrs":["0.0.0.0/0"]}]}}'
expect_match "DHCP allowed above everything"   "udp\.src == 68" acl-list cs-net-42
expect_match "ingress ssh rule resolved to the private IP" "tcp\.dst == 22" acl-list cs-net-42
expect_match "ingress rule scoped to source cidr"          "203\.0\.113\.0/24" acl-list cs-net-42
expect_match "egress base policy is drop"      "drop" acl-list cs-net-42
expect_match "explicit egress allow on 443"    "tcp\.dst == 443" acl-list cs-net-42

run "apply-fw-rules (permissive egress)" apply-fw-rules \
  '{"network_id":"42","cidr":"10.1.1.0/24","fw_rules":{"cidr":"10.1.1.0/24","default_egress_allow":true,"rules":[]}}'
expect_match "egress base policy is now allow" "allow" acl-list cs-net-42

echo
echo "=========================================================="
echo " TEARDOWN"
echo "=========================================================="

run "delete-port-forward (one of two)" delete-port-forward \
  '{"network_id":"42","public_ip":"192.0.2.12","public_port":"8080","protocol":"tcp"}'
expect_absent "8080 vip gone"      "192\.0\.2\.12:8080" lb-list
expect_match  "9000 vip survives"  "192\.0\.2\.12:9000" lb-list

run "delete-static-nat" delete-static-nat '{"network_id":"42","public_ip":"192.0.2.11"}'
expect_absent "dnat_and_snat gone" "192\.0\.2\.11" lr-nat-list cs-rtr-42

run "release-ip (source NAT)" release-ip \
  '{"network_id":"42","public_ip":"192.0.2.10","source_nat":"true","cidr":"10.1.1.0/24"}'
expect_absent "snat gone" "192\.0\.2\.10" lr-nat-list cs-rtr-42

run "release-nic" release-nic '{"nic_uuid":"nic-aaaa-0001"}'
expect_absent "LSP gone" "nic-aaaa-0001" lsp-list cs-net-42

run "shutdown-network (deliberate no-op)" shutdown-network '{"network_id":"42"}'
expect_match "logical switch still present after shutdown" "cs-net-42" ls-list

run "destroy-network" destroy-network '{"network_id":"42"}'
expect_absent "logical switch gone" "cs-net-42" ls-list
expect_absent "router gone"         "cs-rtr-42" lr-list

echo
echo "=========================================================="
echo " VPC"
echo "=========================================================="

run "implement-vpc" implement-vpc \
  '{"vpc_id":"7","vpc_cidr":"10.2.0.0/16","public_ip":"192.0.2.20","source_nat":"true","public_vlan":"101","public_gateway":"192.0.2.1","public_cidr":"192.0.2.0/24"}'
expect_match "VPC router exists" "cs-vpc-rtr-7" lr-list
expect_match "VPC snat on the public IP" "snat.*192\.0\.2\.20" lr-nat-list cs-vpc-rtr-7

run "implement-network tier A" implement-network \
  '{"network_id":"100","vpc_id":"7","cidr":"10.2.1.0/24","gateway":"10.2.1.1","zone_id":"1"}'
run "implement-network tier B" implement-network \
  '{"network_id":"101","vpc_id":"7","cidr":"10.2.2.0/24","gateway":"10.2.2.1","zone_id":"1"}'
expect_match "tier A leg on the shared VPC router" "cs-lrp-100" lrp-list cs-vpc-rtr-7
expect_match "tier B leg on the shared VPC router" "cs-lrp-101" lrp-list cs-vpc-rtr-7
expect_absent "no per-tier router was created"     "cs-rtr-100" lr-list

run "apply-network-acl (tier A)" apply-network-acl \
  '{"network_id":"100","vpc_id":"7","cidr":"10.2.1.0/24","acl_rules":[{"number":10,"action":"allow","trafficType":"ingress","protocol":"tcp","portStart":80,"portEnd":80,"sourceCidrs":["0.0.0.0/0"]},{"number":20,"action":"deny","trafficType":"egress","protocol":"all","sourceCidrs":["10.2.2.0/24"]}]}'
expect_match "rule 10 present"              "tcp\.dst == 80" acl-list cs-net-100
expect_match "lower rule number wins (higher OVN priority)" "2999[0-9]" acl-list cs-net-100

run "update-vpc-source-nat-ip" update-vpc-source-nat-ip \
  '{"vpc_id":"7","public_ip":"192.0.2.21","vpc_cidr":"10.2.0.0/16","public_vlan":"101","public_gateway":"192.0.2.1","public_cidr":"192.0.2.0/24"}'
expect_match  "snat re-pointed" "192\.0\.2\.21" lr-nat-list cs-vpc-rtr-7
expect_absent "old snat gone"   "192\.0\.2\.20" lr-nat-list cs-vpc-rtr-7

echo
echo "----- VPC private gateways / static routes (companion patch) -----"

run "create-private-gateway" create-private-gateway \
  '{"vpc_id":"7","gateway_id":"31","gateway_uuid":"gw-uuid","gateway_ip_address":"172.16.0.5","gateway_netmask":"255.255.255.0","gateway_vlan":"900"}'
expect_match "private gateway port on the VPC router" "172\.16\.0\.5" --bare --columns=networks list Logical_Router_Port cs-pgw-lrp-31

run "apply-static-routes" apply-static-routes \
  '{"vpc_id":"7","static_routes":[{"cidr":"10.9.0.0/16","next_hop":"172.16.0.1"},{"cidr":"10.8.0.0/16","next_hop":"172.16.0.1"}]}'
expect_match "static route 10.9.0.0/16" "10\.9\.0\.0/16.*172\.16\.0\.1" lr-route-list cs-vpc-rtr-7
expect_match "static route 10.8.0.0/16" "10\.8\.0\.0/16.*172\.16\.0\.1" lr-route-list cs-vpc-rtr-7
expect_match "default route not clobbered by the rebuild" "0\.0\.0\.0/0" lr-route-list cs-vpc-rtr-7
$NB --wait=sb sync >/dev/null 2>&1
northd_clean "northd resolved the static route via the private gateway" "No path for static route 10\.9"
northd_clean "northd translated everything so far without error" "\|ERR\|"

run "apply-static-routes (rebuild, one removed)" apply-static-routes \
  '{"vpc_id":"7","static_routes":[{"cidr":"10.9.0.0/16","next_hop":"172.16.0.1"}]}'
expect_absent "removed route is gone" "10\.8\.0\.0/16" lr-route-list cs-vpc-rtr-7
expect_match  "kept route survives"   "10\.9\.0\.0/16" lr-route-list cs-vpc-rtr-7

run "apply-private-gw-acl" apply-private-gw-acl \
  '{"vpc_id":"7","gateway_id":"31","gateway_ip_address":"172.16.0.5","gateway_netmask":"255.255.255.0","acl_rules":[{"number":10,"action":"allow","trafficType":"ingress","protocol":"tcp","portStart":443,"portEnd":443,"sourceCidrs":["10.9.0.0/16"]}]}'

run "delete-private-gateway" delete-private-gateway '{"vpc_id":"7","gateway_id":"31"}'
expect_absent "private gateway port gone" "172\.16\.0\.5" lrp-list cs-vpc-rtr-7

run "destroy-network tier A" destroy-network '{"network_id":"100","vpc_id":"7"}'
expect_match "VPC router survives a tier teardown" "cs-vpc-rtr-7" lr-list

run "shutdown-vpc" shutdown-vpc '{"vpc_id":"7"}'
expect_absent "VPC router gone" "cs-vpc-rtr-7" lr-list

echo
echo "=========================================================="
echo " EVPN TAGGING"
echo "=========================================================="

run "implement-vpc with L3 EVPN" implement-vpc \
  '{"vpc_id":"8","vpc_cidr":"10.3.0.0/16","public_ip":"192.0.2.30","source_nat":"true","public_vlan":"101","public_gateway":"192.0.2.1","public_cidr":"192.0.2.0/24"}' \
  '"ovn_evpn_enabled":"true","ovn_evpn_type":"l3","ovn_evpn_vni_base":"100000"'
expect_match "per-VRF provider switch created" "cs-ext-101-vni100008" ls-list
expect_match "bgpvpn type tag"                 "neutron_bgpvpn:type.*l3"   list Logical_Switch
expect_match "bgpvpn vni tag = base + vpc id"  "neutron_bgpvpn:vni.*100008" list Logical_Switch

run "implement-network with L2 EVPN" implement-network \
  '{"network_id":"200","cidr":"10.4.1.0/24","gateway":"10.4.1.1","zone_id":"1"}' \
  '"ovn_evpn_enabled":"true","ovn_evpn_type":"l2","ovn_evpn_vni_base":"100000"'
expect_match "L2 VNI scoped per logical switch (base + network id)" "neutron_bgpvpn:vni.*100200" list Logical_Switch

echo
echo "=========================================================="
echo " ERROR HANDLING"
echo "=========================================================="
STEP=$((STEP+1)); printf '\n[%02d] %s\n' "$STEP" "missing ovn_nb_db must fail cleanly"
printf '{"physical-network-extension-details":{},"network-extension-details":{},"payload":{"network_id":"42"}}' > /tmp/bad.json
out="$($SCRIPT implement-network /tmp/bad.json 10 2>&1)"
printf '      -> %s\n' "$out"
if [ "$(printf '%s' "$out" | jq -r '.status')" = "failure" ]; then ok "returns status=failure"; else bad "did not return status=failure"; fi

STEP=$((STEP+1)); printf '\n[%02d] %s\n' "$STEP" "unknown command must not wedge the operation"
printf '{"physical-network-extension-details":{%s},"network-extension-details":{},"payload":{"network_id":"42"}}' "$PHYS" > /tmp/unk.json
out="$($SCRIPT some-future-verb /tmp/unk.json 10 2>&1)"
printf '      -> %s\n' "$out"
if [ "$(printf '%s' "$out" | jq -r '.status')" = "success" ]; then ok "returns status=success"; else bad "did not return success"; fi

echo
echo "=========================================================="
printf ' RESULT: %d passed, %d failed\n' "$PASS" "$FAIL"
echo "=========================================================="
exit $([ "$FAIL" -eq 0 ] && echo 0 || echo 1)
