/**
 * 
 */
package com.cloud.network.profiler;

import java.util.Map;

import javax.ejb.Local;

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
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NetworkConcierge;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachine;

@Local(value=NetworkProfiler.class)
public class PublicNetworkProfiler extends AdapterBase implements NetworkProfiler, NetworkConcierge {

    @Override
    public NetworkConfiguration convert(NetworkOffering offering, DeploymentPlan plan, Map<String, String> params, Account owner) {
        if (offering.getTrafficType() != TrafficType.Public) {
            return null;
        }
        
        return new NetworkConfigurationVO(offering.getTrafficType(), Mode.Static, BroadcastDomainType.Vlan, offering.getId(), plan.getDataCenterId());
    }
    
    protected PublicNetworkProfiler() {
        super();
    }

    @Override
    public String getUniqueName() {
        return getName();
    }

    @Override
    public NicProfile allocate(VirtualMachine vm, NetworkConfiguration config, NicProfile nic) throws InsufficientVirtualNetworkCapcityException,
            InsufficientAddressCapacityException {
        if (config.getTrafficType() != TrafficType.Public) {
            return null;
        }
        
        if (nic != null) {
            throw new CloudRuntimeException("Unsupported nic settings");
        }
        
        return new NicProfile(null, null, null);
    }

    @Override
    public boolean create(Nic nic) throws InsufficientVirtualNetworkCapcityException, InsufficientAddressCapacityException {
        return true;
    }

    @Override
    public String reserve(long vmId, NicProfile ch, DeployDestination dest) throws InsufficientVirtualNetworkCapcityException,
            InsufficientAddressCapacityException {
        
        return null;
    }

    @Override
    public boolean release(String uniqueName, String uniqueId) {
        return false;
    }
}
