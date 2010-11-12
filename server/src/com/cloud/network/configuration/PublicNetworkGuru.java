/**
 * 
 */
package com.cloud.network.configuration;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.dc.DataCenter;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
import com.cloud.network.Network.AddressFormat;
import com.cloud.network.Network.BroadcastDomainType;
import com.cloud.network.Network.IsolationType;
import com.cloud.network.Network.Mode;
import com.cloud.network.Network.TrafficType;
import com.cloud.network.NetworkConfiguration;
import com.cloud.network.NetworkConfigurationVO;
import com.cloud.network.NetworkManager;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.resource.Resource.ReservationStrategy;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
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
    public NetworkConfiguration design(NetworkOffering offering, DeploymentPlan plan, NetworkConfiguration config, Account owner) {
        if (offering.getTrafficType() != TrafficType.Public) {
            return null;
        }
        
        return new NetworkConfigurationVO(offering.getTrafficType(), offering.getGuestIpType(), Mode.Static, BroadcastDomainType.Vlan, offering.getId(), plan.getDataCenterId());
    }
    
    protected PublicNetworkGuru() {
        super();
    }
    
    protected void getIp(NicProfile nic, DataCenter dc, VirtualMachineProfile<? extends VirtualMachine> vm) throws InsufficientVirtualNetworkCapcityException {
        if (nic.getIp4Address() == null) {
            Pair<String, VlanVO> ipAndVlan = _vlanDao.assignIpAddress(dc.getId(), vm.getVirtualMachine().getAccountId(), vm.getVirtualMachine().getDomainId(), VlanType.VirtualNetwork, true);
            if (ipAndVlan == null) {
                throw new InsufficientVirtualNetworkCapcityException("Unable to get public ip address in " + dc.getId(), DataCenter.class, dc.getId());
            }
            VlanVO vlan = ipAndVlan.second();
            nic.setIp4Address(ipAndVlan.first());
            nic.setGateway(vlan.getVlanGateway());
            nic.setNetmask(vlan.getVlanNetmask());
            nic.setIsolationUri(IsolationType.Vlan.toUri(vlan.getVlanId()));
            nic.setBroadcastType(BroadcastDomainType.Vlan);
            nic.setBroadcastUri(BroadcastDomainType.Vlan.toUri(vlan.getVlanId()));
            nic.setFormat(AddressFormat.Ip4);
            nic.setReservationId(Long.toString(vlan.getId()));
        }
        nic.setDns1(dc.getDns1());
        nic.setDns2(dc.getDns2());
    }

    @Override
    public NicProfile allocate(NetworkConfiguration config, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm) throws InsufficientVirtualNetworkCapcityException,
            InsufficientAddressCapacityException {
        if (config.getTrafficType() != TrafficType.Public) {
            return null;
        }
        
        if (nic == null) {
            nic = new NicProfile(ReservationStrategy.Create, null, null, null, null);
        } else {
            nic.setStrategy(ReservationStrategy.Create);
        }
        
        String mac = _networkMgr.getNextAvailableMacAddressInNetwork(config.getId());
        nic.setMacAddress(mac);
        
        DataCenter dc = _dcDao.findById(config.getDataCenterId());
        getIp(nic, dc, vm);
        
        return nic;
    }

    @Override
    public void reserve(NicProfile nic, NetworkConfiguration configuration, VirtualMachineProfile<? extends VirtualMachine> vm, DeployDestination dest, ReservationContext context) throws InsufficientVirtualNetworkCapcityException, InsufficientAddressCapacityException {
        if (nic.getIp4Address() == null) {
            getIp(nic, dest.getDataCenter(), vm);
        }
    }

    @Override
    public boolean release(NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm, String reservationId) {
        _ipAddressDao.unassignIpAddress(reservationId);
        return true;
    }

    @Override
    public NetworkConfiguration implement(NetworkConfiguration config, NetworkOffering offering, DeployDestination destination, ReservationContext context) {
        return config;
    }
    
    @Override
    public void deallocate(NetworkConfiguration config, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm) {
    }
    
    @Override
    public void destroy(NetworkConfiguration config, NetworkOffering offering) {
    }

    @Override
    public boolean trash(NetworkConfiguration config, NetworkOffering offering, Account owner) {
        return true;
    }
}
