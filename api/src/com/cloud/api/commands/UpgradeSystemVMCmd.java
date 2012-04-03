/**
 *  Copyright (C) 2012 Citrix Systems, Inc.  All rights reserved.
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

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.SystemVmResponse;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.offering.ServiceOffering;
import com.cloud.user.Account;
import com.cloud.user.UserContext;
import com.cloud.vm.VirtualMachine;

@Implementation(responseObject=SystemVmResponse.class, description="Changes the service offering for a system vm (console proxy or secondary storage). " +
                                                                                            "The system vm must be in a \"Stopped\" state for " +
                                                                                            "this command to take effect.")
public class UpgradeSystemVMCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(UpgradeVMCmd.class.getName());
    private static final String s_name = "changeserviceforsystemvmresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @IdentityMapper(entityTableName="vm_instance")
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, required=true, description="The ID of the system vm")
    private Long id;

    @IdentityMapper(entityTableName="disk_offering")
    @Parameter(name=ApiConstants.SERVICE_OFFERING_ID, type=CommandType.LONG, required=true, 
                description="the service offering ID to apply to the system vm")
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
    public String getCommandName() {
        return s_name;
    }
    
    @Override
    public long getEntityOwnerId() {
        Account account = UserContext.current().getCaller();
        if (account != null) {
            return account.getId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }
    
    @Override
    public void execute(){
        UserContext.current().setEventDetails("Vm Id: "+getId());
        
        ServiceOffering serviceOffering = _configService.getServiceOffering(serviceOfferingId);
        if (serviceOffering == null) {
            throw new InvalidParameterValueException("Unable to find service offering: " + serviceOfferingId);
        }
        
        VirtualMachine result = _mgr.upgradeSystemVM(this);
        if (result != null) {
            SystemVmResponse response = _responseGenerator.createSystemVmResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Fail to reboot system vm");
        }
    }
}
