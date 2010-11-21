/**
 * 
 */
package com.cloud.org;

public interface Cluster extends Grouping {
    long getId();
    
    String getName();
    long getDataCenterId();
    long getPodId();

}
