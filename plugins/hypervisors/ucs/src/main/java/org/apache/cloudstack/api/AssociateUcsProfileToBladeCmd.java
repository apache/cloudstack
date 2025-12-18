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
//
package org.apache.cloudstack.api;

import javax.inject.Inject;


import org.apache.cloudstack.api.response.UcsBladeResponse;
import org.apache.cloudstack.api.response.UcsManagerResponse;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.ucs.manager.UcsManager;
import com.cloud.user.Account;

@APICommand(name = "associateUcsProfileToBlade", description = "associate a profile to a blade", responseObject = UcsBladeResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class AssociateUcsProfileToBladeCmd extends BaseAsyncCmd {

    @Inject
    private UcsManager mgr;

    @Parameter(name = ApiConstants.UCS_MANAGER_ID, type = CommandType.UUID, description = "ucs manager id", entityType = UcsManagerResponse.class, required = true)
    private Long ucsManagerId;
    @Parameter(name = ApiConstants.UCS_PROFILE_DN, type = CommandType.STRING, description = "profile dn", required = true)
    private String profileDn;
    @Parameter(name = ApiConstants.UCS_BLADE_ID, type = CommandType.UUID, entityType = UcsBladeResponse.class, description = "blade id", required = true)
    private Long bladeId;

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException,
        ResourceAllocationException, NetworkRuleConflictException {
        try {
            UcsBladeResponse rsp = mgr.associateProfileToBlade(this);
            rsp.setResponseName(getCommandName());
            this.setResponseObject(rsp);
        } catch (Exception e) {
            logger.warn("Exception: ", e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    public Long getUcsManagerId() {
        return ucsManagerId;
    }

    public void setUcsManagerId(Long ucsManagerId) {
        this.ucsManagerId = ucsManagerId;
    }

    public String getProfileDn() {
        return profileDn;
    }

    public void setProfileDn(String profileDn) {
        this.profileDn = profileDn;
    }

    public Long getBladeId() {
        return bladeId;
    }

    public void setBladeId(Long bladeId) {
        this.bladeId = bladeId;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_UCS_ASSOCIATED_PROFILE;
    }

    @Override
    public String getEventDescription() {
        return "associating a ucs profile to blade";
    }
}
