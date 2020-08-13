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

import org.apache.cloudstack.acl.RolePermissionEntity.Permission;
import org.apache.cloudstack.acl.Rule;
import org.apache.cloudstack.api.ApiArgValidator;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;

import com.google.common.base.Strings;

public abstract class BaseRolePermissionCmd extends BaseCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.RULE, type = CommandType.STRING, required = true, description = "The API name or wildcard rule such as list*",
            validations = {ApiArgValidator.NotNullOrEmpty})
    private String rule;

    @Parameter(name = ApiConstants.PERMISSION, type = CommandType.STRING, required = true, description = "The rule permission, allow or deny. Default: deny.")
    private String permission;

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING, description = "The description of the role permission")
    private String description;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Rule getRule() {
        return new Rule(rule);
    }

    public Permission getPermission() {
        if (Strings.isNullOrEmpty(permission)) {
            return null;
        }
        return Permission.valueOf(permission.toUpperCase());
    }

    public String getDescription() {
        return description;
    }
}
