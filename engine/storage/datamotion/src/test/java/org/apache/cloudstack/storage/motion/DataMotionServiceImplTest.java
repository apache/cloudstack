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
package org.apache.cloudstack.storage.motion;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.StorageStrategyFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.host.Host;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.Storage.StoragePoolType;

@RunWith(MockitoJUnitRunner.class)
public class DataMotionServiceImplTest {

    @InjectMocks
    private DataMotionServiceImpl dataMotionService;

    @Mock
    private StorageStrategyFactory storageStrategyFactory;
    @Mock
    private AsyncCompletionCallback<CopyCommandResult> callback;
    @Mock
    private Host srcHost;
    @Mock
    private Host destHost;

    private final Map<VolumeInfo, DataStore> volumeMap = new LinkedHashMap<>();

    @Before
    public void setUp() {
        Mockito.when(srcHost.getName()).thenReturn("host1");
        Mockito.when(destHost.getName()).thenReturn("host2");
    }

    private void addRbdVolumes() {
        volumeMap.put(mockVolume("root-uuid", mockPool("ceph1", StoragePoolType.RBD)), mockPool("nfs1", StoragePoolType.NetworkFilesystem));
        volumeMap.put(mockVolume("data-uuid", mockPool("ceph1", StoragePoolType.RBD)), mockPool("ceph2", StoragePoolType.RBD));
    }

    private PrimaryDataStore mockPool(String name, StoragePoolType type) {
        PrimaryDataStore pool = Mockito.mock(PrimaryDataStore.class);
        Mockito.lenient().when(pool.getName()).thenReturn(name);
        Mockito.lenient().when(pool.getPoolType()).thenReturn(type);
        return pool;
    }

    private VolumeInfo mockVolume(String uuid, DataStore srcPool) {
        VolumeInfo volume = Mockito.mock(VolumeInfo.class);
        Mockito.when(volume.getUuid()).thenReturn(uuid);
        Mockito.lenient().when(volume.getDataStore()).thenReturn(srcPool);
        return volume;
    }

    private String migrateAndGetError() {
        dataMotionService.copyAsync(volumeMap, null, srcHost, destHost, callback);
        ArgumentCaptor<CopyCommandResult> captor = ArgumentCaptor.forClass(CopyCommandResult.class);
        Mockito.verify(callback).complete(captor.capture());
        return captor.getValue().getResult();
    }

    @Test
    public void copyAsyncKvmNoStrategyNamesPoolTypesAndSaysToStopVm() {
        Mockito.when(srcHost.getHypervisorType()).thenReturn(HypervisorType.KVM);
        addRbdVolumes();

        Assert.assertEquals("KVM cannot live migrate the volumes of this running VM from host host1 to host host2 between these storage pools: "
                + "data-uuid from ceph1 (RBD) to ceph2 (RBD), root-uuid from ceph1 (RBD) to nfs1 (NetworkFilesystem). "
                + "Stop the VM and migrate the volumes while it is stopped.", migrateAndGetError());
    }

    @Test
    public void copyAsyncKvmNoStrategyUnknownPoolType() {
        Mockito.when(srcHost.getHypervisorType()).thenReturn(HypervisorType.KVM);
        DataStore imageStore = Mockito.mock(DataStore.class);
        Mockito.when(imageStore.getName()).thenReturn("store1");
        volumeMap.put(mockVolume("vol-uuid", null), imageStore);
        volumeMap.put(mockVolume("vol2-uuid", mockPool("pool1", null)), mockPool("pool2", null));

        Assert.assertEquals("KVM cannot live migrate the volumes of this running VM from host host1 to host host2 between these storage pools: "
                + "vol-uuid from unknown pool to store1, vol2-uuid from pool1 (unknown type) to pool2 (unknown type). "
                + "Stop the VM and migrate the volumes while it is stopped.", migrateAndGetError());
    }

    @Test
    public void copyAsyncNonKvmNoStrategyKeepsGenericError() {
        Mockito.when(srcHost.getHypervisorType()).thenReturn(HypervisorType.XenServer);
        addRbdVolumes();

        Assert.assertEquals("Can't find strategy to move data. Source Host: host1, Destination Host: host2, Volume UUIDs: root-uuid,data-uuid", migrateAndGetError());
    }
}
