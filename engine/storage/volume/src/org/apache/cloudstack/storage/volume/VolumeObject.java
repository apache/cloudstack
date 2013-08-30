// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.storage.volume;

import java.util.Date;

import javax.inject.Inject;

import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.dao.DiskOfferingDao;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectInStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.cloudstack.storage.command.CreateObjectAnswer;
import org.apache.cloudstack.storage.datastore.ObjectInDataStoreManager;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreVO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.DownloadAnswer;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataTO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.utils.storage.encoding.EncodingType;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;

public class VolumeObject implements VolumeInfo {
    private static final Logger s_logger = Logger.getLogger(VolumeObject.class);
    protected VolumeVO volumeVO;
    private StateMachine2<Volume.State, Volume.Event, Volume> _volStateMachine;
    protected DataStore dataStore;
    @Inject
    VolumeDao volumeDao;
    @Inject
    VolumeDataStoreDao volumeStoreDao;
    @Inject
    ObjectInDataStoreManager objectInStoreMgr;
    @Inject
    VMInstanceDao vmInstanceDao;
    @Inject
    DiskOfferingDao diskOfferingDao;
    private Object payload;

    public VolumeObject() {
        _volStateMachine = Volume.State.getStateMachine();
    }

    protected void configure(DataStore dataStore, VolumeVO volumeVO) {
        this.volumeVO = volumeVO;
        this.dataStore = dataStore;
    }

    public static VolumeObject getVolumeObject(DataStore dataStore, VolumeVO volumeVO) {
        VolumeObject vo = ComponentContext.inject(VolumeObject.class);
        vo.configure(dataStore, volumeVO);
        return vo;
    }

    @Override
    public String getAttachedVmName() {
        Long vmId = this.volumeVO.getInstanceId();
        if (vmId != null) {
            VMInstanceVO vm = vmInstanceDao.findById(vmId);

            if (vm == null) {
                return null;
            }
            return vm.getInstanceName();
        }
        return null;
    }

    @Override
    public String getUuid() {
        return volumeVO.getUuid();
    }

    public void setUuid(String uuid) {
        volumeVO.setUuid(uuid);
    }

    @Override
    public String get_iScsiName() {
        return volumeVO.get_iScsiName();
    }

    public void setSize(Long size) {
        volumeVO.setSize(size);
    }

    @Override
    public Volume.State getState() {
        return volumeVO.getState();
    }

    @Override
    public DataStore getDataStore() {
        return dataStore;
    }

    @Override
    public Long getSize() {
        return volumeVO.getSize();
    }

    @Override
    public Long getMinIops() {
        return volumeVO.getMinIops();
    }

    @Override
    public Long getMaxIops() {
        return volumeVO.getMaxIops();
    }

    public long getVolumeId() {
        return volumeVO.getId();
    }

    @Override
    public boolean stateTransit(Volume.Event event) {
        boolean result = false;
        try {
            volumeVO = volumeDao.findById(volumeVO.getId());
            result = _volStateMachine.transitTo(volumeVO, event, null, volumeDao);
            volumeVO = volumeDao.findById(volumeVO.getId());
        } catch (NoTransitionException e) {
            String errorMessage = "Failed to transit volume: " + this.getVolumeId() + ", due to: " + e.toString();
            s_logger.debug(errorMessage);
            throw new CloudRuntimeException(errorMessage);
        }
        return result;
    }

    private DiskOfferingVO getDiskOfferingVO() {
        if (getDiskOfferingId() != null) {
            DiskOfferingVO diskOfferingVO = diskOfferingDao.findById(getDiskOfferingId());
            return diskOfferingVO;
        }
        return null;
    }

    @Override
    public Long getBytesReadRate() {
        DiskOfferingVO diskOfferingVO = getDiskOfferingVO();
        if (diskOfferingVO != null) {
            return diskOfferingVO.getBytesReadRate();
        }
        return null;
    }

    @Override
    public Long getBytesWriteRate() {
        DiskOfferingVO diskOfferingVO = getDiskOfferingVO();
        if (diskOfferingVO != null) {
            return diskOfferingVO.getBytesWriteRate();
        }
        return null;
    }

    @Override
    public Long getIopsReadRate() {
        DiskOfferingVO diskOfferingVO = getDiskOfferingVO();
        if (diskOfferingVO != null) {
            return diskOfferingVO.getIopsReadRate();
        }
        return null;
    }

    @Override
    public Long getIopsWriteRate() {
        DiskOfferingVO diskOfferingVO = getDiskOfferingVO();
        if (diskOfferingVO != null) {
            return diskOfferingVO.getIopsWriteRate();
        }
        return null;
    }

    public void update() {
        volumeDao.update(volumeVO.getId(), volumeVO);
        volumeVO = volumeDao.findById(volumeVO.getId());
    }

    @Override
    public long getId() {
        return this.volumeVO.getId();
    }

    @Override
    public boolean isAttachedVM() {
        return (this.volumeVO.getInstanceId() == null) ? false : true;
    }

    @Override
    public String getUri() {
        if (this.dataStore == null) {
            throw new CloudRuntimeException("datastore must be set before using this object");
        }
        DataObjectInStore obj = objectInStoreMgr.findObject(this.volumeVO.getId(), DataObjectType.VOLUME,
                this.dataStore.getId(), this.dataStore.getRole());
        if (obj.getState() != ObjectInDataStoreStateMachine.State.Ready) {
            return this.dataStore.getUri() + "&" + EncodingType.OBJTYPE + "=" + DataObjectType.VOLUME + "&"
                    + EncodingType.SIZE + "=" + this.volumeVO.getSize() + "&" + EncodingType.NAME + "="
                    + this.volumeVO.getName();
        } else {
            return this.dataStore.getUri() + "&" + EncodingType.OBJTYPE + "=" + DataObjectType.VOLUME + "&"
                    + EncodingType.PATH + "=" + obj.getInstallPath();
        }
    }

    @Override
    public DataObjectType getType() {
        return DataObjectType.VOLUME;
    }

    @Override
    public void processEvent(ObjectInDataStoreStateMachine.Event event) {
        if (this.dataStore == null) {
            return;
        }
        try {
            Volume.Event volEvent = null;
            if (this.dataStore.getRole() == DataStoreRole.ImageCache) {
                objectInStoreMgr.update(this, event);
                return;
            }
            if (this.dataStore.getRole() == DataStoreRole.Image) {
                objectInStoreMgr.update(this, event);
                if (this.volumeVO.getState() == Volume.State.Migrating || this.volumeVO.getState() == Volume.State.Copying || this.volumeVO.getState() == Volume.State.Uploaded
                        || this.volumeVO.getState() == Volume.State.Expunged) {
                    return;
                }
                if (event == ObjectInDataStoreStateMachine.Event.CreateOnlyRequested) {
                    volEvent = Volume.Event.UploadRequested;
                } else if (event == ObjectInDataStoreStateMachine.Event.MigrationRequested) {
                    volEvent = Volume.Event.CopyRequested;
                }
            } else {
                if (event == ObjectInDataStoreStateMachine.Event.CreateRequested
                        || event == ObjectInDataStoreStateMachine.Event.CreateOnlyRequested) {
                    volEvent = Volume.Event.CreateRequested;
                } else if (event == ObjectInDataStoreStateMachine.Event.CopyingRequested) {
                    volEvent = Volume.Event.CopyRequested;
                } else if (event == ObjectInDataStoreStateMachine.Event.MigrationRequested) {
                    volEvent = Volume.Event.MigrationRequested;
                }
            }

            if (event == ObjectInDataStoreStateMachine.Event.DestroyRequested) {
                volEvent = Volume.Event.DestroyRequested;
            } else if (event == ObjectInDataStoreStateMachine.Event.ExpungeRequested) {
                volEvent = Volume.Event.ExpungingRequested;
            } else if (event == ObjectInDataStoreStateMachine.Event.OperationSuccessed) {
                volEvent = Volume.Event.OperationSucceeded;
            } else if (event == ObjectInDataStoreStateMachine.Event.OperationFailed) {
                volEvent = Volume.Event.OperationFailed;
            } else if (event == ObjectInDataStoreStateMachine.Event.ResizeRequested) {
                volEvent = Volume.Event.ResizeRequested;
            }
            this.stateTransit(volEvent);
        } catch (Exception e) {
            s_logger.debug("Failed to update state", e);
            throw new CloudRuntimeException("Failed to update state:" + e.toString());
        } finally {
            // in case of OperationFailed, expunge the entry
            if (event == ObjectInDataStoreStateMachine.Event.OperationFailed
                    && (this.volumeVO.getState() != Volume.State.Copying && this.volumeVO.getState() != Volume.State.Uploaded)) {
                objectInStoreMgr.deleteIfNotReady(this);
            }
        }

    }

    @Override
    public void processEventOnly(ObjectInDataStoreStateMachine.Event event) {
        try {
            objectInStoreMgr.update(this, event);
        } catch (Exception e) {
            s_logger.debug("Failed to update state", e);
            throw new CloudRuntimeException("Failed to update state:" + e.toString());
        } finally {
            // in case of OperationFailed, expunge the entry
            if (event == ObjectInDataStoreStateMachine.Event.OperationFailed) {
                objectInStoreMgr.deleteIfNotReady(this);
            }
        }
    }

    @Override
    public String getName() {
        return this.volumeVO.getName();
    }

    @Override
    public Long getInstanceId() {
        return this.volumeVO.getInstanceId();
    }

    @Override
    public String getFolder() {
        return this.volumeVO.getFolder();
    }

    @Override
    public String getPath() {
        if (this.dataStore.getRole() == DataStoreRole.Primary) {
            return this.volumeVO.getPath();
        } else {
            DataObjectInStore objInStore = this.objectInStoreMgr.findObject(this, dataStore);
            if (objInStore != null) {
                return objInStore.getInstallPath();
            } else {
                return null;
            }
        }
    }

    @Override
    public Long getPodId() {
        return this.volumeVO.getPodId();
    }

    @Override
    public long getDataCenterId() {
        return this.volumeVO.getDataCenterId();
    }

    @Override
    public Type getVolumeType() {
        return this.volumeVO.getVolumeType();
    }

    @Override
    public Long getPoolId() {
        return this.volumeVO.getPoolId();
    }

    @Override
    public Date getAttached() {
        return this.volumeVO.getAttached();
    }

    @Override
    public Long getDeviceId() {
        return this.volumeVO.getDeviceId();
    }

    @Override
    public Date getCreated() {
        return this.volumeVO.getCreated();
    }

    @Override
    public Long getDiskOfferingId() {
        return this.volumeVO.getDiskOfferingId();
    }

    @Override
    public String getChainInfo() {
        return this.volumeVO.getChainInfo();
    }

    @Override
    public boolean isRecreatable() {
        return this.volumeVO.isRecreatable();
    }

    @Override
    public long getUpdatedCount() {
        return this.volumeVO.getUpdatedCount();
    }

    @Override
    public void incrUpdatedCount() {
        this.volumeVO.incrUpdatedCount();
    }

    @Override
    public Date getUpdated() {
        return this.volumeVO.getUpdated();
    }

    @Override
    public String getReservationId() {
        return this.volumeVO.getReservationId();
    }

    @Override
    public void setReservationId(String reserv) {
        this.volumeVO.setReservationId(reserv);
    }

    @Override
    public long getAccountId() {
        return this.volumeVO.getAccountId();
    }

    @Override
    public long getDomainId() {
        return this.volumeVO.getDomainId();
    }

    @Override
    public Long getTemplateId() {
        return this.volumeVO.getTemplateId();
    }

    @Override
    public void addPayload(Object data) {
        this.payload = data;
    }

    @Override
    public Object getpayload() {
        return this.payload;
    }

    public VolumeVO getVolume() {
        return this.volumeVO;
    }

    @Override
    public HypervisorType getHypervisorType() {
        return this.volumeDao.getHypervisorType(this.volumeVO.getId());
    }

    @Override
    public Long getLastPoolId() {
        return this.volumeVO.getLastPoolId();
    }

    @Override
    public DataTO getTO() {
        DataTO to = this.getDataStore().getDriver().getTO(this);
        if (to == null) {
            to = new VolumeObjectTO(this);
        }
        return to;
    }

    @Override
    public void processEvent(ObjectInDataStoreStateMachine.Event event, Answer answer) {
        try {
            if (this.dataStore.getRole() == DataStoreRole.Primary) {
                if (answer instanceof CopyCmdAnswer) {
                    CopyCmdAnswer cpyAnswer = (CopyCmdAnswer) answer;
                    VolumeVO vol = this.volumeDao.findById(this.getId());
                    VolumeObjectTO newVol = (VolumeObjectTO) cpyAnswer.getNewData();
                    vol.setPath(newVol.getPath());
                    if (newVol.getSize() != null) {
                        vol.setSize(newVol.getSize());
                    }
                    if (newVol.getFormat() != null) {
                        vol.setFormat(newVol.getFormat());
                    }
                    vol.setPoolId(this.getDataStore().getId());
                    volumeDao.update(vol.getId(), vol);
                } else if (answer instanceof CreateObjectAnswer) {
                    CreateObjectAnswer createAnswer = (CreateObjectAnswer) answer;
                    VolumeObjectTO newVol = (VolumeObjectTO) createAnswer.getData();
                    VolumeVO vol = this.volumeDao.findById(this.getId());
                    vol.setPath(newVol.getPath());
                    if (newVol.getSize() != null) {
                        vol.setSize(newVol.getSize());
                    }
                    vol.setPoolId(this.getDataStore().getId());
                    if (newVol.getFormat() != null) {
                        vol.setFormat(newVol.getFormat());
                    }
                    volumeDao.update(vol.getId(), vol);
                }
            } else {
                // image store or imageCache store
                if (answer instanceof DownloadAnswer) {
                    DownloadAnswer dwdAnswer = (DownloadAnswer) answer;
                    VolumeDataStoreVO volStore = this.volumeStoreDao.findByStoreVolume(this.dataStore.getId(),
                            this.getId());
                    volStore.setInstallPath(dwdAnswer.getInstallPath());
                    volStore.setChecksum(dwdAnswer.getCheckSum());
                    this.volumeStoreDao.update(volStore.getId(), volStore);
                } else if (answer instanceof CopyCmdAnswer) {
                    CopyCmdAnswer cpyAnswer = (CopyCmdAnswer) answer;
                    VolumeDataStoreVO volStore = this.volumeStoreDao.findByStoreVolume(this.dataStore.getId(),
                            this.getId());
                    VolumeObjectTO newVol = (VolumeObjectTO) cpyAnswer.getNewData();
                    volStore.setInstallPath(newVol.getPath());
                    if (newVol.getSize() != null) {
                        volStore.setSize(newVol.getSize());
                    }
                    this.volumeStoreDao.update(volStore.getId(), volStore);
                }
            }
        } catch (RuntimeException ex) {
            if (event == ObjectInDataStoreStateMachine.Event.OperationFailed) {
                objectInStoreMgr.deleteIfNotReady(this);
            }
            throw ex;
        }
        this.processEvent(event);

    }

    @Override
    public void incRefCount() {
        if (this.dataStore == null) {
            return;
        }

        if (this.dataStore.getRole() == DataStoreRole.Image || this.dataStore.getRole() == DataStoreRole.ImageCache) {
            VolumeDataStoreVO store = volumeStoreDao.findByStoreVolume(this.dataStore.getId(), this.getId());
            store.incrRefCnt();
            store.setLastUpdated(new Date());
            volumeStoreDao.update(store.getId(), store);
        }
    }

    @Override
    public void decRefCount() {
        if (this.dataStore == null) {
            return;
        }
        if (this.dataStore.getRole() == DataStoreRole.Image || this.dataStore.getRole() == DataStoreRole.ImageCache) {
            VolumeDataStoreVO store = volumeStoreDao.findByStoreVolume(this.dataStore.getId(), this.getId());
            store.decrRefCnt();
            store.setLastUpdated(new Date());
            volumeStoreDao.update(store.getId(), store);
        }
    }

    @Override
    public Long getRefCount() {
        if (this.dataStore == null) {
            return null;
        }
        if (this.dataStore.getRole() == DataStoreRole.Image || this.dataStore.getRole() == DataStoreRole.ImageCache) {
            VolumeDataStoreVO store = volumeStoreDao.findByStoreVolume(this.dataStore.getId(), this.getId());
            return store.getRefCnt();
        }
        return null;
    }

    @Override
    public void processEventOnly(ObjectInDataStoreStateMachine.Event event, Answer answer) {
        try {
            if (this.dataStore.getRole() == DataStoreRole.Primary) {
                if (answer instanceof CopyCmdAnswer) {
                    CopyCmdAnswer cpyAnswer = (CopyCmdAnswer) answer;
                    VolumeVO vol = this.volumeDao.findById(this.getId());
                    VolumeObjectTO newVol = (VolumeObjectTO) cpyAnswer.getNewData();
                    vol.setPath(newVol.getPath());
                    if (newVol.getSize() != null) {
                        vol.setSize(newVol.getSize());
                    }
                    vol.setPoolId(this.getDataStore().getId());
                    volumeDao.update(vol.getId(), vol);
                } else if (answer instanceof CreateObjectAnswer) {
                    CreateObjectAnswer createAnswer = (CreateObjectAnswer) answer;
                    VolumeObjectTO newVol = (VolumeObjectTO) createAnswer.getData();
                    VolumeVO vol = this.volumeDao.findById(this.getId());
                    vol.setPath(newVol.getPath());
                    if (newVol.getSize() != null) {
                        vol.setSize(newVol.getSize());
                    }
                    vol.setPoolId(this.getDataStore().getId());
                    volumeDao.update(vol.getId(), vol);
                }
            } else {
                // image store or imageCache store
                if (answer instanceof DownloadAnswer) {
                    DownloadAnswer dwdAnswer = (DownloadAnswer) answer;
                    VolumeDataStoreVO volStore = this.volumeStoreDao.findByStoreVolume(this.dataStore.getId(),
                            this.getId());
                    volStore.setInstallPath(dwdAnswer.getInstallPath());
                    volStore.setChecksum(dwdAnswer.getCheckSum());
                    this.volumeStoreDao.update(volStore.getId(), volStore);
                } else if (answer instanceof CopyCmdAnswer) {
                    CopyCmdAnswer cpyAnswer = (CopyCmdAnswer) answer;
                    VolumeDataStoreVO volStore = this.volumeStoreDao.findByStoreVolume(this.dataStore.getId(),
                            this.getId());
                    VolumeObjectTO newVol = (VolumeObjectTO) cpyAnswer.getNewData();
                    volStore.setInstallPath(newVol.getPath());
                    if (newVol.getSize() != null) {
                        volStore.setSize(newVol.getSize());
                    }
                    this.volumeStoreDao.update(volStore.getId(), volStore);
                }
            }
        } catch (RuntimeException ex) {
            if (event == ObjectInDataStoreStateMachine.Event.OperationFailed) {
                objectInStoreMgr.deleteIfNotReady(this);
            }
            throw ex;
        }
        this.processEventOnly(event);

    }

    @Override
    public ImageFormat getFormat() {
        return this.volumeVO.getFormat();
    }

    @Override
    public boolean delete() {
        if (dataStore != null) {
            return dataStore.delete(this);
        }
        return true;
    }

    @Override
    public Long getVmSnapshotChainSize() {
        return this.volumeVO.getVmSnapshotChainSize();
    }
}
