package org.apache.cloudstack.storage.to;

import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.disktype.VolumeDiskType;
import org.apache.cloudstack.engine.subsystem.api.storage.type.VolumeType;

public class VolumeTO {
    private final String uuid;
    private final String path;
    private final VolumeType volumeType;
    private final VolumeDiskType diskType;
    private PrimaryDataStoreTO dataStore;
    private final String name;
    private final long size;
    public VolumeTO(VolumeInfo volume) {
        this.uuid = volume.getUuid();
        this.path = volume.getPath();
        this.volumeType = volume.getType();
        this.diskType = volume.getDiskType();
        if (volume.getDataStore() != null) {
            this.dataStore = new PrimaryDataStoreTO(volume.getDataStore());
        } else {
            this.dataStore = null;
        }
        this.name = volume.getName();
        this.size = volume.getSize();
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
    
    public void setDataStore(PrimaryDataStoreTO dataStore) {
        this.dataStore = dataStore;
    }
    
    public String getName() {
        return this.name;
    }
    
    public long getSize() {
        return this.size;
    }
}
