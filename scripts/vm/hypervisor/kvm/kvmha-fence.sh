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
# falls back to hard-reboot. kvm.heartbeat.fence.keep.vms.running=true skips that step,
# which is only safe when no VM on this host can be restarted elsewhere by HA.
#
# Usage from caller:
#   source "$(dirname "$0")/kvmha-fence.sh"
#   fence_action "kvmheartbeat.sh"   # script name passed for log tagging

AGENT_PROPS="${AGENT_PROPS:-/etc/cloudstack/agent/agent.properties}"
FENCE_LOGGER="${FENCE_LOGGER:-/usr/bin/logger}"
FENCE_SYSRQ_TRIGGER="${FENCE_SYSRQ_TRIGGER:-/proc/sysrq-trigger}"

read_agent_property() {
  [ -r "$AGENT_PROPS" ] || return 0
  grep "^$1=" "$AGENT_PROPS" | tail -n 1 | cut -d= -f2- | tr -d '[:space:]'
}

# Stops every active VM on this host. Fails if libvirt cannot be queried or any VM is still
# active afterwards, so the caller never assumes the VMs are stopped when they are not.
stop_all_vms() {
  local source_script="$1"
  local vms vm
  vms=$(virsh -c qemu:///system list --name 2>/dev/null) || return 1
  for vm in $vms; do
    "$FENCE_LOGGER" -t heartbeat "${source_script}: stopping VM ${vm} so it cannot run here while HA starts it on another host."
    virsh -c qemu:///system destroy "$vm" > /dev/null 2>&1
  done
  vms=$(virsh -c qemu:///system list --name 2>/dev/null) || return 1
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
      if [ "$(read_agent_property "kvm.heartbeat.fence.keep.vms.running")" = "true" ]; then
        "$FENCE_LOGGER" -t heartbeat "${source_script}: WARNING fence action '${FENCE_ACTION}' with kvm.heartbeat.fence.keep.vms.running=true leaves the VMs on this host running. If the management server marks this host Down, HA may start the same VMs on another host and both copies would write to the same disks."
      elif ! stop_all_vms "$source_script"; then
        "$FENCE_LOGGER" -t heartbeat "${source_script}: could not stop every VM on this host for fence action '${FENCE_ACTION}'; falling back to hard-reboot so no VM keeps running while HA may start it elsewhere."
        FENCE_ACTION="hard-reboot"
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
