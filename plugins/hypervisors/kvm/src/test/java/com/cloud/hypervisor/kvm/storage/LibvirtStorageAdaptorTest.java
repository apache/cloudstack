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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
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
import com.cloud.storage.StorageLayer;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import org.libvirt.LibvirtException;

import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtStorageAdaptorTest {

    private MockedStatic<LibvirtConnection> libvirtConnectionMockedStatic;

    private AutoCloseable closeable;

    @Mock
    LibvirtStoragePool mockPool;

    @Mock
    StorageLayer storageLayer;

    MockedStatic<Script> mockScript;

    @Spy
    static LibvirtStorageAdaptor libvirtStorageAdaptor = new LibvirtStorageAdaptor(null);

    @Before
    public void initMocks() {
        closeable = MockitoAnnotations.openMocks(this);
        libvirtConnectionMockedStatic = Mockito.mockStatic(LibvirtConnection.class);
        Mockito.reset(mockPool);
        mockScript = Mockito.mockStatic(Script.class);
        ReflectionTestUtils.setField(libvirtStorageAdaptor, "_storageLayer", storageLayer);
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

    /**
     * Creates a {@link LibvirtException} via reflection since its only constructor is package-private.
     */
    private static LibvirtException createLibvirtException(String message) throws Exception {
        org.libvirt.Error virError = Mockito.mock(org.libvirt.Error.class);
        Mockito.when(virError.getMessage()).thenReturn(message);
        java.lang.reflect.Constructor<LibvirtException> ctor =
                LibvirtException.class.getDeclaredConstructor(org.libvirt.Error.class);
        ctor.setAccessible(true);
        return ctor.newInstance(virError);
    }

    private String buildNfsPoolXml(String uuid, String host, String dir, String targetPath) {
        return "<pool type='netfs'>\n" +
                "<name>" + uuid + "</name>\n<uuid>" + uuid + "</uuid>\n" +
                "<source>\n<host name='" + host + "'/>\n<dir path='" + dir + "'/>\n</source>\n" +
                "<target>\n<path>" + targetPath + "</path>\n</target>\n</pool>\n";
    }

    /**
     * NFS pool exists with same host and path: should reuse it without destroying.
     */
    @Test
    public void testCreateStoragePool_NFS_SameHostAndPath_ReusesPool() throws Exception {
        String uuid = UUID.randomUUID().toString();
        String host = "10.0.0.1";
        String path = "/export/primary";
        String targetPath = "/mnt/" + uuid;
        String poolXml = buildNfsPoolXml(uuid, host, path, targetPath);

        Connect conn = Mockito.mock(Connect.class);
        StoragePool sp = Mockito.mock(StoragePool.class);
        Mockito.when(LibvirtConnection.getConnection()).thenReturn(conn);
        Mockito.when(conn.storagePoolLookupByUUIDString(uuid)).thenReturn(sp);
        Mockito.when(sp.isActive()).thenReturn(1);
        Mockito.when(sp.getXMLDesc(0)).thenReturn(poolXml);
        Mockito.when(Script.runSimpleBashScriptForExitValue(anyString())).thenReturn(0);
        Mockito.doReturn(Mockito.mock(KVMStoragePool.class)).when(libvirtStorageAdaptor).getStoragePool(uuid);

        libvirtStorageAdaptor.createStoragePool(uuid, host, 0, path, null,
                Storage.StoragePoolType.NetworkFilesystem, null, true);

        // pool was not destroyed since source didn't change
        Mockito.verify(sp, Mockito.never()).destroy();
        Mockito.verify(sp, Mockito.never()).undefine();
    }

    /**
     * NFS pool exists but path has changed: should destroy and recreate.
     */
    @Test
    public void testCreateStoragePool_NFS_PathChanged_DestroysAndRecreates() throws Exception {
        String uuid = UUID.randomUUID().toString();
        String host = "10.0.0.1";
        String oldPath = "/export/old";
        String newPath = "/export/new";
        String targetPath = "/mnt/" + uuid;
        String poolXml = buildNfsPoolXml(uuid, host, oldPath, targetPath);

        Connect conn = Mockito.mock(Connect.class);
        StoragePool sp = Mockito.mock(StoragePool.class);
        Mockito.when(LibvirtConnection.getConnection()).thenReturn(conn);
        Mockito.when(conn.storagePoolLookupByUUIDString(uuid)).thenReturn(sp);
        Mockito.when(sp.isActive()).thenReturn(1);
        Mockito.when(sp.getXMLDesc(0)).thenReturn(poolXml);
        Mockito.when(sp.isPersistent()).thenReturn(1);
        Mockito.when(conn.listStoragePools()).thenReturn(new String[]{});
        StoragePool newSp = Mockito.mock(StoragePool.class);
        Mockito.when(conn.storagePoolCreateXML(anyString(), Mockito.eq(0))).thenReturn(newSp);
        Mockito.when(Script.runSimpleBashScriptForExitValue(anyString())).thenReturn(0);
        Mockito.doReturn(Mockito.mock(KVMStoragePool.class)).when(libvirtStorageAdaptor).getStoragePool(uuid);

        libvirtStorageAdaptor.createStoragePool(uuid, host, 0, newPath, null,
                Storage.StoragePoolType.NetworkFilesystem, null, true);

        Mockito.verify(sp).destroy();
        Mockito.verify(sp).undefine();
        Mockito.verify(sp).free();
    }

    /**
     * NFS pool exists but host has changed: should destroy and recreate.
     */
    @Test
    public void testCreateStoragePool_NFS_HostChanged_DestroysAndRecreates() throws Exception {
        String uuid = UUID.randomUUID().toString();
        String oldHost = "10.0.0.1";
        String newHost = "10.0.0.2";
        String path = "/export/primary";
        String targetPath = "/mnt/" + uuid;
        String poolXml = buildNfsPoolXml(uuid, oldHost, path, targetPath);

        Connect conn = Mockito.mock(Connect.class);
        StoragePool sp = Mockito.mock(StoragePool.class);
        Mockito.when(LibvirtConnection.getConnection()).thenReturn(conn);
        Mockito.when(conn.storagePoolLookupByUUIDString(uuid)).thenReturn(sp);
        Mockito.when(sp.isActive()).thenReturn(1);
        Mockito.when(sp.getXMLDesc(0)).thenReturn(poolXml);
        Mockito.when(sp.isPersistent()).thenReturn(1);
        Mockito.when(conn.listStoragePools()).thenReturn(new String[]{});
        StoragePool newSp = Mockito.mock(StoragePool.class);
        Mockito.when(conn.storagePoolCreateXML(anyString(), Mockito.eq(0))).thenReturn(newSp);
        Mockito.when(Script.runSimpleBashScriptForExitValue(anyString())).thenReturn(0);
        Mockito.doReturn(Mockito.mock(KVMStoragePool.class)).when(libvirtStorageAdaptor).getStoragePool(uuid);

        libvirtStorageAdaptor.createStoragePool(uuid, newHost, 0, path, null,
                Storage.StoragePoolType.NetworkFilesystem, null, true);

        Mockito.verify(sp).destroy();
        Mockito.verify(sp).undefine();
        Mockito.verify(sp).free();
    }

    /**
     * RBD pool exists with different source — should NOT destroy (RBD is excluded from this logic).
     */
    @Test
    public void testCreateStoragePool_RBD_SourceChanged_DoesNotDestroy() throws Exception {
        String uuid = UUID.randomUUID().toString();
        String rbdPoolXml = "<pool type='rbd'>\n<name>" + uuid + "</name>\n<uuid>" + uuid + "</uuid>\n" +
                "<source>\n<host name='10.0.0.1'/>\n<name>oldpool</name>\n</source>\n</pool>\n";

        Connect conn = Mockito.mock(Connect.class);
        StoragePool sp = Mockito.mock(StoragePool.class);
        Mockito.when(LibvirtConnection.getConnection()).thenReturn(conn);
        Mockito.when(conn.storagePoolLookupByUUIDString(uuid)).thenReturn(sp);
        Mockito.when(sp.isActive()).thenReturn(1);
        Mockito.doReturn(Mockito.mock(KVMStoragePool.class)).when(libvirtStorageAdaptor).getStoragePool(uuid);

        libvirtStorageAdaptor.createStoragePool(uuid, "10.0.0.2", 6789, "newpool", "user:secret",
                Storage.StoragePoolType.RBD, null, true);

        Mockito.verify(sp, Mockito.never()).destroy();
        Mockito.verify(sp, Mockito.never()).undefine();
    }

    /**
     * NFS pool exists, destroy fails — pool should be reused (not crash), sp.free() not called.
     */
    @Test
    public void testCreateStoragePool_NFS_DestroyFails_ReusesExistingPool() throws Exception {
        String uuid = UUID.randomUUID().toString();
        String host = "10.0.0.1";
        String oldPath = "/export/old";
        String newPath = "/export/new";
        String targetPath = "/mnt/" + uuid;
        String poolXml = buildNfsPoolXml(uuid, host, oldPath, targetPath);

        Connect conn = Mockito.mock(Connect.class);
        StoragePool sp = Mockito.mock(StoragePool.class);
        Mockito.when(LibvirtConnection.getConnection()).thenReturn(conn);
        Mockito.when(conn.storagePoolLookupByUUIDString(uuid)).thenReturn(sp);
        Mockito.when(sp.isActive()).thenReturn(1);
        Mockito.when(sp.getXMLDesc(0)).thenReturn(poolXml);
        Mockito.when(sp.isPersistent()).thenReturn(1);
        LibvirtException libvirtException = createLibvirtException("pool is busy");
        Mockito.doThrow(libvirtException).when(sp).destroy();
        Mockito.when(Script.runSimpleBashScriptForExitValue(anyString())).thenReturn(0);
        Mockito.doReturn(Mockito.mock(KVMStoragePool.class)).when(libvirtStorageAdaptor).getStoragePool(uuid);

        libvirtStorageAdaptor.createStoragePool(uuid, host, 0, newPath, null,
                Storage.StoragePoolType.NetworkFilesystem, null, true);

        Mockito.verify(sp).destroy();
        Mockito.verify(sp, Mockito.never()).free();
    }
}
