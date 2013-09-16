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

package org.apache.cloudstack.api.command.admin.network;

import com.cloud.event.EventTypes;
import com.cloud.exception.ResourceInUseException;
import com.cloud.user.Account;

import org.apache.cloudstack.api.*;
import org.apache.cloudstack.api.response.CounterResponse;
import org.apache.cloudstack.api.response.GuestVlanRangeResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.log4j.Logger;

@APICommand(name = "releaseDedicatedGuestVlanRange", description = "Releases a dedicated guest vlan range to the system", responseObject = SuccessResponse.class)
public class ReleaseDedicatedGuestVlanRangeCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(ReleaseDedicatedGuestVlanRangeCmd.class.getName());
    private static final String s_name = "releasededicatedguestvlanrangeresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType=GuestVlanRangeResponse.class,
            required=true, description="the ID of the dedicated guest vlan range")
    private Long id;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    public Long getId() {
        return id;
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.DedicatedGuestVlanRange;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_DEDICATED_GUEST_VLAN_RANGE_RELEASE;
    }

    @Override
    public String getEventDescription() {
        return "Releasing a dedicated guest vlan range.";
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////


    @Override
    public void execute(){
        CallContext.current().setEventDetails("Dedicated guest vlan range Id: " + id);
        boolean result = _networkService.releaseDedicatedGuestVlanRange(getId());
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to release dedicated guest vlan range");
        }
    }

}
