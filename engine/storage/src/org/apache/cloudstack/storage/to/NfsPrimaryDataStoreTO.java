package org.apache.cloudstack.storage.to;

import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;

public class NfsPrimaryDataStoreTO extends PrimaryDataStoreTO {
    private String server;
    private String path;
    
    public NfsPrimaryDataStoreTO(PrimaryDataStoreInfo dataStore) {
        super(dataStore);
    }
    
    public void setServer(String server) {
        this.server = server;
    }
    
    public String getServer() {
        return this.server;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public String getPath() {
        return this.path;
    }
}
