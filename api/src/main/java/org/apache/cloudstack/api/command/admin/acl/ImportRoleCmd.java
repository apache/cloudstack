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

package org.apache.cloudstack.api.command.admin.acl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.acl.Role;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.acl.Rule;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiArgValidator;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ApiServerService;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.RoleResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.commons.collections.MapUtils;

import com.cloud.user.Account;
import com.google.common.base.Strings;

@APICommand(name = ImportRoleCmd.APINAME, description = "Imports a role based on provided map of rule permissions", responseObject = RoleResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        since = "4.15.0",
        authorized = {RoleType.Admin})
public class ImportRoleCmd extends RoleCmd {
    public static final String APINAME = "importRole";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true,
            description = "Creates a role with this unique name", validations = {ApiArgValidator.NotNullOrEmpty})
    private String roleName;

    @Parameter(name = ApiConstants.RULES, type = CommandType.MAP, required = true,
            description = "Rules param list, rule and permission is must. Example: rules[0].rule=create*&rules[0].permission=allow&rules[0].description=create%20rule&rules[1].rule=list*&rules[1].permission=allow&rules[1].description=listing")
    private Map rules;

    @Parameter(name = ApiConstants.FORCED, type = CommandType.BOOLEAN,
            description = "Force create a role with the same name. This overrides the role type, description and rule permissions for the existing role. Default is false.")
    private Boolean forced;

    @Inject
    ApiServerService _apiServer;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getRoleName() {
        return roleName;
    }

    // Returns list of rule maps. Each map corresponds to a rule with the details in the keys: rule, permission & description
    public List<Map<String, Object>> getRules() {
        if (MapUtils.isEmpty(rules)) {
            return null;
        }

        List<Map<String, Object>> rulesDetails = new ArrayList<>();
        Collection rulesCollection = rules.values();
        Iterator iter = rulesCollection.iterator();
        while (iter.hasNext()) {
            HashMap<String, String> detail = (HashMap<String, String>)iter.next();
            Map<String, Object> ruleDetails = new HashMap<>();
            String rule = detail.get(ApiConstants.RULE);
            if (Strings.isNullOrEmpty(rule)) {
                throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Empty rule provided in rules param");
            }
            ruleDetails.put(ApiConstants.RULE, new Rule(rule));

            String permission = detail.get(ApiConstants.PERMISSION);
            if (Strings.isNullOrEmpty(permission)) {
                throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Invalid permission: "+ permission + " provided in rules param");
            }
            ruleDetails.put(ApiConstants.PERMISSION, roleService.getRolePermission(permission));

            String description = detail.get(ApiConstants.DESCRIPTION);
            if (!Strings.isNullOrEmpty(permission)) {
                ruleDetails.put(ApiConstants.DESCRIPTION, description);
            }

            rulesDetails.add(ruleDetails);
        }
        return rulesDetails;
    }

    public boolean isForced() {
        return (forced != null) ? forced : false;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() {
        if (getRoleType() == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Invalid role type provided");
        }

        CallContext.current().setEventDetails("Role: " + getRoleName() + ", type: " + getRoleType() + ", description: " + getRoleDescription());
        Role role = roleService.importRole(getRoleName(), getRoleType(), getRoleDescription(), getRules(), isForced());
        if (role == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to import role");
        }
        setupResponse(role);
    }
}
