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

package org.apache.cloudstack.network.contrail.management;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.cloudstack.network.contrail.model.InstanceIpModel;
import org.apache.cloudstack.network.contrail.model.VMInterfaceModel;
import org.apache.cloudstack.network.contrail.model.VirtualMachineModel;
import org.apache.cloudstack.network.contrail.model.VirtualNetworkModel;
import org.apache.log4j.Logger;

import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.Network.State;
import com.cloud.network.NetworkProfile;
import com.cloud.network.Networks.AddressFormat;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.Mode;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.guru.NetworkGuru;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.Nic.ReservationStrategy;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.NicDao;

import net.juniper.contrail.api.types.MacAddressesType;
import net.juniper.contrail.api.types.VirtualMachineInterface;

@Local(value = {NetworkGuru.class})
public class ContrailGuru extends AdapterBase implements NetworkGuru {
    @Inject
    NetworkDao _networkDao;
    @Inject
    ContrailManager _manager;
    @Inject
    NicDao _nicDao;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    AccountManager _accountMgr;
    @Inject
    IpAddressManager _ipAddrMgr;
    @Inject
    PhysicalNetworkDao _physicalNetworkDao;
    @Inject
    DataCenterDao _dcDao;

    private static final Logger s_logger = Logger.getLogger(ContrailGuru.class);
    private static final TrafficType[] TrafficTypes = {TrafficType.Guest};

    private boolean canHandle(NetworkOffering offering, NetworkType networkType, PhysicalNetwork physicalNetwork) {
        if (physicalNetwork == null) {
            // Physical network can be false for system network during initial setup of CloudStack
            return false;
        }

        if (_manager.getRouterOffering() == null || _manager.getVpcRouterOffering() == null) {
            // FIXME The resource is apparently not configured, we need another way to check this.
            return false;
        }

        if (networkType == NetworkType.Advanced
                && (offering.getId() == _manager.getRouterOffering().getId() || offering.getId() == _manager.getVpcRouterOffering().getId())
                && isMyTrafficType(offering.getTrafficType())
                && offering.getGuestType() == Network.GuestType.Isolated
                && physicalNetwork.getIsolationMethods().contains("L3VPN"))
            return true;

        return false;
    }

    @Override
    public String getName() {
        return "ContrailGuru";
    }

    @Override
    public Network design(NetworkOffering offering, DeploymentPlan plan, Network userSpecified, Account owner) {
        // Check of the isolation type of the related physical network is L3VPN
        PhysicalNetworkVO physnet = _physicalNetworkDao.findById(plan.getPhysicalNetworkId());
        DataCenter dc = _dcDao.findById(plan.getDataCenterId());
        if (!canHandle(offering, dc.getNetworkType(),physnet)) {
            s_logger.debug("Refusing to design this network");
            return null;
        }
        NetworkVO network =
                new NetworkVO(offering.getTrafficType(), Mode.Dhcp, BroadcastDomainType.Lswitch, offering.getId(), State.Allocated, plan.getDataCenterId(),
                        plan.getPhysicalNetworkId(), offering.getRedundantRouter());
        if (userSpecified.getCidr() != null) {
            network.setCidr(userSpecified.getCidr());
            network.setGateway(userSpecified.getGateway());
        }
        s_logger.debug("Allocated network " + userSpecified.getName() + (network.getCidr() == null ? "" : " subnet: " + network.getCidr()));
        return network;
    }

    @Override
    public Network implement(Network network, NetworkOffering offering, DeployDestination destination, ReservationContext context)
            throws InsufficientVirtualNetworkCapacityException {
        s_logger.debug("Implement network: " + network.getName() + ", traffic type: " + network.getTrafficType());

        VirtualNetworkModel vnModel = _manager.getDatabase().lookupVirtualNetwork(network.getUuid(), _manager.getCanonicalName(network), network.getTrafficType());
        if (vnModel == null) {
            vnModel = new VirtualNetworkModel(network, network.getUuid(), _manager.getCanonicalName(network), network.getTrafficType());
            vnModel.setProperties(_manager.getModelController(), network);
        }

        try {
            if (!vnModel.verify(_manager.getModelController())) {
                vnModel.update(_manager.getModelController());
            }
        } catch (Exception ex) {
            s_logger.warn("virtual-network update: ", ex);
            return network;
        }
        _manager.getDatabase().getVirtualNetworks().add(vnModel);

        if (network.getVpcId() != null) {
            List<IPAddressVO> ips = _ipAddressDao.listByAssociatedVpc(network.getVpcId(), true);
            if (ips.isEmpty()) {
                s_logger.debug("Creating a source nat ip for network " + network);
                Account owner = _accountMgr.getAccount(network.getAccountId());
                try {
                    PublicIp publicIp = _ipAddrMgr.assignSourceNatIpAddressToGuestNetwork(owner, network);
                    IPAddressVO ip = publicIp.ip();
                    ip.setVpcId(network.getVpcId());
                    _ipAddressDao.acquireInLockTable(ip.getId());
                    _ipAddressDao.update(ip.getId(), ip);
                    _ipAddressDao.releaseFromLockTable(ip.getId());
                } catch (Exception e) {
                    s_logger.error("Unable to allocate source nat ip: " + e);
                }
            }
        }

        return network;
    }

    /**
     * Allocate the NicProfile object.
     * At this point the UUID of the nic is not yet known. We defer allocating the VMI and instance-ip objects
     * until the reserve API is called because of this reason.
     */
    @Override
    public NicProfile allocate(Network network, NicProfile profile, VirtualMachineProfile vm) throws InsufficientVirtualNetworkCapacityException,
    InsufficientAddressCapacityException, ConcurrentOperationException {
        s_logger.debug("allocate NicProfile on " + network.getName());

        if (profile != null && profile.getRequestedIPv4() != null) {
            throw new CloudRuntimeException("Does not support custom ip allocation at this time: " + profile);
        }
        if (profile == null) {
            profile = new NicProfile(ReservationStrategy.Create, null, null, null, null);
        }

        profile.setReservationStrategy(ReservationStrategy.Start);
        URI broadcastUri = null;
        try {
            broadcastUri = new URI("vlan://untagged");
        } catch (Exception e) {
            s_logger.warn("unable to instantiate broadcast URI: " + e);
        }
        profile.setBroadcastUri(broadcastUri);

        return profile;
    }

    /**
     * Allocate the ip address (and mac) for the specified VM device.
     */
    @Override
    public void reserve(NicProfile nic, Network network, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context)
            throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException, ConcurrentOperationException {
        s_logger.debug("reserve NicProfile on network id: " + network.getId() + " " + network.getName());
        s_logger.debug("deviceId: " + nic.getDeviceId());

        NicVO nicVO = _nicDao.findById(nic.getId());
        assert nicVO != null;

        VirtualNetworkModel vnModel = _manager.getDatabase().lookupVirtualNetwork(network.getUuid(), _manager.getCanonicalName(network), network.getTrafficType());
        /* Network must have been implemented */
        assert vnModel != null;

        VirtualMachineModel vmModel = _manager.getDatabase().lookupVirtualMachine(vm.getUuid());
        if (vmModel == null) {
            VMInstanceVO vmVo = (VMInstanceVO)vm.getVirtualMachine();
            vmModel = new VirtualMachineModel(vmVo, vm.getUuid());
            vmModel.setProperties(_manager.getModelController(), vmVo);
        }

        VMInterfaceModel vmiModel = vmModel.getVMInterface(nicVO.getUuid());
        if (vmiModel == null) {
            vmiModel = new VMInterfaceModel(nicVO.getUuid());
            vmiModel.addToVirtualMachine(vmModel);
            vmiModel.addToVirtualNetwork(vnModel);
        }
        try {
            vmiModel.build(_manager.getModelController(), (VMInstanceVO)vm.getVirtualMachine(), nicVO);
            vmiModel.setActive();
        } catch (IOException ex) {
            s_logger.error("virtual-machine-interface set", ex);
            return;
        }

        InstanceIpModel ipModel = vmiModel.getInstanceIp();
        if (ipModel == null) {
            ipModel = new InstanceIpModel(vm.getInstanceName(), nic.getDeviceId());
            ipModel.addToVMInterface(vmiModel);
        } else {
            s_logger.debug("Reuse existing instance-ip object on " + ipModel.getName());
        }
        if (nic.getIPv4Address() != null) {
            s_logger.debug("Nic using existing IP address " + nic.getIPv4Address());
            ipModel.setAddress(nic.getIPv4Address());
        }

        try {
            vmModel.update(_manager.getModelController());
        } catch (Exception ex) {
            s_logger.warn("virtual-machine update", ex);
            return;
        }

        _manager.getDatabase().getVirtualMachines().add(vmModel);

        VirtualMachineInterface vmi = vmiModel.getVMInterface();
        // allocate mac address
        if (nic.getMacAddress() == null) {
            MacAddressesType macs = vmi.getMacAddresses();
            if (macs == null) {
                s_logger.debug("no mac address is allocated for Nic " + nicVO.getUuid());
            } else {
                s_logger.info("VMI " + _manager.getVifNameByVmUuid(vm.getUuid(), nicVO.getDeviceId()) + " got mac address: " + macs.getMacAddress().get(0));
                nic.setMacAddress(macs.getMacAddress().get(0));
            }
        }

        if (nic.getIPv4Address() == null) {
            s_logger.debug("Allocated IP address " + ipModel.getAddress());
            nic.setIPv4Address(ipModel.getAddress());
            if (network.getCidr() != null) {
                nic.setIPv4Netmask(NetUtils.cidr2Netmask(network.getCidr()));
            }
            nic.setIPv4Gateway(network.getGateway());
            nic.setFormat(AddressFormat.Ip4);
        }
    }

    /**
     * When a VM is stopped this API is called to release transient resources.
     */
    @Override
    public boolean release(NicProfile nic, VirtualMachineProfile vm, String reservationId) {

        s_logger.debug("release NicProfile " + nic.getId());

        return true;
    }

    /**
     * Release permanent resources of a Nic (VMI and addresses).
     */
    @Override
    public void deallocate(Network network, NicProfile nic, VirtualMachineProfile vm) {
        s_logger.debug("deallocate NicProfile " + nic.getId() + " on " + network.getName());
        NicVO nicVO = _nicDao.findById(nic.getId());
        assert nicVO != null;

        VirtualMachineModel vmModel = _manager.getDatabase().lookupVirtualMachine(vm.getUuid());
        if (vmModel == null) {
            return;
        }
        VMInterfaceModel vmiModel = vmModel.getVMInterface(nicVO.getUuid());
        if (vmiModel == null) {
            return;
        }
        try {
            vmiModel.destroy(_manager.getModelController());
        } catch (IOException ex) {
            return;
        }
        vmModel.removeSuccessor(vmiModel);

        if (!vmModel.hasDescendents()) {
            _manager.getDatabase().getVirtualMachines().remove(vmModel);
            try {
                vmModel.delete(_manager.getModelController());
            } catch (IOException ex) {
                s_logger.warn("virtual-machine delete", ex);
                return;
            }
        }

    }

    @Override
    public void updateNicProfile(NicProfile profile, Network network) {
        // TODO Auto-generated method stub
        s_logger.debug("update NicProfile " + profile.getId() + " on " + network.getName());
    }

    @Override
    public void shutdown(NetworkProfile network, NetworkOffering offering) {
        s_logger.debug("NetworkGuru shutdown");
        VirtualNetworkModel vnModel = _manager.getDatabase().lookupVirtualNetwork(network.getUuid(), _manager.getCanonicalName(network), network.getTrafficType());
        if (vnModel == null) {
            return;
        }
        try {
            _manager.getDatabase().getVirtualNetworks().remove(vnModel);
            vnModel.delete(_manager.getModelController());
        } catch (IOException e) {
            s_logger.warn("virtual-network delete", e);
        }
    }

    @Override
    public boolean trash(Network network, NetworkOffering offering) {
        // TODO Auto-generated method stub
        s_logger.debug("NetworkGuru trash");
        return true;
    }

    @Override
    public void updateNetworkProfile(NetworkProfile networkProfile) {
        // TODO Auto-generated method stub
        s_logger.debug("NetworkGuru updateNetworkProfile");
    }

    @Override
    public TrafficType[] getSupportedTrafficType() {
        return TrafficTypes;
    }

    @Override
    public boolean isMyTrafficType(TrafficType type) {
        for (TrafficType t : TrafficTypes) {
            if (t == type) {
                return true;
            }
        }
        return false;
    }

}
