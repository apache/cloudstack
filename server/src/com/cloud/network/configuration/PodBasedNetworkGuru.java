/**
 * 
 */
package com.cloud.network.configuration;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.dc.Pod;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
import com.cloud.network.Network.AddressFormat;
import com.cloud.network.Network.BroadcastDomainType;
import com.cloud.network.Network.Mode;
import com.cloud.network.Network.TrafficType;
import com.cloud.network.NetworkConfiguration;
import com.cloud.network.NetworkConfigurationVO;
import com.cloud.network.NetworkManager;
import com.cloud.offering.NetworkOffering;
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
public class PodBasedNetworkGuru extends AdapterBase implements NetworkGuru {
    private static final Logger s_logger = Logger.getLogger(PodBasedNetworkGuru.class);
    @Inject DataCenterDao _dcDao;
    @Inject NetworkManager _networkMgr;

    @Override
    public NetworkConfiguration design(NetworkOffering offering, DeploymentPlan plan, NetworkConfiguration userSpecified, Account owner) {
        TrafficType type = offering.getTrafficType();
        
        if (type != TrafficType.Management && type != TrafficType.Storage) {
            return null;
        }
        
        NetworkConfigurationVO config = new NetworkConfigurationVO(type, offering.getGuestIpType(), Mode.Static, BroadcastDomainType.Native, offering.getId(), plan.getDataCenterId());
        
        return config;
    }
    
    protected PodBasedNetworkGuru() {
        super();
    }

    @Override
    public void deallocate(NetworkConfiguration config, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm) {
    }
    
    @Override
    public NicProfile allocate(NetworkConfiguration config, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm) throws InsufficientVirtualNetworkCapcityException,
            InsufficientAddressCapacityException {
        TrafficType trafficType = config.getTrafficType();
        assert (trafficType == TrafficType.Storage || trafficType == TrafficType.Management) : "Well, I can't take care of this config now can I? " + config; 
        
        if (nic != null) {
            nic.setStrategy(ReservationStrategy.Start);
        } else {
            nic  = new NicProfile(ReservationStrategy.Start, null, null, null, null);
        } 
        
        String mac = _networkMgr.getNextAvailableMacAddressInNetwork(config.getId());
        nic.setMacAddress(mac);
        
        return nic;
    }

    @Override
    public void reserve(NicProfile nic, NetworkConfiguration config, VirtualMachineProfile<? extends VirtualMachine> vm, DeployDestination dest, ReservationContext context) throws InsufficientVirtualNetworkCapcityException,
            InsufficientAddressCapacityException {
        Pod pod = dest.getPod();
        
        String ip = _dcDao.allocatePrivateIpAddress(dest.getDataCenter().getId(), dest.getPod().getId(), nic.getId(), context.getReservationId());
        if (ip == null) {
            throw new InsufficientAddressCapacityException("Unable to get a management ip address", Pod.class, pod.getId());
        }
        
        nic.setIp4Address(ip);
        nic.setGateway(pod.getGateway());
        nic.setFormat(AddressFormat.Ip4);
        String netmask = NetUtils.getCidrSubNet(pod.getCidrAddress(), pod.getCidrSize());
        nic.setNetmask(netmask);
    }

    @Override
    public boolean release(NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm, String reservationId) {
        _dcDao.releasePrivateIpAddress(nic.getId(), reservationId);
        
        return true;
    }

    @Override
    public NetworkConfiguration implement(NetworkConfiguration config, NetworkOffering offering, DeployDestination destination, ReservationContext context) {
        return config;
    }
    
    @Override
    public void destroy(NetworkConfiguration config, NetworkOffering offering) {
    }
    
    @Override
    public boolean trash(NetworkConfiguration config, NetworkOffering offering, Account owner) {
        return true;
    }
}
