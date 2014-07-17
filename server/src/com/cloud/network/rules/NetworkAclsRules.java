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

package com.cloud.network.rules;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.network.topology.NetworkTopologyVisitor;

import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.routing.SetNetworkACLCommand;
import com.cloud.agent.api.to.NetworkACLTO;
import com.cloud.agent.manager.Commands;
import com.cloud.dc.DataCenterVO;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.network.vpc.VpcGateway;

public class NetworkAclsRules extends RuleApplier {

    private final List<? extends NetworkACLItem> _rules;
    private final boolean _isPrivateGateway;

    public NetworkAclsRules(final Network network, final List<? extends NetworkACLItem> rules, final boolean isPrivateGateway) {
        super(network);
        _rules = rules;
        _isPrivateGateway = isPrivateGateway;
    }

    @Override
    public boolean accept(final NetworkTopologyVisitor visitor, final VirtualRouter router) throws ResourceUnavailableException {
        _router = router;

        return visitor.visit(this);
    }

    public List<? extends NetworkACLItem> getRules() {
        return _rules;
    }

    public boolean isPrivateGateway() {
        return _isPrivateGateway;
    }

    public void createNetworkACLsCommands(final List<? extends NetworkACLItem> rules, final VirtualRouter router, final Commands cmds, final long guestNetworkId, final boolean privateGateway) {
        List<NetworkACLTO> rulesTO = new ArrayList<NetworkACLTO>();
        String guestVlan = null;
        Network guestNtwk = _networkDao.findById(guestNetworkId);
        URI uri = guestNtwk.getBroadcastUri();
        if (uri != null) {
            guestVlan = BroadcastDomainType.getValue(uri);
        }

        if (rules != null) {
            for (NetworkACLItem rule : rules) {
                NetworkACLTO ruleTO = new NetworkACLTO(rule, guestVlan, rule.getTrafficType());
                rulesTO.add(ruleTO);
            }
        }

        SetNetworkACLCommand cmd = new SetNetworkACLCommand(rulesTO, _networkHelper.getNicTO(router, guestNetworkId, null));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, _routerControlHelper.getRouterControlIp(router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, _routerControlHelper.getRouterIpInNetwork(guestNetworkId, router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.GUEST_VLAN_TAG, guestVlan);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        DataCenterVO dcVo = _dcDao.findById(router.getDataCenterId());
        cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());
        if (privateGateway) {
            cmd.setAccessDetail(NetworkElementCommand.VPC_PRIVATE_GATEWAY, String.valueOf(VpcGateway.Type.Private));
        }

        cmds.addCommand(cmd);
    }
}