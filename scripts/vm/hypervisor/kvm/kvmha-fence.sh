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
#   syslog and takes no action). Any other value falls back to hard-reboot.
#
# Usage from caller:
#   source "$(dirname "$0")/kvmha-fence.sh"
#   fence_action "kvmheartbeat.sh"   # script name passed for log tagging

AGENT_PROPS="${AGENT_PROPS:-/etc/cloudstack/agent/agent.properties}"

fence_action() {
  local source_script="${1:-kvmha}"
  local FENCE_ACTION="hard-reboot"

  if [ -r "$AGENT_PROPS" ]; then
    local val
    val=$(grep "^kvm.heartbeat.fence.action=" "$AGENT_PROPS" | tail -n 1 | cut -d= -f2- | tr -d '[:space:]')
    [ -n "$val" ] && FENCE_ACTION="$val"
  fi

  case "$FENCE_ACTION" in
    log-only)
      # Only writes to syslog; no reboot, no agent restart and no management server alert.
      /usr/bin/logger -t heartbeat "${source_script}: heartbeat write to storage failed; fence action 'log-only' selected, taking no action. Operator must investigate."
      exit 0
      ;;
    restart-agent)
      /usr/bin/logger -t heartbeat "${source_script}: heartbeat write to storage failed; fence action 'restart-agent', restarting cloudstack-agent (running VMs preserved)."
      sync &
      sleep 2
      systemctl restart cloudstack-agent
      exit $?
      ;;
    graceful-reboot)
      /usr/bin/logger -t heartbeat "${source_script}: heartbeat write to storage failed; fence action 'graceful-reboot', rebooting via systemctl (allows running VMs to stop cleanly)."
      sync &
      sleep 5
      systemctl reboot
      exit $?
      ;;
    hard-reboot|*)
      # Default, and the fallback for any unrecognised value: identical to the pre-existing behaviour.
      /usr/bin/logger -t heartbeat "${source_script} will reboot system because it was unable to write the heartbeat to the storage."
      sync &
      sleep 5
      echo b > /proc/sysrq-trigger
      exit $?
      ;;
  esac
}
