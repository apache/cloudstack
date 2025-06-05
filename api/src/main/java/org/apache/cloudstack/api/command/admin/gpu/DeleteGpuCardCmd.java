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
package org.apache.cloudstack.api.command.admin.gpu;

import com.cloud.user.Account;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.GpuCardResponse;
import org.apache.cloudstack.api.response.SuccessResponse;


@APICommand(name = "deleteGpuCard", description = "Deletes a GPU card definition from the system",
            responseObject = SuccessResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
            since = "4.21.0", authorized = {RoleType.Admin})
public class DeleteGpuCardCmd extends BaseCmd {

    /// //////////////////////////////////////////////////
    /// ///////////// API parameters /////////////////////
    /// //////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = GpuCardResponse.class, required = true,
               description = "the ID of the GPU card")
    private Long id;

    /// //////////////////////////////////////////////////
    /// //////////////// Accessors ///////////////////////
    /// //////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    @Override
    public void execute() {
        try {
            boolean success = gpuService.deleteGpuCard(this);
            if (success) {
                SuccessResponse response = new SuccessResponse(getCommandName());
                setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete GPU card");
            }
        } catch (Exception e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete GPU card: " + e.getMessage());
        }
    }

    /// //////////////////////////////////////////////////
    /// //////////// API Implementation///////////////////
    /// //////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
