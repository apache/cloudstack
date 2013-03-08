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

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.libvirt.LibvirtException;
import org.libvirt.StoragePool;
import org.libvirt.StoragePoolInfo;
import org.libvirt.StoragePoolInfo.StoragePoolState;

import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.OutputInterpreter.AllLinesParser;
import com.cloud.utils.script.Script;

public class KVMHABase {
    private static final Logger s_logger = Logger.getLogger(KVMHABase.class);
    private long _timeout = 60000; /* 1 minutes */
    protected static String _heartBeatPath;
    protected long _heartBeatUpdateTimeout = 60000;
    protected long _heartBeatUpdateFreq = 60000;
    protected long _heartBeatUpdateMaxRetry = 3;

    public static enum PoolType {
        PrimaryStorage, SecondaryStorage
    }

    public static class NfsStoragePool {
        String _poolUUID;
        String _poolIp;
        String _poolMountSourcePath;
        String _mountDestPath;
        PoolType _type;

        public NfsStoragePool(String poolUUID, String poolIp,
                String poolSourcePath, String mountDestPath, PoolType type) {
            this._poolUUID = poolUUID;
            this._poolIp = poolIp;
            this._poolMountSourcePath = poolSourcePath;
            this._mountDestPath = mountDestPath;
            this._type = type;
        }
    }

    protected String checkingMountPoint(NfsStoragePool pool, String poolName) {
        String mountSource = pool._poolIp + ":" + pool._poolMountSourcePath;
        String mountPaths = Script
                .runSimpleBashScript("cat /proc/mounts | grep " + mountSource);
        String destPath = pool._mountDestPath;

        if (mountPaths != null) {
            String token[] = mountPaths.split(" ");
            String mountType = token[2];
            String mountDestPath = token[1];
            if (mountType.equalsIgnoreCase("nfs")) {
                if (poolName != null && !mountDestPath.startsWith(destPath)) {
                    /* we need to mount it under poolName */
                    Script mount = new Script("/bin/bash", 60000);
                    mount.add("-c");
                    mount.add("mount " + mountSource + " " + destPath);
                    String result = mount.execute();
                    if (result != null) {
                        destPath = null;
                    }
                    destroyVMs(destPath);
                } else if (poolName == null) {
                    destPath = mountDestPath;
                }
            }
        } else {
            /* Can't find the mount point? */
            /* we need to mount it under poolName */
            if (poolName != null) {
                Script mount = new Script("/bin/bash", 60000);
                mount.add("-c");
                mount.add("mount " + mountSource + " " + destPath);
                String result = mount.execute();
                if (result != null) {
                    destPath = null;
                }

                destroyVMs(destPath);
            }
        }

        return destPath;
    }

    protected String getMountPoint(NfsStoragePool storagePool) {

        StoragePool pool = null;
        String poolName = null;
        try {
            pool = LibvirtConnection.getConnection()
                    .storagePoolLookupByUUIDString(storagePool._poolUUID);
            if (pool != null) {
                StoragePoolInfo spi = pool.getInfo();
                if (spi.state != StoragePoolState.VIR_STORAGE_POOL_RUNNING) {
                    pool.create(0);
                } else {
                    /*
                     * Sometimes, the mount point is lost, even libvirt thinks
                     * the storage pool still running
                     */
                }
            }
            poolName = pool.getName();
        } catch (LibvirtException e) {
            s_logger.debug("Ignoring libvirt error.", e);
        } finally {
            try {
                if (pool != null) {
                    pool.free();
                }
            } catch (LibvirtException e) {
                s_logger.debug("Ignoring libvirt error.", e);
            }
        }

        return checkingMountPoint(storagePool, poolName);
    }

    protected void destroyVMs(String mountPath) {
        /* if there are VMs using disks under this mount path, destroy them */
        Script cmd = new Script("/bin/bash", _timeout);
        cmd.add("-c");
        cmd.add("ps axu|grep qemu|grep " + mountPath + "* |awk '{print $2}'");
        AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        String result = cmd.execute(parser);

        if (result != null) {
            return;
        }

        String pids[] = parser.getLines().split("\n");
        for (String pid : pids) {
            Script.runSimpleBashScript("kill -9 " + pid);
        }
    }

    protected String getHBFile(String mountPoint, String hostIP) {
        return mountPoint + File.separator + "KVMHA" + File.separator + "hb-"
                + hostIP;
    }

    protected String getHBFolder(String mountPoint) {
        return mountPoint + File.separator + "KVMHA" + File.separator;
    }

    protected String runScriptRetry(String cmdString,
            OutputInterpreter interpreter) {
        String result = null;
        for (int i = 0; i < 3; i++) {
            Script cmd = new Script("/bin/bash", _timeout);
            cmd.add("-c");
            cmd.add(cmdString);
            if (interpreter != null)
                result = cmd.execute(interpreter);
            else {
                result = cmd.execute();
            }
            if (result == Script.ERR_TIMEOUT) {
                continue;
            } else if (result == null) {
                break;
            }
        }

        return result;
    }
}
