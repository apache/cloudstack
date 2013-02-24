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

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.dc.DataCenterLinkLocalIpAddressVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;

@Component
@Local(value={DataCenterLinkLocalIpAddressDaoImpl.class}) @DB(txn=false)
public class DataCenterLinkLocalIpAddressDaoImpl extends GenericDaoBase<DataCenterLinkLocalIpAddressVO, Long> implements DataCenterLinkLocalIpAddressDao {
    private static final Logger s_logger = Logger.getLogger(DataCenterLinkLocalIpAddressDaoImpl.class);
    
    private final SearchBuilder<DataCenterLinkLocalIpAddressVO> AllFieldsSearch;
    private final GenericSearchBuilder<DataCenterLinkLocalIpAddressVO, Integer> AllIpCount;
    private final GenericSearchBuilder<DataCenterLinkLocalIpAddressVO, Integer> AllAllocatedIpCount;
    
    @DB
    public DataCenterLinkLocalIpAddressVO takeIpAddress(long dcId, long podId, long instanceId, String reservationId) {
        SearchCriteria<DataCenterLinkLocalIpAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("pod", podId);
        sc.setParameters("taken", (Date)null);
        
        Transaction txn = Transaction.currentTxn();
        txn.start();
        
        DataCenterLinkLocalIpAddressVO  vo = lockOneRandomRow(sc, true);
        if (vo == null) {
            return null;
        }
        
        vo.setTakenAt(new Date());
        vo.setInstanceId(instanceId);
        vo.setReservationId(reservationId);
        update(vo.getId(), vo);
        txn.commit();
        return vo;
    }
    
    public boolean deleteIpAddressByPod(long podId) {
        SearchCriteria<DataCenterLinkLocalIpAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("pod", podId);
        return remove(sc) > 0;
    }
    
    @DB
    public void addIpRange(long dcId, long podId, String start, String end) {
        String insertSql = "INSERT INTO `cloud`.`op_dc_link_local_ip_address_alloc` (ip_address, data_center_id, pod_id) VALUES (?, ?, ?)";
        PreparedStatement stmt = null;
        
        long startIP = NetUtils.ip2Long(start);
        long endIP = NetUtils.ip2Long(end);

        Transaction txn = Transaction.currentTxn();
        try {
            txn.start();
            stmt = txn.prepareAutoCloseStatement(insertSql);
            while (startIP <= endIP) {
                    stmt.setString(1, NetUtils.long2Ip(startIP++));
                    stmt.setLong(2, dcId);
                    stmt.setLong(3, podId);
                    stmt.addBatch();
            }
            stmt.executeBatch();
            txn.commit();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to insert", e);
        }
    }
    
    public void releaseIpAddress(String ipAddress, long dcId, long instanceId) {
    	if (s_logger.isDebugEnabled()) {
    		s_logger.debug("Releasing ip address: " + ipAddress + " data center " + dcId);
    	}
        SearchCriteria<DataCenterLinkLocalIpAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("ip", ipAddress);
        sc.setParameters("dc", dcId);
        sc.setParameters("instance", instanceId);

        DataCenterLinkLocalIpAddressVO vo = createForUpdate();
        
        vo.setTakenAt(null);
        vo.setInstanceId(null);
        vo.setReservationId(null);
        update(vo, sc);
    }
    
    public void releaseIpAddress(long nicId, String reservationId) {
        SearchCriteria<DataCenterLinkLocalIpAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("instance", nicId);
        sc.setParameters("reservation", reservationId);

        DataCenterLinkLocalIpAddressVO vo = createForUpdate();
        
        vo.setTakenAt(null);
        vo.setInstanceId(null);
        vo.setReservationId(null);
        update(vo, sc);
    }
    
    public List<DataCenterLinkLocalIpAddressVO> listByPodIdDcId(long podId, long dcId) {
		SearchCriteria<DataCenterLinkLocalIpAddressVO> sc = AllFieldsSearch.create();
		sc.setParameters("pod", podId);
		return listBy(sc);
	}
    
    public int countIPs(long podId, long dcId, boolean onlyCountAllocated) {
        SearchCriteria<Integer> sc;
        if (onlyCountAllocated) { 
            sc = AllAllocatedIpCount.create();
        } else {
            sc = AllIpCount.create();
        }
        
        sc.setParameters("pod", podId);
        List<Integer> count = customSearch(sc, null);
        return count.get(0);
	}
    
    public DataCenterLinkLocalIpAddressDaoImpl() {
        super();
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("ip", AllFieldsSearch.entity().getIpAddress(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("dc", AllFieldsSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("pod", AllFieldsSearch.entity().getPodId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("instance", AllFieldsSearch.entity().getInstanceId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("reservation", AllFieldsSearch.entity().getReservationId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("taken", AllFieldsSearch.entity().getTakenAt(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();
        
        AllIpCount = createSearchBuilder(Integer.class);
        AllIpCount.select(null, Func.COUNT, AllIpCount.entity().getId());
        AllIpCount.and("pod", AllIpCount.entity().getPodId(), SearchCriteria.Op.EQ);
        AllIpCount.done();
        
        AllAllocatedIpCount = createSearchBuilder(Integer.class);
        AllAllocatedIpCount.select(null, Func.COUNT, AllAllocatedIpCount.entity().getId());
        AllAllocatedIpCount.and("pod", AllAllocatedIpCount.entity().getPodId(), SearchCriteria.Op.EQ);
        AllAllocatedIpCount.and("removed", AllAllocatedIpCount.entity().getTakenAt(), SearchCriteria.Op.NNULL);
        AllAllocatedIpCount.done();
        
    }
    
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        
        SearchCriteria<DataCenterLinkLocalIpAddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("ip", NetUtils.getLinkLocalGateway());
        remove(sc);
        
        return true;
    }
}
