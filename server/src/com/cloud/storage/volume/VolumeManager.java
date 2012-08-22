package com.cloud.storage.volume;

import java.util.List;

import com.cloud.agent.api.to.VolumeTO;
import com.cloud.api.commands.AttachVolumeCmd;
import com.cloud.api.commands.ListVolumesCmd;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientStorageCapacityException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.offering.DiskOffering;
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
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

public interface VolumeManager {
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
	 * @throws StorageUnavailableException 
	 * @throws ConcurrentOperationException 
	 */
	VolumeVO createVolume(VolumeVO volume, long VMTemplateId, DiskOfferingVO diskOffering,
            HypervisorType hyperType, StoragePool assignedPool) throws StorageUnavailableException, ConcurrentOperationException;
	
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
    		Pair<? extends DiskOffering, Long> rootDiskOffering,
    		List<Pair<DiskOffering, Long>> dataDiskOfferings, Long templateId, Account owner);
    
    void cleanupVolumes(long vmId) throws ConcurrentOperationException;
	
	boolean processEvent(Volume vol, Event event)
			throws NoTransitionException;
	
	boolean validateVolume(Account caller, long ownerId, Long zoneId, String volumeName, String url, String format) throws ResourceAllocationException;
	VolumeVO persistVolume(Account caller, long ownerId, Long zoneId, String volumeName, String url, String format);

	Volume copyVolume(Long volumeId, Long destStoragePoolId);

	List<VolumeVO> searchForVolumes(ListVolumesCmd cmd);

	VolumeVO allocateDiskVolume(String volumeName, long zoneId, long ownerId,
			long domainId, long diskOfferingId, long size);

	Volume attachVolumeToVM(VolumeVO volume, UserVmVO vm, Long deviceId) throws StorageUnavailableException, ConcurrentOperationException, AgentUnavailableException, OperationTimedoutException;

	void attachISOToVm(UserVmVO vm, VMTemplateVO iso);

	void detachISOToVM(UserVmVO vm);

	Volume detachVolumeFromVM(VolumeVO volume, UserVmVO vm);

	boolean deleteVolume(VolumeVO volume) throws ConcurrentOperationException;

	boolean migrateVolumes(List<Volume> volumes, StoragePool destPool) throws ConcurrentOperationException;

	void release(VolumeVO volume);

	void recreateVolume(VolumeVO volume, long vmId) throws ConcurrentOperationException;
}
