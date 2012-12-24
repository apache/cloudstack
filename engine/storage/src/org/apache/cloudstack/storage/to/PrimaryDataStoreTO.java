package org.apache.cloudstack.storage.to;

import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;

public class PrimaryDataStoreTO {
    private final String uuid;
    private final String name;
    private final String type;
    private final long id;
    public PrimaryDataStoreTO(PrimaryDataStoreInfo dataStore) {
        this.uuid = dataStore.getUuid();
        this.name = dataStore.getName();
        this.type = dataStore.getType();
        this.id = dataStore.getId();
    }
    
    public long getId() {
        return this.id;
    }
    
    public String getUuid() {
        return this.uuid;
    }
    
    public String getName() {
        return this.name;
    }
    
    public String getType() {
        return this.type;
    }
}
