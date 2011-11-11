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
import com.cloud.network.ExternalLoadBalancerDeviceVO;
import com.cloud.network.ExternalLoadBalancerDeviceVO.LBDeviceAllocationState;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Local(value=ExternalLoadBalancerDeviceDao.class) @DB(txn=false)
public class ExternalLoadBalancerDeviceDaoImpl extends GenericDaoBase<ExternalLoadBalancerDeviceVO, Long> implements ExternalLoadBalancerDeviceDao {
    final SearchBuilder<ExternalLoadBalancerDeviceVO> physicalNetworkServiceProviderSearch;
    final SearchBuilder<ExternalLoadBalancerDeviceVO> physicalNetworkIdSearch;
    final SearchBuilder<ExternalLoadBalancerDeviceVO> allocationStateSearch;

    public ExternalLoadBalancerDeviceDaoImpl() {
        super();
        physicalNetworkServiceProviderSearch = createSearchBuilder();
        physicalNetworkServiceProviderSearch.and("physicalNetworkId", physicalNetworkServiceProviderSearch.entity().getPhysicalNetworkId(), Op.EQ);
        physicalNetworkServiceProviderSearch.and("provider_name", physicalNetworkServiceProviderSearch.entity().getProviderName(), Op.EQ);
        physicalNetworkServiceProviderSearch.done();

        physicalNetworkIdSearch = createSearchBuilder();
        physicalNetworkIdSearch.and("physicalNetworkId", physicalNetworkIdSearch.entity().getPhysicalNetworkId(), Op.EQ);
        physicalNetworkIdSearch.done();

        allocationStateSearch = createSearchBuilder();
        allocationStateSearch.and("physicalNetworkId", allocationStateSearch.entity().getPhysicalNetworkId(), Op.EQ);
        allocationStateSearch.and("allocationState", allocationStateSearch.entity().getAllocationState(), Op.EQ);
        allocationStateSearch.done();
    }

    @Override
    public List<ExternalLoadBalancerDeviceVO> listByPhysicalNetwork(long physicalNetworkId) {
        SearchCriteria<ExternalLoadBalancerDeviceVO> sc = physicalNetworkIdSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);
        return search(sc, null);
    }

    @Override
    public List<ExternalLoadBalancerDeviceVO> listByPhysicalNetworkServiceProvider(long physicalNetworkId, String provider_name) {
        SearchCriteria<ExternalLoadBalancerDeviceVO> sc = physicalNetworkServiceProviderSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);
        sc.setParameters("provider_name", provider_name);
        return search(sc, null);
    }

    @Override
    public List<ExternalLoadBalancerDeviceVO> listByDeviceAllocationState(long physicalNetworkId, String provider_name, LBDeviceAllocationState state) {
        SearchCriteria<ExternalLoadBalancerDeviceVO> sc = allocationStateSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);
        sc.setParameters("provider_name", provider_name);
        sc.setParameters("allocationState", state);
        return search(sc, null);
    }
}
