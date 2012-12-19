package org.apache.cloudstack.storage.to;

import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.disktype.VolumeDiskType;
import org.apache.cloudstack.engine.subsystem.api.storage.type.VolumeType;

public class VolumeTO {
    private final String uuid;
    private final String path;
    private final VolumeType volumeType;
    private final VolumeDiskType diskType;
    private final PrimaryDataStoreTO dataStore;
    public VolumeTO(VolumeInfo volume) {
        this.uuid = volume.getUuid();
        this.path = volume.getPath();
        this.volumeType = volume.getType();
        this.diskType = volume.getDiskType();
        this.dataStore = new PrimaryDataStoreTO(volume.getDataStore());
    }
    
    public String getUuid() {
        return this.uuid;
    }
    
    public String getPath() {
        return this.path;
    }
    
    public VolumeType getVolumeType() {
        return this.volumeType;
    }
    
    public VolumeDiskType getDiskType() {
        return this.diskType;
    }
    
    public PrimaryDataStoreTO getDataStore() {
        return this.dataStore;
    }
}
