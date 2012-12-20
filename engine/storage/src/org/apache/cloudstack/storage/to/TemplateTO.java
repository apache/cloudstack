package org.apache.cloudstack.storage.to;

import org.apache.cloudstack.engine.subsystem.api.storage.disktype.VolumeDiskType;
import org.apache.cloudstack.storage.image.TemplateInfo;

public class TemplateTO {
    private final String path;
    private final String uuid;
    private final VolumeDiskType diskType;
    private final ImageDataStoreTO imageDataStore;
    private final long size = 0;
    public TemplateTO(TemplateInfo template) {
        this.path = template.getPath();
        this.uuid = template.getUuid();
        this.diskType = template.getDiskType();
        this.imageDataStore = new ImageDataStoreTO(template.getImageDataStore());
       // this.size = template.getVirtualSize();
    }
    
    public String getPath() {
        return this.path;
    }
    
    public String getUuid() {
        return this.uuid;
    }
    
    public VolumeDiskType getDiskType() {
        return this.diskType;
    }
    
    public ImageDataStoreTO getImageDataStore() {
        return this.imageDataStore;
    }
    
    public long getSize() {
        return this.size;
    }
}
