package org.apache.cloudstack.platform.subsystem.api.storage;

import com.cloud.storage.Volume;

public interface VolumeStrategy {
	Volume createVolume(Volume vol);
	Volume createVolume(Volume vol, DataStore store);
	Volume copyVolume(Volume srcVol, Volume destVol);
	Volume createVolumeFromSnapshot(Volume vol, Snapshot snapshot);
	Volume createVolumeFromTemplate(Volume vol, Template template);
	Volume migrateVolume(Volume srcVol, DataStore srcStore, Volume destVol, DataStore destStore);
	boolean deleteVolume(Volume vol);
}
