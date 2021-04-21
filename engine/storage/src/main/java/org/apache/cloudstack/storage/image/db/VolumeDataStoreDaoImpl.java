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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import com.cloud.utils.db.Filter;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectInStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.Event;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.State;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreVO;

import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.JoinBuilder.JoinType;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.UpdateBuilder;

@Component
public class VolumeDataStoreDaoImpl extends GenericDaoBase<VolumeDataStoreVO, Long> implements VolumeDataStoreDao {
    private static final Logger s_logger = Logger.getLogger(VolumeDataStoreDaoImpl.class);
    private SearchBuilder<VolumeDataStoreVO> updateStateSearch;
    private SearchBuilder<VolumeDataStoreVO> volumeSearch;
    private SearchBuilder<VolumeDataStoreVO> storeSearch;
    private SearchBuilder<VolumeDataStoreVO> cacheSearch;
    private SearchBuilder<VolumeDataStoreVO> storeVolumeSearch;
    private SearchBuilder<VolumeDataStoreVO> downloadVolumeSearch;
    private SearchBuilder<VolumeDataStoreVO> uploadVolumeSearch;
    private SearchBuilder<VolumeVO> volumeOnlySearch;
    private SearchBuilder<VolumeDataStoreVO> uploadVolumeStateSearch;
    private static final String EXPIRE_DOWNLOAD_URLS_FOR_ZONE = "update volume_store_ref set download_url_created=? where download_url_created is not null and store_id in (select id from image_store where data_center_id=?)";


    @Inject
    DataStoreManager storeMgr;
    @Inject
    VolumeDao volumeDao;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        storeSearch = createSearchBuilder();
        storeSearch.and("store_id", storeSearch.entity().getDataStoreId(), SearchCriteria.Op.EQ);
        storeSearch.and("destroyed", storeSearch.entity().getDestroyed(), SearchCriteria.Op.EQ);
        storeSearch.done();

        cacheSearch = createSearchBuilder();
        cacheSearch.and("store_id", cacheSearch.entity().getDataStoreId(), SearchCriteria.Op.EQ);
        cacheSearch.and("destroyed", cacheSearch.entity().getDestroyed(), SearchCriteria.Op.EQ);
        cacheSearch.and("ref_cnt", cacheSearch.entity().getRefCnt(), SearchCriteria.Op.NEQ);
        cacheSearch.done();

        volumeSearch = createSearchBuilder();
        volumeSearch.and("volume_id", volumeSearch.entity().getVolumeId(), SearchCriteria.Op.EQ);
        volumeSearch.and("destroyed", volumeSearch.entity().getDestroyed(), SearchCriteria.Op.EQ);
        volumeSearch.done();

        storeVolumeSearch = createSearchBuilder();
        storeVolumeSearch.and("store_id", storeVolumeSearch.entity().getDataStoreId(), SearchCriteria.Op.EQ);
        storeVolumeSearch.and("volume_id", storeVolumeSearch.entity().getVolumeId(), SearchCriteria.Op.EQ);
        storeVolumeSearch.and("destroyed", storeVolumeSearch.entity().getDestroyed(), SearchCriteria.Op.EQ);
        storeVolumeSearch.done();

        updateStateSearch = this.createSearchBuilder();
        updateStateSearch.and("id", updateStateSearch.entity().getId(), Op.EQ);
        updateStateSearch.and("state", updateStateSearch.entity().getState(), Op.EQ);
        updateStateSearch.and("updatedCount", updateStateSearch.entity().getUpdatedCount(), Op.EQ);
        updateStateSearch.done();

        downloadVolumeSearch = createSearchBuilder();
        downloadVolumeSearch.and("download_url", downloadVolumeSearch.entity().getExtractUrl(), Op.NNULL);
        downloadVolumeSearch.and("download_url_created", downloadVolumeSearch.entity().getExtractUrlCreated(), Op.NNULL);
        downloadVolumeSearch.and("destroyed", downloadVolumeSearch.entity().getDestroyed(), SearchCriteria.Op.EQ);
        downloadVolumeSearch.and("zone_id", downloadVolumeSearch.entity().getZoneId(), Op.EQ);
        downloadVolumeSearch.done();

        uploadVolumeSearch = createSearchBuilder();
        uploadVolumeSearch.and("store_id", uploadVolumeSearch.entity().getDataStoreId(), SearchCriteria.Op.EQ);
        uploadVolumeSearch.and("url", uploadVolumeSearch.entity().getDownloadUrl(), Op.NNULL);
        uploadVolumeSearch.and("destroyed", uploadVolumeSearch.entity().getDestroyed(), SearchCriteria.Op.EQ);
        uploadVolumeSearch.done();

        volumeOnlySearch = volumeDao.createSearchBuilder();
        volumeOnlySearch.and("states", volumeOnlySearch.entity().getState(), Op.IN);
        uploadVolumeStateSearch = createSearchBuilder();
        uploadVolumeStateSearch.join("volumeOnlySearch", volumeOnlySearch, volumeOnlySearch.entity().getId(), uploadVolumeStateSearch.entity().getVolumeId(), JoinType.LEFT);
        uploadVolumeStateSearch.and("destroyed", uploadVolumeStateSearch.entity().getDestroyed(), SearchCriteria.Op.EQ);
        uploadVolumeStateSearch.done();

        return true;
    }

    @Override
    public boolean updateState(State currentState, Event event, State nextState, DataObjectInStore vo, Object data) {
        VolumeDataStoreVO dataObj = (VolumeDataStoreVO)vo;
        Long oldUpdated = dataObj.getUpdatedCount();
        Date oldUpdatedTime = dataObj.getUpdated();

        SearchCriteria<VolumeDataStoreVO> sc = updateStateSearch.create();
        sc.setParameters("id", dataObj.getId());
        sc.setParameters("state", currentState);
        sc.setParameters("updatedCount", dataObj.getUpdatedCount());

        dataObj.incrUpdatedCount();

        UpdateBuilder builder = getUpdateBuilder(dataObj);
        builder.set(dataObj, "state", nextState);
        builder.set(dataObj, "updated", new Date());
        if (nextState == State.Destroyed) {
            builder.set(dataObj, "destroyed", true);
        }

        int rows = update(dataObj, sc);
        if (rows == 0 && s_logger.isDebugEnabled()) {
            VolumeDataStoreVO dbVol = findByIdIncludingRemoved(dataObj.getId());
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
    public List<VolumeDataStoreVO> listByStoreId(long id) {
        SearchCriteria<VolumeDataStoreVO> sc = storeSearch.create();
        sc.setParameters("store_id", id);
        sc.setParameters("destroyed", false);
        return listIncludingRemovedBy(sc);
    }

    @Override
    public List<VolumeDataStoreVO> listActiveOnCache(long id) {
        SearchCriteria<VolumeDataStoreVO> sc = cacheSearch.create();
        sc.setParameters("store_id", id);
        sc.setParameters("destroyed", false);
        sc.setParameters("ref_cnt", 0);
        return listIncludingRemovedBy(sc);
    }

    @Override
    public void deletePrimaryRecordsForStore(long id) {
        SearchCriteria<VolumeDataStoreVO> sc = storeSearch.create();
        sc.setParameters("store_id", id);
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        remove(sc);
        txn.commit();
    }

    @Override
    public VolumeDataStoreVO findByVolume(long volumeId) {
        SearchCriteria<VolumeDataStoreVO> sc = volumeSearch.create();
        sc.setParameters("volume_id", volumeId);
        sc.setParameters("destroyed", false);
        return findVolumeby(sc);
    }

    private VolumeDataStoreVO findVolumeby(SearchCriteria<VolumeDataStoreVO> sc) {
        Filter filter = new Filter(VolumeDataStoreVO.class, "created", false, 0L, 1L);
        List<VolumeDataStoreVO> results = searchIncludingRemoved(sc, filter, null, false);
        return results.size() == 0 ? null : results.get(0);
    }

    @Override
    public VolumeDataStoreVO findByStoreVolume(long storeId, long volumeId) {
        SearchCriteria<VolumeDataStoreVO> sc = storeVolumeSearch.create();
        sc.setParameters("store_id", storeId);
        sc.setParameters("volume_id", volumeId);
        sc.setParameters("destroyed", false);

        /*
        When we download volume then we create entry in volume_store_ref table.
        We mark the volume entry to ready state once download_url gets generated.
        When we migrate that volume, then again one more entry is created with same volume id.
        Its state is marked as allocated. Later we try to list only one dataobject in datastore
        for state transition during volume migration. If the listed volume's state is allocated
        then migration passes otherwise it fails.

         Below fix will remove the randomness and give priority to volume entry which is made for
         migration (download_url/extracturl will be null in case of migration). Giving priority to
         download volume case is not needed as there will be only one entry in that case so no randomness.
        */
        List<VolumeDataStoreVO> vos = listBy(sc);
        if(vos.size() > 1) {
            for(VolumeDataStoreVO vo : vos) {
                if(vo.getExtractUrl() == null) {
                    return vo;
                }
            }
        }

        return vos.size() == 1 ? vos.get(0) : null;
    }

    @Override
    public VolumeDataStoreVO findByStoreVolume(long storeId, long volumeId, boolean lock) {
        SearchCriteria<VolumeDataStoreVO> sc = storeVolumeSearch.create();
        sc.setParameters("store_id", storeId);
        sc.setParameters("volume_id", volumeId);
        sc.setParameters("destroyed", false);
        if (!lock) {
            return findOneIncludingRemovedBy(sc);
        } else {
            return lockOneRandomRow(sc, true);
        }
    }

    @Override
    public List<VolumeDataStoreVO> listDestroyed(long id) {
        SearchCriteria<VolumeDataStoreVO> sc = storeSearch.create();
        sc.setParameters("store_id", id);
        sc.setParameters("destroyed", true);
        return listIncludingRemovedBy(sc);
    }

    @Override
    public void duplicateCacheRecordsOnRegionStore(long storeId) {
        // find all records on image cache
        List<DataStore> cacheStores = storeMgr.listImageCacheStores();
        if (cacheStores == null || cacheStores.size() == 0) {
            return;
        }
        List<VolumeDataStoreVO> vols = new ArrayList<VolumeDataStoreVO>();
        for (DataStore store : cacheStores) {
            // check if the volume is stored there
            vols.addAll(listByStoreId(store.getId()));
        }
        // create an entry for each record, but with empty install path since the content is not yet on region-wide store yet
        if (vols != null) {
            s_logger.info("Duplicate " + vols.size() + " volume cache store records to region store");
            for (VolumeDataStoreVO vol : vols) {
                VolumeDataStoreVO volStore = findByStoreVolume(storeId, vol.getVolumeId());
                if (volStore != null) {
                    s_logger.info("There is already entry for volume " + vol.getVolumeId() + " on region store " + storeId);
                    continue;
                }
                s_logger.info("Persisting an entry for volume " + vol.getVolumeId() + " on region store " + storeId);
                VolumeDataStoreVO vs = new VolumeDataStoreVO();
                vs.setVolumeId(vol.getVolumeId());
                vs.setDataStoreId(storeId);
                vs.setState(vol.getState());
                vs.setDownloadPercent(vol.getDownloadPercent());
                vs.setDownloadState(vol.getDownloadState());
                vs.setSize(vol.getSize());
                vs.setPhysicalSize(vol.getPhysicalSize());
                vs.setErrorString(vol.getErrorString());
                vs.setRefCnt(vol.getRefCnt());
                persist(vs);
                // increase ref_cnt so that this will not be recycled before the content is pushed to region-wide store
                vol.incrRefCnt();
                this.update(vol.getId(), vol);
            }
        }

    }

    @Override
    public List<VolumeDataStoreVO> listVolumeDownloadUrls() {
        SearchCriteria<VolumeDataStoreVO> sc = downloadVolumeSearch.create();
        sc.setParameters("destroyed", false);
        return listBy(sc);
    }

    @Override
    public List<VolumeDataStoreVO> listVolumeDownloadUrlsByZoneId(long zoneId) {
        SearchCriteria<VolumeDataStoreVO> sc = downloadVolumeSearch.create();
        sc.setParameters("destroyed", false);
        sc.setParameters("zone_id", zoneId);
        return listBy(sc);
    }

    @Override
    public List<VolumeDataStoreVO> listUploadedVolumesByStoreId(long id) {
        SearchCriteria<VolumeDataStoreVO> sc = uploadVolumeSearch.create();
        sc.setParameters("store_id", id);
        sc.setParameters("destroyed", false);
        return listIncludingRemovedBy(sc);
    }


    @Override
    public void expireDnldUrlsForZone(Long dcId){
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        PreparedStatement pstmt = null;
        try {
            txn.start();
            pstmt = txn.prepareAutoCloseStatement(EXPIRE_DOWNLOAD_URLS_FOR_ZONE);
            pstmt.setDate(1, new java.sql.Date(-1l));// Set the time before the epoch time.
            pstmt.setLong(2, dcId);
            pstmt.executeUpdate();
            txn.commit();
        } catch (Exception e) {
            txn.rollback();
            s_logger.warn("Failed expiring download urls for dcId: " + dcId, e);
        }

    }

    @Override
    public List<VolumeDataStoreVO> listByVolumeState(Volume.State... states) {
        SearchCriteria<VolumeDataStoreVO> sc = uploadVolumeStateSearch.create();
        sc.setJoinParameters("volumeOnlySearch", "states", (Object[])states);
        sc.setParameters("destroyed", false);
        return listIncludingRemovedBy(sc);
    }

    @Override
    public boolean updateVolumeId(long srcVolId, long destVolId) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try {
            VolumeDataStoreVO volumeDataStoreVO = findByVolume(srcVolId);
            if(volumeDataStoreVO != null) {
                txn.start();
                volumeDataStoreVO.setVolumeId(destVolId);
                update(volumeDataStoreVO.getId(), volumeDataStoreVO);
                txn.commit();
            }
        } catch (Exception e) {
            throw new CloudRuntimeException("Unable to update the volume id for volume store ref", e);
        }
        return true;
    }
}
