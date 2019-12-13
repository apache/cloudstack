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

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.server.ResourceTag.ResourceObjectType;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Volume;
import com.cloud.storage.Volume.Event;
import com.cloud.storage.Volume.State;
import com.cloud.storage.Volume.Type;
import com.cloud.storage.VolumeVO;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.utils.Pair;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.UpdateBuilder;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
public class VolumeDaoImpl extends GenericDaoBase<VolumeVO, Long> implements VolumeDao {
    private static final Logger s_logger = Logger.getLogger(VolumeDaoImpl.class);
    protected final SearchBuilder<VolumeVO> DetachedAccountIdSearch;
    protected final SearchBuilder<VolumeVO> TemplateZoneSearch;
    protected final GenericSearchBuilder<VolumeVO, SumCount> TotalSizeByPoolSearch;
    protected final GenericSearchBuilder<VolumeVO, SumCount> TotalVMSnapshotSizeByPoolSearch;
    protected final GenericSearchBuilder<VolumeVO, Long> ActiveTemplateSearch;
    protected final SearchBuilder<VolumeVO> InstanceStatesSearch;
    protected final SearchBuilder<VolumeVO> AllFieldsSearch;
    protected GenericSearchBuilder<VolumeVO, Long> CountByAccount;
    protected GenericSearchBuilder<VolumeVO, SumCount> primaryStorageSearch;
    protected GenericSearchBuilder<VolumeVO, SumCount> primaryStorageSearch2;
    protected GenericSearchBuilder<VolumeVO, SumCount> secondaryStorageSearch;
    @Inject
    ResourceTagDao _tagsDao;

    protected static final String SELECT_VM_SQL = "SELECT DISTINCT instance_id from volumes v where v.host_id = ? and v.mirror_state = ?";
    // need to account for zone-wide primary storage where storage_pool has
    // null-value pod and cluster, where hypervisor information is stored in
    // storage_pool
    protected static final String SELECT_HYPERTYPE_FROM_CLUSTER_VOLUME = "SELECT c.hypervisor_type from volumes v, storage_pool s, cluster c where v.pool_id = s.id and s.cluster_id = c.id and v.id = ?";
    protected static final String SELECT_HYPERTYPE_FROM_ZONE_VOLUME = "SELECT s.hypervisor from volumes v, storage_pool s where v.pool_id = s.id and v.id = ?";
    protected static final String SELECT_POOLSCOPE = "SELECT s.scope from storage_pool s, volumes v where s.id = v.pool_id and v.id = ?";

    private static final String ORDER_POOLS_NUMBER_OF_VOLUMES_FOR_ACCOUNT = "SELECT pool.id, SUM(IF(vol.state='Ready' AND vol.account_id = ?, 1, 0)) FROM `cloud`.`storage_pool` pool LEFT JOIN `cloud`.`volumes` vol ON pool.id = vol.pool_id WHERE pool.data_center_id = ? "
            + " AND pool.pod_id = ? AND pool.cluster_id = ? " + " GROUP BY pool.id ORDER BY 2 ASC ";
    private static final String ORDER_ZONE_WIDE_POOLS_NUMBER_OF_VOLUMES_FOR_ACCOUNT = "SELECT pool.id, SUM(IF(vol.state='Ready' AND vol.account_id = ?, 1, 0)) FROM `cloud`.`storage_pool` pool LEFT JOIN `cloud`.`volumes` vol ON pool.id = vol.pool_id WHERE pool.data_center_id = ? "
            + " AND pool.scope = 'ZONE' AND pool.status='Up' " + " GROUP BY pool.id ORDER BY 2 ASC ";

    @Override
    public List<VolumeVO> findDetachedByAccount(long accountId) {
        SearchCriteria<VolumeVO> sc = DetachedAccountIdSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("destroyed", Volume.State.Destroy);
        return listBy(sc);
    }

    @Override
    public List<VolumeVO> findByAccount(long accountId) {
        SearchCriteria<VolumeVO> sc = AllFieldsSearch.create();
        sc.setParameters("accountId", accountId);
        return listBy(sc);
    }

    @Override
    public List<VolumeVO> findIncludingRemovedByAccount(long accountId) {
        SearchCriteria<VolumeVO> sc = AllFieldsSearch.create();
        sc.setParameters("accountId", accountId);
        return listIncludingRemovedBy(sc);
    }

    @Override
    public List<VolumeVO> findByInstance(long id) {
        SearchCriteria<VolumeVO> sc = AllFieldsSearch.create();
        sc.setParameters("instanceId", id);
        return listBy(sc);
    }

    @Override
    public List<VolumeVO> findByInstanceAndDeviceId(long instanceId, long deviceId) {
        SearchCriteria<VolumeVO> sc = AllFieldsSearch.create();
        sc.setParameters("instanceId", instanceId);
        sc.setParameters("deviceId", deviceId);
        return listBy(sc);
    }

    @Override
    public List<VolumeVO> findByPoolId(long poolId) {
        SearchCriteria<VolumeVO> sc = AllFieldsSearch.create();
        sc.setParameters("poolId", poolId);
        sc.setParameters("notDestroyed", Volume.State.Destroy, Volume.State.Expunged);
        sc.setParameters("vType", Volume.Type.ROOT.toString());
        return listBy(sc);
    }

    @Override
    public List<VolumeVO> findByInstanceIdAndPoolId(long instanceId, long poolId) {
        SearchCriteria<VolumeVO> sc = AllFieldsSearch.create();
        sc.setParameters("instanceId", instanceId);
        sc.setParameters("poolId", poolId);
        sc.setParameters("notDestroyed", Volume.State.Destroy, Volume.State.Expunged);
        return listBy(sc);
    }

    @Override
    public VolumeVO findByPoolIdName(long poolId, String name) {
        SearchCriteria<VolumeVO> sc = AllFieldsSearch.create();
        sc.setParameters("poolId", poolId);
        sc.setParameters("name", name);
        return findOneBy(sc);
    }

    @Override
    public List<VolumeVO> findByPoolId(long poolId, Volume.Type volumeType) {
        SearchCriteria<VolumeVO> sc = AllFieldsSearch.create();
        sc.setParameters("poolId", poolId);
        sc.setParameters("notDestroyed", Volume.State.Destroy, Volume.State.Expunged);

        if (volumeType != null) {
            sc.setParameters("vType", volumeType.toString());
        }

        return listBy(sc);
    }

    @Override
    public List<VolumeVO> findByPoolIdAndState(long poolId, Volume.State state) {
        SearchCriteria<VolumeVO> sc = AllFieldsSearch.create();
        sc.setParameters("poolId", poolId);
        sc.setParameters("state", state);
        return listBy(sc);
    }

    @Override
    public List<VolumeVO> findCreatedByInstance(long id) {
        SearchCriteria<VolumeVO> sc = AllFieldsSearch.create();
        sc.setParameters("instanceId", id);
        sc.setParameters("state", Volume.State.Ready);
        return listBy(sc);
    }

    @Override
    public List<VolumeVO> findUsableVolumesForInstance(long instanceId) {
        SearchCriteria<VolumeVO> sc = InstanceStatesSearch.create();
        sc.setParameters("instance", instanceId);
        sc.setParameters("states", Volume.State.Creating, Volume.State.Ready, Volume.State.Allocated);

        return listBy(sc);
    }

    @Override
    public List<VolumeVO> findByInstanceAndType(long id, Type vType) {
        SearchCriteria<VolumeVO> sc = AllFieldsSearch.create();
        sc.setParameters("instanceId", id);
        sc.setParameters("vType", vType.toString());
        return listBy(sc);
    }

    @Override
    public List<VolumeVO> findByInstanceIdDestroyed(long vmId) {
        SearchCriteria<VolumeVO> sc = AllFieldsSearch.create();
        sc.setParameters("instanceId", vmId);
        sc.setParameters("destroyed", Volume.State.Destroy);
        return listBy(sc);
    }

    @Override
    public List<VolumeVO> findReadyRootVolumesByInstance(long instanceId) {
        SearchCriteria<VolumeVO> sc = AllFieldsSearch.create();
        sc.setParameters("instanceId", instanceId);
        sc.setParameters("state", Volume.State.Ready);
        sc.setParameters("vType", Volume.Type.ROOT);
        return listBy(sc);
    }

    @Override
    public List<VolumeVO> findByPod(long podId) {
        SearchCriteria<VolumeVO> sc = AllFieldsSearch.create();
        sc.setParameters("pod", podId);

        return listBy(sc);
    }

    @Override
    public List<VolumeVO> findByDc(long dcId) {
        SearchCriteria<VolumeVO> sc = AllFieldsSearch.create();
        sc.setParameters("dcId", dcId);

        return listBy(sc);
    }

    @Override
    public List<VolumeVO> findByAccountAndPod(long accountId, long podId) {
        SearchCriteria<VolumeVO> sc = AllFieldsSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("pod", podId);
        sc.setParameters("state", Volume.State.Ready);

        return listIncludingRemovedBy(sc);
    }

    @Override
    public List<VolumeVO> findByTemplateAndZone(long templateId, long zoneId) {
        SearchCriteria<VolumeVO> sc = TemplateZoneSearch.create();
        sc.setParameters("template", templateId);
        sc.setParameters("zone", zoneId);

        return listIncludingRemovedBy(sc);
    }

    @Override
    public boolean isAnyVolumeActivelyUsingTemplateOnPool(long templateId, long poolId) {
        SearchCriteria<Long> sc = ActiveTemplateSearch.create();
        sc.setParameters("template", templateId);
        sc.setParameters("pool", poolId);

        List<Long> results = customSearchIncludingRemoved(sc, null);
        assert results.size() > 0 : "How can this return a size of " + results.size();

        return results.get(0) > 0;
    }

    @Override
    public void deleteVolumesByInstance(long instanceId) {
        SearchCriteria<VolumeVO> sc = AllFieldsSearch.create();
        sc.setParameters("instanceId", instanceId);
        expunge(sc);
    }

    @Override
    public void attachVolume(long volumeId, long vmId, long deviceId) {
        VolumeVO volume = createForUpdate(volumeId);
        volume.setInstanceId(vmId);
        volume.setDeviceId(deviceId);
        volume.setUpdated(new Date());
        volume.setAttached(new Date());
        if (deviceId == 0L) {
            volume.setVolumeType(Type.ROOT);
        }
        update(volumeId, volume);
    }

    @Override
    public void detachVolume(long volumeId) {
        VolumeVO volume = createForUpdate(volumeId);
        volume.setInstanceId(null);
        volume.setDeviceId(null);
        volume.setUpdated(new Date());
        volume.setAttached(null);
        if (findById(volumeId).getVolumeType() == Type.ROOT) {
            volume.setVolumeType(Type.DATADISK);
        }
        update(volumeId, volume);
    }

    @Override
    @DB
    public HypervisorType getHypervisorType(long volumeId) {
        /* lookup from cluster of pool */
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        PreparedStatement pstmt = null;
        String sql = null;
        try {
            ScopeType scope = getVolumeStoragePoolScope(volumeId);
            if (scope != null) {
                if (scope == ScopeType.CLUSTER || scope == ScopeType.HOST) {
                    sql = SELECT_HYPERTYPE_FROM_CLUSTER_VOLUME;
                } else if (scope == ScopeType.ZONE) {
                    sql = SELECT_HYPERTYPE_FROM_ZONE_VOLUME;
                } else {
                    s_logger.error("Unhandled scope type '" + scope + "' when running getHypervisorType on volume id " + volumeId);
                }

                pstmt = txn.prepareAutoCloseStatement(sql);
                pstmt.setLong(1, volumeId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    if (rs.getString(1) != null) {
                        return HypervisorType.getType(rs.getString(1));
                    }
                }
            }
            return HypervisorType.None;
        } catch (SQLException e) {
            throw new CloudRuntimeException("DB Exception on: " + sql, e);
        } catch (Throwable e) {
            throw new CloudRuntimeException("Caught: " + sql, e);
        }
    }

    @Override
    public ImageFormat getImageFormat(Long volumeId) {
        HypervisorType type = getHypervisorType(volumeId);
        if (type.equals(HypervisorType.KVM)) {
            return ImageFormat.QCOW2;
        } else if (type.equals(HypervisorType.XenServer)) {
            return ImageFormat.VHD;
        } else if (type.equals(HypervisorType.VMware)) {
            return ImageFormat.OVA;
        } else {
            s_logger.warn("Do not support hypervisor " + type.toString());
            return null;
        }
    }

    public VolumeDaoImpl() {
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("state", AllFieldsSearch.entity().getState(), Op.EQ);
        AllFieldsSearch.and("accountId", AllFieldsSearch.entity().getAccountId(), Op.EQ);
        AllFieldsSearch.and("dcId", AllFieldsSearch.entity().getDataCenterId(), Op.EQ);
        AllFieldsSearch.and("pod", AllFieldsSearch.entity().getPodId(), Op.EQ);
        AllFieldsSearch.and("instanceId", AllFieldsSearch.entity().getInstanceId(), Op.EQ);
        AllFieldsSearch.and("deviceId", AllFieldsSearch.entity().getDeviceId(), Op.EQ);
        AllFieldsSearch.and("poolId", AllFieldsSearch.entity().getPoolId(), Op.EQ);
        AllFieldsSearch.and("vType", AllFieldsSearch.entity().getVolumeType(), Op.EQ);
        AllFieldsSearch.and("id", AllFieldsSearch.entity().getId(), Op.EQ);
        AllFieldsSearch.and("destroyed", AllFieldsSearch.entity().getState(), Op.EQ);
        AllFieldsSearch.and("notDestroyed", AllFieldsSearch.entity().getState(), Op.NIN);
        AllFieldsSearch.and("updateTime", AllFieldsSearch.entity().getUpdated(), SearchCriteria.Op.LT);
        AllFieldsSearch.and("updatedCount", AllFieldsSearch.entity().getUpdatedCount(), Op.EQ);
        AllFieldsSearch.and("name", AllFieldsSearch.entity().getName(), Op.EQ);
        AllFieldsSearch.done();

        DetachedAccountIdSearch = createSearchBuilder();
        DetachedAccountIdSearch.and("accountId", DetachedAccountIdSearch.entity().getAccountId(), Op.EQ);
        DetachedAccountIdSearch.and("destroyed", DetachedAccountIdSearch.entity().getState(), Op.NEQ);
        DetachedAccountIdSearch.and("instanceId", DetachedAccountIdSearch.entity().getInstanceId(), Op.NULL);
        DetachedAccountIdSearch.done();

        TemplateZoneSearch = createSearchBuilder();
        TemplateZoneSearch.and("template", TemplateZoneSearch.entity().getTemplateId(), Op.EQ);
        TemplateZoneSearch.and("zone", TemplateZoneSearch.entity().getDataCenterId(), Op.EQ);
        TemplateZoneSearch.done();

        TotalSizeByPoolSearch = createSearchBuilder(SumCount.class);
        TotalSizeByPoolSearch.select("sum", Func.SUM, TotalSizeByPoolSearch.entity().getSize());
        TotalSizeByPoolSearch.select("count", Func.COUNT, (Object[])null);
        TotalSizeByPoolSearch.and("poolId", TotalSizeByPoolSearch.entity().getPoolId(), Op.EQ);
        TotalSizeByPoolSearch.and("removed", TotalSizeByPoolSearch.entity().getRemoved(), Op.NULL);
        TotalSizeByPoolSearch.and("state", TotalSizeByPoolSearch.entity().getState(), Op.NEQ);
        TotalSizeByPoolSearch.done();

        TotalVMSnapshotSizeByPoolSearch = createSearchBuilder(SumCount.class);
        TotalVMSnapshotSizeByPoolSearch.select("sum", Func.SUM, TotalVMSnapshotSizeByPoolSearch.entity().getVmSnapshotChainSize());
        TotalVMSnapshotSizeByPoolSearch.and("poolId", TotalVMSnapshotSizeByPoolSearch.entity().getPoolId(), Op.EQ);
        TotalVMSnapshotSizeByPoolSearch.and("removed", TotalVMSnapshotSizeByPoolSearch.entity().getRemoved(), Op.NULL);
        TotalVMSnapshotSizeByPoolSearch.and("state", TotalVMSnapshotSizeByPoolSearch.entity().getState(), Op.NEQ);
        TotalVMSnapshotSizeByPoolSearch.and("vType", TotalVMSnapshotSizeByPoolSearch.entity().getVolumeType(), Op.EQ);
        TotalVMSnapshotSizeByPoolSearch.and("instanceId", TotalVMSnapshotSizeByPoolSearch.entity().getInstanceId(), Op.NNULL);
        TotalVMSnapshotSizeByPoolSearch.done();

        ActiveTemplateSearch = createSearchBuilder(Long.class);
        ActiveTemplateSearch.and("pool", ActiveTemplateSearch.entity().getPoolId(), Op.EQ);
        ActiveTemplateSearch.and("template", ActiveTemplateSearch.entity().getTemplateId(), Op.EQ);
        ActiveTemplateSearch.and("removed", ActiveTemplateSearch.entity().getRemoved(), Op.NULL);
        ActiveTemplateSearch.select(null, Func.COUNT, null);
        ActiveTemplateSearch.done();

        InstanceStatesSearch = createSearchBuilder();
        InstanceStatesSearch.and("instance", InstanceStatesSearch.entity().getInstanceId(), Op.EQ);
        InstanceStatesSearch.and("states", InstanceStatesSearch.entity().getState(), Op.IN);
        InstanceStatesSearch.done();

        CountByAccount = createSearchBuilder(Long.class);
        CountByAccount.select(null, Func.COUNT, null);
        CountByAccount.and("account", CountByAccount.entity().getAccountId(), SearchCriteria.Op.EQ);
        CountByAccount.and("state", CountByAccount.entity().getState(), SearchCriteria.Op.NIN);
        CountByAccount.and("displayVolume", CountByAccount.entity().isDisplayVolume(), Op.EQ);
        CountByAccount.done();

        primaryStorageSearch = createSearchBuilder(SumCount.class);
        primaryStorageSearch.select("sum", Func.SUM, primaryStorageSearch.entity().getSize());
        primaryStorageSearch.and("accountId", primaryStorageSearch.entity().getAccountId(), Op.EQ);
        primaryStorageSearch.and().op("path", primaryStorageSearch.entity().getPath(), Op.NNULL);
        primaryStorageSearch.or("states", primaryStorageSearch.entity().getState(), Op.IN);
        primaryStorageSearch.cp();
        primaryStorageSearch.and("displayVolume", primaryStorageSearch.entity().isDisplayVolume(), Op.EQ);
        primaryStorageSearch.and("isRemoved", primaryStorageSearch.entity().getRemoved(), Op.NULL);
        primaryStorageSearch.and("NotCountStates", primaryStorageSearch.entity().getState(), Op.NIN);
        primaryStorageSearch.done();

        primaryStorageSearch2 = createSearchBuilder(SumCount.class);
        primaryStorageSearch2.select("sum", Func.SUM, primaryStorageSearch2.entity().getSize());
        primaryStorageSearch2.and("accountId", primaryStorageSearch2.entity().getAccountId(), Op.EQ);
        primaryStorageSearch2.and().op("instanceId", primaryStorageSearch2.entity().getInstanceId(), Op.NULL);
        primaryStorageSearch2.or("virtualRouterVmIds", primaryStorageSearch2.entity().getInstanceId(), Op.NIN);
        primaryStorageSearch2.cp();
        primaryStorageSearch2.and().op("path", primaryStorageSearch2.entity().getPath(), Op.NNULL);
        primaryStorageSearch2.or("states", primaryStorageSearch2.entity().getState(), Op.IN);
        primaryStorageSearch2.cp();
        primaryStorageSearch2.and("displayVolume", primaryStorageSearch2.entity().isDisplayVolume(), Op.EQ);
        primaryStorageSearch2.and("isRemoved", primaryStorageSearch2.entity().getRemoved(), Op.NULL);
        primaryStorageSearch2.and("NotCountStates", primaryStorageSearch2.entity().getState(), Op.NIN);
        primaryStorageSearch2.done();

        secondaryStorageSearch = createSearchBuilder(SumCount.class);
        secondaryStorageSearch.select("sum", Func.SUM, secondaryStorageSearch.entity().getSize());
        secondaryStorageSearch.and("accountId", secondaryStorageSearch.entity().getAccountId(), Op.EQ);
        secondaryStorageSearch.and("path", secondaryStorageSearch.entity().getPath(), Op.NULL);
        secondaryStorageSearch.and("states", secondaryStorageSearch.entity().getState(), Op.NIN);
        secondaryStorageSearch.and("isRemoved", secondaryStorageSearch.entity().getRemoved(), Op.NULL);
        secondaryStorageSearch.done();
    }

    @Override
    @DB()
    public Pair<Long, Long> getCountAndTotalByPool(long poolId) {
        SearchCriteria<SumCount> sc = TotalSizeByPoolSearch.create();
        sc.setParameters("poolId", poolId);
        List<SumCount> results = customSearch(sc, null);
        SumCount sumCount = results.get(0);
        return new Pair<Long, Long>(sumCount.count, sumCount.sum);
    }

    @Override
    public Long countAllocatedVolumesForAccount(long accountId) {
        SearchCriteria<Long> sc = CountByAccount.create();
        sc.setParameters("account", accountId);
        sc.setParameters("state", Volume.State.Destroy, Volume.State.Expunged);
        sc.setParameters("displayVolume", 1);
        return customSearch(sc, null).get(0);
    }

    @Override
    public long primaryStorageUsedForAccount(long accountId, List<Long> virtualRouters) {
        SearchCriteria<SumCount> sc;
        if (!virtualRouters.isEmpty()) {
            sc = primaryStorageSearch2.create();
            sc.setParameters("virtualRouterVmIds", virtualRouters.toArray(new Object[virtualRouters.size()]));
        } else {
            sc = primaryStorageSearch.create();
        }
        sc.setParameters("accountId", accountId);
        sc.setParameters("states", State.Allocated);
        sc.setParameters("NotCountStates", State.Destroy, State.Expunged);
        sc.setParameters("displayVolume", 1);
        List<SumCount> storageSpace = customSearch(sc, null);
        if (storageSpace != null) {
            return storageSpace.get(0).sum;
        } else {
            return 0;
        }
    }

    @Override
    public long secondaryStorageUsedForAccount(long accountId) {
        SearchCriteria<SumCount> sc = secondaryStorageSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("states", State.Allocated);
        List<SumCount> storageSpace = customSearch(sc, null);
        if (storageSpace != null) {
            return storageSpace.get(0).sum;
        } else {
            return 0;
        }
    }

    public static class SumCount {
        public long sum;
        public long count;

        public SumCount() {
        }
    }

    @Override
    public List<VolumeVO> listVolumesToBeDestroyed() {
        SearchCriteria<VolumeVO> sc = AllFieldsSearch.create();
        sc.setParameters("state", Volume.State.Destroy);

        return listBy(sc);
    }

    @Override
    public List<VolumeVO> listVolumesToBeDestroyed(Date date) {
        SearchCriteria<VolumeVO> sc = AllFieldsSearch.create();
        sc.setParameters("state", Volume.State.Destroy);
        sc.setParameters("updateTime", date);

        return listBy(sc);
    }

    @Override
    public boolean updateState(com.cloud.storage.Volume.State currentState, Event event, com.cloud.storage.Volume.State nextState, Volume vo, Object data) {

        Long oldUpdated = vo.getUpdatedCount();
        Date oldUpdatedTime = vo.getUpdated();

        SearchCriteria<VolumeVO> sc = AllFieldsSearch.create();
        sc.setParameters("id", vo.getId());
        sc.setParameters("state", currentState);
        sc.setParameters("updatedCount", vo.getUpdatedCount());

        vo.incrUpdatedCount();

        UpdateBuilder builder = getUpdateBuilder(vo);
        builder.set(vo, "state", nextState);
        builder.set(vo, "updated", new Date());

        int rows = update((VolumeVO)vo, sc);
        if (rows == 0 && s_logger.isDebugEnabled()) {
            VolumeVO dbVol = findByIdIncludingRemoved(vo.getId());
            if (dbVol != null) {
                StringBuilder str = new StringBuilder("Unable to update ").append(vo.toString());
                str.append(": DB Data={id=").append(dbVol.getId()).append("; state=").append(dbVol.getState()).append("; updatecount=").append(dbVol.getUpdatedCount()).append(";updatedTime=")
                .append(dbVol.getUpdated());
                str.append(": New Data={id=").append(vo.getId()).append("; state=").append(nextState).append("; event=").append(event).append("; updatecount=").append(vo.getUpdatedCount())
                .append("; updatedTime=").append(vo.getUpdated());
                str.append(": stale Data={id=").append(vo.getId()).append("; state=").append(currentState).append("; event=").append(event).append("; updatecount=").append(oldUpdated)
                .append("; updatedTime=").append(oldUpdatedTime);
            } else {
                s_logger.debug("Unable to update volume: id=" + vo.getId() + ", as there is no such volume exists in the database anymore");
            }
        }
        return rows > 0;
    }

    @Override
    public List<Long> listPoolIdsByVolumeCount(long dcId, Long podId, Long clusterId, long accountId) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        PreparedStatement pstmt = null;
        List<Long> result = new ArrayList<Long>();
        try {
            String sql = ORDER_POOLS_NUMBER_OF_VOLUMES_FOR_ACCOUNT;
            pstmt = txn.prepareAutoCloseStatement(sql);
            pstmt.setLong(1, accountId);
            pstmt.setLong(2, dcId);
            pstmt.setLong(3, podId);
            pstmt.setLong(4, clusterId);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                result.add(rs.getLong(1));
            }
            return result;
        } catch (SQLException e) {
            throw new CloudRuntimeException("DB Exception on: " + ORDER_POOLS_NUMBER_OF_VOLUMES_FOR_ACCOUNT, e);
        } catch (Throwable e) {
            throw new CloudRuntimeException("Caught: " + ORDER_POOLS_NUMBER_OF_VOLUMES_FOR_ACCOUNT, e);
        }
    }

    @Override
    public List<Long> listZoneWidePoolIdsByVolumeCount(long dcId, long accountId) {

        TransactionLegacy txn = TransactionLegacy.currentTxn();
        PreparedStatement pstmt = null;
        List<Long> result = new ArrayList<Long>();
        try {
            String sql = ORDER_ZONE_WIDE_POOLS_NUMBER_OF_VOLUMES_FOR_ACCOUNT;
            pstmt = txn.prepareAutoCloseStatement(sql);
            pstmt.setLong(1, accountId);
            pstmt.setLong(2, dcId);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                result.add(rs.getLong(1));
            }
            return result;
        } catch (SQLException e) {
            throw new CloudRuntimeException("DB Exception on: " + ORDER_ZONE_WIDE_POOLS_NUMBER_OF_VOLUMES_FOR_ACCOUNT, e);
        } catch (Throwable e) {
            throw new CloudRuntimeException("Caught: " + ORDER_ZONE_WIDE_POOLS_NUMBER_OF_VOLUMES_FOR_ACCOUNT, e);
        }
    }

    @Override
    @DB()
    public Pair<Long, Long> getNonDestroyedCountAndTotalByPool(long poolId) {
        SearchCriteria<SumCount> sc = TotalSizeByPoolSearch.create();
        sc.setParameters("poolId", poolId);
        sc.setParameters("state", State.Destroy);
        List<SumCount> results = customSearch(sc, null);
        SumCount sumCount = results.get(0);
        return new Pair<Long, Long>(sumCount.count, sumCount.sum);
    }

    @Override
    public long getVMSnapshotSizeByPool(long poolId) {
        SearchCriteria<SumCount> sc = TotalVMSnapshotSizeByPoolSearch.create();
        sc.setParameters("poolId", poolId);
        sc.setParameters("state", State.Destroy);
        sc.setParameters("vType", Volume.Type.ROOT.toString());
        List<SumCount> results = customSearch(sc, null);
        if (results != null) {
            return results.get(0).sum;
        } else {
            return 0;
        }
    }

    @Override
    @DB
    public boolean remove(Long id) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        VolumeVO entry = findById(id);
        if (entry != null) {
            _tagsDao.removeByIdAndType(id, ResourceObjectType.Volume);
        }
        boolean result = super.remove(id);
        txn.commit();
        return result;
    }

    @Override
    @DB
    public boolean updateUuid(long srcVolId, long destVolId) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        try {
            VolumeVO srcVol = findById(srcVolId);
            VolumeVO destVol = findById(destVolId);
            String uuid = srcVol.getUuid();
            Long instanceId = srcVol.getInstanceId();
            srcVol.setUuid(null);
            destVol.setUuid(uuid);
            destVol.setInstanceId(instanceId);
            update(srcVolId, srcVol);
            update(destVolId, destVol);
            _tagsDao.updateResourceId(srcVolId, destVolId, ResourceObjectType.Volume);
        } catch (Exception e) {
            throw new CloudRuntimeException("Unable to persist the sequence number for this host");
        }
        txn.commit();
        return true;
    }

    @Override
    public ScopeType getVolumeStoragePoolScope(long volumeId) {
        // finding the storage scope where the volume is present
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        PreparedStatement pstmt = null;

        try {
            String sql = SELECT_POOLSCOPE;
            pstmt = txn.prepareAutoCloseStatement(sql);
            pstmt.setLong(1, volumeId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String scope = rs.getString(1);
                if (scope != null) {
                    try {
                        return Enum.valueOf(ScopeType.class, scope.toUpperCase());
                    } catch (Exception e) {
                        throw new InvalidParameterValueException("invalid scope for pool " + scope);
                    }
                }
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("DB Exception on: " + SELECT_POOLSCOPE, e);
        } catch (Throwable e) {
            throw new CloudRuntimeException("Caught: " + SELECT_POOLSCOPE, e);
        }
        return null;
    }

    private String sqlUpdateDiskOffering = "UPDATE volumes SET disk_offering_id = ? where id =?";

    @Override
    public void updateDiskOffering(long volumeId, long diskOfferingId) {
        try (TransactionLegacy txn = TransactionLegacy.currentTxn();
                PreparedStatement pstmt = txn.prepareAutoCloseStatement(sqlUpdateDiskOffering)) {
            pstmt.setLong(1, diskOfferingId);
            pstmt.setLong(2, volumeId);
            pstmt.executeUpdate();
            txn.commit();
        } catch (SQLException e) {
            throw new CloudRuntimeException(e);
        }
    }
}
