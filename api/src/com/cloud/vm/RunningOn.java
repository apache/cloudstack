/**
 * 
 */
package com.cloud.vm;

/**
 * RunningOn must be implemented by objects that runs on hosts.
 *
 */
public interface RunningOn {
    
    Long getHostId();

}
