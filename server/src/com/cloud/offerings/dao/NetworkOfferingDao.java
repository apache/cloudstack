/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

/**
 * 
 */
package com.cloud.offerings.dao;

import java.util.List;

import com.cloud.network.Network;
import com.cloud.network.Networks.TrafficType;
import com.cloud.offering.NetworkOffering;
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
     * 
     * @param uniqueName
     *            name
     * @return NetworkOfferingVO
     */
    NetworkOfferingVO findByUniqueName(String uniqueName);

    /**
     * Persists the system network offering by checking the name. If it
     * is already there, then it returns the correct one in the database.
     * If not, then it persists it into the database.
     * 
     * @param offering
     *            network offering to persist if not in the database.
     * @return NetworkOfferingVO backed by a row in the database
     */
    NetworkOfferingVO persistDefaultNetworkOffering(NetworkOfferingVO offering);

    List<NetworkOfferingVO> listSystemNetworkOfferings();

    List<NetworkOfferingVO> listByAvailability(Availability availability, boolean isSystem);

    List<Long> getOfferingIdsToUpgradeFrom(NetworkOffering originalOffering);

    List<NetworkOfferingVO> listByTrafficTypeGuestTypeAndState(NetworkOffering.State state, TrafficType trafficType, Network.GuestType type);

}
