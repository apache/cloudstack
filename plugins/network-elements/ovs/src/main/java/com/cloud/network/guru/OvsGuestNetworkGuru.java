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


import javax.inject.Inject;

import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.context.CallContext;
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
import com.cloud.network.Network.Service;
import com.cloud.network.Network.State;
import com.cloud.network.NetworkProfile;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetwork.IsolationMethod;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.ovs.OvsTunnelManager;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.user.Account;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachineProfile;

@Component
public class OvsGuestNetworkGuru extends GuestNetworkGuru {

    @Inject
    OvsTunnelManager _ovsTunnelMgr;
    @Inject
    NetworkOfferingServiceMapDao _ntwkOfferingSrvcDao;
    @Inject
    VpcDao _vpcDao;

    OvsGuestNetworkGuru() {
        super();
        _isolationMethods = new IsolationMethod[] {new IsolationMethod("GRE"),
            new IsolationMethod("L3"), new IsolationMethod("VLAN")};
    }

    @Override
    protected boolean canHandle(NetworkOffering offering,
        final NetworkType networkType, final PhysicalNetwork physicalNetwork) {
        // This guru handles only Guest Isolated network that supports Source
        // nat service
        if (networkType == NetworkType.Advanced
            && isMyTrafficType(offering.getTrafficType())
            && offering.getGuestType() == Network.GuestType.Isolated
            && isMyIsolationMethod(physicalNetwork)
            && _ntwkOfferingSrvcDao.areServicesSupportedByNetworkOffering(
                offering.getId(), Service.Connectivity)) {
            return true;
        } else if (networkType == NetworkType.Advanced
            && offering.getGuestType() == GuestType.Shared
            && _ntwkOfferingSrvcDao.isProviderForNetworkOffering(offering.getId(), Network.Provider.Ovs)
            && physicalNetwork.getIsolationMethods().contains("GRE")) {
            return true;
        } else {
            logger.trace(String.format("We only take care of Guest networks of type %s with Service %s or type with %s provider %s in %s zone",
                    GuestType.Isolated, Service.Connectivity, GuestType.Shared, Network.Provider.Ovs, NetworkType.Advanced));
            return false;
        }
    }

    @Override
    public Network design(NetworkOffering offering, DeploymentPlan plan,
                          Network userSpecified, String name, Long vpcId, Account owner) {

        PhysicalNetworkVO physnet = _physicalNetworkDao.findById(plan
            .getPhysicalNetworkId());
        DataCenter dc = _dcDao.findById(plan.getDataCenterId());
        if (!canHandle(offering, dc.getNetworkType(), physnet)) {
            logger.debug("Refusing to design this network");
            return null;
        }
        NetworkVO config = (NetworkVO)super.design(offering, plan,
            userSpecified, name, vpcId, owner);
        if (config == null) {
            return null;
        }

        config.setBroadcastDomainType(BroadcastDomainType.Vswitch);
        if (config.getBroadcastUri() != null) {
            config.setBroadcastUri(BroadcastDomainType.Vswitch.toUri(config.getBroadcastUri().toString().replace("vlan://", "")));
        }

        return config;
    }

    @Override
    public Network implement(Network network, NetworkOffering offering,
        DeployDestination dest, ReservationContext context)
        throws InsufficientVirtualNetworkCapacityException {
        assert (network.getState() == State.Implementing) : "Why are we implementing "
            + network;

        long dcId = dest.getDataCenter().getId();
        NetworkType nwType = dest.getDataCenter().getNetworkType();
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
            logger.debug("Refusing to implement this network");
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

        implemented.setBroadcastDomainType(BroadcastDomainType.Vswitch);

        // for the networks that are part of VPC enabled for distributed routing use scheme vs://vpcid.GRE key for network
        if (network.getVpcId() != null && isVpcEnabledForDistributedRouter(network.getVpcId())) {
            String keyStr = BroadcastDomainType.getValue(implemented.getBroadcastUri());
            Long vpcid= network.getVpcId();
            implemented.setBroadcastUri(BroadcastDomainType.Vswitch.toUri(vpcid.toString() + "." + keyStr));
        }

        return implemented;
    }

    @Override
    public void reserve(NicProfile nic, Network network,
        VirtualMachineProfile vm,
        DeployDestination dest, ReservationContext context)
        throws InsufficientVirtualNetworkCapacityException,
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
        if (networkObject.getBroadcastDomainType() != BroadcastDomainType.Vswitch
            || networkObject.getBroadcastUri() == null) {
            logger.warn("BroadcastUri is empty or incorrect for guestnetwork "
                + networkObject.getDisplayText());
            return;
        }

        if (profile.getBroadcastDomainType() == BroadcastDomainType.Vswitch ) {
            logger.debug("Releasing vnet for the network id=" + profile.getId());
            _dcDao.releaseVnet(BroadcastDomainType.getValue(profile.getBroadcastUri()), profile.getDataCenterId(), profile.getPhysicalNetworkId(),
                    profile.getAccountId(), profile.getReservationId());
        }
        profile.setBroadcastUri(null);
    }

    @Override
    public boolean trash(Network network, NetworkOffering offering) {
        return super.trash(network, offering);
    }

    @Override
    protected void allocateVnet(Network network, NetworkVO implemented,
        long dcId, long physicalNetworkId, String reservationId)
        throws InsufficientVirtualNetworkCapacityException {
        if (network.getBroadcastUri() == null) {
            String vnet = _dcDao.allocateVnet(dcId, physicalNetworkId,
                network.getAccountId(), reservationId,
                UseSystemGuestVlans.valueIn(network.getAccountId()));
            if (vnet == null) {
                throw new InsufficientVirtualNetworkCapacityException(
                    "Unable to allocate vnet as a part of network "
                        + network + " implement ", DataCenter.class,
                    dcId);
            }
            implemented
                .setBroadcastUri(BroadcastDomainType.Vswitch.toUri(vnet));
            ActionEventUtils.onCompletedActionEvent(
                CallContext.current().getCallingUserId(),
                network.getAccountId(),
                EventVO.LEVEL_INFO,
                EventTypes.EVENT_ZONE_VLAN_ASSIGN,
                "Assigned Zone Vlan: " + vnet + " Network Id: "
                    + network.getId(),
                network.getId(), ApiCommandResourceType.Network.toString(), 0);
        } else {
            implemented.setBroadcastUri(network.getBroadcastUri());
        }
    }

    boolean isVpcEnabledForDistributedRouter(long vpcId) {
        VpcVO vpc = _vpcDao.findById(vpcId);
        return vpc.usesDistributedRouter();
    }
}
