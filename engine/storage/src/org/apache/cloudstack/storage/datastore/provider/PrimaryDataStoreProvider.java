package org.apache.cloudstack.storage.datastore.provider;

import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.storage.datastore.PrimaryDataStore;



public interface PrimaryDataStoreProvider {
	public PrimaryDataStore getDataStore(long dataStoreId);
	public PrimaryDataStoreInfo getDataStoreInfo(long dataStoreId);
}
