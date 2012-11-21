package org.apache.cloudstack.storage.datastore.provider;

import java.util.Map;

import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.storage.datastore.PrimaryDataStore;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreProviderVO;
import org.apache.cloudstack.storage.datastore.lifecycle.PrimaryDataStoreLifeCycle;

public interface PrimaryDataStoreProvider {
    public PrimaryDataStore getDataStore(long dataStoreId);
    public PrimaryDataStoreLifeCycle getDataStoreLifeCycle();
    public PrimaryDataStoreInfo getDataStoreInfo(long dataStoreId);
    public long getId();
    public String getName();
    public boolean register(PrimaryDataStoreProviderVO provider, Map<String, Object> params);
    public boolean init(PrimaryDataStoreProviderVO provider);
}
