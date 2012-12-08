package org.apache.cloudstack.storage.datastore.configurator.xen;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreLifeCycle;
import org.apache.cloudstack.storage.datastore.DefaultPrimaryDataStore;
import org.apache.cloudstack.storage.datastore.PrimaryDataStore;
import org.apache.cloudstack.storage.datastore.configurator.AbstractPrimaryDataStoreConfigurator;
import org.apache.cloudstack.storage.datastore.configurator.PrimaryDataStoreConfigurator;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreVO;
import org.apache.cloudstack.storage.datastore.driver.DefaultPrimaryDataStoreDriverImpl;
import org.apache.cloudstack.storage.datastore.driver.PrimaryDataStoreDriver;
import org.apache.cloudstack.storage.datastore.lifecycle.DefaultXenPrimaryDataStoreLifeCycle;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.exception.CloudRuntimeException;

public abstract class AbstractXenConfigurator extends AbstractPrimaryDataStoreConfigurator {
    @Inject
    PrimaryDataStoreDao dataStoreDao;
    
    @Override
    public HypervisorType getSupportedHypervisor() {
    	return HypervisorType.XenServer;
    }

	protected PrimaryDataStoreLifeCycle getLifeCycle() {
		return new DefaultXenPrimaryDataStoreLifeCycle(dataStoreDao);
	}
    
	protected PrimaryDataStoreDriver getDriver() {
		return new DefaultPrimaryDataStoreDriverImpl();
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
        return dataStore;
    }
}
