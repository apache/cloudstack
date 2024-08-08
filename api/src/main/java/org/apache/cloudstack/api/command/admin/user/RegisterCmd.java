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
package org.apache.cloudstack.api.command.admin.user;

import org.apache.cloudstack.acl.Rule;
import org.apache.cloudstack.acl.apikeypair.ApiKeyPair;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.context.CallContext;
import org.apache.commons.lang3.StringUtils;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ApiKeyPairResponse;
import org.apache.cloudstack.api.response.UserResponse;

import com.cloud.user.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@APICommand(name = "registerUserKeys",
        responseObject = ApiKeyPairResponse.class,
        description = "This command allows a user to register for the developer API, returning a secret key and an API key. This request is made through the integration API port, so it is a privileged command and must be made on behalf of a user. It is up to the implementer just how the username and password are entered, and then how that translates to an integration API request. Both secret key and API key should be returned to the user",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = true)
public class RegisterCmd extends BaseCmd {
    protected Logger logger = LogManager.getLogger(getClass());

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = UserResponse.class, required = true, description = "User ID.")
    private Long id;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "API keypair name.")
    private String name;

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING, description = "API keypair description.")
    private String description;

    @Parameter(name = ApiConstants.START_DATE, type = CommandType.DATE, description = "Start date for the API keypair.")
    private Date startDate;

    @Parameter(name = ApiConstants.END_DATE, type = CommandType.DATE, description = "Expiration date for the API keypair.")
    private Date endDate;

    @Parameter(name = ApiConstants.RULES, type = CommandType.MAP, description = "Rules param list, lower indexed rules take precedence over higher. If no rules are informed, " +
            "defaults to allowing all account permissions. Example input: rules[0].rule=* rules[0].permission=allow")
    private Map rules;

    public void setUserId(Long userId) {
        this.id = userId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public void setRules(Map rules) {
        this.rules = rules;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    public List<Map<String, Object>> getRules() {
        List<Map<String, Object>> rulesDetails = new ArrayList<>();

        if (rules == null) {
            return rulesDetails;
        }

        Collection rulesCollection = rules.values();
        for (Object ruleObject : rulesCollection) {
            HashMap<String, String> detail = (HashMap<String, String>) ruleObject;
            Map<String, Object> ruleDetails = new HashMap<>();
            String rule = detail.get(ApiConstants.RULE);

            ruleDetails.put(ApiConstants.RULE, new Rule(rule));

            String permission = detail.get(ApiConstants.PERMISSION);
            if (StringUtils.isEmpty(permission)) {
                throw new ServerApiException(ApiErrorCode.PARAM_ERROR, String.format("Rule %s has no permission associated with it," +
                        " please specify if it is either allow or deny.", rule));
            }
            ruleDetails.put(ApiConstants.PERMISSION, roleService.getRolePermission(permission));

            String description = detail.get(ApiConstants.DESCRIPTION);
            if (StringUtils.isNotEmpty(description)) {
                ruleDetails.put(ApiConstants.DESCRIPTION, description);
            }

            rulesDetails.add(ruleDetails);
        }
        return rulesDetails;
    }

    @Override
    public long getEntityOwnerId() {
        if (id != null) {
            return id;
        }
        return CallContext.current().getCallingAccount().getId();
    }

    public Long getUserId() {
        return id;
    }

    @Override
    public Long getApiResourceId() {
        User user = _entityMgr.findById(User.class, getUserId());
        if (user != null) {
            return user.getId();
        }
        return null;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.User;
    }

    @Override
    public void execute() {
        apiKeyPairService.validateCallingUserHasAccessToDesiredUser(id);

        ApiKeyPair apiKeyPair = _accountService.createApiKeyAndSecretKey(this);
        ApiKeyPairResponse response = new ApiKeyPairResponse();
        if (apiKeyPair != null) {
            response = _responseGenerator.createKeyPairResponse(apiKeyPair);
        }
        response.setObjectName("userkeys");
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
