package org.apache.cloudstack.network.tungsten.service;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.StartupCommand;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
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
import org.apache.cloudstack.network.tungsten.agent.api.AddTungstenVirtualGatewayCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenFloatingIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenFloatingIpPoolCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenLogicalRouterCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenVirtualMachineCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenFloatingIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenVRouterPortCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenVmCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenVmInterfaceCommand;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenNetNsName;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenPublicNetworkCommand;
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
    implements StaticNatServiceProvider, UserDataServiceProvider, IpDeployer, ResourceStateAdapter {
    @Inject
    AgentManager agentMgr;
    @Inject
    NetworkModel _networkModel;
    @Inject
    ResourceManager _resourceMgr;
    @Inject
    ProjectDao _projectDao;
    @Inject
    HostDao _hostDao;
    @Inject
    AccountDao _accountDao;
    @Inject
    IPAddressDao ipAddressDao;
    @Inject
    VlanDao vlanDao;
    @Inject
    IpAddressManager _ipAddrMgr;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    VMInstanceDao vmInstanceDao;
    @Inject
    TungstenProviderDao tungstenProviderDao;
    @Inject
    TungstenFabricUtils _tunstenFabricUtils;

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
        IPAddressVO ipAddressVO = ipAddressDao.findByIdIncludingRemoved(sourceIpAddressId);
        VMInstanceVO vm = vmInstanceDao.findByIdIncludingRemoved(ipAddressVO.getAssociatedWithVmId());
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
                createTungstenFloatingIpPoolCommand, config);
            if (!createFloatingIpAnswer.getResult()) {
                return false;
            }
        } else {
            DeleteTungstenFloatingIpCommand deleteTungstenFloatingIpCommand = new DeleteTungstenFloatingIpCommand(
                projectUuid, publicNetwork.getUuid(), TungstenUtils.getFloatingIpPoolName(config.getDataCenterId()),
                TungstenUtils.getFloatingIpName(nic.getId()));
            TungstenAnswer deleteFloatingIpAnswer = _tunstenFabricUtils.sendTungstenCommand(
                deleteTungstenFloatingIpCommand, config);
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
            Long zoneId = network.getDataCenterId();
            String projectUuid = getProject(context.getAccountId());
            TungstenProviderVO tungstenProvider = tungstenProviderDao.findByZoneId(zoneId);

            GetTungstenPublicNetworkCommand getTungstenPublicNetworkCommand = new GetTungstenPublicNetworkCommand(
                projectUuid, TungstenUtils.getPublicNetworkName(zoneId));
            TungstenAnswer getTungstenPublicNetworkAnswer = _tunstenFabricUtils.sendTungstenCommand(
                getTungstenPublicNetworkCommand, network);
            if (getTungstenPublicNetworkAnswer.getApiObjectBase() == null) {
                SearchCriteria<VlanVO> sc = vlanDao.createSearchCriteria();
                sc.setParameters("network_id", network.getId());
                VlanVO pubVlanVO = vlanDao.findOneBy(sc);
                String publicNetworkCidr = NetUtils.getCidrFromGatewayAndNetmask(pubVlanVO.getVlanGateway(),
                    pubVlanVO.getVlanNetmask());
                Pair<String, Integer> publicPair = NetUtils.getCidr(publicNetworkCidr);
                String pubIp = this.getPublicIPAddress(network);

                // create public network
                CreateTungstenNetworkCommand createTungstenPublicNetworkCommand = new CreateTungstenNetworkCommand(
                    network.getUuid(), TungstenUtils.getPublicNetworkName(zoneId), projectUuid, true, false,
                    publicPair.first(), publicPair.second(), pubVlanVO.getVlanGateway(), true, null, pubIp, pubIp,
                    false);
                TungstenAnswer createPublicNetworkAnswer = _tunstenFabricUtils.sendTungstenCommand(
                    createTungstenPublicNetworkCommand, network);
                if (!createPublicNetworkAnswer.getResult()) {
                    throw new CloudRuntimeException("can not create tungsten public network");
                }
                VirtualNetwork publicVirtualNetwork = (VirtualNetwork) createPublicNetworkAnswer.getApiObjectBase();

                // create floating ip pool
                CreateTungstenFloatingIpPoolCommand createTungstenFloatingIpPoolCommand =
                    new CreateTungstenFloatingIpPoolCommand(
                    network.getUuid(), TungstenUtils.getFloatingIpPoolName(zoneId));
                TungstenAnswer createFloatingIpPoolAnswer = _tunstenFabricUtils.sendTungstenCommand(
                    createTungstenFloatingIpPoolCommand, network);
                if (!createFloatingIpPoolAnswer.getResult()) {
                    throw new CloudRuntimeException("can not create tungsten floating ip pool");
                }

                // create logical router with public network
                CreateTungstenLogicalRouterCommand createTungstenLogicalRouterCommand =
                    new CreateTungstenLogicalRouterCommand(
                    TungstenUtils.getLogicalRouterName(zoneId), projectUuid, publicVirtualNetwork.getUuid());
                TungstenAnswer createLogicalRouterAnswer = _tunstenFabricUtils.sendTungstenCommand(
                    createTungstenLogicalRouterCommand, network);
                if (!createLogicalRouterAnswer.getResult()) {
                    throw new CloudRuntimeException("can not create tungsten logical router");
                }

                GetTungstenNetNsName getTungstenNetNsName = new GetTungstenNetNsName(
                    createLogicalRouterAnswer.getApiObjectBase().getUuid());
                TungstenAnswer getNetNsNameAnswer = _tunstenFabricUtils.sendTungstenCommand(getTungstenNetNsName,
                    network);

                // add tungsten virtual gateway
                HostVO hostVO = _hostDao.findByPublicIp(tungstenProvider.getVrouter());
                AddTungstenVirtualGatewayCommand addTungstenVirtualGatewayCommand =
                    new AddTungstenVirtualGatewayCommand(
                    TungstenUtils.getVgwName(zoneId), publicNetworkCidr, TungstenUtils.getDefaultRoute(),
                    TungstenUtils.getVrfNetworkName(publicVirtualNetwork.getQualifiedName()),
                    getNetNsNameAnswer.getDetails(), pubVlanVO.getVlanGateway());
                TungstenAnswer addVirtualGatewayAnswer = (TungstenAnswer) agentMgr.easySend(hostVO.getId(),
                    addTungstenVirtualGatewayCommand);
                if (!addVirtualGatewayAnswer.getResult()) {
                    throw new CloudRuntimeException("can not add tungsten virtual gateway");
                }
            }

            nic.setBroadcastType(Networks.BroadcastDomainType.Tungsten);
            nic.setBroadcastUri(Networks.BroadcastDomainType.Tungsten.toUri("tf"));

            String vmType = vm.getType() == VirtualMachine.Type.ConsoleProxy ? TungstenUtils.getProxyVm() :
                TungstenUtils.getSecstoreVm();
            CreateTungstenVirtualMachineCommand cmd = new CreateTungstenVirtualMachineCommand(projectUuid,
                network.getUuid(), vm.getUuid(), vm.getInstanceName(), nic.getUuid(), nic.getId(), nic.getIPv4Address(),
                nic.getMacAddress(), vmType, TungstenUtils.getPublicType());
            TungstenAnswer createVirtualMachineAnswer = _tunstenFabricUtils.sendTungstenCommand(cmd, network);
            if (!createVirtualMachineAnswer.getResult()) {
                throw new CloudRuntimeException("can not create tungsten vm");
            }

            nic.setName(nic.getName() + TungstenUtils.getBridgeName());
        }
        return true;
    }

    @Override
    public boolean release(Network network, NicProfile nic, VirtualMachineProfile vm, ReservationContext context)
        throws ConcurrentOperationException, ResourceUnavailableException {
        if (network.getTrafficType() == Networks.TrafficType.Public) {
            try {
                DeleteTungstenVRouterPortCommand deleteTungstenVRouterPortCommand =
                    new DeleteTungstenVRouterPortCommand(
                    nic.getUuid());

                String vmType = vm.getType() == VirtualMachine.Type.ConsoleProxy ? TungstenUtils.getProxyVm() :
                    TungstenUtils.getSecstoreVm();
                String nicName = TungstenUtils.getVmiName(TungstenUtils.getPublicType(), vmType, vm.getInstanceName(),
                    nic.getId());
                String projectUuid = getProject(vm.getOwner().getAccountId());
                DeleteTungstenVmInterfaceCommand deleteVmiCmd = new DeleteTungstenVmInterfaceCommand(projectUuid,
                    nicName);
                _tunstenFabricUtils.sendTungstenCommand(deleteVmiCmd, network);

                DeleteTungstenVmCommand deleteVmCmd = new DeleteTungstenVmCommand(vm.getUuid());
                _tunstenFabricUtils.sendTungstenCommand(deleteVmCmd, network);
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
        return true;
    }

    @Override
    public boolean shutdownProviderInstances(PhysicalNetworkServiceProvider provider, ReservationContext context)
        throws ConcurrentOperationException, ResourceUnavailableException {
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
        super.configure(name, params);
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

    private String getPublicIPAddress(Network network) {
        List<IPAddressVO> allocatedIps = ipAddressDao.listByAssociatedNetwork(network.getId(), true);
        for (IPAddressVO ip : allocatedIps) {
            if (ip.isSourceNat()) {
                return ip.getAddress().addr();
            }
        }

        try {
            PublicIp publicIp = _ipAddrMgr.assignSourceNatIpAddressToGuestNetwork(
                _accountDao.findById(network.getAccountId()), network);
            IPAddressVO ip = publicIp.ip();
            _ipAddressDao.acquireInLockTable(ip.getId());
            _ipAddressDao.update(ip.getId(), ip);
            _ipAddressDao.releaseFromLockTable(ip.getId());
            return ip.getAddress().addr();
        } catch (Exception e) {
            s_logger.error("Unable to allocate source nat ip: " + e);
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
}
