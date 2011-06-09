/**
 * 
 */
package com.cloud.offerings.dao;

import java.util.List;

import com.cloud.network.Network.GuestIpType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.offering.NetworkOffering.Availability;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.utils.db.GenericDao;

/**
 * NetworkOfferingDao deals with searches and operations done on the
 * network_offering table.
 *
 */
public interface NetworkOfferingDao extends GenericDao<NetworkOfferingVO, Long> {
    /**
     * Returns the network offering that matches the name.
     * @param uniqueName name
     * @return NetworkOfferingVO
     */
    NetworkOfferingVO findByUniqueName(String uniqueName);
    
    /**
     * Persists the system network offering by checking the name.  If it
     * is already there, then it returns the correct one in the database.
     * If not, then it persists it into the database.
     * 
     * @param offering network offering to persist if not in the database.
     * @return NetworkOfferingVO backed by a row in the database
     */
    NetworkOfferingVO persistDefaultNetworkOffering(NetworkOfferingVO offering);
    
    List<NetworkOfferingVO> listNonSystemNetworkOfferings();
    
    List<NetworkOfferingVO> listSystemNetworkOfferings();
    
    List<NetworkOfferingVO> listByAvailability(Availability availability, boolean isSystem);
    
    List<NetworkOfferingVO> listByTrafficTypeAndGuestType(boolean isSystem, TrafficType trafficType, GuestIpType guestType);
    
}
