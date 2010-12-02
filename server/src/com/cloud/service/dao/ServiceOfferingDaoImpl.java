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

package com.cloud.service.dao;

import java.util.List;

import javax.ejb.Local;
import javax.persistence.EntityExistsException;

import org.apache.log4j.Logger;

import com.cloud.dc.DataCenterVO;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value={ServiceOfferingDao.class}) @DB(txn=false)
public class ServiceOfferingDaoImpl extends GenericDaoBase<ServiceOfferingVO, Long> implements ServiceOfferingDao {
    protected static final Logger s_logger = Logger.getLogger(ServiceOfferingDaoImpl.class);

    protected final SearchBuilder<ServiceOfferingVO> UniqueNameSearch;
    protected final SearchBuilder<ServiceOfferingVO> ServiceOfferingsByDomainIdSearch;
    protected final SearchBuilder<ServiceOfferingVO> ServiceOfferingsByKeywordSearch;
    protected final SearchBuilder<ServiceOfferingVO> PublicServiceOfferingSearch;
    
    protected ServiceOfferingDaoImpl() {
        super();
        
        UniqueNameSearch = createSearchBuilder();
        UniqueNameSearch.and("name", UniqueNameSearch.entity().getUniqueName(), SearchCriteria.Op.EQ);
        UniqueNameSearch.and("system", UniqueNameSearch.entity().isSystemUse(), SearchCriteria.Op.EQ);
        UniqueNameSearch.done();
        
        ServiceOfferingsByDomainIdSearch = createSearchBuilder();
        ServiceOfferingsByDomainIdSearch.and("domainId", ServiceOfferingsByDomainIdSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
        ServiceOfferingsByDomainIdSearch.done();
        
        PublicServiceOfferingSearch = createSearchBuilder();
        PublicServiceOfferingSearch.and("domainId", PublicServiceOfferingSearch.entity().getDomainId(), SearchCriteria.Op.NULL);
        PublicServiceOfferingSearch.and("system", PublicServiceOfferingSearch.entity().isSystemUse(), SearchCriteria.Op.EQ);
        PublicServiceOfferingSearch.done();
        
        ServiceOfferingsByKeywordSearch = createSearchBuilder();
        ServiceOfferingsByKeywordSearch.or("name", ServiceOfferingsByKeywordSearch.entity().getName(), SearchCriteria.Op.EQ);        
        ServiceOfferingsByKeywordSearch.or("displayText", ServiceOfferingsByKeywordSearch.entity().getDisplayText(), SearchCriteria.Op.EQ);
        ServiceOfferingsByKeywordSearch.done();
    }
    
    @Override
    public ServiceOfferingVO findByName(String name) {
        SearchCriteria<ServiceOfferingVO> sc = UniqueNameSearch.create();
        sc.setParameters("name", name);
        sc.setParameters("system", true);
        List<ServiceOfferingVO> vos = search(sc, null, null, false);
        if (vos.size() == 0) {
            return null;
        }
        
        return vos.get(0);
    }
    
    @Override @DB
    public ServiceOfferingVO persistSystemServiceOffering(ServiceOfferingVO offering) {
        assert offering.getUniqueName() != null : "how are you going to find this later if you don't set it?";
        ServiceOfferingVO vo = findByName(offering.getUniqueName());
        if (vo != null) {
            return vo;
        }
        try {
            return persist(offering);
        } catch (EntityExistsException e) {
            // Assume it's conflict on unique name
            return findByName(offering.getUniqueName());
        }
    }
    
    @Override
    public List<ServiceOfferingVO> findServiceOfferingByDomainId(Long domainId){
    	SearchCriteria<ServiceOfferingVO> sc = ServiceOfferingsByDomainIdSearch.create();
    	sc.setParameters("domainId", domainId);
        return listBy(sc);    	
    }
    
    @Override
    public List<ServiceOfferingVO> findPublicServiceOfferings(){
    	SearchCriteria<ServiceOfferingVO> sc = PublicServiceOfferingSearch.create();
    	sc.setParameters("system", false);
        return listBy(sc);    	
    }
}
