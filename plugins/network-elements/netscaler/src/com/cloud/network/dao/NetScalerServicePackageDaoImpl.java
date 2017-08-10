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

import org.springframework.stereotype.Component;

import com.cloud.network.NetScalerServicePackageVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
@DB
public class NetScalerServicePackageDaoImpl extends GenericDaoBase<NetScalerServicePackageVO, Long> implements NetScalerServicePackageDao {

    final SearchBuilder<NetScalerServicePackageVO> podIdSearch;
    final SearchBuilder<NetScalerServicePackageVO> deviceIdSearch;

    protected NetScalerServicePackageDaoImpl() {
        super();

        podIdSearch = createSearchBuilder();
        podIdSearch.done();

        deviceIdSearch = createSearchBuilder();
        deviceIdSearch.done();
    }

    @Override
    public NetScalerServicePackageVO findByPodId(long podId) {
        SearchCriteria<NetScalerServicePackageVO> sc = podIdSearch.create();
        sc.setParameters("pod_id", podId);
        return findOneBy(sc);
    }

    @Override
    public List<NetScalerServicePackageVO> listByNetScalerDeviceId(long netscalerDeviceId) {
        SearchCriteria<NetScalerServicePackageVO> sc = deviceIdSearch.create();
        sc.setParameters("netscalerDeviceId", netscalerDeviceId);
        return search(sc, null);
    }

    @Override
    public void removeAll() {
        List<NetScalerServicePackageVO> list_NetScalerServicePackageVO = this.listAll();
        for (NetScalerServicePackageVO row : list_NetScalerServicePackageVO) {
            this.remove(row.getId());
        }
    }
}
