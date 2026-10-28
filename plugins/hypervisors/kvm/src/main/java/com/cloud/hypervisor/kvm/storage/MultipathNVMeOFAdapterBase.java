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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
import com.cloud.utils.storage.TemplateDownloaderUtil;
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
    static final Map<String, KVMStoragePool> MapStorageUuidToStoragePool = new ConcurrentHashMap<>();

    static final int DEFAULT_DISK_WAIT_SECS = 240;
    static final long NS_RESCAN_TIMEOUT_SECS = 5;
    private static final long POLL_INTERVAL_MS = 2000;
    private static final long RESCAN_INTERVAL_MS = 10_000;
    private static final long RESIZE_SETTLE_TIMEOUT_MS = 30_000;

    @Override
    public KVMStoragePool getStoragePool(String uuid) {
        // Dummy pool - adapters that dispatch per-volume don't need
        // connectivity information on the pool itself. Use computeIfAbsent
        // so concurrent callers do not race to create duplicate pool objects.
        return MapStorageUuidToStoragePool.computeIfAbsent(uuid, u -> new MultipathNVMeOFPool(u, this));
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
        if (inPath == null) {
            throw new CloudRuntimeException("Cannot parse null volume path");
        }
        String type = null;
        String address = null;
        String connectionId = null;
        String path = null;
        String hostname = resolveHostnameShort();
        String hostnameFq = resolveHostnameFq();
        String[] parts = inPath.split(";");
        for (String part : parts) {
            // Cap the split at 2 so values containing '=' (e.g. base64) are not silently discarded.
            String[] pair = part.split("=", 2);
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
                try {
                    waitSecs = Integer.parseInt(waitTime);
                } catch (NumberFormatException e) {
                    LOGGER.warn("Ignoring non-numeric " + com.cloud.storage.StorageManager.STORAGE_POOL_DISK_WAIT.toString()
                            + "=[" + waitTime + "] on pool " + pool.getUuid() + ", falling back to default "
                            + DEFAULT_DISK_WAIT_SECS + "s");
                }
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
        long lastRescan = 0;
        while (System.currentTimeMillis() < deadline) {
            if (dev.exists() && isConnected(address.getPath())) {
                long size = getPhysicalDiskSize(address.getPath());
                if (size > 0) {
                    LOGGER.debug("Found NVMe namespace at " + address.getPath());
                    return true;
                }
            }
            // Throttle rescanAllControllers(): spawning one nvme ns-rescan per controller every
            // 2s can be expensive on hosts with many controllers. RESCAN_INTERVAL_MS caps the
            // rate; the first iteration still rescans immediately because lastRescan starts at 0.
            long now = System.currentTimeMillis();
            if (now - lastRescan >= RESCAN_INTERVAL_MS) {
                rescanAllControllers();
                lastRescan = now;
            }
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
                if (!p.waitFor(NS_RESCAN_TIMEOUT_SECS, TimeUnit.SECONDS)) {
                    // Kill runaway nvme-cli invocations so they do not pile
                    // up under the JVM on every poll iteration while we
                    // are still waiting for the namespace to appear.
                    LOGGER.debug("nvme ns-rescan /dev/" + ctrl.getName()
                            + " did not complete within " + NS_RESCAN_TIMEOUT_SECS
                            + "s; terminating");
                    p.destroyForcibly();
                }
            }
        } catch (Exception e) {
            LOGGER.debug("nvme ns-rescan attempt failed: " + e.getMessage());
        }
    }

    @Override
    public boolean disconnectPhysicalDisk(String volumePath, KVMStoragePool pool) {
        // NVMe-oF: the kernel drops the namespace as soon as the target removes
        // this host's connection, so there is no host-side map to tear down the way
        // Fibre Channel must flush its device-mapper entry.
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
        // Namespaces are created and destroyed by the storage provider, never from the
        // host. Report "not handled here" instead of throwing, so a caller on a cleanup
        // path behaves the same as it does on the Fibre Channel adapter.
        LOGGER.info("deletePhysicalDisk({}) not handled by the NVMe-oF adapter; the storage provider owns namespace deletion", uuid);
        return false;
    }

    @Override
    public KVMPhysicalDisk createPhysicalDisk(String name, KVMStoragePool pool, PhysicalDiskFormat format,
            Storage.ProvisioningType provisioningType, long size, byte[] passphrase) {
        throw new UnsupportedOperationException("Unimplemented method 'createPhysicalDisk'");
    }

    @Override
    public KVMPhysicalDisk createTemplateFromDisk(KVMPhysicalDisk disk, String name, QemuImg.PhysicalDiskFormat format, long size, KVMStoragePool destPool) {
        LOGGER.info("createTemplateFromDisk not supported on NVMe-oF pools");
        return null;
    }

    @Override
    public List<KVMPhysicalDisk> listPhysicalDisks(String storagePoolUuid, KVMStoragePool pool) {
        // The array owns the namespace inventory; it is not enumerable from the host.
        LOGGER.info("listPhysicalDisks not supported on NVMe-oF pool {}", storagePoolUuid);
        return null;
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

    /**
     * Write a directly-downloaded template onto a namespace on this pool.
     *
     * The two path arguments are different kinds of thing, which is worth being explicit
     * about: {@code templateFilePath} is a plain local file produced by the direct-download
     * helper, while {@code destTemplatePath} is a managed volume path of the form
     * {@code type=NVMETCP;address=...}. Only the destination may be resolved through
     * {@link KVMStoragePool#getPhysicalDisk(String)}; passing the local file through it
     * would hand a file name to {@link #parseAndValidatePath(String)} and fail. The caller
     * has already issued connectPhysicalDisk() for the destination, so the namespace is
     * expected to be present.
     *
     * The template is written as QCOW2 onto the raw namespace rather than as RAW. That
     * matches the ScaleIO adaptor, is consistent with the QCOW2 format the template is
     * registered with, and is what lets the Qcow2Inspector check the caller runs on the
     * returned path succeed.
     *
     * Note what the caller does with the returned disk: KVMStorageProcessor puts
     * {@code disk.getName()} into the DirectDownloadAnswer, and that becomes the template's
     * install path and later its external name, which the provider interpolates into array
     * REST calls. {@link #getPhysicalDisk(String, KVMStoragePool)} names a disk
     * {@code AddressInfo.toString()}, which contains spaces and brackets and would produce
     * a name that cannot be placed in a URI. So the disk handed back here is named with the
     * managed volume path we were given, matching what a volume records.
     */
    @Override
    public KVMPhysicalDisk createTemplateFromDirectDownloadFile(String templateFilePath, String destTemplatePath, KVMStoragePool destPool, Storage.ImageFormat format, int timeout) {
        if (StringUtils.isAnyEmpty(templateFilePath, destTemplatePath) || destPool == null) {
            throw new CloudRuntimeException("Unable to create a template from a direct download file on an NVMe-oF pool: "
                    + "template file path, destination template path or destination pool not specified");
        }

        if (!Storage.ImageFormat.QCOW2.equals(format) && !Storage.ImageFormat.RAW.equals(format)) {
            throw new CloudRuntimeException("Unsupported direct download template format for NVMe-oF pools: " + format
                    + "; expected " + Storage.ImageFormat.QCOW2 + " or " + Storage.ImageFormat.RAW);
        }

        File sourceFile = new File(templateFilePath);
        if (!sourceFile.exists()) {
            throw new CloudRuntimeException("Direct download template file " + templateFilePath + " does not exist on this host");
        }

        LOGGER.debug("Creating a template on NVMe-oF pool [{}] from direct download file [{}] into [{}], format [{}]",
                destPool.getUuid(), templateFilePath, destTemplatePath, format);

        String srcTemplateFilePath = templateFilePath;
        KVMPhysicalDisk destDisk;
        try {
            destDisk = destPool.getPhysicalDisk(destTemplatePath);
            if (destDisk == null || StringUtils.isEmpty(destDisk.getPath())) {
                throw new CloudRuntimeException("Unable to resolve the NVMe namespace for destination template path ["
                        + destTemplatePath + "] on pool [" + destPool.getUuid() + "]");
            }

            // Direct-download templates are commonly published compressed.
            if (TemplateDownloaderUtil.isTemplateExtractable(templateFilePath)) {
                srcTemplateFilePath = sourceFile.getParent() + "/" + UUID.randomUUID().toString();
                LOGGER.debug("Extracting downloaded template [{}] to [{}]", templateFilePath, srcTemplateFilePath);
                Script.runSimpleBashScript(TemplateDownloaderUtil.getExtractCommandForDownloadedFile(templateFilePath, srcTemplateFilePath));
                Script.runSimpleBashScript("rm -f " + templateFilePath);
            }

            QemuImg.PhysicalDiskFormat srcFormat = Storage.ImageFormat.RAW.equals(format)
                    ? QemuImg.PhysicalDiskFormat.RAW : QemuImg.PhysicalDiskFormat.QCOW2;

            QemuImg qemu = new QemuImg(timeout);
            QemuImgFile srcFile = new QemuImgFile(srcTemplateFilePath, srcFormat);
            // Populates the virtual size, and fails early if the file is unreadable or is
            // not in the format the template claims to be.
            qemu.info(srcFile);

            long namespaceSize = getPhysicalDiskSize(destDisk.getPath());
            if (namespaceSize > 0 && srcFile.getSize() > namespaceSize) {
                throw new CloudRuntimeException("Direct download template needs " + srcFile.getSize()
                        + " bytes but the NVMe namespace at " + destDisk.getPath() + " is only " + namespaceSize + " bytes");
            }

            QemuImgFile destFile = new QemuImgFile(destDisk.getPath(), QemuImg.PhysicalDiskFormat.QCOW2);
            destFile.setSize(srcFile.getSize());

            LOGGER.debug("Converting [{}] onto NVMe namespace [{}]", srcFile.getFileName(), destDisk.getPath());
            qemu.create(destFile);
            qemu.convert(srcFile, destFile);

            KVMPhysicalDisk template = new KVMPhysicalDisk(destDisk.getPath(), destTemplatePath, destPool);
            template.setFormat(QemuImg.PhysicalDiskFormat.QCOW2);
            template.setVirtualSize(srcFile.getSize());
            template.setSize(srcFile.getSize());
            destDisk = template;
            LOGGER.info("Wrote direct download template onto NVMe namespace [{}] on pool [{}]",
                    destDisk.getPath(), destPool.getUuid());
        } catch (QemuImgException | LibvirtException e) {
            throw new CloudRuntimeException("Failed to write the direct download template [" + templateFilePath
                    + "] onto the NVMe namespace for [" + destTemplatePath + "] on pool [" + destPool.getUuid()
                    + "]: " + e.getMessage(), e);
        } finally {
            // Only remove what we extracted; the original download belongs to the caller.
            if (!srcTemplateFilePath.equals(templateFilePath)) {
                Script.runSimpleBashScript("rm -f " + srcTemplateFilePath);
            }
        }

        return destDisk;
    }

    @Override
    public boolean refresh(KVMStoragePool pool) {
        return true;
    }

    @Override
    public boolean createFolder(String uuid, String path) {
        return createFolder(uuid, path, null);
    }

    @Override
    public boolean createFolder(String uuid, String path, String localPath) {
        // Block storage has no directory structure to create. Succeed rather than
        // throw, matching the Fibre Channel adapter.
        LOGGER.info("createFolder({}, {}, {}) is a no-op on NVMe-oF pools", uuid, path, localPath);
        return true;
    }

    /**
     * Host-side half of a volume resize. The storage provider has already grown the
     * namespace on the array by the time we get here, so all that remains is to make
     * the new capacity visible locally and tell a running guest about it.
     *
     * Unlike the SCSI/FC path there is no device-mapper map to grow: the kernel picks
     * up the new namespace size either from the target's asynchronous event
     * notification or from an explicit {@code nvme ns-rescan}, which we issue here
     * rather than waiting for the AEN.
     */
    public void resize(String path, String vmName, long newSize) {
        AddressInfo address = parseAndValidatePath(path);
        if (address == null || address.getPath() == null) {
            throw new CloudRuntimeException("Unable to resize NVMe-oF volume, could not derive a device path from [" + path + "]");
        }

        LOGGER.debug("Resizing NVMe-oF volume " + address.getPath() + " to " + newSize + " bytes for VM " + vmName);

        rescanAllControllers();

        long observed = waitForNamespaceSize(address.getPath(), newSize);
        if (observed < newSize) {
            // Not fatal: the array has already been grown, and the kernel may still
            // catch up via an AEN. Surface it rather than failing the operation, so
            // the management server does not roll back a resize that did happen.
            LOGGER.warn("NVMe namespace " + address.getPath() + " still reports " + observed
                    + " bytes after rescan, expected at least " + newSize
                    + "; the guest may not observe the new size until the next rescan");
        }

        notifyGuestOfResize(address.getPath(), vmName, newSize, address.getAddress());
    }

    /**
     * Poll the block device until it reports at least {@code expectedSize}, since
     * ns-rescan and AEN processing are asynchronous.
     *
     * @return the last size observed, which may be smaller than expected on timeout.
     */
    private long waitForNamespaceSize(String devicePath, long expectedSize) {
        long deadline = System.currentTimeMillis() + RESIZE_SETTLE_TIMEOUT_MS;
        long observed = getPhysicalDiskSize(devicePath);
        while (observed < expectedSize && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
            rescanAllControllers();
            observed = getPhysicalDiskSize(devicePath);
        }
        return observed;
    }

    /**
     * Ask libvirt to re-read the size of the guest's block device, so a running VM
     * sees the extra capacity without a reboot. A stopped VM needs nothing here: it
     * picks up the new size when the disk is next attached.
     */
    private void notifyGuestOfResize(String devicePath, String vmName, long newSize, String eui) {
        if (StringUtils.isEmpty(vmName)) {
            LOGGER.debug("No VM name supplied for resize of " + devicePath + "; skipping guest notification");
            return;
        }

        if (!isVmRunning(vmName)) {
            LOGGER.debug("VM " + vmName + " is not running; skipping guest notification for " + devicePath);
            return;
        }

        String target = findDomainDiskTarget(vmName, devicePath, eui);
        if (target == null) {
            LOGGER.warn("Could not find a disk target for " + devicePath + " in domain " + vmName
                    + "; skipping guest notification");
            return;
        }

        // virsh blockresize takes the new size in KiB.
        Script cmd = new Script("virsh", LOGGER);
        cmd.add("blockresize");
        cmd.add("--path", target);
        cmd.add("--size", String.valueOf(newSize / 1024L));
        cmd.add(vmName);
        String result = cmd.execute();
        if (result != null) {
            LOGGER.warn("virsh blockresize of " + target + " on " + vmName + " failed: " + result);
        } else {
            LOGGER.info("Notified " + vmName + " of new size " + newSize + " bytes for " + target);
        }
    }

    private boolean isVmRunning(String vmName) {
        Script cmd = new Script("virsh", LOGGER);
        cmd.add("domstate", vmName);
        OutputInterpreter.OneLineParser parser = new OutputInterpreter.OneLineParser();
        String result = cmd.execute(parser);
        return result == null && parser.getLine() != null && parser.getLine().trim().startsWith("running");
    }

    /**
     * Resolve the domain-local disk target (vda, vdb, ...) backing {@code devicePath}.
     *
     * libvirt reports the source as it was configured, but may instead surface a
     * canonicalised /dev/nvmeXnY in place of the /dev/disk/by-id symlink we attached,
     * so accept either form, falling back to matching the bare EUI.
     */
    private String findDomainDiskTarget(String vmName, String devicePath, String eui) {
        Script cmd = new Script("virsh", LOGGER);
        cmd.add("domblklist", vmName);
        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        String result = cmd.execute(parser);
        if (result != null || parser.getLines() == null) {
            return null;
        }
        String canonical = resolveCanonicalPath(devicePath);
        for (String line : parser.getLines().split("\\R")) {
            String[] cols = line.trim().split("\\s+");
            if (cols.length < 2) {
                continue;
            }
            String source = cols[1];
            if (source.equals(devicePath)
                    || (canonical != null && source.equals(canonical))
                    || (StringUtils.isNotEmpty(eui) && source.toLowerCase().contains(eui.toLowerCase()))) {
                return cols[0];
            }
        }
        return null;
    }

    /** Resolve a /dev/disk/by-id symlink to its /dev/nvmeXnY target, or null. */
    private String resolveCanonicalPath(String devicePath) {
        try {
            return new File(devicePath).getCanonicalPath();
        } catch (Exception e) {
            LOGGER.debug("Could not canonicalise " + devicePath + ": " + e.getMessage());
            return null;
        }
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
