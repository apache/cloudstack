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
package com.cloud.template;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.BaseListTemplateOrIsoPermissionsCmd;
import org.apache.cloudstack.api.BaseUpdateTemplateOrIsoPermissionsCmd;
import org.apache.cloudstack.api.command.user.iso.DeleteIsoCmd;
import org.apache.cloudstack.api.command.user.iso.ExtractIsoCmd;
import org.apache.cloudstack.api.command.user.iso.ListIsoPermissionsCmd;
import org.apache.cloudstack.api.command.user.iso.RegisterIsoCmd;
import org.apache.cloudstack.api.command.user.iso.UpdateIsoPermissionsCmd;
import org.apache.cloudstack.api.command.user.template.CopyTemplateCmd;
import org.apache.cloudstack.api.command.user.template.CreateTemplateCmd;
import org.apache.cloudstack.api.command.user.template.DeleteTemplateCmd;
import org.apache.cloudstack.api.command.user.template.ExtractTemplateCmd;
import org.apache.cloudstack.api.command.user.template.ListTemplatePermissionsCmd;
import org.apache.cloudstack.api.command.user.template.RegisterTemplateCmd;
import org.apache.cloudstack.api.command.user.template.UpdateTemplatePermissionsCmd;
import org.apache.cloudstack.engine.subsystem.api.storage.CommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreRole;
import org.apache.cloudstack.engine.subsystem.api.storage.ImageDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.ImageService;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.AttachIsoCommand;
import com.cloud.agent.api.ComputeChecksumCommand;
import com.cloud.agent.api.downloadTemplateFromSwiftToSecondaryStorageCommand;
import com.cloud.agent.api.uploadTemplateToSwiftFromSecondaryStorageCommand;
import com.cloud.agent.api.storage.DestroyCommand;
import com.cloud.agent.api.storage.PrimaryStorageDownloadAnswer;
import com.cloud.agent.api.storage.PrimaryStorageDownloadCommand;
import com.cloud.agent.api.to.SwiftTO;

import com.cloud.api.ApiDBUtils;
import com.cloud.async.AsyncJobManager;
import com.cloud.async.AsyncJobVO;
import com.cloud.configuration.Config;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.event.UsageEventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.HypervisorGuruManager;
import com.cloud.projects.Project;
import com.cloud.projects.ProjectManager;

import com.cloud.resource.ResourceManager;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.LaunchPermissionVO;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage;

import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.StoragePoolStatus;
import com.cloud.storage.TemplateProfile;
import com.cloud.storage.Upload;
import com.cloud.storage.Upload.Type;

import com.cloud.storage.UploadVO;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateS3VO;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.VMTemplateSwiftVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VMTemplateZoneVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeManager;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.LaunchPermissionDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.UploadDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateDetailsDao;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VMTemplateS3Dao;
import com.cloud.storage.dao.VMTemplateSwiftDao;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.download.DownloadMonitor;
import com.cloud.storage.s3.S3Manager;
import com.cloud.storage.secondary.SecondaryStorageVmManager;
import com.cloud.storage.swift.SwiftManager;
import com.cloud.storage.upload.UploadMonitor;
import com.cloud.template.TemplateAdapter.TemplateAdapterType;

import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountService;
import com.cloud.user.AccountVO;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.User;
import com.cloud.user.UserContext;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserAccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.*;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

@Component
@Local(value={TemplateManager.class, TemplateService.class})
public class TemplateManagerImpl extends ManagerBase implements TemplateManager, TemplateService {
    private final static Logger s_logger = Logger.getLogger(TemplateManagerImpl.class);
    @Inject VMTemplateDao _tmpltDao;
    @Inject VMTemplateHostDao _tmpltHostDao;
    @Inject VMTemplatePoolDao _tmpltPoolDao;
    @Inject VMTemplateZoneDao _tmpltZoneDao;
    @Inject
    protected VMTemplateDetailsDao _templateDetailsDao;
    @Inject VMInstanceDao _vmInstanceDao;
    @Inject PrimaryDataStoreDao _poolDao;
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
    @Inject
    SwiftManager _swiftMgr;
    @Inject
    S3Manager _s3Mgr;
    @Inject
    VMTemplateSwiftDao _tmpltSwiftDao;
    @Inject
    VMTemplateS3Dao _vmS3TemplateDao;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    ClusterDao _clusterDao;
    @Inject DomainDao _domainDao;
    @Inject UploadDao _uploadDao;
    @Inject
    protected GuestOSDao _guestOSDao;
    long _routerTemplateId = -1;
    @Inject StorageManager _storageMgr;
    @Inject AsyncJobManager _asyncMgr;
    @Inject UserVmManager _vmMgr;
    @Inject UsageEventDao _usageEventDao;
    @Inject HypervisorGuruManager _hvGuruMgr;
    @Inject AccountService _accountService;
    @Inject ResourceLimitService _resourceLimitMgr;
    @Inject SecondaryStorageVmManager _ssvmMgr;
    @Inject LaunchPermissionDao _launchPermissionDao;
    @Inject ProjectManager _projectMgr;
    @Inject
    VolumeDataFactory volFactory;
    @Inject
    ImageDataFactory tmplFactory;
    @Inject
    SnapshotDataFactory snapshotFactory;
    @Inject
    ImageService imageSvr;
    @Inject
    DataStoreManager dataStoreMgr;
    @Inject
    protected ResourceManager _resourceMgr;
    @Inject VolumeManager volumeMgr;
    @Inject VMTemplateHostDao templateHostDao;

    
    int _primaryStorageDownloadWait;
    protected SearchBuilder<VMTemplateHostVO> HostTemplateStatesSearch;
    
    int _storagePoolMaxWaitSeconds = 3600;
    boolean _disableExtraction = false;
    ExecutorService _preloadExecutor;
    ScheduledExecutorService _swiftTemplateSyncExecutor;
    
    private ScheduledExecutorService _s3TemplateSyncExecutor = null;

    @Inject
    protected List<TemplateAdapter> _adapters;
    
    private TemplateAdapter getAdapter(HypervisorType type) {
    	TemplateAdapter adapter = null;
    	if (type == HypervisorType.BareMetal) {
    		adapter = AdapterBase.getAdapterByName(_adapters, TemplateAdapterType.BareMetal.getName());
    	} else {
    		// see HyervisorTemplateAdapter
    		adapter =  AdapterBase.getAdapterByName(_adapters, TemplateAdapterType.Hypervisor.getName());
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
    	VMTemplateVO template = adapter.create(profile);
    	
    	if (template != null){
        	return template;
        }else {
        	throw new CloudRuntimeException("Failed to create ISO");
        }
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
    	VMTemplateVO template = adapter.create(profile);
    	
    	if (template != null){
        	return template;
        }else {
        	throw new CloudRuntimeException("Failed to create a template");
        }
    }
    
    @Override
    public DataStore getImageStore(String storeUuid, Long zoneId) {
        DataStore imageStore = null;
        if (storeUuid != null) {
            imageStore = this.dataStoreMgr.getDataStore(storeUuid, DataStoreRole.Image);
        } else {
            List<DataStore> stores = this.dataStoreMgr.getImageStores(new ZoneScope(zoneId));
            if (stores.size() > 1) {
                throw new CloudRuntimeException("multiple image stores, don't know which one to use");
            }
            imageStore = stores.get(0);
        }
        
        return imageStore;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ISO_EXTRACT, eventDescription = "extracting ISO", async = true)
    public Long extract(ExtractIsoCmd cmd) {
        Account account = UserContext.current().getCaller();
        Long templateId = cmd.getId();
        Long zoneId = cmd.getZoneId();
        String url = cmd.getUrl();
        String mode = cmd.getMode();
        Long eventId = cmd.getStartEventId();
        
        // FIXME: async job needs fixing
        Long uploadId = extract(account, templateId, url, zoneId, mode, eventId, true, null, _asyncMgr);
        if (uploadId != null){
        	return uploadId;
        }else {
        	throw new CloudRuntimeException("Failed to extract the iso");
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_TEMPLATE_EXTRACT, eventDescription = "extracting template", async = true)
    public Long extract(ExtractTemplateCmd cmd) {
        Account caller = UserContext.current().getCaller();
        Long templateId = cmd.getId();
        Long zoneId = cmd.getZoneId();
        String url = cmd.getUrl();
        String mode = cmd.getMode();
        Long eventId = cmd.getStartEventId();

        // FIXME: async job needs fixing
        Long uploadId = extract(caller, templateId, url, zoneId, mode, eventId, false, null, _asyncMgr);
        if (uploadId != null){
        	return uploadId;
        }else {
        	throw new CloudRuntimeException("Failed to extract the teamplate");
        }
    }
    
    @Override
    public VirtualMachineTemplate prepareTemplate(long templateId, long zoneId) {
    	
    	VMTemplateVO vmTemplate = _tmpltDao.findById(templateId);
    	if(vmTemplate == null)
    		throw new InvalidParameterValueException("Unable to find template id=" + templateId);
    	
    	_accountMgr.checkAccess(UserContext.current().getCaller(), AccessType.ModifyEntry, true, vmTemplate);
    	
    	prepareTemplateInAllStoragePools(vmTemplate, zoneId);
    	return vmTemplate;
    }

    private Long extract(Account caller, Long templateId, String url, Long zoneId, String mode, Long eventId, boolean isISO, AsyncJobVO job, AsyncJobManager mgr) {
        String desc = Upload.Type.TEMPLATE.toString();
        if (isISO) {
            desc = Upload.Type.ISO.toString();
        }
        eventId = eventId == null ? 0:eventId;
        
        if (!_accountMgr.isRootAdmin(caller.getType()) && _disableExtraction) {
            throw new PermissionDeniedException("Extraction has been disabled by admin");
        }
        
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
        
		if (zoneId == null && _swiftMgr.isSwiftEnabled()) {
            zoneId = _swiftMgr.chooseZoneForTmpltExtract(templateId);
        }

        if (zoneId == null && _s3Mgr.isS3Enabled()) {
            zoneId = _s3Mgr.chooseZoneForTemplateExtract(template);
        }

        if (_dcDao.findById(zoneId) == null) {
            throw new IllegalArgumentException("Please specify a valid zone.");
        }
        
        if (!_accountMgr.isRootAdmin(caller.getType()) && !template.isExtractable()) {
            throw new InvalidParameterValueException("Unable to extract template id=" + templateId + " as it's not extractable");
        }
        
        _accountMgr.checkAccess(caller, AccessType.ModifyEntry, true, template);
        
        List<HostVO> sservers = getSecondaryStorageHosts(zoneId);

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
        
        if (tmpltHostRef == null && _swiftMgr.isSwiftEnabled()) {
            SwiftTO swift = _swiftMgr.getSwiftTO(templateId);
            if (swift != null && sservers != null) {
                downloadTemplateFromSwiftToSecondaryStorage(zoneId, templateId);
            }
        } else if (tmpltHostRef == null && _s3Mgr.isS3Enabled()) {
            if (sservers != null) {
                _s3Mgr.downloadTemplateFromS3ToSecondaryStorage(zoneId,
                        templateId, _primaryStorageDownloadWait);
            }
        }

        if (tmpltHostRef == null) {
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
	        			StoragePool pol = (StoragePool)dataStoreMgr.getPrimaryDataStore(pool.getId());
	        			prepareTemplateForCreate(template, pol);
	        			s_logger.info("End of preloading template " + template.getId() + " into primary storage " + pool.getId());
	    			}
	    		});
    		} else {
    			s_logger.info("Skip loading template " + template.getId() + " into primary storage " + pool.getId() + " as pool zone " + pool.getDataCenterId() + " is ");
    		}
    	}
    }
    
    String downloadTemplateFromSwiftToSecondaryStorage(long dcId, long templateId){
        VMTemplateVO template = _tmpltDao.findById(templateId);
        if ( template == null ) {
            String errMsg = " Can not find template " + templateId;
            s_logger.warn(errMsg);
            return errMsg;
        }
        VMTemplateSwiftVO tmpltSwift = _swiftMgr.findByTmpltId(templateId);
        if ( tmpltSwift == null ) {
            String errMsg = " Template " + templateId + " doesn't exist in swift";
            s_logger.warn(errMsg);
            return errMsg;
        }
        SwiftTO swift = _swiftMgr.getSwiftTO(tmpltSwift.getSwiftId());
        if ( swift == null ) {
            String errMsg = " Swift " + tmpltSwift.getSwiftId() + " doesn't exit ?";
            s_logger.warn(errMsg);
            return errMsg;
        }

        HostVO secHost = _ssvmMgr.findSecondaryStorageHost(dcId);
        if ( secHost == null ) {
            String errMsg = "Can not find secondary storage in data center " + dcId;
            s_logger.warn(errMsg);
            return errMsg;
        }

        downloadTemplateFromSwiftToSecondaryStorageCommand cmd = new downloadTemplateFromSwiftToSecondaryStorageCommand(swift, secHost.getName(), dcId, template.getAccountId(), templateId,
                tmpltSwift.getPath(), _primaryStorageDownloadWait);
        try {
            Answer answer = _agentMgr.sendToSSVM(dcId, cmd);
            if (answer == null || !answer.getResult()) {
                String errMsg = "Failed to download template from Swift to secondary storage due to " + (answer == null ? "answer is null" : answer.getDetails());
                s_logger.warn(errMsg);
                throw new CloudRuntimeException(errMsg);
            }
            String installPath = "template/tmpl/" + template.getAccountId() + "/" + template.getId() + "/" + tmpltSwift.getPath();
            VMTemplateHostVO tmpltHost = new VMTemplateHostVO(secHost.getId(), templateId, new Date(), 100, Status.DOWNLOADED, null, null, null, installPath, template.getUrl());
            tmpltHost.setSize(tmpltSwift.getSize());
            tmpltHost.setPhysicalSize(tmpltSwift.getPhysicalSize());
            _tmpltHostDao.persist(tmpltHost);
        } catch (Exception e) {
            String errMsg = "Failed to download template from Swift to secondary storage due to " + e.toString();
            s_logger.warn(errMsg);
            throw new CloudRuntimeException(errMsg);
        }
        return null;
    }

    String uploadTemplateToSwiftFromSecondaryStorage(VMTemplateHostVO templateHostRef) {
        Long templateId = templateHostRef.getTemplateId();
        VMTemplateVO template = _tmpltDao.findById(templateId);
        if (template == null) {
            String errMsg = " Can not find template " + templateId;
            s_logger.warn(errMsg);
            return errMsg;
        }

        if (template.getTemplateType() == TemplateType.PERHOST) {
            return null;
        }

        SwiftTO swift = _swiftMgr.getSwiftTO();
        if (swift == null) {
            String errMsg = " There is no Swift in this setup ";
            s_logger.warn(errMsg);
            return errMsg;
        }

        HostVO secHost = _hostDao.findById(templateHostRef.getHostId());
        if (secHost == null) {
            String errMsg = "Can not find secondary storage " + templateHostRef.getHostId();
            s_logger.warn(errMsg);
            return errMsg;
        }

        uploadTemplateToSwiftFromSecondaryStorageCommand cmd = new uploadTemplateToSwiftFromSecondaryStorageCommand(swift, secHost.getName(), secHost.getDataCenterId(), template.getAccountId(),
                templateId, _primaryStorageDownloadWait);
        Answer answer = null;
        try {
            answer = _agentMgr.sendToSSVM(secHost.getDataCenterId(), cmd);
            if (answer == null || !answer.getResult()) {
                if (template.getTemplateType() != TemplateType.SYSTEM) {
                    String errMsg = "Failed to upload template " + templateId + " to Swift from secondary storage due to " + ((answer == null) ? "null" : answer.getDetails());
                    s_logger.warn(errMsg);
                    throw new CloudRuntimeException(errMsg);
                }
                return null;
            }
            String path = templateHostRef.getInstallPath();
            int index = path.lastIndexOf('/');
            path = path.substring(index + 1);
            VMTemplateSwiftVO tmpltSwift = new VMTemplateSwiftVO(swift.getId(), templateHostRef.getTemplateId(), new Date(), path, templateHostRef.getSize(), templateHostRef.getPhysicalSize());
            _tmpltSwiftDao.persist(tmpltSwift);
            _swiftMgr.propagateTemplateOnAllZones(templateHostRef.getTemplateId());
        } catch (Exception e) {
            String errMsg = "Failed to upload template " + templateId + " to Swift from secondary storage due to " + e.toString();
            s_logger.warn(errMsg);
            throw new CloudRuntimeException(errMsg);
        }
        return null;
    }

    @Override @DB
    public VMTemplateStoragePoolVO prepareTemplateForCreate(VMTemplateVO templ, StoragePool pool) {
    	VMTemplateVO template = _tmpltDao.findById(templ.getId(), true);
    	
        long poolId = pool.getId();
        long templateId = template.getId();
        long dcId = pool.getDataCenterId();
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
        
        templateHostRef = findVmTemplateHost(templateId, pool);
        
        if (templateHostRef == null || templateHostRef.getDownloadState() != Status.DOWNLOADED) {
            String result = downloadTemplateFromSwiftToSecondaryStorage(dcId, templateId);
            if (result != null) {
                s_logger.error("Unable to find a secondary storage host who has completely downloaded the template.");
                return null;
            }
            result = _s3Mgr.downloadTemplateFromS3ToSecondaryStorage(dcId,
                    templateId, _primaryStorageDownloadWait);
            if (result != null) {
                s_logger.error("Unable to find a secondary storage host who has completely downloaded the template.");
                return null;
            }
            templateHostRef = findVmTemplateHost(templateId, pool);
            if (templateHostRef == null || templateHostRef.getDownloadState() != Status.DOWNLOADED) {
                s_logger.error("Unable to find a secondary storage host who has completely downloaded the template.");
                return null;
            }
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
        if (vos == null || vos.isEmpty()){
        	 throw new CloudRuntimeException("Cannot download " + templateId + " to poolId " + poolId + " since there is no host in the Up state connected to this pool");            
        }                
        
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
                   template.getAccountId(), pool, _primaryStorageDownloadWait);
            HostVO secondaryStorageHost = _hostDao.findById(templateHostRef.getHostId());
            assert(secondaryStorageHost != null);
            dcmd.setSecondaryStorageUrl(secondaryStorageHost.getStorageUrl());
            // TODO temporary hacking, hard-coded to NFS primary data store
            dcmd.setPrimaryStorageUrl("nfs://" + pool.getHostAddress() + pool.getPath());
            
            for (int retry = 0; retry < 2; retry ++){
            	Collections.shuffle(vos); // Shuffling to pick a random host in the vm deployment retries
            	StoragePoolHostVO vo = vos.get(0);
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
    public VMTemplateHostVO findVmTemplateHost(long templateId,
            StoragePool pool) {
        long dcId = pool.getDataCenterId();
        Long podId = pool.getPodId();

        List<HostVO> secHosts = _ssvmMgr
                .listSecondaryStorageHostsInOneZone(dcId);


        if (secHosts.size() == 1) {
            VMTemplateHostVO templateHostVO = this._tmpltHostDao
                    .findByHostTemplate(secHosts.get(0).getId(), templateId);
            return templateHostVO;
        }
        if (podId != null) {
            List<VMTemplateHostVO> templHosts = this._tmpltHostDao
                    .listByTemplateStatus(templateId, dcId, podId,
                            VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
            if (templHosts != null && !templHosts.isEmpty()) {
                Collections.shuffle(templHosts);
                return templHosts.get(0);
            }
        }
        List<VMTemplateHostVO> templHosts = this._tmpltHostDao
                .listByTemplateStatus(templateId, dcId,
                        VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
        if (templHosts != null && !templHosts.isEmpty()) {
            Collections.shuffle(templHosts);
            return templHosts.get(0);
        }
        return null;
    }
    
    @Override
    public String getChecksum(Long hostId, String templatePath) {
        HostVO ssHost = _hostDao.findById(hostId);
        Host.Type type = ssHost.getType();
        if (type != Host.Type.SecondaryStorage
                && type != Host.Type.LocalSecondaryStorage) {
            return null;
        }
        String secUrl = ssHost.getStorageUrl();
        Answer answer;
        answer = _agentMgr.sendToSecStorage(ssHost, new ComputeChecksumCommand(
                secUrl, templatePath));
        if (answer != null && answer.getResult()) {
            return answer.getDetails();
        }
        return null;
    }
    
    @Override
    @DB
    public VMTemplateHostVO prepareISOForCreate(VMTemplateVO template, StoragePool pool) {
        template = _tmpltDao.findById(template.getId(), true);

        long poolId = pool.getId();
        long templateId = template.getId();
        long dcId = pool.getDataCenterId();
        VMTemplateStoragePoolVO templateStoragePoolRef = null;
        VMTemplateHostVO templateHostRef = null;
        long templateStoragePoolRefId;
        String origUrl = null;

        templateHostRef = findVmTemplateHost(templateId, pool);

        if (templateHostRef == null || templateHostRef.getDownloadState() != Status.DOWNLOADED) {
            String result = downloadTemplateFromSwiftToSecondaryStorage(dcId, templateId);
            if (result != null) {
                s_logger.error("Unable to find a secondary storage host who has completely downloaded the template.");
                return null;
            }
            result = _s3Mgr.downloadTemplateFromS3ToSecondaryStorage(dcId,
                    templateId, _primaryStorageDownloadWait);
            if (result != null) {
                s_logger.error("Unable to find a secondary storage host who has completely downloaded the template.");
                return null;
            }
            templateHostRef = findVmTemplateHost(templateId, pool);
            if (templateHostRef == null || templateHostRef.getDownloadState() != Status.DOWNLOADED) {
                s_logger.error("Unable to find a secondary storage host who has completely downloaded the template.");
                return null;
            }
        }
        return templateHostRef;
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
        _resourceLimitMgr.checkResourceLimit(account, ResourceType.template);
               
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
                    UsageEventUtils.publishUsageEvent(copyEventType, account.getId(), dstZoneId, tmpltId, null, null, null, srcTmpltHost.getSize(),
                            template.getClass().getName(), template.getUuid());
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
        
        if (_swiftMgr.isSwiftEnabled()) {
            throw new CloudRuntimeException("copytemplate API is disabled in Swift setup, templates in Swift can be accessed by all Zones");
        }

        if (_s3Mgr.isS3Enabled()) {
            throw new CloudRuntimeException(
                    "copytemplate API is disabled in S3 setup -- S3 templates are accessible in all zones.");
        }

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
      
        HostVO dstSecHost = getSecondaryStorageHost(destZoneId, templateId);
        if ( dstSecHost != null ) {
            s_logger.debug("There is template " + templateId + " in secondary storage " + dstSecHost.getId() + " in zone " + destZoneId + " , don't need to copy");
            return template;
        }
        
        HostVO srcSecHost = getSecondaryStorageHost(sourceZoneId, templateId);
        if ( srcSecHost == null ) {
            throw new InvalidParameterValueException("There is no template " + templateId + " in zone " + sourceZoneId );
        }
       
        _accountMgr.checkAccess(caller, AccessType.ModifyEntry, true, template);
        
        boolean success = copy(userId, template, srcSecHost, sourceZone, dstZone);
        
    	if (success){
        	return template;
        }else {
        	throw new CloudRuntimeException("Failed to copy template");
        }
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
        StoragePool pool = (StoragePool)this.dataStoreMgr.getPrimaryDataStore(templatePoolVO.getPoolId());
		VMTemplateVO template = _tmpltDao.findByIdIncludingRemoved(templatePoolVO.getTemplateId());
		
		
		if (s_logger.isDebugEnabled()) {
		    s_logger.debug("Evicting " + templatePoolVO);
		}
		DestroyCommand cmd = new DestroyCommand(pool, templatePoolVO);
		
        try {
            Answer answer = _storageMgr.sendToPool(pool, cmd);
		
            if (answer != null && answer.getResult()) {
                // Remove the templatePoolVO
                if (_tmpltPoolDao.remove(templatePoolVO.getId())) {
                    s_logger.debug("Successfully evicted template: " + template.getName() + " from storage pool: " + pool.getName());
                }
            } else {
                s_logger.info("Will retry evicte template: " + template.getName() + " from storage pool: " + pool.getName());
            }
        } catch (StorageUnavailableException e) {
            s_logger.info("Storage is unavailable currently.  Will retry evicte template: " + template.getName() + " from storage pool: " + pool.getName());
        }

	}
    
    void swiftTemplateSync() {
        GlobalLock swiftTemplateSyncLock = GlobalLock.getInternLock("templatemgr.swiftTemplateSync");
        try {
            if (!_swiftMgr.isSwiftEnabled()) {
                return;
            }
            List<HypervisorType> hypers = _clusterDao.getAvailableHypervisorInZone(null);
            List<VMTemplateVO> templates = _tmpltDao.listByHypervisorType(hypers);
            List<Long> templateIds = new ArrayList<Long>();
            for (VMTemplateVO template : templates) {
                if (template.getTemplateType() != TemplateType.PERHOST) {
                    templateIds.add(template.getId());
                }
            }
            List<VMTemplateSwiftVO> templtSwiftRefs = _tmpltSwiftDao.listAll();
            for (VMTemplateSwiftVO templtSwiftRef : templtSwiftRefs) {
                templateIds.remove((Long) templtSwiftRef.getTemplateId());
            }
            if (templateIds.size() < 1) {
                return;
            }
            if (swiftTemplateSyncLock.lock(3)) {
                try {
                    List<VMTemplateHostVO> templtHostRefs = _tmpltHostDao.listByState(VMTemplateHostVO.Status.DOWNLOADED);
                    for (VMTemplateHostVO templtHostRef : templtHostRefs) {
                        if (templtHostRef.getDestroyed()) {
                            continue;
                        }
                        if (!templateIds.contains(templtHostRef.getTemplateId())) {
                            continue;
                        }
                        try {
                            uploadTemplateToSwiftFromSecondaryStorage(templtHostRef);
                        } catch (Exception e) {
                            s_logger.debug("failed to upload template " + templtHostRef.getTemplateId() + " to Swift due to " + e.toString());
                        }
                    }
                } catch (Throwable e) {
                    s_logger.error("Problem with sync swift template due to " + e.toString(), e);
                } finally {
                    swiftTemplateSyncLock.unlock();
                }
            }
        } catch (Throwable e) {
            s_logger.error("Problem with sync swift template due to " + e.toString(), e);
        } finally {
            swiftTemplateSyncLock.releaseRef();
        }
    }

    private Runnable getSwiftTemplateSyncTask() {
        return new Runnable() {
            @Override
            public void run() {
                if (s_logger.isDebugEnabled()) {
                    s_logger.trace("Start Swift Template sync at" + (new Date()));
                }
                swiftTemplateSync();
                if (s_logger.isTraceEnabled()) {
                    s_logger.trace("Finish Swift Template sync at" + (new Date()));
                }
            }
        };
    }

    @Override
    public boolean start() {
        _swiftTemplateSyncExecutor.scheduleAtFixedRate(getSwiftTemplateSyncTask(), 60, 60, TimeUnit.SECONDS);

        if (_s3TemplateSyncExecutor != null) {

            final int initialDelay = 60;
            final int period = 60;

            _s3TemplateSyncExecutor.scheduleAtFixedRate(new S3SyncTask(
                    this._tmpltDao, this._s3Mgr), initialDelay, period,
                    TimeUnit.SECONDS);
            s_logger.info(String.format("Started S3 sync task to execute "
                    + "execute every %1$s after an initial delay of %2$s.",
                    period, initialDelay));

        }

        return true;
    }

    @Override
    public boolean stop() {
        _swiftTemplateSyncExecutor.shutdownNow();

        if (_s3TemplateSyncExecutor != null) {
            _s3TemplateSyncExecutor.shutdownNow();
        }

        return true;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        
        final Map<String, String> configs = _configDao.getConfiguration("AgentManager", params);
        _routerTemplateId = NumbersUtil.parseInt(configs.get("router.template.id"), 1);

        String value = _configDao.getValue(Config.PrimaryStorageDownloadWait.toString());
        _primaryStorageDownloadWait = NumbersUtil.parseInt(value, Integer.parseInt(Config.PrimaryStorageDownloadWait.getDefaultValue()));

        String disableExtraction =  _configDao.getValue(Config.DisableExtraction.toString());
        _disableExtraction  = (disableExtraction == null) ? false : Boolean.parseBoolean(disableExtraction);

        HostTemplateStatesSearch = _tmpltHostDao.createSearchBuilder();
        HostTemplateStatesSearch.and("id", HostTemplateStatesSearch.entity().getTemplateId(), SearchCriteria.Op.EQ);
        HostTemplateStatesSearch.and("state", HostTemplateStatesSearch.entity().getDownloadState(), SearchCriteria.Op.EQ);
        
        SearchBuilder<HostVO> HostSearch = _hostDao.createSearchBuilder();
        HostSearch.and("dcId", HostSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        
        HostTemplateStatesSearch.join("host", HostSearch, HostSearch.entity().getId(), HostTemplateStatesSearch.entity().getHostId(), JoinBuilder.JoinType.INNER);
        HostSearch.done();
        HostTemplateStatesSearch.done();
        
        _storagePoolMaxWaitSeconds = NumbersUtil.parseInt(_configDao.getValue(Config.StoragePoolMaxWaitSeconds.key()), 3600);
        _preloadExecutor = Executors.newFixedThreadPool(8, new NamedThreadFactory("Template-Preloader"));
        _swiftTemplateSyncExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("swift-template-sync-Executor"));

        if (_s3Mgr.isS3Enabled()) {
            _s3TemplateSyncExecutor = Executors
                    .newSingleThreadScheduledExecutor(new NamedThreadFactory(
                            "s3-template-sync"));
        } else {
            s_logger.info("S3 secondary storage synchronization is disabled.");
        }

        return true;
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
	    List<UserVmVO> userVmUsingIso = _userVmDao.listByIsoId(templateId);
	    //check if there is any VM using this ISO.
	    if (!userVmUsingIso.isEmpty()) {
	        s_logger.debug("ISO " + template.getName() + " in zone " + zone.getName() + " is not deleteable because it is attached to " + userVmUsingIso.size() + " VMs");
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
	public boolean detachIso(long vmId)  {
        Account caller = UserContext.current().getCaller();
        Long userId = UserContext.current().getCallerUserId();
        
        // Verify input parameters
        UserVmVO vmInstanceCheck = _userVmDao.findById(vmId);
        if (vmInstanceCheck == null) {
            throw new InvalidParameterValueException ("Unable to find a virtual machine with id " + vmId);
        }
        
        UserVm userVM = _userVmDao.findById(vmId);
        if (userVM == null) {
            throw new InvalidParameterValueException("Please specify a valid VM.");
        }
        
        _accountMgr.checkAccess(caller, null, true, userVM);

        Long isoId = userVM.getIsoId();
        if (isoId == null) {
            throw new InvalidParameterValueException("The specified VM has no ISO attached to it.");
        }
    	UserContext.current().setEventDetails("Vm Id: " +vmId+ " ISO Id: "+isoId);
        
        State vmState = userVM.getState();
        if (vmState != State.Running && vmState != State.Stopped) {
        	throw new InvalidParameterValueException("Please specify a VM that is either Stopped or Running.");
        }

        boolean result = attachISOToVM(vmId, userId, isoId, false); //attach=false => detach
        if (result){
        	return result;
        }else {
        	throw new CloudRuntimeException("Failed to detach iso");
        }        
	}
	
	@Override
    @ActionEvent(eventType = EventTypes.EVENT_ISO_ATTACH, eventDescription = "attaching ISO", async = true)
	public boolean attachIso(long isoId, long vmId) {
        Account caller = UserContext.current().getCaller();
        Long userId = UserContext.current().getCallerUserId();
        
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
    	//check if caller has access to VM and ISO 
    	//and also check if the VM's owner has access to the ISO.
    	
    	_accountMgr.checkAccess(caller, null, false, iso, vm);
    	
    	Account vmOwner = _accountDao.findById(vm.getAccountId());
    	_accountMgr.checkAccess(vmOwner, null, false, iso, vm);
    	
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
        boolean result = attachISOToVM(vmId, userId, isoId, true);
        if (result){
        	return result;
        }else {
        	throw new CloudRuntimeException("Failed to attach iso");
        }
	}
	
    private boolean attachISOToVM(long vmId, long isoId, boolean attach) {
        UserVmVO vm = this._userVmDao.findById(vmId);

        if (vm == null) {
            return false;
        } else if (vm.getState() != State.Running) {
            return true;
        }
        String isoPath;
        VMTemplateVO tmplt = this._tmpltDao.findById(isoId);
        if (tmplt == null) {
            s_logger.warn("ISO: " + isoId + " does not exist");
            return false;
        }
        // Get the path of the ISO
        Pair<String, String> isoPathPair = null;
        if (tmplt.getTemplateType() == TemplateType.PERHOST) {
            isoPath = tmplt.getName();
        } else {
            isoPathPair = getAbsoluteIsoPath(isoId,
                    vm.getDataCenterId());
            if (isoPathPair == null) {
                s_logger.warn("Couldn't get absolute iso path");
                return false;
            } else {
                isoPath = isoPathPair.first();
            }
        }

        String vmName = vm.getInstanceName();

        HostVO host = _hostDao.findById(vm.getHostId());
        if (host == null) {
            s_logger.warn("Host: " + vm.getHostId() + " does not exist");
            return false;
        }
        AttachIsoCommand cmd = new AttachIsoCommand(vmName, isoPath, attach);
        if (isoPathPair != null) {
            cmd.setStoreUrl(isoPathPair.second());
        }
        Answer a = _agentMgr.easySend(vm.getHostId(), cmd);

        return (a != null && a.getResult());
    }

    private boolean attachISOToVM(long vmId, long userId, long isoId, boolean attach) {
    	UserVmVO vm = _userVmDao.findById(vmId);
    	VMTemplateVO iso = _tmpltDao.findById(isoId);

        boolean success = attachISOToVM(vmId, isoId, attach);
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
        
        _accountMgr.checkAccess(caller, AccessType.ModifyEntry, true, template);
    	
    	if (template.getFormat() == ImageFormat.ISO) {
    		throw new InvalidParameterValueException("Please specify a valid template.");
    	}
        if (cmd.getZoneId() == null && _swiftMgr.isSwiftEnabled()) {
            _swiftMgr.deleteTemplate(cmd);
        }
        if (cmd.getZoneId() == null && _s3Mgr.isS3Enabled()) {
            _s3Mgr.deleteTemplate(cmd.getId(), caller.getAccountId());
        }

    	TemplateAdapter adapter = getAdapter(template.getHypervisorType());
    	TemplateProfile profile = adapter.prepareDelete(cmd);
    	boolean result = adapter.delete(profile);
    	
    	if (result){
            if (cmd.getZoneId() == null
                    && (_swiftMgr.isSwiftEnabled() || _s3Mgr.isS3Enabled())) {
                List<VMTemplateZoneVO> templateZones = _tmpltZoneDao
                        .listByZoneTemplate(null, templateId);
                if (templateZones != null) {
                    for (VMTemplateZoneVO templateZone : templateZones) {
                        _tmpltZoneDao.remove(templateZone.getId());
                    }
                }
            }
    		return true;
    	}else{
    		throw new CloudRuntimeException("Failed to delete template");
    	}
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
        
        _accountMgr.checkAccess(caller, AccessType.ModifyEntry, true, template);
         	
    	if (template.getFormat() != ImageFormat.ISO) {
    		throw new InvalidParameterValueException("Please specify a valid iso.");
    	}
        if (cmd.getZoneId() == null && _swiftMgr.isSwiftEnabled()) {
            _swiftMgr.deleteIso(cmd);
    	}
        if (cmd.getZoneId() == null && _s3Mgr.isS3Enabled()) {
            _s3Mgr.deleteTemplate(caller.getAccountId(), templateId);
        }

    	if (zoneId != null && (_ssvmMgr.findSecondaryStorageHost(zoneId) == null)) {
    		throw new InvalidParameterValueException("Failed to find a secondary storage host in the specified zone.");
    	}
    	TemplateAdapter adapter = getAdapter(template.getHypervisorType());
    	TemplateProfile profile = adapter.prepareDelete(cmd);
        boolean result = adapter.delete(profile);
        if (result) {
            if (cmd.getZoneId() == null
                    && (_swiftMgr.isSwiftEnabled() || _s3Mgr.isS3Enabled())) {
                List<VMTemplateZoneVO> templateZones = _tmpltZoneDao
                        .listByZoneTemplate(null, templateId);
                if (templateZones != null) {
                    for (VMTemplateZoneVO templateZone : templateZones) {
                        _tmpltZoneDao.remove(templateZone.getId());
                    }
                }
            }
            return true;
        } else {
    		throw new CloudRuntimeException("Failed to delete ISO");
    	}
	}
	
	@Override
	public VirtualMachineTemplate getTemplate(long templateId) {
	    VMTemplateVO template = _tmpltDao.findById(templateId);
	    if (template != null && template.getRemoved() == null) {
	        return template;
	    }
	    
	    return null;
	}
	
    @Override
    public List<String> listTemplatePermissions(BaseListTemplateOrIsoPermissionsCmd cmd) {
        Account caller = UserContext.current().getCaller();
        Long id = cmd.getId();

        if (id == Long.valueOf(1)) {
            throw new PermissionDeniedException("unable to list permissions for " + cmd.getMediaType() + " with id " + id);
        }

        VirtualMachineTemplate template = getTemplate(id);
        if (template == null) {
            throw new InvalidParameterValueException("unable to find " + cmd.getMediaType() + " with id " + id);
        }
        
        if (cmd instanceof ListTemplatePermissionsCmd) {
            if (template.getFormat().equals(ImageFormat.ISO)) {
                throw new InvalidParameterValueException("Please provide a valid template");
            }
        } else if (cmd instanceof ListIsoPermissionsCmd) {
            if (!template.getFormat().equals(ImageFormat.ISO)) {
                throw new InvalidParameterValueException("Please provide a valid iso");
            }
        }

        if (!template.isPublicTemplate()) {
            _accountMgr.checkAccess(caller, null, true, template);
        }

        List<String> accountNames = new ArrayList<String>();
        List<LaunchPermissionVO> permissions = _launchPermissionDao.findByTemplate(id);
        if ((permissions != null) && !permissions.isEmpty()) {
            for (LaunchPermissionVO permission : permissions) {
                Account acct = _accountDao.findById(permission.getAccountId());
                accountNames.add(acct.getAccountName());
            }
        }
        return accountNames;
    }
    
    @DB
    @Override
    public boolean updateTemplateOrIsoPermissions(BaseUpdateTemplateOrIsoPermissionsCmd cmd) {
        Transaction txn = Transaction.currentTxn();

        // Input validation
        Long id = cmd.getId();
        Account caller = UserContext.current().getCaller();
        List<String> accountNames = cmd.getAccountNames();
        List<Long> projectIds = cmd.getProjectIds();
        Boolean isFeatured = cmd.isFeatured();
        Boolean isPublic = cmd.isPublic();
        Boolean isExtractable = cmd.isExtractable();
        String operation = cmd.getOperation();
        String mediaType = "";

        VMTemplateVO template = _tmpltDao.findById(id);

        if (template == null) {
            throw new InvalidParameterValueException("unable to find " + mediaType + " with id " + id);
        }

        if (cmd instanceof UpdateTemplatePermissionsCmd) {
            mediaType = "template";
            if (template.getFormat().equals(ImageFormat.ISO)) {
                throw new InvalidParameterValueException("Please provide a valid template");
            }
        }
        if (cmd instanceof UpdateIsoPermissionsCmd) {
            mediaType = "iso";
            if (!template.getFormat().equals(ImageFormat.ISO)) {
                throw new InvalidParameterValueException("Please provide a valid iso");
            }
        }
        
        //convert projectIds to accountNames
        if (projectIds != null) {
            for (Long projectId : projectIds) {
                Project project = _projectMgr.getProject(projectId);
                if (project == null) {
                    throw new InvalidParameterValueException("Unable to find project by id " + projectId);
                }
                
                if (!_projectMgr.canAccessProjectAccount(caller, project.getProjectAccountId())) {
                    throw new InvalidParameterValueException("Account " + caller + " can't access project id=" + projectId);
                }
                accountNames.add(_accountMgr.getAccount(project.getProjectAccountId()).getAccountName());
            }
        }

        _accountMgr.checkAccess(caller, AccessType.ModifyEntry, true, template);

        // If the template is removed throw an error.
        if (template.getRemoved() != null) {
            s_logger.error("unable to update permissions for " + mediaType + " with id " + id + " as it is removed  ");
            throw new InvalidParameterValueException("unable to update permissions for " + mediaType + " with id " + id + " as it is removed ");
        }

        if (id == Long.valueOf(1)) {
            throw new InvalidParameterValueException("unable to update permissions for " + mediaType + " with id " + id);
        }

        boolean isAdmin = _accountMgr.isAdmin(caller.getType());
        boolean allowPublicUserTemplates = Boolean.valueOf(_configDao.getValue("allow.public.user.templates"));
        if (!isAdmin && !allowPublicUserTemplates && isPublic != null && isPublic) {
            throw new InvalidParameterValueException("Only private " + mediaType + "s can be created.");
        }

        if (accountNames != null) {
            if ((operation == null) || (!operation.equalsIgnoreCase("add") && !operation.equalsIgnoreCase("remove") && !operation.equalsIgnoreCase("reset"))) {
                throw new InvalidParameterValueException("Invalid operation on accounts, the operation must be either 'add' or 'remove' in order to modify launch permissions."
                        + "  Given operation is: '" + operation + "'");
            }
        }

        Long accountId = template.getAccountId();
        if (accountId == null) {
            // if there is no owner of the template then it's probably already a public template (or domain private template) so
            // publishing to individual users is irrelevant
            throw new InvalidParameterValueException("Update template permissions is an invalid operation on template " + template.getName());
        }

        VMTemplateVO updatedTemplate = _tmpltDao.createForUpdate();

        if (isPublic != null) {
            updatedTemplate.setPublicTemplate(isPublic.booleanValue());
        }

        if (isFeatured != null) {
            updatedTemplate.setFeatured(isFeatured.booleanValue());
        }
        
       if (isExtractable != null && caller.getType() == Account.ACCOUNT_TYPE_ADMIN) {//Only ROOT admins allowed to change this powerful attribute
           updatedTemplate.setExtractable(isExtractable.booleanValue());
       }else if (isExtractable != null && caller.getType() != Account.ACCOUNT_TYPE_ADMIN) {
           throw new InvalidParameterValueException("Only ROOT admins are allowed to modify this attribute.");
       }

       _tmpltDao.update(template.getId(), updatedTemplate);

        Long domainId = caller.getDomainId();
        if ("add".equalsIgnoreCase(operation)) {
            txn.start();
            for (String accountName : accountNames) {
                Account permittedAccount = _accountDao.findActiveAccount(accountName, domainId);
                if (permittedAccount != null) {
                    if (permittedAccount.getId() == caller.getId()) {
                        continue; // don't grant permission to the template owner, they implicitly have permission
                    }
                    LaunchPermissionVO existingPermission = _launchPermissionDao.findByTemplateAndAccount(id, permittedAccount.getId());
                    if (existingPermission == null) {
                        LaunchPermissionVO launchPermission = new LaunchPermissionVO(id, permittedAccount.getId());
                        _launchPermissionDao.persist(launchPermission);
                    }
                } else {
                    txn.rollback();
                    throw new InvalidParameterValueException("Unable to grant a launch permission to account " + accountName + ", account not found.  "
                            + "No permissions updated, please verify the account names and retry.");
                }
            }
            txn.commit();
        } else if ("remove".equalsIgnoreCase(operation)) {
            List<Long> accountIds = new ArrayList<Long>();
            for (String accountName : accountNames) {
                Account permittedAccount = _accountDao.findActiveAccount(accountName, domainId);
                if (permittedAccount != null) {
                    accountIds.add(permittedAccount.getId());
                }
            }
            _launchPermissionDao.removePermissions(id, accountIds);
        } else if ("reset".equalsIgnoreCase(operation)) {
            // do we care whether the owning account is an admin? if the
            // owner is an admin, will we still set public to false?
            updatedTemplate = _tmpltDao.createForUpdate();
            updatedTemplate.setPublicTemplate(false);
            updatedTemplate.setFeatured(false);
            _tmpltDao.update(template.getId(), updatedTemplate);
            _launchPermissionDao.removeAllPermissions(id);
        }
        return true;
    }
    
    
    private String getRandomPrivateTemplateName() {
        return UUID.randomUUID().toString();
    }
    
   
    
   

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_TEMPLATE_CREATE, eventDescription = "creating template", async = true)
    public VirtualMachineTemplate createPrivateTemplate(CreateTemplateCmd command)
            throws CloudRuntimeException {
        Long userId = UserContext.current().getCallerUserId();
        if (userId == null) {
            userId = User.UID_SYSTEM;
        }
        long templateId = command.getEntityId();
        Long volumeId = command.getVolumeId();
        Long snapshotId = command.getSnapshotId();
        VMTemplateVO privateTemplate = null;
        Long accountId = null;
        SnapshotVO snapshot = null;

        try {
            TemplateInfo tmplInfo = this.tmplFactory.getTemplate(templateId);
            snapshot = _snapshotDao.findById(snapshotId);
            ZoneScope scope = new ZoneScope(snapshot.getDataCenterId());
            List<DataStore> store = this.dataStoreMgr.getImageStores(scope);
            if (store.size() > 1) {
                throw new CloudRuntimeException("muliple image data store, don't know which one to use");
            }
            AsyncCallFuture<CommandResult> future = null;
            if (snapshotId != null) {
                SnapshotInfo snapInfo = this.snapshotFactory.getSnapshot(snapshotId);
                future = this.imageSvr.createTemplateFromSnapshotAsync(snapInfo, tmplInfo, store.get(0));
            } else if (volumeId != null) {
               VolumeInfo volInfo = this.volFactory.getVolume(volumeId);
               future = this.imageSvr.createTemplateFromVolumeAsync(volInfo, tmplInfo, store.get(0));
            } else {
                throw new CloudRuntimeException(
                        "Creating private Template need to specify snapshotId or volumeId");
            }
            
            CommandResult result = null;
            try {
                result = future.get();
                if (result.isFailed()) {
                    privateTemplate = null;
                    s_logger.debug("Failed to create template" + result.getResult());
                    throw new CloudRuntimeException("Failed to create template" + result.getResult());
                }
                
                privateTemplate = this._tmpltDao.findById(templateId);
                UsageEventVO usageEvent = new UsageEventVO(
                        EventTypes.EVENT_TEMPLATE_CREATE,
                        privateTemplate.getAccountId(),
                        snapshot.getDataCenterId(),
                        privateTemplate.getId(), privateTemplate.getName(),
                        null, privateTemplate.getSourceTemplateId(),
                        privateTemplate.getSize());
                _usageEventDao.persist(usageEvent);
            } catch (InterruptedException e) {
                s_logger.debug("Failed to create template", e);
                throw new CloudRuntimeException("Failed to create template", e);
            } catch (ExecutionException e) {
                s_logger.debug("Failed to create template", e);
                throw new CloudRuntimeException("Failed to create template", e);
            }
            
        } finally {
            /*if (snapshot != null && snapshot.getSwiftId() != null
                    && secondaryStorageURL != null && zoneId != null
                    && accountId != null && volumeId != null) {
                _snapshotMgr.deleteSnapshotsForVolume(secondaryStorageURL,
                        zoneId, accountId, volumeId);
            }*/
            if (privateTemplate == null) {
                Transaction txn = Transaction.currentTxn();
                txn.start();
                // Remove the template record
                this._tmpltDao.expunge(templateId);

                // decrement resource count
                if (accountId != null) {
                    _resourceLimitMgr.decrementResourceCount(accountId,
                            ResourceType.template);
                }
                txn.commit();
            }
        }

        if (privateTemplate != null) {
            return privateTemplate;
        } else {
            throw new CloudRuntimeException("Failed to create a template");
        }
    }

    private static boolean isAdmin(short accountType) {
        return ((accountType == Account.ACCOUNT_TYPE_ADMIN)
                || (accountType == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN)
                || (accountType == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) || (accountType == Account.ACCOUNT_TYPE_READ_ONLY_ADMIN));
    }
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_TEMPLATE_CREATE, eventDescription = "creating template", create = true)
    public VMTemplateVO createPrivateTemplateRecord(CreateTemplateCmd cmd,
            Account templateOwner) throws ResourceAllocationException {
        Long userId = UserContext.current().getCallerUserId();

        Account caller = UserContext.current().getCaller();
        boolean isAdmin = (isAdmin(caller.getType()));

        _accountMgr.checkAccess(caller, null, true, templateOwner);

        String name = cmd.getTemplateName();
        if ((name == null) || (name.length() > 32)) {
            throw new InvalidParameterValueException(
                    "Template name cannot be null and should be less than 32 characters");
        }

        if (cmd.getTemplateTag() != null) {
            if (!_accountService.isRootAdmin(caller.getType())) {
                throw new PermissionDeniedException(
                        "Parameter templatetag can only be specified by a Root Admin, permission denied");
            }
        }

        // do some parameter defaulting
        Integer bits = cmd.getBits();
        Boolean requiresHvm = cmd.getRequiresHvm();
        Boolean passwordEnabled = cmd.isPasswordEnabled();
        Boolean isPublic = cmd.isPublic();
        Boolean featured = cmd.isFeatured();
        int bitsValue = ((bits == null) ? 64 : bits.intValue());
        boolean requiresHvmValue = ((requiresHvm == null) ? true : requiresHvm
                .booleanValue());
        boolean passwordEnabledValue = ((passwordEnabled == null) ? false
                : passwordEnabled.booleanValue());
        if (isPublic == null) {
            isPublic = Boolean.FALSE;
        }
        boolean allowPublicUserTemplates = Boolean.parseBoolean(_configDao
                .getValue("allow.public.user.templates"));
        if (!isAdmin && !allowPublicUserTemplates && isPublic) {
            throw new PermissionDeniedException("Failed to create template "
                    + name + ", only private templates can be created.");
        }

        Long volumeId = cmd.getVolumeId();
        Long snapshotId = cmd.getSnapshotId();
        if ((volumeId == null) && (snapshotId == null)) {
            throw new InvalidParameterValueException(
                    "Failed to create private template record, neither volume ID nor snapshot ID were specified.");
        }
        if ((volumeId != null) && (snapshotId != null)) {
            throw new InvalidParameterValueException(
                    "Failed to create private template record, please specify only one of volume ID ("
                            + volumeId
                            + ") and snapshot ID ("
                            + snapshotId
                            + ")");
        }

        HypervisorType hyperType;
        VolumeVO volume = null;
        VMTemplateVO privateTemplate = null;
        if (volumeId != null) { // create template from volume
            volume = this._volumeDao.findById(volumeId);
            if (volume == null) {
                throw new InvalidParameterValueException(
                        "Failed to create private template record, unable to find volume "
                                + volumeId);
            }
            // check permissions
            _accountMgr.checkAccess(caller, null, true, volume);

            // If private template is created from Volume, check that the volume
            // will not be active when the private template is
            // created
            if (!this.volumeMgr.volumeInactive(volume)) {
                String msg = "Unable to create private template for volume: "
                        + volume.getName()
                        + "; volume is attached to a non-stopped VM, please stop the VM first";
                if (s_logger.isInfoEnabled()) {
                    s_logger.info(msg);
                }
                throw new CloudRuntimeException(msg);
            }
            hyperType = this._volumeDao.getHypervisorType(volumeId);
        } else { // create template from snapshot
            SnapshotVO snapshot = _snapshotDao.findById(snapshotId);
            if (snapshot == null) {
                throw new InvalidParameterValueException(
                        "Failed to create private template record, unable to find snapshot "
                                + snapshotId);
            }

            volume = this._volumeDao.findById(snapshot.getVolumeId());
            VolumeVO snapshotVolume = this._volumeDao
                    .findByIdIncludingRemoved(snapshot.getVolumeId());

            // check permissions
            _accountMgr.checkAccess(caller, null, true, snapshot);

            if (snapshot.getState() != Snapshot.State.BackedUp) {
                throw new InvalidParameterValueException("Snapshot id="
                        + snapshotId + " is not in " + Snapshot.State.BackedUp
                        + " state yet and can't be used for template creation");
            }

            /*
             * // bug #11428. Operation not supported if vmware and snapshots
             * parent volume = ROOT if(snapshot.getHypervisorType() ==
             * HypervisorType.VMware && snapshotVolume.getVolumeType() ==
             * Type.DATADISK){ throw new UnsupportedServiceException(
             * "operation not supported, snapshot with id " + snapshotId +
             * " is created from Data Disk"); }
             */

            hyperType = snapshot.getHypervisorType();
        }

        _resourceLimitMgr.checkResourceLimit(templateOwner,
                ResourceType.template);

        if (!isAdmin || featured == null) {
            featured = Boolean.FALSE;
        }
        Long guestOSId = cmd.getOsTypeId();
        GuestOSVO guestOS = this._guestOSDao.findById(guestOSId);
        if (guestOS == null) {
            throw new InvalidParameterValueException("GuestOS with ID: "
                    + guestOSId + " does not exist.");
        }

        String uniqueName = Long.valueOf((userId == null) ? 1 : userId)
                .toString()
                + UUID.nameUUIDFromBytes(name.getBytes()).toString();
        Long nextTemplateId = this._tmpltDao.getNextInSequence(Long.class, "id");
        String description = cmd.getDisplayText();
        boolean isExtractable = false;
        Long sourceTemplateId = null;
        if (volume != null) {
            VMTemplateVO template = ApiDBUtils.findTemplateById(volume
                    .getTemplateId());
            isExtractable = template != null
                    && template.isExtractable()
                    && template.getTemplateType() != Storage.TemplateType.SYSTEM;
            if (template != null) {
                sourceTemplateId = template.getId();
            } else if (volume.getVolumeType() == Volume.Type.ROOT) { // vm created out
                // of blank
                // template
                UserVm userVm = ApiDBUtils.findUserVmById(volume
                        .getInstanceId());
                sourceTemplateId = userVm.getIsoId();
            }
        }
        String templateTag = cmd.getTemplateTag();
        if (templateTag != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Adding template tag: " + templateTag);
            }
        }
        privateTemplate = new VMTemplateVO(nextTemplateId, uniqueName, name,
                ImageFormat.RAW, isPublic, featured, isExtractable,
                TemplateType.USER, null, null, requiresHvmValue, bitsValue,
                templateOwner.getId(), null, description, passwordEnabledValue,
                guestOS.getId(), true, hyperType, templateTag, cmd.getDetails());
        if (sourceTemplateId != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("This template is getting created from other template, setting source template Id to: "
                        + sourceTemplateId);
            }
        }
        privateTemplate.setSourceTemplateId(sourceTemplateId);

        VMTemplateVO template = this._tmpltDao.persist(privateTemplate);
        // Increment the number of templates
        if (template != null) {
            if (cmd.getDetails() != null) {
                this._templateDetailsDao.persist(template.getId(), cmd.getDetails());
            }

            _resourceLimitMgr.incrementResourceCount(templateOwner.getId(),
                    ResourceType.template);
        }

        if (template != null) {
            return template;
        } else {
            throw new CloudRuntimeException("Failed to create a template");
        }

    }
    
    @Override
    public Pair<String, String> getAbsoluteIsoPath(long templateId,
            long dataCenterId) {
        String isoPath = null;

        List<HostVO> storageHosts = _resourceMgr.listAllHostsInOneZoneByType(
                Host.Type.SecondaryStorage, dataCenterId);
        if (storageHosts != null) {
            for (HostVO storageHost : storageHosts) {
                List<VMTemplateHostVO> templateHostVOs = this._tmpltHostDao
                        .listByTemplateHostStatus(
                                templateId,
                                storageHost.getId(),
                                VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
                if (templateHostVOs != null && !templateHostVOs.isEmpty()) {
                    VMTemplateHostVO tmpHostVO = templateHostVOs.get(0);
                    isoPath = storageHost.getStorageUrl() + "/"
                            + tmpHostVO.getInstallPath();
                    return new Pair<String, String>(isoPath,
                            storageHost.getStorageUrl());
                }
            }
        }
        s_logger.warn("Unable to find secondary storage in zone id="
                + dataCenterId);
        return null;
    }

    @Override
    public String getSecondaryStorageURL(long zoneId) {
        // Determine the secondary storage URL
        HostVO secondaryStorageHost = getSecondaryStorageHost(zoneId);

        if (secondaryStorageHost == null) {
            return null;
        }

        return secondaryStorageHost.getStorageUrl();
    }

    @Override
    public HostVO getSecondaryStorageHost(long zoneId, long tmpltId) {
        List<HostVO> hosts = _ssvmMgr
                .listSecondaryStorageHostsInOneZone(zoneId);
        if (hosts == null || hosts.size() == 0) {
            return null;
        }
        for (HostVO host : hosts) {
            VMTemplateHostVO tmpltHost = this._tmpltHostDao.findByHostTemplate(
                    host.getId(), tmpltId);
            if (tmpltHost != null
                    && !tmpltHost.getDestroyed()
                    && tmpltHost.getDownloadState() == VMTemplateStorageResourceAssoc.Status.DOWNLOADED) {
                return host;
            }
        }
        return null;
    }

    @Override
    public VMTemplateHostVO getTemplateHostRef(long zoneId, long tmpltId,
            boolean readyOnly) {
        List<HostVO> hosts = _ssvmMgr
                .listSecondaryStorageHostsInOneZone(zoneId);
        if (hosts == null || hosts.size() == 0) {
            return null;
        }
        VMTemplateHostVO inProgress = null;
        VMTemplateHostVO other = null;
        for (HostVO host : hosts) {
            VMTemplateHostVO tmpltHost = this._tmpltHostDao.findByHostTemplate(
                    host.getId(), tmpltId);
            if (tmpltHost != null && !tmpltHost.getDestroyed()) {
                if (tmpltHost.getDownloadState() == VMTemplateStorageResourceAssoc.Status.DOWNLOADED) {
                    return tmpltHost;
                } else if (tmpltHost.getDownloadState() == VMTemplateStorageResourceAssoc.Status.DOWNLOAD_IN_PROGRESS) {
                    inProgress = tmpltHost;
                } else {
                    other = tmpltHost;
                }
            }
        }
        if (inProgress != null) {
            return inProgress;
        }
        return other;
    }

    @Override
    public HostVO getSecondaryStorageHost(long zoneId) {
        List<HostVO> hosts = _ssvmMgr
                .listSecondaryStorageHostsInOneZone(zoneId);
        if (hosts == null || hosts.size() == 0) {
            hosts = _ssvmMgr.listLocalSecondaryStorageHostsInOneZone(zoneId);
            if (hosts.isEmpty()) {
                return null;
            }
        }

        int size = hosts.size();
        Random rn = new Random();
        int index = rn.nextInt(size);
        return hosts.get(index);
    }

    @Override
    public List<HostVO> getSecondaryStorageHosts(long zoneId) {
        List<HostVO> hosts = _ssvmMgr
                .listSecondaryStorageHostsInOneZone(zoneId);
        if (hosts == null || hosts.size() == 0) {
            hosts = _ssvmMgr.listLocalSecondaryStorageHostsInOneZone(zoneId);
            if (hosts.isEmpty()) {
                return new ArrayList<HostVO>();
            }
        }
        return hosts;
    }
    
    @Override
    public Long getTemplateSize(long templateId, long zoneId) {
        SearchCriteria<VMTemplateHostVO> sc = HostTemplateStatesSearch.create();
        sc.setParameters("id", templateId);
        sc.setParameters(
                "state",
                com.cloud.storage.VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
        sc.setJoinParameters("host", "dcId", zoneId);
        List<VMTemplateSwiftVO> tsvs = _tmpltSwiftDao
                .listByTemplateId(templateId);
        Long size = null;
        if (tsvs != null && tsvs.size() > 0) {
            size = tsvs.get(0).getSize();
        }

        if (size == null && _s3Mgr.isS3Enabled()) {
            VMTemplateS3VO vmTemplateS3VO = _vmS3TemplateDao
                    .findOneByTemplateId(templateId);
            if (vmTemplateS3VO != null) {
                size = vmTemplateS3VO.getSize();
            }
        }

        if (size == null) {
            List<VMTemplateHostVO> sss = this.templateHostDao.search(sc, null);
            if (sss == null || sss.size() == 0) {
                throw new CloudRuntimeException("Template "
                        + templateId
                        + " has not been completely downloaded to zone "
                        + zoneId);
            }
            size = sss.get(0).getSize();
        }
        return size;
    }

}
