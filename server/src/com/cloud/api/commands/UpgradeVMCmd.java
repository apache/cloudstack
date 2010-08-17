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

import java.util.Date;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.response.UpgradeVmResponse;
import com.cloud.domain.DomainVO;
import com.cloud.serializer.Param;
import com.cloud.vm.UserVmVO;

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

    private UserVmVO responseObject = null;
    
    public String getName() {
        return s_name;
    }

    public static String getResultObjectName() {
    	return "virtualmachine";
    }    
    
    @Override
    public String getResponse() 
    {
        UpgradeVmResponse response = new UpgradeVmResponse();
        UserVmVO userVm = (UserVmVO)getResponseObject();
        
        UserVmVO responseObject = (UserVmVO)getResponseObject();
        if (responseObject != null) 
        {
//            
//    		Account acct = ms.findAccountById(Long.valueOf(vm.getAccountId()));
//    		resultObj.setAccount(acct.getAccountName());
//    		
//    		ServiceOfferingVO offering = ms.findServiceOfferingById(vm.getServiceOfferingId());
//    		resultObj.setCpuSpeed(offering.getSpeed());
//    		resultObj.setMemory(offering.getRamSize());
//    		if(offering.getDisplayText()!=null)
//    			resultObj.setServiceOfferingName(offering.getDisplayText());
//    		else
//    			resultObj.setServiceOfferingName(offering.getName());
//    		resultObj.setServiceOfferingId(vm.getServiceOfferingId());
//    		
//    		VmStats vmStats = ms.getVmStatistics(vm.getId());
//    		if(vmStats != null)
//    		{
//    			resultObj.setCpuUsed((long) vmStats.getCPUUtilization());
//    			resultObj.setNetworkKbsRead((long) vmStats.getNetworkReadKBs());
//    			resultObj.setNetworkKbsWrite((long) vmStats.getNetworkWriteKBs());
//    		}
//    		
//    		resultObj.setCreated(vm.getCreated());
//    		resultObj.setDisplayName(vm.getDisplayName());
//    		resultObj.setDomain(ms.findDomainIdById(acct.getDomainId()).getName());
//    		resultObj.setDomainId(acct.getDomainId());
//    		resultObj.setHaEnable(vm.isHaEnabled());
//    		if(vm.getHostId() != null)
//    		{
//    			resultObj.setHostId(vm.getHostId());
//    			resultObj.setHostName(ms.getHostBy(vm.getHostId()).getName());
//    		}
//    		resultObj.setIpAddress(vm.getPrivateIpAddress());
//    		resultObj.setName(vm.getName());
//    		resultObj.setState(vm.getState().toString());
//    		resultObj.setZoneId(vm.getDataCenterId());
//    		resultObj.setZoneName(ms.findDataCenterById(vm.getDataCenterId()).getName());
//    		
//    		VMTemplateVO template = ms.findTemplateById(vm.getTemplateId());
//    		resultObj.setPasswordEnabled(template.getEnablePassword());
//    		resultObj.setTemplateDisplayText(template.getDisplayText());
//    		resultObj.setTemplateId(template.getId());
//    		resultObj.setTemplateName(template.getName());
//        } 
//        else 
//        {
//        	throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to update zone; internal error.");
//        }
//        
//        return SerializerHelper.toSerializedString(responseObject);
        	
        }
        return null;
    }
    
    public void setResponseObject(UserVmVO userVm) {
        responseObject = userVm;
    }
        
}
