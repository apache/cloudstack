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
import java.util.Date;
import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.info.ConsoleProxyLoadInfo;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Attribute;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.UpdateBuilder;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.State;
import com.cloud.vm.VirtualMachine;

@Local(value={ConsoleProxyDao.class})
public class ConsoleProxyDaoImpl extends GenericDaoBase<ConsoleProxyVO, Long> implements ConsoleProxyDao {
    private static final Logger s_logger = Logger.getLogger(ConsoleProxyDaoImpl.class);
    
    //
    // query SQL for returnning console proxy assignment info as following
    // 		proxy vm id, count of assignment
    //
    private static final String PROXY_ASSIGNMENT_MATRIX =
    	"SELECT c.id, count(runningVm.id) AS count "																+
    	" FROM console_proxy AS c LEFT JOIN"																		+
    	" (SELECT v.id AS id, v.proxy_id AS proxy_id FROM vm_instance AS v WHERE "  								+
    	"  (v.state='Running' OR v.state='Creating' OR v.state='Starting' OR v.state='Migrating')) "				+
    	" AS runningVm ON c.id = runningVm.proxy_id"																+
    	" GROUP BY c.id";
    
    //
    // query SQL for returnning running VM count at data center basis
    //
    private static final String DATACENTER_VM_MATRIX =
    	"SELECT d.id, d.name, count(v.id) AS count"																	+
    	" FROM data_center AS d LEFT JOIN vm_instance AS v ON v.data_center_id=d.id "								+
    	" WHERE (v.state='Creating' OR v.state='Starting' OR v.state='Running' OR v.state='Migrating')"  +
    	" GROUP BY d.id, d.name";
    
    private static final String DATACENTER_ACTIVE_SESSION_MATRIX =
    	"SELECT d.id, d.name, sum(c.active_session) AS count"														+
    	" FROM data_center AS d LEFT JOIN vm_instance AS v ON v.data_center_id=d.id "								+
    	" LEFT JOIN console_proxy AS c ON v.id=c.id "																+
    	" WHERE v.type='ConsoleProxy' AND (v.state='Creating' OR v.state='Starting' OR v.state='Running' OR v.state='Migrating')"  +
    	" GROUP BY d.id, d.name";
    
    //
    // query SQL for returnning running console proxy count at data center basis
    //
    private static final String DATACENTER_PROXY_MATRIX =
    	"SELECT d.id, d.name, count(dcid) as count" 													+
    	" FROM data_center as d" 																		+
    	" LEFT JOIN (" 																					+
    	" SELECT v.data_center_id as dcid, c.active_session as active_session from vm_instance as v" 	+
    	" INNER JOIN console_proxy as c ON v.id=c.id AND v.type='ConsoleProxy' AND (v.state='Creating' OR v.state='Starting' OR v.state='Running' OR v.state='Migrating')" +
    	" ) as t ON d.id = t.dcid" 																		+
    	" GROUP BY d.id, d.name";
    
    private static final String GET_PROXY_LOAD =
    	"SELECT count(*) AS count"																				+
    	" FROM vm_instance AS v " 																				+
    	" WHERE v.proxy_id=? AND (v.state='Running' OR v.state='Starting' OR v.state='Creating' OR v.state='Migrating')";
    
    private static final String GET_PROXY_ACTIVE_LOAD =
    	"SELECT active_session AS count"		+
    	" FROM console_proxy" 					+
    	" WHERE proxy_id=?";
    
    private static final String STORAGE_POOL_HOST_INFO =
    	"SELECT p.data_center_id,  count(ph.host_id) " +
    	" FROM storage_pool p, storage_pool_host_ref ph " +
    	" WHERE p.id = ph.pool_id AND p.data_center_id = ? " +
    	" GROUP by p.data_center_id";
    
    private static final String SHARED_STORAGE_POOL_HOST_INFO =
    	"SELECT p.data_center_id,  count(ph.host_id) " +
    	" FROM storage_pool p, storage_pool_host_ref ph " +
    	" WHERE p.pool_type <> 'LVM' AND p.id = ph.pool_id AND p.data_center_id = ? " +
    	" GROUP by p.data_center_id";
    	
    protected SearchBuilder<ConsoleProxyVO> DataCenterStatusSearch;
    protected SearchBuilder<ConsoleProxyVO> StateSearch;
    protected SearchBuilder<ConsoleProxyVO> HostSearch;
    protected SearchBuilder<ConsoleProxyVO> HostUpSearch;
    protected SearchBuilder<ConsoleProxyVO> StateChangeSearch;
    
    protected final Attribute _updateTimeAttr;
    
    public ConsoleProxyDaoImpl() {
        DataCenterStatusSearch = createSearchBuilder();
        DataCenterStatusSearch.and("dc", DataCenterStatusSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        DataCenterStatusSearch.and("states", DataCenterStatusSearch.entity().getState(), SearchCriteria.Op.IN);
        DataCenterStatusSearch.done();
        
        StateSearch = createSearchBuilder();
        StateSearch.and("states", StateSearch.entity().getState(), SearchCriteria.Op.IN);
        StateSearch.done();
        
        HostSearch = createSearchBuilder();
        HostSearch.and("host", HostSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        HostSearch.done();
        
        HostUpSearch = createSearchBuilder();
        HostUpSearch.and("host", HostUpSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        HostUpSearch.and("states", HostUpSearch.entity().getState(), SearchCriteria.Op.NIN);
        HostUpSearch.done();        
        
        StateChangeSearch = createSearchBuilder();
        StateChangeSearch.and("id", StateChangeSearch.entity().getId(), SearchCriteria.Op.EQ);
        StateChangeSearch.and("states", StateChangeSearch.entity().getState(), SearchCriteria.Op.EQ);
        StateChangeSearch.and("host", StateChangeSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        StateChangeSearch.and("update", StateChangeSearch.entity().getUpdated(), SearchCriteria.Op.EQ);
        StateChangeSearch.done();
        
        _updateTimeAttr = _allAttributes.get("updateTime");
        assert _updateTimeAttr != null : "Couldn't get this updateTime attribute";
    }
    
    @Override
    public boolean updateIf(ConsoleProxyVO vm, VirtualMachine.Event event, Long hostId) {
    	State oldState = vm.getState();
    	State newState = oldState.getNextState(event);
    	
    	Long oldHostId = vm.getHostId();
    	long oldDate = vm.getUpdated();
    	
    	if (newState == null) {
    		if (s_logger.isDebugEnabled()) {
    	    	s_logger.debug("There's no way to transition from old state: " + oldState.toString() + " event: " + event.toString());
    		}
    		return false;
    	}
    		
    	SearchCriteria sc = StateChangeSearch.create();
    	sc.setParameters("id", vm.getId());
    	sc.setParameters("states", oldState);
    	sc.setParameters("host", vm.getHostId());
    	sc.setParameters("update", vm.getUpdated());
    	
    	vm.incrUpdated();
        UpdateBuilder ub = getUpdateBuilder(vm);
        
        if(newState == State.Running) {
        	// save current running host id
        	ub.set(vm, "lastHostId", vm.getHostId());
        }
        
        ub.set(vm, _updateTimeAttr, new Date());
        ub.set(vm, "state", newState);
        ub.set(vm, "hostId", hostId);
        
        if (newState == State.Stopped) {
        	vm.setActiveSession(0);
        	ub.set(vm, "hostId", null);
        }
        
        int result = update(vm, sc);
        
        if (result == 0 && s_logger.isDebugEnabled()) {
        	ConsoleProxyVO vo = findById(vm.getId());
        	StringBuilder str = new StringBuilder("Unable to update ").append(vo.toString());
        	str.append(": DB Data={Host=").append(vo.getHostId()).append("; State=").append(vo.getState().toString()).append("; updated=").append(vo.getUpdated());
        	str.append("} New Data: {Host=").append(vm.getHostId()).append("; State=").append(vm.getState().toString()).append("; updated=").append(vm.getUpdated());
        	str.append("} Stale Data: {Host=").append(oldHostId).append("; State=").append(oldState.toString()).append("; updated=").append(oldDate).append("}");
        	s_logger.debug(str.toString());
        }
        
        return result > 0;
    }
    

    @Override
    public void update(long id, int activeSession, Date updateTime, byte[] sessionDetails) {
        ConsoleProxyVO ub = createForUpdate();
        ub.setActiveSession(activeSession);
        ub.setLastUpdateTime(updateTime);
        ub.setSessionDetails(sessionDetails);
        
        update(id, ub);
    }
    
    @Override
    public boolean remove(Long id) {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        ConsoleProxyVO proxy = createForUpdate();
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
    public List<ConsoleProxyVO> getProxyListInStates(long dataCenterId, State... states) {
        SearchCriteria sc = DataCenterStatusSearch.create();
        sc.setParameters("states", (Object[])states);
        sc.setParameters("dc", dataCenterId);
        return listActiveBy(sc);
    }

    @Override
    public List<ConsoleProxyVO> getProxyListInStates(State... states) {
        SearchCriteria sc = StateSearch.create();
        sc.setParameters("states", (Object[])states);
        return listActiveBy(sc);
    }
    
    @Override
    public List<ConsoleProxyVO> listByHostId(long hostId) {
        SearchCriteria sc = HostSearch.create();
        sc.setParameters("host", hostId);
        return listActiveBy(sc);
    }
    
    @Override
    public List<ConsoleProxyVO> listUpByHostId(long hostId) {
        SearchCriteria sc = HostUpSearch.create();
        sc.setParameters("host", hostId);
        sc.setParameters("states", new Object[] {State.Destroyed, State.Stopped, State.Expunging}); 
        return listActiveBy(sc);
    }
    
    @Override
    public List<ConsoleProxyLoadInfo> getDatacenterProxyLoadMatrix() {
    	return getDatacenterLoadMatrix(DATACENTER_PROXY_MATRIX);
    }
    
    @Override
    public List<ConsoleProxyLoadInfo> getDatacenterVMLoadMatrix() {
    	return getDatacenterLoadMatrix(DATACENTER_VM_MATRIX);
    }
    
    @Override
    public List<ConsoleProxyLoadInfo> getDatacenterSessionLoadMatrix() {
    	return getDatacenterLoadMatrix(DATACENTER_ACTIVE_SESSION_MATRIX);
    }
    
    @Override
    public List<Pair<Long, Integer>> getProxyLoadMatrix() {
    	ArrayList<Pair<Long, Integer>> l = new ArrayList<Pair<Long, Integer>>();
    	
        Transaction txn = Transaction.currentTxn();;
        PreparedStatement pstmt = null;
        try {
            pstmt = txn.prepareAutoCloseStatement(PROXY_ASSIGNMENT_MATRIX);
            ResultSet rs = pstmt.executeQuery();
            while(rs.next()) {
            	l.add(new Pair<Long, Integer>(rs.getLong(1), rs.getInt(2)));
            }
        } catch (SQLException e) {
        } catch (Throwable e) {
        }
        return l;
    }
    
    @Override
    public List<Pair<Long, Integer>> getDatacenterStoragePoolHostInfo(long dcId, boolean countAllPoolTypes) {
    	ArrayList<Pair<Long, Integer>> l = new ArrayList<Pair<Long, Integer>>();
    	
        Transaction txn = Transaction.currentTxn();;
        PreparedStatement pstmt = null;
        try {
        	if(countAllPoolTypes)
        		pstmt = txn.prepareAutoCloseStatement(STORAGE_POOL_HOST_INFO);
        	else
        		pstmt = txn.prepareAutoCloseStatement(SHARED_STORAGE_POOL_HOST_INFO);
            pstmt.setLong(1, dcId);
            
            ResultSet rs = pstmt.executeQuery();
            while(rs.next()) {
            	l.add(new Pair<Long, Integer>(rs.getLong(1), rs.getInt(2)));
            }
        } catch (SQLException e) {
        } catch (Throwable e) {
        }
        return l;
    }
    
    @Override
    public int getProxyStaticLoad(long proxyVmId) {
        Transaction txn = Transaction.currentTxn();;
        PreparedStatement pstmt = null;
        try {
            pstmt = txn.prepareAutoCloseStatement(GET_PROXY_LOAD);
            pstmt.setLong(1, proxyVmId);
            
            ResultSet rs = pstmt.executeQuery();
            if(rs != null && rs.first())
            	return rs.getInt(1);
        } catch (SQLException e) {
        } catch (Throwable e) {
        }
        return 0;
    }
    
    @Override
    public int getProxyActiveLoad(long proxyVmId) {
        Transaction txn = Transaction.currentTxn();
        PreparedStatement pstmt = null;
        try {
            pstmt = txn.prepareAutoCloseStatement(GET_PROXY_ACTIVE_LOAD);
            pstmt.setLong(1, proxyVmId);
            
            ResultSet rs = pstmt.executeQuery();
            if(rs != null && rs.first())
            	return rs.getInt(1);
        } catch (SQLException e) {
        } catch (Throwable e) {
        }
        return 0;
    }
    
    private List<ConsoleProxyLoadInfo> getDatacenterLoadMatrix(String sql) {
    	ArrayList<ConsoleProxyLoadInfo> l = new ArrayList<ConsoleProxyLoadInfo>();
    	
        Transaction txn = Transaction.currentTxn();;
        PreparedStatement pstmt = null;
        try {
            pstmt = txn.prepareAutoCloseStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            while(rs.next()) {
            	ConsoleProxyLoadInfo info = new ConsoleProxyLoadInfo();
            	info.setId(rs.getLong(1));
            	info.setName(rs.getString(2));
            	info.setCount(rs.getInt(3));
            	l.add(info);
            }
        } catch (SQLException e) {
        } catch (Throwable e) {
        }
        return l;
    }

    @Override
    public List<Long> getRunningProxyListByMsid(long msid) {
    	List<Long> l = new ArrayList<Long>();
        Transaction txn = Transaction.currentTxn();;
        PreparedStatement pstmt = null;
        try {
            pstmt = txn.prepareAutoCloseStatement(
            		"SELECT c.id FROM console_proxy c, vm_instance v, host h " +
            		"WHERE c.id=v.id AND v.state='Running' AND v.host_id=h.id AND h.mgmt_server_id=?");
            
            pstmt.setLong(1, msid);
            ResultSet rs = pstmt.executeQuery();
            while(rs.next()) {
            	l.add(rs.getLong(1));
            }
        } catch (SQLException e) {
        } catch (Throwable e) {
        }
        return l;
    }
}
