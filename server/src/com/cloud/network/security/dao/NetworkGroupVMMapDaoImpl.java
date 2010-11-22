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

package com.cloud.network.security.dao;

import java.util.List;

import javax.ejb.Local;

import com.cloud.network.security.NetworkGroupVMMapVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.State;

@Local(value={NetworkGroupVMMapDao.class})
public class NetworkGroupVMMapDaoImpl extends GenericDaoBase<NetworkGroupVMMapVO, Long> implements NetworkGroupVMMapDao {
    private SearchBuilder<NetworkGroupVMMapVO> ListByIpAndVmId;
    private SearchBuilder<NetworkGroupVMMapVO> ListByVmId;
    private SearchBuilder<NetworkGroupVMMapVO> ListByVmIdGroupId;

    private GenericSearchBuilder<NetworkGroupVMMapVO, Long> ListVmIdByNetworkGroup;

    private SearchBuilder<NetworkGroupVMMapVO> ListByIp;
    private SearchBuilder<NetworkGroupVMMapVO> ListByNetworkGroup;
    private SearchBuilder<NetworkGroupVMMapVO> ListByNetworkGroupAndStates;

    protected NetworkGroupVMMapDaoImpl() {
        ListByIpAndVmId  = createSearchBuilder();
        ListByIpAndVmId.and("ipAddress", ListByIpAndVmId.entity().getGuestIpAddress(), SearchCriteria.Op.EQ);
        ListByIpAndVmId.and("instanceId", ListByIpAndVmId.entity().getInstanceId(), SearchCriteria.Op.EQ);
        ListByIpAndVmId.done();

        ListVmIdByNetworkGroup = createSearchBuilder(Long.class);
        ListVmIdByNetworkGroup.and("networkGroupId", ListVmIdByNetworkGroup.entity().getNetworkGroupId(), SearchCriteria.Op.EQ);
        ListVmIdByNetworkGroup.selectField(ListVmIdByNetworkGroup.entity().getInstanceId());
        ListVmIdByNetworkGroup.done();
        
        ListByNetworkGroup = createSearchBuilder();
        ListByNetworkGroup.and("networkGroupId", ListByNetworkGroup.entity().getNetworkGroupId(), SearchCriteria.Op.EQ);
        ListByNetworkGroup.done();

        ListByIp  = createSearchBuilder();
        ListByIp.and("ipAddress", ListByIp.entity().getGuestIpAddress(), SearchCriteria.Op.EQ);
        ListByIp.done();

        ListByVmId  = createSearchBuilder();
        ListByVmId.and("instanceId", ListByVmId.entity().getInstanceId(), SearchCriteria.Op.EQ);
        ListByVmId.done();
        
        ListByNetworkGroupAndStates = createSearchBuilder();
        ListByNetworkGroupAndStates.and("networkGroupId", ListByNetworkGroupAndStates.entity().getNetworkGroupId(), SearchCriteria.Op.EQ);
        ListByNetworkGroupAndStates.and("states", ListByNetworkGroupAndStates.entity().getVmState(), SearchCriteria.Op.IN);
        ListByNetworkGroupAndStates.done();
        
        ListByVmIdGroupId  = createSearchBuilder();
        ListByVmIdGroupId.and("instanceId", ListByVmIdGroupId.entity().getInstanceId(), SearchCriteria.Op.EQ);
        ListByVmIdGroupId.and("networkGroupId", ListByVmIdGroupId.entity().getNetworkGroupId(), SearchCriteria.Op.EQ);
        ListByVmIdGroupId.done();
    }

    public List<NetworkGroupVMMapVO> listByIpAndInstanceId(String ipAddress, long vmId) {
        SearchCriteria<NetworkGroupVMMapVO> sc = ListByIpAndVmId.create();
        sc.setParameters("ipAddress", ipAddress);
        sc.setParameters("instanceId", vmId);
        return listBy(sc);
    }

    @Override
    public List<NetworkGroupVMMapVO> listByNetworkGroup(long networkGroupId) {
        SearchCriteria<NetworkGroupVMMapVO> sc = ListByNetworkGroup.create();
        sc.setParameters("networkGroupId", networkGroupId);
        return listBy(sc);
    }

    @Override
    public List<NetworkGroupVMMapVO> listByIp(String ipAddress) {
        SearchCriteria<NetworkGroupVMMapVO> sc = ListByIp.create();
        sc.setParameters("ipAddress", ipAddress);
        return listBy(sc);
    }

    @Override
    public List<NetworkGroupVMMapVO> listByInstanceId(long vmId) {
        SearchCriteria<NetworkGroupVMMapVO> sc = ListByVmId.create();
        sc.setParameters("instanceId", vmId);
        return listBy(sc);
    }

    @Override
    public int deleteVM(long instanceId) {
    	SearchCriteria<NetworkGroupVMMapVO> sc = ListByVmId.create();
        sc.setParameters("instanceId", instanceId);
        return super.expunge(sc);
    }

	@Override
	public List<NetworkGroupVMMapVO> listByNetworkGroup(long networkGroupId, State... vmStates) {
		SearchCriteria<NetworkGroupVMMapVO> sc = ListByNetworkGroupAndStates.create();
		sc.setParameters("networkGroupId", networkGroupId);
		sc.setParameters("states", (Object[])vmStates);
		return listBy(sc);
	}
	
    @Override
    public List<Long> listVmIdsByNetworkGroup(long networkGroupId) {
        SearchCriteria<Long> sc = ListVmIdByNetworkGroup.create();
        sc.setParameters("networkGroupId", networkGroupId);
        return searchIncludingRemoved(sc, null);
    }

	@Override
	public NetworkGroupVMMapVO findByVmIdGroupId(long instanceId, long networkGroupId) {
        SearchCriteria<NetworkGroupVMMapVO> sc = ListByVmIdGroupId.create();
        sc.setParameters("networkGroupId", networkGroupId);
        sc.setParameters("instanceId", instanceId);
		return findOneIncludingRemovedBy(sc);
	}
	
}
