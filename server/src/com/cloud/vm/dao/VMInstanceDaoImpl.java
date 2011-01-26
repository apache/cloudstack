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
import java.util.Date;
import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.utils.db.Attribute;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.UpdateBuilder;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.Event;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachine.Type;

@Local(value = { VMInstanceDao.class })
public class VMInstanceDaoImpl extends GenericDaoBase<VMInstanceVO, Long> implements VMInstanceDao {

    public static final Logger s_logger = Logger.getLogger(VMInstanceDaoImpl.class.getName());

    private static final String COUNT_ROUTERS_AND_PROXIES = "SELECT count(*) from `cloud`.`vm_instance` where host_id = ? AND type = 'DomainRouter'" + " UNION ALL"
            + " SELECT count(*) from `cloud`.`vm_instance` where host_id = ? AND type = 'ConsoleProxy'";

    protected final SearchBuilder<VMInstanceVO> IdStatesSearch;
    protected final SearchBuilder<VMInstanceVO> HostSearch;
    protected final SearchBuilder<VMInstanceVO> LastHostSearch;
    protected final SearchBuilder<VMInstanceVO> ZoneSearch;
    protected final SearchBuilder<VMInstanceVO> ZoneVmTypeSearch;
    protected final SearchBuilder<VMInstanceVO> ZoneTemplateNonExpungedSearch;
    protected final SearchBuilder<VMInstanceVO> NameLikeSearch;
    protected final SearchBuilder<VMInstanceVO> StateChangeSearch;
    protected final SearchBuilder<VMInstanceVO> TransitionSearch;
    protected final SearchBuilder<VMInstanceVO> TypesSearch;
    protected final SearchBuilder<VMInstanceVO> IdTypesSearch;
    protected final SearchBuilder<VMInstanceVO> HostIdTypesSearch;
    protected final SearchBuilder<VMInstanceVO> HostIdUpTypesSearch;
    protected final SearchBuilder<VMInstanceVO> HostUpSearch;
    protected final SearchBuilder<VMInstanceVO> TypeStateSearch;
    
    protected final Attribute _updateTimeAttr;

    protected VMInstanceDaoImpl() {
        IdStatesSearch = createSearchBuilder();
        IdStatesSearch.and("id", IdStatesSearch.entity().getId(), SearchCriteria.Op.EQ);
        IdStatesSearch.and("states", IdStatesSearch.entity().getState(), SearchCriteria.Op.IN);
        IdStatesSearch.done();

        HostSearch = createSearchBuilder();
        HostSearch.and("host", HostSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        HostSearch.done();
        
        LastHostSearch = createSearchBuilder();
        LastHostSearch.and("lastHost", LastHostSearch.entity().getLastHostId(), SearchCriteria.Op.EQ);
        LastHostSearch.and("state", LastHostSearch.entity().getState(), SearchCriteria.Op.EQ);
        LastHostSearch.done();
       
        ZoneSearch = createSearchBuilder();
        ZoneSearch.and("zone", ZoneSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        ZoneSearch.done();
        
        ZoneVmTypeSearch = createSearchBuilder();
        ZoneVmTypeSearch.and("zone", ZoneVmTypeSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        ZoneVmTypeSearch.and("type", ZoneVmTypeSearch.entity().getType(), SearchCriteria.Op.EQ);
        ZoneVmTypeSearch.done();

        ZoneTemplateNonExpungedSearch = createSearchBuilder();
        ZoneTemplateNonExpungedSearch.and("zone", ZoneTemplateNonExpungedSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        ZoneTemplateNonExpungedSearch.and("template", ZoneTemplateNonExpungedSearch.entity().getTemplateId(), SearchCriteria.Op.EQ);
        ZoneTemplateNonExpungedSearch.and("state", ZoneTemplateNonExpungedSearch.entity().getState(), SearchCriteria.Op.NEQ);
        ZoneTemplateNonExpungedSearch.done();

        NameLikeSearch = createSearchBuilder();
        NameLikeSearch.and("name", NameLikeSearch.entity().getName(), SearchCriteria.Op.LIKE);
        NameLikeSearch.done();

        StateChangeSearch = createSearchBuilder();
        StateChangeSearch.and("id", StateChangeSearch.entity().getId(), SearchCriteria.Op.EQ);
        StateChangeSearch.and("states", StateChangeSearch.entity().getState(), SearchCriteria.Op.EQ);
        StateChangeSearch.and("host", StateChangeSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        StateChangeSearch.and("update", StateChangeSearch.entity().getUpdated(), SearchCriteria.Op.EQ);
        StateChangeSearch.done();

        TransitionSearch = createSearchBuilder();
        TransitionSearch.and("updateTime", TransitionSearch.entity().getUpdateTime(), SearchCriteria.Op.LT);
        TransitionSearch.and("states", TransitionSearch.entity().getState(), SearchCriteria.Op.IN);
        TransitionSearch.done();

        TypesSearch = createSearchBuilder();
        TypesSearch.and("types", TypesSearch.entity().getType(), SearchCriteria.Op.IN);
        TypesSearch.done();

        IdTypesSearch = createSearchBuilder();
        IdTypesSearch.and("id", IdTypesSearch.entity().getId(), SearchCriteria.Op.EQ);
        IdTypesSearch.and("types", IdTypesSearch.entity().getType(), SearchCriteria.Op.IN);
        IdTypesSearch.done();
        
        HostIdTypesSearch = createSearchBuilder();
        HostIdTypesSearch.and("hostid", HostIdTypesSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        HostIdTypesSearch.and("types", HostIdTypesSearch.entity().getType(), SearchCriteria.Op.IN);
        HostIdTypesSearch.done();
        
        HostIdUpTypesSearch = createSearchBuilder();
        HostIdUpTypesSearch.and("hostid", HostIdUpTypesSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        HostIdUpTypesSearch.and("types", HostIdUpTypesSearch.entity().getType(), SearchCriteria.Op.IN);
        HostIdUpTypesSearch.and("states", HostIdUpTypesSearch.entity().getState(), SearchCriteria.Op.NIN);
        HostIdUpTypesSearch.done();
        
        HostUpSearch = createSearchBuilder();
        HostUpSearch.and("host", HostUpSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        HostUpSearch.and("states", HostUpSearch.entity().getState(), SearchCriteria.Op.IN);
        HostUpSearch.done();
        
        TypeStateSearch = createSearchBuilder();
        TypeStateSearch.and("type", TypeStateSearch.entity().getType(), SearchCriteria.Op.EQ);
        TypeStateSearch.and("state", TypeStateSearch.entity().getState(), SearchCriteria.Op.EQ);
        TypeStateSearch.done();
        
        _updateTimeAttr = _allAttributes.get("updateTime");
        assert _updateTimeAttr != null : "Couldn't get this updateTime attribute";
    }

    @Override
    public List<VMInstanceVO> findVMInstancesLike(String name) {
        SearchCriteria<VMInstanceVO> sc = NameLikeSearch.create();
        sc.setParameters("name", "%" + name + "%");
        return listBy(sc);
    }

    @Override
    public List<VMInstanceVO> listByHostId(long hostid) {
        SearchCriteria<VMInstanceVO> sc = HostSearch.create();
        sc.setParameters("host", hostid);

        return listBy(sc);
    }
    
    @Override
    public List<VMInstanceVO> listByZoneId(long zoneId) {
        SearchCriteria<VMInstanceVO> sc = ZoneSearch.create();
        sc.setParameters("zone", zoneId);

        return listBy(sc);
    }

    @Override
    public List<VMInstanceVO> listByZoneIdAndType(long zoneId, VirtualMachine.Type type) {
        SearchCriteria<VMInstanceVO> sc = ZoneVmTypeSearch.create();
        sc.setParameters("zone", zoneId);
        sc.setParameters("type", type.toString());
        return listBy(sc);
    }
    
    
    @Override
    public List<VMInstanceVO> listNonExpungedByZoneAndTemplate(long zoneId, long templateId) {
        SearchCriteria<VMInstanceVO> sc = ZoneTemplateNonExpungedSearch.create();

        sc.setParameters("zone", zoneId);
        sc.setParameters("template", templateId);
        sc.setParameters("state", State.Expunging);

        return listBy(sc);
    }

    @Override
    public List<VMInstanceVO> findVMInTransition(Date time, State... states) {
        SearchCriteria<VMInstanceVO> sc = TransitionSearch.create();

        sc.setParameters("states", (Object[]) states);
        sc.setParameters("updateTime", time);

        return search(sc, null);
    }

    @Override
    public Integer[] countRoutersAndProxies(Long hostId) {
        Transaction txn = Transaction.currentTxn();
        PreparedStatement pstmt = null;
        Integer[] routerAndProxyCount = new Integer[] { null, null };
        try {
            String sql = COUNT_ROUTERS_AND_PROXIES;
            pstmt = txn.prepareAutoCloseStatement(sql);
            pstmt.setLong(1, hostId);
            pstmt.setLong(2, hostId);
            ResultSet rs = pstmt.executeQuery();
            int i = 0;
            while (rs.next()) {
                routerAndProxyCount[i++] = rs.getInt(1);
            }
        } catch (Exception e) {
            s_logger.warn("Exception searching for routers and proxies", e);
        }
        return routerAndProxyCount;
    }


    @Override
    public List<VMInstanceVO> listByHostIdTypes(long hostid, Type... types) {
        SearchCriteria<VMInstanceVO> sc = HostIdTypesSearch.create();
        sc.setParameters("hostid", hostid);
        sc.setParameters("types", (Object[]) types);
        return listBy(sc);
    }

    @Override
    public List<VMInstanceVO> listUpByHostIdTypes(long hostid, Type... types) {
        SearchCriteria<VMInstanceVO> sc = HostIdUpTypesSearch.create();
        sc.setParameters("hostid", hostid);
        sc.setParameters("types", (Object[]) types);
        sc.setParameters("states", new Object[] {State.Destroyed, State.Stopped, State.Expunging}); 
        return listBy(sc);
    }
    
    @Override
    public List<VMInstanceVO> listUpByHostId(Long hostId) {
        SearchCriteria<VMInstanceVO> sc = HostUpSearch.create();
        sc.setParameters("host", hostId);
        sc.setParameters("states", new Object[] {State.Starting, State.Running});
        return listBy(sc);
    }
    
    @Override
    public List<VMInstanceVO> listByTypes(Type... types) {
        SearchCriteria<VMInstanceVO> sc = TypesSearch.create();
        sc.setParameters("types", (Object[]) types);
        return listBy(sc);
    }
    
    @Override
    public List<VMInstanceVO> listByTypeAndState(State state, VirtualMachine.Type type) {
        SearchCriteria<VMInstanceVO> sc = TypeStateSearch.create();
        sc.setParameters("type", type);
        sc.setParameters("state", state);
        return listBy(sc);
    }

    @Override
    public VMInstanceVO findByIdTypes(long id, Type... types) {
        SearchCriteria<VMInstanceVO> sc = IdTypesSearch.create();
        sc.setParameters("id", id);
        sc.setParameters("types", (Object[]) types);
        return findOneIncludingRemovedBy(sc);
    }

   
    @Override
    public void updateProxyId(long id, Long proxyId, Date time) {
        VMInstanceVO vo = createForUpdate();
        vo.setProxyId(proxyId);
        vo.setProxyAssignTime(time);
        update(id, vo);
    }

    @Override
    public boolean updateState(State oldState, Event event,
    		State newState, VirtualMachine vm, Long hostId) {
    	if (newState == null) {
    		if (s_logger.isDebugEnabled()) {
    			s_logger.debug("There's no way to transition from old state: " + oldState.toString() + " event: " + event.toString());
    		}
    		return false;
    	}
    	VMInstanceVO vmi = (VMInstanceVO)vm;

    	SearchCriteria<VMInstanceVO> sc = StateChangeSearch.create();
    	sc.setParameters("id", vmi.getId());
    	sc.setParameters("states", oldState);
    	sc.setParameters("host", vmi.getHostId());
    	sc.setParameters("update", vmi.getUpdated());

    	vmi.incrUpdated();
    	UpdateBuilder ub = getUpdateBuilder(vmi);
    	ub.set(vmi, "state", newState);
    	ub.set(vmi, "hostId", hostId);
    	ub.set(vmi, _updateTimeAttr, new Date());

    	int result = update(vmi, sc);
    	if (result == 0 && s_logger.isDebugEnabled()) {
    		VMInstanceVO vo = findById(vm.getId());
    		StringBuilder str = new StringBuilder("Unable to update ").append(vo.toString());
    		str.append(": DB Data={Host=").append(vo.getHostId()).append("; State=").append(vo.getState().toString()).append("; updated=").append(vo.getUpdated());
    		str.append("} Stale Data: {Host=").append(vm.getHostId()).append("; State=").append(vm.getState().toString()).append("; updated=").append(vmi.getUpdated()).append("}");
    		s_logger.debug(str.toString());
    	}
    	return result > 0;
    }
    
    @Override
	public List<VMInstanceVO> listByLastHostId(Long hostId) {
		SearchCriteria<VMInstanceVO> sc = LastHostSearch.create();
		sc.setParameters("lastHost", hostId);
		sc.setParameters("state", State.Stopped);
		return listBy(sc);
	}
}
