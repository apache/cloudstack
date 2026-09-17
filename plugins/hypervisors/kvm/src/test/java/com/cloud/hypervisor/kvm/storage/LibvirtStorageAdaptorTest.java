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

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libvirt.Connect;
import org.libvirt.StoragePool;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.hypervisor.kvm.resource.LibvirtConnection;
import com.cloud.hypervisor.kvm.resource.LibvirtStoragePoolDef;
import com.cloud.storage.Storage;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtStorageAdaptorTest {

    private MockedStatic<LibvirtConnection> libvirtConnectionMockedStatic;

    private AutoCloseable closeable;

    @Mock
    LibvirtStoragePool mockPool;

    MockedStatic<Script> mockScript;

    @Spy
    static LibvirtStorageAdaptor libvirtStorageAdaptor = new LibvirtStorageAdaptor(null);

    @Before
    public void initMocks() {
        closeable = MockitoAnnotations.openMocks(this);
        libvirtConnectionMockedStatic = Mockito.mockStatic(LibvirtConnection.class);
        Mockito.reset(mockPool);
        mockScript = Mockito.mockStatic(Script.class);
    }

    @After
    public void tearDown() throws Exception {
        libvirtConnectionMockedStatic.close();
        mockScript.close();
        closeable.close();
    }

    @Test(expected = CloudRuntimeException.class)
    public void testCreateStoragePoolWithNFSMountOpts() throws Exception {
        LibvirtStoragePoolDef.PoolType type = LibvirtStoragePoolDef.PoolType.NETFS;
        String name = "Primary1";
        String uuid = String.valueOf(UUID.randomUUID());
        String host = "127.0.0.1";
        String dir  = "/export/primary";
        String targetPath = "/mnt/" + uuid;

        String poolXml = "<pool type='" + type.toString() + "' xmlns:fs='http://libvirt.org/schemas/storagepool/fs/1.0'>\n" +
                "<name>" +name + "</name>\n<uuid>" + uuid + "</uuid>\n" +
                "<source>\n<host name='" + host + "'/>\n<dir path='" + dir + "'/>\n</source>\n<target>\n" +
                "<path>" + targetPath + "</path>\n</target>\n" +
                "<fs:mount_opts>\n<fs:option name='vers=4.1'/>\n<fs:option name='nconnect=4'/>\n</fs:mount_opts>\n</pool>\n";

        Connect conn =  Mockito.mock(Connect.class);
        StoragePool sp = Mockito.mock(StoragePool.class);
        Mockito.when(LibvirtConnection.getConnection()).thenReturn(conn);
        Mockito.when(conn.storagePoolLookupByUUIDString(uuid)).thenReturn(sp);
        Mockito.when(sp.isActive()).thenReturn(1);
        Mockito.when(sp.getXMLDesc(0)).thenReturn(poolXml);
        Mockito.when(Script.runSimpleBashScriptForExitValue(anyString())).thenReturn(-1);

        Map<String, String> details = new HashMap<>();
        details.put("nfsmountopts", "vers=4.1, nconnect=4");
        KVMStoragePool pool = libvirtStorageAdaptor.createStoragePool(uuid, null, 0, dir, null, Storage.StoragePoolType.NetworkFilesystem, details, true);
    }

    @Test
    public void testUpdateLocalPoolIops_IgnoredForNonFilesystemType() {
        Mockito.when(mockPool.getType()).thenReturn(Storage.StoragePoolType.SharedMountPoint);

        libvirtStorageAdaptor.updateLocalPoolIops(mockPool);

        libvirtStorageAdaptor.updateLocalPoolIops(mockPool);
    }

    @Test
    public void testUpdateLocalPoolIops_IgnoredForBlankLocalPath() {
        Mockito.when(mockPool.getType()).thenReturn(Storage.StoragePoolType.Filesystem);
        Mockito.when(mockPool.getLocalPath()).thenReturn("");

        Mockito.verify(mockPool, never()).getLocalPath();
        libvirtStorageAdaptor.updateLocalPoolIops(mockPool);

        Mockito.verify(mockPool, never()).setUsedIops(anyLong());
    }

    @Test
    public void testUpdateLocalPoolIops_NoDevice() {
        Mockito.when(mockPool.getType()).thenReturn(Storage.StoragePoolType.Filesystem);
        Mockito.when(mockPool.getLocalPath()).thenReturn("/mock/path");
        Mockito.when(mockPool.getName()).thenReturn("mockPool");
        Mockito.when(Script.executePipedCommands(anyList(), Mockito.eq(1000L))).thenReturn(new Pair<>(0, "\n"));

        libvirtStorageAdaptor.updateLocalPoolIops(mockPool);

        Mockito.verify(mockPool, never()).setUsedIops(anyLong());
    }

    @Test
    public void testUpdateLocalPoolIops_SuccessfulUpdate() {
        Mockito.when(mockPool.getType()).thenReturn(Storage.StoragePoolType.Filesystem);
        Mockito.when(mockPool.getLocalPath()).thenReturn("/mock/path");
        Mockito.when(mockPool.getName()).thenReturn("mockPool");
        Mockito.when(Script.executePipedCommands(anyList(), Mockito.eq(1000L))).thenReturn(new Pair<>(0, "sda\n"));
        Mockito.when(Script.executePipedCommands(anyList(), Mockito.eq(10000L))).thenReturn(new Pair<>(0, "42\n"));

        libvirtStorageAdaptor.updateLocalPoolIops(mockPool);

        Mockito.verify(mockPool).setUsedIops(42L);
    }

    @Test
    public void testUpdateLocalPoolIops_HandlesNumberFormatException() {
        Mockito.when(mockPool.getType()).thenReturn(Storage.StoragePoolType.Filesystem);
        Mockito.when(mockPool.getLocalPath()).thenReturn("/mock/path");
        Mockito.when(mockPool.getName()).thenReturn("mockPool");
        Mockito.when(Script.executePipedCommands(anyList(), Mockito.eq(1000L))).thenReturn(new Pair<>(0, "sda\n"));
        Mockito.when(Script.executePipedCommands(anyList(), Mockito.eq(10000L)))
                .thenReturn(new Pair<>(0, "invalid_number"));

        libvirtStorageAdaptor.updateLocalPoolIops(mockPool);

        Mockito.verify(mockPool, never()).setUsedIops(anyLong());
    }

    @Test
    public void testUpdateLocalPoolIops_NullResultFromScript() {
        Mockito.when(mockPool.getType()).thenReturn(Storage.StoragePoolType.Filesystem);
        Mockito.when(mockPool.getLocalPath()).thenReturn("/mock/path");
        Mockito.when(mockPool.getName()).thenReturn("mockPool");
        Mockito.when(Script.executePipedCommands(anyList(), Mockito.eq(1000L))).thenReturn(new Pair<>(0, "sda\n"));
        Mockito.when(Script.executePipedCommands(anyList(), Mockito.eq(10000L)))
                .thenReturn(new Pair<>(0, null));

        libvirtStorageAdaptor.updateLocalPoolIops(mockPool);

        Mockito.verify(mockPool, never()).setUsedIops(anyLong());
    }

    @Test(timeout = 120000)
    public void testStoragePoolRefCountCountsEveryConcurrentIncrement() throws Exception {
        final LibvirtStorageAdaptor adaptor = new LibvirtStorageAdaptor(null);
        final int threads = 16;
        final int rounds = 500;
        final CyclicBarrier barrier = new CyclicBarrier(threads);
        final ExecutorService executor = Executors.newFixedThreadPool(threads);

        try {
            for (int round = 0; round < rounds; round++) {
                // A fresh uuid each round, so every round starts with no entry for the pool.
                final String uuid = String.valueOf(UUID.randomUUID());
                final List<Future<?>> futures = new ArrayList<>();

                for (int i = 0; i < threads; i++) {
                    futures.add(executor.submit(() -> {
                        /*
                         * Every caller arrives with its own String instance, the way the
                         * agent does when the uuid is parsed out of a separate command
                         * payload for each request. The instances are equal but they are
                         * not the same object.
                         */
                        final String ownInstance = new String(uuid);
                        try {
                            barrier.await();
                        } catch (InterruptedException | BrokenBarrierException e) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException(e);
                        }
                        adaptor.incStoragePoolRefCount(ownInstance);
                    }));
                }
                for (Future<?> future : futures) {
                    future.get(60, TimeUnit.SECONDS);
                }

                // Every increment must be counted, so the pool stays in use until the last release.
                for (int i = 1; i < threads; i++) {
                    Assert.assertTrue("Round " + round + ": pool should still be in use after " + i
                            + " of " + threads + " releases", adaptor.decStoragePoolRefCount(uuid));
                }
                Assert.assertFalse("Round " + round + ": pool should no longer be in use after the last release",
                        adaptor.decStoragePoolRefCount(uuid));
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
