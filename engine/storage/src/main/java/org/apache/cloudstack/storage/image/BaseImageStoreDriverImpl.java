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
package org.apache.cloudstack.storage.image;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.cloud.agent.api.storage.OVFPropertyTO;
import com.cloud.storage.ImageStore;
import com.cloud.storage.Upload;
import com.cloud.storage.VMTemplateDetailVO;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.cloudstack.api.net.NetworkPrerequisiteTO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.async.AsyncRpcContext;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.command.DeleteCommand;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreVO;
import org.apache.cloudstack.storage.endpoint.DefaultEndPointSelector;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.CreateDatadiskTemplateCommand;
import com.cloud.agent.api.storage.DownloadAnswer;
import com.cloud.agent.api.storage.GetDatadisksAnswer;
import com.cloud.agent.api.storage.GetDatadisksCommand;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataTO;
import com.cloud.alert.AlertManager;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateDetailsDao;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.download.DownloadMonitor;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.dao.AccountDao;
import com.cloud.agent.api.to.DatadiskTO;
import com.cloud.utils.net.Proxy;
import com.cloud.utils.exception.CloudRuntimeException;

public abstract class BaseImageStoreDriverImpl implements ImageStoreDriver {
    private static final Logger LOGGER = Logger.getLogger(BaseImageStoreDriverImpl.class);

    @Inject
    protected VMTemplateDao _templateDao;
    @Inject
    DownloadMonitor _downloadMonitor;
    @Inject
    VolumeDao volumeDao;
    @Inject
    VolumeDataStoreDao _volumeStoreDao;
    @Inject
    TemplateDataStoreDao _templateStoreDao;
    @Inject
    EndPointSelector _epSelector;
    @Inject
    ConfigurationDao configDao;
    @Inject
    VMTemplateZoneDao _vmTemplateZoneDao;
    @Inject
    AlertManager _alertMgr;
    @Inject
    VMTemplateDetailsDao templateDetailsDao;
    @Inject
    DefaultEndPointSelector _defaultEpSelector;
    @Inject
    AccountDao _accountDao;
    @Inject
    ResourceLimitService _resourceLimitMgr;

    protected String _proxy = null;

    private static Gson gson;

    static {
        GsonBuilder builder = new GsonBuilder();
        builder.disableHtmlEscaping();
        gson = builder.create();
    }

    protected Proxy getHttpProxy() {
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

    @Override
    public Map<String, String> getCapabilities() {
        return null;
    }

    @Override
    public DataTO getTO(DataObject data) {
        return null;
    }

    protected class CreateContext<T> extends AsyncRpcContext<T> {
        final DataObject data;

        public CreateContext(AsyncCompletionCallback<T> callback, DataObject data) {
            super(callback);
            this.data = data;
        }
    }

    protected Long getMaxTemplateSizeInBytes() {
        try {
            return Long.parseLong(configDao.getValue("max.template.iso.size")) * 1024L * 1024L * 1024L;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public void createAsync(DataStore dataStore, DataObject data, AsyncCompletionCallback<CreateCmdResult> callback) {
        CreateContext<CreateCmdResult> context = new CreateContext<CreateCmdResult>(callback, data);
        AsyncCallbackDispatcher<BaseImageStoreDriverImpl, DownloadAnswer> caller = AsyncCallbackDispatcher.create(this);
        caller.setContext(context);
        if (data.getType() == DataObjectType.TEMPLATE) {
            caller.setCallback(caller.getTarget().createTemplateAsyncCallback(null, null));
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Downloading template to data store " + dataStore.getId());
            }
            _downloadMonitor.downloadTemplateToStorage(data, caller);
        } else if (data.getType() == DataObjectType.VOLUME) {
            caller.setCallback(caller.getTarget().createVolumeAsyncCallback(null, null));
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Downloading volume to data store " + dataStore.getId());
            }
            _downloadMonitor.downloadVolumeToStorage(data, caller);
        }
    }

    /**
     * Persist OVF properties as template details for template with id = templateId
     */
    private void persistOVFProperties(List<OVFPropertyTO> ovfProperties, long templateId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(String.format("saving properties for template %d as details", templateId));
        }
        for (OVFPropertyTO property : ovfProperties) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format("saving property %s for template %d as detail", property.getKey(), templateId));
            }
            persistOvfPropertyAsSetOfTemplateDetails(templateId, property);
        }
    }

    private void persistOvfPropertyAsSetOfTemplateDetails(long templateId, OVFPropertyTO property) {
        String key = property.getKey();
        String propKey = ImageStore.OVF_PROPERTY_PREFIX + key;
        try {
            String propValue = gson.toJson(property);
            savePropertyAttribute(templateId, propKey, propValue);
        } catch (RuntimeException re) {
            LOGGER.error("gson marshalling of property object fails: " + propKey,re);
        }
    }

    private void persistNetworkRequirements(List<NetworkPrerequisiteTO> networkRequirements, long templateId) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(String.format("saving network requirements for template %d as details", templateId));
        }
        for (NetworkPrerequisiteTO network : networkRequirements) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format("saving property %s for template %d as detail", network.getName(), templateId));
            }
            persistRequiredNetworkAsSetOfTemplateDetails(templateId, network);
        }
    }

    private void persistRequiredNetworkAsSetOfTemplateDetails(long templateId, NetworkPrerequisiteTO network) {
        String key = network.getName();
        String propKey = ImageStore.REQUIRED_NETWORK_PREFIX + key;
        try {
            String propValue = gson.toJson(network);
            savePropertyAttribute(templateId, propKey, propValue);
        } catch (RuntimeException re) {
            LOGGER.warn("gson marshalling of network object fails: " + propKey,re);
        }
    }

    private void savePropertyAttribute(long templateId, String key, String value) {
        if ( templateDetailsDao.findDetail(templateId,key) != null) {
            LOGGER.debug(String.format("detail '%s' existed for template %d, deleting.", key, templateId));
            templateDetailsDao.removeDetail(templateId,key);
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(String.format("template detail for template %d to save is '%s': '%s'", templateId, key, value));
        }
        VMTemplateDetailVO detailVO = new VMTemplateDetailVO(templateId, key, value, false);
        LOGGER.debug("Persisting template details " + detailVO.getName() + " from OVF properties for template " + templateId);
        templateDetailsDao.persist(detailVO);
    }

    protected Void createTemplateAsyncCallback(AsyncCallbackDispatcher<? extends BaseImageStoreDriverImpl, DownloadAnswer> callback,
        CreateContext<CreateCmdResult> context) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Performing image store createTemplate async callback");
        }
        DownloadAnswer answer = callback.getResult();
        DataObject obj = context.data;
        DataStore store = obj.getDataStore();
        List<OVFPropertyTO> ovfProperties = answer.getOvfProperties();
        List<NetworkPrerequisiteTO> networkRequirements = answer.getNetworkRequirements();

        TemplateDataStoreVO tmpltStoreVO = _templateStoreDao.findByStoreTemplate(store.getId(), obj.getId());
        if (tmpltStoreVO != null) {
            if (tmpltStoreVO.getDownloadState() == VMTemplateStorageResourceAssoc.Status.DOWNLOADED) {
                persistExtraDetails(obj, ovfProperties, networkRequirements);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Template is already in DOWNLOADED state, ignore further incoming DownloadAnswer");
                }
                return null;
            }
            TemplateDataStoreVO updateBuilder = _templateStoreDao.createForUpdate();
            updateBuilder.setDownloadPercent(answer.getDownloadPct());
            updateBuilder.setDownloadState(answer.getDownloadStatus());
            updateBuilder.setLastUpdated(new Date());
            updateBuilder.setErrorString(answer.getErrorString());
            updateBuilder.setJobId(answer.getJobId());
            updateBuilder.setLocalDownloadPath(answer.getDownloadPath());
            updateBuilder.setInstallPath(answer.getInstallPath());
            updateBuilder.setSize(answer.getTemplateSize());
            updateBuilder.setPhysicalSize(answer.getTemplatePhySicalSize());
            _templateStoreDao.update(tmpltStoreVO.getId(), updateBuilder);
            // update size in vm_template table
            VMTemplateVO tmlptUpdater = _templateDao.createForUpdate();
            tmlptUpdater.setSize(answer.getTemplateSize());
            _templateDao.update(obj.getId(), tmlptUpdater);
        }

        AsyncCompletionCallback<CreateCmdResult> caller = context.getParentCallback();

        if (answer.getDownloadStatus() == VMTemplateStorageResourceAssoc.Status.DOWNLOAD_ERROR ||
            answer.getDownloadStatus() == VMTemplateStorageResourceAssoc.Status.ABANDONED || answer.getDownloadStatus() == VMTemplateStorageResourceAssoc.Status.UNKNOWN) {
            CreateCmdResult result = new CreateCmdResult(null, null);
            result.setSuccess(false);
            result.setResult(answer.getErrorString());
            caller.complete(result);
            String msg = "Failed to register template: " + obj.getUuid() + " with error: " + answer.getErrorString();
            _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_UPLOAD_FAILED, _vmTemplateZoneDao.listByTemplateId(obj.getId()).get(0).getZoneId(), null, msg, msg);
            LOGGER.error(msg);
        } else if (answer.getDownloadStatus() == VMTemplateStorageResourceAssoc.Status.DOWNLOADED) {
            if (answer.getCheckSum() != null) {
                VMTemplateVO templateDaoBuilder = _templateDao.createForUpdate();
                templateDaoBuilder.setChecksum(answer.getCheckSum());
                _templateDao.update(obj.getId(), templateDaoBuilder);
            }
            persistExtraDetails(obj, ovfProperties, networkRequirements);

            CreateCmdResult result = new CreateCmdResult(null, null);
            caller.complete(result);
        }
        return null;
    }

    private void persistExtraDetails(DataObject obj, List<OVFPropertyTO> ovfProperties, List<NetworkPrerequisiteTO> networkRequirements) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(String.format("saving %d ovf properties for template '%s' as details", ovfProperties != null ? ovfProperties.size() : 0, obj.getUuid()));
        }
        if (CollectionUtils.isNotEmpty(ovfProperties)) {
            persistOVFProperties(ovfProperties, obj.getId());
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(String.format("saving %d required network requirements for template '%s' as details", networkRequirements != null ? networkRequirements.size() : 0, obj.getUuid()));
        }
        if (CollectionUtils.isNotEmpty(networkRequirements)) {
            persistNetworkRequirements(networkRequirements, obj.getId());
        }
    }

    protected Void
        createVolumeAsyncCallback(AsyncCallbackDispatcher<? extends BaseImageStoreDriverImpl, DownloadAnswer> callback, CreateContext<CreateCmdResult> context) {
        DownloadAnswer answer = callback.getResult();
        DataObject obj = context.data;
        DataStore store = obj.getDataStore();

        VolumeDataStoreVO volStoreVO = _volumeStoreDao.findByStoreVolume(store.getId(), obj.getId());
        if (volStoreVO != null) {
            if (volStoreVO.getDownloadState() == VMTemplateStorageResourceAssoc.Status.DOWNLOADED) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Volume is already in DOWNLOADED state, ignore further incoming DownloadAnswer");
                }
                return null;
            }
            VolumeDataStoreVO updateBuilder = _volumeStoreDao.createForUpdate();
            updateBuilder.setDownloadPercent(answer.getDownloadPct());
            updateBuilder.setDownloadState(answer.getDownloadStatus());
            updateBuilder.setLastUpdated(new Date());
            updateBuilder.setErrorString(answer.getErrorString());
            updateBuilder.setJobId(answer.getJobId());
            updateBuilder.setLocalDownloadPath(answer.getDownloadPath());
            updateBuilder.setInstallPath(answer.getInstallPath());
            updateBuilder.setSize(answer.getTemplateSize());
            updateBuilder.setPhysicalSize(answer.getTemplatePhySicalSize());
            _volumeStoreDao.update(volStoreVO.getId(), updateBuilder);
            // update size in volume table
            VolumeVO volUpdater = volumeDao.createForUpdate();
            volUpdater.setSize(answer.getTemplateSize());
            volumeDao.update(obj.getId(), volUpdater);
        }

        AsyncCompletionCallback<CreateCmdResult> caller = context.getParentCallback();

        if (answer.getDownloadStatus() == VMTemplateStorageResourceAssoc.Status.DOWNLOAD_ERROR ||
            answer.getDownloadStatus() == VMTemplateStorageResourceAssoc.Status.ABANDONED || answer.getDownloadStatus() == VMTemplateStorageResourceAssoc.Status.UNKNOWN) {
            CreateCmdResult result = new CreateCmdResult(null, null);
            result.setSuccess(false);
            result.setResult(answer.getErrorString());
            caller.complete(result);
            String msg = "Failed to upload volume: " + obj.getUuid() + " with error: " + answer.getErrorString();
            _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_UPLOAD_FAILED,
                    (volStoreVO == null ? -1L : volStoreVO.getZoneId()), null, msg, msg);
            LOGGER.error(msg);
        } else if (answer.getDownloadStatus() == VMTemplateStorageResourceAssoc.Status.DOWNLOADED) {
            CreateCmdResult result = new CreateCmdResult(null, null);
            caller.complete(result);
        }
        return null;
    }

    @Override
    public void deleteAsync(DataStore dataStore, DataObject data, AsyncCompletionCallback<CommandResult> callback) {
        CommandResult result = new CommandResult();
        try {
            DeleteCommand cmd = new DeleteCommand(data.getTO());
            EndPoint ep = _epSelector.select(data);
            Answer answer = null;
            if (ep == null) {
                String errMsg = "No remote endpoint to send command, check if host or ssvm is down?";
                LOGGER.error(errMsg);
                answer = new Answer(cmd, false, errMsg);
            } else {
                answer = ep.sendMessage(cmd);
            }
            if (answer != null && !answer.getResult()) {
                result.setResult(answer.getDetails());
            }
        } catch (Exception ex) {
            LOGGER.debug("Unable to destoy " + data.getType().toString() + ": " + data.getId(), ex);
            result.setResult(ex.toString());
        }
        callback.complete(result);
    }

    @Override
    public void copyAsync(DataObject srcdata, DataObject destData, AsyncCompletionCallback<CopyCommandResult> callback) {
    }

    @Override
    public boolean canCopy(DataObject srcData, DataObject destData) {
        return false;
    }

    @Override
    public void resize(DataObject data, AsyncCompletionCallback<CreateCmdResult> callback) {
    }

    @Override
    public void deleteEntityExtractUrl(DataStore store, String installPath, String url, Upload.Type entityType) {
    }

    @Override
    public List<DatadiskTO> getDataDiskTemplates(DataObject obj) {
        List<DatadiskTO> dataDiskDetails = new ArrayList<DatadiskTO>();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Get the data disks present in the OVA template");
        }
        DataStore store = obj.getDataStore();
        GetDatadisksCommand cmd = new GetDatadisksCommand(obj.getTO());
        EndPoint ep = _defaultEpSelector.select(store);
        Answer answer = null;
        if (ep == null) {
            String errMsg = "No remote endpoint to send command, check if host or ssvm is down?";
            LOGGER.error(errMsg);
            answer = new Answer(cmd, false, errMsg);
        } else {
            answer = ep.sendMessage(cmd);
        }
        if (answer != null && answer.getResult()) {
            GetDatadisksAnswer getDatadisksAnswer = (GetDatadisksAnswer)answer;
            dataDiskDetails = getDatadisksAnswer.getDataDiskDetails(); // Details - Disk path, virtual size
        }
        else {
            throw new CloudRuntimeException("Get Data disk command failed " + answer.getDetails());
        }
        return dataDiskDetails;
    }

    @Override
    public Void createDataDiskTemplateAsync(TemplateInfo dataDiskTemplate, String path, String diskId, boolean bootable, long fileSize, AsyncCompletionCallback<CreateCmdResult> callback) {
        Answer answer = null;
        String errMsg = null;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Create Datadisk template: " + dataDiskTemplate.getId());
        }
        CreateDatadiskTemplateCommand cmd = new CreateDatadiskTemplateCommand(dataDiskTemplate.getTO(), path, diskId, fileSize, bootable);
        EndPoint ep = _defaultEpSelector.select(dataDiskTemplate.getDataStore());
        if (ep == null) {
            errMsg = "No remote endpoint to send command, check if host or ssvm is down?";
            LOGGER.error(errMsg);
            answer = new Answer(cmd, false, errMsg);
        } else {
            answer = ep.sendMessage(cmd);
        }
        if (answer != null && !answer.getResult()) {
            errMsg = answer.getDetails();
        }
        CreateCmdResult result = new CreateCmdResult(null, answer);
        result.setResult(errMsg);
        callback.complete(result);
        return null;
    }
}
