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

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestIpType;
import com.cloud.network.Network.State;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkVO;
import com.cloud.network.Networks.AddressFormat;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.Mode;
import com.cloud.network.Networks.TrafficType;
import com.cloud.offering.NetworkOffering;
import com.cloud.resource.Resource.ReservationStrategy;
import com.cloud.user.Account;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.Inject;
import com.cloud.utils.db.DB;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.NicDao;

@Local(value=NetworkGuru.class)
public class GuestNetworkGuru extends AdapterBase implements NetworkGuru {
    private static final Logger s_logger = Logger.getLogger(GuestNetworkGuru.class);
    @Inject protected NetworkManager _networkMgr;
    @Inject protected DataCenterDao _dcDao;
    @Inject protected VlanDao _vlanDao;
    @Inject protected NicDao _nicDao;
    
    String _defaultGateway;
    String _defaultCidr;
    Random _rand = new Random(System.currentTimeMillis());
    
    protected GuestNetworkGuru() {
        super();
    } 
    
    protected boolean canHandle(NetworkOffering offering, DataCenter dc) {
        //This guru handles only non-system Guest network
        if (dc.getNetworkType() == NetworkType.Advanced && offering.getTrafficType() == TrafficType.Guest && !offering.isSystemOnly()) {
            return true;
        } else {
            s_logger.trace("We only take care of Guest networks in zone of type " + NetworkType.Advanced);
            return false;
        }
    }
    
    @Override
    public Network design(NetworkOffering offering, DeploymentPlan plan, Network userSpecified, Account owner) {
        DataCenter dc = _dcDao.findById(plan.getDataCenterId());
        if (!canHandle(offering, dc)) {
            return null;
        }

        NetworkVO network = new NetworkVO(offering.getTrafficType(), GuestIpType.Virtual, Mode.Dhcp, BroadcastDomainType.Vlan, offering.getId(), plan.getDataCenterId(), State.Allocated);
        if (userSpecified != null) {
            if ((userSpecified.getCidr() == null && userSpecified.getGateway() != null) ||
                (userSpecified.getCidr() != null && userSpecified.getGateway() == null)) {
                throw new InvalidParameterValueException("cidr and gateway must be specified together.");
            }
            
            if (userSpecified.getCidr() != null) {
                network.setCidr(userSpecified.getCidr());
                network.setGateway(userSpecified.getGateway());
            } else {
                String guestNetworkCidr = dc.getGuestNetworkCidr();
                //guest network cidr can be null for Basic zone
                if (guestNetworkCidr != null) {
                    String[] cidrTuple = guestNetworkCidr.split("\\/");
                    network.setGateway(NetUtils.getIpRangeStartIpFromCidr(cidrTuple[0], Long.parseLong(cidrTuple[1])));
                    network.setCidr(guestNetworkCidr); 
                }
            }
            
            network.setDns1(dc.getDns1());
            network.setDns2(dc.getDns2());
            
            if (userSpecified.getBroadcastUri() != null) {
                network.setBroadcastUri(userSpecified.getBroadcastUri());
                network.setState(State.Setup);
            }
            if (userSpecified.getNetworkDomain() != null) {
                network.setNetworkDomain(userSpecified.getNetworkDomain());
            }
            
        } else {
            String guestNetworkCidr = dc.getGuestNetworkCidr();
            String[] cidrTuple = guestNetworkCidr.split("\\/");
            network.setGateway(NetUtils.getIpRangeStartIpFromCidr(cidrTuple[0], Long.parseLong(cidrTuple[1])));
            network.setCidr(guestNetworkCidr);
            network.setDns1(dc.getDns1());
            network.setDns2(dc.getDns2());
        }
        
        return network;
    }

    @Override
    public void deallocate(Network network, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm) {
    }
    
    @Override
    public Network implement(Network network, NetworkOffering offering, DeployDestination dest, ReservationContext context) {
        assert (network.getState() == State.Implementing) : "Why are we implementing " + network;
        
        long dcId = dest.getDataCenter().getId();
        
        
        NetworkVO implemented = new NetworkVO(network.getTrafficType(), network.getGuestType(), network.getMode(), network.getBroadcastDomainType(), network.getNetworkOfferingId(), network.getDataCenterId(), State.Allocated);
        
        if (network.getBroadcastUri() == null) {
            String vnet = _dcDao.allocateVnet(dcId, network.getAccountId(), context.getReservationId());
            implemented.setBroadcastUri(BroadcastDomainType.Vlan.toUri(vnet));
        } else {
            implemented.setBroadcastUri(network.getBroadcastUri());
        }
        
        if (network.getGateway() != null) {
            implemented.setGateway(network.getGateway());
        }
        
        if (network.getCidr() != null) {
            implemented.setCidr(network.getCidr());
        }
        
        if (network.getDns1() != null) {
            implemented.setDns1(network.getDns1());
        }
        
        if (network.getDns2() != null) {
            implemented.setDns2(network.getDns2());
        }
        
        return implemented;
    }

    @Override
    public NicProfile allocate(Network network, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm)
            throws InsufficientVirtualNetworkCapcityException, InsufficientAddressCapacityException {
        assert (network.getTrafficType() == TrafficType.Guest) : "Look at my name!  Why are you calling me when the traffic type is : " + network.getTrafficType();
        
        if (nic == null) {
            nic = new NicProfile(ReservationStrategy.Start, null, null, null, null);
        } 
        
        if (nic.getIp4Address() == null){
            nic.setBroadcastUri(network.getBroadcastUri());
            nic.setIsolationUri(network.getBroadcastUri());
            nic.setGateway(network.getGateway());
            nic.setIp4Address(acquireGuestIpAddress(network));
            nic.setNetmask(NetUtils.cidr2Netmask(network.getCidr()));
            nic.setDns1(network.getDns1());
            nic.setDns2(network.getDns2());
            nic.setFormat(AddressFormat.Ip4);
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
    
    @DB
    protected String acquireGuestIpAddress(Network network) {
        List<String> ips = _nicDao.listIpAddressInNetworkConfiguration(network.getId());
        String[] cidr = network.getCidr().split("/");
        Set<Long> allPossibleIps = NetUtils.getAllIpsFromCidr(cidr[0], Integer.parseInt(cidr[1]));
        Set<Long> usedIps = new TreeSet<Long> ();
        for (String ip : ips) {
            usedIps.add(NetUtils.ip2Long(ip));
        }
        if (usedIps.size() != 0) {
            allPossibleIps.removeAll(usedIps);
        }
        if (allPossibleIps.isEmpty()) {
            return null;
        }
        Long[] array = allPossibleIps.toArray(new Long[allPossibleIps.size()]);
        return NetUtils.long2Ip(array[_rand.nextInt(array.length)]);
     }

    @Override
    public void reserve(NicProfile nic, Network network, VirtualMachineProfile<? extends VirtualMachine> vm, DeployDestination dest, ReservationContext context) throws InsufficientVirtualNetworkCapcityException,
            InsufficientAddressCapacityException {
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
    public void destroy(Network network, NetworkOffering offering) {
        s_logger.debug("Releasing vnet for the network id=" + network.getId());
        _dcDao.releaseVnet(network.getBroadcastUri().getHost(), network.getDataCenterId(), network.getAccountId(), network.getReservationId());
        _networkMgr.resetBroadcastUri(network.getId());
    }

    @Override
    public boolean trash(Network network, NetworkOffering offering, Account owner) {
        return true;
    }
}
