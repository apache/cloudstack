package org.apache.cloudstack.platform.subsystem.api.storage;

public interface DataStoreLifeCycle {
	public enum DataStoreEvent {
		HOSTUP,
		HOSTDOWN,
	}
	void add();
	void delete();
	void enable();
	void disable();
	void processEvent(DataStoreEvent event, Object... objs);
}
