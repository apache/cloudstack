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
# Shared fence-action helper for kvmheartbeat.sh, kvmspheartbeat.sh and kvmsmpheartbeat.sh.
# Sourced by those scripts; do not invoke directly.
#
# kvm.heartbeat.fence.action in agent.properties selects the action:
#   hard-reboot (default), graceful-reboot, restart-agent, or log-only (only writes to
#   syslog). Any other value falls back to hard-reboot.
#
# restart-agent and log-only leave the host up. Once the heartbeat is stale the management
# server may declare this host Down and HA may start its VMs on another host, so both
# actions first stop every VM still running here; if any VM cannot be stopped the fence
# falls back to hard-reboot. kvm.heartbeat.fence.include.system.vms=false limits that step
# to user VMs: system VMs (virtual routers, console proxy, secondary storage and internal
# LB VMs) are left running and do not count towards the fallback.
# kvm.heartbeat.fence.keep.vms.running=true skips the step entirely and wins over
# include.system.vms; it is only safe when no VM on this host can be restarted elsewhere.
#
# The agent kills this script once heartbeat.update.timeout (60 s by default) runs out, and
# on hung storage stopping a VM can block for much longer than that. So before stopping any
# VM the fence arms a detached deadman that survives the agent killing this script: unless
# every VM the fence must stop is confirmed gone within kvm.heartbeat.fence.stop.vms.timeout
# seconds (default 30, keep it below heartbeat.update.timeout), the deadman hard-reboots the
# host. Each virsh call is also bounded by coreutils timeout, when present, to at most 10 s
# (or the budget if lower) so one stuck domain cannot use up the whole budget.
#
# Properties read from agent.properties:
#   kvm.heartbeat.fence.action              hard-reboot (default), graceful-reboot,
#                                           restart-agent or log-only
#   kvm.heartbeat.fence.keep.vms.running    false (default); true stops nothing
#   kvm.heartbeat.fence.include.system.vms  true (default); false stops only user VMs
#   kvm.heartbeat.fence.stop.vms.timeout    30 (default); seconds the VM stop phase may take
#                                           before the deadman hard-reboots the host. A value
#                                           that is not a positive integer means 30.
#
# Usage from caller:
#   source "$(dirname "$0")/kvmha-fence.sh"
#   fence_action "kvmheartbeat.sh"   # script name passed for log tagging

AGENT_PROPS="${AGENT_PROPS:-/etc/cloudstack/agent/agent.properties}"
FENCE_LOGGER="${FENCE_LOGGER:-/usr/bin/logger}"
FENCE_SYSRQ_TRIGGER="${FENCE_SYSRQ_TRIGGER:-/proc/sysrq-trigger}"
# Where the deadman keeps its arm file; /run is a local tmpfs, so hung storage cannot block it.
FENCE_DEADMAN_DIR="${FENCE_DEADMAN_DIR:-/run/cloudstack-agent/kvmha-fence}"
FENCE_STOP_VMS_TIMEOUT_DEFAULT=30
FENCE_VIRSH_TIMEOUT_MAX=10
# Per-call virsh timeout in seconds; empty means unbounded. Set by fence_action.
FENCE_VIRSH_TIMEOUT=""
FENCE_DEADMAN_ARM=""

read_agent_property() {
  [ -r "$AGENT_PROPS" ] || return 0
  grep "^$1=" "$AGENT_PROPS" | tail -n 1 | cut -d= -f2- | tr -d '[:space:]'
}

# Prints the VM stop budget in seconds from kvm.heartbeat.fence.stop.vms.timeout. Unset gives
# the default; anything else that is not a positive integer (up to 6 digits) gives the default
# with a warning in syslog.
stop_vms_budget() {
  local source_script="$1" val
  val=$(read_agent_property "kvm.heartbeat.fence.stop.vms.timeout")
  if [[ "$val" =~ ^[0-9]{1,6}$ ]] && [ $((10#$val)) -gt 0 ]; then
    echo $((10#$val))
    return 0
  fi
  if [ -n "$val" ]; then
    "$FENCE_LOGGER" -t heartbeat "${source_script}: WARNING kvm.heartbeat.fence.stop.vms.timeout='${val}' is not a positive number of seconds; using the default of ${FENCE_STOP_VMS_TIMEOUT_DEFAULT} s."
  fi
  echo "$FENCE_STOP_VMS_TIMEOUT_DEFAULT"
}

# Runs virsh against the system libvirt, bounded by FENCE_VIRSH_TIMEOUT seconds when coreutils
# timeout is available. Without timeout the call is unbounded and only the deadman covers it.
fence_virsh() {
  if [ -n "$FENCE_VIRSH_TIMEOUT" ] && command -v timeout > /dev/null 2>&1; then
    timeout -k 2 "$FENCE_VIRSH_TIMEOUT" virsh -c qemu:///system "$@"
  else
    virsh -c qemu:///system "$@"
  fi
}

# Body of the deadman; runs in its own detached bash (see arm_deadman), never in the fence
# script itself. Waits up to budget seconds in 1 s steps and exits quietly as soon as the arm
# file is removed. If the arm file is still there when the budget runs out, the VM stop phase
# did not finish in time (or the agent killed the fence script), so it hard-reboots the host.
fence_deadman() {
  local arm="$1" budget="$2" source_script="$3"
  local deadline=$((SECONDS + budget))
  # Survive the agent or a service stop sending TERM or HUP; only disarm_deadman stops it early.
  trap '' HUP INT TERM
  echo "$$" > "${arm}.pid"
  trap 'rm -f "${arm}.pid"' EXIT
  "$FENCE_LOGGER" -t heartbeat "${source_script}: fence deadman (pid $$) armed; the host will be hard-rebooted in ${budget} s unless every VM the fence must stop is confirmed stopped."
  while [ "$SECONDS" -lt "$deadline" ]; do
    [ -e "$arm" ] || exit 0
    sleep 1
  done
  [ -e "$arm" ] || exit 0
  "$FENCE_LOGGER" -t heartbeat "${source_script}: VMs were not confirmed stopped within kvm.heartbeat.fence.stop.vms.timeout (${budget} s); fence deadman is hard-rebooting the host so no VM keeps running while HA may start it elsewhere."
  sync &
  sleep 5
  echo b > "$FENCE_SYSRQ_TRIGGER"
}

# Starts the deadman for a budget of $2 seconds, detached from this script (its own session
# when setsid exists) so it keeps running if the agent kills the fence script on timeout.
# Fails if it cannot be armed.
arm_deadman() {
  local source_script="$1" budget="$2" arm
  # The deadman runs a new bash, so it gets the values it needs passed in explicitly.
  # shellcheck disable=SC2016
  local body='source "$1"; fence_deadman "$2" "$3" "$4"'
  mkdir -p "$FENCE_DEADMAN_DIR" 2> /dev/null || return 1
  arm="${FENCE_DEADMAN_DIR}/armed.$$"
  : > "$arm" 2> /dev/null || return 1
  FENCE_DEADMAN_ARM="$arm"
  if command -v setsid > /dev/null 2>&1; then
    FENCE_LOGGER="$FENCE_LOGGER" FENCE_SYSRQ_TRIGGER="$FENCE_SYSRQ_TRIGGER" \
      setsid nohup /bin/bash -c "$body" fence-deadman "${BASH_SOURCE[0]}" "$arm" "$budget" "$source_script" \
      < /dev/null > /dev/null 2>&1 &
  else
    FENCE_LOGGER="$FENCE_LOGGER" FENCE_SYSRQ_TRIGGER="$FENCE_SYSRQ_TRIGGER" \
      nohup /bin/bash -c "$body" fence-deadman "${BASH_SOURCE[0]}" "$arm" "$budget" "$source_script" \
      < /dev/null > /dev/null 2>&1 &
  fi
  disown 2> /dev/null || true
}

# Stops the deadman: removing the arm file is enough on its own (it checks every second),
# killing it just makes it go away at once.
disarm_deadman() {
  local arm="$FENCE_DEADMAN_ARM" pid
  [ -n "$arm" ] || return 0
  rm -f "$arm"
  pid=$(cat "${arm}.pid" 2> /dev/null)
  if [[ "$pid" =~ ^[0-9]+$ ]]; then
    kill -KILL "$pid" 2> /dev/null
  fi
  rm -f "${arm}.pid"
  FENCE_DEADMAN_ARM=""
}

# True when the libvirt domain name is a CloudStack system VM, using the names the management
# server gives them (com.cloud.vm.VirtualMachineName): r-<id>-<instance>[-<vnet>] for virtual
# routers, and s-, v- and b-<id>-<instance> for secondary storage, console proxy and internal
# LB VMs. User VMs are i-<account>-<id>-<instance> and never match.
is_system_vm() {
  [[ "$1" =~ ^r-[0-9]+-[^-]+(-[^-]+)?$ || "$1" =~ ^[svb]-[0-9]+-[^-]+$ ]]
}

# Prints the active VMs the fence has to stop: every VM, or only user VMs when
# include_system_vms is not "true". Fails if libvirt cannot be queried.
vms_to_stop() {
  local include_system_vms="$1"
  local vms vm
  vms=$(fence_virsh list --name 2>/dev/null) || return 1
  for vm in $vms; do
    if [ "$include_system_vms" = "true" ] || ! is_system_vm "$vm"; then
      echo "$vm"
    fi
  done
}

# Stops the active VMs selected by vms_to_stop. Fails if libvirt cannot be queried or any of
# them is still active afterwards, so the caller never assumes the VMs are stopped when they
# are not. System VMs left running on purpose do not make it fail.
stop_vms() {
  local source_script="$1"
  local include_system_vms="$2"
  local vms vm
  vms=$(vms_to_stop "$include_system_vms") || return 1
  for vm in $vms; do
    "$FENCE_LOGGER" -t heartbeat "${source_script}: stopping VM ${vm} so it cannot run here while HA starts it on another host."
    fence_virsh destroy "$vm" > /dev/null 2>&1
  done
  vms=$(vms_to_stop "$include_system_vms") || return 1
  [ -z "$(echo "$vms" | tr -d '[:space:]')" ]
}

fence_action() {
  local source_script="${1:-kvmha}"
  local FENCE_ACTION="hard-reboot"
  local val
  val=$(read_agent_property "kvm.heartbeat.fence.action")
  [ -n "$val" ] && FENCE_ACTION="$val"

  case "$FENCE_ACTION" in
    log-only|restart-agent)
      local keep_running include_system_vms="true"
      keep_running=$(read_agent_property "kvm.heartbeat.fence.keep.vms.running")
      [ "$(read_agent_property "kvm.heartbeat.fence.include.system.vms")" = "false" ] && include_system_vms="false"
      if [ "$keep_running" = "true" ]; then
        # keep.vms.running is the master opt-out and wins over include.system.vms.
        "$FENCE_LOGGER" -t heartbeat "${source_script}: WARNING fence action '${FENCE_ACTION}' with kvm.heartbeat.fence.keep.vms.running=true leaves every VM on this host running, system VMs included (this overrides kvm.heartbeat.fence.include.system.vms). If the management server marks this host Down, HA may start the same VMs on another host and both copies would write to the same disks."
      else
        if [ "$include_system_vms" = "false" ]; then
          "$FENCE_LOGGER" -t heartbeat "${source_script}: WARNING fence action '${FENCE_ACTION}' with kvm.heartbeat.fence.include.system.vms=false stops only user VMs and leaves system VMs running. If the management server marks this host Down it may start those system VMs on another host, and a virtual router could then run twice with the same IP and MAC addresses."
        fi
        local budget
        budget=$(stop_vms_budget "$source_script")
        FENCE_VIRSH_TIMEOUT=$(( budget < FENCE_VIRSH_TIMEOUT_MAX ? budget : FENCE_VIRSH_TIMEOUT_MAX ))
        "$FENCE_LOGGER" -t heartbeat "${source_script}: stopping VMs for fence action '${FENCE_ACTION}' with a budget of ${budget} s (kvm.heartbeat.fence.stop.vms.timeout) and at most ${FENCE_VIRSH_TIMEOUT} s per virsh call."
        if ! arm_deadman "$source_script" "$budget"; then
          "$FENCE_LOGGER" -t heartbeat "${source_script}: could not arm the fence deadman in ${FENCE_DEADMAN_DIR}; falling back to hard-reboot because the VM stop phase could not be bounded."
          FENCE_ACTION="hard-reboot"
        elif ! stop_vms "$source_script" "$include_system_vms"; then
          # The deadman stays armed: if this script is killed before it reboots, the deadman still does.
          "$FENCE_LOGGER" -t heartbeat "${source_script}: could not stop every VM on this host that the fence must stop for action '${FENCE_ACTION}'; falling back to hard-reboot so no VM keeps running while HA may start it elsewhere."
          FENCE_ACTION="hard-reboot"
        else
          # Every selected VM is confirmed gone, so the host may stay up.
          disarm_deadman
        fi
      fi
      ;;
  esac

  case "$FENCE_ACTION" in
    log-only)
      # No reboot, no agent restart and no management server alert.
      "$FENCE_LOGGER" -t heartbeat "${source_script}: heartbeat write to storage failed; fence action 'log-only' selected, taking no further action. Operator must investigate."
      exit 0
      ;;
    restart-agent)
      "$FENCE_LOGGER" -t heartbeat "${source_script}: heartbeat write to storage failed; fence action 'restart-agent', restarting cloudstack-agent."
      sync &
      sleep 2
      systemctl restart cloudstack-agent
      exit $?
      ;;
    graceful-reboot)
      "$FENCE_LOGGER" -t heartbeat "${source_script}: heartbeat write to storage failed; fence action 'graceful-reboot', rebooting via systemctl (allows running VMs to stop cleanly)."
      sync &
      sleep 5
      systemctl reboot
      exit $?
      ;;
    hard-reboot|*)
      # Default, and the fallback for any unrecognised value: identical to the pre-existing behaviour.
      "$FENCE_LOGGER" -t heartbeat "${source_script} will reboot system because it was unable to write the heartbeat to the storage."
      sync &
      sleep 5
      echo b > "$FENCE_SYSRQ_TRIGGER"
      exit $?
      ;;
  esac
}
