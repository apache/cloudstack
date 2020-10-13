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
package com.cloud.network.as.dao;

import java.util.List;


import org.springframework.stereotype.Component;

import com.cloud.network.as.AutoScaleVmGroupVmMapVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchCriteria;

@Component
public class AutoScaleVmGroupVmMapDaoImpl extends GenericDaoBase<AutoScaleVmGroupVmMapVO, Long> implements AutoScaleVmGroupVmMapDao {

    @Override
    public Integer countByGroup(long vmGroupId) {

        SearchCriteria<AutoScaleVmGroupVmMapVO> sc = createSearchCriteria();
        sc.addAnd("vmGroupId", SearchCriteria.Op.EQ, vmGroupId);
        return getCountIncludingRemoved(sc);
    }

    @Override
    public List<AutoScaleVmGroupVmMapVO> listByGroup(long vmGroupId) {
        SearchCriteria<AutoScaleVmGroupVmMapVO> sc = createSearchCriteria();
        sc.addAnd("vmGroupId", SearchCriteria.Op.EQ, vmGroupId);
        return listBy(sc);
    }

    @Override
    public int remove(long vmGroupId, long vmId) {
        SearchCriteria<AutoScaleVmGroupVmMapVO> sc = createSearchCriteria();
        sc.addAnd("vmGroupId", SearchCriteria.Op.EQ, vmGroupId);
        sc.addAnd("instanceId", SearchCriteria.Op.EQ, vmId);
        return remove(sc);
    }

}
