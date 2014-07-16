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

package org.apache.cloudstack.network.topology;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.cloud.dc.DataCenter;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.rules.RuleApplier;
import com.cloud.network.rules.RuleApplierWrapper;
import com.cloud.network.rules.UserdataPwdRules;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachineProfile;

public class AdvancedNetworkTopology implements NetworkTopology {

    @Override
    public StringBuilder createGuestBootLoadArgs(final NicProfile guestNic, final String defaultDns1, final String defaultDns2, final DomainRouterVO router) {
        return null;
    }

    @Override
    public String retrieveGuestDhcpRange(final NicProfile guestNic, final Network guestNetwork, final DataCenter dc) {
        return null;
    }

    @Override
    public NicProfile retrieveControlNic(final VirtualMachineProfile profile) {
        return null;
    }

    @Override
    public boolean configDhcpForSubnet(final Network network, final NicProfile nic, final VirtualMachineProfile profile, final DeployDestination dest,
            final List<DomainRouterVO> routers) throws ResourceUnavailableException {
        return false;
    }

    @Override
    public boolean applyDhcpEntry(final Network network, final NicProfile nic, final VirtualMachineProfile profile, final DeployDestination dest,
            final List<DomainRouterVO> routers) throws ResourceUnavailableException {
        return false;
    }

    @Override
    public boolean applyRules(final Network network, final List<? extends VirtualRouter> routers, final String typeString, final boolean isPodLevelException, final Long podId,
            final boolean failWhenDisconnect, final RuleApplier applier) throws ResourceUnavailableException {

        AdvancedNetworkVisitor visitor = new AdvancedNetworkVisitor(this);
        applier.accept(visitor, null);

        return false;
    }

    @Override
    public boolean sendCommandsToRouter(final VirtualRouter router, final List<LoadBalancingRule> rules, final long id) {
        // TODO Auto-generated method stub
        return false;
    }

    private static final Logger s_logger = Logger.getLogger(AdvancedNetworkTopology.class);

    @Autowired
    @Qualifier("advancedNetworkVisitor")
    protected AdvancedNetworkVisitor _advancedVisitor;

    @Override
    public boolean applyUserData(final Network network, final NicProfile nic, final VirtualMachineProfile profile, final DeployDestination dest, final List<DomainRouterVO> routers)
            throws ResourceUnavailableException {

        s_logger.debug("APPLYING USERDATA RULES");

        final String typeString = "userdata and password entry";
        final boolean isPodLevelException = false;
        final boolean failWhenDisconnect = false;
        final Long podId = null;

        UserdataPwdRules pwdRules = _virtualNetworkApplianceFactory.createUserdataPwdRules(network, nic, profile, dest);

        return applyRules(network, routers, typeString, isPodLevelException, podId, failWhenDisconnect, new RuleApplierWrapper<RuleApplier>(pwdRules));
    }
}