/**
 * *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
*
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

import java.util.List;

import javax.ejb.Local;

import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenter;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.event.EventTypes;
import com.cloud.event.EventUtils;
import com.cloud.event.EventVO;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
import com.cloud.network.ExternalNetworkDeviceManager;
import com.cloud.network.Network;
import com.cloud.network.Network.State;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkVO;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.ovs.OvsNetworkManager;
import com.cloud.network.ovs.OvsTunnelManager;
import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.user.UserContext;
import com.cloud.utils.component.Inject;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.Ip;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.Nic.ReservationStrategy;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

@Local(value = NetworkGuru.class)
public class ExternalGuestNetworkGuru extends GuestNetworkGuru {

    @Inject
    NetworkManager _networkMgr;
    @Inject
    ExternalNetworkDeviceManager _externalNetworkMgr;
    @Inject
    NetworkDao _networkDao;
    @Inject
    DataCenterDao _zoneDao;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    PortForwardingRulesDao _pfRulesDao;
    @Inject
    OvsNetworkManager _ovsNetworkMgr;
    @Inject
    OvsTunnelManager _tunnelMgr;

    @Override
    public Network design(NetworkOffering offering, DeploymentPlan plan, Network userSpecified, Account owner) {
        if (_ovsNetworkMgr.isOvsNetworkEnabled() || _tunnelMgr.isOvsTunnelEnabled()) {
            return null;
        }

        NetworkVO config = (NetworkVO) super.design(offering, plan, userSpecified, owner);
        if (config == null) {
            return null;
        } else if (_networkMgr.networkIsConfiguredForExternalNetworking(plan.getDataCenterId(), config.getId())) {
            config.setState(State.Allocated);
        }

        return config;
    }

    @Override
    public Network implement(Network config, NetworkOffering offering, DeployDestination dest, ReservationContext context) throws InsufficientVirtualNetworkCapcityException {
        assert (config.getState() == State.Implementing) : "Why are we implementing " + config;

        if (_ovsNetworkMgr.isOvsNetworkEnabled() || _tunnelMgr.isOvsTunnelEnabled()) {
            return null;
        }

        if (!_networkMgr.networkIsConfiguredForExternalNetworking(config.getDataCenterId(), config.getId())) {
            return super.implement(config, offering, dest, context);
        }

        DataCenter zone = dest.getDataCenter();
        NetworkVO implemented = new NetworkVO(config.getTrafficType(), config.getMode(), config.getBroadcastDomainType(), config.getNetworkOfferingId(), State.Allocated,
                config.getDataCenterId(), config.getPhysicalNetworkId());

        // Get a vlan tag
        int vlanTag;
        if (config.getBroadcastUri() == null) {
            String vnet = _dcDao.allocateVnet(zone.getId(), config.getPhysicalNetworkId(), config.getAccountId(), context.getReservationId());

            try {
                vlanTag = Integer.parseInt(vnet);
            } catch (NumberFormatException e) {
                throw new CloudRuntimeException("Obtained an invalid guest vlan tag. Exception: " + e.getMessage());
            }

            implemented.setBroadcastUri(BroadcastDomainType.Vlan.toUri(vlanTag));
            EventUtils.saveEvent(UserContext.current().getCallerUserId(), config.getAccountId(), EventVO.LEVEL_INFO, EventTypes.EVENT_ZONE_VLAN_ASSIGN, "Assignbed Zone Vlan: "+vnet+ " Network Id: "+config.getId(), 0);
        } else {
            vlanTag = Integer.parseInt(config.getBroadcastUri().getHost());
            implemented.setBroadcastUri(config.getBroadcastUri());
        }

        // Determine the offset from the lowest vlan tag
        int offset = _externalNetworkMgr.getVlanOffset(config.getPhysicalNetworkId(), vlanTag);

        // Determine the new gateway and CIDR
        String[] oldCidr = config.getCidr().split("/");
        String oldCidrAddress = oldCidr[0];
        int cidrSize = _externalNetworkMgr.getGloballyConfiguredCidrSize();

        // If the offset has more bits than there is room for, return null
        long bitsInOffset = 32 - Integer.numberOfLeadingZeros(offset);
        if (bitsInOffset > (cidrSize - 8)) {
            throw new CloudRuntimeException("The offset " + offset + " needs " + bitsInOffset + " bits, but only have " + (cidrSize - 8) + " bits to work with.");
        }

        long newCidrAddress = (NetUtils.ip2Long(oldCidrAddress) & 0xff000000) | (offset << (32 - cidrSize));
        implemented.setGateway(NetUtils.long2Ip(newCidrAddress + 1));
        implemented.setCidr(NetUtils.long2Ip(newCidrAddress) + "/" + cidrSize);
        implemented.setState(State.Implemented);

        // Mask the Ipv4 address of all nics that use this network with the new guest VLAN offset
        List<NicVO> nicsInNetwork = _nicDao.listByNetworkId(config.getId());
        for (NicVO nic : nicsInNetwork) {
            if (nic.getIp4Address() != null) {
                long ipMask = getIpMask(nic.getIp4Address(), cidrSize);
                nic.setIp4Address(NetUtils.long2Ip(newCidrAddress | ipMask));
                _nicDao.persist(nic);
            }
        }       
        
        // Mask the destination address of all port forwarding rules in this network with the new guest VLAN offset
        List<PortForwardingRuleVO> pfRulesInNetwork = _pfRulesDao.listByNetwork(config.getId());
        for (PortForwardingRuleVO pfRule : pfRulesInNetwork) {
        	if (pfRule.getDestinationIpAddress() != null) {
        		long ipMask = getIpMask(pfRule.getDestinationIpAddress().addr(), cidrSize);
        		String maskedDestinationIpAddress = NetUtils.long2Ip(newCidrAddress | ipMask);
        		pfRule.setDestinationIpAddress(new Ip(maskedDestinationIpAddress));
        		_pfRulesDao.update(pfRule.getId(), pfRule);
        	}
        }

        return implemented;
    }

    @Override
    public NicProfile allocate(Network config, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm) throws InsufficientVirtualNetworkCapcityException,
            InsufficientAddressCapacityException {
        
        if (_networkMgr.networkIsConfiguredForExternalNetworking(config.getDataCenterId(), config.getId()) && nic != null && nic.getRequestedIp() != null) {
            throw new CloudRuntimeException("Does not support custom ip allocation at this time: " + nic);
        }
        
        NicProfile profile = super.allocate(config, nic, vm);

        if (_ovsNetworkMgr.isOvsNetworkEnabled() || _tunnelMgr.isOvsTunnelEnabled()) {
            return null;
        }

        if (_networkMgr.networkIsConfiguredForExternalNetworking(config.getDataCenterId(), config.getId())) {
            profile.setStrategy(ReservationStrategy.Start);
            profile.setIp4Address(null);
            profile.setGateway(null);
            profile.setNetmask(null);
        }

        return profile;
    }

    @Override
    public void deallocate(Network config, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm) {
        super.deallocate(config, nic, vm);

        if (_ovsNetworkMgr.isOvsNetworkEnabled() || _tunnelMgr.isOvsTunnelEnabled()) {
            return;
        }

        if (_networkMgr.networkIsConfiguredForExternalNetworking(config.getDataCenterId(), config.getId())) {
            nic.setIp4Address(null);
            nic.setGateway(null);
            nic.setNetmask(null);
            nic.setBroadcastUri(null);
            nic.setIsolationUri(null);
        }
    }

    @Override
    public void reserve(NicProfile nic, Network config, VirtualMachineProfile<? extends VirtualMachine> vm, DeployDestination dest, ReservationContext context)
            throws InsufficientVirtualNetworkCapcityException, InsufficientAddressCapacityException {
        assert (nic.getReservationStrategy() == ReservationStrategy.Start) : "What can I do for nics that are not allocated at start? ";
        if (_ovsNetworkMgr.isOvsNetworkEnabled()) {
            return;
        }
        DataCenter dc = _dcDao.findById(config.getDataCenterId());
        if (_networkMgr.networkIsConfiguredForExternalNetworking(config.getDataCenterId(), config.getId())) {
            nic.setBroadcastUri(config.getBroadcastUri());
            nic.setIsolationUri(config.getBroadcastUri());
            nic.setDns1(dc.getDns1());
            nic.setDns2(dc.getDns2());
            nic.setNetmask(NetUtils.cidr2Netmask(config.getCidr()));
            long cidrAddress = NetUtils.ip2Long(config.getCidr().split("/")[0]);
            int cidrSize = _externalNetworkMgr.getGloballyConfiguredCidrSize();
            nic.setGateway(config.getGateway());

            if (nic.getIp4Address() == null) {

                String guestIp = _networkMgr.acquireGuestIpAddress(config, null);
                if (guestIp == null) {
                    throw new InsufficientVirtualNetworkCapcityException("Unable to acquire guest IP address for network " + config, DataCenter.class, dc.getId());
                }

                nic.setIp4Address(guestIp);
            } else {
                long ipMask = NetUtils.ip2Long(nic.getIp4Address()) & ~(0xffffffffffffffffl << (32 - cidrSize));
                nic.setIp4Address(NetUtils.long2Ip(cidrAddress | ipMask));
            }
        } else {
            super.reserve(nic, config, vm, dest, context);
        }
    }

    @Override
    public boolean release(NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm, String reservationId) {
        if (_ovsNetworkMgr.isOvsNetworkEnabled() || _tunnelMgr.isOvsTunnelEnabled()) {
            return true;
        }

        NetworkVO network = _networkDao.findById(nic.getNetworkId());
        if (network != null && _networkMgr.networkIsConfiguredForExternalNetworking(network.getDataCenterId(), network.getId())) {
            return true;
        } else {
            return super.release(nic, vm, reservationId);
        }
    }
    
    private long getIpMask(String ipAddress, long cidrSize) {
    	return NetUtils.ip2Long(ipAddress) & ~(0xffffffffffffffffl << (32 - cidrSize));
    }

}
