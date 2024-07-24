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


import com.cloud.user.Account;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.IsAccountAllowedToCreateOfferingsWithTagsResponse;

@APICommand(name = "isAccountAllowedToCreateOfferingsWithTags", description = "Return true if the specified account is allowed to create offerings with tags.",
        responseObject = IsAccountAllowedToCreateOfferingsWithTagsResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class IsAccountAllowedToCreateOfferingsWithTagsCmd extends BaseCmd {

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = AccountResponse.class, description = "Account UUID")
    private Long id;

    @Override
    public void execute() {
        Boolean isAllowed = _configService.isAccountAllowedToCreateOfferingsWithTags(this);
        IsAccountAllowedToCreateOfferingsWithTagsResponse response = new IsAccountAllowedToCreateOfferingsWithTagsResponse(isAllowed);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    public Long getId() {
        return id;
    }

    @Override
    public String getCommandName() {
        return "isaccountallowedtocreateofferingswithtagsresponse";
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
