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
import com.cloud.api.response.SecurityGroupResponse;
import com.cloud.network.SecurityGroupVO;
import com.cloud.serializer.SerializerHelper;
import com.cloud.user.Account;

@Implementation(method="searchForSecurityGroups")
public class ListPortForwardingServicesCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListPortForwardingServicesCmd.class.getName());

    private static final String s_name = "listportforwardingservicesresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="account", type=CommandType.STRING)
    private String accountName;

    @Parameter(name="domainid", type=CommandType.LONG)
    private Long domainId;

    @Parameter(name="id", type=CommandType.LONG)
    private Long id;

    @Parameter(name="name", type=CommandType.STRING)
    private String portForwardingServiceName;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getId() {
        return id;
    }

    public String getPortForwardingServiceName() {
        return portForwardingServiceName;
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
        List<SecurityGroupVO> groups = (List<SecurityGroupVO>)getResponseObject();

        List<SecurityGroupResponse> response = new ArrayList<SecurityGroupResponse>();
        for (SecurityGroupVO group : groups) {
            SecurityGroupResponse pfsData = new SecurityGroupResponse();
            pfsData.setId(group.getId());
            pfsData.setName(group.getName());
            pfsData.setDescription(group.getDescription());

            Account accountTemp = getManagementServer().findAccountById(group.getAccountId());
            if (accountTemp != null) {
                pfsData.setAccountName(accountTemp.getAccountName());
                pfsData.setDomainId(accountTemp.getDomainId());
                pfsData.setDomainName(getManagementServer().findDomainIdById(accountTemp.getDomainId()).getName());
            }

            response.add(pfsData);
        }

        return SerializerHelper.toSerializedString(response);
    }
}

