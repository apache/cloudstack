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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
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
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.SecondaryStorageService;
import org.apache.cloudstack.engine.subsystem.api.storage.SecondaryStorageService.DataObjectResult;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreVO;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.log4j.Logger;

import com.cloud.configuration.Config;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.server.StatsCollector;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ImageStoreService.MigrationPolicy;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.StorageService;
import com.cloud.storage.StorageStats;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.StringUtils;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.SecondaryStorageVm;
import com.cloud.vm.SecondaryStorageVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.SecondaryStorageVmDao;

public class StorageOrchestrator extends ManagerBase implements StorageOrchestrationService, Configurable {

    private static final Logger s_logger = Logger.getLogger(StorageOrchestrator.class);
    @Inject
    TemplateDataStoreDao templateDataStoreDao;
    @Inject
    SnapshotDataStoreDao snapshotDataStoreDao;
    @Inject
    VolumeDataStoreDao volumeDataStoreDao;
    @Inject
    VolumeDataFactory volumeFactory;
    @Inject
    VMTemplateDao templateDao;
    @Inject
    TemplateDataFactory templateFactory;
    @Inject
    SnapshotDao snapshotDao;
    @Inject
    SnapshotDataFactory snapshotFactory;
    @Inject
    DataStoreManager dataStoreManager;
    @Inject
    ImageStoreDao imageStoreDao;
    @Inject
    StatsCollector statsCollector;
    @Inject
    public StorageService storageService;
    @Inject
    SecondaryStorageVmDao secStorageVmDao;
    @Inject
    ConfigurationDao configDao;
    @Inject
    HostDao hostDao;
    @Inject
    private AsyncJobManager jobMgr;
    @Inject
    private SecondaryStorageService secStgSrv;

    ConfigKey<Double> ImageStoreImbalanceThreshold = new ConfigKey<>("Advanced", Double.class,
            "image.store.imbalance.threshold",
            "0.1",
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
        numConcurrentCopyTasksPerSSVM = NumbersUtil.parseInt(configDao.getValue(Config.SecStorageMaxMigrateSessions.key()), 2);
        return true;
    }

    @Override
    public MigrationResponse migrateData(Long srcDataStoreId, List<Long> destDatastores, MigrationPolicy migrationPolicy) {
        List<DataObject> files = new LinkedList<>();
        int successCount = 0;
        boolean success = true;
        String message = null;

        if (migrationPolicy == MigrationPolicy.COMPLETE) {
            if (!filesReady(srcDataStoreId)) {
                throw new CloudRuntimeException("Complete migration failed as there are data objects which are not Ready");
            }
        }

        DataStore srcDatastore = dataStoreManager.getDataStore(srcDataStoreId, DataStoreRole.Image);
        Map<DataObject, Pair<List<SnapshotInfo>, Long>> snapshotChains = new HashMap<>();
        files.addAll(getAllValidTemplates(srcDatastore));
        files.addAll(getAllValidSnapshotChains(srcDatastore, snapshotChains));
        files.addAll(getAllValidVolumes(srcDatastore));

        Collections.sort(files, new Comparator<DataObject>() {
            @Override
            public int compare(DataObject o1, DataObject o2) {
                Long size1 = o1.getSize();
                Long size2 = o2.getSize();
                if (o1 instanceof SnapshotInfo) {
                    size1 = snapshotChains.get(o1).second();
                }
                if (o2 instanceof  SnapshotInfo) {
                    size2 = snapshotChains.get(o2).second();
                }
                //return o2.getSize() > o1.getSize() ? 1 : -1;
                return size2 > size1 ? 1 : -1;
            }
        });

        if (files.isEmpty()) {
            return new MigrationResponse("No files in Image store "+srcDatastore.getId()+ " to migrate", migrationPolicy.toString(), true);
        }

        // Create capacity class with free and total space, maybe id of ds too and use that as the value
        Map<Long, Pair<Long, Long>> storageCapacities = new Hashtable<>();

        for (Long storeId : destDatastores) {
            storageCapacities.put(storeId, new Pair<>(null, null));
        }

        storageCapacities.put(srcDataStoreId, new Pair<>(null, null));

        // If the migration policy is to completely migrate data from the given source Image Store, then set it's state
        // to readonly
        if (migrationPolicy == MigrationPolicy.COMPLETE) {
            s_logger.debug("Setting source image store "+srcDatastore.getId()+ " to read-only");
            storageService.updateImageStoreStatus(srcDataStoreId, true);
        }

        storageCapacities = getStorageCapacities(storageCapacities);
        double meanstddev = getStandardDeviation(storageCapacities);
        double threshold = ImageStoreImbalanceThreshold.value();
        MigrationResponse response = null;

        ThreadPoolExecutor executor = new ThreadPoolExecutor(numConcurrentCopyTasksPerSSVM , numConcurrentCopyTasksPerSSVM, 30, TimeUnit.MINUTES, new MigrateBlockingQueue<>(numConcurrentCopyTasksPerSSVM));
        Date start = new Date();
        if (meanstddev < threshold) {
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

            // Choose datastore with maximum free capacity as the destination datastore for migration
            storageCapacities = getStorageCapacities(storageCapacities);
            List<Long> orderedDS = sortDataStores(storageCapacities);
            Long destDatastoreId = orderedDS.get(0);

            // If there aren't anymore files available for migration or no valid Image stores available for migration
            // end the migration process
            if (chosenFileForMigration == null || destDatastoreId == null || destDatastoreId == srcDatastore.getId()) {
                if (destDatastoreId == srcDatastore.getId() && !files.isEmpty() ) {
                    if (migrationPolicy == MigrationPolicy.BALANCE) {
                        s_logger.debug("Migration completed : data stores have been balanced ");
                        message = "Image stores have been balanced";
                        success = true;
                    } else {
                        message = "Files not completely migrated from "+ srcDatastore.getId() +
                                " If you want to continue using the Image Store, please change the read-only status using 'update imagestore' command";
                        success = false;
                    }
                } else {
                    message = "Migration completed";
                    success = true;
                }
                break;
            }

            if (chosenFileForMigration.getSize() > storageCapacities.get(destDatastoreId).first()) {
                s_logger.debug("file: " + chosenFileForMigration.getId() + " too large to be migrated to " + destDatastoreId);
                continue;
            }

            // If there is a benefit in migration of the chosen file to the destination store, then proceed with migration
            if (shouldMigrate(chosenFileForMigration, srcDatastore.getId(), destDatastoreId, storageCapacities, snapshotChains, migrationPolicy)) {
                Long fileSize = getFileSize(chosenFileForMigration, snapshotChains);
                storageCapacities = assumeMigrate(storageCapacities, srcDatastore.getId(), destDatastoreId, fileSize);

                long activeSsvms = activeSSVMCount(srcDatastore);
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
            } else {
                if (migrationPolicy == MigrationPolicy.BALANCE) {
                    message = "Migration completed and has successfully balanced the data objects among stores:  " + StringUtils.join(storageCapacities.keySet(), ",");
                } else {
                    message = "Complete migration failed. Please set the source Image store to read-write mode if you want to continue using it";
                    success = false;
                }
                break;
            }
        }
        Date end = new Date();
        // Migrate snapshots created during the migration process
        List<SnapshotDataStoreVO> snaps = snapshotDataStoreDao.findSnapshots(srcDataStoreId, start, end);
        if (!snaps.isEmpty()) {
            for (SnapshotDataStoreVO snap : snaps) {
                SnapshotVO snapshotVO = snapshotDao.findById(snap.getSnapshotId());
                SnapshotInfo snapshotInfo = snapshotFactory.getSnapshot(snapshotVO.getSnapshotId(), DataStoreRole.Image);
                SnapshotInfo parentSnapshot = snapshotInfo.getParent();

                if (parentSnapshot == null && migrationPolicy == MigrationPolicy.COMPLETE) {
                    List<Long> dstores = sortDataStores(storageCapacities);
                    Long storeId = dstores.get(0);
                    if (storeId == srcDataStoreId) {
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

    private Map<Long, Pair<Long, Long>> getStorageCapacities(Map<Long, Pair<Long, Long>> storageCapacities) {
        Map<Long, Pair<Long, Long>> capacities = new Hashtable<>();
        for (Long storeId : storageCapacities.keySet()) {
            StorageStats stats = statsCollector.getStorageStats(storeId);
            if (stats != null) {
                if (storageCapacities.get(storeId) == null || storageCapacities.get(storeId).first() == null || storageCapacities.get(storeId).second() == null) {
                    capacities.put(storeId, new Pair<>(stats.getCapacityBytes() - stats.getByteUsed(), stats.getCapacityBytes()));
                } else {
                    long totalCapacity = stats.getCapacityBytes();
                    Long freeCapacity = totalCapacity - stats.getByteUsed();
                    if (freeCapacity >= storageCapacities.get(storeId).first()) {
                        capacities.put(storeId, storageCapacities.get(storeId));
                    } else {
                        capacities.put(storeId, new Pair<>(freeCapacity, totalCapacity));
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
     * Sorts the datastores in decreasing order of their free capacities, so as to make
     * an informed decision of picking the datastore with maximum free capactiy for migration
     */
    private List<Long> sortDataStores(Map<Long, Pair<Long, Long>> storageCapacities) {
        List<Map.Entry<Long, Pair<Long, Long>>> list =
                new LinkedList<Map.Entry<Long, Pair<Long, Long>>>((storageCapacities.entrySet()));

        Collections.sort(list, new Comparator<Map.Entry<Long, Pair<Long, Long>>>() {
            @Override
            public int compare(Map.Entry<Long, Pair<Long, Long>> e1, Map.Entry<Long, Pair<Long, Long>> e2) {
                return e2.getValue().first() > e1.getValue().first() ? 1 : -1;
            }
        });
        HashMap<Long, Pair<Long, Long>> temp = new LinkedHashMap<>();
        for (Map.Entry<Long, Pair<Long, Long>> value : list) {
            temp.put(value.getKey(), value.getValue());
        }

        return new ArrayList<>(temp.keySet());
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

    private Long getFileSize(DataObject file, Map<DataObject, Pair<List<SnapshotInfo>, Long>> snapshotChain) {
        Long size = file.getSize();
        Pair<List<SnapshotInfo>, Long> chain = snapshotChain.get(file);
        if (file instanceof SnapshotInfo && chain.first() != null) {
            size = chain.second();
        }
        return size;
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

            Long fileSize = getFileSize(chosenFile, snapshotChains);
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
        if (imageStoreCapacity != null && (imageStoreCapacity.first() / (imageStoreCapacity.second() * 1.0)) <= imageStoreCapacityThreshold) {
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

    /** This function verifies if the given image store comprises of data objects that are not in either the "Ready" or
     * "Allocated" state - in such a case, if the migration policy is complete, the migration is terminated
     */
    private boolean filesReady(Long srcDataStoreId) {
        String[] validStates = new String[]{"Ready", "Allocated"};
        boolean isReady = true;
        List<TemplateDataStoreVO> templates = templateDataStoreDao.listByStoreId(srcDataStoreId);
        for (TemplateDataStoreVO template : templates) {
            isReady &= (Arrays.asList(validStates).contains(template.getState().toString()));
        }
        List<SnapshotDataStoreVO> snapshots = snapshotDataStoreDao.listByStoreId(srcDataStoreId, DataStoreRole.Image);
        for (SnapshotDataStoreVO snapshot : snapshots) {
            isReady &= (Arrays.asList(validStates).contains(snapshot.getState().toString()));
        }
        List<VolumeDataStoreVO> volumes = volumeDataStoreDao.listByStoreId(srcDataStoreId);
        for (VolumeDataStoreVO volume : volumes) {
            isReady &= (Arrays.asList(validStates).contains(volume.getState().toString()));
        }
        return isReady;
    }

    // Gets list of all valid templates, i.e, templates in "Ready" state for migration
    private List<DataObject> getAllValidTemplates(DataStore srcDataStore) {

        List<DataObject> files = new LinkedList<>();
        List<TemplateDataStoreVO> templates = templateDataStoreDao.listByStoreId(srcDataStore.getId());
        for (TemplateDataStoreVO template : templates) {
            VMTemplateVO templateVO = templateDao.findById(template.getTemplateId());
            if (template.getState() == ObjectInDataStoreStateMachine.State.Ready && !templateVO.isPublicTemplate()) {
                files.add(templateFactory.getTemplate(template.getTemplateId(), srcDataStore));
            }
        }
        return files;
    }

    /** Returns parent snapshots and snapshots that do not have any children; snapshotChains comprises of the snapshot chain info
     * for each parent snapshot and the cumulative size of the chain - this is done to ensure that all the snapshots in a chain
     * are migrated to the same datastore
     */
    private List<DataObject> getAllValidSnapshotChains(DataStore srcDataStore, Map<DataObject, Pair<List<SnapshotInfo>, Long>> snapshotChains) {
        List<SnapshotInfo> files = new LinkedList<>();
        List<SnapshotDataStoreVO> snapshots = snapshotDataStoreDao.listByStoreId(srcDataStore.getId(), DataStoreRole.Image);
        for (SnapshotDataStoreVO snapshot : snapshots) {
            SnapshotVO snapshotVO = snapshotDao.findById(snapshot.getSnapshotId());
            if (snapshot.getState() == ObjectInDataStoreStateMachine.State.Ready && snapshot.getParentSnapshotId() == 0 ) {
                SnapshotInfo snap = snapshotFactory.getSnapshot(snapshotVO.getSnapshotId(), DataStoreRole.Image);
                files.add(snap);
            }
        }

        for (SnapshotInfo parent : files) {
            List<SnapshotInfo> chain = new ArrayList<>();
            chain.add(parent);
            for (int i =0; i< chain.size(); i++) {
                SnapshotInfo child = chain.get(i);
                List<SnapshotInfo> children = child.getChildren();
                if (children != null) {
                    chain.addAll(children);
                }
            }
            snapshotChains.put(parent, new Pair<List<SnapshotInfo>, Long>(chain, getSizeForChain(chain)));
        }

        return (List<DataObject>) (List<?>) files;
    }

    // Finds the cumulative file size for all data objects in the chain
    private Long getSizeForChain(List<SnapshotInfo> chain) {
        Long size = 0L;
        for (SnapshotInfo snapshot : chain) {
            size += snapshot.getSize();
        }
        return size;
    }

    // Returns a list of volumes that are in "Ready" state
    private List<DataObject> getAllValidVolumes(DataStore srcDataStore) {
        List<DataObject> files = new LinkedList<>();
        List<VolumeDataStoreVO> volumes = volumeDataStoreDao.listByStoreId(srcDataStore.getId());
        for (VolumeDataStoreVO volume : volumes) {
            if (volume.getState() == ObjectInDataStoreStateMachine.State.Ready) {
                files.add(volumeFactory.getVolume(volume.getVolumeId(), srcDataStore));
            }
        }
        return files;
    }

    /** Returns the count of active SSVMs - SSVM with agents in connected state, so as to dynamically increase the thread pool
     * size when SSVMs scale
     */
    private int activeSSVMCount(DataStore dataStore) {
        long datacenterId = dataStore.getScope().getScopeId();
        List<SecondaryStorageVmVO> ssvms =
                secStorageVmDao.getSecStorageVmListInStates(SecondaryStorageVm.Role.templateProcessor, datacenterId, VirtualMachine.State.Running, VirtualMachine.State.Migrating);
        int activeSSVMs = 0;
        for (SecondaryStorageVmVO vm : ssvms) {
            String name = "s-"+vm.getId()+"-VM";
            HostVO ssHost = hostDao.findByName(name);
            if (ssHost != null) {
                if (ssHost.getState() == Status.Up) {
                    activeSSVMs++;
                }
            }
        }
        return activeSSVMs;
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
