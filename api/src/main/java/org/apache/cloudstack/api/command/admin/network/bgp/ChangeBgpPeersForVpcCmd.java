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

package org.apache.cloudstack.api.command.admin.network.bgp;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiArgValidator;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.admin.AdminCmd;
import org.apache.cloudstack.api.response.BgpPeerResponse;
import org.apache.cloudstack.api.response.VpcResponse;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.vpc.Vpc;
import com.cloud.user.Account;
import com.cloud.utils.exception.CloudRuntimeException;

import java.util.List;

@APICommand(name = "changeBgpPeersForVpc",
        description = "Change the BGP peers for a VPC.",
        responseObject = BgpPeerResponse.class,
        since = "4.20.0",
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin})
public class ChangeBgpPeersForVpcCmd extends BaseAsyncCmd implements AdminCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.VPC_ID,
            type = CommandType.UUID,
            entityType = VpcResponse.class,
            required = true,
            description = "UUID of the VPC which the Bgp Peers are associated to.",
            validations = {ApiArgValidator.PositiveNumber})
    private Long vpcId;

    @Parameter(name = ApiConstants.BGP_PEER_IDS,
            type = CommandType.LIST,
            collectionType = CommandType.UUID,
            entityType = BgpPeerResponse.class,
            description = "Ids of the Bgp Peer. If it is empty, all BGP peers will be unlinked.")
    private List<Long> bgpPeerIds;

    public Long getVpcId() {
        return vpcId;
    }

    public List<Long> getBgpPeerIds() {
        return bgpPeerIds;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VPC_BGP_PEER_UPDATE;
    }

    @Override
    public String getEventDescription() {
        return "Changing Bgp Peers for VPC " + getVpcId();
    }

    @Override
    public void execute() {
        try {
            Vpc result = routedIpv4Manager.changeBgpPeersForVpc(this);
            if (result != null) {
                VpcResponse response = _responseGenerator.createVpcResponse(getResponseView(), result);
                response.setResponseName(getCommandName());
                setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to change BGP Peers for vpc");
            }
        } catch (InvalidParameterValueException ex) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, ex.getMessage());
        } catch (CloudRuntimeException ex) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        }

    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
