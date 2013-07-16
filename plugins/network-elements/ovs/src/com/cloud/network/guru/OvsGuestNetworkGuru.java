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

import javax.ejb.Local;
import javax.inject.Inject;

import com.cloud.event.ActionEventUtils;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.context.CallContext;

import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.State;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetwork.IsolationMethod;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.ovs.OvsTunnelManager;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.vm.ReservationContext;

@Component
@Local(value=NetworkGuru.class)
public class OvsGuestNetworkGuru extends GuestNetworkGuru {
    private static final Logger s_logger = Logger.getLogger(OvsGuestNetworkGuru.class);

    @Inject OvsTunnelManager _ovsTunnelMgr;

    OvsGuestNetworkGuru() {
        super();
        _isolationMethods = new IsolationMethod[] { IsolationMethod.GRE, IsolationMethod.L3, IsolationMethod.VLAN };
    }

    @Override
    protected boolean canHandle(NetworkOffering offering,
            final NetworkType networkType, final PhysicalNetwork physicalNetwork) {
        // This guru handles only Guest Isolated network that supports Source
        // nat service
        if (networkType == NetworkType.Advanced
                && isMyTrafficType(offering.getTrafficType())
                && offering.getGuestType() == Network.GuestType.Isolated
                && isMyIsolationMethod(physicalNetwork)) {
            return true;
        } else {
            s_logger.trace("We only take care of Guest networks of type   "
                    + GuestType.Isolated + " in zone of type "
                    + NetworkType.Advanced);
            return false;
        }
    }    

    @Override
    public Network design(NetworkOffering offering, DeploymentPlan plan, Network userSpecified, Account owner) {

        if (!_ovsTunnelMgr.isOvsTunnelEnabled()) {
            return null;
        }

        NetworkVO config = (NetworkVO) super.design(offering, plan, userSpecified, owner); 
        if (config == null) {
            return null;
        }

        config.setBroadcastDomainType(BroadcastDomainType.Vswitch);

        return config;
    }

    @Override
    protected void allocateVnet(Network network, NetworkVO implemented, long dcId,
            long physicalNetworkId, String reservationId) throws InsufficientVirtualNetworkCapcityException {
        if (network.getBroadcastUri() == null) {
            String vnet = _dcDao.allocateVnet(dcId, physicalNetworkId, network.getAccountId(), reservationId,
                    canUseSystemGuestVlan(network.getAccountId()));
            if (vnet == null) {
                throw new InsufficientVirtualNetworkCapcityException("Unable to allocate vnet as a part of network " + network + " implement ", DataCenter.class, dcId);
            }
            implemented.setBroadcastUri(BroadcastDomainType.Vswitch.toUri(vnet));
            ActionEventUtils.onCompletedActionEvent(CallContext.current().getCallingUserId(), network.getAccountId(), EventVO.LEVEL_INFO, EventTypes.EVENT_ZONE_VLAN_ASSIGN, "Assigned Zone Vlan: " + vnet + " Network Id: " + network.getId(), 0);
        } else {
            implemented.setBroadcastUri(network.getBroadcastUri());
        }
    }

    @Override
    public Network implement(Network config, NetworkOffering offering, DeployDestination dest, ReservationContext context) throws InsufficientVirtualNetworkCapcityException {
        assert (config.getState() == State.Implementing) : "Why are we implementing " + config;
        if (!_ovsTunnelMgr.isOvsTunnelEnabled()) {
            return null;
        }
        NetworkVO implemented = (NetworkVO)super.implement(config, offering, dest, context);		 
        return implemented;
    }

}
