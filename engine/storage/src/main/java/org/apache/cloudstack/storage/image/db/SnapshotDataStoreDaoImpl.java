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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import com.cloud.storage.DataStoreRole;
import com.cloud.storage.SnapshotVO;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectInStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.Event;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.State;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.UpdateBuilder;

@Component
public class SnapshotDataStoreDaoImpl extends GenericDaoBase<SnapshotDataStoreVO, Long> implements SnapshotDataStoreDao {
    private static final Logger s_logger = Logger.getLogger(SnapshotDataStoreDaoImpl.class);
    private SearchBuilder<SnapshotDataStoreVO> updateStateSearch;
    private SearchBuilder<SnapshotDataStoreVO> storeSearch;
    private SearchBuilder<SnapshotDataStoreVO> storeStateSearch;
    private SearchBuilder<SnapshotDataStoreVO> destroyedSearch;
    private SearchBuilder<SnapshotDataStoreVO> cacheSearch;
    private SearchBuilder<SnapshotDataStoreVO> snapshotSearch;
    private SearchBuilder<SnapshotDataStoreVO> storeSnapshotSearch;
    private SearchBuilder<SnapshotDataStoreVO> snapshotIdSearch;
    private SearchBuilder<SnapshotDataStoreVO> volumeIdSearch;
    private SearchBuilder<SnapshotDataStoreVO> volumeSearch;
    private SearchBuilder<SnapshotDataStoreVO> stateSearch;
    private SearchBuilder<SnapshotDataStoreVO> parentSnapshotSearch;
    private SearchBuilder<SnapshotVO> snapshotVOSearch;

    public static ArrayList<Hypervisor.HypervisorType> hypervisorsSupportingSnapshotsChaining = new ArrayList<Hypervisor.HypervisorType>();

    @Inject
    private SnapshotDao _snapshotDao;

    private final String findLatestSnapshot = "select store_id, store_role, snapshot_id from cloud.snapshot_store_ref where " +
            " store_role = ? and volume_id = ? and state = 'Ready'" +
            " order by created DESC " +
            " limit 1";
    private final String findOldestSnapshot = "select store_id, store_role, snapshot_id from cloud.snapshot_store_ref where " +
            " store_role = ? and volume_id = ? and state = 'Ready'" +
            " order by created ASC " +
            " limit 1";

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        // Note that snapshot_store_ref stores snapshots on primary as well as
        // those on secondary, so we need to
        // use (store_id, store_role) to search
        storeSearch = createSearchBuilder();
        storeSearch.and("store_id", storeSearch.entity().getDataStoreId(), SearchCriteria.Op.EQ);
        storeSearch.and("store_role", storeSearch.entity().getRole(), SearchCriteria.Op.EQ);
        storeSearch.and("state", storeSearch.entity().getState(), SearchCriteria.Op.NEQ);
        storeSearch.done();

        storeStateSearch = createSearchBuilder();
        storeStateSearch.and("store_id", storeStateSearch.entity().getDataStoreId(), SearchCriteria.Op.EQ);
        storeStateSearch.and("state", storeStateSearch.entity().getState(), SearchCriteria.Op.EQ);
        storeStateSearch.done();

        destroyedSearch = createSearchBuilder();
        destroyedSearch.and("store_id", destroyedSearch.entity().getDataStoreId(), SearchCriteria.Op.EQ);
        destroyedSearch.and("store_role", destroyedSearch.entity().getRole(), SearchCriteria.Op.EQ);
        destroyedSearch.and("state", destroyedSearch.entity().getState(), SearchCriteria.Op.EQ);
        destroyedSearch.done();

        cacheSearch = createSearchBuilder();
        cacheSearch.and("store_id", cacheSearch.entity().getDataStoreId(), SearchCriteria.Op.EQ);
        cacheSearch.and("store_role", cacheSearch.entity().getRole(), SearchCriteria.Op.EQ);
        cacheSearch.and("state", cacheSearch.entity().getState(), SearchCriteria.Op.NEQ);
        cacheSearch.and("ref_cnt", cacheSearch.entity().getRefCnt(), SearchCriteria.Op.NEQ);
        cacheSearch.done();

        updateStateSearch = this.createSearchBuilder();
        updateStateSearch.and("id", updateStateSearch.entity().getId(), Op.EQ);
        updateStateSearch.and("state", updateStateSearch.entity().getState(), Op.EQ);
        updateStateSearch.and("updatedCount", updateStateSearch.entity().getUpdatedCount(), Op.EQ);
        updateStateSearch.done();

        snapshotSearch = createSearchBuilder();
        snapshotSearch.and("snapshot_id", snapshotSearch.entity().getSnapshotId(), SearchCriteria.Op.EQ);
        snapshotSearch.and("store_role", snapshotSearch.entity().getRole(), SearchCriteria.Op.EQ);
        snapshotSearch.and("state", snapshotSearch.entity().getState(), SearchCriteria.Op.EQ);
        snapshotSearch.done();

        storeSnapshotSearch = createSearchBuilder();
        storeSnapshotSearch.and("snapshot_id", storeSnapshotSearch.entity().getSnapshotId(), SearchCriteria.Op.EQ);
        storeSnapshotSearch.and("store_id", storeSnapshotSearch.entity().getDataStoreId(), SearchCriteria.Op.EQ);
        storeSnapshotSearch.and("store_role", storeSnapshotSearch.entity().getRole(), SearchCriteria.Op.EQ);
        storeSnapshotSearch.and("state", storeSnapshotSearch.entity().getState(), SearchCriteria.Op.EQ);
        storeSnapshotSearch.done();

        snapshotIdSearch = createSearchBuilder();
        snapshotIdSearch.and("snapshot_id", snapshotIdSearch.entity().getSnapshotId(), SearchCriteria.Op.EQ);
        snapshotIdSearch.done();

        volumeIdSearch = createSearchBuilder();
        volumeIdSearch.and("volume_id", volumeIdSearch.entity().getVolumeId(), SearchCriteria.Op.EQ);
        volumeIdSearch.done();

        volumeSearch = createSearchBuilder();
        volumeSearch.and("volume_id", volumeSearch.entity().getVolumeId(), SearchCriteria.Op.EQ);
        volumeSearch.and("store_role", volumeSearch.entity().getRole(), SearchCriteria.Op.EQ);
        volumeSearch.done();

        stateSearch = createSearchBuilder();
        stateSearch.and("state", stateSearch.entity().getState(), SearchCriteria.Op.IN);
        stateSearch.done();

        parentSnapshotSearch = createSearchBuilder();
        parentSnapshotSearch.and("volume_id", parentSnapshotSearch.entity().getVolumeId(), SearchCriteria.Op.EQ);
        parentSnapshotSearch.and("store_id", parentSnapshotSearch.entity().getDataStoreId(), SearchCriteria.Op.EQ);
        parentSnapshotSearch.and("store_role", parentSnapshotSearch.entity().getRole(), SearchCriteria.Op.EQ);
        parentSnapshotSearch.and("state", parentSnapshotSearch.entity().getState(), SearchCriteria.Op.EQ);
        parentSnapshotSearch.done();

        snapshotVOSearch = _snapshotDao.createSearchBuilder();
        snapshotVOSearch.and("volume_id", snapshotVOSearch.entity().getVolumeId(), SearchCriteria.Op.EQ);
        snapshotVOSearch.done();

        return true;
    }

    @Override
    public boolean updateState(State currentState, Event event, State nextState, DataObjectInStore vo, Object data) {
        SnapshotDataStoreVO dataObj = (SnapshotDataStoreVO)vo;
        Long oldUpdated = dataObj.getUpdatedCount();
        Date oldUpdatedTime = dataObj.getUpdated();

        SearchCriteria<SnapshotDataStoreVO> sc = updateStateSearch.create();
        sc.setParameters("id", dataObj.getId());
        sc.setParameters("state", currentState);
        sc.setParameters("updatedCount", dataObj.getUpdatedCount());

        dataObj.incrUpdatedCount();

        UpdateBuilder builder = getUpdateBuilder(dataObj);
        builder.set(dataObj, "state", nextState);
        builder.set(dataObj, "updated", new Date());

        int rows = update(dataObj, sc);
        if (rows == 0 && s_logger.isDebugEnabled()) {
            SnapshotDataStoreVO dbVol = findByIdIncludingRemoved(dataObj.getId());
            if (dbVol != null) {
                StringBuilder str = new StringBuilder("Unable to update ").append(dataObj.toString());
                str.append(": DB Data={id=")
                .append(dbVol.getId())
                .append("; state=")
                .append(dbVol.getState())
                .append("; updatecount=")
                .append(dbVol.getUpdatedCount())
                .append(";updatedTime=")
                .append(dbVol.getUpdated());
                str.append(": New Data={id=")
                .append(dataObj.getId())
                .append("; state=")
                .append(nextState)
                .append("; event=")
                .append(event)
                .append("; updatecount=")
                .append(dataObj.getUpdatedCount())
                .append("; updatedTime=")
                .append(dataObj.getUpdated());
                str.append(": stale Data={id=")
                .append(dataObj.getId())
                .append("; state=")
                .append(currentState)
                .append("; event=")
                .append(event)
                .append("; updatecount=")
                .append(oldUpdated)
                .append("; updatedTime=")
                .append(oldUpdatedTime);
            } else {
                s_logger.debug("Unable to update objectIndatastore: id=" + dataObj.getId() + ", as there is no such object exists in the database anymore");
            }
        }
        return rows > 0;
    }

    @Override
    public List<SnapshotDataStoreVO> listByStoreId(long id, DataStoreRole role) {
        SearchCriteria<SnapshotDataStoreVO> sc = storeSearch.create();
        sc.setParameters("store_id", id);
        sc.setParameters("store_role", role);
        sc.setParameters("state", ObjectInDataStoreStateMachine.State.Destroyed);
        return listBy(sc);
    }

    @Override
    public List<SnapshotDataStoreVO> listByStoreIdAndState(long id, ObjectInDataStoreStateMachine.State state) {
        SearchCriteria<SnapshotDataStoreVO> sc = storeStateSearch.create();
        sc.setParameters("store_id", id);
        sc.setParameters("state", state);
        return listBy(sc);
    }

    @Override
    public void deletePrimaryRecordsForStore(long id, DataStoreRole role) {
        SearchCriteria<SnapshotDataStoreVO> sc = storeSearch.create();
        sc.setParameters("store_id", id);
        sc.setParameters("store_role", role);
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        remove(sc);
        txn.commit();
    }

    @Override
    public void deleteSnapshotRecordsOnPrimary() {
        SearchCriteria<SnapshotDataStoreVO> sc = storeSearch.create();
        sc.setParameters("store_role", DataStoreRole.Primary);
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        remove(sc);
        txn.commit();
    }

    @Override
    public SnapshotDataStoreVO findByStoreSnapshot(DataStoreRole role, long storeId, long snapshotId) {
        SearchCriteria<SnapshotDataStoreVO> sc = storeSnapshotSearch.create();
        sc.setParameters("store_id", storeId);
        sc.setParameters("snapshot_id", snapshotId);
        sc.setParameters("store_role", role);
        return findOneBy(sc);
    }

    @Override
    public SnapshotDataStoreVO findLatestSnapshotForVolume(Long volumeId, DataStoreRole role) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try (
                PreparedStatement pstmt = txn.prepareStatement(findLatestSnapshot);
                ){
            pstmt.setString(1, role.toString());
            pstmt.setLong(2, volumeId);
            try (ResultSet rs = pstmt.executeQuery();) {
                while (rs.next()) {
                    long sid = rs.getLong(1);
                    long snid = rs.getLong(3);
                    return findByStoreSnapshot(role, sid, snid);
                }
            }
        } catch (SQLException e) {
            s_logger.debug("Failed to find latest snapshot for volume: " + volumeId + " due to: "  + e.toString());
        }
        return null;
    }

    @Override
    public SnapshotDataStoreVO findOldestSnapshotForVolume(Long volumeId, DataStoreRole role) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try (
                PreparedStatement pstmt = txn.prepareStatement(findOldestSnapshot);
                ){
            pstmt.setString(1, role.toString());
            pstmt.setLong(2, volumeId);
            try (ResultSet rs = pstmt.executeQuery();) {
                while (rs.next()) {
                    long sid = rs.getLong(1);
                    long snid = rs.getLong(3);
                    return findByStoreSnapshot(role, sid, snid);
                }
            }
        } catch (SQLException e) {
            s_logger.debug("Failed to find oldest snapshot for volume: " + volumeId + " due to: "  + e.toString());
        }
        return null;
    }

    @Override
    @DB
    public SnapshotDataStoreVO findParent(DataStoreRole role, Long storeId, Long volumeId) {
        if(isSnapshotChainingRequired(volumeId)) {
            SearchCriteria<SnapshotDataStoreVO> sc = parentSnapshotSearch.create();
            sc.setParameters("volume_id", volumeId);
            sc.setParameters("store_role", role.toString());
            sc.setParameters("state", ObjectInDataStoreStateMachine.State.Ready.name());
            sc.setParameters("store_id", storeId);

            List<SnapshotDataStoreVO> snapshotList = listBy(sc, new Filter(SnapshotDataStoreVO.class, "created", false, null, null));
            if (snapshotList != null && snapshotList.size() != 0) {
                return snapshotList.get(0);
            }
        }
        return null;
    }

    @Override
    public SnapshotDataStoreVO findBySnapshot(long snapshotId, DataStoreRole role) {
        SearchCriteria<SnapshotDataStoreVO> sc = snapshotSearch.create();
        sc.setParameters("snapshot_id", snapshotId);
        sc.setParameters("store_role", role);
        sc.setParameters("state", State.Ready);
        return findOneBy(sc);
    }

    @Override
    public List<SnapshotDataStoreVO> listAllByVolumeAndDataStore(long volumeId, DataStoreRole role) {
        SearchCriteria<SnapshotDataStoreVO> sc = volumeSearch.create();
        sc.setParameters("volume_id", volumeId);
        sc.setParameters("store_role", role);
        return listBy(sc);
    }

    @Override
    public SnapshotDataStoreVO findByVolume(long volumeId, DataStoreRole role) {
        SearchCriteria<SnapshotDataStoreVO> sc = volumeSearch.create();
        sc.setParameters("volume_id", volumeId);
        sc.setParameters("store_role", role);
        return findOneBy(sc);
    }

    @Override
    public List<SnapshotDataStoreVO> findBySnapshotId(long snapshotId) {
        SearchCriteria<SnapshotDataStoreVO> sc = snapshotIdSearch.create();
        sc.setParameters("snapshot_id", snapshotId);
        return listBy(sc);
    }

    @Override
    public List<SnapshotDataStoreVO> listDestroyed(long id) {
        SearchCriteria<SnapshotDataStoreVO> sc = destroyedSearch.create();
        sc.setParameters("store_id", id);
        sc.setParameters("store_role", DataStoreRole.Image);
        sc.setParameters("state", ObjectInDataStoreStateMachine.State.Destroyed);
        return listBy(sc);
    }

    @Override
    public List<SnapshotDataStoreVO> listActiveOnCache(long id) {
        SearchCriteria<SnapshotDataStoreVO> sc = cacheSearch.create();
        sc.setParameters("store_id", id);
        sc.setParameters("store_role", DataStoreRole.ImageCache);
        sc.setParameters("state", ObjectInDataStoreStateMachine.State.Destroyed);
        sc.setParameters("ref_cnt", 0);
        return listBy(sc);
    }

    @Override
    public void duplicateCacheRecordsOnRegionStore(long storeId) {
        // find all records on image cache
        SearchCriteria<SnapshotDataStoreVO> sc = storeSnapshotSearch.create();
        sc.setParameters("store_role", DataStoreRole.ImageCache);
        sc.setParameters("destroyed", false);
        List<SnapshotDataStoreVO> snapshots = listBy(sc);
        // create an entry for each record, but with empty install path since the content is not yet on region-wide store yet
        if (snapshots != null) {
            s_logger.info("Duplicate " + snapshots.size() + " snapshot cache store records to region store");
            for (SnapshotDataStoreVO snap : snapshots) {
                SnapshotDataStoreVO snapStore = findByStoreSnapshot(DataStoreRole.Image, storeId, snap.getSnapshotId());
                if (snapStore != null) {
                    s_logger.info("There is already entry for snapshot " + snap.getSnapshotId() + " on region store " + storeId);
                    continue;
                }
                s_logger.info("Persisting an entry for snapshot " + snap.getSnapshotId() + " on region store " + storeId);
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
                // increase ref_cnt so that this will not be recycled before the content is pushed to region-wide store
                snap.incrRefCnt();
                update(snap.getId(), snap);
            }
        }

    }

    @Override
    public SnapshotDataStoreVO findReadyOnCache(long snapshotId) {
        SearchCriteria<SnapshotDataStoreVO> sc = storeSnapshotSearch.create();
        sc.setParameters("snapshot_id", snapshotId);
        sc.setParameters("store_role", DataStoreRole.ImageCache);
        sc.setParameters("state", ObjectInDataStoreStateMachine.State.Ready);
        return findOneIncludingRemovedBy(sc);
    }

    @Override
    public List<SnapshotDataStoreVO> listOnCache(long snapshotId) {
        SearchCriteria<SnapshotDataStoreVO> sc = storeSnapshotSearch.create();
        sc.setParameters("snapshot_id", snapshotId);
        sc.setParameters("store_role", DataStoreRole.ImageCache);
        return search(sc, null);
    }

    @Override
    public void updateStoreRoleToCache(long storeId) {
        SearchCriteria<SnapshotDataStoreVO> sc = storeSearch.create();
        sc.setParameters("store_id", storeId);
        sc.setParameters("destroyed", false);
        List<SnapshotDataStoreVO> snaps = listBy(sc);
        if (snaps != null) {
            s_logger.info("Update to cache store role for " + snaps.size() + " entries in snapshot_store_ref");
            for (SnapshotDataStoreVO snap : snaps) {
                snap.setRole(DataStoreRole.ImageCache);
                update(snap.getId(), snap);
            }
        }
    }

    @Override
    public void updateVolumeIds(long oldVolId, long newVolId) {
        SearchCriteria<SnapshotDataStoreVO> sc = volumeIdSearch.create();
        sc.setParameters("volume_id", oldVolId);
        SnapshotDataStoreVO snapshot = createForUpdate();
        snapshot.setVolumeId(newVolId);
        UpdateBuilder ub = getUpdateBuilder(snapshot);
        update(ub, sc, null);
    }

    @Override
    public List<SnapshotDataStoreVO> listByState(ObjectInDataStoreStateMachine.State... states) {
        SearchCriteria<SnapshotDataStoreVO> sc = stateSearch.create();
        sc.setParameters("state", (Object[])states);
        return listBy(sc, null);
    }

    private boolean isSnapshotChainingRequired(long volumeId) {

        hypervisorsSupportingSnapshotsChaining.add(Hypervisor.HypervisorType.XenServer);

        SearchCriteria<SnapshotVO> sc = snapshotVOSearch.create();
        sc.setParameters("volume_id", volumeId);

        SnapshotVO volSnapshot = _snapshotDao.findOneBy(sc);

        if (volSnapshot != null && hypervisorsSupportingSnapshotsChaining.contains(volSnapshot.getHypervisorType())) {
            return true;
        }

        return false;
    }

}
