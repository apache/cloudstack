/**
 * 
 */
package com.cloud.network.configuration;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.dc.DataCenter;
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

    @Override
    public NetworkConfiguration design(NetworkOffering offering, DeploymentPlan plan, NetworkConfiguration userSpecified, Account owner) {
        TrafficType type = offering.getTrafficType();
        
        if (type != TrafficType.Management && type != TrafficType.Storage) {
            return null;
        }
        
        NetworkConfigurationVO config = new NetworkConfigurationVO(type, Mode.Static, BroadcastDomainType.Native, offering.getId(), plan.getDataCenterId());
        
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
        if (trafficType != TrafficType.Storage && trafficType != TrafficType.Management) {
            return null;
        }
        
        if (nic != null) {
            nic.setStrategy(ReservationStrategy.Start);
        } else {
            nic  = new NicProfile(ReservationStrategy.Start, null, null, null, null);
        } 
        
        return nic;
    }

    @Override
    public String reserve(NicProfile nic, NetworkConfiguration config, VirtualMachineProfile<? extends VirtualMachine> vm, DeployDestination dest, ReservationContext context) throws InsufficientVirtualNetworkCapcityException,
            InsufficientAddressCapacityException {
        DataCenter dc = dest.getDataCenter();
        Pod pod = dest.getPod();
        
        String ip = _dcDao.allocatePrivateIpAddress(dest.getDataCenter().getId(), dest.getPod().getId(), nic.getId());
        if (ip == null) {
            throw new InsufficientAddressCapacityException("Unable to get a management ip address");
        }
        
        String[] macs = _dcDao.getNextAvailableMacAddressPair(dc.getId());
        
        nic.setIp4Address(ip);
        nic.setGateway(pod.getGateway());
        nic.setMacAddress(macs[0]);
        nic.setFormat(AddressFormat.Ip4);
        String netmask = NetUtils.getCidrSubNet(pod.getCidrAddress(), pod.getCidrSize());
        nic.setNetmask(netmask);
        
        return Long.toString(nic.getId());
    }

    @Override
    public boolean release(String uniqueId) {
        _dcDao.releasePrivateIpAddress(Long.parseLong(uniqueId));
        return true;
    }

    @Override
    public NetworkConfiguration implement(NetworkConfiguration config, NetworkOffering offering, DeployDestination destination, ReservationContext context) {
        return config;
    }
    
    @Override
    public void destroy(NetworkConfiguration config, NetworkOffering offering) {
    }
}
