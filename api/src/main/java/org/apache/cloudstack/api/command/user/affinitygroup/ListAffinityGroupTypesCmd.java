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
package org.apache.cloudstack.api.command.user.affinitygroup;

import java.util.ArrayList;
import java.util.List;


import org.apache.cloudstack.affinity.AffinityGroupTypeResponse;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.response.ListResponse;

import com.cloud.user.Account;

@APICommand(name = "listAffinityGroupTypes", description = "Lists affinity group types available", responseObject = AffinityGroupTypeResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListAffinityGroupTypesCmd extends BaseListCmd {


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() {
        List<String> result = _affinityGroupService.listAffinityGroupTypes();
        ListResponse<AffinityGroupTypeResponse> response = new ListResponse<AffinityGroupTypeResponse>();
        ArrayList<AffinityGroupTypeResponse> responses = new ArrayList<AffinityGroupTypeResponse>();
        if (result != null) {
            for (String type : result) {
                AffinityGroupTypeResponse affinityTypeResponse = new AffinityGroupTypeResponse();
                affinityTypeResponse.setType(type);
                affinityTypeResponse.setObjectName("affinityGroupType");
                responses.add(affinityTypeResponse);
            }
        }
        response.setResponses(responses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
