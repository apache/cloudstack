package org.apache.cloudstack.storage.datastore.provider;

import org.apache.cloudstack.storage.datastore.PrimaryDataStore;

public interface PrimaryDataStoreProvider {
	public PrimaryDataStore getDataStore(String dataStoreId);
}
