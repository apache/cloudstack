package org.apache.cloudstack.storage.lifecycle;

import org.apache.cloudstack.platform.subsystem.api.storage.DataStore;
import org.apache.cloudstack.platform.subsystem.api.storage.DataStoreLifeCycle;

public class DefaultDataStoreLifeCycle implements DataStoreLifeCycle {
	private DataStore _ds;
	public DefaultDataStoreLifeCycle(DataStore ds) {
		this._ds = ds;
	}
	
	
	public void create() {
		// TODO Auto-generated method stub

	}

	public void delete() {
		// TODO Auto-generated method stub

	}

	public void enable() {
		// TODO Auto-generated method stub

	}

	public void disable() {
		// TODO Auto-generated method stub

	}

	public void processEvent(DataStoreEvent event, Object... objs) {
		// TODO Auto-generated method stub

	}

}
