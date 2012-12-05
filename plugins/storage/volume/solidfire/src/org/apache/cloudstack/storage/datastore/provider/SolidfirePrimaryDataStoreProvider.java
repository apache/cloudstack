package org.apache.cloudstack.storage.datastore.provider;

import javax.inject.Inject;

import org.apache.cloudstack.storage.datastore.DefaultPrimaryDataStore;
import org.apache.cloudstack.storage.datastore.PrimaryDataStore;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreVO;
import org.apache.cloudstack.storage.datastore.driver.SolidfirePrimaryDataStoreDriver;
import org.springframework.stereotype.Component;

@Component
public class SolidfirePrimaryDataStoreProvider extends
	DefaultPrimaryDatastoreProviderImpl {
	private final String name = "Solidfre Primary Data Store Provider";
	private SolidfirePrimaryDataStoreDriver driver;
	
	@Inject
	public SolidfirePrimaryDataStoreProvider(PrimaryDataStoreDao dataStoreDao) {
		super(dataStoreDao);
		driver = new SolidfirePrimaryDataStoreDriver();
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

        PrimaryDataStore pds = DefaultPrimaryDataStore.createDataStore(driver, dsv, null);
        return pds;
	}
}
