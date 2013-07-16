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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.ejb.Local;
import javax.inject.Inject;

import com.cloud.event.ActionEventUtils;
import com.cloud.server.ConfigurationServer;
import com.cloud.utils.Pair;

import org.apache.log4j.Logger;

import org.apache.cloudstack.context.CallContext;

import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Network;
import com.cloud.network.Network.State;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkModel;
import com.cloud.network.NetworkProfile;
import com.cloud.network.Networks.AddressFormat;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.Mode;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetwork.IsolationMethod;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.Ip4Address;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.Nic.ReservationStrategy;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.NicDao;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;

@Local(value = NetworkGuru.class)
public abstract class GuestNetworkGuru extends AdapterBase implements NetworkGuru {
    private static final Logger s_logger = Logger.getLogger(GuestNetworkGuru.class);
    @Inject
    protected NetworkManager _networkMgr;
    @Inject
    protected NetworkModel _networkModel;
    @Inject
    protected DataCenterDao _dcDao;
    @Inject
    protected VlanDao _vlanDao;
    @Inject
    protected NicDao _nicDao;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    protected NetworkDao _networkDao;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject 
    protected PhysicalNetworkDao _physicalNetworkDao;    
    @Inject
    ConfigurationServer _configServer;
    Random _rand = new Random(System.currentTimeMillis());

    private static final TrafficType[] _trafficTypes = {TrafficType.Guest};

    // Currently set to anything except STT for the Nicira integration.
    protected IsolationMethod[] _isolationMethods;
    
    String _defaultGateway;
    String _defaultCidr;

    protected GuestNetworkGuru() {
        super();
        _isolationMethods = null;
    }

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

    public boolean isMyIsolationMethod(PhysicalNetwork physicalNetwork) {
        if (physicalNetwork == null) {
            // Can't tell if there is no physical network
            return false;
        }
        
        List<String> methods = physicalNetwork.getIsolationMethods();
        if (methods.isEmpty()) {
            // The empty isolation method is assumed to be VLAN
            s_logger.debug("Empty physical isolation type for physical network " + physicalNetwork.getUuid());
            methods = new ArrayList<String>(1);
            methods.add("VLAN");
        }
        
        for (IsolationMethod m : _isolationMethods) {
            if (methods.contains(m.toString())) {
                return true;
            }
        }
        
        return false;
    }
    
    public IsolationMethod[] getIsolationMethods() {
        return _isolationMethods;
    }

    public boolean canUseSystemGuestVlan(long accountId) {
        return Boolean.parseBoolean(_configServer.getConfigValue(Config.UseSystemGuestVlans.key(),
            Config.ConfigurationParameterScope.account.toString(), accountId));
    }

    protected abstract boolean canHandle(NetworkOffering offering, final NetworkType networkType, PhysicalNetwork physicalNetwork);

    @Override
    public Network design(NetworkOffering offering, DeploymentPlan plan, Network userSpecified, Account owner) {
        DataCenter dc = _dcDao.findById(plan.getDataCenterId());
        PhysicalNetworkVO physnet = _physicalNetworkDao.findById(plan.getPhysicalNetworkId());

        if (!canHandle(offering, dc.getNetworkType(), physnet)) {
            return null;
        }

        NetworkVO network = new NetworkVO(offering.getTrafficType(), Mode.Dhcp, BroadcastDomainType.Vlan, offering.getId(),
                State.Allocated, plan.getDataCenterId(), plan.getPhysicalNetworkId());
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
                if (guestNetworkCidr != null) {
                    String[] cidrTuple = guestNetworkCidr.split("\\/");
                    network.setGateway(NetUtils.getIpRangeStartIpFromCidr(cidrTuple[0], Long.parseLong(cidrTuple[1])));
                    network.setCidr(guestNetworkCidr);
                } else if (dc.getNetworkType() == NetworkType.Advanced) {
                    throw new CloudRuntimeException("Can't design network " + network + "; guest CIDR is not configured per zone " + dc);
                }
            }

            if (offering.getSpecifyVlan()) {
                network.setBroadcastUri(userSpecified.getBroadcastUri());
                network.setState(State.Setup);
            }
        } else {
            String guestNetworkCidr = dc.getGuestNetworkCidr();
            if (guestNetworkCidr == null && dc.getNetworkType() == NetworkType.Advanced) {
                throw new CloudRuntimeException("Can't design network " + network + "; guest CIDR is not configured per zone " + dc);
            }
            String[] cidrTuple = guestNetworkCidr.split("\\/");
            network.setGateway(NetUtils.getIpRangeStartIpFromCidr(cidrTuple[0], Long.parseLong(cidrTuple[1])));
            network.setCidr(guestNetworkCidr);
        }

        return network;
    }

    @Override @DB
    public void deallocate(Network network, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm) {
        if (network.getSpecifyIpRanges()) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Deallocate network: networkId: " + nic.getNetworkId() + ", ip: " + nic.getIp4Address());
            }

            IPAddressVO ip = _ipAddressDao.findByIpAndSourceNetworkId(nic.getNetworkId(), nic.getIp4Address());
            if (ip != null) {
                Transaction txn = Transaction.currentTxn();
                txn.start();
                _networkMgr.markIpAsUnavailable(ip.getId());
                _ipAddressDao.unassignIpAddress(ip.getId());
                txn.commit();
            }
            nic.deallocate();
        }
    }
    

    public int getVlanOffset(long physicalNetworkId, int vlanTag) {
        PhysicalNetworkVO pNetwork = _physicalNetworkDao.findById(physicalNetworkId);
        if (pNetwork == null) {
            throw new CloudRuntimeException("Could not find the physical Network " + physicalNetworkId + ".");
        }

        if (pNetwork.getVnet() == null) {
            throw new CloudRuntimeException("Could not find vlan range for physical Network " + physicalNetworkId + ".");
        }
        Integer lowestVlanTag = null;
        List<Pair<Integer, Integer>> vnetList = pNetwork.getVnet();
        //finding the vlanrange in which the vlanTag lies.
        for (Pair <Integer,Integer> vnet : vnetList){
            if (vlanTag >= vnet.first() && vlanTag <= vnet.second()){
                lowestVlanTag = vnet.first();
            }
        }
        if (lowestVlanTag == null) {
            throw new InvalidParameterValueException ("The vlan tag does not belong to any of the existing vlan ranges");
        }
        return vlanTag - lowestVlanTag;
    }

    public int getGloballyConfiguredCidrSize() {
        try {
            String globalVlanBits = _configDao.getValue(Config.GuestVlanBits.key());
            return 8 + Integer.parseInt(globalVlanBits);
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to read the globally configured VLAN bits size.");
        }
    }

    protected void allocateVnet(Network network, NetworkVO implemented, long dcId,
    		long physicalNetworkId, String reservationId) throws InsufficientVirtualNetworkCapcityException {
        if (network.getBroadcastUri() == null) {
            String vnet = _dcDao.allocateVnet(dcId, physicalNetworkId, network.getAccountId(), reservationId,
                    canUseSystemGuestVlan(network.getAccountId()));
            if (vnet == null) {
                throw new InsufficientVirtualNetworkCapcityException("Unable to allocate vnet as a " +
                		"part of network " + network + " implement ", DataCenter.class, dcId);
            }
            implemented.setBroadcastUri(BroadcastDomainType.Vlan.toUri(vnet));
            ActionEventUtils.onCompletedActionEvent(CallContext.current().getCallingUserId(), network.getAccountId(),
                    EventVO.LEVEL_INFO, EventTypes.EVENT_ZONE_VLAN_ASSIGN, "Assigned Zone Vlan: " + vnet + " Network Id: " + network.getId(), 0);
        } else {
            implemented.setBroadcastUri(network.getBroadcastUri());
        }
    }
    
    @Override
    public Network implement(Network network, NetworkOffering offering, DeployDestination dest, 
            ReservationContext context) throws InsufficientVirtualNetworkCapcityException {
        assert (network.getState() == State.Implementing) : "Why are we implementing " + network;

        long dcId = dest.getDataCenter().getId();

        //get physical network id
        Long physicalNetworkId = network.getPhysicalNetworkId();
        
       // physical network id can be null in Guest Network in Basic zone, so locate the physical network
       if (physicalNetworkId == null) {        
           physicalNetworkId = _networkModel.findPhysicalNetworkId(dcId, offering.getTags(), offering.getTrafficType());
       }

        NetworkVO implemented = new NetworkVO(network.getTrafficType(), network.getMode(), 
                network.getBroadcastDomainType(), network.getNetworkOfferingId(), State.Allocated,
                network.getDataCenterId(), physicalNetworkId);

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
    public NicProfile allocate(Network network, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm)
            throws InsufficientVirtualNetworkCapcityException,
    InsufficientAddressCapacityException {

        assert (network.getTrafficType() == TrafficType.Guest) : "Look at my name!  Why are you calling" +
        		" me when the traffic type is : " + network.getTrafficType();

        if (nic == null) {
            nic = new NicProfile(ReservationStrategy.Start, null, null, null, null);
        }

        DataCenter dc = _dcDao.findById(network.getDataCenterId());

        if (nic.getIp4Address() == null) {
            nic.setBroadcastUri(network.getBroadcastUri());
            nic.setIsolationUri(network.getBroadcastUri());
            nic.setGateway(network.getGateway()); 

            String guestIp = null;
            if (network.getSpecifyIpRanges()) {
                _networkMgr.allocateDirectIp(nic, dc, vm, network, nic.getRequestedIpv4(), null);
            } else {
                //if Vm is router vm and source nat is enabled in the network, set ip4 to the network gateway
                boolean isGateway = false;
                if (vm.getVirtualMachine().getType() == VirtualMachine.Type.DomainRouter) {
                    if (network.getVpcId() != null) {
                        if (_networkModel.isProviderSupportServiceInNetwork(network.getId(), Service.SourceNat, Provider.VPCVirtualRouter)) {
                            isGateway = true;
                        }
                    } else {
                        if (_networkModel.isProviderSupportServiceInNetwork(network.getId(), Service.SourceNat, Provider.VirtualRouter)) {
                            isGateway = true;
                        }
                    }
                }
                
                if (isGateway) {
                    guestIp = network.getGateway();
                } else {
                    guestIp = _networkMgr.acquireGuestIpAddress(network, nic.getRequestedIpv4());
                    if (guestIp == null) {
                        throw new InsufficientVirtualNetworkCapcityException("Unable to acquire Guest IP" +
                                " address for network " + network, DataCenter.class, dc.getId());
                    }
                }

                nic.setIp4Address(guestIp);
                nic.setNetmask(NetUtils.cidr2Netmask(network.getCidr()));

                nic.setDns1(dc.getDns1());
                nic.setDns2(dc.getDns2());
                nic.setFormat(AddressFormat.Ip4);
            }
        }

        nic.setStrategy(ReservationStrategy.Start);

        if (nic.getMacAddress() == null) {
            nic.setMacAddress(_networkModel.getNextAvailableMacAddressInNetwork(network.getId()));
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
    public void reserve(NicProfile nic, Network network, VirtualMachineProfile<? extends VirtualMachine> vm,
            DeployDestination dest, ReservationContext context)
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
        
        if (profile.getBroadcastDomainType() == BroadcastDomainType.Vlan && 
        		profile.getBroadcastUri() != null && !offering.getSpecifyVlan()) {
        s_logger.debug("Releasing vnet for the network id=" + profile.getId());
            _dcDao.releaseVnet(profile.getBroadcastUri().getHost(), profile.getDataCenterId(), 
                    profile.getPhysicalNetworkId(), profile.getAccountId(), profile.getReservationId());
            ActionEventUtils.onCompletedActionEvent(CallContext.current().getCallingUserId(), profile.getAccountId(),
                    EventVO.LEVEL_INFO, EventTypes.EVENT_ZONE_VLAN_RELEASE, "Released Zone Vlan: "
                    + profile.getBroadcastUri().getHost() + " for Network: " + profile.getId(), 0);
        }
        profile.setBroadcastUri(null);
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
