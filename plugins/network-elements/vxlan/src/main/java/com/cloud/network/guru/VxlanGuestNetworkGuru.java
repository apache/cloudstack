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
package com.cloud.network.guru;


import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.State;
import com.cloud.network.NetworkProfile;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetwork.IsolationMethod;
import com.cloud.network.dao.NetworkVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachineProfile;

@Component
public class VxlanGuestNetworkGuru extends GuestNetworkGuru {
    private static final Logger s_logger = Logger.getLogger(VxlanGuestNetworkGuru.class);

    public VxlanGuestNetworkGuru() {
        super();
        _isolationMethods = new IsolationMethod[] {new IsolationMethod("VXLAN")};
    }

    @Override
    protected boolean canHandle(NetworkOffering offering, final NetworkType networkType, final PhysicalNetwork physicalNetwork) {
        // This guru handles only Guest Isolated network that supports Source nat service
        if (networkType == NetworkType.Advanced && isMyTrafficType(offering.getTrafficType()) &&
                (offering.getGuestType() == Network.GuestType.Isolated || offering.getGuestType() == Network.GuestType.L2) &&
                isMyIsolationMethod(physicalNetwork)) {
            return true;
        } else {
            s_logger.trace("We only take care of Guest networks of type   " + GuestType.Isolated + " or " + GuestType.L2 + " in zone of type " + NetworkType.Advanced);
            return false;
        }
    }

    @Override
    public Network design(NetworkOffering offering, DeploymentPlan plan, Network userSpecified, Account owner) {
        NetworkVO network = (NetworkVO)super.design(offering, plan, userSpecified, owner);
        if (network == null) {
            return null;
        }
        if (offering.getGuestType() == GuestType.L2 && network.getBroadcastUri() != null) {
            String vxlan = BroadcastDomainType.getValue(network.getBroadcastUri());
            network.setBroadcastUri(BroadcastDomainType.Vxlan.toUri(vxlan));
        }
        network.setBroadcastDomainType(BroadcastDomainType.Vxlan);
        return updateNetworkDesignForIPv6IfNeeded(network, userSpecified);
    }

    @Override
    protected void allocateVnet(Network network, NetworkVO implemented, long dcId, long physicalNetworkId, String reservationId)
        throws InsufficientVirtualNetworkCapacityException {
        if (network.getBroadcastUri() == null) {
            String vnet = _dcDao.allocateVnet(dcId, physicalNetworkId, network.getAccountId(), reservationId, UseSystemGuestVlans.valueIn(network.getAccountId()));
            if (vnet == null) {
                throw new InsufficientVirtualNetworkCapacityException("Unable to allocate vnet as a " + "part of network " + network + " implement ", DataCenter.class,
                    dcId);
            }
            implemented.setBroadcastUri(BroadcastDomainType.Vxlan.toUri(vnet));
            allocateVnetComplete(network, implemented, dcId, physicalNetworkId, reservationId, vnet);
        } else {
            implemented.setBroadcastUri(network.getBroadcastUri());
        }
    }

    // For Test: Mockit cannot mock static method, wrap it
    protected void allocateVnetComplete(Network network, NetworkVO implemented, long dcId, long physicalNetworkId, String reservationId, String vnet) {
        //TODO(VXLAN): Add new event type for vxlan?
        ActionEventUtils.onCompletedActionEvent(CallContext.current().getCallingUserId(), network.getAccountId(), EventVO.LEVEL_INFO, EventTypes.EVENT_ZONE_VLAN_ASSIGN,
            "Assigned Zone vNet: " + vnet + " Network Id: " + network.getId(), network.getId(), ApiCommandResourceType.Network.toString(), 0);
    }

    @Override
    public Network implement(Network network, NetworkOffering offering, DeployDestination dest, ReservationContext context)
        throws InsufficientVirtualNetworkCapacityException {
        assert (network.getState() == State.Implementing) : "Why are we implementing " + network;

        long dcId = dest.getDataCenter().getId();

        //get physical network id
        Long physicalNetworkId = network.getPhysicalNetworkId();

        // physical network id can be null in Guest Network in Basic zone, so locate the physical network
        if (physicalNetworkId == null) {
            physicalNetworkId = _networkModel.findPhysicalNetworkId(dcId, offering.getTags(), offering.getTrafficType());
        }

        NetworkVO implemented =
            new NetworkVO(network.getTrafficType(), network.getMode(), network.getBroadcastDomainType(), network.getNetworkOfferingId(), State.Allocated,
                network.getDataCenterId(), physicalNetworkId, offering.isRedundantRouter());

        allocateVnet(network, implemented, dcId, physicalNetworkId, context.getReservationId());

        if (network.getGateway() != null) {
            implemented.setGateway(network.getGateway());
        }

        if (network.getCidr() != null) {
            implemented.setCidr(network.getCidr());
        }

        return implemented;
    }

    @Override
    public void reserve(NicProfile nic, Network network, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context)
        throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException {
        super.reserve(nic, network, vm, dest, context);
    }

    @Override
    public boolean release(NicProfile nic, VirtualMachineProfile vm, String reservationId) {
        return super.release(nic, vm, reservationId);
    }

    @Override
    public void shutdown(NetworkProfile profile, NetworkOffering offering) {
        NetworkVO networkObject = _networkDao.findById(profile.getId());
        if (networkObject.getBroadcastDomainType() != BroadcastDomainType.Vxlan || networkObject.getBroadcastUri() == null) {
            s_logger.warn("BroadcastUri is empty or incorrect for guestnetwork " + networkObject.getDisplayText());
            return;
        }

        super.shutdown(profile, offering);
    }

    @Override
    public boolean trash(Network network, NetworkOffering offering) {
        return super.trash(network, offering);
    }

}
