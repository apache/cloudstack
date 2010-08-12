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

import java.text.DecimalFormat;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.async.AsyncJobManager;
import com.cloud.async.AsyncJobResult;
import com.cloud.async.AsyncJobVO;
import com.cloud.async.BaseAsyncJobExecutor;
import com.cloud.host.Host;
import com.cloud.host.HostStats;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.Status.Event;
import com.cloud.serializer.GsonHelper;
import com.cloud.server.ManagementServer;
import com.cloud.service.ServiceOffering;
import com.cloud.storage.GuestOSCategoryVO;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.fsm.StateMachine;
//import com.cloud.vm.HostStats;
import com.cloud.vm.UserVmVO;
import com.google.gson.Gson;

public class ReconnectExecutor extends BaseAsyncJobExecutor {
    public static final Logger s_logger = Logger.getLogger(ReconnectExecutor.class.getName());
	
    @Override
    public boolean execute() {
    	
    	AsyncJobManager asyncMgr = getAsyncJobMgr();
    	Gson gson = GsonHelper.getBuilder().create();
    	AsyncJobVO job = getJob();

		if(getSyncSource() == null) {
	    	Long param = gson.fromJson(job.getCmdInfo(), Long.class);
	    	asyncMgr.syncAsyncJobExecution(job.getId(), "host", param.longValue());
	    	
	    	// always true if it does not have sync-source
	    	return true;
		} else {
			ManagementServer managementServer = asyncMgr.getExecutorContext().getManagementServer();
			Long param = gson.fromJson(job.getCmdInfo(), Long.class);
		
			try {
				boolean success = managementServer.reconnect(param.longValue());
				if(success)
				{
					HostVO host = managementServer.getHostBy(param);
					final StateMachine<Status, Event> sm = new StateMachine<Status, Event>();
					asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_SUCCEEDED, 0, composeResultObject(host,sm,managementServer));
				}
				else
				{
					HostVO host = managementServer.getHostBy(param);
					final StateMachine<Status, Event> sm = new StateMachine<Status, Event>();
					asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, 
							composeResultObject(host,sm, managementServer));
				}
			} catch(Exception e) {
				s_logger.warn("Unable to reconnect host " + param + ": " + e.getMessage(), e);
				asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, 
					e.getMessage());
			}
		}
		return true;
    }
    
	   private HostResultObject composeResultObject(HostVO hostVO, StateMachine<Status, Event> sm, ManagementServer managementServer)
	    {
	    	HostResultObject hostRO = new HostResultObject();

	    	hostRO.setId(hostVO.getId());
	    	
	    	hostRO.setName(hostVO.getName());
	    	hostRO.setState(hostVO.getStatus().toString());
	    	
	    	if(hostVO.getDisconnectedOn() != null)
	    		hostRO.setDisconnected(hostVO.getDisconnectedOn());
	    	
	    	
            if (hostVO.getType() != null) {
                hostRO.setType(hostVO.getType().toString());
            }
            
            GuestOSCategoryVO guestOSCategory = managementServer.getHostGuestOSCategory(hostVO.getId());
            if (guestOSCategory != null) {
            	hostRO.setOsCategoryId(guestOSCategory.getId());
            	hostRO.setOsCategoryName(guestOSCategory.getName());
            }
	    	
            
            hostRO.setIpAddress(hostVO.getPrivateIpAddress());
            hostRO.setZoneId(hostVO.getDataCenterId());
            hostRO.setZoneName(managementServer.getDataCenterBy(hostVO.getDataCenterId()).getName());

            if (hostVO.getPodId() != null && managementServer.findHostPodById(hostVO.getPodId()) != null) {
            	hostRO.setPodId(hostVO.getPodId());
            	hostRO.setPodName((managementServer.findHostPodById(hostVO.getPodId())).getName());
            }
            
            hostRO.setVersion(hostVO.getVersion().toString());
            
            if (hostVO.getHypervisorType() != null) {
                hostRO.setHypervisorType(hostVO.getHypervisorType().toString());
            }
	    	
            if ((hostVO.getCpus() != null) && (hostVO.getSpeed() != null) && !(hostVO.getType().toString().equals("Storage"))) 
            {
            	
                hostRO.setCpuNumber(hostVO.getCpus());
                hostRO.setCpuSpeed(hostVO.getSpeed());
                // calculate cpu allocated by vm
                int cpu = 0;
                String cpuAlloc = null;
                DecimalFormat decimalFormat = new DecimalFormat("#.##");
                List<UserVmVO> instances = managementServer.listUserVMsByHostId(hostVO.getId());
                for (UserVmVO vm : instances) {
                    ServiceOffering so = managementServer.findServiceOfferingById(vm.getServiceOfferingId());
                    cpu += so.getCpu() * so.getSpeed();
                }
                cpuAlloc = decimalFormat.format(((float) cpu / (float) (hostVO.getCpus() * hostVO.getSpeed())) * 100f) + "%";
                hostRO.setCpuAllocated(cpuAlloc);

                // calculate cpu utilized
                String cpuUsed = null;
                HostStats hostStats = managementServer.getHostStatistics(hostVO.getId());
                if (hostStats != null) {
                    float cpuUtil = (float) hostStats.getCpuUtilization();
                    cpuUsed = decimalFormat.format(cpuUtil) + "%";
                    hostRO.setCpuUsed(cpuUsed);
                    
                    long avgLoad = (long)hostStats.getAverageLoad();
                    hostRO.setAverageLoad(avgLoad);
                    
                    long networkKbsRead = (long)hostStats.getNetworkReadKBs();
                    hostRO.setNetworkKbsRead(networkKbsRead);
                    
                    long networkKbsWrite = (long)hostStats.getNetworkWriteKBs();
                    hostRO.setNetworkKbsWrite(networkKbsWrite);
                }
            }

            if ( hostVO.getType() == Host.Type.Routing ) {
                Long memory = hostVO.getTotalMemory();
                hostRO.setTotalMemory(memory);
                // calculate memory allocated by systemVM and userVm
                long mem = managementServer.getMemoryUsagebyHost(hostVO.getId());
                hostRO.setMemoryAllocated(mem);
                // calculate memory utilized, we don't provide memory over commit
                hostRO.setMemoryUsed(mem);

            }
            if (hostVO.getType().toString().equals("Storage")) {
                hostRO.setDiskSizeTotal(hostVO.getTotalSize());
                hostRO.setDiskSizeAllocated(0);
            }
            hostRO.setCaps(hostVO.getCapabilities());
            hostRO.setLastPinged(hostVO.getLastPinged());
            if (hostVO.getManagementServerId() != null) {
                hostRO.setManagementServerId(hostVO.getManagementServerId());
            }

            if (hostVO.getCreated() != null) {
                hostRO.setCreated(hostVO.getCreated());
            }
            if (hostVO.getRemoved() != null) {
                hostRO.setRemoved(hostVO.getRemoved());
            }
	    	
    		Set<Event> possibleEvents = hostVO.getStatus().getPossibleEvents();
    		hostRO.setEvents(possibleEvents);
			
    		return hostRO;
	    	
	    }
}
