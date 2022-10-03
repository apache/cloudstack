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
package org.apache.cloudstack.storage.image.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectInStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.Event;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.State;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.UpdateBuilder;

@Component
public class SnapshotDataStoreDaoImpl extends GenericDaoBase<SnapshotDataStoreVO, Long> implements SnapshotDataStoreDao {
    private static final Logger s_logger = Logger.getLogger(SnapshotDataStoreDaoImpl.class);
    private static final String STORE_ID = "store_id";
    private static final String STORE_ROLE = "store_role";
    private static final String STATE = "state";
    private static final String REF_CNT = "ref_cnt";
    private static final String ID = "id";
    private static final String UPDATED_COUNT = "updatedCount";
    private static final String SNAPSHOT_ID = "snapshot_id";
    private static final String VOLUME_ID = "volume_id";
    private static final String CREATED = "created";
    private SearchBuilder<SnapshotDataStoreVO> searchFilteringStoreIdEqStoreRoleEqStateNeqRefCntNeq;
    protected SearchBuilder<SnapshotDataStoreVO> searchFilteringStoreIdEqStateEqStoreRoleEqIdEqUpdateCountEqSnapshotIdEqVolumeIdEq;
    private SearchBuilder<SnapshotDataStoreVO> stateSearch;
    protected SearchBuilder<SnapshotVO> snapshotVOSearch;
    private SearchBuilder<SnapshotDataStoreVO> snapshotCreatedSearch;

    protected static final List<Hypervisor.HypervisorType> HYPERVISORS_SUPPORTING_SNAPSHOTS_CHAINING = List.of(Hypervisor.HypervisorType.XenServer);

    @Inject
    protected SnapshotDao snapshotDao;

    private static final String FIND_OLDEST_OR_LATEST_SNAPSHOT = "select store_id, store_role, snapshot_id from cloud.snapshot_store_ref where " +
            " store_role = ? and volume_id = ? and state = 'Ready'" +
            " order by created %s " +
            " limit 1";

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        searchFilteringStoreIdEqStoreRoleEqStateNeqRefCntNeq = createSearchBuilder();
        searchFilteringStoreIdEqStoreRoleEqStateNeqRefCntNeq.and(STORE_ID, searchFilteringStoreIdEqStoreRoleEqStateNeqRefCntNeq.entity().getDataStoreId(), SearchCriteria.Op.EQ);
        searchFilteringStoreIdEqStoreRoleEqStateNeqRefCntNeq.and(STORE_ROLE, searchFilteringStoreIdEqStoreRoleEqStateNeqRefCntNeq.entity().getRole(), SearchCriteria.Op.EQ);
        searchFilteringStoreIdEqStoreRoleEqStateNeqRefCntNeq.and(STATE, searchFilteringStoreIdEqStoreRoleEqStateNeqRefCntNeq.entity().getState(), SearchCriteria.Op.NEQ);
        searchFilteringStoreIdEqStoreRoleEqStateNeqRefCntNeq.and(REF_CNT, searchFilteringStoreIdEqStoreRoleEqStateNeqRefCntNeq.entity().getRefCnt(), SearchCriteria.Op.NEQ);
        searchFilteringStoreIdEqStoreRoleEqStateNeqRefCntNeq.done();

        searchFilteringStoreIdEqStateEqStoreRoleEqIdEqUpdateCountEqSnapshotIdEqVolumeIdEq = createSearchBuilder();
        searchFilteringStoreIdEqStateEqStoreRoleEqIdEqUpdateCountEqSnapshotIdEqVolumeIdEq.and(STORE_ID,
            searchFilteringStoreIdEqStateEqStoreRoleEqIdEqUpdateCountEqSnapshotIdEqVolumeIdEq.entity().getDataStoreId(), SearchCriteria.Op.EQ);

        searchFilteringStoreIdEqStateEqStoreRoleEqIdEqUpdateCountEqSnapshotIdEqVolumeIdEq.and(STATE,
            searchFilteringStoreIdEqStateEqStoreRoleEqIdEqUpdateCountEqSnapshotIdEqVolumeIdEq.entity().getState(), SearchCriteria.Op.EQ);

        searchFilteringStoreIdEqStateEqStoreRoleEqIdEqUpdateCountEqSnapshotIdEqVolumeIdEq.and(STORE_ROLE,
            searchFilteringStoreIdEqStateEqStoreRoleEqIdEqUpdateCountEqSnapshotIdEqVolumeIdEq.entity().getRole(), SearchCriteria.Op.EQ);

        searchFilteringStoreIdEqStateEqStoreRoleEqIdEqUpdateCountEqSnapshotIdEqVolumeIdEq.and(ID,
            searchFilteringStoreIdEqStateEqStoreRoleEqIdEqUpdateCountEqSnapshotIdEqVolumeIdEq.entity().getId(), SearchCriteria.Op.EQ);

        searchFilteringStoreIdEqStateEqStoreRoleEqIdEqUpdateCountEqSnapshotIdEqVolumeIdEq.and(UPDATED_COUNT,
            searchFilteringStoreIdEqStateEqStoreRoleEqIdEqUpdateCountEqSnapshotIdEqVolumeIdEq.entity().getUpdatedCount(), SearchCriteria.Op.EQ);

        searchFilteringStoreIdEqStateEqStoreRoleEqIdEqUpdateCountEqSnapshotIdEqVolumeIdEq.and(SNAPSHOT_ID,
            searchFilteringStoreIdEqStateEqStoreRoleEqIdEqUpdateCountEqSnapshotIdEqVolumeIdEq.entity().getSnapshotId(), SearchCriteria.Op.EQ);

        searchFilteringStoreIdEqStateEqStoreRoleEqIdEqUpdateCountEqSnapshotIdEqVolumeIdEq.and(VOLUME_ID,
            searchFilteringStoreIdEqStateEqStoreRoleEqIdEqUpdateCountEqSnapshotIdEqVolumeIdEq.entity().getVolumeId(), SearchCriteria.Op.EQ);

        searchFilteringStoreIdEqStateEqStoreRoleEqIdEqUpdateCountEqSnapshotIdEqVolumeIdEq.done();


        stateSearch = createSearchBuilder();
        stateSearch.and(STATE, stateSearch.entity().getState(), SearchCriteria.Op.IN);
        stateSearch.done();

        snapshotVOSearch = snapshotDao.createSearchBuilder();
        snapshotVOSearch.and(VOLUME_ID, snapshotVOSearch.entity().getVolumeId(), SearchCriteria.Op.EQ);
        snapshotVOSearch.done();

        snapshotCreatedSearch = createSearchBuilder();
        snapshotCreatedSearch.and(STORE_ID, snapshotCreatedSearch.entity().getDataStoreId(), SearchCriteria.Op.EQ);
        snapshotCreatedSearch.and(CREATED,  snapshotCreatedSearch.entity().getCreated(), SearchCriteria.Op.BETWEEN);
        snapshotCreatedSearch.done();

        return true;
    }

    @Override
    public boolean updateState(State currentState, Event event, State nextState, DataObjectInStore vo, Object data) {
        SnapshotDataStoreVO dataObj = (SnapshotDataStoreVO)vo;
        Long oldUpdated = dataObj.getUpdatedCount();
        Date oldUpdatedTime = dataObj.getUpdated();

        SearchCriteria<SnapshotDataStoreVO> sc = searchFilteringStoreIdEqStateEqStoreRoleEqIdEqUpdateCountEqSnapshotIdEqVolumeIdEq.create();
        sc.setParameters(ID, dataObj.getId());
        sc.setParameters(STATE, currentState);
        sc.setParameters(UPDATED_COUNT, dataObj.getUpdatedCount());

        dataObj.incrUpdatedCount();

        UpdateBuilder builder = getUpdateBuilder(dataObj);
        builder.set(dataObj, STATE, nextState);
        builder.set(dataObj, "updated", new Date());

        if (update(dataObj, sc) > 0) {
            return true;
        }

        SnapshotDataStoreVO dbVol = findByIdIncludingRemoved(dataObj.getId());
        String message;
        if (dbVol != null) {
            message = String.format("Unable to update %s: DB Data={id=%s; state=%s; updatecount=%s;updatedTime=%s: New Data={id=%s; state=%s; event=%s; updatecount=%s; " +
                            "updatedTime=%s: stale Data={id=%s; state=%s; event=%s; updatecount=%s; updatedTime=%s", dataObj, dbVol.getId(), dbVol.getState(),
                            dbVol.getUpdatedCount(), dbVol.getUpdated(), dataObj.getId(), event, nextState, dataObj.getUpdatedCount(), dataObj.getUpdated(), dataObj.getId(),
                            currentState, event, oldUpdated, oldUpdatedTime);
        } else {
            message = String.format("Unable to update objectIndatastore: id=%s, as there is no such object exists in the database anymore", dataObj.getId());
        }

        s_logger.debug(message);
        return false;
    }

    @Override
    public List<SnapshotDataStoreVO> listByStoreId(long id, DataStoreRole role) {
        SearchCriteria<SnapshotDataStoreVO> sc = searchFilteringStoreIdEqStoreRoleEqStateNeqRefCntNeq.create();
        sc.setParameters(STORE_ID, id);
        sc.setParameters(STORE_ROLE, role);
        sc.setParameters(STATE, ObjectInDataStoreStateMachine.State.Destroyed);
        return listBy(sc);
    }

    @Override
    public List<SnapshotDataStoreVO> listByStoreIdAndState(long id, ObjectInDataStoreStateMachine.State state) {
        SearchCriteria<SnapshotDataStoreVO> sc = searchFilteringStoreIdEqStateEqStoreRoleEqIdEqUpdateCountEqSnapshotIdEqVolumeIdEq.create();
        sc.setParameters(STORE_ID, id);
        sc.setParameters(STATE, state);
        return listBy(sc);
    }

    @Override
    public void deletePrimaryRecordsForStore(long id, DataStoreRole role) {
        SearchCriteria<SnapshotDataStoreVO> sc = searchFilteringStoreIdEqStoreRoleEqStateNeqRefCntNeq.create();
        sc.setParameters(STORE_ID, id);
        sc.setParameters(STORE_ROLE, role);
        remove(sc);
    }

    @Override
    public void deleteSnapshotRecordsOnPrimary() {
        SearchCriteria<SnapshotDataStoreVO> sc = searchFilteringStoreIdEqStoreRoleEqStateNeqRefCntNeq.create();
        sc.setParameters(STORE_ROLE, DataStoreRole.Primary);
        remove(sc);
    }

    @Override
    public SnapshotDataStoreVO findByStoreSnapshot(DataStoreRole role, long storeId, long snapshotId) {
        SearchCriteria<SnapshotDataStoreVO> sc = searchFilteringStoreIdEqStateEqStoreRoleEqIdEqUpdateCountEqSnapshotIdEqVolumeIdEq.create();
        sc.setParameters(STORE_ID, storeId);
        sc.setParameters(SNAPSHOT_ID, snapshotId);
        sc.setParameters(STORE_ROLE, role);
        return findOneBy(sc);
    }

    @Override
    public SnapshotDataStoreVO findLatestSnapshotForVolume(Long volumeId, DataStoreRole role) {
        return findOldestOrLatestSnapshotForVolume(volumeId, role, false);
    }

    @Override
    public SnapshotDataStoreVO findOldestSnapshotForVolume(Long volumeId, DataStoreRole role) {
        return findOldestOrLatestSnapshotForVolume(volumeId, role, true);
    }

    protected SnapshotDataStoreVO findOldestOrLatestSnapshotForVolume(long volumeId, DataStoreRole role, boolean oldest) {
        String order = oldest ? "ASC" : "DESC";

        try (TransactionLegacy transactionLegacy = TransactionLegacy.currentTxn()) {
            try (PreparedStatement preparedStatement = transactionLegacy.prepareStatement(String.format(FIND_OLDEST_OR_LATEST_SNAPSHOT, order))) {
                preparedStatement.setString(1, role.toString());
                preparedStatement.setLong(2, volumeId);

                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        long storeId = resultSet.getLong(1);
                        long snapshotId = resultSet.getLong(3);
                        return findByStoreSnapshot(role, storeId, snapshotId);
                    }
                }
            }
        } catch (SQLException e) {
            s_logger.warn(String.format("Failed to find %s snapshot for volume [%s] in %s store due to [%s].", oldest ? "oldest" : "latest", volumeId, role, e.getMessage()), e);
        }
        return null;
    }

    @Override
    @DB
    public SnapshotDataStoreVO findParent(DataStoreRole role, Long storeId, Long volumeId) {
        if (!isSnapshotChainingRequired(volumeId)) {
            s_logger.trace(String.format("Snapshot chaining is not required for snapshots of volume [%s]. Returning null as parent.", volumeId));
            return null;
        }

        SearchCriteria<SnapshotDataStoreVO> sc = searchFilteringStoreIdEqStateEqStoreRoleEqIdEqUpdateCountEqSnapshotIdEqVolumeIdEq.create();
        sc.setParameters(VOLUME_ID, volumeId);
        sc.setParameters(STORE_ROLE, role.toString());
        sc.setParameters(STATE, ObjectInDataStoreStateMachine.State.Ready.name());
        sc.setParameters(STORE_ID, storeId);

        List<SnapshotDataStoreVO> snapshotList = listBy(sc, new Filter(SnapshotDataStoreVO.class, CREATED, false, null, null));
        if (CollectionUtils.isNotEmpty(snapshotList)) {
            return snapshotList.get(0);
        }
        return null;
    }

    @Override
    public SnapshotDataStoreVO findBySnapshot(long snapshotId, DataStoreRole role) {
        SearchCriteria<SnapshotDataStoreVO> sc = createSearchCriteriaBySnapshotIdAndStoreRole(snapshotId, role);
        sc.setParameters(STATE, State.Ready);
        return findOneBy(sc);
    }

    @Override
    public SnapshotDataStoreVO findBySourceSnapshot(long snapshotId, DataStoreRole role) {
        SearchCriteria<SnapshotDataStoreVO> sc = createSearchCriteriaBySnapshotIdAndStoreRole(snapshotId, role);
        sc.setParameters(STATE, State.Migrating);
        return findOneBy(sc);
    }

    @Override
    public List<SnapshotDataStoreVO> listAllByVolumeAndDataStore(long volumeId, DataStoreRole role) {
        SearchCriteria<SnapshotDataStoreVO> sc = searchFilteringStoreIdEqStateEqStoreRoleEqIdEqUpdateCountEqSnapshotIdEqVolumeIdEq.create();
        sc.setParameters(VOLUME_ID, volumeId);
        sc.setParameters(STORE_ROLE, role);
        return listBy(sc);
    }

    @Override
    public SnapshotDataStoreVO findByVolume(long volumeId, DataStoreRole role) {
        SearchCriteria<SnapshotDataStoreVO> sc = searchFilteringStoreIdEqStateEqStoreRoleEqIdEqUpdateCountEqSnapshotIdEqVolumeIdEq.create();
        sc.setParameters(VOLUME_ID, volumeId);
        sc.setParameters(STORE_ROLE, role);
        return findOneBy(sc);
    }

    @Override
    public SnapshotDataStoreVO findByVolume(long snapshotId, long volumeId, DataStoreRole role) {
        SearchCriteria<SnapshotDataStoreVO> sc = searchFilteringStoreIdEqStateEqStoreRoleEqIdEqUpdateCountEqSnapshotIdEqVolumeIdEq.create();
        sc.setParameters(SNAPSHOT_ID, snapshotId);
        sc.setParameters(VOLUME_ID, volumeId);
        sc.setParameters(STORE_ROLE, role);
        return findOneBy(sc);
    }

    @Override
    public List<SnapshotDataStoreVO> findBySnapshotId(long snapshotId) {
        SearchCriteria<SnapshotDataStoreVO> sc = searchFilteringStoreIdEqStateEqStoreRoleEqIdEqUpdateCountEqSnapshotIdEqVolumeIdEq.create();
        sc.setParameters(SNAPSHOT_ID, snapshotId);
        return listBy(sc);
    }

    @Override
    public List<SnapshotDataStoreVO> listDestroyed(long id) {
        SearchCriteria<SnapshotDataStoreVO> sc = searchFilteringStoreIdEqStateEqStoreRoleEqIdEqUpdateCountEqSnapshotIdEqVolumeIdEq.create();
        sc.setParameters(STORE_ID, id);
        sc.setParameters(STORE_ROLE, DataStoreRole.Image);
        sc.setParameters(STATE, ObjectInDataStoreStateMachine.State.Destroyed);
        return listBy(sc);
    }

    @Override
    public List<SnapshotDataStoreVO> listActiveOnCache(long id) {
        SearchCriteria<SnapshotDataStoreVO> sc = searchFilteringStoreIdEqStoreRoleEqStateNeqRefCntNeq.create();
        sc.setParameters(STORE_ID, id);
        sc.setParameters(STORE_ROLE, DataStoreRole.ImageCache);
        sc.setParameters(STATE, ObjectInDataStoreStateMachine.State.Destroyed);
        sc.setParameters(REF_CNT, 0);
        return listBy(sc);
    }

    /**
     * Creates an entry for each record, but with empty install path since the content is not on region-wide store yet.
     */
    @Override
    public void duplicateCacheRecordsOnRegionStore(long storeId) {
        SearchCriteria<SnapshotDataStoreVO> sc = searchFilteringStoreIdEqStateEqStoreRoleEqIdEqUpdateCountEqSnapshotIdEqVolumeIdEq.create();
        sc.setParameters(STORE_ROLE, DataStoreRole.ImageCache);
        sc.setParameters("destroyed", false);
        List<SnapshotDataStoreVO> snapshots = listBy(sc);

        if (snapshots == null) {
            s_logger.debug(String.format("There are no snapshots on cache store to duplicate to region store [%s].", storeId));
            return;
        }

        s_logger.info(String.format("Duplicating [%s] snapshot cache store records to region store [%s].", snapshots.size(), storeId));

        for (SnapshotDataStoreVO snap : snapshots) {
            SnapshotDataStoreVO snapStore = findByStoreSnapshot(DataStoreRole.Image, storeId, snap.getSnapshotId());

            if (snapStore != null) {
                s_logger.debug(String.format("There is already an entry for snapshot [%s] on region store [%s].", snap.getSnapshotId(), storeId));
                continue;
            }

            s_logger.info(String.format("Persisting an entry for snapshot [%s] on region store [%s].", snap.getSnapshotId(), storeId));
            SnapshotDataStoreVO ss = new SnapshotDataStoreVO();
            ss.setSnapshotId(snap.getSnapshotId());
            ss.setDataStoreId(storeId);
            ss.setRole(DataStoreRole.Image);
            ss.setVolumeId(snap.getVolumeId());
            ss.setParentSnapshotId(snap.getParentSnapshotId());
            ss.setState(snap.getState());
            ss.setSize(snap.getSize());
            ss.setPhysicalSize(snap.getPhysicalSize());
            ss.setRefCnt(snap.getRefCnt());
            persist(ss);

            snap.incrRefCnt();
            update(snap.getId(), snap);
        }
    }

    @Override
    public SnapshotDataStoreVO findReadyOnCache(long snapshotId) {
        SearchCriteria<SnapshotDataStoreVO> sc = searchFilteringStoreIdEqStateEqStoreRoleEqIdEqUpdateCountEqSnapshotIdEqVolumeIdEq.create();
        sc.setParameters(SNAPSHOT_ID, snapshotId);
        sc.setParameters(STORE_ROLE, DataStoreRole.ImageCache);
        sc.setParameters(STATE, ObjectInDataStoreStateMachine.State.Ready);
        return findOneIncludingRemovedBy(sc);
    }

    @Override
    public List<SnapshotDataStoreVO> listOnCache(long snapshotId) {
        SearchCriteria<SnapshotDataStoreVO> sc = searchFilteringStoreIdEqStateEqStoreRoleEqIdEqUpdateCountEqSnapshotIdEqVolumeIdEq.create();
        sc.setParameters(SNAPSHOT_ID, snapshotId);
        sc.setParameters(STORE_ROLE, DataStoreRole.ImageCache);
        return search(sc, null);
    }

    @Override
    public void updateStoreRoleToCache(long storeId) {
        SearchCriteria<SnapshotDataStoreVO> sc = searchFilteringStoreIdEqStoreRoleEqStateNeqRefCntNeq.create();
        sc.setParameters(STORE_ID, storeId);
        sc.setParameters("destroyed", false);
        List<SnapshotDataStoreVO> snaps = listBy(sc);
        if (snaps != null) {
            s_logger.info(String.format("Updating role to cache store for [%s] entries in snapshot_store_ref.", snaps.size()));
            for (SnapshotDataStoreVO snap : snaps) {
                s_logger.debug(String.format("Updating role to cache store for entry [%s].", snap));
                snap.setRole(DataStoreRole.ImageCache);
                update(snap.getId(), snap);
            }
        }
    }

    @Override
    public void updateVolumeIds(long oldVolId, long newVolId) {
        SearchCriteria<SnapshotDataStoreVO> sc = searchFilteringStoreIdEqStateEqStoreRoleEqIdEqUpdateCountEqSnapshotIdEqVolumeIdEq.create();
        sc.setParameters(VOLUME_ID, oldVolId);
        SnapshotDataStoreVO snapshot = createForUpdate();
        snapshot.setVolumeId(newVolId);
        UpdateBuilder ub = getUpdateBuilder(snapshot);
        update(ub, sc, null);
    }

    @Override
    public List<SnapshotDataStoreVO> listByState(ObjectInDataStoreStateMachine.State... states) {
        SearchCriteria<SnapshotDataStoreVO> sc = stateSearch.create();
        sc.setParameters(STATE, (Object[])states);
        return listBy(sc, null);
    }

    @Override
    public List<SnapshotDataStoreVO> findSnapshots(Long storeId, Date start, Date end) {
        SearchCriteria<SnapshotDataStoreVO> sc = snapshotCreatedSearch.create();
        sc.setParameters(STORE_ID, storeId);
        if (start != null && end != null) {
            sc.setParameters(CREATED, start, end);
        }
        return search(sc, null);
    }

    public SnapshotDataStoreVO findDestroyedReferenceBySnapshot(long snapshotId, DataStoreRole role) {
        SearchCriteria<SnapshotDataStoreVO> sc = createSearchCriteriaBySnapshotIdAndStoreRole(snapshotId, role);
        sc.setParameters(STATE, State.Destroyed);
        return findOneBy(sc);
    }

    /**
     * Creates a SearchCriteria with snapshot id and data store role.
     * @return A SearchCriteria with snapshot id and data store role
     */
    protected SearchCriteria<SnapshotDataStoreVO> createSearchCriteriaBySnapshotIdAndStoreRole(long snapshotId, DataStoreRole role) {
        SearchCriteria<SnapshotDataStoreVO> sc = searchFilteringStoreIdEqStateEqStoreRoleEqIdEqUpdateCountEqSnapshotIdEqVolumeIdEq.create();
        sc.setParameters(SNAPSHOT_ID, snapshotId);
        sc.setParameters(STORE_ROLE, role);
        return sc;
    }

    protected boolean isSnapshotChainingRequired(long volumeId) {
        SearchCriteria<SnapshotVO> sc = snapshotVOSearch.create();
        sc.setParameters(VOLUME_ID, volumeId);

        SnapshotVO snapshot = snapshotDao.findOneBy(sc);

        return snapshot != null && HYPERVISORS_SUPPORTING_SNAPSHOTS_CHAINING.contains(snapshot.getHypervisorType());
    }

    @Override
    public boolean expungeReferenceBySnapshotIdAndDataStoreRole(long snapshotId, DataStoreRole dataStoreRole) {
        SnapshotDataStoreVO snapshotDataStoreVo = findOneBy(createSearchCriteriaBySnapshotIdAndStoreRole(snapshotId, dataStoreRole));
        return snapshotDataStoreVo == null || expunge(snapshotDataStoreVo.getId());
    }

    @Override
    public List<SnapshotDataStoreVO> listReadyByVolumeId(long volumeId) {
        SearchCriteria<SnapshotDataStoreVO> sc = searchFilteringStoreIdEqStateEqStoreRoleEqIdEqUpdateCountEqSnapshotIdEqVolumeIdEq.create();
        sc.setParameters(VOLUME_ID, volumeId);
        sc.setParameters(STATE, State.Ready);
        return listBy(sc);
    }
}
