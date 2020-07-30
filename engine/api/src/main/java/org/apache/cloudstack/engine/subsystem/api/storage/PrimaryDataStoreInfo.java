/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.engine.subsystem.api.storage;

import java.util.Map;

import org.apache.cloudstack.engine.subsystem.api.storage.disktype.DiskFormat;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StoragePool;

public interface PrimaryDataStoreInfo extends StoragePool {
    static final String MANAGED = "managed";
    static final String STORAGE_HOST= "storageHost";
    static final String STORAGE_PORT = "storagePort";
    static final String MANAGED_STORE_TARGET = "managedStoreTarget";
    static final String MANAGED_STORE_TARGET_ROOT_VOLUME = "managedStoreTargetRootVolume";
    static final String CHAP_INITIATOR_USERNAME = "chapInitiatorUsername";
    static final String CHAP_INITIATOR_SECRET = "chapInitiatorSecret";
    static final String CHAP_TARGET_USERNAME = "chapTargetUsername";
    static final String CHAP_TARGET_SECRET = "chapTargetSecret";
    static final String REMOVE_AFTER_COPY = "removeAfterCopy";
    static final String VOLUME_SIZE = "volumeSize";

    boolean isHypervisorSupported(HypervisorType hypervisor);

    boolean isLocalStorageSupported();

    boolean isVolumeDiskTypeSupported(DiskFormat diskType);

    @Override
    String getUuid();

    @Override
    StoragePoolType getPoolType();

    boolean isManaged();

    void setDetails(Map<String, String> details);

    Map<String, String> getDetails();

    PrimaryDataStoreLifeCycle getLifeCycle();

    Long getParent();
}
