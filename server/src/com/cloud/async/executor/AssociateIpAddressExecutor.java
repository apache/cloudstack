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

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.async.AsyncJobManager;
import com.cloud.async.AsyncJobResult;
import com.cloud.async.AsyncJobVO;
import com.cloud.async.BaseAsyncJobExecutor;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InternalErrorException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.network.IPAddressVO;
import com.cloud.serializer.GsonHelper;
import com.cloud.server.ManagementServer;
import com.cloud.user.Account;
import com.cloud.vm.DomainRouterVO;
import com.google.gson.Gson;

public class AssociateIpAddressExecutor extends BaseAsyncJobExecutor {
    public static final Logger s_logger = Logger.getLogger(AssociateIpAddressExecutor.class.getName());

	public boolean execute() {
    	Gson gson = GsonHelper.getBuilder().create();
    	AsyncJobManager asyncMgr = getAsyncJobMgr();
    	AsyncJobVO job = getJob();
    	ManagementServer managementServer = asyncMgr.getExecutorContext().getManagementServer();
    	AssociateIpAddressParam param = gson.fromJson(job.getCmdInfo(), AssociateIpAddressParam.class);

		if(getSyncSource() == null) {
	        DomainRouterVO router = asyncMgr.getExecutorContext().getRouterDao().findBy(param.getAccountId(), param.getZoneId());
	        if(router == null) {
				asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, 
					BaseCmd.NET_INVALID_PARAM_ERROR, "Unable to find router with given zone " + param.getZoneId() + " and account " + param.getAccountId());
	        } else {
		    	asyncMgr.syncAsyncJobExecution(job.getId(), "Router", router.getId());
	        }
	        
			return true;
		} else {
	    	try {
				String ipAddress = managementServer.associateIpAddress(param.getUserId(), param.getAccountId(), 
						param.getDomainId(), param.getZoneId());
				asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_SUCCEEDED, 0, 
					composeResultObject(managementServer, param, ipAddress));
			} catch (ResourceAllocationException e) {
				if(s_logger.isDebugEnabled())
					s_logger.debug("Unable to associate ip address : " + e.getMessage());
				
	        	if (e.getResourceType().equals("vm")) 
	    			asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, 
	    				BaseCmd.VM_ALLOCATION_ERROR, e.getMessage());
	        	else if (e.getResourceType().equals("ip"))
	    			asyncMgr.completeAsyncJob(getJob().getId(),  AsyncJobResult.STATUS_FAILED, 
	    				BaseCmd.IP_ALLOCATION_ERROR, e.getMessage());
			} catch (InsufficientAddressCapacityException e) {
				if(s_logger.isDebugEnabled())
					s_logger.debug("Unable to associate ip address : " + e.getMessage());
				
				asyncMgr.completeAsyncJob(getJob().getId(), 
		    		AsyncJobResult.STATUS_FAILED, BaseCmd.NET_IP_ASSOC_ERROR, e.getMessage());
			} catch (InvalidParameterValueException e) {
				if(s_logger.isDebugEnabled())
					s_logger.debug("Unable to associate ip address : " + e.getMessage());
				
				asyncMgr.completeAsyncJob(getJob().getId(), 
		    		AsyncJobResult.STATUS_FAILED, BaseCmd.NET_INVALID_PARAM_ERROR, e.getMessage());
			} catch (InternalErrorException e) {
				if(s_logger.isDebugEnabled())
					s_logger.debug("Unable to associate ip address : " + e.getMessage());
				
				asyncMgr.completeAsyncJob(getJob().getId(), 
		    		AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, e.getMessage());
			} catch(Exception e) {
				s_logger.warn("Unable to associate ip address : " + e.getMessage(), e);
				
				asyncMgr.completeAsyncJob(getJob().getId(), 
		    		AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, e.getMessage());
			}
		}
    	return true;
	}
	
	private AssociateIpAddressResultObject composeResultObject(ManagementServer managementServer,
			AssociateIpAddressParam param, String newIpAddr) {
		AssociateIpAddressResultObject resultObject = new AssociateIpAddressResultObject();
		resultObject.setIpAddress(newIpAddr);

    	List<IPAddressVO> ipAddresses = managementServer.listPublicIpAddressesBy(param.getAccountId(), true, null, null);
    	IPAddressVO ipAddress = null;
    	for (Iterator<IPAddressVO> iter = ipAddresses.iterator(); iter.hasNext();) {
    		IPAddressVO current = iter.next();
    		if (current.getAddress().equals(newIpAddr)) {
    			ipAddress = current;
    			break;
    		}
    	}
    	
    	if(ipAddress != null) {
    		if(ipAddress.getAllocated() != null)
    			resultObject.setAllocated(ipAddress.getAllocated());
    		resultObject.setZoneId(ipAddress.getDataCenterId());
    		resultObject.setZoneName(managementServer.findDataCenterById(ipAddress.getDataCenterId()).getName());
    		resultObject.setSourceNat(ipAddress.isSourceNat());
    		resultObject.setVlanDbId(ipAddress.getVlanDbId());
    		resultObject.setVlanId(managementServer.findVlanById(ipAddress.getVlanDbId()).getVlanId());
    		Account account = getAsyncJobMgr().getExecutorContext().getManagementServer().findAccountById(param.getAccountId());
    		if(account != null)
    			resultObject.setAcountName(account.getAccountName());
    		else 
    			resultObject.setAcountName("");
    	}
		return resultObject;
	}
}
