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
package org.apache.cloudstack.api.command.iam;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.iam.IAMApiService;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListDomainResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.iam.IAMPolicyResponse;


@APICommand(name = "listIAMPolicies", description = "Lists IAM policies", responseObject = IAMPolicyResponse.class)
public class ListIAMPoliciesCmd extends BaseListDomainResourcesCmd {
    public static final Logger s_logger = Logger.getLogger(ListIAMPoliciesCmd.class.getName());

    private static final String s_name = "listiampoliciesresponse";

    @Inject
    public IAMApiService _iamApiSrv;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "lists iam policies by name")
    private String iamPolicyName;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, description = "list the iam policy by the id provided", entityType = IAMPolicyResponse.class)
    private Long id;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    public String getIAMPolicyName() {
        return iamPolicyName;
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

        ListResponse<IAMPolicyResponse> response = _iamApiSrv.listIAMPolicies(id, iamPolicyName, getDomainId(),
                getStartIndex(), getPageSizeVal());
        response.setResponseName(getCommandName());
        setResponseObject(response);

    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.IAMPolicy;
    }
}
