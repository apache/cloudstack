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
import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.InstanceGroupResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.user.Account;
import com.cloud.vm.InstanceGroupVO;

@Implementation(method="searchForVmGroups")
public class ListVMGroupsCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListVMGroupsCmd.class.getName());

    private static final String s_name = "listinstancegroupsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, description="list instance groups by ID")
    private Long id;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, description="list instance groups by name")
    private String groupName;

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="list instance group belonging to the specified account. Must be used with domainid parameter")
    private String accountName;

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="the domain ID. If used with the account parameter, lists virtual machines for the specified account in this domain.")
    private Long domainId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

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
	public ListResponse<InstanceGroupResponse> getResponse() {
        List<InstanceGroupVO> groups = (List<InstanceGroupVO>)getResponseObject();

        ListResponse<InstanceGroupResponse> response = new ListResponse<InstanceGroupResponse>();
        List<InstanceGroupResponse> responses = new ArrayList<InstanceGroupResponse>();
        for (InstanceGroupVO group : groups) {
            InstanceGroupResponse groupResponse = new InstanceGroupResponse();

            groupResponse.setId(group.getId());
            groupResponse.setName(group.getName());
            groupResponse.setCreated(group.getCreated());

            Account accountTemp = ApiDBUtils.findAccountById(group.getAccountId());
            if (accountTemp != null) {
                groupResponse.setAccountName(accountTemp.getAccountName());
                groupResponse.setDomainId(accountTemp.getDomainId());
                groupResponse.setDomainName(ApiDBUtils.findDomainById(accountTemp.getDomainId()).getName());
            }

            groupResponse.setResponseName("instancegroup");
            responses.add(groupResponse);
        }

        response.setResponses(responses);
        response.setResponseName(getName());
        return response;
    }
}
