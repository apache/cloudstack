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
package org.apache.cloudstack.service;

import com.amazonaws.util.CollectionUtils;
import com.cloud.agent.AgentManager;
import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.to.LoadBalancerTO;
import com.cloud.api.ApiDBUtils;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.ConnectionException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.network.IpAddress;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.SDNProviderNetworkRule;
import com.cloud.network.Site2SiteCustomerGateway;
import com.cloud.network.Site2SiteVpnConnection;
import com.cloud.network.Site2SiteVpnGateway;
import com.cloud.network.VirtualRouterProvider;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.LoadBalancerVMMapDao;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVMMapVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.dao.Site2SiteCustomerGatewayDao;
import com.cloud.network.dao.Site2SiteCustomerGatewayVO;
import com.cloud.network.dao.Site2SiteVpnGatewayDao;
import com.cloud.network.dao.Site2SiteVpnGatewayVO;
import com.cloud.network.dao.VirtualRouterProviderDao;
import com.cloud.network.element.DhcpServiceProvider;
import com.cloud.network.element.DnsServiceProvider;
import com.cloud.network.element.FirewallServiceProvider;
import com.cloud.network.element.IpDeployer;
import com.cloud.network.element.LoadBalancingServiceProvider;
import com.cloud.network.element.NetworkACLServiceProvider;
import com.cloud.network.element.PortForwardingServiceProvider;
import com.cloud.network.element.Site2SiteVpnServiceProvider;
import com.cloud.network.element.StaticNatServiceProvider;
import com.cloud.network.element.VirtualRouterElement;
import com.cloud.network.element.VirtualRouterProviderVO;
import com.cloud.network.element.VpcProvider;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.nsx.NsxVpnGatewayResult;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.LoadBalancerContainer;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.network.rules.StaticNat;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.network.vpc.PrivateGateway;
import com.cloud.network.vpc.StaticRouteProfile;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.VpcService;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.dao.VpcOfferingServiceMapDao;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceStateAdapter;
import com.cloud.resource.ServerResource;
import com.cloud.resource.UnableDeleteHostException;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.db.QueryBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.VMInstanceDao;
import net.sf.ehcache.config.InvalidConfigurationException;
import org.apache.cloudstack.NsxAnswer;
import org.apache.cloudstack.StartupNsxCommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.admin.internallb.ConfigureInternalLoadBalancerElementCmd;
import org.apache.cloudstack.api.command.admin.internallb.CreateInternalLoadBalancerElementCmd;
import org.apache.cloudstack.api.command.admin.internallb.ListInternalLoadBalancerElementsCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.network.element.InternalLoadBalancerElementService;
import org.apache.cloudstack.resource.NsxLoadBalancerMember;
import org.apache.cloudstack.resource.NsxNetworkRule;
import com.cloud.network.SDNProviderOpObject;
import org.apache.cloudstack.utils.NsxHelper;
import org.apache.cloudstack.utils.NsxVpnCryptoUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.cloudstack.resourcedetail.FirewallRuleDetailVO;
import org.apache.cloudstack.resourcedetail.UserIpAddressDetailVO;
import org.apache.cloudstack.resourcedetail.dao.FirewallRuleDetailsDao;
import org.apache.cloudstack.resourcedetail.dao.UserIpAddressDetailsDao;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.LongFunction;
import java.util.stream.Collectors;

@Component
public class NsxElement extends AdapterBase implements  DhcpServiceProvider, DnsServiceProvider, VpcProvider,
        StaticNatServiceProvider, IpDeployer, PortForwardingServiceProvider, NetworkACLServiceProvider,
        LoadBalancingServiceProvider, FirewallServiceProvider, Site2SiteVpnServiceProvider,
        InternalLoadBalancerElementService, ResourceStateAdapter, Listener {

    protected static final String NSX_VPN_GATEWAY_IP_DETAIL = "nsxVpnGatewayIp";

    @Inject
    AccountManager accountMgr;
    @Inject
    NsxServiceImpl nsxService;
    @Inject
    DataCenterDao dataCenterDao;
    @Inject
    NetworkDao networkDao;
    @Inject
    AgentManager agentManager;
    @Inject
    ResourceManager resourceManager;
    @Inject
    PhysicalNetworkDao physicalNetworkDao;
    @Inject
    NetworkModel networkModel;
    @Inject
    DomainDao domainDao;
    @Inject
    protected VpcOfferingServiceMapDao vpcOfferingServiceMapDao;
    @Inject
    IPAddressDao ipAddressDao;
    @Inject
    VMInstanceDao vmInstanceDao;
    @Inject
    VpcDao vpcDao;
    @Inject
    LoadBalancerVMMapDao lbVmMapDao;
    @Inject
    LoadBalancerDao loadBalancerDao;
    @Inject
    VirtualRouterProviderDao vrProviderDao;
    @Inject
    PhysicalNetworkServiceProviderDao pNtwkSvcProviderDao;
    @Inject
    FirewallRuleDetailsDao firewallRuleDetailsDao;
    @Inject
    IpAddressManager ipAddressManager;
    @Inject
    VpcService vpcService;
    @Inject
    VpcManager vpcManager;
    @Inject
    Site2SiteVpnGatewayDao vpnGatewayDao;
    @Inject
    Site2SiteCustomerGatewayDao customerGatewayDao;
    @Inject
    UserIpAddressDetailsDao userIpAddressDetailsDao;
    @Inject
    FirewallRulesDao firewallRulesDao;
    @Inject
    PortForwardingRulesDao portForwardingRulesDao;

    protected Logger logger = LogManager.getLogger(getClass());

    private final Map<Network.Service, Map<Network.Capability, String>> capabilities = initCapabilities();


    private static Map<Network.Service, Map<Network.Capability, String>> initCapabilities() {
        Map<Network.Service, Map<Network.Capability, String>> capabilities = new HashMap<>();

        Map<Network.Capability, String> dhcpCapabilities = Map.of(Network.Capability.DhcpAccrossMultipleSubnets, "true");
        capabilities.put(Network.Service.Dhcp, dhcpCapabilities);

        Map<Network.Capability, String> dnsCapabilities = new HashMap<>();
        dnsCapabilities.put(Network.Capability.AllowDnsSuffixModification, "true");
        capabilities.put(Network.Service.Dns, dnsCapabilities);

        capabilities.put(Network.Service.StaticNat, null);

        // Set capabilities for LB service
        Map<Network.Capability, String> lbCapabilities = new HashMap<Network.Capability, String>();
        lbCapabilities.put(Network.Capability.SupportedLBAlgorithms, "roundrobin,leastconn");
        lbCapabilities.put(Network.Capability.SupportedLBIsolation, "dedicated");
        lbCapabilities.put(Network.Capability.SupportedProtocols, "tcp, udp");
        lbCapabilities.put(Network.Capability.SupportedStickinessMethods, VirtualRouterElement.getHAProxyStickinessCapability());
        lbCapabilities.put(Network.Capability.LbSchemes, String.join(",", LoadBalancerContainer.Scheme.Internal.name(), LoadBalancerContainer.Scheme.Public.name()));

        capabilities.put(Network.Service.Lb, lbCapabilities);
        capabilities.put(Network.Service.PortForwarding, null);
        capabilities.put(Network.Service.NetworkACL, null);

        Map<Network.Capability, String> firewallCapabilities = new HashMap<>();
        firewallCapabilities.put(Network.Capability.SupportedProtocols, "tcp,udp,icmp");
        firewallCapabilities.put(Network.Capability.SupportedEgressProtocols, "tcp,udp,icmp,all");
        firewallCapabilities.put(Network.Capability.MultipleIps, "true");
        firewallCapabilities.put(Network.Capability.TrafficStatistics, "per public ip");
        firewallCapabilities.put(Network.Capability.SupportedTrafficDirection, "ingress, egress");
        capabilities.put(Network.Service.Firewall, firewallCapabilities);

        Map<Network.Capability, String> sourceNatCapabilities = new HashMap<>();
        sourceNatCapabilities.put(Network.Capability.RedundantRouter, "true");
        sourceNatCapabilities.put(Network.Capability.SupportedSourceNatTypes, "peraccount");
        capabilities.put(Network.Service.SourceNat, sourceNatCapabilities);

        Map<Network.Capability, String> vpnCapabilities = new HashMap<>();
        vpnCapabilities.put(Network.Capability.SupportedVpnProtocols, "ipsec");
        vpnCapabilities.put(Network.Capability.VpnTypes, "s2svpn");
        capabilities.put(Network.Service.Vpn, vpnCapabilities);
        return capabilities;
    }
    @Override
    public boolean addDhcpEntry(Network network, NicProfile nic, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean configDhcpSupportForSubnet(Network network, NicProfile nic, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean removeDhcpSupportForSubnet(Network network) throws ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean setExtraDhcpOptions(Network network, long nicId, Map<Integer, String> dhcpOptions) {
        return true;
    }

    @Override
    public boolean removeDhcpEntry(Network network, NicProfile nic, VirtualMachineProfile vmProfile) throws ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean addDnsEntry(Network network, NicProfile nic, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean configDnsSupportForSubnet(Network network, NicProfile nic, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean removeDnsSupportForSubnet(Network network) throws ResourceUnavailableException {
        return true;
    }

    @Override
    public Map<Network.Service, Map<Network.Capability, String>> getCapabilities() {
        return capabilities;
    }

    @Override
    public boolean applyIps(Network network, List<? extends PublicIpAddress> ipAddress, Set<Network.Service> services) throws ResourceUnavailableException {
        return true;
    }

    @Override
    public Network.Provider getProvider() {
        return Network.Provider.Nsx;
    }

    @Override
    public boolean implement(Network network, NetworkOffering offering, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        // TODO: Check if the network is NSX based (was already implemented as part of the guru.setup()
        return true;
    }

    @Override
    public boolean prepare(Network network, NicProfile nic, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        return false;
    }

    @Override
    public boolean release(Network network, NicProfile nic, VirtualMachineProfile vm, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
        return false;
    }

    @Override
    public boolean shutdown(Network network, ReservationContext context, boolean cleanup) throws ConcurrentOperationException, ResourceUnavailableException {
        return canHandle(network, Network.Service.Connectivity);
    }

    @Override
    public boolean destroy(Network network, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
        Account account = accountMgr.getAccount(network.getAccountId());
        NetworkVO networkVO = networkDao.findById(network.getId());
        DataCenterVO zone = dataCenterDao.findById(network.getDataCenterId());
        DomainVO domain = domainDao.findById(account.getDomainId());
        if (Objects.isNull(zone)) {
            String msg = String.format("Cannot find zone with ID %s", network.getDataCenterId());
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
        return nsxService.deleteNetwork(zone.getId(), account.getId(), domain.getId(), networkVO);
    }

    @Override
    public boolean isReady(PhysicalNetworkServiceProvider provider) {
        return true;
    }

    @Override
    public boolean shutdownProviderInstances(PhysicalNetworkServiceProvider provider, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
        return false;
    }

    @Override
    public boolean canEnableIndividualServices() {
        return true;
    }

    @Override
    public boolean verifyServicesCombination(Set<Network.Service> services) {
        return true;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        agentManager.registerForHostEvents(this, true, true, true);
        resourceManager.registerResourceStateAdapter(this.getClass().getSimpleName(), this);
        return true;
    }

    @Override
    public boolean start() {
        return false;
    }

    @Override
    public boolean stop() {
        return false;
    }

    @Override
    public HostVO createHostVOForConnectedAgent(HostVO host, StartupCommand[] cmd) {
        return null;
    }

    @Override
    public HostVO createHostVOForDirectConnectAgent(HostVO host, StartupCommand[] startup, ServerResource resource, Map<String, String> details, List<String> hostTags) {
        if (!(startup[0] instanceof StartupNsxCommand)) {
            return null;
        }
        host.setType(Host.Type.L2Networking);
        return host;
    }

    @Override
    public DeleteHostAnswer deleteHost(HostVO host, boolean isForced, boolean isForceDeleteStorage) throws UnableDeleteHostException {
        return null;
    }

    private DomainVO getDomainFromAccount(Account account) {
        DomainVO domain = domainDao.findById(account.getDomainId());
        if (Objects.isNull(domain)) {
            String msg = String.format("Unable to find domain with id: %s", account.getDomainId());
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
        return domain;
    }

    @Override
    public boolean implementVpc(Vpc vpc, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        DataCenterVO zone = zoneFunction.apply(vpc.getZoneId());
        Pair<Boolean, Account> isNsxAndAccount = validateVpcConfigurationAndGetAccount(zone, vpc);
        if (Boolean.FALSE.equals(isNsxAndAccount.first())) {
            return true;
        }
        if (Boolean.TRUE.equals(isNsxAndAccount.first()) && Objects.isNull(isNsxAndAccount.second())) {
            throw new InvalidParameterValueException(String.format("Failed to find account with id %s", vpc.getAccountId()));
        }
        return true;
    }

    @Override
    public boolean shutdownVpc(Vpc vpc, ReservationContext context) throws ConcurrentOperationException {
        DataCenterVO zone = zoneFunction.apply(vpc.getZoneId());
        Pair<Boolean, Account> isNsxAndAccount = validateVpcConfigurationAndGetAccount(zone, vpc);
        if (Boolean.FALSE.equals(isNsxAndAccount.first())) {
            return true;
        }
        if (Boolean.TRUE.equals(isNsxAndAccount.first()) && Objects.isNull(isNsxAndAccount.second())) {
            throw new InvalidParameterValueException(String.format("Failed to find account with id %s", vpc.getAccountId()));
        }
        Account account = isNsxAndAccount.second();
        DomainVO domain = getDomainFromAccount(account);
        return nsxService.deleteVpcNetwork(vpc.getZoneId(), account.getId(), domain.getId(), vpc.getId(), vpc.getName());
    }

    private Pair<Boolean, Account> validateVpcConfigurationAndGetAccount(DataCenterVO zone, Vpc vpc) {
        if (Objects.isNull(zone)) {
            throw new InvalidParameterValueException(String.format("Failed to find zone with id %s", vpc.getZoneId()));
        }
        Account account = null;
        boolean forNsx = false;
        List<PhysicalNetworkVO> physicalNetworks = physicalNetworkDao.listByZoneAndTrafficType(zone.getId(), Networks.TrafficType.Guest);
        if (CollectionUtils.isNullOrEmpty(physicalNetworks)) {
            String err = String.format("Desired physical network is not present in the zone %s for traffic type %s. ", zone, Networks.TrafficType.Guest.name());
            logger.error(err);
            throw new InvalidConfigurationException(err);
        }
        List<PhysicalNetworkVO> filteredPhysicalNetworks = physicalNetworks.stream().filter(x -> x.getIsolationMethods().contains("NSX")).collect(Collectors.toList());
        if (CollectionUtils.isNullOrEmpty(filteredPhysicalNetworks)) {
            String err = String.format("No physical network with NSX isolation type for traffic type %s is present in the zone %s.", Networks.TrafficType.Guest.name(), zone.getName());
            logger.error(err);
            throw new InvalidConfigurationException(err);
        }
        if (filteredPhysicalNetworks.get(0).getIsolationMethods().contains("NSX")) {
            account = accountMgr.getAccount(vpc.getAccountId());
            forNsx = true;
        }
        return new Pair<>(forNsx, account);
    }

    @Override
    public boolean createPrivateGateway(PrivateGateway gateway) throws ConcurrentOperationException, ResourceUnavailableException {
        return false;
    }

    @Override
    public boolean deletePrivateGateway(PrivateGateway privateGateway) throws ConcurrentOperationException, ResourceUnavailableException {
        return false;
    }

    @Override
    public boolean applyStaticRoutes(Vpc vpc, List<StaticRouteProfile> routes) throws ResourceUnavailableException {
        return false;
    }

    @Override
    public boolean applyACLItemsToPrivateGw(PrivateGateway gateway, List<? extends NetworkACLItem> rules) throws ResourceUnavailableException {
        return false;
    }

    @Override
    public boolean processAnswers(long agentId, long seq, Answer[] answers) {
        return false;
    }

    @Override
    public boolean processCommands(long agentId, long seq, Command[] commands) {
        return false;
    }

    @Override
    public AgentControlAnswer processControlCommand(long agentId, AgentControlCommand cmd) {
        return null;
    }

    @Override
    public void processHostAdded(long hostId) {
        // Do nothing
    }

    @Override
    public void processConnect(Host host, StartupCommand cmd, boolean forRebalance) throws ConnectionException {
        // Do nothing
    }

    @Override
    public boolean processDisconnect(long agentId, Status state) {
        return false;
    }

    @Override
    public void processHostAboutToBeRemoved(long hostId) {
        // Do nothing
    }

    @Override
    public void processHostRemoved(long hostId, long clusterId) {
        // Do nothing
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
    public boolean processTimeout(long agentId, long seq) {
        return false;
    }

    protected boolean canHandle(Network network, Network.Service service) {
        logger.debug("Checking if Nsx Element can handle service {} on network {}", service.getName(), network);

        if (!networkModel.isProviderForNetwork(getProvider(), network.getId())) {
            logger.debug("Nsx Element is not a provider for network {}", network);
            return false;
        }

        return true;
    }

    private final LongFunction<DataCenterVO> zoneFunction = zoneId -> dataCenterDao.findById(zoneId);

    @Override
    public IpDeployer getIpDeployer(Network network) {
        return this;
    }

    @Override
    public boolean applyStaticNats(Network config, List<? extends StaticNat> rules) throws ResourceUnavailableException {
        for(StaticNat staticNat : rules) {
            long sourceIpAddressId = staticNat.getSourceIpAddressId();
            IPAddressVO ipAddressVO = ipAddressDao.findByIdIncludingRemoved(sourceIpAddressId);
            VMInstanceVO vm = vmInstanceDao.findByIdIncludingRemoved(ipAddressVO.getAssociatedWithVmId());
            // floating ip is released when nic was deleted
            if (vm == null || networkModel.getNicInNetworkIncludingRemoved(vm.getId(), config.getId()) == null) {
                continue;
            }
            Pair<VpcVO, NetworkVO> vpcOrNetwork = getVpcOrNetwork(config.getVpcId(), config.getId());
            VpcVO vpc = vpcOrNetwork.first();
            NetworkVO network = vpcOrNetwork.second();
            Long networkResourceId = Objects.nonNull(vpc) ? vpc.getId() : network.getId();
            String networkResourceName = Objects.nonNull(vpc) ? vpc.getName() : network.getName();
            boolean isVpcResource = Objects.nonNull(vpc);
            if (!staticNat.isForRevoke()) {
                return nsxService.createStaticNatRule(config.getDataCenterId(), config.getDomainId(), config.getAccountId(),
                        networkResourceId, networkResourceName, isVpcResource, vm.getId(),
                        ipAddressVO.getAddress().addr(), staticNat.getDestIpAddress());
            } else {
                return nsxService.deleteStaticNatRule(config.getDataCenterId(), config.getDomainId(), config.getAccountId(),
                        networkResourceId, networkResourceName, isVpcResource);
            }
        }
        return false;
    }

    protected synchronized boolean applyPFRulesInternal(Network network, List<PortForwardingRule> rules) {
        return Transaction.execute((TransactionCallback<Boolean>) status -> {
            boolean result = true;
            for (PortForwardingRule rule : rules) {
                IPAddressVO publicIp = ApiDBUtils.findIpAddressById(rule.getSourceIpAddressId());
                UserVm vm = ApiDBUtils.findUserVmById(rule.getVirtualMachineId());
                if (vm == null && rule.getState() != FirewallRule.State.Revoke) {
                    continue;
                }
                SDNProviderOpObject nsxObject = getNsxOpObject(network);
                String publicPort = PortForwardingServiceProvider.getPublicPortRange(rule);

                String privatePort = PortForwardingServiceProvider.getPrivatePFPortRange(rule);

                SDNProviderNetworkRule networkRule = new SDNProviderNetworkRule.Builder()
                        .setDomainId(nsxObject.getDomainId())
                        .setAccountId(nsxObject.getAccountId())
                        .setZoneId(nsxObject.getZoneId())
                        .setNetworkResourceId(nsxObject.getNetworkResourceId())
                        .setNetworkResourceName(nsxObject.getNetworkResourceName())
                        .setVpcResource(nsxObject.isVpcResource())
                        .setVmId(Objects.nonNull(vm) ? vm.getId() : 0)
                        .setVmIp(Objects.nonNull(vm) ? vm.getPrivateIpAddress() : null)
                        .setPublicIp(publicIp.getAddress().addr())
                        .setPrivatePort(privatePort)
                        .setPublicPort(publicPort)
                        .setRuleId(rule.getId())
                        .setProtocol(rule.getProtocol().toUpperCase(Locale.ROOT))
                        .build();

                NsxNetworkRule nsxNetworkRule = new NsxNetworkRule();
                nsxNetworkRule.setBaseRule(networkRule);

                FirewallRuleDetailVO ruleDetail = firewallRuleDetailsDao.findDetail(rule.getId(), ApiConstants.FOR_NSX);
                if (Arrays.asList(FirewallRule.State.Add, FirewallRule.State.Active).contains(rule.getState())) {
                    if ((ruleDetail == null && FirewallRule.State.Add == rule.getState()) || (ruleDetail != null && !ruleDetail.getValue().equalsIgnoreCase("true"))) {
                        logger.debug("Creating port forwarding rule on NSX for VM {} to ports {} - {}",
                                vm, rule.getDestinationPortStart(), rule.getDestinationPortEnd());
                        NsxAnswer answer = nsxService.createPortForwardRule(nsxNetworkRule);
                        boolean pfRuleResult = answer.getResult();
                        if (pfRuleResult && !answer.isObjectExistent()) {
                            logger.debug("Port forwarding rule {} created on NSX, adding detail on firewall rules details", rule);
                            if (ruleDetail == null && FirewallRule.State.Add == rule.getState()) {
                                logger.debug("Adding new firewall detail for rule {}", rule);
                                firewallRuleDetailsDao.addDetail(rule.getId(), ApiConstants.FOR_NSX, "true", false);
                            } else {
                                logger.debug("Updating firewall detail for rule {}", rule);
                                ruleDetail.setValue("true");
                                firewallRuleDetailsDao.update(ruleDetail.getId(), ruleDetail);
                            }
                        }
                        result &= pfRuleResult;
                    }
                } else if (rule.getState() == FirewallRule.State.Revoke) {
                    if (ruleDetail == null || (ruleDetail != null && ruleDetail.getValue().equalsIgnoreCase("true"))) {
                        boolean pfRuleResult = nsxService.deletePortForwardRule(nsxNetworkRule);
                        if (pfRuleResult && ruleDetail != null) {
                            logger.debug("Updating firewall rule detail {} () for rule {}, set to false", ruleDetail.getId(), ruleDetail.getName(), rule);
                            ruleDetail.setValue("false");
                            firewallRuleDetailsDao.update(ruleDetail.getId(), ruleDetail);
                        }
                        result &= pfRuleResult;
                    }
                }
            }
            return result;
        });
    }

    @Override
    public boolean applyPFRules(Network network, List<PortForwardingRule> rules) throws ResourceUnavailableException {
        if (!canHandle(network, Network.Service.PortForwarding)) {
            return false;
        }
        return applyPFRulesInternal(network, rules);
    }

    public Pair<VpcVO, NetworkVO> getVpcOrNetwork(Long vpcId, long networkId) {
        VpcVO vpc = null;
        NetworkVO network = null;
        if (Objects.nonNull(vpcId)) {
            vpc = vpcDao.findById(vpcId);
            if (Objects.isNull(vpc)) {
                throw new CloudRuntimeException(String.format("Failed to find VPC with id: %s", vpcId));
            }
        } else {
            network = networkDao.findById(networkId);
            if (Objects.isNull(network)) {
                throw new CloudRuntimeException(String.format("Failed to find network with id: %s", networkId));
            }
        }
        return new Pair<>(vpc, network);
    }

    private long getResourceId(String resource, VpcVO vpc, NetworkVO network) {
        switch (resource) {
            case "domain":
                return Objects.nonNull(vpc) ? vpc.getDomainId() : network.getDomainId();
            case "account":
                return Objects.nonNull(vpc) ? vpc.getAccountId() : network.getAccountId();
            case "zone":
                return Objects.nonNull(vpc) ? vpc.getZoneId() : network.getDataCenterId();
            default:
                return 0;
        }
    }

    private SDNProviderOpObject getNsxOpObject(Network network) {
        Pair<VpcVO, NetworkVO> vpcOrNetwork = getVpcOrNetwork(network.getVpcId(), network.getId());
        VpcVO vpc = vpcOrNetwork.first();
        NetworkVO networkVO = vpcOrNetwork.second();
        long domainId = getResourceId("domain", vpc, networkVO);
        long accountId = getResourceId("account", vpc, networkVO);
        long zoneId = getResourceId("zone", vpc, networkVO);

        return new SDNProviderOpObject.Builder()
                .vpcVO(vpc)
                .networkVO(networkVO)
                .domainId(domainId)
                .accountId(accountId)
                .zoneId(zoneId)
                .build();
    }

    @Override
    public boolean applyLBRules(Network network, List<LoadBalancingRule> rules) throws ResourceUnavailableException {
        boolean result = true;
        for (LoadBalancingRule loadBalancingRule : rules) {
            IPAddressVO publicIp = ipAddressDao.findByIpAndDcId(network.getDataCenterId(),
                    loadBalancingRule.getSourceIp().addr());
            SDNProviderOpObject nsxObject = getNsxOpObject(network);

            List<NsxLoadBalancerMember> lbMembers = getLoadBalancerMembers(loadBalancingRule);
            SDNProviderNetworkRule baseNetRule = new SDNProviderNetworkRule.Builder()
            .setDomainId(nsxObject.getDomainId())
            .setAccountId(nsxObject.getAccountId())
            .setZoneId(nsxObject.getZoneId())
            .setNetworkResourceId(nsxObject.getNetworkResourceId())
            .setNetworkResourceName(nsxObject.getNetworkResourceName())
            .setVpcResource(nsxObject.isVpcResource())
            .setPublicIp(LoadBalancerContainer.Scheme.Public == loadBalancingRule.getScheme() ?
                            publicIp.getAddress().addr() : loadBalancingRule.getSourceIp().addr())
            .setPublicPort(String.valueOf(loadBalancingRule.getSourcePortStart()))
            .setPrivatePort(String.valueOf(loadBalancingRule.getDefaultPortStart()))
            .setRuleId(loadBalancingRule.getId())
            .setProtocol(loadBalancingRule.getLbProtocol().toUpperCase(Locale.ROOT))
            .setAlgorithm(loadBalancingRule.getAlgorithm())
                    .build();
            NsxNetworkRule networkRule = new NsxNetworkRule();
            networkRule.setBaseRule(baseNetRule);
            networkRule.setMemberList(lbMembers);
            if (Arrays.asList(FirewallRule.State.Add, FirewallRule.State.Active).contains(loadBalancingRule.getState())) {
                result &= nsxService.createLbRule(networkRule);
            } else if (loadBalancingRule.getState() == FirewallRule.State.Revoke) {
                result &= nsxService.deleteLbRule(networkRule);
            }
        }
        return result;
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

    private List<NsxLoadBalancerMember> getLoadBalancerMembers(LoadBalancingRule lbRule) {
        List<LoadBalancerVMMapVO> lbVms = lbVmMapDao.listByLoadBalancerId(lbRule.getId(), false);
        List<NsxLoadBalancerMember> lbMembers = new ArrayList<>();

        for (LoadBalancerVMMapVO lbVm : lbVms) {
            NsxLoadBalancerMember member = new NsxLoadBalancerMember(lbVm.getInstanceId(), lbVm.getInstanceIp(), lbRule.getDefaultPortStart());
            lbMembers.add(member);
        }
        return lbMembers;
    }

    @Override
    public boolean applyNetworkACLs(Network network, List<? extends NetworkACLItem> rules) throws ResourceUnavailableException {
        if (!canHandle(network, Network.Service.NetworkACL)) {
            return false;
        }

        List<NsxNetworkRule> nsxDelNetworkRules = new ArrayList<>();
        boolean success = true;
        for (NetworkACLItem rule : rules) {
            String privatePort = PortForwardingServiceProvider.getPrivatePortRangeForACLRule(rule);
            NsxNetworkRule networkRule = getNsxNetworkRuleForAcl(rule, privatePort);
            if (Arrays.asList(NetworkACLItem.State.Active, NetworkACLItem.State.Add).contains(rule.getState())) {
                success = success && nsxService.addFirewallRules(network, List.of(networkRule));
            } else if (NetworkACLItem.State.Revoke == rule.getState()) {
                nsxDelNetworkRules.add(networkRule);
            }
        }

        if (!nsxDelNetworkRules.isEmpty()) {
            success = nsxService.deleteFirewallRules(network, nsxDelNetworkRules);
            if (!success) {
                logger.warn("Not all firewall rules were successfully deleted");
            }
        }
        return success;
    }

    @Override
    public boolean reorderAclRules(Vpc vpc, List<? extends Network> networks, List<? extends NetworkACLItem> networkACLItems) {
        List<NsxNetworkRule> aclRulesList = new ArrayList<>();
        for (NetworkACLItem rule : networkACLItems) {
            String privatePort = PortForwardingServiceProvider.getPrivatePortRangeForACLRule(rule);
            aclRulesList.add(getNsxNetworkRuleForAcl(rule, privatePort));
        }
        for (Network network: networks) {
            nsxService.deleteFirewallRules(network, aclRulesList);
        }
        boolean success = true;
        for (Network network : networks) {
            for (NsxNetworkRule aclRule : aclRulesList) {
                success = success && nsxService.addFirewallRules(network, List.of(aclRule));
            }
        }
        return success;
    }

    private NsxNetworkRule getNsxNetworkRuleForAcl(NetworkACLItem rule, String privatePort) {
        NsxNetworkRule nsxNetworkRule = new NsxNetworkRule();
        SDNProviderNetworkRule networkRule = new SDNProviderNetworkRule.Builder()
        .setRuleId(rule.getId())
        .setSourceCidrList(Objects.nonNull(rule.getSourceCidrList()) ? transformCidrListValues(rule.getSourceCidrList()) : List.of("ANY"))
        .setTrafficType(rule.getTrafficType().toString())
        .setProtocol(rule.getProtocol().toUpperCase())
        .setPublicPort(String.valueOf(rule.getSourcePortStart()))
        .setPrivatePort(privatePort)
        .setIcmpCode(rule.getIcmpCode())
        .setIcmpType(rule.getIcmpType())
        .setService(Network.Service.NetworkACL).build();
        nsxNetworkRule.setBaseRule(networkRule);
        nsxNetworkRule.setAclAction(transformActionValue(rule.getAction()));
        return nsxNetworkRule;
    }
        @Override
    public boolean applyFWRules(Network network, List<? extends FirewallRule> rules) throws ResourceUnavailableException {

        if (!canHandle(network, Network.Service.Firewall)) {
            return false;
        }
        List<NsxNetworkRule> nsxAddNetworkRules = new ArrayList<>();
        List<NsxNetworkRule> nsxDelNetworkRules = new ArrayList<>();
        for (FirewallRule rule : rules) {
            NsxNetworkRule networkRule = new NsxNetworkRule();
            SDNProviderNetworkRule baseNetRule = new SDNProviderNetworkRule.Builder()
            .setRuleId(rule.getId())
            .setSourceCidrList(Objects.nonNull(rule.getSourceCidrList()) ?
                        transformCidrListValues(rule.getSourceCidrList()) : List.of("ANY"))
            .setDestinationCidrList(Objects.nonNull(rule.getDestinationCidrList()) ?
                        transformCidrListValues(rule.getDestinationCidrList()) : List.of("ANY"))
            .setIcmpCode(rule.getIcmpCode())
            .setIcmpType(rule.getIcmpType())
            .setPrivatePort(PortForwardingServiceProvider.getPrivatePortRange(rule))
            .setTrafficType(rule.getTrafficType().toString())
            .setService(Network.Service.Firewall)
            .setProtocol(rule.getProtocol().toUpperCase(Locale.ROOT)).build();
            networkRule.setBaseRule(baseNetRule);
            networkRule.setAclAction(NsxNetworkRule.NsxRuleAction.ALLOW);
            if (rule.getState() == FirewallRule.State.Add) {
                nsxAddNetworkRules.add(networkRule);
            } else if (rule.getState() == FirewallRule.State.Revoke) {
                nsxDelNetworkRules.add(networkRule);
            }
        }
        boolean success = true;
        if (!nsxDelNetworkRules.isEmpty()) {
            success = nsxService.deleteFirewallRules(network, nsxDelNetworkRules);
            if (!success) {
                logger.warn("Not all firewall rules were successfully deleted");
            }
        }
        return success && nsxService.addFirewallRules(network, nsxAddNetworkRules);
    }

    protected NsxNetworkRule.NsxRuleAction transformActionValue(NetworkACLItem.Action action) {
        if (action == NetworkACLItem.Action.Allow) {
            return NsxNetworkRule.NsxRuleAction.ALLOW;
        } else if (action == NetworkACLItem.Action.Deny) {
            return NsxNetworkRule.NsxRuleAction.DROP;
        }
        String err = String.format("Unsupported action %s", action.toString());
        logger.error(err);
        throw new CloudRuntimeException(err);
    }

    /**
     * Replace 0.0.0.0/0 to ANY on each occurrence
     */
    protected List<String> transformCidrListValues(List<String> sourceCidrList) {
        Set<String> set = new HashSet<>();
        if (org.apache.commons.collections.CollectionUtils.isNotEmpty(sourceCidrList)) {
            for (String cidr : sourceCidrList) {
                if (cidr.equals(NetUtils.ALL_IP4_CIDRS) || cidr.equals(NetUtils.ALL_IP6_CIDRS)) {
                    set.add("ANY");
                } else {
                    set.add(cidr);
                }
            }
        }
        return set.stream().sorted().collect(Collectors.toList());
    }

    @Override
    public VirtualRouterProvider configureInternalLoadBalancerElement(long id, boolean enable) {
        VirtualRouterProviderVO element = vrProviderDao.findById(id);
        if (element == null || element.getType() != VirtualRouterProvider.Type.Nsx) {
            throw new InvalidParameterValueException("Can't find " + getName() + " " +
                    "element with network service provider id " + id + " to be used as a provider for " +
                    getName());
        }

        element.setEnabled(enable);
        element = vrProviderDao.persist(element);

        return element;
    }

    @Override
    public VirtualRouterProvider addInternalLoadBalancerElement(long ntwkSvcProviderId) {
        VirtualRouterProviderVO element = vrProviderDao.findByNspIdAndType(ntwkSvcProviderId, VirtualRouterProvider.Type.Nsx);
        if (element != null) {
            logger.debug("There is already an " + getName() + " with service provider id " + ntwkSvcProviderId);
            return null;
        }

        PhysicalNetworkServiceProvider provider = pNtwkSvcProviderDao.findById(ntwkSvcProviderId);
        if (provider == null || !provider.getProviderName().equalsIgnoreCase(getName())) {
            throw new InvalidParameterValueException("Invalid network service provider is specified");
        }

        element = new VirtualRouterProviderVO(ntwkSvcProviderId, VirtualRouterProvider.Type.Nsx);
        element = vrProviderDao.persist(element);
        return element;
    }

    @Override
    public VirtualRouterProvider getInternalLoadBalancerElement(long id) {
        VirtualRouterProvider provider = vrProviderDao.findById(id);
        if (provider == null || provider.getType() != VirtualRouterProvider.Type.Nsx) {
            throw new InvalidParameterValueException("Unable to find " + getName() + " by id");
        }
        return provider;
    }

    @Override
    public List<? extends VirtualRouterProvider> searchForInternalLoadBalancerElements(Long id, Long ntwkSvsProviderId, Boolean enabled) {
        QueryBuilder<VirtualRouterProviderVO> sc = QueryBuilder.create(VirtualRouterProviderVO.class);
        if (id != null) {
            sc.and(sc.entity().getId(), SearchCriteria.Op.EQ, id);
        }
        if (ntwkSvsProviderId != null) {
            sc.and(sc.entity().getNspId(), SearchCriteria.Op.EQ, ntwkSvsProviderId);
        }
        if (enabled != null) {
            sc.and(sc.entity().isEnabled(), SearchCriteria.Op.EQ, enabled);
        }

        //return only Internal LB elements
        sc.and(sc.entity().getType(), SearchCriteria.Op.EQ, VirtualRouterProvider.Type.Nsx);

        return sc.list();
    }

    @Override
    public VirtualRouterProvider.Type getProviderType() {
        return VirtualRouterProvider.Type.Nsx;
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(CreateInternalLoadBalancerElementCmd.class);
        cmdList.add(ConfigureInternalLoadBalancerElementCmd.class);
        cmdList.add(ListInternalLoadBalancerElementsCmd.class);
        return cmdList;
    }

    @Override
    public boolean updateVpcSourceNatIp(Vpc vpc, IpAddress address) {
        return nsxService.updateVpcSourceNatIp(vpc, address);
    }

    protected boolean isVpnProvidedByNsx(Vpc vpc) {
        if (Objects.isNull(vpc)) {
            return false;
        }
        if (vpcManager != null) {
            return vpcManager.isProviderSupportServiceInVpc(vpc.getId(), Network.Service.Vpn, Network.Provider.Nsx);
        }
        return Objects.nonNull(vpcOfferingServiceMapDao.findByServiceProviderAndOfferingId(
                Network.Service.Vpn.getName(), Network.Provider.Nsx.getName(), vpc.getVpcOfferingId()));
    }

    protected boolean isVpnProvidedByNsx(Vpc vpc, Site2SiteVpnGateway gateway) {
        return vpc != null && ownsVpnGateway(gateway);
    }

    @Override
    public IpAddress acquireVpnGatewayIp(Vpc vpc, IpAddress requestedIp) {
        if (!isVpnProvidedByNsx(vpc)) {
            return null;
        }
        IPAddressVO ip;
        boolean autoAcquired = false;
        if (Objects.nonNull(requestedIp)) {
            ip = validateRequestedVpnGatewayIp(vpc, requestedIp);
        } else {
            ip = allocateVpnGatewayIp(vpc);
            autoAcquired = true;
        }
        boolean requestedIpOwnershipMarkerAdded = false;
        if (!autoAcquired) {
            try {
                // Preserve an earlier ownership marker: it may represent an ambiguous create that
                // still requires provider-side cleanup and must not be downgraded on a retry.
                UserIpAddressDetailVO existingOwnershipMarker = userIpAddressDetailsDao.findDetail(
                        ip.getId(), NSX_VPN_GATEWAY_IP_DETAIL);
                if (existingOwnershipMarker == null) {
                    userIpAddressDetailsDao.addDetail(ip.getId(), NSX_VPN_GATEWAY_IP_DETAIL, "false", false);
                    requestedIpOwnershipMarkerAdded = true;
                }
            } catch (Exception e) {
                throw new CloudRuntimeException(String.format(
                        "Failed to record NSX VPN ownership for requested IP %s of VPC %s",
                        ip.getAddress(), vpc.getName()), e);
            }
        }
        boolean endpointMayBeInUse = true;
        try {
            NsxVpnGatewayResult result = nsxService.createVpnGateway(vpc, ip.getAddress().addr());
            endpointMayBeInUse = result.isEndpointMayBeInUse();
            if (!result.isSuccessful()) {
                throw new CloudRuntimeException(String.format("The NSX VPN gateway service for VPC %s was not created: the provider returned an unsuccessful answer",
                        vpc.getName()));
            }
        } catch (Exception e) {
            if (autoAcquired && !endpointMayBeInUse) {
                try {
                    releaseAutoAcquiredVpnGatewayIp(ip);
                } catch (Exception cleanupException) {
                    logger.warn("Failed to release the auto-acquired VPN gateway IP {} of VPC {} after creation failed: {}",
                            ip.getAddress(), vpc.getName(), cleanupException.getMessage());
                }
            } else if (autoAcquired) {
                logger.warn("Retaining auto-acquired VPN gateway IP {} for VPC {} because the NSX endpoint may still be using it",
                        ip.getAddress(), vpc.getName());
            } else if (!endpointMayBeInUse && requestedIpOwnershipMarkerAdded) {
                try {
                    userIpAddressDetailsDao.removeDetail(ip.getId(), NSX_VPN_GATEWAY_IP_DETAIL);
                } catch (Exception cleanupException) {
                    logger.warn("Failed to remove the NSX VPN ownership marker from requested IP {} of VPC {} after gateway creation failed: {}",
                            ip.getAddress(), vpc.getName(), cleanupException.getMessage());
                }
            }
            throw new CloudRuntimeException(String.format("Failed to create the NSX VPN gateway for VPC %s: %s",
                    vpc.getName(), e.getMessage()), e);
        }
        return ip;
    }

    private IPAddressVO validateRequestedVpnGatewayIp(Vpc vpc, IpAddress requestedIp) {
        IPAddressVO ip = ipAddressDao.findById(requestedIp.getId());
        if (Objects.isNull(ip) || !Objects.equals(ip.getVpcId(), vpc.getId())
                || !ip.readyToUse() || ip.getRemoved() != null || ip.getAddress() == null) {
            throw new InvalidParameterValueException(String.format(
                    "The requested IP id %s is not an allocated, active IP associated to the VPC %s",
                    requestedIp.getId(), vpc.getName()));
        }
        if (ip.isSourceNat() || ip.isForSystemVms()) {
            throw new InvalidParameterValueException(String.format(
                    "The requested IP %s cannot be used as the VPN gateway IP as it is a source NAT or system IP", ip.getAddress().addr()));
        }
        if (ip.isOneToOneNat() || !firewallRulesDao.listByIpAndNotRevoked(ip.getId()).isEmpty()
                || !portForwardingRulesDao.listByIpAndNotRevoked(ip.getId()).isEmpty()
                || !loadBalancerDao.listByIpAddress(ip.getId()).isEmpty()) {
            throw new InvalidParameterValueException(String.format(
                    "The requested IP %s cannot be used as the VPN gateway IP as it is already in use by static NAT or network rules", ip.getAddress().addr()));
        }
        return ip;
    }

    private IPAddressVO allocateVpnGatewayIp(Vpc vpc) {
        Account owner = accountMgr.getAccount(vpc.getAccountId());
        DataCenterVO zone = dataCenterDao.findById(vpc.getZoneId());
        IpAddress allocatedIp = null;
        try {
            allocatedIp = ipAddressManager.allocateIp(owner, false, CallContext.current().getCallingAccount(),
                    CallContext.current().getCallingUser(), zone, null, null);
            vpcService.associateIPToVpc(allocatedIp.getId(), vpc.getId());
            userIpAddressDetailsDao.addDetail(allocatedIp.getId(), NSX_VPN_GATEWAY_IP_DETAIL, "true", false);
            IPAddressVO ip = ipAddressDao.findById(allocatedIp.getId());
            if (ip == null) {
                throw new CloudRuntimeException(String.format("The allocated VPN gateway IP %s could not be loaded after association",
                        allocatedIp.getId()));
            }
            if (ip.isSourceNat()) {
                throw new CloudRuntimeException(String.format(
                        "The allocated IP %s became a source NAT IP when it was associated to VPC %s; it cannot be used as a dedicated VPN endpoint",
                        ip.getAddress(), vpc.getName()));
            }
            return ip;
        } catch (Exception e) {
            // do not leak the IP when associating or tagging it fails after allocation succeeded
            if (Objects.nonNull(allocatedIp)) {
                IPAddressVO ipToRelease = ipAddressDao.findById(allocatedIp.getId());
                if (Objects.nonNull(ipToRelease)) {
                    try {
                        releaseAutoAcquiredVpnGatewayIp(ipToRelease);
                    } catch (Exception releaseException) {
                        logger.warn("Failed to release the IP {} allocated for the VPN gateway of VPC {}: {}",
                                ipToRelease.getAddress().addr(), vpc.getName(), releaseException.getMessage());
                    }
                } else {
                    try {
                        ipAddressManager.disassociatePublicIpAddress(allocatedIp, CallContext.current().getCallingUserId(),
                                CallContext.current().getCallingAccount());
                    } catch (Exception releaseException) {
                        logger.warn("Failed to release allocated VPN gateway IP {} of VPC {} after its database row disappeared: {}",
                                allocatedIp.getId(), vpc.getName(), releaseException.getMessage());
                    }
                }
            }
            throw new CloudRuntimeException(String.format("Failed to acquire an IP for the VPN gateway of VPC %s: %s",
                    vpc.getName(), e.getMessage()), e);
        }
    }

    private void releaseAutoAcquiredVpnGatewayIp(IPAddressVO ip) {
        boolean disassociated = ipAddressManager.disassociatePublicIpAddress(ip, CallContext.current().getCallingUserId(),
                CallContext.current().getCallingAccount());
        if (!disassociated) {
            throw new CloudRuntimeException(String.format("Failed to disassociate auto-acquired VPN gateway IP %s", ip.getAddress()));
        }
        userIpAddressDetailsDao.removeDetail(ip.getId(), NSX_VPN_GATEWAY_IP_DETAIL);
    }

    @Override
    public void releaseVpnGatewayIp(Site2SiteVpnGateway gateway) {
        VpcVO vpc = vpcDao.findById(gateway.getVpcId());
        if (vpc != null) {
            try {
                if (!nsxService.deleteVpnGateway(vpc)) {
                    throw new CloudRuntimeException(String.format("The NSX VPN gateway service for VPC %s was not deleted: the provider returned an unsuccessful answer",
                            vpc.getName()));
                }
            } catch (Exception e) {
                throw new CloudRuntimeException(String.format("Failed to delete the NSX VPN gateway of VPC %s: %s",
                        vpc.getName(), e.getMessage()), e);
            }
        }
        IPAddressVO ip = ipAddressDao.findById(gateway.getAddrId());
        if (Objects.isNull(ip)) {
            return;
        }
        UserIpAddressDetailVO autoAcquiredDetail = userIpAddressDetailsDao.findDetail(ip.getId(), NSX_VPN_GATEWAY_IP_DETAIL);
        if (Objects.isNull(autoAcquiredDetail)) {
            return;
        }
        if (Boolean.parseBoolean(autoAcquiredDetail.getValue())) {
            logger.debug("Releasing the auto-acquired VPN gateway IP {} of VPC {}", ip.getAddress().addr(),
                    vpc == null ? gateway.getVpcId() : vpc.getName());
            releaseAutoAcquiredVpnGatewayIp(ip);
        } else {
            // The marker is provider ownership state, not a permanent attribute of the address.
            // Remove it after the NSX objects are gone so a later gateway using this IP cannot be
            // mistaken for an NSX-owned gateway during offering-change cleanup.
            userIpAddressDetailsDao.removeDetail(ip.getId(), NSX_VPN_GATEWAY_IP_DETAIL);
        }
    }

    @Override
    public boolean ownsVpnGateway(Site2SiteVpnGateway gateway) {
        if (gateway == null) {
            return false;
        }
        return userIpAddressDetailsDao.findDetail(gateway.getAddrId(), NSX_VPN_GATEWAY_IP_DETAIL) != null;
    }

    @Override
    public void validateSite2SiteVpnCustomerGateway(Site2SiteCustomerGateway customerGateway) {
        if (!NetUtils.isValidIp4(customerGateway.getGatewayIp())) {
            throw new InvalidParameterValueException(String.format(
                    "NSX Site-to-Site VPN requires an IPv4 peer address; customer gateway %s uses %s",
                    customerGateway.getName(), customerGateway.getGatewayIp()));
        }
        NsxVpnCryptoUtils.validate(customerGateway.getIkePolicy(), customerGateway.getEspPolicy(),
                customerGateway.getIkeVersion(), customerGateway.getIkeLifetime(), customerGateway.getEspLifetime(),
                customerGateway.getIpsecPsk());
    }

    @Override
    public boolean startSite2SiteVpn(Site2SiteVpnConnection conn) throws ResourceUnavailableException {
        Site2SiteVpnGatewayVO vpnGateway = vpnGatewayDao.findById(conn.getVpnGatewayId());
        if (Objects.isNull(vpnGateway)) {
            throw new CloudRuntimeException(String.format(
                    "Cannot find the VPN gateway %s of the Site-to-Site VPN connection %s", conn.getVpnGatewayId(), conn.getUuid()));
        }
        VpcVO vpc = vpcDao.findById(vpnGateway.getVpcId());
        if (Objects.isNull(vpc)) {
            throw new CloudRuntimeException(String.format(
                    "Cannot find the VPC %s of the VPN gateway of Site-to-Site VPN connection %s",
                    vpnGateway.getVpcId(), conn.getUuid()));
        }
        if (!isVpnProvidedByNsx(vpc, vpnGateway)) {
            return true;
        }
        Site2SiteCustomerGatewayVO customerGateway = customerGatewayDao.findById(conn.getCustomerGatewayId());
        if (Objects.isNull(customerGateway)) {
            throw new CloudRuntimeException(String.format(
                    "Cannot find the customer gateway %s of the Site-to-Site VPN connection %s", conn.getCustomerGatewayId(), conn.getUuid()));
        }
        validateSite2SiteVpnCustomerGateway(customerGateway);
        if (BooleanUtils.isTrue(customerGateway.getEncap())) {
            logger.debug("Ignoring forceencap for the NSX VPN connection {}: NSX negotiates NAT-T automatically", conn);
        }
        if (BooleanUtils.isTrue(customerGateway.getSplitConnections())) {
            logger.debug("Ignoring splitconnections for the NSX VPN connection {}: a route-based session carries all subnets", conn);
        }
        IPAddressVO localEndpointIp = ipAddressDao.findById(vpnGateway.getAddrId());
        if (Objects.isNull(localEndpointIp)) {
            throw new CloudRuntimeException(String.format(
                    "Cannot find the local endpoint IP %s of the VPN gateway of VPC %s", vpnGateway.getAddrId(), vpc.getName()));
        }
        Pair<String, String> vtiAddresses = NsxHelper.getVpnVtiAddressPair(conn.getId());
        List<String> peerCidrs = Arrays.stream(customerGateway.getGuestCidrList().split(","))
                .map(String::trim)
                .collect(Collectors.toList());
        return nsxService.createVpnConnection(vpc, conn.getUuid(), customerGateway.getGatewayIp(),
                customerGateway.getIpsecPsk(), customerGateway.getIkePolicy(), customerGateway.getEspPolicy(),
                customerGateway.getIkeLifetime(), customerGateway.getEspLifetime(),
                BooleanUtils.isTrue(customerGateway.getDpd()), customerGateway.getIkeVersion(), conn.isPassive(),
                peerCidrs, vtiAddresses.first(), vtiAddresses.second(), NsxHelper.VPN_VTI_PREFIX_LENGTH,
                localEndpointIp.getAddress().addr());
    }

    @Override
    public boolean stopSite2SiteVpn(Site2SiteVpnConnection conn) throws ResourceUnavailableException {
        Site2SiteVpnGatewayVO vpnGateway = vpnGatewayDao.findById(conn.getVpnGatewayId());
        if (Objects.isNull(vpnGateway)) {
            throw new CloudRuntimeException(String.format(
                    "Cannot find the VPN gateway %s of the Site-to-Site VPN connection %s", conn.getVpnGatewayId(), conn.getUuid()));
        }
        VpcVO vpc = vpcDao.findById(vpnGateway.getVpcId());
        if (Objects.isNull(vpc)) {
            throw new CloudRuntimeException(String.format(
                    "Cannot find the VPC %s of the VPN gateway of Site-to-Site VPN connection %s",
                    vpnGateway.getVpcId(), conn.getUuid()));
        }
        if (!isVpnProvidedByNsx(vpc, vpnGateway)) {
            return true;
        }
        return nsxService.updateVpnConnectionState(vpc, conn.getUuid(), false);
    }

    @Override
    public boolean deleteSite2SiteVpn(Site2SiteVpnConnection conn) throws ResourceUnavailableException {
        Site2SiteVpnGatewayVO vpnGateway = vpnGatewayDao.findById(conn.getVpnGatewayId());
        if (Objects.isNull(vpnGateway)) {
            throw new CloudRuntimeException(String.format(
                    "Cannot find the VPN gateway %s of the Site-to-Site VPN connection %s", conn.getVpnGatewayId(), conn.getUuid()));
        }
        VpcVO vpc = vpcDao.findById(vpnGateway.getVpcId());
        if (vpc == null) {
            return true;
        }
        return nsxService.deleteVpnConnection(vpc, conn.getUuid());
    }
}
