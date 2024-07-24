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

import java.util.HashMap;
import java.util.Map;

import org.apache.cloudstack.storage.datastore.client.ScaleIOGatewayClient;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.PrepareStorageClientAnswer;
import com.cloud.agent.api.PrepareStorageClientCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.storage.Storage;
import com.cloud.utils.Ternary;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtPrepareStorageClientCommandWrapperTest {

    @Spy
    LibvirtPrepareStorageClientCommandWrapper libvirtPrepareStorageClientCommandWrapperSpy = Mockito.spy(LibvirtPrepareStorageClientCommandWrapper.class);

    @Mock
    LibvirtComputingResource libvirtComputingResourceMock;

    private final static String poolUuid = "345fc603-2d7e-47d2-b719-a0110b3732e6";
    private final static String systemId = "218ce1797566a00f";
    private final static String sdcId = "301b852c00000003";

    @Test
    public void testPrepareStorageClientSuccess() {
        Map<String, String> details = new HashMap<>();
        details.put(ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID, systemId);
        PrepareStorageClientCommand cmd = Mockito.mock(PrepareStorageClientCommand.class);
        Mockito.when(cmd.getPoolType()).thenReturn(Storage.StoragePoolType.PowerFlex);
        Mockito.when(cmd.getPoolUuid()).thenReturn(poolUuid);
        Mockito.when(cmd.getDetails()).thenReturn(details);

        KVMStoragePoolManager storagePoolMgr = Mockito.mock(KVMStoragePoolManager.class);
        Mockito.when(libvirtComputingResourceMock.getStoragePoolMgr()).thenReturn(storagePoolMgr);
        details.put(ScaleIOGatewayClient.SDC_ID, sdcId);
        Mockito.when(storagePoolMgr.prepareStorageClient(cmd.getPoolType(), cmd.getPoolUuid(), cmd.getDetails())).thenReturn(new Ternary<>(true, details, ""));

        PrepareStorageClientAnswer result = (PrepareStorageClientAnswer) libvirtPrepareStorageClientCommandWrapperSpy.execute(cmd, libvirtComputingResourceMock);

        Assert.assertTrue(result.getResult());
        Assert.assertEquals(sdcId, result.getDetailsMap().get(ScaleIOGatewayClient.SDC_ID));
    }

    @Test
    public void testPrepareStorageClientFailure() {
        Map<String, String> details = new HashMap<>();
        details.put(ScaleIOGatewayClient.STORAGE_POOL_SYSTEM_ID, systemId);
        PrepareStorageClientCommand cmd = Mockito.mock(PrepareStorageClientCommand.class);
        Mockito.when(cmd.getPoolType()).thenReturn(Storage.StoragePoolType.PowerFlex);
        Mockito.when(cmd.getPoolUuid()).thenReturn(poolUuid);
        Mockito.when(cmd.getDetails()).thenReturn(details);

        KVMStoragePoolManager storagePoolMgr = Mockito.mock(KVMStoragePoolManager.class);
        Mockito.when(libvirtComputingResourceMock.getStoragePoolMgr()).thenReturn(storagePoolMgr);
        Mockito.when(storagePoolMgr.prepareStorageClient(cmd.getPoolType(), cmd.getPoolUuid(), cmd.getDetails())).thenReturn(new Ternary<>(false, new HashMap<>() , "Prepare storage client failed"));

        PrepareStorageClientAnswer result = (PrepareStorageClientAnswer) libvirtPrepareStorageClientCommandWrapperSpy.execute(cmd, libvirtComputingResourceMock);

        Assert.assertFalse(result.getResult());
        Assert.assertEquals("Prepare storage client failed", result.getDetails());
    }
}
