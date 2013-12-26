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
import org.apache.cloudstack.api.response.AclPolicyResponse;
import org.apache.cloudstack.api.response.ListResponse;


@APICommand(name = "listAclPolicies", description = "Lists acl policies", responseObject = AclPolicyResponse.class)
public class ListAclPoliciesCmd extends BaseListDomainResourcesCmd {
    public static final Logger s_logger = Logger.getLogger(ListAclPoliciesCmd.class.getName());

    private static final String s_name = "listaclpoliciesresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "lists acl policies by name")
    private String aclPolicyName;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, description = "list the acl policy by the id provided", entityType = AclPolicyResponse.class)
    private Long id;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    public String getAclPolicyName() {
        return aclPolicyName;
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

        ListResponse<AclPolicyResponse> response = _queryService.listAclPolicies(id, aclPolicyName, getDomainId(),
                getStartIndex(), getPageSizeVal());
        response.setResponseName(getCommandName());
        setResponseObject(response);

    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.AclPolicy;
    }
}
