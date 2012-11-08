package org.apache.cloudstack.storage.volume;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.cloud.entity.api.SnapshotEntity;
import org.apache.cloudstack.engine.cloud.entity.api.VolumeEntity;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.disktype.VolumeDiskType;
import org.apache.cloudstack.engine.subsystem.api.storage.disktype.VolumeDiskTypeHelper;
import org.apache.cloudstack.engine.subsystem.api.storage.type.VolumeType;
import org.apache.cloudstack.engine.subsystem.api.storage.type.VolumeTypeHelper;
import org.apache.cloudstack.storage.volume.db.VolumeDao;
import org.apache.cloudstack.storage.volume.db.VolumeVO;

import org.apache.log4j.Logger;

import com.cloud.storage.Volume;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;

public class VolumeEntityImpl implements VolumeEntity {
	private static final Logger s_logger = Logger.getLogger(VolumeEntityImpl.class);
	protected VolumeVO volumeVO;
	private StateMachine2<Volume.State, VolumeEvent, VolumeVO> _volStateMachine;
	protected PrimaryDataStore dataStore;
	@Inject
	VolumeDiskTypeHelper diskTypeHelper;
	@Inject
	VolumeTypeHelper volumeTypeHelper;
	@Inject
	VolumeDao volumeDao;
	
	public VolumeEntityImpl(PrimaryDataStore dataStore, VolumeVO volumeVO) {
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
	
	public Volume.State getState() {
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

	@Override
	public long getId() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getExternalId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getCurrentState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDesiredState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Date getCreatedTime() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Date getLastUpdatedTime() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getOwner() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, String> getDetails(String source) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getDetailSources() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addDetail(String source, String name, String value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delDetail(String source, String name, String value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateDetail(String source, String name, String value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<Method> getApplicableActions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SnapshotEntity takeSnapshotOf(boolean full) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String reserveForMigration(long expirationTime) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void migrate(String reservationToken) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public VolumeEntity setupForCopy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void copy(VolumeEntity dest) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void attachTo(String vm, long deviceId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void detachFrom() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}
}
