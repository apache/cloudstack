/**
 * 
 */
package com.cloud.utils.time;

public interface InaccurateClockMBean {
    String restart();

    String turnOff();

    long[] getCurrentTimes();
}
