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
package org.apache.cloudstack.api.command.admin.guest;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.GuestOSResponse;
import org.apache.cloudstack.api.response.GuestOsMappingResponse;
import org.apache.cloudstack.api.response.ListResponse;

import com.cloud.storage.GuestOSHypervisor;
import com.cloud.utils.Pair;

@APICommand(name = "listGuestOsMapping", description = "Lists all available OS mappings for given hypervisor", responseObject = GuestOsMappingResponse.class,
        since = "4.4.0", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListGuestOsMappingCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListGuestOsMappingCmd.class.getName());


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = GuestOsMappingResponse.class, required = false, description = "list mapping by its UUID")
    private Long id;

    @Parameter(name = ApiConstants.OS_TYPE_ID, type = CommandType.UUID, entityType = GuestOSResponse.class, required = false, description = "list mapping by Guest OS Type UUID")
    private Long osTypeId;

    @Parameter(name = ApiConstants.HYPERVISOR, type = CommandType.STRING, required = false, description = "list Guest OS mapping by hypervisor")
    private String hypervisor;

    @Parameter(name = ApiConstants.HYPERVISOR_VERSION, type = CommandType.STRING, required = false, description = "list Guest OS mapping by hypervisor version. Must be used with hypervisor parameter")
    private String hypervisorVersion;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Long getOsTypeId() {
        return osTypeId;
    }

    public String getHypervisor() {
        return hypervisor;
    }

    public String getHypervisorVersion() {
        return hypervisorVersion;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        Pair<List<? extends GuestOSHypervisor>, Integer> result = _mgr.listGuestOSMappingByCriteria(this);
        ListResponse<GuestOsMappingResponse> response = new ListResponse<GuestOsMappingResponse>();
        List<GuestOsMappingResponse> osMappingResponses = new ArrayList<GuestOsMappingResponse>();
        for (GuestOSHypervisor guestOSHypervisor : result.first()) {
            GuestOsMappingResponse guestOsMappingResponse = _responseGenerator.createGuestOSMappingResponse(guestOSHypervisor);
            osMappingResponses.add(guestOsMappingResponse);
        }

        response.setResponses(osMappingResponses, result.second());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
