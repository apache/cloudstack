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
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.NsxControllerResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.service.NsxProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;

import static org.apache.cloudstack.api.command.ListNsxControllersCmd.APINAME;

@APICommand(name = APINAME, description = "list all NSX controllers added to CloudStack",
        responseObject = NsxControllerResponse.class, requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false, since = "4.19.0")
public class ListNsxControllersCmd extends BaseListCmd {
    public static final String APINAME = "listNsxControllers";
    public static final Logger LOGGER = LoggerFactory.getLogger(ListNsxControllersCmd.class.getName());

    @Inject
    private NsxProviderService nsxProviderService;

    @Parameter(name = ApiConstants.ZONE_ID, description = "NSX controller added to the specific zone",
            type = CommandType.UUID, entityType = ZoneResponse.class)
    Long zoneId;

    @Override
    public void execute() throws ServerApiException, ConcurrentOperationException {
        List<BaseResponse> baseResponseList = nsxProviderService.listNsxProviders(zoneId);
        List<BaseResponse> pagingList = StringUtils.applyPagination(baseResponseList, this.getStartIndex(), this.getPageSizeVal());
        ListResponse<BaseResponse> listResponse = new ListResponse<>();
        listResponse.setResponses(pagingList);
        listResponse.setResponseName(getCommandName());
        setResponseObject(listResponse);
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }
}
