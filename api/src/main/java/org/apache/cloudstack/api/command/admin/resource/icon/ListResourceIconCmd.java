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
package org.apache.cloudstack.api.command.admin.resource.icon;

import com.cloud.server.ResourceIcon;
import com.cloud.server.ResourceTag;
import com.cloud.user.Account;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ResourceIconResponse;
import org.apache.log4j.Logger;

import java.util.List;

@APICommand(name = "listResourceIcon", description = "Lists the resource icon for the specified resource(s)",
        responseObject = ResourceIconResponse.class, since = "4.16.0.0", entityType = {ResourceIcon.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin, RoleType.DomainAdmin, RoleType.ResourceAdmin, RoleType.User})
public class ListResourceIconCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(ListResourceIconCmd.class.getName());

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.RESOURCE_IDS,
            type = BaseCmd.CommandType.LIST,
            required = true,
            collectionType = BaseCmd.CommandType.STRING,
            description = "list of resources to upload the icon/image for")
    private List<String> resourceIds;

    @Parameter(name = ApiConstants.RESOURCE_TYPE, type = BaseCmd.CommandType.STRING, required = true, description = "type of the resource")
    private String resourceType;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public List<String> getResourceIds() {
        return resourceIds;
    }

    public ResourceTag.ResourceObjectType getResourceType() {
        return resourceManagerUtil.getResourceType(resourceType);
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        ListResponse<ResourceIconResponse> response = _queryService.listResourceIcons(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
