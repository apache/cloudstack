/**
 * 
 */
package com.cloud.vm;

import com.cloud.deploy.DeployDestination;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
import com.cloud.network.NetworkConfiguration;
import com.cloud.resource.Concierge;

/**
 * NetworkConcierge reserves network settings for a VM based
 * on the NetworkCharacteristics given.  A concierge must
 * return a unique name so we know to call it to release
 * the reservation. 
 *
 */
public interface NetworkConcierge extends Concierge<Nic> {
    String getUniqueName();

    NicProfile allocate(VirtualMachine vm, NetworkConfiguration config, NicProfile nic) throws InsufficientVirtualNetworkCapcityException, InsufficientAddressCapacityException;
    
    boolean create(Nic nic) throws InsufficientVirtualNetworkCapcityException, InsufficientAddressCapacityException;
    
    String reserve(long vmId, NicProfile ch, DeployDestination dest) throws InsufficientVirtualNetworkCapcityException, InsufficientAddressCapacityException;
    
    boolean release(String uniqueName, String uniqueId);
}
