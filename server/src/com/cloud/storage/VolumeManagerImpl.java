/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.cloud.storage;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.command.user.volume.AttachVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.CreateVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.DetachVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.MigrateVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.ResizeVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.UploadVolumeCmd;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProviderManager;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreRole;
import org.apache.cloudstack.engine.subsystem.api.storage.ImageDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.ScopeType;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.StoragePoolAllocator;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService.VolumeApiResult;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.AttachVolumeAnswer;
import com.cloud.agent.api.AttachVolumeCommand;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.alert.AlertManager;
import com.cloud.api.ApiDBUtils;
import com.cloud.async.AsyncJobExecutor;
import com.cloud.async.AsyncJobManager;
import com.cloud.async.AsyncJobVO;
import com.cloud.async.BaseAsyncJobExecutor;
import com.cloud.capacity.CapacityManager;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.consoleproxy.ConsoleProxyManager;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.domain.Domain;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientStorageCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.HypervisorGuruManager;
import com.cloud.hypervisor.dao.HypervisorCapabilitiesDao;
import com.cloud.network.NetworkModel;
import com.cloud.org.Grouping;
import com.cloud.resource.ResourceManager;
import com.cloud.server.ManagementServer;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.Volume.Event;
import com.cloud.storage.Volume.Type;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotPolicyDao;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.StoragePoolWorkDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VMTemplateS3Dao;
import com.cloud.storage.dao.VMTemplateSwiftDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeHostDao;
import com.cloud.storage.download.DownloadMonitor;
import com.cloud.storage.s3.S3Manager;
import com.cloud.storage.secondary.SecondaryStorageVmManager;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.storage.snapshot.SnapshotScheduler;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.template.TemplateManager;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.UserContext;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.EnumUtils;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.ConsoleProxyDao;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.SecondaryStorageVmDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

@Component
public class VolumeManagerImpl extends ManagerBase implements VolumeManager {
    private static final Logger s_logger = Logger
            .getLogger(VolumeManagerImpl.class);
    @Inject
    protected UserVmManager _userVmMgr;
    @Inject
    protected AgentManager _agentMgr;
    @Inject
    protected TemplateManager _tmpltMgr;
    @Inject
    protected AsyncJobManager _asyncMgr;
    @Inject
    protected SnapshotManager _snapshotMgr;
    @Inject
    protected SnapshotScheduler _snapshotScheduler;
    @Inject
    protected AccountManager _accountMgr;
    @Inject
    protected ConfigurationManager _configMgr;
    @Inject
    protected ConsoleProxyManager _consoleProxyMgr;
    @Inject
    protected SecondaryStorageVmManager _secStorageMgr;
    @Inject
    protected NetworkModel _networkMgr;
    @Inject
    protected ServiceOfferingDao _serviceOfferingDao;
    @Inject
    protected VolumeDao _volsDao;
    @Inject
    protected HostDao _hostDao;
    @Inject
    protected ConsoleProxyDao _consoleProxyDao;
    @Inject
    protected SnapshotDao _snapshotDao;
    @Inject
    protected SnapshotManager _snapMgr;
    @Inject
    protected SnapshotPolicyDao _snapshotPolicyDao;
    @Inject
    protected StoragePoolHostDao _storagePoolHostDao;
    @Inject
    protected AlertManager _alertMgr;
    @Inject
    protected VMTemplateHostDao _vmTemplateHostDao = null;
    @Inject
    protected VMTemplatePoolDao _vmTemplatePoolDao = null;
    @Inject
    protected VMTemplateSwiftDao _vmTemplateSwiftDao = null;
    @Inject
    protected VMTemplateS3Dao _vmTemplateS3Dao;
    @Inject
    protected S3Manager _s3Mgr;
    @Inject
    protected VMTemplateDao _vmTemplateDao = null;
    @Inject
    protected StoragePoolHostDao _poolHostDao = null;
    @Inject
    protected UserVmDao _userVmDao;
    @Inject
    VolumeHostDao _volumeHostDao;
    @Inject
    protected VMInstanceDao _vmInstanceDao;
    @Inject
    protected PrimaryDataStoreDao _storagePoolDao = null;
    @Inject
    protected CapacityDao _capacityDao;
    @Inject
    protected CapacityManager _capacityMgr;
    @Inject
    protected DiskOfferingDao _diskOfferingDao;
    @Inject
    protected AccountDao _accountDao;
    @Inject
    protected EventDao _eventDao = null;
    @Inject
    protected DataCenterDao _dcDao = null;
    @Inject
    protected HostPodDao _podDao = null;
    @Inject
    protected VMTemplateDao _templateDao;
    @Inject
    protected VMTemplateHostDao _templateHostDao;
    @Inject
    protected ServiceOfferingDao _offeringDao;
    @Inject
    protected DomainDao _domainDao;
    @Inject
    protected UserDao _userDao;
    @Inject
    protected ClusterDao _clusterDao;
    @Inject
    protected UsageEventDao _usageEventDao;
    @Inject
    protected VirtualMachineManager _vmMgr;
    @Inject
    protected DomainRouterDao _domrDao;
    @Inject
    protected SecondaryStorageVmDao _secStrgDao;
    @Inject
    protected StoragePoolWorkDao _storagePoolWorkDao;
    @Inject
    protected HypervisorGuruManager _hvGuruMgr;
    @Inject
    protected VolumeDao _volumeDao;
    @Inject
    protected OCFS2Manager _ocfs2Mgr;
    @Inject
    protected ResourceLimitService _resourceLimitMgr;
    @Inject
    protected SecondaryStorageVmManager _ssvmMgr;
    @Inject
    protected ResourceManager _resourceMgr;
    @Inject
    protected DownloadMonitor _downloadMonitor;
    @Inject
    protected ResourceTagDao _resourceTagDao;
    @Inject
    protected List<StoragePoolAllocator> _storagePoolAllocators;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    ManagementServer _msServer;
    @Inject
    DataStoreManager dataStoreMgr;
    @Inject
    DataStoreProviderManager dataStoreProviderMgr;
    @Inject
    VolumeService volService;
    @Inject
    VolumeDataFactory volFactory;
    @Inject
    ImageDataFactory tmplFactory;
    @Inject
    SnapshotDataFactory snapshotFactory;
    private int _copyvolumewait;
    @Inject
    protected HypervisorCapabilitiesDao _hypervisorCapabilitiesDao;
    private final StateMachine2<Volume.State, Volume.Event, Volume> _volStateMachine;
    @Inject
    StorageManager storageMgr;
    private int _customDiskOfferingMinSize = 1;
    private int _customDiskOfferingMaxSize = 1024;
    private long _maxVolumeSizeInGb;
    private boolean _recreateSystemVmEnabled;
    protected SearchBuilder<VMTemplateHostVO> HostTemplateStatesSearch;
    
    public VolumeManagerImpl() {
        _volStateMachine = Volume.State.getStateMachine();
    }
    
    @Override
    public VolumeInfo moveVolume(VolumeInfo volume, long destPoolDcId,
            Long destPoolPodId, Long destPoolClusterId,
            HypervisorType dataDiskHyperType)
            throws ConcurrentOperationException {

        // Find a destination storage pool with the specified criteria
        DiskOfferingVO diskOffering = _diskOfferingDao.findById(volume
                .getDiskOfferingId());
        DiskProfile dskCh = new DiskProfile(volume.getId(),
                volume.getVolumeType(), volume.getName(), diskOffering.getId(),
                diskOffering.getDiskSize(), diskOffering.getTagsArray(),
                diskOffering.getUseLocalStorage(),
                diskOffering.isRecreatable(), null);
        dskCh.setHyperType(dataDiskHyperType);
        DataCenterVO destPoolDataCenter = _dcDao.findById(destPoolDcId);
        HostPodVO destPoolPod = _podDao.findById(destPoolPodId);
        
        StoragePool destPool = storageMgr.findStoragePool(dskCh,
                destPoolDataCenter, destPoolPod, destPoolClusterId, null, null,
                new HashSet<StoragePool>());

        if (destPool == null) {
            throw new CloudRuntimeException(
                    "Failed to find a storage pool with enough capacity to move the volume to.");
        }
        
        Volume newVol = migrateVolume(volume, destPool);
        return this.volFactory.getVolume(newVol.getId());
    }

    /*
     * Upload the volume to secondary storage.
     */
    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_VOLUME_UPLOAD, eventDescription = "uploading volume", async = true)
    public VolumeVO uploadVolume(UploadVolumeCmd cmd)
            throws ResourceAllocationException {
        Account caller = UserContext.current().getCaller();
        long ownerId = cmd.getEntityOwnerId();
        Long zoneId = cmd.getZoneId();
        String volumeName = cmd.getVolumeName();
        String url = cmd.getUrl();
        String format = cmd.getFormat();
        String imageStoreUuid = cmd.getImageStoreUuid();
        DataStore store = this._tmpltMgr.getImageStore(imageStoreUuid, zoneId);

        validateVolume(caller, ownerId, zoneId, volumeName, url, format);
        
        VolumeVO volume = persistVolume(caller, ownerId, zoneId, volumeName,
                url, cmd.getFormat());
        
        VolumeInfo vol = this.volFactory.getVolume(volume.getId());
        
        RegisterVolumePayload payload = new RegisterVolumePayload(cmd.getUrl(), cmd.getChecksum(),
                cmd.getFormat());
        vol.addPayload(payload);
        
        this.volService.registerVolume(vol, store);
        return volume;
    }

    private boolean validateVolume(Account caller, long ownerId, Long zoneId,
            String volumeName, String url, String format)
            throws ResourceAllocationException {

        // permission check
        _accountMgr.checkAccess(caller, null, true,
                _accountMgr.getActiveAccountById(ownerId));

        // Check that the resource limit for volumes won't be exceeded
        _resourceLimitMgr.checkResourceLimit(_accountMgr.getAccount(ownerId),
                ResourceType.volume);

        // Verify that zone exists
        DataCenterVO zone = _dcDao.findById(zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException(
                    "Unable to find zone by id " + zoneId);
        }

        // Check if zone is disabled
        if (Grouping.AllocationState.Disabled == zone.getAllocationState()
                && !_accountMgr.isRootAdmin(caller.getType())) {
            throw new PermissionDeniedException(
                    "Cannot perform this operation, Zone is currently disabled: "
                            + zoneId);
        }

        if (url.toLowerCase().contains("file://")) {
            throw new InvalidParameterValueException(
                    "File:// type urls are currently unsupported");
        }

        ImageFormat imgfmt = ImageFormat.valueOf(format.toUpperCase());
        if (imgfmt == null) {
            throw new IllegalArgumentException("Image format is incorrect "
                    + format + ". Supported formats are "
                    + EnumUtils.listValues(ImageFormat.values()));
        }

        String userSpecifiedName = volumeName;
        if (userSpecifiedName == null) {
            userSpecifiedName = getRandomVolumeName();
        }
        if ((!url.toLowerCase().endsWith("vhd"))
                && (!url.toLowerCase().endsWith("vhd.zip"))
                && (!url.toLowerCase().endsWith("vhd.bz2"))
                && (!url.toLowerCase().endsWith("vhd.gz"))
                && (!url.toLowerCase().endsWith("qcow2"))
                && (!url.toLowerCase().endsWith("qcow2.zip"))
                && (!url.toLowerCase().endsWith("qcow2.bz2"))
                && (!url.toLowerCase().endsWith("qcow2.gz"))
                && (!url.toLowerCase().endsWith("ova"))
                && (!url.toLowerCase().endsWith("ova.zip"))
                && (!url.toLowerCase().endsWith("ova.bz2"))
                && (!url.toLowerCase().endsWith("ova.gz"))
                && (!url.toLowerCase().endsWith("img"))
                && (!url.toLowerCase().endsWith("raw"))) {
            throw new InvalidParameterValueException("Please specify a valid "
                    + format.toLowerCase());
        }

        if ((format.equalsIgnoreCase("vhd") && (!url.toLowerCase().endsWith(
                ".vhd")
                && !url.toLowerCase().endsWith("vhd.zip")
                && !url.toLowerCase().endsWith("vhd.bz2") && !url.toLowerCase()
                .endsWith("vhd.gz")))
                || (format.equalsIgnoreCase("qcow2") && (!url.toLowerCase()
                        .endsWith(".qcow2")
                        && !url.toLowerCase().endsWith("qcow2.zip")
                        && !url.toLowerCase().endsWith("qcow2.bz2") && !url
                        .toLowerCase().endsWith("qcow2.gz")))
                || (format.equalsIgnoreCase("ova") && (!url.toLowerCase()
                        .endsWith(".ova")
                        && !url.toLowerCase().endsWith("ova.zip")
                        && !url.toLowerCase().endsWith("ova.bz2") && !url
                        .toLowerCase().endsWith("ova.gz")))
                || (format.equalsIgnoreCase("raw") && (!url.toLowerCase()
                        .endsWith(".img") && !url.toLowerCase().endsWith("raw")))) {
            throw new InvalidParameterValueException(
                    "Please specify a valid URL. URL:" + url
                            + " is an invalid for the format "
                            + format.toLowerCase());
        }
        validateUrl(url);

        return false;
    }
    
    @Override
    public VolumeVO allocateDuplicateVolume(VolumeVO oldVol, Long templateId) {
        VolumeVO newVol = new VolumeVO(oldVol.getVolumeType(),
                oldVol.getName(), oldVol.getDataCenterId(),
                oldVol.getDomainId(), oldVol.getAccountId(),
                oldVol.getDiskOfferingId(), oldVol.getSize());
        if (templateId != null) {
            newVol.setTemplateId(templateId);
        } else {
            newVol.setTemplateId(oldVol.getTemplateId());
        }
        newVol.setDeviceId(oldVol.getDeviceId());
        newVol.setInstanceId(oldVol.getInstanceId());
        newVol.setRecreatable(oldVol.isRecreatable());
        return _volsDao.persist(newVol);
    }
    
    @DB
    protected VolumeInfo createVolumeFromSnapshot(VolumeVO volume,
            SnapshotVO snapshot) {
        Account account = _accountDao.findById(volume.getAccountId());

        final HashSet<StoragePool> poolsToAvoid = new HashSet<StoragePool>();
        StoragePool pool = null;

        Set<Long> podsToAvoid = new HashSet<Long>();
        Pair<HostPodVO, Long> pod = null;


        DiskOfferingVO diskOffering = _diskOfferingDao
                .findByIdIncludingRemoved(volume.getDiskOfferingId());
        DataCenterVO dc = _dcDao.findById(volume.getDataCenterId());
        DiskProfile dskCh = new DiskProfile(volume, diskOffering,
                snapshot.getHypervisorType());

        // Determine what pod to store the volume in
        while ((pod = _resourceMgr.findPod(null, null, dc, account.getId(),
                podsToAvoid)) != null) {
            podsToAvoid.add(pod.first().getId());
            // Determine what storage pool to store the volume in
            while ((pool = storageMgr.findStoragePool(dskCh, dc, pod.first(), null, null,
                    null, poolsToAvoid)) != null) {
                break;
                
            }
        }
        
        VolumeInfo vol = this.volFactory.getVolume(volume.getId());
        DataStore store = this.dataStoreMgr.getDataStore(pool.getId(), DataStoreRole.Primary);
        SnapshotInfo snapInfo = this.snapshotFactory.getSnapshot(snapshot.getId());
        AsyncCallFuture<VolumeApiResult> future = this.volService.createVolumeFromSnapshot(vol, store, snapInfo);
        try {
            VolumeApiResult result = future.get();
            if (result.isFailed()) {
                s_logger.debug("Failed to create volume from snapshot:" + result.getResult());
                throw new CloudRuntimeException("Failed to create volume from snapshot:" + result.getResult());
            }
            return result.getVolume();
        } catch (InterruptedException e) {
            s_logger.debug("Failed to create volume from snapshot", e);
            throw new CloudRuntimeException("Failed to create volume from snapshot", e);
        } catch (ExecutionException e) {
            s_logger.debug("Failed to create volume from snapshot", e);
            throw new CloudRuntimeException("Failed to create volume from snapshot", e);
        }

    }

    protected DiskProfile createDiskCharacteristics(VolumeInfo volume,
            VMTemplateVO template, DataCenterVO dc, DiskOfferingVO diskOffering) {
        if (volume.getVolumeType() == Type.ROOT
                && Storage.ImageFormat.ISO != template.getFormat()) {
            SearchCriteria<VMTemplateHostVO> sc = HostTemplateStatesSearch
                    .create();
            sc.setParameters("id", template.getId());
            sc.setParameters(
                    "state",
                    com.cloud.storage.VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
            sc.setJoinParameters("host", "dcId", dc.getId());

            List<VMTemplateHostVO> sss = _vmTemplateHostDao.search(sc, null);
            if (sss.size() == 0) {
                throw new CloudRuntimeException("Template "
                        + template.getName()
                        + " has not been completely downloaded to zone "
                        + dc.getId());
            }
            VMTemplateHostVO ss = sss.get(0);

            return new DiskProfile(volume.getId(), volume.getVolumeType(),
                    volume.getName(), diskOffering.getId(), ss.getSize(),
                    diskOffering.getTagsArray(),
                    diskOffering.getUseLocalStorage(),
                    diskOffering.isRecreatable(),
                    Storage.ImageFormat.ISO != template.getFormat() ? template
                            .getId() : null);
        } else {
            return new DiskProfile(volume.getId(), volume.getVolumeType(),
                    volume.getName(), diskOffering.getId(),
                    diskOffering.getDiskSize(), diskOffering.getTagsArray(),
                    diskOffering.getUseLocalStorage(),
                    diskOffering.isRecreatable(), null);
        }
    }

    protected VolumeVO createVolumeFromSnapshot(VolumeVO volume, long snapshotId) {
        VolumeInfo createdVolume = null;
        SnapshotVO snapshot = _snapshotDao.findById(snapshotId); 
        createdVolume = createVolumeFromSnapshot(volume,
                snapshot);

        UsageEventVO usageEvent = new UsageEventVO(
                EventTypes.EVENT_VOLUME_CREATE,
                createdVolume.getAccountId(),
                createdVolume.getDataCenterId(), createdVolume.getId(),
                createdVolume.getName(), createdVolume.getDiskOfferingId(),
                null, createdVolume.getSize());
        _usageEventDao.persist(usageEvent);

        return this._volsDao.findById(createdVolume.getId());
    }

    @DB
    public VolumeInfo copyVolumeFromSecToPrimary(VolumeInfo volume,
            VMInstanceVO vm, VMTemplateVO template, DataCenterVO dc,
            HostPodVO pod, Long clusterId, ServiceOfferingVO offering,
            DiskOfferingVO diskOffering, List<StoragePool> avoids,
            long size, HypervisorType hyperType) throws NoTransitionException {

        final HashSet<StoragePool> avoidPools = new HashSet<StoragePool>(
                avoids);
        DiskProfile dskCh = createDiskCharacteristics(volume, template, dc,
                diskOffering);
        dskCh.setHyperType(vm.getHypervisorType());
        // Find a suitable storage to create volume on
        StoragePool destPool = storageMgr.findStoragePool(dskCh, dc, pod,
                clusterId, null, vm, avoidPools);
        DataStore destStore = this.dataStoreMgr.getDataStore(destPool.getId(), DataStoreRole.Primary);
        AsyncCallFuture<VolumeApiResult> future = this.volService.copyVolume(volume, destStore);

        try {
            VolumeApiResult result = future.get();
            if (result.isFailed()) {
                s_logger.debug("copy volume failed: " + result.getResult());
                throw new CloudRuntimeException("copy volume failed: " + result.getResult());
            } 
            return result.getVolume();
        } catch (InterruptedException e) {
            s_logger.debug("Failed to copy volume: " + volume.getId(), e);
            throw new CloudRuntimeException("Failed to copy volume", e);
        } catch (ExecutionException e) {
            s_logger.debug("Failed to copy volume: " + volume.getId(), e);
            throw new CloudRuntimeException("Failed to copy volume", e);
        }
    }

    @DB
    public VolumeInfo createVolume(VolumeInfo volume, VMInstanceVO vm,
            VMTemplateVO template, DataCenterVO dc, HostPodVO pod,
            Long clusterId, ServiceOfferingVO offering,
            DiskOfferingVO diskOffering, List<StoragePool> avoids,
            long size, HypervisorType hyperType) {
        StoragePool pool = null;
 
        if (diskOffering != null && diskOffering.isCustomized()) {
            diskOffering.setDiskSize(size);
        }
        
        DiskProfile dskCh = null;
        if (volume.getVolumeType() == Type.ROOT
                && Storage.ImageFormat.ISO != template.getFormat()) {
            dskCh = createDiskCharacteristics(volume, template, dc, offering);
        } else {
            dskCh = createDiskCharacteristics(volume, template, dc,
                    diskOffering);
        }

        dskCh.setHyperType(hyperType);
        
        final HashSet<StoragePool> avoidPools = new HashSet<StoragePool>(
                avoids);

        pool = storageMgr.findStoragePool(dskCh, dc, pod, clusterId, vm.getHostId(),
                vm, avoidPools);
        if (pool == null) {
            s_logger.warn("Unable to find storage poll when create volume "
                    + volume.getName());
            throw new CloudRuntimeException("Unable to find storage poll when create volume" + volume.getName());
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Trying to create " + volume + " on " + pool);
        }
        DataStore store = this.dataStoreMgr.getDataStore(pool.getId(), DataStoreRole.Primary);
        AsyncCallFuture<VolumeApiResult> future = null;
        boolean isNotCreatedFromTemplate = volume.getTemplateId() == null ? true : false;
        if (isNotCreatedFromTemplate) {
            future = this.volService.createVolumeAsync(volume, store);
        } else {
            TemplateInfo templ = this.tmplFactory.getTemplate(template.getId());
            future = this.volService.createVolumeFromTemplateAsync(volume, store.getId(), templ);
        }
        try {
            VolumeApiResult result = future.get();
            if (result.isFailed()) {
                s_logger.debug("create volume failed: " + result.getResult());
                throw new CloudRuntimeException("create volume failed:" + result.getResult());
            }
            UsageEventVO usageEvent = new UsageEventVO(
                    EventTypes.EVENT_VOLUME_CREATE, volume.getAccountId(),
                    volume.getDataCenterId(), volume.getId(), volume.getName(),
                    volume.getDiskOfferingId(), null, volume.getSize());
            _usageEventDao.persist(usageEvent);
            return result.getVolume();
        } catch (InterruptedException e) {
            s_logger.error("create volume failed", e);
            throw new CloudRuntimeException("create volume failed", e);
        } catch (ExecutionException e) {
            s_logger.error("create volume failed", e);
            throw new CloudRuntimeException("create volume failed", e);
        }

    }
    
    public String getRandomVolumeName() {
        return UUID.randomUUID().toString();
    }
    
    private VolumeVO persistVolume(Account caller, long ownerId, Long zoneId,
            String volumeName, String url, String format) {

        Transaction txn = Transaction.currentTxn();
        txn.start();

        VolumeVO volume = new VolumeVO(volumeName, zoneId, -1, -1, -1,
                new Long(-1), null, null, 0, Volume.Type.DATADISK);
        volume.setPoolId(null);
        volume.setDataCenterId(zoneId);
        volume.setPodId(null);
        volume.setAccountId(ownerId);
        volume.setDomainId(((caller == null) ? Domain.ROOT_DOMAIN : caller
                .getDomainId()));
        long diskOfferingId = _diskOfferingDao.findByUniqueName(
                "Cloud.com-Custom").getId();
        volume.setDiskOfferingId(diskOfferingId);
        // volume.setSize(size);
        volume.setInstanceId(null);
        volume.setUpdated(new Date());
        volume.setDomainId((caller == null) ? Domain.ROOT_DOMAIN : caller
                .getDomainId());

        volume = _volsDao.persist(volume);
        try {
            stateTransitTo(volume, Event.UploadRequested);
        } catch (NoTransitionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        UserContext.current().setEventDetails("Volume Id: " + volume.getId());

        // Increment resource count during allocation; if actual creation fails,
        // decrement it
        _resourceLimitMgr.incrementResourceCount(volume.getAccountId(),
                ResourceType.volume);

        txn.commit();
        return volume;
    }
    
    @Override
    public boolean volumeOnSharedStoragePool(VolumeVO volume) {
        Long poolId = volume.getPoolId();
        if (poolId == null) {
            return false;
        } else {
            StoragePoolVO pool = _storagePoolDao.findById(poolId);

            if (pool == null) {
                return false;
            } else {
                return (pool.getScope() == ScopeType.HOST) ? false : true;
            }
        }
    }

    @Override
    public boolean volumeInactive(Volume volume) {
        Long vmId = volume.getInstanceId();
        if (vmId != null) {
            UserVm vm = _userVmDao.findById(vmId);
            if (vm == null) {
                return true;
            }
            State state = vm.getState();
            if (state.equals(State.Stopped) || state.equals(State.Destroyed)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getVmNameOnVolume(Volume volume) {
        Long vmId = volume.getInstanceId();
        if (vmId != null) {
            VMInstanceVO vm = _vmInstanceDao.findById(vmId);

            if (vm == null) {
                return null;
            }
            return vm.getInstanceName();
        }
        return null;
    }

    /*
     * Just allocate a volume in the database, don't send the createvolume cmd
     * to hypervisor. The volume will be finally created only when it's attached
     * to a VM.
     */
    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_VOLUME_CREATE, eventDescription = "creating volume", create = true)
    public VolumeVO allocVolume(CreateVolumeCmd cmd)
            throws ResourceAllocationException {
        // FIXME: some of the scheduled event stuff might be missing here...
        Account caller = UserContext.current().getCaller();

        long ownerId = cmd.getEntityOwnerId();

        // permission check
        _accountMgr.checkAccess(caller, null, true,
                _accountMgr.getActiveAccountById(ownerId));

        // Check that the resource limit for volumes won't be exceeded
        _resourceLimitMgr.checkResourceLimit(_accountMgr.getAccount(ownerId),
                ResourceType.volume);

        Long zoneId = cmd.getZoneId();
        Long diskOfferingId = null;
        DiskOfferingVO diskOffering = null;
        Long size = null;
        // Volume VO used for extracting the source template id
        VolumeVO parentVolume = null;

        // validate input parameters before creating the volume
        if ((cmd.getSnapshotId() == null && cmd.getDiskOfferingId() == null)
                || (cmd.getSnapshotId() != null && cmd.getDiskOfferingId() != null)) {
            throw new InvalidParameterValueException(
                    "Either disk Offering Id or snapshot Id must be passed whilst creating volume");
        }

        if (cmd.getSnapshotId() == null) {// create a new volume

            diskOfferingId = cmd.getDiskOfferingId();
            size = cmd.getSize();
            Long sizeInGB = size;
            if (size != null) {
                if (size > 0) {
                    size = size * 1024 * 1024 * 1024; // user specify size in GB
                } else {
                    throw new InvalidParameterValueException(
                            "Disk size must be larger than 0");
                }
            }

            // Check that the the disk offering is specified
            diskOffering = _diskOfferingDao.findById(diskOfferingId);
            if ((diskOffering == null) || diskOffering.getRemoved() != null
                    || !DiskOfferingVO.Type.Disk.equals(diskOffering.getType())) {
                throw new InvalidParameterValueException(
                        "Please specify a valid disk offering.");
            }

            if (diskOffering.isCustomized()) {
                if (size == null) {
                    throw new InvalidParameterValueException(
                            "This disk offering requires a custom size specified");
                }
                if ((sizeInGB < _customDiskOfferingMinSize)
                        || (sizeInGB > _customDiskOfferingMaxSize)) {
                    throw new InvalidParameterValueException("Volume size: "
                            + sizeInGB + "GB is out of allowed range. Max: "
                            + _customDiskOfferingMaxSize + " Min:"
                            + _customDiskOfferingMinSize);
                }
            }

            if (!diskOffering.isCustomized() && size != null) {
                throw new InvalidParameterValueException(
                        "This disk offering does not allow custom size");
            }

            if (diskOffering.getDomainId() == null) {
                // do nothing as offering is public
            } else {
                _configMgr.checkDiskOfferingAccess(caller, diskOffering);
            }

            if (diskOffering.getDiskSize() > 0) {
                size = diskOffering.getDiskSize();
            }

            if (!validateVolumeSizeRange(size)) {// convert size from mb to gb
                                                 // for validation
                throw new InvalidParameterValueException(
                        "Invalid size for custom volume creation: " + size
                                + " ,max volume size is:" + _maxVolumeSizeInGb);
            }
        } else { // create volume from snapshot
            Long snapshotId = cmd.getSnapshotId();
            SnapshotVO snapshotCheck = _snapshotDao.findById(snapshotId);
            if (snapshotCheck == null) {
                throw new InvalidParameterValueException(
                        "unable to find a snapshot with id " + snapshotId);
            }

            if (snapshotCheck.getState() != Snapshot.State.BackedUp) {
                throw new InvalidParameterValueException("Snapshot id="
                        + snapshotId + " is not in " + Snapshot.State.BackedUp
                        + " state yet and can't be used for volume creation");
            }
            parentVolume = _volsDao.findByIdIncludingRemoved(snapshotCheck.getVolumeId());

            diskOfferingId = snapshotCheck.getDiskOfferingId();
            diskOffering = _diskOfferingDao.findById(diskOfferingId);
            zoneId = snapshotCheck.getDataCenterId();
            size = snapshotCheck.getSize(); // ; disk offering is used for tags
                                            // purposes

            // check snapshot permissions
            _accountMgr.checkAccess(caller, null, true, snapshotCheck);
        }

        // Verify that zone exists
        DataCenterVO zone = _dcDao.findById(zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException(
                    "Unable to find zone by id " + zoneId);
        }

        // Check if zone is disabled
        if (Grouping.AllocationState.Disabled == zone.getAllocationState()
                && !_accountMgr.isRootAdmin(caller.getType())) {
            throw new PermissionDeniedException(
                    "Cannot perform this operation, Zone is currently disabled: "
                            + zoneId);
        }

        // If local storage is disabled then creation of volume with local disk
        // offering not allowed
        if (!zone.isLocalStorageEnabled() && diskOffering.getUseLocalStorage()) {
            throw new InvalidParameterValueException(
                    "Zone is not configured to use local storage but volume's disk offering "
                            + diskOffering.getName() + " uses it");
        }

        String userSpecifiedName = cmd.getVolumeName();
        if (userSpecifiedName == null) {
            userSpecifiedName = getRandomVolumeName();
        }

        Transaction txn = Transaction.currentTxn();
        txn.start();

        VolumeVO volume = new VolumeVO(userSpecifiedName, -1, -1, -1, -1,
                new Long(-1), null, null, 0, Volume.Type.DATADISK);
        volume.setPoolId(null);
        volume.setDataCenterId(zoneId);
        volume.setPodId(null);
        volume.setAccountId(ownerId);
        volume.setDomainId(((caller == null) ? Domain.ROOT_DOMAIN : caller
                .getDomainId()));
        volume.setDiskOfferingId(diskOfferingId);
        volume.setSize(size);
        volume.setInstanceId(null);
        volume.setUpdated(new Date());
        volume.setDomainId((caller == null) ? Domain.ROOT_DOMAIN : caller
                .getDomainId());
        if (parentVolume != null) {
            volume.setTemplateId(parentVolume.getTemplateId());
        }  else {
            volume.setTemplateId(null);
        }

        volume = _volsDao.persist(volume);
        if (cmd.getSnapshotId() == null) {
            // for volume created from snapshot, create usage event after volume
            // creation
            UsageEventVO usageEvent = new UsageEventVO(
                    EventTypes.EVENT_VOLUME_CREATE, volume.getAccountId(),
                    volume.getDataCenterId(), volume.getId(), volume.getName(),
                    diskOfferingId, null, size);
            _usageEventDao.persist(usageEvent);
        }

        UserContext.current().setEventDetails("Volume Id: " + volume.getId());

        // Increment resource count during allocation; if actual creation fails,
        // decrement it
        _resourceLimitMgr.incrementResourceCount(volume.getAccountId(),
                ResourceType.volume);

        txn.commit();

        return volume;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_VOLUME_CREATE, eventDescription = "creating volume", async = true)
    public VolumeVO createVolume(CreateVolumeCmd cmd) {
        VolumeVO volume = _volsDao.findById(cmd.getEntityId());
        boolean created = true;

        try {
            if (cmd.getSnapshotId() != null) {
                volume = createVolumeFromSnapshot(volume, cmd.getSnapshotId());
                if (volume.getState() != Volume.State.Ready) {
                    created = false;
                }
            }
            return volume;
        } catch(Exception e) {
            created = false;
            s_logger.debug("Failed to create volume: " + volume.getId(), e);
            return null;
        } finally {
            if (!created) {
                s_logger.trace("Decrementing volume resource count for account id="
                        + volume.getAccountId()
                        + " as volume failed to create on the backend");
                _resourceLimitMgr.decrementResourceCount(volume.getAccountId(),
                        ResourceType.volume);
            }
        }
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_VOLUME_RESIZE, eventDescription = "resizing volume", async = true)
    public VolumeVO resizeVolume(ResizeVolumeCmd cmd) {
        Long newSize = null;
        boolean shrinkOk = cmd.getShrinkOk();
        
        VolumeVO volume = _volsDao.findById(cmd.getEntityId());
        if (volume == null) {
            throw new InvalidParameterValueException("No such volume");
        }
        
        DiskOfferingVO diskOffering = _diskOfferingDao.findById(volume
                .getDiskOfferingId());
        DiskOfferingVO newDiskOffering = null;

        newDiskOffering = _diskOfferingDao.findById(cmd.getNewDiskOfferingId());

        /*
         * Volumes with no hypervisor have never been assigned, and can be
         * resized by recreating. perhaps in the future we can just update the
         * db entry for the volume
         */
        if (_volsDao.getHypervisorType(volume.getId()) == HypervisorType.None) {
            throw new InvalidParameterValueException(
                    "Can't resize a volume that has never been attached, not sure which hypervisor type. Recreate volume to resize.");
        }

        /* Only works for KVM/Xen for now */
        if (_volsDao.getHypervisorType(volume.getId()) != HypervisorType.KVM
                && _volsDao.getHypervisorType(volume.getId()) != HypervisorType.XenServer) {
            throw new InvalidParameterValueException(
                    "Cloudstack currently only supports volumes marked as KVM or XenServer hypervisor for resize");
        }


        if (volume.getState() != Volume.State.Ready) {
            throw new InvalidParameterValueException(
                    "Volume should be in ready state before attempting a resize");
        }

        if (!volume.getVolumeType().equals(Volume.Type.DATADISK)) {
            throw new InvalidParameterValueException(
                    "Can only resize DATA volumes");
        }

        /*
         * figure out whether or not a new disk offering or size parameter is
         * required, get the correct size value
         */
        if (newDiskOffering == null) {
            if (diskOffering.isCustomized()) {
                newSize = cmd.getSize();

                if (newSize == null) {
                    throw new InvalidParameterValueException(
                            "new offering is of custom size, need to specify a size");
                }

                newSize = (newSize << 30);
            } else {
                throw new InvalidParameterValueException("current offering"
                        + volume.getDiskOfferingId()
                        + " cannot be resized, need to specify a disk offering");
            }
        } else {

            if (newDiskOffering.getRemoved() != null
                    || !DiskOfferingVO.Type.Disk.equals(newDiskOffering
                            .getType())) {
                throw new InvalidParameterValueException(
                        "Disk offering ID is missing or invalid");
            }

            if (diskOffering.getTags() != null) {
                if (!newDiskOffering.getTags().equals(diskOffering.getTags())) {
                    throw new InvalidParameterValueException(
                            "Tags on new and old disk offerings must match");
                }
            } else if (newDiskOffering.getTags() != null) {
                throw new InvalidParameterValueException(
                        "There are no tags on current disk offering, new disk offering needs to have no tags");
            }

            if (newDiskOffering.getDomainId() == null) {
                // do nothing as offering is public
            } else {
                _configMgr.checkDiskOfferingAccess(UserContext.current()
                        .getCaller(), newDiskOffering);
            }

            if (newDiskOffering.isCustomized()) {
                newSize = cmd.getSize();

                if (newSize == null) {
                    throw new InvalidParameterValueException(
                            "new offering is of custom size, need to specify a size");
                }

                newSize = (newSize << 30);
            } else {
                newSize = newDiskOffering.getDiskSize();
            }
        }

        if (newSize == null) {
            throw new InvalidParameterValueException(
                    "could not detect a size parameter or fetch one from the diskofferingid parameter");
        }

        if (!validateVolumeSizeRange(newSize)) {
            throw new InvalidParameterValueException(
                    "Requested size out of range");
        }

        /* does the caller have the authority to act on this volume? */
        _accountMgr.checkAccess(UserContext.current().getCaller(), null, true,
                volume);

        UserVmVO userVm = _userVmDao.findById(volume.getInstanceId());

        PrimaryDataStoreInfo pool = (PrimaryDataStoreInfo)this.dataStoreMgr.getDataStore(volume.getPoolId(), DataStoreRole.Primary);
        long currentSize = volume.getSize();

        /*
         * lets make certain they (think they) know what they're doing if they
         * want to shrink, by forcing them to provide the shrinkok parameter.
         * This will be checked again at the hypervisor level where we can see
         * the actual disk size
         */
        if (currentSize > newSize && !shrinkOk) {
            throw new InvalidParameterValueException(
                    "Going from existing size of "
                            + currentSize
                            + " to size of "
                            + newSize
                            + " would shrink the volume, need to sign off by supplying the shrinkok parameter with value of true");
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
                hosts = new long[] { userVm.getHostId() };
            } else if (userVm.getLastHostId() != null) {
                hosts = new long[] { userVm.getLastHostId() };
            }

            /* Xen only works offline, SR does not support VDI.resizeOnline */
            if (_volsDao.getHypervisorType(volume.getId()) == HypervisorType.XenServer
                    && !userVm.getState().equals(State.Stopped)) {
                throw new InvalidParameterValueException(
                        "VM must be stopped or disk detached in order to resize with the Xen HV");
            }
        }
        
        ResizeVolumePayload payload = new ResizeVolumePayload(newSize, shrinkOk, instanceName, hosts);
        
        try {
        	VolumeInfo vol = this.volFactory.getVolume(volume.getId());
            vol.addPayload(payload);
            
        	AsyncCallFuture<VolumeApiResult> future = this.volService.resize(vol);
        	future.get();
        	volume = _volsDao.findById(volume.getId());

        	if (newDiskOffering != null) {
        		volume.setDiskOfferingId(cmd.getNewDiskOfferingId());
        	}
        	_volsDao.update(volume.getId(), volume);

        	return volume;
		} catch (InterruptedException e) {
			s_logger.debug("failed get resize volume result", e);
		} catch (ExecutionException e) {
			s_logger.debug("failed get resize volume result", e);
		} catch (Exception e) {
			s_logger.debug("failed get resize volume result", e);
		}
       
        return null;
    }
    
    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_VOLUME_DELETE, eventDescription = "deleting volume")
    public boolean deleteVolume(long volumeId, Account caller)
            throws ConcurrentOperationException {

        VolumeVO volume = _volsDao.findById(volumeId);
        if (volume == null) {
            throw new InvalidParameterValueException(
                    "Unable to aquire volume with ID: " + volumeId);
        }

        if (!_snapshotMgr.canOperateOnVolume(volume)) {
            throw new InvalidParameterValueException(
                    "There are snapshot creating on it, Unable to delete the volume");
        }

        _accountMgr.checkAccess(caller, null, true, volume);

           if (volume.getInstanceId() != null) {
            throw new InvalidParameterValueException(
                    "Please specify a volume that is not attached to any VM.");
        }

        if (volume.getState() == Volume.State.UploadOp) {
            VolumeHostVO volumeHost = _volumeHostDao.findByVolumeId(volume
                    .getId());
            if (volumeHost.getDownloadState() == VMTemplateStorageResourceAssoc.Status.DOWNLOAD_IN_PROGRESS) {
                throw new InvalidParameterValueException(
                        "Please specify a volume that is not uploading");
            }
        }

        try {
            if (volume.getState() != Volume.State.Destroy && volume.getState() != Volume.State.Expunging && volume.getState() != Volume.State.Expunging) {
                Long instanceId = volume.getInstanceId();
                if (!this.volService.destroyVolume(volume.getId())) {
                    return false;
                }
                
                VMInstanceVO vmInstance = this._vmInstanceDao.findById(instanceId);
                if (instanceId == null
                        || (vmInstance.getType().equals(VirtualMachine.Type.User))) {
                    // Decrement the resource count for volumes belonging user VM's only
                    _resourceLimitMgr.decrementResourceCount(volume.getAccountId(),
                            ResourceType.volume);
                    // Log usage event for volumes belonging user VM's only
                    UsageEventVO usageEvent = new UsageEventVO(
                            EventTypes.EVENT_VOLUME_DELETE, volume.getAccountId(),
                            volume.getDataCenterId(), volume.getId(), volume.getName());
                    _usageEventDao.persist(usageEvent);
                }
            }
            AsyncCallFuture<VolumeApiResult> future = this.volService.expungeVolumeAsync(this.volFactory.getVolume(volume.getId()));
            future.get();
            
        } catch (Exception e) {
            s_logger.warn("Failed to expunge volume:", e);
            return false;
        }

        return true;
    }

    private boolean validateVolumeSizeRange(long size) {
        if (size < 0 || (size > 0 && size < (1024 * 1024 * 1024))) {
            throw new InvalidParameterValueException(
                    "Please specify a size of at least 1 Gb.");
        } else if (size > (_maxVolumeSizeInGb * 1024 * 1024 * 1024)) {
            throw new InvalidParameterValueException("volume size " + size
                    + ", but the maximum size allowed is " + _maxVolumeSizeInGb
                    + " Gb.");
        }

        return true;
    }

    protected DiskProfile toDiskProfile(VolumeVO vol, DiskOfferingVO offering) {
        return new DiskProfile(vol.getId(), vol.getVolumeType(), vol.getName(),
                offering.getId(), vol.getSize(), offering.getTagsArray(),
                offering.getUseLocalStorage(), offering.isRecreatable(),
                vol.getTemplateId());
    }

    @Override
    public DiskProfile allocateRawVolume(Type type,
            String name, DiskOfferingVO offering, Long size, VMInstanceVO vm, Account owner) {
        if (size == null) {
            size = offering.getDiskSize();
        } else {
            size = (size * 1024 * 1024 * 1024);
        }
        VolumeVO vol = new VolumeVO(type, name, vm.getDataCenterId(),
                owner.getDomainId(), owner.getId(), offering.getId(), size);
        if (vm != null) {
            vol.setInstanceId(vm.getId());
        }

        if (type.equals(Type.ROOT)) {
            vol.setDeviceId(0l);
        } else {
            vol.setDeviceId(1l);
        }

        vol = _volsDao.persist(vol);

        // Save usage event and update resource count for user vm volumes
        if (vm instanceof UserVm) {

            UsageEventVO usageEvent = new UsageEventVO(
                    EventTypes.EVENT_VOLUME_CREATE, vol.getAccountId(),
                    vol.getDataCenterId(), vol.getId(), vol.getName(),
                    offering.getId(), null, size);
            _usageEventDao.persist(usageEvent);

            _resourceLimitMgr.incrementResourceCount(vm.getAccountId(),
                    ResourceType.volume);
        }
        return toDiskProfile(vol, offering);
    }

    @Override
    public  DiskProfile allocateTemplatedVolume(
            Type type, String name, DiskOfferingVO offering,
            VMTemplateVO template, VMInstanceVO vm, Account owner) {
        assert (template.getFormat() != ImageFormat.ISO) : "ISO is not a template really....";

        Long size = this._tmpltMgr.getTemplateSize(template.getId(), vm.getDataCenterId());

        VolumeVO vol = new VolumeVO(type, name, vm.getDataCenterId(),
                owner.getDomainId(), owner.getId(), offering.getId(), size);
        if (vm != null) {
            vol.setInstanceId(vm.getId());
        }
        vol.setTemplateId(template.getId());

        if (type.equals(Type.ROOT)) {
            vol.setDeviceId(0l);
            if (!vm.getType().equals(VirtualMachine.Type.User)) {
                vol.setRecreatable(true);
            }
        } else {
            vol.setDeviceId(1l);
        }

        vol = _volsDao.persist(vol);

        // Create event and update resource count for volumes if vm is a user vm
        if (vm instanceof UserVm) {

            Long offeringId = null;

            if (offering.getType() == DiskOfferingVO.Type.Disk) {
                offeringId = offering.getId();
            }

            UsageEventVO usageEvent = new UsageEventVO(
                    EventTypes.EVENT_VOLUME_CREATE, vol.getAccountId(),
                    vol.getDataCenterId(), vol.getId(), vol.getName(),
                    offeringId, template.getId(), vol.getSize());
            _usageEventDao.persist(usageEvent);

            _resourceLimitMgr.incrementResourceCount(vm.getAccountId(),
                    ResourceType.volume);
        }
        return toDiskProfile(vol, offering);
    }
 
    private String getSupportedImageFormatForCluster(Long clusterId) {
        ClusterVO cluster = ApiDBUtils.findClusterById(clusterId);

        if (cluster.getHypervisorType() == HypervisorType.XenServer) {
            return "vhd";
        } else if (cluster.getHypervisorType() == HypervisorType.KVM) {
            return "qcow2";
        } else if (cluster.getHypervisorType() == HypervisorType.VMware) {
            return "ova";
        } else if (cluster.getHypervisorType() == HypervisorType.Ovm) {
            return "raw";
        } else {
            return null;
        }
    }
    
    private VolumeInfo copyVolume(StoragePoolVO rootDiskPool
            , VolumeInfo volume, VMInstanceVO vm, VMTemplateVO rootDiskTmplt, DataCenterVO dcVO,
            HostPodVO pod, DiskOfferingVO diskVO, ServiceOfferingVO svo, HypervisorType rootDiskHyperType) throws NoTransitionException {
        VolumeHostVO volHostVO = _volumeHostDao.findByHostVolume(volume.getDataStore().getId(), volume.getId());
        if (!volHostVO
                .getFormat()
                .getFileExtension()
                .equals(
                        getSupportedImageFormatForCluster(rootDiskPool
                                .getClusterId()))) {
            throw new InvalidParameterValueException(
                    "Failed to attach volume to VM since volumes format "
                            + volHostVO.getFormat()
                            .getFileExtension()
                            + " is not compatible with the vm hypervisor type");
        }

        VolumeInfo volumeOnPrimary = copyVolumeFromSecToPrimary(volume,
                vm, rootDiskTmplt, dcVO, pod,
                rootDiskPool.getClusterId(), svo, diskVO,
                new ArrayList<StoragePool>(),
                volume.getSize(), rootDiskHyperType);

        return volumeOnPrimary;
    }

    private VolumeInfo createVolumeOnPrimaryStorage(VMInstanceVO vm, VolumeVO rootVolumeOfVm, VolumeInfo volume, HypervisorType rootDiskHyperType) throws NoTransitionException {
        VMTemplateVO rootDiskTmplt = _templateDao.findById(vm
                .getTemplateId());
        DataCenterVO dcVO = _dcDao.findById(vm
                .getDataCenterId());
        HostPodVO pod = _podDao.findById(vm.getPodIdToDeployIn());
        StoragePoolVO rootDiskPool = _storagePoolDao
                .findById(rootVolumeOfVm.getPoolId());
        ServiceOfferingVO svo = _serviceOfferingDao.findById(vm
                .getServiceOfferingId());
        DiskOfferingVO diskVO = _diskOfferingDao.findById(volume
                .getDiskOfferingId());
        Long clusterId = (rootDiskPool == null ? null : rootDiskPool
                .getClusterId());

        VolumeInfo vol = null;
        if (volume.getState() == Volume.State.Allocated) {
            vol = createVolume(volume, vm,
                    rootDiskTmplt, dcVO, pod, clusterId, svo, diskVO,
                    new ArrayList<StoragePool>(), volume.getSize(),
                    rootDiskHyperType);
        } else if (volume.getState() == Volume.State.Uploaded) {
            vol = copyVolume(rootDiskPool
                    , volume, vm, rootDiskTmplt,  dcVO,
                    pod,  diskVO,  svo,  rootDiskHyperType);
        }
        return vol;
    }

    private boolean needMoveVolume(VolumeVO rootVolumeOfVm, VolumeInfo volume) {
        StoragePoolVO vmRootVolumePool = _storagePoolDao
                .findById(rootVolumeOfVm.getPoolId());
        DiskOfferingVO volumeDiskOffering = _diskOfferingDao
                .findById(volume.getDiskOfferingId());
        String[] volumeTags = volumeDiskOffering.getTagsArray();

        boolean isVolumeOnSharedPool = !volumeDiskOffering
                .getUseLocalStorage();
        StoragePoolVO sourcePool = _storagePoolDao.findById(volume
                .getPoolId());
        List<StoragePoolVO> matchingVMPools = _storagePoolDao
                .findPoolsByTags(vmRootVolumePool.getDataCenterId(),
                        vmRootVolumePool.getPodId(),
                        vmRootVolumePool.getClusterId(), volumeTags
                        );
        
        boolean moveVolumeNeeded = true;
        if (matchingVMPools.size() == 0) {
            String poolType;
            if (vmRootVolumePool.getClusterId() != null) {
                poolType = "cluster";
            } else if (vmRootVolumePool.getPodId() != null) {
                poolType = "pod";
            } else {
                poolType = "zone";
            }
            throw new CloudRuntimeException(
                    "There are no storage pools in the VM's " + poolType
                    + " with all of the volume's tags ("
                    + volumeDiskOffering.getTags() + ").");
        } else {
            long sourcePoolId = sourcePool.getId();
            Long sourcePoolDcId = sourcePool.getDataCenterId();
            Long sourcePoolPodId = sourcePool.getPodId();
            Long sourcePoolClusterId = sourcePool.getClusterId();
            for (StoragePoolVO vmPool : matchingVMPools) {
                long vmPoolId = vmPool.getId();
                Long vmPoolDcId = vmPool.getDataCenterId();
                Long vmPoolPodId = vmPool.getPodId();
                Long vmPoolClusterId = vmPool.getClusterId();

                // Moving a volume is not required if storage pools belongs
                // to same cluster in case of shared volume or
                // identical storage pool in case of local
                if (sourcePoolDcId == vmPoolDcId
                        && sourcePoolPodId == vmPoolPodId
                        && sourcePoolClusterId == vmPoolClusterId
                        && (isVolumeOnSharedPool || sourcePoolId == vmPoolId)) {
                    moveVolumeNeeded = false;
                    break;
                }
            }
        }
        
        return moveVolumeNeeded;
    }
    
    
    private VolumeVO sendAttachVolumeCommand(UserVmVO vm, VolumeVO volume, Long deviceId) {
        String errorMsg = "Failed to attach volume: " + volume.getName()
                + " to VM: " + vm.getHostName();
        boolean sendCommand = (vm.getState() == State.Running);
        AttachVolumeAnswer answer = null;
        Long hostId = vm.getHostId();
        if (hostId == null) {
            hostId = vm.getLastHostId();
            HostVO host = _hostDao.findById(hostId);
            if (host != null
                    && host.getHypervisorType() == HypervisorType.VMware) {
                sendCommand = true;
            }
        }

        if (sendCommand) {
            StoragePoolVO volumePool = _storagePoolDao.findById(volume
                    .getPoolId());
            AttachVolumeCommand cmd = new AttachVolumeCommand(true,
                    vm.getInstanceName(), volume.getPoolType(),
                    volume.getFolder(), volume.getPath(), volume.getName(),
                    deviceId, volume.getChainInfo());
            cmd.setPoolUuid(volumePool.getUuid());

            try {
                answer = (AttachVolumeAnswer) _agentMgr.send(hostId, cmd);
            } catch (Exception e) {
                throw new CloudRuntimeException(errorMsg + " due to: "
                        + e.getMessage());
            }
        }

        if (!sendCommand || (answer != null && answer.getResult())) {
            // Mark the volume as attached
            if (sendCommand) {
                _volsDao.attachVolume(volume.getId(), vm.getId(),
                        answer.getDeviceId());
            } else {
                _volsDao.attachVolume(volume.getId(), vm.getId(), deviceId);
            }
            return _volsDao.findById(volume.getId());
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
    
    private int getMaxDataVolumesSupported(UserVmVO vm) {
        Long hostId = vm.getHostId();
        if (hostId == null) {
            hostId = vm.getLastHostId();
        }
        HostVO host = _hostDao.findById(hostId);
        Integer maxDataVolumesSupported = null;
        if (host != null) {
            _hostDao.loadDetails(host);
            maxDataVolumesSupported = _hypervisorCapabilitiesDao
                    .getMaxDataVolumesLimit(host.getHypervisorType(),
                            host.getDetail("product_version"));
        }
        if (maxDataVolumesSupported == null) {
            maxDataVolumesSupported = 6; // 6 data disks by default if nothing
            // is specified in
            // 'hypervisor_capabilities' table
        }

        return maxDataVolumesSupported.intValue();
    }
    
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VOLUME_ATTACH, eventDescription = "attaching volume", async = true)
    public Volume attachVolumeToVM(AttachVolumeCmd command) {
        Long vmId = command.getVirtualMachineId();
        Long volumeId = command.getId();
        Long deviceId = command.getDeviceId();
        Account caller = UserContext.current().getCaller();

        // Check that the volume ID is valid
        VolumeInfo volume = volFactory.getVolume(volumeId);
        // Check that the volume is a data volume
        if (volume == null || volume.getVolumeType() != Volume.Type.DATADISK) {
            throw new InvalidParameterValueException(
                    "Please specify a valid data volume.");
        }

        // Check that the volume is not currently attached to any VM
        if (volume.getInstanceId() != null) {
            throw new InvalidParameterValueException(
                    "Please specify a volume that is not attached to any VM.");
        }

        // Check that the volume is not destroyed
        if (volume.getState() == Volume.State.Destroy) {
            throw new InvalidParameterValueException(
                    "Please specify a volume that is not destroyed.");
        }

        // Check that the virtual machine ID is valid and it's a user vm
        UserVmVO vm = _userVmDao.findById(vmId);
        if (vm == null || vm.getType() != VirtualMachine.Type.User) {
            throw new InvalidParameterValueException(
                    "Please specify a valid User VM.");
        }

        // Check that the VM is in the correct state
        if (vm.getState() != State.Running && vm.getState() != State.Stopped) {
            throw new InvalidParameterValueException(
                    "Please specify a VM that is either running or stopped.");
        }

        // Check that the device ID is valid
        if (deviceId != null) {
            if (deviceId.longValue() == 0) {
                throw new InvalidParameterValueException(
                        "deviceId can't be 0, which is used by Root device");
            }
        }

        // Check that the number of data volumes attached to VM is less than
        // that supported by hypervisor
        List<VolumeVO> existingDataVolumes = _volsDao.findByInstanceAndType(
                vmId, Volume.Type.DATADISK);
        int maxDataVolumesSupported = getMaxDataVolumesSupported(vm);
        if (existingDataVolumes.size() >= maxDataVolumesSupported) {
            throw new InvalidParameterValueException(
                    "The specified VM already has the maximum number of data disks ("
                            + maxDataVolumesSupported
                            + "). Please specify another VM.");
        }

        // Check that the VM and the volume are in the same zone
        if (vm.getDataCenterId() != volume.getDataCenterId()) {
            throw new InvalidParameterValueException(
                    "Please specify a VM that is in the same zone as the volume.");
        }

        // If local storage is disabled then attaching a volume with local disk
        // offering not allowed
        DataCenterVO dataCenter = _dcDao.findById(volume.getDataCenterId());
        if (!dataCenter.isLocalStorageEnabled()) {
            DiskOfferingVO diskOffering = _diskOfferingDao.findById(volume
                    .getDiskOfferingId());
            if (diskOffering.getUseLocalStorage()) {
                throw new InvalidParameterValueException(
                        "Zone is not configured to use local storage but volume's disk offering "
                                + diskOffering.getName() + " uses it");
            }
        }

        // permission check
        _accountMgr.checkAccess(caller, null, true, volume, vm);

        if (!(Volume.State.Allocated.equals(volume.getState())
                || Volume.State.Ready.equals(volume.getState()) || Volume.State.Uploaded
                .equals(volume.getState()))) {
            throw new InvalidParameterValueException(
                    "Volume state must be in Allocated, Ready or in Uploaded state");
        }

        VolumeVO rootVolumeOfVm = null;
        List<VolumeVO> rootVolumesOfVm = _volsDao.findByInstanceAndType(vmId,
                Volume.Type.ROOT);
        if (rootVolumesOfVm.size() != 1) {
            throw new CloudRuntimeException(
                    "The VM "
                            + vm.getHostName()
                            + " has more than one ROOT volume and is in an invalid state.");
        } else {
            rootVolumeOfVm = rootVolumesOfVm.get(0);
        }

        HypervisorType rootDiskHyperType = vm.getHypervisorType();

        HypervisorType dataDiskHyperType = _volsDao.getHypervisorType(volume
                .getId());
        if (dataDiskHyperType != HypervisorType.None
                && rootDiskHyperType != dataDiskHyperType) {
            throw new InvalidParameterValueException(
                    "Can't attach a volume created by: " + dataDiskHyperType
                    + " to a " + rootDiskHyperType + " vm");
        }

       
        deviceId = getDeviceId(vmId, deviceId);
        VolumeInfo volumeOnPrimaryStorage = volume;
        if (volume.getState().equals(Volume.State.Allocated)
                || volume.getState() == Volume.State.Uploaded) {
            try {
                volumeOnPrimaryStorage = createVolumeOnPrimaryStorage(vm, rootVolumeOfVm, volume, rootDiskHyperType);
            } catch (NoTransitionException e) {
                s_logger.debug("Failed to create volume on primary storage", e);
                throw new CloudRuntimeException("Failed to create volume on primary storage", e);
            }
        }

        boolean moveVolumeNeeded = needMoveVolume(rootVolumeOfVm, volumeOnPrimaryStorage);

        if (moveVolumeNeeded) {
            PrimaryDataStoreInfo primaryStore = (PrimaryDataStoreInfo)volumeOnPrimaryStorage.getDataStore();
            if (primaryStore.isLocal()) {
                throw new CloudRuntimeException(
                        "Failed to attach local data volume "
                                + volume.getName()
                                + " to VM "
                                + vm.getDisplayName()
                                + " as migration of local data volume is not allowed");
            }
            StoragePoolVO vmRootVolumePool = _storagePoolDao
                    .findById(rootVolumeOfVm.getPoolId());

            try {
                volumeOnPrimaryStorage = moveVolume(volumeOnPrimaryStorage,
                        vmRootVolumePool.getDataCenterId(),
                        vmRootVolumePool.getPodId(),
                        vmRootVolumePool.getClusterId(),
                        dataDiskHyperType);
            } catch (ConcurrentOperationException e) {
                s_logger.debug("move volume failed", e);
                throw new CloudRuntimeException("move volume failed", e);
            }
        }


        AsyncJobExecutor asyncExecutor = BaseAsyncJobExecutor
                .getCurrentExecutor();
        if (asyncExecutor != null) {
            AsyncJobVO job = asyncExecutor.getJob();

            if (s_logger.isInfoEnabled()) {
                s_logger.info("Trying to attaching volume " + volumeId
                        + " to vm instance:" + vm.getId()
                        + ", update async job-" + job.getId()
                        + " progress status");
            }

            _asyncMgr.updateAsyncJobAttachment(job.getId(), "volume", volumeId);
            _asyncMgr.updateAsyncJobStatus(job.getId(),
                    BaseCmd.PROGRESS_INSTANCE_CREATED, volumeId);
        }

        VolumeVO newVol = _volumeDao.findById(volumeOnPrimaryStorage.getId());
        newVol = sendAttachVolumeCommand(vm, newVol, deviceId);
        return newVol;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VOLUME_DETACH, eventDescription = "detaching volume", async = true)
    public Volume detachVolumeFromVM(DetachVolumeCmd cmmd) {
        Account caller = UserContext.current().getCaller();
        if ((cmmd.getId() == null && cmmd.getDeviceId() == null && cmmd
                .getVirtualMachineId() == null)
                || (cmmd.getId() != null && (cmmd.getDeviceId() != null || cmmd
                .getVirtualMachineId() != null))
                || (cmmd.getId() == null && (cmmd.getDeviceId() == null || cmmd
                .getVirtualMachineId() == null))) {
            throw new InvalidParameterValueException(
                    "Please provide either a volume id, or a tuple(device id, instance id)");
        }

        Long volumeId = cmmd.getId();
        VolumeVO volume = null;

        if (volumeId != null) {
            volume = _volsDao.findById(volumeId);
        } else {
            volume = _volsDao.findByInstanceAndDeviceId(
                    cmmd.getVirtualMachineId(), cmmd.getDeviceId()).get(0);
        }

        Long vmId = null;

        if (cmmd.getVirtualMachineId() == null) {
            vmId = volume.getInstanceId();
        } else {
            vmId = cmmd.getVirtualMachineId();
        }

        // Check that the volume ID is valid
        if (volume == null) {
            throw new InvalidParameterValueException(
                    "Unable to find volume with ID: " + volumeId);
        }

        // Permissions check
        _accountMgr.checkAccess(caller, null, true, volume);

        // Check that the volume is a data volume
        if (volume.getVolumeType() != Volume.Type.DATADISK) {
            throw new InvalidParameterValueException(
                    "Please specify a data volume.");
        }

        // Check that the volume is currently attached to a VM
        if (vmId == null) {
            throw new InvalidParameterValueException(
                    "The specified volume is not attached to a VM.");
        }

        // Check that the VM is in the correct state
        UserVmVO vm = this._userVmDao.findById(vmId);
        if (vm.getState() != State.Running && vm.getState() != State.Stopped
                && vm.getState() != State.Destroyed) {
            throw new InvalidParameterValueException(
                    "Please specify a VM that is either running or stopped.");
        }

        AsyncJobExecutor asyncExecutor = BaseAsyncJobExecutor
                .getCurrentExecutor();
        if (asyncExecutor != null) {
            AsyncJobVO job = asyncExecutor.getJob();

            if (s_logger.isInfoEnabled()) {
                s_logger.info("Trying to attaching volume " + volumeId
                        + "to vm instance:" + vm.getId()
                        + ", update async job-" + job.getId()
                        + " progress status");
            }

            _asyncMgr.updateAsyncJobAttachment(job.getId(), "volume", volumeId);
            _asyncMgr.updateAsyncJobStatus(job.getId(),
                    BaseCmd.PROGRESS_INSTANCE_CREATED, volumeId);
        }

        String errorMsg = "Failed to detach volume: " + volume.getName()
                + " from VM: " + vm.getHostName();
        boolean sendCommand = (vm.getState() == State.Running);
        Answer answer = null;

        if (sendCommand) {
            AttachVolumeCommand cmd = new AttachVolumeCommand(false,
                    vm.getInstanceName(), volume.getPoolType(),
                    volume.getFolder(), volume.getPath(), volume.getName(),
                    cmmd.getDeviceId() != null ? cmmd.getDeviceId() : volume
                            .getDeviceId(), volume.getChainInfo());

            StoragePoolVO volumePool = _storagePoolDao.findById(volume
                    .getPoolId());
            cmd.setPoolUuid(volumePool.getUuid());

            try {
                answer = _agentMgr.send(vm.getHostId(), cmd);
            } catch (Exception e) {
                throw new CloudRuntimeException(errorMsg + " due to: "
                        + e.getMessage());
            }
        }

        if (!sendCommand || (answer != null && answer.getResult())) {
            // Mark the volume as detached
            _volsDao.detachVolume(volume.getId());
            if (answer != null && answer instanceof AttachVolumeAnswer) {
                volume.setChainInfo(((AttachVolumeAnswer) answer)
                        .getChainInfo());
                _volsDao.update(volume.getId(), volume);
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

    

    
    

    @DB
    protected VolumeVO switchVolume(VolumeVO existingVolume,
            VirtualMachineProfile<? extends VirtualMachine> vm)
            throws StorageUnavailableException {
        Transaction txn = Transaction.currentTxn();

        Long templateIdToUse = null;
        Long volTemplateId = existingVolume.getTemplateId();
        long vmTemplateId = vm.getTemplateId();
        if (volTemplateId != null && volTemplateId.longValue() != vmTemplateId) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("switchVolume: Old Volume's templateId: "
                        + volTemplateId
                        + " does not match the VM's templateId: "
                        + vmTemplateId
                        + ", updating templateId in the new Volume");
            }
            templateIdToUse = vmTemplateId;
        }

        txn.start();
        VolumeVO newVolume = allocateDuplicateVolume(existingVolume,
                templateIdToUse);
        // In case of Vmware if vm reference is not removed then during root
        // disk cleanup
        // the vm also gets deleted, so remove the reference
        if (vm.getHypervisorType() == HypervisorType.VMware) {
            _volsDao.detachVolume(existingVolume.getId());
        }
        try {
            stateTransitTo(existingVolume, Volume.Event.DestroyRequested);
        } catch (NoTransitionException e) {
            s_logger.debug("Unable to destroy existing volume: " + e.toString());
        }
        txn.commit();
        return newVolume;

    }

    
    @Override
    public void release(VirtualMachineProfile<? extends VMInstanceVO> profile) {
        // add code here
    }

   
    @Override
    @DB
    public void cleanupVolumes(long vmId) throws ConcurrentOperationException {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Cleaning storage for vm: " + vmId);
        }
        List<VolumeVO> volumesForVm = _volsDao.findByInstance(vmId);
        List<VolumeVO> toBeExpunged = new ArrayList<VolumeVO>();
        Transaction txn = Transaction.currentTxn();
        txn.start();
        for (VolumeVO vol : volumesForVm) {
            if (vol.getVolumeType().equals(Type.ROOT)) {
                // This check is for VM in Error state (volume is already
                // destroyed)
                if (!vol.getState().equals(Volume.State.Destroy)) {
                    this.volService.destroyVolume(vol.getId());
                }
                toBeExpunged.add(vol);
            } else {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Detaching " + vol);
                }
                _volsDao.detachVolume(vol.getId());
            }
        }
        txn.commit();
        AsyncCallFuture<VolumeApiResult> future = null;
        for (VolumeVO expunge : toBeExpunged) {
            future = this.volService.expungeVolumeAsync(this.volFactory.getVolume(expunge.getId()));
            try {
                future.get();
            } catch (InterruptedException e) {
                s_logger.debug("failed expunge volume" + expunge.getId(), e);
            } catch (ExecutionException e) {
                s_logger.debug("failed expunge volume" + expunge.getId(), e);
            }
        }
    }

    @DB
    @Override
    public Volume migrateVolume(MigrateVolumeCmd cmd) {
        Long volumeId = cmd.getVolumeId();
        Long storagePoolId = cmd.getStoragePoolId();
        
        VolumeVO vol = _volsDao.findById(volumeId);
        if (vol == null) {
            throw new InvalidParameterValueException(
                    "Failed to find the volume id: " + volumeId);
        }

        if (vol.getState() != Volume.State.Ready) {
            throw new InvalidParameterValueException(
                    "Volume must be in ready state");
        }

        if (vol.getInstanceId() != null) {
            throw new InvalidParameterValueException(
                    "Volume needs to be dettached from VM");
        }

        StoragePool destPool = (StoragePool)this.dataStoreMgr.getDataStore(storagePoolId, DataStoreRole.Primary);
        if (destPool == null) {
            throw new InvalidParameterValueException(
                    "Failed to find the destination storage pool: "
                            + storagePoolId);
        }

        if (!volumeOnSharedStoragePool(vol)) {
            throw new InvalidParameterValueException(
                    "Migration of volume from local storage pool is not supported");
        }

        Volume newVol = migrateVolume(vol, destPool);
        return newVol;
    }

    
    
    @DB
    protected Volume migrateVolume(Volume volume, StoragePool destPool) {
        VolumeInfo vol = this.volFactory.getVolume(volume.getId());
        AsyncCallFuture<VolumeApiResult> future = this.volService.copyVolume(vol, (DataStore)destPool);
        try {
            VolumeApiResult result = future.get();
            if (result.isFailed()) {
                s_logger.debug("migrate volume failed:" + result.getResult());
                return null;
            }
            return result.getVolume();
        } catch (InterruptedException e) {
            s_logger.debug("migrate volume failed", e);
            return null;
        } catch (ExecutionException e) {
            s_logger.debug("migrate volume failed", e);
            return null;
        }
    }

    @Override
    public boolean storageMigration(
            VirtualMachineProfile<? extends VirtualMachine> vm,
            StoragePool destPool) {
        List<VolumeVO> vols = _volsDao.findUsableVolumesForInstance(vm.getId());
        List<Volume> volumesNeedToMigrate = new ArrayList<Volume>();

        for (VolumeVO volume : vols) {
            if (volume.getState() != Volume.State.Ready) {
                s_logger.debug("volume: " + volume.getId() + " is in "
                        + volume.getState() + " state");
                throw new CloudRuntimeException("volume: " + volume.getId()
                        + " is in " + volume.getState() + " state");
            }

            if (volume.getPoolId() == destPool.getId()) {
                s_logger.debug("volume: " + volume.getId()
                        + " is on the same storage pool: " + destPool.getId());
                continue;
            }

            volumesNeedToMigrate.add(volume);
        }

        if (volumesNeedToMigrate.isEmpty()) {
            s_logger.debug("No volume need to be migrated");
            return true;
        }

        for (Volume vol : volumesNeedToMigrate) {
            Volume result = migrateVolume(vol, destPool);
            if (result == null) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public void prepareForMigration(
            VirtualMachineProfile<? extends VirtualMachine> vm,
            DeployDestination dest) {
        List<VolumeVO> vols = _volsDao.findUsableVolumesForInstance(vm.getId());
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Preparing " + vols.size() + " volumes for " + vm);
        }

        for (VolumeVO vol : vols) {
            PrimaryDataStoreInfo pool = (PrimaryDataStoreInfo)this.dataStoreMgr.getDataStore(vol.getPoolId(), DataStoreRole.Primary);
            vm.addDisk(new VolumeTO(vol, pool));
        }

        if (vm.getType() == VirtualMachine.Type.User) {
            UserVmVO userVM = (UserVmVO) vm.getVirtualMachine();
            if (userVM.getIsoId() != null) {
                Pair<String, String> isoPathPair = this._tmpltMgr.getAbsoluteIsoPath(
                        userVM.getIsoId(), userVM.getDataCenterId());
                if (isoPathPair != null) {
                    String isoPath = isoPathPair.first();
                    VolumeTO iso = new VolumeTO(vm.getId(), Volume.Type.ISO,
                            StoragePoolType.ISO, null, null, null, isoPath, 0,
                            null, null);
                    vm.addDisk(iso);
                }
            }
        }
    }

   

    private static enum VolumeTaskType {
        RECREATE,
        NOP,
        MIGRATE
    }
    private static class VolumeTask {
         final VolumeTaskType type;
         final StoragePoolVO pool;
         final VolumeVO volume;
         VolumeTask(VolumeTaskType type, VolumeVO volume, StoragePoolVO pool) {
             this.type = type;
             this.pool = pool;
             this.volume = volume;
         }
    }
    
    private List<VolumeTask> getTasks(List<VolumeVO> vols, Map<Volume, StoragePool> destVols) throws StorageUnavailableException {
        boolean recreate = _recreateSystemVmEnabled;
        List<VolumeTask> tasks = new ArrayList<VolumeTask>();
        for (VolumeVO vol : vols) {
            StoragePoolVO assignedPool = null;
            if (destVols != null) {
                StoragePool pool = destVols.get(vol);
                if (pool != null) {
                    assignedPool = _storagePoolDao.findById(pool.getId());
                }
            }
            if (assignedPool == null && recreate) {
                assignedPool = _storagePoolDao.findById(vol.getPoolId());
            }
            if (assignedPool != null || recreate) {
                Volume.State state = vol.getState();
                if (state == Volume.State.Allocated
                        || state == Volume.State.Creating) {
                    VolumeTask task = new VolumeTask(VolumeTaskType.RECREATE, vol, null);
                    tasks.add(task);
                } else {
                    if (vol.isRecreatable()) {
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Volume " + vol
                                    + " will be recreated on storage pool "
                                    + assignedPool
                                    + " assigned by deploymentPlanner");
                        }
                        VolumeTask task = new VolumeTask(VolumeTaskType.RECREATE, vol, null);
                        tasks.add(task);
                    } else {
                        if (assignedPool.getId() != vol.getPoolId()) {
                            if (s_logger.isDebugEnabled()) {
                                s_logger.debug("Mismatch in storage pool "
                                        + assignedPool
                                        + " assigned by deploymentPlanner and the one associated with volume "
                                        + vol);
                            }
                            DiskOfferingVO diskOffering = _diskOfferingDao
                                    .findById(vol.getDiskOfferingId());
                            if (diskOffering.getUseLocalStorage()) {
                                if (s_logger.isDebugEnabled()) {
                                    s_logger.debug("Local volume "
                                            + vol
                                            + " will be recreated on storage pool "
                                            + assignedPool
                                            + " assigned by deploymentPlanner");
                                }
                                VolumeTask task = new VolumeTask(VolumeTaskType.RECREATE, vol, null);
                                tasks.add(task);
                            } else {
                                if (s_logger.isDebugEnabled()) {
                                    s_logger.debug("Shared volume "
                                            + vol
                                            + " will be migrated on storage pool "
                                            + assignedPool
                                            + " assigned by deploymentPlanner");
                                }
                                VolumeTask task = new VolumeTask(VolumeTaskType.MIGRATE, vol, null);
                                tasks.add(task);
                            }
                        } else {
                            StoragePoolVO pool = _storagePoolDao
                                    .findById(vol.getPoolId());
                            VolumeTask task = new VolumeTask(VolumeTaskType.NOP, vol, pool);
                            tasks.add(task);
                        }

                    }
                }
            } else {
                if (vol.getPoolId() == null) {
                    throw new StorageUnavailableException(
                            "Volume has no pool associate and also no storage pool assigned in DeployDestination, Unable to create "
                                    + vol, Volume.class, vol.getId());
                }
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("No need to recreate the volume: " + vol
                            + ", since it already has a pool assigned: "
                            + vol.getPoolId() + ", adding disk to VM");
                }
                StoragePoolVO pool = _storagePoolDao.findById(vol
                        .getPoolId());
                VolumeTask task = new VolumeTask(VolumeTaskType.NOP, vol, pool);
                tasks.add(task);
            }
        }
        
        return tasks;
    }
    
    private Pair<VolumeVO, DataStore> recreateVolume(VolumeVO vol, VirtualMachineProfile<? extends VirtualMachine> vm, 
            DeployDestination dest) throws StorageUnavailableException {
        VolumeVO newVol;
        boolean recreate = _recreateSystemVmEnabled;
        DataStore destPool = null;
        if (recreate
                && (dest.getStorageForDisks() == null || dest
                        .getStorageForDisks().get(vol) == null)) {
            destPool = dataStoreMgr.getDataStore(vol.getPoolId(), DataStoreRole.Primary);
            s_logger.debug("existing pool: " + destPool.getId());
        } else {
            StoragePool pool = dest.getStorageForDisks().get(vol);
            destPool = dataStoreMgr.getDataStore(pool.getId(), DataStoreRole.Primary);
        }

        if (vol.getState() == Volume.State.Allocated
                || vol.getState() == Volume.State.Creating) {
            newVol = vol;
        } else {
            newVol = switchVolume(vol, vm);
            // update the volume->PrimaryDataStoreVO map since volumeId has
            // changed
            if (dest.getStorageForDisks() != null
                    && dest.getStorageForDisks().containsKey(vol)) {
                StoragePool poolWithOldVol = dest
                        .getStorageForDisks().get(vol);
                dest.getStorageForDisks().put(newVol, poolWithOldVol);
                dest.getStorageForDisks().remove(vol);
            }
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Created new volume " + newVol
                        + " for old volume " + vol);
            }
        }
        VolumeInfo volume = volFactory.getVolume(newVol.getId(), destPool);
        Long templateId = newVol.getTemplateId();
        AsyncCallFuture<VolumeApiResult> future = null;
        if (templateId == null) {
            future = this.volService.createVolumeAsync(volume, destPool);
        } else {
            TemplateInfo templ = this.tmplFactory.getTemplate(templateId);
            future = this.volService.createVolumeFromTemplateAsync(volume, destPool.getId(), templ);
        }
        VolumeApiResult result = null;
        try {
            result = future.get();
            if (result.isFailed()) {
                s_logger.debug("Unable to create "
                        + newVol + ":" + result.getResult());
                throw new StorageUnavailableException("Unable to create "
                        + newVol + ":" + result.getResult(), destPool.getId());
            }
            newVol = this._volsDao.findById(newVol.getId());
        } catch (InterruptedException e) {
            s_logger.error("Unable to create " + newVol, e);
            throw new StorageUnavailableException("Unable to create "
                    + newVol + ":" + e.toString(), destPool.getId());
        } catch (ExecutionException e) {
            s_logger.error("Unable to create " + newVol, e);
            throw new StorageUnavailableException("Unable to create "
                    + newVol + ":" + e.toString(), destPool.getId());
        }
        
        return new Pair<VolumeVO, DataStore>(newVol, destPool);
    }
    
    @Override
    public void prepare(VirtualMachineProfile<? extends VirtualMachine> vm,
            DeployDestination dest) throws StorageUnavailableException,
            InsufficientStorageCapacityException, ConcurrentOperationException {
        
        if (dest == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("DeployDestination cannot be null, cannot prepare Volumes for the vm: "
                        + vm);
            }
            throw new CloudRuntimeException(
                    "Unable to prepare Volume for vm because DeployDestination is null, vm:"
                            + vm);
        }
        List<VolumeVO> vols = _volsDao.findUsableVolumesForInstance(vm.getId());
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Checking if we need to prepare " + vols.size()
                    + " volumes for " + vm);
        }

        List<VolumeTask> tasks = getTasks(vols, dest.getStorageForDisks());
        Volume vol = null;
        StoragePool pool = null;
        for (VolumeTask task : tasks) {
            if (task.type == VolumeTaskType.NOP) {
                pool = (StoragePool)dataStoreMgr.getDataStore(task.pool.getId(), DataStoreRole.Primary);
                vol = task.volume;
            } else if (task.type == VolumeTaskType.MIGRATE) {
                pool = (StoragePool)dataStoreMgr.getDataStore(task.pool.getId(), DataStoreRole.Primary);
                migrateVolume(task.volume, pool);
                vol = task.volume;
            } else if (task.type == VolumeTaskType.RECREATE) {
                Pair<VolumeVO, DataStore> result = recreateVolume(task.volume, vm, dest);
                pool = (StoragePool)dataStoreMgr.getDataStore(result.second().getId(), DataStoreRole.Primary);
                vol = result.first();
            }
            vm.addDisk(new VolumeTO(vol, pool));
        }
    }
    
    private Long getDeviceId(long vmId, Long deviceId) {
        // allocate deviceId
        List<VolumeVO> vols = _volsDao.findByInstance(vmId);
        if (deviceId != null) {
            if (deviceId.longValue() > 15 || deviceId.longValue() == 0
                    || deviceId.longValue() == 3) {
                throw new RuntimeException("deviceId should be 1,2,4-15");
            }
            for (VolumeVO vol : vols) {
                if (vol.getDeviceId().equals(deviceId)) {
                    throw new RuntimeException("deviceId " + deviceId
                            + " is used by vm" + vmId);
                }
            }
        } else {
            // allocate deviceId here
            List<String> devIds = new ArrayList<String>();
            for (int i = 1; i < 15; i++) {
                devIds.add(String.valueOf(i));
            }
            devIds.remove("3");
            for (VolumeVO vol : vols) {
                devIds.remove(vol.getDeviceId().toString().trim());
            }
            deviceId = Long.parseLong(devIds.iterator().next());
        }
        
        return deviceId;
    }
 
    private boolean stateTransitTo(Volume vol, Volume.Event event)
            throws NoTransitionException {
        return _volStateMachine.transitTo(vol, event, null, _volsDao);
    }

    
    private String validateUrl(String url) {
        try {
            URI uri = new URI(url);
            if ((uri.getScheme() == null)
                    || (!uri.getScheme().equalsIgnoreCase("http")
                            && !uri.getScheme().equalsIgnoreCase("https") && !uri
                            .getScheme().equalsIgnoreCase("file"))) {
                throw new IllegalArgumentException(
                        "Unsupported scheme for url: " + url);
            }

            int port = uri.getPort();
            if (!(port == 80 || port == 443 || port == -1)) {
                throw new IllegalArgumentException(
                        "Only ports 80 and 443 are allowed");
            }
            String host = uri.getHost();
            try {
                InetAddress hostAddr = InetAddress.getByName(host);
                if (hostAddr.isAnyLocalAddress()
                        || hostAddr.isLinkLocalAddress()
                        || hostAddr.isLoopbackAddress()
                        || hostAddr.isMulticastAddress()) {
                    throw new IllegalArgumentException(
                            "Illegal host specified in url");
                }
                if (hostAddr instanceof Inet6Address) {
                    throw new IllegalArgumentException(
                            "IPV6 addresses not supported ("
                                    + hostAddr.getHostAddress() + ")");
                }
            } catch (UnknownHostException uhe) {
                throw new IllegalArgumentException("Unable to resolve " + host);
            }

            return uri.toString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL " + url);
        }

    }
    
    @Override
    public boolean canVmRestartOnAnotherServer(long vmId) {
        List<VolumeVO> vols = _volsDao.findCreatedByInstance(vmId);
        for (VolumeVO vol : vols) {
            if (!vol.isRecreatable() && !vol.getPoolType().isShared()) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public boolean configure(String name, Map<String, Object> params)
            throws ConfigurationException {
        String _customDiskOfferingMinSizeStr = _configDao
                .getValue(Config.CustomDiskOfferingMinSize.toString());
        _customDiskOfferingMinSize = NumbersUtil.parseInt(
                _customDiskOfferingMinSizeStr, Integer
                        .parseInt(Config.CustomDiskOfferingMinSize
                                .getDefaultValue()));

        String maxVolumeSizeInGbString = _configDao
                .getValue("storage.max.volume.size");
        _maxVolumeSizeInGb = NumbersUtil.parseLong(maxVolumeSizeInGbString,
                2000);

        String value = _configDao.getValue(Config.RecreateSystemVmEnabled.key());
        _recreateSystemVmEnabled = Boolean.parseBoolean(value);
        _copyvolumewait = NumbersUtil.parseInt(value,
                Integer.parseInt(Config.CopyVolumeWait.getDefaultValue()));

        HostTemplateStatesSearch = _vmTemplateHostDao.createSearchBuilder();
        HostTemplateStatesSearch.and("id", HostTemplateStatesSearch.entity()
                .getTemplateId(), SearchCriteria.Op.EQ);
        HostTemplateStatesSearch.and("state", HostTemplateStatesSearch.entity()
                .getDownloadState(), SearchCriteria.Op.EQ);

        SearchBuilder<HostVO> HostSearch = _hostDao.createSearchBuilder();
        HostSearch.and("dcId", HostSearch.entity().getDataCenterId(),
                SearchCriteria.Op.EQ);

        HostTemplateStatesSearch.join("host", HostSearch, HostSearch.entity()
                .getId(), HostTemplateStatesSearch.entity().getHostId(),
                JoinBuilder.JoinType.INNER);
        HostSearch.done();
        HostTemplateStatesSearch.done();
        return true;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public String getName() {
        return "Volume Manager";
    }
    
    @Override
    public void destroyVolume(VolumeVO volume) {
        try {
            this.volService.destroyVolume(volume.getId());
        } catch (ConcurrentOperationException e) {
            s_logger.debug("Failed to destroy volume" + volume.getId(), e);
            throw new CloudRuntimeException("Failed to destroy volume" + volume.getId(), e);
        }
    }

}
