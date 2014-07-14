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

import java.util.ArrayList;
import java.util.List;

import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.routing.SetStaticNatRulesCommand;
import com.cloud.agent.api.to.StaticNatRuleTO;
import com.cloud.agent.manager.Commands;
import com.cloud.dc.DataCenterVO;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.topology.NetworkTopologyVisitor;

public class StaticNatRules extends RuleApplier {

    private final List<? extends StaticNat> rules;

    public StaticNatRules(final Network network, final List<? extends StaticNat> rules) {
        super(network);
        this.rules = rules;
    }

    public List<? extends StaticNat> getRules() {
        return rules;
    }

    @Override
    public boolean accept(final NetworkTopologyVisitor visitor, final VirtualRouter router) throws ResourceUnavailableException {
        this.router = router;
        return visitor.visit(this);
    }

    public void createApplyStaticNatCommands(final List<? extends StaticNat> rules, final VirtualRouter router, final Commands cmds, final long guestNetworkId) {
        List<StaticNatRuleTO> rulesTO = new ArrayList<StaticNatRuleTO>();
        if (rules != null) {
            for (final StaticNat rule : rules) {
                final IpAddress sourceIp = networkModel.getIp(rule.getSourceIpAddressId());
                final StaticNatRuleTO ruleTO =
                        new StaticNatRuleTO(0, sourceIp.getAddress().addr(), null, null, rule.getDestIpAddress(), null, null, null, rule.isForRevoke(), false);
                rulesTO.add(ruleTO);
            }
        }

        final SetStaticNatRulesCommand cmd = new SetStaticNatRulesCommand(rulesTO, router.getVpcId());
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, routerControlHelper.getRouterControlIp(router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, routerControlHelper.getRouterIpInNetwork(guestNetworkId, router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());

        final DataCenterVO dcVo = dcDao.findById(router.getDataCenterId());
        cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());
        cmds.addCommand(cmd);
    }
}