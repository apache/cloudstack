/**
 * 
 */
package com.cloud.network.element;

import java.util.List;

import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientNetworkCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.rules.FirewallRule;
import com.cloud.offering.NetworkOffering;
import com.cloud.utils.component.Adapter;
import com.cloud.utils.net.Ip;
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
    boolean implement(Network network, NetworkOffering offering, DeployDestination dest, ReservationContext context) throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException;
    
    /**
     * Prepare for a nic to be added into this network.
     * @param network
     * @param nic
     * @param vm
     * @param dest
     * @param context
     * @return
     * @throws ConcurrentOperationException
     * @throws ResourceUnavailableException
     * @throws InsufficientNetworkCapacityException
     */
    boolean prepare(Network network, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientNetworkCapacityException;
    
    /**
     * A nic is released from this network.
     * @param network
     * @param nic
     * @param vm
     * @param context
     * @return
     * @throws ConcurrentOperationException
     * @throws ResourceUnavailableException
     */
    boolean release(Network network, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException;
    
    /**
     * The network is being shutdown.
     * @param network
     * @param context
     * @return
     * @throws ConcurrentOperationException
     * @throws ResourceUnavailableException
     */
    boolean shutdown(Network network, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException;

    /**
     * Associate a new ip address to this network
     * @param network
     * @param ipAddress
     * @return
     * @throws ResourceUnavailableException
     */
    boolean associate(Network network, Ip ipAddress) throws ResourceUnavailableException;

    /**
     * Disassociate the ip address from this network
     * @param network
     * @param ipAddress
     * @return
     * @throws ResourceUnavailableException
     */
    boolean disassociate(Network network, Ip ipAddress) throws ResourceUnavailableException;
    
    /**
     * Apply rules
     * @param network
     * @param rules
     * @return
     * @throws ResourceUnavailableException
     */
    boolean applyRules(Network network, List<? extends FirewallRule> rules) throws ResourceUnavailableException;
}
