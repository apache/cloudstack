package com.cloud.storage.upload;


import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupStorageCommand;
import com.cloud.agent.api.storage.DownloadCommand;
import com.cloud.agent.api.storage.DownloadProgressCommand;
import com.cloud.agent.api.storage.UploadAnswer;
import com.cloud.agent.api.storage.UploadCommand;
import com.cloud.agent.api.storage.UploadProgressCommand;
import com.cloud.agent.api.storage.UploadProgressCommand.RequestType;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.host.HostVO;
import com.cloud.storage.Storage;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.download.DownloadState.DownloadEvent;
import com.cloud.storage.upload.UploadMonitorImpl;
import com.cloud.storage.upload.UploadState.UploadEvent;
import com.cloud.utils.exception.CloudRuntimeException;

public class UploadListener implements Listener {
	

	private static final class StatusTask extends TimerTask {
		private final UploadListener ul;
		private final RequestType reqType;
		
		public StatusTask( UploadListener ul,  RequestType req) {
			this.reqType = req;
			this.ul = ul;
		}

		@Override
		public void run() {
		  ul.sendCommand(reqType);

		}
	}
	
	private static final class TimeoutTask extends TimerTask {
		private final UploadListener ul;
		
		public TimeoutTask( UploadListener ul) {
			this.ul = ul;
		}

		@Override
		public void run() {
		  ul.checkProgress();
		}
	}

	public static final Logger s_logger = Logger.getLogger(UploadListener.class.getName());
	public static final int SMALL_DELAY = 100;
	public static final long STATUS_POLL_INTERVAL = 10000L;
	
	public static final String UPLOADED=Status.UPLOADED.toString();
	public static final String NOT_UPLOADED=Status.NOT_UPLOADED.toString();
	public static final String UPLOAD_ERROR=Status.UPLOAD_ERROR.toString();
	public static final String UPLOAD_IN_PROGRESS=Status.UPLOAD_IN_PROGRESS.toString();
	public static final String UPLOAD_ABANDONED=Status.ABANDONED.toString();


	private HostVO sserver;
	private VMTemplateVO template;
	
	private boolean uploadActive = true;

	private VMTemplateHostDao vmTemplateHostDao;

	private final UploadMonitorImpl uploadMonitor;
	
	private UploadState currState;
	
	private UploadCommand cmd;

	private Timer timer;

	private StatusTask statusTask;
	private TimeoutTask timeoutTask;
	private Date lastUpdated = new Date();
	private String jobId;
	
	private final Map<String,  UploadState> stateMap = new HashMap<String, UploadState>();
	private Long templateHostId;
	
	public UploadListener(HostVO host, VMTemplateVO template, Timer _timer, VMTemplateHostDao dao, Long templHostId, UploadMonitorImpl uploadMonitor, UploadCommand cmd) {
		this.sserver = host;
		this.template = template;
		this.vmTemplateHostDao = dao;
		this.uploadMonitor = uploadMonitor;
		this.cmd = cmd;
		this.templateHostId = templHostId;
		initStateMachine();
		this.currState = getState(Status.NOT_UPLOADED.toString());
		this.timer = _timer;
		this.timeoutTask = new TimeoutTask(this);
		this.timer.schedule(timeoutTask, 3*STATUS_POLL_INTERVAL);
		updateDatabase(Status.NOT_UPLOADED, cmd.getUrl(),"");
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

	public void setCommand(UploadCommand _cmd) {
		this.cmd = _cmd;
	}
	
	public void setJobId(String _jobId) {
		this.jobId = _jobId;
	}
	
	public String getJobId() {
		return jobId;
	}
	
	@Override
	public boolean processAnswer(long agentId, long seq, Answer[] answers) {
		boolean processed = false;
    	if(answers != null & answers.length > 0) {
    		if(answers[0] instanceof UploadAnswer) {
    			final UploadAnswer answer = (UploadAnswer)answers[0];
    			if (getJobId() == null) {
    				setJobId(answer.getJobId());
    			} else if (!getJobId().equalsIgnoreCase(answer.getJobId())){
    				return false;//TODO
    			}
    			transition(UploadEvent.UPLOAD_ANSWER, answer);
    			processed = true;
    		}
    	}
        return processed;
	}
	

	@Override
	public boolean processCommand(long agentId, long seq, Command[] commands) {
		return false;
	}

	@Override
	public boolean processConnect(HostVO agent, StartupCommand cmd) {		
		if (!(cmd instanceof StartupStorageCommand)) {
	        return true;
	    }
	   /* if (cmd.getGuid().startsWith("iso:")) {
	        //FIXME: do not download template for ISO secondary
	        return true;
	    }*/
	    
	    long agentId = agent.getId();
	    
	    StartupStorageCommand storage = (StartupStorageCommand)cmd;
	    if (storage.getResourceType() == Storage.StorageResourceType.STORAGE_HOST ||
	    storage.getResourceType() == Storage.StorageResourceType.SECONDARY_STORAGE )
	    {
	    	uploadMonitor.handleUploadTemplateSync(agentId, storage.getTemplateInfo());
	    } else {
	    	//downloadMonitor.handlePoolTemplateSync(storage.getPoolInfo(), storage.getTemplateInfo());
	    	//no need to do anything. The storagepoolmonitor will initiate template sync.
	    }
		return true;
	}

	@Override
	public AgentControlAnswer processControlCommand(long agentId,
			AgentControlCommand cmd) {
		return null;
	}
	
	public void setUploadInactive(Status reason) {
		uploadActive=false;
		uploadMonitor.handleUploadEvent(sserver, template, reason);
	}
	
	public void logUploadStart() {
		uploadMonitor.logEvent(template.getAccountId(), EventTypes.EVENT_TEMPLATE_UPLOAD_START, "Storage server " + sserver.getName() + " started upload of template " + template.getName(), EventVO.LEVEL_INFO);
	}
	
	public void cancelTimeoutTask() {
		if (timeoutTask != null) timeoutTask.cancel();
	}
	
	public void cancelStatusTask() {
		if (statusTask != null) statusTask.cancel();
	}

	@Override
	public boolean processDisconnect(long agentId, com.cloud.host.Status state) {	
		setDisconnected();
		return true;
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
				throw new CloudRuntimeException("Invalid next state: currState="+prevName+", evt="+event + ", next=" + nextState);
			}
		} else {
			throw new CloudRuntimeException("Unhandled event transition: currState="+prevName+", evt="+event);
		}
	}
	
	public Date getLastUpdated() {
		return lastUpdated;
	}
	
	public void setLastUpdated() {
		lastUpdated  = new Date();
	}
	
	public void log(String message, Level level) {
		s_logger.log(level, message + ", template=" + template.getName() + " at host " + sserver.getName());
	}

	public void setDisconnected() {
		transition(UploadEvent.DISCONNECT, null);
	}
	
	public void scheduleStatusCheck(com.cloud.agent.api.storage.UploadProgressCommand.RequestType getStatus) {
		if (statusTask != null) statusTask.cancel();

		statusTask = new StatusTask(this, getStatus);
		timer.schedule(statusTask, STATUS_POLL_INTERVAL);
	}

	public void scheduleTimeoutTask(long delay) {
		if (timeoutTask != null) timeoutTask.cancel();

		timeoutTask = new TimeoutTask(this);
		timer.schedule(timeoutTask, delay);
		if (s_logger.isDebugEnabled()) {
			log("Scheduling timeout at " + delay + " ms", Level.DEBUG);
		}
	}
	
	public void updateDatabase(Status state, String uploadErrorString) {
		
		VMTemplateHostVO vo = vmTemplateHostDao.createForUpdate();
		vo.setUploadState(state);
		vo.setLastUpdated(new Date());
		vo.setUpload_errorString(uploadErrorString);
		vmTemplateHostDao.update(getTemplateHostId(), vo);
	}
	
	public void updateDatabase(Status state, String uploadUrl,String uploadErrorString) {
		
		VMTemplateHostVO vo = vmTemplateHostDao.createForUpdate();
		vo.setUploadState(state);
		vo.setLastUpdated(new Date());
		vo.setUploadUrl(uploadUrl);
		vo.setUploadJobId(null);
		vo.setUploadPercent(0);
		vo.setUpload_errorString(uploadErrorString);
		
		vmTemplateHostDao.update(getTemplateHostId(), vo);
	}
	
	private Long getTemplateHostId() {
		if (templateHostId == null){
			VMTemplateHostVO templHost = vmTemplateHostDao.findByHostTemplate(sserver.getId(), template.getId());
			templateHostId = templHost.getId();
		}
		return templateHostId;
	}

	public synchronized void updateDatabase(UploadAnswer answer) {		
		
        VMTemplateHostVO updateBuilder = vmTemplateHostDao.createForUpdate();
		updateBuilder.setUploadPercent(answer.getUploadPct());
		updateBuilder.setUploadState(answer.getUploadStatus());
		updateBuilder.setLastUpdated(new Date());
		updateBuilder.setUpload_errorString(answer.getErrorString());
		updateBuilder.setUploadJobId(answer.getJobId());
		
		vmTemplateHostDao.update(getTemplateHostId(), updateBuilder);
	}

	public void sendCommand(RequestType reqType) {
		if (getJobId() != null) {
			if (s_logger.isTraceEnabled()) {
				log("Sending progress command ", Level.TRACE);
			}
			long sent = uploadMonitor.send(sserver.getId(), new UploadProgressCommand(getCommand(), getJobId(), reqType), this);
			if (sent == -1) {
				setDisconnected();
			}
		}
		
	}
	
	private UploadCommand getCommand() {
		return cmd;
	}

	public void logDisconnect() {
		s_logger.warn("Unable to monitor upload progress of " + template.getName() + " at host " + sserver.getName());
		uploadMonitor.logEvent(template.getAccountId(), EventTypes.EVENT_TEMPLATE_UPLOAD_FAILED, "Storage server " + sserver.getName() + " disconnected during upload of template " + template.getName(), EventVO.LEVEL_WARN);
	}
	
	public void scheduleImmediateStatusCheck(RequestType request) {
		if (statusTask != null) statusTask.cancel();
		statusTask = new StatusTask(this, request);
		timer.schedule(statusTask, SMALL_DELAY);
	}

	public void setCurrState(Status uploadState) {
		this.currState = getState(currState.toString());		
	}
	
}
