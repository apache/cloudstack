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
package org.apache.cloudstack.api.command.admin.vpc;

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.command.user.vpc.ListVPCsCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.VpcResponse;
import org.apache.log4j.Logger;

import com.cloud.network.vpc.Vpc;
import com.cloud.utils.Pair;


@APICommand(name = "listVPCs", description = "Lists VPCs", responseObject = VpcResponse.class, responseView = ResponseView.Full, entityType = {Vpc.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListVPCsCmdByAdmin extends ListVPCsCmd {
    public static final Logger s_logger = Logger.getLogger(ListVPCsCmdByAdmin.class.getName());

    @Override
    public void execute() {
        Pair<List<? extends Vpc>, Integer> vpcs =
                _vpcService.listVpcs(getId(), getVpcName(), getDisplayText(), getSupportedServices(), getCidr(), getVpcOffId(), getState(), getAccountName(), getDomainId(),
                        getKeyword(), getStartIndex(), getPageSizeVal(), getZoneId(), isRecursive(), listAll(), getRestartRequired(), getTags(),
                        getProjectId(), getDisplay());
        ListResponse<VpcResponse> response = new ListResponse<VpcResponse>();
        List<VpcResponse> vpcResponses = new ArrayList<VpcResponse>();
        for (Vpc vpc : vpcs.first()) {
            VpcResponse offeringResponse = _responseGenerator.createVpcResponse(ResponseView.Full, vpc);
            vpcResponses.add(offeringResponse);
        }

        response.setResponses(vpcResponses, vpcs.second());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

}
