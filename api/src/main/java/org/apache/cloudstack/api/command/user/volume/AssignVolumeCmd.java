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
package org.apache.cloudstack.api.command.user.volume;

import com.cloud.exception.ResourceAllocationException;
import com.cloud.user.Account;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.log4j.Logger;

import com.cloud.storage.Volume;

import java.util.Map;

@APICommand(name = AssignVolumeCmd.CMD_NAME, responseObject = VolumeResponse.class, description = "Changes ownership of a Volume from one account to another.", entityType = {
        Volume.class}, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "4.18.0.0")
public class AssignVolumeCmd extends BaseCmd implements UserCmd {
    public static final Logger LOGGER = Logger.getLogger(AssignVolumeCmd.class.getName());
    public static final String CMD_NAME = "assignVolume";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.VOLUME_ID, type = CommandType.UUID, entityType = VolumeResponse.class, required = true, description = "The ID of the volume to be reassigned.")
    private Long volumeId;

    @Parameter(name = ApiConstants.ACCOUNT_ID, type = CommandType.UUID, entityType = AccountResponse.class,
            description = "The ID of the account to which the volume will be assigned. Mutually exclusive with parameter 'projectid'.")
    private Long accountId;

    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class,
            description = "The ID of the project to which the volume will be assigned. Mutually exclusive with 'accountid'.")
    private Long projectid;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getVolumeId() {
        return volumeId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public Long getProjectid() {
        return projectid;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        try {
            Volume result = _volumeService.assignVolumeToAccount(this);
            if (result == null) {
                Map<String,String> fullParams = getFullUrlParams();
                if (accountId != null) {
                    throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Failed to move volume [%s] to account [%s].", fullParams.get(ApiConstants.VOLUME_ID),
                            fullParams.get(ApiConstants.ACCOUNT_ID)));
                }
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, String.format("Failed to move volume [%s] to project [%s].", fullParams.get(ApiConstants.VOLUME_ID),
                        fullParams.get(ApiConstants.PROJECT_ID)));
            }

            VolumeResponse response = _responseGenerator.createVolumeResponse(getResponseView(), result);
            response.setResponseName(getCommandName());
            setResponseObject(response);

        } catch (CloudRuntimeException | ResourceAllocationException e) {
            String msg = String.format("Assign volume command for volume [%s] failed due to [%s].", getFullUrlParams().get("volumeid"), e.getMessage());
            LOGGER.error(msg, e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, msg);
        }
    }

    @Override
    public String getCommandName() {
        return CMD_NAME.toLowerCase() + RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        Volume volume = _responseGenerator.findVolumeById(getVolumeId());

        if (volume != null) {
            return volume.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM;
    }
}
