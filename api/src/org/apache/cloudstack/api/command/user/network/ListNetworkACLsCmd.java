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
import org.apache.cloudstack.api.BaseListTaggedResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.FirewallRuleResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.NetworkACLItemResponse;
import org.apache.cloudstack.api.response.NetworkACLResponse;
import org.apache.cloudstack.api.response.NetworkResponse;

import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.utils.Pair;

@APICommand(name = "listNetworkACLs", description = "Lists all network ACL items", responseObject = NetworkACLItemResponse.class)
public class ListNetworkACLsCmd extends BaseListTaggedResourcesCmd {
    public static final Logger s_logger = Logger.getLogger(ListNetworkACLsCmd.class.getName());

    private static final String s_name = "listnetworkaclsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType = NetworkACLItemResponse.class,
            description="Lists network ACL Item with the specified ID")
    private Long id;

    @Parameter(name = ApiConstants.NETWORK_ID, type = CommandType.UUID, entityType = NetworkResponse.class, description = "list network ACL Items by network Id")
    private Long networkId;

    @Parameter(name = ApiConstants.TRAFFIC_TYPE, type = CommandType.STRING, description = "list network ACL Items by traffic type - Ingress or Egress")
    private String trafficType;

    @Parameter(name = ApiConstants.ACL_ID, type = CommandType.UUID, entityType = NetworkACLResponse.class, description = "list network ACL Items by ACL Id")
    private Long aclId;

    @Parameter(name = ApiConstants.PROTOCOL, type = CommandType.STRING, description = "list network ACL Items by Protocol")
    private String protocol;

    @Parameter(name = ApiConstants.ACTION, type = CommandType.STRING, description = "list network ACL Items by Action")
    private String action;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getNetworkId() {
        return networkId;
    }

    public Long getId() {
        return id;
    }

    public String getTrafficType() {
        return trafficType;
    }

    public Long getAclId() {
        return aclId;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getAction() {
        return action;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute() {
        Pair<List<? extends NetworkACLItem>, Integer> result = _networkACLService.listNetworkACLItems(this);
        ListResponse<NetworkACLItemResponse> response = new ListResponse<NetworkACLItemResponse>();
        List<NetworkACLItemResponse> aclResponses = new ArrayList<NetworkACLItemResponse>();

        for (NetworkACLItem acl : result.first()) {
            NetworkACLItemResponse ruleData = _responseGenerator.createNetworkACLItemResponse(acl);
            aclResponses.add(ruleData);
        }
        response.setResponses(aclResponses, result.second());
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
