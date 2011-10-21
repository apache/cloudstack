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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.ServiceOfferingResponse;
import com.cloud.offering.ServiceOffering;

@Implementation(description="Lists all available service offerings.", responseObject=ServiceOfferingResponse.class)
public class ListServiceOfferingsCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListServiceOfferingsCmd.class.getName());

    private static final String s_name = "listserviceofferingsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, description="ID of the service offering")
    private Long id;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, description="name of the service offering")
    private String serviceOfferingName;

    @Parameter(name=ApiConstants.VIRTUAL_MACHINE_ID, type=CommandType.LONG, description="the ID of the virtual machine. Pass this in if you want to see the available service offering that a virtual machine can be changed to.")
    private Long virtualMachineId;
    
    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="the ID of the domain associated with the service offering")
    private Long domainId;

    @Parameter(name=ApiConstants.IS_SYSTEM_OFFERING, type=CommandType.BOOLEAN, description="is this a system vm offering")
    private Boolean isSystem;

    @Parameter(name=ApiConstants.SYSTEM_VM_TYPE, type=CommandType.STRING, description="the system VM type. Possible types are \"consoleproxy\", \"secondarystoragevm\" or \"domainrouter\".")
    private String systemVmType;

    
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getServiceOfferingName() {
        return serviceOfferingName;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    public Long getDomainId(){
    	return domainId;
    }
    
    public Boolean getIsSystem() {
        return isSystem == null ? false : isSystem;
    }
    
    public String getSystemVmType(){
        return systemVmType;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }
    
    @Override
    public void execute(){
        List<? extends ServiceOffering> offerings = _mgr.searchForServiceOfferings(this);
        ListResponse<ServiceOfferingResponse> response = new ListResponse<ServiceOfferingResponse>();
        List<ServiceOfferingResponse> offeringResponses = new ArrayList<ServiceOfferingResponse>();
        for (ServiceOffering offering : offerings) {
            ServiceOfferingResponse offeringResponse = _responseGenerator.createServiceOfferingResponse(offering);
            offeringResponse.setObjectName("serviceoffering");
            offeringResponses.add(offeringResponse);
        }

        response.setResponses(offeringResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
