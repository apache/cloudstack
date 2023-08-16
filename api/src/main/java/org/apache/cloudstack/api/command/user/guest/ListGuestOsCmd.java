// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.api.command.user.guest;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.GuestOSCategoryResponse;
import org.apache.cloudstack.api.response.GuestOSResponse;
import org.apache.cloudstack.api.response.ListResponse;

import com.cloud.storage.GuestOS;
import com.cloud.utils.Pair;

@APICommand(name = "listOsTypes", description = "Lists all supported OS types for this cloud.", responseObject = GuestOSResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListGuestOsCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListGuestOsCmd.class.getName());


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = GuestOSResponse.class, description = "list by Os type Id")
    private Long id;

    @Parameter(name = ApiConstants.OS_CATEGORY_ID, type = CommandType.UUID, entityType = GuestOSCategoryResponse.class, description = "list by Os Category id")
    private Long osCategoryId;

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING, description = "list os by description", since = "3.0.1")
    private String description;

    @Parameter(name = ApiConstants.FOR_DISPLAY, type = CommandType.BOOLEAN, description = "list resources by display flag; only ROOT admin is eligible to pass this parameter",
            since = "4.18.1", authorized = {RoleType.Admin})
    private Boolean display;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Long getOsCategoryId() {
        return osCategoryId;
    }

    public String getDescription() {
        return description;
    }

    public Boolean getDisplay() {
        return BooleanUtils.toBooleanDefaultIfNull(display, true);
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        Pair<List<? extends GuestOS>, Integer> result = _mgr.listGuestOSByCriteria(this);
        ListResponse<GuestOSResponse> response = new ListResponse<>();
        List<GuestOSResponse> osResponses = new ArrayList<>();
        for (GuestOS guestOS : result.first()) {
            GuestOSResponse guestOSResponse = _responseGenerator.createGuestOSResponse(guestOS);
            osResponses.add(guestOSResponse);
        }

        response.setResponses(osResponses, result.second());
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
