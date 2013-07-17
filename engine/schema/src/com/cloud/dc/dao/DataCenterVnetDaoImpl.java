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
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import com.cloud.exception.InvalidParameterValueException;
import org.springframework.stereotype.Component;

import com.cloud.dc.DataCenterVnetVO;
import com.cloud.network.dao.AccountGuestVlanMapDao;
import com.cloud.network.dao.AccountGuestVlanMapVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.JoinBuilder;
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
    private final SearchBuilder<DataCenterVnetVO> FreeDedicatedVnetSearch;
    private final SearchBuilder<DataCenterVnetVO> VnetDcSearch;
    private final SearchBuilder<DataCenterVnetVO> VnetDcSearchAllocated;
    private final SearchBuilder<DataCenterVnetVO> DcSearchAllocated;
    private final SearchBuilder<DataCenterVnetVO> DcSearchAllocatedInRange;
    private final GenericSearchBuilder<DataCenterVnetVO, Integer> countZoneVlans;
    private final GenericSearchBuilder<DataCenterVnetVO, Integer> countAllocatedZoneVlans;
    private final SearchBuilder<DataCenterVnetVO> SearchRange;
    private final SearchBuilder<DataCenterVnetVO> DedicatedGuestVlanRangeSearch;
    private final GenericSearchBuilder<DataCenterVnetVO, Integer> countVnetsAllocatedToAccount;
    protected GenericSearchBuilder<DataCenterVnetVO, Integer> countVnetsDedicatedToAccount;
    protected SearchBuilder<AccountGuestVlanMapVO> AccountGuestVlanMapSearch;
    protected GenericSearchBuilder<DataCenterVnetVO, String> ListAllVnetSearch;

    @Inject protected AccountGuestVlanMapDao _accountGuestVlanMapDao;
    
    public List<DataCenterVnetVO> listAllocatedVnets(long physicalNetworkId) {
        SearchCriteria<DataCenterVnetVO> sc = DcSearchAllocated.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);       
        return listBy(sc);
    }

    public List<DataCenterVnetVO> listAllocatedVnetsInRange(long dcId, long physicalNetworkId, Integer start, Integer end) {
        SearchCriteria<DataCenterVnetVO> sc = DcSearchAllocatedInRange.create();
        sc.setParameters("dc",dcId);
        sc.setParameters("physicalNetworkId", physicalNetworkId);
        sc.setParameters("vnetRange", start.toString(), end.toString());
        return listBy(sc);
    }

    public void lockRange(long dcId, long physicalNetworkId, Integer start, Integer end) {
        SearchCriteria<DataCenterVnetVO> sc = SearchRange.create();
        sc.setParameters("dc",dcId);
        sc.setParameters("physicalNetworkId", physicalNetworkId);
        sc.setParameters("vnetRange", start.toString(), end.toString());
        lockRows(sc,null,true);
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
    public void add(long dcId, long physicalNetworkId, List<String> vnets) {
        String insertVnet = "INSERT INTO `cloud`.`op_dc_vnet_alloc` (vnet, data_center_id, physical_network_id) VALUES ( ?, ?, ?)";
        
        Transaction txn = Transaction.currentTxn();
        try {
            txn.start();
            PreparedStatement stmt = txn.prepareAutoCloseStatement(insertVnet);
            for (int i =0; i <= vnets.size()-1; i++) {
                stmt.setString(1, vnets.get(i));
                stmt.setLong(2, dcId);
                stmt.setLong(3, physicalNetworkId);
                stmt.addBatch();
            }
            stmt.executeBatch();
            txn.commit();
        } catch (SQLException e) {
            throw new CloudRuntimeException(e.getMessage());
        }
    }

    public void deleteRange(Transaction txn, long dcId, long physicalNetworkId, int start, int end) {
        String deleteVnet = "DELETE FROM `cloud`.`op_dc_vnet_alloc` WHERE data_center_id=? AND physical_network_id=? AND taken IS NULL AND vnet BETWEEN ? AND ?";
        try {
            PreparedStatement stmt = txn.prepareAutoCloseStatement(deleteVnet);
            stmt.setLong(1,dcId);
            stmt.setLong(2,physicalNetworkId);
            stmt.setString(3,((Integer)start).toString());
            stmt.setString(4,((Integer)end).toString());
            stmt.execute();
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
    public DataCenterVnetVO take(long physicalNetworkId, long accountId, String reservationId, List<Long> vlanDbIds) {
        SearchCriteria<DataCenterVnetVO> sc;
        if (vlanDbIds != null) {
            sc = FreeDedicatedVnetSearch.create();
            sc.setParameters("accountGuestVlanMapId", vlanDbIds.toArray());
        } else {
            sc = FreeVnetSearch.create();
        }
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

    @Override
    public void releaseDedicatedGuestVlans(Long dedicatedGuestVlanRangeId) {
        SearchCriteria<DataCenterVnetVO> sc = DedicatedGuestVlanRangeSearch.create();
        sc.setParameters("dedicatedGuestVlanRangeId", dedicatedGuestVlanRangeId);
        List<DataCenterVnetVO> vnets = listBy(sc);
        for(DataCenterVnetVO vnet : vnets) {
           vnet.setAccountGuestVlanMapId(null);
           update(vnet.getId(), vnet);
        }
    }

    @Override
    public int countVnetsAllocatedToAccount(long dcId, long accountId) {
        SearchCriteria<Integer> sc = countVnetsAllocatedToAccount.create();
        sc.setParameters("dc", dcId);
        sc.setParameters("accountId", accountId);
        return customSearch(sc, null).get(0);
    }

    @Override
    public int countVnetsDedicatedToAccount(long dcId, long accountId) {
        SearchCriteria<Integer> sc = countVnetsDedicatedToAccount.create();
        sc.setParameters("dc", dcId);
        sc.setParameters("accountId", accountId);
        return customSearch(sc, null).get(0);
    }

    @Override
    public List<String> listVnetsByPhysicalNetworkAndDataCenter(long dcId, long physicalNetworkId){
        SearchCriteria<String> sc = ListAllVnetSearch.create();
        sc.setParameters("dc", dcId );
        sc.setParameters("physicalNetworkId", physicalNetworkId);
        return customSearch(sc, null);
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        boolean result = super.configure(name, params);

        countVnetsDedicatedToAccount = createSearchBuilder(Integer.class);
        countVnetsDedicatedToAccount.and("dc", countVnetsDedicatedToAccount.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        countVnetsDedicatedToAccount.and("accountGuestVlanMapId", countVnetsDedicatedToAccount.entity().getAccountGuestVlanMapId(), Op.NNULL);
        AccountGuestVlanMapSearch = _accountGuestVlanMapDao.createSearchBuilder();
        AccountGuestVlanMapSearch.and("accountId", AccountGuestVlanMapSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        countVnetsDedicatedToAccount.join("AccountGuestVlanMapSearch", AccountGuestVlanMapSearch, countVnetsDedicatedToAccount.entity().getAccountGuestVlanMapId(),
                AccountGuestVlanMapSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        countVnetsDedicatedToAccount.select(null, Func.COUNT, countVnetsDedicatedToAccount.entity().getId());
        countVnetsDedicatedToAccount.done();
        AccountGuestVlanMapSearch.done();

        return result;
    }

    public DataCenterVnetDaoImpl() {
    	super();
        DcSearchAllocated = createSearchBuilder();
        DcSearchAllocated.and("dc", DcSearchAllocated.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        DcSearchAllocated.and("physicalNetworkId", DcSearchAllocated.entity().getPhysicalNetworkId(), SearchCriteria.Op.EQ);
        DcSearchAllocated.and("allocated", DcSearchAllocated.entity().getTakenAt(), SearchCriteria.Op.NNULL);
        DcSearchAllocated.done();

        DcSearchAllocatedInRange = createSearchBuilder();
        DcSearchAllocatedInRange.and("dc",DcSearchAllocatedInRange.entity().getDataCenterId(), Op.EQ);
        DcSearchAllocatedInRange.and("physicalNetworkId", DcSearchAllocatedInRange.entity().getPhysicalNetworkId(), Op.EQ);
        DcSearchAllocatedInRange.and("allocated", DcSearchAllocatedInRange.entity().getTakenAt(), Op.NNULL);
        DcSearchAllocatedInRange.and("vnetRange", DcSearchAllocatedInRange.entity().getVnet(), Op.BETWEEN);
        DcSearchAllocatedInRange.done();

        SearchRange = createSearchBuilder();
        SearchRange.and("dc", SearchRange.entity().getDataCenterId(), Op.EQ);
        SearchRange.and("physicalNetworkId", SearchRange.entity().getPhysicalNetworkId(), Op.EQ);
        SearchRange.and("vnetRange", SearchRange.entity().getVnet(), Op.BETWEEN);
        
        FreeVnetSearch = createSearchBuilder();
        FreeVnetSearch.and("dc", FreeVnetSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        FreeVnetSearch.and("physicalNetworkId", FreeVnetSearch.entity().getPhysicalNetworkId(), SearchCriteria.Op.EQ);
        FreeVnetSearch.and("taken", FreeVnetSearch.entity().getTakenAt(), SearchCriteria.Op.NULL);
        FreeVnetSearch.and("accountGuestVlanMapId", FreeVnetSearch.entity().getAccountGuestVlanMapId(), SearchCriteria.Op.NULL);
        FreeVnetSearch.done();

        FreeDedicatedVnetSearch = createSearchBuilder();
        FreeDedicatedVnetSearch.and("dc", FreeDedicatedVnetSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        FreeDedicatedVnetSearch.and("physicalNetworkId", FreeDedicatedVnetSearch.entity().getPhysicalNetworkId(), SearchCriteria.Op.EQ);
        FreeDedicatedVnetSearch.and("taken", FreeDedicatedVnetSearch.entity().getTakenAt(), SearchCriteria.Op.NULL);
        FreeDedicatedVnetSearch.and("accountGuestVlanMapId", FreeDedicatedVnetSearch.entity().getAccountGuestVlanMapId(), SearchCriteria.Op.IN);
        FreeDedicatedVnetSearch.done();
        
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

        DedicatedGuestVlanRangeSearch = createSearchBuilder();
        DedicatedGuestVlanRangeSearch.and("dedicatedGuestVlanRangeId", DedicatedGuestVlanRangeSearch.entity().getAccountGuestVlanMapId(), SearchCriteria.Op.EQ);
        DedicatedGuestVlanRangeSearch.done();

        countVnetsAllocatedToAccount = createSearchBuilder(Integer.class);
        countVnetsAllocatedToAccount.and("dc", countVnetsAllocatedToAccount.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        countVnetsAllocatedToAccount.and("accountId", countVnetsAllocatedToAccount.entity().getAccountId(), SearchCriteria.Op.EQ);
        countVnetsAllocatedToAccount.select(null, Func.COUNT, countVnetsAllocatedToAccount.entity().getId());
        countVnetsAllocatedToAccount.done();

        ListAllVnetSearch  = createSearchBuilder(String.class);
        ListAllVnetSearch.select(null, Func.NATIVE, ListAllVnetSearch.entity().getVnet());
        ListAllVnetSearch.and("dc", ListAllVnetSearch.entity().getDataCenterId(), Op.EQ);
        ListAllVnetSearch.and("physicalNetworkId", ListAllVnetSearch.entity().getPhysicalNetworkId(), Op.EQ);
        ListAllVnetSearch.done();

    }
}
