package org.apache.cloudstack.storage.datastore.provider;

import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreProvider;

public interface PrimaryDataStoreProviderManager {
	public PrimaryDataStoreProvider getDataStoreProvider(Long providerId);
}
