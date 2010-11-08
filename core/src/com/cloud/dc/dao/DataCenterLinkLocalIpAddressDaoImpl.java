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
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

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

@Local(value={DataCenterLinkLocalIpAddressDaoImpl.class}) @DB(txn=false)
public class DataCenterLinkLocalIpAddressDaoImpl extends GenericDaoBase<DataCenterLinkLocalIpAddressVO, Long> implements GenericDao<DataCenterLinkLocalIpAddressVO, Long> {
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
    
    protected DataCenterLinkLocalIpAddressDaoImpl() {
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
