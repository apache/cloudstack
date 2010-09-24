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

import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseCmd;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ResponseObject;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.UpgradeVmResponse;
import com.cloud.offering.ServiceOffering;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.user.Account;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VmStats;

@Implementation(method="upgradeVirtualMachine", manager=Manager.UserVmManager)
public class UpgradeVMCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(UpgradeVMCmd.class.getName());
    private static final String s_name = "changeserviceforvirtualmachineresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="id", type=CommandType.LONG, required=true)
    private Long id;

    @Parameter(name="serviceofferingid", type=CommandType.LONG, required=true)
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
    
    @Override
    public ResponseObject getResponse() {
        UserVmVO userVm = (UserVmVO)getResponseObject();

        UpgradeVmResponse response = new UpgradeVmResponse();
        if (userVm != null) {
    		Account acct = ApiDBUtils.findAccountById(userVm.getAccountId());
    		response.setAccount(acct.getAccountName());

    		ServiceOffering offering = ApiDBUtils.findServiceOfferingById(userVm.getServiceOfferingId());
    		response.setCpuSpeed(offering.getSpeed());
    		response.setMemory(offering.getRamSize());
    		if (((ServiceOfferingVO)offering).getDisplayText() != null) {
    		    response.setServiceOfferingName(((ServiceOfferingVO)offering).getDisplayText());
    		} else {
    		    response.setServiceOfferingName(offering.getName());
    		}

    		response.setServiceOfferingId(userVm.getServiceOfferingId());
    		
    		VmStats vmStats = ApiDBUtils.getVmStatistics(userVm.getId());
    		if (vmStats != null) {
    		    response.setCpuUsed((long) vmStats.getCPUUtilization());
    		    response.setNetworkKbsRead((long) vmStats.getNetworkReadKBs());
    		    response.setNetworkKbsWrite((long) vmStats.getNetworkWriteKBs());
    		}
    		
    		response.setCreated(userVm.getCreated());
    		response.setDisplayName(userVm.getDisplayName());
    		response.setDomain(ApiDBUtils.findDomainById(acct.getDomainId()).getName());
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
