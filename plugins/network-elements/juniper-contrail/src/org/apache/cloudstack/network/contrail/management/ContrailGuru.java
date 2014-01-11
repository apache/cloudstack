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

import javax.inject.Inject;

import net.juniper.contrail.api.types.MacAddressesType;
import net.juniper.contrail.api.types.VirtualMachineInterface;

import org.apache.cloudstack.network.contrail.model.InstanceIpModel;
import org.apache.cloudstack.network.contrail.model.VMInterfaceModel;
import org.apache.cloudstack.network.contrail.model.VirtualMachineModel;
import org.apache.cloudstack.network.contrail.model.VirtualNetworkModel;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
import com.cloud.network.Network;
import com.cloud.network.Network.State;
import com.cloud.network.NetworkProfile;
import com.cloud.network.Networks.AddressFormat;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.Mode;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.guru.NetworkGuru;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.Nic.ReservationStrategy;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.NicVO;

@Component
public class ContrailGuru extends AdapterBase implements NetworkGuru {
    @Inject NetworkDao _networkDao;
    @Inject ContrailManager _manager;
    @Inject NicDao _nicDao;

    private static final Logger s_logger = Logger.getLogger(ContrailGuru.class);
    private static final TrafficType[] _trafficTypes = {TrafficType.Guest};

    private boolean canHandle(NetworkOffering offering) {
        return (offering.getName().equals(ContrailManager.offeringName));
    }

    @Override
    public String getName() {
	return "ContrailGuru";
    }

    @Override
    public Network design(NetworkOffering offering, DeploymentPlan plan,
            Network userSpecified, Account owner) {
        if (!canHandle(offering)) {
            return null;
        }
        NetworkVO network = new NetworkVO(offering.getTrafficType(), Mode.Dhcp, BroadcastDomainType.Lswitch,
                offering.getId(), State.Allocated, plan.getDataCenterId(), plan.getPhysicalNetworkId());
        if (userSpecified.getCidr() != null) {
            network.setCidr(userSpecified.getCidr());
            network.setGateway(userSpecified.getGateway());
        }
        s_logger.debug("Allocated network " + userSpecified.getName() +
                (network.getCidr() == null ? "" : " subnet: " + network.getCidr()));
        return network;
    }

    @Override
    public Network implement(Network network, NetworkOffering offering,
            DeployDestination destination, ReservationContext context)
                    throws InsufficientVirtualNetworkCapcityException {
        s_logger.debug("Implement network: " + network.getName() + ", traffic type: " + network.getTrafficType());

        VirtualNetworkModel vnModel = _manager.getDatabase().lookupVirtualNetwork(
                network.getUuid(), _manager.getCanonicalName(network), network.getTrafficType());
        if (vnModel == null) {
            vnModel = new VirtualNetworkModel(network, network.getUuid(), 
                    _manager.getCanonicalName(network), network.getTrafficType());
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
        return network;
    }

    /**
     * Allocate the NicProfile object.
     * At this point the UUID of the nic is not yet known. We defer allocating the VMI and instance-ip objects
     * until the reserve API is called because of this reason. 
     */
    @Override
    public NicProfile allocate(Network network, NicProfile profile,
            VirtualMachineProfile vm)
                    throws InsufficientVirtualNetworkCapcityException,
                    InsufficientAddressCapacityException, ConcurrentOperationException {
        s_logger.debug("allocate NicProfile on " + network.getName());

        if (profile != null && profile.getRequestedIpv4() != null) {
            throw new CloudRuntimeException("Does not support custom ip allocation at this time: " + profile);
        }
        if (profile == null) {
            profile = new NicProfile(ReservationStrategy.Create, null, null, null, null);
        }

        profile.setStrategy(ReservationStrategy.Start);
        
        return profile;
    }

    /**
     * Allocate the ip address (and mac) for the specified VM device.
     */
    @Override
    public void reserve(NicProfile nic, Network network,
            VirtualMachineProfile vm,
            DeployDestination dest, ReservationContext context)
                    throws InsufficientVirtualNetworkCapcityException,
                    InsufficientAddressCapacityException, ConcurrentOperationException {
        s_logger.debug("reserve NicProfile on network id: " + network.getId() +
                " " + network.getName());
        s_logger.debug("deviceId: " + nic.getDeviceId());

        NicVO nicVO = _nicDao.findById(nic.getId());
        assert nicVO != null;

        VirtualNetworkModel vnModel = _manager.getDatabase().lookupVirtualNetwork(
                network.getUuid(), _manager.getCanonicalName(network), network.getTrafficType());
        /* Network must have been implemented */
        assert vnModel != null;
        
        VirtualMachineModel vmModel = _manager.getDatabase().lookupVirtualMachine(vm.getUuid());
        if (vmModel == null) {
            VMInstanceVO vmVo = (VMInstanceVO) vm.getVirtualMachine();
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
            vmiModel.build(_manager.getModelController(), (VMInstanceVO) vm.getVirtualMachine(), nicVO);
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
        if (nic.getIp4Address() != null) {
            s_logger.debug("Nic using existing IP address " +  nic.getIp4Address());
            ipModel.setAddress(nic.getIp4Address());
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
                s_logger.info("VMI " + _manager.getVifNameByVmUuid(vm.getUuid(), nicVO.getDeviceId()) + " got mac address: " +
                        macs.getMacAddress().get(0));
                nic.setMacAddress(macs.getMacAddress().get(0));
            }
        }

        if (nic.getIp4Address() == null) {
            s_logger.debug("Allocated IP address " + ipModel.getAddress());
            nic.setIp4Address(ipModel.getAddress());
            nic.setNetmask(NetUtils.cidr2Netmask(network.getCidr()));
            nic.setGateway(network.getGateway());
            nic.setFormat(AddressFormat.Ip4);
        }
    }

    /**
     * When a VM is stopped this API is called to release transient resources.
     */
    @Override
    public boolean release(NicProfile nic,
            VirtualMachineProfile vm,
            String reservationId) {

        s_logger.debug("release NicProfile " + nic.getId());

        return true;
    }

    /**
     * Release permanent resources of a Nic (VMI and addresses).
     */
    @Override
    public void deallocate(Network network, NicProfile nic,
            VirtualMachineProfile vm) {
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
    public void shutdown(NetworkProfile network, NetworkOffering offering)  {
        s_logger.debug("NetworkGuru shutdown");
        VirtualNetworkModel vnModel = _manager.getDatabase().lookupVirtualNetwork(network.getUuid(), 
                _manager.getCanonicalName(network), network.getTrafficType());
        if (vnModel == null) {
            return;
        }
        try {
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
        return _trafficTypes;
    }

    @Override
    public boolean isMyTrafficType(TrafficType type) {
        for (TrafficType t : _trafficTypes) {
            if (t == type) {
                return true;
            }
        }
        return false;
    }

}
