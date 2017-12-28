//
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
//

package com.cloud.api.commands;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;

import javax.inject.Inject;

import java.util.LinkedList;
import java.util.List;

import com.cloud.api.response.NuageVspDomainTemplateResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.manager.NuageVspManager;
import com.cloud.user.Account;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = ListNuageVspGlobalDomainTemplateCmd.APINAME, responseObject = BaseResponse.class, description = "Lists Nuage VSP domain templates", authorized = {RoleType.Admin, RoleType.DomainAdmin, RoleType.User})
public class ListNuageVspGlobalDomainTemplateCmd  extends BaseCmd {
    static final String APINAME = "listNuageVspGlobalDomainTemplate";

    @Inject
    private NuageVspManager _nuageVspManager;

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        try {
            List<NuageVspDomainTemplateResponse> responses = new LinkedList<>();
            NuageVspDomainTemplateResponse answer = new NuageVspDomainTemplateResponse(_nuageVspManager.NuageVspVpcDomainTemplateName.value(),_nuageVspManager.NuageVspVpcDomainTemplateName.value());
            responses.add(answer);

            ListResponse<NuageVspDomainTemplateResponse> response = new ListResponse<>();
            response.setResponses(responses);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } catch (InvalidParameterValueException invalidParamExcp) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, invalidParamExcp.getMessage());
        } catch (CloudRuntimeException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
