/**
 * 
 */
package com.cloud.network.guru;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
import com.cloud.network.IPAddressVO;
import com.cloud.network.Network;
import com.cloud.network.Network.State;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkProfile;
import com.cloud.network.NetworkVO;
import com.cloud.network.Networks.AddressFormat;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.IsolationType;
import com.cloud.network.Networks.Mode;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.user.Account;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.Inject;
import com.cloud.vm.Nic.ReservationStrategy;
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
    @Inject NetworkOfferingDao _networkOfferingDao;
    
    
    protected boolean canHandle(NetworkOffering offering, DataCenter dc) {
        if (dc.getNetworkType() == NetworkType.Advanced && offering.getTrafficType() == TrafficType.Public && offering.isSystemOnly()) {
            return true;
        } else {
            s_logger.trace("We only take care of System only Public Virtual Network");
            return false;
        }
    }

    @Override
    public Network design(NetworkOffering offering, DeploymentPlan plan, Network network, Account owner) {
        DataCenter dc = _dcDao.findById(plan.getDataCenterId());
        
        if (!canHandle(offering, dc)) {
            return null;
        }
        
        if (offering.getTrafficType() == TrafficType.Public) {
            NetworkVO ntwk = new NetworkVO(offering.getTrafficType(), null, Mode.Static, BroadcastDomainType.Vlan, offering.getId(), plan.getDataCenterId(), State.Setup);
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
            PublicIp ip = _networkMgr.assignPublicIpAddress(dc.getId(), null, vm.getOwner(), VlanType.VirtualNetwork, null);
            nic.setIp4Address(ip.getAddress().toString());
            nic.setGateway(ip.getGateway());
            nic.setNetmask(ip.getNetmask()); 
            nic.setIsolationUri(IsolationType.Vlan.toUri(ip.getVlanTag()));
            nic.setBroadcastUri(IsolationType.Vlan.toUri(ip.getVlanTag()));
            nic.setBroadcastType(BroadcastDomainType.Vlan);
            nic.setFormat(AddressFormat.Ip4);
            nic.setReservationId(String.valueOf(ip.getVlanTag()));
            nic.setMacAddress(ip.getMacAddress());
        }
        
        nic.setDns1(dc.getDns1());
        nic.setDns2(dc.getDns2());
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
    public NicProfile allocate(Network network, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm) throws InsufficientVirtualNetworkCapcityException,
            InsufficientAddressCapacityException, ConcurrentOperationException {
        
        DataCenter dc = _dcDao.findById(network.getDataCenterId());
        NetworkOffering offering = _networkOfferingDao.findByIdIncludingRemoved(network.getNetworkOfferingId());
        if (!canHandle(offering, dc)) {
            return null;
        }
        
        if (nic == null) {
            nic = new NicProfile(ReservationStrategy.Create, null, null, null, null);
        }
        
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
        IPAddressVO ip = _ipAddressDao.findByAccountAndIp(vm.getVirtualMachine().getAccountId(), nic.getIp4Address());
        if (ip != null) {
            _networkMgr.unassignPublicIpAddress(ip);
        }
        nic.deallocate();
    }
    
    @Override
    public void destroy(Network network, NetworkOffering offering) {
    }

    @Override
    public boolean trash(Network network, NetworkOffering offering, Account owner) {
        return true;
    }
    
    @Override
    public void updateNetworkProfile(NetworkProfile networkProfile) {
        DataCenter dc = _dcDao.findById(networkProfile.getNetwork().getDataCenterId());
        networkProfile.setDns1(dc.getDns1());
        networkProfile.setDns2(dc.getDns2());
    }
}
