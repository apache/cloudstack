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
package org.apache.cloudstack.api.command.user.resource;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.DetailOptionsResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.server.ResourceTag;
import org.apache.commons.lang3.StringUtils;

@APICommand(name = "listDetailOptions",
        description = "Lists all possible details and their options for a resource type such as a VM or a template",
        responseObject = DetailOptionsResponse.class,
        since = "4.13",
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class ListDetailOptionsCmd extends BaseCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.RESOURCE_TYPE, type = CommandType.STRING, required = true,
            description = "the resource type such as UserVm, Template etc.")
    private String resourceType;

    @Parameter(name = ApiConstants.RESOURCE_ID, type = CommandType.STRING,
            description = "the UUID of the resource (optional)")
    private String resourceId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public ResourceTag.ResourceObjectType getResourceType() {
        return resourceManagerUtil.getResourceType(resourceType);
    }

    public String getResourceId() {
        if (StringUtils.isNotEmpty(resourceId)) {
            return resourceManagerUtil.getUuid(resourceId, getResourceType());
        }
        return null;
    }

    /////////////////////////////////////////////////////
    /////////////////// Implementation //////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccountId();
    }

    @Override
    public void execute() {
        final DetailOptionsResponse response = _queryService.listDetailOptions(this);
        response.setResponseName(getCommandName());
        response.setObjectName("detailoptions");
        setResponseObject(response);
    }
}
