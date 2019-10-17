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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.springframework.stereotype.Component;

import com.cloud.simulator.MockHostVO;
import com.cloud.simulator.MockVMVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.VirtualMachine;

@Component
public class MockVMDaoImpl extends GenericDaoBase<MockVMVO, Long> implements MockVMDao {
    protected SearchBuilder<MockVMVO> GuidSearch;
    protected SearchBuilder<MockVMVO> vmNameSearch;
    protected SearchBuilder<MockVMVO> vmhostSearch;
    @Inject
    MockHostDao _mockHostDao;

    @Override
    public List<MockVMVO> findByHostId(long hostId) {
        return new ArrayList<MockVMVO>();
    }

    @Override
    public MockVMVO findByVmName(String vmName) {
        SearchCriteria<MockVMVO> sc = vmNameSearch.create();
        sc.setParameters("name", vmName);
        return findOneBy(sc);
    }

    @Override
    public List<MockVMVO> findByHostGuid(String guid) {
        SearchCriteria<MockVMVO> sc = GuidSearch.create();
        sc.setJoinParameters("host", "guid", guid);
        sc.setParameters("power_state", VirtualMachine.PowerState.PowerOn);
        return listBy(sc);
    }

    @Override
    public MockVMVO findByVmNameAndHost(String vmName, String hostGuid) {
        SearchCriteria<MockVMVO> sc = vmhostSearch.create();
        sc.setJoinParameters("host", "guid", hostGuid);
        sc.setParameters("name", vmName);
        return findOneBy(sc);
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        SearchBuilder<MockHostVO> host = _mockHostDao.createSearchBuilder();
        host.and("guid", host.entity().getGuid(), SearchCriteria.Op.EQ);

        GuidSearch = createSearchBuilder();
        GuidSearch.join("host", host, host.entity().getId(), GuidSearch.entity().getHostId(), JoinBuilder.JoinType.INNER);
        GuidSearch.and("power_state", GuidSearch.entity().getPowerState(), SearchCriteria.Op.EQ);
        GuidSearch.done();

        vmNameSearch = createSearchBuilder();
        vmNameSearch.and("name", vmNameSearch.entity().getName(), SearchCriteria.Op.EQ);
        vmNameSearch.done();

        SearchBuilder<MockHostVO> newhost = _mockHostDao.createSearchBuilder();
        newhost.and("guid", newhost.entity().getGuid(), SearchCriteria.Op.EQ);
        vmhostSearch = createSearchBuilder();
        vmhostSearch.and("name", vmhostSearch.entity().getName(), SearchCriteria.Op.EQ);
        vmhostSearch.join("host", newhost, newhost.entity().getId(), vmhostSearch.entity().getHostId(), JoinBuilder.JoinType.INNER);
        vmhostSearch.done();

        return true;
    }
}
