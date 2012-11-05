package org.apache.cloudstack.storage;

import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.cloudstack.platform.subsystem.api.storage.DataStore;
import org.apache.cloudstack.platform.subsystem.api.storage.StorageProvider;

import com.cloud.utils.component.Manager;

public class StorageProviderManagerImpl implements StorageProviderManager, Manager {
	
	public StorageProvider getProvider(String uuid) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean start() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean stop() {
		// TODO Auto-generated method stub
		return false;
	}

	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	public StorageProvider getProvider(long poolId) {
		// TODO Auto-generated method stub
		return null;
	}

	public StorageProvider getBackupStorageProvider(long zoneId) {
		// TODO Auto-generated method stub
		return null;
	}

	public DataStore getDataStore(long poolId) {
		// TODO Auto-generated method stub
		return null;
	}

}
