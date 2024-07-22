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
import java.util.Map;
import java.util.UUID;

import com.cloud.utils.exception.CloudRuntimeException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libvirt.Connect;
import org.libvirt.StoragePool;
import org.libvirt.StoragePoolInfo;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.hypervisor.kvm.resource.LibvirtConnection;
import com.cloud.hypervisor.kvm.resource.LibvirtStoragePoolDef;
import com.cloud.storage.Storage;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtStorageAdaptorTest {

    private MockedStatic<LibvirtConnection> libvirtConnectionMockedStatic;

    private AutoCloseable closeable;

    @Spy
    static LibvirtStorageAdaptor libvirtStorageAdaptor = new LibvirtStorageAdaptor(null);

    @Before
    public void initMocks() {
        closeable = MockitoAnnotations.openMocks(this);
        libvirtConnectionMockedStatic = Mockito.mockStatic(LibvirtConnection.class);
    }

    @After
    public void tearDown() throws Exception {
        libvirtConnectionMockedStatic.close();
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
        StoragePoolInfo spinfo = Mockito.mock(StoragePoolInfo.class);
        Mockito.when(LibvirtConnection.getConnection()).thenReturn(conn);
        Mockito.when(conn.storagePoolLookupByUUIDString(uuid)).thenReturn(sp);
        Mockito.when(sp.isActive()).thenReturn(1);
        Mockito.when(sp.getXMLDesc(0)).thenReturn(poolXml);

        Map<String, String> details = new HashMap<>();
        details.put("nfsmountopts", "vers=4.1, nconnect=4");
        KVMStoragePool pool = libvirtStorageAdaptor.createStoragePool(uuid, null, 0, dir, null, Storage.StoragePoolType.NetworkFilesystem, details);
    }
}
