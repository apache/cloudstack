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

package org.apache.cloudstack.backup.dao;

import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.cloudstack.backup.VmCheckpointVO;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class VmCheckpointDaoImpl extends GenericDaoBase<VmCheckpointVO, Long> implements VmCheckpointDao {
    private SearchBuilder<VmCheckpointVO> vmIdSearch;
    private SearchBuilder<VmCheckpointVO> vmIdCheckpointIdSearch;

    @PostConstruct
    protected void init() {
        vmIdSearch = createSearchBuilder();
        vmIdSearch.and("vmId", vmIdSearch.entity().getVmId(), SearchCriteria.Op.EQ);
        vmIdSearch.done();

        vmIdCheckpointIdSearch = createSearchBuilder();
        vmIdCheckpointIdSearch.and("vmId", vmIdCheckpointIdSearch.entity().getVmId(), SearchCriteria.Op.EQ);
        vmIdCheckpointIdSearch.and("checkpointId", vmIdCheckpointIdSearch.entity().getCheckpointId(), SearchCriteria.Op.EQ);
        vmIdCheckpointIdSearch.done();
    }

    @Override
    public List<VmCheckpointVO> listByVmId(long vmId) {
        SearchCriteria<VmCheckpointVO> sc = vmIdSearch.create();
        sc.setParameters("vmId", vmId);
        Filter oldestFirst = new Filter(VmCheckpointVO.class, "createTime", true, null, null);
        oldestFirst.addOrderBy(VmCheckpointVO.class, "id", true);
        return listBy(sc, oldestFirst);
    }

    @Override
    public VmCheckpointVO findByVmIdAndCheckpointId(long vmId, String checkpointId) {
        SearchCriteria<VmCheckpointVO> sc = vmIdCheckpointIdSearch.create();
        sc.setParameters("vmId", vmId);
        sc.setParameters("checkpointId", checkpointId);
        return findOneBy(sc);
    }
}
