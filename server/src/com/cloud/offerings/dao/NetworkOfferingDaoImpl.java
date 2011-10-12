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

import javax.ejb.Local;
import javax.persistence.EntityExistsException;

import com.cloud.network.Network.GuestIpType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.offering.NetworkOffering.Availability;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value=NetworkOfferingDao.class) @DB(txn=false)
public class NetworkOfferingDaoImpl extends GenericDaoBase<NetworkOfferingVO, Long> implements NetworkOfferingDao {
    final SearchBuilder<NetworkOfferingVO> NameSearch;
    final SearchBuilder<NetworkOfferingVO> SystemOfferingSearch;
    final SearchBuilder<NetworkOfferingVO> AvailabilitySearch;
    final SearchBuilder<NetworkOfferingVO> TrafficTypeGuestTypeSearch;
    
    protected NetworkOfferingDaoImpl() {
        super();
        
        NameSearch = createSearchBuilder();
        NameSearch.and("name", NameSearch.entity().getName(), SearchCriteria.Op.EQ);
        NameSearch.and("uniqueName", NameSearch.entity().getUniqueName(), SearchCriteria.Op.EQ);
        NameSearch.done();
        
        SystemOfferingSearch = createSearchBuilder();
        SystemOfferingSearch.and("system", SystemOfferingSearch.entity().isSystemOnly(), SearchCriteria.Op.EQ);
        SystemOfferingSearch.done();
        
        AvailabilitySearch = createSearchBuilder();
        AvailabilitySearch.and("availability", AvailabilitySearch.entity().getAvailability(), SearchCriteria.Op.EQ);
        AvailabilitySearch.and("isSystem", AvailabilitySearch.entity().isSystemOnly(), SearchCriteria.Op.EQ);
        AvailabilitySearch.done();
        
        TrafficTypeGuestTypeSearch = createSearchBuilder();
        TrafficTypeGuestTypeSearch.and("trafficType", TrafficTypeGuestTypeSearch.entity().getTrafficType(), SearchCriteria.Op.EQ);
        TrafficTypeGuestTypeSearch.and("guestType", TrafficTypeGuestTypeSearch.entity().getGuestType(), SearchCriteria.Op.EQ);
        TrafficTypeGuestTypeSearch.and("isSystem", TrafficTypeGuestTypeSearch.entity().isSystemOnly(), SearchCriteria.Op.EQ);
        TrafficTypeGuestTypeSearch.done();
    }
    
    @Override
    public NetworkOfferingVO findByUniqueName(String uniqueName) {
        SearchCriteria<NetworkOfferingVO> sc = NameSearch.create();
        
        sc.setParameters("uniqueName", uniqueName);
        
        return findOneBy(sc);
        
    }
    
    @Override
    public NetworkOfferingVO persistDefaultNetworkOffering(NetworkOfferingVO offering) {
        assert offering.getUniqueName() != null : "how are you going to find this later if you don't set it?";
        NetworkOfferingVO vo = findByUniqueName(offering.getUniqueName());
        if (vo != null) {
            return vo;
        }
        try {
            vo = persist(offering);
            return vo;
        } catch (EntityExistsException e) {
            // Assume it's conflict on unique name from two different management servers.
            return findByUniqueName(offering.getName());
        }
    }
    
    @Override
    public List<NetworkOfferingVO> listNonSystemNetworkOfferings() {
        SearchCriteria<NetworkOfferingVO> sc = SystemOfferingSearch.create();
        sc.setParameters("system", false);
        return this.listIncludingRemovedBy(sc, null);
    }
    
    @Override
    public List<NetworkOfferingVO> listSystemNetworkOfferings() {
        SearchCriteria<NetworkOfferingVO> sc = SystemOfferingSearch.create();
        sc.setParameters("system", true);
        return this.listIncludingRemovedBy(sc, null);
    }
    
    @Override
    public List<NetworkOfferingVO> listByAvailability(Availability availability, boolean isSystem) {
        SearchCriteria<NetworkOfferingVO> sc = AvailabilitySearch.create();
        sc.setParameters("availability", availability);
        sc.setParameters("isSystem", isSystem);
        return listBy(sc, null);
    }
    
    @Override
    public List<NetworkOfferingVO> listByTrafficTypeAndGuestType(boolean isSystem, TrafficType trafficType, GuestIpType guestType) {
        SearchCriteria<NetworkOfferingVO> sc = TrafficTypeGuestTypeSearch.create();
        sc.setParameters("trafficType", trafficType);
        sc.setParameters("guestType", guestType);
        sc.setParameters("isSystem", isSystem);
        return listBy(sc, null);
    }
    
}
