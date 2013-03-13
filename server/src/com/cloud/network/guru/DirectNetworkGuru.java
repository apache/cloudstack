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

import java.util.List;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.log4j.Logger;

import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Ipv6AddressManager;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.Service;
import com.cloud.network.Network.State;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkModel;
import com.cloud.network.NetworkProfile;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.Mode;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.UserIpv6AddressVO;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.UserIpv6AddressDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.user.Account;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.vm.Nic.ReservationStrategy;
import com.cloud.vm.dao.NicSecondaryIpDao;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicSecondaryIp;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

@Local(value = { NetworkGuru.class })
public class DirectNetworkGuru extends AdapterBase implements NetworkGuru {
    private static final Logger s_logger = Logger.getLogger(DirectNetworkGuru.class);

    @Inject
    DataCenterDao _dcDao;
    @Inject
    VlanDao _vlanDao;
    @Inject
    NetworkModel _networkModel;
    @Inject
    NetworkManager _networkMgr;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    NetworkOfferingDao _networkOfferingDao;
    @Inject
    UserIpv6AddressDao _ipv6Dao;
    @Inject
    Ipv6AddressManager _ipv6Mgr;
    @Inject
    NicSecondaryIpDao _nicSecondaryIpDao;

    private static final TrafficType[] _trafficTypes = {TrafficType.Guest};
    
    @Override
    public boolean isMyTrafficType(TrafficType type) {
    	for (TrafficType t : _trafficTypes) {
    		if (t == type) {
    			return true;
    		}
    	}
    	return false;
    }

    @Override
    public TrafficType[] getSupportedTrafficType() {
    	return _trafficTypes;
    }
    
    protected boolean canHandle(NetworkOffering offering, DataCenter dc) {
        // this guru handles only Guest networks in Advance zone with source nat service disabled
        if (dc.getNetworkType() == NetworkType.Advanced && isMyTrafficType(offering.getTrafficType()) && offering.getGuestType() == GuestType.Shared) {
            return true;
        } else {
            s_logger.trace("We only take care of Guest networks of type " + GuestType.Shared);
            return false;
        }
    }

    @Override
    public Network design(NetworkOffering offering, DeploymentPlan plan, Network userSpecified, Account owner) {
        DataCenter dc = _dcDao.findById(plan.getDataCenterId());

        if (!canHandle(offering, dc)) {
            return null;
        }

        State state = State.Allocated;
        if (dc.getNetworkType() == NetworkType.Basic) {
            state = State.Setup;
        }

        NetworkVO config = new NetworkVO(offering.getTrafficType(), Mode.Dhcp, BroadcastDomainType.Vlan, offering.getId(), state, plan.getDataCenterId(), plan.getPhysicalNetworkId());

        if (userSpecified != null) {
            if ((userSpecified.getCidr() == null && userSpecified.getGateway() != null) || (userSpecified.getCidr() != null && userSpecified.getGateway() == null)) {
                throw new InvalidParameterValueException("cidr and gateway must be specified together.");
            }

            if ((userSpecified.getIp6Cidr() == null && userSpecified.getIp6Gateway() != null) || (userSpecified.getIp6Cidr() != null && userSpecified.getIp6Gateway() == null)) {
                throw new InvalidParameterValueException("cidrv6 and gatewayv6 must be specified together.");
            }

            if (userSpecified.getCidr() != null) {
                config.setCidr(userSpecified.getCidr());
                config.setGateway(userSpecified.getGateway());
            }

            if (userSpecified.getIp6Cidr() != null) {
                config.setIp6Cidr(userSpecified.getIp6Cidr());
                config.setIp6Gateway(userSpecified.getIp6Gateway());
            }

            if (userSpecified.getBroadcastUri() != null) {
                config.setBroadcastUri(userSpecified.getBroadcastUri());
                config.setState(State.Setup);
            }

            if (userSpecified.getBroadcastDomainType() != null) {
                config.setBroadcastDomainType(userSpecified.getBroadcastDomainType());
            }
        }

        boolean isSecurityGroupEnabled = _networkModel.areServicesSupportedByNetworkOffering(offering.getId(), Service.SecurityGroup);
        if (isSecurityGroupEnabled) {
        	if (userSpecified.getIp6Cidr() != null) {
                throw new InvalidParameterValueException("Didn't support security group with IPv6");
        	}
            config.setName("SecurityGroupEnabledNetwork");
            config.setDisplayText("SecurityGroupEnabledNetwork");
        }

        return config;
    }

    protected DirectNetworkGuru() {
        super();
    }

    @Override
    public void updateNicProfile(NicProfile profile, Network network) {
        DataCenter dc = _dcDao.findById(network.getDataCenterId());
        if (profile != null) {
            profile.setDns1(dc.getDns1());
            profile.setDns2(dc.getDns2());
            profile.setIp6Dns1(dc.getIp6Dns1());
            profile.setIp6Dns2(dc.getIp6Dns2());
        }
    }

    @Override
    public NicProfile allocate(Network network, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm) throws InsufficientVirtualNetworkCapcityException,
            InsufficientAddressCapacityException, ConcurrentOperationException {

        DataCenter dc = _dcDao.findById(network.getDataCenterId());    

        if (nic == null) {
            nic = new NicProfile(ReservationStrategy.Create, null, null, null, null);
        } else if (nic.getIp4Address() == null && nic.getIp6Address() == null) {
            nic.setStrategy(ReservationStrategy.Start);
        } else {
            nic.setStrategy(ReservationStrategy.Create);
        }

        _networkMgr.allocateDirectIp(nic, dc, vm, network, nic.getRequestedIpv4(), nic.getRequestedIpv6());
        nic.setStrategy(ReservationStrategy.Create);

        return nic;
    }

    @Override
    public void reserve(NicProfile nic, Network network, VirtualMachineProfile<? extends VirtualMachine> vm, DeployDestination dest, ReservationContext context)
            throws InsufficientVirtualNetworkCapcityException, InsufficientAddressCapacityException, ConcurrentOperationException {
        if (nic.getIp4Address() == null && nic.getIp6Address() == null) {
            _networkMgr.allocateDirectIp(nic, dest.getDataCenter(), vm, network, null, null);
            nic.setStrategy(ReservationStrategy.Create);
        }
    }

    @Override
    public boolean release(NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm, String reservationId) {
        return true;
    }

    @Override
    public Network implement(Network network, NetworkOffering offering, DeployDestination destination, ReservationContext context) throws InsufficientVirtualNetworkCapcityException {
        return network;
    }

    @Override @DB
    public void deallocate(Network network, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm) {
    	if (s_logger.isDebugEnabled()) {
            s_logger.debug("Deallocate network: networkId: " + nic.getNetworkId() + ", ip: " + nic.getIp4Address());
        }
    	
    	if (nic.getIp4Address() != null) {
        IPAddressVO ip = _ipAddressDao.findByIpAndSourceNetworkId(nic.getNetworkId(), nic.getIp4Address());
        if (ip != null) {
            Transaction txn = Transaction.currentTxn();
            txn.start();
            _networkMgr.markIpAsUnavailable(ip.getId());
            _ipAddressDao.unassignIpAddress(ip.getId());
            //unassign nic secondary ip address
            s_logger.debug("remove nic " + nic.getId() + " secondary ip ");
            List<String> nicSecIps = null;
            nicSecIps = _nicSecondaryIpDao.getSecondaryIpAddressesForNic(nic.getId());
            for (String secIp: nicSecIps) {
                IPAddressVO pubIp = _ipAddressDao.findByIpAndSourceNetworkId(nic.getNetworkId(), secIp);
                _networkMgr.markIpAsUnavailable(pubIp.getId());
                _ipAddressDao.unassignIpAddress(pubIp.getId());
            }

            txn.commit();
        }
    	}
    	
    	if (nic.getIp6Address() != null) {
    		_ipv6Mgr.revokeDirectIpv6Address(nic.getNetworkId(), nic.getIp6Address());
    	}
        nic.deallocate();
    }

    @Override
    public void shutdown(NetworkProfile network, NetworkOffering offering) {
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
