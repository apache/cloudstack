package org.apache.cloudstack.storage.datastore.configurator.xen;

import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreLifeCycle;
import org.apache.cloudstack.storage.datastore.configurator.AbstractPrimaryDataStoreConfigurator;
import org.apache.cloudstack.storage.datastore.driver.DefaultPrimaryDataStoreDriverImpl;
import org.apache.cloudstack.storage.datastore.driver.PrimaryDataStoreDriver;
import org.apache.cloudstack.storage.datastore.lifecycle.DefaultXenPrimaryDataStoreLifeCycle;

import com.cloud.hypervisor.Hypervisor.HypervisorType;

public abstract class AbstractXenConfigurator extends AbstractPrimaryDataStoreConfigurator {
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
}
