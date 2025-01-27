/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cloud.hypervisor.kvm.resource.wrapper;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.UnprepareStorageClientAnswer;
import com.cloud.agent.api.UnprepareStorageClientCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.storage.Storage;
import com.cloud.utils.Pair;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtUnprepareStorageClientCommandWrapperTest {

    @Spy
    LibvirtUnprepareStorageClientCommandWrapper libvirtUnprepareStorageClientCommandWrapperSpy = Mockito.spy(LibvirtUnprepareStorageClientCommandWrapper.class);

    @Mock
    LibvirtComputingResource libvirtComputingResourceMock;

    private final static String poolUuid = "345fc603-2d7e-47d2-b719-a0110b3732e6";

    @Test
    public void testUnprepareStorageClientSuccess() {
        UnprepareStorageClientCommand cmd = Mockito.mock(UnprepareStorageClientCommand.class);
        Mockito.when(cmd.getPoolType()).thenReturn(Storage.StoragePoolType.PowerFlex);
        Mockito.when(cmd.getPoolUuid()).thenReturn(poolUuid);

        KVMStoragePoolManager storagePoolMgr = Mockito.mock(KVMStoragePoolManager.class);
        Mockito.when(libvirtComputingResourceMock.getStoragePoolMgr()).thenReturn(storagePoolMgr);
        Mockito.when(storagePoolMgr.unprepareStorageClient(cmd.getPoolType(), cmd.getPoolUuid())).thenReturn(new Pair<>(true, ""));

        UnprepareStorageClientAnswer result = (UnprepareStorageClientAnswer) libvirtUnprepareStorageClientCommandWrapperSpy.execute(cmd, libvirtComputingResourceMock);

        Assert.assertTrue(result.getResult());
    }

    @Test
    public void testUnprepareStorageClientFailure() {
        UnprepareStorageClientCommand cmd = Mockito.mock(UnprepareStorageClientCommand.class);
        Mockito.when(cmd.getPoolType()).thenReturn(Storage.StoragePoolType.PowerFlex);
        Mockito.when(cmd.getPoolUuid()).thenReturn(poolUuid);

        KVMStoragePoolManager storagePoolMgr = Mockito.mock(KVMStoragePoolManager.class);
        Mockito.when(libvirtComputingResourceMock.getStoragePoolMgr()).thenReturn(storagePoolMgr);
        Mockito.when(storagePoolMgr.unprepareStorageClient(cmd.getPoolType(), cmd.getPoolUuid())).thenReturn(new Pair<>(false, "Unprepare storage client failed"));

        UnprepareStorageClientAnswer result = (UnprepareStorageClientAnswer) libvirtUnprepareStorageClientCommandWrapperSpy.execute(cmd, libvirtComputingResourceMock);

        Assert.assertFalse(result.getResult());
        Assert.assertEquals("Unprepare storage client failed", result.getDetails());
    }
}
