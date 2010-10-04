/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General License for more details.
 * 
 * You should have received a copy of the GNU General License
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
import com.cloud.api.response.InstanceGroupResponse;
import com.cloud.user.Account;
import com.cloud.vm.InstanceGroupVO;

@Implementation(method="createVmGroup", manager=Manager.UserVmManager)
public class CreateVMGroupCmd extends BaseCmd{
    public static final Logger s_logger = Logger.getLogger(CreateVMGroupCmd.class.getName());

    private static final String s_name = "createinstancegroupresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="name", type=CommandType.STRING, required=true)
    private String groupName;

    @Parameter(name="account", type=CommandType.STRING)
    private String accountName;

    @Parameter(name="domainid", type=CommandType.LONG)
    private Long domainId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getGroupName() {
        return groupName;
    }

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

    @Override @SuppressWarnings("unchecked")
    public InstanceGroupResponse getResponse() {
        InstanceGroupVO group = (InstanceGroupVO)getResponseObject();

        InstanceGroupResponse response = new InstanceGroupResponse();
        response.setId(group.getId());
        response.setName(group.getName());
        response.setCreated(group.getCreated());

        Account accountTemp = ApiDBUtils.findAccountById(group.getAccountId());
        if (accountTemp != null) {
            response.setAccountName(accountTemp.getAccountName());
            response.setDomainId(accountTemp.getDomainId());
            response.setDomainName(ApiDBUtils.findDomainById(accountTemp.getDomainId()).getName());
        }

        response.setResponseName(getName());
        return response;
    }
}
