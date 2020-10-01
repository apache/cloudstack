package org.apache.cloudstack.network.tungsten.service;

import com.cloud.agent.AgentManager;
import com.cloud.dc.DataCenter;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.network.Network;
import com.cloud.network.Networks;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.dao.TungstenProviderDao;
import com.cloud.network.guru.GuestNetworkGuru;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.user.Account;
import com.cloud.utils.TungstenUtils;
import com.cloud.utils.db.DB;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachineProfile;
import net.juniper.contrail.api.types.InstanceIp;
import net.juniper.contrail.api.types.Project;
import net.juniper.contrail.api.types.VirtualMachine;
import net.juniper.contrail.api.types.VirtualMachineInterface;
import net.juniper.contrail.api.types.VirtualNetwork;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenInstanceIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenVmCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenVmInterfaceCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenVmCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenVmInterfaceCommand;
import org.apache.cloudstack.network.tungsten.agent.api.TungstenAnswer;
import org.apache.cloudstack.network.tungsten.vrouter.Port;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;

@Component
public class TungstenGuestNetworkGuru extends GuestNetworkGuru {

    private static final Logger s_logger = Logger.getLogger(TungstenGuestNetworkGuru.class);

    @Inject
    TungstenService tungstenService;
    @Inject
    NetworkOfferingServiceMapDao ntwkOfferingSrvcDao;
    @Inject
    AgentManager _agentMgr;
    @Inject
    TungstenProviderDao _tungstenProviderDao;
    @Inject
    NetworkDao _networkDao;
    @Inject
    DataCenterDao dcDao;

    private static final Networks.TrafficType[] TrafficTypes = {Networks.TrafficType.Guest};
    private TungstenFabricUtils _tunstenFabricUtils = null;

    public TungstenGuestNetworkGuru() {
        _isolationMethods = new PhysicalNetwork.IsolationMethod[]{new PhysicalNetwork.IsolationMethod("TF")};
    }

    @Override
    public boolean isMyTrafficType(Networks.TrafficType type) {
        for (Networks.TrafficType t : TrafficTypes) {
            if (t == type) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean canHandle(NetworkOffering offering, DataCenter.NetworkType networkType,
        PhysicalNetwork physicalNetwork) {
        if (networkType == DataCenter.NetworkType.Advanced && isMyTrafficType(offering.getTrafficType())
            && offering.getGuestType() == Network.GuestType.Isolated && isMyIsolationMethod(physicalNetwork)
            && ntwkOfferingSrvcDao.isProviderForNetworkOffering(offering.getId(), Network.Provider.Tungsten)) {
            return true;
        }

        return false;
    }

    @Override
    public Network design(NetworkOffering offering, DeploymentPlan plan, Network userSpecified, Account owner) {

        PhysicalNetworkVO physnet = _physicalNetworkDao.findById(plan.getPhysicalNetworkId());
        DataCenter dc = _dcDao.findById(plan.getDataCenterId());

        if (!canHandle(offering, dc.getNetworkType(), physnet)) {
            s_logger.debug("Refusing to design this network");
            return null;
        }

        NetworkVO network = (NetworkVO) super.design(offering, plan, userSpecified, owner);

        if (network == null) {
            return null;
        }

        network.setBroadcastDomainType(Networks.BroadcastDomainType.Tungsten);
        return network;
    }

    @Override
    public NicProfile allocate(Network config, NicProfile nic, VirtualMachineProfile vm)
        throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException {

        if (_networkModel.networkIsConfiguredForExternalNetworking(config.getDataCenterId(), config.getId())
            && nic != null && nic.getRequestedIPv4() != null) {
            throw new CloudRuntimeException("Does not support custom ip allocation at this time: " + nic);
        }

        NicProfile profile = super.allocate(config, nic, vm);

        if (_networkModel.networkIsConfiguredForExternalNetworking(config.getDataCenterId(), config.getId())) {
            profile.setReservationStrategy(Nic.ReservationStrategy.Start);
            /* We won't clear IP address, because router may set gateway as it IP, and it would be updated properly
            later */
            //profile.setIp4Address(null);
            profile.setIPv4Gateway(null);
            profile.setIPv4Netmask(null);
        }

        return profile;
    }

    @Override
    @DB
    public void deallocate(Network config, NicProfile nic, VirtualMachineProfile vm) {
        super.deallocate(config, nic, vm);

        tungstenFabricUtilsInit();

        if (_networkModel.networkIsConfiguredForExternalNetworking(config.getDataCenterId(), config.getId())) {
            nic.setIPv4Address(null);
            nic.setIPv4Gateway(null);
            nic.setIPv4Netmask(null);
            nic.setBroadcastUri(null);
            nic.setIsolationUri(null);
        }

        try {
            DeleteTungstenVmCommand cmd = new DeleteTungstenVmCommand(vm.getUuid(), tungstenService);
            _tunstenFabricUtils.sendTungstenCommand(cmd, config);
        } catch (IllegalArgumentException e) {
            throw new CloudRuntimeException("Failing to expuge the vm from tungsten with the uuid " + vm.getUuid());
        }
    }

    @Override
    public Network implement(Network network, NetworkOffering offering, DeployDestination dest,
        ReservationContext context) throws InsufficientVirtualNetworkCapacityException {

        assert (network.getState() == Network.State.Implementing) : "Why are we implementing " + network;

        tungstenFabricUtilsInit();

        // get physical network id
        Long physicalNetworkId = network.getPhysicalNetworkId();

        NetworkVO implemented = new NetworkVO(network.getTrafficType(), network.getMode(),
            network.getBroadcastDomainType(), network.getNetworkOfferingId(), Network.State.Allocated,
            network.getDataCenterId(), physicalNetworkId, offering.isRedundantRouter());

        if (network.getGateway() != null) {
            implemented.setGateway(network.getGateway());
        }

        if (network.getCidr() != null) {
            implemented.setCidr(network.getCidr());
        }

        implemented.setBroadcastUri(Networks.BroadcastDomainType.Tungsten.toUri("tf"));

        // setup tungsten network
        try {
            VirtualNetwork virtualNetwork = tungstenService.getVirtualNetworkFromTungsten(network.getUuid());
            if (virtualNetwork == null) {
                CreateTungstenNetworkCommand cmd = new CreateTungstenNetworkCommand(network.getId(), _networkDao,
                        tungstenService, dcDao);
                _tunstenFabricUtils.sendTungstenCommand(cmd, network);
            }

        } catch (Exception ex) {
            throw new CloudRuntimeException("unable to create tungsten network " + network.getUuid());
        }

        return implemented;
    }

    @Override
    public void reserve(final NicProfile nic, final Network network, final VirtualMachineProfile vm,
        final DeployDestination dest, final ReservationContext context)
        throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException {
        super.reserve(nic, network, vm, dest, context);

        tungstenFabricUtilsInit();

        // create tungsten vm, vmi, instance ip
        VirtualMachine virtualMachine;
        VirtualMachineInterface virtualMachineInterface;
        InstanceIp instanceIp;
        VirtualNetwork virtualNetwork;
        try {
            // for sure tungsten network is created
            virtualNetwork = tungstenService.getVirtualNetworkFromTungsten(network.getUuid());
            if (virtualNetwork == null) {
                throw new CloudRuntimeException("Tungsten network " + network.getUuid() + " is unavailable");
            }

            Project project = tungstenService.getTungstenNetworkProject(network.getAccountId(), network.getDomainId());

            virtualMachine = (VirtualMachine) tungstenService.getObject(VirtualMachine.class, vm.getUuid());
            if (virtualMachine == null) {
                CreateTungstenVmCommand vmCmd = new CreateTungstenVmCommand(vm.getUuid(), vm.getInstanceName(), tungstenService);
                virtualMachine = (VirtualMachine) _tunstenFabricUtils.sendTungstenCommand(vmCmd, network).getApiObjectBase();
            }

            virtualMachineInterface = (VirtualMachineInterface) tungstenService.getObject(VirtualMachineInterface.class,
                nic.getUuid());
            if (virtualMachineInterface == null) {
                CreateTungstenVmInterfaceCommand vmiCmd = new CreateTungstenVmInterfaceCommand(nic, virtualNetwork.getUuid(),
                        virtualMachine.getUuid(), project.getUuid(), tungstenService);
                virtualMachineInterface = (VirtualMachineInterface) _tunstenFabricUtils.sendTungstenCommand(vmiCmd, network).getApiObjectBase();
            }

            CreateTungstenInstanceIpCommand instanceIpCmd = new CreateTungstenInstanceIpCommand(nic, virtualNetwork.getUuid(),
                    virtualMachineInterface.getUuid(), tungstenService);
            instanceIp = (InstanceIp) _tunstenFabricUtils.sendTungstenCommand(instanceIpCmd, network).getApiObjectBase();

            Port port = new Port();
            port.setId(virtualMachineInterface.getUuid());
            port.setVnId(virtualNetwork.getUuid());
            port.setDisplayName(virtualMachine.getName());
            port.setVmProjectId(project.getUuid());
            port.setMacAddress(nic.getMacAddress());
            port.setIpAddress(instanceIp.getAddress());
            port.setInstanceId(virtualMachine.getUuid());
            tungstenService.addTungstenVrouterPort(port);

            nic.setName(nic.getName() + TungstenUtils.getBridgeName());
        } catch (IOException e) {
            throw new CloudRuntimeException(
                "Failed to create resources in tungsten for the network with uuid: " + vm.getUuid());
        }
    }

    @Override
    public boolean release(final NicProfile nic, final VirtualMachineProfile vm, final String reservationId) {

        tungstenFabricUtilsInit();
        // delete instance ip and vmi
        Network network = _networkDao.findById(nic.getNetworkId());
        DeleteTungstenVmInterfaceCommand cmd = new DeleteTungstenVmInterfaceCommand(nic.getUuid(), tungstenService);
        TungstenAnswer tungstenAnswer = _tunstenFabricUtils.sendTungstenCommand(cmd, network);
        if (tungstenAnswer.getResult())
            return super.release(nic, vm, reservationId);
        else
            return false;
    }

    @Override
    public boolean trash(Network network, NetworkOffering offering) {
        tungstenFabricUtilsInit();
        try {
            DeleteTungstenNetworkCommand cmd = new DeleteTungstenNetworkCommand(network.getUuid(), tungstenService);
            _tunstenFabricUtils.sendTungstenCommand(cmd, network);
        } catch (Exception e) {
            return false;
        }
        return super.trash(network, offering);
    }

    private void tungstenFabricUtilsInit() {
        if (_tunstenFabricUtils == null) {
            _tunstenFabricUtils = new TungstenFabricUtils(_agentMgr, _tungstenProviderDao);
            tungstenService.init();
        }
    }
}
