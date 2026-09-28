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

import { formatKvmConversionHostName } from '@/utils/vmware'

const host = {
  name: 'qa1-vmwarelab-576a1b2a-mix-host2',
  podname: 'Pod',
  clustername: 'KVM-Cluster',
  instanceconversionsupported: true,
  details: {
    'host.virtv2v.version': '2.7.1rhel=9',
    'host.vddk.support': 'true',
    'host.vddk.version': '8',
    'host.vddk.blockcopy.support': 'true',
    'host.qemu.img.version': '7.0.0',
    'host.qemu.nbd.version': 'qemu-nbd 7.0.0 (qemu-kvm-7.0.0-13.el9)'
  }
}

describe('utils/vmware', () => {
  describe('formatKvmConversionHostName()', () => {
    it('shows a concise CBT conversion host label', () => {
      expect(formatKvmConversionHostName(host, 'cbt', 'Yes', 'No')).toBe(
        'qa1-vmwarelab-576a1b2a-mix-host2 [KVM-Cluster] — virt-v2v=2.7.1rhel=9 / VDDK=8 / CBT=Yes')
    })

    it('does not expose low-level QEMU or Pod details in the selector', () => {
      const label = formatKvmConversionHostName(host, 'cbt', 'Yes', 'No')

      expect(label).not.toContain('qemu-img')
      expect(label).not.toContain('qemu-nbd')
      expect(label).not.toContain('Pod')
      expect(label).not.toContain('Supported')
    })
  })
})
