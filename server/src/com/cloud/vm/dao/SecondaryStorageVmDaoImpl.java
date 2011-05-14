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
package com.cloud.vm.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.utils.db.Attribute;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.UpdateBuilder;
import com.cloud.vm.SecondaryStorageVm;
import com.cloud.vm.SecondaryStorageVmVO;
import com.cloud.vm.VirtualMachine.State;

@Local(value={SecondaryStorageVmDao.class})
public class SecondaryStorageVmDaoImpl extends GenericDaoBase<SecondaryStorageVmVO, Long> implements SecondaryStorageVmDao {
    private static final Logger s_logger = Logger.getLogger(SecondaryStorageVmDaoImpl.class);
    
    protected SearchBuilder<SecondaryStorageVmVO> DataCenterStatusSearch;
    protected SearchBuilder<SecondaryStorageVmVO> StateSearch;
    protected SearchBuilder<SecondaryStorageVmVO> HostSearch;
    protected SearchBuilder<SecondaryStorageVmVO> LastHostSearch;
    protected SearchBuilder<SecondaryStorageVmVO> HostUpSearch;
    protected SearchBuilder<SecondaryStorageVmVO> ZoneSearch;
    protected SearchBuilder<SecondaryStorageVmVO> StateChangeSearch;
    protected SearchBuilder<SecondaryStorageVmVO> InstanceSearch;
    
    protected final Attribute _updateTimeAttr;
    
    public SecondaryStorageVmDaoImpl() {
        DataCenterStatusSearch = createSearchBuilder();
        DataCenterStatusSearch.and("dc", DataCenterStatusSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        DataCenterStatusSearch.and("states", DataCenterStatusSearch.entity().getState(), SearchCriteria.Op.IN);
        DataCenterStatusSearch.and("role", DataCenterStatusSearch.entity().getRole(), SearchCriteria.Op.EQ);
        DataCenterStatusSearch.done();
        
        StateSearch = createSearchBuilder();
        StateSearch.and("states", StateSearch.entity().getState(), SearchCriteria.Op.IN);
        StateSearch.and("role", StateSearch.entity().getRole(), SearchCriteria.Op.EQ);
        StateSearch.done();
        
        HostSearch = createSearchBuilder();
        HostSearch.and("host", HostSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        HostSearch.and("role", HostSearch.entity().getRole(), SearchCriteria.Op.EQ);
        HostSearch.done();
        
        InstanceSearch = createSearchBuilder();
        InstanceSearch.and("instanceName", InstanceSearch.entity().getInstanceName(), SearchCriteria.Op.EQ);
        InstanceSearch.done();
        
        LastHostSearch = createSearchBuilder();
        LastHostSearch.and("lastHost", LastHostSearch.entity().getLastHostId(), SearchCriteria.Op.EQ);
        LastHostSearch.and("state", LastHostSearch.entity().getState(), SearchCriteria.Op.EQ);
        LastHostSearch.and("role", LastHostSearch.entity().getRole(), SearchCriteria.Op.EQ);
        LastHostSearch.done();
        
        HostUpSearch = createSearchBuilder();
        HostUpSearch.and("host", HostUpSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        HostUpSearch.and("states", HostUpSearch.entity().getState(), SearchCriteria.Op.NIN);
        HostUpSearch.and("role", HostUpSearch.entity().getRole(), SearchCriteria.Op.EQ);
        HostUpSearch.done();
        
        ZoneSearch = createSearchBuilder();
        ZoneSearch.and("zone", ZoneSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        ZoneSearch.and("role", ZoneSearch.entity().getRole(), SearchCriteria.Op.EQ);
        ZoneSearch.done();
        
        StateChangeSearch = createSearchBuilder();
        StateChangeSearch.and("id", StateChangeSearch.entity().getId(), SearchCriteria.Op.EQ);
        StateChangeSearch.and("states", StateChangeSearch.entity().getState(), SearchCriteria.Op.EQ);
        StateChangeSearch.and("host", StateChangeSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        StateChangeSearch.and("update", StateChangeSearch.entity().getUpdated(), SearchCriteria.Op.EQ);
        StateChangeSearch.and("role", StateChangeSearch.entity().getUpdated(), SearchCriteria.Op.EQ);
        StateChangeSearch.done();
        
        _updateTimeAttr = _allAttributes.get("updateTime");
        assert _updateTimeAttr != null : "Couldn't get this updateTime attribute";
    }
    
    @Override
    public boolean remove(Long id) {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        SecondaryStorageVmVO proxy = createForUpdate();
        proxy.setPublicIpAddress(null);
        proxy.setPrivateIpAddress(null);
        
        UpdateBuilder ub = getUpdateBuilder(proxy);
        ub.set(proxy, "state", State.Destroyed);
        ub.set(proxy, "privateIpAddress", null);
        update(id, ub);
        
        boolean result = super.remove(id);
        txn.commit();
        return result;
    }
    
    @Override
    public List<SecondaryStorageVmVO> getSecStorageVmListInStates(SecondaryStorageVm.Role role, long dataCenterId, State... states) {
        SearchCriteria<SecondaryStorageVmVO> sc = DataCenterStatusSearch.create();
        sc.setParameters("states", (Object[])states);
        sc.setParameters("dc", dataCenterId);
        if(role != null)
        	sc.setParameters("role", role);
        return listBy(sc);
    }

    @Override
    public List<SecondaryStorageVmVO> getSecStorageVmListInStates(SecondaryStorageVm.Role role, State... states) {
        SearchCriteria<SecondaryStorageVmVO> sc = StateSearch.create();
        sc.setParameters("states", (Object[])states);
        if(role != null)
        	sc.setParameters("role", role);
        
        return listBy(sc);
    }
    
    @Override
    public List<SecondaryStorageVmVO> listByHostId(SecondaryStorageVm.Role role, long hostId) {
        SearchCriteria<SecondaryStorageVmVO> sc = HostSearch.create();
        sc.setParameters("host", hostId);
        if(role != null)
        	sc.setParameters("role", role);
        return listBy(sc);
    }
    
    @Override
    public List<SecondaryStorageVmVO> listUpByHostId(SecondaryStorageVm.Role role, long hostId) {
        SearchCriteria<SecondaryStorageVmVO> sc = HostUpSearch.create();
        sc.setParameters("host", hostId);
        sc.setParameters("states", new Object[] {State.Destroyed, State.Stopped, State.Expunging});        
        if(role != null)
        	sc.setParameters("role", role);
        return listBy(sc);
    }
    
    @Override
    public List<Long> getRunningSecStorageVmListByMsid(SecondaryStorageVm.Role role, long msid) {
    	List<Long> l = new ArrayList<Long>();
        Transaction txn = Transaction.currentTxn();;
        PreparedStatement pstmt = null;
        try {
        	String sql;
        	if(role == null)
        		sql = "SELECT s.id FROM secondary_storage_vm s, vm_instance v, host h " +
        			"WHERE s.id=v.id AND v.state='Running' AND v.host_id=h.id AND h.mgmt_server_id=?";
        	else
        		sql = "SELECT s.id FROM secondary_storage_vm s, vm_instance v, host h " +
    				"WHERE s.id=v.id AND v.state='Running' AND s.role=? AND v.host_id=h.id AND h.mgmt_server_id=?";
        	
            pstmt = txn.prepareAutoCloseStatement(sql);
            
            if(role == null) {
            	pstmt.setLong(1, msid);
            } else {
            	pstmt.setString(1, role.toString());
            	pstmt.setLong(2, msid);
            }
            
            ResultSet rs = pstmt.executeQuery();
            while(rs.next()) {
            	l.add(rs.getLong(1));
            }
        } catch (SQLException e) {
        } catch (Throwable e) {
        }
        return l;
    }
    

    @Override
    public SecondaryStorageVmVO findByInstanceName(String instanceName) {
        SearchCriteria<SecondaryStorageVmVO> sc = InstanceSearch.create();
        sc.setParameters("instanceName", instanceName);
        List<SecondaryStorageVmVO> list = listBy(sc);
        if( list == null ) {
            return null;
        } else {
            return list.get(0);
        }
    }

	@Override
	public List<SecondaryStorageVmVO> listByZoneId(SecondaryStorageVm.Role role, long zoneId) {
		SearchCriteria<SecondaryStorageVmVO> sc = ZoneSearch.create();
        sc.setParameters("zone", zoneId);
        if(role != null)
        	sc.setParameters("role", role);
        return listBy(sc);
	}

	@Override
	public List<SecondaryStorageVmVO> listByLastHostId(SecondaryStorageVm.Role role, long hostId) {
		SearchCriteria<SecondaryStorageVmVO> sc = LastHostSearch.create();
		sc.setParameters("lastHost", hostId);
		sc.setParameters("state", State.Stopped);
        if(role != null)
        	sc.setParameters("role", role);
		
		return listBy(sc);
	}
	
	@Override
    public List<Long> listRunningSecStorageOrderByLoad(SecondaryStorageVm.Role role, long zoneId) {
    	
    	List<Long> l = new ArrayList<Long>();
        Transaction txn = Transaction.currentTxn();;
        PreparedStatement pstmt = null;
        try {
        	String sql;
        	if(role == null)
        		sql = "SELECT s.id, count(l.id) as count FROM secondary_storage_vm s INNER JOIN vm_instance v ON s.id=v.id LEFT JOIN cmd_exec_log l ON s.id=l.instance_id WHERE v.state='Running' AND v.data_center_id=? GROUP BY s.id ORDER BY count";
        	else
        		sql = "SELECT s.id, count(l.id) as count FROM secondary_storage_vm s INNER JOIN vm_instance v ON s.id=v.id LEFT JOIN cmd_exec_log l ON s.id=l.instance_id WHERE v.state='Running' AND v.data_center_id=? AND s.role=? GROUP BY s.id ORDER BY count";
        	
            pstmt = txn.prepareAutoCloseStatement(sql);
            
            if(role == null) {
            	pstmt.setLong(1, zoneId);
            } else {
            	pstmt.setLong(1, zoneId);
            	pstmt.setString(2, role.toString());
            }
            
            ResultSet rs = pstmt.executeQuery();
            while(rs.next()) {
            	l.add(rs.getLong(1));
            }
        } catch (SQLException e) {
        	s_logger.error("Unexpected exception ", e);
        }
        
        return l;
    }
}
