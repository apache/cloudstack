package org.apache.cloudstack.storage.command;

import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;

import com.cloud.agent.api.Command;

public class CreatePrimaryDataStoreCmd extends Command implements StorageSubSystemCommand {
    private final PrimaryDataStoreTO dataStore;
    public CreatePrimaryDataStoreCmd(PrimaryDataStoreTO dataStore) {
        this.dataStore = dataStore;
    }
    
    public PrimaryDataStoreTO getDataStore() {
        return this.dataStore;
    }
    
    @Override
    public boolean executeInSequence() {
        // TODO Auto-generated method stub
        return false;
    }

}
