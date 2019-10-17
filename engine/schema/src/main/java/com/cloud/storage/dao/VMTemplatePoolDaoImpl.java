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
package com.cloud.storage.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectInStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.Event;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.State;

import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.UpdateBuilder;

@Component
public class VMTemplatePoolDaoImpl extends GenericDaoBase<VMTemplateStoragePoolVO, Long> implements VMTemplatePoolDao {
    public static final Logger s_logger = Logger.getLogger(VMTemplatePoolDaoImpl.class.getName());

    @Inject
    DataStoreManager dataStoreManager;

    protected final SearchBuilder<VMTemplateStoragePoolVO> PoolSearch;
    protected final SearchBuilder<VMTemplateStoragePoolVO> TemplateSearch;
    protected final SearchBuilder<VMTemplateStoragePoolVO> PoolTemplateSearch;
    protected final SearchBuilder<VMTemplateStoragePoolVO> TemplateStatusSearch;
    protected final SearchBuilder<VMTemplateStoragePoolVO> TemplatePoolStatusSearch;
    protected final SearchBuilder<VMTemplateStoragePoolVO> TemplateStatesSearch;
    protected final SearchBuilder<VMTemplateStoragePoolVO> TemplatePoolStateSearch;
    protected final SearchBuilder<VMTemplateStoragePoolVO> updateStateSearch;

    protected static final String UPDATE_TEMPLATE_HOST_REF = "UPDATE template_spool_ref SET download_state = ?, download_pct= ?, last_updated = ? "
        + ", error_str = ?, local_path = ?, job_id = ? " + "WHERE pool_id = ? and template_id = ?";

    protected static final String DOWNLOADS_STATE_DC = "SELECT * FROM template_spool_ref t, storage_pool p where t.pool_id = p.id and p.data_center_id=? "
        + " and t.template_id=? and t.download_state = ?";

    protected static final String DOWNLOADS_STATE_DC_POD =
        "SELECT * FROM template_spool_ref tp, storage_pool_host_ref ph, host h where tp.pool_id = ph.pool_id and ph.host_id = h.id and h.data_center_id=? and h.pod_id=? "
            + " and tp.template_id=? and tp.download_state=?";

    protected static final String HOST_TEMPLATE_SEARCH =
        "SELECT * FROM template_spool_ref tp, storage_pool_host_ref ph, host h where tp.pool_id = ph.pool_id and ph.host_id = h.id and h.id=? "
            + " and tp.template_id=? ";

    public VMTemplatePoolDaoImpl() {
        PoolSearch = createSearchBuilder();
        PoolSearch.and("pool_id", PoolSearch.entity().getPoolId(), SearchCriteria.Op.EQ);
        PoolSearch.done();

        TemplateSearch = createSearchBuilder();
        TemplateSearch.and("template_id", TemplateSearch.entity().getTemplateId(), SearchCriteria.Op.EQ);
        TemplateSearch.done();

        PoolTemplateSearch = createSearchBuilder();
        PoolTemplateSearch.and("pool_id", PoolTemplateSearch.entity().getPoolId(), SearchCriteria.Op.EQ);
        PoolTemplateSearch.and("template_id", PoolTemplateSearch.entity().getTemplateId(), SearchCriteria.Op.EQ);
        PoolTemplateSearch.done();

        TemplateStatusSearch = createSearchBuilder();
        TemplateStatusSearch.and("template_id", TemplateStatusSearch.entity().getTemplateId(), SearchCriteria.Op.EQ);
        TemplateStatusSearch.and("download_state", TemplateStatusSearch.entity().getDownloadState(), SearchCriteria.Op.EQ);
        TemplateStatusSearch.done();

        TemplatePoolStatusSearch = createSearchBuilder();
        TemplatePoolStatusSearch.and("pool_id", TemplatePoolStatusSearch.entity().getPoolId(), SearchCriteria.Op.EQ);
        TemplatePoolStatusSearch.and("template_id", TemplatePoolStatusSearch.entity().getTemplateId(), SearchCriteria.Op.EQ);
        TemplatePoolStatusSearch.and("download_state", TemplatePoolStatusSearch.entity().getDownloadState(), SearchCriteria.Op.EQ);
        TemplatePoolStatusSearch.done();

        TemplatePoolStateSearch = createSearchBuilder();
        TemplatePoolStateSearch.and("pool_id", TemplatePoolStateSearch.entity().getPoolId(), SearchCriteria.Op.EQ);
        TemplatePoolStateSearch.and("state", TemplatePoolStateSearch.entity().getState(), SearchCriteria.Op.EQ);
        TemplatePoolStateSearch.done();

        TemplateStatesSearch = createSearchBuilder();
        TemplateStatesSearch.and("template_id", TemplateStatesSearch.entity().getTemplateId(), SearchCriteria.Op.EQ);
        TemplateStatesSearch.and("states", TemplateStatesSearch.entity().getDownloadState(), SearchCriteria.Op.IN);
        TemplateStatesSearch.done();

        updateStateSearch = this.createSearchBuilder();
        updateStateSearch.and("id", updateStateSearch.entity().getId(), Op.EQ);
        updateStateSearch.and("state", updateStateSearch.entity().getState(), Op.EQ);
        updateStateSearch.and("updatedCount", updateStateSearch.entity().getUpdatedCount(), Op.EQ);
        updateStateSearch.done();
    }

    @Override
    public List<VMTemplateStoragePoolVO> listByPoolId(long id) {
        SearchCriteria<VMTemplateStoragePoolVO> sc = PoolSearch.create();
        sc.setParameters("pool_id", id);
        return listIncludingRemovedBy(sc);
    }

    @Override
    public List<VMTemplateStoragePoolVO> listByTemplateId(long templateId) {
        SearchCriteria<VMTemplateStoragePoolVO> sc = TemplateSearch.create();
        sc.setParameters("template_id", templateId);
        return listIncludingRemovedBy(sc);
    }

    @Override
    public VMTemplateStoragePoolVO findByPoolTemplate(long poolId, long templateId) {
        SearchCriteria<VMTemplateStoragePoolVO> sc = PoolTemplateSearch.create();
        sc.setParameters("pool_id", poolId);
        sc.setParameters("template_id", templateId);
        return findOneIncludingRemovedBy(sc);
    }

    @Override
    public List<VMTemplateStoragePoolVO> listByTemplateStatus(long templateId, VMTemplateStoragePoolVO.Status downloadState) {
        SearchCriteria<VMTemplateStoragePoolVO> sc = TemplateStatusSearch.create();
        sc.setParameters("template_id", templateId);
        sc.setParameters("download_state", downloadState.toString());
        return listIncludingRemovedBy(sc);
    }

    @Override
    public List<VMTemplateStoragePoolVO> listByPoolIdAndState(long poolId, ObjectInDataStoreStateMachine.State state) {
        SearchCriteria<VMTemplateStoragePoolVO> sc = TemplatePoolStateSearch.create();
        sc.setParameters("pool_id", poolId);
        sc.setParameters("state", state);
        return listIncludingRemovedBy(sc);
    }

    @Override
    public List<VMTemplateStoragePoolVO> listByTemplateStatus(long templateId, VMTemplateStoragePoolVO.Status downloadState, long poolId) {
        SearchCriteria<VMTemplateStoragePoolVO> sc = TemplatePoolStatusSearch.create();
        sc.setParameters("pool_id", poolId);
        sc.setParameters("template_id", templateId);
        sc.setParameters("download_state", downloadState.toString());
        return listIncludingRemovedBy(sc);
    }

    @Override
    public List<VMTemplateStoragePoolVO> listByTemplateStatus(long templateId, long datacenterId, VMTemplateStoragePoolVO.Status downloadState) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        PreparedStatement pstmt = null;
        List<VMTemplateStoragePoolVO> result = new ArrayList<VMTemplateStoragePoolVO>();
        try {
            String sql = DOWNLOADS_STATE_DC;
            pstmt = txn.prepareAutoCloseStatement(sql);
            pstmt.setLong(1, datacenterId);
            pstmt.setLong(2, templateId);
            pstmt.setString(3, downloadState.toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                result.add(toEntityBean(rs, false));
            }
        } catch (Exception e) {
            s_logger.warn("Exception: ", e);
        }
        return result;

    }

    @Override
    public List<VMTemplateStoragePoolVO> listByTemplateStatus(long templateId, long datacenterId, long podId, VMTemplateStoragePoolVO.Status downloadState) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        List<VMTemplateStoragePoolVO> result = new ArrayList<VMTemplateStoragePoolVO>();
        String sql = DOWNLOADS_STATE_DC_POD;
        try(PreparedStatement pstmt = txn.prepareStatement(sql);) {
            pstmt.setLong(1, datacenterId);
            pstmt.setLong(2, podId);
            pstmt.setLong(3, templateId);
            pstmt.setString(4, downloadState.toString());
            try(ResultSet rs = pstmt.executeQuery();) {
                while (rs.next()) {
                    // result.add(toEntityBean(rs, false)); TODO: this is buggy in
                    // GenericDaoBase for hand constructed queries
                    long id = rs.getLong(1); // ID column
                    result.add(findById(id));
                }
            }catch (Exception e) {
                s_logger.warn("Exception: ", e);
            }
        } catch (Exception e) {
            s_logger.warn("Exception: ", e);
        }
        return result;

    }

    public List<VMTemplateStoragePoolVO> listByHostTemplate(long hostId, long templateId) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        List<VMTemplateStoragePoolVO> result = new ArrayList<VMTemplateStoragePoolVO>();
        String sql = HOST_TEMPLATE_SEARCH;
        try(PreparedStatement pstmt = txn.prepareStatement(sql);) {
            pstmt.setLong(1, hostId);
            pstmt.setLong(2, templateId);
            try(ResultSet rs = pstmt.executeQuery();) {
                while (rs.next()) {
                    // result.add(toEntityBean(rs, false)); TODO: this is buggy in
                    // GenericDaoBase for hand constructed queries
                    long id = rs.getLong(1); // ID column
                    result.add(findById(id));
                }
            }catch (Exception e) {
                s_logger.warn("Exception: ", e);
            }
        } catch (Exception e) {
            s_logger.warn("Exception: ", e);
        }
        return result;

    }

    @Override
    public boolean templateAvailable(long templateId, long hostId) {
        VMTemplateStorageResourceAssoc tmpltPool = findByPoolTemplate(hostId, templateId);
        if (tmpltPool == null)
            return false;

        return tmpltPool.getDownloadState() == Status.DOWNLOADED;
    }

    @Override
    public List<VMTemplateStoragePoolVO> listByTemplateStates(long templateId, VMTemplateStoragePoolVO.Status... states) {
        SearchCriteria<VMTemplateStoragePoolVO> sc = TemplateStatesSearch.create();
        sc.setParameters("states", (Object[])states);
        sc.setParameters("template_id", templateId);

        return search(sc, null);
    }

    @Override
    public VMTemplateStoragePoolVO findByHostTemplate(Long hostId, Long templateId) {
        List<VMTemplateStoragePoolVO> result = listByHostTemplate(hostId, templateId);
        return (result.size() == 0) ? null : result.get(1);
    }

    @Override
    public boolean updateState(State currentState, Event event, State nextState, DataObjectInStore vo, Object data) {
        VMTemplateStoragePoolVO templatePool = (VMTemplateStoragePoolVO)vo;
        Long oldUpdated = templatePool.getUpdatedCount();
        Date oldUpdatedTime = templatePool.getUpdated();

        SearchCriteria<VMTemplateStoragePoolVO> sc = updateStateSearch.create();
        sc.setParameters("id", templatePool.getId());
        sc.setParameters("state", currentState);
        sc.setParameters("updatedCount", templatePool.getUpdatedCount());

        templatePool.incrUpdatedCount();

        UpdateBuilder builder = getUpdateBuilder(vo);
        builder.set(vo, "state", nextState);
        builder.set(vo, "updated", new Date());

        int rows = update((VMTemplateStoragePoolVO)vo, sc);
        if (rows == 0 && s_logger.isDebugEnabled()) {
            VMTemplateStoragePoolVO dbVol = findByIdIncludingRemoved(templatePool.getId());
            if (dbVol != null) {
                StringBuilder str = new StringBuilder("Unable to update ").append(vo.toString());
                str.append(": DB Data={id=")
                    .append(dbVol.getId())
                    .append("; state=")
                    .append(dbVol.getState())
                    .append("; updatecount=")
                    .append(dbVol.getUpdatedCount())
                    .append(";updatedTime=")
                    .append(dbVol.getUpdated());
                str.append(": New Data={id=")
                    .append(templatePool.getId())
                    .append("; state=")
                    .append(nextState)
                    .append("; event=")
                    .append(event)
                    .append("; updatecount=")
                    .append(templatePool.getUpdatedCount())
                    .append("; updatedTime=")
                    .append(templatePool.getUpdated());
                str.append(": stale Data={id=")
                    .append(templatePool.getId())
                    .append("; state=")
                    .append(currentState)
                    .append("; event=")
                    .append(event)
                    .append("; updatecount=")
                    .append(oldUpdated)
                    .append("; updatedTime=")
                    .append(oldUpdatedTime);
            } else {
                s_logger.debug("Unable to update objectIndatastore: id=" + templatePool.getId() + ", as there is no such object exists in the database anymore");
            }
        }
        return rows > 0;
    }

}
