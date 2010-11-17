/**
 * 
 */
package com.cloud.dc;

import com.cloud.org.Grouping;

/**
 *
 */
public interface DataCenter extends Grouping {
    public enum DataCenterNetworkType {
        Basic,
        Advanced
    }
    long getId();
    String getDns1();
    String getDns2();
    String getGuestNetworkCidr();
    String getName();
    Long getDomainId();
}
