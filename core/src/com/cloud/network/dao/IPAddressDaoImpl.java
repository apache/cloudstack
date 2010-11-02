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

package com.cloud.network.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.network.IPAddressVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;

@Local(value={IPAddressDao.class})
public class IPAddressDaoImpl extends GenericDaoBase<IPAddressVO, String> implements IPAddressDao {
    private static final Logger s_logger = Logger.getLogger(IPAddressDaoImpl.class);
	
	protected SearchBuilder<IPAddressVO> DcIpSearch;
	protected SearchBuilder<IPAddressVO> VlanDbIdSearchUnallocated;
    protected SearchBuilder<IPAddressVO> AccountSearch;

    // make it public for JUnit test
    public IPAddressDaoImpl() {
	    DcIpSearch = createSearchBuilder();
	    DcIpSearch.and("dataCenterId", DcIpSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
	    DcIpSearch.and("ipAddress", DcIpSearch.entity().getAddress(), SearchCriteria.Op.EQ);
	    DcIpSearch.done();
	    
	    VlanDbIdSearchUnallocated = createSearchBuilder();
	    VlanDbIdSearchUnallocated.and("allocated", VlanDbIdSearchUnallocated.entity().getAllocated(), SearchCriteria.Op.NULL);
	    VlanDbIdSearchUnallocated.and("vlanDbId", VlanDbIdSearchUnallocated.entity().getVlanDbId(), SearchCriteria.Op.EQ);
	    //VlanDbIdSearchUnallocated.addRetrieve("ipAddress", VlanDbIdSearchUnallocated.entity().getAddress());
	    VlanDbIdSearchUnallocated.done();
	    
        AccountSearch = createSearchBuilder();
        AccountSearch.and("accountId", AccountSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountSearch.done();
    }
    
    public boolean mark(long dcId, String ip) {
        SearchCriteria sc = DcIpSearch.create();
        sc.setParameters("dataCenterId", dcId);
        sc.setParameters("ipAddress", ip);
        
        IPAddressVO vo = createForUpdate();
        vo.setAllocated(new Date());
        
        return update(vo, sc) >= 1;
    }

    @DB
    public List<String> assignAcccountSpecificIps(long accountId, long domainId, Long vlanDbId, boolean sourceNat) {
    	
    	SearchBuilder<IPAddressVO> VlanDbIdSearch = createSearchBuilder();    	
    	VlanDbIdSearch.and("vlanDbId", VlanDbIdSearch.entity().getVlanDbId(), SearchCriteria.Op.EQ);
    	VlanDbIdSearch.and("sourceNat", VlanDbIdSearch.entity().getSourceNat(), SearchCriteria.Op.EQ);	    
	    VlanDbIdSearch.done();
    	Transaction txn = Transaction.currentTxn();
		try {
			txn.start();
	        SearchCriteria sc = VlanDbIdSearch.create();
	        sc.setParameters("vlanDbId", vlanDbId);
	        sc.setParameters("sourceNat", sourceNat);
	        
			List<IPAddressVO> ipList = this.lock(sc, null, true);
			List<String> ipStringList = new ArrayList<String>();
			
			for(IPAddressVO ip:ipList){
			
				ip.setAccountId(accountId);
				ip.setAllocated(new Date());
				ip.setDomainId(domainId);
				ip.setSourceNat(sourceNat);
				
				if (!update(ip.getAddress(), ip)) {
					s_logger.debug("Unable to retrieve ip address " + ip.getAddress());
					return null;
				}
				ipStringList.add(ip.getAddress());
			}
			txn.commit();
			return ipStringList;
		} catch (Exception e) {
			s_logger.warn("Unable to assign IP", e);
		}
		return null;
    	
    }
    public void setIpAsSourceNat(String ipAddr){

    		IPAddressVO ip = createForUpdate(ipAddr);    	    
    	    ip.setSourceNat(true);
    	    s_logger.debug("Setting " + ipAddr + " as source Nat ");
    	    update(ipAddr, ip);
    }
    
	@Override
    public String assignIpAddress(long accountId, long domainId, long vlanDbId, boolean sourceNat) {

		Transaction txn = Transaction.currentTxn();
		try {
			txn.start();
	        SearchCriteria sc = VlanDbIdSearchUnallocated.create();
	        sc.setParameters("vlanDbId", vlanDbId);
	        
			IPAddressVO ip = this.lock(sc, true);
			if(ip != null) {
				ip.setAccountId(accountId);
				ip.setAllocated(new Date());
				ip.setDomainId(domainId);
				ip.setSourceNat(sourceNat);
				
				if (!update(ip.getAddress(), ip)) {
					s_logger.debug("Unable to retrieve any ip addresses");
					return null;
				}
	
				txn.commit();
				return ip.getAddress();
			} else {
				txn.rollback();
				//we do not log this as an error now, as there can be multiple vlans across which we iterate
				s_logger.warn("Unable to find an available IP address with related vlan, vlanDbId: " + vlanDbId);
			}
		} catch (Exception e) {
			s_logger.warn("Unable to assign IP", e);
		}
		return null;
    }

	@Override
	public void unassignIpAddress(String ipAddress) {
		IPAddressVO address = createForUpdate();
	    address.setAccountId(null);
	    address.setDomainId(null);
	    address.setAllocated(null);
	    address.setSourceNat(false);
	    update(ipAddress, address);
	}
	
	@Override
	public void unassignIpAsSourceNat(String ipAddress) {
		IPAddressVO address = createForUpdate();
	    address.setSourceNat(false);
	    update(ipAddress, address);
	}

    @Override
    public List<IPAddressVO> listByAccount(long accountId) {
    	SearchCriteria sc = AccountSearch.create();
        sc.setParameters("accountId", accountId);
        return listBy(sc);
    }
	
	public List<IPAddressVO> listByDcIdIpAddress(long dcId, String ipAddress) {
		SearchCriteria sc = DcIpSearch.create();
		sc.setParameters("dataCenterId", dcId);
		sc.setParameters("ipAddress", ipAddress);
		return listBy(sc);
	}
	
	@Override @DB
	public int countIPs(long dcId, long vlanDbId, boolean onlyCountAllocated) {
		Transaction txn = Transaction.currentTxn();
		PreparedStatement pstmt = null;
		int ipCount = 0;
		try {
			String sql = "SELECT count(*) from `cloud`.`user_ip_address` where data_center_id = " + dcId;
			
			if (vlanDbId != -1) {
				sql += " AND vlan_db_id = " + vlanDbId;
			}
			
			if (onlyCountAllocated) {
				sql += " AND allocated IS NOT NULL";
			}
			
            pstmt = txn.prepareAutoCloseStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
            	ipCount = rs.getInt(1);
            }
            
        } catch (Exception e) {
            s_logger.warn("Exception counting IP addresses", e);
        }
        
        return ipCount;
	}
	
	@Override @DB
	public int countIPs(long dcId, Long accountId, String vlanId, String vlanGateway, String vlanNetmask) {
		Transaction txn = Transaction.currentTxn();
		int ipCount = 0;
		try {
			String sql = "SELECT count(*) FROM user_ip_address u INNER JOIN vlan v on (u.vlan_db_id = v.id AND v.data_center_id = ? AND v.vlan_id = ? AND v.vlan_gateway = ? AND v.vlan_netmask = ? AND u.account_id = ?)";
			
			
			PreparedStatement pstmt = txn.prepareAutoCloseStatement(sql);
			pstmt.setLong(1, dcId);
			pstmt.setString(2, vlanId);
			pstmt.setString(3, vlanGateway);
			pstmt.setString(4, vlanNetmask);
			pstmt.setLong(5, accountId);
			ResultSet rs = pstmt.executeQuery();
			
			if (rs.next()) {
				ipCount = rs.getInt(1);
			}
		} catch (Exception e) {
			s_logger.warn("Exception counting IP addresses", e);
		}
		
		return ipCount;
	}
}
