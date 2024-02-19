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


import org.springframework.stereotype.Component;

import com.cloud.dc.HostPodVO;
import com.cloud.org.Grouping;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.TransactionLegacy;

@Component
public class HostPodDaoImpl extends GenericDaoBase<HostPodVO, Long> implements HostPodDao {

    protected SearchBuilder<HostPodVO> DataCenterAndNameSearch;
    protected SearchBuilder<HostPodVO> DataCenterIdSearch;
    protected GenericSearchBuilder<HostPodVO, Long> PodIdSearch;

    public HostPodDaoImpl() {
        DataCenterAndNameSearch = createSearchBuilder();
        DataCenterAndNameSearch.and("dc", DataCenterAndNameSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        DataCenterAndNameSearch.and("name", DataCenterAndNameSearch.entity().getName(), SearchCriteria.Op.EQ);
        DataCenterAndNameSearch.done();

        DataCenterIdSearch = createSearchBuilder();
        DataCenterIdSearch.and("dcId", DataCenterIdSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        DataCenterIdSearch.done();

        PodIdSearch = createSearchBuilder(Long.class);
        PodIdSearch.selectFields(PodIdSearch.entity().getId());
        PodIdSearch.and("dataCenterId", PodIdSearch.entity().getDataCenterId(), Op.EQ);
        PodIdSearch.and("allocationState", PodIdSearch.entity().getAllocationState(), Op.EQ);
        PodIdSearch.done();
    }

    @Override
    public List<HostPodVO> listByDataCenterId(long id) {
        SearchCriteria<HostPodVO> sc = DataCenterIdSearch.create();
        sc.setParameters("dcId", id);

        return listBy(sc);
    }

    @Override
    public HostPodVO findByName(String name, long dcId) {
        SearchCriteria<HostPodVO> sc = DataCenterAndNameSearch.create();
        sc.setParameters("dc", dcId);
        sc.setParameters("name", name);

        return findOneBy(sc);
    }

    @Override
    public HashMap<Long, List<Object>> getCurrentPodCidrSubnets(long zoneId, long podIdToSkip) {
        HashMap<Long, List<Object>> currentPodCidrSubnets = new HashMap<Long, List<Object>>();

        String selectSql = "SELECT id, cidr_address, cidr_size FROM host_pod_ref WHERE data_center_id=" + zoneId + " and removed IS NULL";
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try {
            PreparedStatement stmt = txn.prepareAutoCloseStatement(selectSql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Long podId = rs.getLong("id");
                if (podId.longValue() == podIdToSkip) {
                    continue;
                }
                String cidrAddress = rs.getString("cidr_address");
                long cidrSize = rs.getLong("cidr_size");
                List<Object> cidrPair = new ArrayList<Object>();
                cidrPair.add(0, cidrAddress);
                cidrPair.add(1, new Long(cidrSize));
                currentPodCidrSubnets.put(podId, cidrPair);
            }
        } catch (SQLException ex) {
            logger.warn("DB exception " + ex.getMessage(), ex);
            return null;
        }

        return currentPodCidrSubnets;
    }

    @Override
    public boolean remove(Long id) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        HostPodVO pod = createForUpdate();
        pod.setName(null);

        update(id, pod);

        boolean result = super.remove(id);
        txn.commit();
        return result;
    }

    @Override
    public List<Long> listDisabledPods(long zoneId) {
        SearchCriteria<Long> sc = PodIdSearch.create();
        sc.addAnd("dataCenterId", SearchCriteria.Op.EQ, zoneId);
        sc.addAnd("allocationState", SearchCriteria.Op.EQ, Grouping.AllocationState.Disabled);
        return customSearch(sc, null);
    }

    @Override
    public List<Long> listAllPods(Long zoneId) {
        SearchCriteria<Long> sc = PodIdSearch.create();
        if (zoneId != null) {
            sc.addAnd("dataCenterId", SearchCriteria.Op.EQ, zoneId);
        }
        return customSearch(sc, null);
    }
    @Override
    public List<HostPodVO> listAllPodsByCidr(long zoneId, String cidr) {
        SearchCriteria<HostPodVO> sc = DataCenterAndNameSearch.create();
        sc.setParameters("dataCenterId", zoneId);
        sc.setParameters("cidr_address", cidr);
        return listBy(sc);
    }

}
