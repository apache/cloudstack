package org.apache.cloudstack.storage.strategy;

import org.apache.cloudstack.platform.subsystem.api.storage.DataStore;
import org.apache.cloudstack.platform.subsystem.api.storage.Snapshot;
import org.apache.cloudstack.platform.subsystem.api.storage.Template;
import org.apache.cloudstack.platform.subsystem.api.storage.Volume;
import org.apache.cloudstack.platform.subsystem.api.storage.VolumeStrategy;

public class XenVolumeStrategy implements VolumeStrategy {
	protected DataStore _ds;
	public XenVolumeStrategy(DataStore ds) {
		_ds = ds;
	}
	
	public Volume createVolume(Volume vol) {
		// TODO Auto-generated method stub
		return null;
	}

	public Volume copyVolume(Volume srcVol, Volume destVol) {
		// TODO Auto-generated method stub
		return null;
	}

	public Volume createVolumeFromSnapshot(Volume vol, Snapshot snapshot) {
		// TODO Auto-generated method stub
		return null;
	}

	public Volume createVolumeFromTemplate(Volume vol, Template template) {
		// TODO Auto-generated method stub
		return null;
	}

	public Volume migrateVolume(Volume srcVol, Volume destVol) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean deleteVolume(Volume vol) {
		// TODO Auto-generated method stub
		return false;
	}

}
