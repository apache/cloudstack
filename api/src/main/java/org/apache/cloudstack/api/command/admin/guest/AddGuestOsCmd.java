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

import org.apache.commons.collections.MapUtils;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.GuestOSCategoryResponse;
import org.apache.cloudstack.api.response.GuestOSResponse;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.EventTypes;
import com.cloud.storage.GuestOS;
import com.cloud.user.Account;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@APICommand(name = "addGuestOs", description = "Add a new guest OS type", responseObject = GuestOSResponse.class,
        since = "4.4.0", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class AddGuestOsCmd extends BaseAsyncCreateCmd {


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.OS_CATEGORY_ID, type = CommandType.UUID, entityType = GuestOSCategoryResponse.class, required = true, description = "ID of Guest OS category")
    private Long osCategoryId;

    @Parameter(name = ApiConstants.OS_DISPLAY_NAME, type = CommandType.STRING, required = true, description = "Unique display name for Guest OS")
    private String osDisplayName;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = false, description = "Optional name for Guest OS")
    private String osName;

    @Parameter(name = ApiConstants.DETAILS, type = CommandType.MAP, required = false, description = "Map of (key/value pairs)")
    private Map details;

    @Parameter(name="forDisplay", type=CommandType.BOOLEAN, description="whether this guest OS is available for end users", authorized = {RoleType.Admin})
    private Boolean display;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getOsCategoryId() {
        return osCategoryId;
    }

    public String getOsDisplayName() {
        return osDisplayName;
    }

    public String getOsName() {
        return osName;
    }

    public Map getDetails() {
        Map<String, String> detailsMap = new HashMap<>();
        if (!MapUtils.isEmpty(details)) {
            Collection<?> servicesCollection = details.values();
            Iterator<?> iter = servicesCollection.iterator();
            while (iter.hasNext()) {
                HashMap<String, String> services = (HashMap<String, String>)iter.next();
                String key = services.get("key");
                String value = services.get("value");
                detailsMap.put(key, value);
            }
        }
        return detailsMap;
    }

    public Boolean getForDisplay() {
        return display;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void create() {
        GuestOS guestOs = _mgr.addGuestOs(this);
        if (guestOs != null) {
            setEntityId(guestOs.getId());
            setEntityUuid(guestOs.getUuid());
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add new guest OS type entity");
        }
    }

    @Override
    public void execute() {
        CallContext.current().setEventDetails("Guest OS Id: " + getEntityId());
        GuestOS guestOs = _mgr.getAddedGuestOs(getEntityId());
        if (guestOs != null) {
            GuestOSResponse response = _responseGenerator.createGuestOSResponse(guestOs);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add new guest OS type");
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_GUEST_OS_ADD;
    }

    @Override
    public String getEventDescription() {
        return "adding a new guest OS type Id: " + getEntityId();
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.GuestOs;
    }

    @Override
    public String getCreateEventType() {
        return EventTypes.EVENT_GUEST_OS_ADD;
    }

    @Override
    public String getCreateEventDescription() {
        return "adding new guest OS type";
    }

}
