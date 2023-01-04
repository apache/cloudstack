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
package org.apache.cloudstack.api.command.admin.config;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.HypervisorCapabilitiesResponse;
import org.apache.cloudstack.api.response.ListResponse;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.HypervisorCapabilities;
import com.cloud.utils.Pair;

@APICommand(name = "listHypervisorCapabilities",
            description = "Lists all hypervisor capabilities.",
            responseObject = HypervisorCapabilitiesResponse.class,
            since = "3.0.0",
            requestHasSensitiveInfo = false,
            responseHasSensitiveInfo = false)
public class ListHypervisorCapabilitiesCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListHypervisorCapabilitiesCmd.class.getName());


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = HypervisorCapabilitiesResponse.class, description = "ID of the hypervisor capability")
    private Long id;

    @Parameter(name = ApiConstants.HYPERVISOR, type = CommandType.STRING, description = "the hypervisor for which to restrict the search")
    private String hypervisor;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public HypervisorType getHypervisor() {
        if (hypervisor != null) {
            return HypervisorType.getType(hypervisor);
        } else {
            return null;
        }
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        Pair<List<? extends HypervisorCapabilities>, Integer> hpvCapabilities =
            _mgr.listHypervisorCapabilities(getId(), getHypervisor(), getKeyword(), this.getStartIndex(), this.getPageSizeVal());
        ListResponse<HypervisorCapabilitiesResponse> response = new ListResponse<HypervisorCapabilitiesResponse>();
        List<HypervisorCapabilitiesResponse> hpvCapabilitiesResponses = new ArrayList<HypervisorCapabilitiesResponse>();
        for (HypervisorCapabilities capability : hpvCapabilities.first()) {
            HypervisorCapabilitiesResponse hpvCapabilityResponse = _responseGenerator.createHypervisorCapabilitiesResponse(capability);
            hpvCapabilityResponse.setObjectName("hypervisorCapabilities");
            hpvCapabilitiesResponses.add(hpvCapabilityResponse);
        }

        response.setResponses(hpvCapabilitiesResponses, hpvCapabilities.second());
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
