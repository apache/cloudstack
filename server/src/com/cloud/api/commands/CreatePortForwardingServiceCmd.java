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

import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.SecurityGroupResponse;
import com.cloud.network.SecurityGroupVO;
import com.cloud.serializer.SerializerHelper;

@Implementation(method="createPortForwardingService")
public class CreatePortForwardingServiceCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreatePortForwardingServiceCmd.class.getName());

    private static final String s_name = "createportforwardingserviceresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="account", type=CommandType.STRING)
    private String accountName;

    @Parameter(name="description", type=CommandType.STRING)
    private String description;

    @Parameter(name="domainid", type=CommandType.LONG)
    private Long domainId;

    @Parameter(name="name", type=CommandType.STRING, required=true)
    private String portForwardingServiceName;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public String getDescription() {
        return description;
    }

    public Long getDomainId() {
        return domainId;
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

    @Override
    public String getResponse() {
        SecurityGroupVO group = (SecurityGroupVO)getResponseObject();

        SecurityGroupResponse response = new SecurityGroupResponse();
        response.setId(group.getId());
        response.setName(group.getName());
        response.setDescription(group.getDescription());
        response.setAccountName(group.getAccountName());
        response.setDomainId(group.getDomainId());
        // TODO: implement
        //response.setDomainName(group.getDomainName());

        return SerializerHelper.toSerializedString(response);
    }
}
