package org.apache.cloudstack.storage.datastore.provider;

import java.util.List;

import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreProvider;

import com.cloud.utils.component.Manager;

public interface PrimaryDataStoreProviderManager extends Manager {
    public PrimaryDataStoreProvider getDataStoreProvider(Long providerId);
    public PrimaryDataStoreProvider getDataStoreProvider(String name);
    public List<PrimaryDataStoreProvider> getDataStoreProviders();
}
