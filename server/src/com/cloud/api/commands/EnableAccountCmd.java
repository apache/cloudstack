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
import com.cloud.api.ServerApiException;
import com.cloud.api.response.SuccessResponse;
import com.cloud.server.ManagementServer;

@Implementation(method="enableAccount", manager=ManagementServer.class, description="Enables an account")
public class EnableAccountCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(EnableAccountCmd.class.getName());
    private static final String s_name = "enableaccountresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="account", type=CommandType.STRING, description="Enables specified account.")
    private String accountName;

    @Parameter(name="domainid", type=CommandType.LONG, description="Enables specified account in this domain.")
    private Long domainId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

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
    public SuccessResponse getResponse() {
        SuccessResponse response = new SuccessResponse();
        Boolean responseObject = (Boolean)getResponseObject();
      
        if (responseObject != null) {
        	response.setSuccess(responseObject);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to enable account");
        }

        response.setResponseName(getName());
        return response;
    }
}
