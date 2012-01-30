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
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.HostResponse;
import com.cloud.api.response.SwiftResponse;
import com.cloud.exception.DiscoveryException;
import com.cloud.storage.Swift;
import com.cloud.user.Account;

@Implementation(description = "Adds Swift.", responseObject = HostResponse.class, since="3.0.0")
public class AddSwiftCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(AddSwiftCmd.class.getName());
    private static final String s_name = "addswiftresponse";
     
    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.URL, type = CommandType.STRING, required = true, description = "the URL for swift")
    private String url;

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "the account for swift")
    private String account;

    @Parameter(name = ApiConstants.USERNAME, type = CommandType.STRING, description = "the username for swift")
    private String username;

    @Parameter(name = ApiConstants.KEY, type = CommandType.STRING, description = " key for the user for swift")
    private String key;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getUrl() {
        return url;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    public String getAccount() {
        return account;
    }

    public String getUsername() {
        return username;
    }

    public String getKey() {
        return key;
    }

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
        try {
            Swift result = _resourceService.discoverSwift(this);
            SwiftResponse swiftResponse = null;
            if (result != null) {
                swiftResponse = _responseGenerator.createSwiftResponse(result);
                swiftResponse.setResponseName(getCommandName());
                swiftResponse.setObjectName("swift");
                this.setResponseObject(swiftResponse);
            } else {
                throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to add Swift");
            }
        } catch (DiscoveryException ex) {
            String errMsg = "Failed to add Swift due to " + ex.toString();
            s_logger.warn(errMsg, ex);
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, errMsg);
        }
    }
}
