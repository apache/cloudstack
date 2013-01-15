package org.apache.cloudstack.storage.to;

import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.disktype.DiskFormat;
import org.apache.cloudstack.engine.subsystem.api.storage.type.VolumeType;

public class VolumeTO {
    private final String uuid;
    private final String path;
    private  VolumeType volumeType;
    private  DiskFormat diskType;
    private PrimaryDataStoreTO dataStore;
    private  String name;
    private final long size;
    public VolumeTO(VolumeInfo volume) {
        this.uuid = volume.getUuid();
        this.path = volume.getUri();
        //this.volumeType = volume.getType();
        //this.diskType = volume.getDiskType();
        if (volume.getDataStore() != null) {
            this.dataStore = new PrimaryDataStoreTO((PrimaryDataStoreInfo)volume.getDataStore());
        } else {
            this.dataStore = null;
        }
        //this.name = volume.getName();
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
    
    public DiskFormat getDiskType() {
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
