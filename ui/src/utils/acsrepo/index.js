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

const BASE_KUBERNETES_ISO_URL = 'https://download.cloudstack.org/cks/'

function getDefaultLatestKubernetesIsoParams (arch) {
  return {
    name: 'v1.33.1-calico-' + arch,
    semanticversion: '1.33.1',
    url: BASE_KUBERNETES_ISO_URL + 'setup-v1.33.1-calico-' + arch + '.iso',
    arch: arch,
    mincpunumber: 2,
    minmemory: 2048
  }
}

/**
 * Returns the latest Kubernetes ISO info for the given architecture.
 * Falls back to a hardcoded default if fetching fails.
 * @param {string} arch
 * @returns {Promise<{name: string, semanticversion: string, url: string, arch: string}>}
 */
export async function getLatestKubernetesIsoParams (arch) {
  arch = arch || 'x86_64'
  try {
    const html = await fetch(BASE_KUBERNETES_ISO_URL, { cache: 'no-store' }).then(r => r.text())

    const hrefs = [...html.matchAll(/href="([^"]+\.iso)"/gi)].map(m => m[1])

    // Prefer files that explicitly include the arch (e.g. ...-x86_64.iso)
    let isoHrefs = hrefs.filter(h => new RegExp(`${arch}\\.iso$`, 'i').test(h))

    // Fallback: older files without arch suffix (e.g. setup-1.28.4.iso)
    if (isoHrefs.length === 0) {
      isoHrefs = hrefs.filter(h => /setup-\d+\.\d+\.\d+\.iso$/i.test(h))
    }

    const entries = isoHrefs.map(h => {
      const m = h.match(/setup-(?:v)?(\d+\.\d+\.\d+)(?:-calico)?(?:-(x86_64|arm64))?/i)
      return m
        ? {
          name: h.replace('.iso', ''),
          semanticversion: m[1],
          url: new URL(h, BASE_KUBERNETES_ISO_URL).toString(),
          arch: m[2] || arch,
          mincpunumber: 2,
          minmemory: 2048
        }
        : null
    }).filter(Boolean)

    if (entries.length === 0) throw new Error('No matching ISOs found')

    entries.sort((a, b) => {
      const pa = a.semanticversion.split('.').map(Number)
      const pb = b.semanticversion.split('.').map(Number)
      for (let i = 0; i < 3; i++) {
        if ((pb[i] ?? 0) !== (pa[i] ?? 0)) return (pb[i] ?? 0) - (pa[i] ?? 0)
      }
      return 0
    })

    return entries[0]
  } catch {
    return { ...getDefaultLatestKubernetesIsoParams(arch) }
  }
}
