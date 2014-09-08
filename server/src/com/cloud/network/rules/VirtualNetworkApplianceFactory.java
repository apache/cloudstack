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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.RemoteAccessVpn;
import com.cloud.network.VpnUser;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.lb.LoadBalancingRulesManager;
import com.cloud.network.router.NetworkHelper;
import com.cloud.network.router.NicProfileHelper;
import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.network.vpc.NetworkACLManager;
import com.cloud.network.vpc.PrivateGateway;
import com.cloud.network.vpc.StaticRouteProfile;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.dao.PrivateIpDao;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.user.dao.UserStatisticsDao;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.NicIpAliasDao;
import com.cloud.vm.dao.UserVmDao;

public class VirtualNetworkApplianceFactory {

    @Inject
    private NetworkModel _networkModel;
    @Inject
    private LoadBalancingRulesManager _lbMgr;
    @Inject
    private LoadBalancerDao _loadBalancerDao;
    @Inject
    private NicDao _nicDao;
    @Inject
    private VirtualMachineManager _itMgr;
    @Inject
    private DataCenterDao _dcDao;
    @Inject
    private UserVmDao _userVmDao;
    @Inject
    private UserStatisticsDao _userStatsDao;
    @Inject
    private VpcDao _vpcDao;
    @Inject
    private VpcManager _vpcMgr;
    @Inject
    private VMTemplateDao _templateDao;
    @Inject
    private NetworkDao _networkDao;
    @Inject
    private NicIpAliasDao _nicIpAliasDao;
    @Inject
    private HostPodDao _podDao;
    @Inject
    private VlanDao _vlanDao;
    @Inject
    private IPAddressDao _ipAddressDao;
    @Inject
    private PrivateIpDao _privateIpDao;
    @Inject
    private IpAddressManager _ipAddrMgr;
    @Inject
    private NetworkACLManager _networkACLMgr;

    @Autowired
    @Qualifier("networkHelper")
    private NetworkHelper _networkHelper;

    @Inject
    private NicProfileHelper _nicProfileHelper;

    public LoadBalancingRules createLoadBalancingRules(final Network network,
            final List<LoadBalancingRule> rules) {
        LoadBalancingRules lbRules = new LoadBalancingRules(network, rules);

        initBeans(lbRules);

        return lbRules;
    }

    public FirewallRules createFirewallRules(final Network network,
            final List<? extends FirewallRule> rules) {
        FirewallRules fwRules = new FirewallRules(network, rules);

        initBeans(fwRules);

        fwRules._networkDao = _networkDao;

        return fwRules;
    }

    public StaticNatRules createStaticNatRules(final Network network,
            final List<? extends StaticNat> rules) {
        StaticNatRules natRules = new StaticNatRules(network, rules);

        initBeans(natRules);

        return natRules;
    }

    private void initBeans(final RuleApplier applier) {
        applier._networkModel = _networkModel;
        applier._dcDao = _dcDao;
        applier._lbMgr = _lbMgr;
        applier._loadBalancerDao = _loadBalancerDao;
        applier._nicDao = _nicDao;
        applier._itMgr = _itMgr;
        applier._networkHelper = _networkHelper;
    }

    public IpAssociationRules createIpAssociationRules(final Network network, final List<? extends PublicIpAddress> ipAddresses) {
        IpAssociationRules ipAssociationRules = new IpAssociationRules(network, ipAddresses);

        initBeans(ipAssociationRules);

        ipAssociationRules._networkDao = _networkDao;

        return ipAssociationRules;
    }

    public VpcIpAssociationRules createVpcIpAssociationRules(final Network network, final List<? extends PublicIpAddress> ipAddresses) {
        VpcIpAssociationRules ipAssociationRules = new VpcIpAssociationRules(network, ipAddresses);

        initBeans(ipAssociationRules);

        ipAssociationRules._networkDao = _networkDao;

        return ipAssociationRules;
    }

    public BasicVpnRules createBasicVpnRules(final Network network, final List<? extends VpnUser> users) {
        BasicVpnRules vpnRules = new BasicVpnRules(network, users);

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

        return sshKeyToRouterRules;
    }

    public UserdataToRouterRules createUserdataToRouterRules(final Network network, final NicProfile nic, final VirtualMachineProfile profile) {
        UserdataToRouterRules userdataRules = new UserdataToRouterRules(network, nic, profile);

        initBeans(userdataRules);

        userdataRules._userVmDao = _userVmDao;
        userdataRules._templateDao = _templateDao;

        return userdataRules;
    }

    public UserdataPwdRules createUserdataPwdRules(final Network network, final NicProfile nic, final VirtualMachineProfile profile, final DeployDestination destination) {
        UserdataPwdRules userdataRules = new UserdataPwdRules(network, nic, profile, destination);

        initBeans(userdataRules);

        userdataRules._userVmDao = _userVmDao;
        userdataRules._templateDao = _templateDao;

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

        return networkAclsRules;
    }

    public DhcpSubNetRules createDhcpSubNetRules(final Network network, final NicProfile nic, final VirtualMachineProfile profile) {
        DhcpSubNetRules subNetRules = new DhcpSubNetRules(network, nic, profile);

        initBeans(subNetRules);

        subNetRules._vpcDao = _vpcDao;
        subNetRules._userVmDao = _userVmDao;
        subNetRules._podDao = _podDao;
        subNetRules._vlanDao = _vlanDao;
        subNetRules._nicIpAliasDao = _nicIpAliasDao;
        subNetRules._ipAddrMgr = _ipAddrMgr;
        subNetRules._ipAddressDao = _ipAddressDao;

        return subNetRules;
    }

    public DhcpPvlanRules createDhcpPvlanRules(final boolean isAddPvlan, final NicProfile nic) {
        DhcpPvlanRules pvlanRules = new DhcpPvlanRules(isAddPvlan, nic);

        initBeans(pvlanRules);

        pvlanRules._networkDao = _networkDao;

        return pvlanRules;
    }

    public StaticRoutesRules createStaticRoutesRules(final List<StaticRouteProfile> staticRoutes) {
        StaticRoutesRules routesRules = new StaticRoutesRules(staticRoutes);

        initBeans(routesRules);

        return routesRules;
    }

    public AdvancedVpnRules createAdvancedVpnRules(final RemoteAccessVpn remoteAccessVpn, final List<? extends VpnUser> users) {
        AdvancedVpnRules vpnRules = new AdvancedVpnRules(remoteAccessVpn, users);

        initBeans(vpnRules);
        vpnRules._vpcDao = _vpcDao;

        return vpnRules;
    }

    public PrivateGatewayRules createPrivateGatewayRules(final PrivateGateway gateway) {
        PrivateGatewayRules gwRules = new PrivateGatewayRules(gateway);

        initBeans(gwRules);

        gwRules._privateIpDao = _privateIpDao;
        gwRules._networkACLMgr = _networkACLMgr;
        gwRules._nicProfileHelper = _nicProfileHelper;
        gwRules._networkDao = _networkDao;

        return gwRules;
    }
}
