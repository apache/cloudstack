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
import com.cloud.async.AsyncJobManager;
import com.cloud.async.AsyncJobResult;
import com.cloud.async.AsyncJobVO;
import com.cloud.async.BaseAsyncJobExecutor;
import com.cloud.serializer.GsonHelper;
import com.cloud.server.ManagementServer;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.user.Account;
import com.cloud.vm.UserVm;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VmStats;
import com.google.gson.Gson;

public class UpgradeVMExecutor extends BaseAsyncJobExecutor {
    public static final Logger s_logger = Logger.getLogger(UpgradeVMExecutor.class.getName());
	
	public boolean execute() {
		AsyncJobManager asyncMgr = getAsyncJobMgr();
    	AsyncJobVO job = getJob();
    	Gson gson = GsonHelper.getBuilder().create();
		
		if(getSyncSource() == null) {
	    	VMOperationParam param = gson.fromJson(job.getCmdInfo(), VMOperationParam.class);
	    	asyncMgr.syncAsyncJobExecution(job.getId(), "UserVM", param.getVmId());
	    	
	    	// always true if it does not have sync-source
	    	return true;
		} else {
			ManagementServer managementServer = asyncMgr.getExecutorContext().getManagementServer();
			UpgradeVMParam param = gson.fromJson(job.getCmdInfo(), UpgradeVMParam.class);
			
			try {
				asyncMgr.updateAsyncJobAttachment(job.getId(), "vm_instance", param.getVmId());
			    boolean success = managementServer.upgradeVirtualMachine(param.getUserId(), 
			    	param.getVmId(), param.getServiceOfferingId(), param.getEventId());
	
		        if (success) {
		        	//get the upgraded vm to compose the result object
		        	UserVmVO userVm = managementServer.findUserVMInstanceById(param.getVmId());
					asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_SUCCEEDED, 0, 
							composeResultObject(userVm, managementServer));
					
		        } else {
					asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, 
						BaseCmd.VM_CHANGE_SERVICE_ERROR, 
						composeResultObject(null, managementServer));
		        }
			} catch(Exception e) {
				s_logger.warn("Unable to upgrade VM " + param.getVmId() + ":" + e.getMessage(), e);
				asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, 
					BaseCmd.INTERNAL_ERROR, 
					e.getMessage());
			}
			return true;
		}
	}
	
	private VmResultObject composeResultObject(UserVmVO vm, ManagementServer ms)
	{
		if(vm == null)
			return null;
		
		VmResultObject resultObj = new VmResultObject();
		Account acct = ms.findAccountById(Long.valueOf(vm.getAccountId()));
		resultObj.setAccount(acct.getAccountName());
		
		ServiceOfferingVO offering = ms.findServiceOfferingById(vm.getServiceOfferingId());
		resultObj.setCpuSpeed(offering.getSpeed());
		resultObj.setMemory(offering.getRamSize());
		if(offering.getDisplayText()!=null)
			resultObj.setServiceOfferingName(offering.getDisplayText());
		else
			resultObj.setServiceOfferingName(offering.getName());
		resultObj.setServiceOfferingId(vm.getServiceOfferingId());
		
		VmStats vmStats = ms.getVmStatistics(vm.getId());
		if(vmStats != null)
		{
			resultObj.setCpuUsed((long) vmStats.getCPUUtilization());
			resultObj.setNetworkKbsRead((long) vmStats.getNetworkReadKBs());
			resultObj.setNetworkKbsWrite((long) vmStats.getNetworkWriteKBs());
		}
		
		resultObj.setCreated(vm.getCreated());
		resultObj.setDisplayName(vm.getDisplayName());
		resultObj.setDomain(ms.findDomainIdById(acct.getDomainId()).getName());
		resultObj.setDomainId(acct.getDomainId());
		resultObj.setHaEnable(vm.isHaEnabled());
		if(vm.getHostId() != null)
		{
			resultObj.setHostId(vm.getHostId());
			resultObj.setHostName(ms.getHostBy(vm.getHostId()).getName());
		}
		resultObj.setIpAddress(vm.getPrivateIpAddress());
		resultObj.setName(vm.getName());
		resultObj.setState(vm.getState().toString());
		resultObj.setZoneId(vm.getDataCenterId());
		resultObj.setZoneName(ms.findDataCenterById(vm.getDataCenterId()).getName());
		
		VMTemplateVO template = ms.findTemplateById(vm.getTemplateId());
		resultObj.setPasswordEnabled(template.getEnablePassword());
		resultObj.setTemplateDisplayText(template.getDisplayText());
		resultObj.setTemplateId(template.getId());
		resultObj.setTemplateName(template.getName());
		
		return resultObj;
	}
}
