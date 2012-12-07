package org.apache.cloudstack.engine.subsystem.api.storage;

import java.util.Map;

public interface PrimaryDataStoreProvider {
    public PrimaryDataStoreInfo getDataStore(long dataStoreId);
    public long getId();
    public String getName();

    /**
     * @param dsInfos
     * @return
     */
    PrimaryDataStoreInfo registerDataStore(Map<String, String> dsInfos);
    
    //LifeCycle of provider
    public boolean configure();
}
