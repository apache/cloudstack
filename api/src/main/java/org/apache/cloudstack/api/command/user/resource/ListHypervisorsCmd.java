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
package org.apache.cloudstack.api.command.user.resource;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.HypervisorResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ZoneResponse;

import com.cloud.user.Account;

@APICommand(name = "listHypervisors", description = "List hypervisors", responseObject = HypervisorResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListHypervisorsCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(ListHypervisorsCmd.class.getName());

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, description = "the zone id for listing hypervisors.")
    private Long zoneId;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public Long getZoneId() {
        return this.zoneId;
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////
    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() {
        List<String> result = _mgr.getHypervisors(getZoneId());
        ListResponse<HypervisorResponse> response = new ListResponse<HypervisorResponse>();
        ArrayList<HypervisorResponse> responses = new ArrayList<HypervisorResponse>();
        if (result != null) {
            for (String hypervisor : result) {
                HypervisorResponse hypervisorResponse = new HypervisorResponse();
                hypervisorResponse.setName(hypervisor);
                hypervisorResponse.setObjectName("hypervisor");
                responses.add(hypervisorResponse);
            }
        }
        response.setResponses(responses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
