package org.apache.cloudstack.platform.subsystem.api.storage;


import com.cloud.storage.Volume;

public interface VolumeStrategy {
	Volume createVolume(Volume vol);
	Volume createDataVolume(Volume vol, DataStore store);
	Volume copyVolumeFromBackup(VolumeProfile srcVol, Volume destVol, DataStore destStore);
	Volume createVolumeFromSnapshot(SnapshotProfile snapshot, Volume vol, DataStore destStore);
	Volume createVolumeFromTemplate(TemplateProfile template, Volume vol, DataStore destStore);
	Volume migrateVolume(Volume srcVol, DataStore srcStore, Volume destVol, DataStore destStore);
	TemplateProfile createBaseVolume(TemplateProfile tp, DataStore destStore);
	Volume createVolumeFromBaseTemplate(TemplateProfile tp, DataStore destStore);
	boolean deleteVolume(Volume vol);
}
