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
package com.cloud.template;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.BaseListTemplateOrIsoPermissionsCmd;
import org.apache.cloudstack.api.BaseUpdateTemplateOrIsoCmd;
import org.apache.cloudstack.api.BaseUpdateTemplateOrIsoPermissionsCmd;
import org.apache.cloudstack.api.command.user.iso.DeleteIsoCmd;
import org.apache.cloudstack.api.command.user.iso.ExtractIsoCmd;
import org.apache.cloudstack.api.command.user.iso.ListIsoPermissionsCmd;
import org.apache.cloudstack.api.command.user.iso.RegisterIsoCmd;
import org.apache.cloudstack.api.command.user.iso.UpdateIsoCmd;
import org.apache.cloudstack.api.command.user.iso.UpdateIsoPermissionsCmd;
import org.apache.cloudstack.api.command.user.template.CopyTemplateCmd;
import org.apache.cloudstack.api.command.user.template.CreateTemplateCmd;
import org.apache.cloudstack.api.command.user.template.DeleteTemplateCmd;
import org.apache.cloudstack.api.command.user.template.ExtractTemplateCmd;
import org.apache.cloudstack.api.command.user.template.ListTemplatePermissionsCmd;
import org.apache.cloudstack.api.command.user.template.RegisterTemplateCmd;
import org.apache.cloudstack.api.command.user.template.UpdateTemplateCmd;
import org.apache.cloudstack.api.command.user.template.UpdateTemplatePermissionsCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.Scope;
import org.apache.cloudstack.engine.subsystem.api.storage.StorageCacheManager;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateService;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateService.TemplateApiResult;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.storage.command.AttachCommand;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.command.DettachCommand;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.cloudstack.storage.image.datastore.ImageStoreEntity;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.ComputeChecksumCommand;
import com.cloud.agent.api.storage.DestroyCommand;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.query.dao.UserVmJoinDao;
import com.cloud.api.query.vo.UserVmJoinVO;
import com.cloud.async.AsyncJobManager;
import com.cloud.configuration.Config;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.event.UsageEventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.HypervisorGuruManager;
import com.cloud.projects.Project;
import com.cloud.projects.ProjectManager;
import com.cloud.resource.ResourceManager;
import com.cloud.server.ConfigurationServer;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.LaunchPermissionVO;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ScopeType;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.StoragePoolStatus;
import com.cloud.storage.TemplateProfile;
import com.cloud.storage.Upload;
import com.cloud.storage.VMTemplateZoneVO;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeManager;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.LaunchPermissionDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.UploadDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateDetailsDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VMTemplateS3Dao;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.download.DownloadMonitor;
import com.cloud.storage.secondary.SecondaryStorageVmManager;
import com.cloud.storage.upload.UploadMonitor;
import com.cloud.template.TemplateAdapter.TemplateAdapterType;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountService;
import com.cloud.user.AccountVO;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserAccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.EnumUtils;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.*;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

@Component
@Local(value = { TemplateManager.class, TemplateApiService.class })
public class TemplateManagerImpl extends ManagerBase implements TemplateManager, TemplateApiService {
    private final static Logger s_logger = Logger.getLogger(TemplateManagerImpl.class);
    @Inject
    VMTemplateDao _tmpltDao;
    @Inject
    TemplateDataStoreDao _tmplStoreDao;
    @Inject
    VMTemplatePoolDao _tmpltPoolDao;
    @Inject
    VMTemplateZoneDao _tmpltZoneDao;
    @Inject
    protected VMTemplateDetailsDao _templateDetailsDao;
    @Inject
    VMInstanceDao _vmInstanceDao;
    @Inject
    PrimaryDataStoreDao _poolDao;
    @Inject
    StoragePoolHostDao _poolHostDao;
    @Inject
    EventDao _eventDao;
    @Inject
    DownloadMonitor _downloadMonitor;
    @Inject
    UploadMonitor _uploadMonitor;
    @Inject
    UserAccountDao _userAccountDao;
    @Inject
    AccountDao _accountDao;
    @Inject
    UserDao _userDao;
    @Inject
    AgentManager _agentMgr;
    @Inject
    AccountManager _accountMgr;
    @Inject
    HostDao _hostDao;
    @Inject
    DataCenterDao _dcDao;
    @Inject
    UserVmDao _userVmDao;
    @Inject
    VolumeDao _volumeDao;
    @Inject
    SnapshotDao _snapshotDao;
    @Inject
    VMTemplateS3Dao _vmS3TemplateDao;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    ClusterDao _clusterDao;
    @Inject
    DomainDao _domainDao;
    @Inject
    UploadDao _uploadDao;
    @Inject
    protected GuestOSDao _guestOSDao;
    @Inject
    StorageManager _storageMgr;
    @Inject
    AsyncJobManager _asyncMgr;
    @Inject
    UserVmManager _vmMgr;
    @Inject
    UsageEventDao _usageEventDao;
    @Inject
    HypervisorGuruManager _hvGuruMgr;
    @Inject
    AccountService _accountService;
    @Inject
    ResourceLimitService _resourceLimitMgr;
    @Inject
    SecondaryStorageVmManager _ssvmMgr;
    @Inject
    LaunchPermissionDao _launchPermissionDao;
    @Inject
    ProjectManager _projectMgr;
    @Inject
    VolumeDataFactory _volFactory;
    @Inject
    TemplateDataFactory _tmplFactory;
    @Inject
    SnapshotDataFactory _snapshotFactory;
    @Inject
    TemplateService _tmpltSvr;
    @Inject
    DataStoreManager _dataStoreMgr;
    @Inject
    protected ResourceManager _resourceMgr;
    @Inject
    VolumeManager _volumeMgr;
    @Inject
    ImageStoreDao _imageStoreDao;
    @Inject
    EndPointSelector _epSelector;
    @Inject
    UserVmJoinDao _userVmJoinDao;

    @Inject
    ConfigurationServer _configServer;

    int _primaryStorageDownloadWait;
    int _storagePoolMaxWaitSeconds = 3600;
    boolean _disableExtraction = false;
    ExecutorService _preloadExecutor;

    @Inject
    protected List<TemplateAdapter> _adapters;

    @Inject
    StorageCacheManager cacheMgr;
    @Inject
    EndPointSelector selector;

    private TemplateAdapter getAdapter(HypervisorType type) {
        TemplateAdapter adapter = null;
        if (type == HypervisorType.BareMetal) {
            adapter = AdapterBase.getAdapterByName(_adapters, TemplateAdapterType.BareMetal.getName());
        } else {
            // see HypervisorTemplateAdapter
            adapter = AdapterBase.getAdapterByName(_adapters, TemplateAdapterType.Hypervisor.getName());
        }

        if (adapter == null) {
            throw new CloudRuntimeException("Cannot find template adapter for " + type.toString());
        }

        return adapter;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ISO_CREATE, eventDescription = "creating iso")
    public VirtualMachineTemplate registerIso(RegisterIsoCmd cmd) throws ResourceAllocationException {
        TemplateAdapter adapter = getAdapter(HypervisorType.None);
        TemplateProfile profile = adapter.prepare(cmd);
        VMTemplateVO template = adapter.create(profile);

        if (template != null) {
            return template;
        } else {
            throw new CloudRuntimeException("Failed to create ISO");
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_TEMPLATE_CREATE, eventDescription = "creating template")
    public VirtualMachineTemplate registerTemplate(RegisterTemplateCmd cmd) throws URISyntaxException, ResourceAllocationException {
        Account account = CallContext.current().getCallingAccount();
        if (cmd.getTemplateTag() != null) {
            if (!_accountService.isRootAdmin(account.getType())) {
                throw new PermissionDeniedException("Parameter templatetag can only be specified by a Root Admin, permission denied");
            }
        }
        if(cmd.isRoutingType() != null){
            if(!_accountService.isRootAdmin(account.getType())){
                throw new PermissionDeniedException("Parameter isrouting can only be specified by a Root Admin, permission denied");
            }
        }

        TemplateAdapter adapter = getAdapter(HypervisorType.getType(cmd.getHypervisor()));
        TemplateProfile profile = adapter.prepare(cmd);
        VMTemplateVO template = adapter.create(profile);

        if (template != null) {
            return template;
        } else {
            throw new CloudRuntimeException("Failed to create a template");
        }
    }

    @Override
    public DataStore getImageStore(String storeUuid, Long zoneId) {
        DataStore imageStore = null;
        if (storeUuid != null) {
            imageStore = this._dataStoreMgr.getDataStore(storeUuid, DataStoreRole.Image);
        } else {
            List<DataStore> stores = this._dataStoreMgr.getImageStoresByScope(new ZoneScope(zoneId));
            if (stores.size() > 1) {
                throw new CloudRuntimeException("multiple image stores, don't know which one to use");
            }
            imageStore = stores.get(0);
        }

        return imageStore;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ISO_EXTRACT, eventDescription = "extracting ISO", async = true)
    public String extract(ExtractIsoCmd cmd) {
        Account account = CallContext.current().getCallingAccount();
        Long templateId = cmd.getId();
        Long zoneId = cmd.getZoneId();
        String url = cmd.getUrl();
        String mode = cmd.getMode();
        Long eventId = cmd.getStartEventId();

        return extract(account, templateId, url, zoneId, mode, eventId, true);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_TEMPLATE_EXTRACT, eventDescription = "extracting template", async = true)
    public String extract(ExtractTemplateCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        Long templateId = cmd.getId();
        Long zoneId = cmd.getZoneId();
        String url = cmd.getUrl();
        String mode = cmd.getMode();
        Long eventId = cmd.getStartEventId();

        VirtualMachineTemplate template = getTemplate(templateId);
        if (template == null) {
            throw new InvalidParameterValueException("unable to find template with id " + templateId);
        }
        TemplateAdapter adapter = getAdapter(template.getHypervisorType());
        TemplateProfile profile = adapter.prepareExtractTemplate(cmd);

        return extract(caller, templateId, url, zoneId, mode, eventId, false);
    }

    @Override
    public VirtualMachineTemplate prepareTemplate(long templateId, long zoneId) {

        VMTemplateVO vmTemplate = _tmpltDao.findById(templateId);
        if (vmTemplate == null) {
            throw new InvalidParameterValueException("Unable to find template id=" + templateId);
        }

        _accountMgr.checkAccess(CallContext.current().getCallingAccount(), AccessType.ModifyEntry, true, vmTemplate);

        prepareTemplateInAllStoragePools(vmTemplate, zoneId);
        return vmTemplate;
    }

    private String extract(Account caller, Long templateId, String url, Long zoneId, String mode, Long eventId, boolean isISO) {
        String desc = Upload.Type.TEMPLATE.toString();
        if (isISO) {
            desc = Upload.Type.ISO.toString();
        }
        eventId = eventId == null ? 0 : eventId;

        if (!_accountMgr.isRootAdmin(caller.getType()) && _disableExtraction) {
            throw new PermissionDeniedException("Extraction has been disabled by admin");
        }

        VMTemplateVO template = _tmpltDao.findById(templateId);
        if (template == null || template.getRemoved() != null) {
            throw new InvalidParameterValueException("Unable to find " + desc + " with id " + templateId);
        }

        if (template.getTemplateType() == Storage.TemplateType.SYSTEM) {
            throw new InvalidParameterValueException("Unable to extract the " + desc + " " + template.getName()
                    + " as it is a default System template");
        } else if (template.getTemplateType() == Storage.TemplateType.PERHOST) {
            throw new InvalidParameterValueException("Unable to extract the " + desc + " " + template.getName()
                    + " as it resides on host and not on SSVM");
        }

        if (isISO) {
            if (template.getFormat() != ImageFormat.ISO) {
                throw new InvalidParameterValueException("Unsupported format, could not extract the ISO");
            }
        } else {
            if (template.getFormat() == ImageFormat.ISO) {
                throw new InvalidParameterValueException("Unsupported format, could not extract the template");
            }
        }

        if (zoneId != null && _dcDao.findById(zoneId) == null) {
            throw new IllegalArgumentException("Please specify a valid zone.");
        }

        if (!_accountMgr.isRootAdmin(caller.getType()) && !template.isExtractable()) {
            throw new InvalidParameterValueException("Unable to extract template id=" + templateId + " as it's not extractable");
        }

        _accountMgr.checkAccess(caller, AccessType.ModifyEntry, true, template);

        List<DataStore> ssStores = this._dataStoreMgr.getImageStoresByScope(new ZoneScope(zoneId));

        TemplateDataStoreVO tmpltStoreRef = null;
        ImageStoreEntity tmpltStore = null;
        if (ssStores != null) {
            for (DataStore store : ssStores) {
                tmpltStoreRef = this._tmplStoreDao.findByStoreTemplate(store.getId(), templateId);
                if (tmpltStoreRef != null) {
                    if (tmpltStoreRef.getDownloadState() == com.cloud.storage.VMTemplateStorageResourceAssoc.Status.DOWNLOADED) {
                        tmpltStore = (ImageStoreEntity) store;
                        break;
                    }
                }
            }
        }

        if (tmpltStoreRef == null) {
            throw new InvalidParameterValueException("The " + desc + " has not been downloaded ");
        }

        return tmpltStore.createEntityExtractUrl(tmpltStoreRef.getInstallPath(), template.getFormat());
    }

    public void prepareTemplateInAllStoragePools(final VMTemplateVO template, long zoneId) {
        List<StoragePoolVO> pools = _poolDao.listByStatus(StoragePoolStatus.Up);
        for (final StoragePoolVO pool : pools) {
            if (pool.getDataCenterId() == zoneId) {
                s_logger.info("Schedule to preload template " + template.getId() + " into primary storage " + pool.getId());
                this._preloadExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            reallyRun();
                        } catch (Throwable e) {
                            s_logger.warn("Unexpected exception ", e);
                        }
                    }

                    private void reallyRun() {
                        s_logger.info("Start to preload template " + template.getId() + " into primary storage " + pool.getId());
                        StoragePool pol = (StoragePool) _dataStoreMgr.getPrimaryDataStore(pool.getId());
                        prepareTemplateForCreate(template, pol);
                        s_logger.info("End of preloading template " + template.getId() + " into primary storage " + pool.getId());
                    }
                });
            } else {
                s_logger.info("Skip loading template " + template.getId() + " into primary storage " + pool.getId() + " as pool zone "
                        + pool.getDataCenterId() + " is ");
            }
        }
    }

    @Override
    @DB
    public VMTemplateStoragePoolVO prepareTemplateForCreate(VMTemplateVO templ, StoragePool pool) {
        VMTemplateVO template = _tmpltDao.findById(templ.getId(), true);

        long poolId = pool.getId();
        long templateId = template.getId();
        VMTemplateStoragePoolVO templateStoragePoolRef = null;
        TemplateDataStoreVO templateStoreRef = null;

        templateStoragePoolRef = _tmpltPoolDao.findByPoolTemplate(poolId, templateId);
        if (templateStoragePoolRef != null) {
            templateStoragePoolRef.setMarkedForGC(false);
            _tmpltPoolDao.update(templateStoragePoolRef.getId(), templateStoragePoolRef);

            if (templateStoragePoolRef.getDownloadState() == Status.DOWNLOADED) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Template " + templateId + " has already been downloaded to pool " + poolId);
                }

                return templateStoragePoolRef;
            }
        }

        templateStoreRef = this._tmplStoreDao.findByTemplateZoneDownloadStatus(templateId, pool.getDataCenterId(),
                VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
        if (templateStoreRef == null) {
            s_logger.error("Unable to find a secondary storage host who has completely downloaded the template.");
            return null;
        }

        List<StoragePoolHostVO> vos = _poolHostDao.listByHostStatus(poolId, com.cloud.host.Status.Up);
        if (vos == null || vos.isEmpty()) {
            throw new CloudRuntimeException("Cannot download " + templateId + " to poolId " + poolId
                    + " since there is no host in the Up state connected to this pool");
        }

        if (templateStoragePoolRef == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Downloading template " + templateId + " to pool " + poolId);
            }
            DataStore srcSecStore = this._dataStoreMgr.getDataStore(templateStoreRef.getDataStoreId(), DataStoreRole.Image);
            TemplateInfo srcTemplate = this._tmplFactory.getTemplate(templateId, srcSecStore);

            AsyncCallFuture<TemplateApiResult> future = this._tmpltSvr.prepareTemplateOnPrimary(srcTemplate, pool);
            try {
                TemplateApiResult result = future.get();
                if (result.isFailed()) {
                    s_logger.debug("prepare template failed:" + result.getResult());
                    return null;
                }

                return _tmpltPoolDao.findByPoolTemplate(poolId, templateId);
            } catch (Exception ex) {
                s_logger.debug("failed to copy template from image store:" + srcSecStore.getName() + " to primary storage");
            }
        }

        return null;
    }

    @Override
    public String getChecksum(DataStore store, String templatePath) {
        EndPoint ep = _epSelector.select(store);
        ComputeChecksumCommand cmd = new ComputeChecksumCommand(store.getTO(), templatePath);
        Answer answer = ep.sendMessage(cmd);
        if (answer != null && answer.getResult()) {
            return answer.getDetails();
        }
        return null;
    }

    @Override
    @DB
    public boolean resetTemplateDownloadStateOnPool(long templateStoragePoolRefId) {
        // have to use the same lock that prepareTemplateForCreate use to
        // maintain state consistency
        VMTemplateStoragePoolVO templateStoragePoolRef = _tmpltPoolDao.acquireInLockTable(templateStoragePoolRefId, 1200);

        if (templateStoragePoolRef == null) {
            s_logger.warn("resetTemplateDownloadStateOnPool failed - unable to lock TemplateStorgePoolRef " + templateStoragePoolRefId);
            return false;
        }

        try {
            templateStoragePoolRef.setDownloadState(VMTemplateStorageResourceAssoc.Status.NOT_DOWNLOADED);
            _tmpltPoolDao.update(templateStoragePoolRefId, templateStoragePoolRef);
        } finally {
            _tmpltPoolDao.releaseFromLockTable(templateStoragePoolRefId);
        }

        return true;
    }

    @Override
    @DB
    public boolean copy(long userId, VMTemplateVO template, DataStore srcSecStore, DataCenterVO dstZone) throws StorageUnavailableException,
            ResourceAllocationException {
        long tmpltId = template.getId();
        long dstZoneId = dstZone.getId();
        // find all eligible image stores for the destination zone
        List<DataStore> dstSecStores = this._dataStoreMgr.getImageStoresByScope(new ZoneScope(dstZoneId));
        if (dstSecStores == null || dstSecStores.isEmpty()) {
            throw new StorageUnavailableException("Destination zone is not ready, no image store associated", DataCenter.class, dstZone.getId());
        }
        AccountVO account = _accountDao.findById(template.getAccountId());
        // find the size of the template to be copied
        TemplateDataStoreVO srcTmpltStore = this._tmplStoreDao.findByStoreTemplate(srcSecStore.getId(), tmpltId);

        _resourceLimitMgr.checkResourceLimit(account, ResourceType.template);
        _resourceLimitMgr.checkResourceLimit(account, ResourceType.secondary_storage, new Long(srcTmpltStore.getSize()));

        // Event details
        String copyEventType;
        String createEventType;
        if (template.getFormat().equals(ImageFormat.ISO)) {
            copyEventType = EventTypes.EVENT_ISO_COPY;
            createEventType = EventTypes.EVENT_ISO_CREATE;
        } else {
            copyEventType = EventTypes.EVENT_TEMPLATE_COPY;
            createEventType = EventTypes.EVENT_TEMPLATE_CREATE;
        }

        Transaction txn = Transaction.currentTxn();
        txn.start();

        TemplateInfo srcTemplate = this._tmplFactory.getTemplate(template.getId(), srcSecStore);
        // Copy will just find one eligible image store for the destination zone
        // and copy template there, not propagate to all image stores
        // for that zone
        for (DataStore dstSecStore : dstSecStores) {
            TemplateDataStoreVO dstTmpltStore = this._tmplStoreDao.findByStoreTemplate(dstSecStore.getId(), tmpltId);
            if (dstTmpltStore != null && dstTmpltStore.getDownloadState() == Status.DOWNLOADED) {
                return true; // already downloaded on this image store
            }

            AsyncCallFuture<TemplateApiResult> future = this._tmpltSvr.copyTemplate(srcTemplate, dstSecStore);
            try {
                TemplateApiResult result = future.get();
                if (result.isFailed()) {
                    s_logger.debug("copy template failed for image store " + dstSecStore.getName() + ":" + result.getResult());
                    continue; // try next image store
                }

                _tmpltDao.addTemplateToZone(template, dstZoneId);

                if (account.getId() != Account.ACCOUNT_ID_SYSTEM) {
                    UsageEventUtils.publishUsageEvent(copyEventType, account.getId(), dstZoneId, tmpltId, null, null, null, srcTmpltStore.getSize(),
                            template.getClass().getName(), template.getUuid());
                }
                return true;
            } catch (Exception ex) {
                s_logger.debug("failed to copy template to image store:" + dstSecStore.getName() + " ,will try next one");
            }
        }
        return false;

    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_TEMPLATE_COPY, eventDescription = "copying template", async = true)
    public VirtualMachineTemplate copyTemplate(CopyTemplateCmd cmd) throws StorageUnavailableException, ResourceAllocationException {
        Long templateId = cmd.getId();
        Long userId = CallContext.current().getCallingUserId();
        Long sourceZoneId = cmd.getSourceZoneId();
        Long destZoneId = cmd.getDestinationZoneId();
        Account caller = CallContext.current().getCallingAccount();

        // Verify parameters
        if (sourceZoneId.equals(destZoneId)) {
            throw new InvalidParameterValueException("Please specify different source and destination zones.");
        }

        DataCenterVO sourceZone = _dcDao.findById(sourceZoneId);
        if (sourceZone == null) {
            throw new InvalidParameterValueException("Please specify a valid source zone.");
        }

        DataCenterVO dstZone = _dcDao.findById(destZoneId);
        if (dstZone == null) {
            throw new InvalidParameterValueException("Please specify a valid destination zone.");
        }

        VMTemplateVO template = _tmpltDao.findById(templateId);
        if (template == null || template.getRemoved() != null) {
            throw new InvalidParameterValueException("Unable to find template with id");
        }

        DataStore dstSecStore = getImageStore(destZoneId, templateId);
        if (dstSecStore != null) {
            s_logger.debug("There is template " + templateId + " in secondary storage " + dstSecStore.getName() + " in zone " + destZoneId
                    + " , don't need to copy");
            return template;
        }

        DataStore srcSecStore = getImageStore(sourceZoneId, templateId);
        if (srcSecStore == null) {
            throw new InvalidParameterValueException("There is no template " + templateId + " in zone " + sourceZoneId);
        }
        if (srcSecStore.getScope().getScopeType() == ScopeType.REGION) {
            s_logger.debug("Template " + templateId + " is in region-wide secondary storage " + dstSecStore.getName() + " , don't need to copy");
            return template;
        }

        _accountMgr.checkAccess(caller, AccessType.ModifyEntry, true, template);

        boolean success = copy(userId, template, srcSecStore, dstZone);

        if (success) {
            // increase resource count
            long accountId = template.getAccountId();
            if (template.getSize() != null) {
                _resourceLimitMgr.incrementResourceCount(accountId, ResourceType.secondary_storage, template.getSize());
            }
            return template;
        } else {
            throw new CloudRuntimeException("Failed to copy template");
        }
    }

    @Override
    public boolean delete(long userId, long templateId, Long zoneId) {
        VMTemplateVO template = _tmpltDao.findById(templateId);
        if (template == null || template.getRemoved() != null) {
            throw new InvalidParameterValueException("Please specify a valid template.");
        }

        TemplateAdapter adapter = getAdapter(template.getHypervisorType());
        return adapter.delete(new TemplateProfile(userId, template, zoneId));
    }

    @Override
    public List<VMTemplateStoragePoolVO> getUnusedTemplatesInPool(StoragePoolVO pool) {
        List<VMTemplateStoragePoolVO> unusedTemplatesInPool = new ArrayList<VMTemplateStoragePoolVO>();
        List<VMTemplateStoragePoolVO> allTemplatesInPool = _tmpltPoolDao.listByPoolId(pool.getId());

        for (VMTemplateStoragePoolVO templatePoolVO : allTemplatesInPool) {
            VMTemplateVO template = _tmpltDao.findByIdIncludingRemoved(templatePoolVO.getTemplateId());

            // If this is a routing template, consider it in use
            if (template.getTemplateType() == TemplateType.SYSTEM) {
                continue;
            }

            // If the template is not yet downloaded to the pool, consider it in
            // use
            if (templatePoolVO.getDownloadState() != Status.DOWNLOADED) {
                continue;
            }

            if (template.getFormat() != ImageFormat.ISO && !_volumeDao.isAnyVolumeActivelyUsingTemplateOnPool(template.getId(), pool.getId())) {
                unusedTemplatesInPool.add(templatePoolVO);
            }
        }

        return unusedTemplatesInPool;
    }

    @Override
    public void evictTemplateFromStoragePool(VMTemplateStoragePoolVO templatePoolVO) {
        StoragePool pool = (StoragePool) this._dataStoreMgr.getPrimaryDataStore(templatePoolVO.getPoolId());
        VMTemplateVO template = _tmpltDao.findByIdIncludingRemoved(templatePoolVO.getTemplateId());

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Evicting " + templatePoolVO);
        }
        DestroyCommand cmd = new DestroyCommand(pool, templatePoolVO);

        try {
            Answer answer = _storageMgr.sendToPool(pool, cmd);

            if (answer != null && answer.getResult()) {
                // Remove the templatePoolVO
                if (_tmpltPoolDao.remove(templatePoolVO.getId())) {
                    s_logger.debug("Successfully evicted template: " + template.getName() + " from storage pool: " + pool.getName());
                }
            } else {
                s_logger.info("Will retry evicte template: " + template.getName() + " from storage pool: " + pool.getName());
            }
        } catch (StorageUnavailableException e) {
            s_logger.info("Storage is unavailable currently.  Will retry evicte template: " + template.getName() + " from storage pool: "
                    + pool.getName());
        }

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
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {

        final Map<String, String> configs = _configDao.getConfiguration("AgentManager", params);

        String value = _configDao.getValue(Config.PrimaryStorageDownloadWait.toString());
        _primaryStorageDownloadWait = NumbersUtil.parseInt(value, Integer.parseInt(Config.PrimaryStorageDownloadWait.getDefaultValue()));

        String disableExtraction = _configDao.getValue(Config.DisableExtraction.toString());
        _disableExtraction = (disableExtraction == null) ? false : Boolean.parseBoolean(disableExtraction);

        _storagePoolMaxWaitSeconds = NumbersUtil.parseInt(_configDao.getValue(Config.StoragePoolMaxWaitSeconds.key()), 3600);
        _preloadExecutor = Executors.newFixedThreadPool(8, new NamedThreadFactory("Template-Preloader"));

        return true;
    }

    protected TemplateManagerImpl() {
    }

    @Override
    public boolean templateIsDeleteable(VMTemplateHostVO templateHostRef) {
        VMTemplateVO template = _tmpltDao.findByIdIncludingRemoved(templateHostRef.getTemplateId());
        long templateId = template.getId();
        HostVO secondaryStorageHost = _hostDao.findById(templateHostRef.getHostId());
        long zoneId = secondaryStorageHost.getDataCenterId();
        DataCenterVO zone = _dcDao.findById(zoneId);

        // Check if there are VMs running in the template host ref's zone that
        // use the template
        List<VMInstanceVO> nonExpungedVms = _vmInstanceDao.listNonExpungedByZoneAndTemplate(zoneId, templateId);

        if (!nonExpungedVms.isEmpty()) {
            s_logger.debug("Template " + template.getName() + " in zone " + zone.getName()
                    + " is not deleteable because there are non-expunged VMs deployed from this template.");
            return false;
        }
        List<UserVmVO> userVmUsingIso = _userVmDao.listByIsoId(templateId);
        // check if there is any VM using this ISO.
        if (!userVmUsingIso.isEmpty()) {
            s_logger.debug("ISO " + template.getName() + " in zone " + zone.getName() + " is not deleteable because it is attached to "
                    + userVmUsingIso.size() + " VMs");
            return false;
        }
        // Check if there are any snapshots for the template in the template
        // host ref's zone
        List<VolumeVO> volumes = _volumeDao.findByTemplateAndZone(templateId, zoneId);
        for (VolumeVO volume : volumes) {
            List<SnapshotVO> snapshots = _snapshotDao.listByVolumeIdVersion(volume.getId(), "2.1");
            if (!snapshots.isEmpty()) {
                s_logger.debug("Template " + template.getName() + " in zone " + zone.getName()
                        + " is not deleteable because there are 2.1 snapshots using this template.");
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean templateIsDeleteable(long templateId) {
        List<UserVmJoinVO> userVmUsingIso = _userVmJoinDao.listActiveByIsoId(templateId);
        // check if there is any Vm using this ISO. We only need to check the
        // case where templateId is an ISO since
        // VM can be launched from ISO in secondary storage, while template will
        // always be copied to
        // primary storage before deploying VM.
        if (!userVmUsingIso.isEmpty()) {
            s_logger.debug("ISO " + templateId + " is not deleteable because it is attached to " + userVmUsingIso.size() + " VMs");
            return false;
        }

        return true;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ISO_DETACH, eventDescription = "detaching ISO", async = true)
    public boolean detachIso(long vmId) {
        Account caller = CallContext.current().getCallingAccount();
        Long userId = CallContext.current().getCallingUserId();

        // Verify input parameters
        UserVmVO vmInstanceCheck = _userVmDao.findById(vmId);
        if (vmInstanceCheck == null) {
            throw new InvalidParameterValueException("Unable to find a virtual machine with id " + vmId);
        }

        UserVm userVM = _userVmDao.findById(vmId);
        if (userVM == null) {
            throw new InvalidParameterValueException("Please specify a valid VM.");
        }

        _accountMgr.checkAccess(caller, null, true, userVM);

        Long isoId = userVM.getIsoId();
        if (isoId == null) {
            throw new InvalidParameterValueException("The specified VM has no ISO attached to it.");
        }
        CallContext.current().setEventDetails("Vm Id: " + vmId + " ISO Id: " + isoId);

        State vmState = userVM.getState();
        if (vmState != State.Running && vmState != State.Stopped) {
            throw new InvalidParameterValueException("Please specify a VM that is either Stopped or Running.");
        }

        boolean result = attachISOToVM(vmId, userId, isoId, false); // attach=false
                                                                    // => detach
        if (result) {
            return result;
        } else {
            throw new CloudRuntimeException("Failed to detach iso");
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ISO_ATTACH, eventDescription = "attaching ISO", async = true)
    public boolean attachIso(long isoId, long vmId) {
        Account caller = CallContext.current().getCallingAccount();
        Long userId = CallContext.current().getCallingUserId();

        // Verify input parameters
        UserVmVO vm = _userVmDao.findById(vmId);
        if (vm == null) {
            throw new InvalidParameterValueException("Unable to find a virtual machine with id " + vmId);
        }

        VMTemplateVO iso = _tmpltDao.findById(isoId);
        if (iso == null || iso.getRemoved() != null) {
            throw new InvalidParameterValueException("Unable to find an ISO with id " + isoId);
        }

        // check permissions
        // check if caller has access to VM and ISO
        // and also check if the VM's owner has access to the ISO.

        _accountMgr.checkAccess(caller, null, false, iso, vm);

        Account vmOwner = _accountDao.findById(vm.getAccountId());
        _accountMgr.checkAccess(vmOwner, null, false, iso, vm);

        State vmState = vm.getState();
        if (vmState != State.Running && vmState != State.Stopped) {
            throw new InvalidParameterValueException("Please specify a VM that is either Stopped or Running.");
        }

        if ("xen-pv-drv-iso".equals(iso.getDisplayText()) && vm.getHypervisorType() != Hypervisor.HypervisorType.XenServer) {
            throw new InvalidParameterValueException("Cannot attach Xenserver PV drivers to incompatible hypervisor " + vm.getHypervisorType());
        }

        if ("vmware-tools.iso".equals(iso.getName()) && vm.getHypervisorType() != Hypervisor.HypervisorType.VMware) {
            throw new InvalidParameterValueException("Cannot attach VMware tools drivers to incompatible hypervisor " + vm.getHypervisorType());
        }
        boolean result = attachISOToVM(vmId, userId, isoId, true);
        if (result) {
            return result;
        } else {
            throw new CloudRuntimeException("Failed to attach iso");
        }
    }

    // for ISO, we need to consider whether to copy to cache storage or not if it is not on NFS, since our hypervisor resource always assumes that they are in NFS
    @Override
    public TemplateInfo prepareIso(long isoId, long dcId){
        TemplateInfo tmplt = this._tmplFactory.getTemplate(isoId, DataStoreRole.Image, dcId);
        if (tmplt == null || tmplt.getFormat() != ImageFormat.ISO ) {
            s_logger.warn("ISO: " + isoId + " does not exist in vm_template table");
            return null;
        }

        if (tmplt.getDataStore() != null && !(tmplt.getDataStore().getTO() instanceof NfsTO)) {
            // if it is s3, need to download into cache storage first
            Scope destScope = new ZoneScope(dcId);
            TemplateInfo cacheData = (TemplateInfo) cacheMgr.createCacheObject(tmplt, destScope);
            if (cacheData == null) {
                s_logger.error("Failed in copy iso from S3 to cache storage");
                return null;
            }
            return cacheData;
        } else{
            return tmplt;
        }
    }

    private boolean attachISOToVM(long vmId, long isoId, boolean attach) {
        UserVmVO vm = this._userVmDao.findById(vmId);

        if (vm == null) {
            return false;
        } else if (vm.getState() != State.Running) {
            return true;
        }

        // prepare ISO ready to mount on hypervisor resource level
        TemplateInfo tmplt = prepareIso(isoId, vm.getDataCenterId());

        String vmName = vm.getInstanceName();

        HostVO host = _hostDao.findById(vm.getHostId());
        if (host == null) {
            s_logger.warn("Host: " + vm.getHostId() + " does not exist");
            return false;
        }

        DataTO isoTO = tmplt.getTO();
        DiskTO disk = new DiskTO(isoTO, null, null, Volume.Type.ISO);
        Command cmd = null;
        if (attach) {
            cmd = new AttachCommand(disk, vmName);
        } else {
            cmd = new DettachCommand(disk, vmName);
        }
        Answer a = _agentMgr.easySend(vm.getHostId(), cmd);
        return (a != null && a.getResult());
    }

    private boolean attachISOToVM(long vmId, long userId, long isoId, boolean attach) {
        UserVmVO vm = _userVmDao.findById(vmId);
        VMTemplateVO iso = _tmpltDao.findById(isoId);

        boolean success = attachISOToVM(vmId, isoId, attach);
        if (success && attach) {
            vm.setIsoId(iso.getId());
            _userVmDao.update(vmId, vm);
        }
        if (success && !attach) {
            vm.setIsoId(null);
            _userVmDao.update(vmId, vm);
        }
        return success;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_TEMPLATE_DELETE, eventDescription = "deleting template", async = true)
    public boolean deleteTemplate(DeleteTemplateCmd cmd) {
        Long templateId = cmd.getId();
        Account caller = CallContext.current().getCallingAccount();

        VirtualMachineTemplate template = getTemplate(templateId);
        if (template == null) {
            throw new InvalidParameterValueException("unable to find template with id " + templateId);
        }

        _accountMgr.checkAccess(caller, AccessType.ModifyEntry, true, template);

        if (template.getFormat() == ImageFormat.ISO) {
            throw new InvalidParameterValueException("Please specify a valid template.");
        }

        TemplateAdapter adapter = getAdapter(template.getHypervisorType());
        TemplateProfile profile = adapter.prepareDelete(cmd);
        return adapter.delete(profile);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ISO_DELETE, eventDescription = "deleting iso", async = true)
    public boolean deleteIso(DeleteIsoCmd cmd) {
        Long templateId = cmd.getId();
        Account caller = CallContext.current().getCallingAccount();
        Long zoneId = cmd.getZoneId();

        VirtualMachineTemplate template = getTemplate(templateId);
        ;
        if (template == null) {
            throw new InvalidParameterValueException("unable to find iso with id " + templateId);
        }

        _accountMgr.checkAccess(caller, AccessType.ModifyEntry, true, template);

        if (template.getFormat() != ImageFormat.ISO) {
            throw new InvalidParameterValueException("Please specify a valid iso.");
        }

        // check if there is any VM using this ISO.
        if (!templateIsDeleteable(templateId)) {
            throw new InvalidParameterValueException("Unable to delete iso, as it's used by other vms");
        }

        if (zoneId != null && (this._dataStoreMgr.getImageStore(zoneId) == null)) {
            throw new InvalidParameterValueException("Failed to find a secondary storage store in the specified zone.");
        }
        TemplateAdapter adapter = getAdapter(template.getHypervisorType());
        TemplateProfile profile = adapter.prepareDelete(cmd);
        boolean result = adapter.delete(profile);
        if (result) {
            return true;
        } else {
            throw new CloudRuntimeException("Failed to delete ISO");
        }
    }

    @Override
    public VirtualMachineTemplate getTemplate(long templateId) {
        VMTemplateVO template = _tmpltDao.findById(templateId);
        if (template != null && template.getRemoved() == null) {
            return template;
        }

        return null;
    }

    @Override
    public List<String> listTemplatePermissions(BaseListTemplateOrIsoPermissionsCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        Long id = cmd.getId();

        if (id.equals(Long.valueOf(1))) {
            throw new PermissionDeniedException("unable to list permissions for " + cmd.getMediaType() + " with id " + id);
        }

        VirtualMachineTemplate template = getTemplate(id);
        if (template == null) {
            throw new InvalidParameterValueException("unable to find " + cmd.getMediaType() + " with id " + id);
        }

        if (cmd instanceof ListTemplatePermissionsCmd) {
            if (template.getFormat().equals(ImageFormat.ISO)) {
                throw new InvalidParameterValueException("Please provide a valid template");
            }
        } else if (cmd instanceof ListIsoPermissionsCmd) {
            if (!template.getFormat().equals(ImageFormat.ISO)) {
                throw new InvalidParameterValueException("Please provide a valid iso");
            }
        }

        if (!template.isPublicTemplate()) {
            _accountMgr.checkAccess(caller, null, true, template);
        }

        List<String> accountNames = new ArrayList<String>();
        List<LaunchPermissionVO> permissions = _launchPermissionDao.findByTemplate(id);
        if ((permissions != null) && !permissions.isEmpty()) {
            for (LaunchPermissionVO permission : permissions) {
                Account acct = _accountDao.findById(permission.getAccountId());
                accountNames.add(acct.getAccountName());
            }
        }

        // also add the owner if not public
        if (!template.isPublicTemplate()) {
            Account templateOwner = _accountDao.findById(template.getAccountId());
            accountNames.add(templateOwner.getAccountName());
        }

        return accountNames;
    }

    @DB
    @Override
    public boolean updateTemplateOrIsoPermissions(BaseUpdateTemplateOrIsoPermissionsCmd cmd) {
        Transaction txn = Transaction.currentTxn();

        // Input validation
        Long id = cmd.getId();
        Account caller = CallContext.current().getCallingAccount();
        List<String> accountNames = cmd.getAccountNames();
        List<Long> projectIds = cmd.getProjectIds();
        Boolean isFeatured = cmd.isFeatured();
        Boolean isPublic = cmd.isPublic();
        Boolean isExtractable = cmd.isExtractable();
        String operation = cmd.getOperation();
        String mediaType = "";

        VMTemplateVO template = _tmpltDao.findById(id);

        if (template == null) {
            throw new InvalidParameterValueException("unable to find " + mediaType + " with id " + id);
        }

        if (cmd instanceof UpdateTemplatePermissionsCmd) {
            mediaType = "template";
            if (template.getFormat().equals(ImageFormat.ISO)) {
                throw new InvalidParameterValueException("Please provide a valid template");
            }
        }
        if (cmd instanceof UpdateIsoPermissionsCmd) {
            mediaType = "iso";
            if (!template.getFormat().equals(ImageFormat.ISO)) {
                throw new InvalidParameterValueException("Please provide a valid iso");
            }
        }

        // convert projectIds to accountNames
        if (projectIds != null) {
            // CS-17842, initialize accountNames list
            if (accountNames == null ){
                accountNames = new ArrayList<String>();
            }
            for (Long projectId : projectIds) {
                Project project = _projectMgr.getProject(projectId);
                if (project == null) {
                    throw new InvalidParameterValueException("Unable to find project by id " + projectId);
                }

                if (!_projectMgr.canAccessProjectAccount(caller, project.getProjectAccountId())) {
                    throw new InvalidParameterValueException("Account " + caller + " can't access project id=" + projectId);
                }
                accountNames.add(_accountMgr.getAccount(project.getProjectAccountId()).getAccountName());
            }
        }

        _accountMgr.checkAccess(caller, AccessType.ModifyEntry, true, template);

        // If the template is removed throw an error.
        if (template.getRemoved() != null) {
            s_logger.error("unable to update permissions for " + mediaType + " with id " + id + " as it is removed  ");
            throw new InvalidParameterValueException("unable to update permissions for " + mediaType + " with id " + id + " as it is removed ");
        }

        if (id.equals(Long.valueOf(1))) {
            throw new InvalidParameterValueException("unable to update permissions for " + mediaType + " with id " + id);
        }

        boolean isAdmin = _accountMgr.isAdmin(caller.getType());
        // check configuration parameter(allow.public.user.templates) value for
        // the template owner
        boolean allowPublicUserTemplates = Boolean.valueOf(_configServer.getConfigValue(Config.AllowPublicUserTemplates.key(),
                Config.ConfigurationParameterScope.account.toString(), template.getAccountId()));
        if (!isAdmin && !allowPublicUserTemplates && isPublic != null && isPublic) {
            throw new InvalidParameterValueException("Only private " + mediaType + "s can be created.");
        }

        if (accountNames != null) {
            if ((operation == null)
                    || (!operation.equalsIgnoreCase("add") && !operation.equalsIgnoreCase("remove") && !operation.equalsIgnoreCase("reset"))) {
                throw new InvalidParameterValueException(
                        "Invalid operation on accounts, the operation must be either 'add' or 'remove' in order to modify launch permissions."
                                + "  Given operation is: '" + operation + "'");
            }
        }

        Long accountId = template.getAccountId();
        if (accountId == null) {
            // if there is no owner of the template then it's probably already a
            // public template (or domain private template) so
            // publishing to individual users is irrelevant
            throw new InvalidParameterValueException("Update template permissions is an invalid operation on template " + template.getName());
        }

        VMTemplateVO updatedTemplate = _tmpltDao.createForUpdate();

        if (isPublic != null) {
            updatedTemplate.setPublicTemplate(isPublic.booleanValue());
        }

        if (isFeatured != null) {
            updatedTemplate.setFeatured(isFeatured.booleanValue());
        }

        if (isExtractable != null && caller.getType() == Account.ACCOUNT_TYPE_ADMIN) {// Only
                                                                                      // ROOT
                                                                                      // admins
                                                                                      // allowed
                                                                                      // to
                                                                                      // change
                                                                                      // this
                                                                                      // powerful
                                                                                      // attribute
            updatedTemplate.setExtractable(isExtractable.booleanValue());
        } else if (isExtractable != null && caller.getType() != Account.ACCOUNT_TYPE_ADMIN) {
            throw new InvalidParameterValueException("Only ROOT admins are allowed to modify this attribute.");
        }

        _tmpltDao.update(template.getId(), updatedTemplate);

        Long domainId = caller.getDomainId();
        if ("add".equalsIgnoreCase(operation)) {
            txn.start();
            for (String accountName : accountNames) {
                Account permittedAccount = _accountDao.findActiveAccount(accountName, domainId);
                if (permittedAccount != null) {
                    if (permittedAccount.getId() == caller.getId()) {
                        continue; // don't grant permission to the template
                                  // owner, they implicitly have permission
                    }
                    LaunchPermissionVO existingPermission = _launchPermissionDao.findByTemplateAndAccount(id, permittedAccount.getId());
                    if (existingPermission == null) {
                        LaunchPermissionVO launchPermission = new LaunchPermissionVO(id, permittedAccount.getId());
                        _launchPermissionDao.persist(launchPermission);
                    }
                } else {
                    txn.rollback();
                    throw new InvalidParameterValueException("Unable to grant a launch permission to account " + accountName
                            + ", account not found.  " + "No permissions updated, please verify the account names and retry.");
                }
            }
            txn.commit();
        } else if ("remove".equalsIgnoreCase(operation)) {
            List<Long> accountIds = new ArrayList<Long>();
            for (String accountName : accountNames) {
                Account permittedAccount = _accountDao.findActiveAccount(accountName, domainId);
                if (permittedAccount != null) {
                    accountIds.add(permittedAccount.getId());
                }
            }
            _launchPermissionDao.removePermissions(id, accountIds);
        } else if ("reset".equalsIgnoreCase(operation)) {
            // do we care whether the owning account is an admin? if the
            // owner is an admin, will we still set public to false?
            updatedTemplate = _tmpltDao.createForUpdate();
            updatedTemplate.setPublicTemplate(false);
            updatedTemplate.setFeatured(false);
            _tmpltDao.update(template.getId(), updatedTemplate);
            _launchPermissionDao.removeAllPermissions(id);
        }
        return true;
    }

    private String getRandomPrivateTemplateName() {
        return UUID.randomUUID().toString();
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_TEMPLATE_CREATE, eventDescription = "creating template", async = true)
    public VirtualMachineTemplate createPrivateTemplate(CreateTemplateCmd command) throws CloudRuntimeException {
        Long userId = CallContext.current().getCallingUserId();
        if (userId == null) {
            userId = User.UID_SYSTEM;
        }
        long templateId = command.getEntityId();
        Long volumeId = command.getVolumeId();
        Long snapshotId = command.getSnapshotId();
        VMTemplateVO privateTemplate = null;
        Long accountId = null;
        SnapshotVO snapshot = null;
        VolumeVO volume = null;

        try {
            TemplateInfo tmplInfo = this._tmplFactory.getTemplate(templateId, DataStoreRole.Image);
            Long zoneId = null;
            if (snapshotId != null) {
                snapshot = _snapshotDao.findById(snapshotId);
                zoneId = snapshot.getDataCenterId();
            } else if (volumeId != null) {
                volume = _volumeDao.findById(volumeId);
                zoneId = volume.getDataCenterId();
            }
            ZoneScope scope = new ZoneScope(zoneId);
            List<DataStore> store = this._dataStoreMgr.getImageStoresByScope(scope);
            if (store.size() > 1) {
                throw new CloudRuntimeException("muliple image data store, don't know which one to use");
            }
            AsyncCallFuture<TemplateApiResult> future = null;
            if (snapshotId != null) {
                SnapshotInfo snapInfo = this._snapshotFactory.getSnapshot(snapshotId, DataStoreRole.Image);
                future = this._tmpltSvr.createTemplateFromSnapshotAsync(snapInfo, tmplInfo, store.get(0));
            } else if (volumeId != null) {
                VolumeInfo volInfo = this._volFactory.getVolume(volumeId);
                future = this._tmpltSvr.createTemplateFromVolumeAsync(volInfo, tmplInfo, store.get(0));
            } else {
                throw new CloudRuntimeException("Creating private Template need to specify snapshotId or volumeId");
            }

            CommandResult result = null;
            try {
                result = future.get();
                if (result.isFailed()) {
                    privateTemplate = null;
                    s_logger.debug("Failed to create template" + result.getResult());
                    throw new CloudRuntimeException("Failed to create template" + result.getResult());
                }

                VMTemplateZoneVO templateZone = new VMTemplateZoneVO(zoneId, templateId, new Date());
                this._tmpltZoneDao.persist(templateZone);

                privateTemplate = this._tmpltDao.findById(templateId);
                UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_TEMPLATE_CREATE, privateTemplate.getAccountId(), zoneId,
                        privateTemplate.getId(), privateTemplate.getName(), null, privateTemplate.getSourceTemplateId(), privateTemplate.getSize());
                _usageEventDao.persist(usageEvent);
            } catch (InterruptedException e) {
                s_logger.debug("Failed to create template", e);
                throw new CloudRuntimeException("Failed to create template", e);
            } catch (ExecutionException e) {
                s_logger.debug("Failed to create template", e);
                throw new CloudRuntimeException("Failed to create template", e);
            }

        } finally {
            /*if (snapshot != null && snapshot.getSwiftId() != null
                    && secondaryStorageURL != null && zoneId != null
                    && accountId != null && volumeId != null) {
                _snapshotMgr.deleteSnapshotsForVolume(secondaryStorageURL,
                        zoneId, accountId, volumeId);
            }*/
            if (privateTemplate == null) {
                Transaction txn = Transaction.currentTxn();
                txn.start();
                // template_store_ref entries should have been removed using our
                // DataObject.processEvent command in case of failure, but clean
                // it up here to avoid
                // some leftovers which will cause removing template from
                // vm_template table fail.
                this._tmplStoreDao.deletePrimaryRecordsForTemplate(templateId);
                // Remove the template_zone_ref record
                this._tmpltZoneDao.deletePrimaryRecordsForTemplate(templateId);
                // Remove the template record
                this._tmpltDao.expunge(templateId);

                // decrement resource count
                if (accountId != null) {
                    _resourceLimitMgr.decrementResourceCount(accountId, ResourceType.template);
                    _resourceLimitMgr.decrementResourceCount(accountId, ResourceType.secondary_storage, new Long(volume != null ? volume.getSize()
                            : snapshot.getSize()));
                }
                txn.commit();
            }
        }

        if (privateTemplate != null) {
            return privateTemplate;
        } else {
            throw new CloudRuntimeException("Failed to create a template");
        }
    }

    private static boolean isAdmin(short accountType) {
        return ((accountType == Account.ACCOUNT_TYPE_ADMIN) || (accountType == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN)
                || (accountType == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) || (accountType == Account.ACCOUNT_TYPE_READ_ONLY_ADMIN));
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_TEMPLATE_CREATE, eventDescription = "creating template", create = true)
    public VMTemplateVO createPrivateTemplateRecord(CreateTemplateCmd cmd, Account templateOwner) throws ResourceAllocationException {
        Long userId = CallContext.current().getCallingUserId();

        Account caller = CallContext.current().getCallingAccount();
        boolean isAdmin = (isAdmin(caller.getType()));

        _accountMgr.checkAccess(caller, null, true, templateOwner);

        String name = cmd.getTemplateName();
        if ((name == null) || (name.length() > 32)) {
            throw new InvalidParameterValueException("Template name cannot be null and should be less than 32 characters");
        }

        if (cmd.getTemplateTag() != null) {
            if (!_accountService.isRootAdmin(caller.getType())) {
                throw new PermissionDeniedException("Parameter templatetag can only be specified by a Root Admin, permission denied");
            }
        }

        // do some parameter defaulting
        Integer bits = cmd.getBits();
        Boolean requiresHvm = cmd.getRequiresHvm();
        Boolean passwordEnabled = cmd.isPasswordEnabled();
        Boolean isPublic = cmd.isPublic();
        Boolean featured = cmd.isFeatured();
        int bitsValue = ((bits == null) ? 64 : bits.intValue());
        boolean requiresHvmValue = ((requiresHvm == null) ? true : requiresHvm.booleanValue());
        boolean passwordEnabledValue = ((passwordEnabled == null) ? false : passwordEnabled.booleanValue());
        if (isPublic == null) {
            isPublic = Boolean.FALSE;
        }
        // check whether template owner can create public templates
        boolean allowPublicUserTemplates = Boolean.parseBoolean(_configServer.getConfigValue(Config.AllowPublicUserTemplates.key(),
                Config.ConfigurationParameterScope.account.toString(), templateOwner.getId()));
        if (!isAdmin && !allowPublicUserTemplates && isPublic) {
            throw new PermissionDeniedException("Failed to create template " + name + ", only private templates can be created.");
        }

        Long volumeId = cmd.getVolumeId();
        Long snapshotId = cmd.getSnapshotId();
        if ((volumeId == null) && (snapshotId == null)) {
            throw new InvalidParameterValueException("Failed to create private template record, neither volume ID nor snapshot ID were specified.");
        }
        if ((volumeId != null) && (snapshotId != null)) {
            throw new InvalidParameterValueException("Failed to create private template record, please specify only one of volume ID (" + volumeId
                    + ") and snapshot ID (" + snapshotId + ")");
        }

        HypervisorType hyperType;
        VolumeVO volume = null;
        SnapshotVO snapshot = null;
        VMTemplateVO privateTemplate = null;
        if (volumeId != null) { // create template from volume
            volume = this._volumeDao.findById(volumeId);
            if (volume == null) {
                throw new InvalidParameterValueException("Failed to create private template record, unable to find volume " + volumeId);
            }
            // check permissions
            _accountMgr.checkAccess(caller, null, true, volume);

            // If private template is created from Volume, check that the volume
            // will not be active when the private template is
            // created
            if (!this._volumeMgr.volumeInactive(volume)) {
                String msg = "Unable to create private template for volume: " + volume.getName()
                        + "; volume is attached to a non-stopped VM, please stop the VM first";
                if (s_logger.isInfoEnabled()) {
                    s_logger.info(msg);
                }
                throw new CloudRuntimeException(msg);
            }
            hyperType = this._volumeDao.getHypervisorType(volumeId);
        } else { // create template from snapshot
            snapshot = _snapshotDao.findById(snapshotId);
            if (snapshot == null) {
                throw new InvalidParameterValueException("Failed to create private template record, unable to find snapshot " + snapshotId);
            }

            volume = this._volumeDao.findById(snapshot.getVolumeId());
            VolumeVO snapshotVolume = this._volumeDao.findByIdIncludingRemoved(snapshot.getVolumeId());

            // check permissions
            _accountMgr.checkAccess(caller, null, true, snapshot);

            if (snapshot.getState() != Snapshot.State.BackedUp) {
                throw new InvalidParameterValueException("Snapshot id=" + snapshotId + " is not in " + Snapshot.State.BackedUp
                        + " state yet and can't be used for template creation");
            }

            /*
             * // bug #11428. Operation not supported if vmware and snapshots
             * parent volume = ROOT if(snapshot.getHypervisorType() ==
             * HypervisorType.VMware && snapshotVolume.getVolumeType() ==
             * Type.DATADISK){ throw new UnsupportedServiceException(
             * "operation not supported, snapshot with id " + snapshotId +
             * " is created from Data Disk"); }
             */

            hyperType = snapshot.getHypervisorType();
        }

        _resourceLimitMgr.checkResourceLimit(templateOwner, ResourceType.template);
        _resourceLimitMgr.checkResourceLimit(templateOwner, ResourceType.secondary_storage,
                new Long(volume != null ? volume.getSize() : snapshot.getSize()));

        if (!isAdmin || featured == null) {
            featured = Boolean.FALSE;
        }
        Long guestOSId = cmd.getOsTypeId();
        GuestOSVO guestOS = this._guestOSDao.findById(guestOSId);
        if (guestOS == null) {
            throw new InvalidParameterValueException("GuestOS with ID: " + guestOSId + " does not exist.");
        }

        String uniqueName = Long.valueOf((userId == null) ? 1 : userId).toString() + UUID.nameUUIDFromBytes(name.getBytes()).toString();
        Long nextTemplateId = this._tmpltDao.getNextInSequence(Long.class, "id");
        String description = cmd.getDisplayText();
        boolean isExtractable = false;
        Long sourceTemplateId = null;
        if (volume != null) {
            VMTemplateVO template = ApiDBUtils.findTemplateById(volume.getTemplateId());
            isExtractable = template != null && template.isExtractable() && template.getTemplateType() != Storage.TemplateType.SYSTEM;
            if (template != null) {
                sourceTemplateId = template.getId();
            } else if (volume.getVolumeType() == Volume.Type.ROOT) { // vm
                                                                     // created
                                                                     // out
                // of blank
                // template
                UserVm userVm = ApiDBUtils.findUserVmById(volume.getInstanceId());
                sourceTemplateId = userVm.getIsoId();
            }
        }
        String templateTag = cmd.getTemplateTag();
        if (templateTag != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Adding template tag: " + templateTag);
            }
        }
        privateTemplate = new VMTemplateVO(nextTemplateId, uniqueName, name, ImageFormat.RAW, isPublic, featured, isExtractable, TemplateType.USER,
                null, null, requiresHvmValue, bitsValue, templateOwner.getId(), null, description, passwordEnabledValue, guestOS.getId(), true,
                hyperType, templateTag, cmd.getDetails());
        if (sourceTemplateId != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("This template is getting created from other template, setting source template Id to: " + sourceTemplateId);
            }
        }
        privateTemplate.setSourceTemplateId(sourceTemplateId);

        VMTemplateVO template = this._tmpltDao.persist(privateTemplate);
        // Increment the number of templates
        if (template != null) {
            if (cmd.getDetails() != null) {
                this._templateDetailsDao.persist(template.getId(), cmd.getDetails());
            }

            _resourceLimitMgr.incrementResourceCount(templateOwner.getId(), ResourceType.template);
            _resourceLimitMgr.incrementResourceCount(templateOwner.getId(), ResourceType.secondary_storage,
                    new Long(volume != null ? volume.getSize() : snapshot.getSize()));
        }

        if (template != null) {
            return template;
        } else {
            throw new CloudRuntimeException("Failed to create a template");
        }

    }

    @Override
    public Pair<String, String> getAbsoluteIsoPath(long templateId, long dataCenterId) {
        TemplateDataStoreVO templateStoreRef = this._tmplStoreDao.findByTemplateZoneDownloadStatus(templateId, dataCenterId,
                VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
        if (templateStoreRef == null) {
            throw new CloudRuntimeException("Template " + templateId + " has not been completely downloaded to zone " + dataCenterId);
        }
        DataStore store = this._dataStoreMgr.getDataStore(templateStoreRef.getDataStoreId(), DataStoreRole.Image);
        String isoPath = store.getUri() + "/" + templateStoreRef.getInstallPath();
        return new Pair<String, String>(isoPath, store.getUri());
    }

    @Override
    public String getSecondaryStorageURL(long zoneId) {
        DataStore secStore = this._dataStoreMgr.getImageStore(zoneId);
        if (secStore == null) {
            return null;
        }

        return secStore.getUri();
    }

    // get the image store where a template in a given zone is downloaded to,
    // just pick one is enough.
    @Override
    public DataStore getImageStore(long zoneId, long tmpltId) {
        TemplateDataStoreVO tmpltStore = this._tmplStoreDao.findByTemplateZoneDownloadStatus(tmpltId, zoneId,
                VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
        if (tmpltStore != null) {
            return this._dataStoreMgr.getDataStore(tmpltStore.getDataStoreId(), DataStoreRole.Image);
        }

        return null;
    }

    @Override
    public Long getTemplateSize(long templateId, long zoneId) {
        TemplateDataStoreVO templateStoreRef = this._tmplStoreDao.findByTemplateZoneDownloadStatus(templateId, zoneId,
                VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
        if (templateStoreRef == null) {
            throw new CloudRuntimeException("Template " + templateId + " has not been completely downloaded to zone " + zoneId);
        }
        return templateStoreRef.getSize();

    }

    // find image store where this template is located
    @Override
    public List<DataStore> getImageStoreByTemplate(long templateId, Long zoneId) {
        // find all eligible image stores for this zone scope
        List<DataStore> imageStores = this._dataStoreMgr.getImageStoresByScope(new ZoneScope(zoneId));
        if (imageStores == null || imageStores.size() == 0) {
            return null;
        }
        List<DataStore> stores = new ArrayList<DataStore>();
        for (DataStore store : imageStores) {
            // check if the template is stored there
            List<TemplateDataStoreVO> storeTmpl = this._tmplStoreDao.listByTemplateStore(templateId, store.getId());
            if (storeTmpl != null && storeTmpl.size() > 0) {
                stores.add(store);
            }
        }
        return stores;
    }

    @Override
    public VMTemplateVO updateTemplate(UpdateIsoCmd cmd) {
        return updateTemplateOrIso(cmd);
    }

    @Override
    public VMTemplateVO updateTemplate(UpdateTemplateCmd cmd) {
        return updateTemplateOrIso(cmd);
    }

    private VMTemplateVO updateTemplateOrIso(BaseUpdateTemplateOrIsoCmd cmd) {
        Long id = cmd.getId();
        String name = cmd.getTemplateName();
        String displayText = cmd.getDisplayText();
        String format = cmd.getFormat();
        Long guestOSId = cmd.getOsTypeId();
        Boolean passwordEnabled = cmd.isPasswordEnabled();
        Boolean isDynamicallyScalable = cmd.isDynamicallyScalable();
        Boolean isRoutingTemplate = cmd.isRoutingType();
        Boolean bootable = cmd.isBootable();
        Integer sortKey = cmd.getSortKey();
        Account account = CallContext.current().getCallingAccount();

        // verify that template exists
        VMTemplateVO template = _tmpltDao.findById(id);
        if (template == null || template.getRemoved() != null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("unable to find template/iso with specified id");
            ex.addProxyObject(String.valueOf(id), "templateId");
            throw ex;
        }

        // Don't allow to modify system template
        if (id == Long.valueOf(1)) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Unable to update template/iso of specified id");
            ex.addProxyObject(String.valueOf(id), "templateId");
            throw ex;
        }

        // do a permission check
        _accountMgr.checkAccess(account, AccessType.ModifyEntry, true, template);
        if(cmd.isRoutingType() != null){
            if(!_accountService.isRootAdmin(account.getType())){
                throw new PermissionDeniedException("Parameter isrouting can only be specified by a Root Admin, permission denied");
            }
        }

        boolean updateNeeded = !(name == null && displayText == null && format == null && guestOSId == null && passwordEnabled == null
                && bootable == null && sortKey == null && isDynamicallyScalable == null && isRoutingTemplate == null);
        if (!updateNeeded) {
            return template;
        }

        template = _tmpltDao.createForUpdate(id);

        if (name != null) {
            template.setName(name);
        }

        if (displayText != null) {
            template.setDisplayText(displayText);
        }

        if (sortKey != null) {
            template.setSortKey(sortKey);
        }

        ImageFormat imageFormat = null;
        if (format != null) {
            try {
                imageFormat = ImageFormat.valueOf(format.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidParameterValueException("Image format: " + format + " is incorrect. Supported formats are "
                        + EnumUtils.listValues(ImageFormat.values()));
            }

            template.setFormat(imageFormat);
        }

        if (guestOSId != null) {
            GuestOSVO guestOS = _guestOSDao.findById(guestOSId);

            if (guestOS == null) {
                throw new InvalidParameterValueException("Please specify a valid guest OS ID.");
            } else {
                template.setGuestOSId(guestOSId);
            }
        }

        if (passwordEnabled != null) {
            template.setEnablePassword(passwordEnabled);
        }

        if (bootable != null) {
            template.setBootable(bootable);
        }

        if (isDynamicallyScalable != null) {
            template.setDynamicallyScalable(isDynamicallyScalable);
        }

        if (isRoutingTemplate != null) {
            if (isRoutingTemplate) {
                template.setTemplateType(TemplateType.ROUTING);
            } else {
                template.setTemplateType(TemplateType.USER);
            }
        }

        _tmpltDao.update(id, template);

        return _tmpltDao.findById(id);
    }
}
