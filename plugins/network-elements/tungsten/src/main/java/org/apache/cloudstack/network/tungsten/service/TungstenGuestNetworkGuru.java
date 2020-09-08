package org.apache.cloudstack.network.tungsten.service;

import com.cloud.dc.DataCenter;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.network.Network;
import com.cloud.network.Networks;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkVO;
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
import net.juniper.contrail.api.ApiPropertyBase;
import net.juniper.contrail.api.ObjectReference;
import net.juniper.contrail.api.types.InstanceIp;
import net.juniper.contrail.api.types.NetworkIpam;
import net.juniper.contrail.api.types.Project;
import net.juniper.contrail.api.types.VirtualMachine;
import net.juniper.contrail.api.types.VirtualMachineInterface;
import net.juniper.contrail.api.types.VirtualNetwork;
import net.juniper.contrail.api.types.VnSubnetsType;
import org.apache.cloudstack.network.tungsten.vrouter.Port;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

@Component
public class TungstenGuestNetworkGuru extends GuestNetworkGuru {

    private static final Logger s_logger = Logger.getLogger(TungstenGuestNetworkGuru.class);

    @Inject
    TungstenService tungstenService;
    @Inject
    protected NetworkOfferingServiceMapDao ntwkOfferingSrvcDao;

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

        if (_networkModel.networkIsConfiguredForExternalNetworking(config.getDataCenterId(), config.getId())) {
            nic.setIPv4Address(null);
            nic.setIPv4Gateway(null);
            nic.setIPv4Netmask(null);
            nic.setBroadcastUri(null);
            nic.setIsolationUri(null);
        }
        try {
            tungstenService.deleteObject(VirtualMachine.class, vm.getUuid());
        } catch (IOException e) {
            throw new CloudRuntimeException("Failing to expuge the vm from tungsten with the uuid " + vm.getUuid());
        }
    }

    @Override
    public Network implement(Network network, NetworkOffering offering, DeployDestination dest,
        ReservationContext context) throws InsufficientVirtualNetworkCapacityException {

        assert (network.getState() == Network.State.Implementing) : "Why are we implementing " + network;

        long dcId = dest.getDataCenter().getId();

        // get physical network id
        Long physicalNetworkId = network.getPhysicalNetworkId();

        PhysicalNetworkVO physnet = _physicalNetworkDao.findById(physicalNetworkId);

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
                Project project = tungstenService.getTungstenNetworkProject(context.getAccount());

                // we use default network ipam
                NetworkIpam networkIpam = tungstenService.getDefaultProjectNetworkIpam(project);

                // create tungsten subnet
                DataCenter dataCenter = _dcDao.findById(network.getDataCenterId());

                String cidr = network.getCidr();
                if (cidr == null) {
                    cidr = dataCenter.getGuestNetworkCidr();
                }
                String[] addr_pair = cidr.split("\\/");
                String gateway = network.getGateway();
                boolean isDhcpEnable = network.getMode().equals(Networks.Mode.Dhcp);

                VnSubnetsType subnet = tungstenService.getVnSubnetsType(null, null, addr_pair[0],
                    Integer.parseInt(addr_pair[1]), gateway, isDhcpEnable, null, true);

                tungstenService.createTungstenVirtualNetwork(network, project, networkIpam, subnet);
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

            Project project = tungstenService.getTungstenNetworkProject(context.getAccount());

            virtualMachine = (VirtualMachine) tungstenService.getObject(VirtualMachine.class, vm.getUuid());
            if (virtualMachine == null) {
                virtualMachine = tungstenService.createVmInTungsten(vm.getUuid(), vm.getInstanceName());
            }

            virtualMachineInterface = (VirtualMachineInterface) tungstenService.getObject(VirtualMachineInterface.class,
                nic.getUuid());
            if (virtualMachineInterface == null) {
                virtualMachineInterface = tungstenService.createVmInterfaceInTungsten(nic, virtualNetwork,
                    virtualMachine, project);
            }

            instanceIp = tungstenService.createInstanceIpInTungsten(virtualNetwork, virtualMachineInterface,
                nic);

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
        try {
            // delete instance ip and vmi
            VirtualMachineInterface vmi = (VirtualMachineInterface) tungstenService.getObject(
                VirtualMachineInterface.class, nic.getUuid());
            if (vmi != null) {
                List<ObjectReference<ApiPropertyBase>> instanceIpORs = vmi.getInstanceIpBackRefs();
                for (ObjectReference<ApiPropertyBase> instanceIpOR : instanceIpORs) {
                    tungstenService.deleteObject(InstanceIp.class, instanceIpOR.getUuid());
                }
                tungstenService.deleteObject(VirtualMachineInterface.class, vmi.getUuid());
                tungstenService.deleteTungstenVrouterPort(vmi.getUuid());
            }
        } catch (IOException ex) {
            return false;
        }
        return super.release(nic, vm, reservationId);
    }

    @Override
    public boolean trash(Network network, NetworkOffering offering) {
        try {
            tungstenService.deleteNetworkFromTungsten(network.getUuid());
        } catch (IOException e) {
            return false;
        }
        return super.trash(network, offering);
    }
}
