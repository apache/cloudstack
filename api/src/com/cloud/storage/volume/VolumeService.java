package com.cloud.storage.volume;

import java.util.List;

import com.cloud.api.commands.AttachVolumeCmd;
import com.cloud.api.commands.CreateVolumeCmd;
import com.cloud.api.commands.DeleteVolumeCmd;
import com.cloud.api.commands.DetachVolumeCmd;
import com.cloud.api.commands.ListVolumesCmd;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientStorageCapacityException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.offering.DiskOffering;
import com.cloud.storage.pool.StoragePool;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

public interface VolumeService {
    /**
     * Creates the database object for a volume based on the given criteria
     * 
     * @param cmd
     *            the API command wrapping the criteria (account/domainId [admin only], zone, diskOffering, snapshot,
     *            name)
     * @return the volume object
     * @throws PermissionDeniedException
     */
    Volume allocVolume(CreateVolumeCmd cmd) throws ResourceAllocationException;

    /**
     * Creates the volume based on the given criteria
     * 
     * @param cmd
     *            the API command wrapping the criteria (account/domainId [admin only], zone, diskOffering, snapshot,
     *            name)
     * @return the volume object
     */
    Volume createVolume(CreateVolumeCmd cmd);

    /**
     * Delete volume
     * @param volumeId
     * @return
     * @throws ConcurrentOperationException
     */
    boolean deleteVolume(long volumeId) throws ConcurrentOperationException;

    /**
     * Migrate volume to another storage pool
     * @param volumeId
     * @param storagePoolId
     * @return
     * @throws ConcurrentOperationException
     */
    Volume migrateVolume(Long volumeId, Long storagePoolId) throws ConcurrentOperationException;
    
    /**
     * Copy volume another storage pool, a new volume will be created on destination storage pool
     * @param volumeId
     * @param destStoragePoolId
     * @return
     */
    Volume copyVolume(Long volumeId, Long destStoragePoolId);
    
    List<? extends Volume> searchForVolumes(ListVolumesCmd cmd);
    
    void allocateVolume(Long vmId, Pair<? extends DiskOffering, Long> rootDiskOffering, 
    		List<Pair<DiskOffering, Long>> dataDiskOfferings,
    		Long templateId, Account owner);
    
	void prepareForMigration(VirtualMachineProfile<? extends VirtualMachine> vm, DeployDestination dest);
    void prepare(VirtualMachineProfile<? extends VirtualMachine> vm, DeployDestination dest, boolean recreate) throws StorageUnavailableException, 
    InsufficientStorageCapacityException, ConcurrentOperationException;

	Volume attachVolumeToVM(AttachVolumeCmd command);
	boolean attachIsoToVm(long isoId, long vmId);
	boolean detachIsoToVm(long vmId);
	Volume detachVolumeFromVM(DetachVolumeCmd cmd);

	boolean vmStorageMigration(VirtualMachineProfile<? extends VirtualMachine> vm, StoragePool destPool) throws ConcurrentOperationException;
	boolean deleteVolume(DeleteVolumeCmd cmd);
	void release(VirtualMachineProfile<? extends VirtualMachine> profile);
	
	void cleanupVolumes(long vmId) throws ConcurrentOperationException;
	void recreateVolume(long volumeId, long vmId) throws ConcurrentOperationException;
}
