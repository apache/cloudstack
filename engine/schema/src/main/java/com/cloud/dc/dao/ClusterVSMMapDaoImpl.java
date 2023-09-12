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
package com.cloud.dc.dao;

import java.util.List;


import org.springframework.stereotype.Component;

import com.cloud.dc.ClusterVSMMapVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;

@Component
@DB
public class ClusterVSMMapDaoImpl extends GenericDaoBase<ClusterVSMMapVO, Long> implements ClusterVSMMapDao {

    final SearchBuilder<ClusterVSMMapVO> ClusterSearch;
    final SearchBuilder<ClusterVSMMapVO> VsmSearch;

    public ClusterVSMMapDaoImpl() {
        //super();

        ClusterSearch = createSearchBuilder();
        ClusterSearch.and("clusterId", ClusterSearch.entity().getClusterId(), SearchCriteria.Op.EQ);
        ClusterSearch.done();

        VsmSearch = createSearchBuilder();
        VsmSearch.and("vsmId", VsmSearch.entity().getVsmId(), SearchCriteria.Op.EQ);
        VsmSearch.done();
    }

    @Override
    public boolean removeByVsmId(long vsmId) {
        SearchCriteria<ClusterVSMMapVO> sc = VsmSearch.create();
        sc.setParameters("vsmId", vsmId);
        this.remove(sc);
        return true;
    }

    @Override
    public boolean removeByClusterId(long clusterId) {
        SearchCriteria<ClusterVSMMapVO> sc = ClusterSearch.create();
        sc.setParameters("clusterId", clusterId);
        this.remove(sc);
        return true;
    }

    @Override
    public ClusterVSMMapVO findByClusterId(long clusterId) {
        SearchCriteria<ClusterVSMMapVO> sc = ClusterSearch.create();
        sc.setParameters("clusterId", clusterId);
        return findOneBy(sc);
    }

    @Override
    public List<ClusterVSMMapVO> listByVSMId(long vsmId) {
        SearchCriteria<ClusterVSMMapVO> sc = VsmSearch.create();
        sc.setParameters("vsmId", vsmId);
        return listBy(sc);
    }

    @Override
    public boolean remove(Long id) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        ClusterVSMMapVO cluster = createForUpdate();
        //cluster.setClusterId(null);
        //cluster.setVsmId(null);

        update(id, cluster);

        boolean result = super.remove(id);
        txn.commit();
        return result;
    }

}
