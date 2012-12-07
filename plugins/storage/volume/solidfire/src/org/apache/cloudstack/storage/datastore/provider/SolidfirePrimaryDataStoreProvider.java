package org.apache.cloudstack.storage.datastore.provider;

import org.apache.cloudstack.storage.datastore.DefaultPrimaryDataStore;
import org.apache.cloudstack.storage.datastore.PrimaryDataStore;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreVO;
import org.apache.cloudstack.storage.datastore.driver.SolidfirePrimaryDataStoreDriver;
import org.apache.cloudstack.storage.datastore.lifecycle.DefaultPrimaryDataStoreLifeCycleImpl;
import org.springframework.stereotype.Component;

@Component
public class SolidfirePrimaryDataStoreProvider extends
	DefaultPrimaryDatastoreProviderImpl {
	private final String name = "Solidfre Primary Data Store Provider";


	public SolidfirePrimaryDataStoreProvider() {
	    super();
		
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
        
        DefaultPrimaryDataStoreLifeCycleImpl lifeCycle = new DefaultPrimaryDataStoreLifeCycleImpl(super.dataStoreDao, pds);
        pds.setLifeCycle(lifeCycle);
        return pds;
	}
}
