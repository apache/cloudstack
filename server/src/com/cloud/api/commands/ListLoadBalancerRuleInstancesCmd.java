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

import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.UserVmResponse;
import com.cloud.serializer.SerializerHelper;
import com.cloud.user.Account;
import com.cloud.vm.UserVmVO;

@Implementation(method="listLoadBalancerInstances")
public class ListLoadBalancerRuleInstancesCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger (ListLoadBalancerRuleInstancesCmd.class.getName());

    private static final String s_name = "listloadbalancerruleinstancesresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="applied", type=CommandType.BOOLEAN)
    private Boolean applied;

    @Parameter(name="id", type=CommandType.LONG, required=true)
    private Long id;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Boolean isApplied() {
        return applied;
    }

    public Long getId() {
        return id;
    }


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

    @Override @SuppressWarnings("unchecked")
    public String getResponse() {
        List<UserVmVO> instances = (List<UserVmVO>)getResponseObject();

        List<UserVmResponse> response = new ArrayList<UserVmResponse>();
        for (UserVmVO instance : instances) {
            UserVmResponse userVmResponse = new UserVmResponse();
            userVmResponse.setId(instance.getId());
            userVmResponse.setName(instance.getName());
            userVmResponse.setDisplayName(instance.getDisplayName());

            // TODO:  implement
            Account accountTemp = getManagementServer().findAccountById(instance.getAccountId());
            if (accountTemp != null) {
                userVmResponse.setAccountName(accountTemp.getAccountName());
                userVmResponse.setDomainId(accountTemp.getDomainId());
                userVmResponse.setDomainName(getManagementServer().findDomainIdById(accountTemp.getDomainId()).getName());
            }

            response.add(userVmResponse);
        }

        return SerializerHelper.toSerializedString(response);
    }
}
