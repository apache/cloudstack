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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.acl.SecurityChecker.AccessType;
import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.DestroyCommand;
import com.cloud.agent.api.storage.PrimaryStorageDownloadAnswer;
import com.cloud.agent.api.storage.PrimaryStorageDownloadCommand;
import com.cloud.api.commands.AttachIsoCmd;
import com.cloud.api.commands.CopyTemplateCmd;
import com.cloud.api.commands.DeleteIsoCmd;
import com.cloud.api.commands.DeleteTemplateCmd;
import com.cloud.api.commands.DetachIsoCmd;
import com.cloud.api.commands.ExtractIsoCmd;
import com.cloud.api.commands.ExtractTemplateCmd;
import com.cloud.api.commands.PrepareTemplateCmd;
import com.cloud.api.commands.RegisterIsoCmd;
import com.cloud.api.commands.RegisterTemplateCmd;
import com.cloud.async.AsyncJobManager;
import com.cloud.async.AsyncJobVO;
import com.cloud.configuration.Config;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
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
import com.cloud.hypervisor.HypervisorGuruManager;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.StoragePoolStatus;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.Upload;
import com.cloud.storage.Upload.Type;
import com.cloud.storage.UploadVO;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.VMTemplateVO;
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
import com.cloud.storage.secondary.SecondaryStorageVmManager;
import com.cloud.storage.upload.UploadMonitor;
import com.cloud.template.TemplateAdapter.TemplateAdapterType;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountService;
import com.cloud.user.AccountVO;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.UserContext;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserAccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.Adapters;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.component.Manager;
import com.cloud.utils.concurrency.NamedThreadFactory;
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
    @Inject UsageEventDao _usageEventDao;
    @Inject HypervisorGuruManager _hvGuruMgr;
    @Inject AccountService _accountService;
    @Inject ResourceLimitService _resourceLimitMgr;
    @Inject SecondaryStorageVmManager _ssvmMgr;
    int _primaryStorageDownloadWait;
    protected SearchBuilder<VMTemplateHostVO> HostTemplateStatesSearch;
    
    int _storagePoolMaxWaitSeconds = 3600;
    ExecutorService _preloadExecutor;
    
    @Inject (adapter=TemplateAdapter.class)
    protected Adapters<TemplateAdapter> _adapters;
    
    private TemplateAdapter getAdapter(HypervisorType type) {
    	TemplateAdapter adapter = null;
    	if (type == HypervisorType.BareMetal) {
    		adapter = _adapters.get(TemplateAdapterType.BareMetal.getName());
    	} else {
    		// see HyervisorTemplateAdapter
    		adapter =  _adapters.get(TemplateAdapterType.Hypervisor.getName());
    	}
    	
    	if (adapter == null) {
    		throw new CloudRuntimeException("Cannot find template adapter for " + type.toString());
    	}
    	
    	return adapter;
    }
    
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ISO_CREATE, eventDescription = "creating iso")
    public VirtualMachineTemplate registerIso(RegisterIsoCmd cmd) throws ResourceAllocationException{
    	TemplateAdapter adapter = getAdapter(HypervisorType.None);
    	TemplateProfile profile = adapter.prepare(cmd);
    	return adapter.create(profile);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_TEMPLATE_CREATE, eventDescription = "creating template")
    public VirtualMachineTemplate registerTemplate(RegisterTemplateCmd cmd) throws URISyntaxException, ResourceAllocationException{
        if(cmd.getTemplateTag() != null){
            Account account = UserContext.current().getCaller();
            if(!_accountService.isRootAdmin(account.getType())){
                throw new PermissionDeniedException("Parameter templatetag can only be specified by a Root Admin, permission denied");
            }
        }        
    	TemplateAdapter adapter = getAdapter(HypervisorType.getType(cmd.getHypervisor()));
    	TemplateProfile profile = adapter.prepare(cmd);
    	return adapter.create(profile);
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
        Account caller = UserContext.current().getCaller();
        Long templateId = cmd.getId();
        Long zoneId = cmd.getZoneId();
        String url = cmd.getUrl();
        String mode = cmd.getMode();
        Long eventId = cmd.getStartEventId();

        // FIXME: async job needs fixing
        return extract(caller, templateId, url, zoneId, mode, eventId, false, null, _asyncMgr);
    }
    
    @Override
    public VirtualMachineTemplate prepareTemplate(PrepareTemplateCmd cmd) {
    	
    	VMTemplateVO vmTemplate = _tmpltDao.findById(cmd.getTemplateId());
    	if(vmTemplate == null)
    		throw new InvalidParameterValueException("Unable to find template " + cmd.getTemplateId());
    	
    	prepareTemplateInAllStoragePools(vmTemplate, cmd.getZoneId());
    	return vmTemplate;
    }

    private Long extract(Account caller, Long templateId, String url, Long zoneId, String mode, Long eventId, boolean isISO, AsyncJobVO job, AsyncJobManager mgr) {
        String desc = Upload.Type.TEMPLATE.toString();
        if (isISO) {
            desc = Upload.Type.ISO.toString();
        }
        eventId = eventId == null ? 0:eventId;
        
        VMTemplateVO template = _tmpltDao.findById(templateId);
        if (template == null || template.getRemoved() != null) {
            throw new InvalidParameterValueException("Unable to find " +desc+ " with id " + templateId);
        }
        
        if (template.getTemplateType() ==  Storage.TemplateType.SYSTEM){
            throw new InvalidParameterValueException("Unable to extract the " + desc + " " + template.getName() + " as it is a default System template");
        } else if (template.getTemplateType() ==  Storage.TemplateType.PERHOST){
            throw new InvalidParameterValueException("Unable to extract the " + desc + " " + template.getName() + " as it resides on host and not on SSVM");
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
        
        if (_dcDao.findById(zoneId) == null) {
            throw new IllegalArgumentException("Please specify a valid zone.");
        }
        
        if (!_accountMgr.isRootAdmin(caller.getType()) && !template.isExtractable()) {
            throw new InvalidParameterValueException("Unable to extract template id=" + templateId + " as it's not extractable");
        }
        
        _accountMgr.checkAccess(caller, AccessType.ModifyEntry, template);
        
        List<HostVO> sservers = _storageMgr.getSecondaryStorageHosts(zoneId);

        VMTemplateHostVO tmpltHostRef = null;
        if (sservers != null) {
            for(HostVO secondaryStorageHost: sservers){
                tmpltHostRef = _tmpltHostDao.findByHostTemplate(secondaryStorageHost.getId(), templateId);
                if (tmpltHostRef != null){
                    if (tmpltHostRef.getDownloadState() != com.cloud.storage.VMTemplateStorageResourceAssoc.Status.DOWNLOADED) {
                        tmpltHostRef = null;
                    }
                    else {
                        break;
                    }
                }
            }
        }
        
        if (tmpltHostRef == null ) {
            throw new InvalidParameterValueException("The " + desc + " has not been downloaded ");
        }
        
        Upload.Mode extractMode;
        if (mode == null || (!mode.equalsIgnoreCase(Upload.Mode.FTP_UPLOAD.toString()) && !mode.equalsIgnoreCase(Upload.Mode.HTTP_DOWNLOAD.toString())) ){
            throw new InvalidParameterValueException("Please specify a valid extract Mode. Supported modes: "+ Upload.Mode.FTP_UPLOAD + ", " + Upload.Mode.HTTP_DOWNLOAD);
        } else {
            extractMode = mode.equalsIgnoreCase(Upload.Mode.FTP_UPLOAD.toString()) ? Upload.Mode.FTP_UPLOAD : Upload.Mode.HTTP_DOWNLOAD;
        }
        
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
                    
            if (_uploadMonitor.isTypeUploadInProgress(templateId, isISO ? Type.ISO : Type.TEMPLATE) ){
                throw new IllegalArgumentException(template.getName() + " upload is in progress. Please wait for some time to schedule another upload for the same"); 
            }
        
            return _uploadMonitor.extractTemplate(template, url, tmpltHostRef, zoneId, eventId, job.getId(), mgr);            
        }
        
        UploadVO vo = _uploadMonitor.createEntityDownloadURL(template, tmpltHostRef, zoneId, eventId);
        if (vo != null){                                  
            return vo.getId();
        }else{
            return null;
        }
    }
    
    public void prepareTemplateInAllStoragePools(final VMTemplateVO template, long zoneId) {
    	List<StoragePoolVO> pools = _poolDao.listByStatus(StoragePoolStatus.Up);
    	for(final StoragePoolVO pool : pools) {
    		if(pool.getDataCenterId() == zoneId) {
    			s_logger.info("Schedule to preload template " + template.getId() + " into primary storage " + pool.getId());
	    		this._preloadExecutor.execute(new Runnable() {
	    			public void run() {
	    				try {
	    					reallyRun();
	    				} catch(Throwable e) {
	    					s_logger.warn("Unexpected exception ", e);
	    				}
	    			}
	    			
	    			private void reallyRun() {
	        			s_logger.info("Start to preload template " + template.getId() + " into primary storage " + pool.getId());
	    				prepareTemplateForCreate(template, pool);
	        			s_logger.info("End of preloading template " + template.getId() + " into primary storage " + pool.getId());
	    			}
	    		});
    		} else {
    			s_logger.info("Skip loading template " + template.getId() + " into primary storage " + pool.getId() + " as pool zone " + pool.getDataCenterId() + " is ");
    		}
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
        
        templateHostRef = _storageMgr.findVmTemplateHost(templateId, pool);
        
        if (templateHostRef == null) {
            s_logger.debug("Unable to find a secondary storage host who has completely downloaded the template.");
            return null;
        }
        
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
        
        List<StoragePoolHostVO> vos = _poolHostDao.listByHostStatus(poolId, com.cloud.host.Status.Up);
        
        templateStoragePoolRef = _tmpltPoolDao.acquireInLockTable(templateStoragePoolRefId, _storagePoolMaxWaitSeconds);
        if (templateStoragePoolRef == null) {
            throw new CloudRuntimeException("Unable to acquire lock on VMTemplateStoragePool: " + templateStoragePoolRefId);
        }

        try {
            if (templateStoragePoolRef.getDownloadState() == Status.DOWNLOADED) {
                return templateStoragePoolRef;
            }
            String url = origUrl + "/" + templateHostRef.getInstallPath();
            PrimaryStorageDownloadCommand dcmd = new PrimaryStorageDownloadCommand(template.getUniqueName(), url, template.getFormat(), 
                   template.getAccountId(), pool.getId(), pool.getUuid(), _primaryStorageDownloadWait);
            HostVO secondaryStorageHost = _hostDao.findById(templateHostRef.getHostId());
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
            	
            	PrimaryStorageDownloadAnswer answer = (PrimaryStorageDownloadAnswer)_agentMgr.easySend(
                       _hvGuruMgr.getGuruProcessedCommandTargetHost(vo.getHostId(), dcmd), dcmd);
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
    public boolean resetTemplateDownloadStateOnPool(long templateStoragePoolRefId) {
    	// have to use the same lock that prepareTemplateForCreate use to maintain state consistency
    	VMTemplateStoragePoolVO templateStoragePoolRef = _tmpltPoolDao.acquireInLockTable(templateStoragePoolRefId, 1200);
    	
        if (templateStoragePoolRef == null) {
        	s_logger.warn("resetTemplateDownloadStateOnPool failed - unable to lock TemplateStorgePoolRef " + templateStoragePoolRefId);
            return false;
        }
        
        try {
        	templateStoragePoolRef.setDownloadState(VMTemplateStorageResourceAssoc.Status.NOT_DOWNLOADED);
        	_tmpltPoolDao.update(templateStoragePoolRefId, templateStoragePoolRef);
        } finally {
            _tmpltPoolDao.releaseFromLockTable(templateStoragePoolRefId);
        }
        
        return true;
    }
    
    @Override
    @DB
    public boolean copy(long userId, VMTemplateVO template, HostVO srcSecHost, DataCenterVO srcZone, DataCenterVO dstZone) throws StorageUnavailableException, ResourceAllocationException {
    	List<HostVO> dstSecHosts = _ssvmMgr.listSecondaryStorageHostsInOneZone(dstZone.getId());
    	long tmpltId = template.getId();
        long dstZoneId = dstZone.getId();
    	if (dstSecHosts == null || dstSecHosts.isEmpty() ) {
    		throw new StorageUnavailableException("Destination zone is not ready", DataCenter.class, dstZone.getId());
    	}
        AccountVO account = _accountDao.findById(template.getAccountId());
        if (_resourceLimitMgr.resourceLimitExceeded(account, ResourceType.template)) {
        	ResourceAllocationException rae = new ResourceAllocationException("Maximum number of templates and ISOs for account: " + account.getAccountName() + " has been exceeded.");
        	rae.setResourceType("template");
        	throw rae;
        }
               
        // Event details        
        String copyEventType;
        String createEventType;
        if (template.getFormat().equals(ImageFormat.ISO)){
            copyEventType = EventTypes.EVENT_ISO_COPY;
            createEventType = EventTypes.EVENT_ISO_CREATE;
        } else {
            copyEventType = EventTypes.EVENT_TEMPLATE_COPY;
            createEventType = EventTypes.EVENT_TEMPLATE_CREATE;
        }
        
        
        Transaction txn = Transaction.currentTxn();
        txn.start();
        
        VMTemplateHostVO srcTmpltHost = _tmpltHostDao.findByHostTemplate(srcSecHost.getId(), tmpltId);
        for ( HostVO dstSecHost : dstSecHosts ) {
            VMTemplateHostVO dstTmpltHost = null;
            try {
            	dstTmpltHost = _tmpltHostDao.findByHostTemplate(dstSecHost.getId(), tmpltId, true);
            	if (dstTmpltHost != null) {
            		dstTmpltHost = _tmpltHostDao.lockRow(dstTmpltHost.getId(), true);
            		if (dstTmpltHost != null && dstTmpltHost.getDownloadState() == Status.DOWNLOADED) {
            			if (dstTmpltHost.getDestroyed() == false)  {
            				return true;
            			} else {
            				dstTmpltHost.setDestroyed(false);
            				_tmpltHostDao.update(dstTmpltHost.getId(), dstTmpltHost);
            				
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
            
            if(_downloadMonitor.copyTemplate(template, srcSecHost, dstSecHost) ) {
                _tmpltDao.addTemplateToZone(template, dstZoneId);
            	
            	if(account.getId() != Account.ACCOUNT_ID_SYSTEM){
            	    UsageEventVO usageEvent = new UsageEventVO(copyEventType, account.getId(), dstZoneId, tmpltId, null, null, null, srcTmpltHost.getSize());
            	    _usageEventDao.persist(usageEvent);
            	}
            	return true;
            }
        }
        return false;
    }
  
    
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_TEMPLATE_COPY, eventDescription = "copying template", async = true)
    public VirtualMachineTemplate copyTemplate(CopyTemplateCmd cmd) throws StorageUnavailableException, ResourceAllocationException {
    	Long templateId = cmd.getId();
    	Long userId = UserContext.current().getCallerUserId();
    	Long sourceZoneId = cmd.getSourceZoneId();
    	Long destZoneId = cmd.getDestinationZoneId();
    	Account caller = UserContext.current().getCaller();
        
        //Verify parameters
        if (sourceZoneId == destZoneId) {
            throw new InvalidParameterValueException("Please specify different source and destination zones.");
        }
        
        DataCenterVO sourceZone = _dcDao.findById(sourceZoneId);
        if (sourceZone == null) {
            throw new InvalidParameterValueException("Please specify a valid source zone.");
        }
        
        DataCenterVO dstZone = _dcDao.findById(destZoneId);
        if (dstZone == null) {
            throw new InvalidParameterValueException("Please specify a valid destination zone.");
        }
    	
        VMTemplateVO template = _tmpltDao.findById(templateId);
        if (template == null || template.getRemoved() != null) {
            throw new InvalidParameterValueException("Unable to find template with id");
        }
      
        HostVO dstSecHost = _storageMgr.getSecondaryStorageHost(destZoneId, templateId);
        if ( dstSecHost != null ) {
            s_logger.debug("There is template " + templateId + " in secondary storage " + dstSecHost.getId() + " in zone " + destZoneId + " , don't need to copy");
            return template;
        }
        
        HostVO srcSecHost = _storageMgr.getSecondaryStorageHost(sourceZoneId, templateId);
        if ( srcSecHost == null ) {
            throw new InvalidParameterValueException("There is no template " + templateId + " in zone " + sourceZoneId );
        }
       
        _accountMgr.checkAccess(caller, AccessType.ModifyEntry, template);
        
        boolean success = copy(userId, template, srcSecHost, sourceZone, dstZone);
        
        if (success) {
            return template;
        }
       
        return null;
    }

    @Override
    public boolean delete(long userId, long templateId, Long zoneId) {
    	VMTemplateVO template = _tmpltDao.findById(templateId);
    	if (template == null || template.getRemoved() != null) {
    		throw new InvalidParameterValueException("Please specify a valid template.");
    	}
    	
    	TemplateAdapter adapter = getAdapter(template.getHypervisorType());
    	return adapter.delete(new TemplateProfile(userId, template, zoneId));
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
		
		if (s_logger.isDebugEnabled()) {
		    s_logger.debug("Evicting " + templatePoolVO);
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

        String value = configDao.getValue(Config.PrimaryStorageDownloadWait.toString());
        _primaryStorageDownloadWait = NumbersUtil.parseInt(value, Integer.parseInt(Config.PrimaryStorageDownloadWait.getDefaultValue()));

        HostTemplateStatesSearch = _tmpltHostDao.createSearchBuilder();
        HostTemplateStatesSearch.and("id", HostTemplateStatesSearch.entity().getTemplateId(), SearchCriteria.Op.EQ);
        HostTemplateStatesSearch.and("state", HostTemplateStatesSearch.entity().getDownloadState(), SearchCriteria.Op.EQ);
        
        SearchBuilder<HostVO> HostSearch = _hostDao.createSearchBuilder();
        HostSearch.and("dcId", HostSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        
        HostTemplateStatesSearch.join("host", HostSearch, HostSearch.entity().getId(), HostTemplateStatesSearch.entity().getHostId(), JoinBuilder.JoinType.INNER);
        HostSearch.done();
        HostTemplateStatesSearch.done();
        
        _storagePoolMaxWaitSeconds = NumbersUtil.parseInt(configDao.getValue(Config.StoragePoolMaxWaitSeconds.key()), 3600);
        _preloadExecutor = Executors.newFixedThreadPool(8, new NamedThreadFactory("Template-Preloader"));
        return false;
    }
    
    protected TemplateManagerImpl() {
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
			List<SnapshotVO> snapshots = _snapshotDao.listByVolumeIdVersion(volume.getId(), "2.1");
			if (!snapshots.isEmpty()) {
				s_logger.debug("Template " + template.getName() + " in zone " + zone.getName() + " is not deleteable because there are 2.1 snapshots using this template.");
				return false;
			}
		}
		
		return true;
	}

	@Override
    @ActionEvent(eventType = EventTypes.EVENT_ISO_DETACH, eventDescription = "detaching ISO", async = true)
	public boolean detachIso(DetachIsoCmd cmd)  {
        Account caller = UserContext.current().getCaller();
        Long userId = UserContext.current().getCallerUserId();
        Long vmId = cmd.getVirtualMachineId();
        
        // Verify input parameters
        UserVmVO vmInstanceCheck = _userVmDao.findById(vmId.longValue());
        if (vmInstanceCheck == null) {
            throw new InvalidParameterValueException ("Unable to find a virtual machine with id " + vmId);
        }
        
        UserVm userVM = _userVmDao.findById(vmId);
        if (userVM == null) {
            throw new InvalidParameterValueException("Please specify a valid VM.");
        }
        
        _accountMgr.checkAccess(caller, null, userVM);

        Long isoId = userVM.getIsoId();
        if (isoId == null) {
            throw new InvalidParameterValueException("The specified VM has no ISO attached to it.");
        }
    	UserContext.current().setEventDetails("Vm Id: " +vmId+ " ISO Id: "+isoId);
        
        State vmState = userVM.getState();
        if (vmState != State.Running && vmState != State.Stopped) {
        	throw new InvalidParameterValueException("Please specify a VM that is either Stopped or Running.");
        }

        return attachISOToVM(vmId, userId, isoId, false); //attach=false => detach
	}
	
	@Override
    @ActionEvent(eventType = EventTypes.EVENT_ISO_ATTACH, eventDescription = "attaching ISO", async = true)
	public boolean attachIso(AttachIsoCmd cmd) {
        Account caller = UserContext.current().getCaller();
        Long userId = UserContext.current().getCallerUserId();
        Long vmId = cmd.getVirtualMachineId();
        Long isoId = cmd.getId();
        
    	// Verify input parameters
    	UserVmVO vm = _userVmDao.findById(vmId);
    	if (vm == null) {
            throw new InvalidParameterValueException("Unable to find a virtual machine with id " + vmId);
        }
    	
    	VMTemplateVO iso = _tmpltDao.findById(isoId);
    	if (iso == null || iso.getRemoved() != null) {
            throw new InvalidParameterValueException("Unable to find an ISO with id " + isoId);
    	}
    	
    	//check permissions
    	_accountMgr.checkAccess(caller, null, iso, vm);
    	
        State vmState = vm.getState();
        if (vmState != State.Running && vmState != State.Stopped) {
        	throw new InvalidParameterValueException("Please specify a VM that is either Stopped or Running.");
        }
        
        if ("xen-pv-drv-iso".equals(iso.getDisplayText()) && vm.getHypervisorType() != Hypervisor.HypervisorType.XenServer){
        	throw new InvalidParameterValueException("Cannot attach Xenserver PV drivers to incompatible hypervisor " + vm.getHypervisorType());
        }
        
        if("vmware-tools.iso".equals(iso.getName()) && vm.getHypervisorType() != Hypervisor.HypervisorType.VMware) {
        	throw new InvalidParameterValueException("Cannot attach VMware tools drivers to incompatible hypervisor " + vm.getHypervisorType());
        }
        return attachISOToVM(vmId, userId, isoId, true);
	}

    private boolean attachISOToVM(long vmId, long userId, long isoId, boolean attach) {
    	UserVmVO vm = _userVmDao.findById(vmId);
    	VMTemplateVO iso = _tmpltDao.findById(isoId);

        boolean success = _vmMgr.attachISOToVM(vmId, isoId, attach);
        if ( success && attach) {
             vm.setIsoId(iso.getId());
            _userVmDao.update(vmId, vm);
        } 
        if ( success && !attach ) {
            vm.setIsoId(null);
            _userVmDao.update(vmId, vm);
        }    
        return success;
    }
	
	@Override
    @ActionEvent(eventType = EventTypes.EVENT_TEMPLATE_DELETE, eventDescription = "deleting template", async = true)
    public boolean deleteTemplate(DeleteTemplateCmd cmd) {
        Long templateId = cmd.getId();
        Account caller = UserContext.current().getCaller();
        
        VirtualMachineTemplate template = getTemplate(templateId);
        if (template == null) {
            throw new InvalidParameterValueException("unable to find template with id " + templateId);
        }
        
        _accountMgr.checkAccess(caller, AccessType.ModifyEntry, template);
    	
    	if (template.getFormat() == ImageFormat.ISO) {
    		throw new InvalidParameterValueException("Please specify a valid template.");
    	}
    	TemplateAdapter adapter = getAdapter(template.getHypervisorType());
    	TemplateProfile profile = adapter.prepareDelete(cmd);
    	return adapter.delete(profile);
	}
	
	@Override
    @ActionEvent(eventType = EventTypes.EVENT_ISO_DELETE, eventDescription = "deleting iso", async = true)
    public boolean deleteIso(DeleteIsoCmd cmd) {
        Long templateId = cmd.getId();
        Account caller = UserContext.current().getCaller();
        Long zoneId = cmd.getZoneId();
        
        VirtualMachineTemplate template = getTemplate(templateId);;
        if (template == null) {
            throw new InvalidParameterValueException("unable to find iso with id " + templateId);
        }
        
        _accountMgr.checkAccess(caller, AccessType.ModifyEntry, template);
         	
    	if (template.getFormat() != ImageFormat.ISO) {
    		throw new InvalidParameterValueException("Please specify a valid iso.");
    	}
    	
    	if (zoneId != null && (_ssvmMgr.findSecondaryStorageHost(zoneId) == null)) {
    		throw new InvalidParameterValueException("Failed to find a secondary storage host in the specified zone.");
    	}
    	TemplateAdapter adapter = getAdapter(template.getHypervisorType());
    	TemplateProfile profile = adapter.prepareDelete(cmd);
    	return adapter.delete(profile);
	}
	
	@Override
	public VirtualMachineTemplate getTemplate(long templateId) {
	    VMTemplateVO template = _tmpltDao.findById(templateId);
	    if (template != null && template.getRemoved() == null) {
	        return template;
	    }
	    
	    return null;
	}
}
