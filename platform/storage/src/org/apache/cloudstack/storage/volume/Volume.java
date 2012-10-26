package org.apache.cloudstack.storage.volume;

import javax.inject.Inject;

import org.apache.cloudstack.platform.subsystem.api.storage.DataStore;
import org.apache.cloudstack.storage.volume.db.VolumeVO;
import org.apache.cloudstack.storage.volume.disktype.VolumeDiskType;
import org.apache.cloudstack.storage.volume.disktype.VolumeDiskTypeHelper;
import org.apache.cloudstack.storage.volume.type.VolumeType;
import org.apache.cloudstack.storage.volume.type.VolumeTypeHelper;

import com.cloud.utils.fsm.StateObject;

public class Volume implements StateObject<VolumeState> {
	protected VolumeVO volumeVO;
	protected DataStore dataStore;
	@Inject
	VolumeDiskTypeHelper diskTypeHelper;
	@Inject
	VolumeTypeHelper volumeTypeHelper;
	
	public Volume(DataStore dataStore, VolumeVO volumeVO) {
		this.volumeVO = volumeVO;
		this.dataStore = dataStore;
	}
	
	@Override
	public VolumeState getState() {
		return volumeVO.getState();
	}

	public DataStore getDataStore() {
		return dataStore;
	}
	
	public long getSize() {
		return volumeVO.getSize();
	}
	
	public VolumeDiskType getDiskType() {
		return diskTypeHelper.getDiskType(volumeVO.getDiskType());
	}
	
	public VolumeType getType() {
		return volumeTypeHelper.getType(volumeVO.getVolumeType());
	}
}
