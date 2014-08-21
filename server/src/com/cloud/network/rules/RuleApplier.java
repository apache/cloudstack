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

import org.apache.cloudstack.network.topology.NetworkTopologyVisitor;

import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.lb.LoadBalancingRulesManager;
import com.cloud.network.router.NetworkHelper;
import com.cloud.network.router.NicProfileHelper;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.vpc.NetworkACLManager;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.dao.PrivateIpDao;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.user.dao.UserStatisticsDao;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.NicIpAliasDao;
import com.cloud.vm.dao.UserVmDao;

public abstract class RuleApplier {

    protected NetworkModel _networkModel;
    protected LoadBalancingRulesManager _lbMgr;
    protected LoadBalancerDao _loadBalancerDao;
    protected NicDao _nicDao;
    protected DataCenterDao _dcDao;
    protected UserVmDao _userVmDao;
    protected VMTemplateDao _templateDao;
    protected NetworkDao _networkDao;
    protected UserStatisticsDao _userStatsDao;
    protected VpcDao _vpcDao;
    protected NicIpAliasDao _nicIpAliasDao;
    protected HostPodDao _podDao;
    protected VlanDao _vlanDao;
    protected IPAddressDao _ipAddressDao;
    protected PrivateIpDao _privateIpDao;
    protected VpcManager _vpcMgr;
    protected VirtualMachineManager _itMgr;
    protected IpAddressManager _ipAddrMgr;
    protected NetworkACLManager _networkACLMgr;
    protected Network _network;
    protected VirtualRouter _router;
    protected NetworkHelper _networkHelper;
    protected NicProfileHelper _nicProfileHelper;

    public RuleApplier(final Network network) {
        _network = network;
    }

    public abstract boolean accept(NetworkTopologyVisitor visitor, VirtualRouter router) throws ResourceUnavailableException;

    public Network getNetwork() {
        return _network;
    }

    public VirtualRouter getRouter() {
        return _router;
    }
}