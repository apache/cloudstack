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
import com.cloud.network.ExternalLoadBalancerDeviceVO.LBDeviceState;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Local(value=ExternalLoadBalancerDeviceDao.class) @DB(txn=false)
public class ExternalLoadBalancerDeviceDaoImpl extends GenericDaoBase<ExternalLoadBalancerDeviceVO, Long> implements ExternalLoadBalancerDeviceDao {
    final SearchBuilder<ExternalLoadBalancerDeviceVO> physicalNetworkIdSearch;
    final SearchBuilder<ExternalLoadBalancerDeviceVO> physicalNetworkServiceProviderSearch;
    final SearchBuilder<ExternalLoadBalancerDeviceVO> allocationStateSearch;
    final SearchBuilder<ExternalLoadBalancerDeviceVO> deviceStatusSearch;
    final SearchBuilder<ExternalLoadBalancerDeviceVO> deviceManagedTypeSearch;

    public ExternalLoadBalancerDeviceDaoImpl() {
        super();

        physicalNetworkIdSearch = createSearchBuilder();
        physicalNetworkIdSearch.and("physicalNetworkId", physicalNetworkIdSearch.entity().getPhysicalNetworkId(), Op.EQ);
        physicalNetworkIdSearch.done();

        physicalNetworkServiceProviderSearch = createSearchBuilder();
        physicalNetworkServiceProviderSearch.and("physicalNetworkId", physicalNetworkServiceProviderSearch.entity().getPhysicalNetworkId(), Op.EQ);
        physicalNetworkServiceProviderSearch.and("providerName", physicalNetworkServiceProviderSearch.entity().getProviderName(), Op.EQ);
        physicalNetworkServiceProviderSearch.done();

        allocationStateSearch = createSearchBuilder();
        allocationStateSearch.and("physicalNetworkId", allocationStateSearch.entity().getPhysicalNetworkId(), Op.EQ);
        allocationStateSearch.and("providerName", allocationStateSearch.entity().getProviderName(), Op.EQ);
        allocationStateSearch.and("allocationState", allocationStateSearch.entity().getAllocationState(), Op.EQ);
        allocationStateSearch.done();

        deviceStatusSearch = createSearchBuilder();
        deviceStatusSearch.and("physicalNetworkId", deviceStatusSearch.entity().getPhysicalNetworkId(), Op.EQ);
        deviceStatusSearch.and("providerName", deviceStatusSearch.entity().getProviderName(), Op.EQ);
        deviceStatusSearch.and("deviceState", deviceStatusSearch.entity().getState(), Op.EQ);
        deviceStatusSearch.done();

        deviceManagedTypeSearch = createSearchBuilder();
        deviceManagedTypeSearch.and("physicalNetworkId", deviceManagedTypeSearch.entity().getPhysicalNetworkId(), Op.EQ);
        deviceManagedTypeSearch.and("providerName", deviceManagedTypeSearch.entity().getProviderName(), Op.EQ);
        deviceManagedTypeSearch.and("managedType", deviceManagedTypeSearch.entity().getIsManagedDevice(), Op.EQ);
        deviceManagedTypeSearch.done();
    }

    public List<ExternalLoadBalancerDeviceVO> listByPhysicalNetwork(long physicalNetworkId) {
        SearchCriteria<ExternalLoadBalancerDeviceVO> sc = physicalNetworkIdSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);
        return search(sc, null);
    }

    @Override
    public List<ExternalLoadBalancerDeviceVO> listByPhysicalNetworkAndProvider(long physicalNetworkId, String provider_name) {
        SearchCriteria<ExternalLoadBalancerDeviceVO> sc = physicalNetworkServiceProviderSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);
        sc.setParameters("providerName", provider_name);
        return search(sc, null);
    }

    @Override
    public List<ExternalLoadBalancerDeviceVO> listByProviderAndDeviceAllocationState(long physicalNetworkId, String provider_name, LBDeviceAllocationState state) {
        SearchCriteria<ExternalLoadBalancerDeviceVO> sc = allocationStateSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);
        sc.setParameters("providerName", provider_name);
        sc.setParameters("allocationState", state);
        return search(sc, null);
    }

    @Override
    public List<ExternalLoadBalancerDeviceVO> listByProviderAndDeviceStaus(long physicalNetworkId, String providerName, LBDeviceState state) {
        SearchCriteria<ExternalLoadBalancerDeviceVO> sc = deviceStatusSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);
        sc.setParameters("providerName", providerName);
        sc.setParameters("deviceState", state);
        return search(sc, null);
    }

    @Override
    public List<ExternalLoadBalancerDeviceVO> listByProviderAndManagedType(long physicalNetworkId, String providerName, boolean managed) {
        SearchCriteria<ExternalLoadBalancerDeviceVO> sc = deviceManagedTypeSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);
        sc.setParameters("providerName", providerName);
        sc.setParameters("managedType", managed);
        return search(sc, null);
    }
}
