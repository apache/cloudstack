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

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import com.cloud.hypervisor.Hypervisor;
import com.cloud.utils.Pair;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.State;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateService;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.storage.command.UploadStatusAnswer;
import org.apache.cloudstack.storage.command.UploadStatusAnswer.UploadStatus;
import org.apache.cloudstack.storage.command.UploadStatusCommand;
import org.apache.cloudstack.storage.command.UploadStatusCommand.EntityType;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreVO;
import org.apache.cloudstack.storage.image.deployasis.DeployAsIsHelper;
import org.apache.cloudstack.utils.identity.ManagementServerNode;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.alert.AlertManager;
import com.cloud.api.query.dao.TemplateJoinDao;
import com.cloud.api.query.vo.TemplateJoinVO;
import com.cloud.configuration.Resource;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.exception.ConnectionException;
import com.cloud.host.Host;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.Volume.Event;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.ResourceLimitService;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;

/**
 * Monitors the progress of upload.
 */
@Component
public class ImageStoreUploadMonitorImpl extends ManagerBase implements ImageStoreUploadMonitor, Listener, Configurable {

    static final Logger s_logger = Logger.getLogger(ImageStoreUploadMonitorImpl.class);

    @Inject
    private VolumeDao _volumeDao;
    @Inject
    private VolumeDataStoreDao _volumeDataStoreDao;
    @Inject
    private VMTemplateDao _templateDao;
    @Inject
    private TemplateDataStoreDao _templateDataStoreDao;
    @Inject
    private HostDao _hostDao;
    @Inject
    private EndPointSelector _epSelector;
    @Inject
    private DataStoreManager storeMgr;
    @Inject
    ResourceLimitService _resourceLimitMgr;
    @Inject
    private AlertManager _alertMgr;
    @Inject
    private VMTemplateZoneDao _vmTemplateZoneDao;
    @Inject
    private DataStoreManager dataStoreManager;
    @Inject
    private TemplateDataFactory templateFactory;
    @Inject
    private TemplateService templateService;
    @Inject
    private TemplateJoinDao templateJoinDao;
    @Inject
    private DeployAsIsHelper deployAsIsHelper;

    private long _nodeId;
    private ScheduledExecutorService _executor = null;
    private int _monitoringInterval;
    private long _uploadOperationTimeout;

    static final ConfigKey<Integer> UploadMonitoringInterval = new ConfigKey<Integer>("Advanced", Integer.class, "upload.monitoring.interval", "60",
            "Interval (in seconds) to check the status of volumes that are uploaded using HTTP POST request", true);
    static final ConfigKey<Integer> UploadOperationTimeout = new ConfigKey<Integer>("Advanced", Integer.class, "upload.operation.timeout", "60",
            "Time (in minutes) to wait before abandoning volume upload using HTTP POST request", true);

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("Upload-Monitor"));
        _monitoringInterval = UploadMonitoringInterval.value();
        _uploadOperationTimeout = UploadOperationTimeout.value() * 60 * 1000L;
        _nodeId = ManagementServerNode.getManagementServerId();
        return true;
    }

    @Override
    public boolean start() {
        _executor.scheduleWithFixedDelay(new UploadStatusCheck(), _monitoringInterval, _monitoringInterval, TimeUnit.SECONDS);
        return true;
    }

    @Override
    public boolean stop() {
        _executor.shutdownNow();
        return true;
    }

    @Override
    public boolean processAnswers(long agentId, long seq, Answer[] answers) {
        return false;
    }

    @Override
    public boolean processCommands(long agentId, long seq, Command[] commands) {
        return false;
    }

    @Override
    public AgentControlAnswer processControlCommand(long agentId, AgentControlCommand cmd) {
        return null;
    }

    @Override
    public boolean processDisconnect(long agentId, Status state) {
        return false;
    }

    @Override
    public void processHostAboutToBeRemoved(long hostId) {
    }

    @Override
    public void processHostRemoved(long hostId, long clusterId) {
    }

    @Override
    public boolean isRecurring() {
        return false;
    }

    @Override
    public int getTimeout() {
        return 0;
    }

    @Override
    public boolean processTimeout(long agentId, long seq) {
        return false;
    }

    @Override
    public void processHostAdded(long hostId) {
    }

    @Override
    public void processConnect(Host host, StartupCommand cmd, boolean forRebalance) throws ConnectionException {
    }

    protected class UploadStatusCheck extends ManagedContextRunnable {

        public UploadStatusCheck() {
        }

        @Override
        protected void runInContext() {
            // 1. Select all entries with download_state = Not_Downloaded or Download_In_Progress
            // 2. Get corresponding volume
            // 3. Get EP using _epSelector
            // 4. Check if SSVM is owned by this MS
            // 5. If owned by MS then send command to appropriate SSVM
            // 6. In listener check for the answer and update DB accordingly
            List<VolumeDataStoreVO> volumeDataStores = _volumeDataStoreDao.listByVolumeState(Volume.State.NotUploaded, Volume.State.UploadInProgress);
            for (VolumeDataStoreVO volumeDataStore : volumeDataStores) {
                try {
                    DataStore dataStore = storeMgr.getDataStore(volumeDataStore.getDataStoreId(), DataStoreRole.Image);
                    EndPoint ep = _epSelector.select(dataStore, volumeDataStore.getExtractUrl());
                    if (ep == null) {
                        s_logger.warn("There is no secondary storage VM for image store " + dataStore.getName());
                        continue;
                    }
                    VolumeVO volume = _volumeDao.findById(volumeDataStore.getVolumeId());
                    if (volume == null) {
                        s_logger.warn("Volume with id " + volumeDataStore.getVolumeId() + " not found");
                        continue;
                    }
                    Host host = _hostDao.findById(ep.getId());
                    UploadStatusCommand cmd = new UploadStatusCommand(volume.getUuid(), EntityType.Volume);
                    if (host != null && host.getManagementServerId() != null) {
                        if (_nodeId == host.getManagementServerId().longValue()) {
                            Answer answer = null;
                            try {
                                answer = ep.sendMessage(cmd);
                            } catch (CloudRuntimeException e) {
                                s_logger.warn("Unable to get upload status for volume " + volume.getUuid() + ". Error details: " + e.getMessage());
                                answer = new UploadStatusAnswer(cmd, UploadStatus.UNKNOWN, e.getMessage());
                            }
                            if (answer == null || !(answer instanceof UploadStatusAnswer)) {
                                s_logger.warn("No or invalid answer corresponding to UploadStatusCommand for volume " + volumeDataStore.getVolumeId());
                                continue;
                            }
                            handleVolumeStatusResponse((UploadStatusAnswer)answer, volume, volumeDataStore);
                        }
                    } else {
                        String error = "Volume " + volume.getUuid() + " failed to upload as SSVM is either destroyed or SSVM agent not in 'Up' state";
                        handleVolumeStatusResponse(new UploadStatusAnswer(cmd, UploadStatus.ERROR, error), volume, volumeDataStore);
                    }
                } catch (Throwable th) {
                    s_logger.warn("Exception while checking status for uploaded volume " + volumeDataStore.getExtractUrl() + ". Error details: " + th.getMessage());
                    if (s_logger.isTraceEnabled()) {
                        s_logger.trace("Exception details: ", th);
                    }
                }
            }

            // Handle for template upload as well
            List<TemplateDataStoreVO> templateDataStores = _templateDataStoreDao.listByTemplateState(VirtualMachineTemplate.State.NotUploaded, VirtualMachineTemplate.State.UploadInProgress);
            for (TemplateDataStoreVO templateDataStore : templateDataStores) {
                try {
                    DataStore dataStore = storeMgr.getDataStore(templateDataStore.getDataStoreId(), DataStoreRole.Image);
                    EndPoint ep = _epSelector.select(dataStore, templateDataStore.getExtractUrl());
                    if (ep == null) {
                        s_logger.warn("There is no secondary storage VM for image store " + dataStore.getName());
                        continue;
                    }
                    VMTemplateVO template = _templateDao.findById(templateDataStore.getTemplateId());
                    if (template == null) {
                        s_logger.warn("Template with id " + templateDataStore.getTemplateId() + " not found");
                        continue;
                    }
                    Host host = _hostDao.findById(ep.getId());
                    UploadStatusCommand cmd = new UploadStatusCommand(template.getUuid(), EntityType.Template);
                    if (host != null && host.getManagementServerId() != null) {
                        if (_nodeId == host.getManagementServerId().longValue()) {
                            Answer answer = null;
                            try {
                                answer = ep.sendMessage(cmd);
                            } catch (CloudRuntimeException e) {
                                s_logger.warn("Unable to get upload status for template " + template.getUuid() + ". Error details: " + e.getMessage());
                                answer = new UploadStatusAnswer(cmd, UploadStatus.UNKNOWN, e.getMessage());
                            }
                            if (answer == null || !(answer instanceof UploadStatusAnswer)) {
                                s_logger.warn("No or invalid answer corresponding to UploadStatusCommand for template " + templateDataStore.getTemplateId());
                                continue;
                            }
                            handleTemplateStatusResponse((UploadStatusAnswer)answer, template, templateDataStore);
                        }
                    } else {
                        String error = "Template " + template.getUuid() + " failed to upload as SSVM is either destroyed or SSVM agent not in 'Up' state";
                        handleTemplateStatusResponse(new UploadStatusAnswer(cmd, UploadStatus.ERROR, error), template, templateDataStore);
                    }
                } catch (Throwable th) {
                    s_logger.warn("Exception while checking status for uploaded template " + templateDataStore.getExtractUrl() + ". Error details: " + th.getMessage());
                    if (s_logger.isTraceEnabled()) {
                        s_logger.trace("Exception details: ", th);
                    }
                }
            }
        }

        private void handleVolumeStatusResponse(final UploadStatusAnswer answer, final VolumeVO volume, final VolumeDataStoreVO volumeDataStore) {
            final StateMachine2<Volume.State, Event, Volume> stateMachine = Volume.State.getStateMachine();
            Transaction.execute(new TransactionCallbackNoReturn() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) {
                    VolumeVO tmpVolume = _volumeDao.findById(volume.getId());
                    VolumeDataStoreVO tmpVolumeDataStore = _volumeDataStoreDao.findById(volumeDataStore.getId());
                    boolean sendAlert = false;
                    String msg = null;
                    try {
                        switch (answer.getStatus()) {
                        case COMPLETED:
                            tmpVolumeDataStore.setDownloadState(VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
                            tmpVolumeDataStore.setState(State.Ready);
                            tmpVolumeDataStore.setInstallPath(answer.getInstallPath());
                            tmpVolumeDataStore.setPhysicalSize(answer.getPhysicalSize());
                            tmpVolumeDataStore.setSize(answer.getVirtualSize());
                            tmpVolumeDataStore.setDownloadPercent(100);

                            VolumeVO volumeUpdate = _volumeDao.createForUpdate();
                            volumeUpdate.setSize(answer.getVirtualSize());
                            _volumeDao.update(tmpVolume.getId(), volumeUpdate);
                            stateMachine.transitTo(tmpVolume, Event.OperationSucceeded, null, _volumeDao);
                            _resourceLimitMgr.incrementResourceCount(volume.getAccountId(), Resource.ResourceType.secondary_storage, answer.getVirtualSize());

                            // publish usage events
                            UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VOLUME_UPLOAD, tmpVolume.getAccountId(),
                                    tmpVolume.getDataCenterId(), tmpVolume.getId(), tmpVolume.getName(),
                                    null, null, tmpVolumeDataStore.getPhysicalSize(), tmpVolumeDataStore.getSize(),
                                    Volume.class.getName(), tmpVolume.getUuid());

                            if (s_logger.isDebugEnabled()) {
                                s_logger.debug("Volume " + tmpVolume.getUuid() + " uploaded successfully");
                            }
                            break;
                        case IN_PROGRESS:
                            if (tmpVolume.getState() == Volume.State.NotUploaded) {
                                tmpVolumeDataStore.setDownloadState(VMTemplateStorageResourceAssoc.Status.DOWNLOAD_IN_PROGRESS);
                                tmpVolumeDataStore.setDownloadPercent(answer.getDownloadPercent());
                                stateMachine.transitTo(tmpVolume, Event.UploadRequested, null, _volumeDao);
                            } else if (tmpVolume.getState() == Volume.State.UploadInProgress) { // check for timeout
                                if (System.currentTimeMillis() - tmpVolumeDataStore.getCreated().getTime() > _uploadOperationTimeout) {
                                    tmpVolumeDataStore.setDownloadState(VMTemplateStorageResourceAssoc.Status.DOWNLOAD_ERROR);
                                    tmpVolumeDataStore.setState(State.Failed);
                                    stateMachine.transitTo(tmpVolume, Event.OperationFailed, null, _volumeDao);
                                    msg = "Volume " + tmpVolume.getUuid() + " failed to upload due to operation timed out";
                                    s_logger.error(msg);
                                    sendAlert = true;
                                } else {
                                    tmpVolumeDataStore.setDownloadPercent(answer.getDownloadPercent());
                                }
                            }
                            break;
                        case ERROR:
                            tmpVolumeDataStore.setDownloadState(VMTemplateStorageResourceAssoc.Status.DOWNLOAD_ERROR);
                            tmpVolumeDataStore.setState(State.Failed);
                            stateMachine.transitTo(tmpVolume, Event.OperationFailed, null, _volumeDao);
                            msg = "Volume " + tmpVolume.getUuid() + " failed to upload. Error details: " + answer.getDetails();
                            s_logger.error(msg);
                            sendAlert = true;
                            break;
                        case UNKNOWN:
                            if (tmpVolume.getState() == Volume.State.NotUploaded) { // check for timeout
                                if (System.currentTimeMillis() - tmpVolumeDataStore.getCreated().getTime() > _uploadOperationTimeout) {
                                    tmpVolumeDataStore.setDownloadState(VMTemplateStorageResourceAssoc.Status.ABANDONED);
                                    tmpVolumeDataStore.setState(State.Failed);
                                    stateMachine.transitTo(tmpVolume, Event.OperationTimeout, null, _volumeDao);
                                    msg = "Volume " + tmpVolume.getUuid() + " failed to upload due to operation timed out";
                                    s_logger.error(msg);
                                    sendAlert = true;
                                }
                            }
                            break;
                        }
                        _volumeDataStoreDao.update(tmpVolumeDataStore.getId(), tmpVolumeDataStore);
                    } catch (NoTransitionException e) {
                        s_logger.error("Unexpected error " + e.getMessage());
                    } finally {
                        if (sendAlert) {
                            _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_UPLOAD_FAILED, tmpVolume.getDataCenterId(), null, msg, msg);
                        }
                    }
                }
            });
        }

        private void handleTemplateStatusResponse(final UploadStatusAnswer answer, final VMTemplateVO template, final TemplateDataStoreVO templateDataStore) {
            final StateMachine2<VirtualMachineTemplate.State, VirtualMachineTemplate.Event, VirtualMachineTemplate> stateMachine = VirtualMachineTemplate.State.getStateMachine();
            Transaction.execute(new TransactionCallbackNoReturn() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) {
                    VMTemplateVO tmpTemplate = _templateDao.findById(template.getId());
                    TemplateDataStoreVO tmpTemplateDataStore = _templateDataStoreDao.findById(templateDataStore.getId());
                    boolean sendAlert = false;
                    String msg = null;
                    try {
                        switch (answer.getStatus()) {
                        case COMPLETED:
                            tmpTemplateDataStore.setDownloadState(VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
                            tmpTemplateDataStore.setState(State.Ready);
                            tmpTemplateDataStore.setInstallPath(answer.getInstallPath());
                            tmpTemplateDataStore.setPhysicalSize(answer.getPhysicalSize());
                            tmpTemplateDataStore.setSize(answer.getVirtualSize());
                            tmpTemplateDataStore.setDownloadPercent(100);
                            tmpTemplateDataStore.setExtractUrl(null);

                            VMTemplateVO templateUpdate = _templateDao.createForUpdate();
                            templateUpdate.setSize(answer.getVirtualSize());
                            if (template.getHypervisorType() == Hypervisor.HypervisorType.VMware) {
                                Pair<String, String> guestOsInfo = answer.getGuestOsInfo();
                                String minimumHardwareVersion = answer.getMinimumHardwareVersion();
                                String osType = guestOsInfo.first();
                                String osDescription = guestOsInfo.second();
                                s_logger.info("Guest OS information retrieved from the template: " + osType + " - " + osDescription);
                                try {
                                    Long guestOsId = deployAsIsHelper.retrieveTemplateGuestOsIdFromGuestOsInfo(template.getId(),
                                            osType, osDescription, minimumHardwareVersion);
                                    templateUpdate.setGuestOSId(guestOsId);
                                } catch (CloudRuntimeException e) {
                                    s_logger.error("Could not map the guest OS to a CloudStack guest OS", e);
                                }
                            }
                            _templateDao.update(tmpTemplate.getId(), templateUpdate);
                            // For multi-disk OVA, check and create data disk templates or root disks as details
                            if (tmpTemplate.getFormat().equals(Storage.ImageFormat.OVA)) {
                                final DataStore store = dataStoreManager.getDataStore(templateDataStore.getDataStoreId(), templateDataStore.getDataStoreRole());
                                final TemplateInfo templateInfo = templateFactory.getTemplate(tmpTemplate.getId(), store);
                                if (!templateService.createOvaDataDiskTemplates(templateInfo, template.isDeployAsIs())) {
                                    tmpTemplateDataStore.setDownloadState(VMTemplateStorageResourceAssoc.Status.ABANDONED);
                                    tmpTemplateDataStore.setState(State.Failed);
                                    stateMachine.transitTo(tmpTemplate, VirtualMachineTemplate.Event.OperationFailed, null, _templateDao);
                                    msg = "Multi-disk OVA template " + tmpTemplate.getUuid() + " failed to process data disks";
                                    s_logger.error(msg);
                                    sendAlert = true;
                                    break;
                                }
                            }
                            stateMachine.transitTo(tmpTemplate, VirtualMachineTemplate.Event.OperationSucceeded, null, _templateDao);
                            _resourceLimitMgr.incrementResourceCount(template.getAccountId(), Resource.ResourceType.secondary_storage, answer.getVirtualSize());
                            //publish usage event
                            String etype = EventTypes.EVENT_TEMPLATE_CREATE;
                            if (tmpTemplate.getFormat() == Storage.ImageFormat.ISO) {
                                etype = EventTypes.EVENT_ISO_CREATE;
                            }
                            TemplateJoinVO vo = templateJoinDao.findById(tmpTemplate.getId());
                            assert (vo != null) : "Couldn't find the template view for given template ID";
                            UsageEventUtils.publishUsageEvent(etype, tmpTemplate.getAccountId(), vo.getDataCenterId(), tmpTemplate.getId(), tmpTemplate.getName(), null, null,
                                    tmpTemplateDataStore.getPhysicalSize(), tmpTemplateDataStore.getSize(), VirtualMachineTemplate.class.getName(), tmpTemplate.getUuid());

                            if (s_logger.isDebugEnabled()) {
                                s_logger.debug("Template " + tmpTemplate.getUuid() + " uploaded successfully");
                            }
                            break;
                        case IN_PROGRESS:
                            if (tmpTemplate.getState() == VirtualMachineTemplate.State.NotUploaded) {
                                tmpTemplateDataStore.setDownloadState(VMTemplateStorageResourceAssoc.Status.DOWNLOAD_IN_PROGRESS);
                                stateMachine.transitTo(tmpTemplate, VirtualMachineTemplate.Event.UploadRequested, null, _templateDao);
                                tmpTemplateDataStore.setDownloadPercent(answer.getDownloadPercent());
                            } else if (tmpTemplate.getState() == VirtualMachineTemplate.State.UploadInProgress) { // check for timeout
                                if (System.currentTimeMillis() - tmpTemplateDataStore.getCreated().getTime() > _uploadOperationTimeout) {
                                    tmpTemplateDataStore.setDownloadState(VMTemplateStorageResourceAssoc.Status.DOWNLOAD_ERROR);
                                    tmpTemplateDataStore.setState(State.Failed);
                                    stateMachine.transitTo(tmpTemplate, VirtualMachineTemplate.Event.OperationFailed, null, _templateDao);
                                    msg = "Template " + tmpTemplate.getUuid() + " failed to upload due to operation timed out";
                                    s_logger.error(msg);
                                    sendAlert = true;
                                } else {
                                    tmpTemplateDataStore.setDownloadPercent(answer.getDownloadPercent());
                                }
                            }
                            break;
                        case ERROR:
                            tmpTemplateDataStore.setDownloadState(VMTemplateStorageResourceAssoc.Status.DOWNLOAD_ERROR);
                            tmpTemplateDataStore.setState(State.Failed);
                            stateMachine.transitTo(tmpTemplate, VirtualMachineTemplate.Event.OperationFailed, null, _templateDao);
                            msg = "Template " + tmpTemplate.getUuid() + " failed to upload. Error details: " + answer.getDetails();
                            s_logger.error(msg);
                            sendAlert = true;
                            break;
                        case UNKNOWN:
                            if (tmpTemplate.getState() == VirtualMachineTemplate.State.NotUploaded) { // check for timeout
                                if (System.currentTimeMillis() - tmpTemplateDataStore.getCreated().getTime() > _uploadOperationTimeout) {
                                    tmpTemplateDataStore.setDownloadState(VMTemplateStorageResourceAssoc.Status.ABANDONED);
                                    tmpTemplateDataStore.setState(State.Failed);
                                    stateMachine.transitTo(tmpTemplate, VirtualMachineTemplate.Event.OperationTimeout, null, _templateDao);
                                    msg = "Template " + tmpTemplate.getUuid() + " failed to upload due to operation timed out";
                                    s_logger.error(msg);
                                    sendAlert = true;
                                }
                            }
                            break;
                        }
                        _templateDataStoreDao.update(tmpTemplateDataStore.getId(), tmpTemplateDataStore);
                    } catch (NoTransitionException e) {
                        s_logger.error("Unexpected error " + e.getMessage());
                    } finally {
                        if (sendAlert) {
                            _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_UPLOAD_FAILED,
                                    _vmTemplateZoneDao.listByTemplateId(tmpTemplate.getId()).get(0).getZoneId(), null, msg, msg);
                        }
                    }
                }
            });
        }

    }

    @Override
    public String getConfigComponentName() {
        return ImageStoreUploadMonitor.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {UploadMonitoringInterval, UploadOperationTimeout};
    }

    public static int getUploadOperationTimeout() {
        return UploadOperationTimeout.value();
    }

}
