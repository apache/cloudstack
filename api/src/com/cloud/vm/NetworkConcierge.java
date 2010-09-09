/**
 * 
 */
package com.cloud.vm;

import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
import com.cloud.network.NetworkProfile;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Adapter;

/**
 * NetworkConcierge reserves network settings for a VM based
 * on the NetworkCharacteristics given.  A Concierge must
 * return a unique name so we know to call it to release
 * the reservation. 
 *
 */
public interface NetworkConcierge extends Adapter {
    String getUniqueName();

    Nic allocate(VirtualMachine vm, NetworkProfile profile, Nic nic);
    
    Pair<String, String> reserve(long vmId, NetworkCharacteristics ch) throws InsufficientVirtualNetworkCapcityException, InsufficientAddressCapacityException;
    
    boolean release(String uniqueName, String uniqueId);
}
