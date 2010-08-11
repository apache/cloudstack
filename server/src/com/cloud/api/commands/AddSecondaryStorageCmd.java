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

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;

@Implementation(method="discoverHosts", manager=Manager.ManagementServer)
public class AddSecondaryStorageCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(AddSecondaryStorageCmd.class.getName());
    private static final String s_name = "addsecondarystorageresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="url", type=CommandType.STRING, required=true)
    private String url;

    @Parameter(name="zoneid", type=CommandType.LONG)
    private Long zoneId;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getUrl() {
        return url;
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

    /*
    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
        Long zoneId = (Long)params.get(BaseCmd.Properties.ZONE_ID.getName());
        String url = (String)params.get(BaseCmd.Properties.URL.getName());
        
        //Check if the zone exists in the system
        if (getManagementServer().findDataCenterById(zoneId) == null ){
        	throw new ServerApiException(BaseCmd.PARAM_ERROR, "Can't find zone by id " + zoneId);
        }
        
        // Check if a secondary storage host already exists in this zone
        if (getManagementServer().findSecondaryStorageHosT(zoneId) != null) {
        	throw new ServerApiException(BaseCmd.PARAM_ERROR, "A secondary storage host already exists in the specified zone.");
        }

        try {
    		URI uri = new URI(url);
    		if (uri.getScheme() == null)
    			throw new ServerApiException(BaseCmd.PARAM_ERROR, "uri.scheme is null " + url + ", add nfs:// as a prefix");
    		else if (uri.getScheme().equalsIgnoreCase("nfs")) {
    			if (uri.getHost() == null || uri.getHost().equalsIgnoreCase("") || uri.getPath() == null || uri.getPath().equalsIgnoreCase("")) {
    				throw new ServerApiException(BaseCmd.PARAM_ERROR, "Your host and/or path is wrong.  Make sure it's of the format nfs://hostname/path");
    			}
    		}
    	} catch (URISyntaxException e) {
			throw new ServerApiException(BaseCmd.PARAM_ERROR, url + " is not a valid uri");
    	}
    	
    	List<? extends Host> h = null;
        try {
        	h = getManagementServer().discoverHosts(zoneId, null, null, url, null, null);
        } catch (Exception ex) {
        	s_logger.error("Failed to add secondary storage: ", ex);
        	throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Can't add secondary storage with url " + url);
        }
        
        if (h == null || h.size()==0) {
        	s_logger.error("Failed to add secondary storage: ");
        	throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Can't add secondary storage with url " + url);
        }
    	
        List<Pair<String, Object>> serverTags = new ArrayList<Pair<String, Object>>();
        Object[] sTag = new Object[h.size()];
        int i = 0;
        for (Host server1 : h) {
        	HostVO server = (HostVO) server1;
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
            serverData.add(new Pair<String, Object>(BaseCmd.Properties.IP_ADDRESS.getName(), server.getPrivateIpAddress()));
            serverData.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_ID.getName(), Long.valueOf(server.getDataCenterId()).toString()));
            serverData.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_NAME.getName(), getManagementServer().getDataCenterBy(server.getDataCenterId()).getName()));
            if (server.getPodId() != null && getManagementServer().findHostPodById(server.getPodId()) != null) {
           	 serverData.add(new Pair<String, Object>(BaseCmd.Properties.POD_ID.getName(), server.getPodId().toString()));
	             serverData.add(new Pair<String, Object>(BaseCmd.Properties.POD_NAME.getName(), getManagementServer().findHostPodById(server.getPodId()).getName()));
            }
            serverData.add(new Pair<String, Object>(BaseCmd.Properties.VERSION.getName(), server.getVersion().toString()));
            serverData.add(new Pair<String, Object>(BaseCmd.Properties.HYPERVISOR.getName(), server.getHypervisorType().toString()));
            
            if ((server.getCpus() != null) && (server.getSpeed() != null) && !(server.getType().toString().equals("Storage"))) {
           	 serverData.add(new Pair<String, Object>(BaseCmd.Properties.CPU_NUMBER.getName(), server.getCpus().toString()));
           	 serverData.add(new Pair<String, Object>(BaseCmd.Properties.CPU_SPEED.getName(), server.getSpeed().toString()));
           	 //calculate cpu allocated by vm
           	 int cpu = 0;
           	 String cpuAlloc = null;
           	 DecimalFormat decimalFormat = new DecimalFormat("#.##");
           	 List<UserVmVO> instances = getManagementServer().listUserVMsByHostId(server.getId());
        		 for (UserVmVO vm : instances) {
                    ServiceOffering so = getManagementServer().findServiceOfferingById(vm.getServiceOfferingId());
                    cpu += so.getCpu() * so.getSpeed();
                }
        		cpuAlloc = decimalFormat.format(((float)cpu / (float)(server.getCpus() * server.getSpeed())) * 100f) + "%";
        		serverData.add(new Pair<String, Object>(BaseCmd.Properties.CPU_ALLOCATED.getName(), cpuAlloc));
        		
        		//calculate cpu utilized
        		String cpuUsed = null;
        		HostStats hostStats = getManagementServer().getHostStatistics(server.getId());
	       		if (hostStats != null) {
	       			float cpuUtil = (float)hostStats.getCpuUtilization();
	           		cpuUsed = decimalFormat.format(cpuUtil * 100) + "%";
	           		serverData.add(new Pair<String, Object>(BaseCmd.Properties.CPU_USED.getName(), cpuUsed));
	       		}
            }

            if (server.getType().toString().equals("Storage")){

           	serverData.add(new Pair<String, Object>(BaseCmd.Properties.DISK_SIZE_ALLOCATED.getName(), Long.valueOf(0).toString()));
            }
            serverData.add(new Pair<String, Object>(BaseCmd.Properties.CAPABILITIES.getName(), server.getCapabilities()));
            serverData.add(new Pair<String, Object>(BaseCmd.Properties.LASTPINGED.getName(), Long.valueOf(server.getLastPinged()).toString()));
            if (server.getManagementServerId() != null) {
           	 serverData.add(new Pair<String, Object>(BaseCmd.Properties.M_SERVER_ID.getName(), server.getManagementServerId().toString()));
            }
            
            if (server.getCreated() != null) {
           	 serverData.add(new Pair<String, Object>(BaseCmd.Properties.CREATED.getName(), getDateString(server.getCreated())));
            }
            if (server.getRemoved() != null) {
           	 serverData.add(new Pair<String, Object>(BaseCmd.Properties.REMOVED.getName(), getDateString(server.getRemoved())));
            }
            sTag[i++] = serverData;
        }
        Pair<String, Object> serverTag = new Pair<String, Object>("secondarystorage", sTag);
        serverTags.add(serverTag);
        return serverTags;
 
    }
    */
}
