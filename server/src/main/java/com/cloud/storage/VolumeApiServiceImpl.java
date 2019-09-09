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
package com.cloud.storage;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import org.apache.cloudstack.api.command.user.volume.AttachVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.CreateVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.DetachVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.ExtractVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.GetUploadParamsForVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.MigrateVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.ResizeVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.UploadVolumeCmd;
import org.apache.cloudstack.api.response.GetUploadParamsResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.VolumeOrchestrationService;
import org.apache.cloudstack.engine.subsystem.api.storage.ChapInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.HostScope;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.Scope;
import org.apache.cloudstack.engine.subsystem.api.storage.StoragePoolAllocator;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService.VolumeApiResult;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.jobs.AsyncJob;
import org.apache.cloudstack.framework.jobs.AsyncJobExecutionContext;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.framework.jobs.Outcome;
import org.apache.cloudstack.framework.jobs.dao.VmWorkJobDao;
import org.apache.cloudstack.framework.jobs.impl.AsyncJobVO;
import org.apache.cloudstack.framework.jobs.impl.OutcomeImpl;
import org.apache.cloudstack.framework.jobs.impl.VmWorkJobVO;
import org.apache.cloudstack.jobs.JobInfo;
import org.apache.cloudstack.storage.command.AttachAnswer;
import org.apache.cloudstack.storage.command.AttachCommand;
import org.apache.cloudstack.storage.command.DettachCommand;
import org.apache.cloudstack.storage.command.TemplateOrVolumePostUploadCommand;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreVO;
import org.apache.cloudstack.storage.image.datastore.ImageStoreEntity;
import org.apache.cloudstack.utils.identity.ManagementServerNode;
import org.apache.cloudstack.utils.imagestore.ImageStoreUtil;
import org.apache.cloudstack.utils.volume.VirtualMachineDiskInfo;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ModifyTargetsCommand;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.api.ApiDBUtils;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.Domain;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.gpu.GPU;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.HypervisorCapabilitiesVO;
import com.cloud.hypervisor.dao.HypervisorCapabilitiesDao;
import com.cloud.org.Grouping;
import com.cloud.serializer.GsonHelper;
import com.cloud.server.ResourceTag;
import com.cloud.server.TaggedResourceService;
import com.cloud.service.dao.ServiceOfferingDetailsDao;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.StoragePoolTagsDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.snapshot.SnapshotApiService;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.template.TemplateManager;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.User;
import com.cloud.user.VmDiskStatisticsVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.VmDiskStatisticsDao;
import com.cloud.utils.DateUtil;
import com.cloud.utils.EncryptionUtil;
import com.cloud.utils.EnumUtils;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.Predicate;
import com.cloud.utils.ReflectionUse;
import com.cloud.utils.StringUtils;
import com.cloud.utils.UriUtils;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionCallbackWithException;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.db.UUIDManager;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmService;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.VmWork;
import com.cloud.vm.VmWorkAttachVolume;
import com.cloud.vm.VmWorkConstants;
import com.cloud.vm.VmWorkDetachVolume;
import com.cloud.vm.VmWorkExtractVolume;
import com.cloud.vm.VmWorkJobHandler;
import com.cloud.vm.VmWorkJobHandlerProxy;
import com.cloud.vm.VmWorkMigrateVolume;
import com.cloud.vm.VmWorkResizeVolume;
import com.cloud.vm.VmWorkSerializer;
import com.cloud.vm.VmWorkTakeVolumeSnapshot;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

public class VolumeApiServiceImpl extends ManagerBase implements VolumeApiService, VmWorkJobHandler, Configurable {
    private final static Logger s_logger = Logger.getLogger(VolumeApiServiceImpl.class);
    public static final String VM_WORK_JOB_HANDLER = VolumeApiServiceImpl.class.getSimpleName();

    @Inject
    private UserVmManager _userVmMgr;
    @Inject
    private VolumeOrchestrationService _volumeMgr;
    @Inject
    private EntityManager _entityMgr;
    @Inject
    private AgentManager _agentMgr;
    @Inject
    private TemplateManager _tmpltMgr;
    @Inject
    private SnapshotManager _snapshotMgr;
    @Inject
    private AccountManager _accountMgr;
    @Inject
    private ConfigurationManager _configMgr;
    @Inject
    private VolumeDao _volsDao;
    @Inject
    private HostDao _hostDao;
    @Inject
    private SnapshotDao _snapshotDao;
    @Inject
    private ServiceOfferingDetailsDao _serviceOfferingDetailsDao;
    @Inject
    private UserVmDao _userVmDao;
    @Inject
    private UserVmService _userVmService;
    @Inject
    private VolumeDataStoreDao _volumeStoreDao;
    @Inject
    private VMInstanceDao _vmInstanceDao;
    @Inject
    private PrimaryDataStoreDao _storagePoolDao;
    @Inject
    private DiskOfferingDao _diskOfferingDao;
    @Inject
    private AccountDao _accountDao;
    @Inject
    private DataCenterDao _dcDao;
    @Inject
    private VMTemplateDao _templateDao;
    @Inject
    private ResourceLimitService _resourceLimitMgr;
    @Inject
    private VmDiskStatisticsDao _vmDiskStatsDao;
    @Inject
    private VMSnapshotDao _vmSnapshotDao;
    @Inject
    private ConfigurationDao _configDao;
    @Inject
    private DataStoreManager dataStoreMgr;
    @Inject
    private VolumeService volService;
    @Inject
    private VolumeDataFactory volFactory;
    @Inject
    private SnapshotApiService snapshotMgr;
    @Inject
    private UUIDManager _uuidMgr;
    @Inject
    private HypervisorCapabilitiesDao _hypervisorCapabilitiesDao;
    @Inject
    private AsyncJobManager _jobMgr;
    @Inject
    private VmWorkJobDao _workJobDao;
    @Inject
    private ClusterDetailsDao _clusterDetailsDao;
    @Inject
    private StorageManager storageMgr;
    @Inject
    private StoragePoolTagsDao storagePoolTagsDao;
    @Inject
    private StorageUtil storageUtil;
    @Inject
    public TaggedResourceService taggedResourceService;

    protected Gson _gson;

    private List<StoragePoolAllocator> _storagePoolAllocators;

    private List<HypervisorType> supportingDefaultHV;

    VmWorkJobHandlerProxy _jobHandlerProxy = new VmWorkJobHandlerProxy(this);

    static final ConfigKey<Long> VmJobCheckInterval = new ConfigKey<Long>("Advanced", Long.class, "vm.job.check.interval", "3000", "Interval in milliseconds to check if the job is complete", false);

    static final ConfigKey<Boolean> VolumeUrlCheck = new ConfigKey<Boolean>("Advanced", Boolean.class, "volume.url.check", "true",
            "Check the url for a volume before downloading it from the management server. Set to false when you managment has no internet access.", true);

    private long _maxVolumeSizeInGb;
    private final StateMachine2<Volume.State, Volume.Event, Volume> _volStateMachine;

    protected VolumeApiServiceImpl() {
        _volStateMachine = Volume.State.getStateMachine();
        _gson = GsonHelper.getGsonLogger();
    }

    /*
     * Upload the volume to secondary storage.
     */
    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_VOLUME_UPLOAD, eventDescription = "uploading volume", async = true)
    public VolumeVO uploadVolume(UploadVolumeCmd cmd) throws ResourceAllocationException {
        Account caller = CallContext.current().getCallingAccount();
        long ownerId = cmd.getEntityOwnerId();
        Account owner = _entityMgr.findById(Account.class, ownerId);
        Long zoneId = cmd.getZoneId();
        String volumeName = cmd.getVolumeName();
        String url = cmd.getUrl();
        String format = cmd.getFormat();
        Long diskOfferingId = cmd.getDiskOfferingId();
        String imageStoreUuid = cmd.getImageStoreUuid();
        DataStore store = _tmpltMgr.getImageStore(imageStoreUuid, zoneId);

        validateVolume(caller, ownerId, zoneId, volumeName, url, format, diskOfferingId);

        VolumeVO volume = persistVolume(owner, zoneId, volumeName, url, cmd.getFormat(), diskOfferingId, Volume.State.Allocated);

        VolumeInfo vol = volFactory.getVolume(volume.getId());

        RegisterVolumePayload payload = new RegisterVolumePayload(cmd.getUrl(), cmd.getChecksum(), cmd.getFormat());
        vol.addPayload(payload);

        volService.registerVolume(vol, store);
        return volume;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VOLUME_UPLOAD, eventDescription = "uploading volume for post upload", async = true)
    public GetUploadParamsResponse uploadVolume(final GetUploadParamsForVolumeCmd cmd) throws ResourceAllocationException, MalformedURLException {
        Account caller = CallContext.current().getCallingAccount();
        long ownerId = cmd.getEntityOwnerId();
        final Account owner = _entityMgr.findById(Account.class, ownerId);
        final Long zoneId = cmd.getZoneId();
        final String volumeName = cmd.getName();
        String format = cmd.getFormat();
        final Long diskOfferingId = cmd.getDiskOfferingId();
        String imageStoreUuid = cmd.getImageStoreUuid();
        final DataStore store = _tmpltMgr.getImageStore(imageStoreUuid, zoneId);

        validateVolume(caller, ownerId, zoneId, volumeName, null, format, diskOfferingId);

        return Transaction.execute(new TransactionCallbackWithException<GetUploadParamsResponse, MalformedURLException>() {
            @Override
            public GetUploadParamsResponse doInTransaction(TransactionStatus status) throws MalformedURLException {

                VolumeVO volume = persistVolume(owner, zoneId, volumeName, null, cmd.getFormat(), diskOfferingId, Volume.State.NotUploaded);

                VolumeInfo vol = volFactory.getVolume(volume.getId());

                RegisterVolumePayload payload = new RegisterVolumePayload(null, cmd.getChecksum(), cmd.getFormat());
                vol.addPayload(payload);

                Pair<EndPoint, DataObject> pair = volService.registerVolumeForPostUpload(vol, store);
                EndPoint ep = pair.first();
                DataObject dataObject = pair.second();

                GetUploadParamsResponse response = new GetUploadParamsResponse();

                String ssvmUrlDomain = _configDao.getValue(Config.SecStorageSecureCopyCert.key());

                String url = ImageStoreUtil.generatePostUploadUrl(ssvmUrlDomain, ep.getPublicAddr(), vol.getUuid());
                response.setPostURL(new URL(url));

                // set the post url, this is used in the monitoring thread to determine the SSVM
                VolumeDataStoreVO volumeStore = _volumeStoreDao.findByVolume(vol.getId());
                assert (volumeStore != null) : "sincle volume is registered, volumestore cannot be null at this stage";
                volumeStore.setExtractUrl(url);
                _volumeStoreDao.persist(volumeStore);

                response.setId(UUID.fromString(vol.getUuid()));

                int timeout = ImageStoreUploadMonitorImpl.getUploadOperationTimeout();
                DateTime currentDateTime = new DateTime(DateTimeZone.UTC);
                String expires = currentDateTime.plusMinutes(timeout).toString();
                response.setTimeout(expires);

                String key = _configDao.getValue(Config.SSVMPSK.key());
                /*
                 * encoded metadata using the post upload config key
                 */
                TemplateOrVolumePostUploadCommand command = new TemplateOrVolumePostUploadCommand(vol.getId(), vol.getUuid(), volumeStore.getInstallPath(), cmd.getChecksum(), vol.getType().toString(),
                        vol.getName(), vol.getFormat().toString(), dataObject.getDataStore().getUri(), dataObject.getDataStore().getRole().toString());
                command.setLocalPath(volumeStore.getLocalDownloadPath());
                //using the existing max upload size configuration
                command.setProcessTimeout(NumbersUtil.parseLong(_configDao.getValue("vmware.package.ova.timeout"), 3600));
                command.setMaxUploadSize(_configDao.getValue(Config.MaxUploadVolumeSize.key()));
                command.setDefaultMaxAccountSecondaryStorage(_configDao.getValue(Config.DefaultMaxAccountSecondaryStorage.key()));
                command.setAccountId(vol.getAccountId());
                Gson gson = new GsonBuilder().create();
                String metadata = EncryptionUtil.encodeData(gson.toJson(command), key);
                response.setMetadata(metadata);

                /*
                 * signature calculated on the url, expiry, metadata.
                 */
                response.setSignature(EncryptionUtil.generateSignature(metadata + url + expires, key));
                return response;
            }
        });
    }

    private boolean validateVolume(Account caller, long ownerId, Long zoneId, String volumeName, String url, String format, Long diskOfferingId) throws ResourceAllocationException {

        // permission check
        Account volumeOwner = _accountMgr.getActiveAccountById(ownerId);
        _accountMgr.checkAccess(caller, null, true, volumeOwner);

        // Check that the resource limit for volumes won't be exceeded
        _resourceLimitMgr.checkResourceLimit(volumeOwner, ResourceType.volume);

        // Verify that zone exists
        DataCenterVO zone = _dcDao.findById(zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException("Unable to find zone by id " + zoneId);
        }

        // Check if zone is disabled
        if (Grouping.AllocationState.Disabled == zone.getAllocationState() && !_accountMgr.isRootAdmin(caller.getId())) {
            throw new PermissionDeniedException("Cannot perform this operation, Zone is currently disabled: " + zoneId);
        }

        //validating the url only when url is not null. url can be null incase of form based post upload
        if (url != null) {
            if (url.toLowerCase().contains("file://")) {
                throw new InvalidParameterValueException("File:// type urls are currently unsupported");
            }
            UriUtils.validateUrl(format, url);
            if (VolumeUrlCheck.value()) { // global setting that can be set when their MS does not have internet access
                s_logger.debug("Checking url: " + url);
                UriUtils.checkUrlExistence(url);
            }
            // Check that the resource limit for secondary storage won't be exceeded
            _resourceLimitMgr.checkResourceLimit(_accountMgr.getAccount(ownerId), ResourceType.secondary_storage, UriUtils.getRemoteSize(url));
        } else {
            _resourceLimitMgr.checkResourceLimit(_accountMgr.getAccount(ownerId), ResourceType.secondary_storage);
        }

        try {
            ImageFormat.valueOf(format.toUpperCase());
        } catch (IllegalArgumentException e) {
            s_logger.debug("ImageFormat IllegalArgumentException: " + e.getMessage());
            throw new IllegalArgumentException("Image format: " + format + " is incorrect. Supported formats are " + EnumUtils.listValues(ImageFormat.values()));
        }

        // Check that the the disk offering specified is valid
        if (diskOfferingId != null) {
            DiskOfferingVO diskOffering = _diskOfferingDao.findById(diskOfferingId);
            if ((diskOffering == null) || diskOffering.getRemoved() != null || !DiskOfferingVO.Type.Disk.equals(diskOffering.getType())) {
                throw new InvalidParameterValueException("Please specify a valid disk offering.");
            }
            if (!diskOffering.isCustomized()) {
                throw new InvalidParameterValueException("Please specify a custom sized disk offering.");
            }

            _configMgr.checkDiskOfferingAccess(volumeOwner, diskOffering, zone);
        }

        return false;
    }

    public String getRandomVolumeName() {
        return UUID.randomUUID().toString();
    }

    @DB
    protected VolumeVO persistVolume(final Account owner, final Long zoneId, final String volumeName, final String url, final String format, final Long diskOfferingId, final Volume.State state) {
        return Transaction.execute(new TransactionCallback<VolumeVO>() {
            @Override
            public VolumeVO doInTransaction(TransactionStatus status) {
                VolumeVO volume = new VolumeVO(volumeName, zoneId, -1, -1, -1, new Long(-1), null, null, Storage.ProvisioningType.THIN, 0, Volume.Type.DATADISK);
                volume.setPoolId(null);
                volume.setDataCenterId(zoneId);
                volume.setPodId(null);
                volume.setState(state); // initialize the state
                // to prevent a null pointer deref I put the system account id here when no owner is given.
                // TODO Decide if this is valid or whether  throwing a CloudRuntimeException is more appropriate
                volume.setAccountId((owner == null) ? Account.ACCOUNT_ID_SYSTEM : owner.getAccountId());
                volume.setDomainId((owner == null) ? Domain.ROOT_DOMAIN : owner.getDomainId());

                if (diskOfferingId == null) {
                    DiskOfferingVO diskOfferingVO = _diskOfferingDao.findByUniqueName("Cloud.com-Custom");
                    if (diskOfferingVO != null) {
                        long defaultDiskOfferingId = diskOfferingVO.getId();
                        volume.setDiskOfferingId(defaultDiskOfferingId);
                    }
                } else {
                    volume.setDiskOfferingId(diskOfferingId);

                    DiskOfferingVO diskOfferingVO = _diskOfferingDao.findById(diskOfferingId);

                    Boolean isCustomizedIops = diskOfferingVO != null && diskOfferingVO.isCustomizedIops() != null ? diskOfferingVO.isCustomizedIops() : false;

                    if (isCustomizedIops == null || !isCustomizedIops) {
                        volume.setMinIops(diskOfferingVO.getMinIops());
                        volume.setMaxIops(diskOfferingVO.getMaxIops());
                    }
                }

                // volume.setSize(size);
                volume.setInstanceId(null);
                volume.setUpdated(new Date());
                volume.setDomainId((owner == null) ? Domain.ROOT_DOMAIN : owner.getDomainId());
                volume.setFormat(ImageFormat.valueOf(format));
                volume = _volsDao.persist(volume);
                CallContext.current().setEventDetails("Volume Id: " + volume.getUuid());

                // Increment resource count during allocation; if actual creation fails,
                // decrement it
                _resourceLimitMgr.incrementResourceCount(volume.getAccountId(), ResourceType.volume);
                //url can be null incase of postupload
                if (url != null) {
                    _resourceLimitMgr.incrementResourceCount(volume.getAccountId(), ResourceType.secondary_storage, UriUtils.getRemoteSize(url));
                }

                return volume;
            }
        });
    }

    /**
     * Retrieves the volume name from CreateVolumeCmd object.
     *
     * If the retrieved volume name is null, empty or blank, then A random name
     * will be generated using getRandomVolumeName method.
     *
     * @param cmd
     * @return Either the retrieved name or a random name.
     */
    public String getVolumeNameFromCommand(CreateVolumeCmd cmd) {
        String userSpecifiedName = cmd.getVolumeName();

        if (org.apache.commons.lang.StringUtils.isBlank(userSpecifiedName)) {
            userSpecifiedName = getRandomVolumeName();
        }

        return userSpecifiedName;
    }

    /*
     * Just allocate a volume in the database, don't send the createvolume cmd
     * to hypervisor. The volume will be finally created only when it's attached
     * to a VM.
     */
    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_VOLUME_CREATE, eventDescription = "creating volume", create = true)
    public VolumeVO allocVolume(CreateVolumeCmd cmd) throws ResourceAllocationException {
        Account caller = CallContext.current().getCallingAccount();

        long ownerId = cmd.getEntityOwnerId();
        Account owner = _accountMgr.getActiveAccountById(ownerId);
        Boolean displayVolume = cmd.getDisplayVolume();

        // permission check
        _accountMgr.checkAccess(caller, null, true, _accountMgr.getActiveAccountById(ownerId));

        if (displayVolume == null) {
            displayVolume = true;
        } else {
            if (!_accountMgr.isRootAdmin(caller.getId())) {
                throw new PermissionDeniedException("Cannot update parameter displayvolume, only admin permitted ");
            }
        }

        // Check that the resource limit for volumes won't be exceeded
        _resourceLimitMgr.checkResourceLimit(owner, ResourceType.volume, displayVolume);

        Long zoneId = cmd.getZoneId();
        Long diskOfferingId = null;
        DiskOfferingVO diskOffering = null;
        Long size = null;
        Long minIops = null;
        Long maxIops = null;
        // Volume VO used for extracting the source template id
        VolumeVO parentVolume = null;

        // validate input parameters before creating the volume
        if (cmd.getSnapshotId() == null && cmd.getDiskOfferingId() == null) {
            throw new InvalidParameterValueException("At least one of disk Offering ID or snapshot ID must be passed whilst creating volume");
        }

        // disallow passing disk offering ID with DATA disk volume snapshots
        if (cmd.getSnapshotId() != null && cmd.getDiskOfferingId() != null) {
            SnapshotVO snapshot = _snapshotDao.findById(cmd.getSnapshotId());
            if (snapshot != null) {
                parentVolume = _volsDao.findByIdIncludingRemoved(snapshot.getVolumeId());
                if (parentVolume != null && parentVolume.getVolumeType() != Volume.Type.ROOT)
                    throw new InvalidParameterValueException("Disk Offering ID cannot be passed whilst creating volume from snapshot other than ROOT disk snapshots");
            }
            parentVolume = null;
        }

        if (cmd.getDiskOfferingId() != null) { // create a new volume

            diskOfferingId = cmd.getDiskOfferingId();
            size = cmd.getSize();
            Long sizeInGB = size;
            if (size != null) {
                if (size > 0) {
                    size = size * 1024 * 1024 * 1024; // user specify size in GB
                } else {
                    throw new InvalidParameterValueException("Disk size must be larger than 0");
                }
            }

            // Check that the the disk offering is specified
            diskOffering = _diskOfferingDao.findById(diskOfferingId);
            if ((diskOffering == null) || diskOffering.getRemoved() != null || !DiskOfferingVO.Type.Disk.equals(diskOffering.getType())) {
                throw new InvalidParameterValueException("Please specify a valid disk offering.");
            }

            if (diskOffering.isCustomized()) {
                if (size == null) {
                    throw new InvalidParameterValueException("This disk offering requires a custom size specified");
                }
                Long customDiskOfferingMaxSize = VolumeOrchestrationService.CustomDiskOfferingMaxSize.value();
                Long customDiskOfferingMinSize = VolumeOrchestrationService.CustomDiskOfferingMinSize.value();

                if ((sizeInGB < customDiskOfferingMinSize) || (sizeInGB > customDiskOfferingMaxSize)) {
                    throw new InvalidParameterValueException("Volume size: " + sizeInGB + "GB is out of allowed range. Max: " + customDiskOfferingMaxSize + " Min:" + customDiskOfferingMinSize);
                }
            }

            if (!diskOffering.isCustomized() && size != null) {
                throw new InvalidParameterValueException("This disk offering does not allow custom size");
            }

            _configMgr.checkDiskOfferingAccess(owner, diskOffering, _dcDao.findById(zoneId));

            if (diskOffering.getDiskSize() > 0) {
                size = diskOffering.getDiskSize();
            }

            Boolean isCustomizedIops = diskOffering.isCustomizedIops();

            if (isCustomizedIops != null) {
                if (isCustomizedIops) {
                    minIops = cmd.getMinIops();
                    maxIops = cmd.getMaxIops();

                    if (minIops == null && maxIops == null) {
                        minIops = 0L;
                        maxIops = 0L;
                    } else {
                        if (minIops == null || minIops <= 0) {
                            throw new InvalidParameterValueException("The min IOPS must be greater than 0.");
                        }

                        if (maxIops == null) {
                            maxIops = 0L;
                        }

                        if (minIops > maxIops) {
                            throw new InvalidParameterValueException("The min IOPS must be less than or equal to the max IOPS.");
                        }
                    }
                } else {
                    minIops = diskOffering.getMinIops();
                    maxIops = diskOffering.getMaxIops();
                }
            }

            if (!validateVolumeSizeRange(size)) {// convert size from mb to gb
                // for validation
                throw new InvalidParameterValueException("Invalid size for custom volume creation: " + size + " ,max volume size is:" + _maxVolumeSizeInGb);
            }
        }

        if (cmd.getSnapshotId() != null) { // create volume from snapshot
            Long snapshotId = cmd.getSnapshotId();
            SnapshotVO snapshotCheck = _snapshotDao.findById(snapshotId);
            if (snapshotCheck == null) {
                throw new InvalidParameterValueException("unable to find a snapshot with id " + snapshotId);
            }

            if (snapshotCheck.getState() != Snapshot.State.BackedUp) {
                throw new InvalidParameterValueException("Snapshot id=" + snapshotId + " is not in " + Snapshot.State.BackedUp + " state yet and can't be used for volume creation");
            }
            parentVolume = _volsDao.findByIdIncludingRemoved(snapshotCheck.getVolumeId());

            if (zoneId == null) {
                // if zoneId is not provided, we default to create volume in the same zone as the snapshot zone.
                zoneId = snapshotCheck.getDataCenterId();
            }

            _configMgr.checkDiskOfferingAccess(null, diskOffering, _dcDao.findById(zoneId));

            if (diskOffering == null) { // Pure snapshot is being used to create volume.
                diskOfferingId = snapshotCheck.getDiskOfferingId();
                diskOffering = _diskOfferingDao.findById(diskOfferingId);

                minIops = snapshotCheck.getMinIops();
                maxIops = snapshotCheck.getMaxIops();
                size = snapshotCheck.getSize(); // ; disk offering is used for tags purposes
            } else {
                if (size < snapshotCheck.getSize()) {
                    throw new InvalidParameterValueException(String.format("Invalid size for volume creation: %dGB, snapshot size is: %dGB",
                            size / (1024 * 1024 * 1024), snapshotCheck.getSize() / (1024 * 1024 * 1024)));
                }
            }

            // check snapshot permissions
            _accountMgr.checkAccess(caller, null, true, snapshotCheck);

            // one step operation - create volume in VM's cluster and attach it
            // to the VM
            Long vmId = cmd.getVirtualMachineId();
            if (vmId != null) {
                // Check that the virtual machine ID is valid and it's a user vm
                UserVmVO vm = _userVmDao.findById(vmId);
                if (vm == null || vm.getType() != VirtualMachine.Type.User) {
                    throw new InvalidParameterValueException("Please specify a valid User VM.");
                }

                // Check that the VM is in the correct state
                if (vm.getState() != State.Running && vm.getState() != State.Stopped) {
                    throw new InvalidParameterValueException("Please specify a VM that is either running or stopped.");
                }

                // permission check
                _accountMgr.checkAccess(caller, null, false, vm);
            }
        }

        Storage.ProvisioningType provisioningType = diskOffering.getProvisioningType();

        // Check that the resource limit for primary storage won't be exceeded
        _resourceLimitMgr.checkResourceLimit(owner, ResourceType.primary_storage, displayVolume, new Long(size));

        // Verify that zone exists
        DataCenterVO zone = _dcDao.findById(zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException("Unable to find zone by id " + zoneId);
        }

        // Check if zone is disabled
        if (Grouping.AllocationState.Disabled == zone.getAllocationState() && !_accountMgr.isRootAdmin(caller.getId())) {
            throw new PermissionDeniedException("Cannot perform this operation, Zone is currently disabled: " + zoneId);
        }

        // If local storage is disabled then creation of volume with local disk
        // offering not allowed
        if (!zone.isLocalStorageEnabled() && diskOffering.isUseLocalStorage()) {
            throw new InvalidParameterValueException("Zone is not configured to use local storage but volume's disk offering " + diskOffering.getName() + " uses it");
        }

        String userSpecifiedName = getVolumeNameFromCommand(cmd);

        return commitVolume(cmd, caller, owner, displayVolume, zoneId, diskOfferingId, provisioningType, size, minIops, maxIops, parentVolume, userSpecifiedName,
                _uuidMgr.generateUuid(Volume.class, cmd.getCustomId()));
    }

    private VolumeVO commitVolume(final CreateVolumeCmd cmd, final Account caller, final Account owner, final Boolean displayVolume, final Long zoneId, final Long diskOfferingId,
            final Storage.ProvisioningType provisioningType, final Long size, final Long minIops, final Long maxIops, final VolumeVO parentVolume, final String userSpecifiedName, final String uuid) {
        return Transaction.execute(new TransactionCallback<VolumeVO>() {
            @Override
            public VolumeVO doInTransaction(TransactionStatus status) {
                VolumeVO volume = new VolumeVO(userSpecifiedName, -1, -1, -1, -1, new Long(-1), null, null, provisioningType, 0, Volume.Type.DATADISK);
                volume.setPoolId(null);
                volume.setUuid(uuid);
                volume.setDataCenterId(zoneId);
                volume.setPodId(null);
                volume.setAccountId(owner.getId());
                volume.setDomainId(owner.getDomainId());
                volume.setDiskOfferingId(diskOfferingId);
                volume.setSize(size);
                volume.setMinIops(minIops);
                volume.setMaxIops(maxIops);
                volume.setInstanceId(null);
                volume.setUpdated(new Date());
                volume.setDisplayVolume(displayVolume);
                if (parentVolume != null) {
                    volume.setTemplateId(parentVolume.getTemplateId());
                    volume.setFormat(parentVolume.getFormat());
                } else {
                    volume.setTemplateId(null);
                }

                volume = _volsDao.persist(volume);
                if (cmd.getSnapshotId() == null && displayVolume) {
                    // for volume created from snapshot, create usage event after volume creation
                    UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VOLUME_CREATE, volume.getAccountId(), volume.getDataCenterId(), volume.getId(), volume.getName(), diskOfferingId, null, size,
                            Volume.class.getName(), volume.getUuid(), displayVolume);
                }

                CallContext.current().setEventDetails("Volume Id: " + volume.getUuid());

                // Increment resource count during allocation; if actual creation fails,
                // decrement it
                _resourceLimitMgr.incrementResourceCount(volume.getAccountId(), ResourceType.volume, displayVolume);
                _resourceLimitMgr.incrementResourceCount(volume.getAccountId(), ResourceType.primary_storage, displayVolume, new Long(volume.getSize()));
                return volume;
            }
        });
    }

    public boolean validateVolumeSizeRange(long size) {
        if (size < 0 || (size > 0 && size < (1024 * 1024 * 1024))) {
            throw new InvalidParameterValueException("Please specify a size of at least 1 GB.");
        } else if (size > (_maxVolumeSizeInGb * 1024 * 1024 * 1024)) {
            throw new InvalidParameterValueException("Requested volume size is " + size + ", but the maximum size allowed is " + _maxVolumeSizeInGb + " GB.");
        }

        return true;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_VOLUME_CREATE, eventDescription = "creating volume", async = true)
    public VolumeVO createVolume(CreateVolumeCmd cmd) {
        VolumeVO volume = _volsDao.findById(cmd.getEntityId());
        boolean created = true;

        try {
            if (cmd.getSnapshotId() != null) {
                volume = createVolumeFromSnapshot(volume, cmd.getSnapshotId(), cmd.getVirtualMachineId());
                if (volume.getState() != Volume.State.Ready) {
                    created = false;
                }

                // if VM Id is provided, attach the volume to the VM
                if (cmd.getVirtualMachineId() != null) {
                    try {
                        attachVolumeToVM(cmd.getVirtualMachineId(), volume.getId(), volume.getDeviceId());
                    } catch (Exception ex) {
                        StringBuilder message = new StringBuilder("Volume: ");
                        message.append(volume.getUuid());
                        message.append(" created successfully, but failed to attach the newly created volume to VM: ");
                        message.append(cmd.getVirtualMachineId());
                        message.append(" due to error: ");
                        message.append(ex.getMessage());
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug(message, ex);
                        }
                        throw new CloudRuntimeException(message.toString());
                    }
                }
            }
            return volume;
        } catch (Exception e) {
            created = false;
            VolumeInfo vol = volFactory.getVolume(cmd.getEntityId());
            vol.stateTransit(Volume.Event.DestroyRequested);
            throw new CloudRuntimeException("Failed to create volume: " + volume.getId(), e);
        } finally {
            if (!created) {
                s_logger.trace("Decrementing volume resource count for account id=" + volume.getAccountId() + " as volume failed to create on the backend");
                _resourceLimitMgr.decrementResourceCount(volume.getAccountId(), ResourceType.volume, cmd.getDisplayVolume());
                _resourceLimitMgr.decrementResourceCount(volume.getAccountId(), ResourceType.primary_storage, cmd.getDisplayVolume(), new Long(volume.getSize()));
            }
        }
    }

    protected VolumeVO createVolumeFromSnapshot(VolumeVO volume, long snapshotId, Long vmId) throws StorageUnavailableException {
        VolumeInfo createdVolume = null;
        SnapshotVO snapshot = _snapshotDao.findById(snapshotId);
        snapshot.getVolumeId();

        UserVmVO vm = null;
        if (vmId != null) {
            vm = _userVmDao.findById(vmId);
        }

        // sync old snapshots to region store if necessary

        createdVolume = _volumeMgr.createVolumeFromSnapshot(volume, snapshot, vm);
        VolumeVO volumeVo = _volsDao.findById(createdVolume.getId());
        UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VOLUME_CREATE, createdVolume.getAccountId(), createdVolume.getDataCenterId(), createdVolume.getId(), createdVolume.getName(),
                createdVolume.getDiskOfferingId(), null, createdVolume.getSize(), Volume.class.getName(), createdVolume.getUuid(), volumeVo.isDisplayVolume());

        return volumeVo;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_VOLUME_RESIZE, eventDescription = "resizing volume", async = true)
    public VolumeVO resizeVolume(ResizeVolumeCmd cmd) throws ResourceAllocationException {
        Long newSize;
        Long newMinIops;
        Long newMaxIops;
        Integer newHypervisorSnapshotReserve;
        boolean shrinkOk = cmd.isShrinkOk();

        VolumeVO volume = _volsDao.findById(cmd.getEntityId());
        if (volume == null) {
            throw new InvalidParameterValueException("No such volume");
        }

        // checking if there are any ongoing snapshots on the volume which is to be resized
        List<SnapshotVO> ongoingSnapshots = _snapshotDao.listByStatus(cmd.getId(), Snapshot.State.Creating, Snapshot.State.CreatedOnPrimary, Snapshot.State.BackingUp);
        if (ongoingSnapshots.size() > 0) {
            throw new CloudRuntimeException("There is/are unbacked up snapshot(s) on this volume, resize volume is not permitted, please try again later.");
        }

        /* Does the caller have authority to act on this volume? */
        _accountMgr.checkAccess(CallContext.current().getCallingAccount(), null, true, volume);

        DiskOfferingVO diskOffering = _diskOfferingDao.findById(volume.getDiskOfferingId());
        DiskOfferingVO newDiskOffering = null;

        if (cmd.getNewDiskOfferingId() != null && volume.getDiskOfferingId() != cmd.getNewDiskOfferingId()) {
            newDiskOffering = _diskOfferingDao.findById(cmd.getNewDiskOfferingId());
        }

        /* Only works for KVM/XenServer/VMware (or "Any") for now, and volumes with 'None' since they're just allocated in DB */

        HypervisorType hypervisorType = _volsDao.getHypervisorType(volume.getId());

        if (hypervisorType != HypervisorType.KVM && hypervisorType != HypervisorType.XenServer && hypervisorType != HypervisorType.VMware && hypervisorType != HypervisorType.Any
                && hypervisorType != HypervisorType.None) {
            throw new InvalidParameterValueException("Hypervisor " + hypervisorType + " does not support  rootdisksize override");
        }

        if (volume.getState() != Volume.State.Ready && volume.getState() != Volume.State.Allocated) {
            throw new InvalidParameterValueException("Volume should be in ready or allocated state before attempting a resize. Volume " + volume.getUuid() + " is in state " + volume.getState() + ".");
        }

        // if we are to use the existing disk offering
        if (newDiskOffering == null) {
            newSize = cmd.getSize();
            newHypervisorSnapshotReserve = volume.getHypervisorSnapshotReserve();

            // if the caller is looking to change the size of the volume
            if (newSize != null) {
                if (!diskOffering.isCustomized() && !volume.getVolumeType().equals(Volume.Type.ROOT)) {
                    throw new InvalidParameterValueException("To change a volume's size without providing a new disk offering, its current disk offering must be "
                            + "customizable or it must be a root volume (if providing a disk offering, make sure it is different from the current disk offering).");
                }

                // convert from bytes to GiB
                newSize = newSize << 30;
            } else {
                // no parameter provided; just use the original size of the volume
                newSize = volume.getSize();
            }

            newMinIops = cmd.getMinIops();

            if (newMinIops != null) {
                if (!volume.getVolumeType().equals(Volume.Type.ROOT) && (diskOffering.isCustomizedIops() == null || !diskOffering.isCustomizedIops())) {
                    throw new InvalidParameterValueException("The current disk offering does not support customization of the 'Min IOPS' parameter.");
                }
            } else {
                // no parameter provided; just use the original min IOPS of the volume
                newMinIops = volume.getMinIops();
            }

            newMaxIops = cmd.getMaxIops();

            if (newMaxIops != null) {
                if (!volume.getVolumeType().equals(Volume.Type.ROOT) && (diskOffering.isCustomizedIops() == null || !diskOffering.isCustomizedIops())) {
                    throw new InvalidParameterValueException("The current disk offering does not support customization of the 'Max IOPS' parameter.");
                }
            } else {
                // no parameter provided; just use the original max IOPS of the volume
                newMaxIops = volume.getMaxIops();
            }

            validateIops(newMinIops, newMaxIops);
        } else {
            if (newDiskOffering.getRemoved() != null) {
                throw new InvalidParameterValueException("Requested disk offering has been removed.");
            }

            if (!DiskOfferingVO.Type.Disk.equals(newDiskOffering.getType())) {
                throw new InvalidParameterValueException("Requested disk offering type is invalid.");
            }

            if (diskOffering.getTags() != null) {
                if (!StringUtils.areTagsEqual(diskOffering.getTags(), newDiskOffering.getTags())) {
                    throw new InvalidParameterValueException("The tags on the new and old disk offerings must match.");
                }
            } else if (newDiskOffering.getTags() != null) {
                throw new InvalidParameterValueException("There are no tags on the current disk offering. The new disk offering needs to have no tags, as well.");
            }

            _configMgr.checkDiskOfferingAccess(_accountMgr.getActiveAccountById(volume.getAccountId()), newDiskOffering, _dcDao.findById(volume.getDataCenterId()));

            if (newDiskOffering.isCustomized()) {
                newSize = cmd.getSize();

                if (newSize == null) {
                    throw new InvalidParameterValueException("The new disk offering requires that a size be specified.");
                }

                // convert from GiB to bytes
                newSize = newSize << 30;
            } else {
                if (cmd.getSize() != null) {
                    throw new InvalidParameterValueException("You cannnot pass in a custom disk size to a non-custom disk offering.");
                }

                newSize = newDiskOffering.getDiskSize();
            }

            if (!volume.getSize().equals(newSize) && !volume.getVolumeType().equals(Volume.Type.DATADISK)) {
                throw new InvalidParameterValueException("Only data volumes can be resized via a new disk offering.");
            }

            if (newDiskOffering.isCustomizedIops() != null && newDiskOffering.isCustomizedIops()) {
                newMinIops = cmd.getMinIops() != null ? cmd.getMinIops() : volume.getMinIops();
                newMaxIops = cmd.getMaxIops() != null ? cmd.getMaxIops() : volume.getMaxIops();

                validateIops(newMinIops, newMaxIops);
            } else {
                newMinIops = newDiskOffering.getMinIops();
                newMaxIops = newDiskOffering.getMaxIops();
            }

            // if the hypervisor snapshot reserve value is null, it must remain null (currently only KVM uses null and null is all KVM uses for a value here)
            newHypervisorSnapshotReserve = volume.getHypervisorSnapshotReserve() != null ? newDiskOffering.getHypervisorSnapshotReserve() : null;
        }

        long currentSize = volume.getSize();

        // if the caller is looking to change the size of the volume
        if (currentSize != newSize) {
            if (volume.getInstanceId() != null) {
                // Check that VM to which this volume is attached does not have VM snapshots
                if (_vmSnapshotDao.findByVm(volume.getInstanceId()).size() > 0) {
                    throw new InvalidParameterValueException("A volume that is attached to a VM with any VM snapshots cannot be resized.");
                }
            }

            if (!validateVolumeSizeRange(newSize)) {
                throw new InvalidParameterValueException("Requested size out of range");
            }

            Long storagePoolId = volume.getPoolId();

            if (storagePoolId != null) {
                StoragePoolVO storagePoolVO = _storagePoolDao.findById(storagePoolId);

                if (storagePoolVO.isManaged()) {
                    Long instanceId = volume.getInstanceId();

                    if (instanceId != null) {
                        VMInstanceVO vmInstanceVO = _vmInstanceDao.findById(instanceId);

                        if (vmInstanceVO.getHypervisorType() == HypervisorType.KVM && vmInstanceVO.getState() != State.Stopped) {
                            throw new CloudRuntimeException("This kind of KVM disk cannot be resized while it is connected to a VM that's not in the Stopped state.");
                        }
                    }
                }
            }

            /*
             * Let's make certain they (think they) know what they're doing if they
             * want to shrink by forcing them to provide the shrinkok parameter.
             * This will be checked again at the hypervisor level where we can see
             * the actual disk size.
             */
            if (currentSize > newSize && !shrinkOk) {
                throw new InvalidParameterValueException("Going from existing size of " + currentSize + " to size of " + newSize + " would shrink the volume."
                        + "Need to sign off by supplying the shrinkok parameter with value of true.");
            }

            if (newSize > currentSize) {
                /* Check resource limit for this account on primary storage resource */
                _resourceLimitMgr.checkResourceLimit(_accountMgr.getAccount(volume.getAccountId()), ResourceType.primary_storage, volume.isDisplayVolume(),
                        new Long(newSize - currentSize).longValue());
            }
        }

        // Note: The storage plug-in in question should perform validation on the IOPS to check if a sufficient number of IOPS is available to perform
        // the requested change

        /* If this volume has never been beyond allocated state, short circuit everything and simply update the database. */
        // We need to publish this event to usage_volume table
        if (volume.getState() == Volume.State.Allocated) {
            s_logger.debug("Volume is in the allocated state, but has never been created. Simply updating database with new size and IOPS.");

            volume.setSize(newSize);
            volume.setMinIops(newMinIops);
            volume.setMaxIops(newMaxIops);
            volume.setHypervisorSnapshotReserve(newHypervisorSnapshotReserve);

            if (newDiskOffering != null) {
                volume.setDiskOfferingId(cmd.getNewDiskOfferingId());
            }

            _volsDao.update(volume.getId(), volume);
            UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VOLUME_RESIZE, volume.getAccountId(), volume.getDataCenterId(), volume.getId(), volume.getName(),
                    volume.getDiskOfferingId(), volume.getTemplateId(), volume.getSize(), Volume.class.getName(), volume.getUuid());
            return volume;
        }

        UserVmVO userVm = _userVmDao.findById(volume.getInstanceId());

        if (userVm != null) {
            if (volume.getVolumeType().equals(Volume.Type.ROOT) && userVm.getPowerState() != VirtualMachine.PowerState.PowerOff && hypervisorType == HypervisorType.VMware) {
                s_logger.error(" For ROOT volume resize VM should be in Power Off state.");
                throw new InvalidParameterValueException("VM current state is : " + userVm.getPowerState() + ". But VM should be in " + VirtualMachine.PowerState.PowerOff + " state.");
            }
            // serialize VM operation
            AsyncJobExecutionContext jobContext = AsyncJobExecutionContext.getCurrentExecutionContext();

            if (jobContext.isJobDispatchedBy(VmWorkConstants.VM_WORK_JOB_DISPATCHER)) {
                // avoid re-entrance

                VmWorkJobVO placeHolder = null;

                placeHolder = createPlaceHolderWork(userVm.getId());

                try {
                    return orchestrateResizeVolume(volume.getId(), currentSize, newSize, newMinIops, newMaxIops, newHypervisorSnapshotReserve,
                            newDiskOffering != null ? cmd.getNewDiskOfferingId() : null, shrinkOk);
                } finally {
                    _workJobDao.expunge(placeHolder.getId());
                }
            } else {
                Outcome<Volume> outcome = resizeVolumeThroughJobQueue(userVm.getId(), volume.getId(), currentSize, newSize, newMinIops, newMaxIops, newHypervisorSnapshotReserve,
                        newDiskOffering != null ? cmd.getNewDiskOfferingId() : null, shrinkOk);

                try {
                    outcome.get();
                } catch (InterruptedException e) {
                    throw new RuntimeException("Operation was interrupted", e);
                } catch (java.util.concurrent.ExecutionException e) {
                    throw new RuntimeException("Execution exception", e);
                }

                Object jobResult = _jobMgr.unmarshallResultObject(outcome.getJob());

                if (jobResult != null) {
                    if (jobResult instanceof ConcurrentOperationException) {
                        throw (ConcurrentOperationException)jobResult;
                    } else if (jobResult instanceof ResourceAllocationException) {
                        throw (ResourceAllocationException)jobResult;
                    } else if (jobResult instanceof RuntimeException) {
                        throw (RuntimeException)jobResult;
                    } else if (jobResult instanceof Throwable) {
                        throw new RuntimeException("Unexpected exception", (Throwable)jobResult);
                    } else if (jobResult instanceof Long) {
                        return _volsDao.findById((Long)jobResult);
                    }
                }

                return volume;
            }
        }

        return orchestrateResizeVolume(volume.getId(), currentSize, newSize, newMinIops, newMaxIops, newHypervisorSnapshotReserve, newDiskOffering != null ? cmd.getNewDiskOfferingId() : null,
                shrinkOk);
    }

    private void validateIops(Long minIops, Long maxIops) {
        if ((minIops == null && maxIops != null) || (minIops != null && maxIops == null)) {
            throw new InvalidParameterValueException("Either 'miniops' and 'maxiops' must both be provided or neither must be provided.");
        }

        if (minIops != null && maxIops != null) {
            if (minIops > maxIops) {
                throw new InvalidParameterValueException("The 'miniops' parameter must be less than or equal to the 'maxiops' parameter.");
            }
        }
    }

    private VolumeVO orchestrateResizeVolume(long volumeId, long currentSize, long newSize, Long newMinIops, Long newMaxIops, Integer newHypervisorSnapshotReserve, Long newDiskOfferingId,
            boolean shrinkOk) {
        final VolumeVO volume = _volsDao.findById(volumeId);
        UserVmVO userVm = _userVmDao.findById(volume.getInstanceId());
        StoragePoolVO storagePool = _storagePoolDao.findById(volume.getPoolId());
        boolean isManaged = storagePool.isManaged();

        if (!storageMgr.storagePoolHasEnoughSpaceForResize(storagePool, currentSize, newSize)) {
            throw new CloudRuntimeException("Storage pool " + storagePool.getName() + " does not have enough space to resize volume " + volume.getName());
        }

        /*
         * get a list of hosts to send the commands to, try the system the
         * associated vm is running on first, then the last known place it ran.
         * If not attached to a userVm, we pass 'none' and resizevolume.sh is ok
         * with that since it only needs the vm name to live resize
         */
        long[] hosts = null;
        String instanceName = "none";
        if (userVm != null) {
            instanceName = userVm.getInstanceName();
            if (userVm.getHostId() != null) {
                hosts = new long[] {userVm.getHostId()};
            } else if (userVm.getLastHostId() != null) {
                hosts = new long[] {userVm.getLastHostId()};
            }

            final String errorMsg = "The VM must be stopped or the disk detached in order to resize with the XenServer Hypervisor.";

            if (storagePool.isManaged() && storagePool.getHypervisor() == HypervisorType.Any && hosts != null && hosts.length > 0) {
                HostVO host = _hostDao.findById(hosts[0]);

                if (currentSize != newSize && host.getHypervisorType() == HypervisorType.XenServer && !userVm.getState().equals(State.Stopped)) {
                    throw new InvalidParameterValueException(errorMsg);
                }
            }

            /* Xen only works offline, SR does not support VDI.resizeOnline */
            if (currentSize != newSize && _volsDao.getHypervisorType(volume.getId()) == HypervisorType.XenServer && !userVm.getState().equals(State.Stopped)) {
                throw new InvalidParameterValueException(errorMsg);
            }
        }

        ResizeVolumePayload payload = new ResizeVolumePayload(newSize, newMinIops, newMaxIops, newHypervisorSnapshotReserve, shrinkOk, instanceName, hosts, isManaged);

        try {
            VolumeInfo vol = volFactory.getVolume(volume.getId());
            vol.addPayload(payload);

            // this call to resize has a different impact depending on whether the
            // underlying primary storage is managed or not
            // if managed, this is the chance for the plug-in to change the size and/or IOPS values
            // if not managed, this is the chance for the plug-in to talk to the hypervisor layer
            // to change the size of the disk
            AsyncCallFuture<VolumeApiResult> future = volService.resize(vol);
            VolumeApiResult result = future.get();

            if (result.isFailed()) {
                s_logger.warn("Failed to resize the volume " + volume);
                String details = "";
                if (result.getResult() != null && !result.getResult().isEmpty()) {
                    details = result.getResult();
                }
                throw new CloudRuntimeException(details);
            }

            // managed storage is designed in such a way that the storage plug-in does not
            // talk to the hypervisor layer; as such, if the storage is managed and the
            // current and new sizes are different, then CloudStack (i.e. not a storage plug-in)
            // needs to tell the hypervisor to resize the disk
            if (storagePool.isManaged() && currentSize != newSize) {
                if (hosts != null && hosts.length > 0) {
                    HostVO hostVO = _hostDao.findById(hosts[0]);

                    if (hostVO.getHypervisorType() != HypervisorType.KVM) {
                        volService.resizeVolumeOnHypervisor(volumeId, newSize, hosts[0], instanceName);
                    }
                }
            }

            if (newDiskOfferingId != null) {
                volume.setDiskOfferingId(newDiskOfferingId);
            }

            if (currentSize != newSize) {
                volume.setSize(newSize);
            }

            _volsDao.update(volume.getId(), volume);

            /* Update resource count for the account on primary storage resource */
            if (!shrinkOk) {
                _resourceLimitMgr.incrementResourceCount(volume.getAccountId(), ResourceType.primary_storage, volume.isDisplayVolume(), newSize - currentSize);
            } else {
                _resourceLimitMgr.decrementResourceCount(volume.getAccountId(), ResourceType.primary_storage, volume.isDisplayVolume(), currentSize - newSize);
            }

            UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VOLUME_RESIZE, volume.getAccountId(), volume.getDataCenterId(), volume.getId(), volume.getName(),
                    volume.getDiskOfferingId(), volume.getTemplateId(), volume.getSize(), Volume.class.getName(), volume.getUuid());

            return volume;

        } catch (Exception e) {
            throw new CloudRuntimeException("Exception caught during resize volume operation of volume UUID: " + volume.getUuid(), e);
        }
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VOLUME_DELETE, eventDescription = "deleting volume")
    /**
     * Executes the removal of the volume. If the volume is only allocated we do not try to remove it from primary and secondary storage.
     * Otherwise, after the removal in the database, we will try to remove the volume from both primary and secondary storage.
     */
    public boolean deleteVolume(long volumeId, Account caller) throws ConcurrentOperationException {
        VolumeVO volume = retrieveAndValidateVolume(volumeId, caller);
        try {
            destroyVolumeIfPossible(volume);
            // Mark volume as removed if volume has not been created on primary or secondary
            if (volume.getState() == Volume.State.Allocated) {
                _volsDao.remove(volumeId);
                stateTransitTo(volume, Volume.Event.DestroyRequested);
                return true;
            }
            expungeVolumesInPrimaryStorageIfNeeded(volume);
            expungeVolumesInSecondaryStorageIfNeeded(volume);
            cleanVolumesCache(volume);
            return true;
        } catch (InterruptedException | ExecutionException | NoTransitionException e) {
            s_logger.warn("Failed to expunge volume: " + volume.getUuid(), e);
            return false;
        }
    }

    /**
     *  Retrieves and validates the volume for the {@link #deleteVolume(long, Account)} method. The following validation are executed.
     *  <ul>
     *      <li> if no volume is found in the database, we throw an {@link InvalidParameterValueException};
     *      <li> if there are snapshots operation on the volume we cannot delete it. Therefore, an {@link InvalidParameterValueException} is thrown;
     *      <li> if the volume is still attached to a VM we throw an {@link InvalidParameterValueException};
     *      <li> if volume state is in {@link Volume.State#UploadOp}, we check the {@link VolumeDataStoreVO}. Then, if the {@link VolumeDataStoreVO} for the given volume has download status of {@link VMTemplateStorageResourceAssoc.Status#DOWNLOAD_IN_PROGRESS}, an exception is throw;
     *      <li> if the volume state is in {@link Volume.State#NotUploaded} or if the state is {@link Volume.State#UploadInProgress}, an {@link InvalidParameterValueException} is thrown;
     *      <li> we also check if the user has access to the given volume using {@link AccountManager#checkAccess(Account, org.apache.cloudstack.acl.SecurityChecker.AccessType, boolean, String)}.
     *  </ul>
     *
     *  After all validations we return the volume object.
     */
    protected VolumeVO retrieveAndValidateVolume(long volumeId, Account caller) {
        VolumeVO volume = _volsDao.findById(volumeId);
        if (volume == null) {
            throw new InvalidParameterValueException("Unable to find volume with ID: " + volumeId);
        }
        if (!_snapshotMgr.canOperateOnVolume(volume)) {
            throw new InvalidParameterValueException("There are snapshot operations in progress on the volume, unable to delete it");
        }
        if (volume.getInstanceId() != null) {
            throw new InvalidParameterValueException("Please specify a volume that is not attached to any VM.");
        }
        if (volume.getState() == Volume.State.UploadOp) {
            VolumeDataStoreVO volumeStore = _volumeStoreDao.findByVolume(volume.getId());
            if (volumeStore.getDownloadState() == VMTemplateStorageResourceAssoc.Status.DOWNLOAD_IN_PROGRESS) {
                throw new InvalidParameterValueException("Please specify a volume that is not uploading");
            }
        }
        if (volume.getState() == Volume.State.NotUploaded || volume.getState() == Volume.State.UploadInProgress) {
            throw new InvalidParameterValueException("The volume is either getting uploaded or it may be initiated shortly, please wait for it to be completed");
        }
        _accountMgr.checkAccess(caller, null, true, volume);
        return volume;
    }

    /**
     * Destroy the volume if possible and then decrement the following resource types.
     * <ul>
     *  <li> {@link ResourceType#volume};
     *  <li> {@link ResourceType#primary_storage}
     * </ul>
     *
     * A volume can be destroyed if it is not in any of the following states.
     * <ul>
     *  <li> {@value Volume.State#Destroy};
     *  <li> {@value Volume.State#Expunging};
     *  <li> {@value Volume.State#Expunged}.
     * </ul>
     *
     * The volume is destroyed via {@link VolumeService#destroyVolume(long)} method.
     */
    protected void destroyVolumeIfPossible(VolumeVO volume) {
        if (volume.getState() != Volume.State.Destroy && volume.getState() != Volume.State.Expunging && volume.getState() != Volume.State.Expunged) {
            volService.destroyVolume(volume.getId());

            // Decrement the resource count for volumes and primary storage belonging user VM's only
            _resourceLimitMgr.decrementResourceCount(volume.getAccountId(), ResourceType.volume, volume.isDisplayVolume());
            _resourceLimitMgr.decrementResourceCount(volume.getAccountId(), ResourceType.primary_storage, volume.isDisplayVolume(), volume.getSize());
        }
    }

    /**
     * We will check if the given volume is in the primary storage. If it is, we will execute an asynchronous call to delete it there.
     * If the volume is not in the primary storage, we do nothing here.
     */
    protected void expungeVolumesInPrimaryStorageIfNeeded(VolumeVO volume) throws InterruptedException, ExecutionException {
        VolumeInfo volOnPrimary = volFactory.getVolume(volume.getId(), DataStoreRole.Primary);
        if (volOnPrimary != null) {
            s_logger.info("Expunging volume " + volume.getId() + " from primary data store");
            AsyncCallFuture<VolumeApiResult> future = volService.expungeVolumeAsync(volOnPrimary);
            future.get();
        }
    }

    /**
     * We will check if the given volume is in the secondary storage. If the volume is not in the primary storage, we do nothing here.
     * If it is, we will execute an asynchronous call to delete it there. Then, we decrement the {@link ResourceType#secondary_storage} for the account that owns the volume.
     */
    protected void expungeVolumesInSecondaryStorageIfNeeded(VolumeVO volume) throws InterruptedException, ExecutionException {
        VolumeInfo volOnSecondary = volFactory.getVolume(volume.getId(), DataStoreRole.Image);
        if (volOnSecondary != null) {
            s_logger.info("Expunging volume " + volume.getId() + " from secondary data store");
            AsyncCallFuture<VolumeApiResult> future2 = volService.expungeVolumeAsync(volOnSecondary);
            future2.get();

            _resourceLimitMgr.decrementResourceCount(volOnSecondary.getAccountId(), ResourceType.secondary_storage, volOnSecondary.getSize());
        }
    }

    /**
     * Clean volumes cache entries (if they exist).
     */
    protected void cleanVolumesCache(VolumeVO volume) {
        List<VolumeInfo> cacheVols = volFactory.listVolumeOnCache(volume.getId());
        if (CollectionUtils.isEmpty(cacheVols)) {
            return;
        }
        for (VolumeInfo volOnCache : cacheVols) {
            s_logger.info("Delete volume from image cache store: " + volOnCache.getDataStore().getName());
            volOnCache.delete();
        }
    }

    protected boolean stateTransitTo(Volume vol, Volume.Event event) throws NoTransitionException {
        return _volStateMachine.transitTo(vol, event, null, _volsDao);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VOLUME_ATTACH, eventDescription = "attaching volume", async = true)
    public Volume attachVolumeToVM(AttachVolumeCmd command) {
        return attachVolumeToVM(command.getVirtualMachineId(), command.getId(), command.getDeviceId());
    }

    private Volume orchestrateAttachVolumeToVM(Long vmId, Long volumeId, Long deviceId) {
        VolumeInfo volumeToAttach = volFactory.getVolume(volumeId);

        if (volumeToAttach.isAttachedVM()) {
            throw new CloudRuntimeException("This volume is already attached to a VM.");
        }

        UserVmVO vm = _userVmDao.findById(vmId);
        VolumeVO exstingVolumeOfVm = null;
        List<VolumeVO> rootVolumesOfVm = _volsDao.findByInstanceAndType(vmId, Volume.Type.ROOT);
        if (rootVolumesOfVm.size() > 1) {
            throw new CloudRuntimeException("The VM " + vm.getHostName() + " has more than one ROOT volume and is in an invalid state.");
        } else {
            if (!rootVolumesOfVm.isEmpty()) {
                exstingVolumeOfVm = rootVolumesOfVm.get(0);
            } else {
                // locate data volume of the vm
                List<VolumeVO> diskVolumesOfVm = _volsDao.findByInstanceAndType(vmId, Volume.Type.DATADISK);
                for (VolumeVO diskVolume : diskVolumesOfVm) {
                    if (diskVolume.getState() != Volume.State.Allocated) {
                        exstingVolumeOfVm = diskVolume;
                        break;
                    }
                }
            }
        }

        HypervisorType rootDiskHyperType = vm.getHypervisorType();
        HypervisorType volumeToAttachHyperType = _volsDao.getHypervisorType(volumeToAttach.getId());

        VolumeInfo newVolumeOnPrimaryStorage = volumeToAttach;

        //don't create volume on primary storage if its being attached to the vm which Root's volume hasn't been created yet
        StoragePoolVO destPrimaryStorage = null;
        if (exstingVolumeOfVm != null && !exstingVolumeOfVm.getState().equals(Volume.State.Allocated)) {
            destPrimaryStorage = _storagePoolDao.findById(exstingVolumeOfVm.getPoolId());
        }

        boolean volumeOnSecondary = volumeToAttach.getState() == Volume.State.Uploaded;

        if (destPrimaryStorage != null && (volumeToAttach.getState() == Volume.State.Allocated || volumeOnSecondary)) {
            try {
                newVolumeOnPrimaryStorage = _volumeMgr.createVolumeOnPrimaryStorage(vm, volumeToAttach, rootDiskHyperType, destPrimaryStorage);
            } catch (NoTransitionException e) {
                s_logger.debug("Failed to create volume on primary storage", e);
                throw new CloudRuntimeException("Failed to create volume on primary storage", e);
            }
        }

        // reload the volume from db
        newVolumeOnPrimaryStorage = volFactory.getVolume(newVolumeOnPrimaryStorage.getId());
        boolean moveVolumeNeeded = needMoveVolume(exstingVolumeOfVm, newVolumeOnPrimaryStorage);

        if (moveVolumeNeeded) {
            PrimaryDataStoreInfo primaryStore = (PrimaryDataStoreInfo)newVolumeOnPrimaryStorage.getDataStore();
            if (primaryStore.isLocal()) {
                throw new CloudRuntimeException(
                        "Failed to attach local data volume " + volumeToAttach.getName() + " to VM " + vm.getDisplayName() + " as migration of local data volume is not allowed");
            }
            StoragePoolVO vmRootVolumePool = _storagePoolDao.findById(exstingVolumeOfVm.getPoolId());

            try {
                newVolumeOnPrimaryStorage = _volumeMgr.moveVolume(newVolumeOnPrimaryStorage, vmRootVolumePool.getDataCenterId(), vmRootVolumePool.getPodId(), vmRootVolumePool.getClusterId(),
                        volumeToAttachHyperType);
            } catch (ConcurrentOperationException e) {
                s_logger.debug("move volume failed", e);
                throw new CloudRuntimeException("move volume failed", e);
            } catch (StorageUnavailableException e) {
                s_logger.debug("move volume failed", e);
                throw new CloudRuntimeException("move volume failed", e);
            }
        }
        VolumeVO newVol = _volsDao.findById(newVolumeOnPrimaryStorage.getId());
        // Getting the fresh vm object in case of volume migration to check the current state of VM
        if (moveVolumeNeeded || volumeOnSecondary) {
            vm = _userVmDao.findById(vmId);
            if (vm == null) {
                throw new InvalidParameterValueException("VM not found.");
            }
        }
        newVol = sendAttachVolumeCommand(vm, newVol, deviceId);
        return newVol;
    }

    public Volume attachVolumeToVM(Long vmId, Long volumeId, Long deviceId) {
        Account caller = CallContext.current().getCallingAccount();

        // Check that the volume ID is valid
        VolumeInfo volumeToAttach = volFactory.getVolume(volumeId);
        // Check that the volume is a data volume
        if (volumeToAttach == null || !(volumeToAttach.getVolumeType() == Volume.Type.DATADISK || volumeToAttach.getVolumeType() == Volume.Type.ROOT)) {
            throw new InvalidParameterValueException("Please specify a volume with the valid type: " + Volume.Type.ROOT.toString() + " or " + Volume.Type.DATADISK.toString());
        }

        // Check that the volume is not currently attached to any VM
        if (volumeToAttach.getInstanceId() != null) {
            throw new InvalidParameterValueException("Please specify a volume that is not attached to any VM.");
        }

        // Check that the volume is not destroyed
        if (volumeToAttach.getState() == Volume.State.Destroy) {
            throw new InvalidParameterValueException("Please specify a volume that is not destroyed.");
        }

        // Check that the virtual machine ID is valid and it's a user vm
        UserVmVO vm = _userVmDao.findById(vmId);
        if (vm == null || vm.getType() != VirtualMachine.Type.User) {
            throw new InvalidParameterValueException("Please specify a valid User VM.");
        }

        // Check that the VM is in the correct state
        if (vm.getState() != State.Running && vm.getState() != State.Stopped) {
            throw new InvalidParameterValueException("Please specify a VM that is either running or stopped.");
        }

        // Check that the VM and the volume are in the same zone
        if (vm.getDataCenterId() != volumeToAttach.getDataCenterId()) {
            throw new InvalidParameterValueException("Please specify a VM that is in the same zone as the volume.");
        }

        // Check that the device ID is valid
        if (deviceId != null) {
            // validate ROOT volume type
            if (deviceId.longValue() == 0) {
                validateRootVolumeDetachAttach(_volsDao.findById(volumeToAttach.getId()), vm);
                // vm shouldn't have any volume with deviceId 0
                if (!_volsDao.findByInstanceAndDeviceId(vm.getId(), 0).isEmpty()) {
                    throw new InvalidParameterValueException("Vm already has root volume attached to it");
                }
                // volume can't be in Uploaded state
                if (volumeToAttach.getState() == Volume.State.Uploaded) {
                    throw new InvalidParameterValueException("No support for Root volume attach in state " + Volume.State.Uploaded);
                }
            }
        }

        // Check that the number of data volumes attached to VM is less than
        // that supported by hypervisor
        if (deviceId == null || deviceId.longValue() != 0) {
            List<VolumeVO> existingDataVolumes = _volsDao.findByInstanceAndType(vmId, Volume.Type.DATADISK);
            int maxAttachableDataVolumesSupported = getMaxDataVolumesSupported(vm);
            if (existingDataVolumes.size() >= maxAttachableDataVolumesSupported) {
                throw new InvalidParameterValueException(
                        "The specified VM already has the maximum number of data disks (" + maxAttachableDataVolumesSupported + ") attached. Please specify another VM.");
            }
        }

        // If local storage is disabled then attaching a volume with local disk
        // offering not allowed
        DataCenterVO dataCenter = _dcDao.findById(volumeToAttach.getDataCenterId());
        if (!dataCenter.isLocalStorageEnabled()) {
            DiskOfferingVO diskOffering = _diskOfferingDao.findById(volumeToAttach.getDiskOfferingId());
            if (diskOffering.isUseLocalStorage()) {
                throw new InvalidParameterValueException("Zone is not configured to use local storage but volume's disk offering " + diskOffering.getName() + " uses it");
            }
        }

        // if target VM has associated VM snapshots
        List<VMSnapshotVO> vmSnapshots = _vmSnapshotDao.findByVm(vmId);
        if (vmSnapshots.size() > 0) {
            throw new InvalidParameterValueException("Unable to attach volume, please specify a VM that does not have VM snapshots");
        }

        // permission check
        _accountMgr.checkAccess(caller, null, true, volumeToAttach, vm);

        if (!(Volume.State.Allocated.equals(volumeToAttach.getState()) || Volume.State.Ready.equals(volumeToAttach.getState()) || Volume.State.Uploaded.equals(volumeToAttach.getState()))) {
            throw new InvalidParameterValueException("Volume state must be in Allocated, Ready or in Uploaded state");
        }

        Account owner = _accountDao.findById(volumeToAttach.getAccountId());

        if (!(volumeToAttach.getState() == Volume.State.Allocated || volumeToAttach.getState() == Volume.State.Ready)) {
            try {
                _resourceLimitMgr.checkResourceLimit(owner, ResourceType.primary_storage, volumeToAttach.getSize());
            } catch (ResourceAllocationException e) {
                s_logger.error("primary storage resource limit check failed", e);
                throw new InvalidParameterValueException(e.getMessage());
            }
        }

        HypervisorType rootDiskHyperType = vm.getHypervisorType();
        HypervisorType volumeToAttachHyperType = _volsDao.getHypervisorType(volumeToAttach.getId());

        StoragePoolVO volumeToAttachStoragePool = _storagePoolDao.findById(volumeToAttach.getPoolId());

        // managed storage can be used for different types of hypervisors
        // only perform this check if the volume's storage pool is not null and not managed
        if (volumeToAttachStoragePool != null && !volumeToAttachStoragePool.isManaged()) {
            if (volumeToAttachHyperType != HypervisorType.None && rootDiskHyperType != volumeToAttachHyperType) {
                throw new InvalidParameterValueException("Can't attach a volume created by: " + volumeToAttachHyperType + " to a " + rootDiskHyperType + " vm");
            }
        }

        AsyncJobExecutionContext asyncExecutionContext = AsyncJobExecutionContext.getCurrentExecutionContext();

        if (asyncExecutionContext != null) {
            AsyncJob job = asyncExecutionContext.getJob();

            if (s_logger.isInfoEnabled()) {
                s_logger.info("Trying to attaching volume " + volumeId + " to vm instance:" + vm.getId() + ", update async job-" + job.getId() + " progress status");
            }

            _jobMgr.updateAsyncJobAttachment(job.getId(), "Volume", volumeId);
        }

        AsyncJobExecutionContext jobContext = AsyncJobExecutionContext.getCurrentExecutionContext();
        if (jobContext.isJobDispatchedBy(VmWorkConstants.VM_WORK_JOB_DISPATCHER)) {
            // avoid re-entrance

            VmWorkJobVO placeHolder = null;
            placeHolder = createPlaceHolderWork(vmId);
            try {
                return orchestrateAttachVolumeToVM(vmId, volumeId, deviceId);
            } finally {
                _workJobDao.expunge(placeHolder.getId());
            }

        } else {
            Outcome<Volume> outcome = attachVolumeToVmThroughJobQueue(vmId, volumeId, deviceId);

            Volume vol = null;
            try {
                outcome.get();
            } catch (InterruptedException e) {
                throw new RuntimeException("Operation is interrupted", e);
            } catch (java.util.concurrent.ExecutionException e) {
                throw new RuntimeException("Execution excetion", e);
            }

            Object jobResult = _jobMgr.unmarshallResultObject(outcome.getJob());
            if (jobResult != null) {
                if (jobResult instanceof ConcurrentOperationException) {
                    throw (ConcurrentOperationException)jobResult;
                } else if (jobResult instanceof InvalidParameterValueException) {
                    throw (InvalidParameterValueException)jobResult;
                } else if (jobResult instanceof RuntimeException) {
                    throw (RuntimeException)jobResult;
                } else if (jobResult instanceof Throwable) {
                    throw new RuntimeException("Unexpected exception", (Throwable)jobResult);
                } else if (jobResult instanceof Long) {
                    vol = _volsDao.findById((Long)jobResult);
                }
            }
            return vol;
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VOLUME_UPDATE, eventDescription = "updating volume", async = true)
    public Volume updateVolume(long volumeId, String path, String state, Long storageId, Boolean displayVolume, String customId, long entityOwnerId, String chainInfo) {

        VolumeVO volume = _volsDao.findById(volumeId);

        if (volume == null) {
            throw new InvalidParameterValueException("The volume id doesn't exist");
        }

        if (path != null) {
            volume.setPath(path);
        }

        if (chainInfo != null) {
            volume.setChainInfo(chainInfo);
        }

        if (state != null) {
            try {
                Volume.State volumeState = Volume.State.valueOf(state);
                volume.setState(volumeState);
            } catch (IllegalArgumentException ex) {
                throw new InvalidParameterValueException("Invalid volume state specified");
            }
        }

        if (storageId != null) {
            StoragePool pool = _storagePoolDao.findById(storageId);
            if (pool.getDataCenterId() != volume.getDataCenterId()) {
                throw new InvalidParameterValueException("Invalid storageId specified; refers to the pool outside of the volume's zone");
            }
            volume.setPoolId(pool.getId());
        }

        if (customId != null) {
            volume.setUuid(customId);
        }

        updateDisplay(volume, displayVolume);

        _volsDao.update(volumeId, volume);

        return volume;
    }

    @Override
    public void updateDisplay(Volume volume, Boolean displayVolume) {
        // 1. Resource limit changes
        updateResourceCount(volume, displayVolume);

        // 2. generate usage event if not in destroyed state
        saveUsageEvent(volume, displayVolume);

        // 3. Set the flag
        if (displayVolume != null && displayVolume != volume.isDisplayVolume()) {
            // FIXME - Confused - typecast for now.
            ((VolumeVO)volume).setDisplayVolume(displayVolume);
            _volsDao.update(volume.getId(), (VolumeVO)volume);
        }

    }

    private void updateResourceCount(Volume volume, Boolean displayVolume) {
        // Update only when the flag has changed.
        if (displayVolume != null && displayVolume != volume.isDisplayVolume()) {
            _resourceLimitMgr.changeResourceCount(volume.getAccountId(), ResourceType.volume, displayVolume);
            _resourceLimitMgr.changeResourceCount(volume.getAccountId(), ResourceType.primary_storage, displayVolume, new Long(volume.getSize()));
        }
    }

    private void saveUsageEvent(Volume volume, Boolean displayVolume) {

        // Update only when the flag has changed  &&  only when volume in a non-destroyed state.
        if ((displayVolume != null && displayVolume != volume.isDisplayVolume()) && !isVolumeDestroyed(volume)) {
            if (displayVolume) {
                // flag turned 1 equivalent to freshly created volume
                UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VOLUME_CREATE, volume.getAccountId(), volume.getDataCenterId(), volume.getId(), volume.getName(), volume.getDiskOfferingId(),
                        volume.getTemplateId(), volume.getSize(), Volume.class.getName(), volume.getUuid());
            } else {
                // flag turned 0 equivalent to deleting a volume
                UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VOLUME_DELETE, volume.getAccountId(), volume.getDataCenterId(), volume.getId(), volume.getName(), Volume.class.getName(),
                        volume.getUuid());
            }
        }
    }

    private boolean isVolumeDestroyed(Volume volume) {
        if (volume.getState() == Volume.State.Destroy || volume.getState() == Volume.State.Expunging && volume.getState() == Volume.State.Expunged) {
            return true;
        }
        return false;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VOLUME_DETACH, eventDescription = "detaching volume", async = true)
    public Volume detachVolumeFromVM(DetachVolumeCmd cmmd) {
        Account caller = CallContext.current().getCallingAccount();
        if ((cmmd.getId() == null && cmmd.getDeviceId() == null && cmmd.getVirtualMachineId() == null) || (cmmd.getId() != null && (cmmd.getDeviceId() != null || cmmd.getVirtualMachineId() != null))
                || (cmmd.getId() == null && (cmmd.getDeviceId() == null || cmmd.getVirtualMachineId() == null))) {
            throw new InvalidParameterValueException("Please provide either a volume id, or a tuple(device id, instance id)");
        }

        Long volumeId = cmmd.getId();
        VolumeVO volume = null;

        if (volumeId != null) {
            volume = _volsDao.findById(volumeId);
        } else {
            volume = _volsDao.findByInstanceAndDeviceId(cmmd.getVirtualMachineId(), cmmd.getDeviceId()).get(0);
        }

        // Check that the volume ID is valid
        if (volume == null) {
            throw new InvalidParameterValueException("Unable to find volume with ID: " + volumeId);
        }

        Long vmId = null;

        if (cmmd.getVirtualMachineId() == null) {
            vmId = volume.getInstanceId();
        } else {
            vmId = cmmd.getVirtualMachineId();
        }

        // Permissions check
        _accountMgr.checkAccess(caller, null, true, volume);

        // Check that the volume is currently attached to a VM
        if (vmId == null) {
            throw new InvalidParameterValueException("The specified volume is not attached to a VM.");
        }

        // Check that the VM is in the correct state
        UserVmVO vm = _userVmDao.findById(vmId);
        if (vm.getState() != State.Running && vm.getState() != State.Stopped && vm.getState() != State.Destroyed) {
            throw new InvalidParameterValueException("Please specify a VM that is either running or stopped.");
        }

        // Check that the volume is a data/root volume
        if (!(volume.getVolumeType() == Volume.Type.ROOT || volume.getVolumeType() == Volume.Type.DATADISK)) {
            throw new InvalidParameterValueException("Please specify volume of type " + Volume.Type.DATADISK.toString() + " or " + Volume.Type.ROOT.toString());
        }

        // Root volume detach is allowed for following hypervisors: Xen/KVM/VmWare
        if (volume.getVolumeType() == Volume.Type.ROOT) {
            validateRootVolumeDetachAttach(volume, vm);
        }

        // Don't allow detach if target VM has associated VM snapshots
        List<VMSnapshotVO> vmSnapshots = _vmSnapshotDao.findByVm(vmId);
        if (vmSnapshots.size() > 0) {
            throw new InvalidParameterValueException("Unable to detach volume, please specify a VM that does not have VM snapshots");
        }

        AsyncJobExecutionContext asyncExecutionContext = AsyncJobExecutionContext.getCurrentExecutionContext();
        if (asyncExecutionContext != null) {
            AsyncJob job = asyncExecutionContext.getJob();

            if (s_logger.isInfoEnabled()) {
                s_logger.info("Trying to attaching volume " + volumeId + "to vm instance:" + vm.getId() + ", update async job-" + job.getId() + " progress status");
            }

            _jobMgr.updateAsyncJobAttachment(job.getId(), "Volume", volumeId);
        }

        AsyncJobExecutionContext jobContext = AsyncJobExecutionContext.getCurrentExecutionContext();
        if (jobContext.isJobDispatchedBy(VmWorkConstants.VM_WORK_JOB_DISPATCHER)) {
            // avoid re-entrance
            VmWorkJobVO placeHolder = null;
            placeHolder = createPlaceHolderWork(vmId);
            try {
                return orchestrateDetachVolumeFromVM(vmId, volumeId);
            } finally {
                _workJobDao.expunge(placeHolder.getId());
            }
        } else {
            Outcome<Volume> outcome = detachVolumeFromVmThroughJobQueue(vmId, volumeId);

            Volume vol = null;
            try {
                outcome.get();
            } catch (InterruptedException e) {
                throw new RuntimeException("Operation is interrupted", e);
            } catch (java.util.concurrent.ExecutionException e) {
                throw new RuntimeException("Execution excetion", e);
            }

            Object jobResult = _jobMgr.unmarshallResultObject(outcome.getJob());
            if (jobResult != null) {
                if (jobResult instanceof ConcurrentOperationException) {
                    throw (ConcurrentOperationException)jobResult;
                } else if (jobResult instanceof RuntimeException) {
                    throw (RuntimeException)jobResult;
                } else if (jobResult instanceof Throwable) {
                    throw new RuntimeException("Unexpected exception", (Throwable)jobResult);
                } else if (jobResult instanceof Long) {
                    vol = _volsDao.findById((Long)jobResult);
                }
            }
            return vol;
        }
    }

    private void validateRootVolumeDetachAttach(VolumeVO volume, UserVmVO vm) {
        if (!(vm.getHypervisorType() == HypervisorType.XenServer || vm.getHypervisorType() == HypervisorType.VMware || vm.getHypervisorType() == HypervisorType.KVM
                || vm.getHypervisorType() == HypervisorType.Simulator)) {
            throw new InvalidParameterValueException("Root volume detach is not supported for hypervisor type " + vm.getHypervisorType());
        }
        if (!(vm.getState() == State.Stopped) || (vm.getState() == State.Destroyed)) {
            throw new InvalidParameterValueException("Root volume detach can happen only when vm is in states: " + State.Stopped.toString() + " or " + State.Destroyed.toString());
        }

        if (volume.getPoolId() != null) {
            StoragePoolVO pool = _storagePoolDao.findById(volume.getPoolId());
            if (pool.isManaged()) {
                throw new InvalidParameterValueException("Root volume detach is not supported for Managed DataStores");
            }
        }
    }

    @ActionEvent(eventType = EventTypes.EVENT_VOLUME_DETACH, eventDescription = "detaching volume")
    public Volume detachVolumeViaDestroyVM(long vmId, long volumeId) {
        return orchestrateDetachVolumeFromVM(vmId, volumeId);
    }

    private Volume orchestrateDetachVolumeFromVM(long vmId, long volumeId) {
        Volume volume = _volsDao.findById(volumeId);
        VMInstanceVO vm = _vmInstanceDao.findById(vmId);

        String errorMsg = "Failed to detach volume " + volume.getName() + " from VM " + vm.getHostName();
        boolean sendCommand = vm.getState() == State.Running;

        Long hostId = vm.getHostId();

        if (hostId == null) {
            hostId = vm.getLastHostId();
            HostVO host = _hostDao.findById(hostId);

            if (host != null && host.getHypervisorType() == HypervisorType.VMware) {
                sendCommand = true;
            }
        }

        HostVO host = null;
        StoragePoolVO volumePool = _storagePoolDao.findByIdIncludingRemoved(volume.getPoolId());

        if (hostId != null) {
            host = _hostDao.findById(hostId);

            if (host != null && host.getHypervisorType() == HypervisorType.XenServer && volumePool != null && volumePool.isManaged()) {
                sendCommand = true;
            }
        }

        if (volumePool == null) {
            sendCommand = false;
        }

        Answer answer = null;

        if (sendCommand) {
            // collect vm disk statistics before detach a volume
            UserVmVO userVm = _userVmDao.findById(vmId);
            if (userVm != null && userVm.getType() == VirtualMachine.Type.User) {
                _userVmService.collectVmDiskStatistics(userVm);
            }

            DataTO volTO = volFactory.getVolume(volume.getId()).getTO();
            DiskTO disk = new DiskTO(volTO, volume.getDeviceId(), volume.getPath(), volume.getVolumeType());

            DettachCommand cmd = new DettachCommand(disk, vm.getInstanceName());

            cmd.setManaged(volumePool.isManaged());

            cmd.setStorageHost(volumePool.getHostAddress());
            cmd.setStoragePort(volumePool.getPort());

            cmd.set_iScsiName(volume.get_iScsiName());

            try {
                answer = _agentMgr.send(hostId, cmd);
            } catch (Exception e) {
                throw new CloudRuntimeException(errorMsg + " due to: " + e.getMessage());
            }
        }

        if (!sendCommand || (answer != null && answer.getResult())) {
            // Mark the volume as detached
            _volsDao.detachVolume(volume.getId());

            // volume.getPoolId() should be null if the VM we are detaching the disk from has never been started before
            if (volume.getPoolId() != null) {
                DataStore dataStore = dataStoreMgr.getDataStore(volume.getPoolId(), DataStoreRole.Primary);
                volService.revokeAccess(volFactory.getVolume(volume.getId()), host, dataStore);
            }
            if (volumePool != null && hostId != null) {
                handleTargetsForVMware(hostId, volumePool.getHostAddress(), volumePool.getPort(), volume.get_iScsiName());
            }
            return _volsDao.findById(volumeId);
        } else {

            if (answer != null) {
                String details = answer.getDetails();
                if (details != null && !details.isEmpty()) {
                    errorMsg += "; " + details;
                }
            }

            throw new CloudRuntimeException(errorMsg);
        }
    }

    public void updateMissingRootDiskController(final VMInstanceVO vm, final String rootVolChainInfo) {
        if (vm == null || !VirtualMachine.Type.User.equals(vm.getType()) || Strings.isNullOrEmpty(rootVolChainInfo)) {
            return;
        }
        String rootDiskController = null;
        try {
            final VirtualMachineDiskInfo infoInChain = _gson.fromJson(rootVolChainInfo, VirtualMachineDiskInfo.class);
            if (infoInChain != null) {
                rootDiskController = infoInChain.getControllerFromDeviceBusName();
            }
            final UserVmVO userVmVo = _userVmDao.findById(vm.getId());
            if ((rootDiskController != null) && (!rootDiskController.isEmpty())) {
                _userVmDao.loadDetails(userVmVo);
                _userVmMgr.persistDeviceBusInfo(userVmVo, rootDiskController);
            }
        } catch (JsonParseException e) {
            s_logger.debug("Error parsing chain info json: " + e.getMessage());
        }
    }

    private void handleTargetsForVMware(long hostId, String storageAddress, int storagePort, String iScsiName) {
        HostVO host = _hostDao.findById(hostId);

        if (host.getHypervisorType() == HypervisorType.VMware) {
            ModifyTargetsCommand cmd = new ModifyTargetsCommand();

            List<Map<String, String>> targets = new ArrayList<>();

            Map<String, String> target = new HashMap<>();

            target.put(ModifyTargetsCommand.STORAGE_HOST, storageAddress);
            target.put(ModifyTargetsCommand.STORAGE_PORT, String.valueOf(storagePort));
            target.put(ModifyTargetsCommand.IQN, iScsiName);

            targets.add(target);

            cmd.setTargets(targets);
            cmd.setApplyToAllHostsInCluster(true);
            cmd.setAdd(false);
            cmd.setTargetTypeToRemove(ModifyTargetsCommand.TargetTypeToRemove.DYNAMIC);

            sendModifyTargetsCommand(cmd, hostId);
        }
    }

    private void sendModifyTargetsCommand(ModifyTargetsCommand cmd, long hostId) {
        Answer answer = _agentMgr.easySend(hostId, cmd);

        if (answer == null) {
            String msg = "Unable to get an answer to the modify targets command";

            s_logger.warn(msg);
        } else if (!answer.getResult()) {
            String msg = "Unable to modify target on the following host: " + hostId;

            s_logger.warn(msg);
        }
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VOLUME_MIGRATE, eventDescription = "migrating volume", async = true)
    public Volume migrateVolume(MigrateVolumeCmd cmd) {
        Long volumeId = cmd.getVolumeId();
        Long storagePoolId = cmd.getStoragePoolId();

        VolumeVO vol = _volsDao.findById(volumeId);
        if (vol == null) {
            throw new InvalidParameterValueException("Failed to find the volume id: " + volumeId);
        }

        if (vol.getState() != Volume.State.Ready) {
            throw new InvalidParameterValueException("Volume must be in ready state");
        }

        if (vol.getPoolId() == storagePoolId) {
            throw new InvalidParameterValueException("Volume " + vol + " is already on the destination storage pool");
        }

        boolean liveMigrateVolume = false;
        Long instanceId = vol.getInstanceId();
        Long srcClusterId = null;
        VMInstanceVO vm = null;
        if (instanceId != null) {
            vm = _vmInstanceDao.findById(instanceId);
        }

        // Check that Vm to which this volume is attached does not have VM Snapshots
        // OfflineVmwareMigration: considder if this is needed and desirable
        if (vm != null && _vmSnapshotDao.findByVm(vm.getId()).size() > 0) {
            throw new InvalidParameterValueException("Volume cannot be migrated, please remove all VM snapshots for VM to which this volume is attached");
        }

        // OfflineVmwareMigration: extract this block as method and check if it is subject to regression
        if (vm != null && vm.getState() == State.Running) {
            // Check if the VM is GPU enabled.
            if (_serviceOfferingDetailsDao.findDetail(vm.getServiceOfferingId(), GPU.Keys.pciDevice.toString()) != null) {
                throw new InvalidParameterValueException("Live Migration of GPU enabled VM is not supported");
            }
            // Check if the underlying hypervisor supports storage motion.
            Long hostId = vm.getHostId();
            if (hostId != null) {
                HostVO host = _hostDao.findById(hostId);
                HypervisorCapabilitiesVO capabilities = null;
                if (host != null) {
                    capabilities = _hypervisorCapabilitiesDao.findByHypervisorTypeAndVersion(host.getHypervisorType(), host.getHypervisorVersion());
                    srcClusterId = host.getClusterId();
                }

                if (capabilities != null) {
                    liveMigrateVolume = capabilities.isStorageMotionSupported();
                }
            }

            // If vm is running, and hypervisor doesn't support live migration, then return error
            if (!liveMigrateVolume) {
                throw new InvalidParameterValueException("Volume needs to be detached from VM");
            }
        }

        if (liveMigrateVolume && !cmd.isLiveMigrate()) {
            throw new InvalidParameterValueException("The volume " + vol + "is attached to a vm and for migrating it " + "the parameter livemigrate should be specified");
        }

        StoragePool destPool = (StoragePool)dataStoreMgr.getDataStore(storagePoolId, DataStoreRole.Primary);
        if (destPool == null) {
            throw new InvalidParameterValueException("Failed to find the destination storage pool: " + storagePoolId);
        } else if (destPool.isInMaintenance()) {
            throw new InvalidParameterValueException("Cannot migrate volume " + vol + "to the destination storage pool " + destPool.getName() + " as the storage pool is in maintenance mode.");
        }

        if (!storageMgr.storagePoolHasEnoughSpace(Collections.singletonList(vol), destPool)) {
            throw new CloudRuntimeException("Storage pool " + destPool.getName() + " does not have enough space to migrate volume " + vol.getName());
        }

        // OfflineVmwareMigration: check storage tags on disk(offering)s in comparison to destination storage pool
        // OfflineVmwareMigration: if no match return a proper error now
        DiskOfferingVO diskOffering = _diskOfferingDao.findById(vol.getDiskOfferingId());
        if (diskOffering.equals(null)) {
            throw new CloudRuntimeException("volume '" + vol.getUuid() + "', has no diskoffering. Migration target cannot be checked.");
        }
        if (!doesTargetStorageSupportDiskOffering(destPool, diskOffering)) {
            throw new CloudRuntimeException(String.format("Migration target pool [%s, tags:%s] has no matching tags for volume [%s, uuid:%s, tags:%s]", destPool.getName(),
                    getStoragePoolTags(destPool), vol.getName(), vol.getUuid(), diskOffering.getTags()));
        }

        if (liveMigrateVolume && destPool.getClusterId() != null && srcClusterId != null) {
            if (!srcClusterId.equals(destPool.getClusterId())) {
                throw new InvalidParameterValueException("Cannot migrate a volume of a virtual machine to a storage pool in a different cluster");
            }
        }
        // In case of VMware, if ROOT volume is being cold-migrated, then ensure destination storage pool is in the same Datacenter as the VM.
        if (vm != null && vm.getHypervisorType().equals(HypervisorType.VMware)) {
            if (!liveMigrateVolume && vol.volumeType.equals(Volume.Type.ROOT)) {
                Long hostId = vm.getHostId() != null ? vm.getHostId() : vm.getLastHostId();
                HostVO host = _hostDao.findById(hostId);
                if (host != null) {
                    srcClusterId = host.getClusterId();
                }
                if (srcClusterId != null && destPool.getClusterId() != null && !srcClusterId.equals(destPool.getClusterId())) {
                    String srcDcName = _clusterDetailsDao.getVmwareDcName(srcClusterId);
                    String destDcName = _clusterDetailsDao.getVmwareDcName(destPool.getClusterId());
                    if (srcDcName != null && destDcName != null && !srcDcName.equals(destDcName)) {
                        throw new InvalidParameterValueException("Cannot migrate ROOT volume of a stopped VM to a storage pool in a different VMware datacenter");
                    }
                }
                updateMissingRootDiskController(vm, vol.getChainInfo());
            }
        }
        DiskOfferingVO newDiskOffering = retrieveAndValidateNewDiskOffering(cmd);
        validateConditionsToReplaceDiskOfferingOfVolume(vol, newDiskOffering, destPool);
        if (vm != null) {
            // serialize VM operation
            AsyncJobExecutionContext jobContext = AsyncJobExecutionContext.getCurrentExecutionContext();
            if (jobContext.isJobDispatchedBy(VmWorkConstants.VM_WORK_JOB_DISPATCHER)) {
                // avoid re-entrance

                VmWorkJobVO placeHolder = null;
                placeHolder = createPlaceHolderWork(vm.getId());
                try {
                    return orchestrateMigrateVolume(vol, destPool, liveMigrateVolume, newDiskOffering);
                } finally {
                    _workJobDao.expunge(placeHolder.getId());
                }

            } else {
                Outcome<Volume> outcome = migrateVolumeThroughJobQueue(vm, vol, destPool, liveMigrateVolume, newDiskOffering);

                try {
                    outcome.get();
                } catch (InterruptedException e) {
                    throw new RuntimeException("Operation is interrupted", e);
                } catch (java.util.concurrent.ExecutionException e) {
                    throw new RuntimeException("Execution excetion", e);
                }

                Object jobResult = _jobMgr.unmarshallResultObject(outcome.getJob());
                if (jobResult != null) {
                    if (jobResult instanceof ConcurrentOperationException) {
                        throw (ConcurrentOperationException)jobResult;
                    } else if (jobResult instanceof RuntimeException) {
                        throw (RuntimeException)jobResult;
                    } else if (jobResult instanceof Throwable) {
                        throw new RuntimeException("Unexpected exception", (Throwable)jobResult);
                    }
                }

                // retrieve the migrated new volume from job result
                if (jobResult != null && jobResult instanceof Long) {
                    return _entityMgr.findById(VolumeVO.class, ((Long)jobResult));
                }
                return null;
            }
        }

        return orchestrateMigrateVolume(vol, destPool, liveMigrateVolume, newDiskOffering);
    }

    /**
     * Retrieves the new disk offering UUID that might be sent to replace the current one in the volume being migrated.
     * If no disk offering UUID is provided we return null. Otherwise, we perform the following checks.
     * <ul>
     *  <li>Is the disk offering UUID entered valid? If not, an  {@link InvalidParameterValueException} is thrown;
     *  <li>If the disk offering was already removed, we thrown an {@link InvalidParameterValueException} is thrown;
     *  <li>We then check if the user executing the operation has access to the given disk offering.
     * </ul>
     *
     * If all checks pass, we move forward returning the disk offering object.
     */
    private DiskOfferingVO retrieveAndValidateNewDiskOffering(MigrateVolumeCmd cmd) {
        String newDiskOfferingUuid = cmd.getNewDiskOfferingUuid();
        if (org.apache.commons.lang.StringUtils.isBlank(newDiskOfferingUuid)) {
            return null;
        }
        DiskOfferingVO newDiskOffering = _diskOfferingDao.findByUuid(newDiskOfferingUuid);
        if (newDiskOffering == null) {
            throw new InvalidParameterValueException(String.format("The disk offering informed is not valid [id=%s].", newDiskOfferingUuid));
        }
        if (newDiskOffering.getRemoved() != null) {
            throw new InvalidParameterValueException(String.format("We cannot assign a removed disk offering [id=%s] to a volume. ", newDiskOffering.getUuid()));
        }
        Account caller = CallContext.current().getCallingAccount();
        DataCenter zone = null;
        Volume volume = _volsDao.findById(cmd.getId());
        if (volume != null) {
            zone = _dcDao.findById(volume.getDataCenterId());
        }
        _accountMgr.checkAccess(caller, newDiskOffering, zone);
        return newDiskOffering;
    }

    /**
     * Performs the validations required for replacing the disk offering while migrating the volume of storage. If no new disk offering is provided, we do not execute any validation.
     * If a disk offering is informed, we then proceed with the following checks.
     * <ul>
     *  <li>We check if the given volume is of ROOT type. We cannot change the disk offering of a ROOT volume. Therefore, we thrown an {@link InvalidParameterValueException};
     *  <li>We the disk is being migrated to shared storage and the new disk offering is for local storage (or vice versa), we throw an {@link InvalidParameterValueException}. Bear in mind that we are validating only the new disk offering. If none is provided we can override the current disk offering. This means, placing a volume with shared disk offering in local storage and vice versa;
     *  <li>We then proceed checking the target storage pool supports the new disk offering {@link #doesTargetStorageSupportNewDiskOffering(StoragePool, DiskOfferingVO)}.
     * </ul>
     *
     * If all of the above validations pass, we check if the size of the new disk offering is different from the volume. If it is, we log a warning message.
     */
    protected void validateConditionsToReplaceDiskOfferingOfVolume(VolumeVO volume, DiskOfferingVO newDiskOffering, StoragePool destPool) {
        if (newDiskOffering == null) {
            return;
        }
        if ((destPool.isShared() && newDiskOffering.isUseLocalStorage()) || destPool.isLocal() && newDiskOffering.isShared()) {
            throw new InvalidParameterValueException("You cannot move the volume to a shared storage and assing a disk offering for local storage and vice versa.");
        }
        if (!doesTargetStorageSupportDiskOffering(destPool, newDiskOffering)) {
            throw new InvalidParameterValueException(String.format("Target Storage [id=%s] tags [%s] does not match new disk offering [id=%s] tags [%s].", destPool.getUuid(),
                    getStoragePoolTags(destPool), newDiskOffering.getUuid(), newDiskOffering.getTags()));
        }
        if (volume.getSize() != newDiskOffering.getDiskSize()) {
            DiskOfferingVO oldDiskOffering = this._diskOfferingDao.findById(volume.getDiskOfferingId());
            s_logger.warn(String.format(
                    "You are migrating a volume [id=%s] and changing the disk offering[from id=%s to id=%s] to reflect this migration. However, the sizes of the volume and the new disk offering are different.",
                    volume.getUuid(), oldDiskOffering.getUuid(), newDiskOffering.getUuid()));
        }
        s_logger.info(String.format("Changing disk offering to [uuid=%s] while migrating volume [uuid=%s, name=%s].", newDiskOffering.getUuid(), volume.getUuid(), volume.getName()));
    }

    /**
     *  Checks if the target storage supports the new disk offering.
     *  This validation is consistent with the mechanism used to select a storage pool to deploy a volume when a virtual machine is deployed or when a new data disk is allocated.
     *
     *  The scenarios when this method returns true or false is presented in the following table.
     *
     *   <table border="1">
     *      <tr>
     *          <th>#</th><th>Disk offering tags</th><th>Storage tags</th><th>Does the storage support the disk offering?</th>
     *      </tr>
     *      <body>
     *      <tr>
     *          <td>1</td><td>A,B</td><td>A</td><td>NO</td>
     *      </tr>
     *      <tr>
     *          <td>2</td><td>A,B,C</td><td>A,B,C,D,X</td><td>YES</td>
     *      </tr>
     *      <tr>
     *          <td>3</td><td>A,B,C</td><td>X,Y,Z</td><td>NO</td>
     *      </tr>
     *      <tr>
     *          <td>4</td><td>null</td><td>A,S,D</td><td>YES</td>
     *      </tr>
     *      <tr>
     *          <td>5</td><td>A</td><td>null</td><td>NO</td>
     *      </tr>
     *      <tr>
     *          <td>6</td><td>null</td><td>null</td><td>YES</td>
     *      </tr>
     *      </body>
     *   </table>
     */
    protected boolean doesTargetStorageSupportDiskOffering(StoragePool destPool, DiskOfferingVO diskOffering) {
        String targetStoreTags = diskOffering.getTags();
        return doesTargetStorageSupportDiskOffering(destPool, targetStoreTags);
    }

    @Override
    public boolean doesTargetStorageSupportDiskOffering(StoragePool destPool, String diskOfferingTags) {
        if (org.apache.commons.lang.StringUtils.isBlank(diskOfferingTags)) {
            return true;
        }
        String storagePoolTags = getStoragePoolTags(destPool);
        if (org.apache.commons.lang.StringUtils.isBlank(storagePoolTags)) {
            return false;
        }
        String[] storageTagsAsStringArray = org.apache.commons.lang.StringUtils.split(storagePoolTags, ",");
        String[] newDiskOfferingTagsAsStringArray = org.apache.commons.lang.StringUtils.split(diskOfferingTags, ",");

        return CollectionUtils.isSubCollection(Arrays.asList(newDiskOfferingTagsAsStringArray), Arrays.asList(storageTagsAsStringArray));
    }

    /**
     *  Retrieves the storage pool tags as a {@link String}. If the storage pool does not have tags we return a null value.
     */
    protected String getStoragePoolTags(StoragePool destPool) {
        List<String> destPoolTags = storagePoolTagsDao.getStoragePoolTags(destPool.getId());
        if (CollectionUtils.isEmpty(destPoolTags)) {
            return null;
        }
        return StringUtils.join(destPoolTags, ",");
    }

    private Volume orchestrateMigrateVolume(VolumeVO volume, StoragePool destPool, boolean liveMigrateVolume, DiskOfferingVO newDiskOffering) {
        Volume newVol = null;
        try {
            if (liveMigrateVolume) {
                newVol = liveMigrateVolume(volume, destPool);
            } else {
                newVol = _volumeMgr.migrateVolume(volume, destPool);
            }
            if (newDiskOffering != null) {
                _volsDao.updateDiskOffering(newVol.getId(), newDiskOffering.getId());
            }
        } catch (StorageUnavailableException e) {
            s_logger.debug("Failed to migrate volume", e);
            throw new CloudRuntimeException(e.getMessage());
        } catch (Exception e) {
            s_logger.debug("Failed to migrate volume", e);
            throw new CloudRuntimeException(e.getMessage());
        }
        return newVol;
    }

    @DB
    protected Volume liveMigrateVolume(Volume volume, StoragePool destPool) throws StorageUnavailableException {
        VolumeInfo vol = volFactory.getVolume(volume.getId());

        DataStore dataStoreTarget = dataStoreMgr.getDataStore(destPool.getId(), DataStoreRole.Primary);
        AsyncCallFuture<VolumeApiResult> future = volService.migrateVolume(vol, dataStoreTarget);
        try {
            VolumeApiResult result = future.get();
            if (result.isFailed()) {
                s_logger.debug("migrate volume failed:" + result.getResult());
                throw new StorageUnavailableException("Migrate volume failed: " + result.getResult(), destPool.getId());
            }
            return result.getVolume();
        } catch (InterruptedException e) {
            s_logger.debug("migrate volume failed", e);
            throw new CloudRuntimeException(e.getMessage());
        } catch (ExecutionException e) {
            s_logger.debug("migrate volume failed", e);
            throw new CloudRuntimeException(e.getMessage());
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_SNAPSHOT_CREATE, eventDescription = "taking snapshot", async = true)
    public Snapshot takeSnapshot(Long volumeId, Long policyId, Long snapshotId, Account account, boolean quiescevm, Snapshot.LocationType locationType, boolean asyncBackup, Map<String, String> tags)
            throws ResourceAllocationException {
        final Snapshot snapshot = takeSnapshotInternal(volumeId, policyId, snapshotId, account, quiescevm, locationType, asyncBackup);
        if (snapshot != null && MapUtils.isNotEmpty(tags)) {
            taggedResourceService.createTags(Collections.singletonList(snapshot.getUuid()), ResourceTag.ResourceObjectType.Snapshot, tags, null);
        }
        return snapshot;
    }

    private Snapshot takeSnapshotInternal(Long volumeId, Long policyId, Long snapshotId, Account account, boolean quiescevm, Snapshot.LocationType locationType, boolean asyncBackup)
            throws ResourceAllocationException {
        VolumeInfo volume = volFactory.getVolume(volumeId);
        if (volume == null) {
            throw new InvalidParameterValueException("Creating snapshot failed due to volume:" + volumeId + " doesn't exist");
        }

        if (volume.getState() != Volume.State.Ready) {
            throw new InvalidParameterValueException("VolumeId: " + volumeId + " is not in " + Volume.State.Ready + " state but " + volume.getState() + ". Cannot take snapshot.");
        }

        StoragePoolVO storagePoolVO = _storagePoolDao.findById(volume.getPoolId());

        if (storagePoolVO.isManaged() && locationType == null) {
            locationType = Snapshot.LocationType.PRIMARY;
        }

        VMInstanceVO vm = null;
        if (volume.getInstanceId() != null) {
            vm = _vmInstanceDao.findById(volume.getInstanceId());
        }

        if (vm != null) {
            // serialize VM operation
            AsyncJobExecutionContext jobContext = AsyncJobExecutionContext.getCurrentExecutionContext();
            if (jobContext.isJobDispatchedBy(VmWorkConstants.VM_WORK_JOB_DISPATCHER)) {
                // avoid re-entrance

                VmWorkJobVO placeHolder = null;
                placeHolder = createPlaceHolderWork(vm.getId());
                try {
                    return orchestrateTakeVolumeSnapshot(volumeId, policyId, snapshotId, account, quiescevm, locationType, asyncBackup);
                } finally {
                    _workJobDao.expunge(placeHolder.getId());
                }

            } else {
                Outcome<Snapshot> outcome = takeVolumeSnapshotThroughJobQueue(vm.getId(), volumeId, policyId, snapshotId, account.getId(), quiescevm, locationType, asyncBackup);

                try {
                    outcome.get();
                } catch (InterruptedException e) {
                    throw new RuntimeException("Operation is interrupted", e);
                } catch (java.util.concurrent.ExecutionException e) {
                    throw new RuntimeException("Execution excetion", e);
                }

                Object jobResult = _jobMgr.unmarshallResultObject(outcome.getJob());
                if (jobResult != null) {
                    if (jobResult instanceof ConcurrentOperationException) {
                        throw (ConcurrentOperationException)jobResult;
                    } else if (jobResult instanceof ResourceAllocationException) {
                        throw (ResourceAllocationException)jobResult;
                    } else if (jobResult instanceof Throwable) {
                        throw new RuntimeException("Unexpected exception", (Throwable)jobResult);
                    }
                }

                return _snapshotDao.findById(snapshotId);
            }
        } else {
            CreateSnapshotPayload payload = new CreateSnapshotPayload();
            payload.setSnapshotId(snapshotId);
            payload.setSnapshotPolicyId(policyId);
            payload.setAccount(account);
            payload.setQuiescevm(quiescevm);
            payload.setAsyncBackup(asyncBackup);
            volume.addPayload(payload);
            return volService.takeSnapshot(volume);
        }
    }

    private Snapshot orchestrateTakeVolumeSnapshot(Long volumeId, Long policyId, Long snapshotId, Account account, boolean quiescevm, Snapshot.LocationType locationType, boolean asyncBackup)
            throws ResourceAllocationException {

        VolumeInfo volume = volFactory.getVolume(volumeId);

        if (volume == null) {
            throw new InvalidParameterValueException("Creating snapshot failed due to volume:" + volumeId + " doesn't exist");
        }

        if (volume.getState() != Volume.State.Ready) {
            throw new InvalidParameterValueException("VolumeId: " + volumeId + " is not in " + Volume.State.Ready + " state but " + volume.getState() + ". Cannot take snapshot.");
        }

        CreateSnapshotPayload payload = new CreateSnapshotPayload();

        payload.setSnapshotId(snapshotId);
        payload.setSnapshotPolicyId(policyId);
        payload.setAccount(account);
        payload.setQuiescevm(quiescevm);
        payload.setLocationType(locationType);
        payload.setAsyncBackup(asyncBackup);
        volume.addPayload(payload);

        return volService.takeSnapshot(volume);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_SNAPSHOT_CREATE, eventDescription = "allocating snapshot", create = true)
    public Snapshot allocSnapshot(Long volumeId, Long policyId, String snapshotName, Snapshot.LocationType locationType) throws ResourceAllocationException {
        Account caller = CallContext.current().getCallingAccount();

        VolumeInfo volume = volFactory.getVolume(volumeId);
        if (volume == null) {
            throw new InvalidParameterValueException("Creating snapshot failed due to volume:" + volumeId + " doesn't exist");
        }
        DataCenter zone = _dcDao.findById(volume.getDataCenterId());
        if (zone == null) {
            throw new InvalidParameterValueException("Can't find zone by id " + volume.getDataCenterId());
        }

        if (Grouping.AllocationState.Disabled == zone.getAllocationState() && !_accountMgr.isRootAdmin(caller.getId())) {
            throw new PermissionDeniedException("Cannot perform this operation, Zone is currently disabled: " + zone.getName());
        }

        if (volume.getState() != Volume.State.Ready) {
            throw new InvalidParameterValueException("VolumeId: " + volumeId + " is not in " + Volume.State.Ready + " state but " + volume.getState() + ". Cannot take snapshot.");
        }

        if (ImageFormat.DIR.equals(volume.getFormat())) {
            throw new InvalidParameterValueException("Snapshot not supported for volume:" + volumeId);
        }

        if (volume.getTemplateId() != null) {
            VMTemplateVO template = _templateDao.findById(volume.getTemplateId());
            if (template != null && template.getTemplateType() == Storage.TemplateType.SYSTEM) {
                throw new InvalidParameterValueException("VolumeId: " + volumeId + " is for System VM , Creating snapshot against System VM volumes is not supported");
            }
        }

        StoragePoolVO storagePoolVO = _storagePoolDao.findById(volume.getPoolId());

        if (!storagePoolVO.isManaged() && locationType != null) {
            throw new InvalidParameterValueException("VolumeId: " + volumeId + " LocationType is supported only for managed storage");
        }

        if (storagePoolVO.isManaged() && locationType == null) {
            locationType = Snapshot.LocationType.PRIMARY;
        }

        StoragePool storagePool = (StoragePool)volume.getDataStore();
        if (storagePool == null) {
            throw new InvalidParameterValueException("VolumeId: " + volumeId + " please attach this volume to a VM before create snapshot for it");
        }

        return snapshotMgr.allocSnapshot(volumeId, policyId, snapshotName, locationType);
    }

    @Override
    public Snapshot allocSnapshotForVm(Long vmId, Long volumeId, String snapshotName) throws ResourceAllocationException {
        Account caller = CallContext.current().getCallingAccount();
        VMInstanceVO vm = _vmInstanceDao.findById(vmId);
        if (vm == null) {
            throw new InvalidParameterValueException("Creating snapshot failed due to vm:" + vmId + " doesn't exist");
        }
        _accountMgr.checkAccess(caller, null, true, vm);

        VolumeInfo volume = volFactory.getVolume(volumeId);
        if (volume == null) {
            throw new InvalidParameterValueException("Creating snapshot failed due to volume:" + volumeId + " doesn't exist");
        }
        _accountMgr.checkAccess(caller, null, true, volume);
        VirtualMachine attachVM = volume.getAttachedVM();
        if (attachVM == null || attachVM.getId() != vm.getId()) {
            throw new InvalidParameterValueException("Creating snapshot failed due to volume:" + volumeId + " doesn't attach to vm :" + vm);
        }

        DataCenter zone = _dcDao.findById(volume.getDataCenterId());
        if (zone == null) {
            throw new InvalidParameterValueException("Can't find zone by id " + volume.getDataCenterId());
        }

        if (Grouping.AllocationState.Disabled == zone.getAllocationState() && !_accountMgr.isRootAdmin(caller.getId())) {
            throw new PermissionDeniedException("Cannot perform this operation, Zone is currently disabled: " + zone.getName());
        }

        if (volume.getState() != Volume.State.Ready) {
            throw new InvalidParameterValueException("VolumeId: " + volumeId + " is not in " + Volume.State.Ready + " state but " + volume.getState() + ". Cannot take snapshot.");
        }

        if (volume.getTemplateId() != null) {
            VMTemplateVO template = _templateDao.findById(volume.getTemplateId());
            if (template != null && template.getTemplateType() == Storage.TemplateType.SYSTEM) {
                throw new InvalidParameterValueException("VolumeId: " + volumeId + " is for System VM , Creating snapshot against System VM volumes is not supported");
            }
        }

        StoragePool storagePool = (StoragePool)volume.getDataStore();
        if (storagePool == null) {
            throw new InvalidParameterValueException("VolumeId: " + volumeId + " please attach this volume to a VM before create snapshot for it");
        }

        return snapshotMgr.allocSnapshot(volumeId, Snapshot.MANUAL_POLICY_ID, snapshotName, null);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VOLUME_EXTRACT, eventDescription = "extracting volume", async = true)
    public String extractVolume(ExtractVolumeCmd cmd) {
        Long volumeId = cmd.getId();
        Long zoneId = cmd.getZoneId();
        String mode = cmd.getMode();
        Account account = CallContext.current().getCallingAccount();

        if (!_accountMgr.isRootAdmin(account.getId()) && ApiDBUtils.isExtractionDisabled()) {
            throw new PermissionDeniedException("Extraction has been disabled by admin");
        }

        VolumeVO volume = _volsDao.findById(volumeId);
        if (volume == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find volume with specified volumeId");
            ex.addProxyObject(volumeId.toString(), "volumeId");
            throw ex;
        }

        // perform permission check
        _accountMgr.checkAccess(account, null, true, volume);

        if (_dcDao.findById(zoneId) == null) {
            throw new InvalidParameterValueException("Please specify a valid zone.");
        }
        if (volume.getPoolId() == null) {
            throw new InvalidParameterValueException("The volume doesn't belong to a storage pool so can't extract it");
        }
        // Extract activity only for detached volumes or for volumes whose
        // instance is stopped
        if (volume.getInstanceId() != null && ApiDBUtils.findVMInstanceById(volume.getInstanceId()).getState() != State.Stopped) {
            s_logger.debug("Invalid state of the volume with ID: " + volumeId + ". It should be either detached or the VM should be in stopped state.");
            PermissionDeniedException ex = new PermissionDeniedException("Invalid state of the volume with specified ID. It should be either detached or the VM should be in stopped state.");
            ex.addProxyObject(volume.getUuid(), "volumeId");
            throw ex;
        }

        if (volume.getVolumeType() != Volume.Type.DATADISK) {
            // Datadisk dont have any template dependence.

            VMTemplateVO template = ApiDBUtils.findTemplateById(volume.getTemplateId());
            if (template != null) { // For ISO based volumes template = null and
                // we allow extraction of all ISO based
                // volumes
                boolean isExtractable = template.isExtractable() && template.getTemplateType() != Storage.TemplateType.SYSTEM;
                if (!isExtractable && account != null && !_accountMgr.isRootAdmin(account.getId())) {
                    // Global admins are always allowed to extract
                    PermissionDeniedException ex = new PermissionDeniedException("The volume with specified volumeId is not allowed to be extracted");
                    ex.addProxyObject(volume.getUuid(), "volumeId");
                    throw ex;
                }
            }
        }

        if (mode == null || (!mode.equals(Upload.Mode.FTP_UPLOAD.toString()) && !mode.equals(Upload.Mode.HTTP_DOWNLOAD.toString()))) {
            throw new InvalidParameterValueException("Please specify a valid extract Mode ");
        }

        // Check if the url already exists
        VolumeDataStoreVO volumeStoreRef = _volumeStoreDao.findByVolume(volumeId);
        if (volumeStoreRef != null && volumeStoreRef.getExtractUrl() != null) {
            return volumeStoreRef.getExtractUrl();
        }

        VMInstanceVO vm = null;
        if (volume.getInstanceId() != null) {
            vm = _vmInstanceDao.findById(volume.getInstanceId());
        }

        if (vm != null) {
            // serialize VM operation
            AsyncJobExecutionContext jobContext = AsyncJobExecutionContext.getCurrentExecutionContext();
            if (jobContext.isJobDispatchedBy(VmWorkConstants.VM_WORK_JOB_DISPATCHER)) {
                // avoid re-entrance

                VmWorkJobVO placeHolder = null;
                placeHolder = createPlaceHolderWork(vm.getId());
                try {
                    return orchestrateExtractVolume(volume.getId(), zoneId);
                } finally {
                    _workJobDao.expunge(placeHolder.getId());
                }

            } else {
                Outcome<String> outcome = extractVolumeThroughJobQueue(vm.getId(), volume.getId(), zoneId);

                try {
                    outcome.get();
                } catch (InterruptedException e) {
                    throw new RuntimeException("Operation is interrupted", e);
                } catch (java.util.concurrent.ExecutionException e) {
                    throw new RuntimeException("Execution excetion", e);
                }

                Object jobResult = _jobMgr.unmarshallResultObject(outcome.getJob());
                if (jobResult != null) {
                    if (jobResult instanceof ConcurrentOperationException) {
                        throw (ConcurrentOperationException)jobResult;
                    } else if (jobResult instanceof RuntimeException) {
                        throw (RuntimeException)jobResult;
                    } else if (jobResult instanceof Throwable) {
                        throw new RuntimeException("Unexpected exception", (Throwable)jobResult);
                    }
                }

                // retrieve the entity url from job result
                if (jobResult != null && jobResult instanceof String) {
                    return (String)jobResult;
                }
                return null;
            }
        }

        return orchestrateExtractVolume(volume.getId(), zoneId);
    }

    private String orchestrateExtractVolume(long volumeId, long zoneId) {
        // get latest volume state to make sure that it is not updated by other parallel operations
        VolumeVO volume = _volsDao.findById(volumeId);
        if (volume == null || volume.getState() != Volume.State.Ready) {
            throw new InvalidParameterValueException("Volume to be extracted has been removed or not in right state!");
        }
        // perform extraction
        ImageStoreEntity secStore = (ImageStoreEntity)dataStoreMgr.getImageStoreWithFreeCapacity(zoneId);
        if (secStore == null) {
            throw new InvalidParameterValueException(String.format("Secondary storage to satisfy storage needs cannot be found for zone: %d", zoneId));
        }
        String value = _configDao.getValue(Config.CopyVolumeWait.toString());
        NumbersUtil.parseInt(value, Integer.parseInt(Config.CopyVolumeWait.getDefaultValue()));

        // Copy volume from primary to secondary storage
        VolumeInfo srcVol = volFactory.getVolume(volumeId);
        AsyncCallFuture<VolumeApiResult> cvAnswer = volService.copyVolume(srcVol, secStore);
        // Check if you got a valid answer.
        VolumeApiResult cvResult = null;
        try {
            cvResult = cvAnswer.get();
        } catch (InterruptedException e1) {
            s_logger.debug("failed copy volume", e1);
            throw new CloudRuntimeException("Failed to copy volume", e1);
        } catch (ExecutionException e1) {
            s_logger.debug("failed copy volume", e1);
            throw new CloudRuntimeException("Failed to copy volume", e1);
        }
        if (cvResult == null || cvResult.isFailed()) {
            String errorString = "Failed to copy the volume from the source primary storage pool to secondary storage.";
            throw new CloudRuntimeException(errorString);
        }

        VolumeInfo vol = cvResult.getVolume();

        String extractUrl = secStore.createEntityExtractUrl(vol.getPath(), vol.getFormat(), vol);
        VolumeDataStoreVO volumeStoreRef = _volumeStoreDao.findByVolume(volumeId);

        volumeStoreRef.setExtractUrl(extractUrl);
        volumeStoreRef.setExtractUrlCreated(DateUtil.now());
        volumeStoreRef.setDownloadState(VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
        volumeStoreRef.setDownloadPercent(100);
        volumeStoreRef.setZoneId(zoneId);

        _volumeStoreDao.update(volumeStoreRef.getId(), volumeStoreRef);

        return extractUrl;
    }

    @Override
    public boolean isDisplayResourceEnabled(Long id) {
        Volume volume = _volsDao.findById(id);
        if (volume == null) {
            return true; // bad id given, default to true
        }
        return volume.isDisplayVolume();
    }

    private boolean needMoveVolume(VolumeVO existingVolume, VolumeInfo newVolume) {
        if (existingVolume == null || existingVolume.getPoolId() == null || newVolume.getPoolId() == null) {
            return false;
        }

        DataStore storeForExistingVol = dataStoreMgr.getPrimaryDataStore(existingVolume.getPoolId());
        DataStore storeForNewVol = dataStoreMgr.getPrimaryDataStore(newVolume.getPoolId());

        Scope storeForExistingStoreScope = storeForExistingVol.getScope();
        if (storeForExistingStoreScope == null) {
            throw new CloudRuntimeException("Can't get scope of data store: " + storeForExistingVol.getId());
        }

        Scope storeForNewStoreScope = storeForNewVol.getScope();
        if (storeForNewStoreScope == null) {
            throw new CloudRuntimeException("Can't get scope of data store: " + storeForNewVol.getId());
        }

        if (storeForNewStoreScope.getScopeType() == ScopeType.ZONE) {
            return false;
        }

        if (storeForExistingStoreScope.getScopeType() != storeForNewStoreScope.getScopeType()) {
            if (storeForNewStoreScope.getScopeType() == ScopeType.CLUSTER) {
                Long vmClusterId = null;
                if (storeForExistingStoreScope.getScopeType() == ScopeType.HOST) {
                    HostScope hs = (HostScope)storeForExistingStoreScope;
                    vmClusterId = hs.getClusterId();
                } else if (storeForExistingStoreScope.getScopeType() == ScopeType.ZONE) {
                    Long hostId = _vmInstanceDao.findById(existingVolume.getInstanceId()).getHostId();
                    if (hostId != null) {
                        HostVO host = _hostDao.findById(hostId);
                        vmClusterId = host.getClusterId();
                    }
                }
                if (storeForNewStoreScope.getScopeId().equals(vmClusterId)) {
                    return false;
                } else {
                    return true;
                }
            } else if (storeForNewStoreScope.getScopeType() == ScopeType.HOST
                    && (storeForExistingStoreScope.getScopeType() == ScopeType.CLUSTER || storeForExistingStoreScope.getScopeType() == ScopeType.ZONE)) {
                Long hostId = _vmInstanceDao.findById(existingVolume.getInstanceId()).getHostId();
                if (storeForNewStoreScope.getScopeId().equals(hostId)) {
                    return false;
                }
            }
            throw new InvalidParameterValueException("Can't move volume between scope: " + storeForNewStoreScope.getScopeType() + " and " + storeForExistingStoreScope.getScopeType());
        }

        return !storeForExistingStoreScope.isSameScope(storeForNewStoreScope);
    }

    private synchronized void checkAndSetAttaching(Long volumeId) {
        VolumeInfo volumeToAttach = volFactory.getVolume(volumeId);

        if (volumeToAttach.isAttachedVM()) {
            throw new CloudRuntimeException("volume: " + volumeToAttach.getName() + " is already attached to a VM: " + volumeToAttach.getAttachedVmName());
        }

        if (Volume.State.Allocated.equals(volumeToAttach.getState())) {
            return;
        }

        if (Volume.State.Ready.equals(volumeToAttach.getState())) {
            volumeToAttach.stateTransit(Volume.Event.AttachRequested);
            return;
        }

        final String error = "Volume: " + volumeToAttach.getName() + " is in " + volumeToAttach.getState() + ". It should be in Ready or Allocated state";
        s_logger.error(error);
        throw new CloudRuntimeException(error);
    }

    private void verifyManagedStorage(Long storagePoolId, Long hostId) {
        if (storagePoolId == null || hostId == null) {
            return;
        }

        StoragePoolVO storagePoolVO = _storagePoolDao.findById(storagePoolId);

        if (storagePoolVO == null || !storagePoolVO.isManaged()) {
            return;
        }

        HostVO hostVO = _hostDao.findById(hostId);

        if (hostVO == null) {
            return;
        }

        if (!storageUtil.managedStoragePoolCanScale(storagePoolVO, hostVO.getClusterId(), hostVO.getId())) {
            throw new CloudRuntimeException("Insufficient number of available " + getNameOfClusteredFileSystem(hostVO));
        }
    }

    private String getNameOfClusteredFileSystem(HostVO hostVO) {
        HypervisorType hypervisorType = hostVO.getHypervisorType();

        if (HypervisorType.XenServer.equals(hypervisorType)) {
            return "SRs";
        }

        if (HypervisorType.VMware.equals(hypervisorType)) {
            return "datastores";
        }

        return "clustered file systems";
    }

    private VolumeVO sendAttachVolumeCommand(UserVmVO vm, VolumeVO volumeToAttach, Long deviceId) {
        String errorMsg = "Failed to attach volume " + volumeToAttach.getName() + " to VM " + vm.getHostName();
        boolean sendCommand = vm.getState() == State.Running;
        AttachAnswer answer = null;
        Long hostId = vm.getHostId();

        if (hostId == null) {
            hostId = vm.getLastHostId();

            HostVO host = _hostDao.findById(hostId);

            if (host != null && host.getHypervisorType() == HypervisorType.VMware) {
                sendCommand = true;
            }
        }

        HostVO host = null;
        StoragePoolVO volumeToAttachStoragePool = _storagePoolDao.findById(volumeToAttach.getPoolId());

        if (hostId != null) {
            host = _hostDao.findById(hostId);

            if (host != null && host.getHypervisorType() == HypervisorType.XenServer && volumeToAttachStoragePool != null && volumeToAttachStoragePool.isManaged()) {
                sendCommand = true;
            }
        }

        if (volumeToAttachStoragePool != null) {
            verifyManagedStorage(volumeToAttachStoragePool.getId(), hostId);
        }

        // volumeToAttachStoragePool should be null if the VM we are attaching the disk to has never been started before
        DataStore dataStore = volumeToAttachStoragePool != null ? dataStoreMgr.getDataStore(volumeToAttachStoragePool.getId(), DataStoreRole.Primary) : null;

        checkAndSetAttaching(volumeToAttach.getId());

        boolean attached = false;
        try {
            // if we don't have a host, the VM we are attaching the disk to has never been started before
            if (host != null) {
                try {
                    volService.grantAccess(volFactory.getVolume(volumeToAttach.getId()), host, dataStore);
                } catch (Exception e) {
                    volService.revokeAccess(volFactory.getVolume(volumeToAttach.getId()), host, dataStore);

                    throw new CloudRuntimeException(e.getMessage());
                }
            }

            if (sendCommand) {
                if (host != null && host.getHypervisorType() == HypervisorType.KVM && volumeToAttachStoragePool.isManaged() && volumeToAttach.getPath() == null) {
                    volumeToAttach.setPath(volumeToAttach.get_iScsiName());

                    _volsDao.update(volumeToAttach.getId(), volumeToAttach);
                }

                DataTO volTO = volFactory.getVolume(volumeToAttach.getId()).getTO();

                deviceId = getDeviceId(vm, deviceId);

                DiskTO disk = storageMgr.getDiskWithThrottling(volTO, volumeToAttach.getVolumeType(), deviceId, volumeToAttach.getPath(), vm.getServiceOfferingId(),
                        volumeToAttach.getDiskOfferingId());

                AttachCommand cmd = new AttachCommand(disk, vm.getInstanceName());

                ChapInfo chapInfo = volService.getChapInfo(volFactory.getVolume(volumeToAttach.getId()), dataStore);

                Map<String, String> details = new HashMap<String, String>();

                disk.setDetails(details);

                details.put(DiskTO.MANAGED, String.valueOf(volumeToAttachStoragePool.isManaged()));
                details.put(DiskTO.STORAGE_HOST, volumeToAttachStoragePool.getHostAddress());
                details.put(DiskTO.STORAGE_PORT, String.valueOf(volumeToAttachStoragePool.getPort()));
                details.put(DiskTO.VOLUME_SIZE, String.valueOf(volumeToAttach.getSize()));
                details.put(DiskTO.IQN, volumeToAttach.get_iScsiName());
                details.put(DiskTO.MOUNT_POINT, volumeToAttach.get_iScsiName());
                details.put(DiskTO.PROTOCOL_TYPE, (volumeToAttach.getPoolType() != null) ? volumeToAttach.getPoolType().toString() : null);

                if (chapInfo != null) {
                    details.put(DiskTO.CHAP_INITIATOR_USERNAME, chapInfo.getInitiatorUsername());
                    details.put(DiskTO.CHAP_INITIATOR_SECRET, chapInfo.getInitiatorSecret());
                    details.put(DiskTO.CHAP_TARGET_USERNAME, chapInfo.getTargetUsername());
                    details.put(DiskTO.CHAP_TARGET_SECRET, chapInfo.getTargetSecret());
                }
                _userVmDao.loadDetails(vm);
                Map<String, String> controllerInfo = new HashMap<String, String>();
                controllerInfo.put(VmDetailConstants.ROOT_DISK_CONTROLLER, vm.getDetail(VmDetailConstants.ROOT_DISK_CONTROLLER));
                controllerInfo.put(VmDetailConstants.DATA_DISK_CONTROLLER, vm.getDetail(VmDetailConstants.DATA_DISK_CONTROLLER));
                cmd.setControllerInfo(controllerInfo);
                s_logger.debug("Attach volume id:" + volumeToAttach.getId() + " on VM id:" + vm.getId() + " has controller info:" + controllerInfo);

                try {
                    answer = (AttachAnswer)_agentMgr.send(hostId, cmd);
                } catch (Exception e) {
                    if (host != null) {
                        volService.revokeAccess(volFactory.getVolume(volumeToAttach.getId()), host, dataStore);
                    }
                    throw new CloudRuntimeException(errorMsg + " due to: " + e.getMessage());
                }
            }

            if (!sendCommand || (answer != null && answer.getResult())) {
                // Mark the volume as attached
                if (sendCommand) {
                    DiskTO disk = answer.getDisk();

                    _volsDao.attachVolume(volumeToAttach.getId(), vm.getId(), disk.getDiskSeq());

                    volumeToAttach = _volsDao.findById(volumeToAttach.getId());

                    if (volumeToAttachStoragePool.isManaged() && volumeToAttach.getPath() == null) {
                        volumeToAttach.setPath(answer.getDisk().getPath());

                        _volsDao.update(volumeToAttach.getId(), volumeToAttach);
                    }
                } else {
                    deviceId = getDeviceId(vm, deviceId);

                    _volsDao.attachVolume(volumeToAttach.getId(), vm.getId(), deviceId);

                    volumeToAttach = _volsDao.findById(volumeToAttach.getId());

                    if (vm.getHypervisorType() == HypervisorType.KVM &&
                            volumeToAttachStoragePool != null && volumeToAttachStoragePool.isManaged() &&
                            volumeToAttach.getPath() == null && volumeToAttach.get_iScsiName() != null) {
                        volumeToAttach.setPath(volumeToAttach.get_iScsiName());
                        _volsDao.update(volumeToAttach.getId(), volumeToAttach);
                    }
                }

                // insert record for disk I/O statistics
                VmDiskStatisticsVO diskstats = _vmDiskStatsDao.findBy(vm.getAccountId(), vm.getDataCenterId(), vm.getId(), volumeToAttach.getId());
                if (diskstats == null) {
                    diskstats = new VmDiskStatisticsVO(vm.getAccountId(), vm.getDataCenterId(), vm.getId(), volumeToAttach.getId());
                    _vmDiskStatsDao.persist(diskstats);
                }

                attached = true;
            } else {
                if (answer != null) {
                    String details = answer.getDetails();
                    if (details != null && !details.isEmpty()) {
                        errorMsg += "; " + details;
                    }
                }
                if (host != null) {
                    volService.revokeAccess(volFactory.getVolume(volumeToAttach.getId()), host, dataStore);
                }
                throw new CloudRuntimeException(errorMsg);
            }
        } finally {
            Volume.Event ev = Volume.Event.OperationFailed;
            VolumeInfo volInfo = volFactory.getVolume(volumeToAttach.getId());
            if (attached) {
                ev = Volume.Event.OperationSucceeded;
                s_logger.debug("Volume: " + volInfo.getName() + " successfully attached to VM: " + volInfo.getAttachedVmName());
            } else {
                s_logger.debug("Volume: " + volInfo.getName() + " failed to attach to VM: " + volInfo.getAttachedVmName());
            }
            volInfo.stateTransit(ev);
        }
        return _volsDao.findById(volumeToAttach.getId());
    }

    private int getMaxDataVolumesSupported(UserVmVO vm) {
        Long hostId = vm.getHostId();
        if (hostId == null) {
            hostId = vm.getLastHostId();
        }
        HostVO host = _hostDao.findById(hostId);
        Integer maxDataVolumesSupported = null;
        if (host != null) {
            _hostDao.loadDetails(host);
            maxDataVolumesSupported = _hypervisorCapabilitiesDao.getMaxDataVolumesLimit(host.getHypervisorType(), host.getDetail("product_version"));
        } else {
            HypervisorType hypervisorType = vm.getHypervisorType();
            if (hypervisorType != null && CollectionUtils.isNotEmpty(supportingDefaultHV) && supportingDefaultHV.contains(hypervisorType)) {
                maxDataVolumesSupported = _hypervisorCapabilitiesDao.getMaxDataVolumesLimit(hypervisorType, "default");
            }
        }
        if (maxDataVolumesSupported == null || maxDataVolumesSupported.intValue() <= 0) {
            maxDataVolumesSupported = 6; // 6 data disks by default if nothing
            // is specified in
            // 'hypervisor_capabilities' table
        }

        return maxDataVolumesSupported.intValue();
    }

    private Long getDeviceId(UserVmVO vm, Long deviceId) {
        // allocate deviceId
        int maxDevices = getMaxDataVolumesSupported(vm) + 2; // add 2 to consider devices root volume and cdrom
        int maxDeviceId = maxDevices - 1;
        List<VolumeVO> vols = _volsDao.findByInstance(vm.getId());
        if (deviceId != null) {
            if (deviceId.longValue() < 0 || deviceId.longValue() > maxDeviceId || deviceId.longValue() == 3) {
                throw new RuntimeException("deviceId should be 0,1,2,4-" + maxDeviceId);
            }
            for (VolumeVO vol : vols) {
                if (vol.getDeviceId().equals(deviceId)) {
                    throw new RuntimeException("deviceId " + deviceId + " is used by vm " + vm.getId());
                }
            }
        } else {
            // allocate deviceId here
            List<String> devIds = new ArrayList<String>();
            for (int i = 1; i <= maxDeviceId; i++) {
                devIds.add(String.valueOf(i));
            }
            devIds.remove("3");
            for (VolumeVO vol : vols) {
                devIds.remove(vol.getDeviceId().toString().trim());
            }
            if (devIds.isEmpty()) {
                throw new RuntimeException("All device Ids are used by vm " + vm.getId());
            }
            deviceId = Long.parseLong(devIds.iterator().next());
        }

        return deviceId;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) {
        String maxVolumeSizeInGbString = _configDao.getValue(Config.MaxVolumeSize.toString());
        _maxVolumeSizeInGb = NumbersUtil.parseLong(maxVolumeSizeInGbString, 2000);
        supportingDefaultHV = _hypervisorCapabilitiesDao.getHypervisorsWithDefaultEntries();
        return true;
    }

    public List<StoragePoolAllocator> getStoragePoolAllocators() {
        return _storagePoolAllocators;
    }

    @Inject
    public void setStoragePoolAllocators(List<StoragePoolAllocator> storagePoolAllocators) {
        _storagePoolAllocators = storagePoolAllocators;
    }

    public class VmJobVolumeUrlOutcome extends OutcomeImpl<String> {
        public VmJobVolumeUrlOutcome(final AsyncJob job) {
            super(String.class, job, VmJobCheckInterval.value(), new Predicate() {
                @Override
                public boolean checkCondition() {
                    AsyncJobVO jobVo = _entityMgr.findById(AsyncJobVO.class, job.getId());
                    assert (jobVo != null);
                    if (jobVo == null || jobVo.getStatus() != JobInfo.Status.IN_PROGRESS) {
                        return true;
                    }

                    return false;
                }
            }, AsyncJob.Topics.JOB_STATE);
        }
    }

    public class VmJobVolumeOutcome extends OutcomeImpl<Volume> {
        private long _volumeId;

        public VmJobVolumeOutcome(final AsyncJob job, final long volumeId) {
            super(Volume.class, job, VmJobCheckInterval.value(), new Predicate() {
                @Override
                public boolean checkCondition() {
                    AsyncJobVO jobVo = _entityMgr.findById(AsyncJobVO.class, job.getId());
                    assert (jobVo != null);
                    if (jobVo == null || jobVo.getStatus() != JobInfo.Status.IN_PROGRESS) {
                        return true;
                    }

                    return false;
                }
            }, AsyncJob.Topics.JOB_STATE);
            _volumeId = volumeId;
        }

        @Override
        protected Volume retrieve() {
            return _volsDao.findById(_volumeId);
        }
    }

    public class VmJobSnapshotOutcome extends OutcomeImpl<Snapshot> {
        private long _snapshotId;

        public VmJobSnapshotOutcome(final AsyncJob job, final long snapshotId) {
            super(Snapshot.class, job, VmJobCheckInterval.value(), new Predicate() {
                @Override
                public boolean checkCondition() {
                    AsyncJobVO jobVo = _entityMgr.findById(AsyncJobVO.class, job.getId());
                    assert (jobVo != null);
                    if (jobVo == null || jobVo.getStatus() != JobInfo.Status.IN_PROGRESS) {
                        return true;
                    }

                    return false;
                }
            }, AsyncJob.Topics.JOB_STATE);
            _snapshotId = snapshotId;
        }

        @Override
        protected Snapshot retrieve() {
            return _snapshotDao.findById(_snapshotId);
        }
    }

    public Outcome<Volume> attachVolumeToVmThroughJobQueue(final Long vmId, final Long volumeId, final Long deviceId) {

        final CallContext context = CallContext.current();
        final User callingUser = context.getCallingUser();
        final Account callingAccount = context.getCallingAccount();

        final VMInstanceVO vm = _vmInstanceDao.findById(vmId);

        VmWorkJobVO workJob = new VmWorkJobVO(context.getContextId());

        workJob.setDispatcher(VmWorkConstants.VM_WORK_JOB_DISPATCHER);
        workJob.setCmd(VmWorkAttachVolume.class.getName());

        workJob.setAccountId(callingAccount.getId());
        workJob.setUserId(callingUser.getId());
        workJob.setStep(VmWorkJobVO.Step.Starting);
        workJob.setVmType(VirtualMachine.Type.Instance);
        workJob.setVmInstanceId(vm.getId());
        workJob.setRelated(AsyncJobExecutionContext.getOriginJobId());

        // save work context info (there are some duplications)
        VmWorkAttachVolume workInfo = new VmWorkAttachVolume(callingUser.getId(), callingAccount.getId(), vm.getId(), VolumeApiServiceImpl.VM_WORK_JOB_HANDLER, volumeId, deviceId);
        workJob.setCmdInfo(VmWorkSerializer.serialize(workInfo));

        _jobMgr.submitAsyncJob(workJob, VmWorkConstants.VM_WORK_QUEUE, vm.getId());

        AsyncJobVO jobVo = _jobMgr.getAsyncJob(workJob.getId());
        s_logger.debug("New job " + workJob.getId() + ", result field: " + jobVo.getResult());

        AsyncJobExecutionContext.getCurrentExecutionContext().joinJob(workJob.getId());

        return new VmJobVolumeOutcome(workJob, volumeId);
    }

    public Outcome<Volume> detachVolumeFromVmThroughJobQueue(final Long vmId, final Long volumeId) {

        final CallContext context = CallContext.current();
        final User callingUser = context.getCallingUser();
        final Account callingAccount = context.getCallingAccount();

        final VMInstanceVO vm = _vmInstanceDao.findById(vmId);

        VmWorkJobVO workJob = new VmWorkJobVO(context.getContextId());

        workJob.setDispatcher(VmWorkConstants.VM_WORK_JOB_DISPATCHER);
        workJob.setCmd(VmWorkDetachVolume.class.getName());

        workJob.setAccountId(callingAccount.getId());
        workJob.setUserId(callingUser.getId());
        workJob.setStep(VmWorkJobVO.Step.Starting);
        workJob.setVmType(VirtualMachine.Type.Instance);
        workJob.setVmInstanceId(vm.getId());
        workJob.setRelated(AsyncJobExecutionContext.getOriginJobId());

        // save work context info (there are some duplications)
        VmWorkDetachVolume workInfo = new VmWorkDetachVolume(callingUser.getId(), callingAccount.getId(), vm.getId(), VolumeApiServiceImpl.VM_WORK_JOB_HANDLER, volumeId);
        workJob.setCmdInfo(VmWorkSerializer.serialize(workInfo));

        _jobMgr.submitAsyncJob(workJob, VmWorkConstants.VM_WORK_QUEUE, vm.getId());

        AsyncJobExecutionContext.getCurrentExecutionContext().joinJob(workJob.getId());

        return new VmJobVolumeOutcome(workJob, volumeId);
    }

    public Outcome<Volume> resizeVolumeThroughJobQueue(final Long vmId, final long volumeId, final long currentSize, final long newSize, final Long newMinIops, final Long newMaxIops,
            final Integer newHypervisorSnapshotReserve, final Long newServiceOfferingId, final boolean shrinkOk) {
        final CallContext context = CallContext.current();
        final User callingUser = context.getCallingUser();
        final Account callingAccount = context.getCallingAccount();

        final VMInstanceVO vm = _vmInstanceDao.findById(vmId);

        VmWorkJobVO workJob = new VmWorkJobVO(context.getContextId());

        workJob.setDispatcher(VmWorkConstants.VM_WORK_JOB_DISPATCHER);
        workJob.setCmd(VmWorkResizeVolume.class.getName());

        workJob.setAccountId(callingAccount.getId());
        workJob.setUserId(callingUser.getId());
        workJob.setStep(VmWorkJobVO.Step.Starting);
        workJob.setVmType(VirtualMachine.Type.Instance);
        workJob.setVmInstanceId(vm.getId());
        workJob.setRelated(AsyncJobExecutionContext.getOriginJobId());

        // save work context info (there are some duplications)
        VmWorkResizeVolume workInfo = new VmWorkResizeVolume(callingUser.getId(), callingAccount.getId(), vm.getId(), VolumeApiServiceImpl.VM_WORK_JOB_HANDLER, volumeId, currentSize, newSize,
                newMinIops, newMaxIops, newHypervisorSnapshotReserve, newServiceOfferingId, shrinkOk);
        workJob.setCmdInfo(VmWorkSerializer.serialize(workInfo));

        _jobMgr.submitAsyncJob(workJob, VmWorkConstants.VM_WORK_QUEUE, vm.getId());

        AsyncJobExecutionContext.getCurrentExecutionContext().joinJob(workJob.getId());

        return new VmJobVolumeOutcome(workJob, volumeId);
    }

    public Outcome<String> extractVolumeThroughJobQueue(final Long vmId, final long volumeId, final long zoneId) {

        final CallContext context = CallContext.current();
        final User callingUser = context.getCallingUser();
        final Account callingAccount = context.getCallingAccount();

        final VMInstanceVO vm = _vmInstanceDao.findById(vmId);

        VmWorkJobVO workJob = new VmWorkJobVO(context.getContextId());

        workJob.setDispatcher(VmWorkConstants.VM_WORK_JOB_DISPATCHER);
        workJob.setCmd(VmWorkExtractVolume.class.getName());

        workJob.setAccountId(callingAccount.getId());
        workJob.setUserId(callingUser.getId());
        workJob.setStep(VmWorkJobVO.Step.Starting);
        workJob.setVmType(VirtualMachine.Type.Instance);
        workJob.setVmInstanceId(vm.getId());
        workJob.setRelated(AsyncJobExecutionContext.getOriginJobId());

        // save work context info (there are some duplications)
        VmWorkExtractVolume workInfo = new VmWorkExtractVolume(callingUser.getId(), callingAccount.getId(), vm.getId(), VolumeApiServiceImpl.VM_WORK_JOB_HANDLER, volumeId, zoneId);
        workJob.setCmdInfo(VmWorkSerializer.serialize(workInfo));

        _jobMgr.submitAsyncJob(workJob, VmWorkConstants.VM_WORK_QUEUE, vm.getId());

        AsyncJobExecutionContext.getCurrentExecutionContext().joinJob(workJob.getId());

        return new VmJobVolumeUrlOutcome(workJob);
    }

    private Outcome<Volume> migrateVolumeThroughJobQueue(VMInstanceVO vm, VolumeVO vol, StoragePool destPool, boolean liveMigrateVolume, DiskOfferingVO newDiskOffering) {
        CallContext context = CallContext.current();
        User callingUser = context.getCallingUser();
        Account callingAccount = context.getCallingAccount();

        VmWorkJobVO workJob = new VmWorkJobVO(context.getContextId());

        workJob.setDispatcher(VmWorkConstants.VM_WORK_JOB_DISPATCHER);
        workJob.setCmd(VmWorkMigrateVolume.class.getName());

        workJob.setAccountId(callingAccount.getId());
        workJob.setUserId(callingUser.getId());
        workJob.setStep(VmWorkJobVO.Step.Starting);
        workJob.setVmType(VirtualMachine.Type.Instance);
        workJob.setVmInstanceId(vm.getId());
        workJob.setRelated(AsyncJobExecutionContext.getOriginJobId());

        Long newDiskOfferingId = newDiskOffering != null ? newDiskOffering.getId() : null;

        // save work context info (there are some duplications)
        VmWorkMigrateVolume workInfo = new VmWorkMigrateVolume(callingUser.getId(), callingAccount.getId(), vm.getId(), VolumeApiServiceImpl.VM_WORK_JOB_HANDLER, vol.getId(), destPool.getId(),
                liveMigrateVolume, newDiskOfferingId);
        workJob.setCmdInfo(VmWorkSerializer.serialize(workInfo));

        _jobMgr.submitAsyncJob(workJob, VmWorkConstants.VM_WORK_QUEUE, vm.getId());

        AsyncJobExecutionContext.getCurrentExecutionContext().joinJob(workJob.getId());

        return new VmJobVolumeOutcome(workJob, vol.getId());
    }

    public Outcome<Snapshot> takeVolumeSnapshotThroughJobQueue(final Long vmId, final Long volumeId, final Long policyId, final Long snapshotId, final Long accountId, final boolean quiesceVm,
            final Snapshot.LocationType locationType, final boolean asyncBackup) {

        final CallContext context = CallContext.current();
        final User callingUser = context.getCallingUser();
        final Account callingAccount = context.getCallingAccount();

        final VMInstanceVO vm = _vmInstanceDao.findById(vmId);

        VmWorkJobVO workJob = new VmWorkJobVO(context.getContextId());

        workJob.setDispatcher(VmWorkConstants.VM_WORK_JOB_DISPATCHER);
        workJob.setCmd(VmWorkTakeVolumeSnapshot.class.getName());

        workJob.setAccountId(callingAccount.getId());
        workJob.setUserId(callingUser.getId());
        workJob.setStep(VmWorkJobVO.Step.Starting);
        workJob.setVmType(VirtualMachine.Type.Instance);
        workJob.setVmInstanceId(vm.getId());
        workJob.setRelated(AsyncJobExecutionContext.getOriginJobId());

        // save work context info (there are some duplications)
        VmWorkTakeVolumeSnapshot workInfo = new VmWorkTakeVolumeSnapshot(callingUser.getId(), accountId != null ? accountId : callingAccount.getId(), vm.getId(),
                VolumeApiServiceImpl.VM_WORK_JOB_HANDLER, volumeId, policyId, snapshotId, quiesceVm, locationType, asyncBackup);
        workJob.setCmdInfo(VmWorkSerializer.serialize(workInfo));

        _jobMgr.submitAsyncJob(workJob, VmWorkConstants.VM_WORK_QUEUE, vm.getId());

        AsyncJobExecutionContext.getCurrentExecutionContext().joinJob(workJob.getId());

        return new VmJobSnapshotOutcome(workJob, snapshotId);
    }

    @ReflectionUse
    private Pair<JobInfo.Status, String> orchestrateExtractVolume(VmWorkExtractVolume work) throws Exception {
        String volUrl = orchestrateExtractVolume(work.getVolumeId(), work.getZoneId());
        return new Pair<JobInfo.Status, String>(JobInfo.Status.SUCCEEDED, _jobMgr.marshallResultObject(volUrl));
    }

    @ReflectionUse
    private Pair<JobInfo.Status, String> orchestrateAttachVolumeToVM(VmWorkAttachVolume work) throws Exception {
        Volume vol = orchestrateAttachVolumeToVM(work.getVmId(), work.getVolumeId(), work.getDeviceId());

        return new Pair<JobInfo.Status, String>(JobInfo.Status.SUCCEEDED, _jobMgr.marshallResultObject(new Long(vol.getId())));
    }

    @ReflectionUse
    private Pair<JobInfo.Status, String> orchestrateDetachVolumeFromVM(VmWorkDetachVolume work) throws Exception {
        Volume vol = orchestrateDetachVolumeFromVM(work.getVmId(), work.getVolumeId());
        return new Pair<JobInfo.Status, String>(JobInfo.Status.SUCCEEDED, _jobMgr.marshallResultObject(new Long(vol.getId())));
    }

    @ReflectionUse
    private Pair<JobInfo.Status, String> orchestrateResizeVolume(VmWorkResizeVolume work) throws Exception {
        Volume vol = orchestrateResizeVolume(work.getVolumeId(), work.getCurrentSize(), work.getNewSize(), work.getNewMinIops(), work.getNewMaxIops(), work.getNewHypervisorSnapshotReserve(),
                work.getNewServiceOfferingId(), work.isShrinkOk());
        return new Pair<JobInfo.Status, String>(JobInfo.Status.SUCCEEDED, _jobMgr.marshallResultObject(new Long(vol.getId())));
    }

    @ReflectionUse
    private Pair<JobInfo.Status, String> orchestrateMigrateVolume(VmWorkMigrateVolume work) throws Exception {
        VolumeVO volume = _volsDao.findById(work.getVolumeId());
        StoragePoolVO targetStoragePool = _storagePoolDao.findById(work.getDestPoolId());
        DiskOfferingVO newDiskOffering = _diskOfferingDao.findById(work.getNewDiskOfferingId());

        Volume newVol = orchestrateMigrateVolume(volume, targetStoragePool, work.isLiveMigrate(), newDiskOffering);

        return new Pair<JobInfo.Status, String>(JobInfo.Status.SUCCEEDED, _jobMgr.marshallResultObject(newVol.getId()));
    }

    @ReflectionUse
    private Pair<JobInfo.Status, String> orchestrateTakeVolumeSnapshot(VmWorkTakeVolumeSnapshot work) throws Exception {
        Account account = _accountDao.findById(work.getAccountId());
        orchestrateTakeVolumeSnapshot(work.getVolumeId(), work.getPolicyId(), work.getSnapshotId(), account, work.isQuiesceVm(), work.getLocationType(), work.isAsyncBackup());
        return new Pair<JobInfo.Status, String>(JobInfo.Status.SUCCEEDED, _jobMgr.marshallResultObject(work.getSnapshotId()));
    }

    @Override
    public Pair<JobInfo.Status, String> handleVmWorkJob(VmWork work) throws Exception {
        return _jobHandlerProxy.handleVmWorkJob(work);
    }

    private VmWorkJobVO createPlaceHolderWork(long instanceId) {
        VmWorkJobVO workJob = new VmWorkJobVO("");

        workJob.setDispatcher(VmWorkConstants.VM_WORK_JOB_PLACEHOLDER);
        workJob.setCmd("");
        workJob.setCmdInfo("");

        workJob.setAccountId(0);
        workJob.setUserId(0);
        workJob.setStep(VmWorkJobVO.Step.Starting);
        workJob.setVmType(VirtualMachine.Type.Instance);
        workJob.setVmInstanceId(instanceId);
        workJob.setInitMsid(ManagementServerNode.getManagementServerId());

        _workJobDao.persist(workJob);

        return workJob;
    }

    @Override
    public String getConfigComponentName() {
        return VolumeApiService.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {ConcurrentMigrationsThresholdPerDatastore};
    }
}
