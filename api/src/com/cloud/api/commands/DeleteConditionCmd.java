//       Licensed to the Apache Software Foundation (ASF) under one
//       or more contributor license agreements.  See the NOTICE file
//       distributed with this work for additional information
//       regarding copyright ownership.  The ASF licenses this file
//       to you under the Apache License, Version 2.0 (the
//       "License"); you may not use this file except in compliance
//       with the License.  You may obtain a copy of the License at
//
//         http://www.apache.org/licenses/LICENSE-2.0
//
//       Unless required by applicable law or agreed to in writing,
//       software distributed under the License is distributed on an
//       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//       KIND, either express or implied.  See the License for the
//       specific language governing permissions and limitations
//       under the License.

package com.cloud.api.commands;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.SuccessResponse;
import com.cloud.async.AsyncJob;
import com.cloud.event.EventTypes;
import com.cloud.exception.ResourceInUseException;
import com.cloud.network.as.Condition;
import com.cloud.user.Account;

@Implementation(description = "Removes a condition", responseObject = SuccessResponse.class)
public class DeleteConditionCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(DeleteConditionCmd.class.getName());
    private static final String s_name = "deleteconditionresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @IdentityMapper(entityTableName = "conditions")
    @Parameter(name = ApiConstants.ID, type = CommandType.LONG, required = true, description = "the ID of the condition.")
    private Long id;

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public void execute() {
        boolean result = false;
        try {
            result = _autoScaleService.deleteCondition(getId());
        } catch (ResourceInUseException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(BaseCmd.RESOURCE_IN_USE_ERROR, ex.getMessage());
        }
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            s_logger.warn("Failed to delete condition " + getId());
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to delete condition.");
        }
    }

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    @Override
    public AsyncJob.Type getInstanceType() {
        return AsyncJob.Type.Condition;
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        Condition condition = _entityMgr.findById(Condition.class, getId());
        if (condition != null) {
            return condition.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are
        // tracked
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_CONDITION_DELETE;
    }

    @Override
    public String getEventDescription() {
        return "Deleting a condition.";
    }
}