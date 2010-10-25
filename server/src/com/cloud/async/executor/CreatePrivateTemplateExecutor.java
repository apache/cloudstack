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

package com.cloud.async.executor;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.async.AsyncJobExecutorContext;
import com.cloud.async.AsyncJobManager;
import com.cloud.async.AsyncJobResult;
import com.cloud.async.AsyncJobVO;
import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.dc.DataCenterVO;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.HostVO;
import com.cloud.serializer.GsonHelper;
import com.cloud.server.ManagementServer;
import com.cloud.storage.GuestOS;
import com.cloud.storage.Snapshot;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmManager;
import com.google.gson.Gson;

public class CreatePrivateTemplateExecutor extends VolumeOperationExecutor {
    public static final Logger s_logger = Logger.getLogger(CreatePrivateTemplateExecutor.class.getName());
	
	public boolean execute() {
    	Gson gson = GsonHelper.getBuilder().create();
    	AsyncJobManager asyncMgr = getAsyncJobMgr();
    	AsyncJobVO job = getJob();

		if(getSyncSource() == null) {
		    CreatePrivateTemplateParam param = gson.fromJson(job.getCmdInfo(), CreatePrivateTemplateParam.class);
	    	asyncMgr.syncAsyncJobExecution(job.getId(), "Volume", param.getVolumeId());
	    	
	    	// always true if it does not have sync-source
	    	return true;
		} else {
	    	CreatePrivateTemplateParam param = gson.fromJson(job.getCmdInfo(), CreatePrivateTemplateParam.class);
	    	
	    	AsyncJobExecutorContext asyncJobExecutorContext = asyncMgr.getExecutorContext();
	    	ManagementServer managerServer = asyncJobExecutorContext.getManagementServer();
	    	AccountManager accountManager = asyncJobExecutorContext.getAccountMgr();
	    	UserVmManager vmMgr = asyncJobExecutorContext.getVmMgr();
	    	
	    	Long snapshotId = param.getSnapshotId();
	    	Long volumeId = param.getVolumeId();
	    	
	    	// By default, assume failure
	    	String details = "Could not create private template from ";
	    	if (snapshotId != null) {
	    	    details += "snapshot with id: " + snapshotId;
	    	}
	    	else {
	    	    details += "volume with id: " + volumeId;
	    	}
	    	// Job details
	    	Long jobId = getJob().getId();
	    	int jobStatus = AsyncJobResult.STATUS_FAILED;
            int resultCode = BaseCmd.INTERNAL_ERROR;
            Object resultObject = null;
			try {
		        // Check that the resource limit for templates won't be exceeded
				VolumeVO volume = managerServer.findAnyVolumeById(volumeId);
				
				if (volume == null) {
					throw new InvalidParameterValueException("Could not find active volume with ID " + volumeId);
				}
				
		    	AccountVO account = (AccountVO) managerServer.findAccountById(volume.getAccountId());
		        if (accountManager.resourceLimitExceeded(account, ResourceType.template)) {
		            details += ", reason: The maximum number of templates for the specified account has been exceeded.";
		        }
		        else {
                    VMTemplateVO template = vmMgr.createPrivateTemplateRecord(param.getUserId(),
                                                                              param.getVolumeId(),
                                                                              param.getName(),
                                                                              param.getDescription(),
                                                                              param.getGuestOsId(),
                                                                              param.getRequiresHvm(),
                                                                              param.getBits(),
                                                                              param.isPasswordEnabled(),
                                                                              param.isPublic(),
                                                                              param.isFeatured());
    
    		    	if (template != null) {
        		    	
                    	if(s_logger.isInfoEnabled())
                    		s_logger.info("CreatePrivateTemplate created a new instance " + template.getId() 
                    			+ ", update async job-" + job.getId() + " progress status");
        
                    	asyncMgr.updateAsyncJobAttachment(job.getId(), "vm_template", template.getId());
                    	asyncMgr.updateAsyncJobStatus(job.getId(), BaseCmd.PROGRESS_INSTANCE_CREATED, template.getId());
                    	Snapshot snapshot = null;
        	    	        
            	        if (snapshotId == null && managerServer.getHyperType().equalsIgnoreCase("KVM")) {
            	            // We are create private template from volume. Create a snapshot, copy the vhd chain of the disk to secondary storage. 
            	            // For template snapshot, we use a separate snapshot method.
            	            snapshot = vmMgr.createTemplateSnapshot(param.getUserId(), param.getVolumeId());           		       
            	        }
            	        else {
            	            // We are creating a private template from an already present snapshot. 
            	            // This snapshot is already backed up on secondary storage.
            	            snapshot = managerServer.findSnapshotById(snapshotId);
            	        }
            	        
                        if (snapshot == null) {
                            details += ", reason: Failed to create snapshot for basis of private template";
                        } else {
                            param.setSnapshotId(snapshot.getId());
            		    	
            				template = managerServer.createPrivateTemplate(template, 
            				                                               param.getUserId(), 
            						                                       param.getSnapshotId(), 
            						                                       param.getName(), 
            						                                       param.getDescription());
            				            				            			
            				if(template != null) {
            					VMTemplateHostVO templateHostRef = managerServer.findTemplateHostRef(template.getId(), volume.getDataCenterId());
            				    jobStatus = AsyncJobResult.STATUS_SUCCEEDED;
            				    resultCode = 0;
            				    details = null;
            				    resultObject = composeResultObject(template, templateHostRef, volume.getDataCenterId());
            				} 
            				
                        }
    		    	}
		        }
			} catch (InvalidParameterValueException e) {
			    details += ", reason: " + e.getMessage();
				s_logger.error(details, e);
				resultCode = BaseCmd.PARAM_ERROR;
			} catch (Exception e) {
	            details += ", reason: " + e.getMessage();
	            s_logger.error(details, e);
			}
			
			if (jobStatus == AsyncJobResult.STATUS_FAILED) {
			    resultObject = details;
			}
	        asyncMgr.completeAsyncJob(jobId, jobStatus, resultCode, resultObject);
	    	return true;
		}
	}
	
	private CreatePrivateTemplateResultObject composeResultObject(VMTemplateVO template, VMTemplateHostVO templateHostRef, Long dataCenterId) {
		CreatePrivateTemplateResultObject resultObject = new CreatePrivateTemplateResultObject();
		
		resultObject.setId(template.getId());
		resultObject.setName(template.getName());
		resultObject.setDisplayText(template.getDisplayText());
		resultObject.setPublic(template.isPublicTemplate());
		resultObject.setCreated(templateHostRef.getCreated());
		resultObject.setReady(templateHostRef != null && templateHostRef.getDownloadState() == Status.DOWNLOADED);
		resultObject.setPasswordEnabled(template.getEnablePassword());
		ManagementServer managerServer = getAsyncJobMgr().getExecutorContext().getManagementServer();
		GuestOS os = managerServer.findGuestOSById(template.getGuestOSId());
        if (os != null) {
        	resultObject.setOsTypeId(os.getId());
        	resultObject.setOsTypeName(os.getDisplayName());
        } else {
        	resultObject.setOsTypeId(-1L);
        	resultObject.setOsTypeName("");
        }
        
        Account owner = managerServer.findAccountById(template.getAccountId());
        if (owner != null) {
        	resultObject.setAccount(owner.getAccountName());
        	resultObject.setDomainId(owner.getDomainId());
        	resultObject.setDomainName(managerServer.findDomainIdById(owner.getDomainId()).getName());
        }

        
    	DataCenterVO zone = managerServer.findDataCenterById(dataCenterId);
    	if (zone != null) {
    		resultObject.setZoneId(zone.getId());
    		resultObject.setZoneName(zone.getName());
    	}
        	
		return resultObject;
	}
}
