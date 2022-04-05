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
import com.cloud.utils.script.Script;
import org.apache.log4j.Logger;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;
import org.libvirt.StoragePool;
import org.libvirt.StoragePoolInfo.StoragePoolState;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class KVMHAMonitor extends KVMHABase implements Runnable {

    private static final Logger s_logger = Logger.getLogger(KVMHAMonitor.class);
    private final Map<String, NfsStoragePool> nfsstoragePool = new ConcurrentHashMap<>();
    private final Map<String, RbdStoragePool> rbdstoragePool = new ConcurrentHashMap<>();
    private final NfsStoragePool nfsStoragePool = null;
    private final RbdStoragePool rbdStoragePool = null;
    private final boolean rebootHostAndAlertManagementOnHeartbeatTimeout;

    private final String hostPrivateIp;

    public KVMHAMonitor(NfsStoragePool pool, RbdStoragePool rbdpool, String host, String scriptPath, String scriptPathRbd) {
        if (pool != null) {
            nfsstoragePool.put(pool._poolUUID, pool);
        }else if (rbdpool != null) {
            rbdstoragePool.put(rbdpool._poolUUID, rbdpool);
        }
        configureHeartBeatPath(scriptPath, scriptPathRbd);
        hostPrivateIp = host;
        _heartBeatUpdateTimeout = AgentPropertiesFileHandler.getPropertyValue(AgentProperties.HEARTBEAT_UPDATE_TIMEOUT);
        rebootHostAndAlertManagementOnHeartbeatTimeout = AgentPropertiesFileHandler.getPropertyValue(AgentProperties.REBOOT_HOST_AND_ALERT_MANAGEMENT_ON_HEARTBEAT_TIMEOUT);
    }

    private static synchronized void configureHeartBeatPath(String scriptPath, String scriptPathRbd) {
        KVMHABase.s_heartBeatPath = scriptPath;
        KVMHABase.s_heartBeatPathRbd = scriptPathRbd;
    }

    public void addStoragePool(NfsStoragePool pool) {
        nfsstoragePool.put(pool._poolUUID, pool);
    }

    public void addStoragePool(RbdStoragePool pool) {
        rbdstoragePool.put(pool._poolUUID, pool);
    }

    public void removeStoragePool(String uuid) {
        NfsStoragePool pool = nfsstoragePool.get(uuid);
        if (pool != null) {
            Script.runSimpleBashScript("umount " + pool._mountDestPath);
            nfsstoragePool.remove(uuid);
        }
    }

    public void removeRbdStoragePool(String uuid) {
        RbdStoragePool pool = rbdstoragePool.get(uuid);
        if (pool != null) {
            Script.runSimpleBashScript("umount " + pool._mountDestPath);
            rbdstoragePool.remove(uuid);
        }
    }

    public List<NfsStoragePool> getStoragePools() {
        return new ArrayList<>(nfsstoragePool.values());
    }

    public List<RbdStoragePool> getRbdStoragePools() {
        return new ArrayList<>(rbdstoragePool.values());
    }

    public NfsStoragePool getStoragePool(String uuid) {
        return nfsstoragePool.get(uuid);
    }

    public RbdStoragePool getRbdStoragePool(String uuid) {
        return rbdstoragePool.get(uuid);
    }

    protected void runHeartBeat() {
        Set<String> removedPools = new HashSet<>();
        if (nfsstoragePool != null && !nfsstoragePool.isEmpty()) {
            for (String uuid : nfsstoragePool.keySet()) {
                NfsStoragePool nfsStoragePool = nfsstoragePool.get(uuid);
                runHeartbeatToPool(nfsStoragePool, rbdStoragePool, uuid, removedPools);
            }
        }

        if (rbdstoragePool != null && !rbdstoragePool.isEmpty()) {
            for (String uuid : rbdstoragePool.keySet()) {
                RbdStoragePool rbdStoragePool = rbdstoragePool.get(uuid);
                runHeartbeatToPool(nfsStoragePool, rbdStoragePool, uuid, removedPools);
            }
        }

        if (!removedPools.isEmpty()) {
            for (String uuid : removedPools) {
                removeStoragePool(uuid);
            }
        }
    }

    private void runHeartbeatToPool(NfsStoragePool nfsStoragePool, RbdStoragePool rbdStoragePool, String uuid, Set<String> removedPools) {
        StoragePool storage;
        try {
            Connect conn = LibvirtConnection.getConnection();
            storage = conn.storagePoolLookupByUUIDString(uuid);
            if (storage == null || storage.getInfo().state != StoragePoolState.VIR_STORAGE_POOL_RUNNING) {
                if (storage == null) {
                    s_logger.debug(String.format("Libvirt storage pool [%s] not found, removing from HA list.", uuid));
                } else {
                    s_logger.debug(String.format("Libvirt storage pool [%s] was found, but it is not running, removing it from the HA list.", uuid));
                }

                removedPools.add(uuid);
            }

            s_logger.debug(String.format("Found NFS storage pool [%s] in libvirt, continuing.", uuid));

        } catch (LibvirtException e) {
            s_logger.debug(String.format("Failed to lookup libvirt storage pool [%s].", uuid), e);

            if (e.toString().contains("pool not found")) {
                s_logger.debug(String.format("Removing pool [%s] from HA monitor since it was deleted.", uuid));
                removedPools.add(uuid);
            }

        }

        String result = null;
        Process process = null;
        for (int i = 1; i <= _heartBeatUpdateMaxTries; i++) {
            if (nfsStoragePool != null) {
                Script cmd = createHeartBeatCommand(nfsStoragePool, hostPrivateIp, true);
                result = cmd.execute();
                s_logger.debug(String.format("The command [%s], to the pool [%s], had the result [%s].", cmd.toString(), uuid, result));
            } else if (rbdStoragePool != null) {
                ProcessBuilder processBuilder = createRbdHeartBeatCommand(rbdStoragePool, hostPrivateIp, true);
                try {
                    process = processBuilder.start();
                    BufferedReader bfr = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    result = bfr.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                s_logger.debug(String.format("The command [%s], to the pool [%s], had the result [%s].", processBuilder.command().toString(), uuid, result));
            }
            if (result != null) {
                s_logger.warn(String.format("Write heartbeat for pool [%s] failed: %s; try: %s of %s.", uuid, result, i, _heartBeatUpdateMaxTries));
                try {
                    Thread.sleep(_heartBeatUpdateRetrySleep);
                } catch (InterruptedException e) {
                    s_logger.debug("[IGNORED] Interrupted between heartbeat retries.", e);
                }
            } else {
                break;
            }

        }

        if (result != null && rebootHostAndAlertManagementOnHeartbeatTimeout) {
            s_logger.warn(String.format("Write heartbeat for pool [%s] failed: %s; stopping cloudstack-agent.", uuid, result));
            if (nfsStoragePool != null) {
                Script cmd = createHeartBeatCommand(nfsStoragePool, null, false);
                result = cmd.execute();
            } else if (rbdStoragePool != null) {
                ProcessBuilder processBuilder = createRbdHeartBeatCommand(rbdStoragePool, null, false);
                try {
                    process = processBuilder.start();
                    BufferedReader bfr = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    result = bfr.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Script createHeartBeatCommand(NfsStoragePool primaryStoragePool, String hostPrivateIp, boolean hostValidation) {
        Script cmd = new Script(s_heartBeatPath, _heartBeatUpdateTimeout, s_logger);
        cmd.add("-i", primaryStoragePool._poolIp);
        cmd.add("-p", primaryStoragePool._poolMountSourcePath);
        cmd.add("-m", primaryStoragePool._mountDestPath);

        if (hostValidation) {
            cmd.add("-h", hostPrivateIp);
        }

        if (!hostValidation) {
            cmd.add("-c");
        }

        return cmd;
    }

    private ProcessBuilder createRbdHeartBeatCommand(RbdStoragePool primaryStoragePool, String hostPrivateIp, boolean hostValidation) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command().add("python3");
        processBuilder.command().add(s_heartBeatPathRbd);
        processBuilder.command().add("-i");
        processBuilder.command().add(primaryStoragePool._poolSourceHost);
        processBuilder.command().add("-p");
        processBuilder.command().add(primaryStoragePool._poolMountSourcePath);
        processBuilder.command().add("-n");
        processBuilder.command().add(primaryStoragePool._poolAuthUserName);
        processBuilder.command().add("-s");
        processBuilder.command().add(primaryStoragePool._poolAuthSecret);

        if (hostValidation) {
            processBuilder.command().add("-v");
            processBuilder.command().add(hostPrivateIp);
        }

        return processBuilder;
    }

    @Override
    public void run() {
        while (true) {

            runHeartBeat();

            try {
                Thread.sleep(_heartBeatUpdateFreq);
            } catch (InterruptedException e) {
                s_logger.debug("[IGNORED] Interrupted between heartbeats.", e);
            }
        }
    }

}
