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
package org.apache.cloudstack.api.command.admin.host;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.SuccessResponse;

import com.cloud.user.Account;

@APICommand(name = "deleteHost", description = "Deletes a host.", responseObject = SuccessResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class DeleteHostCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(DeleteHostCmd.class.getName());


    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = HostResponse.class, required = true, description = "the host ID")
    private Long id;

    @Parameter(name = ApiConstants.FORCED,
               type = CommandType.BOOLEAN,
               description = "Force delete the host. All HA enabled vms running on the host will be put to HA; HA disabled ones will be stopped")
    private Boolean forced;

    @Parameter(name = ApiConstants.FORCED_DESTROY_LOCAL_STORAGE,
               type = CommandType.BOOLEAN,
               description = "Force destroy local storage on this host. All VMs created on this local storage will be destroyed")
    private Boolean forceDestroyLocalStorage;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public boolean isForced() {
        return (forced != null) ? forced : false;
    }

    public boolean isForceDestroyLocalStorage() {
        return (forceDestroyLocalStorage != null) ? forceDestroyLocalStorage : true;
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() {
        boolean result = _resourceService.deleteHost(getId(), isForced(), isForceDestroyLocalStorage());
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete host");
        }
    }
}
