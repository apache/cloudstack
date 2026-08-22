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
# Shared fence-action helper for kvmheartbeat.sh and kvmspheartbeat.sh.
# Sourced by both scripts; do not invoke directly.
#
# Usage from caller:
#   source "$(dirname "$0")/kvmha-fence.sh"
#   fence_action "kvmheartbeat.sh"   # script name passed for log tagging

AGENT_PROPS="${AGENT_PROPS:-/etc/cloudstack/agent/agent.properties}"

fence_action() {
  local source_script="${1:-kvmha}"
  local FENCE_ACTION="hard-reboot"
  local CUSTOM_SCRIPT="/etc/cloudstack/agent/heartbeat-fence-custom.sh"

  if [ -r "$AGENT_PROPS" ]; then
    local val
    val=$(grep -E '^[[:space:]]*kvm\.heartbeat\.fence\.action[[:space:]]*=' "$AGENT_PROPS" | tail -n 1 | cut -d= -f2- | tr -d '[:space:]')
    [ -n "$val" ] && FENCE_ACTION="$val"
    local cval
    cval=$(grep -E '^[[:space:]]*kvm\.heartbeat\.fence\.custom\.script[[:space:]]*=' "$AGENT_PROPS" | tail -n 1 | cut -d= -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
    [ -n "$cval" ] && CUSTOM_SCRIPT="$cval"
  fi

  case "$FENCE_ACTION" in
    log-only)
      /usr/bin/logger -t heartbeat "${source_script}: heartbeat write to storage failed; fence action 'log-only' selected — taking no automatic action. Operator must investigate."
      exit 0
      ;;
    restart-agent)
      /usr/bin/logger -t heartbeat "${source_script}: heartbeat write to storage failed; fence action 'restart-agent' — restarting cloudstack-agent (running VMs preserved)."
      sync &
      sleep 2
      systemctl restart cloudstack-agent
      exit $?
      ;;
    graceful-reboot)
      /usr/bin/logger -t heartbeat "${source_script}: heartbeat write to storage failed; fence action 'graceful-reboot' — rebooting via systemctl (allows running VMs to stop cleanly)."
      sync &
      sleep 5
      systemctl reboot
      exit $?
      ;;
    custom)
      if [ -x "$CUSTOM_SCRIPT" ]; then
        /usr/bin/logger -t heartbeat "${source_script}: heartbeat write to storage failed; fence action 'custom' — running ${CUSTOM_SCRIPT}."
        sync &
        sleep 2
        "$CUSTOM_SCRIPT" "$source_script"
        exit $?
      else
        /usr/bin/logger -t heartbeat "${source_script}: heartbeat write to storage failed; fence action 'custom' selected but ${CUSTOM_SCRIPT} is missing or not executable — falling back to hard-reboot."
        sync &
        sleep 5
        echo b > /proc/sysrq-trigger
        exit $?
      fi
      ;;
    hard-reboot|reboot|*)
      # 'reboot' kept as alias for back-compat with pre-existing deployments.
      /usr/bin/logger -t heartbeat "${source_script} will reboot system because it was unable to write the heartbeat to the storage."
      sync &
      sleep 5
      echo b > /proc/sysrq-trigger
      exit $?
      ;;
  esac
}
