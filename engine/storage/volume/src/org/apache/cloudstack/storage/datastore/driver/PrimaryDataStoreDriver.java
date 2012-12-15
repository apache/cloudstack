package org.apache.cloudstack.storage.datastore.driver;

import java.util.Map;

import org.apache.cloudstack.storage.EndPoint;
import org.apache.cloudstack.storage.datastore.PrimaryDataStore;
import org.apache.cloudstack.storage.volume.TemplateOnPrimaryDataStoreInfo;
import org.apache.cloudstack.storage.volume.VolumeObject;

public interface PrimaryDataStoreDriver {
    boolean createVolume(VolumeObject vol);

    boolean createVolumeFromBaseImage(VolumeObject volume, TemplateOnPrimaryDataStoreInfo template);

    boolean deleteVolume(VolumeObject vo);

    String grantAccess(VolumeObject vol, EndPoint ep);

    boolean revokeAccess(VolumeObject vol, EndPoint ep);
    
    long getCapacity();
    
    long getAvailableCapacity();
    
    
    //Lifecycle API
    boolean initialize(Map<String, String> params);
    boolean grantAccess(EndPoint ep);
    boolean revokeAccess(EndPoint ep);
    void setDataStore(PrimaryDataStore dataStore);
}
