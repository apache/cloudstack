package org.apache.cloudstack.storage.datastoreconfigurator;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.cloudstack.platform.subsystem.api.storage.DataStore;
import org.apache.cloudstack.platform.subsystem.api.storage.DataStoreConfigurator;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.Storage.StoragePoolType;

public abstract class NfsDataStoreConfigurator implements DataStoreConfigurator {
	private enum NfsConfigName {
		SERVER,
		PORT,
		PATH;
	}
	
	public NfsDataStoreConfigurator() {
		
	}
	
	public String getProtocol() {
		return "NFS";
	}

	public Map<String, String> getConfigs(URI uri, Map<String, String> extras) {
		Map<String, String> configs = new HashMap<String, String>();
		String storageHost = uri.getHost();
		String hostPath = uri.getPath();
		configs.put(NfsConfigName.SERVER.toString(), storageHost);
		configs.put(NfsConfigName.PATH.toString(), hostPath);
		configs.putAll(extras);
		
		return configs;
	}

	public List<String> getConfigNames() {
		List<String> names = new ArrayList<String>();
		names.add(NfsConfigName.SERVER.toString());
		names.add(NfsConfigName.PATH.toString());
		return names;
	}

	public boolean validate(Map<String, String> configs) {
		  String uriHost = configs.get(NfsConfigName.SERVER.toString());
          String uriPath = configs.get(NfsConfigName.PATH.toString());
          if (uriHost == null || uriPath == null || uriHost.trim().isEmpty() || uriPath.trim().isEmpty()) {
              throw new InvalidParameterValueException("host or path is null, should be nfs://hostname/path");
          }
          return true;
	}

	public DataStore getDataStore(StoragePool pool) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
