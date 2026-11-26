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

import com.cloud.hypervisor.kvm.resource.LibvirtConnection;
import com.cloud.hypervisor.kvm.resource.LibvirtDomainXMLParser;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

public class IscsiStorageCleanupMonitor implements Runnable{
    private static final Logger logger = LogManager.getLogger(IscsiStorageCleanupMonitor.class);
    private static final int CLEANUP_INTERVAL_SEC = 60; // check every X seconds
    private static final long UNRESOLVED_WARNING_INTERVAL_MS = 60L * 60L * 1000L;
    private static final String ISCSI_PATH_PREFIX = "/dev/disk/by-path";
    private static final String KEYWORD_ISCSI = "iscsi";
    private static final String KEYWORD_IQN = "iqn";
    private static final Pattern PARTITION_PATTERN = Pattern.compile("\\S+part\\d+$");
    private static final Path DEV_ROOT = Paths.get("/dev");
    private static final Path DEV_DISK_BY_ID = Paths.get("/dev/disk/by-id");
    private static final Path DEV_MAPPER = Paths.get("/dev/mapper");
    private static final List<Path> DEVICE_ALIAS_DIRS = List.of(DEV_DISK_BY_ID, DEV_MAPPER);
    private static final Path SYS_CLASS_BLOCK = Paths.get("/sys/class/block");

    private final KVMStoragePoolManager storagePoolManager;

    private final Map<String, Boolean> diskStatusMap;
    private final Map<String, Long> unresolvedPathWarningTimes = new HashMap<>();

    public IscsiStorageCleanupMonitor(KVMStoragePoolManager storagePoolManager) {
        diskStatusMap = new HashMap<>();
        this.storagePoolManager = storagePoolManager;
        logger.debug("Initialize cleanup thread");
    }

    static boolean isIscsiDisk(String path) {
        return path.startsWith(ISCSI_PATH_PREFIX) && path.contains(KEYWORD_ISCSI) && path.contains(KEYWORD_IQN)
                && !PARTITION_PATTERN.matcher(path).matches();
    }

    /**
     * Returns the kernel block devices backing a path. Device-mapper (multipath) devices are
     * represented by their slaves as well, so an alias that resolves to the mapper still matches
     * an alias that resolves to the underlying SCSI disk.
     * An empty result means the path could not be resolved to a known block device.
     */
    static Set<String> getKernelDevices(Path sysBlock, String path) {
        Set<String> devices = new HashSet<>();
        if (path == null) {
            return devices;
        }
        try {
            String device = Paths.get(path).toRealPath().getFileName().toString();
            if (Files.exists(sysBlock.resolve(device))) {
                addKernelDevice(sysBlock, device, devices);
            }
        } catch (IOException | InvalidPathException | SecurityException e) {
            // The caller decides how to treat a path whose identity is unknown.
        }
        return devices;
    }

    private static void addKernelDevice(Path sysBlock, String device, Set<String> devices) {
        if (!devices.add(device)) {
            return;
        }
        Path devicePath = sysBlock.resolve(device);
        String parent = getPartitionParent(sysBlock, devicePath);
        if (parent != null) {
            addKernelDevice(sysBlock, parent, devices);
        }
        File[] slaves = devicePath.resolve("slaves").toFile().listFiles();
        if (slaves != null) {
            for (File slave : slaves) {
                addKernelDevice(sysBlock, slave.getName(), devices);
            }
        }
    }

    private static String getPartitionParent(Path sysBlock, Path devicePath) {
        if (!Files.exists(devicePath.resolve("partition"))) {
            return null;
        }
        try {
            Path parent = devicePath.toRealPath().getParent();
            if (parent != null) {
                String parentName = parent.getFileName().toString();
                if (Files.exists(sysBlock.resolve(parentName))) {
                    return parentName;
                }
            }
        } catch (IOException | SecurityException e) {
            // The partition has no resolvable parent, so only its own device is returned.
        }
        return null;
    }

    /**
     * The iSCSI paths matched to running VM disks, plus the paths whose identity is unknown.
     * Protected paths must not be disconnected but do not block cleanup of other stale paths.
     * A blocking path is a VM disk whose identity is unknown; cleanup must be skipped for the
     * whole round because there is no safe way to tell which iSCSI path backs it.
     */
    static final class ActiveIscsiPaths {
        private final Set<String> paths;
        private final Set<String> protectedPaths;
        private final String blockingPath;

        private ActiveIscsiPaths(Set<String> paths, Set<String> protectedPaths, String blockingPath) {
            this.paths = paths;
            this.protectedPaths = protectedPaths;
            this.blockingPath = blockingPath;
        }

        static ActiveIscsiPaths resolved(Set<String> paths, Set<String> protectedPaths) {
            return new ActiveIscsiPaths(paths, protectedPaths, null);
        }

        static ActiveIscsiPaths blocked(String path) {
            return new ActiveIscsiPaths(Collections.emptySet(), Collections.emptySet(), path);
        }

        boolean isResolved() {
            return blockingPath == null;
        }

        Set<String> getPaths() {
            return paths;
        }

        Set<String> getProtectedPaths() {
            return protectedPaths;
        }

        String getBlockingPath() {
            return blockingPath;
        }
    }

    /**
     * Returns the iSCSI paths backing at least one of the given disk paths. An unresolved iSCSI
     * path is protected from cleanup; an unresolved VM disk path blocks the whole round.
     */
    static ActiveIscsiPaths findActiveIscsiPaths(Collection<String> iscsiPaths, Collection<String> diskPaths, Path sysBlock) {
        return findActiveIscsiPaths(iscsiPaths, diskPaths, sysBlock, DEV_ROOT);
    }

    private static boolean isStorageAlias(String path, String devPrefix) {
        return path.startsWith(devPrefix + "disk" + File.separator)
                || path.startsWith(devPrefix + "mapper" + File.separator);
    }

    private static boolean pathExists(String path) {
        try {
            return Files.exists(Paths.get(path));
        } catch (InvalidPathException e) {
            return false;
        }
    }

    static boolean shouldWarnUnresolvedPath(Map<String, Long> warningTimes, String path, long now) {
        Long lastWarning = warningTimes.get(path);
        if (lastWarning == null || now - lastWarning >= UNRESOLVED_WARNING_INTERVAL_MS) {
            warningTimes.put(path, now);
            return true;
        }
        return false;
    }

    static ActiveIscsiPaths findActiveIscsiPaths(Collection<String> iscsiPaths, Collection<String> diskPaths, Path sysBlock, Path devRoot) {
        Map<String, List<String>> iscsiPathsByDevice = new HashMap<>();
        Set<String> protectedPaths = new HashSet<>();
        for (String iscsiPath : iscsiPaths) {
            Set<String> devices = getKernelDevices(sysBlock, iscsiPath);
            if (devices.isEmpty()) {
                if (pathExists(iscsiPath)) {
                    protectedPaths.add(iscsiPath);
                }
                continue;
            }
            for (String device : devices) {
                iscsiPathsByDevice.computeIfAbsent(device, key -> new ArrayList<>()).add(iscsiPath);
            }
        }

        String devPrefix = devRoot.toString() + File.separator;
        Set<String> activePaths = new HashSet<>();
        for (String diskPath : diskPaths) {
            if (diskPath == null || !diskPath.startsWith(devPrefix)) {
                continue;
            }
            Set<String> diskDevices = getKernelDevices(sysBlock, diskPath);
            if (diskDevices.isEmpty()) {
                if (isStorageAlias(diskPath, devPrefix) && pathExists(diskPath)) {
                    return ActiveIscsiPaths.blocked(diskPath);
                }
                continue;
            }
            for (String device : diskDevices) {
                List<String> matchingPaths = iscsiPathsByDevice.get(device);
                if (matchingPaths != null) {
                    activePaths.addAll(matchingPaths);
                }
            }
        }
        return ActiveIscsiPaths.resolved(activePaths, protectedPaths);
    }

    /**
     * Indexes the device aliases by their backing kernel devices so an inactive iSCSI path can be
     * offered to the adaptor that owns its identity-stable alias (for example an ONTAP WWN or a
     * multipath mapper path).
     */
    static Map<String, List<String>> indexAliases(Collection<Path> aliasesDirs, Path sysBlock) {
        Map<String, List<String>> aliasesByDevice = new HashMap<>();
        for (Path aliasesDir : aliasesDirs) {
            File[] entries = aliasesDir.toFile().listFiles();
            if (entries == null) {
                continue;
            }
            for (File entry : entries) {
                if (PARTITION_PATTERN.matcher(entry.getName()).matches()) {
                    continue;
                }
                for (String device : getKernelDevices(sysBlock, entry.getAbsolutePath())) {
                    aliasesByDevice.computeIfAbsent(device, key -> new ArrayList<>()).add(entry.getAbsolutePath());
                }
            }
        }
        return aliasesByDevice;
    }

    static boolean disconnectPhysicalDisk(KVMStoragePoolManager storagePoolManager, String diskPath,
                                          Map<String, List<String>> aliasesByDevice, Path sysBlock) {
        Set<String> aliases = new TreeSet<>();
        for (String device : getKernelDevices(sysBlock, diskPath)) {
            aliases.addAll(aliasesByDevice.getOrDefault(device, Collections.emptyList()));
        }
        for (String alias : aliases) {
            if (storagePoolManager.disconnectPhysicalDiskByPath(alias)) {
                return true;
            }
        }
        return storagePoolManager.disconnectPhysicalDiskByPath(diskPath);
    }

    /**
     * Disconnects every iSCSI path that was not matched to a running VM disk.
     * XXX: It is possible that someone had manually added an iSCSI volume.
     * we would not be able to detect that
     */
    void disconnectInactiveSessions(Map<String, Boolean> status, Collection<Path> aliasesDirs, Path sysBlock) {
        Map<String, List<String>> aliasesByDevice = null;
        for (Map.Entry<String, Boolean> entry : status.entrySet()) {
            String diskPath = entry.getKey();
            if (!entry.getValue() && pathExists(diskPath)) {
                if (aliasesByDevice == null) {
                    aliasesByDevice = indexAliases(aliasesDirs, sysBlock);
                }
                try {
                    logger.info("Cleaning up disk [{}]", diskPath);
                    disconnectPhysicalDisk(storagePoolManager, diskPath, aliasesByDevice, sysBlock);
                } catch (Exception e) {
                    logger.warn("[ignored] Error cleaning up [{}]", diskPath, e);
                }
            }
        }
    }

    boolean updateAndDisconnectInactiveSessions(Connect conn, Map<String, Boolean> status,
                                                Collection<Path> aliasesDirs, Path sysBlock, Path devRoot) {
        if (!updateDiskStatusMapWithInactiveIscsiSessions(conn, status, sysBlock, devRoot, unresolvedPathWarningTimes)) {
            return false;
        }
        disconnectInactiveSessions(status, aliasesDirs, sysBlock);
        return true;
    }

    static boolean updateDiskStatusMapWithInactiveIscsiSessions(Connect conn, Map<String, Boolean> status,
                                                                Path sysBlock, Path devRoot,
                                                                Map<String, Long> unresolvedPathWarningTimes) {
        try {
            List<String> diskPaths = new ArrayList<>();
            int[] domains = conn.listDomains();
            logger.debug("iSCSI cleanup found {} domains", domains.length);
            for (int domId : domains) {
                Domain dm = conn.domainLookupByID(domId);
                final String domXml = dm.getXMLDesc(0);
                final LibvirtDomainXMLParser parser = new LibvirtDomainXMLParser();
                parser.parseDomainXML(domXml);
                for (final LibvirtVMDef.DiskDef disk : parser.getDisks()) {
                    diskPaths.add(disk.getDiskPath());
                }
            }

            ActiveIscsiPaths activePaths = findActiveIscsiPaths(status.keySet(), diskPaths, sysBlock, devRoot);
            if (!activePaths.isResolved()) {
                String blockingPath = activePaths.getBlockingPath();
                if (shouldWarnUnresolvedPath(unresolvedPathWarningTimes, blockingPath, System.currentTimeMillis())) {
                    logger.warn("Unable to identify the block device backing {}. Skipping iSCSI cleanup to avoid "
                            + "disconnecting a disk that may be in use", blockingPath);
                }
                return false;
            }
            unresolvedPathWarningTimes.clear();
            for (String activePath : activePaths.getPaths()) {
                status.put(activePath, true);
                logger.debug("iSCSI cleanup thread found active disk [{}]", activePath);
            }
            for (String protectedPath : activePaths.getProtectedPaths()) {
                status.put(protectedPath, true);
                logger.debug("Protecting iSCSI disk with unresolved identity [{}]", protectedPath);
            }
            return true;
        } catch (LibvirtException e) {
            logger.warn("[ignored] Error trying to cleanup ", e);
            return false;
        }
    }

    private class Monitor extends ManagedContextRunnable {

        @Override
        protected void runInContext() {
            Connect conn;
            try {
                conn = LibvirtConnection.getConnection();

                // populate all the iscsi disks currently attached to this host
                File[] iscsiVolumes = new File(ISCSI_PATH_PREFIX).listFiles();
                if (iscsiVolumes == null || iscsiVolumes.length == 0) {
                    logger.debug("No iscsi sessions found for cleanup");
                    return;
                }

                // set all status values to false
                initializeDiskStatusMap(iscsiVolumes);

                // check if iscsi sessions belong to any VM; disconnect stale sessions only when safe
                updateAndDisconnectInactiveSessions(conn, diskStatusMap, DEVICE_ALIAS_DIRS, SYS_CLASS_BLOCK, DEV_ROOT);

            } catch (LibvirtException e) {
                logger.warn("[ignored] Error trying to cleanup ", e);
            }
        }

        /**
         * For each volume, if the volume path is of type iSCSI, add to diskstatusmap and set status to false.
         */
        private void initializeDiskStatusMap(File[] iscsiVolumes) {
            diskStatusMap.clear();
            for (File v : iscsiVolumes) {
                if (isIscsiDisk(v.getAbsolutePath())) {
                    logger.debug("Cleanup thread found iSCSI disk [{}], marking it as inactive", v.getAbsolutePath());
                    diskStatusMap.put(v.getAbsolutePath(), false);
                }
            }
        }

    }

    @Override
    public void run() {
        while(true) {
            try {
                Thread.sleep(CLEANUP_INTERVAL_SEC * 1000);
            } catch (InterruptedException e) {
                logger.debug("[ignored] interrupted between heartbeats.");
            }

            Thread monitorThread = new Thread(new Monitor());
            monitorThread.start();
            try {
                monitorThread.join();
            } catch (InterruptedException e) {
                logger.debug("[ignored] interrupted joining monitor.");
            }
        }
    }
}
