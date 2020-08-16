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
import com.cloud.network.guru.NetworkGuruTungsten;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.utils.db.DB;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachineProfile;
import net.juniper.contrail.api.types.VirtualMachine;
import net.juniper.contrail.api.types.VirtualMachineInterface;
import net.juniper.contrail.api.types.VirtualNetwork;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class TungstenGuestNetworkGuru extends GuestNetworkGuru implements NetworkGuruTungsten {

    private static final Logger s_logger = Logger
            .getLogger(TungstenGuestNetworkGuru.class);

    @Inject
    TungstenService tungstenService;

    @Override
    protected boolean canHandle(NetworkOffering offering, DataCenter.NetworkType networkType, PhysicalNetwork physicalNetwork) {
        return offering.isForTungsten();
    }

    @Override
    public Network design(NetworkOffering offering, DeploymentPlan plan, Network userSpecified, Account owner) {

        PhysicalNetworkVO physnet = _physicalNetworkDao.findById(plan.getPhysicalNetworkId());
        DataCenter dc = _dcDao.findById(plan.getDataCenterId());

        if(!canHandle(offering, dc.getNetworkType(),physnet)){
            s_logger.debug("Refusing to design this network");
            return null;
        }

        NetworkVO network = (NetworkVO)super.design(offering, plan, userSpecified, owner);

        return network;
    }

    @Override
    public NicProfile allocate(Network config, NicProfile nic, VirtualMachineProfile vm) throws InsufficientVirtualNetworkCapacityException,
            InsufficientAddressCapacityException {

        if (_networkModel.networkIsConfiguredForExternalNetworking(config.getDataCenterId(), config.getId()) && nic != null && nic.getRequestedIPv4() != null) {
            throw new CloudRuntimeException("Does not support custom ip allocation at this time: " + nic);
        }

        NicProfile profile = super.allocate(config, nic, vm);

        if (_networkModel.networkIsConfiguredForExternalNetworking(config.getDataCenterId(), config.getId())) {
            profile.setReservationStrategy(Nic.ReservationStrategy.Start);
            /* We won't clear IP address, because router may set gateway as it IP, and it would be updated properly later */
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
            tungstenService.expugeVmFromTungsten(vm.getUuid());
        } catch (IOException e) {
            throw new CloudRuntimeException("Failing to expuge the vm from tungsten with the uuid " + vm.getUuid());
        }
    }

    @Override
    public Network implement(Network network, NetworkOffering offering, DeployDestination dest, ReservationContext context) throws InsufficientVirtualNetworkCapacityException {

        assert (network.getState() == Network.State.Implementing) : "Why are we implementing "
                + network;

        long dcId = dest.getDataCenter().getId();
        DataCenter.NetworkType nwType = dest.getDataCenter().getNetworkType();
        // get physical network id
        Long physicalNetworkId = network.getPhysicalNetworkId();
        // physical network id can be null in Guest Network in Basic zone, so
        // locate the physical network
        if (physicalNetworkId == null) {
            physicalNetworkId = _networkModel.findPhysicalNetworkId(dcId,
                    offering.getTags(), offering.getTrafficType());
        }
        PhysicalNetworkVO physnet = _physicalNetworkDao
                .findById(physicalNetworkId);

        if (!canHandle(offering, nwType, physnet)) {
            s_logger.debug("Refusing to implement this network");
            return null;
        }
        NetworkVO implemented = (NetworkVO)super.implement(network, offering,
                dest, context);

        if (network.getGateway() != null) {
            implemented.setGateway(network.getGateway());
        }

        if (network.getCidr() != null) {
            implemented.setCidr(network.getCidr());
        }

        implemented.setBroadcastDomainType(Networks.BroadcastDomainType.Vxlan);

        return implemented;
    }

    @Override
    public boolean trash(Network network, NetworkOffering offering) {
        try {
            tungstenService.deleteNetworkFromTungsten(network.getTungstenNetworkUuid());
        } catch (IOException e) {
            return false;
        }
        return super.trash(network, offering);
    }

    @Override
    public Network createNetworkInTungsten(Network networkVO) {
        List<String> subnetIp = Arrays.asList(networkVO.getCidr().split("/"));
        String subnetIpPrefix = subnetIp.get(0);
        int subnetIpPrefixLength = Integer.parseInt(subnetIp.get(1));
        boolean isDhcpEnabled = networkVO.getMode().equals(Networks.Mode.Dhcp);
        VirtualNetwork virtualNetwork = tungstenService.createNetworkInTungsten(null, networkVO.getName(), null,
                null, null, subnetIpPrefix, subnetIpPrefixLength, networkVO.getGateway(), isDhcpEnabled, null, false);
        networkVO.setTungstenNetworkUuid(virtualNetwork.getUuid());
        return networkVO;
    }

    @Override
    public String createVirtualMachineInTungsten(String virtualMachineUuid, String virtualMachineName) {
        VirtualMachine virtualMachine = tungstenService.createVmInTungsten(virtualMachineUuid, virtualMachineName);
        return virtualMachine.getUuid();
    }

    @Override
    public String createVmInterfaceInTungsten(String vmInterfaceName, String tungstenProjectUuid, String tungstenNetworkUuid, String tungstenVirtualMachineUuid, String tungstenSecurityGroupUuid, List<String> tungstenVmInterfaceMacAddresses) {
        VirtualMachineInterface virtualMachineInterface = tungstenService.createVmInterfaceInTungsten(vmInterfaceName + "-Interface", tungstenProjectUuid, tungstenNetworkUuid, tungstenVirtualMachineUuid, tungstenSecurityGroupUuid, tungstenVmInterfaceMacAddresses);
        return virtualMachineInterface.getUuid();
    }

    @Override
    public void createTungstenInstanceIp(String instanceIpName, String tungstenVmInterfaceUuid, String tungstenNetworkUuid, String tungstenInstanceIpAddress) {
        tungstenService.createInstanceIpInTungsten(instanceIpName, tungstenVmInterfaceUuid, tungstenNetworkUuid, tungstenInstanceIpAddress);
    }
}
