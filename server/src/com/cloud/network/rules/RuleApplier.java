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

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;

import com.cloud.dc.dao.DataCenterDao;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.lb.LoadBalancingRulesManager;
import com.cloud.network.router.NEWVirtualNetworkApplianceManager;
import com.cloud.network.router.RouterControlHelper;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.topology.NetworkTopologyVisitor;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;

public abstract class RuleApplier {

    protected NEWVirtualNetworkApplianceManager applianceManager;

    protected NetworkModel networkModel;

    protected LoadBalancingRulesManager lbMgr;

    protected LoadBalancerDao loadBalancerDao;

    protected ConfigurationDao configDao;

    protected NicDao nicDao;

    protected NetworkOfferingDao networkOfferingDao;

    protected DataCenterDao dcDao;

    protected DomainRouterDao routerDao;

    protected NetworkDao networkDao;

    protected FirewallRulesDao rulesDao;

    protected VirtualMachineManager itMgr;

    protected Network network;

    protected VirtualRouter router;

    protected RouterControlHelper routerControlHelper;

    public RuleApplier(final Network network) {
        this.network = network;
    }

    public abstract boolean accept(NetworkTopologyVisitor visitor, VirtualRouter router) throws ResourceUnavailableException;

    public Network getNetwork() {
        return network;
    }

    public VirtualRouter getRouter() {
        return router;
    }

    public NEWVirtualNetworkApplianceManager getApplianceManager() {
        return applianceManager;
    }
}