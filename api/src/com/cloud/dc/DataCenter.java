/**
 * 
 */
package com.cloud.dc;

import com.cloud.org.Grouping;

/**
 *
 */
public interface DataCenter extends Grouping {
    long getId();
    String getDns1();
    String getDns2();
}
