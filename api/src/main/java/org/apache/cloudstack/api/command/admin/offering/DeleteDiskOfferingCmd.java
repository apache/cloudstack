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
package org.apache.cloudstack.api.command.admin.offering;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DiskOfferingResponse;
import org.apache.cloudstack.api.response.SuccessResponse;

import com.cloud.user.Account;

@APICommand(name = "deleteDiskOffering", description = "Updates a disk offering.", responseObject = SuccessResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class DeleteDiskOfferingCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(DeleteDiskOfferingCmd.class.getName());
    private static final String s_name = "deletediskofferingresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = DiskOfferingResponse.class, required = true, description = "ID of the disk offering")
    private Long id;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() {
        boolean result = _configService.deleteDiskOffering(this);
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete disk offering");
        }
    }
}
