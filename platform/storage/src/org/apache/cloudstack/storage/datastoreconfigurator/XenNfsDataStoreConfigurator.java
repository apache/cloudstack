package org.apache.cloudstack.storage.datastoreconfigurator;

import java.util.Map;

import org.apache.cloudstack.platform.subsystem.api.storage.DataStore;
import org.apache.cloudstack.platform.subsystem.api.storage.DataStore.StoreType;
import org.apache.cloudstack.storage.datastore.DefaultDataStore;
import org.apache.cloudstack.storage.datastore.XenDataStoreDriver;
import org.apache.cloudstack.storage.driver.XenServerStorageDriver;
import org.apache.cloudstack.storage.epselector.DefaultPrimaryEndpointSelector;
import org.apache.cloudstack.storage.filesystem.DefaultFileSystem;
import org.apache.cloudstack.storage.lifecycle.DefaultPrimaryDataStoreLifeCycle;
import org.apache.cloudstack.storage.strategy.XenBackupStrategy;
import org.apache.cloudstack.storage.strategy.XenSnapshotStrategy;
import org.apache.cloudstack.storage.strategy.DefaultVolumeStrategy;

import com.cloud.storage.StoragePool;

public class XenNfsDataStoreConfigurator extends NfsDataStoreConfigurator {
	@Override
	public DataStore getDataStore(StoragePool pool) {
		DefaultDataStore ds = new DefaultDataStore();
		ds.setEndPointSelector(new DefaultPrimaryEndpointSelector(ds));
		ds.setId(pool.getId());
		ds.setType(StoreType.Primary);
		ds.setURI(pool.getHostAddress() + "/" + pool.getPath());
		ds.setUUID(pool.getUuid());
		ds.setDataStoreDriver(new XenServerStorageDriver(ds));
		ds.setBackupStrategy(new XenBackupStrategy(ds));
		ds.setVolumeStrategy(new DefaultVolumeStrategy(ds));
		ds.setSnapshotStrategy(new XenSnapshotStrategy(ds));
		ds.setLifeCycle(new DefaultPrimaryDataStoreLifeCycle(ds));
		return ds;
	}

	public StoragePool getStoragePool(Map<String, String> configs) {
		// TODO Auto-generated method stub
		return null;
	}
}
