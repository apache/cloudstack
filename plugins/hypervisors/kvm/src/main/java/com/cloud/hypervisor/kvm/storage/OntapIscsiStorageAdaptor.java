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
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.libvirt.LibvirtException;

import com.cloud.agent.api.to.DiskTO;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.ProvisioningType;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;

/**
 * Serves {@link StoragePoolType#OntapiSCSI} pools:
 *
 * This must stay in the {@code com.cloud.hypervisor.kvm.storage} package: {@link KVMStoragePoolManager}
 * discovers adaptors by a Reflections scan of that package alone, and an unregistered type silently
 * falls back to {@link LibvirtStorageAdaptor} rather than failing at startup.
 */
public class OntapIscsiStorageAdaptor implements StorageAdaptor {

    protected Logger logger = LogManager.getLogger(getClass());
    private static final Map<String, KVMStoragePool> MAP_STORAGE_UUID_TO_STORAGE_POOL = new HashMap<>();
    private static final String BY_ID_WWN_PREFIX = "/dev/disk/by-id/wwn-0x";

    /**
     * A LUN WWID is NetApp's NAA prefix (type 6, OUI 00a098, vendor nibble 0) followed by the hex of
     * a 12-character serial: 32 hex digits.
     * Anchoring on the prefix is what stops this adaptor claiming another vendor's WWN device
     * when KVMStoragePoolManager.disconnectPhysicalDiskByPath scans every adaptor.
     */
    private static  final String NETAPP_NAA_PREFIX = "600a0980";
    private static final Pattern LUN_WWID = Pattern.compile(NETAPP_NAA_PREFIX + "[0-9a-f]{24}");

    private static final String SYS_BLOCK = "/sys/block";
    private static final String SYS_ISCSI_SESSION = "/sys/class/iscsi_session";
    private static final String SESSION_DIR_PREFIX = "session";

    /** iscsiadm's ISCSI_ERR_NO_OBJS_FOUND: returned by "-m session" when no session is established. */
    private static final int ISCSI_ERR_NO_OBJS_FOUND = 21;

    /** iscsiadm's ISCSI_ERR_SESS_EXISTS: returned by "--login" when already logged in (e.g. Ubuntu). */
    private static final int ISCSI_SESSION_EXISTS_CODE = 15;

    private static final int DEVICE_WAIT_TRIES = 15;
    private static final int DEVICE_WAIT_INTERVAL_MS = 1000;
    private static final int DEFAULT_ISCSI_PORT = 3260;

    @Override
    public StoragePoolType getStoragePoolType() {
        return StoragePoolType.OntapiSCSI;
    }

    @Override
    public KVMStoragePool createStoragePool(String uuid, String host, int port, String path, String userInfo,
                                            StoragePoolType storagePoolType, Map<String, String> details, boolean isPrimaryStorage) {
        IscsiAdmStoragePool storagePool = new IscsiAdmStoragePool(uuid, host, port, storagePoolType, this);

        MAP_STORAGE_UUID_TO_STORAGE_POOL.put(uuid, storagePool);

        return storagePool;
    }

    @Override
    public KVMStoragePool getStoragePool(String uuid) {
        return MAP_STORAGE_UUID_TO_STORAGE_POOL.get(uuid);
    }

    @Override
    public KVMStoragePool getStoragePool(String uuid, boolean refreshInfo) {
        return MAP_STORAGE_UUID_TO_STORAGE_POOL.get(uuid);
    }

    @Override
    public boolean deleteStoragePool(String uuid) {
        return MAP_STORAGE_UUID_TO_STORAGE_POOL.remove(uuid) != null;
    }

    @Override
    public boolean deleteStoragePool(KVMStoragePool pool) {
        return deleteStoragePool(pool.getUuid());
    }

    @Override
    public KVMPhysicalDisk getPhysicalDisk(String volumeUuid, KVMStoragePool pool) {
        String devicePath = getDeviceById(getLunWwid(volumeUuid));

        KVMPhysicalDisk physicalDisk = new KVMPhysicalDisk(devicePath, volumeUuid, pool);
        physicalDisk.setFormat(PhysicalDiskFormat.RAW);

        long deviceSize = getDeviceSize(devicePath);

        physicalDisk.setSize(deviceSize);
        physicalDisk.setVirtualSize(deviceSize);

        return physicalDisk;
    }

    @Override
    public boolean connectPhysicalDisk(String volumePath, KVMStoragePool pool, Map<String, String> details, boolean isVMMigrate) {
        final String host = pool.getSourceHost();
        final int port = pool.getSourcePort();
        final String iqn = getTargetIqn(volumePath);

        if (!createIscsiNode(host, port, iqn, volumePath)) {
            return false;
        }

        if (!applyChapCredentials(host, port, iqn, volumePath, details)) {
            return false;
        }

        if (!loginOrRescanExistingSession(iqn, host, port, volumePath)) {
            return false;
        }

        // Logging in can return before the kernel has added the device, so the disk is not usable
        // the moment iscsiadm succeeds. Waiting on a non-zero size also guards against reporting
        // success when the device never appears, which would otherwise let a caller create a plain
        // file where the LUN device was expected.
        if (!waitForDeviceToAppear(volumePath, pool)) {
            logger.warn("iSCSI device for LUN {} on target {} at {}:{} did not become available",
                    getLunWwid(volumePath), iqn, host, port);
            return false;
        }

        return true;
    }

    @Override
    public boolean disconnectPhysicalDisk(String volumePath, KVMStoragePool pool) {
        return disconnectLun(pool.getSourceHost(), pool.getSourcePort(), getTargetIqn(volumePath), getLunWwid(volumePath));
    }

    @Override
    public boolean disconnectPhysicalDisk(Map<String, String> volumeToDisconnect) {
        String host = volumeToDisconnect.get(DiskTO.STORAGE_HOST);
        String port = volumeToDisconnect.get(DiskTO.STORAGE_PORT);
        String path = volumeToDisconnect.get(DiskTO.IQN);

        if (host == null || port == null || path == null) {
            return false;
        }

        return disconnectLun(host, Integer.parseInt(port), getTargetIqn(path), getLunWwid(path));
    }

    /**
     * Claims the by-id devices this adaptor hands out.
     *
     * The target IQN and portal are not recoverable from a by-id name, so they are read back from
     * the device's iSCSI session in sysfs.
     *
     * Returning false for anything else is required by the {@link StorageAdaptor} contract:
     * {@code KVMStoragePoolManager.disconnectPhysicalDiskByPath} scans every adaptor and stops at
     * the first one that claims the path.
     */
    @Override
    public boolean disconnectPhysicalDiskByPath(String localPath) {
        if (!isOntapDevicePath(localPath)) {
            return false;
        }

        String kernelDevice = resolveKernelDevice(localPath);
        if (kernelDevice == null) {
            logger.info("Device {} is already gone, nothing to disconnect", localPath);
            return true;
        }

        Integer sessionId = findSessionId(kernelDevice);
        String iqn = sessionId == null ? null : readSessionAttribute(sessionId, "targetname");
        if (iqn == null) {
            // A real ONTAP LUN always sits under an iSCSI session. If this one does not, it is not
            // ours, so leave it alone and let the manager try the next adaptor.
            logger.debug("Device {} ({}) is not on a readable iSCSI session, not claiming it",
                    localPath, kernelDevice);
            return false;
        }

        return disconnectLun(null, 0, iqn, localPath.substring(BY_ID_WWN_PREFIX.length()));
    }

    @Override
    public KVMPhysicalDisk copyPhysicalDisk(KVMPhysicalDisk disk, String name, KVMStoragePool destPool, int timeout) {
        return copyPhysicalDisk(disk, name, destPool, timeout, null, null, null);
    }

    @Override
    public KVMPhysicalDisk copyPhysicalDisk(KVMPhysicalDisk srcDisk, String destVolumeUuid, KVMStoragePool destPool,
                                            int timeout, byte[] srcPassphrase, byte[] destPassphrase, ProvisioningType provisioningType) {
        KVMStoragePool srcPool = srcDisk.getPool();
        QemuImgFile srcFile = srcPool.getType() == StoragePoolType.RBD
                ? new QemuImgFile(KVMPhysicalDisk.RBDStringBuilder(srcPool, srcDisk.getPath()), srcDisk.getFormat())
                : new QemuImgFile(srcDisk.getPath(), srcDisk.getFormat());

        KVMPhysicalDisk destDisk = destPool.getPhysicalDisk(destVolumeUuid);
        QemuImgFile destFile = new QemuImgFile(destDisk.getPath(), destDisk.getFormat());

        try {
            new QemuImg(timeout).convert(srcFile, destFile);

            // A small template can still be sitting in the page cache when convert returns. The LUN
            // is disconnected right after a copy, so without an explicit flush that data would never
            // reach the array and the copy would be reported successful while the LUN stayed empty.
            flushToDevice(destDisk.getPath());
        } catch (QemuImgException | LibvirtException ex) {
            String msg = "Failed to copy data from " + srcDisk.getPath() + " to " + destDisk.getPath()
                    + ". The error was the following: " + ex.getMessage();
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }

        return destPool.getPhysicalDisk(destVolumeUuid);
    }

    @Override
    public boolean refresh(KVMStoragePool pool) {
        return true;
    }

    @Override
    public KVMPhysicalDisk createPhysicalDisk(String volumeUuid, KVMStoragePool pool, PhysicalDiskFormat format,
                                              ProvisioningType provisioningType, long size, byte[] passphrase) {
        throw new UnsupportedOperationException("Creating a physical disk is not supported; ONTAP LUNs are provisioned by the management server.");
    }

    @Override
    public boolean deletePhysicalDisk(String volumeUuid, KVMStoragePool pool, Storage.ImageFormat format) {
        throw new UnsupportedOperationException("Deleting a physical disk is not supported; ONTAP LUNs are removed by the management server.");
    }

    @Override
    public List<KVMPhysicalDisk> listPhysicalDisks(String storagePoolUuid, KVMStoragePool pool) {
        throw new UnsupportedOperationException("Listing disks is not supported for this configuration.");
    }

    @Override
    public KVMPhysicalDisk createDiskFromTemplate(KVMPhysicalDisk template, String name, PhysicalDiskFormat format,
                                                  ProvisioningType provisioningType, long size, KVMStoragePool destPool,
                                                  int timeout, byte[] passphrase) {
        throw new UnsupportedOperationException("Creating a disk from a template is not supported for this configuration.");
    }

    @Override
    public KVMPhysicalDisk createTemplateFromDisk(KVMPhysicalDisk disk, String name, PhysicalDiskFormat format, long size, KVMStoragePool destPool) {
        throw new UnsupportedOperationException("Creating a template from a disk is not supported for this configuration.");
    }

    @Override
    public boolean createFolder(String uuid, String path) {
        return createFolder(uuid, path, null);
    }

    @Override
    public boolean createFolder(String uuid, String path, String localPath) {
        throw new UnsupportedOperationException("A folder cannot be created in this configuration.");
    }

    @Override
    public KVMPhysicalDisk createDiskFromTemplateBacking(KVMPhysicalDisk template, String name, PhysicalDiskFormat format,
                                                         long size, KVMStoragePool destPool, int timeout, byte[] passphrase) {
        return null;
    }

    @Override
    public KVMPhysicalDisk createTemplateFromDirectDownloadFile(String templateFilePath, String destTemplatePath,
                                                                KVMStoragePool destPool, Storage.ImageFormat format, int timeout) {
        return null;
    }

    private boolean createIscsiNode(String host, int port, String iqn, String volumePath) {
        String result = runIscsiadmNodeCommand(host, port, iqn, "-o", "new");

        if (result == null) {
            logger.debug("Added iSCSI node for target {}", iqn);
            return true;
        }
        if (result.toLowerCase().contains("exists")) {
            logger.debug("iSCSI node already exists for target {}, proceeding", iqn);
            return true;
        }
        logger.warn("Failed to add iSCSI node for {}: {}", volumePath, result);
        return false;
    }

    private boolean applyChapCredentials(String host, int port, String iqn, String volumePath, Map<String, String> details) {
        if (details == null) {
            return true;
        }

        String username = details.get(DiskTO.CHAP_INITIATOR_USERNAME);
        String secret = details.get(DiskTO.CHAP_INITIATOR_SECRET);

        if (!StringUtils.isNoneBlank(username, secret)) {
            return true;
        }

        return updateNodeSetting(host, port, iqn, "node.session.auth.authmethod", "CHAP", volumePath)
                && updateNodeSetting(host, port, iqn, "node.session.auth.username", username, volumePath)
                && updateNodeSetting(host, port, iqn, "node.session.auth.password", secret, volumePath);
    }

    private boolean updateNodeSetting(String host, int port, String iqn, String name, String value, String volumePath) {
        String result = runIscsiadmNodeCommand(host, port, iqn, "--op", "update", "-n", name, "-v", value);
        if (result != null) {
            // The value is not logged: for the CHAP settings it is a credential.
            logger.warn("Failed to set {} on iSCSI target {} for {}: {}", name, iqn, volumePath, result);
            return false;
        }
        return true;
    }

    /**
     * Logs in, rescanning instead when the session was already established.
     *
     * Login is idempotent but its exit status is not portable: re-login exits 0 on some
     * distributions and ISCSI_ERR_SESS_EXISTS on others, where it also produces an error message
     * that would otherwise read as a failure. The session is therefore checked beforehand and a
     * pre-existing session treated as success.
     */
    private boolean loginOrRescanExistingSession(String iqn, String host, int port, String volumePath) {
        boolean sessionAlreadyActive = isIscsiSessionActive(iqn, host);

        Script login = new Script(true, "iscsiadm", 0, logger);
        login.add("-m", "node");
        login.add("-T", iqn);
        login.add("-p", host + ":" + port);
        login.add("--login");

        String result = login.execute();
        boolean sessionPreExisted = login.getExitValue() == ISCSI_SESSION_EXISTS_CODE || sessionAlreadyActive;

        if (sessionPreExisted) {
            logger.debug("iSCSI session for target {} at {}:{} pre-existed, rescanning", iqn, host, port);
            rescanIscsiSession(host, port, iqn);
            return true;
        }
        if (result == null) {
            logger.debug("Logged in to iSCSI target {} for {}", iqn, volumePath);
            return true;
        }
        logger.warn("Failed to log in to iSCSI target {} for {}: {}", iqn, volumePath, result);
        return false;
    }

    /**
     * Reports whether a session to this target and portal already exists.
     *
     * ISCSI_ERR_NO_OBJS_FOUND simply means no session exists, which is a normal outcome here; any
     * other non-zero exit is treated as "not confirmed active" so that login is still attempted.
     */
    private boolean isIscsiSessionActive(String iqn, String host) {
        Script sessionCmd = new Script(true, "iscsiadm", 0, logger);
        sessionCmd.add("-m", "session");

        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        sessionCmd.executeIgnoreExitValue(parser, ISCSI_ERR_NO_OBJS_FOUND);

        int exitValue = sessionCmd.getExitValue();
        if (exitValue != 0 && exitValue != ISCSI_ERR_NO_OBJS_FOUND) {
            logger.warn("Unable to determine iSCSI session state for target {} at {}: 'iscsiadm -m session' exited with {}",
                    iqn, host, exitValue);
            return false;
        }

        String sessions = parser.getLines();
        if (StringUtils.isBlank(sessions)) {
            return false;
        }
        for (String line : sessions.split("\n")) {
            if (line.contains(iqn) && line.contains(host)) {
                return true;
            }
        }
        return false;
    }

    private void rescanIscsiSession(String host, int port, String iqn) {
        String result = runIscsiadmNodeCommand(host, port, iqn, "--rescan");
        if (result != null) {
            logger.warn("iSCSI session rescan of target {} returned: {}", iqn, result);
        }
    }

    /**
     * Releases one LUN, tearing the session down only once it is the last one on the target.
     *
     * ONTAP presents all of an SVM's LUNs through a single IQN, so {@code iscsiadm --logout} would
     * drop every LUN on the session rather than just this one. While siblings remain, the LUN is
     * released by deleting its SCSI device through sysfs instead; the kernel does not remove
     * devices from a live session on its own when the array unmaps a LUN.
     *
     * @param host the portal address, or null to read it back from the device's session
     */
    private boolean disconnectLun(String host, int port, String iqn, String lunWwid) {
        String devicePath = getDeviceById(lunWwid);
        String kernelDevice = resolveKernelDevice(devicePath);

        if (kernelDevice == null) {
            logger.info("Device {} for LUN {} on target {} is already gone; nothing to disconnect", devicePath, lunWwid, iqn);
            return true;
        }

        Integer sessionId = findSessionId(kernelDevice);
        if (sessionId == null) {
            logger.warn("Unable to resolve the iSCSI session id for device {} (LUN {}, target {}); "
                    + "Removing the SCSI device only ", kernelDevice, lunWwid, iqn);
            removeScsiDevice(kernelDevice);
            return true;
        }
        if (hasOtherLunsInSession(sessionId, kernelDevice)) {
            logger.info("Skipping iSCSI logout for LUN {} on target {}: other LUNs on the same session are still "
                    + "active. Removing device {} only.", lunWwid, iqn, kernelDevice);
            removeScsiDevice(kernelDevice);
            return true;
        }

        if (host == null) {
            host = readConnectionAttribute(sessionId, "persistent_address");
            port = parsePort(readConnectionAttribute(sessionId, "persistent_port"));
        }
        if (host == null) {
            logger.warn("Unable to determine the portal of target {} even with sessionId={}; removing device {} without logging out",
                    iqn, sessionId, kernelDevice);
            removeScsiDevice(kernelDevice);
            return true;
        }

        if (!logoutAndForgetTarget(host, port, iqn, lunWwid)) {
            return false;
        }

        waitForDeviceToDisappear(devicePath);
        return true;
    }

    private boolean logoutAndForgetTarget(String host, int port, String iqn, String lunWwid) {
        String logoutResult = runIscsiadmNodeCommand(host, port, iqn, "--logout");
        if (logoutResult != null) {
            logger.warn("Failed to log out of iSCSI target {} while releasing LUN {}: {}", iqn, lunWwid, logoutResult);
            return false;
        }
        logger.debug("Logged out of iSCSI target {} while releasing LUN {}", iqn, lunWwid);

        String deleteResult = runIscsiadmNodeCommand(host, port, iqn, "-o", "delete");
        if (deleteResult != null) {
            logger.warn("Failed to delete the iSCSI node record for target {}: {}", iqn, deleteResult);
            return false;
        }
        logger.debug("Deleted the iSCSI node record for target {}", iqn);

        return true;
    }

    /** @return null on success, or the command output describing the failure */
    private String runIscsiadmNodeCommand(String host, int port, String iqn, String... operation) {
        Script command = new Script(true, "iscsiadm", 0, logger);
        command.add("-m", "node");
        command.add("-T", iqn);
        command.add("-p", host + ":" + port);
        for (String argument : operation) {
            command.add(argument);
        }
        return command.execute();
    }

    // ---------------------------------------------------------------------------------------------
    // Device and sysfs handling
    // ---------------------------------------------------------------------------------------------

    /**
     * Removes a single SCSI device from the kernel, the standard alternative to tearing down the
     * whole session. The kernel removes the device's udev aliases once it processes the delete.
     */
    private void removeScsiDevice(String kernelDevice) {
        File deleteFile = new File(SYS_BLOCK + "/" + kernelDevice + "/device/delete");
        if (!deleteFile.exists()) {
            logger.warn("No sysfs delete entry for device {}; cannot remove it", kernelDevice);
            return;
        }
        try (FileWriter writer = new FileWriter(deleteFile)) {
            writer.write("1");
            logger.info("Removed SCSI device {} via sysfs", kernelDevice);
        } catch (IOException ex) {
            logger.warn("Failed to remove SCSI device {}: {}", kernelDevice, ex.getMessage());
        }
    }

    /**
     * Reports whether the session carries any LUN other than {@code ownDevice}.
     *
     * Every SCSI disk reachable through an iSCSI session has that session in its sysfs device path,
     * so comparing session ids across {@code /sys/block} identifies the siblings without needing to
     * know any logical unit numbers.
     */
    private boolean hasOtherLunsInSession(int sessionId, String ownDevice) {
        File[] blockDevices = new File(SYS_BLOCK).listFiles();
        if (blockDevices == null) {
            return false;
        }
        for (File blockDevice : blockDevices) {
            String name = blockDevice.getName();
            if (name.equals(ownDevice)) {
                continue;
            }
            Integer otherSessionId = findSessionId(name);
            if (otherSessionId != null && otherSessionId == sessionId) {
                logger.debug("Device {} is another LUN on iSCSI session {}", name, sessionId);
                return true;
            }
        }
        return false;
    }

    /**
     * Extracts the iSCSI session id from a block device's sysfs path, which looks like
     * {@code /sys/devices/platform/host33/session13/target33:0:0/33:0:0:1/block/sdc}.
     *
     * @return the session id, or null if the device is not backed by iSCSI
     */
    private Integer findSessionId(String kernelDevice) {
        try {
            Path deviceLink = Paths.get(SYS_BLOCK, kernelDevice, "device");
            if (!Files.exists(deviceLink)) {
                return null;
            }
            for (Path element : deviceLink.toRealPath()) {
                String name = element.toString();
                if (name.startsWith(SESSION_DIR_PREFIX)) {
                    return Integer.parseInt(name.substring(SESSION_DIR_PREFIX.length()));
                }
            }
        } catch (IOException | NumberFormatException ex) {
            logger.debug("Unable to determine the iSCSI session of device {}: {}", kernelDevice, ex.getMessage());
        }
        return null;
    }

    private String readSessionAttribute(int sessionId, String attribute) {
        return readSysfsValue(Paths.get(SYS_ISCSI_SESSION, SESSION_DIR_PREFIX + sessionId, attribute));
    }

    /**
     * Reads a connection attribute of a session. A connection's own index is not guaranteed to
     * match its session's, so the connection directory is discovered under the session rather than
     * assumed to be {@code connection<sessionId>:0}.
     */
    private String readConnectionAttribute(int sessionId, String attribute) {
        Path sessionDevice = Paths.get(SYS_ISCSI_SESSION, SESSION_DIR_PREFIX + sessionId, "device");
        File[] children = sessionDevice.toFile().listFiles();
        if (children == null) {
            return null;
        }
        for (File child : children) {
            String name = child.getName();
            if (!name.startsWith("connection")) {
                continue;
            }
            String value = readSysfsValue(sessionDevice.resolve(name).resolve("iscsi_connection").resolve(name).resolve(attribute));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String readSysfsValue(Path path) {
        try {
            if (!Files.exists(path)) {
                return null;
            }
            String value = new String(Files.readAllBytes(path)).trim();
            return value.isEmpty() ? null : value;
        } catch (IOException ex) {
            logger.debug("Unable to read {}: {}", path, ex.getMessage());
            return null;
        }
    }

    private int parsePort(String port) {
        try {
            return port != null ? Integer.parseInt(port) : DEFAULT_ISCSI_PORT;
        } catch (NumberFormatException ex) {
            return DEFAULT_ISCSI_PORT;
        }
    }

    private String resolveKernelDevice(String devicePath) {
        try {
            Path link = Paths.get(devicePath);
            if (!Files.exists(link)) {
                return null;
            }
            return link.toRealPath().getFileName().toString();
        } catch (IOException ex) {
            logger.debug("Unable to resolve {}: {}", devicePath, ex.getMessage());
            return null;
        }
    }

    private boolean waitForDeviceToAppear(String volumePath, KVMStoragePool pool) {
        for (int attempt = 0; attempt < DEVICE_WAIT_TRIES; attempt++) {
            if (getPhysicalDisk(volumePath, pool).getSize() > 0) {
                return true;
            }
            if (!sleepBetweenAttempts()) {
                return false;
            }
        }
        return getPhysicalDisk(volumePath, pool).getSize() > 0;
    }

    private void waitForDeviceToDisappear(String devicePath) {
        for (int attempt = 0; attempt < DEVICE_WAIT_TRIES && getDeviceSize(devicePath) > 0; attempt++) {
            if (!sleepBetweenAttempts()) {
                return;
            }
        }
    }

    private boolean sleepBetweenAttempts() {
        try {
            Thread.sleep(DEVICE_WAIT_INTERVAL_MS);
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * @return the device size in bytes, or 0 if the device is absent or not a block device
     */
    private long getDeviceSize(String devicePath) {
        Path path = Paths.get(devicePath);

        if (!Files.exists(path)) {
            logger.debug("Device does not exist yet: {}", devicePath);
            return 0L;
        }
        if (Files.isRegularFile(path)) {
            // A plain file here means something wrote to the device name before the LUN appeared;
            // treating it as a disk would silently back a volume with local storage.
            logger.warn("Found a regular file at {} where a block device was expected; it must be removed manually", devicePath);
            return 0L;
        }

        Script command = new Script(true, "blockdev", 0, logger);
        command.add("--getsize64", devicePath);

        OutputInterpreter.OneLineParser parser = new OutputInterpreter.OneLineParser();
        String result = command.execute(parser);

        if (result != null) {
            logger.warn("Unable to get the size of device {}: {}", devicePath, result);
            return 0L;
        }

        try {
            return Long.parseLong(parser.getLine().trim());
        } catch (NumberFormatException | NullPointerException ex) {
            logger.warn("Unable to parse the size of device {}", devicePath);
            return 0L;
        }
    }

    private void flushToDevice(String devicePath) {
        Script flush = new Script(true, "blockdev", 0, logger);
        flush.add("--flushbufs", devicePath);
        String flushResult = flush.execute();
        if (flushResult != null) {
            logger.warn("blockdev --flushbufs on {} returned: {}", devicePath, flushResult);
        }
        new Script(true, "sync", 0, logger).execute();
        logger.debug("Flushed buffers to {}", devicePath);
    }

    // ---------------------------------------------------------------------------------------------
    // Volume path handling
    // ---------------------------------------------------------------------------------------------

    private boolean isOntapDevicePath(String localPath) {
        return localPath != null
                && localPath.startsWith(BY_ID_WWN_PREFIX)
                && LUN_WWID.matcher(localPath.substring(BY_ID_WWN_PREFIX.length())).matches();
    }

    private String getDeviceById(String lunWwid) {
        return BY_ID_WWN_PREFIX + lunWwid;
    }

    private String getTargetIqn(String volumePath) {
        return getPathComponent(volumePath, 1);
    }

    private String getLunWwid(String volumePath) {
        return getPathComponent(volumePath, 2);
    }

    /**
     * Splits a {@code /<targetIQN>/<lunWwid>} volume path.
     *
     * The WWID occupies the component a logical unit number would hold for other iSCSI vendors, so
     * the path keeps the two-component shape every hypervisor resource expects. That is also why
     * the path carries the WWID rather than the ONTAP serial it encodes: serials contain '/'.
     */
    private String getPathComponent(String volumePath, int index) {
        String[] components = volumePath == null ? new String[0] : volumePath.split("/");

        if (components.length != 3) {
            String message = "Wrong format for ONTAP iSCSI path: " + volumePath
                    + ". It should be formatted as '/targetIQN/lunWwid'.";
            logger.warn(message);
            throw new CloudRuntimeException(message);
        }

        return components[index].trim();
    }
}
