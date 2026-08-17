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
package org.apache.cloudstack.api.command;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.utils.StringUtils;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.NetrisProviderResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.service.NetrisProviderService;

import javax.inject.Inject;
import java.util.List;

@APICommand(name = ListNetrisProvidersCmd.APINAME, description = "list all Netris providers added to CloudStack",
        responseObject = NetrisProviderResponse.class, requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false, since = "4.21.0")
public class ListNetrisProvidersCmd extends BaseListCmd {
    public static final String APINAME = "listNetrisProviders";

    @Inject
    NetrisProviderService netrisProviderService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    ///
    @Parameter(name = ApiConstants.ZONE_ID, description = "ID of the zone",
            type = CommandType.UUID, entityType = ZoneResponse.class)
    Long zoneId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getZoneId() {
        return zoneId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ServerApiException, ConcurrentOperationException {
        List<BaseResponse> baseResponseList = netrisProviderService.listNetrisProviders(zoneId);
        List<BaseResponse> pagingList = StringUtils.applyPagination(baseResponseList, this.getStartIndex(), this.getPageSizeVal());
        ListResponse<BaseResponse> listResponse = new ListResponse<>();
        listResponse.setResponses(pagingList);
        listResponse.setResponseName(getCommandName());
        setResponseObject(listResponse);
    }
}
