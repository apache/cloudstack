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

import org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class KVMPhysicalDiskTest {
    @Mock
    KVMStoragePool kvmStoragePoolMock;

    private final String authUserName = "admin";

    private final String authSecret = "supersecret";

    @Test
    public void testRBDStringBuilder() {
        String monHosts = "ceph-monitor";
        int monPort = 8000;

        Mockito.doReturn(monHosts).when(kvmStoragePoolMock).getSourceHost();
        Mockito.doReturn(monPort).when(kvmStoragePoolMock).getSourcePort();
        Mockito.doReturn(authUserName).when(kvmStoragePoolMock).getAuthUserName();
        Mockito.doReturn(authSecret).when(kvmStoragePoolMock).getAuthSecret();

        String expected = "rbd:volume1:mon_host=ceph-monitor\\:8000:auth_supported=cephx:id=admin:key=supersecret:rbd_default_format=2:client_mount_timeout=30";
        String result = KVMPhysicalDisk.RBDStringBuilder(kvmStoragePoolMock, "volume1");

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testRBDStringBuilder2() {
        String monHosts = "ceph-monitor1,ceph-monitor2,ceph-monitor3";
        int monPort = 3300;

        Mockito.doReturn(monHosts).when(kvmStoragePoolMock).getSourceHost();
        Mockito.doReturn(monPort).when(kvmStoragePoolMock).getSourcePort();
        Mockito.doReturn(authUserName).when(kvmStoragePoolMock).getAuthUserName();
        Mockito.doReturn(authSecret).when(kvmStoragePoolMock).getAuthSecret();

        String expected = "rbd:volume1:" +
                "mon_host=ceph-monitor1\\:3300\\;ceph-monitor2\\:3300\\;ceph-monitor3\\:3300:" +
                "auth_supported=cephx:id=admin:key=supersecret:rbd_default_format=2:client_mount_timeout=30";
        String actualResult = KVMPhysicalDisk.RBDStringBuilder(kvmStoragePoolMock, "volume1");

        Assert.assertEquals(expected, actualResult);
    }

    @Test
    public void testRBDStringBuilder3() {
        String monHosts = "[fc00:1234::1],[fc00:1234::2],[fc00:1234::3]";
        int monPort = 3300;

        Mockito.doReturn(monHosts).when(kvmStoragePoolMock).getSourceHost();
        Mockito.doReturn(monPort).when(kvmStoragePoolMock).getSourcePort();
        Mockito.doReturn(authUserName).when(kvmStoragePoolMock).getAuthUserName();
        Mockito.doReturn(authSecret).when(kvmStoragePoolMock).getAuthSecret();

        String expected = "rbd:volume1:" +
                "mon_host=[fc00\\:1234\\:\\:1]\\:3300\\;[fc00\\:1234\\:\\:2]\\:3300\\;[fc00\\:1234\\:\\:3]\\:3300:" +
                "auth_supported=cephx:id=admin:key=supersecret:rbd_default_format=2:client_mount_timeout=30";
        String actualResult = KVMPhysicalDisk.RBDStringBuilder(kvmStoragePoolMock, "volume1");

        Assert.assertEquals(expected, actualResult);
    }

    @Test
    public void testAttributes() {
        String name = "3bc186e0-6c29-45bf-b2b0-ddef6f91f5ef";
        String path = "/" + name;

        LibvirtStoragePool pool = Mockito.mock(LibvirtStoragePool.class);

        KVMPhysicalDisk disk = new KVMPhysicalDisk(path, name, pool);
        Assert.assertEquals(disk.getName(), name);
        Assert.assertEquals(disk.getPath(), path);
        Assert.assertEquals(disk.getPool(), pool);
        Assert.assertEquals(disk.getSize(), 0);
        Assert.assertEquals(disk.getVirtualSize(), 0);

        disk.setSize(1024);
        disk.setVirtualSize(2048);
        Assert.assertEquals(disk.getSize(), 1024);
        Assert.assertEquals(disk.getVirtualSize(), 2048);

        disk.setFormat(PhysicalDiskFormat.RAW);
        Assert.assertEquals(disk.getFormat(), PhysicalDiskFormat.RAW);
    }
}
