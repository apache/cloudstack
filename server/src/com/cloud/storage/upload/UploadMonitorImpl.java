package com.cloud.storage.upload;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.Listener;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.storage.UploadCommand;
import com.cloud.agent.api.storage.UploadProgressCommand;
import com.cloud.agent.api.storage.UploadProgressCommand.RequestType;
import com.cloud.api.commands.CreateNetworkGroupCmd;
import com.cloud.async.AsyncJobManager;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.Upload;
import com.cloud.storage.UploadVO;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Upload.Type;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.UploadDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.template.TemplateInfo;
import com.cloud.utils.component.Inject;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.dao.SecondaryStorageVmDao;

/**
 * @author nitin
 * Monitors the progress of upload.
 */
@Local(value={UploadMonitor.class})
public class UploadMonitorImpl implements UploadMonitor {

	static final Logger s_logger = Logger.getLogger(UploadMonitorImpl.class);
	
	private String _hyperVisorType;
    @Inject 
    VMTemplateHostDao _vmTemplateHostDao;
    @Inject 
    UploadDao _uploadDao;
    @Inject
    SecondaryStorageVmDao _secStorageVmDao;

    
    @Inject
    HostDao _serverDao = null;    
    @Inject
    VMTemplateDao _templateDao =  null;
    @Inject
	private final EventDao _eventDao = null;
    @Inject
	private AgentManager _agentMgr;
    @Inject
    ConfigurationDao _configDao;

	private String _name;
	private Boolean _sslCopy = new Boolean(false);
	private String _copyAuthPasswd;


	Timer _timer;

	final Map<UploadVO, UploadListener> _listenerMap = new ConcurrentHashMap<UploadVO, UploadListener>();

	
	@Override
	public void cancelAllUploads(Long templateId) {
		// TODO

	}	
	
	@Override
	public boolean isTypeUploadInProgress(Long typeId, Type type) {
		List<UploadVO> uploadsInProgress =
			_uploadDao.listByTypeUploadStatus(typeId, type, UploadVO.Status.UPLOAD_IN_PROGRESS);
		
		if(uploadsInProgress.size() > 0)
		    return true;
		else if (type == Type.VOLUME && _uploadDao.listByTypeUploadStatus(typeId, type, UploadVO.Status.COPY_IN_PROGRESS).size() > 0){
		    return true;
		}
		return false;
		
	}
	
	@Override
	public UploadVO createNewUploadEntry(Long hostId, Long typeId, UploadVO.Status  uploadState,
	                                        int uploadPercent, Type  type, 
	                                        String  errorString, String  jobId, String  uploadUrl){
	       
        UploadVO uploadObj = new UploadVO(hostId, typeId, new Date(), 
                            uploadState, 0, type, null, "jobid0000", uploadUrl);
        _uploadDao.persist(uploadObj);
        
        return uploadObj;
	    
	}
	
	@Override
	public void extractVolume(UploadVO uploadVolumeObj, HostVO sserver, VolumeVO volume, String url, Long dataCenterId, String installPath, long eventId, long asyncJobId, AsyncJobManager asyncMgr){				
						
		uploadVolumeObj.setUploadState(Upload.Status.NOT_UPLOADED);
		_uploadDao.update(uploadVolumeObj.getId(), uploadVolumeObj);
				
	    start();		
		UploadCommand ucmd = new UploadCommand(url, volume.getId(), volume.getSize(), installPath, Type.VOLUME);
		UploadListener ul = new UploadListener(sserver, _timer, _uploadDao, uploadVolumeObj.getId(), this, ucmd, volume.getAccountId(), volume.getName(), Type.VOLUME, eventId, asyncJobId, asyncMgr);
		_listenerMap.put(uploadVolumeObj, ul);

		long result = send(sserver.getId(), ucmd, ul);	
		if (result == -1) {
			s_logger.warn("Unable to start upload of volume " + volume.getName() + " from " + sserver.getName() + " to " +url);
			ul.setDisconnected();
			ul.scheduleStatusCheck(RequestType.GET_OR_RESTART);
		}				
		
	}

	@Override
	public void extractTemplate( VMTemplateVO template, String url,
			VMTemplateHostVO vmTemplateHost,Long dataCenterId, long eventId, long asyncJobId, AsyncJobManager asyncMgr){

		Type type = (template.getFormat() == ImageFormat.ISO) ? Type.ISO : Type.TEMPLATE ;
				
		List<HostVO> storageServers = _serverDao.listByTypeDataCenter(Host.Type.SecondaryStorage, dataCenterId);
		HostVO sserver = storageServers.get(0);			
		
		UploadVO uploadTemplateObj = new UploadVO(sserver.getId(), template.getId(), new Date(), 
													Upload.Status.NOT_UPLOADED, 0, type, 
													null, "jobid0000", url);
		_uploadDao.persist(uploadTemplateObj);        		               
        		
		if(vmTemplateHost != null) {
		    start();
			UploadCommand ucmd = new UploadCommand(template, url, vmTemplateHost);	
			UploadListener ul = new UploadListener(sserver, _timer, _uploadDao, uploadTemplateObj.getId(), this, ucmd, template.getAccountId(), template.getName(), type, eventId, asyncJobId, asyncMgr);			
			_listenerMap.put(uploadTemplateObj, ul);

			long result = send(sserver.getId(), ucmd, ul);	
			if (result == -1) {
				s_logger.warn("Unable to start upload of " + template.getUniqueName() + " from " + sserver.getName() + " to " +url);
				ul.setDisconnected();
				ul.scheduleStatusCheck(RequestType.GET_OR_RESTART);
			}
		}
		
	}


	public long send(Long hostId, Command cmd, Listener listener) {
		return _agentMgr.gatherStats(hostId, cmd, listener);
	}

	@Override
	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {
		_name = name;
        final Map<String, String> configs = _configDao.getConfiguration("ManagementServer", params);
        _sslCopy = Boolean.parseBoolean(configs.get("secstorage.encrypt.copy"));
        
        String cert = configs.get("secstorage.secure.copy.cert");
        if ("realhostip.com".equalsIgnoreCase(cert)) {
        	s_logger.warn("Only realhostip.com ssl cert is supported, ignoring self-signed and other certs");
        }
        
        _hyperVisorType = _configDao.getValue("hypervisor.type");
        
        _copyAuthPasswd = configs.get("secstorage.copy.password");
        
        _agentMgr.registerForHostEvents(new UploadListener(this), true, false, false);
		return true;
	}

	@Override
	public String getName() {
		return _name;
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
	
	public String getEvent(Type type){
					
		if(type == Type.TEMPLATE) return EventTypes.EVENT_TEMPLATE_UPLOAD;
		if(type == Type.ISO) return EventTypes.EVENT_ISO_UPLOAD;
		if(type == Type.VOLUME) return EventTypes.EVENT_VOLUME_UPLOAD;			
		
		return null;
	}
	
	public void handleUploadEvent(HostVO host, Long accountId, String typeName, Type type, Long uploadId, com.cloud.storage.Upload.Status reason, long eventId) {
		
		if ((reason == Upload.Status.UPLOADED) || (reason==Upload.Status.ABANDONED)){
			UploadVO uploadObj = new UploadVO(uploadId);
			UploadListener oldListener = _listenerMap.get(uploadObj);
			if (oldListener != null) {
				_listenerMap.remove(uploadObj);
			}
		}
		if (reason == Upload.Status.UPLOADED) {
			logEvent(accountId, getEvent(type), typeName + " successfully uploaded from storage server " + host.getName(), EventVO.LEVEL_INFO, eventId);
		}
		if (reason == Upload.Status.UPLOAD_ERROR) {
			logEvent(accountId, getEvent(type), typeName + " failed to upload from storage server " + host.getName(), EventVO.LEVEL_ERROR, eventId);
		}
		if (reason == Upload.Status.ABANDONED) {
			logEvent(accountId, getEvent(type), typeName + " :aborted upload from storage server " + host.getName(), EventVO.LEVEL_WARN, eventId);
		}			

	}
	
	public void logEvent(long accountId, String evtType, String description, String level, long eventId) {
		EventVO event = new EventVO();
		event.setUserId(1);
		event.setAccountId(accountId);
		event.setType(evtType);
		event.setDescription(description);
		event.setLevel(level);
		event.setStartId(eventId);
		_eventDao.persist(event);
		
	}

	@Override
	public void handleUploadSync(long sserverId) {
	    
	    HostVO storageHost = _serverDao.findById(sserverId);
        if (storageHost == null) {
            s_logger.warn("Huh? Agent id " + sserverId + " does not correspond to a row in hosts table?");
            return;
        }
        s_logger.debug("Handling upload sserverId " +sserverId);
        List<UploadVO> uploadsInProgress = new ArrayList<UploadVO>();
        uploadsInProgress.addAll(_uploadDao.listByHostAndUploadStatus(sserverId, UploadVO.Status.UPLOAD_IN_PROGRESS));
        uploadsInProgress.addAll(_uploadDao.listByHostAndUploadStatus(sserverId, UploadVO.Status.COPY_IN_PROGRESS));
        if (uploadsInProgress.size() > 0){
            for (UploadVO uploadJob : uploadsInProgress){
                uploadJob.setUploadState(UploadVO.Status.UPLOAD_ERROR);
                uploadJob.setErrorString("Could not complete the upload.");
                uploadJob.setLastUpdated(new Date());
                _uploadDao.update(uploadJob.getId(), uploadJob);
            }
            
        }
	        

	}				
		
}
