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

package org.apache.cloudstack.api.command.admin.guest;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.GuestOSCategoryResponse;

import com.cloud.storage.GuestOsCategory;
import com.cloud.user.Account;

@APICommand(name = "updateOsCategory",
        description = "Updates an OS category",
        responseObject = GuestOSCategoryResponse.class,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        since = "4.21.0",
        authorized = {RoleType.Admin})
public class UpdateGuestOsCategoryCmd extends BaseCmd {



    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = GuestOSCategoryResponse.class,
            required = true, description = "ID of the OS category")
    private Long id;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "Name for the OS category")
    private String name;

    @Parameter(name = ApiConstants.IS_FEATURED, type = CommandType.BOOLEAN,
            description = "Whether the category is featured or not")
    private Boolean featured;

    @Parameter(name = ApiConstants.SORT_KEY, type = CommandType.INTEGER,
            description = "sort key of the OS category for listing")
    private Integer sortKey;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Boolean isFeatured() {
        return featured;
    }

    public Integer getSortKey() {
        return sortKey;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() {
        GuestOsCategory guestOs = _mgr.updateGuestOsCategory(this);
        if (guestOs != null) {
            GuestOSCategoryResponse response = _responseGenerator.createGuestOSCategoryResponse(guestOs);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update OS category");
        }
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.GuestOsCategory;
    }

    @Override
    public Long getApiResourceId() {
        return getId();
    }
}
