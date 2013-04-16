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
package com.cloud.storage.download;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.agent.Listener;
import com.cloud.agent.api.Command;

import com.cloud.agent.api.storage.DownloadCommand;

import com.cloud.agent.api.storage.DownloadCommand.Proxy;
import com.cloud.agent.api.storage.DownloadCommand.ResourceType;
import com.cloud.agent.api.storage.DownloadProgressCommand.RequestType;

import com.cloud.agent.api.storage.DownloadProgressCommand;
import com.cloud.agent.manager.Commands;
import com.cloud.alert.AlertManager;
import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.resource.ResourceManager;
import com.cloud.storage.Storage.ImageFormat;

import com.cloud.storage.StorageManager;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.VolumeHostVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.SwiftDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VMTemplateSwiftDao;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeHostDao;

import com.cloud.storage.secondary.SecondaryStorageVmManager;
import com.cloud.storage.swift.SwiftManager;
import com.cloud.storage.template.TemplateConstants;
import com.cloud.template.TemplateManager;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.ResourceLimitService;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.SecondaryStorageVm;
import com.cloud.vm.SecondaryStorageVmVO;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.SecondaryStorageVmDao;
import com.cloud.vm.dao.UserVmDao;

import org.apache.cloudstack.framework.async.AsyncCompletionCallback;

@Component
@Local(value = { DownloadMonitor.class })
public class DownloadMonitorImpl extends ManagerBase implements DownloadMonitor {
    static final Logger s_logger = Logger.getLogger(DownloadMonitorImpl.class);

    @Inject
    VMTemplateHostDao _vmTemplateHostDao;
    @Inject
    TemplateDataStoreDao _vmTemplateStoreDao;
    @Inject
    VMTemplateZoneDao _vmTemplateZoneDao;
    @Inject
    VMTemplatePoolDao _vmTemplatePoolDao;
    @Inject
    VMTemplateSwiftDao _vmTemplateSwiftlDao;
    @Inject
    StoragePoolHostDao _poolHostDao;
    @Inject
    SecondaryStorageVmDao _secStorageVmDao;
    @Inject
    VolumeDao _volumeDao;
    @Inject
    VolumeHostDao _volumeHostDao;
    @Inject
    VolumeDataStoreDao _volumeStoreDao;
    @Inject
    AlertManager _alertMgr;
    @Inject
    protected SwiftManager _swiftMgr;
    @Inject
    SecondaryStorageVmManager _ssvmMgr;
    @Inject
    StorageManager _storageMgr;

    @Inject
    private final DataCenterDao _dcDao = null;
    @Inject
    VMTemplateDao _templateDao = null;
    @Inject
    private AgentManager _agentMgr;
    @Inject
    SecondaryStorageVmManager _secMgr;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    UserVmManager _vmMgr;

    @Inject
    TemplateManager templateMgr;

    @Inject
    private UsageEventDao _usageEventDao;

    @Inject
    private ClusterDao _clusterDao;
    @Inject
    private HostDao _hostDao;
    @Inject
    private ResourceManager _resourceMgr;
    @Inject
    private SwiftDao _swiftDao;
    @Inject
    protected ResourceLimitService _resourceLimitMgr;
    @Inject
    protected UserVmDao _userVmDao;
    @Inject
    protected AccountManager _accountMgr;

    private Boolean _sslCopy = new Boolean(false);
    private String _copyAuthPasswd;
    private String _proxy = null;
    protected SearchBuilder<TemplateDataStoreVO> ReadyTemplateStatesSearch;

    Timer _timer;

    @Inject
    DataStoreManager storeMgr;

    final Map<TemplateDataStoreVO, DownloadListener> _listenerTemplateMap = new ConcurrentHashMap<TemplateDataStoreVO, DownloadListener>();
    final Map<VMTemplateHostVO, DownloadListener> _listenerMap = new ConcurrentHashMap<VMTemplateHostVO, DownloadListener>();
    final Map<VolumeHostVO, DownloadListener> _listenerVolumeMap = new ConcurrentHashMap<VolumeHostVO, DownloadListener>();
    final Map<VolumeDataStoreVO, DownloadListener> _listenerVolMap = new ConcurrentHashMap<VolumeDataStoreVO, DownloadListener>();

    public void send(Long hostId, Command cmd, Listener listener) throws AgentUnavailableException {
        _agentMgr.send(hostId, new Commands(cmd), listener);
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) {
        final Map<String, String> configs = _configDao.getConfiguration("ManagementServer", params);
        _sslCopy = Boolean.parseBoolean(configs.get("secstorage.encrypt.copy"));
        _proxy = configs.get(Config.SecStorageProxy.key());

        String cert = configs.get("secstorage.ssl.cert.domain");
        if (!"realhostip.com".equalsIgnoreCase(cert)) {
            s_logger.warn("Only realhostip.com ssl cert is supported, ignoring self-signed and other certs");
        }

        _copyAuthPasswd = configs.get("secstorage.copy.password");

        _agentMgr.registerForHostEvents(new DownloadListener(this), true, false, false);

        ReadyTemplateStatesSearch = _vmTemplateStoreDao.createSearchBuilder();
        ReadyTemplateStatesSearch.and("state", ReadyTemplateStatesSearch.entity().getState(), SearchCriteria.Op.EQ);
        ReadyTemplateStatesSearch.and("destroyed", ReadyTemplateStatesSearch.entity().getDestroyed(), SearchCriteria.Op.EQ);
        ReadyTemplateStatesSearch.and("store_id", ReadyTemplateStatesSearch.entity().getDataStoreId(), SearchCriteria.Op.EQ);

        SearchBuilder<VMTemplateVO> TemplatesWithNoChecksumSearch = _templateDao.createSearchBuilder();
        TemplatesWithNoChecksumSearch.and("checksum", TemplatesWithNoChecksumSearch.entity().getChecksum(), SearchCriteria.Op.NULL);

        ReadyTemplateStatesSearch.join("vm_template", TemplatesWithNoChecksumSearch, TemplatesWithNoChecksumSearch.entity().getId(),
                ReadyTemplateStatesSearch.entity().getTemplateId(), JoinBuilder.JoinType.INNER);
        TemplatesWithNoChecksumSearch.done();
        ReadyTemplateStatesSearch.done();

        return true;
    }

    @Override
    public boolean start() {
        _timer = new Timer();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    public boolean isTemplateUpdateable(Long templateId, Long storeId) {
        List<TemplateDataStoreVO> downloadsInProgress = _vmTemplateStoreDao.listByTemplateStoreDownloadStatus(templateId, storeId,
                Status.DOWNLOAD_IN_PROGRESS, Status.DOWNLOADED);
        return (downloadsInProgress.size() == 0);
    }

    // TODO: consider using dataMotionStrategy later
    @Override
    public boolean copyTemplate(VMTemplateVO template, DataStore sourceStore, DataStore destStore) throws StorageUnavailableException {

        boolean downloadJobExists = false;
        TemplateDataStoreVO destTmpltStore = null;
        TemplateDataStoreVO srcTmpltStore = null;

        srcTmpltStore = this._vmTemplateStoreDao.findByStoreTemplate(sourceStore.getId(), template.getId());
        if (srcTmpltStore == null) {
            throw new InvalidParameterValueException("Template " + template.getName() + " not associated with " + sourceStore.getName());
        }

        // generate a storage url on ssvm to copy from
        String url = generateCopyUrl(sourceStore, srcTmpltStore);
        if (url == null) {
            s_logger.warn("Unable to start/resume copy of template " + template.getUniqueName() + " to " + destStore.getName()
                    + ", no secondary storage vm in running state in source zone");
            throw new CloudRuntimeException("No secondary VM in running state in zone " + sourceStore.getScope().getScopeId());
        }
        destTmpltStore = _vmTemplateStoreDao.findByStoreTemplate(destStore.getId(), template.getId());
        if (destTmpltStore == null) {
            destTmpltStore = new TemplateDataStoreVO(destStore.getId(), template.getId(), new Date(), 0,
                    VMTemplateStorageResourceAssoc.Status.NOT_DOWNLOADED, null, null, "jobid0000", null, url);
            destTmpltStore.setCopy(true);
            destTmpltStore.setPhysicalSize(srcTmpltStore.getPhysicalSize());
            _vmTemplateStoreDao.persist(destTmpltStore);
        } else if ((destTmpltStore.getJobId() != null) && (destTmpltStore.getJobId().length() > 2)) {
            downloadJobExists = true;
        }

        Long maxTemplateSizeInBytes = getMaxTemplateSizeInBytes();
        if (srcTmpltStore.getSize() > maxTemplateSizeInBytes) {
            throw new CloudRuntimeException("Cant copy the template as the template's size " + srcTmpltStore.getSize()
                    + " is greater than max.template.iso.size " + maxTemplateSizeInBytes);
        }

        if (destTmpltStore != null) {
            start();
            String sourceChecksum = this.templateMgr.getChecksum(sourceStore, srcTmpltStore.getInstallPath());
            DownloadCommand dcmd = new DownloadCommand(destStore.getTO(), destStore.getUri(), url, template,
                    TemplateConstants.DEFAULT_HTTP_AUTH_USER, _copyAuthPasswd, maxTemplateSizeInBytes);
            dcmd.setProxy(getHttpProxy());
            if (downloadJobExists) {
                dcmd = new DownloadProgressCommand(dcmd, destTmpltStore.getJobId(), RequestType.GET_OR_RESTART);
            }
            dcmd.setChecksum(sourceChecksum); // We need to set the checksum as
                                              // the source template might be a
                                              // compressed url and have cksum
                                              // for compressed image. Bug
                                              // #10775
            HostVO ssAhost = _ssvmMgr.pickSsvmHost(destStore);
            if (ssAhost == null) {
                s_logger.warn("There is no secondary storage VM for secondary storage host " + destStore.getName());
                return false;
            }
            DownloadListener dl = new DownloadListener(ssAhost, destStore, template, _timer, _vmTemplateStoreDao, destTmpltStore.getId(), this, dcmd,
                    _templateDao, _resourceLimitMgr, _alertMgr, _accountMgr, null);
            if (downloadJobExists) {
                dl.setCurrState(destTmpltStore.getDownloadState());
            }
            DownloadListener old = null;
            synchronized (_listenerTemplateMap) {
                old = _listenerTemplateMap.put(destTmpltStore, dl);
            }
            if (old != null) {
                old.abandon();
            }

            try {
                send(ssAhost.getId(), dcmd, dl);
                return true;
            } catch (AgentUnavailableException e) {
                s_logger.warn("Unable to start /resume COPY of template " + template.getUniqueName() + " to " + destStore.getName(), e);
                dl.setDisconnected();
                dl.scheduleStatusCheck(RequestType.GET_OR_RESTART);
                e.printStackTrace();
            }
        }

        return false;
    }

    private String generateCopyUrl(String ipAddress, String dir, String path) {
        String hostname = ipAddress;
        String scheme = "http";
        if (_sslCopy) {
            hostname = ipAddress.replace(".", "-");
            hostname = hostname + ".realhostip.com";
            scheme = "https";
        }
        return scheme + "://" + hostname + "/copy/SecStorage/" + dir + "/" + path;
    }

    private String generateCopyUrl(DataStore sourceServer, TemplateDataStoreVO srcTmpltStore) {
        List<SecondaryStorageVmVO> ssVms = _secStorageVmDao.getSecStorageVmListInStates(SecondaryStorageVm.Role.templateProcessor, sourceServer
                .getScope().getScopeId(), State.Running);
        if (ssVms.size() > 0) {
            SecondaryStorageVmVO ssVm = ssVms.get(0);
            if (ssVm.getPublicIpAddress() == null) {
                s_logger.warn("A running secondary storage vm has a null public ip?");
                return null;
            }
            //TODO: how to handle parent field from hostVO in image_store? and how we can populate that column?
          //  return generateCopyUrl(ssVm.getPublicIpAddress(), sourceServer.getParent(), srcTmpltStore.getInstallPath());
            return generateCopyUrl(ssVm.getPublicIpAddress(), null, srcTmpltStore.getInstallPath());
        }

        VMTemplateVO tmplt = _templateDao.findById(srcTmpltStore.getTemplateId());
        HypervisorType hyperType = tmplt.getHypervisorType();
        /*No secondary storage vm yet*/
        if (hyperType != null && hyperType == HypervisorType.KVM) {
            //return "file://" + sourceServer.getParent() + "/" + srcTmpltStore.getInstallPath();
            return "file://"  + "/" + srcTmpltStore.getInstallPath();
        }
        return null;
    }

    private void initiateTemplateDownload(VMTemplateVO template, DataStore store, AsyncCompletionCallback<CreateCmdResult> callback) {
        boolean downloadJobExists = false;
        TemplateDataStoreVO vmTemplateStore = null;

        vmTemplateStore = _vmTemplateStoreDao.findByStoreTemplate(store.getId(), template.getId());
        if (vmTemplateStore == null) {
            // This method can be invoked other places, for example,
            // handleTemplateSync, in that case, vmTemplateStore may be null
            vmTemplateStore = new TemplateDataStoreVO(store.getId(), template.getId(), new Date(), 0,
                    VMTemplateStorageResourceAssoc.Status.NOT_DOWNLOADED, null, null, "jobid0000", null, template.getUrl());
            _vmTemplateStoreDao.persist(vmTemplateStore);
        } else if ((vmTemplateStore.getJobId() != null) && (vmTemplateStore.getJobId().length() > 2)) {
            downloadJobExists = true;
        }

        Long maxTemplateSizeInBytes = getMaxTemplateSizeInBytes();
        String secUrl = store.getUri();
        if (vmTemplateStore != null) {
            start();
            DownloadCommand dcmd = new DownloadCommand(store.getTO(), secUrl, template, maxTemplateSizeInBytes);
            dcmd.setProxy(getHttpProxy());
            if (downloadJobExists) {
                dcmd = new DownloadProgressCommand(dcmd, vmTemplateStore.getJobId(), RequestType.GET_OR_RESTART);
            }
            if (vmTemplateStore.isCopy()) {
                dcmd.setCreds(TemplateConstants.DEFAULT_HTTP_AUTH_USER, _copyAuthPasswd);
            }
            HostVO ssAhost = _ssvmMgr.pickSsvmHost(store);
            if (ssAhost == null) {
                s_logger.warn("There is no secondary storage VM for downloading template to image store " + store.getName());
                return;
            }
            DownloadListener dl = new DownloadListener(ssAhost, store, template, _timer, _vmTemplateStoreDao, vmTemplateStore.getId(), this, dcmd,
                    _templateDao, _resourceLimitMgr, _alertMgr, _accountMgr, callback);
            if (downloadJobExists) {
                // due to handling existing download job issues, we still keep
                // downloadState in template_store_ref to avoid big change in
                // DownloadListener to use
                // new ObjectInDataStore.State transition. TODO: fix this later
                // to be able to remove downloadState from template_store_ref.
                dl.setCurrState(vmTemplateStore.getDownloadState());
            }
            DownloadListener old = null;
            synchronized (_listenerTemplateMap) {
                old = _listenerTemplateMap.put(vmTemplateStore, dl);
            }
            if (old != null) {
                old.abandon();
            }

            try {
                send(ssAhost.getId(), dcmd, dl);
            } catch (AgentUnavailableException e) {
                s_logger.warn("Unable to start /resume download of template " + template.getUniqueName() + " to " + store.getName(), e);
                dl.setDisconnected();
                dl.scheduleStatusCheck(RequestType.GET_OR_RESTART);
            }
        }
    }

    @Override
    public void downloadTemplateToStorage(VMTemplateVO template, DataStore store, AsyncCompletionCallback<CreateCmdResult> callback) {
        long templateId = template.getId();
        if (isTemplateUpdateable(templateId, store.getId())) {
            if (template != null && template.getUrl() != null) {
                initiateTemplateDownload(template, store, callback);
            }
        }
    }

    @Override
    public void downloadVolumeToStorage(VolumeVO volume, DataStore store, String url, String checkSum, ImageFormat format,
            AsyncCompletionCallback<CreateCmdResult> callback) {
        boolean downloadJobExists = false;
        VolumeDataStoreVO volumeHost = null;

        volumeHost = _volumeStoreDao.findByStoreVolume(store.getId(), volume.getId());
        if (volumeHost == null) {
            volumeHost = new VolumeDataStoreVO(store.getId(), volume.getId(), new Date(), 0, VMTemplateStorageResourceAssoc.Status.NOT_DOWNLOADED,
                    null, null, "jobid0000", null, url, checkSum, format);
            _volumeStoreDao.persist(volumeHost);
        } else if ((volumeHost.getJobId() != null) && (volumeHost.getJobId().length() > 2)) {
            downloadJobExists = true;
        }

        Long maxVolumeSizeInBytes = getMaxVolumeSizeInBytes();
        String secUrl = store.getUri();
        if (volumeHost != null) {
            start();
            DownloadCommand dcmd = new DownloadCommand(secUrl, volume, maxVolumeSizeInBytes, checkSum, url, format);
            dcmd.setProxy(getHttpProxy());
            if (downloadJobExists) {
                dcmd = new DownloadProgressCommand(dcmd, volumeHost.getJobId(), RequestType.GET_OR_RESTART);
                dcmd.setResourceType(ResourceType.VOLUME);
            }

            HostVO ssAhost = _ssvmMgr.pickSsvmHost(store);
            if (ssAhost == null) {
                s_logger.warn("There is no secondary storage VM for image store " + store.getName());
                return;
            }
            DownloadListener dl = new DownloadListener(ssAhost, store, volume, _timer, _volumeStoreDao, volumeHost.getId(), this, dcmd, _volumeDao,
                    _storageMgr, _resourceLimitMgr, _alertMgr, _accountMgr, callback);

            if (downloadJobExists) {
                dl.setCurrState(volumeHost.getDownloadState());
            }
            DownloadListener old = null;
            synchronized (_listenerVolMap) {
                old = _listenerVolMap.put(volumeHost, dl);
            }
            if (old != null) {
                old.abandon();
            }

            try {
                send(ssAhost.getId(), dcmd, dl);
            } catch (AgentUnavailableException e) {
                s_logger.warn("Unable to start /resume download of volume " + volume.getName() + " to " + store.getName(), e);
                dl.setDisconnected();
                dl.scheduleStatusCheck(RequestType.GET_OR_RESTART);
            }
        }
    }

    @DB
    public void handleDownloadEvent(HostVO host, VMTemplateVO template, Status dnldStatus) {
        if ((dnldStatus == VMTemplateStorageResourceAssoc.Status.DOWNLOADED) || (dnldStatus == Status.ABANDONED)) {
            VMTemplateHostVO vmTemplateHost = new VMTemplateHostVO(host.getId(), template.getId());
            synchronized (_listenerMap) {
                _listenerMap.remove(vmTemplateHost);
            }
        }

        VMTemplateHostVO vmTemplateHost = _vmTemplateHostDao.findByHostTemplate(host.getId(), template.getId());

        Transaction txn = Transaction.currentTxn();
        txn.start();

        if (dnldStatus == Status.DOWNLOADED) {
            long size = -1;
            if (vmTemplateHost != null) {
                size = vmTemplateHost.getPhysicalSize();
                template.setSize(size);
                this._templateDao.update(template.getId(), template);
            } else {
                s_logger.warn("Failed to get size for template" + template.getName());
            }
            String eventType = EventTypes.EVENT_TEMPLATE_CREATE;
            if ((template.getFormat()).equals(ImageFormat.ISO)) {
                eventType = EventTypes.EVENT_ISO_CREATE;
            }
            if (template.getAccountId() != Account.ACCOUNT_ID_SYSTEM) {
                UsageEventUtils.publishUsageEvent(eventType, template.getAccountId(), host.getDataCenterId(), template.getId(), template.getName(),
                        null, template.getSourceTemplateId(), size, template.getClass().getName(), template.getUuid());
            }
        }
        txn.commit();
    }

    @DB
    public void handleDownloadEvent(HostVO host, VolumeVO volume, Status dnldStatus) {
        if ((dnldStatus == VMTemplateStorageResourceAssoc.Status.DOWNLOADED) || (dnldStatus == Status.ABANDONED)) {
            VolumeHostVO volumeHost = new VolumeHostVO(host.getId(), volume.getId());
            synchronized (_listenerVolumeMap) {
                _listenerVolumeMap.remove(volumeHost);
            }
        }

        VolumeHostVO volumeHost = _volumeHostDao.findByHostVolume(host.getId(), volume.getId());

        Transaction txn = Transaction.currentTxn();
        txn.start();

        if (dnldStatus == Status.DOWNLOADED) {

            // Create usage event
            long size = -1;
            if (volumeHost != null) {
                size = volumeHost.getPhysicalSize();
                volume.setSize(size);
                this._volumeDao.update(volume.getId(), volume);
            } else {
                s_logger.warn("Failed to get size for volume" + volume.getName());
            }
            String eventType = EventTypes.EVENT_VOLUME_UPLOAD;
            if (volume.getAccountId() != Account.ACCOUNT_ID_SYSTEM) {
                UsageEventUtils.publishUsageEvent(eventType, volume.getAccountId(), host.getDataCenterId(), volume.getId(), volume.getName(), null,
                        0l, size, volume.getClass().getName(), volume.getUuid());
            }
        } else if (dnldStatus == Status.DOWNLOAD_ERROR || dnldStatus == Status.ABANDONED || dnldStatus == Status.UNKNOWN) {
            // Decrement the volume and secondary storage space count
            _resourceLimitMgr.decrementResourceCount(volume.getAccountId(), com.cloud.configuration.Resource.ResourceType.volume);
            _resourceLimitMgr.recalculateResourceCount(volume.getAccountId(), volume.getDomainId(),
                    com.cloud.configuration.Resource.ResourceType.secondary_storage.getOrdinal());
        }
        txn.commit();
    }

    /*
    @Override
    public void addSystemVMTemplatesToHost(HostVO host, Map<String, TemplateProp> templateInfos){
        if ( templateInfos == null ) {
            return;
        }
        Long hostId = host.getId();
        List<VMTemplateVO> rtngTmplts = _templateDao.listAllSystemVMTemplates();
        for ( VMTemplateVO tmplt : rtngTmplts ) {
            TemplateProp tmpltInfo = templateInfos.get(tmplt.getUniqueName());
            if ( tmpltInfo == null ) {
                continue;
            }
            VMTemplateHostVO tmpltHost = _vmTemplateHostDao.findByHostTemplate(hostId, tmplt.getId());
            if ( tmpltHost == null ) {
                tmpltHost = new VMTemplateHostVO(hostId, tmplt.getId(), new Date(), 100, Status.DOWNLOADED, null, null, null, tmpltInfo.getInstallPath(), tmplt.getUrl());
                tmpltHost.setSize(tmpltInfo.getSize());
                tmpltHost.setPhysicalSize(tmpltInfo.getPhysicalSize());
                _vmTemplateHostDao.persist(tmpltHost);
            }
        }
    }
    */

    @Override
    public void cancelAllDownloads(Long templateId) {
        List<VMTemplateHostVO> downloadsInProgress = _vmTemplateHostDao.listByTemplateStates(templateId,
                VMTemplateHostVO.Status.DOWNLOAD_IN_PROGRESS, VMTemplateHostVO.Status.NOT_DOWNLOADED);
        if (downloadsInProgress.size() > 0) {
            for (VMTemplateHostVO vmthvo : downloadsInProgress) {
                DownloadListener dl = null;
                synchronized (_listenerMap) {
                    dl = _listenerMap.remove(vmthvo);
                }
                if (dl != null) {
                    dl.abandon();
                    s_logger.info("Stopping download of template " + templateId + " to storage server " + vmthvo.getHostId());
                }
            }
        }
    }

    /*
    private void checksumSync(long hostId){
        SearchCriteria<TemplateDataStoreVO> sc = ReadyTemplateStatesSearch.create();
        sc.setParameters("state", ObjectInDataStoreStateMachine.State.Ready);
        sc.setParameters("host_id", hostId);

        List<VMTemplateHostVO> templateHostRefList = _vmTemplateHostDao.search(sc, null);
        s_logger.debug("Found " +templateHostRefList.size()+ " templates with no checksum. Will ask for computation");
        for(VMTemplateHostVO templateHostRef : templateHostRefList){
            s_logger.debug("Getting checksum for template - " + templateHostRef.getTemplateId());
            String checksum = this.templateMgr.getChecksum(hostId, templateHostRef.getInstallPath());
            VMTemplateVO template = _templateDao.findById(templateHostRef.getTemplateId());
            s_logger.debug("Setting checksum " +checksum+ " for template - " + template.getName());
            template.setChecksum(checksum);
            _templateDao.update(template.getId(), template);
        }

    }
    */

    private Long getMaxTemplateSizeInBytes() {
        try {
            return Long.parseLong(_configDao.getValue("max.template.iso.size")) * 1024L * 1024L * 1024L;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long getMaxVolumeSizeInBytes() {
        try {
            return Long.parseLong(_configDao.getValue("storage.max.volume.upload.size")) * 1024L * 1024L * 1024L;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Proxy getHttpProxy() {
        if (_proxy == null) {
            return null;
        }
        try {
            URI uri = new URI(_proxy);
            Proxy prx = new Proxy(uri);
            return prx;
        } catch (URISyntaxException e) {
            return null;
        }
    }

}
