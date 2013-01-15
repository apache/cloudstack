package org.apache.cloudstack.storage.datastore.driver;

import java.util.Set;

import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.snapshot.SnapshotInfo;
import org.apache.cloudstack.storage.volume.PrimaryDataStoreDriver;

public class SolidfirePrimaryDataStoreDriver implements PrimaryDataStoreDriver {

    @Override
    public String grantAccess(DataObject data,
            org.apache.cloudstack.engine.subsystem.api.storage.EndPoint ep) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean revokeAccess(DataObject data,
            org.apache.cloudstack.engine.subsystem.api.storage.EndPoint ep) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Set<DataObject> listObjects(DataStore store) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void createAsync(DataObject data,
            AsyncCompletionCallback<CreateCmdResult> callback) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void deleteAsync(
            DataObject data,
            AsyncCompletionCallback<org.apache.cloudstack.engine.subsystem.api.storage.CommandResult> callback) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void copyAsync(DataObject srcdata, DataObject destData,
            AsyncCompletionCallback<CopyCommandResult> callback) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean canCopy(DataObject srcData, DataObject destData) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void takeSnapshot(
            SnapshotInfo snapshot,
            AsyncCompletionCallback<org.apache.cloudstack.engine.subsystem.api.storage.CommandResult> callback) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void revertSnapshot(
            SnapshotInfo snapshot,
            AsyncCompletionCallback<org.apache.cloudstack.engine.subsystem.api.storage.CommandResult> callback) {
        // TODO Auto-generated method stub
        
    }


	

}
