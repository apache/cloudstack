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
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.log4j.Logger;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;
import org.libvirt.StoragePool;
import org.libvirt.StoragePoolInfo.StoragePoolState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class KVMHAMonitor extends KVMHABase implements Runnable {

    private static final Logger s_logger = Logger.getLogger(KVMHAMonitor.class);
    private final Map<String, NfsStoragePool> storagePool = new ConcurrentHashMap<>();
    private Set<String> removedPools = new HashSet<>();
    private final boolean rebootHostAndAlertManagementOnHeartbeatTimeout;
    private final Map<String, CheckPoolThread> _storagePoolCheckThreads = new HashMap<String, CheckPoolThread>();
    private final Map<String, String> _storagePoolCheckStatus = new HashMap<String, String>();
    private final static String STATUS_RUNNING = "Running";
    private final static String STATUS_TERMINATED = "Terminated";

    private final String hostPrivateIp;

    public KVMHAMonitor(NfsStoragePool pool, String host, String scriptPath) {
        if (pool != null) {
            storagePool.put(pool._poolUUID, pool);
        }
        hostPrivateIp = host;
        configureHeartBeatPath(scriptPath);

        _heartBeatUpdateTimeout = AgentPropertiesFileHandler.getPropertyValue(AgentProperties.HEARTBEAT_UPDATE_TIMEOUT);
        rebootHostAndAlertManagementOnHeartbeatTimeout = AgentPropertiesFileHandler.getPropertyValue(AgentProperties.REBOOT_HOST_AND_ALERT_MANAGEMENT_ON_HEARTBEAT_TIMEOUT);
    }

    private static synchronized void configureHeartBeatPath(String scriptPath) {
        KVMHABase.s_heartBeatPath = scriptPath;
    }

    public static synchronized void configureHeartBeatParams(Long heartBeatUpdateMaxTries,
                                                             Long heartBeatUpdateRetrySleep,
                                                             Long heartBeatUpdateTimeout,
                                                             HeartBeatAction heartBeatFailureAction) {
        s_logger.debug(String.format("Configuring heartbeat params: max retries = %s, retry sleep = %s, timeout = %s, action = %s",
                heartBeatUpdateMaxTries, heartBeatUpdateRetrySleep, heartBeatUpdateTimeout, heartBeatFailureAction));
        if (heartBeatUpdateMaxTries != null) {
            KVMHABase._heartBeatUpdateMaxRetries = heartBeatUpdateMaxTries;
        }
        if (heartBeatUpdateRetrySleep != null) {
            KVMHABase._heartBeatUpdateRetrySleep = heartBeatUpdateRetrySleep;
        }
        if (heartBeatUpdateTimeout != null) {
            KVMHABase._heartBeatUpdateTimeout = heartBeatUpdateTimeout;
        }
        if (heartBeatFailureAction != null) {
            KVMHABase._heartBeatFailureAction = heartBeatFailureAction;
        }
    }

    public void addStoragePool(NfsStoragePool pool) {
        synchronized (storagePool) {
            storagePool.put(pool._poolUUID, pool);
        }
    }

    public void removeStoragePool(String uuid) {
        synchronized (storagePool) {
            NfsStoragePool pool = storagePool.get(uuid);
            if (pool != null) {
                Script.runSimpleBashScript("umount " + pool._mountDestPath);
                storagePool.remove(uuid);
            }
        }
    }

    public List<NfsStoragePool> getStoragePools() {
        synchronized (storagePool) {
            return new ArrayList<>(storagePool.values());
        }
    }

    public NfsStoragePool getStoragePool(String uuid) {
        synchronized (storagePool) {
            return storagePool.get(uuid);
        }
    }

    protected class CheckPoolThread extends ManagedContextRunnable {
        private final NfsStoragePool primaryStoragePool;

        public CheckPoolThread(NfsStoragePool pool) {
            this.primaryStoragePool = pool;
        }

        @Override
        public void runInContext() {
            check();
        }

        private void check() {
            if (! storagePool.containsKey(primaryStoragePool._poolUUID)) {
                s_logger.info("Removing check on storage pool as it has been removed: " + primaryStoragePool._poolUUID);
                _storagePoolCheckStatus.remove(primaryStoragePool._poolUUID);
                _storagePoolCheckThreads.remove(primaryStoragePool._poolUUID);
                Thread.currentThread().interrupt();
                return;
            }

            if (_storagePoolCheckStatus.containsKey(primaryStoragePool._poolUUID)) {
                s_logger.info("Ignoring check on storage pool: " + primaryStoragePool._poolUUID);
                return;
            }

            s_logger.debug("Starting check on storage pool: " + primaryStoragePool._poolUUID);

            String result = null;
            // Try multiple times, but sleep in between tries to ensure it isn't a short lived transient error
            for (int i = 1; i <= _heartBeatUpdateMaxRetries; i++) {
                s_logger.info(String.format("Trying to write heartbeat to pool %s %s of %s times", primaryStoragePool._mountDestPath, i, _heartBeatUpdateMaxRetries));
                Script cmd = createHeartBeatCommand(primaryStoragePool, hostPrivateIp, true);
                result = cmd.execute();
                s_logger.debug(String.format("The command (%s), to the pool [%s], has the result [%s].", cmd.toString(), primaryStoragePool._poolUUID, result));
                if (result != null) {
                    s_logger.warn(String.format("Write heartbeat for pool [%s] failed: %s; try: %s of %s.", primaryStoragePool._poolUUID, result, i, _heartBeatUpdateMaxRetries));
                    _storagePoolCheckStatus.put(primaryStoragePool._poolUUID, STATUS_RUNNING);
                    if (i < _heartBeatUpdateMaxRetries) {
                        while(true) {
                            try {
                                Thread.currentThread().sleep(_heartBeatUpdateRetrySleep);
                                break;
                            } catch (InterruptedException e) {
                                s_logger.debug("[ignored] interupted between heartbeat retries with error message: " + e.getMessage());
                            }
                        }
                    }
                } else {
                    _storagePoolCheckStatus.remove(primaryStoragePool._poolUUID);
                    break;
                }
            }

            if (result != null) {
                // Perform action if can't write to heartbeat file.
                // This will raise an alert on the mgmt server
                s_logger.warn("write heartbeat failed: " + result);
                if (HeartBeatAction.NOACTION.equals(s_heartBeatFailureAction)) {
                    s_logger.warn("No action will be performed on storage pool: " + primaryStoragePool._poolUUID);
                    _storagePoolCheckStatus.remove(primaryStoragePool._poolUUID);
                    return true;
                }

                performAction(primaryStoragePool);
                _storagePoolCheckStatus.put(primaryStoragePool._poolUUID, STATUS_TERMINATED);
                s_logger.debug("End performing action " + _heartBeatFailureAction + " on storage pool: " + primaryStoragePool._poolUUID);
                return;
            }

            s_logger.debug("End checking on storage pool " + primaryStoragePool._poolUUID);
        }

        private void performAction(NfsStoragePool primaryStoragePool) {
            s_logger.warn("Performing action " + _heartBeatFailureAction + " on storage pool: " + primaryStoragePool._poolUUID);

            Script cmd = createHeartBeatCommand(primaryStoragePool, null, false);
            // give priority to action defined in agent.properties file
            if (rebootHostAndAlertManagementOnHeartbeatTimeout) {
                s_logger.warn(String.format("Write heartbeat for pool [%s] failed; stopping cloudstack-agent.", primaryStoragePool._poolUUID));
                cmd.execute();
                return;
            }

            if (HeartBeatAction.DESTROYVMS.equals(_heartBeatFailureAction)
                    || HeartBeatAction.HARDRESET.equals(_heartBeatFailureAction)) {
                String destroyvmsCmd = "ps aux | grep '" + LibvirtVMDef.MANUFACTURER_APACHE + "' | grep -v ' grep '";
                if (HeartBeatAction.DESTROYVMS.equals(_heartBeatFailureAction)) {
                    destroyvmsCmd += " | grep " + primaryStoragePool._mountDestPath;
                }
                destroyvmsCmd += " | awk '{print $14}' | tr '\n' ','";
                String destroyvms = Script.runSimpleBashScript(destroyvmsCmd);
                if (destroyvms != null) {
                    s_logger.warn("The following vms will be destroyed: " + destroyvms);
                } else {
                    s_logger.info("No vms will be destroyed");
                }
            }

            // take action according to global setting
            cmd.add(_heartBeatFailureAction.getFlag());
            cmd.execute();
        }

        private Script createHeartBeatCommand(NfsStoragePool primaryStoragePool, String hostPrivateIp, boolean hostValidation) {
            Script cmd = new Script(s_heartBeatPath, _heartBeatUpdateTimeout, s_logger);
            cmd.add("-i", primaryStoragePool._poolIp);
            cmd.add("-p", primaryStoragePool._poolMountSourcePath);
            cmd.add("-m", primaryStoragePool._mountDestPath);

            if (hostValidation) {
                cmd.add("-h", hostPrivateIp);
            } else {
                cmd.add("-c");
            }

            return cmd;
        }
    }

    private class Monitor extends ManagedContextRunnable {

        private boolean checkPoolValidity(String uuid) {
            StoragePool storage;
            boolean valid = true;
            try {
                Connect conn = LibvirtConnection.getConnection();
                storage = conn.storagePoolLookupByUUIDString(uuid);
                if (storage == null || storage.getInfo().state != StoragePoolState.VIR_STORAGE_POOL_RUNNING) {
                    if (storage == null) {
                        s_logger.debug(String.format("Libvirt storage pool [%s] not found, removing from HA list.", uuid));
                    } else {
                        s_logger.debug(String.format("Libvirt storage pool [%s] found, but not running, removing from HA list.", uuid));
                    }
                    valid = false;
                }

                s_logger.debug(String.format("Found NFS storage pool [%s] in libvirt, continuing.", uuid));
            } catch (LibvirtException e) {
                s_logger.debug(String.format("Failed to lookup libvirt storage pool [%s].", uuid), e);

                if (e.toString().contains("pool not found")) {
                    s_logger.debug(String.format("Removing pool [%s] from HA monitor since it was deleted.", uuid));
                    valid = false;
                }

            }

            return valid;
        }

        @Override
        protected void runInContext() {
            synchronized (storagePool) {
                for (String uuid : storagePool.keySet()) {
                    if (_storagePoolCheckThreads.containsKey(uuid)) {
                        s_logger.trace("Ignoring check on storage pool as there is already a thread: " + uuid);
                        continue;
                    }
                    NfsStoragePool primaryStoragePool = storagePool.get(uuid);
                    if (checkPoolValidity(uuid)) {
                        s_logger.debug(String.format("Starting check thread for storage pool uuid = %s, ip = %s, source = %s, mount point = %s",
                                uuid, primaryStoragePool._poolIp, primaryStoragePool._poolMountSourcePath, primaryStoragePool._mountDestPath));

                        CheckPoolThread checkPoolThread = new CheckPoolThread(primaryStoragePool);
                        _storagePoolCheckThreads.put(uuid, checkPoolThread);
                        checkPoolThread.runInContext();
                    } else {
                        removedPools.add(uuid);
                    }
                }

                if (! _storagePoolCheckStatus.isEmpty()) {
                    boolean isAllTerminated = true;
                    for (Map.Entry<String, String> entry : _storagePoolCheckStatus.entrySet()) {
                        String status= entry.getValue();
                        s_logger.debug(String.format("State of check thread for pool %s is %s", entry.getKey(), status));
                        if (!status.equals(STATUS_TERMINATED)) {
                            isAllTerminated = false;
                        }
                    }
                    if (isAllTerminated) {
                        s_logger.debug("All heartbeat check threads on pools with issues are terminated, stopping cloudstack-agent");
                        Script cmd = new Script("/bin/systemctl", s_logger);
                        cmd.add("stop");
                        cmd.add("cloudstack-agent");
                        cmd.execute();
                    }
                }
            }

            if (!removedPools.isEmpty()) {
                for (String uuid : removedPools) {
                    removeStoragePool(uuid);
                }
            }
            removedPools.clear();
        }
    }

    @Override
    public void run() {
        ScheduledExecutorService haMonitor = Executors.newSingleThreadScheduledExecutor();
        haMonitor.scheduleAtFixedRate(new Monitor(), 0, _heartBeatUpdateFreq, TimeUnit.SECONDS);
    }

}
