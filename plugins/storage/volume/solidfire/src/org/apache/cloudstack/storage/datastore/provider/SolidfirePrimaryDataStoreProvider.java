package org.apache.cloudstack.storage.datastore.provider;

import java.util.List;

import org.apache.cloudstack.storage.datastore.DefaultPrimaryDataStore;
import org.apache.cloudstack.storage.datastore.PrimaryDataStore;
import org.apache.cloudstack.storage.datastore.configurator.PrimaryDataStoreConfigurator;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreVO;
import org.apache.cloudstack.storage.datastore.driver.SolidfirePrimaryDataStoreDriver;
import org.apache.cloudstack.storage.datastore.lifecycle.DefaultPrimaryDataStoreLifeCycleImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class SolidfirePrimaryDataStoreProvider extends
	DefaultPrimaryDatastoreProviderImpl {
	private final String name = "Solidfre Primary Data Store Provider";


	public SolidfirePrimaryDataStoreProvider(@Qualifier("solidfire") List<PrimaryDataStoreConfigurator> configurators) {
	    super(configurators);
		
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public PrimaryDataStore getDataStore(long dataStoreId) {
		PrimaryDataStoreVO dsv = dataStoreDao.findById(dataStoreId);
        if (dsv == null) {
            return null;
        }

        DefaultPrimaryDataStore pds = DefaultPrimaryDataStore.createDataStore(dsv);
        SolidfirePrimaryDataStoreDriver driver = new SolidfirePrimaryDataStoreDriver();
        pds.setDriver(driver);
<<<<<<< HEAD
        
        DefaultPrimaryDataStoreLifeCycleImpl lifeCycle = new DefaultPrimaryDataStoreLifeCycleImpl(dataStoreDao);
=======

        DefaultPrimaryDataStoreLifeCycleImpl lifeCycle = new DefaultPrimaryDataStoreLifeCycleImpl(super.dataStoreDao, pds);
>>>>>>> Getting things to compile
        pds.setLifeCycle(lifeCycle);
        return pds;
    }
}
