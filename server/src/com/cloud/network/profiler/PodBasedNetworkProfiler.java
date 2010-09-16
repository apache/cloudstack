/**
 * 
 */
package com.cloud.network.profiler;

import java.util.Map;

import javax.ejb.Local;

import org.apache.log4j.Logger;

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
import com.cloud.network.NetworkProfiler;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.Inject;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NetworkConcierge;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachine;

@Local(value={NetworkProfiler.class, NetworkConcierge.class})
public class PodBasedNetworkProfiler extends AdapterBase implements NetworkProfiler, NetworkConcierge {
    private static final Logger s_logger = Logger.getLogger(PodBasedNetworkProfiler.class);
    @Inject DataCenterDao _dcDao;

    @Override
    public NetworkConfiguration convert(NetworkOffering offering, DeploymentPlan plan, Map<String, String> params, Account owner) {
        TrafficType type = offering.getTrafficType();
        
        if (type != TrafficType.Management && type != TrafficType.Storage) {
            return null;
        }
        
        NetworkConfigurationVO config = new NetworkConfigurationVO(type, Mode.Static, BroadcastDomainType.Native, offering.getId(), plan.getDataCenterId());
        
        return config;
    }
    
    protected PodBasedNetworkProfiler() {
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
    public String reserve(long vmId, NicProfile nic, DeployDestination dest) throws InsufficientVirtualNetworkCapcityException,
            InsufficientAddressCapacityException {
        Pod pod = dest.getPod();
        String ip = _dcDao.allocatePrivateIpAddress(dest.getDataCenter().getId(), dest.getPod().getId(), nic.getId());
        nic.setIp4Address(ip);
        nic.setCidr(pod.getCidrAddress() + "/" + pod.getCidrSize());
        
        return Long.toString(nic.getId());
    }

    @Override
    public boolean release(String uniqueName, String uniqueId) {
        _dcDao.releasePrivateIpAddress(Long.parseLong(uniqueId));
        return true;
    }

}
