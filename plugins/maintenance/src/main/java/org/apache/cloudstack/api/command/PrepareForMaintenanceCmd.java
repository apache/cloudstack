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

import com.cloud.user.Account;

import org.apache.cloudstack.api.response.ManagementServerMaintenanceResponse;
import org.apache.cloudstack.acl.RoleType;

@APICommand(name = PrepareForMaintenanceCmd.APINAME,
            description = "Prepares management server for maintenance by preventing new jobs from being accepted after completion of active jobs and migrating the agents",
            since = "4.21.0",
            responseObject = ManagementServerMaintenanceResponse.class,
            requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
            authorized = {RoleType.Admin})
public class PrepareForMaintenanceCmd extends BaseMSMaintenanceActionCmd {
    public static final String APINAME = "prepareForMaintenance";

    @Parameter(name = ApiConstants.ALGORITHM, type = CommandType.STRING, description = "indirect agents load balancer algorithm (static, roundrobin, shuffle);" +
            " when this is not set, already configured algorithm from setting 'indirect.agent.lb.algorithm' is considered")
    private String algorithm;

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
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
        final ManagementServerMaintenanceResponse response = managementServerMaintenanceManager.prepareForMaintenance(this);
        response.setResponseName(getCommandName());
        response.setObjectName("prepareformaintenance");
        setResponseObject(response);
    }
}
