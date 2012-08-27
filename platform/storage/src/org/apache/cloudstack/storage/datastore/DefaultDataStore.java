package org.apache.cloudstack.storage.datastore;

import org.apache.cloudstack.platform.subsystem.api.storage.BackupStrategy;
import org.apache.cloudstack.platform.subsystem.api.storage.DataStore;
import org.apache.cloudstack.platform.subsystem.api.storage.DataStoreDriver;
import org.apache.cloudstack.platform.subsystem.api.storage.DataStoreEndPointSelector;
import org.apache.cloudstack.platform.subsystem.api.storage.DataStoreLifeCycle;
import org.apache.cloudstack.platform.subsystem.api.storage.FileSystem;
import org.apache.cloudstack.platform.subsystem.api.storage.SnapshotStrategy;
import org.apache.cloudstack.platform.subsystem.api.storage.VolumeStrategy;

public class DefaultDataStore implements DataStore {
	protected VolumeStrategy _volumeStrategy;
	protected SnapshotStrategy _snapshotStrategy;
	protected BackupStrategy _backupStrategy;
	protected String _uri;
	protected String _uuid;
	protected StoreType _type;
	protected StoreScope _scope;
	protected long _poolId;
	protected DataStoreDriver _driverRef;
	protected DataStoreEndPointSelector _selector;
	protected FileSystem _fs;
	protected VolumeStrategy _volumeSt;
	protected SnapshotStrategy _snapshotSt;
	protected BackupStrategy _backupSt;
	protected long _id;
	protected DataStoreLifeCycle _dslf;
	
	public DefaultDataStore(
	) {
	}

	public String getURI() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void setURI(String uri) {
		this._uri = uri;
	}

	public String getUUID() {
		return this._uuid;
	}
	
	public void setUUID(String uuid) {
		this._uuid = uuid;
	}

	public StoreType getType() {
		return this._type;
	}
	
	public void setType(StoreType type) {
		this._type = type;
	}

	public StoreScope getScope() {
		return this._scope;
	}
	
	public void setScope(StoreScope scope) {
		this._scope = scope;
	}

	public Long getId() {
		return this._id;
	}
	
	public void setId(long id) {
		this._id = id;
	}

	public DataStoreDriver getDataStoreDriver() {
		return this._driverRef;
	}
	
	public void setDataStoreDriver(DataStoreDriver drv) {
		this._driverRef = drv;
	}

	public void setEndPointSelector(DataStoreEndPointSelector selector) {
		this._selector = selector;
	}
	
	public DataStoreEndPointSelector getSelector() {
		return this._selector;
	}

	public FileSystem getFileSystem() {
		return this._fs;
	}
	
	public void setFileSystem(FileSystem fs) {
		this._fs = fs;
	}

	public VolumeStrategy getVolumeStrategy() {
		return this._volumeSt;
	}
	
	public void setVolumeStrategy(VolumeStrategy vs) {
		this._volumeSt = vs;
	}

	public SnapshotStrategy getSnapshotStrategy() {
		return this._snapshotSt;
	}
	
	public void setSnapshotStrategy(SnapshotStrategy ss) {
		this._snapshotSt = ss;
	}

	public BackupStrategy getBackupStrategy() {
		return this._backupSt;
	}
	
	public void setBackupStrategy(BackupStrategy bs) {
		this._backupSt = bs;
	}

	public DataStoreLifeCycle getLifeCycle() {
		return this._dslf;
	}
	
	public void setLifeCycle(DataStoreLifeCycle lf) {
		this._dslf = lf;
	}
}
