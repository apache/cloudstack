//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
package org.apache.cloudstack.api.command;

import com.cloud.user.Account;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiArgValidator;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.QuotaResponseBuilder;
import org.apache.cloudstack.api.response.QuotaTariffResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;

import javax.inject.Inject;

@APICommand(name = "quotaTariffDelete", description = "Marks a quota tariff as removed.", responseObject = SuccessResponse.class, requestHasSensitiveInfo = false,
responseHasSensitiveInfo = false, since = "4.18.0.0", authorized = {RoleType.Admin})
public class QuotaTariffDeleteCmd extends BaseCmd {

    @Inject
    QuotaResponseBuilder responseBuilder;

    @Parameter(name = ApiConstants.ID, type = BaseCmd.CommandType.STRING, required = true, entityType = QuotaTariffResponse.class,
            description = "ID of the quota tariff", validations = {ApiArgValidator.UuidString})
    private String id;

    public String getId() {
        return id;
    }

    @Override
    public void execute() {
        CallContext.current().setEventDetails(String.format("Tariff id: %s", getId()));
        boolean result = responseBuilder.deleteQuotaTariff(getId());
        SuccessResponse response = new SuccessResponse(getCommandName());
        response.setSuccess(result);
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.QuotaTariff;
    }
}
