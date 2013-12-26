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
package org.apache.cloudstack.iam.api.command;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListDomainResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.AclGroupResponse;
import org.apache.cloudstack.api.response.ListResponse;


@APICommand(name = "listAclGroups", description = "Lists acl groups", responseObject = AclGroupResponse.class)
public class ListAclGroupsCmd extends BaseListDomainResourcesCmd {
    public static final Logger s_logger = Logger.getLogger(ListAclGroupsCmd.class.getName());

    private static final String s_name = "listaclgroupsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "lists acl groups by name")
    private String aclGroupName;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, description = "list the acl group by the id provided", entityType = AclGroupResponse.class)
    private Long id;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    public String getAclGroupName() {
        return aclGroupName;
    }


    public Long getId(){
        return id;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute(){

        ListResponse<AclGroupResponse> response = _queryService.listAclGroups(id, aclGroupName, getDomainId(),
                getStartIndex(), getPageSizeVal());
        response.setResponseName(getCommandName());
        setResponseObject(response);

    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.AclGroup;
    }
}
