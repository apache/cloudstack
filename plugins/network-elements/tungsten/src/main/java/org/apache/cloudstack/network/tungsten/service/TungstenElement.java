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
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.Vlan;
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
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.TungstenProvider;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkTrafficTypeDao;
import com.cloud.network.dao.PhysicalNetworkTrafficTypeVO;
import com.cloud.network.dao.TungstenProviderDao;
import com.cloud.network.element.FirewallServiceProvider;
import com.cloud.network.element.IpDeployer;
import com.cloud.network.element.StaticNatServiceProvider;
import com.cloud.network.element.TungstenProviderVO;
import com.cloud.network.element.UserDataServiceProvider;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.StaticNat;
import com.cloud.offering.NetworkOffering;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceStateAdapter;
import com.cloud.resource.ServerResource;
import com.cloud.resource.UnableDeleteHostException;
import com.cloud.utils.Pair;
import com.cloud.utils.TungstenUtils;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.VMInstanceDao;
import net.juniper.tungsten.api.types.VirtualNetwork;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.PublishScope;
import org.apache.cloudstack.network.tungsten.agent.api.ApplyTungstenNetworkPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.AssignTungstenFloatingIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenNetworkPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenVirtualMachineCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenFloatingIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenFloatingIpPoolCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenNetworkPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenVRouterPortCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenVmCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenVmInterfaceCommand;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenFabricNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenPublicNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ReleaseTungstenFloatingIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.SetupTungstenVRouterCommand;
import org.apache.cloudstack.network.tungsten.agent.api.StartupTungstenCommand;
import org.apache.cloudstack.network.tungsten.agent.api.TungstenAnswer;
import org.apache.cloudstack.network.tungsten.model.TungstenRule;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

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
    ResourceStateAdapter, Listener {
    @Inject
    HostPodDao _podDao;
    @Inject
    NetworkModel _networkModel;
    @Inject
    ResourceManager _resourceMgr;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    PhysicalNetworkDao _physicalNetworkDao;
    @Inject
    VMInstanceDao _vmInstanceDao;
    @Inject
    AgentManager _agentMgr;
    @Inject
    VlanDao _vlanDao;
    @Inject
    HostDao _hostDao;
    @Inject
    NetworkServiceMapDao _networkServiceMapDao;
    @Inject
    TungstenService _tungstenService;
    @Inject
    MessageBus _messageBus;
    @Inject
    TungstenProviderDao _tungstenProviderDao;
    @Inject
    TungstenFabricUtils _tungstenFabricUtils;
    @Inject
    PhysicalNetworkTrafficTypeDao _physicalNetworkTrafficTypeDao;

    private static final Logger s_logger = Logger.getLogger(TungstenElement.class);
    private final Map<Network.Service, Map<Network.Capability, String>> _capabilities = InitCapabilities();

    protected boolean canHandle(final Network network, final Network.Service service) {
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
        String projectUuid = _tungstenService.getProject(config.getAccountId());
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
            if (!assignFloatingIpAnswer.getResult()) {
                return false;
            }
        } else {
            ReleaseTungstenFloatingIpCommand releaseTungstenFloatingIpCommand = new ReleaseTungstenFloatingIpCommand(
                publicNetwork.getUuid(), TungstenUtils.getFloatingIpPoolName(config.getDataCenterId()),
                TungstenUtils.getFloatingIpName(ipAddressVO.getId()));
            TungstenAnswer deleteFloatingIpAnswer = _tungstenFabricUtils.sendTungstenCommand(
                releaseTungstenFloatingIpCommand, config.getDataCenterId());
            if (!deleteFloatingIpAnswer.getResult()) {
                return false;
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

    private static Map<Network.Service, Map<Network.Capability, String>> InitCapabilities() {
        Map<Network.Service, Map<Network.Capability, String>> capabilities = new HashMap<Network.Service,
            Map<Network.Capability, String>>();
        final Map<Network.Capability, String> dhcpCapabilities = new HashMap<>();
        capabilities.put(Network.Service.Dhcp, dhcpCapabilities);
        Map<Network.Capability, String> sourceNatCapabilities = new HashMap<>();
        sourceNatCapabilities.put(Network.Capability.RedundantRouter, "true");
        sourceNatCapabilities.put(Network.Capability.SupportedSourceNatTypes, "peraccount");
        capabilities.put(Network.Service.SourceNat, sourceNatCapabilities);
        capabilities.put(Network.Service.Connectivity, null);
        capabilities.put(Network.Service.StaticNat, null);
        capabilities.put(Network.Service.UserData, null);
        final Map<Network.Capability, String> dnsCapabilities = new HashMap<>();
        dnsCapabilities.put(Network.Capability.AllowDnsSuffixModification, "true");
        capabilities.put(Network.Service.Dns, dnsCapabilities);
        Map<Network.Capability, String> firewallCapabilities = new HashMap<Network.Capability, String>();
        firewallCapabilities.put(Network.Capability.SupportedProtocols, "tcp,udp,icmp");
        firewallCapabilities.put(Network.Capability.SupportedEgressProtocols, "tcp,udp,icmp,all");
        firewallCapabilities.put(Network.Capability.MultipleIps, "true");
        firewallCapabilities.put(Network.Capability.TrafficStatistics, "per public ip");
        firewallCapabilities.put(Network.Capability.SupportedTrafficDirection, "ingress, egress");
        capabilities.put(Network.Service.Firewall, firewallCapabilities);
        return capabilities;
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
            String projectUuid = _tungstenService.getProject(context.getAccountId());
            String vmType = vm.getType() == VirtualMachine.Type.ConsoleProxy ? TungstenUtils.getProxyVm() :
                TungstenUtils.getSecstoreVm();
            VMInstanceVO vmInstanceVO = _vmInstanceDao.findById(vm.getId());
            HostVO host = _hostDao.findById(vmInstanceVO.getHostId());

            CreateTungstenVirtualMachineCommand createTungstenVirtualMachineCommand =
                new CreateTungstenVirtualMachineCommand(
                projectUuid, network.getUuid(), vm.getUuid(), vm.getInstanceName(), nic.getUuid(), nic.getId(),
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
            String projectUuid = _tungstenService.getProject(context.getAccountId());
            String vmType = vm.getType() == VirtualMachine.Type.ConsoleProxy ? TungstenUtils.getProxyVm() :
                TungstenUtils.getSecstoreVm();
            VMInstanceVO vmInstanceVO = _vmInstanceDao.findById(vm.getId());
            HostVO host = _hostDao.findById(vmInstanceVO.getHostId());

            CreateTungstenVirtualMachineCommand createTungstenVirtualMachineCommand =
                new CreateTungstenVirtualMachineCommand(
                projectUuid, network.getUuid(), vm.getUuid(), vm.getInstanceName(), nic.getUuid(), nic.getId(),
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
                String projectUuid = _tungstenService.getProject(vm.getOwner().getAccountId());
                DeleteTungstenVmInterfaceCommand deleteVmiCmd = new DeleteTungstenVmInterfaceCommand(projectUuid,
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
                String projectUuid = _tungstenService.getProject(vm.getOwner().getAccountId());
                DeleteTungstenVmInterfaceCommand deleteVmiCmd = new DeleteTungstenVmInterfaceCommand(projectUuid,
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
        if (!canHandle(network, Network.Service.Connectivity)) {
            return false;
        }
        return true;
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
        PhysicalNetworkTrafficTypeVO physicalNetworkTrafficTypeVO = _physicalNetworkTrafficTypeDao.findBy(
            provider.getPhysicalNetworkId(), Networks.TrafficType.Public);
        return physicalNetworkTrafficTypeVO != null;
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
            // delete floating ip pool
            DeleteTungstenFloatingIpPoolCommand deleteTungstenFloatingIpPoolCommand =
                new DeleteTungstenFloatingIpPoolCommand(
                publicNetwork.getUuid(), TungstenUtils.getFloatingIpPoolName(zoneId));
            _tungstenFabricUtils.sendTungstenCommand(deleteTungstenFloatingIpPoolCommand, zoneId);

            // get tungsten fabric network and remove default network policy
            GetTungstenFabricNetworkCommand getTungstenFabricNetworkCommand = new GetTungstenFabricNetworkCommand();
            TungstenAnswer getTungstenFabricNetworkAnswer = _tungstenFabricUtils.sendTungstenCommand(
                getTungstenFabricNetworkCommand, zoneId);
            if (getTungstenFabricNetworkAnswer.getResult()) {
                DeleteTungstenNetworkPolicyCommand deleteFabricNetworkPolicyCommand =
                    new DeleteTungstenNetworkPolicyCommand(
                    TungstenUtils.getFabricNetworkPolicyName(), null,
                    getTungstenFabricNetworkAnswer.getApiObjectBase().getUuid());
                _tungstenFabricUtils.sendTungstenCommand(deleteFabricNetworkPolicyCommand, zoneId);
            }

            // clear public network policy
            DeleteTungstenNetworkPolicyCommand deleteTungstenNetworkPolicyCommand =
                new DeleteTungstenNetworkPolicyCommand(
                TungstenUtils.getVirtualNetworkPolicyName(publicNetwork.getId()), null, publicNetwork.getUuid());
            _tungstenFabricUtils.sendTungstenCommand(deleteTungstenNetworkPolicyCommand, zoneId);

            // delete public network
            DeleteTungstenNetworkCommand deleteTungstenNetworkCommand = new DeleteTungstenNetworkCommand(
                publicNetwork.getUuid());
            _tungstenFabricUtils.sendTungstenCommand(deleteTungstenNetworkCommand, zoneId);

            List<HostPodVO> listPod = _podDao.listByDataCenterId(zoneId);
            for (HostPodVO pod : listPod) {
                _tungstenService.deleteManagementNetwork(pod);
            }
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

    private Vlan getVlan(long zoneId) {
        Network network = _networkModel.getSystemNetworkByZoneAndTrafficType(zoneId, Networks.TrafficType.Public);
        SearchCriteria<VlanVO> sc = _vlanDao.createSearchCriteria();
        sc.setParameters("network_id", network.getId());
        return _vlanDao.findOneBy(sc);
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
            && cmd instanceof StartupRoutingCommand && host.getPublicIpAddress()
            .equals(tungstenProvider.getVrouter())) {
            Vlan vlan = getVlan(zoneId);
            String publicSubnet = NetUtils.getCidrFromGatewayAndNetmask(vlan.getVlanGateway(), vlan.getVlanNetmask());
            GetTungstenPublicNetworkCommand getTungstenPublicNetworkCommand = new GetTungstenPublicNetworkCommand(null,
                TungstenUtils.getPublicNetworkName(zoneId));
            TungstenAnswer answer = _tungstenFabricUtils.sendTungstenCommand(getTungstenPublicNetworkCommand, zoneId);
            VirtualNetwork publicVirtualNetwork = (VirtualNetwork) answer.getApiObjectBase();
            SetupTungstenVRouterCommand setupTungstenVRouterCommand = new SetupTungstenVRouterCommand("create",
                TungstenUtils.getVgwName(zoneId), publicSubnet, NetUtils.ALL_IP4_CIDRS,
                TungstenUtils.getVrfNetworkName(publicVirtualNetwork.getQualifiedName()));
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
            && host.getPublicIpAddress().equals(tungstenProvider.getVrouter())) {
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
        String projectUuid;
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
                    projectUuid = _tungstenService.getProject(network.getAccountId());
                    networkUuid = network.getUuid();
                } else {
                    projectUuid = null;
                    networkUuid = publicNetwork.getUuid();
                }

                if (firewallRule.getState() == FirewallRule.State.Add) {
                    CreateTungstenNetworkPolicyCommand createTungstenNetworkPolicyCommand =
                        new CreateTungstenNetworkPolicyCommand(
                        TungstenUtils.getRuleNetworkPolicyName(firewallRule.getId()), projectUuid, tungstenRuleList);
                    TungstenAnswer createNetworkPolicyAnswer = _tungstenFabricUtils.sendTungstenCommand(
                        createTungstenNetworkPolicyCommand, network.getDataCenterId());
                    result = result && createNetworkPolicyAnswer.getResult();

                    ApplyTungstenNetworkPolicyCommand applyTungstenNetworkPolicyCommand =
                        new ApplyTungstenNetworkPolicyCommand(
                        projectUuid, TungstenUtils.getRuleNetworkPolicyName(firewallRule.getId()), networkUuid, true);
                    TungstenAnswer applyNetworkPolicyAnswer = _tungstenFabricUtils.sendTungstenCommand(
                        applyTungstenNetworkPolicyCommand, network.getDataCenterId());
                    result = result && applyNetworkPolicyAnswer.getResult();

                    return result;
                }

                if (firewallRule.getState() == FirewallRule.State.Revoke) {
                    DeleteTungstenNetworkPolicyCommand deleteTungstenNetworkPolicyCommand =
                        new DeleteTungstenNetworkPolicyCommand(
                        TungstenUtils.getRuleNetworkPolicyName(firewallRule.getId()), projectUuid, networkUuid);
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
}
