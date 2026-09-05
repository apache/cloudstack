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
package com.cloud.simulator.dao;

import com.cloud.simulator.MockGpuDeviceVO;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MockGpuDeviceDaoImpl extends GenericDaoBase<MockGpuDeviceVO, Long> implements MockGpuDeviceDao {

    private final SearchBuilder<MockGpuDeviceVO> allFieldSearch;

    public MockGpuDeviceDaoImpl() {
        allFieldSearch = createSearchBuilder();
        allFieldSearch.and("busAddress", allFieldSearch.entity().getBusAddress(), SearchCriteria.Op.EQ);
        allFieldSearch.and("hostId", allFieldSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        allFieldSearch.and("vendorName", allFieldSearch.entity().getVendorName(), SearchCriteria.Op.EQ);
        allFieldSearch.and("vendorId", allFieldSearch.entity().getVendorId(), SearchCriteria.Op.EQ);
        allFieldSearch.and("deviceId", allFieldSearch.entity().getDeviceId(), SearchCriteria.Op.EQ);
        allFieldSearch.and("deviceName", allFieldSearch.entity().getDeviceName(), SearchCriteria.Op.EQ);
        allFieldSearch.and("state", allFieldSearch.entity().getState(), SearchCriteria.Op.EQ);
        allFieldSearch.and("vmId", allFieldSearch.entity().getVmId(), SearchCriteria.Op.EQ);
        allFieldSearch.done();
    }

    @Override
    public MockGpuDeviceVO listByHostIdAndBusAddress(long hostId, String busAddress) {
        SearchCriteria<MockGpuDeviceVO> sc = allFieldSearch.create();
        sc.setParameters("hostId", hostId);
        sc.setParameters("busAddress", busAddress);
        return findOneBy(sc);
    }

    @Override
    public List<MockGpuDeviceVO> listByHostId(Long hostId) {
        SearchCriteria<MockGpuDeviceVO> sc = allFieldSearch.create();
        sc.setParameters("hostId", hostId);
        return search(sc, new Filter(MockGpuDeviceVO.class, "id", true));
    }

    @Override
    public List<MockGpuDeviceVO> listByVmId(Long vmId) {
        SearchCriteria<MockGpuDeviceVO> sc = allFieldSearch.create();
        sc.setParameters("vmId", vmId);
        return search(sc, null);
    }
}
