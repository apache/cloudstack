/**
 * 
 */
package com.cloud.network.guru;

import java.net.URI;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
import com.cloud.network.Network;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkVO;
import com.cloud.network.Networks.AddressFormat;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.IsolationType;
import com.cloud.network.Networks.Mode;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.NetworkOffering.GuestIpType;
import com.cloud.resource.Resource.ReservationStrategy;
import com.cloud.user.Account;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.Inject;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

@Local(value={NetworkGuru.class})
public class PublicNetworkGuru extends AdapterBase implements NetworkGuru {
    private static final Logger s_logger = Logger.getLogger(PublicNetworkGuru.class);
    
    @Inject DataCenterDao _dcDao;
    @Inject VlanDao _vlanDao;
    @Inject NetworkManager _networkMgr;
    @Inject IPAddressDao _ipAddressDao;

    @Override
    public Network design(NetworkOffering offering, DeploymentPlan plan, Network network, Account owner) {
        if (offering.getTrafficType() != TrafficType.Public || (offering.getGuestIpType() != null && offering.getGuestIpType() != GuestIpType.Virtual)) {
            s_logger.trace("We only take care of Public Virtual Network");
            return null;
        }
        
        if (offering.getTrafficType() == TrafficType.Public) {
            NetworkVO ntwk = new NetworkVO(offering.getTrafficType(), offering.getGuestIpType(), Mode.Static, BroadcastDomainType.Vlan, offering.getId(), plan.getDataCenterId());
            DataCenterVO dc = _dcDao.findById(plan.getDataCenterId());
            ntwk.setDns1(dc.getDns1());
            ntwk.setDns2(dc.getDns2());
            return ntwk;
        } else {
            return null;
        }
    }
    
    protected PublicNetworkGuru() {
        super();
    }
    
    protected void getIp(NicProfile nic, DataCenter dc, VirtualMachineProfile<? extends VirtualMachine> vm, Network network) throws InsufficientVirtualNetworkCapcityException, InsufficientAddressCapacityException, ConcurrentOperationException {
        if (nic.getIp4Address() == null) {
            PublicIp ip = _networkMgr.assignPublicIpAddress(dc.getId(), vm.getOwner(), dc.getNetworkType().equals(NetworkType.Basic) ?  VlanType.DirectAttached : VlanType.VirtualNetwork, null);
            nic.setIp4Address(ip.getAddress());
            nic.setGateway(ip.getGateway());
            nic.setNetmask(ip.getNetmask());
            if(ip.getVlanTag() != null && ip.getVlanTag().equalsIgnoreCase("untagged")) {
                nic.setIsolationUri(URI.create("vlan://untagged"));
                nic.setBroadcastUri(URI.create("vlan://untagged"));
                nic.setBroadcastType(BroadcastDomainType.Native);
            } else if (ip.getVlanTag() != null){
                nic.setIsolationUri(IsolationType.Vlan.toUri(ip.getVlanTag()));
                nic.setBroadcastUri(IsolationType.Vlan.toUri(ip.getVlanTag()));
                nic.setBroadcastType(BroadcastDomainType.Vlan);
            }
            	
            nic.setFormat(AddressFormat.Ip4);
            nic.setReservationId(String.valueOf(ip.getVlanTag()));
            nic.setMacAddress(ip.getMacAddress());
        }
        nic.setDns1(dc.getDns1());
        nic.setDns2(dc.getDns2());
    }

    @Override
    public NicProfile allocate(Network network, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm) throws InsufficientVirtualNetworkCapcityException,
            InsufficientAddressCapacityException, ConcurrentOperationException {
        if (network.getTrafficType() != TrafficType.Public) {
            return null;
        }
        
        if (nic == null) {
            nic = new NicProfile(ReservationStrategy.Create, null, null, null, null);
        }
        
        DataCenter dc = _dcDao.findById(network.getDataCenterId());
        getIp(nic, dc, vm, network);
        
        if (nic.getIp4Address() == null) {
            nic.setStrategy(ReservationStrategy.Start);
        } else {
            nic.setStrategy(ReservationStrategy.Create);
        }
        
        return nic;
    }

    @Override
    public void reserve(NicProfile nic, Network network, VirtualMachineProfile<? extends VirtualMachine> vm, DeployDestination dest, ReservationContext context) throws InsufficientVirtualNetworkCapcityException, InsufficientAddressCapacityException, ConcurrentOperationException {
        if (nic.getIp4Address() == null) {
            getIp(nic, dest.getDataCenter(), vm, network);
        } 
    }

    @Override
    public boolean release(NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm, String reservationId) {
        return true;
    }

    @Override
    public Network implement(Network network, NetworkOffering offering, DeployDestination destination, ReservationContext context) {
        return network;
    }
    
    @Override
    public void deallocate(Network network, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm) {
        _ipAddressDao.unassignIpAddress(nic.getIp4Address());
        nic.deallocate();
    }
    
    @Override
    public void destroy(Network network, NetworkOffering offering) {
    }

    @Override
    public boolean trash(Network network, NetworkOffering offering, Account owner) {
        return true;
    }
}
