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

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.DestroyCommand;
import com.cloud.agent.api.storage.PrimaryStorageDownloadAnswer;
import com.cloud.agent.api.storage.PrimaryStorageDownloadCommand;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.api.commands.AttachIsoCmd;
import com.cloud.api.commands.CopyIsoCmd;
import com.cloud.api.commands.CopyTemplateCmd;
import com.cloud.api.commands.DeleteIsoCmd;
import com.cloud.api.commands.DeleteTemplateCmd;
import com.cloud.api.commands.DetachIsoCmd;
import com.cloud.api.commands.ExtractIsoCmd;
import com.cloud.api.commands.ExtractTemplateCmd;
import com.cloud.api.commands.RegisterIsoCmd;
import com.cloud.api.commands.RegisterTemplateCmd;
import com.cloud.async.AsyncJobManager;
import com.cloud.async.AsyncJobVO;
import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.EventTypes;
import com.cloud.event.EventUtils;
import com.cloud.event.EventVO;
import com.cloud.event.UsageEventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.Upload;
import com.cloud.storage.Upload.Type;
import com.cloud.storage.UploadVO;
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
import com.cloud.storage.dao.UploadDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.download.DownloadMonitor;
import com.cloud.storage.upload.UploadMonitor;
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
import com.cloud.utils.component.Manager;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;


@Local(value={TemplateManager.class, TemplateService.class})
public class TemplateManagerImpl implements TemplateManager, Manager, TemplateService {
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
    @Inject UploadMonitor _uploadMonitor;
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
    @Inject UploadDao _uploadDao;
    long _routerTemplateId = -1;
    @Inject StorageManager _storageMgr;
    @Inject AsyncJobManager _asyncMgr;
    @Inject UserVmManager _vmMgr;
    @Inject ConfigurationDao _configDao;
    @Inject UsageEventDao _usageEventDao;
    protected SearchBuilder<VMTemplateHostVO> HostTemplateStatesSearch;
    
    @Override
    public VirtualMachineTemplate registerIso(RegisterIsoCmd cmd) throws ResourceAllocationException{
        Account ctxAccount = UserContext.current().getCaller();
        Long userId = UserContext.current().getCallerUserId();
        String name = cmd.getIsoName();
        String displayText = cmd.getDisplayText();
        String url = cmd.getUrl();
        Boolean isPublic = cmd.isPublic();
        Boolean featured = cmd.isFeatured();
        Long guestOSId = cmd.getOsTypeId();
        Boolean bootable = cmd.isBootable();
        Long zoneId = cmd.getZoneId();
        String accountName = cmd.getAccountName();
        Long domainId = cmd.getDomainId();
        Account resourceAccount = null;
        Long accountId = null;
        
        if (isPublic == null) {
            isPublic = Boolean.FALSE;
        }
        
        if (zoneId.longValue() == -1) {
        	zoneId = null;
        }        
        
        if ( (accountName == null) ^ (domainId == null) ){// XOR - Both have to be passed or don't pass any of them 
        	throw new InvalidParameterValueException("Please specify both account and domainId or dont specify any of them");
        }
        
        if ((ctxAccount == null) || isAdmin(ctxAccount.getType())) {
            if (domainId != null) {
                if ((ctxAccount != null) && !_domainDao.isChildDomain(ctxAccount.getDomainId(), domainId)) {
                    throw new PermissionDeniedException("Failed to register ISO, invalid domain id (" + domainId + ") given.");
                }
                if (accountName != null) {
                	resourceAccount = _accountDao.findActiveAccount(accountName, domainId);
                    if (resourceAccount == null) {
                        throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId);
                    }
                    accountId = resourceAccount.getId();
                }
            } else {
                accountId = ((ctxAccount != null) ? ctxAccount.getId() : null);
            }
        } else {
            accountId = ctxAccount.getId();
        }
        
        if (null == accountId && null == accountName && null == domainId && null == ctxAccount){
        	accountId = 1L;
        }
        if (accountId == null) {
            throw new InvalidParameterValueException("No valid account specified for registering an ISO.");
        }
        
        boolean isAdmin = _accountDao.findById(accountId).getType() == Account.ACCOUNT_TYPE_ADMIN;
        
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
        
        return createTemplateOrIso(userId, accountId, zoneId, name, displayText, isPublic.booleanValue(), featured.booleanValue(), true, ImageFormat.ISO.toString(), TemplateType.USER, url, null, true, 64 /*bits*/, false, guestOSId, bootable, HypervisorType.None);
    }

    @Override
    public VirtualMachineTemplate registerTemplate(RegisterTemplateCmd cmd) throws URISyntaxException, ResourceAllocationException{
    	
        Account ctxAccount = UserContext.current().getCaller();
        Long userId = UserContext.current().getCallerUserId();
        String name = cmd.getTemplateName();
        String displayText = cmd.getDisplayText(); 
        Integer bits = cmd.getBits();
        Boolean passwordEnabled = cmd.isPasswordEnabled();
        Boolean requiresHVM = cmd.getRequiresHvm();
        String url = cmd.getUrl();
        Boolean isPublic = cmd.isPublic();
        Boolean featured = cmd.isFeatured();
        Boolean isExtractable = cmd.isExtractable();
        String format = cmd.getFormat();
        Long guestOSId = cmd.getOsTypeId();
        Long zoneId = cmd.getZoneId();
        HypervisorType hypervisorType = HypervisorType.getType(cmd.getHypervisor());
        String accountName = cmd.getAccountName();
        Long domainId = cmd.getDomainId();
        Account resourceAccount = null;
        Long accountId = null;

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
        if(isExtractable == null){
        	isExtractable = Boolean.TRUE;
        }
        
        if (zoneId.longValue() == -1) {
        	zoneId = null;
        }
        
        if ( (accountName == null) ^ (domainId == null) ){// XOR - Both have to be passed or don't pass any of them 
        	throw new InvalidParameterValueException("Please specify both account and domainId or dont specify any of them");
        }
        
        // This complex logic is just for figuring out the template owning account because a user can register templates on other account's behalf.
        if ((ctxAccount == null) || isAdmin(ctxAccount.getType())) {
            if (domainId != null) {
                if ((ctxAccount != null) && !_domainDao.isChildDomain(ctxAccount.getDomainId(), domainId)) {
                    throw new PermissionDeniedException("Failed to register template, invalid domain id (" + domainId + ") given.");
                }
                if (accountName != null) {
                	resourceAccount = _accountDao.findActiveAccount(accountName, domainId);
                    if (resourceAccount == null) {
                        throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId);
                    }
                    accountId = resourceAccount.getId();
                }
            } else {
                accountId = ((ctxAccount != null) ? ctxAccount.getId() : null);
            }
        } else {
            accountId = ctxAccount.getId();
        }
        
        if (null == accountId && null == accountName && null == domainId && null == ctxAccount){
        	accountId = 1L;
        }
        if (null == accountId) {
            throw new InvalidParameterValueException("No valid account specified for registering template.");
        }
        
        boolean isAdmin = _accountDao.findById(accountId).getType() == Account.ACCOUNT_TYPE_ADMIN;
        
        if (!isAdmin && zoneId == null) {
        	throw new ServerApiException(BaseCmd.PARAM_ERROR, "Please specify a valid zone Id.");
        }
        
        if(url.toLowerCase().contains("file://")){
        	throw new ServerApiException(BaseCmd.PARAM_ERROR, "File:// type urls are currently unsupported");
        }
        
        if((!url.toLowerCase().endsWith("vhd"))&&(!url.toLowerCase().endsWith("vhd.zip"))
        	&&(!url.toLowerCase().endsWith("vhd.bz2"))&&(!url.toLowerCase().endsWith("vhd.gz")) 
        	&&(!url.toLowerCase().endsWith("qcow2"))&&(!url.toLowerCase().endsWith("qcow2.zip"))
        	&&(!url.toLowerCase().endsWith("qcow2.bz2"))&&(!url.toLowerCase().endsWith("qcow2.gz"))
        	&&(!url.toLowerCase().endsWith("ova"))&&(!url.toLowerCase().endsWith("ova.zip"))
        	&&(!url.toLowerCase().endsWith("ova.bz2"))&&(!url.toLowerCase().endsWith("ova.gz"))){
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
        
        return createTemplateOrIso(userId, accountId, zoneId, name, displayText, isPublic, featured, isExtractable, format, TemplateType.USER, url, null, requiresHVM, bits, passwordEnabled, guestOSId, true, hypervisorType);
    	
    }
    
    private VMTemplateVO createTemplateOrIso(long userId, Long accountId, Long zoneId, String name, String displayText, boolean isPublic, boolean featured, boolean isExtractable, String format, TemplateType diskType, String url, String chksum, boolean requiresHvm, int bits, boolean enablePassword, long guestOSId, boolean bootable, HypervisorType hypervisorType) throws IllegalArgumentException, ResourceAllocationException {
        try
        {
            if (name.length() > 32)
            {
                throw new InvalidParameterValueException("Template name should be less than 32 characters");
            }
        	
            ImageFormat imgfmt = ImageFormat.valueOf(format.toUpperCase());
            if (imgfmt == null) {
                throw new IllegalArgumentException("Image format is incorrect " + format + ". Supported formats are " + EnumUtils.listValues(ImageFormat.values()));
            }
            
//            FileSystem fileSystem = FileSystem.valueOf(diskType);
//            if (fileSystem == null) {
//                throw new IllegalArgumentException("File system is incorrect " + diskType + ". Supported file systems are " + EnumUtils.listValues(FileSystem.values()));
//            }
            
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
        	AccountVO account = _accountDao.findById(accountId);
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
            
            return create(userId, accountId, zoneId, name, displayText, isPublic, featured, isExtractable, imgfmt, diskType, uri, chksum, requiresHvm, bits, enablePassword, guestOSId, bootable, hypervisorType);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL " + url);
        }
    }

    private VMTemplateVO create(long userId, long accountId, Long zoneId, String name, String displayText, boolean isPublic, boolean featured, boolean isExtractable, ImageFormat format,  TemplateType type, URI url, String chksum, boolean requiresHvm, int bits, boolean enablePassword, long guestOSId, boolean bootable, HypervisorType hyperType) {
        Long id = _tmpltDao.getNextInSequence(Long.class, "id");
                
        AccountVO account = _accountDao.findById(accountId);
        if (account.getType() != Account.ACCOUNT_TYPE_ADMIN && zoneId == null) {
        	throw new IllegalArgumentException("Only admins can create templates in all zones");
        }
        
        VMTemplateVO template = new VMTemplateVO(id, name, format, isPublic, featured, isExtractable, type, url.toString(), requiresHvm, bits, accountId, chksum, displayText, enablePassword, guestOSId, bootable, hyperType);
        if (zoneId == null) {
            List<DataCenterVO> dcs = _dcDao.listAllIncludingRemoved();

        	for (DataCenterVO dc: dcs) {
    			_tmpltDao.addTemplateToZone(template, dc.getId());
    		}
        	template.setCrossZones(true);
        } else {
			_tmpltDao.addTemplateToZone(template, zoneId);
        }

        _downloadMonitor.downloadTemplateToStorage(id, zoneId);
        
        _accountMgr.incrementResourceCount(accountId, ResourceType.template);
        
        return template;
    }

    @Override
    public Long extract(ExtractIsoCmd cmd) {
        Account account = UserContext.current().getCaller();
        Long templateId = cmd.getId();
        Long zoneId = cmd.getZoneId();
        String url = cmd.getUrl();
        String mode = cmd.getMode();
        Long eventId = cmd.getStartEventId();
        
        // FIXME: async job needs fixing
        return extract(account, templateId, url, zoneId, mode, eventId, true, null, _asyncMgr);
    }

    @Override
    public Long extract(ExtractTemplateCmd cmd) {
        Account account = UserContext.current().getCaller();
        Long templateId = cmd.getId();
        Long zoneId = cmd.getZoneId();
        String url = cmd.getUrl();
        String mode = cmd.getMode();
        Long eventId = cmd.getStartEventId();

        // FIXME: async job needs fixing
        return extract(account, templateId, url, zoneId, mode, eventId, false, null, _asyncMgr);
    }

    private Long extract(Account account, Long templateId, String url, Long zoneId, String mode, Long eventId, boolean isISO, AsyncJobVO job, AsyncJobManager mgr) {
        String desc = "template";
        if (isISO) {
            desc = "ISO";
        }

        VMTemplateVO template = _tmpltDao.findById(templateId);
        if (template == null) {
            throw new InvalidParameterValueException("Unable to find " +desc+ " with id " + templateId);
        }
        if (template.getTemplateType() ==  Storage.TemplateType.SYSTEM){
            throw new InvalidParameterValueException("Unable to extract the " + desc + " " + template.getName() + " as it is a default System template");
        }
        if (isISO) {
            if (template.getFormat() != ImageFormat.ISO ){
                throw new InvalidParameterValueException("Unsupported format, could not extract the ISO");
            }
        } else {
            if (template.getFormat() == ImageFormat.ISO ){
                throw new InvalidParameterValueException("Unsupported format, could not extract the template");
            }
        }
        if(!template.isExtractable() && account!=null && account.getType() != Account.ACCOUNT_TYPE_ADMIN){ // Global admins are always allowed to extract
        	throw new PermissionDeniedException("The "+ desc + " is not allowed to be extracted" );
        }
        if (_dcDao.findById(zoneId) == null) {
            throw new IllegalArgumentException("Please specify a valid zone.");
        }

        if (account != null) {                  
            if(!isAdmin(account.getType())){
                if (template.getAccountId() != account.getId()){
                    throw new PermissionDeniedException("Unable to find " + desc + " with ID: " + templateId + " for account: " + account.getAccountName());
                }
            } else {
                Account userAccount = _accountDao.findById(template.getAccountId());
                if((userAccount == null) || !_domainDao.isChildDomain(account.getDomainId(), userAccount.getDomainId())) {
                    throw new PermissionDeniedException("Unable to extract " + desc + "=" + templateId + " - permission denied.");
                }
            }
        }

        HostVO secondaryStorageHost = _storageMgr.getSecondaryStorageHost(zoneId);
        VMTemplateHostVO tmpltHostRef = null;
        if (secondaryStorageHost != null) {
            tmpltHostRef = _tmpltHostDao.findByHostTemplate(secondaryStorageHost.getId(), templateId);
            if (tmpltHostRef != null && tmpltHostRef.getDownloadState() != com.cloud.storage.VMTemplateStorageResourceAssoc.Status.DOWNLOADED) {
                throw new InvalidParameterValueException("The " + desc + " has not been downloaded ");
            }
        }
        
        Upload.Mode extractMode;
        if( mode == null || (!mode.equals(Upload.Mode.FTP_UPLOAD.toString()) && !mode.equals(Upload.Mode.HTTP_DOWNLOAD.toString())) ){
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Please specify a valid extract Mode "+Upload.Mode.values());
        }else{
            extractMode = mode.equals(Upload.Mode.FTP_UPLOAD.toString()) ? Upload.Mode.FTP_UPLOAD : Upload.Mode.HTTP_DOWNLOAD;
        }
        
        long userId = UserContext.current().getCallerUserId();
        long accountId = template.getAccountId();
        String event = isISO ? EventTypes.EVENT_ISO_EXTRACT : EventTypes.EVENT_TEMPLATE_EXTRACT;
        if (extractMode == Upload.Mode.FTP_UPLOAD){
            URI uri = null;
            try {
                uri = new URI(url);
                if ((uri.getScheme() == null) || (!uri.getScheme().equalsIgnoreCase("ftp") )) {
                   throw new InvalidParameterValueException("Unsupported scheme for url: " + url);
                }
            } catch (Exception ex) {
                throw new InvalidParameterValueException("Invalid url given: " + url);
            }
    
            String host = uri.getHost();
            try {
                InetAddress hostAddr = InetAddress.getByName(host);
                if (hostAddr.isAnyLocalAddress() || hostAddr.isLinkLocalAddress() || hostAddr.isLoopbackAddress() || hostAddr.isMulticastAddress() ) {
                    throw new InvalidParameterValueException("Illegal host specified in url");
                }
                if (hostAddr instanceof Inet6Address) {
                    throw new InvalidParameterValueException("IPV6 addresses not supported (" + hostAddr.getHostAddress() + ")");
                }
            } catch (UnknownHostException uhe) {
                throw new InvalidParameterValueException("Unable to resolve " + host);
            }
                    
            if ( _uploadMonitor.isTypeUploadInProgress(templateId, isISO ? Type.ISO : Type.TEMPLATE) ){
                throw new IllegalArgumentException(template.getName() + " upload is in progress. Please wait for some time to schedule another upload for the same"); 
            }
        
            //long eventId = EventUtils.saveScheduledEvent(userId, accountId, event, "Extraction job");
      
    // FIXME:  scheduled event should've already been saved, we should be saving this started event here...
    //        String event = template.getFormat() == ImageFormat.ISO ? EventTypes.EVENT_ISO_UPLOAD : EventTypes.EVENT_TEMPLATE_UPLOAD;
    //        EventUtils.saveStartedEvent(template.getAccountId(), template.getAccountId(), event, "Starting upload of " +template.getName()+ " to " +url, cmd.getStartEventId());
           
            EventUtils.saveStartedEvent(userId, accountId, event, "Starting extraction of " +template.getName()+ " mode:" +extractMode.toString(), eventId);            
            return _uploadMonitor.extractTemplate(template, url, tmpltHostRef, zoneId, eventId, job.getId(), mgr);            
        }
        
        EventUtils.saveStartedEvent(userId, accountId, event, "Starting extraction of " +template.getName()+ " in mode:" +extractMode.toString(), eventId);
        UploadVO vo = _uploadMonitor.createEntityDownloadURL(template, tmpltHostRef, zoneId, eventId);
        if (vo!=null){                                  
            EventUtils.saveEvent(userId, accountId, EventVO.LEVEL_INFO, event, "Completed extraction of "+template.getName()+ " in mode:" +mode, null, eventId);
            return vo.getId();
        }else{
            EventUtils.saveEvent(userId, accountId, EventVO.LEVEL_ERROR, event, "Failed extraction of "+template.getName()+ " in mode:" +mode, null, eventId);
            return null;
        }
    }    
    
    @Override @DB
    public VMTemplateStoragePoolVO prepareTemplateForCreate(VMTemplateVO template, StoragePool pool) {
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
        
        templateStoragePoolRef = _tmpltPoolDao.acquireInLockTable(templateStoragePoolRefId, 1200);
        if (templateStoragePoolRef == null) {
            throw new CloudRuntimeException("Unable to acquire lock on VMTemplateStoragePool: " + templateStoragePoolRefId);
        }

        try {
            if (templateStoragePoolRef.getDownloadState() == Status.DOWNLOADED) {
                return templateStoragePoolRef;
            }
            String url = origUrl + "/" + templateHostRef.getInstallPath();
            PrimaryStorageDownloadCommand dcmd = new PrimaryStorageDownloadCommand(template.getUniqueName(), url, template.getFormat(), 
            	template.getAccountId(), pool.getId(), pool.getUuid());
            HostVO secondaryStorageHost = _hostDao.findSecondaryStorageHost(pool.getDataCenterId());
            assert(secondaryStorageHost != null);
            dcmd.setSecondaryStorageUrl(secondaryStorageHost.getStorageUrl());
            // TODO temporary hacking, hard-coded to NFS primary data store
            dcmd.setPrimaryStorageUrl("nfs://" + pool.getHostAddress() + pool.getPath());
            
            for (StoragePoolHostVO vo : vos) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Downloading " + templateId + " via " + vo.getHostId());
                }
            	dcmd.setLocalPath(vo.getLocalPath());
            	// set 120 min timeout for this command
            	PrimaryStorageDownloadAnswer answer = (PrimaryStorageDownloadAnswer)_agentMgr.easySend(vo.getHostId(), dcmd, 120*60*1000);
                if (answer != null && answer.getResult() ) {
            		templateStoragePoolRef.setDownloadPercent(100);
            		templateStoragePoolRef.setDownloadState(Status.DOWNLOADED);
            		templateStoragePoolRef.setLocalDownloadPath(answer.getInstallPath());
            		templateStoragePoolRef.setInstallPath(answer.getInstallPath());
            		templateStoragePoolRef.setTemplateSize(answer.getTemplateSize());
            		_tmpltPoolDao.update(templateStoragePoolRef.getId(), templateStoragePoolRef);
            		if (s_logger.isDebugEnabled()) {
            			s_logger.debug("Template " + templateId + " is downloaded via " + vo.getHostId());
            		}
            		return templateStoragePoolRef;
                } else {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Template " + templateId + " download to pool " + vo.getPoolId() + " failed due to " + (answer!=null?answer.getDetails():"return null"));                }
                }
            }
        } finally {
            _tmpltPoolDao.releaseFromLockTable(templateStoragePoolRefId);
        }
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Template " + templateId + " is not found on and can not be downloaded to pool " + poolId);
        }
        return null;
    }
    
    @Override
    @DB
    public boolean copy(long userId, long templateId, long sourceZoneId, long destZoneId) throws StorageUnavailableException {
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
    		throw new StorageUnavailableException("Source zone is not ready", DataCenter.class, sourceZoneId);
    	}
    	if (dstSecHost == null) {
    		throw new StorageUnavailableException("Destination zone is not ready", DataCenter.class, destZoneId);
    	}
    	
    	VMTemplateVO vmTemplate = _tmpltDao.findById(templateId);
    	VMTemplateHostVO srcTmpltHost = null;
        srcTmpltHost = _tmpltHostDao.findByHostTemplate(srcSecHost.getId(), templateId);
        if (srcTmpltHost == null || srcTmpltHost.getDestroyed() || srcTmpltHost.getDownloadState() != VMTemplateStorageResourceAssoc.Status.DOWNLOADED) {
	      	throw new InvalidParameterValueException("Please specify a template that is installed on secondary storage host: " + srcSecHost.getName());
	      }
        
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
        		dstTmpltHost = _tmpltHostDao.lockRow(dstTmpltHost.getId(), true);
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
    	
        UsageEventVO usageEvent = new UsageEventVO(copyEventType, account.getId(), destZoneId, templateId, null, null, null, srcTmpltHost.getSize());
        _usageEventDao.persist(usageEvent);
        saveEvent(userId, account.getId(), account.getDomainId(), copyEventType, copyEventDescription, EventVO.LEVEL_INFO, params);
    	return true;
    }
      
    @Override
    public VirtualMachineTemplate copyIso(CopyIsoCmd cmd) throws StorageUnavailableException {
    	Long isoId = cmd.getId();
    	Long userId = UserContext.current().getCallerUserId();
    	Long sourceZoneId = cmd.getSourceZoneId();
    	Long destZoneId = cmd.getDestinationZoneId();
    	Account account = UserContext.current().getCaller();
    	
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
        
        boolean success = copy(userId, isoId, sourceZoneId, destZoneId);
        
        VMTemplateVO copiedIso = null;
        if (success) {
            copiedIso = _tmpltDao.findById(isoId);
        }
       
        return copiedIso;
    }
    
    
    @Override
    public VirtualMachineTemplate copyTemplate(CopyTemplateCmd cmd) throws StorageUnavailableException {
    	Long templateId = cmd.getId();
    	Long userId = UserContext.current().getCallerUserId();
    	Long sourceZoneId = cmd.getSourceZoneId();
    	Long destZoneId = cmd.getDestinationZoneId();
    	Account account = UserContext.current().getCaller();
        
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
        
        boolean success = copy(userId, templateId, sourceZoneId, destZoneId);
        
        VMTemplateVO copiedTemplate = null;
        if (success) {
            copiedTemplate = _tmpltDao.findById(templateId);
        }
       
        return copiedTemplate;
    }

    @Override @DB
    public boolean delete(long userId, long templateId, Long zoneId) {
    	boolean success = true;
    	VMTemplateVO template = _tmpltDao.findById(templateId);
    	if (template == null || template.getRemoved() != null) {
    		throw new InvalidParameterValueException("Please specify a valid template.");
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
					throw new CloudRuntimeException(errorMsg);
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
				VMTemplateHostVO lock = _tmpltHostDao.acquireInLockTable(templateHostVO.getId());
				
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
					UsageEventVO usageEvent = new UsageEventVO(eventType, account.getId(), sZoneId, templateId, null, null, null, null);
					_usageEventDao.persist(usageEvent);
				} finally {
					if (lock != null) {
						_tmpltHostDao.releaseFromLockTable(lock.getId());
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
			
			VMTemplateVO lock = _tmpltDao.acquireInLockTable(templateId);

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
					_tmpltDao.releaseFromLockTable(lock.getId());
				}
			}
			
			s_logger.debug("Removed template: " + template.getName() + " because all of its template host refs were marked as destroyed.");
		}
		
		return success;
    }
    
    @Override
    public List<VMTemplateStoragePoolVO> getUnusedTemplatesInPool(StoragePoolVO pool) {
		List<VMTemplateStoragePoolVO> unusedTemplatesInPool = new ArrayList<VMTemplateStoragePoolVO>();
		List<VMTemplateStoragePoolVO> allTemplatesInPool = _tmpltPoolDao.listByPoolId(pool.getId());
		
		for (VMTemplateStoragePoolVO templatePoolVO : allTemplatesInPool) {
			VMTemplateVO template = _tmpltDao.findByIdIncludingRemoved(templatePoolVO.getTemplateId());			
		
			// If this is a routing template, consider it in use
			if (template.getTemplateType() == TemplateType.SYSTEM) {
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
    
    @Override
    public void evictTemplateFromStoragePool(VMTemplateStoragePoolVO templatePoolVO) {
		StoragePoolVO pool = _poolDao.findById(templatePoolVO.getPoolId());
		VMTemplateVO template = _tmpltDao.findByIdIncludingRemoved(templatePoolVO.getTemplateId());
		
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
        
        HostTemplateStatesSearch.join("host", HostSearch, HostSearch.entity().getId(), HostTemplateStatesSearch.entity().getHostId(), JoinBuilder.JoinType.INNER);
        HostSearch.done();
        HostTemplateStatesSearch.done();
        
        return false;
    }
    
    protected TemplateManagerImpl() {
    }

	@Override
	public Long createInZone(long zoneId, long userId, String displayText,
			boolean isPublic, boolean featured, boolean isExtractable, ImageFormat format,
			TemplateType type, URI url, String chksum, boolean requiresHvm,
			int bits, boolean enablePassword, long guestOSId, boolean bootable) {
		Long id = _tmpltDao.getNextInSequence(Long.class, "id");

		UserVO user = _userDao.findById(userId);
		long accountId = user.getAccountId();

		VMTemplateVO template = new VMTemplateVO(id, displayText, format, isPublic, featured, isExtractable, type, url.toString(), requiresHvm, bits, accountId, chksum, displayText, enablePassword, guestOSId, bootable, null);

		Long templateId = _tmpltDao.addTemplateToZone(template, zoneId);
		UserAccount userAccount = _userAccountDao.findById(userId);
		saveEvent(userId, userAccount.getAccountId(), userAccount.getDomainId(), EventTypes.EVENT_TEMPLATE_DOWNLOAD_START,
				"Started download of template:  " + template.getName(), null, null);

		_downloadMonitor.downloadTemplateToStorage(id, zoneId);

		_accountMgr.incrementResourceCount(userAccount.getAccountId(), ResourceType.template);

		return templateId;
	}
	
	@Override
    public boolean templateIsDeleteable(VMTemplateHostVO templateHostRef) {
		VMTemplateVO template = _tmpltDao.findByIdIncludingRemoved(templateHostRef.getTemplateId());
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
	public boolean detachIso(DetachIsoCmd cmd)  {
        Account account = UserContext.current().getCaller();
        Long userId = UserContext.current().getCallerUserId();
        Long vmId = cmd.getVirtualMachineId();
        
        // Verify input parameters
        UserVmVO vmInstanceCheck = _userVmDao.findById(vmId.longValue());
        if (vmInstanceCheck == null) {
            throw new ServerApiException (BaseCmd.PARAM_ERROR, "Unable to find a virtual machine with id " + vmId);
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
	public boolean attachIso(AttachIsoCmd cmd) {
        Account account = UserContext.current().getCaller();
        Long userId = UserContext.current().getCallerUserId();
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
        
        VMInstanceVO vm = ApiDBUtils.findVMInstanceById(vmId);
        VMTemplateVO vmTemplate = ApiDBUtils.findTemplateById(vm.getTemplateId());
        if ("xen-pv-drv-iso".equals(iso.getDisplayText()) && vmTemplate.getHypervisorType() != Hypervisor.HypervisorType.XenServer){
        	throw new InvalidParameterValueException("Cannot attach Xenserver PV drivers to incompatible hypervisor " +vmTemplate.getHypervisorType());
        }
        
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
        
        if (attach) {
            vm.setIsoId(iso.getId());
        } else {
            vm.setIsoId(null);
        }
        _userVmDao.update(vmId, vm);
        
        if (success) {
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
				if ((vmInstanceCheck != null) && (account.getId() != vmInstanceCheck.getAccountId())) {
		            throw new PermissionDeniedException(msg + ". Permission denied.");
		        }

	    		if ((template != null) && (!template.isPublicTemplate() && (account.getId() != template.getAccountId()) && (template.getTemplateType() != TemplateType.PERHOST))) {
                    throw new PermissionDeniedException(msg + ". Permission denied.");
                }
                
    	    } else {
    	        if ((vmInstanceCheck != null) && !_domainDao.isChildDomain(account.getDomainId(), vmInstanceCheck.getDomainId())) {
                    throw new PermissionDeniedException(msg + ". Permission denied.");
    	        }
    	        // FIXME:  if template/ISO owner is null we probably need to throw some kind of exception
    	        
    	        if (template != null) {
    	        	Account templateOwner = _accountDao.findById(template.getAccountId());
	    	        if ((templateOwner != null) && !_domainDao.isChildDomain(account.getDomainId(), templateOwner.getDomainId())) {
	                    throw new PermissionDeniedException(msg + ". Permission denied.");
	    	        }
    	        }
    	    }
    	}
        // If command is executed via 8096 port, set userId to the id of System account (1)
        if (userId == null) {
            userId = new Long(1);
        }
        
        return userId;
	}
	
	private static boolean isAdmin(short accountType) {
	    return ((accountType == Account.ACCOUNT_TYPE_ADMIN) ||
	            (accountType == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) ||
	            (accountType == Account.ACCOUNT_TYPE_READ_ONLY_ADMIN));
	}
	
	@Override
    public boolean deleteTemplate(DeleteTemplateCmd cmd) {
        Long templateId = cmd.getId();
        Long userId = UserContext.current().getCallerUserId();
        Account account = UserContext.current().getCaller();
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
    	
    	if (template.getTemplateType() == TemplateType.SYSTEM) {
    		throw new InvalidParameterValueException("The DomR template cannot be deleted.");
    	}
    	
    	if (zoneId != null && (_hostDao.findSecondaryStorageHost(zoneId) == null)) {
    		throw new InvalidParameterValueException("Failed to find a secondary storage host in the specified zone.");
    	}
    	return delete(userId, templateId, zoneId);
	}
	
	@Override
    public boolean deleteIso(DeleteIsoCmd cmd) {
        Long templateId = cmd.getId();
        Long userId = UserContext.current().getCallerUserId();
        Account account = UserContext.current().getCaller();
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
