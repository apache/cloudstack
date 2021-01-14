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

import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.VlanDao;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.utils.TungstenUtils;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.network.tungsten.agent.api.ApplyTungstenNetworkPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenFloatingIpPoolCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenNetworkPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenFabricNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.TungstenAnswer;
import org.apache.cloudstack.network.tungsten.model.TungstenRule;
import org.apache.cloudstack.network.tungsten.service.TungstenFabricUtils;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

@APICommand(name = "createTungstenPublicNetwork", description = "create tungsten public network", responseObject =
    SuccessResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CreateTungstenPublicNetworkCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateTungstenPublicNetworkCmd.class.getName());

    private static final String s_name = "createtungstenpublicnetworkresponse";

    @Inject
    VlanDao _vlanDao;
    @Inject
    NetworkModel _networkModel;
    @Inject
    TungstenFabricUtils _tungstenFabricUtils;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, required = true
        , description = "the ID of zone")
    private Long zoneId;

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(final Long zoneId) {
        this.zoneId = zoneId;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
        ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        Network publicNetwork = _networkModel.getSystemNetworkByZoneAndTrafficType(zoneId, Networks.TrafficType.Public);

        // create public ip address
        SearchCriteria<VlanVO> sc = _vlanDao.createSearchCriteria();
        sc.setParameters("network_id", publicNetwork.getId());
        VlanVO pubVlanVO = _vlanDao.findOneBy(sc);
        String[] ipAddress = pubVlanVO.getIpRange().split("-");
        String publicNetworkCidr = NetUtils.getCidrFromGatewayAndNetmask(pubVlanVO.getVlanGateway(),
            pubVlanVO.getVlanNetmask());
        Pair<String, Integer> publicPair = NetUtils.getCidr(publicNetworkCidr);

        // create public network
        CreateTungstenNetworkCommand createTungstenPublicNetworkCommand = new CreateTungstenNetworkCommand(
            publicNetwork.getUuid(), TungstenUtils.getPublicNetworkName(zoneId), null, true, false, publicPair.first(),
            publicPair.second(), pubVlanVO.getVlanGateway(), true, null, ipAddress[0], ipAddress[1], false, false);
        TungstenAnswer createPublicNetworkAnswer = _tungstenFabricUtils.sendTungstenCommand(
            createTungstenPublicNetworkCommand, zoneId);
        if (!createPublicNetworkAnswer.getResult()) {
            throw new CloudRuntimeException("can not create tungsten public network");
        }

        List<TungstenRule> tungstenRuleList = new ArrayList<>();
        tungstenRuleList.add(
            new TungstenRule(null, TungstenUtils.DENY_ACTION, TungstenUtils.ONE_WAY_DIRECTION, TungstenUtils.ANY_PROTO,
                TungstenUtils.ALL_IP4_PREFIX, 0, -1, -1, publicPair.first(), publicPair.second(), -1, -1));

        // create default public network policy rule
        CreateTungstenNetworkPolicyCommand createTungstenNetworkPolicyCommand = new CreateTungstenNetworkPolicyCommand(
            TungstenUtils.getVirtualNetworkPolicyName(publicNetwork.getId()), null, tungstenRuleList);
        TungstenAnswer createTungstenNetworkPolicyAnswer = _tungstenFabricUtils.sendTungstenCommand(
            createTungstenNetworkPolicyCommand, zoneId);
        if (!createTungstenNetworkPolicyAnswer.getResult()) {
            throw new CloudRuntimeException("can not create tungsten public network policy");
        }

        // apply network policy
        ApplyTungstenNetworkPolicyCommand applyTungstenNetworkPolicyCommand = new ApplyTungstenNetworkPolicyCommand(
            null, TungstenUtils.getVirtualNetworkPolicyName(publicNetwork.getId()), publicNetwork.getUuid(), false);
        TungstenAnswer applyNetworkPolicyAnswer = _tungstenFabricUtils.sendTungstenCommand(
            applyTungstenNetworkPolicyCommand, zoneId);
        if (!applyNetworkPolicyAnswer.getResult()) {
            throw new CloudRuntimeException("can not apply default tungsten public network policy");
        }

        // change default tungsten security group
        // change default forwarding mode

        // consider policy to protect fabric network
        List<TungstenRule> fabricRuleList = new ArrayList<>();
        fabricRuleList.add(
            new TungstenRule(null, TungstenUtils.PASS_ACTION, TungstenUtils.TWO_WAY_DIRECTION, TungstenUtils.ANY_PROTO,
                TungstenUtils.ALL_IP4_PREFIX, 0, -1, -1, TungstenUtils.ALL_IP4_PREFIX, 0, -1, -1));


        GetTungstenFabricNetworkCommand getTungstenFabricNetworkCommand = new GetTungstenFabricNetworkCommand();
        TungstenAnswer getTungstenFabricNetworkAnswer = _tungstenFabricUtils.sendTungstenCommand(
            getTungstenFabricNetworkCommand, zoneId);
        if (!getTungstenFabricNetworkAnswer.getResult()) {
            throw new CloudRuntimeException("can not get tungsten fabric network");
        }

        // create default public network policy rule
        CreateTungstenNetworkPolicyCommand createFabricNetworkPolicyCommand = new CreateTungstenNetworkPolicyCommand(
            TungstenUtils.getFabricNetworkPolicyName(), null, fabricRuleList);
        TungstenAnswer createfabricNetworkPolicyAnswer = _tungstenFabricUtils.sendTungstenCommand(
            createFabricNetworkPolicyCommand, zoneId);
        if (!createfabricNetworkPolicyAnswer.getResult()) {
            throw new CloudRuntimeException("can not create default tungsten fabric network policy");
        }

        // apply fabric network policy
        ApplyTungstenNetworkPolicyCommand applyTungstenFabricNetworkPolicyCommand =
            new ApplyTungstenNetworkPolicyCommand(
            null, TungstenUtils.getFabricNetworkPolicyName(),
            getTungstenFabricNetworkAnswer.getApiObjectBase().getUuid(), false);
        TungstenAnswer applyNetworkFabricNetworkPolicyAnswer = _tungstenFabricUtils.sendTungstenCommand(
            applyTungstenFabricNetworkPolicyCommand, zoneId);
        if (!applyNetworkFabricNetworkPolicyAnswer.getResult()) {
            throw new CloudRuntimeException("can not apply default tungsten fabric network policy");
        }

        // create floating ip pool
        CreateTungstenFloatingIpPoolCommand createTungstenFloatingIpPoolCommand =
            new CreateTungstenFloatingIpPoolCommand(
            publicNetwork.getUuid(), TungstenUtils.getFloatingIpPoolName(zoneId));
        TungstenAnswer createFloatingIpPoolAnswer = _tungstenFabricUtils.sendTungstenCommand(
            createTungstenFloatingIpPoolCommand, zoneId);
        if (!createFloatingIpPoolAnswer.getResult()) {
            throw new CloudRuntimeException("can not create tungsten floating ip pool");
        }

        SuccessResponse response = new SuccessResponse(getCommandName());
        response.setDisplayText("create tungsten public network successfully");
        setResponseObject(response);
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
