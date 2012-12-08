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
    PrimaryDataStoreDao dataStoreDao;
    
    protected PrimaryDataStoreLifeCycle getLifeCycle() {
        return null;
    }
    
    protected PrimaryDataStoreDriver getDriver() {
        return null;
    }
    
    protected boolean isLocalStorageSupported() {
        return false;
    }

    @Override
    public PrimaryDataStore getDataStore(long dataStoreId) {
        PrimaryDataStoreVO dataStoreVO = dataStoreDao.findById(dataStoreId);
        if (dataStoreVO == null) {
            throw new CloudRuntimeException("Can't find primary data store: " + dataStoreId);
        }
        
        DefaultPrimaryDataStore dataStore = DefaultPrimaryDataStore.createDataStore(dataStoreVO);
        dataStore.setDriver(this.getDriver());
        dataStore.setLifeCycle(getLifeCycle());
        dataStore.setSupportedHypervisor(getSupportedHypervisor());
        dataStore.setLocalStorageFlag(isLocalStorageSupported());
        return dataStore;
    }
}
