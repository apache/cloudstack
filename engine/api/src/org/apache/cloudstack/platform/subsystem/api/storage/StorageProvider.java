package org.apache.cloudstack.platform.subsystem.api.storage;

import java.util.List;
import java.util.Map;

import org.apache.cloudstack.platform.subsystem.api.storage.DataStore.StoreType;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.StoragePool;
import com.cloud.utils.component.Adapter;

public interface StorageProvider extends Adapter {
	List<HypervisorType> supportedHypervisors();
	String getProviderName();
	List<StoreType> supportedStoreTypes();
	void configure(Map<String, String> storeProviderInfo);
	DataStore addDataStore(StoragePool sp, String uri, Map<String, String> params);
	DataStore getDataStore(StoragePool pool);
	Map<HypervisorType, Map<String,DataStoreConfigurator>> getDataStoreConfigs();
}
