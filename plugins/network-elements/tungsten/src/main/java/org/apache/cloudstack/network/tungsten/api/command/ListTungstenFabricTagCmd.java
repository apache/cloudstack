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
package org.apache.cloudstack.network.tungsten.api.command;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.TungstenProvider;
import com.cloud.network.element.TungstenProviderVO;
import com.cloud.user.Account;
import com.cloud.utils.StringUtils;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenFabricTagResponse;
import org.apache.cloudstack.network.tungsten.service.TungstenService;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

@APICommand(name = ListTungstenFabricTagCmd.APINAME, responseObject = TungstenFabricTagResponse.class,
    description = "Lists Tungsten-Fabric tags", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListTungstenFabricTagCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListTungstenFabricTagCmd.class.getName());
    public static final String APINAME = "listTungstenFabricTag";

    @Inject
    TungstenService tungstenService;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, description = "the ID of zone")
    private Long zoneId;

    @Parameter(name = ApiConstants.NETWORK_UUID, type = CommandType.STRING, description = "the uuid of Tungsten-Fabric network")
    private String networkUuid;

    @Parameter(name = ApiConstants.VM_UUID, type = CommandType.STRING, description = "the uuid of Tungsten-Fabric vm")
    private String vmUuid;

    @Parameter(name = ApiConstants.NIC_UUID, type = CommandType.STRING, description = "tthe uuid of Tungsten-Fabric nic")
    private String nicUuid;

    @Parameter(name = ApiConstants.POLICY_UUID, type = CommandType.STRING, description = "the uuid of Tungsten-Fabric policy")
    private String policyUuid;

    @Parameter(name = ApiConstants.APPLICATION_POLICY_SET_UUID, type = CommandType.STRING, description = "the uuid of Tungsten-Fabric application policy set")
    private String applicationPolicySetUuid;

    @Parameter(name = ApiConstants.TAG_UUID, type = CommandType.STRING, description = "the uuid of Tungsten-Fabric tag")
    private String tagUuid;

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
        ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        List<BaseResponse> baseResponseList = new ArrayList<>();
        if (zoneId != null) {
            baseResponseList.addAll(tungstenService.listTungstenTags(zoneId, networkUuid, vmUuid, nicUuid, policyUuid, applicationPolicySetUuid, tagUuid));
        } else {
            List<TungstenProviderVO> tungstenProviderVOList = tungstenService.getTungstenProviders();
            for (TungstenProvider tungstenProvider : tungstenProviderVOList) {
                baseResponseList.addAll(tungstenService.listTungstenTags(tungstenProvider.getZoneId(), networkUuid, vmUuid, nicUuid, policyUuid, applicationPolicySetUuid, tagUuid));
            }
        }
        List<BaseResponse> pagingList = StringUtils.applyPagination(baseResponseList, this.getStartIndex(), this.getPageSizeVal());
        ListResponse<BaseResponse> listResponse = new ListResponse<>();
        listResponse.setResponses(pagingList);
        listResponse.setResponseName(getCommandName());
        setResponseObject(listResponse);
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
