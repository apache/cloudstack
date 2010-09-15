/**
 * 
 */
package com.cloud.org;

public interface RunningIn {
    long getDataCenterId();
    long getPodId();
    Long getClusterId();
    Long getHostId();
}
