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
import org.mockito.Mockito;

import junit.framework.TestCase;

public class KVMPhysicalDiskTest extends TestCase {

    public void testRBDStringBuilder() {
        assertEquals(KVMPhysicalDisk.RBDStringBuilder("ceph-monitor", 8000, "admin", "supersecret", "volume1"),
                     "rbd:volume1:mon_host=ceph-monitor\\:8000:auth_supported=cephx:id=admin:key=supersecret:rbd_default_format=2:client_mount_timeout=30");
    }

    public void testAttributes() {
        String name = "3bc186e0-6c29-45bf-b2b0-ddef6f91f5ef";
        String path = "/" + name;

        LibvirtStoragePool pool = Mockito.mock(LibvirtStoragePool.class);

        KVMPhysicalDisk disk = new KVMPhysicalDisk(path, name, pool);
        assertEquals(disk.getName(), name);
        assertEquals(disk.getPath(), path);
        assertEquals(disk.getPool(), pool);
        assertEquals(disk.getSize(), 0);
        assertEquals(disk.getVirtualSize(), 0);

        disk.setSize(1024);
        disk.setVirtualSize(2048);
        assertEquals(disk.getSize(), 1024);
        assertEquals(disk.getVirtualSize(), 2048);

        disk.setFormat(PhysicalDiskFormat.RAW);
        assertEquals(disk.getFormat(), PhysicalDiskFormat.RAW);
    }
}