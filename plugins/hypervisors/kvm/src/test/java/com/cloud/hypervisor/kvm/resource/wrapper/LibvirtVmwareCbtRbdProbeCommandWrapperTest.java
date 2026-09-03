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

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.VmwareCbtRbdProbeCommand;
import com.cloud.agent.api.to.VmwareCbtTargetStorageType;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.storage.Storage;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtVmwareCbtRbdProbeCommandWrapperTest {

    private static final String POOL_UUID = "rbd-pool-uuid";
    private static final String PROBE_IMAGE = "cloudstack-cbt-probe-11111111-2222-3333-4444-555555555555";

    private final TestLibvirtVmwareCbtRbdProbeCommandWrapper wrapper = new TestLibvirtVmwareCbtRbdProbeCommandWrapper();

    @Mock
    private LibvirtComputingResource libvirtComputingResource;
    @Mock
    private KVMStoragePoolManager storagePoolManager;
    @Mock
    private KVMStoragePool rbdStoragePool;

    @Before
    public void setUp() {
        Mockito.when(libvirtComputingResource.getPrivateIp()).thenReturn("10.0.0.10");
        Mockito.when(libvirtComputingResource.getStoragePoolMgr()).thenReturn(storagePoolManager);
        Mockito.when(storagePoolManager.getStoragePool(Storage.StoragePoolType.RBD, POOL_UUID)).thenReturn(rbdStoragePool);
        Mockito.when(rbdStoragePool.getType()).thenReturn(Storage.StoragePoolType.RBD);
        Mockito.when(rbdStoragePool.getUuid()).thenReturn(POOL_UUID);
        Mockito.when(rbdStoragePool.getSourceDir()).thenReturn("cloudstack");
        Mockito.when(rbdStoragePool.getSourceHost()).thenReturn("10.0.0.20,10.0.0.21");
        Mockito.when(rbdStoragePool.getSourcePort()).thenReturn(6789);
        Mockito.when(rbdStoragePool.getAuthUserName()).thenReturn("cloudstack");
        Mockito.when(rbdStoragePool.getAuthSecret()).thenReturn("secret");
    }

    @Test
    public void testExecuteCreatesWritesReadsAndCleansTemporaryRbdImage() {
        Answer answer = wrapper.execute(createCommand(), libvirtComputingResource);

        Assert.assertTrue(answer.getDetails(), answer.getResult());
        Assert.assertEquals(2, wrapper.commands.size());
        Assert.assertTrue(wrapper.commands.get(0), wrapper.commands.get(0).contains("qemu-img create -f raw"));
        Assert.assertTrue(wrapper.commands.get(0), wrapper.commands.get(0).contains("rbd:cloudstack/" + PROBE_IMAGE));
        Assert.assertTrue(wrapper.commands.get(1), wrapper.commands.get(1).contains("qemu-io -f raw"));
        Assert.assertTrue(wrapper.cleanedImages.contains(PROBE_IMAGE));
    }

    @Test
    public void testExecuteFailsForNonRbdCommand() {
        VmwareCbtRbdProbeCommand command = new VmwareCbtRbdProbeCommand(Storage.StoragePoolType.NetworkFilesystem,
                POOL_UUID, PROBE_IMAGE);

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        Assert.assertFalse(answer.getResult());
        Assert.assertTrue(answer.getDetails(), answer.getDetails().contains("requires an RBD destination storage pool"));
    }

    @Test
    public void testExecuteFailsForUnsafeProbeImageName() {
        VmwareCbtRbdProbeCommand command = new VmwareCbtRbdProbeCommand(Storage.StoragePoolType.RBD,
                POOL_UUID, "not-cloudstack-owned");

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        Assert.assertFalse(answer.getResult());
        Assert.assertTrue(answer.getDetails(), answer.getDetails().contains("probe image name must match"));
    }

    @Test
    public void testExecuteCreatesWritesReadsAndCleansTemporaryBlockDeviceVolume() {
        KVMStoragePool linstorStoragePool = Mockito.mock(KVMStoragePool.class);
        com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk probeDisk = Mockito.mock(com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk.class);
        Mockito.when(storagePoolManager.getStoragePool(Storage.StoragePoolType.Linstor, "linstor-pool-uuid")).thenReturn(linstorStoragePool);
        Mockito.when(linstorStoragePool.getType()).thenReturn(Storage.StoragePoolType.Linstor);
        Mockito.when(probeDisk.getPath()).thenReturn("/dev/drbd/by-res/cs-cbt-probe-1234abcd/0");
        Mockito.when(linstorStoragePool.createPhysicalDisk(Mockito.eq("cbt-probe-1234abcd"), Mockito.any(), Mockito.any(),
                Mockito.anyLong(), Mockito.isNull())).thenReturn(probeDisk);

        VmwareCbtRbdProbeCommand command = new VmwareCbtRbdProbeCommand(Storage.StoragePoolType.Linstor,
                "linstor-pool-uuid", "cbt-probe-1234abcd");
        command.setTargetStorageType(VmwareCbtTargetStorageType.RAW_BLOCK_DEVICE);

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        Assert.assertTrue(answer.getDetails(), answer.getResult());
        Assert.assertEquals(1, wrapper.commands.size());
        Assert.assertTrue(wrapper.commands.get(0), wrapper.commands.get(0).contains("qemu-io -f raw"));
        Assert.assertTrue(wrapper.commands.get(0), wrapper.commands.get(0).contains("/dev/drbd/by-res/cs-cbt-probe-1234abcd/0"));
        Assert.assertTrue(wrapper.cleanedImages.contains("cbt-probe-1234abcd"));
    }

    @Test
    public void testExecuteBlockDeviceProbeFailsForUnsafeName() {
        VmwareCbtRbdProbeCommand command = new VmwareCbtRbdProbeCommand(Storage.StoragePoolType.Linstor,
                "linstor-pool-uuid", "not-cloudstack-owned");
        command.setTargetStorageType(VmwareCbtTargetStorageType.RAW_BLOCK_DEVICE);

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        Assert.assertFalse(answer.getResult());
        Assert.assertTrue(answer.getDetails(), answer.getDetails().contains("probe volume name must match"));
    }

    @Test
    public void testExecuteReportsQemuFailureAndStillAttemptsCleanup() {
        wrapper.exitValue = 1;

        Answer answer = wrapper.execute(createCommand(), libvirtComputingResource);

        Assert.assertFalse(answer.getResult());
        Assert.assertTrue(answer.getDetails(), answer.getDetails().contains("qemu RBD probe create failed"));
        Assert.assertTrue(wrapper.cleanedImages.contains(PROBE_IMAGE));
    }

    private VmwareCbtRbdProbeCommand createCommand() {
        return new VmwareCbtRbdProbeCommand(Storage.StoragePoolType.RBD, POOL_UUID, PROBE_IMAGE);
    }

    private static class TestLibvirtVmwareCbtRbdProbeCommandWrapper extends LibvirtVmwareCbtRbdProbeCommandWrapper {
        private final List<String> commands = new ArrayList<>();
        private final List<String> cleanedImages = new ArrayList<>();
        private int exitValue;

        @Override
        protected int runBashCommand(String command, long timeout) {
            commands.add(command);
            return exitValue;
        }

        @Override
        protected void cleanupProbeImage(KVMStoragePool targetPool, String probeImageName) {
            cleanedImages.add(probeImageName);
        }
    }
}
