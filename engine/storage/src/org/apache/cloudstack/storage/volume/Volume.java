package org.apache.cloudstack.storage.volume;

import javax.inject.Inject;

import org.apache.cloudstack.storage.datastore.PrimaryDataStore;
import org.apache.cloudstack.storage.datastore.PrimaryDataStoreInfo;
import org.apache.cloudstack.storage.volume.db.VolumeDao;
import org.apache.cloudstack.storage.volume.db.VolumeVO;
import org.apache.cloudstack.storage.volume.disktype.VolumeDiskType;
import org.apache.cloudstack.storage.volume.disktype.VolumeDiskTypeHelper;
import org.apache.cloudstack.storage.volume.type.VolumeType;
import org.apache.cloudstack.storage.volume.type.VolumeTypeHelper;
import org.apache.log4j.Logger;

import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;

public class Volume {
	private static final Logger s_logger = Logger.getLogger(Volume.class);
	protected VolumeVO volumeVO;
	private StateMachine2<VolumeState, VolumeEvent, VolumeVO> _volStateMachine;
	protected PrimaryDataStore dataStore;
	@Inject
	VolumeDiskTypeHelper diskTypeHelper;
	@Inject
	VolumeTypeHelper volumeTypeHelper;
	@Inject
	VolumeDao volumeDao;
	
	public Volume(PrimaryDataStore dataStore, VolumeVO volumeVO) {
		this.volumeVO = volumeVO;
		this.dataStore = dataStore;
	}
	
	public String getUuid() {
		return volumeVO.getUuid();
	}
	
	public void setUuid(String uuid) {
		volumeVO.setUuid(uuid);
	}
	
	public String getPath() {
		return volumeVO.getPath();
	}
	
	public String getTemplateUuid() {
		return null;
	}
	
	public String getTemplatePath() {
		return null;
	}
	
	public PrimaryDataStoreInfo getDataStoreInfo() {
		return dataStore.getDataStoreInfo();
	}
	
	public VolumeState getState() {
		return volumeVO.getState();
	}

	public PrimaryDataStore getDataStore() {
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
	
	public long getVolumeId() {
		return volumeVO.getId();
	}
	
	public void setVolumeDiskType(VolumeDiskType type) {
		volumeVO.setDiskType(type.toString());
	}
	
	public boolean stateTransit(VolumeEvent event) {
		boolean result = false;
		try {
			result = _volStateMachine.transitTo(volumeVO, event, null, volumeDao);
		} catch (NoTransitionException e) {
			s_logger.debug("Failed to transit volume: " + this.getVolumeId() + ", due to: " + e.toString());
		}
		return result;
	}
	
	public void update() {
		volumeDao.update(volumeVO.getId(), volumeVO);
		volumeVO = volumeDao.findById(volumeVO.getId());
	}
}
