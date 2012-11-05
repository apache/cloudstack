package org.apache.cloudstack.storage.datastore.provider;

import javax.inject.Inject;

import org.apache.cloudstack.storage.datastore.DefaultPrimaryDataStoreImpl;
import org.apache.cloudstack.storage.datastore.PrimaryDataStore;
import org.apache.cloudstack.storage.datastore.PrimaryDataStoreInfo;
import org.apache.cloudstack.storage.datastore.db.DataStoreVO;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.driver.DefaultPrimaryDataStoreDriverImpl;
import org.apache.cloudstack.storage.datastore.driver.PrimaryDataStoreDriver;
import org.springframework.stereotype.Component;

import com.cloud.utils.component.ComponentInject;

@Component
public class DefaultPrimaryDatastoreProviderImpl implements
		PrimaryDataStoreProvider {
	protected PrimaryDataStoreDriver driver;
	@Inject
	public PrimaryDataStoreDao dataStoreDao;

	public DefaultPrimaryDatastoreProviderImpl() {
		this.driver = new DefaultPrimaryDataStoreDriverImpl();
	}
	@Override
	public PrimaryDataStore getDataStore(long dataStoreId) {
		DataStoreVO dsv = dataStoreDao.findById(dataStoreId);
		if (dsv == null) {
			return null;
		}
		
		PrimaryDataStore pds = new DefaultPrimaryDataStoreImpl(driver, dsv, null);
		pds = ComponentInject.inject(pds);
		return pds;
	}
	
	@Override
	public PrimaryDataStoreInfo getDataStoreInfo(long dataStoreId) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
