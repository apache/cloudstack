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

import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.HostResponse;
import com.cloud.api.response.StoragePoolResponse;
import com.cloud.dc.ClusterVO;
import com.cloud.host.Host;
import com.cloud.host.HostStats;
import com.cloud.host.HostVO;
import com.cloud.host.Status.Event;
import com.cloud.offering.ServiceOffering;
import com.cloud.serializer.SerializerHelper;
import com.cloud.storage.GuestOSCategoryVO;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.StorageStats;
import com.cloud.vm.UserVmVO;

@Implementation(method="")
public class ListStoragePoolsAndHostsCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListStoragePoolsAndHostsCmd.class.getName());

    private static final String s_name = "liststoragepoolsandhostsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="ipaddress", type=CommandType.STRING)
    private String ipAddress;

    @Parameter(name="name", type=CommandType.STRING)
    private String storagePoolName;

    @Parameter(name="path", type=CommandType.STRING)
    private String path;

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

    public String getIpAddress() {
        return ipAddress;
    }

    public String getStoragePoolName() {
        return storagePoolName;
    }

    public String getPath() {
        return path;
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
        List<Object> poolsAndHosts = (List<Object>)getResponseObject();

        List<Object> response = new ArrayList<Object>();
        for (Object poolOrHost : poolsAndHosts) {
            if (poolOrHost instanceof StoragePoolVO) {
                StoragePoolVO pool = (StoragePoolVO)poolOrHost;
                response.add(constructStoragePoolResponse(pool));
            } else if (poolOrHost instanceof HostVO) {
                HostVO host = (HostVO)poolOrHost;
                response.add(constructHostResponse(host));
            }
        }

        return SerializerHelper.toSerializedString(response);
    }

    private StoragePoolResponse constructStoragePoolResponse(StoragePoolVO pool) {
        StoragePoolResponse poolResponse = new StoragePoolResponse();
        poolResponse.setId(pool.getId());
        poolResponse.setName(pool.getName());
        poolResponse.setPath(pool.getPath());
        poolResponse.setIpAddress(pool.getHostAddress());
        poolResponse.setZoneId(pool.getDataCenterId());
        poolResponse.setZoneName(ApiDBUtils.findZoneById(pool.getDataCenterId()).getName());
        if (pool.getPoolType() != null) {
            poolResponse.setType(pool.getPoolType().toString());
        }
        if (pool.getPodId() != null) {
            poolResponse.setPodId(pool.getPodId());
            poolResponse.setPodName(ApiDBUtils.findPodById(pool.getPodId()).getName());
        }
        if (pool.getCreated() != null) {
            poolResponse.setCreated(pool.getCreated());
        }

        StorageStats stats = ApiDBUtils.getStoragePoolStatistics(pool.getId());
        long capacity = pool.getCapacityBytes();
        long available = pool.getAvailableBytes() ;
        long used = capacity - available;

        if (stats != null) {
            used = stats.getByteUsed();
            available = capacity - used;
        }

        poolResponse.setDiskSizeTotal(pool.getCapacityBytes());
        poolResponse.setDiskSizeAllocated(used);

        if (pool.getClusterId() != null) {
            ClusterVO cluster = ApiDBUtils.findClusterById(pool.getClusterId());
            poolResponse.setClusterId(cluster.getId());
            poolResponse.setClusterName(cluster.getName());
        }           

        poolResponse.setTags(ApiDBUtils.getStoragePoolTags(pool.getId()));

        return poolResponse;
    }

    private HostResponse constructHostResponse(HostVO host) {
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
        GuestOSCategoryVO guestOSCategory = ApiDBUtils.getHostGuestOSCategory(host.getId());
        if (guestOSCategory != null) {
            hostResponse.setOsCategoryId(guestOSCategory.getId());
            hostResponse.setOsCategoryName(guestOSCategory.getName());
        }
        hostResponse.setZoneName(ApiDBUtils.findZoneById(host.getDataCenterId()).getName());
        hostResponse.setPodName(ApiDBUtils.findPodById(host.getPodId()).getName());

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
        hostResponse.setCpuAllocated(cpuAlloc);

        // calculate cpu utilized
        String cpuUsed = null;
        HostStats hostStats = ApiDBUtils.getHostStatistics(host.getId());
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
            long mem = ApiDBUtils.getMemoryUsagebyHost(host.getId());
            hostResponse.setMemoryAllocated(mem);
            hostResponse.setMemoryUsed(mem);
        } else if (host.getType().toString().equals("Storage")) {
            hostResponse.setDiskSizeTotal(host.getTotalSize());
            hostResponse.setDiskSizeAllocated(0L);
        }

        if (host.getClusterId() != null) {
            ClusterVO cluster = ApiDBUtils.findClusterById(host.getClusterId());
            hostResponse.setClusterName(cluster.getName());
        }

        hostResponse.setLocalStorageActive(ApiDBUtils.isLocalStorageActiveOnHost(host));

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

        return hostResponse;
    }
}
