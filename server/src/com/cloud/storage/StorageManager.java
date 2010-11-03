/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.storage;

import java.net.UnknownHostException;
import java.util.List;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.manager.Commands;
import com.cloud.api.commands.CancelPrimaryStorageMaintenanceCmd;
import com.cloud.api.commands.CreateStoragePoolCmd;
import com.cloud.api.commands.CreateVolumeCmd;
import com.cloud.api.commands.DeletePoolCmd;
import com.cloud.api.commands.DeleteVolumeCmd;
import com.cloud.api.commands.PreparePrimaryStorageForMaintenanceCmd;
import com.cloud.api.commands.UpdateStoragePoolCmd;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientStorageCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceInUseException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.Volume.VolumeType;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Manager;
import com.cloud.utils.exception.ExecutionException;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

public interface StorageManager extends Manager {
    
    
    VolumeVO allocateIsoInstalledVm(VMInstanceVO vm, VMTemplateVO template, DiskOfferingVO rootOffering, Long size, DataCenterVO dc, Account account);
    
	/**
	 * Calls the storage agent and makes the volumes sharable with this host.
	 * 
	 * @param vm vm that owns the volumes
	 * @param vols volumes to share
	 * @param host host to share the volumes to.
	 * @param cancelPrevious cancel the previous shares?
	 * @return true if works.
	 * 
	 * @throws StorageUnavailableException if the storage server is unavailable.
	 */
	boolean share(VMInstanceVO vm, List<VolumeVO> vols, HostVO host, boolean cancelPrevious) throws StorageUnavailableException;

    List<VolumeVO> prepare(VMInstanceVO vm, HostVO host);
    
	/**
	 * Calls the storage server to unshare volumes to the host.
	 * 
	 * @param vm vm that owns the volumes.
	 * @param vols volumes to remove from share.
	 * @param host host to unshare the volumes to.
	 * @return true if it worked; false if not.
	 */
	boolean unshare(VMInstanceVO vm, List<VolumeVO> vols, HostVO host);
	
	/**
	 * unshares the storage volumes of a certain vm to the host.
	 * 
	 * @param vm vm to unshare.
	 * @param host host.
	 * @return List<VolumeVO> if succeeded. null if not.
	 */
	List<VolumeVO> unshare(VMInstanceVO vm, HostVO host);
	
	/**
     * destroy the storage volumes of a certain vm.
     * 
     * @param vm vm to destroy.
     * @param vols volumes to remove from storage pool
     */
	void destroy(VMInstanceVO vm, List<VolumeVO> vols);
	
    /**
     * Creates volumes for a particular VM.
     * @param account account to create volumes for.
     * @param vm vm to create the volumes for.
     * @param template template the root volume is based on.
     * @param dc datacenter to put this.
     * @param pod pod to put this.
     * @param offering service offering of the vm.
     * @param diskOffering disk offering of the vm.
     * @param avoids storage pools to avoid.
     * @param size : size of the volume if defined
     * @return List of VolumeVO
     */
	List<VolumeVO> create(Account account, VMInstanceVO vm, VMTemplateVO template, DataCenterVO dc, HostPodVO pod, ServiceOfferingVO offering, DiskOfferingVO diskOffering, long size) throws StorageUnavailableException, ExecutionException;
	
	/**
	 * Create StoragePool based on uri
	 * @param cmd the command object that specifies the zone, cluster/pod, URI, details, etc. to use to create the storage pool.
	 * @return
	 * @throws ResourceInUseException
	 * @throws IllegalArgumentException
	 * @throws UnknownHostException
	 * @throws ResourceAllocationException
	 */
	StoragePoolVO createPool(CreateStoragePoolCmd cmd) throws ResourceInUseException, IllegalArgumentException, UnknownHostException, ResourceAllocationException;
	
    /**
     * Get the storage ip address to connect to.
     * @param vm vm to run.
     * @param host host to run it on.
     * @param storage storage that contains the vm.
     * @return ip address if it can be determined.  null if not.
     */
    String chooseStorageIp(VMInstanceVO vm, Host host, Host storage);

    boolean canVmRestartOnAnotherServer(long vmId);

    /** Returns the absolute path of the specified ISO
     * @param templateId - the ID of the template that represents the ISO
     * @param datacenterId
     * @return absolute ISO path
     */
	public Pair<String, String> getAbsoluteIsoPath(long templateId, long dataCenterId);
	
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
	 * Create the volumes for a user VM based on service offering in a particular data center
	 * 
	 * @return true if successful
	 */
	public long createUserVM(Account account, VMInstanceVO vm,
			VMTemplateVO template, DataCenterVO dc, HostPodVO pod,
			ServiceOfferingVO offering, DiskOfferingVO diskOffering,
			List<StoragePoolVO> avoids, long size);

	/**
	 * This method sends the given command on all the hosts in the primary storage pool given until is succeeds on any one.
	 * If the command doesn't succeed on any, it return null. All exceptions are swallowed. Any errors are expected be be in
	 * answer.getDetails(), if it's not null.
	 * @param poolId        The primary storage pool. The cmd uses this for some reason.
	 * @param cmd           Any arbitrary command which needs access to the volumes on the given storage pool.
	 * @param basicErrMsg   The cmd specific error msg to spew out in case of any exception.
	 * @return The answer for that command, could be success or failure.
	 */
	Answer sendToHostsOnStoragePool(Long poolId, Command cmd, String basicErrMsg);
	Answer sendToHostsOnStoragePool(Long poolId, Command cmd, String basicErrMsg, int retriesPerHost, int pauseBeforeRetry, boolean shouldBeSnapshotCapable, Long vmId );
	

	/**
	 * Add a pool to a host
	 * @param hostId
	 * @param pool
	 */
	boolean addPoolToHost(long hostId, StoragePoolVO pool);
	
	/**
	 * Moves a volume from its current storage pool to a storage pool with enough capacity in the specified zone, pod, or cluster
	 * @param volume
	 * @param destPoolDcId
	 * @param destPoolPodId
	 * @param destPoolClusterId
	 * @return VolumeVO
	 */
	VolumeVO moveVolume(VolumeVO volume, long destPoolDcId, Long destPoolPodId, Long destPoolClusterId);

	/**
	 * Creates the database object for a volume based on the given criteria
	 * @param cmd the API command wrapping the criteria (account/domainId [admin only], zone, diskOffering, snapshot, name)
	 * @return the volume object
	 * @throws InvalidParameterValueException
	 * @throws PermissionDeniedException
	 */
	VolumeVO allocVolume(CreateVolumeCmd cmd) throws ResourceAllocationException;

	/**
     * Creates the volume based on the given criteria
     * @param cmd the API command wrapping the criteria (account/domainId [admin only], zone, diskOffering, snapshot, name)
     * @return the volume object
	 */
	VolumeVO createVolume(CreateVolumeCmd cmd);

	/**
	 * Create a volume based on the given criteria
	 * @param volume
	 * @param vm
	 * @param template
	 * @param dc
	 * @param pod
	 * @param clusterId
	 * @param offering
	 * @param diskOffering
	 * @param avoids
	 * @param size
	 * @param hyperType
	 * @return volume VO if success, null otherwise
	 */
	VolumeVO createVolume(VolumeVO volume, VMInstanceVO vm, VMTemplateVO template, DataCenterVO dc, HostPodVO pod, Long clusterId,
            ServiceOfferingVO offering, DiskOfferingVO diskOffering, List<StoragePoolVO> avoids, long size, HypervisorType hyperType);

	/**
	 * Marks the specified volume as destroyed in the management server database. The expunge thread will delete the volume from its storage pool.
	 * @param volume
	 */
	void destroyVolume(VolumeVO volume);
	boolean deleteVolume(DeleteVolumeCmd cmd) throws InvalidParameterValueException;
	
	/** Create capacity entries in the op capacity table
	 * @param storagePool
	 */
	public void createCapacityEntry(StoragePoolVO storagePool);

	/**
	 * Checks that the volume is stored on a shared storage pool
	 * @param volume
	 * @return true if the volume is on a shared storage pool, false otherwise
	 */
	boolean volumeOnSharedStoragePool(VolumeVO volume);
	
	Answer[] sendToPool(StoragePool pool, Commands cmds);
	
	Answer sendToPool(StoragePool pool, Command cmd);
	
	/**
	 * Checks that one of the following is true:
	 * 1. The volume is not attached to any VM
	 * 2. The volume is attached to a VM that is running on a host with the KVM hypervisor, and the VM is stopped
	 * 3. The volume is attached to a VM that is running on a host with the XenServer hypervisor (the VM can be stopped or running)
	 * @return true if one of the above conditions is true
	 */
	boolean volumeInactive(VolumeVO volume);
	
	String getVmNameOnVolume(VolumeVO volume);
	
	List<Pair<VolumeVO, StoragePoolVO>> isStoredOn(VMInstanceVO vm);

	/**
	 * Checks if a host has running VMs that are using its local storage pool.
	 * @return true if local storage is active on the host
	 */
	boolean isLocalStorageActiveOnHost(HostVO host);
	
    /**
	 * Cleans up storage pools by removing unused templates.
	 * @param recurring - true if this cleanup is part of a recurring garbage collection thread
	 */
	void cleanupStorage(boolean recurring);
	
	/**
	 * Delete the storage pool
	 * @param cmd - the command specifying poolId
	 * @return success or failure
	 * @throws InvalidParameterValueException
	 */
	boolean deletePool(DeletePoolCmd cmd) throws InvalidParameterValueException;
	

	/**
	 * Find all of the storage pools needed for this vm.
	 * 
	 * @param vmId id of the vm.
	 * @return List of StoragePoolVO
	 */
	List<StoragePoolVO> getStoragePoolsForVm(long vmId);
	
    String getPrimaryStorageNameLabel(VolumeVO volume);

    /**
     * Enable maintenance for primary storage
     * @param cmd - the command specifying primaryStorageId
     * @return the primary storage pool
     * @throws InvalidParameterValueException
     */
    public StoragePoolVO preparePrimaryStorageForMaintenance(PreparePrimaryStorageForMaintenanceCmd cmd) throws InvalidParameterValueException;
    
    /**
     * Complete maintenance for primary storage
     * @param cmd - the command specifying primaryStorageId
     * @return the primary storage pool
     * @throws InvalidParameterValueException
     */
    public StoragePoolVO cancelPrimaryStorageForMaintenance(CancelPrimaryStorageMaintenanceCmd cmd) throws InvalidParameterValueException;

	public StoragePoolVO updateStoragePool(UpdateStoragePoolCmd cmd) throws IllegalArgumentException;
    
    /**
     * Allocates one volume.
     * @param <T>
     * @param type
     * @param offering
     * @param name
     * @param size
     * @param template
     * @param vm
     * @param account
     * @return VolumeVO a persisted volume.
     */
    <T extends VMInstanceVO> DiskProfile allocateRawVolume(VolumeType type, String name, DiskOfferingVO offering, Long size, T vm, Account owner);
    <T extends VMInstanceVO> DiskProfile allocateTemplatedVolume(VolumeType type, String name, DiskOfferingVO offering, VMTemplateVO template, T vm, Account owner);
    
    Long findHostIdForStoragePool(StoragePool pool);
	void createCapacityEntry(StoragePoolVO storagePool, long allocated);

    
    void prepare(VirtualMachineProfile<? extends VirtualMachine> vm, DeployDestination dest) throws StorageUnavailableException, InsufficientStorageCapacityException, ConcurrentOperationException;
    void release(VirtualMachineProfile<? extends VirtualMachine> vm);
}
