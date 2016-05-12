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

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

import javax.inject.Inject;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import org.apache.cloudstack.api.command.user.iso.ExtractIsoCmd;
import org.apache.cloudstack.api.command.user.template.ExtractTemplateCmd;
import org.apache.cloudstack.api.command.user.volume.ExtractVolumeCmd;
import org.apache.cloudstack.api.response.ExtractResponse;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.jobs.JobInfo;
import org.apache.cloudstack.managed.context.ManagedContextTimerTask;

import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupStorageCommand;
import com.cloud.agent.api.storage.UploadAnswer;
import com.cloud.agent.api.storage.UploadCommand;
import com.cloud.agent.api.storage.UploadProgressCommand;
import com.cloud.agent.api.storage.UploadProgressCommand.RequestType;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.ApiSerializerHelper;
import com.cloud.host.Host;
import com.cloud.storage.Storage;
import com.cloud.storage.Upload.Status;
import com.cloud.storage.Upload.Type;
import com.cloud.storage.UploadVO;
import com.cloud.storage.dao.UploadDao;
import com.cloud.storage.upload.UploadState.UploadEvent;
import com.cloud.utils.exception.CloudRuntimeException;

public class UploadListener implements Listener {

    private static final class StatusTask extends ManagedContextTimerTask {
        private final UploadListener ul;
        private final RequestType reqType;

        public StatusTask(UploadListener ul, RequestType req) {
            reqType = req;
            this.ul = ul;
        }

        @Override
        protected void runInContext() {
            ul.sendCommand(reqType);

        }
    }

    private static final class TimeoutTask extends ManagedContextTimerTask {
        private final UploadListener ul;

        public TimeoutTask(UploadListener ul) {
            this.ul = ul;
        }

        @Override
        protected void runInContext() {
            ul.checkProgress();
        }
    }

    public static final Logger s_logger = Logger.getLogger(UploadListener.class.getName());
    public static final int SMALL_DELAY = 100;
    public static final long STATUS_POLL_INTERVAL = 10000L;

    public static final String UPLOADED = Status.UPLOADED.toString();
    public static final String NOT_UPLOADED = Status.NOT_UPLOADED.toString();
    public static final String UPLOAD_ERROR = Status.UPLOAD_ERROR.toString();
    public static final String UPLOAD_IN_PROGRESS = Status.UPLOAD_IN_PROGRESS.toString();
    public static final String UPLOAD_ABANDONED = Status.ABANDONED.toString();
    public static final Map<String, String> responseNameMap;
    static {
        Map<String, String> tempMap = new HashMap<String, String>();
        tempMap.put(Type.ISO.toString(), ExtractIsoCmd.getStaticName());
        tempMap.put(Type.TEMPLATE.toString(), ExtractTemplateCmd.getStaticName());
        tempMap.put(Type.VOLUME.toString(), ExtractVolumeCmd.getStaticName());
        tempMap.put("DEFAULT", "extractresponse");
        responseNameMap = Collections.unmodifiableMap(tempMap);
    }

    private DataStore sserver;

    private UploadDao uploadDao;

    private final UploadMonitorImpl uploadMonitor;

    private UploadState currState;

    private UploadCommand cmd;

    private Timer timer;

    private StatusTask statusTask;
    private TimeoutTask timeoutTask;
    private Date lastUpdated = new Date();
    private String jobId;
    private Long accountId;
    private String typeName;
    private Type type;
    private long asyncJobId;
    private long eventId;
    private AsyncJobManager asyncMgr;
    private ExtractResponse resultObj;
    @Inject
    EndPointSelector _epSelector;

    public AsyncJobManager getAsyncMgr() {
        return asyncMgr;
    }

    public void setAsyncMgr(AsyncJobManager asyncMgr) {
        this.asyncMgr = asyncMgr;
    }

    public long getAsyncJobId() {
        return asyncJobId;
    }

    public void setAsyncJobId(long asyncJobId) {
        this.asyncJobId = asyncJobId;
    }

    public long getEventId() {
        return eventId;
    }

    public void setEventId(long eventId) {
        this.eventId = eventId;
    }

    private final Map<String, UploadState> stateMap = new HashMap<String, UploadState>();
    private Long uploadId;

    public UploadListener(DataStore host, Timer timerInput, UploadDao uploadDao, UploadVO uploadObj, UploadMonitorImpl uploadMonitor, UploadCommand cmd, Long accountId,
            String typeName, Type type, long eventId, long asyncJobId, AsyncJobManager asyncMgr) {
        sserver = host;
        this.uploadDao = uploadDao;
        this.uploadMonitor = uploadMonitor;
        this.cmd = cmd;
        uploadId = uploadObj.getId();
        this.accountId = accountId;
        this.typeName = typeName;
        this.type = type;
        initStateMachine();
        currState = getState(Status.NOT_UPLOADED.toString());
        timer = timerInput;
        timeoutTask = new TimeoutTask(this);
        timer.schedule(timeoutTask, 3 * STATUS_POLL_INTERVAL);
        this.eventId = eventId;
        this.asyncJobId = asyncJobId;
        this.asyncMgr = asyncMgr;
        String extractId = null;
        if (type == Type.VOLUME) {
            extractId = ApiDBUtils.findVolumeById(uploadObj.getTypeId()).getUuid();
        } else {
            extractId = ApiDBUtils.findTemplateById(uploadObj.getTypeId()).getUuid();
        }
        resultObj =
            new ExtractResponse(extractId, typeName, ApiDBUtils.findAccountById(accountId).getUuid(), Status.NOT_UPLOADED.toString(), ApiDBUtils.findUploadById(uploadId)
                .getUuid());
        resultObj.setResponseName(responseNameMap.get(type.toString()));
        updateDatabase(Status.NOT_UPLOADED, cmd.getUrl(), "");
    }

    public UploadListener(UploadMonitorImpl monitor) {
        uploadMonitor = monitor;
    }

    public void checkProgress() {
        transition(UploadEvent.TIMEOUT_CHECK, null);
    }

    @Override
    public int getTimeout() {
        return -1;
    }

    @Override
    public boolean isRecurring() {
        return false;
    }

    public void setCommand(UploadCommand cmd) {
        this.cmd = cmd;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getJobId() {
        return jobId;
    }

    @Override
    public boolean processAnswers(long agentId, long seq, Answer[] answers) {
        boolean processed = false;
        if (answers != null & answers.length > 0) {
            if (answers[0] instanceof UploadAnswer) {
                final UploadAnswer answer = (UploadAnswer)answers[0];
                if (getJobId() == null) {
                    setJobId(answer.getJobId());
                } else if (!getJobId().equalsIgnoreCase(answer.getJobId())) {
                    return false;//TODO
                }
                transition(UploadEvent.UPLOAD_ANSWER, answer);
                processed = true;
            }
        }
        return processed;
    }

    @Override
    public boolean processCommands(long agentId, long seq, Command[] commands) {
        return false;
    }

    @Override
    public void processHostAdded(long hostId) {
    }

    @Override
    public void processConnect(Host agent, StartupCommand cmd, boolean forRebalance) {
        if (!(cmd instanceof StartupStorageCommand)) {
            return;
        }

        long agentId = agent.getId();

        StartupStorageCommand storage = (StartupStorageCommand)cmd;
        if (storage.getResourceType() == Storage.StorageResourceType.STORAGE_HOST || storage.getResourceType() == Storage.StorageResourceType.SECONDARY_STORAGE) {
            uploadMonitor.handleUploadSync(agentId);
        }
    }

    @Override
    public AgentControlAnswer processControlCommand(long agentId, AgentControlCommand cmd) {
        return null;
    }

    public void setUploadInactive(Status reason) {
        uploadMonitor.handleUploadEvent(accountId, typeName, type, uploadId, reason, eventId);
    }

    public void logUploadStart() {
        //uploadMonitor.logEvent(accountId, event, "Storage server " + sserver.getName() + " started upload of " +type.toString() + " " + typeName, EventVO.LEVEL_INFO, eventId);
    }

    public void cancelTimeoutTask() {
        if (timeoutTask != null)
            timeoutTask.cancel();
    }

    public void cancelStatusTask() {
        if (statusTask != null)
            statusTask.cancel();
    }

    @Override
    public boolean processDisconnect(long agentId, com.cloud.host.Status state) {
        setDisconnected();
        return true;
    }

    @Override
    public void processHostAboutToBeRemoved(long hostId) {
    }

    @Override
    public void processHostRemoved(long hostId, long clusterId) {
    }

    @Override
    public boolean processTimeout(long agentId, long seq) {
        return true;
    }

    private void initStateMachine() {
        stateMap.put(Status.NOT_UPLOADED.toString(), new NotUploadedState(this));
        stateMap.put(Status.UPLOADED.toString(), new UploadCompleteState(this));
        stateMap.put(Status.UPLOAD_ERROR.toString(), new UploadErrorState(this));
        stateMap.put(Status.UPLOAD_IN_PROGRESS.toString(), new UploadInProgressState(this));
        stateMap.put(Status.ABANDONED.toString(), new UploadAbandonedState(this));
    }

    private UploadState getState(String stateName) {
        return stateMap.get(stateName);
    }

    private synchronized void transition(UploadEvent event, Object evtObj) {
        if (currState == null) {
            return;
        }
        String prevName = currState.getName();
        String nextState = currState.handleEvent(event, evtObj);
        if (nextState != null) {
            currState = getState(nextState);
            if (currState != null) {
                currState.onEntry(prevName, event, evtObj);
            } else {
                throw new CloudRuntimeException("Invalid next state: currState=" + prevName + ", evt=" + event + ", next=" + nextState);
            }
        } else {
            throw new CloudRuntimeException("Unhandled event transition: currState=" + prevName + ", evt=" + event);
        }
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated() {
        lastUpdated = new Date();
    }

    public void log(String message, Level level) {
        s_logger.log(level, message + ", " + type.toString() + " = " + typeName + " at host " + sserver.getName());
    }

    public void setDisconnected() {
        transition(UploadEvent.DISCONNECT, null);
    }

    public void scheduleStatusCheck(com.cloud.agent.api.storage.UploadProgressCommand.RequestType getStatus) {
        if (statusTask != null)
            statusTask.cancel();

        statusTask = new StatusTask(this, getStatus);
        timer.schedule(statusTask, STATUS_POLL_INTERVAL);
    }

    public void scheduleTimeoutTask(long delay) {
        if (timeoutTask != null)
            timeoutTask.cancel();

        timeoutTask = new TimeoutTask(this);
        timer.schedule(timeoutTask, delay);
        if (s_logger.isDebugEnabled()) {
            log("Scheduling timeout at " + delay + " ms", Level.DEBUG);
        }
    }

    public void updateDatabase(Status state, String uploadErrorString) {
        resultObj.setResultString(uploadErrorString);
        resultObj.setState(state.toString());
        asyncMgr.updateAsyncJobAttachment(asyncJobId, type.toString(), 1L);
        asyncMgr.updateAsyncJobStatus(asyncJobId, JobInfo.Status.IN_PROGRESS.ordinal(), ApiSerializerHelper.toSerializedString(resultObj));

        UploadVO vo = uploadDao.createForUpdate();
        vo.setUploadState(state);
        vo.setLastUpdated(new Date());
        vo.setErrorString(uploadErrorString);
        uploadDao.update(getUploadId(), vo);
    }

    public void updateDatabase(Status state, String uploadUrl, String uploadErrorString) {
        resultObj.setResultString(uploadErrorString);
        resultObj.setState(state.toString());
        asyncMgr.updateAsyncJobAttachment(asyncJobId, type.toString(), 1L);
        asyncMgr.updateAsyncJobStatus(asyncJobId, JobInfo.Status.IN_PROGRESS.ordinal(), ApiSerializerHelper.toSerializedString(resultObj));

        UploadVO vo = uploadDao.createForUpdate();
        vo.setUploadState(state);
        vo.setLastUpdated(new Date());
        vo.setUploadUrl(uploadUrl);
        vo.setJobId(null);
        vo.setUploadPercent(0);
        vo.setErrorString(uploadErrorString);

        uploadDao.update(getUploadId(), vo);
    }

    private Long getUploadId() {
        return uploadId;
    }

    public synchronized void updateDatabase(UploadAnswer answer) {

        if (answer.getErrorString().startsWith("553")) {
            answer.setErrorString(answer.getErrorString().concat("Please check if the file name already exists."));
        }
        resultObj.setResultString(answer.getErrorString());
        resultObj.setState(answer.getUploadStatus().toString());
        resultObj.setUploadPercent(answer.getUploadPct());

        if (answer.getUploadStatus() == Status.UPLOAD_IN_PROGRESS) {
            asyncMgr.updateAsyncJobAttachment(asyncJobId, type.toString(), 1L);
            asyncMgr.updateAsyncJobStatus(asyncJobId, JobInfo.Status.IN_PROGRESS.ordinal(), ApiSerializerHelper.toSerializedString(resultObj));
        } else if (answer.getUploadStatus() == Status.UPLOADED) {
            resultObj.setResultString("Success");
            asyncMgr.completeAsyncJob(asyncJobId, JobInfo.Status.SUCCEEDED, 1, ApiSerializerHelper.toSerializedString(resultObj));
        } else {
            asyncMgr.completeAsyncJob(asyncJobId, JobInfo.Status.FAILED, 2, ApiSerializerHelper.toSerializedString(resultObj));
        }
        UploadVO updateBuilder = uploadDao.createForUpdate();
        updateBuilder.setUploadPercent(answer.getUploadPct());
        updateBuilder.setUploadState(answer.getUploadStatus());
        updateBuilder.setLastUpdated(new Date());
        updateBuilder.setErrorString(answer.getErrorString());
        updateBuilder.setJobId(answer.getJobId());

        uploadDao.update(getUploadId(), updateBuilder);
    }

    public void sendCommand(RequestType reqType) {
        if (getJobId() != null) {
            if (s_logger.isTraceEnabled()) {
                log("Sending progress command ", Level.TRACE);
            }
            try {
                EndPoint ep = _epSelector.select(sserver);
                if (ep == null) {
                    String errMsg = "No remote endpoint to send command, check if host or ssvm is down?";
                    s_logger.error(errMsg);
                    return;
                }
                ep.sendMessageAsync(new UploadProgressCommand(getCommand(), getJobId(), reqType), new Callback(ep.getId(), this));
            } catch (Exception e) {
                s_logger.debug("Send command failed", e);
                setDisconnected();
            }
        }

    }

    private UploadCommand getCommand() {
        return cmd;
    }

    public void logDisconnect() {
        s_logger.warn("Unable to monitor upload progress of " + typeName + " at host " + sserver.getName());
    }

    public void scheduleImmediateStatusCheck(RequestType request) {
        if (statusTask != null)
            statusTask.cancel();
        statusTask = new StatusTask(this, request);
        timer.schedule(statusTask, SMALL_DELAY);
    }

    public void setCurrState(Status uploadState) {
        currState = getState(currState.toString());
    }

    public static class Callback implements AsyncCompletionCallback<Answer> {
        long id;
        Listener listener;

        public Callback(long id, Listener listener) {
            this.id = id;
            this.listener = listener;
        }

        @Override
        public void complete(Answer answer) {
            listener.processAnswers(id, -1, new Answer[] {answer});
        }
    }
}
