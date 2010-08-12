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

import org.apache.log4j.Logger;

import com.cloud.service.ServiceOfferingVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;

@Local(value={ServiceOfferingDao.class}) @DB(txn=false)
public class ServiceOfferingDaoImpl extends GenericDaoBase<ServiceOfferingVO, Long> implements ServiceOfferingDao {
    protected static final Logger s_logger = Logger.getLogger(ServiceOfferingDaoImpl.class);

    protected final SearchBuilder<ServiceOfferingVO> UniqueNameSearch;
    protected ServiceOfferingDaoImpl() {
        super();
        
        UniqueNameSearch = createSearchBuilder();
        UniqueNameSearch.and("name", UniqueNameSearch.entity().getUniqueName(), SearchCriteria.Op.EQ);
        UniqueNameSearch.and("removed", UniqueNameSearch.entity().getRemoved(), SearchCriteria.Op.NNULL);
        UniqueNameSearch.done();
    }
    
    @Override
    public ServiceOfferingVO findByName(String name) {
        SearchCriteria sc = UniqueNameSearch.create();
        sc.setParameters("name", name);
        List<ServiceOfferingVO> vos = searchAll(sc, null, null, false);
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
        Transaction txn = Transaction.currentTxn();
        try {
            txn.start();
            vo = persist(offering);
            remove(vo.getId());
            txn.commit();
            return vo;
        } catch (Exception e) {
            // Assume it's conflict on unique name
            return findByName(offering.getUniqueName());
        }
    }
}
