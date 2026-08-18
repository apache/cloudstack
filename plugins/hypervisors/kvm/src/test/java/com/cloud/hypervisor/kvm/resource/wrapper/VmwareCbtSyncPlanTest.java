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
package com.cloud.hypervisor.kvm.resource.wrapper;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.cloud.agent.api.to.VmwareCbtChangedBlockRangeTO;
import com.cloud.agent.api.to.VmwareCbtDiskTO;

public class VmwareCbtSyncPlanTest {

    @Test
    public void testCreateGroupsChangedBlocksByDisk() {
        VmwareCbtDiskTO disk1 = createDisk("disk-1", "/var/lib/libvirt/images/disk-1.qcow2", 8192);
        VmwareCbtDiskTO disk2 = createDisk("disk-2", "/var/lib/libvirt/images/disk-2.qcow2", 8192);

        VmwareCbtSyncPlan syncPlan = VmwareCbtSyncPlan.create(List.of(disk1, disk2), List.of(
                new VmwareCbtChangedBlockRangeTO("disk-1", 0, 1024),
                new VmwareCbtChangedBlockRangeTO("disk-1", 4096, 512),
                new VmwareCbtChangedBlockRangeTO("disk-2", 2048, 2048)));

        Assert.assertTrue(syncPlan.isValid());
        Assert.assertEquals(3, syncPlan.getChangedRangeCount());
        Assert.assertEquals(3, syncPlan.getCopyRangeCount());
        Assert.assertEquals(3584, syncPlan.getChangedBytes());
        Assert.assertEquals(2, syncPlan.getDiskPlans().size());
        Assert.assertEquals(1536, syncPlan.getDiskPlans().get(0).getChangedBytes());
        Assert.assertEquals(2048, syncPlan.getDiskPlans().get(1).getChangedBytes());
    }

    @Test
    public void testCreateCoalescesAdjacentAndOverlappingRanges() {
        VmwareCbtSyncPlan syncPlan = VmwareCbtSyncPlan.create(List.of(createDisk("disk-1", "/target", 8192)), List.of(
                new VmwareCbtChangedBlockRangeTO("disk-1", 4096, 512),
                new VmwareCbtChangedBlockRangeTO("disk-1", 0, 1024),
                new VmwareCbtChangedBlockRangeTO("disk-1", 1024, 1024),
                new VmwareCbtChangedBlockRangeTO("disk-1", 4352, 1024)));

        Assert.assertTrue(syncPlan.isValid());
        Assert.assertEquals(4, syncPlan.getChangedRangeCount());
        Assert.assertEquals(2, syncPlan.getCopyRangeCount());
        Assert.assertEquals(3328, syncPlan.getChangedBytes());
        Assert.assertEquals(2, syncPlan.getDiskPlans().get(0).getChangedBlocks().size());
        Assert.assertEquals(0, syncPlan.getDiskPlans().get(0).getChangedBlocks().get(0).getStartOffset());
        Assert.assertEquals(2048, syncPlan.getDiskPlans().get(0).getChangedBlocks().get(0).getLength());
        Assert.assertEquals(4096, syncPlan.getDiskPlans().get(0).getChangedBlocks().get(1).getStartOffset());
        Assert.assertEquals(1280, syncPlan.getDiskPlans().get(0).getChangedBlocks().get(1).getLength());
    }

    @Test
    public void testCreateRejectsUnknownDisk() {
        VmwareCbtSyncPlan syncPlan = VmwareCbtSyncPlan.create(List.of(createDisk("disk-1", "/target", 8192)),
                List.of(new VmwareCbtChangedBlockRangeTO("disk-2", 0, 1024)));

        Assert.assertFalse(syncPlan.isValid());
        Assert.assertTrue(syncPlan.getValidationError().contains("unknown disk disk-2"));
    }

    @Test
    public void testCreateRejectsMissingTargetPath() {
        VmwareCbtSyncPlan syncPlan = VmwareCbtSyncPlan.create(List.of(createDisk("disk-1", null, 8192)),
                List.of(new VmwareCbtChangedBlockRangeTO("disk-1", 0, 1024)));

        Assert.assertFalse(syncPlan.isValid());
        Assert.assertTrue(syncPlan.getValidationError().contains("no target path"));
    }

    @Test
    public void testCreateRejectsOutOfBoundsRange() {
        VmwareCbtSyncPlan syncPlan = VmwareCbtSyncPlan.create(List.of(createDisk("disk-1", "/target", 1024)),
                List.of(new VmwareCbtChangedBlockRangeTO("disk-1", 512, 1024)));

        Assert.assertFalse(syncPlan.isValid());
        Assert.assertTrue(syncPlan.getValidationError().contains("exceeds disk capacity"));
    }

    private VmwareCbtDiskTO createDisk(String diskId, String targetPath, long capacityBytes) {
        return new VmwareCbtDiskTO(diskId, 2000, String.format("[%s] vm/%s.vmdk", diskId, diskId),
                "datastore1", targetPath, "qcow2", "*", null, capacityBytes);
    }
}
