package org.apache.cloudstack.network.tungsten.service;

import com.cloud.dc.DataCenter;
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
import com.cloud.network.guru.GuestNetworkGuru;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.projects.ProjectVO;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.user.Account;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.Pair;
import com.cloud.utils.TungstenUtils;
import com.cloud.utils.db.DB;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachineProfile;
import org.apache.cloudstack.network.tungsten.agent.api.ClearTungstenNetworkGatewayCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenVirtualMachineCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenVRouterPortCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenVmCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenVmInterfaceCommand;
import org.apache.cloudstack.network.tungsten.agent.api.SetTungstenNetworkGatewayCommand;
import org.apache.cloudstack.network.tungsten.agent.api.TungstenAnswer;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class TungstenGuestNetworkGuru extends GuestNetworkGuru {

    private static final Logger s_logger = Logger.getLogger(TungstenGuestNetworkGuru.class);

    @Inject
    NetworkOfferingServiceMapDao ntwkOfferingSrvcDao;
    @Inject
    NetworkDao _networkDao;
    @Inject
    TungstenFabricUtils _tunstenFabricUtils;
    @Inject
    AccountDao _accountDao;
    @Inject
    ProjectDao _projectDao;

    private static final Networks.TrafficType[] TrafficTypes = {Networks.TrafficType.Guest};

    public TungstenGuestNetworkGuru() {
        _isolationMethods = new PhysicalNetwork.IsolationMethod[] {new PhysicalNetwork.IsolationMethod("TF")};
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
        return super.allocate(config, nic, vm);
    }

    @Override
    @DB
    public void deallocate(Network config, NicProfile nic, VirtualMachineProfile vm) {
        super.deallocate(config, nic, vm);

        try {
            DeleteTungstenVmCommand cmd = new DeleteTungstenVmCommand(vm.getUuid());
            _tunstenFabricUtils.sendTungstenCommand(cmd, config);
        } catch (IllegalArgumentException e) {
            throw new CloudRuntimeException("Failing to expuge the vm from tungsten with the uuid " + vm.getUuid());
        }
    }

    @Override
    public Network implement(Network network, NetworkOffering offering, DeployDestination dest,
        ReservationContext context) throws InsufficientVirtualNetworkCapacityException {

        assert (network.getState() == Network.State.Implementing) : "Why are we implementing " + network;

        // get physical network id
        Long physicalNetworkId = network.getPhysicalNetworkId();
        Long zoneId = network.getDataCenterId();

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
            String projectUuid = getProject(context.getAccountId());

            // create tungsten network
            Pair<String, Integer> pair = NetUtils.getCidr(network.getCidr());
            CreateTungstenNetworkCommand createTungstenGuestNetworkCommand = new CreateTungstenNetworkCommand(
                network.getUuid(), network.getName(), projectUuid, false, false, pair.first(), pair.second(),
                network.getGateway(), network.getMode().equals(Networks.Mode.Dhcp), null, null, null, true);
            _tunstenFabricUtils.sendTungstenCommand(createTungstenGuestNetworkCommand, network);

            // create gateway vmi, update logical router
            SetTungstenNetworkGatewayCommand setTungstenNetworkGatewayCommand = new SetTungstenNetworkGatewayCommand(
                projectUuid, TungstenUtils.getLogicalRouterName(zoneId), network.getId(), network.getUuid(),
                network.getGateway());
            _tunstenFabricUtils.sendTungstenCommand(setTungstenNetworkGatewayCommand, network);

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

        String projectUuid = getProject(context.getAccountId());

        // create tungsten vm ( vmi - ii - port )
        CreateTungstenVirtualMachineCommand cmd = new CreateTungstenVirtualMachineCommand(projectUuid,
            network.getUuid(), vm.getUuid(), vm.getInstanceName(), nic.getUuid(), nic.getId(), nic.getIPv4Address(),
            nic.getMacAddress(), TungstenUtils.getUserVm(), TungstenUtils.getGuestType());
        _tunstenFabricUtils.sendTungstenCommand(cmd, network);
        nic.setName(nic.getName() + TungstenUtils.getBridgeName());
    }

    @Override
    public boolean release(final NicProfile nic, final VirtualMachineProfile vm, final String reservationId) {
        // delete vrouter port
        DeleteTungstenVRouterPortCommand deleteTungstenVRouterPortCommand = new DeleteTungstenVRouterPortCommand(
            nic.getUuid());

        // delete instance ip and vmi
        Network network = _networkDao.findById(nic.getNetworkId());
        String projectUuid = getProject(vm.getOwner().getAccountId());
        String nicName = TungstenUtils.getVmiName(TungstenUtils.getGuestType(), TungstenUtils.getUserVm(),
            vm.getInstanceName(), nic.getId());
        DeleteTungstenVmInterfaceCommand cmd = new DeleteTungstenVmInterfaceCommand(projectUuid, nicName);
        TungstenAnswer tungstenAnswer = _tunstenFabricUtils.sendTungstenCommand(cmd, network);
        if (tungstenAnswer.getResult())
            return super.release(nic, vm, reservationId);
        else
            return false;
    }

    @Override
    public boolean trash(Network network, NetworkOffering offering) {
        try {
            String projectUuid = getProject(network.getAccountId());
            ClearTungstenNetworkGatewayCommand clearTungstenNetworkGatewayCommand =
                new ClearTungstenNetworkGatewayCommand(
                projectUuid, TungstenUtils.getLogicalRouterName(network.getDataCenterId()), network.getId());
            _tunstenFabricUtils.sendTungstenCommand(clearTungstenNetworkGatewayCommand, network);
            DeleteTungstenNetworkCommand deleteTungstenNetworkCommand = new DeleteTungstenNetworkCommand(
                network.getUuid());
            _tunstenFabricUtils.sendTungstenCommand(deleteTungstenNetworkCommand, network);
        } catch (Exception e) {
            return false;
        }
        return super.trash(network, offering);
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
}
