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

import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;

import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.VpnUser;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.lb.LoadBalancingRulesManager;
import com.cloud.network.router.NetworkGeneralHelper;
import com.cloud.network.router.RouterControlHelper;
import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.user.dao.UserStatisticsDao;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;

public class VirtualNetworkApplianceFactory {

    @Inject
    protected NetworkModel _networkModel;

    @Inject
    protected LoadBalancingRulesManager _lbMgr;

    @Inject
    protected LoadBalancerDao _loadBalancerDao;

    @Inject
    protected ConfigurationDao _configDao;

    @Inject
    protected NicDao _nicDao;

    @Inject
    protected VirtualMachineManager _itMgr;

    @Inject
    protected NetworkOfferingDao _networkOfferingDao;

    @Inject
    protected DataCenterDao _dcDao;

    @Inject
    protected UserVmDao _userVmDao;

    @Inject
    protected UserStatisticsDao _userStatsDao;

    @Inject
    protected VpcDao _vpcDao;

    @Inject
    protected VpcManager _vpcMgr;

    @Inject
    protected ServiceOfferingDao _serviceOfferingDao;

    @Inject
    protected VMTemplateDao _templateDao;

    @Inject
    protected DomainRouterDao _routerDao;

    @Inject
    protected NetworkDao _networkDao;

    @Inject
    protected FirewallRulesDao _rulesDao;

    @Inject
    protected RouterControlHelper _routerControlHelper;

    @Inject
    protected NetworkGeneralHelper _networkHelper;

    public LoadBalancingRules createLoadBalancingRules(final Network network, final List<LoadBalancingRule> rules) {
        LoadBalancingRules lbRules = new LoadBalancingRules(network, rules);

        initBeans(lbRules);

        return lbRules;
    }

    public FirewallRules createFirewallRules(final Network network, final List<? extends FirewallRule> rules) {
        FirewallRules fwRules = new FirewallRules(network, rules);

        initBeans(fwRules);

        fwRules._networkDao = _networkDao;
        fwRules._rulesDao = _rulesDao;

        return fwRules;
    }

    public StaticNatRules createStaticNatRules(final Network network, final List<? extends StaticNat> rules) {
        StaticNatRules natRules = new StaticNatRules(network, rules);

        initBeans(natRules);

        return natRules;
    }

    private void initBeans(final RuleApplier applier) {
        applier._networkModel = _networkModel;
        applier._dcDao = _dcDao;
        applier._lbMgr = _lbMgr;
        applier._loadBalancerDao = _loadBalancerDao;
        applier._configDao = _configDao;
        applier._nicDao = _nicDao;
        applier._itMgr = _itMgr;
        applier._networkOfferingDao = _networkOfferingDao;
        applier._routerDao = _routerDao;
        applier._routerControlHelper = _routerControlHelper;
    }

    public IpAssociationRules createIpAssociationRules(final Network network, final List<? extends PublicIpAddress> ipAddresses) {
        IpAssociationRules ipAssociationRules = new IpAssociationRules(network, ipAddresses);

        initBeans(ipAssociationRules);

        ipAssociationRules._networkDao = _networkDao;

        return ipAssociationRules;
    }

    public VpcIpAssociationRules createVpcIpAssociationRules(final Network network, final List<? extends PublicIpAddress> ipAddresses, final NicPlugInOutRules nicPlugInOutRules) {
        VpcIpAssociationRules ipAssociationRules = new VpcIpAssociationRules(network, ipAddresses, nicPlugInOutRules);

        initBeans(ipAssociationRules);

        ipAssociationRules._networkDao = _networkDao;

        return ipAssociationRules;
    }

    public VpnRules createVpnRules(final Network network, final List<? extends VpnUser> users) {
        VpnRules vpnRules = new VpnRules(network, users);

        initBeans(vpnRules);

        return vpnRules;
    }

    public PasswordToRouterRules createPasswordToRouterRules(final Network network, final NicProfile nic, final VirtualMachineProfile profile) {
        PasswordToRouterRules routerRules = new PasswordToRouterRules(network, nic, profile);

        initBeans(routerRules);

        routerRules._userVmDao = _userVmDao;

        return routerRules;
    }

    public SshKeyToRouterRules createSshKeyToRouterRules(final Network network, final NicProfile nic, final VirtualMachineProfile profile, final String sshKey) {
        SshKeyToRouterRules sshKeyToRouterRules = new SshKeyToRouterRules(network, nic, profile, sshKey);

        initBeans(sshKeyToRouterRules);

        sshKeyToRouterRules._userVmDao = _userVmDao;
        sshKeyToRouterRules._templateDao = _templateDao;
        sshKeyToRouterRules._serviceOfferingDao = _serviceOfferingDao;

        return sshKeyToRouterRules;
    }

    public UserdataToRouterRules createUserdataToRouterRules(final Network network, final NicProfile nic, final VirtualMachineProfile profile) {
        UserdataToRouterRules userdataRules = new UserdataToRouterRules(network, nic, profile);

        initBeans(userdataRules);

        userdataRules._userVmDao = _userVmDao;
        userdataRules._templateDao = _templateDao;
        userdataRules._serviceOfferingDao = _serviceOfferingDao;

        return userdataRules;
    }

    public UserdataPwdRules createUserdataPwdRules(final Network network, final NicProfile nic, final VirtualMachineProfile profile, final DeployDestination destination) {
        UserdataPwdRules userdataRules = new UserdataPwdRules(network, nic, profile, destination);

        initBeans(userdataRules);

        userdataRules._userVmDao = _userVmDao;
        userdataRules._templateDao = _templateDao;
        userdataRules._serviceOfferingDao = _serviceOfferingDao;

        return userdataRules;
    }

    public DhcpEntryRules createDhcpEntryRules(final Network network, final NicProfile nic, final VirtualMachineProfile profile, final DeployDestination destination) {
        DhcpEntryRules dhcpRules = new DhcpEntryRules(network, nic, profile, destination);

        initBeans(dhcpRules);

        dhcpRules._userVmDao = _userVmDao;
        dhcpRules._networkDao = _networkDao;

        return dhcpRules;
    }

    public NicPlugInOutRules createNicPluInOutRules(final Network network, final List<? extends PublicIpAddress> ipAddresses) {
        NicPlugInOutRules nicPlug = new NicPlugInOutRules(network, ipAddresses);

        initBeans(nicPlug);

        nicPlug._vpcDao = _vpcDao;
        nicPlug._userStatsDao = _userStatsDao;
        nicPlug._vpcMgr = _vpcMgr;

        return nicPlug;
    }

    public NetworkAclsRules createNetworkAclRules(final Network network, final List<? extends NetworkACLItem> rules, final boolean isPrivateGateway) {
        NetworkAclsRules networkAclsRules = new NetworkAclsRules(network, rules, isPrivateGateway);

        initBeans(networkAclsRules);

        networkAclsRules._networkHelper = _networkHelper;

        return networkAclsRules;
    }
}