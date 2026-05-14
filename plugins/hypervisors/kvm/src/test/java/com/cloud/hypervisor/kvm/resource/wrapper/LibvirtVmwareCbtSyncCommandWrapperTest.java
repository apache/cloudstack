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

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.VmwareCbtMigrationAnswer;
import com.cloud.agent.api.VmwareCbtSyncCommand;
import com.cloud.agent.api.to.VmwareCbtChangedBlockRangeTO;
import com.cloud.agent.api.to.VmwareCbtDiskTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtVmwareCbtSyncCommandWrapperTest {

    private final LibvirtVmwareCbtSyncCommandWrapper wrapper = new LibvirtVmwareCbtSyncCommandWrapper();

    @Mock
    private LibvirtComputingResource libvirtComputingResource;

    @Before
    public void setUp() {
        Mockito.when(libvirtComputingResource.hostSupportsVmwareCbtMigration(Mockito.isNull())).thenReturn(true);
    }

    @Test
    public void testExecuteNoChangedBlocksReturnsReadyForCutover() {
        VmwareCbtSyncCommand command = createCommand(Collections.emptyList(), Collections.emptyList());

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        Assert.assertTrue(answer.getResult());
        Assert.assertTrue(((VmwareCbtMigrationAnswer) answer).getReadyForCutover());
    }

    @Test
    public void testExecuteRejectsChangedBlocksWithoutTargetPath() {
        VmwareCbtDiskTO disk = createDisk("disk-1", null, 8192);
        VmwareCbtSyncCommand command = createCommand(List.of(disk),
                List.of(new VmwareCbtChangedBlockRangeTO("disk-1", 0, 1024)));

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        Assert.assertFalse(answer.getResult());
        Assert.assertTrue(answer.getDetails().contains("no target path"));
    }

    @Test
    public void testExecuteReportsValidatedChangedBlocks() {
        VmwareCbtDiskTO disk = createDisk("disk-1", "/var/lib/libvirt/images/disk-1.qcow2", 8192);
        VmwareCbtSyncCommand command = createCommand(List.of(disk),
                List.of(new VmwareCbtChangedBlockRangeTO("disk-1", 0, 1024)));

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        Assert.assertFalse(answer.getResult());
        Assert.assertEquals(1024, ((VmwareCbtMigrationAnswer) answer).getChangedBytes());
        Assert.assertTrue(answer.getDetails().contains("validated 1 changed block range"));
    }

    @Test
    public void testExecuteUsesCommandVddkLibDirOverrideForSupportCheck() {
        VmwareCbtDiskTO disk = createDisk("disk-1", "/var/lib/libvirt/images/disk-1.qcow2", 8192);
        VmwareCbtSyncCommand command = createCommand(List.of(disk),
                List.of(new VmwareCbtChangedBlockRangeTO("disk-1", 0, 1024)));
        command.setVddkLibDir("/opt/vmware-vddk/override");
        Mockito.when(libvirtComputingResource.hostSupportsVmwareCbtMigration("/opt/vmware-vddk/override"))
                .thenReturn(true);

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        Assert.assertFalse(answer.getResult());
        Assert.assertTrue(answer.getDetails().contains("validated 1 changed block range"));
        Mockito.verify(libvirtComputingResource).hostSupportsVmwareCbtMigration("/opt/vmware-vddk/override");
    }

    private VmwareCbtSyncCommand createCommand(List<VmwareCbtDiskTO> disks, List<VmwareCbtChangedBlockRangeTO> changedBlocks) {
        return new VmwareCbtSyncCommand("migration-uuid", null, disks, changedBlocks, 1, "snapshot-1", false);
    }

    private VmwareCbtDiskTO createDisk(String diskId, String targetPath, long capacityBytes) {
        return new VmwareCbtDiskTO(diskId, 2000, String.format("[%s] vm/%s.vmdk", diskId, diskId),
                "datastore1", targetPath, "qcow2", "*", null, capacityBytes);
    }
}
