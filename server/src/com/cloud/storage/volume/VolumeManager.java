package com.cloud.storage.volume;

import java.util.List;

import com.cloud.api.commands.ListVolumesCmd;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientStorageCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.pool.StoragePool;
import com.cloud.storage.volume.Volume.Event;
import com.cloud.storage.volume.Volume.Type;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

public interface VolumeManager {
    /** Returns the absolute path of the specified ISO
     * @param templateId - the ID of the template that represents the ISO
     * @param datacenterId
     * @return absolute ISO path
     */
	public Pair<String, String> getAbsoluteIsoPath(long templateId, long dataCenterId);
	
	/**
	 * Moves a volume from its current storage pool to a storage pool with enough capacity in the specified zone, pod, or cluster
	 * @param volume
	 * @param destPoolDcId
	 * @param destPoolPodId
	 * @param destPoolClusterId
	 * @return VolumeVO
	 * @throws ConcurrentOperationException 
	 */
	VolumeVO moveVolume(VolumeVO volume, long destPoolDcId, Long destPoolPodId, Long destPoolClusterId, HypervisorType dataDiskHyperType) throws ConcurrentOperationException;

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
	 * @return 
	 */
	boolean destroyVolume(VolumeVO volume) throws ConcurrentOperationException;
	
	/**
	 * Checks that one of the following is true:
	 * 1. The volume is not attached to any VM
	 * 2. The volume is attached to a VM that is running on a host with the KVM hypervisor, and the VM is stopped
	 * 3. The volume is attached to a VM that is running on a host with the XenServer hypervisor (the VM can be stopped or running)
	 * @return true if one of the above conditions is true
	 */
	boolean volumeInactive(VolumeVO volume);
	
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
    DiskProfile allocateVolume(Long vmId,
    		Pair<? extends DiskOfferingVO, Long> rootDiskOffering,
    		List<Pair<DiskOfferingVO, Long>> dataDiskOfferings, Long templateId, Account owner);
    
    void cleanupVolumes(long vmId) throws ConcurrentOperationException;
	boolean StorageMigration(
			VirtualMachineProfile<? extends VirtualMachine> vm,
			StoragePool destPool) throws ConcurrentOperationException;
	
	boolean stateTransitTo(Volume vol, Event event)
			throws NoTransitionException;
	
	VolumeVO allocateDuplicateVolume(VolumeVO oldVol, Long templateId);
	boolean validateVolume(Account caller, long ownerId, Long zoneId, String volumeName, String url, String format) throws ResourceAllocationException;
	VolumeVO persistVolume(Account caller, long ownerId, Long zoneId, String volumeName, String url, String format);
	
	/**
	 * Checks that the volume is stored on a shared storage pool
	 * @param volume
	 * @return true if the volume is on a shared storage pool, false otherwise
	 */
	boolean volumeOnSharedStoragePool(VolumeVO volume);
	
	String getVmNameOnVolume(VolumeVO volume);
	void expungeVolume(VolumeVO vol, boolean force);

	Volume migrateVolume(Long volumeId, Long storagePoolId)
			throws ConcurrentOperationException;

	void prepare(VirtualMachineProfile<? extends VirtualMachine> vm,
			DeployDestination dest, boolean recreate)
			throws StorageUnavailableException,
			InsufficientStorageCapacityException;

	Volume copyVolume(Long volumeId, Long destStoragePoolId);

	List<VolumeVO> searchForVolumes(ListVolumesCmd cmd);
}
