// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

// Shared, module-level cache of the effective (zone-override-or-global) value of the
// multi.network.nic.enabled ConfigKey, keyed by zoneid. Reused by any view that needs to
// gate multi-VLAN trunk NIC UI on whether the feature is actually usable in a given zone
// (e.g. the host list's VLAN-filtering badge, the deploy wizard's associate-network control).
import { reactive } from 'vue'
import { getAPI } from '@/api'

export const multiNetworkNicEnabledZones = reactive({})

// in-flight requests only, keyed by zoneid - dedupes simultaneous callers (e.g. many host rows
// in the same zone on one page) without skipping a fresh re-check on the next page visit
const pendingRequests = {}

export function isMultiNetworkNicEnabledForZone (zoneid) {
  return !!multiNetworkNicEnabledZones[zoneid]
}

export function fetchMultiNetworkNicEnabledForZone (zoneid) {
  if (!zoneid || pendingRequests[zoneid]) {
    return
  }
  pendingRequests[zoneid] = getAPI('listConfigurations', { name: 'multi.network.nic.enabled', zoneid }).then(json => {
    const config = json?.listconfigurationsresponse?.configuration?.[0]
    multiNetworkNicEnabledZones[zoneid] = !!(config && config.value === 'true')
  }).catch(() => {}).finally(() => {
    delete pendingRequests[zoneid]
  })
}

// Flattens a nic's selectable IP targets for rule-creation pickers (Static NAT / Port Forwarding / Load
// Balancing), distinguishing three kinds that are otherwise easy to conflate in the UI:
//  - primary: the nic's own network (nic.networkid / nic.ipaddress)
//  - secondary: another IP on that SAME network (nic.secondaryip[], pre-existing nic_secondary_ips feature)
//  - associated: an IP on a DIFFERENT network this nic trunks to (nic.associatednetworks[], nic_network_map)
// Each option carries the networkid a rule-creation call (enableStaticNat/createPortForwardingRule/
// assignToLoadBalancerRule) must submit alongside its ip - an associated option's networkid differs from
// the nic's own, which is the one real behavioral difference from a secondary IP.
export function buildNicIpOptions (nic) {
  const options = []
  if (nic.ipaddress) {
    options.push({ kind: 'primary', ip: nic.ipaddress, networkid: nic.networkid, networkname: nic.networkname, nicid: nic.id })
  }
  for (const secondary of (nic.secondaryip || [])) {
    options.push({ kind: 'secondary', ip: secondary.ipaddress, networkid: nic.networkid, networkname: nic.networkname, nicid: nic.id })
  }
  for (const association of (nic.associatednetworks || [])) {
    options.push({ kind: 'associated', ip: association.ipaddress, networkid: association.networkid, networkname: association.networkname, nicid: nic.id })
  }
  return options
}

// Groups buildNicIpOptions() output into labeled sections for a picker (e.g. <a-select-opt-group>),
// in a fixed, meaningful order - primary first, then secondary, then associated - omitting any section
// that has nothing to show (the common legacy case has only a primary section).
export function groupNicIpOptionsByKind (options, translate) {
  return [
    { kind: 'primary', label: translate('label.primary') },
    { kind: 'secondary', label: translate('label.secondaryips') },
    { kind: 'associated', label: translate('label.associatednetworks') }
  ]
    .map(({ kind, label }) => ({ kind, label, items: options.filter(item => item.kind === kind) }))
    .filter(group => group.items.length > 0)
}

// Resolves the networkid a rule-creation call (enableStaticNat/createPortForwardingRule/
// assignToLoadBalancerRule) must submit for a chosen ip, falling back to fallbackNetworkId when ip
// doesn't match any option (e.g. options haven't loaded yet) - keeps existing callers' behavior for
// every pre-existing case, only changing the outcome for an associated-network selection.
export function resolveNetworkIdForIp (options, ip, fallbackNetworkId) {
  const match = (options || []).find(item => item.ip === ip)
  return match ? match.networkid : fallbackNetworkId
}
