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
package org.apache.cloudstack.api.command.admin.region;

import java.util.ArrayList;
import java.util.List;


import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.PortableIpRangeResponse;
import org.apache.cloudstack.api.response.PortableIpResponse;
import org.apache.cloudstack.region.PortableIp;
import org.apache.cloudstack.region.PortableIpRange;

import com.cloud.user.Account;

@APICommand(name = "listPortableIpRanges", description = "list portable IP ranges", responseObject = PortableIpRangeResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListPortableIpRangesCmd extends BaseListCmd {


    private static final String s_name = "listportableipresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.REGION_ID, type = CommandType.INTEGER, required = false, description = "Id of a Region")
    private Integer regionId;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, required = false, entityType = PortableIpRangeResponse.class, description = "Id of the portable ip range")
    private Long id;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Integer getRegionIdId() {
        return regionId;
    }

    public Long getPortableIpRangeId() {
        return id;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() {
        ListResponse<PortableIpRangeResponse> response = new ListResponse<PortableIpRangeResponse>();
        List<PortableIpRangeResponse> responses = new ArrayList<PortableIpRangeResponse>();

        List<? extends PortableIpRange> portableIpRanges = _configService.listPortableIpRanges(this);
        if (portableIpRanges != null && !portableIpRanges.isEmpty()) {
            for (PortableIpRange range : portableIpRanges) {
                PortableIpRangeResponse rangeResponse = _responseGenerator.createPortableIPRangeResponse(range);

                List<? extends PortableIp> portableIps = _configService.listPortableIps(range.getId());
                if (portableIps != null && !portableIps.isEmpty()) {
                    List<PortableIpResponse> portableIpResponses = new ArrayList<PortableIpResponse>();
                    for (PortableIp portableIP : portableIps) {
                        PortableIpResponse portableIpresponse = _responseGenerator.createPortableIPResponse(portableIP);
                        portableIpResponses.add(portableIpresponse);
                    }
                    rangeResponse.setPortableIpResponses(portableIpResponses);
                }
                responses.add(rangeResponse);
            }
            response.setResponses(responses, portableIpRanges.size());
        }
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
