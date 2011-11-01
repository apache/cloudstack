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
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.BaseCmd.CommandType;
import com.cloud.api.response.GuestOSResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.storage.GuestOS;

@Implementation(description="Lists all supported OS types for this cloud.", responseObject=GuestOSResponse.class)
public class ListGuestOsCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListIsosCmd.class.getName());

    private static final String s_name = "listostypesresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, description="list by Os type Id")
    private Long id;

    @Parameter(name=ApiConstants.OS_CATEGORY_ID, type=CommandType.LONG, description="list by Os Category id")
    private Long osCategoryId;

    @Parameter(name=ApiConstants.HYPERVISOR, type=CommandType.STRING, description="the hypervisor for which to restrict the search")
    private String hypervisor;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Long getOsCategoryId() {
        return osCategoryId;
    }

    public String getHypervisor() {
        return hypervisor;
    }
    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }
    
    @Override
    public void execute(){
        List<? extends GuestOS> result = null;
        if (getHypervisor() == null){
            result = _mgr.listGuestOSByCriteria(this);
        }
        else {
            result = _mgr.listGuestOSByHypervisor(this);
        }

        ListResponse<GuestOSResponse> response = new ListResponse<GuestOSResponse>();
        List<GuestOSResponse> osResponses = new ArrayList<GuestOSResponse>();
        for (GuestOS guestOS : result) {
            GuestOSResponse guestOSResponse = new GuestOSResponse();
            guestOSResponse.setDescription(guestOS.getDisplayName());
            guestOSResponse.setId(guestOS.getId());
            guestOSResponse.setOsCategoryId(guestOS.getCategoryId());

            guestOSResponse.setObjectName("ostype");
            osResponses.add(guestOSResponse);
        }

        response.setResponses(osResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
