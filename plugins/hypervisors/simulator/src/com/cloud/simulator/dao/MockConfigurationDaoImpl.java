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
package com.cloud.simulator.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Formatter;

import javax.ejb.Local;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.simulator.MockConfigurationVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;

@Component
@Local(value = {MockConfigurationDao.class})
public class MockConfigurationDaoImpl extends GenericDaoBase<MockConfigurationVO, Long> implements MockConfigurationDao {
    final static Logger s_logger = Logger.getLogger(MockConfigurationDaoImpl.class);
    private final SearchBuilder<MockConfigurationVO> _searchByDcIdName;
    private final SearchBuilder<MockConfigurationVO> _searchByDcIDPodIdName;
    private final SearchBuilder<MockConfigurationVO> _searchByDcIDPodIdClusterIdName;
    private final SearchBuilder<MockConfigurationVO> _searchByDcIDPodIdClusterIdHostIdName;
    private final SearchBuilder<MockConfigurationVO> _searchByGlobalName;

    public MockConfigurationDaoImpl() {
        _searchByGlobalName = createSearchBuilder();
        _searchByGlobalName.and("dcId", _searchByGlobalName.entity().getDataCenterId(), SearchCriteria.Op.NULL);
        _searchByGlobalName.and("podId", _searchByGlobalName.entity().getPodId(), SearchCriteria.Op.NULL);
        _searchByGlobalName.and("clusterId", _searchByGlobalName.entity().getClusterId(), SearchCriteria.Op.NULL);
        _searchByGlobalName.and("hostId", _searchByGlobalName.entity().getHostId(), SearchCriteria.Op.NULL);
        _searchByGlobalName.and("name", _searchByGlobalName.entity().getName(), SearchCriteria.Op.EQ);
        _searchByGlobalName.done();

        _searchByDcIdName = createSearchBuilder();
        _searchByDcIdName.and("dcId", _searchByDcIdName.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        _searchByDcIdName.and("podId", _searchByDcIdName.entity().getPodId(), SearchCriteria.Op.NULL);
        _searchByDcIdName.and("clusterId", _searchByDcIdName.entity().getClusterId(), SearchCriteria.Op.NULL);
        _searchByDcIdName.and("hostId", _searchByDcIdName.entity().getHostId(), SearchCriteria.Op.NULL);
        _searchByDcIdName.and("name", _searchByDcIdName.entity().getName(), SearchCriteria.Op.EQ);
        _searchByDcIdName.done();

        _searchByDcIDPodIdName = createSearchBuilder();
        _searchByDcIDPodIdName.and("dcId", _searchByDcIDPodIdName.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        _searchByDcIDPodIdName.and("podId", _searchByDcIDPodIdName.entity().getPodId(), SearchCriteria.Op.EQ);
        _searchByDcIDPodIdName.and("clusterId", _searchByDcIDPodIdName.entity().getClusterId(), SearchCriteria.Op.NULL);
        _searchByDcIDPodIdName.and("hostId", _searchByDcIDPodIdName.entity().getHostId(), SearchCriteria.Op.NULL);
        _searchByDcIDPodIdName.and("name", _searchByDcIDPodIdName.entity().getName(), SearchCriteria.Op.EQ);
        _searchByDcIDPodIdName.done();

        _searchByDcIDPodIdClusterIdName = createSearchBuilder();
        _searchByDcIDPodIdClusterIdName.and("dcId", _searchByDcIDPodIdClusterIdName.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        _searchByDcIDPodIdClusterIdName.and("podId", _searchByDcIDPodIdClusterIdName.entity().getPodId(), SearchCriteria.Op.EQ);
        _searchByDcIDPodIdClusterIdName.and("clusterId", _searchByDcIDPodIdClusterIdName.entity().getClusterId(), SearchCriteria.Op.EQ);
        _searchByDcIDPodIdClusterIdName.and("hostId", _searchByDcIDPodIdClusterIdName.entity().getHostId(), SearchCriteria.Op.NULL);
        _searchByDcIDPodIdClusterIdName.and("name", _searchByDcIDPodIdClusterIdName.entity().getName(), SearchCriteria.Op.EQ);
        _searchByDcIDPodIdClusterIdName.done();

        _searchByDcIDPodIdClusterIdHostIdName = createSearchBuilder();
        _searchByDcIDPodIdClusterIdHostIdName.and("dcId", _searchByDcIDPodIdClusterIdHostIdName.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        _searchByDcIDPodIdClusterIdHostIdName.and("podId", _searchByDcIDPodIdClusterIdHostIdName.entity().getPodId(), SearchCriteria.Op.EQ);
        _searchByDcIDPodIdClusterIdHostIdName.and("clusterId", _searchByDcIDPodIdClusterIdHostIdName.entity().getClusterId(), SearchCriteria.Op.EQ);
        _searchByDcIDPodIdClusterIdHostIdName.and("hostId", _searchByDcIDPodIdClusterIdHostIdName.entity().getHostId(), SearchCriteria.Op.EQ);
        _searchByDcIDPodIdClusterIdHostIdName.and("name", _searchByDcIDPodIdClusterIdHostIdName.entity().getName(), SearchCriteria.Op.EQ);
        _searchByDcIDPodIdClusterIdHostIdName.done();
    }

    @Override
    public MockConfigurationVO findByCommand(Long dcId, Long podId, Long clusterId, Long hostId, String name) {

        if (dcId == null) {
            SearchCriteria<MockConfigurationVO> sc = _searchByGlobalName.create();
            sc.setParameters("name", name);
            return findOneBy(sc);
        } else if (podId == null) {
            SearchCriteria<MockConfigurationVO> sc = _searchByDcIdName.create();
            sc.setParameters("name", name);
            sc.setParameters("dcId", dcId);
            return findOneBy(sc);
        } else if (clusterId == null) {
            SearchCriteria<MockConfigurationVO> sc = _searchByDcIDPodIdName.create();
            sc.setParameters("name", name);
            sc.setParameters("dcId", dcId);
            sc.setParameters("podId", podId);
            return findOneBy(sc);
        } else if (hostId == null) {
            SearchCriteria<MockConfigurationVO> sc = _searchByDcIDPodIdClusterIdName.create();
            sc.setParameters("name", name);
            sc.setParameters("dcId", dcId);
            sc.setParameters("podId", podId);
            sc.setParameters("clusterId", clusterId);
            return findOneBy(sc);
        } else {
            SearchCriteria<MockConfigurationVO> sc = _searchByDcIDPodIdClusterIdHostIdName.create();
            sc.setParameters("name", name);
            sc.setParameters("dcId", dcId);
            sc.setParameters("podId", podId);
            sc.setParameters("clusterId", clusterId);
            sc.setParameters("hostId", hostId);
            return findOneBy(sc);
        }
    }

    @Override
    public MockConfigurationVO findByNameBottomUP(Long dcId, Long podId, Long clusterId, Long hostId, String name) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        StringBuilder search = new StringBuilder();
        Formatter formatter = new Formatter(search);
        formatter.format("select * from mockconfiguration where (name='%s') and ((data_center_id = %d and pod_id = %d and cluster_id = %d and host_id = %d)", name, dcId,
            podId, clusterId, hostId);
        formatter.format(" or (data_center_id = %d and pod_id = %d and cluster_id = %d and host_id is null)", dcId, podId, clusterId);
        formatter.format(" or (data_center_id = %d and pod_id = %d and cluster_id is null and host_id is null)", dcId, podId);
        formatter.format(" or (data_center_id = %d and pod_id is null and cluster_id is null and host_id is null)", dcId);
        formatter.format(" or (data_center_id is null and pod_id is null and cluster_id is null and host_id is null))");
        formatter.format(" and removed is NULL ORDER BY id ASC LIMIT 1 for update");
        formatter.close();

        String sql = search.toString();
        try (
                PreparedStatement pstmt = txn.prepareAutoCloseStatement(sql);
                ResultSet rs = pstmt.executeQuery();) {
            if (rs.next()) {
                return toEntityBean(rs, false);
            }
        } catch (Exception e) {
            s_logger.info("[ignored]"
                    + "error while executing dynamically build search: " + e.getLocalizedMessage());
        }
        return null;
    }

}
