/**
 * 
 */
package com.cloud.network.element;

import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientNetworkCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.NetworkConfiguration;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
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
     * @return true if network configuration is now usable; false if not; null if not handled by this element.
     */
    boolean implement(NetworkConfiguration config, NetworkOffering offering, DeployDestination dest, Account user) throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException;
    
    boolean prepare(NetworkConfiguration config, NicProfile nic, VirtualMachineProfile vm, NetworkOffering offering, DeployDestination dest, Account user) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientNetworkCapacityException;
    
    boolean release(NetworkConfiguration config, NicProfile nic, VirtualMachineProfile vm, NetworkOffering offering, Account user) throws ConcurrentOperationException, ResourceUnavailableException;
    
    boolean shutdown(NetworkConfiguration config, NetworkOffering offering, Account user) throws ConcurrentOperationException, ResourceUnavailableException;
}
