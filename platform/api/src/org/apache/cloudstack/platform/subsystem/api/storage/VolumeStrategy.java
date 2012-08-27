package org.apache.cloudstack.platform.subsystem.api.storage;

public interface VolumeStrategy {
	Volume createVolume(Volume vol);
	Volume copyVolume(Volume srcVol, Volume destVol);
	Volume createVolumeFromSnapshot(Volume vol, Snapshot snapshot);
	Volume createVolumeFromTemplate(Volume vol, Template template);
	Volume migrateVolume(Volume srcVol, Volume destVol);
	boolean deleteVolume(Volume vol);
}
