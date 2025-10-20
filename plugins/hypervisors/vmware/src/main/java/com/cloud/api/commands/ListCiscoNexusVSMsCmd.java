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

package com.cloud.api.commands;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;


import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ClusterResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ZoneResponse;

import com.cloud.api.response.CiscoNexusVSMResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.CiscoNexusVSMDevice;
import com.cloud.network.element.CiscoNexusVSMElementService;
import com.cloud.user.Account;

@APICommand(name = "listCiscoNexusVSMs",
            responseObject = CiscoNexusVSMResponse.class,
            description = "Retrieves a Cisco Nexus 1000v Virtual Switch Manager device associated with a Cluster",
            requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListCiscoNexusVSMsCmd extends BaseListCmd {

    /**
     * This command returns a list of all the VSMs configured in the management server.
     * If a clusterId is specified, it will return a list containing only that VSM
     * that is associated with that cluster. If a zone is specified, it will pull
     * up all the clusters of type vmware in that zone, and prepare a list of VSMs
     * associated with those clusters.
     */
    private static final String s_name = "listcisconexusvsmscmdresponse";
    @Inject
    CiscoNexusVSMElementService _ciscoNexusVSMService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.CLUSTER_ID,
               type = CommandType.UUID,
               entityType = ClusterResponse.class,
               required = false,
               description = "Id of the CloudStack cluster in which the Cisco Nexus 1000v VSM appliance.")
    private long clusterId;

    @Parameter(name = ApiConstants.ZONE_ID,
               type = CommandType.UUID,
               entityType = ZoneResponse.class,
               required = false,
               description = "Id of the CloudStack cluster in which the Cisco Nexus 1000v VSM appliance.")
    private long zoneId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public long getClusterId() {
        return clusterId;
    }

    public long getZoneId() {
        return zoneId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    // NOTE- The uuid that is sent in during the invocation of the API AddCiscoNexusVSM()
    // automagically gets translated to the corresponding db id before this execute() method
    // is invoked. That's the reason why we don't have any uuid-dbid translation code here.
    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException,
        ResourceAllocationException {
        List<? extends CiscoNexusVSMDevice> vsmDeviceList = _ciscoNexusVSMService.getCiscoNexusVSMs(this);

        if (vsmDeviceList.size() > 0) {
            ListResponse<CiscoNexusVSMResponse> response = new ListResponse<CiscoNexusVSMResponse>();
            List<CiscoNexusVSMResponse> vsmResponses = new ArrayList<CiscoNexusVSMResponse>();
            for (CiscoNexusVSMDevice vsmDevice : vsmDeviceList) {
                CiscoNexusVSMResponse vsmresponse = _ciscoNexusVSMService.createCiscoNexusVSMDetailedResponse(vsmDevice);
                vsmresponse.setObjectName("cisconexusvsm");
                response.setResponseName(getCommandName());
                vsmResponses.add(vsmresponse);
            }
            response.setResponses(vsmResponses);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "No VSM found.");
        }
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
