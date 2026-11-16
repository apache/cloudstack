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

package org.apache.cloudstack.outofbandmanagement.dao;

import com.cloud.utils.DateUtil;
import com.cloud.utils.db.Attribute;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.db.UpdateBuilder;
import org.apache.cloudstack.outofbandmanagement.OutOfBandManagement;
import org.apache.cloudstack.outofbandmanagement.OutOfBandManagementVO;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@DB
@Component
public class OutOfBandManagementDaoImpl extends GenericDaoBase<OutOfBandManagementVO, Long> implements OutOfBandManagementDao {

    private SearchBuilder<OutOfBandManagementVO> HostSearch;
    private SearchBuilder<OutOfBandManagementVO> ManagementServerSearch;
    private SearchBuilder<OutOfBandManagementVO> OutOfBandManagementOwnerSearch;
    private SearchBuilder<OutOfBandManagementVO> StateUpdateSearch;

    private Attribute PowerStateAttr;
    private Attribute MsIdAttr;
    private Attribute UpdateTimeAttr;

    public OutOfBandManagementDaoImpl() {
        super();

        HostSearch = createSearchBuilder();
        HostSearch.and("hostId", HostSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        HostSearch.done();

        ManagementServerSearch = createSearchBuilder();
        ManagementServerSearch.and("server", ManagementServerSearch.entity().getManagementServerId(), SearchCriteria.Op.EQ);
        ManagementServerSearch.done();

        OutOfBandManagementOwnerSearch = createSearchBuilder();
        OutOfBandManagementOwnerSearch.and("server", OutOfBandManagementOwnerSearch.entity().getManagementServerId(), SearchCriteria.Op.EQ);
        OutOfBandManagementOwnerSearch.or("serverNull", OutOfBandManagementOwnerSearch.entity().getManagementServerId(), SearchCriteria.Op.NULL);
        OutOfBandManagementOwnerSearch.done();

        StateUpdateSearch = createSearchBuilder();
        StateUpdateSearch.and("status", StateUpdateSearch.entity().getPowerState(), SearchCriteria.Op.EQ);
        StateUpdateSearch.and("id", StateUpdateSearch.entity().getId(), SearchCriteria.Op.EQ);
        StateUpdateSearch.and("update", StateUpdateSearch.entity().getUpdateCount(), SearchCriteria.Op.EQ);
        StateUpdateSearch.done();

        PowerStateAttr = _allAttributes.get("powerState");
        MsIdAttr = _allAttributes.get("managementServerId");
        UpdateTimeAttr = _allAttributes.get("updateTime");
        assert (PowerStateAttr != null && MsIdAttr != null && UpdateTimeAttr != null) : "Couldn't find one of these attributes";
    }

    @Override
    public OutOfBandManagement findByHost(long hostId) {
        SearchCriteria<OutOfBandManagementVO> sc = HostSearch.create("hostId", hostId);
        return findOneBy(sc);
    }

    @Override
    public OutOfBandManagementVO findByHostAddress(String address) {
        SearchCriteria<OutOfBandManagementVO> sc = HostSearch.create("address", address);
        return findOneBy(sc);
    }

    @Override
    public List<OutOfBandManagementVO> findAllByManagementServer(long serverId) {
        SearchCriteria<OutOfBandManagementVO> sc = OutOfBandManagementOwnerSearch.create();
        sc.setParameters("server", serverId);
        return listBy(sc, new Filter(OutOfBandManagementVO.class, "updateTime", true, null, null));
    }

    private void executeExpireOwnershipSql(final String sql, final long resource) {
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                TransactionLegacy txn = TransactionLegacy.currentTxn();
                try (final PreparedStatement pstmt = txn.prepareAutoCloseStatement(sql);) {
                    pstmt.setLong(1, resource);
                    pstmt.executeUpdate();
                } catch (SQLException e) {
                    txn.rollback();
                    logger.warn("Failed to expire ownership for out-of-band management server id: " + resource);
                }
            }
        });
    }

    @Override
    public void expireServerOwnership(long serverId) {
        final String resetOwnerSql = "UPDATE oobm set mgmt_server_id=NULL, power_state=NULL where mgmt_server_id=?";
        executeExpireOwnershipSql(resetOwnerSql, serverId);
        if (logger.isDebugEnabled()) {
            logger.debug("Expired out-of-band management ownership for hosts owned by management server id:" + serverId);
        }
    }

    @Override
    public boolean updateState(OutOfBandManagement.PowerState oldStatus, OutOfBandManagement.PowerState.Event event, OutOfBandManagement.PowerState newStatus, OutOfBandManagement vo, Object data) {
        OutOfBandManagementVO oobmHost = (OutOfBandManagementVO) vo;
        if (oobmHost == null) {
            if (logger.isTraceEnabled()) {
                logger.trace("Invalid out-of-band management host view object provided");
            }
            return false;
        }

        Long newManagementServerId = event.getServerId();
        // Avoid updates when old ownership and state are same as new
        if (oldStatus == newStatus && (oobmHost.getManagementServerId() != null && oobmHost.getManagementServerId().equals(newManagementServerId))) {
            return false;
        }

        if (event == OutOfBandManagement.PowerState.Event.Disabled) {
            newManagementServerId = null;
        }

        SearchCriteria<OutOfBandManagementVO> sc = StateUpdateSearch.create();
        sc.setParameters("status", oldStatus);
        sc.setParameters("id", oobmHost.getId());
        sc.setParameters("update", oobmHost.getUpdateCount());

        oobmHost.incrUpdateCount();
        UpdateBuilder ub = getUpdateBuilder(oobmHost);
        ub.set(oobmHost, PowerStateAttr, newStatus);
        ub.set(oobmHost, UpdateTimeAttr, DateUtil.currentGMTTime());
        ub.set(oobmHost, MsIdAttr, newManagementServerId);

        int result = update(ub, sc, null);
        if (logger.isDebugEnabled() && result <= 0) {
            logger.debug(String.format("Failed to update out-of-band management power state from:%s to:%s due to event:%s for the host id:%d", oldStatus, newStatus, event, oobmHost.getHostId()));
        }
        return result > 0;
    }
}
