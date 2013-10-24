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

import com.cloud.event.EventTypes;

import com.cloud.exception.*;
import com.cloud.ucs.manager.UcsManager;
import com.cloud.user.Account;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.api.response.UcsBladeResponse;
import org.apache.log4j.Logger;

import javax.inject.Inject;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Date: 9/13/13
 * Time: 6:23 PM
 * To change this template use File | Settings | File Templates.
 */
@APICommand(name="disassociateUcsProfileFromBlade", description="disassociate a profile from a blade", responseObject=UcsBladeResponse.class)
public class DisassociateUcsProfileCmd extends  BaseAsyncCmd {
    private static Logger logger = Logger.getLogger(DisassociateUcsProfileCmd.class);

    @Inject
    private UcsManager mgr;

    @Parameter(name=ApiConstants.UCS_BLADE_ID, type=CommandType.UUID, entityType=UcsBladeResponse.class, description="blade id", required=true)
    private Long bladeId;

    @Parameter(name=ApiConstants.UCS_DELETE_PROFILE, type=CommandType.BOOLEAN, description="is deleting profile after disassociating")
    private boolean deleteProfile;

    @Override
    public String getEventType() {
        return EventTypes.EVENT_UCS_DISASSOCIATED_PROFILE;
    }

    @Override
    public String getEventDescription() {
        return "disassociate a profile from blade";
    }

    public Long getBladeId() {
        return bladeId;
    }

    public void setBladeId(Long bladeId) {
        this.bladeId = bladeId;
    }

    public boolean isDeleteProfile() {
        return deleteProfile;
    }

    public void setDeleteProfile(boolean deleteProfile) {
        this.deleteProfile = deleteProfile;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        try {
            UcsBladeResponse rsp = mgr.disassociateProfile(this);
            rsp.setResponseName(getCommandName());
            this.setResponseObject(rsp);
        } catch(Exception e) {
            logger.warn(e.getMessage(), e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    @Override
    public String getCommandName() {
        return "disassociateucsprofilefrombladeresponse";
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
