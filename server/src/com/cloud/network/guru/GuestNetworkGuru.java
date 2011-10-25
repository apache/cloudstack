/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.network.guru;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.event.EventTypes;
import com.cloud.event.EventUtils;
import com.cloud.event.EventVO;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Network;
import com.cloud.network.Network.State;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkProfile;
import com.cloud.network.NetworkVO;
import com.cloud.network.Networks.AddressFormat;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.Mode;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.NetworkDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.user.UserContext;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.Inject;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.Nic.ReservationStrategy;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.NicDao;

@Local(value = NetworkGuru.class)
public class GuestNetworkGuru extends AdapterBase implements NetworkGuru {
    private static final Logger s_logger = Logger.getLogger(GuestNetworkGuru.class);
    @Inject
    protected NetworkManager _networkMgr;
    @Inject
    protected DataCenterDao _dcDao;
    @Inject
    protected VlanDao _vlanDao;
    @Inject
    protected NicDao _nicDao;
    @Inject
    protected NetworkDao _networkDao;

    String _defaultGateway;
    String _defaultCidr;

    protected GuestNetworkGuru() {
        super();
    }

    protected boolean canHandle(NetworkOffering offering, DataCenter dc) {
        // This guru handles only non-system Guest Isolated network
        if (dc.getNetworkType() == NetworkType.Advanced && offering.getTrafficType() == TrafficType.Guest && offering.getType() == Network.Type.Isolated && !offering.isSystemOnly()) {
            return true;
        } else {
            s_logger.trace("We only take care of Guest Virtual networks in zone of type " + NetworkType.Advanced);
            return false;
        }
    }

    @Override
    public Network design(NetworkOffering offering, DeploymentPlan plan, Network userSpecified, Account owner) {
        DataCenter dc = _dcDao.findById(plan.getDataCenterId());
        if (!canHandle(offering, dc)) {
            return null;
        }

        NetworkVO network = new NetworkVO(offering.getTrafficType(), Mode.Dhcp, BroadcastDomainType.Vlan, offering.getId(), State.Allocated, plan.getDataCenterId(), plan.getPhysicalNetworkId());
        if (userSpecified != null) {
            if ((userSpecified.getCidr() == null && userSpecified.getGateway() != null) || (userSpecified.getCidr() != null && userSpecified.getGateway() == null)) {
                throw new InvalidParameterValueException("cidr and gateway must be specified together.");
            }

            if (userSpecified.getCidr() != null) {
                network.setCidr(userSpecified.getCidr());
                network.setGateway(userSpecified.getGateway());
            } else {
                String guestNetworkCidr = dc.getGuestNetworkCidr();
                // guest network cidr can be null for Basic zone
                if (guestNetworkCidr != null) {
                    String[] cidrTuple = guestNetworkCidr.split("\\/");
                    network.setGateway(NetUtils.getIpRangeStartIpFromCidr(cidrTuple[0], Long.parseLong(cidrTuple[1])));
                    network.setCidr(guestNetworkCidr);
                }
            }

            if (userSpecified.getBroadcastUri() != null) {
                network.setBroadcastUri(userSpecified.getBroadcastUri());
                network.setState(State.Setup);
            }

        } else {
            String guestNetworkCidr = dc.getGuestNetworkCidr();
            String[] cidrTuple = guestNetworkCidr.split("\\/");
            network.setGateway(NetUtils.getIpRangeStartIpFromCidr(cidrTuple[0], Long.parseLong(cidrTuple[1])));
            network.setCidr(guestNetworkCidr);
            ;
        }

        return network;
    }

    @Override
    public void deallocate(Network network, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm) {
    }

    @Override
    public Network implement(Network network, NetworkOffering offering, DeployDestination dest, ReservationContext context) throws InsufficientVirtualNetworkCapcityException {
        assert (network.getState() == State.Implementing) : "Why are we implementing " + network;

        long dcId = dest.getDataCenter().getId();

        NetworkVO implemented = new NetworkVO(network.getTrafficType(), network.getMode(), network.getBroadcastDomainType(), network.getNetworkOfferingId(), State.Allocated,
                network.getDataCenterId(), network.getPhysicalNetworkId());

        if (network.getBroadcastUri() == null) {
            String vnet = _dcDao.allocateVnet(dcId, network.getPhysicalNetworkId(), network.getAccountId(), context.getReservationId());
            if (vnet == null) {
                throw new InsufficientVirtualNetworkCapcityException("Unable to allocate vnet as a part of network " + network + " implement ", DataCenter.class, dcId);
            }
            implemented.setBroadcastUri(BroadcastDomainType.Vlan.toUri(vnet));
            EventUtils.saveEvent(UserContext.current().getCallerUserId(), network.getAccountId(), EventVO.LEVEL_INFO, EventTypes.EVENT_ZONE_VLAN_ASSIGN, "Assignbed Zone Vlan: "+vnet+ " Network Id: "+network.getId(), 0);
        } else {
            implemented.setBroadcastUri(network.getBroadcastUri());
        }

        if (network.getGateway() != null) {
            implemented.setGateway(network.getGateway());
        }

        if (network.getCidr() != null) {
            implemented.setCidr(network.getCidr());
        }
        return implemented;
    }

    @Override
    public NicProfile allocate(Network network, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm) throws InsufficientVirtualNetworkCapcityException,
            InsufficientAddressCapacityException {

        assert (network.getTrafficType() == TrafficType.Guest) : "Look at my name!  Why are you calling me when the traffic type is : " + network.getTrafficType();

        if (nic == null) {
            nic = new NicProfile(ReservationStrategy.Start, null, null, null, null);
        }

        DataCenter dc = _dcDao.findById(network.getDataCenterId());

        if (nic.getIp4Address() == null) {
            nic.setBroadcastUri(network.getBroadcastUri());
            nic.setIsolationUri(network.getBroadcastUri());
            nic.setGateway(network.getGateway());

            String guestIp = _networkMgr.acquireGuestIpAddress(network, nic.getRequestedIp());
            if (guestIp == null) {
                throw new InsufficientVirtualNetworkCapcityException("Unable to acquire guest IP address for network " + network, DataCenter.class, dc.getId());
            }

            nic.setIp4Address(guestIp);
            nic.setNetmask(NetUtils.cidr2Netmask(network.getCidr()));
            nic.setFormat(AddressFormat.Ip4);

            nic.setDns1(dc.getDns1());
            nic.setDns2(dc.getDns2());
        } 

        nic.setStrategy(ReservationStrategy.Start);

        if (nic.getMacAddress() == null) {
            nic.setMacAddress(_networkMgr.getNextAvailableMacAddressInNetwork(network.getId()));
            if (nic.getMacAddress() == null) {
                throw new InsufficientAddressCapacityException("Unable to allocate more mac addresses", Network.class, network.getId());
            }
        }

        return nic;
    }

    @Override
    public void updateNicProfile(NicProfile profile, Network network) {
        DataCenter dc = _dcDao.findById(network.getDataCenterId());
        if (profile != null) {
            profile.setDns1(dc.getDns1());
            profile.setDns2(dc.getDns2());
        }
    }

    @Override
    public void reserve(NicProfile nic, Network network, VirtualMachineProfile<? extends VirtualMachine> vm, DeployDestination dest, ReservationContext context)
            throws InsufficientVirtualNetworkCapcityException, InsufficientAddressCapacityException {
        assert (nic.getReservationStrategy() == ReservationStrategy.Start) : "What can I do for nics that are not allocated at start? ";

        nic.setBroadcastUri(network.getBroadcastUri());
        nic.setIsolationUri(network.getBroadcastUri());
    }

    @Override
    public boolean release(NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm, String reservationId) {
        nic.setBroadcastUri(null);
        nic.setIsolationUri(null);
        return true;
    }

    @Override
    public void shutdown(NetworkProfile profile, NetworkOffering offering) {
        s_logger.debug("Releasing vnet for the network id=" + profile.getId());
        if (profile.getBroadcastUri() != null) {
            _dcDao.releaseVnet(profile.getBroadcastUri().getHost(), profile.getDataCenterId(), profile.getPhysicalNetworkId(), profile.getAccountId(), profile.getReservationId());
            EventUtils.saveEvent(UserContext.current().getCallerUserId(), profile.getAccountId(), EventVO.LEVEL_INFO, EventTypes.EVENT_ZONE_VLAN_RELEASE, "Released Zone Vlan: "
                    +profile.getBroadcastUri().getHost()+" for Network: "+profile.getId(), 0);
            profile.setBroadcastUri(null);
        }
    }

    @Override
    public boolean trash(Network network, NetworkOffering offering, Account owner) {
        return true;
    }

    @Override
    public void updateNetworkProfile(NetworkProfile networkProfile) {
        DataCenter dc = _dcDao.findById(networkProfile.getDataCenterId());
        networkProfile.setDns1(dc.getDns1());
        networkProfile.setDns2(dc.getDns2());
    }
}
