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
package org.apache.cloudstack.api.command.admin.usage;

import java.util.List;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.UsageTypeResponse;

import com.cloud.user.Account;

@APICommand(name = "listUsageTypes", description = "List Usage Types", responseObject = UsageTypeResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListUsageTypesCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(ListUsageTypesCmd.class.getName());
    private static final String s_name = "listusagetypesresponse";

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
        List<UsageTypeResponse> result = _usageService.listUsageTypes();
        ListResponse<UsageTypeResponse> response = new ListResponse<UsageTypeResponse>();
        response.setResponses(result);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
