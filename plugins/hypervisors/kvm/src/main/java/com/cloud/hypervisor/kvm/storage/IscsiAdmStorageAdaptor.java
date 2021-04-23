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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.apache.log4j.Logger;

import org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat;

import com.cloud.agent.api.to.DiskTO;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.ProvisioningType;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.utils.StringUtils;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import org.libvirt.LibvirtException;

@StorageAdaptorInfo(storagePoolType=StoragePoolType.Iscsi)
public class IscsiAdmStorageAdaptor implements StorageAdaptor {
    private static final Logger s_logger = Logger.getLogger(IscsiAdmStorageAdaptor.class);

    private static final Map<String, KVMStoragePool> MapStorageUuidToStoragePool = new HashMap<>();

    @Override
    public KVMStoragePool createStoragePool(String uuid, String host, int port, String path, String userInfo, StoragePoolType storagePoolType) {
        IscsiAdmStoragePool storagePool = new IscsiAdmStoragePool(uuid, host, port, storagePoolType, this);

        MapStorageUuidToStoragePool.put(uuid, storagePool);

        return storagePool;
    }

    @Override
    public KVMStoragePool getStoragePool(String uuid) {
        return MapStorageUuidToStoragePool.get(uuid);
    }

    @Override
    public KVMStoragePool getStoragePool(String uuid, boolean refreshInfo) {
        return MapStorageUuidToStoragePool.get(uuid);
    }

    @Override
    public boolean deleteStoragePool(String uuid) {
        return MapStorageUuidToStoragePool.remove(uuid) != null;
    }

    @Override
    public boolean deleteStoragePool(KVMStoragePool pool) {
        return deleteStoragePool(pool.getUuid());
    }

    // called from LibvirtComputingResource.execute(CreateCommand)
    // does not apply for iScsiAdmStorageAdaptor
    @Override
    public KVMPhysicalDisk createPhysicalDisk(String volumeUuid, KVMStoragePool pool, PhysicalDiskFormat format, Storage.ProvisioningType provisioningType, long size) {
        throw new UnsupportedOperationException("Creating a physical disk is not supported.");
    }

    @Override
    public boolean connectPhysicalDisk(String volumeUuid, KVMStoragePool pool, Map<String, String> details) {
        // ex. sudo iscsiadm -m node -T iqn.2012-03.com.test:volume1 -p 192.168.233.10:3260 -o new
        Script iScsiAdmCmd = new Script(true, "iscsiadm", 0, s_logger);

        iScsiAdmCmd.add("-m", "node");
        iScsiAdmCmd.add("-T", getIqn(volumeUuid));
        iScsiAdmCmd.add("-p", pool.getSourceHost() + ":" + pool.getSourcePort());
        iScsiAdmCmd.add("-o", "new");

        String result = iScsiAdmCmd.execute();

        if (result != null) {
            s_logger.debug("Failed to add iSCSI target " + volumeUuid);
            System.out.println("Failed to add iSCSI target " + volumeUuid);

            return false;
        } else {
            s_logger.debug("Successfully added iSCSI target " + volumeUuid);
            System.out.println("Successfully added to iSCSI target " + volumeUuid);
        }

        String chapInitiatorUsername = details.get(DiskTO.CHAP_INITIATOR_USERNAME);
        String chapInitiatorSecret = details.get(DiskTO.CHAP_INITIATOR_SECRET);

        if (StringUtils.isNotBlank(chapInitiatorUsername) && StringUtils.isNotBlank(chapInitiatorSecret)) {
            try {
                // ex. sudo iscsiadm -m node -T iqn.2012-03.com.test:volume1 -p 192.168.233.10:3260 --op update -n node.session.auth.authmethod -v CHAP
                executeChapCommand(volumeUuid, pool, "node.session.auth.authmethod", "CHAP", null);

                // ex. sudo iscsiadm -m node -T iqn.2012-03.com.test:volume1 -p 192.168.233.10:3260 --op update -n node.session.auth.username -v username
                executeChapCommand(volumeUuid, pool, "node.session.auth.username", chapInitiatorUsername, "username");

                // ex. sudo iscsiadm -m node -T iqn.2012-03.com.test:volume1 -p 192.168.233.10:3260 --op update -n node.session.auth.password -v password
                executeChapCommand(volumeUuid, pool, "node.session.auth.password", chapInitiatorSecret, "password");
            } catch (Exception ex) {
                return false;
            }
        }

        // ex. sudo iscsiadm -m node -T iqn.2012-03.com.test:volume1 -p 192.168.233.10:3260 --login
        iScsiAdmCmd = new Script(true, "iscsiadm", 0, s_logger);

        iScsiAdmCmd.add("-m", "node");
        iScsiAdmCmd.add("-T", getIqn(volumeUuid));
        iScsiAdmCmd.add("-p", pool.getSourceHost() + ":" + pool.getSourcePort());
        iScsiAdmCmd.add("--login");

        result = iScsiAdmCmd.execute();

        if (result != null) {
            s_logger.debug("Failed to log in to iSCSI target " + volumeUuid);
            System.out.println("Failed to log in to iSCSI target " + volumeUuid);

            return false;
        } else {
            s_logger.debug("Successfully logged in to iSCSI target " + volumeUuid);
            System.out.println("Successfully logged in to iSCSI target " + volumeUuid);
        }

        // There appears to be a race condition where logging in to the iSCSI volume via iscsiadm
        // returns success before the device has been added to the OS.
        // What happens is you get logged in and the device shows up, but the device may not
        // show up before we invoke Libvirt to attach the device to a VM.
        // waitForDiskToBecomeAvailable(String, KVMStoragePool) invokes blockdev
        // via getPhysicalDisk(String, KVMStoragePool) and checks if the size came back greater
        // than 0.
        // After a certain number of tries and a certain waiting period in between tries,
        // this method could still return (it should not block indefinitely) (the race condition
        // isn't solved here, but made highly unlikely to be a problem).
        waitForDiskToBecomeAvailable(volumeUuid, pool);

        return true;
    }

    private void waitForDiskToBecomeAvailable(String volumeUuid, KVMStoragePool pool) {
        int numberOfTries = 10;
        int timeBetweenTries = 1000;

        while (getPhysicalDisk(volumeUuid, pool).getSize() == 0 && numberOfTries > 0) {
            numberOfTries--;

            try {
                Thread.sleep(timeBetweenTries);
            } catch (Exception ex) {
                // don't do anything
            }
        }
    }

    private void waitForDiskToBecomeUnavailable(String host, int port, String iqn, String lun) {
        int numberOfTries = 10;
        int timeBetweenTries = 1000;

        String deviceByPath = getByPath(host, port, "/" + iqn + "/" + lun);

        while (getDeviceSize(deviceByPath) > 0 && numberOfTries > 0) {
            numberOfTries--;

            try {
                Thread.sleep(timeBetweenTries);
            } catch (Exception ex) {
                // don't do anything
            }
        }
    }

    private void executeChapCommand(String path, KVMStoragePool pool, String nParameter, String vParameter, String detail) throws Exception {
        Script iScsiAdmCmd = new Script(true, "iscsiadm", 0, s_logger);

        iScsiAdmCmd.add("-m", "node");
        iScsiAdmCmd.add("-T", getIqn(path));
        iScsiAdmCmd.add("-p", pool.getSourceHost() + ":" + pool.getSourcePort());
        iScsiAdmCmd.add("--op", "update");
        iScsiAdmCmd.add("-n", nParameter);
        iScsiAdmCmd.add("-v", vParameter);

        String result = iScsiAdmCmd.execute();

        boolean useDetail = detail != null && detail.trim().length() > 0;

        detail = useDetail ? detail.trim() + " " : detail;

        if (result != null) {
            s_logger.debug("Failed to execute CHAP " + (useDetail ? detail : "") + "command for iSCSI target " + path + " : message = " + result);
            System.out.println("Failed to execute CHAP " + (useDetail ? detail : "") + "command for iSCSI target " + path + " : message = " + result);

            throw new Exception("Failed to execute CHAP " + (useDetail ? detail : "") + "command for iSCSI target " + path + " : message = " + result);
        } else {
            s_logger.debug("CHAP " + (useDetail ? detail : "") + "command executed successfully for iSCSI target " + path);
            System.out.println("CHAP " + (useDetail ? detail : "") + "command executed successfully for iSCSI target " + path);
        }
    }

    // example by-path: /dev/disk/by-path/ip-192.168.233.10:3260-iscsi-iqn.2012-03.com.solidfire:storagepool2-lun-0
    private static String getByPath(String host, int port, String path) {
        return "/dev/disk/by-path/ip-" + host + ":" + port + "-iscsi-" + getIqn(path) + "-lun-" + getLun(path);
    }

    @Override
    public KVMPhysicalDisk getPhysicalDisk(String volumeUuid, KVMStoragePool pool) {
        String deviceByPath = getByPath(pool.getSourceHost(), pool.getSourcePort(), volumeUuid);
        KVMPhysicalDisk physicalDisk = new KVMPhysicalDisk(deviceByPath, volumeUuid, pool);

        physicalDisk.setFormat(PhysicalDiskFormat.RAW);

        long deviceSize = getDeviceSize(deviceByPath);

        physicalDisk.setSize(deviceSize);
        physicalDisk.setVirtualSize(deviceSize);

        return physicalDisk;
    }

    private long getDeviceSize(String deviceByPath) {
        Script iScsiAdmCmd = new Script(true, "blockdev", 0, s_logger);

        iScsiAdmCmd.add("--getsize64", deviceByPath);

        OutputInterpreter.OneLineParser parser = new OutputInterpreter.OneLineParser();

        String result = iScsiAdmCmd.execute(parser);

        if (result != null) {
            s_logger.warn("Unable to retrieve the size of device (resource may have moved to a different host)" + deviceByPath);

            return 0;
        }
        else {
            s_logger.info("Successfully retrieved the size of device " + deviceByPath);
        }

        return Long.parseLong(parser.getLine());
    }

    private static String getIqn(String path) {
        return getComponent(path, 1);
    }

    private static String getLun(String path) {
        return getComponent(path, 2);
    }

    private static String getComponent(String path, int index) {
        String[] tmp = path.split("/");

        if (tmp.length != 3) {
            String msg = "Wrong format for iScsi path: " + path + ". It should be formatted as '/targetIQN/LUN'.";

            s_logger.warn(msg);

            throw new CloudRuntimeException(msg);
        }

        return tmp[index].trim();
    }

    private boolean disconnectPhysicalDisk(String host, int port, String iqn, String lun) {
        // use iscsiadm to log out of the iSCSI target and un-discover it

        // ex. sudo iscsiadm -m node -T iqn.2012-03.com.test:volume1 -p 192.168.233.10:3260 --logout
        Script iScsiAdmCmd = new Script(true, "iscsiadm", 0, s_logger);

        iScsiAdmCmd.add("-m", "node");
        iScsiAdmCmd.add("-T", iqn);
        iScsiAdmCmd.add("-p", host + ":" + port);
        iScsiAdmCmd.add("--logout");

        String result = iScsiAdmCmd.execute();

        if (result != null) {
            s_logger.debug("Failed to log out of iSCSI target /" + iqn + "/" + lun + " : message = " + result);
            System.out.println("Failed to log out of iSCSI target /" + iqn + "/" + lun + " : message = " + result);

            return false;
        } else {
            s_logger.debug("Successfully logged out of iSCSI target /" + iqn + "/" + lun);
            System.out.println("Successfully logged out of iSCSI target /" + iqn + "/" + lun);
        }

        // ex. sudo iscsiadm -m node -T iqn.2012-03.com.test:volume1 -p 192.168.233.10:3260 -o delete
        iScsiAdmCmd = new Script(true, "iscsiadm", 0, s_logger);

        iScsiAdmCmd.add("-m", "node");
        iScsiAdmCmd.add("-T", iqn);
        iScsiAdmCmd.add("-p", host + ":" + port);
        iScsiAdmCmd.add("-o", "delete");

        result = iScsiAdmCmd.execute();

        if (result != null) {
            s_logger.debug("Failed to remove iSCSI target /" + iqn + "/" + lun + " : message = " + result);
            System.out.println("Failed to remove iSCSI target /" + iqn + "/" + lun + " : message = " + result);

            return false;
        } else {
            s_logger.debug("Removed iSCSI target /" + iqn + "/" + lun);
            System.out.println("Removed iSCSI target /" + iqn + "/" + lun);
        }

        waitForDiskToBecomeUnavailable(host, port, iqn, lun);

        return true;
    }

    @Override
    public boolean disconnectPhysicalDisk(String volumeUuid, KVMStoragePool pool) {
        return disconnectPhysicalDisk(pool.getSourceHost(), pool.getSourcePort(), getIqn(volumeUuid), getLun(volumeUuid));
    }

    @Override
    public boolean disconnectPhysicalDisk(Map<String, String> volumeToDisconnect) {
        String poolType = volumeToDisconnect.get(DiskTO.PROTOCOL_TYPE);
        // Unsupported pool types
        if (poolType != null && poolType.equalsIgnoreCase(StoragePoolType.PowerFlex.toString())) {
            return false;
        }

        String host = volumeToDisconnect.get(DiskTO.STORAGE_HOST);
        String port = volumeToDisconnect.get(DiskTO.STORAGE_PORT);
        String path = volumeToDisconnect.get(DiskTO.IQN);

        if (host != null && port != null && path != null) {
            return disconnectPhysicalDisk(host, Integer.parseInt(port), getIqn(path), getLun(path));
        }

        return false;
    }

    @Override
    public boolean disconnectPhysicalDiskByPath(String localPath) {
        String search1 = "/dev/disk/by-path/ip-";
        String search2 = ":";
        String search3 = "-iscsi-";
        String search4 = "-lun-";

        if (!localPath.contains(search3)) {
            return false;
        }

        int index = localPath.indexOf(search2);

        String host = localPath.substring(search1.length(), index);

        int index2 = localPath.indexOf(search3);

        String port = localPath.substring(index + search2.length(), index2);

        index = localPath.indexOf(search4);

        String iqn = localPath.substring(index2 + search3.length(), index);

        String lun = localPath.substring(index + search4.length());

        return disconnectPhysicalDisk(host, Integer.parseInt(port), iqn, lun);
    }

    @Override
    public boolean deletePhysicalDisk(String volumeUuid, KVMStoragePool pool, Storage.ImageFormat format) {
        throw new UnsupportedOperationException("Deleting a physical disk is not supported.");
    }

    // does not apply for iScsiAdmStorageAdaptor
    @Override
    public List<KVMPhysicalDisk> listPhysicalDisks(String storagePoolUuid, KVMStoragePool pool) {
        throw new UnsupportedOperationException("Listing disks is not supported for this configuration.");
    }

    @Override
    public KVMPhysicalDisk createDiskFromTemplate(KVMPhysicalDisk template, String name, PhysicalDiskFormat format,
            ProvisioningType provisioningType, long size,
            KVMStoragePool destPool, int timeout) {
        throw new UnsupportedOperationException("Creating a disk from a template is not yet supported for this configuration.");
    }

    @Override
    public KVMPhysicalDisk createTemplateFromDisk(KVMPhysicalDisk disk, String name, PhysicalDiskFormat format, long size, KVMStoragePool destPool) {
        throw new UnsupportedOperationException("Creating a template from a disk is not yet supported for this configuration.");
    }

    @Override
    public KVMPhysicalDisk copyPhysicalDisk(KVMPhysicalDisk srcDisk, String destVolumeUuid, KVMStoragePool destPool, int timeout) {
        QemuImg q = new QemuImg(timeout);

        QemuImgFile srcFile;

        KVMStoragePool srcPool = srcDisk.getPool();

        if (srcPool.getType() == StoragePoolType.RBD) {
            srcFile = new QemuImgFile(KVMPhysicalDisk.RBDStringBuilder(srcPool.getSourceHost(), srcPool.getSourcePort(),
                                                                       srcPool.getAuthUserName(), srcPool.getAuthSecret(),
                                                                       srcDisk.getPath()),srcDisk.getFormat());
        } else {
            srcFile = new QemuImgFile(srcDisk.getPath(), srcDisk.getFormat());
        }

        KVMPhysicalDisk destDisk = destPool.getPhysicalDisk(destVolumeUuid);

        QemuImgFile destFile = new QemuImgFile(destDisk.getPath(), destDisk.getFormat());

        try {
            q.convert(srcFile, destFile);
        } catch (QemuImgException | LibvirtException ex) {
            String msg = "Failed to copy data from " + srcDisk.getPath() + " to " +
                    destDisk.getPath() + ". The error was the following: " + ex.getMessage();

            s_logger.error(msg);

            throw new CloudRuntimeException(msg);
        }

        return destPool.getPhysicalDisk(destVolumeUuid);
    }

    @Override
    public KVMPhysicalDisk createDiskFromSnapshot(KVMPhysicalDisk snapshot, String snapshotName, String name, KVMStoragePool destPool, int timeout) {
        throw new UnsupportedOperationException("Creating a disk from a snapshot is not supported in this configuration.");
    }

    @Override
    public boolean refresh(KVMStoragePool pool) {
        return true;
    }

    @Override
    public boolean createFolder(String uuid, String path) {
        throw new UnsupportedOperationException("A folder cannot be created in this configuration.");
    }

    @Override
    public KVMPhysicalDisk createDiskFromTemplateBacking(KVMPhysicalDisk template, String name, PhysicalDiskFormat format, long size, KVMStoragePool destPool, int timeout) {
        return null;
    }

    @Override
    public KVMPhysicalDisk createTemplateFromDirectDownloadFile(String templateFilePath, String destTemplatePath, KVMStoragePool destPool, Storage.ImageFormat format, int timeout) {
        return null;
    }
}
