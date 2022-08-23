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

import com.cloud.configuration.Resource.ResourceType;
import com.cloud.dc.VsphereStoragePolicyVO;
import com.cloud.dc.dao.VsphereStoragePolicyDao;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import org.apache.cloudstack.secret.dao.PassphraseDao;
import org.apache.cloudstack.secret.PassphraseVO;
import com.cloud.service.dao.ServiceOfferingDetailsDao;
import com.cloud.storage.MigrationOptions;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeDetailVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.user.ResourceLimitService;
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
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.DownloadAnswer;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataTO;
import com.cloud.exception.ConcurrentOperationException;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;

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
    ResourceLimitService resourceLimitMgr;
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
    @Inject
    PassphraseDao passphraseDao;

    private Object payload;
    private MigrationOptions migrationOptions;
    private boolean directDownload;
    private String vSphereStoragePolicyId;

    private final List<Volume.State> volumeStatesThatShouldNotTransitWhenDataStoreRoleIsImage = Arrays.asList(Volume.State.Migrating, Volume.State.Uploaded, Volume.State.Copying,
      Volume.State.Expunged);

    private final List<Volume.State> volumeStatesThatShouldNotDeleteEntry = Arrays.asList(Volume.State.UploadError, Volume.State.Uploaded, Volume.State.Copying);

    private final List<DataStoreRole> imageAndImageCacheRoles = Arrays.asList(DataStoreRole.Image, DataStoreRole.ImageCache);

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
        VMInstanceVO vm = null;

        if (vmId != null) {
            vm = vmInstanceDao.findById(vmId);
        }

        return vm == null ? null : vm.getInstanceName();
    }

    @Override
    public VirtualMachine getAttachedVM() {
        Long vmId = volumeVO.getInstanceId();
        return vmId == null ? null : vmInstanceDao.findById(vmId);
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
            String errorMessage = String.format("Failed to transit volume %s to [%s] due to [%s].", volumeVO.getVolumeDescription(), event, e.getMessage());
            s_logger.warn(errorMessage, e);
            throw new CloudRuntimeException(errorMessage, e);
        }
        return result;
    }

    protected DiskOfferingVO getDiskOfferingVO() {
        Long diskOfferingId = getDiskOfferingId();
        return diskOfferingId == null ? null : diskOfferingDao.findById(diskOfferingId);
    }

    @Override
    public long getPhysicalSize() {
        VolumeDataStoreVO volumeDataStoreVO = volumeStoreDao.findByVolume(volumeVO.getId());
        if (volumeDataStoreVO != null) {
            return volumeDataStoreVO.getPhysicalSize();
        }
        return volumeVO.getSize();
    }

    @Override
    public Long getBytesReadRate() {
        return getLongValueFromDiskOfferingVoMethod(DiskOfferingVO::getBytesReadRate);
    }

    @Override
    public Long getBytesReadRateMax() {
        return getLongValueFromDiskOfferingVoMethod(DiskOfferingVO::getBytesReadRateMax);
    }

    @Override
    public Long getBytesReadRateMaxLength() {
        return getLongValueFromDiskOfferingVoMethod(DiskOfferingVO::getBytesReadRateMaxLength);
    }

    @Override
    public Long getBytesWriteRate() {
        return getLongValueFromDiskOfferingVoMethod(DiskOfferingVO::getBytesWriteRate);
    }

    @Override
    public Long getBytesWriteRateMax() {
        return getLongValueFromDiskOfferingVoMethod(DiskOfferingVO::getBytesWriteRateMax);
    }

    @Override
    public Long getBytesWriteRateMaxLength() {
        return getLongValueFromDiskOfferingVoMethod(DiskOfferingVO::getBytesWriteRateMaxLength);
    }

    @Override
    public Long getIopsReadRate() {
        return getLongValueFromDiskOfferingVoMethod(DiskOfferingVO::getIopsReadRate);
    }

    @Override
    public Long getIopsReadRateMax() {
        return getLongValueFromDiskOfferingVoMethod(DiskOfferingVO::getIopsReadRateMax);
    }

    @Override
    public Long getIopsReadRateMaxLength() {
        return getLongValueFromDiskOfferingVoMethod(DiskOfferingVO::getIopsReadRateMaxLength);
    }

    @Override
    public Long getIopsWriteRate() {
        return getLongValueFromDiskOfferingVoMethod(DiskOfferingVO::getIopsWriteRate);
    }

    @Override
    public Long getIopsWriteRateMax() {
        return getLongValueFromDiskOfferingVoMethod(DiskOfferingVO::getIopsWriteRateMax);
    }

    @Override
    public Long getIopsWriteRateMaxLength() {
        return getLongValueFromDiskOfferingVoMethod(DiskOfferingVO::getIopsWriteRateMaxLength);
    }

    protected Long getLongValueFromDiskOfferingVoMethod(Function<DiskOfferingVO, Long> method){
        DiskOfferingVO diskOfferingVO = getDiskOfferingVO();
        if (diskOfferingVO != null) {
            return method.apply(diskOfferingVO);
        }
        return null;
    }

    @Override
    public DiskCacheMode getCacheMode() {
        DiskOfferingVO diskOfferingVO = getDiskOfferingVO();
        return diskOfferingVO == null ? null : diskOfferingVO.getCacheMode();
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
        return volumeVO.getInstanceId() != null;
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

        if (imageAndImageCacheRoles.contains(dataStore.getRole())) {
            updateObjectInDataStoreManager(event, volumeVO != null && !volumeStatesThatShouldNotDeleteEntry.contains(volumeVO.getState()));

            if (dataStore.getRole() == DataStoreRole.ImageCache || volumeStatesThatShouldNotTransitWhenDataStoreRoleIsImage.contains(volumeVO.getState())
              || event == ObjectInDataStoreStateMachine.Event.MigrateDataRequested) {
                return;
            }
        }

        stateTransit(getMapOfEvents().get(event));
    }

    protected Map<ObjectInDataStoreStateMachine.Event, Volume.Event> getMapOfEvents() {
        Map<ObjectInDataStoreStateMachine.Event, Volume.Event> mapOfEvents = new HashMap<>();
        if (dataStore.getRole() == DataStoreRole.Image) {
            mapOfEvents.put(ObjectInDataStoreStateMachine.Event.CreateOnlyRequested, Volume.Event.UploadRequested);
            mapOfEvents.put(ObjectInDataStoreStateMachine.Event.MigrationRequested, Volume.Event.CopyRequested);
        } else {
            mapOfEvents.put(ObjectInDataStoreStateMachine.Event.CreateRequested, Volume.Event.CreateRequested);
            mapOfEvents.put(ObjectInDataStoreStateMachine.Event.CreateOnlyRequested, Volume.Event.CreateRequested);
            mapOfEvents.put(ObjectInDataStoreStateMachine.Event.CopyingRequested, Volume.Event.CopyRequested);
            mapOfEvents.put(ObjectInDataStoreStateMachine.Event.MigrationRequested, Volume.Event.MigrationRequested);
            mapOfEvents.put(ObjectInDataStoreStateMachine.Event.MigrationCopyRequested, Volume.Event.MigrationCopyRequested);
        }
        mapOfEvents.put(ObjectInDataStoreStateMachine.Event.DestroyRequested, Volume.Event.DestroyRequested);
        mapOfEvents.put(ObjectInDataStoreStateMachine.Event.ExpungeRequested, Volume.Event.ExpungingRequested);
        mapOfEvents.put(ObjectInDataStoreStateMachine.Event.OperationSuccessed, Volume.Event.OperationSucceeded);
        mapOfEvents.put(ObjectInDataStoreStateMachine.Event.MigrationCopySucceeded, Volume.Event.MigrationCopySucceeded);
        mapOfEvents.put(ObjectInDataStoreStateMachine.Event.OperationFailed, Volume.Event.OperationFailed);
        mapOfEvents.put(ObjectInDataStoreStateMachine.Event.MigrationCopyFailed, Volume.Event.MigrationCopyFailed);
        mapOfEvents.put(ObjectInDataStoreStateMachine.Event.ResizeRequested, Volume.Event.ResizeRequested);
        return mapOfEvents;
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
        updateObjectInDataStoreManager(event, true);
    }

    protected void updateObjectInDataStoreManager(ObjectInDataStoreStateMachine.Event event, boolean callExpungeEntry){
        try {
            objectInStoreMgr.update(this, event);
        } catch (ConcurrentOperationException | NoTransitionException e) {
            String message = String.format("Failed to update %sto state [%s] due to [%s].", volumeVO == null ? "" : String.format("volume %s ", volumeVO.getVolumeDescription()),
              getMapOfEvents().get(event), e.getMessage());
            s_logger.warn(message, e);
            throw new CloudRuntimeException(message, e);
        } finally {
            expungeEntryOnOperationFailed(event, callExpungeEntry);
        }
    }

    protected void expungeEntryOnOperationFailed(ObjectInDataStoreStateMachine.Event event) {
        expungeEntryOnOperationFailed(event, true);
    }

    protected void expungeEntryOnOperationFailed(ObjectInDataStoreStateMachine.Event event, boolean callExpungeEntry) {
        if (event == ObjectInDataStoreStateMachine.Event.OperationFailed && callExpungeEntry) {
            objectInStoreMgr.deleteIfNotReady(this);
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
        if (answer != null) {
            handleProcessEventAnswer(event, answer);
        }

        this.processEvent(event);
    }

    protected void handleProcessEventAnswer(ObjectInDataStoreStateMachine.Event event, Answer answer) throws RuntimeException {
        try {
            if (answer instanceof CopyCmdAnswer) {
                handleProcessEventAnswer((CopyCmdAnswer)answer);
            } else if (answer instanceof CreateObjectAnswer) {
                handleProcessEventAnswer((CreateObjectAnswer)answer);
            } else if (answer instanceof DownloadAnswer) {
                handleProcessEventAnswer((DownloadAnswer) answer);
            }
        } catch (RuntimeException ex) {
            expungeEntryOnOperationFailed(event);
            throw ex;
        }
    }

    protected boolean isPrimaryDataStore(){
        return dataStore.getRole() == DataStoreRole.Primary;
    }

    protected void setVolumeFormat(VolumeObjectTO newVolume, boolean setFormat, VolumeVO volumeVo) {
        if (newVolume.getFormat() != null && setFormat) {
            volumeVo.setFormat(newVolume.getFormat());
        }
    }

    protected void handleProcessEventAnswer(CopyCmdAnswer copyAnswer) {
        handleProcessEventAnswer(copyAnswer, true, true);
    }

    protected void handleProcessEventAnswer(CopyCmdAnswer copyAnswer, boolean validateVolumeSize, boolean setFormat) {
        VolumeObjectTO newVolume = (VolumeObjectTO)copyAnswer.getNewData();

        if (this.isPrimaryDataStore()) {
            handleProcessEventCopyCmdAnswerPrimaryStore(newVolume, validateVolumeSize, setFormat);
        } else {
            handleProcessEventCopyCmdAnswerNotPrimaryStore(newVolume);
        }
    }

    protected void handleProcessEventCopyCmdAnswerPrimaryStore(VolumeObjectTO newVolume, boolean validateVolumeSize, boolean setFormat) {
        VolumeVO volumeVo = volumeDao.findById(getId());
        updateVolumeInfo(newVolume, volumeVo, (!validateVolumeSize || newVolume.getSize() == null || volumeVo.getSize() == null || volumeVo.getSize() < newVolume.getSize()),
          setFormat);
    }

    protected void updateVolumeInfo(VolumeObjectTO newVolume, VolumeVO volumeVo, boolean setVolumeSize, boolean setFormat) {
        String previousValues = ReflectionToStringBuilderUtils.reflectOnlySelectedFields(volumeVo, "path", "size", "format", "encryptFormat", "poolId");

        volumeVo.setPath(newVolume.getPath());
        Long newVolumeSize = newVolume.getSize();

        volumeVo.setEncryptFormat(newVolume.getEncryptFormat());

        if (newVolumeSize != null && setVolumeSize) {
            volumeVo.setSize(newVolumeSize);
        }

        setVolumeFormat(newVolume, setFormat, volumeVo);

        volumeVo.setPoolId(getDataStore().getId());
        volumeDao.update(volumeVo.getId(), volumeVo);

        String newValues = ReflectionToStringBuilderUtils.reflectOnlySelectedFields(volumeVo, "path", "size", "format", "encryptFormat", "poolId");
        s_logger.debug(String.format("Updated %s from %s to %s ", volumeVo.getVolumeDescription(), previousValues, newValues));
    }

    protected void updateResourceCount(VolumeObjectTO newVolume, VolumeVO oldVolume) {
        if (newVolume == null || newVolume.getSize() == null || oldVolume == null || oldVolume.getSize() == null) {
            return;
        }

        long newVolumeSize = newVolume.getSize();
        long oldVolumeSize = oldVolume.getSize();
        if (newVolumeSize != oldVolumeSize) {
            if (oldVolumeSize < newVolumeSize) {
                resourceLimitMgr.incrementResourceCount(oldVolume.getAccountId(), ResourceType.primary_storage, oldVolume.isDisplayVolume(), newVolumeSize - oldVolumeSize);
            } else {
                resourceLimitMgr.decrementResourceCount(oldVolume.getAccountId(), ResourceType.primary_storage, oldVolume.isDisplayVolume(), oldVolumeSize - newVolumeSize);
            }
        }
    }

   protected void handleProcessEventCopyCmdAnswerNotPrimaryStore(VolumeObjectTO newVolume) {
        VolumeDataStoreVO volStore = volumeStoreDao.findByStoreVolume(dataStore.getId(), getId());

        String previousValues = ReflectionToStringBuilderUtils.reflectOnlySelectedFields(volStore, "installPath", "size");

        volStore.setInstallPath(newVolume.getPath());
        Long newVolumeSize = newVolume.getSize();

        if (newVolumeSize != null) {
            volStore.setSize(newVolumeSize);
        }

        volumeStoreDao.update(volStore.getId(), volStore);

        String newValues = ReflectionToStringBuilderUtils.reflectOnlySelectedFields(volStore, "installPath", "size");
        s_logger.debug(String.format("Updated volume_store_ref %s from %s to %s.", ReflectionToStringBuilderUtils.reflectOnlySelectedFields(volStore, "id", "volumeId"),
          previousValues, newValues));
    }

    protected void handleProcessEventAnswer(CreateObjectAnswer createObjectAnswer) {
        handleProcessEventAnswer(createObjectAnswer, true);
    }

    protected void handleProcessEventAnswer(CreateObjectAnswer createObjectAnswer, boolean setFormat) {
        if (!isPrimaryDataStore()) {
            return;
        }

        VolumeObjectTO newVolume = (VolumeObjectTO)createObjectAnswer.getData();
        VolumeVO volumeVo = volumeDao.findById(getId());
        updateVolumeInfo(newVolume, volumeVo, true, setFormat);
        updateResourceCount(newVolume, volumeVo);
    }

    protected void handleProcessEventAnswer(DownloadAnswer downloadAnswer) {
        if (isPrimaryDataStore()) {
            return;
        }

        VolumeDataStoreVO volumeDataStoreVo = volumeStoreDao.findByStoreVolume(dataStore.getId(), getId());
        String previousValues = ReflectionToStringBuilderUtils.reflectOnlySelectedFields(volumeDataStoreVo, "installPath", "checksum");

        volumeDataStoreVo.setInstallPath(downloadAnswer.getInstallPath());
        volumeDataStoreVo.setChecksum(downloadAnswer.getCheckSum());
        volumeStoreDao.update(volumeDataStoreVo.getId(), volumeDataStoreVo);

        String newValues = ReflectionToStringBuilderUtils.reflectOnlySelectedFields(volumeDataStoreVo, "installPath", "checksum");
        s_logger.debug(String.format("Updated volume_store_ref %s from %s to %s.", ReflectionToStringBuilderUtils.
          reflectOnlySelectedFields(volumeDataStoreVo, "id", "volumeId"), previousValues, newValues));
    }
    @Override
    public void incRefCount() {
        updateRefCount(true);
    }

    @Override
    public void decRefCount() {
        updateRefCount(false);
    }

    protected void updateRefCount(boolean increase){
        if (dataStore == null) {
            return;
        }

        if (imageAndImageCacheRoles.contains(dataStore.getRole())) {
            VolumeDataStoreVO store = volumeStoreDao.findByStoreVolume(dataStore.getId(), getId());

            if (increase) {
                store.incrRefCnt();
            } else {
                store.decrRefCnt();
            }

            store.setLastUpdated(new Date());
            volumeStoreDao.update(store.getId(), store);
        }
    }

    @Override
    public Long getRefCount() {
        if (dataStore == null) {
            return null;
        }

        if (imageAndImageCacheRoles.contains(dataStore.getRole())) {
            VolumeDataStoreVO store = volumeStoreDao.findByStoreVolume(dataStore.getId(), getId());
            return store.getRefCnt();
        }
        return null;
    }

    @Override
    public void processEventOnly(ObjectInDataStoreStateMachine.Event event, Answer answer) {
        try {
            if (answer instanceof CopyCmdAnswer){
                handleProcessEventAnswer((CopyCmdAnswer) answer, false, false);
            } else if (answer instanceof CreateObjectAnswer) {
                handleProcessEventAnswer((CreateObjectAnswer) answer, false);
            } else if (answer instanceof DownloadAnswer) {
                handleProcessEventAnswer((DownloadAnswer) answer);
            }
        } catch (RuntimeException ex) {
            expungeEntryOnOperationFailed(event);
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
        return dataStore == null ? true : dataStore.delete(this);
    }

    @Override
    public Long getVmSnapshotChainSize() {
        return volumeVO.getVmSnapshotChainSize();
    }

    @Override
    public Class<?> getEntityType() {
        return Volume.class;
    }

    @Override
    public String getExternalUuid() {
        return volumeVO.getExternalUuid();
    }

    @Override
    public void setExternalUuid(String externalUuid) {
        volumeVO.setExternalUuid(externalUuid);
    }

    @Override
    public Long getPassphraseId() {
        return volumeVO.getPassphraseId();
    }

    @Override
    public void setPassphraseId(Long id) {
        volumeVO.setPassphraseId(id);
    }

    /**
     * Removes passphrase reference from underlying volume. Also removes the associated passphrase entry if it is the last user.
     */
    public void deletePassphrase() {
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                Long passphraseId = volumeVO.getPassphraseId();
                if (passphraseId != null) {
                    volumeVO.setPassphraseId(null);
                    volumeDao.persist(volumeVO);

                    s_logger.debug(String.format("Checking to see if we can delete passphrase id %s", passphraseId));
                    List<VolumeVO> volumes = volumeDao.listVolumesByPassphraseId(passphraseId);

                    if (volumes != null && !volumes.isEmpty()) {
                        s_logger.debug("Other volumes use this passphrase, skipping deletion");
                        return;
                    }

                    s_logger.debug(String.format("Deleting passphrase %s", passphraseId));
                    passphraseDao.remove(passphraseId);
                }
            }
        });
    }

    /**
     * Looks up passphrase from underlying volume.
     * @return passphrase as bytes
     */
    public byte[] getPassphrase() {
        PassphraseVO passphrase = passphraseDao.findById(volumeVO.getPassphraseId());
        if (passphrase != null) {
            return passphrase.getPassphrase();
        }
        return new byte[0];
    }

    @Override
    public String getEncryptFormat() { return volumeVO.getEncryptFormat(); }

    @Override
    public void setEncryptFormat(String encryptFormat) {
        volumeVO.setEncryptFormat(encryptFormat);
    }
}
