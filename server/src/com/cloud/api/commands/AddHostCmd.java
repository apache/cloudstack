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

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.host.Host;
import com.cloud.host.HostStats;
import com.cloud.service.ServiceOffering;
import com.cloud.storage.GuestOSCategoryVO;
import com.cloud.utils.Pair;
import com.cloud.vm.UserVmVO;

public class AddHostCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(AddHostCmd.class.getName());
    private static final String s_name = "addhostresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ZONE_ID, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.POD_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.CLUSTER_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.CLUSTER_NAME, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.URL, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.USERNAME, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PASSWORD, Boolean.TRUE));
    }

    @Override
    public String getName() {
        return s_name;
    }
    @Override
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params)
    {
        Long zoneId = (Long)params.get(BaseCmd.Properties.ZONE_ID.getName());
        Long podId = (Long)params.get(BaseCmd.Properties.POD_ID.getName());
        String url = (String)params.get(BaseCmd.Properties.URL.getName());
        String username = (String)params.get(BaseCmd.Properties.USERNAME.getName());
        String password = (String)params.get(BaseCmd.Properties.PASSWORD.getName());
        Long clusterId = (Long)params.get(BaseCmd.Properties.CLUSTER_ID.getName());
        String clusterName = (String)params.get(BaseCmd.Properties.CLUSTER_NAME.getName());
        
        if (clusterName != null && clusterId != null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Can't specify cluster by both id and name");
        }
        
        if ((clusterName != null || clusterId != null) && podId == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Can't specify cluster without specifying the pod");
        }
        
        //Check if the zone exists in the system
        if (getManagementServer().findDataCenterById(zoneId) == null ){
        	throw new ServerApiException(BaseCmd.PARAM_ERROR, "Can't find zone by id " + zoneId);
        }

        //Check if the pod exists in the system
        if (podId != null) {
            if (getManagementServer().findHostPodById(podId) == null ){
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Can't find pod by id " + podId);
            }
            //check if pod belongs to the zone
            HostPodVO pod = getManagementServer().findHostPodById(podId);
            if (!Long.valueOf(pod.getDataCenterId()).equals(zoneId)) {
            	throw new ServerApiException(BaseCmd.PARAM_ERROR, "Pod " + podId + " doesn't belong to the zone " + zoneId);
            }
        }

        if (clusterId != null) {
            if (getManagementServer().findClusterById(clusterId) == null) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Can't find cluster by id " + clusterId);
            }
        }
        
        
        boolean success = false;
        List<Pair<String, Object>> serverTags = new ArrayList<Pair<String, Object>>();

        try
        {
            if (clusterName != null) {
                ClusterVO cluster = getManagementServer().createCluster(zoneId, podId, clusterName);
                clusterId = cluster.getId();
            }
            
        	List<? extends Host> h = getManagementServer().discoverHosts(zoneId, podId, clusterId, url, username, password);
        	success = !h.isEmpty();

        	if(success)
        	{
                Object[] sTag = new Object[h.size()];
                int i=0;
	        	for(Host host:h)
	        	{
	                List<Pair<String, Object>> serverData = new ArrayList<Pair<String, Object>>();
	                serverData.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), host.getId().toString()));
	                serverData.add(new Pair<String, Object>(BaseCmd.Properties.NAME.getName(), host.getName()));
	                if (host.getStatus() != null) {
	                    serverData.add(new Pair<String, Object>(BaseCmd.Properties.STATE.getName(), host.getStatus().toString()));
	                }
	                if (host.getDisconnectedOn() != null) {
	                    serverData.add(new Pair<String, Object>(BaseCmd.Properties.DISCONNECTED.getName(), getDateString(host.getDisconnectedOn())));
	                }
	                if (host.getType() != null) {
	                    serverData.add(new Pair<String, Object>(BaseCmd.Properties.TYPE.getName(), host.getType().toString()));
	                }
	                
	                GuestOSCategoryVO guestOSCategory = getManagementServer().getHostGuestOSCategory(host.getId());
	                if (guestOSCategory != null) {
	                	serverData.add(new Pair<String, Object>(BaseCmd.Properties.OS_CATEGORY_ID.getName(), guestOSCategory.getId()));
	                	serverData.add(new Pair<String, Object>(BaseCmd.Properties.OS_CATEGORY_NAME.getName(), guestOSCategory.getName()));
	                }
	                
	                serverData.add(new Pair<String, Object>(BaseCmd.Properties.IP_ADDRESS.getName(), host.getPrivateIpAddress()));
	                serverData.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_ID.getName(), Long.valueOf(host.getDataCenterId()).toString()));
	                serverData.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_NAME.getName(), getManagementServer().getDataCenterBy(host.getDataCenterId()).getName()));
	                if (host.getPodId() != null && getManagementServer().findHostPodById(host.getPodId()) != null) {
	                    serverData.add(new Pair<String, Object>(BaseCmd.Properties.POD_ID.getName(), host.getPodId().toString()));
	                    serverData.add(new Pair<String, Object>(BaseCmd.Properties.POD_NAME.getName(), getManagementServer().findHostPodById(host.getPodId()).getName()));
	                }
	                serverData.add(new Pair<String, Object>(BaseCmd.Properties.VERSION.getName(), host.getVersion().toString()));
	                if (host.getHypervisorType() != null) {
	                    serverData.add(new Pair<String, Object>(BaseCmd.Properties.HYPERVISOR.getName(), host.getHypervisorType().toString()));
	                }
	
	                if ((host.getCpus() != null) && (host.getSpeed() != null) && !(host.getType().toString().equals("Storage"))) {
	                    serverData.add(new Pair<String, Object>(BaseCmd.Properties.CPU_NUMBER.getName(), host.getCpus().toString()));
	                    serverData.add(new Pair<String, Object>(BaseCmd.Properties.CPU_SPEED.getName(), host.getSpeed().toString()));
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
	                    serverData.add(new Pair<String, Object>(BaseCmd.Properties.CPU_ALLOCATED.getName(), cpuAlloc));
	
	                    // calculate cpu utilized
	                    String cpuUsed = null;
	                    HostStats hostStats = getManagementServer().getHostStatistics(host.getId());
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
	                if (host.getType() == Host.Type.Routing) {
	                    Long memory = host.getTotalMemory();
	                    serverData.add(new Pair<String, Object>(BaseCmd.Properties.MEMORY_TOTAL.getName(), memory.toString()));
	                    
	                    // calculate memory allocated by domR and userVm
	                    long mem = getManagementServer().getMemoryUsagebyHost(host.getId());
	                    serverData.add(new Pair<String, Object>(BaseCmd.Properties.MEMORY_ALLOCATED.getName(), Long.valueOf(mem).toString()));
	                    
	                    // calculate memory utilized, we don't provide memory over commit
	                    serverData.add(new Pair<String, Object>(BaseCmd.Properties.MEMORY_USED.getName(), mem));
	    
	                }
	                if (host.getType().toString().equals("Storage")) {
	                    serverData.add(new Pair<String, Object>(BaseCmd.Properties.DISK_SIZE_TOTAL.getName(), Long.valueOf(host.getTotalSize()).toString()));
	                    serverData.add(new Pair<String, Object>(BaseCmd.Properties.DISK_SIZE_ALLOCATED.getName(), Long.valueOf(0).toString()));
	                }
	                serverData.add(new Pair<String, Object>(BaseCmd.Properties.CAPABILITIES.getName(), host.getCapabilities()));
	                serverData.add(new Pair<String, Object>(BaseCmd.Properties.LASTPINGED.getName(), Long.valueOf(host.getLastPinged()).toString()));
	                if (host.getManagementServerId() != null) {
	                    serverData.add(new Pair<String, Object>(BaseCmd.Properties.M_SERVER_ID.getName(), host.getManagementServerId().toString()));
	                }
	                
	                if (host.getClusterId() != null) {
	                	ClusterVO cluster = getManagementServer().findClusterById(host.getClusterId());
	                	serverData.add(new Pair<String, Object>(BaseCmd.Properties.CLUSTER_ID.getName(), cluster.getId()));
	                	serverData.add(new Pair<String, Object>(BaseCmd.Properties.CLUSTER_NAME.getName(), cluster.getName()));
	                }
	
	                if (host.getCreated() != null) {
	                    serverData.add(new Pair<String, Object>(BaseCmd.Properties.CREATED.getName(), getDateString(host.getCreated())));
	                }
	                if (host.getRemoved() != null) {
	                    serverData.add(new Pair<String, Object>(BaseCmd.Properties.REMOVED.getName(), getDateString(host.getRemoved())));
	                }
	                sTag[i++] = serverData;
	        		
	        	}
	        	
	            Pair<String, Object> serverTag = new Pair<String, Object>("host", sTag);
	            serverTags.add(serverTag);
	            return serverTags;
        	}
        }
        catch (Exception ex)
        {
        	s_logger.error("Failed to add host: ", ex);
        	throw new ServerApiException(BaseCmd.INTERNAL_ERROR, ex.getMessage());
        }
		return serverTags;
    
    }
}
