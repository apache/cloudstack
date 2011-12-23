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
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.HostResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.SwiftResponse;
import com.cloud.storage.Swift;
import com.cloud.user.Account;

@Implementation(description = "List Swift.", responseObject = HostResponse.class)
public class ListSwiftsCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(ListSwiftsCmd.class.getName());
    private static final String s_name = "listswiftresponse";
     
    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.LONG, description = "the id of the swift")
    private Long id;

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
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
        List<? extends Swift> result = _resourceService.listSwifts(this);
        ListResponse<SwiftResponse> response = new ListResponse<SwiftResponse>();
        List<SwiftResponse> swiftResponses = new ArrayList<SwiftResponse>();

        if (result != null) {
            SwiftResponse swiftResponse = null;
            for (Swift swift : result) {
                swiftResponse = _responseGenerator.createSwiftResponse(swift);
                swiftResponse.setResponseName(getCommandName());
                swiftResponse.setObjectName("swift");
                swiftResponses.add(swiftResponse);
            }
            response.setResponses(swiftResponses);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);

        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to add Swift");
        }
    }
}
