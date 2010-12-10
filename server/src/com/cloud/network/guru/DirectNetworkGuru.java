/**
 * 
 */
package com.cloud.network.guru;

import java.util.Random;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Network;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkVO;
import com.cloud.network.Network.State;
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
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

@Local(value={NetworkGuru.class})
public class DirectNetworkGuru extends AdapterBase implements NetworkGuru {
    private static final Logger s_logger = Logger.getLogger(DirectNetworkGuru.class);
    
    @Inject DataCenterDao _dcDao;
    @Inject VlanDao _vlanDao;
    @Inject NetworkManager _networkMgr;
    @Inject IPAddressDao _ipAddressDao;
    Random _rand = new Random(System.currentTimeMillis());

    @Override
    public Network design(NetworkOffering offering, DeploymentPlan plan, Network userSpecified, Account owner) {
        if (!(offering.getTrafficType() == TrafficType.Public && offering.getGuestIpType() == GuestIpType.Direct)) {
            s_logger.trace("We only take care of public direct network, so this is no ours");
            return null;
        }
        
        NetworkVO config = new NetworkVO(offering.getTrafficType(), offering.getGuestIpType(), Mode.Dhcp, BroadcastDomainType.Vlan, offering.getId(), plan.getDataCenterId());
        DataCenterVO dc = _dcDao.findById(plan.getDataCenterId());

        if (userSpecified != null) {
            if ((userSpecified.getCidr() == null && userSpecified.getGateway() != null) ||
                (userSpecified.getCidr() != null && userSpecified.getGateway() == null)) {
                throw new InvalidParameterValueException("cidr and gateway must be specified together.");
            }
            
            if (userSpecified.getCidr() != null) {
                config.setCidr(userSpecified.getCidr());
                config.setGateway(userSpecified.getGateway());
            } 
            
            if (userSpecified.getBroadcastUri() != null) {
                config.setBroadcastUri(userSpecified.getBroadcastUri());
                config.setState(State.Setup);
            }
        }
        
        config.setDns1(dc.getDns1());
        config.setDns2(dc.getDns2());
        
       return config;
    }
    
    protected DirectNetworkGuru() {
        super();
    }
    
    protected void getIp(NicProfile nic, DataCenter dc, VirtualMachineProfile<? extends VirtualMachine> vm, Network network) throws InsufficientVirtualNetworkCapcityException, InsufficientAddressCapacityException, ConcurrentOperationException {
        if (nic.getIp4Address() == null) {
            PublicIp ip = _networkMgr.fetchNewPublicIp(dc.getId(), VlanType.DirectAttached, vm.getOwner(), network.getId(), false);
            nic.setIp4Address(ip.getAddress());
            nic.setGateway(ip.getGateway());
            nic.setNetmask(ip.getNetmask());
            nic.setIsolationUri(IsolationType.Vlan.toUri(ip.getVlanTag()));
            nic.setBroadcastType(BroadcastDomainType.Vlan);
            nic.setBroadcastUri(BroadcastDomainType.Vlan.toUri(ip.getVlanTag()));
            nic.setFormat(AddressFormat.Ip4);
            nic.setReservationId(ip.getVlanTag());
            nic.setMacAddress(NetUtils.long2Mac(ip.getMacAddress() | 0x060000000000l | (((long)_rand.nextInt(32768) << 25) & 0x00fffe000000l)));
        }
        nic.setDns1(dc.getDns1());
        nic.setDns2(dc.getDns2());
    }

    @Override
    public NicProfile allocate(Network network, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm) throws InsufficientVirtualNetworkCapcityException,
            InsufficientAddressCapacityException, ConcurrentOperationException {
       
        if (nic == null) {
            nic = new NicProfile(ReservationStrategy.Create, null, null, null, null);
        } else if (nic.getIp4Address() == null) {
            nic.setStrategy(ReservationStrategy.Start);
        } else {
            nic.setStrategy(ReservationStrategy.Create);
        }
        
        DataCenter dc = _dcDao.findById(network.getDataCenterId());
        getIp(nic, dc, vm, network);

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
    }
    
    @Override
    public void destroy(Network network, NetworkOffering offering) {
    }

    @Override
    public boolean trash(Network network, NetworkOffering offering, Account owner) {
        return true;
    }
}
