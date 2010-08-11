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
import com.cloud.async.executor.VMOperationParam.VmOp;
import com.cloud.exception.InternalErrorException;
import com.cloud.serializer.GsonHelper;
import com.cloud.server.ManagementServer;
import com.cloud.vm.SecondaryStorageVmVO;
import com.cloud.vm.VMInstanceVO;
import com.google.gson.Gson;

public class SystemVmCmdExecutor extends VMOperationExecutor {
    public static final Logger s_logger = Logger.getLogger(SystemVmCmdExecutor.class.getName());
    
	public boolean execute() {
    	Gson gson = GsonHelper.getBuilder().create();
    	AsyncJobManager asyncMgr = getAsyncJobMgr();
    	AsyncJobVO job = getJob();
		ManagementServer managementServer = asyncMgr.getExecutorContext().getManagementServer();
    	VMOperationParam param = gson.fromJson(job.getCmdInfo(), VMOperationParam.class);
    	VmOp oper = param.getOperation();
    	VMInstanceVO vm;
		if(getSyncSource() == null) {
	    	asyncMgr.syncAsyncJobExecution(job.getId(), "SystemVm", param.getVmId());
			return true;
		} else {
	    	try {
	    		switch (oper){
	    		case Destroy:
	    			asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.UNSUPPORTED_ACTION_ERROR, 
					"operation not allowed");
	    			break;
	    		case Noop:
	    			asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_SUCCEEDED, 0, "noop operation");
	    			break;
	    		case Start:
	    			vm = managementServer.startSystemVM(param.getVmId(), param.getEventId());
	    			if(vm != null)
		    			asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_SUCCEEDED, 0, 
							composeResultObject(managementServer, vm));
		    		else
		    			asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, 
							"operation failed");
	    			break;
	    		case Stop:
	    			boolean result = managementServer.stopSystemVM(param.getVmId(), param.getEventId());
	    			if(result) {
	    				vm = managementServer.findSystemVMById(param.getVmId());
		    			asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_SUCCEEDED, 0, composeResultObject(managementServer, vm));
	    			} else {
		    			asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, 
							"operation failed");
	    			}
	    			break;
	    		case Reboot:
	    			result = managementServer.rebootSystemVM(param.getVmId(), param.getEventId());
	    			if(result) {
	    				vm = managementServer.findSystemVMById(param.getVmId());
		    			asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_SUCCEEDED, 0, composeResultObject(managementServer, vm));
	    			} else {
		    			asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, 
							"operation failed");
	    			}
	    			break;
	    		default:
	    			assert false :"Unknown vm operation";
	    		}
	    		
			} catch (InternalErrorException e) {
				asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, 
					e.getMessage());
			} catch(Exception e) {
				s_logger.warn("Unable to start console vm " + param.getVmId() + ":" + e.getMessage(), e);
				asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, 
					e.getMessage());
			}
	    	return true;
		}
	}
	
	private Object composeResultObject(ManagementServer managementServer, VMInstanceVO vm) {
		SystemVmOperationResultObject result = new SystemVmOperationResultObject();
		if (vm instanceof SecondaryStorageVmVO) {
			SecondaryStorageVmVO ssVm = (SecondaryStorageVmVO)vm;
		result.setId(ssVm.getId());
		result.setName(vm.getName());
		result.setZoneId(ssVm.getDataCenterId());
		result.setZoneName(managementServer.findDataCenterById(ssVm.getDataCenterId()).getName());
		result.setDns1(ssVm.getDns1());
		result.setDns2(ssVm.getDns2());
		result.setNetworkDomain(ssVm.getDomain());
		result.setGateway(ssVm.getGateway());
		result.setPodId(ssVm.getPodId());
		result.setHostId(ssVm.getHostId());
		if(ssVm.getHostId() != null) 
			result.setHostName(managementServer.getHostBy(ssVm.getHostId()).getName());
		
		result.setPrivateIp(ssVm.getPrivateIpAddress());
		result.setPrivateMac(ssVm.getPrivateMacAddress());
		result.setPrivateNetmask(ssVm.getPrivateNetmask());
		result.setPublicIp(ssVm.getPublicIpAddress());
		result.setPublicMac(ssVm.getPublicMacAddress());
		result.setPublicNetmask(ssVm.getPublicNetmask());
		result.setTemplateId(ssVm.getTemplateId());
		result.setCreated(ssVm.getCreated());
		result.setState(ssVm.getState().toString());
		//result.setNfsShare(ssVm.getNfsShare());
		}
		return result;
	}

	public void processAnswer(VMOperationListener listener, long agentId, long seq, Answer answer) {
	}
	
	public void processDisconnect(VMOperationListener listener, long agentId) {
	}

	public void processTimeout(VMOperationListener listener, long agentId, long seq) {
	}
}
