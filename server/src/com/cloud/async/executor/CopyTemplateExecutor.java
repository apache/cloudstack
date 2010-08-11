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

import com.cloud.agent.api.Answer;
import com.cloud.api.BaseCmd;
import com.cloud.async.AsyncJobManager;
import com.cloud.async.AsyncJobResult;
import com.cloud.async.AsyncJobVO;
import com.cloud.async.BaseAsyncJobExecutor;
import com.cloud.dc.DataCenterVO;
import com.cloud.domain.DomainVO;
import com.cloud.serializer.GsonHelper;
import com.cloud.server.ManagementServer;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.google.gson.Gson;

public class CopyTemplateExecutor extends BaseAsyncJobExecutor {
    public static final Logger s_logger = Logger.getLogger(CopyTemplateExecutor.class.getName());
	
	public boolean execute() {
    	Gson gson = GsonHelper.getBuilder().create();
    	AsyncJobManager asyncMgr = getAsyncJobMgr();
    	AsyncJobVO job = getJob();

    	CopyTemplateParam param = gson.fromJson(job.getCmdInfo(), CopyTemplateParam.class);
    	
    	ManagementServer managementServer = asyncMgr.getExecutorContext().getManagementServer();
 
		try {
			boolean success = managementServer.copyTemplate(param.getUserId(), param.getTemplateId(), param.getSourceZoneId(), param.getDestZoneId(), param.getEventId());

			if (success) {
				VMTemplateVO template = managementServer.findTemplateById(param.getTemplateId());
				DataCenterVO destZone = managementServer.findDataCenterById(param.getDestZoneId());
				VMTemplateHostVO templateHostRef = managementServer.findTemplateHostRef(param.getTemplateId(), destZone.getId());
				long guestOsId = template.getGuestOSId();
		        Account owner = managementServer.findAccountById(template.getAccountId());
		        DomainVO domain = managementServer.findDomainIdById(owner.getDomainId());
		        String guestOSName = managementServer.findGuestOSById(guestOsId).getName();
				asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_SUCCEEDED, 0, composeResultObject(template, templateHostRef, destZone,guestOSName, owner, domain));
			} else {
				asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, "Failed to copy template.");
			}
		
		} catch (Exception e) {
			s_logger.warn("Unable to copy template: " + e.getMessage(), e);
    		asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, e.getMessage());
		}
		
    	return true;
	}

	public void processAnswer(VMOperationListener listener, long agentId, long seq, Answer answer) {
	}
	
	public void processDisconnect(VMOperationListener listener, long agentId) {
	}

	public void processTimeout(VMOperationListener listener, long agentId, long seq) {
	}
	
	private CopyTemplateResultObject composeResultObject(VMTemplateVO template, VMTemplateHostVO templateHostRef, DataCenterVO destZone, String guestOSName, Account owner, DomainVO domain) {
		CopyTemplateResultObject resultObject = new CopyTemplateResultObject();
		
		
        // If the user is an admin, add the template download status
		boolean isAdmin = false;
		
		if(owner.getType() == Account.ACCOUNT_TYPE_ADMIN)
			isAdmin = true;
		
		if (isAdmin || owner.getId().longValue() == template.getAccountId()) {
            // add download status
            if (templateHostRef.getDownloadState()!=Status.DOWNLOADED) {
                String templateStatus = "Processing";
                if (templateHostRef.getDownloadState() == VMTemplateHostVO.Status.DOWNLOAD_IN_PROGRESS) {
                    if (templateHostRef.getDownloadPercent() == 100) {
                        templateStatus = "Installing Template";
                    } else {
                        templateStatus = templateHostRef.getDownloadPercent() + "% Downloaded";
                    }
                } else {
                    templateStatus = templateHostRef.getErrorString();
                }
                resultObject.setTemplateStatus(templateStatus);
            } else if (templateHostRef.getDownloadState() == VMTemplateHostVO.Status.DOWNLOADED) {
            	resultObject.setTemplateStatus("Download Complete");
            } else {
            	resultObject.setTemplateStatus("Successfully Installed");
            }
        }
		
		resultObject.setAccountName(owner.getAccountName());
		resultObject.setDomainId(owner.getDomainId());
		resultObject.setDomainName(domain.getName());
		resultObject.setId(template.getId());
		resultObject.setName(template.getName());
		resultObject.setDisplayText(template.getDisplayText());
		resultObject.setPublic(template.isPublicTemplate());
		resultObject.setOsTypeName(guestOSName);
		resultObject.setCreated(templateHostRef.getCreated());
		resultObject.setReady(templateHostRef != null && templateHostRef.getDownloadState() == Status.DOWNLOADED);
		resultObject.setFeatured(template.isFeatured());
		resultObject.setPasswordEnabled(template.getEnablePassword());
		resultObject.setFormat(template.getFormat());
		resultObject.setGuestOsId(template.getGuestOSId());
		resultObject.setZoneId(destZone.getId());
		resultObject.setZoneName(destZone.getName());
		return resultObject;
	}
}
