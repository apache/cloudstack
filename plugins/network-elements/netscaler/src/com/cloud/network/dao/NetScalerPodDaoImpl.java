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

import com.cloud.network.NetScalerPodVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Component
@Local(value=NetScalerPodDao.class) @DB
public class NetScalerPodDaoImpl extends GenericDaoBase<NetScalerPodVO, Long> implements NetScalerPodDao {

    final SearchBuilder<NetScalerPodVO> podIdSearch;
    final SearchBuilder<NetScalerPodVO> deviceIdSearch;

    protected NetScalerPodDaoImpl() {
        super();

        podIdSearch = createSearchBuilder();
        podIdSearch.and("pod_id", podIdSearch.entity().getPodId(), Op.EQ);
        podIdSearch.done();

        deviceIdSearch = createSearchBuilder();
        deviceIdSearch.and("netscalerDeviceId", deviceIdSearch.entity().getNetscalerDeviceId(), Op.EQ);
        deviceIdSearch.done();
    }

    @Override
    public NetScalerPodVO findByPodId(long podId) {
        SearchCriteria<NetScalerPodVO> sc = podIdSearch.create();
        sc.setParameters("pod_id", podId);
        return findOneBy(sc);
    }

    @Override
    public List<NetScalerPodVO> listByNetScalerDeviceId(long netscalerDeviceId) {
        SearchCriteria<NetScalerPodVO> sc = deviceIdSearch.create();
        sc.setParameters("netscalerDeviceId", netscalerDeviceId);
        return search(sc, null);
    }

}
