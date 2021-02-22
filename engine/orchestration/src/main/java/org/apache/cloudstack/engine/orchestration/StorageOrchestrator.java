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

package org.apache.cloudstack.engine.orchestration;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.response.MigrationResponse;
import org.apache.cloudstack.engine.orchestration.service.StorageOrchestrationService;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.SecondaryStorageService;
import org.apache.cloudstack.engine.subsystem.api.storage.SecondaryStorageService.DataObjectResult;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.ImageStoreService.MigrationPolicy;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreDao;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.log4j.Logger;

import com.cloud.server.StatsCollector;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StorageService;
import com.cloud.storage.StorageStats;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;

public class StorageOrchestrator extends ManagerBase implements StorageOrchestrationService, Configurable {

    private static final Logger s_logger = Logger.getLogger(StorageOrchestrator.class);
    @Inject
    SnapshotDataStoreDao snapshotDataStoreDao;
    @Inject
    SnapshotDao snapshotDao;
    @Inject
    SnapshotDataFactory snapshotFactory;
    @Inject
    DataStoreManager dataStoreManager;
    @Inject
    StatsCollector statsCollector;
    @Inject
    public StorageService storageService;
    @Inject
    ConfigurationDao configDao;
    @Inject
    private SecondaryStorageService secStgSrv;
    @Inject
    TemplateDataStoreDao templateDataStoreDao;
    @Inject
    VolumeDataStoreDao volumeDataStoreDao;
    @Inject
    DataMigrationUtility migrationHelper;

    ConfigKey<Double> ImageStoreImbalanceThreshold = new ConfigKey<>("Advanced", Double.class,
            "image.store.imbalance.threshold",
            "0.3",
            "The storage imbalance threshold that is compared with the standard deviation percentage for a storage utilization metric. " +
                    "The value is a percentage in decimal format.",
            true, ConfigKey.Scope.Global);

    Integer numConcurrentCopyTasksPerSSVM = 2;
    private double imageStoreCapacityThreshold = 0.90;

    @Override
    public String getConfigComponentName() {
        return StorageOrchestrationService.class.getName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[]{ImageStoreImbalanceThreshold};
    }

    static class MigrateBlockingQueue<T> extends ArrayBlockingQueue<T> {

        MigrateBlockingQueue(int size) {
            super(size);
        }

        public boolean offer(T task) {
            try {
                this.put(task);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return true;
        }
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        numConcurrentCopyTasksPerSSVM = StorageManager.SecStorageMaxMigrateSessions.value();
        return true;
    }

    @Override
    public MigrationResponse migrateData(Long srcDataStoreId, List<Long> destDatastores, MigrationPolicy migrationPolicy) {
        List<DataObject> files = new LinkedList<>();
        boolean success = true;
        String message = null;

        migrationHelper.checkIfCompleteMigrationPossible(migrationPolicy, srcDataStoreId);
        DataStore srcDatastore = dataStoreManager.getDataStore(srcDataStoreId, DataStoreRole.Image);
        Map<DataObject, Pair<List<SnapshotInfo>, Long>> snapshotChains = new HashMap<>();
        files = migrationHelper.getSortedValidSourcesList(srcDatastore, snapshotChains);

        if (files.isEmpty()) {
            return new MigrationResponse("No files in Image store "+srcDatastore.getId()+ " to migrate", migrationPolicy.toString(), true);
        }
        Map<Long, Pair<Long, Long>> storageCapacities = new Hashtable<>();
        for (Long storeId : destDatastores) {
            storageCapacities.put(storeId, new Pair<>(null, null));
        }
        storageCapacities.put(srcDataStoreId, new Pair<>(null, null));
        if (migrationPolicy == MigrationPolicy.COMPLETE) {
            s_logger.debug("Setting source image store "+srcDatastore.getId()+ " to read-only");
            storageService.updateImageStoreStatus(srcDataStoreId, true);
        }

        storageCapacities = getStorageCapacities(storageCapacities, srcDataStoreId);
        double meanstddev = getStandardDeviation(storageCapacities);
        double threshold = ImageStoreImbalanceThreshold.value();
        MigrationResponse response = null;
        ThreadPoolExecutor executor = new ThreadPoolExecutor(numConcurrentCopyTasksPerSSVM , numConcurrentCopyTasksPerSSVM, 30,
                TimeUnit.MINUTES, new MigrateBlockingQueue<>(numConcurrentCopyTasksPerSSVM));
        Date start = new Date();
        if (meanstddev < threshold && migrationPolicy == MigrationPolicy.BALANCE) {
            s_logger.debug("mean std deviation of the image stores is below threshold, no migration required");
            response = new MigrationResponse("Migration not required as system seems balanced", migrationPolicy.toString(), true);
            return response;
        }

        List<Future<AsyncCallFuture<DataObjectResult>>> futures = new ArrayList<>();
        while (true) {
            DataObject chosenFileForMigration = null;
            if (files.size() > 0) {
                chosenFileForMigration = files.remove(0);
            }

            storageCapacities = getStorageCapacities(storageCapacities, srcDataStoreId);
            List<Long> orderedDS = migrationHelper.sortDataStores(storageCapacities);
            Long destDatastoreId = orderedDS.get(0);

            if (chosenFileForMigration == null || destDatastoreId == null || (destDatastoreId == srcDatastore.getId() && migrationPolicy == MigrationPolicy.BALANCE) ) {
                Pair<String, Boolean> result = migrateCompleted(destDatastoreId, srcDatastore, files, migrationPolicy);
                message = result.first();
                success = result.second();
                break;
            }

            if (migrationPolicy == MigrationPolicy.COMPLETE && destDatastoreId == srcDatastore.getId()) {
                destDatastoreId = orderedDS.get(1);
            }

            if (chosenFileForMigration.getSize() > storageCapacities.get(destDatastoreId).first()) {
                s_logger.debug("file: " + chosenFileForMigration.getId() + " too large to be migrated to " + destDatastoreId);
                continue;
            }

            if (shouldMigrate(chosenFileForMigration, srcDatastore.getId(), destDatastoreId, storageCapacities, snapshotChains, migrationPolicy)) {
                storageCapacities = migrateAway(chosenFileForMigration, storageCapacities, snapshotChains, srcDatastore, destDatastoreId, executor, futures);
            } else {
                if (migrationPolicy == MigrationPolicy.BALANCE) {
                    continue;
                }
                message = "Complete migration failed. Please set the source Image store to read-write mode if you want to continue using it";
                success = false;
                break;
            }
        }
        Date end = new Date();
        handleSnapshotMigration(srcDataStoreId, start, end, migrationPolicy, futures, storageCapacities, executor);
        return handleResponse(futures, migrationPolicy, message, success);
    }

    protected Pair<String, Boolean> migrateCompleted(Long destDatastoreId, DataStore srcDatastore, List<DataObject> files, MigrationPolicy migrationPolicy) {
        String message = "";
        boolean success = true;
        if (destDatastoreId == srcDatastore.getId() && !files.isEmpty()) {
            if (migrationPolicy == MigrationPolicy.BALANCE) {
                s_logger.debug("Migration completed : data stores have been balanced ");
                if (destDatastoreId == srcDatastore.getId()) {
                    message = "Seems like source datastore has more free capacity than the destination(s)";
                }
                message += "Image stores have been attempted to be balanced";
                success = true;
            } else {
                message = "Files not completely migrated from "+ srcDatastore.getId() + ". Datastore (source): " + srcDatastore.getId() + "has equal or more free space than destination."+
                        " If you want to continue using the Image Store, please change the read-only status using 'update imagestore' command";
                success = false;
            }
        } else {
            message = "Migration completed";
        }
        return new Pair<String, Boolean>(message, success);
    }

    protected Map<Long, Pair<Long, Long>> migrateAway(DataObject chosenFileForMigration, Map<Long, Pair<Long, Long>> storageCapacities,
                               Map<DataObject, Pair<List<SnapshotInfo>, Long>> snapshotChains, DataStore srcDatastore, Long destDatastoreId, ThreadPoolExecutor executor,
    List<Future<AsyncCallFuture<DataObjectResult>>> futures) {
        Long fileSize = migrationHelper.getFileSize(chosenFileForMigration, snapshotChains);
        storageCapacities = assumeMigrate(storageCapacities, srcDatastore.getId(), destDatastoreId, fileSize);
        long activeSsvms = migrationHelper.activeSSVMCount(srcDatastore);
        long totalJobs = activeSsvms * numConcurrentCopyTasksPerSSVM;
        // Increase thread pool size with increase in number of SSVMs
        if ( totalJobs > executor.getCorePoolSize()) {
            executor.setMaximumPoolSize((int) (totalJobs));
            executor.setCorePoolSize((int) (totalJobs));
        }

        MigrateDataTask task = new MigrateDataTask(chosenFileForMigration, srcDatastore, dataStoreManager.getDataStore(destDatastoreId, DataStoreRole.Image));
        if (chosenFileForMigration instanceof SnapshotInfo ) {
            task.setSnapshotChains(snapshotChains);
        }
        futures.add((executor.submit(task)));
        s_logger.debug("Migration of file  " + chosenFileForMigration.getId() + " is initiated");
        return storageCapacities;
    }



    private MigrationResponse handleResponse(List<Future<AsyncCallFuture<DataObjectResult>>> futures, MigrationPolicy migrationPolicy, String message, boolean success) {
        int successCount = 0;
        for (Future<AsyncCallFuture<DataObjectResult>> future : futures) {
            try {
                AsyncCallFuture<DataObjectResult> res = future.get();
                if (res.get().isSuccess()) {
                    successCount++;
                }
            } catch ( InterruptedException | ExecutionException e) {
                s_logger.warn("Failed to get result");
                continue;
            }
        }
        message += ". successful migrations: "+successCount;
        return new MigrationResponse(message, migrationPolicy.toString(), success);
    }

    private void handleSnapshotMigration(Long srcDataStoreId, Date start, Date end, MigrationPolicy policy,
                                          List<Future<AsyncCallFuture<DataObjectResult>>> futures, Map<Long, Pair<Long, Long>> storageCapacities, ThreadPoolExecutor executor) {
        DataStore srcDatastore = dataStoreManager.getDataStore(srcDataStoreId, DataStoreRole.Image);
        List<SnapshotDataStoreVO> snaps = snapshotDataStoreDao.findSnapshots(srcDataStoreId, start, end);
        if (!snaps.isEmpty()) {
            for (SnapshotDataStoreVO snap : snaps) {
                SnapshotVO snapshotVO = snapshotDao.findById(snap.getSnapshotId());
                SnapshotInfo snapshotInfo = snapshotFactory.getSnapshot(snapshotVO.getSnapshotId(), DataStoreRole.Image);
                SnapshotInfo parentSnapshot = snapshotInfo.getParent();

                if (parentSnapshot == null && policy == MigrationPolicy.COMPLETE) {
                    List<Long> dstores = migrationHelper.sortDataStores(storageCapacities);
                    Long storeId = dstores.get(0);
                    if (storeId.equals(srcDataStoreId)) {
                        storeId = dstores.get(1);
                    }
                    DataStore datastore =  dataStoreManager.getDataStore(storeId, DataStoreRole.Image);
                    futures.add(executor.submit(new MigrateDataTask(snapshotInfo, srcDatastore, datastore)));
                }
                if (parentSnapshot != null) {
                    DataStore parentDS = dataStoreManager.getDataStore(parentSnapshot.getDataStore().getId(), DataStoreRole.Image);
                    if (parentDS.getId() != snapshotInfo.getDataStore().getId()) {
                        futures.add(executor.submit(new MigrateDataTask(snapshotInfo, srcDatastore, parentDS)));
                    }
                }
            }
        }
    }

    private Map<Long, Pair<Long, Long>> getStorageCapacities(Map<Long, Pair<Long, Long>> storageCapacities, Long srcDataStoreId) {
        Map<Long, Pair<Long, Long>> capacities = new Hashtable<>();
        for (Long storeId : storageCapacities.keySet()) {
            StorageStats stats = statsCollector.getStorageStats(storeId);
            if (stats != null) {
                if (storageCapacities.get(storeId) == null || storageCapacities.get(storeId).first() == null || storageCapacities.get(storeId).second() == null) {
                    capacities.put(storeId, new Pair<>(stats.getCapacityBytes() - stats.getByteUsed(), stats.getCapacityBytes()));
                } else {
                    long totalCapacity = stats.getCapacityBytes();
                    Long freeCapacity = totalCapacity - stats.getByteUsed();
                    if (storeId.equals(srcDataStoreId) || freeCapacity < storageCapacities.get(storeId).first()) {
                        capacities.put(storeId, new Pair<>(freeCapacity, totalCapacity));
                    } else {
                        capacities.put(storeId, storageCapacities.get(storeId));
                    }
                }
            } else {
                throw new CloudRuntimeException("Stats Collector hasn't yet collected metrics from the Image store, kindly try again later");
            }
        }
        return capacities;
    }


    /**
     *
     * @param storageCapacities Map comprising the metrics(free and total capacities) of the images stores considered
     * @return mean standard deviation
     */
    private double getStandardDeviation(Map<Long, Pair<Long, Long>> storageCapacities) {
        double[] freeCapacities = storageCapacities.values().stream().mapToDouble(x -> ((double) x.first() / x.second())).toArray();
        double mean = calculateStorageMean(freeCapacities);
        return (calculateStorageStandardDeviation(freeCapacities, mean) / mean);
    }

    /**
     *
     * @param storageCapacities Map comprising the metrics(free and total capacities) of the images stores considered
     * @param srcDsId source image store ID from where data is to be migrated
     * @param destDsId destination image store ID to where data is to be migrated
     * @param fileSize size of the data object to be migrated so as to recompute the storage metrics
     * @return a map - Key: Datastore ID ;  Value: Pair<Free Capacity , Total Capacity>
     */
    private Map<Long, Pair<Long, Long>> assumeMigrate(Map<Long, Pair<Long, Long>> storageCapacities, Long srcDsId, Long destDsId, Long fileSize) {
        Map<Long, Pair<Long, Long>> modifiedCapacities = new Hashtable<>();
        modifiedCapacities.putAll(storageCapacities);
        Pair<Long, Long> srcDSMetrics = storageCapacities.get(srcDsId);
        Pair<Long, Long> destDSMetrics = storageCapacities.get(destDsId);
        modifiedCapacities.put(srcDsId, new Pair<>(srcDSMetrics.first() + fileSize, srcDSMetrics.second()));
        modifiedCapacities.put(destDsId, new Pair<>(destDSMetrics.first() - fileSize, destDSMetrics.second()));
        return modifiedCapacities;
    }

    /**
     * This function determines if migration should in fact take place or not :
     *  - For Balanced migration - the mean standard deviation is calculated before and after (supposed) migration
     *      and a decision is made if migration is afterall beneficial
     *  - For Complete migration - We check if the destination image store has sufficient capacity i.e., below the threshold of (90%)
     *      and then proceed with the migration
     * @param chosenFile file for migration
     * @param srcDatastoreId source image store ID from where data is to be migrated
     * @param destDatastoreId destination image store ID to where data is to be migrated
     * @param storageCapacities Map comprising the metrics(free and total capacities) of the images stores considered
     * @param snapshotChains Map containing details of chain of snapshots and their cumulative size
     * @param migrationPolicy determines whether a "Balance" or "Complete" migration operation is to be performed
     * @return
     */
    private boolean shouldMigrate(DataObject chosenFile, Long srcDatastoreId, Long destDatastoreId, Map<Long, Pair<Long, Long>> storageCapacities,
                                  Map<DataObject, Pair<List<SnapshotInfo>, Long>> snapshotChains, MigrationPolicy migrationPolicy) {

        if (migrationPolicy == MigrationPolicy.BALANCE) {
            double meanStdDevCurrent = getStandardDeviation(storageCapacities);

            Long fileSize = migrationHelper.getFileSize(chosenFile, snapshotChains);
            Map<Long, Pair<Long, Long>> proposedCapacities = assumeMigrate(storageCapacities, srcDatastoreId, destDatastoreId, fileSize);
            double meanStdDevAfter = getStandardDeviation(proposedCapacities);

            if (meanStdDevAfter > meanStdDevCurrent) {
                s_logger.debug("migrating the file doesn't prove to be beneficial, skipping migration");
                return false;
            }

            Double threshold = ImageStoreImbalanceThreshold.value();
            if (meanStdDevCurrent > threshold && storageCapacityBelowThreshold(storageCapacities, destDatastoreId)) {
                return true;
            }
        } else {
            if (storageCapacityBelowThreshold(storageCapacities, destDatastoreId)) {
                return true;
            }
        }
        return false;
    }

    private boolean storageCapacityBelowThreshold(Map<Long, Pair<Long, Long>> storageCapacities, Long destStoreId) {
        Pair<Long, Long> imageStoreCapacity = storageCapacities.get(destStoreId);
        long usedCapacity = imageStoreCapacity.second() - imageStoreCapacity.first();
        if (imageStoreCapacity != null && (usedCapacity / (imageStoreCapacity.second() * 1.0)) <= imageStoreCapacityThreshold) {
            s_logger.debug("image store: " + destStoreId + " has sufficient capacity to proceed with migration of file");
            return true;
        }
        s_logger.debug("Image store capacity threshold exceeded, migration not possible");
        return false;
    }

    private double calculateStorageMean(double[] storageMetrics) {
        return new Mean().evaluate(storageMetrics);
    }

    private double calculateStorageStandardDeviation(double[] metricValues, double mean) {
        StandardDeviation standardDeviation = new StandardDeviation(false);
        return standardDeviation.evaluate(metricValues, mean);
    }

    private class MigrateDataTask implements Callable<AsyncCallFuture<DataObjectResult>> {
        private DataObject file;
        private DataStore srcDataStore;
        private DataStore destDataStore;
        private Map<DataObject, Pair<List<SnapshotInfo>, Long>> snapshotChain;
        public MigrateDataTask(DataObject file, DataStore srcDataStore, DataStore destDataStore) {
            this.file = file;
            this.srcDataStore = srcDataStore;
            this.destDataStore = destDataStore;
        }

        public void setSnapshotChains(Map<DataObject, Pair<List<SnapshotInfo>, Long>> snapshotChain) {
            this.snapshotChain = snapshotChain;
        }

        public Map<DataObject, Pair<List<SnapshotInfo>, Long>> getSnapshotChain() {
            return snapshotChain;
        }
        public DataObject getFile() {
            return file;
        }

        @Override
        public AsyncCallFuture<DataObjectResult> call() throws Exception {
            return secStgSrv.migrateData(file, srcDataStore, destDataStore, snapshotChain);
        }
    }
}
