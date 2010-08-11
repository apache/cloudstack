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
import com.cloud.exception.PermissionDeniedException;
import com.cloud.network.IPAddressVO;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.serializer.GsonHelper;
import com.cloud.server.ManagementServer;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.dao.DomainRouterDao;
import com.google.gson.Gson;

public class DisassociateIpAddressExecutor extends BaseAsyncJobExecutor {
    public static final Logger s_logger = Logger.getLogger(DisassociateIpAddressExecutor.class.getName());
	
	public boolean execute() {
    	Gson gson = GsonHelper.getBuilder().create();
    	AsyncJobManager asyncMgr = getAsyncJobMgr();
    	AsyncJobVO job = getJob();
    	ManagementServer managementServer = asyncMgr.getExecutorContext().getManagementServer();
    	DisassociateIpAddressParam param = gson.fromJson(job.getCmdInfo(), DisassociateIpAddressParam.class);
    	
		if(getSyncSource() == null) {
			DomainRouterVO router = getRouterSyncSource(param);
	        if(router == null) {
				asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, 
					BaseCmd.NET_INVALID_PARAM_ERROR, "Unable to find router with given user " + param.getUserId() + " and ip " 
					+ param.getIpAddress() + " to disassociate");
	        } else {
		    	asyncMgr.syncAsyncJobExecution(job.getId(), "Router", router.getId());
	        }
			return true;
		} else {
	    	try {
	    		if(s_logger.isDebugEnabled())
	    			s_logger.debug("Executing disassociateIpAddress, uid: " + param.getUserId() + ", account id: " 
	    				+ param.getAccountId() + ", ip: " + param.getIpAddress());
				boolean result = managementServer.disassociateIpAddress(param.getUserId(), 
					param.getAccountId(), param.getIpAddress());
				
				if(result) {
					if(s_logger.isDebugEnabled())
						s_logger.debug("disassociateIpAddress executed successfully, complete async-execution");
					
					asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_SUCCEEDED, 0, "success");
				} else {
					s_logger.warn("disassociateIpAddress execution failed, complete async-execution");
					asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, "failed");
				}
			} catch (PermissionDeniedException e) {
				s_logger.warn("disassociateIpAddress execution failed : PermissionDeniedException, complete async-execution", e);
				asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.PARAM_ERROR, e.getMessage());
			} catch(IllegalArgumentException e) {
				s_logger.warn("disassociateIpAddress execution failed : IllegalArgumentException, complete async-execution", e);
				asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.PARAM_ERROR, e.getMessage());
			} catch(Exception e) {
				s_logger.warn("disassociateIpAddress execution failed : Exception, complete async-execution", e);
				asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.PARAM_ERROR, e.getMessage());
			}
		}
		return true;
	}
	
	private DomainRouterVO getRouterSyncSource(DisassociateIpAddressParam param) {
	    IPAddressDao ipAddressDao = getAsyncJobMgr().getExecutorContext().getIpAddressDao();
	    DomainRouterDao routerDao = getAsyncJobMgr().getExecutorContext().getRouterDao();
		
        IPAddressVO ip = null;
        try {
            ip = ipAddressDao.acquire(param.getIpAddress());
            
            DomainRouterVO router = null;
            if (ip.isSourceNat()) {
                router = routerDao.findByPublicIpAddress(param.getIpAddress());
            } else {
                router = routerDao.findBy(ip.getAccountId(), ip.getDataCenterId());
            }
            
            return router;
            
        } finally {
        	if(ip != null) {
	            ipAddressDao.release(param.getIpAddress());
        	}
        }
	}
}
