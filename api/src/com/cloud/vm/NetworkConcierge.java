/**
 * 
 */
package com.cloud.vm;

import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
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
    
    Pair<String, NetworkTO> reserve(long vmId, NetworkCharacteristics ch) throws InsufficientVirtualNetworkCapcityException;
    
    boolean release(String uniqueName, String uniqueId);
}
