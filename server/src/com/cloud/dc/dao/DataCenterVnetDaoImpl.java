// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.dc.dao;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Component;

import com.cloud.dc.DataCenterVnetVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;

/**
 * DataCenterVnetDaoImpl maintains the one-to-many relationship between
 * data center/physical_network and the vnet that appears within the physical network.
 */
@Component
@DB(txn=false)
public class DataCenterVnetDaoImpl extends GenericDaoBase<DataCenterVnetVO, Long> implements DataCenterVnetDao {
    private final SearchBuilder<DataCenterVnetVO> FreeVnetSearch;
    private final SearchBuilder<DataCenterVnetVO> VnetDcSearch;
    private final SearchBuilder<DataCenterVnetVO> VnetDcSearchAllocated;
    private final SearchBuilder<DataCenterVnetVO> DcSearchAllocated;
    private final GenericSearchBuilder<DataCenterVnetVO, Integer> countZoneVlans;
    private final GenericSearchBuilder<DataCenterVnetVO, Integer> countAllocatedZoneVlans;
    
    public List<DataCenterVnetVO> listAllocatedVnets(long physicalNetworkId) {
        SearchCriteria<DataCenterVnetVO> sc = DcSearchAllocated.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);       
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
    
    public List<DataCenterVnetVO> findVnet(long dcId, long physicalNetworkId, String vnet) {
        SearchCriteria<DataCenterVnetVO> sc = VnetDcSearch.create();
        sc.setParameters("dc", dcId);
        sc.setParameters("physicalNetworkId", physicalNetworkId);
        sc.setParameters("vnet", vnet);

        return listBy(sc);
    }    
    
    @DB
    public void add(long dcId, long physicalNetworkId, int start, int end) {
        String insertVnet = "INSERT INTO `cloud`.`op_dc_vnet_alloc` (vnet, data_center_id, physical_network_id) VALUES ( ?, ?, ?)";
        
        Transaction txn = Transaction.currentTxn();
        try {
            txn.start();
            PreparedStatement stmt = txn.prepareAutoCloseStatement(insertVnet);
            for (int i = start; i <= end; i++) {
                stmt.setString(1, String.valueOf(i));
                stmt.setLong(2, dcId);
                stmt.setLong(3, physicalNetworkId);
                stmt.addBatch();
            }
            stmt.executeBatch();
            txn.commit();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Exception caught adding vnet ", e);
        }
    }
    
    public void delete(long physicalNetworkId) {
        SearchCriteria<DataCenterVnetVO> sc = VnetDcSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);
        remove(sc);
    }

    @DB
    public DataCenterVnetVO take(long physicalNetworkId, long accountId, String reservationId) {
        SearchCriteria<DataCenterVnetVO> sc = FreeVnetSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);        
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

    public void release(String vnet, long physicalNetworkId, long accountId, String reservationId) {
        SearchCriteria<DataCenterVnetVO> sc = VnetDcSearchAllocated.create();
        sc.setParameters("vnet", vnet);
        sc.setParameters("physicalNetworkId", physicalNetworkId);
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

    public DataCenterVnetDaoImpl() {
    	super();
        DcSearchAllocated = createSearchBuilder();
        DcSearchAllocated.and("dc", DcSearchAllocated.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        DcSearchAllocated.and("physicalNetworkId", DcSearchAllocated.entity().getPhysicalNetworkId(), SearchCriteria.Op.EQ);
        DcSearchAllocated.and("allocated", DcSearchAllocated.entity().getTakenAt(), SearchCriteria.Op.NNULL);
        DcSearchAllocated.done();
        
        FreeVnetSearch = createSearchBuilder();
        FreeVnetSearch.and("dc", FreeVnetSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        FreeVnetSearch.and("physicalNetworkId", FreeVnetSearch.entity().getPhysicalNetworkId(), SearchCriteria.Op.EQ);
        FreeVnetSearch.and("taken", FreeVnetSearch.entity().getTakenAt(), SearchCriteria.Op.NULL);
        FreeVnetSearch.done();
        
        VnetDcSearch = createSearchBuilder();
        VnetDcSearch.and("vnet", VnetDcSearch.entity().getVnet(), SearchCriteria.Op.EQ);
        VnetDcSearch.and("dc", VnetDcSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        VnetDcSearch.and("physicalNetworkId", VnetDcSearch.entity().getPhysicalNetworkId(), SearchCriteria.Op.EQ);
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
        VnetDcSearchAllocated.and("physicalNetworkId", VnetDcSearchAllocated.entity().getPhysicalNetworkId(), SearchCriteria.Op.EQ);        
        VnetDcSearchAllocated.and("taken", VnetDcSearchAllocated.entity().getTakenAt(), SearchCriteria.Op.NNULL);
        VnetDcSearchAllocated.and("account", VnetDcSearchAllocated.entity().getAccountId(), SearchCriteria.Op.EQ);
        VnetDcSearchAllocated.and("reservation", VnetDcSearchAllocated.entity().getReservationId(), SearchCriteria.Op.EQ);
        VnetDcSearchAllocated.done();
        
    }
}
