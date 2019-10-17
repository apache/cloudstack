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
package org.apache.cloudstack.api.command.admin.network;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.command.admin.AdminCmd;
import org.apache.cloudstack.api.command.user.network.CreateNetworkCmd;
import org.apache.cloudstack.api.response.NetworkResponse;

import com.cloud.network.Network;

@APICommand(name = "createNetwork", description = "Creates a network", responseObject = NetworkResponse.class, responseView = ResponseView.Full, entityType = {Network.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CreateNetworkCmdByAdmin extends CreateNetworkCmd implements AdminCmd {
    public static final Logger s_logger = Logger.getLogger(CreateNetworkCmdByAdmin.class.getName());

    @Parameter(name=ApiConstants.VLAN, type=CommandType.STRING, description="the ID or VID of the network")
    private String vlan;

    @Parameter(name=ApiConstants.BYPASS_VLAN_OVERLAP_CHECK, type=CommandType.BOOLEAN, description="when true bypasses VLAN id/range overlap check during network creation for shared and L2 networks")
    private Boolean bypassVlanOverlapCheck;

    @Parameter(name=ApiConstants.HIDE_IP_ADDRESS_USAGE, type=CommandType.BOOLEAN, description="when true ip address usage for the network will not be exported by the listUsageRecords API")
    private Boolean hideIpAddressUsage;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getVlan() {
        return vlan;
    }

    public Boolean getBypassVlanOverlapCheck() {
        if (bypassVlanOverlapCheck != null) {
            return bypassVlanOverlapCheck;
        }
        return false;
    }

    public Boolean getHideIpAddressUsage() {
        if (hideIpAddressUsage != null) {
            return hideIpAddressUsage;
        }
        return false;
    }
}
