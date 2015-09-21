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
package com.cloud.dc;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import javax.ejb.Local;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.TransactionLegacy;

@Local(value = ClusterDetailsDao.class)
public class ClusterPhysicalNetworkTrafficInfoDaoImpl extends GenericDaoBase<ClusterPhysicalNetworkTrafficInfoVO, Long> implements ClusterPhysicalNetworkTrafficInfoDao {
    protected final SearchBuilder<ClusterPhysicalNetworkTrafficInfoVO> ClusterSearch;
    protected final SearchBuilder<ClusterPhysicalNetworkTrafficInfoVO> clusterPhysicalNetworkTrafficSearch;

    protected ClusterPhysicalNetworkTrafficInfoDaoImpl() {
        super();
        ClusterSearch = createSearchBuilder();
        ClusterSearch.and("clusterId", ClusterSearch.entity().getClusterId(), SearchCriteria.Op.EQ);
        ClusterSearch.done();

        clusterPhysicalNetworkTrafficSearch = createSearchBuilder();
        clusterPhysicalNetworkTrafficSearch.and("clusterId", clusterPhysicalNetworkTrafficSearch.entity().getClusterId(), SearchCriteria.Op.EQ);
        clusterPhysicalNetworkTrafficSearch.and("physicalNetworkTrafficId", clusterPhysicalNetworkTrafficSearch.entity().getPhysicalNetworkTrafficId(), Op.EQ);
        clusterPhysicalNetworkTrafficSearch.done();
    }

    @Override
    public ClusterPhysicalNetworkTrafficInfoVO findDetail(long clusterId, long physicalNetworkTrafficId) {
        SearchCriteria<ClusterPhysicalNetworkTrafficInfoVO> sc = clusterPhysicalNetworkTrafficSearch.create();
        sc.setParameters("clusterId", clusterId);
        sc.setParameters("physicalNetworkTrafficId", physicalNetworkTrafficId);

        ClusterPhysicalNetworkTrafficInfoVO detail = findOneBy(sc);
        return detail;
    }

    @Override
    public void persist(long clusterId, long physicalNetworkTrafficId, String vmwareNetworkLabel) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        SearchCriteria<ClusterPhysicalNetworkTrafficInfoVO> sc = clusterPhysicalNetworkTrafficSearch.create();
        sc.setParameters("clusterId", clusterId);
        sc.setParameters("physicalNetworkTrafficId", physicalNetworkTrafficId);
        expunge(sc);

        ClusterPhysicalNetworkTrafficInfoVO vo = new ClusterPhysicalNetworkTrafficInfoVO(clusterId, physicalNetworkTrafficId, vmwareNetworkLabel);
        persist(vo);
        txn.commit();
    }

    @Override
    public void persist(long clusterId, Map<Long, String> physicalNetworkTrafficLabels) {
        if(physicalNetworkTrafficLabels == null || physicalNetworkTrafficLabels.isEmpty()) {
            return;
        }

        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        SearchCriteria<ClusterPhysicalNetworkTrafficInfoVO> sc = clusterPhysicalNetworkTrafficSearch.create();
        sc.setParameters("clusterId", clusterId);
        for (Map.Entry<Long, String> detail : physicalNetworkTrafficLabels.entrySet()) {
            sc.setParameters("physicalNetworkTrafficId", detail.getKey());
            expunge(sc);
        }

        for (Map.Entry<Long, String> detail : physicalNetworkTrafficLabels.entrySet()) {
            ClusterPhysicalNetworkTrafficInfoVO vo = new ClusterPhysicalNetworkTrafficInfoVO(clusterId, detail.getKey(), detail.getValue());
            persist(vo);
        }
        txn.commit();
    }

    @Override
    public String getVmwareNetworkLabel(long clusterId, long physicalNetworkTrafficId) {
        ClusterPhysicalNetworkTrafficInfoVO trafficInfo = findDetail(clusterId, physicalNetworkTrafficId);
        if (trafficInfo == null) {
            return null;
        }

        return trafficInfo.getVmwareNetworkLabel();
    }

    @Override
    public Map<Long, String> getTrafficInfo(long clusterId) {
        SearchCriteria<ClusterPhysicalNetworkTrafficInfoVO> sc = ClusterSearch.create();
        sc.setParameters("clusterId", clusterId);
        List<ClusterPhysicalNetworkTrafficInfoVO> trafficInfoList= listBy(sc);

        if (trafficInfoList == null || trafficInfoList.size() == 0) {
            return null;
        }

        Map<Long, String> trafficInfoMap = new HashMap<Long, String>();
        for (ClusterPhysicalNetworkTrafficInfoVO trafficInfo : trafficInfoList) {
            trafficInfoMap.put(new Long(trafficInfo.getPhysicalNetworkTrafficId()), trafficInfo.getVmwareNetworkLabel());
        }

        return trafficInfoMap;
    }

    @Override
    public void deleteDetails(long clusterId) {
        SearchCriteria<ClusterPhysicalNetworkTrafficInfoVO> sc = ClusterSearch.create();
        sc.setParameters("clusterId", clusterId);

        List<ClusterPhysicalNetworkTrafficInfoVO> results = search(sc, null);
        for (ClusterPhysicalNetworkTrafficInfoVO result : results) {
            remove(result.getId());
        }
    }
}
