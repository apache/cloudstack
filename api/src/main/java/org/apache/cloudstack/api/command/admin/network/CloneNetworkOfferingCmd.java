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

import java.util.List;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.NetworkOfferingResponse;

import com.cloud.offering.NetworkOffering;

@APICommand(name = "cloneNetworkOffering",
        description = "Clones a network offering. All parameters are copied from the source offering unless explicitly overridden. " +
                "Use 'addServices' and 'dropServices' to modify the service list without respecifying everything.",
        responseObject = NetworkOfferingResponse.class,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        since = "4.23.0")
public class CloneNetworkOfferingCmd extends NetworkOfferingBaseCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.SOURCE_OFFERING_ID,
            type = BaseCmd.CommandType.UUID,
            entityType = NetworkOfferingResponse.class,
            required = true,
            description = "The ID of the network offering to clone")
    private Long sourceOfferingId;

    @Parameter(name = "addservices",
            type = CommandType.LIST,
            collectionType = CommandType.STRING,
            description = "Services to add to the cloned offering (in addition to source offering services). " +
                    "If specified along with 'supportedservices', this parameter is ignored.")
    private List<String> addServices;

    @Parameter(name = "dropservices",
            type = CommandType.LIST,
            collectionType = CommandType.STRING,
            description = "Services to remove from the cloned offering (that exist in source offering). " +
                    "If specified along with 'supportedservices', this parameter is ignored.")
    private List<String> dropServices;

    @Parameter(name = ApiConstants.TRAFFIC_TYPE,
            type = CommandType.STRING,
            description = "The traffic type for the network offering. Supported type in current release is GUEST only")
    private String traffictype;

    @Parameter(name = ApiConstants.GUEST_IP_TYPE, type = CommandType.STRING, description = "Guest type of the network offering: Shared or Isolated")
    private String guestIptype;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getSourceOfferingId() {
        return sourceOfferingId;
    }

    public List<String> getAddServices() {
        return addServices;
    }

    public List<String> getDropServices() {
        return dropServices;
    }

    public String getGuestIpType() {
        return guestIptype;
    }

    public String getTraffictype() {
        return traffictype;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        NetworkOffering result = _configService.cloneNetworkOffering(this);
        if (result != null) {
            NetworkOfferingResponse response = _responseGenerator.createNetworkOfferingResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to clone network offering");
        }
    }
}

