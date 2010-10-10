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

import com.cloud.service.ServiceOffering;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.db.Attribute;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.UpdateBuilder;
import com.cloud.vm.State;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;

@Local(value={UserVmDao.class})
public class UserVmDaoImpl extends GenericDaoBase<UserVmVO, Long> implements UserVmDao {
    public static final Logger s_logger = Logger.getLogger(UserVmDaoImpl.class.getName());
    
    protected final SearchBuilder<UserVmVO> RouterStateSearch;
    protected final SearchBuilder<UserVmVO> RouterIdSearch;
    protected final SearchBuilder<UserVmVO> AccountPodSearch;
    protected final SearchBuilder<UserVmVO> AccountDataCenterSearch;
    protected final SearchBuilder<UserVmVO> AccountSearch;
    protected final SearchBuilder<UserVmVO> HostSearch;
    protected final SearchBuilder<UserVmVO> HostUpSearch;
    protected final SearchBuilder<UserVmVO> HostRunningSearch;
    protected final SearchBuilder<UserVmVO> NameSearch;
    protected final SearchBuilder<UserVmVO> StateChangeSearch;
    protected final SearchBuilder<UserVmVO> StorageIpSearch;
    protected final SearchBuilder<UserVmVO> GuestIpSearch;

    protected final SearchBuilder<UserVmVO> DestroySearch;
    protected SearchBuilder<UserVmVO> AccountDataCenterVirtualSearch;
    protected final Attribute _updateTimeAttr;
    
    protected UserVmDaoImpl() {
        AccountSearch = createSearchBuilder();
        AccountSearch.and("account", AccountSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountSearch.done();
        
        HostSearch = createSearchBuilder();
        HostSearch.and("host", HostSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        HostSearch.done();
        
        HostUpSearch = createSearchBuilder();
        HostUpSearch.and("host", HostUpSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        HostUpSearch.and("states", HostUpSearch.entity().getState(), SearchCriteria.Op.NIN);
        HostUpSearch.done();
        
        HostRunningSearch = createSearchBuilder();
        HostRunningSearch.and("host", HostRunningSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        HostRunningSearch.and("state", HostRunningSearch.entity().getState(), SearchCriteria.Op.EQ);
        HostRunningSearch.done();
        
        NameSearch = createSearchBuilder();
        NameSearch.and("name", NameSearch.entity().getName(), SearchCriteria.Op.EQ);
        NameSearch.done();
        
        RouterStateSearch = createSearchBuilder();
        RouterStateSearch.and("router", RouterStateSearch.entity().getDomainRouterId(), SearchCriteria.Op.EQ);
        RouterStateSearch.done();
        
        RouterIdSearch = createSearchBuilder();
        RouterIdSearch.and("router", RouterIdSearch.entity().getDomainRouterId(), SearchCriteria.Op.EQ);
        RouterIdSearch.done();
        
        AccountPodSearch = createSearchBuilder();
        AccountPodSearch.and("account", AccountPodSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountPodSearch.and("pod", AccountPodSearch.entity().getPodId(), SearchCriteria.Op.EQ);
        AccountPodSearch.done();

        AccountDataCenterSearch = createSearchBuilder();
        AccountDataCenterSearch.and("account", AccountDataCenterSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountDataCenterSearch.and("dc", AccountDataCenterSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        AccountDataCenterSearch.done();

        StateChangeSearch = createSearchBuilder();
        StateChangeSearch.and("id", StateChangeSearch.entity().getId(), SearchCriteria.Op.EQ);
        StateChangeSearch.and("states", StateChangeSearch.entity().getState(), SearchCriteria.Op.EQ);
        StateChangeSearch.and("host", StateChangeSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        StateChangeSearch.and("update", StateChangeSearch.entity().getUpdated(), SearchCriteria.Op.EQ);
        StateChangeSearch.done();
        
        StorageIpSearch = createSearchBuilder();
        StorageIpSearch.and("dc", StorageIpSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        StorageIpSearch.and("pod", StorageIpSearch.entity().getPodId(), SearchCriteria.Op.EQ);
        StorageIpSearch.and("ip", StorageIpSearch.entity().getStorageIp(), SearchCriteria.Op.EQ);
        StorageIpSearch.done();
        
        GuestIpSearch = createSearchBuilder();
        GuestIpSearch.and("dc", GuestIpSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        GuestIpSearch.and("ip", GuestIpSearch.entity().getGuestIpAddress(), SearchCriteria.Op.EQ);
        GuestIpSearch.and("states", GuestIpSearch.entity().getState(), SearchCriteria.Op.NIN);
        GuestIpSearch.done();

        DestroySearch = createSearchBuilder();
        DestroySearch.and("state", DestroySearch.entity().getState(), SearchCriteria.Op.IN);
        DestroySearch.and("updateTime", DestroySearch.entity().getUpdateTime(), SearchCriteria.Op.LT);
        DestroySearch.done();

        _updateTimeAttr = _allAttributes.get("updateTime");
        assert _updateTimeAttr != null : "Couldn't get this updateTime attribute";
    }
    
    public List<UserVmVO> listByAccountAndPod(long accountId, long podId) {
    	SearchCriteria sc = AccountPodSearch.create();
    	sc.setParameters("account", accountId);
    	sc.setParameters("pod", podId);
    	
    	return listBy(sc);
    }
    
    public List<UserVmVO> listByAccountAndDataCenter(long accountId, long dcId) {
        SearchCriteria sc = AccountDataCenterSearch.create();
        sc.setParameters("account", accountId);
        sc.setParameters("dc", dcId);
        
        return listBy(sc);
    }
    
    @Override
    public List<UserVmVO> listBy(long routerId, State... states) {
        SearchCriteria sc = RouterStateSearch.create();
        SearchCriteria ssc = createSearchCriteria();
        
        sc.setParameters("router", routerId);
        for (State state: states) {
            ssc.addOr("state", SearchCriteria.Op.EQ, state.toString());
        }
        sc.addAnd("state", SearchCriteria.Op.SC, ssc);
        return listBy(sc);
    }
    
    @Override
    public List<UserVmVO> listByRouterId(long routerId) {
        SearchCriteria sc = RouterIdSearch.create();
        
        sc.setParameters("router", routerId);
        
        return listBy(sc);
    }

    @Override
    public boolean updateIf(UserVmVO vm, VirtualMachine.Event event, Long hostId) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("UpdateIf called " + vm.toString() + " event " + event.toString() + " host " + hostId);
        }
    	
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
        	// save current running host id to last_host_id field
        	ub.set(vm, "lastHostId", vm.getHostId());
        } else if(newState == State.Expunging) {
        	ub.set(vm, "lastHostId", null);
        }
        
        ub.set(vm, "state", newState);
        ub.set(vm, "hostId", hostId);
        ub.set(vm, _updateTimeAttr, new Date());
        
        int result = update(vm, sc);
        if (result == 0 && s_logger.isDebugEnabled()) {
        	UserVmVO vo = findById(vm.getId());
        	StringBuilder str = new StringBuilder("Unable to update ").append(vo.toString());
        	str.append(": DB Data={Host=").append(vo.getHostId()).append("; State=").append(vo.getState().toString()).append("; updated=").append(vo.getUpdated());
        	str.append("} New Data: {Host=").append(vm.getHostId()).append("; State=").append(vm.getState().toString()).append("; updated=").append(vm.getUpdated());
        	str.append("} Stale Data: {Host=").append(oldHostId).append("; State=").append(oldState.toString()).append("; updated=").append(oldDate).append("}");
        	s_logger.debug(str.toString());
        }
        
        return result > 0;
    }
    
    @Override
    public List<UserVmVO> findVMsUsingIpAddress(long dcId, long podId, String ipAddress) {
    	SearchCriteria sc = StorageIpSearch.create();
    	sc.setParameters("dc", dcId);
    	sc.setParameters("pod", podId);
    	sc.setParameters("ip", ipAddress);
    	
    	return listActiveBy(sc);
    }
    
    @Override
    public List<UserVmVO> findDestroyedVms(Date date) {
    	SearchCriteria sc = DestroySearch.create();
    	sc.setParameters("state", State.Destroyed, State.Expunging, State.Error);
    	sc.setParameters("updateTime", date);
    	
    	return listActiveBy(sc);
    }
    
    public List<UserVmVO> listByAccountId(long id) {
        SearchCriteria sc = AccountSearch.create();
        sc.setParameters("account", id);
        return listActiveBy(sc);
    }
    
    public List<UserVmVO> listByHostId(Long id) {
        SearchCriteria sc = HostSearch.create();
        sc.setParameters("host", id);
        
        return listActiveBy(sc);
    }
    
    @Override
    public List<UserVmVO> listUpByHostId(Long hostId) {
        SearchCriteria sc = HostUpSearch.create();
        sc.setParameters("host", hostId);
        sc.setParameters("states", new Object[] {State.Destroyed, State.Stopped, State.Expunging});
        return listActiveBy(sc);
    }
    
    public List<UserVmVO> listRunningByHostId(long hostId) {
        SearchCriteria sc = HostRunningSearch.create();
        sc.setParameters("host", hostId);
        sc.setParameters("state", State.Running);
        
        return listActiveBy(sc);
    }
    
    public UserVmVO findByName(String name) {
        SearchCriteria sc = NameSearch.create();
        sc.setParameters("name", name);
        return findOneBy(sc);
    }

    @Override
    public List<UserVmVO> listVirtualNetworkInstancesByAcctAndZone(long accountId, long dcId) {
        if (AccountDataCenterVirtualSearch == null) {
            ServiceOfferingDao offeringDao = ComponentLocator.getLocator("management-server").getDao(ServiceOfferingDao.class);
            SearchBuilder<ServiceOfferingVO> offeringSearch = offeringDao.createSearchBuilder();
            offeringSearch.and("guestIpType", offeringSearch.entity().getGuestIpType(), SearchCriteria.Op.EQ);

            AccountDataCenterVirtualSearch = createSearchBuilder();
            AccountDataCenterVirtualSearch.and("account", AccountDataCenterVirtualSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
            AccountDataCenterVirtualSearch.and("dc", AccountDataCenterVirtualSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
            AccountDataCenterVirtualSearch.join("offeringSearch", offeringSearch, AccountDataCenterVirtualSearch.entity().getServiceOfferingId(), offeringSearch.entity().getId());
            AccountDataCenterVirtualSearch.done();
        }

        SearchCriteria sc = AccountDataCenterVirtualSearch.create();
        sc.setParameters("account", accountId);
        sc.setParameters("dc", dcId);
        sc.setJoinParameters("offeringSearch", "guestIpType", ServiceOffering.GuestIpType.Virtualized);

        return listActiveBy(sc);
    }

	@Override
	public List<UserVmVO> listVmsUsingGuestIpAddress(long dcId, String ipAddress) {
    	SearchCriteria sc = GuestIpSearch.create();
    	sc.setParameters("dc", dcId);
    	sc.setParameters("ip", ipAddress);
    	sc.setParameters("states", new Object[] {State.Destroyed,  State.Expunging});
    	
    	return listActiveBy(sc);
	}
}
