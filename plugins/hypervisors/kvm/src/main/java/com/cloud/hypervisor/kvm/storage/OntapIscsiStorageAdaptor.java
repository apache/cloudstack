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
package com.cloud.hypervisor.kvm.storage;

import com.cloud.storage.Storage.StoragePoolType;

/**
 * Serves {@link StoragePoolType#OntapiSCSI} pools, which are ONTAP FlexVols exposed over iSCSI with one
 * LUN per CloudStack volume. The host-side handling is identical to a generic iSCSI target
 *
 * The class exists so that ONTAP-specific host behaviour can diverge here without altering the storage
 * path of the other vendors that register as {@link StoragePoolType#Iscsi} which all share the superclass.
 *
 * This must stay in the {@code com.cloud.hypervisor.kvm.storage} package: {@link KVMStoragePoolManager}
 * discovers adaptors by a Reflections scan of that package alone, and an unregistered type silently
 * falls back to {@link LibvirtStorageAdaptor} rather than failing at startup.
 */
public class OntapIscsiStorageAdaptor extends IscsiAdmStorageAdaptor {

    @Override
    public StoragePoolType getStoragePoolType() {
        return StoragePoolType.OntapiSCSI;
    }
}
