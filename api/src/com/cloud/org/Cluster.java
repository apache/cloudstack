/**
 * 
 */
package com.cloud.org;

import com.cloud.hypervisor.Hypervisor.HypervisorType;

public interface Cluster extends Grouping {
    public static enum ClusterType {
    	CloudManaged,
    	ExternalManaged;
    };
    
    long getId();
    
    String getName();
    long getDataCenterId();
    long getPodId();

    HypervisorType getHypervisorType();
    ClusterType getClusterType();
}
