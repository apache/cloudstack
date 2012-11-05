package org.apache.cloudstack.storage.strategy;

import org.apache.cloudstack.platform.subsystem.api.storage.DataStore;
import org.apache.cloudstack.platform.subsystem.api.storage.SnapshotStrategy;

public class XenSnapshotStrategy implements SnapshotStrategy {
	protected DataStore _ds;
	public XenSnapshotStrategy(DataStore ds) {
		_ds = ds;
	}
}
