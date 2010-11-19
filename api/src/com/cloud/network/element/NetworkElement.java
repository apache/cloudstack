/**
 * 
 */
package com.cloud.network.element;

import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientNetworkCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.offering.NetworkOffering;
import com.cloud.utils.component.Adapter;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
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
    boolean implement(Network config, NetworkOffering offering, DeployDestination dest, ReservationContext context) throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException;
    
    boolean prepare(Network config, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientNetworkCapacityException;
    
    boolean release(Network config, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException;
    
    boolean shutdown(Network config, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException;
    
    boolean addRule();
    
    boolean revokeRule();
}
