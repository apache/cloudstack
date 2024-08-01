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
package org.apache.cloudstack.storage.datastore.driver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.State;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.util.StorPoolUtil;
import org.apache.cloudstack.storage.snapshot.StorPoolConfigurationManager;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.storage.DataStoreRole;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotDetailsDao;
import com.cloud.storage.dao.SnapshotDetailsVO;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class StorPoolStatsCollector extends ManagerBase {

    @Inject
    private PrimaryDataStoreDao storagePoolDao;
    @Inject
    private StoragePoolDetailsDao storagePoolDetailsDao;
    @Inject
    private ConfigurationDao configurationDao;
    @Inject
    private SnapshotDao snapshotDao;
    @Inject
    private SnapshotDataStoreDao snapshotDataStoreDao;
    @Inject
    private SnapshotDetailsDao snapshotDetailsDao;

    private ScheduledExecutorService executor;

    static volatile Map<String, Pair<Long, Long>> volumesStats = new ConcurrentHashMap<>();
    static volatile Map<Long, Map<String, Pair<Long, Long>>> templatesStats = new ConcurrentHashMap<>();


    enum StorPoolObject {
        VOLUME, TEMPLATE;
    }

    @Override
    public boolean start() {
        List<StoragePoolVO> spPools = storagePoolDao.findPoolsByProvider(StorPoolUtil.SP_PROVIDER_NAME);
        if (CollectionUtils.isNotEmpty(spPools)) {
            executor = Executors.newScheduledThreadPool(3, new NamedThreadFactory("StorPoolStatsCollector"));
            long storageStatsInterval = NumbersUtil.parseLong(configurationDao.getValue("storage.stats.interval"), 60000L);
            long volumeStatsInterval = NumbersUtil.parseLong(configurationDao.getValue("volume.stats.interval"), 60000L);

            if (StorPoolConfigurationManager.VolumesStatsInterval.value() > 0 && volumeStatsInterval > 0) {
                executor.scheduleAtFixedRate(new StorPoolVolumeStatsMonitorTask(),120, StorPoolConfigurationManager.VolumesStatsInterval.value(), TimeUnit.SECONDS);
            }
            if (StorPoolConfigurationManager.StorageStatsInterval.value() > 0 && storageStatsInterval > 0) {
                executor.scheduleAtFixedRate(new StorPoolStorageStatsMonitorTask(), 120, StorPoolConfigurationManager.StorageStatsInterval.value(), TimeUnit.SECONDS);
            }
            for (StoragePoolVO pool: spPools) {
                Integer deleteAfter = StorPoolConfigurationManager.DeleteAfterInterval.valueIn(pool.getId());
                if (deleteAfter != null && deleteAfter > 0) {
                    executor.scheduleAtFixedRate(new StorPoolSnapshotsWithDelayDelete(), 120, StorPoolConfigurationManager.ListSnapshotsWithDeleteAfterInterval.value(), TimeUnit.SECONDS);
                    break;
                }
            }
        }

        return true;
    }

    class StorPoolVolumeStatsMonitorTask implements Runnable {

        @Override
        public void run() {
            List<StoragePoolVO> spPools = storagePoolDao.findPoolsByProvider(StorPoolUtil.SP_PROVIDER_NAME);
            if (CollectionUtils.isNotEmpty(spPools)) {
                volumesStats.clear();

                logger.debug("Collecting StorPool volumes used space");
                Map<Long, StoragePoolVO> onePoolforZone = new HashMap<>();
                for (StoragePoolVO storagePoolVO : spPools) {
                    onePoolforZone.put(storagePoolVO.getDataCenterId(), storagePoolVO);
                }
                for (StoragePoolVO storagePool : onePoolforZone.values()) {
                    try {
                        logger.debug(String.format("Collecting volumes statistics for zone [%s]", storagePool.getDataCenterId()));
                        JsonArray arr = StorPoolUtil.volumesSpace(StorPoolUtil.getSpConnection(storagePool.getUuid(),
                                storagePool.getId(), storagePoolDetailsDao, storagePoolDao));
                        volumesStats.putAll(getClusterVolumeOrTemplateSpace(arr, StorPoolObject.VOLUME));
                    } catch (Exception e) {
                        logger.debug(String.format("Could not collect StorPool volumes statistics due to %s", e.getMessage()));
                    }
                }
            }
        }
    }

    class StorPoolStorageStatsMonitorTask implements Runnable {

        @Override
        public void run() {
            List<StoragePoolVO> spPools = storagePoolDao.findPoolsByProvider(StorPoolUtil.SP_PROVIDER_NAME);
            if (CollectionUtils.isNotEmpty(spPools)) {
                templatesStats.clear();

                Map<Long, StoragePoolVO> onePoolforZone = new HashMap<>();
                for (StoragePoolVO storagePoolVO : spPools) {
                    onePoolforZone.put(storagePoolVO.getDataCenterId(), storagePoolVO);
                }
                for (StoragePoolVO storagePool : onePoolforZone.values()) {
                    try {
                        logger.debug(String.format("Collecting templates statistics for zone [%s]", storagePool.getDataCenterId()));
                        JsonArray arr = StorPoolUtil.templatesStats(StorPoolUtil.getSpConnection(storagePool.getUuid(),
                                storagePool.getId(), storagePoolDetailsDao, storagePoolDao));
                        templatesStats.put(storagePool.getDataCenterId(), getClusterVolumeOrTemplateSpace(arr, StorPoolObject.TEMPLATE));
                    } catch (Exception e) {
                        logger.debug(String.format("Could not collect StorPool templates statistics %s", e.getMessage()));
                    }
                }
            }
        }
    }

    private Map<String, Pair<Long, Long>> getClusterVolumeOrTemplateSpace(JsonArray arr, StorPoolObject spObject) {
        Map<String, Pair<Long, Long>> map = new HashMap<>();
        for (JsonElement jsonElement : arr) {
            JsonObject name = jsonElement.getAsJsonObject().getAsJsonObject("response");
            if (name != null) {
                JsonArray data = name.getAsJsonObject().getAsJsonArray("data");
                if (StorPoolObject.VOLUME == spObject) {
                    map.putAll(getStatsForVolumes(data));
                } else if (StorPoolObject.TEMPLATE == spObject) {
                    getClusterStats(data, map);
                }
            } else if (StorPoolObject.TEMPLATE == spObject) {
                return map;
            }
        }
        return map;
    }

    private Map<String, Pair<Long, Long>> getStatsForVolumes(JsonArray arr) {
        Map<String, Pair<Long, Long>> map = new HashMap<>();
        for (int i = 0; i < arr.size(); i++) {
            String name = arr.get(i).getAsJsonObject().get("name").getAsString();
            if (!name.startsWith("*") && !name.contains("@")) {
                Long spaceUsed = arr.get(i).getAsJsonObject().get("spaceUsed").getAsLong();
                Long size = arr.get(i).getAsJsonObject().get("size").getAsLong();
                map.put(name, new Pair<>(spaceUsed, size));
            }
        }
        return map;
    }

    private void getClusterStats(JsonArray data, Map<String, Pair<Long, Long>> map) {
        for (JsonElement dat : data) {
            long capacity = dat.getAsJsonObject().get("stored").getAsJsonObject().get("capacity").getAsLong();
            long free = dat.getAsJsonObject().get("stored").getAsJsonObject().get("free").getAsLong();
            long used = capacity - free;
            String templateName = dat.getAsJsonObject().get("name").getAsString();
            if (!map.containsKey(templateName)) {
                map.put(templateName, new Pair<>(capacity, used));
            } else {
                Pair<Long, Long> template = map.get(templateName);
                template.first(template.first() + capacity);
                template.second(template.second() + used);
                map.put(templateName, template);
            }
        }
    }

    class StorPoolSnapshotsWithDelayDelete implements Runnable {

        @Override
        public void run() {
            List<StoragePoolVO> spPools = storagePoolDao.findPoolsByProvider(StorPoolUtil.SP_PROVIDER_NAME);
            if (CollectionUtils.isNotEmpty(spPools)) {
                Map<Long, StoragePoolVO> onePoolForZone = new HashMap<>();
                for (StoragePoolVO storagePoolVO : spPools) {
                    onePoolForZone.put(storagePoolVO.getDataCenterId(), storagePoolVO);
                }
                for (StoragePoolVO storagePool : onePoolForZone.values()) {
                    List<SnapshotDetailsVO> snapshotsDetails = snapshotDetailsDao.findDetailsByZoneAndKey(storagePool.getDataCenterId(), StorPoolUtil.SP_DELAY_DELETE);
                    if (CollectionUtils.isEmpty(snapshotsDetails)) {
                        return;
                    }
                    Map<String, String> snapshotsWithDelayDelete = new HashMap<>();

                    try {
                        logger.debug(String.format("Collecting snapshots marked to be deleted for zone [%s]", storagePool.getDataCenterId()));
                        JsonArray arr = StorPoolUtil.snapshotsListAllClusters(StorPoolUtil.getSpConnection(storagePool.getUuid(),
                                storagePool.getId(), storagePoolDetailsDao, storagePoolDao));
                         snapshotsWithDelayDelete.putAll(getSnapshotsMarkedForDeletion(arr));
                         logger.debug(String.format("Found snapshot details [%s] and snapshots on StorPool with delay delete flag [%s]", snapshotsDetails, snapshotsWithDelayDelete));
                         syncSnapshots(snapshotsDetails, snapshotsWithDelayDelete);
                    } catch (Exception e) {
                        logger.debug("Could not fetch the snapshots with delay delete flag " + e.getMessage());
                    }
                }
            }
        }

        private void syncSnapshots(List<SnapshotDetailsVO> snapshotsDetails,
                Map<String, String> snapshotsWithDelayDelete) {
            for (SnapshotDetailsVO snapshotDetailsVO : snapshotsDetails) {
                 if (!snapshotsWithDelayDelete.containsKey(snapshotDetailsVO.getValue())) {
                     StorPoolUtil.spLog("The snapshot [%s] with delayDelete flag is no longer on StorPool. Removing it from CloudStack", snapshotDetailsVO.getValue());
                     SnapshotDataStoreVO ss = snapshotDataStoreDao
                             .findBySourceSnapshot(snapshotDetailsVO.getResourceId(), DataStoreRole.Primary);
                     if (ss != null) {
                         ss.setState(State.Destroyed);
                         snapshotDataStoreDao.update(ss.getId(), ss);
                     }
                     SnapshotVO snap = snapshotDao.findById(snapshotDetailsVO.getResourceId());
                     if (snap != null) {
                         snap.setState(com.cloud.storage.Snapshot.State.Destroyed);
                         snapshotDao.update(snap.getId(), snap);
                     }
                     snapshotDetailsDao.remove(snapshotDetailsVO.getId());
                 }
             }
        }

        private  Map<String, String> getSnapshotsMarkedForDeletion(JsonArray arr) {
            for (JsonElement jsonElement : arr) {
                JsonObject error = jsonElement.getAsJsonObject().getAsJsonObject("error");
                if (error != null) {
                    throw new CloudRuntimeException(String.format("Could not collect the snapshots marked for deletion from all storage nodes due to: [%s]", error));
                }
            }
            Map<String, String> snapshotsWithDelayDelete = new HashMap<>();
            for (JsonElement jsonElement : arr) {
                JsonObject response = jsonElement.getAsJsonObject().getAsJsonObject("response");
                if (response == null) {
                    return snapshotsWithDelayDelete;
                }
                collectSnapshots(snapshotsWithDelayDelete, response);
            }
            logger.debug("Found snapshots on StorPool" + snapshotsWithDelayDelete);
            return snapshotsWithDelayDelete;
        }

        private void collectSnapshots(Map<String, String> snapshotsWithDelayDelete, JsonObject response) {
            JsonArray snapshots = response.getAsJsonObject().getAsJsonArray("data");
            for (JsonElement snapshot : snapshots) {
                String name = snapshot.getAsJsonObject().get("name").getAsString();
                JsonObject tags = snapshot.getAsJsonObject().get("tags").getAsJsonObject();
                if (!StringUtils.startsWith(name, "*") && StringUtils.containsNone(name, "@") && tags != null && !tags.entrySet().isEmpty()) {
                    String tag = tags.getAsJsonPrimitive("cs").getAsString();
                    if (tag != null && tag.equals(StorPoolUtil.DELAY_DELETE)) {
                        snapshotsWithDelayDelete.put(name, tag);
                    }
                }
            }
        }
    }
}
