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
import com.cloud.network.Network.BroadcastDomainType;
import com.cloud.network.Network.Mode;
import com.cloud.network.Network.TrafficType;
import com.cloud.network.NetworkConfiguration;
import com.cloud.network.NetworkConfigurationVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.Inject;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.NetworkConcierge;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachine;

@Local(value={NetworkGuru.class, NetworkConcierge.class})
public class PodBasedNetworkGuru extends AdapterBase implements NetworkGuru, NetworkConcierge {
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
    public String getUniqueName() {
        return getName();
    }

    @Override
    public NicProfile allocate(VirtualMachine vm, NetworkConfiguration config, NicProfile nic) throws InsufficientVirtualNetworkCapcityException,
            InsufficientAddressCapacityException {
        TrafficType trafficType = config.getTrafficType();
        if (trafficType != TrafficType.Storage && trafficType != TrafficType.Management) {
            return null;
        }
        
        if (nic != null) {
            throw new CloudRuntimeException("Does not support nic configuration");
        }
        
        NicProfile profile = new NicProfile(null, null, null);
        return profile;
    }

    @Override
    public boolean create(Nic nic) throws InsufficientVirtualNetworkCapcityException, InsufficientAddressCapacityException {
        return true;
    }

    @Override
    public String reserve(VirtualMachine vm, NicProfile nic, DeployDestination dest) throws InsufficientVirtualNetworkCapcityException,
            InsufficientAddressCapacityException {
        DataCenter dc = dest.getDataCenter();
        Pod pod = dest.getPod();
        
        String ip = _dcDao.allocatePrivateIpAddress(dest.getDataCenter().getId(), dest.getPod().getId(), nic.getId());
        String[] macs = _dcDao.getNextAvailableMacAddressPair(dc.getId());
        
        nic.setIp4Address(ip);
        nic.setCidr(pod.getCidrAddress() + "/" + pod.getCidrSize());
        nic.setGateway(pod.getGateway());
        nic.setMacAddress(macs[0]);
        String netmask = NetUtils.getCidrSubNet(pod.getCidrAddress(), pod.getCidrSize());
        nic.setNetmask(netmask);
        
        return Long.toString(nic.getId());
    }

    @Override
    public boolean release(String uniqueName, String uniqueId) {
        _dcDao.releasePrivateIpAddress(Long.parseLong(uniqueId));
        return true;
    }

    @Override
    public NetworkConfiguration implement(NetworkConfiguration config, NetworkOffering offering, DeployDestination destination) {
        return config;
    }

}
