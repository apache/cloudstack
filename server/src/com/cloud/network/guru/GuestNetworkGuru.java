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

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Network;
import com.cloud.network.Network.State;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkVO;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.Mode;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.guru.NetworkGuru;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.NetworkOffering.GuestIpType;
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
    
    @Override
    public Network design(NetworkOffering offering, DeploymentPlan plan, Network userSpecified, Account owner) {
        if (offering.getTrafficType() != TrafficType.Guest) {
            return null;
        }
       
        BroadcastDomainType broadcastType = null;
        Mode mode = null;
        GuestIpType ipType = offering.getGuestIpType();
        if (ipType == GuestIpType.Virtual) {
            mode = Mode.Dhcp;
            broadcastType = BroadcastDomainType.Vlan;
        } else {
            broadcastType = BroadcastDomainType.Native;
            mode = Mode.Dhcp;
        }
        DataCenterVO dc = _dcDao.findById(plan.getDataCenterId());
        
        NetworkVO config = new NetworkVO(offering.getTrafficType(), offering.getGuestIpType(), mode, broadcastType, offering.getId(), plan.getDataCenterId());
        if (userSpecified != null) {
            if ((userSpecified.getCidr() == null && userSpecified.getGateway() != null) ||
                (userSpecified.getCidr() != null && userSpecified.getGateway() == null)) {
                throw new InvalidParameterValueException("cidr and gateway must be specified together.");
            }
            
            if (userSpecified.getCidr() != null) {
                config.setCidr(userSpecified.getCidr());
                config.setGateway(userSpecified.getGateway());
            } else {
                String guestNetworkCidr = dc.getGuestNetworkCidr();
                String[] cidrTuple = guestNetworkCidr.split("\\/");
                config.setGateway(NetUtils.getIpRangeStartIpFromCidr(cidrTuple[0], Long.parseLong(cidrTuple[1])));
                config.setCidr(guestNetworkCidr);
            }
            
            config.setDns1(dc.getDns1());
            config.setDns2(dc.getDns2());
            
            if (userSpecified.getBroadcastUri() != null) {
                config.setBroadcastUri(userSpecified.getBroadcastUri());
                config.setState(State.Setup);
            }
        } else {
            String guestNetworkCidr = dc.getGuestNetworkCidr();
            String[] cidrTuple = guestNetworkCidr.split("\\/");
            config.setGateway(NetUtils.getIpRangeStartIpFromCidr(cidrTuple[0], Long.parseLong(cidrTuple[1])));
            config.setCidr(guestNetworkCidr);
            config.setDns1(dc.getDns1());
            config.setDns2(dc.getDns2());
        }
        
        return config;
    }

    @Override
    public void deallocate(Network config, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm) {
    }
    
    @Override
    public Network implement(Network config, NetworkOffering offering, DeployDestination dest, ReservationContext context) {
        assert (config.getState() == State.Allocated) : "Why implement are we implementing " + config;
        
        long dcId = dest.getDataCenter().getId();
        NetworkVO implemented = new NetworkVO(config.getTrafficType(), config.getGuestType(), config.getMode(), config.getBroadcastDomainType(), config.getNetworkOfferingId(), config.getDataCenterId());
        
        if (config.getBroadcastUri() == null) {
            String vnet = _dcDao.allocateVnet(dcId, config.getAccountId(), context.getReservationId());
            implemented.setBroadcastUri(BroadcastDomainType.Vlan.toUri(vnet));
        } else {
            implemented.setBroadcastUri(config.getBroadcastUri());
        }
        
        if (config.getGateway() != null) {
            implemented.setGateway(config.getGateway());
        }
        
        if (config.getCidr() != null) {
            implemented.setCidr(config.getCidr());
        }
        
        if (config.getDns1() != null) {
            implemented.setDns1(config.getDns1());
        }
        
        if (config.getDns2() != null) {
            implemented.setDns2(config.getDns2());
        }
        
        return implemented;
    }

    @Override
    public NicProfile allocate(Network config, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm)
            throws InsufficientVirtualNetworkCapcityException, InsufficientAddressCapacityException {
        if (config.getTrafficType() != TrafficType.Guest) {
            return null;
        }
        
        if (nic == null) {
            nic = new NicProfile(ReservationStrategy.Start, null, null, null, null);
        } else if (nic.getIp4Address() != null){
            nic.setStrategy(ReservationStrategy.Create);
        } else {
            nic.setStrategy(ReservationStrategy.Start);
        }
        
        if (nic.getMacAddress() == null) {
            nic.setMacAddress(_networkMgr.getNextAvailableMacAddressInNetwork(config.getId()));
            if (nic.getMacAddress() == null) {
                throw new InsufficientAddressCapacityException("Unable to allocate more mac addresses", Network.class, config.getId());
            }
        }
        
        return nic;
    }
    
    @DB
    protected String acquireGuestIpAddress(Network config) {
        List<String> ips = _nicDao.listIpAddressInNetworkConfiguration(config.getId());
        String[] cidr = config.getCidr().split("/");
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
    public void reserve(NicProfile nic, Network config, VirtualMachineProfile<? extends VirtualMachine> vm, DeployDestination dest, ReservationContext context) throws InsufficientVirtualNetworkCapcityException,
            InsufficientAddressCapacityException {
        assert (nic.getReservationStrategy() == ReservationStrategy.Start) : "What can I do for nics that are not allocated at start? ";

        nic.setBroadcastUri(config.getBroadcastUri());
        nic.setIsolationUri(config.getBroadcastUri());
        nic.setGateway(config.getGateway());
        nic.setIp4Address(acquireGuestIpAddress(config));
        nic.setNetmask(NetUtils.cidr2Netmask(config.getCidr()));
        nic.setDns1(config.getDns1());
        nic.setDns2(config.getDns2());
    }

    @Override
    public boolean release(NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm, String reservationId) {
        nic.setBroadcastUri(null);
        nic.setIsolationUri(null);
        return true;
    }

    @Override
    public void destroy(Network config, NetworkOffering offering) {
        config.getBroadcastUri();
    }

    @Override
    public boolean trash(Network config, NetworkOffering offering, Account owner) {
        return true;
    }
}
