/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.template;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.DestroyCommand;
import com.cloud.agent.api.storage.DownloadAnswer;
import com.cloud.agent.api.storage.PrimaryStorageDownloadCommand;
import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.event.EventState;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.exception.InternalErrorException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VMTemplateZoneVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.Storage.FileSystem;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.Volume.VolumeType;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.StoragePoolDao;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.download.DownloadMonitor;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.UserAccount;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserAccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;

@Local(value=TemplateManager.class)
public class TemplateManagerImpl implements TemplateManager {
    private final static Logger s_logger = Logger.getLogger(TemplateManagerImpl.class);
    String _name;
    @Inject VMTemplateDao _tmpltDao;
    @Inject VMTemplateHostDao _tmpltHostDao;
    @Inject VMTemplatePoolDao _tmpltPoolDao;
    @Inject VMTemplateZoneDao _tmpltZoneDao;
    @Inject VMInstanceDao _vmInstanceDao;
    @Inject StoragePoolDao _poolDao;
    @Inject StoragePoolHostDao _poolHostDao;
    @Inject EventDao _eventDao;
    @Inject DownloadMonitor _downloadMonitor;
    @Inject UserAccountDao _userAccountDao;
    @Inject AccountDao _accountDao;
    @Inject UserDao _userDao;
    @Inject AgentManager _agentMgr;
    @Inject AccountManager _accountMgr;
    @Inject HostDao _hostDao;
    @Inject DataCenterDao _dcDao;
    @Inject VolumeDao _volumeDao;
    @Inject SnapshotDao _snapshotDao;
    long _routerTemplateId = -1;
    @Inject StorageManager _storageMgr;
    protected SearchBuilder<VMTemplateHostVO> HostTemplateStatesSearch;
    

    @Override
    public Long create(long userId, Long zoneId, String name, String displayText, boolean isPublic, boolean featured, ImageFormat format, FileSystem fs, URI url, String chksum, boolean requiresHvm, int bits, boolean enablePassword, long guestOSId, boolean bootable) {
        Long id = _tmpltDao.getNextInSequence(Long.class, "id");
        
        UserVO user = _userDao.findById(userId);
        long accountId = user.getAccountId();
        AccountVO account = _accountDao.findById(accountId);
        if (account.getType() != Account.ACCOUNT_TYPE_ADMIN && zoneId == null) {
        	throw new IllegalArgumentException("Only admins can create templates in all zones");
        }
        
        VMTemplateVO template = new VMTemplateVO(id, name, format, isPublic, featured, fs, url.toString(), requiresHvm, bits, accountId, chksum, displayText, enablePassword, guestOSId, bootable);
        if (zoneId == null) {
            List<DataCenterVO> dcs = _dcDao.listAll();

        	for (DataCenterVO dc: dcs) {
    			_tmpltDao.addTemplateToZone(template, dc.getId());
    		}
        	template.setCrossZones(true);
        } else {
			_tmpltDao.addTemplateToZone(template, zoneId);
        }

		
        UserAccount userAccount = _userAccountDao.findById(userId);
       
        _downloadMonitor.downloadTemplateToStorage(id, zoneId);
        
        _accountMgr.incrementResourceCount(userAccount.getAccountId(), ResourceType.template);
        
        return id;
    }
    
    @Override @DB
    public VMTemplateStoragePoolVO prepareTemplateForCreate(VMTemplateVO template, StoragePoolVO pool) {
    	template = _tmpltDao.findById(template.getId(), true);
    	
        long poolId = pool.getId();
        long templateId = template.getId();
        VMTemplateStoragePoolVO templateStoragePoolRef = null;
        VMTemplateHostVO templateHostRef = null;
        long templateStoragePoolRefId;
        String origUrl = null;
        
        templateStoragePoolRef = _tmpltPoolDao.findByPoolTemplate(poolId, templateId);
        if (templateStoragePoolRef != null) {
        	templateStoragePoolRef.setMarkedForGC(false);
            _tmpltPoolDao.update(templateStoragePoolRef.getId(), templateStoragePoolRef);
            
            if (templateStoragePoolRef.getDownloadState() == Status.DOWNLOADED) {
	            if (s_logger.isDebugEnabled()) {
	                s_logger.debug("Template " + templateId + " has already been downloaded to pool " + poolId);
	            }
	            
	            return templateStoragePoolRef;
	        }
        }
        
        SearchCriteria sc = HostTemplateStatesSearch.create();
        sc.setParameters("id", templateId);
        sc.setParameters("state", Status.DOWNLOADED);
        sc.setJoinParameters("host", "dcId", pool.getDataCenterId());

        List<VMTemplateHostVO> templateHostRefs = _tmpltHostDao.search(sc, null);
        
        if (templateHostRefs.size() == 0) {
            s_logger.debug("Unable to find a secondary storage host who has completely downloaded the template.");
            return null;
        }
        
        templateHostRef = templateHostRefs.get(0);
        
        HostVO sh = _hostDao.findById(templateHostRef.getHostId());
        origUrl = sh.getStorageUrl();
        if (origUrl == null) {
            throw new CloudRuntimeException("Unable to find the orig.url from host " + sh.toString());
        }
        
        if (templateStoragePoolRef == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Downloading template " + templateId + " to pool " + poolId);
            }
            templateStoragePoolRef = new VMTemplateStoragePoolVO(poolId, templateId);
            try {
                templateStoragePoolRef = _tmpltPoolDao.persist(templateStoragePoolRef);
                templateStoragePoolRefId =  templateStoragePoolRef.getId();
                
            } catch (Exception e) {
                s_logger.debug("Assuming we're in a race condition: " + e.getMessage());
                templateStoragePoolRef = _tmpltPoolDao.findByPoolTemplate(poolId, templateId);
                if (templateStoragePoolRef == null) {
                    throw new CloudRuntimeException("Unable to persist a reference for pool " + poolId + " and template " + templateId);
                }
                templateStoragePoolRefId = templateStoragePoolRef.getId();
            }
        } else {
            templateStoragePoolRefId = templateStoragePoolRef.getId();
        }
        
        List<StoragePoolHostVO> vos = _poolHostDao.listByPoolId(poolId);
        
        templateStoragePoolRef = _tmpltPoolDao.acquire(templateStoragePoolRefId, 1200);
        if (templateStoragePoolRef == null) {
            throw new CloudRuntimeException("Unable to acquire lock on VMTemplateStoragePool: " + templateStoragePoolRefId);
        }

        try {
            if (templateStoragePoolRef.getDownloadState() == Status.DOWNLOADED) {
                return templateStoragePoolRef;
            }
            String url = origUrl + "/" + templateHostRef.getInstallPath();
            PrimaryStorageDownloadCommand dcmd = new PrimaryStorageDownloadCommand(template.getUniqueName(), url, template.getFormat(), template.getAccountId(), pool.getId(), pool.getUuid());
            
            for (StoragePoolHostVO vo : vos) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Downloading " + templateId + " via " + vo.getHostId());
                }
            	dcmd.setLocalPath(vo.getLocalPath());
            	// set 120 min timeout for this command
                DownloadAnswer answer = (DownloadAnswer)_agentMgr.easySend(vo.getHostId(), dcmd, 120*60*1000);
                if (answer != null) {
            		templateStoragePoolRef.setDownloadPercent(templateStoragePoolRef.getDownloadPercent());
            		templateStoragePoolRef.setDownloadState(answer.getDownloadStatus());
            		templateStoragePoolRef.setLocalDownloadPath(answer.getDownloadPath());
            		templateStoragePoolRef.setInstallPath(answer.getInstallPath());
            		templateStoragePoolRef.setTemplateSize(answer.getTemplateSize());
            		_tmpltPoolDao.update(templateStoragePoolRef.getId(), templateStoragePoolRef);
            		if (s_logger.isDebugEnabled()) {
            			s_logger.debug("Template " + templateId + " is downloaded via " + vo.getHostId());
            		}
            		return templateStoragePoolRef;
                }
            }
        } finally {
            _tmpltPoolDao.release(templateStoragePoolRefId);
        }
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Template " + templateId + " is not found on and can not be downloaded to pool " + poolId);
        }
        return null;
    }
    
    @Override
    @DB
    public boolean copy(long userId, long templateId, long sourceZoneId, long destZoneId, long startEventId) throws StorageUnavailableException, InvalidParameterValueException {
    	HostVO srcSecHost = _storageMgr.getSecondaryStorageHost(sourceZoneId);
    	HostVO dstSecHost = _storageMgr.getSecondaryStorageHost(destZoneId);
    	DataCenterVO destZone = _dcDao.findById(destZoneId);
    	if (srcSecHost == null) {
    		throw new StorageUnavailableException("Source zone is not ready");
    	}
    	if (dstSecHost == null) {
    		throw new StorageUnavailableException("Destination zone is not ready");
    	}
    	VMTemplateVO vmTemplate = _tmpltDao.findById(templateId);
    	if (vmTemplate == null) {
    		throw new InvalidParameterValueException("Invalid or illegal template id");
    	}
    	
    	VMTemplateHostVO srcTmpltHost = null;
        srcTmpltHost = _tmpltHostDao.findByHostTemplate(srcSecHost.getId(), templateId);
        if (srcTmpltHost == null) {
        	throw new InvalidParameterValueException("Template " + vmTemplate.getName() + " not associated with " + srcSecHost.getName());
        }
        EventVO event = new EventVO();
        event.setUserId(userId);
        event.setAccountId(vmTemplate.getAccountId());
        event.setType(EventTypes.EVENT_TEMPLATE_COPY);
        event.setState(EventState.Started);
        event.setDescription("Copying template with Id: "+templateId);
        event.setStartId(startEventId);
        event = _eventDao.persist(event);
        
        // Event details
        String params = "id=" + templateId + "\ndcId="+destZoneId+"\nsize="+srcTmpltHost.getSize();
        Account account = _accountDao.findById(vmTemplate.getAccountId());
        String copyEventType;
        String copyEventDescription;
        String createEventType;
        String createEventDescription;
        String templateType;
        if (vmTemplate.getFormat().equals(ImageFormat.ISO)){
            copyEventType = EventTypes.EVENT_ISO_COPY;
            createEventType = EventTypes.EVENT_ISO_CREATE;
            templateType = "ISO ";
        } else {
            copyEventType = EventTypes.EVENT_TEMPLATE_COPY;
            createEventType = EventTypes.EVENT_TEMPLATE_CREATE;
            templateType = "Template ";
        }
        
        copyEventDescription = templateType + vmTemplate.getName() + " started copying to zone: " + destZone.getName() + ".";
        createEventDescription = templateType + vmTemplate.getName() + " succesfully created in zone: " + destZone.getName() + ".";
        
        Transaction txn = Transaction.currentTxn();
        txn.start();

        VMTemplateHostVO dstTmpltHost = null;
        try {
        	dstTmpltHost = _tmpltHostDao.findByHostTemplate(dstSecHost.getId(), templateId, true);
        	if (dstTmpltHost != null) {
        		dstTmpltHost = _tmpltHostDao.lock(dstTmpltHost.getId(), true);
        		if (dstTmpltHost != null && dstTmpltHost.getDownloadState() == Status.DOWNLOADED) {
        			if (dstTmpltHost.getDestroyed() == false)  {
        				return true;
        			} else {
        				dstTmpltHost.setDestroyed(false);
        				_tmpltHostDao.update(dstTmpltHost.getId(), dstTmpltHost);
        				
        				saveEvent(userId, account.getId(), account.getDomainId(), copyEventType, copyEventDescription, EventVO.LEVEL_INFO, params);
        				saveEvent(userId, account.getId(), account.getDomainId(), createEventType, createEventDescription, EventVO.LEVEL_INFO, params);
        				return true;
        			}
        		} else if (dstTmpltHost != null && dstTmpltHost.getDownloadState() == Status.DOWNLOAD_ERROR){
        			if (dstTmpltHost.getDestroyed() == true)  {
        				dstTmpltHost.setDestroyed(false);
        				dstTmpltHost.setDownloadState(Status.NOT_DOWNLOADED);
        				dstTmpltHost.setDownloadPercent(0);
        				dstTmpltHost.setCopy(true);
        				dstTmpltHost.setErrorString("");
        				dstTmpltHost.setJobId(null);
        				_tmpltHostDao.update(dstTmpltHost.getId(), dstTmpltHost);
        			}
        		}
        	}
        } finally {
        	txn.commit();
        }
    	_tmpltDao.addTemplateToZone(vmTemplate, destZoneId);
        
    	_downloadMonitor.copyTemplate(vmTemplate, srcSecHost, dstSecHost);
    	
        saveEvent(userId, account.getId(), account.getDomainId(), copyEventType, copyEventDescription, EventVO.LEVEL_INFO, params, startEventId);
    	return true;
    }
    
    @Override
    public boolean delete(long userId, long templateId, Long zoneId, long startEventId) throws InternalErrorException {
    	boolean success = true;
    	VMTemplateVO template = _tmpltDao.findById(templateId);
    	if (template == null || template.getRemoved() != null) {
    		throw new InternalErrorException("Please specify a valid template.");
    	}
        EventVO event = new EventVO();
        event.setUserId(userId);
        event.setAccountId(template.getAccountId());
        event.setType(EventTypes.EVENT_TEMPLATE_DELETE);
        event.setState(EventState.Started);
        event.setDescription("Deleting template with Id: "+templateId);
        event.setStartId(startEventId);
        event = _eventDao.persist(event);
        
    	String zoneName;
    	List<HostVO> secondaryStorageHosts;
    	if (!template.isCrossZones() && zoneId != null) {
    		DataCenterVO zone = _dcDao.findById(zoneId);
    		zoneName = zone.getName();
    		secondaryStorageHosts = new ArrayList<HostVO>();
			secondaryStorageHosts.add(_hostDao.findSecondaryStorageHost(zoneId));
    	} else {
    		zoneName = "(all zones)";
    		secondaryStorageHosts = _hostDao.listSecondaryStorageHosts();
    	}
    	
    	s_logger.debug("Attempting to mark template host refs for template: " + template.getName() + " as destroyed in zone: " + zoneName);
    	
		// Make sure the template is downloaded to all the necessary secondary storage hosts
		for (HostVO secondaryStorageHost : secondaryStorageHosts) {
			long hostId = secondaryStorageHost.getId();
			List<VMTemplateHostVO> templateHostVOs = _tmpltHostDao.listByHostTemplate(hostId, templateId);
			for (VMTemplateHostVO templateHostVO : templateHostVOs) {
				if (templateHostVO.getDownloadState() == Status.DOWNLOAD_IN_PROGRESS) {
					String errorMsg = "Please specify a template that is not currently being downloaded.";
					s_logger.debug("Template: " + template.getName() + " is currently being downloaded to secondary storage host: " + secondaryStorageHost.getName() + "; cant' delete it.");
					throw new InternalErrorException(errorMsg);
				}
			}
		}
		
		String params = "id=" + template.getId();
		Account account = _accountDao.findById(template.getAccountId());
		String eventType = "";
		String description = "";
		
		if (template.getFormat().equals(ImageFormat.ISO)){
			eventType = EventTypes.EVENT_ISO_DELETE;
			description = "ISO ";
		} else {
			eventType = EventTypes.EVENT_TEMPLATE_DELETE;
			description = "Template ";
		}
		
		// Iterate through all necessary secondary storage hosts and mark the template on each host as destroyed
		for (HostVO secondaryStorageHost : secondaryStorageHosts) {
			long hostId = secondaryStorageHost.getId();
			long sZoneId = secondaryStorageHost.getDataCenterId();
			List<VMTemplateHostVO> templateHostVOs = _tmpltHostDao.listByHostTemplate(hostId, templateId);
			for (VMTemplateHostVO templateHostVO : templateHostVOs) {
				VMTemplateHostVO lock = _tmpltHostDao.acquire(templateHostVO.getId());
				
				try {
					if (lock == null) {
						s_logger.debug("Failed to acquire lock when deleting templateHostVO with ID: " + templateHostVO.getId());
						success = false;
						break;
					}
					
					templateHostVO.setDestroyed(true);
					_tmpltHostDao.update(templateHostVO.getId(), templateHostVO);
					VMTemplateZoneVO templateZone = _tmpltZoneDao.findByZoneTemplate(sZoneId, templateId);
					
					if (templateZone != null) {
						_tmpltZoneDao.remove(templateZone.getId());
					}
					
					String zoneParams = params + "\ndcId=" + sZoneId;
					saveEvent(userId, account.getId(), account.getDomainId(), eventType, description + template.getName() + " succesfully deleted.", EventVO.LEVEL_INFO, zoneParams, startEventId);
				} finally {
					if (lock != null) {
						_tmpltHostDao.release(lock.getId());
					}
				}
			}
			
			if (!success) {
				break;
			}
		}
		
		s_logger.debug("Successfully marked template host refs for template: " + template.getName() + " as destroyed in zone: " + zoneName);
		
		// If there are no more non-destroyed template host entries for this template, delete it
		if (success && (_tmpltHostDao.listByTemplateId(templateId).size() == 0)) {
			long accountId = template.getAccountId();
			
			VMTemplateVO lock = _tmpltDao.acquire(templateId);

			try {
				if (lock == null) {
					s_logger.debug("Failed to acquire lock when deleting template with ID: " + templateId);
					success = false;
				} else if (_tmpltDao.remove(templateId)) {
					// Decrement the number of templates
					_accountMgr.decrementResourceCount(accountId, ResourceType.template);
				}

			} finally {
				if (lock != null) {
					_tmpltDao.release(lock.getId());
				}
			}
			
			s_logger.debug("Removed template: " + template.getName() + " because all of its template host refs were marked as destroyed.");
		}
		
		return success;
    }
    
    public List<VMTemplateStoragePoolVO> getUnusedTemplatesInPool(StoragePoolVO pool) {
		List<VMTemplateStoragePoolVO> unusedTemplatesInPool = new ArrayList<VMTemplateStoragePoolVO>();
		List<VMTemplateStoragePoolVO> allTemplatesInPool = _tmpltPoolDao.listByPoolId(pool.getId());
		
		for (VMTemplateStoragePoolVO templatePoolVO : allTemplatesInPool) {
			VMTemplateVO template = _tmpltDao.findById(templatePoolVO.getTemplateId());
			
			// If this is a routing template, consider it in use
			if (template.getUniqueName().equals("routing") && pool.isShared() ) {
				continue;
			}
			
			// If the template is not yet downloaded to the pool, consider it in use
			if (templatePoolVO.getDownloadState() != Status.DOWNLOADED) {
				continue;
			}

			if (template.getFormat() != ImageFormat.ISO && !_volumeDao.isAnyVolumeActivelyUsingTemplateOnPool(template.getId(), pool.getId())) {
                unusedTemplatesInPool.add(templatePoolVO);
			}
		}
		
		return unusedTemplatesInPool;
	}
    
    public void evictTemplateFromStoragePool(VMTemplateStoragePoolVO templatePoolVO) {
		StoragePoolVO pool = _poolDao.findById(templatePoolVO.getPoolId());
		VMTemplateVO template = _tmpltDao.findById(templatePoolVO.getTemplateId());
		
		long hostId;
		List<StoragePoolHostVO> poolHostVOs = _poolHostDao.listByPoolId(pool.getId());
		if (poolHostVOs.isEmpty()) {
			return;
		} else {
			hostId = poolHostVOs.get(0).getHostId();
		}
		
		DestroyCommand cmd = new DestroyCommand(pool, templatePoolVO);
		Answer answer = _agentMgr.easySend(hostId, cmd);
		
    	if (answer != null && answer.getResult()) {
    		// Remove the templatePoolVO
    		if (_tmpltPoolDao.remove(templatePoolVO.getId())) {
    			s_logger.debug("Successfully evicted template: " + template.getName() + " from storage pool: " + pool.getName());
    		}
    	}
	}
    
    private Long saveEvent(long userId, Long accountId, Long domainId, String type, String description, String level, String params, long startEventId) {
        EventVO event = new EventVO();
        event.setUserId(userId);
        event.setAccountId(accountId);
        event.setType(type);
        event.setDescription(description);
        event.setStartId(startEventId);
        
        if (domainId != null) {
        	event.setDomainId(domainId);
        }
        
        if (level != null) {
        	event.setLevel(level);
        }
        
        if (params != null) {
        	event.setParameters(params);
        }
        
        return _eventDao.persist(event).getId();
    }
    
    private Long saveEvent(long userId, Long accountId, Long domainId, String type, String description, String level, String params) {
        return saveEvent(userId, accountId, domainId, type, description, level, params,0); 
    }
    
    @Override
    public String getName() {
        return _name;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;
        
        ComponentLocator locator = ComponentLocator.getCurrentLocator();
        ConfigurationDao configDao = locator.getDao(ConfigurationDao.class);
        
        if (configDao == null) {
            throw new ConfigurationException("Unable to find ConfigurationDao");
        }
        
        final Map<String, String> configs = configDao.getConfiguration("AgentManager", params);
        _routerTemplateId = NumbersUtil.parseInt(configs.get("router.template.id"), 1);
        
        HostTemplateStatesSearch = _tmpltHostDao.createSearchBuilder();
        HostTemplateStatesSearch.and("id", HostTemplateStatesSearch.entity().getTemplateId(), SearchCriteria.Op.EQ);
        HostTemplateStatesSearch.and("state", HostTemplateStatesSearch.entity().getDownloadState(), SearchCriteria.Op.EQ);
        
        SearchBuilder<HostVO> HostSearch = _hostDao.createSearchBuilder();
        HostSearch.and("dcId", HostSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        
        HostTemplateStatesSearch.join("host", HostSearch, HostSearch.entity().getId(), HostTemplateStatesSearch.entity().getHostId());
        HostSearch.done();
        HostTemplateStatesSearch.done();
        
        return false;
    }
    
    protected TemplateManagerImpl() {
    }

	@Override
	public Long createInZone(long zoneId, long userId, String displayText,
			boolean isPublic, boolean featured, ImageFormat format,
			FileSystem fs, URI url, String chksum, boolean requiresHvm,
			int bits, boolean enablePassword, long guestOSId, boolean bootable) {
		Long id = _tmpltDao.getNextInSequence(Long.class, "id");

		UserVO user = _userDao.findById(userId);
		long accountId = user.getAccountId();

		VMTemplateVO template = new VMTemplateVO(id, displayText, format, isPublic, featured, fs, url.toString(), requiresHvm, bits, accountId, chksum, displayText, enablePassword, guestOSId, bootable);

		Long templateId = _tmpltDao.addTemplateToZone(template, zoneId);
		UserAccount userAccount = _userAccountDao.findById(userId);
		saveEvent(userId, userAccount.getAccountId(), userAccount.getDomainId(), EventTypes.EVENT_TEMPLATE_DOWNLOAD_START,
				"Started download of template:  " + template.getName(), null, null);

		_downloadMonitor.downloadTemplateToStorage(id, zoneId);

		_accountMgr.incrementResourceCount(userAccount.getAccountId(), ResourceType.template);

		return templateId;
	}
	
	public boolean templateIsDeleteable(VMTemplateHostVO templateHostRef) {
		VMTemplateVO template = _tmpltDao.findById(templateHostRef.getTemplateId());
		long templateId = template.getId();
		HostVO secondaryStorageHost = _hostDao.findById(templateHostRef.getHostId());
		long zoneId = secondaryStorageHost.getDataCenterId();
		DataCenterVO zone = _dcDao.findById(zoneId);
		
		// Check if there are VMs running in the template host ref's zone that use the template
		List<VMInstanceVO> nonExpungedVms = _vmInstanceDao.listNonExpungedByZoneAndTemplate(zoneId, templateId);
		
		if (!nonExpungedVms.isEmpty()) {
			s_logger.debug("Template " + template.getName() + " in zone " + zone.getName() + " is not deleteable because there are non-expunged VMs deployed from this template.");
			return false;
		}
		
		// Check if there are any snapshots for the template in the template host ref's zone
		List<VolumeVO> volumes = _volumeDao.findByTemplateAndZone(templateId, zoneId);
		if( volumes.size() > 0 ) {
		    return false;
		}
		for (VolumeVO volume : volumes) {
			List<SnapshotVO> snapshots = _snapshotDao.listByVolumeId(volume.getId());
			if (!snapshots.isEmpty()) {
				s_logger.debug("Template " + template.getName() + " in zone " + zone.getName() + " is not deleteable because there are snapshots using this template.");
				return false;
			}
		}
		
		return true;
	}
}
