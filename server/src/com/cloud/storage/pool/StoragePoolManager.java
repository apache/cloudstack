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
package com.cloud.storage.pool;

import java.util.List;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.manager.Commands;
import com.cloud.capacity.CapacityVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientStorageCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.pool.Storage.ImageFormat;
import com.cloud.storage.volume.Volume;
import com.cloud.storage.volume.Volume.Event;
import com.cloud.storage.volume.Volume.Type;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Manager;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

public interface StoragePoolManager extends StoragePoolService, Manager {
    boolean canVmRestartOnAnotherServer(long vmId);

	/**
	 * Returns the URL of the secondary storage host
	 * @param zoneId
	 * @return URL
	 */
	public String getSecondaryStorageURL(long zoneId);
	
	/**
	 * Returns a comma separated list of tags for the specified storage pool
	 * @param poolId
	 * @return comma separated list of tags
	 */
	public String getStoragePoolTags(long poolId);
	
	/**
	 * Returns the secondary storage host
	 * @param zoneId
	 * @return secondary storage host
	 */
	public HostVO getSecondaryStorageHost(long zoneId);
	
	/**
	 * Returns the secondary storage host
	 * @param zoneId
	 * @return secondary storage host
	 */
    public VMTemplateHostVO findVmTemplateHost(long templateId, StoragePool pool);

	/** Create capacity entries in the op capacity table
	 * @param storagePool
	 */
	public void createCapacityEntry(StoragePoolVO storagePool);

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
	boolean isLocalStorageActiveOnHost(Host host);
	
    /**
	 * Cleans up storage pools by removing unused templates.
	 * @param recurring - true if this cleanup is part of a recurring garbage collection thread
	 */
	void cleanupStorage(boolean recurring);
	
    String getPrimaryStorageNameLabel(VolumeVO volume);

	void createCapacityEntry(StoragePoolVO storagePool, short capacityType, long allocated);
    
	void release(VirtualMachineProfile<? extends VMInstanceVO> profile);

	Answer sendToPool(StoragePool pool, long[] hostIdsToTryFirst, Command cmd) throws StorageUnavailableException;

	CapacityVO getSecondaryStorageUsedStats(Long hostId, Long zoneId);

	CapacityVO getStoragePoolUsedStats(Long poolId, Long clusterId, Long podId, Long zoneId);

    boolean createStoragePool(long hostId, StoragePoolVO pool);

    boolean delPoolFromHost(long hostId);

    HostVO getSecondaryStorageHost(long zoneId, long tmpltId);

    List<HostVO> getSecondaryStorageHosts(long zoneId);

    List<StoragePoolVO> ListByDataCenterHypervisor(long datacenterId, HypervisorType type);


    List<VMInstanceVO> listByStoragePool(long storagePoolId);

    StoragePoolVO findLocalStorageOnHost(long hostId);

    VMTemplateHostVO getTemplateHostRef(long zoneId, long tmpltId, boolean readyOnly);

	Host updateSecondaryStorage(long secStorageId, String newUrl);

	List<Long> getUpHostsInPool(long poolId);

    void cleanupSecondaryStorage(boolean recurring);

	String getSupportedImageFormatForCluster(Long clusterId);

	HypervisorType getHypervisorTypeFromFormat(ImageFormat format);

	boolean storagePoolHasEnoughSpace(List<Volume> volume, StoragePool pool);

	StoragePool getStoragePoolById(Long id);
}
