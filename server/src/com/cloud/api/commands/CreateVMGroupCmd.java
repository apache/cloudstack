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

import com.cloud.api.ApiConstants;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.InstanceGroupResponse;
import com.cloud.user.Account;
import com.cloud.vm.InstanceGroupVO;
import com.cloud.vm.UserVmManager;

@Implementation(method="createVmGroup", manager=UserVmManager.class)
public class CreateVMGroupCmd extends BaseCmd{
    public static final Logger s_logger = Logger.getLogger(CreateVMGroupCmd.class.getName());

    private static final String s_name = "createinstancegroupresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, required=true, description="the name of the instance group")
    private String groupName;

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="the account of the instance group. The account parameter must be used with the domainId parameter.")
    private String accountName;

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="the domain ID of account owning the instance group")
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
