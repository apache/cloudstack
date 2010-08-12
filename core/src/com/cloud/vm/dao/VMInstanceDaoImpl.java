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
import com.cloud.vm.State;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
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
    protected final SearchBuilder<VMInstanceVO> ZoneTemplateNonExpungedSearch;
    protected final SearchBuilder<VMInstanceVO> NameLikeSearch;
    protected final SearchBuilder<VMInstanceVO> StateChangeSearch;
    protected final SearchBuilder<VMInstanceVO> TransitionSearch;
    protected final SearchBuilder<VMInstanceVO> TypesSearch;
    protected final SearchBuilder<VMInstanceVO> IdTypesSearch;
    protected final SearchBuilder<VMInstanceVO> HostIdTypesSearch;
    protected final SearchBuilder<VMInstanceVO> HostIdUpTypesSearch;
    
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
        LastHostSearch.done();
       
        ZoneSearch = createSearchBuilder();
        ZoneSearch.and("zone", ZoneSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        ZoneSearch.done();

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
        
        _updateTimeAttr = _allAttributes.get("updateTime");
        assert _updateTimeAttr != null : "Couldn't get this updateTime attribute";
    }

    @Override
    public List<VMInstanceVO> findVMInstancesLike(String name) {
        SearchCriteria sc = NameLikeSearch.create();
        sc.setParameters("name", "%" + name + "%");
        return listActiveBy(sc);
    }

    @Override
    public boolean updateIf(VMInstanceVO vm, VirtualMachine.Event event, Long hostId) {

        State oldState = vm.getState();
        State newState = oldState.getNextState(event);

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
        ub.set(vm, "state", newState);
        ub.set(vm, "hostId", hostId);
        ub.set(vm, _updateTimeAttr, new Date());

        int result = update(vm, sc);
        if (result == 0 && s_logger.isDebugEnabled()) {
            VMInstanceVO vo = findById(vm.getId());
            StringBuilder str = new StringBuilder("Unable to update ").append(vo.toString());
            str.append(": DB Data={Host=").append(vo.getHostId()).append("; State=").append(vo.getState().toString()).append("; updated=").append(vo.getUpdated());
            str.append("} Stale Data: {Host=").append(vm.getHostId()).append("; State=").append(vm.getState().toString()).append("; updated=").append(vm.getUpdated()).append("}");
            s_logger.debug(str.toString());
        }
        return result > 0;
    }

    @Override
    public void updateVM(long id, String displayName, String group, boolean enable) {
        VMInstanceVO vo = createForUpdate();
        vo.setDisplayName(displayName);
        vo.setGroup(group);
        vo.setHaEnabled(enable);
        update(id, vo);
    }
    
    @Override
    public List<VMInstanceVO> listByHostId(long hostid) {
        SearchCriteria sc = HostSearch.create();
        sc.setParameters("host", hostid);

        return listActiveBy(sc);
    }
    
    @Override
	public List<VMInstanceVO> listByLastHostId(long hostId) {
        SearchCriteria sc = LastHostSearch.create();
        sc.setParameters("lastHost", hostId);
        
        return listActiveBy(sc);
	}

    @Override
    public List<VMInstanceVO> listByZoneId(long zoneId) {
        SearchCriteria sc = ZoneSearch.create();
        sc.setParameters("zone", zoneId);

        return listActiveBy(sc);
    }

    @Override
    public List<VMInstanceVO> listNonExpungedByZoneAndTemplate(long zoneId, long templateId) {
        SearchCriteria sc = ZoneTemplateNonExpungedSearch.create();

        sc.setParameters("zone", zoneId);
        sc.setParameters("template", templateId);
        sc.setParameters("state", State.Expunging);

        return listActiveBy(sc);
    }

    @Override
    public List<VMInstanceVO> findVMInTransition(Date time, State... states) {
        SearchCriteria sc = TransitionSearch.create();

        sc.setParameters("states", (Object[]) states);
        sc.setParameters("updateTime", time);

        return search(sc, null);
    }

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
        SearchCriteria sc = HostIdTypesSearch.create();
        sc.setParameters("hostid", hostid);
        sc.setParameters("types", (Object[]) types);
        return listActiveBy(sc);
    }

    @Override
    public List<VMInstanceVO> listUpByHostIdTypes(long hostid, Type... types) {
        SearchCriteria sc = HostIdUpTypesSearch.create();
        sc.setParameters("hostid", hostid);
        sc.setParameters("types", (Object[]) types);
        sc.setParameters("states", new Object[] {State.Destroyed, State.Stopped, State.Expunging}); 
        return listActiveBy(sc);
    }
    
    @Override
    public List<VMInstanceVO> listByTypes(Type... types) {
        SearchCriteria sc = TypesSearch.create();
        sc.setParameters("types", (Object[]) types);
        return listActiveBy(sc);
    }

    @Override
    public VMInstanceVO findByIdTypes(long id, Type... types) {
        SearchCriteria sc = IdTypesSearch.create();
        sc.setParameters("id", id);
        sc.setParameters("types", (Object[]) types);
        return findOneBy(sc);
    }

   
    @Override
    public void updateProxyId(long id, Long proxyId, Date time) {
        VMInstanceVO vo = createForUpdate();
        vo.setProxyId(proxyId);
        vo.setProxyAssignTime(time);
        update(id, vo);
    }
}
