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
package com.cloud.storage.upload;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.cloudstack.storage.image.datastore.ImageStoreEntity;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.CreateEntityDownloadURLCommand;
import com.cloud.agent.api.storage.DeleteEntityDownloadURLCommand;
import com.cloud.agent.api.storage.UploadCommand;
import com.cloud.agent.api.storage.UploadProgressCommand.RequestType;
import com.cloud.api.ApiDBUtils;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.resource.ResourceManager;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Upload;
import com.cloud.storage.Upload.Mode;
import com.cloud.storage.Upload.Status;
import com.cloud.storage.Upload.Type;
import com.cloud.storage.UploadVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.UploadDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.SecondaryStorageVm;
import com.cloud.vm.SecondaryStorageVmVO;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.SecondaryStorageVmDao;

/**
 * Monitors the progress of upload.
 */
@Component
public class UploadMonitorImpl extends ManagerBase implements UploadMonitor {

    static final Logger s_logger = Logger.getLogger(UploadMonitorImpl.class);

    @Inject
    private UploadDao _uploadDao;
    @Inject
    private SecondaryStorageVmDao _secStorageVmDao;

    @Inject
    private final HostDao _serverDao = null;
    @Inject
    private AgentManager _agentMgr;
    @Inject
    private ConfigurationDao _configDao;
    @Inject
    private ResourceManager _resourceMgr;
    @Inject
    private EndPointSelector _epSelector;
    @Inject
    private DataStoreManager storeMgr;

    private boolean _sslCopy = false;
    private String _ssvmUrlDomain;
    private ScheduledExecutorService _executor = null;

    private Timer _timer;
    private int _cleanupInterval;
    private int _urlExpirationInterval;

    final Map<UploadVO, UploadListener> _listenerMap = new ConcurrentHashMap<UploadVO, UploadListener>();

    @Override
    public void cancelAllUploads(Long templateId) {
        // TODO

    }

    @Override
    public boolean isTypeUploadInProgress(Long typeId, Type type) {
        List<UploadVO> uploadsInProgress = _uploadDao.listByTypeUploadStatus(typeId, type, UploadVO.Status.UPLOAD_IN_PROGRESS);

        if (uploadsInProgress.size() > 0) {
            return true;
        } else if (type == Type.VOLUME && _uploadDao.listByTypeUploadStatus(typeId, type, UploadVO.Status.COPY_IN_PROGRESS).size() > 0) {
            return true;
        }
        return false;

    }

    @Override
    public UploadVO createNewUploadEntry(Long hostId, Long typeId, UploadVO.Status uploadState, Type type, String uploadUrl, Upload.Mode mode) {

        UploadVO uploadObj = new UploadVO(hostId, typeId, new Date(), uploadState, type, uploadUrl, mode);
        _uploadDao.persist(uploadObj);

        return uploadObj;

    }

    @Override
    public void extractVolume(UploadVO uploadVolumeObj, DataStore secStore, VolumeVO volume, String url, Long dataCenterId, String installPath, long eventId,
        long asyncJobId, AsyncJobManager asyncMgr) {

        uploadVolumeObj.setUploadState(Upload.Status.NOT_UPLOADED);
        _uploadDao.update(uploadVolumeObj.getId(), uploadVolumeObj);

        start();
        UploadCommand ucmd = new UploadCommand(url, volume.getId(), volume.getSize(), installPath, Type.VOLUME);
        UploadListener ul =
            new UploadListener(secStore, _timer, _uploadDao, uploadVolumeObj, this, ucmd, volume.getAccountId(), volume.getName(), Type.VOLUME, eventId, asyncJobId,
                asyncMgr);
        _listenerMap.put(uploadVolumeObj, ul);

        try {
            EndPoint ep = _epSelector.select(secStore);
            if (ep == null) {
                String errMsg = "No remote endpoint to send command, check if host or ssvm is down?";
                s_logger.error(errMsg);
                return;
            }
            ep.sendMessageAsync(ucmd, new UploadListener.Callback(ep.getId(), ul));
        } catch (Exception e) {
            s_logger.warn("Unable to start upload of volume " + volume.getName() + " from " + secStore.getName() + " to " + url, e);
            ul.setDisconnected();
            ul.scheduleStatusCheck(RequestType.GET_OR_RESTART);
        }
    }

    @Override
    public Long extractTemplate(VMTemplateVO template, String url, TemplateDataStoreVO vmTemplateHost, Long dataCenterId, long eventId, long asyncJobId,
        AsyncJobManager asyncMgr) {

        Type type = (template.getFormat() == ImageFormat.ISO) ? Type.ISO : Type.TEMPLATE;

        DataStore secStore = storeMgr.getImageStoreWithFreeCapacity(dataCenterId);
        if(secStore == null) {
            s_logger.error("Unable to extract template, secondary storage to satisfy storage needs cannot be found!");
            return null;
        }

        UploadVO uploadTemplateObj = new UploadVO(secStore.getId(), template.getId(), new Date(), Upload.Status.NOT_UPLOADED, type, url, Mode.FTP_UPLOAD);
        _uploadDao.persist(uploadTemplateObj);

        if (vmTemplateHost != null) {
            start();
            UploadCommand ucmd = new UploadCommand(template, url, vmTemplateHost.getInstallPath(), vmTemplateHost.getSize());
            UploadListener ul =
                new UploadListener(secStore, _timer, _uploadDao, uploadTemplateObj, this, ucmd, template.getAccountId(), template.getName(), type, eventId, asyncJobId,
                    asyncMgr);
            _listenerMap.put(uploadTemplateObj, ul);
            try {
                EndPoint ep = _epSelector.select(secStore);
                if (ep == null) {
                    String errMsg = "No remote endpoint to send command, check if host or ssvm is down?";
                    s_logger.error(errMsg);
                    return null;
                }
                ep.sendMessageAsync(ucmd, new UploadListener.Callback(ep.getId(), ul));
            } catch (Exception e) {
                s_logger.warn("Unable to start upload of " + template.getUniqueName() + " from " + secStore.getName() + " to " + url, e);
                ul.setDisconnected();
                ul.scheduleStatusCheck(RequestType.GET_OR_RESTART);
            }
            return uploadTemplateObj.getId();
        }
        return null;
    }

    @Override
    public UploadVO createEntityDownloadURL(VMTemplateVO template, TemplateDataStoreVO vmTemplateHost, Long dataCenterId, long eventId) {

        String errorString = "";
        boolean success = false;
        Type type = (template.getFormat() == ImageFormat.ISO) ? Type.ISO : Type.TEMPLATE;

        // find an endpoint to send command
        DataStore store = storeMgr.getDataStore(vmTemplateHost.getDataStoreId(), DataStoreRole.Image);
        EndPoint ep = _epSelector.select(store);
        if (ep == null) {
            String errMsg = "No remote endpoint to send command, check if host or ssvm is down?";
            s_logger.error(errMsg);
            return null;
        }

        //Check if it already exists.
        List<UploadVO> extractURLList = _uploadDao.listByTypeUploadStatus(template.getId(), type, UploadVO.Status.DOWNLOAD_URL_CREATED);
        if (extractURLList.size() > 0) {
            // do some check here
            UploadVO upload = extractURLList.get(0);
            String uploadUrl = extractURLList.get(0).getUploadUrl();
            String[] token = uploadUrl.split("/");
            // example: uploadUrl = https://10-11-101-112.realhostip.com/userdata/2fdd9a70-9c4a-4a04-b1d5-1e41c221a1f9.iso
            // then token[2] = 10-11-101-112.realhostip.com, token[4] = 2fdd9a70-9c4a-4a04-b1d5-1e41c221a1f9.iso
            String hostname = ep.getPublicAddr().replace(".", "-") + ".";
            if ((token != null) && (token.length == 5) && (token[2].equals(hostname + _ssvmUrlDomain))) // ssvm publicip and domain suffix not changed
                return extractURLList.get(0);
            else if ((token != null) && (token.length == 5) && (token[2].startsWith(hostname))) { // domain suffix changed
                String uuid = token[4];
                uploadUrl = generateCopyUrl(ep.getPublicAddr(), uuid);
                UploadVO vo = _uploadDao.createForUpdate();
                vo.setLastUpdated(new Date());
                vo.setUploadUrl(uploadUrl);
                _uploadDao.update(upload.getId(), vo);
                return _uploadDao.findById(upload.getId(), true);
            } else { // ssvm publicip changed
                return null;
            }
        }

        // It doesn't exist so create a DB entry.
        UploadVO uploadTemplateObj =
            new UploadVO(vmTemplateHost.getDataStoreId(), template.getId(), new Date(), Status.DOWNLOAD_URL_NOT_CREATED, 0, type, Mode.HTTP_DOWNLOAD);
        uploadTemplateObj.setInstallPath(vmTemplateHost.getInstallPath());
        _uploadDao.persist(uploadTemplateObj);

        try {
            // Create Symlink at ssvm
            String path = vmTemplateHost.getInstallPath();
            String uuid = UUID.randomUUID().toString() + "." + template.getFormat().getFileExtension(); // adding "." + vhd/ova... etc.
            CreateEntityDownloadURLCommand cmd = new CreateEntityDownloadURLCommand(((ImageStoreEntity)store).getMountPoint(), path, uuid, null);
            Answer ans = ep.sendMessage(cmd);
            if (ans == null || !ans.getResult()) {
                errorString = "Unable to create a link for " + type + " id:" + template.getId() + "," + (ans == null ? "" : ans.getDetails());
                s_logger.error(errorString);
                throw new CloudRuntimeException(errorString);
            }

            //Construct actual URL locally now that the symlink exists at SSVM
            String extractURL = generateCopyUrl(ep.getPublicAddr(), uuid);
            UploadVO vo = _uploadDao.createForUpdate();
            vo.setLastUpdated(new Date());
            vo.setUploadUrl(extractURL);
            vo.setUploadState(Status.DOWNLOAD_URL_CREATED);
            _uploadDao.update(uploadTemplateObj.getId(), vo);
            success = true;
            return _uploadDao.findById(uploadTemplateObj.getId(), true);
        } finally {
            if (!success) {
                UploadVO uploadJob = _uploadDao.createForUpdate(uploadTemplateObj.getId());
                uploadJob.setLastUpdated(new Date());
                uploadJob.setErrorString(errorString);
                uploadJob.setUploadState(Status.ERROR);
                _uploadDao.update(uploadTemplateObj.getId(), uploadJob);
            }
        }

    }

    @Override
    public void createVolumeDownloadURL(Long entityId, String path, Type type, Long dataCenterId, Long uploadId, ImageFormat format) {

        String errorString = "";
        boolean success = false;
        try {
            List<HostVO> storageServers = _resourceMgr.listAllHostsInOneZoneByType(Host.Type.SecondaryStorage, dataCenterId);
            if (storageServers == null) {
                errorString = "No Storage Server found at the datacenter - " + dataCenterId;
                throw new CloudRuntimeException(errorString);
            }

            // Update DB for state = DOWNLOAD_URL_NOT_CREATED.
            UploadVO uploadJob = _uploadDao.createForUpdate(uploadId);
            uploadJob.setUploadState(Status.DOWNLOAD_URL_NOT_CREATED);
            uploadJob.setLastUpdated(new Date());
            _uploadDao.update(uploadJob.getId(), uploadJob);

            // Create Symlink at ssvm
            String uuid = UUID.randomUUID().toString() + "." + format.toString().toLowerCase();
            DataStore secStore = storeMgr.getDataStore(ApiDBUtils.findUploadById(uploadId).getDataStoreId(), DataStoreRole.Image);
            EndPoint ep = _epSelector.select(secStore);
            if (ep == null) {
                errorString = "There is no secondary storage VM for secondary storage host " + secStore.getName();
                throw new CloudRuntimeException(errorString);
            }

            CreateEntityDownloadURLCommand cmd = new CreateEntityDownloadURLCommand(((ImageStoreEntity)secStore).getMountPoint(), path, uuid, null);
            Answer ans = ep.sendMessage(cmd);
            if (ans == null || !ans.getResult()) {
                errorString = "Unable to create a link for " + type + " id:" + entityId + "," + (ans == null ? "" : ans.getDetails());
                s_logger.warn(errorString);
                throw new CloudRuntimeException(errorString);
            }

            List<SecondaryStorageVmVO> ssVms = _secStorageVmDao.getSecStorageVmListInStates(SecondaryStorageVm.Role.templateProcessor, dataCenterId, State.Running);
            if (ssVms.size() > 0) {
                SecondaryStorageVmVO ssVm = ssVms.get(0);
                if (ssVm.getPublicIpAddress() == null) {
                    errorString = "A running secondary storage vm has a null public ip?";
                    s_logger.error(errorString);
                    throw new CloudRuntimeException(errorString);
                }
                //Construct actual URL locally now that the symlink exists at SSVM
                String extractURL = generateCopyUrl(ssVm.getPublicIpAddress(), uuid);
                UploadVO vo = _uploadDao.createForUpdate();
                vo.setLastUpdated(new Date());
                vo.setUploadUrl(extractURL);
                vo.setUploadState(Status.DOWNLOAD_URL_CREATED);
                _uploadDao.update(uploadId, vo);
                success = true;
                return;
            }
            errorString = "Couldnt find a running SSVM in the zone" + dataCenterId + ". Couldnt create the extraction URL.";
            throw new CloudRuntimeException(errorString);
        } finally {
            if (!success) {
                UploadVO uploadJob = _uploadDao.createForUpdate(uploadId);
                uploadJob.setLastUpdated(new Date());
                uploadJob.setErrorString(errorString);
                uploadJob.setUploadState(Status.ERROR);
                _uploadDao.update(uploadId, uploadJob);
            }
        }
    }

    private String generateCopyUrl(String ipAddress, String uuid) {
        String hostname = ipAddress;
        String scheme = "http";
        if (_sslCopy) {
            hostname = ipAddress.replace(".", "-");
            scheme = "https";

            // Code for putting in custom certificates.
            if (_ssvmUrlDomain != null && _ssvmUrlDomain.length() > 0) {
                hostname = hostname + "." + _ssvmUrlDomain;
            } else {
                hostname = hostname + ".realhostip.com";
            }
        }
        return scheme + "://" + hostname + "/userdata/" + uuid;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        final Map<String, String> configs = _configDao.getConfiguration("management-server", params);
        _sslCopy = Boolean.parseBoolean(configs.get("secstorage.encrypt.copy"));

        String cert = configs.get("secstorage.secure.copy.cert");
        if ("realhostip.com".equalsIgnoreCase(cert)) {
            s_logger.warn("Only realhostip.com ssl cert is supported, ignoring self-signed and other certs");
        }

        _ssvmUrlDomain = configs.get("secstorage.ssl.cert.domain");

        _agentMgr.registerForHostEvents(new UploadListener(this), true, false, false);
        String cleanupInterval = configs.get("extract.url.cleanup.interval");
        _cleanupInterval = NumbersUtil.parseInt(cleanupInterval, 7200);

        String urlExpirationInterval = configs.get("extract.url.expiration.interval");
        _urlExpirationInterval = NumbersUtil.parseInt(urlExpirationInterval, 14400);

        String workers = (String)params.get("expunge.workers");
        int wrks = NumbersUtil.parseInt(workers, 1);
        _executor = Executors.newScheduledThreadPool(wrks, new NamedThreadFactory("UploadMonitor-Scavenger"));
        return true;
    }

    @Override
    public boolean start() {
        _executor.scheduleWithFixedDelay(new StorageGarbageCollector(), _cleanupInterval, _cleanupInterval, TimeUnit.SECONDS);
        _timer = new Timer();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    public void handleUploadEvent(Long accountId, String typeName, Type type, Long uploadId, com.cloud.storage.Upload.Status reason, long eventId) {

        if ((reason == Upload.Status.UPLOADED) || (reason == Upload.Status.ABANDONED)) {
            UploadVO uploadObj = new UploadVO(uploadId);
            UploadListener oldListener = _listenerMap.get(uploadObj);
            if (oldListener != null) {
                _listenerMap.remove(uploadObj);
            }
        }

    }

    @Override
    public void handleUploadSync(long sserverId) {

        HostVO storageHost = _serverDao.findById(sserverId);
        if (storageHost == null) {
            s_logger.warn("Huh? Agent id " + sserverId + " does not correspond to a row in hosts table?");
            return;
        }
        s_logger.debug("Handling upload sserverId " + sserverId);
        List<UploadVO> uploadsInProgress = new ArrayList<UploadVO>();
        uploadsInProgress.addAll(_uploadDao.listByHostAndUploadStatus(sserverId, UploadVO.Status.UPLOAD_IN_PROGRESS));
        uploadsInProgress.addAll(_uploadDao.listByHostAndUploadStatus(sserverId, UploadVO.Status.COPY_IN_PROGRESS));
        if (uploadsInProgress.size() > 0) {
            for (UploadVO uploadJob : uploadsInProgress) {
                uploadJob.setUploadState(UploadVO.Status.UPLOAD_ERROR);
                uploadJob.setErrorString("Could not complete the upload.");
                uploadJob.setLastUpdated(new Date());
                _uploadDao.update(uploadJob.getId(), uploadJob);
            }

        }

    }

    protected class StorageGarbageCollector extends ManagedContextRunnable {

        public StorageGarbageCollector() {
        }

        @Override
        protected void runInContext() {
            try {
                GlobalLock scanLock = GlobalLock.getInternLock("uploadmonitor.storageGC");
                try {
                    if (scanLock.lock(3)) {
                        try {
                            cleanupStorage();
                        } finally {
                            scanLock.unlock();
                        }
                    }
                } finally {
                    scanLock.releaseRef();
                }

            } catch (Exception e) {
                s_logger.error("Caught the following Exception", e);
            }
        }
    }

    private long getTimeDiff(Date date) {
        Calendar currentDateCalendar = Calendar.getInstance();
        Calendar givenDateCalendar = Calendar.getInstance();
        givenDateCalendar.setTime(date);

        return (currentDateCalendar.getTimeInMillis() - givenDateCalendar.getTimeInMillis()) / 1000;
    }

    public void cleanupStorage() {

        final int EXTRACT_URL_LIFE_LIMIT_IN_SECONDS = _urlExpirationInterval;
        List<UploadVO> extractJobs = _uploadDao.listByModeAndStatus(Mode.HTTP_DOWNLOAD, Status.DOWNLOAD_URL_CREATED);

        for (UploadVO extractJob : extractJobs) {
            if (getTimeDiff(extractJob.getLastUpdated()) > EXTRACT_URL_LIFE_LIMIT_IN_SECONDS) {
                String path = extractJob.getInstallPath();
                DataStore secStore = storeMgr.getDataStore(extractJob.getDataStoreId(), DataStoreRole.Image);

                // Would delete the symlink for the Type and if Type == VOLUME then also the volume
                DeleteEntityDownloadURLCommand cmd =
                    new DeleteEntityDownloadURLCommand(path, extractJob.getType(), extractJob.getUploadUrl(), ((ImageStoreVO)secStore).getParent());
                EndPoint ep = _epSelector.select(secStore);
                if (ep == null) {
                    s_logger.warn("UploadMonitor cleanup: There is no secondary storage VM for secondary storage host " + extractJob.getDataStoreId());
                    continue; //TODO: why continue? why not break?
                }
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("UploadMonitor cleanup: Sending deletion of extract URL " + extractJob.getUploadUrl() + " to ssvm " + ep.getHostAddr());
                }
                Answer ans = ep.sendMessage(cmd);
                if (ans != null && ans.getResult()) {
                    _uploadDao.remove(extractJob.getId());
                } else {
                    s_logger.warn("UploadMonitor cleanup: Unable to delete the link for " + extractJob.getType() + " id=" + extractJob.getTypeId() + " url=" +
                        extractJob.getUploadUrl() + " on ssvm " + ep.getHostAddr());
                }
            }
        }

    }

}
