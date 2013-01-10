package org.apache.cloudstack.storage.datastore.driver;

import java.util.Map;

import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.EndPoint;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.datastore.PrimaryDataStore;
import org.apache.cloudstack.storage.volume.TemplateOnPrimaryDataStoreInfo;
import org.apache.cloudstack.storage.volume.VolumeObject;

public interface PrimaryDataStoreDriver {
    void createVolumeAsync(VolumeObject vol, AsyncCompletionCallback<CommandResult> callback);

    void createVolumeFromBaseImageAsync(VolumeObject volume, String template, AsyncCompletionCallback<CommandResult> callback);

    void deleteVolumeAsync(VolumeObject vo, AsyncCompletionCallback<CommandResult> callback);

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
