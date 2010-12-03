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
import com.cloud.event.EventTypes;
import com.cloud.event.EventUtils;
import com.cloud.event.EventVO;
import com.cloud.serializer.GsonHelper;
import com.cloud.server.ManagementServer;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine.Event;
import com.google.gson.Gson;

public class StopVMExecutor extends VMOperationExecutor {
    public static final Logger s_logger = Logger.getLogger(StopVMExecutor.class.getName());
	
	public boolean execute() {
    	Gson gson = GsonHelper.getBuilder().create();
    	AsyncJobManager asyncMgr = getAsyncJobMgr();
    	AsyncJobVO job = getJob();
    	VMOperationParam param = gson.fromJson(job.getCmdInfo(), VMOperationParam.class);
    	
    	/*
		if(getSyncSource() == null) {
	    	asyncMgr.syncAsyncJobExecution(job.getId(), "UserVM", param.getVmId());
	    	
	    	// always true if it does not have sync-source
	    	return true;
		} else {
			ManagementServer managementServer = asyncMgr.getExecutorContext().getManagementServer();
			managementServer.saveStartedEvent(param.getUserId(), param.getAccountId(), EventTypes.EVENT_VM_STOP,
				  							"Stopping Vm with Id: "+param.getVmId(), param.getEventId());
	    	OperationResponse response = asyncMgr.getExecutorContext().getVmMgr().executeStopVM(this, param);
	    	UserVmVO vm = managementServer.findUserVMInstanceById(param.getVmId());
	    	String params = "id="+vm.getId() + "\nvmName=" + vm.getName() + "\nsoId=" + vm.getServiceOfferingId() + "\ntId=" + vm.getTemplateId() + "\ndcId=" + vm.getDataCenterId();
	    	
	    	if (OperationResponse.STATUS_SUCCEEDED == response.getResultCode() ){
	    	    EventUtils.saveEvent(param.getUserId(), param.getAccountId(), EventVO.LEVEL_INFO, EventTypes.EVENT_VM_STOP, "Successfully stopped VM instance : " + param.getVmId(), params, param.getEventId());
	    		return true;
	    	}else if (OperationResponse.STATUS_FAILED == response.getResultCode()){
	    	    EventUtils.saveEvent(param.getUserId(), param.getAccountId(), EventVO.LEVEL_ERROR, EventTypes.EVENT_VM_STOP, "Failed to stop VM instance : " + response.getResultDescription(), params, param.getEventId());
	    		return true;
	    	}
		}
		*/
		return false;
	}
	
	public void processAnswer(VMOperationListener listener, long agentId, long seq, Answer answer) {
		
		UserVmVO vm = listener.getVm();
		VMOperationParam param = listener.getParam();
		AsyncJobManager asyncMgr = getAsyncJobMgr();
		ManagementServer managementServer = asyncMgr.getExecutorContext().getManagementServer();
        String params = "id="+vm.getId() + "\nvmName=" + vm.getHostName() + "\nsoId=" + vm.getServiceOfferingId() + "\ntId=" + vm.getTemplateId() + "\ndcId=" + vm.getDataCenterId();
    	if(s_logger.isDebugEnabled())
    		s_logger.debug("Execute asynchronize stop VM command: received answer, " + vm.getHostId() + "-" + seq);
    	
        boolean stopped = false;
    	if(answer != null && answer.getResult()) 
			stopped = true;

    	boolean jobStatusUpdated = false;
    	try {
	    	if(stopped) {
	    		// completeStopCommand will log the event, if we log it here we will end up with duplicated stop event
	        	asyncMgr.getExecutorContext().getVmMgr().completeStopCommand(param.getUserId(), vm, Event.OperationSucceeded, param.getEventId());
	    		asyncMgr.completeAsyncJob(getJob().getId(), 
		        		AsyncJobResult.STATUS_SUCCEEDED, 0, VMExecutorHelper.composeResultObject(asyncMgr.getExecutorContext().getManagementServer(), vm, null));
	    		jobStatusUpdated = true;
	    	} else {
	            asyncMgr.getExecutorContext().getItMgr().stateTransitTo(vm, Event.OperationFailed, vm.getHostId());
	            asyncMgr.completeAsyncJob(getJob().getId(), 
		        		AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, "Agent failed to stop VM");
	    		jobStatusUpdated = true;
	    		
	    		EventUtils.saveEvent(param.getUserId(), param.getAccountId(), EventVO.LEVEL_ERROR, EventTypes.EVENT_VM_STOP,
	    				"Failed to stop VM instance : " + vm.getHostName(), params, param.getEventId());
	    	}
    	} catch(Exception e) {
    		s_logger.error("Unexpected exception " + e.getMessage(), e);
    		if(!jobStatusUpdated) {
    	    	if(stopped) {
		    		asyncMgr.completeAsyncJob(getJob().getId(), 
		        		AsyncJobResult.STATUS_SUCCEEDED, 0, VMExecutorHelper.composeResultObject(asyncMgr.getExecutorContext().getManagementServer(), vm, null));
    	    	} else {
    	            asyncMgr.completeAsyncJob(getJob().getId(), 
    		        		AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, "Agent failed to stop VM");
    	            EventUtils.saveEvent(param.getUserId(), param.getAccountId(), EventVO.LEVEL_ERROR, EventTypes.EVENT_VM_STOP,
	    				"Failed to stop VM instance : " + vm.getHostName(), params, param.getEventId());
    	    	}
    		}
    	} finally {
    		asyncMgr.releaseSyncSource(this);
    	}
	}
	
	public void processDisconnect(VMOperationListener listener, long agentId) {
    	if(s_logger.isDebugEnabled())
    		s_logger.debug("Execute asynchronize stop VM command: agent " + agentId + " disconnected");
    	
    	processDisconnectAndTimeout(listener, "agent is disconnected");
	}

	public void processTimeout(VMOperationListener listener, long agentId, long seq) {
    	if(s_logger.isDebugEnabled())
    		s_logger.debug("Execute asynchronize stop VM command: timed out, " + agentId + "-" + seq);
    	
    	processDisconnectAndTimeout(listener, "operation timed out");
	}
	
	private void processDisconnectAndTimeout(VMOperationListener listener, String resultMessage) {
		UserVmVO vm = listener.getVm();
		VMOperationParam param = listener.getParam();
		AsyncJobManager asyncMgr = getAsyncJobMgr(); 
		
        EventVO event = new EventVO();
        event.setUserId(param.getUserId());
        event.setAccountId(vm.getAccountId());
        event.setType(EventTypes.EVENT_VM_STOP);
        event.setParameters("id="+vm.getId() + "\nvmName=" + vm.getHostName() + "\nsoId=" + vm.getServiceOfferingId() + "\ntId=" + vm.getTemplateId() + "\ndcId=" + vm.getDataCenterId());
        event.setDescription("failed to stop VM instance : " + vm.getHostName() + " due to " + resultMessage);
        event.setLevel(EventVO.LEVEL_ERROR);
        
        asyncMgr.completeAsyncJob(getJob().getId(), 
    		AsyncJobResult.STATUS_FAILED, 0, resultMessage);
    	asyncMgr.getExecutorContext().getEventDao().persist(event);
    	
    	asyncMgr.releaseSyncSource(this);
	}
}
