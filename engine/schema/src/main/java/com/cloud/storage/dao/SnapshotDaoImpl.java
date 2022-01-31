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
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.server.ResourceTag.ResourceObjectType;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Snapshot;
import com.cloud.storage.Snapshot.Event;
import com.cloud.storage.Snapshot.State;
import com.cloud.storage.Snapshot.Type;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.JoinBuilder.JoinType;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.UpdateBuilder;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;

@Component
public class SnapshotDaoImpl extends GenericDaoBase<SnapshotVO, Long> implements SnapshotDao {
    public static final Logger s_logger = Logger.getLogger(SnapshotDaoImpl.class.getName());
    // TODO: we should remove these direct sqls
    private static final String GET_LAST_SNAPSHOT =
        "SELECT snapshots.id FROM snapshot_store_ref, snapshots where snapshots.id = snapshot_store_ref.snapshot_id AND snapshosts.volume_id = ? AND snapshot_store_ref.role = ? ORDER BY created DESC";

    private SearchBuilder<SnapshotVO> snapshotIdsSearch;
    private SearchBuilder<SnapshotVO> VolumeIdSearch;
    private SearchBuilder<SnapshotVO> VolumeIdTypeSearch;
    private SearchBuilder<SnapshotVO> VolumeIdTypeNotDestroyedSearch;
    private SearchBuilder<SnapshotVO> VolumeIdVersionSearch;
    private SearchBuilder<SnapshotVO> AccountIdSearch;
    private SearchBuilder<SnapshotVO> InstanceIdSearch;
    private SearchBuilder<SnapshotVO> StatusSearch;
    private SearchBuilder<SnapshotVO> notInStatusSearch;
    private GenericSearchBuilder<SnapshotVO, Long> CountSnapshotsByAccount;
    @Inject
    ResourceTagDao _tagsDao;
    @Inject
    protected VMInstanceDao _instanceDao;
    @Inject
    protected VolumeDao _volumeDao;

    @Override
    public List<SnapshotVO> listByVolumeIdTypeNotDestroyed(long volumeId, Type type) {
        SearchCriteria<SnapshotVO> sc = VolumeIdTypeNotDestroyedSearch.create();
        sc.setParameters("volumeId", volumeId);
        sc.setParameters("type", type.ordinal());
        sc.setParameters("status", State.Destroyed);
        return listBy(sc, null);
    }

    @Override
    public List<SnapshotVO> listByVolumeIdVersion(long volumeId, String version) {
        return listByVolumeIdVersion(null, volumeId, version);
    }

    @Override
    public List<SnapshotVO> listByVolumeId(long volumeId) {
        return listByVolumeId(null, volumeId);
    }

    @Override
    public List<SnapshotVO> listByVolumeId(Filter filter, long volumeId) {
        SearchCriteria<SnapshotVO> sc = VolumeIdSearch.create();
        sc.setParameters("volumeId", volumeId);
        return listBy(sc, filter);
    }

    @Override
    public List<SnapshotVO> listByVolumeIdIncludingRemoved(long volumeId) {
        SearchCriteria<SnapshotVO> sc = VolumeIdSearch.create();
        sc.setParameters("volumeId", volumeId);
        return listIncludingRemovedBy(sc, null);
    }

    public List<SnapshotVO> listByVolumeIdType(Filter filter, long volumeId, Type type) {
        SearchCriteria<SnapshotVO> sc = VolumeIdTypeSearch.create();
        sc.setParameters("volumeId", volumeId);
        sc.setParameters("type", type.ordinal());
        return listBy(sc, filter);
    }

    public List<SnapshotVO> listByVolumeIdVersion(Filter filter, long volumeId, String version) {
        SearchCriteria<SnapshotVO> sc = VolumeIdVersionSearch.create();
        sc.setParameters("volumeId", volumeId);
        sc.setParameters("version", version);
        return listBy(sc, filter);
    }

    public SnapshotDaoImpl() {
    }

    @PostConstruct
    protected void init() {
        VolumeIdSearch = createSearchBuilder();
        VolumeIdSearch.and("volumeId", VolumeIdSearch.entity().getVolumeId(), SearchCriteria.Op.EQ);
        VolumeIdSearch.done();

        VolumeIdTypeSearch = createSearchBuilder();
        VolumeIdTypeSearch.and("volumeId", VolumeIdTypeSearch.entity().getVolumeId(), SearchCriteria.Op.EQ);
        VolumeIdTypeSearch.and("type", VolumeIdTypeSearch.entity().getSnapshotType(), SearchCriteria.Op.EQ);
        VolumeIdTypeSearch.done();

        VolumeIdTypeNotDestroyedSearch = createSearchBuilder();
        VolumeIdTypeNotDestroyedSearch.and("volumeId", VolumeIdTypeNotDestroyedSearch.entity().getVolumeId(), SearchCriteria.Op.EQ);
        VolumeIdTypeNotDestroyedSearch.and("type", VolumeIdTypeNotDestroyedSearch.entity().getSnapshotType(), SearchCriteria.Op.EQ);
        VolumeIdTypeNotDestroyedSearch.and("status", VolumeIdTypeNotDestroyedSearch.entity().getState(), SearchCriteria.Op.NEQ);
        VolumeIdTypeNotDestroyedSearch.done();

        VolumeIdVersionSearch = createSearchBuilder();
        VolumeIdVersionSearch.and("volumeId", VolumeIdVersionSearch.entity().getVolumeId(), SearchCriteria.Op.EQ);
        VolumeIdVersionSearch.and("version", VolumeIdVersionSearch.entity().getVersion(), SearchCriteria.Op.EQ);
        VolumeIdVersionSearch.done();

        AccountIdSearch = createSearchBuilder();
        AccountIdSearch.and("accountId", AccountIdSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountIdSearch.done();

        StatusSearch = createSearchBuilder();
        StatusSearch.and("volumeId", StatusSearch.entity().getVolumeId(), SearchCriteria.Op.EQ);
        StatusSearch.and("status", StatusSearch.entity().getState(), SearchCriteria.Op.IN);
        StatusSearch.done();

        notInStatusSearch  = createSearchBuilder();
        notInStatusSearch.and("volumeId", notInStatusSearch.entity().getVolumeId(), SearchCriteria.Op.EQ);
        notInStatusSearch.and("status", notInStatusSearch.entity().getState(), SearchCriteria.Op.NOTIN);
        notInStatusSearch.done();

        CountSnapshotsByAccount = createSearchBuilder(Long.class);
        CountSnapshotsByAccount.select(null, Func.COUNT, null);
        CountSnapshotsByAccount.and("account", CountSnapshotsByAccount.entity().getAccountId(), SearchCriteria.Op.EQ);
        CountSnapshotsByAccount.and("status", CountSnapshotsByAccount.entity().getState(), SearchCriteria.Op.NIN);
        CountSnapshotsByAccount.and("removed", CountSnapshotsByAccount.entity().getRemoved(), SearchCriteria.Op.NULL);
        CountSnapshotsByAccount.done();

        InstanceIdSearch = createSearchBuilder();
        InstanceIdSearch.and("status", InstanceIdSearch.entity().getState(), SearchCriteria.Op.IN);

        snapshotIdsSearch = createSearchBuilder();
        snapshotIdsSearch.and("id", snapshotIdsSearch.entity().getId(), SearchCriteria.Op.IN);

        SearchBuilder<VMInstanceVO> instanceSearch = _instanceDao.createSearchBuilder();
        instanceSearch.and("instanceId", instanceSearch.entity().getId(), SearchCriteria.Op.EQ);

        SearchBuilder<VolumeVO> volumeSearch = _volumeDao.createSearchBuilder();
        volumeSearch.and("state", volumeSearch.entity().getState(), SearchCriteria.Op.EQ);
        volumeSearch.join("instanceVolumes", instanceSearch, instanceSearch.entity().getId(), volumeSearch.entity().getInstanceId(), JoinType.INNER);

        InstanceIdSearch.join("instanceSnapshots", volumeSearch, volumeSearch.entity().getId(), InstanceIdSearch.entity().getVolumeId(), JoinType.INNER);
        InstanceIdSearch.done();
    }

    @Override
    public long getLastSnapshot(long volumeId, DataStoreRole role) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        PreparedStatement pstmt = null;
        String sql = GET_LAST_SNAPSHOT;
        try {
            pstmt = txn.prepareAutoCloseStatement(sql);
            pstmt.setLong(1, volumeId);
            pstmt.setString(2, role.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (Exception ex) {
            s_logger.error("error getting last snapshot", ex);
        }
        return 0;
    }

    @Override
    public Long countSnapshotsForAccount(long accountId) {
        SearchCriteria<Long> sc = CountSnapshotsByAccount.create();
        sc.setParameters("account", accountId);
        sc.setParameters("status", State.Error, State.Destroyed);
        return customSearch(sc, null).get(0);
    }

    @Override
    public List<SnapshotVO> listByInstanceId(long instanceId, Snapshot.State... status) {
        SearchCriteria<SnapshotVO> sc = InstanceIdSearch.create();

        if (status != null && status.length != 0) {
            sc.setParameters("status", (Object[])status);
        }

        sc.setJoinParameters("instanceSnapshots", "state", Volume.State.Ready);
        sc.setJoinParameters("instanceVolumes", "instanceId", instanceId);
        return listBy(sc, null);
    }

    @Override
    public List<SnapshotVO> listByStatus(long volumeId, Snapshot.State... status) {
        SearchCriteria<SnapshotVO> sc = StatusSearch.create();
        sc.setParameters("volumeId", volumeId);
        sc.setParameters("status", (Object[])status);
        return listBy(sc, null);
    }

    @Override
    @DB
    public boolean remove(Long id) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        SnapshotVO entry = findById(id);
        if (entry != null) {
            _tagsDao.removeByIdAndType(id, ResourceObjectType.Snapshot);
        }
        boolean result = super.remove(id);
        txn.commit();
        return result;
    }

    @Override
    public List<SnapshotVO> listAllByStatus(Snapshot.State... status) {
        SearchCriteria<SnapshotVO> sc = StatusSearch.create();
        sc.setParameters("status", (Object[])status);
        return listBy(sc, null);
    }

    @Override
    public List<SnapshotVO> listByIds(Object... ids) {
        SearchCriteria<SnapshotVO> sc = snapshotIdsSearch.create();
        sc.setParameters("id", ids);
        return listBy(sc, null);
    }

    @Override
    public boolean updateState(State currentState, Event event, State nextState, SnapshotVO snapshot, Object data) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        SnapshotVO snapshotVO = snapshot;
        snapshotVO.setState(nextState);
        super.update(snapshotVO.getId(), snapshotVO);
        txn.commit();
        return true;
    }

    @Override
    public void updateVolumeIds(long oldVolId, long newVolId) {
        SearchCriteria<SnapshotVO> sc = VolumeIdSearch.create();
        sc.setParameters("volumeId", oldVolId);
        SnapshotVO snapshot = createForUpdate();
        snapshot.setVolumeId(newVolId);
        UpdateBuilder ub = getUpdateBuilder(snapshot);
        update(ub, sc, null);
    }

    @Override
    public List<SnapshotVO> listByStatusNotIn(long volumeId, Snapshot.State... status) {
        SearchCriteria<SnapshotVO> sc = this.notInStatusSearch.create();
        sc.setParameters("volumeId", volumeId);
        sc.setParameters("status", (Object[]) status);
        return listBy(sc, null);
    }
}
