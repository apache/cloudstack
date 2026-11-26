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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.mockito.InOrder;

public class IscsiStorageCleanupMonitorTest {

    private static final String BY_PATH = "ip-10.0.0.1:3260-iscsi-iqn.1992-08.com.netapp:sn.abc:vs.3-lun-0";
    private static final String BY_ID = "wwn-0x600a098078304d2d383f2f6b45734c6a";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private Path dev;
    private Path sysBlock;
    private Path devices;

    @Before
    public void setUp() throws IOException {
        dev = temporaryFolder.newFolder("dev").toPath();
        sysBlock = temporaryFolder.newFolder("sys-block").toPath();
        devices = temporaryFolder.newFolder("devices").toPath();
    }

    private Path createDevice(String name) throws IOException {
        Files.createDirectories(sysBlock.resolve(name));
        return Files.createFile(devices.resolve(name));
    }

    private Path createAlias(String directory, String name, Path target) throws IOException {
        return Files.createSymbolicLink(Files.createDirectories(dev.resolve(directory)).resolve(name), target);
    }

    @Test
    public void getKernelDevicesMatchesDifferentUdevAliasesForTheSameDevice() throws IOException {
        Path sda = createDevice("sda");
        Path byPath = createAlias("disk/by-path", BY_PATH, sda);
        Path byId = createAlias("disk/by-id", BY_ID, sda);

        assertEquals(Collections.singleton("sda"), IscsiStorageCleanupMonitor.getKernelDevices(sysBlock, byPath.toString()));
        assertEquals(IscsiStorageCleanupMonitor.getKernelDevices(sysBlock, byPath.toString()),
                IscsiStorageCleanupMonitor.getKernelDevices(sysBlock, byId.toString()));
    }

    @Test
    public void getKernelDevicesDoesNotMatchDifferentDevicesOrMissingPaths() throws IOException {
        Path sda = createDevice("sda");
        Path sdb = createDevice("sdb");
        Path byPath = createAlias("disk/by-path", BY_PATH, sda);

        assertNotEquals(IscsiStorageCleanupMonitor.getKernelDevices(sysBlock, byPath.toString()),
                IscsiStorageCleanupMonitor.getKernelDevices(sysBlock, sdb.toString()));
        assertTrue(IscsiStorageCleanupMonitor.getKernelDevices(sysBlock, byPath.resolveSibling("missing").toString()).isEmpty());
    }

    @Test
    public void getKernelDevicesIncludesTheSlavesOfADeviceMapperPath() throws IOException {
        Path sda = createDevice("sda");
        Path dm0 = createDevice("dm-0");
        Path slaves = Files.createDirectories(sysBlock.resolve("dm-0/slaves"));
        Files.createFile(slaves.resolve("sda"));
        Path byPath = createAlias("disk/by-path", BY_PATH, sda);
        Path byId = createAlias("disk/by-id", BY_ID, dm0);

        Set<String> mapperDevices = IscsiStorageCleanupMonitor.getKernelDevices(sysBlock, byId.toString());
        assertTrue(mapperDevices.containsAll(IscsiStorageCleanupMonitor.getKernelDevices(sysBlock, byPath.toString())));
    }

    @Test
    public void indexAliasesGroupsNonPartitionAliasesByKernelDevice() throws IOException {
        Path sda = createDevice("sda");
        Path byId = createAlias("disk/by-id", BY_ID, sda);
        Path partition = createAlias("disk/by-id", BY_ID + "-part1", sda);

        Map<String, List<String>> aliases = IscsiStorageCleanupMonitor.indexAliases(Collections.singletonList(dev.resolve("disk/by-id")), sysBlock);

        assertEquals(Collections.singletonList(byId.toString()), aliases.get("sda"));
        assertFalse(aliases.get("sda").contains(partition.toString()));
    }

    @Test
    public void disconnectPhysicalDiskUsesByIdAliasBeforeByPath() throws IOException {
        Path sda = createDevice("sda");
        Path byPath = createAlias("disk/by-path", BY_PATH, sda);
        Path byId = createAlias("disk/by-id", BY_ID, sda);
        Map<String, List<String>> aliases = IscsiStorageCleanupMonitor.indexAliases(Collections.singletonList(dev.resolve("disk/by-id")), sysBlock);
        KVMStoragePoolManager storagePoolManager = mock(KVMStoragePoolManager.class);
        when(storagePoolManager.disconnectPhysicalDiskByPath(byId.toString())).thenReturn(true);

        assertTrue(IscsiStorageCleanupMonitor.disconnectPhysicalDisk(storagePoolManager, byPath.toString(), aliases, sysBlock));
        verify(storagePoolManager).disconnectPhysicalDiskByPath(byId.toString());
        verify(storagePoolManager, never()).disconnectPhysicalDiskByPath(byPath.toString());
    }

    @Test
    public void disconnectPhysicalDiskFallsBackToByPathWhenAliasIsNotClaimed() throws IOException {
        Path sda = createDevice("sda");
        Path byPath = createAlias("disk/by-path", BY_PATH, sda);
        Path byId = createAlias("disk/by-id", BY_ID, sda);
        Map<String, List<String>> aliases = IscsiStorageCleanupMonitor.indexAliases(Collections.singletonList(dev.resolve("disk/by-id")), sysBlock);
        KVMStoragePoolManager storagePoolManager = mock(KVMStoragePoolManager.class);
        when(storagePoolManager.disconnectPhysicalDiskByPath(byId.toString())).thenReturn(false);
        when(storagePoolManager.disconnectPhysicalDiskByPath(byPath.toString())).thenReturn(true);

        assertTrue(IscsiStorageCleanupMonitor.disconnectPhysicalDisk(storagePoolManager, byPath.toString(), aliases, sysBlock));
        verify(storagePoolManager).disconnectPhysicalDiskByPath(byId.toString());
        verify(storagePoolManager).disconnectPhysicalDiskByPath(byPath.toString());
    }

    @Test
    public void disconnectPhysicalDiskOffersAliasesInDeterministicOrder() throws IOException {
        Path sda = createDevice("sda");
        Path byPath = createAlias("disk/by-path", BY_PATH, sda);
        String dmUuid = createAlias("disk/by-id", "dm-uuid-mpath-3600a098078304d2d383f2f6b45734c6a", sda).toString();
        String scsi = createAlias("disk/by-id", "scsi-3600a098078304d2d383f2f6b45734c6a", sda).toString();
        String wwn = createAlias("disk/by-id", BY_ID, sda).toString();
        Map<String, List<String>> aliases = IscsiStorageCleanupMonitor.indexAliases(Collections.singletonList(dev.resolve("disk/by-id")), sysBlock);
        KVMStoragePoolManager storagePoolManager = mock(KVMStoragePoolManager.class);
        when(storagePoolManager.disconnectPhysicalDiskByPath(dmUuid)).thenReturn(false);
        when(storagePoolManager.disconnectPhysicalDiskByPath(scsi)).thenReturn(false);
        when(storagePoolManager.disconnectPhysicalDiskByPath(wwn)).thenReturn(true);

        assertTrue(IscsiStorageCleanupMonitor.disconnectPhysicalDisk(storagePoolManager, byPath.toString(), aliases, sysBlock));

        InOrder order = inOrder(storagePoolManager);
        order.verify(storagePoolManager).disconnectPhysicalDiskByPath(dmUuid);
        order.verify(storagePoolManager).disconnectPhysicalDiskByPath(scsi);
        order.verify(storagePoolManager).disconnectPhysicalDiskByPath(wwn);
        verify(storagePoolManager, never()).disconnectPhysicalDiskByPath(byPath.toString());
    }

    @Test
    public void disconnectPhysicalDiskUsesMapperAliasForMultipathDevice() throws IOException {
        Path sda = createDevice("sda");
        Path dm0 = createDevice("dm-0");
        Path slaves = Files.createDirectories(sysBlock.resolve("dm-0/slaves"));
        Files.createFile(slaves.resolve("sda"));
        Path byPath = createAlias("disk/by-path", BY_PATH, sda);
        Path mapper = createAlias("mapper", "3600a098078304d2d383f2f6b45734c6a", dm0);
        Map<String, List<String>> aliases = IscsiStorageCleanupMonitor.indexAliases(
                List.of(dev.resolve("disk/by-id"), dev.resolve("mapper")), sysBlock);
        KVMStoragePoolManager storagePoolManager = mock(KVMStoragePoolManager.class);
        when(storagePoolManager.disconnectPhysicalDiskByPath(mapper.toString())).thenReturn(true);

        assertTrue(IscsiStorageCleanupMonitor.disconnectPhysicalDisk(storagePoolManager, byPath.toString(), aliases, sysBlock));
        verify(storagePoolManager).disconnectPhysicalDiskByPath(mapper.toString());
        verify(storagePoolManager, never()).disconnectPhysicalDiskByPath(byPath.toString());
    }

    @Test
    public void disconnectInactiveSessionsOnlyDisconnectsInactiveExistingPaths() throws IOException {
        Path sda = createDevice("sda");
        Path sdb = createDevice("sdb");
        Path inactive = createAlias("disk/by-path", BY_PATH, sda);
        Path byId = createAlias("disk/by-id", BY_ID, sda);
        Path active = createAlias("disk/by-path",
                "ip-10.0.0.2:3260-iscsi-iqn.1992-08.com.netapp:sn.abc:vs.3-lun-0", sdb);
        String missing = dev.resolve("disk/by-path/missing").toString();
        KVMStoragePoolManager storagePoolManager = mock(KVMStoragePoolManager.class);
        when(storagePoolManager.disconnectPhysicalDiskByPath(byId.toString())).thenReturn(true);
        IscsiStorageCleanupMonitor monitor = new IscsiStorageCleanupMonitor(storagePoolManager);

        monitor.disconnectInactiveSessions(Map.of(inactive.toString(), false, active.toString(), true, missing, false),
                Collections.singletonList(dev.resolve("disk/by-id")), sysBlock);

        verify(storagePoolManager).disconnectPhysicalDiskByPath(byId.toString());
        verify(storagePoolManager, never()).disconnectPhysicalDiskByPath(inactive.toString());
        verify(storagePoolManager, never()).disconnectPhysicalDiskByPath(active.toString());
        verify(storagePoolManager, never()).disconnectPhysicalDiskByPath(missing);
    }

    @Test
    public void findActiveIscsiPathsIgnoresMissingIscsiDevices() throws IOException {
        Path sda = createDevice("sda");
        Path byPath = createAlias("disk/by-path", BY_PATH, sda);
        Path byId = createAlias("disk/by-id", BY_ID, sda);
        Path missingAlias = createAlias("disk/by-path",
                "ip-10.0.0.2:3260-iscsi-iqn.1992-08.com.netapp:sn.abc:vs.3-lun-0", devices.resolve("missing"));

        IscsiStorageCleanupMonitor.ActiveIscsiPaths activePaths = IscsiStorageCleanupMonitor.findActiveIscsiPaths(
                Set.of(byPath.toString(), missingAlias.toString()), Collections.singleton(byId.toString()), sysBlock, dev);

        assertTrue(activePaths.isResolved());
        assertEquals(Collections.singleton(byPath.toString()), activePaths.getPaths());
    }

    @Test
    public void findActiveIscsiPathsProtectsExistingIscsiDevicesWithUnknownIdentity() throws IOException {
        Path regularFile = temporaryFolder.newFolder("by-path").toPath().resolve(BY_PATH);
        Files.createFile(regularFile);

        IscsiStorageCleanupMonitor.ActiveIscsiPaths activePaths = IscsiStorageCleanupMonitor.findActiveIscsiPaths(
                Collections.singleton(regularFile.toString()), Collections.emptyList(), sysBlock);

        assertTrue(activePaths.isResolved());
        assertTrue(activePaths.getPaths().isEmpty());
        assertEquals(Collections.singleton(regularFile.toString()), activePaths.getProtectedPaths());
    }

    @Test
    public void findActiveIscsiPathsMatchesByIdDiskToByPathEntry() throws IOException {
        Path sda = createDevice("sda");
        Path byPath = createAlias("disk/by-path", BY_PATH, sda);
        Path byId = createAlias("disk/by-id", BY_ID, sda);

        IscsiStorageCleanupMonitor.ActiveIscsiPaths activePaths = IscsiStorageCleanupMonitor.findActiveIscsiPaths(
                Collections.singleton(byPath.toString()), Collections.singleton(byId.toString()), sysBlock, dev);

        assertTrue(activePaths.isResolved());
        assertTrue(activePaths.getPaths().contains(byPath.toString()));
    }

    @Test
    public void findActiveIscsiPathsMatchesMultipathDiskToByPathEntry() throws IOException {
        Path sda = createDevice("sda");
        Path dm0 = createDevice("dm-0");
        Path slaves = Files.createDirectories(sysBlock.resolve("dm-0/slaves"));
        Files.createFile(slaves.resolve("sda"));
        Path byPath = createAlias("disk/by-path", BY_PATH, sda);
        Path byId = createAlias("disk/by-id", "dm-uuid-mpath-3600a098078304d2d383f2f6b45734c6a", dm0);

        IscsiStorageCleanupMonitor.ActiveIscsiPaths activePaths = IscsiStorageCleanupMonitor.findActiveIscsiPaths(
                Collections.singleton(byPath.toString()), Collections.singleton(byId.toString()), sysBlock, dev);

        assertTrue(activePaths.isResolved());
        assertTrue(activePaths.getPaths().contains(byPath.toString()));
    }

    @Test
    public void findActiveIscsiPathsMatchesPartitionDiskToByPathEntry() throws IOException {
        Path sda = createDevice("sda");
        Path sda1 = Files.createDirectories(sysBlock.resolve("sda/sda1"));
        Files.createFile(sda1.resolve("partition"));
        Files.createSymbolicLink(sysBlock.resolve("sda1"), sda1);
        Path sda1Device = Files.createFile(devices.resolve("sda1"));
        Path byPath = createAlias("disk/by-path", BY_PATH, sda);
        Path byId = createAlias("disk/by-id", BY_ID + "-part1", sda1Device);

        assertEquals(Set.of("sda", "sda1"), IscsiStorageCleanupMonitor.getKernelDevices(sysBlock, byId.toString()));

        IscsiStorageCleanupMonitor.ActiveIscsiPaths activePaths = IscsiStorageCleanupMonitor.findActiveIscsiPaths(
                Collections.singleton(byPath.toString()), Collections.singleton(byId.toString()), sysBlock, dev);

        assertTrue(activePaths.isResolved());
        assertTrue(activePaths.getPaths().contains(byPath.toString()));
    }

    @Test
    public void findActiveIscsiPathsIgnoresUnresolvedNonAliasDisks() throws IOException {
        Path sda = createDevice("sda");
        Path byPath = createAlias("disk/by-path", BY_PATH, sda);

        IscsiStorageCleanupMonitor.ActiveIscsiPaths activePaths = IscsiStorageCleanupMonitor.findActiveIscsiPaths(
                Collections.singleton(byPath.toString()), Collections.singleton(dev.resolve("sdz").toString()), sysBlock, dev);

        assertTrue(activePaths.isResolved());
        assertTrue(activePaths.getPaths().isEmpty());
    }

    @Test
    public void findActiveIscsiPathsIgnoresMissingVmDisksWithStorageAlias() throws IOException {
        Path sda = createDevice("sda");
        Path byPath = createAlias("disk/by-path", BY_PATH, sda);
        Path byId = createAlias("disk/by-id", BY_ID, devices.resolve("missing"));

        IscsiStorageCleanupMonitor.ActiveIscsiPaths activePaths = IscsiStorageCleanupMonitor.findActiveIscsiPaths(
                Collections.singleton(byPath.toString()), Collections.singleton(byId.toString()), sysBlock, dev);

        assertTrue(activePaths.isResolved());
        assertTrue(activePaths.getPaths().isEmpty());
        assertTrue(activePaths.getProtectedPaths().isEmpty());
    }

    @Test
    public void monitorIgnoresMissingVmDiskAliasAndStillCleansUp() throws Exception {
        Path sda = createDevice("sda");
        Path byPath = createAlias("disk/by-path", BY_PATH, sda);
        Path byId = createAlias("disk/by-id", BY_ID, devices.resolve("missing"));
        Connect connect = mock(Connect.class);
        Domain domain = mock(Domain.class);
        when(connect.listDomains()).thenReturn(new int[]{1});
        when(connect.domainLookupByID(1)).thenReturn(domain);
        when(domain.getXMLDesc(0)).thenReturn(domainXml(byId));
        Map<String, Boolean> status = new HashMap<>();
        status.put(byPath.toString(), false);
        KVMStoragePoolManager storagePoolManager = mock(KVMStoragePoolManager.class);
        IscsiStorageCleanupMonitor monitor = new IscsiStorageCleanupMonitor(storagePoolManager);

        assertTrue(monitor.updateAndDisconnectInactiveSessions(connect, status, List.of(dev.resolve("disk/by-id"),
                dev.resolve("mapper")), sysBlock, dev));

        verify(storagePoolManager).disconnectPhysicalDiskByPath(byPath.toString());
    }

    @Test
    public void monitorProtectsAnUnresolvedIscsiPathWithoutBlockingOtherCleanup() throws Exception {
        Path sda = createDevice("sda");
        Path byPath = createAlias("disk/by-path", BY_PATH, sda);
        String unresolved = Files.createFile(Files.createDirectories(dev.resolve("disk/by-path"))
                .resolve("ip-10.0.0.3:3260-iscsi-iqn.1992-08.com.netapp:sn.abc:vs.3-lun-0")).toString();
        Map<String, Boolean> status = new HashMap<>();
        status.put(byPath.toString(), false);
        status.put(unresolved, false);
        Connect connect = mock(Connect.class);
        when(connect.listDomains()).thenReturn(new int[]{});
        KVMStoragePoolManager storagePoolManager = mock(KVMStoragePoolManager.class);

        assertTrue(IscsiStorageCleanupMonitor.updateDiskStatusMapWithInactiveIscsiSessions(connect, status,
                sysBlock, dev, new HashMap<>()));
        assertFalse(status.get(byPath.toString()));
        assertTrue(status.get(unresolved));

        IscsiStorageCleanupMonitor monitor = new IscsiStorageCleanupMonitor(storagePoolManager);
        monitor.disconnectInactiveSessions(status, List.of(dev.resolve("disk/by-id"), dev.resolve("mapper")), sysBlock);

        verify(storagePoolManager).disconnectPhysicalDiskByPath(byPath.toString());
        verify(storagePoolManager, never()).disconnectPhysicalDiskByPath(unresolved);
    }

    @Test
    public void findActiveIscsiPathsRejectsExistingVmDisksWithUnknownIdentity() throws IOException {
        Path sda = createDevice("sda");
        Path byPath = createAlias("disk/by-path", BY_PATH, sda);
        Path unknownDisk = Files.createFile(Files.createDirectories(dev.resolve("disk/by-id")).resolve("unknown"));

        IscsiStorageCleanupMonitor.ActiveIscsiPaths activePaths = IscsiStorageCleanupMonitor.findActiveIscsiPaths(
                Collections.singleton(byPath.toString()), Collections.singleton(unknownDisk.toString()), sysBlock, dev);

        assertFalse(activePaths.isResolved());
    }

    @Test
    public void findActiveIscsiPathsIgnoresUnresolvedNonStorageDevices() throws IOException {
        Path sda = createDevice("sda");
        Path byPath = createAlias("disk/by-path", BY_PATH, sda);
        Path nonStorageDevice = Files.createFile(dev.resolve("non-storage-device"));

        IscsiStorageCleanupMonitor.ActiveIscsiPaths activePaths = IscsiStorageCleanupMonitor.findActiveIscsiPaths(
                Collections.singleton(byPath.toString()), Collections.singleton(nonStorageDevice.toString()), sysBlock, dev);

        assertTrue(activePaths.isResolved());
        assertTrue(activePaths.getPaths().isEmpty());
    }

    @Test
    public void findActiveIscsiPathsIgnoresInvalidIscsiPaths() {
        IscsiStorageCleanupMonitor.ActiveIscsiPaths activePaths = IscsiStorageCleanupMonitor.findActiveIscsiPaths(
                Collections.singleton("/dev/disk/by-path/invalid\0path"), Collections.emptyList(), sysBlock);

        assertTrue(activePaths.isResolved());
        assertTrue(activePaths.getPaths().isEmpty());
    }

    @Test
    public void findActiveIscsiPathsIgnoresInvalidVmDiskPaths() throws IOException {
        Path sda = createDevice("sda");
        Path byPath = createAlias("disk/by-path", BY_PATH, sda);
        String invalidDisk = dev.resolve("disk/by-id/invalid") + "\0path";

        IscsiStorageCleanupMonitor.ActiveIscsiPaths activePaths = IscsiStorageCleanupMonitor.findActiveIscsiPaths(
                Collections.singleton(byPath.toString()), Collections.singleton(invalidDisk), sysBlock, dev);

        assertTrue(activePaths.isResolved());
        assertTrue(activePaths.getPaths().isEmpty());
    }

    @Test
    public void monitorKeepsAnActiveByIdDiskAndDoesNotDisconnectIt() throws Exception {
        Path sda = createDevice("sda");
        Path byPath = createAlias("disk/by-path", BY_PATH, sda);
        Path byId = createAlias("disk/by-id", BY_ID, sda);
        Connect connect = mock(Connect.class);
        Domain domain = mock(Domain.class);
        when(connect.listDomains()).thenReturn(new int[]{1});
        when(connect.domainLookupByID(1)).thenReturn(domain);
        when(domain.getXMLDesc(0)).thenReturn(domainXml(byId));
        Map<String, Boolean> status = new HashMap<>();
        status.put(byPath.toString(), false);

        assertTrue(IscsiStorageCleanupMonitor.updateDiskStatusMapWithInactiveIscsiSessions(connect, status, sysBlock, dev,
                new HashMap<>()));
        assertTrue(status.get(byPath.toString()));

        KVMStoragePoolManager storagePoolManager = mock(KVMStoragePoolManager.class);
        IscsiStorageCleanupMonitor monitor = new IscsiStorageCleanupMonitor(storagePoolManager);
        monitor.disconnectInactiveSessions(status, List.of(dev.resolve("disk/by-id"), dev.resolve("mapper")), sysBlock);

        verify(storagePoolManager, never()).disconnectPhysicalDiskByPath(anyString());
    }

    @Test
    public void monitorKeepsAllActiveLunsOfASharedTargetAndCleansOnlyTheInactiveOne() throws Exception {
        String iqn = "iqn.1992-08.com.netapp:sn.abc:vs.3";
        Path sda = createDevice("sda");
        Path sdb = createDevice("sdb");
        Path sdc = createDevice("sdc");
        Path byPath0 = createAlias("disk/by-path", BY_PATH, sda);
        Path byPath1 = createAlias("disk/by-path", "ip-10.0.0.1:3260-iscsi-" + iqn + "-lun-1", sdb);
        Path byPath2 = createAlias("disk/by-path", "ip-10.0.0.1:3260-iscsi-" + iqn + "-lun-2", sdc);
        Path byId0 = createAlias("disk/by-id", BY_ID, sda);
        Connect connect = mock(Connect.class);
        Domain firstDomain = mock(Domain.class);
        Domain secondDomain = mock(Domain.class);
        when(connect.listDomains()).thenReturn(new int[]{1, 2});
        when(connect.domainLookupByID(1)).thenReturn(firstDomain);
        when(connect.domainLookupByID(2)).thenReturn(secondDomain);
        when(firstDomain.getXMLDesc(0)).thenReturn(domainXml(byId0));
        when(secondDomain.getXMLDesc(0)).thenReturn(domainXml(byPath1));
        Map<String, Boolean> status = new HashMap<>();
        status.put(byPath0.toString(), false);
        status.put(byPath1.toString(), false);
        status.put(byPath2.toString(), false);
        KVMStoragePoolManager storagePoolManager = mock(KVMStoragePoolManager.class);
        IscsiStorageCleanupMonitor monitor = new IscsiStorageCleanupMonitor(storagePoolManager);

        assertTrue(monitor.updateAndDisconnectInactiveSessions(connect, status,
                List.of(dev.resolve("disk/by-id"), dev.resolve("mapper")), sysBlock, dev));

        assertTrue(status.get(byPath0.toString()));
        assertTrue(status.get(byPath1.toString()));
        verify(storagePoolManager, never()).disconnectPhysicalDiskByPath(byPath0.toString());
        verify(storagePoolManager, never()).disconnectPhysicalDiskByPath(byPath1.toString());
        verify(storagePoolManager).disconnectPhysicalDiskByPath(byPath2.toString());
    }

    @Test
    public void updateDiskStatusMapWarnsOnceWithinTheIntervalForAnUnresolvedPath() throws Exception {
        Path sda = createDevice("sda");
        Path byPath = createAlias("disk/by-path", BY_PATH, sda);
        Path unknownDisk = Files.createFile(Files.createDirectories(dev.resolve("disk/by-id")).resolve("unknown"));
        Connect connect = mock(Connect.class);
        Domain domain = mock(Domain.class);
        when(connect.listDomains()).thenReturn(new int[]{1});
        when(connect.domainLookupByID(1)).thenReturn(domain);
        when(domain.getXMLDesc(0)).thenReturn(domainXml(unknownDisk));
        Map<String, Boolean> status = new HashMap<>();
        status.put(byPath.toString(), false);
        Map<String, Long> warningTimes = new HashMap<>();

        assertFalse(IscsiStorageCleanupMonitor.updateDiskStatusMapWithInactiveIscsiSessions(connect, status, sysBlock, dev,
                warningTimes));
        assertFalse(IscsiStorageCleanupMonitor.updateDiskStatusMapWithInactiveIscsiSessions(connect, status, sysBlock, dev,
                warningTimes));

        assertTrue(warningTimes.containsKey(unknownDisk.toString()));
        assertEquals(1, warningTimes.size());
    }

    @Test
    public void shouldWarnUnresolvedPathAgainAfterTheInterval() {
        Map<String, Long> warningTimes = new HashMap<>();
        long now = 1000L;

        assertTrue(IscsiStorageCleanupMonitor.shouldWarnUnresolvedPath(warningTimes, "disk", now));
        assertFalse(IscsiStorageCleanupMonitor.shouldWarnUnresolvedPath(warningTimes, "disk", now + 1));
        assertTrue(IscsiStorageCleanupMonitor.shouldWarnUnresolvedPath(warningTimes, "disk",
                now + 24L * 60L * 60L * 1000L));
    }

    private String domainXml(Path diskPath) {
        return "<domain><devices><disk type='block' device='disk'>"
                + "<driver name='qemu' type='raw'/>"
                + "<source dev='" + diskPath + "'/>"
                + "<target dev='vda' bus='virtio'/>"
                + "</disk></devices></domain>";
    }

    @Test
    public void isIscsiDiskIgnoresPartitionAliases() {
        String lun = "/dev/disk/by-path/" + BY_PATH;

        assertTrue(IscsiStorageCleanupMonitor.isIscsiDisk(lun));
        assertFalse(IscsiStorageCleanupMonitor.isIscsiDisk(lun + "-part1"));
    }
}
