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


import java.util.Date;
import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDaoImpl;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.db.Attribute;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.JoinBuilder.JoinType;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.UpdateBuilder;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.Event;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachine.Type;

@Local(value = { VMInstanceDao.class })
public class VMInstanceDaoImpl extends GenericDaoBase<VMInstanceVO, Long> implements VMInstanceDao {

    public static final Logger s_logger = Logger.getLogger(VMInstanceDaoImpl.class);

    protected final SearchBuilder<VMInstanceVO> VMClusterSearch;
    protected final SearchBuilder<VMInstanceVO> LHVMClusterSearch;
    protected final SearchBuilder<VMInstanceVO> IdStatesSearch;
    protected final SearchBuilder<VMInstanceVO> AllFieldsSearch;
    protected final SearchBuilder<VMInstanceVO> ZoneTemplateNonExpungedSearch;
    protected final SearchBuilder<VMInstanceVO> NameLikeSearch;
    protected final SearchBuilder<VMInstanceVO> StateChangeSearch;
    protected final SearchBuilder<VMInstanceVO> TransitionSearch;
    protected final SearchBuilder<VMInstanceVO> TypesSearch;
    protected final SearchBuilder<VMInstanceVO> IdTypesSearch;
    protected final SearchBuilder<VMInstanceVO> HostIdTypesSearch;
    protected final SearchBuilder<VMInstanceVO> HostIdUpTypesSearch;
    protected final SearchBuilder<VMInstanceVO> HostUpSearch;
    protected final GenericSearchBuilder<VMInstanceVO, Long> CountVirtualRoutersByAccount;

    protected final Attribute _updateTimeAttr;

    protected final HostDaoImpl _hostDao = ComponentLocator.inject(HostDaoImpl.class);
    protected VMInstanceDaoImpl() {
        IdStatesSearch = createSearchBuilder();
        IdStatesSearch.and("id", IdStatesSearch.entity().getId(), Op.EQ);
        IdStatesSearch.and("states", IdStatesSearch.entity().getState(), Op.IN);
        IdStatesSearch.done();
               
        VMClusterSearch = createSearchBuilder();
        SearchBuilder<HostVO> hostSearch = _hostDao.createSearchBuilder();
        VMClusterSearch.join("hostSearch", hostSearch, hostSearch.entity().getId(), VMClusterSearch.entity().getHostId(), JoinType.INNER);
        hostSearch.and("clusterId", hostSearch.entity().getClusterId(), SearchCriteria.Op.EQ);
        VMClusterSearch.done();

        LHVMClusterSearch = createSearchBuilder();
        SearchBuilder<HostVO> hostSearch1 = _hostDao.createSearchBuilder();
        LHVMClusterSearch.join("hostSearch1", hostSearch1, hostSearch1.entity().getId(), LHVMClusterSearch.entity().getLastHostId(), JoinType.INNER);
        hostSearch1.and("clusterId", hostSearch1.entity().getClusterId(), SearchCriteria.Op.EQ);
        LHVMClusterSearch.done();

        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("host", AllFieldsSearch.entity().getHostId(), Op.EQ);
        AllFieldsSearch.and("lastHost", AllFieldsSearch.entity().getLastHostId(), Op.EQ);
        AllFieldsSearch.and("state", AllFieldsSearch.entity().getState(), Op.EQ);
        AllFieldsSearch.and("zone", AllFieldsSearch.entity().getDataCenterIdToDeployIn(), Op.EQ);
        AllFieldsSearch.and("type", AllFieldsSearch.entity().getType(), Op.EQ);
        AllFieldsSearch.and("account", AllFieldsSearch.entity().getAccountId(), Op.EQ);
        AllFieldsSearch.done();

        ZoneTemplateNonExpungedSearch = createSearchBuilder();
        ZoneTemplateNonExpungedSearch.and("zone", ZoneTemplateNonExpungedSearch.entity().getDataCenterIdToDeployIn(), Op.EQ);
        ZoneTemplateNonExpungedSearch.and("template", ZoneTemplateNonExpungedSearch.entity().getTemplateId(), Op.EQ);
        ZoneTemplateNonExpungedSearch.and("state", ZoneTemplateNonExpungedSearch.entity().getState(), Op.NEQ);
        ZoneTemplateNonExpungedSearch.done();

        NameLikeSearch = createSearchBuilder();
        NameLikeSearch.and("name", NameLikeSearch.entity().getHostName(), Op.LIKE);
        NameLikeSearch.done();

        StateChangeSearch = createSearchBuilder();
        StateChangeSearch.and("id", StateChangeSearch.entity().getId(), Op.EQ);
        StateChangeSearch.and("states", StateChangeSearch.entity().getState(), Op.EQ);
        StateChangeSearch.and("host", StateChangeSearch.entity().getHostId(), Op.EQ);
        StateChangeSearch.and("update", StateChangeSearch.entity().getUpdated(), Op.EQ);
        StateChangeSearch.done();

        TransitionSearch = createSearchBuilder();
        TransitionSearch.and("updateTime", TransitionSearch.entity().getUpdateTime(), Op.LT);
        TransitionSearch.and("states", TransitionSearch.entity().getState(), Op.IN);
        TransitionSearch.done();

        TypesSearch = createSearchBuilder();
        TypesSearch.and("types", TypesSearch.entity().getType(), Op.IN);
        TypesSearch.done();

        IdTypesSearch = createSearchBuilder();
        IdTypesSearch.and("id", IdTypesSearch.entity().getId(), Op.EQ);
        IdTypesSearch.and("types", IdTypesSearch.entity().getType(), Op.IN);
        IdTypesSearch.done();
        
        HostIdTypesSearch = createSearchBuilder();
        HostIdTypesSearch.and("hostid", HostIdTypesSearch.entity().getHostId(), Op.EQ);
        HostIdTypesSearch.and("types", HostIdTypesSearch.entity().getType(), Op.IN);
        HostIdTypesSearch.done();
        
        HostIdUpTypesSearch = createSearchBuilder();
        HostIdUpTypesSearch.and("hostid", HostIdUpTypesSearch.entity().getHostId(), Op.EQ);
        HostIdUpTypesSearch.and("types", HostIdUpTypesSearch.entity().getType(), Op.IN);
        HostIdUpTypesSearch.and("states", HostIdUpTypesSearch.entity().getState(), Op.NIN);
        HostIdUpTypesSearch.done();
        
        HostUpSearch = createSearchBuilder();
        HostUpSearch.and("host", HostUpSearch.entity().getHostId(), Op.EQ);
        HostUpSearch.and("states", HostUpSearch.entity().getState(), Op.IN);
        HostUpSearch.done();
        
        CountVirtualRoutersByAccount = createSearchBuilder(Long.class);
        CountVirtualRoutersByAccount.select(null, Func.COUNT, null);
        CountVirtualRoutersByAccount.and("account", CountVirtualRoutersByAccount.entity().getAccountId(), SearchCriteria.Op.EQ);
        CountVirtualRoutersByAccount.and("type", CountVirtualRoutersByAccount.entity().getType(), SearchCriteria.Op.EQ);
        CountVirtualRoutersByAccount.and("state", CountVirtualRoutersByAccount.entity().getState(), SearchCriteria.Op.NIN);        
        CountVirtualRoutersByAccount.done();

        _updateTimeAttr = _allAttributes.get("updateTime");
        assert _updateTimeAttr != null : "Couldn't get this updateTime attribute";
    }

    @Override
    public List<VMInstanceVO> listByAccountId(long accountId) {
        SearchCriteria<VMInstanceVO> sc = AllFieldsSearch.create();
        sc.setParameters("account", accountId);
        return listBy(sc);
    }
    
    @Override
    public List<VMInstanceVO> findVMInstancesLike(String name) {
        SearchCriteria<VMInstanceVO> sc = NameLikeSearch.create();
        sc.setParameters("name", "%" + name + "%");
        return listBy(sc);
    }

    @Override
    public List<VMInstanceVO> listByHostId(long hostid) {
        SearchCriteria<VMInstanceVO> sc = AllFieldsSearch.create();
        sc.setParameters("host", hostid);

        return listBy(sc);
    }
    
    @Override
    public List<VMInstanceVO> listByZoneId(long zoneId) {
        SearchCriteria<VMInstanceVO> sc = AllFieldsSearch.create();
        sc.setParameters("zone", zoneId);

        return listBy(sc);
    }
    
    @Override
    public List<VMInstanceVO> listByClusterId(long clusterId) {
        SearchCriteria<VMInstanceVO> sc = VMClusterSearch.create();
        sc.setJoinParameters("hostSearch", "clusterId", clusterId);

        return listBy(sc);
    }
    

    @Override
    public List<VMInstanceVO> listLHByClusterId(long clusterId) {
        SearchCriteria<VMInstanceVO> sc = LHVMClusterSearch.create();
        sc.setJoinParameters("hostSearch1", "clusterId", clusterId);
        return listBy(sc);
    }

    @Override
    public List<VMInstanceVO> listByZoneIdAndType(long zoneId, VirtualMachine.Type type) {
        SearchCriteria<VMInstanceVO> sc = AllFieldsSearch.create();
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
        SearchCriteria<VMInstanceVO> sc = AllFieldsSearch.create();
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
    public boolean updateState(State oldState, Event event,	State newState, VirtualMachine vm, Long hostId) {
    	if (newState == null) {
    		if (s_logger.isDebugEnabled()) {
    			s_logger.debug("There's no way to transition from old state: " + oldState.toString() + " event: " + event.toString());
    		}
    		return false;
    	}
    	
    	VMInstanceVO vmi = (VMInstanceVO)vm;
    	Long oldHostId = vmi.getHostId();
    	Long oldUpdated = vmi.getUpdated();
    	Date oldUpdateDate = vmi.getUpdateTime();
    	
    	SearchCriteria<VMInstanceVO> sc = StateChangeSearch.create();
    	sc.setParameters("id", vmi.getId());
    	sc.setParameters("states", oldState);
    	sc.setParameters("host", vmi.getHostId());
    	sc.setParameters("update", vmi.getUpdated());

    	vmi.incrUpdated();
    	UpdateBuilder ub = getUpdateBuilder(vmi);
    	
    	ub.set(vmi, "state", newState);
    	ub.set(vmi, "hostId", hostId);
    	ub.set(vmi, "podIdToDeployIn", vmi.getPodIdToDeployIn());
    	ub.set(vmi, _updateTimeAttr, new Date());

    	int result = update(vmi, sc);
    	if (result == 0 && s_logger.isDebugEnabled()) {

    		VMInstanceVO vo = findByIdIncludingRemoved(vm.getId());
    		
    		if (vo != null) {
        		StringBuilder str = new StringBuilder("Unable to update ").append(vo.toString());
        		str.append(": DB Data={Host=").append(vo.getHostId()).append("; State=").append(vo.getState().toString()).append("; updated=").append(vo.getUpdated()).append("; time=").append(vo.getUpdateTime());
        		str.append("} New Data: {Host=").append(vm.getHostId()).append("; State=").append(vm.getState().toString()).append("; updated=").append(vmi.getUpdated()).append("; time=").append(vo.getUpdateTime());
        		str.append("} Stale Data: {Host=").append(oldHostId).append("; State=").append(oldState).append("; updated=").append(oldUpdated).append("; time=").append(oldUpdateDate).append("}");
        		s_logger.debug(str.toString());

    		} else {
    		    s_logger.debug("Unable to update the vm id=" + vm.getId() + "; the vm either doesn't exist or already removed");
    		}
    	}
    	return result > 0;
    }
    
    @Override
	public List<VMInstanceVO> listByLastHostId(Long hostId) {
		SearchCriteria<VMInstanceVO> sc = AllFieldsSearch.create();
		sc.setParameters("lastHost", hostId);
		sc.setParameters("state", State.Stopped);
		return listBy(sc);
	}
    
    @Override
    public Long countAllocatedVirtualRoutersForAccount(long accountId) {
    	SearchCriteria<Long> sc = CountVirtualRoutersByAccount.create();
        sc.setParameters("account", accountId);
        sc.setParameters("type", VirtualMachine.Type.DomainRouter);
		sc.setParameters("state", new Object[] {State.Destroyed, State.Error, State.Expunging});
        return customSearch(sc, null).get(0);
    }
    
    @Override
    public List<VMInstanceVO> listVmsMigratingFromHost(Long hostId) {
        SearchCriteria<VMInstanceVO> sc = AllFieldsSearch.create();
        sc.setParameters("lastHost", hostId);
        sc.setParameters("state", State.Migrating);
        return listBy(sc);
    }
}
