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
package com.cloud.hypervisor.kvm.resource;

import com.cloud.agent.properties.AgentProperties;
import com.cloud.agent.properties.AgentPropertiesFileHandler;
import com.cloud.ha.HighAvailabilityManager;
import com.cloud.utils.script.Script;
import org.apache.commons.lang3.StringUtils;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;
import org.libvirt.StoragePool;
import org.libvirt.StoragePoolInfo.StoragePoolState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class KVMHAMonitor extends KVMHABase implements Runnable {

    private final Map<String, HAStoragePool> haStoragePools = new ConcurrentHashMap<>();
    private final boolean rebootHostAndAlertManagementOnHeartbeatTimeout;

    private final String hostPrivateIp;
    private final String hostGuid;

    /**
     * @param host     the private IP address of the host, which names its heartbeat on NFS and SharedMountPoint pools
     * @param hostGuid the GUID of the host as the management server knows it, which names its heartbeat on RBD pools
     */
    public KVMHAMonitor(String host, String hostGuid) {
        hostPrivateIp = host;
        this.hostGuid = hostGuid;
        rebootHostAndAlertManagementOnHeartbeatTimeout = AgentPropertiesFileHandler.getPropertyValue(AgentProperties.REBOOT_HOST_AND_ALERT_MANAGEMENT_ON_HEARTBEAT_TIMEOUT);
    }

    public void addStoragePool(HAStoragePool pool) {
        synchronized (haStoragePools) {
            haStoragePools.put(pool.getPoolUUID(), pool);
        }
    }

    public void unregisterStoragePool(String uuid) {
        synchronized (haStoragePools) {
            haStoragePools.remove(uuid);
        }
    }

    public void removeStoragePool(String uuid) {
        synchronized (haStoragePools) {
            HAStoragePool pool = haStoragePools.get(uuid);
            if (pool != null) {
                if (StringUtils.isNotEmpty(pool.getMountDestPath())) {
                    Script umount = new Script("umount", logger);
                    umount.add(pool.getMountDestPath());
                    umount.execute();
                }
                haStoragePools.remove(uuid);
            }
        }
    }

    public List<HAStoragePool> getStoragePools() {
        synchronized (haStoragePools) {
            return new ArrayList<>(haStoragePools.values());
        }
    }

    public HAStoragePool getStoragePool(String uuid) {
        synchronized (haStoragePools) {
            return haStoragePools.get(uuid);
        }
    }

    protected void runHeartBeat() {
        synchronized (haStoragePools) {
            Set<String> removedPools = new HashSet<>();
            for (String uuid : haStoragePools.keySet()) {
                HAStoragePool primaryStoragePool = haStoragePools.get(uuid);
                if (HighAvailabilityManager.LIBVIRT_STORAGE_POOL_TYPES_WITH_HA_SUPPORT.contains(primaryStoragePool.getPool().getType())) {
                    checkForNotExistingLibvirtStoragePools(removedPools, uuid);
                    if (removedPools.contains(uuid)) {
                        continue;
                    }
                }
                String result = null;
                result = executePoolHeartBeatCommand(uuid, primaryStoragePool, result);

                if (result != null && rebootHostAndAlertManagementOnHeartbeatTimeout) {
                    logger.warn("Write heartbeat for pool [{}] failed: {}; stopping cloudstack-agent.", uuid, result);
                    try {
                        primaryStoragePool.getPool().createHeartBeatCommand(primaryStoragePool, null, false);
                    } catch (RuntimeException e) {
                        logger.error("Unable to stop the host after the failed heartbeat of pool [{}]", uuid, e);
                    }
                }
            }
            if (!removedPools.isEmpty()) {
                for (String uuid : removedPools) {
                    removeStoragePool(uuid);
                }
            }
        }
    }

    private String executePoolHeartBeatCommand(String uuid, HAStoragePool primaryStoragePool, String result) {
        for (int attempt = 1; attempt <= _heartBeatUpdateMaxTries; attempt++) {
            try {
                String heartBeatHostId = primaryStoragePool.getPool().getHeartBeatHostId(hostPrivateIp, hostGuid);
                result = primaryStoragePool.getPool().createHeartBeatCommand(primaryStoragePool, heartBeatHostId, true);
            } catch (RuntimeException e) {
                // the heartbeat can not even be started, e.g. the pool is not configured completely or its script is missing:
                // it is a failure of this pool only, which must not stop the heartbeats of the other pools, and trying again would not help
                logger.error("Unable to run the heartbeat for pool [{}]: {}", uuid, e.toString(), e);
                return "unable to run the heartbeat: " + e;
            }
            if (result == null) {
                break;
            }

            logger.warn("Write heartbeat for pool [{}] failed: {}; try: {} of {}.", uuid, result, attempt, _heartBeatUpdateMaxTries);
            try {
                Thread.sleep(_heartBeatUpdateRetrySleepInMs);
            } catch (InterruptedException e) {
                logger.debug("[IGNORED] Interrupted between heartbeat retries.", e);
            }
        }
        return result;
    }

    private void checkForNotExistingLibvirtStoragePools(Set<String> removedPools, String uuid) {
        try {
            Connect conn = LibvirtConnection.getConnection();
            StoragePool storage = conn.storagePoolLookupByUUIDString(uuid);
            if (storage == null || storage.getInfo().state != StoragePoolState.VIR_STORAGE_POOL_RUNNING) {
                if (storage == null) {
                    logger.debug("Libvirt storage pool [{}] not found, removing from HA list.", uuid);
                } else {
                    logger.debug("Libvirt storage pool [{}] found, but not running, removing from HA list.", uuid);
                }

                removedPools.add(uuid);
            }

            logger.debug("Found storage pool [{}] in libvirt, continuing.", uuid);

        } catch (LibvirtException e) {
            logger.debug("Failed to lookup libvirt storage pool [{}].", uuid, e);

            if (e.toString().contains("pool not found")) {
                logger.debug("Removing pool [{}] from HA monitor since it was deleted.", uuid);
                removedPools.add(uuid);
            }
        }
    }

    @Override
    public void run() {
        while (true) {

            try {
                runHeartBeat();
            } catch (RuntimeException e) {
                // the thread must go on: if it stopped, the heartbeats of all the pools would stop and the host would look dead
                logger.error("Unexpected error while writing the heartbeats", e);
            }

            try {
                Thread.sleep(_heartBeatUpdateFreqInMs);
            } catch (InterruptedException e) {
                logger.debug("[IGNORED] Interrupted between heartbeats.", e);
            }
        }
    }
}
