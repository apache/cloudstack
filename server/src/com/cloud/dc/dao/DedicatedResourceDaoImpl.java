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

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.dc.DedicatedResourceVO;
import com.cloud.utils.Pair;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;

@Component
@Local(value={DedicatedResourceDao.class}) @DB
public class DedicatedResourceDaoImpl extends GenericDaoBase<DedicatedResourceVO, Long> implements DedicatedResourceDao {
    protected final SearchBuilder<DedicatedResourceVO> ZoneSearch;
    protected final SearchBuilder<DedicatedResourceVO> PodSearch;
    protected final SearchBuilder<DedicatedResourceVO> ClusterSearch;
    protected final SearchBuilder<DedicatedResourceVO> HostSearch;

    protected SearchBuilder<DedicatedResourceVO> ListZonesByDomainIdSearch;
    protected SearchBuilder<DedicatedResourceVO> ListPodsByDomainIdSearch;
    protected SearchBuilder<DedicatedResourceVO> ListClustersByDomainIdSearch;
    protected SearchBuilder<DedicatedResourceVO> ListHostsByDomainIdSearch;

    protected SearchBuilder<DedicatedResourceVO> ListZonesByAccountIdSearch;
    protected SearchBuilder<DedicatedResourceVO> ListPodsByAccountIdSearch;
    protected SearchBuilder<DedicatedResourceVO> ListClustersByAccountIdSearch;
    protected SearchBuilder<DedicatedResourceVO> ListHostsByAccountIdSearch;

    protected SearchBuilder<DedicatedResourceVO> ListAllZonesSearch;
    protected SearchBuilder<DedicatedResourceVO> ListAllPodsSearch;
    protected SearchBuilder<DedicatedResourceVO> ListAllClustersSearch;
    protected SearchBuilder<DedicatedResourceVO> ListAllHostsSearch;

    protected SearchBuilder<DedicatedResourceVO> ListByAccountId;
    protected SearchBuilder<DedicatedResourceVO> ListByDomainId;
    protected SearchBuilder<DedicatedResourceVO> ListByAffinityGroupId;
    protected SearchBuilder<DedicatedResourceVO> ZoneByDomainIdsSearch;

    protected GenericSearchBuilder<DedicatedResourceVO, Long> ListPodsSearch;
    protected GenericSearchBuilder<DedicatedResourceVO, Long> ListClustersSearch;
    protected GenericSearchBuilder<DedicatedResourceVO, Long> ListHostsSearch;

    protected DedicatedResourceDaoImpl() {
        PodSearch = createSearchBuilder();
        PodSearch.and("podId", PodSearch.entity().getPodId(), SearchCriteria.Op.EQ);
        PodSearch.done();

        ZoneSearch = createSearchBuilder();
        ZoneSearch.and("zoneId", ZoneSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        ZoneSearch.done();

        ClusterSearch = createSearchBuilder();
        ClusterSearch.and("clusterId", ClusterSearch.entity().getClusterId(), SearchCriteria.Op.EQ);
        ClusterSearch.done();

        HostSearch = createSearchBuilder();
        HostSearch.and("hostId", HostSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        HostSearch.done();

        ListZonesByDomainIdSearch = createSearchBuilder();
        ListZonesByDomainIdSearch.and("zoneId", ListZonesByDomainIdSearch.entity().getDataCenterId(), SearchCriteria.Op.NNULL);
        ListZonesByDomainIdSearch.and("domainId", ListZonesByDomainIdSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
        ListZonesByDomainIdSearch.and("accountId", ListZonesByDomainIdSearch.entity().getAccountId(), SearchCriteria.Op.NULL);
        ListZonesByDomainIdSearch.done();

        ListZonesByAccountIdSearch = createSearchBuilder();
        ListZonesByAccountIdSearch.and("zoneId", ListZonesByAccountIdSearch.entity().getDataCenterId(), SearchCriteria.Op.NNULL);
        ListZonesByAccountIdSearch.and("accountId", ListZonesByAccountIdSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        ListZonesByAccountIdSearch.done();

        ListPodsByDomainIdSearch = createSearchBuilder();
        ListPodsByDomainIdSearch.and("podId", ListPodsByDomainIdSearch.entity().getPodId(), SearchCriteria.Op.NNULL);
        ListPodsByDomainIdSearch.and("domainId", ListPodsByDomainIdSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
        ListPodsByDomainIdSearch.and("accountId", ListPodsByDomainIdSearch.entity().getAccountId(), SearchCriteria.Op.NULL);
        ListPodsByDomainIdSearch.done();

        ListPodsByAccountIdSearch = createSearchBuilder();
        ListPodsByAccountIdSearch.and("podId", ListPodsByAccountIdSearch.entity().getPodId(), SearchCriteria.Op.NNULL);
        ListPodsByAccountIdSearch.and("accountId", ListPodsByAccountIdSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        ListPodsByAccountIdSearch.done();

        ListClustersByDomainIdSearch = createSearchBuilder();
        ListClustersByDomainIdSearch.and("clusterId", ListClustersByDomainIdSearch.entity().getClusterId(), SearchCriteria.Op.NNULL);
        ListClustersByDomainIdSearch.and("domainId", ListClustersByDomainIdSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
        ListClustersByDomainIdSearch.and("accountId", ListClustersByDomainIdSearch.entity().getAccountId(), SearchCriteria.Op.NULL);
        ListClustersByDomainIdSearch.done();

        ListClustersByAccountIdSearch = createSearchBuilder();
        ListClustersByAccountIdSearch.and("clusterId", ListClustersByAccountIdSearch.entity().getClusterId(), SearchCriteria.Op.NNULL);
        ListClustersByAccountIdSearch.and("accountId", ListClustersByAccountIdSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        ListClustersByAccountIdSearch.done();

        ListHostsByDomainIdSearch = createSearchBuilder();
        ListHostsByDomainIdSearch.and("hostId", ListHostsByDomainIdSearch.entity().getHostId(), SearchCriteria.Op.NNULL);
        ListHostsByDomainIdSearch.and("domainId", ListHostsByDomainIdSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
        ListHostsByDomainIdSearch.and("accountId", ListHostsByDomainIdSearch.entity().getAccountId(), SearchCriteria.Op.NULL);
        ListHostsByDomainIdSearch.done();

        ListHostsByAccountIdSearch = createSearchBuilder();
        ListHostsByAccountIdSearch.and("hostId", ListHostsByAccountIdSearch.entity().getHostId(), SearchCriteria.Op.NNULL);
        ListHostsByAccountIdSearch.and("accountId", ListHostsByAccountIdSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        ListHostsByAccountIdSearch.done();

        ListAllZonesSearch = createSearchBuilder();
        ListAllZonesSearch.and("zoneId", ListAllZonesSearch.entity().getDataCenterId(), Op.EQ);
        ListAllZonesSearch.and("podId", ListAllZonesSearch.entity().getPodId(), Op.NULL);
        ListAllZonesSearch.and("clusterId", ListAllZonesSearch.entity().getClusterId(), Op.NULL);
        ListAllZonesSearch.and("hostId", ListAllZonesSearch.entity().getHostId(), Op.NULL);
        ListAllZonesSearch.and("accountId", ListAllZonesSearch.entity().getAccountId(), Op.EQ);
        ListAllZonesSearch.and("domainId", ListAllZonesSearch.entity().getDomainId(), Op.EQ);
        ListAllZonesSearch.and("affinityGroupId", ListAllZonesSearch.entity().getAffinityGroupId(), Op.EQ);
        ListAllZonesSearch.done();

        ListAllPodsSearch = createSearchBuilder();
        ListAllPodsSearch.and("zoneId", ListAllPodsSearch.entity().getDataCenterId(), Op.NULL);
        ListAllPodsSearch.and("podId", ListAllPodsSearch.entity().getPodId(), Op.EQ);
        ListAllPodsSearch.and("clusterId", ListAllPodsSearch.entity().getClusterId(), Op.NULL);
        ListAllPodsSearch.and("hostId", ListAllPodsSearch.entity().getHostId(), Op.NULL);
        ListAllPodsSearch.and("accountId", ListAllPodsSearch.entity().getAccountId(), Op.EQ);
        ListAllPodsSearch.and("domainId", ListAllPodsSearch.entity().getDomainId(), Op.EQ);
        ListAllPodsSearch.and("affinityGroupId", ListAllPodsSearch.entity().getAffinityGroupId(), Op.EQ);
        ListAllPodsSearch.done();

        ListAllClustersSearch = createSearchBuilder();
        ListAllClustersSearch.and("zoneId", ListAllClustersSearch.entity().getDataCenterId(), Op.NULL);
        ListAllClustersSearch.and("podId", ListAllClustersSearch.entity().getPodId(), Op.NULL);
        ListAllClustersSearch.and("clusterId", ListAllClustersSearch.entity().getClusterId(), Op.EQ);
        ListAllClustersSearch.and("hostId", ListAllClustersSearch.entity().getHostId(), Op.NULL);
        ListAllClustersSearch.and("accountId", ListAllClustersSearch.entity().getAccountId(), Op.EQ);
        ListAllClustersSearch.and("domainId", ListAllClustersSearch.entity().getDomainId(), Op.EQ);
        ListAllClustersSearch.and("affinityGroupId", ListAllClustersSearch.entity().getAffinityGroupId(), Op.EQ);
        ListAllClustersSearch.done();

        ListAllHostsSearch = createSearchBuilder();
        ListAllHostsSearch.and("zoneId", ListAllHostsSearch.entity().getDataCenterId(), Op.NULL);
        ListAllHostsSearch.and("podId", ListAllHostsSearch.entity().getPodId(), Op.NULL);
        ListAllHostsSearch.and("clusterId", ListAllHostsSearch.entity().getClusterId(), Op.NULL);
        ListAllHostsSearch.and("hostId", ListAllHostsSearch.entity().getHostId(), Op.EQ);
        ListAllHostsSearch.and("accountId", ListAllHostsSearch.entity().getAccountId(), Op.EQ);
        ListAllHostsSearch.and("domainId", ListAllHostsSearch.entity().getDomainId(), Op.EQ);
        ListAllHostsSearch.and("affinityGroupId", ListAllHostsSearch.entity().getAffinityGroupId(), Op.EQ);
        ListAllHostsSearch.done();

        ListByAccountId = createSearchBuilder();
        ListByAccountId.and("accountId", ListByAccountId.entity().getAccountId(), SearchCriteria.Op.EQ);
        ListByAccountId.done();

        ListByDomainId = createSearchBuilder();
        ListByDomainId.and("accountId", ListByDomainId.entity().getAccountId(), SearchCriteria.Op.NULL);
        ListByDomainId.and("domainId", ListByDomainId.entity().getDomainId(), SearchCriteria.Op.EQ);
        ListByDomainId.done();

        ListByAffinityGroupId = createSearchBuilder();
        ListByAffinityGroupId.and("affinityGroupId", ListByAffinityGroupId.entity().getAffinityGroupId(),
                SearchCriteria.Op.EQ);
        ListByAffinityGroupId.done();

        ZoneByDomainIdsSearch = createSearchBuilder();
        ZoneByDomainIdsSearch.and("zoneId", ZoneByDomainIdsSearch.entity().getDataCenterId(), SearchCriteria.Op.NNULL);
        ZoneByDomainIdsSearch.and("domainId", ZoneByDomainIdsSearch.entity().getDomainId(), SearchCriteria.Op.NIN);
        ZoneByDomainIdsSearch.done();

        ListPodsSearch = createSearchBuilder(Long.class);
        ListPodsSearch.select(null, Func.DISTINCT, ListPodsSearch.entity().getPodId());
        ListPodsSearch.and("podId", ListPodsSearch.entity().getPodId(), Op.NNULL);
        ListPodsSearch.done();

        ListClustersSearch = createSearchBuilder(Long.class);
        ListClustersSearch.select(null, Func.DISTINCT, ListClustersSearch.entity().getClusterId());
        ListClustersSearch.and("clusterId", ListClustersSearch.entity().getClusterId(), Op.NNULL);
        ListClustersSearch.done();

        ListHostsSearch = createSearchBuilder(Long.class);
        ListHostsSearch.select(null, Func.DISTINCT, ListHostsSearch.entity().getHostId());
        ListHostsSearch.and("hostId", ListHostsSearch.entity().getHostId(), Op.NNULL);
        ListHostsSearch.done();
    }

    @Override
    public DedicatedResourceVO findByZoneId(Long zoneId) {
        SearchCriteria<DedicatedResourceVO> sc = ZoneSearch.create();
        sc.setParameters("zoneId", zoneId);
        return findOneBy(sc);
    }

    @Override
    public DedicatedResourceVO findByPodId(Long podId) {
        SearchCriteria<DedicatedResourceVO> sc = PodSearch.create();
        sc.setParameters("podId", podId);

        return findOneBy(sc);
    }

    @Override
    public DedicatedResourceVO findByClusterId(Long clusterId) {
        SearchCriteria<DedicatedResourceVO> sc = ClusterSearch.create();
        sc.setParameters("clusterId", clusterId);

        return findOneBy(sc);
    }

    @Override
    public DedicatedResourceVO findByHostId(Long hostId) {
        SearchCriteria<DedicatedResourceVO> sc = HostSearch.create();
        sc.setParameters("hostId", hostId);

        return findOneBy(sc);
    }

    @Override
    public Pair<List<DedicatedResourceVO>, Integer> searchDedicatedZones(Long dataCenterId, Long domainId,
            Long accountId, Long affinityGroupId) {
        SearchCriteria<DedicatedResourceVO> sc = ListAllZonesSearch.create();
        if (dataCenterId != null) {
            sc.setParameters("zoneId", dataCenterId);
        }
        if (affinityGroupId != null) {
            sc.setParameters("affinityGroupId", affinityGroupId);
        }
        if(domainId != null) {
            sc.setParameters("domainId", domainId);
            if(accountId != null) {
                sc.setParameters("accountId", accountId);
            } else {
                sc.setParameters("accountId", (Object)null);
            }
        }
        return searchAndCount(sc, null);
    }
    @Override
    public Pair<List<DedicatedResourceVO>, Integer> searchDedicatedPods(Long podId, Long domainId, Long accountId,
            Long affinityGroupId) {
        SearchCriteria<DedicatedResourceVO> sc = ListAllPodsSearch.create();
        if (podId != null) {
            sc.setParameters("podId", podId);
        }
        if (affinityGroupId != null) {
            sc.setParameters("affinityGroupId", affinityGroupId);
        }
        if(domainId != null) {
            sc.setParameters("domainId", domainId);
            if(accountId != null) {
                sc.setParameters("accountId", accountId);
            } else {
                sc.setParameters("accountId", (Object)null);
            }
        }
        return searchAndCount(sc, null);
    }

    @Override
    public Pair<List<DedicatedResourceVO>, Integer> searchDedicatedClusters(Long clusterId, Long domainId,
            Long accountId, Long affinityGroupId) {
        SearchCriteria<DedicatedResourceVO> sc = ListAllClustersSearch.create();
        if (clusterId != null) {
            sc.setParameters("clusterId", clusterId);
        }
        if (affinityGroupId != null) {
            sc.setParameters("affinityGroupId", affinityGroupId);
        }

        if(domainId != null) {
            sc.setParameters("domainId", domainId);
            if(accountId != null) {
                sc.setParameters("accountId", accountId);
            } else {
                sc.setParameters("accountId", (Object)null);
            }
        }
        return searchAndCount(sc, null);
    }

    @Override
    public Pair<List<DedicatedResourceVO>, Integer> searchDedicatedHosts(Long hostId, Long domainId, Long accountId, Long affinityGroupId){
        SearchCriteria<DedicatedResourceVO> sc = ListAllHostsSearch.create();
        if (hostId != null) {
            sc.setParameters("hostId", hostId);
        }
        if (affinityGroupId != null) {
            sc.setParameters("affinityGroupId", affinityGroupId);
        }
        if(domainId != null) {
            sc.setParameters("domainId", domainId);
            if(accountId != null) {
                sc.setParameters("accountId", accountId);
            } else {
                sc.setParameters("accountId", (Object)null);
            }
        }
        return searchAndCount(sc, null);
    }

    @Override
    public List<DedicatedResourceVO> listByAccountId(Long accountId){
        SearchCriteria<DedicatedResourceVO> sc = ListByAccountId.create();
        sc.setParameters("accountId", accountId);
        return listBy(sc);
    }

    @Override
    public List<DedicatedResourceVO> listByDomainId(Long domainId){
        SearchCriteria<DedicatedResourceVO> sc = ListByDomainId.create();
        sc.setParameters("domainId", domainId);
        return listBy(sc);
    }

    @Override
    public List<DedicatedResourceVO> listZonesNotInDomainIds(List<Long> domainIds) {
        SearchCriteria<DedicatedResourceVO> sc = ZoneByDomainIdsSearch.create();
        sc.setParameters("domainId", domainIds.toArray(new Object[domainIds.size()]));
        return listBy(sc);
    }

    @Override
    public boolean remove(Long id) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        DedicatedResourceVO resource = createForUpdate();
        update(id, resource);

        boolean result = super.remove(id);
        txn.commit();
        return result;
    }

    @Override
    public List<Long> listAllPods() {
        SearchCriteria<Long> sc = ListPodsSearch.create();
        return customSearch(sc, null);
    }

    @Override
    public List<Long> listAllClusters() {
        SearchCriteria<Long> sc = ListClustersSearch.create();
        return customSearch(sc, null);
    }

    @Override
    public List<Long> listAllHosts() {
        SearchCriteria<Long> sc = ListHostsSearch.create();
        return customSearch(sc, null);
    }

    @Override
    public List<DedicatedResourceVO> listByAffinityGroupId(Long affinityGroupId) {
        SearchCriteria<DedicatedResourceVO> sc = ListByAffinityGroupId.create();
        sc.setParameters("affinityGroupId", affinityGroupId);
        return listBy(sc);
    }
}
