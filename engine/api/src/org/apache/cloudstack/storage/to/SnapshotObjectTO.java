package org.apache.cloudstack.storage.to;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectType;
import org.apache.cloudstack.engine.subsystem.api.storage.DataTO;

import com.cloud.agent.api.to.DataStoreTO;

public class SnapshotObjectTO implements DataTO {
	private String path;
	@Override
	public DataObjectType getObjectType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataStoreTO getDataStore() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPath() {
		return this.path;
	}
	
	public void setPath(String path) {
		this.path = path;
	}
}
