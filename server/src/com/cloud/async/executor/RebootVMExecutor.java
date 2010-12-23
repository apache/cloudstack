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
import com.cloud.serializer.GsonHelper;
import com.cloud.server.ManagementServer;
import com.cloud.vm.UserVmVO;
import com.google.gson.Gson;

public class RebootVMExecutor extends VMOperationExecutor {
    public static final Logger s_logger = Logger.getLogger(RebootVMExecutor.class.getName());
	
	public boolean execute() {
    	Gson gson = GsonHelper.getBuilder().create();
    	AsyncJobManager asyncMgr = getAsyncJobMgr();
    	AsyncJobVO job = getJob();
    	VMOperationParam param = gson.fromJson(job.getCmdInfo(), VMOperationParam.class);
    	ManagementServer managementServer = asyncMgr.getExecutorContext().getManagementServer();
    	UserVmVO vm = managementServer.findUserVMInstanceById(param.getVmId());
		OperationResponse response;

		/*
		if(getSyncSource() == null) {
	    	asyncMgr.syncAsyncJobExecution(job.getId(), "UserVM", param.getVmId());
	    	
	    	// always true if it does not have sync-source
	    	return true;
		} else {
			
			managementServer.saveStartedEvent(param.getUserId(), param.getAccountId(), EventTypes.EVENT_VM_REBOOT, "Rebooting Vm with Id: "+param.getVmId(), param.getEventId());
			asyncMgr.updateAsyncJobAttachment(job.getId(), "vm_instance", param.getVmId());
			response = asyncMgr.getExecutorContext().getVmMgr().executeRebootVM(this, param);	    		    	
	    	String params = "id="+vm.getId() + "\nvmName=" + vm.getName() + "\nsoId=" + vm.getServiceOfferingId() + "\ntId=" + vm.getTemplateId() + "\ndcId=" + vm.getDataCenterId();
	    	
	    	if (OperationResponse.STATUS_SUCCEEDED == response.getResultCode() ){
	    		managementServer.saveEvent(param.getUserId(), param.getAccountId(), EventVO.LEVEL_INFO, EventTypes.EVENT_VM_REBOOT,
	    				"Rebooting Vm with Id: " + param.getVmId(), params, param.getEventId());
	    		return true;
	    	}else if (OperationResponse.STATUS_FAILED == response.getResultCode()){
	    		managementServer.saveEvent(param.getUserId(), param.getAccountId(), EventVO.LEVEL_ERROR, EventTypes.EVENT_VM_REBOOT,
	    				"Failed to reboot VM instance : " + response.getResultDescription(), params, param.getEventId());
	    		return true;
	    	}
	    	return false;
		}
		*/
		return true;
	}
	
	public void processAnswer(VMOperationListener listener, long agentId, long seq, Answer answer) {
		UserVmVO vm = listener.getVm();
		AsyncJobManager asyncMgr = getAsyncJobMgr();
		
		boolean jobStatusUpdated = false;
		try {
	    	if(s_logger.isDebugEnabled())
	    		s_logger.debug("Execute asynchronize Reboot VM command: received answer, " + vm.getHostId() + "-" + seq);
	    		        	    	
	    	if(answer != null) {
	    		asyncMgr.completeAsyncJob(getJob().getId(), 
	        		AsyncJobResult.STATUS_SUCCEEDED, 0, VMExecutorHelper.composeResultObject(asyncMgr.getExecutorContext().getManagementServer(), vm, null));
	    		jobStatusUpdated = true;	            
	    	} else {
	    		asyncMgr.completeAsyncJob(getJob().getId(), 
	            		AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, "Agent is unable to execute the command");
	    		
	    		jobStatusUpdated = true;
	    	}
	    	    		
		} catch(Exception e) {
			s_logger.error("Unexpected exception " + e.getMessage(), e);
			
			if(!jobStatusUpdated) {
		    	if(answer != null) {
		    		asyncMgr.completeAsyncJob(getJob().getId(), 
		        		AsyncJobResult.STATUS_SUCCEEDED, 0, VMExecutorHelper.composeResultObject(asyncMgr.getExecutorContext().getManagementServer(), vm, null));
		    	} else {
		    		asyncMgr.completeAsyncJob(getJob().getId(), 
		            		AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, "Agent is unable to execute the command");
		    	}
			}
		} finally {
			asyncMgr.releaseSyncSource(this);
		}
	}
	
	public void processDisconnect(VMOperationListener listener, long agentId) {
    	if(s_logger.isDebugEnabled())
    		s_logger.debug("Execute asynchronize Reboot VM command: agent " + agentId + " disconnected");
    	
    	processDisconnectAndTimeout(listener, "agent is disconnected");
	}
	
	public void processTimeout(VMOperationListener listener, long agentId, long seq) {
    	if(s_logger.isDebugEnabled())
    		s_logger.debug("Execute asynchronize Reboot VM command: timed out, " + agentId + "-" + seq);
    	
    	processDisconnectAndTimeout(listener, "operation timed out");
	}
	
	private void processDisconnectAndTimeout(VMOperationListener listener, String resultMessage) {
		
		AsyncJobManager asyncMgr = getAsyncJobMgr(); 
		
        boolean jobStatusUpdated = false;
        try {
			asyncMgr.completeAsyncJob(getJob().getId(), 
	        		AsyncJobResult.STATUS_FAILED, 0, resultMessage);
			jobStatusUpdated = true;
        } catch (Exception e) {
        	s_logger.error("Unexpected exception " + e.getMessage(), e);
        	if(!jobStatusUpdated)
    			asyncMgr.completeAsyncJob(getJob().getId(), 
    	        		AsyncJobResult.STATUS_FAILED, 0, resultMessage);
        } finally {
        	asyncMgr.releaseSyncSource(this);
        }
	}
}
