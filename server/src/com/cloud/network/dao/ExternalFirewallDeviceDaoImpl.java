/**
 *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
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

import java.util.List;
import javax.ejb.Local;
import com.cloud.network.ExternalFirewallDeviceVO;
import com.cloud.network.ExternalFirewallDeviceVO.FirewallDeviceAllocationState;
import com.cloud.network.ExternalFirewallDeviceVO.FirewallDeviceState;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Local(value=ExternalFirewallDeviceDao.class) @DB(txn=false)
public class ExternalFirewallDeviceDaoImpl extends GenericDaoBase<ExternalFirewallDeviceVO, Long> implements ExternalFirewallDeviceDao {
    final SearchBuilder<ExternalFirewallDeviceVO> physicalNetworkServiceProviderSearch;
    final SearchBuilder<ExternalFirewallDeviceVO> physicalNetworkIdSearch;
    final SearchBuilder<ExternalFirewallDeviceVO> allocationStateSearch;
    final SearchBuilder<ExternalFirewallDeviceVO> deviceStatusSearch;

    protected ExternalFirewallDeviceDaoImpl() {
        physicalNetworkIdSearch = createSearchBuilder();
        physicalNetworkIdSearch.and("physicalNetworkId", physicalNetworkIdSearch.entity().getPhysicalNetworkId(), Op.EQ);
        physicalNetworkIdSearch.done();

        physicalNetworkServiceProviderSearch = createSearchBuilder();
        physicalNetworkServiceProviderSearch.and("physicalNetworkId", physicalNetworkServiceProviderSearch.entity().getPhysicalNetworkId(), Op.EQ);
        physicalNetworkServiceProviderSearch.and("networkServiceProviderName", physicalNetworkServiceProviderSearch.entity().getProviderName(), Op.EQ);
        physicalNetworkServiceProviderSearch.done();

        allocationStateSearch = createSearchBuilder();
        allocationStateSearch.and("physicalNetworkId", allocationStateSearch.entity().getPhysicalNetworkId(), Op.EQ);
        allocationStateSearch.and("providerName", allocationStateSearch.entity().getProviderName(), Op.EQ);
        allocationStateSearch.and("allocationState", allocationStateSearch.entity().getAllocationState(), Op.EQ);
        allocationStateSearch.done();

        deviceStatusSearch = createSearchBuilder();
        deviceStatusSearch.and("physicalNetworkId", deviceStatusSearch.entity().getPhysicalNetworkId(), Op.EQ);
        deviceStatusSearch.and("providerName", deviceStatusSearch.entity().getProviderName(), Op.EQ);
        deviceStatusSearch.and("deviceState", deviceStatusSearch.entity().getDeviceState(), Op.EQ);
        deviceStatusSearch.done();
    }

	@Override
	public List<ExternalFirewallDeviceVO> listByPhysicalNetwork(long physicalNetworkId) {
        SearchCriteria<ExternalFirewallDeviceVO> sc = physicalNetworkIdSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);
        return search(sc, null);
	}

	@Override
	public List<ExternalFirewallDeviceVO> listByPhysicalNetworkAndProvider(long physicalNetworkId, String providerName) {
        SearchCriteria<ExternalFirewallDeviceVO> sc = physicalNetworkServiceProviderSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);
        sc.setParameters("networkServiceProviderName", providerName);
        return search(sc, null);
	}

    @Override
    public List<ExternalFirewallDeviceVO> listByProviderAndDeviceAllocationState(long physicalNetworkId, String providerName, FirewallDeviceAllocationState allocationState) {
        SearchCriteria<ExternalFirewallDeviceVO> sc = allocationStateSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);
        sc.setParameters("providerName", providerName);
        sc.setParameters("allocationState", allocationState);
        return search(sc, null);
    }

    @Override
    public List<ExternalFirewallDeviceVO> listByProviderAndDeviceStaus(long physicalNetworkId, String providerName, FirewallDeviceState state) {
        SearchCriteria<ExternalFirewallDeviceVO> sc = deviceStatusSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);
        sc.setParameters("providerName", providerName);
        sc.setParameters("deviceState", state);
        return search(sc, null);
    }

}
