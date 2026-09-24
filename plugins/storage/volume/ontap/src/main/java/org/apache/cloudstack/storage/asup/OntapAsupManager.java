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

package org.apache.cloudstack.storage.asup;

import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.event.EventTypes;
import com.cloud.server.ManagementService;
import com.cloud.storage.Volume;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.vm.snapshot.VMSnapshot;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.net.NetUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.feign.model.Cluster;
import org.apache.cloudstack.storage.feign.model.EmsApplicationLog;
import org.apache.cloudstack.storage.service.StorageStrategy;
import org.apache.cloudstack.storage.utils.OntapConfigurationManager;
import org.apache.cloudstack.storage.utils.OntapStorageConstants;
import org.apache.cloudstack.storage.utils.OntapStorageUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Periodic ASUP (AutoSupport) telemetry pusher for the NetApp ONTAP plugin.
 *
 * <p>Each run pushes, for every ONTAP-backed primary storage pool, two minimal EMS
 * application-log messages to the backing ONTAP cluster:</p>
 *
 * <ul>
 *     <li><b>event-id 0 (heartbeat):</b> identifies the CloudStack deployment (CloudStack
 *         version, management host) connected to the ONTAP cluster (ONTAP cluster version).</li>
 *     <li><b>event-id 1 (pool):</b> maps the CloudStack storage pool to its backing ONTAP
 *         volume - protocol (NFS/iSCSI), ONTAP FlexVolume UUID, SVM, disk usage, and
 *         snapshot telemetry (counts by state, total provisioned size).</li>
 * </ul>
 *
 * <p>Runs are booked one at a time: after each cycle starts, the next run is scheduled for
 * {@link OntapConfigurationManager#AsupIntervalHours} after that start, so production
 * intervals (hours) are start-to-start. REST work sits inside the interval; it is not added
 * after it. Editing the ONTAP ASUP interval re-books the pending run immediately, with no
 * management-server restart.</p>
 */
public class OntapAsupManager extends ManagerBase {
    private static final int ASUP_LOCK_TIMEOUT_SECONDS = 5;

    /**
     * Volume states that guarantee a physical object exists on the ONTAP FlexVolume.
     * States like {@link Volume.State#Allocated} have a CloudStack DB row pointing to this
     * pool but ONTAP provisioning has not been called yet — they must be excluded to avoid
     * inflating disk counts and provisioned-size totals. Upload-family states live on
     * secondary storage, not on the primary ONTAP volume, so they are also excluded.
     */
    private static final Set<Volume.State> CS_VOLUME_STATES = EnumSet.of(
            Volume.State.Ready,
            Volume.State.Snapshotting,
            Volume.State.RevertSnapshotting,
            Volume.State.Attaching,
            Volume.State.Restoring,
            Volume.State.Expunging,
            Volume.State.Destroying
    );

    /** Serializes the structured event-description payloads to JSON. */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Timestamp of the last ASUP cycle start (or {@link Instant#EPOCH} if none has run).
     * The next run is due {@link OntapConfigurationManager#AsupIntervalHours} after this
     * instant. {@code volatile} so the scheduler thread's write is visible without extra locking.
     */
    volatile Instant lastPushTime = Instant.EPOCH;

    /** Single daemon thread that runs the ASUP pushes; owned by this manager. */
    private ScheduledExecutorService asupScheduler;

    /** The one pending run. Cancelled and re-booked whenever the schedule changes. */
    private ScheduledFuture<?> pendingRun;

    @Inject
    private PrimaryDataStoreDao storagePoolDao;
    @Inject
    private StoragePoolDetailsDao storagePoolDetailsDao;
    @Inject
    private VolumeDao volumeDao;
    @Inject
    private SnapshotDao snapshotDao;
    @Inject
    private VMSnapshotDao vmSnapshotDao;
    @Inject
    private ManagementService managementService;
    @Inject
    private ManagementServerHostDao managementServerHostDao;
    @Inject
    private MessageBus messageBus;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        // React to edits of ontap.autosupport.interval so a new value re-books the pending run
        // instead of waiting for it to fire on the old schedule.
        messageBus.subscribe(EventTypes.EVENT_CONFIGURATION_VALUE_EDIT, this::onAsupConfigEdited);
        return true;
    }

    @Override
    public boolean start() {
        asupScheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("OntapAsup"));
        logger.info("OntapAsupManager started; ASUP telemetry interval={}h",
                getAsupIntervalHours(OntapConfigurationManager.AsupIntervalHours.value()));
        scheduleNextRun();
        return super.start();
    }

    @Override
    public boolean stop() {
        if (asupScheduler != null) {
            asupScheduler.shutdownNow();
        }
        return super.stop();
    }

    /**
     * Books the single pending run for the moment the configured interval elapses, cancelling
     * whatever was booked before. Called at start-up, after every run, and whenever
     * {@code ontap.autosupport.interval} is edited, so the live config always decides the next run.
     *
     * <p>When the interval is {@link OntapStorageConstants#ASUP_DISABLED_INTERVAL_HOURS}
     * nothing is booked; setting a non-zero interval publishes a configuration-edit event,
     * which books a run again.</p>
     */
    private synchronized void scheduleNextRun() {
        if (asupScheduler == null || asupScheduler.isShutdown()) {
            return;
        }
        if (pendingRun != null) {
            pendingRun.cancel(false);
            pendingRun = null;
        }
        int intervalHours = getAsupIntervalHours(OntapConfigurationManager.AsupIntervalHours.value());
        if (intervalHours == OntapStorageConstants.ASUP_DISABLED_INTERVAL_HOURS) {
            logger.debug("ONTAP ASUP: telemetry is disabled ({}={}); no run scheduled.",
                    OntapStorageConstants.ASUP_INTERVAL_CONFIG_KEY,
                    OntapStorageConstants.ASUP_DISABLED_INTERVAL_HOURS);
            return;
        }
        long delayMs = millisUntilNextPush();
        pendingRun = asupScheduler.schedule(new OntapAsupTask(), delayMs, TimeUnit.MILLISECONDS);
        logger.debug("ONTAP ASUP: next telemetry push scheduled in {} ms.", delayMs);
    }

    /**
     * Milliseconds remaining until {@link #lastPushTime} plus the live configured interval.
     * Zero when a push is already overdue — for example after the interval is shortened, the
     * previous cycle ran longer than the interval, or on the very first run when
     * {@link #lastPushTime} is still {@link Instant#EPOCH}.
     */
    long millisUntilNextPush() {
        Duration configuredInterval = Duration.ofHours(
                getAsupIntervalHours(OntapConfigurationManager.AsupIntervalHours.value()));
        Duration remaining = configuredInterval.minus(Duration.between(lastPushTime, Instant.now()));
        return remaining.isNegative() ? 0L : remaining.toMillis();
    }

    /**
     * CloudStack publishes this after invalidating the config cache, so the new value is already
     * readable. Re-books the pending run: shortening the interval past the due time schedules
     * with zero delay, which pushes right away on the scheduler thread rather than blocking the
     * API thread that served {@code updateConfiguration}.
     */
    @SuppressWarnings("unchecked")
    private void onAsupConfigEdited(String senderAddress, String subject, Object args) {
        if (!(args instanceof Ternary)) {
            return;
        }
        String updatedKey = ((Ternary<String, ConfigKey.Scope, Long>) args).first();
        if (!OntapConfigurationManager.AsupIntervalHours.key().equals(updatedKey)) {
            return;
        }
        logger.debug("ONTAP ASUP: [{}] was updated; re-booking the next push.", updatedKey);
        scheduleNextRun();
    }

    /**
     * One ASUP run inside a managed CloudStack context, which the DAO calls made during the push
     * require.
     *
     * <p>{@link #lastPushTime} is stamped at the start of the cycle so the next wait is the
     * remainder of the interval (work does not get added on top). The next run is booked again
     * in {@code finally} so the schedule never stalls.</p>
     */
    protected class OntapAsupTask extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            try {
                lastPushTime = Instant.now();
                pushAsupTelemetry();
            } catch (Exception e) {
                logger.warn("ONTAP ASUP: unexpected error during periodic push: {}", e.getMessage());
            } finally {
                scheduleNextRun();
            }
        }
    }

    /**
     * Iterates all ONTAP-backed primary storage pools and pushes ASUP telemetry for each.
     *
     * <p>Guarded by a {@link GlobalLock} so that, in a multi-management-server deployment,
     * only one node emits per cycle.</p>
     */
    protected void pushAsupTelemetry() {
        List<StoragePoolVO> pools = storagePoolDao.findPoolsByProvider(OntapStorageConstants.ONTAP_PLUGIN_NAME);
        if (CollectionUtils.isEmpty(pools)) {
            logger.debug("ONTAP ASUP: no ONTAP-backed storage pools found; nothing to push.");
            return;
        }

        GlobalLock lock = GlobalLock.getInternLock(OntapStorageConstants.ASUP_GLOBAL_LOCK_NAME);
        try {
            if (!lock.lock(ASUP_LOCK_TIMEOUT_SECONDS)) {
                logger.debug("ONTAP ASUP: another management server holds the ASUP lock; skipping this cycle.");
                return;
            }
            logger.debug("ONTAP ASUP: pushing telemetry for {} pool(s) [CloudStack version={}]",
                    pools.size(), getCloudStackVersion());
            Map<String, AsupClusterClient> clientsByStorageIp = new HashMap<>();
            for (StoragePoolVO pool : pools) {
                pushAsupForStoragePool(pool, clientsByStorageIp);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Pushes the heartbeat (event-id 0) and pool (event-id 1) ASUP messages for a single pool.
     *
     * <p>{@code clientsByStorageIp} is the per-cycle cache keyed by management {@code storageIP}.
     * A miss builds the strategy, GETs cluster info and node model, and sends event-0. A hit
     * reuses that client and skips those calls and event-0. Event-1 is always sent. SVM name
     * in the payload still comes from this pool's details.</p>
     *
     * <p>Best-effort: any failure is logged and swallowed.</p>
     */
    protected void pushAsupForStoragePool(StoragePoolVO pool, Map<String, AsupClusterClient> clientsByStorageIp) {
        try {
            Map<String, String> details = storagePoolDetailsDao.listDetailsKeyPairs(pool.getId());
            if (details == null || details.isEmpty()) {
                logger.warn("ONTAP ASUP: storage pool [{}] has no details; skipping.", pool.getId());
                return;
            }

            String storageIp = details.get(OntapStorageConstants.STORAGE_IP);
            AsupClusterClient asupClusterClient = StringUtils.isNotBlank(storageIp)
                    ? clientsByStorageIp.get(storageIp) : null;
            boolean sendHeartbeat = asupClusterClient == null;
            if (asupClusterClient == null) {
                StorageStrategy strategy = OntapStorageUtils.resolveStrategyFromPoolDetails(details);
                Cluster cluster = strategy.getClusterInfo();
                asupClusterClient = new AsupClusterClient(strategy, cluster);
                if (StringUtils.isNotBlank(storageIp)) {
                    clientsByStorageIp.put(storageIp, asupClusterClient);
                }
            }

            StorageStrategy strategy = asupClusterClient.strategy;
            Cluster cluster = asupClusterClient.cluster;
            String ontapVersion = strategy.getClusterVersion(cluster);
            String clusterUuid = cluster != null ? cluster.getUuid() : null;
            String cloudStackVersion = getCloudStackVersion();
            String computerName = getComputerName();

            if (sendHeartbeat) {
                EmsApplicationLog heartbeat = buildBaseMessage(computerName, cloudStackVersion);
                heartbeat.setEventId(OntapStorageConstants.ASUP_EVENT_ID_HEARTBEAT);
                heartbeat.setEventDescription(buildHeartbeatDescription(
                        cloudStackVersion, ontapVersion, cluster));
                strategy.sendAsupMessage(heartbeat);
            } else {
                logger.debug("ONTAP ASUP: heartbeat already sent this cycle for storage IP [{}]; skipping for pool [{}]",
                        storageIp, pool.getId());
            }

            // event-id 1: CloudStack storage pool -> backing ONTAP volume mapping, once per pool.
            // The description also includes disk usage and snapshot telemetry
            EmsApplicationLog poolMessage = buildBaseMessage(computerName, cloudStackVersion);
            poolMessage.setEventId(OntapStorageConstants.ASUP_EVENT_ID_STORAGE_POOL);
            poolMessage.setEventDescription(buildPoolDescription(pool, details, clusterUuid));
            strategy.sendAsupMessage(poolMessage);

            logger.debug("ONTAP ASUP: pushed telemetry for pool [{}] (ONTAP version={})",
                    pool.getId(), defaultUnknown(ontapVersion));
        } catch (Exception e) {
            // Best-effort telemetry; never propagate.
            logger.warn("ONTAP ASUP: failed to push telemetry for pool [{}]: {}", pool.getId(), e.getMessage());
        }
    }

    /**
     * Builds the heartbeat (event-id 0) description as a JSON object carrying the CloudStack and
     * ONTAP versions, cluster hardware model, the management-server operating system platform,
     * and the ONTAP cluster UUID.
     * Example: {@code {"message":"CloudStack connected to ONTAP cluster","cloudstackVersion":
     * "4.23.0.0","platform":"Linux 5.15.0-91-generic (amd64)","ontapVersion":"9.17.1",
     * "ontapClusterModel":"AFF-A400","ontapPlatformType":"performance","clusterUuid":"...",
     * "managementServerCount":2}}
     */
    private String buildHeartbeatDescription(String cloudStackVersion, String ontapVersion, Cluster cluster) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(OntapStorageConstants.ASUP_MESSAGE, OntapStorageConstants.ASUP_HEARTBEAT_MESSAGE);
        payload.put(OntapStorageConstants.ASUP_CLOUDSTACK_VERSION, defaultUnknown(cloudStackVersion));
        payload.put(OntapStorageConstants.ASUP_PLATFORM, getOperatingSystem());
        payload.put(OntapStorageConstants.ASUP_ONTAP_VERSION, defaultUnknown(ontapVersion));
        payload.put(OntapStorageConstants.ASUP_ONTAP_CLUSTER_MODEL,
                defaultUnknown(cluster != null ? cluster.getModel() : null));
        payload.put(OntapStorageConstants.ASUP_ONTAP_PLATFORM_TYPE,
                defaultUnknown(cluster != null ? cluster.getPlatformType() : null));
        payload.put(OntapStorageConstants.ASUP_CLUSTER_UUID,
                defaultUnknown(cluster != null ? cluster.getUuid() : null));
        payload.put(OntapStorageConstants.ASUP_MANAGEMENT_SERVER_COUNT, getManagementServerCount());
        return toJson(payload);
    }

    /**
     * Builds the pool description (event-id 1) as a JSON object combining the backing-volume
     * mapping, disk usage, and snapshot telemetry into a single EMS message.
     *
     * <p>Example: {@code {"message":"CloudStack storage pool backed by ONTAP volume",
     * "poolName":"...","poolStatus":"Up","protocol":"nfs","clusterUuid":"...","svm":"...",
     * "ontapVolumeUuid":"...","rootDiskCount":12,"dataDiskCount":18,
     * "totalLogicalSizeBytes":322122547200,"multiPrimaryStoragePoolVm":false,
     * "volumeSnapshotCount":5,"vmSnapshotCount":3}}</p>
     */
    private String buildPoolDescription(StoragePoolVO pool, Map<String, String> details,
            String clusterUuid) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(OntapStorageConstants.ASUP_MESSAGE, OntapStorageConstants.ASUP_POOL_MESSAGE);
        payload.put(OntapStorageConstants.ASUP_POOL_NAME, defaultUnknown(pool.getName()));
        payload.put(OntapStorageConstants.ASUP_POOL_STATUS,
                pool.getStatus() == null ? OntapStorageConstants.ASUP_UNKNOWN : pool.getStatus().toString());
        payload.put(OntapStorageConstants.ASUP_PROTOCOL, defaultUnknown(details.get(OntapStorageConstants.PROTOCOL)));
        payload.put(OntapStorageConstants.ASUP_CLUSTER_UUID, defaultUnknown(clusterUuid));
        payload.put(OntapStorageConstants.ASUP_SVM, defaultUnknown(details.get(OntapStorageConstants.SVM_NAME)));
        payload.put(OntapStorageConstants.ASUP_ONTAP_VOLUME_UUID, defaultUnknown(details.get(OntapStorageConstants.VOLUME_UUID)));
        List<VolumeVO> volumes = null;
        try {
            volumes = loadNonDestroyedVolumes(pool);
        } catch (Exception e) {
            logger.error("ONTAP ASUP: failed to load volumes for pool [{}]: {}", pool.getId(), e.getMessage());
        }
        if (volumes != null) {
            addStoragePoolUsage(pool, payload, volumes);
            addSnapshotMetrics(pool, payload, volumes);
        }
        hasMultiPrimaryStoragePoolVm(pool, payload);
        return toJson(payload);
    }

    /**
     * Single CloudStack DB read of non-destroyed volumes on this pool. Shared by usage and
     * snapshot counts so the list is not queried three times. Never returns null.
     */
    private List<VolumeVO> loadNonDestroyedVolumes(StoragePoolVO pool) {
        List<VolumeVO> volumes = volumeDao.findNonDestroyedVolumesByPoolId(pool.getId(), null);
        return volumes == null ? java.util.Collections.emptyList() : volumes;
    }

    /**
     * Computes pool usage from CloudStack's volume records and adds it to the payload:
     * <ul>
     *     <li>{@code rootDiskCount} - number of ROOT (boot) disks physically on this pool</li>
     *     <li>{@code dataDiskCount} - number of DATADISK disks physically on this pool</li>
     *     <li>{@code totalLogicalSizeBytes} - sum of those volumes' provisioned (logical) sizes
     *         in bytes; for thin-provisioned volumes this is the logical size requested at
     *         creation time, not the physical space consumed on ONTAP</li>
     * </ul>
     * {@code volumes} is the non-destroyed list for this pool (already loaded). Only
     * {@link #CS_VOLUME_STATES} are counted. Best-effort: any failure leaves the usage
     * fields out and never breaks telemetry.
     */
    private void addStoragePoolUsage(StoragePoolVO pool, Map<String, Object> payload, List<VolumeVO> volumes) {
        try {
            // Only count volumes that definitely have a physical object on the ONTAP FlexVolume.
            // "Allocated" volumes have a pool_id row in the CS DB but ONTAP provisioning has not
            // yet been called, so including them would inflate counts and provisioned size.
            List<VolumeVO> cstackVolumes = volumes.stream()
                    .filter(v -> CS_VOLUME_STATES.contains(v.getState()))
                    .collect(java.util.stream.Collectors.toList());

            long rootDiskCount = cstackVolumes.stream()
                    .filter(v -> Volume.Type.ROOT.equals(v.getVolumeType())).count();
            long dataDiskCount = cstackVolumes.stream()
                    .filter(v -> Volume.Type.DATADISK.equals(v.getVolumeType())).count();

            long totalLogicalSizeBytes = cstackVolumes.stream()
                    .mapToLong(v -> v.getSize() != null ? v.getSize() : 0L).sum();
            payload.put(OntapStorageConstants.ASUP_ROOT_DISK_COUNT, rootDiskCount);
            payload.put(OntapStorageConstants.ASUP_DATA_DISK_COUNT, dataDiskCount);
            payload.put(OntapStorageConstants.ASUP_TOTAL_LOGICAL_SIZE_BYTES, totalLogicalSizeBytes);
        } catch (Exception e) {
            logger.error("ONTAP ASUP: failed to compute usage for pool [{}]: {}", pool.getId(), e.getMessage());
        }
    }

    /**
     * Adds {@code hasMultiPrimaryStoragePoolVm}: true when at least one VM with ROOT on this
     * pool also has an attached DATADISK on a different primary storage pool. Uses a single
     * {@code LIMIT 1} existence query.
     */
    private void hasMultiPrimaryStoragePoolVm(StoragePoolVO pool, Map<String, Object> payload) {
        try {
            payload.put(OntapStorageConstants.ASUP_MULTI_PRIMARY_STORAGE_POOL_VM,
                    volumeDao.hasMultiPrimaryStoragePoolVm(pool.getId()));
        } catch (Exception e) {
            logger.warn("ONTAP ASUP: failed to compute multiPrimaryStoragePoolVm for pool [{}]: {}",
                    pool.getId(), e.getMessage());
        }
    }

    /**
     * Computes and adds two groups of snapshot telemetry to the pool description payload.
     *
     * <p><b>Volume-snapshot metrics</b> ({@code volumeSnapshotCount}): counts all
     * non-destroyed CloudStack volume-level snapshots for volumes on this pool.</p>
     *
     * <p><b>VM-snapshot metrics</b> ({@code vmSnapshotCount}): counts all active
     * (non-expunging, non-removed) VM snapshots for VMs whose ROOT disk is on this pool.
     * A data disk on another pool does not duplicate the count.</p>
     *
     * <p>Best-effort: any failure leaves the fields out without breaking telemetry.</p>
     */
    private void addSnapshotMetrics(StoragePoolVO pool, Map<String, Object> payload, List<VolumeVO> volumes) {
        addVmSnapshotMetrics(pool, payload, volumes);
        addVolumeSnapshotMetrics(pool, payload, volumes);
    }

    /**
     * Adds {@code volumeSnapshotCount} to the payload.
     * Counts all non-destroyed CloudStack volume-level snapshots for volumes on this pool.
     */
    private void addVolumeSnapshotMetrics(StoragePoolVO pool, Map<String, Object> payload, List<VolumeVO> volumes) {
        try {
            if (volumes.isEmpty()) {
                payload.put(OntapStorageConstants.ASUP_VOLUME_SNAPSHOT_COUNT, 0);
                return;
            }

            List<Long> volumeIds = volumes.stream()
                    .map(VolumeVO::getId)
                    .collect(java.util.stream.Collectors.toList());

            List<SnapshotVO> snapshots = snapshotDao.searchByVolumes(volumeIds);
            long snapCount = snapshots == null ? 0L : snapshots.stream()
                    .filter(snap -> !com.cloud.storage.Snapshot.State.Destroyed.equals(snap.getState()))
                    .count();

            payload.put(OntapStorageConstants.ASUP_VOLUME_SNAPSHOT_COUNT, snapCount);
        } catch (Exception e) {
            logger.warn("ONTAP ASUP: failed to compute volume-snapshot metrics for pool [{}]: {}",
                    pool.getId(), e.getMessage());
        }
    }

    /**
     * Adds {@code vmSnapshotCount} to the payload.
     * Counts all active (non-expunging, non-removed) VM snapshots for VMs whose ROOT
     * disk is on this pool, so a multi-pool VM is counted once (on the root pool).
     */
    private void addVmSnapshotMetrics(StoragePoolVO pool, Map<String, Object> payload, List<VolumeVO> volumes) {
        try {
            if (volumes.isEmpty()) {
                payload.put(OntapStorageConstants.ASUP_VM_SNAPSHOT_COUNT, 0);
                return;
            }

            java.util.Set<Long> vmIds = volumes.stream()
                    .filter(v -> Volume.Type.ROOT.equals(v.getVolumeType()))
                    .map(VolumeVO::getInstanceId)
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toSet());

            if (vmIds.isEmpty()) {
                payload.put(OntapStorageConstants.ASUP_VM_SNAPSHOT_COUNT, 0);
                return;
            }

            List<VMSnapshotVO> vmSnapshots = vmSnapshotDao.searchByVms(new java.util.ArrayList<>(vmIds));
            long vmSnapCount = vmSnapshots == null ? 0L : vmSnapshots.stream()
                    .filter(vmSnap -> !VMSnapshot.State.Expunging.equals(vmSnap.getState())
                            && vmSnap.getRemoved() == null)
                    .count();

            payload.put(OntapStorageConstants.ASUP_VM_SNAPSHOT_COUNT, vmSnapCount);
        } catch (Exception e) {
            logger.warn("ONTAP ASUP: failed to compute VM-snapshot metrics for pool [{}]: {}",
                    pool.getId(), e.getMessage());
        }
    }

    /**
     * Serializes a payload map to a JSON string. Falls back to the map's {@code toString()} if
     * serialization unexpectedly fails, so telemetry is still emitted (best-effort).
     */
    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            logger.warn("ONTAP ASUP: failed to serialize event description to JSON: {}", e.getMessage());
            return String.valueOf(payload);
        }
    }

    /** Builds the common EMS message envelope shared by all ASUP messages. */
    private EmsApplicationLog buildBaseMessage(String computerName, String appVersion) {
        EmsApplicationLog message = new EmsApplicationLog();
        message.setComputerName(computerName);
        message.setEventSource(OntapStorageConstants.ASUP_EVENT_SOURCE);
        message.setAppVersion(appVersion);
        message.setCategory(OntapStorageConstants.ASUP_CATEGORY);
        message.setSeverity(OntapStorageConstants.ASUP_SEVERITY);
        message.setAutosupportRequired(Boolean.FALSE);
        return message;
    }

    /**
     * CloudStack version of this management server, same source as {@link ManagementService#getVersion()}
     * / {@code listCapabilities}. Falls back to "unknown" when the server JAR has no manifest
     * (for example running from an IDE).
     */
    protected String getCloudStackVersion() {
        String version = managementService != null ? managementService.getVersion() : null;
        return StringUtils.isBlank(version) ? OntapStorageConstants.ASUP_UNKNOWN : version;
    }

    /**
     * Number of management servers registered in {@code mshost} (not removed), including nodes
     * that are not currently {@code Up}.
     */
    protected int getManagementServerCount() {
        try {
            List<ManagementServerHostVO> hosts = managementServerHostDao.listAll();
            return hosts == null ? 0 : hosts.size();
        } catch (Exception e) {
            logger.debug("ONTAP ASUP: unable to count management servers: {}", e.getMessage());
            return 0;
        }
    }

    /** Resolves the management server hostname for the EMS computer-name field. */
    protected String getComputerName() {
        String hostName = NetUtils.getCanonicalHostName();
        return StringUtils.isBlank(hostName) ? OntapStorageConstants.ASUP_UNKNOWN : hostName;
    }

    /**
     * Resolves the management server operating system (name, version and architecture) from JVM
     * system properties, e.g. {@code "Linux 5.15.0-91-generic (amd64)"}. Falls back to "unknown".
     */
    protected String getOperatingSystem() {
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        String osArch = System.getProperty("os.arch");
        if (StringUtils.isBlank(osName)) {
            return OntapStorageConstants.ASUP_UNKNOWN;
        }
        StringBuilder sb = new StringBuilder(osName);
        if (StringUtils.isNotBlank(osVersion)) {
            sb.append(' ').append(osVersion);
        }
        if (StringUtils.isNotBlank(osArch)) {
            sb.append(" (").append(osArch).append(')');
        }
        return sb.toString();
    }

    private String defaultUnknown(String value) {
        return StringUtils.isBlank(value) ? OntapStorageConstants.ASUP_UNKNOWN : value;
    }

    /**
     * Returns a usable interval in hours. {@link OntapStorageConstants#ASUP_DISABLED_INTERVAL_HOURS}
     * means telemetry is off. Out-of-range or missing DB values (for example set outside the API)
     * fall back to the default so ASUP is not sent on an unintended cadence. The scheduler
     * converts a non-zero value to seconds via {@link Duration#ofHours(long)}.
     */
    int getAsupIntervalHours(Integer configured) {
        if (configured == null) {
            return OntapStorageConstants.ASUP_DEFAULT_INTERVAL_HOURS;
        }
        if (configured == OntapStorageConstants.ASUP_DISABLED_INTERVAL_HOURS) {
            return OntapStorageConstants.ASUP_DISABLED_INTERVAL_HOURS;
        }
        if (configured < OntapStorageConstants.ASUP_MIN_INTERVAL_HOURS
                || configured > OntapStorageConstants.ASUP_MAX_INTERVAL_HOURS) {
            logger.warn("ONTAP ASUP: {} value [{}] is outside [{}-{}]; using default [{}]",
                    OntapStorageConstants.ASUP_INTERVAL_CONFIG_KEY, configured,
                    OntapStorageConstants.ASUP_MIN_INTERVAL_HOURS,
                    OntapStorageConstants.ASUP_MAX_INTERVAL_HOURS,
                    OntapStorageConstants.ASUP_DEFAULT_INTERVAL_HOURS);
            return OntapStorageConstants.ASUP_DEFAULT_INTERVAL_HOURS;
        }
        return configured;
    }

    /**
     * One ONTAP cluster HTTP client plus the cluster GET and node-model GET for this ASUP cycle,
     * keyed by {@code storageIP}. Discarded when the cycle ends.
     */
    static final class AsupClusterClient {
        final StorageStrategy strategy;
        final Cluster cluster;

        AsupClusterClient(StorageStrategy strategy, Cluster cluster) {
            this.strategy = strategy;
            this.cluster = cluster;
        }
    }

}
