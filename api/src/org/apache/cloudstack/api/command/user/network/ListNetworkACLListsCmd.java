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
package org.apache.cloudstack.api.command.user.network;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.BaseListProjectAndAccountResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.NetworkACLResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.VpcResponse;

import com.cloud.network.vpc.NetworkACL;
import com.cloud.utils.Pair;

@APICommand(name = "listNetworkACLLists", description="Lists all network ACLs", responseObject=NetworkACLResponse.class)
public class ListNetworkACLListsCmd extends BaseListProjectAndAccountResourcesCmd {
    public static final Logger s_logger = Logger.getLogger(ListNetworkACLListsCmd.class.getName());

    private static final String s_name = "listnetworkacllistsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = NetworkACLResponse.class, description = "Lists network ACL with the specified ID.")
    private Long id;

    @Parameter(name = ApiConstants.NETWORK_ID, type = CommandType.UUID, entityType = NetworkResponse.class, description = "list network ACLs by network Id")
    private Long networkId;

    @Parameter(name = ApiConstants.VPC_ID, type = CommandType.UUID, entityType = VpcResponse.class, description = "list network ACLs by Vpc Id")
    private Long vpcId;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "list network ACLs by specified name")
    private String name;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getNetworkId() {
        return networkId;
    }

    public Long getId() {
        return id;
    }

    public Long getVpcId() {
        return vpcId;
    }

    public String getName() {
        return name;
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
        Pair<List<? extends NetworkACL>,Integer> result = _networkACLService.listNetworkACLs(this);
        ListResponse<NetworkACLResponse> response = new ListResponse<NetworkACLResponse>();
        List<NetworkACLResponse> aclResponses = new ArrayList<NetworkACLResponse>();

        for (NetworkACL acl : result.first()) {
            NetworkACLResponse aclResponse = _responseGenerator.createNetworkACLResponse(acl);
            aclResponses.add(aclResponse);
        }
        response.setResponses(aclResponses, result.second());
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
