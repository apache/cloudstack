package org.apache.cloudstack.platform.subsystem.api.storage;

import java.net.URI;
import java.util.List;
import java.util.Map;

import com.cloud.storage.StoragePool;

public interface DataStoreConfigurator {
	String getProtocol();
	StoragePool getStoragePool(Map<String, String> configs);
	List<String> getConfigNames();
	Map<String, String> getConfigs(URI uri, Map<String, String> extras);
	boolean validate(Map<String, String> configs);
	DataStore getDataStore(StoragePool pool);
}
