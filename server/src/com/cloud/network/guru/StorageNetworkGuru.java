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

import org.apache.log4j.Logger;

import com.cloud.dc.Pod;
import com.cloud.dc.StorageNetworkIpAddressVO;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.network.Network;
import com.cloud.network.NetworkProfile;
import com.cloud.network.Networks.AddressFormat;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.Mode;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.StorageNetworkManager;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.Nic.ReservationStrategy;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachineProfile;

@Local(value = NetworkGuru.class)
public class StorageNetworkGuru extends PodBasedNetworkGuru implements NetworkGuru {
    private static final Logger s_logger = Logger.getLogger(StorageNetworkGuru.class);
    @Inject
    StorageNetworkManager _sNwMgr;
    @Inject
    NetworkDao _nwDao;

    protected StorageNetworkGuru() {
        super();
    }

    private static final TrafficType[] TrafficTypes = {TrafficType.Storage};

    @Override
    public boolean isMyTrafficType(TrafficType type) {
        for (TrafficType t : TrafficTypes) {
            if (t == type) {
                return true;
            }
        }
        return false;
    }

    @Override
    public TrafficType[] getSupportedTrafficType() {
        return TrafficTypes;
    }

    protected boolean canHandle(NetworkOffering offering) {
        if (isMyTrafficType(offering.getTrafficType()) && offering.isSystemOnly()) {
            return true;
        } else {
            s_logger.trace("It's not storage network offering, skip it.");
            return false;
        }
    }

    @Override
    public Network design(NetworkOffering offering, DeploymentPlan plan, Network userSpecified, Account owner) {
        if (!canHandle(offering)) {
            return null;
        }

        NetworkVO config =
            new NetworkVO(offering.getTrafficType(), Mode.Static, BroadcastDomainType.Native, offering.getId(), Network.State.Setup, plan.getDataCenterId(),
                plan.getPhysicalNetworkId(), offering.getRedundantRouter());
        return config;
    }

    @Override
    public Network implement(Network network, NetworkOffering offering, DeployDestination destination, ReservationContext context)
        throws InsufficientVirtualNetworkCapacityException {
        assert network.getTrafficType() == TrafficType.Storage : "Why are you sending this configuration to me " + network;
        if (!_sNwMgr.isStorageIpRangeAvailable(destination.getDataCenter().getId())) {
            return super.implement(network, offering, destination, context);
        }
        return network;
    }

    @Override
    public NicProfile allocate(Network network, NicProfile nic, VirtualMachineProfile vm) throws InsufficientVirtualNetworkCapacityException,
        InsufficientAddressCapacityException {
        assert network.getTrafficType() == TrafficType.Storage : "Well, I can't take care of this config now can I? " + network;
        if (!_sNwMgr.isStorageIpRangeAvailable(network.getDataCenterId())) {
            return super.allocate(network, nic, vm);
        }

        return new NicProfile(ReservationStrategy.Start, null, null, null, null);
    }

    @Override
    public void reserve(NicProfile nic, Network network, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context)
        throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException {
        if (!_sNwMgr.isStorageIpRangeAvailable(dest.getDataCenter().getId())) {
            super.reserve(nic, network, vm, dest, context);
            return;
        }

        Pod pod = dest.getPod();
        Integer vlan = null;

        StorageNetworkIpAddressVO ip = _sNwMgr.acquireIpAddress(pod.getId());
        if (ip == null) {
            throw new InsufficientAddressCapacityException("Unable to get a storage network ip address", Pod.class, pod.getId());
        }

        vlan = ip.getVlan();
        nic.setIPv4Address(ip.getIpAddress());
        nic.setMacAddress(NetUtils.long2Mac(NetUtils.createSequenceBasedMacAddress(ip.getMac())));
        nic.setFormat(AddressFormat.Ip4);
        nic.setIPv4Netmask(ip.getNetmask());
        nic.setBroadcastType(BroadcastDomainType.Storage);
        nic.setIPv4Gateway(ip.getGateway());
        if (vlan != null) {
            nic.setBroadcastUri(BroadcastDomainType.Storage.toUri(vlan));
        } else {
            nic.setBroadcastUri(null);
        }
        nic.setIsolationUri(null);
        s_logger.debug("Allocated a storage nic " + nic + " for " + vm);
    }

    @Override
    public boolean release(NicProfile nic, VirtualMachineProfile vm, String reservationId) {
        Network nw = _nwDao.findById(nic.getNetworkId());
        if (!_sNwMgr.isStorageIpRangeAvailable(nw.getDataCenterId())) {
            return super.release(nic, vm, reservationId);
        }

        _sNwMgr.releaseIpAddress(nic.getIPv4Address());
        s_logger.debug("Release an storage ip " + nic.getIPv4Address());
        nic.deallocate();
        return true;
    }

    @Override
    public void deallocate(Network network, NicProfile nic, VirtualMachineProfile vm) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateNicProfile(NicProfile profile, Network network) {
        // TODO Auto-generated method stub

    }

    @Override
    public void shutdown(NetworkProfile network, NetworkOffering offering) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean trash(Network network, NetworkOffering offering) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void updateNetworkProfile(NetworkProfile networkProfile) {
        // TODO Auto-generated method stub

    }

}
