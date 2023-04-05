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

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.util.StorPoolUtil;
import org.apache.cloudstack.storage.snapshot.StorPoolConfigurationManager;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class StorPoolStatsCollector extends ManagerBase {

    private static Logger log = Logger.getLogger(StorPoolStatsCollector.class);

    @Inject
    private PrimaryDataStoreDao storagePoolDao;
    @Inject
    private StoragePoolDetailsDao storagePoolDetailsDao;
    @Inject
    private ConfigurationDao configurationDao;

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
            executor = Executors.newScheduledThreadPool(2,new NamedThreadFactory("StorPoolStatsCollector"));
            long storageStatsInterval = NumbersUtil.parseLong(configurationDao.getValue("storage.stats.interval"), 60000L);
            long volumeStatsInterval = NumbersUtil.parseLong(configurationDao.getValue("volume.stats.interval"), 60000L);

            if (StorPoolConfigurationManager.VolumesStatsInterval.value() > 0 && volumeStatsInterval > 0) {
                executor.scheduleAtFixedRate(new StorPoolVolumeStatsMonitorTask(),120, StorPoolConfigurationManager.VolumesStatsInterval.value(), TimeUnit.SECONDS);
            }
            if (StorPoolConfigurationManager.StorageStatsInterval.value() > 0 && storageStatsInterval > 0) {
                executor.scheduleAtFixedRate(new StorPoolStorageStatsMonitorTask(), 120, StorPoolConfigurationManager.StorageStatsInterval.value(), TimeUnit.SECONDS);
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

                log.debug("Collecting StorPool volumes used space");
                Map<Long, StoragePoolVO> onePoolforZone = new HashMap<>();
                for (StoragePoolVO storagePoolVO : spPools) {
                    onePoolforZone.put(storagePoolVO.getDataCenterId(), storagePoolVO);
                }
                for (StoragePoolVO storagePool : onePoolforZone.values()) {
                    try {
                        log.debug(String.format("Collecting volumes statistics for zone [%s]", storagePool.getDataCenterId()));
                        JsonArray arr = StorPoolUtil.volumesSpace(StorPoolUtil.getSpConnection(storagePool.getUuid(),
                                storagePool.getId(), storagePoolDetailsDao, storagePoolDao));
                        volumesStats.putAll(getClusterVolumeOrTemplateSpace(arr, StorPoolObject.VOLUME));
                    } catch (Exception e) {
                        log.debug(String.format("Could not collect StorPool volumes statistics due to %s", e.getMessage()));
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
                        log.debug(String.format("Collecting templates statistics for zone [%s]", storagePool.getDataCenterId()));
                        JsonArray arr = StorPoolUtil.templatesStats(StorPoolUtil.getSpConnection(storagePool.getUuid(),
                                storagePool.getId(), storagePoolDetailsDao, storagePoolDao));
                        templatesStats.put(storagePool.getDataCenterId(), getClusterVolumeOrTemplateSpace(arr, StorPoolObject.TEMPLATE));
                    } catch (Exception e) {
                        log.debug(String.format("Could not collect StorPool templates statistics %s", e.getMessage()));
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
}