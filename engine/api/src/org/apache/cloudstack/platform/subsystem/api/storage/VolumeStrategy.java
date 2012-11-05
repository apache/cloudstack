package org.apache.cloudstack.platform.subsystem.api.storage;


import com.cloud.storage.Volume;

public interface VolumeStrategy {
	Volume createVolume(Volume vol);
	Volume createDataVolume(Volume vol);
	Volume copyVolumeFromBackup(VolumeProfile srcVol, Volume destVol);
	Volume createVolumeFromSnapshot(SnapshotProfile snapshot, Volume vol);
	Volume createVolumeFromTemplate(TemplateProfile template, Volume vol);
	Volume migrateVolume(Volume srcVol, Volume destVol, DataStore destStore);
	Volume createVolumeFromBaseTemplate(Volume destVol, TemplateProfile tp);
	boolean deleteVolume(Volume vol);
	VolumeProfile get(long volumeId);
}
