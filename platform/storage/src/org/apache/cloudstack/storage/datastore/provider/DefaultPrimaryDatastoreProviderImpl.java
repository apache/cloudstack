package org.apache.cloudstack.storage.datastore.provider;

import javax.inject.Inject;

import org.apache.cloudstack.storage.datastore.PrimaryDataStore;
import org.apache.cloudstack.storage.datastore.driver.PrimaryDataStoreDriver;

public class DefaultPrimaryDatastoreProviderImpl implements
		PrimaryDataStoreProvider {
	@Inject
	public PrimaryDataStoreDriver driver;
	@Override
	public PrimaryDataStore getDataStore(String dataStoreId) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
