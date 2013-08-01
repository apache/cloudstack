/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.cloud.network.guru;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.CreateVnsNetworkAnswer;
import com.cloud.agent.api.CreateVnsNetworkCommand;
import com.cloud.agent.api.DeleteVnsNetworkCommand;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.network.BigSwitchVnsDeviceVO;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.State;
import com.cloud.network.NetworkProfile;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetwork.IsolationMethod;
import com.cloud.network.dao.BigSwitchVnsDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.resource.ResourceManager;
import com.cloud.user.Account;
import com.cloud.user.dao.AccountDao;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachineProfile;


@Local(value = NetworkGuru.class)
public class BigSwitchVnsGuestNetworkGuru extends GuestNetworkGuru {
    private static final Logger s_logger = Logger.getLogger(BigSwitchVnsGuestNetworkGuru.class);

    @Inject
    DataCenterDao _zoneDao;
    @Inject
    PhysicalNetworkDao _physicalNetworkDao;
    @Inject
    AccountDao _accountDao;
    @Inject
    BigSwitchVnsDao _bigswitchVnsDao;
    @Inject
    HostDao _hostDao;
    @Inject
    ResourceManager _resourceMgr;
    @Inject
    AgentManager _agentMgr;
    @Inject
    HostDetailsDao _hostDetailsDao;

    public BigSwitchVnsGuestNetworkGuru() {
        super();
        _isolationMethods = new IsolationMethod[] { IsolationMethod.VNS };
    }

    @Override
    protected boolean canHandle(NetworkOffering offering, NetworkType networkType,
                                PhysicalNetwork physicalNetwork) {
        if (networkType == NetworkType.Advanced
                && isMyTrafficType(offering.getTrafficType())
                && offering.getGuestType() == Network.GuestType.Isolated
                && isMyIsolationMethod(physicalNetwork)) {
            return true;
        } else {
            s_logger.trace("We only take care of Guest networks of type   " + GuestType.Isolated +
                        " in zone of type " + NetworkType.Advanced);
            return false;
        }
    }

    @Override
    public Network design(NetworkOffering offering, DeploymentPlan plan,
            Network userSpecified, Account owner) {
         // Check of the isolation type of the related physical network is VNS
        PhysicalNetworkVO physnet = _physicalNetworkDao.findById(plan.getPhysicalNetworkId());
        if (physnet == null ||
                        physnet.getIsolationMethods() == null ||
                        !physnet.getIsolationMethods().contains("VNS")) {
            s_logger.debug("Refusing to design this network, the physical isolation type is not VNS");
            return null;
        }

        List<BigSwitchVnsDeviceVO> devices = _bigswitchVnsDao.listByPhysicalNetwork(physnet.getId());
        if (devices.isEmpty()) {
            s_logger.error("No BigSwitxh Controller on physical network " + physnet.getName());
            return null;
        }
        s_logger.debug("BigSwitch Controller " + devices.get(0).getUuid() +
                        " found on physical network " + physnet.getId());

        s_logger.debug("Physical isolation type is VNS, asking GuestNetworkGuru to design this network");
        NetworkVO networkObject = (NetworkVO) super.design(offering, plan, userSpecified, owner);
        if (networkObject == null) {
            return null;
        }
        // Override the broadcast domain type
        networkObject.setBroadcastDomainType(BroadcastDomainType.Lswitch);

        return networkObject;
    }

    @Override
    public Network implement(Network network, NetworkOffering offering,
            DeployDestination dest, ReservationContext context)
            throws InsufficientVirtualNetworkCapcityException {
        assert (network.getState() == State.Implementing) : "Why are we implementing " + network;

        long dcId = dest.getDataCenter().getId();

        //get physical network id
        long physicalNetworkId = _networkModel.findPhysicalNetworkId(dcId,
                                                        offering.getTags(),
                                                        offering.getTrafficType());

        NetworkVO implemented = new NetworkVO(network.getTrafficType(), network.getMode(),
                        network.getBroadcastDomainType(), network.getNetworkOfferingId(), State.Allocated,
                network.getDataCenterId(), physicalNetworkId);

        if (network.getGateway() != null) {
            implemented.setGateway(network.getGateway());
        }

        if (network.getCidr() != null) {
            implemented.setCidr(network.getCidr());
        }

        String vnet = _dcDao.allocateVnet(dcId, physicalNetworkId, network.getAccountId(), context.getReservationId(), UseSystemGuestVlans.valueIn(network.getAccountId()));
        if (vnet == null) {
            throw new InsufficientVirtualNetworkCapcityException("Unable to allocate vnet as a " +
                        "part of network " + network + " implement ", DataCenter.class, dcId);
        }
        // when supporting more types of networks this need to become
//        int vlan = Integer.parseInt(BroadcastDomainType.getValue(vnet));
        int vlan = Integer.parseInt(vnet);

        // Name is either the given name or the uuid
        String name = network.getName();
        String networkUuid = implemented.getUuid();
        if (name == null || name.isEmpty()) {
            name = ((NetworkVO)network).getUuid();
        }
        if (name.length() > 64 ) {
            name = name.substring(0, 63); // max length 64
        }

        String tenantId = context.getDomain().getName();
        List<BigSwitchVnsDeviceVO> devices = _bigswitchVnsDao.listByPhysicalNetwork(physicalNetworkId);
        if (devices.isEmpty()) {
            s_logger.error("No BigSwitch Controller on physical network " + physicalNetworkId);
            return null;
        }
        BigSwitchVnsDeviceVO bigswitchVnsDevice = devices.get(0);
        HostVO bigswitchVnsHost = _hostDao.findById(bigswitchVnsDevice.getHostId());
        _hostDao.loadDetails(bigswitchVnsHost);

        CreateVnsNetworkCommand cmd = new CreateVnsNetworkCommand(networkUuid, name, tenantId, vlan);
        CreateVnsNetworkAnswer answer = (CreateVnsNetworkAnswer) _agentMgr.easySend(bigswitchVnsHost.getId(), cmd);

        if (answer == null || !answer.getResult()) {
            s_logger.error ("CreateNetworkCommand failed");
            return null;
        }

        try {
            implemented.setBroadcastUri(new URI("vns", cmd.getNetworkUuid(), null));
            implemented.setBroadcastDomainType(BroadcastDomainType.Lswitch);
            s_logger.info("Implemented OK, network " + networkUuid + " in tenant " +
                        tenantId + " linked to " + implemented.getBroadcastUri().toString());
        } catch (URISyntaxException e) {
            s_logger.error("Unable to store network id in broadcast uri, uuid = " + implemented.getUuid(), e);
        }

        return implemented;
    }

    @Override
    public void reserve(NicProfile nic, Network network,
            VirtualMachineProfile vm,
            DeployDestination dest, ReservationContext context)
            throws InsufficientVirtualNetworkCapcityException,
            InsufficientAddressCapacityException {
        // TODO Auto-generated method stub
        super.reserve(nic, network, vm, dest, context);
    }

    @Override
    public boolean release(NicProfile nic,
            VirtualMachineProfile vm,
            String reservationId) {
        // TODO Auto-generated method stub
        return super.release(nic, vm, reservationId);
    }

    @Override
    public void shutdown(NetworkProfile profile, NetworkOffering offering) {
        NetworkVO networkObject = _networkDao.findById(profile.getId());
        if (networkObject.getBroadcastDomainType() != BroadcastDomainType.Lswitch ||
                networkObject.getBroadcastUri() == null) {
            s_logger.warn("BroadcastUri is empty or incorrect for guestnetwork " + networkObject.getDisplayText());
            return;
        }

        List<BigSwitchVnsDeviceVO> devices = _bigswitchVnsDao.listByPhysicalNetwork(networkObject.getPhysicalNetworkId());
        if (devices.isEmpty()) {
            s_logger.error("No BigSwitch Controller on physical network " + networkObject.getPhysicalNetworkId());
            return;
        }
        BigSwitchVnsDeviceVO bigswitchVnsDevice = devices.get(0);
        HostVO bigswitchVnsHost = _hostDao.findById(bigswitchVnsDevice.getHostId());

        String tenantId = profile.getNetworkDomain();

        DeleteVnsNetworkCommand cmd = new DeleteVnsNetworkCommand(tenantId,
                BroadcastDomainType.getValue(networkObject.getBroadcastUri()));
        _agentMgr.easySend(bigswitchVnsHost.getId(), cmd);

        super.shutdown(profile, offering);
    }

    @Override
    public boolean trash(Network network, NetworkOffering offering) {
        return super.trash(network, offering);
    }
}
