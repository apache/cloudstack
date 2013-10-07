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

import java.util.List;
import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.network.dao.ExternalFirewallDeviceVO.FirewallDeviceAllocationState;
import com.cloud.network.dao.ExternalFirewallDeviceVO.FirewallDeviceState;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Component
@Local(value=ExternalFirewallDeviceDao.class) @DB
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
