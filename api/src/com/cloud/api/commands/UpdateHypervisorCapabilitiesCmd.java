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

import java.util.UUID;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.HypervisorCapabilitiesResponse;
import com.cloud.api.response.ServiceOfferingResponse;
import com.cloud.hypervisor.HypervisorCapabilities;
import com.cloud.user.Account;


@Implementation(description="Updates a hypervisor capabilities.", responseObject=ServiceOfferingResponse.class, since="3.0.0")
public class UpdateHypervisorCapabilitiesCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateHypervisorCapabilitiesCmd.class.getName());
    private static final String s_name = "updatehypervisorcapabilitiesresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @IdentityMapper(entityTableName="hypervisor_capabilities")
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, description="ID of the hypervisor capability")
    private Long id;

    @Parameter(name=ApiConstants.SECURITY_GROUP_EANBLED, type=CommandType.BOOLEAN, description="set true to enable security group for this hypervisor.")
    private Boolean securityGroupEnabled;

    @Parameter(name=ApiConstants.MAX_GUESTS_LIMIT, type=CommandType.LONG, description="the max number of Guest VMs per host for this hypervisor.")
    private Long maxGuestsLimit;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Boolean getSecurityGroupEnabled() {
        return securityGroupEnabled;
    }

    public Long getId() {
        return id;
    }

    public Long getMaxGuestsLimit() {
        return maxGuestsLimit;
    }



    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////


    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute(){
        HypervisorCapabilities result = _mgr.updateHypervisorCapabilities(getId(), getMaxGuestsLimit(), getSecurityGroupEnabled());
        if (result != null){
            HypervisorCapabilitiesResponse response = _responseGenerator.createHypervisorCapabilitiesResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to update hypervisor capabilities");
        }
    }
}
