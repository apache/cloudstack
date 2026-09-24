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

export const formatKvmConversionHostName = (host, migrationMode, yes, no) => {
  const details = host.details || {}
  const isSupported = value => value === true || value === 'true'
  const hostParts = [host.name]
  const capabilityParts = []

  if (host.clustername) {
    hostParts.push(`[${host.clustername}]`)
  }

  const virtV2vVersion = details['host.virtv2v.version']
  capabilityParts.push(`virt-v2v=${virtV2vVersion || (isSupported(host.instanceconversionsupported) ? yes : no)}`)

  if (migrationMode === 'cbt' || migrationMode === 'vddk') {
    const vddkSupported = isSupported(details['host.vddk.support'])
    const vddkVersion = details['host.vddk.version']
    capabilityParts.push(`VDDK=${vddkSupported ? (vddkVersion || yes) : no}`)
  }
  if (migrationMode === 'cbt') {
    capabilityParts.push(`CBT=${isSupported(details['host.vddk.blockcopy.support']) ? yes : no}`)
  }

  return `${hostParts.join(' ')} — ${capabilityParts.join(' / ')}`
}
