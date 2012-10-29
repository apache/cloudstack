package org.apache.cloudstack.storage.datastore.provider;

public interface PrimaryDataStoreProviderManager {
	public PrimaryDataStoreProvider getDataStoreProvider(String providerUuid);
}
