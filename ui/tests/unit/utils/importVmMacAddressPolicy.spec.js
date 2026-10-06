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

import {
  getDefaultMacAddressConflictOptions,
  getMacAddressConflictApiParams,
  selectAllowDuplicateMacAddressesOption,
  selectForcedMacAddressConflictOption
} from '@/utils/importVmMacAddressPolicy'

describe('remote KVM MAC address conflict options', () => {
  it('defaults remote KVM imports to replacement and leaves other imports unchanged', () => {
    expect(getDefaultMacAddressConflictOptions(true)).toEqual({
      forced: true,
      allowDuplicateMacAddresses: false
    })
    expect(getDefaultMacAddressConflictOptions(false)).toEqual({
      forced: false,
      allowDuplicateMacAddresses: false
    })
  })

  it('keeps replacement and duplicate preservation mutually exclusive', () => {
    expect(selectAllowDuplicateMacAddressesOption({
      forced: true,
      allowDuplicateMacAddresses: false
    }, true)).toEqual({
      forced: false,
      allowDuplicateMacAddresses: true
    })

    expect(selectForcedMacAddressConflictOption({
      forced: false,
      allowDuplicateMacAddresses: true
    }, true)).toEqual({
      forced: true,
      allowDuplicateMacAddresses: false
    })
  })

  it('submits an explicit false when replacement is disabled for remote KVM', () => {
    expect(getMacAddressConflictApiParams({
      forced: false,
      allowDuplicateMacAddresses: false
    }, true, true, true)).toEqual({ forced: false })
  })

  it('submits duplicate preservation together with replacement disabled', () => {
    expect(getMacAddressConflictApiParams({
      forced: false,
      allowDuplicateMacAddresses: true
    }, true, true, true)).toEqual({
      forced: false,
      allowduplicatemacaddresses: true
    })
  })

  it('preserves truthy-only serialization for other import paths', () => {
    expect(getMacAddressConflictApiParams({
      forced: false,
      allowDuplicateMacAddresses: false
    }, false, true, true)).toEqual({})
    expect(getMacAddressConflictApiParams({
      forced: true,
      allowDuplicateMacAddresses: false
    }, false, true, true)).toEqual({ forced: true })
  })

  it('does not submit duplicate preservation when the API capability is absent', () => {
    expect(getMacAddressConflictApiParams({
      forced: false,
      allowDuplicateMacAddresses: true
    }, true, true, false)).toEqual({ forced: false })
  })
})
