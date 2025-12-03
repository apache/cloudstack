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
import com.cloud.storage.Storage.StoragePoolType;
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

    private final Map<String, HAStoragePool> storagePool = new ConcurrentHashMap<>();
    private final boolean rebootHostAndAlertManagementOnHeartbeatTimeout;

    private final String hostPrivateIp;

    public KVMHAMonitor(HAStoragePool pool, String host, String scriptPath) {
        if (pool != null) {
            storagePool.put(pool.getPoolUUID(), pool);
        }
        hostPrivateIp = host;
        configureHeartBeatPath(scriptPath);

        rebootHostAndAlertManagementOnHeartbeatTimeout = AgentPropertiesFileHandler.getPropertyValue(AgentProperties.REBOOT_HOST_AND_ALERT_MANAGEMENT_ON_HEARTBEAT_TIMEOUT);
    }

    private static synchronized void configureHeartBeatPath(String scriptPath) {
        KVMHABase.s_heartBeatPath = scriptPath;
    }

    public void addStoragePool(HAStoragePool pool) {
        synchronized (storagePool) {
            storagePool.put(pool.getPoolUUID(), pool);
        }
    }

    public void removeStoragePool(String uuid) {
        synchronized (storagePool) {
            HAStoragePool pool = storagePool.get(uuid);
            if (pool != null) {
                Script.runSimpleBashScript("umount " + pool.getMountDestPath());
                storagePool.remove(uuid);
            }
        }
    }

    public List<HAStoragePool> getStoragePools() {
        synchronized (storagePool) {
            return new ArrayList<>(storagePool.values());
        }
    }

    public HAStoragePool getStoragePool(String uuid) {
        synchronized (storagePool) {
            return storagePool.get(uuid);
        }
    }

    protected void runHeartBeat() {
        synchronized (storagePool) {
            Set<String> removedPools = new HashSet<>();
            for (String uuid : storagePool.keySet()) {
                HAStoragePool primaryStoragePool = storagePool.get(uuid);
                if (primaryStoragePool.getPool().getType() == StoragePoolType.NetworkFilesystem) {
                    checkForNotExistingPools(removedPools, uuid);
                    if (removedPools.contains(uuid)) {
                        continue;
                    }
                }
                String result = null;
                result = executePoolHeartBeatCommand(uuid, primaryStoragePool, result);

                if (result != null && rebootHostAndAlertManagementOnHeartbeatTimeout) {
                    logger.warn(String.format("Write heartbeat for pool [%s] failed: %s; stopping cloudstack-agent.", uuid, result));
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
        for (int i = 1; i <= _heartBeatUpdateMaxTries; i++) {
            result = primaryStoragePool.getPool().createHeartBeatCommand(primaryStoragePool, hostPrivateIp, true);

            if (result != null) {
                logger.warn(String.format("Write heartbeat for pool [%s] failed: %s; try: %s of %s.", uuid, result, i, _heartBeatUpdateMaxTries));
                try {
                    Thread.sleep(_heartBeatUpdateRetrySleep);
                } catch (InterruptedException e) {
                    logger.debug("[IGNORED] Interrupted between heartbeat retries.", e);
                }
            } else {
                break;
            }

        }
        return result;
    }

    private void checkForNotExistingPools(Set<String> removedPools, String uuid) {
        try {
            Connect conn = LibvirtConnection.getConnection();
            StoragePool storage = conn.storagePoolLookupByUUIDString(uuid);
            if (storage == null || storage.getInfo().state != StoragePoolState.VIR_STORAGE_POOL_RUNNING) {
                if (storage == null) {
                    logger.debug(String.format("Libvirt storage pool [%s] not found, removing from HA list.", uuid));
                } else {
                    logger.debug(String.format("Libvirt storage pool [%s] found, but not running, removing from HA list.", uuid));
                }

                removedPools.add(uuid);
            }

            logger.debug(String.format("Found NFS storage pool [%s] in libvirt, continuing.", uuid));

        } catch (LibvirtException e) {
            logger.debug(String.format("Failed to lookup libvirt storage pool [%s].", uuid), e);

            if (e.toString().contains("pool not found")) {
                logger.debug(String.format("Removing pool [%s] from HA monitor since it was deleted.", uuid));
                removedPools.add(uuid);
            }
        }
    }

    @Override
    public void run() {
        while (true) {

            runHeartBeat();

            try {
                Thread.sleep(_heartBeatUpdateFreq);
            } catch (InterruptedException e) {
                logger.debug("[IGNORED] Interrupted between heartbeats.", e);
            }
        }
    }

}
