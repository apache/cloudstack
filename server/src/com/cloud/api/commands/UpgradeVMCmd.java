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

import org.apache.log4j.Logger;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseCmd;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.UserVmResponse;
import com.cloud.offering.ServiceOffering;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.user.Account;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VmStats;

@Implementation(method="upgradeVirtualMachine", manager=Manager.UserVmManager, description="Changes the service offering for a virtual machine. " +
																							"The virtual machine must be in a \"Stopped\" state for " +
																							"this command to take effect.")
public class UpgradeVMCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(UpgradeVMCmd.class.getName());
    private static final String s_name = "changeserviceforvirtualmachineresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="id", type=CommandType.LONG, required=true, description="The ID of the virtual machine")
    private Long id;

    @Parameter(name="serviceofferingid", type=CommandType.LONG, required=true, description="the service offering ID to apply to the virtual machine")
    private Long serviceOfferingId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Long getServiceOfferingId() {
        return serviceOfferingId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

    public static String getResultObjectName() {
    	return "virtualmachine";
    }
    
    @Override @SuppressWarnings("unchecked")
    public UserVmResponse getResponse() {
        UserVmVO userVm = (UserVmVO)getResponseObject();

        UserVmResponse response = new UserVmResponse();
        if (userVm != null) {
    		Account acct = ApiDBUtils.findAccountById(userVm.getAccountId());
    		response.setId(userVm.getId());
    		response.setAccountName(acct.getAccountName());

    		ServiceOffering offering = ApiDBUtils.findServiceOfferingById(userVm.getServiceOfferingId());
    		response.setCpuSpeed(offering.getSpeed());
    		response.setMemory(offering.getRamSize());
    		if (((ServiceOfferingVO)offering).getDisplayText() != null) {
    		    response.setServiceOfferingName(((ServiceOfferingVO)offering).getDisplayText());
    		} else {
    		    response.setServiceOfferingName(offering.getName());
    		}

    		response.setServiceOfferingId(userVm.getServiceOfferingId());

            //stats calculation
            DecimalFormat decimalFormat = new DecimalFormat("#.##");
            String cpuUsed = null;
    		VmStats vmStats = ApiDBUtils.getVmStatistics(userVm.getId());
    		if (vmStats != null) {
                float cpuUtil = (float) vmStats.getCPUUtilization();
                cpuUsed = decimalFormat.format(cpuUtil) + "%";
                response.setCpuUsed(cpuUsed);

                long networkKbRead = (long)vmStats.getNetworkReadKBs();
                response.setNetworkKbsRead(networkKbRead);
                
                long networkKbWrite = (long)vmStats.getNetworkWriteKBs();
                response.setNetworkKbsWrite(networkKbWrite);
    		}
    		
    		response.setCreated(userVm.getCreated());
    		response.setDisplayName(userVm.getDisplayName());
    		response.setDomainName(ApiDBUtils.findDomainById(acct.getDomainId()).getName());
    		response.setDomainId(acct.getDomainId());
    		response.setHaEnable(userVm.isHaEnabled());

    		if (userVm.getHostId() != null) {
    		    response.setHostId(userVm.getHostId());
    			response.setHostName(ApiDBUtils.findHostById(userVm.getHostId()).getName());
    		}
    		response.setIpAddress(userVm.getPrivateIpAddress());
    		response.setName(userVm.getName());
    		response.setState(userVm.getState().toString());
    		response.setZoneId(userVm.getDataCenterId());
    		response.setZoneName(ApiDBUtils.findZoneById(userVm.getDataCenterId()).getName());
    		
    		VMTemplateVO template = ApiDBUtils.findTemplateById(userVm.getTemplateId());
    		response.setPasswordEnabled(template.getEnablePassword());
    		response.setTemplateDisplayText(template.getDisplayText());
    		response.setTemplateId(template.getId());
    		response.setTemplateName(template.getName());
        } else {
        	throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to update zone; internal error.");
        }

        response.setResponseName(getName());
        return response;
    }
}
