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

package com.cloud.api.commands;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.HostResponse;
import com.cloud.dc.HostPodVO;
import com.cloud.event.EventTypes;
import com.cloud.host.Host;
import com.cloud.host.HostStats;
import com.cloud.host.HostVO;
import com.cloud.host.Status.Event;
import com.cloud.offering.ServiceOffering;
import com.cloud.storage.GuestOSCategoryVO;
import com.cloud.user.Account;
import com.cloud.user.UserContext;
import com.cloud.vm.UserVmVO;

@Implementation(method="cancelMaintenance", manager=Manager.AgentManager, description="Cancels host maintenance.")
public class CancelMaintenanceCmd extends BaseAsyncCmd  {
    public static final Logger s_logger = Logger.getLogger(CancelMaintenanceCmd.class.getName());

    private static final String s_name = "cancelhostmaintenanceresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="id", type=CommandType.LONG, required=true, description="the host ID")
    private Long id;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }
    
    public static String getResultObjectName() {
    	return "host";
    }

    @Override
    public long getAccountId() {
        Account account = (Account)UserContext.current().getAccountObject();
        if (account != null) {
            return account.getId();
        }

        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_MAINTENANCE_CANCEL;
    }

    @Override
    public String getEventDescription() {
        return  "canceling maintenance for host: " + getId();
    }

	@Override @SuppressWarnings("unchecked")
	public HostResponse getResponse() {
        HostVO host = (HostVO)getResponseObject();

        HostResponse response = new HostResponse();
        response.setId(host.getId());

        response.setName(host.getName());
        response.setState(host.getStatus());

        if (host.getDisconnectedOn() != null) {
            response.setDisconnectedOn(host.getDisconnectedOn());
        }

        if (host.getType() != null) {
            response.setHostType(host.getType());
        }

        GuestOSCategoryVO guestOSCategory = ApiDBUtils.getHostGuestOSCategory(host.getId());
        if (guestOSCategory != null) {
            response.setOsCategoryId(guestOSCategory.getId());
            response.setOsCategoryName(guestOSCategory.getName());
        }

        response.setIpAddress(host.getPrivateIpAddress());
        response.setZoneId(host.getDataCenterId());
        response.setZoneName(ApiDBUtils.findZoneById(host.getDataCenterId()).getName());

        if (host.getPodId() != null) {
            HostPodVO pod = ApiDBUtils.findPodById(host.getPodId());
            response.setPodId(host.getPodId());
            response.setPodName(pod.getName());
        }

        response.setVersion(host.getVersion().toString());

        if (host.getHypervisorType() != null) {
            response.setHypervisor(host.getHypervisorType());
        }

        if ((host.getCpus() != null) && (host.getSpeed() != null) && !(host.getType().toString().equals("Storage"))) {
            response.setCpuNumber(host.getCpus());
            response.setCpuSpeed(host.getSpeed());
            // calculate cpu allocated by vm
            int cpu = 0;
            String cpuAlloc = null;
            DecimalFormat decimalFormat = new DecimalFormat("#.##");
            List<UserVmVO> instances = ApiDBUtils.listUserVMsByHostId(host.getId());
            for (UserVmVO vm : instances) {
                ServiceOffering so = ApiDBUtils.findServiceOfferingById(vm.getServiceOfferingId());
                cpu += so.getCpu() * so.getSpeed();
            }
            cpuAlloc = decimalFormat.format(((float) cpu / (float) (host.getCpus() * host.getSpeed())) * 100f) + "%";
            response.setCpuAllocated(cpuAlloc);

            // calculate cpu utilized
            String cpuUsed = null;
            HostStats hostStats = ApiDBUtils.getHostStatistics(host.getId());
            if (hostStats != null) {
                float cpuUtil = (float) hostStats.getCpuUtilization();
                cpuUsed = decimalFormat.format(cpuUtil) + "%";
                response.setCpuUsed(cpuUsed);
                
                long avgLoad = (long)hostStats.getAverageLoad();
                response.setAverageLoad(avgLoad);
                
                long networkKbsRead = (long)hostStats.getNetworkReadKBs();
                response.setNetworkKbsRead(networkKbsRead);
                
                long networkKbsWrite = (long)hostStats.getNetworkWriteKBs();
                response.setNetworkKbsWrite(networkKbsWrite);
            }
        }

        if (host.getType() == Host.Type.Routing) {
            Long memory = host.getTotalMemory();
            response.setMemoryTotal(memory);
            // calculate memory allocated by systemVM and userVm
            long mem = ApiDBUtils.getMemoryUsagebyHost(host.getId());
            response.setMemoryAllocated(mem);
            // calculate memory utilized, we don't provide memory over commit
            response.setMemoryUsed(mem);

        }

        if (host.getType().toString().equals("Storage")) {
            response.setDiskSizeTotal(host.getTotalSize());
            response.setDiskSizeAllocated(0L);
        }
        response.setCapabilities(host.getCapabilities());
        response.setLastPinged(new Date(host.getLastPinged()));
        if (host.getManagementServerId() != null) {
            response.setManagementServerId(host.getManagementServerId());
        }

        if (host.getCreated() != null) {
            response.setCreated(host.getCreated());
        }
        if (host.getRemoved() != null) {
            response.setRemoved(host.getRemoved());
        }
        
        Set<Event> possibleEvents = host.getStatus().getPossibleEvents();
        if ((possibleEvents != null) && !possibleEvents.isEmpty()) {
            String events = "";
            Iterator<Event> iter = possibleEvents.iterator();
            while (iter.hasNext()) {
                Event event = iter.next();
                events += event.toString();
                if (iter.hasNext()) {
                    events += "; ";
                }
            }
            response.setEvents(events);
        }
        response.setResponseName(getName());
		return response;
	}
}
