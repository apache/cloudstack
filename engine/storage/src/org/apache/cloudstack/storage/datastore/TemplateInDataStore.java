package org.apache.cloudstack.storage.datastore;

import org.apache.cloudstack.engine.subsystem.api.storage.disktype.VolumeDiskType;
import org.apache.cloudstack.storage.db.ObjectInDataStoreVO;
import org.apache.cloudstack.storage.image.TemplateInfo;
import org.apache.cloudstack.storage.image.store.ImageDataStoreInfo;

public class TemplateInDataStore implements TemplateInfo {
    public TemplateInDataStore(TemplateInfo template, DataStore dataStore, ObjectInDataStoreVO obj) {
        
    }
    @Override
    public ImageDataStoreInfo getDataStore() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getId() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public VolumeDiskType getDiskType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getPath() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getUuid() {
        // TODO Auto-generated method stub
        return null;
    }

}
