package org.apache.cloudstack.storage.strategy;

import java.util.List;

import org.apache.cloudstack.platform.subsystem.api.storage.DataStore;
import org.apache.cloudstack.platform.subsystem.api.storage.DataStoreDriver;

import org.apache.cloudstack.platform.subsystem.api.storage.DataStoreEndPoint;
import org.apache.cloudstack.platform.subsystem.api.storage.DataStoreEndPointSelector;
import org.apache.cloudstack.platform.subsystem.api.storage.SnapshotProfile;
import org.apache.cloudstack.platform.subsystem.api.storage.StorageEvent;
import org.apache.cloudstack.platform.subsystem.api.storage.TemplateProfile;
import org.apache.cloudstack.platform.subsystem.api.storage.VolumeProfile;
import org.apache.cloudstack.platform.subsystem.api.storage.VolumeStrategy;
import org.apache.cloudstack.storage.volume.VolumeManager;

import org.apache.log4j.Logger;


import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.utils.component.Inject;


public class DefaultVolumeStrategy implements VolumeStrategy {
	private static final Logger s_logger = Logger.getLogger(DefaultVolumeStrategy.class);
	protected DataStore _ds;
	protected DataStoreDriver _driver;
	@Inject
	VolumeManager _volumeMgr;
	
	public VolumeProfile get(long volumeId) {
		return _volumeMgr.getProfile(volumeId);
	}

	public DefaultVolumeStrategy(DataStore ds) {
		_ds = ds;
	}
	public Volume createVolume(Volume vol) {
		// TODO Auto-generated method stub
		return null;
	}
	public Volume createDataVolume(Volume vol) {
		// TODO Auto-generated method stub
		return null;
	}
	public Volume copyVolumeFromBackup(VolumeProfile srcVol, Volume destVol) {
		// TODO Auto-generated method stub
		return null;
	}
	public Volume createVolumeFromSnapshot(SnapshotProfile snapshot, Volume vol) {
		// TODO Auto-generated method stub
		return null;
	}
	public Volume createVolumeFromTemplate(TemplateProfile template, Volume vol) {
		// TODO Auto-generated method stub
		return null;
	}
	public Volume migrateVolume(Volume srcVol, Volume destVol, DataStore destStore) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public Volume createVolumeFromBaseTemplate(Volume volume, TemplateProfile tp) {
		DataStoreEndPointSelector dsep = _ds.getEndPointSelector();
		List<DataStoreEndPoint> dseps = dsep.getEndPoints(StorageEvent.CreateVolumeFromTemplate);
		DataStoreEndPoint dp = dseps.get(0);
		
		VolumeProfile vp = _driver.createVolumeFromTemplate(get(volume.getId()), tp, dp);
		
		VolumeVO vlvo = _volumeMgr.getVolume(volume.getId());
		
		vlvo.setFolder(_ds.getPath());
		vlvo.setPath(vp.getPath());
		vlvo.setSize(vp.getSize());
		vlvo.setPoolType(_ds.getPoolType());
		vlvo.setPoolId(_ds.getId());
		vlvo.setPodId(_ds.getPodId());
		
		return _volumeMgr.updateVolume(vlvo);
	}
	public boolean deleteVolume(Volume vol) {
		// TODO Auto-generated method stub
		return false;
	}
	
	


}
