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

import org.apache.cloudstack.acl.Role;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.response.RoleResponse;

public abstract class RoleCmd extends BaseCmd {

    protected void setupResponse(final Role role) {
        final RoleResponse response = new RoleResponse();
        response.setId(role.getUuid());
        response.setRoleName(role.getName());
        response.setRoleType(role.getRoleType());
        response.setDescription(role.getDescription());
        response.setResponseName(getCommandName());
        response.setObjectName("role");
        setResponseObject(response);
    }
}
