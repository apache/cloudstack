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

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.DataCenterGuestIpv6PrefixResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ZoneResponse;

import com.cloud.dc.DataCenterGuestIpv6Prefix;

@APICommand(name = "listGuestNetworkIpv6Prefixes",
        description = "Lists guest network IPv6 prefixes",
        responseObject = DataCenterGuestIpv6PrefixResponse.class,
        since = "4.17.0",
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false)

public class ListGuestNetworkIpv6PrefixesCmd extends BaseListCmd {


    @Parameter(name = ApiConstants.ID,
            type = CommandType.UUID,
            entityType = DataCenterGuestIpv6PrefixResponse.class,
            description = "UUID of the IPv6 prefix.")
    private Long id;

    @Parameter(name = ApiConstants.ZONE_ID,
            type = CommandType.UUID,
            entityType = ZoneResponse.class,
            description = "UUID of zone to which the IPv6 prefix belongs to.")
    private Long zoneId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Long getZoneId() {
        return zoneId;
    }

    @Override
    public void execute() {
        List<? extends DataCenterGuestIpv6Prefix> prefixes = _configService.listDataCenterGuestIpv6Prefixes(this);
        ListResponse<DataCenterGuestIpv6PrefixResponse> response = new ListResponse<>();
        List<DataCenterGuestIpv6PrefixResponse> prefixResponses = new ArrayList<>();
        for (DataCenterGuestIpv6Prefix prefix : prefixes) {
            DataCenterGuestIpv6PrefixResponse prefixResponse = _responseGenerator.createDataCenterGuestIpv6PrefixResponse(prefix);
            prefixResponse.setObjectName("guestnetworkipv6prefix");
            prefixResponses.add(prefixResponse);
        }

        response.setResponses(prefixResponses, prefixes.size());
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }

    }
