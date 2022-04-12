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
package com.cloud.hypervisor.kvm.storage;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat;
import org.apache.log4j.Logger;

import com.cloud.agent.api.to.DiskTO;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.ProvisioningType;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;

@StorageAdaptorInfo(storagePoolType=StoragePoolType.SharedMountPoint)
public class StorPoolStorageAdaptor implements StorageAdaptor {
    public static void SP_LOG(String fmt, Object... args) {
        try (PrintWriter spLogFile = new PrintWriter(new BufferedWriter(new FileWriter("/var/log/cloudstack/agent/storpool-agent.log", true)))) {
            final String line = String.format(fmt, args);
            String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,ms").format(Calendar.getInstance().getTime());
            spLogFile.println(timeStamp +" "+line);
            spLogFile.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final Logger log = Logger.getLogger(StorPoolStorageAdaptor.class);

    private static final Map<String, KVMStoragePool> storageUuidToStoragePool = new HashMap<String, KVMStoragePool>();

    @Override
    public KVMStoragePool createStoragePool(String uuid, String host, int port, String path, String userInfo, StoragePoolType storagePoolType) {
        SP_LOG("StorpooolStorageAdaptor.createStoragePool: uuid=%s, host=%s:%d, path=%s, userInfo=%s, type=%s", uuid, host, port, path, userInfo, storagePoolType);

        StorPoolStoragePool storagePool = new StorPoolStoragePool(uuid, host, port, storagePoolType, this);
        storageUuidToStoragePool.put(uuid, storagePool);
        return storagePool;
    }

    @Override
    public KVMStoragePool getStoragePool(String uuid) {
        SP_LOG("StorpooolStorageAdaptor.getStoragePool: uuid=%s", uuid);
        return storageUuidToStoragePool.get(uuid);
    }

    @Override
    public KVMStoragePool getStoragePool(String uuid, boolean refreshInfo) {
        SP_LOG("StorpooolStorageAdaptor.getStoragePool: uuid=%s, refresh=%s", uuid, refreshInfo);
        return storageUuidToStoragePool.get(uuid);
    }

    @Override
    public boolean deleteStoragePool(String uuid) {
        SP_LOG("StorpooolStorageAdaptor.deleteStoragePool: uuid=%s", uuid);
        return storageUuidToStoragePool.remove(uuid) != null;
    }

    @Override
    public boolean deleteStoragePool(KVMStoragePool pool) {
        SP_LOG("StorpooolStorageAdaptor.deleteStoragePool: uuid=%s", pool.getUuid());
        return deleteStoragePool(pool.getUuid());
    }

    private static long getDeviceSize(final String devPath) {
        SP_LOG("StorpooolStorageAdaptor.getDeviceSize: path=%s", devPath);

        if (getVolumeNameFromPath(devPath, true) == null) {
            return 0;
        }
        File file = new File(devPath);
        if (!file.exists()) {
            return 0;
        }
        Script sc = new Script("blockdev", 0, log);
        sc.add("--getsize64", devPath);

        OutputInterpreter.OneLineParser parser = new OutputInterpreter.OneLineParser();

        String res = sc.execute(parser);
        if (res != null) {
            SP_LOG("Unable to retrieve device size for %s. Res: %s", devPath, res);

            log.debug(String.format("Unable to retrieve device size for %s. Res: %s", devPath, res));
            return 0;
        }

        return Long.parseLong(parser.getLine());
    }

    private static boolean waitForDeviceSymlink(String devPath) {
        final int numTries = 10;
        final int sleepTime = 100;

        for(int i = 0; i < numTries; i++) {
            if (getDeviceSize(devPath) != 0) {
                return true;
            } else {
                try {
                    Thread.sleep(sleepTime);
                } catch (Exception ex) {
                    // don't do anything
                }
            }
        }
        return false;
    }

    public static String getVolumeNameFromPath(final String volumeUuid, boolean tildeNeeded) {
        if (volumeUuid.startsWith("/dev/storpool/")) {
            return volumeUuid.split("/")[3];
        } else if (volumeUuid.startsWith("/dev/storpool-byid/")) {
            return tildeNeeded ? "~" + volumeUuid.split("/")[3] : volumeUuid.split("/")[3];
        }

        return null;
    }

    public static boolean attachOrDetachVolume(String command, String type, String volumeUuid) {
        final String name = getVolumeNameFromPath(volumeUuid, true);
        if (name == null) {
            return false;
        }

        SP_LOG("StorpooolStorageAdaptor.attachOrDetachVolume: cmd=%s, type=%s, uuid=%s, name=%s", command, type, volumeUuid, name);

        final int numTries = 10;
        final int sleepTime = 1000;
        String err = null;

        for(int i = 0; i < numTries; i++) {
            Script sc = new Script("storpool", 0, log);
            sc.add("-M");
            sc.add(command);
            sc.add(type, name);
            sc.add("here");
            if (command.equals("attach")) {
                sc.add("onRemoteAttached");
                sc.add("export");
            }

            OutputInterpreter.OneLineParser parser = new OutputInterpreter.OneLineParser();

            String res = sc.execute(parser);
            if (res == null) {
                err = null;
                break;
            }
            err = String.format("Unable to %s volume %s. Error: %s", command, name, res);

            if (command.equals("detach")) {
                try {
                    Thread.sleep(sleepTime);
                } catch (Exception ex) {
                    // don't do anything
                }
            } else {
                break;
            }
        }

        if (err != null) {
            SP_LOG(err);
            log.warn(err);
            throw new CloudRuntimeException(err);
        }

        if (command.equals("attach")) {
            return waitForDeviceSymlink(volumeUuid);
        } else {
            return true;
        }
    }

    public static boolean resize(String newSize, String volumeUuid ) {
        final String name = getVolumeNameFromPath(volumeUuid, true);
        if (name == null) {
            return false;
        }

        SP_LOG("StorpooolStorageAdaptor.resize: size=%s, uuid=%s, name=%s", newSize, volumeUuid, name);

        Script sc = new Script("storpool", 0, log);
        sc.add("-M");
        sc.add("volume");
        sc.add(name);
        sc.add("update");
        sc.add("size");
        sc.add(newSize);
        sc.add("shrinkOk");

        OutputInterpreter.OneLineParser parser = new OutputInterpreter.OneLineParser();
        String res = sc.execute(parser);
        if (res == null) {
            return true;
        }

        String err = String.format("Unable to resize volume %s. Error: %s", name, res);
        SP_LOG(err);
        log.warn(err);
        throw new CloudRuntimeException(err);
    }

    @Override
    public KVMPhysicalDisk getPhysicalDisk(String volumeUuid, KVMStoragePool pool) {
        SP_LOG("StorpooolStorageAdaptor.getPhysicalDisk: uuid=%s, pool=%s", volumeUuid, pool);

        log.debug(String.format("getPhysicalDisk: uuid=%s, pool=%s", volumeUuid, pool));

        final long deviceSize = getDeviceSize(volumeUuid);

        KVMPhysicalDisk physicalDisk = new KVMPhysicalDisk(volumeUuid, volumeUuid, pool);
        physicalDisk.setFormat(PhysicalDiskFormat.RAW);
        physicalDisk.setSize(deviceSize);
        physicalDisk.setVirtualSize(deviceSize);
        return physicalDisk;
    }

    @Override
    public boolean connectPhysicalDisk(String volumeUuid, KVMStoragePool pool, Map<String, String> details) {
        SP_LOG("StorpooolStorageAdaptor.connectPhysicalDisk: uuid=%s, pool=%s", volumeUuid, pool);

        log.debug(String.format("connectPhysicalDisk: uuid=%s, pool=%s", volumeUuid, pool));

        return attachOrDetachVolume("attach", "volume", volumeUuid);
    }

    @Override
    public boolean disconnectPhysicalDisk(String volumeUuid, KVMStoragePool pool) {
        SP_LOG("StorpooolStorageAdaptor.disconnectPhysicalDisk: uuid=%s, pool=%s", volumeUuid, pool);

        log.debug(String.format("disconnectPhysicalDisk: uuid=%s, pool=%s", volumeUuid, pool));
        return attachOrDetachVolume("detach", "volume", volumeUuid);
    }

    public boolean disconnectPhysicalDisk(Map<String, String> volumeToDisconnect) {
        String volumeUuid = volumeToDisconnect.get(DiskTO.UUID);
        SP_LOG("StorpooolStorageAdaptor.disconnectPhysicalDisk: map. uuid=%s", volumeUuid);
        return attachOrDetachVolume("detach", "volume", volumeUuid);
    }

    @Override
    public boolean disconnectPhysicalDiskByPath(String localPath) {
        SP_LOG("StorpooolStorageAdaptor.disconnectPhysicalDiskByPath: localPath=%s", localPath);

        log.debug(String.format("disconnectPhysicalDiskByPath: localPath=%s", localPath));
        return attachOrDetachVolume("detach", "volume", localPath);
    }

    // The following do not apply for StorpoolStorageAdaptor?
    @Override
    public KVMPhysicalDisk createPhysicalDisk(String volumeUuid, KVMStoragePool pool, PhysicalDiskFormat format, Storage.ProvisioningType provisioningType, long size) {
        SP_LOG("StorpooolStorageAdaptor.createPhysicalDisk: uuid=%s, pool=%s, format=%s, size=%d", volumeUuid, pool, format, size);
        throw new UnsupportedOperationException("Creating a physical disk is not supported.");
    }

    @Override
    public boolean deletePhysicalDisk(String volumeUuid, KVMStoragePool pool, Storage.ImageFormat format) {
        // Should only come here when cleaning-up StorPool snapshots associated with CloudStack templates.
        SP_LOG("StorpooolStorageAdaptor.deletePhysicalDisk: uuid=%s, pool=%s, format=%s", volumeUuid, pool, format);
        final String name = getVolumeNameFromPath(volumeUuid, true);
        if (name == null) {
            final String err = String.format("StorpooolStorageAdaptor.deletePhysicalDisk: '%s' is not a StorPool volume?", volumeUuid);
            SP_LOG(err);
            throw new UnsupportedOperationException(err);
        }

        Script sc = new Script("storpool", 0, log);
        sc.add("-M");
        sc.add("snapshot", name);
        sc.add("delete", name);

        OutputInterpreter.OneLineParser parser = new OutputInterpreter.OneLineParser();

        String res = sc.execute(parser);
        if (res != null) {
            final String err = String.format("Unable to delete StorPool snapshot '%s'. Error: %s", name, res);
            SP_LOG(err);
            log.warn(err);
            throw new UnsupportedOperationException(err);
        }
        return true; // apparently ignored
    }

    @Override
    public List<KVMPhysicalDisk> listPhysicalDisks(String storagePoolUuid, KVMStoragePool pool) {
        SP_LOG("StorpooolStorageAdaptor.listPhysicalDisks: uuid=%s, pool=%s", storagePoolUuid, pool);
        throw new UnsupportedOperationException("Listing disks is not supported for this configuration.");
    }

    @Override
    public KVMPhysicalDisk createDiskFromTemplate(KVMPhysicalDisk template, String name, PhysicalDiskFormat format,
            ProvisioningType provisioningType, long size, KVMStoragePool destPool, int timeout) {
        SP_LOG("StorpooolStorageAdaptor.createDiskFromTemplate: template=%s, name=%s, fmt=%s, ptype=%s, size=%d, dst_pool=%s, to=%d",
            template, name, format, provisioningType, size, destPool.getUuid(), timeout);
        throw new UnsupportedOperationException("Creating a disk from a template is not yet supported for this configuration.");
    }

    @Override
    public KVMPhysicalDisk createTemplateFromDisk(KVMPhysicalDisk disk, String name, PhysicalDiskFormat format, long size, KVMStoragePool destPool) {
        SP_LOG("StorpooolStorageAdaptor.createTemplateFromDisk: disk=%s, name=%s, fmt=%s, size=%d, dst_pool=%s", disk, name, format, size, destPool.getUuid());
        throw new UnsupportedOperationException("Creating a template from a disk is not yet supported for this configuration.");
    }

    @Override
    public KVMPhysicalDisk copyPhysicalDisk(KVMPhysicalDisk disk, String name, KVMStoragePool destPool, int timeout) {
        SP_LOG("StorpooolStorageAdaptor.copyPhysicalDisk: disk=%s, name=%s, dst_pool=%s, to=%d", disk, name, destPool.getUuid(), timeout);
        throw new UnsupportedOperationException("Copying a disk is not supported in this configuration.");
    }

    public KVMPhysicalDisk createDiskFromSnapshot(KVMPhysicalDisk snapshot, String snapshotName, String name, KVMStoragePool destPool) {
        SP_LOG("StorpooolStorageAdaptor.createDiskFromSnapshot: snap=%s, snap_name=%s, name=%s, dst_pool=%s", snapshot, snapshotName, name, destPool.getUuid());
        throw new UnsupportedOperationException("Creating a disk from a snapshot is not supported in this configuration.");
    }

    @Override
    public boolean refresh(KVMStoragePool pool) {
        SP_LOG("StorpooolStorageAdaptor.refresh: pool=%s", pool);
        return true;
    }

    @Override
    public boolean createFolder(String uuid, String path) {
        SP_LOG("StorpooolStorageAdaptor.createFolder: uuid=%s, path=%s", uuid, path);
        throw new UnsupportedOperationException("A folder cannot be created in this configuration.");
    }

    public KVMPhysicalDisk createDiskFromSnapshot(KVMPhysicalDisk snapshot, String snapshotName, String name,
            KVMStoragePool destPool, int timeout) {
        SP_LOG("StorpooolStorageAdaptor.createDiskFromSnapshot: snap=%s, snap_name=%s, name=%s, dst_pool=%s", snapshot,
                snapshotName, name, destPool.getUuid());
        throw new UnsupportedOperationException(
                "Creating a disk from a snapshot is not supported in this configuration.");
    }

    public KVMPhysicalDisk createDiskFromTemplateBacking(KVMPhysicalDisk template, String name,
            PhysicalDiskFormat format, long size, KVMStoragePool destPool, int timeout) {
        SP_LOG("StorpooolStorageAdaptor.createDiskFromTemplateBacking: template=%s, name=%s, dst_pool=%s", template,
                name, destPool.getUuid());
        throw new UnsupportedOperationException(
                "Creating a disk from a template is not supported in this configuration.");
    }

    public KVMPhysicalDisk createTemplateFromDirectDownloadFile(String templateFilePath, KVMStoragePool destPool,
            boolean isIso) {
        SP_LOG("StorpooolStorageAdaptor.createTemplateFromDirectDownloadFile: templateFilePath=%s, dst_pool=%s",
                templateFilePath, destPool.getUuid());
        throw new UnsupportedOperationException(
                "Creating a template from direct download is not supported in this configuration.");
    }

    public KVMPhysicalDisk createTemplateFromDirectDownloadFile(String templateFilePath, String destTemplatePath,
            KVMStoragePool destPool, ImageFormat format, int timeout) {
        return null;
    }

    @Override
    public boolean createFolder(String uuid, String path, String localPath) {
        return false;
    }
}
