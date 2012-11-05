package org.apache.cloudstack.storage.datastore.provider;

import org.apache.cloudstack.storage.datastore.PrimaryDataStore;
import org.apache.cloudstack.storage.datastore.PrimaryDataStoreInfo;

public interface PrimaryDataStoreProvider {
	public PrimaryDataStore getDataStore(long dataStoreId);
	public PrimaryDataStoreInfo getDataStoreInfo(long dataStoreId);
}
