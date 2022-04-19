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

import org.apache.commons.lang3.BooleanUtils;
import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.command.admin.AdminCmd;
import org.apache.cloudstack.api.command.user.vpc.CreatePrivateGatewayCmd;
import org.apache.cloudstack.api.response.PhysicalNetworkResponse;
import org.apache.cloudstack.api.response.PrivateGatewayResponse;

import com.cloud.network.vpc.VpcGateway;

@APICommand(name = "createPrivateGateway", description = "Creates a private gateway",
        responseObject = PrivateGatewayResponse.class,
        responseView = ResponseView.Full,
        entityType = {VpcGateway.class},
        since = "4.17.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CreatePrivateGatewayByAdminCmd extends CreatePrivateGatewayCmd implements AdminCmd {
    public static final Logger s_logger = Logger.getLogger(CreatePrivateGatewayByAdminCmd.class.getName());

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.PHYSICAL_NETWORK_ID,
            type = CommandType.UUID,
            entityType = PhysicalNetworkResponse.class,
            description = "the Physical Network ID the network belongs to")
    private Long physicalNetworkId;

    @Parameter(name = ApiConstants.VLAN, type = CommandType.STRING, description = "the network implementation uri for the private gateway")
    private String broadcastUri;

    @Parameter(name = ApiConstants.BYPASS_VLAN_OVERLAP_CHECK, type = CommandType.BOOLEAN, description = "when true bypasses VLAN id/range overlap check during private gateway creation")
    private Boolean bypassVlanOverlapCheck;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////


    public String getBroadcastUri() {
        return broadcastUri;
    }

    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    public Boolean getBypassVlanOverlapCheck() {
        return BooleanUtils.toBoolean(bypassVlanOverlapCheck);
    }
}