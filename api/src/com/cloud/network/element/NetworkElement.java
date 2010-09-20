/**
 * 
 */
package com.cloud.network.element;

import com.cloud.network.NetworkConfiguration;
import com.cloud.offering.NetworkOffering;
import com.cloud.utils.component.Adapter;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachineProfile;

/**
 * Represents one network element that exists in a network.
 */
public interface NetworkElement extends Adapter {
    /**
     * Implement the network configuration as specified. 
     * @param config fully specified network configuration.
     * @param offering network offering that originated the network configuration.
     * @return true if network configuration is now usable; false if not.
     */
    boolean implement(NetworkConfiguration config, NetworkOffering offering);
    
    /**
     * Prepare the nic profile to be used within the network.
     * @param config
     * @param nic
     * @param offering
     * @return
     */
    boolean prepare(NetworkConfiguration config, NicProfile nic, VirtualMachineProfile vm, NetworkOffering offering);
    
    boolean release(NetworkConfiguration config, NicProfile nic, VirtualMachineProfile vm, NetworkOffering offering);
    
    boolean shutdown(NetworkConfiguration config, NetworkOffering offering);
}
