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

package org.apache.cloudstack.api.command;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.log4j.Logger;

import com.cloud.user.Account;

import org.apache.cloudstack.api.response.ReadyForShutdownResponse;
import org.apache.cloudstack.acl.RoleType;

@APICommand(name = TriggerShutdownCmd.APINAME,
            description = "Triggers an automatic safe shutdown of CloudStack by not accepting new jobs and shutting down when all pending jobbs have been completed. Triggers an immediate shutdown if forced",
            responseObject = ReadyForShutdownResponse.class,
            requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
            authorized = {RoleType.Admin})
public class TriggerShutdownCmd extends BaseShutdownActionCmd {
    public static final Logger LOG = Logger.getLogger(TriggerShutdownCmd.class);
    public static final String APINAME = "triggerShutdown";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.FORCED, type = CommandType.BOOLEAN, description = "Force an immediate shutdown instead of a safe one")
    private Boolean forced;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Boolean getForced() {
        return forced;
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        final ReadyForShutdownResponse response = shutdownManager.triggerShutdown(this);
        response.setResponseName(getCommandName());
        response.setObjectName("triggershutdown");
        setResponseObject(response);
    }
}
