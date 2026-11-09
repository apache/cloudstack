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
package com.cloud.storage.clvm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.DetailVO;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.Storage;
import com.cloud.storage.StoragePool;
import com.cloud.storage.VolumeDetailVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.utils.concurrency.NamedThreadFactory;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.storage.clvm.command.ClvmLockTransferCommand;
import org.apache.cloudstack.storage.clvm.command.ClvmLockTransferAnswer;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class ClvmPoolManager implements Configurable {
    @Inject
    private VolumeDetailsDao _volsDetailsDao;
    @Inject
    private AgentManager _agentMgr;
    @Inject
    private HostDao _hostDao;
    @Inject
    private HostDetailsDao _hostDetailsDao;
    @Inject
    private PrimaryDataStoreDao _storagePoolDao;

    protected Logger logger = LogManager.getLogger(getClass());

    /**
     * Constant for the volume detail key that stores the host ID currently holding the CLVM exclusive lock.
     * This is used during lightweight lock migration to determine the source host for lock transfer.
     */
    public static final String CLVM_LOCK_HOST_ID = "clvmLockHostId";

    private static final String LOCK_START_IN_PROGRESS = "lock start in progress";
    private static final List<String> TRANSIENT_LOCK_REFUSALS = List.of("locked by other host", LOCK_START_IN_PROGRESS);

    public static final ConfigKey<Integer> ClvmLockRecoveryRetryTimeout = new ConfigKey<>("Advanced", Integer.class, "clvm.lock.recovery.retry.timeout", "180",
            "Maximum time in seconds, counted from the start of the CLVM lock holder lookup, to keep retrying the exclusive activation of a volume that is refused only temporarily: the previous lock holder's lease has not expired yet, or the lockspace of the volume group is still being started on the host. The lease expires 8 x io_timeout + 60 seconds after the holder stops renewing it (140 seconds with the default lvmlockd sanlock io_timeout of 10). If lvmlockd runs with a different --sanlock-timeout, set this to at least that value plus about 40 seconds. 0 disables the retry.", true);

    public static final ConfigKey<Integer> ClvmLockRecoveryRetryInterval = new ConfigKey<>("Advanced", Integer.class, "clvm.lock.recovery.retry.interval", "10",
            "Time in seconds between retries of the exclusive activation of a CLVM volume that is refused only temporarily (previous lock holder's lease not expired yet, or lockspace still starting).", true);

    public static final ConfigKey<Boolean> CLVMSecureZeroFill = new ConfigKey<>("Advanced", Boolean.class, "clvm.secure.zero.fill", "false",
            "When enabled, CLVM volumes to be zero-filled at the time of deletion to prevent data from being recovered by VMs reusing the space, as thick LVM volumes write data linearly. Note: This setting is propagated to hosts when they connect to the storage pool. Changing this setting requires disconnecting and reconnecting hosts or restarting the KVM agent for it to take effect.", false, ConfigKey.Scope.StoragePool);

    public static final ConfigKey<Integer> ClvmLockQueryThreads = new ConfigKey<>("Advanced", Integer.class, "clvm.lock.query.threads", "5",
            "Number of threads a management server uses to send CLVM lock commands to several hosts in parallel: the fallback lock holder lookup that asks every host, and the deactivation of a template LV on all hosts before it is removed. Requires a management server restart.", false);

    private static final int MAX_LEASE_READERS = 2;

    private ExecutorService lockQueryExecutor;

    /** State shared by the lock lookups of all volumes of one operation. */
    public static class LockLookup {
        private final long activationDeadlineMs;
        private final Long preferredHostId;
        private final Set<Long> unreachableHostIds = ConcurrentHashMap.newKeySet();

        LockLookup(long activationDeadlineMs, Long preferredHostId) {
            this.activationDeadlineMs = activationDeadlineMs;
            this.preferredHostId = preferredHostId;
        }

        boolean isUnreachable(Long hostId) {
            return unreachableHostIds.contains(hostId);
        }
    }

    /** Created on first use, as the config value is not available when the bean is constructed. */
    private synchronized ExecutorService getLockQueryExecutor() {
        if (lockQueryExecutor == null) {
            int threads = Math.max(1, ClvmLockQueryThreads.value());
            ThreadPoolExecutor executor = new ThreadPoolExecutor(threads, threads, 60L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(), new NamedThreadFactory("ClvmLockQuery"));
            executor.allowCoreThreadTimeOut(true);
            lockQueryExecutor = executor;
        }
        return lockQueryExecutor;
    }

    public static boolean isClvmPoolType(Storage.StoragePoolType poolType) {
        return Arrays.asList(Storage.StoragePoolType.CLVM, Storage.StoragePoolType.CLVM_NG).contains(poolType);
    }

    private static String getVgName(StoragePool pool) {
        String vgName = pool.getPath();
        return vgName.startsWith("/") ? vgName.substring(1) : vgName;
    }

    private static String getLvPath(StoragePool pool, String lvName) {
        return String.format("/dev/%s/%s", getVgName(pool), lvName);
    }

    /**
     * Pools of the same zone backed by the same VG share its LVs, e.g. one template LV used as backing file by volumes of all of them.
     *
     * @return ids of the pools sharing the VG of {@code pool}, including {@code pool}
     */
    public Set<Long> findPoolIdsSharingVg(StoragePool pool) {
        String vgName = getVgName(pool);
        Set<Long> poolIds = _storagePoolDao.findPoolsByStorageTypeAndZone(pool.getPoolType(), pool.getDataCenterId()).stream()
                .filter(p -> vgName.equals(getVgName(p)))
                .map(StoragePoolVO::getId)
                .collect(Collectors.toCollection(HashSet::new));
        poolIds.add(pool.getId());
        return poolIds;
    }

    /**
     * Deactivates an LV on every Up host of the pools sharing its VG, releasing their shared locks so it can be removed.
     * Best effort: a host still using the LV refuses, and removing it then fails on the lock.
     */
    public void deactivateOnAllHosts(StoragePool pool, String lvName) {
        String lvPath = getLvPath(pool, lvName);
        Map<Long, HostVO> hosts = new LinkedHashMap<>();
        findPoolIdsSharingVg(pool).stream()
                .map(_storagePoolDao::findById)
                .filter(Objects::nonNull)
                .flatMap(p -> findPoolHosts(p).stream())
                .filter(h -> h.getHypervisorType() == Hypervisor.HypervisorType.KVM)
                .forEach(h -> hosts.putIfAbsent(h.getId(), h));
        List<Long> upHostIds = hosts.values().stream().filter(this::isUpKvmRoutingHost).map(HostVO::getId).collect(Collectors.toList());
        Map<Long, Boolean> results = callOnHosts(upHostIds, lvName, hostId -> deactivateOnHost(hosts.get(hostId), lvPath, lvName));
        List<String> notDeactivated = hosts.values().stream()
                .filter(h -> !Boolean.TRUE.equals(results.get(h.getId())))
                .map(h -> String.format("%s (%s, %s)", h.getName(), h.getUuid(), h.getStatus()))
                .collect(Collectors.toList());
        if (notDeactivated.isEmpty()) {
            logger.info("Deactivated {} on all hosts {}", lvPath, hosts.values().stream().map(HostVO::getName).collect(Collectors.toList()));
            return;
        }
        logger.warn("{} could not be deactivated on hosts {}. It cannot be removed while active there; deactivate it on those hosts with 'lvchange -an {}'.",
                lvPath, notDeactivated, lvPath);
    }

    private Boolean deactivateOnHost(HostVO host, String lvPath, String lvName) {
        try {
            Answer answer = _agentMgr.send(host.getId(), new ClvmLockTransferCommand(ClvmLockTransferCommand.Operation.DEACTIVATE, lvPath, lvName));
            if (answer != null && answer.getResult()) {
                return true;
            }
            logger.warn("Failed to deactivate {} on host {}: {}", lvPath, host.getName(), answer != null ? answer.getDetails() : "null answer");
        } catch (AgentUnavailableException | OperationTimedoutException e) {
            logger.warn("Could not deactivate {} on host {}: {}", lvPath, host.getName(), e.getMessage());
        }
        return false;
    }

    /**
     * Gets the CLVM lock host ID for a volume, optionally querying actual LVM state.
     *
     * @param volumeId The volume ID
     * @param volumeUuid The volume UUID
     * @return Host ID that holds the lock, or null if not found
     * @deprecated Use getClvmLockHostId(volumeId, volumeUuid, volumePath, pool, queryActual) instead
     */
    public Long getClvmLockHostId(Long volumeId, String volumeUuid) {
        VolumeDetailVO detail = _volsDetailsDao.findDetail(volumeId, CLVM_LOCK_HOST_ID);
        if (detail != null && detail.getValue() != null && !detail.getValue().isEmpty()) {
            try {
                return Long.parseLong(detail.getValue());
            } catch (NumberFormatException e) {
                logger.warn("Invalid clvmLockHostId in volume_details for volume {}: {}",
                        volumeUuid, detail.getValue());
            }
        }
        return null;
    }

    /**
     * Gets the CLVM lock host ID for a volume, optionally querying actual LVM state.
     * This method can query the actual lock state from LVM (source of truth) instead of
     * relying solely on potentially stale database records.
     *
     * @param volumeId The volume ID
     * @param volumeUuid The volume UUID
     * @param volumePath The LV path (required if queryActual is true)
     * @param pool The storage pool (required if queryActual is true)
     * @param queryActual If true, queries actual LVM state instead of database
     * @return Host ID that holds the lock, or null if not found
     */
    public Long getClvmLockHostId(Long volumeId, String volumeUuid, String volumePath,
                                  StoragePool pool, boolean queryActual) {
        return getClvmLockHostId(volumeId, volumeUuid, volumePath, pool, queryActual, newLockLookup());
    }

    /** Same as above, sharing {@code lookup} with the other volumes of the same operation, see {@link #newLockLookup()}. */
    public Long getClvmLockHostId(Long volumeId, String volumeUuid, String volumePath,
                                  StoragePool pool, boolean queryActual, LockLookup lookup) {
        if (queryActual) {
            if (volumePath == null || pool == null) {
                logger.warn("Cannot query actual CLVM lock state for volume {} - missing volumePath or pool", volumeUuid);
                return getClvmLockHostId(volumeId, volumeUuid);
            }
            return queryCurrentLockHolder(volumeId, volumeUuid, volumePath, pool, true, lookup);
        }

        return getClvmLockHostId(volumeId, volumeUuid);
    }

    /** @return lookup state for one operation; its activation retry deadline is counted from now */
    public LockLookup newLockLookup() {
        return newLockLookup(null);
    }

    /** Same as above; a volume no host holds is activated on {@code preferredHostId} when possible. */
    public LockLookup newLockLookup(Long preferredHostId) {
        return new LockLookup(now() + getRetryTimeoutMillis(), preferredHostId);
    }

    /**
     * Safely sets or updates the CLVM_LOCK_HOST_ID detail for a volume.
     * If the detail already exists, it will be updated. Otherwise, it will be created.
     *
     * @param volumeId The ID of the volume
     * @param hostId The host ID that holds/should hold the CLVM exclusive lock
     */
    public void setClvmLockHostId(long volumeId, long hostId) {
        VolumeDetailVO existingDetail = _volsDetailsDao.findDetail(volumeId, CLVM_LOCK_HOST_ID);
        if (existingDetail != null) {
            existingDetail.setValue(String.valueOf(hostId));
            _volsDetailsDao.update(existingDetail.getId(), existingDetail);
            logger.debug("Updated CLVM_LOCK_HOST_ID for volume {} to host {}", volumeId, hostId);
            return;
        }
        _volsDetailsDao.addDetail(volumeId, CLVM_LOCK_HOST_ID, String.valueOf(hostId), false);
        logger.debug("Created CLVM_LOCK_HOST_ID for volume {} with host {}", volumeId, hostId);
    }

    /**
     * Query LVM to find the actual current lock holder for a volume.
     * This is the SOURCE OF TRUTH - it queries the actual LVM state via sanlock/lvmlockd.
     *
     * <p>If no host holds the exclusive lock (e.g. after a storage outage), this method attempts
     * an exclusive activation on the best available host before giving up. Activation failure
     * is non-fatal: the method returns null so callers can apply their own fallback logic.
     *
     * @param volumeId The volume ID
     * @param volumeUuid The volume UUID
     * @param volumePath The LV path (e.g., "vm-123-disk-0")
     * @param pool The storage pool
     * @param updateDatabase If true, persists the discovered or newly-activated lock host to the DB
     * @return Host ID of current or newly-activated lock holder, or null if none found/activated
     */
    public Long queryCurrentLockHolder(Long volumeId, String volumeUuid, String volumePath,
                                       StoragePool pool, boolean updateDatabase) {
        return queryCurrentLockHolder(volumeId, volumeUuid, volumePath, pool, updateDatabase, newLockLookup());
    }

    /** Same as above, sharing the retry deadline and the unreachable hosts in {@code lookup}. */
    public Long queryCurrentLockHolder(Long volumeId, String volumeUuid, String volumePath,
                                       StoragePool pool, boolean updateDatabase, LockLookup lookup) {
        if (pool == null) {
            logger.error("Cannot query CLVM lock for volume {} - pool is null", volumeUuid);
            return null;
        }

        String lvPath = getLvPath(pool, volumePath);
        Long dbHostId = getClvmLockHostId(volumeId, volumeUuid);
        // Hosts that did not answer the lock-state query for this volume
        Set<Long> noAnswerHostIds = new HashSet<>();

        Boolean dbHostActive = queryDbHostLockState(dbHostId, lvPath, volumeUuid, noAnswerHostIds, lookup);
        if (Boolean.TRUE.equals(dbHostActive)) {
            logger.debug("Fast path: volume {} confirmed active on DB host {}", volumeUuid, dbHostId);
            return dbHostId;
        }

        List<HostVO> hosts = findPoolHosts(pool);

        ClvmLockTransferAnswer lease = hosts.isEmpty() ? null : readLease(lvPath, volumeUuid, dbHostId, hosts, lookup);
        if (lease != null) {
            if (isLeaseFree(lease, volumeUuid)) {
                return recoverInactiveVolume(volumeId, volumeUuid, lvPath, dbHostId, hosts, noAnswerHostIds, updateDatabase, lookup);
            }
            List<Long> holderSanlockIds = getLeaseHolderIds(lease);
            if (holderSanlockIds.size() > 1) {
                logger.warn("Volume {} is held in shared mode by sanlock hosts {}. Skipping exclusive lock transfer.",
                        volumeUuid, holderSanlockIds);
                return null;
            }
            Long lockHostId = findHostBySanlockId(hosts, holderSanlockIds.get(0));
            if (lockHostId != null) {
                if (isLeaseHolderInUse(lockHostId, dbHostId, dbHostActive, lease.isLeaseOwnerRenewing(), lvPath, volumeUuid, lookup)) {
                    logger.info("Volume {} is held by host {} (sanlock lease, DB had {})", volumeUuid, lockHostId, dbHostId);
                    return recordLockHolder(volumeId, volumeUuid, dbHostId, lockHostId, updateDatabase);
                }
                return recoverInactiveVolume(volumeId, volumeUuid, lvPath, dbHostId, hosts, noAnswerHostIds, updateDatabase, lookup);
            }
        }
        return findLockHolderByFanOut(volumeId, volumeUuid, lvPath, pool, dbHostId, hosts, noAnswerHostIds, updateDatabase, lookup);
    }

    /**
     * Fast path: asks the host recorded in the DB, as an LV is only active on the host holding its lock.
     *
     * @return the lock state on the DB host, or null when it was not asked or did not answer
     */
    private Boolean queryDbHostLockState(Long dbHostId, String lvPath, String volumeUuid, Set<Long> noAnswerHostIds, LockLookup lookup) {
        if (dbHostId == null) {
            return null;
        }
        if (lookup.isUnreachable(dbHostId)) {
            logger.info("Fast path skip: DB host {} for volume {} did not answer an earlier query", dbHostId, volumeUuid);
            return null;
        }
        if (!isUpKvmRoutingHost(_hostDao.findById(dbHostId))) {
            logger.info("Fast path skip: DB host {} for volume {} is down/missing", dbHostId, volumeUuid);
            return null;
        }
        Boolean active = querySingleHostLockState(dbHostId, lvPath, volumeUuid, lookup);
        if (active == null) {
            noAnswerHostIds.add(dbHostId);
        } else if (!active) {
            logger.info("Fast path miss: volume {} not active on DB host {}", volumeUuid, dbHostId);
        }
        return active;
    }

    /** Asks every host whether the volume is active there; used when the sanlock lease cannot name the holder. */
    private Long findLockHolderByFanOut(Long volumeId, String volumeUuid, String lvPath, StoragePool pool, Long dbHostId,
                                        List<HostVO> hosts, Set<Long> noAnswerHostIds, boolean updateDatabase, LockLookup lookup) {
        if (hosts.isEmpty()) {
            logger.warn("No KVM routing hosts found to query CLVM lock state for volume {} (pool: {}, cluster: {}, zone: {})",
                    volumeUuid, pool.getName(), pool.getClusterId(), pool.getDataCenterId());
            return null;
        }

        // The DB host was already queried in the fast path
        List<Long> hostIdsToQuery = hosts.stream()
                .filter(this::isUpKvmRoutingHost)
                .map(HostVO::getId)
                .filter(id -> !id.equals(dbHostId) && !lookup.isUnreachable(id))
                .collect(Collectors.toList());
        Map<Long, Boolean> lockStates = queryHostsLockState(hostIdsToQuery, lvPath, volumeUuid, lookup);

        List<Long> activeHostIds = new ArrayList<>();
        for (Long hostId : hostIdsToQuery) {
            Boolean active = lockStates.get(hostId);
            if (Boolean.TRUE.equals(active)) {
                logger.debug("Volume {} is locally active on host {} (fan-out)", volumeUuid, hostId);
                activeHostIds.add(hostId);
            } else if (active == null) {
                noAnswerHostIds.add(hostId);
            }
        }

        if (activeHostIds.isEmpty()) {
            logger.debug("Volume {} is not active on any reachable host — no exclusive lock held", volumeUuid);
            return recoverInactiveVolume(volumeId, volumeUuid, lvPath, dbHostId, hosts, noAnswerHostIds, updateDatabase, lookup);
        }

        if (activeHostIds.size() > 1) {
            logger.warn("Volume {} is active on {} hosts {}, shared-mode LV (template?). "
                    + "Skipping exclusive lock transfer.",
                    volumeUuid, activeHostIds.size(), activeHostIds);
            return null;
        }

        Long lockHostId = activeHostIds.get(0);
        logger.info("Volume {} is exclusively active on host {} (found via fan-out, DB had {})",
                volumeUuid, lockHostId, dbHostId);
        return recordLockHolder(volumeId, volumeUuid, dbHostId, lockHostId, updateDatabase);
    }

    private Long recordLockHolder(Long volumeId, String volumeUuid, Long dbHostId, Long lockHostId, boolean updateDatabase) {
        if (updateDatabase && !lockHostId.equals(dbHostId)) {
            logger.info("Correcting database: volume {} lock host: {} -> {} (actual)",
                    volumeUuid, dbHostId, lockHostId);
            setClvmLockHostId(volumeId, lockHostId);
        }
        return lockHostId;
    }

    /**
     * Reads the sanlock lease of the volume on one host; any host in the lockspace sees every lease.
     * Tries a second host if the first does not answer.
     *
     * @return the lease, or null when no host could read it
     */
    private ClvmLockTransferAnswer readLease(String lvPath, String volumeUuid, Long dbHostId, List<HostVO> hosts, LockLookup lookup) {
        for (Long hostId : selectLeaseReaders(dbHostId, hosts, lookup)) {
            try {
                Answer answer = _agentMgr.send(hostId, new ClvmLockTransferCommand(
                        ClvmLockTransferCommand.Operation.QUERY_LEASE_OWNER, lvPath, volumeUuid));
                if (answer instanceof ClvmLockTransferAnswer && answer.getResult()
                        && ((ClvmLockTransferAnswer) answer).getLeaseMode() != null) {
                    return (ClvmLockTransferAnswer) answer;
                }
                logger.debug("Host {} could not read the sanlock lease of volume {}: {}",
                        hostId, volumeUuid, answer != null ? answer.getDetails() : "null answer");
            } catch (AgentUnavailableException | OperationTimedoutException e) {
                logger.debug("Could not query host {} for the sanlock lease of volume {}: {}", hostId, volumeUuid, e.getMessage());
                lookup.unreachableHostIds.add(hostId);
            }
        }
        return null;
    }

    /** Up hosts that can read the lease, the preferred host and then the DB host first. */
    private List<Long> selectLeaseReaders(Long dbHostId, List<HostVO> hosts, LockLookup lookup) {
        List<Long> hostIds = hosts.stream()
                .filter(this::isUpKvmRoutingHost)
                .map(HostVO::getId)
                .filter(id -> !lookup.isUnreachable(id))
                .collect(Collectors.toList());
        Collections.shuffle(hostIds);
        for (Long firstHostId : Arrays.asList(dbHostId, lookup.preferredHostId)) {
            if (firstHostId != null && hostIds.remove(firstHostId)) {
                hostIds.add(0, firstHostId);
            }
        }
        return hostIds.subList(0, Math.min(MAX_LEASE_READERS, hostIds.size()));
    }

    /**
     * Whether the host named by a live lease still uses the volume. A host that restarted keeps its old lease until it
     * rejoins the lockspace or the lease expires, and the LV is then not active there. When the host cannot be asked,
     * a lease that is still renewed means the host is alive, otherwise it is down and its lease will expire.
     */
    private boolean isLeaseHolderInUse(Long lockHostId, Long dbHostId, Boolean dbHostActive, boolean ownerRenewing,
                                       String lvPath, String volumeUuid, LockLookup lookup) {
        Boolean active;
        if (lockHostId.equals(dbHostId)) {
            active = dbHostActive;
        } else {
            active = isUpKvmRoutingHost(_hostDao.findById(lockHostId)) ? querySingleHostLockState(lockHostId, lvPath, volumeUuid, lookup) : null;
        }
        if (Boolean.TRUE.equals(active)) {
            return true;
        }
        if (Boolean.FALSE.equals(active)) {
            logger.info("Volume {} is not active on host {} although its sanlock lease names it, the host restarted; treating the lease as free",
                    volumeUuid, lockHostId);
            return false;
        }
        if (ownerRenewing) {
            logger.warn("Host {} holding volume {} cannot be reached but still renews its sanlock lease; it is alive, so its lock is kept",
                    lockHostId, volumeUuid);
            return true;
        }
        logger.info("Host {} holding volume {} cannot be reached and no longer renews its sanlock lease; activating elsewhere once the lease expires",
                lockHostId, volumeUuid);
        return false;
    }

    /** A lease that is not held, or held by a host that died or restarted since, is free to take. */
    private boolean isLeaseFree(ClvmLockTransferAnswer lease, String volumeUuid) {
        if (lease.getLeaseMode() == ClvmLockTransferAnswer.LeaseMode.FREE) {
            logger.debug("Volume {} is not held by any host (sanlock lease)", volumeUuid);
            return true;
        }
        if (lease.getLeaseMode() == ClvmLockTransferAnswer.LeaseMode.EXCLUSIVE && !lease.isLeaseOwnerAlive()) {
            logger.info("Volume {} is held by sanlock host {}, which is dead or restarted since; treating the lease as free",
                    volumeUuid, lease.getLeaseOwnerId());
            return true;
        }
        return false;
    }

    private List<Long> getLeaseHolderIds(ClvmLockTransferAnswer lease) {
        if (lease.getLeaseMode() == ClvmLockTransferAnswer.LeaseMode.EXCLUSIVE) {
            return List.of(lease.getLeaseOwnerId());
        }
        return lease.getSharedOwnerIds();
    }

    /** @return the only host of the pool reporting this sanlock host id, or null when none or several do */
    private Long findHostBySanlockId(List<HostVO> hosts, Long sanlockId) {
        List<Long> hostIds = findHostIdsBySanlockId(hosts, String.valueOf(sanlockId));
        if (hostIds.size() == 1) {
            return hostIds.get(0);
        }
        logger.warn("Sanlock host id {} matches hosts {} of the pool, expected exactly one. Querying all hosts instead.",
                sanlockId, hostIds);
        return null;
    }

    private List<Long> findHostIdsBySanlockId(List<HostVO> hosts, String sanlockId) {
        return hosts.stream()
                .filter(h -> h.getType() == Host.Type.Routing && h.getHypervisorType() == Hypervisor.HypervisorType.KVM)
                .map(HostVO::getId)
                .filter(id -> sanlockId.equals(getSanlockId(id)))
                .collect(Collectors.toList());
    }

    private String getSanlockId(long hostId) {
        DetailVO detail = _hostDetailsDao.findDetail(hostId, Host.HOST_SANLOCK_ID);
        return detail != null ? detail.getValue() : null;
    }

    /**
     * Checks that the host's sanlock host id identifies it among the hosts of the pool, which the lease based
     * lock holder lookup relies on.
     *
     * @return the other hosts of the pool reporting the same sanlock host id
     */
    public List<Long> findHostsSharingSanlockId(StoragePool pool, long hostId) {
        String sanlockId = getSanlockId(hostId);
        if (sanlockId == null) {
            logger.warn("Host {} reports no {}, so CLVM lock holder lookups in pool {} query every host",
                    hostId, Host.HOST_SANLOCK_ID, pool.getName());
            return Collections.emptyList();
        }
        List<Long> hostIds = new ArrayList<>(findHostIdsBySanlockId(findPoolHosts(pool), sanlockId));
        hostIds.remove(Long.valueOf(hostId));
        return hostIds;
    }

    private boolean isUpKvmRoutingHost(HostVO host) {
        return host != null
                && host.getStatus() == Status.Up
                && host.getType() == Host.Type.Routing
                && host.getHypervisorType() == Hypervisor.HypervisorType.KVM;
    }

    private List<HostVO> findPoolHosts(StoragePool pool) {
        List<HostVO> hosts = null;
        if (pool.getClusterId() != null) {
            hosts = _hostDao.findByClusterId(pool.getClusterId(), Host.Type.Routing);
        } else if (pool.getDataCenterId() > 0) {
            hosts = _hostDao.findByDataCenterId(pool.getDataCenterId());
        }
        return hosts != null ? hosts : Collections.emptyList();
    }

    /**
     * Attempts an exclusive activation on the best available host when no host holds the volume.
     * Removes the stale DB record if that fails.
     */
    private Long recoverInactiveVolume(Long volumeId, String volumeUuid, String lvPath, Long dbHostId, List<HostVO> hosts,
                                      Set<Long> noAnswerHostIds, boolean updateDatabase, LockLookup lookup) {
        Set<Long> skipHostIds = new HashSet<>(noAnswerHostIds);
        skipHostIds.addAll(lookup.unreachableHostIds);
        Long targetHostId = selectActivationTargetHost(lookup.preferredHostId, dbHostId, hosts, skipHostIds);
        if (targetHostId != null) {
            Long recoveredHostId = tryActivateExclusivelyOnHost(volumeId, volumeUuid, lvPath,
                    targetHostId, updateDatabase, lookup, TRANSIENT_LOCK_REFUSALS);
            if (recoveredHostId != null) {
                return recoveredHostId;
            }
        }

        if (updateDatabase && dbHostId != null) {
            VolumeDetailVO detail = _volsDetailsDao.findDetail(volumeId, CLVM_LOCK_HOST_ID);
            if (detail != null && String.valueOf(dbHostId).equals(detail.getValue())) {
                _volsDetailsDao.remove(detail.getId());
            }
        }
        return null;
    }

    /** @return lock state per host that answered; hosts that did not answer are absent */
    private Map<Long, Boolean> queryHostsLockState(List<Long> hostIds, String lvPath, String volumeUuid, LockLookup lookup) {
        return callOnHosts(hostIds, volumeUuid, hostId -> querySingleHostLockState(hostId, lvPath, volumeUuid, lookup));
    }

    /**
     * Calls the hosts in parallel, so one slow host does not delay the others.
     *
     * @return result per host; hosts whose call returned null are absent
     */
    private <T> Map<Long, T> callOnHosts(List<Long> hostIds, String volumeUuid, Function<Long, T> call) {
        Map<Long, T> results = new ConcurrentHashMap<>();
        List<Future<?>> futures = new ArrayList<>();
        for (Long hostId : hostIds) {
            futures.add(getLockQueryExecutor().submit(new ManagedContextRunnable() {
                @Override
                protected void runInContext() {
                    T result = call.apply(hostId);
                    if (result != null) {
                        results.put(hostId, result);
                    }
                }
            }));
        }
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (ExecutionException e) {
                logger.warn("Host call for volume {} failed: {}", volumeUuid, e.getMessage());
            }
        }
        return new HashMap<>(results);
    }

    /**
     * Queries a single host for the CLVM LV activation state.
     * A host that cannot be reached is remembered in {@code lookup}.
     *
     * @return {@code Boolean.TRUE} if the LV is active on that host,
     *         {@code Boolean.FALSE} if reachable but inactive,
     *         {@code null} if the host is unreachable or returned an error
     */
    private Boolean querySingleHostLockState(Long hostId, String lvPath, String volumeUuid, LockLookup lookup) {
        try {
            ClvmLockTransferCommand queryCmd = new ClvmLockTransferCommand(
                    ClvmLockTransferCommand.Operation.QUERY_LOCK_STATE, lvPath, volumeUuid);
            Answer answer = _agentMgr.send(hostId, queryCmd);

            if (answer == null || !answer.getResult()) {
                logger.debug("Failed to query lock state from host {}: {}",
                        hostId, answer != null ? answer.getDetails() : "null answer");
                return null;
            }
            if (!(answer instanceof ClvmLockTransferAnswer)) {
                logger.warn("Unexpected answer type from host {} for QUERY_LOCK_STATE: {}",
                        hostId, answer.getClass());
                return null;
            }
            ClvmLockTransferAnswer queryAnswer = (ClvmLockTransferAnswer) answer;
            logger.debug("Host {} reports volume {} active={} (attr={})",
                    hostId, volumeUuid, queryAnswer.isActive(), queryAnswer.getLvAttributes());
            return queryAnswer.isActive();
        } catch (AgentUnavailableException | OperationTimedoutException e) {
            logger.debug("Could not query host {} for lock state: {}", hostId, e.getMessage());
            lookup.unreachableHostIds.add(hostId);
            return null;
        }
    }

    /**
     * Selects the best host on which to exclusively activate an inactive CLVM volume.
     *
     * <p>Priority 0: the preferred host of the lookup, e.g. the host a VM starts on.
     *
     * <p>Priority 1: the last known lock holder ({@code clvmLockHostId} from DB), if that host
     * is UP and KVM.
     *
     * <p>Priority 2: a random UP KVM routing host from the cluster/zone list (fallback).
     *
     * <p>Hosts that did not answer the lock-state query are skipped in both cases.
     *
     * @param preferredHostId    host to use first, may be null
     * @param dbHostId           last known lock holder host ID from the DB (may be null)
     * @param hosts              routing hosts in the cluster or zone collected during fan-out
     * @param unreachableHostIds hosts that failed to answer the lock-state query or are unreachable
     * @return host ID to activate on, or null if no eligible host found
     */
    private Long selectActivationTargetHost(Long preferredHostId, Long dbHostId, List<HostVO> hosts, Set<Long> unreachableHostIds) {
        if (preferredHostId != null && !unreachableHostIds.contains(preferredHostId)
                && isUpKvmRoutingHost(_hostDao.findById(preferredHostId))) {
            logger.debug("selectActivationTargetHost: using preferred host {}", preferredHostId);
            return preferredHostId;
        }
        if (dbHostId != null && !unreachableHostIds.contains(dbHostId)) {
            if (isUpKvmRoutingHost(_hostDao.findById(dbHostId))) {
                logger.debug("selectActivationTargetHost: preferring DB host {} (last known lock holder)", dbHostId);
                return dbHostId;
            }
        }
        if (hosts != null) {
            List<HostVO> eligible = hosts.stream()
                    .filter(h -> isUpKvmRoutingHost(h) && !unreachableHostIds.contains(h.getId()))
                    .collect(Collectors.toList());
            if (!eligible.isEmpty()) {
                Collections.shuffle(eligible);
                HostVO chosen = eligible.get(0);
                logger.debug("selectActivationTargetHost: falling back to random UP KVM host {} in cluster/zone",
                        chosen.getId());
                return chosen.getId();
            }
        }
        logger.warn("selectActivationTargetHost: no eligible UP KVM host found");
        return null;
    }

    /**
     * Sends an {@code ACTIVATE_EXCLUSIVE} command to {@code targetHostId} and optionally
     * persists the new lock host to the database.
     *
     * <p>A temporarily refused activation (lease not expired yet, lockspace still starting) is retried
     * on the same host until the deadline of {@code lookup}. Other failures are not retried.
     *
     * <p>Activation failure is non-fatal: returns null so the caller can apply its own fallback.
     *
     * @param volumeId       volume DB ID
     * @param volumeUuid     volume UUID (for logging)
     * @param lvPath         full LV device path, e.g. {@code /dev/vgname/vol-path}
     * @param targetHostId   host to activate on
     * @param updateDatabase if true, persists the new lock host on success
     * @param lookup         lookup state of the operation: retry deadline and unreachable hosts
     * @return {@code targetHostId} on success, {@code null} if the command failed or threw
     */
    private Long tryActivateExclusivelyOnHost(Long volumeId, String volumeUuid, String lvPath,
                                              Long targetHostId, boolean updateDatabase, LockLookup lookup,
                                              List<String> retryableRefusals) {
        long retryIntervalMs = getRetryIntervalMillis();
        while (true) {
            try {
                ClvmLockTransferCommand activateCmd = new ClvmLockTransferCommand(
                        ClvmLockTransferCommand.Operation.ACTIVATE_EXCLUSIVE, lvPath, volumeUuid);
                Answer activateAnswer = _agentMgr.send(targetHostId, activateCmd);
                if (activateAnswer != null && activateAnswer.getResult()) {
                    logger.info("Exclusively activated volume {} on host {}", volumeUuid, targetHostId);
                    if (updateDatabase) {
                        setClvmLockHostId(volumeId, targetHostId);
                    }
                    return targetHostId;
                }
                logger.warn("Exclusive activation of volume {} on host {} failed: {}",
                        volumeUuid, targetHostId,
                        activateAnswer != null ? activateAnswer.getDetails() : "null answer");
                if (!isRetryableRefusal(activateAnswer, retryableRefusals, retryIntervalMs, lookup.activationDeadlineMs)) {
                    return null;
                }
                logger.info("Activation of volume {} is refused temporarily, retrying on host {} in {} ms until the previous holder's lease expires or the lockspace is started",
                        volumeUuid, targetHostId, retryIntervalMs);
            } catch (AgentUnavailableException | OperationTimedoutException e) {
                logger.warn("Exclusive activation of volume {} on host {} threw exception: {}",
                        volumeUuid, targetHostId, e.getMessage());
                lookup.unreachableHostIds.add(targetHostId);
                return null;
            }
            if (!sleep(retryIntervalMs)) {
                return null;
            }
        }
    }

    private boolean isRetryableRefusal(Answer activateAnswer, List<String> retryableRefusals, long retryIntervalMs, long deadlineMs) {
        return retryIntervalMs > 0
                && activateAnswer != null
                && activateAnswer.getDetails() != null
                && retryableRefusals.stream().anyMatch(activateAnswer.getDetails()::contains)
                && now() + retryIntervalMs <= deadlineMs;
    }

    protected long getRetryTimeoutMillis() {
        return ClvmLockRecoveryRetryTimeout.value() * 1000L;
    }

    protected long getRetryIntervalMillis() {
        return ClvmLockRecoveryRetryInterval.value() * 1000L;
    }

    protected long now() {
        return System.currentTimeMillis();
    }

    /**
     * @return false if the thread was interrupted while waiting
     */
    protected boolean sleep(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Cleans up CLVM lock host tracking detail from volume_details table.
     * Called after successful volume deletion to prevent orphaned records.
     *
     * @param volume The volume being deleted
     */
    public void clearClvmLockHostDetail(VolumeVO volume) {
        try {
            VolumeDetailVO detail = _volsDetailsDao.findDetail(volume.getId(), CLVM_LOCK_HOST_ID);
            if (detail != null) {
                logger.debug("Removing CLVM lock host detail for deleted volume {}", volume.getUuid());
                _volsDetailsDao.remove(detail.getId());
            }
        } catch (Exception e) {
            logger.warn("Failed to clean up CLVM lock host detail for volume {}: {}",
                    volume.getUuid(), e.getMessage());
        }
    }

    /**
     * Transfers the CLVM exclusive lock for a volume from the source host to the destination host.
     *
     * @param volumeUuid   The volume UUID
     * @param volumeId     The volume DB ID
     * @param volumePath   The LV name within the VG (e.g. "vm-123-disk-0")
     * @param pool         The storage pool
     * @param sourceHostId The host currently holding the lock (pre-validated by caller)
     * @param destHostId   The host that should hold the lock after transfer
     * @return true if the lock was successfully transferred and activated on the destination
     */
    public boolean transferClvmVolumeLock(String volumeUuid, Long volumeId, String volumePath,
                                          StoragePool pool, Long sourceHostId, Long destHostId) {
        if (pool == null) {
            logger.error("Cannot transfer CLVM lock for volume {} - pool is null", volumeUuid);
            return false;
        }

        String lvPath = getLvPath(pool, volumePath);

        try {
            // sourceHostId is trusted as pre-validated by the caller
            Long hostToDeactivate = sourceHostId;

            logger.info("Transferring CLVM lock for volume {}: source={}, destination={}",
                    volumeUuid, sourceHostId, destHostId);

            if (hostToDeactivate != null && !hostToDeactivate.equals(destHostId)) {
                HostVO deactivateHost = _hostDao.findById(hostToDeactivate);
                if (deactivateHost != null && deactivateHost.getStatus() == Status.Up) {
                    ClvmLockTransferCommand deactivateCmd = new ClvmLockTransferCommand(
                            ClvmLockTransferCommand.Operation.DEACTIVATE, lvPath, volumeUuid);

                    Answer deactivateAnswer = _agentMgr.send(hostToDeactivate, deactivateCmd);

                    if (deactivateAnswer == null || !deactivateAnswer.getResult()) {
                        logger.warn("Failed to deactivate CLVM volume {} on host {}. Will attempt activation on destination.",
                                volumeUuid, hostToDeactivate);
                    } else {
                        logger.debug("Successfully deactivated volume {} on host {}", volumeUuid, hostToDeactivate);
                    }
                } else {
                    logger.warn("Host {} (current lock holder) is down. Will attempt force claim on destination host {}",
                            hostToDeactivate, destHostId);
                }
            } else if (hostToDeactivate == null) {
                logger.debug("Volume {} has no active lock holder, will directly activate on destination", volumeUuid);
            }

            // A live source refuses with "locked by other host": fail fast. Only wait for the destination's lockspace to start.
            if (tryActivateExclusivelyOnHost(volumeId, volumeUuid, lvPath, destHostId, true,
                    newLockLookup(destHostId), List.of(LOCK_START_IN_PROGRESS)) == null) {
                logger.error("Failed to activate CLVM volume {} exclusively on dest host {}", volumeUuid, destHostId);
                return false;
            }

            logger.info("Successfully transferred CLVM lock for volume {} from host {} to host {}",
                    volumeUuid, sourceHostId != null ? sourceHostId : "none", destHostId);

            return true;

        } catch (AgentUnavailableException | OperationTimedoutException e) {
            logger.error("Exception during CLVM lock transfer for volume {}: {}", volumeUuid, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getConfigComponentName() {
        return ClvmPoolManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {
                CLVMSecureZeroFill,
                ClvmLockRecoveryRetryTimeout,
                ClvmLockRecoveryRetryInterval,
                ClvmLockQueryThreads
        };
    }
}
