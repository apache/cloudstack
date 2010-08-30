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

@Implementation(method="updateHost", manager=Manager.AgentManager)
public class UpdateHostCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateHostCmd.class.getName());
    private static final String s_name = "updatehostresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="id", type=CommandType.LONG, required=true)
    private Long id;

    @Parameter(name="oscategoryid", type=CommandType.LONG)
    private Long osCategoryId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Long getOsCategoryId() {
        return osCategoryId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }
   
    public static String getResultObjectName() {
    	return "updatehost";
    }

//    @Override
//    public List<Pair<String, Object>> execute(Map<String, Object> params) {
//        Long id = (Long)params.get(BaseCmd.Properties.ID.getName());
//        Long guestOSCategoryId = (Long)params.get(BaseCmd.Properties.OS_CATEGORY_ID.getName());
//        
//        if (guestOSCategoryId == null) {
//        	guestOSCategoryId = new Long(-1);
//        } 
//        
//        // Verify that the host exists
//    	HostVO host = getManagementServer().getHostBy(id);
//    	if (host == null) {
//    		throw new ServerApiException(BaseCmd.PARAM_ERROR, "Host with id " + id.toString() + " doesn't exist");
//    	}
//       
//        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
//        try {
//        	getManagementServer().updateHost(id, guestOSCategoryId);
//        } catch (Exception ex) {
//        	s_logger.error("Failed to update host: ", ex);
//        	throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to update host: " + ex.getMessage());
//        }
//        final StateMachine<Status, Event> sm = new StateMachine<Status, Event>();
//        returnValues.add(new Pair<String,Object> (BaseCmd.Properties.SUCCESS.getName(), "true"));
//        returnValues.add(new Pair<String,Object> (getResultObjectName(),composeResultObject(host,sm)));
//        return returnValues;
//    }
//    
//    	private HostResultObject composeResultObject(HostVO hostVO, StateMachine<Status, Event> sm)
//	    {
//	    	HostResultObject hostRO = new HostResultObject();
//
//	    	hostRO.setName(hostVO.getName());
//	    	hostRO.setState(hostVO.getStatus().toString());
//	    	
//	    	if(hostVO.getDisconnectedOn() != null)
//	    		hostRO.setDisconnected(hostVO.getDisconnectedOn());
//	    	
//	    	
//            if (hostVO.getType() != null) {
//                hostRO.setType(hostVO.getType().toString());
//            }
//            
//            GuestOSCategoryVO guestOSCategory = getManagementServer().getHostGuestOSCategory(hostVO.getId());
//            if (guestOSCategory != null) {
//            	hostRO.setOsCategoryId(guestOSCategory.getId());
//            	hostRO.setOsCategoryName(guestOSCategory.getName());
//            }
//	    	
//            
//            hostRO.setIpAddress(hostVO.getPrivateIpAddress());
//            hostRO.setZoneId(hostVO.getDataCenterId());
//            hostRO.setZoneName(getManagementServer().getDataCenterBy(hostVO.getDataCenterId()).getName());
//
//            if (hostVO.getPodId() != null && getManagementServer().findHostPodById(hostVO.getPodId()) != null) {
//            	hostRO.setPodId(hostVO.getPodId());
//            	hostRO.setPodName((getManagementServer().findHostPodById(hostVO.getPodId())).getName());
//            }
//            
//            hostRO.setVersion(hostVO.getVersion().toString());
//            
//            if (hostVO.getHypervisorType() != null) {
//                hostRO.setHypervisorType(hostVO.getHypervisorType().toString());
//            }
//	    	
//            if ((hostVO.getCpus() != null) && (hostVO.getSpeed() != null) && !(hostVO.getType().toString().equals("Storage"))) 
//            {
//            	
//                hostRO.setCpuNumber(hostVO.getCpus());
//                hostRO.setCpuSpeed(hostVO.getSpeed());
//                // calculate cpu allocated by vm
//                int cpu = 0;
//                String cpuAlloc = null;
//                DecimalFormat decimalFormat = new DecimalFormat("#.##");
//                List<UserVmVO> instances = getManagementServer().listUserVMsByHostId(hostVO.getId());
//                for (UserVmVO vm : instances) {
//                    ServiceOffering so = getManagementServer().findServiceOfferingById(vm.getServiceOfferingId());
//                    cpu += so.getCpu() * so.getSpeed();
//                }
//                cpuAlloc = decimalFormat.format(((float) cpu / (float) (hostVO.getCpus() * hostVO.getSpeed())) * 100f) + "%";
//                hostRO.setCpuAllocated(cpuAlloc);
//
//                // calculate cpu utilized
//                String cpuUsed = null;
//                HostStats hostStats = getManagementServer().getHostStatistics(hostVO.getId());
//                if (hostStats != null) {
//                    float cpuUtil = (float) hostStats.getCpuUtilization();
//                    cpuUsed = decimalFormat.format(cpuUtil) + "%";
//                    hostRO.setCpuUsed(cpuUsed);
//                    
//                    long avgLoad = (long)hostStats.getAverageLoad();
//                    hostRO.setAverageLoad(avgLoad);
//                    
//                    long networkKbsRead = (long)hostStats.getNetworkReadKBs();
//                    hostRO.setNetworkKbsRead(networkKbsRead);
//                    
//                    long networkKbsWrite = (long)hostStats.getNetworkWriteKBs();
//                    hostRO.setNetworkKbsWrite(networkKbsWrite);
//
//                }
//            }
//
//            if ( hostVO.getType() == Host.Type.Routing) {
//                Long memory = hostVO.getTotalMemory();
//                hostRO.setTotalMemory(memory);
//                // calculate memory allocated by systemVM and userVm
//                long mem = getManagementServer().getMemoryUsagebyHost(hostVO.getId());
//                hostRO.setMemoryAllocated(mem);
//                
//                // calculate memory utilized, we don't provide memory over commit
//                hostRO.setMemoryUsed(mem);             
//            }
//            if (hostVO.getType().toString().equals("Storage")) {
//                hostRO.setDiskSizeTotal(hostVO.getTotalSize());
//                hostRO.setDiskSizeAllocated(0);
//            }
//            hostRO.setCaps(hostVO.getCapabilities());
//            hostRO.setLastPinged(hostVO.getLastPinged());
//            if (hostVO.getManagementServerId() != null) {
//                hostRO.setManagementServerId(hostVO.getManagementServerId());
//            }
//
//            if (hostVO.getCreated() != null) {
//                hostRO.setCreated(hostVO.getCreated());
//            }
//            if (hostVO.getRemoved() != null) {
//                hostRO.setRemoved(hostVO.getRemoved());
//            }
//	    	
//    		Set<Event> possibleEvents = hostVO.getStatus().getPossibleEvents();
//    		hostRO.setEvents(possibleEvents);
//			
//    		return hostRO;
//	    	
//	    }
    
	@Override
	public String getResponse() {
		// TODO Implement getResponse method
		return null;
	}
}
