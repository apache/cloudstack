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

import javax.inject.Inject;

import com.cloud.agent.api.to.VmwareVmForMigrationTO;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.storage.StorageManager;
import com.cloud.storage.VMTemplateDetailVO;
import com.cloud.storage.dao.VMTemplateDetailsDao;
import com.cloud.utils.Pair;
import com.cloud.vm.VmDetailConstants;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.command.DownloadCommand;
import org.apache.cloudstack.storage.command.DownloadCommand.ResourceType;
import org.apache.cloudstack.storage.command.DownloadProgressCommand;
import org.apache.cloudstack.storage.command.DownloadProgressCommand.RequestType;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreVO;
import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.storage.DownloadAnswer;
import com.cloud.utils.net.Proxy;
import com.cloud.configuration.Config;
import com.cloud.storage.RegisterVolumePayload;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.template.TemplateConstants;
import com.cloud.storage.upload.UploadListener;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
public class DownloadMonitorImpl extends ManagerBase implements DownloadMonitor {
    static final Logger LOGGER = Logger.getLogger(DownloadMonitorImpl.class);

    @Inject
    private TemplateDataStoreDao _vmTemplateStoreDao;
    @Inject
    private VolumeDataStoreDao _volumeStoreDao;
    @Inject
    private AgentManager _agentMgr;
    @Inject
    private ConfigurationDao _configDao;
    @Inject
    private EndPointSelector _epSelector;
    @Inject
    private VMTemplateDetailsDao templateDetailsDao;

    private String _copyAuthPasswd;
    private String _proxy = null;

    private Timer _timer;

    @Override
    public boolean configure(String name, Map<String, Object> params) {
        final Map<String, String> configs = _configDao.getConfiguration("management-server", params);
        _proxy = configs.get(Config.SecStorageProxy.key());

        String cert = configs.get("secstorage.ssl.cert.domain");
        if (!"realhostip.com".equalsIgnoreCase(cert)) {
            LOGGER.warn("Only realhostip.com ssl cert is supported, ignoring self-signed and other certs");
        }

        _copyAuthPasswd = configs.get("secstorage.copy.password");

        DownloadListener dl = new DownloadListener(this);
        ComponentContext.inject(dl);
        _agentMgr.registerForHostEvents(dl, true, false, false);

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
        List<TemplateDataStoreVO> downloadsInProgress =
            _vmTemplateStoreDao.listByTemplateStoreDownloadStatus(templateId, storeId, Status.DOWNLOAD_IN_PROGRESS, Status.DOWNLOADED);
        return (downloadsInProgress.size() == 0);
    }

    protected TemplateDataStoreVO persistTemplateDataStoreRecord(DataObject template, DataStore store) {
        TemplateDataStoreVO vmTemplateStore = new TemplateDataStoreVO(store.getId(), template.getId(),
                new Date(), 0, Status.NOT_DOWNLOADED, null, null,
                "jobid0000", null, template.getUri());
        vmTemplateStore.setDataStoreRole(store.getRole());
        return _vmTemplateStoreDao.persist(vmTemplateStore);
    }

    /**
     * The first element indicates if there is an existing download job, and second element is the templateDataStoreVO
     */
    protected Pair<Boolean, TemplateDataStoreVO> getTemplateDataStoreRecordPair(DataObject template, DataStore store) {
        boolean downloadJobExists = false;
        TemplateDataStoreVO vmTemplateStore = _vmTemplateStoreDao.findByStoreTemplate(store.getId(), template.getId());
        if (vmTemplateStore == null) {
            vmTemplateStore = persistTemplateDataStoreRecord(template, store);
        } else if ((vmTemplateStore.getJobId() != null) && (vmTemplateStore.getJobId().length() > 2)) {
            downloadJobExists = true;
        }
        return new Pair<>(downloadJobExists, vmTemplateStore);
    }

    protected String getValueFromTemplateDetail(Long templateId, String key) {
        VMTemplateDetailVO detail = templateDetailsDao.findDetail(templateId, key);
        if (detail == null) {
            throw new InvalidParameterValueException(String.format("Could not find the template detail" +
                    " with key %s for the template with ID: %s", key, templateId));
        }
        return detail.getValue();
    }

    protected void sendMigrateVmwareVmCommandToKvmHost(DataObject template, DataStore imageStore, AsyncCompletionCallback<DownloadAnswer> callback) {
        String vcenter = getValueFromTemplateDetail(template.getId(), VmDetailConstants.VMWARE_VCENTER);
        String datacenter = getValueFromTemplateDetail(template.getId(), VmDetailConstants.VMWARE_DATACENTER);
        String cluster = getValueFromTemplateDetail(template.getId(), VmDetailConstants.VMWARE_CLUSTER);
        String username = getValueFromTemplateDetail(template.getId(), VmDetailConstants.VMWARE_VCENTER_USERNAME);
        String password = getValueFromTemplateDetail(template.getId(), VmDetailConstants.VMWARE_VCENTER_PASSWORD);
        String hostName = getValueFromTemplateDetail(template.getId(), VmDetailConstants.VMWARE_HOST);
        String vmName = getValueFromTemplateDetail(template.getId(), VmDetailConstants.VMWARE_VM_NAME);

        // Migrate the clone of the stopped VM
        EndPoint endPoint = _epSelector.select(template);
        LOGGER.debug(String.format("Found host %s to send command for VMware to KVM migration", endPoint.getHostAddr()));

        Long maxTemplateSizeInBytes = getMaxTemplateSizeInBytes();
        VmwareVmForMigrationTO vmTO = new VmwareVmForMigrationTO(vcenter, datacenter, cluster,
                username, password, null, hostName, vmName, imageStore.getUri());
        DownloadCommand cmd = new DownloadCommand((TemplateObjectTO) template.getTO(), maxTemplateSizeInBytes);
        cmd.setVmwareVmForMigrationTO(vmTO);
        cmd.setWait(StorageManager.KvmTemplateFromVmwareVmMigrationWait.value());

        DownloadListener dl = new DownloadListener(endPoint, imageStore, template, _timer,
                this, cmd, callback);
        ComponentContext.inject(dl);  // initialize those auto-wired field in download listener.
        try {
            endPoint.sendMessageAsync(cmd, new UploadListener.Callback(endPoint.getId(), dl));
            LOGGER.debug(String.format("Sent DownloadCommand to endpoint: %s for template %s",
                    endPoint.getHostAddr(), template.getUuid()));
            updateTemplateStoreRefRecordForVmwareVmToKvmMigration(template, imageStore, endPoint);
        } catch (Exception e) {
            String err = String.format("Could not migrate VM from VMware: %s", e.getMessage());
            LOGGER.error(err, e);
        }
    }

    private void updateTemplateStoreRefRecordForVmwareVmToKvmMigration(DataObject template, DataStore imageStore, EndPoint endPoint) {
        TemplateDataStoreVO storeRef = _vmTemplateStoreDao.findByStoreTemplate(imageStore.getId(), template.getId());
        storeRef.setDownloadState(Status.NOT_DOWNLOADED);
        storeRef.setErrorString(String.format("Downloading, virt-v2v migration initiated on host %s",
                endPoint.getHostAddr()));
        _vmTemplateStoreDao.update(storeRef.getId(), storeRef);
    }

    protected void migrateVmwareVmToSecondaryStorage(DataObject template, AsyncCompletionCallback<DownloadAnswer> callback) {
        DataStore imageStore = template.getDataStore();
        Pair<Boolean, TemplateDataStoreVO> pair = getTemplateDataStoreRecordPair(template, imageStore);
        TemplateDataStoreVO templateDataStoreVO = pair.second();
        if (templateDataStoreVO != null) {
            imageStore.getScope().getScopeId();
            sendMigrateVmwareVmCommandToKvmHost(template, imageStore, callback);
        }
    }

    private void initiateTemplateDownload(DataObject template, AsyncCompletionCallback<DownloadAnswer> callback) {
        DataStore store = template.getDataStore();

        Pair<Boolean, TemplateDataStoreVO> pair = getTemplateDataStoreRecordPair(template, store);
        boolean downloadJobExists = pair.first();
        TemplateDataStoreVO vmTemplateStore = pair.second();

        Long maxTemplateSizeInBytes = getMaxTemplateSizeInBytes();
        if (vmTemplateStore != null) {
            start();
            DownloadCommand dcmd = new DownloadCommand((TemplateObjectTO)(template.getTO()), maxTemplateSizeInBytes);
            dcmd.setProxy(getHttpProxy());
            if (downloadJobExists) {
                dcmd = new DownloadProgressCommand(dcmd, vmTemplateStore.getJobId(), RequestType.GET_OR_RESTART);
            }
            if (vmTemplateStore.isCopy()) {
                dcmd.setCreds(TemplateConstants.DEFAULT_HTTP_AUTH_USER, _copyAuthPasswd);
            }
            EndPoint ep = _epSelector.select(template);
            if (ep == null) {
                String errMsg = "There is no secondary storage VM for downloading template to image store " + store.getName();
                LOGGER.warn(errMsg);
                throw new CloudRuntimeException(errMsg);
            }
            DownloadListener dl = new DownloadListener(ep, store, template, _timer, this, dcmd, callback);
            ComponentContext.inject(dl);  // initialize those auto-wired field in download listener.
            if (downloadJobExists) {
                // due to handling existing download job issues, we still keep
                // downloadState in template_store_ref to avoid big change in
                // DownloadListener to use
                // new ObjectInDataStore.State transition. TODO: fix this later
                // to be able to remove downloadState from template_store_ref.
                LOGGER.info("found existing download job");
                dl.setCurrState(vmTemplateStore.getDownloadState());
            }

            try {
                ep.sendMessageAsync(dcmd, new UploadListener.Callback(ep.getId(), dl));
            } catch (Exception e) {
                LOGGER.warn("Unable to start /resume download of template " + template.getId() + " to " + store.getName(), e);
                dl.setDisconnected();
                dl.scheduleStatusCheck(RequestType.GET_OR_RESTART);
            }
        }
    }

    @Override
    public void downloadTemplateToStorage(DataObject template, AsyncCompletionCallback<DownloadAnswer> callback) {
        if(template != null) {
            long templateId = template.getId();
            DataStore store = template.getDataStore();
            if (isTemplateUpdateable(templateId, store.getId())) {
                if (template.getUri() != null) {
                    String templateUri = template.getUri();
                    if (templateUri.startsWith("vpx://")) {
                        migrateVmwareVmToSecondaryStorage(template, callback);
                    } else {
                        initiateTemplateDownload(template, callback);
                    }
                } else {
                    LOGGER.info("Template url is null, cannot download");
                    DownloadAnswer ans = new DownloadAnswer("Template url is null", Status.UNKNOWN);
                    callback.complete(ans);
                }
            } else {
                LOGGER.info("Template download is already in progress or already downloaded");
                DownloadAnswer ans =
                        new DownloadAnswer("Template download is already in progress or already downloaded", Status.UNKNOWN);
                callback.complete(ans);
            }
        }
    }

    @Override
    public void downloadVolumeToStorage(DataObject volume, AsyncCompletionCallback<DownloadAnswer> callback) {
        boolean downloadJobExists = false;
        VolumeDataStoreVO volumeHost;
        DataStore store = volume.getDataStore();
        VolumeInfo volInfo = (VolumeInfo)volume;
        RegisterVolumePayload payload = (RegisterVolumePayload)volInfo.getpayload();
        String url = payload.getUrl();
        String checkSum = payload.getChecksum();
        ImageFormat format = ImageFormat.valueOf(payload.getFormat());

        volumeHost = _volumeStoreDao.findByStoreVolume(store.getId(), volume.getId());
        if (volumeHost == null) {
            volumeHost = new VolumeDataStoreVO(store.getId(), volume.getId(), new Date(), 0, Status.NOT_DOWNLOADED, null, null, "jobid0000", null, url, checkSum);
            volumeHost = _volumeStoreDao.persist(volumeHost);
        } else if ((volumeHost.getJobId() != null) && (volumeHost.getJobId().length() > 2)) {
            downloadJobExists = true;
        } else {
            // persit url and checksum
            volumeHost.setDownloadUrl(url);
            volumeHost.setChecksum(checkSum);
            _volumeStoreDao.update(volumeHost.getId(), volumeHost);
        }

        Long maxVolumeSizeInBytes = getMaxVolumeSizeInBytes();
        start();
        DownloadCommand dcmd = new DownloadCommand((VolumeObjectTO)(volume.getTO()), maxVolumeSizeInBytes, checkSum, url, format);
        dcmd.setProxy(getHttpProxy());
        if (downloadJobExists) {
            dcmd = new DownloadProgressCommand(dcmd, volumeHost.getJobId(), RequestType.GET_OR_RESTART);
            dcmd.setResourceType(ResourceType.VOLUME);
        }

        EndPoint ep = _epSelector.select(volume);
        if (ep == null) {
            LOGGER.warn("There is no secondary storage VM for image store " + store.getName());
            return;
        }
        DownloadListener dl = new DownloadListener(ep, store, volume, _timer, this, dcmd, callback);
        ComponentContext.inject(dl); // auto-wired those injected fields in DownloadListener

        if (downloadJobExists) {
            dl.setCurrState(volumeHost.getDownloadState());
        }

        try {
            ep.sendMessageAsync(dcmd, new UploadListener.Callback(ep.getId(), dl));
        } catch (Exception e) {
            LOGGER.warn("Unable to start /resume download of volume " + volume.getId() + " to " + store.getName(), e);
            dl.setDisconnected();
            dl.scheduleStatusCheck(RequestType.GET_OR_RESTART);
        }
    }

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
            return new Proxy(uri);
        } catch (URISyntaxException e) {
            return null;
        }
    }

}
