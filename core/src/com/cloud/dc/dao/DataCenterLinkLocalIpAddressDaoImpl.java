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
import java.util.Date;
import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.dc.DataCenterLinkLocalIpAddressVO;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;

@Local(value={DataCenterLinkLocalIpAddressDaoImpl.class})
public class DataCenterLinkLocalIpAddressDaoImpl extends GenericDaoBase<DataCenterLinkLocalIpAddressVO, Long> implements GenericDao<DataCenterLinkLocalIpAddressVO, Long> {
    private static final Logger s_logger = Logger.getLogger(DataCenterLinkLocalIpAddressDaoImpl.class);
    
	private static final String COUNT_ALL_PRIVATE_IPS = "SELECT count(*) from `cloud`.`op_dc_link_local_ip_address_alloc` where pod_id = ? AND data_center_id = ?";
	private static final String COUNT_ALLOCATED_PRIVATE_IPS = "SELECT count(*) from `cloud`.`op_dc_link_local_ip_address_alloc` where pod_id = ? AND data_center_id = ? AND taken is not null";
	
    private final SearchBuilder<DataCenterLinkLocalIpAddressVO> FreeIpSearch;
    private final SearchBuilder<DataCenterLinkLocalIpAddressVO> IpDcSearch;
    private final SearchBuilder<DataCenterLinkLocalIpAddressVO> PodDcSearch;
    private final SearchBuilder<DataCenterLinkLocalIpAddressVO> PodDcIpSearch;
    private final SearchBuilder<DataCenterLinkLocalIpAddressVO> FreePodDcIpSearch;
    
    public DataCenterLinkLocalIpAddressVO takeIpAddress(long dcId, long podId, long instanceId) {
        SearchCriteria sc = FreeIpSearch.create();
        sc.setParameters("dc", dcId);
        sc.setParameters("pod", podId);
        
        Transaction txn = Transaction.currentTxn();
        try {
            txn.start();
            
            DataCenterLinkLocalIpAddressVO  vo = lock(sc, true);
            if (vo == null) {
                txn.rollback();
                return vo;
            }
            vo.setTakenAt(new Date());
            vo.setInstanceId(instanceId);
            update(vo.getId(), vo);
            txn.commit();
            return vo;
        } catch (Exception e) {
            txn.rollback();
            throw new CloudRuntimeException("Caught Exception ", e);
        }
    }
    
    public boolean deleteIpAddressByPod(long podId) {
        Transaction txn = Transaction.currentTxn();
        try {
            String deleteSql = "DELETE FROM `cloud`.`op_dc_link_local_ip_address_alloc` WHERE `pod_id` = ?";
            PreparedStatement stmt = txn.prepareAutoCloseStatement(deleteSql);
            stmt.setLong(1, podId);
            return stmt.execute();
        } catch(Exception e) {
            throw new CloudRuntimeException("Caught Exception ", e);
        }
    }
    
    public boolean mark(long dcId, long podId, String ip) {
        SearchCriteria sc = FreePodDcIpSearch.create();
        sc.setParameters("podId", podId);
        sc.setParameters("dcId", dcId);
        sc.setParameters("ipAddress", ip);
        
        DataCenterLinkLocalIpAddressVO vo = createForUpdate();
        vo.setTakenAt(new Date());
        
        return update(vo, sc) >= 1;
    }
    
    public void addIpRange(long dcId, long podId, String start, String end) {
        Transaction txn = Transaction.currentTxn();
        String insertSql = "INSERT INTO `cloud`.`op_dc_link_local_ip_address_alloc` (ip_address, data_center_id, pod_id) VALUES (?, ?, ?)";
        PreparedStatement stmt = null;
        
        long startIP = NetUtils.ip2Long(start);
        long endIP = NetUtils.ip2Long(end);
        
        while (startIP <= endIP) {
            try {
                stmt = txn.prepareAutoCloseStatement(insertSql);
                stmt.setString(1, NetUtils.long2Ip(startIP));
                stmt.setLong(2, dcId);
                stmt.setLong(3, podId);
                stmt.executeUpdate();
                stmt.close();
            } catch (Exception ex) {
                s_logger.warn("Unable to persist " + NetUtils.long2Ip(startIP) + " due to " + ex.getMessage());
            }
            startIP++;
        }
    }
    
    public void releaseIpAddress(String ipAddress, long dcId, Long instanceId) {
    	if (s_logger.isDebugEnabled()) {
    		s_logger.debug("Releasing ip address: " + ipAddress + " data center " + dcId);
    	}
        SearchCriteria sc = IpDcSearch.create();
        sc.setParameters("ip", ipAddress);
        sc.setParameters("dc", dcId);
        sc.setParameters("instance", instanceId);

        DataCenterLinkLocalIpAddressVO vo = createForUpdate();
        
        vo.setTakenAt(null);
        vo.setInstanceId(null);
        update(vo, sc);
    }
    
    protected DataCenterLinkLocalIpAddressDaoImpl() {
    	super();
        FreeIpSearch = createSearchBuilder();
        FreeIpSearch.and("dc", FreeIpSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        FreeIpSearch.and("pod", FreeIpSearch.entity().getPodId(), SearchCriteria.Op.EQ);
        FreeIpSearch.and("taken", FreeIpSearch.entity().getTakenAt(), SearchCriteria.Op.NULL);
        FreeIpSearch.done();
        
        IpDcSearch = createSearchBuilder();
        IpDcSearch.and("ip", IpDcSearch.entity().getIpAddress(), SearchCriteria.Op.EQ);
        IpDcSearch.and("dc", IpDcSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        IpDcSearch.and("instance", IpDcSearch.entity().getInstanceId(), SearchCriteria.Op.EQ);
        IpDcSearch.done();
        
        PodDcSearch = createSearchBuilder();
        PodDcSearch.and("podId", PodDcSearch.entity().getPodId(), SearchCriteria.Op.EQ);
        PodDcSearch.and("dataCenterId", PodDcSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        PodDcSearch.done();
        
        PodDcIpSearch = createSearchBuilder();
        PodDcIpSearch.and("dcId", PodDcIpSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        PodDcIpSearch.and("podId", PodDcIpSearch.entity().getPodId(), SearchCriteria.Op.EQ);
        PodDcIpSearch.and("ipAddress", PodDcIpSearch.entity().getIpAddress(), SearchCriteria.Op.EQ);
        PodDcIpSearch.done();
        
        FreePodDcIpSearch = createSearchBuilder();
        FreePodDcIpSearch.and("dcId", FreePodDcIpSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        FreePodDcIpSearch.and("podId", FreePodDcIpSearch.entity().getPodId(), SearchCriteria.Op.EQ);
        FreePodDcIpSearch.and("ipAddress", FreePodDcIpSearch.entity().getIpAddress(), SearchCriteria.Op.EQ);
        FreePodDcIpSearch.and("taken", FreePodDcIpSearch.entity().getTakenAt(), SearchCriteria.Op.EQ);
        FreePodDcIpSearch.done();
    }
    
    public List<DataCenterLinkLocalIpAddressVO> listByPodIdDcId(long podId, long dcId) {
		SearchCriteria sc = PodDcSearch.create();
		sc.setParameters("podId", podId);
		sc.setParameters("dataCenterId", dcId);
		return listBy(sc);
	}
    
    public List<DataCenterLinkLocalIpAddressVO> listByPodIdDcIdIpAddress(long podId, long dcId, String ipAddress) {
    	SearchCriteria sc = PodDcIpSearch.create();
    	sc.setParameters("dcId", dcId);
		sc.setParameters("podId", podId);
		sc.setParameters("ipAddress", ipAddress);
		return listBy(sc);
    }
    
    public int countIPs(long podId, long dcId, boolean onlyCountAllocated) {
		Transaction txn = Transaction.currentTxn();
		PreparedStatement pstmt = null;
		int ipCount = 0;
		try {
			String sql = "";
			if (onlyCountAllocated) sql = COUNT_ALLOCATED_PRIVATE_IPS;
			else sql = COUNT_ALL_PRIVATE_IPS;
			
            pstmt = txn.prepareAutoCloseStatement(sql);
            pstmt.setLong(1, podId);
            pstmt.setLong(2, dcId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) ipCount = rs.getInt(1);
            
        } catch (Exception e) {
            s_logger.warn("Exception searching for routers and proxies", e);
        }
        return ipCount;
	}
}
