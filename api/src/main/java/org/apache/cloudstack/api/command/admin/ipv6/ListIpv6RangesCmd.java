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
package org.apache.cloudstack.api.command.admin.ipv6;

import java.util.ArrayList;
import java.util.List;

import com.cloud.network.Ipv6Address;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.BaseListProjectAndAccountResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.log4j.Logger;

import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.PhysicalNetworkResponse;
import org.apache.cloudstack.api.response.Ipv6RangeResponse;
import org.apache.cloudstack.api.response.ZoneResponse;

import com.cloud.utils.Pair;

@APICommand(name = "listIpv6Ranges", description = "Lists all IPv6 ranges.", responseObject = Ipv6RangeResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListIpv6RangesCmd extends BaseListProjectAndAccountResourcesCmd {
    public static final Logger s_logger = Logger.getLogger(ListIpv6RangesCmd.class.getName());

    public static final String APINAME = "listipv6ranges";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = Ipv6RangeResponse.class, required = false, description = "the ID of the IPv6 range")
    private Long id;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, description = "the Zone ID of the IPv6 range")
    private Long zoneId;

    @Parameter(name = ApiConstants.NETWORK_ID, type = CommandType.UUID, entityType = NetworkResponse.class, description = "network id of the IPv6 range")
    private Long networkId;

    @Parameter(name = ApiConstants.PHYSICAL_NETWORK_ID,
               type = CommandType.UUID,
               entityType = PhysicalNetworkResponse.class,
               description = "physical network id of the IPv6 range")
    private Long physicalNetworkId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public Long getNetworkId() {
        return networkId;
    }

    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override
    public void execute() {
        Pair<List<? extends Ipv6Address>, Integer> addrs = ipv6Service.searchForIpv6Range(this);
        ListResponse<Ipv6RangeResponse> response = new ListResponse<Ipv6RangeResponse>();
        List<Ipv6RangeResponse> responses = new ArrayList<Ipv6RangeResponse>();
        for (Ipv6Address address : addrs.first()) {
            Ipv6RangeResponse ipv6RangeResponse = ipv6Service.createIpv6RangeResponse(address);
            ipv6RangeResponse.setObjectName("ipv6range");
            responses.add(ipv6RangeResponse);
        }

        response.setResponses(responses, addrs.second());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
