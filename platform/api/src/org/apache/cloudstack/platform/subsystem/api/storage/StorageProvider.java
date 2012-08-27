package org.apache.cloudstack.platform.subsystem.api.storage;

import java.util.List;
import java.util.Map;

import org.apache.cloudstack.platform.subsystem.api.storage.DataStore.StoreType;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.StoragePool;

public interface StorageProvider {
	List<HypervisorType> supportedHypervisors();
	String getProviderName();
	StoreType supportedStoreType();
	void configure(Map<String, String> storeProviderInfo);
	DataStore createDataStore(HypervisorType hypervisor, DataStoreConfigurator dsc);
	DataStore getDataStore(StoragePool pool);
	Map<HypervisorType, Map<String,DataStoreConfigurator>> getDataStoreConfigs();
}
