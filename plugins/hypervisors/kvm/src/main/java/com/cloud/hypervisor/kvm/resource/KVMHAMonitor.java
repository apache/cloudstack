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

    public KVMHAMonitor(String host) {
        hostPrivateIp = host;
        rebootHostAndAlertManagementOnHeartbeatTimeout = AgentPropertiesFileHandler.getPropertyValue(AgentProperties.REBOOT_HOST_AND_ALERT_MANAGEMENT_ON_HEARTBEAT_TIMEOUT);
    }

    public void addStoragePool(HAStoragePool pool) {
        synchronized (haStoragePools) {
            haStoragePools.put(pool.getPoolUUID(), pool);
        }
    }

    public void removeStoragePool(String uuid) {
        synchronized (haStoragePools) {
            HAStoragePool pool = haStoragePools.get(uuid);
            if (pool != null) {
                Script.runSimpleBashScript("umount " + pool.getMountDestPath());
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
                    primaryStoragePool.getPool().createHeartBeatCommand(primaryStoragePool, null, false);;
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
            result = primaryStoragePool.getPool().createHeartBeatCommand(primaryStoragePool, hostPrivateIp, true);
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

            logger.debug("Found NFS storage pool [{}] in libvirt, continuing.", uuid);

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

            runHeartBeat();

            try {
                Thread.sleep(_heartBeatUpdateFreqInMs);
            } catch (InterruptedException e) {
                logger.debug("[IGNORED] Interrupted between heartbeats.", e);
            }
        }
    }
}
