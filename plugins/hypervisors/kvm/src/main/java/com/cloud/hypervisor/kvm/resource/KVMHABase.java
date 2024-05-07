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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.libvirt.LibvirtException;
import org.libvirt.StoragePool;
import org.libvirt.StoragePoolInfo;
import org.libvirt.StoragePoolInfo.StoragePoolState;

import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.OutputInterpreter.AllLinesParser;
import com.cloud.utils.script.Script;
import com.cloud.agent.properties.AgentProperties;
import com.cloud.agent.properties.AgentPropertiesFileHandler;

public class KVMHABase {
    protected Logger logger = LogManager.getLogger(getClass());
    private long _timeout = 60000; /* 1 minutes */
    protected static String s_heartBeatPath;
    protected long _heartBeatUpdateTimeout = AgentPropertiesFileHandler.getPropertyValue(AgentProperties.HEARTBEAT_UPDATE_TIMEOUT);
    protected long _heartBeatUpdateFreq = AgentPropertiesFileHandler.getPropertyValue(AgentProperties.KVM_HEARTBEAT_UPDATE_FREQUENCY);
    protected long _heartBeatUpdateMaxTries = AgentPropertiesFileHandler.getPropertyValue(AgentProperties.KVM_HEARTBEAT_UPDATE_MAX_TRIES);
    protected long _heartBeatUpdateRetrySleep = AgentPropertiesFileHandler.getPropertyValue(AgentProperties.KVM_HEARTBEAT_UPDATE_RETRY_SLEEP);

    public static enum PoolType {
        PrimaryStorage, SecondaryStorage
    }

    public static class HAStoragePool {
        String poolUuid;
        String poolIp;
        String poolMountSourcePath;
        String mountDestPath;
        PoolType poolType;
        KVMStoragePool pool;

        public HAStoragePool(KVMStoragePool pool, String host, String path, PoolType type) {
            this.pool = pool;
            this.poolUuid = pool.getUuid();
            this.mountDestPath = pool.getLocalPath();
            this.poolIp = host;
            this.poolMountSourcePath = path;
            this.poolType = type;
        }

        public String getPoolUUID() {
            return poolUuid;
        }

        public void setPoolUUID(String poolUuid) {
            this.poolUuid = poolUuid;
        }

        public String getPoolIp() {
            return poolIp;
        }

        public void setPoolIp(String poolIp) {
            this.poolIp = poolIp;
        }

        public String getPoolMountSourcePath() {
            return poolMountSourcePath;
        }

        public void setPoolMountSourcePath(String poolMountSourcePath) {
            this.poolMountSourcePath = poolMountSourcePath;
        }

        public String getMountDestPath() {
            return mountDestPath;
        }

        public void setMountDestPath(String mountDestPath) {
            this.mountDestPath = mountDestPath;
        }

        public PoolType getType() {
            return poolType;
        }

        public void setType(PoolType type) {
            this.poolType = type;
        }

        public KVMStoragePool getPool() {
            return pool;
        }

        public void setPool(KVMStoragePool pool) {
            this.pool = pool;
        }
    }

    protected String checkingMountPoint(HAStoragePool pool, String poolName) {
        String mountSource = pool.getPoolIp() + ":" + pool.getPoolMountSourcePath();
        String mountPaths = Script.runSimpleBashScript("cat /proc/mounts | grep " + mountSource);
        String destPath = pool.getMountDestPath();

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

    protected String getMountPoint(HAStoragePool storagePool) {

        StoragePool pool = null;
        String poolName = null;
        try {
            pool = LibvirtConnection.getConnection().storagePoolLookupByUUIDString(storagePool.getPoolUUID());
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
                poolName = pool.getName();
            }

        } catch (LibvirtException e) {
            logger.debug("Ignoring libvirt error.", e);
        } finally {
            try {
                if (pool != null) {
                    pool.free();
                }
            } catch (LibvirtException e) {
                logger.debug("Ignoring libvirt error.", e);
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
        return mountPoint + File.separator + "KVMHA" + File.separator + "hb-" + hostIP;
    }

    protected String getHBFolder(String mountPoint) {
        return mountPoint + File.separator + "KVMHA" + File.separator;
    }

    protected String runScriptRetry(String cmdString, OutputInterpreter interpreter) {
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

    public Boolean checkingHeartBeat() {
        // TODO Auto-generated method stub
        return null;
    }
}
