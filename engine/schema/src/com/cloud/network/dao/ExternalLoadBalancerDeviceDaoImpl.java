// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.network.dao;

import com.cloud.network.dao.ExternalLoadBalancerDeviceVO.LBDeviceAllocationState;
import com.cloud.network.dao.ExternalLoadBalancerDeviceVO.LBDeviceState;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import org.springframework.stereotype.Component;

import javax.ejb.Local;
import java.util.List;

@Component
@Local(value=ExternalLoadBalancerDeviceDao.class) @DB
public class ExternalLoadBalancerDeviceDaoImpl extends GenericDaoBase<ExternalLoadBalancerDeviceVO, Long> implements ExternalLoadBalancerDeviceDao {
    final SearchBuilder<ExternalLoadBalancerDeviceVO> physicalNetworkIdSearch;
    final SearchBuilder<ExternalLoadBalancerDeviceVO> physicalNetworkServiceProviderSearch;
    final SearchBuilder<ExternalLoadBalancerDeviceVO> allocationStateSearch;
    final SearchBuilder<ExternalLoadBalancerDeviceVO> deviceStatusSearch;
    final SearchBuilder<ExternalLoadBalancerDeviceVO> deviceManagedTypeSearch;
    final SearchBuilder<ExternalLoadBalancerDeviceVO> gslbProviderSearch;

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

        gslbProviderSearch = createSearchBuilder();
        gslbProviderSearch.and("physicalNetworkId", gslbProviderSearch.entity().getPhysicalNetworkId(), Op.EQ);
        gslbProviderSearch.and("providerName", gslbProviderSearch.entity().getProviderName(), Op.EQ);
        gslbProviderSearch.and("gslbProvider", gslbProviderSearch.entity().getGslbProvider(), Op.EQ);

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

    @Override
    public ExternalLoadBalancerDeviceVO findGslbServiceProvider(long physicalNetworkId, String providerName) {
        SearchCriteria<ExternalLoadBalancerDeviceVO> sc = gslbProviderSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);
        sc.setParameters("providerName", providerName);
        sc.setParameters("gslbProvider", true);
        return findOneBy(sc);
    }
}
