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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat;
import org.apache.cloudstack.utils.qemu.QemuImgFile;

import com.cloud.storage.Storage;
import com.cloud.storage.StorageManager;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.Duration;

public abstract class MultipathSCSIAdapterBase implements StorageAdaptor {
    protected static Logger LOGGER = LogManager.getLogger(MultipathSCSIAdapterBase.class);
    static final Map<String, KVMStoragePool> MapStorageUuidToStoragePool = new HashMap<>();

    /**
     * A lock to avoid any possiblity of multiple requests for a scan
     */
    static byte[] CLEANUP_LOCK = new byte[0];

    /**
     * List of supported OUI's (needed for path-based cleanup logic on disconnects after live migrations)
     */
    static String[] SUPPORTED_OUI_LIST = {
        "0002ac", // HPE Primera 3PAR
        "24a937"  // Pure Flasharray
    };

    /**
     * Property keys and defaults
     */
    static final Property<Integer> CLEANUP_FREQUENCY_SECS = new Property<Integer>("multimap.cleanup.frequency.secs", 60);
    static final Property<Integer> CLEANUP_TIMEOUT_SECS = new Property<Integer>("multimap.cleanup.timeout.secs", 4);
    static final Property<Boolean> CLEANUP_ENABLED = new Property<Boolean>("multimap.cleanup.enabled", true);
    static final Property<String> CLEANUP_SCRIPT = new Property<String>("multimap.cleanup.script", "cleanStaleMaps.sh");
    static final Property<String> CONNECT_SCRIPT = new Property<String>("multimap.connect.script", "connectVolume.sh");
    static final Property<String> COPY_SCRIPT = new Property<String>("multimap.copy.script", "copyVolume.sh");
    static final Property<String> DISCONNECT_SCRIPT = new Property<String>("multimap.disconnect.script", "disconnectVolume.sh");
    static final Property<String> RESIZE_SCRIPT = new Property<String>("multimap.resize.script", "resizeVolume.sh");
    static final Property<Integer> DISK_WAIT_SECS = new Property<Integer>("multimap.disk.wait.secs", 240);
    static final Property<String> STORAGE_SCRIPTS_DIR = new Property<String>("multimap.storage.scripts.dir", "scripts/storage/multipath");

    static Timer cleanupTimer = new Timer();
    private static int cleanupTimeoutSecs = CLEANUP_TIMEOUT_SECS.getFinalValue();
    private static String connectScript = CONNECT_SCRIPT.getFinalValue();
    private static String disconnectScript = DISCONNECT_SCRIPT.getFinalValue();
    private static String cleanupScript = CLEANUP_SCRIPT.getFinalValue();
    private static String resizeScript = RESIZE_SCRIPT.getFinalValue();
    private static String copyScript = COPY_SCRIPT.getFinalValue();
    private static int diskWaitTimeSecs = DISK_WAIT_SECS.getFinalValue();

    /**
     * Initialize static program-wide configurations and background jobs
     */
    static {

        long cleanupFrequency = CLEANUP_FREQUENCY_SECS.getFinalValue() * 1000;
        boolean cleanupEnabled = CLEANUP_ENABLED.getFinalValue();


        connectScript = Script.findScript(STORAGE_SCRIPTS_DIR.getFinalValue(), connectScript);
        if (connectScript == null) {
            throw new Error("Unable to find the connectVolume.sh script");
        }

        disconnectScript = Script.findScript(STORAGE_SCRIPTS_DIR.getFinalValue(), disconnectScript);
        if (disconnectScript == null) {
            throw new Error("Unable to find the disconnectVolume.sh script");
        }

        copyScript = Script.findScript(STORAGE_SCRIPTS_DIR.getFinalValue(), copyScript);
        if (copyScript == null) {
            throw new Error("Unable to find the copyVolume.sh script");
        }

        resizeScript = Script.findScript(STORAGE_SCRIPTS_DIR.getFinalValue(), resizeScript);

        if (cleanupEnabled) {
            cleanupScript = Script.findScript(STORAGE_SCRIPTS_DIR.getFinalValue(), cleanupScript);
            if (cleanupScript == null) {
                throw new Error("Unable to find the cleanStaleMaps.sh script and " + CLEANUP_ENABLED.getName() + " is true");
            }

            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    try {
                        MultipathSCSIAdapterBase.cleanupStaleMaps();
                    } catch (Throwable e) {
                        LOGGER.warn("Error running stale multipath map cleanup", e);
                    }
                }
            };

            cleanupTimer = new Timer("MultipathMapCleanupJob");
            cleanupTimer.scheduleAtFixedRate(task, 0, cleanupFrequency);
        }
    }

    @Override
    public KVMStoragePool getStoragePool(String uuid, boolean refreshInfo) {
        return getStoragePool(uuid);
    }

    public abstract String getName();

    public abstract boolean isStoragePoolTypeSupported(Storage.StoragePoolType type);

    public abstract AddressInfo parseAndValidatePath(String path);

    @Override
    public KVMPhysicalDisk getPhysicalDisk(String volumePath, KVMStoragePool pool) {
        LOGGER.debug(String.format("getPhysicalDisk(volumePath,pool) called with args (%s,%s)", volumePath, pool));

        if (StringUtils.isEmpty(volumePath) || pool == null) {
            LOGGER.error("Unable to get physical disk, volume path or pool not specified");
            return null;
        }

        // we expect WWN values in the volumePath so need to convert it to an actual physical path
        AddressInfo address = parseAndValidatePath(volumePath);
        return getPhysicalDisk(address, pool);
    }

    private KVMPhysicalDisk getPhysicalDisk(AddressInfo address, KVMStoragePool pool) {
        LOGGER.debug(String.format("getPhysicalDisk(addressInfo,pool) called with args (%s,%s)", address.getPath(), pool));
        KVMPhysicalDisk disk = new KVMPhysicalDisk(address.getPath(), address.toString(), pool);
        disk.setFormat(QemuImg.PhysicalDiskFormat.RAW);

        long diskSize = getPhysicalDiskSize(address.getPath());
        disk.setSize(diskSize);
        disk.setVirtualSize(diskSize);
        LOGGER.debug("Physical disk " + disk.getPath() + " with format " + disk.getFormat() + " and size " + disk.getSize() + " provided");
        return disk;
    }

    @Override
    public KVMStoragePool createStoragePool(String uuid, String host, int port, String path, String userInfo, Storage.StoragePoolType type, Map<String, String> details) {
        LOGGER.info(String.format("createStoragePool(uuid,host,port,path,type) called with args (%s, %s, %s, %s, %s)", uuid, host, ""+port, path, type));
        MultipathSCSIPool storagePool = new MultipathSCSIPool(uuid, host, port, path, type, details, this);
        MapStorageUuidToStoragePool.put(uuid, storagePool);
        return storagePool;
    }

    @Override
    public boolean deleteStoragePool(String uuid) {
        return MapStorageUuidToStoragePool.remove(uuid) != null;
    }

   @Override
    public boolean connectPhysicalDisk(String volumePath, KVMStoragePool pool, Map<String, String> details) {
        LOGGER.info("connectPhysicalDisk called for [" + volumePath + "]");

        if (StringUtils.isEmpty(volumePath)) {
            LOGGER.error("Unable to connect physical disk due to insufficient data - volume path is undefined");
            return false;
        }

        if (pool == null) {
            LOGGER.error("Unable to connect physical disk due to insufficient data - pool is not set");
            return false;
        }

        // we expect WWN values in the volumePath so need to convert it to an actual physical path
        AddressInfo address = this.parseAndValidatePath(volumePath);

        // validate we have a connection id - we can't proceed without that
        if (address.getConnectionId() == null) {
            LOGGER.error("Unable to connect volume with address [" + address.getPath() + "] of the storage pool: " + pool.getUuid() + " - connection id is not set in provided path");
            return false;
        }

        int waitTimeInSec = diskWaitTimeSecs;
        if (details != null && details.containsKey(StorageManager.STORAGE_POOL_DISK_WAIT.toString())) {
            String waitTime = details.get(StorageManager.STORAGE_POOL_DISK_WAIT.toString());
            if (StringUtils.isNotEmpty(waitTime)) {
                waitTimeInSec = Integer.valueOf(waitTime).intValue();
            }
        }
        return waitForDiskToBecomeAvailable(address, pool, waitTimeInSec);
    }

    @Override
    public boolean disconnectPhysicalDisk(String volumePath, KVMStoragePool pool) {
        if (LOGGER.isDebugEnabled()) LOGGER.debug(String.format("disconnectPhysicalDisk(volumePath,pool) called with args (%s, %s) START", volumePath, pool.getUuid()));
        AddressInfo address = this.parseAndValidatePath(volumePath);
        if (address.getAddress() == null) {
            if (LOGGER.isDebugEnabled()) LOGGER.debug(String.format("disconnectPhysicalDisk(volumePath,pool) returning FALSE, volume path has no address field", volumePath, pool.getUuid()));
            return false;
        }
        ScriptResult result = runScript(disconnectScript, 60000L, address.getAddress().toLowerCase());

        if (result.getExitCode() != 0) {
            LOGGER.warn(String.format("Disconnect failed for path [%s] with return code [%s]", address.getAddress().toLowerCase(), result.getExitCode()));
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("multipath flush output: " + result.getResult());
            LOGGER.debug(String.format("disconnectPhysicalDisk(volumePath,pool) called with args (%s, %s) COMPLETE [rc=%s]", volumePath, pool.getUuid(), result.getResult()));
        }

        return (result.getExitCode() == 0);
    }

    @Override
    public boolean disconnectPhysicalDisk(Map<String, String> volumeToDisconnect) {
        LOGGER.debug(String.format("disconnectPhysicalDisk(volumeToDisconnect) called with arg bag [not implemented]:") + " " + volumeToDisconnect);
        return false;
    }

    @Override
    public boolean disconnectPhysicalDiskByPath(String localPath) {
        if (localPath == null) {
            return false;
        }
        if (LOGGER.isDebugEnabled()) LOGGER.debug(String.format("disconnectPhysicalDiskByPath(localPath) called with args (%s) START", localPath));
        if (localPath.startsWith("/dev/mapper/")) {
            String multipathName = localPath.replace("/dev/mapper/3", "");
            // this ensures we only disconnect multipath devices supported by this driver
            for (String oui: SUPPORTED_OUI_LIST) {
                if (multipathName.length() > 1 && multipathName.substring(2).startsWith(oui)) {
                    ScriptResult result = runScript(disconnectScript, 60000L, multipathName);
                    if (result.getExitCode() != 0) {
                        LOGGER.warn(String.format("Disconnect failed for path [%s] with return code [%s]", multipathName, result.getExitCode()));
                    }
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("multipath flush output: " + result.getResult());
                        LOGGER.debug(String.format("disconnectPhysicalDiskByPath(localPath) called with args (%s) COMPLETE [rc=%s]", localPath, result.getExitCode()));
                    }
                    return (result.getExitCode() == 0);
                }
            }
        }
        if (LOGGER.isDebugEnabled()) LOGGER.debug(String.format("disconnectPhysicalDiskByPath(localPath) returning FALSE, volume path is not a multipath volume: %s", localPath));
        return false;
    }

    @Override
    public boolean deletePhysicalDisk(String uuid, KVMStoragePool pool, Storage.ImageFormat format) {
        return false;
    }

    @Override
    public KVMPhysicalDisk createTemplateFromDisk(KVMPhysicalDisk disk, String name, QemuImg.PhysicalDiskFormat format, long size, KVMStoragePool destPool) {
        LOGGER.info(String.format("createTemplateFromDisk(disk,name,format,size,destPool) called with args (%s, %s, %s, %s, %s) [not implemented]", disk.getPath(), name, format.toString(), ""+size, destPool.getUuid()));
        return null;
    }

    @Override
    public List<KVMPhysicalDisk> listPhysicalDisks(String storagePoolUuid, KVMStoragePool pool) {
        LOGGER.info(String.format("listPhysicalDisks(uuid,pool) called with args (%s, %s) [not implemented]", storagePoolUuid, pool.getUuid()));
        return null;
    }

    @Override
    public KVMPhysicalDisk copyPhysicalDisk(KVMPhysicalDisk disk, String name, KVMStoragePool destPool, int timeout) {
        return copyPhysicalDisk(disk, name, destPool, timeout, null, null, null);
    }

    @Override
    public boolean refresh(KVMStoragePool pool) {
        LOGGER.info(String.format("refresh(pool) called with args (%s)", pool.getUuid()));
        return true;
    }

    @Override
    public boolean deleteStoragePool(KVMStoragePool pool) {
        LOGGER.info(String.format("deleteStroagePool(pool) called with args (%s)", pool.getUuid()));
        return deleteStoragePool(pool.getUuid());
    }

    @Override
    public boolean createFolder(String uuid, String path) {
        LOGGER.info(String.format("createFolder(uuid,path) called with args (%s, %s) [not implemented]", uuid, path));
        return createFolder(uuid, path, null);
    }

    @Override
    public boolean createFolder(String uuid, String path, String localPath) {
        LOGGER.info(String.format("createFolder(uuid,path,localPath) called with args (%s, %s, %s) [not implemented]", uuid, path, localPath));
        return true;
    }


    @Override
    public KVMPhysicalDisk createTemplateFromDirectDownloadFile(String templateFilePath, String destTemplatePath, KVMStoragePool destPool, Storage.ImageFormat format, int timeout) {
        if (StringUtils.isAnyEmpty(templateFilePath, destTemplatePath) || destPool == null) {
            LOGGER.error("Unable to create template from direct download template file due to insufficient data");
            throw new CloudRuntimeException("Unable to create template from direct download template file due to insufficient data");
        }

        LOGGER.debug("Create template from direct download template - file path: " + templateFilePath + ", dest path: " + destTemplatePath + ", format: " + format.toString());

        File sourceFile = new File(templateFilePath);
        if (!sourceFile.exists()) {
            throw new CloudRuntimeException("Direct download template file " + templateFilePath + " does not exist on this host");
        }

        KVMPhysicalDisk sourceDisk = destPool.getPhysicalDisk(templateFilePath);
        return copyPhysicalDisk(sourceDisk, destTemplatePath, destPool, timeout, null, null,  Storage.ProvisioningType.THIN);
    }

    @Override
    public KVMPhysicalDisk copyPhysicalDisk(KVMPhysicalDisk disk, String name, KVMStoragePool destPool, int timeout,
            byte[] srcPassphrase, byte[] dstPassphrase, Storage.ProvisioningType provisioningType) {
        if (StringUtils.isEmpty(name) || disk == null || destPool == null) {
            LOGGER.error("Unable to copy physical disk due to insufficient data");
            throw new CloudRuntimeException("Unable to copy physical disk due to insufficient data");
        }

        LOGGER.info("Copying FROM source physical disk " + disk.getPath() + ", size: " + disk.getSize() + ", virtualsize: " + disk.getVirtualSize()+ ", format: " + disk.getFormat());

        KVMPhysicalDisk destDisk = destPool.getPhysicalDisk(name);
        if (destDisk == null) {
            LOGGER.error("Failed to find the disk: " + name + " of the storage pool: " + destPool.getUuid());
            throw new CloudRuntimeException("Failed to find the disk: " + name + " of the storage pool: " + destPool.getUuid());
        }

        if (srcPassphrase != null || dstPassphrase != null) {
            throw new CloudRuntimeException("Storage provider does not support user-space encrypted source or destination volumes");
        }

        destDisk.setFormat(QemuImg.PhysicalDiskFormat.RAW);
        destDisk.setVirtualSize(disk.getVirtualSize());
        destDisk.setSize(disk.getSize());

        LOGGER.info("Copying TO destination physical disk " + destDisk.getPath() + ", size: " + destDisk.getSize() + ", virtualsize: " + destDisk.getVirtualSize()+ ", format: " + destDisk.getFormat());
        QemuImgFile srcFile = new QemuImgFile(disk.getPath(), disk.getFormat());
        QemuImgFile destFile = new QemuImgFile(destDisk.getPath(), destDisk.getFormat());

        LOGGER.debug("Starting COPY from source path " + srcFile.getFileName() + " to target volume path: " + destDisk.getPath());

        ScriptResult result = runScript(copyScript, timeout, destDisk.getFormat().toString().toLowerCase(), srcFile.getFileName(), destFile.getFileName());
        int rc = result.getExitCode();
        if (rc != 0) {
            throw new CloudRuntimeException("Failed to convert from " + srcFile.getFileName() + " to " + destFile.getFileName() + " the error was: " + rc + " - " + result.getResult());
        }
        LOGGER.debug("Successfully converted source volume at " + srcFile.getFileName() + " to destination volume: " + destDisk.getPath() + " " + result.getResult());

        return destDisk;
    }

    private static final ScriptResult runScript(String script, long timeout, String...args) {
        ScriptResult result = new ScriptResult();
        Script cmd = new Script(script, Duration.millis(timeout), LOGGER);
        cmd.add(args);
        OutputInterpreter.OneLineParser parser = new OutputInterpreter.OneLineParser();
        String output = cmd.execute(parser);
        // its possible the process never launches which causes an NPE on getExitValue below
        if (output != null && output.contains("Unable to execute the command")) {
            result.setResult(output);
            result.setExitCode(-1);
            return result;
        }
        result.setResult(output);
        result.setExitCode(cmd.getExitValue());
        return result;
    }

    @Override
    public KVMPhysicalDisk createDiskFromTemplate(KVMPhysicalDisk template,
            String name, PhysicalDiskFormat format, Storage.ProvisioningType provisioningType, long size,
        KVMStoragePool destPool, int timeout, byte[] passphrase) {
            throw new UnsupportedOperationException("Unimplemented method 'createDiskFromTemplate'");
    }

    @Override
    public KVMPhysicalDisk createDiskFromTemplateBacking(KVMPhysicalDisk template,
         String name, PhysicalDiskFormat format, long size,
         KVMStoragePool destPool, int timeout, byte[] passphrase) {
        throw new UnsupportedOperationException("Unimplemented method 'createDiskFromTemplateBacking'");
    }

    @Override
    public KVMPhysicalDisk createPhysicalDisk(String name, KVMStoragePool pool,
            PhysicalDiskFormat format, Storage.ProvisioningType provisioningType, long size, byte[] passphrase) {
        throw new UnsupportedOperationException("Unimplemented method 'createPhysicalDisk'");
    }

    boolean isTemplateExtractable(String templatePath) {
        ScriptResult result = runScript("file", 5000L, templatePath, "| awk -F' ' '{print $2}'");
        String type = result.getResult();
        return type.equalsIgnoreCase("bzip2") || type.equalsIgnoreCase("gzip") || type.equalsIgnoreCase("zip");
    }

    String getExtractCommandForDownloadedFile(String downloadedTemplateFile, String templateFile) {
        if (downloadedTemplateFile.endsWith(".zip")) {
            return "unzip -p " + downloadedTemplateFile + " | cat > " + templateFile;
        } else if (downloadedTemplateFile.endsWith(".bz2")) {
            return "bunzip2 -c " + downloadedTemplateFile + " > " + templateFile;
        } else if (downloadedTemplateFile.endsWith(".gz")) {
            return "gunzip -c " + downloadedTemplateFile + " > " + templateFile;
        } else {
            throw new CloudRuntimeException("Unable to extract template " + downloadedTemplateFile);
        }
    }

    boolean waitForDiskToBecomeAvailable(AddressInfo address, KVMStoragePool pool, long waitTimeInSec) {
        LOGGER.debug("Waiting for the volume with id: " + address.getPath() + " of the storage pool: " + pool.getUuid() + " to become available for " + waitTimeInSec + " secs");

        long scriptTimeoutSecs = 30; // how long to wait for each script execution to run
        long maxTries = 10; // how many max retries to attempt the script
        long waitTimeInMillis = waitTimeInSec * 1000; // how long overall to wait
        int timeBetweenTries = 1000; // how long to sleep between tries
        // wait at least 60 seconds even if input was lower
        if (waitTimeInSec < 60) {
            waitTimeInSec = 60;
        }
        KVMPhysicalDisk physicalDisk = null;

        // Rescan before checking for the physical disk
        int tries = 0;
        while (waitTimeInMillis > 0 && tries < maxTries) {
            tries++;
            long start = System.currentTimeMillis();
            String lun;
            if (address.getConnectionId() == null) {
                lun = "-";
            } else {
                lun = address.getConnectionId();
            }

            Process p = null;
            try {
                ProcessBuilder builder = new ProcessBuilder(connectScript, lun, address.getAddress());
                p = builder.start();
                if (p.waitFor(scriptTimeoutSecs, TimeUnit.SECONDS)) {
                    int rc = p.exitValue();
                    StringBuffer output = new StringBuffer();
                    if (rc == 0) {
                        BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
                        String line = null;
                        while ((line = input.readLine()) != null) {
                            output.append(line);
                            output.append(" ");
                        }

                        physicalDisk = getPhysicalDisk(address, pool);
                        if (physicalDisk != null && physicalDisk.getSize() > 0) {
                            LOGGER.debug("Found the volume using id: " + address.getPath() + " of the storage pool: " + pool.getUuid());
                            return true;
                        }

                        break;
                    } else {
                        LOGGER.warn("Failure discovering LUN via " + connectScript);
                        BufferedReader error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                        String line = null;
                        while ((line = error.readLine()) != null) {
                            LOGGER.warn("error --> " + line);
                        }
                    }
                } else {
                    LOGGER.debug("Timeout waiting for " + connectScript + " to complete - try " + tries);
                }
            } catch (IOException | InterruptedException | IllegalThreadStateException e) {
                LOGGER.warn("Problem performing scan on SCSI hosts - try " + tries, e);
            } finally {
                if (p != null && p.isAlive()) {
                    p.destroyForcibly();
                }
            }

            long elapsed = System.currentTimeMillis() - start;
            waitTimeInMillis = waitTimeInMillis - elapsed;

            try {
                Thread.sleep(timeBetweenTries);
            } catch (Exception ex) {
                // don't do anything
            }
        }

        LOGGER.debug("Unable to find the volume with id: " + address.getPath() + " of the storage pool: " + pool.getUuid());
        return false;
    }

    long getPhysicalDiskSize(String diskPath) {
        if (StringUtils.isEmpty(diskPath)) {
            return 0;
        }

        Script diskCmd = new Script("blockdev", LOGGER);
        diskCmd.add("--getsize64", diskPath);

        OutputInterpreter.OneLineParser parser = new OutputInterpreter.OneLineParser();
        String result = diskCmd.execute(parser);

        if (result != null) {
            LOGGER.debug("Unable to get the disk size at path: " + diskPath);
            return 0;
        }

        Long size = Long.parseLong(parser.getLine());

        if (size <= 0) {
            // its possible the path can't be seen on the host yet, lets rescan
            // now rerun the command
            parser = new OutputInterpreter.OneLineParser();
            result = diskCmd.execute(parser);

            if (result != null) {
                LOGGER.debug("Unable to get the disk size at path: " + diskPath);
                return 0;
            }

            size = Long.parseLong(parser.getLine());
        }

        return size;
    }

    public void resize(String path, String vmName, long newSize) {
        if (LOGGER.isDebugEnabled()) LOGGER.debug("Executing resize of " + path + " to " + newSize + " bytes for VM " + vmName);

        // extract wwid
        AddressInfo address = parseAndValidatePath(path);
        if (address == null || address.getAddress() == null) {
            LOGGER.error("Unable to resize volume, address value is not valid");
            throw new CloudRuntimeException("Unable to resize volume, address value is not valid");
        }

        if (LOGGER.isDebugEnabled()) LOGGER.debug(String.format("Running %s %s %s %s", resizeScript, address.getAddress(), vmName, newSize));

        // call resizeVolume.sh <wwid>
        ScriptResult result = runScript(resizeScript, 60000L, address.getAddress(), vmName, ""+newSize);

        if (result.getExitCode() != 0) {
            throw new CloudRuntimeException("Failed to resize volume at address " + address.getAddress() + " to " + newSize + " bytes for VM " + vmName + ": " + result.getResult());
        }

        LOGGER.info("Resize of volume at address " + address.getAddress() + " completed successfully: " + result.getResult());
    }

    static void cleanupStaleMaps() {
        synchronized(CLEANUP_LOCK) {
            long start = System.currentTimeMillis();
            ScriptResult result = runScript(cleanupScript, cleanupTimeoutSecs * 1000);
            LOGGER.debug("Multipath Cleanup Job elapsed time (ms): "+ (System.currentTimeMillis() - start) + "; result: " + result.getExitCode());
        }
    }

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

        public String getType() {
            return type;
        }

        public String getAddress() {
            return address;
        }

        public String getConnectionId() {
            return connectionId;
        }

        public String getPath() {
            return path;
        }

        public String toString() {
            return String.format("type=%s; address=%s; connid=%s", getType(), getAddress(), getConnectionId());
        }
    }

    public static class Property <T> {
        private String name;
        private T defaultValue;

        Property(String name, T value) {
           this.name = name;
           this.defaultValue = value;
        }

        public String getName() {
           return this.name;
        }

        public T getDefaultValue() {
           return this.defaultValue;
        }

        public T getFinalValue() {
            File agentPropertiesFile = PropertiesUtil.findConfigFile("agent.properties");
            if (agentPropertiesFile == null) {
                LOGGER.debug(String.format("File [%s] was not found, we will use default defined values. Property [%s]: [%s].", "agent.properties", name, defaultValue));
                return defaultValue;
            } else {
                try {
                    String configValue = PropertiesUtil.loadFromFile(agentPropertiesFile).getProperty(name);
                    if (StringUtils.isBlank(configValue)) {
                        LOGGER.debug(String.format("Property [%s] has empty or null value. Using default value [%s].", name, defaultValue));
                        return defaultValue;
                    } else {
                        if (defaultValue instanceof Integer) {
                            return (T)Integer.getInteger(configValue);
                        } else if (defaultValue instanceof Long) {
                            return (T)Long.getLong(configValue);
                        } else if (defaultValue instanceof String) {
                            return (T)configValue;
                        } else if (defaultValue instanceof Boolean) {
                            return (T)Boolean.valueOf(configValue);
                        } else {
                            return null;
                        }
                    }
                } catch (IOException var5) {
                    LOGGER.debug(String.format("Failed to get property [%s]. Using default value [%s].", name, defaultValue), var5);
                    return defaultValue;
                }
            }
        }
    }

    public static class ScriptResult {
        private int exitCode = -1;
        private String result = null;
        public int getExitCode() {
            return exitCode;
        }
        public void setExitCode(int exitCode) {
            this.exitCode = exitCode;
        }
        public String getResult() {
            return result;
        }
        public void setResult(String result) {
            this.result = result;
        }
    }

}
