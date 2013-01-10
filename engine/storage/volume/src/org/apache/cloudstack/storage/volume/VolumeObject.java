package org.apache.cloudstack.storage.volume;

import java.util.Date;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.disktype.VolumeDiskType;
import org.apache.cloudstack.engine.subsystem.api.storage.disktype.VolumeDiskTypeHelper;
import org.apache.cloudstack.engine.subsystem.api.storage.type.VolumeType;
import org.apache.cloudstack.engine.subsystem.api.storage.type.VolumeTypeHelper;
import org.apache.cloudstack.storage.datastore.PrimaryDataStore;
import org.apache.cloudstack.storage.volume.db.VolumeDao2;
import org.apache.cloudstack.storage.volume.db.VolumeVO;
import org.apache.log4j.Logger;

import com.cloud.storage.Volume;
import com.cloud.storage.Volume.State;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;

public class VolumeObject implements VolumeInfo {
    private static final Logger s_logger = Logger.getLogger(VolumeObject.class);
    protected VolumeVO volumeVO;
    private StateMachine2<Volume.State, Volume.Event, VolumeVO> _volStateMachine;
    protected PrimaryDataStore dataStore;
    @Inject
    VolumeDiskTypeHelper diskTypeHelper;
    @Inject
    VolumeTypeHelper volumeTypeHelper;
    @Inject
    VolumeDao2 volumeDao;
    @Inject
    VolumeManager volumeMgr;
    private VolumeObject(PrimaryDataStore dataStore, VolumeVO volumeVO) {
        this.volumeVO = volumeVO;
        this.dataStore = dataStore;
    }

    public static VolumeObject getVolumeObject(PrimaryDataStore dataStore, VolumeVO volumeVO) {
        VolumeObject vo = new VolumeObject(dataStore, volumeVO);
        vo = ComponentContext.inject(vo);
        return vo;
    }

    @Override
    public String getUuid() {
        return volumeVO.getUuid();
    }

    public void setPath(String uuid) {
        volumeVO.setUuid(uuid);
    }

    @Override
    public String getPath() {
        return volumeVO.getPath();
    }

    @Override
    public String getTemplateUuid() {
        return null;
    }

    @Override
    public String getTemplatePath() {
        return null;
    }

    public PrimaryDataStoreInfo getDataStoreInfo() {
        return dataStore;
    }

    public Volume.State getState() {
        return volumeVO.getState();
    }

    @Override
    public PrimaryDataStore getDataStore() {
        return dataStore;
    }

    @Override
    public long getSize() {
        return volumeVO.getSize();
    }

    @Override
    public VolumeDiskType getDiskType() {
        return diskTypeHelper.getDiskType(volumeVO.getDiskType());
    }

    @Override
    public VolumeType getType() {
        return volumeTypeHelper.getType(volumeVO.getVolumeType());
    }

    public long getVolumeId() {
        return volumeVO.getId();
    }

    public void setVolumeDiskType(VolumeDiskType type) {
        volumeVO.setDiskType(type.toString());
    }

    public boolean stateTransit(Volume.Event event) {
        boolean result = false;
        _volStateMachine = volumeMgr.getStateMachine();
        try {
            result = _volStateMachine.transitTo(volumeVO, event, null, volumeDao);
        } catch (NoTransitionException e) {
            String errorMessage = "Failed to transit volume: " + this.getVolumeId() + ", due to: " + e.toString();
            s_logger.debug(errorMessage);
            throw new CloudRuntimeException(errorMessage);
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
    public State getCurrentState() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public State getDesiredState() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Date getCreatedDate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Date getUpdatedDate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getOwner() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getName() {
        return this.volumeVO.getName();
    }

    @Override
    public boolean isAttachedVM() {
        return (this.volumeVO.getInstanceId() == null) ? false : true;
    }
}
