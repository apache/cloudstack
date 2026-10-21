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
  vms=$(virsh -c qemu:///system list --name 2>/dev/null) || return 1
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
    virsh -c qemu:///system destroy "$vm" > /dev/null 2>&1
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
        if ! stop_vms "$source_script" "$include_system_vms"; then
          "$FENCE_LOGGER" -t heartbeat "${source_script}: could not stop every VM on this host that the fence must stop for action '${FENCE_ACTION}'; falling back to hard-reboot so no VM keeps running while HA may start it elsewhere."
          FENCE_ACTION="hard-reboot"
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
