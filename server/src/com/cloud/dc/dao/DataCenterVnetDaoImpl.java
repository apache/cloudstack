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
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import com.cloud.dc.DataCenterVnetVO;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.network.IpAddress.State;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.exception.CloudRuntimeException;

/**
 * DataCenterVnetDaoImpl maintains the one-to-many relationship between
 * data center and the vnet that appears within its network.
 */
@DB(txn=false)
public class DataCenterVnetDaoImpl extends GenericDaoBase<DataCenterVnetVO, Long> implements GenericDao<DataCenterVnetVO, Long> {
    private final SearchBuilder<DataCenterVnetVO> FreeVnetSearch;
    private final SearchBuilder<DataCenterVnetVO> VnetDcSearch;
    private final SearchBuilder<DataCenterVnetVO> VnetDcSearchAllocated;
    private final SearchBuilder<DataCenterVnetVO> DcSearchAllocated;
    private final GenericSearchBuilder<DataCenterVnetVO, Integer> countZoneVlans;
    private final GenericSearchBuilder<DataCenterVnetVO, Integer> countAllocatedZoneVlans;
    
    public List<DataCenterVnetVO> listAllocatedVnets(long dcId) {
    	SearchCriteria<DataCenterVnetVO> sc = DcSearchAllocated.create();
    	sc.setParameters("dc", dcId);
    	return listBy(sc);
    }
    
    public List<DataCenterVnetVO> findVnet(long dcId, String vnet) {
    	SearchCriteria<DataCenterVnetVO> sc = VnetDcSearch.create();;
    	sc.setParameters("dc", dcId);
    	sc.setParameters("vnet", vnet);
    	return listBy(sc);
    }
    
    public int countZoneVlans(long dcId, boolean onlyCountAllocated){    	
        SearchCriteria<Integer> sc = onlyCountAllocated ?  countAllocatedZoneVlans.create() : countZoneVlans.create();
        sc.setParameters("dc", dcId);                
        return customSearch(sc, null).get(0);
    }
    
    
    @DB
    public void add(long dcId, int start, int end) {
        String insertVnet = "INSERT INTO `cloud`.`op_dc_vnet_alloc` (vnet, data_center_id) VALUES ( ?, ?)";
        
        Transaction txn = Transaction.currentTxn();
        try {
            txn.start();
            PreparedStatement stmt = txn.prepareAutoCloseStatement(insertVnet);
            for (int i = start; i <= end; i++) {
                stmt.setString(1, String.valueOf(i));
                stmt.setLong(2, dcId);
                stmt.addBatch();
            }
            stmt.executeBatch();
            txn.commit();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Exception caught adding vnet ", e);
        }
    }
    
    public void delete(long dcId) {
        SearchCriteria<DataCenterVnetVO> sc = VnetDcSearch.create();
        sc.setParameters("dc", dcId);
        
        remove(sc);
    }

    @DB
    public DataCenterVnetVO take(long dcId, long accountId, String reservationId) {
        SearchCriteria<DataCenterVnetVO> sc = FreeVnetSearch.create();
        sc.setParameters("dc", dcId);
        Date now = new Date();
        Transaction txn = Transaction.currentTxn();
        txn.start();
        DataCenterVnetVO vo = lockOneRandomRow(sc, true);
        if (vo == null) {
            return null;
        }

        vo.setTakenAt(now);
        vo.setAccountId(accountId);
        vo.setReservationId(reservationId);
        update(vo.getId(), vo);
        txn.commit();
        return vo;
    }

    public void release(String vnet, long dcId, long accountId, String reservationId) {
        SearchCriteria<DataCenterVnetVO> sc = VnetDcSearchAllocated.create();
        sc.setParameters("vnet", vnet);
        sc.setParameters("dc", dcId);
        sc.setParameters("account", accountId);
        sc.setParameters("reservation", reservationId);

        DataCenterVnetVO vo = findOneIncludingRemovedBy(sc);
        if (vo == null) {
            return;
        }

        vo.setTakenAt(null);
        vo.setAccountId(null);
        vo.setReservationId(null);
        update(vo.getId(), vo);
    }

    protected DataCenterVnetDaoImpl() {
    	super();
        DcSearchAllocated = createSearchBuilder();
        DcSearchAllocated.and("dc", DcSearchAllocated.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        DcSearchAllocated.and("allocated", DcSearchAllocated.entity().getTakenAt(), SearchCriteria.Op.NNULL);
        DcSearchAllocated.done();
        
        FreeVnetSearch = createSearchBuilder();
        FreeVnetSearch.and("dc", FreeVnetSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        FreeVnetSearch.and("taken", FreeVnetSearch.entity().getTakenAt(), SearchCriteria.Op.NULL);
        FreeVnetSearch.done();
        
        VnetDcSearch = createSearchBuilder();
        VnetDcSearch.and("vnet", VnetDcSearch.entity().getVnet(), SearchCriteria.Op.EQ);
        VnetDcSearch.and("dc", VnetDcSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        VnetDcSearch.done();
        
        countZoneVlans = createSearchBuilder(Integer.class);
        countZoneVlans.select(null, Func.COUNT, countZoneVlans.entity().getId());
        countZoneVlans.and("dc", countZoneVlans.entity().getDataCenterId(), Op.EQ);  
        countZoneVlans.done();
        
        countAllocatedZoneVlans = createSearchBuilder(Integer.class);
        countAllocatedZoneVlans.select(null, Func.COUNT, countAllocatedZoneVlans.entity().getId());
        countAllocatedZoneVlans.and("dc", countAllocatedZoneVlans.entity().getDataCenterId(), Op.EQ);
        countAllocatedZoneVlans.and("allocated", countAllocatedZoneVlans.entity().getTakenAt(), SearchCriteria.Op.NNULL);
        countAllocatedZoneVlans.done();

        VnetDcSearchAllocated = createSearchBuilder();
        VnetDcSearchAllocated.and("vnet", VnetDcSearchAllocated.entity().getVnet(), SearchCriteria.Op.EQ);
        VnetDcSearchAllocated.and("dc", VnetDcSearchAllocated.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        VnetDcSearchAllocated.and("taken", VnetDcSearchAllocated.entity().getTakenAt(), SearchCriteria.Op.NNULL);
        VnetDcSearchAllocated.and("account", VnetDcSearchAllocated.entity().getAccountId(), SearchCriteria.Op.EQ);
        VnetDcSearchAllocated.and("reservation", VnetDcSearchAllocated.entity().getReservationId(), SearchCriteria.Op.EQ);
        VnetDcSearchAllocated.done();
    }
}
