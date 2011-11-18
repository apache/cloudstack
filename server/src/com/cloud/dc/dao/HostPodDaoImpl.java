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

package com.cloud.dc.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.dc.HostPodVO;
import com.cloud.org.Grouping;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;

@Local(value={HostPodDao.class})
public class HostPodDaoImpl extends GenericDaoBase<HostPodVO, Long> implements HostPodDao {
    private static final Logger s_logger = Logger.getLogger(HostPodDaoImpl.class);
	
	protected SearchBuilder<HostPodVO> DataCenterAndNameSearch;
	protected SearchBuilder<HostPodVO> DataCenterIdSearch;
	
	protected HostPodDaoImpl() {
	    DataCenterAndNameSearch = createSearchBuilder();
	    DataCenterAndNameSearch.and("dc", DataCenterAndNameSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
	    DataCenterAndNameSearch.and("name", DataCenterAndNameSearch.entity().getName(), SearchCriteria.Op.EQ);
	    DataCenterAndNameSearch.done();
	    
	    DataCenterIdSearch = createSearchBuilder();
	    DataCenterIdSearch.and("dcId", DataCenterIdSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
	    DataCenterIdSearch.done();
	}
	
	@Override
    public List<HostPodVO> listByDataCenterId(long id) {
		SearchCriteria<HostPodVO> sc = DataCenterIdSearch.create();
		sc.setParameters("dcId", id);
		
	    return listBy(sc);
	}
	
	@Override
    public HostPodVO findByName(String name, long dcId) {
	    SearchCriteria<HostPodVO> sc = DataCenterAndNameSearch.create();
	    sc.setParameters("dc", dcId);
	    sc.setParameters("name", name);
	    
	    return findOneBy(sc);
	}
	
	@Override
	public HashMap<Long, List<Object>> getCurrentPodCidrSubnets(long zoneId, long podIdToSkip) {
		HashMap<Long, List<Object>> currentPodCidrSubnets = new HashMap<Long, List<Object>>();
		
		String selectSql = "SELECT id, cidr_address, cidr_size FROM host_pod_ref WHERE data_center_id=" + zoneId +" and removed IS NULL";
		Transaction txn = Transaction.currentTxn();
		try {
        	PreparedStatement stmt = txn.prepareAutoCloseStatement(selectSql);
        	ResultSet rs = stmt.executeQuery();
        	while (rs.next()) {
        		Long podId = rs.getLong("id");
        		if (podId.longValue() == podIdToSkip) {
                    continue;
                }
        		String cidrAddress = rs.getString("cidr_address");
        		long cidrSize = rs.getLong("cidr_size");
        		List<Object> cidrPair = new ArrayList<Object>();
        		cidrPair.add(0, cidrAddress);
        		cidrPair.add(1, new Long(cidrSize));
        		currentPodCidrSubnets.put(podId, cidrPair);
        	}
        } catch (SQLException ex) {
        	s_logger.warn("DB exception " + ex.getMessage(), ex);
            return null;
        }
        
        return currentPodCidrSubnets;
	}
	
    @Override
    public boolean remove(Long id) {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        HostPodVO pod = createForUpdate();
        pod.setName(null);
        
        update(id, pod);

        boolean result = super.remove(id);
        txn.commit();
        return result;
    }

    @Override
    public List<Long> listDisabledPods(long zoneId) {
        GenericSearchBuilder<HostPodVO, Long> podIdSearch = createSearchBuilder(Long.class);
        podIdSearch.selectField(podIdSearch.entity().getId());
        podIdSearch.and("dataCenterId", podIdSearch.entity().getDataCenterId(), Op.EQ);
        podIdSearch.and("allocationState", podIdSearch.entity().getAllocationState(), Op.EQ);
        podIdSearch.done();

        
        SearchCriteria<Long> sc = podIdSearch.create();
        sc.addAnd("dataCenterId", SearchCriteria.Op.EQ, zoneId);
        sc.addAnd("allocationState", SearchCriteria.Op.EQ, Grouping.AllocationState.Disabled);
        return customSearch(sc, null);
    }
    
}
