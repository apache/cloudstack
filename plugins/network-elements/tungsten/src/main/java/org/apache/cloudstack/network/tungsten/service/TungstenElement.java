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
package org.apache.cloudstack.network.tungsten.service;

import com.cloud.agent.AgentManager;
import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.to.LoadBalancerTO;
import com.cloud.api.ApiDBUtils;
import com.cloud.configuration.Config;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.NetworkMigrationResponder;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.TungstenGuestNetworkIpAddressVO;
import com.cloud.network.TungstenProvider;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.LoadBalancerCertMapDao;
import com.cloud.network.dao.LoadBalancerCertMapVO;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVMMapDao;
import com.cloud.network.dao.LoadBalancerVMMapVO;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkDetailVO;
import com.cloud.network.dao.NetworkDetailsDao;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkTrafficTypeDao;
import com.cloud.network.dao.PhysicalNetworkTrafficTypeVO;
import com.cloud.network.dao.TungstenGuestNetworkIpAddressDao;
import com.cloud.network.dao.TungstenProviderDao;
import com.cloud.network.element.DnsServiceProvider;
import com.cloud.network.element.FirewallServiceProvider;
import com.cloud.network.element.IpDeployer;
import com.cloud.network.element.LoadBalancingServiceProvider;
import com.cloud.network.element.PortForwardingServiceProvider;
import com.cloud.network.element.StaticNatServiceProvider;
import com.cloud.network.element.TungstenProviderVO;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.LbStickinessMethod;
import com.cloud.network.rules.LoadBalancerContainer;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.network.rules.StaticNat;
import com.cloud.offering.NetworkOffering;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceStateAdapter;
import com.cloud.resource.ServerResource;
import com.cloud.user.AccountManager;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.TungstenUtils;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.StateListener;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.utils.net.Ip;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.google.gson.Gson;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.PublishScope;
import org.apache.cloudstack.network.tungsten.agent.api.ApplyTungstenNetworkPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ApplyTungstenPortForwardingCommand;
import org.apache.cloudstack.network.tungsten.agent.api.AssignTungstenFloatingIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenNetworkLoadbalancerCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenNetworkPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenVirtualMachineCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenFloatingIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenLoadBalancerCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenLoadBalancerListenerCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenNetworkPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenVRouterPortCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenVmCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenVmInterfaceCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ReleaseTungstenFloatingIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.SetupTfRouteCommand;
import org.apache.cloudstack.network.tungsten.agent.api.SetupTungstenVRouterCommand;
import org.apache.cloudstack.network.tungsten.agent.api.StartupTungstenCommand;
import org.apache.cloudstack.network.tungsten.agent.api.TungstenAnswer;
import org.apache.cloudstack.network.tungsten.agent.api.TungstenCommand;
import org.apache.cloudstack.network.tungsten.agent.api.UpdateTungstenLoadBalancerHealthMonitorCommand;
import org.apache.cloudstack.network.tungsten.agent.api.UpdateTungstenLoadBalancerMemberCommand;
import org.apache.cloudstack.network.tungsten.agent.api.UpdateTungstenLoadBalancerPoolCommand;
import org.apache.cloudstack.network.tungsten.dao.TungstenFabricLBHealthMonitorDao;
import org.apache.cloudstack.network.tungsten.dao.TungstenFabricLBHealthMonitorVO;
import org.apache.cloudstack.network.tungsten.model.TungstenLoadBalancerMember;
import org.apache.cloudstack.network.tungsten.model.TungstenRule;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

@Component
public class TungstenElement extends AdapterBase
    implements StaticNatServiceProvider, IpDeployer, FirewallServiceProvider,
    LoadBalancingServiceProvider, PortForwardingServiceProvider, ResourceStateAdapter, DnsServiceProvider, Listener,
    StateListener<VirtualMachine.State, VirtualMachine.Event, VirtualMachine>, NetworkMigrationResponder {
    private static final Logger s_logger = Logger.getLogger(TungstenElement.class);

    private static final String NETWORK = "network";

    private final Map<Network.Service, Map<Network.Capability, String>> capabilities = initCapabilities();
    @Inject
    HostPodDao podDao;
    @Inject
    DataCenterDao dataCenterDao;
    @Inject
    NetworkModel networkModel;
    @Inject
    NetworkDao networkDao;
    @Inject
    AccountManager accountMgr;
    @Inject
    ResourceManager resourceMgr;
    @Inject
    IPAddressDao ipAddressDao;
    @Inject
    IpAddressManager ipAddressMgr;
    @Inject
    PhysicalNetworkDao physicalNetworkDao;
    @Inject
    VMInstanceDao vmInstanceDao;
    @Inject
    AgentManager agentMgr;
    @Inject
    ConfigurationDao configDao;
    @Inject
    VlanDao vlanDao;
    @Inject
    HostDao hostDao;
    @Inject
    NicDao nicDao;
    @Inject
    NetworkServiceMapDao networkServiceMapDao;
    @Inject
    NetworkDetailsDao networkDetailsDao;
    @Inject
    LoadBalancerVMMapDao lbVmMapDao;
    @Inject
    LoadBalancerDao lbDao;
    @Inject
    TungstenService tungstenService;
    @Inject
    MessageBus messageBus;
    @Inject
    TungstenProviderDao tungstenProviderDao;
    @Inject
    TungstenGuestNetworkIpAddressDao tungstenGuestNetworkIpAddressDao;
    @Inject
    TungstenFabricUtils tungstenFabricUtils;
    @Inject
    PhysicalNetworkTrafficTypeDao physicalNetworkTrafficTypeDao;
    @Inject
    TungstenFabricLBHealthMonitorDao tungstenFabricLBHealthMonitorDao;
    @Inject
    LoadBalancerCertMapDao loadBalancerCertMapDao;

    private static Map<Network.Service, Map<Network.Capability, String>> initCapabilities() {
        Map<Network.Service, Map<Network.Capability, String>> capabilities = new HashMap<>();
        Map<Network.Capability, String> dhcpCapabilities = new HashMap<>();
        dhcpCapabilities.put(Network.Capability.DhcpAccrossMultipleSubnets, "true");
        capabilities.put(Network.Service.Dhcp, dhcpCapabilities);
        Map<Network.Capability, String> sourceNatCapabilities = new HashMap<>();
        sourceNatCapabilities.put(Network.Capability.RedundantRouter, "true");
        sourceNatCapabilities.put(Network.Capability.SupportedSourceNatTypes, "peraccount");
        capabilities.put(Network.Service.SourceNat, sourceNatCapabilities);
        capabilities.put(Network.Service.Connectivity, null);
        capabilities.put(Network.Service.SecurityGroup, null);
        capabilities.put(Network.Service.StaticNat, null);
        Map<Network.Capability, String> dnsCapabilities = new HashMap<>();
        dnsCapabilities.put(Network.Capability.AllowDnsSuffixModification, "true");
        capabilities.put(Network.Service.Dns, dnsCapabilities);
        Map<Network.Capability, String> firewallCapabilities = new HashMap<>();
        firewallCapabilities.put(Network.Capability.SupportedProtocols, "tcp,udp,icmp");
        firewallCapabilities.put(Network.Capability.SupportedEgressProtocols, "tcp,udp,icmp,all");
        firewallCapabilities.put(Network.Capability.MultipleIps, "true");
        firewallCapabilities.put(Network.Capability.TrafficStatistics, "per public ip");
        firewallCapabilities.put(Network.Capability.SupportedTrafficDirection, "ingress, egress");
        capabilities.put(Network.Service.Firewall, firewallCapabilities);

        Map<Network.Capability, String> lbCapabilities = new HashMap<>();
        lbCapabilities.put(Network.Capability.SupportedLBAlgorithms, "roundrobin,leastconn,source");
        lbCapabilities.put(Network.Capability.SupportedLBIsolation, "dedicated");
        lbCapabilities.put(Network.Capability.SupportedProtocols, "tcp,udp,http,ssl");
        lbCapabilities.put(Network.Capability.TrafficStatistics, "per public ip");
        lbCapabilities.put(Network.Capability.LoadBalancingSupportedIps, "additional");
        lbCapabilities.put(Network.Capability.LbSchemes, LoadBalancerContainer.Scheme.Public.toString());
        lbCapabilities.put(Network.Capability.SslTermination, "true");

        LbStickinessMethod method;
        List<LbStickinessMethod> methodList = new ArrayList<>();
        method = new LbStickinessMethod(LbStickinessMethod.StickinessMethodType.LBCookieBased,
            "This is cookie based sticky method, can be used only for http");
        methodList.add(method);

        method = new LbStickinessMethod(LbStickinessMethod.StickinessMethodType.AppCookieBased,
            "This is app session based sticky method, can be used only for http");
        methodList.add(method);
        method.addParam("name", true, "cookie name passed in http header by apllication to the client", false);

        method = new LbStickinessMethod(LbStickinessMethod.StickinessMethodType.SourceBased,
            "This is source based sticky method, can be used for any type of protocol.");
        methodList.add(method);

        Gson gson = new Gson();
        String stickyMethodList = gson.toJson(methodList);
        lbCapabilities.put(Network.Capability.SupportedStickinessMethods, stickyMethodList);

        lbCapabilities.put(Network.Capability.HealthCheckPolicy, "true");
        capabilities.put(Network.Service.Lb, lbCapabilities);
        capabilities.put(Network.Service.PortForwarding, null);

        return capabilities;
    }

    protected boolean canHandle(Network network, Network.Service service) {
        s_logger.debug("Checking if TungstenElement can handle service " + service.getName() + " on network "
            + network.getDisplayText());

        if (!networkModel.isProviderForNetwork(getProvider(), network.getId())) {
            s_logger.debug("TungstenElement is not a provider for network " + network.getDisplayText());
            return false;
        }

        return true;
    }

    @Override
    public boolean applyStaticNats(Network config, List<? extends StaticNat> rules) {
        for(StaticNat staticNat : rules) {
            long sourceIpAddressId = staticNat.getSourceIpAddressId();
            IPAddressVO ipAddressVO = ipAddressDao.findByIdIncludingRemoved(sourceIpAddressId);
            VMInstanceVO vm = vmInstanceDao.findByIdIncludingRemoved(ipAddressVO.getAssociatedWithVmId());
            // floating ip is released when nic was deleted
            if (vm == null || networkModel.getNicInNetworkIncludingRemoved(vm.getId(), config.getId()) == null) {
                continue;
            }
            Nic nic = networkModel.getNicInNetworkIncludingRemoved(vm.getId(), config.getId());
            Network publicNetwork = networkModel.getSystemNetworkByZoneAndTrafficType(config.getDataCenterId(), Networks.TrafficType.Public);
            if (!staticNat.isForRevoke()) {
                TungstenCommand assignTungstenFloatingIpCommand = new AssignTungstenFloatingIpCommand(publicNetwork.getUuid(), nic.getUuid(),
                    TungstenUtils.getFloatingIpPoolName(config.getDataCenterId()), TungstenUtils.getFloatingIpName(ipAddressVO.getId()), nic.getIPv4Address());
                TungstenAnswer assignFloatingIpAnswer = tungstenFabricUtils.sendTungstenCommand(
                    assignTungstenFloatingIpCommand, config.getDataCenterId());
                return assignFloatingIpAnswer.getResult();
            } else {
                TungstenCommand releaseTungstenFloatingIpCommand = new ReleaseTungstenFloatingIpCommand(publicNetwork.getUuid(), TungstenUtils.getFloatingIpPoolName(config.getDataCenterId()),
                    TungstenUtils.getFloatingIpName(ipAddressVO.getId()));
                TungstenAnswer releaseFloatingIpAnswer = tungstenFabricUtils.sendTungstenCommand(
                    releaseTungstenFloatingIpCommand, config.getDataCenterId());
                return releaseFloatingIpAnswer.getResult();
            }
        }

        return true;
    }

    @Override
    public boolean applyLBRules(Network network, List<LoadBalancingRule> rules) {
        Network publicNetwork = networkModel.getSystemNetworkByZoneAndTrafficType(network.getDataCenterId(),
            Networks.TrafficType.Public);
        for (LoadBalancingRule loadBalancingRule : rules) {
            String tungstenProjectFqn = tungstenService.getTungstenProjectFqn(network);
            LoadBalancerCertMapVO loadBalancerCertMapVO = loadBalancerCertMapDao.findByLbRuleId(loadBalancingRule.getId());
            if ((loadBalancerCertMapVO == null || !loadBalancerCertMapVO.isRevoke()) && loadBalancingRule.getState() == FirewallRule.State.Add) {
                String protocol = StringUtils.upperCase(loadBalancingRule.getLbProtocol());
                if (loadBalancingRule.getSourcePortStart() == NetUtils.HTTP_PORT
                    || loadBalancingRule.getSourcePortStart() == NetUtils.HTTPS_PORT) {
                    protocol = "HTTP";
                }

                IPAddressVO ipAddressVO = ipAddressDao.findByIpAndDcId(network.getDataCenterId(),
                    loadBalancingRule.getSourceIp().addr());

                List<TungstenLoadBalancerMember> memberIp = createTungstenLoadBalancerMember(loadBalancingRule);

                createLoadBalancer(network, publicNetwork, loadBalancingRule, ipAddressVO, memberIp, tungstenProjectFqn, protocol);

                updateLBHealthMonitor(network, loadBalancingRule, ipAddressVO, tungstenProjectFqn);

                updateLBPool(network, loadBalancingRule, protocol, tungstenProjectFqn);

                updateLoadBalancerMember(network, loadBalancingRule, memberIp, tungstenProjectFqn);

                return tungstenService.updateLoadBalancer(network, loadBalancingRule);
            }

            if (loadBalancingRule.getState() == FirewallRule.State.Revoke) {
                return applyRevokeRule(network, loadBalancingRule, publicNetwork, tungstenProjectFqn);
            }
        }

        return true;
    }

    private void updateLoadBalancerMember(Network network, LoadBalancingRule loadBalancingRule, List<TungstenLoadBalancerMember> memberIp, String tungstenProjectFqn) {
        TungstenCommand updateTungstenLoadBalancerMemberCommand = new UpdateTungstenLoadBalancerMemberCommand(
                tungstenProjectFqn, network.getUuid(), TungstenUtils.getLoadBalancerPoolName(loadBalancingRule.getId()), memberIp);
        TungstenAnswer updateTungstenLoadBalancerMemberAnswer = tungstenFabricUtils.sendTungstenCommand(
                updateTungstenLoadBalancerMemberCommand, network.getDataCenterId());
        if (!updateTungstenLoadBalancerMemberAnswer.getResult()) {
            throw new CloudRuntimeException("Can not create Tungsten Fabric Load Balancer Member");
        }
    }

    private void createLoadBalancer(Network network, Network publicNetwork, LoadBalancingRule loadBalancingRule, IPAddressVO ipAddressVO, List<TungstenLoadBalancerMember> memberIp, String tungstenProjectFqn, String protocol) {
        String lbIp = getLoadBalancerIpAddress(network, ipAddressVO);
        TungstenCommand createTungstenNetworkLoadbalancerCommand = new CreateTungstenNetworkLoadbalancerCommand(
                tungstenProjectFqn, network.getUuid(), publicNetwork.getUuid(), TungstenUtils.getLoadBalancerAlgorithm(loadBalancingRule.getAlgorithm()),
                TungstenUtils.getLoadBalancerName(ipAddressVO.getId()), TungstenUtils.getLoadBalancerListenerName(loadBalancingRule.getId()),
                TungstenUtils.getLoadBalancerPoolName(loadBalancingRule.getId()), TungstenUtils.getLoadBalancerHealthMonitorName(ipAddressVO.getId()),
                TungstenUtils.getLoadBalancerVmiName(ipAddressVO.getId()), TungstenUtils.getLoadBalancerIiName(ipAddressVO.getId()), loadBalancingRule.getId(), memberIp,
                protocol, loadBalancingRule.getSourcePortStart(), loadBalancingRule.getDefaultPortStart(), lbIp,
                TungstenUtils.getFloatingIpPoolName(network.getDataCenterId()), TungstenUtils.getFloatingIpName(ipAddressVO.getId()),
                TungstenService.MonitorType.PING.toString(), 3, 5, 5, null, null, null);
        TungstenAnswer createTungstenNetworkLoadbalancerAnswer = tungstenFabricUtils.sendTungstenCommand(
                createTungstenNetworkLoadbalancerCommand, network.getDataCenterId());
        if (!createTungstenNetworkLoadbalancerAnswer.getResult()) {
            throw new CloudRuntimeException("Can not create Tungsten Fabric Load Balancer");
        }
    }

    private List<TungstenLoadBalancerMember> createTungstenLoadBalancerMember(LoadBalancingRule loadBalancingRule) {
        List<TungstenLoadBalancerMember> memberIp = new ArrayList<>();

        List<LoadBalancerVMMapVO> loadBalancerVMMapVOList = lbVmMapDao.listByLoadBalancerId(
                loadBalancingRule.getId(), false);

        for (LoadBalancerVMMapVO lbVMMapVO : loadBalancerVMMapVOList) {
            int port = loadBalancingRule.getDefaultPortStart();
            if (loadBalancingRule.getDefaultPortStart() == NetUtils.HTTPS_PORT) {
                port = NetUtils.HTTP_PORT;
            }

            TungstenLoadBalancerMember tungstenLoadBalancerMember = new TungstenLoadBalancerMember(
                    TungstenUtils.getLoadBalancerMemberName(loadBalancingRule.getId(), lbVMMapVO.getInstanceIp()),
                    lbVMMapVO.getInstanceIp(), port, 1);
            memberIp.add(tungstenLoadBalancerMember);
        }

        return memberIp;
    }

    private String getLoadBalancerIpAddress(Network network, IPAddressVO ipAddressVO) {
        String lbIp;
        TungstenGuestNetworkIpAddressVO guestNetworkIpAddressVO = tungstenGuestNetworkIpAddressDao.findByNetworkIdAndPublicIp(
                network.getId(), ipAddressVO.getAddress().addr());

        if (guestNetworkIpAddressVO == null) {
            lbIp = ipAddressMgr.acquireGuestIpAddress(network, null);

            if (lbIp == null) {
                throw new CloudRuntimeException("Can not acquire ip address for loadbalancer");
            }

            // register tungsten guest network ip
            TungstenGuestNetworkIpAddressVO tungstenGuestNetworkIpAddressVO = new TungstenGuestNetworkIpAddressVO();
            tungstenGuestNetworkIpAddressVO.setNetworkId(network.getId());
            tungstenGuestNetworkIpAddressVO.setPublicIpAddress(ipAddressVO.getAddress());
            tungstenGuestNetworkIpAddressVO.setGuestIpAddress(new Ip(lbIp));
            tungstenGuestNetworkIpAddressDao.persist(tungstenGuestNetworkIpAddressVO);
        } else {
            lbIp = guestNetworkIpAddressVO.getGuestIpAddress().addr();
        }

        return lbIp;
    }

    private void updateLBPool(Network network, LoadBalancingRule loadBalancingRule, String protocol, String tungstenProjectFqn) {
        // update stickiness, algorithm, protocol
        List<LoadBalancingRule.LbStickinessPolicy> lbStickinessPolicyList = loadBalancingRule.getStickinessPolicies();
        String lbSessionPersistence = null;
        String lbPersistenceCookieName = null;
        if (!lbStickinessPolicyList.isEmpty()) {
            lbSessionPersistence = TungstenUtils.getLoadBalancerSession(lbStickinessPolicyList.get(0).getMethodName());
            if (lbStickinessPolicyList.get(0).getMethodName().equals("AppCookie")) {
                lbPersistenceCookieName = lbStickinessPolicyList.get(0).getParams().get(0).second();
            }
        }

        // update haproxy
        TungstenCommand updateTungstenLoadBalancerPoolCommand;
        String lbStatsVisibility = configDao.getValue(Config.NetworkLBHaproxyStatsVisbility.key());
        if (!lbStatsVisibility.equals("disabled")) {
            String lbStatsUri = configDao.getValue(Config.NetworkLBHaproxyStatsUri.key());
            String lbStatsAuth = configDao.getValue(Config.NetworkLBHaproxyStatsAuth.key());
            String lbStatsPort = configDao.getValue(Config.NetworkLBHaproxyStatsPort.key());
            updateTungstenLoadBalancerPoolCommand = new UpdateTungstenLoadBalancerPoolCommand(
                    tungstenProjectFqn, TungstenUtils.getLoadBalancerPoolName(loadBalancingRule.getId()),
                    TungstenUtils.getLoadBalancerAlgorithm(loadBalancingRule.getAlgorithm()),
                    lbSessionPersistence, lbPersistenceCookieName, protocol, true, lbStatsPort, lbStatsUri,
                    lbStatsAuth);
        } else {
            updateTungstenLoadBalancerPoolCommand = new UpdateTungstenLoadBalancerPoolCommand(
                    tungstenProjectFqn, TungstenUtils.getLoadBalancerPoolName(loadBalancingRule.getId()),
                    TungstenUtils.getLoadBalancerAlgorithm(loadBalancingRule.getAlgorithm()),
                    lbSessionPersistence, lbPersistenceCookieName, protocol, false, null, null, null);
        }
        TungstenAnswer updateTungstenLoadBalancerPoolAnswer = tungstenFabricUtils.sendTungstenCommand(
                updateTungstenLoadBalancerPoolCommand, network.getDataCenterId());
        if (!updateTungstenLoadBalancerPoolAnswer.getResult()) {
            throw new CloudRuntimeException("Can not update Tungsten Fabric Load Balancer Pool");
        }
    }

    private void updateLBHealthMonitor(Network network, LoadBalancingRule loadBalancingRule, IPAddressVO ipAddressVO, String tungstenProjectFqn) {
        // update load balancer health monitor
        TungstenFabricLBHealthMonitorVO tungstenFabricLBHealthMonitorVO = tungstenFabricLBHealthMonitorDao.findByLbId(
                loadBalancingRule.getId());
        if (tungstenFabricLBHealthMonitorVO != null) {
            TungstenCommand updateTungstenHealthMonitorCommand = new UpdateTungstenLoadBalancerHealthMonitorCommand(
                    tungstenProjectFqn, TungstenUtils.getLoadBalancerHealthMonitorName(ipAddressVO.getId()),
                    tungstenFabricLBHealthMonitorVO.getType(), tungstenFabricLBHealthMonitorVO.getRetry(),
                    tungstenFabricLBHealthMonitorVO.getTimeout(), tungstenFabricLBHealthMonitorVO.getInterval(),
                    tungstenFabricLBHealthMonitorVO.getHttpMethod(), tungstenFabricLBHealthMonitorVO.getExpectedCode(),
                    tungstenFabricLBHealthMonitorVO.getUrlPath());
            TungstenAnswer updateTungstenHealthMonitorAnswer = tungstenFabricUtils.sendTungstenCommand(
                    updateTungstenHealthMonitorCommand, network.getDataCenterId());
            if (!updateTungstenHealthMonitorAnswer.getResult()) {
                throw new CloudRuntimeException("Can not update Tungsten Fabric Health Monitor");
            }
        }
    }

    private boolean applyRevokeRule(Network network, LoadBalancingRule loadBalancingRule, Network publicNetwork, String tungstenProjectFqn) {
        IPAddressVO ipAddressVO = ipAddressDao.findByIpAndDcId(network.getDataCenterId(),
                loadBalancingRule.getSourceIp().addr());
        List<LoadBalancerVO> listLoadBalancerVO = lbDao.listByIpAddress(ipAddressVO.getId());
        if (listLoadBalancerVO.size() > 1) {
            TungstenCommand deleteTungstenLoadBalancerListenerCommand =
                    new DeleteTungstenLoadBalancerListenerCommand(
                            tungstenProjectFqn, TungstenUtils.getLoadBalancerListenerName(loadBalancingRule.getId()));
            TungstenAnswer deleteTungstenLoadBalancerListenerAnswer = tungstenFabricUtils.sendTungstenCommand(
                    deleteTungstenLoadBalancerListenerCommand, network.getDataCenterId());
            if (!deleteTungstenLoadBalancerListenerAnswer.getResult()) {
                return false;
            }

            return tungstenService.updateLoadBalancer(network, loadBalancingRule);
        } else {
            TungstenCommand deleteTungstenLoadBalancerCommand = new DeleteTungstenLoadBalancerCommand(
                    tungstenProjectFqn, publicNetwork.getUuid(),
                    TungstenUtils.getLoadBalancerName(ipAddressVO.getId()),
                    TungstenUtils.getLoadBalancerHealthMonitorName(ipAddressVO.getId()),
                    TungstenUtils.getLoadBalancerVmiName(ipAddressVO.getId()),
                    TungstenUtils.getFloatingIpPoolName(network.getDataCenterId()),
                    TungstenUtils.getFloatingIpName(ipAddressVO.getId()));
            TungstenAnswer deleteTungstenLoadBalancerAnswer = tungstenFabricUtils.sendTungstenCommand(
                    deleteTungstenLoadBalancerCommand, network.getDataCenterId());
            if (!deleteTungstenLoadBalancerAnswer.getResult()) {
                return false;
            }

            TungstenGuestNetworkIpAddressVO guestNetworkIpAddressVO =
                    tungstenGuestNetworkIpAddressDao.findByNetworkIdAndPublicIp(
                            network.getId(), ipAddressVO.getAddress().addr());
            if (guestNetworkIpAddressVO != null) {
                return tungstenGuestNetworkIpAddressDao.remove(guestNetworkIpAddressVO.getId());
            }
        }

        return true;
    }

    @Override
    public boolean validateLBRule(Network network, LoadBalancingRule rule) {
        return true;
    }

    @Override
    public List<LoadBalancerTO> updateHealthChecks(Network network, List<LoadBalancingRule> lbrules) {
        return new ArrayList<>();
    }

    @Override
    public boolean handlesOnlyRulesInTransitionState() {
        return false;
    }

    @Override
    public boolean applyPFRules(final Network network, final List<PortForwardingRule> rules) {
        Network publicNetwork = networkModel.getSystemNetworkByZoneAndTrafficType(network.getDataCenterId(),
            Networks.TrafficType.Public);
        // tungsten port forwarding don't support multiple vm per public ip address
        // if multiple vm, only 1 vm get effect
        // frontend : don't allow to add multiple vm per public ip address
        for (PortForwardingRule rule : rules) {
            IPAddressVO publicIp = ApiDBUtils.findIpAddressById(rule.getSourceIpAddressId());
            UserVm vm = ApiDBUtils.findUserVmById(rule.getVirtualMachineId());
            if (vm == null || networkModel.getNicInNetwork(vm.getId(), network.getId()) == null) {
                continue;
            }
            Nic guestNic = networkModel.getNicInNetwork(vm.getId(), network.getId());
            if (rule.getState() == FirewallRule.State.Add || rule.getState() == FirewallRule.State.Revoke) {
                TungstenCommand applyTungstenPortForwardingCommand = new ApplyTungstenPortForwardingCommand(
                    rule.getState() == FirewallRule.State.Add, publicNetwork.getUuid(),
                    TungstenUtils.getFloatingIpPoolName(network.getDataCenterId()),
                    TungstenUtils.getFloatingIpName(publicIp.getId()), guestNic.getUuid(),
                    StringUtils.upperCase(rule.getProtocol()), rule.getSourcePortStart(),
                    rule.getDestinationPortStart());
                TungstenAnswer applyTungstenPortForwardingAnswer = tungstenFabricUtils.sendTungstenCommand(
                    applyTungstenPortForwardingCommand, network.getDataCenterId());
                if (!applyTungstenPortForwardingAnswer.getResult()) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public IpDeployer getIpDeployer(Network network) {
        return this;
    }

    @Override
    public Map<Network.Service, Map<Network.Capability, String>> getCapabilities() {
        return capabilities;
    }

    @Override
    public boolean applyIps(Network network, List<? extends PublicIpAddress> ipAddress, Set<Network.Service> services) {
        return true;
    }

    @Override
    public Network.Provider getProvider() {
        return Network.Provider.Tungsten;
    }

    @Override
    public boolean implement(Network network, NetworkOffering offering, DeployDestination dest,
        ReservationContext context)
        throws ConcurrentOperationException {
        return true;
    }

    @Override
    public boolean prepare(Network network, NicProfile nic, VirtualMachineProfile vm, DeployDestination dest,
        ReservationContext context)
        throws ConcurrentOperationException {
        if (network.getTrafficType() == Networks.TrafficType.Public) {
            TungstenAnswer createVirtualMachineAnswer = createTungstenVM(network, nic, vm, dest);
            if (!createVirtualMachineAnswer.getResult()) {
                throw new CloudRuntimeException("can not create Tungsten-Fabric vm");
            }

            // get tungsten public firewall rule
            IPAddressVO ipAddressVO = ipAddressDao.findByIpAndDcId(network.getDataCenterId(), nic.getIPv4Address());
            List<TungstenRule> tungstenRuleList = createDefaultTungstenFirewallRuleList(vm.getType(),
                nic.getIPv4Address());

            // create tungsten public network policy for system vm
            TungstenCommand createTungstenNetworkPolicyCommand = new CreateTungstenNetworkPolicyCommand(
                TungstenUtils.getPublicNetworkPolicyName(ipAddressVO.getId()), null, tungstenRuleList);
            TungstenAnswer createTungstenNetworkPolicyAnswer = tungstenFabricUtils.sendTungstenCommand(
                createTungstenNetworkPolicyCommand, network.getDataCenterId());
            if (!createTungstenNetworkPolicyAnswer.getResult()) {
                throw new CloudRuntimeException("can not create Tungsten-Fabric public network policy");
            }

            messageBus.publish(_name, TungstenService.MESSAGE_APPLY_NETWORK_POLICY_EVENT, PublishScope.LOCAL, network);

            updateTungstenFabricService(nic);
        }

        if (network.getTrafficType() == Networks.TrafficType.Management) {
            TungstenAnswer createVirtualMachineAnswer = createTungstenVM(network, nic, vm, dest);
            if (!createVirtualMachineAnswer.getResult()) {
                throw new CloudRuntimeException("can not create Tungsten-Fabric vm");
            }

            updateTungstenFabricService(nic);
        }

        return true;
    }

    @Override
    public boolean release(Network network, NicProfile nic, VirtualMachineProfile vm, ReservationContext context)
        throws ConcurrentOperationException {
        VMInstanceVO vmInstanceVO = vmInstanceDao.findById(vm.getId());
        HostVO host = hostDao.findById(vmInstanceVO.getLastHostId());

        if (host != null) {
            if (network.getTrafficType() == Networks.TrafficType.Public) {
                try {
                    TungstenCommand deleteTungstenVRouterPortCommand = new DeleteTungstenVRouterPortCommand(
                            host.getPublicIpAddress(), nic.getUuid());
                    tungstenFabricUtils.sendTungstenCommand(deleteTungstenVRouterPortCommand, network.getDataCenterId());
                    String nicName = TungstenUtils.getVmiName(network.getTrafficType().toString(), vm.getType().toString(),
                            vm.getInstanceName(), nic.getId());
                    String tungstenProjectFqn = tungstenService.getTungstenProjectFqn(network);
                    DeleteTungstenVmInterfaceCommand deleteVmiCmd = new DeleteTungstenVmInterfaceCommand(tungstenProjectFqn,
                            nicName);
                    tungstenFabricUtils.sendTungstenCommand(deleteVmiCmd, network.getDataCenterId());

                    TungstenCommand deleteVmCmd = new DeleteTungstenVmCommand(vm.getUuid());
                    tungstenFabricUtils.sendTungstenCommand(deleteVmCmd, network.getDataCenterId());

                    IPAddressVO ipAddressVO = ipAddressDao.findByIpAndDcId(network.getDataCenterId(), nic.getIPv4Address());
                    TungstenCommand deleteTungstenNetworkPolicyCommand = new DeleteTungstenNetworkPolicyCommand(
                            TungstenUtils.getPublicNetworkPolicyName(ipAddressVO.getId()), null, network.getUuid());
                    tungstenFabricUtils.sendTungstenCommand(deleteTungstenNetworkPolicyCommand, network.getDataCenterId());
                } catch (IllegalArgumentException e) {
                    throw new CloudRuntimeException(
                            "Failing to expunge the vm from Tungsten-Fabric with the uuid " + vm.getUuid());
                }
            }

            if (network.getTrafficType() == Networks.TrafficType.Management) {
                try {
                    TungstenCommand deleteTungstenVRouterPortCommand = new DeleteTungstenVRouterPortCommand(
                            host.getPublicIpAddress(), nic.getUuid());
                    tungstenFabricUtils.sendTungstenCommand(deleteTungstenVRouterPortCommand, network.getDataCenterId());
                    String nicName = TungstenUtils.getVmiName(network.getTrafficType().toString(), vm.getType().toString(),
                            vm.getInstanceName(), nic.getId());
                    String tungstenProjectFqn = tungstenService.getTungstenProjectFqn(network);
                    TungstenCommand deleteVmiCmd = new DeleteTungstenVmInterfaceCommand(tungstenProjectFqn, nicName);
                    tungstenFabricUtils.sendTungstenCommand(deleteVmiCmd, network.getDataCenterId());

                    TungstenCommand deleteVmCmd = new DeleteTungstenVmCommand(vm.getUuid());
                    tungstenFabricUtils.sendTungstenCommand(deleteVmCmd, network.getDataCenterId());
                } catch (IllegalArgumentException e) {
                    throw new CloudRuntimeException(
                            "Failing to expunge the vm from Tungsten-Fabric with the uuid " + vm.getUuid());
                }
            }
        }

        return true;
    }

    @Override
    public boolean shutdown(Network network, ReservationContext context, boolean cleanup)
        throws ConcurrentOperationException {
        return canHandle(network, Network.Service.Connectivity);
    }

    @Override
    public boolean destroy(Network network, ReservationContext context)
        throws ConcurrentOperationException, ResourceUnavailableException {
        boolean result = true;
        // delete floating ips
        List<IPAddressVO> staticNatIpList = ipAddressDao.listByAssociatedNetwork(network.getId(), false);
        for (IPAddressVO ipAddressVO : staticNatIpList) {
            TungstenCommand deleteTungstenFloatingIpCommand = new DeleteTungstenFloatingIpCommand(network.getUuid(),
                TungstenUtils.getFloatingIpPoolName(network.getDataCenterId()),
                TungstenUtils.getFloatingIpName(ipAddressVO.getId()));
            TungstenAnswer tungstenDeleteFIPAnswer = tungstenFabricUtils.sendTungstenCommand(
                deleteTungstenFloatingIpCommand, network.getDataCenterId());
            result = result && tungstenDeleteFIPAnswer.getResult();

            TungstenCommand deleteTungstenNetworkPolicyCommand = new DeleteTungstenNetworkPolicyCommand(
                TungstenUtils.getPublicNetworkPolicyName(ipAddressVO.getId()), null, network.getUuid());
            TungstenAnswer tungstenDeleteNPAnswer = tungstenFabricUtils.sendTungstenCommand(
                deleteTungstenNetworkPolicyCommand, network.getDataCenterId());
            result = result && tungstenDeleteNPAnswer.getResult();
        }

        return result;
    }

    @Override
    public boolean isReady(PhysicalNetworkServiceProvider provider) {
        PhysicalNetworkTrafficTypeVO managementNetwork = physicalNetworkTrafficTypeDao.findBy(
            provider.getPhysicalNetworkId(), Networks.TrafficType.Management);
        return managementNetwork != null;
    }

    @Override
    public boolean shutdownProviderInstances(PhysicalNetworkServiceProvider provider, ReservationContext context)
        throws ConcurrentOperationException {
        long zoneId = physicalNetworkDao.findById(provider.getPhysicalNetworkId()).getDataCenterId();

        TungstenProvider tungstenProvider = tungstenProviderDao.findByZoneId(zoneId);
        if (tungstenProvider != null) {
            DataCenter dataCenter = dataCenterDao.findById(zoneId);

            if (!dataCenter.isSecurityGroupEnabled()) {
                // delete network service map
                Network publicNetwork = networkModel.getSystemNetworkByZoneAndTrafficType(zoneId,
                    Networks.TrafficType.Public);
                if (publicNetwork != null) {
                    networkServiceMapDao.deleteByNetworkId(publicNetwork.getId());
                    List<VlanVO> listVlanVO = vlanDao.listVlansByNetworkIdIncludingRemoved(publicNetwork.getId());
                    for (VlanVO vlanVO : listVlanVO) {
                        tungstenService.removePublicNetworkSubnet(vlanVO);
                    }

                    tungstenService.deletePublicNetwork(zoneId);
                }
            }

            List<HostPodVO> listPod = podDao.listByDataCenterId(zoneId);
            for (HostPodVO pod : listPod) {
                tungstenService.removeManagementNetworkSubnet(pod);
            }

            tungstenService.deleteManagementNetwork(zoneId);
            tungstenProviderDao.expunge(tungstenProvider.getId());
        }
        return true;
    }

    @Override
    public boolean canEnableIndividualServices() {
        return true;
    }

    @Override
    public boolean verifyServicesCombination(final Set<Network.Service> services) {
        // tf can use configdrive for userdata
        services.remove(Network.Service.UserData);
        final Set<Network.Service> sharedZoneServices = new HashSet<>(
            Arrays.asList(Network.Service.Connectivity,
                Network.Service.Dhcp,
                Network.Service.Dns, Network.Service.SecurityGroup));
        final Set<Network.Service> isolatedZoneServices = new HashSet<>(
            Arrays.asList(Network.Service.Connectivity,
                Network.Service.Dhcp,
                Network.Service.Dns, Network.Service.SourceNat, Network.Service.StaticNat,
                Network.Service.Lb, Network.Service.PortForwarding, Network.Service.Firewall));
        return (services.containsAll(sharedZoneServices) && sharedZoneServices.containsAll(services)) || (
            services.containsAll(isolatedZoneServices) && isolatedZoneServices.containsAll(services));
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        agentMgr.registerForHostEvents(this, true, true, true);
        resourceMgr.registerResourceStateAdapter(this.getClass().getSimpleName(), this);
        VirtualMachine.State.getStateMachine().registerListener(this);

        return true;
    }

    @Override
    public HostVO createHostVOForConnectedAgent(HostVO host, StartupCommand[] cmd) {
        return null;
    }

    @Override
    public HostVO createHostVOForDirectConnectAgent(HostVO host, StartupCommand[] startup, ServerResource resource,
        Map<String, String> details, List<String> hostTags) {
        if (!(startup[0] instanceof StartupTungstenCommand)) {
            return null;
        }
        host.setType(Host.Type.L2Networking);
        return host;
    }

    @Override
    public DeleteHostAnswer deleteHost(HostVO host, boolean isForced, boolean isForceDeleteStorage) {
        if (host.getType() != Host.Type.L2Networking) {
            return null;
        }
        return new DeleteHostAnswer(true);
    }

    @Override
    public boolean processAnswers(final long agentId, final long seq, final Answer[] answers) {
        return false;
    }

    @Override
    public boolean processCommands(final long agentId, final long seq, final Command[] commands) {
        return false;
    }

    @Override
    public AgentControlAnswer processControlCommand(final long agentId, final AgentControlCommand cmd) {
        return null;
    }

    @Override
    public void processHostAdded(final long hostId) {
        // Do nothing
    }

    @Override
    public void processConnect(final Host host, final StartupCommand cmd, final boolean forRebalance) {
        long zoneId = host.getDataCenterId();
        TungstenProviderVO tungstenProvider = tungstenProviderDao.findByZoneId(zoneId);
        if (host.getHypervisorType() == Hypervisor.HypervisorType.KVM && tungstenProvider != null
            && host.getPublicIpAddress().equals(tungstenProvider.getGateway())) {
            setupVrouter(host);
        }

        if ((host.getType() == Host.Type.ConsoleProxy || host.getType() == Host.Type.SecondaryStorageVM) && tungstenProvider != null) {
            VirtualMachine.Type type = host.getType() == Host.Type.SecondaryStorageVM ? VirtualMachine.Type.SecondaryStorageVm : VirtualMachine.Type.ConsoleProxy;
            List<VMInstanceVO> vmList = vmInstanceDao.listByZoneIdAndType(zoneId, type);
            if (vmList.size() == 1 && vmList.get(0).getState() == VirtualMachine.State.Running) {
                NicVO nicVO = nicDao.getControlNicForVM(vmList.get(0).getId());
                HostVO kvmHost = hostDao.findById(vmList.get(0).getHostId());
                String srcNetwork = NetUtils.getCidrFromGatewayAndNetmask(kvmHost.getPrivateIpAddress(), kvmHost.getPrivateNetmask());
                Command setupTfRoute = new SetupTfRouteCommand(nicVO.getIPv4Address(), host.getPublicIpAddress(), srcNetwork);
                agentMgr.easySend(vmList.get(0).getHostId(), setupTfRoute);
            }
        }
    }

    private void setupVrouter(Host host) {
        long zoneId = host.getDataCenterId();
        DataCenterVO dataCenterVO = dataCenterDao.findById(zoneId);
        if (dataCenterVO.isSecurityGroupEnabled()) {
            List<NetworkVO> networks = networkDao.listByZoneSecurityGroup(zoneId);
            for (NetworkVO network : networks) {
                NetworkDetailVO networkDetail = networkDetailsDao.findDetail(network.getId(), "vrf");
                if (networkDetail != null) {
                    Command setupTungstenVRouterCommand = new SetupTungstenVRouterCommand(
                            "create", TungstenUtils.getSgVgwName(network.getId()), network.getCidr(),
                            NetUtils.ALL_IP4_CIDRS, networkDetail.getValue());
                    agentMgr.easySend(host.getId(), setupTungstenVRouterCommand);
                }
            }
        } else {
            List<VlanVO> vlanList = vlanDao.listByZone(zoneId);
            List<String> publicSubnetList = new ArrayList<>();

            for (VlanVO vlanVO : vlanList) {
                String subnet = NetUtils.getCidrFromGatewayAndNetmask(vlanVO.getVlanGateway(),
                        vlanVO.getVlanNetmask());
                publicSubnetList.add(subnet);
            }

            String publicSubnet = StringUtils.join(publicSubnetList.toArray(), " ");
            Network publicNetwork = networkModel.getSystemNetworkByZoneAndTrafficType(zoneId,
                    Networks.TrafficType.Public);
            NetworkDetailVO networkDetail = networkDetailsDao.findDetail(publicNetwork.getId(), "vrf");
            if (networkDetail != null) {
                Command setupTungstenVRouterCommand = new SetupTungstenVRouterCommand("create",
                        TungstenUtils.getVgwName(zoneId), publicSubnet, NetUtils.ALL_IP4_CIDRS,
                        networkDetail.getValue());
                agentMgr.easySend(host.getId(), setupTungstenVRouterCommand);
            }
        }
    }

    @Override
    public boolean processDisconnect(final long agentId, final Status state) {
        return true;
    }

    @Override
    public void processHostAboutToBeRemoved(final long hostId) {
        Host host = hostDao.findById(hostId);
        long zoneId = host.getDataCenterId();

        TungstenProviderVO tungstenProvider = tungstenProviderDao.findByZoneId(zoneId);
        if (host.getHypervisorType() == Hypervisor.HypervisorType.KVM && tungstenProvider != null
            && host.getPublicIpAddress().equals(tungstenProvider.getGateway())) {
            DataCenterVO dataCenterVO = dataCenterDao.findById(zoneId);
            if (dataCenterVO.isSecurityGroupEnabled()) {
                List<NetworkVO> networks = networkDao.listByZoneSecurityGroup(zoneId);
                for (NetworkVO network : networks) {
                    Command setupTungstenVRouterCommand = new SetupTungstenVRouterCommand("delete",
                        TungstenUtils.getSgVgwName(network.getId()), NetUtils.ALL_IP4_CIDRS, NetUtils.ALL_IP4_CIDRS,
                        NetUtils.ALL_IP4_CIDRS);
                    agentMgr.easySend(host.getId(), setupTungstenVRouterCommand);
                }
            } else {
                Command setupTungstenVRouterCommand = new SetupTungstenVRouterCommand("delete",
                    TungstenUtils.getVgwName(zoneId), NetUtils.ALL_IP4_CIDRS, NetUtils.ALL_IP4_CIDRS,
                    NetUtils.ALL_IP4_CIDRS);
                agentMgr.easySend(host.getId(), setupTungstenVRouterCommand);
            }
        }
    }

    @Override
    public void processHostRemoved(final long hostId, final long clusterId) {
        // Do no thing
    }

    @Override
    public boolean isRecurring() {
        return false;
    }

    @Override
    public int getTimeout() {
        return 0;
    }

    @Override
    public boolean processTimeout(final long agentId, final long seq) {
        return false;
    }

    @Override
    public boolean applyFWRules(final Network network, final List<? extends FirewallRule> rules)
        throws ResourceUnavailableException {
        String tungstenProjectFqn;
        String networkUuid;
        String policyName;
        Network publicNetwork = networkModel.getSystemNetworkByZoneAndTrafficType(network.getDataCenterId(),
            Networks.TrafficType.Public);
        for (FirewallRule firewallRule : rules) {
            if (firewallRule.getPurpose() == FirewallRule.Purpose.Firewall) {
                if (firewallRule.getDestinationCidrList() == null) {
                    return false;
                }

                if (firewallRule.getTrafficType() == FirewallRule.TrafficType.Egress) {
                    tungstenProjectFqn = tungstenService.getTungstenProjectFqn(network);
                    networkUuid = network.getUuid();
                    policyName = TungstenUtils.getRuleNetworkPolicyName(firewallRule.getId());
                } else {
                    tungstenProjectFqn = null;
                    networkUuid = publicNetwork.getUuid();
                    policyName = TungstenUtils.getPublicNetworkPolicyName(firewallRule.getSourceIpAddressId());
                }

                applyAddRule(network, firewallRule, tungstenProjectFqn, networkUuid,policyName);

                if (firewallRule.getState() == FirewallRule.State.Revoke) {
                    TungstenCommand deleteTungstenNetworkPolicyCommand = new DeleteTungstenNetworkPolicyCommand(
                        policyName, tungstenProjectFqn, networkUuid);
                    tungstenFabricUtils.sendTungstenCommand(
                        deleteTungstenNetworkPolicyCommand, network.getDataCenterId());
                }
            }
        }

        return true;
    }

    private void applyAddRule(Network network, FirewallRule firewallRule, String tungstenProjectFqn, String networkUuid,String policyName) throws ResourceUnavailableException {
        if (firewallRule.getState() == FirewallRule.State.Add) {
            TungstenRule tungstenRule = convertFirewallRule(firewallRule);
            List<TungstenRule> tungstenRuleList = new ArrayList<>();
            tungstenRuleList.add(tungstenRule);

            TungstenCommand createTungstenNetworkPolicyCommand = new CreateTungstenNetworkPolicyCommand(
                    policyName, tungstenProjectFqn, tungstenRuleList);
            TungstenAnswer createNetworkPolicyAnswer = tungstenFabricUtils.sendTungstenCommand(
                    createTungstenNetworkPolicyCommand, network.getDataCenterId());
            if (!createNetworkPolicyAnswer.getResult()) {
                throw new CloudRuntimeException("Can not create tungsten fabric network policy");
            }

            TungstenCommand applyTungstenNetworkPolicyCommand = new ApplyTungstenNetworkPolicyCommand(
                    tungstenProjectFqn, policyName, networkUuid, 0, 0);
            TungstenAnswer applyNetworkPolicyAnswer = tungstenFabricUtils.sendTungstenCommand(
                    applyTungstenNetworkPolicyCommand, network.getDataCenterId());
            if (!applyNetworkPolicyAnswer.getResult()) {
                throw new CloudRuntimeException("Can not apply tungsten fabric network policy");
            }
        }
    }

    private TungstenRule convertFirewallRule(FirewallRule firewallRule) throws ResourceUnavailableException {
        List<String> srcCidrs = firewallRule.getSourceCidrList();
        List<String> dstCidrs = firewallRule.getDestinationCidrList();

        if (firewallRule.getTrafficType() == FirewallRule.TrafficType.Egress && dstCidrs == null) {
            throw new ResourceUnavailableException("invalid destination cidr", FirewallRule.class,
                    firewallRule.getId());
        }

        String dstPrefix = dstCidrs.size() == 1 ? NetUtils.getCidr(dstCidrs.get(0)).first() : "0.0.0.0";
        int dstPrefixLen = dstCidrs.size() == 1 ? NetUtils.getCidr(dstCidrs.get(0)).second() : 0;
        int dstPortStart = firewallRule.getSourcePortStart() != null ? firewallRule.getSourcePortStart() : -1;
        int dstPortEnd = firewallRule.getSourcePortEnd() != null ? firewallRule.getSourcePortEnd() : -1;
        String tungstenProtocol =
            firewallRule.getProtocol().equals(NetUtils.ALL_PROTO) ? TungstenUtils.ANY : firewallRule.getProtocol();

        if (srcCidrs == null || srcCidrs.size() != 1) {
            throw new ResourceUnavailableException("invalid source cidr", FirewallRule.class, firewallRule.getId());
        }

        Pair<String, Integer> srcSubnet = NetUtils.getCidr(srcCidrs.get(0));

        if (firewallRule.getTrafficType() == FirewallRule.TrafficType.Ingress) {
            long id = firewallRule.getSourceIpAddressId();
            IPAddressVO ipAddressVO = ipAddressDao.findById(id);
            dstPrefix = ipAddressVO.getAddress().addr();
            dstPrefixLen = 32;
        }

        return new TungstenRule(firewallRule.getUuid(), TungstenUtils.PASS_ACTION, TungstenUtils.ONE_WAY_DIRECTION,
            tungstenProtocol, TungstenUtils.ANY, srcSubnet.first(), srcSubnet.second(), -1, -1, TungstenUtils.ANY,
            dstPrefix, dstPrefixLen, dstPortStart, dstPortEnd);
    }

    private TungstenRule getDefaultIngressRule(String ip, String protocol, int startPort, int endPort) {
        return new TungstenRule(TungstenUtils.PASS_ACTION, TungstenUtils.TWO_WAY_DIRECTION, protocol, TungstenUtils.ANY,
            TungstenUtils.ALL_IP4_PREFIX, 0, -1, -1, TungstenUtils.ANY, ip, TungstenUtils.MAX_CIDR, startPort, endPort);
    }

    private TungstenRule getDefaultEgressRule(String ip, String protocol, int startPort, int endPort) {
        return new TungstenRule(TungstenUtils.PASS_ACTION, TungstenUtils.ONE_WAY_DIRECTION, protocol, TungstenUtils.ANY,
            TungstenUtils.ALL_IP4_PREFIX, 0, startPort, endPort, TungstenUtils.ANY, ip, TungstenUtils.MAX_CIDR, -1, -1);
    }

    private List<TungstenRule> createDefaultTungstenFirewallRuleList(VirtualMachine.Type vmType, String ip) {
        List<TungstenRule> tungstenRuleList = new ArrayList<>();
        tungstenRuleList.add(getDefaultIngressRule(ip, NetUtils.ICMP_PROTO, -1, -1));
        tungstenRuleList.add(getDefaultEgressRule(ip, NetUtils.UDP_PROTO, TungstenUtils.DNS_SERVICE_PORT,
            TungstenUtils.DNS_SERVICE_PORT));
        tungstenRuleList.add(getDefaultEgressRule(ip, NetUtils.UDP_PROTO, TungstenUtils.NTP_SERVICE_PORT,
            TungstenUtils.NTP_SERVICE_PORT));
        if (vmType == VirtualMachine.Type.ConsoleProxy) {
            tungstenRuleList.add(getDefaultIngressRule(ip, NetUtils.TCP_PROTO, NetUtils.HTTP_PORT, NetUtils.HTTP_PORT));
            tungstenRuleList.add(getDefaultIngressRule(ip, NetUtils.TCP_PROTO, TungstenUtils.WEB_SERVICE_PORT,
                TungstenUtils.WEB_SERVICE_PORT));
            tungstenRuleList.add(
                getDefaultIngressRule(ip, NetUtils.TCP_PROTO, NetUtils.HTTPS_PORT, NetUtils.HTTPS_PORT));
        } else {
            tungstenRuleList.add(getDefaultEgressRule(ip, NetUtils.TCP_PROTO, NetUtils.HTTP_PORT, NetUtils.HTTP_PORT));
        }

        return tungstenRuleList;
    }

    @Override
    public boolean addDnsEntry(Network network, NicProfile nic, VirtualMachineProfile vm, DeployDestination dest,
        ReservationContext context)
        throws ConcurrentOperationException {
        return networkModel.isProviderSupportServiceInNetwork(network.getId(), Network.Service.Dhcp, getProvider());
    }

    @Override
    public boolean configDnsSupportForSubnet(Network network, NicProfile nic, VirtualMachineProfile vm,
        DeployDestination dest, ReservationContext context)
        throws ConcurrentOperationException {
        return networkModel.isProviderSupportServiceInNetwork(network.getId(), Network.Service.Dhcp, getProvider());
    }

    @Override
    public boolean removeDnsSupportForSubnet(Network network) {
        return networkModel.isProviderSupportServiceInNetwork(network.getId(), Network.Service.Dhcp, getProvider());
    }

    @Override
    public boolean preStateTransitionEvent(final VirtualMachine.State oldState, final VirtualMachine.Event event,
        final VirtualMachine.State newState, final VirtualMachine vo, final boolean status, final Object opaque) {
        return true;
    }

    @Override
    public boolean postStateTransitionEvent(
        final StateMachine2.Transition<VirtualMachine.State, VirtualMachine.Event> transition, final VirtualMachine vo,
        final boolean status, final Object opaque) {
        if (!status) {
            return false;
        }

        VirtualMachine.State oldState = transition.getCurrentState();
        VirtualMachine.State newState = transition.getToState();
        VirtualMachine.Event event = transition.getEvent();
        if (VirtualMachine.State.isVmStarted(oldState, event, newState)) {
            return tungstenService.addTungstenVmSecurityGroup((VMInstanceVO) vo);
        } else if (VirtualMachine.State.isVmStopped(oldState, event, newState)) {
            return tungstenService.removeTungstenVmSecurityGroup((VMInstanceVO) vo);
        }

        return true;
    }

    @Override
    public boolean prepareMigration(final NicProfile nic, final Network network, final VirtualMachineProfile vm,
        final DeployDestination dest, final ReservationContext context) {
        if (vm.getType().isUsedBySystem()) {
            TungstenAnswer answer = createTungstenVM(network, nic, vm, dest);
            return answer.getResult();
        } else {
            return true;
        }
    }

    @Override
    public void rollbackMigration(final NicProfile nic, final Network network, final VirtualMachineProfile vm,
        final ReservationContext src, final ReservationContext dst) {
        if (vm.getType().isUsedBySystem()) {
            Long hostId = vm.getVirtualMachine().getHostId();
            HostVO host = hostDao.findById(hostId);
            TungstenCommand deleteTungstenVRouterPortCommand = new DeleteTungstenVRouterPortCommand(
                host.getPublicIpAddress(), nic.getUuid());
            tungstenFabricUtils.sendTungstenCommand(deleteTungstenVRouterPortCommand, network.getDataCenterId());
        }
    }

    @Override
    public void commitMigration(final NicProfile nic, final Network network, final VirtualMachineProfile vm,
        final ReservationContext src, final ReservationContext dst) {
        if (vm.getType().isUsedBySystem()) {
            Long hostId = vm.getVirtualMachine().getLastHostId();
            HostVO host = hostDao.findById(hostId);
            TungstenCommand deleteTungstenVRouterPortCommand = new DeleteTungstenVRouterPortCommand(
                host.getPublicIpAddress(), nic.getUuid());
            tungstenFabricUtils.sendTungstenCommand(deleteTungstenVRouterPortCommand, network.getDataCenterId());
        }
    }

    private TungstenAnswer createTungstenVM(Network network, NicProfile nic, VirtualMachineProfile vm,
        DeployDestination dest) {
        String tungstenProjectFqn = tungstenService.getTungstenProjectFqn(network);
        VMInstanceVO vmInstanceVO = vmInstanceDao.findById(vm.getId());
        Host host = dest.getHost();
        if (host == null) {
            host = hostDao.findById(vmInstanceVO.getHostId());
        }

        TungstenCommand createTungstenVirtualMachineCommand = new CreateTungstenVirtualMachineCommand(
            tungstenProjectFqn, network.getUuid(), vm.getUuid(), vm.getInstanceName(), nic.getUuid(), nic.getId(),
            nic.getIPv4Address(), null, nic.getMacAddress(), vm.getType().toString(),
            network.getTrafficType().toString(), host.getPublicIpAddress(), network.getGateway(), nic.isDefaultNic());
        return tungstenFabricUtils.sendTungstenCommand(createTungstenVirtualMachineCommand, network.getDataCenterId());
    }

    private void updateTungstenFabricService(NicProfile nic) {
        if (nic.getIPv4Address() != null) {
            nic.setReservationStrategy(Nic.ReservationStrategy.Create);
        }

        nic.setBroadcastType(Networks.BroadcastDomainType.TUNGSTEN);
        nic.setBroadcastUri(Networks.BroadcastDomainType.TUNGSTEN.toUri("tf"));

        if (nic.getName() == null) {
            nic.setName(TungstenUtils.DEFAULT_VHOST_INTERFACE);
        }
    }
}
