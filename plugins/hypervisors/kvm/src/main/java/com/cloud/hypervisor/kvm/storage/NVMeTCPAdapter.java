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

import com.cloud.storage.Storage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * StorageAdaptor for the {@link Storage.StoragePoolType#NVMeTCP} pool type.
 * All operational logic lives in {@link MultipathNVMeOFAdapterBase}; this
 * class just binds that logic to a pool type so
 * {@link KVMStoragePoolManager} can find it via reflection.
 */
public class NVMeTCPAdapter extends MultipathNVMeOFAdapterBase {
    private static final Logger LOGGER = LogManager.getLogger(NVMeTCPAdapter.class);

    public NVMeTCPAdapter() {
        LOGGER.info("Loaded NVMeTCPAdapter for StorageLayer");
    }

    @Override
    public String getName() {
        return "NVMeTCPAdapter";
    }

    @Override
    public Storage.StoragePoolType getStoragePoolType() {
        return Storage.StoragePoolType.NVMeTCP;
    }

    @Override
    public boolean isStoragePoolTypeSupported(Storage.StoragePoolType type) {
        return Storage.StoragePoolType.NVMeTCP.equals(type);
    }
}
