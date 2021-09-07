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

import com.cloud.dc.VsphereStoragePolicyVO;
import com.cloud.dc.dao.VsphereStoragePolicyDao;
import com.cloud.service.dao.ServiceOfferingDetailsDao;
import com.cloud.storage.MigrationOptions;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeDetailVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.vm.VmDetailConstants;

import org.apache.cloudstack.resourcedetail.dao.DiskOfferingDetailsDao;
import org.apache.cloudstack.api.ApiConstants;
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
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.DownloadAnswer;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataTO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.offering.DiskOffering.DiskCacheMode;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.ProvisioningType;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.utils.storage.encoding.EncodingType;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
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
    @Inject
    VMTemplateDao templateDao;
    @Inject
    VolumeDetailsDao volumeDetailsDao;
    @Inject
    ServiceOfferingDetailsDao serviceOfferingDetailsDao;
    @Inject
    DiskOfferingDetailsDao diskOfferingDetailsDao;
    @Inject
    VsphereStoragePolicyDao vsphereStoragePolicyDao;

    private Object payload;
    private MigrationOptions migrationOptions;
    private boolean directDownload;
    private String vSphereStoragePolicyId;

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
        Long vmId = volumeVO.getInstanceId();
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
    public VirtualMachine getAttachedVM() {
        Long vmId = volumeVO.getInstanceId();
        if (vmId != null) {
            VMInstanceVO vm = vmInstanceDao.findById(vmId);
            return vm;
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

    @Override
    public Integer getHypervisorSnapshotReserve() {
        return volumeVO.getHypervisorSnapshotReserve();
    }

    @Override
    public boolean isDisplayVolume() {
        return volumeVO.isDisplayVolume();
    }

    @Override
    public boolean isDisplay() {
        return volumeVO.isDisplay();
    }

    public long getVolumeId() {
        return volumeVO.getId();
    }

    @Override
    public boolean stateTransit(Volume.Event event) {
        boolean result = false;
        try {
            volumeVO = volumeDao.findById(volumeVO.getId());
            if (volumeVO != null) {
                result = _volStateMachine.transitTo(volumeVO, event, null, volumeDao);
                volumeVO = volumeDao.findById(volumeVO.getId());
            }
        } catch (NoTransitionException e) {
            String errorMessage = "Failed to transit volume: " + getVolumeId() + ", due to: " + e.toString();
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
    public Long getBytesReadRateMax() {
        DiskOfferingVO diskOfferingVO = getDiskOfferingVO();
        if (diskOfferingVO != null) {
            return diskOfferingVO.getBytesReadRateMax();
        }
        return null;
    }

    @Override
    public Long getBytesReadRateMaxLength() {
        DiskOfferingVO diskOfferingVO = getDiskOfferingVO();
        if (diskOfferingVO != null) {
            return diskOfferingVO.getBytesReadRateMaxLength();
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
    public Long getBytesWriteRateMax() {
        DiskOfferingVO diskOfferingVO = getDiskOfferingVO();
        if (diskOfferingVO != null) {
            return diskOfferingVO.getBytesWriteRateMax();
        }
        return null;
    }

    @Override
    public Long getBytesWriteRateMaxLength() {
        DiskOfferingVO diskOfferingVO = getDiskOfferingVO();
        if (diskOfferingVO != null) {
            return diskOfferingVO.getBytesWriteRateMaxLength();
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
    public Long getIopsReadRateMax() {
        DiskOfferingVO diskOfferingVO = getDiskOfferingVO();
        if (diskOfferingVO != null) {
            return diskOfferingVO.getIopsReadRateMax();
        }
        return null;
    }

    @Override
    public Long getIopsReadRateMaxLength() {
        DiskOfferingVO diskOfferingVO = getDiskOfferingVO();
        if (diskOfferingVO != null) {
            return diskOfferingVO.getIopsReadRateMaxLength();
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

    @Override
    public Long getIopsWriteRateMax() {
        DiskOfferingVO diskOfferingVO = getDiskOfferingVO();
        if (diskOfferingVO != null) {
            return diskOfferingVO.getIopsWriteRateMax();
        }
        return null;
    }

    @Override
    public Long getIopsWriteRateMaxLength() {
        DiskOfferingVO diskOfferingVO = getDiskOfferingVO();
        if (diskOfferingVO != null) {
            return diskOfferingVO.getIopsWriteRateMaxLength();
        }
        return null;
    }

    @Override
    public DiskCacheMode getCacheMode() {
        DiskOfferingVO diskOfferingVO = getDiskOfferingVO();
        if (diskOfferingVO != null) {
            return diskOfferingVO.getCacheMode();
        }
        return null;
    }

    @Override
    public MigrationOptions getMigrationOptions() {
        return migrationOptions;
    }

    @Override
    public void setMigrationOptions(MigrationOptions migrationOptions) {
        this.migrationOptions = migrationOptions;
    }

    @Override
    public boolean isDirectDownload() {
        return directDownload;
    }

    @Override
    public void setDirectDownload(boolean directDownload) {
        this.directDownload = directDownload;
    }

    public void update() {
        volumeDao.update(volumeVO.getId(), volumeVO);
        volumeVO = volumeDao.findById(volumeVO.getId());
    }

    @Override
    public long getId() {
        return volumeVO.getId();
    }

    @Override
    public boolean isAttachedVM() {
        return (volumeVO.getInstanceId() == null) ? false : true;
    }

    @Override
    public String getUri() {
        if (dataStore == null) {
            throw new CloudRuntimeException("datastore must be set before using this object");
        }
        DataObjectInStore obj = objectInStoreMgr.findObject(volumeVO.getId(), DataObjectType.VOLUME, dataStore.getId(), dataStore.getRole(), null);
        if (obj.getState() != ObjectInDataStoreStateMachine.State.Ready) {
            return dataStore.getUri() + "&" + EncodingType.OBJTYPE + "=" + DataObjectType.VOLUME + "&" + EncodingType.SIZE + "=" + volumeVO.getSize() + "&" +
                EncodingType.NAME + "=" + volumeVO.getName();
        } else {
            return dataStore.getUri() + "&" + EncodingType.OBJTYPE + "=" + DataObjectType.VOLUME + "&" + EncodingType.PATH + "=" + obj.getInstallPath();
        }
    }

    @Override
    public DataObjectType getType() {
        return DataObjectType.VOLUME;
    }

    @Override
    public void processEvent(ObjectInDataStoreStateMachine.Event event) {
        if (dataStore == null) {
            return;
        }
        try {
            Volume.Event volEvent = null;
            if (dataStore.getRole() == DataStoreRole.ImageCache) {
                objectInStoreMgr.update(this, event);
                return;
            }
            if (dataStore.getRole() == DataStoreRole.Image) {
                objectInStoreMgr.update(this, event);
                if (volumeVO.getState() == Volume.State.Migrating || volumeVO.getState() == Volume.State.Copying ||
                    volumeVO.getState() == Volume.State.Uploaded || volumeVO.getState() == Volume.State.Expunged) {
                    return;
                }
                if (event == ObjectInDataStoreStateMachine.Event.CreateOnlyRequested) {
                    volEvent = Volume.Event.UploadRequested;
                } else if (event == ObjectInDataStoreStateMachine.Event.MigrationRequested) {
                    volEvent = Volume.Event.CopyRequested;
                } else if (event == ObjectInDataStoreStateMachine.Event.MigrateDataRequested) {
                    return;
                }
            } else {
                if (event == ObjectInDataStoreStateMachine.Event.CreateRequested || event == ObjectInDataStoreStateMachine.Event.CreateOnlyRequested) {
                    volEvent = Volume.Event.CreateRequested;
                } else if (event == ObjectInDataStoreStateMachine.Event.CopyingRequested) {
                    volEvent = Volume.Event.CopyRequested;
                } else if (event == ObjectInDataStoreStateMachine.Event.MigrationRequested) {
                    volEvent = Volume.Event.MigrationRequested;
                } else if (event == ObjectInDataStoreStateMachine.Event.MigrationCopyRequested) {
                    volEvent = Event.MigrationCopyRequested;
                }
            }

            if (event == ObjectInDataStoreStateMachine.Event.DestroyRequested) {
                volEvent = Volume.Event.DestroyRequested;
            } else if (event == ObjectInDataStoreStateMachine.Event.ExpungeRequested) {
                volEvent = Volume.Event.ExpungingRequested;
            } else if (event == ObjectInDataStoreStateMachine.Event.OperationSuccessed) {
                volEvent = Volume.Event.OperationSucceeded;
            } else if (event == ObjectInDataStoreStateMachine.Event.MigrationCopySucceeded) {
              volEvent = Event.MigrationCopySucceeded;
            } else if (event == ObjectInDataStoreStateMachine.Event.OperationFailed) {
                volEvent = Volume.Event.OperationFailed;
            } else if (event == ObjectInDataStoreStateMachine.Event.MigrationCopyFailed) {
              volEvent = Event.MigrationCopyFailed;
            } else if (event == ObjectInDataStoreStateMachine.Event.ResizeRequested) {
                volEvent = Volume.Event.ResizeRequested;
            }
            stateTransit(volEvent);
        } catch (Exception e) {
            s_logger.debug("Failed to update state", e);
            throw new CloudRuntimeException("Failed to update state:" + e.toString());
        } finally {
            // in case of OperationFailed, expunge the entry
            // state transit call reloads the volume from DB and so check for null as well
            if (event == ObjectInDataStoreStateMachine.Event.OperationFailed &&
                (volumeVO != null && volumeVO.getState() != Volume.State.Copying && volumeVO.getState() != Volume.State.Uploaded && volumeVO.getState() != Volume.State.UploadError)) {
                objectInStoreMgr.deleteIfNotReady(this);
            }
        }
    }

    @Override
    public boolean isDeployAsIs() {
        VMTemplateVO template = templateDao.findById(getTemplateId());
        return template != null && template.isDeployAsIs();
    }

    @Override
    public String getDeployAsIsConfiguration() {
        VolumeDetailVO detail = volumeDetailsDao.findDetail(getId(), VmDetailConstants.DEPLOY_AS_IS_CONFIGURATION);
        return detail != null ? detail.getValue() : null;
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
        return volumeVO.getName();
    }

    @Override
    public Long getInstanceId() {
        return volumeVO.getInstanceId();
    }

    @Override
    public String getFolder() {
        return volumeVO.getFolder();
    }

    @Override
    public String getPath() {
        if (dataStore.getRole() == DataStoreRole.Primary) {
            return volumeVO.getPath();
        } else {
            DataObjectInStore objInStore = objectInStoreMgr.findObject(this, dataStore);
            if (objInStore != null) {
                return objInStore.getInstallPath();
            } else {
                return null;
            }
        }
    }

    @Override
    public Long getPodId() {
        return volumeVO.getPodId();
    }

    @Override
    public long getDataCenterId() {
        return volumeVO.getDataCenterId();
    }

    @Override
    public Type getVolumeType() {
        return volumeVO.getVolumeType();
    }

    @Override
    public Long getPoolId() {
        return volumeVO.getPoolId();
    }

    @Override
    public Date getAttached() {
        return volumeVO.getAttached();
    }

    @Override
    public Long getDeviceId() {
        return volumeVO.getDeviceId();
    }

    @Override
    public Date getCreated() {
        return volumeVO.getCreated();
    }

    @Override
    public Long getDiskOfferingId() {
        return volumeVO.getDiskOfferingId();
    }

    @Override
    public String getChainInfo() {
        return volumeVO.getChainInfo();
    }

    @Override
    public boolean isRecreatable() {
        return volumeVO.isRecreatable();
    }

    @Override
    public long getUpdatedCount() {
        return volumeVO.getUpdatedCount();
    }

    @Override
    public void incrUpdatedCount() {
        volumeVO.incrUpdatedCount();
    }

    @Override
    public Date getUpdated() {
        return volumeVO.getUpdated();
    }

    @Override
    public String getReservationId() {
        return volumeVO.getReservationId();
    }

    @Override
    public void setReservationId(String reserv) {
        volumeVO.setReservationId(reserv);
    }

    @Override
    public long getAccountId() {
        return volumeVO.getAccountId();
    }

    @Override
    public long getDomainId() {
        return volumeVO.getDomainId();
    }

    @Override
    public Long getTemplateId() {
        return volumeVO.getTemplateId();
    }

    @Override
    public void addPayload(Object data) {
        payload = data;
    }

    @Override
    public Object getpayload() {
        return payload;
    }

    public VolumeVO getVolume() {
        return volumeVO;
    }

    @Override
    public HypervisorType getHypervisorType() {
        return volumeDao.getHypervisorType(volumeVO.getId());
    }

    @Override
    public Storage.StoragePoolType getStoragePoolType() {
        return volumeVO.getPoolType();
    }

    @Override
    public Long getLastPoolId() {
        return volumeVO.getLastPoolId();
    }

    @Override
    public DataTO getTO() {
        DataTO to = getDataStore().getDriver().getTO(this);
        if (to == null) {
            to = new VolumeObjectTO(this);
        }
        return to;
    }

    @Override
    public void processEvent(ObjectInDataStoreStateMachine.Event event, Answer answer) {
        try {
            if (dataStore.getRole() == DataStoreRole.Primary) {
                if (answer instanceof CopyCmdAnswer) {
                    CopyCmdAnswer cpyAnswer = (CopyCmdAnswer)answer;
                    VolumeVO vol = volumeDao.findById(getId());
                    VolumeObjectTO newVol = (VolumeObjectTO)cpyAnswer.getNewData();
                    vol.setPath(newVol.getPath());
                    if (newVol.getSize() != null) {
                        // Root disk resize may be requested where the original
                        // template size is less than the requested root disk size
                        if (vol.getSize() == null || vol.getSize() < newVol.getSize()) {
                            vol.setSize(newVol.getSize());
                        }
                    }
                    if (newVol.getFormat() != null) {
                        vol.setFormat(newVol.getFormat());
                    }
                    vol.setPoolId(getDataStore().getId());
                    volumeDao.update(vol.getId(), vol);
                } else if (answer instanceof CreateObjectAnswer) {
                    CreateObjectAnswer createAnswer = (CreateObjectAnswer)answer;
                    VolumeObjectTO newVol = (VolumeObjectTO)createAnswer.getData();
                    VolumeVO vol = volumeDao.findById(getId());
                    vol.setPath(newVol.getPath());
                    if (newVol.getSize() != null) {
                        vol.setSize(newVol.getSize());
                    }
                    vol.setPoolId(getDataStore().getId());
                    if (newVol.getFormat() != null) {
                        vol.setFormat(newVol.getFormat());
                    }
                    volumeDao.update(vol.getId(), vol);
                }
            } else {
                // image store or imageCache store
                if (answer instanceof DownloadAnswer) {
                    DownloadAnswer dwdAnswer = (DownloadAnswer)answer;
                    VolumeDataStoreVO volStore = volumeStoreDao.findByStoreVolume(dataStore.getId(), getId());
                    volStore.setInstallPath(dwdAnswer.getInstallPath());
                    volStore.setChecksum(dwdAnswer.getCheckSum());
                    volumeStoreDao.update(volStore.getId(), volStore);
                } else if (answer instanceof CopyCmdAnswer) {
                    CopyCmdAnswer cpyAnswer = (CopyCmdAnswer)answer;
                    VolumeDataStoreVO volStore = volumeStoreDao.findByStoreVolume(dataStore.getId(), getId());
                    VolumeObjectTO newVol = (VolumeObjectTO)cpyAnswer.getNewData();
                    volStore.setInstallPath(newVol.getPath());
                    if (newVol.getSize() != null) {
                        volStore.setSize(newVol.getSize());
                    }
                    volumeStoreDao.update(volStore.getId(), volStore);
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
        if (dataStore == null) {
            return;
        }

        if (dataStore.getRole() == DataStoreRole.Image || dataStore.getRole() == DataStoreRole.ImageCache) {
            VolumeDataStoreVO store = volumeStoreDao.findByStoreVolume(dataStore.getId(), getId());
            store.incrRefCnt();
            store.setLastUpdated(new Date());
            volumeStoreDao.update(store.getId(), store);
        }
    }

    @Override
    public void decRefCount() {
        if (dataStore == null) {
            return;
        }
        if (dataStore.getRole() == DataStoreRole.Image || dataStore.getRole() == DataStoreRole.ImageCache) {
            VolumeDataStoreVO store = volumeStoreDao.findByStoreVolume(dataStore.getId(), getId());
            store.decrRefCnt();
            store.setLastUpdated(new Date());
            volumeStoreDao.update(store.getId(), store);
        }
    }

    @Override
    public Long getRefCount() {
        if (dataStore == null) {
            return null;
        }
        if (dataStore.getRole() == DataStoreRole.Image || dataStore.getRole() == DataStoreRole.ImageCache) {
            VolumeDataStoreVO store = volumeStoreDao.findByStoreVolume(dataStore.getId(), getId());
            return store.getRefCnt();
        }
        return null;
    }

    @Override
    public void processEventOnly(ObjectInDataStoreStateMachine.Event event, Answer answer) {
        try {
            if (dataStore.getRole() == DataStoreRole.Primary) {
                if (answer instanceof CopyCmdAnswer) {
                    CopyCmdAnswer cpyAnswer = (CopyCmdAnswer)answer;
                    VolumeVO vol = volumeDao.findById(getId());
                    VolumeObjectTO newVol = (VolumeObjectTO)cpyAnswer.getNewData();
                    vol.setPath(newVol.getPath());
                    if (newVol.getSize() != null) {
                        vol.setSize(newVol.getSize());
                    }
                    vol.setPoolId(getDataStore().getId());
                    volumeDao.update(vol.getId(), vol);
                } else if (answer instanceof CreateObjectAnswer) {
                    CreateObjectAnswer createAnswer = (CreateObjectAnswer)answer;
                    VolumeObjectTO newVol = (VolumeObjectTO)createAnswer.getData();
                    VolumeVO vol = volumeDao.findById(getId());
                    vol.setPath(newVol.getPath());
                    if (newVol.getSize() != null) {
                        vol.setSize(newVol.getSize());
                    }
                    vol.setPoolId(getDataStore().getId());
                    volumeDao.update(vol.getId(), vol);
                }
            } else {
                // image store or imageCache store
                if (answer instanceof DownloadAnswer) {
                    DownloadAnswer dwdAnswer = (DownloadAnswer)answer;
                    VolumeDataStoreVO volStore = volumeStoreDao.findByStoreVolume(dataStore.getId(), getId());
                    volStore.setInstallPath(dwdAnswer.getInstallPath());
                    volStore.setChecksum(dwdAnswer.getCheckSum());
                    volumeStoreDao.update(volStore.getId(), volStore);
                } else if (answer instanceof CopyCmdAnswer) {
                    CopyCmdAnswer cpyAnswer = (CopyCmdAnswer)answer;
                    VolumeDataStoreVO volStore = volumeStoreDao.findByStoreVolume(dataStore.getId(), getId());
                    VolumeObjectTO newVol = (VolumeObjectTO)cpyAnswer.getNewData();
                    volStore.setInstallPath(newVol.getPath());
                    if (newVol.getSize() != null) {
                        volStore.setSize(newVol.getSize());
                    }
                    volumeStoreDao.update(volStore.getId(), volStore);
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

    public String getvSphereStoragePolicyId() {
        if (StringUtils.isEmpty(vSphereStoragePolicyId)) {
            String storagePolicyVOid = null;
            if (Volume.Type.ROOT == getVolumeType()) {
                Long vmId = volumeVO.getInstanceId();
                if (vmId != null) {
                    VMInstanceVO vm = vmInstanceDao.findByIdIncludingRemoved(vmId);
                    storagePolicyVOid = serviceOfferingDetailsDao.getDetail(vm.getServiceOfferingId(),
                            ApiConstants.STORAGE_POLICY);
                }
            } else {
                storagePolicyVOid = diskOfferingDetailsDao.getDetail(volumeVO.getDiskOfferingId(),
                        ApiConstants.STORAGE_POLICY);
            }
            if (storagePolicyVOid != null) {
                VsphereStoragePolicyVO vsphereStoragePolicyVO = vsphereStoragePolicyDao.findById(Long.parseLong(storagePolicyVOid));
                vSphereStoragePolicyId = vsphereStoragePolicyVO.getPolicyId();
            }
        }
        return vSphereStoragePolicyId;
    }

    @Override
    public ImageFormat getFormat() {
        return volumeVO.getFormat();
    }

    @Override
    public ProvisioningType getProvisioningType(){
        return this.volumeVO.getProvisioningType();
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
        return volumeVO.getVmSnapshotChainSize();
    }

    @Override
    public Class<?> getEntityType() {
        return Volume.class;
    }
}
