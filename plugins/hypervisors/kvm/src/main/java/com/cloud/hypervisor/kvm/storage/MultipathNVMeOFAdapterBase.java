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

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.libvirt.LibvirtException;

import com.cloud.storage.Storage;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Base class for KVM storage adapters that surface remote block volumes over
 * NVMe-over-Fabrics (NVMe-oF). It is the NVMe-oF counterpart of
 * {@link MultipathSCSIAdapterBase}: it does not drive device-mapper multipath
 * and does not rescan the SCSI bus, because NVMe-oF has its own multipath
 * (the kernel's native NVMe multipath) and namespaces show up via
 * asynchronous event notifications as soon as the target grants access.
 *
 * Volumes are identified on the host by their EUI-128 NGUID, which udev
 * exposes as {@code /dev/disk/by-id/nvme-eui.<eui>}.
 */
public abstract class MultipathNVMeOFAdapterBase implements StorageAdaptor {
    protected static Logger LOGGER = LogManager.getLogger(MultipathNVMeOFAdapterBase.class);
    static final Map<String, KVMStoragePool> MapStorageUuidToStoragePool = new HashMap<>();

    static final int DEFAULT_DISK_WAIT_SECS = 240;
    static final long NS_RESCAN_TIMEOUT_SECS = 5;
    private static final long POLL_INTERVAL_MS = 2000;

    @Override
    public KVMStoragePool getStoragePool(String uuid) {
        KVMStoragePool pool = MapStorageUuidToStoragePool.get(uuid);
        if (pool == null) {
            // Dummy pool - adapters that dispatch per-volume don't need
            // connectivity information on the pool itself.
            pool = new MultipathNVMeOFPool(uuid, this);
            MapStorageUuidToStoragePool.put(uuid, pool);
        }
        return pool;
    }

    @Override
    public KVMStoragePool getStoragePool(String uuid, boolean refreshInfo) {
        return getStoragePool(uuid);
    }

    public abstract String getName();

    @Override
    public abstract Storage.StoragePoolType getStoragePoolType();

    public abstract boolean isStoragePoolTypeSupported(Storage.StoragePoolType type);

    /**
     * Parse a {@code type=NVMETCP; address=<eui>; connid.<host>=<nsid>; ...}
     * volume path and produce an {@link AddressInfo} with the host-side device
     * path set to {@code /dev/disk/by-id/nvme-eui.<eui>}.
     */
    public AddressInfo parseAndValidatePath(String inPath) {
        String type = null;
        String address = null;
        String connectionId = null;
        String path = null;
        String hostname = resolveHostnameShort();
        String hostnameFq = resolveHostnameFq();
        String[] parts = inPath.split(";");
        for (String part : parts) {
            String[] pair = part.split("=");
            if (pair.length != 2) {
                continue;
            }
            String key = pair[0].trim();
            String value = pair[1].trim();
            if (key.equals("type")) {
                type = value.toUpperCase();
            } else if (key.equals("address")) {
                address = value;
            } else if (key.equals("connid")) {
                connectionId = value;
            } else if (key.startsWith("connid.")) {
                String inHostname = key.substring("connid.".length());
                if (inHostname.equals(hostname) || inHostname.equals(hostnameFq)) {
                    connectionId = value;
                }
            }
        }

        if (!"NVMETCP".equals(type)) {
            throw new CloudRuntimeException("Invalid address type provided for NVMe-oF target disk: " + type);
        }
        if (address == null) {
            throw new CloudRuntimeException("NVMe-oF volume path is missing the required address field");
        }
        path = "/dev/disk/by-id/nvme-eui." + address.toLowerCase();
        return new AddressInfo(type, address, connectionId, path);
    }

    @Override
    public KVMPhysicalDisk getPhysicalDisk(String volumePath, KVMStoragePool pool) {
        if (StringUtils.isEmpty(volumePath) || pool == null) {
            LOGGER.error("Unable to get physical disk, volume path or pool not specified");
            return null;
        }
        return getPhysicalDisk(parseAndValidatePath(volumePath), pool);
    }

    private KVMPhysicalDisk getPhysicalDisk(AddressInfo address, KVMStoragePool pool) {
        KVMPhysicalDisk disk = new KVMPhysicalDisk(address.getPath(), address.toString(), pool);
        disk.setFormat(QemuImg.PhysicalDiskFormat.RAW);

        if (!isConnected(address.getPath())) {
            if (!connectPhysicalDisk(address, pool, null)) {
                throw new CloudRuntimeException("Unable to connect to NVMe namespace at " + address.getPath());
            }
        }
        long diskSize = getPhysicalDiskSize(address.getPath());
        disk.setSize(diskSize);
        disk.setVirtualSize(diskSize);
        return disk;
    }

    @Override
    public KVMStoragePool createStoragePool(String uuid, String host, int port, String path, String userInfo, Storage.StoragePoolType type, Map<String, String> details, boolean isPrimaryStorage) {
        LOGGER.info(String.format("createStoragePool(uuid,host,port,path,type) called with args (%s, %s, %d, %s, %s)", uuid, host, port, path, type));
        MultipathNVMeOFPool pool = new MultipathNVMeOFPool(uuid, host, port, path, type, details, this);
        MapStorageUuidToStoragePool.put(uuid, pool);
        return pool;
    }

    @Override
    public boolean deleteStoragePool(String uuid) {
        MapStorageUuidToStoragePool.remove(uuid);
        return true;
    }

    @Override
    public boolean deleteStoragePool(KVMStoragePool pool) {
        return deleteStoragePool(pool.getUuid());
    }

    @Override
    public boolean connectPhysicalDisk(String volumePath, KVMStoragePool pool, Map<String, String> details, boolean isVMMigrate) {
        if (StringUtils.isEmpty(volumePath) || pool == null) {
            LOGGER.error("Unable to connect NVMe-oF physical disk: insufficient arguments");
            return false;
        }
        return connectPhysicalDisk(parseAndValidatePath(volumePath), pool, details);
    }

    private boolean connectPhysicalDisk(AddressInfo address, KVMStoragePool pool, Map<String, String> details) {
        if (address.getConnectionId() == null) {
            LOGGER.error("NVMe-oF volume " + address.getPath() + " on pool " + pool.getUuid() + " is missing a connid.<host> token in its path");
            return false;
        }
        long waitSecs = DEFAULT_DISK_WAIT_SECS;
        if (details != null && details.containsKey(com.cloud.storage.StorageManager.STORAGE_POOL_DISK_WAIT.toString())) {
            String waitTime = details.get(com.cloud.storage.StorageManager.STORAGE_POOL_DISK_WAIT.toString());
            if (StringUtils.isNotEmpty(waitTime)) {
                waitSecs = Integer.parseInt(waitTime);
            }
        }
        return waitForNamespace(address, pool, waitSecs);
    }

    /**
     * Poll for the EUI-keyed udev symlink to show up. On every iteration also
     * nudge the kernel with {@code nvme ns-rescan} on every local NVMe
     * controller, to cover arrays / firmware combinations that do not emit a
     * reliable asynchronous event notification when a new namespace is
     * mapped.
     */
    private boolean waitForNamespace(AddressInfo address, KVMStoragePool pool, long waitSecs) {
        if (waitSecs < 60) {
            waitSecs = 60;
        }
        long deadline = System.currentTimeMillis() + (waitSecs * 1000);
        File dev = new File(address.getPath());
        while (System.currentTimeMillis() < deadline) {
            if (dev.exists() && isConnected(address.getPath())) {
                long size = getPhysicalDiskSize(address.getPath());
                if (size > 0) {
                    LOGGER.debug("Found NVMe namespace at " + address.getPath());
                    return true;
                }
            }
            rescanAllControllers();
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        LOGGER.debug("NVMe namespace did not appear at " + address.getPath() + " within " + waitSecs + "s");
        return false;
    }

    private void rescanAllControllers() {
        try {
            File sysClass = new File("/sys/class/nvme");
            File[] ctrls = sysClass.listFiles();
            if (ctrls == null) {
                return;
            }
            for (File ctrl : ctrls) {
                Process p = new ProcessBuilder("nvme", "ns-rescan", "/dev/" + ctrl.getName())
                        .redirectErrorStream(true).start();
                p.waitFor(NS_RESCAN_TIMEOUT_SECS, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            LOGGER.debug("nvme ns-rescan attempt failed: " + e.getMessage());
        }
    }

    @Override
    public boolean disconnectPhysicalDisk(String volumePath, KVMStoragePool pool) {
        // NVMe-oF: the kernel drops the namespace as soon as the target
        // removes the host(-group) connection. No host-side action needed.
        return true;
    }

    @Override
    public boolean disconnectPhysicalDisk(Map<String, String> volumeToDisconnect) {
        return true;
    }

    @Override
    public boolean disconnectPhysicalDiskByPath(String localPath) {
        // Same rationale as disconnectPhysicalDisk above. Only claim paths
        // that look like NVMe EUI symlinks so we don't swallow foreign paths.
        return localPath != null && localPath.startsWith("/dev/disk/by-id/nvme-eui.");
    }

    @Override
    public boolean deletePhysicalDisk(String uuid, KVMStoragePool pool, Storage.ImageFormat format) {
        throw new UnsupportedOperationException("Deletion of NVMe namespaces is the storage provider's responsibility");
    }

    @Override
    public KVMPhysicalDisk createPhysicalDisk(String name, KVMStoragePool pool, PhysicalDiskFormat format,
            Storage.ProvisioningType provisioningType, long size, byte[] passphrase) {
        throw new UnsupportedOperationException("Unimplemented method 'createPhysicalDisk'");
    }

    @Override
    public KVMPhysicalDisk createTemplateFromDisk(KVMPhysicalDisk disk, String name, QemuImg.PhysicalDiskFormat format, long size, KVMStoragePool destPool) {
        throw new UnsupportedOperationException("Unimplemented method 'createTemplateFromDisk'");
    }

    @Override
    public List<KVMPhysicalDisk> listPhysicalDisks(String storagePoolUuid, KVMStoragePool pool) {
        throw new UnsupportedOperationException("Unimplemented method 'listPhysicalDisks'");
    }

    @Override
    public KVMPhysicalDisk copyPhysicalDisk(KVMPhysicalDisk disk, String name, KVMStoragePool destPool, int timeout) {
        return copyPhysicalDisk(disk, name, destPool, timeout, null, null, null);
    }

    /**
     * Copy a template or source disk into a pre-provisioned NVMe namespace on
     * this pool, so it can be consumed by a VM as a root or data volume.
     *
     * The destination namespace is expected to have already been created on
     * the storage provider and connected to this host's hostgroup (that is
     * the storage orchestrator's responsibility, not the KVM adapter's). All
     * this method does is resolve the destination device path via
     * {@link #getPhysicalDisk} - which will nvme ns-rescan and wait for the
     * by-id/nvme-eui.&lt;NGUID&gt; symlink to show up if the kernel has not
     * picked it up yet - and {@code qemu-img convert} the source image into
     * the raw block device.
     *
     * User-space encryption passphrases are not supported: the provider
     * already encrypts at rest and qemu-img LUKS on top of a shared
     * hostgroup-scoped namespace is not a sensible layering.
     */
    @Override
    public KVMPhysicalDisk copyPhysicalDisk(KVMPhysicalDisk disk, String name, KVMStoragePool destPool, int timeout,
            byte[] srcPassphrase, byte[] destPassphrase, Storage.ProvisioningType provisioningType) {
        if (disk == null || StringUtils.isEmpty(name) || destPool == null) {
            throw new CloudRuntimeException("Unable to copy disk to NVMe-oF pool: source disk, destination volume name or destination pool not specified");
        }
        if (srcPassphrase != null || destPassphrase != null) {
            throw new CloudRuntimeException("NVMe-oF adapter does not support user-space encrypted source or destination volumes");
        }

        KVMPhysicalDisk destDisk = destPool.getPhysicalDisk(name);
        if (destDisk == null || StringUtils.isEmpty(destDisk.getPath())) {
            throw new CloudRuntimeException("Unable to resolve NVMe namespace for destination volume [" + name + "] on pool [" + destPool.getUuid() + "]");
        }

        destDisk.setFormat(QemuImg.PhysicalDiskFormat.RAW);
        destDisk.setVirtualSize(disk.getVirtualSize());
        destDisk.setSize(disk.getSize());

        LOGGER.info(String.format("Copying source disk [path=%s, format=%s, virtualSize=%d] to NVMe-oF namespace [path=%s] on pool [%s]",
                disk.getPath(), disk.getFormat(), disk.getVirtualSize(), destDisk.getPath(), destPool.getUuid()));

        QemuImgFile srcFile = new QemuImgFile(disk.getPath(), disk.getFormat());
        QemuImgFile destFile = new QemuImgFile(destDisk.getPath(), destDisk.getFormat());

        try {
            QemuImg qemu = new QemuImg(timeout);
            qemu.convert(srcFile, destFile, true);
        } catch (QemuImgException | LibvirtException e) {
            throw new CloudRuntimeException("Failed to copy source disk [" + disk.getPath() + "] to NVMe-oF namespace ["
                    + destDisk.getPath() + "] on pool [" + destPool.getUuid() + "]: " + e.getMessage(), e);
        }

        LOGGER.info("Successfully copied source disk to NVMe-oF namespace [" + destDisk.getPath() + "] on pool [" + destPool.getUuid() + "]");
        return destDisk;
    }

    @Override
    public KVMPhysicalDisk createDiskFromTemplate(KVMPhysicalDisk template, String name, PhysicalDiskFormat format, Storage.ProvisioningType provisioningType, long size, KVMStoragePool destPool, int timeout, byte[] passphrase) {
        throw new UnsupportedOperationException("Unimplemented method 'createDiskFromTemplate'");
    }

    @Override
    public KVMPhysicalDisk createDiskFromTemplateBacking(KVMPhysicalDisk template, String name, PhysicalDiskFormat format, long size, KVMStoragePool destPool, int timeout, byte[] passphrase) {
        throw new UnsupportedOperationException("Unimplemented method 'createDiskFromTemplateBacking'");
    }

    @Override
    public KVMPhysicalDisk createTemplateFromDirectDownloadFile(String templateFilePath, String destTemplatePath, KVMStoragePool destPool, Storage.ImageFormat format, int timeout) {
        throw new UnsupportedOperationException("Unimplemented method 'createTemplateFromDirectDownloadFile'");
    }

    @Override
    public boolean refresh(KVMStoragePool pool) {
        return true;
    }

    @Override
    public boolean createFolder(String uuid, String path) {
        throw new UnsupportedOperationException("Unimplemented method 'createFolder'");
    }

    @Override
    public boolean createFolder(String uuid, String path, String localPath) {
        throw new UnsupportedOperationException("Unimplemented method 'createFolder'");
    }

    public void resize(String path, String vmName, long newSize) {
        throw new UnsupportedOperationException("Volume resize on NVMe-oF pools is driven by the storage provider, not the KVM adapter");
    }

    boolean isConnected(String path) {
        Script test = new Script("/bin/test", LOGGER);
        test.add("-b", path);
        test.execute();
        return test.getExitValue() == 0;
    }

    long getPhysicalDiskSize(String diskPath) {
        if (StringUtils.isEmpty(diskPath)) {
            return 0;
        }
        Script cmd = new Script("blockdev", LOGGER);
        cmd.add("--getsize64", diskPath);
        OutputInterpreter.OneLineParser parser = new OutputInterpreter.OneLineParser();
        String result = cmd.execute(parser);
        if (result != null) {
            LOGGER.debug("Unable to get the disk size at path: " + diskPath);
            return 0;
        }
        try {
            return Long.parseLong(parser.getLine());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String resolveHostnameShort() {
        try {
            String h = java.net.InetAddress.getLocalHost().getHostName();
            int dot = h.indexOf('.');
            return dot > 0 ? h.substring(0, dot) : h;
        } catch (Exception e) {
            return null;
        }
    }

    private static String resolveHostnameFq() {
        try {
            return java.net.InetAddress.getLocalHost().getCanonicalHostName();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Same shape as {@link MultipathSCSIAdapterBase.AddressInfo}. Kept
     * separate so this class can be consumed by adapters that don't share the
     * SCSI base.
     */
    public static final class AddressInfo {
        String type;
        String address;
        String connectionId;
        String path;

        public AddressInfo(String type, String address, String connectionId, String path) {
            this.type = type;
            this.address = address;
            this.connectionId = connectionId;
            this.path = path;
        }

        public String getType() { return type; }
        public String getAddress() { return address; }
        public String getConnectionId() { return connectionId; }
        public String getPath() { return path; }

        public String toString() {
            return String.format("AddressInfo %s [address=%s, connectionId=%s, path=%s]", type, address, connectionId, path);
        }
    }
}
