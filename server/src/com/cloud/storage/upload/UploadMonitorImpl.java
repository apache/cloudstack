package com.cloud.storage.upload;

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
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.template.TemplateInfo;
import com.cloud.utils.component.Inject;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.dao.SecondaryStorageVmDao;

/**
 * @author nitin
 *
 */
@Local(value={UploadMonitor.class})
public class UploadMonitorImpl implements UploadMonitor {

	static final Logger s_logger = Logger.getLogger(UploadMonitorImpl.class);
	
	private String _hyperVisorType;
    @Inject 
    VMTemplateHostDao _vmTemplateHostDao;
    @Inject
	VMTemplatePoolDao _vmTemplatePoolDao;
    @Inject
    StoragePoolHostDao _poolHostDao;
    @Inject
    SecondaryStorageVmDao _secStorageVmDao;

    
    @Inject
    HostDao _serverDao = null;
    @Inject
    private final DataCenterDao _dcDao = null;
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

	final Map<VMTemplateHostVO, UploadListener> _listenerMap = new ConcurrentHashMap<VMTemplateHostVO, UploadListener>();

	
	@Override
	public void cancelAllUploads(Long templateId) {
		// TODO Auto-generated method stub

	}
	public boolean isTemplateUploadInProgress(Long templateId) {
		List<VMTemplateHostVO> uploadsInProgress =
			_vmTemplateHostDao.listByTemplateStatus(templateId, VMTemplateHostVO.Status.UPLOAD_IN_PROGRESS);
		return (uploadsInProgress.size() != 0);
	}

	@Override
	public void extractTemplate( VMTemplateVO template, String url,
			VMTemplateHostVO vmTemplateHost,Long dataCenterId){

		if (isTemplateUploadInProgress(template.getId()) ){
			return;
		}		
		
		List<HostVO> storageServers = _serverDao.listByTypeDataCenter(Host.Type.SecondaryStorage, dataCenterId);
		HostVO sserver = storageServers.get(0);			
		
		_vmTemplateHostDao.updateUploadStatus(sserver.getId(), template.getId(), 0, VMTemplateStorageResourceAssoc.Status.NOT_UPLOADED, "jobid0000", url);                
        		
		if(vmTemplateHost != null) {
		    start();
			UploadCommand ucmd = new UploadCommand(template, url, vmTemplateHost);	
			UploadListener ul = new UploadListener(sserver, template, _timer, _vmTemplateHostDao, vmTemplateHost.getId(), this, ucmd);
			_listenerMap.put(vmTemplateHost, ul);

			long result = send(sserver.getId(), ucmd, ul);	
			if (result == -1) {
				s_logger.warn("Unable to start upload of template " + template.getUniqueName() + " from " + sserver.getName() + " to " +url);
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
		// TODO Auto-generated method stub
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
	
	public void handleUploadEvent(HostVO host, VMTemplateVO template, Status upldStatus) {
		
		if ((upldStatus == VMTemplateStorageResourceAssoc.Status.UPLOADED) || (upldStatus==Status.ABANDONED)){
			VMTemplateHostVO vmTemplateHost = new VMTemplateHostVO(host.getId(), template.getId());
			UploadListener oldListener = _listenerMap.get(vmTemplateHost);
			if (oldListener != null) {
				_listenerMap.remove(vmTemplateHost);
			}
		}
		if (upldStatus == VMTemplateStorageResourceAssoc.Status.UPLOADED) {
			logEvent(template.getAccountId(), EventTypes.EVENT_TEMPLATE_UPLOAD_SUCCESS, template.getName() + " successfully uploaded from storage server " + host.getName(), EventVO.LEVEL_INFO);
		}
		if (upldStatus == Status.UPLOAD_ERROR) {
			logEvent(template.getAccountId(), EventTypes.EVENT_TEMPLATE_UPLOAD_FAILED, template.getName() + " failed to upload from storage server " + host.getName(), EventVO.LEVEL_ERROR);
		}
		if (upldStatus == Status.ABANDONED) {
			logEvent(template.getAccountId(), EventTypes.EVENT_TEMPLATE_UPLOAD_FAILED, template.getName() + " :aborted upload from storage server " + host.getName(), EventVO.LEVEL_WARN);
		}
		
		/*VMTemplateHostVO vmTemplateHost = _vmTemplateHostDao.findByHostTemplate(host.getId(), template.getId());
		
        if (upldStatus == Status.UPLOADED) {
            long size = -1;
            if(vmTemplateHost!=null){
            	size = vmTemplateHost.getSize();
            }
            else{
            	s_logger.warn("Failed to get size for template" + template.getName());
            }
			String eventParams = "id=" + template.getId() + "\ndcId="+host.getDataCenterId()+"\nsize="+size;
            EventVO event = new EventVO();
            event.setUserId(1L);
            event.setAccountId(template.getAccountId());
            if((template.getFormat()).equals(ImageFormat.ISO)){
            	event.setType(EventTypes.EVENT_ISO_CREATE);
            	event.setDescription("Successfully uploaded ISO " + template.getName());
            }
            else{
            	event.setType(EventTypes.EVENT_TEMPLATE_);
            	event.setDescription("Successfully uploaded template " + template.getName());
            }
            event.setParameters(eventParams);
            event.setLevel(EventVO.LEVEL_INFO);
            _eventDao.persist(event);
        } 
        
		if (vmTemplateHost != null) {
			Long poolId = vmTemplateHost.getPoolId();
			if (poolId != null) {
				VMTemplateStoragePoolVO vmTemplatePool = _vmTemplatePoolDao.findByPoolTemplate(poolId, template.getId());
				StoragePoolHostVO poolHost = _poolHostDao.findByPoolHost(poolId, host.getId());
				if (vmTemplatePool != null && poolHost != null) {
					vmTemplatePool.setDownloadPercent(vmTemplateHost.getUploadPercent());
					vmTemplatePool.setDownloadState(vmTemplateHost.getUploadState());
					vmTemplatePool.setErrorString(vmTemplateHost.getUpload_errorString());
					String localPath = poolHost.getLocalPath();
					String installPath = vmTemplateHost.getInstallPath();
					if (installPath != null) {
						if (!installPath.startsWith("/")) {
							installPath = "/" + installPath;
						}
						if (!(localPath == null) && !installPath.startsWith(localPath)) {
							localPath = localPath.replaceAll("/\\p{Alnum}+/*$", ""); //remove instance if necessary
						}
						if (!(localPath == null) && installPath.startsWith(localPath)) {
							installPath = installPath.substring(localPath.length());
						}
					}
					vmTemplatePool.setInstallPath(installPath);
					vmTemplatePool.setLastUpdated(vmTemplateHost.getLastUpdated());
					vmTemplatePool.setJobId(vmTemplateHost.getJobId());
					vmTemplatePool.setLocalDownloadPath(vmTemplateHost.getLocalDownloadPath());
					_vmTemplatePoolDao.update(vmTemplatePool.getId(),vmTemplatePool);
				}
			}
		}*/

	}
	
	public void logEvent(long accountId, String evtType, String description, String level) {
		EventVO event = new EventVO();
		event.setUserId(1);
		event.setAccountId(accountId);
		event.setType(evtType);
		event.setDescription(description);
		event.setLevel(level);
		_eventDao.persist(event);
		
	}

	@Override
	public void handleUploadTemplateSync(long sserverId, Map<String, TemplateInfo> templateInfo) {
		HostVO storageHost = _serverDao.findById(sserverId);
		if (storageHost == null) {
			s_logger.warn("Huh? Agent id " + sserverId + " does not correspond to a row in hosts table?");
			return;
		}		
		
		List<VMTemplateVO> allTemplates = _templateDao.listAllInZone(storageHost.getDataCenterId());
		VMTemplateVO rtngTmplt = _templateDao.findRoutingTemplate();
		VMTemplateVO defaultBuiltin = _templateDao.findDefaultBuiltinTemplate();

		if (rtngTmplt != null && !allTemplates.contains(rtngTmplt))
			allTemplates.add(rtngTmplt);

		if (defaultBuiltin != null && !allTemplates.contains(defaultBuiltin)) {
			allTemplates.add(defaultBuiltin);
		}			
		        
        
		for (VMTemplateVO tmplt: allTemplates) {
			String uniqueName = tmplt.getUniqueName();
			VMTemplateHostVO tmpltHost = _vmTemplateHostDao.findByHostTemplate(sserverId, tmplt.getId());
			if (templateInfo.containsKey(uniqueName)) {		
				if (tmpltHost != null) {
					s_logger.info("Template Sync found " + uniqueName + " already in the template host table");
                    if (tmpltHost.getUploadState() != Status.UPLOADED) {
                    	tmpltHost.setUpload_errorString("");
                    }
                    tmpltHost.setUploadPercent(100);
                    tmpltHost.setUploadState(Status.UPLOADED);                    
                    tmpltHost.setLastUpdated(new Date());
					_vmTemplateHostDao.update(tmpltHost.getId(), tmpltHost);
				} else {
					VMTemplateHostVO templtHost = new VMTemplateHostVO(sserverId, tmplt.getId(), new Date(), 100, Status.UPLOADED, null, null, null, templateInfo.get(uniqueName).getInstallPath(), tmplt.getUrl());
					templtHost.setSize(templateInfo.get(uniqueName).getSize());
					_vmTemplateHostDao.persist(templtHost);
				}
				templateInfo.remove(uniqueName);
				continue;
			}
			/*if (tmpltHost != null && tmpltHost.getUploadState() != Status.UPLOADED) {
				s_logger.info("Template Sync did not find " + uniqueName + " ready on server " + sserverId + ", will request upload to start/resume shortly");

			} else if (tmpltHost == null) {
				s_logger.info("Template Sync did not find " + uniqueName + " on the server " + sserverId + ", will request upload shortly");
				VMTemplateHostVO templtHost = new VMTemplateHostVO(sserverId, tmplt.getId(), new Date(), 0, Status.NOT_UPLOADED, null, null, null, null, tmplt.getUrl());
				_vmTemplateHostDao.persist(templtHost);
			}*/

		}				
	}	
}
