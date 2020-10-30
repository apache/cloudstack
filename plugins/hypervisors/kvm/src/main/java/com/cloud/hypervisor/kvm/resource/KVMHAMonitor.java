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

import com.cloud.utils.script.Script;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KVMHAMonitor extends KVMHABase implements Runnable {
    private static final Logger s_logger = Logger.getLogger(KVMHAMonitor.class);
    private final Map<String, NfsStoragePool> _storagePool = new ConcurrentHashMap<String, NfsStoragePool>();
    private final Map<String, CheckPoolThread> _storagePoolCheckThreads = new HashMap<String, CheckPoolThread>();
    private final Map<String, String> _storagePoolCheckStatus = new HashMap<String, String>();
    private final static String STATUS_RUNNING = "Running";
    private final static String STATUS_TERMINATED = "Terminated";

    private final String _hostIP; /* private ip address */

    public KVMHAMonitor(NfsStoragePool pool, String host, String scriptPath) {
        if (pool != null) {
            _storagePool.put(pool._poolUUID, pool);
        }
        _hostIP = host;
        configureHeartBeatPath(scriptPath);
    }

    private static synchronized void configureHeartBeatPath(String scriptPath) {
        KVMHABase.s_heartBeatPath = scriptPath;
    }

    public static synchronized void configureHeartBeatParams(Long heartBeatUpdateMaxTries, Long heartBeatUpdateRetrySleep, Long heartBeatUpdateTimeout, HeartBeatAction heartBeatFailureAction) {
        s_logger.debug(String.format("Configuring heartbeat params: max retries = %s, retry sleep = %s, timeout = %s, action = %s", heartBeatUpdateMaxTries, heartBeatUpdateRetrySleep, heartBeatUpdateTimeout, heartBeatFailureAction));
        if (heartBeatUpdateMaxTries != null) {
            KVMHABase.s_heartBeatUpdateMaxRetries = heartBeatUpdateMaxTries;
        }
        if (heartBeatUpdateRetrySleep != null) {
            KVMHABase.s_heartBeatUpdateRetrySleep = heartBeatUpdateRetrySleep;
        }
        if (heartBeatUpdateTimeout != null) {
            KVMHABase.s_heartBeatUpdateTimeout = heartBeatUpdateTimeout;
        }
        if (heartBeatFailureAction != null) {
            KVMHABase.s_heartBeatFailureAction = heartBeatFailureAction;
        }
    }

    public void addStoragePool(NfsStoragePool pool) {
        synchronized (_storagePool) {
            _storagePool.put(pool._poolUUID, pool);
        }
    }

    public void removeStoragePool(String uuid) {
        synchronized (_storagePool) {
            NfsStoragePool pool = _storagePool.get(uuid);
            if (pool != null) {
                Script.runSimpleBashScript("umount " + pool._mountDestPath);
                _storagePool.remove(uuid);
            }
        }
    }

    public List<NfsStoragePool> getStoragePools() {
        synchronized (_storagePool) {
            return new ArrayList<NfsStoragePool>(_storagePool.values());
        }
    }

    public NfsStoragePool getStoragePool(String uuid) {
        synchronized (_storagePool) {
            return _storagePool.get(uuid);
        }
    }

    protected class CheckPoolThread extends Thread {
        private NfsStoragePool primaryStoragePool;

        public CheckPoolThread(NfsStoragePool pool) {
            this.primaryStoragePool = pool;
        }

        @Override
        public void run() {
            while (true) {
                if (! check()) {
                    break;
                }
                try {
                    Thread.sleep(s_heartBeatUpdateFreq);
                } catch (InterruptedException e) {
                    s_logger.debug("[ignored] interupted between heartbeat checks.");
                }
            }
        }

        private boolean check() {
            if (! _storagePool.containsKey(primaryStoragePool._poolUUID)) {
                s_logger.info("Removing check on storage pool as it has been removed: " + primaryStoragePool._poolUUID);
                _storagePoolCheckStatus.remove(primaryStoragePool._poolUUID);
                _storagePoolCheckThreads.remove(primaryStoragePool._poolUUID);
                Thread.currentThread().interrupt();
                return false;
            }

            if (_storagePoolCheckStatus.containsKey(primaryStoragePool._poolUUID)) {
                s_logger.info("Ignoring check on storage pool: " + primaryStoragePool._poolUUID);
                return true;
            }

            s_logger.debug("Starting check on storage pool: " + primaryStoragePool._poolUUID);

            String result = null;
            // Try multiple times, but sleep in between tries to ensure it isn't a short lived transient error
            for (int i = 1; i <= s_heartBeatUpdateMaxRetries; i++) {
                s_logger.info(String.format("Trying to write heartbeat to pool %s %s of %s times", primaryStoragePool._mountDestPath, i, s_heartBeatUpdateMaxRetries));
                Script cmd = new Script(s_heartBeatPath, s_heartBeatUpdateTimeout, s_logger);
                cmd.add("-i", primaryStoragePool._poolIp);
                cmd.add("-p", primaryStoragePool._poolMountSourcePath);
                cmd.add("-m", primaryStoragePool._mountDestPath);
                cmd.add("-h", _hostIP);
                result = cmd.execute();
                if (result != null) {
                    s_logger.warn("write heartbeat failed: " + result + ", tried: " + i + " of " + s_heartBeatUpdateMaxRetries);
                    _storagePoolCheckStatus.put(primaryStoragePool._poolUUID, STATUS_RUNNING);
                    if (i < s_heartBeatUpdateMaxRetries) {
                        while(true) {
                            try {
                                Thread.currentThread().sleep(s_heartBeatUpdateRetrySleep);
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

                s_logger.warn("Performing action " + s_heartBeatFailureAction + " on storage pool: " + primaryStoragePool._poolUUID);

                String destroyvmsCmd = "ps aux | grep '" + LibvirtVMDef.MANUFACTURER_APACHE + "' | grep -v ' grep '";
                if (HeartBeatAction.DESTROYVMS.equals(s_heartBeatFailureAction)) {
                    destroyvmsCmd += " | grep " + primaryStoragePool._mountDestPath;
                }
                destroyvmsCmd += " | awk '{print $14}' | tr '\n' ','";
                if (HeartBeatAction.DESTROYVMS.equals(s_heartBeatFailureAction)
                        || HeartBeatAction.HARDRESET.equals(s_heartBeatFailureAction)) {
                    String destroyvms = Script.runSimpleBashScript(destroyvmsCmd);
                    if (destroyvms != null) {
                        s_logger.warn("The following vms will be destroyed: " + destroyvms);
                    } else {
                        s_logger.info("No vms will be destroyed");
                    }
                }

                Script cmd = new Script(s_heartBeatPath, s_logger);
                cmd.add("-i", primaryStoragePool._poolIp);
                cmd.add("-p", primaryStoragePool._poolMountSourcePath);
                cmd.add("-m", primaryStoragePool._mountDestPath);
                cmd.add(s_heartBeatFailureAction.getFlag());
                result = cmd.execute();
                _storagePoolCheckStatus.put(primaryStoragePool._poolUUID, STATUS_TERMINATED);
                s_logger.debug("End performing action " + s_heartBeatFailureAction + " on storage pool: " + primaryStoragePool._poolUUID);
                return false;
            }

            s_logger.debug("End checking on storage pool " + primaryStoragePool._poolUUID);
            return true;
        }
    }

    private class Monitor extends ManagedContextRunnable {

        @Override
        protected void runInContext() {
            synchronized (_storagePool) {
                for (String uuid : _storagePool.keySet()) {
                    if (_storagePoolCheckThreads.containsKey(uuid)) {
                        s_logger.trace("Ignoring check on storage pool as there is already a thread: " + uuid);
                        continue;
                    }
                    NfsStoragePool primaryStoragePool = _storagePool.get(uuid);
                    s_logger.debug(String.format("Starting check thread for storage pool uuid = %s, ip = %s, source = %s, mount point = %s", uuid, primaryStoragePool._poolIp, primaryStoragePool._poolMountSourcePath, primaryStoragePool._mountDestPath));

                    CheckPoolThread checkPoolThread = new CheckPoolThread(primaryStoragePool);
                    _storagePoolCheckThreads.put(uuid, checkPoolThread);
                    checkPoolThread.start();
                }

                if (! _storagePoolCheckStatus.isEmpty()) {
                    boolean isAllTerminated = true;
                    for (Map.Entry<String, String> entry : _storagePoolCheckStatus.entrySet()) {
                        String status= entry.getValue();
                        s_logger.debug(String.format("State of check thread for pool %s is %s", entry.getKey(), status));
                        if (status != STATUS_TERMINATED) {
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

        }
    }

    @Override
    public void run() {
        // s_logger.addAppender(new org.apache.log4j.ConsoleAppender(new
        // org.apache.log4j.PatternLayout(), "System.out"));
        while (true) {
            Thread monitorThread = new Thread(new Monitor());
            monitorThread.start();
            try {
                monitorThread.join();
            } catch (InterruptedException e) {
                s_logger.debug("[ignored] interupted joining monitor.");
            }

            try {
                Thread.sleep(s_heartBeatUpdateFreq);
            } catch (InterruptedException e) {
                s_logger.debug("[ignored] interupted between heartbeats.");
            }
        }
    }

}
