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
import com.cloud.dc.Vlan;
import com.cloud.dc.VlanVO;
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
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.PhysicalNetworkTrafficTypeDao;
import com.cloud.network.dao.PhysicalNetworkTrafficTypeVO;
import com.cloud.network.dao.TungstenProviderDao;
import com.cloud.network.element.IpDeployer;
import com.cloud.network.element.StaticNatServiceProvider;
import com.cloud.network.element.TungstenProviderVO;
import com.cloud.network.element.UserDataServiceProvider;
import com.cloud.network.rules.StaticNat;
import com.cloud.offering.NetworkOffering;
import com.cloud.projects.ProjectVO;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceStateAdapter;
import com.cloud.resource.ServerResource;
import com.cloud.resource.UnableDeleteHostException;
import com.cloud.user.Account;
import com.cloud.user.dao.AccountDao;
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
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenFloatingIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenVirtualMachineCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenFloatingIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenVRouterPortCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenVmCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenVmInterfaceCommand;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenPublicNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.SetupTungstenVRouterCommand;
import org.apache.cloudstack.network.tungsten.agent.api.StartupTungstenCommand;
import org.apache.cloudstack.network.tungsten.agent.api.TungstenAnswer;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

@Component
public class TungstenElement extends AdapterBase
    implements StaticNatServiceProvider, UserDataServiceProvider, IpDeployer, ResourceStateAdapter, Listener {
    @Inject
    NetworkModel _networkModel;
    @Inject
    ResourceManager _resourceMgr;
    @Inject
    ProjectDao _projectDao;
    @Inject
    AccountDao _accountDao;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    VMInstanceDao _vmInstanceDao;
    @Inject
    AgentManager _agentMgr;
    @Inject
    VlanDao _vlanDao;
    @Inject
    NetworkDao _networkDao;
    @Inject
    HostDao _hostDao;
    @Inject
    TungstenProviderDao _tungstenProviderDao;
    @Inject
    TungstenFabricUtils _tunstenFabricUtils;
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
        String projectUuid = getProject(config.getAccountId());
        StaticNat staticNat = rules.get(0);
        long sourceIpAddressId = staticNat.getSourceIpAddressId();
        IPAddressVO ipAddressVO = _ipAddressDao.findByIdIncludingRemoved(sourceIpAddressId);
        VMInstanceVO vm = _vmInstanceDao.findByIdIncludingRemoved(ipAddressVO.getAssociatedWithVmId());
        Nic nic = _networkModel.getNicInNetworkIncludingRemoved(vm.getId(), config.getId());
        Network publicNetwork = _networkModel.getSystemNetworkByZoneAndTrafficType(config.getDataCenterId(),
            Networks.TrafficType.Public);
        if (!staticNat.isForRevoke()) {
            CreateTungstenFloatingIpCommand createTungstenFloatingIpPoolCommand = new CreateTungstenFloatingIpCommand(
                projectUuid, publicNetwork.getUuid(), nic.getUuid(),
                TungstenUtils.getFloatingIpPoolName(config.getDataCenterId()),
                TungstenUtils.getFloatingIpName(nic.getId()), ipAddressVO.getAddress().addr(),
                staticNat.getDestIpAddress());
            TungstenAnswer createFloatingIpAnswer = _tunstenFabricUtils.sendTungstenCommand(
                createTungstenFloatingIpPoolCommand, config.getDataCenterId());
            if (!createFloatingIpAnswer.getResult()) {
                return false;
            }
        } else {
            DeleteTungstenFloatingIpCommand deleteTungstenFloatingIpCommand = new DeleteTungstenFloatingIpCommand(
                projectUuid, publicNetwork.getUuid(), TungstenUtils.getFloatingIpPoolName(config.getDataCenterId()),
                TungstenUtils.getFloatingIpName(nic.getId()));
            TungstenAnswer deleteFloatingIpAnswer = _tunstenFabricUtils.sendTungstenCommand(
                deleteTungstenFloatingIpCommand, config.getDataCenterId());
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
            String projectUuid = getProject(context.getAccountId());
            String vmType = vm.getType() == VirtualMachine.Type.ConsoleProxy ? TungstenUtils.getProxyVm() :
                TungstenUtils.getSecstoreVm();
            VMInstanceVO vmInstanceVO = _vmInstanceDao.findById(vm.getId());
            HostVO host = _hostDao.findById(vmInstanceVO.getHostId());
            CreateTungstenVirtualMachineCommand cmd = new CreateTungstenVirtualMachineCommand(projectUuid,
                network.getUuid(), vm.getUuid(), vm.getInstanceName(), nic.getUuid(), nic.getId(), nic.getIPv4Address(),
                nic.getMacAddress(), vmType, TungstenUtils.getPublicType(), host.getPublicIpAddress());
            TungstenAnswer createVirtualMachineAnswer = _tunstenFabricUtils.sendTungstenCommand(cmd,
                network.getDataCenterId());
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
        if (network.getTrafficType() == Networks.TrafficType.Public) {
            try {
                VMInstanceVO vmInstanceVO = _vmInstanceDao.findById(vm.getId());
                HostVO host = _hostDao.findById(vmInstanceVO.getLastHostId());
                DeleteTungstenVRouterPortCommand deleteTungstenVRouterPortCommand =
                    new DeleteTungstenVRouterPortCommand(
                    host.getPublicIpAddress(), nic.getUuid());
                _tunstenFabricUtils.sendTungstenCommand(deleteTungstenVRouterPortCommand, network.getDataCenterId());

                String vmType = vm.getType() == VirtualMachine.Type.ConsoleProxy ? TungstenUtils.getProxyVm() :
                    TungstenUtils.getSecstoreVm();
                String nicName = TungstenUtils.getVmiName(TungstenUtils.getPublicType(), vmType, vm.getInstanceName(),
                    nic.getId());
                String projectUuid = getProject(vm.getOwner().getAccountId());
                DeleteTungstenVmInterfaceCommand deleteVmiCmd = new DeleteTungstenVmInterfaceCommand(projectUuid,
                    nicName);
                _tunstenFabricUtils.sendTungstenCommand(deleteVmiCmd, network.getDataCenterId());

                DeleteTungstenVmCommand deleteVmCmd = new DeleteTungstenVmCommand(vm.getUuid());
                _tunstenFabricUtils.sendTungstenCommand(deleteVmCmd, network.getDataCenterId());
            } catch (IllegalArgumentException e) {
                throw new CloudRuntimeException("Failing to expuge the vm from tungsten with the uuid " + vm.getUuid());
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
        return true;
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

    public String getProject(long accountId) {
        Account account = _accountDao.findById(accountId);
        if (account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            ProjectVO projectVO = _projectDao.findByProjectAccountId(account.getId());
            if (projectVO != null) {
                return projectVO.getUuid();
            }
        }
        return null;
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
            String vgwName = TungstenUtils.getVgwName(zoneId);
            Network network = _networkDao.listByZoneAndTrafficType(zoneId, Networks.TrafficType.Public).get(0);
            SearchCriteria<VlanVO> sc = _vlanDao.createSearchCriteria();
            sc.setParameters("network_id", network.getId());
            Vlan vlan = _vlanDao.findOneBy(sc);
            String publicSubnet = NetUtils.getCidrFromGatewayAndNetmask(vlan.getVlanGateway(), vlan.getVlanNetmask());
            GetTungstenPublicNetworkCommand getTungstenPublicNetworkCommand = new GetTungstenPublicNetworkCommand(null,
                TungstenUtils.getPublicNetworkName(zoneId));
            TungstenAnswer answer = _tunstenFabricUtils.sendTungstenCommand(getTungstenPublicNetworkCommand, zoneId);
            VirtualNetwork publicVirtualNetwork = (VirtualNetwork) answer.getApiObjectBase();
            SetupTungstenVRouterCommand setupTungstenVRouterCommand = new SetupTungstenVRouterCommand(vgwName,
                publicSubnet, TungstenUtils.getDefaultRoute(),
                TungstenUtils.getVrfNetworkName(publicVirtualNetwork.getQualifiedName()), vlan.getVlanGateway());
            _agentMgr.easySend(host.getId(), setupTungstenVRouterCommand);
        }
    }

    @Override
    public boolean processDisconnect(final long agentId, final Status state) {
        return false;
    }

    @Override
    public void processHostAboutToBeRemoved(final long hostId) {

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
}
