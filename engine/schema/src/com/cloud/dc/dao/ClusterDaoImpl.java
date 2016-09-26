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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterDetailsVO;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.org.Grouping;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
public class ClusterDaoImpl extends GenericDaoBase<ClusterVO, Long> implements ClusterDao {

    protected final SearchBuilder<ClusterVO> PodSearch;
    protected final SearchBuilder<ClusterVO> HyTypeWithoutGuidSearch;
    protected final SearchBuilder<ClusterVO> AvailHyperSearch;
    protected final SearchBuilder<ClusterVO> ZoneSearch;
    protected final SearchBuilder<ClusterVO> ZoneHyTypeSearch;
    protected final SearchBuilder<ClusterVO> ZoneClusterSearch;

    protected GenericSearchBuilder<ClusterVO, Long> ClusterIdSearch;

    private static final String GET_POD_CLUSTER_MAP_PREFIX = "SELECT pod_id, id FROM cloud.cluster WHERE cluster.id IN( ";
    private static final String GET_POD_CLUSTER_MAP_SUFFIX = " )";
    @Inject
    private ClusterDetailsDao clusterDetailsDao;
    @Inject
    protected HostPodDao hostPodDao;

    public ClusterDaoImpl() {
        super();

        HyTypeWithoutGuidSearch = createSearchBuilder();
        HyTypeWithoutGuidSearch.and("hypervisorType", HyTypeWithoutGuidSearch.entity().getHypervisorType(), SearchCriteria.Op.EQ);
        HyTypeWithoutGuidSearch.and("guid", HyTypeWithoutGuidSearch.entity().getGuid(), SearchCriteria.Op.NULL);
        HyTypeWithoutGuidSearch.done();

        ZoneHyTypeSearch = createSearchBuilder();
        ZoneHyTypeSearch.and("hypervisorType", ZoneHyTypeSearch.entity().getHypervisorType(), SearchCriteria.Op.EQ);
        ZoneHyTypeSearch.and("dataCenterId", ZoneHyTypeSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        ZoneHyTypeSearch.done();

        PodSearch = createSearchBuilder();
        PodSearch.and("pod", PodSearch.entity().getPodId(), SearchCriteria.Op.EQ);
        PodSearch.and("name", PodSearch.entity().getName(), SearchCriteria.Op.EQ);
        PodSearch.done();

        ZoneSearch = createSearchBuilder();
        ZoneSearch.and("dataCenterId", ZoneSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        ZoneSearch.groupBy(ZoneSearch.entity().getHypervisorType());
        ZoneSearch.done();

        AvailHyperSearch = createSearchBuilder();
        AvailHyperSearch.and("zoneId", AvailHyperSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        AvailHyperSearch.select(null, Func.DISTINCT, AvailHyperSearch.entity().getHypervisorType());
        AvailHyperSearch.done();

        ZoneClusterSearch = createSearchBuilder();
        ZoneClusterSearch.and("dataCenterId", ZoneClusterSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        ZoneClusterSearch.done();

        ClusterIdSearch = createSearchBuilder(Long.class);
        ClusterIdSearch.selectFields(ClusterIdSearch.entity().getId());
        ClusterIdSearch.and("dataCenterId", ClusterIdSearch.entity().getDataCenterId(), Op.EQ);
        ClusterIdSearch.done();
    }

    @Override
    public List<ClusterVO> listByZoneId(long zoneId) {
        SearchCriteria<ClusterVO> sc = ZoneSearch.create();
        sc.setParameters("dataCenterId", zoneId);
        return listBy(sc);
    }

    @Override
    public List<ClusterVO> listByPodId(long podId) {
        SearchCriteria<ClusterVO> sc = PodSearch.create();
        sc.setParameters("pod", podId);

        return listBy(sc);
    }

    @Override
    public ClusterVO findBy(String name, long podId) {
        SearchCriteria<ClusterVO> sc = PodSearch.create();
        sc.setParameters("pod", podId);
        sc.setParameters("name", name);

        return findOneBy(sc);
    }

    @Override
    public List<ClusterVO> listByHyTypeWithoutGuid(String hyType) {
        SearchCriteria<ClusterVO> sc = HyTypeWithoutGuidSearch.create();
        sc.setParameters("hypervisorType", hyType);

        return listBy(sc);
    }

    @Override
    public List<ClusterVO> listByDcHyType(long dcId, String hyType) {
        SearchCriteria<ClusterVO> sc = ZoneHyTypeSearch.create();
        sc.setParameters("dataCenterId", dcId);
        sc.setParameters("hypervisorType", hyType);
        return listBy(sc);
    }

    @Override
    public List<HypervisorType> getAvailableHypervisorInZone(Long zoneId) {
        SearchCriteria<ClusterVO> sc = AvailHyperSearch.create();
        if (zoneId != null) {
            sc.setParameters("zoneId", zoneId);
        }
        List<ClusterVO> clusters = listBy(sc);
        List<HypervisorType> hypers = new ArrayList<HypervisorType>(4);
        for (ClusterVO cluster : clusters) {
            hypers.add(cluster.getHypervisorType());
        }

        return hypers;
    }

    @Override
    public Map<Long, List<Long>> getPodClusterIdMap(List<Long> clusterIds) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        PreparedStatement pstmt = null;
        Map<Long, List<Long>> result = new HashMap<Long, List<Long>>();

        try {
            StringBuilder sql = new StringBuilder(GET_POD_CLUSTER_MAP_PREFIX);
            if (clusterIds.size() > 0) {
                for (Long clusterId : clusterIds) {
                    sql.append(clusterId).append(",");
                }
                sql.delete(sql.length() - 1, sql.length());
                sql.append(GET_POD_CLUSTER_MAP_SUFFIX);
            }

            pstmt = txn.prepareAutoCloseStatement(sql.toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Long podId = rs.getLong(1);
                Long clusterIdInPod = rs.getLong(2);
                if (result.containsKey(podId)) {
                    List<Long> clusterList = result.get(podId);
                    clusterList.add(clusterIdInPod);
                    result.put(podId, clusterList);
                } else {
                    List<Long> clusterList = new ArrayList<Long>();
                    clusterList.add(clusterIdInPod);
                    result.put(podId, clusterList);
                }
            }
            return result;
        } catch (SQLException e) {
            throw new CloudRuntimeException("DB Exception on: " + GET_POD_CLUSTER_MAP_PREFIX, e);
        } catch (Throwable e) {
            throw new CloudRuntimeException("Caught: " + GET_POD_CLUSTER_MAP_PREFIX, e);
        }
    }

    @Override
    public List<Long> listDisabledClusters(long zoneId, Long podId) {
        GenericSearchBuilder<ClusterVO, Long> clusterIdSearch = createSearchBuilder(Long.class);
        clusterIdSearch.selectFields(clusterIdSearch.entity().getId());
        clusterIdSearch.and("dataCenterId", clusterIdSearch.entity().getDataCenterId(), Op.EQ);
        if (podId != null) {
            clusterIdSearch.and("podId", clusterIdSearch.entity().getPodId(), Op.EQ);
        }
        clusterIdSearch.and("allocationState", clusterIdSearch.entity().getAllocationState(), Op.EQ);
        clusterIdSearch.done();

        SearchCriteria<Long> sc = clusterIdSearch.create();
        sc.addAnd("dataCenterId", SearchCriteria.Op.EQ, zoneId);
        if (podId != null) {
            sc.addAnd("podId", SearchCriteria.Op.EQ, podId);
        }
        sc.addAnd("allocationState", SearchCriteria.Op.EQ, Grouping.AllocationState.Disabled);
        return customSearch(sc, null);
    }

    @Override
    public List<Long> listClustersWithDisabledPods(long zoneId) {

        GenericSearchBuilder<HostPodVO, Long> disabledPodIdSearch = hostPodDao.createSearchBuilder(Long.class);
        disabledPodIdSearch.selectFields(disabledPodIdSearch.entity().getId());
        disabledPodIdSearch.and("dataCenterId", disabledPodIdSearch.entity().getDataCenterId(), Op.EQ);
        disabledPodIdSearch.and("allocationState", disabledPodIdSearch.entity().getAllocationState(), Op.EQ);

        GenericSearchBuilder<ClusterVO, Long> clusterIdSearch = createSearchBuilder(Long.class);
        clusterIdSearch.selectFields(clusterIdSearch.entity().getId());
        clusterIdSearch.join("disabledPodIdSearch", disabledPodIdSearch, clusterIdSearch.entity().getPodId(), disabledPodIdSearch.entity().getId(),
            JoinBuilder.JoinType.INNER);
        clusterIdSearch.done();

        SearchCriteria<Long> sc = clusterIdSearch.create();
        sc.setJoinParameters("disabledPodIdSearch", "dataCenterId", zoneId);
        sc.setJoinParameters("disabledPodIdSearch", "allocationState", Grouping.AllocationState.Disabled);

        return customSearch(sc, null);
    }

    @Override
    public List<ClusterVO> listClustersByDcId(long zoneId) {
        SearchCriteria<ClusterVO> sc = ZoneClusterSearch.create();
        sc.setParameters("dataCenterId", zoneId);
        return listBy(sc);
    }

    @Override
    public boolean remove(Long id) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        ClusterVO cluster = createForUpdate();
        cluster.setName(null);
        cluster.setGuid(null);

        update(id, cluster);

        boolean result = super.remove(id);
        txn.commit();
        return result;
    }

    @Override
    public List<Long> listAllCusters(long zoneId) {
        SearchCriteria<Long> sc = ClusterIdSearch.create();
        sc.setParameters("dataCenterId", zoneId);
        return customSearch(sc, null);
    }

    @Override
    public boolean getSupportsResigning(long clusterId) {
        ClusterVO cluster = findById(clusterId);

        if (cluster == null || cluster.getAllocationState() != Grouping.AllocationState.Enabled) {
            return false;
        }

        ClusterDetailsVO clusterDetailsVO = clusterDetailsDao.findDetail(clusterId, "supportsResign");

        if (clusterDetailsVO != null) {
            String value = clusterDetailsVO.getValue();

            return Boolean.parseBoolean(value);
        }

        return false;
    }
}
