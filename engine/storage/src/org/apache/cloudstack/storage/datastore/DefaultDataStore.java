package org.apache.cloudstack.storage.datastore;

import org.apache.cloudstack.platform.subsystem.api.storage.BackupStrategy;
import org.apache.cloudstack.platform.subsystem.api.storage.DataStore;
import org.apache.cloudstack.platform.subsystem.api.storage.DataStoreDriver;
import org.apache.cloudstack.platform.subsystem.api.storage.DataStoreEndPointSelector;
import org.apache.cloudstack.platform.subsystem.api.storage.DataStoreLifeCycle;
import org.apache.cloudstack.platform.subsystem.api.storage.FileSystem;
import org.apache.cloudstack.platform.subsystem.api.storage.SnapshotProfile;
import org.apache.cloudstack.platform.subsystem.api.storage.SnapshotStrategy;
import org.apache.cloudstack.platform.subsystem.api.storage.StorageProvider;
import org.apache.cloudstack.platform.subsystem.api.storage.TemplateProfile;
import org.apache.cloudstack.platform.subsystem.api.storage.TemplateStrategy;
import org.apache.cloudstack.platform.subsystem.api.storage.VolumeProfile;
import org.apache.cloudstack.platform.subsystem.api.storage.VolumeStrategy;

import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.storage.Snapshot;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.Volume;

public class DefaultDataStore implements DataStore {
	protected VolumeStrategy _volumeStrategy;
	protected SnapshotStrategy _snapshotStrategy;
	protected BackupStrategy _backupStrategy;
	protected TemplateStrategy _templateStrategy;
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
	
	public TemplateStrategy getTemplateStrategy() {
		return this._templateStrategy;
	}
	
	public void setTemplateStrategy(TemplateStrategy ts) {
		this._templateStrategy = ts;
	}

	public DataStoreLifeCycle getLifeCycle() {
		return this._dslf;
	}
	
	public void setLifeCycle(DataStoreLifeCycle lf) {
		this._dslf = lf;
	}

	public long getCluterId() {
		// TODO Auto-generated method stub
		return 0;
	}

	public long getPodId() {
		// TODO Auto-generated method stub
		return 0;
	}

	public long getZoneId() {
		// TODO Auto-generated method stub
		return 0;
	}

	public String getPath() {
		// TODO Auto-generated method stub
		return null;
	}

	public StoragePoolType getPoolType() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isSharedStorage() {
		// TODO Auto-generated method stub
		return false;
	}

	public StorageProvider getProvider() {
		// TODO Auto-generated method stub
		return null;
	}

	public DataStoreEndPointSelector getEndPointSelector() {
		// TODO Auto-generated method stub
		return null;
	}

	public VolumeProfile prepareVolume(Volume volume, DataStore destStore) {
		// TODO Auto-generated method stub
		return null;
	}

	public SnapshotProfile prepareSnapshot(Snapshot snapshot, DataStore destStore) {
		// TODO Auto-generated method stub
		return null;
	}

	public TemplateProfile prepareTemplate(long templateId, DataStore destStore) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean contains(Volume volume) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean contains(Snapshot snapshot) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean contains(TemplateProfile template) {
		// TODO Auto-generated method stub
		return false;
	}

	public TemplateProfile get(TemplateProfile template) {
		// TODO Auto-generated method stub
		return null;
	}

	public StorageFilerTO getTO() {
		// TODO Auto-generated method stub
		return null;
	}
}
