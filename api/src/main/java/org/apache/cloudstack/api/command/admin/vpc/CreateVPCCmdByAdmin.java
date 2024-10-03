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

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.command.admin.AdminCmd;
import org.apache.cloudstack.api.command.user.vpc.CreateVPCCmd;
import org.apache.cloudstack.api.response.BgpPeerResponse;
import org.apache.cloudstack.api.response.VpcResponse;

import com.cloud.network.vpc.Vpc;

import java.util.List;

@APICommand(name = "createVPC", description = "Creates a VPC", responseObject = VpcResponse.class, responseView = ResponseView.Full, entityType = {Vpc.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CreateVPCCmdByAdmin extends CreateVPCCmd implements AdminCmd {
    @Parameter(name = ApiConstants.BGP_PEER_IDS,
            type = CommandType.LIST,
            collectionType = CommandType.UUID,
            entityType = BgpPeerResponse.class,
            description = "Ids of the Bgp Peer for the VPC",
            since = "4.20.0")
    private List<Long> bgpPeerIds;


    public List<Long> getBgpPeerIds() {
        return bgpPeerIds;
    }
}
