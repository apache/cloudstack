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

import javax.inject.Inject;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ManagementServerResponse;
import org.apache.cloudstack.api.response.ReadyForShutdownResponse;
import org.apache.cloudstack.shutdown.ShutdownManager;
import org.apache.log4j.Logger;
import com.cloud.user.Account;

@APICommand(name = ReadyForShutdownCmd.APINAME,
            description = "Returs the status of CloudStack, whether a shutdown has been triggered and if ready to shutdown",
            since = "4.19.0",
            responseObject = ReadyForShutdownResponse.class,
            requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ReadyForShutdownCmd extends BaseCmd {
    public static final Logger LOG = Logger.getLogger(ReadyForShutdownCmd.class);
    public static final String APINAME = "readyForShutdown";

    @Inject
    private ShutdownManager shutdownManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.MANAGEMENT_SERVER_ID, type = CommandType.UUID, entityType = ManagementServerResponse.class, description = "the uuid of the management server")
    private Long managementServerId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getManagementServerId() {
        return managementServerId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        final ReadyForShutdownResponse response = shutdownManager.readyForShutdown(this);
        response.setResponseName(getCommandName());
        response.setObjectName("readyforshutdown");
        setResponseObject(response);
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
