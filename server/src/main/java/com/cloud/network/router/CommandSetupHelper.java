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
package com.cloud.network.router;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.network.lb.LoadBalancerConfigKey;
import org.apache.cloudstack.network.lb.LoadBalancerConfigManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.cloud.agent.api.SetupGuestNetworkCommand;
import com.cloud.agent.api.routing.CreateIpAliasCommand;
import com.cloud.agent.api.routing.DeleteIpAliasCommand;
import com.cloud.agent.api.routing.DhcpEntryCommand;
import com.cloud.agent.api.routing.DnsMasqConfigCommand;
import com.cloud.agent.api.routing.IpAliasTO;
import com.cloud.agent.api.routing.IpAssocCommand;
import com.cloud.agent.api.routing.IpAssocVpcCommand;
import com.cloud.agent.api.routing.LoadBalancerConfigCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.routing.RemoteAccessVpnCfgCommand;
import com.cloud.agent.api.routing.SavePasswordCommand;
import com.cloud.agent.api.routing.SetFirewallRulesCommand;
import com.cloud.agent.api.routing.SetIpv6FirewallRulesCommand;
import com.cloud.agent.api.routing.SetNetworkACLCommand;
import com.cloud.agent.api.routing.SetPortForwardingRulesCommand;
import com.cloud.agent.api.routing.SetPortForwardingRulesVpcCommand;
import com.cloud.agent.api.routing.SetSourceNatCommand;
import com.cloud.agent.api.routing.SetStaticNatRulesCommand;
import com.cloud.agent.api.routing.SetStaticRouteCommand;
import com.cloud.agent.api.routing.Site2SiteVpnCfgCommand;
import com.cloud.agent.api.routing.UpdateNetworkCommand;
import com.cloud.agent.api.routing.VmDataCommand;
import com.cloud.agent.api.routing.VpnUsersCfgCommand;
import com.cloud.agent.api.to.DhcpTO;
import com.cloud.agent.api.to.FirewallRuleTO;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.agent.api.to.LoadBalancerConfigTO;
import com.cloud.agent.api.to.LoadBalancerTO;
import com.cloud.agent.api.to.NetworkACLTO;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.PortForwardingRuleTO;
import com.cloud.agent.api.to.StaticNatRuleTO;
import com.cloud.agent.manager.Commands;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.domain.Domain;
import com.cloud.domain.dao.DomainDao;
import com.cloud.host.Host;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.IpAddress;
import com.cloud.network.Ipv6Service;
import com.cloud.network.Network;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.RemoteAccessVpn;
import com.cloud.network.Site2SiteVpnConnection;
import com.cloud.network.VpnUser;
import com.cloud.network.VpnUserVO;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkDetailVO;
import com.cloud.network.dao.NetworkDetailsDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.Site2SiteCustomerGatewayDao;
import com.cloud.network.dao.Site2SiteCustomerGatewayVO;
import com.cloud.network.dao.Site2SiteVpnGatewayDao;
import com.cloud.network.dao.Site2SiteVpnGatewayVO;
import com.cloud.network.dao.VirtualRouterProviderDao;
import com.cloud.network.dao.VpnUserDao;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.lb.LoadBalancingRule.LbDestination;
import com.cloud.network.lb.LoadBalancingRule.LbStickinessPolicy;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.network.rules.StaticNat;
import com.cloud.network.rules.StaticNatRule;
import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.network.vpc.PrivateIpAddress;
import com.cloud.network.vpc.StaticRouteProfile;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.VpcGateway;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingDetailsDao;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.StringUtils;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.Nic;
import com.cloud.vm.NicIpAlias;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.NicIpAliasDao;
import com.cloud.vm.dao.NicIpAliasVO;
import com.cloud.vm.dao.UserVmDao;

public class CommandSetupHelper {

    private static final Logger s_logger = Logger.getLogger(CommandSetupHelper.class);

    @Inject
    private EntityManager _entityMgr;

    @Inject
    private DomainDao domainDao;
    @Inject
    private NicDao _nicDao;
    @Inject
    private NetworkDao _networkDao;
    @Inject
    private DomainRouterDao _routerDao;
    @Inject
    private NetworkModel _networkModel;
    @Inject
    private VirtualMachineManager _itMgr;
    @Inject
    private DataCenterDao _dcDao;
    @Inject
    private NicIpAliasDao _nicIpAliasDao;
    @Inject
    private FirewallRulesDao _rulesDao;
    @Inject
    private NetworkOfferingDao _networkOfferingDao;
    @Inject
    private ConfigurationDao _configDao;
    @Inject
    private ServiceOfferingDao _serviceOfferingDao;
    @Inject
    private UserVmDao _userVmDao;
    @Inject
    private VpnUserDao _vpnUsersDao;
    @Inject
    private Site2SiteCustomerGatewayDao _s2sCustomerGatewayDao;
    @Inject
    private Site2SiteVpnGatewayDao _s2sVpnGatewayDao;
    @Inject
    private VpcDao _vpcDao;
    @Inject
    private VlanDao _vlanDao;
    @Inject
    private IPAddressDao _ipAddressDao;
    @Inject
    private RouterControlHelper _routerControlHelper;
    @Inject
    private HostDao _hostDao;
    @Inject
    ConfigurationManager _configMgr;
    @Inject
    private NetworkOfferingDetailsDao networkOfferingDetailsDao;
    @Inject
    private NetworkDetailsDao networkDetailsDao;
    @Inject
    Ipv6Service ipv6Service;
    @Inject
    VirtualRouterProviderDao vrProviderDao;

    @Autowired
    @Qualifier("networkHelper")
    protected NetworkHelper _networkHelper;

    @Inject
    private LoadBalancerConfigManager _lbConfigMgr;

    public void createVmDataCommand(final VirtualRouter router, final UserVm vm, final NicVO nic, final String publicKey, final Commands cmds) {
        if (vm != null && router != null && nic != null) {
            final String serviceOffering = _serviceOfferingDao.findByIdIncludingRemoved(vm.getId(), vm.getServiceOfferingId()).getDisplayText();
            final String zoneName = _dcDao.findById(router.getDataCenterId()).getName();
            final IPAddressVO staticNatIp = _ipAddressDao.findByVmIdAndNetworkId(nic.getNetworkId(), vm.getId());

            Host host = _hostDao.findById(vm.getHostId());
            String destHostname = VirtualMachineManager.getHypervisorHostname(host != null ? host.getName() : "");
            VmDataCommand vmDataCommand = generateVmDataCommand(router, nic.getIPv4Address(), vm.getUserData(), vm.getUserDataDetails(), serviceOffering, zoneName,
                    staticNatIp == null || staticNatIp.getState() != IpAddress.State.Allocated ? null : staticNatIp.getAddress().addr(), vm.getHostName(), vm.getInstanceName(),
                    vm.getId(), vm.getUuid(), publicKey, nic.getNetworkId(), destHostname);

            Domain domain = domainDao.findById(vm.getDomainId());
            if (domain != null && VirtualMachineManager.AllowExposeDomainInMetadata.valueIn(domain.getId())) {
                s_logger.debug("Adding domain info to cloud metadata");
                vmDataCommand.addVmData(NetworkModel.METATDATA_DIR, NetworkModel.CLOUD_DOMAIN_FILE, domain.getName());
                vmDataCommand.addVmData(NetworkModel.METATDATA_DIR, NetworkModel.CLOUD_DOMAIN_ID_FILE, domain.getUuid());
            }

            cmds.addCommand("vmdata", vmDataCommand);
        }
    }

    public void createApplyVpnUsersCommand(final List<? extends VpnUser> users, final VirtualRouter router, final Commands cmds) {
        final List<VpnUser> addUsers = new ArrayList<VpnUser>();
        final List<VpnUser> removeUsers = new ArrayList<VpnUser>();
        for (final VpnUser user : users) {
            if (user.getState() == VpnUser.State.Add || user.getState() == VpnUser.State.Active) {
                addUsers.add(user);
            } else if (user.getState() == VpnUser.State.Revoke) {
                removeUsers.add(user);
            }
        }

        final VpnUsersCfgCommand cmd = new VpnUsersCfgCommand(addUsers, removeUsers);
        cmd.setAccessDetail(NetworkElementCommand.ACCOUNT_ID, String.valueOf(router.getAccountId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, _routerControlHelper.getRouterControlIp(router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        final DataCenterVO dcVo = _dcDao.findById(router.getDataCenterId());
        cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());

        cmds.addCommand("users", cmd);
    }

    public void createDhcpEntryCommand(final VirtualRouter router, final UserVm vm, final NicVO nic, boolean remove, final Commands cmds) {
        final DhcpEntryCommand dhcpCommand = new DhcpEntryCommand(nic.getMacAddress(), nic.getIPv4Address(), vm.getHostName(), nic.getIPv6Address(),
                _networkModel.getExecuteInSeqNtwkElmtCmd());

        String gatewayIp = nic.getIPv4Gateway();

        final DataCenterVO dcVo = _dcDao.findById(router.getDataCenterId());

        dhcpCommand.setDefaultRouter(gatewayIp);
        dhcpCommand.setIp6Gateway(nic.getIPv6Gateway());
        String ipaddress = null;
        final NicVO domrDefaultNic = findDefaultDnsIp(vm.getId());
        if (domrDefaultNic != null) {
            ipaddress = domrDefaultNic.getIPv4Address();
        }
        dhcpCommand.setDefaultDns(ipaddress);
        dhcpCommand.setDuid(NetUtils.getDuidLL(nic.getMacAddress()));
        dhcpCommand.setDefault(nic.isDefaultNic());
        dhcpCommand.setRemove(remove);

        dhcpCommand.setAccessDetail(NetworkElementCommand.ROUTER_IP, _routerControlHelper.getRouterControlIp(router.getId()));
        dhcpCommand.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        dhcpCommand.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, _routerControlHelper.getRouterIpInNetwork(nic.getNetworkId(), router.getId()));
        dhcpCommand.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());

        cmds.addCommand("dhcp", dhcpCommand);
    }

    public void createIpAlias(final VirtualRouter router, final List<IpAliasTO> ipAliasTOs, final Long networkid, final Commands cmds) {

        final String routerip = _routerControlHelper.getRouterIpInNetwork(networkid, router.getId());
        final DataCenterVO dcVo = _dcDao.findById(router.getDataCenterId());
        final CreateIpAliasCommand ipaliasCmd = new CreateIpAliasCommand(routerip, ipAliasTOs);
        ipaliasCmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, _routerControlHelper.getRouterControlIp(router.getId()));
        ipaliasCmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        ipaliasCmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, routerip);
        ipaliasCmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());

        cmds.addCommand("ipalias", ipaliasCmd);
    }

    public void configDnsMasq(final VirtualRouter router, final Network network, final Commands cmds) {
        final DataCenterVO dcVo = _dcDao.findById(router.getDataCenterId());
        final List<NicIpAliasVO> ipAliasVOList = _nicIpAliasDao.listByNetworkIdAndState(network.getId(), NicIpAlias.State.active);
        final List<DhcpTO> ipList = new ArrayList<DhcpTO>();

        final NicVO router_guest_nic = _nicDao.findByNtwkIdAndInstanceId(network.getId(), router.getId());
        final String cidr = NetUtils.getCidrFromGatewayAndNetmask(router_guest_nic.getIPv4Gateway(), router_guest_nic.getIPv4Netmask());
        final String[] cidrPair = cidr.split("\\/");
        final String cidrAddress = cidrPair[0];
        final long cidrSize = Long.parseLong(cidrPair[1]);
        final String startIpOfSubnet = NetUtils.getIpRangeStartIpFromCidr(cidrAddress, cidrSize);

        ipList.add(new DhcpTO(router_guest_nic.getIPv4Address(), router_guest_nic.getIPv4Gateway(), router_guest_nic.getIPv4Netmask(), startIpOfSubnet));
        for (final NicIpAliasVO ipAliasVO : ipAliasVOList) {
            final DhcpTO DhcpTO = new DhcpTO(ipAliasVO.getIp4Address(), ipAliasVO.getGateway(), ipAliasVO.getNetmask(), ipAliasVO.getStartIpOfSubnet());
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("configDnsMasq : adding ip {" + DhcpTO.getGateway() + ", " + DhcpTO.getNetmask() + ", " + DhcpTO.getRouterIp() + ", " + DhcpTO.getStartIpOfSubnet()
                        + "}");
            }
            ipList.add(DhcpTO);
            ipAliasVO.setVmId(router.getId());
        }
        _dcDao.findById(router.getDataCenterId());
        final DnsMasqConfigCommand dnsMasqConfigCmd = new DnsMasqConfigCommand(ipList);
        dnsMasqConfigCmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, _routerControlHelper.getRouterControlIp(router.getId()));
        dnsMasqConfigCmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        dnsMasqConfigCmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, _routerControlHelper.getRouterIpInNetwork(network.getId(), router.getId()));
        dnsMasqConfigCmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());
        cmds.addCommand("dnsMasqConfig", dnsMasqConfigCmd);
    }

    public void createApplyLoadBalancingRulesCommands(final List<LoadBalancingRule> rules, final VirtualRouter router, final Commands cmds, final long guestNetworkId) {
        final LoadBalancerTO[] lbs = new LoadBalancerTO[rules.size()];
        int i = 0;
        // We don't support VR to be inline currently
        final boolean inline = false;
        for (final LoadBalancingRule rule : rules) {
            final boolean revoked = rule.getState().equals(FirewallRule.State.Revoke);
            final String protocol = rule.getProtocol();
            final String lb_protocol = rule.getLbProtocol();
            final String algorithm = rule.getAlgorithm();
            final String uuid = rule.getUuid();

            final String srcIp = rule.getSourceIp().addr();
            final int srcPort = rule.getSourcePortStart();
            final List<LbDestination> destinations = rule.getDestinations();
            final List<LbStickinessPolicy> stickinessPolicies = rule.getStickinessPolicies();
            final LoadBalancerTO lb = new LoadBalancerTO(uuid, srcIp, srcPort, protocol, algorithm, revoked, false, inline, destinations, stickinessPolicies);
            lb.setLbSslCert(rule.getLbSslCert());
            lb.setCidrList(rule.getCidrList());
            lb.setLbProtocol(lb_protocol);
            lb.setLbConfigs(_lbConfigMgr.getRuleLbConfigs(rule.getId()));
            lbs[i++] = lb;
        }
        String routerPublicIp = null;

        if (router instanceof DomainRouterVO) {
            final DomainRouterVO domr = _routerDao.findById(router.getId());
            routerPublicIp = domr.getPublicIpAddress();
            if (routerPublicIp == null) {
                routerPublicIp = router.getPublicIpAddress();
            }
        }

        final Network guestNetwork = _networkModel.getNetwork(guestNetworkId);
        final Nic nic = _nicDao.findByNtwkIdAndInstanceId(guestNetwork.getId(), router.getId());
        final NicProfile nicProfile = new NicProfile(nic, guestNetwork, nic.getBroadcastUri(), nic.getIsolationUri(), _networkModel.getNetworkRate(guestNetwork.getId(),
                router.getId()), _networkModel.isSecurityGroupSupportedInNetwork(guestNetwork), _networkModel.getNetworkTag(router.getHypervisorType(), guestNetwork));
        final NetworkOffering offering = _networkOfferingDao.findById(guestNetwork.getNetworkOfferingId());
        String maxconn = null;
        if (offering.getConcurrentConnections() == null) {
            maxconn = _configDao.getValue(Config.NetworkLBHaproxyMaxConn.key());
        } else {
            maxconn = offering.getConcurrentConnections().toString();
        }

        final LoadBalancerConfigCommand cmd = new LoadBalancerConfigCommand(lbs, routerPublicIp, _routerControlHelper.getRouterIpInNetwork(guestNetworkId, router.getId()),
                router.getPrivateIpAddress(), _itMgr.toNicTO(nicProfile, router.getHypervisorType()), router.getVpcId(), maxconn, offering.isKeepAliveEnabled());

        boolean isTransparent = false;
        for (final LoadBalancerTO lbTO : lbs) {
            final LoadBalancerConfigTO[] lbConfigs = lbTO.getLbConfigs();
            for (LoadBalancerConfigTO lbConfig: lbConfigs) {
                if (lbConfig.getName().equals(LoadBalancerConfigKey.LbTransparent.key())) {
                    isTransparent = "true".equalsIgnoreCase(lbConfig.getValue());
                    break;
                }
            }
            if (isTransparent) {
                break;
            }
        }
        cmd.setIsTransparent(isTransparent);
        cmd.setNetworkCidr(guestNetwork.getCidr());
        if (router.getVpcId() != null) {
            cmd.setNetworkLbConfigs(_lbConfigMgr.getVpcLbConfigs(router.getVpcId()));
        } else {
            cmd.setNetworkLbConfigs(_lbConfigMgr.getNetworkLbConfigs(guestNetworkId));
        }
        cmd.lbStatsVisibility = _configDao.getValue(Config.NetworkLBHaproxyStatsVisbility.key());
        cmd.lbStatsUri = _configDao.getValue(Config.NetworkLBHaproxyStatsUri.key());
        cmd.lbStatsAuth = _configDao.getValue(Config.NetworkLBHaproxyStatsAuth.key());
        cmd.lbStatsPort = _configDao.getValue(Config.NetworkLBHaproxyStatsPort.key());
        cmd.lbSslConfiguration = _configDao.getValue("default.lb.ssl.configuration");

        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, _routerControlHelper.getRouterControlIp(router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, _routerControlHelper.getRouterIpInNetwork(guestNetworkId, router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        final DataCenterVO dcVo = _dcDao.findById(router.getDataCenterId());
        cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());
        cmds.addCommand(cmd);
    }

    public void createApplyPortForwardingRulesCommands(final List<? extends PortForwardingRule> rules, final VirtualRouter router, final Commands cmds, final long guestNetworkId) {
        final List<PortForwardingRuleTO> rulesTO = new ArrayList<PortForwardingRuleTO>();
        if (rules != null) {
            for (final PortForwardingRule rule : rules) {
                final IpAddress sourceIp = _networkModel.getIp(rule.getSourceIpAddressId());
                final PortForwardingRuleTO ruleTO = new PortForwardingRuleTO(rule, null, sourceIp.getAddress().addr());
                rulesTO.add(ruleTO);
            }
        }

        SetPortForwardingRulesCommand cmd = null;

        if (router.getVpcId() != null) {
            cmd = new SetPortForwardingRulesVpcCommand(rulesTO);
        } else {
            cmd = new SetPortForwardingRulesCommand(rulesTO);
        }

        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, _routerControlHelper.getRouterControlIp(router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, _routerControlHelper.getRouterIpInNetwork(guestNetworkId, router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        final DataCenterVO dcVo = _dcDao.findById(router.getDataCenterId());
        cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());

        cmds.addCommand(cmd);
    }

    public void createApplyStaticNatRulesCommands(final List<? extends StaticNatRule> rules, final VirtualRouter router, final Commands cmds, final long guestNetworkId) {
        final List<StaticNatRuleTO> rulesTO = new ArrayList<StaticNatRuleTO>();
        if (rules != null) {
            for (final StaticNatRule rule : rules) {
                final IpAddress sourceIp = _networkModel.getIp(rule.getSourceIpAddressId());
                final StaticNatRuleTO ruleTO = new StaticNatRuleTO(rule, null, sourceIp.getAddress().addr(), rule.getDestIpAddress());
                rulesTO.add(ruleTO);
            }
        }

        final SetStaticNatRulesCommand cmd = new SetStaticNatRulesCommand(rulesTO, router.getVpcId());
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, _routerControlHelper.getRouterControlIp(router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, _routerControlHelper.getRouterIpInNetwork(guestNetworkId, router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        final DataCenterVO dcVo = _dcDao.findById(router.getDataCenterId());
        cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());
        cmds.addCommand(cmd);
    }

    public void createApplyFirewallRulesCommands(final List<? extends FirewallRule> rules, final VirtualRouter router, final Commands cmds, final long guestNetworkId) {
        final List<FirewallRuleTO> rulesTO = new ArrayList<FirewallRuleTO>();
        String systemRule = null;
        Boolean defaultEgressPolicy = false;
        if (rules != null) {
            if (rules.size() > 0) {
                if (rules.get(0).getTrafficType() == FirewallRule.TrafficType.Egress && rules.get(0).getType() == FirewallRule.FirewallRuleType.System) {
                    systemRule = String.valueOf(FirewallRule.FirewallRuleType.System);
                }
            }
            for (final FirewallRule rule : rules) {
                _rulesDao.loadSourceCidrs((FirewallRuleVO) rule);
                final FirewallRule.TrafficType traffictype = rule.getTrafficType();
                if (traffictype == FirewallRule.TrafficType.Ingress) {
                    final IpAddress sourceIp = _networkModel.getIp(rule.getSourceIpAddressId());
                    final FirewallRuleTO ruleTO = new FirewallRuleTO(rule, null, sourceIp.getAddress().addr(), Purpose.Firewall, traffictype);
                    rulesTO.add(ruleTO);
                } else if (rule.getTrafficType() == FirewallRule.TrafficType.Egress) {
                    final NetworkVO network = _networkDao.findById(guestNetworkId);
                    final NetworkOfferingVO offering = _networkOfferingDao.findById(network.getNetworkOfferingId());
                    defaultEgressPolicy = offering.isEgressDefaultPolicy();
                    assert rule.getSourceIpAddressId() == null : "ipAddressId should be null for egress firewall rule. ";
                    final FirewallRuleTO ruleTO = new FirewallRuleTO(rule, null, "", Purpose.Firewall, traffictype, defaultEgressPolicy);
                    rulesTO.add(ruleTO);
                }
            }
        }

        final SetFirewallRulesCommand cmd = new SetFirewallRulesCommand(rulesTO);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, _routerControlHelper.getRouterControlIp(router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, _routerControlHelper.getRouterIpInNetwork(guestNetworkId, router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        final DataCenterVO dcVo = _dcDao.findById(router.getDataCenterId());
        cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());
        if (systemRule != null) {
            cmd.setAccessDetail(NetworkElementCommand.FIREWALL_EGRESS_DEFAULT, systemRule);
        } else {
            cmd.setAccessDetail(NetworkElementCommand.FIREWALL_EGRESS_DEFAULT, String.valueOf(defaultEgressPolicy));
        }

        cmds.addCommand(cmd);
    }

    public void createApplyIpv6FirewallRulesCommands(final List<? extends FirewallRule> rules, final VirtualRouter router, final Commands cmds, final long guestNetworkId) {
        final List<FirewallRuleTO> rulesTO = new ArrayList<>();
        String systemRule = null;
        final NetworkVO network = _networkDao.findById(guestNetworkId);
        final NetworkOfferingVO offering = _networkOfferingDao.findById(network.getNetworkOfferingId());
        Boolean defaultEgressPolicy = offering.isEgressDefaultPolicy();;
        if (rules != null) {
            if (rules.size() > 0) {
                if (rules.get(0).getTrafficType() == FirewallRule.TrafficType.Egress && rules.get(0).getType() == FirewallRule.FirewallRuleType.System) {
                    systemRule = String.valueOf(FirewallRule.FirewallRuleType.System);
                }
            }
            for (final FirewallRule rule : rules) {
                _rulesDao.loadSourceCidrs((FirewallRuleVO)rule);
                _rulesDao.loadDestinationCidrs((FirewallRuleVO)rule);
                final FirewallRule.TrafficType trafficType = rule.getTrafficType();
                if (trafficType == FirewallRule.TrafficType.Ingress) {
                    final FirewallRuleTO ruleTO = new FirewallRuleTO(rule, null, null, Purpose.Ipv6Firewall, trafficType, defaultEgressPolicy);
                    rulesTO.add(ruleTO);
                } else if (rule.getTrafficType() == FirewallRule.TrafficType.Egress) {
                    final FirewallRuleTO ruleTO = new FirewallRuleTO(rule, null, "", Purpose.Ipv6Firewall, trafficType, defaultEgressPolicy);
                    rulesTO.add(ruleTO);
                }
            }
        }

        final SetIpv6FirewallRulesCommand cmd = new SetIpv6FirewallRulesCommand(rulesTO, network.getIp6Cidr());
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, _routerControlHelper.getRouterControlIp(router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, _routerControlHelper.getRouterIpInNetwork(guestNetworkId, router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        final DataCenterVO dcVo = _dcDao.findById(router.getDataCenterId());
        cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());
        if (systemRule != null) {
            cmd.setAccessDetail(NetworkElementCommand.FIREWALL_EGRESS_DEFAULT, systemRule);
        } else {
            cmd.setAccessDetail(NetworkElementCommand.FIREWALL_EGRESS_DEFAULT, String.valueOf(defaultEgressPolicy));
        }

        cmds.addCommand(cmd);
    }

    public void createFirewallRulesCommands(final List<? extends FirewallRule> rules, final VirtualRouter router, final Commands cmds, final long guestNetworkId) {
        final List<FirewallRuleTO> rulesTO = new ArrayList<FirewallRuleTO>();
        String systemRule = null;
        Boolean defaultEgressPolicy = false;
        if (rules != null) {
            if (rules.size() > 0) {
                if (rules.get(0).getTrafficType() == FirewallRule.TrafficType.Egress && rules.get(0).getType() == FirewallRule.FirewallRuleType.System) {
                    systemRule = String.valueOf(FirewallRule.FirewallRuleType.System);
                }
            }
            for (final FirewallRule rule : rules) {
                _rulesDao.loadSourceCidrs((FirewallRuleVO) rule);
                _rulesDao.loadDestinationCidrs((FirewallRuleVO)rule);
                final FirewallRule.TrafficType traffictype = rule.getTrafficType();
                if (traffictype == FirewallRule.TrafficType.Ingress) {
                    final IpAddress sourceIp = _networkModel.getIp(rule.getSourceIpAddressId());
                    final FirewallRuleTO ruleTO = new FirewallRuleTO(rule, null, sourceIp.getAddress().addr(), Purpose.Firewall, traffictype);
                    rulesTO.add(ruleTO);
                } else if (rule.getTrafficType() == FirewallRule.TrafficType.Egress) {
                    final NetworkVO network = _networkDao.findById(guestNetworkId);
                    final NetworkOfferingVO offering = _networkOfferingDao.findById(network.getNetworkOfferingId());
                    defaultEgressPolicy = offering.isEgressDefaultPolicy();
                    assert rule.getSourceIpAddressId() == null : "ipAddressId should be null for egress firewall rule. ";
                    final FirewallRuleTO ruleTO = new FirewallRuleTO(rule, null, "", Purpose.Firewall, traffictype, defaultEgressPolicy);
                    rulesTO.add(ruleTO);
                }
            }
        }

        final SetFirewallRulesCommand cmd = new SetFirewallRulesCommand(rulesTO);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, _routerControlHelper.getRouterControlIp(router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, _routerControlHelper.getRouterIpInNetwork(guestNetworkId, router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        final DataCenterVO dcVo = _dcDao.findById(router.getDataCenterId());
        cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());
        if (systemRule != null) {
            cmd.setAccessDetail(NetworkElementCommand.FIREWALL_EGRESS_DEFAULT, systemRule);
        } else {
            cmd.setAccessDetail(NetworkElementCommand.FIREWALL_EGRESS_DEFAULT, String.valueOf(defaultEgressPolicy));
        }

        cmds.addCommand(cmd);
    }

    public void createIpv6FirewallRulesCommands(final List<? extends FirewallRule> rules, final VirtualRouter router, final Commands cmds, final long guestNetworkId) {
        final List<FirewallRuleTO> rulesTO = new ArrayList<>();
        String systemRule = null;
        final NetworkVO network = _networkDao.findById(guestNetworkId);
        final NetworkOfferingVO offering = _networkOfferingDao.findById(network.getNetworkOfferingId());
        Boolean defaultEgressPolicy = offering.isEgressDefaultPolicy();
        if (rules != null) {
            if (rules.size() > 0) {
                if (rules.get(0).getTrafficType() == FirewallRule.TrafficType.Egress && rules.get(0).getType() == FirewallRule.FirewallRuleType.System) {
                    systemRule = String.valueOf(FirewallRule.FirewallRuleType.System);
                }
            }
            for (final FirewallRule rule : rules) {
                _rulesDao.loadSourceCidrs((FirewallRuleVO)rule);
                _rulesDao.loadDestinationCidrs((FirewallRuleVO)rule);
                final FirewallRule.TrafficType traffictype = rule.getTrafficType();
                if (traffictype == FirewallRule.TrafficType.Ingress) {
                    final FirewallRuleTO ruleTO = new FirewallRuleTO(rule, null, null, Purpose.Ipv6Firewall, traffictype, defaultEgressPolicy);
                    rulesTO.add(ruleTO);
                } else if (rule.getTrafficType() == FirewallRule.TrafficType.Egress) {
                    final FirewallRuleTO ruleTO = new FirewallRuleTO(rule, null, "", Purpose.Ipv6Firewall, traffictype, defaultEgressPolicy);
                    rulesTO.add(ruleTO);
                }
            }
        }

        final SetIpv6FirewallRulesCommand cmd = new SetIpv6FirewallRulesCommand(rulesTO, network.getIp6Cidr());
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, _routerControlHelper.getRouterControlIp(router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, _routerControlHelper.getRouterIpInNetwork(guestNetworkId, router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        final DataCenterVO dcVo = _dcDao.findById(router.getDataCenterId());
        cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());
        if (systemRule != null) {
            cmd.setAccessDetail(NetworkElementCommand.FIREWALL_EGRESS_DEFAULT, systemRule);
        } else {
            cmd.setAccessDetail(NetworkElementCommand.FIREWALL_EGRESS_DEFAULT, String.valueOf(defaultEgressPolicy));
        }

        cmds.addCommand(cmd);
    }

    public void createAssociateIPCommands(final VirtualRouter router, final List<? extends PublicIpAddress> ips, final Commands cmds, final long vmId) {
        final String ipAssocCommand = "IPAssocCommand";
        createRedundantAssociateIPCommands(router, ips, cmds, ipAssocCommand, false);
    }

    public void createNetworkACLsCommands(final List<? extends NetworkACLItem> rules, final VirtualRouter router, final Commands cmds, final long guestNetworkId,
            final boolean privateGateway) {
        final List<NetworkACLTO> rulesTO = new ArrayList<NetworkACLTO>();
        String guestVlan = null;
        final Network guestNtwk = _networkDao.findById(guestNetworkId);
        final URI uri = guestNtwk.getBroadcastUri();
        if (uri != null) {
            guestVlan = BroadcastDomainType.getValue(uri);
        }

        if (rules != null) {
            for (final NetworkACLItem rule : rules) {
                final NetworkACLTO ruleTO = new NetworkACLTO(rule, guestVlan, rule.getTrafficType());
                rulesTO.add(ruleTO);
            }
        }

        NicTO nicTO = _networkHelper.getNicTO(router, guestNetworkId, null);
        final SetNetworkACLCommand cmd = new SetNetworkACLCommand(rulesTO, nicTO);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, _routerControlHelper.getRouterControlIp(router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, _routerControlHelper.getRouterIpInNetwork(guestNetworkId, router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.GUEST_VLAN_TAG, guestVlan);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        final DataCenterVO dcVo = _dcDao.findById(router.getDataCenterId());
        cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());
        if (privateGateway) {
            cmd.setAccessDetail(NetworkElementCommand.VPC_PRIVATE_GATEWAY, String.valueOf(VpcGateway.Type.Private));
        }

        cmds.addCommand(cmd);
    }

    public void createPasswordCommand(final VirtualRouter router, final VirtualMachineProfile profile, final NicVO nic, final Commands cmds) {
        final String password = (String) profile.getParameter(VirtualMachineProfile.Param.VmPassword);
        final DataCenterVO dcVo = _dcDao.findById(router.getDataCenterId());

        // password should be set only on default network element
        if (password != null && nic.isDefaultNic()) {
            final SavePasswordCommand cmd = new SavePasswordCommand(password, nic.getIPv4Address(), profile.getVirtualMachine().getHostName(),
                    _networkModel.getExecuteInSeqNtwkElmtCmd());
            cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, _routerControlHelper.getRouterControlIp(router.getId()));
            cmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, _routerControlHelper.getRouterIpInNetwork(nic.getNetworkId(), router.getId()));
            cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
            cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());

            cmds.addCommand("password", cmd);
        }

    }

    public void createApplyStaticNatCommands(final List<? extends StaticNat> rules, final VirtualRouter router, final Commands cmds, final long guestNetworkId) {
        final List<StaticNatRuleTO> rulesTO = new ArrayList<StaticNatRuleTO>();
        if (rules != null) {
            for (final StaticNat rule : rules) {
                final IpAddress sourceIp = _networkModel.getIp(rule.getSourceIpAddressId());
                final StaticNatRuleTO ruleTO = new StaticNatRuleTO(0, sourceIp.getAddress().addr(), null, null, rule.getDestIpAddress(), null, null, null, rule.isForRevoke(),
                        false);
                rulesTO.add(ruleTO);
            }
        }

        final SetStaticNatRulesCommand cmd = new SetStaticNatRulesCommand(rulesTO, router.getVpcId());
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, _routerControlHelper.getRouterControlIp(router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, _routerControlHelper.getRouterIpInNetwork(guestNetworkId, router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());

        final DataCenterVO dcVo = _dcDao.findById(router.getDataCenterId());
        cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());
        cmds.addCommand(cmd);
    }

    public void createStaticRouteCommands(final List<StaticRouteProfile> staticRoutes, final VirtualRouter router, final Commands cmds) {
        final SetStaticRouteCommand cmd = new SetStaticRouteCommand(staticRoutes);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, _routerControlHelper.getRouterControlIp(router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        final DataCenterVO dcVo = _dcDao.findById(router.getDataCenterId());
        cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());
        cmds.addCommand(cmd);
    }

    public void createApplyVpnCommands(final boolean isCreate, final RemoteAccessVpn vpn, final VirtualRouter router, final Commands cmds) {
        final List<VpnUserVO> vpnUsers = _vpnUsersDao.listByAccount(vpn.getAccountId());

        createApplyVpnUsersCommand(vpnUsers, router, cmds);

        final IpAddress ip = _networkModel.getIp(vpn.getServerAddressId());

        // This block is needed due to the line 206 of the
        // RemoteAccessVpnManagenerImpl:
        // TODO: assumes one virtual network / domr per account per zone
        final String cidr;
        final Network network = _networkDao.findById(vpn.getNetworkId());
        if (network == null) {
            final Vpc vpc = _vpcDao.findById(vpn.getVpcId());
            cidr = vpc.getCidr();
        } else {
            cidr = network.getCidr();
        }

        final RemoteAccessVpnCfgCommand startVpnCmd = new RemoteAccessVpnCfgCommand(isCreate, ip.getAddress().addr(), vpn.getLocalIp(), vpn.getIpRange(),
                vpn.getIpsecPresharedKey(), vpn.getVpcId() != null);
        startVpnCmd.setLocalCidr(cidr);
        startVpnCmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, _routerControlHelper.getRouterControlIp(router.getId()));
        startVpnCmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        final DataCenterVO dcVo = _dcDao.findById(router.getDataCenterId());
        startVpnCmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());

        cmds.addCommand("startVpn", startVpnCmd);
    }

    public void createVmDataCommandForVMs(final DomainRouterVO router, final Commands cmds, final long guestNetworkId) {
        final List<UserVmVO> vms = _userVmDao.listByNetworkIdAndStates(guestNetworkId, VirtualMachine.State.Running, VirtualMachine.State.Migrating, VirtualMachine.State.Stopping);
        final DataCenterVO dc = _dcDao.findById(router.getDataCenterId());
        for (final UserVmVO vm : vms) {
            boolean createVmData = true;
            if (dc.getNetworkType() == NetworkType.Basic && router.getPodIdToDeployIn().longValue() != vm.getPodIdToDeployIn().longValue()) {
                createVmData = false;
            }

            if (createVmData) {
                final NicVO nic = _nicDao.findByNtwkIdAndInstanceId(guestNetworkId, vm.getId());
                if (nic != null) {
                    s_logger.debug("Creating user data entry for vm " + vm + " on domR " + router);

                    _userVmDao.loadDetails(vm);
                    createVmDataCommand(router, vm, nic, vm.getDetail("SSH.PublicKey"), cmds);
                }
            }
        }
    }

    public void createDhcpEntryCommandsForVMs(final DomainRouterVO router, final Commands cmds, final long guestNetworkId) {
        final List<UserVmVO> vms = _userVmDao.listByNetworkIdAndStates(guestNetworkId, VirtualMachine.State.Running, VirtualMachine.State.Migrating, VirtualMachine.State.Stopping);
        final DataCenterVO dc = _dcDao.findById(router.getDataCenterId());
        String dnsBasicZoneUpdates = _configDao.getValue(Config.DnsBasicZoneUpdates.key());
        for (final UserVmVO vm : vms) {
            if (dc.getNetworkType() == NetworkType.Basic && router.getPodIdToDeployIn().longValue() != vm.getPodIdToDeployIn().longValue()
                    && dnsBasicZoneUpdates.equalsIgnoreCase("pod")) {
                continue;
            }

            final NicVO nic = _nicDao.findByNtwkIdAndInstanceId(guestNetworkId, vm.getId());
            if (nic != null) {
                s_logger.debug("Creating dhcp entry for vm " + vm + " on domR " + router + ".");
                createDhcpEntryCommand(router, vm, nic, false, cmds);
            }
        }
    }

    public void createDeleteIpAliasCommand(final DomainRouterVO router, final List<IpAliasTO> deleteIpAliasTOs, final List<IpAliasTO> createIpAliasTos, final long networkId,
            final Commands cmds) {
        final String routerip = _routerControlHelper.getRouterIpInNetwork(networkId, router.getId());
        final DataCenterVO dcVo = _dcDao.findById(router.getDataCenterId());
        final DeleteIpAliasCommand deleteIpaliasCmd = new DeleteIpAliasCommand(routerip, deleteIpAliasTOs, createIpAliasTos);
        deleteIpaliasCmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, _routerControlHelper.getRouterControlIp(router.getId()));
        deleteIpaliasCmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        deleteIpaliasCmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, routerip);
        deleteIpaliasCmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());

        cmds.addCommand("deleteIpalias", deleteIpaliasCmd);
    }

    public void createVpcAssociatePublicIPCommands(final VirtualRouter router, final List<? extends PublicIpAddress> ips, final Commands cmds,
            final Map<String, String> vlanMacAddress) {

        final String ipAssocCommand = "IPAssocVpcCommand";
        if (router.getIsRedundantRouter()) {
            createRedundantAssociateIPCommands(router, ips, cmds, ipAssocCommand, true);
            return;
        }

        Pair<IpAddressTO, Long> sourceNatIpAdd = null;
        Boolean addSourceNat = null;
        // Ensure that in multiple vlans case we first send all ip addresses of
        // vlan1, then all ip addresses of vlan2, etc..
        final Map<String, ArrayList<PublicIpAddress>> vlanIpMap = new HashMap<String, ArrayList<PublicIpAddress>>();
        for (final PublicIpAddress ipAddress : ips) {
            final String vlanTag = ipAddress.getVlanTag();
            ArrayList<PublicIpAddress> ipList = vlanIpMap.get(vlanTag);
            if (ipList == null) {
                ipList = new ArrayList<PublicIpAddress>();
            }
            // VR doesn't support release for sourceNat IP address; so reset the
            // state
            if (ipAddress.isSourceNat() && ipAddress.getState() == IpAddress.State.Releasing) {
                ipAddress.setState(IpAddress.State.Allocated);
            }
            ipList.add(ipAddress);
            vlanIpMap.put(vlanTag, ipList);
        }

        Long guestNetworkId = null;
        final List<NicVO> nics = _nicDao.listByVmId(router.getId());
        for (final NicVO nic : nics) {
            final NetworkVO nw = _networkDao.findById(nic.getNetworkId());
            if (nw.getTrafficType() == TrafficType.Guest) {
                guestNetworkId = nw.getId();
                break;
            }
        }

        Map<String, Boolean> vlanLastIpMap = getVlanLastIpMap(router.getVpcId(), guestNetworkId);

        for (final Map.Entry<String, ArrayList<PublicIpAddress>> vlanAndIp : vlanIpMap.entrySet()) {
            final String vlanTagKey = vlanAndIp.getKey();
            final List<PublicIpAddress> ipAddrList = vlanAndIp.getValue();

            // Source nat ip address should always be sent first
            Collections.sort(ipAddrList, new Comparator<PublicIpAddress>() {
                @Override
                public int compare(final PublicIpAddress o1, final PublicIpAddress o2) {
                    final boolean s1 = o1.isSourceNat();
                    final boolean s2 = o2.isSourceNat();
                    return s1 ^ s2 ? s1 ^ true ? 1 : -1 : 0;
                }
            });


            // Get network rate - required for IpAssoc
            final Integer networkRate = _networkModel.getNetworkRate(ipAddrList.get(0).getNetworkId(), router.getId());
            final Network network = _networkModel.getNetwork(ipAddrList.get(0).getNetworkId());

            final IpAddressTO[] ipsToSend = new IpAddressTO[ipAddrList.size()];
            int i = 0;
            boolean firstIP = true;

            for (final PublicIpAddress ipAddr : ipAddrList) {
                final boolean add = ipAddr.getState() == IpAddress.State.Releasing ? false : true;

                final String macAddress = vlanMacAddress.get(BroadcastDomainType.getValue(BroadcastDomainType.fromString(ipAddr.getVlanTag())));

                final IpAddressTO ip = new IpAddressTO(ipAddr.getAccountId(), ipAddr.getAddress().addr(), add, firstIP, ipAddr.isSourceNat(), BroadcastDomainType.fromString(ipAddr.getVlanTag()).toString(), ipAddr.getGateway(),
                        ipAddr.getNetmask(), macAddress, networkRate, ipAddr.isOneToOneNat());
                setIpAddressNetworkParams(ip, network, router);
                if (network.getPublicMtu() != null) {
                    ip.setMtu(network.getPublicMtu());
                }
                if (router.getVpcId() != null) {
                    VpcVO vpc = _vpcDao.findById(router.getVpcId());
                    if (vpc != null) {
                        ip.setMtu(vpc.getPublicMtu());
                    }
                }
                ipsToSend[i++] = ip;
                if (ipAddr.isSourceNat()) {
                    sourceNatIpAdd = new Pair<IpAddressTO, Long>(ip, ipAddr.getNetworkId());
                    addSourceNat = add;
                }

                //for additional public subnet on delete it is not sure which ip is set to first ip. So on delete we
                //want to set sourcenat to true for all ips to delete source nat rules.
                if (!firstIP || add) {
                    firstIP = false;
                }
            }
            final IpAssocVpcCommand cmd = new IpAssocVpcCommand(ipsToSend);
            cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, _routerControlHelper.getRouterControlIp(router.getId()));
            cmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, _routerControlHelper.getRouterIpInNetwork(ipAddrList.get(0).getNetworkId(), router.getId()));
            cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
            final DataCenterVO dcVo = _dcDao.findById(router.getDataCenterId());
            cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());

            setAccessDetailNetworkLastPublicIp(vlanLastIpMap, vlanTagKey, cmd);

            cmds.addCommand(ipAssocCommand, cmd);
        }

        // set source nat ip
        if (sourceNatIpAdd != null) {
            final IpAddressTO sourceNatIp = sourceNatIpAdd.first();
            final SetSourceNatCommand cmd = new SetSourceNatCommand(sourceNatIp, addSourceNat);
            cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, _routerControlHelper.getRouterControlIp(router.getId()));
            cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
            final DataCenterVO dcVo = _dcDao.findById(router.getDataCenterId());
            cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());
            cmds.addCommand("SetSourceNatCommand", cmd);
        }
    }

    public void createRedundantAssociateIPCommands(final VirtualRouter router, final List<? extends PublicIpAddress> ips, final Commands cmds, final String ipAssocCommand, final boolean isVPC) {

        // Ensure that in multiple vlans case we first send all ip addresses of
        // vlan1, then all ip addresses of vlan2, etc..
        final Map<String, ArrayList<PublicIpAddress>> vlanIpMap = new HashMap<String, ArrayList<PublicIpAddress>>();
        for (final PublicIpAddress ipAddress : ips) {
            final String vlanTag = ipAddress.getVlanTag();
            ArrayList<PublicIpAddress> ipList = vlanIpMap.get(vlanTag);
            if (ipList == null) {
                ipList = new ArrayList<PublicIpAddress>();
            }
            // domR doesn't support release for sourceNat IP address; so reset
            // the state
            if (ipAddress.isSourceNat() && ipAddress.getState() == IpAddress.State.Releasing) {
                ipAddress.setState(IpAddress.State.Allocated);
            }
            ipList.add(ipAddress);
            vlanIpMap.put(vlanTag, ipList);
        }

        final List<NicVO> nics = _nicDao.listByVmId(router.getId());
        String baseMac = null;
        Map<String, String> vlanMacAddress = new HashMap<String, String>();;
        Long guestNetworkId = null;
        for (final NicVO nic : nics) {
            final NetworkVO nw = _networkDao.findById(nic.getNetworkId());
            if (nw.getTrafficType() == TrafficType.Public) {
                if (baseMac == null) {
                    baseMac = nic.getMacAddress();
                }
                final String vlanTag = BroadcastDomainType.getValue(nic.getBroadcastUri());
                vlanMacAddress.put(vlanTag, nic.getMacAddress());
            } else if (nw.getTrafficType() == TrafficType.Guest && guestNetworkId == null) {
                guestNetworkId = nw.getId();
            }
        }

        Map<String, Boolean> vlanLastIpMap = getVlanLastIpMap(router.getVpcId(), guestNetworkId);

        for (final Map.Entry<String, ArrayList<PublicIpAddress>> vlanAndIp : vlanIpMap.entrySet()) {
            final String vlanTagKey = vlanAndIp.getKey();
            final List<PublicIpAddress> ipAddrList = vlanAndIp.getValue();
            // Source nat ip address should always be sent first
            Collections.sort(ipAddrList, new Comparator<PublicIpAddress>() {
                @Override
                public int compare(final PublicIpAddress o1, final PublicIpAddress o2) {
                    final boolean s1 = o1.isSourceNat();
                    final boolean s2 = o2.isSourceNat();
                    return s1 ^ s2 ? s1 ^ true ? 1 : -1 : 0;
                }
            });

            // Get network rate - required for IpAssoc
            final Integer networkRate = _networkModel.getNetworkRate(ipAddrList.get(0).getNetworkId(), router.getId());
            final Network network = _networkModel.getNetwork(ipAddrList.get(0).getNetworkId());

            final IpAddressTO[] ipsToSend = new IpAddressTO[ipAddrList.size()];
            int i = 0;
            boolean firstIP = true;

            for (final PublicIpAddress ipAddr : ipAddrList) {

                final boolean add = ipAddr.getState() == IpAddress.State.Releasing ? false : true;
                boolean sourceNat = ipAddr.isSourceNat();
                /* enable sourceNAT for the first ip of the public interface */
                if (firstIP) {
                    sourceNat = true;
                }
                final String vlanId = ipAddr.getVlanTag();
                final String vlanGateway = ipAddr.getGateway();
                final String vlanNetmask = ipAddr.getNetmask();
                String vifMacAddress = null;
                final String vlanTag = BroadcastDomainType.getValue(BroadcastDomainType.fromString(ipAddr.getVlanTag()));
                if (vlanMacAddress.containsKey(vlanTag)) {
                    vifMacAddress = vlanMacAddress.get(vlanTag);
                } else {
                    if (ipAddr.getVlanId() != 0) {
                        vifMacAddress = NetUtils.generateMacOnIncrease(baseMac, ipAddr.getVlanId());
                    } else {
                        vifMacAddress = ipAddr.getMacAddress();
                    }
                    vlanMacAddress.put(vlanTag, vifMacAddress);
                }

                final IpAddressTO ip = new IpAddressTO(ipAddr.getAccountId(), ipAddr.getAddress().addr(), add, firstIP, sourceNat, vlanId, vlanGateway, vlanNetmask,
                        vifMacAddress, networkRate, ipAddr.isOneToOneNat());
                NetworkVO networkVO = _networkDao.findById(ipAddr.getAssociatedWithNetworkId());
                if (networkVO != null && networkVO.getPublicMtu() != null) {
                    ip.setMtu(networkVO.getPublicMtu());
                } else if (router.getVpcId() != null) {
                    VpcVO vpc = _vpcDao.findById(router.getVpcId());
                    if (vpc != null) {
                        ip.setMtu(vpc.getPublicMtu());
                    }
                }

                setIpAddressNetworkParams(ip, network, router);
                if (router.getHypervisorType() == Hypervisor.HypervisorType.VMware) {
                    Map<String, String> details = new HashMap<>();
                    String defaultSystemVmNicAdapterType = _configDao.getValue(Config.VmwareSystemVmNicDeviceType.key());
                    if (defaultSystemVmNicAdapterType == null) {
                        defaultSystemVmNicAdapterType = Config.VmwareSystemVmNicDeviceType.getDefaultValue();
                    }
                    details.put(VmDetailConstants.NIC_ADAPTER, defaultSystemVmNicAdapterType);
                    ip.setDetails(details);
                }
                ipsToSend[i++] = ip;
                /*
                 * send the firstIP = true for the first Add, this is to create
                 * primary on interface
                 */
                if (!firstIP || add) {
                    firstIP = false;
                }
            }

            final IpAssocCommand cmd;
            if (router.getVpcId() != null) {
                cmd = new IpAssocVpcCommand(ipsToSend);
            } else {
                cmd = new IpAssocCommand(ipsToSend);
            }
            cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, _routerControlHelper.getRouterControlIp(router.getId()));
            cmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, _routerControlHelper.getRouterIpInNetwork(ipAddrList.get(0).getNetworkId(), router.getId()));
            cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
            final DataCenterVO dcVo = _dcDao.findById(router.getDataCenterId());
            cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());

            setAccessDetailNetworkLastPublicIp(vlanLastIpMap, vlanTagKey, cmd);

            cmds.addCommand(ipAssocCommand, cmd);
        }
    }

    private void setAccessDetailNetworkLastPublicIp(Map<String, Boolean> vlanLastIpMap, String vlanTagKey, IpAssocCommand cmd) {
        // if public ip is the last ip in the vlan which is used for static nat or has active rules,
        // it will be checked for remove at the resource
        Boolean lastIp = vlanLastIpMap.get(vlanTagKey);
        if (lastIp == null) {
            cmd.setAccessDetail(NetworkElementCommand.NETWORK_PUB_LAST_IP, "true");
        } else {
            cmd.setAccessDetail(NetworkElementCommand.NETWORK_PUB_LAST_IP, "false");
        }
    }

    private Map<String, Boolean> getVlanLastIpMap(Long vpcId, Long guestNetworkId) {
        // for network if the ips does not have any rules, then only last ip
        final Map<String, Boolean> vlanLastIpMap = new HashMap<String, Boolean>();
        final List<IPAddressVO> userIps;
        if (vpcId != null) {
            userIps = _ipAddressDao.listByAssociatedVpc(vpcId, null);
        } else {
            userIps = _ipAddressDao.listByAssociatedNetwork(guestNetworkId, null);
        }
        for (IPAddressVO ip : userIps) {
            String vlanTag = _vlanDao.findById(ip.getVlanId()).getVlanTag();
            Boolean lastIp = vlanLastIpMap.get(vlanTag);
            if (lastIp != null && !lastIp) {
                continue;
            }
            if (ip.isSourceNat()
                    || _rulesDao.countRulesByIpIdAndState(ip.getId(), FirewallRule.State.Active) > 0
                    || (ip.isOneToOneNat() && ip.getRuleState() == null)) {
                vlanLastIpMap.put(vlanTag, false);
            }
        }
        return vlanLastIpMap;
    }

    public void createStaticRouteCommands(final List<StaticRouteProfile> staticRoutes, final DomainRouterVO router, final Commands cmds) {
        final SetStaticRouteCommand cmd = new SetStaticRouteCommand(staticRoutes);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, _routerControlHelper.getRouterControlIp(router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        final DataCenterVO dcVo = _dcDao.findById(router.getDataCenterId());
        cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());
        cmds.addCommand(cmd);
    }

    public void createSite2SiteVpnCfgCommands(final Site2SiteVpnConnection conn, final boolean isCreate, final VirtualRouter router, final Commands cmds) {
        final Site2SiteCustomerGatewayVO gw = _s2sCustomerGatewayDao.findById(conn.getCustomerGatewayId());
        final Site2SiteVpnGatewayVO vpnGw = _s2sVpnGatewayDao.findById(conn.getVpnGatewayId());
        final IpAddress ip = _ipAddressDao.findById(vpnGw.getAddrId());
        final Vpc vpc = _vpcDao.findById(ip.getVpcId());
        final String localPublicIp = ip.getAddress().toString();
        final String localGuestCidr = vpc.getCidr();
        final String localPublicGateway = _vlanDao.findById(ip.getVlanId()).getVlanGateway();
        final String peerGatewayIp = gw.getGatewayIp();
        final String peerGuestCidrList = gw.getGuestCidrList();
        final String ipsecPsk = gw.getIpsecPsk();
        final String ikePolicy = gw.getIkePolicy();
        final String espPolicy = gw.getEspPolicy();
        final Long ikeLifetime = gw.getIkeLifetime();
        final Long espLifetime = gw.getEspLifetime();
        final Boolean dpd = gw.getDpd();
        final Boolean encap = gw.getEncap();
        final Boolean splitConnections = gw.getSplitConnections();
        final String ikeVersion = gw.getIkeVersion();

        final Site2SiteVpnCfgCommand cmd = new Site2SiteVpnCfgCommand(isCreate, localPublicIp, localPublicGateway, localGuestCidr, peerGatewayIp, peerGuestCidrList, ikePolicy,
                espPolicy, ipsecPsk, ikeLifetime, espLifetime, dpd, conn.isPassive(), encap, splitConnections, ikeVersion);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, _routerControlHelper.getRouterControlIp(router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, _routerControlHelper.getRouterControlIp(router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        final DataCenterVO dcVo = _dcDao.findById(router.getDataCenterId());
        cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());
        cmds.addCommand("applyS2SVpn", cmd);
    }

    public void createVpcAssociatePrivateIPCommands(final VirtualRouter router, final List<PrivateIpAddress> ips, final Commands cmds, final boolean add) {

        // Ensure that in multiple vlans case we first send all ip addresses of
        // vlan1, then all ip addresses of vlan2, etc..
        final Map<String, ArrayList<PrivateIpAddress>> vlanIpMap = new HashMap<String, ArrayList<PrivateIpAddress>>();
        for (final PrivateIpAddress ipAddress : ips) {
            final String vlanTag = ipAddress.getBroadcastUri();
            ArrayList<PrivateIpAddress> ipList = vlanIpMap.get(vlanTag);
            if (ipList == null) {
                ipList = new ArrayList<PrivateIpAddress>();
            }

            ipList.add(ipAddress);
            vlanIpMap.put(vlanTag, ipList);
        }

        for (final Map.Entry<String, ArrayList<PrivateIpAddress>> vlanAndIp : vlanIpMap.entrySet()) {
            final List<PrivateIpAddress> ipAddrList = vlanAndIp.getValue();
            final IpAddressTO[] ipsToSend = new IpAddressTO[ipAddrList.size()];
            int i = 0;

            for (final PrivateIpAddress ipAddr : ipAddrList) {
                final Network network = _networkModel.getNetwork(ipAddr.getNetworkId());
                final IpAddressTO ip = new IpAddressTO(Account.ACCOUNT_ID_SYSTEM, ipAddr.getIpAddress(), add, false, ipAddr.getSourceNat(), ipAddr.getBroadcastUri(),
                        ipAddr.getGateway(), ipAddr.getNetmask(), ipAddr.getMacAddress(), null, false);
                if (network.getPrivateMtu() != null) {
                    ip.setMtu(network.getPublicMtu());
                }
                setIpAddressNetworkParams(ip, network, router);
                ipsToSend[i++] = ip;

            }
            final IpAssocVpcCommand cmd = new IpAssocVpcCommand(ipsToSend);
            cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, _routerControlHelper.getRouterControlIp(router.getId()));
            cmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, _routerControlHelper.getRouterIpInNetwork(ipAddrList.get(0).getNetworkId(), router.getId()));
            cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
            final DataCenterVO dcVo = _dcDao.findById(router.getDataCenterId());
            cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());

            cmds.addCommand("IPAssocVpcCommand", cmd);
        }
    }

    public SetupGuestNetworkCommand createSetupGuestNetworkCommand(final DomainRouterVO router, final boolean add, final NicProfile guestNic) {
        final Network network = _networkModel.getNetwork(guestNic.getNetworkId());

        String defaultDns1 = null;
        String defaultDns2 = null;
        String defaultIp6Dns1 = null;
        String defaultIp6Dns2 = null;

        final boolean dnsProvided = _networkModel.isProviderSupportServiceInNetwork(network.getId(), Service.Dns, Provider.VPCVirtualRouter);
        final boolean dhcpProvided = _networkModel.isProviderSupportServiceInNetwork(network.getId(), Service.Dhcp, Provider.VPCVirtualRouter);

        final boolean setupDns = dnsProvided || dhcpProvided;

        if (setupDns) {
            final DataCenterVO dcVo = _dcDao.findById(router.getDataCenterId());
            defaultDns1 = guestNic.getIPv4Dns1();
            defaultDns2 = guestNic.getIPv4Dns2();
            if (org.apache.commons.lang3.StringUtils.isAllBlank(guestNic.getIPv4Dns1(), guestNic.getIPv4Dns2())) {
                Pair<String, String> dns = _networkModel.getNetworkIp4Dns(network, dcVo);
                defaultDns1 = dns.first();
                defaultDns2 = dns.second();
            }
            defaultIp6Dns1 = guestNic.getIPv6Dns1();
            defaultIp6Dns2 = guestNic.getIPv6Dns2();
            if (org.apache.commons.lang3.StringUtils.isAllBlank(guestNic.getIPv6Dns1(), guestNic.getIPv6Dns2())) {
                Pair<String, String> dns = _networkModel.getNetworkIp6Dns(network, dcVo);
                defaultIp6Dns1 = dns.first();
                defaultIp6Dns2 = dns.second();
            }
        }

        final Nic nic = _nicDao.findByNtwkIdAndInstanceId(network.getId(), router.getId());
        final String networkDomain = network.getNetworkDomain();
        final String dhcpRange = getGuestDhcpRange(guestNic, network, _entityMgr.findById(DataCenter.class, network.getDataCenterId()));

        final NicProfile nicProfile = _networkModel.getNicProfile(router, nic.getNetworkId(), null);
        final SetupGuestNetworkCommand setupCmd = new SetupGuestNetworkCommand(dhcpRange, networkDomain, router.getIsRedundantRouter(), defaultDns1, defaultDns2, add, _itMgr.toNicTO(nicProfile,
                router.getHypervisorType()));

        NicVO publicNic = _nicDao.findDefaultNicForVM(router.getId());
        if (publicNic != null) {
            updateSetupGuestNetworkCommandIpv6(setupCmd, network, publicNic, defaultIp6Dns1, defaultIp6Dns2);
        }

        final String brd = NetUtils.long2Ip(NetUtils.ip2Long(guestNic.getIPv4Address()) | ~NetUtils.ip2Long(guestNic.getIPv4Netmask()));
        setupCmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, _routerControlHelper.getRouterControlIp(router.getId()));
        setupCmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, _routerControlHelper.getRouterIpInNetwork(network.getId(), router.getId()));

        setupCmd.setAccessDetail(NetworkElementCommand.GUEST_NETWORK_GATEWAY, network.getGateway());
        setupCmd.setAccessDetail(NetworkElementCommand.GUEST_BRIDGE, brd);
        setupCmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());

        if (network.getBroadcastDomainType() == BroadcastDomainType.Vlan && network.getBroadcastUri() != null) {
            final long guestVlanTag = Long.parseLong(BroadcastDomainType.Vlan.getValueFrom(network.getBroadcastUri()));
            setupCmd.setAccessDetail(NetworkElementCommand.GUEST_VLAN_TAG, String.valueOf(guestVlanTag));
        }

        return setupCmd;
    }

    private void updateSetupGuestNetworkCommandIpv6(SetupGuestNetworkCommand setupCmd, Network network, Nic nic, String defaultIp6Dns1, String defaultIp6Dns2) {
        boolean isIpv6Supported = _networkOfferingDao.isIpv6Supported(network.getNetworkOfferingId());
        if (isIpv6Supported) {
            setupCmd.setDefaultIp6Dns1(defaultIp6Dns1);
            setupCmd.setDefaultIp6Dns2(defaultIp6Dns2);
            Nic updatedNic = ipv6Service.assignPublicIpv6ToNetwork(network, nic);
            setupCmd.setRouterIpv6(updatedNic.getIPv6Address());
            setupCmd.setRouterIpv6Gateway(updatedNic.getIPv6Gateway());
            setupCmd.setRouterIpv6Cidr(updatedNic.getIPv6Cidr());
        }
    }

    private VmDataCommand generateVmDataCommand(final VirtualRouter router, final String vmPrivateIpAddress, final String userData, String userDataDetails, final String serviceOffering,
                                                final String zoneName, final String publicIpAddress, final String vmName, final String vmInstanceName, final long vmId, final String vmUuid, final String publicKey,
                                                final long guestNetworkId, String hostname) {
        final VmDataCommand cmd = new VmDataCommand(vmPrivateIpAddress, vmName, _networkModel.getExecuteInSeqNtwkElmtCmd());

        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, _routerControlHelper.getRouterControlIp(router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, _routerControlHelper.getRouterIpInNetwork(guestNetworkId, router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());

        final DataCenterVO dcVo = _dcDao.findById(router.getDataCenterId());
        cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());

        cmd.addVmData("userdata", "user-data", userData);
        cmd.addVmData("metadata", "service-offering", StringUtils.unicodeEscape(serviceOffering));
        cmd.addVmData("metadata", "availability-zone", StringUtils.unicodeEscape(zoneName));
        cmd.addVmData("metadata", "local-ipv4", vmPrivateIpAddress);
        cmd.addVmData("metadata", "local-hostname", StringUtils.unicodeEscape(vmName));

        addUserDataDetailsToCommand(cmd, userDataDetails);

        Network network = _networkDao.findById(guestNetworkId);
        if (dcVo.getNetworkType() == NetworkType.Basic || network.getGuestType() == Network.GuestType.Shared) {
            cmd.addVmData("metadata", "public-ipv4", vmPrivateIpAddress);
            cmd.addVmData("metadata", "public-hostname", StringUtils.unicodeEscape(vmName));
        } else {
            if (publicIpAddress != null) {
                cmd.addVmData("metadata", "public-ipv4", publicIpAddress);
                cmd.addVmData("metadata", "public-hostname", publicIpAddress);
            } else if (router.getPublicIpAddress() != null) {
                cmd.addVmData("metadata", "public-ipv4", router.getPublicIpAddress());
                cmd.addVmData("metadata", "public-hostname", router.getPublicIpAddress());
            }
        }
        if (vmUuid == null) {
            cmd.addVmData("metadata", "instance-id", vmInstanceName);
            cmd.addVmData("metadata", "vm-id", String.valueOf(vmId));
        } else {
            cmd.addVmData("metadata", "instance-id", vmUuid);
            cmd.addVmData("metadata", "vm-id", vmUuid);
        }
        cmd.addVmData("metadata", "public-keys", publicKey);

        String cloudIdentifier = _configDao.getValue("cloud.identifier");
        if (cloudIdentifier == null) {
            cloudIdentifier = "";
        } else {
            cloudIdentifier = "CloudStack-{" + cloudIdentifier + "}";
        }
        cmd.addVmData("metadata", "cloud-identifier", cloudIdentifier);

        cmd.addVmData("metadata", "hypervisor-host-name", hostname);
        return cmd;
    }

    protected void addUserDataDetailsToCommand(VmDataCommand cmd, String userDataDetails) {
        if(userDataDetails != null && !userDataDetails.isEmpty()) {
            userDataDetails = userDataDetails.substring(1, userDataDetails.length()-1);
            String[] keyValuePairs = userDataDetails.split(",");
            for(String pair : keyValuePairs)
            {
                final Pair<String, String> keyValue = StringUtils.getKeyValuePairWithSeparator(pair, "=");
                cmd.addVmData("metadata", keyValue.first(), keyValue.second());
            }
        }
    }

    private NicVO findGatewayIp(final long userVmId) {
        final NicVO defaultNic = _nicDao.findDefaultNicForVM(userVmId);
        return defaultNic;
    }

    private NicVO findDefaultDnsIp(final long userVmId) {
        final NicVO defaultNic = _nicDao.findDefaultNicForVM(userVmId);

        // check if DNS provider is the domR
        if (defaultNic == null || !_networkModel.isProviderSupportServiceInNetwork(defaultNic.getNetworkId(), Service.Dns, Provider.VirtualRouter)) {
            return null;
        }

        final NetworkOffering offering = _networkOfferingDao.findById(_networkDao.findById(defaultNic.getNetworkId()).getNetworkOfferingId());
        if (offering.isRedundantRouter()) {
            return findGatewayIp(userVmId);
        }

        final DataCenter dc = _dcDao.findById(_networkModel.getNetwork(defaultNic.getNetworkId()).getDataCenterId());
        final boolean isZoneBasic = dc.getNetworkType() == NetworkType.Basic;

        // find domR's nic in the network
        NicVO domrDefaultNic;
        if (isZoneBasic) {
            domrDefaultNic = _nicDao.findByNetworkIdTypeAndGateway(defaultNic.getNetworkId(), VirtualMachine.Type.DomainRouter, defaultNic.getIPv4Gateway());
        } else {
            domrDefaultNic = _nicDao.findByNetworkIdAndType(defaultNic.getNetworkId(), VirtualMachine.Type.DomainRouter);
        }
        return domrDefaultNic;
    }

    protected String getGuestDhcpRange(final NicProfile guestNic, final Network guestNetwork, final DataCenter dc) {
        String dhcpRange = null;
        // setup dhcp range
        if (dc.getNetworkType() == NetworkType.Basic) {
            final long cidrSize = NetUtils.getCidrSize(guestNic.getIPv4Netmask());
            final String cidr = NetUtils.getCidrSubNet(guestNic.getIPv4Gateway(), cidrSize);
            if (cidr != null) {
                dhcpRange = NetUtils.getIpRangeStartIpFromCidr(cidr, cidrSize);
            }
        } else if (dc.getNetworkType() == NetworkType.Advanced) {
            final String cidr = guestNetwork.getCidr();
            if (cidr != null) {
                dhcpRange = NetUtils.getDhcpRange(cidr);
            }
        }
        return dhcpRange;
    }

    private void setIpAddressNetworkParams(IpAddressTO ipAddress, final Network network, final VirtualRouter router) {
        if (_networkModel.isPrivateGateway(network.getId())) {
            s_logger.debug("network " + network.getId() + " (name: " + network.getName() + " ) is a vpc private gateway, set traffic type to Public");
            ipAddress.setTrafficType(TrafficType.Public);
            ipAddress.setPrivateGateway(true);
        } else {
            ipAddress.setTrafficType(network.getTrafficType());
            ipAddress.setPrivateGateway(false);
        }
        ipAddress.setNetworkName(_networkModel.getNetworkTag(router.getHypervisorType(), network));
        final NetworkOfferingVO networkOfferingVO = _networkOfferingDao.findById(network.getNetworkOfferingId());
        NicTO nicTO = new NicTO();
        nicTO.setMac(ipAddress.getVifMacAddress());
        nicTO.setType(ipAddress.getTrafficType());
        nicTO.setGateway(ipAddress.getVlanGateway());
        nicTO.setBroadcastUri(BroadcastDomainType.fromString(ipAddress.getBroadcastUri()));
        nicTO.setType(network.getTrafficType());
        nicTO.setName(_networkModel.getNetworkTag(router.getHypervisorType(), network));
        nicTO.setBroadcastType(network.getBroadcastDomainType());
        nicTO.setIsolationuri(network.getBroadcastUri());
        nicTO.setNetworkRateMbps(_configMgr.getNetworkOfferingNetworkRate(networkOfferingVO.getId(), network.getDataCenterId()));
        nicTO.setSecurityGroupEnabled(_networkModel.isSecurityGroupSupportedInNetwork(network));
        nicTO.setDetails(getNicDetails(network));

        ipAddress.setNicTO(nicTO);
    }

    private Map<NetworkOffering.Detail, String> getNicDetails(Network network) {
        if (network == null) {
            s_logger.debug("Unable to get NIC details as the network is null");
            return null;
        }
        Map<NetworkOffering.Detail, String> details = networkOfferingDetailsDao.getNtwkOffDetails(network.getNetworkOfferingId());
        if (details != null) {
            details.putIfAbsent(NetworkOffering.Detail.PromiscuousMode, NetworkOrchestrationService.PromiscuousMode.value().toString());
            details.putIfAbsent(NetworkOffering.Detail.MacAddressChanges, NetworkOrchestrationService.MacAddressChanges.value().toString());
            details.putIfAbsent(NetworkOffering.Detail.ForgedTransmits, NetworkOrchestrationService.ForgedTransmits.value().toString());
            details.putIfAbsent(NetworkOffering.Detail.MacLearning, NetworkOrchestrationService.MacLearning.value().toString());
        }
        NetworkDetailVO pvlantypeDetail = networkDetailsDao.findDetail(network.getId(), ApiConstants.ISOLATED_PVLAN_TYPE);
        if (pvlantypeDetail != null) {
            details.putIfAbsent(NetworkOffering.Detail.pvlanType, pvlantypeDetail.getValue());
        }
        return details;
    }

    public void setupUpdateNetworkCommands(final VirtualRouter router, final Set<IpAddressTO> ips, Commands cmds) {
        IpAddressTO[] ipsToSend = ips.toArray(new IpAddressTO[0]);
        if (!ips.isEmpty()) {
            UpdateNetworkCommand cmd = new UpdateNetworkCommand(ipsToSend);
            cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, _routerControlHelper.getRouterControlIp(router.getId()));
            cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
            final DataCenterVO dcVo = _dcDao.findById(router.getDataCenterId());
            cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());
            cmds.addCommand("updateNetwork", cmd);
        }
    }
}
