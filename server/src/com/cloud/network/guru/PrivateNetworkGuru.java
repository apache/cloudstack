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

import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.State;
import com.cloud.network.NetworkModel;
import com.cloud.network.NetworkProfile;
import com.cloud.network.Networks.AddressFormat;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.IsolationType;
import com.cloud.network.Networks.Mode;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.vpc.PrivateIpAddress;
import com.cloud.network.vpc.PrivateIpVO;
import com.cloud.network.vpc.dao.PrivateIpDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.Nic.ReservationStrategy;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachineProfile;

@Local(value = NetworkGuru.class)
public class PrivateNetworkGuru extends AdapterBase implements NetworkGuru {
    private static final Logger s_logger = Logger.getLogger(PrivateNetworkGuru.class);
    @Inject
    protected ConfigurationManager _configMgr;
    @Inject
    protected PrivateIpDao _privateIpDao;
    @Inject
    protected NetworkModel _networkMgr;
    @Inject
    EntityManager _entityMgr;

    private static final TrafficType[] TrafficTypes = {TrafficType.Guest};

    protected PrivateNetworkGuru() {
        super();
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

    @Override
    public TrafficType[] getSupportedTrafficType() {
        return TrafficTypes;
    }

    protected boolean canHandle(NetworkOffering offering, DataCenter dc) {
        // This guru handles only system Guest network
        if (dc.getNetworkType() == NetworkType.Advanced && isMyTrafficType(offering.getTrafficType()) && offering.getGuestType() == Network.GuestType.Isolated &&
            offering.isSystemOnly()) {
            return true;
        } else {
            s_logger.trace("We only take care of system Guest networks of type   " + GuestType.Isolated + " in zone of type " + NetworkType.Advanced);
            return false;
        }
    }

    @Override
    public Network design(NetworkOffering offering, DeploymentPlan plan, Network userSpecified, Account owner) {
        DataCenter dc = _entityMgr.findById(DataCenter.class, plan.getDataCenterId());
        if (!canHandle(offering, dc)) {
            return null;
        }

        BroadcastDomainType broadcastType;
        if (userSpecified != null) {
            broadcastType = userSpecified.getBroadcastDomainType();
        } else {
            broadcastType = BroadcastDomainType.Vlan;
        }
        NetworkVO network =
            new NetworkVO(offering.getTrafficType(), Mode.Static, broadcastType, offering.getId(), State.Allocated, plan.getDataCenterId(),
                    plan.getPhysicalNetworkId(), offering.getRedundantRouter());
        if (userSpecified != null) {
            if ((userSpecified.getCidr() == null && userSpecified.getGateway() != null) || (userSpecified.getCidr() != null && userSpecified.getGateway() == null)) {
                throw new InvalidParameterValueException("cidr and gateway must be specified together.");
            }

            if (userSpecified.getCidr() != null) {
                network.setCidr(userSpecified.getCidr());
                network.setGateway(userSpecified.getGateway());
            } else {
                throw new InvalidParameterValueException("Can't design network " + network + "; netmask/gateway must be passed in");
            }

            if (offering.getSpecifyVlan()) {
                network.setBroadcastUri(userSpecified.getBroadcastUri());
                network.setState(State.Setup);
            }
        } else {
            throw new CloudRuntimeException("Can't design network " + network + "; netmask/gateway must be passed in");

        }

        return network;
    }

    @Override
    public void deallocate(Network network, NicProfile nic, VirtualMachineProfile vm) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Deallocate network: networkId: " + nic.getNetworkId() + ", ip: " + nic.getIPv4Address());
        }

        PrivateIpVO ip = _privateIpDao.findByIpAndSourceNetworkId(nic.getNetworkId(), nic.getIPv4Address());
        if (ip != null) {
            _privateIpDao.releaseIpAddress(nic.getIPv4Address(), nic.getNetworkId());
        }
        nic.deallocate();
    }

    @Override
    public Network implement(Network network, NetworkOffering offering, DeployDestination dest, ReservationContext context)
        throws InsufficientVirtualNetworkCapacityException {

        return network;
    }

    @Override
    public NicProfile allocate(Network network, NicProfile nic, VirtualMachineProfile vm) throws InsufficientVirtualNetworkCapacityException,
        InsufficientAddressCapacityException {
        DataCenter dc = _entityMgr.findById(DataCenter.class, network.getDataCenterId());
        NetworkOffering offering = _entityMgr.findById(NetworkOffering.class, network.getNetworkOfferingId());
        if (!canHandle(offering, dc)) {
            return null;
        }

        if (nic == null) {
            nic = new NicProfile(ReservationStrategy.Create, null, null, null, null);
        }

        getIp(nic, dc, network);

        if (nic.getIPv4Address() == null) {
            nic.setReservationStrategy(ReservationStrategy.Start);
        } else {
            nic.setReservationStrategy(ReservationStrategy.Create);
        }

        return nic;
    }

    protected void getIp(NicProfile nic, DataCenter dc, Network network) throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException {
        if (nic.getIPv4Address() == null) {
            PrivateIpVO ipVO = _privateIpDao.allocateIpAddress(network.getDataCenterId(), network.getId(), null);
            String vlanTag = BroadcastDomainType.getValue(network.getBroadcastUri());
            String netmask = NetUtils.getCidrNetmask(network.getCidr());
            PrivateIpAddress ip =
                new PrivateIpAddress(ipVO, vlanTag, network.getGateway(), netmask, NetUtils.long2Mac(NetUtils.createSequenceBasedMacAddress(ipVO.getMacAddress())));

            nic.setIPv4Address(ip.getIpAddress());
            nic.setIPv4Gateway(ip.getGateway());
            nic.setIPv4Netmask(ip.getNetmask());
            nic.setIsolationUri(IsolationType.Vlan.toUri(ip.getBroadcastUri()));
            nic.setBroadcastUri(IsolationType.Vlan.toUri(ip.getBroadcastUri()));
            nic.setBroadcastType(BroadcastDomainType.Vlan);
            nic.setFormat(AddressFormat.Ip4);
            nic.setReservationId(String.valueOf(ip.getBroadcastUri()));
            nic.setMacAddress(ip.getMacAddress());
        }

        nic.setIPv4Dns1(dc.getDns1());
        nic.setIPv4Dns2(dc.getDns2());
    }

    @Override
    public void updateNicProfile(NicProfile profile, Network network) {
        DataCenter dc = _entityMgr.findById(DataCenter.class, network.getDataCenterId());
        if (profile != null) {
            profile.setIPv4Dns1(dc.getDns1());
            profile.setIPv4Dns2(dc.getDns2());
        }
    }

    @Override
    public void reserve(NicProfile nic, Network network, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context)
        throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException {
        if (nic.getIPv4Address() == null) {
            getIp(nic, _entityMgr.findById(DataCenter.class, network.getDataCenterId()), network);
            nic.setReservationStrategy(ReservationStrategy.Create);
        }
    }

    @Override
    public boolean release(NicProfile nic, VirtualMachineProfile vm, String reservationId) {
        return true;
    }

    @Override
    public void shutdown(NetworkProfile profile, NetworkOffering offering) {

    }

    @Override
    public boolean trash(Network network, NetworkOffering offering) {
        return true;
    }

    @Override
    public void updateNetworkProfile(NetworkProfile networkProfile) {
        DataCenter dc = _entityMgr.findById(DataCenter.class, networkProfile.getDataCenterId());
        networkProfile.setDns1(dc.getDns1());
        networkProfile.setDns2(dc.getDns2());
    }
}
