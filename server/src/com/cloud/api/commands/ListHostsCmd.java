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
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.HostResponse;
import com.cloud.dc.ClusterVO;
import com.cloud.host.Host;
import com.cloud.host.HostStats;
import com.cloud.host.HostVO;
import com.cloud.host.Status.Event;
import com.cloud.offering.ServiceOffering;
import com.cloud.serializer.SerializerHelper;
import com.cloud.storage.GuestOSCategoryVO;
import com.cloud.utils.Pair;
import com.cloud.vm.UserVmVO;

@Implementation(method="searchForServers")
public class ListHostsCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListHostsCmd.class.getName());

    private static final String s_name = "listhostsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="clusterid", type=CommandType.LONG)
    private Long clusterId;

    @Parameter(name="id", type=CommandType.LONG)
    private Long id;

    @Parameter(name="name", type=CommandType.STRING)
    private String hostName;

    @Parameter(name="podid", type=CommandType.LONG)
    private Long podId;

    @Parameter(name="state", type=CommandType.STRING)
    private String state;

    @Parameter(name="type", type=CommandType.STRING)
    private String type;

    @Parameter(name="zoneid", type=CommandType.LONG)
    private Long zoneId;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getClusterId() {
        return clusterId;
    }

    public Long getId() {
        return id;
    }

    public String getHostName() {
        return hostName;
    }

    public Long getPodId() {
        return podId;
    }

    public String getState() {
        return state;
    }

    public String getType() {
        return type;
    }

    public Long getZoneId() {
        return zoneId;
    }


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

    @Override @SuppressWarnings("unchecked")
    public String getResponse() {
        List<HostVO> hosts = (List<HostVO>)getResponseObject();

        List<HostResponse> response = new ArrayList<HostResponse>();
        for (HostVO host : hosts) {
            HostResponse hostResponse = new HostResponse();
            hostResponse.setId(host.getId());
            hostResponse.setCapabilities(host.getCapabilities());
            hostResponse.setClusterId(host.getClusterId());
            hostResponse.setCpuNumber(host.getCpus());
            hostResponse.setZoneId(host.getDataCenterId());
            hostResponse.setDisconnectedOn(host.getDisconnectedOn());
            hostResponse.setHypervisor(host.getHypervisorType());
            hostResponse.setHostType(host.getType());
            hostResponse.setLastPinged(new Date(host.getLastPinged()));
            hostResponse.setManagementServerId(host.getManagementServerId());
            hostResponse.setName(host.getName());
            hostResponse.setPodId(host.getPodId());
            hostResponse.setRemoved(host.getRemoved());
            hostResponse.setCpuSpeed(host.getSpeed());
            hostResponse.setState(host.getStatus());
            hostResponse.setIpAddress(host.getPrivateIpAddress());
            hostResponse.setVersion(host.getVersion());

            // TODO:  implement
            GuestOSCategoryVO guestOSCategory = getManagementServer().getHostGuestOSCategory(host.getId());
            if (guestOSCategory != null) {
                hostResponse.setOsCategoryId(guestOSCategory.getId());
                hostResponse.setOsCategoryName(guestOSCategory.getName());
            }
            hostResponse.setZoneName(getManagementServer().getDataCenterBy(host.getDataCenterId()).getName());
            hostResponse.setPodName(getManagementServer().findHostPodById(host.getPodId()).getName());

            // calculate cpu allocated by vm
            int cpu = 0;
            String cpuAlloc = null;
            DecimalFormat decimalFormat = new DecimalFormat("#.##");
            List<UserVmVO> instances = getManagementServer().listUserVMsByHostId(host.getId());
            for (UserVmVO vm : instances) {
                ServiceOffering so = getManagementServer().findServiceOfferingById(vm.getServiceOfferingId());
                cpu += so.getCpu() * so.getSpeed();
            }
            cpuAlloc = decimalFormat.format(((float) cpu / (float) (host.getCpus() * host.getSpeed())) * 100f) + "%";
            hostResponse.setCpuAllocated(cpuAlloc);

            // calculate cpu utilized
            String cpuUsed = null;
            HostStats hostStats = getManagementServer().getHostStatistics(host.getId());
            if (hostStats != null) {
                float cpuUtil = (float) hostStats.getCpuUtilization();
                cpuUsed = decimalFormat.format(cpuUtil) + "%";
                hostResponse.setCpuUsed(cpuUsed);
                hostResponse.setAverageLoad((long)hostStats.getAverageLoad());
                hostResponse.setNetworkKbsRead((long)hostStats.getNetworkReadKBs());
                hostResponse.setNetworkKbsWrite((long)hostStats.getNetworkWriteKBs());
            }

            if (host.getType() == Host.Type.Routing) {
                hostResponse.setMemoryTotal(host.getTotalMemory());
                
                // calculate memory allocated by systemVM and userVm
                long mem = getManagementServer().getMemoryUsagebyHost(host.getId());
                hostResponse.setMemoryAllocated(mem);
                hostResponse.setMemoryUsed(mem);
            } else if (host.getType().toString().equals("Storage")) {
                hostResponse.setDiskSizeTotal(host.getTotalSize());
                hostResponse.setDiskSizeAllocated(0L);
            }

            if (host.getClusterId() != null) {
                ClusterVO cluster = getManagementServer().findClusterById(host.getClusterId());
                hostResponse.setClusterName(cluster.getName());
            }

            hostResponse.setLocalStorageActive(getManagementServer().isLocalStorageActiveOnHost(host));

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
                hostResponse.setEvents(events);
            }

            response.add(hostResponse);
        }

        return SerializerHelper.toSerializedString(response);
    }
}
