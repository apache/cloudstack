package org.apache.cloudstack.storage.datastore.configurator;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreLifeCycle;
import org.apache.cloudstack.storage.datastore.DefaultPrimaryDataStore;
import org.apache.cloudstack.storage.datastore.PrimaryDataStore;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreVO;
import org.apache.cloudstack.storage.datastore.driver.PrimaryDataStoreDriver;
import com.cloud.utils.exception.CloudRuntimeException;

public abstract class AbstractPrimaryDataStoreConfigurator implements PrimaryDataStoreConfigurator {
    @Inject
    protected PrimaryDataStoreDao dataStoreDao;
    
    protected abstract PrimaryDataStoreLifeCycle getLifeCycle();
    
    protected abstract PrimaryDataStoreDriver getDriver();
    
    protected abstract boolean isLocalStorageSupported();

    @Override
    public PrimaryDataStore getDataStore(long dataStoreId) {
        PrimaryDataStoreVO dataStoreVO = dataStoreDao.findById(dataStoreId);
        if (dataStoreVO == null) {
            throw new CloudRuntimeException("Can't find primary data store: " + dataStoreId);
        }
        
        DefaultPrimaryDataStore dataStore = DefaultPrimaryDataStore.createDataStore(dataStoreVO);
        dataStore.setDriver(getDriver());
        dataStore.setLifeCycle(getLifeCycle());
        dataStore.setSupportedHypervisor(getSupportedHypervisor());
        dataStore.setLocalStorageFlag(isLocalStorageSupported());
        dataStore.setProtocolTransFormer(getProtocolTransformer());
        return dataStore;
    }
}
