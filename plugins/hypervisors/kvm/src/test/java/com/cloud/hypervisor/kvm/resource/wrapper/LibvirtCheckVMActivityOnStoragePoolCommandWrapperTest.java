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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.joda.time.Duration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckVMActivityOnStoragePoolCommand;
import com.cloud.agent.api.to.HostTO;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.hypervisor.kvm.resource.KVMHABase;
import com.cloud.hypervisor.kvm.resource.KVMHAMonitor;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.utils.script.Script;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtCheckVMActivityOnStoragePoolCommandWrapperTest {

    private static final String POOL_UUID = "7684f6e5-fd91-3435-b04e-59ae0efb3914";
    private static final String RBD_SCRIPT = "/scripts/kvmvmactivity_rbd.py";
    private static final String SCRIPT = "/scripts/kvmvmactivity.sh";

    private final LibvirtCheckVMActivityOnStoragePoolCommandWrapper wrapper = new LibvirtCheckVMActivityOnStoragePoolCommandWrapper();
    private final LibvirtComputingResource resource = mock(LibvirtComputingResource.class);
    private final KVMStoragePool pool = mock(KVMStoragePool.class);
    private final CheckVMActivityOnStoragePoolCommand command = mock(CheckVMActivityOnStoragePoolCommand.class);

    private void setUp(StoragePoolType type, boolean supportsHa) {
        StorageFilerTO filer = mock(StorageFilerTO.class);
        when(filer.getType()).thenReturn(type);
        when(filer.getUuid()).thenReturn(POOL_UUID);
        when(command.getPool()).thenReturn(filer);
        KVMStoragePoolManager poolManager = mock(KVMStoragePoolManager.class);
        when(poolManager.getStoragePool(type, POOL_UUID)).thenReturn(pool);
        when(resource.getStoragePoolMgr()).thenReturn(poolManager);
        when(pool.isPoolSupportHA()).thenReturn(supportsHa);
    }

    private KVMHABase.HAStoragePool addHaPool() {
        KVMHABase.HAStoragePool haPool = new KVMHABase.HAStoragePool(pool, "10.0.0.1", "cloudstack", KVMHABase.PoolType.PrimaryStorage);
        KVMHAMonitor monitor = mock(KVMHAMonitor.class);
        when(monitor.getStoragePool(POOL_UUID)).thenReturn(haPool);
        when(resource.getMonitor()).thenReturn(monitor);
        return haPool;
    }

    private void expectActivityCheck(String volumes, long suspectTime, boolean activity) {
        when(command.getHost()).thenReturn(mock(HostTO.class));
        when(command.getVolumeList()).thenReturn(volumes);
        when(command.getSuspectTimeInSeconds()).thenReturn(suspectTime);
        when(pool.hasVmActivity(any(), any(), any(), anyString(), anyString(), anyLong())).thenReturn(activity);
    }

    @Test
    public void testRbdPoolIsCheckedWithTheScriptOfRbd() {
        setUp(StoragePoolType.RBD, true);
        KVMHABase.HAStoragePool haPool = addHaPool();
        expectActivityCheck("volume-1,volume-2", 123L, false);

        try (MockedStatic<Script> script = Mockito.mockStatic(Script.class)) {
            script.when(() -> Script.findScript(anyString(), eq("kvmvmactivity_rbd.py"))).thenReturn(RBD_SCRIPT);
            Answer answer = wrapper.execute(command, resource);

            assertTrue(answer.getResult());
        }
        verify(pool).hasVmActivity(eq(haPool), any(HostTO.class), any(Duration.class), eq("volume-1,volume-2"), eq(RBD_SCRIPT), eq(123L));
        verify(resource, never()).getVmActivityCheckPath();
    }

    @Test
    public void testOtherPoolsAreCheckedWithTheScriptOfTheAgent() {
        for (StoragePoolType type : new StoragePoolType[] {StoragePoolType.NetworkFilesystem, StoragePoolType.SharedMountPoint}) {
            Mockito.reset(resource, pool, command);
            setUp(type, true);
            KVMHABase.HAStoragePool haPool = addHaPool();
            expectActivityCheck("volume-1", 5L, false);
            when(resource.getVmActivityCheckPath()).thenReturn(SCRIPT);

            Answer answer = wrapper.execute(command, resource);

            assertTrue(answer.getResult());
            verify(pool).hasVmActivity(eq(haPool), any(HostTO.class), any(Duration.class), eq("volume-1"), eq(SCRIPT), eq(5L));
        }
    }

    @Test
    public void testActivityIsReported() {
        setUp(StoragePoolType.NetworkFilesystem, true);
        addHaPool();
        expectActivityCheck("volume-1", 5L, true);
        when(resource.getVmActivityCheckPath()).thenReturn(SCRIPT);

        Answer answer = wrapper.execute(command, resource);

        assertFalse(answer.getResult());
        assertEquals("VMHA disk activity detected ...", answer.getDetails());
    }

    @Test
    public void testMissingScriptOfRbdIsAnError() {
        setUp(StoragePoolType.RBD, true);
        addHaPool();

        try (MockedStatic<Script> script = Mockito.mockStatic(Script.class)) {
            script.when(() -> Script.findScript(anyString(), anyString())).thenReturn(null);
            Answer answer = wrapper.execute(command, resource);

            assertFalse(answer.getResult());
            assertTrue(answer.getDetails().contains("unable to find the script"));
        }
        verify(pool, never()).hasVmActivity(any(), any(), any(), any(), any(), anyLong());
    }

    @Test
    public void testPoolWithoutHaSupportIsNotChecked() {
        setUp(StoragePoolType.RBD, false);
        when(resource.getMonitor()).thenReturn(mock(KVMHAMonitor.class));

        Answer answer = wrapper.execute(command, resource);

        assertFalse(answer.getResult());
        assertEquals("Unsupported Storage", answer.getDetails());
        verify(resource, never()).getVmActivityCheckPath();
    }
}
