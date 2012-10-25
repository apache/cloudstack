package org.apache.cloudstack.storage.volume;

import org.apache.cloudstack.storage.volume.db.VolumeVO;

import com.cloud.utils.fsm.StateObject;

public class Volume implements StateObject<VolumeState>{
	private VolumeVO volumeVO;
	@Override
	public VolumeState getState() {
		// TODO Auto-generated method stub
		return null;
	}

	

}
