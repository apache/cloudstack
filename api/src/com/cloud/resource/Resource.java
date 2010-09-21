/**
 * 
 */
package com.cloud.resource;

import java.util.Date;

/**
 * Indicates a resource in CloudStack.  
 * Any resource that requires an reservation and release system
 * must implement this interface.
 *
 */
public interface Resource {
    enum State {
        Allocated, // The resource is allocated but not re
        Reserving,
        Reserved,
        Releasing,
    }
    
    enum ReservationStrategy {
        UserSpecified,
        Create,
        Start
    }
    
    /**
     * @return id in the CloudStack database
     */
    long getId();
    
    /**
     * @return reservation id returned by the allocation source.  This can be the
     * String version of the database id if the allocation source does not need it's 
     * own implementation of the reservation id.  This is passed back to the 
     * allocation source to release the resource.
     */
    String getReservationId();
    
    /**
     * @return unique name for the allocation source.
     */
    String getReserver();
    
    /**
     * @return the time a reservation request was made to the allocation source.
     */
    Date getUpdateTime();
    
    /**
     * @return the expected reservation interval.  -1 indicates 
     */
    int getExpectedReservationInterval();
    
    /**
     * @return the expected release interval.
     */
    int getExpectedReleaseInterval();
    
    /**
     * @return the reservation state of the resource.
     */
    State getState();
    
    ReservationStrategy getReservationStrategy();
}
