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

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.DestroyCommand;
import com.cloud.agent.api.storage.DownloadAnswer;
import com.cloud.agent.api.storage.PrimaryStorageDownloadCommand;
import com.cloud.agent.manager.AgentManager;
import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.api.commands.AttachIsoCmd;
import com.cloud.api.commands.CopyIsoCmd;
import com.cloud.api.commands.CopyTemplateCmd;
import com.cloud.api.commands.DeleteIsoCmd;
import com.cloud.api.commands.DeleteTemplateCmd;
import com.cloud.api.commands.DetachIsoCmd;
import com.cloud.api.commands.RegisterIsoCmd;
import com.cloud.api.commands.RegisterTemplateCmd;
import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.EventState;
import com.cloud.event.EventTypes;
import com.cloud.event.EventUtils;
import com.cloud.event.EventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.exception.InternalErrorException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.FileSystem;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VMTemplateZoneVO;
import com.cloud.storage.VolumeVO;
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
import com.cloud.user.UserContext;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserAccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.EnumUtils;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.State;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.UserVmDao;
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
    @Inject UserVmDao _userVmDao;
    @Inject VolumeDao _volumeDao;
    @Inject SnapshotDao _snapshotDao;
    @Inject DomainDao _domainDao;
    long _routerTemplateId = -1;
    @Inject StorageManager _storageMgr;
    @Inject UserVmManager _vmMgr;
    @Inject ConfigurationDao _configDao;
    protected SearchBuilder<VMTemplateHostVO> HostTemplateStatesSearch;
    
    @Override
    public Long registerIso(RegisterIsoCmd cmd) throws InvalidParameterValueException, IllegalArgumentException, ResourceAllocationException{
        Account account = (Account)UserContext.current().getAccountObject();
        Long userId = UserContext.current().getUserId();
        String name = cmd.getName();
        String displayText = cmd.getDisplayText();
        String url = cmd.getUrl();
        Boolean isPublic = cmd.isPublic();
        Boolean featured = cmd.isFeatured();
        Long guestOSId = cmd.getOsTypeId();
        Boolean bootable = cmd.isBootable();
        Long zoneId = cmd.getZoneId();

        if (isPublic == null) {
            isPublic = Boolean.FALSE;
        }
        
        if (zoneId.longValue() == -1) {
        	zoneId = null;
        }
        
        long accountId = 1L; // default to system account
        if (account != null) {
            accountId = account.getId().longValue();
        }
        
        Account accountObj;
        if (account == null) {
        	accountObj = _accountDao.findById(accountId);
        } else {
        	accountObj = account;
        }
        
        boolean isAdmin = (accountObj.getType() == Account.ACCOUNT_TYPE_ADMIN);
        
        if (!isAdmin && zoneId == null) {
        	throw new ServerApiException(BaseCmd.PARAM_ERROR, "Please specify a valid zone Id.");
        }
        
        if((!url.toLowerCase().endsWith("iso"))&&(!url.toLowerCase().endsWith("iso.zip"))&&(!url.toLowerCase().endsWith("iso.bz2"))
        		&&(!url.toLowerCase().endsWith("iso.gz"))){
        	throw new ServerApiException(BaseCmd.PARAM_ERROR, "Please specify a valid iso");
        }
        
        boolean allowPublicUserTemplates = Boolean.parseBoolean(_configDao.getValue("allow.public.user.templates"));        
        if (!isAdmin && !allowPublicUserTemplates && isPublic) {
        	throw new ServerApiException(BaseCmd.PARAM_ERROR, "Only private ISOs can be created.");
        }
        
        if (!isAdmin || featured == null) {
        	featured = Boolean.FALSE;
        }

        // If command is executed via 8096 port, set userId to the id of System account (1)
        if (userId == null) {
            userId = Long.valueOf(1);
        }
        
        if (bootable == null) {
        	bootable = Boolean.TRUE;
        }

        //removing support for file:// type urls (bug: 4239)
        if(url.toLowerCase().contains("file://")){
        	throw new ServerApiException(BaseCmd.PARAM_ERROR, "File:// type urls are currently unsupported");
        }
        
        return createTemplateOrIso(userId, zoneId, name, displayText, isPublic.booleanValue(), featured.booleanValue(), ImageFormat.ISO.toString(), FileSystem.cdfs.toString(), url, null, true, 64 /*bits*/, false, guestOSId, bootable);
    }

    @Override
    public Long registerTemplate(RegisterTemplateCmd cmd) throws InvalidParameterValueException, URISyntaxException, ResourceAllocationException{
    	
        Account account = (Account)UserContext.current().getAccountObject();
        Long userId = UserContext.current().getUserId();
        String name = cmd.getName();
        String displayText = cmd.getDisplayText(); 
        Integer bits = cmd.getBits();
        Boolean passwordEnabled = cmd.isPasswordEnabled();
        Boolean requiresHVM = cmd.getRequiresHvm();
        String url = cmd.getUrl();
        Boolean isPublic = cmd.isPublic();
        Boolean featured = cmd.isFeatured();
        String format = cmd.getFormat();
        Long guestOSId = cmd.getOsTypeId();
        Long zoneId = cmd.getZoneId();

        //parameters verification
        if (bits == null) {
            bits = Integer.valueOf(64);
        }
        if (passwordEnabled == null) {
            passwordEnabled = false;
        }
        if (requiresHVM == null) {
            requiresHVM = true;
        }
        if (isPublic == null) {
            isPublic = Boolean.FALSE;
        }
        
        if (zoneId.longValue() == -1) {
        	zoneId = null;
        }
                
        long accountId = 1L; // default to system account
        if (account != null) {
            accountId = account.getId().longValue();
        }
        
        Account accountObj;
        if (account == null) {
        	accountObj = _accountDao.findById(accountId);
        } else {
        	accountObj = account;
        }
        
        boolean isAdmin = (accountObj.getType() == Account.ACCOUNT_TYPE_ADMIN);
        
        if (!isAdmin && zoneId == null) {
        	throw new ServerApiException(BaseCmd.PARAM_ERROR, "Please specify a valid zone Id.");
        }
        
        if(url.toLowerCase().contains("file://")){
        	throw new ServerApiException(BaseCmd.PARAM_ERROR, "File:// type urls are currently unsupported");
        }
        
        if((!url.toLowerCase().endsWith("vhd"))&&(!url.toLowerCase().endsWith("vhd.zip"))
        	&&(!url.toLowerCase().endsWith("vhd.bz2"))&&(!url.toLowerCase().endsWith("vhd.gz") 
        	&&(!url.toLowerCase().endsWith("qcow2"))&&(!url.toLowerCase().endsWith("qcow2.zip"))
        	&&(!url.toLowerCase().endsWith("qcow2.bz2"))&&(!url.toLowerCase().endsWith("qcow2.gz")))){
        	throw new ServerApiException(BaseCmd.PARAM_ERROR, "Please specify a valid "+format.toLowerCase());
        }
        	
        boolean allowPublicUserTemplates = Boolean.parseBoolean(_configDao.getValue("allow.public.user.templates"));        
        if (!isAdmin && !allowPublicUserTemplates && isPublic) {
        	throw new ServerApiException(BaseCmd.PARAM_ERROR, "Only private templates can be created.");
        }
        
        if (!isAdmin || featured == null) {
        	featured = Boolean.FALSE;
        }

        //If command is executed via 8096 port, set userId to the id of System account (1)
        if (userId == null) {
            userId = Long.valueOf(1);
        }
        
        return createTemplateOrIso(userId, zoneId, name, displayText, isPublic, featured, format, "ext3", url, null, requiresHVM, bits, passwordEnabled, guestOSId, true);
    	
    }
    
    private Long createTemplateOrIso(long userId, Long zoneId, String name, String displayText, boolean isPublic, boolean featured, String format, String diskType, String url, String chksum, boolean requiresHvm, int bits, boolean enablePassword, long guestOSId, boolean bootable) throws InvalidParameterValueException,IllegalArgumentException, ResourceAllocationException {
        try
        {
            if (name.length() > 32)
            {
                throw new InvalidParameterValueException("Template name should be less than 32 characters");
            }
            	
            if (!name.matches("^[\\p{Alnum} ._-]+")) {
                throw new InvalidParameterValueException("Only alphanumeric, space, dot, dashes and underscore characters allowed");
            }
        	
            ImageFormat imgfmt = ImageFormat.valueOf(format.toUpperCase());
            if (imgfmt == null) {
                throw new IllegalArgumentException("Image format is incorrect " + format + ". Supported formats are " + EnumUtils.listValues(ImageFormat.values()));
            }
            
            FileSystem fileSystem = FileSystem.valueOf(diskType);
            if (fileSystem == null) {
                throw new IllegalArgumentException("File system is incorrect " + diskType + ". Supported file systems are " + EnumUtils.listValues(FileSystem.values()));
            }
            
            URI uri = new URI(url);
            if ((uri.getScheme() == null) || (!uri.getScheme().equalsIgnoreCase("http") && !uri.getScheme().equalsIgnoreCase("https") && !uri.getScheme().equalsIgnoreCase("file"))) {
               throw new IllegalArgumentException("Unsupported scheme for url: " + url);
            }
            int port = uri.getPort();
            if (!(port == 80 || port == 443 || port == -1)) {
            	throw new IllegalArgumentException("Only ports 80 and 443 are allowed");
            }
            String host = uri.getHost();
            try {
            	InetAddress hostAddr = InetAddress.getByName(host);
            	if (hostAddr.isAnyLocalAddress() || hostAddr.isLinkLocalAddress() || hostAddr.isLoopbackAddress() || hostAddr.isMulticastAddress() ) {
            		throw new IllegalArgumentException("Illegal host specified in url");
            	}
            	if (hostAddr instanceof Inet6Address) {
            		throw new IllegalArgumentException("IPV6 addresses not supported (" + hostAddr.getHostAddress() + ")");
            	}
            } catch (UnknownHostException uhe) {
            	throw new IllegalArgumentException("Unable to resolve " + host);
            }
            
            // Check that the resource limit for templates/ISOs won't be exceeded
            UserVO user = _userDao.findById(userId);
            if (user == null) {
                throw new IllegalArgumentException("Unable to find user with id " + userId);
            }
        	AccountVO account = _accountDao.findById(user.getAccountId());
            if (_accountMgr.resourceLimitExceeded(account, ResourceType.template)) {
            	ResourceAllocationException rae = new ResourceAllocationException("Maximum number of templates and ISOs for account: " + account.getAccountName() + " has been exceeded.");
            	rae.setResourceType("template");
            	throw rae;
            }
            
            // If a zoneId is specified, make sure it is valid
            if (zoneId != null) {
            	if (_dcDao.findById(zoneId) == null) {
            		throw new IllegalArgumentException("Please specify a valid zone.");
            	}
            }
            VMTemplateVO systemvmTmplt = _tmpltDao.findRoutingTemplate();
            if (systemvmTmplt.getName().equalsIgnoreCase(name) || systemvmTmplt.getDisplayText().equalsIgnoreCase(displayText)) {
            	throw new IllegalArgumentException("Cannot use reserved names for templates");
            }
            
            return create(userId, zoneId, name, displayText, isPublic, featured, imgfmt, fileSystem, uri, chksum, requiresHvm, bits, enablePassword, guestOSId, bootable);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL " + url);
        }
    }

    private Long create(long userId, Long zoneId, String name, String displayText, boolean isPublic, boolean featured, ImageFormat format, FileSystem fs, URI url, String chksum, boolean requiresHvm, int bits, boolean enablePassword, long guestOSId, boolean bootable) {
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
        
        SearchCriteria<VMTemplateHostVO> sc = HostTemplateStatesSearch.create();
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
                DownloadAnswer answer = (DownloadAnswer)_agentMgr.easySend(vo.getHostId(), dcmd);
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
    	
    	DataCenterVO sourceZone = _dcDao.findById(sourceZoneId);
		if (sourceZone == null) {
			throw new InvalidParameterValueException("Please specify a valid source zone.");
		}
		
		DataCenterVO dstZone = _dcDao.findById(destZoneId);
		if (dstZone == null) {
			throw new InvalidParameterValueException("Please specify a valid destination zone.");
		}
		
    	if (sourceZoneId == destZoneId) {
    		throw new InvalidParameterValueException("Please specify different source and destination zones.");
    	}
    	
    	if (srcSecHost == null) {
    		throw new StorageUnavailableException("Source zone is not ready");
    	}
    	if (dstSecHost == null) {
    		throw new StorageUnavailableException("Destination zone is not ready");
    	}
    	
    	VMTemplateVO vmTemplate = _tmpltDao.findById(templateId);
    	VMTemplateHostVO srcTmpltHost = null;
        srcTmpltHost = _tmpltHostDao.findByHostTemplate(srcSecHost.getId(), templateId);
        if (srcTmpltHost == null || srcTmpltHost.getDestroyed() || srcTmpltHost.getDownloadState() != VMTemplateStorageResourceAssoc.Status.DOWNLOADED) {
	      	throw new InvalidParameterValueException("Please specify a template that is installed on secondary storage host: " + srcSecHost.getName());
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
    public VMTemplateVO copyIso(CopyIsoCmd cmd) throws InvalidParameterValueException, StorageUnavailableException, PermissionDeniedException {
    	Long isoId = cmd.getId();
    	Long userId = UserContext.current().getUserId();
    	Long sourceZoneId = cmd.getSourceZoneId();
    	Long destZoneId = cmd.getDestinationZoneId();
    	Account account = (Account)UserContext.current().getAccountObject();
    	
        //Verify parameters
        VMTemplateVO iso = _tmpltDao.findById(isoId);
        if (iso == null) {
            throw new InvalidParameterValueException("Unable to find ISO with id " + isoId);
        }
        
        boolean isIso = Storage.ImageFormat.ISO.equals(iso.getFormat());
        if (!isIso) {
        	throw new InvalidParameterValueException("Please specify a valid ISO.");
        }
        
        //Verify account information
        String errMsg = "Unable to copy ISO " + isoId;
        userId = accountAndUserValidation(account, userId, null, iso, errMsg);
        
        long eventId = EventUtils.saveScheduledEvent(userId, iso.getAccountId(), EventTypes.EVENT_ISO_COPY, "copying iso with Id: " + isoId +" from zone: " + sourceZoneId +" to: " + destZoneId);
        boolean success = copy(userId, isoId, sourceZoneId, destZoneId, eventId);
        
        VMTemplateVO copiedIso = null;
        if (success) 
        	copiedIso = _tmpltDao.findById(isoId);
       
        return copiedIso;
    }
    
    
    @Override
    public VMTemplateVO copyTemplate(CopyTemplateCmd cmd) throws InvalidParameterValueException, StorageUnavailableException, PermissionDeniedException {
    	Long templateId = cmd.getId();
    	Long userId = UserContext.current().getUserId();
    	Long sourceZoneId = cmd.getSourceZoneId();
    	Long destZoneId = cmd.getDestinationZoneId();
    	Account account = (Account)UserContext.current().getAccountObject();
        
        //Verify parameters
        VMTemplateVO template = _tmpltDao.findById(templateId);
        if (template == null) {
            throw new InvalidParameterValueException("Unable to find template with id");
        }
        
        boolean isIso = Storage.ImageFormat.ISO.equals(template.getFormat());
        if (isIso) {
        	throw new InvalidParameterValueException("Please specify a valid template.");
        }
        
        //Verify account information
        String errMsg = "Unable to copy template " + templateId;
        userId = accountAndUserValidation(account, userId, null, template, errMsg);
        
        long eventId = EventUtils.saveScheduledEvent(userId, template.getAccountId(), EventTypes.EVENT_TEMPLATE_COPY, "copying template with Id: " + templateId+" from zone: " + sourceZoneId +" to: " + destZoneId);
        boolean success = copy(userId, templateId, sourceZoneId, destZoneId, eventId);
        
        VMTemplateVO copiedTemplate = null;
        if (success) 
        	copiedTemplate = _tmpltDao.findById(templateId);
       
        return copiedTemplate;
    }
    
    @Override
    public boolean delete(long userId, long templateId, Long zoneId) throws InternalErrorException {
    	boolean success = true;
    	VMTemplateVO template = _tmpltDao.findById(templateId);
    	if (template == null || template.getRemoved() != null) {
    		throw new InternalErrorException("Please specify a valid template.");
    	}
        
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
					saveEvent(userId, account.getId(), account.getDomainId(), eventType, description + template.getName() + " succesfully deleted.", EventVO.LEVEL_INFO, zoneParams, 0);
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
			if (template.getUniqueName().equals("routing")) {
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
		for (VolumeVO volume : volumes) {
			List<SnapshotVO> snapshots = _snapshotDao.listByVolumeId(volume.getId());
			if (!snapshots.isEmpty()) {
				s_logger.debug("Template " + template.getName() + " in zone " + zone.getName() + " is not deleteable because there are snapshots using this template.");
				return false;
			}
		}
		
		return true;
	}

	@Override
	public boolean detachIso(DetachIsoCmd cmd) throws InternalErrorException, InvalidParameterValueException, PermissionDeniedException {
        Account account = (Account) UserContext.current().getAccountObject();
        Long userId = UserContext.current().getUserId();
        Long vmId = cmd.getVirtualMachineId();
        
        // Verify input parameters
        UserVmVO vmInstanceCheck = _userVmDao.findById(vmId.longValue());
        if (vmInstanceCheck == null) {
            throw new ServerApiException (BaseCmd.VM_INVALID_PARAM_ERROR, "Unable to find a virtual machine with id " + vmId);
        }
        
        UserVm userVM = _userVmDao.findById(vmId);
        if (userVM == null) {
            throw new InvalidParameterValueException("Please specify a valid VM.");
        }

        Long isoId = userVM.getIsoId();
        if (isoId == null) {
            throw new InvalidParameterValueException("The specified VM has no ISO attached to it.");
        }
        
        State vmState = userVM.getState();
        if (vmState != State.Running && vmState != State.Stopped) {
        	throw new InvalidParameterValueException("Please specify a VM that is either Stopped or Running.");
        }
        
        String errMsg = "Unable to detach ISO " + isoId + " from virtual machine";
        userId = accountAndUserValidation(account, userId, vmInstanceCheck, null, errMsg);
        
        return attachISOToVM(vmId, userId, isoId, false); //attach=false => detach

	}
	
	@Override
	public boolean attachIso(AttachIsoCmd cmd) throws InternalErrorException, InvalidParameterValueException, PermissionDeniedException {
        Account account = (Account) UserContext.current().getAccountObject();
        Long userId = UserContext.current().getUserId();
        Long vmId = cmd.getVirtualMachineId();
        Long isoId = cmd.getId();
        
    	// Verify input parameters
    	UserVmVO vmInstanceCheck = _userVmDao.findById(vmId);
    	if (vmInstanceCheck == null) {
            throw new InvalidParameterValueException("Unable to find a virtual machine with id " + vmId);
        }
    	
    	VMTemplateVO iso = _tmpltDao.findById(isoId);
    	if (iso == null) {
            throw new ServerApiException (BaseCmd.PARAM_ERROR, "Unable to find an ISO with id " + isoId);
    	}
    	
        State vmState = vmInstanceCheck.getState();
        if (vmState != State.Running && vmState != State.Stopped) {
        	throw new InvalidParameterValueException("Please specify a VM that is either Stopped or Running.");
        }
        
        String errMsg = "Unable to attach ISO" + isoId + "to virtual machine " + vmId;
        userId = accountAndUserValidation(account, userId, vmInstanceCheck, iso, errMsg);
        
        return attachISOToVM(vmId, userId, isoId, true);
	}

    private boolean attachISOToVM(long vmId, long userId, long isoId, boolean attach) {
    	UserVmVO vm = _userVmDao.findById(vmId);
    	VMTemplateVO iso = _tmpltDao.findById(isoId);
    	long startEventId = 0;
    	if(attach){
    		startEventId = EventUtils.saveStartedEvent(userId, vm.getAccountId(), EventTypes.EVENT_ISO_ATTACH, "Attaching ISO: "+isoId+" to Vm: "+vmId, startEventId);
    	} else {
    		startEventId = EventUtils.saveStartedEvent(userId, vm.getAccountId(), EventTypes.EVENT_ISO_DETACH, "Detaching ISO: "+isoId+" from Vm: "+vmId, startEventId);
    	}
        boolean success = _vmMgr.attachISOToVM(vmId, isoId, attach);

        if (success) {
            if (attach) {
                vm.setIsoId(iso.getId().longValue());
            } else {
                vm.setIsoId(null);
            }
            _userVmDao.update(vmId, vm);

            if (attach) {
            	EventUtils.saveEvent(userId, vm.getAccountId(), EventVO.LEVEL_INFO, EventTypes.EVENT_ISO_ATTACH, "Successfully attached ISO: " + iso.getName() + " to VM with ID: " + vmId,
                        null, startEventId);
            } else {
            	EventUtils.saveEvent(userId, vm.getAccountId(), EventVO.LEVEL_INFO, EventTypes.EVENT_ISO_DETACH, "Successfully detached ISO from VM with ID: " + vmId, null, startEventId);
            }
        } else {
            if (attach) {
            	EventUtils.saveEvent(userId, vm.getAccountId(), EventVO.LEVEL_ERROR, EventTypes.EVENT_ISO_ATTACH, "Failed to attach ISO: " + iso.getName() + " to VM with ID: " + vmId, null, startEventId);
            } else {
            	EventUtils.saveEvent(userId, vm.getAccountId(), EventVO.LEVEL_ERROR, EventTypes.EVENT_ISO_DETACH, "Failed to detach ISO from VM with ID: " + vmId, null, startEventId);
            }
        }

        return success;
    }

	private Long accountAndUserValidation(Account account, Long userId, UserVmVO vmInstanceCheck, VMTemplateVO template, String msg) throws PermissionDeniedException{
		
    	if (account != null) {
    	    if (!isAdmin(account.getType())) {
				if ((vmInstanceCheck != null) && (account.getId().longValue() != vmInstanceCheck.getAccountId())) {
		            throw new PermissionDeniedException(msg + ". Permission denied.");
		        }

	    		if ((template != null) && (!template.isPublicTemplate() && (account.getId().longValue() != template.getAccountId()) && (!template.getName().startsWith("xs-tools")))) {
                    throw new PermissionDeniedException(msg + ". Permission denied.");
                }
                
    	    } else {
    	        if ((vmInstanceCheck != null) && !_domainDao.isChildDomain(account.getDomainId(), vmInstanceCheck.getDomainId())) {
                    throw new PermissionDeniedException(msg + ". Permission denied.");
    	        }
    	        // FIXME:  if template/ISO owner is null we probably need to throw some kind of exception
    	        
    	        if (template != null) {
    	        	Account templateOwner = _accountDao.findById(template.getId());
	    	        if ((templateOwner != null) && !_domainDao.isChildDomain(account.getDomainId(), templateOwner.getDomainId())) {
	                    throw new PermissionDeniedException(msg + ". Permission denied.");
	    	        }
    	        }
    	    }
    	}
        // If command is executed via 8096 port, set userId to the id of System account (1)
        if (userId == null)
            userId = new Long(1);
        
        return userId;
	}
	
	private static boolean isAdmin(short accountType) {
	    return ((accountType == Account.ACCOUNT_TYPE_ADMIN) ||
	            (accountType == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) ||
	            (accountType == Account.ACCOUNT_TYPE_READ_ONLY_ADMIN));
	}
	
	@Override
    public boolean deleteTemplate(DeleteTemplateCmd cmd) throws InvalidParameterValueException, InternalErrorException, PermissionDeniedException {
        Long templateId = cmd.getId();
        Long userId = UserContext.current().getUserId();
        Account account = (Account)UserContext.current().getAccountObject();
        Long zoneId = cmd.getZoneId();
        
        VMTemplateVO template = _tmpltDao.findById(templateId.longValue());
        if (template == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to find template with id " + templateId);
        }
        
        userId = accountAndUserValidation(account, userId, null, template, "Unable to delete template " );
        
    	UserVO user = _userDao.findById(userId);
    	if (user == null) {
    		throw new InvalidParameterValueException("Please specify a valid user.");
    	}
    	
    	if (template.getFormat() == ImageFormat.ISO) {
    		throw new InvalidParameterValueException("Please specify a valid template.");
    	}
    	
    	if (template.getUniqueName().equals("routing")) {
    		throw new InvalidParameterValueException("The DomR template cannot be deleted.");
    	}
    	
    	if (zoneId != null && (_hostDao.findSecondaryStorageHost(zoneId) == null)) {
    		throw new InvalidParameterValueException("Failed to find a secondary storage host in the specified zone.");
    	}
    	return delete(userId, templateId, zoneId);
	}
	
	@Override
    public boolean deleteIso(DeleteIsoCmd cmd) throws InvalidParameterValueException, InternalErrorException, PermissionDeniedException {
        Long templateId = cmd.getId();
        Long userId = UserContext.current().getUserId();
        Account account = (Account)UserContext.current().getAccountObject();
        Long zoneId = cmd.getZoneId();
        
        VMTemplateVO template = _tmpltDao.findById(templateId.longValue());
        if (template == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to find iso with id " + templateId);
        }
        
        userId = accountAndUserValidation(account, userId, null, template, "Unable to delete iso " );
        
    	UserVO user = _userDao.findById(userId);
    	if (user == null) {
    		throw new InvalidParameterValueException("Please specify a valid user.");
    	}
    	
    	if (template.getFormat() != ImageFormat.ISO) {
    		throw new InvalidParameterValueException("Please specify a valid iso.");
    	}
    	
    	if (zoneId != null && (_hostDao.findSecondaryStorageHost(zoneId) == null)) {
    		throw new InvalidParameterValueException("Failed to find a secondary storage host in the specified zone.");
    	}
    	return delete(userId, templateId, zoneId);
	}
}
