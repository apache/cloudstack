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
import com.cloud.dc.HostPodVO;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.ConnectionException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.TungstenGuestNetworkIpAddressVO;
import com.cloud.network.TungstenProvider;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVMMapDao;
import com.cloud.network.dao.LoadBalancerVMMapVO;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.dao.NetworkDetailVO;
import com.cloud.network.dao.NetworkDetailsDao;
import com.cloud.network.dao.NetworkServiceMapDao;
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
import com.cloud.network.element.UserDataServiceProvider;
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
import com.cloud.resource.UnableDeleteHostException;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.uservm.UserVm;
import com.cloud.utils.EncryptionUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.TungstenUtils;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.Ip;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.VMInstanceDao;
import com.google.gson.Gson;
import org.apache.cloudstack.context.CallContext;
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
import org.apache.cloudstack.network.tungsten.agent.api.SetupTungstenVRouterCommand;
import org.apache.cloudstack.network.tungsten.agent.api.StartupTungstenCommand;
import org.apache.cloudstack.network.tungsten.agent.api.TungstenAnswer;
import org.apache.cloudstack.network.tungsten.agent.api.UpdateTungstenLoadBalancerListenerCommand;
import org.apache.cloudstack.network.tungsten.agent.api.UpdateTungstenLoadBalancerMemberCommand;
import org.apache.cloudstack.network.tungsten.agent.api.UpdateTungstenLoadBalancerPoolCommand;
import org.apache.cloudstack.network.tungsten.model.TungstenLoadBalancerMember;
import org.apache.cloudstack.network.tungsten.model.TungstenRule;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

@Component
public class TungstenElement extends AdapterBase
    implements StaticNatServiceProvider, UserDataServiceProvider, IpDeployer, FirewallServiceProvider,
    LoadBalancingServiceProvider, PortForwardingServiceProvider, ResourceStateAdapter, DnsServiceProvider, Listener {
    private static final Logger s_logger = Logger.getLogger(TungstenElement.class);
    private final Map<Network.Service, Map<Network.Capability, String>> _capabilities = InitCapabilities();
    @Inject
    HostPodDao _podDao;
    @Inject
    NetworkModel _networkModel;
    @Inject
    AccountManager _accountMgr;
    @Inject
    ResourceManager _resourceMgr;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    IpAddressManager _ipAddressMgr;
    @Inject
    PhysicalNetworkDao _physicalNetworkDao;
    @Inject
    VMInstanceDao _vmInstanceDao;
    @Inject
    AgentManager _agentMgr;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    VlanDao _vlanDao;
    @Inject
    HostDao _hostDao;
    @Inject
    NetworkServiceMapDao _networkServiceMapDao;
    @Inject
    NetworkDetailsDao _networkDetailsDao;
    @Inject
    LoadBalancerVMMapDao _lbVmMapDao;
    @Inject
    LoadBalancerDao _lbDao;
    @Inject
    TungstenService _tungstenService;
    @Inject
    MessageBus _messageBus;
    @Inject
    TungstenProviderDao _tungstenProviderDao;
    @Inject
    TungstenGuestNetworkIpAddressDao _tungstenGuestNetworkIpAddressDao;
    @Inject
    TungstenFabricUtils _tungstenFabricUtils;
    @Inject
    PhysicalNetworkTrafficTypeDao _physicalNetworkTrafficTypeDao;

    private static Map<Network.Service, Map<Network.Capability, String>> InitCapabilities() {
        Map<Network.Service, Map<Network.Capability, String>> capabilities = new HashMap<Network.Service,
            Map<Network.Capability, String>>();
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
        capabilities.put(Network.Service.UserData, null);
        Map<Network.Capability, String> dnsCapabilities = new HashMap<>();
        dnsCapabilities.put(Network.Capability.AllowDnsSuffixModification, "true");
        capabilities.put(Network.Service.Dns, dnsCapabilities);
        Map<Network.Capability, String> firewallCapabilities = new HashMap<Network.Capability, String>();
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
        List<LbStickinessMethod> methodList = new ArrayList<LbStickinessMethod>();
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

        if (!_networkModel.isProviderForNetwork(getProvider(), network.getId())) {
            s_logger.debug("TungstenElement is not a provider for network " + network.getDisplayText());
            return false;
        }

        return true;
    }

    @Override
    public boolean applyStaticNats(Network config, List<? extends StaticNat> rules)
        throws ResourceUnavailableException {
        StaticNat staticNat = rules.get(0);
        long sourceIpAddressId = staticNat.getSourceIpAddressId();
        IPAddressVO ipAddressVO = _ipAddressDao.findByIdIncludingRemoved(sourceIpAddressId);
        VMInstanceVO vm = _vmInstanceDao.findByIdIncludingRemoved(ipAddressVO.getAssociatedWithVmId());
        Nic nic = _networkModel.getNicInNetworkIncludingRemoved(vm.getId(), config.getId());
        Network publicNetwork = _networkModel.getSystemNetworkByZoneAndTrafficType(config.getDataCenterId(),
            Networks.TrafficType.Public);
        if (!staticNat.isForRevoke()) {
            AssignTungstenFloatingIpCommand assignTungstenFloatingIpCommand = new AssignTungstenFloatingIpCommand(
                publicNetwork.getUuid(), nic.getUuid(), TungstenUtils.getFloatingIpPoolName(config.getDataCenterId()),
                TungstenUtils.getFloatingIpName(ipAddressVO.getId()), nic.getIPv4Address());
            TungstenAnswer assignFloatingIpAnswer = _tungstenFabricUtils.sendTungstenCommand(
                assignTungstenFloatingIpCommand, config.getDataCenterId());
            return assignFloatingIpAnswer.getResult();
        } else {
            ReleaseTungstenFloatingIpCommand releaseTungstenFloatingIpCommand = new ReleaseTungstenFloatingIpCommand(
                publicNetwork.getUuid(), TungstenUtils.getFloatingIpPoolName(config.getDataCenterId()),
                TungstenUtils.getFloatingIpName(ipAddressVO.getId()));
            TungstenAnswer releaseFloatingIpAnswer = _tungstenFabricUtils.sendTungstenCommand(
                releaseTungstenFloatingIpCommand, config.getDataCenterId());
            return releaseFloatingIpAnswer.getResult();
        }
    }

    @Override
    public boolean applyLBRules(Network network, List<LoadBalancingRule> rules) throws ResourceUnavailableException {
        Network publicNetwork = _networkModel.getSystemNetworkByZoneAndTrafficType(network.getDataCenterId(),
            Networks.TrafficType.Public);
        for (LoadBalancingRule loadBalancingRule : rules) {
            String tungstenProjectFqn = _tungstenService.getTungstenProjectFqn(network);
            if (loadBalancingRule.getState() == FirewallRule.State.Add) {
                String protocol = StringUtils.upperCase(loadBalancingRule.getLbProtocol());
                if (loadBalancingRule.getSourcePortStart() == NetUtils.HTTP_PORT
                    || loadBalancingRule.getSourcePortStart() == NetUtils.HTTPS_PORT) {
                    protocol = "HTTP";
                }

                IPAddressVO ipAddressVO = _ipAddressDao.findByIpAndDcId(network.getDataCenterId(),
                    loadBalancingRule.getSourceIp().addr());
                List<LoadBalancerVMMapVO> loadBalancerVMMapVOList = _lbVmMapDao.listByLoadBalancerId(
                    loadBalancingRule.getId(), false);

                TungstenGuestNetworkIpAddressVO guestNetworkIpAddressVO =
                    _tungstenGuestNetworkIpAddressDao.findByNetworkIdAndPublicIp(
                    network.getId(), ipAddressVO.getAddress().addr());
                String lbIp;
                if (guestNetworkIpAddressVO == null) {
                    lbIp = _ipAddressMgr.acquireGuestIpAddress(network, null);
                } else {
                    lbIp = guestNetworkIpAddressVO.getGuestIpAddress().addr();
                }

                if (lbIp == null) {
                    return false;
                }

                List<TungstenLoadBalancerMember> memberIp = new ArrayList<>();

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

                CreateTungstenNetworkLoadbalancerCommand createTungstenNetworkLoadbalancerCommand =
                    new CreateTungstenNetworkLoadbalancerCommand(
                    tungstenProjectFqn, network.getUuid(), publicNetwork.getUuid(),
                    TungstenUtils.getLoadBalancerAlgorithm(loadBalancingRule.getAlgorithm()),
                    TungstenUtils.getLoadBalancerName(ipAddressVO.getId()),
                    TungstenUtils.getLoadBalancerListenerName(loadBalancingRule.getId()),
                    TungstenUtils.getLoadBalancerPoolName(loadBalancingRule.getId()),
                    TungstenUtils.getLoadBalancerHealthMonitorName(ipAddressVO.getId()),
                    TungstenUtils.getLoadBalancerVmiName(ipAddressVO.getId()),
                    TungstenUtils.getLoadBalancerIiName(ipAddressVO.getId()), loadBalancingRule.getId(), memberIp,
                    protocol, loadBalancingRule.getSourcePortStart().intValue(),
                    loadBalancingRule.getDefaultPortStart(), lbIp,
                    TungstenUtils.getFloatingIpPoolName(network.getDataCenterId()),
                    TungstenUtils.getFloatingIpName(ipAddressVO.getId()), "PING", 3, 5, 5, null, null, null);
                TungstenAnswer createTungstenNetworkLoadbalancerAnswer = _tungstenFabricUtils.sendTungstenCommand(
                    createTungstenNetworkLoadbalancerCommand, network.getDataCenterId());
                if (!createTungstenNetworkLoadbalancerAnswer.getResult()) {
                    return false;
                }

                // update stickiness, algorithm, protocol
                List<LoadBalancingRule.LbStickinessPolicy> lbStickinessPolicyList =
                    loadBalancingRule.getStickinessPolicies();
                String lbSessionPersistence = null;
                String lbPersistenceCookieName = null;
                if (lbStickinessPolicyList.size() > 0) {
                    lbSessionPersistence = TungstenUtils.getLoadBalancerSession(
                        lbStickinessPolicyList.get(0).getMethodName());
                    if (lbStickinessPolicyList.get(0).getMethodName().equals("AppCookie")) {
                        lbPersistenceCookieName = lbStickinessPolicyList.get(0).getParams().get(0).second();
                    }
                }

                UpdateTungstenLoadBalancerPoolCommand updateTungstenLoadBalancerPoolCommand;
                String lbStatsVisibility = _configDao.getValue(Config.NetworkLBHaproxyStatsVisbility.key());
                if (!lbStatsVisibility.equals("disabled")) {
                    String lbStatsUri = _configDao.getValue(Config.NetworkLBHaproxyStatsUri.key());
                    String lbStatsAuth = _configDao.getValue(Config.NetworkLBHaproxyStatsAuth.key());
                    String lbStatsPort = _configDao.getValue(Config.NetworkLBHaproxyStatsPort.key());
                    updateTungstenLoadBalancerPoolCommand = new UpdateTungstenLoadBalancerPoolCommand(
                        tungstenProjectFqn, TungstenUtils.getLoadBalancerPoolName(loadBalancingRule.getId()),
                        TungstenUtils.getLoadBalancerAlgorithm(loadBalancingRule.getAlgorithm()), lbSessionPersistence,
                        lbPersistenceCookieName, protocol, true, lbStatsPort, lbStatsUri, lbStatsAuth);
                } else {
                    updateTungstenLoadBalancerPoolCommand = new UpdateTungstenLoadBalancerPoolCommand(
                        tungstenProjectFqn, TungstenUtils.getLoadBalancerPoolName(loadBalancingRule.getId()),
                        TungstenUtils.getLoadBalancerAlgorithm(loadBalancingRule.getAlgorithm()), lbSessionPersistence,
                        lbPersistenceCookieName, protocol, false, null, null, null);
                }
                TungstenAnswer updateTungstenLoadBalancerPoolAnswer = _tungstenFabricUtils.sendTungstenCommand(
                    updateTungstenLoadBalancerPoolCommand, network.getDataCenterId());
                if (!updateTungstenLoadBalancerPoolAnswer.getResult()) {
                    return false;
                }

                UpdateTungstenLoadBalancerMemberCommand updateTungstenLoadBalancerMemberCommand =
                    new UpdateTungstenLoadBalancerMemberCommand(
                    tungstenProjectFqn, network.getUuid(),
                    TungstenUtils.getLoadBalancerPoolName(loadBalancingRule.getId()), memberIp);
                TungstenAnswer updateTungstenLoadBalancerMemberAnswer = _tungstenFabricUtils.sendTungstenCommand(
                    updateTungstenLoadBalancerMemberCommand, network.getDataCenterId());
                if (!updateTungstenLoadBalancerMemberAnswer.getResult()) {
                    return false;
                }

                LoadBalancingRule.LbSslCert lbSslCert = loadBalancingRule.getLbSslCert();
                if (lbSslCert != null) {
                    String httpsProtocol = "TERMINATED_HTTPS";
                    int listenerPort = NetUtils.HTTPS_PORT;

                    User callerUser = _accountMgr.getActiveUser(CallContext.current().getCallingUserId());
                    String apiKey = callerUser.getApiKey();
                    String secretKey = callerUser.getSecretKey();
                    if (apiKey != null && secretKey != null) {
                        String url;
                        try {
                            String data = "apiKey=" + URLEncoder.encode(apiKey, "UTF-8").replaceAll("\\+", "%20")
                                + "&command=getLoadBalancerSslCertificate" + "&id=" + URLEncoder.encode(
                                loadBalancingRule.getUuid(), "UTF-8").replaceAll("\\+", "%20") + "&response=json";
                            String signature = EncryptionUtil.generateSignature(data.toLowerCase(), secretKey);
                            url = data + "&signature=" + URLEncoder.encode(signature, "UTF-8").replaceAll("\\+", "%2B");
                        } catch (UnsupportedEncodingException e) {
                            return false;
                        }

                        UpdateTungstenLoadBalancerListenerCommand updateTungstenLoadBalancerListenerCommand =
                            new UpdateTungstenLoadBalancerListenerCommand(
                            tungstenProjectFqn, TungstenUtils.getLoadBalancerListenerName(loadBalancingRule.getId()),
                            httpsProtocol, listenerPort, url);
                        TungstenAnswer updateTungstenLoadBalancerListenerAnswer =
                            _tungstenFabricUtils.sendTungstenCommand(
                            updateTungstenLoadBalancerListenerCommand, network.getDataCenterId());
                        if (!updateTungstenLoadBalancerListenerAnswer.getResult()) {
                            return false;
                        }
                    } else {
                        s_logger.error("tungsten ssl require user api key");
                    }
                }

                // register tungsten guest network ip
                if (guestNetworkIpAddressVO == null) {
                    TungstenGuestNetworkIpAddressVO tungstenGuestNetworkIpAddressVO =
                        new TungstenGuestNetworkIpAddressVO();
                    tungstenGuestNetworkIpAddressVO.setNetworkId(network.getId());
                    tungstenGuestNetworkIpAddressVO.setPublicIpAddress(ipAddressVO.getAddress());
                    tungstenGuestNetworkIpAddressVO.setGuestIpAddress(new Ip(lbIp));
                    _tungstenGuestNetworkIpAddressDao.persist(tungstenGuestNetworkIpAddressVO);
                }

                return _tungstenService.updateLoadBalancer(network, loadBalancingRule);
            }

            if (loadBalancingRule.getState() == FirewallRule.State.Revoke) {
                IPAddressVO ipAddressVO = _ipAddressDao.findByIpAndDcId(network.getDataCenterId(),
                    loadBalancingRule.getSourceIp().addr());
                List<LoadBalancerVO> listLoadBalancerVO = _lbDao.listByIpAddress(ipAddressVO.getId());
                if (listLoadBalancerVO.size() > 1) {
                    DeleteTungstenLoadBalancerListenerCommand deleteTungstenLoadBalancerListenerCommand =
                        new DeleteTungstenLoadBalancerListenerCommand(
                        tungstenProjectFqn, TungstenUtils.getLoadBalancerListenerName(loadBalancingRule.getId()));
                    TungstenAnswer deleteTungstenLoadBalancerListenerAnswer = _tungstenFabricUtils.sendTungstenCommand(
                        deleteTungstenLoadBalancerListenerCommand, network.getDataCenterId());
                    if (!deleteTungstenLoadBalancerListenerAnswer.getResult()) {
                        return false;
                    }

                    return _tungstenService.updateLoadBalancer(network, loadBalancingRule);
                } else {
                    DeleteTungstenLoadBalancerCommand deleteTungstenLoadBalancerCommand =
                        new DeleteTungstenLoadBalancerCommand(
                        tungstenProjectFqn, publicNetwork.getUuid(),
                        TungstenUtils.getLoadBalancerName(ipAddressVO.getId()),
                        TungstenUtils.getLoadBalancerHealthMonitorName(ipAddressVO.getId()),
                        TungstenUtils.getLoadBalancerVmiName(ipAddressVO.getId()),
                        TungstenUtils.getFloatingIpPoolName(network.getDataCenterId()),
                        TungstenUtils.getFloatingIpName(ipAddressVO.getId()));
                    TungstenAnswer deleteTungstenLoadBalancerAnswer = _tungstenFabricUtils.sendTungstenCommand(
                        deleteTungstenLoadBalancerCommand, network.getDataCenterId());
                    if (!deleteTungstenLoadBalancerAnswer.getResult()) {
                        return false;
                    }

                    TungstenGuestNetworkIpAddressVO guestNetworkIpAddressVO =
                        _tungstenGuestNetworkIpAddressDao.findByNetworkIdAndPublicIp(
                        network.getId(), ipAddressVO.getAddress().addr());
                    _tungstenGuestNetworkIpAddressDao.remove(guestNetworkIpAddressVO.getId());
                }
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
        return null;
    }

    @Override
    public boolean handlesOnlyRulesInTransitionState() {
        return false;
    }

    @Override
    public boolean applyPFRules(final Network network, final List<PortForwardingRule> rules)
        throws ResourceUnavailableException {
        Network publicNetwork = _networkModel.getSystemNetworkByZoneAndTrafficType(network.getDataCenterId(),
            Networks.TrafficType.Public);
        // tungsten port forwarding don't support multiple vm per public ip address
        // if multiple vm, only 1 vm get effect
        // frontend : don't allow to add multiple vm per public ip address
        for (PortForwardingRule rule : rules) {
            IPAddressVO publicIp = ApiDBUtils.findIpAddressById(rule.getSourceIpAddressId());
            UserVm vm = ApiDBUtils.findUserVmById(rule.getVirtualMachineId());
            Nic guestNic = _networkModel.getNicInNetwork(vm.getId(), network.getId());
            if (rule.getState() == FirewallRule.State.Add || rule.getState() == FirewallRule.State.Revoke) {
                ApplyTungstenPortForwardingCommand applyTungstenPortForwardingCommand =
                    new ApplyTungstenPortForwardingCommand(
                    rule.getState() == FirewallRule.State.Add, publicNetwork.getUuid(),
                    TungstenUtils.getFloatingIpPoolName(network.getDataCenterId()),
                    TungstenUtils.getFloatingIpName(publicIp.getId()), guestNic.getUuid(),
                    StringUtils.upperCase(rule.getProtocol()), rule.getSourcePortStart(),
                    rule.getDestinationPortStart());
                TungstenAnswer applyTungstenPortForwardingAnswer = _tungstenFabricUtils.sendTungstenCommand(
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
        return _capabilities;
    }

    @Override
    public boolean applyIps(Network network, List<? extends PublicIpAddress> ipAddress, Set<Network.Service> services)
        throws ResourceUnavailableException {
        return true;
    }

    @Override
    public Network.Provider getProvider() {
        return Network.Provider.Tungsten;
    }

    @Override
    public boolean implement(Network network, NetworkOffering offering, DeployDestination dest,
        ReservationContext context)
        throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        return true;
    }

    @Override
    public boolean prepare(Network network, NicProfile nic, VirtualMachineProfile vm, DeployDestination dest,
        ReservationContext context)
        throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        if (network.getTrafficType() == Networks.TrafficType.Public) {
            String tungstenProjectFqn = _tungstenService.getTungstenProjectFqn(network);
            String vmType = vm.getType() == VirtualMachine.Type.ConsoleProxy ? TungstenUtils.getProxyVm() :
                TungstenUtils.getSecstoreVm();
            VMInstanceVO vmInstanceVO = _vmInstanceDao.findById(vm.getId());
            HostVO host = _hostDao.findById(vmInstanceVO.getHostId());

            CreateTungstenVirtualMachineCommand createTungstenVirtualMachineCommand =
                new CreateTungstenVirtualMachineCommand(
                tungstenProjectFqn, network.getUuid(), vm.getUuid(), vm.getInstanceName(), nic.getUuid(), nic.getId(),
                nic.getIPv4Address(), nic.getMacAddress(), vmType, TungstenUtils.getPublicType(),
                host.getPublicIpAddress());
            TungstenAnswer createVirtualMachineAnswer = _tungstenFabricUtils.sendTungstenCommand(
                createTungstenVirtualMachineCommand, network.getDataCenterId());
            if (!createVirtualMachineAnswer.getResult()) {
                throw new CloudRuntimeException("can not create tungsten vm");
            }

            // get tungsten public firewall rule
            IPAddressVO ipAddressVO = _ipAddressDao.findByIpAndDcId(network.getDataCenterId(), nic.getIPv4Address());
            List<TungstenRule> tungstenRuleList = createDefaultTungstenFirewallRuleList(vm.getType(),
                nic.getIPv4Address());

            // create tungsten public network policy for system vm
            CreateTungstenNetworkPolicyCommand createTungstenNetworkPolicyCommand =
                new CreateTungstenNetworkPolicyCommand(
                TungstenUtils.getPublicNetworkPolicyName(ipAddressVO.getId()), null, tungstenRuleList);
            TungstenAnswer createTungstenNetworkPolicyAnswer = _tungstenFabricUtils.sendTungstenCommand(
                createTungstenNetworkPolicyCommand, network.getDataCenterId());
            if (!createTungstenNetworkPolicyAnswer.getResult()) {
                throw new CloudRuntimeException("can not create tungsten public network policy");
            }

            _messageBus.publish(_name, TungstenService.MESSAGE_APPLY_NETWORK_POLICY_EVENT, PublishScope.LOCAL, network);

            nic.setBroadcastType(Networks.BroadcastDomainType.Tungsten);
            nic.setBroadcastUri(Networks.BroadcastDomainType.Tungsten.toUri("tf"));
            nic.setName(nic.getName() + TungstenUtils.getBridgeName());
        }

        if (network.getTrafficType() == Networks.TrafficType.Management) {
            String tungstenProjectFqn = _tungstenService.getTungstenProjectFqn(network);
            String vmType = vm.getType() == VirtualMachine.Type.ConsoleProxy ? TungstenUtils.getProxyVm() :
                TungstenUtils.getSecstoreVm();
            VMInstanceVO vmInstanceVO = _vmInstanceDao.findById(vm.getId());
            HostVO host = _hostDao.findById(vmInstanceVO.getHostId());

            CreateTungstenVirtualMachineCommand createTungstenVirtualMachineCommand =
                new CreateTungstenVirtualMachineCommand(
                tungstenProjectFqn, network.getUuid(), vm.getUuid(), vm.getInstanceName(), nic.getUuid(), nic.getId(),
                nic.getIPv4Address(), nic.getMacAddress(), vmType, TungstenUtils.getManagementType(),
                host.getPublicIpAddress());
            TungstenAnswer createVirtualMachineAnswer = _tungstenFabricUtils.sendTungstenCommand(
                createTungstenVirtualMachineCommand, network.getDataCenterId());
            if (!createVirtualMachineAnswer.getResult()) {
                throw new CloudRuntimeException("can not create tungsten vm");
            }

            nic.setBroadcastType(Networks.BroadcastDomainType.Tungsten);
            nic.setBroadcastUri(Networks.BroadcastDomainType.Tungsten.toUri("tf"));
            nic.setName(nic.getName() + TungstenUtils.getBridgeName());
        }

        return true;
    }

    @Override
    public boolean release(Network network, NicProfile nic, VirtualMachineProfile vm, ReservationContext context)
        throws ConcurrentOperationException, ResourceUnavailableException {
        VMInstanceVO vmInstanceVO = _vmInstanceDao.findById(vm.getId());
        HostVO host = _hostDao.findById(vmInstanceVO.getLastHostId());

        if (host == null) {
            return true;
        }

        if (network.getTrafficType() == Networks.TrafficType.Public) {
            try {
                DeleteTungstenVRouterPortCommand deleteTungstenVRouterPortCommand =
                    new DeleteTungstenVRouterPortCommand(
                    host.getPublicIpAddress(), nic.getUuid());
                _tungstenFabricUtils.sendTungstenCommand(deleteTungstenVRouterPortCommand, network.getDataCenterId());

                String vmType = vm.getType() == VirtualMachine.Type.ConsoleProxy ? TungstenUtils.getProxyVm() :
                    TungstenUtils.getSecstoreVm();
                String nicName = TungstenUtils.getVmiName(TungstenUtils.getPublicType(), vmType, vm.getInstanceName(),
                    nic.getId());
                String tungstenProjectFqn = _tungstenService.getTungstenProjectFqn(network);
                DeleteTungstenVmInterfaceCommand deleteVmiCmd = new DeleteTungstenVmInterfaceCommand(tungstenProjectFqn,
                    nicName);
                _tungstenFabricUtils.sendTungstenCommand(deleteVmiCmd, network.getDataCenterId());

                DeleteTungstenVmCommand deleteVmCmd = new DeleteTungstenVmCommand(vm.getUuid());
                _tungstenFabricUtils.sendTungstenCommand(deleteVmCmd, network.getDataCenterId());

                IPAddressVO ipAddressVO = _ipAddressDao.findByIpAndDcId(network.getDataCenterId(),
                    nic.getIPv4Address());
                DeleteTungstenNetworkPolicyCommand deleteTungstenNetworkPolicyCommand =
                    new DeleteTungstenNetworkPolicyCommand(
                    TungstenUtils.getPublicNetworkPolicyName(ipAddressVO.getId()), null, network.getUuid());
                _tungstenFabricUtils.sendTungstenCommand(deleteTungstenNetworkPolicyCommand, network.getDataCenterId());
            } catch (IllegalArgumentException e) {
                throw new CloudRuntimeException(
                    "Failing to expunge the vm from tungsten with the uuid " + vm.getUuid());
            }
        }

        if (network.getTrafficType() == Networks.TrafficType.Management) {
            try {
                DeleteTungstenVRouterPortCommand deleteTungstenVRouterPortCommand =
                    new DeleteTungstenVRouterPortCommand(
                    host.getPublicIpAddress(), nic.getUuid());
                _tungstenFabricUtils.sendTungstenCommand(deleteTungstenVRouterPortCommand, network.getDataCenterId());

                String vmType = vm.getType() == VirtualMachine.Type.ConsoleProxy ? TungstenUtils.getProxyVm() :
                    TungstenUtils.getSecstoreVm();
                String nicName = TungstenUtils.getVmiName(TungstenUtils.getManagementType(), vmType,
                    vm.getInstanceName(), nic.getId());
                String tungstenProjectFqn = _tungstenService.getTungstenProjectFqn(network);
                DeleteTungstenVmInterfaceCommand deleteVmiCmd = new DeleteTungstenVmInterfaceCommand(tungstenProjectFqn,
                    nicName);
                _tungstenFabricUtils.sendTungstenCommand(deleteVmiCmd, network.getDataCenterId());

                DeleteTungstenVmCommand deleteVmCmd = new DeleteTungstenVmCommand(vm.getUuid());
                _tungstenFabricUtils.sendTungstenCommand(deleteVmCmd, network.getDataCenterId());
            } catch (IllegalArgumentException e) {
                throw new CloudRuntimeException(
                    "Failing to expunge the vm from tungsten with the uuid " + vm.getUuid());
            }
        }

        return true;
    }

    @Override
    public boolean shutdown(Network network, ReservationContext context, boolean cleanup)
        throws ConcurrentOperationException, ResourceUnavailableException {
        return canHandle(network, Network.Service.Connectivity);
    }

    @Override
    public boolean destroy(Network network, ReservationContext context)
        throws ConcurrentOperationException, ResourceUnavailableException {
        boolean result = true;
        // delete floating ips
        List<IPAddressVO> staticNatIpList = _ipAddressDao.listByAssociatedNetwork(network.getId(), false);
        for (IPAddressVO ipAddressVO : staticNatIpList) {
            DeleteTungstenFloatingIpCommand deleteTungstenFloatingIpCommand = new DeleteTungstenFloatingIpCommand(
                network.getUuid(), TungstenUtils.getFloatingIpPoolName(network.getDataCenterId()),
                TungstenUtils.getFloatingIpName(ipAddressVO.getId()));
            TungstenAnswer tungstenDeleteFIPAnswer = _tungstenFabricUtils.sendTungstenCommand(
                deleteTungstenFloatingIpCommand, network.getDataCenterId());
            result = result && tungstenDeleteFIPAnswer.getResult();

            DeleteTungstenNetworkPolicyCommand deleteTungstenNetworkPolicyCommand =
                new DeleteTungstenNetworkPolicyCommand(
                TungstenUtils.getPublicNetworkPolicyName(ipAddressVO.getId()), null, network.getUuid());
            TungstenAnswer tungstenDeleteNPAnswer = _tungstenFabricUtils.sendTungstenCommand(
                deleteTungstenNetworkPolicyCommand, network.getDataCenterId());
            result = result && tungstenDeleteNPAnswer.getResult();
        }

        return result;
    }

    @Override
    public boolean isReady(PhysicalNetworkServiceProvider provider) {
        PhysicalNetworkTrafficTypeVO publicNetwork = _physicalNetworkTrafficTypeDao.findBy(
            provider.getPhysicalNetworkId(), Networks.TrafficType.Public);
        PhysicalNetworkTrafficTypeVO managementNetwork = _physicalNetworkTrafficTypeDao.findBy(
                provider.getPhysicalNetworkId(), Networks.TrafficType.Management);
        return publicNetwork != null || managementNetwork != null;
    }

    @Override
    public boolean shutdownProviderInstances(PhysicalNetworkServiceProvider provider, ReservationContext context)
        throws ConcurrentOperationException, ResourceUnavailableException {
        long zoneId = _physicalNetworkDao.findById(provider.getPhysicalNetworkId()).getDataCenterId();
        Network publicNetwork = _networkModel.getSystemNetworkByZoneAndTrafficType(zoneId, Networks.TrafficType.Public);

        // delete network service map
        _networkServiceMapDao.deleteByNetworkId(publicNetwork.getId());

        TungstenProvider tungstenProvider = _tungstenProviderDao.findByZoneId(zoneId);
        if (tungstenProvider != null) {
            List<VlanVO> listVlanVO = _vlanDao.listByZone(zoneId);
            for (VlanVO vlanVO : listVlanVO) {
                _tungstenService.removePublicNetworkSubnet(vlanVO);
            }

            _tungstenService.deletePublicNetwork(zoneId);

            List<HostPodVO> listPod = _podDao.listByDataCenterId(zoneId);
            for (HostPodVO pod : listPod) {
                _tungstenService.removeManagementNetworkSubnet(pod);
            }

            _tungstenService.deleteManagementNetwork(zoneId);
        }
        return true;
    }

    @Override
    public boolean canEnableIndividualServices() {
        return false;
    }

    @Override
    public boolean verifyServicesCombination(Set<Network.Service> services) {
        return true;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        _agentMgr.registerForHostEvents(this, true, true, true);
        _resourceMgr.registerResourceStateAdapter(this.getClass().getSimpleName(), this);
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
    public DeleteHostAnswer deleteHost(HostVO host, boolean isForced, boolean isForceDeleteStorage)
        throws UnableDeleteHostException {
        if (!(host.getType() == Host.Type.L2Networking)) {
            return null;
        }
        return new DeleteHostAnswer(true);
    }

    @Override
    public boolean addPasswordAndUserdata(final Network network, final NicProfile nic, final VirtualMachineProfile vm,
        final DeployDestination dest, final ReservationContext context)
        throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean savePassword(final Network network, final NicProfile nic, final VirtualMachineProfile vm)
        throws ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean saveUserData(final Network network, final NicProfile nic, final VirtualMachineProfile vm)
        throws ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean saveSSHKey(final Network network, final NicProfile nic, final VirtualMachineProfile vm,
        final String sshPublicKey) throws ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean saveHypervisorHostname(final NicProfile profile, final Network network,
        final VirtualMachineProfile vm, final DeployDestination dest) throws ResourceUnavailableException {
        return true;
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
    }

    @Override
    public void processConnect(final Host host, final StartupCommand cmd, final boolean forRebalance)
        throws ConnectionException {
        long zoneId = host.getDataCenterId();
        TungstenProviderVO tungstenProvider = _tungstenProviderDao.findByZoneId(zoneId);
        if (host.getHypervisorType() == Hypervisor.HypervisorType.KVM && tungstenProvider != null
            && host.getPublicIpAddress().equals(tungstenProvider.getGateway())) {
            List<VlanVO> vlanList = _vlanDao.listByZone(zoneId);
            List<String> publicSubnetList = new ArrayList<>();

            for (VlanVO vlanVO : vlanList) {
                String subnet = NetUtils.getCidrFromGatewayAndNetmask(vlanVO.getVlanGateway(), vlanVO.getVlanNetmask());
                publicSubnetList.add(subnet);
            }

            String publicSubnet = StringUtils.join(publicSubnetList.toArray(), " ");
            Network publicNetwork = _networkModel.getSystemNetworkByZoneAndTrafficType(zoneId,
                Networks.TrafficType.Public);
            NetworkDetailVO networkDetail = _networkDetailsDao.findDetail(publicNetwork.getId(), "vrf");
            SetupTungstenVRouterCommand setupTungstenVRouterCommand = new SetupTungstenVRouterCommand("create",
                TungstenUtils.getVgwName(zoneId), publicSubnet, NetUtils.ALL_IP4_CIDRS, networkDetail.getValue());
            _agentMgr.easySend(host.getId(), setupTungstenVRouterCommand);
        }
    }

    @Override
    public boolean processDisconnect(final long agentId, final Status state) {
        return true;
    }

    @Override
    public void processHostAboutToBeRemoved(final long hostId) {
        Host host = _hostDao.findById(hostId);
        long zoneId = host.getDataCenterId();

        TungstenProviderVO tungstenProvider = _tungstenProviderDao.findByZoneId(zoneId);
        if (host.getHypervisorType() == Hypervisor.HypervisorType.KVM && tungstenProvider != null
            && host.getPublicIpAddress().equals(tungstenProvider.getGateway())) {
            SetupTungstenVRouterCommand setupTungstenVRouterCommand = new SetupTungstenVRouterCommand("delete",
                TungstenUtils.getVgwName(zoneId), NetUtils.ALL_IP4_CIDRS, NetUtils.ALL_IP4_CIDRS,
                NetUtils.ALL_IP4_CIDRS);
            _agentMgr.easySend(host.getId(), setupTungstenVRouterCommand);
        }
    }

    @Override
    public void processHostRemoved(final long hostId, final long clusterId) {
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
        boolean result = true;
        String tungstenProjectFqn;
        String networkUuid;
        Network publicNetwork = _networkModel.getSystemNetworkByZoneAndTrafficType(network.getDataCenterId(),
            Networks.TrafficType.Public);
        for (FirewallRule firewallRule : rules) {
            if (firewallRule.getPurpose() == FirewallRule.Purpose.Firewall) {
                if (firewallRule.getDestinationCidrList() == null) {
                    return false;
                }

                TungstenRule tungstenRule = convertFirewallRule(firewallRule);
                List<TungstenRule> tungstenRuleList = new ArrayList<>();
                tungstenRuleList.add(tungstenRule);

                if (firewallRule.getTrafficType() == FirewallRule.TrafficType.Egress) {
                    tungstenProjectFqn = _tungstenService.getTungstenProjectFqn(network);
                    networkUuid = network.getUuid();
                } else {
                    tungstenProjectFqn = null;
                    networkUuid = publicNetwork.getUuid();
                }

                if (firewallRule.getState() == FirewallRule.State.Add) {
                    CreateTungstenNetworkPolicyCommand createTungstenNetworkPolicyCommand =
                        new CreateTungstenNetworkPolicyCommand(
                        TungstenUtils.getRuleNetworkPolicyName(firewallRule.getId()), tungstenProjectFqn,
                        tungstenRuleList);
                    TungstenAnswer createNetworkPolicyAnswer = _tungstenFabricUtils.sendTungstenCommand(
                        createTungstenNetworkPolicyCommand, network.getDataCenterId());
                    result = result && createNetworkPolicyAnswer.getResult();

                    ApplyTungstenNetworkPolicyCommand applyTungstenNetworkPolicyCommand =
                        new ApplyTungstenNetworkPolicyCommand(
                        tungstenProjectFqn, TungstenUtils.getRuleNetworkPolicyName(firewallRule.getId()), networkUuid,
                        true);
                    TungstenAnswer applyNetworkPolicyAnswer = _tungstenFabricUtils.sendTungstenCommand(
                        applyTungstenNetworkPolicyCommand, network.getDataCenterId());
                    result = result && applyNetworkPolicyAnswer.getResult();

                    return result;
                }

                if (firewallRule.getState() == FirewallRule.State.Revoke) {
                    DeleteTungstenNetworkPolicyCommand deleteTungstenNetworkPolicyCommand =
                        new DeleteTungstenNetworkPolicyCommand(
                        TungstenUtils.getRuleNetworkPolicyName(firewallRule.getId()), tungstenProjectFqn, networkUuid);
                    TungstenAnswer deleteNetworkPolicyAnswer = _tungstenFabricUtils.sendTungstenCommand(
                        deleteTungstenNetworkPolicyCommand, network.getDataCenterId());

                    return deleteNetworkPolicyAnswer.getResult();
                }
            }
        }

        return true;
    }

    private TungstenRule convertFirewallRule(FirewallRule firewallRule) throws ResourceUnavailableException {
        List<String> srcCidrs = firewallRule.getSourceCidrList();
        List<String> dstCidrs = firewallRule.getDestinationCidrList();
        String dstPrefix = dstCidrs.size() == 1 ? NetUtils.getCidr(dstCidrs.get(0)).first() : "0.0.0.0";
        int dstPrefixLen = dstCidrs.size() == 1 ? NetUtils.getCidr(dstCidrs.get(0)).second() : 0;
        int dstPortStart = firewallRule.getSourcePortStart() != null ? firewallRule.getSourcePortStart() : -1;
        int dstPortEnd = firewallRule.getSourcePortEnd() != null ? firewallRule.getSourcePortEnd() : -1;
        String tungstenProtocol = firewallRule.getProtocol().equals(NetUtils.ALL_PROTO) ? TungstenUtils.ANY_PROTO :
            firewallRule.getProtocol();

        if (srcCidrs == null || srcCidrs.size() != 1) {
            throw new ResourceUnavailableException("invalid source cidr", FirewallRule.class, firewallRule.getId());
        }

        Pair<String, Integer> srcSubnet = NetUtils.getCidr(srcCidrs.get(0));

        if (firewallRule.getTrafficType() == FirewallRule.TrafficType.Egress && dstCidrs == null) {
            throw new ResourceUnavailableException("invalid destination cidr", FirewallRule.class,
                firewallRule.getId());
        }

        if (firewallRule.getTrafficType() == FirewallRule.TrafficType.Ingress) {
            long id = firewallRule.getSourceIpAddressId();
            IPAddressVO ipAddressVO = _ipAddressDao.findById(id);
            dstPrefix = ipAddressVO.getAddress().addr();
            dstPrefixLen = 32;
        }

        return new TungstenRule(firewallRule.getUuid(), TungstenUtils.PASS_ACTION, TungstenUtils.ONE_WAY_DIRECTION,
            tungstenProtocol, srcSubnet.first(), srcSubnet.second(), -1, -1, dstPrefix, dstPrefixLen, dstPortStart,
            dstPortEnd);
    }

    private TungstenRule getDefaultRule(String ip, String protocol, int startPort, int endPort) {
        return new TungstenRule(null, TungstenUtils.PASS_ACTION, TungstenUtils.ONE_WAY_DIRECTION, protocol,
            TungstenUtils.ALL_IP4_PREFIX, 0, -1, -1, ip, TungstenUtils.MAX_CIDR, startPort, endPort);
    }

    private List<TungstenRule> createDefaultTungstenFirewallRuleList(VirtualMachine.Type vmType, String ip) {
        List<TungstenRule> tungstenRuleList = new ArrayList<>();
        tungstenRuleList.add(getDefaultRule(ip, NetUtils.ICMP_PROTO, -1, -1));
        if (vmType == VirtualMachine.Type.ConsoleProxy) {
            tungstenRuleList.add(getDefaultRule(ip, NetUtils.TCP_PROTO, NetUtils.HTTP_PORT, NetUtils.HTTP_PORT));
            tungstenRuleList.add(
                getDefaultRule(ip, NetUtils.TCP_PROTO, TungstenUtils.WEB_SERVICE_PORT, TungstenUtils.WEB_SERVICE_PORT));
            tungstenRuleList.add(getDefaultRule(ip, NetUtils.TCP_PROTO, NetUtils.HTTPS_PORT, NetUtils.HTTPS_PORT));
        }

        return tungstenRuleList;
    }

    @Override
    public boolean addDnsEntry(Network network, NicProfile nic, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        if (_networkModel.isProviderSupportServiceInNetwork(network.getId(), Network.Service.Dhcp, getProvider())) {
            return true;
        }
        return false;
    }

    @Override
    public boolean configDnsSupportForSubnet(Network network, NicProfile nic, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        if (_networkModel.isProviderSupportServiceInNetwork(network.getId(), Network.Service.Dhcp, getProvider())) {
            return true;
        }
        return false;
    }

    @Override
    public boolean removeDnsSupportForSubnet(Network network) throws ResourceUnavailableException {
        if (_networkModel.isProviderSupportServiceInNetwork(network.getId(), Network.Service.Dhcp, getProvider())) {
            return true;
        }
        return false;
    }
}
