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

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.CloudIdentifierResponse;
import com.cloud.user.Account;

@Implementation(description="Retrieves a cloud identifier.", responseObject=CloudIdentifierResponse.class)
public class GetCloudIdentifierCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(GetCloudIdentifierCmd.class.getName());
    private static final String s_name = "getcloudidentifierresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @IdentityMapper(entityTableName="user")
    @Parameter(name=ApiConstants.USER_ID, type=CommandType.LONG, required=true, description="the user ID for the cloud identifier")
    private Long userid;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getUserId() {
        return userid;
    }


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    public String getCommandName() {
        return s_name;
    }
    
    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
    
    @Override
    public void execute(){
        ArrayList<String> result = _mgr.getCloudIdentifierResponse(userid);
        CloudIdentifierResponse response = new CloudIdentifierResponse();
        if (result != null) {
            response.setCloudIdentifier(result.get(0));
            response.setSignature(result.get(1));
            response.setObjectName("cloudidentifier");
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to get cloud identifier");
        }
      
    }
}