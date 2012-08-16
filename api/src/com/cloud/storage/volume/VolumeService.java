package com.cloud.storage.volume;

import java.util.List;

import com.cloud.api.commands.CreateVolumeCmd;
import com.cloud.api.commands.ListVolumesCmd;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;

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
}
