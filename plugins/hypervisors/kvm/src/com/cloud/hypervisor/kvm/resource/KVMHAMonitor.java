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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.log4j.Logger;

import com.cloud.utils.script.Script;

import org.libvirt.Connect;
import org.libvirt.LibvirtException;
import org.libvirt.Secret;
import org.libvirt.StoragePool;
import org.libvirt.StoragePoolInfo;
import org.libvirt.StoragePoolInfo.StoragePoolState;

import com.cloud.hypervisor.kvm.resource.LibvirtConnection;

public class KVMHAMonitor extends KVMHABase implements Runnable {
    private static final Logger s_logger = Logger.getLogger(KVMHAMonitor.class);
    private Map<String, NfsStoragePool> _storagePool = new ConcurrentHashMap<String, NfsStoragePool>();

    private String _hostIP; /* private ip address */

    public KVMHAMonitor(NfsStoragePool pool, String host, String scriptPath) {
        if (pool != null) {
            this._storagePool.put(pool._poolUUID, pool);
        }
        this._hostIP = host;
        this._heartBeatPath = scriptPath;
    }

    public void addStoragePool(NfsStoragePool pool) {
        synchronized (_storagePool) {
            this._storagePool.put(pool._poolUUID, pool);
        }
    }

    public void removeStoragePool(String uuid) {
        synchronized (_storagePool) {
            NfsStoragePool pool = this._storagePool.get(uuid);
            if (pool != null) {
                Script.runSimpleBashScript("umount " + pool._mountDestPath);
                this._storagePool.remove(uuid);
            }
        }
    }

    public List<NfsStoragePool> getStoragePools() {
        synchronized (_storagePool) {
            return new ArrayList<NfsStoragePool>(_storagePool.values());
        }
    }

    private class Monitor extends ManagedContextRunnable {

        @Override
        protected void runInContext() {
            synchronized (_storagePool) {
                for (String uuid : _storagePool.keySet()) {
                    NfsStoragePool primaryStoragePool = _storagePool.get(uuid);

                    // check for any that have been deregistered with libvirt and
                    // skip,remove them

                    StoragePool storage = null;
                    try {
                        Connect conn = LibvirtConnection.getConnection();
                        storage = conn.storagePoolLookupByUUIDString(uuid);
                        if (storage == null) {
                            s_logger.debug("Libvirt storage pool " + uuid
                                           +" not found, removing from HA list");
                            removeStoragePool(uuid);
                            continue;

                        } else if (storage.getInfo().state != StoragePoolState.VIR_STORAGE_POOL_RUNNING) {
                            s_logger.debug("Libvirt storage pool " + uuid
                                           +" found, but not running, removing from HA list");
                            
                            removeStoragePool(uuid);
                            continue;
                        }
                        s_logger.debug("Found NFS storage pool " + uuid + " in libvirt, continuing");

                    } catch (LibvirtException e) {
                        s_logger.debug("Failed to lookup libvirt storage pool " + uuid
                                       + " due to: " + e );

                        // we only want to remove pool if it's not found, not if libvirt
                        // connection fails
                        if (e.toString().contains("pool not found")) {
                            s_logger.debug("removing pool from HA monitor since it was deleted");
                            removeStoragePool(uuid);
                            continue;
                        }
                    }

                    String result = null;
                    for (int i = 0; i < 5; i++) {
                        Script cmd = new Script(_heartBeatPath,
                                _heartBeatUpdateTimeout, s_logger);
                        cmd.add("-i", primaryStoragePool._poolIp);
                        cmd.add("-p", primaryStoragePool._poolMountSourcePath);
                        cmd.add("-m", primaryStoragePool._mountDestPath);
                        cmd.add("-h", _hostIP);
                        result = cmd.execute();
                        if (result != null) {
                            s_logger.warn("write heartbeat failed: " + result
                                    + ", retry: " + i);
                        } else {
                            break;
                        }
                    }

                    if (result != null) {
                        s_logger.warn("write heartbeat failed: " + result
                                + "; reboot the host");
                        Script cmd = new Script(_heartBeatPath,
                                _heartBeatUpdateTimeout, s_logger);
                        cmd.add("-i", primaryStoragePool._poolIp);
                        cmd.add("-p", primaryStoragePool._poolMountSourcePath);
                        cmd.add("-m", primaryStoragePool._mountDestPath);
                        cmd.add("-c");
                        result = cmd.execute();
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

            }

            try {
                Thread.sleep(_heartBeatUpdateFreq);
            } catch (InterruptedException e) {

            }
        }
    }

}
