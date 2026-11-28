/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.agent.api.storage.ResizeVolumeAnswer;
import com.cloud.agent.api.storage.StorPoolResizeVolumeCommand;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.hypervisor.kvm.storage.StorPoolStorageAdaptor;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class StorPoolResizeVolumeCommandWrapperTest {

    private static final String VOLUME_PATH = "/dev/storpool-byid/t.t.t";
    private static final long GiB = 1024L * 1024 * 1024;

    @Mock
    private LibvirtComputingResource libvirtComputingResource;
    @Mock
    private KVMStoragePoolManager storagePoolMgr;
    @Mock
    private KVMStoragePool pool;
    @Mock
    private StorageFilerTO storageFilerTO;

    private MockedStatic<StorPoolStorageAdaptor> storPoolStorageAdaptor;
    private final StorPoolResizeVolumeCommandWrapper wrapper = new StorPoolResizeVolumeCommandWrapper();

    @Before
    public void setUp() {
        storPoolStorageAdaptor = Mockito.mockStatic(StorPoolStorageAdaptor.class);

        KVMPhysicalDisk disk = new KVMPhysicalDisk(VOLUME_PATH, "t.t.t", pool);
        disk.setVirtualSize(20 * GiB);

        when(storageFilerTO.getType()).thenReturn(StoragePoolType.StorPool);
        when(storageFilerTO.getUuid()).thenReturn("pool-uuid");
        when(libvirtComputingResource.getStoragePoolMgr()).thenReturn(storagePoolMgr);
        when(storagePoolMgr.getStoragePool(StoragePoolType.StorPool, "pool-uuid")).thenReturn(pool);
        when(pool.getPhysicalDisk(VOLUME_PATH)).thenReturn(disk);
        Mockito.lenient().when(libvirtComputingResource.getResizeVolumePath()).thenReturn("resizevolume.sh");
        Mockito.lenient().when(libvirtComputingResource.getCmdsTimeout()).thenReturn(60);
    }

    @After
    public void tearDown() {
        storPoolStorageAdaptor.close();
    }

    @Test
    public void testResizeSucceedsWhenDetachFails() {
        mockDetachFailure();

        try (MockedConstruction<Script> ignored = Mockito.mockConstruction(Script.class)) {
            ResizeVolumeAnswer answer = wrapper.execute(detachedVolumeResizeCommand(), libvirtComputingResource);

            Assert.assertTrue(answer.getResult());
            Assert.assertEquals(20 * GiB, answer.getNewSize());
        }
        storPoolStorageAdaptor.verify(() -> StorPoolStorageAdaptor.attachOrDetachVolume("detach", "volume", VOLUME_PATH));
    }

    @Test
    public void testResizeReturnsTheAttachErrorWhenDetachFails() {
        storPoolStorageAdaptor.when(() -> StorPoolStorageAdaptor.attachOrDetachVolume("attach", "volume", VOLUME_PATH))
                .thenThrow(new CloudRuntimeException("Unable to attach volume ~t.t.t"));
        mockDetachFailure();

        ResizeVolumeAnswer answer = wrapper.execute(detachedVolumeResizeCommand(), libvirtComputingResource);

        Assert.assertFalse(answer.getResult());
        Assert.assertTrue(answer.getDetails().contains("Unable to attach volume ~t.t.t"));
        storPoolStorageAdaptor.verify(() -> StorPoolStorageAdaptor.attachOrDetachVolume("detach", "volume", VOLUME_PATH));
    }

    private void mockDetachFailure() {
        storPoolStorageAdaptor.when(() -> StorPoolStorageAdaptor.attachOrDetachVolume("detach", "volume", VOLUME_PATH))
                .thenThrow(new CloudRuntimeException("Unable to detach volume ~t.t.t"));
    }

    private StorPoolResizeVolumeCommand detachedVolumeResizeCommand() {
        return new StorPoolResizeVolumeCommand(VOLUME_PATH, storageFilerTO, 10 * GiB, 20 * GiB, false, null, false);
    }
}
