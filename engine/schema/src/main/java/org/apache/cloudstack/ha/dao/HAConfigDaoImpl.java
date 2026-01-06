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

package org.apache.cloudstack.ha.dao;

import com.cloud.utils.DateUtil;
import com.cloud.utils.db.Attribute;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.db.UpdateBuilder;
import org.apache.cloudstack.ha.HAConfig;
import org.apache.cloudstack.ha.HAConfigVO;
import org.apache.cloudstack.ha.HAResource;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@DB
@Component
public class HAConfigDaoImpl extends GenericDaoBase<HAConfigVO, Long> implements HAConfigDao {

    private static final String EXPIRE_OWNERSHIP = "UPDATE ha_config set mgmt_server_id=NULL where mgmt_server_id=?";

    private SearchBuilder<HAConfigVO> ResourceSearch;
    private SearchBuilder<HAConfigVO> StateUpdateSearch;

    private Attribute HAStateAttr;
    private Attribute MsIdAttr;
    private Attribute UpdateTimeAttr;

    public HAConfigDaoImpl() {
        super();

        ResourceSearch = createSearchBuilder();
        ResourceSearch.and("resourceId", ResourceSearch.entity().getResourceId(), SearchCriteria.Op.EQ);
        ResourceSearch.and("resourceType", ResourceSearch.entity().getResourceType(), SearchCriteria.Op.EQ);
        ResourceSearch.done();

        StateUpdateSearch = createSearchBuilder();
        StateUpdateSearch.and("id", StateUpdateSearch.entity().getId(), SearchCriteria.Op.EQ);
        StateUpdateSearch.and("haState", StateUpdateSearch.entity().getHaState(), SearchCriteria.Op.EQ);
        StateUpdateSearch.and("update", StateUpdateSearch.entity().getUpdateCount(), SearchCriteria.Op.EQ);
        StateUpdateSearch.done();

        HAStateAttr = _allAttributes.get("haState");
        MsIdAttr = _allAttributes.get("managementServerId");
        UpdateTimeAttr = _allAttributes.get("updateTime");
        assert (HAStateAttr != null && MsIdAttr != null && UpdateTimeAttr != null) : "Couldn't find one of these attributes";
    }

    @Override
    public boolean updateState(HAConfig.HAState currentState, HAConfig.Event event, HAConfig.HAState nextState, HAConfig vo, Object data) {
        HAConfigVO haConfig = (HAConfigVO) vo;
        if (haConfig == null) {
            if (logger.isTraceEnabled()) {
                logger.trace("Invalid ha config view object provided");
            }
            return false;
        }

        Long newManagementServerId = event.getServerId();
        if (currentState == nextState && (haConfig.getManagementServerId() != null && haConfig.getManagementServerId().equals(newManagementServerId))) {
            return false;
        }

        if (event == HAConfig.Event.Disabled) {
            newManagementServerId = null;
        }

        SearchCriteria<HAConfigVO> sc = StateUpdateSearch.create();
        sc.setParameters("id", haConfig.getId());
        sc.setParameters("haState", currentState);
        sc.setParameters("update", haConfig.getUpdateCount());

        haConfig.incrUpdateCount();
        UpdateBuilder ub = getUpdateBuilder(haConfig);
        ub.set(haConfig, HAStateAttr, nextState);
        ub.set(haConfig, UpdateTimeAttr, DateUtil.currentGMTTime());
        ub.set(haConfig, MsIdAttr, newManagementServerId);

        int result = update(ub, sc, null);
        if (logger.isTraceEnabled() && result <= 0) {
            logger.trace(String.format("Failed to update HA state from:%s to:%s due to event:%s for the ha_config id:%d", currentState, nextState, event, haConfig.getId()));
        }
        return result > 0;
    }

    @Override
    public HAConfig findHAResource(final long resourceId, final HAResource.ResourceType resourceType) {
        final SearchCriteria<HAConfigVO> sc = ResourceSearch.create();
        sc.setParameters("resourceId", resourceId);
        sc.setParameters("resourceType", resourceType);
        return findOneBy(sc);
    }

    @Override
    public List<HAConfig> listHAResource(final Long resourceId, final HAResource.ResourceType resourceType) {
        final SearchCriteria<HAConfigVO> sc = ResourceSearch.create();
        if (resourceId != null && resourceId > 0L) {
            sc.setParameters("resourceId", resourceId);
        }
        if (resourceType != null) {
            sc.setParameters("resourceType", resourceType);
        }
        return new ArrayList<HAConfig>(listBy(sc));
    }

    @Override
    public void expireServerOwnership(final long serverId) {
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                TransactionLegacy txn = TransactionLegacy.currentTxn();
                try (final PreparedStatement pstmt = txn.prepareAutoCloseStatement(EXPIRE_OWNERSHIP);) {
                    pstmt.setLong(1, serverId);
                    pstmt.executeUpdate();
                } catch (SQLException e) {
                    txn.rollback();
                    logger.warn("Failed to expire HA ownership of management server id: " + serverId);
                }
            }
        });
    }
}
