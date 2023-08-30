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
import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.GuestOSResponse;
import org.apache.cloudstack.acl.RoleType;

import com.cloud.event.EventTypes;
import com.cloud.storage.GuestOS;
import com.cloud.user.Account;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@APICommand(name = "updateGuestOs", description = "Updates the information about Guest OS", responseObject = GuestOSResponse.class,
        since = "4.4.0", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class UpdateGuestOsCmd extends BaseAsyncCmd {

    public static final Logger s_logger = Logger.getLogger(UpdateGuestOsCmd.class.getName());


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = GuestOSResponse.class, required = true, description = "UUID of the Guest OS")
    private Long id;

    @Parameter(name = ApiConstants.OS_DISPLAY_NAME, type = CommandType.STRING, required = true, description = "Unique display name for Guest OS")
    private String osDisplayName;

    @Parameter(name = ApiConstants.DETAILS, type = CommandType.MAP, required = false, description = "Map of (key/value pairs)")
    private Map details;

    @Parameter(name="forDisplay", type=CommandType.BOOLEAN, description="whether this guest OS is available for end users", authorized = {RoleType.Admin})
    private Boolean display;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getOsDisplayName() {
        return osDisplayName;
    }

    public Map getDetails() {
        Map<String, String> detailsMap = new HashMap<>();;
        if (MapUtils.isNotEmpty(detailsMap)) {
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
    public void execute() {
        GuestOS guestOs = _mgr.updateGuestOs(this);
        if (guestOs != null) {
            GuestOSResponse response = _responseGenerator.createGuestOSResponse(guestOs);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update guest OS type");
        }
    }

    @Override
    public String getEventDescription() {
        return "Updating guest OS: " + getId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_GUEST_OS_UPDATE;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.GuestOs;
    }
}
