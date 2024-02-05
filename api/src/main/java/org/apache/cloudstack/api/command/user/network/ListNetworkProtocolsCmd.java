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

import com.cloud.utils.net.NetworkProtocols;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.NetworkProtocolResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

@APICommand(name = "listNetworkProtocols", description = "Lists details of network protocols", responseObject = NetworkProtocolResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        authorized = { RoleType.Admin, RoleType.DomainAdmin, RoleType.ResourceAdmin, RoleType.User}, since = "4.19.0")
public class ListNetworkProtocolsCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(ListNetworkProtocolsCmd.class.getName());


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.OPTION, type = CommandType.STRING, required = true,
            description = "The option of network protocols. Supported values are: protocolnumber, icmptype.")
    private String option;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////


    public String getOption() {
        return option;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        ListResponse<NetworkProtocolResponse> response = new ListResponse<>();
        List<NetworkProtocolResponse> networkProtocolResponses = new ArrayList<>();

        NetworkProtocols.Option option = NetworkProtocols.Option.getOption(getOption());
        switch (option) {
            case ProtocolNumber:
                updateResponseWithProtocolNumbers(networkProtocolResponses);
                break;
            case IcmpType:
                updateResponseWithIcmpTypes(networkProtocolResponses);
                break;
            default:
                break;
        }

        response.setResponses(networkProtocolResponses);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

    private void updateResponseWithProtocolNumbers(List<NetworkProtocolResponse> responses) {
        for (NetworkProtocols.ProtocolNumber protocolNumber : NetworkProtocols.ProtocolNumbers) {
            NetworkProtocolResponse networkProtocolResponse = new NetworkProtocolResponse(protocolNumber.getNumber(),
                    protocolNumber.getKeyword(), protocolNumber.getProtocol());
            networkProtocolResponse.setObjectName("networkprotocol");
            responses.add(networkProtocolResponse);
        }
    }

    private void updateResponseWithIcmpTypes(List<NetworkProtocolResponse> responses) {
        for (NetworkProtocols.IcmpType icmpType : NetworkProtocols.IcmpTypes) {
            NetworkProtocolResponse networkProtocolResponse = new NetworkProtocolResponse(icmpType.getType(),
                    null, icmpType.getDescription());
            for (NetworkProtocols.IcmpCode code : icmpType.getIcmpCodes()) {
                networkProtocolResponse.addDetail(String.valueOf(code.getCode()), code.getDescription());
            }
            networkProtocolResponse.setObjectName("networkprotocol");
            responses.add(networkProtocolResponse);
        }
    }
}
