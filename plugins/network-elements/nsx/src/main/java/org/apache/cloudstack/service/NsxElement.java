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
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.ConnectionException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.element.DhcpServiceProvider;
import com.cloud.network.element.DnsServiceProvider;
import com.cloud.network.element.VpcProvider;
import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.network.vpc.PrivateGateway;
import com.cloud.network.vpc.StaticRouteProfile;
import com.cloud.network.vpc.Vpc;
import com.cloud.offering.NetworkOffering;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceStateAdapter;
import com.cloud.resource.ServerResource;
import com.cloud.resource.UnableDeleteHostException;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.Pair;
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachineProfile;
import net.sf.ehcache.config.InvalidConfigurationException;
import org.apache.cloudstack.StartupNsxCommand;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.LongFunction;

@Component
public class NsxElement extends AdapterBase implements DhcpServiceProvider, DnsServiceProvider, VpcProvider,
        ResourceStateAdapter, Listener {

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

    private static final Logger LOGGER = Logger.getLogger(NsxElement.class);

    private final Map<Network.Service, Map<Network.Capability, String>> capabilities = initCapabilities();


    private static Map<Network.Service, Map<Network.Capability, String>> initCapabilities() {
        Map<Network.Service, Map<Network.Capability, String>> capabilities = new HashMap<>();

        Map<Network.Capability, String> dhcpCapabilities = Map.of(Network.Capability.DhcpAccrossMultipleSubnets, "true");
        capabilities.put(Network.Service.Dhcp, dhcpCapabilities);

        Map<Network.Capability, String> dnsCapabilities = new HashMap<>();
        dnsCapabilities.put(Network.Capability.AllowDnsSuffixModification, "true");
        capabilities.put(Network.Service.Dns, dnsCapabilities);

//        capabilities.put(Network.Service.Connectivity, null);
        capabilities.put(Network.Service.StaticNat, null);

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
    public Network.Provider getProvider() {
        return Network.Provider.Nsx;
    }

    @Override
    public boolean implement(Network network, NetworkOffering offering, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
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
        return nsxService.deleteNetwork(account.getAccountName(), networkVO);
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
        return false;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        agentManager.registerForHostEvents(this, true, true, true);
        resourceManager.registerResourceStateAdapter(this.getClass().getSimpleName(), this);
        return false;
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
        Account account = isNsxAndAccount.second();
        return nsxService.createVpcNetwork(vpc.getZoneId(), zone.getName(), account.getAccountId(), account.getName(), vpc.getName());
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

        return nsxService.deleteVpcNetwork(vpc.getZoneId(), zone.getName(), account.getAccountId(), account.getName(), vpc.getName());
    }

    private Pair<Boolean, Account> validateVpcConfigurationAndGetAccount(DataCenterVO zone, Vpc vpc) {
        if (Objects.isNull(zone)) {
            throw new InvalidParameterValueException(String.format("Failed to find zone with id %s", vpc.getZoneId()));
        }
        Account account = null;
        boolean forNsx = false;
        List<PhysicalNetworkVO> physicalNetworks = physicalNetworkDao.listByZoneAndTrafficType(zone.getId(), Networks.TrafficType.Guest);
        if (CollectionUtils.isNullOrEmpty(physicalNetworks) || physicalNetworks.size() > 1 ) {
            throw new InvalidConfigurationException(String.format("Desired number of physical networks is not present in the zone %s for traffic type %s. ", zone.getName(), Networks.TrafficType.Guest.name()));
        }
        if (physicalNetworks.get(0).getIsolationMethods().contains("NSX")) {
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
        LOGGER.debug("Checking if Nsx Element can handle service " + service.getName() + " on network "
                + network.getDisplayText());

        if (!networkModel.isProviderForNetwork(getProvider(), network.getId())) {
            LOGGER.debug("Nsx Element is not a provider for network " + network.getDisplayText());
            return false;
        }

        return true;
    }

    private final LongFunction<DataCenterVO> zoneFunction = zoneId -> dataCenterDao.findById(zoneId);
}
