//
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
//

package com.cloud.hypervisor.kvm.resource.wrapper;

import java.util.Arrays;
import java.util.List;

import org.apache.cloudstack.backup.RemoveBackupAnchorsCommand;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.storage.Storage;
import com.cloud.utils.Pair;
import com.cloud.utils.script.Script;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtRemoveBackupAnchorsCommandWrapperTest {

    private final LibvirtRemoveBackupAnchorsCommandWrapper wrapper = new LibvirtRemoveBackupAnchorsCommandWrapper();

    private PrimaryDataStoreTO poolOfType(Storage.StoragePoolType type) {
        PrimaryDataStoreTO pool = Mockito.mock(PrimaryDataStoreTO.class);
        Mockito.lenient().when(pool.getPoolType()).thenReturn(type);
        Mockito.lenient().when(pool.getUuid()).thenReturn("pool-uuid");
        return pool;
    }

    private LibvirtComputingResource resourceWithRbdPool() {
        LibvirtComputingResource resource = Mockito.mock(LibvirtComputingResource.class);
        KVMStoragePoolManager poolManager = Mockito.mock(KVMStoragePoolManager.class);
        KVMStoragePool pool = Mockito.mock(KVMStoragePool.class);
        Mockito.lenient().when(pool.getSourceDir()).thenReturn("rbd-pool");
        Mockito.lenient().when(pool.getAuthUserName()).thenReturn(null);
        Mockito.lenient().when(pool.getSourceHost()).thenReturn("10.0.0.1");
        Mockito.lenient().when(pool.getSourcePort()).thenReturn(6789);
        Mockito.lenient().when(poolManager.getStoragePool(Mockito.any(), Mockito.anyString())).thenReturn(pool);
        Mockito.lenient().when(resource.getStoragePoolMgr()).thenReturn(poolManager);
        Mockito.lenient().when(resource.getNasBackupPath()).thenReturn("/usr/share/cloudstack-common/scripts/vm/hypervisor/kvm/nasbackup.sh");
        Mockito.lenient().when(resource.getCmdsTimeout()).thenReturn(60);
        return resource;
    }

    /**
     * The volumes of an RBD pool are the ones that carry an anchor, and are passed to the script
     * as the rbd disk strings it needs to reach the images.
     */
    @Test
    public void anRbdVolumeIsPassedToTheScript() {
        RemoveBackupAnchorsCommand command = new RemoveBackupAnchorsCommand(
                List.of(poolOfType(Storage.StoragePoolType.RBD)), List.of("volume-path"));

        try (MockedStatic<Script> script = Mockito.mockStatic(Script.class)) {
            script.when(() -> Script.executePipedCommands(Mockito.anyList(), Mockito.anyLong()))
                    .thenReturn(new Pair<>(0, ""));

            Answer answer = wrapper.execute(command, resourceWithRbdPool());

            Assert.assertTrue(answer.getResult());
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<String[]>> captor = ArgumentCaptor.forClass(List.class);
            script.verify(() -> Script.executePipedCommands(captor.capture(), Mockito.anyLong()));
            List<String> argv = Arrays.asList(captor.getValue().get(0));
            Assert.assertTrue("the removeanchors operation is the one asked for: " + argv, argv.contains("removeanchors"));
            Assert.assertTrue("the disk of the volume is passed: " + argv,
                    argv.stream().anyMatch(arg -> arg.startsWith("rbd:")));
        }
    }

    /**
     * A volume of any other pool type carries no anchor, so the script is not run at all rather
     * than run with nothing to remove.
     */
    @Test
    public void aVolumeOfAnotherPoolTypeRunsNoScript() {
        RemoveBackupAnchorsCommand command = new RemoveBackupAnchorsCommand(
                List.of(poolOfType(Storage.StoragePoolType.NetworkFilesystem)), List.of("/mnt/uuid/volume-path"));

        try (MockedStatic<Script> script = Mockito.mockStatic(Script.class)) {
            Answer answer = wrapper.execute(command, resourceWithRbdPool());

            Assert.assertTrue(answer.getResult());
            script.verifyNoInteractions();
        }
    }

    /**
     * A failing script is reported as a failed answer: the caller treats the removal as best
     * effort, but it has to be able to log what happened.
     */
    @Test
    public void aFailingScriptIsReported() {
        RemoveBackupAnchorsCommand command = new RemoveBackupAnchorsCommand(
                List.of(poolOfType(Storage.StoragePoolType.RBD)), List.of("volume-path"));

        try (MockedStatic<Script> script = Mockito.mockStatic(Script.class)) {
            script.when(() -> Script.executePipedCommands(Mockito.anyList(), Mockito.anyLong()))
                    .thenReturn(new Pair<>(1, "rbd: error"));

            Answer answer = wrapper.execute(command, resourceWithRbdPool());

            Assert.assertFalse(answer.getResult());
            Assert.assertEquals("rbd: error", answer.getDetails());
        }
    }
}
