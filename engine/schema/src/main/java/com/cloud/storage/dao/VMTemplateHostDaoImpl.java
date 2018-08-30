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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectInStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.Event;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.State;

import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.utils.DateUtil;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.UpdateBuilder;

@Component
public class VMTemplateHostDaoImpl extends GenericDaoBase<VMTemplateHostVO, Long> implements VMTemplateHostDao {
    public static final Logger s_logger = Logger.getLogger(VMTemplateHostDaoImpl.class.getName());
    @Inject
    HostDao _hostDao;
    protected final SearchBuilder<VMTemplateHostVO> HostSearch;
    protected final SearchBuilder<VMTemplateHostVO> TemplateSearch;
    protected final SearchBuilder<VMTemplateHostVO> HostTemplateSearch;
    protected final SearchBuilder<VMTemplateHostVO> HostTemplateStateSearch;
    protected final SearchBuilder<VMTemplateHostVO> HostDestroyedSearch;
    protected final SearchBuilder<VMTemplateHostVO> TemplateStatusSearch;
    protected final SearchBuilder<VMTemplateHostVO> TemplateStatesSearch;
    protected final SearchBuilder<VMTemplateHostVO> updateStateSearch;
    protected SearchBuilder<VMTemplateHostVO> ZoneTemplateSearch;
    protected SearchBuilder<VMTemplateHostVO> LocalSecondaryStorageSearch;

    protected static final String UPDATE_TEMPLATE_HOST_REF = "UPDATE template_host_ref SET download_state = ?, download_pct= ?, last_updated = ? "
        + ", error_str = ?, local_path = ?, job_id = ? " + "WHERE host_id = ? and type_id = ?";

    protected static final String DOWNLOADS_STATE_DC = "SELECT t.id, t.host_id, t.template_id, t.created, t.last_updated, t.job_id, "
        + "t.download_pct, t.size, t.physical_size, t.download_state, t.error_str, t.local_path, "
        + "t.install_path, t.url, t.destroyed, t.is_copy FROM template_host_ref t, host h " + "where t.host_id = h.id and h.data_center_id=? "
        + " and t.template_id=? and t.download_state = ?";

    protected static final String DOWNLOADS_STATE_DC_POD = "SELECT * FROM template_host_ref t, host h where t.host_id = h.id and h.data_center_id=? and h.pod_id=? "
        + " and t.template_id=? and t.download_state=?";

    protected static final String DOWNLOADS_STATE = "SELECT * FROM template_host_ref t " + " where t.template_id=? and t.download_state=?";

    public VMTemplateHostDaoImpl() {
        HostSearch = createSearchBuilder();
        HostSearch.and("host_id", HostSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        HostSearch.done();

        TemplateSearch = createSearchBuilder();
        TemplateSearch.and("template_id", TemplateSearch.entity().getTemplateId(), SearchCriteria.Op.EQ);
        TemplateSearch.and("destroyed", TemplateSearch.entity().getDestroyed(), SearchCriteria.Op.EQ);
        TemplateSearch.done();

        HostTemplateSearch = createSearchBuilder();
        HostTemplateSearch.and("host_id", HostTemplateSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        HostTemplateSearch.and("template_id", HostTemplateSearch.entity().getTemplateId(), SearchCriteria.Op.EQ);
        HostTemplateSearch.and("destroyed", HostTemplateSearch.entity().getDestroyed(), SearchCriteria.Op.EQ);
        HostTemplateSearch.done();

        HostDestroyedSearch = createSearchBuilder();
        HostDestroyedSearch.and("host_id", HostDestroyedSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        HostDestroyedSearch.and("destroyed", HostDestroyedSearch.entity().getDestroyed(), SearchCriteria.Op.EQ);
        HostDestroyedSearch.done();

        TemplateStatusSearch = createSearchBuilder();
        TemplateStatusSearch.and("template_id", TemplateStatusSearch.entity().getTemplateId(), SearchCriteria.Op.EQ);
        TemplateStatusSearch.and("download_state", TemplateStatusSearch.entity().getDownloadState(), SearchCriteria.Op.EQ);
        TemplateStatusSearch.and("destroyed", TemplateStatusSearch.entity().getDestroyed(), SearchCriteria.Op.EQ);
        TemplateStatusSearch.done();

        TemplateStatesSearch = createSearchBuilder();
        TemplateStatesSearch.and("template_id", TemplateStatesSearch.entity().getTemplateId(), SearchCriteria.Op.EQ);
        TemplateStatesSearch.and("states", TemplateStatesSearch.entity().getDownloadState(), SearchCriteria.Op.IN);
        TemplateStatesSearch.and("destroyed", TemplateStatesSearch.entity().getDestroyed(), SearchCriteria.Op.EQ);
        TemplateStatesSearch.done();

        HostTemplateStateSearch = createSearchBuilder();
        HostTemplateStateSearch.and("template_id", HostTemplateStateSearch.entity().getTemplateId(), SearchCriteria.Op.EQ);
        HostTemplateStateSearch.and("host_id", HostTemplateStateSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        HostTemplateStateSearch.and("states", HostTemplateStateSearch.entity().getDownloadState(), SearchCriteria.Op.IN);
        HostTemplateStateSearch.and("destroyed", HostTemplateStateSearch.entity().getDestroyed(), SearchCriteria.Op.EQ);
        HostTemplateStateSearch.done();

        updateStateSearch = this.createSearchBuilder();
        updateStateSearch.and("id", updateStateSearch.entity().getId(), Op.EQ);
        updateStateSearch.and("state", updateStateSearch.entity().getState(), Op.EQ);
        updateStateSearch.and("updatedCount", updateStateSearch.entity().getUpdatedCount(), Op.EQ);
        updateStateSearch.done();

    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        boolean result = super.configure(name, params);
        ZoneTemplateSearch = createSearchBuilder();
        ZoneTemplateSearch.and("template_id", ZoneTemplateSearch.entity().getTemplateId(), SearchCriteria.Op.EQ);
        ZoneTemplateSearch.and("state", ZoneTemplateSearch.entity().getDownloadState(), SearchCriteria.Op.EQ);
        SearchBuilder<HostVO> hostSearch = _hostDao.createSearchBuilder();
        hostSearch.and("zone_id", hostSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        ZoneTemplateSearch.join("tmplHost", hostSearch, hostSearch.entity().getId(), ZoneTemplateSearch.entity().getHostId(), JoinBuilder.JoinType.INNER);
        ZoneTemplateSearch.done();

        LocalSecondaryStorageSearch = createSearchBuilder();
        LocalSecondaryStorageSearch.and("template_id", LocalSecondaryStorageSearch.entity().getTemplateId(), SearchCriteria.Op.EQ);
        LocalSecondaryStorageSearch.and("state", LocalSecondaryStorageSearch.entity().getDownloadState(), SearchCriteria.Op.EQ);
        SearchBuilder<HostVO> localSecondaryHost = _hostDao.createSearchBuilder();
        localSecondaryHost.and("private_ip_address", localSecondaryHost.entity().getPrivateIpAddress(), SearchCriteria.Op.EQ);
        localSecondaryHost.and("state", localSecondaryHost.entity().getStatus(), SearchCriteria.Op.EQ);
        localSecondaryHost.and("data_center_id", localSecondaryHost.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        localSecondaryHost.and("type", localSecondaryHost.entity().getType(), SearchCriteria.Op.EQ);
        LocalSecondaryStorageSearch.join("host", localSecondaryHost, localSecondaryHost.entity().getId(), LocalSecondaryStorageSearch.entity().getHostId(),
            JoinBuilder.JoinType.INNER);
        LocalSecondaryStorageSearch.done();

        return result;
    }

    @Override
    public void update(VMTemplateHostVO instance) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        PreparedStatement pstmt = null;
        try {
            Date now = new Date();
            String sql = UPDATE_TEMPLATE_HOST_REF;
            pstmt = txn.prepareAutoCloseStatement(sql);
            pstmt.setString(1, instance.getDownloadState().toString());
            pstmt.setInt(2, instance.getDownloadPercent());
            pstmt.setString(3, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), now));
            pstmt.setString(4, instance.getErrorString());
            pstmt.setString(5, instance.getLocalDownloadPath());
            pstmt.setString(6, instance.getJobId());
            pstmt.setLong(7, instance.getHostId());
            pstmt.setLong(8, instance.getTemplateId());
            pstmt.executeUpdate();
        } catch (Exception e) {
            s_logger.warn("Exception: ", e);
        }
    }

    @Override
    public List<VMTemplateHostVO> listByHostId(long id) {
        SearchCriteria<VMTemplateHostVO> sc = HostSearch.create();
        sc.setParameters("host_id", id);
        return listIncludingRemovedBy(sc);
    }

    @Override
    public List<VMTemplateHostVO> listByTemplateId(long templateId) {
        SearchCriteria<VMTemplateHostVO> sc = TemplateSearch.create();
        sc.setParameters("template_id", templateId);
        sc.setParameters("destroyed", false);
        return listIncludingRemovedBy(sc);
    }

    @Override
    public List<VMTemplateHostVO> listByOnlyTemplateId(long templateId) {
        SearchCriteria<VMTemplateHostVO> sc = TemplateSearch.create();
        sc.setParameters("template_id", templateId);
        sc.setParameters("destroyed", false);
        return listIncludingRemovedBy(sc);
    }

    @Override
    public VMTemplateHostVO findByHostTemplate(long hostId, long templateId) {
        SearchCriteria<VMTemplateHostVO> sc = HostTemplateSearch.create();
        sc.setParameters("host_id", hostId);
        sc.setParameters("template_id", templateId);
        sc.setParameters("destroyed", false);
        return findOneIncludingRemovedBy(sc);
    }

    @Override
    public VMTemplateHostVO findByTemplateId(long templateId) {
        SearchCriteria<VMTemplateHostVO> sc = HostTemplateSearch.create();
        sc.setParameters("template_id", templateId);
        sc.setParameters("destroyed", false);
        return findOneIncludingRemovedBy(sc);
    }

    @Override
    public List<VMTemplateHostVO> listByTemplateStatus(long templateId, VMTemplateHostVO.Status downloadState) {
        SearchCriteria<VMTemplateHostVO> sc = TemplateStatusSearch.create();
        sc.setParameters("template_id", templateId);
        sc.setParameters("download_state", downloadState.toString());
        sc.setParameters("destroyed", false);
        return listIncludingRemovedBy(sc);
    }

    @Override
    public List<VMTemplateHostVO> listByTemplateStatus(long templateId, long datacenterId, VMTemplateHostVO.Status downloadState) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        PreparedStatement pstmt = null;
        List<VMTemplateHostVO> result = new ArrayList<VMTemplateHostVO>();
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
    public List<VMTemplateHostVO> listByTemplateHostStatus(long templateId, long hostId, VMTemplateHostVO.Status... states) {
        SearchCriteria<VMTemplateHostVO> sc = HostTemplateStateSearch.create();
        sc.setParameters("template_id", templateId);
        sc.setParameters("host_id", hostId);
        sc.setParameters("states", (Object[])states);
        return search(sc, null);
    }

    @Override
    public List<VMTemplateHostVO> listByTemplateStatus(long templateId, long datacenterId, long podId, VMTemplateHostVO.Status downloadState) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        List<VMTemplateHostVO> result = new ArrayList<VMTemplateHostVO>();
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
            }catch (SQLException e) {
                s_logger.warn("listByTemplateStatus:Exception: "+e.getMessage(), e);
            }
        } catch (Exception e) {
            s_logger.warn("listByTemplateStatus:Exception: "+e.getMessage(), e);
        }
        return result;

    }

    @Override
    public boolean templateAvailable(long templateId, long hostId) {
        VMTemplateHostVO tmpltHost = findByHostTemplate(hostId, templateId);
        if (tmpltHost == null)
            return false;

        return tmpltHost.getDownloadState() == Status.DOWNLOADED;
    }

    @Override
    public List<VMTemplateHostVO> listByTemplateStates(long templateId, VMTemplateHostVO.Status... states) {
        SearchCriteria<VMTemplateHostVO> sc = TemplateStatesSearch.create();
        sc.setParameters("states", (Object[])states);
        sc.setParameters("template_id", templateId);
        sc.setParameters("destroyed", false);
        return search(sc, null);
    }

    @Override
    public List<VMTemplateHostVO> listByState(VMTemplateHostVO.Status state) {
        SearchCriteria<VMTemplateHostVO> sc = createSearchCriteria();
        sc.addAnd("downloadState", SearchCriteria.Op.EQ, state);
        sc.addAnd("destroyed", SearchCriteria.Op.EQ, false);
        return search(sc, null);
    }

    @Override
    public List<VMTemplateHostVO> listByHostTemplate(long hostId, long templateId) {
        SearchCriteria<VMTemplateHostVO> sc = HostTemplateSearch.create();
        sc.setParameters("host_id", hostId);
        sc.setParameters("template_id", templateId);
        sc.setParameters("destroyed", false);
        return listIncludingRemovedBy(sc);
    }

    @Override
    public List<VMTemplateHostVO> listByZoneTemplate(long dcId, long templateId, boolean readyOnly) {
        SearchCriteria<VMTemplateHostVO> sc = ZoneTemplateSearch.create();
        sc.setParameters("template_id", templateId);
        sc.setJoinParameters("tmplHost", "zone_id", dcId);
        if (readyOnly) {
            sc.setParameters("state", VMTemplateHostVO.Status.DOWNLOADED);
        }
        return listBy(sc);
    }

    @Override
    public List<VMTemplateHostVO> listDestroyed(long hostId) {
        SearchCriteria<VMTemplateHostVO> sc = HostDestroyedSearch.create();
        sc.setParameters("host_id", hostId);
        sc.setParameters("destroyed", true);
        return listIncludingRemovedBy(sc);
    }

    @Override
    public VMTemplateHostVO findByHostTemplate(long hostId, long templateId, boolean lock) {
        SearchCriteria<VMTemplateHostVO> sc = HostTemplateSearch.create();
        sc.setParameters("host_id", hostId);
        sc.setParameters("template_id", templateId);
        sc.setParameters("destroyed", false);
        if (!lock)
            return findOneIncludingRemovedBy(sc);
        else
            return lockOneRandomRow(sc, true);
    }

    // Based on computing node host id, and template id, find out the
    // corresponding template_host_ref, assuming local secondary storage and
    // computing node is in the same zone, and private ip
    @Override
    public VMTemplateHostVO findLocalSecondaryStorageByHostTemplate(long hostId, long templateId) {
        HostVO computingHost = _hostDao.findById(hostId);
        SearchCriteria<VMTemplateHostVO> sc = LocalSecondaryStorageSearch.create();
        sc.setJoinParameters("host", "private_ip_address", computingHost.getPrivateIpAddress());
        sc.setJoinParameters("host", "state", com.cloud.host.Status.Up);
        sc.setJoinParameters("host", "data_center_id", computingHost.getDataCenterId());
        sc.setJoinParameters("host", "type", Host.Type.LocalSecondaryStorage);
        sc.setParameters("template_id", templateId);
        sc.setParameters("state", VMTemplateHostVO.Status.DOWNLOADED);
        sc.setParameters("destroyed", false);
        return findOneBy(sc);
    }

    @Override
    public void deleteByHost(Long hostId) {
        List<VMTemplateHostVO> tmpltHosts = listByHostId(hostId);
        for (VMTemplateHostVO tmpltHost : tmpltHosts) {
            remove(tmpltHost.getId());
        }
    }

    @Override
    public boolean updateState(State currentState, Event event, State nextState, DataObjectInStore vo, Object data) {
        VMTemplateHostVO templateHost = (VMTemplateHostVO)vo;
        Long oldUpdated = templateHost.getUpdatedCount();
        Date oldUpdatedTime = templateHost.getUpdated();

        SearchCriteria<VMTemplateHostVO> sc = updateStateSearch.create();
        sc.setParameters("id", templateHost.getId());
        sc.setParameters("state", currentState);
        sc.setParameters("updatedCount", templateHost.getUpdatedCount());

        templateHost.incrUpdatedCount();

        UpdateBuilder builder = getUpdateBuilder(vo);
        builder.set(vo, "state", nextState);
        builder.set(vo, "updated", new Date());

        int rows = update((VMTemplateHostVO)vo, sc);
        if (rows == 0 && s_logger.isDebugEnabled()) {
            VMTemplateHostVO dbVol = findByIdIncludingRemoved(templateHost.getId());
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
                    .append(templateHost.getId())
                    .append("; state=")
                    .append(nextState)
                    .append("; event=")
                    .append(event)
                    .append("; updatecount=")
                    .append(templateHost.getUpdatedCount())
                    .append("; updatedTime=")
                    .append(templateHost.getUpdated());
                str.append(": stale Data={id=")
                    .append(templateHost.getId())
                    .append("; state=")
                    .append(currentState)
                    .append("; event=")
                    .append(event)
                    .append("; updatecount=")
                    .append(oldUpdated)
                    .append("; updatedTime=")
                    .append(oldUpdatedTime);
            } else {
                s_logger.debug("Unable to update objectIndatastore: id=" + templateHost.getId() + ", as there is no such object exists in the database anymore");
            }
        }
        return rows > 0;
    }

}
