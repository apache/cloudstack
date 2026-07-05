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
package org.apache.cloudstack.vm;

import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;

import com.cloud.agent.api.to.VmwareCbtTargetStorageType;
import com.cloud.storage.Storage;

class VmwareCbtStorageTarget {

    private final StoragePoolVO pool;
    private final VmwareCbtTargetStorageType targetStorageType;
    private final boolean supported;
    private final boolean requiresInPlaceFinalization;
    private final String supportMessage;

    private VmwareCbtStorageTarget(StoragePoolVO pool, VmwareCbtTargetStorageType targetStorageType,
                                   boolean supported, boolean requiresInPlaceFinalization, String supportMessage) {
        this.pool = pool;
        this.targetStorageType = targetStorageType;
        this.supported = supported;
        this.requiresInPlaceFinalization = requiresInPlaceFinalization;
        this.supportMessage = supportMessage;
    }

    static VmwareCbtStorageTarget forPool(StoragePoolVO pool) {
        if (pool == null) {
            return new VmwareCbtStorageTarget(null, VmwareCbtTargetStorageType.QCOW2_FILE, true, false,
                    "No explicit pool selected; CloudStack will use the default filesystem target path.");
        }

        Storage.StoragePoolType poolType = pool.getPoolType();
        if (poolType == Storage.StoragePoolType.NetworkFilesystem ||
                poolType == Storage.StoragePoolType.Filesystem ||
                poolType == Storage.StoragePoolType.SharedMountPoint) {
            return new VmwareCbtStorageTarget(pool, VmwareCbtTargetStorageType.QCOW2_FILE, true, false,
                    "Filesystem-like primary storage will use qcow2 file targets.");
        }
        if (poolType == Storage.StoragePoolType.RBD) {
            return new VmwareCbtStorageTarget(pool, VmwareCbtTargetStorageType.RBD_RAW, true, true,
                    "Ceph/RBD primary storage will use raw RBD targets and requires in-place finalization.");
        }
        return new VmwareCbtStorageTarget(pool, VmwareCbtTargetStorageType.QCOW2_FILE, false, true,
                String.format("Storage pool type %s is not supported for VMware CBT migration targets.", poolType));
    }

    StoragePoolVO getPool() {
        return pool;
    }

    VmwareCbtTargetStorageType getTargetStorageType() {
        return targetStorageType;
    }

    boolean isSupported() {
        return supported;
    }

    boolean requiresInPlaceFinalization() {
        return requiresInPlaceFinalization;
    }

    boolean supportsNonInPlaceFinalizationFallback() {
        return supported && targetStorageType == VmwareCbtTargetStorageType.QCOW2_FILE;
    }

    String getSupportMessage() {
        return supportMessage;
    }
}
