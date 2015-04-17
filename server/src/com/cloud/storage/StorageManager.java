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
package com.cloud.storage;

import java.math.BigDecimal;
import java.util.List;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.HypervisorHostListener;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.agent.manager.Commands;
import com.cloud.capacity.CapacityVO;
import com.cloud.exception.ConnectionException;
import com.cloud.exception.StorageConflictException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.Host;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.utils.Pair;
import com.cloud.vm.VMInstanceVO;

public interface StorageManager extends StorageService {
    /**
     * Returns a comma separated list of tags for the specified storage pool
     * @param poolId
     * @return comma separated list of tags
     */
    public String getStoragePoolTags(long poolId);

    Answer sendToPool(long poolId, Command cmd) throws StorageUnavailableException;

    Answer sendToPool(StoragePool pool, Command cmd) throws StorageUnavailableException;

    Answer[] sendToPool(long poolId, Commands cmd) throws StorageUnavailableException;

    Answer[] sendToPool(StoragePool pool, Commands cmds) throws StorageUnavailableException;

    Pair<Long, Answer[]> sendToPool(StoragePool pool, long[] hostIdsToTryFirst, List<Long> hostIdsToAvoid, Commands cmds) throws StorageUnavailableException;

    Pair<Long, Answer> sendToPool(StoragePool pool, long[] hostIdsToTryFirst, List<Long> hostIdsToAvoid, Command cmd) throws StorageUnavailableException;

    /**
     * Checks if a host has running VMs that are using its local storage pool.
     * @return true if local storage is active on the host
     */
    boolean isLocalStorageActiveOnHost(Long hostId);

    /**
     * Cleans up storage pools by removing unused templates.
     * @param recurring - true if this cleanup is part of a recurring garbage collection thread
     */
    void cleanupStorage(boolean recurring);

    String getPrimaryStorageNameLabel(VolumeVO volume);

    void createCapacityEntry(StoragePoolVO storagePool, short capacityType, long allocated);

    Answer sendToPool(StoragePool pool, long[] hostIdsToTryFirst, Command cmd) throws StorageUnavailableException;

    CapacityVO getSecondaryStorageUsedStats(Long hostId, Long zoneId);

    CapacityVO getStoragePoolUsedStats(Long poolId, Long clusterId, Long podId, Long zoneId);

    List<StoragePoolVO> ListByDataCenterHypervisor(long datacenterId, HypervisorType type);

    List<VMInstanceVO> listByStoragePool(long storagePoolId);

    StoragePoolVO findLocalStorageOnHost(long hostId);

    Host updateSecondaryStorage(long secStorageId, String newUrl);

    List<Long> getUpHostsInPool(long poolId);

    void cleanupSecondaryStorage(boolean recurring);

    HypervisorType getHypervisorTypeFromFormat(ImageFormat format);

    boolean storagePoolHasEnoughIops(List<Volume> volume, StoragePool pool);

    boolean storagePoolHasEnoughSpace(List<Volume> volume, StoragePool pool);

    boolean registerHostListener(String providerUuid, HypervisorHostListener listener);

    void connectHostToSharedPool(long hostId, long poolId) throws StorageUnavailableException, StorageConflictException;

    void createCapacityEntry(long poolId);

    DataStore createLocalStorage(Host host, StoragePoolInfo poolInfo) throws ConnectionException;

    BigDecimal getStorageOverProvisioningFactor(Long dcId);

    Long getDiskBytesReadRate(ServiceOfferingVO offering, DiskOfferingVO diskOffering);

    Long getDiskBytesWriteRate(ServiceOfferingVO offering, DiskOfferingVO diskOffering);

    Long getDiskIopsReadRate(ServiceOfferingVO offering, DiskOfferingVO diskOffering);

    Long getDiskIopsWriteRate(ServiceOfferingVO offering, DiskOfferingVO diskOffering);

    void cleanupDownloadUrls();
}
