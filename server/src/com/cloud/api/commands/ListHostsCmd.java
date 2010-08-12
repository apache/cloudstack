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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.dc.ClusterVO;
import com.cloud.host.Host;
import com.cloud.host.HostStats;
import com.cloud.host.HostVO;
import com.cloud.host.Status.Event;
import com.cloud.server.Criteria;
import com.cloud.service.ServiceOffering;
import com.cloud.storage.GuestOSCategoryVO;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.vm.UserVmVO;
//import com.cloud.vm.HostStats;

public class ListHostsCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(ListHostsCmd.class.getName());

    private static final String s_name = "listhostsresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.NAME, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ZONE_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.POD_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.TYPE, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.KEYWORD, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.STATE, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PAGE, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PAGESIZE, Boolean.FALSE));
    }

    public String getName() {
        return s_name;
    }

    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    public List<Pair<String, Object>> execute(Map<String, Object> params) {
        Long id = (Long) params.get(BaseCmd.Properties.ID.getName());
        String name = (String) params.get(BaseCmd.Properties.NAME.getName());
        Long zoneId = (Long) params.get(BaseCmd.Properties.ZONE_ID.getName());
        Long podId = (Long) params.get(BaseCmd.Properties.POD_ID.getName());
        String type = (String) params.get(BaseCmd.Properties.TYPE.getName());
        String state = (String) params.get(BaseCmd.Properties.STATE.getName());
        String keyword = (String) params.get(BaseCmd.Properties.KEYWORD.getName());
        Integer page = (Integer) params.get(BaseCmd.Properties.PAGE.getName());
        Integer pageSize = (Integer) params.get(BaseCmd.Properties.PAGESIZE.getName());

        Long startIndex = Long.valueOf(0);
        int pageSizeNum = 50;
        if (pageSize != null) {
            pageSizeNum = pageSize.intValue();
        }
        if (page != null) {
            int pageNum = page.intValue();
            if (pageNum > 0) {
                startIndex = Long.valueOf(pageSizeNum * (pageNum - 1));
            }
        }
        Criteria c = new Criteria("id", Boolean.TRUE, startIndex, Long.valueOf(pageSizeNum));
        c.addCriteria(Criteria.KEYWORD, keyword);
        c.addCriteria(Criteria.ID, id);
        c.addCriteria(Criteria.NAME, name);
        c.addCriteria(Criteria.DATACENTERID, zoneId);
        c.addCriteria(Criteria.PODID, podId);
        c.addCriteria(Criteria.TYPE, type);
        c.addCriteria(Criteria.STATE, state);

        List<HostVO> servers = getManagementServer().searchForServers(c);

        if (servers == null) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "unable to find servers");
        }

        List<Pair<String, Object>> serverTags = new ArrayList<Pair<String, Object>>();
        Object[] sTag = new Object[servers.size()];
        int i = 0;
        for (HostVO server : servers) {
            List<Pair<String, Object>> serverData = new ArrayList<Pair<String, Object>>();
            serverData.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), server.getId().toString()));
            serverData.add(new Pair<String, Object>(BaseCmd.Properties.NAME.getName(), server.getName()));
            if (server.getStatus() != null) {
                serverData.add(new Pair<String, Object>(BaseCmd.Properties.STATE.getName(), server.getStatus().toString()));
            }
            if (server.getDisconnectedOn() != null) {
                serverData.add(new Pair<String, Object>(BaseCmd.Properties.DISCONNECTED.getName(), getDateString(server.getDisconnectedOn())));
            }
            if (server.getType() != null) {
                serverData.add(new Pair<String, Object>(BaseCmd.Properties.TYPE.getName(), server.getType().toString()));
            }
            
            GuestOSCategoryVO guestOSCategory = getManagementServer().getHostGuestOSCategory(server.getId());
            if (guestOSCategory != null) {
            	serverData.add(new Pair<String, Object>(BaseCmd.Properties.OS_CATEGORY_ID.getName(), guestOSCategory.getId()));
            	serverData.add(new Pair<String, Object>(BaseCmd.Properties.OS_CATEGORY_NAME.getName(), guestOSCategory.getName()));
            }
            
            serverData.add(new Pair<String, Object>(BaseCmd.Properties.IP_ADDRESS.getName(), server.getPrivateIpAddress()));
            serverData.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_ID.getName(), Long.valueOf(server.getDataCenterId()).toString()));
            serverData.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_NAME.getName(), getManagementServer().getDataCenterBy(server.getDataCenterId()).getName()));
            if (server.getPodId() != null && getManagementServer().findHostPodById(server.getPodId()) != null) {
                serverData.add(new Pair<String, Object>(BaseCmd.Properties.POD_ID.getName(), server.getPodId().toString()));
                serverData.add(new Pair<String, Object>(BaseCmd.Properties.POD_NAME.getName(), getManagementServer().findHostPodById(server.getPodId()).getName()));
            }
            serverData.add(new Pair<String, Object>(BaseCmd.Properties.VERSION.getName(), server.getVersion().toString()));
            if (server.getHypervisorType() != null) {
                serverData.add(new Pair<String, Object>(BaseCmd.Properties.HYPERVISOR.getName(), server.getHypervisorType().toString()));
            }

            if ((server.getCpus() != null) && (server.getSpeed() != null) && !(server.getType().toString().equals("Storage"))) {
                serverData.add(new Pair<String, Object>(BaseCmd.Properties.CPU_NUMBER.getName(), server.getCpus().toString()));
                serverData.add(new Pair<String, Object>(BaseCmd.Properties.CPU_SPEED.getName(), server.getSpeed().toString()));
                // calculate cpu allocated by vm
                int cpu = 0;
                String cpuAlloc = null;
                DecimalFormat decimalFormat = new DecimalFormat("#.##");
                List<UserVmVO> instances = getManagementServer().listUserVMsByHostId(server.getId());
                for (UserVmVO vm : instances) {
                    ServiceOffering so = getManagementServer().findServiceOfferingById(vm.getServiceOfferingId());
                    cpu += so.getCpu() * so.getSpeed();
                }
                cpuAlloc = decimalFormat.format(((float) cpu / (float) (server.getCpus() * server.getSpeed())) * 100f) + "%";
                serverData.add(new Pair<String, Object>(BaseCmd.Properties.CPU_ALLOCATED.getName(), cpuAlloc));

                // calculate cpu utilized
                String cpuUsed = null;
                HostStats hostStats = getManagementServer().getHostStatistics(server.getId());
                if (hostStats != null) {
                    float cpuUtil = (float) hostStats.getCpuUtilization();
                    cpuUsed = decimalFormat.format(cpuUtil) + "%";
                    serverData.add(new Pair<String, Object>(BaseCmd.Properties.CPU_USED.getName(), cpuUsed));
                    
                    long avgLoad = (long)hostStats.getAverageLoad();
                    serverData.add(new Pair<String, Object>(BaseCmd.Properties.AVERAGE_LOAD.getName(), avgLoad));
                    
                    long networkKbRead = (long)hostStats.getNetworkReadKBs();
                    serverData.add(new Pair<String, Object>(BaseCmd.Properties.NETWORK_KB_READ.getName(), networkKbRead));
                    
                    long networkKbWrite = (long)hostStats.getNetworkWriteKBs();
                    serverData.add(new Pair<String, Object>(BaseCmd.Properties.NETWORK_KB_WRITE.getName(), networkKbWrite));
                }
            }
            if (server.getType() == Host.Type.Routing) {
                Long memory = server.getTotalMemory();
                serverData.add(new Pair<String, Object>(BaseCmd.Properties.MEMORY_TOTAL.getName(), memory.toString()));
                
                // calculate memory allocated by systemVM and userVm
                long mem = getManagementServer().getMemoryUsagebyHost(server.getId());
                serverData.add(new Pair<String, Object>(BaseCmd.Properties.MEMORY_ALLOCATED.getName(), Long.valueOf(mem).toString()));
                
                // calculate memory utilized, we don't provide memory over commit
                serverData.add(new Pair<String, Object>(BaseCmd.Properties.MEMORY_USED.getName(), mem));                
            }
            if (server.getType().toString().equals("Storage")) {
                serverData.add(new Pair<String, Object>(BaseCmd.Properties.DISK_SIZE_TOTAL.getName(), Long.valueOf(server.getTotalSize()).toString()));
                serverData.add(new Pair<String, Object>(BaseCmd.Properties.DISK_SIZE_ALLOCATED.getName(), Long.valueOf(0).toString()));
            }
            serverData.add(new Pair<String, Object>(BaseCmd.Properties.CAPABILITIES.getName(), server.getCapabilities()));
            serverData.add(new Pair<String, Object>(BaseCmd.Properties.LASTPINGED.getName(), Long.valueOf(server.getLastPinged()).toString()));
            if (server.getManagementServerId() != null) {
                serverData.add(new Pair<String, Object>(BaseCmd.Properties.M_SERVER_ID.getName(), server.getManagementServerId().toString()));
            }
            
            if (server.getClusterId() != null) {
            	ClusterVO cluster = getManagementServer().findClusterById(server.getClusterId());
            	serverData.add(new Pair<String, Object>(BaseCmd.Properties.CLUSTER_ID.getName(), cluster.getId()));
            	serverData.add(new Pair<String, Object>(BaseCmd.Properties.CLUSTER_NAME.getName(), cluster.getName()));
            }
            
            serverData.add(new Pair<String, Object>(BaseCmd.Properties.IS_LOCAL_STORAGE_ACTIVE.getName(), getManagementServer().isLocalStorageActiveOnHost(server)));

            if (server.getCreated() != null) {
                serverData.add(new Pair<String, Object>(BaseCmd.Properties.CREATED.getName(), getDateString(server.getCreated())));
            }
            if (server.getRemoved() != null) {
                serverData.add(new Pair<String, Object>(BaseCmd.Properties.REMOVED.getName(), getDateString(server.getRemoved())));
            }

    		Set<Event> possibleEvents = server.getStatus().getPossibleEvents();
    		for(Event event:possibleEvents)
    		{
    			serverData.add(new Pair<String, Object>(BaseCmd.Properties.EVENTS.getName(),event.toString()));
    		}
            

            sTag[i++] = serverData;
        }
        Pair<String, Object> serverTag = new Pair<String, Object>("host", sTag);
        serverTags.add(serverTag);
        return serverTags;
    }
}
