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
package org.apache.cloudstack.engine.datacenter.entity.api.db.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


import org.springframework.stereotype.Component;

import org.apache.cloudstack.engine.datacenter.entity.api.DataCenterResourceEntity;
import org.apache.cloudstack.engine.datacenter.entity.api.DataCenterResourceEntity.State;
import org.apache.cloudstack.engine.datacenter.entity.api.DataCenterResourceEntity.State.Event;
import org.apache.cloudstack.engine.datacenter.entity.api.db.EngineHostPodVO;

import com.cloud.org.Grouping;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.UpdateBuilder;

@Component(value = "EngineHostPodDao")
public class EngineHostPodDaoImpl extends GenericDaoBase<EngineHostPodVO, Long> implements EngineHostPodDao {

    protected SearchBuilder<EngineHostPodVO> DataCenterAndNameSearch;
    protected SearchBuilder<EngineHostPodVO> DataCenterIdSearch;
    protected SearchBuilder<EngineHostPodVO> UUIDSearch;
    protected SearchBuilder<EngineHostPodVO> StateChangeSearch;

    protected EngineHostPodDaoImpl() {
        DataCenterAndNameSearch = createSearchBuilder();
        DataCenterAndNameSearch.and("dc", DataCenterAndNameSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        DataCenterAndNameSearch.and("name", DataCenterAndNameSearch.entity().getName(), SearchCriteria.Op.EQ);
        DataCenterAndNameSearch.done();

        DataCenterIdSearch = createSearchBuilder();
        DataCenterIdSearch.and("dcId", DataCenterIdSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        DataCenterIdSearch.done();

        UUIDSearch = createSearchBuilder();
        UUIDSearch.and("uuid", UUIDSearch.entity().getUuid(), SearchCriteria.Op.EQ);
        UUIDSearch.done();

        StateChangeSearch = createSearchBuilder();
        StateChangeSearch.and("id", StateChangeSearch.entity().getId(), SearchCriteria.Op.EQ);
        StateChangeSearch.and("state", StateChangeSearch.entity().getState(), SearchCriteria.Op.EQ);
        StateChangeSearch.done();

    }

    @Override
    public List<EngineHostPodVO> listByDataCenterId(long id) {
        SearchCriteria<EngineHostPodVO> sc = DataCenterIdSearch.create();
        sc.setParameters("dcId", id);

        return listBy(sc);
    }

    @Override
    public EngineHostPodVO findByName(String name, long dcId) {
        SearchCriteria<EngineHostPodVO> sc = DataCenterAndNameSearch.create();
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
        EngineHostPodVO pod = createForUpdate();
        pod.setName(null);

        update(id, pod);

        boolean result = super.remove(id);
        txn.commit();
        return result;
    }

    @Override
    public List<Long> listDisabledPods(long zoneId) {
        GenericSearchBuilder<EngineHostPodVO, Long> podIdSearch = createSearchBuilder(Long.class);
        podIdSearch.selectFields(podIdSearch.entity().getId());
        podIdSearch.and("dataCenterId", podIdSearch.entity().getDataCenterId(), Op.EQ);
        podIdSearch.and("allocationState", podIdSearch.entity().getAllocationState(), Op.EQ);
        podIdSearch.done();

        SearchCriteria<Long> sc = podIdSearch.create();
        sc.addAnd("dataCenterId", SearchCriteria.Op.EQ, zoneId);
        sc.addAnd("allocationState", SearchCriteria.Op.EQ, Grouping.AllocationState.Disabled);
        return customSearch(sc, null);
    }

    @Override
    public boolean updateState(State currentState, Event event, State nextState, DataCenterResourceEntity podEntity, Object data) {

        EngineHostPodVO vo = findById(podEntity.getId());

        Date oldUpdatedTime = vo.getLastUpdated();

        SearchCriteria<EngineHostPodVO> sc = StateChangeSearch.create();
        sc.setParameters("id", vo.getId());
        sc.setParameters("state", currentState);

        UpdateBuilder builder = getUpdateBuilder(vo);
        builder.set(vo, "state", nextState);
        builder.set(vo, "lastUpdated", new Date());

        int rows = update(vo, sc);

        if (rows == 0 && logger.isDebugEnabled()) {
            EngineHostPodVO dbDC = findByIdIncludingRemoved(vo.getId());
            if (dbDC != null) {
                StringBuilder str = new StringBuilder("Unable to update ").append(vo.toString());
                str.append(": DB Data={id=").append(dbDC.getId()).append("; state=").append(dbDC.getState()).append(";updatedTime=").append(dbDC.getLastUpdated());
                str.append(": New Data={id=")
                    .append(vo.getId())
                    .append("; state=")
                    .append(nextState)
                    .append("; event=")
                    .append(event)
                    .append("; updatedTime=")
                    .append(vo.getLastUpdated());
                str.append(": stale Data={id=")
                    .append(vo.getId())
                    .append("; state=")
                    .append(currentState)
                    .append("; event=")
                    .append(event)
                    .append("; updatedTime=")
                    .append(oldUpdatedTime);
            } else {
                logger.debug("Unable to update dataCenter: id=" + vo.getId() + ", as there is no such dataCenter exists in the database anymore");
            }
        }
        return rows > 0;

    }

}
