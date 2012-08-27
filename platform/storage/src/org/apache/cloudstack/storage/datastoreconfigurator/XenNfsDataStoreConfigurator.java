package org.apache.cloudstack.storage.datastoreconfigurator;

import org.apache.cloudstack.platform.subsystem.api.storage.DataStore;
import org.apache.cloudstack.platform.subsystem.api.storage.DataStore.StoreType;
import org.apache.cloudstack.storage.datastore.DefaultDataStore;
import org.apache.cloudstack.storage.datastore.XenDataStoreDriver;
import org.apache.cloudstack.storage.epselector.DefaultPrimaryEndpointSelector;
import org.apache.cloudstack.storage.filesystem.DefaultFileSystem;
import org.apache.cloudstack.storage.lifecycle.DefaultDataStoreLifeCycle;
import org.apache.cloudstack.storage.strategy.XenBackupStrategy;
import org.apache.cloudstack.storage.strategy.XenSnapshotStrategy;
import org.apache.cloudstack.storage.strategy.XenVolumeStrategy;

import com.cloud.storage.StoragePool;

public class XenNfsDataStoreConfigurator extends NfsDataStoreConfigurator {
	@Override
	public DataStore getDataStore(StoragePool pool) {
		DefaultDataStore ds = new DefaultDataStore();
		ds.setBackupStrategy(new XenBackupStrategy());
		ds.setVolumeStrategy(new XenVolumeStrategy());
		ds.setSnapshotStrategy(new XenSnapshotStrategy());
		ds.setEndPointSelector(new DefaultPrimaryEndpointSelector());
		ds.setFileSystem(new DefaultFileSystem());
		ds.setId(pool.getId());
		ds.setType(StoreType.Primary);
		ds.setURI(pool.getHostAddress() + "/" + pool.getPath());
		ds.setUUID(pool.getUuid());
		ds.setDataStoreDriver(new XenDataStoreDriver());
		ds.setLifeCycle(new DefaultDataStoreLifeCycle(ds));
		return ds;
	}
}
