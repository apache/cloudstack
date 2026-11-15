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
package com.cloud.hypervisor.vmware.manager;

import java.util.Arrays;
import java.util.Collections;

import org.apache.cloudstack.vm.VmwareCbtDiskInfo;
import org.junit.Assert;
import org.junit.Test;

import com.cloud.utils.exception.CloudRuntimeException;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDisk;
import com.vmware.vim25.VirtualDiskFlatVer2BackingInfo;

public class VmwareCbtMigrationServiceImplTest {

    private final VmwareCbtMigrationServiceImpl service = new VmwareCbtMigrationServiceImpl();

    @Test
    public void getSnapshotDiskChangeIdUsesMatchingDiskBacking() {
        VirtualDisk otherDisk = createDisk(2001, "other-snapshot");
        VirtualDisk sourceDisk = createDisk(2000, "cycle-2-snapshot");
        VmwareCbtDiskInfo diskInfo = createDiskInfo(2000);

        String changeId = service.getSnapshotDiskChangeId(Arrays.<VirtualDevice>asList(otherDisk, sourceDisk), diskInfo);

        Assert.assertEquals("cycle-2-snapshot", changeId);
        Assert.assertNotEquals(diskInfo.getChangeId(), changeId);
    }

    @Test(expected = CloudRuntimeException.class)
    public void getSnapshotDiskChangeIdRejectsMissingDisk() {
        service.getSnapshotDiskChangeId(Collections.singletonList(createDisk(2001, "other-snapshot")),
                createDiskInfo(2000));
    }

    @Test(expected = CloudRuntimeException.class)
    public void getSnapshotDiskChangeIdRejectsBlankChangeId() {
        service.getSnapshotDiskChangeId(Collections.singletonList(createDisk(2000, null)), createDiskInfo(2000));
    }

    private VmwareCbtDiskInfo createDiskInfo(int key) {
        return new VmwareCbtDiskInfo("disk-1", key, null, null, null, 1024L, "baseline");
    }

    private VirtualDisk createDisk(int key, String changeId) {
        VirtualDisk disk = new VirtualDisk();
        disk.setKey(key);
        VirtualDiskFlatVer2BackingInfo backing = new VirtualDiskFlatVer2BackingInfo();
        backing.setChangeId(changeId);
        disk.setBacking(backing);
        return disk;
    }
}
