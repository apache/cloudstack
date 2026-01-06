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


import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.UsageTypeResponse;
import org.apache.cloudstack.usage.UsageTypes;

import com.cloud.user.Account;

@APICommand(name = "listUsageTypes", description = "List Usage Types", responseObject = UsageTypeResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListUsageTypesCmd extends BaseCmd {

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() {
        List<UsageTypeResponse> result = UsageTypes.listUsageTypes();
        ListResponse<UsageTypeResponse> response = new ListResponse<>();
        response.setResponses(result);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
