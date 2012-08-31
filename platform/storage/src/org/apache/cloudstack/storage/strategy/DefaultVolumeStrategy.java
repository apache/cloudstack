package org.apache.cloudstack.storage.strategy;

import org.apache.cloudstack.platform.subsystem.api.storage.DataStore;
import org.apache.cloudstack.platform.subsystem.api.storage.SnapshotProfile;
import org.apache.cloudstack.platform.subsystem.api.storage.TemplateProfile;
import org.apache.cloudstack.platform.subsystem.api.storage.VolumeProfile;
import org.apache.cloudstack.platform.subsystem.api.storage.VolumeStrategy;

import com.cloud.storage.Volume;

public class DefaultVolumeStrategy implements VolumeStrategy {
	protected DataStore _ds;
	public DefaultVolumeStrategy(DataStore ds) {
		_ds = ds;
	}
	
	public Volume createVolume(Volume vol) {
		// TODO Auto-generated method stub
		return null;
	}
	public Volume createDataVolume(Volume vol, DataStore store) {
		// TODO Auto-generated method stub
		return null;
	}
	public Volume copyVolumeFromBackup(VolumeProfile srcVol, Volume destVol, DataStore destStore) {
		// TODO Auto-generated method stub
		return null;
	}
	public Volume createVolumeFromSnapshot(SnapshotProfile snapshot, Volume vol, DataStore destStore) {
		// TODO Auto-generated method stub
		return null;
	}
	public Volume createVolumeFromTemplate(TemplateProfile template, Volume vol, DataStore destStore) {
		// TODO Auto-generated method stub
		return null;
	}
	public Volume migrateVolume(Volume srcVol, DataStore srcStore, Volume destVol, DataStore destStore) {
		// TODO Auto-generated method stub
		return null;
	}
	public TemplateProfile createBaseVolume(TemplateProfile tp, DataStore destStore) {
		// TODO Auto-generated method stub
		return null;
	}
	public Volume createVolumeFromBaseTemplate(TemplateProfile tp, DataStore destStore) {
		// TODO Auto-generated method stub
		return null;
	}
	public boolean deleteVolume(Volume vol) {
		// TODO Auto-generated method stub
		return false;
	}



}
