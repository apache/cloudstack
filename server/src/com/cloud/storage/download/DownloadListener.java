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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateService;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreDao;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.agent.api.StartupSecondaryStorageCommand;
import com.cloud.agent.api.StartupStorageCommand;
import com.cloud.agent.api.storage.DownloadAnswer;
import com.cloud.agent.api.storage.DownloadCommand;
import com.cloud.agent.api.storage.DownloadCommand.ResourceType;
import com.cloud.agent.api.storage.DownloadProgressCommand;
import com.cloud.agent.api.storage.DownloadProgressCommand.RequestType;
import com.cloud.alert.AlertManager;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConnectionException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.resource.ResourceManager;
import com.cloud.storage.Storage;
import com.cloud.storage.StorageManager;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeHostVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeHostDao;
import com.cloud.storage.download.DownloadState.DownloadEvent;
import com.cloud.user.AccountManager;
import com.cloud.user.ResourceLimitService;
import com.cloud.utils.UriUtils;
import com.cloud.utils.exception.CloudRuntimeException;

/**
 * Monitor progress of template download to a single storage server
 *
 */
public class DownloadListener implements Listener {


	private static final class StatusTask extends TimerTask {
		private final DownloadListener dl;
		private final RequestType reqType;

		public StatusTask( DownloadListener dl,  RequestType req) {
			this.reqType = req;
			this.dl = dl;
		}

		@Override
		public void run() {
		  dl.sendCommand(reqType);

		}
	}

	private static final class TimeoutTask extends TimerTask {
		private final DownloadListener dl;

		public TimeoutTask( DownloadListener dl) {
			this.dl = dl;
		}

		@Override
		public void run() {
		  dl.checkProgress();
		}
	}


	public static final Logger s_logger = Logger.getLogger(DownloadListener.class.getName());
	public static final int SMALL_DELAY = 100;
    public static final long STATUS_POLL_INTERVAL = 10000L;

	public static final String DOWNLOADED=Status.DOWNLOADED.toString();
	public static final String NOT_DOWNLOADED=Status.NOT_DOWNLOADED.toString();
	public static final String DOWNLOAD_ERROR=Status.DOWNLOAD_ERROR.toString();
	public static final String DOWNLOAD_IN_PROGRESS=Status.DOWNLOAD_IN_PROGRESS.toString();
	public static final String DOWNLOAD_ABANDONED=Status.ABANDONED.toString();


	private HostVO _sserver;
	private HostVO _ssAgent;

	private VMTemplateVO _template;
	private VolumeVO _volume;
	private boolean _downloadActive = true;

	private VolumeHostDao _volumeHostDao;
	private VolumeDataStoreDao _volumeStoreDao;
	private VolumeDao _volumeDao;
	private StorageManager _storageMgr;
	private VMTemplateHostDao _vmTemplateHostDao;
	private TemplateDataStoreDao _vmTemplateStoreDao;
	private VMTemplateDao _vmTemplateDao;
	private ResourceLimitService _resourceLimitMgr;
	private AccountManager _accountMgr;
	private AlertManager _alertMgr;

	private final DownloadMonitorImpl _downloadMonitor;

	private DownloadState _currState;

	private DownloadCommand _cmd;

	private Timer _timer;

	private StatusTask _statusTask;
	private TimeoutTask _timeoutTask;
	private Date _lastUpdated = new Date();
	private String _jobId;

	private final Map<String,  DownloadState> _stateMap = new HashMap<String, DownloadState>();
	private Long _templateHostId;
	private Long _volumeHostId;

    private DataStore _sstore;
    private Long _templateStoreId;
    private Long _volumeStoreId;
	private AsyncCompletionCallback<CreateCmdResult> _callback;

    @Inject
    private ResourceManager _resourceMgr;
    @Inject
    private TemplateService _imageSrv;
    @Inject
    private DataStoreManager _storeMgr;
    @Inject
    private VolumeService _volumeSrv;

	public DownloadListener(HostVO ssAgent, HostVO host, VMTemplateVO template, Timer _timer, VMTemplateHostDao dao, Long templHostId, DownloadMonitorImpl downloadMonitor, DownloadCommand cmd, VMTemplateDao templateDao, ResourceLimitService _resourceLimitMgr, AlertManager _alertMgr, AccountManager _accountMgr) {
	    this._ssAgent = ssAgent;
        this._sserver = host;
		this._template = template;
		this._vmTemplateHostDao = dao;
		this._downloadMonitor = downloadMonitor;
		this._cmd = cmd;
		this._templateHostId = templHostId;
		initStateMachine();
		this._currState=getState(Status.NOT_DOWNLOADED.toString());
		this._timer = _timer;
		this._timeoutTask = new TimeoutTask(this);
		this._timer.schedule(_timeoutTask, 3*STATUS_POLL_INTERVAL);
		this._vmTemplateDao = templateDao;
		this._resourceLimitMgr = _resourceLimitMgr;
		this._accountMgr = _accountMgr;
		this._alertMgr = _alertMgr;
		updateDatabase(Status.NOT_DOWNLOADED, "");
	}

	// TODO: this constructor should be the one used for template only, remove other template constructor later
    public DownloadListener(HostVO ssAgent, DataStore store, VMTemplateVO template, Timer _timer, TemplateDataStoreDao dao, Long templStoreId, DownloadMonitorImpl downloadMonitor, DownloadCommand cmd, VMTemplateDao templateDao, ResourceLimitService _resourceLimitMgr, AlertManager _alertMgr, AccountManager _accountMgr, AsyncCompletionCallback<CreateCmdResult> callback) {
        this._ssAgent = ssAgent;
        this._sstore = store;
        this._template = template;
        this._vmTemplateStoreDao = dao;
        this._downloadMonitor = downloadMonitor;
        this._cmd = cmd;
        this._templateStoreId = templStoreId;
        initStateMachine();
        this._currState=getState(Status.NOT_DOWNLOADED.toString());
        this._timer = _timer;
        this._timeoutTask = new TimeoutTask(this);
        this._timer.schedule(_timeoutTask, 3*STATUS_POLL_INTERVAL);
        this._vmTemplateDao = templateDao;
        this._resourceLimitMgr = _resourceLimitMgr;
        this._accountMgr = _accountMgr;
        this._alertMgr = _alertMgr;
        this._callback = callback;
        updateDatabase(Status.NOT_DOWNLOADED, "");
    }


	public DownloadListener(HostVO ssAgent, DataStore store, VolumeVO volume, Timer _timer, VolumeDataStoreDao dao, Long volStoreId, DownloadMonitorImpl downloadMonitor, DownloadCommand cmd, VolumeDao volumeDao, StorageManager storageMgr, ResourceLimitService _resourceLimitMgr, AlertManager _alertMgr, AccountManager _accountMgr, AsyncCompletionCallback<CreateCmdResult> callback) {
	    this._ssAgent = ssAgent;
        this._sstore = store;
		this._volume = volume;
		this._volumeStoreDao = dao;
		this._downloadMonitor = downloadMonitor;
		this._cmd = cmd;
		this._volumeStoreId = volStoreId;
		initStateMachine();
		this._currState=getState(Status.NOT_DOWNLOADED.toString());
		this._timer = _timer;
		this._timeoutTask = new TimeoutTask(this);
		this._timer.schedule(_timeoutTask, 3*STATUS_POLL_INTERVAL);
		this._volumeDao = volumeDao;
		this._storageMgr = storageMgr;
		this._resourceLimitMgr = _resourceLimitMgr;
		this._accountMgr = _accountMgr;
		this._alertMgr = _alertMgr;
		this._callback = callback;
		updateDatabase(Status.NOT_DOWNLOADED, "");
	}


	public void setCurrState(VMTemplateHostVO.Status currState) {
		this._currState = getState(currState.toString());
	}

	private void initStateMachine() {
		_stateMap.put(Status.NOT_DOWNLOADED.toString(), new NotDownloadedState(this));
		_stateMap.put(Status.DOWNLOADED.toString(), new DownloadCompleteState(this));
		_stateMap.put(Status.DOWNLOAD_ERROR.toString(), new DownloadErrorState(this));
		_stateMap.put(Status.DOWNLOAD_IN_PROGRESS.toString(), new DownloadInProgressState(this));
		_stateMap.put(Status.ABANDONED.toString(), new DownloadAbandonedState(this));
	}

	private DownloadState getState(String stateName) {
		return _stateMap.get(stateName);
	}

	public void sendCommand(RequestType reqType) {
		if (getJobId() != null) {
			if (s_logger.isTraceEnabled()) {
				log("Sending progress command ", Level.TRACE);
			}
			try {
				DownloadProgressCommand dcmd = new DownloadProgressCommand(getCommand(), getJobId(), reqType);
				if (_template == null){
					dcmd.setResourceType(ResourceType.VOLUME);
				}
	            _downloadMonitor.send(_ssAgent.getId(), dcmd, this);
            } catch (AgentUnavailableException e) {
            	s_logger.debug("Send command failed", e);
				setDisconnected();
            }
		}

	}

	public void checkProgress() {
		transition(DownloadEvent.TIMEOUT_CHECK, null);
	}

	public void setDisconnected() {
		transition(DownloadEvent.DISCONNECT, null);
	}

	public void logDisconnect() {
		if (_template != null){
			s_logger.warn("Unable to monitor download progress of " + _template.getName() + " at host " + _sserver.getName());
		}else {
			s_logger.warn("Unable to monitor download progress of " + _volume.getName() + " at host " + _sserver.getName());
		}
	}

	public synchronized void updateDatabase(Status state, String errorString) {
		if (_template != null){
		    VMTemplateHostVO vo = _vmTemplateHostDao.createForUpdate();
			vo.setDownloadState(state);
			vo.setLastUpdated(new Date());
			vo.setErrorString(errorString);
			_vmTemplateHostDao.update(getTemplateHostId(), vo);
		}else {
		    VolumeHostVO vo = _volumeHostDao.createForUpdate();
			vo.setDownloadState(state);
			vo.setLastUpdated(new Date());
			vo.setErrorString(errorString);
			_volumeHostDao.update(getVolumeHostId(), vo);
		}
	}

	public void log(String message, Level level) {
		if (_template != null){
			s_logger.log(level, message + ", template=" + _template.getName() + " at host " + _sserver.getName());
		}else {
			s_logger.log(level, message + ", volume=" + _volume.getName() + " at host " + _sserver.getName());
		}
	}

	private Long getTemplateHostId() {
		if (_templateHostId == null){
			VMTemplateHostVO templHost = _vmTemplateHostDao.findByHostTemplate(_sserver.getId(), _template.getId());
			_templateHostId = templHost.getId();
		}
		return _templateHostId;
	}

    private Long getTemplateStoreId() {
        if (_templateStoreId == null){
            TemplateDataStoreVO templStore = _vmTemplateStoreDao.findByStoreTemplate(_sstore.getId(), _template.getId());
            _templateStoreId = templStore.getId();
        }
        return _templateStoreId;
    }

	private Long getVolumeHostId() {
		if (_volumeHostId == null){
			VolumeHostVO volHost = _volumeHostDao.findByHostVolume(_sserver.getId(), _volume.getId());
			_volumeHostId = volHost.getId();
		}
		return _volumeHostId;
	}

	public DownloadListener(DownloadMonitorImpl monitor) {
	    _downloadMonitor = monitor;
	}



	@Override
	public boolean isRecurring() {
		return false;
	}


	@Override
	public boolean processAnswers(long agentId, long seq, Answer[] answers) {
		boolean processed = false;
    	if(answers != null & answers.length > 0) {
    		if(answers[0] instanceof DownloadAnswer) {
    			final DownloadAnswer answer = (DownloadAnswer)answers[0];
    			if (getJobId() == null) {
    				setJobId(answer.getJobId());
    			} else if (!getJobId().equalsIgnoreCase(answer.getJobId())){
    				return false;//TODO
    			}
    			transition(DownloadEvent.DOWNLOAD_ANSWER, answer);
    			processed = true;
    		}
    	}
        return processed;
	}

	private synchronized void transition(DownloadEvent event, Object evtObj) {
	    if (_currState == null) {
	        return;
	    }
		String prevName = _currState.getName();
		String nextState = _currState.handleEvent(event, evtObj);
		if (nextState != null) {
			_currState = getState(nextState);
			if (_currState != null) {
				_currState.onEntry(prevName, event, evtObj);
			} else {
				throw new CloudRuntimeException("Invalid next state: currState="+prevName+", evt="+event + ", next=" + nextState);
			}
		} else {
			throw new CloudRuntimeException("Unhandled event transition: currState="+prevName+", evt="+event);
		}
	}

	public synchronized void updateDatabase(DownloadAnswer answer) {
		if (_template != null){
	        TemplateDataStoreVO updateBuilder = _vmTemplateStoreDao.createForUpdate();
			updateBuilder.setDownloadPercent(answer.getDownloadPct());
			updateBuilder.setDownloadState(answer.getDownloadStatus());
			updateBuilder.setLastUpdated(new Date());
			updateBuilder.setErrorString(answer.getErrorString());
			updateBuilder.setJobId(answer.getJobId());
			updateBuilder.setLocalDownloadPath(answer.getDownloadPath());
			updateBuilder.setInstallPath(answer.getInstallPath());
			updateBuilder.setSize(answer.getTemplateSize());
			updateBuilder.setPhysicalSize(answer.getTemplatePhySicalSize());

            // only invoke callback when Download is completed or errored so that callback will update template_store_ref state column
            Status dndStatus = answer.getDownloadStatus();
           // if (dndStatus == Status.DOWNLOAD_ERROR || dndStatus == Status.DOWNLOADED ){
                if ( _callback != null ){
                    if (dndStatus == Status.DOWNLOAD_ERROR){
                        CreateCmdResult result = new CreateCmdResult(null, null);
                        result.setSucess(false);
                        result.setResult("Download template failed");
                        _callback.complete(result);
                    } else if (dndStatus == Status.DOWNLOADED){
                        CreateCmdResult result = new CreateCmdResult(null, null);
                        _callback.complete(result);
                    }
                }
                else{
                    // no callback specified, just update state here
                    if (dndStatus == Status.DOWNLOAD_ERROR){
                        updateBuilder.setState(ObjectInDataStoreStateMachine.State.Failed);
                    } else if (dndStatus == Status.DOWNLOAD_IN_PROGRESS){
                        updateBuilder.setState(ObjectInDataStoreStateMachine.State.Creating2);
                    } else if (dndStatus == Status.DOWNLOADED){
                        updateBuilder.setState(ObjectInDataStoreStateMachine.State.Ready);
                    }
                }
           // }
			_vmTemplateStoreDao.update(getTemplateStoreId(), updateBuilder);

			if (answer.getCheckSum() != null) {
				VMTemplateVO templateDaoBuilder = _vmTemplateDao.createForUpdate();
				templateDaoBuilder.setChecksum(answer.getCheckSum());
				_vmTemplateDao.update(_template.getId(), templateDaoBuilder);
			}

            if (answer.getTemplateSize() > 0) {
                //long hostId = vmTemplateHostDao.findByTemplateId(template.getId()).getHostId();
                long accountId = _template.getAccountId();
                try {
                    _resourceLimitMgr.checkResourceLimit(_accountMgr.getAccount(accountId),
                            com.cloud.configuration.Resource.ResourceType.secondary_storage,
                            answer.getTemplateSize() - UriUtils.getRemoteSize(_template.getUrl()));
                } catch (ResourceAllocationException e) {
                    s_logger.warn(e.getMessage());
                    _alertMgr.sendAlert(_alertMgr.ALERT_TYPE_RESOURCE_LIMIT_EXCEEDED, _sserver.getDataCenterId(),
                            null, e.getMessage(), e.getMessage());
                } finally {
                    _resourceLimitMgr.recalculateResourceCount(accountId, _accountMgr.getAccount(accountId).getDomainId(),
                            com.cloud.configuration.Resource.ResourceType.secondary_storage.getOrdinal());
                }
            }

		} else {
	        VolumeHostVO updateBuilder = _volumeHostDao.createForUpdate();
			updateBuilder.setDownloadPercent(answer.getDownloadPct());
			updateBuilder.setDownloadState(answer.getDownloadStatus());
			updateBuilder.setLastUpdated(new Date());
			updateBuilder.setErrorString(answer.getErrorString());
			updateBuilder.setJobId(answer.getJobId());
			updateBuilder.setLocalDownloadPath(answer.getDownloadPath());
			updateBuilder.setInstallPath(answer.getInstallPath());
			updateBuilder.setSize(answer.getTemplateSize());
			updateBuilder.setPhysicalSize(answer.getTemplatePhySicalSize());

			_volumeHostDao.update(getVolumeHostId(), updateBuilder);

			// Update volume size in Volume table.
			VolumeVO updateVolume = _volumeDao.createForUpdate();
			updateVolume.setSize(answer.getTemplateSize());
			_volumeDao.update(_volume.getId(), updateVolume);

            if (answer.getTemplateSize() > 0) {
                try {
                    String url = _volumeHostDao.findByVolumeId(_volume.getId()).getDownloadUrl();
                    _resourceLimitMgr.checkResourceLimit(_accountMgr.getAccount(_volume.getAccountId()),
                            com.cloud.configuration.Resource.ResourceType.secondary_storage,
                            answer.getTemplateSize() - UriUtils.getRemoteSize(url));
                } catch (ResourceAllocationException e) {
                    s_logger.warn(e.getMessage());
                    _alertMgr.sendAlert(_alertMgr.ALERT_TYPE_RESOURCE_LIMIT_EXCEEDED, _volume.getDataCenterId(),
                            _volume.getPodId(), e.getMessage(), e.getMessage());
                } finally {
                    _resourceLimitMgr.recalculateResourceCount(_volume.getAccountId(), _volume.getDomainId(),
                            com.cloud.configuration.Resource.ResourceType.secondary_storage.getOrdinal());
                }
            }

			/*if (answer.getCheckSum() != null) {
				VMTemplateVO templateDaoBuilder = _vmTemplateDao.createForUpdate();
				templateDaoBuilder.setChecksum(answer.getCheckSum());
				_vmTemplateDao.update(template.getId(), templateDaoBuilder);
			}*/
		}
 	}

	@Override
	public boolean processCommands(long agentId, long seq, Command[] req) {
		return false;
	}

    @Override
    public AgentControlAnswer processControlCommand(long agentId, AgentControlCommand cmd) {
    	return null;
    }

	@Override
	public boolean processDisconnect(long agentId, com.cloud.host.Status state) {
		setDisconnected();
		return true;
	}

	@Override
	public void processConnect(HostVO agent, StartupCommand cmd, boolean forRebalance) throws ConnectionException {
	    if (cmd instanceof StartupRoutingCommand) {
	        List<HypervisorType> hypers = _resourceMgr.listAvailHypervisorInZone(agent.getId(), agent.getDataCenterId());
	        HypervisorType hostHyper = agent.getHypervisorType();
	        if (hypers.contains(hostHyper)) {
	            return;
	        }
	        _imageSrv.handleSysTemplateDownload(hostHyper, agent.getDataCenterId());
	    }
	    /* This can be removed since
	    else if ( cmd instanceof StartupStorageCommand) {
	        StartupStorageCommand storage = (StartupStorageCommand)cmd;
            if( storage.getResourceType() == Storage.StorageResourceType.SECONDARY_STORAGE ||
                    storage.getResourceType() == Storage.StorageResourceType.LOCAL_SECONDARY_STORAGE  ) {
                downloadMonitor.addSystemVMTemplatesToHost(agent, storage.getTemplateInfo());
                downloadMonitor.handleTemplateSync(agent);
                downloadMonitor.handleVolumeSync(agent);
            }
	    }*/
	    else if ( cmd instanceof StartupSecondaryStorageCommand ) {
	        List<DataStore> imageStores = this._storeMgr.getImageStoresByScope(new ZoneScope(agent.getDataCenterId()));
	        for (DataStore store : imageStores){
	            _volumeSrv.handleVolumeSync(store);
	            _imageSrv.handleTemplateSync(store);
	        }
	    }
	}

	public void setCommand(DownloadCommand _cmd) {
		this._cmd = _cmd;
	}

	public DownloadCommand getCommand() {
		return _cmd;
	}


	public void abandon() {
		transition(DownloadEvent.ABANDON_DOWNLOAD, null);
	}

	public void setJobId(String _jobId) {
		this._jobId = _jobId;
	}

	public String getJobId() {
		return _jobId;
	}

	public void scheduleStatusCheck(RequestType request) {
		if (_statusTask != null) _statusTask.cancel();

		_statusTask = new StatusTask(this, request);
		_timer.schedule(_statusTask, STATUS_POLL_INTERVAL);
	}

	public void scheduleTimeoutTask(long delay) {
		if (_timeoutTask != null) _timeoutTask.cancel();

		_timeoutTask = new TimeoutTask(this);
		_timer.schedule(_timeoutTask, delay);
		if (s_logger.isDebugEnabled()) {
			log("Scheduling timeout at " + delay + " ms", Level.DEBUG);
		}
	}

	public void scheduleImmediateStatusCheck(RequestType request) {
		if (_statusTask != null) _statusTask.cancel();
		_statusTask = new StatusTask(this, request);
		_timer.schedule(_statusTask, SMALL_DELAY);
	}

	public boolean isDownloadActive() {
		return _downloadActive;
	}

	public void cancelStatusTask() {
		if (_statusTask != null) _statusTask.cancel();
	}

	public Date getLastUpdated() {
		return _lastUpdated;
	}

	public void setLastUpdated() {
		_lastUpdated  = new Date();
	}

	public void setDownloadInactive(Status reason) {
		_downloadActive=false;
		if (_template != null){
			_downloadMonitor.handleDownloadEvent(_sserver, _template, reason);
		}else {
			_downloadMonitor.handleDownloadEvent(_sserver, _volume, reason);
		}
	}

	public void cancelTimeoutTask() {
		if (_timeoutTask != null) _timeoutTask.cancel();
	}

	public void logDownloadStart() {
	}

    @Override
    public boolean processTimeout(long agentId, long seq) {
    	return true;
    }

    @Override
    public int getTimeout() {
    	return -1;
    }
}
