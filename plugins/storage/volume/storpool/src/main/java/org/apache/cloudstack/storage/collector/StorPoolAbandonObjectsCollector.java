/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.storage.collector;

import com.cloud.dc.dao.ClusterDao;
import com.cloud.storage.dao.SnapshotDetailsDao;
import com.cloud.storage.dao.SnapshotDetailsVO;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.TransactionStatus;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.util.StorPoolHelper;
import org.apache.cloudstack.storage.datastore.util.StorPoolUtil;
import org.apache.cloudstack.storage.datastore.util.StorPoolUtil.SpApiResponse;
import org.apache.cloudstack.storage.datastore.util.StorPoolUtil.SpConnectionDesc;
import org.apache.commons.collections.CollectionUtils;

import javax.inject.Inject;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StorPoolAbandonObjectsCollector extends ManagerBase implements Configurable {
    @Inject
    private PrimaryDataStoreDao storagePoolDao;
    @Inject
    private StoragePoolDetailsDao storagePoolDetailsDao;
    @Inject
    private SnapshotDetailsDao snapshotDetailsDao;
    @Inject
    private ClusterDao clusterDao;

    private ScheduledExecutorService _volumeTagsUpdateExecutor;
    private ScheduledExecutorService snapshotRecoveryCheckExecutor;
    private static final String ABANDON_LOGGER = "/var/log/cloudstack/management/storpool-abandoned-objects";


    static final ConfigKey<Integer> volumeCheckupTagsInterval = new ConfigKey<Integer>("Advanced", Integer.class,
            "storpool.volume.tags.checkup", "86400",
            "Minimal interval (in seconds) to check and report if StorPool volume exists in CloudStack volumes database",
            false);
    static final ConfigKey<Integer> snapshotCheckupTagsInterval = new ConfigKey<Integer>("Advanced", Integer.class,
            "storpool.snapshot.tags.checkup", "86400",
            "Minimal interval (in seconds) to check and report if StorPool snapshot exists in CloudStack snapshots database",
            false);
    static final ConfigKey<Integer> snapshotRecoveryFromRemoteCheck = new ConfigKey<Integer>("Advanced", Integer.class,
            "storpool.snapshot.recovery.from.remote.check", "300",
            "Minimal interval (in seconds) to check and recover StorPool snapshot from remote", false);

    @Override
    public String getConfigComponentName() {
        return StorPoolAbandonObjectsCollector.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] { volumeCheckupTagsInterval, snapshotCheckupTagsInterval, snapshotRecoveryFromRemoteCheck };
    }

    @Override
    public boolean start() {
        init();
        return true;
    }

    private void init() {
        List<StoragePoolVO> spPools = storagePoolDao.findPoolsByProvider(StorPoolUtil.SP_PROVIDER_NAME);
        if (CollectionUtils.isNotEmpty(spPools)) {
//            StorPoolHelper.appendLogger(logger, ABANDON_LOGGER, "abandon");
        }
        _volumeTagsUpdateExecutor = Executors.newScheduledThreadPool(2,
                new NamedThreadFactory("StorPoolAbandonObjectsCollector"));
        snapshotRecoveryCheckExecutor = Executors.newScheduledThreadPool(1,
                new NamedThreadFactory("StorPoolSnapshotRecoveryCheck"));

        if (volumeCheckupTagsInterval.value() > 0) {
            _volumeTagsUpdateExecutor.scheduleAtFixedRate(new StorPoolVolumesTagsUpdate(),
                    volumeCheckupTagsInterval.value(), volumeCheckupTagsInterval.value(), TimeUnit.SECONDS);
        }
        if (snapshotCheckupTagsInterval.value() > 0) {
            _volumeTagsUpdateExecutor.scheduleAtFixedRate(new StorPoolSnapshotsTagsUpdate(),
                    snapshotCheckupTagsInterval.value(), snapshotCheckupTagsInterval.value(), TimeUnit.SECONDS);
        }
        if (snapshotRecoveryFromRemoteCheck.value() > 0) {
            snapshotRecoveryCheckExecutor.scheduleAtFixedRate(new StorPoolSnapshotRecoveryCheck(),
                    snapshotRecoveryFromRemoteCheck.value(), snapshotRecoveryFromRemoteCheck.value(), TimeUnit.SECONDS);
        }
    }

    class StorPoolVolumesTagsUpdate extends ManagedContextRunnable {

        @Override
        @DB
        protected void runInContext() {
            List<StoragePoolVO> spPools = storagePoolDao.findPoolsByProvider(StorPoolUtil.SP_PROVIDER_NAME);
            if (CollectionUtils.isEmpty(spPools)) {
                return;
            }
            Map<String, String> volumes = new HashMap<>();
            for (StoragePoolVO storagePoolVO : spPools) {
                try {
                    JsonArray arr = StorPoolUtil.volumesList(StorPoolUtil.getSpConnection(storagePoolVO.getUuid(), storagePoolVO.getId(), storagePoolDetailsDao, storagePoolDao));
                    volumes.putAll(getStorPoolNamesAndCsTag(arr));
                } catch (Exception e) {
                    logger.debug(String.format("Could not collect abandon objects due to %s", e.getMessage()), e);
                }
            }
            Transaction.execute(new TransactionCallbackNoReturn() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) {
                    TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);

                    try {
                        PreparedStatement pstmt = txn.prepareAutoCloseStatement(
                                "CREATE TEMPORARY TABLE `cloud`.`volumes1`(`id` bigint unsigned NOT NULL auto_increment, `name` varchar(255) NOT NULL,`tag` varchar(255) NOT NULL, PRIMARY KEY (`id`))");
                        pstmt.executeUpdate();

                        pstmt = txn.prepareAutoCloseStatement(
                                "CREATE TEMPORARY TABLE `cloud`.`volumes_on_host1`(`id` bigint unsigned NOT NULL auto_increment, `name` varchar(255) NOT NULL,`tag` varchar(255) NOT NULL, PRIMARY KEY (`id`))");
                        pstmt.executeUpdate();

                    } catch (SQLException e) {
                        logger.info(String.format("[ignored] SQL failed to delete vm work job: %s ",
                                e.getLocalizedMessage()));
                    } catch (Throwable e) {
                        logger.info(String.format("[ignored] caught an error during delete vm work job: %s",
                                e.getLocalizedMessage()));
                    }

                    try {
                        PreparedStatement pstmt = txn.prepareStatement("INSERT INTO `cloud`.`volumes1` (name, tag) VALUES (?, ?)");
                        PreparedStatement volumesOnHostpstmt = txn.prepareStatement("INSERT INTO `cloud`.`volumes_on_host1` (name, tag) VALUES (?, ?)");
                        for (Map.Entry<String, String> volume : volumes.entrySet()) {
                            if (volume.getValue().equals("volume")) {
                                addRecordToDb(volume.getKey(), pstmt, volume.getValue(), true);
                            } else if (volume.getValue().equals("check-volume-is-on-host")) {
                                addRecordToDb(volume.getKey(), volumesOnHostpstmt, volume.getValue(), true);
                            }
                        }
                        pstmt.executeBatch();
                        volumesOnHostpstmt.executeBatch();
                        String sql = "SELECT f.* FROM `cloud`.`volumes1` f LEFT JOIN `cloud`.`volumes` v ON f.name=v.path where v.path is NULL OR NOT state=?";
                        findMissingRecordsInCS(txn, sql, "volume");

                        String sqlVolumeOnHost = "SELECT f.* FROM `cloud`.`volumes_on_host1` f LEFT JOIN `cloud`.`storage_pool_details` v ON f.name=v.value where v.value is NULL";
                        findMissingRecordsInCS(txn, sqlVolumeOnHost, "volumes_on_host");
                    } catch (SQLException e) {
                        logger.info(String.format("[ignored] SQL failed due to: %s ",
                                e.getLocalizedMessage()));
                    } catch (Throwable e) {
                        logger.info(String.format("[ignored] caught an error: %s",
                                e.getLocalizedMessage()));
                    } finally {
                        try {
                            PreparedStatement pstmt = txn.prepareStatement("DROP TABLE `cloud`.`volumes1`");
                            pstmt.executeUpdate();
                            pstmt = txn.prepareStatement("DROP TABLE `cloud`.`volumes_on_host1`");
                            pstmt.executeUpdate();
                        } catch (SQLException e) {
                            txn.close();
                            logger.info(String.format("createTemporaryVolumeTable %s", e.getMessage()));
                        }
                        txn.close();
                    }
                }
            });
        }
    }

    class StorPoolSnapshotsTagsUpdate extends ManagedContextRunnable {

        @Override
        @DB
        protected void runInContext() {
            List<StoragePoolVO> spPools = storagePoolDao.findPoolsByProvider(StorPoolUtil.SP_PROVIDER_NAME);
            Map<String, String> snapshots = new HashMap<String, String>();
            if (CollectionUtils.isEmpty(spPools)) {
                return;
            }
            for (StoragePoolVO storagePoolVO : spPools) {
                try {
                    JsonArray arr = StorPoolUtil.snapshotsList(StorPoolUtil.getSpConnection(storagePoolVO.getUuid(), storagePoolVO.getId(), storagePoolDetailsDao, storagePoolDao));
                    snapshots.putAll(getStorPoolNamesAndCsTag(arr));
                } catch (Exception e) {
                    logger.debug(String.format("Could not collect abandon objects due to %s", e.getMessage()));
                }
            }
            Transaction.execute(new TransactionCallbackNoReturn() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) {
                    TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);

                    try{
                        PreparedStatement pstmt = txn.prepareAutoCloseStatement(
                                "CREATE TEMPORARY TABLE `cloud`.`snapshots1`(`id` bigint unsigned NOT NULL auto_increment, `name` varchar(255) NOT NULL,`tag` varchar(255) NOT NULL, PRIMARY KEY (`id`))");
                        pstmt.executeUpdate();

                        pstmt = txn.prepareAutoCloseStatement(
                                "CREATE TEMPORARY TABLE `cloud`.`vm_snapshots1`(`id` bigint unsigned NOT NULL auto_increment, `name` varchar(255) NOT NULL,`tag` varchar(255) NOT NULL, PRIMARY KEY (`id`))");
                        pstmt.executeUpdate();

                        pstmt = txn.prepareAutoCloseStatement(
                                "CREATE TEMPORARY TABLE `cloud`.`vm_templates1`(`id` bigint unsigned NOT NULL auto_increment, `name` varchar(255) NOT NULL,`tag` varchar(255) NOT NULL, PRIMARY KEY (`id`))");
                        pstmt.executeUpdate();
                    } catch (SQLException e) {
                        logger.info(String.format("[ignored] SQL failed to delete vm work job: %s ",
                                e.getLocalizedMessage()));
                    } catch (Throwable e) {
                        logger.info(String.format("[ignored] caught an error during delete vm work job: %s",
                                e.getLocalizedMessage()));
                    }

                    try {
                        PreparedStatement snapshotsPstmt = txn.prepareStatement("INSERT INTO `cloud`.`snapshots1` (name, tag) VALUES (?, ?)");
                        PreparedStatement groupSnapshotsPstmt = txn.prepareStatement("INSERT INTO `cloud`.`vm_snapshots1` (name, tag) VALUES (?, ?)");
                        PreparedStatement templatePstmt = txn.prepareStatement("INSERT INTO `cloud`.`vm_templates1` (name, tag) VALUES (?, ?)");
                        for (Map.Entry<String, String> snapshot : snapshots.entrySet()) {
                            if (!snapshot.getValue().equals("group") && !snapshot.getValue().equals("template")) {
                                addRecordToDb(snapshot.getKey(), snapshotsPstmt, snapshot.getValue(), true);
                            } else if (snapshot.getValue().equals("group")) {
                                addRecordToDb(snapshot.getKey(), groupSnapshotsPstmt, snapshot.getValue(), true);
                            } else if (snapshot.getValue().equals("template")) {
                                addRecordToDb(snapshot.getKey(), templatePstmt, snapshot.getValue(), true);
                            }
                        }
                        snapshotsPstmt.executeBatch();
                        groupSnapshotsPstmt.executeBatch();
                        templatePstmt.executeBatch();

                        String sqlSnapshots = "SELECT f.* FROM `cloud`.`snapshots1` f LEFT JOIN `cloud`.`snapshot_details` v ON f.name=v.value where v.value is NULL";
                        findMissingRecordsInCS(txn, sqlSnapshots, "snapshot");

                        String sqlVmSnapshots = "SELECT f.* FROM `cloud`.`vm_snapshots1` f LEFT JOIN `cloud`.`vm_snapshot_details` v ON f.name=v.value where v.value is NULL";
                        findMissingRecordsInCS(txn, sqlVmSnapshots, "snapshot");

                        String sqlTemplates = "SELECT temp.*"
                                + " FROM `cloud`.`vm_templates1` temp"
                                + " LEFT JOIN `cloud`.`template_store_ref` store"
                                + " ON temp.name=store.local_path"
                                + " LEFT JOIN `cloud`.`template_spool_ref` spool"
                                + " ON temp.name=spool.local_path"
                                + " where store.local_path is NULL"
                                + " and spool.local_path is NULL";
                        findMissingRecordsInCS(txn, sqlTemplates, "snapshot");
                    } catch (SQLException e) {
                        logger.info(String.format("[ignored] SQL failed due to: %s ",
                                e.getLocalizedMessage()));
                    } catch (Throwable e) {
                        logger.info(String.format("[ignored] caught an error: %s",
                                e.getLocalizedMessage()));
                    } finally {
                        try {
                            PreparedStatement pstmt = txn.prepareStatement("DROP TABLE `cloud`.`snapshots1`");
                            pstmt.executeUpdate();
                            pstmt = txn.prepareStatement("DROP TABLE `cloud`.`vm_snapshots1`");
                            pstmt.executeUpdate();
                            pstmt = txn.prepareStatement("DROP TABLE `cloud`.`vm_templates1`");
                            pstmt.executeUpdate();
                        } catch (SQLException e) {
                            txn.close();
                            logger.info(String.format("createTemporaryVolumeTable %s", e.getMessage()));
                        }
                        txn.close();
                    }
                }
            });
        }
    }

    private void addRecordToDb(String name, PreparedStatement pstmt, String tag, boolean pathNeeded)
            throws SQLException {
        name = name.startsWith("~") ? name.split("~")[1] : name;
        pstmt.setString(1, pathNeeded ? StorPoolUtil.devPath(name) : name);
        pstmt.setString(2, tag);
        pstmt.addBatch();
    }

    private void findMissingRecordsInCS(TransactionLegacy txn, String sql, String object) throws SQLException {
        ResultSet rs;
        PreparedStatement pstmt2 = txn.prepareStatement(sql);
        if (object.equals("volume")) {
            pstmt2.setString(1, "Ready");
        }
        rs = pstmt2.executeQuery();
        String name = null;
        while (rs.next()) {
            name = rs.getString(2);
            logger.info(String.format(
                    "CloudStack does not know about StorPool %s %s, it had to be a %s", object, name, rs.getString(3)));
        }
    }

    private Map<String,String> getStorPoolNamesAndCsTag(JsonArray arr) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < arr.size(); i++) {
            String name = arr.get(i).getAsJsonObject().get("name").getAsString();
            String tag = null;
            if (!name.startsWith("*") && !name.contains("@")) {
                JsonObject tags = arr.get(i).getAsJsonObject().get("tags").getAsJsonObject();
                if (tags != null && tags.getAsJsonPrimitive("cs") != null && !(arr.get(i).getAsJsonObject().get("deleted") != null && arr.get(i).getAsJsonObject().get("deleted").getAsBoolean())) {
                    tag = tags.getAsJsonPrimitive("cs").getAsString();
                    map.put(name, tag);
                }
            }
        }
        return map;
    }

    class StorPoolSnapshotRecoveryCheck extends ManagedContextRunnable {

        @Override
        protected void runInContext() {
            List<StoragePoolVO> spPools = storagePoolDao.findPoolsByProvider(StorPoolUtil.SP_PROVIDER_NAME);
            if (CollectionUtils.isEmpty(spPools)) {
                return;
            }
            List<SnapshotDetailsVO> snapshotDetails = snapshotDetailsDao.findDetails(StorPoolUtil.SP_RECOVERED_SNAPSHOT);
            if (CollectionUtils.isEmpty(snapshotDetails)) {
                return;
            }
            Map<Long, StoragePoolVO> onePoolforZone = new HashMap<>();
            for (StoragePoolVO storagePoolVO : spPools) {
                onePoolforZone.put(storagePoolVO.getDataCenterId(), storagePoolVO);
            }
            List<Long> recoveredSnapshots = new ArrayList<>();
            for (StoragePoolVO storagePool : onePoolforZone.values()) {
                try {
                    logger.debug(String.format("Checking StorPool recovered snapshots for zone [%s]",
                            storagePool.getDataCenterId()));
                    SpConnectionDesc conn = StorPoolUtil.getSpConnection(storagePool.getUuid(),
                            storagePool.getId(), storagePoolDetailsDao, storagePoolDao);
                    JsonArray arr = StorPoolUtil.snapshotsList(conn);
                    List<String> snapshots = snapshotsForRcovery(arr);
                    if (snapshots.isEmpty()) {
                        continue;
                    }
                    for (SnapshotDetailsVO snapshot : snapshotDetails) {
                        String name = snapshot.getValue().split(";")[0];
                        String location = snapshot.getValue().split(";")[1];
                        if (name == null || location == null) {
                            StorPoolUtil.spLog("Could not find name or location for the snapshot %s", snapshot.getValue());
                            continue;
                        }
                        if (snapshots.contains(name)) {
                            Long clusterId = StorPoolHelper.findClusterIdByGlobalId(StorPoolUtil.getSnapshotClusterId(name, conn), clusterDao);
                            conn = StorPoolHelper.getSpConnectionDesc(conn, clusterId);
                            SpApiResponse resp = StorPoolUtil.snapshotUnexport(name, location, conn);
                            if (resp.getError() == null) {
                                StorPoolUtil.spLog("Unexport of snapshot %s was successful", name);
                                recoveredSnapshots.add(snapshot.getId());
                            } else {
                                StorPoolUtil.spLog("Could not recover StorPool snapshot %s", resp.getError());
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.debug(String.format("Could not collect StorPool recovered snapshots %s", e.getMessage()));
                }
            }
            for (Long recoveredSnapshot : recoveredSnapshots) {
                snapshotDetailsDao.remove(recoveredSnapshot);
            }
        }
    }

    private static List<String> snapshotsForRcovery(JsonArray arr) {
        List<String> snapshots = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            boolean recoveringFromRemote = arr.get(i).getAsJsonObject().get("recoveringFromRemote").getAsBoolean();
            if (!recoveringFromRemote) {
               snapshots.add(arr.get(i).getAsJsonObject().get("name").getAsString());
            }
        }
        return snapshots;
    }
}
