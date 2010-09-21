/**
 * 
 */
package com.cloud.dc;

import com.cloud.org.Grouping;

/**
 * Represents one pod in the cloud stack.
 *
 */
public interface Pod extends Grouping {
    /**
     * @return unique id mapped to the pod.
     */
    long getId();
    
    String getCidrAddress();
    int getCidrSize();
    public String getGateway();
    
    //String getUniqueName();
}
