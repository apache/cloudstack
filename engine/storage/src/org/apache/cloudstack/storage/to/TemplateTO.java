package org.apache.cloudstack.storage.to;

import org.apache.cloudstack.engine.subsystem.api.storage.disktype.DiskFormat;
import org.apache.cloudstack.storage.image.TemplateInfo;
import org.apache.cloudstack.storage.image.datastore.ImageDataStoreInfo;

public class TemplateTO {
    private final String path;
    private final String uuid;
    private  DiskFormat diskType;
    private final ImageDataStoreTO imageDataStore;

    public TemplateTO(TemplateInfo template) {
        this.path = template.getPath();
        this.uuid = template.getUuid();
        //this.diskType = template.getDiskType();
        this.imageDataStore = new ImageDataStoreTO((ImageDataStoreInfo)template.getDataStore());
    }
    
    public String getPath() {
        return this.path;
    }
    
    public String getUuid() {
        return this.uuid;
    }
    
    public DiskFormat getDiskType() {
        return this.diskType;
    }
    
    public ImageDataStoreTO getImageDataStore() {
        return this.imageDataStore;
    }
}
