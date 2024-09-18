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
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.VirtualRouterProvider;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.LoadBalancerVMMapDao;
import com.cloud.network.dao.LoadBalancerVMMapVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.dao.VirtualRouterProviderDao;
import com.cloud.network.element.DhcpServiceProvider;
import com.cloud.network.element.DnsServiceProvider;
import com.cloud.network.element.FirewallServiceProvider;
import com.cloud.network.element.IpDeployer;
import com.cloud.network.element.LoadBalancingServiceProvider;
import com.cloud.network.element.NetworkACLServiceProvider;
import com.cloud.network.element.PortForwardingServiceProvider;
import com.cloud.network.element.StaticNatServiceProvider;
import com.cloud.network.element.VirtualRouterElement;
import com.cloud.network.element.VirtualRouterProviderVO;
import com.cloud.network.element.VpcProvider;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.LoadBalancerContainer;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.network.rules.StaticNat;
import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.network.vpc.PrivateGateway;
import com.cloud.network.vpc.StaticRouteProfile;
import com.cloud.network.vpc.Vpc;
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
import org.apache.cloudstack.network.element.InternalLoadBalancerElementService;
import org.apache.cloudstack.resource.NsxLoadBalancerMember;
import org.apache.cloudstack.resource.NsxNetworkRule;
import org.apache.cloudstack.resource.NsxOpObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.cloudstack.resourcedetail.FirewallRuleDetailVO;
import org.apache.cloudstack.resourcedetail.dao.FirewallRuleDetailsDao;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
        LoadBalancingServiceProvider, FirewallServiceProvider, InternalLoadBalancerElementService, ResourceStateAdapter, Listener {


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
    VirtualRouterProviderDao vrProviderDao;
    @Inject
    PhysicalNetworkServiceProviderDao pNtwkSvcProviderDao;
    @Inject
    FirewallRuleDetailsDao firewallRuleDetailsDao;

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
            String err = String.format("Desired physical network is not present in the zone %s for traffic type %s. ", zone.getName(), Networks.TrafficType.Guest.name());
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
        logger.debug("Checking if Nsx Element can handle service " + service.getName() + " on network "
                + network.getDisplayText());

        if (!networkModel.isProviderForNetwork(getProvider(), network.getId())) {
            logger.debug("Nsx Element is not a provider for network " + network.getDisplayText());
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
                NsxOpObject nsxObject = getNsxOpObject(network);
                String publicPort = getPublicPortRange(rule);

                String privatePort = getPrivatePFPortRange(rule);

                NsxNetworkRule networkRule = new NsxNetworkRule.Builder()
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
                FirewallRuleDetailVO ruleDetail = firewallRuleDetailsDao.findDetail(rule.getId(), ApiConstants.FOR_NSX);
                if (Arrays.asList(FirewallRule.State.Add, FirewallRule.State.Active).contains(rule.getState())) {
                    if ((ruleDetail == null && FirewallRule.State.Add == rule.getState()) || (ruleDetail != null && !ruleDetail.getValue().equalsIgnoreCase("true"))) {
                        logger.debug("Creating port forwarding rule on NSX for VM {} to ports {} - {}",
                                vm.getUuid(), rule.getDestinationPortStart(), rule.getDestinationPortEnd());
                        NsxAnswer answer = nsxService.createPortForwardRule(networkRule);
                        boolean pfRuleResult = answer.getResult();
                        if (pfRuleResult && !answer.isObjectExistent()) {
                            logger.debug("Port forwarding rule {} created on NSX, adding detail on firewall rules details", rule.getId());
                            if (ruleDetail == null && FirewallRule.State.Add == rule.getState()) {
                                logger.debug("Adding new firewall detail for rule {}", rule.getId());
                                firewallRuleDetailsDao.addDetail(rule.getId(), ApiConstants.FOR_NSX, "true", false);
                            } else {
                                logger.debug("Updating firewall detail for rule {}", rule.getId());
                                ruleDetail.setValue("true");
                                firewallRuleDetailsDao.update(ruleDetail.getId(), ruleDetail);
                            }
                        }
                        result &= pfRuleResult;
                    }
                } else if (rule.getState() == FirewallRule.State.Revoke) {
                    if (ruleDetail == null || (ruleDetail != null && ruleDetail.getValue().equalsIgnoreCase("true"))) {
                        boolean pfRuleResult = nsxService.deletePortForwardRule(networkRule);
                        if (pfRuleResult && ruleDetail != null) {
                            logger.debug("Updating firewall rule detail {} for rule {}, set to false", ruleDetail.getId(), rule.getId());
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

    private static String getPublicPortRange(PortForwardingRule rule) {
        return Objects.equals(rule.getSourcePortStart(), rule.getSourcePortEnd()) ?
                String.valueOf(rule.getSourcePortStart()) :
                String.valueOf(rule.getSourcePortStart()).concat("-").concat(String.valueOf(rule.getSourcePortEnd()));
    }

    private static String getPrivatePFPortRange(PortForwardingRule rule) {
        return rule.getDestinationPortStart() == rule.getDestinationPortEnd() ?
                String.valueOf(rule.getDestinationPortStart()) :
                String.valueOf(rule.getDestinationPortStart()).concat("-").concat(String.valueOf(rule.getDestinationPortEnd()));
    }

    private static String getPrivatePortRange(FirewallRule rule) {
        return Objects.equals(rule.getSourcePortStart(), rule.getSourcePortEnd()) ?
                String.valueOf(rule.getSourcePortStart()) :
                String.valueOf(rule.getSourcePortStart()).concat("-").concat(String.valueOf(rule.getSourcePortEnd()));
    }

    private static String getPrivatePortRangeForACLRule(NetworkACLItem rule) {
        return Objects.equals(rule.getSourcePortStart(), rule.getSourcePortEnd()) ?
                String.valueOf(rule.getSourcePortStart()) :
                String.valueOf(rule.getSourcePortStart()).concat("-").concat(String.valueOf(rule.getSourcePortEnd()));
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

    private NsxOpObject getNsxOpObject(Network network) {
        Pair<VpcVO, NetworkVO> vpcOrNetwork = getVpcOrNetwork(network.getVpcId(), network.getId());
        VpcVO vpc = vpcOrNetwork.first();
        NetworkVO networkVO = vpcOrNetwork.second();
        long domainId = getResourceId("domain", vpc, networkVO);
        long accountId = getResourceId("account", vpc, networkVO);
        long zoneId = getResourceId("zone", vpc, networkVO);

        return new NsxOpObject.Builder()
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
            NsxOpObject nsxObject = getNsxOpObject(network);

            List<NsxLoadBalancerMember> lbMembers = getLoadBalancerMembers(loadBalancingRule);
            NsxNetworkRule networkRule = new NsxNetworkRule.Builder()
                    .setDomainId(nsxObject.getDomainId())
                    .setAccountId(nsxObject.getAccountId())
                    .setZoneId(nsxObject.getZoneId())
                    .setNetworkResourceId(nsxObject.getNetworkResourceId())
                    .setNetworkResourceName(nsxObject.getNetworkResourceName())
                    .setVpcResource(nsxObject.isVpcResource())
                    .setMemberList(lbMembers)
                    .setPublicIp(LoadBalancerContainer.Scheme.Public == loadBalancingRule.getScheme() ?
                            publicIp.getAddress().addr() : loadBalancingRule.getSourceIp().addr())
                    .setPublicPort(String.valueOf(loadBalancingRule.getSourcePortStart()))
                    .setPrivatePort(String.valueOf(loadBalancingRule.getDefaultPortStart()))
                    .setRuleId(loadBalancingRule.getId())
                    .setProtocol(loadBalancingRule.getLbProtocol().toUpperCase(Locale.ROOT))
                    .setAlgorithm(loadBalancingRule.getAlgorithm())
                    .build();
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
            String privatePort = getPrivatePortRangeForACLRule(rule);
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
            String privatePort = getPrivatePortRangeForACLRule(rule);
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
        return new NsxNetworkRule.Builder()
                .setRuleId(rule.getId())
                .setSourceCidrList(Objects.nonNull(rule.getSourceCidrList()) ? transformCidrListValues(rule.getSourceCidrList()) : List.of("ANY"))
                .setAclAction(transformActionValue(rule.getAction()))
                .setTrafficType(rule.getTrafficType().toString())
                .setProtocol(rule.getProtocol().toUpperCase())
                .setPublicPort(String.valueOf(rule.getSourcePortStart()))
                .setPrivatePort(privatePort)
                .setIcmpCode(rule.getIcmpCode())
                .setIcmpType(rule.getIcmpType())
                .setService(Network.Service.NetworkACL)
                .build();
    }
        @Override
    public boolean applyFWRules(Network network, List<? extends FirewallRule> rules) throws ResourceUnavailableException {

        if (!canHandle(network, Network.Service.Firewall)) {
            return false;
        }
        List<NsxNetworkRule> nsxAddNetworkRules = new ArrayList<>();
        List<NsxNetworkRule> nsxDelNetworkRules = new ArrayList<>();
        for (FirewallRule rule : rules) {
            NsxNetworkRule networkRule = new NsxNetworkRule.Builder()
                    .setRuleId(rule.getId())
                    .setAclAction(NsxNetworkRule.NsxRuleAction.ALLOW)
                    .setSourceCidrList(Objects.nonNull(rule.getSourceCidrList()) ?
                            transformCidrListValues(rule.getSourceCidrList()) : List.of("ANY"))
                    .setDestinationCidrList(Objects.nonNull(rule.getDestinationCidrList()) ?
                            transformCidrListValues(rule.getDestinationCidrList()) : List.of("ANY"))
                    .setIcmpCode(rule.getIcmpCode())
                    .setIcmpType(rule.getIcmpType())
                    .setPrivatePort(getPrivatePortRange(rule))
                    .setTrafficType(rule.getTrafficType().toString())
                    .setService(Network.Service.Firewall)
                    .setProtocol(rule.getProtocol().toUpperCase(Locale.ROOT))
                    .build();
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
        List<String> list = new ArrayList<>();
        if (org.apache.commons.collections.CollectionUtils.isNotEmpty(sourceCidrList)) {
            for (String cidr : sourceCidrList) {
                if (cidr.equals("0.0.0.0/0")) {
                    list.add("ANY");
                } else {
                    list.add(cidr);
                }
            }
        }
        return list;
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
}
