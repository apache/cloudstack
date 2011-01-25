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
import java.util.Map;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.uservm.UserVm;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.db.Attribute;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.NicVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine.State;

@Local(value={UserVmDao.class})
public class UserVmDaoImpl extends GenericDaoBase<UserVmVO, Long> implements UserVmDao {
    public static final Logger s_logger = Logger.getLogger(UserVmDaoImpl.class);
    
    protected final SearchBuilder<UserVmVO> RouterStateSearch;
    protected final SearchBuilder<UserVmVO> RouterIdSearch;
    protected final SearchBuilder<UserVmVO> AccountPodSearch;
    protected final SearchBuilder<UserVmVO> AccountDataCenterSearch;
    protected final SearchBuilder<UserVmVO> AccountSearch;
    protected final SearchBuilder<UserVmVO> HostSearch;
    protected final SearchBuilder<UserVmVO> LastHostSearch;
    protected final SearchBuilder<UserVmVO> HostUpSearch;
    protected final SearchBuilder<UserVmVO> HostRunningSearch;
    protected final SearchBuilder<UserVmVO> NameSearch;
    protected final SearchBuilder<UserVmVO> StateChangeSearch;
    protected final SearchBuilder<UserVmVO> ZoneNameSearch;
    protected final SearchBuilder<UserVmVO> AccountHostSearch;

    protected final SearchBuilder<UserVmVO> DestroySearch;
    protected SearchBuilder<UserVmVO> AccountDataCenterVirtualSearch;
    protected SearchBuilder<UserVmVO> UserVmSearch;
    protected final Attribute _updateTimeAttr;
    
    protected final UserVmDetailsDaoImpl _detailsDao = ComponentLocator.inject(UserVmDetailsDaoImpl.class);
    
    protected UserVmDaoImpl() {
        AccountSearch = createSearchBuilder();
        AccountSearch.and("account", AccountSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountSearch.done();
        
        HostSearch = createSearchBuilder();
        HostSearch.and("host", HostSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        HostSearch.done();
        
        LastHostSearch = createSearchBuilder();
        LastHostSearch.and("lastHost", LastHostSearch.entity().getLastHostId(), SearchCriteria.Op.EQ);
        LastHostSearch.and("state", LastHostSearch.entity().getState(), SearchCriteria.Op.EQ);
        LastHostSearch.done();
        
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
        
        DestroySearch = createSearchBuilder();
        DestroySearch.and("state", DestroySearch.entity().getState(), SearchCriteria.Op.IN);
        DestroySearch.and("updateTime", DestroySearch.entity().getUpdateTime(), SearchCriteria.Op.LT);
        DestroySearch.done();

        ZoneNameSearch = createSearchBuilder();
        ZoneNameSearch.and("dataCenterId", ZoneNameSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        ZoneNameSearch.and("name", ZoneNameSearch.entity().getName(), SearchCriteria.Op.EQ);
        ZoneNameSearch.done();
        
        AccountHostSearch = createSearchBuilder();
        AccountHostSearch.and("accountId", AccountHostSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountHostSearch.and("hostId", AccountHostSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        AccountHostSearch.done();

        _updateTimeAttr = _allAttributes.get("updateTime");
        assert _updateTimeAttr != null : "Couldn't get this updateTime attribute";
    }
    
    @Override
    public List<UserVmVO> listByAccountAndPod(long accountId, long podId) {
    	SearchCriteria<UserVmVO> sc = AccountPodSearch.create();
    	sc.setParameters("account", accountId);
    	sc.setParameters("pod", podId);
    	
    	return listIncludingRemovedBy(sc);
    }
    
    @Override
    public List<UserVmVO> listByAccountAndDataCenter(long accountId, long dcId) {
        SearchCriteria<UserVmVO> sc = AccountDataCenterSearch.create();
        sc.setParameters("account", accountId);
        sc.setParameters("dc", dcId);
        
        return listIncludingRemovedBy(sc);
    }
    
    @Override
    public List<UserVmVO> listBy(long routerId, State... states) {
        SearchCriteria<UserVmVO> sc = RouterStateSearch.create();
        SearchCriteria<UserVmVO> ssc = createSearchCriteria();
        
        sc.setParameters("router", routerId);
        for (State state: states) {
            ssc.addOr("state", SearchCriteria.Op.EQ, state.toString());
        }
        sc.addAnd("state", SearchCriteria.Op.SC, ssc);
        return listIncludingRemovedBy(sc);
    }
    
    @Override
    public void updateVM(long id, String displayName, boolean enable, Long osTypeId) {
        UserVmVO vo = createForUpdate();
        vo.setDisplayName(displayName);
        vo.setHaEnabled(enable);
        vo.setGuestOSId(osTypeId);
        update(id, vo);
    }
    
    @Override
    public List<UserVmVO> listByRouterId(long routerId) {
        SearchCriteria<UserVmVO> sc = RouterIdSearch.create();
        
        sc.setParameters("router", routerId);
        
        return listIncludingRemovedBy(sc);
    }

    @Override
    public List<UserVmVO> findDestroyedVms(Date date) {
    	SearchCriteria<UserVmVO> sc = DestroySearch.create();
    	sc.setParameters("state", State.Destroyed, State.Expunging, State.Error);
    	sc.setParameters("updateTime", date);
    	
    	return listBy(sc);
    }
    
    @Override
    public List<UserVmVO> listByAccountId(long id) {
        SearchCriteria<UserVmVO> sc = AccountSearch.create();
        sc.setParameters("account", id);
        return listBy(sc);
    }
    
    @Override
    public List<UserVmVO> listByHostId(Long id) {
        SearchCriteria<UserVmVO> sc = HostSearch.create();
        sc.setParameters("host", id);
        
        return listBy(sc);
    }
    
    @Override
    public List<UserVmVO> listUpByHostId(Long hostId) {
        SearchCriteria<UserVmVO> sc = HostUpSearch.create();
        sc.setParameters("host", hostId);
        sc.setParameters("states", new Object[] {State.Destroyed, State.Stopped, State.Expunging});
        return listBy(sc);
    }
    
    @Override
    public List<UserVmVO> listRunningByHostId(long hostId) {
        SearchCriteria<UserVmVO> sc = HostRunningSearch.create();
        sc.setParameters("host", hostId);
        sc.setParameters("state", State.Running);
        
        return listBy(sc);
    }
    
    @Override
    public UserVmVO findByName(String name) {
        SearchCriteria<UserVmVO> sc = NameSearch.create();
        sc.setParameters("name", name);
        return findOneIncludingRemovedBy(sc);
    }

    @Override
    public List<UserVmVO> listVirtualNetworkInstancesByAcctAndZone(long accountId, long dcId, long networkId) {
        if (AccountDataCenterVirtualSearch == null) {
            NicDao _nicDao = ComponentLocator.getLocator("management-server").getDao(NicDao.class);
            SearchBuilder<NicVO> nicSearch = _nicDao.createSearchBuilder();
            nicSearch.and("networkId", nicSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
            nicSearch.and("ip4Address", nicSearch.entity().getIp4Address(), SearchCriteria.Op.NNULL);

            AccountDataCenterVirtualSearch = createSearchBuilder();
            AccountDataCenterVirtualSearch.and("account", AccountDataCenterVirtualSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
            AccountDataCenterVirtualSearch.and("dc", AccountDataCenterVirtualSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
            AccountDataCenterVirtualSearch.join("nicSearch", nicSearch, AccountDataCenterVirtualSearch.entity().getId(), nicSearch.entity().getInstanceId(), JoinBuilder.JoinType.INNER);
            AccountDataCenterVirtualSearch.done();
        }

        SearchCriteria<UserVmVO> sc = AccountDataCenterVirtualSearch.create();
        sc.setParameters("account", accountId);
        sc.setParameters("dc", dcId);
        sc.setJoinParameters("nicSearch", "networkId", networkId);

        return listBy(sc);
    }
    
    @Override
    public List<UserVmVO> listByNetworkId(long networkId) {
        if (UserVmSearch == null) {
            NicDao _nicDao = ComponentLocator.getLocator("management-server").getDao(NicDao.class);
            SearchBuilder<NicVO> nicSearch = _nicDao.createSearchBuilder();
            nicSearch.and("networkId", nicSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
            nicSearch.and("ip4Address", nicSearch.entity().getIp4Address(), SearchCriteria.Op.NNULL);

            UserVmSearch = createSearchBuilder();
            UserVmSearch.join("nicSearch", nicSearch, UserVmSearch.entity().getId(), nicSearch.entity().getInstanceId(), JoinBuilder.JoinType.INNER);
            UserVmSearch.done();
        }

        SearchCriteria<UserVmVO> sc = UserVmSearch.create();
        sc.setJoinParameters("nicSearch", "networkId", networkId);

        return listBy(sc);
    }

	@Override
	public List<UserVmVO> listByLastHostId(Long hostId) {
		SearchCriteria<UserVmVO> sc = LastHostSearch.create();
		sc.setParameters("lastHost", hostId);
		sc.setParameters("state", State.Stopped);
		return listBy(sc);
	}
	
    @Override
    public UserVm findVmByZoneIdAndName(long zoneId, String name) {
        SearchCriteria<UserVmVO> sc = ZoneNameSearch.create();
        sc.setParameters("dataCenterId", zoneId);
        sc.setParameters("name", name);
        return findOneBy(sc);
    }

	@Override
	public List<UserVmVO> listByAccountIdAndHostId(long accountId, long hostId) {
		SearchCriteria<UserVmVO> sc = AccountHostSearch.create();
		sc.setParameters("hostId", hostId);
		sc.setParameters("accountId", accountId);
		return listBy(sc);
	}

	@Override
	public void loadDetails(UserVmVO vm) {
        Map<String, String> details = _detailsDao.findDetails(vm.getId());
        vm.setDetails(details);
	}
	
	@Override
    public void saveDetails(UserVmVO vm) {
        Map<String, String> details = vm.getDetails();
        if (details == null) {
            return;
        }
        _detailsDao.persist(vm.getId(), details);
    }
}
